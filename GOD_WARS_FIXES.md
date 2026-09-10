# God Wars fights — step 3

Implemented 2026-09-09. The owner authorized the third consolidated boss batch, then explicitly approved OSRS Wiki and other reputable sources for uncertain mechanics, with 2011 RuneScape Wiki material preferred where available. This report supersedes the original boss review only for the changes below.

## Result

The four original God Wars bosses and their twelve bodyguards now use dedicated, room-owned encounters. Access shortcuts, teleports, drops, account saves, godmode/XP, custom PvP and the Summoning/Dungeoneering deferrals are preserved. No client/cache editing or server restart was performed.

| Boss | LP | Attack interval | Implemented damage caps, LP |
| --- | ---: | ---: | --- |
| General Graardor | 2550 | 6 ticks | Melee 600; ranged shockwave 150–350 on a successful roll |
| Commander Zilyana | 2550 | 2 ticks | Melee 315; magic 100–315 on a successful roll |
| K'ril Tsutsaroth | 2550 | 6 ticks | Melee 490; magic 100–300; prayer smash 350–499 |
| Kree'arra | 2550 | 3 ticks | Melee 260; ranged 715; magic 210 |

Damage caps retain the pre-EoC reconstruction where period references conflict with current OSRS; see confidence limits. Protection, absorption, spirit shields, godmode, recoil and vengeance use the existing typed hit path. Numeric LP ranges are inclusive; the engine uses individual LP rolls rather than limiting damage to multiples of ten.

Graardor uses 2/3 melee and 1/3 room shockwave. Both require contact with his primary target. Zilyana's two attacks also require contact, permitting kiting; her magic can affect other eligible room occupants. K'ril's magic and melee are single-target/contact attacks. With melee protection active, one in nine selected melee attacks becomes a guaranteed-accuracy prayer smash, bypassing protection and draining half the remaining prayer points, rounded down, after positive damage. Normal magic remains one third of attacks. Poison can occur through a protection prayer, with antipoison and godmode still respected. Kree uses room-wide tornadoes while targeted; otherwise he pursues his target to use melee. Blue tornado accuracy uses ranged defence while damage/protection remain magic. Knockback checks the destination, clipping and room boundary. The existing restriction on ordinary melee attacks against Armadyl NPCs remains.

Each launched victim has an independent damage record, NPC-life/instance context and room revision check. Leaving, hiding, disconnecting, changing plane, or invalidating a life prevents that affected impact. A departing primary target cannot cancel valid secondary room hits. A room reset/closure invalidates outstanding hits. This does not claim that an ordinary teleport out and back between checks is a separately versioned room departure.

## Room lifecycle and NPC behavior

- One room owns exact NPC objects by ID; duplicate membership is rejected. Separate explicitly attached rooms cannot share target ownership. The normal four rooms attach from their physical spawn coordinates; copied maps must attach an explicit room through their content owner.
- Bosses respawn after 100 ticks (60 seconds), independently of surviving minions. Dead bodyguards return after 25 ticks (15 seconds) while their boss is alive, or wait for its return. Living minions are not healed/resurrected by a boss respawn. These fixed timings preserve a convenient private-server pace; population-scaled historical timing is not reproduced.
- Shared death processing still handles faction credit and the existing delayed loot once. God Wars bypasses the old global four-ID group search. Missing followers no longer prevent a boss returning.
- Bodyguards periodically prefer the boss's last valid attacker, with its tank as fallback. On boss death they prefer its last attacker, falling back to loot credit when none exists. This is a practical shared policy, not a claim of exact individual bodyguard aggro for every god; current OSRS describes random post-death Zamorak targets.
- An engaged room empty of eligible players for ten ticks resets living members' HP, numeric drained levels, poison, freeze, queued combat and positions. Dead members retain their respawn timers. The older percentage curse ledger remains a separate combat backlog.
- God Wars bypasses the generic 12-tile spawn leash and the shared tick rule that could reset first-contact pursuit before any attack had landed. Other NPCs retain those rules. NPC world tasks execute sequentially in the current World loop.

## Stats and assets

Only the four boss records changed in `NDE/NPCDefinitions.bin`. Their editable XMLs match. HP, speed, five combat levels, offensive bonuses and ordinary defensive bonuses use the profile below. The Summoning-defence and prayer-bonus slots remain untouched. All other records/bytes are verified unchanged from this batch's input. Bodyguard combat data outside the dedicated attack selection/caps remains its existing packed data.

| Boss | Attack / Strength / Defence / Ranged / Magic | Melee / Magic / Ranged attack bonus | Melee / Magic / Ranged defence |
| --- | --- | --- | --- |
| Graardor | 280 / 350 / 250 / 350 / 280 | 120 / 0 / 100 | 90 / 65 / 90 |
| Zilyana | 280 / 196 / 300 / 250 / 300 | 195 / 200 / 0 | 100 / 100 / 100 |
| K'ril | 340 / 300 / 270 / 1 / 200 | 160 / 0 / 0 | 80 / 130 / 80 |
| Kree | 300 / 200 / 260 / 380 / 200 | 136 / 0 / 120 | 180 / 200 / 200 |

These hidden stats are an explicitly authorized OSRS fallback, not certified recovered 2011 server values. Documented later changes were excluded: Graardor's 2017 magic/Twisted Bow rebalance, K'ril's 2025 defence reductions, split ranged weaknesses and elemental weaknesses. Fixed attack caps are separate from these accuracy/defence inputs; exact damage changes under Strength drains remain an encounter calibration boundary.

Missing projectile delivery for Wingman Skree, Flockleader Geerin and Balfrug was supplied with native-cache IDs 1199, 1190 and 1213. Existing bodyguard start/end graphics are emitted. All selected boss animation/graphic definitions and non-melee bodyguard projectiles exist in the supplied cache. Cache presence proves loadability, not visual identity or timing; live visual acceptance is required.

## Evidence and unresolved differences

- [May 2011 Armadyl event guide](https://runescape.wiki/w/RuneScape%3AEvents_Team/Armadyl_GWD_%2821_May_2011%29) supplies the period Kree LP/damage figures retained above. This is a dated guide still available today, not a verified immutable 2011 page revision.
- [December 2011 Saradomin event guide](https://runescape.wiki/w/RuneScape%3AEvents_Team/Saradomin_GWD_%2810_Dec_2011%29) corroborates rapid attacks and the contact requirement for both styles. [Pre-EoC Zilyana mirror](https://wiki.darkan.org/Commander_Zilyana) reports 315 caps, whereas [OSRS Zilyana](https://oldschool.runescape.wiki/w/Commander_Zilyana) reports 270/200 LP-equivalent caps. The implementation retains 315 provisionally; this explicit conflict is not closed by the OSRS fallback.
- [OSRS Graardor](https://oldschool.runescape.wiki/w/General_Graardor) includes Jagex/Mod Ash citations for style odds and shockwave damage, and documents the later magic-stat change. Its ordinary stats are used with that change reversed.
- [OSRS K'ril](https://oldschool.runescape.wiki/w/K%27ril_Tsutsaroth) and [strategy guide](https://oldschool.runescape.wiki/w/K%27ril_Tsutsaroth/Strategies) support selection odds, minimum magic/smash damage, prayer drain and close-range behavior. [Pre-EoC strategies mirror](https://wiki.darkan.org/K%27ril_Tsutsaroth/Strategies) and OSRS differ on regular damage/poison figures; current 490/499 caps retain the initial pre-EoC reconstruction and require further period calibration. Poison chance is a provisional 1/4; no authoritative rate was located. No modern Spectral shield prayer-drain passive was imported.
- [OSRS Kree](https://oldschool.runescape.wiki/w/Kree%27arra) and [strategies](https://oldschool.runescape.wiki/w/Kree%27arra/Strategies) supply the three-tick fallback, unopposed pursuit, tornado defence behavior and knockback. Modern halberd access and elemental weaknesses were excluded. The implementation uses one melee hit, not the disputed double-hit account. Its ordinary melee accuracy still uses the shared melee formula; the modern page's magical-melee classification is not certified for 637.
- Zilyana's style choice and Kree's per-player tornado choice currently use 50/50 selection; exact original distributions remain unverified. Attack visuals, knockback direction/feel, all doorway boundaries and real-world target switching need in-game checks.
- Direct wiki revision-history requests were blocked by the browsing service. Dated event pages, indexed wiki content and update histories were usable. Do not present this investigation as a complete recovery of archived 2011 revisions.

## Validation and runtime

Java 8 compilation passed for the ten changed source families and selected test dependencies. Scoped checks:

| Suite | Checks | Reason |
| --- | ---: | --- |
| GodWarsRegression | 9304 | Four fights and all bodyguard handlers, mitigation, bounded damage, prayer smash, deterministic odds, independently owned area hits, invalid contexts, real death routing, lifecycle/reset, native assets and all 16 actual spawn footprints |
| BossBatchOneRegression | 18754 | Preserve DK attacks/data and singleton spawn policy; Bandos-specific tests moved to the new suite |
| NPCFoundationRegression | 425 | Shared NPC life contexts, drains, ordinary leash, retained targets and timers |
| FightCavesInstanceRegression | 414 | Shared NPC death/tick changes preserve existing cave ownership/progression |
| InstanceLifecycleRegression | 73 | NPC destruction/context ownership boundaries |

The backed-up old NPC implementation fails the dedicated new death-route test: a boss with missing followers does not return through room ownership. Log: `build/godwars/negative-old-npc.log`. No unrelated accumulated gameplay suites were run. The new reusable scoped runner is `tests/godwars/verify.ps1`.

Twenty-four runtime classes from ten source families are staged and SHA256 verified in `bin`. Source/data backups: `build/godwars-before`; runtime backups: `build/godwars-before/bin`. Build, logs, validated-input hashes, backup manifests and staged-class hashes: `build/godwars`. Old unused legacy action classes remain unregistered; unrelated source/runtime work was preserved.

**Restart the server before live testing.** Use ordinary combat with godmode off. Test solo and with a second player: enter through the usual shortcut, kite each boss, compare prayers, observe all projectiles/animations, exit during a launched room attack, test Kree against corners, kill followers before/after the boss, leave a follower alive over boss respawn, and leave/re-enter after the empty-room reset. Client appearance, real network transitions and live loot/respawn acceptance remain pending. Step 4 was not started.

## Evidence follow-up (2026-09-09)
Read GOD_WARS_EVIDENCE_REVIEW.md for the subsequent source review. It downgrades the late-2012 mirrors as proof of 2011 caps, finds direct Jagex poll support for Kree magic using ranged defence, and recommends explicit OSRS fallback damage corrections plus separate Zamorak bodyguard targeting. These are research recommendations only: the staged gameplay and validation above have not changed. Remaining original rates, Kree melee details, visuals and historical drain arithmetic are not certified.
