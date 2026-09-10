package org.dementhium.model.player;

import org.dementhium.model.Item;
import org.dementhium.model.definition.ItemDefinition;
import org.dementhium.net.ActionSender;

public final class Bonuses {

    public static final int STAB_ATTACK = 0, SLASH_ATTACK = 1, CRUSH_ATTACK = 2, MAGIC_ATTACK = 3, RANGED_ATTACK = 4, STAB_DEFENCE = 5, SLASH_DEFENCE = 6, CRUSH_DEFENCE = 7, MAGIC_DEFENCE = 8, RANGED_DEFENCE = 9, SUMMONING_DEFENCE = 10, STRENGTH = 11, RANGED = 12, PRAYER = 13, MAGIC = 14;

    public static final int MELEE_ABSORPTION = 0, MAGIC_ABSORPTION = 1, RANGE_ABSORPTION = 2;

    private int[] bonuses;
    private double weight;

    private final Player player;

    private int[] absorptionBonus;

    public Bonuses(Player player) {
        this.player = player;
    }

    public void calculate() { calculate(player); }

    public void calculate(Player victim) {
        bonuses = new int[15];
        for (int i = 0; i < Equipment.SIZE; i++) {
            Item equip = victim.getEquipment().get(i);
            if (equip != null) {
				ItemDefinition definition = combatDefinition(equip);
                for (int x = 0; x < 15; x++) {
                	
					bonuses[x] += x == RANGED ? RangedEquipmentStats.strength(definition, i, victim.getEquipment().get(Equipment.SLOT_WEAPON)) : definition.getBonus()[x];
                }
            }
        }
        absorptionBonus = new int[3];
        for (int i = 0; i < Equipment.SIZE; i++) {
            Item equip = victim.getEquipment().get(i);
            if (equip != null) {
				ItemDefinition definition = combatDefinition(equip);
                for (int x = 0; x < 3; x++) {
					absorptionBonus[x] += definition.getAbsorptionBonus()[x];
                }
            }
        }
        weight = 0.0;
        for (Item item : victim.getInventory().getContainer().toArray()) {
            if (item != null) {
				weight += combatDefinition(item).getWeight();
            }
        }
        for (Item item : victim.getEquipment().getContainer().toArray()) {
            if (item != null) {
				weight += combatDefinition(item).getWeight();
            }
        }
        refreshEquipScreen();
    }

    public int getBonus(int id) {
    	try {
            if (id < 0 || id >= bonuses.length) {
                return 0;
            }
            return bonuses[id];
    	} catch (NullPointerException e) {
    		return 0;
    	}
    }

    /** Open a stationary modal, recalculating from the equipment actually worn. */
    public void openEquipmentScreen(boolean fromBank) {
        player.getActionManager().stopAction();
        player.removeTick("following_mob");
        player.getCombatExecutor().reset(); // Also clears the queued walking/running path.
        player.stopAll();
        // Do not retain the bank inventory (763) behind this screen.  The
        // client can close interface 667 locally when minimap walking, which
        // used to orphan 763 and poison the next bank open.  Transition to a
        // self-contained equipment screen instead; only keep enough state for
        // the Back button to reopen the bank.
        player.closeAll(true, true);
        player.removeAttribute("itemPriceCheckId");
        if (fromBank) player.setAttribute("fromBank", Boolean.TRUE);
        // Varbit 4894 (1248 bit 31) enables the bank-return parent in
        // equipment on-load script 787 and config-change script 2371.
        ActionSender.sendConfig(player, 1248, 268435464 | (fromBank ? Integer.MIN_VALUE : 0));
        ActionSender.sendBConfig(player, 199, -1);
        ActionSender.sendAMask(player, 1538, 667, 7, 0, 15);
        ActionSender.sendAMask(player, 1538, 670, 0, 0, 28);
        ActionSender.sendBConfig(player, 779, 28);
        ActionSender.sendInterfaceConfig(player, 667, 49, fromBank);
        ActionSender.sendInterfaceConfig(player, 667, 50, fromBank);
        ActionSender.sendInterfaceConfig(player, 667, 51, false);
        calculate();
        ActionSender.sendInterface(player, 667);
        ActionSender.sendInventoryInterface(player, 670);
        // sendInterfaceConfig's legacy boolean means visible on the wire.
        // Set the root and return parent after the interface's on-load script.
        ActionSender.sendInterfaceConfig(player, 667, 0, true);
        ActionSender.sendInterfaceConfig(player, 667, 48, fromBank);
    }
    public void refreshEquipScreen() {
        // Weight
        ActionSender.sendString(player, 667, 24, Math.ceil(weight) + " kg");

        // Attack bonus
        ActionSender.sendString(player, 667, 30, "Stab: " + (bonuses[STAB_ATTACK] >= 0 ? "+" : "") + bonuses[STAB_ATTACK]);
        ActionSender.sendString(player, 667, 31, "Slash: " + (bonuses[SLASH_ATTACK] >= 0 ? "+" : "") + bonuses[SLASH_ATTACK]);
        ActionSender.sendString(player, 667, 32, "Crush: " + (bonuses[CRUSH_ATTACK] >= 0 ? "+" : "") + bonuses[CRUSH_ATTACK]);
        ActionSender.sendString(player, 667, 33, "Magic: " + (bonuses[MAGIC_ATTACK] >= 0 ? "+" : "") + bonuses[MAGIC_ATTACK]);
        ActionSender.sendString(player, 667, 34, "Ranged: " + (bonuses[RANGED_ATTACK] >= 0 ? "+" : "") + bonuses[RANGED_ATTACK]);
        // Defence bonus
        ActionSender.sendString(player, 667, 35, "Stab: " + (bonuses[STAB_DEFENCE] >= 0 ? "+" : "") + bonuses[STAB_DEFENCE]);
        ActionSender.sendString(player, 667, 36, "Slash: " + (bonuses[SLASH_DEFENCE] >= 0 ? "+" : "") + bonuses[SLASH_DEFENCE]);
        ActionSender.sendString(player, 667, 37, "Crush: " + (bonuses[CRUSH_DEFENCE] >= 0 ? "+" : "") + bonuses[CRUSH_DEFENCE]);
        ActionSender.sendString(player, 667, 38, "Magic: " + (bonuses[MAGIC_DEFENCE] >= 0 ? "+" : "") + bonuses[MAGIC_DEFENCE]);
        ActionSender.sendString(player, 667, 39, "Ranged: " + (bonuses[RANGED_DEFENCE] >= 0 ? "+" : "") + bonuses[RANGED_DEFENCE]);
        ActionSender.sendString(player, 667, 40, "Summoning: " + (bonuses[SUMMONING_DEFENCE] >= 0 ? "+" : "") + bonuses[SUMMONING_DEFENCE]);

        // Absorb bonus
        ActionSender.sendString(player, 667, 41, "Absorb Melee: " + (absorptionBonus[MELEE_ABSORPTION] >= 0 ? "+" : "") + absorptionBonus[MELEE_ABSORPTION] + "%");
        ActionSender.sendString(player, 667, 42, "Absorb Magic: " + (absorptionBonus[MAGIC_ABSORPTION] >= 0 ? "+" : "") + absorptionBonus[MAGIC_ABSORPTION] + "%");
        ActionSender.sendString(player, 667, 43, "Absorb Range: " + (absorptionBonus[RANGE_ABSORPTION] >= 0 ? "+" : "") + absorptionBonus[RANGE_ABSORPTION] + "%");

        // Other bonus
        ActionSender.sendString(player, 667, 44, "Strength: +" + bonuses[STRENGTH]);
        ActionSender.sendString(player, 667, 45, "Ranged Strength: +" + bonuses[RANGED]);
        ActionSender.sendString(player, 667, 46, "Prayer: " + (bonuses[PRAYER] >= 0 ? "+" : "") + bonuses[PRAYER]);
        ActionSender.sendString(player, 667, 47, "Magic Damage: " + (bonuses[MAGIC] >= 0 ? "+" : "") + bonuses[MAGIC] + "%");
    }

    public int getAbsorptionBonus(int id) {
        if (id < 0 || id >= 3) return 0;
        int total = 0;
        for (Item equip : player.getEquipment().getContainer().toArray()) {
            if (equip != null) total += combatDefinition(equip).getAbsorptionBonus()[id];
        }
        return total;
    }

    public int getDefence(int type) {
    	if (bonuses == null) {
    		calculate();
    	}
        return bonuses[type + 5];
    }

	private static ItemDefinition combatDefinition(Item item) {
		return ItemDefinition.forId(DegradingHandler.getCombatItemId(item.getId()));
	}
}
