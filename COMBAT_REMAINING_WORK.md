# Combat remaining-work review

Current plan (2026-09-09): [NPC/shared combat repair and completion plan](docs/project/NPC_COMBAT_LIVE_REVIEW_2026-09-09.md). Its **next three steps** are shared animation/pursuit/visibility repairs; complete God Wars/Corp/Nex encounter behavior; release and live acceptance. The owner supplied additional Sara/Bandos/Corp/Nex recordings; the current plan includes those reports, prior findings and explicit proof for each step. The wider combat completion register remains open; this reorganization does not mark unfinished items complete or create new deferrals. The body below is retained as a historical 2026-09-08 snapshot: later overnight, NPC foundation and boss-step reports supersede its statements that AoE/player sets/BGS/range support or boss work are absent. Do not reopen completed work from this old list.

Follow-up: the authorized 2026-09-09 overnight batch is recorded in COMBAT_OVERNIGHT_FIXES.md. Read it and COMBAT_MORNING_DECISIONS.md before treating the findings below as current defects. Steps 1-4 received concrete fixes; major calibration/coverage decisions remain explicitly listed, and step 5 was not started.

Reviewed 2026-09-08 against active `src` and the completed combat reports. This is a source/status review, not a new historical audit or a gameplay patch. No regression suites or live clients were run for this review. Previous test counts below are recorded batch evidence, not fresh results. Original review documents remain historical snapshots.

## Overall status

The shared foundation is substantially repaired: actual absorption/HP accounting, incoming styles, ranged aggregation, independent rolls, effective levels, ordinary reflection/XP/healing, several prayers/equipment effects, ammo reservation, ice impact, and selected specials have implementations and regression coverage. It would be incorrect to restart the original 18 findings as if none were fixed.

The largest remaining implementation gaps are multi-target selection, non-ice status-effect lifecycle, player-worn equipment effects, remaining specials, weapon reach and bespoke PvM paths. Exact 2011 calibration and live acceptance are separate tracks.

Target remains provisionally early-2011/revision 637; no exact date has been selected. Preserve godmode, personal XP rates, custom PvP/EP/Teleblock policy, Barrows loot boosts and Primal behavior. Summoning-specific work and Dungeoneering migration remain explicitly deferred.

## Completed work to retain

| Area | Completed scope | Remaining boundary |
| --- | --- | --- |
| Shared hits | Consistent soaking, HP clamp/credit, typed incoming hits, ordinary recoil/Vengeance, Soul Split/Smite/blood healing and ordinary XP at impact | Raw/bespoke callers, direct HP mutations and all effect snapshots are not fully audited |
| Formula baseline | Uniform independent rolls, ranged aggregation/ammo corrections, effective-level reconstruction, magic cleanup, melee/mage Void, Piety/Chivalry defence, longrange defence | Historical accuracy/rounding, ranged Void, NPC exceptions and broad packed data |
| Equipment/bolts | Verified absorption subset; Slayer/Salve/Dharok changes; Ruby/Onyx/Sapphire/Topaz/Emerald impact work; ordinary and 13 special ammo paths | Player Barrows procs, further equipment, per-gem tuning, poison, Ava/charges |
| Prayer/status | Drain bonus indexing, flat Turmoil additions, separate ranged accessors, ice impact/thaw guards, Miasmic attribute spelling | Deflect/Sap/Leech/Turmoil timing; non-ice statuses and remaining immunity rules |
| Specials/shields | Attack-owned follow-up hits, real delayed HP/XP for selected specials, claws/DDS/godswords/SGS/BGS/ZGS/Korasi corrections, typed shield pipeline | All handlers are not certified; raw shield ordering and reported Elysian symptom remain open |
| Encounters/PvP | Nex movement/ice/ownership fixes, six Barrows brother behaviors, custom PvP death/target/EP corrections | Complete boss balance and live acceptance are not established |

Sources: COMBAT_FOUNDATION_FIXES.md, COMBAT_FORMULA_FIXES.md, COMBAT_ENHANCEMENTS_FIXES.md, EQUIPMENT_ABSORPTION_FIXES.md, SPECIAL_ATTACK_SHIELD_FIXES.md, NEX_REVIEW_AND_FIXES.md, BARROWS_FIXES.md and PVP_TARGET_EP_FIXES.md. Later reports supersede older pending lists only within their stated scope. For example, the old magic Slayer TODO remains in source even though `EquipmentEffects.multiplier` now implements that bonus.

## Recommended implementation order

### 1. Multi-target selection and status lifecycle

**Confirmed source defects / concrete patch candidates:**

- `src/org/dementhium/model/combat/CombatUtils.java:396` and `:426`: both target collectors still use `count++ <= maximum` before eligibility finishes. They can admit maximum + 1 candidates, and rejected candidates can consume the allowance. The selected victim is not explicitly prioritized. Callers disagree about whether their number means total targets or extras: ancient spells commonly pass 8, chinchompas 9. Define the contract and migrate callers together; simply changing `<=` to `<` can alter existing spell totals incorrectly. NPC/player collection is split by the source argument's type; mixed-target policy also needs an explicit specification. Korasi has its own additional cap, so do not assume every caller actually lands maximum + 1 hits.
- All four `impl/spells/ancient/Miasmic*.java` handlers reset the **victim's** combat executor when the caster lacks Zuriel's staff. They also write slow/immunity during `castSpell`. The earlier patch repaired the misspelled cooldown attribute, not these paths.
- `impl/MeleeAction.java:51`, `impl/RangeAction.java:51` and Smoke handlers still apply poison before damage resolution, based on the rolled hit. Ordinary weapon paths shown have no separate poison proc roll. `CombatUtils.java:516` applies Jade stun while constructing damage. Move relevant effects to validated hit resolution, capturing the originating weapon/ammo and guarding cancellation/immunity; establish which effects require a positive hit versus merely successful accuracy.
- `model/misc/PoisonManager.java`: `isPoisoned()` uses amount > 10 while the ticking path permits amount == 10; existing poison cannot be upgraded through `poison`; scheduled poison tasks have no generation/ownership token. Cure/re-poison before an old tick executes can let an older task process the new poison. Add focused lifecycle tests before patching, including exact-10 damage, cure/reapply, stronger poison, logout/death and instance departure. The setter itself is already fixed.

Acceptance: selected target retained in a crowded area, exact accepted-target caps, rejected candidates not exhausting caps, splash/zero-hit distinctions, poison cure/reapply producing one owned tick stream, and no stale status effects after departure. Exact historical proc rates/durations are a separate research task.

### 2. Player-worn equipment effects

**Source-confirmed implementation gap:** `Equipment.barrowsSet` recognizes six sets, but the active combat search finds only the Dharok call in `MeleeFormulae.java:116`. The completed `BarrowBrother` implementation applies to NPC brothers; it did not implement player Guthan healing, Verac bypass, Ahrim Strength drain, Karil Agility drain or Torag energy drain.

Implement those player set effects with full-set/degradation recognition and actual-impact tests. Source searches also found no Berserker-necklace combat effect in the reviewed paths. Add it to a dated equipment inventory rather than assuming its item bonuses implement the effect.

`EquipmentEffects.java` uses exact names with simple trailing-s normalization for Slayer and a short explicit undead list for Salve/Onyx. Broaden this to verified NPC/task families; current support is partial, not absent. Audit stacking with Void, Salve, Slayer gear, Dharok and specials. Review Staff of Light rune saving (`MagicAction.java:92`, currently 1/8), dragonfire-shield charging (TODO in `CombatUtils.java:471`), charges/degradation, Ava restrictions/recovery and self-contained ranged weapons.

Acceptance: each set effect has positive/negative/overkill/immunity cases, broken/incomplete sets cannot proc, eligible task variants receive bonuses, and exclusions do not stack.

### 3. Finish prayer/bolt/formula calibration

These are mostly **known provisional implementations**, not proof that every retained number is wrong:

- Deflect still uses a one-in-three roll with an explicit invented-chance comment (`Player.java:1970`). Audit probability and reflected-damage stage together.
- Sap/Leech rates, buildup/decay, Turmoil warm-up/retention, target transitions and magic-defence curse contribution need specifications and state tests. Flat Turmoil additions and prayer drain indexing are already fixed.
- Ranged Void retains provisional 20% final damage; determine effective-level versus final-damage placement and rounding. Rigour retains the early-era 20% value; separate ranged accessors already exist.
- Enchanted bolts still share the 25% gate (`CombatUtils.java:507`). Diamond defence reduction, Dragonstone protection, Pearl fiery-target rules, individual proc rates, Ruby launch-versus-impact HP policy and remaining exceptions need sourced calibration.
- Confirm shared accuracy/max-hit benchmarks, Dharok scaling and hidden special multipliers against the chosen date. Existing simulation pass counts establish consistency, not historical authenticity.

Do not substitute modern OSRS/RS3 values merely because they are easier to obtain.

### 4. Remaining specials, resources and reach

- BGS drain is still player-only (`impl/specs/Warstrike.java`, `drainStats`). NPC stat drains need an explicit mutable-stat contract and boss exceptions.
- Follow the remaining list in SPECIAL_ATTACK_SHIELD_FIXES.md: granite-maul instant attack/energy interactions, spear pushing, staff variants, Morrigan/chaining/continuous effects, claw accuracy/minimums, godsword hidden damage and Korasi extra-target range/multiplier.
- Complete damage/XP/effect ownership for all bespoke handlers; selected follow-up hits are already migrated. Snapshot stance/rate/gear where the intended contract requires it. Audit chinchompa and chained/multi-target paths individually.
- Reach remains global: `CombatType.java` uses ranged 10 and magic 15, and `RangeWeapon.java` has no reach field. Add verified weapon-specific ranges, longrange caps and movement tests for chasing, corners, walls and large NPCs. Earlier clipping/cross-plane changes should remain.
- Review food/drink/attack timing, potion effects, charge-based weapons and resource cancellation against existing custom policy. Food behavior was reviewed and deliberately retained, not authenticated as historical.

### 5. Bosses, packed data and spell coverage

- `CorporealBeast.java:22` still returns incoming damage unchanged in its override. This is a follow-up audit lead, not proof that no other caller could mitigate Corp damage. Establish its weapon restrictions, stats, core behavior and prayer exceptions end to end.
- Audit dragons/KBD/dragonfire, God Wars, Tormented Demons, Jad and Nex phase mechanics separately. Shared formula improvements and instance ownership do not certify encounter max hits, accuracy, immunities or attack selection.
- Complete typed versus raw damage contracts: the special/shield patch puts shields before protection for supported `Damage` snapshots, but raw pre-adjusted hits cannot reconstruct that original amount. Audit direct HP writes, reflection and callbacks per caller.
- Packed item/NPC bonuses remain incompletely verified. Extend beyond the corrected ammo/absorption subset to throwing weapons, chinchompas, crystal/degrading forms, NPC stats and attack bonuses.
- Actual combat registration is `data/xml/MagicSpells.xml`, loaded by `SpellContainer.java:40`. God spells, Iban Blast and Magic Dart are absent from that registration; check alternate routes/requirements before implementing. Later-2011 spells/equipment require a dated scope decision.
- `ServerThread.java` still replaces its executor when its queue backs up. Instance load tests do not certify end-to-end combat tick cadence under server load; separately measure delayed impacts and world-cycle ownership under sustained combat.

## Live validation still open

- Elysian excessive PvP reduction reported by the owner remains **unreproduced**, not resolved. The owner ruled out Staff of Light. Keep 70% activation/25% reduction unless evidence supports a change. Capture full `::combatdebug` hit/shield lines with fixed attacker stats, first shield only, then protection, then soaking armour.
- Check Divine Prayer cost at high/low/zero Prayer and compare actual HP loss through ordinary, special and boss damage routes.
- Confirm staggered DDS/claw/shortbow/dark-bow HP loss, SGS/BGS/ZGS impact effects, Korasi large targets/Magic protection and departure before delayed hits.
- Retest fixed-gear triangle/accuracy/max-hit cases and equipment switching with godmode off; then Nex pulls/ice/phase transitions and all brothers.
- There is recorded live confirmation of the earlier soaking examples and a functioning Barrows run. Do not describe all prior work as wholly untested live. Those observations do not complete the full acceptance matrix.
- Reports record staged runtime classes requiring restart at their respective implementation times. This review did not inspect the running JVM, verify current binary hashes or establish whether the owner has since restarted. Verify loaded build before attributing live results to any particular patch.

## Original 18 findings reconciled

| Original finding | Current disposition |
| --- | --- |
| 1 soaking HP | Implemented; limited live examples recorded |
| 2 incoming style | Implemented for shared typed pipeline; bespoke contracts remain |
| 3 ranged aggregation | Implemented; broader data audit remains |
| 4 formulas | Reconstruction implemented; historical calibration remains |
| 5 enchanted bolts | Partially corrected; gate/effects/exceptions remain |
| 6 ammo | Ordinary and selected specials corrected; remaining recovery/charge paths need audit |
| 7 Miasmic | Spelling fixed; wrong executor and cast-time status remain |
| 8 poison | Setter fixed; proc/timing/lifecycle remain |
| 9 ranged recoil | Implemented |
| 10 prayers/curses | Substantial partial implementation; remaining state/rates/ordering |
| 11 Void/Barrows equipment | Melee/mage Void and Dharok changed; ranged Void provisional; other player sets remain |
| 12 equipment modifiers | Slayer/Salve implemented with limited recognition; inventory/stacking remains |
| 13 freezes/Teleblock/AoE | Ice and Teleblock lifecycle fixed; generic AoE remains; custom Teleblock duration preserved |
| 14 reach/movement | Clipping/plane/cap fixes implemented; weapon reach/chasing remains |
| 15 specials | Selected handlers corrected; not full coverage |
| 16 entry points/timing | Shared and selected special corrections implemented; bespoke paths/snapshots remain |
| 17 PvM/data | NPC magic index fixed and encounter-specific patches exist; broader audit remains |
| 18 timing/content | Partial follow-up; spell inventory, consumption calibration and real load/live coverage remain |

Recommended next patch: item 1 (multi-target selection plus status lifecycle), followed by item 2 (player-worn equipment effects). Run historical calibration alongside implementation only where an exact numeric choice is required. No gameplay changes are made by this review.
