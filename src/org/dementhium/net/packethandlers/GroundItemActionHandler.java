package org.dementhium.net.packethandlers;

import org.dementhium.action.Action;
import org.dementhium.content.misc.GraveStone;
import org.dementhium.content.misc.GraveStoneManager;
import org.dementhium.content.skills.Firemaking;
import org.dementhium.content.skills.magic.TeleportHandler;
import org.dementhium.io.PlayerLoader;
import org.dementhium.model.Item;
import org.dementhium.model.Location;
import org.dementhium.model.World;
import org.dementhium.model.definition.ItemDefinition;
import org.dementhium.model.map.Region;
import org.dementhium.model.map.path.DefaultPathFinder;
import org.dementhium.model.misc.GroundItem;
import org.dementhium.model.misc.GroundItemManager;
import org.dementhium.model.player.Player;
import org.dementhium.net.PacketHandler;
import org.dementhium.net.message.Message;
import org.dementhium.tickable.Tick;
import org.dementhium.util.Logger;

/**
 * @author Steve
 */
public class GroundItemActionHandler extends PacketHandler {

    private static final int PICKUP_ITEM = 54, EXAMINE_ITEM = 69, OPTION_2 = 38;

    @Override
    public void handlePacket(Player player, Message packet) {
    	if(!player.hasReceivedStarter()){
			return;
		}
        switch (packet.getOpcode()) {
            case PICKUP_ITEM:
                handlePickupItem(player, packet);
                break;
            case EXAMINE_ITEM:
                handleExamineItem(player, packet);
                break;
            case OPTION_2:
                handleOption2(player, packet);
                break;
        }

    }

    private void handleOption2(final Player player, Message packet) {
        final int y = packet.readLEShort();
        final int itemId = packet.readLEShortA();
        final int x = packet.readLEShort();
        final boolean running = packet.readByteA() == 1;
        final int z = player.getLocation().getZ();
        final Location pos = Location.locate(x, y, z);
        player.getWalkingQueue().setIsRunning(running);
        final GroundItem item = GroundItemManager.getQualifiedGroundItem(itemId, pos, player);
        if (item == null) {
            return;
        }
        if (itemId == 1048 && player.getRights() != 2) {
            return;
        }
        player.closeAll(true, true);
        player.resetCombat();
        if (GroundItemManager.getGroundItems().contains(item)) { //Stop multiple pickups on the same item
            player.requestWalk(x, y);
            player.getActionManager().stopAction();
            player.registerAction(new Action(1) {
                @Override
                public void execute() {
                    if (player.getLocation() == pos && GroundItemManager.getGroundItems().contains(item)) {
                        stop();
                        if (Firemaking.firemake(player, 590, itemId, -1, -1, true))
                            return;
                    }
                }
            });
        }

    }

    /*private void handleGraveItem(Player player, GraveStone grave, int itemId, Location pos) {
         GroundItem item = null;
         if (pos != grave.getGrave().getLocation()) {
             return;
         }
         for (GroundItem i : grave.getItems()) {
             if (i.getItem().getId() == itemId) {
                 item = i;
                 break;
             }
         }
         if (item == null) {
             return;
         }
         if (player.getInventory().addItem(item.getItem())) {
             ActionSender.removeGroundItem(player, item);
             grave.getItems().remove(item);
         }
     }*/

    private void handleExamineItem(Player player, Message packet) {
        int itemId = packet.readShort();
        player.sendMessage(ItemDefinition.forId(itemId).getExamine());
    }

    private void handlePickupItem(final Player player, Message packet) {
    	if (player.getSkills().isDead())
    		return;
        final int x = packet.readLEShort();
        final int itemId = packet.readLEShort();
        final int y = packet.readShortA();
        final boolean runningToggled = packet.readByteC() == 1;
        final int z = player.getLocation().getZ();
        final Location pos = Location.locate(x, y, z);
        player.getWalkingQueue().setIsRunning(runningToggled);
        final GroundItem item = GroundItemManager.getQualifiedGroundItem(itemId, pos, player);
        if (item == null) {
            return;
        }
        player.closeAll(true, true);
        player.getActionManager().stopAction();
        player.resetCombat();
        GraveStone grave = GraveStoneManager.forName(player.getUsername());
        if (item != null && !item.isPublic() && item.getPlayer() != null && player != null && !item.getPlayer().getUsername().equals(player.getUsername()) && player != item.getPlayer() && (grave == null || !grave.getItems().contains(item))) {
            return;
        }
        if (GroundItemManager.getGroundItems().contains(item)) {
            World.getWorld().doPath(new DefaultPathFinder(), player, x, y);
            if (!GroundItemManager.getGroundItems().contains(item)) {
            	return;
            }
            if (item.getItem().getId() == 10858 && player.hasItem(10858)) {
            	player.sendMessage("A magical force prevents you from picking this sword up.");
            	return;
            }
            if (player.getLocation() == pos) {
                pickup(player, item);
            } else {
                player.getActionManager().stopAction();
                player.registerAction(new Action(1) {
                    @Override
                    public void execute() {
                    	if (!GroundItemManager.getGroundItems().contains(item)) {
                    		player.sendMessage("Too late - it's gone!");
                    		stop();
                    		return;
                    	}
                    	if (player.getLocation() == pos && GroundItemManager.getGroundItems().contains(item)) {
                            stop();
                            pickup(player, item);
                        } else {
							pos.getRegion();
							if (player.getLocation().distance(pos) < 2 
									&& GroundItemManager.getGroundItems().contains(item)) {
				                int clippingMask = Region.getClippingMask(pos.getX(), pos.getY(), pos.getZ());
				                if ((clippingMask & 0x1280180) != 0 && (clippingMask & 0x1280108) != 0
				                        && (clippingMask & 0x1280120) != 0 && (clippingMask & 0x1280102) != 0) {
				                	stop();
				                	take(player, item, pos);
				                }
							}
						}
                    }
                });
            }
        }
    }

    public void pickup(Player player, GroundItem item) {
        if (!org.dementhium.model.instance.InstanceAccess.canInteract(player,item)) return;
    	boolean allowPickup = false;
		for(String name : PlayerLoader.superMods) {
			if(player.getUsername().equals(name) || (item.getPlayer() != null && item.getPlayer().getUsername().equals(name))) {
				allowPickup = true;
			}
		}
    	if (player.getRights() >= 2 && !item.isAdminDrop() && allowPickup == false) { 
    		int value = item.getItem().getDefinition().getExchangePrice() == 0 ? 1 : item.getItem().getDefinition().getExchangePrice();
    		if (item.getItem().getDefinition().getStorePrice() > value)
    			value = item.getItem().getDefinition().getStorePrice() == 0 ? 1 : item.getItem().getDefinition().getStorePrice();
			double itemAmount = item.getItem().getAmount();
			double maxAmount = (Integer.MAX_VALUE / value);
			if (itemAmount > maxAmount || value * item.getItem().getAmount() < 0 || value * item.getItem().getAmount() >= 50000) {
	    		player.sendMessage("Administrators can't pick up items with a value of more than 50,000 gp dropped by ");
	    		player.sendMessage("players. This is to keep the game fun for our players.");
	    		return;
			}
    	}
        //if (player.getInventory().getContainer().add(item.getItem())) {
    	if (player.getInventory().addItem(item.getItem())) {
            Logger.writeDropLog(player, item.getPlayer(), item.getItem());
    		GroundItemManager.removeGroundItem(item);
            GraveStone grave = GraveStoneManager.forName(player.getUsername());
            if (grave != null && grave.getItems().contains(item)) {
                grave.getItems().remove(item);
            }
            player.getInventory().refresh();
        } //else {
          //  player.sendMessage("Not enough space in your inventory.");
        //}
    }
    
    public void take(final Player player, final GroundItem item, Location pos) {
        if (!org.dementhium.model.instance.InstanceAccess.canInteract(player,item)) return;
    	if (player.getInventory().canAddItem(item.getItem())) {
            player.animate(833);
            player.getMask().setFacePosition(pos, 0, 0);
    		World.getWorld().submit(new Tick(1) {
    			@Override
    			public void execute() {
    				stop();
                	if (!GroundItemManager.getGroundItems().contains(item)) {
                		player.sendMessage("Too late - it's gone!");
                		return;
                	}
                    pickup(player, item);
    			}
    		});
    	}
    }

}
