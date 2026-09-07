package org.dementhium.model.player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.dementhium.model.Item;
import org.dementhium.model.definition.ItemDefinition;

/**
 * Combat degrading for 2011 / 637 gear: Barrows, Nex, PvP, crystal, chaotic.
 * Charges are stored on {@link Item#setHealth(int)}.
 */
public class DegradingHandler {

	public static final int HITS_PER_STAGE = 80;

	private static final Map<Integer, Integer> BARROWS_START = new HashMap<Integer, Integer>();
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
		int[] broken = { 4860, 4866, 4872, 4878, 4884, 4890, 4896, 4902, 4908,
				4914, 4920, 4926, 4932, 4938, 4944, 4950, 4956, 4962, 4968,
				4974, 4980, 4986, 4992, 4998 };
		for (int id : broken) {
			BARROWS_BROKEN.add(Integer.valueOf(id));
		}
	}

	public static void process(Player player) {
		if (player == null || player.getEquipment() == null) {
			return;
		}
		Equipment equipment = player.getEquipment();
		boolean changed = false;
		for (int slot = 0; slot < Equipment.SIZE; slot++) {
			Item item = equipment.get(slot);
			if (item == null || item.getDefinition() == null) {
				continue;
			}
			int nextId = nextStage(item);
			if (nextId == -1) {
				continue;
			}
			int charges = item.getHealth() + 1;
			item.setHealth(charges);
			if (charges < HITS_PER_STAGE) {
				continue;
			}
			item.setHealth(0);
			String name = item.getDefinition().getName();
			if (name == null) {
				name = "item";
			}
			if (nextId == 0) {
				equipment.set(slot, null);
				player.sendMessage("Your " + name.toLowerCase() + " has degraded and turned to dust.");
			} else {
				equipment.set(slot, new Item(nextId, 1));
				player.sendMessage("Your " + name.toLowerCase() + " has degraded.");
			}
			changed = true;
		}
		if (changed) {
			equipment.refresh();
		}
	}

	private static int nextStage(Item item) {
		int id = item.getId();
		if (BARROWS_BROKEN.contains(Integer.valueOf(id))) {
			return -1;
		}
		if (BARROWS_START.containsKey(Integer.valueOf(id))) {
			return BARROWS_START.get(Integer.valueOf(id)).intValue();
		}
		if (id >= 4856 && id <= 4998 && !BARROWS_BROKEN.contains(Integer.valueOf(id + 1))) {
			return id + 1;
		}
		if (id == 4212) {
			return 4214;
		}
		if (id >= 4214 && id < 4223) {
			return id + 1;
		}
		if (id == 4223) {
			return 0;
		}
		if (id == 4224) {
			return 4225;
		}
		if (id >= 4225 && id < 4234) {
			return id + 1;
		}
		if (id == 4234) {
			return 0;
		}
		ItemDefinition def = item.getDefinition();
		if (def == null || def.getName() == null) {
			return -1;
		}
		String name = def.getName();
		if (isChargeName(name) && !name.contains("(deg)") && !name.toLowerCase().contains("broken")) {
			int degId = id + 2;
			if (degId < ItemDefinition.MAX_SIZE) {
				ItemDefinition deg = ItemDefinition.forId(degId);
				if (deg != null && deg.getName() != null && deg.getName().endsWith("(deg)")) {
					return degId;
				}
			}
			if (name.toLowerCase().startsWith("chaotic")) {
				int brokenId = id + 1;
				if (brokenId < ItemDefinition.MAX_SIZE) {
					return brokenId;
				}
			}
		}
		if (name.endsWith("(deg)")) {
			if (isPvpName(name)) {
				return 0;
			}
			for (int offset = 1; offset <= 2; offset++) {
				int brokenId = id + offset;
				if (brokenId < ItemDefinition.MAX_SIZE) {
					ItemDefinition broken = ItemDefinition.forId(brokenId);
					if (broken != null && broken.getName() != null
							&& broken.getName().toLowerCase().contains("broken")) {
						return brokenId;
					}
				}
			}
		}
		return -1;
	}

	private static boolean isChargeName(String name) {
		return isNexGear(name) || isPvpName(name) || name.toLowerCase().startsWith("chaotic")
				|| name.toLowerCase().startsWith("corrupt");
	}

	private static boolean isNexGear(String name) {
		return name.contains("Torva") || name.contains("Pernix") || name.contains("Virtus");
	}

	private static boolean isPvpName(String name) {
		return name.contains("Vesta") || name.contains("Statius") || name.contains("Morrigan")
				|| name.contains("Zuriel") || name.toLowerCase().startsWith("corrupt");
	}
}
