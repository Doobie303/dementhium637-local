# Instance map foundation - step 1

Implemented 2026-09-08 in the active src tree. This is the first infrastructure batch, not the session manager or a migration of Fight Caves, quests, housing, or Dungeoneering.

## Completed

- MapAllocation handles reserve space immediately, with unique IDs and stale-handle rejection. Reservation/copy/release operations serialize through RegionBuilder.
- Explicit units: world tile, 8x8 chunk, 64x64 cache region, 128x128 server spatial region. Footprints round up to 128-tile cells. A 128-tile guard surrounds each footprint; both are checked against cache maps, existing dynamic maps, ordinary runtime map state, residents, and drops.
- The initial search area is bounded to tiles 2688..9984 on X and 2688..16000 on Y, including guards. Exhaustion returns null without publishing a partial reservation. This is a conservative allocation policy, not a server capacity guarantee.
- Reserved chunks, padding, and guard tiles start blocked. Copying opens only the requested chunks and planes.
- Each copied chunk snapshots the loaded source's collision and clones its objects at destination coordinates. Later source mutations do not change the copy. Normal Location object lookup, object clicks, add/remove operations and collision consult destination state.
- Source dependencies, dimensions, planes, ownership and destination bounds are checked before any destination chunk is published. Existing cache maps cannot be overwritten; dynamic-to-dynamic source copying is rejected. Adjacent source cache maps load before sampling boundary collision.
- Nonzero chunk rotation is rejected until collision, object and coordinate transforms can be verified together. Existing object orientations 0..3 are retained.
- Dynamic collision additions are counted so removing an overlapping temporary addition preserves prior flags. Unbuilt chunks cannot become walkable through object clipping writes.
- Corrected shared object footprint orientation and removal-before-addition ordering for replacements. Corrected neighboring-region lookup for local player/NPC queries.
- Removed the Dungeoneering packet fallback that fabricated missing doors from client-provided coordinates. Copied doors now use real destination-object lookup. The separate existing Barrows exception was preserved.
- DynamicMapPacket constructs/validates opcode 31 without teleporting or writing to a player. Source XTEAs use deterministic, deduplicated ordering without a fixed-size ID array.
- Dynamic entry validates before changing player location or movement state. The dynamic flag resets when returning to an ordinary map. Scene revisions refresh stationary viewers after map edits. Dynamic plane changes rebuild even when revisiting a plane.
- Dynamic rebuilds refresh ground items and replay changed object slots, including deletions. Source object changes through the shared object APIs are reflected in copied scenes.
- Release refuses a map containing players, NPCs or drops. Successful release removes its dynamic regions, object bookkeeping and empty spatial tile caches, then frees the reservation. Repeated release is harmless.
- Generic temporary-object and respawnable-item callbacks check their original map and chunk version. Rebuilt/released chunks cannot receive their old restoration callbacks; repeated item removal cannot schedule another respawn.

## Startup audit

RegionBuilder.init previously copied 100x100 chunks (800x800 tiles) from (3200,3200) to (4000,4000), then overlaid one Nex-area chunk near (4021,4024). No activity or configuration reference to these destination pairs was found in the inspected src/settings/text-data files. These automatic debug copies were removed; init remains a compatibility no-op.

The generatemap command only sends a rebuild packet; it does not own/create one of these maps.

Player previously reserved a static housing location during class initialization. That unused placeholder is now lazy, retaining its old shared-location semantics if called. This is not a housing implementation.

## API contract

New code must retain a MapAllocation handle:

    MapAllocation map = RegionBuilder.reserveMap(16, 16); // chunk dimensions
    if (map == null) {
        // Report capacity failure without changing activity/location.
        return;
    }
    try {
        RegionBuilder.copyMap(map, 360, 648, 0, 0, 8, 8,
            new int[] {0, 1, 2, 3}, new int[] {0, 1, 2, 3});
        // Copied source origin is now at map.getX(), map.getY() in tile units.
        // Admit participants only after this succeeds.
    } catch (RuntimeException failure) {
        RegionBuilder.releaseMap(map);
        throw failure;
    }

Copy offsets are chunks relative to the reservation's tile origin. Copies may expand into unused parts of the footprint. Copying over residents or drops is rejected. Evacuate entities and remove drops before release.

Map publication, object changes, and release must run on the game thread. Synchronization makes reservations atomic; it does not make other world collections safe for arbitrary background edits.

Legacy findEmptyMap now reserves rather than merely searching. Its coordinates work with legacy copy wrappers; getAllocation(x,y) retrieves its release handle. These coordinate APIs are transitional and cannot authenticate callers like retained handles. Do not use them for the new session layer. destroyMap clears mappings only; releaseMap(handle) releases the reservation. Destruction never creates an absent dynamic region.

getRegionCoords returns a snapshot: direct array mutation no longer changes maps. The old unused createDynamicRegion/setRegionCoords bypasses were removed. Use validated copy APIs.

## Validation and deployment

Compiled with Java 8 compatibility using build/instance-foundation/compile.ps1. Tests use the installed Java 8 JRE. The old XStream library could not run the complete harness under Java 21; no unrelated XStream changes were made.

Seven selected suites pass:
- InstanceFoundationRegression: real-cache mask parity on four planes; independent objects/collision; guarded/concurrent reservations; validation/rollback; all viewport packet sizes; XTEA ordering; map/plane transitions and deletion replay; occupancy/drop refusal; stale handles/callbacks; exhaustion and repeated release.
- NexIceRegression and NexMovementRegression.
- BarrowsRegression and BarrowsFaceRegression.
- EquipmentAbsorptionRegression.
- CombatEnhancementsRegression.

Exact counts/output: build/instance-foundation/*.log.
Reproduction: build/instance-foundation/verify.ps1.
Tests do not start a server or write player saves.

Source backups: build/instance-foundation-before/src/.
Runtime backups: build/instance-foundation-before/bin/.
Staged class manifest: build/instance-foundation/staged-classes.csv.
Only classes for this batch's changed/new sources, including generated inner classes, are staged. No restart is performed.

## Remaining scope and confidence limits

- This supplies map allocation/storage/packets, not instance membership, admission policies, NPC ownership, activity registration, rewards or login recovery.
- The lifecycle layer must cancel all activity-specific tasks before release. Only the two generic object/item restoration paths listed above gained map guards here. Arbitrary existing skill/minigame timers are not certified safe for reuse.
- Dungeoneering retains its dormant startup/thread, slot arrays and lifecycle problems. Its old offset-based layout needs migration to an explicit reserved footprint. Enabling it is outside this batch.
- Fight Caves still uses one shared arena and static run state. Concurrent private runs remain step 4.
- Historical cache/bridge/object data and ordinary-map collision are not comprehensively certified. Copied masks use the server's loaded source representation; packed source flags do not retain separate provenance for every overlapping original object. This batch verifies parity and new overlay accounting, not a replacement landscape decoder.
- Source state is snapshotted when copying. An immutable template cache, procedural generation, rotated chunks, parties, persistent checkpoints, metrics and operational commands remain later work.
- Client rendering and live multiplayer acceptance remain unverified. Automated packet checks validate server output, not the client renderer.

## Next milestones

Step 2: stable instance IDs, members/boundaries, creation rollback, tracked NPCs/objects/drops/tasks, and idempotent close. Repair ActivityManager's unstable list-index IDs when integrating it.

Step 3: logout/death/teleports/shutdown and safe login recovery.
Step 4: independent FightCavesSession runs.
Then quest/skilling proof cases and Dungeoneering/party migration.

## Live acceptance after restart, using a controlled instance harness

1. Enter separate copies; inspect floors, walls, objects and map edges.
2. Add/remove a door/resource in one copy. Verify walking/projectile collision and independence from the other copy/source.
3. Expand a room while standing still; revisit planes. Deleted objects and drops must refresh correctly.
4. Return to the ordinary world and verify scene/object/drop reload.
5. Evacuate and release a copy, reuse its allocation, and verify no prior restoration callback appears.

Godmode, personal XP rates, private Barrows rewards and the Summoning-specific deferral are preserved.

Final verification: InstanceFoundationRegression passed 18,776 checks on Java 8. All seven suites passed. Runtime staging completed with SHA-256 verification; see staged-classes.csv for the exact file list and backups.
