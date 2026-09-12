package org.dementhium.content.items;

import java.util.ArrayList;

import org.dementhium.model.definition.ItemDefinition;
import org.dementhium.model.player.Player;

/** Shared declarative server definition for a custom item and its client support. */
final class ItemSpec {

    enum Capability {
        INFERNAL_CAPE("the Infernal cape") {
            boolean supported(Player player) {
                return player.getConnection().supportsInfernalCape();
            }
        },
        OSRS_EQUIPMENT("this OSRS item") {
            boolean supported(Player player) {
                return player.getConnection().supportsOsrsEquipment();
            }
        };

        final String itemName;

        Capability(String itemName) {
            this.itemName = itemName;
        }

        abstract boolean supported(Player player);
    }

    final int id, equipId, slot, value;
    // Supplies both the render animation and the next legacy appearance fallback.
    final int baseItem;
    final String name, examine;
    final boolean tradeable;
    final double weight;
    final int highAlchPrice, lowAlchPrice;
    final int[] bonuses, requirements;
    final Capability capability;
    final CustomItemCache cache;

    ItemSpec(int id, String name, String examine, int equipId, int baseItem, int slot,
            boolean tradeable, double weight, int value, int highAlchPrice, int lowAlchPrice,
            int[] bonuses, int[] requirements, Capability capability, CustomItemCache cache) {
        this.id = id;
        this.name = name;
        this.examine = examine;
        this.equipId = equipId;
        this.baseItem = baseItem;
        this.slot = slot;
        this.tradeable = tradeable;
        this.weight = weight;
        this.value = value;
        this.highAlchPrice = highAlchPrice;
        this.lowAlchPrice = lowAlchPrice;
        this.bonuses = bonuses;
        this.requirements = requirements;
        this.capability = capability;
        this.cache = cache;
    }

    ItemDefinition definition() {
        cache.prepare();
        ArrayList<Integer> skillIds = new ArrayList<Integer>();
        ArrayList<Integer> skillLevels = new ArrayList<Integer>();
        for (int i = 0; i < requirements.length; i += 2) {
            skillIds.add(requirements[i]);
            skillLevels.add(requirements[i + 1]);
        }
        return new ItemDefinition(id, name, examine, equipId, ItemDefinition.forId(baseItem).getRenderId(),
            bonuses.clone(), false, false, tradeable, skillIds, skillLevels, weight,
            highAlchPrice, lowAlchPrice, value, value, 0, slot, new int[]{0, 0, 0}, true, false);
    }
}
