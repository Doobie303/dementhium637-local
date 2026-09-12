package org.dementhium.content.items;

import java.util.ArrayList;
import java.util.List;

import org.dementhium.content.items.ItemSpec.Capability;
import org.dementhium.model.definition.ItemDefinition;
import org.dementhium.model.player.Equipment;
import org.dementhium.model.player.Player;
import org.dementhium.model.player.Skills;

/**
 * Authoritative custom-item catalog and integration points. Add ordinary imported
 * items as entries here; cache encoding and resource loading are kept separate.
 */
public final class CustomItems {

    public static final int INFERNAL_CAPE = 20430;
    public static final int PRIMORDIAL_BOOTS = 20431;
    public static final int PEGASIAN_BOOTS = 20432;
    public static final int ETERNAL_BOOTS = 20433;
    public static final int AVERNIC_DEFENDER = 20434;
    public static final int MAX_CAPE = 20435;
    public static final int INFERNAL_MAX_CAPE = 20436;
    static final String INFERNAL_CAPE_DEFINITION = "data/custom/infernal-cape/item.dat";

    // Register a custom base item before any entry that derives its render/fallback.
    private static final ItemSpec[] ITEMS = {
        new ItemSpec(INFERNAL_CAPE, "Infernal cape",
            "A cape of fire, awarded to those who have defeated the Inferno.",
            5156, 6570, Equipment.SLOT_CAPE, false, 1.814, 80000, 48000, 32000,
            new int[]{4, 4, 4, 1, 1, 12, 12, 12, 12, 12, 0, 8, 0, 2, 0},
            requirements(), Capability.INFERNAL_CAPE, new InfernalCapeCache()),
        new ItemSpec(PRIMORDIAL_BOOTS, "Primordial boots", "A pair of upgraded dragon boots.",
            5157, 11732, Equipment.SLOT_FEET, true, 1.814, 75000, 45000, 30000,
            new int[]{2, 2, 2, -4, -1, 22, 22, 22, 0, 0, 0, 5, 0, 0, 0},
            requirements(Skills.STRENGTH, 75, Skills.DEFENCE, 75), Capability.OSRS_EQUIPMENT,
            CustomItemCache.models(65009, 65010, 65011, 976, 147, 279, 5, -5, false)),
        new ItemSpec(PEGASIAN_BOOTS, "Pegasian boots", "A pair of upgraded ranger boots.",
            5158, 2577, Equipment.SLOT_FEET, true, 1.814, 75000, 45000, 30000,
            new int[]{0, 0, 0, -12, 12, 5, 5, 5, 5, 5, 0, 0, 0, 0, 0},
            requirements(Skills.RANGED, 75, Skills.DEFENCE, 75), Capability.OSRS_EQUIPMENT,
            CustomItemCache.models(65006, 65007, 65008, 976, 147, 279, 5, -5, false)),
        new ItemSpec(ETERNAL_BOOTS, "Eternal boots", "A pair of upgraded infinity boots.",
            5159, 6920, Equipment.SLOT_FEET, true, 1.814, 75000, 45000, 30000,
            new int[]{0, 0, 0, 8, 0, 5, 5, 5, 8, 5, 0, 0, 0, 0, 1},
            requirements(Skills.MAGIC, 75, Skills.DEFENCE, 75), Capability.OSRS_EQUIPMENT,
            CustomItemCache.models(65003, 65004, 65005, 976, 147, 279, 5, -5, false)),
        new ItemSpec(AVERNIC_DEFENDER, "Avernic defender", "Defensive weaponry from the Avernic realm.",
            5160, 20072, Equipment.SLOT_SHIELD, false, 0.453, 2500000, 0, 0,
            new int[]{30, 29, 28, -5, -4, 30, 29, 28, -5, -4, 0, 8, 0, 0, 0},
            requirements(Skills.ATTACK, 70, Skills.DEFENCE, 70), Capability.OSRS_EQUIPMENT,
            CustomItemCache.models(65012, 65013, 65014, 717, 498, 256, 8, 8, false)),
        new ItemSpec(MAX_CAPE, "Max cape", "The cape worn by only the most experienced players.",
            5161, 19709, Equipment.SLOT_CAPE, false, 0.453, 99000, 0, 0,
            new int[]{0, 0, 0, 0, 0, 9, 9, 9, 9, 9, 0, 0, 0, 4, 0},
            maxRequirements(), Capability.OSRS_EQUIPMENT,
            CustomItemCache.models(65015, 65016, 65017, 2232, 687, 27, 0, -5, false)),
        new ItemSpec(INFERNAL_MAX_CAPE, "Infernal max cape", "The cape worn by only the most experienced players.",
            5162, INFERNAL_CAPE, Equipment.SLOT_CAPE, false, 0.453, 99000, 0, 0,
            new int[]{4, 4, 4, 1, 1, 12, 12, 12, 12, 12, 0, 8, 0, 2, 0},
            maxRequirements(), Capability.OSRS_EQUIPMENT,
            CustomItemCache.models(65018, 65019, 65020, 2232, 687, 27, 0, -5, true))
    };

    private CustomItems() {
    }

    public static int maxItemId() {
        int maximum = -1;
        for (ItemSpec item : ITEMS) {
            maximum = Math.max(maximum, item.id);
        }
        return maximum;
    }

    public static List<Integer> osrsEquipmentIds() {
        List<Integer> ids = new ArrayList<Integer>();
        for (ItemSpec item : ITEMS) {
            if (item.capability == Capability.OSRS_EQUIPMENT) {
                ids.add(item.id);
            }
        }
        return ids;
    }

    /** Called after native definitions/misc overrides and before absorption corrections. */
    public static void applyDefinitions(ItemDefinition[] definitions) {
        for (ItemSpec item : ITEMS) {
            definitions[item.id] = item.definition();
        }
    }

    /** Returns null to leave an ordinary item on the native cache-loading path. */
    public static byte[] cacheDefinition(int id) {
        ItemSpec item = spec(id);
        return item == null ? null : item.cache.encode(item);
    }

    public static int appearanceId(Player viewer, int itemId, int equipId) {
        ItemSpec item = spec(itemId);
        if (item == null) {
            return equipId;
        }
        item.cache.prepare();
        if (item.capability.supported(viewer)) {
            return equipId;
        }
        return appearanceId(viewer, item.baseItem, ItemDefinition.forId(item.baseItem).getEquipId());
    }

    public static boolean canEquip(Player player, int itemId) {
        return checkClientSupport(player, itemId, "equip");
    }

    public static boolean canSpawn(Player player, int itemId) {
        return checkClientSupport(player, itemId, "test");
    }

    private static boolean checkClientSupport(Player player, int itemId, String action) {
        ItemSpec item = spec(itemId);
        if (item == null) {
            return true;
        }
        item.cache.prepare();
        if (item.capability.supported(player)) {
            return true;
        }
        player.sendMessage("Use the updated development client to " + action + " " + item.capability.itemName + ".");
        return false;
    }

    private static ItemSpec spec(int id) {
        for (ItemSpec item : ITEMS) {
            if (item.id == id) {
                return item;
            }
        }
        return null;
    }

    private static int[] requirements(int... requirements) {
        return requirements;
    }

    private static int[] maxRequirements() {
        int[] requirements = new int[Skills.SKILL_COUNT * 2];
        for (int skill = 0; skill < Skills.SKILL_COUNT; skill++) {
            requirements[skill * 2] = skill;
            requirements[skill * 2 + 1] = 99;
        }
        return requirements;
    }
}
