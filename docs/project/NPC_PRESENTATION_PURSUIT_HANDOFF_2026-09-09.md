# Task handoff: NPC animation, overlap and arena pursuit — step 1

- **Authorized scope:** owner requested implementation of step 1 of the current three-step plan. This batch repairs shared attack interruption, overlap and arena pursuit. Step 2 remains the complete God Wars/Corp/Nex encounter work; step 3 retains live acceptance. Preserve custom definitions, rewards, godmode, unrestricted access and the developer client launcher.
- **Relevant reports:** current plan in [NPC_COMBAT_LIVE_REVIEW_2026-09-09.md](NPC_COMBAT_LIVE_REVIEW_2026-09-09.md), subsystem rows in [CONTEXT_INDEX.md](CONTEXT_INDEX.md), earlier [NPC_MOVEMENT_TIMING_HANDOFF_2026-09-09.md](NPC_MOVEMENT_TIMING_HANDOFF_2026-09-09.md). Use the listed God Wars/Corp/Nex reports for step 2; do not repeat the completed broad audit.

## Implementation and expected behavior

Seven production families changed, all under `src/org/dementhium/`:

| Files | Changed contract and production proof |
| --- | --- |
| `model/mask/Mask.java`, new `model/mask/NPCAnimation.java` | Keep the NPC action protected from ordinary defence flinches for its native sequence duration, including across outgoing-mask resets. New actions, death and explicit animation cancellation retain priority; combat-generation reset invalidates old protection. Frame lengths are read and cached from native index 20 without cache writes. Actual NPC task launch, incoming player melee/ranged actions and outgoing NpcUpdate are exercised for Bandos smash and both Sara attacks. |
| `model/combat/CombatMovement.java`, `model/npc/godwars/GodWarsRoom.java` | Ranged NPCs standing on the target must move clear before attacking. God Wars followers avoid increasing overlap with living room members and use a bounded local search to separate or pass a blocking room member. This checks at most the room's four actors; it does not impose global NPC solidity. Real task/update cycles with all followers, changing target positions and both actor orders retain world registration and finish separated. God Wars room admission also wins over the contradictory ordinary 17-tile chase cutoff. |
| `model/npc/impl/Nex.java`, `model/combat/NPCCombatContext.java`, `task/impl/NPCTickTask.java` | Nex's action prototype uses the current executor target before an Interaction exists. Her selected approach persists, and cached routes find full-footprint contact inside the arena, including around pits. Ordinary movement respects freeze/stun/busy/forced-movement and special locks. The arena event owns targeting and empty-room reset instead of generic NPC aggression/12-tile home recovery. Eligible room targets survive the generic distance gate; departure/invisibility/invalid life still rejects the pair. Actual NexAreaEvent Tick.run precedes NPCTickTask/queue progression in the distant-acquisition and departure/empty-room test. Existing launched-hit captures remain independent. |

New tests are in `tests/NPCPresentationRegression.java`. Native terrain cases assert the target is on walkable floor, cover nine arena approaches including opposite sides, and use normal executor startup cooldown. Explicit Bandos/Sara branch tests deliberately choose an attack and zero the initial cooldown; they do not prove natural full-fight selection. Concurrent room tests use actual God Wars multicombat coordinates with isolated flat terrain; the broader movement/instance suites provide real cover and managed-map edge coverage. Legitimate walk-under behavior, ordinary safespots and Corp's encounter-specific underfoot handler remain intact.

## Verification and retained evidence

Final manifest: `tools/batches/npc-animation-pursuit-step1-final-20260909.json`.
Receipt/log directory: `build/batches/npc-animation-pursuit-step1-final-20260909/verify-ea946484274445a9b040ffad20109207/` (`summary.json` and individual suite logs).

All 11 selected suites passed:

- `NPCPresentationRegression`: attack interruption, native client semantics, overlap/visibility registration, concurrent room followers, Nex terrain/locks and arena-event ordering.
- `NPCMovementTimingRegression`, `NPCMovementInstanceRegression`, `FightCavesInstanceRegression`: shared pursuit/contact, sizes/planes/sector offsets, launch/impact timing, real cover, safespots and full-footprint managed-map edges.
- `SharedCombatRepairRegression`, `NPCDeathRewardRegression`: affected status/lifetime/reflection and death/reward contracts.
- `GodWarsRegression runtime`, `GodWarsCustomRegression`: room behavior and approved custom definitions.
- `NexDamageRegression`, `NexMovementRegression`, `NexIceRegression`: captured damage, forced movement ownership/timing and ice placement/rescue/cleanup.

Measured suite execution **17.407 s**; compilation **6.516 s**; candidate assembly **6.585 s**; total Verify **52.489 s**, including remaining hash work. No unrelated all-suite release gate was run. The presentation suite measured **437.252 ms for 240 isolated four-NPC room/update cycles**; this is not a live World-cycle or server-load measurement.

Decisive negative evidence is retained under `build/batches/`:

- `npc-animation-pursuit-step1-release-20260909/debug-27b51995e1b94d55a475c3489e19657c/`: real incoming-flinch interruption, ranged follower/player overlap and fresh Nex prototype failure before fixes.
- `npc-animation-pursuit-step1-release-20260909/debug-5cdacb1d99c54de8af0ad62a72aee1e1/`: Nex drops an eligible target across the actual room because of generic distance admission.
- `npc-animation-pursuit-step1-final-20260909/debug-d3eec2f267f6493ea0ffe63552b0bb63/`: Sara far-room reset loop and Nex arena event's independent distant-acquisition failure.
- The final presentation log includes a negative control using **the packaged v7 developer client's own sequence replacement function**: incoming defence 7061 replaces unfinished Bandos smash 7063. Native client decoding agrees with the server duration reader for nine affected Bandos/Sara/Corp/Nex sequences. This is headless decoder/replacement proof, not renderer/network playback or visual animation identity.

Earlier failed setup attempts remain intact. The first concurrent fixture was incorrectly outside multicombat, and one initial terrain target was inside a wall; those failures are not gameplay defects. The corrected production-coordinate cases pass. There is no evidence here that every recorded boss symptom has one cause or was caused by the efficiency policy.

## Artifact and live state

| State | Evidence |
| --- | --- |
| Implemented | Seven source families plus the new presentation suite. Exact scoped diff is `build/batches/npc-animation-pursuit-step1-final-20260909/source-review.diff`, against the appropriate Prepare backups rather than unrelated all-day Git changes. |
| Automated verified | All 11 selected suites passed against the isolated exact staging candidate; receipt above. |
| Staged | **28 classes in seven families**, with runtime backups and SHA-256 checks in `build/batches/npc-animation-pursuit-step1-final-20260909/before/runtime/` and `staged.json`. Test classes were not staged. |
| Loaded after restart | **Unverified. No server restart was performed.** Disk hashes cannot establish bytes already loaded into the running JVM or Eclipse hot-swap state. |
| Live accepted | **Pending.** Full visible animation playback, clickability, real network/World-cycle timing and complete encounter acceptance remain open. |

Prepare provenance: the original pre-step-1 backups for the first six families are in `npc-animation-pursuit-step1-release-20260909/before/source/`, including the absent new animation helper. The final batch was prepared when NPCCombatContext and its arena dependency were added: its copy of that file is before the first edit, while its other six files are an intermediate step-1 snapshot. `source-provenance.txt` records this distinction. Earlier prepares/debug logs are preserved; no retrospective backup is claimed.

Launch inspection identified server PID 11704 started 19:34:03 via Eclipse/Java 8 with this workspace's `bin` classpath, and developer client PID 14020 started 19:34:30 with `Gambler-live-v7.jar`. Evidence is `npc-animation-pursuit-step1-release-20260909/live-launch-identity.json`. The stable launcher remains `build/gambler-interface/dev-client/Run Dev Client.bat`. The client JAR and cache were only read.

The prior movement-edge release's CombatMovement.class hash did not match the file present before this repair (last written 19:33:59); the other eight earlier movement class checks matched. Evidence is `prior-runtime-identity.json` in that release batch. Eclipse is a known writer to `bin`, but the mismatch alone does not prove its cause, loaded bytes or gameplay behavior. This final batch verified the current candidate and backed up the actual pre-stage runtime, including that mismatched class. Preserve these backups and guard against later output drift before relying on this receipt.

## Next work and closure conditions

- Step 2: God Wars attack/follower completion and Sara door restoration/clipping/two-player views; Corp's complete attack and single-owned-core lifecycle; Nex natural phases/specials, return to normal attacks, wrath/loot/reset/respawn. Corp's apparent extra cores still require world-NPC ownership counts versus split graphics. Neither the Sara door nor Corp core rules were changed here.
- After the staged build is deliberately loaded, test Bandos/Sara stationary and kited combat with godmode disabled: full tells under incoming player attacks, followers visible and clickable off the player/boss footprint, no back-and-forth loop. Test Nex distant acquisition, pit-corner pursuit, walk-under, special locks and departure/empty-room reset. Compare loaded-build evidence to this receipt.
- Measure representative simultaneous combat World-cycle timing and inspect actual client playback. An isolated task benchmark cannot close the reported delayed feel. Full encounter kill/loot/reset/respawn and two-player transitions remain the step 2/3 acceptance card in the main plan.
- No implementation blocker remains for this batch. Summoning/Dungeoneering remain deferred; preserve custom XP/rewards/access and all historical logs, account state and recovery artifacts.
