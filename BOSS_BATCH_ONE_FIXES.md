# Boss batch 1: data, spawns and urgent Graardor repair

Implemented 2026-09-09 following the owner's authorization to start the first consolidated boss batch. The owner explicitly selected **one of each Dagannoth King and one Corp**. This report supersedes only the defects explicitly repaired below in BOSS_AND_NPC_REVIEW.md.

## Implemented

- Removed extra active King/Corp spawns by commenting their original lines. Retained Supreme (2903,4447,0), Prime (2915,4449,0), Rex (2923,4438,0), and Corp in the main chamber (2988,4384,2). Each full native NPC footprint passes collision checks against the actual cache landscape.
- Restored the three missing packed King records and added matching editor XML. Registered an actual DagannothKing handler: Supreme ranged, Prime magic, Rex melee; explicit maximum hits 300/610/280 LP and four-tick cooldowns. LP is 2560/2550/2550. They no longer use the 100-LP, zero-stat fallback.
- Corrected Grimspike from 160 to 1460 LP in both active packed data and editor XML.
- Replaced Graardor's empty/wrong-formula branches with real melee/ranged rolls, explicit 600/350 LP caps, matching typed damage and distinct animations. Removed the separate world-timer attack and its hand-written half-damage/full-reflection behavior. All hits now use the existing prayer/shield/absorption/reflection path.
- BasicBossAttack holds each pending hit's style, damage and completion state on its Interaction. It prevents replay, rejects remote melee and blocked projectile paths, and cancels on observed death, hiding, logout, plane/distance departure or instance-context change. It retains launch-time prayer evaluation; shields and absorption remain impact-time shared processing.

Only four packed NPC records changed. All other 13,484 records are byte-identical to the backup; the file still contains 13,488 sequential slots. The complete binary was not rebuilt from the possibly stale editor directory.

Access conveniences, teleport destinations, drops/rewards, godmode, personal XP and existing Summoning/Dungeoneering deferrals are preserved. No client or account-save changes.

## Historical confidence and retained limits

This is a functional first batch, not certification of complete encounters or every hidden 2011 stat.

- The [pre-EoC Supreme](https://wiki.darkan.org/Dagannoth_Supreme), [Prime](https://wiki.darkan.org/Dagannoth_Prime) and [Rex](https://wiki.darkan.org/Dagannoth_Rex) pages support their style/LP/max-hit reconstruction. These are later mirrors, not frozen early-2011 evidence; Supreme's 2560 versus commonly rounded 2550 is explicitly provisional. The [December 2012 implementation discussion](https://rune-server.org/threads/dagganoth-kings.463693/) corroborates those LP/cap values but is not an authoritative 2011 specification.
- King hidden levels and defence bonuses are a **provisional reconstruction**, not authenticated historical values. Supreme uses attack/strength/ranged/magic 255 and defence 128; Prime uses attack/strength/defence/magic 255; Rex uses attack/strength/defence 255 and magic 1. Unused ranged levels are 1. Defence bonuses in stab/slash/crush/magic/range order are Supreme 10/10/10/255/550, Prime 255/255/255/255/10, Rex 255/255/255/10/255. Other bonuses are zero. These restore their intended weaknesses; exact accuracy belongs to calibration.
- [Grimspike's pre-EoC record](https://wiki.darkan.org/Sergeant_Grimspike) supports 1460 LP. Other minion stats were not altered.
- The [period Graardor discussion](https://forum.tip.it/topic/211715-grasping-the-game/) supports the 60/35 old-hitpoint caps, represented here as 600/350 LP. Graardor retains his existing 3940 LP and five-tick cooldown; suspicious GWD HP/cadence values remain for the full encounter batch. The melee/ranged selection is provisionally 2:1.
- King animation IDs are native and cache-validated; Supreme uses projectile 475, Prime the native Water Wave projectile/end pair 162/163. Live visual identity/timing remains unverified. Graardor retains the existing ranged projectile/end 1200/1218; these also exist in the supplied cache.
- Graardor still pursues using melee reach, and the repaired ranged attack hits the selected victim only. Room-wide ranged damage, proper tank retention, and room-owned minion respawns remain batch 3. Existing Bandos.tick random retargeting is unchanged.
- DK arena aggression/safespots, Prime splash targeting and individual respawn accuracy remain batch 4. The definition's respawn value is 60; generic NPC respawning still follows the existing hard-coded path.
- Missing/uncertain projectile data for other GWD followers, Kree/KQ and frost dragons remains explicit encounter work. This patch does not invent assets or register incomplete handlers for those bosses.
- Shared per-life generation is still batch 2. These new attacks cancel if death is observed while pending; a death-and-revival between observations without an instance revision is not comprehensively covered. Nearby same-plane ordinary-world teleport/re-entry also lacks a universal generation token. No claim of full encounter-reset safety is made.
- Existing shared deflect proc calibration is retained, including its known historical uncertainty.

## Validation and staging

Selected suite: tests/BossBatchOneRegression.java, **20,207 checks passed**. It checks actual packed loading and registry dispatch, unchanged-record preservation, exact spawn counts, native assets, real retained-spawn collision footprints, all new boss attack branches with seeded rolls, caps and HP loss, overlapping interactions, replay rejection, death/logout/hiding/plane/distance/context cancellation, prayer/godmode, Divine impact mitigation, blocked paths and the actual CombatAction lifecycle/projectile delay.

Negative control: compiled the backed-up BandosAction into an isolated overlay. The same suite rejects it with `Every attack owns damage 6260`, confirming the test detects the original empty branch. No old runtime was reinstated.

Compilation covered the three changed/new source families and the focused test against the current runtime dependencies, targeting Java 8. No shared formula, damage-manager, instance or unrelated gameplay source changed; unrelated suites and full-source compilation were therefore not required. Early fixture failures were unloaded synthetic collision and a floating-point expected-rounding error; both were corrected in the test.

Four runtime classes staged: BandosAction, BasicBossAttack, BasicBossAttack$Hit, DagannothKing. Build/logs/hash manifests: build/boss-batch1. Source/data/runtime backups: build/boss-batch1-before. Every staged class hash matches its compiled artifact. Existing data backups are preserved; no server process was restarted.

**Server restart required.** Then check ordinary gear with godmode off: one of each King, one main-chamber Corp, each King's animations/projectiles/weakness, Grimspike durability, and repeated Graardor melee/ranged damage with matching prayers and shields. Check corner movement and two-client presentation. Live client acceptance remains pending.
