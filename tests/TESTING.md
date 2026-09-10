# Selecting tests for a change

For new ordinary server Java batches, use `tools/Invoke-ChangeBatch.ps1` and its manifest workflow in `tools/README.md`. Declare each selected suite's reason; compile helpers without automatically executing their suites. Prepare source backups before edits, Verify, then Stage separately. Specialized client/data/recovery pipelines remain separate. Historical runners are unchanged release records.

Owner preference, recorded 2026-09-09: validate the affected behavior thoroughly without automatically running every server suite. This policy governs future selection; historical all-suite reports remain accurate records of what was run.

## Procedure

1. Identify the changed behavior, methods, data and persistence/protocol contracts. Trace callers where a shared operation actually changes. Distinguish new helper methods from changed existing behavior.
2. Select the smallest sufficient set of scenarios that proves the intended behavior and relevant boundaries. Define expected behavior independently of the implementation: include a concrete trigger, result and important negative case. For an item transfer, this normally means conservation, invalid inputs, capacity/overflow and success/failure behavior.
3. Include integration coverage for affected entry paths and ordering. A narrow file diff can change a broad runtime contract; use the matrix below. Direct helper tests alone do not validate a changed scheduler, session owner, movement path or packet route. Merely sharing `Container`, `Player`, `World` or a package is insufficient reason to run an unrelated suite.
4. Compile enough source to catch affected API/linkage errors. A full active-source compilation may be convenient or necessary; it does not imply running every gameplay test.
5. Run the selected checks. Expand when a failure or newly discovered dependency warrants it. After another code change, rerun checks affected by that change; do not automatically restart the entire suite collection.
6. Record the selected suite names, reasons, results and material limits in the batch report. Use their success as the new batch's staging gate. Keep source/runtime backups and hash verification separate from behavioral test scope.

Do not repeat successful checks without a changed implementation, relevant failure or unresolved concern. A full sweep is appropriate when requested by the owner, for a deliberately broad release verification, or when a change genuinely affects many subsystems and narrower coverage cannot reasonably bound it. Explain the reason rather than using the number of previous suites as a target.

## Integration coverage for changed runtime contracts

Apply only the relevant rows. Reuse an existing test that actually covers the contract; do not add duplicate assertions simply to satisfy a table. Record coverage and remaining limits in the existing batch report or handoff, not a second mandatory report.

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

## Applying this to the item-transaction batch

The previous batch ran 28 suites. That was broader than necessary for its actual changes.

`ItemTransactionRegression` directly covers bank deposits/charged withdrawals, shop accounting, both trade participants, complete capacity checks, degraded-item restrictions and the bank numeric-input packet route. The changed existing `Container.hasSpaceFor` method is used by ordinary trade; the new copy/replace helpers are used by the patched transaction paths. `tryAddAll` itself was not changed. The new trade-classification method did not alter combat degradation rates or existing combat callers.

Consequently, the presence of Gambler recovery callers of unchanged `tryAddAll` does not by itself require the Gambler game suite. Nor did the fixes require a full boss, combat, instance-load or cape suite sweep. A focused transaction regression, appropriate compilation and relevant live bank/shop/trade acceptance would have been the default selection. Additional checks need a specific affected contract to justify them.

Existing test sources remain available for future applicable changes. Do not delete unrelated suites or rewrite old verification logs, counts or staging gates to suggest they were not run. For a new batch, create a fresh runner with an explicit justified selection instead of copying a historical all-suite runner unchanged.

## Efficient test development and reporting

Owner-approved follow-up (2026-09-09):

- Debug only the failing suite and affected callers using the runner's Debug -Suite option. Once ready to release, run the complete justified manifest selection with Verify once; repeat only if changes or failures require it. A diagnostic pass never authorizes Stage. Keep compilation, backup and hash guards intact.
- Reuse suitable existing fixtures. Start with tests/support/README.md for headless combat setup. Check real API signatures with rg before adding setup code; do not guess inventory clearing APIs or implicit executor cooldown state. Keep encounter-specific behavior and assertions in their suites. Generalize only when an actual second consumer benefits.
- Read the relevant context-index row and report sections. Search for the changed method/contract, batch independent reads, and retain full output on disk. Read a compact failure excerpt first and expand only enough to explain it. Do not repeatedly dump full reports, sources, successful logs or unchanged runtime data into the conversation.
- Report suite names/coverage and measured execution duration first. Label assertion totals as automated assertions and keep them secondary. Distinguish test time from compilation/hash time, and measured duration from estimates. Keep live acceptance limits visible. Do not reduce useful loop coverage merely to lower the count, or promise a weekly-usage percentage from wall time or assertion counts.

These rules apply to server and developer-client work. The new Debug switch belongs to the server batch runner; client proof diagnostics still use their existing proof commands and must be followed by the full justified candidate Verify selection before Publish. No client release gate is relaxed.
