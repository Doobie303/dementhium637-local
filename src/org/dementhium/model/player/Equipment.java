package org.dementhium.model.player;

import org.dementhium.cache.format.CacheItemDefinition;
import org.dementhium.content.SkillCapes;
import org.dementhium.content.activity.impl.DuelActivity;
import org.dementhium.content.activity.impl.castlewars.CastleWarsObjects;
import org.dementhium.model.Container;
import org.dementhium.model.Item;
import org.dementhium.model.World;
import org.dementhium.model.definition.ItemDefinition;
import org.dementhium.model.definition.WeaponInterface;
import org.dementhium.net.ActionSender;
import org.dementhium.util.Constants;
import org.dementhium.util.InterfaceSettings;

public class Equipment {

    public static final byte SLOT_HAT = 0, SLOT_CAPE = 1, SLOT_AMULET = 2,
            SLOT_WEAPON = 3, SLOT_CHEST = 4, SLOT_SHIELD = 5, SLOT_LEGS = 7,
            SLOT_HANDS = 9, SLOT_FEET = 10, SLOT_RING = 12, SLOT_ARROWS = 13;

    public static final int SIZE = 14;

    private final Container equipment = new Container(15, false);
    private final Player player;

    public Equipment(Player player) {
        this.player = player;
    }

    public Container getContainer() {
        return equipment;
    }

    public boolean contains(int item) {
        return equipment.containsOne(new Item(item));
    }

    public void deleteItem(int item, int amount) {
        equipment.remove(new Item(item, amount));
        refresh();
    }

    public Item get(int slot) {
        return equipment.get(slot);
    }

    public void set(int slot, Item item) {
        equipment.set(slot, item);
        refresh();
    }

    public void toggleStyle(Player p, int buttonId) {
        if (p.getAttribute("autocastId", -1) != -1) {
            p.removeAttribute("autocastId");
            ActionSender.sendConfig(p, 108, -1);
        }
        int select = buttonId - 11;
        ActionSender.sendConfig(player, 43, select);
        player.getSettings().setLastSelection(select);
        calculateType();
    }

    public void calculateType() {
        int itemId = get(SLOT_WEAPON) == null ? -1 : get(SLOT_WEAPON).getId();
		itemId = DegradingHandler.getCombatItemId(itemId);
        int groupId = itemId == -1 ? 0 : CacheItemDefinition.getItemDefinition(itemId).getGroupId();
        int select = player.getSettings().getLastSelection();
        int type = WeaponInterface.getType(groupId, select);
        int style = WeaponInterface.getStyle(groupId, select);
        player.getSettings().setCombatType(type);
        player.getSettings().setCombatStyle(style);
        if (player.getAttribute("autoCastSpell") == null) {
            ActionSender.sendConfig(player, 43, select);
        } else {
            ActionSender.sendConfig(player, 43, 4);
        }
    }

    public void clear() {
        equipment.reset();
        refresh();
    }

    public void refresh() {
        if (getSlot(SLOT_WEAPON) != 15486) player.removeAttribute("staffOfLightEffect");
        player.getMask().setAppearanceUpdate(true);
        ActionSender.sendItems(player, 94, equipment, false);
        player.getBonuses().calculate();
    }

    private static String[] FULL_BODY = {"Investigator's coat", "armour",
            "hauberk", "top", "shirt", "platebody", "Ahrims robetop",
            "Karils leathertop", "brassard", "Robe top", "robetop",
            "platebody (t)", "platebody (g)", "chestplate", "torso",
            "Morrigan's", "Vesta's", "leather body", "robe top", "Pernix body", "Torva platebody",
            "robe"};
    private static String[] FULL_HAT = {"sallet", "med helm", "coif",
            "Dharok's helm", "hood", "Initiate helm", "Coif",
            "Helm of neitiznot", "Pernix cowl", "Cowl", "cowl",
            }; //Santa hat doesn't work here, it will mess up
    		//partyhat does work here, BUT it will mess up the head in dialogues.
    private static String[] FULL_MASK = {"Christmas ghost hood", "Grim reaper hood",
            "Dragon full helm (or)", "sallet", "full helm", "mask",
            "Veracs helm", "Guthans helm", "Torags helm", "Karils coif",
            "full helm (t)", "full helm (g)", "mask"};

    public static int getItemType(int wearId) {
        if (wearId == 542)
        	return SLOT_LEGS;
        else if (wearId == 544)
        	return SLOT_CHEST;
        else if (wearId == 4084) //sled
        	return SLOT_WEAPON;
        if (wearId == -1) {
            return -1;
        }
		ItemDefinition def = ItemDefinition.forId(DegradingHandler.getCombatItemId(wearId));
        if (def.getEquipmentSlot() == -1) {
        	/*
        	 * Be very careful that ONLY wearable items have the following parts in their name:
        	 */
        	if (def.getName().contains("chaps"))
        		return SLOT_LEGS;
        	if (def.getName().contains("body"))
        		return SLOT_CHEST;
        }
        return def.getEquipmentSlot();
    }

    public static boolean isFullBody(ItemDefinition def) {
        String weapon = def.getName();
        for (String string : FULL_BODY) {
            if (weapon.contains(string)) {
                return true;
            }
        }
        return  def.getId() == 544 || def.getId() == 6107 || def.getId() == 13624 || def.getId() == 13887;
    }

    public static boolean isFullHat(ItemDefinition def) {
        String weapon = def.getName();
        for (int i = 0; i < FULL_HAT.length; i++) {
            if (weapon.startsWith(FULL_HAT[i]) || weapon.endsWith(FULL_HAT[i])) {
                return true;
            }
        }
        return def.getId() == 14824;
    }

    public static boolean isFullMask(ItemDefinition def) {
        String weapon = def.getName();
        for (int i = 0; i < FULL_MASK.length; i++) {
            if ((weapon.startsWith(FULL_MASK[i]) || weapon.endsWith(FULL_MASK[i])) && !weapon.endsWith("ighwayman mask")) {
                return true;
            }
        }
        return false;
    }

    public static boolean isTwoHanded(ItemDefinition def) {
        String wepEquiped = def.getName();
        int itemId = def.getId();
        if (itemId == 4212)
            return true;
        else if (itemId == 4214)
            return true;
        else if (itemId == 18353)
            return true;
        else if (itemId == 15403)
            return true;
        else if (itemId == 1419)
            return true;
        else if (wepEquiped.endsWith("claws"))
            return true;
        else if (wepEquiped.endsWith("anchor"))
            return true;
        else if (wepEquiped.endsWith("2h sword"))
            return true;
        else if (wepEquiped.endsWith("longbow"))
            return true;
        else if (wepEquiped.equals("Seercull"))
            return true;
        else if (wepEquiped.endsWith("shortbow"))
            return true;
        else if (wepEquiped.endsWith("Longbow"))
            return true;
        else if (wepEquiped.startsWith("Zaryte"))
            return true;
        else if (wepEquiped.endsWith("Shortbow"))
            return true;
        else if (wepEquiped.endsWith("bow full"))
            return true;
        else if (wepEquiped.equals("Dark bow"))
            return true;
        else if (wepEquiped.endsWith("halberd"))
            return true;
        else if (wepEquiped.contains(" maul"))
            return true;
        else if (wepEquiped.contains("Karils crossbow"))
            return true;
        else if (wepEquiped.contains("Torag's hammers"))
            return true;
        else if (wepEquiped.contains("Verac's flail"))
            return true;
        else if (wepEquiped.contains("Dharok's greataxe"))
            return true;
        else if (wepEquiped.contains("Guthan's warspear"))
            return true;
        else if (wepEquiped.contains("spear"))
        	return true;
        else if (wepEquiped.equals("Tzhaar-ket-om"))
            return true;
        else if (wepEquiped.contains("godsword"))
            return true;
        else if (wepEquiped.contains("Saradomin sword"))
            return true;
        else if (wepEquiped.contains("Hand Cannon"))
            return true;
        else if (wepEquiped.equals("salamander"))
        	return true;
    	return def.isTwoHanded();
    }

    public int getRenderAnim() {
        if (get(3) != null) {
			int combatId = DegradingHandler.getCombatItemId(get(3).getId());
			int renderEmote = ItemDefinition.forId(combatId).getRenderId();
            if (renderEmote != 0) {
                return renderEmote;
            }
        }
        return 1426;
    }
    //id 1427 is agility lol

    /**
     * Currently used for bank equipment.
     */
    public boolean recalculateHpModifier() {
        player.getSkills().setMaximumLifePoints(player.getSkills().getLevelForExperience(Skills.CONSTITUTION) * 10);
        for (int i = 0; i < player.getEquipment().getContainer().getSize(); i++) {
        	if (player.getEquipment().get(i) == null)
        		continue;
        	int itemId = player.getEquipment().get(i).getId();
            ItemDefinition definition = ItemDefinition.forId(itemId);
            if (hpModifier(definition)) {
                player.getSkills().raiseMaximumLifePoints(getModifier(definition));
            }
        }
        return true;
    }
    
    public void unEquip(Player p, int itemId, int interfaceId, int slot, boolean forced) {
        if (slot < 0 || itemId < 0) {
            return;
        }
        if (checkUnequip(slot)) {
            return;
        }
        if (interfaceId == 387)
        	player.closeAll(false, false);
        if (p.getInventory().getFreeSlots() <= 0) {
        	if (forced)
        		p.sendMessage("You do not have enough inventory space to complete this action.");
        	else
        		p.sendMessage("Not enough space in your inventory.");
            return;
        }
        int equipedAmount = p.getEquipment().get(slot).getAmount();
        int inventoryAmount = p.getInventory().getContainer().getNumberOf(p.getEquipment().get(slot));
        boolean restoreItem = false;
		if ((equipedAmount + inventoryAmount) < 0) {
			equipedAmount = Integer.MAX_VALUE - inventoryAmount;
			if (equipedAmount == 0)
				player.sendMessage("Not enough space in your inventory.");
			else 
				player.sendMessage("Not enough space in your inventory to unequip all of that item.");
			restoreItem = true;
		}
        ItemDefinition definition = ItemDefinition.forId(itemId);
        int equipedItemId = p.getEquipment().get(slot).getId();
        int equipedAmount2 = p.getEquipment().get(slot).getAmount();
        if (slot <= 15 && p.getEquipment().get(slot) != null) {
            Item inventoryItem = new Item(p.getEquipment().get(slot));
            inventoryItem.setAmount(equipedAmount);
            if (p.getInventory().getContainer().add(inventoryItem)) {
                if (restoreItem) {
                    Item remainingItem = new Item(p.getEquipment().get(slot));
                    remainingItem.setAmount(equipedAmount2 - equipedAmount);
                    p.getEquipment().set(slot, remainingItem);
                }
                else {
                    p.getEquipment().set(slot, null);
                }
                p.getInventory().refresh();
            }
        }
        if (hpModifier(definition)) {
            player.getSkills().decreaseMaximumLifePoints(getModifier(definition));
        }
    }

    public void equip(Player p, int buttonId, int buttonId2, int buttonId3, boolean checkForNotEquipable) {
        if (buttonId2 < 0 || buttonId2 >= Inventory.SIZE)
            return;
        System.out.println("buttonID: "+buttonId+" buttonId2: "+buttonId2+" buttonId3: "+buttonId3);
        Item item = p.getInventory().getContainer().get(buttonId2);
        if (item == null) {
            return;
        }    
        if (item.getId() == 2572 && player.getDonor() < 6) {
        player.sendMessage("You must donate to wear this ring!");
        return;
        
        }
    if (item.getId() == 1635 && player.getDonor() < 7) {
    player.sendMessage("You must donate to wear this ring!");
    return;
    }
    
        if(item.getId()==org.dementhium.content.InfernalCape.ID&&!org.dementhium.content.InfernalCape.supported(p)){p.sendMessage("Use the updated development client to equip the Infernal cape.");return;}
        if(org.dementhium.content.OsrsEquipment.isItem(item.getId())&&!org.dementhium.content.OsrsEquipment.supported(p)){p.sendMessage("Use the updated development client to equip this OSRS item.");return;}
        buttonId3 = getDegradedItem(item, false).getId();
        Item oldInvItem = item;
        item = getDegradedItem(item, true);
        int targetSlot = Equipment.getItemType(buttonId3);
        ItemDefinition definition = ItemDefinition.forId(item.getId());
        if (definition.getName().toLowerCase().contains("cape")) {
            targetSlot = SLOT_CAPE;
        }
        if (definition.getName().toLowerCase().contains("mask")) {
            targetSlot = SLOT_HAT;
        }
        if (item.getId() == 542) //also add items like this to the getItemType, so they can be unequiped.
        	targetSlot = SLOT_LEGS;
        else if (item.getId() == 544)
        	targetSlot = SLOT_CHEST;
        else if (item.getId() == 4084) //sled
        	targetSlot = SLOT_WEAPON;
        if (targetSlot > 13 || targetSlot == 6 || targetSlot == 8 || targetSlot == 11) {
            return;
        }
        if (checkForNotEquipable) {
        	if ((definition.isStackable() && targetSlot != SLOT_ARROWS && targetSlot != SLOT_WEAPON) /*&& definition.getEquipId() <= 0*/) {
        	//if (definition.getEquipmentSlot() == -1) {
        		player.sendMessage("You can't wear that item. If you do think you can, please inform an administrator.");
        		return;
        	}
        } else {
        	player.closeAll(false, false);
        }
        Item oldItem = p.getEquipment().get(targetSlot);
        if (targetSlot == -1) {
        	System.out.println("Could not equip item " + item.getId() + ", equipment slot: " + targetSlot);
            return;
        }
        if (targetSlot==SLOT_WEAPON && player.getActivity() instanceof DuelActivity
                && !((DuelActivity)player.getActivity()).getDuelConfigurations().weaponAllowed(player,item.getId())) return;
        if (!allowed(targetSlot, Equipment.isTwoHanded(item.getDefinition()))) {
            return;
        }
        if (targetSlot == 3) {
            player.getSettings().setUsingSpecial(false);
            ActionSender.sendConfig(player, 301, 0);
            if (item != null)
                player.resetCombat();
        }
        if (Equipment.isTwoHanded(item.getDefinition()) && p.getInventory().getFreeSlots() < 1 && p.getEquipment().get(5) != null) {
            player.sendMessage("Not enough free space in your inventory.");
            return;
        }
        if (Equipment.isTwoHanded(item.getDefinition()) && p.getInventory().getFreeSlots() < 1 && p.getEquipment().get(5) != null) {
            player.sendMessage("Not enough free space in your inventory.");
            return;
        }
        boolean hasReq = true;
        if (item.getDefinition().getSkillRequirementId() != null) {
            for (int skillIndex = 0; skillIndex < item.getDefinition().getSkillRequirementId().size(); skillIndex++) {
                int reqId = item.getDefinition().getSkillRequirementId().get(skillIndex);
                int reqLvl = -1;
                if (item.getDefinition().getSkillRequirementLvl().size() > skillIndex)
                    reqLvl = item.getDefinition().getSkillRequirementLvl().get(skillIndex);
                if (reqId > 25 || reqId < 0 || reqLvl < 0 || reqLvl > 120)
                    continue;
                if (p.getSkills().getLevelForExperience(reqId) < reqLvl) {
                    if (hasReq)
                        player.sendMessage("You are not high enough level to use this item.");
                    player.sendMessage("You need to have "+((Skills.SKILL_NAME[reqId]).startsWith("A") ? " an " : " a ") + (Skills.SKILL_NAME[reqId]) + " level of " + reqLvl + ".");
                    hasReq = false;
                }
            }
        }
        for (int i = 0; i < 24; i++) {
            for (int z = 0; z < 2; z++) {
                if (SkillCapes.skillCapeId[i][z] == item.getId() || (SkillCapes.skillCapeId[i][z] + 2 - z) == item.getId()) {
                    if (player.getSkills().getLevelForExperience(i) != 99) {
                        if (hasReq)
                            player.sendMessage("You are not high enough level to use this item.");
                        player.sendMessage("You need "+((Skills.SKILL_NAME[i]).startsWith("A") ? " an " : " a ") + (Skills.SKILL_NAME[i]) + " level of 99" + (item.getDefinition().getName().contains("cape") ? " to wear this cape." : "."));
                        hasReq = false;
                        i = 25; //so it doesn't double loop
                        z = 3;
                    }
                }
            }
        }
        if (!hasReq)
            return;
		int equipedAmount = 0; //oldAmount
		int equipedItemId = -1;
		if (p.getEquipment().get(targetSlot) != null) {
			equipedAmount = p.getEquipment().get(targetSlot).getAmount();
			equipedItemId = p.getEquipment().get(targetSlot).getId();
		}
		int inventoryAmount = item.getAmount();
		int itemId = item.getId();
		if (itemId == equipedItemId) {
			if ((inventoryAmount + equipedAmount) < 0) {
				inventoryAmount = Integer.MAX_VALUE - equipedAmount;
				if (inventoryAmount == 0)
					player.sendMessage("Not enough space in your equipment.");
			}
		}
        p.getInventory().deleteItem(oldInvItem.getId(), inventoryAmount);
        if (p.getEquipment().get(targetSlot) != null && (buttonId3 != p.getEquipment().get(targetSlot).getDefinition().getId() || !item.getDefinition().isStackable())) {
            if (p.getInventory().contains(p.getEquipment().get(targetSlot).getId())) {
            	if (item.getDefinition().isStackable() && (equipedAmount + p.getInventory().getContainer().getNumberOf(p.getEquipment().get(targetSlot))) < 0) {
            		p.getInventory().set(buttonId2, new Item(buttonId3, inventoryAmount));
            		p.getInventory().refresh();
            		//p.getInventory().addItem(buttonId3, inventoryAmount);
        			player.sendMessage("Not enough space in your inventory.");
        			return;
            	}
            }
        	if (p.getInventory().get(buttonId2) == null) { //slot used
        		if (item.getDefinition().isStackable() && (equipedAmount + p.getInventory().getContainer().getNumberOf(p.getEquipment().get(targetSlot))) < 0) {
        			p.getInventory().set(buttonId2, new Item(buttonId3, inventoryAmount));
        			p.getInventory().refresh();
        			//p.getInventory().addItem(buttonId3, inventoryAmount);
        			player.sendMessage("Not enough space in your inventory.");
        			return;
        		}
        		if (p.getInventory().contains(equipedItemId) && (ItemDefinition.forId(equipedItemId).isStackable() || ItemDefinition.forId(equipedItemId).isNoted()))
        			p.getInventory().addItem(p.getEquipment().get(targetSlot));
        		else
        			p.getInventory().set(buttonId2, p.getEquipment().get(targetSlot));
            } else {
                //p.getInventory().getContainer().add(p.getEquipment().get(targetSlot));
                if (!p.getInventory().addItem(p.getEquipment().get(targetSlot).getDefinition().getId(), equipedAmount)) {
                	p.getInventory().set(buttonId2, new Item(buttonId3, inventoryAmount));
                	p.getInventory().refresh();
                	//p.getInventory().addItem(buttonId3, inventoryAmount);
                    return;
                }
            }
            p.getInventory().refresh();
            p.getEquipment().set(targetSlot, null);
        }
        if (targetSlot == 3) {
            if (Equipment.isTwoHanded(item.getDefinition()) && p.getEquipment().get(5) != null) {
                if (!p.getInventory().addItem(p.getEquipment().get(5).getDefinition().getId(), p.getEquipment().get(5).getAmount())) {
                	p.getInventory().set(buttonId2, new Item(buttonId3, inventoryAmount));
                	p.getInventory().refresh();
                	//p.getInventory().addItem(buttonId3, inventoryAmount);
                    return;
                }
                p.getEquipment().set(5, null);
            }
        } else if (targetSlot == 5) {
            if (p.getEquipment().get(3) != null && Equipment.isTwoHanded(p.getEquipment().get(3).getDefinition())) {
                if (!p.getInventory().addItem(p.getEquipment().get(3).getDefinition().getId(), p.getEquipment().get(3).getAmount())) {
                	p.getInventory().set(buttonId2, new Item(buttonId3, inventoryAmount));
                	p.getInventory().refresh();
                	//p.getInventory().addItem(buttonId3, inventoryAmount);
                    return;
                }
                p.getEquipment().set(3, null);
            }
        }
        Item item2 = new Item(buttonId3, equipedAmount + inventoryAmount);
        if (itemId != equipedItemId || !item.getDefinition().isStackable())
        	item2 = new Item(buttonId3, inventoryAmount);
        p.getEquipment().set(targetSlot, item2);
        if (oldItem != null && oldItem.getId() == 15486) {
            player.removeAttribute("staffOfLightEffect");
        }
        if (player.getEquipment().getSlot(Equipment.SLOT_SHIELD) == 8856 && !player.getAttribute("disabledTabs", false)) {
            for (int i : Constants.W_GUILD_CATAPULT_TABS)
                InterfaceSettings.disableTab(player, i);
            ActionSender.sendInterface(player, 1, player.getConnection().getDisplayMode() >= 2 ? 746 : 548, player.getConnection().getDisplayMode() >= 2 ? 92 : 207, 411);
            ActionSender.sendBConfig(player, 168, 5);
            player.setAttribute("disabledTabs", true);
        }
        if (oldItem != null && oldItem.getId() != 4037 && oldItem.getId() != 4039) {
            if (hpModifier(oldItem.getDefinition())) {
                player.getSkills().decreaseMaximumLifePoints(getModifier(oldItem.getDefinition()));
            }
        }
        if (hpModifier(item.getDefinition())) {
            player.getSkills().raiseMaximumLifePoints(getModifier(item.getDefinition()));
        }
        calculateType();
    }

    public boolean checkUnequip(int slot) {
        if (player.getActivity().getActivityId() == 0) {
            if (slot == SLOT_WEAPON || slot == SLOT_SHIELD) {
                if (getSlot(SLOT_WEAPON) == 4037 || getSlot(SLOT_WEAPON) == 4039) {
                    CastleWarsObjects.createDroppedFlag(player);
                    return true;
                }
            }
        } else if (player.getEquipment().getSlot(Equipment.SLOT_SHIELD) == 8856 && player.getAttribute("disabledTabs", false)) {
            player.removeAttribute("disabledTabs");
            for (int i : Constants.W_GUILD_CATAPULT_TABS)
                InterfaceSettings.enableTab(player, i);
            ActionSender.sendInterface(player, 1, player.getConnection().getDisplayMode() >= 2 ? 746 : 548, player.getConnection().getDisplayMode() >= 2 ? 92 : 207, 387);
            ActionSender.sendBConfig(player, 168, 4);
        }
        return false;
    }

    public boolean allowed(int slot, boolean isTwoHanded) {
        if (checkUnequip(slot)) {
            return true;
        }
        if (!(player.getActivity() instanceof DuelActivity)) {
            return true;
        }
        DuelActivity duel = (DuelActivity) player.getActivity();
        int currentSlot = slot == 7 ? 6 : slot > 7 ? slot - 2 : slot;
        if (!duel.getDuelConfigurations().canEquip(player, currentSlot)) {
            return false;
        }
        if (isTwoHanded && !duel.getDuelConfigurations().canEquip(player, SLOT_SHIELD)) {
            return false;
        }
        return true;
    }

    /**
     * Gets the (deg) version of the item.
     * @param item
     * @return
     */
    public Item getDegradedItem(Item item, boolean sendMessage) {
		return DegradingHandler.activateOnEquip(player, item, sendMessage);
    }
     
    public boolean degrades(ItemDefinition item) {
        String name = item.getName();
        return name.contains("Torva") || name.contains("Vesta");
    }/*
    * Real Messages:
    * Your torva full helm/platebody has degraded and is untradeable in this state.
    * Your torva platelegs have degraded and is (this should be 'are'.. but real RS has is =P) untradeable in this state.
    */


    public boolean hpModifier(ItemDefinition item) {
        if (item != null) {
            String name = item.getName();
            return name.contains("Torva") || name.contains("Pernix") || name.contains("Virtus");
        }
        return false;
    }

    public int getModifier(ItemDefinition item) {
        if (item != null) {
            String name = item.getName();
            if (name.contains("Torva full helm") || name.contains("Pernix cowl") || name.contains("Virtus mask")) {
                return 66;
            } else if (name.contains("Torva platelegs") || name.contains("Pernix chaps") || name.contains("Virtus robe legs")) {
                return 134;
            } else if (name.contains("Torva platebody") || name.contains("Pernix body") || name.contains("Virtus robe top")) {
                return 200;
            }
        }
        return 0;
    }

    public boolean usingRanged() {
        Item item = get(SLOT_WEAPON);
        if (item != null) {
            String name = item.getDefinition().getName().toLowerCase();
            if (name.contains("bow") || name.contains("dart") || name.contains("knife") || name.contains("javelin") || name.contains("chinchompa")) {
                return true;
            }
        }
        return false;
    }

    public boolean barrowsSet(int setID) {
        Item hat = get(0), body = get(4), bottoms = get(7), weaponSlot = get(3);
        if (hat == null || body == null || bottoms == null || weaponSlot == null)
            return false;
		int[] set;
        switch (setID) {
            case 1:    //Ahrim's
				set = new int[] { 4708, 4712, 4714, 4710 };
                break;
            case 2: //Dharok's
				set = new int[] { 4716, 4720, 4722, 4718 };
                break;
            case 3: //Guthan's
				set = new int[] { 4724, 4728, 4730, 4726 };
                break;
            case 4: //Karil's
				set = new int[] { 4732, 4736, 4738, 4734 };
                break;
            case 5: //Torag's
				set = new int[] { 4745, 4749, 4751, 4747 };
                break;
            case 6: //Verac's
				set = new int[] { 4753, 4757, 4759, 4755 };
                break;
			default:
				return false;
        }
		return DegradingHandler.getCombatItemId(hat.getId()) == set[0]
				&& DegradingHandler.getCombatItemId(body.getId()) == set[1]
				&& DegradingHandler.getCombatItemId(bottoms.getId()) == set[2]
				&& DegradingHandler.getCombatItemId(weaponSlot.getId()) == set[3];
    }

    public int getSlot(int i) {
    	return getSlot(i, -1);
    }
    public int getSlot(int i, int fail) {
        if (get(i) == null) {
            return fail;
        }
        return get(i).getId();
    }

    public boolean voidSet(int setID) {
        int helmet = setID == 1 ? 11665 : setID == 2 ? 11664 : setID == 3 ? 11663 : -1;
        if (helmet == -1 || getSlot(SLOT_HAT) != helmet) return false;
        int top = getSlot(SLOT_CHEST), legs = getSlot(SLOT_LEGS);
        int parts = top == 8839 || top == 19785 || top == 19787 || top == 19789 ? 1 : 0;
        if (legs == 8840 || legs == 19786 || legs == 19788 || legs == 19790) parts++;
        if (getSlot(SLOT_HANDS) == 8842) parts++;
        if (getSlot(SLOT_SHIELD) == 19711) parts++;
        return parts >= 3;
    }

    public void removeSlot(int slot) {
        Item item = equipment.get(slot);
        if (item == null) {
            return;
        }
        if (!player.getInventory().addItem(item)) {
            return;
        }
        set(slot, null);
    }
}

