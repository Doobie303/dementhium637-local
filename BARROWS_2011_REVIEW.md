# Barrows review against 2011 RuneScape

Review date: 2026-09-07. Review only: no gameplay code or runtime classes changed.

## Assessment and baseline

The basic six-brother activity exists, but it is not currently a faithful 2011 implementation. The largest gaps are brother combat effects, prayer drain, tunnel routing/puzzles, rewards and run lifecycle. These need functional changes, not just adjusted drop rates.

Use early-2011/six-brother Barrows as the provisional baseline for this 637 project. This is a review assumption, not a final user-selected date. Later-2011 support needs a separate decision about Akrisae and the Ritual of the Mahjarrat quest gate; do not import later RS3 or modern OSRS additions by default.

Sources:
- [Period-style Barrows Knowledge Base mirror](https://www.2011.rs/kb/barrows). This is a third-party reproduction, not an authenticated dated Jagex archive. It describes crypt entry, random tunnel doors, four pattern puzzles, haunting/prayer loss, six distinct brother effects and the old reward categories. Treat its exact wording and provenance cautiously.
- [Jagex drop-rate explanation](https://www.runescape.com/drop-rates). Primary source, but current and explicitly includes later content. Useful for distinguishing brother rolls from tunnel-monster reward potential; not proof that every numeric value applied in 2011.
- [Ritual of the Mahjarrat release history](https://runescape.wiki/w/Ritual_of_the_mahjarrat). Secondary chronology places the quest on 14 September 2011. Exact historical additions, luck-ring rules and reward probabilities should be verified against a dated baseline before implementation.

## Existing foundations worth retaining

- Six hill/crypt mappings, spade entry, stairs, sarcophagus searches.
- Random missing-brother crypt and stored tunnel configuration.
- Owner-restricted brother combat and duplicate-spawn checks.
- Tracking of killed brothers, total kills and tunnel identity, including player serialization.
- Chest-triggered tunnel brother and occasional door-triggered brother spawns.
- Rune/coin/bolt-rack rewards and all 24 original equipment items.
- Reward interface and inventory transfer on closing; logout/shutdown also flush interface items. Do not misdiagnose normal logout as guaranteed reward loss.
- Minimap blackout, kill overlay and a post-loot camera shake.

## Prioritized findings

### 1. Brother signature combat is missing — critical fidelity gap

BarrowsCrypt.duplicate creates ordinary NPC instances. NPC.getCombatAction (around line 1264) dispatches the generic melee, ranged or magic action. BarrowBrother.java is an empty subclass and is not used by the crypt duplication path.

Searches through the active NPC, damage and combat paths found no brother-specific NPC handling for Dharok's low-health damage scaling, Guthan's healing, Verac's protection/defence bypass, Torag's run-energy effect, Karil's Agility reduction, or Ahrim's stat effects/spell selection. The Dharok modifier in MeleeFormulae around line 124 requires a player wearing the set; it does not implement the NPC.

Implement explicit brother combat profiles and verify proc conditions, prayer interactions, attack speed, animations, projectiles and hit limits. Do not use modern player-equipment set changes as the specification for the brothers.

### 2. Haunting does not drain prayer — confirmed

PlayerAreaTick.java:185-200 decrements a timer and sends an overlay/BConfig. There is no prayer reduction in the active branch.

The face calculation also uses stored NPC IDs directly: 4761 + (2025 * 2), for example, instead of a brother index. This produces 8811 before the plane adjustment. The exact correct graphic/config mapping needs client verification.

Restore actual prayer loss, a verified timer and valid face mappings, and verify behavior both before and after brother kills. Do not copy a modern prayer-drain immunity item.

### 3. Door keys collide severely — confirmed by deterministic check

BarrowsTunnels.java:33 and BarrowsActivity.java's gate lookup use:
    id << 16 | x << 14 | y << 12

These overlapping bit ranges are not a unique key. Evaluating the 64 actual Gate entries produces only 21 distinct keys. For example, 6749 at (3558,9677) and 6748 at (3541,9677) both yield 461230080.

Map insertion overwrites entries. A clicked door can therefore resolve to a different gate/replacement position. Replace this with an unambiguous object/coordinate key and test all 64 entries.

### 4. Tunnel restrictions and puzzles are incomplete — confirmed

BarrowsTunnels computes closed gates, but BarrowsActivity's gate.isClosed check is commented out. No active Barrows pattern-puzzle implementation was found in the activity or interface handlers.

The intended random route is therefore not enforced. Implement valid connected routes, the permitted central entrance, puzzle presentation/answers and incorrect-answer reshuffling. Test all configurations for reachability and no unintended chest access.

Gate movement also uses an unclipped requestWalk and sends the door changes to nearby players even though tunnel configuration is per player. The source itself warns about wrong-door movement. Audit per-player scene state and crossing validation as part of the same fix.

### 5. Rewards use a custom algorithm — confirmed

BarrowsActivity.java:300 onward generates each common item independently with 60% probability and fixed amount ranges. Total killcount boosts the equipment roll. Equipment selection is uniform across all 24 items, regardless of which brothers were killed; at most one equipment piece is added.

With exactly six brothers killed and total killcount six:
    chance = 2 + round(6 / 1.5) + 6 * 4 = 30
    nextInt(110) <= 30 gives 31/110 = 28.18% equipment probability.

At total killcount 21 with six brothers, this becomes 41/110 = 37.27%. The extra random condition can exceed the apparent cap at higher killcounts.

This tightly links killing ordinary monsters to equipment rewards. Rebuild around separate brother eligibility/rolls and ordinary reward potential. Establish dated numeric probabilities before claiming exact 2011 odds. A single tunable percentage is not enough.

RARE_REWARDS contains dragon med helm and key halves but has no active consumer, so these declared rewards are unreachable through the current chest logic. Common reward categories also have no potential thresholds.

### 6. Run reset is split across incompatible states — confirmed source risk

After looting, BarrowsActivity resets tunnel IDs to -1 and clears killed lists, but retains the current activity's crypt instances, their isSpawned flags, entrance designation and BarrowsTunnels object. initializeActivity only rebuilds these when a fresh activity starts.

Consequences to test:
- Starting another run without leaving the whole Barrows zone.
- Previously spawned/dead brothers remaining unavailable.
- A tunnel NPC kill after looting: NPC.java around line 376 indexes activity entities using the stored tunnelEntranceId, which has just become -1.
- Repeated chest clicks, teleports, death and logout at each stage.

Use one run state with explicit reset/start/looted states and idempotent chest reward assignment. Guard all persisted indices. Calling instantDeath repeatedly is not a substitute for a complete run reset.

### 7. Entry, exit and collapse are simplified

Tunnel entry always teleports to (3568,9712,0). Rope exit always teleports to (3565,3307,0), regardless of the selected crypt. Reconcile this with the selected 2011 entrance/return behavior.

Post-loot PlayerAreaTick sends a camera shake and immediately returns; it does not implement a falling-debris damage cycle. Restore the intended escape sequence after confirming the historical timing/damage, without interfering with teleport escape.

### 8. Brother ownership and kill accounting need defensive checks

NPC death handling credits brothers based on ID and player location/activity. It does not independently assert the dead NPC's barrowsOwner matches the credited player, and it appends IDs without checking uniqueness. Ordinary tunnel NPC kills are counted indiscriminately rather than accumulating verified reward potential for eligible monsters.

Existing attack ownership restrictions help, but kill accounting should remain correct under familiars, reflected damage, multiple players and cleanup. Store a unique killed-brother set and validate owner/run identity.

### 9. Data and integration remain uncertified

Inspected the source profile for Dharok (NDE/data/NPCs/NPCDefinition2026.xml), including 1000 life points and combat flags. The runtime loads NDE/NPCDefinitions.bin, so XML alone is not proof of active stats. A full six-brother binary/profile comparison is still needed before certifying stats, defences, attack speeds and visuals.

Check poison/freeze behavior, all tunnel monster spawns/aggression, familiar restrictions, dangerous death handling, reward overflow, and crash recovery with a live client. Shared accuracy, prayer, soaking and reflection issues from COMBAT_2011_REVIEW.md can also affect Barrows after encounter-specific repairs.

## Suggested implementation order

1. Fix door identity and run lifecycle safety.
2. Restore prayer drain, tunnel locks/puzzles and correct entrance/exit state.
3. Implement all six brother combat profiles, coordinating with shared damage fixes.
4. Replace reward generation using a dated specification and deterministic tests.
5. Validate cache data, visuals, multiplayer isolation, save/resume and full runs.

## Acceptance checks

- All six crypts spawn only the correct owner-bound brother, with exactly one tunnel crypt.
- Each brother demonstrates its specific effect, including protection interactions.
- Prayer haunting uses valid faces and measurable loss at the verified cadence.
- Every route has a reachable intended chest door; wrong puzzle answers reroute correctly.
- Killing tunnel creatures changes common reward potential, not an ad-hoc equipment chance.
- Reward simulations cover zero through six brothers, eligible item pools, multiple successful rolls and inventory overflow.
- Repeated runs work without relogging; kills after loot cannot index -1.
- Logout/death/teleport during fights, door movement and chest interaction neither duplicate rewards nor strand the run.
- Two players with different tunnel layouts do not corrupt one another's door scene.

## Review limits

This was a source review plus a deterministic gate-key calculation, with historical-source comparison. No live Barrows run or statistical reward simulation was performed. No production source or runtime classes were changed. Findings are not a claim of a complete proprietary 2011 specification.
