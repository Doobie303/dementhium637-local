# Quest system and Quest Helper review

Reviewed 2026-09-09. Status: review and proposed implementation plan only.

## Conclusion

The active server has a quest skeleton, not a working reusable quest framework. No registered, persistent playable quest was found in the inspected source and dialogue data. A solid base is feasible using existing interactions, dialogue, account saving, managed instances and the developer client's custom widgets. Build the quest engine and its helper metadata together, then prove them with one complete small quest before adding a catalogue.

The RuneLite Quest Helper is a useful feature reference, not a drop-in plugin for this client. Its official repository describes quest selection, requirements, step instructions, map/minimap arrows and NPC/object/tile highlights: https://github.com/Zoinkwiz/quest-helper . Our implementation should use our own server-authoritative quest state and developer-client presentation. OSRS quest scripts and coordinates cannot be assumed to match our revision or custom world.

## Current evidence

Paths below are relative to the workspace and refer to the active `src` tree.

| Area | Finding | Evidence |
|---|---|---|
| Quest catalogue | Empty map; `init()` only returns true. Ten slots are declared, but that does not mean ten quests exist. No initialization caller or registration path was found. | `src/org/dementhium/content/quests/QuestRepository.java:16` and `:28` |
| Quest definition | Only ID, name and a configurable finish threshold, default 15. No objectives, requirements, rewards, journal or event processing. | `src/org/dementhium/content/quests/Quest.java:8` |
| Player progress | Ten-element integer array with unchecked indexing and unrestricted stage assignment. Completion is a numeric threshold check. It cannot safely express branching objectives or versioned content. | `src/org/dementhium/model/player/QuestStorage.java:16` |
| Persistence | Player constructs QuestStorage, but `load` and `save` do not serialize it. No alternate QuestStorage serializer was found. Progress written to this object would be lost when the player object is recreated. | `src/org/dementhium/model/player/Player.java:126`, `:726`, `:958` |
| Quest display | Login sends fixed values of 100 for configs annotated as completed/total quests, then many hard-coded completion flags. These are not derived from QuestStorage. Interface 190 is mounted in fixed and resizable layouts. | `src/org/dementhium/net/ActionSender.java:629`, `:675`; `src/org/dementhium/util/InterfaceSettings.java:118`, `:159` |
| Dialogue integration | XML dialogue supports a QUEST_STATE equality requirement, but QUEST_OPTION is only an enum entry: its factory has no implementing case and returns null. No quest requirements/actions were found in the supplied dialogue XML. | `src/org/dementhium/content/dialogue/Requirement.java:63`; `src/org/dementhium/content/dialogue/OptionAction.java:19` |
| Existing story material | Double Intruders is mostly a comment describing a possible story and rewards. The NPC branch sends a line and a message; this is not a completed quest. | `src/org/dementhium/content/DialogueManager.java:3175` |
| Instance proof | Admin-only crate/clue/chest scene has guarded per-session steps and one reward per live run. Its state is deliberately ephemeral; it is not registered in QuestRepository and is not saved. | `src/org/dementhium/content/instance/InstanceExamples.java`; `INSTANCE_CONTENT_FIXES.md` |
| Guidance | Existing NPC/player and coordinate hint icons, with packet output, are usable starting points. No integrated quest tracking/helper was found. | `src/org/dementhium/model/misc/IconManager.java:81`, `:139`; `src/org/dementhium/net/ActionSender.java:179` |
| Developer client | Existing build selectively patches widgets and packages v7. Custom Gambler widgets and capability-based UI selection establish a practical interface route; they do not prove arbitrary overlays. | `client/gambler-interface/build.ps1`; `client/gambler-interface/README.md`; `src/org/dementhium/net/GameSession.java` |

## Reusable components and integration risks

1. **Dialogue and interactions exist, but need an explicit quest dispatch layer.** XML and legacy Java dialogue paths coexist. NPC first-option routing tries XML dialogue before the legacy handler. Quest handling needs defined priority, current-stage validation and a consumed/not-consumed result. The existing EventManager has one object listener per object ID and runs all registered interface listeners; it is not already a general quest event bus. Match NPC/object identity, location and player context so common IDs do not hijack ordinary content.
2. **Saving infrastructure is usable, quest reward transactions are absent.** PlayerLoader already uses atomic account-file replacement and recovery for Duel/Gambler transactions. Add a bounded, versioned quest section without changing existing trailer ordering or discarding item-charge/Barrows/DUL2/GAM1 data. Do not reuse gambling or duel settlement semantics for quest rewards. Commit completion, consumed items, XP, unlocks and reward claims consistently in the account image, with rollback on write failure and durable pending items for full inventories. Test crash/retry boundaries before claiming exactly-once rewards.
3. **Legacy quest flags are a migration concern.** Some configs may affect client unlocks as well as journal colors. Trace affected consumers before replacing them. Preserve current spellbooks, equipment access and private-server conveniences by default; do not suddenly impose historical quest gates. Do not interpret the current hard-coded flags as proof that accounts earned quest completions or rewards. Track newly implemented quests separately from legacy access policy.
4. **Hint markers need ownership before helper reuse.** `freeIconSlot` returns the first empty slot before completing its search for an existing target, returns slot zero when full, compares coordinate objects by reference, and does not distinguish NPC/player target type when matching an index. Removal uses that same lookup. This can duplicate, overwrite or miss markers. Add owner handles, find-before-allocate behavior, explicit exhaustion and exact release; preserve PvP, duel, Barrows, graves and Castle Wars markers. Recheck those systems' saved review instructions before making shared changes.
5. **Managed instances are an optional quest tool.** Persist narrative checkpoints and named local anchors, never allocated world coordinates or live NPC indices. Recreate an instance from a safe checkpoint after reconnect; an old allocation is not durable quest state. Use tracked resources/tasks and current lifecycle rules. Existing load tests do not certify a future quest's live scene.
6. **Client scope must remain bounded.** The developer client has successful selective builds; the full original decompilation still does not build. Custom panels are credible. Scene outlines, dialogue highlighting, world-map markers and path overlays require tracing and testing new client hooks, including software/OpenGL and fixed/resizable modes. Continue using the single `Run Dev Client.bat` launcher. Preserve cape, Gambler and approved branding; deferred lobby/loading artwork remains deferred.

## Recommended quest foundation

Use one Java module per quest with declarative metadata and explicit handlers for unusual behavior. Avoid a new general scripting language initially.

- **QuestDefinition:** stable ID/key, content version, title, summary, start location, requirements, rewards, quest points and steps. Validate duplicate IDs, missing steps, invalid transitions and prerequisite cycles at startup. Definition order must not determine saved IDs.
- **QuestProgress:** explicit not-started/in-progress/completed state, stable checkpoint ID, bounded objective counters/flags, revision and durable reward claims. Support content-version migration and deliberate handling of unknown or corrupt records. Invalid state must not silently reset a completed quest and re-enable rewards.
- **QuestService:** authoritative start/transition/completion methods on the game thread. Revalidate requirements and interaction context, handle duplicate events idempotently and reject stale callbacks. Helper buttons select/pause guidance; they cannot advance a quest or grant rewards.
- **Typed objectives/events:** talk to an NPC, choose dialogue, use an item, interact with an object, collect/hand in items, enter a region and receive eligible kill credit. Inventory possession, lifetime acquisition and hand-in consumption are different objectives. Define boosted/base skill requirements and inventory/equipment/bank checks explicitly.
- **Quest journal:** actual implemented catalogue, requirements, status, rewards and progress-sensitive journal text; searchable/filterable as the catalogue grows. Separate quest points from completed-quest count.
- **Helper definition:** guidance attached to the same steps/objectives, including current instruction, carried-item checklist, target selectors, optional dialogue hints and spoiler detail. Recompute from server state on login and relevant changes. A quest remains playable with the helper disabled.
- **Authoring support:** a small example module, registration guide and test fixture. Adding an ordinary quest should not require another series of branches across global packet handlers.

## Quest Helper delivery

| Feature | Proposed delivery |
|---|---|
| Choose/track/pause a quest | First helper release; one active tracked quest, any number of quests in progress |
| Requirements and required/recommended items | First release; distinguish carried items from banked items and consumable quantities |
| Current instruction and objective checklist | First release; update on progress and relevant inventory/state changes |
| NPC/location arrows | First release after marker ownership and packet/client validation; resolve the player's current world/instance target |
| Journal and completion summary | First release; native-compatible fallback plus enhanced developer-client widgets |
| Persistent compact helper panel | Developer-client first-release target; prove it leaves inventory, dialogue and normal interaction usable |
| NPC/object outlines and tile highlights | Later rendering phase; hooks not established by this review |
| Highlight matching dialogue choices | Later phase; bound to the currently owned conversation and step |
| World-map destinations and route guidance | Later phase; a destination arrow is not a traversable route. Floors, doors, transports and instances need explicit treatment |
| Puzzle solutions and branch-specific hints | Per-quest additions after the base is proven; optional spoiler controls |

Use a versioned cosmetic capability for enhanced UI, following the existing client-selection pattern. Unsupported clients must receive only supported interfaces/packets and still be able to complete the same quests. A client capability grants no gameplay authority. Send current helper snapshots after login, tracking changes and region transitions; discard stale revisions and clear guidance on pause/completion/departure. Prefer relevant event updates to scanning every quest for every player each tick.

## Proposed implementation order and acceptance

1. **Core and persistence:** implement definitions, validated state transitions, safe account serialization and reward claims. Prove old-account loading, new round trips, invalid/truncated data, migration, duplicate completion, save failure and restart recovery. Preserve existing account trailers and XP policy.
2. **One complete small quest:** dialogue, several objectives, item hand-in, journal, completion and rewards. An original short quest is a good initial proof; exact story/rewards remain to be selected. Include reconnect at every checkpoint, full inventory, item loss/reacquisition, repeated clicks, stale dialogue, multiple simultaneous players and ordinary-content coexistence. The admin instance proof can supply a later private scene, but is not itself this deliverable.
3. **Helper v1 with that quest:** catalogue/tracking, panel, requirements, checklist and owned arrows. Verify real packet/button paths, capability fallback, fixed/resizable layouts, map/floor transitions and coexistence with existing markers. Retest developer-client cape/Gambler/login features. Live visual acceptance is required.
4. **Prove a second quest and publish the authoring template:** exercise a branch or eligible kill objective and optional private checkpointed scene. This demonstrates reuse before scaling content.
5. **Advanced helper visuals:** investigate and implement outlines, dialogue highlighting and map guidance in separately reviewable client batches.

Recommended initial scope is the foundation plus one complete quest and Helper v1. Do not promise the entire RuneLite feature set as part of that first batch. No time estimate is certified before the client panel and save migration spikes.

## Review limits

This was static source/data inspection plus the linked primary feature reference. No server/client was launched, regression suite executed, account opened for modification, runtime staged or gameplay source changed. Prior regression counts in existing reports are historical evidence, not tests rerun here. The only new artifact is this review and its AGENTS.md pointer. Exact client quest-config semantics, available custom interface slots, overlay hooks and live usability remain implementation checks. Preserve Summoning and Dungeoneering deferrals, godmode, personal XP rates and custom gameplay policy.
