# Developer client

Use `build/gambler-interface/dev-client/Run Dev Client.bat`. This remains the only user-facing launcher. Its current JAR target is authoritative; it still selects **Gambler-live-v7.jar** after the workflow-only update.

The client includes automatic Gambler UI capability, Infernal Cape support and the approved login artwork. Lobby/startup artwork work remains deferred. Reopen after a client release; server restart is required only for separately staged server changes.

## Development and release

Read `client/DEVELOPMENT.md` (paths relative to repository root). `client/gambler-interface/build.ps1` is now a safe wrapper around `tools/Invoke-ClientBatch.ps1`: **Prepare → Build → Verify → Publish**. Name the batch and select checks explicitly. Build/Verify create an isolated candidate and never switch the launcher or seed caches. Publish is separate and requires the validated candidate and unchanged dependencies.

`client/developer-client.json` lists all maintained patch sources, bundled assets and available proofs. Preserve every existing patch when adding a new feature. Full decompiled-client compilation and cache repacking remain unproven; this workflow uses selective compilation against the hash-verified original binary.

Read GAMBLER_FIXES.md, INFERNAL_CAPE_FIXES.md and LOGIN_BRANDING_FIXES.md for the relevant behavior and live acceptance limits. Latest custom UI uses automatic capability selection; the older enable-command/prototype instructions are historical. Existing layout approval does not certify every subsequent real-wager or display-mode change.

The previous README is preserved verbatim in `client/gambler-interface/HISTORY.md`. Historical build scripts/source snapshots remain under their original build backups; the immediately previous script is under build/client-workflow-before.