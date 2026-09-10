# Client editability review — 2026-09-08

## Conclusion

The inspected clients support a credible route to custom items, changed maps and new areas. This is not yet a verified end-to-end content authoring workflow. The current DyNamic decompilation cannot be rebuilt as a whole without repairs; a separate source tree on this machine does compile and is a candidate development base. Correctly packed cache changes using existing supported formats generally do not require rebuilding the client executable.

This review did not edit the external client folders, caches, server gameplay sources or runtime bin. No client or server was launched. Compilation output and logs are isolated under `build/client-editability-review/`. This review does not advance instance steps 5 or 6.

## Inspected clients and build evidence

- Current runnable client: `C:\Users\Tcarn\Desktop\DyNamic's 639`. `Run.bat` launches `DyNamic-local.jar` using Java 8.
- Matching decompilation: `C:\Users\Tcarn\Desktop\DyNamic's 639 - Decompiled`. Its `binary/DyNamic-local.jar` matches the runnable JAR: SHA-256 `5A5A4D1ED4B45E02A08EB2EBF392F01EDD1BD80A1C85DA8AEA1DDF92A7486587`.
- Additional candidate source: `C:\Users\Tcarn\Desktop\639\Client`. This includes source, dependency JARs, models, a cache and old build scripts.

All compilation checks used the installed javac with `--release 8`; they did not execute compiled client code.

| Check | Result | Evidence under build/client-editability-review |
| --- | --- | --- |
| Full DyNamic decompilation, 833 Java files | Failed: 262 errors reported, first 200 printed | full-compile.args, full-compile.log |
| DyNamic RunClient, compiled against current JAR | Passed | RunClient.log |
| DyNamic Class316 cache-path resolver | Passed | Class316.log |
| DyNamic Class205 item loader | Passed | Class205.log |
| DyNamic Class297 item definition | Failed: four type errors | Class297.log |
| DyNamic Class352 object definition | Failed: two type errors | Class352.log |
| DyNamic Class302 object loader | Failed: malformed decompiled literal | Class302.log |
| Alternate source, including module-info.java, targeting Java 8 | Failed: module descriptor requires Java 9+ | alternate-compile.log |
| Alternate source, excluding module-info.java from compile arguments | Passed: all other 833 Java files; seven warnings | alternate-java8.args, alternate-java8.log |

The full DyNamic failures include invalid character literals and impossible decompiler-generated GOTO syntax. The reported error count is not a complete repair estimate: parsing failures may conceal additional errors. Individual compilation proves a limited replacement-class build is possible, not that a patched JAR has been packaged or successfully launched.

The alternate source has differences that prevent treating it as a drop-in replacement. Its launcher defaults to `www.neos639.com`, whereas the current DyNamic launcher defaults to localhost. Its renamed `Item.java` contains custom model overrides. Endpoints, cache paths, resources/native libraries, gameplay customizations and packet compatibility need comparison before adopting it. Folder names alone are insufficient to establish protocol compatibility.

## What future content changes require

| Requested change | Required work |
| --- | --- |
| Add or change an item | Cache item definition and applicable inventory/equipped models, textures and animations; matching server name/stats/slot/actions/effects and equipment mapping |
| Place existing scenery or NPCs | Often server placement/spawn/content changes using existing client assets; persistent map edits require landscape archive updates |
| Change terrain or build a new map area | Terrain and landscape authoring/repacking, archive references and applicable landscape XTEAs; matching server collision/pathfinding and entrances/content |
| Reuse an existing area privately | Managed instances copy/rearrange existing map chunks; this does not author new terrain or visual assets |
| Add a new client interface, rendering feature or unsupported asset format | Client code changes, resource packaging and runtime validation; format conversion may be preferable for imported assets |

Assets from a different game revision cannot be assumed compatible with these model, texture, skeleton or animation loaders.

## Cache and server constraints

The server's authoritative cache path is `data/cache/` (`src/org/dementhium/cache/CacheConstants.java`). Relevant indices include maps 5, models 7, sprites 8, object definitions 16, NPC definitions 18 and item definitions 19. The client loads item/object definitions and models through cache archives; item count is derived from archive contents rather than one fixed client list.

No compatible, working item/map cache authoring and repacking tool was demonstrated in the inspected client directories. Existing server definition editors are not evidence of a terrain/model cache editor. The server FileStore opens cache files for reading; CacheManager reads reference tables and generates update checksums. A content pipeline must correctly rebuild archive/reference metadata, counts, checksums and revisions. Cache writing and client redownload have not been tested.

The current client's Class316 resolver searches several locations, including `C:\` and the user's home, for `.dynamic pvp_cache_32` and `.file_store_32` directories. A plausible populated cache exists at `C:\.dynamic pvp_cache_32\runescape`; other desktop copies also exist. The resolver was inspected, not executed, so the cache used by a live process is not certified. Different cache sizes do not establish incompatibility because downloaded contents can differ. A future development setup should explicitly select its own cache location.

Server item definitions currently use `ItemDefinition.MAX_SIZE = 20430`; their binary serialization includes signed-short fields. New ID allocation must respect or deliberately extend every relevant limit. Wearable appearance also depends on equipment IDs: client Class313 builds an equipment mapping and writes equipids.txt, while the server stores equipId in its definitions. These mappings must agree. Arbitrarily assigning a new high item/model ID is not a verified workflow.

Server collision and visible client terrain/objects must come from compatible map data. Otherwise an area may render while movement, object interaction or pathfinding is incorrect.

## Recommended sequence when content work is authorized

1. Preserve the current working JAR/cache as the baseline and establish a separate development client/cache. Reproduce the isolated builds through a repeatable script.
2. Compare the buildable alternate source against current DyNamic behavior and dependencies. Decide whether to migrate the current customizations to it or repair the current decompilation. Do not replace the current client solely because the alternate compiles.
3. Establish and validate a cache authoring/repacking workflow for this cache format. First prove unchanged archives round-trip correctly and a small definition edit loads through the server update path.
4. Prove one test item, including inventory and equipped appearance plus matching server definitions/equipment IDs. Then prove a small test map with terrain, objects, collision and entry/exit behavior.
5. Use those validated examples as templates for larger content additions. Runtime rendering and interaction checks remain necessary before declaring production readiness.

The current client is not an established blocker for continuing the instance plan using existing maps. `CLIENT_INSTANCE_PACKET_CHECK.md` separately records 26 packet cases, 32,932 assertions and five negative controls against the current client's readers/decoder. That coverage does not validate the alternate client, rendering, real archive loading or live networking. Instance steps 5 and 6 and live acceptance remain outstanding.
