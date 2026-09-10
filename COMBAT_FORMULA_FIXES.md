# Combat accuracy and maximum-hit formula pass - 2026-09-07

## Outcome and baseline

Implemented in active src, following COMBAT_FOUNDATION_FIXES.md and EQUIPMENT_ABSORPTION_FIXES.md. Target remains early-2011/revision 637 equipment and mechanics. Later-2011 research is supporting evidence for the reconstruction, not authorization to add later content. This is a tested formula baseline, not a claim to possess Jagex's proprietary combat code.

The owner's Nex screenshots also confirmed the preceding soaking fix live: 280 melee -> 10 soaked / 270 HP lost; 351 melee -> 19 soaked / 332 HP lost, consistent with 13% melee absorption above 200 LP.

## Changes

- Added CombatFormula for explicit, independently testable effective levels, roll maxima and life-point max hits. Melee/ranged use current boosted levels, then prayer adjustment with flooring, stance, +8, and applicable effective-level set effects. Removed the old +14 expression and premature integer division of equipment bonuses. The base damage is 5 + effective strength * (64 + bonus) / 64; final caller multipliers precede final flooring. Decimal roundoff cannot accidentally remove an exact integer level.
- All three accuracy paths now use the same effective-level/equipment structure and roll scale. Attack and defence still roll independently; defender wins ties. Spell damage no longer increases spell accuracy. Removed magic's arbitrary 1.15/1.17 multipliers, differing NPC/player equipment weights, extra +5 roll offsets and the undocumented fire-spell metal-dragon defence reduction. Negative attack bonuses can reduce accuracy to zero rather than receiving an artificial minimum.
- Player magic defence separates its Magic and Defence contributions. Defence prayers affect the Defence contribution instead of multiplying the whole weighted level and equipment term. The NPC adapter retains a weighted Magic/Defence approach pending a separate NPC baseline audit; it is not certified as Jagex's NPC formula.
- Melee Void applies 10% to effective attack/strength, with flooring. Magic Void applies 30% to effective accuracy and no generic damage bonus. Specials compose with set bonuses rather than adding percentages to their multiplier. Void detection now checks the actual equipment slots, recognizes the omitted elite robe 19786, and allows a deflector to replace a non-helmet piece.
- Ranged Void damage remains a provisional 20% final multiplier. Contemporary observations dispute the Knowledge Base's advertised 10%; precise hidden-level versus final-damage placement still needs a historical calibration pass. It now multiplies a caller's special multiplier instead of adding to it. Do not describe this portion as historically certified.
- Longrange now supplies its +3 Defence stance bonus. Piety and Chivalry's missing +25%/+20% Defence inputs are restored. Their attack/strength behavior is retained. This does not complete the separate curse/prayer audit.
- Magic retains the period 3% damage per boosted Magic level and existing equipment magic-damage bonus. Generic NPC magic maximum damage and special caller multipliers remain unchanged; shared NPC accuracy now uses the common roll scale. Generic NPC melee/ranged damage inherits the revised base calculation.

Godmode, personal XP rates, damage soaking, private Barrows rewards and equipment availability remain. No Summoning-specific files, boss AI, player saves or packed definition files were changed. Familiars calling shared formula APIs inherit those changes; their individual behavior still needs the owner's separate tests. Dharok's existing missing-LP multiplier is retained as an approximation, now applied to the base max hit rather than multiplying the effective strength; its exact period effect remains in the equipment-effects backlog.

## Formula contract and evidence limits

The chosen accuracy reconstruction uses floor(10 * effective level * (64 + equipment bonus) / 64 * caller multiplier). Effective levels floor the prayer adjustment before stance/+8 and floor again after Void. The scale is explicit because integer ties depend on it. These are reconstructed roll maxima; passing regression tests verifies implementation and internal consistency, not historical hit rates.

[Archived December-2011 reproduction of the RuneScape forum accuracy research](https://rune-server.org/threads/attack-accuracy-formula.367522/) describes the staged effective levels, tenfold roll scale, stance bonuses, Void accuracy and weighted magic defence. The page itself acknowledges approximation; it is not a Jagex-published algorithm. [September-2011 research discussion](https://forum.tip.it/topic/301821-accuracy-calculator/) distinguishes maximum rolls from max hits and records defender-wins-ties. Its 99 Strength/+31 example supplies a 163-LP benchmark. [Firsthand pre-EoC max-hit testing notes](https://runescape.fandom.com/wiki/Talk%3AMaximum_melee_hit) support flooring the Void melee effective level before damage. These sources cannot establish every rounding detail or NPC exception conclusively.

[Mirrored Jagex Pest Control guide](https://www.2011.rs/kb/pest_control) supports Void set composition and the melee/magic bonuses. [Contemporary October-2011 ranged Void discussion](https://forum.tip.it/topic/303424-range-void-actual-effects-its-clearly-not-10/) conflicts with a simple 10% ranged-damage interpretation, so that uncertainty remains explicit. [Mirrored Prayer guide](https://www.2011.rs/kb/prayer_the_basics) supports Piety/Chivalry defence boosts. [Mirrored Magic FAQ](https://www.2011.rs/kb/magic_faq) supports boosted-level spell damage. Mirrors may contain later pre-EoC edits; exact January-2011 equivalence remains a limitation.

## Validation

Java 8-target compilation passed. CombatFormulaRegression passed 90 assertions and 180,000 generated attacks. Coverage includes fixed max-hit benchmarks, potions before prayers, rounding boundaries, negative accuracy, special multiplier composition, normal/elite/deflector Void, longrange, Piety/Chivalry toggle symmetry, magic spell accuracy independence, guaranteed splashes through both player magic APIs, and real equipment comparisons.

Sample simulation: 99 offensive/defensive skills, one defender body piece, no prayers, neutral offensive stance. Figures are positive-damage hit rates, not claims of historical measured RuneScape rates. Armour did not change the attacker's maximum or conditional damage distribution.

| Attack fixture | Rune platebody | Black d'hide body | Mystic robe top | Max LP |
| --- | ---: | ---: | ---: | ---: |
| Whip, slash | 50.24% | 61.25% | 77.93% | 249 |
| Magic shortbow, rune arrows | 46.46% | 55.35% | 75.20% | 193 |
| Air staff, Fire Wave | 62.81% | 34.36% | 46.01% | 200 |

Also passed: CombatFoundationRegression (117), CombatBalanceRegression (48 plus 200,000 samples), EquipmentAbsorptionRegression (102), NexMovementRegression, NexIceRegression, BarrowsRegression (711,204), and BarrowsFaceRegression. Logs are in build/combat-formulas. These tests do not replace live encounter checks.

## Runtime and next checks

Eight runtime class files staged to bin with prior runtime backups and SHA-256 verification. Restart the server to load them. Source/runtime backups: build/combat-formulas-before. Compiled verification output: build/combat-formulas. No automatic restart performed.

After restart, test ordinary melee, ranged and magic with godmode off; compare accurate/aggressive/rapid/longrange stances, potions, and Void. Recheck Bandos/Nex and Barrows because shared accuracy can change encounter difficulty. An occasional maximum or short run of misses is not enough to assess accuracy; use repeated fixed-gear trials.

Remaining: exact Void ranged damage placement, Rigour's accuracy/strength split, Turmoil and curse state/drain rules, magic-defence curse effects, NPC-specific stats and exceptions, Slayer/Salve/Dharok and other equipment effects, and bespoke special multipliers. Modern OSRS/RS3 formulas were not treated as proof of 2011 behavior. Summoning-specific work remains deferred.