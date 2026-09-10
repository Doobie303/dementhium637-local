package org.dementhium.model.player;

import org.dementhium.model.Item;
import org.dementhium.model.definition.ItemDefinition;
import org.dementhium.net.ActionSender;

/** Selected-item details for equipment interface 667, rendered by clientscript 2782. */
public final class EquipmentItemStats {
    private EquipmentItemStats() {}

    public static void show(Player player, Item item) {
        if (item == null) return;
        ItemDefinition definition = ItemDefinition.forId(DegradingHandler.getCombatItemId(item.getId()));
        int slot = Equipment.getItemType(item.getId());
        if (definition.isNoted() || slot < 0 || slot >= Equipment.SIZE) {
            close(player);
            player.sendMessage("This item has no equipment stats.");
            return;
        }
        int[] bonuses = definition.getBonus();
        int[] absorption = definition.getAbsorptionBonus();
        StringBuilder text = new StringBuilder("<col=ffffff>Attack bonuses</col>");
        String[] styles = {"Stab", "Slash", "Crush", "Magic", "Ranged"};
        for (int i = 0; i < styles.length; i++) row(text, styles[i], bonuses[i], false);
        text.append("<br><col=ffffff>Defence bonuses</col>");
        for (int i = 0; i < styles.length; i++) row(text, styles[i], bonuses[i + 5], false);
        row(text, "Summoning", bonuses[Bonuses.SUMMONING_DEFENCE], false);
        row(text, "Absorb melee", absorption[0], true);
        row(text, "Absorb magic", absorption[1], true);
        row(text, "Absorb ranged", absorption[2], true);
        text.append("<br><col=ffffff>Other bonuses</col>");
        row(text, "Strength", bonuses[Bonuses.STRENGTH], false);
        // Inspect the item's own ranged strength, independent of the player's
        // current weapon. This also applies the existing verified ammo values.
        row(text, "Ranged strength", RangedEquipmentStats.strength(definition, Equipment.SLOT_WEAPON, item), false);
        row(text, "Prayer", bonuses[Bonuses.PRAYER], false);
        row(text, "Magic damage", bonuses[Bonuses.MAGIC], true);
        ActionSender.sendSpecialString(player, 321, item.getDefinition().getName());
        ActionSender.sendSpecialString(player, 322, "");
        ActionSender.sendSpecialString(player, 323, "");
        ActionSender.sendSpecialString(player, 324, text.toString());
        ActionSender.sendSpecialString(player, 325, "");
        ActionSender.sendClientScript(player, 2782, new Object[0], "");
        ActionSender.sendString(player, 667, 65, "Back");
    }

    private static void row(StringBuilder text, String label, int value, boolean percent) {
        text.append("<br>").append(label).append(": ");
        if (value >= 0) text.append('+');
        text.append(value);
        if (percent) text.append('%');
    }

    public static void close(Player player) {
        // The native Back script clears all five strings and both inventory blockers.
        ActionSender.sendClientScript(player, 2947, new Object[0], "");
    }
}

