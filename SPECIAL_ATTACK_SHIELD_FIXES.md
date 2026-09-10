# Special attacks and spirit shields - 2026-09-08

Owner requested the next combat priority (special attacks), plus investigation of Elysian/Divine. The reported Elysian symptom was excessive reduction with no visible activation during PvP. The owner explicitly confirmed that no Staff of Light had been equipped in that test. Do not present the separate Staff of Light defect as the proven explanation of the owner's symptom.

## Elysian investigation and shield changes

Controlled tests exercised 24,000 actual player-to-player hits through Damage.getDamage and DamageManager: 4,000 per attack style, with and without the corresponding protection prayer. Elysian had no armour-soaking bonus in the loaded item data, and its ordinary defence bonuses matched the pre-EoC reference (63/65/75/2/57, +3 Prayer). No double reduction was reproduced.

For an unmitigated 400-LP hit, Elysian alone delivered either 400 or 300; protection plus Elysian delivered either 240 or 180, before armour absorption. Observed proc counts were 2,782-2,832 per 4,000 hits. Mean unprotected damage was 329.2-330.45; protected mean was 197.925-198.06. Prayer remained unchanged by Elysian. Its 70% activation/25% reduction is retained; the tests do not prove every bespoke boss or special behaves identically.

Additional tests compose Elysian with actual Torva absorption. No arbitrary nerf was made to explain an unreproduced live symptom.

- `::combatdebug` now emits `[Shield] Elysian activated.` or Divine, and incoming-hit output separates `shield=` from `soaked=`. This is diagnostic feedback, not a newly invented historical proc animation. Zero armour soak does not mean the spirit shield failed.
- Normal typed Damage objects retain their pre-protection input and launch-time protection/Staff-of-Light stages. At impact, spirit shields now precede those stages. Divine's Prayer charge is therefore based on original damage: a 500-LP PvP hit with protection becomes 350 after Divine, then 210 after prayer, costing 7.5 server Prayer units. With only 1 Prayer, it becomes 480, then 288, costing 1.
- Explicit later setHit overrides invalidate that snapshot. Integer/raw bespoke hits retain their caller-adjusted contract; those callers cannot reconstruct an original prayer-adjusted hit without more metadata. Dragonfire retains its separate path. These are explicit limits on shield-order coverage.
- A resolved Damage object cannot be applied a second time. Impact callbacks compose instead of replacing each other.
- Staff of Light removal previously cleared `meleeImmunity` rather than `staffOfLightEffect`. Equipment refresh now clears the correct effect if the staff is absent, and player mitigation checks that it is still wielded. This closes a real excessive-reduction exploit, but the owner's reported test did not use the staff.
- Arcane/Spectral retain ordinary equipment bonuses; modern RS3 universal spirit-shield damage reduction was not added.

## Special-attack corrections

- Six special handlers now store their second hit on Interaction, instead of the shared player `secondHit` attribute: DDS, magic shortbow, Zamorak bow, dragon halberd, Saradomin sword, plus dark bow's existing RangeData pair. Overlapping attacks cannot overwrite another attack's pending hit.
- Added SpecialHits for actual server-tick delays, actual-impact XP, shared reflection and one-shot resolution. DDS, magic shortbow, Zamorak bow, halberd follow-up, Saradomin lightning and dark bow second hits resolve one tick later. Claws resolve the first two hits immediately and the latter two one tick later. This replaces hitsplat-only delays that removed HP immediately. Exact client animation alignment still needs live acceptance.
- Pending hits respect death, godmode, hit immunity and instance revisions/admission. Managed-instance work is registered with the owning session for cancellation on close.
- Existing special XP calls now defer to impact and use actual applied damage. Duplicate awards are suppressed; range primary/second hits are registered too. Legacy custom Magic XP paths in Korasi remain separately handled. Full XP stance/rate snapshotting and every bespoke special effect remain follow-up work.
- All four godswords now use a normalized 2x accuracy multiplier and neutral defence multiplier, replacing arbitrary 4.5499/1.12/1.139/1.175 values and 0.998 defence factors. Damage multipliers remain AGS 1.25, BGS 1.1, SGS 1.1, ZGS 1.0. The 2x normalization is a reconstruction, not an independently measured January-2011 engine constant; sources disagree about hidden godsword damage stages. Do not describe 1.375 AGS as implemented.
- DDS accuracy/damage multipliers normalized to 1.15. Both hits now receive the shared reflection treatment, including the previously omitted second-hit Vengeance.
- Claws use raw hit success for fallback selection, not a prayer-reduced amount. Corrected the second-success split, removed the arbitrary third-success +10 LP and the 1.4 damage/defence inflation, and use 1.5 damage for a fourth-success roll. All four hit entries are initialized; all-fail chip damage belongs to the fourth entry. Exact accuracy/minimum-roll distributions remain reconstruction limits.
- SGS healing and Prayer restoration occur on a damaging impact, using actual loss and existing 100-LP/5-Prayer minima. BGS's existing player-stat drain order now resolves from actual impact. ZGS freezes on impact through CombatStatus without cancelling retaliation or bypassing thaw immunity.
- Saradomin sword's lightning roll is now uniform 50-150 LP, replacing a biased 0-160 roll whose low results were reassigned to 50-61.
- Seercull no longer reads the unrelated/null Interaction.damage when applying its Magic drain; it reads its actual ranged result.
- Korasi no longer rejects large NPCs. Mitigation is Magic while melee reach remains. Single-target range includes both endpoints without rejection-loop sampling; multi-hit damage halves from the same primary roll. It requires both source and victim to be in multi before selecting extra targets, and delayed entries validate context before graphics/reflection/XP. Its legacy multi-target range and first-hit multiplier remain separate calibration work.

This is a corrective special-attack pass, not certification of all 37 handlers. Examples still needing separate specifications include BGS NPC stat drain, staff variants, exact godsword hidden multipliers, claw minimums/accuracy, granite-maul instant-energy interactions, spear pushing, enchanted-bolt exceptions, Morrigan/chaining/continuous effects, and other legacy effect timing. No unsupported authentic shield animation was added.

## Validation

Java 8-compatible compilation passed. All 19 suites passed: special/shields, PvP, combat foundation/formula/balance/enhancements, equipment absorption, Nex ice/movement, Barrows/face, instance foundation/lifecycle/integration/content/party/operations/load, and Fight Caves instances. After final Korasi replay/departure guards, special/shields, PvP and instance integration were rerun and passed.

SpecialShieldRegression: **26,075 checks**, including 24,000 typed PvP shield hits, 1,000 claw attacks/split checks, Torva composition, Divine order/low Prayer, Staff of Light removal, delayed DDS, second-hit isolation, actual/duplicate XP, immunity, changed instance revisions and Korasi Magic protection/large-target acceptance. The tests use the real cache and headless players; they do not reproduce the owner's client session.

Other retained evidence: PvP 9,568; equipment absorption 238; combat foundation 117; formulas 90 plus 180,000 generated attacks; balance 48 plus 200,000 samples; enhancements 46; Barrows 711,204; instance load 16,200 sessions/46,000 measured cycles.

Reproduce with build/special-shields/verify.ps1. Logs, changed-source list and staging manifest are in build/special-shields. Prior source/runtime families are backed up under build/special-shields-before. SpecialHits is NEW in this batch; a source snapshot there is intermediate work, not a preexisting implementation. Runtime rollback should follow staged-classes.csv, including entries without previous binaries.

Godmode, personal XP rates, custom PvP/EP policy, private Barrows boosts, existing instance features and the Summoning/Dungeoneering deferrals are preserved. No account saves, cache or client files were changed. Compiled runtime classes are staged and hash-verified; restart is required. No automatic restart was performed.

## Live acceptance

Restart, then enable `::combatdebug` on the Elysian wearer with godmode off. Start with no protection prayer and no soaking armour, then add the matching prayer and finally armour. Capture complete `[Hit]` and `[Shield]` lines rather than only the `soaked=` value. Keep attacker weapon/stats fixed. The reported unexplained PvP over-reduction remains open until that live evidence is available.

Test DDS, claws, shortbow and dark bow for staggered HP loss and equipment switching; SGS/BGS/ZGS for impact effects; Korasi against a large NPC and a player praying Magic; and instance exit/death before a delayed second hit. Visual timing and untested special exceptions may still need tuning.

## Historical references and confidence

- [Archived Jagex special-attack guide](https://www.2011.rs/kb/special_attacks): effect descriptions, godsword advertised damage, lightning range, Staff-of-Light removal and Korasi behavior. This mirror contains mixed-era text and does not publish every hidden formula.
- [Pre-EoC Elysian snapshot](https://wiki.darkan.org/Elysian_spirit_shield): proc rate, reduction and equipment bonuses.
- [Pre-EoC Divine snapshot](https://wiki.darkan.org/Divine_spirit_shield): original-hit Prayer cost, prayer ordering and limited Prayer coverage. Its displayed Prayer units require conversion to this server's 0-99 scale.
- [Pre-EoC special-attack snapshot](https://wiki.darkan.org/Special_attack): claws fallback/split descriptions.
- [Dragon dagger snapshot](https://wiki.darkan.org/Dragon_dagger): reported January 2011 staggered-hit change; exact tick/visual alignment is not independently authenticated.
- [Armadyl snapshot](https://wiki.darkan.org/Armadyl_godsword) versus [maximum-hit reconstruction](https://wiki.darkan.org/Maximum_melee_hit): conflicting advertised/hidden damage descriptions are intentionally not treated as conclusive proof of one universal 2011 formula.
