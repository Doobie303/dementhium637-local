package org.dementhium.model.player;

import org.dementhium.model.Container;
import org.dementhium.model.Item;
import org.dementhium.model.definition.ItemDefinition;
import org.dementhium.model.misc.GroundItem;
import org.dementhium.model.misc.GroundItemManager;
import org.dementhium.net.ActionSender;

/**
 * Manages the player inventory.
 * 
 * @author Graham
 */
public class Inventory {

	public static final int SIZE = 28;

	private final Container inventory = new Container(SIZE, false);

	private final Player player;

	public Inventory(Player player) {
		this.player = player;
	}

	public boolean addItem(int item, int amount) {
		return addItem(item, amount, true, true);
	}

	public boolean canAddItem(int item, int amount) {
		return addItem(item, amount, true, false);
	}

	public boolean addItem(int item, int amount, boolean refresh, boolean addItem) {
		if (item < 0 || amount < 1 || item > ItemDefinition.MAX_SIZE) {
			ActionSender.sendChatMessage(player, 0,
					"Not enough space in your inventory.");
			return false;
		}
		int freeSlots = inventory.getFreeSlots();
		ItemDefinition def = ItemDefinition.forId(item);
		if (freeSlots < 1) {
			if (!inventory.contains(new Item(item))) {
				ActionSender.sendChatMessage(player, 0,
						"Not enough space in your inventory.");
				return false;
			} else if (inventory.contains(new Item(item)) && (!def.isStackable() && !def.isNoted())) {
				ActionSender.sendChatMessage(player, 0,
						"Not enough space in your inventory.");
				return false;
			}
		}
		int inventoryAmount = inventory.getNumberOf(new Item(item));
		if ((amount + inventoryAmount) < 0) {
			ActionSender.sendChatMessage(player, 0,
					"Not enough space in your inventory.");
			return false;
		}
		if (!def.isStackable() && amount > freeSlots) {
			//amount = freeSlots;
			ActionSender.sendChatMessage(player, 0,
					"Not enough space in your inventory.");
			return false;
		}
		if (addItem) {
			boolean b = inventory.add(new Item(item, amount));
			if (!b) {
				ActionSender.sendChatMessage(player, 0,
						"Not enough space in your inventory.");
				if (refresh)
					refresh();
				return false;
			}
			if (refresh)
				refresh();
		}
		return true;
	}

	public boolean containsExactAmount(int item, int amount) {
		if (getContainer().getNumberOf(new Item(item)) == amount && inventory.contains(new Item(item, amount)))
			return true;
		return false;
	}

	public boolean contains(int item, int amount) {
		return inventory.contains(new Item(item, amount));
	}

	public boolean contains(int item) {
		return inventory.containsOne(new Item(item));
	}

	public void deleteItem(Item item) {
		inventory.remove(item);
		refresh();
	}

	public void deleteItem(int item, int amount) {
		inventory.remove(new Item(item, amount));
		refresh();
	}

	public void deleteItem(int item, int amount, boolean refresh) {
		inventory.remove(new Item(item, amount));
		if (refresh) {
			refresh();
		}
	}

	public void deleteAll(int item) {
		inventory.removeAll(new Item(item));
		refresh();
	}

	public void refresh() {
		ActionSender.sendItems(player, 93, inventory, false);
	}

	public Container getContainer() {
		return inventory;
	}

	public int getFreeSlots() {
		return inventory.getFreeSlots();
	}

	public boolean hasRoomFor(int id, int itemAmount) {
		if (ItemDefinition.forId(id).isStackable()) {
			return getFreeSlots() >= 1 || contains(id);
		} else {
			return getFreeSlots() >= itemAmount;
		}
	}

	public int numberOf(int id) {
		return inventory.getNumberOf(new Item(id, 1));
	}

	public Item lookup(int id) {
		return inventory.lookup(id);
	}

	public int lookupSlot(int id) {
		return inventory.lookupSlot(id);
	}

	public Item get(int slot) {
		return inventory.get(slot);
	}

	public void set(int slot, Item item) {
		inventory.set(slot, item);
	}

	public void deleteItem(int id, int amount, int slot) {
		inventory.remove(slot, new Item(id, amount));
		refresh();
	}

	public void deleteItem(int id, int amount, int slot, boolean refresh) {
		inventory.remove(slot, new Item(id, amount));
		if (refresh) {
			refresh();
		}
	}

	public boolean addItem(Item item) {
		if (item != null && item.getHealth() > 0) {
			if (inventory.getFreeSlots() < 1) {
				ActionSender.sendChatMessage(player, 0, "Not enough space in your inventory.");
				return false;
			}
			boolean added = inventory.add(new Item(item));
			refresh();
			return added;
		}
		return addItem(item.getId(), item.getAmount(), true, true);
	}

	public boolean canAddItem(Item item) {
		return addItem(item.getId(), item.getAmount(), true, false);
	}

	public boolean removeItems(Item... items) {
		return removeItems(true, items);
	}

	public boolean removeItems(boolean refresh, Item... items) {
		for (Item item : items) {
			if (item != null) {
				if (contains(item.getId(), item.getAmount())) {
					deleteItem(item.getId(), item.getAmount(), false);
				}
			}
		}
		if (refresh) {
			refresh();
		}
		return true;
	}

	/*
	 * Adds an item but if the inventory is full it will drop it to the ground.
	 * Used for the spoils in staking in the Duel Arena.
	 */
	public void addDropable(Item item) {
		if (!addItem(item)) {
			GroundItemManager.createGroundItem(new GroundItem(player, item,
					player.getLocation(), false, player.getRights() >= 2, GroundItemManager.groundItemIndex++));
		}
	}

	public void addAllDropable(Container container) {
		for (int i = 0; i < container.getSize(); i++) {
			Item item = container.get(i);
			if (item != null) {
				if (item.getDefinition().isStackable()) {
					int count = this.numberOf(item.getId());
					int count_ = item.getAmount();
					if (count + count_ < 0) {
						GroundItemManager.createGroundItem(new GroundItem(player, item, player.getLocation(), false, player.getRights() >= 2, GroundItemManager.groundItemIndex++));
						continue;
					}
				}
				if (!addItem(item)) {
					GroundItemManager.createGroundItem(new GroundItem(player, item, player.getLocation(), false, player.getRights() >= 2, GroundItemManager.groundItemIndex++));
				}
			}
		}
	}

	public boolean contains(int[] items) {
		for (int i : items) {
			if (!contains(i)) {
				return false;
			}
		}
		return true;
	}

	public boolean contains(Item... items) {
		for (Item item : items) {
			if (item != null && !contains(item.getId(), item.getAmount())) {
				return false;
			}
		}
		return true;
	}

}
