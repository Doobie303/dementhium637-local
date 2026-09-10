# Project instructions

## Always preserve
- Work in `src`; loose root `combat/` is not the configured source root. Preserve unrelated local changes.
- Target early-2011 pre-EoC combat provisionally; no exact 2011 date is selected. Preserve approved custom definitions. Recheck current code before treating historical findings as active defects.
- Preserve godmode, personal XP, rewards and all private-server access conveniences, teleports and entrances. Do not restore historical quest/travel/killcount restrictions. Validate fidelity with godmode disabled.
- Summoning-specific work is deferred except necessary shared dependencies; managed instances exclude familiars. Dungeoneering migration/overhaul is deferred. Quest implementation and solo minigame ideas are review-only, not authorized by their reports.
- Keep custom PvP timers/rewards and EP policy. Barrows boosts remain 1.5x equipment chance/runes/bolt racks and 1.25x coins. Degraded items (including noted/worn forms) are untradeable even for admins.
- Preserve durable duel/gamble journals, escrow, account/house backups and saved custom items. Recover DUL2/GAM1 state before runtime downgrade; never discard duel-commit.bin or gamble-commit.bin. Read the relevant report before recovery work.
- Use the name **developer client** and the single launcher `build/gambler-interface/dev-client/Run Dev Client.bat`, maintained at `client/gambler-interface/Run Dev Client.bat`. Update it on releases; remove only superseded developer-client launchers, not originals/server launchers/backups. Preserve item 20430 and approved visuals. Lobby/startup branding fixes remain explicitly deferred.

## Read only the relevant context
Before planning or editing a subsystem, read its row in `docs/project/CONTEXT_INDEX.md` and the applicable reports. For combat, include the shared combat row. Those reports retain implementation details, owner decisions, confidence limits and live acceptance requirements. Read relevant sections, not every report on each task.

The exact previous instructions are preserved in `docs/project/AGENTS_HISTORY_2026-09-09.md`; consult its matching topic for historical nuances absent from a report. It is an archive, not a list of current defects or new authorizations. Later explicit decisions supersede only the behavior they address. Never repeat a completed audit merely to reconstruct context.

## Validation and release
- Read `tests/TESTING.md` when selecting checks. Test changed contracts and affected callers; broaden only for a concrete dependency, failure or unresolved risk. Stop after justified checks pass. Documentation-only changes need no gameplay regressions. Full compilation and full behavioral testing are separate decisions.
- For movement, timing, session, lifecycle or packet changes, include the affected production entry path and ordering in validation; direct helper tests alone are insufficient. State the behavior expected before writing assertions. See the integration matrix in `tests/TESTING.md`.
- Keep existing tests and historical verification logs. Old all-suite scripts/counts are release records, not future staging gates. Record selected suites, coverage, measured duration and untested boundaries; assertion totals are secondary. Debug failures with Debug -Suite before a full justified release Verify. Reuse tests/support fixtures where applicable.
- Use `tools/Invoke-ChangeBatch.ps1` for ordinary server Java batches; read `tools/README.md` first. Prepare before edits, verify selected suites, then stage exact declared class families with backups and SHA-256 checks. Specialized client/cache/data pipelines remain separate.
- Preserve source/runtime backups and hash manifests. Staged classes require server restart; automated checks do not certify live rendering/network/gameplay. Report relevant live acceptance. Do not restart or downgrade merely because staging succeeds.
- New instance code retains MapAllocation handles, publishes changes on the game thread and cancels owned tasks before releasing maps. Read instance reports before changing lifecycle/admission/recovery.

## Efficient continuation
Keep batch scope explicit. Reuse settled evidence and server definitions; investigate again when behavior or evidence changes. Batch independent reads, keep full logs on disk and return useful summaries. At a subsystem handoff, use `docs/project/HANDOFF_TEMPLATE.md`; record remaining work and acceptance, not another release-history copy. A fresh task is appropriate at a subsystem boundary only when the owner requests one.

Follow the current policy at the top of `docs/project/EFFICIENCY_WORKFLOW.md`: bounded context retrieval, coherent behavior batches, evidence reuse and explicit implementation/verification/staging/live-acceptance states. Repair a reproduced shared defect before extending that same defective mechanism to more content; continue independent authorized work. Efficiency never means omitting affected integration coverage or calling an untested encounter complete.

Keep this file compact. Add detail to the relevant report and update its index row, rather than appending every release here. No reduction in model capability, safeguards or required validation is implied by this workflow.
