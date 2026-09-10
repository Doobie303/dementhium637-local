package org.dementhium.content.misc;

import org.dementhium.cache.format.CacheObjectDefinition;
import org.dementhium.model.map.GameObject;
import org.dementhium.model.map.ObjectManager;
import org.dementhium.model.player.Player;

/**
 * Handles the opening and closing of doors.
 *
 * @author Emperor
 * @author Mystic Flow'
 * @author Steve
 */
public class DoorManager {
	
	/*
	 * TODO:
	 * 
	 * Update doors when player enters region with doors.
	 */

    /**
     * Handles a door.
     *
     * @param player The player.
     * @param object The door gameobject.
     * @return {@code True} if the object was a door and got handled, {@code false} if not.
     */
    public static boolean handleDoor(Player player, final GameObject object) {
        if(org.dementhium.model.npc.godwars.GodWarsDoor.handle(player,object))return true;
        String name = object.getDefinition().getName().toLowerCase();
        if ((!name.contains("door") && !name.contains("gate")) || name.toLowerCase().contains("trapdoor")) {
            return false;
        }
        GameObject secondDoor = getSecondDoor(object);
        boolean doubleGate = secondDoor != null;
        boolean open = object.getDefinition().options[0].equalsIgnoreCase("open") || object.getDefinition().options[0].equalsIgnoreCase("search");
        if (doubleGate && open) {
        	System.out.println("DoorManager, case 1 (doubleGate && open).");
            return openDoubleDoor(player, object, secondDoor);
        } else if (open) {
        	System.out.println("DoorManager, case 2 (open).");
            return openDoor(player, object);
        } else if (!open && !doubleGate) {
        	System.out.println("DoorManager, case 3 (NOT open && NOT doubleGate).");
        	closeDoor(player, object);
        }
        //TODO: closing for double doors (= case 4).
        return false;
    }
    
    private static boolean openDoor(Player player, final GameObject object) {
        int direction = object.getRotation() + 1;
        int transformX = 0, transformY = 0;
        switch (object.getRotation() % 4) {
            case 0:
                transformX--;
                break;
            case 1:
                transformY++;
                break;
            case 2:
                transformX++;
                break;
            case 3:
                transformY--;
                break;
        }
        ObjectManager.removeCustomObject(object.getLocation().getX(), object.getLocation().getY(), object.getLocation().getZ(), object.getType());
        object.setLocation(object.getLocation().transform(transformX, transformY, 0));
        GameObject toReplace = new GameObject(findNextDoor(object.getId()), object.getLocation(), object.getType(), direction);
        ObjectManager.addCustomObject(toReplace);
        return true;
    }

    private static boolean openDoubleDoor(Player player, final GameObject door, final GameObject secondDoor) {
    	openOneOfTheDoubleDoors(player, door);
    	openOneOfTheDoubleDoors(player, secondDoor);
        return true;
    }

    private static void openOneOfTheDoubleDoors(Player player, GameObject object) {
        int direction = object.getRotation();
        int transformX = 0, transformY = 0;
        switch (object.getId() % 4) {
            case 2:
                direction += 1;
                break;
            case 1:
                direction -= 1;
                break;
            case 3:
                direction += 4;
            case 4:
                direction += 1;
                break;
        }
        switch (object.getRotation() % 4) {
            case 0:
                transformX--;
                break;
            case 1:
                transformY++;
                break;
            case 2:
                transformX++;
                break;
            case 3:
                transformY--;
                break;
        }
        ObjectManager.removeCustomObject(object.getLocation().getX(), object.getLocation().getY(), object.getLocation().getZ(), object.getType());
        object.setLocation(object.getLocation().transform(transformX, transformY, 0));
        GameObject toReplace = new GameObject(findNextDoor(object.getId()), object.getLocation(), object.getType(), direction);
        ObjectManager.addCustomObject(toReplace);
    }
    
    public static boolean closeDoor(Player player, final GameObject object) {
        int direction = object.getRotation() - 1;
        if (direction < 0) {
            direction *= -1;
        }
        int transformX = 0, transformY = 0;
        switch (object.getRotation() % 4) {
            case 0:
                transformY--;
                break;
            case 1:
                transformX++;
                break;
            case 2:
                transformY++;
                break;
            case 3:
                transformX--;
                break;
        }
        ObjectManager.removeCustomObject(object.getLocation().getX(), object.getLocation().getY(), object.getLocation().getZ(), object.getType());
        object.setLocation(object.getLocation().transform(transformX, transformY, 0));
        GameObject toReplace = new GameObject(findNextDoor(object.getId()), object.getLocation(), object.getType(), direction);
        ObjectManager.addCustomObject(toReplace);
        return true;
    }

    /*public static boolean closeDoubleDoor(Player player, GameObject object, GameObject secondDoor) {
        closeOneOfTheDoubleDoors(player, object);
        closeOneOfTheDoubleDoors(player, secondDoor);
        return true;
    }

    private static boolean closeOneOfTheDoubleDoors(Player player, GameObject object) {
        int direction = object.getRotation();
        if (direction < 0) {
            direction *= -1;
        }
        switch (object.getRotation() % 4) {
            case 2:
                direction--;
                break;
            case 1:
                direction--;
                break;
            case 3:
                direction++;
                break;
            case 4:
                direction++;
                break;
        }
        int transformX = 0, transformY = 0;
        switch (object.getRotation() % 4) {
            case 0:
                transformY--;
                break;
            case 1:
                transformX++;
                break;
            case 2:
                transformY--;
                break;
            case 3:
                transformX--;
                break;
        }
        ObjectManager.removeCustomObject(object.getLocation().getX(), object.getLocation().getY(), object.getLocation().getZ(), object.getType());
        object.setLocation(object.getLocation().transform(transformX, transformY, 0));
        GameObject toReplace = new GameObject(findNextDoor(object.getId()), object.getLocation(), object.getType(), direction);
        ObjectManager.addCustomObject(toReplace);
        return true;
    }*/

    private static GameObject getSecondDoor(GameObject object) {
        GameObject o = null;
        for (int i = -2; i < 3; i++) {
            for (int j = -2; j < 3; j++) {
                o = object.getLocation().transform(i, j, 0).getGameObject(object.getLocation().transform(i, j, 0));
                if (o != null && !object.equals(o) && (o.getDefinition().getName().toLowerCase().contains("door") || o.getDefinition().getName().toLowerCase().contains("gate"))
                		&& !o.getDefinition().getName().toLowerCase().contains("trapdoor")) {
                	return o;
                }
            }
        }
        //return object.getLocation().transform(x, y, 0).getGameObject();
        return null;
    }

    private static int findNextDoor(int id) {
        CacheObjectDefinition doorDef = CacheObjectDefinition.forId(id);
        String option = doorDef.options[0];
        if (option.equalsIgnoreCase("open")) {
            for (int i = 0; i < 4; i++) {
                CacheObjectDefinition def = CacheObjectDefinition.forId(id + i);
                if (def.options != null && def.options[0] != null && def.options[0].equalsIgnoreCase("close") && def.name.equalsIgnoreCase(doorDef.name)) {
                    return id + i;
                }
            }
            for (int i = 0; i > -4; i--) {
                CacheObjectDefinition def = CacheObjectDefinition.forId(id + i);
                if (def.options != null && def.options[0] != null && def.options[0].equalsIgnoreCase("close") && def.name.equalsIgnoreCase(doorDef.name)) {
                    return id + i;
                }
            }
        } else if (option.equalsIgnoreCase("close")) {
            for (int i = 0; i > -4; i--) {
                CacheObjectDefinition def = CacheObjectDefinition.forId(id + i);
                if (def.options != null && def.options[0] != null && def.options[0].equalsIgnoreCase("open") && def.name.equalsIgnoreCase(doorDef.name)) {
                    return id + i;
                }
            }
            for (int i = 0; i < 4; i++) {
                CacheObjectDefinition def = CacheObjectDefinition.forId(id + i);
                if (def.options != null && def.options[0] != null && def.options[0].equalsIgnoreCase("open") && def.name.equalsIgnoreCase(doorDef.name)) {
                    return id + i;
                }
            }
        }
        return 0;
    }

}
