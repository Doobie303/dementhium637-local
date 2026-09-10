# Instance integration - step 3

Implemented 2026-09-08 after the owner's instruction to begin step 3. Read INSTANCE_FOUNDATION_FIXES.md and INSTANCE_LIFECYCLE_FIXES.md for the earlier map and lifecycle layers. Those documents remain historical records of their respective batches.

## Completed hooks

### Session transitions and cleanup

Login/logout tasks now queue through the instance request queue and execute in World.run, instead of mutating instance state on the background session executor. Direct World.register/unregister calls route through that same path when called off-cycle. Repeated logout work checks player identity before performing removal/saving. The existing account-authentication and lobby logic is retained.

World.run is synchronized and opens the existing instance mutation window. It also checks for disconnected, offline or removed members and retries CLOSING instances. A disconnected member is moved to its ordinary return position without relying on a successful network teleport. Cleanup happens before the normal logout save.

The shared location setter and teleport preflight enforce managed membership. Outsiders cannot enter a managed map or its guard area; members cannot teleport into another instance or an unbuilt chunk. Managed NPCs cannot leave their map. Normal exits detach the player, cancel follow/teleport/action work, close interfaces/trades, and close an instance when its last member leaves through these hooks. Other members retain their session.

GameInstance's explicit leave API remains usable by content that controls its own lifetime; use InstanceAccess.depart for a departure that also closes an empty session. The internal transfer flag prevents recursive leave/close during admission, evacuation and failed-entry rollback. Content must still explicitly close an abandoned instance that was created but never admitted a member.

WalkingQueue validates both walking and running destinations. If only the second running step is rejected, the valid first walking step is still sent to the client.

### Death

Managed sessions have an explicit creation-time death policy:

- SAFE_RETURN is the default. It keeps items, restores the player and returns them to the configured ordinary-world exit.
- STANDARD_AT_EXIT invokes the existing gravestone/item-loss calculation at that exit, so recoverable losses are not placed in a map that cleanup immediately deletes. Configure this with setDeathPolicy(GameInstance.DeathPolicy.STANDARD_AT_EXIT) inside the instance initializer.

These are session policies, not a claim that every RuneScape activity used the same death rules. No existing activity has been migrated to either policy in this batch.

Managed death keeps the existing five-cycle recovery interval, with Wrath/Retribution work scheduled within the owning session. Pending death blocks ordinary teleport escape. Disconnect, explicit close and shutdown resolve a pending death before releasing membership and saving. Owned delayed death work is cancelled when the session closes, preventing a second resolution. Godmode and the ordinary non-instance death branches are preserved.

### Login recovery and save compatibility

Player.save writes a member's ordinary-world return location without moving the live player. This uses the existing position fields and does not change the binary save layout. Autosaving a pending SAFE_RETURN death writes surviving life points, so reconnect cannot turn a safe instance death into an ordinary item-loss death. Standard deaths retain the existing dead-state recovery behavior if saved before resolution.

Player.load validates raw coordinates/plane before Location's plane normalization. A saved position inside a currently managed map, or in blank cache space eligible for dynamic allocation, returns to Mob.DEFAULT. The blank-cache check also works after restart, when there is no instance registry, and prevents entry into another run that reused old coordinates. World.register checks recovery again before sending login/map state.

Ordinary cached-world saves retain their coordinates. Live legacy dynamic maps remain separate from managed-session membership; Dungeoneering still needs its own migration. No durable quest checkpoint or instance resumption is introduced here.

### Interaction and delayed work

InstanceAccess provides shared membership/boundary policy used by:

- NPC/player interaction and cast/item-on-entity handlers.
- Object/item-on-object handlers, including delayed object actions and object-identity checks.
- Ground-item lookup and pickup/take entry points.
- Following and delayed trade/duel requests.
- Combat targeting, active attack callbacks and shared damage impact.

Objects from a replaced/removed managed tile are rejected by identity. Cross-instance requests cannot reach another session just by supplying its NPC/player index or coordinates.

Membership changes increment a mob revision. Area callbacks, ordinary combat-action callbacks and generated Damage records capture revisions and reject stale work after a player leaves/re-enters, even when returning to the same session. Damage from an instance NPC is rejected after that NPC/session is removed. Ordinary-world damage and the existing custom combat settings remain unchanged.

### Shutdown

DementhiumShutdownHook takes the same monitor as World.run, opens a mutation window, closes instances and then performs the existing saves. New instance creation/requests are rejected once shutdown begins, queued futures complete exceptionally, and later world cycles do not resume gameplay. Cleanup failure retains diagnostics; save-position projection still supplies ordinary-world exits for retained members.

The shutdown hook is for orderly termination. A forced process kill cannot execute it; the safe coordinates in the most recent completed save provide recovery in that case. This does not replace the server's existing file-save durability mechanism.

## Verification and runtime staging

Java 8-targeted compilation passed. Twelve suites passed:

- InstanceIntegrationRegression: 53 checks covering real walking/running boundaries, forged NPC/trade requests, pickup isolation, in-memory old/new saves, stale map reuse, same-instance reentry/projectiles, safe/standard deaths, disconnect and pending-death cleanup, shutdown and session-request dispatch.
- InstanceLifecycleRegression: 73 checks. The old foreign-occupant fixture now first verifies placement is denied, then deliberately injects corrupt spatial state to retain defensive cleanup coverage.
- InstanceFoundationRegression: 18,776 checks.
- NexIceRegression and NexMovementRegression.
- BarrowsRegression: 711,204 checks; BarrowsFaceRegression.
- EquipmentAbsorptionRegression: 238 checks.
- CombatEnhancementsRegression: 46 checks plus 10,000 Elysian trials.
- CombatFoundationRegression: 117 checks.
- CombatFormulaRegression: 90 checks and 180,000 generated attacks.
- CombatBalanceRegression: 48 checks and 200,000 samples.

Reproduction: build/instance-integration/verify.ps1.
Compiler/test logs, changed-source list, and a comparison against this batch's source backups: build/instance-integration/.
Source and prior runtime backups: build/instance-integration-before/.
Exact runtime files and SHA-256 hashes: build/instance-integration/staged-classes.csv.

Tests use mocked connections and in-memory player save buffers. They do not invoke real account saving or the process shutdown hook, start a server, or edit existing player saves. The shutdown controller and source integration are tested/reviewed; an actual client disconnect/reconnect and orderly server restart remain live acceptance checks. Runtime staging does not restart the running server.

## Remaining work and acceptance

Step 4 remains migration of the existing shared/static Fight Caves implementation to independent FightCavesSession runs. No existing minigame, quest, skill or Dungeoneering entry point has been switched to the new manager in step 3.

Arbitrary content callbacks must still use the instance-owned scheduler/resource APIs. A source-less custom damage call or a bespoke timer created directly on the world scheduler cannot automatically reveal which old session it belongs to. Shared hooks cover the paths listed above; each content migration must audit its own timers, rewards and special state. Familiar admission remains disabled under the owner's Summoning deferral.

After restart, use a controlled content harness to check:
1. Two simultaneous copies: reject forged cross-copy interaction, movement and teleport requests.
2. Teleport one party member out, then the last member; verify scene refresh and cleanup.
3. Safe death and explicitly configured standard death; verify exit, items, gravestone and other members.
4. Disconnect during life/death, reconnect after autosave, and load a pre-integration save containing an old dynamic position.
5. Orderly shutdown with active sessions, restart, and confirm every affected player is in the ordinary world.
6. Repeat ordinary Fight Caves, Barrows and combat checks to confirm existing content remains intact.

Godmode, personal XP rates, boosted Barrows loot and the Summoning-specific deferral are preserved.

Final staging: 83 runtime classes copied and SHA-256 verified, with zero mismatches. Prior classes are backed up. A normal server restart is required; no restart was performed.
