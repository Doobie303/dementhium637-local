package org.dementhium.event.impl.interfaces;

import org.dementhium.content.BookManager;
import org.dementhium.content.DialogueManager;
import org.dementhium.content.activity.impl.BarrowsActivity;
import org.dementhium.content.activity.impl.CastleWarsActivity;
import org.dementhium.content.activity.impl.DuelActivity;
import org.dementhium.content.activity.impl.DuelActivity.State;
import org.dementhium.content.activity.impl.DungeoneeringActivity;
import org.dementhium.content.activity.impl.barrows.BarrowsConstants;
import org.dementhium.content.activity.impl.puropuro.ImplingJar;
import org.dementhium.content.misc.ChanceItem;
import org.dementhium.content.misc.GraveStone;
import org.dementhium.content.misc.GraveStoneManager;
import org.dementhium.content.misc.treasuretrails.Casket;
import org.dementhium.content.skills.herblore.Herb;
import org.dementhium.content.skills.herblore.Herblore;
import org.dementhium.content.skills.magic.TeleportHandler;
import org.dementhium.content.skills.runecrafting.Runecrafting;
import org.dementhium.content.skills.runecrafting.Talisman;
import org.dementhium.content.skills.summoning.Familiar;
import org.dementhium.content.skills.summoning.Summoning;
import org.dementhium.content.skills.summoning.SummoningPouch;
import org.dementhium.event.EventListener;
import org.dementhium.event.EventManager;
import org.dementhium.model.Item;
import org.dementhium.model.World;
import org.dementhium.model.definition.ItemDefinition;
import org.dementhium.model.mask.Animation;
import org.dementhium.model.mask.ForceText;
import org.dementhium.model.misc.DamageManager.DamageType;
import org.dementhium.model.misc.GroundItem;
import org.dementhium.model.misc.GroundItemManager;
import org.dementhium.model.player.Equipment;
import org.dementhium.model.player.Player;
import org.dementhium.net.ActionSender;
import org.dementhium.tickable.Tick;
import org.dementhium.util.Misc;

/**
 * @author 'Mystic Flow <Steven@rune-server.org>
 */
public class InventoryListener extends EventListener {

	public static final int SPADE = 952;

	@Override
	public void register(EventManager manager) {
		manager.registerInterfaceListener(149, this);
	}

	private static final String[] NUMBERWORD = {
		"zero", "one", "two", "three", "four", "five",
		"six", "seven", "eight", "nine", "ten", "eleven", "twelve" };

	@Override
	public boolean interfaceOption(final Player player, int interfaceId, int buttonId, int slot, int itemId, int opcode) {
		if (interfaceId != 149) {
			return false;
		}
		Item slotItem = player.getInventory().get(slot);
		if (slotItem == null || slotItem.getId() != itemId)
			return false;
		if(opcode == 0){
			if (itemId >= 5509 && itemId <= 5515) {
				int pouchHealth = player.getInventory().getContainer().get(slot).getHealth();
				if(pouchHealth > 0){
					player.sendMessage("There "+(pouchHealth == 1 ? "is " : "are ") +NUMBERWORD[pouchHealth]+" pure essence"+(pouchHealth == 1 ? "" : "s")+" in this pouch.");
					return true;
				}else{
					player.sendMessage("There are no essences in your pouch.");
				}
			}
			switch (itemId) {
			case 6865: //marionette
				player.animate(3005);
				player.graphics(513);
				return true;
			case 6866: //marionette
				player.animate(3005);
				player.graphics(517);
				return true;
			case 6867: //marionette
				player.animate(3005);
				player.graphics(509);
				return true;
			case 4566: //rubber chicken
				player.animate(1835);
				return true;
			case 4079:
				player.animate(1459); //yo-yo walk emote
				return true;
			case 19335: //fury (or)
				if (player.getInventory().getFreeSlots() < 1) {
					player.sendMessage("You need at least one free inventory space to do this.");
					return true;
				}
				player.getInventory().deleteItem(itemId, 1);
				player.getInventory().addItem(6585, 1);
				player.getInventory().addItem(19333, 1);
				player.getInventory().refresh();
				return true;
			case 19336: //d full helm (or)
				if (player.getInventory().getFreeSlots() < 1) {
					player.sendMessage("You need at least one free inventory space to do this.");
					return true;
				}
				player.getInventory().deleteItem(itemId, 1);
				player.getInventory().addItem(11335, 1);
				player.getInventory().addItem(19346, 1);
				player.getInventory().refresh();
				return true;
			case 19338: //d platelegs (or)
				if (player.getInventory().getFreeSlots() < 1) {
					player.sendMessage("You need at least one free inventory space to do this.");
					return true;
				}
				player.getInventory().deleteItem(itemId, 1);
				player.getInventory().addItem(4087, 1);
				player.getInventory().addItem(19348, 1);
				player.getInventory().refresh();
				return true;
			case 19339: //d platelegs (or)
				if (player.getInventory().getFreeSlots() < 1) {
					player.sendMessage("You need at least one free inventory space to do this.");
					return true;
				}
				player.getInventory().deleteItem(itemId, 1);
				player.getInventory().addItem(4585, 1);
				player.getInventory().addItem(19348, 1);
				player.getInventory().refresh();
				return true;
			case 19337: //d platebody (or)
				if (player.getInventory().getFreeSlots() < 1) {
					player.sendMessage("You need at least one free inventory space to do this.");
					return true;
				}
				player.getInventory().deleteItem(itemId, 1);
				player.getInventory().addItem(14479, 1);
				player.getInventory().addItem(19350, 1);
				player.getInventory().refresh();
				return true;
			case 19340: //d sq shield (or)
				if (player.getInventory().getFreeSlots() < 1) {
					player.sendMessage("You need at least one free inventory space to do this.");
					return true;
				}
				player.getInventory().deleteItem(itemId, 1);
				player.getInventory().addItem(1187, 1);
				player.getInventory().addItem(19352, 1);
				player.getInventory().refresh();
				return true;
			case 19341: //d full helm (sp)
				if (player.getInventory().getFreeSlots() < 1) {
					player.sendMessage("You need at least one free inventory space to do this.");
					return true;
				}
				player.getInventory().deleteItem(itemId, 1);
				player.getInventory().addItem(11335, 1);
				player.getInventory().addItem(19354, 1);
				player.getInventory().refresh();
				return true;
			case 19343: //d platelegs (sp)
				if (player.getInventory().getFreeSlots() < 1) {
					player.sendMessage("You need at least one free inventory space to do this.");
					return true;
				}
				player.getInventory().deleteItem(itemId, 1);
				player.getInventory().addItem(4087, 1);
				player.getInventory().addItem(19356, 1);
				player.getInventory().refresh();
				return true;
			case 19344: //d plateskirt (sp)
				if (player.getInventory().getFreeSlots() < 1) {
					player.sendMessage("You need at least one free inventory space to do this.");
					return true;
				}
				player.getInventory().deleteItem(itemId, 1);
				player.getInventory().addItem(4585, 1);
				player.getInventory().addItem(19356, 1);
				player.getInventory().refresh();
				return true;
			case 19342: //d platebody (sp)
				if (player.getInventory().getFreeSlots() < 1) {
					player.sendMessage("You need at least one free inventory space to do this.");
					return true;
				}
				player.getInventory().deleteItem(itemId, 1);
				player.getInventory().addItem(14479, 1);
				player.getInventory().addItem(19358, 1);
				player.getInventory().refresh();
				return true;
			case 19345: //d sq shield (sp)
				if (player.getInventory().getFreeSlots() < 1) {
					player.sendMessage("You need at least one free inventory space to do this.");
					return true;
				}
				player.getInventory().deleteItem(itemId, 1);
				player.getInventory().addItem(1187, 1);
				player.getInventory().addItem(19360, 1);
				player.getInventory().refresh();
				return true;
			}
		}
		if(opcode == 82){
			//Summoning pouches:
			SummoningPouch pouch = SummoningPouch.POUCHES.get(itemId);
			if (pouch != null) {
				if (player.getFamiliar() != null) {
					player.sendMessage("You already have a follower.");
					return true;
				} else {
			        if (BarrowsConstants.isInBarrowsZone(player) 
			        		&& player.getActivity() instanceof BarrowsActivity
			        		&& !World.getWorld().getAreaManager().getAreaByName("BarrowsSurface").contains(player.getLocation())) {
			        	player.sendMessage("You can't summon a familiar in this area.");
			            return true;
			        }
			        if (World.getWorld().getAreaManager().getAreaByName("NormalArena").contains(player.getLocation()) 
			        		|| World.getWorld().getAreaManager().getAreaByName("ObstaclesArena").contains(player.getLocation())) {
			        	player.sendMessage("You can't summon a familiar in this arena.");
			            return true;
			        }
			        if (player.getActivity() instanceof CastleWarsActivity && Summoning.getIsBeastOfBurdenFromId(pouch.getNpcId())) {
			        	player.sendMessage("You can't summon a beast of burden familiar here.");
			        	return true;
			        }
			        if (player.getActivity() instanceof DungeoneeringActivity) {
			        	player.sendMessage("You can't summon a familiar here.");
			        	return true;
			        }
					if (player.getSkills().getLevelForExperience(23) < pouch.getLevelRequired()) {
						player.sendMessage("You need a Summoning level of "+pouch.getLevelRequired()+" to summon this familiar.");
						return true;
					}
					if (player.getSkills().getLevel(23) < pouch.getSummonCost()) {
						player.sendMessage("You do not have enough summoning points to summon this familiar.");
						return true;
					}
					player.getInventory().getContainer().set(slot, null);
					player.getInventory().refresh();
					//if (Summoning.getIsBeastOfBurdenFromId(pouch.getNpcId())) {
						//player.setBeastOfBurden(new BeastOfBurden(player, pouch.getNpcId(), 100));
						//player.getBeastOfBurden().summon();
					//} else {
						player.setFamiliar(new Familiar(player, pouch.getNpcId(), 100));
						player.getFamiliar().summon();
					//}
					player.getSkills().addExperience(23, pouch.getSummonExperience());
					player.getSkills().drainLevel(23, pouch.getSummonCost());
				}
				return true;
			}
			switch(itemId) {
			case 6865: //marionette
				player.animate(3006);
				player.graphics(514);
				return true;
			case 6866: //marionette
				player.animate(3006);
				player.graphics(518);
				return true;
			case 6867: //marionette
				player.animate(3006);
				player.graphics(510);
				return true;
			case 4079: //yo-yo crazy emote
				player.animate(1460);
				break;
			case 1712:
				player.sendMessage("You rub the amulet...");
				DialogueManager.sendOptionDialogue(player, new int[]{563, 564, 565, 566, -1}, "Edgeville", "Karamja", "Draynor Village", "Al Kharid","Cancel");
				break;
			case 1710:
				player.sendMessage("You rub the amulet...");
				DialogueManager.sendOptionDialogue(player, new int[]{567, 568, 569, 570, -1}, "Edgeville", "Karamja", "Draynor Village", "Al Kharid","Cancel");
				break;
			case 1708:
				player.sendMessage("You rub the amulet...");
				DialogueManager.sendOptionDialogue(player, new int[]{571, 572, 573, 574, -1}, "Edgeville", "Karamja", "Draynor Village", "Al Kharid","Cancel");
				break;
			case 1706:
				player.sendMessage("You rub the amulet...");
				DialogueManager.sendOptionDialogue(player, new int[]{575, 576, 577, 578, -1}, "Edgeville", "Karamja", "Draynor Village", "Al Kharid","Cancel");
				break;
			case 1704:
				player.sendMessage("The amulet has lost its charge.");
				player.sendMessage("It will need to be recharged before you can use it again.");
				break;
			case 3853: 
				player.sendMessage("You rub the necklace...");
				DialogueManager.sendOptionDialogue(player, new int[]{584, 585, -1, -1, -1}, "Troll Invasion", "Barbarian Outpost", "Gamers Grotto", "Corporal Beast","Cancel");
				break;
			case 3855:
				player.sendMessage("You rub the necklace...");
				DialogueManager.sendOptionDialogue(player, new int[]{586, 587, -1, -1, -1}, "Troll Invasion", "Barbarian Outpost", "Gamers Grotto", "Corporal Beast","Cancel");
				break;
			case 3857:
				player.sendMessage("You rub the necklace...");
				DialogueManager.sendOptionDialogue(player, new int[]{588, 589, -1, -1, -1}, "Troll Invasion", "Barbarian Outpost", "Gamers Grotto", "Corporal Beast","Cancel");
				break;
			case 3859:
				player.sendMessage("You rub the necklace...");
				DialogueManager.sendOptionDialogue(player, new int[]{590, 591, -1, -1, -1}, "Troll Invasion", "Barbarian Outpost", "Gamers Grotto", "Corporal Beast","Cancel");
				break;
			case 3861:
				player.sendMessage("You rub the necklace...");
				DialogueManager.sendOptionDialogue(player, new int[]{592, 593, -1, -1, -1}, "Troll Invasion", "Barbarian Outpost", "Gamers Grotto", "Corporal Beast","Cancel");
				break;
			case 3863:
				player.sendMessage("You rub the necklace...");
				DialogueManager.sendOptionDialogue(player, new int[]{594, 595, -1, -1, -1}, "Troll Invasion", "Barbarian Outpost", "Gamers Grotto", "Corporal Beast","Cancel");
				break;
			case 3865:
				player.sendMessage("You rub the necklace...");
				DialogueManager.sendOptionDialogue(player, new int[]{596, 597, -1, -1, -1}, "Troll Invasion", "Barbarian Outpost", "Gamers Grotto", "Corporal Beast","Cancel");
				break;
			case 3867:
				player.sendMessage("You rub the necklace...");
				DialogueManager.sendOptionDialogue(player, new int[]{598, 599, -1, -1, -1}, "Troll Invasion", "Barbarian Outpost", "Gamers Grotto", "Corporal Beast","Cancel");
				break;
			case 2552: 
				player.sendMessage("You rub the necklace...");
				DialogueManager.sendOptionDialogue(player, new int[]{600, 601, -1, -1, -1}, "Duel Arena", "Castle Wars", "Mobilising Armies", "Fist Of Guthix","Cancel");
				break;
			case 2554:
				player.sendMessage("You rub the necklace...");
				DialogueManager.sendOptionDialogue(player, new int[]{602, 603, -1, -1, -1}, "Duel Arena", "Castle Wars", "Mobilising Armies", "Fist Of Guthix","Cancel");
				break;
			case 2556:
				player.sendMessage("You rub the necklace...");
				DialogueManager.sendOptionDialogue(player, new int[]{604, 605, -1, -1, -1}, "Duel Arena", "Castle Wars", "Mobilising Armies", "Fist Of Guthix","Cancel");
				break;
			case 2558:
				player.sendMessage("You rub the necklace...");
				DialogueManager.sendOptionDialogue(player, new int[]{606, 607, -1, -1, -1}, "Duel Arena", "Castle Wars", "Mobilising Armies", "Fist Of Guthix","Cancel");
				break;
			case 2560:
				player.sendMessage("You rub the necklace...");
				DialogueManager.sendOptionDialogue(player, new int[]{608, 609, -1, -1, -1}, "Duel Arena", "Castle Wars", "Mobilising Armies", "Fist Of Guthix","Cancel");
				break;
			case 2562:
				player.sendMessage("You rub the necklace...");
				DialogueManager.sendOptionDialogue(player, new int[]{610, 611, -1, -1, -1}, "Duel Arena", "Castle Wars", "Mobilising Armies", "Fist Of Guthix","Cancel");
				break;
			case 2564:
				player.sendMessage("You rub the necklace...");
				DialogueManager.sendOptionDialogue(player, new int[]{612, 613, -1, -1, -1}, "Duel Arena", "Castle Wars", "Mobilising Armies", "Fist Of Guthix","Cancel");
				break;
			case 2566:
				player.sendMessage("You rub the necklace...");
				DialogueManager.sendOptionDialogue(player, new int[]{614, 615, -1, -1, -1}, "Duel Arena", "Castle Wars", "Mobilising Armies", "Fist Of Guthix","Cancel");
				break;
			case 227:
				player.getInventory().getContainer().remove(new Item(227, 1));
				player.getInventory().getContainer().add(new Item(229, 1));
				player.getInventory().refresh();
				break;
			case 1929:
				player.getInventory().getContainer().remove(new Item(1929, 1));
				player.getInventory().getContainer().add(new Item(1925, 1));
				player.getInventory().refresh();
				break;
			case 1937:
				player.getInventory().getContainer().remove(new Item(1937, 1));
				player.getInventory().getContainer().add(new Item(1935, 1));
				player.getInventory().refresh();
				break;
			default:
				System.out.println("Item option not added for Item "+itemId);
			}
		}
		
		Item item = player.getInventory().get(slot);
		if (item == null || (item != null && itemId != item.getId())) {
			return false;
		}
		if (opcode == 39) {
			if (player.getAttribute("Droptick", -1) > World.getTicks()) {
				player.sendMessage("You can't do this right after combat!");
				return false;
			}
			dropItem(player, slot, itemId);
			return true;
		}
		if (opcode == 13) {//wear
			int equipSlot = Equipment.getItemType(itemId);
			if (player.getActivity().getActivityId() == 0 && (equipSlot == Equipment.SLOT_CAPE || equipSlot == Equipment.SLOT_HAT)) {
				player.sendMessage("You can't equip a cape or hat in this activity.");
				return true;
			}
			if (itemId == 8856) {
				if (!World.getWorld().getAreaManager().getAreaByName("WGuildCatapult").contains(player.getLocation())) {
					player.sendMessage("You may not equip this shield outside the catapult room in the Warriors' Guild.");
					return false;
				}
					if (itemId == 2572 && player.getDonor() < 6 || player.getRights() < 1) {
							player.sendMessage("You must donate to use this item!");
							return false;
						
				}
				if (player.getEquipment().get(Equipment.SLOT_WEAPON) != null) {
					DialogueManager.sendInfoDialogue(player, "You will need to make sure your sword hand is free", "to equip this shield.");
					return false;
				}

			}
			if(itemId >= 5509 && itemId <= 5515){
				int pouchHealth = player.getInventory().getContainer().get(slot).getHealth();
				if(pouchHealth > 0){
					if(player.getInventory().getContainer().getFreeSlots() < pouchHealth){
						player.sendMessage("You don't have enough space to do that.");
						return false;
					}
					player.getInventory().getContainer().add(new Item(7936, pouchHealth));
					player.getInventory().getContainer().get(slot).setHealth(0);
					player.getInventory().refresh();
					return true;
				}else{
					player.sendMessage("Your pouch has no essence left in it.");
					return false;
				}
			}
			Talisman talisman = Talisman.getTalismanByTiara(itemId);
			if (talisman != null) {
				if(itemId == talisman.getTiaraId()){
					ActionSender.sendConfig(player, 491, talisman.getTiaraConfig());
					return true;
				}
			}
			if(itemId == 15362){
				int amount = player.getInventory().getContainer().getNumberOf(new Item(15362));
				player.getInventory().getContainer().remove(new Item(15362, amount));
				player.getInventory().getContainer().add(new Item(230, 50*amount));
				player.getInventory().refresh();
				return true;
			}
			if(itemId == 15364){
				int amount = player.getInventory().getContainer().getNumberOf(new Item(15364));
				player.getInventory().getContainer().remove(new Item(15364, amount));
				player.getInventory().getContainer().add(new Item(222, 50*amount));
				player.getInventory().refresh();
				return true;
			}
			if (itemId == 4079) { //yo-yo loop emote
				player.animate(1458);
				return true;
			}
			if (itemId >= 6865 && itemId <= 6867) { //marionettes
				player.animate(3004);
				if (itemId == 6865)
					player.graphics(512);
				else if (itemId == 6866)
					player.graphics(516);
				else
					player.graphics(508);
				return true;
			}
			player.getEquipment().equip(player, buttonId, slot, itemId, false);
		} else if (opcode == 58) { //examine
			if (player.getInventory().get(slot) != null) {
				player.sendMessage(item.getDefinition().getExamine());
			}
			return true;
		}
		if (player.getActivity().itemAction(player, item, 1, "ItemOption")) {
			return true;
		}
		ImplingJar jar = ImplingJar.forId(itemId);
		if (jar != null) {
			if (player.getInventory().getFreeSlots() < 1) {
				player.sendMessage("You do not have enough space in your inventory.");
				return true;
			}
			player.getInventory().deleteItem(itemId, 1, slot);
			if (player.getRandom().nextInt(10) < 2) {
				player.sendMessage("You break the jar as you try to open it. You throw the shattered remains away.");
			} else {
				player.getInventory().addItem(11260, 1);
			}
			Item loot = null;
			while (loot == null) {
				ChanceItem current = jar.getLoot()[player.getRandom().nextInt(jar.getLoot().length)];
				if (player.getRandom().nextInt(100) < current.getRarity()) {
					loot = current.getItem();
					break;
				}
			}
			player.getInventory().addItem(loot);
		}
		if(Talisman.forId(itemId) != null){
			Runecrafting runecrafting = new Runecrafting(player, player.getInventory().get(slot));
			player.submitTick("skill_action_tick", runecrafting, true);
		}
		if (Herb.forId(itemId) != null) {
			Herblore herblore = new Herblore(player, player.getInventory().get(slot), (byte) slot);
			herblore.execute();
			player.submitTick("skill_action_tick", herblore, true);
			return true;
		}
		if (itemId == SPADE) {
			player.animate(Animation.DIG_ANIMATION);
			World.getWorld().submit(new Tick(1) {
				public void execute() {
					player.animate(Animation.RESET);
					stop();
				}
			});
			ActionSender.sendMessage(player, "Nothing interesting happens.");
			return true;
		}
		if (itemId >= 6099 && itemId <= 6102) { //teleport crystals
			player.setAttribute("teleportCrystal", itemId);
			DialogueManager.sendOptionDialogue(player, new int[]{773, 774, 775, 776, -1}, "Save Location.", "Teleport to Saved Area.", "Draynor Village.", "Al Kharid.", "Cancel.");
		}
		if (opcode == 6) {
			if(Runecrafting.isPouch(player, itemId, slot)){
				return true;
			}
			switch (itemId) {
			case 5:
				BookManager.proceedBook(player, 42);
				return false;
			case 6865: //marionette
				player.animate(3003);
				player.graphics(511);
				return true;
			case 6199://Hard coded
				int randomItem = RandomItems[Misc.random(8)];
				int amount = 1;
				player.getInventory().addDropable(new Item(randomItem, amount));
				player.getInventory().getContainer().remove(new Item(6199, 1));
				player.getInventory().refresh();
				ActionSender.sendMessage(player, "You have recieved a rare item, thanks for your donation!");
				for (Player p : World.getWorld().getPlayers()) {
				p.sendMessage("<img=2><col=800080><shad=000000>" + Misc.formatPlayerNameForDisplay(player.getDisplayName())+ " got a random item from a donator box!");
				}
				return true;
			case 3062://Hard coded
				int NexItem = NexBox[Misc.random(7)];
				int amnt = 1;
				player.getInventory().addDropable(new Item(NexItem, amnt));
				player.getInventory().getContainer().remove(new Item(3062, 1));
				player.getInventory().refresh();
				ActionSender.sendMessage(player, "You have recieved a rare item, thanks for your donation!");
				for (Player p : World.getWorld().getPlayers()) {
				p.sendMessage("<img=2><col=800080><shad=000000>" + Misc.formatPlayerNameForDisplay(player.getDisplayName())+ " got a random item from a Nex box!");
				}
				return true;
			case 4447:
				int Random_ = RandomItem_[Misc.random(55)];
				int amt_ = 1;
				player.getInventory().addDropable(new Item(Random_, amt_));
				player.getInventory().getContainer().remove(new Item(4447, 1));
				player.getInventory().refresh();
				ActionSender.sendMessage(player, "You have recieved a random item, thanks for your donation!");
				for (Player p : World.getWorld().getPlayers()) {
				p.sendMessage("<img=2><col=800080><shad=000000>" + Misc.formatPlayerNameForDisplay(player.getDisplayName())+ " got a random item from the $5 box!");
				}
				return true;
			case 18781: //Dragonkinlamp
				DialogueManager.proceedDialogue(player, 5100);
				return true;
			case 6866: //marionette
				player.animate(3003);
				player.graphics(515);
				return true;
			case 6867: //marionette
				player.animate(3003);
				player.graphics(507);
				return true;
			case 4079: //yo-yo play emote
				player.animate(1457);
				return true;
			case 757:
				BookManager.proceedBook(player, 1);
				return true;
			case 1856:
				BookManager.proceedBook(player, 4);
				return true;
			case 11640: //book of knowledge
				BookManager.proceedBook(player, 20);
				return true;
			case 7144: //book o' piracy
				BookManager.proceedBook(player, 40);
				return true;
			case 8007:
			case 8008:
			case 8009:
			case 8010:
			case 8011:
			case 8012:
			case 8013:
				TeleportHandler.teletab(player, item, TeleportHandler.getLocation(itemId), true);
				return true;
			case 15362:
				player.getInventory().getContainer().remove(new Item(15362, 1));
				player.getInventory().getContainer().add(new Item(230, 50));
				player.getInventory().refresh();
				return true;
			case 15364:
				player.getInventory().getContainer().remove(new Item(15364, 1));
				player.getInventory().getContainer().add(new Item(222, 50));
				player.getInventory().refresh();
				return true;
			case 2714: //easy
			case 2802: //medium
			case 2724: //hard
			case 19039: //elite
				Casket.openCasket(player, itemId);
				return true;
			}
		}
		return true;
	}

	public void dropItem(Player player, int slot, int itemId) {
		GraveStone grave = GraveStoneManager.forName(player.getUsername());
		if (grave != null && player.getLocation() == grave.getGrave().getLocation()) {
			player.sendMessage("Surely you aren't going to drop litter on your own grave!");
			return;
		}
		Item item = player.getInventory().getContainer().get(slot);
		if (item == null) {
			return;
		}
		if (player.getActivity() instanceof DuelActivity) {
			DuelActivity duel = (DuelActivity) player.getActivity();
			if (duel.getCurrentState() == State.FIGHTING) {
				player.sendMessage("You can't drop any items during a duel.");
				return;
			}
		}
		if (itemId == 6105 || itemId == 20428) {
			//Giant heads...
			return;
		}
		//ActionSender.sendCloseChatBox(player);
		//ActionSender.sendCloseInterface(player);
		//ActionSender.sendCloseInventoryInterface(player);
		//player.getPriceCheck().close();
		//if (player.getTradeSession() != null) {
			//player.getTradeSession().tradeFailed(player);
		//}
		player.closeAll(true, true);
		if (itemId != 4045 && ItemDefinition.forId(itemId).isDropable()) {
			if (player.isInWilderness() && item.getDefinition().isTradeable()) //I think that 250 update ticks is good (that is 150 s. and is how long an item is visible after it got public).
				GroundItemManager.createGroundItem(new GroundItem(player, item, player.getLocation(), true, player.getRights() >= 2, GroundItemManager.groundItemIndex++), 250);
			else
				GroundItemManager.createGroundItem(new GroundItem(player, item, player.getLocation(), false, player.getRights() >= 2, GroundItemManager.groundItemIndex++));
		} else if (itemId == 4045) {
			player.getDamageManager().miscDamage(150, DamageType.RED_DAMAGE);
			player.getMask().setForceText(new ForceText("Ow! The liquid exploded!"));
		} else {
			ActionSender.sendChatboxInterface(player, 94);
			ActionSender.sendString(player, 94, 2, "Are you sure you want to destroy this object?");
			ActionSender.sendString(player, 94, 8, ItemDefinition.forId(itemId).getName());
			ActionSender.sendString(player, 94, 7, "<br>If you destroy this item, you will have to earn it again.");
			ActionSender.sendItemOnInterface(player, 94, 9, -1, itemId);
			player.removeAttribute("buyItem");
			player.setAttribute("destroyItem", item);
			player.setAttribute("destroyItemSlot", slot);
			return;
		}
		player.getInventory().getContainer().remove(slot, item);
		World.getWorld().getPlayerLoader().save(player);
		player.getInventory().refresh();
}
public static final int[] RandomItems = {1038, 1040, 1042, 1044, 1046, 1048, 1050, 1053, 1055, 1057};
	
public static final int[] NexBox = {20135, 20139, 20143, 20147, 20151, 20155, 20159, 20163, 20167};

public static final int[] RandomItem_ = {1050, 1053, 1055, 1057, 11694, 11696, 11698, 11700, 10350, 10349, 10346, 10352, 10342, 10344, 10340, 10339, 10337, 10334, 10332, 10330, 14484, 20171, 11724, 11726, 11720, 11722, 2577, 6585, 19333, 14479, 4151, 4087, 11335, 11283, 6889, 6914, 13754, 13899, 13887, 13893, 1965, 2313, 1942, 9724, 9726, 15679, 4566, 4083, 343, 357, 369, 375, 381, 10748, 10750, 10752, 10754};
}

