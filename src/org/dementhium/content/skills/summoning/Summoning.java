package org.dementhium.content.skills.summoning;

import java.util.List;

import org.dementhium.model.Item;
import org.dementhium.model.World;
import org.dementhium.model.definition.ItemDefinition;
import org.dementhium.model.map.GameObject;
import org.dementhium.model.map.Region;
import org.dementhium.model.mask.Animation;
import org.dementhium.model.player.Player;
import org.dementhium.net.ActionSender;
import org.dementhium.tickable.Tick;


public class Summoning {
	
	public static int POUCH_CREATING = 672;
	public static int SCROLL_CREATING = 666;
	
	public static void sendInterface(Player player, int creationInterface) {
		if (creationInterface == POUCH_CREATING)
			ActionSender.CreatePouchOptions(player);
		else if (creationInterface == SCROLL_CREATING)
			ActionSender.CreateScrollOptions(player);
	}

	public static void createPouch(final Player player, int itemId, int slot, int amount) {
		if (amount <= 0)
			return;
		SummoningPouch pouch = SummoningPouch.POUCHES.get(itemId);
		if (pouch == null) {
			int pouchId = getPouchIdFromSlotId(slot);
			if (pouchId == 0)
				return;
			pouch = SummoningPouch.POUCHES.get(pouchId);
		}
		if (pouch != null) {
			if(player.getSkills().getLevelForExperience(23) < pouch.getLevelRequired()) {
				player.sendMessage("You need a Summoning level of "+pouch.getLevelRequired()+" to infuse this pouch.");
				return;
			}
			if (amount > 28)
				amount = 28;
			int freeInvSpace = player.getInventory().getFreeSlots();
			for (int i = 0; i <= amount; i++) {
				for (Item requiredItem : pouch.getItems()) {
					if (i == 0 && !player.getInventory().contains(requiredItem)) {
						player.sendMessage("You do not have the required items to infuse this pouch.");
						return;
					}
					if (!player.getInventory().contains(new Item(requiredItem.getId(), requiredItem.getAmount() * (i+1)))) {
						amount = i;
						break;
					}
					if (i == 0 && player.getInventory().getContainer().getItemCount(requiredItem.getId()) - requiredItem.getAmount() == 0 && requiredItem.getDefinition().isStackable())
						freeInvSpace++;
					else if (!requiredItem.getDefinition().isStackable())
						freeInvSpace += requiredItem.getAmount();
				}
			}
			if (amount <= 0)
				return;
			if (amount > freeInvSpace)
				amount = freeInvSpace;
			if (freeInvSpace < 1) {
				player.sendMessage("You need at least one free inventory space to infuse this pouch.");
				return;
			}
			for (Item requiredItem : pouch.getItems()) {
				player.getInventory().deleteItem(new Item(requiredItem.getId(), requiredItem.getAmount() * amount));
			}
			player.getInventory().addItem(pouch.getPouchId(), amount);
			player.getInventory().refresh();
			player.getSkills().addExperience(23, pouch.getCreateExperience() * amount);
			player.closeAll(true, true);
			player.animate(9068); //827 was wrong
			final GameObject object = player.getAttribute("summoningObelisk", null);
			World.getWorld().submit(new Tick(4) {
                @Override
                public void execute() {
                	stop();
                	player.animate(-1);
    				for (Player p : Region.getLocalPlayers(object.getLocation())) {
    					ActionSender.sendAnimateObject(p, object, 8510);
    				}
                }
            });
			if (object != null) {
				List<Player> players = Region.getLocalPlayers(player.getLocation());
				for (Player p : players) {
					ActionSender.sendAnimateObject(p, object, 8509);
				}
			}
			//Gfx2 8509 GFX Id: 4277 wr Gfx2 8510
			//player.sendMessage("You infuse a/some (if amount bigger than 1) "+ItemDefinition.forId(pouch.getPouchId()).getName()+".");
		} else
			player.sendMessage("You do not have the required items to infuse this pouch.");
	}
	
	public static void sendRequiredItemsList(Player player, int itemId, int slot) {
		SummoningPouch pouch = SummoningPouch.POUCHES.get(itemId);
		if (pouch == null) {
			int pouchId = getPouchIdFromSlotId(slot);
			if (pouchId == 0)
				return;
			pouch = SummoningPouch.POUCHES.get(pouchId);
		}
		if (pouch != null) {
			//int amount = 0;
			//for (Item requiredItem : pouch.getItems()) {
				//amount++;
			//}
			int amount = pouch.getItems().length;
			int amount2 = 0;
			String requiredItems = ItemDefinition.forId(pouch.getPouchId()).getName()+" requires:";
			String requiredItems2 = "";
			for (Item requiredItem : pouch.getItems()) {
				amount2++;
				boolean isLast = amount2 == amount;
				boolean isOneToLast = amount2 + 1 == amount;
				if (requiredItems.length() + (" "+(isLast ? "and " : "")+requiredItem.getAmount()+" x "+requiredItem.getDefinition().getName()).length() < 85)
					requiredItems += " "+requiredItem.getAmount()+" x "+requiredItem.getDefinition().getName()+(isLast ? "." : (isOneToLast ? "" : ","));
				else {
					if (requiredItems2.equals(""))
						requiredItems2 += (isLast ? "and " : "")+requiredItem.getAmount()+" x "+requiredItem.getDefinition().getName()+(isLast ? "." : (isOneToLast ? "" : ","));
					else
						requiredItems2 += " "+(isLast ? "and " : "")+requiredItem.getAmount()+" x "+requiredItem.getDefinition().getName()+(isLast ? "." : (isOneToLast ? "" : ","));
				}
			}
			player.sendMessage(requiredItems);
			if (!requiredItems2.equals(""))
				player.sendMessage(requiredItems2);
		}
	}
	
	//REDO THE METHOD BELOW (if needed):
	 /**
	  * Handles the creating of a scroll.
	  * @param player The player.
	  * @param itemId The item id to create.
	  * @param amount The amount to make.
	  */
	public static void createScroll(final Player player, int itemId, int amount) {
		final GameObject obelisk = player.getAttribute("summoningObelisk");
	  /*  if (obelisk == null) {
	   return;
	  }*/
		SummoningScroll scroll = SummoningScroll.get(itemId);
		if (scroll == null) {
			ItemDefinition def = ItemDefinition.forId(itemId);
			if (def != null && def.getSkillRequirementId() != null) {
				for (int i = 0; i < def.getSkillRequirementId().size(); i++) {
					if (def.getSkillRequirementId().get(i) == 23) {
						if (player.getSkills().getLevelForExperience(23) < def.getSkillRequirementLvl().get(i)) {
							player.sendMessage("You need a summoning level of " + def.getSkillRequirementLvl().get(i) + " to create this scroll.");
							return;
						}
						break;
					}
				}
			} else {
				System.out.println("Def is " + def + ", skill requirement: " + def.getSkillRequirementId());
			}
			player.sendMessage("You do not have the items required to create this scroll.");
			return;
		}
		ActionSender.sendCloseInterface(player);
		boolean end = false;
		int i = 0;
		for (i = 0; i < amount; i++) {
			if (!player.getInventory().contains(scroll.getPouch())) {
				if (amount == 1) {
					player.sendMessage("You do not have the items required to create this scroll.");
				}
				end = true;
				break;
			}
			if (end) {
				break;
			}
			player.getInventory().removeItems(false, scroll.getPouch());
			player.getInventory().addItem(scroll.getItemId(), 10, false, true);
			player.getSkills().addExperience(23, scroll.getExperience());
		}
		if (i == 1) {
			player.sendMessage("You transform a " + ItemDefinition.forId(itemId).getName().toLowerCase() + ".");
		} else if (i > 0) {
			player.sendMessage("You transform some " + ItemDefinition.forId(itemId).getName().toLowerCase() + "es.");
		} else {
			return;
		}
		player.animate(Animation.create(9068));
		World.getWorld().submit(new Tick((int) 6.5) {
			@Override
			public void execute() {
				player.animate(Animation.create(-1));
				this.stop();
			}
		});
		player.getInventory().refresh();
	}
	
	public static boolean getIsBeastOfBurdenFromId(int id) {
		switch (id) {
		case 6806: //thorny snail
		case 6994: //spirit kalphite
		case 6867: //bull ant
		case 6794: //spirit terrorbird
		case 6818: //abyssal parasite
		case 6820: //abyssal lurker
		case 6815: //war toirtoise
		case 7349: //abyssal titan
		case 6873: //pack yak
			return true;
		default:
			return false;
		}
	}
	
	//TODO: Fix this method:
	private static int getPouchIdFromSlotId(int buttonId) {
		int pouchId = 0;
		switch (buttonId) {
		case 2: //12231: //spirit wolf
			pouchId = 12047;
			break;
		case 7: //12225: //dreadfowl
			pouchId = 12043;
			break;
		case 12: //12236: //spirit spider
			pouchId = 12059;
			break;
		case 17: //12255: //thorny snail
			pouchId = 12019;
			break;
		case 22: //12226: //granite crab
			pouchId = 12009;
			break;
		case 27: //12286: //spirit mosquito
			pouchId = 12778;
			break;
		case 32: //12256: //desert wyrm
			pouchId = 12049;
			break;
		case 37: //12240: //spirit scorpion
			pouchId = 12055;
			break;
		case 42: //12290: //spirit tz-kih
			pouchId = 12808;
			break;
		case 47: //12245: //albino rat
			pouchId = 12067;
			break;
		case 52: //12241: //spirit kalphite
			pouchId = 12064;
			break;
		case 57: //12237: //compost mound
			pouchId = 12091;
			break;
		case 62: //12276: //giant chinchompa
			pouchId = 12800;
			break;
		case 67: //12227: //vampire bat
			pouchId = 12053;
			break;
		case 72: //12246: //honey badger
			pouchId = 12065;
			break;
		case 77: //12232: //beaver
			pouchId = 12021;
			break;
		case 82: //12282: //void ravager
			pouchId = 12818;
			break;
		case 87: //12284: //void spinner
			pouchId = 12781;
			break;
		case 92: //12283: //void torcher
			pouchId = 12798;
			break;
		case 97: //12285: //void shifter
			pouchId = 12814;
			break;
		case 102: //12243: //bull ant
			pouchId = 12087;
			break;
		case 107: //12228: //macaw pouch
			pouchId = 12071;
			break;
		case 112: //12229: //evil turnip
			pouchId = 12051;
			break;
		case 117: //12266: //sp. cockatrice
			pouchId = 12095;
			break;
			
		case 122: //sp. guthatrice
			pouchId = 12097;
			break;
		case 127: //sp. saratrice
			pouchId = 12099;
			break;
		case 132: //sp. zamatrice
			pouchId = 12101;
			break;
		case 137: //sp. pengatrice
			pouchId = 12103;
			break;
		case 142: //sp. coraxatrice
			pouchId = 12105;
			break;
		case 147: //sp. vulatrice
			pouchId = 12107;
			break;
		case 152: //pyrelord
			pouchId = 12816;
			break;
		case 157: //magpie
			pouchId = 12041;
			break;
		case 162: //bloated leech
			pouchId = 12061;
			break;
		case 167: //spirit terrorbird
			pouchId = 12007;
			break;
		case 172: //abyssal parasite
			pouchId = 12035;
			break;
		case 177: //spirit jelly
			pouchId = 12027;
			break;
		case 182: //ibis
			pouchId = 12531;
			break;
		case 187: //spirit kyatt
			pouchId = 12812;
			break;
		case 192: //spirit larupia
			pouchId = 12784;
			break;
		case 197: //spirit graahk
			pouchId = 12710;
			break;
		case 202: //karam. overlord
			pouchId = 12023; //?
			break;
		case 207: //smoke devil
			pouchId = 12085;
			break;
		case 212: //abyssal lurker
			pouchId = 12037;
			break;
		case 217: //spirit cobra
			pouchId = 12015;
			break;
		case 222: //stranger plant
			pouchId = 12045;
			break;
		case 227: //barker toad
			pouchId = 12123;
			break;
		case 232: //war toirtoise
			pouchId = 12031;
			break;
		case 237: //bunyup
			pouchId = 12029;
			break;
		case 242: //fruit bat
			pouchId = 12033;
			break;
		case 247: //ravenous locust
			pouchId = 12820;
			break;
		case 252: //arctic bear
			pouchId = 12057;
			break;
		case 257: //phoenix
			pouchId = 14623;
			break;
		case 262: //obsidian golem
			pouchId = 12792;
			break;
		case 267: //granite lobster
			pouchId = 12069;
			break;
		case 272: //praying mantis
			pouchId = 12011;
			break;
		case 277: //forge regent
			pouchId = 12782;
			break;
		case 282: //talon beast
			pouchId = 12794;
			break;
		case 287: //giant ent
			pouchId = 12013;
			break;
		case 292: //fire titan
			pouchId = 12802;
			break;
		case 297: //moss titan
			pouchId = 12804;
			break;
		case 302: //ice titan
			pouchId = 12806;
			break;
		case 307: //hydra
			pouchId = 12025;
			break;
		case 312: //spirit dagannoth
			pouchId = 12017;
			break;
		case 317: //lava titan
			pouchId = 12788;
			break;
		case 322: //swamp titan
			pouchId = 12776;
			break;
		case 327: //bronze minotaur
			pouchId = 12073;
			break;
		case 332: //iron minotaur
			pouchId = 12075;
			break;
		case 337: //steel minotaur
			pouchId = 12077;
			break;
		case 342: //mithril minotaur
			pouchId = 12079;
			break;
		case 347: //adamant minotaur
			pouchId = 12081;
			break;
		case 352: //rune minotaur
			pouchId = 12083;
			break;
		case 357: //unicorn stallion
			pouchId = 12039;
			break;
		case 362: //geyser titan
			pouchId = 12786;
			break;
		case 367: //wolpertinger
			pouchId = 12089;
			break;
		case 372: //abyssal titan
			pouchId = 12796;
			break;
		case 377: //iron titan
			pouchId = 12822;
			break;
		case 382: //pack yak
			pouchId = 12093;
			break;
		case 387: //steel titan
			pouchId = 12790;
			break;	
		}
		return pouchId;
	}

}
