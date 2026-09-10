# DyNamic PvP login branding

The existing development launcher now selects Gambler-live-v7.jar. Close and reopen `build/gambler-interface/dev-client/Run Dev Client.bat`; no server restart is required.

The generated Wilderness artwork includes the exact DyNamic PvP wordmark, metallic gold/stone lettering, crossed swords, ruined fortresses and subdued embers. Project artwork is stored in client/login-branding/assets/login.png and bundled into the client JAR. No external downloads occur at runtime.

The cached loginscreen name resolves to interface 744. Class215 draws the artwork behind this interface only, using native renderer sprites and a top-anchored proportional cover layout. Class85 suppresses the original decorative scene, tint, tiled background/frame and logo widgets (744 children 18,20,21,23). The username/password form (596), input scripts, actions, connections and login packets are unchanged. Lobby (906) and in-game screens remain outside the branding scope. Existing native settings/music controls are retained. The login scene may still be updated internally; this patch replaces presentation, not scene/network initialization.

208 packaged-client checks pass, including named interface lookup, actual native software rendering, login-only scope, decorative-only visibility changes and layout bounds. Native artwork rendering was visually inspected at 765x503; build/login-branding/native-background-preview.png contains the actual software-rendered background, without the login form. These checks do not certify live mouse/keyboard login, OpenGL rendering, every resizable layout or the full composed login form. Check the title, form readability, typing, Login, settings, entering the lobby/game and logout/reopen in the development client.

The packaged v7 also passes 66,157 texture/collar assertions, 6,037 cape assertions, 81 Gambler widget assertions and the launcher capability proof. No server source/classes, real accounts, caches or original external client were changed. Client source/build/launcher backups are in build/login-branding-before; v6 remains available for rollback. Build using client/gambler-interface/build.ps1.

Artwork generated for this project using the built-in image generation tool. The full source PNG is retained unchanged; native sprite scaling performs display sizing. This is a combined backdrop and logo asset, not a separate transparent logo pack.


## Known issues — deferred at owner's request

Live v7 feedback: the owner likes the new login artwork. They explicitly requested recording the following for a later patch, with no fixes now:

- **Lobby retains old RuneScape background.** After login, the Player Info lobby shows the original artwork around its panel. The supplied screenshot confirms this. Extend DyNamic PvP branding to the lobby background while preserving its controls.
- **Old artwork appears while loading the login screen.** The owner reports other original RuneScape pictures during startup/loading before the branded login screen appears. Identify those loading/transition artwork paths and replace them with consistent DyNamic PvP branding. Exact loading stages/assets have not yet been traced.

Future acceptance: check a cold developer-client launch through loading, login, lobby, entry into the game, and logout/return to login. The approved branding should remain consistent across these non-game screens without old artwork flashing during transitions. Keep native login/lobby controls functional and use the same Run Dev Client.bat launcher.

Status: recorded only; no code, client build, assets or runtime changed for this report. Resume implementation when the owner asks to address this backlog.
