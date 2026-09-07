package org.dementhium.event.impl.interfaces;

import org.dementhium.content.areas.CoordinateEvent;
import org.dementhium.content.dialogue.DialogueManager;
import org.dementhium.content.dialogue.OptionAction;
import org.dementhium.event.EventListener;
import org.dementhium.event.EventManager;
import org.dementhium.model.Location;
import org.dementhium.model.World;
import org.dementhium.model.map.path.DefaultPathFinder;
import org.dementhium.model.player.Player;
import org.dementhium.net.ActionSender;
import org.dementhium.net.packethandlers.NpcOption;
import org.dementhium.net.packethandlers.WalkingHandler;
import org.dementhium.tickable.Tick;
import org.dementhium.util.InputHandler;


/**
 * Handles the summoning orb and interface buttons.
 * @author Emperor
 *
 */
public class FamiliarInterfaceListener extends EventListener {

	@Override
	public void register(EventManager manager) {
		manager.registerInterfaceListener(747, this);
		manager.registerInterfaceListener(662, this);
		manager.registerInterfaceListener(880, this);
		manager.registerInterfaceListener(665, this);
		manager.registerInterfaceListener(671, this);
	}

	@Override
    public boolean interfaceOption(final Player player, int interfaceId, int buttonId, int slot, int itemId, int opcode) {
		if (interfaceId == 662) {
			if (player.getFamiliar() == null)
				return true;
			switch (buttonId) {
			case 49:
				player.getFamiliar().callToOwner();
				return true;
			case 51:
				if (opcode == 6) {
					DialogueManager.get(34).getActions().set(0, new OptionAction() {
						@Override
						public boolean handle(Player player) {
							player.getFamiliar().dismiss(true, null);
							return true;
						}
					});
					DialogueManager.sendDialogue(player, 33);
				} else if (opcode == 13)
					player.getFamiliar().dismiss(true, null);
				return true;
			case 67:
				if (!player.getFamiliar().isBeastOfBurden()) {
					player.sendMessage("Your familiar is not a beast of burden, so it's not carrying any items.");
					return true;
				}
				player.getFamiliar().withdrawAll();
				return true;
			case 69:
				if (player.getFamiliar().getTicks() > 100) {
					player.sendMessage("You can only renew your familiar when it's about to die.");
					return true;
				}
				if (!player.getInventory().contains(player.getFamiliar().getPouchId())) {
					player.sendMessage("You need a pouch to renew your familiar.");
					return true;
				}
				player.sendMessage("You sacrifice a pouch to renew your familiar's timer.");
				player.getInventory().deleteItem(player.getFamiliar().getPouchId(), 1);
				player.getFamiliar().setTicks(player.getFamiliar().getMaximumTicks());
				player.getFamiliar().callToOwner();
				return true;
			case 74:
				player.getFamiliar().specialMove(null);
				return true;
			}
		} else if (interfaceId == 747) {
			if (player.getFamiliar() == null && buttonId != 7) {
				player.sendMessage("You do not have a familiar following you.");
				return true;
			}
			switch (buttonId) {
			case 18:
			case 9:
				ActionSender.sendConfig(player, 1160, 243269632);
				ActionSender.sendInterAnimation(player, player.getFamiliar().getDefinition().getCacheDefinition().renderEmote, 662, 1);
				World.getWorld().submit(new Tick(1) {
					@Override
					public void execute() {
						stop(); 
						ActionSender.sendInterAnimation(player, player.getFamiliar().getDefinition().getCacheDefinition().renderEmote, 662, 1);
						ActionSender.sendConfig(player, 1160, 243269632);
					}					
				});
				return true;
			case 11:
			case 20:
				DialogueManager.get(34).getActions().set(0, new OptionAction() {
					@Override
					public boolean handle(Player player) {
						player.getFamiliar().dismiss(true, null);
						return true;
					}					
				});
				DialogueManager.sendDialogue(player, 33);
				return true;
			case 12:
			case 21:
				if (!player.getFamiliar().isBeastOfBurden()) {
					player.sendMessage("Your familiar is not a beast of burden, so it's not carrying any items.");
					return true;
				}
				player.getFamiliar().withdrawAll();
				return true;
			case 15:
			case 26:
				player.closeAll(true, true);
				Location locationToWalk = player.getLocation();
				if (player.getLocation().distance(player.getFamiliar().getLocation()) != 1) {
					locationToWalk = NpcOption.getNearLocation(player.getLocation(), player.getFamiliar());
				}
				WalkingHandler.reset(player);
				player.turnTo(player.getFamiliar(), false);
				if (!World.getWorld().doPath(new DefaultPathFinder(), player, locationToWalk.getX(), locationToWalk.getY()).isRouteFound()) {
					player.sendMessage("I can't reach that!");
					return true;
				}
				World.getWorld().submitAreaEvent(player, new CoordinateEvent(player, locationToWalk.getX(), locationToWalk.getX(), player.getFamiliar().getDefinition().getCacheDefinition().size, player.getFamiliar().getDefinition().getCacheDefinition().size) {
					@Override
					public void execute() {
						player.getFamiliar().turnTo(player, true);
						player.getSettings().setSpeakingTo(player.getFamiliar());
						org.dementhium.content.DialogueManager.handle(player, player.getFamiliar());
					}
				});
				return true;
			case 13:
			case 22:
				if (player.getFamiliar().getTicks() > 100) {
					player.sendMessage("You can only renew your familiar when it's about to die.");
					return true;
				}
				if (!player.getInventory().contains(player.getFamiliar().getPouchId())) {
					player.sendMessage("You need a pouch to renew your familiar.");
					return true;
				}
				player.sendMessage("You sacrifice a pouch to renew your familiar's timer.");
				player.getInventory().deleteItem(player.getFamiliar().getPouchId(), 1);
				player.getFamiliar().setTicks(player.getFamiliar().getMaximumTicks());
				player.getFamiliar().callToOwner();
				return true;
			case 10:
			case 19:
				player.getFamiliar().callToOwner();
				return true;
			case 7:
				boolean res = player.getConnection().getDisplayMode() > 1;
				ActionSender.sendConfig(player, 1494, player.getSettings().getSummoningOption());
				ActionSender.sendInterface(player, 1, res ? 746 : 548, res ? 104 : 219, 880);
				ActionSender.sendBConfig(player, 168, 95);
				return true;
			case 17:
				player.getFamiliar().specialMove(null);
				return true;
			}
		} else if (interfaceId == 880) {
			if (buttonId == 25) {
				buttonId = 21;
			} else if (buttonId == 21) {
				int option = player.getAttribute("summoningOption", 0);
				player.getSettings().setSummoningOption(option);
				boolean res = player.getConnection().getDisplayMode() > 1;
				ActionSender.sendConfig(player, 1493, option);
				if (player.getFamiliar() != null) {
					ActionSender.sendInterface(player, 1, res ? 746 : 548, res ? 104 : 219, 662);
					ActionSender.sendBConfig(player, 1436, 1);
					ActionSender.sendConfig(player, 1160, 243269632);
					ActionSender.sendInterAnimation(player, player.getFamiliar().getDefinition().getCacheDefinition().renderEmote, 662, 1);
					World.getWorld().submit(new Tick(1) {
						@Override
						public void execute() {
							ActionSender.sendInterAnimation(player, player.getFamiliar().getDefinition().getCacheDefinition().renderEmote, 662, 1);
							ActionSender.sendConfig(player, 1160, 243269632);
							stop();
						}
					});
				} else {
					ActionSender.sendCloseInterface(player, res ? 746 : 548, res ? 104 : 219);
				}
				return true;
			}
			player.setAttribute("summoningOption", (buttonId - 7) / 2);
			ActionSender.sendConfig(player, 1494, (buttonId - 7) / 2);
			return true;
		} else if (interfaceId == 665) {
			if (player.getFamiliar() == null || !player.getFamiliar().isBeastOfBurden()) {
				return true;
			}
			switch (opcode) {
			case 6:
				player.getFamiliar().store(itemId, slot, 1);
				return true;
			case 13:
				player.getFamiliar().store(itemId, slot, 5);
				return true;
			case 0:
				player.getFamiliar().store(itemId, slot, 10);
				return true;
			case 15:
				player.getFamiliar().store(itemId, slot, Integer.MAX_VALUE);
				return true;
			case 46:
				InputHandler.requestIntegerInput(player, 9, "How many would you like to store?");
				player.setAttribute("slotId", slot);
				return true;
			}
		} else if (interfaceId == 671) {
			if (player.getFamiliar() == null || !player.getFamiliar().isBeastOfBurden()) {
				return true;
			}
			switch (opcode) {
			case 6:
				if (buttonId == 29) {
					player.getFamiliar().withdrawAll();
					return true;
				}
				player.getFamiliar().withdraw(itemId, slot, 1, true);
				return true;
			case 13:
				player.getFamiliar().withdraw(itemId, slot, 5, true);
				return true;
			case 0:
				player.getFamiliar().withdraw(itemId, slot, 10, true);
				return true;
			case 15:
				player.getFamiliar().withdraw(itemId, slot, Integer.MAX_VALUE, true);
				return true;
			case 46:
				InputHandler.requestIntegerInput(player, 10, "How many would you like to withdraw?");
				player.setAttribute("slotId", slot);
				return true;
			}
		}

		player.sendMessage("Unhandled summoning button for interface " + interfaceId + ": " + opcode + ", " + buttonId + ", " + slot + ", " + itemId + ".");
        return false;
    }
}