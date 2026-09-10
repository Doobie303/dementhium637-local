# Combat decisions for morning review - 2026-09-09

The overnight instruction was interpreted as working through steps 1-4 and stopping before step 5, as stated when work began. Concrete changes are recorded in COMBAT_OVERNIGHT_FIXES.md. This list is the work held for major decisions or further evidence; it is not a list of completed fixes.

## 1. Choose the exact historical baseline

Early-2011/revision 637 remains provisional. Do not introduce later-2011 equipment/spells or modern OSRS/RS3 formulas as an implicit upgrade. This decision affects several numbers below.

## 2. Historical curse state and proc calibration

Implemented: Sap no longer boosts its caster; first successful application drains 10%, individual Sap/Leech effects cap at 20%/25%, and Leech adds at most 5% dynamic self-boost on top of its existing 5% static boost. Per-caster cleanup no longer over-restores or subtracts unrelated effects. Invalid/departed targets are pruned. Sap Spirit now drains special energy without crediting it to the caster.

Still held: replacing the hit-counter/random activation cadence; applying the initial drain immediately on prayer activation; gradual residual recovery after switching off; cross-caster/overlapping-curse stacking policy; Turmoil warm-up/retention. These require a broader state model and dated specifications. Current deactivation still restores that curse's full recorded drain immediately. Deflect retains the legacy one-in-three probability; no precise period rate was established. Existing 70%/25% Elysian behavior is unchanged, and the owner's unexplained over-reduction is still not reproduced.

## 3. Bolt probabilities and formula balance

Implemented: Dragonstone protection now cancels the enchantment and falls back to an ordinary bolt instead of reducing the entire arrow as dragonfire; additional water-staff variants cancel Pearl's enchantment. Jade's stun and the new harmful callbacks wait for impact.

Still held: the common 25% enchanted-bolt gate; individual gem proc tables, Diamond's 0.55 defence multiplier, exact extra damage and fiery-target taxonomy; Ruby HP sampling timing; ordinary weapon poison proc rates and poison's existing 30-tick/8-hit progression. Weapon poison now runs once per actual damaging hit with launch equipment captured; moving from the previous guaranteed eligible application to a new probability was not guessed. Ranged Void's provisional 20%, Rigour's retained 20%, Dharok's current formula, exact accuracy rounding and hidden godsword/claw/Korasi multipliers are unchanged.

The non-ice status inventory is not exhaustive: standard Bind/Snare/Entangle retain their existing cast-time freeze paths. They still need a dedicated impact/cancellation pass; the Miasmic/Smoke/Shadow changes do not certify every spell.

## 4. NPC mutable stats and equipment exceptions

Player-worn Barrows effects were added, but Ahrim's stat drain currently targets players; Karil/Torag affect player-only Agility/run energy. Guthan works through actual player/NPC damage. The new set proc chance is a 1-in-4 reconstruction consistent with the existing brother implementation, not an independently authenticated 2011 probability. Verac bypass skips accuracy and protection for its proc while retaining immunity, shields and absorption; historical absorption ordering/minimum damage remains calibration work.

BGS NPC drains and additional NPC stat-changing specials remain held. They need per-NPC mutable levels, restoration and immunity rules without modifying shared packed definitions or accidentally changing Summoning. This also intersects the boss audit in step 5. Slayer and undead recognition was expanded with explicit examples, not certified for the whole NPC database.

## 5. Weapon reach and resource systems

`RangeWeaponData.xml` now supports an optional `<attackRange>` from 1 to 10; degraded variants inherit it, and the movement code adds two for longrange with a cap of ten. Existing entries omit this property and retain ten. A shorter range table was not populated from modern wiki values without period evidence. Global magic range fifteen and moving-melee chase allowances remain unchanged. Selecting and applying the dated range table is still needed; the new field alone does not finish reach calibration.

DFS charge storage/release, weapon-specific charge/degradation exceptions, Ava metal-armour restrictions and attractor/accumulator/alerter recovery/break distributions remain held. These can alter item persistence and ammunition economy. Existing ammo behavior was retained apart from making chinchompa consumption use the guarded one-shot debit.

Food/drink/potion timing and existing private-server restrictions remain unchanged. Ring-of-vigour cost coverage and later Staff-of-Light variants need a separately specified inventory. Launch stance/rate capture now covers ordinary melee/ranged and the shared special XP routes; legacy magic/Korasi XP semantics still need their own pass.

## 6. Remaining bespoke specials and live acceptance

The overnight batch fixes concrete lifecycle/resource defects in whip, granite maul, healing bows, hand cannon, rune throwing axe, Morrigan javelin, dragon hatchet and dragon scimitar, plus a working clipped spear push. It does not authenticate every special multiplier or animation. Morrigan throwing-axe run-energy behavior and the full novelty-weapon inventory still need specifications; no arbitrary behavior was added. Chained attacks retain their existing range and per-bounce energy policy. Healing bows retain existing pulse amounts/intervals, now using the actual ranged result. The spear push uses a one-tick forced movement and retains its five-tick stun; live presentation requires acceptance. Scimitar protection suppression uses eight ticks (4.8 seconds), an explicit rounding choice for its five-second description.

Verify the staged build after restart with godmode off: poison/slow timing, complete/broken Barrows sets, shield composition, two concurrent special effects, granite-maul double clicks, spear walls/boundaries, and leaving an instance during a pending effect. Capture full combat debug lines for Elysian. No server restart or live client validation was performed overnight.

## Step 5

Not started: boss-by-boss audits, broad packed item/NPC data certification, missing spell registrations and server scheduler/load changes. Summoning-specific work and Dungeoneering migration remain deferred. Existing custom PvP/EP/Teleblock policy, Barrows loot boosts, godmode, personal XP rates and custom items remain preserved.
