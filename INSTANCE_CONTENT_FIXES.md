# Instance content examples - Step 5A

Implemented 2026-09-08 in the active src tree, following authorization to resume Step 5 in three batches. This completes the first batch's reusable templates and private quest/skilling proofs. It does not complete all of Step 5.

## Agreed breakdown and current position

1. **5A: reusable templates and small content examples - implemented and tested.**
2. **5B: party rules, active room expansion and activity-specific death/respawn - pending.**
3. **5C: Dungeoneering migration - pending, after 5B.**

Step 6 operational diagnostics, configured limits and sustained load testing follows these. Live client acceptance remains necessary. The client editing investigation is saved in CLIENT_EDITABILITY_REVIEW.md and was not resumed here.

## Implemented

### Template API

InstanceTemplate is an immutable definition for an unrotated rectangular map copy, with an identifier, explicit source policy, source/destination plane and named local tile anchors. Constructors validate dimensions, coordinate limits and anchor bounds/planes; the anchor map is defensively copied and exposed read-only. Map dimensions/source coordinates are chunks; anchors are tiles.

InstanceTemplate.create delegates reservation, population and rollback to InstanceManager. GameInstance.location(name) translates a template anchor into its allocated map; toLocal(location) checks that a world position belongs to the built instance before returning local tile coordinates. Existing coordinate-based creation and Fight Caves behavior remain compatible. This batch does not add active room expansion or rotated chunks.

The supported policy is explicitly SNAPSHOT_LOADED_WORLD. As before, the surrounding map snapshots currently loaded collision and objects, including temporary source changes. This is not a pristine immutable cache baseline. Each example seeds its own fresh objects/resources after copying. The regression deliberately depletes a source resource slot and proves two new skilling copies both start with their own fresh trees without changing the source. Other source changes remain intentional snapshots; a blocked entry causes creation to roll back.

### Quest scene

InstanceExamples provides a solo private scene with a crate, chest and exit portal. Search the crate to obtain a session-local clue, then open the chest to complete the scene. Completion awards one coin exactly once per live run. A full inventory without room for that reward preserves the claim; an existing coin stack can accept it with no free slot. Repeated clicks, foreign callers and forged/stale object identities cannot award again.

Progress is deliberately ephemeral: there is no save-layout change, quest catalogue registration or persistent checkpoint. A new run starts from the beginning and can earn its own one-coin test reward. This is an administrator-accessible integration example, not a newly released public quest or durable reward transaction.

### Skilling room

A separate solo room has a fresh owned tree and exit portal. A first-option tree click starts one three-cycle harvest. If the player is still alive, connected, in the same membership and at the starting position when it finishes, a successful inventory insertion grants one log and 25 base Woodcutting XP through the existing XP modifier API. It replaces that room's tree with a stump, then regrows after eight cycles. Repeated clicks cannot queue multiple harvests; a full inventory grants nothing and leaves the tree intact.

The room is a deterministic resource-lifecycle proof; it does not change ordinary Woodcutting, certify historical chopping probabilities or require an axe/level progression system. The animation uses existing client assets. Movement to another position before completion prevents the harvest; this is a completion-position check, not a recorded history of every intervening movement.

Harvest and regrowth timers belong to the instance. Object replacement and regrowth failures close the session. Leaving, death, disconnect or shutdown cancels owned callbacks, and retained callbacks cannot affect later runs or reused maps. The default SAFE_RETURN death policy preserves inventory and restores the player at the ordinary-world return location.

### Entry and interaction

Administrator commands (rights >= 2):

- `::instanceexample quest`
- `::instanceexample skill`
- `::instanceexample exit`

Entry records the administrator's current ordinary-world location as the return position. Existing activity/familiar/membership and capacity restrictions still apply. Repeating entry keeps the current example; leave first to switch kinds. Off-cycle command requests go through the existing manager queue.

ObjectPacketHandler detects an example after instance/identity validation, checks movement permission, routes the click and dispatches a guarded coordinate callback to the owning example. These example object clicks are consumed before ordinary object scripts. Other activities' object handling remains unchanged. The content handler independently checks caller, current session, object identity, plane and proximity. Only first-option interactions perform example actions.

No player fields or save format were added. The session reference is a temporary player attribute removed by the existing departure callback, which also closes the empty solo session. All new resource timers use the owning GameInstance scheduler. Ordinary player movement/area-event callbacks retain the existing revision guards.

## Validation

Java 8-targeted compilation and **all 14 suites passed**. Reproduce with `build/instance-content/verify.ps1`.

- InstanceContentRegression: **118 checks**. Includes immutable template configuration, bounds/planes, named coordinate round trips, foreign coordinates, initializer resource rollback, administrator admission, concurrent quests and skilling rooms, required clue order, duplicate/full-inventory rewards, source snapshot policy and fresh node seeding, timed depletion/regrowth, source object/collision isolation, duplicate harvest clicks, XP modifier preservation, movement/full-inventory cancellation, stale callbacks/reentry, portal/direct exit, save-position projection, disconnect, safe death, scheduler failure and shutdown.
- Actual object-option packets for crate, chest, portal and tree run through ObjectPacketHandler, pathfinding, walking and guarded area events using simulated player connections. The harness processes the post-teleport movement-unlock tick before clicks.
- FightCavesInstanceRegression: 414; InstanceIntegrationRegression: 53; InstanceLifecycleRegression: 73; InstanceFoundationRegression: 18,776.
- The other nine suites cover Nex ice/movement, Barrows (711,204 checks) and face timing, equipment absorption (238), combat enhancements (46 plus 10,000 shield trials), combat foundation (117), combat formula (90 plus 180,000 attacks), and combat balance (48 plus 200,000 samples).

Tests read the real cache, use isolated session schedulers and simulated players, and mutate test-process world state only. They do not start the server, write account saves or modify client/cache files. This validates server behavior and outgoing updates, not rendered maps, visible animations or live networking. The separate client packet suite was not rerun because this batch does not change its encoder or decoder.

## Runtime staging and rollback

**26 runtime classes were copied to bin and SHA-256 verified. No server restart was performed.**

- Build scripts, compiler/test logs: `build/instance-content/`.
- Exact source families: `build/instance-content/changed-sources.txt`.
- Prior versions of changed existing sources and reports: `build/instance-content-before/`.
- Prior runtime classes: `build/instance-content-before/bin/`.
- Exact staged files, prior existence, backups and hashes: `build/instance-content/staged-classes.csv`.
- Review of edits against this batch's backups: `build/instance-content/source-review.diff`; the two new source files are listed separately in that review.

Rollback only the listed source/class families, restoring prior files where present and removing this batch's new files where no previous counterpart existed. Preserve unrelated local work and earlier instance/combat changes. The stage script refuses to overwrite an existing backup.

## Live acceptance after restart

Use two administrator accounts from safe ordinary-world locations. Enter separate quest copies; search the crate/open the chest, verify isolation and one reward, then use the portal. Repeat with a full inventory. In separate skilling copies, observe chopping, log/XP delivery, stump replacement and regrowth; verify one room's resource state does not affect the other. Leave/reenter during harvest and regrowth, check disconnect/reconnect and safe death, and verify the ordinary return map loads correctly.

Godmode, personal XP rates, boosted Barrows rewards and the Summoning-specific deferral are preserved. No Dungeoneering entry point was enabled or migrated in 5A.

## Live acceptance fix: searchable quest crate - 2026-09-08

The owner reported that the clue crate offered only Examine in-game. The example used decorative cache object 354, whose five action slots are all null. The original simulated first-option packets bypassed the client menu and therefore did not establish that a real client could initiate the quest action.

Replaced only the example's clue object with existing cache object 1: Crate, first action Search, 1x1 footprint, no transforms. The chest (375: Open), portal (2465: Enter) and tree (1276: Chop down) also have the expected first action in the cache. No client or cache edit is required; the ordinary source-map objects are unchanged.

InstanceContentRegression now checks the expected first menu action on each example's actual spawned objects, in addition to its existing packet/path routing and lifecycle/reward coverage. This new assertion failed against the old crate (`clue object 354 must expose first-option Search`) and passed after replacement. The targeted suite passes 144 checks. Reproduce with build/instance-menu-fix/verify.ps1; before/after logs and backups are under build/instance-menu-fix and build/instance-menu-fix-before.

Three InstanceExamples runtime class-family files were staged and SHA-256 verified with prior versions backed up. Restart the server, then create a fresh room with ::instanceexample quest and verify Search in the real client. No restart, client launch or account-save write was performed by the fix. Other live acceptance remains outstanding; the 17-suite Step 6 result is historical and was not presented as a rerun of this small content fix.
