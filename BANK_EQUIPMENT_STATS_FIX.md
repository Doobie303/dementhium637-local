# Bank equipment-stats lifecycle fix - 2026-09-08

## Latest follow-up: clientscript state and missing return control

The owner confirmed the latest bank/equipment lifecycle patch works in-game on 2026-09-08. The separate item Stats popup follow-up is recorded in EQUIPMENT_ITEM_STATS_FIX.md.

The earlier sections describe attempts that passed packet-only tests but did **not** resolve the owner's in-client report. The latest patch identifies additional client state from the local matching client and the server cache:

- Bank button 762:117 and return button 667:48 run clientscript 2318 **before** the server handles the click. It toggles varbit 8348. Script 2319 uses that value to hide bank root 762:0 and inventory root 763:0 while showing equipment root 667:0 and 763:1. Closing interfaces does not reset this varbit. Reopening 762 runs script 2317 -> 2319 and can hide the bank again, regardless of whether server attributes were cleared.
- Cache index 22 defines varbit 8348 as varp 638 bit 19. `Player.closeAll` now sends a varbit-only reset, including on fresh bank entry, walking and client-close cleanup. New `ActionSender.sendVarbit` uses verified revision-639 opcodes 38/112 and preserves unrelated bits in the parent varp.
- Return control **667:48** defaults to hidden. Scripts 787 and 2371 decide its visibility from varbit 4894 (varp 1248 bit 31). Bank-origin stats now sets that bit; ordinary stats clears it. Updating only children 49/50 could not reveal their hidden parent. After mounting the equipment panels, the server explicitly shows root 667:0 and sets parent 667:48's visibility for the entry context.
- `sendInterfaceConfig` has a misleading legacy parameter name: `true` sends zero, which the client treats as **visible**. Its global behavior was preserved.

The separate 667/670 equipment modal and fresh 762/763 bank reconstruction remain. No combat mechanics, equipment bonuses, item data, player saves or client files were changed.

Validation: Java 8-target compilation passed; `EquipmentAbsorptionRegression` passes **194 checks** across display modes 0-3. Added checks cover simulated client-local toggles, varbit-reset packets, bank-return config and parent visibility, minimap cleanup and stale-state bank recovery. The new suite fails against the previous runtime classes at the return-parent check. Cache inspection and script dumps are saved under `build/bank-ui-inspect.txt`, `build/bank-scripts.txt` and `build/bank-scripts-followup.txt`. These tests do not replace visual acceptance in the running client.

Latest runtime output: `build/bank-ui-lifecycle`; source/runtime backups: `build/bank-ui-lifecycle-before`; test log: `build/bank-ui-lifecycle-test.log`. Compiled production classes are staged in `bin` with SHA-256 verification. **Restart the server** to load them. Then verify bank -> equipment -> return, bank -> equipment -> minimap walk -> reopen bank, and equipment close -> reopen, in fixed and resizable modes. Ordinary equipment-tab entry should have no bank-return control. No server restart was performed by this patch.

## Reported bug

After opening **Show Equipment Stats** from the bank, leaving the interface by walking could make a later bank interaction reopen/glitch the equipment screen instead of opening the bank. Logging out cleared the stale session state and was the only known workaround.

## Initial hardening

`fromBank` is a temporary marker used only so interface 667's Back button can return to interface 762. Several unrelated bank-entry paths also consulted that marker and reopened interface 667 when it was present. Those paths were hardened so a new bank interaction always opens interface 762.

Bank booths, bank chests, banker second-click options and banker-dialogue access now always call `Bank.openBank()`. That method closes the previous modal and clears the temporary state before opening a fresh bank interface. Only the equipment screen's Back button consumes `fromBank` now.

Closing bank-origin equipment stats with its own close button now calls the normal `Player.closeAll` cleanup. This closes both interface panels and clears `inBank`, `fromBank` and `bankScreen`. Walking continues to use the same cleanup path.

## Minimap-close root cause and fix

In-client retesting showed that the first hardening did not resolve minimap walking. The screenshot exposed a retained bank inventory-side panel after the main equipment panel closed.

The revision-639 client sends zero-payload opcode 32 when its interfaces are closed client-side. `DefaultGameDecoder` previously required packet payloads to be greater than zero bytes, so it consumed and silently discarded this valid packet. The server also had no opcode-32 handler. As a result, the client removed interface 667 while the server retained the bank equipment/inventory session.

The decoder now emits valid zero-length packets. Opcode 32 is registered to `CloseInterfaceHandler`, which runs the normal interface cleanup and closes the retained inventory panel. The other defined zero-length protocol packets (12 and 75) are registered as no-op miscellaneous packets so enabling zero-length decoding does not create unhandled-packet noise.

## Bank-panel isolation

A second in-client retest showed the orphaned panel could still occur. The remaining structural problem was in `Bonuses.openEquipmentScreen(true)`: it deliberately kept bank inventory interface 763 mounted while replacing only the main bank interface with equipment stats. That made the two screens one shared client lifecycle, and a local minimap close could leave 763 occupying the inventory-interface slot. Reopening the bank then tried to attach 763 to a slot already containing the same child, producing the screenshot's inventory-only state.

Bank-origin equipment stats is now a clean modal transition. It closes both bank panels and ends `inBank`/`bankScreen`, retains only the temporary `fromBank` return marker, and opens the standard equipment pair (667 main plus 670 inventory). The Back button consumes that marker and performs a fresh `Bank.openBank()` for 762 plus 763. Walking or the equipment close button can therefore clean up an ordinary equipment modal; bank interface 763 is never shared across the transition or left mounted behind stats.

## Validation and runtime

`EquipmentAbsorptionRegression` now covers decoding and dispatching the zero-length client-close packet, full bank-panel teardown on the equipment transition, the standard 667/670 equipment pair, the Back-button bank reconstruction, bank -> equipment stats -> walking cleanup -> bank reopen, stale-marker recovery and the equipment-screen close button in display modes 0 through 3. It passes 162 checks. Java 8-target compilation passed.

Runtime classes are staged in `bin`, with their previous versions backed up under `build/bank-equipment-stats-before`. Restart the server to load the fix. After restart, repeat the reported sequence with both a bank booth/chest and a banker; the bank should reopen normally without relogging.
