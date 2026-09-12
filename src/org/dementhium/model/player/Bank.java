package org.dementhium.model.player;

import org.dementhium.model.Container;
import org.dementhium.model.Item;
import org.dementhium.model.definition.ItemDefinition;
import org.dementhium.net.ActionSender;
import org.dementhium.util.Constants;
import org.dementhium.util.InputHandler;

public class Bank {

	public static int SIZE = 516;
	public static final int FREE_SIZE = 68;
	public static int TAB_SIZE = 11;

	private final Container bank = new Container(SIZE, true);
	private final Player player;
	private final int[] tabStartSlot = new int[TAB_SIZE];

	private boolean checkingBank = false;
	private Bank inspectedBank;
	private int inspectedTab = 10;
	public Bank(Player player) {
		this.player = player;
	}

	public void openBank() {
		player.removeAttribute("bankInputItem");
		//if (player.getTradeSession() != null) {
			//player.getTradeSession().tradeFailed(player);
		//}
		player.closeAll(false, true);
		//player.stopAll();
		if (player.getAttribute("cantMove") == Boolean.TRUE) {
            return;
        }
		checkingBank = false;
		inspectedBank = null;
		normalizeLayout();
		if (!canDisplay()) return;
		player.setAttribute("inBank", Boolean.TRUE);
		player.setAttribute("bankScreen", 2);
		player.setAttribute("noting", false);
		ActionSender.sendItems(player, 93, player.getInventory().getContainer(), false);
		ActionSender.sendItems(player, 95, bank, false);
		ActionSender.sendConfig(player, 563, 4194304);
		ActionSender.sendConfig(player, 115, 0);
		//resets noting config
		//ActionSender.sendConfig(player, 1248, -2013265920);
		//Amask 194: 27 195: 4261378 196: 50003970 197: 0
		/*Amask 194: 516 195: 2622718 196: 49938525 197: 0
		Amask 194: 27 195: 2361214 196: 50003968 197: 0
		Amask 194: 14 195: 4261378 196: 43712519 197: 0
		Amask 194: 27 195: 4261378 196: 50003970 197: 0*/

		/*
		 * bldr.writeInt2(interfaceId2 << 16 | childId2);
		bldr.writeShortA(set2);
		bldr.writeShortA(set1);
		bldr.writeLEInt(interfaceId1 << 16 | childId1);
		player.write(bldr.toMessage());

		 */
		ActionSender.sendInterface(player, 762);
		ActionSender.sendInventoryInterface(player, 763);
		// Replacing a mounted interface clears its access masks in the client.
		// Install permissions only after both panels have been attached.
		ActionSender.sendAMask(player, 0, SIZE - 1, 762, 93, 40, 1278);
		ActionSender.sendAMask(player, 0, Inventory.SIZE - 1, 763, 0, 37, 1150);
		ActionSender.sendBlankClientScript(player, 1451);
		sendBankSpace();
		ActionSender.sendString(player, 762, 45, "Bank of "+Constants.SERVER_NAME);
		sendTabConfig();
		//ActionSender.sendConfig(player, 1248, player.getAttribute("currentTabConfig", -2013265920)); //Sends the currently viewed tab <3
	}

	public void openPlayerBank(Player victim) {
		if (victim == null) {
			return;
		}
		//if (player.getTradeSession() != null) {
			//player.getTradeSession().tradeFailed(player);
		//}
		player.closeAll(false, true);
		//player.stopAll();
		checkingBank = true;
		inspectedBank = victim.getBank();
		inspectedTab = 10;
		player.removeAttribute("bankInputItem");
		if (!canDisplay()) return;
		player.setAttribute("inBank", Boolean.TRUE);
		player.setAttribute("bankScreen", 2);
		player.setAttribute("noting", false);
		ActionSender.sendItems(player, 93, victim.getInventory().getContainer(), false);
		ActionSender.sendItems(player, 95, victim.getBank().getContainer(), false);
		ActionSender.sendConfig(player, 563, 4194304);
		ActionSender.sendConfig(player, 1248, -2013265920);
		ActionSender.sendConfig(player, 115, 0); //resets noting config
		ActionSender.sendInterface(player, 762);
		ActionSender.sendInventoryInterface(player, 763);
		ActionSender.sendAMask(player, 0, SIZE - 1, 762, 93, 40, 1278);
		ActionSender.sendAMask(player, 0, Inventory.SIZE - 1, 763, 0, 37, 1150);
		ActionSender.sendBlankClientScript(player, 1451);
		sendBankSpace();
		sendTabConfig();
	}

	public void addItem(int slot, int amount) {
		addItem(slot, amount, true);
	}

	public void addItem(int slot, int amount, boolean refresh) {
		if (!isOpen() || amount <= 0 || !validItem(player.getInventory().get(slot))) {
			return;
		}
		ActionSender.sendCloseChatBox(player);
		Item selected = player.getInventory().get(slot);
		int remaining = amount;
		remaining -= depositSlot(player.getInventory().getContainer(), slot, remaining);
		for (int i = 0; i < Inventory.SIZE && remaining > 0; i++) {
			if (i == slot) continue;
			Item item = player.getInventory().get(i);
			if (item != null && item.getId() == selected.getId()) {
				remaining -= depositSlot(player.getInventory().getContainer(), i, remaining);
			}
		}
		if (remaining == amount) {
			player.sendMessage("You don't have enough bank space left to bank this item.");
		}
		if (refresh) {
			player.getInventory().refresh();
			refresh();
		}
	}

	public boolean isOpen() {
		return !checkingBank && Boolean.TRUE.equals(player.getAttribute("inBank", Boolean.FALSE));
	}

	private static boolean validItem(Item item) {
		return item != null && item.getId() >= 0 && item.getId() < ItemDefinition.MAX_SIZE
				&& item.getHash() >= 0 && item.getAmount() > 0;
	}

	private boolean validContents() {
		for (Item item : bank.toArray()) {
			if (item != null && !validItem(item)) {
				return false;
			}
		}
		return true;
	}

	private boolean canDisplay() {
		if (displayedBank().validContents()) return true;
		player.removeAttribute("inBank");
		player.sendMessage("Your bank contains invalid item data. Please contact an administrator.");
		return false;
	}

	private Bank displayedBank() {
		return checkingBank && inspectedBank != null ? inspectedBank : this;
	}

	/** Resolve the reciprocal cache link, never an adjacent ID/name guess. */
	private int bankItemId(Item item) {
		if (!item.getDefinition().isNoted() || item.getHealth() > 0) return item.getId();
		int id = item.getDefinition().getCacheDefinition().getCertId();
		if (id < 0 || id >= ItemDefinition.MAX_SIZE) return -1;
		ItemDefinition unnoted = ItemDefinition.forId(id);
		return !unnoted.isNoted() && unnoted.getCacheDefinition().getCertId() == item.getId()
				? id : -1;
	}

	/** Prepare capacity and representation before committing either container. */
	private int depositSlot(Container source, int slot, int requested) {
		Item item = source.get(slot);
		if (!validItem(item) || requested <= 0) return 0;
		int id = bankItemId(item);
		if (id < 0) return 0;
		int index = -1;
		if (item.getHealth() == 0) {
			for (int i = 0; i < SIZE; i++) {
				Item existing = bank.get(i);
				if (existing != null && existing.getId() == id && existing.getHealth() == 0) {
					index = i;
					break;
				}
			}
		}
		int amount = Math.min(requested, item.getAmount());
		int free = bank.freeSlot();
		if (index >= 0) {
			amount = Math.min(amount, Integer.MAX_VALUE - bank.get(index).getAmount());
		} else if (free < 0) {
			return 0;
		}
		if (amount <= 0) return 0;
		Item addition = new Item(id, amount);
		addition.setHealth(item.getHealth());
		if (index >= 0) {
			addition.setAmount(bank.get(index).getAmount() + amount);
			bank.set(index, addition);
		} else {
			int tab = player.getLastBankTab();
			if (tab < 2 || tab > 10) tab = 10;
			int destination = tab == 10 ? free : tabStartSlot[tab] + getItemsInTab(tab);
			if (destination < 0 || destination > free) return 0;
			insert(free, destination);
			bank.set(destination, addition);
			increaseTabStartSlots(tab);
		}
		if (amount == item.getAmount()) source.set(slot, null);
		else {
			Item remainder = new Item(item);
			remainder.setAmount(item.getAmount() - amount);
			source.set(slot, remainder);
		}
		return amount;
	}

	/** Familiar scrolls bank remotely without impersonating an open bank interface. */
	public boolean depositFromFamiliar(int slot) {
		if ((checkingBank && Boolean.TRUE.equals(player.getAttribute("inBank", false)))
				|| depositSlot(player.getInventory().getContainer(), slot, 1) != 1) return false;
		player.getInventory().refresh();
		if (isOpen()) refresh();
		return true;
	}

	public void refresh() {
		if (!checkingBank) normalizeLayout();
		if (!canDisplay()) return;
		ActionSender.sendItems(player, 95, displayedBank().bank, false);
		sendBankSpace();
		sendTabConfig();
	}

	/** Refresh the server-owned slot counters when changing bank tabs. */
	public void refreshBankSpace() {
		if (Boolean.TRUE.equals(player.getAttribute("inBank", Boolean.FALSE))) {
			sendBankSpace();
		}
	}

	private void sendBankSpace() {
		// Own these labels on the server. Updating varcs 192/1038 would queue
		// native script 1465, replacing our total with a members-only count
		// and its stock 438-slot limit. Do not trigger it or race it with a timer.
		// These are hidden in the cache; changing child text does not show them.
		// The legacy boolean parameter means visible, despite its name.
		ActionSender.sendInterfaceConfig(player, 762, 22, true);
		ActionSender.sendInterfaceConfig(player, 762, 23, false);
		ActionSender.sendString(player, 762, 29, Integer.toString(displayedBank().getFreeToPlayItemCount()));
		ActionSender.sendString(player, 762, 30, Integer.toString(FREE_SIZE));
		ActionSender.sendString(player, 762, 31, Integer.toString(displayedBank().bank.size()));
		ActionSender.sendString(player, 762, 32, Integer.toString(SIZE));
	}

	private int getFreeToPlayItemCount() {
		int count = 0;
		for (Item item : bank.toArray()) {
			if (validItem(item) && !item.getDefinition().getCacheDefinition().isMembersOnly()) {
				count++;
			}
		}
		return count;
	}

	/** Repair holes left by administrative removals without discarding any item. */
	private void normalizeLayout() {
		boolean valid = tabStartSlot[0] == 0 && tabStartSlot[1] == 0 && tabStartSlot[2] == 0;
		for (int i = 3; i < TAB_SIZE; i++) {
			valid &= tabStartSlot[i] >= tabStartSlot[i - 1] && tabStartSlot[i] <= SIZE;
		}
		int selected = player.getLastBankTab();
		if (selected < 2 || selected > 10) player.setLastBankTab(10);
		int free = bank.freeSlot();
		boolean holes = free >= 0 && free < bank.size();
		if (valid && !holes && tabStartSlot[10] <= bank.size()) return;
		int[] counts = new int[TAB_SIZE];
		if (valid) {
			for (int slot = 0; slot < SIZE; slot++) {
				if (bank.get(slot) != null) counts[getTabByItemSlot(slot)]++;
			}
		}
		bank.shift();
		java.util.Arrays.fill(tabStartSlot, 0);
		int next = 2, start = 0, viewed = 10;
		for (int tab = 2; tab < 10; tab++) {
			if (counts[tab] == 0) continue;
			tabStartSlot[next] = start;
			start += counts[tab];
			if (selected == tab) viewed = next;
			next++;
		}
		while (next < TAB_SIZE) tabStartSlot[next++] = start;
		player.setLastBankTab(viewed);
	}

	public void commandAdd(int id, int amount, int tab) {
		if (amount <= 0 || id < 0 || id >= ItemDefinition.MAX_SIZE) {
			return;
		}
		Item item = new Item(id, amount);
		int index = -1;
		for (int i = 0; i < SIZE; i++) {
			Item existing = bank.get(i);
			if (existing != null && existing.getId() == id && existing.getHealth() == 0) { index = i; break; }
		}
		if (index > -1) {
			Item existing = bank.get(index);
			if (existing != null && existing.getId() == id) {
				long total = (long) existing.getAmount() + amount;
				if (total > Integer.MAX_VALUE) {
					total = Integer.MAX_VALUE;
				}
				bank.set(index, new Item(id, (int) total));
				return;
			}
		}
		int free = bank.freeSlot();
		if (free < 0) return;
		int dest;
		if (tab < 2 || tab >= 10) {
			dest = free;
		} else {
			dest = tabStartSlot[tab] + getItemsInTab(tab);
			if (dest < 0 || dest > free) return;
			insert(free, dest);
			increaseTabStartSlots(tab);
		}
		if (dest < 0) {
			return;
		}
		bank.set(dest, item);
	}

	public void removeItem(int slot, int amount) {
		if (!isOpen() || amount <= 0 || slot < 0 || slot >= SIZE) return;
		Item stored = bank.get(slot);
		if (!validItem(stored)) return;
		ActionSender.sendCloseChatBox(player);
		int id = stored.getId();
		if (noting() && stored.getHealth() == 0 && !stored.getDefinition().isNoted()) {
			int note = stored.getDefinition().getCacheDefinition().getCertId();
			if (note >= 0 && note < ItemDefinition.MAX_SIZE
					&& ItemDefinition.forId(note).isNoted()
					&& ItemDefinition.forId(note).getCacheDefinition().getCertId() == id) {
				id = note;
			} else {
				player.sendMessage("You cannot withdraw this item as a note.");
				return;
			}
		}
		amount = Math.min(amount, stored.getAmount());
		Container inventory = player.getInventory().getContainer();
		Item withdrawn = new Item(id, amount);
		withdrawn.setHealth(stored.getHealth());
		if (withdrawn.getDefinition().isStackable()) {
			long held = 0;
			for (Item item : inventory.toArray()) {
				if (item != null && item.getId() == id && item.getHealth() == withdrawn.getHealth()) held += item.getAmount();
			}
			amount = (int) Math.min(amount, Math.max(0L, Integer.MAX_VALUE - held));
		} else {
			amount = Math.min(amount, inventory.freeSlots());
		}
		if (amount <= 0) {
			player.sendMessage("You don't have enough inventory space to withdraw that many.");
			return;
		}
		withdrawn.setAmount(amount);
		Container addition = new Container(1, false);
		addition.set(0, withdrawn);
		if (!inventory.tryAddAll(addition)) {
			player.sendMessage("Not enough space in your inventory.");
			return;
		}
		if (amount == stored.getAmount()) {
			int tab = getTabByItemSlot(slot);
			bank.set(slot, null);
			bank.shift();
			decreaseTabStartSlots(tab);
		} else {
			Item remainder = new Item(stored);
			remainder.setAmount(stored.getAmount() - amount);
			bank.set(slot, remainder);
		}
		player.getInventory().refresh();
		refresh();
	}

	public boolean noting() {
		return player.getAttribute("noting") == Boolean.TRUE;
	}

	public void selectTab(int tab) {
		if (tab < 2 || tab > 10 || !Boolean.TRUE.equals(player.getAttribute("inBank", false))) return;
		if (checkingBank) inspectedTab = tab;
		else player.setLastBankTab(tab);
	}

	public boolean matchesItem(int slot, int id, boolean inventory) {
		Item item = inventory ? player.getInventory().get(slot) : bank.get(slot);
		return isOpen() && validItem(item) && item.getId() == id;
	}

	public void requestAmount(int slot, boolean inventory) {
		Item item = inventory ? player.getInventory().get(slot) : bank.get(slot);
		if (!isOpen() || !validItem(item)) return;
		InputHandler.requestIntegerInput(player, inventory ? 4 : 3, "Please enter an amount:");
		player.setAttribute("slotId", slot);
		player.setAttribute("bankInputItem", new Item(item));
	}

	public void submitAmount(int slot, int amount, boolean inventory) {
		Item expected = player.getAttribute("bankInputItem", null);
		player.removeAttribute("bankInputItem");
		Item current = inventory ? player.getInventory().get(slot) : bank.get(slot);
		if (!isOpen() || amount <= 0 || expected == null || !validItem(current)
				|| current.getId() != expected.getId() || current.getHash() != expected.getHash()) return;
		player.getSettings().setLastXAmount(amount);
		ActionSender.sendConfig(player, 1249, amount);
		if (inventory) addItem(slot, amount);
		else removeItem(slot, amount);
	}

	/**
	 * Banks the inventory items.
	 */
	public void bankInv() {
		if (checkingBank || player.getAttribute("inBank", Boolean.FALSE) == Boolean.FALSE) {
			return;
		}
		if (player.getInventory().getContainer().size() < 1) {
			player.sendMessage("You don't have any items to bank.");
			return;
		}
		for (int i = 0; i < Inventory.SIZE; i++) {
			Item item = player.getInventory().get(i);
			if (item != null) {
				addItem(i, item.getAmount(), false);
			}
		}
		refresh();
		player.getInventory().refresh();
	}

	/**
	 * Banks all the equipped items.
	 */
	public void bankEquip() {
		if (checkingBank || player.getAttribute("inBank", Boolean.FALSE) == Boolean.FALSE) {
			return;
		}
		if (player.getEquipment().getContainer().size() < 1) {
			player.sendMessage("You're not wearing anything to bank.");
			return;
		}
		bankItems(player.getEquipment().getContainer());
		refresh();
		player.getEquipment().recalculateHpModifier();
		player.getEquipment().refresh();
	}

	/**
	 * Banks all of the beast of burden's items.
	 */
	public void bankBob() {
		if (checkingBank || player.getAttribute("inBank", Boolean.FALSE) == Boolean.FALSE) {
			return;
		}
		if (player.getFamiliar() == null) {
			player.sendMessage("You do not have a familiar following you.");
			return;
		}
		if (!player.getFamiliar().isBeastOfBurden()) {
			player.sendMessage("Your familiar is not a beast of burden.");
			return;
		} else if (player.getFamiliar().getContainer().size() < 1) {
			player.sendMessage("Your beast of burden is not carying any items.");
			return;
		}
		bankItems(player.getFamiliar().getContainer());
		player.getFamiliar().refresh(false);
		refresh();
	}

	/**
	 * Banks all the items from a certain container.
	 * @param container The container.
	 * @return {@code True}.
	 */
	private boolean bankItems(Container container) {
		boolean complete = true;
		for (int i = 0; i < container.getSize(); i++) {
			Item item = container.get(i);
			if (item != null && (!validItem(item) || depositSlot(container, i, item.getAmount()) != item.getAmount())) {
				complete = false;
			}
		}
		if (!complete) player.sendMessage("There wasn't enough space to add all of your items to your bank.");
		return complete;
	}

	public boolean contains(int item, int amount) {
		return bank.contains(new Item(item, amount));
	}

	public boolean contains(int item) {
		return bank.contains(new Item(item));
	}

	public Container getContainer() {
		return bank;
	}

	public Item get(int slot) {
		return bank.get(slot);
	}

	public void set(int slot, Item item) {
		bank.set(slot, item);
	}

	public void increaseTabStartSlots(int startId) {
		if (startId < 2 || startId >= 10) return;
		for (int i = startId + 1; i < tabStartSlot.length; i++) {
			tabStartSlot[i]++;
		}
	}

	public void decreaseTabStartSlots(int startId) {
		if (startId < 2 || startId >= 10)
			return;
		for (int i = startId + 1; i < tabStartSlot.length; i++) {
			tabStartSlot[i]--;
		}
		if (getItemsInTab(startId) == 0) {
			collapseTab(startId);
		}
	}

	public void insert(int fromId, int toId) {
		if (fromId < 0 || fromId >= SIZE || toId < 0 || toId >= SIZE) return;
		Item temp = bank.toArray()[fromId];
		if (toId > fromId) {
			for (int i = fromId; i < toId; i++) {
				set(i, get(i + 1));
			}
		} else if (fromId > toId) {
			for (int i = fromId; i > toId; i--) {
				set(i, get(i - 1));
			}
		}
		set(toId, temp);
	}

	public int getItemsInTab(int tabId) {
		if (tabId < 2 || tabId >= 10) return 0;
		return tabStartSlot[tabId + 1] - tabStartSlot[tabId];
	}

	public int getTabByItemSlot(int itemSlot) {
		int tabId = 0;
		for (int i = 0; i < tabStartSlot.length; i++) {
			if (itemSlot >= tabStartSlot[i]) {
				tabId = i;
			}
		}
		return tabId;
	}

	public void collapseTab(int tabId) {
		if (!isOpen() || tabId < 2 || tabId >= 10) {
			return;
		}
		int size = getItemsInTab(tabId);
		Item[] tempTabItems = new Item[size];
		for (int i = 0; i < size; i++) {
			tempTabItems[i] = get(tabStartSlot[tabId] + i);
			set(tabStartSlot[tabId] + i, null);
		}
		bank.shift();
		for (int i = tabId; i < tabStartSlot.length - 1; i++) {
			tabStartSlot[i] = tabStartSlot[i + 1] - size;
		}
		tabStartSlot[10] = tabStartSlot[10] - size;
		for (int i = 0; i < size; i++) {
			int slot = bank.getFreeSlot();
			set(slot, tempTabItems[i]);
		}
		int selected = player.getLastBankTab();
		if (selected == tabId) player.setLastBankTab(10);
		else if (selected > tabId && selected < 10) player.setLastBankTab(selected - 1);
	}

	public void moveToTab(int from, int tab) {
		if (!isOpen() || !validItem(bank.get(from)) || tab < 2 || tab > 10) return;
		int fromTab = getTabByItemSlot(from);
		int end = tab == 10 ? bank.size() : tabStartSlot[tab] + getItemsInTab(tab);
		insert(from, end > from ? end - 1 : end);
		if (fromTab != tab) {
			increaseTabStartSlots(tab);
			decreaseTabStartSlots(fromTab);
		}
		refresh();
	}

	public void moveItem(int from, int to, boolean inserting) {
		if (!isOpen() || !validItem(bank.get(from)) || !validItem(bank.get(to)) || from == to) return;
		if (inserting) {
			int fromTab = getTabByItemSlot(from), toTab = getTabByItemSlot(to);
			// Item drops target an occupied slot, not the boundary before it.
			insert(from, to);
			if (fromTab != toTab) {
				increaseTabStartSlots(toTab);
				decreaseTabStartSlots(fromTab);
			}
		} else {
			Item item = bank.get(from);
			bank.set(from, bank.get(to));
			bank.set(to, item);
		}
		refresh();
	}

	public void sendTabConfig() {
		Bank contents = displayedBank();
		int config = 0;
		config += contents.getItemsInTab(2);
		config += contents.getItemsInTab(3) << 10;
		config += contents.getItemsInTab(4) << 20;
		ActionSender.sendConfig(player, 1246, config);
		config = 0;
		config += contents.getItemsInTab(5);
		config += contents.getItemsInTab(6) << 10;
		config += contents.getItemsInTab(7) << 20;
		ActionSender.sendConfig(player, 1247, config);
		int tab = checkingBank ? inspectedTab : player.getLastBankTab();
		config = -2013265920;
		config += (134217728 * (tab == 10 ? 0 : tab - 1));
		config += contents.getItemsInTab(8);
		config += contents.getItemsInTab(9) << 10;
		ActionSender.sendConfig(player, 1248, config);
	}

	public static int getArrayIndex(int tabId) {
		if (tabId == 62 || tabId == 74) {
			return 10;
		}
		int base = 60;
		for (int i = 2; i < 10; i++) {
			if (tabId == base) {
				return i;
			}
			base -= 2;
		}
		base = 75;
		for (int i = 2; i < 10; i++) {
			if (tabId == base) {
				return i;
			}
			base++;
		}
		//Should not happen
		return -1;
	}
	
	/**
	 * Gets the config value for setting the currently viewed tab.
	 * @param buttonId The button id.
	 * @return The config value.
	 */
	public static int getViewedTabConfig(int buttonId) {
		if (buttonId == 46) {
			return -939499493;
		} else if (buttonId == 48) {
			return -1073717221;
		} else if (buttonId == 50) {
			return -1207934949;
		} else if (buttonId == 52) {
			return -1342152677;
		} else if (buttonId == 54) {
			return -1476370405;
		} else if (buttonId == 56) {
			return -1610588133;
		} else if (buttonId == 58) {
			return -1744805861;
		} else if (buttonId == 60) {
			return -1879023589;
		}
		return -2013241317;
	}

	public int[] getTab() {
		return tabStartSlot;
	}

	public boolean isCheckingBank() {
		return checkingBank;
	}
}
