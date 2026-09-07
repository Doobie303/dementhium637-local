package org.dementhium.model.misc;

import org.dementhium.io.PlayerLoader;
import org.dementhium.model.Entity;
import org.dementhium.model.Item;
import org.dementhium.model.Location;
import org.dementhium.model.map.Position;
import org.dementhium.model.player.Player;

/**
 * Represents a single ground item.
 *
 * @author Emperor
 */
public class GroundItem extends Entity {

    /**
     * If the item is global.
     */
    private boolean isPublic;

    /**
     * If the item is removed.
     */
    private boolean removed;

    /**
     * The amount of update ticks.
     */
    private int updateTicks;

    /**
     * The item's location.
     */
    private final Position location;

    /**
     * The item's location.
     */
    private Item item;

    /**
     * The player who dropped the item.
     */
    private final Player player;
    
    /**
     * The player who dropped the item (String in case he logs out and logs back in).
     */
    private final boolean adminDrop;
 
    /**
     * To separate same items dropped on the same location and have other equal attributes.
     */
    private int index;
    
    /**
     * The constructor.
     *
     * @param item     The item.
     * @param location The location.
     * @param isPublic The isPublic flag.
     */
    public GroundItem(Player player, Item item, Location location,
                      boolean isPublic, boolean adminDrop, int index) {
        this.item = item;
        this.location = Position.create(location.getX(), location.getY(), location.getZ());
        this.isPublic = isPublic;
        this.player = player;
        this.adminDrop = adminDrop;
        this.index = index;
    }

    public GroundItem(Player player, Item item, int x, int y, int z,
                      boolean isPublic, boolean adminDrop, int index) {
        this.item = item;
        this.location = Position.create(x, y, z);
        this.isPublic = isPublic;
        this.player = player;
        this.adminDrop = adminDrop;
        this.index = index;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public Location getLocation() {
        return Location.locate(location.getX(), location.getY(), location.getZ());
    }

    public Item getItem() {
        return item;
    }
    
    public void setItem(Item item) {
        this.item = item;
    }

    public Player getPlayer() {
        return player;
    }
    
    public boolean isAdminDrop() {
    	boolean allowPublicAdminDrop = false;
		for(String name : PlayerLoader.superMods) {
			if(getPlayer() != null && getPlayer().getUsername().equals(name)) {
				allowPublicAdminDrop = true;
			}
		}
        return allowPublicAdminDrop == false ? adminDrop : false;
    }

    public void setRemoved(boolean removed) {
        this.removed = removed;
    }

    public boolean isRemoved() {
        return removed;
    }

    public void setUpdateTicks(int updateTicks) {
        this.updateTicks = updateTicks;
    }

    public int getUpdateTicks() {
        return updateTicks;
    }
    
    public void setIndex(int i) {
        this.index = i;
    }

    public int getIndex() {
        return index;
    }
}