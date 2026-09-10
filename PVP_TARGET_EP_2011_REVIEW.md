# PvP, targets and EP review

Reviewed 2026-09-08 against the current active `src` working tree. Review only: no gameplay code, runtime classes, configuration or player saves changed. This is a historical snapshot for later implementation, not a list of completed fixes.

## Overall assessment and historical baseline

The server is a custom hybrid: ordinary Wilderness item drops and skull protection, an old Bounty-style target overlay/EP counter, and private-server PK points/random bonus loot. It is not a faithful implementation of either January 2011 Bounty Worlds or the restored Wilderness from February 2011 onward. Basic Wilderness combat is substantially closer than the target/EP/reward layer. A numerical fidelity percentage would imply coverage and historical certainty this review does not have.

The date matters. Jagex announced the Wilderness/free-trade restoration for February 1, 2011 ([preserved January announcement](https://runescape.wiki/w/Update%3AThe_Wilderness_and_Free_Trade_Will_Return%21)). The old PvP worlds were removed that day ([historical PvP-world record](https://wiki.darkan.org/PvP_World)). A contemporary interview with Jagex's Mark Ogilvie describes moving revenants into a multi-combat cave and giving them former Bounty rewards ([February 14 interview](https://www.mmorpg.com/general-articles/the-wilderness-and-free-trade-return-2000082885%26page%3D2)). Thus EP/assigned targets are appropriate for the earlier system, or as an explicitly custom addition to later Wilderness rules.

The [preserved in-game Bounty worlds manual](https://wiki.darkan.org/Bounty_worlds_manual) is particularly useful primary text: searching begins after 30 dangerous-area minutes, improves after 60, waiting minutes survive leaving, targets hunt each other, target kills receive improved drops with an extra hour's EP toward that drop, and accumulated safe/logout allowances are ten minutes. These are public rules, not the complete hidden matching/reward algorithm. Mirrors may contain transcription/version limitations. Exact EP risk thresholds, hot-zone rates, death potential, reward weights and anti-farming rules need a dated specification before implementation; community recollections are not sufficient to certify them.

## Confirmed current findings

Line references below refer to the reviewed source snapshot.

### 1. Critical: EP payout executes on the wrong player and can interrupt death completion

`Player.java:1932` calls `lastHitter.EpDrop()`. That method (`669-685`) asks **that player's** DamageManager for their killer and uses that player's location. It should be resolving the death of `departed`, with the already-selected recipient, rather than discovering who damaged the surviving attacker.

Example: A kills B. The code calls A.EpDrop(). If B did the most damage to A, the generated item belongs to B at A's location. If A took no credited damage, getKiller() returns null and line 672 dereferences it. An NPC entry is another unsupported receiver case. These are direct control-flow deductions, not live reproductions.

`Skills.java:242-257` calls appendDeath before removing the skull, respawning, closing prayers and clearing damage credit. Its death tick has already stopped and restored health (`191-212`). An exception in the reward callback can therefore skip remaining death cleanup after items have already been processed. Fix the death context and make optional bonus settlement unable to prevent core death completion.

### 2. High: EP is not a meaningful reward-quality calculation

`Player.java:669-685` chooses one item from the same PVPItems array before checking EP. The branches under 20 and 20-39 create the identical kind of drop. The final branch tests the receiver object's EP against 76, but the selected killer's EP against 100; the usual 40-75 band has no dedicated branch. Increasing EP does not select a better table or scale its value.

`1930-1932` resets lastHitter's EP to zero before invoking this routine. In multi-player kills, the top-damage recipient's EP may remain unchanged while another player's is reset. There is no coherent earn/spend contract. Capture EP and eligibility before settlement, evaluate the selected reward mode once, then debit the correct account and refresh its display.

### 3. High: targets are not reciprocal or eligibility-filtered

`PlayerAreaTick.java:444-454` selects any other player whose cached Wilderness flag is true. It does not check combat level, attackability, existing assignment, waiting time, life state, or a reciprocal reservation. A low-level player can receive an opponent they cannot attack; several players can independently target the same person. Assignment sets only the selecting player's field (`248`).

Use an explicit pairing record with stable player identities and atomic two-sided reservation. Eligibility should use the same combat rules as attack validation, with a documented historical matching policy and a deliberate low-population policy.

### 4. High: killing a target has no target-specific outcome

A source-wide search for the player `.target` field finds its active reads/writes in PlayerAreaTick, not the reward/death code. `handlePkStatistics` never compares the dead player to the killer's assigned target. There is no target-kill bonus, successful pairing settlement or target-specific cooldown. The current assignment is effectively a name/arrow, not the Bounty gameplay loop described in the manual.

### 5. High historical mismatch: accelerated, unconditional EP/target timers

`PlayerAreaTick.java:61,230-246` runs every game tick. The counter starts at 400, subtracts 5 and triggers below zero: **81 eligible executions**, nominally **48.6 seconds** at the 600 ms cadence (`ServerThread.java:40`). Each trigger adds 2 EP and 5 likelihood. From zero, 100 EP takes **40 minutes 30 seconds** of eligible ticks without interruption/reset.

Likelihood starts at 5. At 30, a per-tick random threshold can begin matching: five increments, approximately **4 minutes 3 seconds**. It reaches 60 after eleven increments, approximately **8 minutes 54 seconds**. These are code-derived nominal times, not wall-clock measurements under load. They differ markedly from the public 30/60-minute Bounty waiting rules.

Accrual checks being in the Wilderness branch, not risked wealth, hot-zone status, qualifying loss, or activity. Naked waiting earns EP. Other custom PvP zones have a different branch and do not accrue EP here. No separate death-potential model was found in these paths. Treat accelerated rates as a possible customization, but make them explicit settings if retained.

### 6. Target departure and persistence do not follow Bounty rules

`PlayerAreaTick.java:67-75,266-270` clears assignments on leaving/disappearance and resets likelihood to 5 outside the cached Wilderness state. There is no ten-minute safe/logout allowance, stable pairing recovery or distinction between the deserter and the innocent partner. EP itself is saved/loaded (`Player.java:891,1126`); target, likelihood and the partial accrual counter are not persisted by those paths. One quick trip to safety can erase waiting progress while preserving EP.

### 7. Critical: death-container copying omits ordinary stacks and can overflow

`ItemsKeptOnDeath.java:175-178` builds the lost-item container with only **36 slots**, then adds the inventory and equipment before removing protected items. Inventory has 28 slots and equipment supports more than eight occupied slots. With distinct items, a full inventory plus worn equipment exceeds 36. `Container.addAll` (`380 onward`) does not report rejected additions to its caller. `GraveStoneManager.java:69 onward` subsequently clears the real inventory/equipment.

There is also a more direct copying defect: `Container.addAll` only calls add for a stackable item when `count + count_ < 0`; otherwise its only add branch is for non-stackable items (`380-397`). Normal positive coin/ammunition stacks are therefore omitted from the death containers. When the real containers are cleared, those stacks cannot be retained or dropped from the computed result. This affects the death preview and risk calculations too.

These are source-confirmed conservation defects with potential permanent item loss, independent of EP. Correct the bulk-copy behavior, size the intermediate container for all inputs, handle merge/overflow failures, and verify item conservation before committing a death. Reproduce ordinary coin/ammunition stacks and full equipment plus full inventory before release. Audit other addAll callers before changing this shared helper.

### 8. Displayed attack range differs from enforced attack range

`PlayerAreaTick.java:509-527` displays a range based on 10% of combat level + 5 + Wilderness depth. `Player.java:2030-2043` enforces plain Wilderness depth for both participants. With combat 100 and Wilderness level 1 (no Summoning complication), the display says **84-116**, while ordinary Wilderness validation only permits **99-101** when both stand at level 1.

Custom PvP/SafePk paths skip that combat-level restriction altogether. Use a shared mode-aware eligibility/display calculation; do not fix this by simply widening ordinary Wilderness attacks to match the old PvP-world UI. Summoning-specific correction remains deferred.

### 9. PK points are custom and include an arithmetic defect

`Player.java:1863-1933` awards points to the top-damage killer and sometimes separately to the last hitter, adds rewards for killing staff, scales with the recipient's own risk, and doubles the main donor reward. These are private-server rules, not historical Bounty rewards. No repeated-victim cooldown, qualifying victim-loss check or account-pair reward limit appears in this settlement path.

The last-hitter donor branch uses `Math.ceil(lastHitterReward *= 0.3)` (`1888`). The compound assignment first truncates to int; ceil's return value is discarded. A base reward of 2 becomes **0**, while the main donor path doubles points. Confirm intended custom donor benefit and implement it explicitly. Add farming resistance and reward audit logs if retaining minted rewards; do not use IP alone as proof of abuse.

### 10. Damage-credit history needs encounter lifetime rules

`DamageManager.java:321-349` accumulates damage by Mob and selects the highest total without per-entry expiry. Source call sites clear player enemy hits during death/recovery, not normal combat disengagement. Old damage can therefore influence a later death after healing/disengaging. Meanwhile `CombatExecutor.java:273-285` expires the last-attacker pointer using a separate 16-tick combat timer, and PK statistics require that pointer (`GraveStoneManager.java:160-161`). Delayed deaths can receive different statistics/bonus behavior despite a stored killer.

Specify credit expiry and tie handling for the chosen date, snapshot the death once, and use the same result for loot, statistics and target completion. Familiar aggregation has additional legacy concerns but remains outside the owner's deferred Summoning scope.

### 11. Teleblock remains an outstanding PvP mechanic

`Teleblock.java:30-32` applies during casting for 550 ticks (330 nominal seconds), with immunity at 450 ticks (270 seconds), and no Protect-from-Magic duration branch. That permits immunity to expire before the block. This remains unresolved from the prior combat review. `TeleportHandler` does contain Teleblock and level-20 checks, so the issue is not that blocking is wholly absent. Source all date-specific durations and exceptions, move application to successful impact, and test every escape route and delayed cast.

### 12. Custom zones, rewards and presentation require separate rules

`Skills.java:213-239` makes RandomPVPZone/SafePk/FFA death branches safe and leaves their old random rewards commented out. `PlayerAreaTick.java:333-340` fully replenishes special energy in its custom PvP branch every 101 executions (60.6 nominal seconds). `GraveStoneManager.java:186 onward` contains hard-coded millions-of-coins substitutions for degraded PvP equipment. Ordinary Wilderness transfers eligible lost items as well as calling the custom bonus routine.

These differences should be documented per mode rather than presented as authentic 2011 rules or silently removed. The EP UI also has incomplete colour bands (`483-488`), and Wilderness overlays 591 and 381 are both sent around entry (`244,294-297,457-459`); confirm client behavior after core logic is corrected. Duplicate reward arrays in Player, GraveStoneManager and Constants make maintenance misleading; call-site tracing is essential.

## What is already present

- Ordinary Wilderness checks on both participants, single/multi gating and activity-specific combat rules.
- Three protected items when unskulled, none when skulled, and an extra item with Protect Item; item ordering uses the server's exchange-price data. Counts exist, but the capacity defect above prevents certification of all inventories.
- Skull and retaliation bookkeeping, roughly twenty-minute offensive skull timing, saved remaining skull time, and a shorter Abyss path. Re-login/expiry retaliation correctness still needs dedicated tests.
- Victim item drops, several degradation conversions, a target name/arrow, an EP display and EP save persistence.
- Previously implemented combat foundation/formula/enhancement work. Historical reports of broken soaking, ordinary Smite/Soul Split and freeze application must not be treated as current unfixed findings. Specials, Teleblock, curse calibration and timing still have the explicitly recorded limits.

## Recommended implementation order

1. Fix wrong death/reward context, null handling, death completion and item-conservation/capacity defects first. These are correctness issues under any ruleset.
2. Select a dated mode: January Bounty Worlds, February-onward Wilderness, or a documented hybrid. My recommendation if keeping targets/EP is a clearly custom Bounty layer over ordinary pre-EoC Wilderness combat.
3. Share attack eligibility and UI logic; implement reciprocal assignments, completion and departure rules.
4. Implement a single reward settlement with explicit risk inputs, EP spending, tables, target bonus and abuse limits. Preserve authorized private-server features as deliberate configuration.
5. Calibrate timers and historical mechanics; fix Teleblock; validate skull, protection, PJ/credit expiry and escape boundaries.
6. Run headless regressions followed by two/three-client acceptance with godmode disabled. Do not alter Summoning or migrate Dungeoneering as part of this work.

## Required acceptance cases

| Area | Cases |
| --- | --- |
| Death safety | Non-retaliating victim; NPC previously hit killer; top damager differs from last hitter; disconnected top damager; poison/delayed death; bonus failure still completes death once |
| Item conservation | Full inventory/full equipment; 0/1/3/4 kept items; stacks; duplicate IDs; untradeables; degraded conversions; no loss/duplication outside documented death rules |
| Matching | Two reciprocal players; three-player contention; no legal opponent; extreme combat mismatch; already assigned/dead/offline candidate |
| Lifecycle | Safe excursion; logout/reconnect; partner departure; death; cooldown; restart and saved progress according to selected mode |
| EP/rewards | Every band boundary; zero/max EP; below/at risk threshold; target versus ordinary kill; correct owner/location; repeated victim; donor/staff rules; overflow |
| Combat/UI | Display matches attackability at depth boundaries; single/multi; retaliation and skull expiry/relog; protected item after Smite; Teleblock success/splash/prayer/escape |

Validation limits: source/call-site review and nominal arithmetic only. No new Java regression suite, running-server/client test, economy simulation or complete packed-item/zone audit was performed. Actual loaded runtime may differ from staged source until restarted. No claim of complete 2011 combat certification is made.
