package org.dementhium.content.interfaces;

import org.dementhium.content.skills.Prayer;
import org.dementhium.event.EventListener;
import org.dementhium.event.EventManager;
import org.dementhium.model.Container;
import org.dementhium.model.Item;
import org.dementhium.model.definition.ItemDefinition;
import org.dementhium.model.player.Equipment;
import org.dementhium.model.player.Inventory;
import org.dementhium.model.player.Player;
import org.dementhium.net.ActionSender;

/**
 * @author 'Mystic Flow <Steven@rune-server.org>
 */
public class ItemsKeptOnDeath {

    public static class EquipmentInterfaceListener extends EventListener {

        @Override
        public void register(EventManager manager) {
            manager.registerInterfaceListener(387, this);
        }

        public boolean interfaceOption(Player player, int interfaceId, int buttonId, int slot, int itemId, int opcode) {
            if (buttonId == 45) {
                displayInterface(player);
                return true;
            }
            return false;
        }

    }

    /*private static final Comparator<Item> PRICE_ORDER = new Comparator<Item>() {
        @Override
        public int compare(Item o1, Item o2) {
            if (o1 != null && o2 == null) {
                return 1;
            } else if (o1 == null && o2 != null) {
                return -1;
            } else if (o1 == null && o2 == null) {
                return 0;
            }
            if (o1.getDefinition().getStorePrice() > o2.getDefinition().getStorePrice()) {
                return 1;
            } else if (o1.getDefinition().getStorePrice() < o2.getDefinition().getStorePrice()) {
                return -1;
            }
            return 0;
        }
    };*/

    public static void displayInterface(Player player) {
        int allowedAmount = allowedAmount(player);
        int carriedWealth = getCarriedWealth(player);
        Container[] itemData = getDeathContainers(player);
        int riskedWealth = getRiskedWealth(itemData[1]);

        int displayType = 0;

        //I had this wrong =p
        //		if(player.inWilderness()) {
        //			displayType = 0;
        //		}
        Container keptItems = new Container(itemData[0].size(), false, true);
        for (Item item : itemData[0].toArray()) {
            if (item != null) {
                keptItems.add(item);
            }
        }
        keptItems.shift();
        ItemsKeptOnDeath.sendPacket(player, allowedAmount, riskedWealth, carriedWealth, player.getFamiliar() == null ? false : player.getFamiliar().isBeastOfBurden(), player.getSkullManager().isSkulled(), displayType, keptItems);
    }

    /**
     * @return Two containers, one being the items you keep on death, and the second being the lost items.
     * @author Michael (Scu11)
     * Gets two containers, one being the items you keep on death, and the second being the lost items.
     */
    /*public static Container[] getKeptItems(Player player, int allowed) {
        Container topItems = new Container(allowed, false, true);
        Container clonedInventory = player.getInventory().getContainer().clone();
        Container clonedEquipment = player.getEquipment().getContainer().clone();
        for (int i = 0; i < Inventory.SIZE; i++) {
            Item item = clonedInventory.get(i);
            if (item != null) {
                item = new Item(clonedInventory.get(i).getId(), 1);
                for (int k = 0; k < allowed; k++) {
                    Item topItem = topItems.get(k);
                    if (topItem == null || item.getDefinition().getStorePrice() > topItem.getDefinition().getStorePrice()) {
                        if (topItem != null) {
                            topItems.remove(topItem);
                        }
                        topItems.add(item);
                        clonedInventory.remove(item);
                        if (topItem != null) {
                            clonedInventory.add(topItem);
                        }
                        if (item.getDefinition().isStackable() && clonedInventory.getItemCount(item.getId()) > 0) {
                            i--;
                        }
                        break;
                    }
                }
            }
        }
        for (int i = 0; i < Equipment.SIZE; i++) {
            Item item = clonedEquipment.get(i);
            if (item != null) {
                item = new Item(clonedEquipment.get(i).getId(), 1);
                for (int k = 0; k < allowed; k++) {
                    int lowest = -1;
                    int lowestSlot = -1;
                    //This fixes the bug with inv
                    for (int j = 0; j < topItems.size(); j++) {
                        if (topItems.get(j) != null) {
                            if (lowest == -1 || lowest > topItems.get(j).getDefinition().getStorePrice()) {
                                lowest = topItems.get(j).getDefinition().getStorePrice();
                                lowestSlot = j;
                            }
                        } else {
                            lowest = -1;
                            lowestSlot = j;
                        }
                    }
                    Item topItem = topItems.get(lowestSlot);
                    if (topItem == null || item.getDefinition().getStorePrice() > topItem.getDefinition().getStorePrice()) {
                        if (topItem != null) {
                            topItems.remove(topItem);
                        }
                        topItems.add(item);
                        clonedEquipment.remove(item);
                        if (topItem != null) {
                            clonedEquipment.add(topItem);
                        }
                        if (item.getDefinition().isStackable() && clonedEquipment.getItemCount(item.getId()) > 0) {
                            i--;
                        }
                        break;
                    }
                }
            }
        }
        Container lostItems = new Container(Inventory.SIZE + Equipment.SIZE, false);
        for (Item lostItem : clonedInventory.toArray()) {
            if (lostItem != null) {
                lostItems.add(lostItem);
            }
        }
        for (Item lostItem : clonedEquipment.toArray()) {
            if (lostItem != null) {
                lostItems.add(lostItem);
            }
        }
        topItems.sort(PRICE_ORDER);
        return new Container[]{topItems, lostItems};
    }*/
    
	/**
	 * Gets the death containers.<br>
	 * 1 is items kept, 2 is items lost.
	 * 
	 * @return The 2 containers.
	 */
    public static Container[] getDeathContainers(Player player) {
        int count = allowedAmount(player);
        Container kept = new Container(count, false);
        Container lost = new Container(Inventory.SIZE + Equipment.SIZE, false);
        int slot = 0;
        // Preserve physical stacks separately; two MAX_VALUE stacks must not merge/overflow.
        for (Container source : new Container[]{player.getInventory().getContainer(), player.getEquipment().getContainer()}) {
            for (Item item : source.toArray()) {
                if (item != null && item.getAmount() > 0) lost.set(slot++, new Item(item));
            }
        }
        for (int i = 0; i < count; i++) {
            int best = -1;
            for (int j = 0; j < lost.getSize(); j++) {
                Item candidate = lost.get(j);
                if (candidate != null && (best < 0 || candidate.getDefinition().getExchangePrice()
                        > lost.get(best).getDefinition().getExchangePrice())) best = j;
            }
            if (best < 0) break;
            Item item = lost.get(best);
            Item unit = new Item(item); unit.setAmount(1); kept.set(i, unit);
            if (item.getAmount()==1) lost.set(best,null);
            else item.setAmount(item.getAmount()-1);
        }
        return new Container[]{kept, lost};
    }
    public static void sendPacket(Player player, int allowedItems, int riskedWealth, int carriedWealth, boolean hasBeastOfBurdenFamiliar, boolean skulled, int type, Container keptItems) {
        ActionSender.sendAMask(player, 211, 0, 2, 102, 18, 4);
        ActionSender.sendAMask(player, 212, 0, 2, 102, 21, 42);
        Object[] params = new Object[]{riskedWealth, carriedWealth, "", hasBeastOfBurdenFamiliar ? 1 : 0, skulled ? 1 : 0, keptItems.getItemSlot(3), keptItems.getItemSlot(2), keptItems.getItemSlot(1), keptItems.getItemSlot(0), allowedItems, type};
        ActionSender.sendClientScript(player, 118, params, "noooooobsll");
        ActionSender.sendBConfig(player, 199, 442);
        ActionSender.sendInterface(player, 102);
    }

    public static int allowedAmount(Player player) {
        int allowedItems = 3;
        if (player.getPrayer().usingPrayer(0, Prayer.PROTECT_ITEM) || player.getPrayer().usingPrayer(1, Prayer.CURSE_PROTECT_ITEM)) {
            allowedItems++;
        }
        if (player.getSkullManager().isSkulled()) {
        	allowedItems -= 3;
        }
        return allowedItems;
    }

    public static int getCarriedWealth(Player player) {
        return (int)Math.min(Integer.MAX_VALUE, wealth(player.getInventory().getContainer())
                + wealth(player.getEquipment().getContainer()));
    }

    public static int getRiskedWealth(Container lostItems) {
        return (int)Math.min(Integer.MAX_VALUE, wealth(lostItems));
    }

    private static long wealth(Container items) {
        long total = 0;
        for (Item item : items.toArray()) {
            if (item != null) total += (long)Math.max(0, item.getDefinition().getExchangePrice()) * Math.max(0, item.getAmount());
        }
        return total;
    }
    //types, 1=safe area, 2=poh, 3=in castlewars 4=in trouble brewing 5=barbie assualt

}
