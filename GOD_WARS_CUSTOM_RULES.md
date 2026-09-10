# God Wars: selected server fight definitions

2026-09-09. The owner authorized best-judgment definitions for unresolved historical gaps. These are intentional server rules, not claims that missing 2011 evidence was recovered. This document supersedes provisional choices in GOD_WARS_FIXES.md and the proposed next batch in GOD_WARS_EVIDENCE_REVIEW.md only where listed below. Source research and its confidence limits remain preserved there.

## Fight contract

All numbers below use this server's life points. Existing boss HP (2,550), packed combat levels/bonuses, follower profiles, attack cadence, access conveniences, drops and rewards remain the selected baseline.

| Mechanic | Selected definition and rationale |
| --- | --- |
| Zilyana | Melee maximum 270; successful magic rolls 100–200. Equal melee/magic selection, 2-tick cadence. Gives her rapid attacks a consistent damage budget. |
| K'ril | Ordinary melee maximum 460; successful magic rolls 100–300; prayer smash uniformly 350–490. Retain 6-tick cadence. |
| K'ril selection | One-third magic; otherwise melee, with a one-in-nine replacement by smash only against Protect from Melee or Deflect Melee. Overall smash chance with eligible prayer is 2/27 per selection. No smash against Protect from Ranged. |
| Smash effects | Bypass protection; drain half current prayer, rounded down, only on positive applied damage. Strength drains do not weaken this fixed special. Shields and godmode retain their existing typed-damage handling. |
| Poison | One-in-four application roll on K'ril melee/smash; successful accuracy contact required. Ordinary melee may poison through protection even when damage becomes zero. True accuracy misses cannot poison. Initial strength 160 LP; existing antipoison/godmode rules apply. Retain the shared poison schedule: 30-tick intervals, reduction of 2 LP after each poison hit, at most eight hits. This is our existing server poison behavior, not historical certification. |
| Kree attack selection | When nobody eligible is attacking Kree, select one physical melee strike (maximum 260), pursuing into contact. Otherwise independently select ranged/magic tornadoes 50/50 for each room target. Retain 3-tick cadence. This gives untargeted pursuit a predictable response. |
| Kree defence and displacement | Physical melee uses melee accuracy/defence. Both tornadoes use ranged accuracy/defence, with their own damage/protection style; ranged maximum 715, magic maximum 210. Each successful tornado contact attempts one tile away from Kree's centre, including diagonal movement. Reject blocked or out-of-room destinations; no extra freeze. Protection-zero contact can push; godmode prevents the status. No second melee hit. |
| Graardor | Retain two-thirds melee (maximum 600), one-third ranged room attack (successful roll 150–350), 6-tick cadence. |
| Bodyguards | While the boss lives, retain existing room-local support targeting. After K'ril dies, each living, visible follower independently selects an eligible room player, with no kill-credit preference. An empty eligible list clears its target. Other gods retain their existing killer-focus policy. |
| Strength drains | Scale ordinary physical melee maximum by current/base Strength, rounded down and clamped to 1..base maximum. Applies to bosses and melee bodyguards. Capture the maximum when launching the hit; later recovery cannot modify a pending hit. Magic, ranged and smash caps stay fixed. Existing numeric stat recovery/reset remains in use. |
| Visuals and impact timing | Adopt the current native animation/projectile/graphic IDs in GodWarsAction as our definitions. No-projectile attacks impact after 1 tick; projectile attacks use max(1, floor(distance × 0.3)) ticks. Preserve current projectile parameters and bodyguard native definitions. These are selected presentation rules pending live appearance checks. |
| Respawn/reset | Preserve boss 100 ticks, follower 25 ticks conditional on boss alive, and empty-room reset after 10 ticks. Boss revival does not heal surviving followers. |

## Implementation

GodWarsAction now implements the new damage caps and ordinary Strength scaling. Its melee accuracy path retains -1 for a genuine miss, preventing poison contact effects on misses. It uses the shared melee accuracy/defence formulae without changing those shared methods.

Validation exposed a pre-existing prayer-index mistake: K'ril checked index 18 (ranged) for smash eligibility. The action now uses CombatType.MELEE.getProtectionPrayer() (19). The prior regression's duplicated wrong index was corrected; new tests explicitly distinguish ranged prayer and Deflect Melee.

GodWarsRoom gives Zamorak survivors independent eligible target draws even if the boss's killer is absent. Other gods' death behavior is unchanged. No client, cache, account format, drop table or shared combat implementation was modified.

## Validation and release

- Focused Java 8 compilation passed (only compiler warnings about the old target).
- GodWarsRegression: **9,304 checks passed**. Covers all attack families, protection/godmode/Divine, delayed hits, room boundaries, lifecycle, actual death routing, native resource existence and spawn maps.
- GodWarsCustomRegression: **2,504 checks passed**. Covers independently specified caps, ordinary drain scaling and reset, immutable launched maximums, fixed special/magic caps, poison on protected contacts versus accuracy misses, poison strength, post-death target independence and eligibility, empty-room behavior and prayer selection.
- Total: **11,808 checks**. Tests were selected for the two changed God Wars classes; unrelated gameplay suites were not rerun. Existing test helpers were compiled for fixtures only.

Runtime staging: build/godwars-custom, with prior source/runtime backups under build/godwars-custom-before. The staging gate checks both suite passes and validated source/test SHA-256 hashes. Exact changed class families are copied and their runtime hashes checked; manifests record staged and backup classes. Earlier God Wars staging and evidence records remain intact.

**Server restart required.** Live fight acceptance remains: verify Zilyana's lower damage budget, K'ril prayer switching/poison, Kree pursuit and clipped push, target switching with multiple players, and animations/projectiles on the developer client. Automated cache existence does not certify visual identity or client rendering. The definitions are settled; live feel and presentation are still acceptance work.

Shared percentage-curse ownership/reset calibration remains the separately recorded combat backlog; this batch implements ordinary numeric Strength drains, not a broader curse overhaul. Preserve Summoning and Dungeoneering deferrals, godmode and private-server conveniences.
