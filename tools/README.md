# Scoped server batches

Use `Invoke-ChangeBatch.ps1` for ordinary server Java changes. It replaces copy/pasted batch commands; historical scripts remain release records. Read `tests/TESTING.md` to select suites. Client packaging, packed-data mutation, save migrations and special recovery steps still use their relevant reviewed pipelines.

For developer-client Java/resources, use `Invoke-ClientBatch.ps1` through the maintained `client/gambler-interface/build.ps1` entry point. Read `client/DEVELOPMENT.md`. Its separate Prepare/Build/Verify/Publish workflow preserves the stable launcher; do not use server class staging for client JARs.

## Configure once per shell

Pass `-Javac` and `-Java`, or set `CODEX_JAVAC` / `CODEX_JAVA`. Otherwise executables on PATH are used. javac must support `--release 8` (JDK 9+); use the Java 8 runtime for server regression compatibility. This workspace's known installed paths at consolidation:

```powershell
$env:CODEX_JAVAC = 'C:/Users/Tcarn/Desktop/eclipse/plugins/org.eclipse.justj.openjdk.hotspot.jre.full.win32.x86_64_21.0.11.v20260515-1531/jre/bin/javac.exe'
$env:CODEX_JAVA = 'C:/Program Files/Java/jre1.8.0_503/bin/java.exe'
```

These are local session settings, not changes to the developer client, server launcher or machine PATH. Supply updated paths if installations change. PowerShell 7 is the tested shell.

## One manifest per change

Copy `tools/batches/godwars-example.json` to a descriptive new manifest and edit **before Prepare**. The example documents an already-completed release and is not an instruction to rerun it.

- `name`: unique lowercase batch slug. Existing output/backups are never reused by Prepare.
- `scope`: authorized change and its boundary.
- `sources`: explicit Java sources to compile, including required test helpers and affected production callers. Paths start with `src/` or `tests/`.
- `stageSources`: production sources whose exact class families will replace runtime files; must be included in `sources`.
- `inputs`: other relevant file dependencies, such as definitions, spawns or settings. List actual files. This is an explicit dependency declaration, not automatic data discovery.
- `suites`: selected Java main classes, arguments (empty array if none), an identifying success regex, and why each is affected. Both zero exit code and the success marker are required. Fixtures/helpers need not be executed as suites.

```powershell
./tools/Invoke-ChangeBatch.ps1 -Phase Prepare -Manifest tools/batches/my-change.json
# Make the authorized source/test changes.
./tools/Invoke-ChangeBatch.ps1 -Phase Verify -Manifest tools/batches/my-change.json
./tools/Invoke-ChangeBatch.ps1 -Phase Stage -Manifest tools/batches/my-change.json
```

Prepare snapshots existing declared sources/data before edits and records absent new files. Never claim a snapshot taken after edits is a pre-change backup. Changing scope/manifest requires a new batch; preserve prior snapshots. Verify can be retried after a failed test with new attempt directories, but it removes any previous success receipt first. Stop after appropriate checks pass.

If scope expands after edits, prepare the replacement batch before editing newly added files. Preserve and link the earlier batch's original backups for already-edited files; the replacement snapshot records their intermediate state, not their original state.

Compilation uses an empty source path and `-implicit:none`; undeclared source dependencies cannot silently appear in the release. Add changed dependencies/callers to `sources` explicitly and production replacements to `stageSources`. Production compilation checks all declared production sources. The runner then constructs an isolated `runtime-candidate` from the retained `bin` files and the exact new `stageSources` families, excluding every old member of those replaced families. Tests compile separately against that candidate and `lib/*` and execute with their fresh test classes. Unstaged production source cannot accidentally supply a newer implementation to the tests, and obsolete inner classes cannot leak back from live `bin`. Each selected suite must itself have been freshly compiled; a matching old class in `bin` or a library cannot authorize Stage.

Staged source paths are normalized before class-family matching. A test class that duplicates a candidate runtime class is rejected, so the test classpath cannot replace the implementation being verified. Keep test-only classes outside `bin`.

This tool supports the repository's normal source-path-to-class-family convention; unusual extra top-level classes, renamed/deleted families or special generators require explicit reviewed handling, not guessed deletion. The candidate adds temporary disk/copy work to verify the actual planned release; it does not write to the real runtime during Debug/Verify. Compiled production output, candidate dependencies and test classes all receive output/hash guards.

Verify writes complete logs, tool/source/input/runtime dependency hashes and compiled-output hashes under `build/batches/<name>/verify-<id>`. It checks inputs/runtime stayed unchanged across the run. Tests run from the selected workspace like existing runners: select tests that safely use their fixtures; the tool does not sandbox arbitrary Java test side effects. It does not stage runtime classes during Verify.

If an existing read/write handle conflicts with the ordinary SHA-256 command, hashing falls back to a read-only stream sharing read/write access. It still computes the complete SHA-256 and rejects size/write-time changes during that read; all before/after and staging content comparisons remain. It does not bypass exclusive locks or permit missing inputs. Focused checks and the prior tool backup are in `build/workflow-shared-read-hash-20260909/`.

Stage rechecks all recorded hashes and the runtime/output file sets, backs up every affected runtime file before changing any, then copies only declared class families and removes their obsolete inner classes. Sibling class names and test classes remain untouched. Runtime backups and operation records include hashes and previously absent files. On a caught mutation failure it attempts restoration and verifies restored hashes. A `stage-started.json` marker blocks repeat attempts; inspect `staged.json` or failure state first. Do not delete markers to bypass a guard.

There is no automatic restart, live-server atomic hot reload, crash-proof deployment or automatic rollback command. A process interruption during staging requires inspecting the operation record and recovering from backups. Arrange a quiet release window; re-verify after dependency changes. Runtime restart and relevant live acceptance remain required. Preserve save-format/journal constraints from AGENTS.md and subsystem reports before any manual rollback.

Treat compiler output ownership explicitly during a release. This repository's Eclipse output also targets `bin`; another build can replace staged classes. Avoid concurrent writers during Verify/Stage and retain the runner's hash checks. A later hash mismatch means the old receipt no longer proves the current artifact identity; it does not by itself prove a gameplay regression or authorize rollback. Compare the affected families and current source/build evidence, then make a fresh verified release when needed. Do not disable IDE settings or restart processes merely to enforce this guidance without task authorization.

Suite success and matching hashes prove only the declared automated checks and artifacts. They cannot assess whether the selected tests cover the right production path. Use the integration matrix in `tests/TESTING.md`; record implementation, automated verification, staging, loaded-runtime evidence and live acceptance separately in the existing report.

## Validate the tooling

```powershell
./tests/tooling/verify.ps1 -Javac $env:CODEX_JAVAC -Java $env:CODEX_JAVA
```

This compiles tiny fixture classes and exercises the actual runner under a new child workspace in `build/tooling-tests`. It does not stage the server's `bin`, rerun gameplay suites or change accounts/cache/client files. Historical batch manifests and backups remain unchanged.

## Debug one failure, verify the release once

After a failure, use the diagnostic phase for the failing suite and any concretely affected callers:

```powershell
./tools/Invoke-ChangeBatch.ps1 -Phase Debug -Manifest tools/batches/my-change.json -Suite NexDamageRegression
# Multiple affected suites: -Suite NexDamageRegression,AdvancedBossRegression
# Once the fixes are ready, run the complete justified manifest selection:
./tools/Invoke-ChangeBatch.ps1 -Phase Verify -Manifest tools/batches/my-change.json
./tools/Invoke-ChangeBatch.ps1 -Phase Stage -Manifest tools/batches/my-change.json
```

Debug requires exact suite names from the prepared manifest. It compiles all declared sources into fresh output, preserving dependency correctness, but executes only the requested suites. It invalidates an earlier verified.json and never creates a release receipt, even when all suites are named. Verify rejects -Suite and always runs the full manifest selection. No automatic reuse of old test passes or stale class output was added. Already-staged batches remain closed; create a new manifest for new work.

Both Debug and Verify write summary.json beside their full logs, on success or failure. It records the selected suites, coverage reasons, per-suite results/exit codes and elapsed seconds, compilation seconds, candidate-assembly seconds, suite-execution seconds and total attempt seconds (including remaining hash work). Routine output shows suite, duration and coverage; failures show at most the last 12 log lines plus the full-log path. Counts inside Java output remain available in the log and do not represent individual scenarios or model requests. A process termination cannot guarantee a final summary.

When changing test setup, consult tests/support/README.md before inventing another fixture. List any reused helper source explicitly in a new batch manifest. Historical manifests and receipts remain release records; never edit them to accommodate a new helper dependency.

## Client proof dependencies

The separate client runner hashes the union of the selected proofs' external dependencies. Current defaults are `capability`: none; `interface`/`login`: runtime and cache; `cape`: runtime, cache and cape references; `texture`: runtime, cache and texture reference. Candidate/build inputs and the executing Java tool remain guarded for every selection. A capability-only check no longer reads unrelated server/cache files or loses its receipt when those files change. Cache-dependent checks still enforce both hashes and the exact dependency file set.

New catalog proofs may declare `dependencyGroups`: `runtime` (`bin`/`lib`), `cache` (`data/cache`), `cape` (equipment mapping/custom item reference), and `texture` (texture asset), or an explicit empty array for self-contained proofs. Declare the actual dependency closure; this scopes evidence, not which required proofs to select. Unknown legacy proofs without a declaration conservatively retain all groups. `client/DEVELOPMENT.md` still governs affected proof selection and live acceptance.

Runner changes invalidate older success receipts through the existing tool-hash guards. For an open server batch, rerun Verify with the current tool; for an open client batch, rebuild and reverify. Preserve earlier records and backups. A staged/published batch remains closed and requires a new batch for further work.
