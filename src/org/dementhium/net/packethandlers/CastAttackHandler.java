package org.dementhium.net.packethandlers;

import org.dementhium.content.DialogueManager;
import org.dementhium.content.areas.CoordinateEvent;
import org.dementhium.content.misc.Following;
import org.dementhium.io.PlayerLoader;
import org.dementhium.model.Item;
import org.dementhium.model.World;
import org.dementhium.model.map.path.DefaultPathFinder;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.npc.impl.Impling;
import org.dementhium.model.player.Player;
import org.dementhium.net.ActionSender;
import org.dementhium.net.PacketHandler;
import org.dementhium.net.message.Message;
import org.dementhium.util.Constants;

/**
 * @author 'Mystic Flow <Steven@rune-server.org>
 * @author Steve <golden_32@live.com>
 * @author Lumby <lumbyjr@hotmail.com>
 */
public class CastAttackHandler extends PacketHandler {

	@Override
	public void handlePacket(Player player, Message packet) {
		switch (packet.getOpcode()) {
		case 14:
			attackNPC(player, packet);
			break;
		case 78:
			attackPlayer(player, packet);
			break;
		}

	}

	private void attackPlayer(final Player player, Message packet) {
		int index = packet.readLEShortA(); //pindex
		int slot = (packet.readLEShortA() >> 8) - 128;//?
		final int buttonId = packet.readLEShort();
		final int interfaceId = packet.readLEShort();
		int test = packet.readByteS(); //?
		int itemId = packet.readShortA(); //? item id maybe :p (yes)	
        if (index < 0 || index >= Constants.MAX_AMT_OF_PLAYERS) {
            return;
        }
		final Player toAttack = World.getWorld().getPlayers().get(index);
        if (!org.dementhium.model.instance.InstanceAccess.canInteract(player,toAttack)) return;
		if (toAttack == null) {
			return;
		}
		player.closeAll(true, true);
		player.getCombatExecutor().reset();
		player.getActionManager().stopAction();
		player.turnTo(toAttack, false);
		if (interfaceId == 193 || interfaceId == 192) { //magic book interface
			player.setAttribute("spellId", buttonId);
			player.getCombatExecutor().setVictim(toAttack);		
		} else if (interfaceId == 149) { //inv. interface
			final Item item = player.getInventory().get(slot);
			if (item == null || item.getId() != itemId) {
				return;
			}
	        if (toAttack == null || !toAttack.isOnline()) {
	            return;
	        }
	        player.getActionManager().stopAction();
			player.setAttribute("itemSlot", slot);
			//if (player.getLocation().getDistance(toAttack.getLocation()) < 2) {
				//handleItemPlayerInteraction(player, item, toAttack);
				//return;
			//}
			//if (!World.getWorld().doPath(new DefaultPathFinder(), player, toAttack.getLocation().getX(), toAttack.getLocation().getY()).isRouteFound()) {
				//player.sendMessage("I can't reach that!");
				//return;
			//}
            if (!World.getWorld().doPath(new DefaultPathFinder(), player, toAttack.getLocation().getX(), toAttack.getLocation().getY(), false, false).isRouteFound()) {
                player.sendMessage("I can't reach that!");
                return;
            } else {
            	Following.combatFollow(player, toAttack);
            }
			//int instantActionId = player.getAttribute("instantPlayerAction", -1); //do something with this
			//player.setAttribute("instantPlayerAction", -1); //do something with this
			
			//World.getWorld().getPlayers().get(index);
			
			//if (instantActionId == toAttack.getId() && player.getLocation().getDistance(toAttack.getLocation()) < 6) {
				//handleItemPlayerInteraction(player, item, toAttack);
				//return;
			//}
			World.getWorld().submitAreaEvent(player, new CoordinateEvent(player, toAttack.getLocation().getX(), toAttack.getLocation().getX(), toAttack.size(), toAttack.size()) {
				@Override
				public void execute() {
					handleItemPlayerInteraction(player, item, toAttack);
				}
			});
		} else if (interfaceId == 747 || interfaceId == 662 || interfaceId == 880) {
			if (player.getFamiliar() != null) {
				if (buttonId == 14 || buttonId == 23 || buttonId == 65) {
					if (player.getFamiliar().canCastAttack(toAttack)) {
						if (player.getCombatExecutor().getLastAttacker() == null && player.getCombatExecutor().getVictim() == null) {
							player.sendMessage("Your familiar cannot fight unless you have attacked or been attacked recently.");
							return;
						}
						player.getFamiliar().getCombatExecutor().setVictim(toAttack);
					}
				} else if (buttonId == 17 || buttonId == 74) {
					player.getFamiliar().specialMove(toAttack);
				}
			}
		}
	}
	
	private void handleItemPlayerInteraction(Player player, Item item, Player other) {
		int slot = player.getAttribute("itemSlot", 0);
		Item used = player.getInventory().get(slot);
		if (used == null || used.getId() != item.getId()) {
			return;
		}
		player.turnTo(other, false);
		player.setAttribute("itemUsedOn", other);
		if (player.getActivity().itemAction(player, item, 0, "ItemOnPlayer", other)) {
			return;
		}
		if (used.getId() == 962) {
			boolean allowAdmin = false;
			if (player.getRights() >=2 || other.getRights() >= 2) {
				for(String name : PlayerLoader.superMods) {
					if(player.getUsername().equals(name) || other.getUsername().equals(name)) {
						allowAdmin = true;
					}
				}
			}
			if (((player.getRights() >= 2 && other.getRights() < 2) || (other.getRights() >= 2 && player.getRights() < 2)) && !allowAdmin) {
				if (player.getRights() >= 2)
					player.sendMessage("Administrators can't use Christmas crackers on players.");
				if (other.getRights() >= 2)
					player.sendMessage("You can't use a Christmas cracker on an administrator.");
				return;
			}
			if (!other.getSettings().getAcceptAidOn()) {
				player.sendMessage("The other player has accept aid turned off.");
				return;
			}
			DialogueManager.send2OptionDialogueWithLongTitle(player, new int[]{629, -1}, "That's okay, I might get a party hat!", "Stop, I want to keep my cracker.");
			ActionSender.sendString(player, "If you pull the cracker, it will be destroyed.", 718, 0); //inter 140/554 will do as well?
			return;
		}
		boolean allowAdmin = false;
		if (player.getRights() >=2 || other.getRights() >= 2) {
			for(String name : PlayerLoader.superMods) {
				if(player.getUsername().equals(name) || other.getUsername().equals(name)) {
					allowAdmin = true;
				}
			}
		}
		if (((player.getRights() >= 2 && other.getRights() < 2) || (other.getRights() >= 2 && player.getRights() < 2)) && !allowAdmin) {
			if (player.getRights() >= 2)
				player.sendMessage("Administrators can't use items on players.");
			if (other.getRights() >= 2)
				player.sendMessage("You can't use items on an administrator.");
			return;
		}
		if (used.getId() == 10535) {
			if (!other.getSettings().getAcceptAidOn()) {
				player.sendMessage("The other player has accept aid turned off.");
				return;
			} else if (other.getInventory().getFreeSlots() < 1) {
				player.sendMessage("The other player does not have enough inventory space to perform this action.");
				return;
			} else {
				player.animate(832);
				player.getInventory().deleteItem(10535, 1);
				player.getInventory().refresh();
				other.getInventory().addItem(10535, 1);
				other.getInventory().refresh();
				return;
			}
		} else {
			System.out.println("Unhandled item on Player interaction: " + item.getId() + ".");
		}
	}

	private void attackNPC(final Player player, Message packet) {
		final int buttonId = packet.readShort();
		final int interfaceId = packet.readShort();
		final int slot = packet.readShort() >> 8;
		int npcIndex = packet.readShort();
		boolean running = packet.readByteS() == 1;
		final int itemId = packet.readLEShort();
		final NPC toAttack = World.getWorld().getNpcs().get(npcIndex);
        if (!org.dementhium.model.instance.InstanceAccess.canInteract(player,toAttack)) return;
		if (toAttack == null) {
			return;
		}
		System.out.println(buttonId + ", " + interfaceId + ", " + slot + ", " + npcIndex + ", " + running + ", " + itemId);
		player.closeAll(true, true);
		player.getActionManager().stopAction();
		player.getCombatExecutor().reset();
		player.getWalkingQueue().setIsRunning(running);
		player.turnTo(toAttack, false);
		if (interfaceId == 193 || interfaceId == 192) {
			if (!(toAttack instanceof Impling) || (buttonId != 36 && buttonId != 55 && buttonId != 81)) {
				if (!toAttack.isAttackable() && (toAttack.getDefinition().getName() == null 
						|| !toAttack.getDefinition().getName().equals("Barricade"))) {
					return;
				}
			}
			int lockedEnemy = -1;
			Object enemyAttr = toAttack.getAttribute("enemyIndex");
			if (enemyAttr instanceof Number) {
				lockedEnemy = ((Number) enemyAttr).intValue();
			}
			if (lockedEnemy > -1 && lockedEnemy != player.getIndex()) {
				player.sendMessage("This is not your enemy!");
				return;
			}
			player.setAttribute("spellId", buttonId);
			player.getCombatExecutor().setVictim(toAttack);            
		} else if (interfaceId == 149) {
			final Item item = player.getInventory().get(slot);
			if (item == null || item.getId() != itemId) {
				return;
			}
			player.setAttribute("itemSlot", slot);
			if (player.getLocation().getDistance(toAttack.getLocation()) < 2) {
				handleItemNPCInteraction(player, item, toAttack);
				return;
			}
			if (!World.getWorld().doPath(new DefaultPathFinder(), player, toAttack.getLocation().getX(), toAttack.getLocation().getY()).isRouteFound()) {
				player.sendMessage("I can't reach that!");
				return;
			}
			int instantActionId = player.getAttribute("instantNPCAction", -1);
			player.setAttribute("instantNPCAction", -1);
			if (instantActionId == toAttack.getId() && player.getLocation().getDistance(toAttack.getLocation()) < 6) {
				handleItemNPCInteraction(player, item, toAttack);
				return;
			}
			World.getWorld().submitAreaEvent(player, new CoordinateEvent(player, toAttack.getLocation().getX(), toAttack.getLocation().getX(), toAttack.getDefinition().getCacheDefinition().size, toAttack.getDefinition().getCacheDefinition().size) {
				@Override
				public void execute() {
					handleItemNPCInteraction(player, item, toAttack);
				}
			});
		} else if (interfaceId == 747 || interfaceId == 662 || interfaceId == 880) {
			if (player.getFamiliar() != null) {
				if (buttonId == 14 || buttonId == 23 || buttonId == 65) {
					if (player.getFamiliar().canCastAttack(toAttack)) {
						if (player.getCombatExecutor().getLastAttacker() == null && player.getCombatExecutor().getVictim() == null) {
							player.sendMessage("Your familiar cannot fight unless you have attacked or been attacked recently.");
							return;
						}
						player.getFamiliar().getCombatExecutor().setVictim(toAttack);
					}
				} else if (buttonId == 17 || buttonId == 74) {
					player.getFamiliar().specialMove(toAttack);
				}
			}
			//Disabled so Mystic Flow doesn't leech again..
		}
	}

	/**
	 * Handles item on NPC interaction.
	 * @param player The player.
	 * @param item The item.
	 * @param toAttack The NPC the item is used on.
	 */
	private void handleItemNPCInteraction(Player player, Item item, NPC toAttack) {
		int slot = player.getAttribute("itemSlot", 0);
		Item used = player.getInventory().get(slot);
		if (used == null || used.getId() != item.getId()) {
			return;
		}
		player.getMask().setFacePosition(toAttack.getLocation(),
				toAttack.getDefinition().getCacheDefinition().size, toAttack.getDefinition().getCacheDefinition().size);
		if (player.getActivity().itemAction(player, item, 0, "ItemOnNPC", toAttack)) {
			return;
		} else if (toAttack.isFamiliar()) {
			if ((player.getFamiliar() != null && !player.getFamiliar().equals(toAttack))
					|| player.getFamiliar() == null) {
				player.sendMessage("This is not your familiar.");
				return;
			} else if (!player.getFamiliar().isBeastOfBurden()) {
				player.sendMessage("Your familiar is not a beast of burden.");
				return;
			}
			player.getFamiliar().store(item.getId(), slot, item.getAmount());
			//Disabled so Mystic Flow doesn't leech again..
			return;
		}
		System.out.println("Unhandled item on NPC interaction: " + item.getId() + ", " + toAttack.getId() + ".");
	}
}
