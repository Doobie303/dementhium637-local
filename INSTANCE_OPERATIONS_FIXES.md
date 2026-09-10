# Managed instance operations - Step 6

Implemented 2026-09-08. The owner explicitly deferred Step 5C (Dungeoneering migration) until its gameplay overhaul and authorized Step 6. This report supersedes the operational-pending statements in INSTANCE_PLAN_STATUS.md only for the work listed here.

## Behavior and administration

- `::instances` or `::instances stats`: managed sessions/members, bounded queue depths, reserved and built map usage, NPC/object/drop/task totals, creation/closure/expiry/rejection/overflow counters, recent world-cycle timing, JVM heap and allocator totals.
- `::instances list [page]`: eight sessions per page, with activity ID, state, population/resources, age and idle time.
- `::instances inspect <id>`: reservation location and dimensions, built chunk-planes, resources, ordinary exit, death policy, party leader/members and their return points, cleanup reason/attempt count/age and individual failures.
- `::instances close <id>`: normal owned-resource cancellation and safe evacuation. Failed cleanup stays quarantined and is retried; a failed evacuation never releases an occupied allocation. Administrator closes are printed to the server console.
- `::instances recent`: the last 32 successful closures, including original reasons. This bounded history stores strings, not sessions or players.

Commands independently require administrator rights, and mutations run in the world cycle. IDs are runtime IDs, not persistent checkpoint IDs. Fight Caves, proof quest/skilling templates and the party example have explicit activity IDs. New custom initializers can call `setActivity()`.

World-cycle timing samples cover `beginCycle()` through `endCycle()`, including request draining and maintenance as well as normal World work. The rolling sample buffer holds at most 512 completed cycles. These diagnostics are populated by the running server after restart; the separately measured load-harness times below are not actual World.run timings.

## Startup configuration

`settings/instances.properties` is read once at startup. Restart after changing it. Unknown keys, malformed integers and out-of-range values fail configuration loading; omitted settings have defaults. The singleton uses conservative throttles even if the optional file is absent. The injectable test-manager constructor uses higher default per-cycle/admission throttles for existing controlled-cycle regressions; tests of production capacity and mixed load explicitly load the production file.

| Limit | Default |
|---|---:|
| Managed sessions / admitted members | 32 / 128 |
| Members per session | 16 |
| Reserved chunk-planes, total / per session | 16,384 / 4,096 |
| Reserved 64-tile cache-region cells, including guard space | 2,048 |
| NPCs, total / per session | 2,048 / 128 |
| Owned dynamic objects, total / per session | 8,192 / 1,024 |
| Ground drops, total / per session | 8,192 / 512 |
| Owned timers, total / per session | 16,384 / 512 |
| General queued requests / drained per cycle | 1,024 / 128 |
| Pending logins / drained per cycle | 1,024 / 256 |
| Pending logouts / drained per cycle | 4,096 fixed / 256 |
| Creation attempts / build chunk-planes per cycle | 4 / 512 |
| Admissions per cycle | 32 |
| Admissions per account per 60-second window | 8 |
| Retained account throttle records | 4,096 |
| Empty-session expiry | 60 seconds |
| Occupied idle expiry | Disabled (`idleSeconds=0`) |

Reservation budgets charge every possible plane of the footprint up front, including unbuilt space. Built chunk-planes are also counted separately for inspection. Guard-region accounting is rounded to the allocator's actual reservation shape. Copied cache objects are bounded through map budgets; the separate object quota counts objects owned through the dynamic object API. Large reservations can be populated through smaller initial rectangles and later append-only room builds; a single copy/append cannot exceed the per-cycle work budget.

Creation/build attempts consume the current-cycle budget even if later validation fails. Admission attempts consume their account window once shared admission checks pass, before the entry teleport. Expired account records are pruned, and a full record table rejects new identities until records expire. Counts include quarantined sessions/resources until cleanup actually releases them.

Login/logout work has separate bounded queues, coalesced by Player identity within each lane. Logout runs before login and general instance requests. The logout reserve covers both 2,048-entry world/lobby lists and cannot be consumed by logins. A rejected or failed lifecycle request closes its connection; the World loop retries registered disconnected players. Registration now skips disconnected connections. Shutdown resolves queued futures and closes managed sessions. Ordinary lifecycle tests use harmless callbacks; they do not invoke account persistence.

## Expiry and overflow policy

Empty sessions, including successfully initialized sessions that never admitted a player, expire after 60 seconds. The empty timer restarts when the last member leaves. Existing content can still close immediately on departure. Disconnected members are evacuated during maintenance; failed cleanup remains visible and retried.

Occupied idle expiry is deliberately disabled so an extended Fight Caves run cannot be ended just because a generic timeout elapsed. If enabled, it uses monotonic time since entry, player location updates or valid same-instance interaction checks (including combat involving a player). It is an activity heuristic, not a security boundary or exact input-idle detector. Content can explicitly report activity with `GameInstance.touch()`. Keep it disabled unless custom content has appropriate activity reporting; repeating timers and connection pings alone do not refresh it.

Resource limits reject before publication and preserve initializer rollback. A full managed ground-drop quota is handled differently because reward/inventory overflow paths must preserve the item: the new drop is created at its owning member's ordinary return point, or the session exit if no member owns it. Amount, owner, public/private visibility and administrator restriction are retained, and the owner receives a message. Ordinary ground-item visibility and expiry still apply, so the player must retrieve the item there. A respawnable overflow is a one-time ordinary drop, not a new respawning source outside the instance. This is not a mailbox, bank deposit or durable delivery guarantee. Normal-world item totals remain outside managed quotas.

## Validation

Java compilation targets Java 8, using the Java 21 compiler with `--release 8`. Runtime tests use Java 8 with a 512 MiB heap. All 17 regression/load suites passed; after the final built-map diagnostic additions, the operational suite was rerun and passed 123 checks.

The new operational suite exercises concurrent saturation (800 requests from eight threads into a 16-slot queue), login/logout isolation and priority, coalescing, cancellation, action failure, shutdown including shutdown during drain, resource/creation/build/admission limits, failure rollback, preserved overflow loot, empty/idle expiry, movement activity, cleanup quarantine/retry, bounded closure history and administrator authorization. Production limits were reached with 32 simultaneous real-cache sessions and 128 simulated members (1,152 guarded cache-region cells); session 33 and member 129 were rejected cleanly. Administrator closure at capacity freed a slot and allowed immediate reuse.

The sustained mixed-content harness passed **73,576 checks**:

- **16,200 sessions**, including 200 warmup sessions; 2,000 measured batches of eight concurrent sessions and ten simulated members.
- **46,000 measured simulated cycles**, without real-time sleeps between cycles. This was approximately **28.88 seconds of measured wall time**, not a multi-hour live soak test.
- Quest progression/rewards, skilling harvest/regrowth, invitation admission, room expansion, party death/internal respawn, leader disconnect/promotion, early Fight Caves wave progression/splits, owned drops, packet production and repeated close/reuse.
- Allocations, managed membership/resources, world NPCs/drops, owned schedulers and content-player references returned to baseline after every batch.
- Measured cycle **p50 0.008 ms, p95 4.746 ms, p99 6.049 ms, maximum 10.750 ms**.
- Retained heap after forced test-only GC: **40.14 -> 40.27 MiB**. Timing storage is a fixed preallocated array; eight intermediate memory samples are recorded. This supports bounded behavior for this workload, not a proof of absence of every leak.
- **3,981,822 generated packets** collected and discarded by simulated connections. Peak mixed-workload resources: 6 NPCs, 18 owned timers, 10 owned objects, 2 drops and 288 guarded cache-region cells.

The harness invokes real map/content/death/timer and player-update paths, but does not run the entire World executor, real network sockets, client rendering, full combat scheduling or file saves. These timings do not certify live player capacity, slow-host behavior or a production tick-time service level. Existing Fight Caves coverage separately checks all 63 waves through controlled death callbacks.

Other passing suites: InstancePartyRegression (122), InstanceContentRegression (118), FightCavesInstanceRegression (414), InstanceIntegrationRegression (53), InstanceLifecycleRegression (73), InstanceFoundationRegression (18,776), NexIceRegression, NexMovementRegression, BarrowsRegression (711,204), BarrowsFaceRegression, EquipmentAbsorptionRegression (238), CombatEnhancementsRegression (46 plus 10,000 shield trials), CombatFoundationRegression (117), CombatFormulaRegression (90 plus 180,000 attacks) and CombatBalanceRegression (48 plus 200,000 samples).

Reproduce with `build/instance-operations/verify.ps1`. Compilation and each suite have separate logs in that directory. Raw load metrics are in `load-memory.csv` and `load-cycle-nanos.csv`.

## Runtime staging and remaining acceptance

Source backups are under `build/instance-operations-before`. The staging script backs up existing runtime classes before copying only the class families listed in `build/instance-operations/changed-sources.txt`; its hash-verified manifest is `build/instance-operations/staged-classes.csv`.

A normal server restart is required to load the staged classes and configuration. No server/client was launched or restarted, and no existing account save was edited by this work.

Step 5C is explicitly deferred: legacy Dungeoneering does not inherit managed quotas, lifecycle guarantees or instance-admin closure, although allocator-wide diagnostic totals include its reservations. The party/room/respawn APIs remain available for its later overhaul. Durable checkpoints/reward transactions, pristine source snapshots, rotations and familiar support remain outside this batch; preserve the Summoning deferral and intentional server customizations.

Remaining release acceptance: restart normally, inspect the production counters, test two real clients entering/leaving independent scenes and viewing active room expansion, exercise actual disconnect/reconnect and orderly restart, then observe live cycle/memory behavior under representative population. The server-side implementation is complete for the agreed managed scope; client rendering and end-to-end operational acceptance remain unverified.

Staging completed: 32 runtime classes copied and SHA-256 verified, with every existing version backed up before replacement. The source manifest is build/instance-operations/source-manifest.csv.
