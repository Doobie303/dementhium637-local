# Efficient development with preserved quality

## Current policy — revised after NPC/combat live feedback, 2026-09-09

Objective: maximize useful, validated server work within the owner's weekly allowance. Preserve the selected model capability, safeguards, backups, custom definitions and required acceptance. The policy revision below changed workflow documents only. The subsequent control review implemented the specific tooling improvements recorded below; neither change fixes the pending NPC defects or changes gameplay, model settings, plugins, accounts or live runtime.

### Evaluation of the earlier efficiency changes

There is no evidence that instruction consolidation, Debug selection or fixture reuse directly changed gameplay. There is evidence that the validation approach was incomplete. Reducing redundant work was appropriate; equating direct action coverage with runtime integration was not. The causal distinction matters:

| Finding | Relationship to the efficiency work |
| --- | --- |
| Large-NPC coordinate loop | Faulty conversion predates today's efficiency work and remains unchanged against the repository baseline. The missing coverage was large-body approaches across sector offsets, not damage-roll volume. |
| Nex pursuit regression | Introduced by the production change to independent attack sessions. Movement still read the prototype's now-unset Interaction. The later fixture extraction did not introduce session isolation or remove an executor-pursuit test. |
| Encounter early impacts | Production clock/entry-order defect. Direct launches followed by manually advanced NPC clocks missed the combat-before-NPC.tick boundary. |
| Fixture extraction | Before/after comparison of AdvancedBossRegression and NexDamageRegression shows setup reuse, with the existing scenarios/assertions retained. Nex already set victim/cooldown and launched an explicit session before extraction. Both versions lack the needed natural pursuit/clock checks. |
| Missing-looking Bandos animation | Cause still open. The review observed the correct outgoing attack sequence and selected cadence, but did not certify client playback. Earlier reports already listed live presentation as pending. |

The initial workflow tooling validation was recorded around 13:02; the later fixture/Debug follow-up around 15:10. Nex and EncounterNPC source writes were observed around 14:41 and 14:13 respectively. Timestamps are supporting chronology, not proof of exact runtime contents. Before/after fixture diffs, preserved release records and the reproductions are stronger evidence. `build/workflow-efficiency-fixtures/summary.json` records unchanged runtime during that follow-up. Full NPC findings and diagnostic logs are indexed in `docs/project/NPC_COMBAT_LIVE_REVIEW_2026-09-09.md`.

We cannot establish that running fewer suites caused these escapes: no omitted suite has been shown to contain the missing scenarios. The policy needed a clearer definition of sufficient coverage. The new integration matrix in `tests/TESTING.md` supplies it. Repeating thousands of passing damage assertions would not resolve a missing pursuit or clock-order test.

### Working sequence

1. **Retrieve the active contract.** Read the relevant context-index row, current report sections, applicable owner decisions and current code. Reuse settled evidence. Search headings/symbols with `rg` before selecting bounded excerpts; expand only when a concrete question remains. Do not concatenate multiple long reports or dump the whole working tree to reconstruct one subsystem. Preserve required nuance even when it costs more context.
2. **Define a coherent behavior batch.** State the intended result, changed contract, affected callers and what proves success in a few lines in the existing report or handoff. Include the implementation and its relevant tests together. Group tightly coupled changes; do not create one release per line or add adjacent features merely because the files are open. Prepare the explicit release manifest/backups before production edits.
3. **Prove the relevant failure and integration.** For regressions, keep a decisive reproduction where feasible. Use the actual entry path for changed movement, timing, session, lifecycle or packet behavior. Direct helper tests cover internals; a small production-path set covers the connections. Expected results come from the selected behavior contract, not values copied unquestioningly from the new implementation.
4. **Implement, inspect the final diff, and debug locally.** Reuse fixtures and stable tooling; verify APIs before inventing setup. Use Debug for failures and affected callers, retain complete logs, and read the smallest useful error excerpt. Review target/state ownership, tick/coordinate units, downstream callers and preserved custom rules before final Verify. This is a focused self-review, not an automatic second audit or delegation.
5. **Verify the justified release once it is ready.** Keep fresh compilation, hash/input guards, backups and exact-family staging. Rerun for relevant edits, failures or dependency drift; reuse unchanged research and diagnosis. Never reuse a success receipt whose inputs changed. Full compilation and a full behavioral sweep remain separate scope decisions.
6. **Track completion honestly.** Record implemented, automated verified, staged, loaded after restart, and live accepted as separate facts. Unknown loaded-runtime identity stays unknown. An encounter remains pending where its full fight or rendering acceptance is missing. If a live defect is reproduced in a shared mechanism, repair it before extending that mechanism to more dependent content; continue independent authorized work. Live availability is not a reason to stop all useful coding or silently waive acceptance.

No new approval checkpoint is introduced by this sequence. Continue within existing authorization; carry unresolved acceptance explicitly. Use `docs/project/HANDOFF_TEMPLATE.md` at meaningful boundaries or before a long pause. It is a compact resumption record, not a second release-history report. Do not create new tasks or background agents unless requested under the applicable instructions.

### Reduce repeated work and tool overhead

2026-09-09 NPC repair continuation: final validation encountered a persistent read/write handle on unchanged `data/objects/packedKeys.bin`. The release tool now retries an I/O sharing conflict using a read-only shared stream, retains complete SHA-256 comparisons and rejects size/write-time changes during hashing. Focused checks prove normal/shared SHA equality, rejection of a changed dependency, missing input and exclusive lock. The three-suite NPC instance-edge release then verified and staged with the updated tool. Evidence and original tool backup: `build/workflow-shared-read-hash-20260909/`; gameplay details remain in the NPC repair report. No process closure, skipped hash or reduced validation was used.

- Keep one current report entry for a subsystem's decisions, known defects and next acceptance. Add only new evidence and link existing logs. Do not rewrite settled audits or repeat historical pass totals in every update.
- Save long logs/data on disk and return concise findings with paths. If output is truncated, retrieve the specific missing section instead of repeating the whole command. Batch independent reads; keep adaptive reads, edits and validation sequential.
- Reuse tested scripts for release and specialized pipelines. For genuinely one-off probes, use a small reproducible script in an isolated build directory, label its scope and preserve its results. Do not turn every diagnostic into new permanent tooling.
- Use PowerShell syntax and inspected paths. Avoid repeated compiler failures caused by the already-known Windows JAR-access limitation; use the established scoped approval route when needed, without weakening permissions. Collect hash inputs before writing the output manifest and exclude that manifest from its own input set.
- Track source, on-disk runtime and running-process evidence separately. Check affected artifact identity when release/live evidence disagrees; avoid repeated full-bin inventories during source-only diagnosis. Do not treat an Eclipse rebuild or hash difference alone as proof of a bug or permission to roll back.
- Keep frequent progress updates focused on new findings and consequential decisions. Concise output must still explain relevant failures and remaining work. Do not economize by ending an authorized implementation early.

The preceding NPC review itself over-read several long reports, produced truncated outputs and retried avoidable shell/hash mistakes. These consumed effort without improving the diagnosis. Bounded retrieval and consistent scripts are practical savings to apply immediately, alongside better tests that reduce later rework.

### Usage and quality measurement

Official OpenAI documentation says allowance consumption depends on model, task complexity, context, reasoning and tool use, among other factors. It recommends limiting unnecessary context/source material and matching output to the task. Prompt length alone is not a reliable allowance estimate. These support the retrieval changes; they do not establish a percentage saving for this repository. [Official usage guidance](https://learn.chatgpt.com/docs/pricing#what-can-i-do-to-make-my-usage-limits-last-longer).

Use existing batch summaries to record the selected contract/scenario coverage, compiler/test/total duration, failed attempts and live acceptance. At a meaningful subsystem checkpoint, note repeated causes of rework and whether more work reached live acceptance. Do not add a separate reporting task or repeated usage polling to every tiny change. When account usage is requested, distinguish shared account consumption from task-specific attribution. Do not infer token cost from test seconds or assertion totals, promise extra weekly hours, or purchase/redeem usage automatically.

Success means more validated changes with fewer avoidable retries, repeated investigations and escaped defects. Keep the current model/reasoning choices; this plan makes no capability downgrade and no automatic plugin/settings changes. Necessary integration tests may increase a particular batch's work while preventing a larger repair later. No workflow can guarantee zero defects; unverified boundaries must remain visible.

### Validation of this policy revision

Documentation-only scope: AGENTS.md, this report, context index, testing guidance, tools README and handoff template. Pre-edit copies and SHA-256 hashes are in `build/workflow-quality-20260909/`. Validate report-link preservation, root safeguards, compactness, consistency and the recorded causal evidence. No gameplay/tooling implementation changed, so gameplay suites, compiler runs and staging tests are not justified for this revision. The existing historical tooling results below remain unchanged.

## Control review and implemented improvements — 2026-09-09

Reviewed the current efficiency policy, context routing, server Prepare/Debug/Verify/Stage controls, developer-client Build/Verify/Publish controls and their tests. The existing requirements for scoped integration coverage, backups, input hashes, separate release states and live acceptance remain appropriate. No wholesale policy rewrite or reduction in checks was justified.

### Server verification must match what Stage installs

Two isolated reproductions showed false verification before this update: an undeclared suite could run from an old `bin` class, and tests could pass using a freshly compiled dependency omitted from `stageSources`. In the second case, staging the tiny fixture left the old dependency in place and the same test failed. Evidence and the reproduction script are preserved in `build/workflow-controls-review-20260909/reproduction.json` and `reproduce.ps1`; these staged only child fixtures, never the real server.

`tools/Invoke-ChangeBatch.ps1` now compiles production sources, constructs an isolated candidate containing the exact staged class families plus retained runtime files, and compiles/runs tests against that candidate. Replaced families exclude obsolete inner classes. Every selected suite must be freshly compiled for the candidate. Duplicate test/runtime class names are rejected, and staged paths are normalized so path aliases cannot retain obsolete classes. Candidate files and test outputs join the existing receipt hash/file-set checks. Debug still cannot authorize staging; source backups, runtime backups, drift checks and exact-family staging remain in force.

This adds a second compiler invocation when test sources are present and copies retained `bin` files. That cost prevents misleading success and later rework; it is not a claimed speedup for every server batch. `summary.json` reports candidate assembly separately as `CandidateSeconds`, alongside compilation, suites and total time.

### Client dependency checks follow the selected proofs

`tools/Invoke-ClientBatch.ps1` now hashes the union of external inputs used by the selected proofs. A capability-only proof no longer requires or repeatedly hashes server/cache/cape inputs. The previous unconditional directory scan covered 1,162 files totaling about 606 MB in this workspace. Runtime/cache-dependent proofs retain those inputs; new proofs can declare documented dependency groups, while unknown legacy proofs conservatively retain the complete reference scope. Java, candidate/build integrity, baseline auditing, backups, dependency drift and launcher publication guards remain enforced. Required proof selection and live acceptance in `client/DEVELOPMENT.md` are unchanged.

`tools/README.md` records these contracts and explains source-backup provenance when batch scope expands after edits. The shared-combat context row now distinguishes historical step-5/BGS status from the current repair register, avoiding repeat investigation of completed work. Existing receipts tied to the older tooling cannot be reused: rerun server Verify, or client Build then Verify, for an open batch as documented. Preserve closed releases and their evidence.

### Validation and remaining boundaries

| Selected validation | Coverage and measured result | Evidence |
| --- | --- | --- |
| Server tooling fixtures | 145 checks passed in 35.605 seconds: fresh-suite enforcement, unstaged dependencies, removed inner classes, source aliases, class shadowing, candidate/test-output drift, Debug behavior and existing backup/staging guards. Real server `bin` hashes unchanged. | `build/tooling-tests/e5e36124527c49c3a2056c78617d047f/result.txt`; `build/workflow-controls-review-20260909/server-tooling-final-run.json` and full log |
| Client tooling fixtures | 38 checks passed in 10.069 seconds: dependency union/defaults, self-contained checks, unknown/invalid groups, relevant file changes/additions/deletions and existing publication guards. | `build/client-tooling-tests/ea324c691ef443a39acb0a3c947b293d/result.txt` |
| Actual developer-client candidate | Prepare/Build/Verify passed all five required infrastructure proofs in 12.758 seconds: interface, capability, cape, texture and login; 942 baseline entries audited. Both launchers and the active client JAR retained their hashes. No Publish. | `build/client-batches/workflow-client-dependencies-03348b74/`; `build/workflow-client-controls-before/7fca6a1f24254e3698d3a377f12641ac/workflow-client-dependencies-03348b74-result.json` and full log |

Pre-edit source copies and hashes: `build/workflow-controls-review-20260909/before-hashes.csv` and `build/workflow-client-controls-before/7fca6a1f24254e3698d3a377f12641ac/manifest.json`. Initial reproductions, the earlier server fixture run and historical validation logs remain preserved.

No production Java, real server staging, client publication or restart occurred. Tooling fixture coverage does not certify gameplay or client rendering. No gameplay sweep was needed for these tooling changes; existing combat repairs and restart/live acceptance remain pending in their reports. Mid-copy failure recovery and process interruption were not newly fault-injected. No percentage saving in account allowance is asserted.

## Historical implementation record — 2026-09-09

The owner requested less repeated work without reducing quality. This release changes project instructions and development tooling; no gameplay implementation, server runtime, accounts, cache, client or model settings changed.

## Context consolidation

AGENTS.md decreased from **34,590 to 4,330 bytes (87.5%)**. This measures file size, not a promised reduction in weekly usage. The root now contains global safeguards and routes to a topic index. The index retains **all 40 original Markdown report references**, and validation confirms each file exists. The root retains critical owner policies, deferrals, durable recovery constraints, launcher policy, instance ownership requirements and scoped testing/release requirements.

The exact original AGENTS.md was copied and hash-compared before replacement to docs/project/AGENTS_HISTORY_2026-09-09.md. Its SHA-256 is:

`6C10A56A7107973EAF89C69B8751F30C1DE0B9B577606296876EDF0346FC6EF3`

Reports, source/runtime backups and historical suite records remain intact. The archive is historical; current decisions are found through docs/project/CONTEXT_INDEX.md. Future changes should update their reports/index rather than append every release to the root instructions.

## Reusable release workflow

tools/Invoke-ChangeBatch.ps1 accepts an explicit JSON manifest and three phases: Prepare, Verify and Stage. It provides pre-edit source snapshots (including records for new files), selected Java 8 compilation/tests, isolated fresh attempt outputs, dependency/output hashes, guarded exact-family staging, prior-runtime backups and restoration on caught staging failures. Prepare and Verify do not stage classes. Inputs changed since validation, dependency/output file-set changes, failed or incomplete tests and repeat staging attempts are rejected.

See tools/README.md for configuration, supported boundaries and recovery limits. No recursive cleanup or guessed dependency/suite selection was added. The runner is for ordinary server Java batches; client packaging, packed data changes and save-format recovery still need their established specialized procedures. Arbitrary Java test side effects are not sandboxed by this script. Process interruption during staging still requires inspection/recovery from its operation record; it is not a crash-proof live deployment system.

Additional adopted improvements: a compact handoff template, explicit suite-selection reasons, reusable settled research, and concise output with full logs retained on disk. No model downgrade, automatic task creation, background agent work, settings change or removal of required acceptance testing was introduced.

## Validation

`tests/tooling/verify.ps1` passed **115 checks** using actual installed javac and Java 8 against tiny fixture classes. Checks cover:

- Preservation and existence of all original report references; global safeguards and reduced root size.
- Source backups, successful compile/test/stage, runtime hashes/backups, replacement inner classes, removal of obsolete inner classes, sibling preservation and exclusion of test classes from runtime.
- Failed tests even when printing a success marker, missing success markers, compilation failure and invalidation of a prior pass after unsuccessful re-verification.
- Changed sources, added runtime dependencies, modified/added class outputs, modified logs, changed manifests, path escape rejection and repeated Prepare/Stage rejection.
- Hash comparison confirming the real server bin remained unchanged throughout the fixture run.

Artifacts: build/tooling-tests/b4071a67e26a4e0ba475831f32fe9838/result.txt and its fixture directories. Testing was scoped to the new workflow; no unrelated gameplay regressions were rerun. Mid-copy failure restoration and process-interruption recovery were reviewed but not fault-injected in this run. Real production release use and exceptional recovery remain boundaries beyond the isolated fixture acceptance.

No restart is needed for this workflow release. Previous gameplay staging (including God Wars) still requires its already-recorded restart/live acceptance; this release does not certify those checks as complete.

## Efficiency follow-up — diagnostic selection and reusable fixtures

Implemented after boss step 5, 2026-09-09. The four approved improvements are now executable tooling and project guidance:

1. Invoke-ChangeBatch.ps1 adds Debug -Suite for focused failure investigation. It compiles fresh declared sources, runs only named manifest suites, invalidates old verification approval and cannot authorize staging. Verify still runs the complete justified release selection; Stage retains all hash/backup guards.
2. tests/support/CombatFixtures.java centralizes the shared real-definition/headless-player setup. AdvancedBossRegression and NexDamageRegression use it, with explicit readyAttack setup for direct Nex actions. Helper consumers must declare it in new manifests; old staged manifests/receipts remain historical and unchanged.
3. tests/TESTING.md codifies narrow context reads, saved evidence, compact failure excerpts and full logs on disk. Existing context-index routing remains authoritative. The runner prints the last 12 failure-log lines and its path instead of dumping the complete log.
4. Debug/Verify write measured per-suite, compilation and total timings in summary.json, including failed attempts. Routine output leads with suite, duration and coverage. AGENTS.md and the handoff template make automated assertion counts secondary. No weekly-usage savings percentage is claimed.

Validation: the tooling integration suite passed (132 automated assertions) using isolated Java fixtures, including subset execution, invalid names/options, diagnostic receipt invalidation, failure status/durations, all-suite diagnostic rejection for staging, full final verification and existing hash/backup guards. Artifacts: build/tooling-tests/6bd74a706ac042459ad73f4a15aae8a1. The two actual shared-fixture consumers also passed: AdvancedBossRegression in 1.740 seconds and NexDamageRegression in 0.855 seconds; compilation took 1.071 seconds. Their existing 20,918 and 750 assertions were retained. Artifacts: build/workflow-efficiency-fixtures/summary.json and full logs. A compile-only import error was fixed before those suites ran; no passed gameplay suite was rerun for that fix.

Real server bin hashes remained unchanged in both validations. No production Java, account, cache, client JAR, launcher or model settings changed. No new runtime staging or restart is needed for this workflow update; earlier gameplay restart/live acceptance requirements still apply. Pre-edit changed-file copies are under build/workflow-efficiency-before. Historical release records remain intact.

The client uses the same scoped-debugging/reporting policy, but its existing proof and candidate Verify/Publish commands are unchanged. Do not pass the new server-only Debug option to the client runner. Future fixture reuse should follow an actual common contract; no broad test rewrite or reduction in coverage is required.
