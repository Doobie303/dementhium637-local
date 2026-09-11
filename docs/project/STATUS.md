# Status ledger

## Current state

- The broader NPC/combat Step 2 remains paused. The separately authorized Nex blood-phase repair is staged and accepted by the owner with godmode on; godmode-off fidelity acceptance remains pending. Do not release unrelated God Wars, Corp, or reward work without renewed direction.
- Owner live testing with godmode on confirms the repaired Nex blood phase and Saradomin behavior. Bandos's distinct smash and melee sequences were also observed; its remaining godmode-off, two-player damage, and active ranged-playback boundaries remain open.
- God Wars doorway and room reentry behavior is owner-confirmed fixed in live testing.
- NPC animation/overlap/arena-pursuit work and Piscatoris home are staged with remaining live acceptance. Starfall remains a paused concept; its asset prototype is tests-only, and its statue/native rendering/collision/cache-map work is unproven.
- The developer-client loading artwork is published through the maintained launcher. Cold startup, native crossfade/OpenGL, and composed login/input acceptance remain pending; lobby branding is deferred.
- The OSRS equipment expansion (Primordial, Pegasian and Eternal boots; Avernic defender; Max and Infernal max capes) is implemented, verified, and published through the maintained launcher. Owner live review accepted the Avernic but found boot floor and max-cape shoulder fit issues; the focused correction is verified and published from `build/client-batches/osrs-equipment-fit`, with live fit acceptance pending.

## Active blockers

- Fight Caves: the Tz-Kek retaliation hit icon is corrected in the working tree with isolated regression coverage; live visual acceptance remains pending. The ground-item refresh ghost is staged with restart and live drop/move/pickup acceptance pending. Local profiles for IDs 2734–2744 remain uncompiled, unstaged, and balance-unaccepted at the owner's request.
- Verify initial-login interface refresh: the player's name does not appear immediately after login, but appears after clicking the minimap, inventory, or another interface element.
- God Wars live attack/follower presentation remains pending. Corp's energy/HP/hop behavior is present in the local runtime and awaits owner live validation; ground-projectile timing is staged with isolated packet/scheduler coverage and awaits live visual acceptance.
- Verify Corporeal Beast projectile impact behavior: a projectile was observed striking the player and producing an explosion effect; confirm that the impact and visual are accurate to the 2011 encounter.
- Verify Dark Energy Core behavior: when spawned, it did not attack the player, followed the player, and moved underneath the player. Confirm its attack, pursuit, and collision/positioning behavior against the 2011 Corp encounter.
- Nex godmode-off testing needs further review before Fumus phase activates: attacks appeared unusually fast, and Blood Barrage was confirmed at least two or three times before that phase; whether this behavior is accurate still needs review.
- Verify Blood Reaver respawn timing: under godmode/high DPS, Reavers appeared to respawn very shortly after being killed. Confirm whether this is a defect or a timing-calibration issue against the 2011 Nex encounter, excluding intentional admin-only features such as godmode.
- Wider combat calibration remains open, including formulas, reach/equipment/resources, spell coverage, specials, and delayed callbacks. Elysian over-reduction remains unreproduced; Staff of Light is ruled out.

## Durable boundaries

- Completed audits and implemented repairs are historical evidence, not fresh defect lists; recheck current source before reopening them.
- Summoning-specific work and Dungeoneering migration remain deferred. Quest implementation and solo-minigame concepts remain review-only.
- Private friend testing is deferred with two approved options for later review: (1) restrict TCP 43594 to the tester's public IP at the router/Windows Firewall, which is simpler but requires that IP; or (2) add private access-key authentication to both the client and server, which does not require the tester's IP but needs implementation and validation on both sides. No hosting or network exposure is currently authorized.
