# Developer-client efficiency workflow — 2026-09-09

The owner authorized reusable client tooling while preserving quality. No new real client release was published. The existing v7 JAR, both maintained launcher targets, approved client behavior, caches and server runtime remain unchanged. Lobby/startup branding remains deferred.

## Implemented

- `client/developer-client.json`: one inventory for the verified baseline, 13 patch sources, five bundled assets and five available proof families.
- `tools/Invoke-ClientBatch.ps1`: Prepare source backups, Build a fresh isolated candidate, Verify selected packaged-client proofs, then Publish separately through the existing launcher. Explicit checks/reason are required at Prepare.
- `client/gambler-interface/build.ps1`: stable wrapper replacing the previous combined compile/package/cache-seed/launcher-update script. It now requires an explicit phase and batch name; running the old no-argument invocation no longer changes the live client.
- Package integrity checks compare each baseline entry and declared patch/asset. No stale compiler output is reused. Publishing validates source/tool/launcher/candidate/reference hashes, selected proof results and reference file sets; retains old JARs, backs up launchers and rejects repeat attempts.
- Four existing proof programs accept `codex.proofOutput` for isolated SVG/PNG/binary artifacts. Their old output defaults and behavioral assertions remain unchanged.
- `client/DEVELOPMENT.md` gives the workflow and dependency-based check selection. The shortened current README links its exact previous text in `client/gambler-interface/HISTORY.md`; old reports and backups remain intact.

No lower-capability model, weaker test threshold, skipped live acceptance, automatic task creation, cache authoring or broad client migration was introduced. This preserves selective compilation against the original binary; it does not solve full decompilation compilation.

## Validation

The final candidate under `build/client-batches/workflow-validation` passes:

| Proof | Result |
| --- | --- |
| Packaged Gambler interface | 81 checks |
| Packaged launcher capability and native login-string encoding | Pass |
| Cape definitions / meshes / equipment mappings | 6,037 checks |
| Original texture / native scrolling / collar bounds | 66,157 checks |
| Login branding / native software rendering | 208 checks |

Total: **72,483 counted client checks plus the capability proof**. All five are justified here because the new builder assembles the complete patch set; future edits use affected proof selection. Server gameplay suites were not rerun.

Packaging audited **942 original baseline entries**. More strongly, a separate comparison of the rebuilt candidate with the existing v7 release found **all 951 file-entry payloads identical**, with no added, removed or changed payloads. JAR archive timestamps/compression metadata need not be byte-identical. Evidence: `build/client-batches/workflow-validation/v7-entry-comparison.json`.

`tests/tooling/client-verify.ps1` passes **17 checks** against a tiny isolated client, including prepare/build/verify/publish, missing receipts, changed source/JAR/log/reference guards, prior-JAR retention, exact candidate copy, both launcher copies, one launcher, preserved Java flags, backups and repeat-publication rejection. Fixture: `build/client-tooling-tests/373d98695f8b49a6a65700c501f4d702/result.txt`.

Final real proof logs and isolated artifacts are under `build/client-batches/workflow-validation/verify-574d9ee74a074a4d9e3175dbe26c41f7`. The pre-work script/README/proofs and live JAR/launcher hash records are under `build/client-workflow-before`. The real v7 JAR/launcher hashes and exact README archive were checked after validation.

Publication was exercised only with the fixture, not the working client. Mid-copy failure restoration and process-interruption recovery were not fault-injected. A caught failure restores and verifies launchers; power loss still needs operation-record inspection/manual recovery. First-install cache setup remains separate. Future visual/input/network changes still require the relevant live acceptance from their feature reports.

**No client reopen or server restart is needed for this tooling-only change.** Earlier gameplay staging and pending live acceptance retain their existing requirements.
