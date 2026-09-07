package org.dementhium.event.impl.interfaces;

import org.dementhium.event.EventListener;
import org.dementhium.event.EventManager;
import org.dementhium.model.definition.ItemDefinition;
import org.dementhium.model.player.Player;


public class CastleWarsRewardsListener extends EventListener {

	@Override
	public void register(EventManager manager) {
		manager.registerInterfaceListener(60, this);
	}
	
	@Override
	public boolean interfaceOption(final Player player, int interfaceId, int buttonId, int buttonId2, int itemId, int opcode) {
		if (interfaceId != 60)
			return false;
		int ticketCost = -1;
		int rewardId = -1;
		int amount = 1;
		switch (buttonId) {
		//Decorative Armour:
		case 173: //Basic:
			ticketCost = 4;
			rewardId = 4071;
			break;
		case 178:
			ticketCost = 8;
			rewardId = 4069;
			break;
		case 183:
			ticketCost = 6;
			rewardId = 4070;
			break;
		case 188:
			ticketCost = 6;
			rewardId = 4072;
			break;
		case 193:
			ticketCost = 5;
			rewardId = 4068;
			break;
		case 198: //Detailed:
			ticketCost = 40;
			rewardId = 4506;
			break;
		case 203:
			ticketCost = 80;
			rewardId = 4504;
			break;
		case 208:
			ticketCost = 60;
			rewardId = 4505;
			break;
		case 213:
			ticketCost = 60;
			rewardId = 4507;
			break;
		case 218:
			ticketCost = 50;
			rewardId = 4503;
			break;
		case 163: //Intricate:
			ticketCost = 400;
			rewardId = 4511;
			break;
		case 164:
			ticketCost = 800;
			rewardId = 4509;
			break;
		case 165:
			ticketCost = 600;
			rewardId = 4510;
			break;
		case 166:
			ticketCost = 600;
			rewardId = 4512;
			break;
		case 167:
			ticketCost = 500;
			rewardId = 4508;
			break;
		case 168: //Profound:
			ticketCost = 650;
			rewardId = 18708;
			break;
		case 169:
			ticketCost = 1100;
			rewardId = 18706;
			break;
		case 170:
			ticketCost = 800;
			rewardId = 18707;
			break;
		case 171:
			ticketCost = 800;
			rewardId = 18709;
			break;
		case 172:
			ticketCost = 800;
			rewardId = 18705;
			break;
			
		//Consumables:
		case 47:
			ticketCost = 2;
			rewardId = 18710;
			break;
		case 48:
			ticketCost = 2;
			rewardId = 18711;
			break;
		case 49:
			ticketCost = 2;
			rewardId = 18712;
			break;
		case 54:
			ticketCost = 2;
			rewardId = 18713;
			break;
		case 55:
			ticketCost = 4;
			rewardId = 18714;
			amount = 100;
			break;
			//Potion sets are done below
			
		//Miscellaneous:
		case 93:
			ticketCost = 10;
			rewardId = 4514;
			break;
		case 98:
			ticketCost = 10;
			rewardId = 4516;
			break;
		case 103:
			ticketCost = 10;
			rewardId = 4513;
			break;
		case 108:
			ticketCost = 10;
			rewardId = 4515;
			break;
		case 109:
			ticketCost = 2;
			rewardId = 18739;
			break;
		case 110:
			ticketCost = 2;
			rewardId = 18740;
			break;
		case 111:
			ticketCost = 2;
			rewardId = 18741;
			break;
		case 112:
			ticketCost = 2;
			rewardId = 18742;
			break;
		case 113:
			ticketCost = 2;
			rewardId = 18743;
			break;
		case 114:
			ticketCost = 300;
			rewardId = 18744;
			break;
		case 115:
			ticketCost = 300;
			rewardId = 18745;
			break;
		case 116:
			ticketCost = 300;
			rewardId = 18746;
			break;
		case 117:
			ticketCost = 200;
			rewardId = 18747;
			break;
		case 118:
			ticketCost = 0;
			rewardId = 4055;
			break;
		}
		if (buttonId >= 56 && buttonId <= 58) { //Potion sets.
			if (opcode == 6) {
				if (player.getInventory().getContainer().getItemCount(4067) >= ticketCost) {
					int freeSlotsNeeded = 3;
					if (buttonId == 56)
						freeSlotsNeeded++;
					if (player.getInventory().getContainer().getItemCount(4067) == 1)
						freeSlotsNeeded--;
					if (player.getInventory().getFreeSlots() >= freeSlotsNeeded) {
						player.getInventory().deleteItem(4067, 1);
						switch (buttonId) {
						case 56:
							player.getInventory().addItem(18715, 1); //att
							player.getInventory().addItem(18719, 1); //str
							break;
						case 57:
							player.getInventory().addItem(18731, 1); //ranging
							break;
						case 58:
							player.getInventory().addItem(18735, 1); //magic
							break;
						}
						player.getInventory().addItem(18723, 1); //def
						player.getInventory().addItem(18727, 1); //run
						player.getInventory().refresh();
					} else
						player.sendMessage("You need at least "+(freeSlotsNeeded == 4 ? "four" : "three")+" free inventory spaces to buy this item.");	
				} else
					player.sendMessage("You do not have enough castle wars tickets to buy this item.");
			} else if (opcode == 13) {
				String examine = "A potion set containing boosts to ";
				switch (buttonId) {
				case 56:
					examine += "Attack, Strength, ";
					break;
				case 57:
					examine += "Ranged, ";
					break;
				case 58:
					examine += "Magic, ";
					break;
				}
				examine += "Defence and run energy. Can only";
				player.sendMessage(examine);
				player.sendMessage("be used inside the arena.");
			}
			return true;
		}
		if (ticketCost > -1 && rewardId > -1) {
			if (opcode == 6) {
				if (player.getInventory().getContainer().getItemCount(4067) >= ticketCost) {
					if (player.getInventory().getFreeSlots() > 0
							|| (ItemDefinition.forId(rewardId).isStackable() && player.getInventory().contains(rewardId) && (player.getInventory().getContainer().getItemCount(rewardId) + amount) > 0)
							|| player.getInventory().getContainer().getItemCount(4067) == ticketCost) {
						player.getInventory().deleteItem(4067, ticketCost);
						player.getInventory().addItem(rewardId, amount);
						player.getInventory().refresh();
					} else
						player.sendMessage("You need at least one free inventory space to buy this item.");	
				} else
					player.sendMessage("You do not have enough castle wars tickets to buy this item.");
			} else if (opcode == 13)
				player.sendMessage(ItemDefinition.forId(rewardId).getExamine());
		}
		return true;
	}

}
