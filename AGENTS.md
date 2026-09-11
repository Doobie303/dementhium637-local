# Project instructions

## Normal work

- Work in `src`; loose root `combat/` is not the configured source root. Preserve unrelated local changes.
- Make the smallest correct change for the request. Do not add unrelated cleanup, refactors, reports, or audits.
- For normal Java work: inspect the relevant code, edit source, compile affected code into the normal local `bin` output when practical, run the closest useful check when warranted, review `git diff --check`, `git status --short`, and the expected changed files, then stop. This is ordinary local development, not guarded release staging; use the repository's normal compilation inputs or another lightweight non-release method if needed, and report a genuine blocker rather than falling back to batch/release tooling. Do not run a full build or broad suite for a small isolated edit without a concrete reason.
- Prefer existing focused checks. Expand validation only for an affected shared/runtime contract, a concrete caller, a failed check, or unresolved risk. Add regression coverage only when it protects a meaningful reusable or subtle behavior.
- For movement, timing, sessions, lifecycle, packets, persistence, or comparable runtime contracts, use the relevant production-path guidance in `tests/TESTING.md`.

## Context and authorization

- Read the relevant `docs/project/CONTEXT_INDEX.md` row only when the requested subsystem has a durable constraint, prior decision, or uncertainty that matters to the change. Historical reports are evidence, not a default reading list.
- Updating local `bin` by compiling affected Java source is ordinary development and normally makes a change locally test-ready. Phrases such as “so I can test it in game” authorize that normal local compilation, but do not authorize release manifests, guarded staging, deployment, publication, or restart. Leave a stopped server stopped; if it is running, the compiled output is for the owner's later restart. Restart always requires explicit owner intent.
- For explicitly requested server staging, deployment, recovery, or restart work, read `tools/README.md`; only those release operations use `tools/batches` manifests, `build/batches` receipts, or `Invoke-ChangeBatch.ps1`. For developer-client source/assets, Build, Verify, client-specific validation, or publication work, read `client/DEVELOPMENT.md`; server-only work does not need it.

## Documentation control

- Do not create new Markdown files unless the owner explicitly says “Create a file named ...” or asks to keep useful notes. New ordinary notes belong only under `docs/project/notes/`; reuse/update relevant notes when practical, consolidate overlaps, and do not scatter task-specific reports, summaries, or handoffs.
- Remove obsolete notes with no continuing value; archive useful superseded history only under `docs/project/notes/archive/`, not automatically. Routine fixes do not require a note.
- `docs/project/STATUS.md` is concise durable project-wide state, not scratch notes; update a context-index row only when its routing or durable constraints change.
- Reviews are read-only unless the owner asks for a patch. Return findings in chat; record only accepted durable conclusions in `STATUS.md`.
