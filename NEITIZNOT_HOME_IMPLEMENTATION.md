# Neitiznot home implementation

Later design decision: [Starfall Haven plan](docs/project/STARFALL_HAVEN_PLAN.md) records the owner-endorsed spacious replacement concept and priority on asset reuse. It is planning only. The implementation, evidence and pending acceptance recorded below remain the Neitiznot baseline; no Starfall migration occurred with that documentation update.

2026-09-09. Owner approved moving the home to Neitiznot. Implemented and staged: 30 classes across eight source families, with verified backups. This supersedes the review-only status in HOME_AREA_REVIEW.md. No server restart has been performed; client rendering and live multiplayer acceptance remain pending.

## Implemented layout

New home/arrival: **(2337,3803,0)**. Four native bank chests at (2334/2335/2338/2339,3808,0) remain in place. Native scenery and existing NPCs are retained. Added services are stationary and registered once during normal NPC loading; added objects use the existing custom-object broadcast system.

| Service | NPC | Coordinate (plane 0) | Behaviour |
| --- | --- | --- | --- |
| Guide / Roddeck | 8863 | 2336,3802 | Home guide and all three travel menus |
| Melee / Horvik | 549 | 2340,3802 | Existing shops 549,551,552 |
| Ranged / Lowe | 550 | 2341,3805 | Existing shops 550,682,683 |
| Magic / Aubury | 553 | 2343,3806 | Existing shops 553,546,461 |
| Supplies | 520 | 2343,3808 | Existing shops 520,521,519 |
| Slayer / Vannaka | 1597 | 2337,3810 | Existing Slayer dialogue |
| Rewards trader | 9711 | 2339,3810 | Existing rewards shop and Dungeoneering/Frost access dialogue |
| Gambler | 2998 | 2331,3810 | Existing wager UI, house funds and durable recovery |
| Tools / Bob | 519 | 2330,3806 | Existing tools shop and repair dialogue |
| Skilling / Mage | 1513 | 2344,3802 | Existing skilling destination dialogue |

West portals, south to north: PvM 2465 at (2331,3803), PvP 2466 at (2331,3805), Activities 2467 at (2331,3807). They retain native object names/appearance; opening one gives its titled destination menu. The guide provides the same menus. Preparation altar 409 at (2344,3804), footprint 1x2, restores Prayer or changes standard/ancient/lunar spellbook and prayers/curses. It does not replenish HP or special attack energy.

PvM: God Wars entrance, Nex entrance, Corp entrance, KQ, KBD, tormented demons, Barrows, Fight Caves entrance, training dungeon, Taverley dungeon, rock crabs. PvP: Edgeville staging, Duel Arena, Varrock Wilderness, Chaos Elemental, lava maze, safe free-for-all, dangerous PvP island. Activities: Fight Caves, Barrows, Castle Wars lobby, Puro-Puro, Daemonheim, legacy services/skillcapes, Grand Exchange shops, Warriors Guild. The Mage retains the existing skilling menu.

All destinations are in HomeHub.java. Menus paginate three destinations at a time. Dangerous PvP choices require confirmation. Owner-scoped menus reject remote, expired, closed, logged-out, dead, locked or instance-transition selections. Teleblock and activity restrictions remain enforced. New travel resolves immediately after validation. No new quest, travel or killcount restriction was introduced. Existing Frost access remains through the rewards trader with its existing level check.

## Compatibility and findings

- `Mob.DEFAULT` changes new-player spawn, ordinary non-Wilderness death return and existing home teleports. Saved accounts keep their saved locations until they travel. Existing object return cases 2274/38700 and the tutorial home camera/returns now use Neitiznot.
- Original services at Piscatoris and the Grand Exchange remain accessible, including skillcapes and lesser-used conveniences. No stock, currency, price, reward, PvP timer or EP policy was changed. No packed cache or client update is required by this implementation.
- The home Gambler is an additional approved stand, identified by actual world registration, NPC ID and exact anchor. It uses the original shared house and journal flow. The original Duel Arena stand remains valid. No production account, house or journal files were modified by testing.
- Native map inspection found old Fight Caves and safe-FFA arrival anchors on solid objects. New portal landings use clear nearby tiles (2438,5169,0) and (2815,5513,0). Existing encounter exit/respawn coordinates outside this home scope were not changed.
- The old dialogue calls (3807,2975,0) a safe PvP island, but active server area rules classify it as dangerous. The new menu labels it dangerous and requires confirmation; area/death/reward rules are unchanged.
- Exact footprint and path checks passed on the local revision-637 cache. NPC click approaches from arrival and cardinal approaches to each service/bank/object were checked. These do not certify roof visibility, camera composition, congestion or client interaction timing.

## Validation and release

Batch: `tools/batches/neitiznot-home.json`. Eight production source families: HomeHub, Mob, NPCLoader, ObjectManager, ObjectPacketHandler, DialogueManager, GamblerSession and TutorialScene. Unrelated source edits were preserved.

Selected tests:

- HomeHubRegression: real-cache footprints, arrivals and destination danger classification; pathfinding; native NPC first-click options; bank/object dispatch; actual dialogue packets; shop identities; book changes; independent menus; stale/closed/remote/locked/instance-transition/teleblocked choices; fresh-player home; existing return objects; home Gambler settlement with conservation.
- GamblerRegression: original stand and complete existing wager/recovery/house behaviour. Financial fixtures use newly created isolated directories under build/gambler.

Final Verify passed both suites: HomeHubRegression 1.613 seconds (218 checks), GamblerRegression 1.561 seconds (10,384 checks). Test execution total 3.174 seconds; compilation 2.622 seconds; complete verification including hash work 25.482 seconds. Logs and summary: `build/batches/neitiznot-home/verify-8969a0947cee45c9aa8f63742ef002bf/`. Earlier diagnostic attempts are retained, including the blocked landing and PvP-label findings and fixture correction for the post-teleport movement lock.

Stage succeeded for 30 classes. Receipt: `build/batches/neitiznot-home/staged.json`; source snapshots and exact runtime backups: `build/batches/neitiznot-home/before/source/` and `before/runtime/`. The batch tool verified input/dependency/output hashes, recorded previous absence for newly added classes, and checked staged SHA-256 hashes. No restart occurred. No full gameplay sweep was selected: no combat formula or encounter lifecycle was changed.

## Live acceptance after restart

Use `::home` in the developer client after a controlled server restart. Check arrival/camera/roofs; walk to all four chests, ten NPCs, three portals and the altar; try first-click and Trade options; buy/sell and repair through the existing services. Confirm books/Prayer and home returns. Try each destination and return home, with particular attention to Fight Caves entry, safe FFA and dangerous PvP confirmation. Use two players for simultaneous menus, shops and a wager at each Gambler stand, checking shared house behaviour and ordinary reconnects. Confirm old skillcape/services access. Live checks and restarting were not automated or claimed complete.

## Handoff

- Authorized scope: implement Neitiznot as the new home with practical service/travel access.
- Resume context: this report and HOME_AREA_REVIEW.md; Gambler report only for wager-specific changes.
- Implementation verified and staged; next acceptance is a developer-client tour after restart. Automated results establish the contracts listed above, not complete live gameplay acceptance.
- Keep existing economies, convenience access and durable journals. Summoning/Dungeoneering overhaul, branding, skillcape consolidation and unrelated historical fixes remain outside this batch.
