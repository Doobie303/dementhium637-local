package org.dementhium.net.packethandlers;

import org.dementhium.content.misc.Decanting;
import org.dementhium.content.skills.Firemaking;
import org.dementhium.content.skills.Fletching;
import org.dementhium.content.skills.crafting.GemCutting;
import org.dementhium.content.skills.crafting.LeatherCrafting;
import org.dementhium.content.skills.herblore.Herblore;
import org.dementhium.content.skills.magic.Enchant;
import org.dementhium.model.Item;
import org.dementhium.model.World;
import org.dementhium.model.definition.ItemDefinition;
import org.dementhium.model.player.Player;
import org.dementhium.model.player.Skills;
import org.dementhium.net.ActionSender;
import org.dementhium.net.PacketHandler;
import org.dementhium.net.message.Message;
import org.dementhium.tickable.Tick;

import java.io.BufferedWriter;
import java.io.FileWriter;

/**
 * @author Steve <golden_32@live.com>
 * @author Lumby <lumbyjr@hotmail.com>
 */
public class ItemOnItemHandler extends PacketHandler {

	private static final int ITEM_ON_ITEM = 3;

	@Override
	public void handlePacket(Player player, Message packet) {
		switch (packet.getOpcode()) {
		case ITEM_ON_ITEM:
			handleItemOnItem(player, packet);
			break;
		}

	}

	private void handleItemOnItem(Player player, Message packet) {
		int interfaceId1 = packet.readShort();
		packet.readShort();
		int usedWithSlot = packet.readShort();
		int itemUsed = packet.readLEShort();
		int usedSlot = packet.readShort();
		int interfaceId2 = packet.readShort();
		int childId = packet.readShort();
		int itemUsedWith = packet.readLEShortA();

		Item firstItem = player.getInventory().get(usedSlot);
		Item secondItem = player.getInventory().get(usedWithSlot);
		player.closeAll(true, true);
		if (interfaceId1 == 149) {
			if (firstItem == null || firstItem.getId() != itemUsed)
				return;
			switch (childId) {
			case 38:
				lowAlch(player, itemUsed, usedSlot);
				break;
			case 59:
				highAlch(player, itemUsed, usedSlot);
				break;
			case 17:
			case 74:
				if ((interfaceId2 == 747 || interfaceId2 == 662)
						&& player.getFamiliar().getId() > 0) {
					player.setAttribute("itemId", itemUsed);
					player.setAttribute("itemSlot", usedSlot);
					player.getFamiliar().specialMove(null);
					player.removeAttribute("itemId");
					player.removeAttribute("itemSlot");
				}
				break;
			default:
			}
		}
		if (firstItem == null || firstItem.getId() != itemUsed)
			return;
		if (Enchant.enchant(player, childId, itemUsed, usedSlot)) //enchant needs to be placed here as it doesn't require a second item.
			return;
		if (firstItem == null || secondItem == null) {
			return;
		}
		if (firstItem.getId() != itemUsed || secondItem.getId() != itemUsedWith) {
			return;
		}
		if (usedWithSlot == usedSlot) {
			return;
		}
		ActionSender.sendCloseChatBox(player);
		if (Fletching.getItemForId(itemUsed, itemUsedWith, false) != null) {
			Fletching.sendInterfaces(player,
					Fletching.getItemForId(itemUsed, itemUsedWith, false));
			return;
		} else if (Firemaking.firemake(player, itemUsed, itemUsedWith,
				usedWithSlot, usedSlot, false)) {
			return;
		} else if (GemCutting.cutGem(player, itemUsed, itemUsedWith)) {
			return;
		} else if (LeatherCrafting.handleItemOnItem(player, firstItem,
				secondItem)) {
			return;
		} else if (Decanting.decantPotion(player, usedSlot, usedWithSlot)) {
			return;
		}
		int item = Herblore.isHerbloreSkill(new Item(itemUsed), new Item(
				itemUsedWith));
		if (item > -1) {
			player.setAttribute("itemUsedSkill", new Item(itemUsed));
			player.setAttribute("itemUsedSkill2", new Item(itemUsedWith));
			Herblore.sendInterface(player, item, new Item(itemUsed), new Item(
					itemUsedWith));
			return;
		}
		
		if (useWith(985, 987, itemUsed, itemUsedWith)) {
			createItemFrom2Items(player, usedSlot, usedWithSlot, 989);
			player.sendMessage("You join the two halves of the key together.");
			return;
		}
		for(int i = 0; i < 4; i++) {
			if (useWith(11690, 11702+(2*i), itemUsed, itemUsedWith)) {
				int createdItem = 11694+(2*i);
				String name = ItemDefinition.forId(createdItem).getName();
				createItemFrom2Items(player, usedSlot, usedWithSlot, createdItem);
				player.sendMessage("You attach the hilt to the blade and make"+(name.startsWith("A") ? " an " : " a ")+name+".");
				return;
			}
		}
		if (useWith(11710, 11712, itemUsed, itemUsedWith) || useWith(11710, 11714, itemUsed, itemUsedWith)
				|| useWith(11712, 11714, itemUsed, itemUsedWith) || useWith(11710, 11692, itemUsed, itemUsedWith) 
				|| useWith(11712, 11688, itemUsed, itemUsedWith) || useWith(11714, 11686, itemUsed, itemUsedWith)) {
			player.sendMessage("Those pieces of the godsword can't be joined together like that - try forging them.");
			return;
		}
		if (useWith(19333, 6585, itemUsed, itemUsedWith)) { //make fury (or)
			createItemFrom2Items(player, usedSlot, usedWithSlot, 19335);
			return;
		}
		if (useWith(19346, 11335, itemUsed, itemUsedWith)) { //make dragon full helm (or)
			createItemFrom2Items(player, usedSlot, usedWithSlot, 19336);
			return;
		}
		if (useWith(19348, 4087, itemUsed, itemUsedWith)) { //make dragon platelegs (or)
			createItemFrom2Items(player, usedSlot, usedWithSlot, 19338);
			return;
		}
		if (useWith(19348, 4585, itemUsed, itemUsedWith)) { //make dragon plateskirt (or)
			createItemFrom2Items(player, usedSlot, usedWithSlot, 19339);
			return;
		}
		if (useWith(19350, 14479, itemUsed, itemUsedWith)) { //make dragon platebody (or)
			createItemFrom2Items(player, usedSlot, usedWithSlot, 19337);
			return;
		}
		if (useWith(19352, 1187, itemUsed, itemUsedWith)) { //make dragon sq shield (or)
			createItemFrom2Items(player, usedSlot, usedWithSlot, 19340);
			return;
		}
		if (useWith(19354, 11335, itemUsed, itemUsedWith)) { //make dragon full helm (sp)
			createItemFrom2Items(player, usedSlot, usedWithSlot, 19341);
			return;
		}
		if (useWith(19356, 4087, itemUsed, itemUsedWith)) { //make dragon platelegs (sp)
			createItemFrom2Items(player, usedSlot, usedWithSlot, 19343);
			return;
		}
		if (useWith(19356, 4585, itemUsed, itemUsedWith)) { //make dragon plateskirt (sp)
			createItemFrom2Items(player, usedSlot, usedWithSlot, 19344);
			return;
		}
		if (useWith(19358, 14479, itemUsed, itemUsedWith)) { //make dragon platebody (sp)
			createItemFrom2Items(player, usedSlot, usedWithSlot, 19342);
			return;
		}
		if (useWith(19360, 1187, itemUsed, itemUsedWith)) { //make dragon sq shield (sp)
			createItemFrom2Items(player, usedSlot, usedWithSlot, 19345);
			return;
		}

		if (player.getRights() == 2)
			player.sendMessage("Unhandled option on item action: "
					+ interfaceId1 + ", " + childId + ", " + interfaceId2 + ".");
		player.sendMessage("Nothing interesting happens.");
	}

    private boolean useWith(int Item1, int Item2, int itemUsed, int usedWith) {
        if (itemUsed == Item1 && usedWith == Item2 || itemUsed == Item2 && usedWith == Item1)
            return true;
        return false;
    }
    
    private void createItemFrom2Items(Player player, int usedSlot, int usedWithSlot, int createdItemId) {
    	Item usedItem = player.getInventory().get(usedSlot);
    	Item usedWithItem = player.getInventory().get(usedWithSlot);
    	boolean replace = false;
 		if (usedItem.getDefinition().isStackable() && usedItem.getAmount() > 1)
 			player.getInventory().getContainer().remove(new Item(usedItem.getId(), 1));
 		else {
 			player.getInventory().set(usedSlot, null);
 			replace = true;
 		}
 		if (usedWithItem.getDefinition().isStackable() && usedWithItem.getAmount() > 1)
 			player.getInventory().getContainer().remove(new Item(usedWithItem.getId(), 1));
 		else
 			player.getInventory().set(usedWithSlot, null);
 		if (replace)
 			player.getInventory().set(usedSlot, new Item(createdItemId, 1));
 		else
 			player.getInventory().addItem(new Item(createdItemId, 1));
		player.getInventory().refresh();
    }
	
	private boolean hasFireStaff(Player player) {
		if (player.getEquipment().getContainer().contains(new Item(1387))
				|| player.getEquipment().getContainer()
						.contains(new Item(1393))
				|| player.getEquipment().getContainer()
						.contains(new Item(1401))
				|| player.getEquipment().getContainer()
						.contains(new Item(3053))
				|| player.getEquipment().getContainer()
						.contains(new Item(3054))) {
			return true;
		}
		return false;
	}

	private void lowAlch(final Player player, int itemId, int slot) {
		if (player.getSkills().getLevel(Skills.MAGIC) < 21) {
			player.sendMessage("You need a higher magic level to cast this spell.");
			return;
		} else if (player.getAttribute("alching") != null) {
			player.sendMessage("You are already casting an alchemy spell.");
			return;
		} else if (!player.getInventory().getContainer()
				.contains(new Item(561, 1))) {
			player.sendMessage("You do not have the required runes or staff to cast that.");
			return;
		} else if (!player.getInventory().getContainer()
				.contains(new Item(554, 3))
				&& !hasFireStaff(player)) {
			player.sendMessage("You do not have the required runes or staff to cast that.");
			return;
		} else if (player.getInventory().getContainer()
				.getItemCount(561) == 1 && itemId == 561) {
			player.sendMessage("You can't cast an alchemy spell on the Nature rune required to cast the spell.");
			return;
		} else if (player.getInventory().getContainer()
				.getItemCount(554) == 3 && itemId == 554) {
			player.sendMessage("You can't cast an alchemy spell on the Fire rune required to cast the spell.");
			return;
		} else {

			final double alchValue = player.getInventory().getContainer()
					.get(slot).getDefinition().getLowAlchPrice();
			if (alchValue == 0) {
				player.sendMessage("This item has no alchemy value. Please report it to an administrator.");
				return;
			}
			if (itemId == 995) {
				player.sendMessage("You cannot cast an alchemy spell on money.");
				return;
			}
			if (player.getInventory().getContainer().getFreeSlots() == 0
					&& !player.getInventory().getContainer()
							.contains(new Item(995))
							&& player.getInventory().getContainer().get(slot).getAmount() != 1) {
				player.sendMessage("You do not have any room in your inventory to cast an alchemy spell.");
				return;
			}
			if (player.getInventory().getContainer().getItemCount(995) + alchValue < 0) {
				player.sendMessage("You have too much Coins in your inventory to cast an alchemy spell on this item.");
				return;
			}
			if (hasFireStaff(player)) {
				player.animate(9625);
				player.graphics(1692);
			} else {
				player.animate(712);
				player.graphics(112);
			}

			player.setAttribute("cantMove", Boolean.TRUE);
			player.setAttribute("alching", Boolean.TRUE);
			player.getInventory().getContainer().remove(new Item(itemId, 1));
			player.getInventory().getContainer().remove(new Item(561, 1));
			player.getInventory().getContainer().remove(new Item(554, 3));
			player.getInventory().addItem(995, (int) alchValue);
			player.getInventory().refresh();
			ActionSender.sendBConfig(player, 168, 7);
			World.getWorld().submit(new Tick(4) {
				@Override
				public void execute() {
					player.getSkills().addExperience(Skills.MAGIC, 31);
					player.getInventory().refresh();
					player.removeAttribute("cantMove");
					player.removeAttribute("alching");
					stop();
				}
			});
		}
	}

	private void highAlch(final Player player, int itemId, int slot) {
		if (player.getSettings().getSpellBook() == 192) {
			if (player.getSkills().getLevel(6) < 55) {
				player.sendMessage("You need a higher magic level to cast this spell.");
				return;
			} else if (player.getAttribute("alching") != null) {
				player.sendMessage("You are already casting an alchemy spell.");
				return;
			} else if (!player.getInventory().getContainer()
					.contains(new Item(561, 1))) {
				player.sendMessage("You do not have the required runes or staff to cast that.");
				return;
			} else if (!player.getInventory().getContainer()
					.contains(new Item(554, 5))
					&& !hasFireStaff(player)) {
				player.sendMessage("You do not have the required runes or staff to cast that.");
				return;
			} else if (player.getInventory().getContainer()
					.getItemCount(561) == 1 && itemId == 561) {
				player.sendMessage("You can't cast an alchemy spell on the Nature rune required to cast the spell.");
				return;
			} else if (player.getInventory().getContainer()
					.getItemCount(554) == 5 && itemId == 554) {
				player.sendMessage("You can't cast an alchemy spell on the Fire rune required to cast the spell.");
				return;
			} else {
				final double alchValue = player.getInventory().getContainer()
						.get(slot).getDefinition().getHighAlchPrice();
				if (alchValue == 0) {
					player.sendMessage("This item has no alchemy value. Please report it to an administrator.");
					try {
						BufferedWriter bw = new BufferedWriter(new FileWriter(
								"./data/missing_alch.txt", true));
						bw.write("\n Item Id: " + itemId + " has no alch value");
						bw.flush();
						bw.close();
					} catch (Throwable t) {
						t.printStackTrace();
					}
					return;
				}
				if (itemId == 995) {
					player.sendMessage("You cannot cast an alchemy spell on money.");
					return;
				}
				if (player.getInventory().getContainer().getFreeSlots() == 0
						&& !player.getInventory().getContainer()
								.contains(new Item(995))
								&& player.getInventory().getContainer().get(slot).getAmount() != 1) {
					player.sendMessage("You do not have any room in your inventory to cast an alchemy spell.");
					return;
				}
				if (player.getInventory().getContainer().getItemCount(995) + alchValue < 0) {
					player.sendMessage("You have too much Coins in your inventory to cast an alchemy spell on this item.");
					return;
				}
				if (hasFireStaff(player)) {
					player.animate(9633);
					player.graphics(1693);
				} else {
					player.animate(713);
					player.graphics(113);
				}
				player.setAttribute("cantMove", Boolean.TRUE);
				player.setAttribute("alching", Boolean.TRUE);
				player.getInventory().getContainer().remove(new Item(itemId, 1));
				player.getInventory().getContainer().remove(new Item(561, 1));
				player.getInventory().getContainer().remove(new Item(554, 5));
				player.getInventory().addItem(995, (int) alchValue);
				player.getInventory().refresh();
				ActionSender.sendBConfig(player, 168, 7);
				World.getWorld().submit(new Tick(4) {
					@Override
					public void execute() {
						player.getSkills().addExperience(Skills.MAGIC, 65);
						player.getInventory().refresh();
						player.removeAttribute("cantMove");
						player.removeAttribute("alching");
						stop();
					}
				});
			}
		} else {
			ActionSender
					.sendMessage(player,
							"Please acknowledge that using cheat-clients is against the rules.");
			ActionSender
					.sendMessage(player,
							"If you think this is a bug, please report it on the forums.");
		}
	}

}
