# Barrows implementation follow-up — 2026-09-07

This records the changes authorized after BARROWS_2011_REVIEW.md. The target remains the original six brothers in early 2011. This is a substantial fidelity and correctness pass, not certification of every historical formula or client visual.

## Implemented

- Dedicated, owner-bound brother combat instead of generic NPC attacks. Dharok scales with missing life points; Guthan heals from actual life points removed; Verac has a prayer/accuracy bypass proc; Torag drains energy; Karil drains Agility; Ahrim has Strength reduction and separate stat spells. Attack delays distinguish Dharok (7 ticks), Karil (4), and the others (5).
- Godmode still prevents damage, prayer drain, and the new harmful brother effects. Verac's exception applies only to his protection-prayer check, preserving other reductions. Personal XP rates and existing custom features remain.
- Haunting drains 8 plus the number of defeated brothers approximately every 18 seconds. Fixed face selection to translate NPC IDs into brother indices.
- All 64 gate identities now survive lookup. There are 32 connected layouts, each with one central approach and one blocked outer connection. Boundary doors remain locked. Crossings use actual cached door rotations, validate proximity, and cannot override a freeze or an existing movement lock.
- Native interface 25 displays the four shape puzzles using cached models 6713–6736. Answer positions shuffle. Wrong answers change the central route. Closed interfaces and stale location answers cannot unlock doors. Opcode 4 was checked against the local client decoder; no cache/client modification was needed.
- Randomized entry across four corner rooms. Rope exits return to the selected crypt. Added missing corner ropes while retaining existing custom ropes and the tunnel bank.
- Brother identity/ownership checks prevent foreign or duplicate kill credit. Cleanup does not fabricate kills. Leaving a crypt allows its undefeated brother to be summoned again.
- Loot is marked claimed before rewards are generated. A run remains valid after looting until the next dig resets it, avoiding the old invalid entrance index. Surviving brothers can continue attacking after looting.
- Rewards use one roll plus one per defeated brother. Equipment comes only from defeated brothers, and multiple pieces are possible. Tunnel kills improve ordinary reward potential, not equipment chance. Brother combat levels also contribute (656 for all six); potential caps at 1,000 before the small brother bonus. Key halves and dragon med helms are reachable.
- Stored tunnel-monster potential has an optional tagged save extension. Older saves load without it; the defeated-brother contribution is derived from the existing killed list. Existing looted-state serialization remains intact.
- Collapsing tunnels now inflict modest environmental damage as well as shaking the camera; godmode remains immune.

## Reward and historical limits

The selected equipment formula is 1 / (450 - 58 * brothers) per roll, with seven rolls for all six: about 6.66% of chests contain equipment. This replaces the previous roughly 28.18% six-brother custom chance. Common stack quantities are a reconstruction, not verified 2011 quantities.

[Jagex's published drop-rate explanation](https://www.runescape.com/drop-rates) supports the roll structure and reward categories, but was published after 2011 and includes later content. Only the six-brother rules are used here; no Akrisae, Linza, defenders, later luck items or modern convenience unlocks were added. Exact 2011 quantity distributions, proc probabilities/timing, Ring of Wealth changes during 2011, and the original maze-generation distribution remain historical calibration work.

The [period-style Knowledge Base reproduction](https://www.2011.rs/kb/barrows) describes the original encounter features but is not an authenticated dated archive. [The puzzle implementation author's model mapping](https://rune-server.org/threads/apollo-set-widget-model-barrow-puzzle-shapes.619806/) supplied the shape families, verified to exist in this cache. [August 2011 player discussion](https://forum.tip.it/topic/299238-is-it-worth-getting-a-barrows-killcount/) provides period context for brother and tunnel kills.

Shared combat accuracy/soaking issues from COMBAT_2011_REVIEW.md are still pending. Brother attacks deliberately use the existing damage system, so this pass does not make those underlying formulae historically exact.

## Validation and deployment

- Java 8-targeted compilation passed. Only the compiler's obsolete-target warnings were emitted.
- tests/BarrowsRegression.java passed against the cache: all 32 layouts, all 64 door identities, both directions of each crossing, solid-tile exclusion, boundary locks, connectivity, and puzzle assets.
- Tests cover unique/owner-only kill credit, cancelled/correct/incorrect puzzles, fresh-run reset, old/new player saves, prayer/Verac behavior, all six brother impacts and harmful-proc immunity with godmode.
- A seeded simulation of 100,000 full-brother chests produced 6,650 equipment chests, including 169 with multiple pieces, consistent with the selected probability.
- Existing Nex movement and ice regression suites passed.
- 39 affected runtime classes were staged in bin. Original sources and prior runtime classes are backed up under build/barrows-before/20260907-205016. No running server was restarted.

After a normal server restart, perform an in-game acceptance run:
1. Fight each brother with ordinary equipment and godmode off; compare attack cadence, Ahrim graphics, protection prayers, and signature effects.
2. Traverse doors from both directions; try each puzzle family, a wrong answer, and closing the puzzle during combat.
3. Loot, escape by rope, start a second run, then test logout/reconnect before and after looting.
4. Repeat with godmode enabled to confirm the intended custom immunity.
5. Check the existing bank/extra ropes and the new corner ropes.

Actual client animation/rendering, multiplayer behavior and real-session reconnects still require that in-game check; the headless tests do not certify them.
## Face timing follow-up

The user confirmed the minigame worked in-game, but reported nearly constant faces. The prayer/haunt trigger was already approximately 18 seconds apart. The face selector (client config 1043) was never cleared, and interface 24 was reopened on door crossings and kills with that stale value.

The fix retains the 30-tick interval, explicitly clears faces after five ticks (3 seconds), clears stale faces before activity overlay refreshes, and clears/resets haunting on leaving the underground area or looting. The three-second visual window is a chosen brief display duration, not a certified frame-exact 2011 duration. Current [Barrows strategy documentation](https://runescape.wiki/w/Barrows/Strategies) corroborates the approximately 18-second drain interval, but is not dated 2011 evidence.

tests/BarrowsFaceRegression.java executes the real area tick with captured outgoing packets: verifies 30-tick spacing, five-tick face clear, unchanged prayer loss, no positive face packets on overlay refresh, and surface/re-entry timer reset. Passed. The updated PlayerAreaTick and BarrowsActivity runtime families are staged; backups are in build/barrows-face-before. A normal server restart and visual retest are required.
## Intentional private-server loot boost

The user subsequently requested slightly better loot. This explicitly supersedes the historical reward-rate target above; preserve it in future fidelity work.

- Equipment rolls are 1.5 times as likely (base denominator divided by 1.5, rounded up). All six brothers give seven 1/68 rolls: approximately 9.85% of chests contain equipment, versus 6.66% previously (roughly 1 in 10 instead of 1 in 15).
- Rune and bolt-rack stacks are multiplied by 1.5; coin stacks by 1.25, rounded up.
- Equipment still comes only from defeated brothers, multiple pieces remain possible, and tunnel kills still affect ordinary loot potential. Key halves and dragon med helms retain their quantities and eligibility.
- Compilation and BarrowsRegression passed, including supply quantities, rare quantity preservation and a 100,000-chest simulation (9,742 equipment chests, 405 multi-piece chests).
- Updated BarrowsRules.class staged in bin, hash verified. Prior source/runtime backed up in build/barrows-loot-before. Normal server restart required.