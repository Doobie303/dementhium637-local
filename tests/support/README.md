# Reusable test fixtures

These are test-only sources; never put them in stageSources or package them into a client.

## CombatFixtures.java

Used by AdvancedBossRegression and NexDamageRegression. Include this source explicitly when compiling either current suite. It uses the default package to match the existing standalone regression mains.

- init(): loads real cache/definitions and custom handlers once per JVM, installs the headless area manager, and creates the synthetic 3200/3200 clipping region. Use only in isolated standalone test JVMs, never a running server.
- player(npc): connected, online level-99 player with starter flag, 1,000 LP, 99 prayer, calculated bonuses and a reproducible random seed. Adjacent placement updates region membership; it does not add to World.players or enable godmode. A test needing World-list targeting must explicitly register it.
- clearPlayers(): marks only helper-created players offline, removes them from World.players if registered, and clears fixture ownership. It is not a general logout/recovery simulation or a complete Region registry reset.
- readyAttack(npc, player): selects the victim and sets executor cooldown to zero for direct action tests. Ordinary tick/cooldown tests should leave this helper unused so they exercise production timing.

Encounter-specific bounds, phases, protections and assertions stay with their tests. Do not import all combat suites simply for fixture setup; NexDamageRegression no longer depends on AdvancedBossRegression.

Existing narrow helpers worth reusing when relevant: BossBatchOneRegression.records parses packed NPC records; BossBatchOneRegression.prayers exposes the test prayer state. Inventory's actual clear API is getInventory().getContainer().clear(). Verify signatures before extending setup. Shared fixture changes require checks of their actual consumers, not every combat suite.

Historical step-5 manifests refer to the old self-contained test sources and are already staged. Preserve those manifests, logs and backups. For a future batch, declare tests/support/CombatFixtures.java alongside the suites that now use it.
