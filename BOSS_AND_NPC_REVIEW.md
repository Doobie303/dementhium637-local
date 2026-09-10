# Boss encounters and shared NPC behavior review

Review date: 2026-09-09. Review only; no gameplay source, runtime classes, definitions, spawns, clients or accounts changed. Early-2011/revision-637 behavior is the provisional historical target. Exact date remains undecided. Preserve custom rewards/access policy, godmode/personal XP, and Summoning/Dungeoneering deferrals.

## Owner-approved baseline - 2026-09-09

The owner agreed to item 1 of the recommended work order: use early-2011 combat provisionally and retain all existing private-server access conveniences. The focus is accurate boss fights and combat stats: attack styles, damage, accuracy, timing, prayers, phases, targeting, adds and encounter resets. Existing teleports, convenient entrances and bypassed access requirements must remain; do not restore historical quest, travel, equipment or killcount entry restrictions as part of this work. Boss combat stats are candidates for historical correction, not automatically protected custom values. Preserve existing rewards, godmode/personal XP and Summoning/Dungeoneering deferrals. Exact historical date and intended extra boss populations remain unresolved. This approves the baseline only; implementation of the remaining work list has not yet been requested.

## Overall assessment

The encounter roster is substantially less complete than its spawn list suggests. Nex, Fight Caves/Jad and Barrows have the strongest dedicated implementations. Corp and the original God Wars bosses require substantial corrections; Kalphite Queen and Dagannoth Kings are particularly incomplete. Several problems are directly demonstrable from dispatch/data, without guessing historical probabilities.

This review traced the active `src` loader, custom-handler registry, ordinary spawn file, boss actions, generic combat selection, NPC ticking, movement, damage and respawn paths. An independent read-only decoder walked all 13,488 sequential slots and exactly 63,012 bytes of the actual NDE binary, checking record IDs against slot indices. It joined 3,892 active spawn lines to names, handlers and definitions, respecting the loader's block-comment behavior. Dynamic Nex, Barrows and Fight Caves spawns were inspected separately.

Evidence is saved in [inventory.ps1](<C:/Users/Tcarn/Desktop/Dementhium 637/build/boss-npc-review/inventory.ps1>), [active-spawns.csv](<C:/Users/Tcarn/Desktop/Dementhium 637/build/boss-npc-review/active-spawns.csv>), [packed-definitions.csv](<C:/Users/Tcarn/Desktop/Dementhium 637/build/boss-npc-review/packed-definitions.csv>) and input-hashes.csv in the same directory. These are disk configuration/source observations, not an inventory of NPCs presently alive in the running JVM. No live fights or gameplay regression suites were run for this review. Loot-table completeness, every map entrance and all cached animation assets are not certified.

## Encounter inventory

Coordinates are x,y,plane. Ratings describe implementation coverage, not measured completion percentages.

| Encounter | Current configured location | Assessment | Work needed during the fight |
| --- | --- | --- | --- |
| Corporeal Beast, 8133 | Two spawns: 2942,4384,2 and 2988,4384,2 | Major reconstruction | Dark core, ground/splash attack, weapon-damage rules, correct melee reach, targeted magic/stat effects, attack-owned state and reset-safe timers. Verify whether both spawns are intentional. |
| General Graardor, 6260 | Bandos room, 2865,5356,2; three sergeants present | Major defects | Restore damage to ordinary attacks; correct ranged formula and room-wide attack coverage; stable tank targeting, minion behavior and group reset. |
| Commander Zilyana, 6247 | Saradomin room, 2899,5265,0; three followers present | Partial/custom | Reconstruct fast melee/lightning cadence, nearby-player coverage and pursuit. Existing magic-stat drain/delayed blast is a custom approximation. |
| K'ril Tsutsaroth, 6203 | Zamorak room, 2925,5322,2; three followers present | Partial/custom | Implement/verify poison and the prayer-punishing attack, correct melee reach and magic selection. Existing magic-stat drain/delayed blast does not provide those mechanics. |
| Kree'arra, 6222 | Armadyl room, 2832,5302,2; three followers present | Generic ranged combat | Add encounter-specific style selection, multi-target attacks and movement/knockback rules. Preserve existing player-melee prohibition. Verify all follower projectiles. |
| Nex, 13447-13450 | Dynamic Ancient Prison encounter; center 2925,5203,0 | Substantial implementation | Preserve phases/minion gates, virus, drag/charge, shadows, siphon/reavers, sacrifice, ice, final phase and reset fixes. Audit actual-damage healing/status callbacks and remaining historical values; conduct a full ordinary-gear run. |
| Kalphite Queen, 1158 | Hive, 3474,9495,0 | Incomplete encounter | Add owned first-to-second-form transition (1160), phase defences/protection semantics, correct styles/area attacks, final-form-only kill/reward and clean reset. |
| Dagannoth Supreme, 2881 | 2848,4400,0 twice; 2903,4447,0 twice | Critical data/placement gap | Supply real combat data, ranged identity, correct arena positions and intended population. |
| Dagannoth Prime, 2882 | 2915,4449,0 twice; 2911,4449,0; 2912,4449,0 | Critical data/placement gap | Supply real combat data, magic identity, attack reach and arena targeting. |
| Dagannoth Rex, 2883 | 2923,4438,0 twice; 2911,4449,0; 2914,4449,0 | Critical data/placement gap | Supply real combat data, melee identity, legitimate rock safespots and spawn control. |
| King Black Dragon, 50 | Lair, 2273,4695,0 | Recognizable but needs corrections | Four breaths and melee exist; move poison/freeze/stat effects from launch to validated impact, calibrate protection and effects, improve target selection. |
| Chaos Elemental, 3200 | Wilderness/Rogues' Castle vicinity, 3255,3921,0 | Recognizable but needs verification | Mixed damage, teleport and disarm exist. Validate attack state, safe landing, equipment metadata/actual slot and full-inventory behavior; improve targeting. |
| Tormented demons, 8349/8353/8357/8361 | 2595,5727,0; 2612,5729,0; 2601,5744,0; 2600,5711,0 | Substantial outline, significant defects | Fix shield/protection arithmetic, damage-based switching at impact, Darklight interaction, grounded AoE footprint and death/reset ownership. |
| TzTok-Jad and Fight Caves | Individual dynamic caves; entry area near 2438,5168,0 | Substantial implementation | Preserve current waves, personal maps, tells, impact-time prayer and healers. Verify exact tell/impact cadence, melee choice and healer luring in live fights. |
| Six Barrows brothers | Barrows crypts/tunnels near 3565,3305 and 3551,9694, plane 0 | Substantial implementation | Preserve all six effects, owner checks, maze and rewards. Remaining proc/formula calibration and live timing checks, not a fresh implementation. |
| Frost dragons, 51 (adjacent PvM coverage) | 19 spawns around x1293-1333,y4488-4534,plane 0 | Handler exists but is not wired | Register correct behavior only after checking source/assets; current spawns use generic magic and have -1 projectile data. Decide whether later-2011 mechanics belong in the selected baseline. |

No configured encounter was found for Giant Mole, Bork, combat Nomad, Barrelchest, Phoenix, Glacors or Balance Elemental in the inspected active-spawn/handler/source search. Cache models/names, quest references or weapon names do not establish a working encounter. This is not a claim that every historical quest boss was individually audited. Dungeoneering boss/content overhaul remains explicitly deferred. Ordinary dragons and combat NPCs share the behavior issues below; they are not all separate bosses.

The boss teleport dialogue provides KBD, Nex, KQ, tormented demons, Chaos Elemental and the central God Wars destination. Spawn availability alone does not certify walkable access; all entrances need live acceptance if access is changed. The extra text such as `godwars.KreeArra` and `KalphiteQueen` on spawn lines is ignored by the active loader: custom dispatch comes from custom_npcs.xml.

## Confirmed high-priority findings

1. **Dagannoth Kings have no packed combat records.** All three slots contain -1. NPCDefinition supplies 100 LP, zero stats/speed and false aggression/style flags; generic NPC combat then defaults to melee. Their cache combat level can still make them clickable/attackable. Twelve active spawn lines create four of each king, with exact-position duplicates. Fix both data and spawn intent before balance work. The XML editor files are not authoritative while NPCDefinitions.bin exists; the packer returns without rebuilding it.

2. **Graardor's usual branch deals no damage.** BandosAction creates Damage only for `Style.RANGE`; PRIMARY just animates. Selection gives PRIMARY two-thirds probability, MAGIC one-sixth and RANGE one-sixth. The MAGIC branch schedules a single-victim ranged hit; RANGE sometimes labels a MagicFormulae result as ranged. This is a control-flow finding, not a statistical live claim. The active movement type is always MELEE, limiting the whole script to melee following even for its ranged attack. See [BandosAction.java](<C:/Users/Tcarn/Desktop/Dementhium 637/src/org/dementhium/model/combat/impl/npc/BandosAction.java:124>).

3. **Corp/Zilyana/K'ril contain attack branches that only animate.** Their RANGE enum branch creates no Damage, unlike their PRIMARY branch. More importantly, each action advertises MAGIC reach while PRIMARY may roll a melee hit without checking melee contact. This permits a melee-labelled result at magic distance. Reconstruct separate movement reach, chosen attack and impact style; do not just increase damage.

4. **Old delayed specials can outlive their encounter.** Corp/Bandos/Sara/Zamm schedule world Tick callbacks that capture a player but do not recheck boss life, victim life/session, room, plane or encounter generation. They use `miscDamage`, losing the attacker; this bypasses source-dependent interaction validation. Current shared damage still applies godmode, shields and absorption: it is incorrect to claim these hits bypass every defence. Each script manually halves damage for prayer and then reflects that whole post-prayer amount when the matching deflect curse is active. That is distinct from ordinary deflect handling. The replacement should use explicitly owned hits and an encounter-defined protection rule.

5. **Kree and KQ cannot cycle their three enabled styles.** NPC.getCombatAction returns ranged first, then magic, otherwise melee; three true flags do not create a rotation. Neither NPC has a registered custom handler, both have -1 start/projectile/end graphics, and no KQ second-form transition exists in the inspected death/handler paths. Both currently select generic ranged. See [NPC.java](<C:/Users/Tcarn/Desktop/Dementhium 637/src/org/dementhium/model/npc/NPC.java:1225>) and the extracted definitions.

6. **Corp lacks its distinctive encounter systems.** No dark-core creation/AI, split ground magic or Corp weapon-reduction rule was found in active dispatch/damage paths. CorporealBeast.updateHit returns the hit unchanged. The current scripted special drains 25 Defence before a delayed magic hit; the primary branch also includes ranged damage despite Corp's packed Range level being 1. Specify the dated weapon/style exceptions and stat-drain attack, rather than importing modern OSRS exceptions. Familiar consumption is an acknowledged fidelity gap but stays deferred under the owner's Summoning policy.

7. **God Wars respawning depends on global NPC IDs.** doSpecialDeath waits until all four members are hidden, then finds them globally with getById and revives the group. A surviving minion stalls the whole group; a missing member can fail lookup; additional copies/instances cannot be distinguished. Introduce a room-owned boss/minion group with an explicit respawn policy. Ordinary NPC respawn is hard-coded to 60 plus death-animation ticks; the definition's respawn field has no caller in the active generic path.

8. **God Wars data is visibly inconsistent.** Packed LP: Graardor 3,940; Zilyana 5,550; K'ril 6,000; Kree 2,550. These values require review against custom difficulty intent and historical evidence, not automatic replacement. Sergeant Grimspike has only 160 LP compared with 1,280/1,270 for the other Bandos sergeants: a strong unit/typo candidate, not yet an authenticated replacement value. Several followers lack projectile data. Sara's packed eight-tick speed is overridden by the script's five-tick setting, so editing the data alone does not fix cadence.

9. **Tormented demon incoming-hit state advances too early and on zeroes.** updateHit subtracts 75% of the original hit after protection has already multiplied it by 0.6, often flooring the combination to zero. That prevents the subsequent positive-hit Darklight check. It then adds at least 20 to damageReceived even for zero; threshold is 310. This runs during Damage construction, ahead of actual impact/cancellation. Style switching occurs every 27 ticks (16.2 seconds), despite a comment saying three minutes. Ground magic builds tiles on plane 0, then damages players returned within radius 5 of the chosen tile, with no exact impact-footprint restriction. Its global tasks lack encounter reset guards. See [TormentedDemon.java](<C:/Users/Tcarn/Desktop/Dementhium 637/src/org/dementhium/model/npc/impl/TormentedDemon.java:129>).

10. **Nex still has actual-damage accounting work.** The active commenceSession delayed auto-attack submits a numeric hit, then feeds the original `castedDamage` to Soul Split/Turmoil and blood healing. Shields, absorption, overkill and immunity can make this differ from LP removed. Smoke poison is also applied after that call without a successful-contact callback. Preserve the existing encounter lifecycle fixes while migrating these effects. Inspect active code carefully: Nex.java also retains a legacy attack method, and NexAction.java is not its active implementation. See [Nex.java](<C:/Users/Tcarn/Desktop/Dementhium 637/src/org/dementhium/model/npc/impl/Nex.java:1654>).

11. **KBD applies breath statuses at launch.** FireType.task.execute runs before projectile damage is resolved. Freeze, poison or stat drain can occur without a surviving valid impact. Current icy breath can set 25 freeze ticks; shock drains a selected combat skill by five. Keep those numbers provisional until sourced. Its shared static action also stores mutable type across attacks.

## How NPC behavior currently works

The loader combines a spawn position with an NDE combat record and optional custom Java class. NPCTickTask advances combat, attached timers, NPC-specific tick behavior and movement, then searches for an aggression target. Generic aggression looks within five tiles, requires a path and attackability, skips invisibility, applies the greater-than-double-combat-level rule and chooses the nearest player. CombatExecutor handles cooldown and pending Interactions; CombatMovement determines contact/projectile reach and queues walking. DamageManager applies HP/accounting hooks. NPC.sendDead handles loot/death/respawn, with special activity/group paths.

This already includes clipped following, single/multi checks, poison, curse modifiers and managed-instance restrictions. The recommended enhancement is a more explicit, configurable version of this behavior, not indiscriminate modern pathfinding that removes intended 2011 safespots.

| Shared area | Current limitation | Recommended improvement |
| --- | --- | --- |
| Aggression | Five-tile generic search and level rule; no general aggression-tolerance lifecycle found. `aggressiveTicks` is exposed but unused. | Per-NPC aggression radius, tolerance/reset rules and boss exemptions; select only eligible room participants. |
| Targeting | KBD, Chaos, Bandos, Sara and Zamm independently select a random nearby player with 40% probability each tick, regardless of their current tank. | Encounter-specific target retention and deliberate switching; preserve historical exceptions rather than use one universal threat table. |
| God factions | playerHasItem, ignoreGodItems and canAttackFaction helpers have no active callers. Generic aggression searches players only. | Wire god-item protection for ordinary troops and faction-vs-faction behavior, with explicit boss exclusions. |
| Roaming/reset | 15% idle walk chance within +/-3 of origin; generic leash at >12 tiles; separate combat cutoff at >17. No complete boss phase reset follows from merely walking home. | Separate roaming bounds, arena bounds, chase bounds and a returning/reset state. Stop old attacks and reset HP/status/adds once according to encounter policy. |
| Reach/clipping | Generic magic range 15, ranged 10; moving-melee shortcuts; some custom actions advertise the wrong movement style. | Define reach per attack, verify large-NPC footprint/walls/corners, and preserve legitimate safespots. |
| Pending attack state | Some NPC action instances are static (notably Corp/KBD/dragons); others keep style/attack fields on one reusable action. Executor restores Interaction but not those fields. | Store selected style, effects and targets on each pending attack. Sequential ticks still allow overlapping attacks to overwrite shared state; this does not require a multithreaded race. |
| Mutable combat stats | Curse percentage modifiers exist, but there is no complete per-life mutable level/restoration model for all weapon drains. | Add isolated current levels, floors, immunities and recovery/reset; never mutate shared NPCDefinition objects. This unlocks correct BGS and similar NPC drains. |
| Statuses | Shared poison/impact work has improved, but bespoke bosses retain cast-time or numeric-hit effects. | One-use contact/damage callbacks with explicit immunity, instance and encounter checks. |
| Lifecycle | Ordinary/group respawn uses fixed timers and partially reused state. | Per-life generation, configurable timers, owned adds/tasks and deterministic reset/reward boundaries. |
| Observability | Missing records/handlers can silently degrade to generic combat. | Startup validation for combat NPCs; diagnostic output for effective handler, stats, attack choice, target, roll, mitigation and reset reason. Missing definitions for noncombat scenery NPCs need not be errors. |

God Wars access is also inconsistent: the inspected Bandos-door path deducts 40 only when killcount is greater than 40, but opens regardless. Its current Mob.requestWalk queues movement; older review statements about an empty method are not current. Treat access requirements as a separate owner-policy decision, preserving intentional teleport conveniences. It is not necessary to reintroduce historical entry friction to fix the fights.

## Historical evidence and confidence

Local code/data findings above have high confidence; historical replacement details have lower confidence until a dated specification is agreed. No modern OSRS/RS3 data table was adopted as a 2011 source.

- [Contemporary Corp guide](https://forum.tip.it/topic/202534-guide-to-killing-the-corporeal-beast/) describes direct and spreading magic plus a mobile core that drains nearby players and heals Corp. This establishes missing mechanic families, not exact 2011 timings or caps.
- [2009 Zilyana discussion](https://forum.tip.it/topic/237921-question-about-zilyana/) records fast melee pressure and running strategies. Exact cadence and targeting need stronger dated evidence before implementation.
- [April 2011 K'ril discussion](https://forum.tip.it/topic/292812-zamorak-gwd-boss/) corroborates the need for antipoison. His prayer special's trigger/rate/amount remain specification work.
- [June 2010 KQ discussion](https://forum.tip.it/topic/270136-kalphite-queen-slayer-assignment/) corroborates two forms and changing equipment. Exact defence/prayer semantics must not be simplified into assumed total immunity.
- [God Wars guide](https://www.tip.it/runescape/pages/view/god_wars_dungeon.htm) describes faction equipment protection and distinct strongholds. This page includes later additions, so it is contextual evidence rather than a frozen 2011 item list.
- [Period God Wars guide/discussion](https://forum.tip.it/topic/178246-the-god-wars-dungeon-aow/) describes distinct minion roles and post-boss target behavior. Reproduce encounter behavior only after resolving date/variant differences.

Existing COMBAT_* fixes, NEX_REVIEW_AND_FIXES.md, BARROWS_FIXES.md and FIGHT_CAVES_INSTANCE_FIXES.md remain authoritative for their explicitly recorded work. The old original combat review is not a list of still-open bugs. Barrows has owner-reported gameplay acceptance and Fight Caves' invisible-first-wave issue has owner-confirmed resolution; neither implies every fight mechanic is certified.

## Recommended implementation order and acceptance

1. Establish the boss specification/data inventory and preserve custom values explicitly. Correct missing Dagannoth data, accidental duplicate placements, invalid required projectiles and handler registration. Confirm intended Corp population.
2. Harden shared attack ownership, encounter target validation, per-life reset and NPC mutable stats. Fix Bandos' empty/wrong-formula branches as a focused urgent correction.
3. Rebuild original four God Wars fights and room-owned minions. Cover each boss's real attack types, tank behavior, protection interaction and individual/group respawn rules.
4. Reconstruct Corp core, splashes, weapon rules and stat effects. Keep familiar-specific work visibly deferred.
5. Complete KQ's two-form lifecycle and DK arena behavior; correct tormented demon shield/switching/AoE and KBD status timing.
6. Finish focused Nex actual-damage effects and historical/live calibration of the more mature Nex/Barrows/Jad encounters. Wire and verify frost dragons as a separate adjacent PvM task.

For each changed boss: test attack branches deterministically, selected style vs defence/mitigation, immunity, damage bounds, prayer timing, old-projectile cancellation, boss/player death, teleport/logout, empty-room reset, respawn and two simultaneous copies where supported. Then perform an ordinary-gear, godmode-off live encounter with two players where appropriate. Test large-NPC corners and intentional safespots, adds, rewards exactly once, and reopening after reset. Run only directly affected suites and necessary shared callers per tests/TESTING.md; a documentation review does not justify all gameplay suites.

Completion means the encounter specification, actual dispatch/data, deterministic behavior checks and live presentation agree. It does not mean adding a class named after each boss or copying a modern maximum-hit number.

## Subsequent implementation
The owner authorized the first consolidated implementation batch. See BOSS_BATCH_ONE_FIXES.md for the 2026-09-09 King definitions/dispatch, selected spawn cleanup, Grimspike LP and Graardor attack repairs, scoped validation, runtime staging and explicit historical/encounter limits. This original review remains a historical snapshot.

Step 2 was subsequently authorized and implemented. See NPC_COMBAT_FOUNDATION_FIXES.md for current-level/BGS support, shared NPC attack/life ownership, target/return behavior, validation, staged runtime and remaining limits.
