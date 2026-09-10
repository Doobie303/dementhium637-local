# Equipment item Stats popup - 2026-09-08

## Cause and patch

The worn-item Stats handler only wrote the item name to 667:63, changed the Back label and showed 667:51. It never supplied the popup body. Inventory interface 670 had no Stats handler.

The matching local client and cache show that equipment on-load script 787 registers string-change script 2782 for client strings 321-325. Script 2782 dynamically creates and sizes the popup from these strings; 321 is the title, 322/323 are optional upper fields, 324 is the body, and 325 is an optional lower field. Script 2947 clears these strings and hides the popup and its inventory blockers. The native Back script 2783 calls 2947.

`EquipmentItemStats` supplies all five strings, using 324 for a single readable list of attack, defence, absorption, strength, ranged strength, prayer and magic-damage bonuses. Values come from the selected item's effective server definition, including existing Barrows degradation mapping and verified ranged-ammunition strength corrections. Inspection does not change equipment, combat calculations or bonuses. The title retains the actual selected item's name.

ActionButtonHandler routes Stats (opcode 73) from worn equipment 667:7 and equipment-screen inventory 670:0 to this helper. Inventory slot/ID validation prevents invalid and stale clicks. Non-equipment and noted items close old details and report that they have no equipment stats. Back uses native cleanup, preserving bank-return state.

The helper invokes scripts through the verified opcode-16 `sendClientScript` path. It deliberately does not use `sendBlankClientScript`, whose legacy opcode-98 packet does not match this client's script packet. That unrelated global helper was not changed.

## Validation and runtime

Java 8-target compilation passed. EquipmentAbsorptionRegression passes **238 checks**, covering individual Torva bonuses/absorption, worn Barrows, inventory armour, rune-arrow intrinsic strength, food, invalid/stale item clicks, native script packets, Back cleanup and bank lifecycles in display modes 0-3. Cache-script inspection is recorded in `build/item-stats-scripts.txt`.

Production classes are staged in `bin`, with old runtime/source backups under `build/item-stats-before` and SHA-256 staging verification in `build/item-stats-staging.json`. Test output: `build/item-stats-test.log`. Restart the server to load the changes; no restart or player-save edit was performed.

In-client acceptance remains: inspect worn Torva and an inventory weapon, return with Back, inspect another item, then check a food item. Repeat when entering equipment from the bank and confirm bank return/minimap cleanup. Check that the generated popup fits in fixed and resizable mode; automated tests verify data and packets rather than final client rendering.
