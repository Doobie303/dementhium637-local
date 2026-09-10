package org.dementhium.model.player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.dementhium.model.Item;
import org.dementhium.model.World;
import org.dementhium.model.definition.ItemDefinition;

/**
 * Degradation rules for equipment available in the 2011 / revision 637 era.
 * An item's health stores charges consumed in its current degradation stage.
 */
public final class DegradingHandler {

	private static final int BARROWS_STAGE_CHARGES = 22500;
	private static final int CRYSTAL_STAGE_CHARGES = 250;
	private static final int NEX_CHARGES = 90000;
	private static final int CHAOTIC_CHARGES = 60000;
	private static final int PVP_CHARGES = 6000;
	private static final int CORRUPT_PVP_CHARGES = 1500;

	private static final Map<Integer, Integer> BARROWS_START = new HashMap<Integer, Integer>();
	private static final Map<Integer, Integer> BARROWS_BASE = new HashMap<Integer, Integer>();
	private static final Set<Integer> BARROWS_BROKEN = new HashSet<Integer>();

	static {
		BARROWS_START.put(4708, 4856);
		BARROWS_START.put(4710, 4862);
		BARROWS_START.put(4712, 4868);
		BARROWS_START.put(4714, 4874);
		BARROWS_START.put(4716, 4880);
		BARROWS_START.put(4718, 4886);
		BARROWS_START.put(4720, 4892);
		BARROWS_START.put(4722, 4898);
		BARROWS_START.put(4724, 4904);
		BARROWS_START.put(4726, 4910);
		BARROWS_START.put(4728, 4916);
		BARROWS_START.put(4730, 4922);
		BARROWS_START.put(4732, 4928);
		BARROWS_START.put(4734, 4934);
		BARROWS_START.put(4736, 4940);
		BARROWS_START.put(4738, 4946);
		BARROWS_START.put(4745, 4952);
		BARROWS_START.put(4747, 4958);
		BARROWS_START.put(4749, 4964);
		BARROWS_START.put(4751, 4970);
		BARROWS_START.put(4753, 4976);
		BARROWS_START.put(4755, 4982);
		BARROWS_START.put(4757, 4988);
		BARROWS_START.put(4759, 4994);
		for (Map.Entry<Integer, Integer> entry : BARROWS_START.entrySet()) {
			for (int stage = 0; stage < 4; stage++) {
				BARROWS_BASE.put(Integer.valueOf(entry.getValue().intValue() + stage), entry.getKey());
			}
		}
		for (int id = 4860; id <= 4998; id += 6) {
			BARROWS_BROKEN.add(Integer.valueOf(id));
		}
	}

	private DegradingHandler() {
	}

	/** Applies one successful combat-use charge to every degradable worn item. */
	public static void process(Player player) {
		if (player == null || player.getEquipment() == null) {
			return;
		}
		int currentTick = World.getTicks();
		if (player.getAttribute("lastDegradeCombatTick", -1) == currentTick) {
			return;
		}
		player.setAttribute("lastDegradeCombatTick", currentTick);
		Equipment equipment = player.getEquipment();
		boolean changed = false;
		for (int slot = 0; slot < Equipment.SIZE; slot++) {
			Item item = equipment.get(slot);
			if (!isDegradable(item) || isBroken(item) || isCorruptPvp(item)) {
				continue;
			}
			int activationId = activationId(item);
			if (activationId != -1) {
				Item activated = new Item(activationId, item.getAmount());
				activated.setHealth(1);
				equipment.set(slot, activated);
				player.sendMessage("Your " + itemName(item) + " has started to degrade.");
				changed = true;
				continue;
			}
			int stageCharges = stageCharges(item);
			int nextId = nextStage(item);
			if (stageCharges <= 0 || nextId == -1) {
				continue;
			}
			int used = item.getHealth() + 1;
			if (used < stageCharges) {
				item.setHealth(used);
				continue;
			}
			if (nextId == 0) {
				equipment.set(slot, null);
				player.sendMessage("Your " + itemName(item) + " has degraded and turned to dust.");
			} else {
				equipment.set(slot, new Item(nextId, 1));
				player.sendMessage("Your " + itemName(item) + " has degraded.");
			}
			changed = true;
		}
		if (changed) {
			equipment.refresh();
		}
	}

	/** Corrupt Ancient Warriors' equipment lasts 15 minutes while worn. */
	public static void processWorn(Player player) {
		if (player == null || player.getEquipment() == null) {
			return;
		}
		Equipment equipment = player.getEquipment();
		boolean changed = false;
		for (int slot = 0; slot < Equipment.SIZE; slot++) {
			Item item = equipment.get(slot);
			if (!isCorruptPvp(item) || isBroken(item)) {
				continue;
			}
			int used = item.getHealth() + 1;
			if (used < CORRUPT_PVP_CHARGES) {
				item.setHealth(used);
				continue;
			}
			equipment.set(slot, null);
			player.sendMessage("Your " + itemName(item) + " has degraded and turned to dust.");
			changed = true;
		}
		if (changed) {
			equipment.refresh();
		}
	}

	/** Converts tradeable Nex/PvP equipment to its untradeable form on equip. */
	public static Item activateOnEquip(Player player, Item item, boolean sendMessage) {
		if (item == null || item.getDefinition() == null || item.getDefinition().getName() == null) {
			return item;
		}
		String name = item.getDefinition().getName();
		if ((!isNexName(name) && !isPvpName(name)) || isNexDegraded(item.getId())
				|| name.endsWith("(deg)") || isBroken(item)) {
			return item;
		}
		int degradedId = item.getId() + 2;
		if (degradedId >= ItemDefinition.MAX_SIZE) {
			return item;
		}
		ItemDefinition degraded = ItemDefinition.forId(degradedId);
		if (degraded == null || degraded.getName() == null
				|| (!degraded.getName().contains(name) && !isNexName(name))) {
			return item;
		}
		Item activated = new Item(degradedId, item.getAmount());
		if (sendMessage) {
			player.sendMessage("Your " + name.toLowerCase() + " "
					+ (name.contains("legs") ? "have" : "has")
					+ " degraded and is untradeable in this state.");
		}
		return activated;
	}

	/** Sends the charge percentage for a supported degradable item. */
	public static boolean checkCharges(Player player, Item item) {
		if (!isDegradable(item)) {
			return false;
		}
		player.sendMessage("Your " + itemName(item) + " has " + remainingPercent(item)
				+ "% of its charge remaining.");
		return true;
	}

	public static boolean isDegradable(Item item) {
		if (item == null || item.getDefinition() == null || item.getDefinition().getName() == null) {
			return false;
		}
		int id = item.getId();
		String name = item.getDefinition().getName();
		return BARROWS_START.containsKey(Integer.valueOf(id)) || isBarrowsStage(id)
				|| isCrystal(id) || isNexName(name) || isPvpName(name) || isChaoticName(name);
	}

    /** Used/broken item states cannot bypass trade restrictions through administrator rights. */
    public static boolean isDegradedForTrade(Item item) {
        if (item == null) return false;
        if (item.getHealth() > 0) return true;
        int id = item.getId();
        // Use the same note mapping as banking; noting must not bypass the restriction.
        if (item.getDefinition().isNoted()) {
            id = id == 10843 ? 10828 : id - 1;
            if (id < 0) return true;
            item = new Item(id);
        }
        String name = item.getDefinition().getName();
        return isBarrowsStage(id)
                || (id >= 4214 && id <= 4223) || (id >= 4225 && id <= 4234)
                || isNexDegraded(id) || isBroken(item)
                || (name != null && name.toLowerCase().endsWith("(deg)"));
    }

	/** Returns the pristine combat-equivalent id for a usable Barrows stage. */
	public static int getCombatItemId(int id) {
		Integer baseId = BARROWS_BASE.get(Integer.valueOf(id));
		return baseId == null ? id : baseId.intValue();
	}

	private static int activationId(Item item) {
		Integer barrowsId = BARROWS_START.get(Integer.valueOf(item.getId()));
		if (barrowsId != null) {
			return barrowsId.intValue();
		}
		if (item.getId() == 4212) {
			return 4214;
		}
		if (item.getId() == 4224) {
			return 4225;
		}
		return -1;
	}

	private static int nextStage(Item item) {
		int id = item.getId();
		if (isBarrowsStage(id)) {
			return BARROWS_BROKEN.contains(Integer.valueOf(id)) ? -1 : id + 1;
		}
		if (id >= 4214 && id < 4223) {
			return id + 1;
		}
		if (id == 4223) {
			return 0;
		}
		if (id >= 4225 && id < 4234) {
			return id + 1;
		}
		if (id == 4234) {
			return 0;
		}
		String name = item.getDefinition().getName();
		if (isNexDegraded(id)) {
			return id + 1;
		}
		if (isChaoticName(name) && !isBroken(item)) {
			return id + 1;
		}
		if (name.endsWith("(deg)") && isPvpName(name)) {
			return 0;
		}
		return -1;
	}

	private static int stageCharges(Item item) {
		int id = item.getId();
		String name = item.getDefinition().getName();
		if (isBarrowsStage(id)) {
			return BARROWS_STAGE_CHARGES;
		}
		if (isCrystal(id)) {
			return CRYSTAL_STAGE_CHARGES;
		}
		if (isNexDegraded(id)) {
			return NEX_CHARGES;
		}
		if (isChaoticName(name)) {
			return CHAOTIC_CHARGES;
		}
		if (isPvpName(name)) {
			return name.toLowerCase().startsWith("corrupt")
					? CORRUPT_PVP_CHARGES : PVP_CHARGES;
		}
		return -1;
	}

	private static int remainingPercent(Item item) {
		int id = item.getId();
		if (isBroken(item)) {
			return 0;
		}
		if (BARROWS_START.containsKey(Integer.valueOf(id)) || id == 4212 || id == 4224) {
			return 100;
		}
		if (isBarrowsStage(id)) {
			int stage = (id - 4856) % 6;
			int remaining = ((4 - stage) * BARROWS_STAGE_CHARGES) - item.getHealth();
			return percent(remaining, BARROWS_STAGE_CHARGES * 4);
		}
		if (id >= 4214 && id <= 4223) {
			int remaining = ((10 - (id - 4214)) * CRYSTAL_STAGE_CHARGES) - item.getHealth();
			return percent(remaining, CRYSTAL_STAGE_CHARGES * 10);
		}
		if (id >= 4225 && id <= 4234) {
			int remaining = ((10 - (id - 4225)) * CRYSTAL_STAGE_CHARGES) - item.getHealth();
			return percent(remaining, CRYSTAL_STAGE_CHARGES * 10);
		}
		int maximum = stageCharges(item);
		return maximum <= 0 ? 100 : percent(maximum - item.getHealth(), maximum);
	}

	private static int percent(int remaining, int maximum) {
		if (remaining <= 0 || maximum <= 0) {
			return 0;
		}
		return Math.min(100, Math.max(1, (remaining * 100) / maximum));
	}

	private static boolean isBarrowsStage(int id) {
		return id >= 4856 && id <= 4998 && ((id - 4856) % 6) <= 4;
	}

	private static boolean isCrystal(int id) {
		return id == 4212 || (id >= 4214 && id <= 4223)
				|| id == 4224 || (id >= 4225 && id <= 4234);
	}

	private static boolean isNexDegraded(int id) {
		return id >= 20137 && id <= 20169 && ((id - 20135) % 4) == 2;
	}

	private static boolean isNexBroken(int id) {
		return id >= 20138 && id <= 20170 && ((id - 20135) % 4) == 3;
	}

	private static boolean isBroken(Item item) {
		return BARROWS_BROKEN.contains(Integer.valueOf(item.getId()))
				|| isNexBroken(item.getId())
				|| item.getDefinition().getName().toLowerCase().contains("broken");
	}

	private static boolean isNexName(String name) {
		return name.contains("Torva") || name.contains("Pernix") || name.contains("Virtus");
	}

	private static boolean isChaoticName(String name) {
		return name.toLowerCase().startsWith("chaotic");
	}

	private static boolean isPvpName(String name) {
		String lower = name.toLowerCase();
		return name.contains("Vesta") || name.contains("Statius") || name.contains("Morrigan")
				|| name.contains("Zuriel") || lower.startsWith("corrupt");
	}

	private static boolean isCorruptPvp(Item item) {
		if (item == null || item.getDefinition() == null || item.getDefinition().getName() == null) {
			return false;
		}
		String lower = item.getDefinition().getName().toLowerCase();
		return lower.startsWith("corrupt") && lower.endsWith("(deg)");
	}

	private static String itemName(Item item) {
		return item.getDefinition().getName().toLowerCase();
	}
}
