package org.dementhium.net.packethandlers;

import java.text.NumberFormat;

import org.dementhium.content.BookManager;
import org.dementhium.content.DialogueManager;
import org.dementhium.content.activity.impl.DuelActivity;
import org.dementhium.content.activity.impl.duel.DuelConfigurations.Rules;
import org.dementhium.content.clans.Clan;
import org.dementhium.content.clans.ClanChatUtils;
import org.dementhium.content.interfaces.Emotes;
import org.dementhium.content.skills.magic.TeleportHandler;
import org.dementhium.event.EventManager;
import org.dementhium.model.Item;
import org.dementhium.model.Mob;
import org.dementhium.model.Projectile;
import org.dementhium.model.SpecialAttack;
import org.dementhium.model.SpecialAttackContainer;
import org.dementhium.model.World;
import org.dementhium.model.combat.CombatType;
import org.dementhium.model.combat.impl.specs.QuickSmash;
import org.dementhium.model.definition.ItemDefinition;
import org.dementhium.model.mask.Animation;
import org.dementhium.model.misc.ProjectileManager;
import org.dementhium.model.misc.DamageManager.DamageType;
import org.dementhium.model.player.Bank;
import org.dementhium.model.player.DegradingHandler;
import org.dementhium.model.player.Equipment;
import org.dementhium.model.player.EquipmentItemStats;
import org.dementhium.model.player.Inventory;
import org.dementhium.model.player.Player;
import org.dementhium.model.player.Shop;
import org.dementhium.model.player.Skills;
import org.dementhium.net.ActionSender;
import org.dementhium.net.PacketHandler;
import org.dementhium.net.message.Message;
import org.dementhium.tickable.Tick;
import org.dementhium.tickable.impl.WGuildTick;
import org.dementhium.util.Constants;
import org.dementhium.util.InputHandler;
import org.dementhium.util.InterfaceSettings;
import org.dementhium.util.Misc;
import org.dementhium.util.SQL;

/**
 * @author 'Mystic Flow
 * @author `Discardedx2
 * @auhtor Steve
 */
public class ActionButtonHandler extends PacketHandler {

	private static final int[] BUTTON_PACKET_IDS = { 6, 13, 0, 15, 46, 67, 82,
			39, 73 };
	private EventManager eventManager = EventManager.getEventManager();

	/*
	 * case 6: bob.removeItem(slot, 1); break; case 13: bob.removeItem(slot, 5);
	 * break; case 0: bob.removeItem(slot, 10); break; case 15:
	 * bob.removeItem(slot, bob.numberOf(bob.getContainer().get(slot).getId()));
	 * break; case 46: InputHandler.requestIntegerInput(player, 9,
	 * "Please enter an amount:"); player.setAttribute("slotId", slot); break;
	 * 
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.dementhium.net.PacketHandler#handlePacket(org.dementhium.model.player
	 * .Player, org.dementhium.net.message.Message)
	 */
	@Override
	public void handlePacket(Player player, Message packet) {
		try {
			handleButtons(player, packet,
					getMenuOptionIndex(packet.getOpcode()));
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	private int getMenuOptionIndex(int opcode) {
		for (int i = 0; i < BUTTON_PACKET_IDS.length; i++) {
			if (BUTTON_PACKET_IDS[i] == opcode) {
				return i;
			}
		}
		return -1;
	}

	private void handleButtons(final Player player, Message packet,
			int menuIndex) {
	int interfaceId = packet.readShort(); // :O 
		int buttonId = packet.readShort();
		int slot = packet.readLEShortA();
		int itemId = packet.readShort();
		if (slot == 65535) {
			slot = -1;
		}
		if (!player.hasReceivedStarter() && interfaceId != 182 && interfaceId != 1028) { //logout and char interfaces
			return;
		}
		System.out.println("interfaceId=" + interfaceId + " buttonId="
				+ buttonId + " slot=" + slot + " itemId=" + itemId);
		if (org.dementhium.content.minigames.gambler.GamblerInterfacePreview.button(player,interfaceId,buttonId,packet.getOpcode()))return;
        if (org.dementhium.content.minigames.gambler.GamblerSession.button(player,interfaceId,buttonId,packet.getOpcode()))return;
        if (eventManager.handleInterfaceOption(player, interfaceId, buttonId,
				slot, itemId, packet.getOpcode())) {
			return;
		}
		switch (interfaceId) {
        case 25:
            if (player.getActivity() instanceof org.dementhium.content.activity.impl.BarrowsActivity)
                ((org.dementhium.content.activity.impl.BarrowsActivity) player.getActivity()).answerPuzzle(buttonId);
            return;
		case 364:
			if (buttonId == 3) { //Treasure Trails casket interface close button
        		if (player.interfaceItems != null) {
        			player.getInventory().addAllDropable(player.interfaceItems);
        			player.getInventory().refresh();
        			player.interfaceItems = null;
        		}
			}
		case 672:
		case 206: 
			if (buttonId == 13) {
				player.getPriceCheck().close();
			}
			break;
		case 411:
			if (System.currentTimeMillis() - WGuildTick.getLastLaunch() < 3500)
				player.setAttribute("shieldStyle", buttonId);
			break;
		case 652:
			if (buttonId == 34) {
				if (slot < 0) {
					slot = 0;
				}
				player.setAttribute("graveSelection", slot);
				player.setAttribute("gravePrice", -1);
				if (slot == player.getSettings().getGraveStone()) {
					player.sendMessage("You're already using this gravestone.");
					return;
				}
				if (slot == 1) {
					player.setAttribute("gravePrice", 50);
				} else if (slot == 2) {
					player.setAttribute("gravePrice", 500);
				} else if (slot == 3) {
					player.setAttribute("gravePrice", 5000);
				} else if (slot > 3 && slot < 12) {
					player.setAttribute("gravePrice", 50000);
				} else if (slot > 11) {
					player.setAttribute("gravePrice", 500000);
				}
				ActionSender.sendCloseInterface(player);
				int price = player.getAttribute("gravePrice", -1);
				if (price > 0) {
					DialogueManager.sendDialogue(player,
							DialogueManager.CALM_TALK, 456, 400,
							"That's a fine selection,",
							"the gods will be pleased with your choice.",
							"Though they're still handcrafted so it'll cost",
							"a small fee of " + price + " coins.");
				} else {
					player.getSettings().setGraveStone(slot);
					DialogueManager.sendDialogue(player,
							DialogueManager.CALM_TALK, 456, -1,
							"That's a fine selection,",
							"the gods will be pleased with your choice.");
				}
			}
			break;
		case 749:
			switch (packet.getOpcode()) {
			case 6: 
				player.getPrayer().switchQuickPrayers();
				break;
			case 13: 
				player.getPrayer().switchSettingQuickPrayer();
				break;
			}
			break;
		case 916:
			switch (buttonId) {
			case 19:
				player.getSettings().increaseAmountToProduce();
				break;
			case 20:
				player.getSettings().decreaseAmountToProduce();
				break;
			case 5:
				player.getSettings().setAmountToProduce(1);
				break;
			case 6:
				player.getSettings().setAmountToProduce(5);
				break;
			case 7:
				player.getSettings().setAmountToProduce(10);
				break;

			}
			break;
		// case 1028:
		// CharacterDesign.handleButton(player, buttonId, slot, itemId);
		case 464:
			Emotes.handleButton(player, buttonId, slot, itemId);
			break;
		case 763:
			if (buttonId == 0) {
				switch (packet.getOpcode()) {
				case 6:
					player.getBank().addItem(slot, 1);
					break;
				case 13:
					player.getBank().addItem(slot, 5);
					break;
				case 0:
					player.getBank().addItem(slot, 10);
					break;
				case 15:
					player.getBank().addItem(slot,
							player.getSettings().getLastXAmount());
					break;
				case 67:
					Item item = player.getInventory().getContainer().get(slot);
					player.getBank().addItem(
							slot,
							player.getInventory().getContainer()
									.getNumberOf(item));// getContainer(slot).getAmount());
					break;
				case 46:
					InputHandler.requestIntegerInput(player, 2,
							"Please enter an amount:");
					player.setAttribute("inputId", 4);
					player.setAttribute("slotId", slot);
					break;
				case 58:
					//player.sendMessage(player.getInventory().getContainer()
							//.get(slot).getDefinition().getExamine());
					Item item2 = player.getInventory().getContainer().get(slot);
					if (item2 != null) {
						player.sendMessage(item2.getDefinition().getExamine());
					}
					break;
				}
			}
			break;
		case 762:
			if (buttonId >= 46 && buttonId <= 62) { //Fixed bank glitch
				player.setLastBankTab(Bank.getArrayIndex(buttonId));
				ActionSender.sendString(player, 762, 45, "Bank of "+Constants.SERVER_NAME);
			}
			switch (buttonId) {
			case 117:
				if (player.getAttribute("inBank", Boolean.FALSE) == Boolean.TRUE) {
					player.getBonuses().openEquipmentScreen(true);
				}
				break;
			case 33:
				player.getBank().bankInv();
				break;
			case 35:
				player.getBank().bankEquip();
				break;
			case 37:
				player.getBank().bankBob();
				break;
			case 19:
				player.setAttribute("noting", player.getAttribute("noting",
						Boolean.FALSE) == Boolean.FALSE ? Boolean.TRUE
						: Boolean.FALSE);
				break;
			case 15:
				player.setAttribute(
						"inserting",
						player.getAttribute("inserting", Boolean.FALSE) == Boolean.FALSE ? Boolean.TRUE
								: Boolean.FALSE);
				break;
			case 18:
				if (player.getAttribute("inBank", Boolean.FALSE) == Boolean.TRUE) {
					player.removeAttribute("inBank");
				}
				break;
			case 62:
			case 60:
			case 58:
			case 56:
			case 54:
			case 52:
			case 50:
			case 48:
			case 46:
				switch (packet.getOpcode()) {
				case 6:
					player.setLastBankTab(Bank.getArrayIndex(buttonId));
					player.setAttribute("currentTabConfig",
							Bank.getViewedTabConfig(buttonId));
					break;
				case 13:
					player.getBank().collapseTab(Bank.getArrayIndex(buttonId));
					player.getBank().refresh();
					break;
				}
				break;

			case 93:
				switch (packet.getOpcode()) {
				case 6:
					player.getBank().removeItem(slot, 1);
					break;
				case 13:
					player.getBank().removeItem(slot, 5);
					break;
				case 0:
					player.getBank().removeItem(slot, 10);
					break;
				case 15:
					player.getBank().removeItem(slot,
							player.getSettings().getLastXAmount());
					break;
				case 67:
					Item item = player.getBank().getContainer().get(slot);
					player.getBank().removeItem(slot,
							player.getBank().getContainer().getNumberOf(item));
					break;
				case 46:
					InputHandler.requestIntegerInput(player, 2,
							"Please enter an amount:");
					player.setAttribute("inputId", 3);
					player.setAttribute("slotId", slot);
					break;
				case 82:
					Item item2 = player.getBank().getContainer().get(slot);
					int itemAmt = player.getBank().getContainer()
							.getNumberOf(item2);
					player.getBank().removeItem(slot, itemAmt - 1);
					break;
				case 58:
					//player.sendMessage(player.getBank().getContainer()
							//.get(slot).getDefinition().getExamine());
					Item item3 = player.getBank().getContainer().get(slot);
					if (item3 != null) {
						player.sendMessage(item3.getDefinition().getExamine());
					}
					break;
				}
				break;

			default:
				System.out.println(buttonId);
			}
			break;

		/*case 665:
			if (player.getFamiliar() != null && player.getFamiliar() instanceof BeastOfBurden) {
			BeastOfBurden bob = (BeastOfBurden) player.getFamiliar();
			switch (packet.getOpcode()) {
			case 6:
				bob.putItem(slot, 1);
				break;
			case 13:
				bob.putItem(slot, 5);
				break;
			case 0:
				bob.putItem(slot, 10);
				break;
			case 15:
				bob.putItem(slot, player.getInventory().numberOf(player.getInventory().get(slot).getId()));
				break;
			case 46:
				InputHandler.requestIntegerInput(player, 8,"Please enter an amount:");
				player.setAttribute("slotId", slot);
				break;
			}
			}
			break;*/
		case 670:
			 if (slot < 0 || slot >= Inventory.SIZE || itemId < 0 || itemId >= ItemDefinition.MAX_SIZE) return;
			 int equipSlot3 = Equipment.getItemType(itemId); 
			 //Item item3 =
			
			 //the 4 lines below fix the equipment dupe =)
			 Item item4 = player.getInventory().get(slot); 
			 if (item4 == null || (item4 != null && itemId != item4.getId())) {
				 return;
			 } //end of equipment dupe fix
			 if (packet.getOpcode() == 73 && buttonId == 0) {
				 EquipmentItemStats.show(player, item4);
				 break;
			 }
			 
			 if (packet.getOpcode() == 6) { 
				 switch (buttonId) {
				 case 0:
					 if (player.getActivity().getActivityId() == 0 
							 && (equipSlot3 == Equipment.SLOT_CAPE || equipSlot3 == Equipment.SLOT_HAT)) {
						 		player.sendMessage("You can't equip a cape or hat in this activity.");
						 		return; }
					 if (itemId == 8856) {
						 if (!World.getWorld().getAreaManager().getAreaByName("WGuildCatapult").contains(player.getLocation())) { 
							 player.sendMessage("You may not equip this shield outside the catapult room in the Warriors' Guild."); 
							 return; 
							 }
						 if (player.getEquipment().get(Equipment.SLOT_WEAPON)!= null) { 
							 DialogueManager.sendInfoDialogue(player, "You will need to make sure your sword hand is free", "to equip this shield."); 
							 return; 
							 }
					 }
					 player.getEquipment().equip(player, buttonId, slot, itemId, true);
					 break; 
				 }
			}
			//player.sendMessage("This feature has been disabled until further notice.");
			break;
		case 667:
			int equipSlot2 = itemId >= 0 && itemId < ItemDefinition.MAX_SIZE ? Equipment.getItemType(itemId) : -1;
			Item item2 = equipSlot2 >= 0 && equipSlot2 < Equipment.SIZE ? player.getEquipment().get(equipSlot2) : null;
			boolean degradableItem2 = item2 != null && item2.getId() == itemId
					&& DegradingHandler.isDegradable(item2);
			if (packet.getOpcode() == 58) {
				if (item2 != null) {
					player.sendMessage(item2.getDefinition().getExamine());
				}
				return;
			} else if (packet.getOpcode() == 0
					|| packet.getOpcode() == 13 && degradableItem2) {
				if (item2 != null && item2.getId() == itemId) {
					DegradingHandler.checkCharges(player, item2);
				}
				return;
			} else if (packet.getOpcode() == 73) {
				switch (buttonId) {
				case 7:
					if (item2 == null || item2.getId() != itemId) return;
					EquipmentItemStats.show(player, item2);
					break;
				}
			} else if (packet.getOpcode() == 6) {
				switch (buttonId) {
				case 64:
					EquipmentItemStats.close(player);
					break;
				case 7:
					if (item2 == null) {
						return;
					}
					if (item2.getId() != itemId) {
						return;
					}
					if (itemId >= 5527 && itemId <= 5545 || itemId == 5547
							|| itemId == 5549 || itemId == 5551
							|| itemId == 9106) {
						ActionSender.sendConfig(player, 491, 0);
					}
					if (player.getActivity().getActivityId() == 0
							&& (equipSlot2 == Equipment.SLOT_CAPE || equipSlot2 == Equipment.SLOT_HAT)) {
						player.sendMessage("You can't remove your cape or hat in this activity.");
						return;
					}
					if (player.getEquipment().checkUnequip(equipSlot2)) {
						return;
					}
					player.getEquipment().unEquip(player, item2.getId(), 667, equipSlot2, false);
					/*if (player.getInventory().hasRoomFor(item2.getId(),
							item2.getAmount())) {
						player.getEquipment().set(equipSlot2, null);
						player.getInventory().getContainer().add(item2);
						if (player.getEquipment().hpModifier(definition)) {
							player.getSkills().lowerTotalHp(
									player.getEquipment().getModifier(
											definition));
						}
						player.getInventory().refresh();
					} else {
						ActionSender.sendMessage(player,
								"Not enough space in your inventory.");
					}*/
					break;
				case 48:
					if (player.getAttribute("fromBank", Boolean.FALSE) == Boolean.TRUE) {
						player.getBank().openBank();
					}
					break;
				case 74:
					if (player.getAttribute("fromBank", Boolean.FALSE) == Boolean.TRUE) {
						// The client closes the main equipment panel itself, but the
						// companion inventory panel and return marker also need cleanup.
						player.closeAll(false, true);
					}
					break;
				}
			}
			break;
		case 671:
			 /*if (player.getFamiliar() != null && player.getFamiliar() instanceof BeastOfBurden) {
				 BeastOfBurden bob = (BeastOfBurden) player.getFamiliar();
				 if (buttonId == 29) {
					 bob.take();
					 return;
				 }
				 switch (packet.getOpcode()) {
				 case 6:
					 bob.removeItem(slot, 1);
					 break;
				 case 13:
					 bob.removeItem(slot, 5);
					 break;
				 case 0:
					 bob.removeItem(slot, 10);
					 break;
				 case 15:
					 bob.removeItem(slot, bob.numberOf(bob.getContainer().get(slot).getId()));
					 break;
				 case 46:
					 InputHandler.requestIntegerInput(player, 9, "Please enter an amount:"); player.setAttribute("slotId", slot);
					 break;
				 }
			 }
			 break;*/
		case 620:
		case 621:
			if (World.getWorld().getShopManager().getShop((Integer) player.getAttribute("shopId",-1)) == null) {
				return;
			}
			World.getWorld()
			.getShopManager()
			.getShop((Integer) player.getAttribute("shopId"))
			.handleOption(player, interfaceId, buttonId, slot,
					packet.getOpcode(), itemId);
			break;
		case 449:
			if (buttonId == 1) {
				Shop.sendInventory(player);
				player.removeAttribute("itemInfoSlot");
			} else if (buttonId == 21) {
				World.getWorld().getShopManager().getShop((Integer) player.getAttribute("shopId")).handleOption(player, 449, 21,(Integer) player.getAttribute("itemInfoSlot"), packet.getOpcode(), 0);
				break;
			}
			break;
		case 589: // Clan chat tab
			if (buttonId == 16) {
				ActionSender.sendInterface(player, 590);
				if (World
						.getWorld()
						.getClanManager()
						.getClan(
								Misc.formatPlayerNameForProtocol(player
										.getDisplayName())) != null) {
					ActionSender
							.sendConfig(
									player,
									1083,
									(World.getWorld()
											.getClanManager()
											.getClan(
													Misc.formatPlayerNameForProtocol(player
															.getDisplayName()))
											.isCoinSharing() ? 1 : 0) << 18
											| (World.getWorld()
													.getClanManager()
													.getClan(
															Misc.formatPlayerNameForProtocol(player.getDisplayName())).isLootsharing() ? 1 : 0));

					ActionSender.sendString(player, World.getWorld().getClanManager().getClanName(player.getDisplayName()),590, 22);    
					World.getWorld().getClanManager().handleOption(player, 27 - buttonId, buttonId == 25 ? menuIndex + 3 : menuIndex);
					ActionSender.sendString(player,590, 23, ClanChatUtils.RANK_INDEX[World.getWorld().getClanManager().getClan(Misc.formatPlayerNameForProtocol(player.getDisplayName())).getJoinReq()]);
					ActionSender.sendString(player, 590, 24, ClanChatUtils.RANK_INDEX[World.getWorld().getClanManager().getClan(Misc.formatPlayerNameForProtocol(player.getDisplayName())).getTalkReq()]);
					try {
					ActionSender.sendString(player, 590, 25, ClanChatUtils.RANK_INDEX[World.getWorld().getClanManager().getClan(Misc.formatPlayerNameForProtocol(player.getDisplayName())).getKickReq() + 3]);
					ActionSender.sendString(player, 590, 26, ClanChatUtils.RANK_INDEX[World.getWorld().getClanManager().getClan(Misc.formatPlayerNameForProtocol(player.getDisplayName())).getLootReq()]);
					} catch (Exception e){
						
					}
				}
			}
			if (buttonId == 0) {
				World.getWorld().getClanManager().toggleLootshare(player);
			//}
			//if (buttonId == 1) {
				// World.getWorld().getClanManager().banMember(player, InputHandler.)
			}
			if (buttonId == 15) {
				World.getWorld().getClanManager().leaveClan(player, false);
				player.getSettings().setCurrentClan(null);
			}
			break;
		case 590: // Clan chat interface
			if (buttonId > 22 && buttonId < 27) {
				World.getWorld()
						.getClanManager()
						.handleOption(player, 27 - buttonId,
								buttonId == 25 ? menuIndex + 3 : menuIndex);
				String rank = ClanChatUtils.RANK_INDEX[buttonId == 25 ? menuIndex + 3
						: menuIndex];
				if (buttonId == 26) {
					if (menuIndex == 0) {
						rank = "No-one";
					}
				}
				ActionSender.sendString(player, 590, buttonId, rank);
			}

			if (buttonId == 22) {
				switch (packet.getOpcode()) {
				case 6: // prefix
					InputHandler.requestStringInput(player, 0,
							"Enter clan prefix:");
					break;
				case 13: // disable
					break;
				}
			}
			if (buttonId == 33) {
				World.getWorld().getClanManager().toggleCoinshare(player);
				
				Clan clan = World.getWorld().getClanManager().getClan(Misc.formatPlayerNameForProtocol(player.getDisplayName()));
				
			if(clan!= null){		
						
				ActionSender.sendConfig(player,1083,(clan.isCoinSharing() ? 1 : 0) << 18| (clan.isLootsharing() ? 1 : 0));
			}
			}
			break;

		case 335: // trade
			switch (buttonId) {
			/**
			 * Close button.
			 */
			case 18:
			case 12:
				if (player.getTradeSession() != null) {
					player.getTradeSession().tradeFailed(player);
				}
				break;
			case 16:
				if (player.getTradeSession() != null) {
					player.getTradeSession().acceptPressed(player);
				}
				break;

			case 31:
				if (player.getTradeSession() != null) {
					switch (packet.getOpcode()) {// 6. 13. 15. 67. 58
					case 0:
						player.getTradeSession().removeItem(player, slot, 10);
						break;
					case 6:
						player.getTradeSession().removeItem(player, slot, 1);
						break;
					case 13:
						player.getTradeSession().removeItem(player, slot, 5);
						break;
					case 15:
						player.getTradeSession().removeItem(
								player,
								slot,
								player.getTradeSession()
										.getPlayerItemsOffered(player)
										.getNumberOf(
												player.getTradeSession()
														.getPlayerItemsOffered(
																player)
														.get(slot)));
						break;
					case 67:
						/*player.sendMessage(player.getTradeSession()
								.getPlayerItemsOffered(player).get(slot)
								.getDefinition().getName()
								+ " is worth "
								+ player.getTradeSession()
										.getPlayerItemsOffered(player)
										.get(slot).getDefinition()
										.getExchangePrice()+".");*/
						Item item = player.getTradeSession().getPlayerItemsOffered(player).get(slot);
						NumberFormat nf1 = NumberFormat.getInstance();
						if (item != null) {
							int value = item.getDefinition().getExchangePrice();
							player.sendMessage(ItemDefinition.forId(item.getId()).getName()+" is worth "+nf1.format(value)+" x Coins.");
						}
						break;
					case 46:
						InputHandler.requestIntegerInput(player, 2,
								"Please enter an amount:");
						player.getTradeSession().resetAccept();
						player.setAttribute("inputId", 2);
						player.setAttribute("slotId", slot);
						break;
					case 58:
						/*ActionSender.sendMessage(player, player
								.getTradeSession()
								.getPlayerItemsOffered(player).get(slot)
								.getDefinition().getName()
								+ " is valued at "
								+ player.getTradeSession()
										.getPlayerItemsOffered(player)
										.get(slot).getDefinition()
										.getExchangePrice());*/
						Item item3 = player.getTradeSession().getPlayerItemsOffered(player).get(slot);
						if (item3 != null) {
							player.sendMessage(item3.getDefinition().getExamine());
						}
						break;
					}
				}
				break;
			case 34:
				if (player.getTradeSession() != null) {
					switch (packet.getOpcode()) {
					case 6: //value
						Player partner = player.getTradeSession().getPartner(player);
						if (partner != null) {
							Item item = player.getTradeSession().getPlayerItemsOffered(partner).get(slot);
							NumberFormat nf1 = NumberFormat.getInstance();
							if (item != null) {
								int value = item.getDefinition().getExchangePrice();
								player.sendMessage(ItemDefinition.forId(item.getId()).getName()+" is worth "+nf1.format(value)+" x Coins.");
							}
						}
						break;
					case 58: //examine
						Player partner2 = player.getTradeSession().getPartner(player);
						if (partner2 != null) {
							Item item3 = player.getTradeSession().getPlayerItemsOffered(partner2).get(slot);
							if (item3 != null) {
								player.sendMessage(item3.getDefinition().getExamine());
							}
						}
					}
				}
				break;
			}
			break;
		case 334:
			switch (buttonId) {
			case 21:
				if (player.getTradeSession() != null) {
					player.getTradeSession().acceptPressed(player);
				}
				break;
			case 22:
			case 6:
				if (player.getTradeSession() != null) {
					player.getTradeSession().tradeFailed(player);
				}
				break;
			}
			break;
		case 336:
			if (player.getTradeSession() != null) {
				switch (packet.getOpcode()) {
				case 6:// 6. 13. 15. 67. 58
					player.getTradeSession().offerItem(player, slot, 1);
					break;
				case 13:
					player.getTradeSession().offerItem(player, slot, 5);
					break;
				case 0:
					player.getTradeSession().offerItem(player, slot, 10);
					break;
				case 15:
					player.getTradeSession().offerItem(
							player,
							slot,
							player.getInventory().numberOf(
									player.getInventory().get(slot).getId()));
					break;
				case 46:
					InputHandler.requestIntegerInput(player, 2,
							"Please enter an amount:");
					player.setAttribute("inputId", 1);
					player.setAttribute("slotId", slot);
					break;
				case 58:
					/*ActionSender.sendMessage(
							player,
							player.getTradeSession()
									.getPlayerItemsOffered(player).get(slot)
									.getDefinition().getName()
									+ " is valued at "
									+ player.getTradeSession()
											.getPlayerItemsOffered(player)
											.get(slot).getDefinition()
											.getExchangePrice());*/
					Item item = player.getInventory().get(slot);
					if (item != null) {
						player.sendMessage(item.getDefinition().getExamine());
					}
					break;
				}
			}
			break;
		case 387:
			System.out.println("OPCODE: " + packet.getOpcode() + " BUTTON "
					+ buttonId);
			int equipSlot = itemId >= 0 && itemId < ItemDefinition.MAX_SIZE ? Equipment.getItemType(itemId) : -1;
			Item item = equipSlot >= 0 && equipSlot < Equipment.SIZE ? player.getEquipment().get(equipSlot) : null;
			boolean degradableItem = item != null && item.getId() == itemId
					&& DegradingHandler.isDegradable(item);
			if (packet.getOpcode() == 58) {
				if (item != null) {
					player.sendMessage(item.getDefinition().getExamine());
				}
				return;
			} else if (packet.getOpcode() == 0
					|| packet.getOpcode() == 13 && degradableItem) {
				if (item != null && item.getId() == itemId) {
					DegradingHandler.checkCharges(player, item);
				}
				return;
			} else if (packet.getOpcode() == 6) {
				switch (buttonId) {
				case 17:
				case 20:
				case 8:
				case 11:
				case 14:
				case 26:
				case 32:
				case 29:
				case 35:
				case 23:
				case 38:
					if (item == null) {
						return;
					}
					if (item.getId() != itemId) {
						return;
					}
					if (itemId >= 5527 && itemId <= 5545 || itemId == 5547
							|| itemId == 5549 || itemId == 5551
							|| itemId == 9106) {
						ActionSender.sendConfig(player, 491, 0);
					}
					if (player.getActivity().getActivityId() == 0
							&& (equipSlot == Equipment.SLOT_CAPE || equipSlot == Equipment.SLOT_HAT)) {
						player.sendMessage("You can't remove your cape or hat in this activity.");
						return;
					}
					if (player.getEquipment().checkUnequip(equipSlot)) {
						return;
					}
					player.getEquipment().unEquip(player, item.getId(), 387, equipSlot, false);
					/*if (player.getInventory().hasRoomFor(item.getId(),
							item.getAmount())) {
						player.getEquipment().set(equipSlot, null);
						player.getInventory().getContainer().add(item);
						if (player.getEquipment().hpModifier(definition)) {
							player.getSkills().lowerTotalHp(
									player.getEquipment().getModifier(
											definition));
						}
						player.getInventory().refresh();
					} else {
						ActionSender.sendMessage(player,
								"Not enough space in your inventory.");
					}*/
					return;
				case 39:
					player.getBonuses().openEquipmentScreen(false);
					break;
				case 42:
					player.getPriceCheck().open();
					break;
				default:
					ActionSender.sendChatMessage(player, 0,
							"Equip button slot " + buttonId + " not handled.");
					break;
				}
			} else if (packet.getOpcode() == 13) {
				/*
				 * Operate options. TODO: Do this better, this is just for
				 * testing DFS spec.
				 */
				if (item != null) {
					switch (item.getId()) {
					case 11283:
					case 11284:
						/*
						 * If you want to do it correct you have to gain charges from dragon fire.
						 * You can have 50 charges at max.
						 * The right click option in the inventory called 'Inspect' shows you how many charges you have atm.
						 * using 'Empty' you lose all charges (so that you can trade the dfs).
						 * But I think it's fine like this =P.
						 */
						if (player.getAttribute("dischargeDelay", 0) > World
								.getTicks()) {
							player.sendMessage("Your dragonfire shield is recharging.");
							return;
						}
						if (player.getCombatExecutor().getVictim() == null
								&& player.getCombatExecutor().getLastAttacker() == null) {
							player.sendMessage("You can only operate the dragonfire shield whilst in combat.");
							return;
						}
						if (player.getCombatExecutor().getVictim() == player && !player.getCombatExecutor().getVictim().inWilderness()) {
							player.sendMessage("You can only use this special on players in a PVP-zone!");
							return;
						}
						if (player.getCombatExecutor().getVictim() != null && player.getCombatExecutor().getLastAttacker() == null) {
							player.sendMessage("That player is already in combat!");
							return;
							
						}
						final Mob dfsTarget = player.getCombatExecutor().getVictim() == null ?
						player.getCombatExecutor().getLastAttacker() : player.getCombatExecutor().getVictim();
						player.setAttribute("dischargeDelay", World.getTicks() + 200);
                        player.animate(6696);
                        player.graphics(1165);
                        player.turnTo(dfsTarget, false);
            			World.getWorld().submit(new Tick(3) {
                            @Override
                            public void execute() {
                            	stop();
                            	int speed = (int) (27 + (player.getLocation().distance(dfsTarget.getLocation()) * 5));
                            	ProjectileManager.sendProjectile(Projectile.create(player, dfsTarget, 1166, 40, 36, 20, speed, 15, 11));
                            }
                        });
            			World.getWorld().submit(new Tick(4) {
                            @Override
                            public void execute() {
                            	stop();
                            	dfsTarget.getDamageManager().miscDamage(Misc.random(290), DamageType.MAGE);
                            	//add def xp, constitution and magic..
                            }
                        });
						return;
					case 12645: //chocatrice cape
						//player.animate(???);
						player.graphics(1566);
						return;
					}
				}

				switch (buttonId) {
				case 17:
				case 20:
				case 8:
				case 11:
				case 14:
				case 26:
				case 32:
				case 29:
				case 35:
				case 23:
				case 38:
					if (item == null) {
						return;
					}
					if (item.getId() != itemId) {
						return;
					}
					if (item.getId() == 1712) {
						player.sendMessage("You rub the amulet...");
						TeleportHandler.telePlayer(player, 3087, 3496, 0, 0, 0,
								false, 1712, false);
						player.getEquipment().set(Equipment.SLOT_AMULET,
								new Item(1710, 1));
						player.getEquipment().refresh();
						player.sendMessage("Your amulet has three charges left.");
						return;
					}
					if (item.getId() == 1710) {
						player.sendMessage("You rub the amulet...");
						TeleportHandler.telePlayer(player, 3087, 3496, 0, 0, 0,
								false, 1710, false);
						player.getEquipment().set(Equipment.SLOT_AMULET,
								new Item(1708, 1));
						player.getEquipment().refresh();
						player.sendMessage("Your amulet has two charges left.");
						return;
					}
					if (item.getId() == 1708) {
						player.sendMessage("You rub the amulet...");
						TeleportHandler.telePlayer(player, 3087, 3496, 0, 0, 0,
								false, 1708, false);
						player.getEquipment().set(Equipment.SLOT_AMULET,
								new Item(1706, 1));
						player.getEquipment().refresh();
						player.sendMessage("Your amulet has one charge left.");
						return;
					}
					if (item.getId() == 1706) {
						player.sendMessage("You rub the amulet...");
						TeleportHandler.telePlayer(player, 3087, 3496, 0, 0, 0,
								false, 1706, false);
						player.getEquipment().set(Equipment.SLOT_AMULET,
								new Item(1704, 1));
						player.getEquipment().refresh();
						player.sendMessage("You use the amulet's last charge.");
						return;
					}
					if (item.getId() == 1704) {
						player.sendMessage("You rub the amulet...");
						player.sendMessage("The amulet has lost its charge.");
						player.sendMessage("It will need to be recharged before you can use it again.");
						return;
					}
					// Games Necklace
					if (item.getId() == 3853) {
						player.sendMessage("You rub the necklace...");
						TeleportHandler.telePlayer(player, 2876, 3557, 0, 0, 0,
								false, 3853, false);
						player.getEquipment().set(Equipment.SLOT_AMULET,
								new Item(3855, 1));
						player.getEquipment().refresh();
						player.sendMessage("Your amulet has seven charges left.");
						return;
					}
					if (item.getId() == 3855) {
						player.sendMessage("You rub the necklace...");
						TeleportHandler.telePlayer(player, 2876, 3557, 0, 0, 0,
								false, 3855, false);
						player.getEquipment().set(Equipment.SLOT_AMULET,
								new Item(3857, 1));
						player.getEquipment().refresh();
						player.sendMessage("Your amulet has six charges left.");
						return;
					}
					if (item.getId() == 3857) {
						player.sendMessage("You rub the necklace...");
						TeleportHandler.telePlayer(player, 2876, 3557, 0, 0, 0,
								false, 3857, false);
						player.getEquipment().set(Equipment.SLOT_AMULET,
								new Item(3859, 1));
						player.getEquipment().refresh();
						player.sendMessage("Your amulet has five charges left.");
						return;
					}
					if (item.getId() == 3859) {
						player.sendMessage("You rub the necklace...");
						TeleportHandler.telePlayer(player, 2876, 3557, 0, 0, 0,
								false, 3859, false);
						player.getEquipment().set(Equipment.SLOT_AMULET,
								new Item(3861, 1));
						player.getEquipment().refresh();
						player.sendMessage("Your amulet has four charges left.");
						return;
					}
					if (item.getId() == 3861) {
						player.sendMessage("You rub the necklace...");
						TeleportHandler.telePlayer(player, 2876, 3557, 0, 0, 0,
								false, 3861, false);
						player.getEquipment().set(Equipment.SLOT_AMULET,
								new Item(3863, 1));
						player.getEquipment().refresh();
						player.sendMessage("Your amulet has three charges left.");
						return;
					}
					if (item.getId() == 3863) {
						player.sendMessage("You rub the necklace...");
						TeleportHandler.telePlayer(player, 2876, 3557, 0, 0, 0,
								false, 3863, false);
						player.getEquipment().set(Equipment.SLOT_AMULET,
								new Item(3865, 1));
						player.getEquipment().refresh();
						player.sendMessage("Your amulet has two charges left.");
						return;
					}
					if (item.getId() == 3865) {
						player.sendMessage("You rub the necklace...");
						TeleportHandler.telePlayer(player, 2876, 3557, 0, 0, 0,
								false, 3865, false);
						player.getEquipment().set(Equipment.SLOT_AMULET,
								new Item(3867, 1));
						player.getEquipment().refresh();
						player.sendMessage("Your amulet has one charge left.");
						return;
					}
					if (item.getId() == 3867) {
						TeleportHandler.telePlayer(player, 2876, 3557, 0, 0, 0,
								false, 3867, false);
						player.getEquipment().refresh();
						player.sendMessage("Your games necklace crumbles to dust.");
						return;
					}
					// Ring Of Duelling
					if (item.getId() == 2552) {
						player.sendMessage("You rub the ring...");
						TeleportHandler.telePlayer(player, 3313, 3234, 0, 0, 0,
								false, 2552, false);
						player.getEquipment().set(Equipment.SLOT_RING,
								new Item(2554, 1));
						player.getEquipment().refresh();
						player.sendMessage("Your ring has seven charges left.");
						return;
					}
					if (item.getId() == 2554) {
						player.sendMessage("You rub the ring...");
						TeleportHandler.telePlayer(player, 3313, 3234, 0, 0, 0,
								false, 2554, false);
						player.getEquipment().set(Equipment.SLOT_RING,
								new Item(2556, 1));
						player.getEquipment().refresh();
						player.sendMessage("Your ring has six charges left.");
						return;
					}
					if (item.getId() == 2556) {
						player.sendMessage("You rub the ring...");
						TeleportHandler.telePlayer(player, 3313, 3234, 0, 0, 0,
								false, 2556, false);
						player.getEquipment().set(Equipment.SLOT_RING,
								new Item(2558, 1));
						player.getEquipment().refresh();
						player.sendMessage("Your ring has five charges left.");
						return;
					}
					if (item.getId() == 2558) {
						player.sendMessage("You rub the ring...");
						TeleportHandler.telePlayer(player, 3313, 3234, 0, 0, 0,
								false, 2558, false);
						player.getEquipment().set(Equipment.SLOT_RING,
								new Item(2560, 1));
						player.getEquipment().refresh();
						player.sendMessage("Your ring has four charges left.");
						return;
					}
					if (item.getId() == 2560) {
						player.sendMessage("You rub the ring...");
						TeleportHandler.telePlayer(player, 3313, 3234, 0, 0, 0,
								false, 2560, false);
						player.getEquipment().set(Equipment.SLOT_RING,
								new Item(2562, 1));
						player.getEquipment().refresh();
						player.sendMessage("Your ring has three charges left.");
						return;
					}
					if (item.getId() == 2562) {
						player.sendMessage("You rub the ring...");
						TeleportHandler.telePlayer(player, 3313, 3234, 0, 0, 0,
								false, 2562, false);
						player.getEquipment().set(Equipment.SLOT_RING,
								new Item(2564, 1));
						player.getEquipment().refresh();
						player.sendMessage("Your ring has two charges left.");
						return;
					}
					if (item.getId() == 2564) {
						player.sendMessage("You rub the ring...");
						TeleportHandler.telePlayer(player, 3313, 3234, 0, 0, 0,
								false, 2564, false);
						player.getEquipment().set(Equipment.SLOT_RING,
								new Item(2566, 1));
						player.getEquipment().refresh();
						player.sendMessage("Your ring has one charge left.");
						return;
					}
					if (item.getId() == 2566) {
						player.sendMessage("You rub the ring...");
						TeleportHandler.telePlayer(player, 3313, 3234, 0, 0, 0,
								false, 2566, false);
						player.getEquipment().refresh();
						player.sendMessage("Your ring of duelling crumbles to dust.");
						return;
					}
					if (item.getId() == 15398) {
						player.sendMessage("You rub the ring...");
						TeleportHandler.telePlayer(player, 1690, 5287, 1, 0, 0,
								false, 15398, false);
						player.getEquipment().set(Equipment.SLOT_RING,
								new Item(15399, 1));
						player.getEquipment().refresh();
						player.sendMessage("Your ring has four charges left.");
						return;
					}
					if (item.getId() == 15399) {
						player.sendMessage("You rub the ring...");
						TeleportHandler.telePlayer(player, 1690, 5287, 1, 0, 0,
								false, 15399, false);
						player.getEquipment().set(Equipment.SLOT_RING,
								new Item(15400, 1));
						player.getEquipment().refresh();
						player.sendMessage("Your ring has three charges left.");
						return;
					}
					if (item.getId() == 15400) {
						player.sendMessage("You rub the ring...");
						TeleportHandler.telePlayer(player, 1690, 5287, 1, 0, 0,
								false, 15400, false);
						player.getEquipment().set(Equipment.SLOT_RING,
								new Item(15401, 1));
						player.getEquipment().refresh();
						player.sendMessage("Your ring has two charges left.");
						return;
					}
					if (item.getId() == 15401) {
						player.sendMessage("You rub the ring...");
						TeleportHandler.telePlayer(player, 1690, 5287, 1, 0, 0,
								false, 15401, false);
						player.getEquipment().set(Equipment.SLOT_RING,
								new Item(15402, 1));
						player.getEquipment().refresh();
						player.sendMessage("Your ring has one charges left.");
						return;
					}
					if (item.getId() == 15402) {
						player.sendMessage("You rub the ring...");
						TeleportHandler.telePlayer(player, 1690, 5287, 1, 0, 0,
								false, 15402, false);
						player.getEquipment().refresh();
						player.sendMessage("Your ferocious ring crumbles to dust.");
						return;
					}
					if (player.getActivity().getActivityId() == 0
							&& (equipSlot == Equipment.SLOT_CAPE || equipSlot == Equipment.SLOT_HAT)) {
						player.sendMessage("You can't remove your cape or hat in this activity.");
						return;
					}
					if (player.getEquipment().checkUnequip(equipSlot)) {
						return;
					}
					player.getEquipment().unEquip(player, item.getId(), 387, equipSlot, false);
					/*if (player.getInventory().hasRoomFor(item.getId(),
							item.getAmount())) {
						player.getEquipment().set(equipSlot, null);
						player.getInventory().getContainer().add(item);
						if (player.getEquipment().hpModifier(definition)) {
							player.getSkills().lowerTotalHp(
									player.getEquipment().getModifier(
											definition));
						}
						player.getInventory().refresh();
					} else {
						ActionSender.sendMessage(player,
								"Not enough space in your inventory.");
					}*/
					return;
				case 39:
					player.getBonuses().openEquipmentScreen(false);
					break;
				case 42:
					player.getPriceCheck().open();
					break;
				default:
					ActionSender.sendChatMessage(player, 0,
							"Equip button slot " + buttonId + " not handled.");
					break;
				}
			} else if (packet.getOpcode() == 0) {// Third option
				if(item == null){
					return;//I think
				}
				if (item.getId() == 1712) {
					player.sendMessage("You rub the amulet...");
					TeleportHandler.telePlayer(player, 2918, 3176, 0, 0, 0,
							false, 1712, false);
					player.getEquipment().set(Equipment.SLOT_AMULET,
							new Item(1710, 1));
					player.getEquipment().refresh();
					player.sendMessage("Your amulet has three charges left.");
					return;
				}
				if (item.getId() == 1710) {
					player.sendMessage("You rub the amulet...");
					TeleportHandler.telePlayer(player, 2918, 3176, 0, 0, 0,
							false, 1710, false);
					player.getEquipment().set(Equipment.SLOT_AMULET,
							new Item(1708, 1));
					player.getEquipment().refresh();
					player.sendMessage("Your amulet has two charges left.");
					return;
				}
				if (item.getId() == 1708) {
					player.sendMessage("You rub the amulet...");
					TeleportHandler.telePlayer(player, 2918, 3176, 0, 0, 0,
							false, 1708, false);
					player.getEquipment().set(Equipment.SLOT_AMULET,
							new Item(1706, 1));
					player.getEquipment().refresh();
					player.sendMessage("Your amulet has one charge left.");
					return;
				}
				if (item.getId() == 1706) {
					player.sendMessage("You rub the amulet...");
					TeleportHandler.telePlayer(player, 2918, 3176, 0, 0, 0,
							false, 1706, false);
					player.getEquipment().set(Equipment.SLOT_AMULET,
							new Item(1704, 1));
					player.getEquipment().refresh();
					player.sendMessage("You use the amulet's last charge.");
					return;
				}
				if (item.getId() == 1704) {
					player.sendMessage("You rub the amulet...");
					player.sendMessage("The amulet has lost its charge.");
					player.sendMessage("It will need to be recharged before you can use it again.");
				}
				if (item.getId() == 3853) {
					player.sendMessage("You rub the necklace...");
					TeleportHandler.telePlayer(player, 2518, 3570, 0, 0, 0,
							false, 3853, false);
					player.getEquipment().set(Equipment.SLOT_AMULET,
							new Item(3855, 1));
					player.getEquipment().refresh();
					player.sendMessage("Your amulet has seven charges left.");
					return;
				}
				if (item.getId() == 3855) {
					player.sendMessage("You rub the necklace...");
					TeleportHandler.telePlayer(player, 2518, 3570, 0, 0, 0,
							false, 3855, false);
					player.getEquipment().set(Equipment.SLOT_AMULET,
							new Item(3857, 1));
					player.getEquipment().refresh();
					player.sendMessage("Your amulet has six charges left.");
					return;
				}
				if (item.getId() == 3857) {
					player.sendMessage("You rub the necklace...");
					TeleportHandler.telePlayer(player, 2518, 3570, 0, 0, 0,
							false, 3857, false);
					player.getEquipment().set(Equipment.SLOT_AMULET,
							new Item(3859, 1));
					player.getEquipment().refresh();
					player.sendMessage("Your amulet has five charges left.");
					return;
				}
				if (item.getId() == 3859) {
					player.sendMessage("You rub the necklace...");
					TeleportHandler.telePlayer(player, 2518, 3570, 0, 0, 0,
							false, 3859, false);
					player.getEquipment().set(Equipment.SLOT_AMULET,
							new Item(3861, 1));
					player.getEquipment().refresh();
					player.sendMessage("Your amulet has four charges left.");
					return;
				}
				if (item.getId() == 3861) {
					player.sendMessage("You rub the necklace...");
					TeleportHandler.telePlayer(player, 2518, 3570, 0, 0, 0,
							false, 3861, false);
					player.getEquipment().set(Equipment.SLOT_AMULET,
							new Item(3863, 1));
					player.getEquipment().refresh();
					player.sendMessage("Your amulet has three charges left.");
					return;
				}
				if (item.getId() == 3863) {
					player.sendMessage("You rub the necklace...");
					TeleportHandler.telePlayer(player, 2518, 3570, 0, 0, 0,
							false, 3863, false);
					player.getEquipment().set(Equipment.SLOT_AMULET,
							new Item(3865, 1));
					player.getEquipment().refresh();
					player.sendMessage("Your amulet has two charges left.");
					return;
				}
				if (item.getId() == 3865) {
					player.sendMessage("You rub the necklace...");
					TeleportHandler.telePlayer(player, 2518, 3570, 0, 0, 0,
							false, 3865, false);
					player.getEquipment().set(Equipment.SLOT_AMULET,
							new Item(3867, 1));
					player.getEquipment().refresh();
					player.sendMessage("Your amulet has one charges left.");
					return;
				}
				if (item.getId() == 3867) {
					TeleportHandler.telePlayer(player, 2518, 3570, 0, 0, 0,
							false, 3867, false);
					player.getEquipment().refresh();
					player.sendMessage("Your games necklace crumbles to dust.");
					return;
				}
				if (item.getId() == 2552) {
					player.sendMessage("You rub the ring...");
					TeleportHandler.telePlayer(player, 2441, 3089, 0, 0, 0,
							false, 2552, false);
					player.getEquipment().set(Equipment.SLOT_RING,
							new Item(2554, 1));
					player.getEquipment().refresh();
					player.sendMessage("Your ring has seven charges left.");
					return;
				}
				if (item.getId() == 2554) {
					player.sendMessage("You rub the ring...");
					TeleportHandler.telePlayer(player, 2441, 3089, 0, 0, 0,
							false, 2554, false);
					player.getEquipment().set(Equipment.SLOT_RING,
							new Item(2556, 1));
					player.getEquipment().refresh();
					player.sendMessage("Your ring has six charges left.");
					return;
				}
				if (item.getId() == 2556) {
					player.sendMessage("You rub the ring...");
					TeleportHandler.telePlayer(player, 2441, 3089, 0, 0, 0,
							false, 2556, false);
					player.getEquipment().set(Equipment.SLOT_RING,
							new Item(2558, 1));
					player.getEquipment().refresh();
					player.sendMessage("Your ring has five charges left.");
					return;
				}
				if (item.getId() == 2558) {
					player.sendMessage("You rub the ring...");
					TeleportHandler.telePlayer(player, 2441, 3089, 0, 0, 0,
							false, 2558, false);
					player.getEquipment().set(Equipment.SLOT_RING,
							new Item(2560, 1));
					player.getEquipment().refresh();
					player.sendMessage("Your ring has four charges left.");
					return;
				}
				if (item.getId() == 2560) {
					player.sendMessage("You rub the ring...");
					TeleportHandler.telePlayer(player, 2441, 3089, 0, 0, 0,
							false, 2560, false);
					player.getEquipment().set(Equipment.SLOT_RING,
							new Item(2562, 1));
					player.getEquipment().refresh();
					player.sendMessage("Your ring has three charges left.");
					return;
				}
				if (item.getId() == 2562) {
					player.sendMessage("You rub the ring...");
					TeleportHandler.telePlayer(player, 2441, 3089, 0, 0, 0,
							false, 2562, false);
					player.getEquipment().set(Equipment.SLOT_RING,
							new Item(2564, 1));
					player.getEquipment().refresh();
					player.sendMessage("Your ring has two charges left.");
					return;
				}
				if (item.getId() == 2564) {
					player.sendMessage("You rub the ring...");
					TeleportHandler.telePlayer(player, 2441, 3089, 0, 0, 0,
							false, 2564, false);
					player.getEquipment().set(Equipment.SLOT_RING,
							new Item(2566, 1));
					player.getEquipment().refresh();
					player.sendMessage("Your ring has one charge left.");
					return;
				}
				if (item.getId() == 2566) {
					player.sendMessage("You rub the ring...");
					TeleportHandler.telePlayer(player, 2441, 3089, 0, 0, 0,
							false, 2566, false);
					player.getEquipment().refresh();
					player.sendMessage("Your ring of duelling crumbles to dust.");
					return;
				}
			} else if (packet.getOpcode() == 46) {// Fifth option
				if (item.getId() == 1712) {
					player.sendMessage("You rub the amulet...");
					TeleportHandler.telePlayer(player, 3293, 3163, 0, 0, 0,
							false, 1712, false);
					player.getEquipment().set(Equipment.SLOT_AMULET,
							new Item(1710, 1));
					player.getEquipment().refresh();
					player.sendMessage("Your amulet has three charges left.");
					return;
				}
				if (item.getId() == 1710) {
					player.sendMessage("You rub the amulet...");
					TeleportHandler.telePlayer(player, 3293, 3163, 0, 0, 0,
							false, 1710, false);
					player.getEquipment().set(Equipment.SLOT_AMULET,
							new Item(1708, 1));
					player.getEquipment().refresh();
					player.sendMessage("Your amulet has two charges left.");
					return;
				}
				if (item.getId() == 1708) {
					player.sendMessage("You rub the amulet...");
					TeleportHandler.telePlayer(player, 3293, 3163, 0, 0, 0,
							false, 1708, false);
					player.getEquipment().set(Equipment.SLOT_AMULET,
							new Item(1706, 1));
					player.getEquipment().refresh();
					player.sendMessage("Your amulet has one charge left.");
					return;
				}
				if (item.getId() == 1706) {
					player.sendMessage("You rub the amulet...");
					TeleportHandler.telePlayer(player, 3293, 3163, 0, 0, 0,
							false, 1706, false);
					player.getEquipment().set(Equipment.SLOT_AMULET,
							new Item(1704, 1));
					player.getEquipment().refresh();
					player.sendMessage("You use the amulet's last charge.");
					return;
				}
			} else if (packet.getOpcode() == 15) {// Fourth option
				if (item.getId() == 1712) {
					player.sendMessage("You rub the amulet...");
					TeleportHandler.telePlayer(player, 3105, 3251, 0, 0, 0,
							false, 1712, false);
					player.getEquipment().set(Equipment.SLOT_AMULET,
							new Item(1710, 1));
					player.getEquipment().refresh();
					player.sendMessage("Your amulet has three charges left.");
					return;
				}
				if (item.getId() == 1710) {
					player.sendMessage("You rub the amulet...");
					TeleportHandler.telePlayer(player, 3105, 3251, 0, 0, 0,
							false, 1710, false);
					player.getEquipment().set(Equipment.SLOT_AMULET,
							new Item(1708, 1));
					player.getEquipment().refresh();
					player.sendMessage("Your amulet has two charges left.");
					return;
				}
				if (item.getId() == 1708) {
					player.sendMessage("You rub the amulet...");
					TeleportHandler.telePlayer(player, 3105, 3251, 0, 0, 0,
							false, 1708, false);
					player.getEquipment().set(Equipment.SLOT_AMULET,
							new Item(1706, 1));
					player.getEquipment().refresh();
					player.sendMessage("Your amulet has one charge left.");
					return;
				}
				if (item.getId() == 1706) {
					player.sendMessage("You rub the amulet...");
					TeleportHandler.telePlayer(player, 3105, 3251, 0, 0, 0,
							false, 1706, false);
					player.getEquipment().set(Equipment.SLOT_AMULET,
							new Item(1704, 1));
					player.getEquipment().refresh();
					player.sendMessage("You use the amulet's last charge.");
					return;
				}
				if (item.getId() == 1704) {
					player.sendMessage("You rub the amulet...");
					player.sendMessage("The amulet has lost its charge.");
					player.sendMessage("It will need to be recharged before you can use it again.");
				}

			}

			break;
		case 650: //corporeal beast warning
			switch (buttonId) {
			case 17:
				player.teleport(2974, 4384, 2, false);
				break;
			case 18:
				player.closeAll(true, true);
				break;
			}
			break;
		case 750:
			switch (buttonId) {
			case 1:
				if (packet.getOpcode() == 6) {
					player.getWalkingQueue().setRunToggled(
							!player.getWalkingQueue().isRunToggled());
				} else if (packet.getOpcode() == 13) {
					if (player.getSettings().isResting()) {
						player.sendMessage("You're already resting!");
						return;
					}
					player.getWalkingQueue().reset();
					player.getSettings().setResting(true);
					ActionSender.sendConfig(player, 1433, 1);
					ActionSender.sendConfig(player, 1189, 3833973);
					ActionSender.sendBConfig(player, 119, 3);
					//if (player.getRandom().nextBoolean()) {
					if (player.getLastRestingEmote()) {
						player.setLastRestingEmote(false);
						player.animate(5713);
					}
					else {
					//} else {
						player.setLastRestingEmote(true);
						player.animate(11786);
					//}
					}
					player.setAttribute("restAnimation", player.getMask()
							.getLastAnimation());
				}
				break;
			}
			break;
		case 884:
			switch (buttonId) {
			case 4:// special bar
				if (player.getActivity().toString().equals("DuelActivity")) {
					DuelActivity arena = (DuelActivity) player.getActivity();
					if (arena.getDuelConfigurations().getRule(
							Rules.SPECIAL_ATTACKS)) {
						player.sendMessage("You cannot use special attacks during this duel!");
						return;
					}
				}
				if (player.getSpecialAmount() < 1
						&& !player.getSettings().isUsingSpecial()) {
					player.sendMessage("You do not have enough power left.");
					return;
				}
				player.reverseSpecialActive();
				SpecialAttack spec = SpecialAttackContainer.get(player
						.getEquipment().getSlot(3));
				if (spec instanceof QuickSmash) {
                    if(!QuickSmash.activate(player)) player.sendMessage("You need a reachable opponent and enough special energy.");
                } else if (player.getEquipment().get(3).getDefinition()
						.getName().contains("Staff of light")) {
					if (player.getSpecialAmount() < 1000) {
						player.sendMessage("You do not have enough power left.");
						return;
					}
					
					player.setAttribute("staffOfLightEffect",
							World.getTicks() + 100);
					player.getCombatExecutor().setTicks(
							player.getCombatExecutor().getTicks() + 3);
					player.animate(12804);
					player.graphics(2319);
					player.setSpecialAmount(player.getSpecialAmount() - 1000);
					player.reverseSpecialActive();
				} else if (player.getEquipment().get(3).getDefinition()
						.getName().contains("Dragon battle")) {
					if (player.getSpecialAmount() < 1000) {
						player.sendMessage("You do not have enough power left.");
						return;
					}
					player.forceText("Raarrrrrgggggghhhhhhh!");
					player.animate(1056);
					player.graphics(246);
					player.setSpecialAmount(player.getSpecialAmount() - 1000);
					player.reverseSpecialActive();
					int attackLevel = (int) (0.1 * player.getSkills().getLevel(
							Skills.ATTACK));
					int defenceLevel = (int) (0.1 * player.getSkills()
							.getLevel(Skills.DEFENCE));
					int rangeLevel = (int) (0.1 * player.getSkills().getLevel(
							Skills.RANGED));
					int magicLevel = (int) (0.1 * player.getSkills().getLevel(
							Skills.MAGIC));
					int strengthLevel = (int) (10 + (0.25 * (magicLevel
							+ rangeLevel + defenceLevel + attackLevel)));
					player.getSkills().set(
							Skills.ATTACK,
							player.getSkills().getLevel(Skills.ATTACK)
									- attackLevel);
					player.getSkills().set(
							Skills.DEFENCE,
							player.getSkills().getLevel(Skills.DEFENCE)
									- defenceLevel);
					player.getSkills().set(
							Skills.RANGED,
							player.getSkills().getLevel(Skills.RANGED)
									- rangeLevel);
					player.getSkills().set(
							Skills.MAGIC,
							player.getSkills().getLevel(Skills.MAGIC)
									- magicLevel);
					player.getSkills().set(
							Skills.STRENGTH,
							player.getSkills().getLevelForExperience(
									Skills.STRENGTH)
									+ strengthLevel);
				} else if (player.getEquipment().get(3).getDefinition()
						.getName().contains("xcalibur")) {
					if (player.getSpecialAmount() < 1000) {
						player.sendMessage("You do not have enough power left.");
						return;
					}
					final boolean enhanced = player.getEquipment().get(3)
							.getDefinition().getName().contains("Enhanced");
					player.forceText("For "+Constants.SERVER_NAME+"!");
					player.animate(1168);
					player.graphics(247);
					player.setSpecialAmount(player.getSpecialAmount() - 1000);
					player.reverseSpecialActive();
					int defenceLevel = enhanced ? (int) (0.15 * player
							.getSkills().getLevelForExperience(Skills.DEFENCE))
							: 8;
					player.getSkills().set(
							Skills.DEFENCE,
							player.getSkills().getLevelForExperience(
									Skills.DEFENCE)
									+ defenceLevel);
					if (enhanced) {
						World.getWorld().submit(new Tick(3) {
							int count = 5;

							@Override
							public void execute() {
								player.getSkills().heal(40);
								if (--count == 0) {
									this.stop();
								}
							}
						});
					}
				}
				break;
			case 15:// auto retaliate
				player.reverseAutoRetaliate();
				break;
			case 11:
			case 12:
			case 13:
			case 14:
				player.getEquipment().toggleStyle(player, buttonId);
				break;
			}
			break;
		case 271:
			if (player.getActivity().toString().equals("DuelActivity")) {
				DuelActivity arena = (DuelActivity) player.getActivity();
				if (arena.getDuelConfigurations().getRule(Rules.PRAYER)) {
					player.sendMessage("You cannot use prayer during this duel!");
					return;
				}
			}
			switch (buttonId) {
			case 8:
			case 42:
				player.getPrayer().switchPrayer(slot, player.getPrayer().isAncientCurses());
				break;
			case 43:
				player.getPrayer().setQuickPrayers();
				break;
			default:
			}
			break;
		case 182:
			switch (buttonId) {
			case 5: // lobby
			case 10: // out
				if (player.getAttribute("duelingWith") != null && World.getWorld().getAreaManager().getAreaByName("Duel").contains(player.getLocation())) {
					player.sendMessage("You cannot logout while in a duel!");
					return;
			//}
					//if (player.getRights() != 2) {
					//	SQL.createConnection();
					//	SQL.saveHighScore(player);
					//	SQL.destroyConnection();
				}
				ActionSender.sendLogout(player, buttonId);
				break;
			}
			break;
		case 982: // split chat settings
			if (buttonId == 5) {
				int winId = player.getConnection().getDisplayMode() < 2 ? 548
						: 746;
				int slotId = player.getConnection().getDisplayMode() < 2 ? 214
						: 99;
				ActionSender.sendInterface(player, 1, winId, slotId, 261);
			} else if (buttonId >= 13 && buttonId <= 32) {
				int txtcolor = buttonId - 13;
				player.getSettings().setClanChatTextColor(txtcolor);
				ActionSender.sendConfig(player, 1438, player.getSettings()
						.getClanChatTextColor());
			} else if (buttonId == 37) {
				if (player.getSettings().getPrivateTextColor() == 0) {
					player.getSettings().setPrivateTextColor(1);
					ActionSender.sendConfig(player, 287, 1);
				} else {
					player.getSettings().setPrivateTextColor(0); //turns split chat off
				}
			} else {
				if (buttonId >= 45 && buttonId <= 62) {
					int color = buttonId - 44;
					player.getSettings().setPrivateTextColor(color);
					ActionSender.sendConfig(player, 287, player.getSettings()
							.getPrivateTextColor());
				}
			}
			break;
		case 755:
			switch (packet.getOpcode()) {
			case 6:
				switch (buttonId) {
				case 44:// Close Button
					player.removeAttribute("worldmap");
					break;
				}
				break;
			}
			break;
		case 746: //for resizeable mode, do everything here if needed also for case 548
			switch (packet.getOpcode()) {
			case 39:
				player.getSkills().setExperienceCounter(0);
				ActionSender.sendConfig(player, 1801, player.getSkills()
						.getExperienceCounter() * 10);
				break;
			}
			switch (buttonId) {
			case 179:
				ActionSender.sendWindowsPane(player, 755, 0);
				player.setAttribute("worldmap", true);
				ActionSender.sendBConfig(player, 622, player.getLocation()
						.getX() << 14
						| player.getLocation().getY()
						| player.getLocation().getZ() << 28);
				World.getWorld().submit(new Tick(3) {
					@Override
					public void execute() {
						if (player.getAttribute("worldmap") != null) {
							player.animate(840);
							ActionSender.sendBConfig(player, 674, player
									.getLocation().getX() << 14
									| player.getLocation().getY()
									| player.getLocation().getZ() << 28);
						} else {
							player.animate(Animation.RESET);
							player.setAttribute("resetCanvasAndRegionalData",
									true);
							InterfaceSettings.switchWindow(player, player
									.getConnection().getDisplayMode());
							stop();
						}
					}
				});
				break;
			case 172:
				player.closeAll(true, true);
				BookManager.proceedBook(player, 5); //advice
				break;
			}
			break;
		case 548: //for fixed screen, do everything here if needed also for case 746
			switch (packet.getOpcode()) {
			case 39:
				player.getSkills().setExperienceCounter(0);
				ActionSender.sendConfig(player, 1801, player.getSkills()
						.getExperienceCounter() * 10);
				break;
			case 6:
				switch (buttonId) {
				case 179:
					ActionSender.sendWindowsPane(player, 755, 0);
					player.setAttribute("worldmap", true);
					ActionSender.sendBConfig(player, 622, player.getLocation()
							.getX() << 14
							| player.getLocation().getY()
							| player.getLocation().getZ() << 28);
					World.getWorld().submit(new Tick(3) {
						@Override
						public void execute() {
							if (player.getAttribute("worldmap") != null) {
								player.animate(840);
								ActionSender.sendBConfig(player, 674, player
										.getLocation().getX() << 14
										| player.getLocation().getY()
										| player.getLocation().getZ() << 28);
							} else {
								player.setAttribute(
										"resetCanvasAndRegionalData", true);
								player.animate(Animation.RESET);
								InterfaceSettings.switchWindow(player, player
										.getConnection().getDisplayMode());
								stop();
							}
						}
					});
					break;
				case 183:
					player.closeAll(true, true);
					BookManager.proceedBook(player, 5); //advice
					break;
				}
				break;
			}
			if (buttonId > 127) {
				player.setAttribute("viewTab", buttonId - 128);
			} else {
				player.setAttribute("viewTab", 8 + buttonId - 98);
			}
			// ActionSender.closeSideInterface(p);
			/*
			 * switch (buttonId) { case 124: ActionSender.sendInterface(p, 1,
			 * 548, 199, 320); break; case 123: ActionSender.sendInterface(p, 1,
			 * 548, 198, 884); break; case 125: ActionSender.sendInterface(p, 1,
			 * 548, 200, 190); break; case 126: ActionSender.sendInterface(p, 1,
			 * 548, 201, 259); break; case 127: ActionSender.sendInterface(p, 1,
			 * 548, 202, 149); break; case 128: ActionSender.sendInterface(p, 1,
			 * 548, 203, 387); break; case 129: ActionSender.sendInterface(p, 1,
			 * 548, 204, 271); break; case 130: ActionSender.sendInterface(p, 1,
			 * 548, 205, p.getSettings().getSpellBook());// BOOK break; case 93:
			 * ActionSender.sendInterface(p, 1, 548, 206, 891); break; case 94:
			 * ActionSender.sendInterface(p, 1, 548, 207, 550); break; case 95:
			 * ActionSender.sendInterface(p, 1, 548, 208, 551); break; case 96:
			 * ActionSender.sendInterface(p, 1, 548, 209, 589); break; case 97:
			 * ActionSender.sendInterface(p, 1, 548, 210, 261); break; case 98:
			 * ActionSender.sendInterface(p, 1, 548, 211, 464); break; case 99:
			 * ActionSender.sendInterface(p, 1, 548, 212, 187); break; case 100:
			 * ActionSender.sendInterface(p, 1, 548, 213, 34); break; default:
			 * System.out.println(buttonId); }
			 */

			break;
		case 853:
			System.out.println(buttonId);
			break;
		case 751:
			switch (packet.getOpcode()) { //tab settings (like private chat on/off)
			case 0: //private chat on friends
				if (player.getSettings().getPrivateChatSetting() != 1) {
					player.getSettings().setPrivateChatSetting(1);
					String name = player.getFormattedUsername();
					for (Player pl : World.getWorld().getPlayers()) {
						if (pl.getFriendManager().getFriends().contains(name)) {
							pl.getFriendManager().updateFriend(name, player);
						}
					}
					for (Player pl : World.getWorld().getLobbyPlayers()) {
						if (pl.getFriendManager().getFriends().contains(name)) {
							pl.getFriendManager().updateFriend(name, player);
						}
					}
				}
				break;
			case 13: //private chat on
				if (player.getSettings().getPrivateChatSetting() != 0) {
					player.getSettings().setPrivateChatSetting(0);
					String name = player.getFormattedUsername();
					for (Player pl : World.getWorld().getPlayers()) {
						if (pl.getFriendManager().getFriends().contains(name)) {
							pl.getFriendManager().updateFriend(name, player);
						}
					}
					for (Player pl : World.getWorld().getLobbyPlayers()) {
						if (pl.getFriendManager().getFriends().contains(name)) {
							pl.getFriendManager().updateFriend(name, player);
						}
					}
				}
				break;
			case 15: //private chat off
				if (player.getSettings().getPrivateChatSetting() != 2) {
					player.getSettings().setPrivateChatSetting(2);
					String name = player.getFormattedUsername();
					for (Player pl : World.getWorld().getPlayers()) {
						if (pl.getFriendManager().getFriends().contains(name)) {
							pl.getFriendManager().updateFriend(name, player);
						}
					}
					for (Player pl : World.getWorld().getLobbyPlayers()) {
						if (pl.getFriendManager().getFriends().contains(name)) {
							pl.getFriendManager().updateFriend(name, player);
						}
					}
				}
				break;
			}
			break;
		case 261:
			switch (buttonId) {
			case 3:
				ActionSender.sendConfig(player, 173, player.getWalkingQueue()
						.isRunToggled() ? 0 : 1);
				player.getWalkingQueue().setRunToggled(
						!player.getWalkingQueue().isRunToggled());
				break;
			case 5:
				if (player.getConnection().getDisplayMode() == 1) {
					ActionSender.sendInterface(player, 1, 548, 214, 982);
					// ActionSender.sendAccessMask(player, -1, -1, 548, 97, 0,
					// 2);
				} else {
					ActionSender.sendInterface(player, 1, 746, 99, 982);
					// ActionSender.sendAccessMask(player, -1, -1, 746, 48, 0,
					// 2);
				}
				break;
			case 6: // Mouse Button config
				player.setAttribute("mouseButtons", (Integer) player
						.getAttribute("mouseButtons", 0) == 0 ? 1 : 0);
				ActionSender.sendConfig(player, 170,
						(Integer) player.getAttribute("mouseButtons"));
				break;
			case 7: // Accept aid config
				if (!player.getSettings().getAcceptAidOn()) {
					player.getSettings().setAcceptAidOn(true);
					ActionSender.sendConfig(player, 427, 1);
				} else {
					player.getSettings().setAcceptAidOn(false);
					ActionSender.sendConfig(player, 427, 0);
				}
				break;
			case 8: // House Building Options
				ActionSender.sendInventoryInterface(player, 398);
				break;
			case 14:
				ActionSender.sendInterface(player, 742);
				break;
			case 16:
				ActionSender.sendInterface(player, 743);
				break;
			default:
				break;
			}
			break;
		/*case 662: {
			  Familiar familiar = player.getFamiliar();
			  if (familiar != null) {
				  switch (buttonId) {
				  case 49:
					  familiar.callToPlayer(false);
					  break;
				  case 51:
					  familiar.dismiss();
					  break;
					  }
				  }
			 }
			break;
		case 747:
			 Familiar familiar = player.getFamiliar();
			 if (familiar != null) {
				switch (buttonId) {
			 	case 18:
			 		familiar.showDetails();
			 		break;
			 	case 12:
			 		if (familiar instanceof BeastOfBurden) {
			 			((BeastOfBurden)player.getFamiliar()).take();
			 		}
			 		break;
			 	case 10:
			 		familiar.callToPlayer(false);
			 		break;
			 	case 11:
			 		familiar.dismiss();
			 		break;
				}
			 }
			break;*/
		default:
			// remove this before you put it on the main server
			// System.out.println("interfaceId: " + interfaceId + ", buttonId: "
			// + buttonId);
		}
	}
}

