# Duel Arena review against a provisional 2011 baseline

Review date: 2026-09-08. Historical snapshot of the current active `src` tree, including existing local changes. Review only: no gameplay code, runtime classes, client/cache files or account saves changed. No live duel or exploit reproduction was performed. Findings below distinguish source-confirmed defects from lifecycle risks and client validation work.

## Scope and historical limits

Reviewed challenge admission, both confirmation stages, stake addition/removal, rule dispatch, equipment removal, forfeit, result delivery, interface claiming, logout, shutdown, item/container semantics and relevant combat gates. Existing combat fixes were not presumed absent. Preserve godmode, personal XP and custom PvP policy; Summoning and Dungeoneering implementation remain deferred. Test ordinary duels with godmode disabled.

Provisional target: the 2011 pre-EoC arena after the February return of unrestricted members' staking. Exact 2011 date is still unselected. Do not silently add later OSRS taxes, caps, presets or interface behavior.

Historical references:

- [Jagex February 2011 Behind the Scenes, archived transcription](https://runescape.fandom.com/wiki/Update:Behind_the_Scenes_-_February_%282011%29) describes limitless members' staking.
- [Global RuneScape's arena guide, last updated March 2008](https://www.global-rs.com/guides/activities/duelarena/) documents the older foundation: safe death except stakes, selectable restrictions, a second review of rules and both stakes, restoration of boosted stats, countdown, and winner's opponent name/level and claim screen. This supports the inherited structure, not exact 2011 interface component numbers.
- [January 2011 Jagex FAQ reproduced on Tip.it](https://forum.tip.it/topic/285394-21-jan-2011-free-trade-wilderness-faq-answers/) has a search-indexed reference to red exclamation marks when a trade changes. That is trade evidence; it does not establish the exact 2011 duel-rule warning graphic.
- [OSRS arena history](https://oldschool.runescape.wiki/w/Duel_Arena) describes later confirmation and acceptance-delay changes. Later behavior is not proof of 2011 behavior.

The exact 2011 warning graphic/color/timing remains unverified. Nevertheless, safe consent requires both players to accept the same rules and stakes. Checking AND unchecking a rule should invalidate acceptance and visibly identify what changed, including automatically changed dependent rules. A short server-enforced acceptance delay would be a proposed safety enhancement pending historical confirmation.

## Findings

### 1. P1 — Settlement is delayed across logout and is not a durable transfer

`DuelActivity.endSession` (185 onward) marks the duel finished, then schedules two callbacks before `addSpoils` (458 onward) delivers the result. `World.unregister` (327 onward) invokes this method and continues to save and remove the player immediately. The callbacks retain Player objects, clear activities, teleport, and save both accounts later without checking that these are still the current sessions.

An opponent can also leave during the settlement window. The winner may be offline when `interfaceItems` is populated, after ordinary logout already collected that field. `Player.save` (956 onward) does not serialize duel escrow or pending interface rewards. An old callback can save a disconnected Player after a replacement login. These are concrete ordering/ownership defects; item loss, stale-save rollback and duplication outcomes require controlled disconnect/relogin fault tests. No live dupe is certified by this review.

Fix direction: resolve the winner once, detach and consume escrow once, create a recoverable payout before session removal, and bind callbacks to a duel/session identity. Persist transfer ownership so crash/restart recovery cannot mint or lose stakes. Sequential saves of two accounts alone are not atomic.

### 2. P1 — Stake operations discard item metadata and ignore transfer failures

`Stakes.stake` (44 onward) replaces inventory items with `new Item(itemId, amount)` or quantity-one items. `Stakes.remove` (116 onward) also reconstructs by ID/quantity. Packed item health/charge metadata supported by `Item(Item)` is discarded. This is a definite metadata bug whenever an eligible tradeable item carries that metadata; the complete eligible item set was not audited.

Inventory removal is followed by unchecked `stake.add`. Stake removal happens before unchecked inventory addition, with no rollback or capacity reservation. A full destination can therefore lose items. `Inventory.removeItems` returns true even when it did not remove a requested absent quantity, making it unsuitable as proof of debit. Ordinary stake requests have amount/availability checks, so that helper alone is not proof of an ordinary click dupe.

Use slot-specific metadata-preserving transfers, validate all quantities/capacity before mutation, and check the actual debit and credit. Include notes, maximum stacks, differing charges and filled inventories.

### 3. P1 — Multiple retained containers obscure who owns the stake

`transferStake` (DuelActivity:489) adds original stake contents into an aggregate without clearing the originals. Settlement actually pays from the original containers, not that aggregate. `reset` (391) clears `duelStake` singular, whereas the stored attribute is `duelStakes` plural. Winner return and opponent reward retain their source containers; `Inventory.addAllDropable` does not consume a container.

There is a `finished` guard, normal decline clears both containers, normal interface claim nulls `interfaceItems`, and first/second-screen packet guards exist. Consequently retained references are not, by themselves, a proven repeat-click dupe. They do leave fragile ownership and recovery semantics. Establish one escrow owner, consume the source on settlement/refund, and make every terminal path idempotent.

### 4. P2 — Acceptance resets exist, but change feedback is wrong/incomplete

`DuelArenaListener` (232 onward) resets both acceptance flags before ordinary rule changes. `Stakes` also resets both flags on changes. First/second-screen state gates reject normal cross-stage packets. These protections must be retained.

However, rule changes clear component **631:28**, while `DuelActivity.accept` writes acceptance messages to **631:26**. Stake changes do not clear the acceptance messages either. The screen can retain stale acceptance text. Rule refresh sends config 286 but no explicit description of the change; no server acceptance cooldown was found. `swapRule` itself does not invalidate acceptance, so the invariant is also spread across callers.

Recommended behavior: clear both actual acceptance labels, show e.g. “Alex changed No special attacks: ON → OFF; special attacks are now allowed,” highlight the affected rule, and require fresh acceptance. Apply this to additions/removals and automatic dependent changes. Store a revision of the complete offer; both final accepts must refer to the same immutable revision.

### 5. P2 — Opponent-name issue has a plausible packet-order cause, not a verified fix

`DuelActivity.initializeActivity` (109–119) sends opponent level/name to 631:25/23 before opening interface 631. A rule click sends the name again after the interface is open. `MunityBugs.txt` independently records names failing on dueling/trading interfaces and appearing on refresh. This is consistent with an initialization/order problem, but neither client handling nor cache layout was validated here.

`sendSecondInterface` contains no explicit opponent-name update. The result interface sends the name both before and after opening. Verify the actual 637 cache components and load scripts; open the interface before setting its dynamic fields where required. Test fresh login, first open, refresh/reopen, renamed accounts and fixed/resizable display. Do not assume an appearance update fix or import another revision's component IDs.

### 6. P1 — No-forfeit rule is not enforced

`ObjectPacketHandler` (335) opens the forfeit dialogue for object 3203. `DialogueManager` (2472) confirms forfeiting based only on having a DuelActivity; neither path checks `Rules.FORFEIT`. The rule is offered and described on confirmation but does not prevent voluntary forfeiting. Enforce it at the authoritative confirmation, with an active-fight and participant check. Disconnect handling requires a separate policy and must still settle safely.

### 7. P2 — Fun-weapons option has no implementation

`DuelConfigurations.FUN_WEAPONS` is empty. Active-source searches find rule selection/text and mutual exclusion with No weapons, but no fun-weapon combat/equipment whitelist enforcement. Players can agree to a restriction the server does not implement. Old notes propose custom “Fun Bridding”; that note is not authorization to replace the historical option. Implement a dated whitelist or make the unavailable option explicit.

### 8. P1 — No-shield can leave an existing two-handed weapon equipped

`canAccept` (DuelConfigurations:397 onward) reserves inventory space for a two-handed weapon when shields are disabled. `removeEquipment` (455 onward) only removes the explicitly disabled equipment slots and never removes that weapon for the shield rule. `Equipment.allowed` (519 onward) DOES correctly reject newly equipping a two-handed weapon under No shield; it also converts sparse equipment slots correctly. The defect is the initial equipment transition, not that conversion.

Apply the same derived two-handed restriction at start and equip time, and validate both players' inventory capacity immediately before the transition.

### 9. P2 — Friendly request choice is not carried into the activity

`DuelArenaListener.handleChallengeOption` stores `isStaking` and labels the request friendly/stake. `PlayerOption` creates the same two-argument DuelActivity either way. That activity always opens staking interface 631 and creates stake containers. Friendly mode therefore is not a separate enforced no-stake agreement. Bind mode to the challenge and require matching consent.

### 10. P1 — Challenge admission lacks robust busy/session identity checks

`PlayerOption` (95 onward) compares `duelingWith` with Boolean.TRUE, but a fighting duel stores the opponent Player in that attribute. Pending challenge acceptance is tied to a Short player index and booleans, without a challenge identity/expiry. The delayed coordinate event does not revalidate both players' ordinary activity ownership or arena presence. DuelActivity's constructor unconditionally resets/replaces both activity references.

The precise multi-player overlap must be reproduced; the type mismatch and missing revalidation are definite. Reject busy players and stale/reused indices, expire requests, recheck at event execution, and acquire both participants before publishing a new duel.

### 11. P2 — Confirmation content and inventory checks need reconstruction/verification

`sendSecondInterface` (245 onward) retains the staking inventory (explicit TODO). It clears text 626:25/26 whenever stake.freeSlots() < 28, which is always true for the nine-slot stake container, even when empty. It does not construct an explicit final stake text summary. Client script bindings might supply some display content, so visual emptiness is not certified here.

Rule text is written sequentially beginning at 33 with no explicit region bound; enough selected rules plus the two-handed warning can overlap fields intended for other text. `canAccept` checks an aggregate that is still empty until commencement. `sendSecondInterface` checks only the opponent's stake against current inventory, and `acceptSecond` does not repeat capacity/equipment validation. A failed first player's second-screen transition can decline both while the caller still attempts to show the other screen.

Build a bounded final summary of exact names, items/amounts, equipment restrictions and rules from the accepted snapshot; stop transition on either validation failure. Verify it using the actual client.

### 12. P2 — Shutdown and unusual terminal states have uncovered gaps

`forceEnd` (252) immediately returns unless activity state is UPDATE_STATE. Normal negotiating shutdown is nevertheless refunded by subsequent `Player.closeAll`, which calls decline for FIRST_SCREEN/SECOND_SCREEN. Do not report ordinary negotiation shutdown loss as confirmed.

The transition after both final accepts is FIGHTING plus COMMENCE_STATE: forceEnd skips it and closeAll does not decline it. That leaves stakes outside saved inventory at this shutdown boundary. Settlement PAUSE_STATE is another unhandled boundary. Fighting forceEnd returns the participant's original stake without consuming it and teleports getPlayer() instead of its player argument, so the second participant is not correctly evacuated.

Countdown and settlement tasks are not owned/cancelled as a group. `addSpoils` dereferences its player even when endSession found no winner. Define interruption/draw/refund policy, cancel callbacks and settle before saves. Delayed deaths after a result also need testing because onDeath writes winner flags before the finished guard in endSession.

## Suggested implementation order and acceptance matrix

1. One duel identity/state machine, validated participant ownership, exact item escrow, one-time settlement and crash recovery.
2. Versioned consent, correct change warnings, opponent names and final stake/rule display.
3. Rule enforcement and initial equipment transition; friendly mode.
4. Client and restart acceptance before considering staking trustworthy.

| Area | Required cases |
|---|---|
| Conservation | Stake/remove/decline/win/forfeit: inventory + escrow + pending payout + ground items conserved by ID, quantity AND metadata; repeat every terminal action |
| Boundaries | Zero/negative/excess amounts, notes, full nine-slot offers, full inventory, maximum stacks and charge-bearing tradeables |
| Consent | Either participant checks/unchecks every rule after acceptance; dependent rule changes; stale accepts and delayed X inputs; identical final snapshot |
| UI | Both names/levels at first open, accurate final stakes, correct status reset and visible change notice; fixed/resizable, relog, renamed accounts |
| Rules | Every combat/equipment restriction including already equipped two-handers, No forfeit, fun weapons, quick prayer and special entry points |
| Lifecycle | Disconnect each player at every stage, both disconnect, immediate relog, double death, countdown exit, repeated claim/close, shutdown between every transition, crash between account saves |
| Admission | Three-player overlapping requests, walk away before arrival, stale index, logout/relogin, participant already negotiating/fighting |

This source review does not certify every shared combat mechanic, client rendering, packed item data or a particular historical 2012 exploit. The old root-level dupe note describes a temporary-inventory trade bug, not proof that current duel staking uses that same mechanism. Current duel stakes are removed from the real inventory as offered.
