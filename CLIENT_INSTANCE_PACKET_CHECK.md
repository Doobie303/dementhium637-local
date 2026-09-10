# Client instance-packet compatibility check

Completed 2026-09-08 after the owner supplied the runnable and decompiled DyNamic 639 client. This checks the existing instance protocol; it does not implement step 5 or 6.

## Result

**PASS: 26 server-generated packet cases, 32,932 assertions and five deliberately corrupted negative controls. No packet-format mismatch was found in the covered cases.**

The current server's DynamicMapPacket and DefaultGameEncoder were compiled into an isolated test build. The test exercised the actual client JAR's byte/bit readers, viewport-size table and opcode metadata, together with the verbatim decompiled instance-map decoder method. Both client folders remained unchanged. No client application or live server was launched, no existing player save was touched, and no runtime classes were staged into `bin`.

## Client identity and method

- Runnable client: `C:/Users/Tcarn/Desktop/DyNamic's 639/DyNamic-local.jar`.
- Decompilation archive: `C:/Users/Tcarn/Desktop/DyNamic's 639 - Decompiled/binary/DyNamic-local.jar`.
- Both JARs have SHA-256 `5A5A4D1ED4B45E02A08EB2EBF392F01EDD1BD80A1C85DA8AEA1DDF92A7486587`.
- `Class150.aClass58_1212` registers incoming opcode 31 with variable-short length (-2). The test reads this registration from the binary JAR.
- `PacketParser.java` dispatches it to `Class98_Sub36.method1459(-1048016408)`.
- The build script extracts that complete method from the decompiled source unchanged, then compiles it inside a test environment. Packet reads use the unmodified binary `Class98_Sub22` and `Class98_Sub22_Sub1` classes. Viewport sizes come from the binary `Class246_Sub3_Sub4_Sub5.anIntArray6265` table.

The test replaces graphics/scene reset, archive lookup and scene publication with stubs that record calls and outputs. It does not execute the entire original binary packet handler or initialize its renderer. The matching archived JAR and recorded source hashes establish which supplied artifacts were used; the decompiled method itself has not been formally proven equivalent to the binary handler.

## Verified contract

| Field or behavior | Observed client/server agreement |
|---|---|
| Frame | Opcode 31, two-byte big-endian payload length. Actual server encoder output is read by the binary client's readers. |
| Header, seven bytes | Byte-A mode 1, Byte-A viewport index, Short-A centre chunk Y, little-endian short centre chunk X, Byte-A force-rebuild flag 1. |
| Viewports | 104, 120, 136 and 168 tiles; 13, 15, 17 and 21 chunks on each axis. |
| Chunk ordering | Plane, X, then Y. Four destination planes are decoded. |
| Chunk presence | One presence bit; absent chunks become -1 in the client. |
| Chunk mapping | 26 bits: source plane in bits 24-25, source X in 14-23, source Y in 3-13, rotation in 1-2. Server rotation remains zero. |
| Alignment | Client finishes bit access at the same byte boundary as the server. |
| Map keys | Four big-endian signed integers per distinct source region, in first-discovery order across the chunk grid. |
| Source archives | Correct `m`, `l`, `um` and `ul` names are requested for each decoded source region. Archive requests are recorded, not loaded. |
| Completion | Exact payload consumption and one scene-transition call with the intended centre coordinates, force flag and state 11. |

## Cases

- Real Fight Caves source map, its entry location, map corners, adjacent blank chunks and shifted view centres, for every viewport size.
- Multiple source maps in one packet, repeated source regions on different destination planes and source-plane remapping.
- Replacing destination chunk mappings and decoding the resulting fresh packet.
- Real loaded map keys plus temporary in-memory sentinel words, including negative and mixed-byte values, to expose byte-order errors. The original in-memory key entries are restored afterward; key files are not edited.
- Deliberately swapped coordinate bytes, a changed key word, a flipped chunk-presence bit, truncated data and a trailing byte. All five were detected by decoder exceptions or independent expected-result assertions. This does not claim that the production client itself rejects every malformed packet gracefully.
- Test map allocations return to their starting count after cleanup.

Expected destination mappings are constructed from the fixture's explicit copy operations, independently of the server packet's chunk-reading logic. The decoder method is extracted from the client source rather than rewritten to mirror DynamicMapPacket.

## Reproduction and evidence

Run `tests/client/verify.ps1` from PowerShell. It accepts optional `ClientDirectory` and `DecompiledDirectory` arguments. The runner checks that the two supplied JARs still match and runs with Java 8 in headless mode.

- Regression: `tests/client/ClientInstancePacketRegression.java`.
- Decoder adapters: `tests/client/ClientMapDecoder.java.template`.
- Extracted/compiled decoder, payload fixtures, compiler log, regression log and hashes: `build/client-instance-packets/`.
- Artifact provenance: `build/client-instance-packets/provenance.json`.

This optional client-dependent suite is separate from the existing 13 server suites, so ordinary server verification does not require these external client directories.

## Remaining boundary

This substantially strengthens the evidence for packet 31 compatibility, but does not certify cache archive availability/decryption in the running client, landscape/object rendering, visual deletion replay, ordinary-map return rendering, animations, interfaces, live socket/login framing or gameplay. Scene effects and archive lookups were stubbed. Actual client visual acceptance remains necessary.

The next client-focused investigation would trace cache-backed scene construction and object updates using these decoded mappings; that work should retain the distinction between executable headless coverage and observed rendering.

## NPC-list semantic follow-up - 2026-09-08

The earlier packet-31 check verified encoding but stubbed the scene reset's effects. Live first-wave invisibility exposed an additional state contract: mode-1 map rebuilds clear the client's local NPC table, and the server must reset its per-player tracking as well. NpcUpdate/ActionSender now do so, including return to ordinary mode. See FIGHT_CAVES_INSTANCE_FIXES.md, section "Live acceptance fix: NPC list after map rebuild".

The separate tests/client/verify-npc-scenes.ps1 harness now passes 80 checks using the verbatim Class98_Sub10_Sub13.method1043, Class98_Sub39.method1468 and Class341.method3810 methods with actual binary bit readers and stubbed NPC/rendering effects. It reproduced the client's gnpov1 exception before the fix. This establishes the additional list-state contract, not full rendering or live-network validation. The fix is staged server-side; external clients/caches remain unchanged.
