# Boss step 6: fight accuracy and acceptance

2026-09-09. This pass covers active Jad and six-brother combat, with targeted automated acceptance. **Live complete-fight acceptance remains pending.** It does not certify all historical combat calibration or repeat completed boss audits. The active 2745 registry points to TzTokJad; the unused Jad/JadAction implementation was not substituted or edited.

## Implemented

- Jad now creates a separate queued attack session while retaining one encounter-owned healer controller. Style, roll, maximum and completion belong to that attack. Moving into melee reach during a ranged/magic tell cannot change its combat lifecycle or skip the wind-up. Overlapping attacks cannot overwrite one another.
- Prayer still resolves at impact. Under the existing CombatAction lifecycle, launch is tick 0, four execute ticks follow, and ranged/magic impact occurs on tick 5; melee resolves on tick 1. The eight-tick launch cadence is retained. These are server tick boundaries, not a measured client animation duration.
- Jad launch checks both projectile-path directions. His controller now supplies stable magic pursuit reach (the existing 15-tile magic limit), instead of alternating pursuit reach with the previous random style. Pending hits retain launch style and do not recheck cover at impact. Exact large-footprint corner geometry and historical reach remain live/calibration boundaries.
- Jad impact is single-use and checks the original NPC/instance context. Magic splash remains distinct from a successful zero. Ranged tell graphics and healer callbacks reject stale generations. A subsequent attack after an explicit combat reset removes the previous healer set and allows a fresh half-health summon. Managed NPC removal cancels owned tasks before releasing anything; maps remain owned by the existing session.
- Each Barrows attack has an independent session and single-use impact with original-context validation. Ahrim's separate stat spells use successful-contact callbacks (including successful zero, excluding splash). Signature debuffs and Guthan healing use actual positive damage callbacks. Hit immunity and godmode prevent harmful effects; Guthan uses actual LP removed, including lethal damage. Ahrim's separate non-damaging stat spell no longer additionally triggers the damaging set proc.
- Owner-only Barrows combat, Verac's protection/accuracy exception, spirit shields/absorption, haunting, puzzles, saved progress and boosted rewards are preserved. No player save, client, packed-data, world spawn or shared formula source was changed.

## Calibration disposition

The provisional early-2011 baseline is retained. Research checked the [Fight Cave Knowledge Base reproduction](https://www.2011.rs/kb/tzhaar_fight_cave), [Barrows encounter reproduction](https://www.2011.rs/kb/barrows), and [Barrows rewards reproduction](https://www.2011.rs/kb/barrows_rewards_page). These reproduce period descriptions but are not authenticated frozen archives. They support attack tells, reactive protection, healer distraction and the named set effects; they do not establish every hidden probability or exact tick boundary.

No uncertain numeric value was silently promoted to an authenticated historical fact:

| Behavior | Retained implementation / confidence boundary |
| --- | --- |
| Jad | 2,500 LP; caps 970 melee/ranged and 950 magic; eight-tick cooldown; guaranteed melee when adjacent, otherwise equal ranged/magic. Melee selection weighting, caps and exact timing remain reconstruction. |
| Healers | Four at half health, 50 LP each every four ticks within the existing five-tile origin-distance test. A nearby healer targeting the owner stops healing; beyond the existing ten-tile player-distance threshold it resumes seeking Jad. Killing all permits another summon only if they previously restored Jad to full. Retained luring model, not a newly authenticated January-2011 rule. |
| Brothers | Dharok seven ticks, Karil four, others five; 1-in-4 signature proc. Caps remain Dharok 290 plus missing-HP scaling toward 580, Ahrim/Karil 200, Torag 230, Guthan/Verac 240. Hidden stats, exact proc rates and scaling remain provisional. |
| Rewards | Explicit owner customization: Barrows equipment/runes/racks 1.5x and coins 1.25x; cave fire cape plus 16,064 Tokkul. Historical reward normalization is not authorized. |

The broader shared backlog in COMBAT_MORNING_DECISIONS.md (curse cadence/recovery, individual bolt probabilities, Void/rounding, weapon ranges/resources and other inventory work) remains separate. Steps 3–5 retain their documented custom boss definitions. Summoning and Dungeoneering remain deferred. Godmode, personal XP and access conveniences remain intact.

## Selected verification

- BossStepSixRegression: both Jad remote styles, unchanged wind-up while entering melee range, last-tick protection, independent overlapping rolls, replay, reset/offline/plane/godmode rejection, splash, blocked launch and melee contact; all six brothers' immune/ordinary impacts, replay/reset/session isolation and Ahrim contact/splash semantics.
- BarrowsRegression: six effects, ownership, run/save/puzzle lifecycle and boosted reward distribution.
- BarrowsFaceRegression: 30-tick prayer drain and five-tick face expiry, overlay and surface reset.
- FightCavesInstanceRegression: existing real-cache 63-wave progression and two independent sessions, healer tagging/resumption, generation reset/re-engagement, departure/death/reward cleanup and admin restart. Its wave progression uses simulated kills, not complete gear-driven combat fights. Active healer counting now excludes removed/hidden NPCs awaiting session-list pruning.

Compilation is limited to the declared two production families and selected tests/helper. No full source build or unrelated all-suite run is claimed. Compiler execution uses the established outside-sandbox workaround for Windows JAR access. The reset test initially counted stale removed entries; its active-NPC filter was corrected, then the failing suite passed Debug before final Verify.

Release manifest: tools/batches/boss-step6-release.json. Genuine pre-edit source backups: build/batches/boss-step6-release/before/source. The initial boss-step6 preparation is also preserved; it was superseded before edits to correct a suite success marker. Final measured results and staging status are recorded below when released.

Final Verify passed all four suites: BossStepSixRegression 0.993s, BarrowsRegression 0.838s, BarrowsFaceRegression 0.726s, FightCavesInstanceRegression 1.314s. Test execution 3.871s; compilation 1.387s; total including hashes 23.157s. Logs and summary: build/batches/boss-step6-release/verify-f8d8e0fa17354acdb2a82af12d106df0/.

**Five runtime classes from two source families are staged**, with SHA-256 verification and prior runtime backups under build/batches/boss-step6-release/before/runtime. Exact operations/hashes are in build/batches/boss-step6-release/staged.json. No restart was performed. Preserve these backups and all earlier releases; this batch does not alter duel/gamble journals or account data.

## Live acceptance card — not yet performed

Restart normally to load staged classes. Use ordinary gear with godmode disabled; record gear/stats, prayer inputs, tell/impact timing and combatdebug output for discrepancies.

| Check | Required observation | Status |
| --- | --- | --- |
| Jad complete fight | Use `::wave 63`; survive both remote tells, switch immediately before impact and deliberately late, approach during a tell, test melee separately, tag/lure/kill healers, finish and receive rewards once. | Pending |
| Cave rocks/reset | Use waves 3, 62 and 63; approach real rock corners from both sides, check large NPC pursuit and valid cover, exit/die/restart during a tell, re-enter without stale hits/healers. | Pending |
| Two cave players | Start independent runs; restart/exit one while the other fights Jad. Check rendering, overheads, healers and rewards remain independent. | Pending |
| Full Barrows runs | Fight all six, check protection/Verac and signature effects, haunting, maze/puzzle, loot/escape, start another run; logout/reconnect before and after looting. | Pending |
| Two Barrows players | Simultaneous same-crypt/tunnel fights: no foreign attacks, kill credit, prayer drain or loot crossover. | Pending |
| Earlier encounter acceptance | Complete the live matrices in GOD_WARS_CUSTOM_RULES.md, BOSS_STEP_FOUR_FIXES.md and BOSS_STEP_FIVE_FIXES.md: tank/departure behavior, safespots, adds/phases, reset and multiplayer. Existing headless results do not replace these fights. | Pending |

The earlier owner-confirmed Barrows usability and first-wave visibility fix remain accepted for those issues only. No live server was restarted, no client was driven, and no full live fight was observed in this pass. Step 6 stays open for these acceptance results and unresolved historical calibration.
