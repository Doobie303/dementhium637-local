# Boss step 4 — Corp, Kalphite Queen and Dagannoth Kings

Implemented and staged 2026-09-09. The server implementation follows the definitions below; restart and live acceptance remain required. This is not certification of every historical 2011 value or client animation. It supersedes the original boss review only for these encounters and their arena adds.

The owner authorized completing these fights and using sensible server definitions wherever historical evidence remains uncertain. Access conveniences, teleports, entrances, rewards, godmode and personal XP are preserved. One of each King and one main-chamber Corp remain. Familiar-specific mechanics and Dungeoneering remain deferred. No developer-client, account-save or reward-table files were changed.

## Corporeal Beast

Corp now uses an encounter controller rather than its old placeholder action. It has independent melee, direct magic, stat-draining magic, targeted ground splash/splinters, underfoot stomp and an owned dark energy core.

| Rule | Implemented definition |
| --- | --- |
| Profile | 20,000 LP; Attack/Strength 320, Defence 310, Magic 350. Six-tick attack interval; ranged reach 12 tiles from the footprint. |
| Attack selection | At contact, 50% melee; otherwise equal selection among the three magic attacks. At distance, magic only. |
| Caps | Melee 513; direct magic 699; drain magic 499; initial ground splash 399; each splinter 150; stomp 300. Ordinary melee scales down with drained Strength. |
| Protection | Corp magic retains half damage through Protect from Magic/Deflect Magic. Typed hits preserve shield input and actual-damage callbacks. Stomp and core drain bypass protection prayers; godmode and applicable shield mitigation still work. |
| Weapon rule | Magic and spears using stab deal full typed damage. Other melee and ranged typed damage are halved. The weapon/style is captured when the hit is constructed. Typed incoming damage is capped at 1,000 LP. Existing Ruby-bolt calculation caps at 1,000 before Corp's reduction, so its proc reaches 500; its self-cost still occurs only on damaging impact. |
| Drain | One randomly chosen stat: Magic or Prayer. Drain is actual LP removed / 10, minimum one. An already empty chosen stat converts that drain to additional LP damage. Summoning drain is excluded under the existing deferral. |
| Ground attack | Snapshot the target tile, show a ground warning/projectile, strike its 3×3 square after three ticks, then strike three warned splinter tiles two ticks later. Splinters are west/east/north by two tiles and hit only their tile. Moving away avoids them; a different current occupant can be hit. |
| Core spawning | Below 75% LP, check every ten ticks with a 25% chance; at most one live core. Spawn/landing remains inside the room. |
| Core | 250 LP; a 3×3 drain area, 1–100 damage per player every two ticks. Corp heals only the LP actually removed, capped at maximum. A vacated core hops toward a current player every three ticks. Poison slows its pulse to twenty ticks while occupied; vacating releases the pin. It has no additional generic melee attack. |
| Stomp | Overlapping the boss footprint triggers a separate attack, at most every six ticks. |
| Cleanup | Core disappears on death, reset or owner-life change. Empty engaged arena resets after ten ticks. Dead Corp respawns independently after its death-animation interval plus sixty ticks. |

Corp's three magic families, spear restriction and mobile healing core are supported by the [pre-EoC strategy mirror](https://wiki.darkan.org/Corporeal_Beast/Strategies). Core LP and poison slowdown are supported by the [core page](https://wiki.darkan.org/Dark_energy_core). The [Corp page](https://wiki.darkan.org/Corporeal_Beast) supports its LP and main damage caps. These pages include material later than 2011; the exact selection, splash geometry, timers, profile bonuses, drain conversion and ceiling above are explicit server definitions, not claims of a frozen 2011 implementation. Familiar consumption remains deferred.

## Kalphite Queen

Both IDs now dispatch to the same owned two-form controller. The first lethal hit starts a four-tick transition without loot. Form 1160 starts with a fresh 2,550 LP; old attacks cannot cross that transition. Damage credit survives both forms. Only the final kill invokes the existing 1158 reward table, once. Respawn and an empty-arena reset return to form 1158.

Both forms have level 300 combat stats, a four-tick interval and 314 attack caps. At contact, one third of selections are melee; the remainder split evenly between ranged and magic. Ranged/magic attacks roll damage without a separate accuracy miss, use normal protection prayers, and can hit eligible players within eight tiles and line of sight. First-form ranged/magic defence bonuses are 550; second-form melee defence bonuses are 550; other attack/defence bonuses in those slots are 50. Head icons represent these higher defences, not a second incoming damage multiplier or absolute immunity.

Second form creates one owned worker every twenty ticks, up to two, when an eligible spawn tile is available. Workers retain their existing 400-LP profile, use four-tick melee with a 30 cap, and grant no separate encounter reward. They are removed on final death/reset.

Two LP pools, changing defence emphasis, accurate ranged/magic, protection and worker families are supported by the [pre-EoC KQ page](https://wiki.darkan.org/Kalphite_Queen). Exact bonuses, selection weights, transition and worker timings are server definitions. Native sequence IDs exist in the supplied cache; form-two attacks use its existing packed attack sequence 9454. The visual suitability of each style and the transition still need live acceptance.

## Dagannoth Kings and moat

| NPC | Fight definition |
| --- | --- |
| Supreme | Retains the prior 2,560-LP calibrated profile, four-tick ranged and cap 300. Fans attacks across the forward half of its eight-tile reach, oriented toward the primary target. |
| Prime | Retains 2,550 LP, four-tick magic and cap 610. Nearby secondary players in the primary target's 3×3 square can be hit, within reach and line of sight. |
| Rex | Retains 2,550 LP, four-tick melee and cap 280. Requires actual contact; footprint clipping, freeze and arena bounds preserve legitimate rock safespots. |
| Targeting | Rex acquires within five tiles; Prime/Supreme within seven. A valid lured target is retained inside the arena. Each King's death/respawn/reset is independent; another King's death never heals a survivor. |
| Spinolyps | Six IDs (2891–2896) now have real profiles and handlers instead of 100-LP/zero-level fallback data. Surface forms have 750 LP, five-tick attacks, cap 100 and equal ranged/magic selection. Successful magic drains one server Prayer point; ranged contact has a 25% poison chance starting at 68 LP. |
| Spinolyp lifecycle | Stationary moat positions, including when old spawn flags say walking. Suspicious water emerges after eight ticks. Once per life, a surface form below half LP dives for eight ticks, cannot be attacked while submerged, and returns with its remaining HP. Reset restores the profile. Exact duplicate ID/coordinate spawns are removed; unique positions remain. |

The existing King profiles come from BOSS_BATCH_ONE_FIXES.md and remain provisional server calibration. The [period strategy mirror](https://wiki.darkan.org/Dagannoth_Kings/Strategies) supports distinct styles, multi-target pressure, luring and Spinolyp supply pressure. The [OSRS strategy page](https://oldschool.runescape.wiki/w/Dagannoth_Kings/Strategies) also supports Prime splash and Supreme facing. Geometry and acquisition radii here are our definitions.

The [Spinolyp mirror](https://wiki.darkan.org/Spinolyp) supports 750 LP, poison and water forms. The exact combat levels/bonuses (Attack/Strength/Defence/Magic 50, Ranged 75, zero bonuses), poison chance and dive timing are custom calibration. Submerged forms remain stationary and non-attacking; the mirror's uncertain moving-water melee behavior was not copied. This is an explicit simplified water-phase definition, not a historical equivalence claim.

## Shared behavior and scope

EncounterNPC owns local timers, attack generation, cleanup and respawn. Every queued targeted impact validates its own source and victim life, visibility, online status, plane, arena and instance boundary. Ground effects intentionally query current occupants. Attacks cannot replay after reset/death or leak outside the arena. The shared NPC context, movement dispatch and tick task recognize these encounter controllers; other NPCs retain their existing routes.

Actual landscape parsing verifies the unchanged Corp/KQ/King spawn footprints are walkable and within their arena rectangles. Clipped stepping checks the entire NPC footprint. It deliberately does not find a path around a blocked rock, preserving safespots. These controllers target the current shared world arenas; owned adds are not enabled for managed-instance copies. No instance migration was added.

## Validation and release

Selected checks only, under tests/TESTING.md:

- BossStepFourRegression: **20,364 checks passed**. Real registry/data and native asset presence; only nine intended packed records changed; unique boss/add spawns; all attack families; actual mitigation and stat effects; replay and boundary cancellation; real Ruby-bolt entry point; core healing/poison/hopping/cleanup; KQ lethal credit, transition, workers and final reward ownership; King areas, freeze/clipping and independent respawn; actual NPCTickTask lure routing; Spinolyp forms and real boss spawn landscapes.
- NPCFoundationRegression: **425 checks passed**. Shared NPC mutable levels, damage/life ownership, target retention, BGS actual-damage drains and old timer cancellation.
- Explicit Java 8-compatible compilation of the nine changed production families and these test sources/helpers passed. A full server compilation/all-suite gameplay run was not required by this scope.

Final verification: `build/batches/boss-step4-release/verify-7d717969d5cc4ceaa343887346f09758`.

**Eleven runtime classes staged** from nine source families, with verified runtime backups and SHA-256 manifests under `build/batches/boss-step4-release/before/runtime`, `verified.json` and `staged.json`. The packed definitions, nine XML profiles, NPC registry and spawn file are updated on disk. The running server was not restarted.

Original pre-edit source/data backups are under `build/batches/boss-step4/before/source`. The later release batch was prepared when adding missing Spinolyp profiles and correcting the compile-helper selection; its source snapshot includes work in progress, so it is **not** the original pre-step-4 source backup. New Spinolyp XML files were absent before this work. Keep both batch directories. The repeatable data construction script `tools/batches/boss-step4-data.ps1` uses the original packed backup; it is a historical release tool, not a safe general editor to rerun after later data changes.

No drops, saved accounts, cache/client binaries, entrance rules or launchers were changed. Original old boss action files remain historical/unused for these new handlers.

## Live acceptance still needed after restart

Use ordinary gear with godmode off, and two players for multi-target checks:

1. Corp: spear stab versus other styles; half-strength magic protection; visible/dodgeable ground warnings; core spawn, poison pin, hopping and actual healing; underfoot stomp; leave/re-enter reset and one respawn.
2. KQ: both visible forms and protection icons, attack animations/projectiles, no first-form loot, workers, exactly one final reward and reset to the first form. Check Slayer/drop presentation through the real final loot path; the regression observes reward ownership with a test loot callback.
3. Kings: approach each independently, lure/freeze Rex at real rock corners, compare Supreme front/rear positions and Prime nearby/far partners, kill one without resetting the others, then leave/re-enter. Check Spinolyp emergence/dive and moat projectile paths.

Native asset existence and headless damage/landscape tests do not prove live animation identity, packet presentation or complete real-network encounters. Those are the remaining acceptance boundaries for this step; broader bosses/Nex calibration and familiar work remain their separate backlog.
