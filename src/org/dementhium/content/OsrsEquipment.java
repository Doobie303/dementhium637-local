package org.dementhium.content;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.dementhium.model.definition.ItemDefinition;
import org.dementhium.model.player.Equipment;
import org.dementhium.model.player.Player;
import org.dementhium.model.player.Skills;

/** Capability-gated OSRS equipment supplied by the maintained developer client. */
public final class OsrsEquipment {

    public static final int PRIMORDIAL_BOOTS = 20431;
    public static final int PEGASIAN_BOOTS = 20432;
    public static final int ETERNAL_BOOTS = 20433;
    public static final int AVERNIC_DEFENDER = 20434;
    public static final int MAX_CAPE = 20435;
    public static final int INFERNAL_MAX_CAPE = 20436;
    public static final int FIRST_ID = PRIMORDIAL_BOOTS;
    public static final int LAST_ID = INFERNAL_MAX_CAPE;

    private static final int FIRST_EQUIP_ID = 5157;
    private static final int[] FALLBACK_ITEMS = {11732, 2577, 6920, 20072, 19709, InfernalCape.ID};
    private static final ItemSpec[] ITEMS = {
        new ItemSpec(PRIMORDIAL_BOOTS, "Primordial boots", "A pair of upgraded dragon boots.",
            65009, 65010, 65011, 976, 147, 279, 5, -5, 75000,
            new int[]{2, 2, 2, -4, -1, 22, 22, 22, 0, 0, 0, 5, 0, 0, 0},
            Equipment.SLOT_FEET, 11732, true, requirements(Skills.STRENGTH, 75, Skills.DEFENCE, 75), false),
        new ItemSpec(PEGASIAN_BOOTS, "Pegasian boots", "A pair of upgraded ranger boots.",
            65006, 65007, 65008, 976, 147, 279, 5, -5, 75000,
            new int[]{0, 0, 0, -12, 12, 5, 5, 5, 5, 5, 0, 0, 0, 0, 0},
            Equipment.SLOT_FEET, 2577, true, requirements(Skills.RANGED, 75, Skills.DEFENCE, 75), false),
        new ItemSpec(ETERNAL_BOOTS, "Eternal boots", "A pair of upgraded infinity boots.",
            65003, 65004, 65005, 976, 147, 279, 5, -5, 75000,
            new int[]{0, 0, 0, 8, 0, 5, 5, 5, 8, 5, 0, 0, 0, 0, 1},
            Equipment.SLOT_FEET, 6920, true, requirements(Skills.MAGIC, 75, Skills.DEFENCE, 75), false),
        new ItemSpec(AVERNIC_DEFENDER, "Avernic defender", "Defensive weaponry from the Avernic realm.",
            65012, 65013, 65014, 717, 498, 256, 8, 8, 2500000,
            new int[]{30, 29, 28, -5, -4, 30, 29, 28, -5, -4, 0, 8, 0, 0, 0},
            Equipment.SLOT_SHIELD, 20072, false, requirements(Skills.ATTACK, 70, Skills.DEFENCE, 70), false),
        new ItemSpec(MAX_CAPE, "Max cape", "The cape worn by only the most experienced players.",
            65015, 65016, 65017, 2232, 687, 27, 0, -5, 99000,
            new int[]{0, 0, 0, 0, 0, 9, 9, 9, 9, 9, 0, 0, 0, 4, 0},
            Equipment.SLOT_CAPE, 19709, false, maxRequirements(), false),
        new ItemSpec(INFERNAL_MAX_CAPE, "Infernal max cape", "The cape worn by only the most experienced players.",
            65018, 65019, 65020, 2232, 687, 27, 0, -5, 99000,
            new int[]{4, 4, 4, 1, 1, 12, 12, 12, 12, 12, 0, 8, 0, 2, 0},
            Equipment.SLOT_CAPE, InfernalCape.ID, false, maxRequirements(), true)
    };

    private OsrsEquipment() {
    }

    public static boolean isItem(int id) {
        return id >= FIRST_ID && id <= LAST_ID;
    }

    public static boolean supported(Player player) {
        return player.getConnection().supportsOsrsEquipment();
    }

    public static int appearanceId(Player viewer, int itemId, int equipId) {
        if (!isItem(itemId) || supported(viewer)) {
            return equipId;
        }
        int fallback = FALLBACK_ITEMS[itemId - FIRST_ID];
        if (itemId == INFERNAL_MAX_CAPE) {
            return InfernalCape.appearanceId(viewer, fallback, ItemDefinition.forId(fallback).getEquipId());
        }
        return ItemDefinition.forId(fallback).getEquipId();
    }

    public static ItemDefinition definition(int id) {
        ItemSpec item = spec(id);
        ArrayList<Integer> skillIds = new ArrayList<Integer>();
        ArrayList<Integer> skillLevels = new ArrayList<Integer>();
        for (int i = 0; i < item.requirements.length; i += 2) {
            skillIds.add(item.requirements[i]);
            skillLevels.add(item.requirements[i + 1]);
        }
        return new ItemDefinition(item.id, item.name, item.examine,
            FIRST_EQUIP_ID + item.id - FIRST_ID, ItemDefinition.forId(item.renderSource).getRenderId(),
            item.bonuses.clone(), false, false, item.tradeable, skillIds, skillLevels,
            item.id == PRIMORDIAL_BOOTS || item.id == PEGASIAN_BOOTS || item.id == ETERNAL_BOOTS ? 1.814 : 0.453,
            item.tradeable ? item.value * 3 / 5 : 0, item.tradeable ? item.value * 2 / 5 : 0,
            item.value, item.value, 0, item.slot, new int[]{0, 0, 0}, true, false);
    }

    public static byte[] cacheDefinition(int id) {
        ItemSpec item = spec(id);
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeByte(1); out.writeShort(item.inventoryModel);
            out.writeByte(2); out.writeBytes(item.name); out.writeByte(0);
            out.writeByte(4); out.writeShort(item.zoom);
            out.writeByte(5); out.writeShort(item.rotation1);
            out.writeByte(6); out.writeShort(item.rotation2);
            if (item.offset1 != 0) { out.writeByte(7); out.writeShort(item.offset1); }
            if (item.offset2 != 0) { out.writeByte(8); out.writeShort(item.offset2); }
            out.writeByte(12); out.writeInt(item.value);
            out.writeByte(16);
            out.writeByte(23); out.writeShort(item.maleModel);
            out.writeByte(25); out.writeShort(item.femaleModel);
            out.writeByte(36); out.writeBytes(item.slot == Equipment.SLOT_SHIELD ? "Wield" : "Wear"); out.writeByte(0);
            if (item.infernalTexture) {
                out.writeByte(41); out.writeByte(1); out.writeShort(59); out.writeShort(915);
            }
            out.writeByte(0);
            return bytes.toByteArray();
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }
    }

    private static ItemSpec spec(int id) {
        if (!isItem(id)) {
            throw new IllegalArgumentException("Not a custom OSRS equipment item: " + id);
        }
        return ITEMS[id - FIRST_ID];
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

    private static final class ItemSpec {
        private final int id, inventoryModel, maleModel, femaleModel, zoom, rotation1, rotation2;
        private final int offset1, offset2, value, slot, renderSource;
        private final String name, examine;
        private final int[] bonuses, requirements;
        private final boolean tradeable, infernalTexture;

        private ItemSpec(int id, String name, String examine, int inventoryModel, int maleModel,
                int femaleModel, int zoom, int rotation1, int rotation2, int offset1, int offset2,
                int value, int[] bonuses, int slot, int renderSource, boolean tradeable,
                int[] requirements, boolean infernalTexture) {
            this.id = id;
            this.name = name;
            this.examine = examine;
            this.inventoryModel = inventoryModel;
            this.maleModel = maleModel;
            this.femaleModel = femaleModel;
            this.zoom = zoom;
            this.rotation1 = rotation1;
            this.rotation2 = rotation2;
            this.offset1 = offset1;
            this.offset2 = offset2;
            this.value = value;
            this.bonuses = bonuses;
            this.slot = slot;
            this.renderSource = renderSource;
            this.tradeable = tradeable;
            this.requirements = requirements;
            this.infernalTexture = infernalTexture;
        }
    }
}
