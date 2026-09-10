# Equipment absorption and stats-screen fixes - 2026-09-07

## Findings and scope

The screenshot shows Primal armour. Daemonheim armour did not provide damage soaking, so its 0% entries are expected for the early-2011 target. Allowing Primal outside Daemonheim remains a private-server customization; no extra absorption was invented for it.

There were genuine packed-data faults: the three Dungeoneering reward shields and rune platebody/full helm had no absorption, Torva platebody had [11,5,19] instead of [6,0,12], and Virtus top had [8,4,0] instead of [12,6,0]. Usable Nex item variants also had zero absorption. This is a correction of a verified subset, not certification of all 20,430 item definitions.

`EquipmentAbsorption.apply()` runs after packed definitions load and corrects only absorption arrays for 46 explicit item IDs: standard rune full helm and platebody; all six Barrows armour sets' pristine pieces; Bandos chestplate/tassets; Armadyl helmet/chestplate/chainskirt; all nine Nex armour pieces in pristine and usable untradeable forms; and the three reward shields. Existing Barrows combat-ID resolution carries the corrected values through the four usable wear stages. Broken gear is not upgraded. The binary item file and all non-absorption bonuses are untouched.

Values below are percentages ordered **melee / magic / ranged**:

| Equipment | Head | Body | Legs |
| --- | --- | --- | --- |
| Torva | 3/0/6 | 6/0/12 | 4/0/8 |
| Pernix | 0/6/3 | 0/12/6 | 0/8/4 |
| Virtus | 6/3/0 | 12/6/0 | 8/4/0 |
| Dharok, Guthan, Torag, Verac | 2/0/5 | 5/0/10 | 3/0/7 |
| Ahrim | 5/2/0 | 10/5/0 | 7/3/0 |
| Karil | 0/7/3 | 0/10/5 | 0/7/3 |
| Armadyl | 0/5/2 | 0/10/5 | 0/7/3 |
| Bandos | n/a | 4/0/9 | 3/0/6 |

Chaotic kiteshield: 7/0/14; eagle-eye: 0/14/7; farseer: 14/7/0. Standard rune full helm: 1/0/3; platebody: 3/0/6. Primal remains 0/0/0.

Both the equipment screen and shared incoming damage consume these definitions. For example, full Torva shows 13% melee and 26% ranged absorption. A 500-LP melee hit soaks 39 LP from the portion above 200, dealing 461 LP.

## Equipment-screen fixes

All equipment-stats opening paths now use `Bonuses.openEquipmentScreen`. Opening stops the queued walk/run, following and active attack target, closes the previous modal for normal equipment-tab entry, and recalculates current equipment totals. Run toggle, freezes, forced-movement state and godmode are not cleared. Bank entry retains its bank state and inventory. Item-information visibility is reset when reopening.

Display mode 3 now opens and closes the same inventory overlay as mode 2. Existing fixed/resizable parent IDs remain unchanged. Empty or invalid equipment-item IDs no longer cause slot/index errors on interface buttons, and stale item-information clicks are ignored.

The reported visual glitch still needs an in-client acceptance check after restart; the movement and packet behavior is regression-tested. No client files changed.

## Sources and confidence

- [Mirrored Jagex Knowledge Base: Dungeoneering rewards](https://www.2011.rs/kb/dungeoneering_rewards): three shield values.
- [Mirrored Jagex Knowledge Base: melee special armour](https://www.2011.rs/kb/melee_third_age_equipment): Bandos and Torva values.
- [Mirrored Jagex Knowledge Base: Barrows rewards](https://www.2011.rs/kb/barrows_rewards_page): all six sets, including Karil coif's 7% magic soaking.
- [Mirrored Jagex Knowledge Base: more ranged armour](https://www.2011.rs/kb/ranged_more_ranged_armour): Armadyl and Pernix.
- [Mirrored Jagex Knowledge Base: more mage items](https://www.2011.rs/kb/magic_more_mage_items): Virtus.
- [Contemporary December 2010 equipment-screen observations](https://forum.tip.it/topic/282551-14-dec-2010-damage-soaking-and-new-hitsplats/page/5/): rune full helm and platebody. Lower confidence than original Jagex tables.
- [Historical Daemonheim equipment reference](https://wiki.darkan.org/Dungeoneering/Armour): Daemonheim armour's absence of soaking. This is a later pre-EoC snapshot, not an independently dated January-2011 capture.

These mirrors are supporting historical evidence, not a guarantee that every page is an exact revision-637 snapshot. Other standard armour pieces, cosmetic duplicates, PvP sets and remaining packed data still need a separate sourced pass; unknown values were not guessed from tiers.

## Validation and runtime

Java 8-target compilation passed. EquipmentAbsorptionRegression: 102 checks passed, including real worn Torva damage/HP/hitsplats, Barrows worn versus broken gear, Primal zeroes, stale item clicks, bank state, and screen/movement behavior in display modes 0-3. Existing CombatFoundationRegression: 117 passed. CombatBalanceRegression: 48 passed, including 200,000 roll samples. Total: 267 checks.

Runtime classes are staged in bin after backup and SHA-256 comparison. Restart the server to load them. Source and prior runtime backups are under build/equipment-absorption-before; compiled verification output is under build/equipment-absorption. No server restart or player-save edits were performed.

After restart: run across the room and open equipment stats; verify you stop and both panels remain stable, then walk again to close them. Repeat from the bank and in resizable mode. Try full Torva (13/0/26) or a chaotic shield alone (7/0/14). Primal alone should still display 0/0/0. Preserve godmode and the existing private-server settings; Summoning has not been changed.