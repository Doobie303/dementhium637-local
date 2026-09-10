package org.dementhium.net.packethandlers;

import java.text.NumberFormat;

import org.dementhium.content.BookManager;
import org.dementhium.content.DialogueManager;
import org.dementhium.content.dialogue.Dialogue;
import org.dementhium.content.misc.WildernessDitch;
import org.dementhium.content.skills.Fletching;
import org.dementhium.content.skills.cooking.Cooking;
import org.dementhium.content.skills.cooking.Cooking.CookingMethod;
import org.dementhium.content.skills.crafting.GemCutting.GemCuttingAction;
import org.dementhium.content.skills.crafting.LeatherCrafting.LeatherProduction;
import org.dementhium.content.skills.crafting.LeatherCrafting.LeatherProductionAction;
import org.dementhium.content.skills.herblore.Herblore;
import org.dementhium.content.skills.magic.TeleportHandler;
import org.dementhium.content.skills.smithing.Smelting;
import org.dementhium.content.skills.smithing.SmithingUtils.SmeltingBar;
import org.dementhium.event.impl.FairyRing;
import org.dementhium.event.impl.SpiritTreeListener;
import org.dementhium.model.Item;
import org.dementhium.model.Location;
import org.dementhium.model.World;
import org.dementhium.model.mask.Animation;
import org.dementhium.model.mask.Graphic;
import org.dementhium.model.player.Player;
import org.dementhium.model.player.Skills;
import org.dementhium.net.ActionSender;
import org.dementhium.net.PacketHandler;
import org.dementhium.net.message.Message;
import org.dementhium.tickable.Tick;
import org.dementhium.util.Constants;

public class DialogueHandler extends PacketHandler {

	@Override
	public void handlePacket(Player player, Message packet) {
		switch (packet.getOpcode()) {
		case 4:
			appendDialogue(player, packet);
			break;
		}

	}

	private void appendDialogue(final Player player, Message packet) {
		packet.readShort();
		int buttonId = packet.readLEShort();
		int interfaceId = packet.readLEShort();
		System.out.println("Interface: " + interfaceId + ", button: " + buttonId);
		switch (interfaceId) {
		case 382: //Wildi warning message.
			if (buttonId == 19) {
				if (player.getAttribute("teleportingToWild") != null && player.getAttribute("teleportingToWildMethod") != null) {
					player.closeAll(true, true);
					final Location coords = player.getAttribute("teleportingToWild", null);
					int method = player.getAttribute("teleportingToWildMethod", null);
					if (method == 0) //Mob.java
						player.teleport(coords, false);
					else if (method == 1) //Mob.java
						player.teleportWithAnimAndGfx(coords, false, false);
					else if (method >= 2 && method <= 6) { //TeleportHandler.java
						if (method == 2) {
							boolean leverTeleport = false;
							if (player.getAttribute("teleportingToWildLever") != null)
								leverTeleport = player.getAttribute("teleportingToWildLever");
			    			final int distance = player.getAttribute("teleportingToWildDistance", null);
			    			final int ticks = player.getAttribute("teleportingToWildTicks", null);
			    			final boolean ignoreLvl = player.getAttribute("teleportingToWildIgnoreLvl", null);
			    			if (leverTeleport) {
			    				player.animate(2140); //835 was wrong
			    				player.sendMessage("You pull the lever ...");
			    				World.getWorld().submit(new Tick(1) {
			    					@Override
			    					public void execute() {
			    						stop();
			    						TeleportHandler.telePlayer(player, coords.getX(), coords.getY(), coords.getZ(), distance, ticks, ignoreLvl, false);
			    						player.sendMessage("... and teleport into the Wilderness.");
			    					}
			    				});
			    			} else
			    				TeleportHandler.telePlayer(player, coords.getX(), coords.getY(), coords.getZ(), distance, ticks, ignoreLvl, false);
							player.removeAttribute("teleportingToWildDistance");
							player.removeAttribute("teleportingToWildTicks");
							player.removeAttribute("teleportingToWildIgnoreLvl");
						} else if (method == 3) {
			    			int distance = player.getAttribute("teleportingToWildDistance", null);
			    			int ticks = player.getAttribute("teleportingToWildTicks", null);
							int teleItem = player.getAttribute("teleportingToWildTeleItem", null);
							boolean ignoreLvl = player.getAttribute("teleportingToWildIgnoreLvl", null);
							TeleportHandler.telePlayer(player, coords.getX(), coords.getY(), coords.getZ(), distance, ticks, ignoreLvl, teleItem, false);
							player.removeAttribute("teleportingToWildDistance");
							player.removeAttribute("teleportingToWildTicks");
							player.removeAttribute("teleportingToWildTeleItem");
							player.removeAttribute("teleportingToWildIgnoreLvl");
						} else if (method == 4) {
							int spellId = player.getAttribute("teleportingToWildSpellId", null);
							TeleportHandler.spellbookTeleport(player, spellId, false);
							player.removeAttribute("teleportingToWildSpellId");
						} else if (method == 5) {
							Animation start = player.getAttribute("teleportingToWildStart", null);
							Graphic sg = player.getAttribute("teleportingToWildSq", null);
			    			Animation end = player.getAttribute("teleportingToWildEnd", null);
			    			Graphic eg = player.getAttribute("teleportingToWildEg", null);
			    			int ticks = player.getAttribute("teleportingToWildTicks", null);
							TeleportHandler.teleport(player, coords, start, sg, end, eg, ticks, false);
							player.removeAttribute("teleportingToWildStart");
							player.removeAttribute("teleportingToWildSq");
			    			player.removeAttribute("teleportingToWildEnd");
			    			player.removeAttribute("teleportingToWildEg");
							player.removeAttribute("teleportingToWildTicks");
						}
						else if (method == 6) {
							Item item = player.getAttribute("teleportingToWildItem", null);
							TeleportHandler.teletab(player, item, coords, false);
							player.removeAttribute("teleportingToWildItem");
						}
						else if (method == 7) {
							FairyRing.fairyRingTeleport(player, coords, false);
						}
						else if (method == 8) {
							SpiritTreeListener.vineTeleport(player, coords, false);
						}
					}
				} else {
					player.closeAll(true, true);
			        player.setAttribute("busy", Boolean.TRUE);
			        player.setAttribute("stallRegion", Boolean.TRUE);

			        int y = 3;
			        int dir = 0;
			        //Animation.create(6132)
			        player.forceMovement(WildernessDitch.CROSS_ANIMATION, player.getLocation().getX(), y < 0 ? 3520 : 3523, 33, 60, dir, 2, true);
				}
				player.removeAttribute("teleportingToWild");
				player.removeAttribute("teleportingToWildMethod");
			}
			break;
		case 64:
		case 65:
		case 66:
		case 67:
		case 241:
		case 242:
		case 243:
		case 244:
			Dialogue dialogue = player.getAttribute("dialogue");
			if (dialogue != null) {
				if (dialogue.getActions().get(0).handle(player)) {
					ActionSender.sendCloseChatBox(player);
					player.setAttribute("dialogue", null);
					return;
				}
				return;
			}
			DialogueManager.processNextDialogue(player, -1);
			break;
		case 226:
		case 228:
		case 230:
		case 232:
		case 234:
			dialogue = player.getAttribute("dialogue");
			if (dialogue != null) {
				int option = buttonId - 2;
				if (option < 0) {
					option = 0;
				}
				if (dialogue.getActions().get(option).handle(player)) {
					ActionSender.sendCloseChatBox(player);
					player.setAttribute("dialogue", null);
					return;
				}
				return;
			}
			DialogueManager.processNextDialogue(player, buttonId - 2);
			break;
		case 718: //2 option inter with room for long title (used for christmass cracker e.g.)
			dialogue = player.getAttribute("dialogue");
			if (dialogue != null) {
				int option = buttonId - 1;
				if (option < 0) {
					option = 0;
				}
				if (dialogue.getActions().get(option).handle(player)) {
					ActionSender.sendCloseChatBox(player);
					player.setAttribute("dialogue", null);
					return;
				}
				return;
			}
			DialogueManager.processNextDialogue(player, buttonId - 1);
		case 210:
		case 211:
		case 212:
		case 213:
		case 214: // Five-line display box (sendDisplayBox supports one through five lines).
			dialogue = player.getAttribute("dialogue");
			if (dialogue != null) {
				if (dialogue.getActions().get(0).handle(player)) {
					ActionSender.sendCloseChatBox(player);
					player.setAttribute("dialogue", null);
					return;
				}
				return;
			}
			DialogueManager.processNextDialogue(player, -1);
			break;
		case 94:
			if (buttonId == 3) {
				if (player.getAttribute("buyItem") != null && player.getAttribute("destroyItem") == null) {
					Item buyItem = player.getAttribute("buyItem", null);
					int itemPrice = player.getAttribute("buyItemPrice", null);
					if (player.getInventory().addItem(buyItem.getId(), buyItem.getAmount())) {
						player.getInventory().deleteItem(995, itemPrice, true);
						NumberFormat nf1 = NumberFormat.getInstance();
						player.sendMessage("You bought "+(buyItem.getAmount() != 1 ? buyItem.getAmount()+" x " : "") +buyItem.getDefinition().getName()+" for "+(itemPrice == 0 ? "free!" : nf1.format(itemPrice)+" coins."));
					}
				} else if (player.getAttribute("destroyItem") != null && player.getAttribute("buyItem") == null) {
					Item item = player.getAttribute("destroyItem", null);
					int slot = player.getAttribute("destroyItemSlot", null);
					//player.getInventory().deleteItem(item.getId(), item.getAmount());
					//player.getInventory().getContainer().remove(new Item(item.getId(), item.getAmount()));
					player.getInventory().getContainer().remove(slot, item);
					player.getInventory().refresh();
				}
			}
			ActionSender.sendCloseChatBox(player);
			player.removeAttribute("destroyItem");
			player.removeAttribute("destroyItemSlot");
			player.removeAttribute("buyItem");
			break;
		case 905:
			ActionSender.sendCloseChatBox(player);
			if (player.getSettings().getAmountToProduce() < 1) {
				break;
			}
			switch (player.getSettings().getDialoguesSkill()) {
			case Skills.CRAFTING:
				switch (player.<Integer>getAttribute("craftingType")) {
				case 1: // gem cutting
					int productionItem = player.getSettings().getItemToProduce();
					if (productionItem > -1) {
						player.registerAction(new GemCuttingAction(player, productionItem, player.getSettings().getAmountToProduce()));
					}
					break;
				case 2: // leather crafting
					LeatherProduction toProduce = LeatherProduction.values()[player.getSettings().getPossibleProductions()[buttonId - 14]];
					if (toProduce != null) {
						LeatherProductionAction produceAction = new LeatherProductionAction(player, toProduce,  player.getSettings().getAmountToProduce());
						produceAction.execute();
						if (produceAction.isRunning()) {
							player.submitTick("skill_action_tick", produceAction, true);
						}
					}
					break;
				}
				break;
			case Skills.SMITHING:
				SmeltingBar bar = SmeltingBar.values()[player.getSettings().getPossibleProductions()[buttonId - 14]];
				if (bar != null) {
					player.registerAction(new Smelting(player.getSettings().getAmountToProduce(), bar));
				}
				break;
			case Skills.COOKING:
				int objId = player.getAttribute("cookingObj", -1);
				player.registerAction(new Cooking(3, player.getSettings().getAmountToProduce(), Cooking.itemForId(player, player.getSettings().getItemToProduce(), objId), CookingMethod.STOVE));
				break;
			case Skills.FLETCHING:
				if (player.getAttribute("isCutting") == Boolean.TRUE) {
					player.registerAction(new Fletching(1, player.getSettings().getAmountToProduce(), Fletching.getItemForId(player.getSettings().getPossibleProductions()[-14 + buttonId], player.getSettings().getItemUsed(), true)));
					player.removeAttribute("isCutting");
				} else {
					player.registerAction(new Fletching(2, player.getSettings().getAmountToProduce(), Fletching.getItemForId(player.getSettings().getItemToProduce(), player.getSettings().getItemUsed(), false)));
				}
				break;
			case Skills.HERBLORE:
				Item firstItem = player.getAttribute("itemUsedSkill");
				Item secondItem = player.getAttribute("itemUsedSkill2");
				Herblore herblore = new Herblore(player, firstItem, secondItem, player.getSettings().getAmountToProduce());
				herblore.execute();
				player.submitTick("skill_action_tick", herblore, true);
				break;
			}
			break;
		case 740:
			ActionSender.sendCloseChatBox(player);
			break;
		case 131:
			if (player.getAttribute("chooseBoots", false)) {
				DialogueManager.sendDisplayBox(player, 111, "They will both protect your feet in exactly the same manner;",
						"however, they look very different. You can always come back and",
						"get another pair if you lose them, or even swamp them for the other",
				"style!");
				break;
			}
			if (player.getAttribute("closeInterface") == Boolean.TRUE) {
				ActionSender.sendCloseChatBox(player);
				player.setAttribute("closeInterface", false);
				break;
			}
			dialogue = player.getAttribute("dialogue");
			if (dialogue != null) {
				if (dialogue.getActions().get(0).handle(player)) {
					ActionSender.sendCloseChatBox(player);
					player.setAttribute("dialogue", null);
					return;
				}
				return;
			}
			break;
		case 959:
			switch (buttonId) {
			case 28:
				BookManager.processPreviousPage(player);
				break;
			case 29:
				BookManager.processNextPage(player);
				break;
			}
			break;
			//Don't break, for debugging purpose in case this interface will be used again.
		default:
			if (Constants.CONNECTING_TO_FORUMS) {
				//System.out.println("Interface id " + interfaceId + ", " + buttonId + " in dialouge not added yet.");
			}
		//System.out.println("Interface id " + interfaceId + ", " + buttonId + " in dialouge not added yet.");
		break;
		}
	}

}
