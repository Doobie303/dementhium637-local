# Home area review and proposed layout

Latest planning direction: [Starfall Haven plan](docs/project/STARFALL_HAVEN_PLAN.md). The owner now prefers a spacious coastal home with existing assets reused wherever possible. That plan supersedes the compact-layout recommendation below; it does not implement or migrate the home. Preserve this original review as historical evidence.

2026-09-09. Original review and recommendation. The owner subsequently approved Neitiznot; implementation and acceptance status are recorded in [NEITIZNOT_HOME_IMPLEMENTATION.md](NEITIZNOT_HOME_IMPLEMENTATION.md). Findings below describe the pre-move server and original proposal.

## Recommendation

Use **Neitiznot's southern settlement around its existing bank** as the new home. Keep Edgeville as a directly accessible PvP destination. Use one compact public home rather than dividing a small population between several hubs. Lunar Isle is the strongest alternative if a magical atmosphere is preferred.

The proposed identity is a coastal adventurers' town: bank, supplies, preparation and departures grouped together, with existing buildings used for secondary services. Essential services should be visible or discoverable from the arrival area, aiming for a roughly 10–15 tile walk from banking to preparation/travel. This is a design target, not a measured route yet.

## Current home findings

- Mob.DEFAULT is `(2344,3691,0)`, at Piscatoris. Home teleport and `::home` use it; the home dialogue still has a historical `nardah` parameter name. Names/comments alone are not reliable location evidence.
- Custom bank booths occupy `(2328,3686–3692)`. Prayer restoration is at `(2340,3687)` and the curse-book altar at `(2335,3682)`. A substantial skillcape NPC cluster occupies the southwest area.
- The spawn file places TzHaar-Hur-Tel, Rewards trader, Shop assistant, Aubury, Mage of Zamorak, Roddeck, Town crier, Ghaslor, Vannaka and other services around this area. Many other merchants are near the Grand Exchange, while the Gambler is at `(3367,3266)`.
- The home is a collection of additions with different purposes. Moving every NPC into another long row would reproduce the same problem.
- An object commented as a clan-wars portal (36972) actually restores Prayer in the active click handler. Existing portal comments are not a reliable destination inventory.

## Candidate comparison

| Location | Design assessment | Main compromise |
| --- | --- | --- |
| **Neitiznot** | Best overall fit: coastal identity, compact bank anchor, buildings for secondary services and room to organize departure points. | Native gates, bridges and buildings need a camera/walking tour before exact placement. Keep core services south of the bridges. |
| **Lunar Isle** | Strong magical identity; appropriate for a portal-focused home. | Layout needs more care to avoid scattered services; no seal, quest or travel restrictions should be added. |
| **Edgeville** | Strongest immediate Wilderness association; familiar and convenient for a PvP-heavy community. | Less of a visual fresh start; I would use it as the PvP outpost rather than make it the whole server's identity. |
| **Reworked Piscatoris** | Lowest disruption; existing service layout can be improved. | Less likely to deliver the new-home feeling requested. |

Local cache inspection, in an isolated JVM, found actual terrain/object archives and successful landscape parsing for all four candidate windows. Neitiznot region `36,59` contains native bank chests 21301 at `(2334,3808)`, `(2335,3808)`, `(2338,3808)`, `(2339,3808)`, plus bank table 21358 at `(2334,3805)`. Lunar bank booths 16700 appear at `(2097–2099,3920)`; Edgeville bank booths 26972 also load. The active object handler recognizes bank chests/booths by name.

**This confirms local map availability, not rendered appearance or a tested arrival tile.** Coordinates above are object anchors, not recommended teleport landing tiles. Exact NPC/object footprints, gates, line of sight, routes and safe-area coverage require the placement pass and developer-client tour.

Evidence: `build/home-review/HomeMapReview.java` and `build/home-review/map-inspection.txt`. This helper only reads the cache/spawn file and creates an in-memory map. It was not staged. Terrain counts are not a quality score.

Geographic cross-checks: [Fremennik Isles guide](https://www.tip.it/runescape/pages/view/fremennik_isles.htm), [Neitiznot supplies](https://www.tip.it/runescape/shops/view/142), and [Lunar Isle guide, updated December 2011](https://runescape.salmoneus.net/locations/lunar-isle.html). Their present-day pages do not replace the local revision-637 cache as the placement authority. Design rankings are recommendations, not historical claims.

## Proposed service layout

These are functional zones, not surveyed compass positions.

| Zone | NPCs / objects | Purpose |
| --- | --- | --- |
| Arrival and bank | Existing bank chests; one guide/town crier; clear open arrival tiles | Players see where to bank and where to go next immediately. Keep NPCs off chest click tiles and main paths. |
| Equipment and supplies | Three specialists: melee gear, ranged gear/ammunition, magic gear/runes; one general supplies merchant for existing food/potions/tools | Reuse existing stock, prices and eligibility. Separate menus can expose existing shops without combining their economies. |
| Preparation | Prayer restoration altar; clearly separate prayer-book and spellbook choices; repair service if currently supported | Put repeat-use preparation beside banking. Do not silently add full healing, special-energy refill, overloads or new repair discounts. |
| Adventure departures | Three labelled travel objects: PvM, PvP, Activities; Vannaka nearby | Central access with understandable categories. Use labels/menu names as well as colour. |
| Social / rewards | Existing Rewards trader and Gambler in a nearby building or alcove; a route to the real Duel Arena | Keep social activity visible but clear of the bank. Reuse the existing wager UI and house; no new bank or wager authority. |
| Progression | Cape/achievement service in a side building | Prefer one cape-selection interface over nineteen NPCs in a row. Consolidation is proposed new UI work; preserve all existing cape requirements, prices and options. |

Likely starting population: about 10–12 visible service NPCs, with secondary menus or services inside buildings. The exact roster follows a stock/action inventory, so relocating an NPC does not accidentally remove another role. Existing IDs suitable for reuse include Aubury 553, Vannaka 1597, Rewards trader 9711, Gambler 2998 and Town crier 6135. Names/models are presentation candidates; their active handlers determine actual behavior.

## Travel structure

**PvM:** bosses, Slayer/training and dungeons. Expose existing GWD, Corp, KQ, Dagannoth Kings, Nex, KBD, tormented demons and frost destinations through their current safe arrival/admission routes. Barrows and Fight Caves can appear as highlighted encounter entries. Do not teleport directly into a phase controller or bypass the Fight Caves session entry facade. Preserve convenient access and current rewards.

**PvP:** Edgeville safe staging, existing PvP/Wilderness destinations and Duel Arena. Distinguish **safe destination**, **dangerous Wilderness**, and **staking** in the destination menu. A dangerous destination gets its risk warning before teleport; selecting the category itself should never place a player in combat. Keep Wilderness/Teleblock/activity checks and custom EP/reward rules. Home itself stays safe; do not add an invisible dangerous strip or make the Neitiznot bridges a PvP boundary.

**Activities:** working minigames, skilling travel and the existing Dungeoneering entrance where supported. List only verified working routes. New party tools or activities need their own implementation; a portal does not make unfinished content ready.

The Crucible, Bounty Board and Last Outpost remain the saved review-only concepts in SOLO_COMBAT_MINIGAME_IDEAS.md. They could later occupy an adventure-board area, but should not be presented as playable activities now. Summoning services and Dungeoneering internals remain unchanged/deferred.

A future favourite/last-destination action would reduce repeated menu navigation. It is optional UI work, not part of the current implementation. Avoid one portal per boss: three categories keep the town readable.

## Suggested implementation sequence after location selection

1. **Survey and preview.** Tour Neitiznot and Lunar Isle in the developer client. Select an arrival tile and exact placement sheet; inspect camera/roofs, bank routes, object clicks and two-player traffic. Keep the original home active while reviewing the proposal.
2. **Inventory current services.** Map every retained NPC action, shop, prayer/spellbook option, donor convenience and travel endpoint. Read the corresponding subsystem reports before touching integration. Decide whether to relocate or provide an additional access point; preserve original non-home services unless expressly superseded.
3. **Build the compact core.** Arrival, banking, existing supplies/preparation, Vannaka, reward access and three travel menus. Prefer the existing map and models; no new client cache or custom terrain is required for the initial concept.
4. **Migrate home references together.** Review Mob.DEFAULT callers, new-player arrival, death fallback, `::home`, home/house teleports and hard-coded object returns. Object cases 2274 and 38700 currently return directly to `(2344,3691)`; changing only Mob.DEFAULT would leave old-home routes. Preserve private-server conveniences and intentional encounter exits.
5. **Scoped validation and live tour.** Verify shop identity/stock, bank access, prayer/spellbook choices, travel restrictions, activity-specific admission, logout/death return, existing Gambler integration and two players. Use the normal source backup/Verify/Stage workflow for authorized Java changes; no unrelated combat-suite sweep. Publish only after the actual layout is reviewed.

Potential implementation trap: many object actions are keyed globally by object ID. A new home portal must be scoped to its intended placement or use a dedicated route; reusing an ID must not redirect every matching portal in the world. Native scenery should not be removed merely to create a large blank square.

No production changes were made during this review. The recommended next decision is **Neitiznot coastal hub versus Lunar Isle magical hub**, followed by an exact placement preview.
