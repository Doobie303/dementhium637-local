# Instance lifecycle implementation - step 2

Implemented 2026-09-08 following the owner's instruction to start step 2. Read INSTANCE_FOUNDATION_FIXES.md for the preceding map foundation. This document records the lifecycle layer; it does not mark the remaining instance milestones complete.

## Delivered

- InstanceManager and GameInstance provide ephemeral, monotonic allocation/session IDs; an explicit BUILDING -> ACTIVE -> CLOSING -> CLOSED lifecycle; capacity; player/account membership; ordinary-world return locations; built-map boundary and spawn checks.
- Creation reserves a guarded map, runs the content initializer, then activates. Runtime exceptions roll back partial NPCs, objects, drops and tasks. If cleanup cannot finish, the session stays registered as CLOSING with diagnostic failures and its reservation intact.
- Entry claims membership before teleporting. Duplicate players/accounts, occupied capacity, invalid spawns, running legacy activities and familiars are rejected. Failed or partial entry attempts undo membership after evacuation succeeds; failed evacuation remains tracked.
- Leave and close evacuate members through the normal teleport/map-refresh path. Explicit close also moves offline members back to their return location. Members already outside the instance detach without being pulled back.
- Close cancels owned tasks, evacuates members, removes owned NPCs/drops/objects, and finally releases the map. Repeated close succeeds after completion. Cleanup failures allow retry, and foreign occupants block release without being deleted.
- Content can schedule one-shot/repeating work, submit existing Tick callbacks, and spawn/remove NPCs, objects and drops through the session API. Every wrapped callback checks the instance lifetime; cancelled wrappers cannot restart.
- Ordinary NPC.java callbacks inherit ownership for explicitly adopted NPCs. This includes death/loot/removal timers. NPC removal cancels pending owned and mob-local callbacks, and queued NPCTickTask work stops when its session closes or the NPC is removed. NPCs retain an ownership tombstone so later core callbacks cannot fall back to the ordinary world scheduler.
- GroundItemManager tracks drops added inside managed maps, including normal loot and replacement stacks. Normal respawn callbacks are instance-owned. Teardown discards items without scheduling respawns.
- Object teardown removes the exact object identity/type; it cannot accidentally remove a neighbouring object type through the legacy approximate lookup.
- ActivityManager now uses stable registration IDs and identity lookup instead of mutable list positions. The first global registration remains ID 0 for existing Castle Wars checks. Unregistered/default activities use -1. Duplicate registration, recursive stop, repeated unregister, failing reset callbacks and stale callbacks from restarted activities are handled. Collection access returns an immutable snapshot.

## Thread and content contract

World.run opens/closes the mutation window and drains queued instance requests before processing ticks. Instance APIs reject off-cycle access to mutable state. The window follows each world cycle, rather than permanently binding to a thread that ServerThread may replace. Concurrent callers use InstanceManager.submit; its future reports success/failure. Cancelled requests do not execute, and one failed request does not prevent later requests. A maximum of 1,024 queued requests is drained per cycle.

Production uses InstanceManager.getSingleton(). The public scheduler-injected constructor and beginCycle/endCycle support isolated regression harnesses; content should not create a separate manager or open its own mutation window.

Example content entry, invoked from the world cycle:

    InstanceManager manager = InstanceManager.getSingleton();
    GameInstance instance = manager.create(16, 16, 1, ordinaryWorldExit, i -> {
        i.copyMap(360, 648, 0, 0, 8, 8,
            new int[] {0, 1, 2, 3}, new int[] {0, 1, 2, 3});
        i.spawnNpc(new NPC(npcId), i.location(44, 13, 0));
    });
    if (instance != null) {
        instance.enter(player, instance.location(44, 13, 0));
    }

The coordinates above are the regression map, not a production quest/minigame configuration. Content supplies suitable exit/spawn/NPC definitions. Map copies must finish before population and activation. The NPC API adopts a fresh caller-supplied NPC; rejected, unadopted NPCs remain the caller's responsibility. Existing world NPCs and familiars cannot be adopted.

From outside the world cycle, wrap the operation in manager.submit(() -> { ...; return result; }). Handle the returned future; do not block the world cycle waiting for a future whose request needs a later cycle. Creation returns null when allocation capacity is exhausted. close() returns false while cleanup needs retry; getCloseFailures() and getInstances() expose retained sessions for follow-up.

Return positions must be ordinary-world coordinates. Content is responsible for choosing a safe exit. canOccupy checks built tiles, size and solid movement flags; it is not a route finder.

## Validation

Java 8-targeted compilation passed. Eight suites passed:

- InstanceLifecycleRegression: 72 checks, including two simultaneous instances, membership/account/capacity, blocked spawns, periodic tasks, ordinary NPC death scheduling, stale queued NPC work, exact object removal, naturally created drops, respawn cancellation, partial entry failure, evacuation failure/retry, foreign occupants, creation/scheduler rollback, map reuse, production World.run request processing and activity registration lifecycle.
- InstanceFoundationRegression: 18,776 checks.
- NexIceRegression and NexMovementRegression.
- BarrowsRegression: 711,204 checks; BarrowsFaceRegression.
- EquipmentAbsorptionRegression: 238 checks.
- CombatEnhancementsRegression: 46 checks plus 10,000 Elysian trials.

Reproduce with build/instance-lifecycle/verify.ps1. Compiler/test logs, changed-source manifest and a comparison against this batch's source backups are in build/instance-lifecycle/.

Source backups: build/instance-lifecycle-before/src/.
Runtime backups and exact staged class hashes: build/instance-lifecycle-before/bin/ and build/instance-lifecycle/staged-classes.csv.
Only runtime families belonging to this batch's changed/new sources are staged. The tests neither start the server nor write player saves. No server restart is performed.

## Remaining scope

Step 3 still supplies automatic logout/disconnect/death/teleport/shutdown handling and safe login recovery. This batch provides explicit enter/leave/close APIs and a canInteract policy predicate; global interaction, movement and teleport handlers do not yet enforce instance membership. Do not open this infrastructure to ordinary players before those hooks and a content migration are ready.

Step 4 remains independent FightCavesSession runs. Existing Fight Caves retains its current static arena/session implementation. Quests/skilling proof cases and Dungeoneering/party migration remain later work.

Arbitrary content that directly schedules World ticks or creates NPCs outside the ownership API still needs migration. Bespoke NPC subclasses and combat/skill callbacks are not automatically certified merely because their ordinary NPC.java callbacks are wrapped. Persistent checkpoints, operational commands, metrics, rotated templates and procedural content remain outside this batch.

Live client rendering, multiplayer, disconnect/reconnect and shutdown acceptance still require the later hooks and a controlled content harness. No claim of a fully deployed player-facing instance system is made at step 2.

Godmode, personal XP rates, boosted Barrows loot and the Summoning-specific deferral are preserved. This batch changes resource ownership/scheduling, not historical combat formulae or rewards.

Final staging: 37 runtime classes copied and SHA-256 verified; zero mismatches. Backups and the exact file list are in the staging manifest. A normal server restart is required to load them.
