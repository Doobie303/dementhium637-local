# AI players: source review and implementation plan

Reviewed 2026-09-09. Planning only: no gameplay source, runtime classes, client, settings or account saves changed. No server restart or live test performed. The owner wants a world populated by believable players who converse, train, trade, PvP and fight bosses, improve with experience, and can be switched off when unwanted or when friends join.

## Recommendation

Build persistent AI residents using ordinary Player entities with explicit server-controlled input and lifecycle. Start with one reliable resident, then a small population concentrated in useful hubs. Separate persistent residents from resettable test actors. Use deterministic action execution, utility-based goals and bounded statistical learning first; add optional language-model conversation and later isolated combat training. Neither conversation memory nor ordinary skill XP is neural-network training.

This is a substantial content and engine feature, not a generic AI library that immediately understands this server. Every activity needs executable actions, observations, failure handling and acceptance coverage. A busy hub is achievable earlier than autonomous coverage of the whole world.

## Current-source findings

These are architectural observations from the current active src tree, not a full bot-readiness audit or a claim that historical combat findings remain unfixed.

| Finding | Evidence | Consequence |
|---|---|---|
| Player is final and takes GameSession and PlayerDefinition. | src/org/dementhium/model/player/Player.java:89,250 | Prefer composition through a controller; a BotPlayer subclass is not currently possible. |
| A session with no channel is disconnected. World admission and ticking require a connected session. | src/org/dementhium/net/GameSession.java; src/org/dementhium/model/World.java:196,291 | A null socket plus setOnline(true) is insufficient. Add explicit actor liveness without weakening human connection/authentication checks. |
| Normal ticks begin by processing network packets; updates build player/NPC packets for each viewer. | src/org/dementhium/task/impl/PlayerTickTask.java; PlayerUpdateTask.java | Separate input processing from simulation. Bots still appear in human viewers' player updates, but do not need a fabricated client receiving their own updates. Retain required mask/reset lifecycle. |
| Login initialization, packet handling and profile loading contain direct channel/IP/pipeline access. | Player.loadPlayer/getHandler; src/org/dementhium/io/PlayerLoader.java | Extract shared gameplay initialization and isolate transport-specific work. Review all session consumers, including friends/clans/target matching. |
| Registration checks duplicate identities and current save versions; departure coordinates instances, activities, duels, trades and saves. | World.register/unregister; src/org/dementhium/task/impl/SessionLogoutTask.java | Preserve these invariants in the controlled-player lifecycle. Never register by directly inserting into the player list. |
| Movement/pathfinding and normal combat already operate on Mob/Player. Object actions are substantially embedded in packet handlers. | model/map/path/PathFinder.java; net/packethandlers/WalkingHandler.java; ObjectPacketHandler.java | Reuse gameplay services; extract validated action entry points incrementally where packet handlers own them. Do not emulate arbitrary client packets or directly award XP/items. |
| Headless regression fixtures already construct players with null or simulated channels. | tests/CombatFoundationRegression.java; InstanceLifecycleRegression.java; DuelRegression.java | Useful test scaffolding, but these fixtures do not demonstrate a production socket-free player lifecycle. |
| ServerThread submits a cycle nominally every 600 ms and replaces its executor when work queues. PlayerTickTask suppresses exceptions in its simulation block. | src/org/dementhium/ServerThread.java; PlayerTickTask.java | Make tick diagnostics and scheduler-overrun investigation early gates. Do not put model calls/training on the world thread or claim a population capacity from existing fixture timings. |

Read alongside COMBAT_2011_REVIEW.md, COMBAT_FOUNDATION_FIXES.md, COMBAT_OVERNIGHT_FIXES.md, COMBAT_MORNING_DECISIONS.md, INSTANCE_PLAN_STATUS.md and INSTANCE_OPERATIONS_FIXES.md. PvP and duel integration must consult both their original review and current fix reports before changing those systems. Current fixes supersede only their stated historical findings. Preserve custom PvP/EP, Barrows boosts, godmode/personal XP, developer-client work, and the Summoning/Dungeoneering deferrals. Bots initially have no familiars and do not migrate Dungeoneering.

## Intelligence and improvement

1. **Action executor:** walk, follow, attack, eat, equip, pray, bank and use a supported object through the same authoritative validations as humans. React to success/failure, cooldowns and resource changes. Limit reaction speed, simultaneous actions and knowledge to the configured skill profile.
2. **Goal planner:** choose between gathering supplies, training, banking, socializing, trading and joining an activity. Score goals by personality, needs, available resources, commitments and recent outcomes. Stick with goals long enough to avoid erratic switching.
3. **Persistent memory:** remember discovered routes, failed approaches, preferred activities, completed boss attempts, relationships and selected conversation summaries. Facts carry origin/time and expire or are invalidated when mechanics change. Store IDs rather than live Player references. Different residents retain different memories.
4. **Bounded learning:** collect outcomes for authored alternatives and adjust their selection within tested limits. For example, compare food thresholds by survival and food cost, or training locations by XP, travel cost and deaths. This is real statistical adaptation over a defined action set; it cannot invent unsupported skills or mechanics. Begin with simple contextual bandit/score updates rather than a neural network.
5. **Optional language-model service:** generate conversation and propose validated high-level goals using a small observed-state summary. Gameplay remains functional when it is slow, disabled or unavailable. An external service runs asynchronously; decisions return with actor/session/revision/expiry identifiers and are revalidated on the world thread. Stale results cannot revive an offlined bot or execute an old trade.
6. **Later learned combat policy:** train against varied scripted and frozen opponents in isolated test worlds using recorded observations/actions/outcomes. Compare candidate versions against held-out encounters before promotion. Freeze and roll back policies independently of character saves. Broad online neural learning in the live economy is outside the initial scope.

Learning needs measurable objectives: survival, successful objectives, resource efficiency, appropriate cooperation and bounded reaction times. Win rate alone can reward stalling, exploiting implementation bugs or farming weaker opponents. Compare same gear/stats and multiple opponents/seeds; report success and illegal-action rejection rates, encounter duration and supply use. Learning should remain capped by personality/difficulty: a novice can improve without eventually becoming a perfect prayer-switching machine. Difficulty changes should be visible/admin-controlled, not secretly counter the owner on every fight.

Observation restrictions matter: own inventory/stats, legitimately visible equipment/animations/chat and known terrain are reasonable. Other players' private inventories, queued actions, RNG rolls, admin secrets and remote hidden events are not. Boss knowledge can be an explicit novice/experienced profile; omniscient test actors must be clearly separate from normal residents.

Research basis: [Generative Agents](https://arxiv.org/abs/2304.03442) supports memory, reflection and planning as useful ingredients for believable social agents, not proof of reliable RuneScape gameplay. [DeepMind's AlphaStar report](https://deepmind.google/blog/alphastar-grandmaster-level-in-starcraft-ii-using-multi-agent-reinforcement-learning/) demonstrates imitation/self-play/league training for game skill; its results do not establish a practical compute budget or transfer to this server. The staged design here is an engineering recommendation, not a promise of comparable training results.

## Making the world feel inhabited

- Persistent names, appearance, gear progression, budgets, skill interests, sociability and risk preferences. AI identity is discoverable and human/AI population counts are separate; do not impersonate real friends.
- A population director allocates residents among supported banks, training areas, gathering spots and PvP/boss staging areas. Begin with a few populated hubs and travel routes, then expand the activity catalogue.
- Shared schedules, invitations and supply needs produce causal behavior: gather logs, bank, arrange a sale, buy supplies and join a trip. Trades need an actual reason, not random item exchanges for decoration.
- Residents have pauses and varied session lengths. They do not all respond to every chat message or synchronize their actions. A companion can remember a successful trip and invite the owner again.
- Active residents use real simulation even off-screen while engaged in combat, resource consumption or transactions. Dormant residents may plan cheaply but do not invisibly generate XP, loot or trades. Reactivate at saved safe locations, not by teleporting into the owner's view.
- Initially propose 6-12 active residents across a small supported area after the single-bot gate. Later measure 10/25/50 populations before selecting limits. These are rollout/load-test sizes, not verified capacity estimates or a promise that a huge map will feel full with a dozen residents.

## Population controls and safe shutdown

Proposed admin commands (not implemented):

| Command | Behavior |
|---|---|
| ::bots on | Enable the configured resident population and stagger admission. |
| ::bots off | Persist desired population zero, reject new admissions/goals, drain active work and save/depart all residents. Display pending cleanup until it is finished. |
| ::bots count 12 | Set the desired active population within measured/configured limits. |
| ::bots mode auto | Adjust toward a configured population target as human players join/leave, using debounce and reserved human capacity. |
| ::bots mode friends | Request zero ambient residents; explicitly retained companions are optional and visible in status. |
| ::bots status | Show human/resident/test counts, mode, activities, pending shutdown, tick/AI latency, learning/model version and errors. |
| ::bots learning off | Freeze adaptation while retaining learned behavior and memory. |
| ::bots chat off | Disable generated conversation while gameplay continues. |
| ::bots inspect <name> | Show current goal, permitted observations, recent decisions and blocked-action reason. |

Default first release: disabled on startup unless deliberately enabled through durable configuration. Persist operator intent separately from transient active count; an explicit off must survive restart and override automatic backfill. Auto mode counts connected humans, not lobby sessions/bots, with optional configured non-owner-human threshold for switching to friends mode. Use hysteresis to avoid churn on reconnects. Human logins take priority over bot admissions; pinned companions never override a hard off.

Lifecycle proposal: OFFLINE -> STARTING -> ACTIVE -> DRAINING -> SAVING -> OFFLINE, with retryable cleanup failure visible to admins. Off immediately stops new work, but does not erase actors mid-transaction. Cancel uncommitted trade offers and refund through owned containers; recover committed transfers exactly once. Finish or settle duels through their departure contract, clear PvP targets, cancel actor-owned tasks and model requests, depart managed instances, save safe positions and unbind references. Honor combat consequences for ordinary draining so toggling cannot create free escapes or loot duplication. Bound draining time and define an explicit administrative interruption policy before supporting dangerous PvP/boss shutdown. A failed save or settlement stays quarantined/retryable, not falsely reported as off.

Emergency disable stops AI planning/admission and invalidates queued decisions on the next world cycle; minimal engine cleanup continues. It is not a blind delete-player-list operation. Off must not close an instance still needed by a human; content ownership/departure policy decides what can close.

## Identity, economy and persistence

- Reserve AI identities in the authoritative identity registry and reject network login to them. No shared bot password or invented IP to bypass authentication. Keep permissions ordinary; AI cannot execute admin commands.
- Separate resident records and learning/chat data from human profiles with versioned, atomic persistence and safe-position recovery. Resolve how actor-aware storage participates in existing account journals before enabling any human-bot transfer; simply choosing a different save folder is not sufficient for cross-account transactions.
- Residents get a finite, recorded starter allocation and then consume/earn actual resources. Any later supply subsidy must be explicit, budgeted and logged. Banks, shops, drops and NPC stocks must not become unlimited replenishment paths.
- Test actors use isolated inventories/rewards and resettable profiles. Block transfer to persistent players/residents, including trades, death loot, ground drops, shared banks and activity rewards; do not rely only on a flag on the initial spawned item.
- Initially exclude bot participants from custom EP/PK bonuses, reciprocal target pools, staking and Gambler payouts until an explicit bot reward policy is accepted and tested. Human-versus-human custom behavior stays intact. Friendly no-stake testing is the first PvP mode.
- Before economic trading, review TradeSession and every transfer/recovery path for slot/metadata conservation, both-party versioned acceptance, inventory-full handling, save failures and restart recovery. A language-model suggestion cannot consent to a changed offer or set an arbitrary price. Use bounded valuations/budgets and revisions.
- Keep only useful, bounded chat memories. If external conversation is enabled, disclose the provider, select what text may leave the machine, and set a spending limit before connecting it. Do not send credentials, account saves or unnecessary private chat. Treat conversational instructions as untrusted input, never server authority.

## Implementation phases and acceptance gates

| Phase | Deliverable | Required gate |
|---|---|---|
| 0. Contracts and harness | Actor identity/liveness, input/action boundary, persistence/disable contracts, diagnostics; baseline world-cycle measurements and scheduler investigation. | Exact touched-system inventory, normal-client compatibility tests and no overlapping world mutations under forced slow cycles. Decide storage and emergency settlement before coding dependent features. |
| 1. One controllable resident | Explicit socket-free lifecycle; real appearance/movement/following; safe save/reload; on/off/status commands. | Human client sees/interacts with it; collision and logout/restart work; repeated enable/disable leaks no players/timers/membership; no human profile collision. |
| 2. Companion and test opponent | Normal attack/eat/equipment/prayer actions; adjustable reaction profile; one supported PvM encounter and friendly no-stake duels. | Existing combat rules/resources apply; no hidden information; blocked paths, death, delayed hits and off-during-combat covered. Live client acceptance with godmode off. |
| 3. Living population | Small persistent roster, goal selection, two supported training/gathering loops, banking, schedules, director and auto/friends controls. | Repeated complete activity cycles, varied behavior, human join/leave debouncing, durable off, resource accounting and live mixed-load soak. |
| 4. Social and economic activity | Personality/memory, optional async conversation, invitations, controlled resident trades and then human trades. | Model outage/stale replies harmless; useful bounded chat; revised offers cannot reuse consent; interrupted transactions/restarts conserve items. |
| 5. Measured learning | Outcome records, bounded strategy adaptation, policy versions, freeze/reset/rollback and decision inspector. | Held-out comparisons demonstrate gains beyond chance without illegal actions, reward farming or loss of intended difficulty. Memory survives restart; scenario reset remains deterministic. |
| 6. Broader content and optional training | Additional boss roles, routes, skills, configured risky PvP and possibly imitation/self-play service. | Per-activity support matrix, reward/disable contracts, failure tests and real-client/load acceptance. Expand only within measured limits. |

Memory/event recording begins early so later learning has useful evidence; adaptation is enabled only after its phase-5 acceptance. One boss encounter does not certify Nex or every other bespoke mechanic. Solo Fight Caves remains solo; companions require an activity that already supports multiple participants or a separately authorized content change.

All phases need tests appropriate to the touched systems. Critical cases: duplicated requests, repeated on/off, off during pathing/trade/duel/death/instance close, committed transfer plus crash, stale model responses after relog, full inventory, invalid target, failed saves and map/task cleanup. Re-run affected combat/instance/PvP/duel regression suites and broaden according to shared changes. Use fixture tests for correctness, actual World/network/client sessions for integration and live soak/load evidence. Record tick percentiles/overruns, path queue length, model queue age, retained memory, actor counts and cleanup failures. Existing 16,200-session instance fixtures do not establish full AI-world capacity.

## Proposed starting decisions

Proceed, if implementation is requested, with phases 0-1 first: bots disabled by default, explicit AI identities, persistent residents separate from test actors, no external model dependency, no reward-bearing bot PvP/trades yet, no Summoning or Dungeoneering work. Deliver one visible, controllable resident and durable safe off before population growth. Then phase 2 proves useful combat companionship/testing. Provider/hardware budget, exact initial hubs/boss, risky PvP rewards and human trading policy can be decided before their dependent phases; they do not block this plan.

Future implementation reports should record source/runtime changes and validation separately from this planning snapshot.
