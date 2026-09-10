# Nex review and fixes — 2026-09-07

Follow-up to COMBAT_2011_REVIEW.md, authorized by the user's report of briefly being pulled outside the map while crossing Nex's room. Existing enhancements and intentional godmode behavior were preserved.

## Findings patched

- **Forced movement:** east was encoded as 4, but the supplied 639 client handles directions 0–3. Corrected east to 1. Delayed completion captures its destination and original plane and ignores superseded movements, dead/disconnected actors, or actors who have left the starting position.
- **Movement timing:** completion cannot occur before the client animation duration (30 client frames per 600 ms server tick). Pull uses 60 frames/two ticks; charge knockback uses two ticks instead of one. Completion preserves an active stun lock.
- **Pull placement:** the old one-tile offset could land inside Nex's footprint. Pull now chooses the closest unblocked perimeter tile outside her full footprint, within the arena's interior. No valid tile means no pull. Locked players cannot receive another pull; delayed damage requires the same encounter and landing position.
- **Charge:** Nex's move to the centre now emits a teleport update. Removed the premature player lock and immediate knockback unlock. Knockback checks bounds/clipping and skips immobilized players. Delayed stages belong to the original living Nex.
- **Reset:** cancels outstanding player forced movements. Old phase transitions and auto-attacks cannot apply to a replacement Nex.
- **Auto-attacks:** preserve phase and eligible targets at cast time, reject departed/dead/disconnected targets at impact, and send projectiles once at launch. Corrected the 21-target off-by-one. Shadow ranged attacks use ranged accuracy rather than magic accuracy.
- **Blood phase exit:** clears the siphon tick, healing mode and animation lock.
- **Shadow traps:** one trap per tile prevents stacked players multiplying damage. Delayed detonation belongs to the original encounter.
- **Ice prison:** all stacked prisoners are prevented from breaking their own prison. Containment creation checks its original boss and ice phase; old cleanup cannot clear a new encounter's containment.
- **Virus:** ends on death, logout, room departure or encounter replacement; spread uses room and plane checks.
- **Incoming Nex/minion hits:** the Damage-object overload delegates to the existing numeric overload for this NPC family, applying the same shields, siphon conversion, hit caps and deflect processing. Existing godmode exceptions remain.

## Verification and limits

Compiled changed source and dependencies with JDK 21, --release 8 and windows-1252 encoding. The repository contains non-UTF-8 files. Only obsolete Java-8 target warnings remain.

Ran tests/NexMovementRegression.java against the actual Mob movement implementation under Java 8. It checks east direction, two- versus three-tick timing, superseded movement ownership, departure/plane changes, death, stun preservation and encounter cancellation. It does not boot a server or bind network ports.

Inspected the local 639 client decoder and interpolation. Its long-distance teleport packet uses absolute coordinates; that encoding was left intact.

These checks do not certify a complete 2011 balance match or prove the reported visual defect is gone in the live client. No live reproduction was performed. Broader combat formula/soaking/style concerns remain in COMBAT_2011_REVIEW.md.

## In-game acceptance checks after restart

1. Cross all four lanes while receiving smoke pulls. Verify no black void, landing inside Nex, stuck movement or early snap.
2. Trigger NO ESCAPE in each direction, while running and near walls. Verify dodging, knockback and control recovery.
3. Teleport out or die during a pull/delayed attack. Verify no return teleport or subsequent room attack.
4. Reset Nex with a special pending, then start another encounter. Verify no old special affects it.
5. Stack two players on a shadow trap: each takes one explosion. Stack two inside a prison: neither self-rescues; an outside player can free them.
6. Complete phase transitions, especially during siphon; test melee/ranged/magic against Nex and minions with godmode disabled.

Compiled runtime classes are staged in bin with previous versions backed up under build/nex-review-backup. Restart normally to load them; no running server was stopped.

## Ice visual follow-up — 2026-09-07

User retesting reported missing/glitchy ice during Prison of Ice and Contain This, with spikes visible below the walkway.

Confirmed against the actual cache and arena map:
- Walkway tile (2924,5197,0) has floor decoration 57244/type 22 and zero collision. The old hasObjects check rejected it.
- Pit tile (2919,5196,0) has no object but collision 0x200000. The old check accepted it.
- Object 57262 is a non-interactive stalagmite; 57263 uses the same model and has an Attack option. Neither definition has a default object animation.

Patched the shared ice helper to allow floor decorations while rejecting blocked terrain, occupied scene objects, other planes and out-of-room tiles. Prison uses 57263 and its listener now registers that ID; containment keeps 57262. Prison also plays Nex's existing casting animation. Cleanup uses the actual ice ID and preserves floor decorations. Containment damage requires a successfully spawned spike, and its delayed creation/cleanup uses a sequence guard. Prison expiry now runs on the world scheduler so a target disconnect cannot strand the shared prison.

Validation: tests/NexIceRegression.java loads the real cache/map and exercises the actual private spawn/cleanup helpers. Passed decorated-floor placement, pit exclusion, rescue action, overlap ownership, scene preservation and collision restoration. Compiled with Java 8 compatibility. No live-client visual certification was performed.

After restart, retest both specials on each lane and the central platform, including near lane edges. Ice should appear on the walkway rather than below it. Have a second player use Attack on a prison spike to rescue the prisoner. Verify both effects disappear cleanly and the walkway remains traversable.
