# Combat overnight implementation - 2026-09-09

The owner authorized consecutive work through the combat enhancement steps while asleep, with major adjustments noted for morning review. Work proceeded through steps 1-4 and stopped before step 5, as announced at the start. This is a corrective implementation across those steps, not a claim that all historical calibration or all equipment/special coverage is finished. COMBAT_MORNING_DECISIONS.md explicitly lists the remaining decisions and incomplete areas.

## Step 1 - Target selection and hit-owned statuses

- Generic AoE limits now mean total accepted victims. The selected victim is considered first; duplicate, dead, other-plane, out-of-range, non-multi and unattackable candidates cannot consume accepted-target slots. Instance admission is checked. Ancient burst/barrage callers now explicitly request nine total targets; chinchompas request nine. Player/NPC candidate pools remain separate. Caller-specific limits still apply (for example Korasi's additional cap).
- All four Miasmic handlers now reset the caster, not the victim, when the required staff is missing. Slow and immunity move to a one-use successful-contact callback. Successful zero damage can slow; splash, godmode/hit immunity and rejected hit contexts do not. Existing durations/slow magnitude are retained.
- Ordinary melee/ranged and player special weapon poison is attached centrally to the pending Damage object with the originating weapon/ammunition captured. Legacy cast-time applications were removed. Smoke poison also uses actual-impact callbacks. Retained poison proc policy is explicitly listed in the morning notes.
- Jade stun now resolves on a damaging impact. Shadow's existing proc selection is retained, while its Attack reduction now runs on successful contact rather than during casting.
- Poison has one owned tick stream. Curing invalidates/stops the old task; stronger poison upgrades the existing stream without scheduling another; weaker poison cannot downgrade it. Ten-LP poison is consistently active and its final tick clears the client icon. Death, offline players and instance-revision changes stop the stream. The existing progression/cadence is retained. World-owned cleanup lets revision invalidation clear poison state after instance departure.
- Damage contact callbacks compose, run once and are distinct from positive-damage callbacks. DamageManager suppresses contact effects for immune/healing/shielded-minion resolutions. Existing hit and effect replay guards remain.

## Step 2 - Player equipment effects

- Added player Guthan healing, Verac accuracy/protection bypass, Ahrim Strength drain, Karil Agility drain and Torag run-energy drain. Full usable sets are captured at hit creation. Guthan uses actual HP loss; positive-damage effects cannot benefit from immunity/overkill. Current proc reconstruction and NPC limits are documented in the morning notes.
- Verac bypass is a parameter of the individual hit, not a shared player flag. It does not disable godmode, spirit shields or armour absorption. The existing NPC-brother bypass remains separate and unchanged.
- Added the Berserker necklace's 20% damage modifier for four obsidian melee weapons (6523, 6525, 6527, 6528). It is damage-only and does not boost thrown rings, accuracy, unrelated weapons or the shared Slayer accuracy multiplier. Existing item penalties remain in equipment data.
- Expanded explicit Slayer-family matching for giant bats, mutated bloodvelds, mighty banshees and spectre spelling; expanded the explicit undead list for selected skeletons/hands/banshees. Avoided broad substring matching that could grant bonuses to unrelated NPCs.
- Kept Staff of Light's 1/8 rune saving. Removed the stale magic Slayer TODO because the actual effect already exists.

## Step 3 - Curse accounting and bolt exclusions

- Reworked curse-owned accounting: first application 10%, per-curse victim caps 20% Sap / 25% Leech, no Sap caster boost, Leech dynamic boost capped at 5% across targets plus its existing static 5%. Release/deactivation restores precisely the values owned by that curse and is idempotent. It preserves unrelated incoming penalties, including another caster's contribution. Target references and boosts are cleaned up across invalid instance contexts; stopped targets are pruned during prayer ticks.
- Sap Spirit now drains special energy without transferring it to the caster; nonexistent projectile IDs are not emitted. Godmode/hit immunity blocks harmful curse applications. Familiar-specific behavior was not expanded.
- Dynamic magic curse modifiers now affect the Magic-level contribution to magic defence. Ordinary magic attack-prayer boosts are not indiscriminately applied to the entire defence roll.
- Dragonstone exclusions now return ordinary ranged damage when protected; the bolt no longer sends its whole ranged hit through the dragonfire protection reducer. Recognized dragon targets, anti-dragon shields and active antifire suppress the enchantment. Pearl recognizes additional water-staff variants. The common activation gate and existing damage multipliers remain provisional.

## Step 4 - Specials, resources and timing

- Shared melee/ranged XP callbacks capture the attack stance and personal combat rate at creation, including ordinary attacks and chained/multi-hit special paths using this contract. Chinchompa XP waits for each actual impact; its ammunition uses the guarded one-shot debit. Legacy magic XP scope is separately listed.
- Ordinary dark bow second hits now use a real one-tick server delay, with shared context/reflection/XP checks. Previously only their hitsplat was delayed.
- Whip energy transfer now waits for actual damage, subtracts from the victim correctly, credits only available energy and respects capacity. Previously it assigned the victim the drain amount instead of remaining energy, even before impact.
- Healing-bow specials read their actual RangeData hit rather than unrelated/null Interaction.damage. Their timers capture their own amount and stop across invalid contexts. Replaying endSpecialAttack cannot start another timer.
- Morrigan javelin bleed owns its remaining damage and no longer shares the victim's `phantomStrike` amount with another attack. Each tick respects context, death/offline/immunity; actual tick damage awards ranged XP. No extra zero-damage final tick is emitted.
- Rune throwing-axe bounces use owned targets/damage and instance-owned scheduling. Invalid/dead/departed targets stop the chain before additional special energy is charged. Replayed end calls cannot create another chain. Existing range/per-bounce costs remain.
- Granite-maul button handling resolves only its own immediate hit; it no longer recursively invokes CombatExecutor.tick and advances unrelated pending actions. Reach/clipping/admission/duel/energy checks run first. Failed attempts clear the toggle; the normal attack cooldown is preserved by the instant activation route. One Interaction cannot spend/apply the same hit twice. Godmode's intentional special-energy behavior remains.
- Hand cannon aiming countdown belongs to Interaction and releases its projectile once, instead of repeatedly firing after a shared `aimingTicks` attribute reaches zero.
- Dragon hatchet stat drains use actual-impact amounts with level floors. Dragon scimitar closes protections at impact and enforces an eight-tick expiry against normal and quick-prayer activation; its old unconsumed `restrict protection` attribute had no enforcing reader.
- Spear push rejects immune/locked/cross-context targets and uses clipped one-tick forced movement instead of the empty Mob.requestWalk method. Existing duel restrictions and five-tick stun remain; live visuals still require testing.
- Added optional validated per-weapon attackRange definitions, inherited by degraded variants and consumed by CombatMovement with a ten-tile longrange cap. Existing unspecified ranges are retained; population of a dated shorter-range table is explicitly unfinished.

## Verification and limits

Java 8-targeted full active-source and test compilation passes. CombatOvernightRegression has 211 checks covering target selection, status lifecycle, poison replacement, equipment effects, all four usable wear stages and broken-set rejection, curse accounting, bolt exclusion, XP stance capture, special replay, timers and prayer reactivation. The old MiasmicRush class, compiled in an isolated overlay, fails the new existing-API requirement test because it resets the victim; the corrected implementation passes.

All 27 suites passed during combined verification: overnight; foundation/formula/enhancement/balance/special-shield/absorption; Nex movement/ice; Barrows/face; PvP; duel; Gambler plus preview/live/automatic interface; Infernal Cape; TestGear; instance foundation/lifecycle/integration/content/party/operations/load; Fight Caves instances. Final logs and verification status are under build/combat-overnight. The suite names and commands are in verify.ps1. Compile separately with compile.ps1. Tests use the real cache with headless fixtures, not a live client.

Recorded retained evidence includes 117 foundation, 90 formula plus 180,000 attacks, 48 balance plus 200,000 samples, 238 absorption, 9,568 PvP, 2,126 duel, 10,384 Gambler, 414 Fight Caves and 16,200 simulated instance sessions. Special/shield totals include 24,000 PvP hits; minor total-count variation follows random branch coverage and is not a new statistical claim.

The Windows sandbox caused Java's archive-close AccessDenied error; compilation succeeded using the approved local unsandboxed compiler. One later automatic approval review timed out without a safety finding; its allowed retry succeeded. No permission issue remains blocked.

## Runtime, preservation and rollback

Source/test backups: build/combat-overnight-before/src and tests. After the final 27-suite pass, 92 runtime classes from 71 changed source families were staged and SHA-256 verified. Runtime-family backups are under build/combat-overnight-before/bin. Consult build/combat-overnight/staged-classes.csv for exactly which binaries were staged, previous hashes and new hashes; new families have no previous runtime counterpart. source-manifest.csv and source.diff identify this batch against the initial working tree, preserving its pre-existing unrelated work.

The server is not restarted automatically. Restart normally to load staged classes, then perform the morning acceptance checks. No production account saves, packed definitions, cache, client, PvP settings or reward pools were edited. Regression fixtures write isolated build test-account directories. Source files under Summoning and Dungeoneering are unchanged; shared hit/poison/formula behavior naturally affects callers. Preserve newer custom cape/client work and all existing optional save trailers during rollback.

## Evidence notes

- [Archived Barrows reward descriptions](https://www.2011.rs/kb/barrows_rewards_page) support player set-effect types, not an exact published proc probability. The new 1-in-4 setting follows the existing reconstruction and is explicitly provisional.
- [Archived curse descriptions](https://2011.rs/kb/prayer_the_ancient_curses) support Sap/Leech caps and the distinction between draining and boosting. Its activation/residual-recovery descriptions are not fully reproduced by the retained cadence/deactivation policy.
- [Archived enchanted-bolt descriptions](https://www.2011.rs/kb/ranged_crossbows_and_bolts) support Dragonstone/Pearl exclusions but do not establish individual engine proc rates or the full fiery-NPC inventory.
- [Contemporary Berserker-necklace discussion](https://forum.tip.it/topic/112494-onyxes-ur-choice/) supplies period observations of the 20% damage behavior; modern obsidian-armour additions were not imported.
- [Archived special descriptions](https://www.2011.rs/kb/special_attacks) support the named effects. Exact hidden multipliers, tick presentation and all later variants remain outside this batch's certification.

The original COMBAT_2011_REVIEW.md and COMBAT_REMAINING_WORK.md remain snapshots. This report supersedes only the changes explicitly listed here. Step 5 has not been implemented by this batch.
