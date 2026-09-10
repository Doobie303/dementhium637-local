# Developer-client workflow

Use the existing developer client and its stable `build/gambler-interface/dev-client/Run Dev Client.bat`. The maintained launcher source is `client/gambler-interface/Run Dev Client.bat`. This workflow supports its selective binary patches, not a replacement client or a full decompilation build.

## Start a change

Read the relevant reports through docs/project/CONTEXT_INDEX.md and the testing policy. Select a unique batch name and affected checks before editing:

```powershell
./client/gambler-interface/build.ps1 -Phase Prepare -Name login-layout-fix -Checks login,interface -Reason 'Changes shared interface loader visibility and login layout.'
# Make the authorized source/asset changes.
./client/gambler-interface/build.ps1 -Phase Build -Name login-layout-fix
./client/gambler-interface/build.ps1 -Phase Verify -Name login-layout-fix
# After inspecting results and completing any required release review:
./client/gambler-interface/build.ps1 -Phase Publish -Name login-layout-fix
```

The sample name/scope illustrates syntax, not permission to work on deferred branding. Use your actual authorized change. `-Jdk`, `-Java` and `-Baseline` override the existing known local paths when installations move. PowerShell 7 and Java 8 execution are tested. The underlying entry point is tools/Invoke-ClientBatch.ps1.

Prepare backs up all currently catalogued source/assets/proofs and the launcher source, recording selected checks and their justification. New files added afterward have no pre-existing version to preserve. Existing files first added to the catalog must be backed up before editing them. Build creates a fresh candidate per attempt; no cached output reuse or stale class directory is involved. All 13 current patch sources compile together because assembly is small and preserving cumulative features matters. Proof selection remains scoped to affected behavior.

## Patch inventory and checks

`client/developer-client.json` is the single inventory for baseline hash, patch sources, resource-entry mappings and packaged-client proofs. Add new maintained source/assets there; do not add another hard-coded packaging command. Keep proofs separate from shipped classes.

External proof inputs are hashed for the selected checks only: capability is self-contained, while the cache-based checks retain their relevant runtime/cache/reference guards. New proofs may declare `dependencyGroups`; see [the dependency groups](../tools/README.md#client-proof-dependencies). This does not reduce the required proof selection below or its live acceptance.

| Changed behavior | Relevant checks / additional acceptance |
| --- | --- |
| Gambler widgets or shared Class85 loader | `interface`; add `login` when login decoration/loading can be affected. Live fixed/resizable buttons, icons and reopening. |
| RunClient settings / capability advertisement | `capability`; affected server capability decoder tests if the wire contract changes. Actual login is a separate acceptance step. |
| Item/equipment/model overrides | `cape`; add `texture` when materials, geometry or Class98_Sub6 change. Live male/female fit, motion and old-client appearance fallback. |
| Texture pixels / HDR / scrolling / cape collar | `texture`; add `cape` for item/model mapping dependencies. Live software/OpenGL fit and motion. |
| Login artwork / Class215 / LoginBranding | `login`; add `interface` if shared Class85 changes. Live form/input/settings, fixed/resizable and renderer checks. Lobby/startup changes are deferred. |
| Packaging inventory / compiler / release infrastructure | Full five-proof selection for the current complete assembly, plus tooling fixture tests. This justified selection does not become the default for every future visual edit. |
| Connection routing, protocol, new features | Add a focused proof to the catalog and select relevant existing callers; existing checks do not certify all networking or newly introduced behavior. |

Each candidate is always checked structurally: original JAR hash, duplicate-entry rejection, every original non-patch entry preserved, every declared output/asset matching its packaged bytes, no unlisted additions. Native asset existence and headless rendering are useful evidence, not live visual acceptance.

## Candidate verification and publication

Build and Verify write logs, hashes and proof artifacts beneath `build/client-batches/<name>`. Proofs support `-Dcodex.proofOutput=<directory>`; old invocation defaults still work, while new runs avoid overwriting historical SVG/PNG/binary evidence. Full logs remain on disk; routine output is concise.

Verify compiles the selected proofs against the **candidate JAR**, then requires both zero exit and the specific success marker. It records source/tool/launcher hashes, reference server/cache dependencies and proof logs. This is local hashing, not sending cache contents to the model. It also records the original texture reference used by TextureProof. A failed Build/Verify does not publish a candidate. Rebuild/reverify after relevant inputs change.

Publish rechecks candidate, inputs, proof logs and reference file sets. It copies a uniquely named release JAR, preserves all old JARs, backs up both launcher copies, then changes only their JAR target. It never creates a new launcher name. The existing working directory, Java command, memory limit and cache selection remain intact. Repeat publication is blocked by an operation record. Caught failures restore and hash-check launchers; the previous JAR remains available. A process interruption or power loss still needs inspection/recovery from the record; this is not a crash-proof installer. No automatic rollback or server restart is performed.

This workflow is for the already-established developer-client installation. It does not seed or overwrite any cache, repack archives, change external/original clients, distribute a production client, or edit accounts/server runtime. Initial installation and cache migration require a separate explicit procedure. New client features needing server changes use the server batch workflow and its own relevant checks.

## Current acceptance and tooling evidence

The workflow was validated without publishing a new real client. The existing v7 launcher and JAR are unchanged. Full proof results and the isolated release fixture are recorded in docs/project/CLIENT_WORKFLOW.md. Live acceptance still follows the relevant feature reports. Use the handoff template for future subsystem pauses; link this inventory and latest batch receipt rather than copying release histories into AGENTS.md.
