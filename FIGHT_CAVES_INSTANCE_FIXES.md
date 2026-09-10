# Fight Caves instance migration — step 4

Implemented 2026-09-08 against the active `src` tree. This supersedes the shared-Fight-Caves status in the earlier instance reports. It migrates the existing encounter; it does not certify or rebalance its historical combat mechanics.

## Completed

- `FightCavesSession` owns one player's run: wave counter, live monsters, spawn anchors, wave queue, failure/completion state and rewards. The static `FightCaves` facade retains entrance, dialogue, combat-helper and command integration. No global current player or global live-NPC list remains.
- Each run reserves its own map and copies the complete 64×64 cave region, source chunks (296,632), plane 0. Player entry, all five spawn anchors, the wave-three/Jad anchor, splits and healers use translated positions. The copied exit object (9357), objects and clipping come from the actual cache. Capacity is one participant per instance; multiple independent runs can coexist.
- Cave monsters use `GameInstance.spawnNpc` and retain their existing subclasses, health, animations and combat actions. Ownership drives target acquisition, long-distance cave pursuit and nearby MejKot healing. Other runs' NPCs cannot become targets or receive healing.
- Wave delays and the watchdog use instance-owned tasks. Jad's delayed ranged graphic belongs to Jad; each repeating healer task belongs to its healer. Closure/removal cancels them. Old Jad callbacks cannot spawn healers into a player's subsequent run.
- All 63 existing wave arrays, five spawn anchors, five-cycle wave delay, four-cycle victory delay, Jad anchor selection and Tz-Kek splits are retained. Spawn failure stops progression; startup failure rolls back admission without rewards. Familiars remain excluded.
- Added a creation-time `GameInstance.setDepartureHandler`. It runs once per successful departure, after evacuation, pending-death resolution and membership removal, including direct leave, disconnect and shutdown. Evacuation failures retain membership for retry before the handler runs. A handler exception closes the instance rather than leaking an empty active map; completed membership transitions do not replay the handler. The handler reference is cleared on close.
- Fight Caves uses that handler to clear the session reference, cave flag, teleblock and wave configuration, settle the reward once and close its empty solo allocation. Normal exit, death, disconnect, an allowed ordinary-world teleport and orderly shutdown all use the same cleanup. Safe instance death restores the player and preserves inventory. A player dying during Jad's victory delay receives consolation only.
- Victory still awards one fire cape and 16,064 Tokkul. Consolation remains `2*c*c + 6*c + 4`, where `c = max(0, currentWave - 1)`; reaching Jad yields 8,064. Rewards are settled after departure. Inventory insertion is checked, allowing existing Tokkul stacks to grow even with no free slot; rejected items drop privately at the ordinary cave lobby and survive map cleanup. This is not a durable transactional reward/checkpoint system.
- Repeat entrance clicks preserve the current run. `::wave N` explicitly closes only the caller's old run and starts the requested wave. `::caves` and `::cavepull` retain their purposes; pulls now find valid owned-map positions. A generic completion call cannot award a cape without an owned Jad death.

## Verification

Java 8-targeted compilation and all 13 suites pass. Reproduce with `build/fight-caves-instances/verify.ps1`.

- `FightCavesInstanceRegression`: **414 checks**, using the real cache and isolated schedulers. Covers all 63 waves while another run remains active, copied exit access, NPC pursuit beyond the ordinary leash, multi-combat recognition, splits, Jad anchor reuse, two simultaneous Jads/healer sets, MejKot targeting, stale callbacks, exact/duplicate/full-inventory rewards, death during victory delay, disconnect, teleport, direct leave, startup/spawn failures, queued entry, admin restart and shutdown.
- `InstanceIntegrationRegression`: 53 checks; `InstanceLifecycleRegression`: 73; `InstanceFoundationRegression`: 18,776.
- Nex ice and movement suites; Barrows regression (711,204 checks) and face timing suite.
- Equipment absorption (238 checks), combat enhancements (46 plus 10,000 Elysian trials), combat foundation (117), combat formula (90 plus 180,000 generated attacks) and combat balance (48 plus 200,000 samples).

The cave test deliberately injects a failed wave and a scheduler failure; their two diagnostic messages are expected. No unexpected exceptions appeared. Tests use mocked connections and do not start a server, invoke account saving, alter existing saves or execute the process shutdown hook. They validate the shutdown controller and departure behavior in isolation.

## Runtime and rollback

**18 runtime classes were staged into `bin` and SHA-256 verified. The server was not restarted.**

- Compiled output, logs and reproduction/staging scripts: `build/fight-caves-instances/`.
- Exact source list: `build/fight-caves-instances/changed-sources.txt`.
- Pre-step source and prior staged-class backups: `build/fight-caves-instances-before/`.
- Runtime files, whether a prior class existed, backup paths and hashes: `build/fight-caves-instances/staged-classes.csv`.
- Review against this step's source backups: `build/fight-caves-instances/source-review.diff`.

Rollback only this batch's listed sources/classes, restoring prior files where present and removing newly introduced files listed without a prior counterpart. Preserve unrelated workspace changes and previous instance/combat batches. Old unused FightCaves anonymous class files are not referenced by the new facade and were left in place.

## Live acceptance and remaining work

After a normal restart, use two accounts to enter simultaneously and verify map rendering, movement around rocks, NPC attacks and independent wave configurations. Exercise `::wave 3`, `::wave 62`, `::wave 63` and `::cavepull`; check splits, Jad/healer behavior and that restarting one run leaves the other intact. Use ordinary combat with godmode disabled when checking encounter behavior.

Check the exit object, safe death, disconnect/reconnect during a wave and during death, full-inventory victory drops, and orderly shutdown with active runs. Confirm reentry does not display stale NPCs, objects or wave UI. These client and real restart checks remain outstanding.

Runs remain ephemeral: logout/shutdown aborts with consolation, and saves project the ordinary cave exit. There is no wave checkpoint/resume across login or process restart. Recovery after a forced process kill uses the latest completed player save; it cannot run departure rewards. Quest/skilling proof cases and Dungeoneering/party migration remain later work.

Godmode, personal XP rates, boosted Barrows rewards and the owner's Summoning-specific deferral are preserved.

## Live acceptance fix: NPC list after map rebuild - 2026-09-08

The owner reported Doobie's first-wave monster attacking invisibly for the entire wave while Doobie and Test123 entered through the cave entrance a few seconds apart. Both accounts had already been logged in for several minutes; later waves displayed normally.

A reproducible server/client state mismatch was found. DynamicMapPacket sends rebuild mode 1. The supplied client's Class98_Sub10_Sub13.method1043 clears its local NPC count/table for every mode-1 rebuild, including position-triggered scene refreshes. The server's NpcUpdate retained its old per-player localNpcs list. Its next packet could therefore reference NPCs the client had discarded. The actual decompiled retained-list reader rejects that packet with `gnpov1` when the server's retained count exceeds the client's count. This explains how an NPC may remain alive and attacking on the server while absent on the client, with visibility recovering once the stale local list empties and later-wave additions arrive. The mismatch was reproduced headlessly; the exact triggering rebuild in the owner's live session was not captured.

NpcUpdate now tracks whether its last published scene was dynamic and resets its per-player local list whenever a dynamic map is published, or when returning from dynamic to ordinary mode. ActionSender invokes the hook only after constructing the corresponding map packet. The next NPC packet carries fresh additions. Ordinary-to-ordinary rebuilds preserve the list, matching the client, and another player's list is unaffected. Map encoding, wave timing, NPC combat/model definitions, rewards and instance ownership are unchanged.

Validation:

- New headless ClientNpcSceneRegression: **80 checks passed**. It extracts the exact reset, retained-list and NPC-addition methods from the matching DyNamic decompilation and uses the unmodified binary client's bit readers. NPC definitions, rendering and movement effects are stubs; this validates list/placement state rather than visible rendering or mask effects.
- Before the fix, the regression reproduced the actual client `gnpov1` exception on ordinary-to-cave entry with an existing NPC list. After the fix it covers first-wave entry, exact NPC coordinates, two independent caves, twelve repeated rebuilds while a first-wave NPC remains alive, movement-triggered scene publication, ordinary-world return, ordinary-to-ordinary list retention, shared-room expansion for stationary viewers, and failure before map publication.
- All **16 existing server regression suites** pass, including FightCavesInstanceRegression (414), InstanceContentRegression (144), InstancePartyRegression (122), InstanceOperationsRegression (123), foundation/integration/lifecycle and the nine prior combat/encounter suites. The Step 6 load benchmark was not rerun for this packet-state fix.
- Client decoder sources/JAR hashes and the test boundary are recorded in build/instance-npc-sync/provenance.json. Before-fix and passing logs are in the same directory. Reproduce client checks with tests/client/verify-npc-scenes.ps1 and server checks with build/instance-npc-sync/verify.ps1.

Only NpcUpdate and ActionSender class families were staged and SHA-256 verified; prior source/runtime versions are under build/instance-npc-sync-before, with exact runtime files in build/instance-npc-sync/staged-classes.csv. Restart is required. No live server/client was started or restarted, no external client/cache files were edited, and no player saves were written. The owner should repeat the two-account first-wave test after restart, moving around while the first monster remains alive. Live visual confirmation is still required.

### Owner-confirmed live result

The owner subsequently retested and reported: "tested - its fixed." This confirms the reported invisible first-wave NPC issue is resolved in the owner's live test. It supersedes the pending live-confirmation statement for that issue only; it does not certify every instance, client-rendering, disconnect or restart acceptance case.
