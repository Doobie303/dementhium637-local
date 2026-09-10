# Boss step 2: shared NPC combat foundation

Implemented 2026-09-09 after the owner authorized step 2. Read BOSS_BATCH_ONE_FIXES.md for the first batch. This report records implemented behavior separately from the historical review.

## Changes

**Per-NPC current levels.** NPCCombatStats owns Attack, Strength, Defence, Ranged and Magic independently of packed definitions and other copies of the NPC. The three shared formula paths now use those current levels. Drains have bounded carry-over and a floor of one (a base-zero stat stays zero). Recovery restores one point per 100 NPC ticks without exceeding the base. Life changes and explicit combat-state resets clear ordinary drains. Definition replacement resets the snapshot. Percentage curse modifiers remain separate.

**Bandos godsword.** The existing actual-damage callback now drains ordinary NPCs as well as players: actual LP removed / 10, carrying over through Defence, Strength, Attack, Magic and Ranged. NPCs have no Prayer level. Misses, immunity and cancelled hits do not apply the callback; replay cannot drain again. Player behavior and existing special multipliers/energy remain unchanged. `statDrainImmune` is an explicit NPC override; Nex defaults to immune to preserve its previous no-drain behavior pending its encounter specification. Familiars retain definition-based formula levels and cannot receive these new ordinary drains.

**Pending attacks.** CombatAction.newSession creates independent action instances for Corp, KBD, Zilyana, K'ril, Chaos Elemental, chromatic/metal/frost dragons and tormented demons. The executor creates one session per queued attack, so mutable style/attack state cannot be overwritten by another pending attack or another boss. TD's chosen style is copied into that session. Chromatic dragon damage type was moved off the source NPC's shared attribute into its session. Stateless actions retain their existing Interaction-owned behavior; activity-specific action counters were not indiscriminately reset.

**Life/reset ownership.** NPCCombatContext captures both ordinary NPC participants' generation and existing instance revisions. CombatAction execution, queued executor work and typed Damage reject obsolete NPC lives, observed death/hiding/offline participants, invalid plane/distance and managed-instance mismatches. PvP-only interactions retain their existing validation. Death, revival, destroy and explicit NPC combat-state reset advance the generation and cancel the NPC's pending executor work. Incoming typed hits from an earlier generation cannot land on a revived/reset NPC.

**Timers.** Shared NPC lifecycle callbacks retain their original delays but now belong to the NPC generation that scheduled them, including normal corpse/respawn callbacks. Corp/Zilyana/K'ril's legacy delayed specials capture a combat context and stop when it is no longer valid. Their alternate-target searches now reject invalid, invisible or unattackable candidates. These are lifetime repairs, not reconstruction of those specials' historical mechanics.

**Targets and returning home.** KBD, Graardor, Zilyana and K'ril retain an existing target rather than replacing it with a random nearby player 40% of every tick. Acquisition filters invalid targets. Chaos Elemental retains its intentional random switching with the same validation filter. The existing ordinary 12-tile leash now enters a returning-home state, rejects reacquisition while returning, and restores HP/current levels once on arrival. Generic aggression skips hidden/returning NPCs and invalid participants. Fight Caves retains its explicit extended pursuit exception and instance-owned target selection.

No packed data, spawns, access requirements, rewards, client files or save format changed. Preserve private-server access conveniences, godmode/XP, custom PvP and the Summoning/Dungeoneering deferrals. Shared NPC lifecycle/target invalidation necessarily also protects ordinary NPC targets of familiar attacks; no familiar-specific action or stat behavior was changed.

## Historical and implementation limits

- Early-2011 remains the provisional target. The [pre-EoC BGS description](https://wiki.darkan.org/Bandos_godsword) supports the 10%-of-LP stat drain and ordered carry-over; the [period godsword description](https://wiki.darkan.org/Godsword) describes the one-level floor. These are preserved later mirrors, not frozen early-2011 specifications. The 100-tick recovery interval is a provisional conventional reconstruction, with no boss-specific accelerated restoration table added.
- Exact immunity tables, percentage-curse cleanup/recovery and hidden boss stat calibration remain open. This implementation resets ordinary mutable levels; it does not reset percentage curse ledgers or certify their behavior across every reset. Nex's ordinary-drain immunity is deliberately retained pending specification.
- BGS is the integrated ordinary-drain consumer. Ahrim and other player-only drains still need their own NPC applicability rules. Boss attacks with explicit fixed maximum caps retain those caps; reduced current levels affect their applicable accuracy/defence rolls, not an invented scaling rule for their fixed maximum hit.
- Observed logout/death/departure and existing instance revision changes invalidate contexts. Same-plane ordinary-world teleport out-and-back or player death/revival between observations still needs a universal player transition token. Direct numeric/anonymous damage and every bespoke content timer have not all been migrated to typed generation-owned hits.
- The three legacy specials retain their old damage/protection/stat-effect mechanics, now guarded against stale execution. TD ground attacks, Nex's bespoke callbacks and KBD launch-time statuses remain the later encounter batches. No claim that all old boss timers are now owned.
- Room-specific boundaries, minion groups/respawns, boss phase/add cleanup, encounter-specific target exceptions and full attack-reach accuracy remain encounter work. The ordinary return path keeps existing clipping; it does not teleport an NPC through an unreachable route. Aggression tolerance and god-faction fighting remain backlog, not silently enabled here.
- Existing managed-instance cleanup is retained. Global God Wars group lookup/respawn is still an explicit batch-3 defect; generation guarding the scheduling NPC is not a substitute for a room-owned group.

## Scoped validation

| Suite | Result | Why it ran |
| --- | --- | --- |
| NPCFoundationRegression | 425 checks | Current levels/floors/recovery/isolation; formula use; generation cancellation; target retention; action independence; actual BGS callback; shared and legacy timers |
| BossBatchOneRegression | 20,207 checks | Existing DK/Graardor attacks call the changed formulas, Damage and CombatAction paths |
| CombatFormulaRegression | 90 checks plus 180,000 generated attacks | Changed NPC formula inputs and unchanged player branches |
| SpecialShieldRegression | 26,077 checks including 24,000 PvP hits | Warstrike callback and shared typed-Damage validation |
| FightCavesInstanceRegression | 414 checks | NPC life/death/timers and executor validation affect migrated cave NPCs |
| InstanceLifecycleRegression | 73 checks | NPC.destroy and owned lifecycle-task cleanup |

All six selected suites pass. CombatEnhancementsRegression and CombatBalanceRegression were compiled only as fixture dependencies of selected tests; their suites were not run. Other gameplay suites were not run. Compilation covered 27 changed/new source families plus connected test fixtures against current runtime dependencies, targeting Java 8; no full-source build was required.

The Fight Caves regression initially detected rejection of its long-distance member target. NPCCombatContext now preserves the existing cave-opponent exception, and the suite passes. The new standalone fixture also needed the normal AreaManager initialization before constructing Chaos Elemental's action.

Negative controls compiled old Damage and old Warstrike separately into isolated class overlays. The focused suite rejected them at `Old damage rejected after revival` and `BGS uses actual LP / 10`. No old classes were copied into runtime.

Reproduction runner: tests/boss-foundation/verify.ps1. It runs only the six justified suites and does not stage classes. Logs and source/runtime/backup SHA-256 manifests are in build/boss-batch2. The runner requires the preserved batch-1 baseline for the byte-preservation regression.

## Runtime and live acceptance

65 runtime classes from 27 source families are staged in bin, including all affected nested classes. Every staged SHA-256 matches its build artifact. Backups are under build/boss-batch2-before; build output is build/boss-batch2. No server restart was performed.

**Restart required.** Then test godmode off: BGS lowers boss Defence and subsequent attacks respond; two copies do not share drains; death/respawn clears ordinary drains; leave/re-enter while a projectile or legacy special is pending; bosses retain a valid tank; a leashed NPC returns and resets once; Fight Caves continues its normal pursuit. Check two-client targeting, large-NPC corners and live spell/animation timing. These live checks remain pending.
