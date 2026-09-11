# OSRS equipment expansion and fit follow-up

The `osrs-equipment` server/client build adds Primordial boots (20431), Pegasian boots (20432), Eternal boots (20433), the Avernic defender (20434), the Max cape (20435), and the Infernal max cape (20436). These IDs continue after the existing custom Infernal cape (20430), and wearable mappings 5157–5162 continue after its mapping 5156 without replacing native 2011 definitions.

The server implements the current OSRS combat bonuses, weights, base values, tradeability, and wear requirements. The Max cape has +9 to all five defences and +4 Prayer; in this server it requires 99 in all 25 available skills. The Infernal max cape has the Infernal cape bonus vector, 0.453 kg weight, and the same all-99 server requirement. This build does not add OSRS Max-cape teleports/perks, item-combination flows, Cerberus/Theatre reward sources, broken-item states, or noted boot variants. `::osrsgear` is an administrator-only test grant.

The client uses the original OSRS inventory, male, and female source meshes. Boots and both capes come from OpenRS2 cache 426 (OSRS revision 144, 2017-06-15); the Avernic defender comes from cache 38 (OSRS revision 171, 2018-06-07). The Infernal max cape maps its original OSRS texture 59 to the existing lossless animated material 915. The new `|osrs-equipment=1|` capability prevents older clients from equipping/spawning these IDs and supplies dragon/ranger/infinity boots, dragon defender, Dungeoneering master cape, or Infernal/Fire cape appearance fallbacks to legacy viewers.

The initial `osrs-equipment` build passed 7,373 packaged item/model checks, 66,157 existing texture/collar checks, the packaged capability proof, and 164 focused server checks, then was published through the maintained launcher. Owner live review accepted the Avernic defender and identified that all three boot pairs crossed the floor plane and both max-cape variants needed the established upper-shoulder clearance.

The `osrs-equipment-fit` follow-up raises only the six boot wearer meshes by six original model units, bringing their measured maximum vertical coordinate from 6 to the floor plane at 0 without scaling them or changing animation weights. It applies the same bounded upper-back clearance already used by the Infernal cape only to the four Max/Infernal-max wearer meshes; inventory meshes, the lower cape silhouette, and the Avernic are unchanged. The build passes 17,143 cape/model checks and 66,157 texture/collar checks and is verified and published from `build/client-batches/osrs-equipment-fit` through the maintained launcher. Live male/female idle/walk/run acceptance remains required.

---

# Original-texture and Torva fit follow-up

This v6 update supersedes the v5 Fire Cape material adaptation described below. The owner explicitly requested replication of the original Infernal Cape appearance, reported good movement, and identified Torva as the armour in the fit screenshot.

Reopen the SAME `build/gambler-interface/dev-client/Run Dev Client.bat` launcher, now selecting Gambler-live-v6.jar. Existing capes update automatically. No server code/runtime classes changed in this follow-up, so no server restart is needed for the visuals.

- Original OSRS texture 59 uses sprite 318 from the same OpenRS2 cache 426 (revision 144). Its original 128x128 RGB pixels, dark pattern, average colour 3875, vertical animation direction 1 and speed 1 are retained. The material is isolated at new native texture slot 915; no existing material, including Fire Cape texture 40, is replaced. Native rendering lighting/gamma still apply, so pixel identity of the source does not certify identical final OSRS rasterization.
- Class260 serves the bundled pixels through the native integer/HDR texture APIs and appends one material. It retains the native renderer flags appropriate to an animated opaque cape. Native downward scrolling is used (Y speed -1 in this client's convention).
- Torva platebody models 62746/62762 were inspected against the cape's upper-back bounds. A conservative upper-back Z offset tapers from zero to six original model units (24 native units) over Y -130 to -150. X/Y coordinates, face topology and all animation skin groups are preserved; the inventory mesh is unchanged. This is a proposed armour-clearance improvement, not a certified no-clipping fit for every torso. Live Torva male/female and unarmoured acceptance remain needed. The original asset files remain unchanged.
- `Class98_Sub6` applies that adjustment only to the two bundled cape wearer models from the designated model archive. Other models are unchanged.
- The native item-definition data file now remaps to 915, matching the client definition. This cosmetic field is not used for server combat or equipment logic. Backups of changed existing client sources/build/launcher and item data are under build/infernal-visual-before. The v5 JAR is retained.

Validation: packaged v6 passes 66,157 texture/collar assertions (mostly exhaustive pixel checks), 6,037 cape/equipment checks, 81 Gambler widget checks and the packaged login capability proof. The texture test compares every RGB pixel against the original native-decoded sprite, checks flipped/HDR output and actual native software scrolling, and bounds both wearer geometry adjustments while preserving weights. Logs are under build/infernal-visual. These headless checks do not substitute for live rendering and armour-fit acceptance.

Asset provenance: https://archive.openrs2.org/caches/runescape/426/archives/9/groups/0.dat contains texture definition 59; https://archive.openrs2.org/caches/runescape/426/archives/8/groups/318.dat contains its sprite. `client/infernal-cape/assets/texture318.dat` is the decompressed sprite; texture318.rgb is a lossless big-endian integer RGB export, without recolouring. Existing archived geometry provenance remains in client/infernal-cape/README.md.

---

# Infernal Cape: admin testing build

Implemented in the existing Gambler development client. No second client installation, launcher, or cache was created.

## Test in game

1. Restart the server with no active duels to load the staged classes.
2. Close and reopen `build/gambler-interface/dev-client/Run Dev Client.bat`. The same launcher now selects `Gambler-live-v5.jar`.
3. Log in as an administrator and run `::infernalcape` (server item ID 20430), then wear it.
4. Check the inventory icon, male/female fit, idle/walk/run motion, animated lava, and equipment bonuses. Have an original-client player view the wearer as a compatibility check. Test the Gambler normally as well.

Admin grant is the only new acquisition route. No Inferno encounter or permanent reward source has been added. Normal players cannot use the grant command.

## Definition and appearance

The [OSRS Wiki Infernal cape](https://oldschool.runescape.wiki/w/Infernal_cape) supplies the reference stats: +4 stab/slash/crush attack, +1 magic/ranged attack, +12 all five combat defences, +8 melee strength, +2 prayer, no ranged strength or magic damage. Weight is 1.814 kg. The item is untradeable, unnoted and nonstackable. It adds no Summoning defence or 2011 absorption. Value is 80,000; high/low alchemy values 48,000/32,000.

The original OSRS inventory/male/female meshes are bundled without geometry changes. Item-level retexturing maps OSRS lava texture 59 to this client's existing animated Fire Cape lava texture 40, adapting the material to the 2011 renderer. This is an OSRS-shaped cape with a native 2011 material, not a pixel-identical reproduction of OSRS rendering. Live visual fit and animation acceptance remain outstanding.

The server uses item 20430 and wearable mapping 5156. The client appends the item after all native definitions, including lent variants; existing equipment IDs do not shift. Bundled model slots are 65000-65002. No cache archives are repacked. The server's native-format definition is `data/custom/infernal-cape/item.dat`; its bytes are compared with the client's generated definition by the packaged-client test.

The login settings marker `|infernal-cape=1|` identifies supported visuals and coexists with the Gambler marker. It grants no account privileges. Unsupported clients cannot spawn/equip the cape through these routes; wearer appearance packets use a Fire Cape substitute for unsupported viewers. This does not add custom inventory artwork to old clients, so use the updated development client for accounts holding this test item.

Existing server death/drop/recovery behavior remains in force. OSRS broken-cape/Perdu/Wilderness repair rules and Inferno completion requirements are not recreated in this admin test release. Combat formulae and the Summoning/Dungeoneering deferrals are unchanged.

## Verification and staging

- 6,037 packaged-client cape checks: actual binary item/model loaders, both wearer meshes and skin groups, native lava mapping, all 5,156 existing wearable mappings, model ID availability, resource isolation and server/client definition equality.
- 36 server checks: bonus definition and equipped aggregation, flags, admin-only grant, unsupported-client rejection, both gender appearance packets for capable/legacy viewers, and equipped save/reload. Storage is isolated under build/infernal-cape/test-accounts-*.
- 81 Gambler client checks and packaged login-string capability proof pass with v5.
- 10,384 Gambler checks, 77 custom interface integration checks and 17 automatic-selection checks pass with the cape server classes.

Java 8-targeted compilation passes. Logs, changed source list and staged hash manifest are under `build/infernal-cape`. Runtime backups are under `build/infernal-cape-before/bin`; source backups are alongside them. No running server was restarted and no real account saves or Gambler funds were modified. The original external client JAR is unchanged.

Build the same client with `client/gambler-interface/build.ps1`. Client cape source/assets and the binary proof are under `client/infernal-cape`. Rebuild server files listed in `build/infernal-cape/changed-sources.txt` against bin/lib, then stage with `build/infernal-cape/stage.ps1`.

Before downgrading to a server without the new item definition, remove or migrate test capes from accounts, banks and any saved recovery containers while the current runtime still understands ID 20430. Preserve unrelated durable duel/Gambler journals and their existing recovery requirements.


