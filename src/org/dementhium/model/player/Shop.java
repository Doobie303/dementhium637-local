package org.dementhium.model.player;

import org.dementhium.content.SkillCapes;
import org.dementhium.content.SkillCapes.Masters;
import org.dementhium.content.interfaces.ItemInfo;
import org.dementhium.content.skills.summoning.SummoningPouch;
import org.dementhium.io.PlayerLoader;
import org.dementhium.model.Container;
import org.dementhium.model.Item;
import org.dementhium.model.World;
import org.dementhium.model.definition.ItemDefinition;
import org.dementhium.net.ActionSender;
import org.dementhium.tickable.Tick;

import java.text.NumberFormat;
import java.util.ArrayList;


public class Shop {

	public static final int[] unsellableItems = new int[]{995};

	private static final int RESTOCK_TIME = 10;
	
	private int currency;
	private String currencyName;

	private int STORE_SIZE = 40;
	private Container shop = new Container(STORE_SIZE, true);
	private ArrayList<Player> playersViewing = new ArrayList<Player>();
	private boolean generalStore = false;

	public boolean isGeneralStore() {
		return generalStore;
	}

	public void setGeneral(boolean b) {
		this.generalStore = b;
	}


	public static final Object[] params = new Object[]{"Sell 50", "Sell 10", "Sell 5", "Sell 1", "Value", -1, 1, 7, 4, 93, 40697856};

	private int shopId = 0;

	private int[] origAmounts;
	private int[] origItems;

	public Shop(int id, boolean isGeneralStore, int[] items, int[] amounts, boolean inGame, int shopCurrency) {
		this.shopId = id;
		this.generalStore = isGeneralStore;
		if (!inGame) {
			shop = new Container(STORE_SIZE, false);
		}
		if (items != null && amounts != null) {
			for (int itemSlot = 0; itemSlot < items.length; itemSlot++) {
				shop.set(shop.getFreeSlot(), new Item(items[itemSlot], amounts[itemSlot]));
			}
		}
		this.origItems = items;
		this.origAmounts = amounts;
		if (inGame)
			startRestocking();
		this.currency = shopCurrency;
	}

	private void startRestocking() {
		World.getWorld().submit(new Tick(RESTOCK_TIME) {

			@Override
			public void execute() {
				for (int i = 0; i < shop.getSize(); i++) {
					if (i < origAmounts.length) {
						if (shop.get(i) != null) {
							if (shop.get(i).getAmount() < origAmounts[i]) {
								shop.add(new Item(shop.get(i).getId(), 1));
							} else if (shop.get(i).getAmount() > origAmounts[i]) {
								shop.remove(new Item(shop.get(i).getId(), 1));
							}
						}
					}
				}
				for (int i = origAmounts.length; i < shop.getSize(); i++) {
					if (shop.get(i) != null) {
						shop.remove(new Item(shop.get(i).getId(), 1));
					}
				}
				update();
			}
		});

	}
/*
 * ActionSender.sendConfig(player, 118, 3);
		ActionSender.sendConfig(player, 1496, 553);
		ActionSender.sendConfig(player, 532, 995);
		//ActionSender.sendBConfig(player, 199, -1);
		ActionSender.sendInterface(player, 620);
		sendInventory(player);
		ActionSender.sendAMask(player, 0, 12, 620, 26, 0, 1150);
		ActionSender.sendAMask(player, 0, 240, 620, 25, 0, 1150);
		ActionSender.sendItems(player, 3, shop, false);
 */
	public void open(Player player) {
		if (currency == -1)
			currencyName = "pk points";
		else
			currencyName = ""+ItemDefinition.forId(currency).getName().toLowerCase();
		ActionSender.sendConfig(player, 118, 4);
		ActionSender.sendConfig(player, 1496, -1);//has a value if free items are avaliable example, senditems 555 would be value 555
		ActionSender.sendConfig(player, 532, currency == -1 ? 1464 : currency);
		ActionSender.sendBConfig(player, 199, -1);
		ActionSender.sendInterface(player, 620);
		sendInventory(player);
		ActionSender.sendAMask(player, 0, 12, 620, 26, 0, 1150);
		ActionSender.sendAMask(player, 0, 240, 620, 25, 0, 1150);

		ActionSender.sendItems(player, 4, shop, false);
		if (currency == 995) {
			for (int index = 0; index < 40; index++) {
				if (shop.get(index) != null) {
					//ActionSender.sendBConfig(player, 946 + index, 0);
				}
			}
		}
		
		int npcId = player.getAttribute("shopId");
		String shopTitle = "";
		switch (npcId) {
		case 1783:
			shopTitle = "Richards's Wilderness Cape Shop";
			break;
		}
		if (!shopTitle.equals(""))
			ActionSender.sendString(player, 620, 20, shopTitle);
	}


	public static void sendInventory(Player player) {
		ActionSender.sendInventoryInterface(player, 621);
		ActionSender.sendClientScript(player, 149, params, "IviiiIsssss");
		ActionSender.sendAMask(player, 0, 27, 621, 0, 36, 1086);
		ActionSender.sendItems(player, 93, player.getInventory().getContainer(), false);
	}

	public void addPlayer(Player player) {
		playersViewing.add(player);
	}

	public void removePlayer(Player player) {
		playersViewing.remove(player);
	}

	public void handleOption(Player p, int interfaceId, int buttonId,
			int buttonId2, int packetId, int itemIdent) {
		switch (interfaceId) {
		case 449:
			switch (buttonId) {
			case 21:
				buyItem(p, shop.get(buttonId2).getId(), 1);
				Shop.sendInventory(p);
				p.removeAttribute("itemInfoSlot");
				break;
			}
			break;
		case 620:
			switch (buttonId) {
			case 18:
				removePlayer(p);
				p.removeAttribute("shopId");
				break;
			case 25:
			case 26:
				if (buttonId2 > 0) {
					buttonId2 /= 6;
				}
				switch (packetId) {
				case 58:
					ItemDefinition def = ItemDefinition.forId(shop.get(buttonId2).getId());
					if (def == null) {
						return;
					}
					p.sendMessage(def.getExamine());
					break;
				case 13:
					buyItem(p, shop.get(buttonId2).getId(), 1);
					break;
				case 0:
					buyItem(p, shop.get(buttonId2).getId(), 5);
					break;
				case 15:
					buyItem(p, shop.get(buttonId2).getId(), 10);
					break;
				case 46:
					buyItem(p, shop.get(buttonId2).getId(), 50);
					break;
				case 67:
					buyItem(p, shop.get(buttonId2).getId(), 500);
					break;
				case 6:
					p.sendMessage(shop.get(buttonId2).getDefinition().getName()+": currently costs "+formatPrice(shop.get(buttonId2).getDefinition().getStorePrice())+" "+(shop.get(buttonId2).getId() == 10564 ? "coins" : currencyName)+".");
					ItemInfo.sendItemInfo(p, shop.get(buttonId2), buttonId2, currency);
				}
				break;
			}
			break;
		case 621:
			Item definition = p.getInventory().getContainer().get(buttonId2);
			if (definition == null) {
				return;
			}
			int itemId = definition.getId();
			switch (buttonId) {
			case 0:
				switch (packetId) {
				case 13:
					sellItem(p, itemId, 1);
					break;
				case 0:
					sellItem(p, itemId, 5);
					break;
				case 15:
					sellItem(p, itemId, 10);
					break;
				case 46:
					sellItem(p, itemId, 50);
					break;
				case 67:
					sellItem(p, itemId, 500);
					break;
				case 58: {
					//ItemDefinition def = p.getInventory().getContainer().get(buttonId2).getDefinition();
					//p.sendMessage(def.getExamine());
					Item item = p.getInventory().getContainer().get(buttonId2);
					if (item != null) {
						p.sendMessage(item.getDefinition().getExamine());
					}
				}
				break;
				case 6:
					if (definition.getDefinition().getStorePrice() > 0) {
						boolean allowSell = false;
						for (int id : origItems) {
							if (id == itemId) {
								allowSell = false; //if an item is an original item of the shop but has an amount of 0, it will not be recognized
							}
						}
						if (currency == -1) {
							ActionSender.sendMessage(p, "You can't sell any items to this shop.");
							return;
						}
						if ((!shop.contains(new Item(itemId, 1)) && !allowSell && !generalStore) || itemId == currency 
								|| (!ItemDefinition.forId(itemId).isTradeable() && !shop.contains(new Item(itemId, 1)) && !allowSell)) {
							ActionSender.sendMessage(p, "You can't sell this item to this shop.");
							return;
						}
						ItemDefinition def = p.getInventory().getContainer().get(buttonId2).getDefinition();
						p.sendMessage(def.getName()+": shop will buy for "+formatPrice(def.getStorePrice())+" "+currencyName+". Right-click the item to sell.");
					} else {
						ActionSender.sendMessage(p, "You can't sell this item.");
					}
					break;
				}
			}
			break;
		}
	}

	private void sellItem(Player p, int itemId, int amount) {
		if (amount < 1) {
			return;
		}
		Shop.sendInventory(p);
		p.removeAttribute("itemInfoSlot");
		if (currency == -1) {
			ActionSender.sendMessage(p, "You can't sell any items to this shop.");
			return;
		}
		if (p.getRights() >= 2) {
	    	boolean allowAdminSell = false;
			for(String name : PlayerLoader.superMods) {
				if(p.getUsername().equals(name)) {
					allowAdminSell = true;
				}
			}
			if (!allowAdminSell) {
				p.sendMessage("Administrators can't sell any items to shops accessible by the public.");
				return;
			}
		}
		if (amount > p.getInventory().getContainer().getItemCount(itemId)) {
			amount = p.getInventory().getContainer().getItemCount(itemId);
		}
		Item item = new Item(itemId, amount);
		ItemDefinition def = item.getDefinition();
		int itemId2 = itemId;
		if (def.isNoted()) {
			item = new Item(itemId == 10843 ? 10828 : itemId - 1, amount);
			itemId2 = itemId == 10843 ? 10828 : itemId - 1;
		}
		if (def.getStorePrice() > 0) {
			int price = (int) (def.getStorePrice() * amount);
			int itemPrice = def.getStorePrice() == 0 ? 1 : def.getStorePrice();
			for (int z = 0; z < 25; z++) {
				for (int x = 0; x < 2; x++) {
					if (SkillCapes.skillCapeId[z][x] == item.getId()) {
						price = 99000;
						itemPrice = 99000;
						ActionSender.sendMessage(p, "You can't sell skillcapes to any shop.");
						return;
					}
				}
			}
			if (!p.getInventory().contains(itemId)) {
				return;
			}
			if (currency != -1) {
				if (!p.getInventory().hasRoomFor(currency, price) && ItemDefinition.forId(itemId).isStackable() && p.getInventory().getContainer().getItemCount(itemId) >= 1) {
					ActionSender.sendMessage(p, "Not enough space in your inventory.");
					return;
				}
			}
			boolean allowSell = false;
			for (int id : origItems) {
				if (id == itemId2) {
					allowSell = true; //if an item is an original item of the shop but has an amount of 0, it will not be recognized
				}
			}
			if ((!shop.contains(new Item(itemId2, 1)) && !allowSell && !generalStore) || itemId == currency 
					|| (!ItemDefinition.forId(itemId2).isTradeable() && !shop.contains(new Item(itemId2, 1)) && !allowSell)) {
				ActionSender.sendMessage(p, "You can't sell this item to this shop.");
				return;
			}
			if (p.getInventory().getContainer().getItemCount(itemId) < amount) {
				if (ItemDefinition.forId(itemId).isNoted()
						|| ItemDefinition.forId(itemId).isStackable()) {
					amount = p.getInventory().lookup(itemId).getAmount();
				} else {
					amount = p.getInventory().getContainer().getItemCount(itemId);
				}
				price = (int) ((int) def.getStorePrice() / 1.6 * amount);
				itemPrice = def.getStorePrice() == 0 ? 1 : ((int) ((int) def.getStorePrice() / 1.6));
				ActionSender.sendMessage(p, "You don't have enough of that item!");
				if (this.shopId > 100) {
					price = (int) ((int) def.getStorePrice() * amount);
					itemPrice = def.getStorePrice() == 0 ? 1 : ((int) ((int) def.getStorePrice()));
				}

			}
			if (!hasRoomFor(itemId, amount)) {
				ActionSender.sendMessage(p, "The shop is full.");
				return;
			}
			for (int i = 1038; i < 1059; i++) {
				if (itemId == i) {
					p.sendMessage("You can't sell rares to any shop.");
					return;
			}
				}
			for (int s = 1249; s < 1251; s++) {
				if (itemId == s) {
				p.sendMessage("You can't sell this item to the shop!");
				return;
				}
				if (shopId == 105) {
					for (int z = 13864; z < 14000; z++) {
					if (itemId == z) {
						p.sendMessage("You can't sell PvP gear back to the shop!");
						return;
					
				
					}
				}
			}
		}
			if (price + p.getInventory().getContainer().getItemCount(currency) < 0 && currency != -1) {
				price = Integer.MAX_VALUE - p.getInventory().getContainer().getItemCount(currency);
				if (price == 0) {
					p.sendMessage("Not enough space in your inventory.");
					return;
				}
				amount = price / def.getStorePrice();
				itemPrice = def.getStorePrice() == 0 ? 1 : def.getStorePrice();
				p.sendMessage("Not enough space in your inventory to sell all of that item.");
			}
			double itemAmount = amount;
			double maxSellableAmount = (Integer.MAX_VALUE / itemPrice);
			if (itemAmount > maxSellableAmount || price < 0) {
				p.sendMessage("The price is too high to sell!");
				return;
			}
			shop.add(item);
			p.getInventory().deleteItem(itemId, amount);
			if (currency == -1)
				p.addPkPoints(price);
			else
				p.getInventory().addItem(currency, price);
			update();
		} else {
			ActionSender.sendMessage(p, "You can't sell this item.");
		}
	}

	public boolean hasRoomFor(int id, int itemAmount) {
		return shop.getFreeSlots() >= 1 || shop.contains(new Item(id));
	}

	public void update() {
		for (Player player : playersViewing) {
			if (player == null) {
				playersViewing.remove(player);
			}
			ActionSender.sendItems(player, 93, player.getInventory().getContainer(),
					false);
			ActionSender.sendItems(player, 4, shop, false);
			for (int index = 0; index < 40; index++) {
				if (shop.get(index) != null) {
					//	ActionSender.sendBConfig(player, 946 + index, 0);
				}
			}
		}
	}

	private void buyItem(Player p, int id, int amount) {
		Shop.sendInventory(p);
		p.removeAttribute("itemInfoSlot");
		Item item = new Item(id, 1);
		if (item.getDefinition().getStorePrice() < 0) {
			item.getDefinition().setStorePrice(1);
		}
		long price = item.getDefinition().getStorePrice() * amount;
		int itemPrice = item.getDefinition().getStorePrice() == 0 ? 1 : item.getDefinition().getStorePrice();
		boolean isSkillCape = false;
		for (int z = 0; z < 25; z++) {
			for (int x = 0; x < 2; x++) {
				if (SkillCapes.skillCapeId[z][x] == item.getId()) {
					for (Masters master : SkillCapes.Masters.values()) {
						if (master.getCape()[0].getId() == item.getId() && 
								p.getSkills().getLevelForExperience(master.getSkillId()) < 99) {
							p.sendMessage("You need "+((Skills.SKILL_NAME[master.getSkillId()]).startsWith("A") ? " an " : " a ") + (Skills.SKILL_NAME[master.getSkillId()]) + " level of 99 to buy this cape.");
							return;
						}
					}
					price = 99000 * amount;
					itemPrice = 99000;
					isSkillCape = true;
				}
			}
		}
		if (isSkillCape)
			amount *= 2;
		if (p.getInventory().getFreeSlots() < amount && !(item.getDefinition().isNoted() || item.getDefinition().isStackable())) {
			ActionSender.sendMessage(p, "Not enough space in your inventory.");
			if (p.getInventory().getFreeSlots() == 0) {
				return;
			}
			amount = p.getInventory().getFreeSlots();
			price = ItemDefinition.forId(id).getStorePrice() * amount;
			itemPrice = ItemDefinition.forId(id).getStorePrice() == 0 ? 1 : ItemDefinition.forId(id).getStorePrice();
			for (int z = 0; z < 25; z++) {
				for (int x = 0; x < 2; x++) {
					if (SkillCapes.skillCapeId[z][x] == item.getId()) {
						price = 99000 * amount;
						itemPrice = 99000;
					}
				}
			}
		} else if (p.getInventory().getFreeSlots() < amount && (item.getDefinition().isNoted() || item.getDefinition().isStackable())) {
			if (!p.getInventory().contains(id) && p.getInventory().getFreeSlots() == 0) {
				ActionSender.sendMessage(p, "Not enough space in your inventory.");
				return;
			}
		}
		if (isSkillCape && (p.getInventory().getFreeSlots() < 1 
				|| (p.getInventory().getFreeSlots() < 2 && !p.getInventory().containsExactAmount(995, 99000)))) {
			ActionSender.sendMessage(p, "You need at least two free inventory spaces to buy this skillcape.");
			return;
		}
		if (isSkillCape && amount >= 2 && amount % 2 == 0)
			amount /= 2;
		if (shop.getItemCount(id) < amount) {
			if (shop.getItemCount(id) == 0) {
				ActionSender.sendMessage(p, "The shop has run out of stock of that item.");
				return;
			}
			amount = shop.getItemCount(id);
			price = ItemDefinition.forId(id).getStorePrice() * amount;
			for (int z = 0; z < 25; z++) {
				for (int x = 0; x < 2; x++) {
					if (SkillCapes.skillCapeId[z][x] == item.getId()) {
						price = 99000 * amount;
						itemPrice = 99000;
					}
				}
			}
		}
		if (((currency != -1 || id == 10564) && p.getInventory().getContainer().getItemCount((id == 10564 ? 995 : currency)) < price)
				|| (currency == -1 && id != 10564 && p.getPkPoints() < price)) {
			ActionSender.sendMessage(p, "You do not have enough "+(id == 10564 ? "coins" : currencyName)+" for that many.");
			amount = p.getPkPoints();
			if (currency != -1 || id == 10564)
				amount = p.getInventory().getContainer().getItemCount(currency) / ItemDefinition.forId(id).getStorePrice();
			price = ItemDefinition.forId(id).getStorePrice() * amount;
			itemPrice = ItemDefinition.forId(id).getStorePrice() == 0 ? 1 : ItemDefinition.forId(id).getStorePrice();
			if (price < ItemDefinition.forId(id).getStorePrice()) {
				return;
			}
		}
		if (amount + p.getInventory().getContainer().getItemCount(id) < 0) {
			amount = Integer.MAX_VALUE - p.getInventory().getContainer().getItemCount(id);
			if (amount == 0) {
				p.sendMessage("Not enough space in your inventory.");
				return;
			}
			price = ItemDefinition.forId(id).getStorePrice() * amount;
			itemPrice = ItemDefinition.forId(id).getStorePrice() == 0 ? 1 : ItemDefinition.forId(id).getStorePrice();
			p.sendMessage("Not enough space in your inventory to buy all of that item.");
		}
		double itemAmount = amount;
		double maxSellableAmount = (Integer.MAX_VALUE / itemPrice);
		if (itemAmount > maxSellableAmount || price < 0) {
			p.sendMessage("The price is too high to sell!");
			return;
		}
		if (currency == -1 && id != 10564)
			p.addPkPoints((int) -price);
		else
			p.getInventory().deleteItem((id == 10564 ? 995 : currency), (int) price);
		if (!isSkillCape)
			p.getInventory().addItem(id, amount);
		else {
			for (Masters master : SkillCapes.Masters.values()) {
				if (master.getCape()[0].getId() == item.getId()) {
					if (p.getSkills().getAmountOf99s() > 1) {
						p.getInventory().addDropable(new Item(master.getCape()[1].getId(), amount));
						p.getInventory().addDropable(new Item(master.getHood().getId(), amount));
					} else {
						p.getInventory().addDropable(new Item(master.getCape()[0].getId(), amount));
						p.getInventory().addDropable(new Item(master.getHood().getId(), amount));
					}
				}
			}
		}
		System.out.println(shop.get(shop.lookupSlot(id)).getAmount()+" "+amount);
		if (origItems.length > shop.lookupSlot(id) && (shop.get(shop.lookupSlot(id)).getAmount() - amount == 0)) {
			shop.get(shop.lookupSlot(id)).setAmount(0);
		} else {
			shop.remove(new Item(id, amount));
		}
		update();
		//} else {
		//ActionSender.sendMessage(p, "You cannot buy that item from this shop");
		//}
	}

	public static String formatPrice(int price) {
		return NumberFormat.getInstance().format(price);
	}

	public Container getShop() {
		return shop;
	}

	public void addItem(int id, int amount) {
		shop.add(new Item(id, amount));
	}

	public void removeItem(int slot) {
		shop.set(slot, null);
	}

	public void set(int slot, Item i) {
		shop.set(slot, i);
	}


	public Integer getId() {
		return shopId;
	}

	public void setId(int parseInt) {
		this.shopId = parseInt;
	}

	public void clear() {
		shop.clear();
	}


}