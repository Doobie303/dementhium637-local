Combat review against 2011 RuneScape
==================================

Review date: 2026-09-07. This is a source and configuration review of the current working tree, including its existing local changes. No gameplay code was changed. Godmode and personal XP rates are treated as intentional custom features.

The server has a substantial pre-EoC combat foundation, but its combat outcomes are not yet a faithful 2011 reconstruction. The largest gaps are shared damage processing, ranged equipment bonuses, custom formulae, and incomplete equipment/spell effects. Fixing individual weapons before these foundations would produce misleading balance results.

Scope and confidence
--------------------

Reviewed the active combat executor and action lifecycle, all three formula classes, damage processing, equipment bonuses, spell and ranged-weapon registration, prayer/curse integration, movement, ammunition, representative specials, ancient spells, consumption/poison, PvP eligibility, and representative NPC/boss/familiar paths. Individual specials and bosses were sampled; their presence is not a certification of every mechanic. Packed item/NPC statistics were not exhaustively decoded or compared with historical records. No server launch, client combat session, or statistical gameplay benchmark was performed.

The build configuration points to `src`; the loose root `combat/` directory is not the active source root in `.classpath` or `Compile.bat`. The active Nex implementation is in `model/npc/impl/Nex.java`; the similarly named `combat/impl/npc/NexAction.java` is largely commented out.

Choose a dated 2011 baseline before implementation. “637-era/early 2011” and “everything available by December 2011” are different targets. Later content should have a separate dated inventory rather than being silently mixed into the baseline. Exact proprietary accuracy formulae cannot be certified from this review; historical formula candidates need evidence and measured acceptance tests.

What is already present
-----------------------

- Separate melee, ranged, and magic actions, animations, projectiles, and life-point damage.
- Stab/slash/crush attack and defence bonuses; ranged and magic defence bonuses.
- The 70% Magic / 30% Defence level weighting in magic defence. This matches the weighting quoted in a [February 2011 discussion of the game guide](https://forum.tip.it/topic/287352-magic-defense/), though the rest of this server's formula still needs work.
- Weapon attack delays, rapid ranged adjustment, accurate/aggressive/controlled/defensive styles, and a nominal 600 ms server cadence.
- Ordinary protection prayers using 0.6 damage against players and zero against NPCs in the generic player damage handler; custom boss paths need separate inspection.
- Standard prayers, curses, special energy, Vengeance, recoil, spirit shields, food, potions, poison, and equipment degradation infrastructure.
- 44 registered combat spells: 24 modern and 20 ancient, including surges and Miasmic spells.
- Single/multi-combat and Wilderness level checks, plus activity rules.
- Custom implementations for Nex, Fight Caves/Jad, dragons, God Wars and other NPCs. These still inherit shared combat problems or use separate damage paths.

Priority findings
-----------------

1. **Damage soaking reduces the hitsplat, not actual life-point loss — confirmed, highest priority.**

   `DamageManager.soak` assigns the reduced amount to `hit.damage`, but `updateDamageAttributes` calls `Skills.hit(damage)` with the original value. With a 500 hit and 20% absorption, the code displays 440 damage plus 60 soaked while removing 500 life points. This is a direct source-derived example, not a live measurement. Fix health subtraction, displayed damage, kill credit, reflection, healing and XP to use explicitly defined stages of the same hit result. [Code](<C:/Users/Tcarn/Desktop/Dementhium 637/src/org/dementhium/model/misc/DamageManager.java:464>).

   Damage soaking above 200 life points was already part of the target era and was explicitly tied to the triangle. [Jagex's December 2010 announcement, preserved on Darkan](https://wiki.darkan.org/Update:Damage_Soaking_and_New_Hitsplats).

2. **Soaking uses the attacker's current combat action instead of the incoming hit's style — confirmed.**

   A projectile can launch as ranged, then the attacker switches to melee before it lands. The soaking calculation can select melee absorption. Null attackers default to melee, so miscellaneous damage can also enter an inappropriate absorption path. Store damage style on each pending hit and specify which damage categories are absorbable. [Code](<C:/Users/Tcarn/Desktop/Dementhium 637/src/org/dementhium/model/misc/DamageManager.java:470>).

3. **Ranged accuracy and strength are overwritten incorrectly — confirmed.**

   `Bonuses.calculate()` first totals equipment, then replaces total ranged strength with the weapon's ranged attack bonus; when the weapon has ranged strength, it replaces ranged accuracy with that value. This can discard ammunition/armour contributions and make ammunition upgrades behave incorrectly. The overload `calculate(Player)` does not perform those replacements, creating inconsistent calculations too. Audit the underlying item schema before correcting it so any compensating data errors are corrected together. [Code](<C:/Users/Tcarn/Desktop/Dementhium 637/src/org/dementhium/model/player/Bonuses.java:24>).

4. **All three core formulae contain custom balancing — confirmed implementation; historical replacement needs calibration.**

   Accuracy and defence are rolled using truncated Gaussian samples. Successful damage is another Gaussian sample whose mean depends on the accuracy/defence result. Consequently accuracy influences both landing the hit and its damage distribution. Ranged has an additional universal 1.2 accuracy multiplier. Player magic includes fixed 1.15 and 1.17 multipliers, adds spell damage to accuracy, and applies a fire-spell defence reduction against metal dragons. NPC and player magic use different equations. These are not a documented, unified 2011 specification.

   Separate effective levels, equipment bonuses, accuracy rolls, maximum hits, and successful-hit damage rolls. Validate a historically supported roll model; do not simply substitute today's OSRS or RS3 formulae. [Random generator](<C:/Users/Tcarn/Desktop/Dementhium 637/src/org/dementhium/model/combat/CombatExecutor.java:185>), [melee](<C:/Users/Tcarn/Desktop/Dementhium 637/src/org/dementhium/model/combat/MeleeFormulae.java:44>), [ranged](<C:/Users/Tcarn/Desktop/Dementhium 637/src/org/dementhium/model/combat/RangeFormulae.java:106>), [magic](<C:/Users/Tcarn/Desktop/Dementhium 637/src/org/dementhium/model/combat/MagicFormulae.java:214>).

5. **Enchanted bolts are approximations — confirmed.**

   A shared 25% branch selects bolt effects. Ruby bolts use a triple-damage ordinary roll, with the original health-sacrifice code explicitly commented out. Diamond bolts use a 0.55 defence multiplier. Other effects also use custom multipliers. Replace this with individually sourced activation chances, accuracy bypass rules, damage effects, self-costs and immunities for the chosen date. Ruby's target-health-based effect and self-cost require restoration; exact caps and boss exceptions should be specified separately. [Code](<C:/Users/Tcarn/Desktop/Dementhium 637/src/org/dementhium/model/combat/CombatUtils.java:510>).

6. **Non-dropping ammunition can avoid consumption — confirmed ordinary ranged path.**

   `dropArrows` returns before removing ammunition when `isDropAmmo()` is false. Normal hand-cannon and bolt-rack paths set that flag false. Chinchompas have explicit separate consumption, but the ordinary handler does not separate “consume” from “drop.” Ammunition is otherwise removed at impact, from live equipment. Reserve/consume the correct stack at launch; handle breaking, dropping, recovery and charge-based weapons independently. Validate Ava variants and equipment restrictions. [Consumption](<C:/Users/Tcarn/Desktop/Dementhium 637/src/org/dementhium/model/combat/CombatUtils.java:668>), [weapon branches](<C:/Users/Tcarn/Desktop/Dementhium 637/src/org/dementhium/model/player/Player.java:2147>).

7. **Miasmic's slow is disconnected — confirmed.**

   Spells set `miasmicTime`; the executor reads `miasamicTime`. The existing slowdown therefore does not activate from those spells. Also verify the 1.5 multiplier, integer rounding, durations and immunity against period evidence. Casting without Zuriel's staff resets the victim's combat rather than the caster's in the sampled barrage handler. [Executor](<C:/Users/Tcarn/Desktop/Dementhium 637/src/org/dementhium/model/combat/CombatExecutor.java:239>), [spell](<C:/Users/Tcarn/Desktop/Dementhium 637/src/org/dementhium/model/combat/impl/spells/ancient/MiasmicBarrage.java:32>).

8. **Antipoison immunity does not work through its setter — confirmed.**

   `setCanBePoisoned(boolean b)` always sets `canBe = true`. Potion handlers call it with false expecting temporary immunity. Ordinary poisoned-weapon actions also attempt poisoning on every positive eligible hit without a separate proc roll. Review application chances, damage progression, stronger-poison replacement, NPC immunity and logout persistence. [Code](<C:/Users/Tcarn/Desktop/Dementhium 637/src/org/dementhium/model/misc/PoisonManager.java:110>).

9. **Ranged recoil applies the wrong number — confirmed.**

   The first-hit ranged recoil branch checks `getRecoiled()` but submits `getDeflected()`. A recoil ring can consume its charge without returning the intended damage. [Code](<C:/Users/Tcarn/Desktop/Dementhium 637/src/org/dementhium/model/combat/impl/RangeAction.java:120>).

10. **Prayer/curse fidelity needs another pass — mixed confirmed defects and calibration work.**

    Soul Split heals 20% against NPCs but 40% against players, based on pre-impact damage. Deflect's one-in-three chance is explicitly described in a comment as invented. Turmoil reads `hasTurmoil` where it appears intended to set/reset it. Ranged prayer accuracy and strength share one modifier, restricting faithful representation of prayers with different boosts. Reconstruct curse rates, stacking, unit conversion, drain timing and state transitions from dated references. Do not assume each existing number is correct because the prayer activates. [Soul Split](<C:/Users/Tcarn/Desktop/Dementhium 637/src/org/dementhium/model/player/Player.java:2219>), [Deflect](<C:/Users/Tcarn/Desktop/Dementhium 637/src/org/dementhium/model/player/Player.java:2045>), [Turmoil](<C:/Users/Tcarn/Desktop/Dementhium 637/src/org/dementhium/content/skills/Prayer.java:1241>).

11. **Void and Barrows effects are incomplete or inconsistent with period descriptions.**

    Void melee accuracy receives +15%, ranged damage +20%, and magic receives +10% accuracy plus a damage boost. A [rehosted period Knowledge Base page](https://2011.rs/kb/pest_control) describes +10% melee attack/damage, +10% ranged accuracy/damage, and +30% magic attack. Treat that mirror as a historical reference to corroborate, not Jagex's current official site. Check multiplier placement and rounding as well as percentages.

    All six Barrows sets can be recognized, but the only active `barrowsSet(...)` combat call found is Dharok's. Guthan's healing, Verac's bypass and the other sets' effects need implementation/verification. Dharok's current damage multiplier depends only on missing life points and needs historical validation across Constitution levels. The Void robe check also repeats item 19788. [Set detection](<C:/Users/Tcarn/Desktop/Dementhium 637/src/org/dementhium/model/player/Equipment.java:589>), [Dharok](<C:/Users/Tcarn/Desktop/Dementhium 637/src/org/dementhium/model/combat/MeleeFormulae.java:123>).

12. **Slayer and other equipment-specific combat effects are missing from the reviewed formula paths.**

    Magic explicitly leaves Slayer/hexcrest modifiers as a TODO. No active Black mask, Salve or Berserker necklace combat multiplier was found in the reviewed paths and searches. Implement a dated effect inventory, including stacking/exclusion rules, instead of relying on generic item bonuses. Staff of light currently saves runes at 1/8; verify its period chance separately. [Magic TODO](<C:/Users/Tcarn/Desktop/Dementhium 637/src/org/dementhium/model/combat/MagicFormulae.java:293>), [rune saving](<C:/Users/Tcarn/Desktop/Dementhium 637/src/org/dementhium/model/combat/impl/MagicAction.java:104>).

13. **Freezes, Teleblock and multi-target spells need correction.**

    Ice Barrage uses 36 nominal ticks (21.6 seconds), applies freeze during casting, resets the victim's attack, and treats a zero-damage result as a splash. A [guide published in February 2011](https://www.sythe.org/threads/guide-to-bursting-pure-style/) describes a roughly 20-second freeze; precise tick boundaries need verification. Accuracy success, zero damage, immunity and movement restriction should be represented separately.

    Teleblock uses 550 ticks (330 seconds) with no prayer-dependent duration branch in its handler, and its immunity expires before the block does. Check all teleport entry points and reapplication rules. Multi-target collection uses `count++ <= maximum`, counts some rejected entries, and does not explicitly prioritize the selected target. It also chooses NPC-only or player-only lists. Specify actual eligible targets and caps before changing the helper because callers currently pass different limits. [Barrage](<C:/Users/Tcarn/Desktop/Dementhium 637/src/org/dementhium/model/combat/impl/spells/ancient/IceBarrage.java:74>), [Teleblock](<C:/Users/Tcarn/Desktop/Dementhium 637/src/org/dementhium/model/combat/impl/spells/modern/Teleblock.java:30>), [target collection](<C:/Users/Tcarn/Desktop/Dementhium 637/src/org/dementhium/model/combat/CombatUtils.java:384>).

14. **Attack reach is too generic to model weapon differences faithfully.**

    Ranged has a global 10-tile range and magic 15; longrange adds two to the generic ranged limit. The active ranged weapon definition has speed and ammunition information but no per-weapon reach. Add weapon/spell reach and appropriate caps. Moving-melee shortcuts can authorize attacks from several tiles away without entering the ordinary wall-check path; regression-test chasing, corners, large NPCs, freezes and safespots. [Style distances](<C:/Users/Tcarn/Desktop/Dementhium 637/src/org/dementhium/model/combat/CombatType.java:24>), [movement](<C:/Users/Tcarn/Desktop/Dementhium 637/src/org/dementhium/model/combat/CombatMovement.java:60>).

15. **Important specials have custom behaviour.**

    Korasi refuses targets larger than one tile and uses DEFAULT for mitigation while displaying magic damage and reporting melee for movement. Separate reach, accuracy style, mitigation style and hitsplat style. Dragon claws' fallback branches do not consistently match their own documented hit splits. AGS uses a 4.5499 accuracy multiplier. Build independent specifications for DDS, claws, godswords, maul, dark bow, crossbows, spears, Staff of light and Korasi before tuning damage. [Korasi](<C:/Users/Tcarn/Desktop/Dementhium 637/src/org/dementhium/model/combat/impl/specs/Disrupt.java:42>), [claws](<C:/Users/Tcarn/Desktop/Dementhium 637/src/org/dementhium/model/combat/impl/specs/SliceAndDice.java:62>), [AGS](<C:/Users/Tcarn/Desktop/Dementhium 637/src/org/dementhium/model/combat/impl/specs/Judgement.java:27>).

16. **Damage entry points and effect timing disagree.**

    The integer overloads of DamageManager apply Nex hit caps and other hooks; the Damage-object overload does not run those same hooks. Normal attacks use the latter, making the route itself affect encounter rules. Recoil/Vengeance are calculated before the later life-point clamp; Soul Split and XP are also often processed before damage lands. A delay parameter on DamageManager stores hitsplat delay but subtracts life points immediately. Standardize resolution and document which values are captured at launch and which are evaluated on impact. Preserve intended attack-specific prayer timing rather than moving every check to impact indiscriminately. [Damage overload](<C:/Users/Tcarn/Desktop/Dementhium 637/src/org/dementhium/model/misc/DamageManager.java:420>), [reflection calculation](<C:/Users/Tcarn/Desktop/Dementhium 637/src/org/dementhium/model/combat/Damage.java:59>).

17. **PvM cannot be certified from player formula fixes alone.**

    NPC stats are loaded from a separately packed NDE definition file. Generic NPC magic accuracy reads bonus index 4, also used by ranged accuracy; establish the NPC schema and correct mappings. Corp's incoming hit override simply returns the hit, with no weapon-specific reduction in that override. Several bosses deliver raw or miscellaneous damage with hand-coded prayer behaviour. Compare each boss's max hits, stats, reach, attack selection, immunities, special mechanics and prayer exceptions independently. Familiar attacks also need separate validation. [NPC schema](<C:/Users/Tcarn/Desktop/Dementhium 637/src/org/dementhium/model/definition/NPCDefinition.java:40>), [Corp override](<C:/Users/Tcarn/Desktop/Dementhium 637/src/org/dementhium/model/npc/impl/CorporealBeast.java:22>).

18. **Timing and content coverage remain verification work.**

    Food adds three attack-delay ticks and potions have a separate two-tick consumption gate. Validate attack/eat/drink combinations, brewing/restoring, overload restrictions and stat restoration. Spell registration lacks handlers for several familiar standard-book combat spells (for example, god spells, Iban Blast and Magic Dart); distinguish absent registrations from utility spells handled elsewhere. Vengeance exists outside the combat spell container. If targeting later 2011, build a separate list for content such as Storm of Armadyl and relevant new equipment.

    The world currently runs tick tasks sequentially, so shared action objects are not evidence of an ordinary parallel-combat race in this configuration. They remain fragile because pending attacks query mutable action state. The server scheduler also replaces its executor when work queues up; run load/timing checks before claiming faithful tick behaviour. [Scheduler](<C:/Users/Tcarn/Desktop/Dementhium 637/src/org/dementhium/ServerThread.java:30>).

Recommended implementation order
--------------------------------

1. Freeze the historical date, write the baseline specification, and keep intentional custom switches explicit.
2. Correct damage soaking, hit-type preservation, hit-processing overloads, ranged bonus aggregation, recoil, antipoison and Miasmic state.
3. Extract and validate item/NPC combat data. Make formulas independently testable, then calibrate accuracy and max-hit calculations.
4. Implement dated prayer/curse and equipment effects, followed by bolt effects and resource consumption.
5. Correct freezes, reach, movement, pending-hit timing, multi-hit/multi-target behaviour and specials.
6. Audit each boss/familiar and complete the dated spell/weapon inventory.
7. Run repeatable PvP/PvM validation with godmode disabled and fixed stats/gear.

Acceptance matrix
-----------------

| Test group | Required evidence |
|---|---|
| Triangle | Same-level melee/ranged/magic against plate, dragonhide and robes; test mixed armour and gear swaps too. Measure hit rate, mean successful damage and damage per second separately. |
| Formulae | Known maximum-hit cases with different levels, potions, styles, prayers, negative bonuses, Void and task effects; explicitly verify rounding and stacking. |
| Absorption | Hits below/at/above 200, style-specific armour, prayer/shield interactions and projectile gear switches; visible damage must agree with life-point loss. |
| Timing | Weapon cooldowns, projectile launch/impact, prayer changes, eating/drinking, maul follow-ups, multi-hit attacks, death/logout while attacks are pending. |
| Effects | Rune/ammo consumption, bolt procs, poison immunity, freeze expiry, Teleblock, Miasmic, recoil, Vengeance, Soul Split and all Barrows effects. |
| PvM | Low-level monsters, high-defence targets, dragons, Corp, God Wars, Jad/Fight Caves, Nex and familiars with documented exceptions. |
| Reliability | Multiple simultaneous fights, stable tick cadence under load, no cross-target state leakage, and no disagreement between damage APIs. |

Completion should mean this matrix passes against a dated evidence set. A percentage such as “80% authentic” would not be meaningful without it. This is a repairable foundation, but reaching 2011 fidelity requires a focused combat-correctness project rather than a handful of balance tweaks.
