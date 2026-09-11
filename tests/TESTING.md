# Selecting tests for a change

Use this guide when the closest useful check is not obvious or a changed runtime contract needs production-path coverage. Reuse an existing focused test first. Release/staging procedure belongs in `tools/README.md` and is read only for explicitly authorized runtime work.

## Procedure

1. Identify the changed behavior and trace callers only where a shared operation actually changes.
2. Select the smallest sufficient existing scenario set that proves the intended result and material boundary.
3. Include integration coverage for affected entry paths and ordering. A narrow file diff can change a broad runtime contract; use the matrix below. Direct helper tests alone do not validate a changed scheduler, session owner, movement path or packet route. Merely sharing `Container`, `Player`, `World` or a package is insufficient reason to run an unrelated suite.
4. Compile enough source to catch affected API/linkage errors. A full active-source compilation may be convenient or necessary; it does not imply running every gameplay test.
5. Run the selected checks. Expand when a failure or newly discovered dependency warrants it. After another code change, rerun checks affected by that change; do not automatically restart the entire suite collection.

Add a regression test only when it is likely to prevent a meaningful future failure: the bug is difficult to reproduce, likely to recur, subtle in timing/state, shared/core, or materially risky. Do not repeat successful checks without a changed implementation, relevant failure, or unresolved concern. A full sweep is appropriate when requested or when narrower coverage cannot bound a genuinely broad change.

## Integration coverage for changed runtime contracts

Apply only the relevant rows. Reuse an existing test that actually covers the contract; do not add duplicate assertions simply to satisfy a table.

| Changed contract | Minimum relevant evidence |
| --- | --- |
| NPC movement/reach | Actual follow/queue progression to contact or a justified blocked state; all approach sides, relevant body sizes/planes, offsets across eight-tile boundaries, moving targets, overlap and real clipped terrain. Show that valid cover remains effective. A direct attack from an already-adjacent fixture does not exercise pursuit. |
| Attack sessions/selection | Start through CombatExecutor with its normal action prototype; prove target-aware movement/selection, independent launched sessions and consecutive attacks. Setting Interaction directly only covers the action internals. |
| Timers/delayed effects | Exercise the actual scheduler entry order, including combat before NPC.tick where applicable. Assert no effect before the specified tick, exactly one effect on the due tick, and cancellation after reset/death/departure. Check scheduling from mechanics/callbacks separately when those callers are affected. |
| Lifecycle/ownership | Reach the transition through the real caller (death, logout, instance exit, replacement or phase change); verify pending-work cancellation, cleanup and one permitted reward/respawn. Keep mocked reward callbacks labeled as such. |
| Network/visuals | Check changed packet routing/encoding, then the affected sequence in the developer client. Cache existence, valid IDs and outgoing masks do not prove animation identity, interruption, projectile arrival or full rendering. |
| Accounting/persistence | Verify conservation, actual committed effects, replay/idempotency and relevant failure/recovery paths. Preserve genuine recovery artifacts; never test recovery destructively against production accounts. |

For a reproducible bug, first retain the failing scenario or a safe negative control against the old implementation, then show the intended correction. Do not force reproduction for a documentation edit or a trivial reversible change. Direct method tests remain valuable for broad damage/formula samples; pair them with a small number of production-path scenarios when integration is the risk. A fixture that zeroes cooldowns, supplies a chosen attack or manually advances a clock must not be presented as testing natural selection/cadence/scheduler order.

If the test must bypass the relevant production path because a fixture cannot model it, mark that contract unverified and name the concrete follow-up; do not substitute an unrelated full-suite pass. Broaden tests when a real dependency or failure requires it. Keep stochastic coverage and meaningful boundaries; do not shrink loops to make assertion totals look cheaper.

## Examples by subsystem

| Change | Relevant default coverage | Expand only when |
| --- | --- | --- |
| Bank amount validation or full-bank deposit | Bank regression; applicable numeric-input route; item conservation | A shared item operation or persistence format changes |
| Shop sale quantity/payment calculation | Shop sales, inventory/stock capacity and currency boundaries | Shared inventory/currency behavior changes for other consumers |
| Ordinary trade offer/capacity fix | Offer/remove, both parties, confirmations, cancellation, stack limits | Shared transfer semantics, saving or another activity's interface handling changes |
| Shared `Container.tryAddAll` behavior | Container boundaries and affected callers, potentially duel stakes and Gambler recovery | Other callers rely on the altered stacking/metadata/overflow contract |
| New trade-only helper in `DegradingHandler` | Trade restriction and relevant item variants | Existing combat degradation behavior also changes |
| Gambler result colors or labels | Result messages/layout and the relevant client acceptance | Gameplay settlement, shared scripts or other interfaces are affected |
| Documentation or testing policy | Review text, links and consistency | Executable behavior also changes |

For headless combat setup, consult `tests/support/README.md`. Keep encounter-specific behavior in its suite; generalize a fixture only when another consumer benefits. Do not delete unrelated suites or rewrite historical verification records.
