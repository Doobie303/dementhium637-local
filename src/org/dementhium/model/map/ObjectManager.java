package org.dementhium.model.map;

import org.dementhium.model.Location;
import org.dementhium.model.World;
import org.dementhium.model.map.region.DynamicRegion;
import org.dementhium.model.map.region.RegionBuilder;
import org.dementhium.model.player.Player;
import org.dementhium.net.ActionSender;
import org.dementhium.tickable.Tick;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 'Mystic Flow
 */
public class ObjectManager {
    /** Exact identity/type removal; the legacy remover searches neighbouring types. */
    public static void discardCustomObject(GameObject object) {
        Location tile = object.getLocation();
        if (tile.getGameObjectType(object.getType()) == object) {
            Region.removeObject(tile.getX(), tile.getY(), tile.getZ(), object.getType());
            for (Player player : Region.getLocalPlayers(tile))
                ActionSender.deleteObject(player, object.getId(), tile.getX(), tile.getY(), tile.getZ(), object.getType(), object.getRotation());
        }
        customObjects.remove(object);
        removedObjects.remove(object);
    }


	private static List<GameObject> customObjects = new ArrayList<GameObject>();

	private static List<GameObject> removedObjects = new ArrayList<GameObject>();

	public static GameObject addCustomObject(int objectId, int x, int y, int height, int type, int direction) {
		return addCustomObject(objectId, x, y, height, type, direction, true);
	}

	public static GameObject removeCustomObject(Location l, int type) {
		return removeCustomObject(l.getX(), l.getY(), l.getZ(), type, true);
	}

	public static GameObject removeCustomObject(int x, int y, int height, int type) {
		return removeCustomObject(x, y, height, type, true);
	}

	public static GameObject addCustomObject(Player owner, int objectId, int x, int y, int height, int type, int direction) {
		return addCustomObject(objectId, x, y, height, type, direction, true);
	}

	public static GameObject addCustomObject(GameObject object) {
		return addCustomObject(object.getId(), object.getLocation().getX(), object.getLocation().getY(), object.getLocation().getZ(), object.getType(), object.getRotation());
	}

	public static GameObject addCustomObject(int objectId, int x, int y, int height, int type, int direction, boolean refresh) {
		GameObject objectAdded = Region.addObject(objectId, x, y, height, type, direction, false);
		if (objectAdded != null && removedObjects.contains(objectAdded)) {
			removedObjects.remove(objectAdded);
		}
		if (objectAdded != null) {
			customObjects.add(objectAdded);
			if (refresh) {
				refresh(objectAdded);
			}
		}
		return objectAdded;
	}

	public static GameObject removeCustomObject(int x, int y, int height, int type, boolean refresh) {
		GameObject objectRemoved = null;
		for (int i = type - 1; i <= type + 2; i++) {
			objectRemoved = Region.removeObject(x, y, height, i);
			if (objectRemoved != null) {
				customObjects.remove(objectRemoved);
			}
			if (refresh) {
				if (objectRemoved != null) {
					if (objectRemoved.getLocation() == null)
						objectRemoved.setLocation(Location.locate(x, y, height));
					for (Player player : World.getWorld().getPlayers()) {//TODO: Check if this works >.< Region.getLocalPlayers(objectRemoved.getLocation())) {
						if (player != null && player.getLocation().withinDistance(objectRemoved.getLocation())) {
							ActionSender.deleteObject(player, objectRemoved.getId(), objectRemoved.getLocation().getX(), objectRemoved.getLocation().getY(), objectRemoved.getLocation().getZ(), objectRemoved.getType(), objectRemoved.getRotation());
						}
					}
				}
			}
			if (objectRemoved != null) {
				break;
			}
		}
		return objectRemoved;
	}

	public static void addCustomObject(Player owner, int objectId, int x, int y, int height, int type, int direction, boolean refresh) {
		GameObject objectAdded = Region.addObject(objectId, x, y, height, type, direction, false);
		if (objectAdded != null) {
			customObjects.add(objectAdded);
			if (refresh) {
				refresh(objectAdded);
			}
		}
	}

	public static void replaceObjectTemporarily(Location location, int newId, int delay) {
		replaceObjectTemporarily(location.getX(), location.getY(), location.getZ(), newId, delay);
	}

	public static void replaceObjectTemporarily(final int x, final int y, final int height, final int newId, final int delay) {
		final DynamicRegion map = RegionBuilder.getDynamicRegion(x, y);
		final long revision = map == null ? 0 : map.getChunkRevision(height, x & 63, y & 63);
		final GameObject objectRemoved = removeCustomObject(x, y, height, 10, false);
		if (objectRemoved != null) { //nothing to replace if it's null
			final int oldId = objectRemoved.getId();
			final GameObject temporary = addCustomObject(newId, x, y, height, 10, objectRemoved.getRotation());
			World.getWorld().submit(new Tick(delay) {
				public void execute() {
					if (temporary == null || Location.locate(x, y, height).getGameObjectType(temporary.getType()) != temporary || RegionBuilder.getDynamicRegion(x, y) != map || (map != null && map.getChunkRevision(height, x & 63, y & 63) != revision)) { stop(); return; }
					removeCustomObject(x, y, height, 10);
					refresh(addCustomObject(oldId, x, y, height, 10, objectRemoved.getRotation()));
					stop();
				}
			});
		}
	}

	public static void replaceObject(Location location, int newId) {
		replaceObject(location.getX(), location.getY(), location.getZ(), newId);
	}

	public static void replaceObject(final int x, final int y, final int height, final int newId) {
		GameObject objectRemoved = removeCustomObject(x, y, height, 10, false);
		if (objectRemoved != null) {
			addCustomObject(newId, x, y, height, 10, 0);
		}
	}

	public static void clearArea(Location loc, int depth) {
		List<GameObject> toRemove = new ArrayList<GameObject>();
		for (GameObject object : customObjects) {
			if (object.getLocation().distance(loc) <= depth) {
				toRemove.add(object);
			}
		}
		for (GameObject object : toRemove) {
			ObjectManager.removeCustomObject(object.getLocation().getX(), object.getLocation().getY(), object.getLocation().getZ(), object.getType(), true);
			customObjects.remove(object);
		}
	}

	public static void refresh() {
		for (GameObject object : customObjects) {
			if (object.getOwner() == null) {
				for (Player player : Region.getLocalPlayers(object.getLocation())) {
					ActionSender.sendObject(player, object);
				}
			} else {
				ActionSender.sendObject(object.getOwner(), object);
			}
		}
	}

	public static void refresh(GameObject object) {
		if (object.getOwner() == null) {
			for (Player player : Region.getLocalPlayers(object.getLocation())) {
				ActionSender.sendObject(player, object);
			}
		} else {
			ActionSender.sendObject(object.getOwner(), object);
		}
	}

	public static void refresh(Player player) {
		RegionBuilder.refreshObjects(player);
		//		for (GameObject object : removedObjects) {
			//		//	ActionSender.deleteObject(player, object.getId(), object.getLocation().getX(), object.getLocation().getY(),
					//			//		object.getLocation().getZ(), object.getType(), object.getRotation());
			//		}
		for (GameObject object : customObjects) {
			if (RegionBuilder.getDynamicRegion(object.getLocation().getX(), object.getLocation().getY()) != null) continue;
			if (object.getOwner() == null || object.getOwner() == player) ActionSender.sendObject(player, object);
		}
	}

	/** Drop bookkeeping without altering terrain or scheduling a respawn. */
	public static void forgetDynamicRegion(int id) {
		customObjects.removeIf(o -> o.getLocation().getRegionId() == id);
		removedObjects.removeIf(o -> o.getLocation().getRegionId() == id);
	}
	public static void forgetDynamicChunk(int x, int y, int plane) {
		customObjects.removeIf(o -> inChunk(o, x, y, plane));
		removedObjects.removeIf(o -> inChunk(o, x, y, plane));
	}
	private static boolean inChunk(GameObject o, int x, int y, int plane) {
		Location l = o.getLocation();
		return l.getZ() == plane && l.getX() >= x && l.getX() < x + 8 && l.getY() >= y && l.getY() < y + 8;
	}

	public static void init() {
        // Support all four Barrows entry corners; retain existing custom exits/bank.
        ObjectManager.addCustomObject(2352, 3534, 9711, 0, 10, 0, false);
        ObjectManager.addCustomObject(2352, 3534, 9677, 0, 10, 0, false);
        ObjectManager.addCustomObject(2352, 3569, 9677, 0, 10, 0, false);
		System.out.println("Loading objects...");
		ObjectManager.addCustomObject(2352, 3568, 9677, 0, 10, 0, false); //climbing rope @ barrows
		ObjectManager.addCustomObject(2352, 3568, 9711, 0, 10, 0, false); //climbing rope @ barrows
		ObjectManager.addCustomObject(2352, 3568, 9694, 0, 10, 0, false);  //climbing rope @ barrows
		ObjectManager.addCustomObject(2352, 3551, 9711, 0, 10, 0, false);  //climbing rope @ barrows
		ObjectManager.addCustomObject(1293, 2540, 2849, 0, 10, 0, false); //spirit tree
		ObjectManager.addCustomObject(2273, 2648, 9562, 0, 10, 0, false);
		ObjectManager.addCustomObject(2274, 2648, 9557, 0, 10, 0, false);
		ObjectManager.addCustomObject(2465, 2683, 9504, 0, 10, 0, false);
		ObjectManager.addCustomObject(2466, 2687, 9508, 0, 10, 0, false);
		ObjectManager.addCustomObject(12128, 3198, 3425, 0, 22, 0, false);
		ObjectManager.addCustomObject(12129, 3199, 3425, 0, 22, 0, false);
		ObjectManager.addCustomObject(12130, 3199, 3424, 0, 22, 0, false);
		ObjectManager.addCustomObject(2782, 2401, 4470, 0, 10, 0, false);
		
		//Nardah objects:
		ObjectManager.addCustomObject(39842, 3430, 2928, 0, 10, 1, false); //altar
		ObjectManager.addCustomObject(16050, 3425, 2906, 0, 10, 0, false); //'hidden' portal
		ObjectManager.addCustomObject(16050, 3419, 2905, 1, 10, 0, false); //gen store portal (nardah)
		ObjectManager.addCustomObject(16050, 3212, 3243, 1, 10, 0, false); //gen store portal (lummy)
		ObjectManager.addCustomObject(16050, 3214, 3412, 1, 10, 0, false); //gen store portal (varrock)
		ObjectManager.addCustomObject(16050, 2955, 3388, 3, 10, 0, false); //gen store portal (fally)
		ObjectManager.addCustomObject(16050, 3077, 3511, 1, 10, 0, false); //gen store portal (edge)
		//Nardah thieving stalls:
		ObjectManager.addCustomObject(4706, 3425, 2880, 0, 10, 1, false); //veg stall
		ObjectManager.addCustomObject(34384, 3425, 2876, 0, 10, 1, false); //baker stall
		ObjectManager.addCustomObject(4874, 3429, 2878, 0, 10, 0, false); //crafting stall
		ObjectManager.addCustomObject(7053, 3428, 2873, 0, 10, 0, false); //seed stall
		ObjectManager.addCustomObject(34387, 3436, 2871, 0, 10, 0, false); //fur stall
		ObjectManager.addCustomObject(17031, 3440, 2874, 0, 10, 3, false); //crossbow stall
		ObjectManager.addCustomObject(6162, 3440, 2877, 0, 10, 3, false); //custom gem stall
		ObjectManager.addCustomObject(14011, 3438, 2879, 0, 10, 2, false); //custom market stall
		ObjectManager.addCustomObject(6164, 3434, 2879, 0, 10, 3, false); //custom silver stall
		ObjectManager.addCustomObject(22772, 3432, 2872, 0, 10, 2, false); //'decorative' stall
		ObjectManager.addCustomObject(22766, 3432, 2879, 0, 10, 0, false); //'decorative' stall
		ObjectManager.addCustomObject(22765, 3435, 2876, 0, 10, 0, false); //'decorative' stall
		//Nardah quest objects:
		ObjectManager.addCustomObject(43753, 3442, 2919, 2, 10, 1, false); //shipwreck
		ObjectManager.addCustomObject(2213, 3568, 9677, 0, 10, 0, false);
		ObjectManager.addCustomObject(29943, 2339, 3691, 0, 10, 0, false);//climbing rope @ barrows
		ObjectManager.addCustomObject(2352, 3568, 9711, 0, 10, 0, false); //climbing rope @ barrows
		ObjectManager.addCustomObject(2352, 3568, 9694, 0, 10, 0, false);  //climbing rope @ barrows
		ObjectManager.addCustomObject(2352, 3551, 9711, 0, 10, 0, false);  //climbing rope @ barrows
		ObjectManager.addCustomObject(1293, 2540, 2849, 0, 10, 0, false); //spirit tree
		ObjectManager.addCustomObject(2274, 3087, 3934, 0, 10, 0, false);
		ObjectManager.addCustomObject(2465, 2683, 9504, 0, 10, 0, false);
		ObjectManager.addCustomObject(2466, 2687, 9508, 0, 10, 0, false);
		ObjectManager.addCustomObject(12128, 3198, 3425, 0, 22, 0, false);
		ObjectManager.addCustomObject(12129, 3199, 3425, 0, 22, 0, false);
		ObjectManager.addCustomObject(12130, 3199, 3424, 0, 22, 0, false);
		ObjectManager.addCustomObject(2782, 2401, 4470, 0 , 10, 0, false);
		ObjectManager.addCustomObject(718, 2608, 3144, 0 , 10, 0, false);
		ObjectManager.addCustomObject(718, 2607, 3144, 0 , 10, 0, false);
		ObjectManager.addCustomObject(3045, 2612, 3151, 0 , 10, 5, false);
		ObjectManager.addCustomObject(3045, 2612, 3150, 0 , 10, 5, false);
		ObjectManager.addCustomObject(3045, 2612, 3149, 0 , 10, 5, false);
		ObjectManager.addCustomObject(3045, 2612, 3148, 0 , 10, 5, false);
		ObjectManager.addCustomObject(3045, 2612, 3147, 0 , 10, 5, false);//stuf
		ObjectManager.addCustomObject(3045, 3212, 3439, 0 , 10, 52, false);
		ObjectManager.addCustomObject(38698, 2350, 3692, 0, 10, 5, false);
		ObjectManager.addCustomObject(38699, 2350, 3686, 0, 10, 5, false);
		ObjectManager.addCustomObject(20608, 2347, 3700, 0, 10, 10, false);
		ObjectManager.addCustomObject(27254, 2341, 3700, 0, 10, 10, false);
		ObjectManager.addCustomObject(11666, 2318, 3690, 0, 10, 15, false);
		ObjectManager.addCustomObject(2783, 2324, 3695, 0, 10, 10, false);
		ObjectManager.addCustomObject(2783, 2322, 3695, 0, 10, 10, false);
		ObjectManager.addCustomObject(14097, 2332, 3699, 0, 10, 0, false);
		ObjectManager.addCustomObject(14859, 3052, 9763, 0, 10, 10, false);
		ObjectManager.addCustomObject(14859, 3052, 9762, 0, 10, 10, false);
		ObjectManager.addCustomObject(14859, 3052, 9761, 0, 10, 10, false);
		ObjectManager.addCustomObject(14859, 3052, 9760, 0, 10, 10, false);
		
		//DONOR ZONE START
		ObjectManager.addCustomObject(1306, 3144, 5702, 0, 10, 10, false); //Magic tree
		ObjectManager.addCustomObject(1306, 3144, 5710, 0, 10, 10, false); //Magic tree
		ObjectManager.addCustomObject(11402, 3189, 5720, 0, 10, 10, false); //Bank Booth
		ObjectManager.addCustomObject(11402, 3190, 5720, 0, 10, 10, false); //Bank booth
		ObjectManager.addCustomObject(11402, 3191, 5720, 0, 10, 10, false); //Bank booth
		ObjectManager.addCustomObject(11402, 3184, 5720, 0, 10, 10, false); //Bank booth
		ObjectManager.addCustomObject(11402, 3183, 5720, 0, 10, 10, false); //Bank booth
		ObjectManager.addCustomObject(11402, 3182, 5720, 0, 10, 10, false); //Bank booth
		ObjectManager.addCustomObject(14859, 3140, 5707, 0, 10, 10, false); //Rune ore
		ObjectManager.addCustomObject(14859, 3140, 5708, 0, 10, 10, false); //Rune ore
		ObjectManager.addCustomObject(14859, 3140, 5709, 0, 10, 10, false); //Rune ore
		ObjectManager.addCustomObject(14859, 3140, 5710, 0, 10, 10, false); //Rune ore
		ObjectManager.addCustomObject(5770, 3140, 5705, 0, 10, 10, false); //Coal ore
		ObjectManager.addCustomObject(5770, 3140, 5704, 0, 10, 10, false); //Coal ore
		ObjectManager.addCustomObject(5770, 3140, 5703, 0, 10, 10, false); //Coal ore
		ObjectManager.addCustomObject(5770, 3140, 5702, 0, 10, 10, false); //Coal ore
		ObjectManager.addCustomObject(362, 3185, 5729, 0, 10, 10, false); // Barrel
		ObjectManager.addCustomObject(362, 3186, 5729, 0, 10, 10, false); // Barrel
		ObjectManager.addCustomObject(362, 3187, 5729, 0, 10, 10, false); // Barrel
		ObjectManager.addCustomObject(362, 3188, 5729, 0, 10, 10, false); // Barrel
		ObjectManager.addCustomObject(28716, 3182, 5713, 0, 10, 10, false); // Summoning
		ObjectManager.addCustomObject(11666, 3178, 5720, 0, 10, 5, false); //Furnace
		ObjectManager.addCustomObject(2782, 3177, 5717, 0, 10, 10, false); //Anvil
		ObjectManager.addCustomObject(2782, 3179, 5717, 0, 10, 10, false); //Anvil
		ObjectManager.addCustomObject(114, 3195, 5714, 0, 10, 15, false); //Cooking Range
		ObjectManager.addCustomObject(8749, 3174, 5711, 0, 10, 15, false); //Altar
		
		
		
		//DONOR ZONE END
		ObjectManager.addCustomObject(10733, 116, 5191, 0 , 10, 0, false);
		ObjectManager.addCustomObject(12260, 72, 1985, 0 , 10, 0, false);
		ObjectManager.addCustomObject(15478, 71, 1969, 0 , 10, 0, false);
		ObjectManager.addCustomObject(49745, 120, 5156, 0 , 10, 0, false);
		ObjectManager.addCustomObject(7288, 113, 5157, 0 , 10, 0, false);
		ObjectManager.addCustomObject(49766, 113, 5158, 0 , 10, 0, false);
		ObjectManager.addCustomObject(49770, 113, 5159, 0 , 10, 0, false);
		ObjectManager.addCustomObject(49776, 113, 5160, 0 , 10, 0, false);
		ObjectManager.addCustomObject(53125, 116, 5154, 0 , 10, 0, false);
		ObjectManager.addCustomObject(7289, 113, 5162, 0 , 10, 0, false);
		ObjectManager.addCustomObject(7316, 67, 1933, 0 , 10, 0, false);
		ObjectManager.addCustomObject(49345, 2886, 3460, 0 , 10, 0, false);
//thef stall
		ObjectManager.addCustomObject(4874, 2651, 3305, 0 , 10, 0, false);
		//ObjectManager.addCustomObject(4875, 2331, 3694, 0 , 10, 0, false);
		ObjectManager.addCustomObject(4876, 2649, 3305, 0 , 10, 0, false);
		//ObjectManager.addCustomObject(4877, 2329, 3694, 0 , 10, 0, false);
		//ObjectManager.addCustomObject(4878, 2328, 3694, 0 , 10, 0, false);


		//theif stall end 36972 - 29943
	
		//pray altar
		ObjectManager.addCustomObject(47120, 2335, 3682, 0 , 10, 0, false);
		//prayer altar end
		ObjectManager.addCustomObject(11402, 2852, 2951, 0, 10, 0, false);
                ObjectManager.addCustomObject(5960, 2848, 2955, 0, 22, 0, false);
                ObjectManager.addCustomObject(11402, 2853, 2951, 0, 10, 5, false);
                ObjectManager.addCustomObject(11402, 2328, 3686, 0, 10, 5, false);
                ObjectManager.addCustomObject(11402, 2328, 3687, 0, 10, 5, false);
                ObjectManager.addCustomObject(11402, 2328, 3688, 0, 10, 5, false);
                ObjectManager.addCustomObject(11402, 2328, 3689, 0, 10, 5, false);
                ObjectManager.addCustomObject(11402, 2328, 3690, 0, 10, 5, false);
                ObjectManager.addCustomObject(11402, 2328, 3691, 0, 10, 5, false);
                ObjectManager.addCustomObject(11402, 2328, 3692, 0, 10, 5, false);
                //ObjectManager.addCustomObject(29943, 2262, 3316, 0, 10, 0, false);
		       //clan wars portal
                //ObjectManager.addCustomObject(2273, 2264, 3316 , 0 , 10, 0, false);
                ObjectManager.addCustomObject(563, 2856, 2960 , 0 , 10, 0, false);
                ObjectManager.addCustomObject(36972, 2340, 3687 , 0 , 10, 10, false);

 //Hunter Burrow
		ObjectManager.addCustomObject(1276, 2410, 3531, 0 , 10, 0, false);
		//ObjectManager.addCustomObject(2274, 2708, 3155, 0 , 10, 0, false);//uuu
		ObjectManager.addCustomObject(1276, 2412, 3531, 0 , 10, 0, false);
		ObjectManager.addCustomObject(1281, 2414, 3531, 0 , 10, 0, false);
		ObjectManager.addCustomObject(1281, 2416, 3531, 0 , 10, 0, false);
		ObjectManager.addCustomObject(1308, 2419, 3531, 0 , 10, 0, false);
		ObjectManager.addCustomObject(1307, 2421, 3531, 0 , 10, 0, false);
		ObjectManager.addCustomObject(1309, 2423, 3531, 0 , 10, 0, false);
		ObjectManager.addCustomObject(1306, 2426, 3531, 0 , 10, 0, false);
		ObjectManager.addCustomObject(11402, 2413, 3522, 0 , 10, 0, false);//donor area
		ObjectManager.addCustomObject(11402, 2412, 3522, 0 , 10, 0, false);//donor area
		ObjectManager.addCustomObject(11402, 2411, 3522, 0 , 10, 0, false);//donor area
		ObjectManager.addCustomObject(28716, 2413, 3527, 0 , 10, 0, false);//donor area
		//Mining Rocks
		ObjectManager.addCustomObject(5780, 2427, 3529, 0 , 10, 0, false);
		ObjectManager.addCustomObject(5776, 2427, 3528, 0 , 10, 0, false);
		ObjectManager.addCustomObject(5773, 2427, 3527, 0 , 10, 0, false);
		ObjectManager.addCustomObject(5770, 2427, 3526, 0 , 10, 0, false);
		ObjectManager.addCustomObject(3044, 2426, 3523, 0 , 10, 2, false);
		ObjectManager.addCustomObject(5784, 2425, 3522, 0 , 10, 0, false);
		ObjectManager.addCustomObject(5782, 2424, 3521, 0 , 10, 0, false);
		ObjectManager.addCustomObject(14859, 2423, 3521, 0 , 10, 0, false);
		ObjectManager.addCustomObject(14859, 2422, 3521, 0 , 10, 0, false);
		org.dementhium.content.home.HomeHub.spawnObjects();
		System.out.println("Loaded " + customObjects.size() + " objects.");

		//		Region.addObject(4411, 2418, 3123, 0, 22, 0, true);
		//		Region.addObject(4411, 2418, 3125, 0, 22, 0, true);
		//		Region.addObject(4411, 2419, 3125, 0, 22, 0, true);
		//		Region.addObject(4411, 2419, 3123, 0, 22, 0, true);
		//
		//		Region.addObject(36691, 2400, 3108, 0, 22, 0, true);
	}

	public static void removeObjectTemporarily(Location location, int delay, final int type, final int dir) {
		final int x = location.getX();
		final int y = location.getY();
		final int height = location.getZ();
		GameObject objectRemoved = removeCustomObject(x, y, height, type, true);
		if (objectRemoved != null) { //nothing to replace if it's null
			final int oldId = objectRemoved.getId();
			World.getWorld().submit(new Tick(delay) {
				public void execute() {
					addCustomObject(oldId, x, y, height, type, dir);
					stop();
				}
			});
		}

	}

	public static void addObjectTemporarily(final int x, final int y, final int height, int rotation, final int type, int id, int ticks) {
		addCustomObject(id, x, y, height, type, rotation);
		World.getWorld().submit(new Tick(ticks) {
			public void execute() {
				stop();
				removeCustomObject(x, y, height, type, true);
			}
		});

	}
}
