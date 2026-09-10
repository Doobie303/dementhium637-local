# Combat enhancements: batches 1-5

Implemented 2026-09-08 following the owner's request to make the five recommended updates. This is a completed corrective batch across all five areas, not certification of every 2011 mechanic. The original COMBAT_2011_REVIEW.md remains a historical snapshot.

## 1. Prayers and curses

- Prayer drain now reads the Prayer equipment bonus instead of the stab-defence slot (Skills.PRAYER was incorrectly used as a bonus index). +30 Prayer doubles duration in the implemented reconstruction.
- Turmoil's stolen Attack/Defence/Strength levels are separate flat additions, capped at 14/14/9. They no longer inflate percentage multipliers or erase unrelated incoming curse penalties when cleared. Target changes refresh the additions; the curse display includes them.
- Ranged accuracy and strength now have separate prayer accessors. The existing early-era Rigour 20% value is retained; modern OSRS 23% strength was not imported without period evidence.
- Existing protection and reflection regression checks pass. The legacy Deflect activation probability, Sap/Leech timing and Turmoil warm-up/retention duration are not historically certified by this batch.

## 2. Divine and Elysian

- Typed melee/ranged/magic shield reduction is centralized at damage impact, so direct typed boss hits also use it and ordinary hits do not apply it twice.
- Divine reduces 30%, limited by available Prayer. The server stores Prayer in 0-99 units: absorbing 150 life points costs 7.5 Prayer. Low Prayer permits only the affordable reduction and cannot go negative.
- Elysian retains a 70% chance of 25% reduction. The seeded regression observed 6,926 procs in 10,000 trials.
- Godmode/hit immunity causes neither HP loss nor shield prayer cost. Untyped encounter damage remains distinct; dragonfire retains its existing separate mitigation path.
- Current typed pipeline is caller prayer adjustment, then shield, then armour absorption. Historical shield/prayer ordering and all bespoke untyped boss attacks remain follow-up work. Do not describe the order as fully authenticated 2011 behavior.

## 3. Equipment effects

- Target-aware Slayer bonuses now feed normal accuracy and damage calculations: melee 7/6, full-helmet/focus-sight ranged 15%, full-helmet/hexcrest magic 15%. Black masks and basic Slayer helmets no longer boost all styles.
- Removed the old NPC-side 15% damage addition to prevent double application. Completed tasks and PvP receive no task bonus. Task names tolerate this server's singular/plural variants.
- Salve and Salve (e) provide melee-only undead bonuses without stacking with the task-helmet effect. Undead recognition is an explicit limited name list; it is not a complete NPC taxonomy.
- Dharok scales by the missing fraction of maximum life points. This follows a pre-EoC reconstruction; competing historical/community formulas remain a confidence limitation.
- Existing Void melee/mage checks remain green. Ranged Void's provisional 20% damage is unchanged because period evidence conflicts. Explicit-cap special overloads and bespoke equipment effects still need individual calibration.

## 4. Bolts and ammunition

- Ruby bolts replace the old triple-weapon-damage effect with 20% of target current HP (1,000-LP cap), with a 10% current-HP sacrifice on a damaging impact. Below 10% maximum HP, the effect does not activate. Launch-time HP determines the target damage; the cost resolves at impact.
- Onyx healing, Sapphire transfer, Topaz stat loss and Emerald poison now resolve through a one-use impact callback based on actual damage. Onyx is disabled against the recognized undead list. Sapphire cannot transfer Prayer the victim did not have.
- Fixed PoisonManager's setter, which previously ignored false and always enabled poison susceptibility.
- Ordinary shots reserve ammunition before projectile travel. Non-droppable Karil bolt racks and hand-cannon shots are consumed. Two-shot ammunition counts and repeated calls cannot double-debit a shot. Changing equipment cannot debit the replacement stack.
- Corrected 13 ranged specials to use their own RangeData for ammunition accounting, with missing-ammo checks. Magic shortbow, dark bow and hand cannon special debits are directly tested. Aimed Shot's old separate deletion was removed to prevent duplicate charging.
- Fixed repeated ground-arrow stacking using an item amount as a list index, and limited merging to the same owner's private stack.
- The old 25% enchanted-effect gate, per-gem tuning, Diamond defence reduction, Dragonstone protection exceptions, Jade timing, ordinary poisoned-ammo timing and Ava retrieval details remain provisional. No modern proc table was silently substituted.

## 5. Timing and movement

- Ice Rush/Burst/Blitz/Barrage freeze at impact, not cast time. A successful zero-damage hit can freeze; a splash cannot. Freezes stop walking without cancelling the victim's attack.
- Freeze refresh and thaw immunity use a shared check, including godmode/hit immunity. Durations are 8/17/25/32 ticks with 5 ticks of thaw immunity. Prayer no longer inconsistently halves only selected ice spells. Tick rounding and client presentation still need live acceptance.
- Fixed the misspelled Miasmic time attribute in combat cooldown handling. Its existing 1.5x slowdown magnitude is retained pending historical calibration.
- Cross-plane attacks are rejected. Running-melee shortcuts now require clipping checks from both ends; ranged longrange cannot extend the existing reach beyond 10 tiles.
- Reading a ranged cooldown no longer creates a new damage roll or triggers bolt effects; shot data is created in commenceSession.
- Food delay was reviewed without changing private-server behavior. Weapon-specific reach, chasing distances, delayed second-hit HP timing, Teleblock duration/impact and special-attack XP/effect timing are still individual follow-ups. The AGS's legacy accuracy multiplier is also unaudited. These are not silently marked resolved.

## Validation and deployment

Compiled with Java 8 compatibility. All nine suites passed against this batch:

- CombatEnhancementsRegression: 46 assertions plus 10,000 Elysian trials.
- CombatFormulaRegression: 90 checks and 180,000 generated attacks.
- CombatFoundationRegression: 117 checks.
- CombatBalanceRegression: 48 checks and 200,000 samples.
- EquipmentAbsorptionRegression: 102 checks.
- NexMovementRegression and NexIceRegression: passed.
- BarrowsRegression: 711,204 checks; BarrowsFaceRegression passed.

Commands: build/combat-enhancements/compile.ps1. Logs and changed-source manifest: build/combat-enhancements/. Source/runtime backups: build/combat-enhancements-before/. Runtime deployment manifest: build/combat-enhancements/staged-classes.csv. Staged classes are hash-verified. No server restart or player save edits were performed.

Godmode, personal XP rates, private Barrows loot boosts and Primal customization remain intact. No Summoning-specific source was edited; shared combat dependencies naturally affect callers.

### Live acceptance after restart

1. With godmode off, take a known typed hit using Divine at high, low and zero Prayer; compare HP and Prayer drain, then repeat with Elysian.
2. Compare Slayer helmet hits on/off task and Salve against an ordinary skeleton. Check Turmoil display when switching strong/weak targets.
3. Fire Karil/cannon ammo and use magic shortbow/dark bow specials. Check stack decreases, repeated ground drops and swapping ammo while a projectile travels.
4. Fire Ruby/Onyx bolts at a durable target; verify HP cost/healing on impact. Existing Nex damage caps still apply.
5. Cast all four ice spells, including protected zero-damage hits; check frozen movement, continued retaliation and thaw immunity.

## Research notes

- Archived [Ancient Curses knowledge base](https://2011.rs/kb/prayer_the_ancient_curses): Turmoil structure and caps; does not establish an exact Deflect proc rate.
- Archived [Prayer basics](https://www.2011.rs/kb/prayer_the_basics): early Rigour context.
- Period player measurements: [Prayer bonus](https://forum.tip.it/topic/203306-prayer-bonus/), [Power slaying](https://forum.tip.it/topic/271817-power-slaying/).
- Pre-EoC community snapshots: [Divine spirit shield](https://wiki.darkan.org/Divine_spirit_shield), [maximum melee hit](https://wiki.darkan.org/Maximum_melee_hit). These are reconstructions, not published Jagex engine code.
- Archived [Crossbows and bolts](https://2011.rs/kb/ranged_crossbows_and_bolts): Ruby HP fractions/minimum health, Onyx undead exclusion and effect descriptions. It does not publish the exact proc table used by the engine.
- [October 2011 ranged Void testing](https://forum.tip.it/topic/303424-range-void-actual-effects-its-clearly-not-10/): reason to retain an explicit confidence limit.

