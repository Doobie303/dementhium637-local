# Instance plan status review

Reviewed 2026-09-08 after step 4. Compared the original six-step plan in the "Review instance management system" conversation with current source, the four implementation reports and the automated regression harnesses. No gameplay code was changed or staged, no live server was started/restarted, and no existing player saves were edited during this review.

## Original plan and current position

| Step | Original scope | Current status |
|---|---|---|
| 1 | Repair map allocation/release, units, collision, copied objects, transitions and startup copies; initially reject rotation. | Implemented and regression-tested. See INSTANCE_FOUNDATION_FIXES.md. |
| 2 | Stable instance IDs, membership/boundaries, resource ownership, cancellation, rollback, safe cleanup and game-thread mutations. | Implemented and regression-tested. See INSTANCE_LIFECYCLE_FIXES.md. |
| 3 | Entry/exit, logout/disconnect, death, teleports/admin movement, shutdown/startup and safe saved positions; explicit checkpoint policy. | Implemented for ephemeral sessions and safe exits. Durable resumable checkpoints are not implemented. See INSTANCE_INTEGRATION_FIXES.md. |
| 4 | Independent Fight Caves sessions, translated positions, splits/healers/callbacks and one-time completion rewards. | Implemented and regression-tested. See FIGHT_CAVES_INSTANCE_FIXES.md. Rewards are protected against duplicate live callbacks, not transactional across process crashes. |
| 5 | Prove reuse with a small quest scene and skilling room; then migrate Dungeoneering, parties, owner departure and dynamic room expansion. | Pending. The original step includes all of these, not just the two small demonstrations. |
| 6 | Operational counts/usage/resource/age/idle diagnostics, administrative inspection/closure, capacity limits and load validation. | Pending. Basic resource counts, cleanup failures and allocator exhaustion behavior exist, but the operational layer and sustained workload validation do not. |

Steps 1-4 are implemented, not fully certified through the client. There is no evidence from this review that another foundation rewrite is required. Dungeoneering still uses the old slot arrays, coordinate-copy APIs and activity cleanup; it does not inherit the new session guarantees automatically.

## Make these requirements explicit in the remaining steps

### Step 5

- Add reusable template definitions and named/local coordinate conversion. The original design proposed an InstanceTemplate abstraction; FightCavesSession currently supplies its own map coordinates and translation. Also define whether a template deliberately snapshots current world state or must start from a clean baseline. DynamicRegion currently snapshots the loaded source, including temporary object/resource changes; this needs an intentional policy for skilling rooms.
- Add a validated active-session room-building API. GameInstance.copyMap currently accepts only BUILDING state before resources exist; InstanceLifecycleRegression explicitly verifies this restriction. Dungeoneering cannot expand active rooms through this API yet. Preserve footprint ownership, resident protection, rollback and stationary-player scene refresh when adding that capability.
- Add content-specific death/respawn policy and tests. Current managed policies are SAFE_RETURN and STANDARD_AT_EXIT. The old DungeoneeringActivity expects an internal respawn and death counter; merely admitting its player into GameInstance would instead select the managed death path first.
- Implement actual party authorization and ownership policy: membership capacity already exists, but invitations/join authorization, leader departure, remaining-member lifetime and any shared completion/reward rules still need content integration.
- Audit every migrated content timer, spawned NPC, object, drop and reward. Shared ownership cannot infer the origin of arbitrary direct World.submit callbacks or source-less delayed damage. Fight Caves is migrated; unrelated activities are not.
- Decide explicitly whether the proof quest requires persistent progress or reentry. The existing quest repository initialization remains empty. A small private scene demonstrates instance reuse; it does not implement the server's wider quest catalogue or a full Dungeoneering gameplay system.

### Step 6

- Add operational inspection and safe close commands, template/activity identifiers, allocation/resource totals, age/idle tracking and actionable retained-cleanup diagnostics.
- Enforce configured active-session/resource/request limits and admission throttling. InstanceManager currently drains at most 1,024 queued requests per cycle, but that is not a bound on its ConcurrentLinkedQueue or a measured gameplay capacity limit.
- Define empty/abandoned-session expiry and idle policy. Failed cleanup is retried, but generic sessions that were created and never populated still require their content owner to close them.
- Run sustained create/play/close/reuse workloads, mixed concurrent activities and fault injection. Measure allocation/resource return to baseline, retained memory after warmup, tick latency and clean capacity rejection. Existing concurrency and exhaustion tests do not establish production throughput or long-duration memory behavior.

## What is additional or optional outside those steps?

Required release acceptance: normal restart to load staged classes, client map/rendering/UI verification, two real clients transitioning between copies and the ordinary world, and real disconnect/reconnect and orderly restart checks. These are validation gates rather than another architecture phase. This review did not establish whether the owner has restarted since staging.

Optional feature extensions, unless a chosen activity requires them:

- Resume/checkpoints across logout/restart and durable completion/reward transactions. The current Fight Caves policy aborts with consolation on orderly departure; a forced process kill uses the latest completed save and cannot run departure rewards. Live duplicate-callback protection is not a database transaction.
- Rotated chunks, broad procedural generation and player housing. Rotation was deliberately excluded from the initial foundation, and the old housing placeholder is not a housing system.
- Familiar support inside managed instances, still excluded under the owner's Summoning deferral.
- Additional quest/minigame/skill content beyond the concrete step-5 migrations.

## Testing without entering the game

Reran build/fight-caves-instances/verify.ps1 during this review: Java 8-targeted compilation and all 13 suites passed.

Instance-specific counts: FightCavesInstanceRegression 414; InstanceIntegrationRegression 53; InstanceLifecycleRegression 73; InstanceFoundationRegression 18,776. The other nine suites cover Nex, Barrows, absorption and shared combat.

Current automated tests use real cache data, simulated players/connections and controlled tick schedulers. They cover all 63 Fight Caves waves, simultaneous runs/Jads, splits/healers, reward duplication, owned cleanup, departure/death/rollback, stale callbacks and map reuse, collision/object isolation, packet construction, in-memory save/load recovery, allocator concurrency and exhaustion. Advancing waves through NPC death callbacks is not a full fight played by an automated client.

Further server-side work can be tested independently: quest progression, depletion/respawn, party transitions, room expansion, randomized lifecycle sequences, large mixed workloads and failure recovery. A disposable process and temporary save directory could additionally test actual file persistence and process restarts without touching live accounts; that harness has not been built or run.

Server-side assertions cannot certify the actual client's rendering, interface presentation, animations, visible scene refresh or end-to-end live network behavior. A short targeted client acceptance pass remains necessary even after all automated suites pass.

## Step 5A implementation update - 2026-09-08

The earlier review above is a historical snapshot after Step 4. Step 5A is now implemented; see INSTANCE_CONTENT_FIXES.md. It adds immutable reusable templates, named/local coordinates, an explicit loaded-world snapshot policy, and admin-accessible private quest/skilling examples. Owned resources are initialized fresh per room. Fourteen regression suites pass, including 118 new content checks. Runtime classes are staged; restart and live client acceptance remain outstanding.

Remaining breakdown:

- **5B:** party admission/leadership/lifetime rules, validated active room expansion and activity-specific death/respawn.
- **5C:** Dungeoneering migration onto those APIs while preserving existing gameplay.
- **6:** operational diagnostics/closure, configured limits/expiry and sustained workload validation.

A pristine cache-template baseline, durable quest progress and checkpoints were not implemented. The examples deliberately use ephemeral progress and loaded-world snapshots with fresh owned resources. The initial Step 5 'pending' row above is superseded only for the completed 5A scope.

## Step 5B implementation update - 2026-09-08

Step 5B is now implemented; see INSTANCE_PARTY_FIXES.md. It adds invitation-only party admission, explicit leader-departure/lifetime rules, append-only construction within reserved maps, and opt-in internal respawn. Fifteen suites pass, including 122 new checks and real rebuild output for two stationary simulated viewers. Twenty-one runtime classes are staged; restart and live client acceptance remain outstanding.

The remaining implementation sequence is **5C: Dungeoneering migration**, followed by **6: operational controls and sustained load validation**. The earlier 5B-pending statements are historical. Map expansion does not enlarge reservations or transactionally roll back arbitrary content population. Loaded-source snapshots, ephemeral membership/progress, unsupported rotations and the familiar deferral remain intentional limits.

## Step 6 implementation and Step 5C deferral - 2026-09-08

The owner explicitly deferred **5C: Dungeoneering migration** until its later gameplay overhaul and authorized Step 6 independently. The earlier mandatory 5C-before-6 sequence is superseded.

**Step 6 is implemented:** activity/resource/map/age/idle and cleanup diagnostics, administrator inspection/closure, startup-configured limits, bounded and isolated request/lifecycle queues, admission throttling, empty-session expiry, opt-in occupied expiry and sustained mixed workload validation. See INSTANCE_OPERATIONS_FIXES.md for configuration, overflow policy, 17 passing suites, the 16,200-session workload and limits of the measurements. Runtime classes are staged with backups; restart is required.

Current implementation status: **Steps 1-4 and 5A/5B/6 complete for their documented scope; 5C deliberately deferred.** Legacy Dungeoneering remains outside managed lifecycle/operational guarantees. Live client acceptance, actual restart/disconnect testing and production observation remain outstanding; earlier historical validation limits and optional feature extensions are not silently resolved by Step 6.
