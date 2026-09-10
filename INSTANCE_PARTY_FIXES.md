# Instance parties, room expansion and respawn - Step 5B

Implemented 2026-09-08 following authorization to proceed with Step 5B. This supplies the shared APIs and an administrator acceptance harness. It does not enable or migrate Dungeoneering; that remains Step 5C.

## Current plan

- Step 5A: templates and private quest/skilling examples - implemented; see INSTANCE_CONTENT_FIXES.md.
- Step 5B: party rules, active room expansion and activity-specific respawn - implemented and regression-tested here.
- Step 5C: migrate the existing Dungeoneering content onto the managed APIs - next.
- Step 6: operational diagnostics/closure, configurable limits/expiry and sustained workload testing - pending.

Client visual/network acceptance and a normal restart remain required. Client editing remains deferred.

## Party rules

GameInstance.createParty selects an InstanceParty leader and departure policy during BUILDING. Once configured, direct GameInstance.enter calls are rejected unless invoked by that party's admission path. The configured leader must join first. Subsequent joins require an invitation bound to the invited Player identity, so a new login with the same account name cannot reuse an old invitation. Existing account, capacity, activity, familiar, online-state and spawn checks still apply.

Only the connected, living member who currently leads can invite, revoke, transfer leadership, kick or explicitly close the party. Invitations expire through instance-owned timers. Their duration is validated at 1..10,000 cycles; at most capacity-minus-one invitations can be outstanding. Invitations do not reserve membership slots: queued admissions still compete against actual capacity. Renewal cancels the earlier expiry, successful entry consumes the invitation, and a clean failed entry/capacity rejection leaves it usable until expiry. A partial entry that cannot evacuate closes/quarantines the session through existing cleanup.

The leader must still be connected and a member at admission time, including the interval before maintenance processes a disconnect. An invite cannot authorize entry under a disconnected leader.

Creation-time leader-departure policies:

- PROMOTE_OLDEST promotes the earliest-joined remaining connected member. If none qualifies, close the session.
- CLOSE_SESSION evacuates the remaining members and closes the session when the leader leaves.

Invitations are cleared when leadership changes or closure begins. A nonleader departure keeps the remaining party alive; the final departure closes it. Outstanding invitations never keep an empty started session alive. These rules run on direct leave, ordinary-world teleport, disconnect/logout, kick and explicit close. Internal respawn retains membership and leadership.

Party closure disables admission immediately. Failed evacuation keeps membership and the reservation tracked for retry. The content departure callback is captured before party policy runs so recursive closure cannot suppress it; each successful member departure delivers it once, even when the leader's departure closes other members.

Parties define admission and lifetime, not universal combat, loot or completion rules. Content still owns friendly-fire policy, reward eligibility and completion settlement. No durable roster, reconnect reservation or lobby matchmaking UI is introduced. Content must close a session it creates but never populates.

## Active room expansion

GameInstance.buildRoom operates only on ACTIVE sessions and delegates to RegionBuilder.appendMap using the retained allocation handle. Offsets and dimensions are chunks relative to the reserved footprint. The full destination rectangle and every destination plane must be unbuilt. Even an empty previously built chunk cannot be replaced through this API.

Validation covers footprint bounds, planes, ordinary source maps, ownership, source dependencies and residents/drops. Existing coarse resident protection applies across planes: a different destination plane is not permission to build beneath an occupied coordinate rectangle. The entire request is validated and source chunks prepared before publication. Invalid sources, load failures and partially overlapping multi-plane requests leave the existing scene revision, maps and resources unchanged.

Successful publication updates scene revisions. The normal PlayerUpdate path detects the change and sends rebuilds even for stationary members; no teleport is required. Existing NPCs, objects, drops and member positions in the old room survive expansion. Expanded chunks are released with their instance and do not survive allocation reuse.

This is append-only construction within the original reservation, not enlargement of the allocation or replacement of occupied rooms. The map copy is the atomic operation. Content should populate a successfully built room using owned resource APIs and close the session if subsequent content initialization fails; this API does not transactionally roll back arbitrary content callbacks. Rotation remains unsupported. Map data still snapshots loaded source state as documented in Step 5A; room connectivity, doors and visual seams belong to content integration.

## Activity-specific respawn

GameInstance.setRespawnPolicy supplies a location resolver and post-recovery callback during creation. It selects RESPAWN_INSIDE. Choosing the enum without supplying a resolver is rejected when creation activates. SAFE_RETURN remains the default; STANDARD_AT_EXIT still uses ordinary item-loss handling at the ordinary-world exit.

For RESPAWN_INSIDE, the existing five-cycle managed death interval is retained. At recovery, the destination must be a walkable built tile in the same active session. A successful teleport restores the existing managed safe-death state, retains inventory/membership and invokes the content callback once. Content can use that callback for a death counter or UI. The proof harness does so without changing the player's persisted Dungeoneering fields.

Both death and successful internal recovery advance the player's instance revision. Shared delayed damage, actions and area events from the previous life therefore cannot affect the next life. Death also stops current action/follow/teleport/area-event work. Each scheduled death event captures its death sequence; duplicate or obsolete events cannot resolve a later death. Arbitrary content callbacks still need their own per-life checks if they must expire across respawn while their instance remains active.

Disconnect, explicit exit, close and orderly shutdown during death evacuate and restore the player at their ordinary return location; they do not perform an internal respawn or its content callback. Saved coordinates remain the ordinary-world return position, including after internal recovery. Pending internal deaths project surviving HP into saves, matching the existing safe-return protection. No save-format change or durable checkpoint is added.

Invalid/blocked respawn positions, rejected respawn teleports, post-recovery callback failures or death-scheduling failures close the affected instance and use normal evacuation/retry cleanup. This deliberately fails the session rather than leaving a dead member stranded. A failing post-recovery callback is not replayed. Existing Wrath/Retribution calls and standard death item rules are retained; this batch does not rebalance combat or certify historical encounter death rules.

## Administrator acceptance harness

PartyInstanceExample provides a four-person administrator-only session, with an initial 64x64 map, reserved space for expansion and an internal respawn point. Commands require rights >= 2; invites target online administrators. The harness grants no items or XP and is not Dungeoneering gameplay.

Commands:

- `::instanceparty create` - defaults to promotion on leader departure.
- `::instanceparty create close` - closes when the leader departs. `create promote` is also accepted.
- `::instanceparty invite PLAYER NAME` - invitation valid for 100 world cycles.
- `::instanceparty join LEADER NAME` - accept that leader's invitation.
- `::instanceparty revoke PLAYER NAME`
- `::instanceparty transfer PLAYER NAME`
- `::instanceparty kick PLAYER NAME`
- `::instanceparty expand` - leader builds a second copy beside the first; repeated calls are harmless.
- `::instanceparty room` / `::instanceparty entry` - visit the second room or initial entry.
- `::instanceparty respawntest` - deliberately triggers the caller's death inside this test session.
- `::instanceparty status` - instance ID, current leader, member/invitation counts and the caller's deaths.
- `::instanceparty leave` / `::instanceparty close` - leave individually or leader-close the session.

Each member's ordinary starting location is their return position. Commands queue through the production manager when invoked outside its world-cycle mutation window. The example uses temporary player attributes and clears them on departure. It uses command-based room travel; it does not demonstrate connected dungeon doors or a public party interface.

## Verification

Java 8-targeted compilation and **all 15 regression suites pass**. Reproduce with `build/instance-parties/verify.ps1`.

InstancePartyRegression: **122 checks**, including:

- Direct-admission bypass prevention, leader-first admission, expired/revoked/renewed invitations, reconnect identity, failed entry, queued capacity contention, unauthorized leader actions and disconnected-leader admission.
- Leadership transfer, kick, disconnect promotion, ordinary-world teleport, final-member cleanup, close-on-leader departure, exactly-once recursive content callbacks, failed evacuation/retry and invitation scheduler rollback.
- Expansion bounds/ownership/source/plane validation, source-load rollback, atomic multi-plane rejection, cross-plane resident protection, source collision parity across all 4,096 new-room tiles, old resource survival and allocation reuse.
- Two stationary simulated players receiving actual opcode-31 rebuild messages through PlayerUpdate.sendUpdate, with their scene revisions recorded afterward.
- Internal respawn delay, duplicate death, damage revisions across lives, safe-save projection, repeated respawns, pending-death disconnect/close/shutdown and injected target/teleport/callback/scheduler failures.
- The administrator harness's shared membership, expansion authorization/idempotence, death counter, promotion and attribute cleanup.

Five diagnostic lines in the new suite are expected fault injections: two invalid respawn destinations, a rejected teleport, a failing post-respawn callback and a failed death scheduler. No unexpected exceptions remain.

Other suites: InstanceContentRegression 118; FightCavesInstanceRegression 414; InstanceIntegrationRegression 53; InstanceLifecycleRegression 73; InstanceFoundationRegression 18,776; Nex ice/movement; Barrows 711,204 and face timing; equipment absorption 238; combat enhancements 46 plus 10,000 shield trials; combat foundation 117; combat formula 90 plus 180,000 attacks; combat balance 48 plus 200,000 samples.

Tests read the real cache and use isolated schedulers and simulated connections. No server/client was launched, no player save files or cache files were written, and the process shutdown hook was not executed. Headless outgoing rebuild verification does not certify rendering or live networking. The separate client decoder suite was not rerun because packet encoding/decoding was not changed.

## Runtime and rollback

**21 runtime classes were staged to bin and SHA-256 verified. No restart was performed.**

- Build scripts, logs and source review: `build/instance-parties/`.
- Exact changed/new source families: `build/instance-parties/changed-sources.txt`.
- Existing-source/report backups: `build/instance-parties-before/`.
- Prior runtime files: `build/instance-parties-before/bin/`.
- Exact staged classes, previous existence, backups and hashes: `build/instance-parties/staged-classes.csv`.

Rollback only these listed families, restoring prior files where present and removing new files where the manifest records no prior counterpart. Preserve unrelated local work and previous instance/content/combat batches.

## Live acceptance and next work

After a normal restart, use two administrator accounts to test invitations, joining, visible map separation, expansion while one account stands still, room travel and internal respawn. Check the death counter, preserved inventory, membership/leader retention, invite expiry, leadership transfer, both leader-departure policies, kick/leave, disconnect/reconnect and orderly restart recovery. Reenter fresh rooms and confirm old map/object state is absent.

Next is Step 5C: replace Dungeoneering's legacy slots/maps and per-player activity assumptions with these party/session APIs, integrating doors, room progression, its internal death counter and content cleanup. Existing Dungeoneering entry points remain unchanged in this batch. Completing missing dungeon gameplay remains separate content work.

Godmode, personal XP rates, boosted Barrows rewards and the Summoning-specific deferral are preserved. Familiars remain excluded from managed admission.
