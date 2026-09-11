package org.dementhium.model.misc;

import java.util.ArrayList;
import java.util.List;
import org.dementhium.model.instance.GameInstance;
import org.dementhium.model.instance.InstanceManager;

import org.dementhium.content.misc.GraveStone;
import org.dementhium.content.misc.GraveStoneManager;
import org.dementhium.model.Item;
import org.dementhium.model.Location;
import org.dementhium.model.World;
import org.dementhium.model.definition.ItemDefinition;
import org.dementhium.model.map.Region;
import org.dementhium.model.player.Player;
import org.dementhium.net.ActionSender;
import org.dementhium.tickable.Tick;

/**
 * The ground item manager, makes sure dropped items get added/removed/made
 * global when needed.
 *
 * @author Emperor
 */
public class GroundItemManager {

    public static boolean hasItemsInArea(int x, int y, int width, int height) {
        if (groundItems == null) return false;
        for (GroundItem item : groundItems) {
            Location l = item.getLocation();
            if (l != null && l.getX() >= x && l.getY() >= y && l.getX() < x + width && l.getY() < y + height) return true;
        }
        return false;
    }

    /**
     * Represents all the current ground items.
     */
    private static List<GroundItem> groundItems;
    
    /**
     * Handles item indexes.
     */
    public static int groundItemIndex = 0;

    /**
     * Prepares the ground items list.
     */
    @SuppressWarnings("serial")
	public static void load() {
        groundItems = new ArrayList<GroundItem>() {
            @Override
            public boolean add(GroundItem item) {
                GameInstance owner = InstanceManager.at(item.getLocation());
                if (owner != null) owner.trackDrop(item);
                return super.add(item);
            }
            @Override
            public GroundItem remove(int index) {
                GroundItem item = get(index);
                GameInstance owner = InstanceManager.at(item.getLocation());
                if (owner != null) owner.untrackDrop(item);
                return super.remove(index);
            }
        	@Override
        	public boolean remove(Object item) {
                GameInstance owner = item instanceof GroundItem ? InstanceManager.at(((GroundItem) item).getLocation()) : null;
                if (owner != null) owner.untrackDrop((GroundItem) item);
                boolean ok = super.remove(item);
                if (ok && item instanceof RespawnableGroundItem) {
        			final RespawnableGroundItem respawnable = (RespawnableGroundItem) item;
                    final Location respawnLocation = respawnable.getLocation();
                    final org.dementhium.model.map.region.DynamicRegion respawnMap =
                        org.dementhium.model.map.region.RegionBuilder.getDynamicRegion(respawnLocation.getX(), respawnLocation.getY());
                    final long respawnRevision = respawnMap == null ? 0 : respawnMap.getChunkRevision(respawnLocation.getZ(), respawnLocation.getX() & 63, respawnLocation.getY() & 63);
                    Tick respawnTask = new Tick(respawnable.getDelay()) {
        				public void execute() {
        					stop();
                            if (org.dementhium.model.map.region.RegionBuilder.getDynamicRegion(respawnLocation.getX(), respawnLocation.getY()) != respawnMap
                                || (respawnMap != null && respawnMap.getChunkRevision(respawnLocation.getZ(), respawnLocation.getX() & 63, respawnLocation.getY() & 63) != respawnRevision)) return;
                            GroundItemManager.createGroundItem(respawnable, 1);
        				}
                };
                if (owner == null) World.getWorld().submit(respawnTask);
                else owner.submitTask(respawnTask);
        		}
        		return ok;
        	}
        };
    }

    /**
     * Creates a new ground item.
     *
     * @param groundItem The ground item.
     */
    public static void createGroundItem(final GroundItem groundItem) {
        createGroundItem(groundItem, 100);
    }

    /**
     * Creates a new ground item.
     *
     * @param groundItem The ground item.
     */
    public static void createGroundItem(final GroundItem groundItem, int updateTicks) {
        GameInstance instance = InstanceManager.at(groundItem.getLocation());
        if (instance != null && !instance.canTrackDrop(groundItem)) {
            Location target = instance.overflowDropLocation(groundItem.getPlayer());
            GroundItem overflow = new GroundItem(groundItem.getPlayer(), groundItem.getItem(), target,
                groundItem.isPublic(), groundItem.isAdminDrop(), groundItemIndex++);
            createGroundItem(overflow, updateTicks);
            if (groundItem.getPlayer() != null) groundItem.getPlayer().sendMessage("Your instance is full of ground items. This drop was placed at your return point.");
            return;
        }
    	if (!ItemDefinition.forId(groundItem.getItem().getId()).isTradeable())
    		groundItem.setUpdateTicks(updateTicks + 250);
    	else
    		groundItem.setUpdateTicks(updateTicks);
        groundItems.add(groundItem);
        if (groundItem.isPublic()) {
            //if (groundItem.getPlayer() != null) {
              //  ActionSender.removeGroundItem(groundItem.getPlayer(), groundItem);
            //}
            List<Player> players = Region.getLocalPlayers(groundItem.getLocation());
            for (Player player : players) {
                //if (groundItem.getPlayer() != null && groundItem.getPlayer().getUsername().equals(player.getUsername()))
                	//ActionSender.removeGroundItem(player, groundItem);
                if (!groundItem.isAdminDrop() || (groundItem.isAdminDrop() && player.getRights() >= 2))
                	ActionSender.sendGroundItem(player, groundItem);
            }
        } else {
            if (groundItem.getPlayer() != null) {
                ActionSender.sendGroundItem(groundItem.getPlayer(), groundItem);
            }
        }
    }

    /**
     * Sets a ground item in a public state.
     *
     * @param groundItem The ground item.
     */
    public static void setPublic(final GroundItem groundItem) {
        groundItem.setPublic(true);
        //groundItem.setUpdateTicks(100);
        //1 Tick = 0.6 s. -> 100 Ticks = 60 s. -> An item that is dropped by a player, is invisible to others for 60 seconds,
        //after those 60 s. it will turn visible to others for another 150 s. -> 150 : 0.6 = 250 Ticks.
        groundItem.setUpdateTicks(250);
        List<Player> players = Region.getLocalPlayers(groundItem.getLocation());
        for (Player player : players) {
        	if (!groundItem.isAdminDrop() || (groundItem.isAdminDrop() && player.getRights() >= 2)) {
        		if (groundItem.getPlayer() != null && groundItem.getPlayer().getUsername().equals(player.getUsername()))
                	ActionSender.removeGroundItem(player, groundItem);
                ActionSender.sendGroundItem(player, groundItem);
        	}
        }
    }

    /** Teardown removal bypasses RespawnableGroundItem's normal respawn scheduling. */
    public static void discardGroundItem(GroundItem groundItem) {
        for (int i = 0; i < groundItems.size(); i++) {
            if (groundItems.get(i) == groundItem) {
                groundItems.remove(i);
                notifyRemoval(groundItem);
                return;
            }
        }
    }
    public static void removeGroundItem(GroundItem groundItem) {
        groundItems.remove(groundItem);
        notifyRemoval(groundItem);
    }
    private static void notifyRemoval(GroundItem groundItem) {
        if (groundItem.isPublic()) {
            List<Player> players = Region.getLocalPlayers(groundItem.getLocation());
            for (Player player : players) {
            	if (!groundItem.isAdminDrop() || (groundItem.isAdminDrop() && player.getRights() >= 2))
            		ActionSender.removeGroundItem(player, groundItem);
            }
        } else {
            if (groundItem.getPlayer() != null) {
                List<Player> players = Region.getLocalPlayers(groundItem.getLocation());
                for (Player player : players) {
                	if (groundItem.getPlayer().getUsername().equals(player.getUsername()))
                		ActionSender.removeGroundItem(player, groundItem);
                }
            }
        }
    }

    /**
     * Removes a ground item holding the public flag.
     *
     * @param item The ground item.
     */
    public static void removePublicGroundItem(GroundItem item) {
        groundItems.remove(item);
        List<Player> players = Region.getLocalPlayers(item.getLocation());
        for (Player player : players) {
        	if (!item.isAdminDrop() || (item.isAdminDrop() && player.getRights() >= 2))
        		ActionSender.removeGroundItem(player, item);
        }
    }

    /**
     * Removes a ground item holding the private flag.
     *
     * @param item The ground item.
     */
    public static void removePrivateGroundItem(GroundItem item) {
        groundItems.remove(item);
        if (item.getPlayer() != null) {
            List<Player> players = Region.getLocalPlayers(item.getLocation());
            for (Player player : players) {
            	if (item.getPlayer().getUsername().equals(player.getUsername()))
            		ActionSender.removeGroundItem(player, item);
            }
        }
    }

    /**
     * Replaces an existing ground item with a new one.
     *
     * @param player    The player.
     * @param id        the id of the ground item to replace.
     * @param location  The location of the ground item to replace.
     * @param toReplace The ground item to replace with.
     */
    public static void replaceGroundItem(Player player, int id,
                                         Location location, GroundItem toReplace) {
        GroundItem item = getQualifiedGroundItem(id, location, player);
        if (item == null) {
            return;
        }
        if (item.isPublic()) {
            List<Player> players = Region.getLocalPlayers(item.getLocation());
            for (Player p : players) {
            	if (!item.isAdminDrop() || (item.isAdminDrop() && p.getRights() >= 2))
            		ActionSender.sendGroundItem(p, item);
            }
        } else {
            if (item.getPlayer() != null) {
                List<Player> players = Region.getLocalPlayers(item.getLocation());
                for (Player pl : players) {
                	if (item.getPlayer().getUsername().equals(pl.getUsername()))
                		ActionSender.sendGroundItem(pl, item);
                }
            }
        }
        groundItems.remove(item);
    	if (!toReplace.isPublic() && !ItemDefinition.forId(toReplace.getItem().getId()).isTradeable())
    		toReplace.setUpdateTicks(350);
    	else
    		toReplace.setUpdateTicks(100);
        groundItems.add(toReplace);
        if (toReplace.isPublic()) {
            List<Player> players = Region.getLocalPlayers(toReplace.getLocation());
            for (Player p : players) {
                if (toReplace.getPlayer() != null && toReplace.getPlayer().getUsername().equals(p.getUsername()))
                    ActionSender.removeGroundItem(p, toReplace);
            	if (!toReplace.isAdminDrop() || (toReplace.isAdminDrop() && p.getRights() >= 2))
            		ActionSender.sendGroundItem(p, toReplace);
            }
        } else {
            if (item.getPlayer() != null) {
                List<Player> players = Region.getLocalPlayers(item.getLocation());
                for (Player pl : players) {
                	if (item.getPlayer().getUsername().equals(pl.getUsername()))
                		ActionSender.sendGroundItem(pl, toReplace);
                }
            }
        }
    }

    /**
     * Replaces an existing ground item with a new one.
     *
     * @param player    The player.
     * @param id        the id of the ground item to replace.
     * @param location  The location of the ground item to replace.
     * @param toReplace The ground item to replace with.
     */
    public static void replacePrivateGroundItem(Player player, int id,
                                                Location location, GroundItem toReplace) {
        GroundItem item = null;
        for (GroundItem candidate : groundItems) {
            if (candidate != null && !candidate.isPublic() && candidate.getItem().getId() == id
                    && candidate.getLocation().equals(location) && candidate.getPlayer() != null
                    && candidate.getPlayer().getUsername().equals(player.getUsername())) {
                item = candidate;
                break;
            }
        }
        if (item == null) {
            return;
        }
        if (item.getPlayer() != null) {
            List<Player> players = Region.getLocalPlayers(item.getLocation());
            for (Player pl : players) {
            	if (player.getUsername().equals(pl.getUsername())) {
                    ActionSender.removeGroundItem(player, item);
                    groundItems.remove(item);
                	if (!ItemDefinition.forId(toReplace.getItem().getId()).isTradeable())
                		toReplace.setUpdateTicks(350);
                	else
                		toReplace.setUpdateTicks(100);
                    groundItems.add(toReplace);
                    ActionSender.sendGroundItem(player, toReplace);
            	}
            }
        }
    }

    /**
     * Increases the amount of a ground item by {@code 1}, <br>
     * or creates a new ground item at the given location when there was no
     * ground item found.
     *
     * @param player   The player.
     * @param id       The id of the ground item to increase.
     * @param location The location of the ground item to increase.
     */
    public static boolean increaseAmount(Player player, int id, Location location) {
        return increaseAmount(player, id, location, 1);
    }
    
    /**
     * Increases the amount of a ground item by {@code 1}, <br>
     * or creates a new ground item at the given location when there was no
     * ground item found.
     *
     * @param player   The player.
     * @param id       The id of the ground item to increase.
     * @param location The location of the ground item to increase.
     * @param amount The amount to increase.
     */
    public static boolean increaseAmount(Player player, int id, Location location, int amount) {
        GroundItem item = null;
        for (GroundItem candidate : groundItems) {
            if (candidate != null && !candidate.isPublic() && candidate.getItem().getId() == id
                    && candidate.getLocation().equals(location) && candidate.getPlayer() != null
                    && candidate.getPlayer().getUsername().equals(player.getUsername())) {
                item = candidate;
                break;
            }
        }
        if (item == null) {
            createGroundItem(new GroundItem(player, new Item(id, amount),
                    location, false, player.getRights() >= 2, groundItemIndex++));
            return true;
        }
        else if (!item.getItem().getDefinition().isStackable() && !item.getItem().getDefinition().isNoted()) {
            createGroundItem(new GroundItem(player, new Item(id, amount),
                    location, false, player.getRights() >= 2, groundItemIndex++));
            return true;
        } else if (item.getItem().getAmount() + amount < 0) {
            createGroundItem(new GroundItem(player, new Item(id, amount),
                    location, false, player.getRights() >= 2, groundItemIndex++));
            return true;
        }
        if (item.getPlayer() != null) {
            List<Player> players = Region.getLocalPlayers(item.getLocation());
            for (Player pl : players) {
            	if (player.getUsername().equals(pl.getUsername())) {
                    ActionSender.removeGroundItem(player, item);
                    groundItems.remove(item);
                    item.getItem().setAmount(item.getItem().getAmount() + amount);
                    groundItems.add(item);
                    ActionSender.sendGroundItem(player, item);
                    return true;
            	}
            }
        }
        return false;
    }

    /**
     * Gets a ground item with the given id and location, holding the private
     * flag.
     *
     * @param id  The id.
     * @param loc The location.
     * @return The ground item.
     */
    public static GroundItem getPrivateGroundItem(int id, Location loc) {
        GroundItem lastGroundItem = null;
        for (GroundItem item : groundItems) {
            if (item == null) {
                continue;
            }
            if (item.getItem().getId() == id && item.getLocation() == loc && !item.isPublic()) {
                if (lastGroundItem == null) {
                    lastGroundItem = item;
                    continue;
                }
                if (item.getUpdateTicks() > lastGroundItem.getUpdateTicks()) {
                    lastGroundItem = item;
                }
            }
        }
        return lastGroundItem;
    }

    /**
     * Gets a ground item with the given id and location, holding the public
     * flag.
     *
     * @param id  The id.
     * @param loc The location.
     * @return The ground item.
     */
    public static GroundItem getPublicGroundItem(int id, Location loc) {
        GroundItem lastGroundItem = null;
        for (GroundItem item : groundItems) {
            if (item == null) {
                continue;
            }
            if (item.getItem().getId() == id && item.getLocation() == loc && item.isPublic()) {
                if (lastGroundItem == null) {
                    lastGroundItem = item;
                    continue;
                }
                if (item.getUpdateTicks() > lastGroundItem.getUpdateTicks()) {
                    lastGroundItem = item;
                }
            }
        }
        return lastGroundItem;
    }

    /**
     * Gets a ground item with the given id and location.
     *
     * @param id  The id.
     * @param loc The location.
     * @return The ground item.
     */
   /*public static GroundItem getGroundItem(int id, Location loc) {
        for (GroundItem item : groundItems) {
            if (item == null) {
                continue;
            }
            if (item.getItem().getId() == id && item.getLocation() == loc) {
                return item;
            }
        }
        return null;
    }*/
    
    /**
     * Gets a for the player 'pickupable' ground item (if there is any) with the given id, location and player.
     *
     * @param id  The id.
     * @param loc The location.
     * @param player The player interacting with the item.
     * @return The ground item.
     */
    public static GroundItem getQualifiedGroundItem(int id, Location loc, Player player) {
        if (player!=null && !org.dementhium.model.instance.InstanceAccess.canAccess(player,loc)) return null;
    	GroundItem firstItem = null;
    	GroundItem firstQualifiedItem = null;
    	int numberOfSameItemsOnLoc = 0;
        for (GroundItem item : groundItems) {
            if (item == null) {
                continue;
            }
            if (item.getItem().getId() == id && item.getLocation() == loc) {
            	if (firstItem == null)
            		firstItem = item;
        		if (firstQualifiedItem == null) {
                	if ((player != null && player.isOnline()) 
                			&& ((item.getPlayer() != null && item.getPlayer().getUsername().equals(player.getUsername())) 
                					|| (item.isAdminDrop() && player.getRights() >= 2 && item.isPublic())
                					|| (!item.isAdminDrop() && item.isPublic()))) {
                		firstQualifiedItem = item;
                	}
        		}
            	numberOfSameItemsOnLoc++;
            }
        }
        if (firstItem == null)
        	return null;
        else if (numberOfSameItemsOnLoc == 1)
        	return firstItem;
        else if (firstQualifiedItem != null) {
			return firstQualifiedItem; 
		}
        return firstItem;
    }
    
    /**
     * Gets ALL ground items with the given id and player.
     *
     * @param id  The id.
     * @param player The item owner.
     * @return The ground item.
     */
	public static List<GroundItem> getAllGroundItemsFromPlayer(int id, Player player) {
        List<GroundItem> personalGroundItems = new ArrayList<GroundItem>();
        for (GroundItem item : groundItems) {
            if (item == null) {
                continue;
            }
            if (item.getItem().getId() == id && item.getPlayer() != null && item.getPlayer().getUsername().equals(player.getUsername())) {
            	personalGroundItems.add(item);
            }
        }
        return personalGroundItems;
    }
    
    /**
     * Used for replacing skillcapes with trimmed skillcapes when a player gets a 99.
     * 
     * @param groundItemsToReplace Only enter a list with grounditems that have the SAME ID (the reason we use a list is because a player could have multiple of the same item on the ground).
     * @param groundItemsReplacementId The item that replaces all grounditems of the list entered.
     */
    public static void replaceGroundItems(List<GroundItem> groundItemsToReplace, Item groundItemsReplacement, Player player) {
    	if (groundItemsToReplace == null)
    		return;
        for (GroundItem item : groundItemsToReplace) {
            if (item == null) {
                continue;
            }
            if (item.getPlayer() != null) {
                if (!groundItems.contains(item))
                	continue;
                List<Player> players = Region.getLocalPlayers(item.getLocation());
                if (item.isPublic()) {
                    for (Player pl : players) {
                        ActionSender.removeGroundItem(pl, item);
                    }
                    groundItems.remove(item);
                    item.setItem(groundItemsReplacement);
                    groundItems.add(item);
                    for (Player pl : players) {
                        ActionSender.sendGroundItem(pl, item);
                    }
                } else {
                    for (Player pl : players) {
                    	if (player.getUsername().equals(pl.getUsername())) {
                            ActionSender.removeGroundItem(player, item);
                    	}
                    }
                    groundItems.remove(item);
                    item.setItem(groundItemsReplacement);
                    groundItems.add(item);
                    for (Player pl : players) {
                    	if (player.getUsername().equals(pl.getUsername())) {
                            ActionSender.sendGroundItem(player, item);
                    	}
                    }
                }
            }
        }
    }

    /**
     * Gets the list of ground items.
     *
     * @return The list.
     */
    public static List<GroundItem> getGroundItems() {
        return groundItems;
    }

    /**
     * Refreshes the ground items when changing regions.
     *
     * @param player
     */
    public static void refresh(Player player) {
        GraveStone grave = GraveStoneManager.forName(player.getUsername());
        List<GroundItem> visibleItems = new ArrayList<GroundItem>();
        for (GroundItem item : groundItems) {
            if (item != null) {
                if ((item.getPlayer() != null && player != null && item.getPlayer() == player) || (item.getPlayer() != null && player != null && item.getPlayer().getUsername().equals(player.getUsername())) || item.isPublic() || (grave != null && grave.getItems().contains(item))) {
                	if (!item.isAdminDrop() || (item.isAdminDrop() && player.getRights() >= 2))
                        visibleItems.add(item);
                }
            }
        }
        // Rebuilds retain ground entries in the overlapping scene. Clear each old
        // entry before replaying the snapshot. Removal matches one ID at a tile,
        // so interleaving remove/add would consume newly added same-ID piles.
        for (GroundItem item : visibleItems) {
            ActionSender.removeGroundItem(player, item);
        }
        for (GroundItem item : visibleItems) {
            ActionSender.sendGroundItem(player, item);
        }
    }
}
