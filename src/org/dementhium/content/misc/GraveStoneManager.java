package org.dementhium.content.misc;

import java.util.HashMap;
import java.util.Map;

import org.dementhium.content.interfaces.ItemsKeptOnDeath;
import org.dementhium.io.PlayerLoader;
import org.dementhium.model.Container;
import org.dementhium.model.Item;
import org.dementhium.model.Mob;
import org.dementhium.model.World;
import org.dementhium.model.mask.Animation;
import org.dementhium.model.misc.GroundItem;
import org.dementhium.model.misc.GroundItemManager;
import org.dementhium.model.misc.IconManager;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.player.Player;
import org.dementhium.net.ActionSender;
import org.dementhium.util.Misc;

/**
 * The gravestone manager, handles the creating; removing and updating of gravestones.
 *
 * @author Emperor
 */
public class GraveStoneManager {

    /**
     * The gravestone appearing animation.
     */
    private static final Animation APPEAR_ANIMATION = Animation.create(7394);

    /**
     * A mapping holding the currently active gravestones.
     */
    private static final Map<String, GraveStone> GRAVESTONES = new HashMap<String, GraveStone>();

    /**
     * Appends a player's death.
     *
     * @param player The player.
     * @return {@code True} if the player created a grave stone, {@code false} if not.
     */
    public static boolean appendDeath(Player player, Mob killer) {
        if (player.getAttribute("deathItemsApplied",false)) return false;
        player.setAttribute("deathItemsApplied",true);
        killer = player.getAttribute("pvpDeathKiller",killer);
        Mob lastHitter = player.getAttribute("pvpDeathLastHitter",player.getCombatExecutor().getLastAttacker()); //last hit could be 0 (in multi)..
        if (lastHitter != null && lastHitter.isFamiliar()) lastHitter = lastHitter.getFamiliar().getOwner();
        if (killer != null && killer.isFamiliar()) killer = killer.getFamiliar().getOwner();
        if (lastHitter != null && killer != null) {
            if (killer.isPlayer() && lastHitter.isPlayer() && !lastHitter.getPlayer().getUsername().equals(killer.getPlayer().getUsername())) {
            	if (!killer.getPlayer().isOnline())
            		killer = lastHitter;
            }
        }
    	boolean keepItems = player.getRights() >= 2;
    	boolean rewardItems = false;
		for(String name : PlayerLoader.superMods) {
			if (player.getUsername().equals(name)) {
				if (player.getKeepItemsOnDeath() == false
						&& player.isInWilderness()
								&& killer != null && killer.isPlayer() && killer.getPlayer().getRights() < 2)
					keepItems = false;
				if (player.getRewardItemsDroppedOnDeath() != null && killer != null && killer.isPlayer() && killer.getPlayer().getRights() < 2)
					rewardItems = true;
			}
		}
        if (!keepItems) {
            boolean canCreateGraveStone = !World.getWorld().getAreaManager().getAreaByName("CorporealBeast").contains(player.getLocation())
            	&& !player.isInWilderness()
            	&& player.getRights() < 2;
            Container[] keptItems = ItemsKeptOnDeath.getDeathContainers(player);
            player.getInventory().getContainer().clear();
            player.getEquipment().getContainer().clear();
            for (Item item : keptItems[0].toArray()) {
                if (item != null) {
                    player.getInventory().addItem(item);
                }
            }
            player.getEquipment().refresh();
            player.getInventory().refresh();
            player.getSkills().raiseMaximumLifePoints(0);
            if (canCreateGraveStone) {
                if (GRAVESTONES.containsKey(player.getUsername())) {
                    GraveStone g = GRAVESTONES.get(player.getUsername());
                    g.demolish(player, "Your previous gravestone has collapsed.");
                    g.stop();
                }
                NPC npc = World.getWorld().register(
                        GraveStone.getNPCId(player.getSettings().getGraveStone()),
                        player.getLocation());
                npc.setDoesWalk(false);
                npc.animate(APPEAR_ANIMATION);
                GraveStone grave = new GraveStone(player.getUsername(), player.getSettings().getGraveStone(), npc);
                IconManager.iconOnCoordinate(player, player.getLocation(), 1, 65536);
                for (Item item : keptItems[1].toArray()) {
                    if (item != null) {
                        GroundItem groundItem = new GroundItem(player, item, player.getLocation(), false, player.getRights() >= 2, GroundItemManager.groundItemIndex++);
                        grave.getItems().add(groundItem);
                        GroundItemManager.createGroundItem(groundItem, grave.getTicks());
                    }
                }
                int id = player.getConnection().getDisplayMode() < 2 ? 548 : 746;
                ActionSender.sendInterfaceConfig(player, id, id == 548 ? 12 : 164, true);
                ActionSender.sendInterfaceConfig(player, id, id == 548 ? 13 : 165, true);
                ActionSender.sendInterfaceConfig(player, id, id == 548 ? 14 : 166, true);
                World.getWorld().submit(grave);
                GRAVESTONES.put(player.getUsername(), grave);
                if (player.getFamiliar() != null) {
                	player.getFamiliar().dismiss(true, null);
                }
                return true;
            }
            if (killer != null && killer.isPlayer()) {
	            for (Item item : keptItems[1].toArray()) {
	                if (item != null) {
	                	item = getGroundItemToDropAsPkLoot(item);
	                	if (!item.getDefinition().isDropable()) {
	                		continue;
	                	}
	                	if (!item.getDefinition().isTradeable()) {
	                		 GroundItemManager.createGroundItem(new GroundItem(player, item, player.getLocation(), false, player.getRights() >= 2, GroundItemManager.groundItemIndex++));
	                		 continue;
        						
	                	}
	                	GroundItemManager.createGroundItem(new GroundItem(killer.getPlayer(), item, player.getLocation(), false, killer.getPlayer().getRights() >= 2, GroundItemManager.groundItemIndex++));
	                }
	            }
	            if (player.getFamiliar() != null) {
	            	player.getFamiliar().dismiss(true, killer.getPlayer());
	            }
            } else {
    	        for (Item item : keptItems[1].toArray()) {
    	            if (item != null) {
    	            	if (!item.getDefinition().isDropable()) {
                    		continue;
                    	}
    	                GroundItemManager.createGroundItem(new GroundItem(player, item, player.getLocation(), false, player.getRights() >= 2, GroundItemManager.groundItemIndex++));
    	            }
    	        }
	            if (player.getFamiliar() != null) {
	            	player.getFamiliar().dismiss(true, null);
	            }
            }
        }
        
        if (rewardItems) { //superMods only
            for (Item item : player.getRewardItemsDroppedOnDeath().toArray()) {
                if (item != null) {
                	if (!item.getDefinition().isDropable()) {
                		continue;
                	}
                	GroundItemManager.createGroundItem(new GroundItem(killer.getPlayer(), item, player.getLocation(), false, killer.getPlayer().getRights() >= 2, GroundItemManager.groundItemIndex++));
                }
            }
            player.setRewardItemsDroppedOnDeath(null);
        }
        if (killer != null && killer.isPlayer() && killer != player) {
            // Optional reward/UI errors must never interrupt the rest of death cleanup.
            try {
                sendDeathMessage(player, killer, lastHitter);
                killer.getPlayer().handlePkStatistics(player,
                    lastHitter != null && lastHitter.isPlayer() ? lastHitter.getPlayer() : null);
            } catch (RuntimeException failure) {
                System.err.println("[PvP reward failure] " + player.getUsername());
                failure.printStackTrace();
            }
        }
        return false;
    }

    /**
     * Gets a grave stone from the gravestones mapping.
     *
     * @param user The username.
     * @return The gravestone, or {@code null} if the player didn't have a gravestone.
     */
    public static GraveStone forName(String user) {
        return GRAVESTONES.get(user);
    }

    /**
     * @return the gravestones
     */
    public static Map<String, GraveStone> getGravestones() {
        return GRAVESTONES;
    }
    
    public static Item getGroundItemToDropAsPkLoot(Item item) {
    	int itemId = item.getId();
    	if (itemId >= 15441 && itemId <= 15444) //coloured whips
    		return new Item(4151, item.getAmount());
    	if (itemId >= 15701 && itemId <= 15704) //coloured dark bows
    		return new Item(11235, item.getAmount());
    	
    	/*
    	 * Barrows:
    	 */
    	if (itemId >= 4708 && itemId <= 4738 
    			&& itemId % 2 == 0) //meaning it's an even number
    		return new Item(itemId + 152 + (2*(itemId - 4708)), item.getAmount());
    	if (itemId >= 4745 && itemId <= 4759 && itemId % 2 != 0)
    		return new Item(itemId + 211 + (2*(itemId - 4745)), item.getAmount());
    	
    	//an example of how this method can be used for untradeables e.g.:
    	switch (itemId) {
    	case 13886:
    	return new Item(995, 25000000);
    
        case 13892:
		return new Item(995, 20000000);
	
        case 13896:
        return new Item(995, 15000000);
        
        case 13904:
        return new Item(995, 30000000);
        
        case 13889:
        return new Item(995, 25000000);
        
        case 13895:
        return new Item(995, 15000000);
        
        case 13901:
        return new Item(995, 35000000);
        	
        case 13907:
       	return new Item(995, 5000000);
       	
        case 13872:
        return new Item(995, 15000000);
        
        case 13875:
        return new Item(995, 15000000);
        
        case 13878:
        return new Item(995, 8000000);
        
        case 13860:
        return new Item(995, 10000000);
        
        case 13863:
        return new Item(995, 15000000);
        
        case 13866:
        return new Item(995, 10000000);
        
        case 13869:
        return new Item(995, 25000000);
    }
    	return item;
    }
    
    public static void sendDeathMessage(Player player, Mob killer, Mob lastHitter) {
        if (lastHitter != null && killer.isPlayer() && lastHitter.isPlayer() && !lastHitter.getPlayer().getUsername().equals(killer.getPlayer().getUsername())) {
        		lastHitter.getPlayer().sendMessage("You killed "+player.getDisplayName()+" but "+killer.getPlayer().getDisplayName()+" did more damage so they got the drop.");
        		killer.getPlayer().sendMessage(lastHitter.getPlayer().getDisplayName()+" killed "+player.getDisplayName()+" but you did more damage so you got the drop.");
        }
        else if (killer.isPlayer()) {
        	killer.getPlayer().sendMessage(getRandomDeathMessage(player));
        }
    }
    
    /**
     * Gets a random message to display when someone kills someone else
     *
     * @param player The player who died.
     */
    public static String getRandomDeathMessage(Player player) {
    	String name = player.getDisplayName();
    	int random = Misc.random(15); //15 = message below
    	String message = "You have killed "+name+".";
    	switch (random) {
    	case 0:
    		message = "Well done, you've pwned "+name+".";
    		break;
    	case 1:
    		message = name+" was clearly no match for you.";
    		break;
    	case 2:
    		message = "Remember this day with pride: on this day you slew "+name+".";
    		break;
    	case 3:
    		message = "You have defeated "+name+" in battle.";
    		break;
    	case 4:
    		message = name+" has fallen before your mighty mightiness.";
    		break;
    	case 5:
    		message = "Your gods smile on you as "+name+" meets "+(player.getAppearance().getGender() == 1 ? "her" : "his") +" demise.";
    		break;
    	case 6:
    		message = "It's official: you are far more awesome than "+name+" is.";
    		break;
    	case 7:
    		message = "You have wiped the floor with "+name+".";
    		break;
    	case 8:
    		message = name+" was no match for your unchecked power.";
    		break;
    	case 9:
    		message = "Ooh, "+name+" just dropped death, and it's all thanks to you!";
    		break;
    	case 10:
    		message = "Perhaps, one day, "+name+" will have the courage to face you again.";
    		break;
    	case 11:
    		message = "You rock, "+name+" clearly does not.";
    		break;
    	case 12:
    		message = "Let all warriors learn from the fate of "+name+" and fear you.";
    		break;
    	case 13:
    		message = "You just made "+name+" lose the game.";
    		break;
    	case 14:
    		message = "You have proven your superiority over "+name+".";
    		break;
    	}
    	return message;
    }
    public static int PVPItems[] = {
    	379, 373, 385, 391, 15272, 2434, 6685,
         11235, 11732, 11335, 11283, 11284, 
        8850, 10551, 1079, 1093, 1113, 1127, 
        1147, 1163, 1185, 1201, 1303, 1319, 1333, 1347, 1373, 2615, 
        2617, 2619, 2621, 2623, 2625, 2627, 2629, 3101, 3202, 3476, 
        3477, 7336, 7342, 7348, 7354, 7360, 8464, 8466, 8468, 8470, 
        8472, 8474, 8476, 8478, 8480, 8482, 8484, 8486, 8488, 8490, 
        8492, 8494, 8714, 8716, 8718, 8720, 8722, 8724, 8726, 8728, 
        8730, 8732, 8734, 8736, 8738, 8740, 8742, 8744, 9185, 9185, 
        10286, 10288, 10290, 10292, 10294, 10667, 10670, 10673, 10676, 10679, 
        10679, 10705, 10707, 10704, 10705, 10706, 10708, 10798, 10800, 1149, 
        1187, 1187, 5698, 1377, 5698, 1377, 1434, 1434, 1540, 3140, 
        3204, 3204, 4087, 4585, 4587, 4587, 5699, 7158, 1712, 1712, 
        1712, 1712, 2491, 2491, 2491, 2497, 2497, 2497, 2503, 2503, 
        2503, 861, 861, 861, 4131, 7461, 7461, 7462, 6916, 6918, 
        6920, 6922, 6924, 6914, 13672, 13673, 13674, 13675, 14497, 14499, 
        14501, 11728, 4151, 4151, 14490, 14492, 14494, 10828, 
        3751, 3751, 11128, 3749, 3749, 3751, 3751, 3753, 3753, 3755, 
        3755, 1725, 1725, 4675, 4675, 3842, 3842, 3840, 3840, 3843, 
        2412, 2413, 2414, 13899, 4089, 4091, 4093, 
        4095, 4097, 4099, 4101, 4103, 4105, 4107, 4109, 4111, 4113, 
        4115, 4117, 4119, 3385, 3387, 3389, 3391, 3393, 3394, 13867,
        10828, 11730, 10887, 5698, 5698, 6524, 
        6570, 4153, 6535, 6536, 6537, 6538, 6568, 3122, 3122, 3122, 
        6809, 6809, 6737, 13672, 13673, 13674, 13675, 14600, 14602, 14603, 
        14605,  13899, 14497, 14499, 14501, 14490, 14492, 
        14494, 14592, 14593, 14594, 14479, 6585, 6585, 6737, 3140, 4087, 
        4585, 13858, 13861, 13870, 13876, 13873,
        13861, 13864, 13858, 10499,
        
        1727, 1729, 841, 839, 843, 845, 577, 579, 1381, 1383, 1385, 1387, 1131, 1133, 1097, 1169, 1061, 1323, 1325, 1329,1309,1311,1315, 1293, 1295, 1299, 1335, 1339, 1343, 1267, 1269, 1273, 1101, 1105, 1109, 1067, 1069, 1071, 1081, 1083, 1085, 1191, 1193, 1197, 1154, 1157, 1159, 1137, 1141, 1143, 1121, 1181, 2550,
        1725, 1731, 1681, 847, 849, 851, 853, 1065, 1099, 1135, 1301, 1287, 1211, 1430,1112, 1271, 1183, 1333, 1319, 1303, 1213, 1373, 1347, 1432, 1113, 1185, 1275, 1147, 4675, 11126, 1247, 4091, 4093, 4101, 4103, 4111, 4113, 14499, 14494
        ,1731,10366, 855, 857, 859, 861, 1333, 1319, 1303, 1213, 1373, 1347, 1432, 1163, 1113, 1127, 1079, 1201, 1185, 1275, 1147, 1683, 4675, 13006, 13003, 6739, 1305, 4587, 3755, 10564, 10589, 4153, 6809, 6918, 6920, 6922, 6568, 6129, 4131, 1247, 6139, 6524, 3751, 6131, 14497,14501, 14490, 14492
        ,6585, 4214, 6566,6733,6731,6735,6737, 1187, 4087, 11133, 3204, 4151, 14479
        ,8055,8056,8057,11181,11182, 13899 ,
        2293, 315, 325, 347,351, 333, 329,361, 379,2323, 2331, 2327,2003,2297,1896,1899,1897,1891,1893, 373, 383, 391, 7946, 361};


    public static int PVPItems(){
        return PVPItems[(int)(Math.random() * PVPItems.length)];
    }
}
