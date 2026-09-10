# Combat foundation fixes

Implemented 2026-09-07 against the active `src` tree. This is the first implementation batch following `COMBAT_2011_REVIEW.md`, not a claim that all 18 findings or the complete 2011 combat system are finished. The provisional target remains 637-era/early 2011.

## Completed

- **Actual soaking:** all six DamageManager entry points now use the same application path. A 500 incoming melee hit against 20% absorption removes 440 life points and displays 440 damage plus 60 soaked. Absorption applies only to the portion above 200, rounds down, and uses the incoming hitsplat's melee/ranged/magic type. Untyped, poison, reflected and environmental red damage do not borrow an attacker's current weapon style.
- **Damage accounting:** life-point loss, displayed damage, Damage-object results and enemy damage credit agree, including overkill. Healing entries restore health. Negative raw damage cannot heal. Godmode and hit immunity work through each overload. Soaking is a partner hitsplat, not another damage application. Hitsplat delay remains a client display delay; this patch does not turn it into a server scheduling delay.
- **Boosted life points:** a miss or small hit no longer clamps all overhealed life points back to the ordinary maximum.
- **Reflection:** recoil charges and Vengeance amounts on Damage-object attacks are calculated at impact from damage actually received. Ordinary ranged recoil now submits the recoil amount rather than the deflect amount. The second ordinary double-shot can trigger Vengeance if it is still available.
- **Damage-based effects:** Soul Split and Smite run once per applied offensive melee/ranged/magic hit, including individual multi-target hits. Soul Split heals 20% against both players and NPCs and drains 1 prayer point per 50 life points of actual player damage. Smite drains 1 per 40. No healing/drain comes from immune targets or excess overkill. Blood Rush/Blitz/Burst/Barrage healing moved from casting to applied damage. Existing projectile IDs are retained.
- **Ordinary attack XP:** normal melee, ranged and magic actions award damage-related XP after applying damage. Personal XP-rate calculations are preserved. Magic splashes retain the distinct -1 result and cannot subtract Constitution XP. This is not yet a migration of every custom special-attack XP handler.
- **Ranged equipment:** both Bonuses calculation overloads use the same aggregation. Weapon accuracy no longer overwrites strength or discards armour contributions. A source-level correction layer fixes verified ordinary arrows, bolts, gem-tipped bolts and Karil's racks, including poisoned/enchanted names. Ordinary bows/crossbows with separate ammunition do not add phantom weapon strength. Quivered ammunition does not boost self-contained or thrown weapons. Zaryte remains +120 accuracy/+115 strength; the existing hand-cannon allocation is retained. Packed binary files are not rewritten.
- **Accuracy rolls:** core melee/ranged/magic methods now use independent uniform integer attack, defence and successful-damage rolls; ties favour defence. Successful damage no longer increases with the winning accuracy roll. Removed the universal 1.2 ranged accuracy multiplier. NPC magic accuracy reads magic attack, not ranged attack. NPCs retain their distinct 14-entry bonus schema, including magic strength at index 13.
- **Diagnostics:** `::combatdebug` toggles session-only incoming/outgoing hit messages showing style, incoming/adjusted damage, absorbed damage and actual health loss. Here, incoming means the amount passed into DamageManager, after any caller-specific protection/shield adjustment; it is not a complete accuracy/prayer trace.

## Evidence and historical limits

The absorption threshold comes from [Jagex's December 2010 damage-soaking announcement, preserved on Darkan](https://wiki.darkan.org/Update:Damage_Soaking_and_New_Hitsplats). Standard ranged corrections use the rehosted period Knowledge Base tables for [bows and arrows](https://www.2011.rs/kb/ranged_standard_equipment) and [crossbows and bolts](https://2011.rs/kb/ranged_crossbows_and_bolts). These are historical mirrors, not current official Jagex pages. [Contemporary player-submitted ranged-strength measurements](https://forum.tip.it/topic/225942-ranged-strength-needed-for-items-database/) provide supporting evidence.

Soul Split's period ratio is recorded in the [Ancient Curses Knowledge Base mirror](https://2011.rs/kb/prayer_the_ancient_curses). The roll model is a reconstruction consistent with [September 2011 accuracy research](https://forum.tip.it/topic/301821-accuracy-calculator/), including separate hit chance/average damage and defender-wins-ties discussion. That research explicitly acknowledges uncertainty about maximum-roll formulas. The existing effective-level and maximum-hit equations have NOT been certified as Jagex's proprietary formulas merely because the random sampling is now separated.

## Preserved custom behavior

Godmode immunity and its 750 outgoing test hit remain, including its deliberate Nex-cap bypass. Nex shielded-minion rules still take precedence. Nex's ordinary 500 hit cap, vulnerable-minion 600 cap, siphon conversion and encounter-specific effects remain in the shared path. Personal XP rates, Barrows reward boosts, Nex encounter code and the test-gear contents were preserved.

No Summoning-specific source changed; its directory was hash-compared with the pre-task backup. Familiars that call shared damage/formula APIs naturally inherit those shared corrections. Their AI, pouches, scroll registration and special moves were not reworked or certified.

Raw integer damage callers retain their existing protection/shield contract: they supply an already adjusted amount. Applying generic prayers again would double-reduce bespoke boss attacks. Reflected hits on those bespoke paths, defensive prayer/shield snapshot timing, and direct Skills/NPC health mutations still require their own caller audit.

## Validation

Java compilation passed targeting Java 8 (only obsolete source/target warnings).

- CombatFoundationRegression: 117 checks covering all six overloads, three absorption types, threshold/rounding, hitsplat delays, kill credit, healing, immunity, boosted health, overkill, reflection, godmode and Nex/minion caps.
- CombatBalanceRegression: 48 checks, including 200,000 seeded statistical samples; armour contribution, standard/enchanted ammunition, Zaryte, Karil, upgrade ordering, overload agreement, independent damage distribution, triangle armour comparisons, PvP/PvM protection for all three styles, Soul Split/Smite, blood healing, real ranged-action recoil and NPC bonus-array compatibility.
- Existing NexMovementRegression, NexIceRegression, BarrowsRegression, BarrowsFaceRegression and TestGearRegression passed. BarrowsRegression completed 711,204 checks. These regression tests are isolated harnesses, not live-client acceptance tests.

## Runtime and rollback

33 class files across the 18 changed source families were copied into `bin` and hash-verified against the tested build. The server was NOT restarted. Restart it before in-game testing.

Pre-task source backup: `build/combat-foundation-before/src`.
Pre-staging runtime-family backup: `build/combat-foundation-before/bin`.
Compiled build: `build/combat-foundation`.
Exact source list: `build/combat-foundation-sources.txt`.
Runtime staging manifest: `build/combat-foundation-staging.json`.

These backups are specifically for this batch. Do not revert unrelated working-tree changes or older Nex/Barrows/testgear work when rolling this back. The new CombatRolls and RangedEquipmentStats classes have no pre-existing runtime counterparts.

## Next priority work and live acceptance

1. Test ordinary combat with godmode OFF and `::combatdebug` ON: absorption gear against all styles, prayer on/off, armour/ammunition upgrades, Soul Split/blood healing, recoil, and overhealed life points. Repeat Nex and Barrows smoke tests because the shared roll/damage changes can affect difficulty even though encounter code is unchanged.
2. Calibrate effective-level/max-roll/max-hit equations and Void against a selected dated baseline; remove the remaining unsupported magic multipliers only with reference cases. Broaden the packed-stat audit to throwing weapons, chinchompas, crystal degradation and NPC data.
3. Complete the remaining prayer/curse review: Deflect activation probability, Turmoil state/target updates, ranged accuracy versus strength modifiers, drain timing and shield ordering.
4. Migrate bespoke specials/enchanted bolt effects, XP, ammunition reservation and timing into explicitly tested stages. Several custom effect/XP paths still use pre-impact amounts; the original review remains the backlog for these.
5. Leave Summoning-specific changes deferred while the owner tests it.
