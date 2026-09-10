# Gambler NPC review and proposed rebuild

Review date: 2026-09-09. Review only; no gameplay source, runtime, cache, or account saves changed. This is a custom server game proposal, not a claim of historical Jagex gambling rules. The existing Duel Arena changes and their recovery format must remain intact.

## Confirmed current behavior

- NPC 2998 is spawned at 3367,3266,0 in data/npcs/npcspawns.txt:4053. NpcOption.java:538-545 routes unmatched NPC dialogue to the legacy DialogueManager; its active case 2998 starts stage 9050 (DialogueManager.java:3110).
- Both menu choices are broken. Stage 9051 (random amount) gives its response no next stage and returns false, so processNextDialogue resets it. The actual random bet code at 9052 is not reached from this choice. Stage 6669 (set amount) sets next stage 6670 but also returns false, so processNextDialogue immediately closes and clears it. See DialogueManager.java:81-87,126-142,3547-3561.
- The older lottery/dice branch near line 2985 is commented out. Its 630-series stage IDs now belong to other active content. Uncommenting that block is not a viable repair.
- Stakes are checked when selected, but not escrowed. Stage 9056 adds a bet on a win or attempts to delete it on a loss, using mutable Player.bet. No round identity, durable reserved stake, one-time settlement, or transaction-specific recovery protects these operations. Closing/changing interactions after selecting a stake is not equivalent to a secured wager. These are source-confirmed design gaps, not a live exploit reproduction.
- Results are driven by dialogue clicks, not an owned roll timer. The tie path sets cantMove=true with no dedicated cleanup; resetDialouge does not clear that flag. Other payout messages check whether the player still has the bet even after a loss and incorrectly label small amounts as K.
- Both actors' roll ranges depend on Commands.diceChance (Commands.java:85,2091). Normally each rolls 1-100; with the switch on the player rolls 60-100 and the NPC 50-100. The game must not inherit this adjustable global bias. The separate handheld DiceGame also uses the flag; changing that unrelated feature needs its own scope.
- The animation code locates the first NPC with ID 2998 globally instead of retaining the NPC actually clicked. That is unsuitable for multiple spawns/concurrent rounds and can fail if no matching NPC exists.

## Recommended game

Build one polished game first: Dice Duel with the Gambler. Player and NPC each roll independently from 1 through 100; higher wins; a tie returns the stake. Show the exact stake and total return before confirmation. For a fair version, a 1M GP stake returns 2M total on a win (1M profit), 1M on a tie, and zero on a loss. Mathematical probabilities are 49.5% win, 49.5% loss, 1% tie. These figures follow directly from the 10,000 ordered roll pairs.

Use unbiased bounded server RNG, independent of admin/donor status and the handheld dice switch. If the owner wants an economy sink, choose and visibly disclose a smaller payout before implementation; do not silently rig rolls. For example, 1.9x total return with refunded ties has a 4.95% expected stake loss, before integer rounding. Recommended starting policy is the simple fair game with configurable limits and a persisted house reserve. Reserve the maximum potential liability before accepting each round; a finite reserve must not promise unfunded wins. A server-funded unlimited house is a separate explicit economy decision.

Flow: Talk -> Rules / Play GP / Eligible items -> choose stake -> review stake, odds, total payout -> Confirm roll -> short animation -> both results and net gain/loss -> Close or explicitly confirm another round. Never automatically repeat a bet. Cancel before confirmation changes no inventory. Once confirmed, closing the window or disconnecting does not reroll/cancel a committed result.

For items, prefer a small configuration whitelist and same-item rewards: stake 100 eligible units, win returns 200 total. This avoids unreliable market-price conversions. Start with GP; then select ordinary stackable commodities or specific ordinary equipment with explicit quantity caps and funded house inventory. Exclude untradeables, quest/admin/donor items, rares and charged/degraded variants initially. Notes must have one explicit policy, not create a second valuation route. Full inventories retain saved claimable rewards; never spill high-value payout onto the floor.

## Presentation and reusable assets

The existing DiceGame.java:81 onward already uses throw animation 11900 and positioned graphic 2075; its alternate dice uses 2074. The legacy Gambler uses 11900/2075 on both player and NPC. These are concrete code references, not certified live visual results. Preview them on NPC 2998 before selecting the final timing; verify model compatibility, graphic positioning and appearance. Do not imply the decorative dice faces encode the numeric result unless the asset supports that.

Use the clicked Gambler facing the player, a brief throw animation, dice graphic on a nearby unobstructed tile, and readable result text. Allow only one visible rolling round per NPC with a short busy response; other players can review rules without sharing round state. Avoid global chat spam.

Existing NPC-head dialogues, option menus, amount inputs and display boxes (210-214) can provide the initial complete flow without new client assets. A larger confirmation/result window could adapt existing item-container interfaces such as 626/634, with appropriate gambling labels, item icons, total return and rules. These are candidates, not already verified gambling layouts: inspect all on-load/transmit scripts, varcs, buttons and fixed/resizable modes before reuse. The duel name failure demonstrates why sending text alone is insufficient. Do not route gambling buttons through the duel listener or share a live DuelActivity. A bespoke graphical dice table is a later client/cache project, not necessary for the first release.

## Required implementation and acceptance

Use a dedicated owned round with explicit draft/committed/settled states, immutable bet and rules, unique ID, and one round per account. Validate interaction distance, clicked NPC identity, current session and activity availability, positive/capped amounts, exact inventory slot/metadata and overflow-safe arithmetic at confirmation. Block overlaps with trading, dueling, banking or other item-transfer activities.

Persist stake debit, house liability, chosen rolls and recoverable entitlement atomically before showing the result. Persist settlement once; recovery must replay the same outcome after a restart. Durable house/account updates require a coordinated journal, not two unrelated saves. Reuse the proven item-copy/transaction patterns from duel work, but do not repurpose its two-player protocol or DUL2 fields without a reviewed format extension. Old dialogue payout stages must be retired so no alternate path can bypass the new owner. Log round ID, stake, rule version, rolls and settlement without credentials.

Tests should cover repeated/stale clicks, two users at one NPC, concurrent liability limits, inventory changes after draft, note/charge rejection, maximum stacks, full inventory, disconnect during animation, logout/relogin, interrupted save/settlement and recovery retry. Enumerate all 10,000 result pairs to verify payout math, then test actual packet routes and live fixed/resizable rendering. No live Gambler round or animation was run during this review.
