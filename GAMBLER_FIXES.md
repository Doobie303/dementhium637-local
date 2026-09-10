# Gambler implementation and acceptance

Implemented 2026-09-09, following GAMBLER_REVIEW.md and the owner's request for a winnings screen. Active source is src. The historical review is retained unchanged. This is custom server content, not a historical Jagex rule claim.

## Player experience

Talk to Gambler NPC 2998 at the Duel Arena. The menu offers coins, runes, rules, and the last result/saved reward. Enter a quantity, review the stake and total return, then press Roll after the short review delay. Cancel changes nothing. Each new bet needs confirmation; there is no automatic replay.

The player throws first, then the clicked Gambler throws, using animation 11900 and dice graphic 2075 over six world ticks (normally 3.6 seconds). The reward window uses interface 634 with the reward item/quantity, both rolls, and different win/tie/loss titles. Win and loss headings show net gain/loss; chat also records the exact returned quantity and net amount, plus the round's short ID. A tie returns the original stake. The displayed item is a receipt, not an additional claim authority.

Rewards collect automatically if they fit. Otherwise the window offers Collect, and the saved entitlement remains available through ::gambleclaim or the NPC's last-result menu. Login also attempts collection. Reopening the result does not pay again. Leaving, teleporting, closing another interface, death, logout and shutdown release the activity/NPC reservation; an already committed outcome remains recoverable. Decorative dice graphics do not encode the numeric outcome.

## Rules and economy

Two independent SecureRandom bounded rolls from 1 through 100. Higher wins; ties refund. A win returns exactly twice the stake, including the original stake. No hidden house fee, donor bonus, admin override or dependency on Commands.diceChance. The handheld dice feature is unchanged.

Default policy in settings/gambler.properties (restart to change):

| Asset | Minimum stake | Maximum stake | Initial house stock |
| --- | ---: | ---: | ---: |
| GP | 10,000 | 10,000,000 | 1,000,000,000 |
| Death runes (560) | 1 | 1,000 | 100,000 |
| Blood runes (565) | 1 | 1,000 | 100,000 |
| Nature runes (561) | 1 | 1,000 | 100,000 |

Initial house stock is a one-time server-funded seed, created on the first valid betting transaction that reaches house storage. It is finite, persists independently of settings, and is never automatically topped up. A bet is refused if the house cannot cover the maximum possible profit liability. Editing the seed after initialization does not refill the house. No operator refill command is included. Do not delete the initialized house file to reset it; a marker makes missing house state fail closed. Restore a consistent backup instead. The enabled setting can close new play while saved rewards remain claimable.

Only the four explicit ordinary stackable assets are supported. Policy can disable rune choices by removing them from assets; the menu then explains that they are unavailable. Notes, charged items and all other item IDs are rejected. No market-value conversion, rares, equipment or arbitrary item whitelist was introduced.

## Ownership and durability

GamblerSession owns the menu/review/animation/result and validates the current player, exact clicked NPC, distance, lobby location, phase, expiry, exact inventory slot/hash and amount. There is one visible rolling round per NPC. Draft/review/result sessions time out after 100 world ticks; confirmation has a three-tick review gate. Other interactions close the gambling session through Player.closeAll; duel availability also rejects a player with this active owner. Amount packets are bound to the current session and cannot directly commit money.

At confirmation, PlayerLoader serializes a transaction under the existing World -> fileLock order. It checks inventory and house coverage before rolling. The account post-image debits the stake and records the fixed rolls and exact pending reward; the house post-image applies the matching win/loss/refund. A password-free round receipt is included. The durable redo journal is committed before animation or reward presentation. There is no unsettled house liability afterward: the outcome is already settled into a saved entitlement. A disconnect cannot reroll it.

Storage under the configured player-save directory:

- gamble-house.bin: finite stock by asset.
- gamble-initialized: guards against silently reseeding a lost house file.
- gamble-commit.bin: pending account + house + receipt post-images, removed only after successful installation.
- gamble-rounds/<UUID>.txt: idempotent receipt with account, stake, policy version, rolls, return, house stock and timestamp; no password.
- Optional GAM1 account trailer: most recent round, rolls, stake and uncollected entitlement. Accounts that never played have no GAM1 trailer. GAM1 follows the existing optional DUL2 trailer without changing it.

Normal account load/password load/save/current-load verification and duel commit all recover both journals before I/O. Installation failures after the journal is durable retain committed memory state and retry before subsequent I/O. Failed collection saves roll back inventory and pending entitlement together. Full-inventory payouts are saved, not dropped on the floor. Unknown/corrupt journal or house data fails closed. The initial account save structure and all existing duel recovery behavior are preserved.

The old Gambler payout/dialogue stages are explicitly retired. Replaying their stage IDs cannot award or remove items. Gambling interface buttons route independently through GamblerSession before the normal event dispatcher, avoiding DuelArenaListener's 634 collection path. Custom shared-interface labels/visibility are restored before reuse by ordinary duels.

## Verification

All 21 suites passed on the final production source, including DuelRegression's 2,114 checks and the prior combat/PvP/instance/Barrows suites. The initial Gambler run had 10,105 checks; additional recovery tests subsequently passed, bringing its final total to **10,113** without production changes. Build/logs: build/gambler/compile.ps1, verify.ps1, verify.log and individual suite logs.

Gambler coverage includes all 10,000 ordered dice outcomes (49.5% wins, 49.5% losses, 1% ties, exactly fair total returns), actual confirmation/result packet routing, exact displayed amounts, review delays, repeated confirmation/closing/claiming, every supported asset, one-NPC concurrency, full inventories, stale inputs/inventory, expiry/distance, closed-session claims, insufficient house funds, failed commits, failed collection rollback, interrupted journal installation before and after account installation, ordinary PlayerLoader replay, mixed DUL2/GAM1 saves and refusal to reseed missing initialized house state. Tests use isolated temporary account directories under build/gambler; real account saves were not changed.

Read-only client validation: GamblerInterfaceInspect uses the supplied DyNamic-local.jar's actual Class293 decoder to list all hooks on 626/634 (interface-hooks.log). The reused text fields have no competing hooks except the known 634:33 script 1640; varc-string 274 is populated before opening. Existing button hover, close and scrollbar hooks remain. This is script/packet validation, not a rendered screenshot or live gameplay acceptance.

## Runtime and remaining live checks

Production classes are compiled for Java 8 and staged by build/gambler/stage.ps1, with SHA-256 checks in staged-classes.csv. Original source/runtime backups are under build/gambler-before. No server restart, client/cache modification or real account edit was performed.

Restart with no active duels. Test two clients, fixed and resizable modes: all menu options, both throw effects on the Gambler model, reward layout/counts, win/tie/loss results, pending collection, reconnect during the throw, NPC contention and an ordinary duel immediately afterward. Confirm that dice graphics are placed attractively on the live tiles and text stays within the cached component widths. These visual/timing checks remain outstanding.

Preserve the house, initialized marker, any commit journal and account saves as one backup set. Do not downgrade to a runtime that ignores GAM1 while any entitlement remains unclaimed or a gamble-commit.bin exists. Complete recovery and claim first. The existing DUL2 downgrade restriction also still applies. Godmode, custom PvP policy, Summoning and Dungeoneering deferrals are unchanged.

## Repeat-play boundary correction - 2026-09-09

The owner reported successful first play followed by repeated "Finish your current activity" refusals. Reproduced a concrete cause: NPC 2998 spawns at y=3266, exactly ChallengeRoom's southern boundary, and is configured to wander +/-3 tiles. A normal adjacent interaction south of that line failed both open and ongoing presence checks; the combined guard mislabeled location failure as a busy activity.

GamblerSession now validates the registered live NPC's original stand anchor inside ChallengeRoom on plane 0, current NPC position within six tiles of that anchor (covering normal diagonal wandering), and player within three tiles of the NPC on the same plane. It no longer requires the player's tile to be inside the duel negotiation rectangle. Both opening and ongoing review/confirmation use the same location policy. Real busy-activity restrictions remain; location failures now have a distinct stand-near-the-Gambler message. Duel boundaries, spawn/wandering settings and all transaction/payout behavior are unchanged.

The new regression fails against the preceding runtime after a completed first round when reopening beside the wandered NPC. The patched suite passes **10,319 Gambler checks**, including all 49 normal wandering positions through DialogueManager.handle, continued session validity, no repeated settlement, a second committed wager south of the old boundary, and wrong-plane/remote/displaced-NPC rejection. This confirms the boundary defect; the screenshot alone did not expose player coordinates or rule out other live busy states.

Source/test/runtime backups: build/gambler-repeat-before. Tested and hash-verified runtime staging: build/gambler-repeat (two GamblerSession classes). Restart with no active duels, then repeat the live two-round test. No account, house, cache or client files were changed.

## Rules dialog close correction - 2026-09-09

The owner reproduced a stuck Please wait state after clicking the five-line rules display. DialogueManager.sendDisplayBox supports interfaces 210-214, but DialogueHandler routed only 210-213. Added interface 214 to the existing continuation handler. The Gambler rules now use close stage 19109, which ends the activity and closes/clears the conversation rather than returning to the menu. No betting, payout or save-format changes.

The new actual opcode-4 regression fails against the preceding handler at rules-close activity release. The patched suite passes **10,329 Gambler checks**, including five-line rules, closing/clearing state, reopening afterward and all one-through-five-line display box routes. Source/test/runtime backups: build/gambler-dialog-before. Updated classes staged and SHA-256 verified using build/gambler-dialog/stage.ps1 and its manifest. Restart with no active duels; live dialog close confirmation remains required.

## Wager/reward preview correction - 2026-09-09

The owner showed blank confirmation panels and requested item previews and gambling terminology. Actual cached script 206 rebuilds 626:46/47 from container 134's ordinary and alternate sides. The initial implementation sent only text; a client inventory refresh could overwrite it with empty container-derived text.

Gambler confirmation now populates both 134 sides with the exact wager and twice-wager total return. One-time script 153 grids on parent panels 21/22 display centred item icons using 134 and 136, with the item in the middle row to separate it from the cached name/quantity text. Script 153 preserves the existing parent refresh hooks; script 149 would install a new hook and was not used for these previews. Dynamic children are explicitly removed on cancel/end and before rolling so they cannot leak into ordinary duels. Client opcode 31 is an inclusive comparison: cleanup uses a negative column/row product to skip all cell creation after deletion, rather than zero dimensions that would enter the loop. Cached script dumps are under build/gambler-preview.

Visible Gambler review/rules/result/error wording now uses wager instead of stake. Original stake wording is restored only when returning the shared interface to duels. The potential reward includes the original wager; previews do not debit or pay anything.

The new packet regression fails against the prior runtime's missing wager container. Patched GamblerRegression passes **10,369 checks** including all four assets, exact quantities, both cache text sources, grid setup/cleanup, cancellation conservation and the rules-close fix. DuelRegression passes **2,126 checks**. Client script inspection verifies the container bindings and loop behavior; live icon layout, clipping and automatic text refresh still require owner acceptance.

Source/test/runtime backups: build/gambler-preview-before. Tested classes staged and SHA-256 verified using build/gambler-preview/stage.ps1. The preceding rules-dialog close fix remains staged as well. Restart with no active duels and verify both confirmation panels, rule-dialog close, and a normal duel afterward. No account/house/cache/client files were modified.

## Billion-GP wager and roll display - 2026-09-09

The GP cap is now 1,000,000,000 in settings/gambler.properties and the policy fallback. The 10,000 GP minimum, rune limits and once-only 1 billion GP house seed remain unchanged. Existing house balances are not refilled; the finite house must cover the wager before accepting it. A winning maximum wager returns 2,000,000,000 GP, within the signed integer item-quantity limit; existing overflow claim protection remains.

The full-width result title and chat now show both actual numbers in red: You rolled: N | Gambler rolled: N. The right-side status identifies win, refund or loss. Narrow legacy duel name/level fields are cleared, including varc-string 274 before opening the interface, so the cached name script cannot overwrite the display. Exact return/net chat and reward items remain available.

GamblerRegression passes 10,375 checks, including the exact cap, rejection above it, 2-billion preview quantities, a funded maximum-wager transaction with conservation, and actual roll-number/color result packets. The maximum-wager test runs last because a legitimate win can exhaust the isolated seeded house. Live layout acceptance remains required.

Three runtime classes from GamblerPolicy and GamblerSession are staged and SHA-256 verified under build/gambler-billion; source/test/config/runtime backups are under build/gambler-billion-before. Restart with no active duels to load the cap and display changes. No live account, house, client or cache files were changed.

## Outcome styling follow-up - 2026-09-09

The result status now uses the more prominent heading font (cached font 496 instead of 495), with concise You won!, You lost!, or Tie - refunded text to fit the existing panel. The full-width actual-roll heading and matching chat line are green for wins (00cc00), red for losses (ff0000), and gold for ties (ffcc00). Status text uses the same outcome color. Closing restores font 495 and ordinary duel labels.

Read-only cache inspection found script 1191 applies a supplied widget font in the English client, with arguments widget/font; ActionSender serializes the supplied array in reverse logical order. Font IDs were confirmed by decoding cached widgets 634:15 and 634:29 with the supplied binary client. Inspection source/output: build/gambler-colors/FontInspect.java and fonts.log. No client/cache files were edited. Live font appearance and clipping remain to be checked; the existing script's English-language guard is retained.

All 10,384 Gambler checks pass, including explicit win/loss/tie roll colors and actual numbers, heading-font packets and restoring the duel font on close. Two GamblerSession runtime classes are staged and SHA-256 verified under build/gambler-colors; backups are under build/gambler-colors-before. Restart with no active duels for live testing. Wager limits, odds, house funds and payouts are unchanged.

## Dedicated interface prototype - 2026-09-09

The owner requested starting the custom Gambler interface before the proposed solo combat minigame. Implemented a separate development client and administrator-only ::gamblerpreview visual proof; real wagering remains on the existing UI. Read client/gambler-interface/README.md for launch/build instructions and scope.

The prototype uses empty cached interface slot 891 and 35 purpose-built widgets: green/gold frame, wager and return item panels, separate roll cards, central outcome/net text, explicit sample footer and win/loss/tie/review/close controls. A selective Class85 patch supplies the layout; Class316 restricts cache access to the launcher's isolated dev-cache folder. The original client JAR and external caches are untouched. The build verifies the known original binary hash before packaging. This proves a narrow interface-injection route, not a full decompilation rebuild or cache-repacking pipeline.

Server preview is restricted to administrators, owns an expiring Activity, closes on walking/other interface use, and consumes stale/unsupported interface packets without starting any game. Sample item containers never enter inventories or recovery storage. No bet/claim transaction runs. The working NPC game is unchanged. Demo controls intentionally do not imply real Confirm/Collect functionality.

Validation: 81 checks against the packaged client (empty slot, loader linkage, widget bounds, available fonts, action masks, reload); 16 command/button/cleanup/non-mutation checks; all 10,384 Gambler and 2,126 duel checks pass. The SVG preview is approximate; live fixed/resizable rendering, item icons and mouse interaction remain acceptance work. Nineteen runtime classes from four server sources are staged and hash-verified under build/gambler-interface, with backups build/gambler-interface-before. Restart with no active duels and launch the separate development client, then ::gamblerpreview. No real saves or house funds were modified.

Connecting the layout to real wagers, production client compatibility/distribution and full animated roll presentation are subsequent work after live visual acceptance. Challenger's Vault remains an idea; no combat minigame changes were made.

Custom-client connection follow-up: owner reported intermittent lobby-to-game connection errors, then successful entry after retries. Binary inspection and a negative control confirm Class354's first route uses uninitialized port 0; the alternate uses 43594. The development client now initializes both ports to 43594. Six initial/retry route checks and all 81 packaged-interface checks pass; no live authentication was automated. The same launcher now selects Gambler-dev-v2.jar, leaving the running/previous JAR untouched. Source build/launcher backups are under build/gambler-connection-before. Client restart required; no server/source/runtime/account/cache changes in this follow-up. The NPC retains the existing real-money UI; ::gamblerpreview opens the new sample layout.

## Owner's live interface acceptance - 2026-09-09

The owner tested ::gamblerpreview and approved the layout, supplying screenshots of review, win, loss and tie states. Visible item icons and exact quantities render correctly: 1,000,000 GP wager, 2,000,000 win return, empty item panel/0 GP loss return and 1,000,000 tie refund. Both roll cards and green/red/gold outcome styling render clearly, with no apparent text clipping in the supplied views. This supersedes the prototype's pending live appearance acceptance for those shown states. The screenshots do not independently certify every display mode, cleanup path or real wagering integration. The displayed controls remain sample-only; connecting the accepted layout to the existing durable Gambler lifecycle and handling old-client compatibility are next implementation work.

## Real-wager custom interface integration - 2026-09-09

The owner authorized connecting the accepted layout to the actual game. GamblerSession now snapshots a login-scoped gamblerCustomUi preference on creation. ::gamblerui on enables the new screen for the updated client; ::gamblerui off restores the original UI. Changing this preference during an activity is rejected. It is not saved to the account, preventing a later login with an old client from inheriting an unsupported interface. This is explicit user opt-in, not automatic client capability detection. Existing clients default to the working legacy screen. The NPC menu and amount input remain the existing dialogues.

Custom review shows actual wager/asset and twice-wager return with Confirm roll, Change wager and Close. Confirmation retains the three-tick review gate, inventory identity checks, house coverage and durable transaction. The custom window remains visible while the normal player/NPC throw animations run; the player's true roll is revealed at tick 3, the house result at tick 6. Betting controls are hidden/ignored during rolling. Closing never reverses a committed transaction. Results show the true item, return, signed net result and green/red/gold rolls. Existing auto-collection remains; Collect appears when pending rewards do not fit. Repeated collection cannot pay again. New wager/Change wager return to selection and require fresh confirmation. Saved last results use their recorded asset/quantity, independent of any current draft.

Interface 891 packets route through the owned real session after the admin preview declines them. Legacy confirm packets cannot submit custom reviews, and custom packets cannot submit legacy reviews. The admin preview remains sample-only and explicitly restores its sample wording after real UI use. New client packaging Gambler-live-v3.jar replaces static demo menu actions with neutral functional actions; the same launcher selects it. Original client/cache files and real account/house state were not edited.

Validation: all 10,384 existing Gambler checks plus 35 new integration checks pass, including a real isolated transaction, conservation, queued/repeated controls, staged roll reveal, rune last-results, full-inventory collection and client-mode fallback. The 16 preview checks and 81 packaged-client checks also pass. Runtime staging and hash manifest: build/gambler-live; source/runtime backups: build/gambler-live-before. Client sources/build script retain the selective patch and corrected connection port. Live real-wager acceptance remains required; previous screenshots certify the sample layout only.

To use: restart the server with no active duels, close/reopen build/gambler-interface/dev-client/Run-Gambler-Preview.bat, enter ::gamblerui on after logging in, then talk to the Gambler normally. The command must be repeated after each login. ::gamblerpreview is still a separate admin demonstration. No Challenger's Vault changes were made.

## Automatic client capability selection - 2026-09-09

The owner rejected the repeated enable-command step. The v4 client launcher now appends |gambler-ui=1 to the existing settings string. Actual client bytecode confirms that field is written in both lobby and game login payloads. RS2LoginDecoder now reads the previously discarded settings into a connection-owned cosmetic capability before scheduling player loading. GamblerSession defaults to that capability unless an optional manual override exists. No account-save change or permission/odds/payout authority is attached to the marker. Fresh/old connections default to the original UI. New client detection no longer requires a command.

The selective client build now includes RunClient and its anonymous helper, in addition to the interface/cache/port patches. The same launcher selects Gambler-live-v4.jar; previous JARs remain untouched. A headless test reads parameters from the actual packaged launcher and encodes them through the actual binary login-string writer; server tests consume those bytes. All 17 automatic-selection checks pass (including old/unknown marker fallback, fresh connection state and optional manual override), alongside all 10,384 Gambler and 35 integration checks. Live v4 authentication and automatic NPC UI acceptance remain pending; tests do not claim a real network login.

Five runtime classes from GameSession, RS2LoginDecoder and GamblerSession are staged and hash-verified under build/gambler-auto. Backups: build/gambler-auto-before. Restart the server with no active duels and reopen the same client launcher. Then log in and talk to the Gambler normally. The v3 documentation's required ::gamblerui on step is superseded; manual on/off remains available only as an override. Authentication, account saves, real house funds and the original client/cache were unchanged.


## Missing real-game buttons correction - 2026-09-09

Owner screenshot showed Collect during REVIEW and missing Confirm/Change. Confirmed inverted visibility at the new UI call sites: ActionSender.sendInterfaceConfig has a misleading hidden parameter but encodes true as hidden=0. Actual client packet 3 reads that value into the widget hidden state. Corrected only Gambler custom-screen calls: review/results show action labels and backgrounds, rolling hides actions, Collect appears only for a pending reward, and Close remains available. Also corrected admin preview reset to show its demo buttons after real UI use. Shared helper semantics and unrelated interfaces were preserved.

The new wire-level assertions decode actual opcode-3 widget IDs and hidden flags, covering review, rolling, settled result, full-inventory pending collection, failed collection and successful collection. They fail against the preceding runtime on action visibility. Patched runs pass all 10,384 Gambler, 77 custom integration, 17 auto-selection and 16 preview checks. Previous tests exercised button packets without verifying whether a live player could see those buttons; this coverage closes that gap.

Three runtime classes from GamblerSession and GamblerInterfacePreview staged/hash-verified under build/gambler-buttons; backups build/gambler-buttons-before. Restart server with no active duels. No client rebuild/replacement is needed; use existing v4 client and automatic selection. Live confirmation of corrected visibility remains pending. No account/house/odds/payout changes.
