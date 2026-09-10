# Boss step 5 — remaining bosses and advanced NPCs

Implemented 2026-09-09. This report supersedes the original BOSS_AND_NPC_REVIEW.md findings only for the changes below. Target remains the provisional early-2011/revision-637 game, with explicit server definitions where historical calibration is uncertain. Existing access, spawn positions/counts, reward tables, godmode, personal XP and developer client are preserved. Summoning-specific and Dungeoneering work remain deferred.

## Implemented

| Encounter | Result |
| --- | --- |
| Tormented demons | Independent offense/protection state, 75% fire-shield reduction, protection changes committed on contact, positive Darklight impact suppressing the shield for 100 ticks, restoration/reset, preserved mutable stat drains across overhead model changes, and telegraphed ground magic on offense switches. Verac protection bypass retains the fire shield. Loot uses the original spawn ID despite overhead transformations. |
| Nex | Active auto-attacks now use typed damage and actual LP removed for blood healing, Soul Split, Turmoil and smoke poison. Shields, absorption, immunity and overkill settle first. Each ranged target has its own captured encounter/instance context and phase. Blood sacrifice validates its original Nex life and heals only actual damage. Independent action sessions replace reusable mutable state; the inactive duplicate attack implementation was removed. |
| King Black Dragon | Four independently owned breaths. Poison, five-tick freeze and stat drain happen at a valid impact, with protection/antipoison changes during flight respected. No random per-tick target reassignment. |
| Chaos Elemental | Typed primary projectiles, including the distant melee-labelled attack, and separate delayed teleport/disarm specials. Disarm transfers the exact item and wear metadata atomically from its actual equipment slot; full inventories resist it. Teleports choose an in-arena, clipped, visible, unoccupied-by-NPC destination or leave the player in place. |
| Frost dragons | All 19 existing ID-51 spawns now dispatch to the dedicated controller with melee, magic and dragonfire. Projectile 2465 is present in the supplied cache and is set in both packed and editable definitions. No new spawns or access gates. |

The four AdvancedNPC controllers reuse the tested EncounterNPC target, footprint movement, generation-owned pending attacks, empty-arena reset and death/respawn lifecycle from step 4. They retain valid targets instead of randomly replacing the tank each tick. Their selected attack belongs to its individual session. Old attacks cancel on invalid player/arena/instance context or NPC reset/death. Packed respawn values are retained with the existing death-animation interval.

Only packed record 51 changes, and only its projectile field (-1 to 2465); the other 13,487 slots are unchanged. custom_npcs.xml adds the ID-51 handler. Shared Damage.java has one targeted dispatch branch to pass protection-bypass intent to tormented demons; other victims retain their existing route.

## Explicit fight definitions and evidence

Ticks are 600 ms. The Darkan pages are pre-EoC wiki mirrors, including later-2011/2012 material, rather than certified snapshots of the exact revision. Native animation/graphic presence was checked against this cache; presence alone does not certify visual alignment.

- **Tormented demons:** 3,260 LP; six-tick autos; melee cap 189, ranged/magic cap 269. Begin Protect from Melee and ranged offense. Switch offense to a different style every 27 engaged clock ticks with a three-tick roar warning; a three-by-three ground burst can be dodged and deliberately does not require cover line of sight. Each incoming style has a 310 pre-shield contact-damage threshold, with successful contacts below 20 contributing 20; switching clears all counters. Matching protection blocks damage/counting unless bypassed. The shield reduces damage to one quarter and positive Darklight melee damage suppresses it for 100 ticks. These mechanics are based on [Tormented demon](https://wiki.darkan.org/Tormented_demon) and [strategies](https://wiki.darkan.org/Tormented_demon/Strategies). **Deliberate server boundary:** the mirror also describes misses contributing 20. This implementation counts successful contacts (including zero), not accuracy misses/splashes; it avoids changing shared miss routing. Exact historical overhead asset pairing, roar probabilities and missed-hit accounting are not certified.
- **KBD:** four-tick attacks, melee cap 250, unprotected breath cap 620. At melee contact, 30% melee; otherwise four equally likely breaths. Toxic breath attempts poison 88; ice has 70% chance to freeze for five ticks; shock has 30% chance to lower one of Attack/Strength/Defence by two. Status probabilities and attack weights are explicit server calibration. [Period mirror](https://wiki.darkan.org/King_Black_Dragon) supports the four breath effects, short freeze and special residual damage; it contains later access/visual additions which were not adopted.
- **Breath protection:** for KBD and frost dragonfire only, a shield or ordinary antifire caps ordinary fire at 100 and KBD specials at 200. Shield plus antifire, or super antifire, blocks ordinary fire and caps KBD specials at 100. Damage is uniformly scaled into the protected range; Protect from Magic applies a further 0.6 multiplier. Spirit shields are evaluated once at impact. This explicit table replaces the old controllers' launch-time/subtractive behavior without changing the shared dragonfire calculator. Poison/freeze/shock are contact effects, so zero damage alone does not suppress them; antipoison/status immunity still applies.
- **Chaos Elemental:** five ticks, primary cap 284; two-thirds primary, one-sixth teleport, one-sixth disarm. Primary styles use 50% magic / 30% ranged / 20% melee-labelled damage. Teleport offset is three to six Chebyshev tiles, constrained by arena safety; disarm selects one occupied equipment slot. The [period mirror](https://wiki.darkan.org/Chaos_Elemental) conflicts between 284 in its body and 295 in its infobox. Retain 284 and document these weights/displacement as our definitions rather than claiming an exact historical reconstruction.
- **Frost dragons:** four ticks; melee 214, magic 250, fire 595. At contact, 50% melee; otherwise equal magic/fire. Use the early-2011 fight: the [period mirror](https://wiki.darkan.org/Frost_dragon) dates the ranged attack and reflection orb to **17 October 2011**. Those later mechanics are intentionally absent. Dragonfire immunity is retained. Attack weights, protection table and exact visual timing are server calibration.
- **Nex:** retain previous phase, cadence and special definitions. Blood ranged/magic leech is rounded 10% of actual damage; Soul Split heals actual/5 and drains floor(actual/50) prayer; final-phase positive hits drain one Attack/Strength/Defence level; smoke poison has 25% positive-hit chance. Her existing 0.6 protection multiplier is preserved, with Divine using pre-protection shield input. Unescaped blood sacrifice heals the actual LP it removes; escaped damage does not heal. Its existing room-wide prayer penalty remains a separate special, now respecting status immunity. These are accounting corrections, not a new certification of every Nex special or historical proc rate. See NEX_REVIEW_AND_FIXES.md for the retained encounter and pending ice/drag/rendering acceptance.

## Validation and release

Manifest: tools/batches/boss-step5-release.json. Explicit compilation targets Java 8 and uses existing runtime dependencies; it is not a full source-tree build.

| Suite | Checks | Relevant coverage |
| --- | ---: | --- |
| AdvancedBossRegression | 20,918 | All attack kinds with normal/protected/godmode/Divine victims; caps, actual impacts, no early/replayed effects; player departure/plane/visibility/revision and NPC reset/death; TD shield/overheads/Darklight/stat preservation/roar; late breath protection and antipoison; atomic disarm and blocked teleport; actual NPC tick dispatch, reset, packed-data isolation, native assets and real spawn regions. |
| NexDamageRegression | 750 | Typed blood/final/smoke/ice effects through shield, prayer, absorption gear, overkill, misses, zero, godmode and offline contexts; actual delayed ranged dispatch with captured phase and stale-life/target cancellation; sacrifice timers with immunity, overkill and replaced/reset Nex. |
| NPCFoundationRegression | 425 | Connected NPC context, stat and delayed-action contracts after controller replacement. |

Total: **22,093 checks**, all passing. Final suite logs contain no exception/error traces. No unrelated accumulated gameplay suites were run. No client/cache authoring or account-save changes.

The initial pre-edit source/data backup is build/batches/boss-step5/before/source. Scope expanded to include Damage.java after implementation began, so build/batches/boss-step5-release/before/source contains a work-in-progress snapshot for the other sources, and a genuine pre-edit Damage.java. Preserve both; do not call the release snapshot a complete original-source backup. The batch runner separately backs up the affected runtime class families before staging, verifies hashes, and records added/removed files in its stage receipts. The data-edit script is a release construction record, not a safe future merge/update tool.

27 runtime classes are staged. Verified runtime backups are under build/batches/boss-step5-release/before/runtime; operation hashes are in staged.json. The passing logs are under build/batches/boss-step5-release/verify-dd0b4287a0fe407595910826c66f589c.

Restart the server to load the staged runtime and definitions. No restart or live playtest was performed here.

## Live acceptance still required

Use ordinary gear with godmode off for fidelity, and two players where relevant:

1. TD: overhead/model/animation pairing, Darklight tell and duration, prayer swaps, roar tile dodge and legitimate cover, correct drops after form changes, empty reset and respawn.
2. Nex: blood healing and final Soul Split with/without shield, sacrifice escape and actual healing, two-player delayed casts/departures; retain earlier ice/drag acceptance checklist.
3. KBD: four breath visuals, late antifire/antipoison protection, brief ice bind, shock drain, sustained tank and reset.
4. Chaos: full-inventory resistance, worn-item metadata and equipment appearance after disarm, edge/obstacle teleports and continued attacks after displacement.
5. Frost: real map positions and large-body routing, melee versus projectile visuals, protection behavior, reset/loot/respawn. Headless map parsing does not establish client rendering or a complete live route through all 19 spawns.

Historical accuracy remains bounded by the explicit definitions above. This completes the step-5 server implementation and targeted checks; live fight acceptance remains outstanding.

