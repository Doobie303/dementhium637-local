# Custom PvP, targets and EP fixes

Implemented 2026-09-08 following PVP_TARGET_EP_2011_REVIEW.md. The owner explicitly chose to preserve the custom theme, existing reward pool and faster EP gain, with EP improving bonus chance and target kills earning an extra roll. This is a custom pre-EoC PvP system, not a conversion to January Bounty Worlds or February-onward historical Wilderness rules.

## Preserved custom behavior

- The existing Player.PVPItems pool, including its duplicate-entry weighting.
- 2 EP every 81 eligible game ticks: nominally 48.6 seconds. No new wealth requirement for earning EP.
- The fast likelihood progression: starts at 5, adds 5 per EP interval, caps at 60, with random matching eligibility beginning at 30. Both candidates must now be ready and compatible.
- Ordinary Wilderness item drops, protected-item counts, configured exchange-price ordering and existing equipment-to-coins conversions.
- Existing custom safe-PK zones, respawns, special-energy refills and activity death policies.
- Main killer donor points at x2, staff-victim bonuses, and the existing recipient-own-risk point scaling.
- Godmode, personal XP rates, private Barrows rewards, and the Dungeoneering migration deferral.

## Correctness fixes

1. **Death items are conserved.** The death snapshot has room for all 28 inventory and 14 equipment slots. Physical stacks remain separate, so inventory and equipment stacks cannot overflow when combined. Protection removes individual units from the correct snapshot slot. Copies retain item degradation/charge metadata. Ordinary coins and ammunition are no longer omitted.

2. **Bulk container copying works for stacks.** Container.addAll now uses an atomic copy operation. Insufficient capacity or unsupported overflow rejects the whole transfer rather than silently dropping items or partially mutating it. Explicit split-stack containers support overflow into an available slot. Source items and their metadata are copied rather than aliased. Existing callers in price checking, duel stakes and familiar inventory reordering inherit this shared correction; their gameplay rules were not rewritten.

3. **Death and rewards use the victim's context.** Lethal player damage is credited before synchronous death callbacks choose the killer, including the actual HP clamp. The death captures its killer, last attacker, target and location. Old EpDrop's lookup on the surviving attacker was removed. Ground rewards belong to the resolved killer at the victim's death location. Point caps no longer disable EP loot.

4. **Repeated callbacks cannot settle the same death twice.** Item application and custom reward settlement have separate guards reset at the start of the next death. Optional reward/UI exceptions are logged and cannot prevent subsequent skull removal, respawn, prayer closure and credit cleanup. Completed death contexts release player references. This is in-process protection, not a durable transaction across a forced server crash.

5. **Targets are mutual and exclusive.** Pairing requires two online, connected, living, registered players in the Wilderness, on the same plane, without existing assignments, both sufficiently ready, and within the enforced combat-level range. Managed-instance participants are excluded. Pairing reserves both players together. Death captures the assignment before clearing both arrows. A third party cannot take an occupied target.

6. **Departure cleanup is consistent.** Leaving, logout/disconnect, death, invalid range/plane and stale assignments clear both sides. The original immediate-departure policy is retained: the departing player loses waiting progress, while an innocent remaining partner keeps theirs. EP remains saved; active pairings and partial waiting timers remain session-only. Historical ten-minute grace and persistent pairing recovery were deliberately not added.

7. **EP has one earn/spend contract.** Bonus chance uses the resolved killer's EP before it is reset. All EP bands are covered. Only that recipient's EP is consumed; an unrelated last hitter retains their own EP. Saved EP is clamped to 0-100. A completed target resets the winner's target likelihood and grants the selected extra roll.

8. **PK points and repeat rewards are consistent.** A donor assist now receives a rounded-up 30% bonus (base 2 becomes 3), replacing the truncating expression that could award zero. The main donor x2 remains. A five-minute account-pair cooldown suppresses extra points and EP loot for repeated kills in either direction. Ordinary carried-item loot and kill/death statistics remain; suppressed kills do not consume EP. The bounded cooldown table uses account names, survives logout within this server process, and resets at server restart. It is basic farming resistance, not comprehensive alternate-account detection.

9. **The UI and attack rules share the same combat range.** Wilderness uses both players' depth constraints. Custom unrestricted PvP/SafePk modes retain their existing behavior. Entry refreshes range/EP strings, colour bands are complete, and Wilderness entry uses the target overlay consistently. Actual client presentation still requires acceptance testing.

10. **Old damage credit expires.** A player's prior encounter stops influencing kills after 100 ticks without credited damage, including a later environmental death. NPC credit expiration is unchanged. Damage totals saturate safely. The shared existing owner/familiar aggregation lookup was corrected to avoid null unboxing, and death routing normalizes the owner even when the last-attacker pointer has expired. These were necessary shared reward dependencies; no Summoning-specific source, attacks or balance were changed.

11. **Skull cleanup is safe.** Expiry removes reciprocal bookkeeping, uses the exact expiry boundary, and tolerates missing references without fragile list indices. Existing durations remain. Correction to the review's approximate wording: the retained offensive timer is 3333 ticks (about 33m20s), and the retained Abyss minimum is 1650 ticks (16m30s), despite inaccurate old source comments describing 20/10 minutes. This patch does not calibrate these custom durations to historical RuneScape.

12. **Teleblock applies at successful impact.** Splashes, godmode and hit immunity do not apply it. Active blocks cannot be extended by reapplication; immunity ends with the block. The existing custom 550-tick duration (330 seconds) and absence of prayer-based duration reduction remain. Existing teleport gates are preserved. This fixes lifecycle bugs without introducing an unrequested historical duration rebalance.

## Bonus configuration

Read once at startup from settings/pvp.properties. Restart after editing.

| Setting | Default / meaning |
| --- | --- |
| epIntervalTicks | 81 |
| epGain | 2 |
| baseDropChancePercent | 10% at 0 EP, rising linearly to 100% at 100 EP, integer-rounded down |
| targetExtraRolls | 1 guaranteed extra roll for a completed target |
| repeatRewardTicks | 500 (five nominal minutes), account pair in either direction |
| damageCreditIdleTicks | 100 (one nominal minute without credited damage) |

For an eligible ordinary kill, the bonus chance is `10 + floor(90 * EP / 100)` percent with these defaults. Every eligible rewarded kill resets the winner's EP to zero, even if its ordinary bonus roll fails. At 100 EP an ordinary kill gives one pool item; a target kill gives two. At lower EP the target still guarantees its extra roll. Point rewards and ordinary victim-item drops are separate. This policy is the owner's approved custom enhancement, not a historical reward-table reconstruction.

## Validation

Java compilation targets Java 8. Runtime tests use Java 8 and the real project cache with simulated players/connections; no server/client was launched and no account saves were edited.

All 18 regression suites passed during this batch: PvP; combat foundation, formulas, balance and enhancements; equipment absorption; Nex ice and movement; Barrows and Barrows face; instance foundation, lifecycle, integration, content, parties, operations and load; and Fight Caves instances. The load suite exercised 16,200 simulated sessions and 46,000 measured cycles. After the final interface-entry refresh, PvP, Barrows face and instance integration were rerun and passed.

**PvpRegression: 9,568 checks.** Coverage includes atomic bulk-copy failures, overflow, metadata, full 42-slot inventories, 0/1/3/4 kept items, randomized conservation, actual coin/ammo ground drops, non-retaliating victims, ordinary versus target bonuses, multi-player assist ownership, repeat callbacks/kills, donor/staff/point-cap cases, EP bands, real area-tick accrual, matching restrictions, disconnect/death cleanup, range/UI agreement, stale and lethal credit, shared owner/familiar attribution, skull retaliation/expiry, and Teleblock cast/impact/splash/immunity.

A fault-injection case deliberately throws during the bonus reward roll and runs the real scheduled death path. Respawn, skull removal, prayer cleanup and damage/context release still complete. The injected stack trace in PvpRegression.log is expected and the suite ends in a passing result.

Reproduce: build/pvp-fixes/verify.ps1. Compilation and per-suite logs are in build/pvp-fixes.

## Runtime staging and live acceptance

**20 runtime classes were staged and SHA-256 verified. A normal server restart is required.** No automatic restart was performed.

- Source backups: build/pvp-fixes-before/src.
- Previous runtime families: build/pvp-fixes-before/bin.
- Source hashes: build/pvp-fixes/source-manifest.csv.
- Staging/backup manifest: build/pvp-fixes/staged-classes.csv.
- Explicit changed-source list: build/pvp-fixes/changed-sources.txt.

Rollback must use this batch's manifest/backups and preserve unrelated changes; PvpSystem is a new class with no previous counterpart. The original review remains a historical snapshot; only the fixes listed here supersede it.

After restart, use ordinary accounts with godmode off to verify full-inventory coin/ammo deaths and protected items, both clients' target names/arrows, leaving/reentering, an ordinary and target kill at 100 EP, the repeated-opponent message, and Teleblock impact/expiry. Check fixed and resizable UI modes. Staging and headless tests do not certify live client rendering, real disconnect/restart behavior, or production economy balance. Historical prayer-dependent Teleblock, skull-duration calibration, persistent bounties and more advanced anti-farming remain separate choices.
