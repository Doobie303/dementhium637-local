# Status ledger

Update this file in place. Do not create siblings.

## Current focus

Step 2 paused at the owner's request after the Nex blood-phase timer/cancellation fix. Do not run the full regression selection, start further door/reward/boss work, or release this unfinished batch without renewed direction. Separate Starfall work needs a replacement statue sculpt and native preview.

## Live vs not live

- Live after restart: Latest repairs are not confirmed loaded or live accepted; no restart recorded. Earlier Barrows/Fight Caves acceptance retains its original scope.
- Staged only: NPC animation/overlap/arena pursuit step 1 and earlier combat repairs; Piscatoris home is staged with live acceptance pending.
- Tests only: Starfall asset/export workflow prototype and developer-client candidate proofs; no publication or native rendering acceptance.
- Working tree only: Step 2 includes Nex changes plus earlier God Wars door/room and Corp attack/core edits; none was staged or restarted in this turn. Starfall home migration, cache/map writing and service integration remain unstarted. Final release/live closure is pending.

## Recent accepted work

- Shared death/reward, reset/status/reflection and movement/timing repairs implemented, verified and staged.
- Step 1 protects NPC attack animations, separates overlapping followers and repairs God Wars/Nex arena pursuit; automated checks passed, live presentation pending.
- Starfall unified asset build/import prototype works; arch composition approved as a baseline, further arch/eagle improvement allowed, statue likeness rejected.
- Documentation control installed: existing AGENTS.md rules retained; index points here; this is the sole new file. No gameplay changes.

### Piscatoris home batch (2026-09-09)

- Restored arrival/default, tutorial returns and shared home/death/return paths to (2344,3691,0); no Wilderness or other destination changed.
- Removed HomeHub services at (2336,3802), (2340,3802), (2341,3805), (2343,3806), (2343,3808), (2337,3810), (2339,3810), (2331,3810), (2330,3806), (2344,3802); removed objects at (2331,3803), (2331,3805), (2331,3807), (2344,3804).
- Final services: guide (2343,3691); melee/range/magic/supplies (2330,3686-3689); rewards (2333,3691); Slayer/skilling/Gambler/tools (2332/2335/2338/2341,3697).
- Final objects: PvM/PvP/Activities (2332/2334/2336,3684); preparation altar reuses 47120 at (2335,3682); prayer restore remains (2340,3687).
- Existing bank booths remain untouched at (2328,3686-3692); the nearby existing rewards trader is reused. Existing Slayer (2329,3668) failed the arrival path check, so the reachable stand shifted to (2332,3697).
- HomeHubRegression alone passed 345 checks in 2.181s; coverage includes real clipping/pathing, shops, portals, altar, bank routing, stale-menu boundaries, Gambler conservation, tutorial/default and return objects. No full test folder or live walk was run.
- Batch `piscatoris-home` staged 11 classes across HomeHub, Mob and TutorialScene with verified backups/hashes. A server restart and live walk remain required.
- Nex blood rotation reproduced a pending-special stall: siphon's completed eight-tick action retained its named timer for another 50 ticks. The timer now stops at completion; phase/life cancellation also clears owned special state.
- One diagnostic suite passed before the stop: `BossEncounterCompletionRegression` (1.853s tests; 41.696s total including compile/candidate/hash work), under `build/batches/boss-encounter-step2-complete-20260909/debug-7314eeee432046e2b2360dfface8988a`. It exercised natural Nex phases/death/fixture loot/respawn, four native entrance crossings and core lifecycle; it is not a release Verify or live acceptance.
- Preserve all four `tools/batches/boss-encounter-step2*.json` preparations/backups. Earlier preparations retain original source/absent-file evidence; later ones include intermediate states. No full regression selection or Stage was run.

## Open defects

- God Wars doorway restoration/clipping/reentry has working edits and diagnostic coverage; broader affected checks and live attack/follower presentation remain pending.
- Corp graphic 1826 resolves to core model 42314, confirming the apparent extra-core visual cause. Energy-effect, HP gate and hop/ownership edits remain unreleased; ground-projectile timing still needs correction because the location overload derives duration from its speed argument. Existing BossStepFour helper expectations still need updating for the HP gate and two-tick hop.
- Nex natural progression now completes in the isolated diagnostic; broader cancellation/affected-caller checks and live timing remain pending. Once a verified fix is loaded, test repeated siphon/sacrifice cycles with godmode off, immediate return to normal attacks, and blood-to-ice transition without lingering siphon/healing/animation locks.
- Wider combat register remains open: historical/formula calibration, reach/equipment/resources, missing spell coverage, remaining specials and delayed callbacks. Elysian over-reduction remains unreproduced; Staff of Light ruled out.
- Starfall statue likeness is unaccepted; native rendering/performance/collision and cache/map pipeline remain unproven.

## Do not reopen unless evidence changed

- Completed audits and implemented AoE, Barrows/Berserker, BGS and boss repairs are historical evidence, not fresh defect lists. Recheck current code before reopening.
- Preserve approved custom rules, rewards, access conveniences, durable recovery state and developer-client visuals/launcher; early-2011 fidelity remains provisional.
- Summoning-specific work, Dungeoneering overhaul and lobby/startup branding remain deferred; quest implementation and solo minigame proposals remain review-only.
