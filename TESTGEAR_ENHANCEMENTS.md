# Testgear enhancement

User requested a more useful private-server testing bank, including familiars.

- ::testgear and ::gearbank retain the existing million-item restock behavior and combat bank tabs.
- Summoning tab 9 stocks all 78 registered pouch entries and the scroll registry (68 entries, 67 distinct IDs), plus empty pouches, spirit shards and four charm types.
- Potion tab includes Summoning potions, Sanfew serum, super antifire and the individual extreme potions.
- Added basic rune/dragon melee options, recoil ring, anti-dragon shield, utility jewellery, spade/rope/tools, Karil's bolt racks, additional enchanted bolts, missing runes and chinchompas.
- Entries are deduplicated within a category and checked against item definitions. New entries are skipped when no bank slots remain, with a message. Existing stocks and tabs are preserved. Normal skill requirements are unchanged.
- The generic Container.getFreeSlot returns zero when full; this command uses getFreeSlots instead to avoid overwrites. Broader bank handling was not changed.
- This stocks registered familiar test items; it does not certify or implement familiar behavior. Existing SummoningScroll registry lists Mantis Strike and Slime Spray with the same ID (12459); that underlying mapping was not changed here.

Validation: Java 8-targeted build passed. TestGearRegression executes both aliases, checks every registry entry, key utilities, repeat restocking/tab stability, and preserves a completely full bank. Clean test bank uses 377 kit slots (378 including a pre-existing test item). Cache confirms Pack yak 12093, Unicorn stallion 12039, Steel titan 12790, Spirit terrorbird 12007, Summoning potion (4) 12140, Super antifire (4) 15304.

Commands runtime family staged in bin and hash-verified. Prior source/runtime backed up under build/testgear-before. Restart the server and rerun ::testgear to stock the additions.