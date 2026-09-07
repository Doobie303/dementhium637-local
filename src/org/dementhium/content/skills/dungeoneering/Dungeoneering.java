package org.dementhium.content.skills.dungeoneering;

import org.dementhium.event.EventListener.ClickOption;
import org.dementhium.model.Location;
import org.dementhium.model.Mob;
import org.dementhium.model.map.GameObject;
import org.dementhium.model.map.region.RegionBuilder;
import org.dementhium.model.player.Player;

/**
 * @author Ediremix
 */
public class Dungeoneering {
	
	public static int[][] dungArray = new int[201][2]; //players, x and y
	
	private static String[] players = new String[201];
	
	private static boolean[][] open = new boolean[201][15]; //players, number of doors
	
	public static void startSingleDungeon(Player player) {
		int slot = 0; //move slot to player
		boolean empty = true;
		for (int i = 0; i < 200; i++) {
			if (players[i] == null) {
				players[i] = player.getUsername();
				player.setDungeonIndex(i);
				slot = i;
				empty = false;
				break;
			}
		}
		if (empty) {
			player.sendMessage("Sorry, but all dungeoneering map regions are in use, please come");
			player.sendMessage("back another time.");
			return;
		}
		int x = dungArray[slot][0];
		int y = dungArray[slot][1];
		int var = 0;
		while (var < 15) {
			open[slot][var] = false;
			var++;
		}
		RegionBuilder.copyAllPlanesMap(RegionBuilder.getRegion(96), 
			RegionBuilder.getRegion(5136), RegionBuilder.getRegion(x), 
			RegionBuilder.getRegion(y), 2);
		player.teleport(Mob.DEFAULT, false);
		player.teleport(getDungeonRespawnLocation(player), false);
	}
	
	public static void quitDungeon(Player player) {
		int slot = player.getDungeonIndex();
		players[slot] = null;
		player.setDungeonIndex(-1);
		int x = (dungArray[slot][0] - 80);
		int y = (dungArray[slot][1] - 80);
		RegionBuilder.destroyAllPlanesMap(RegionBuilder.getRegion(x), 
				RegionBuilder.getRegion(y), 16);
		player.teleport(3450, 3729, 0, false);
	}
	
	public static Location getDungeonRespawnLocation(Player player) {
		int slot = player.getDungeonIndex();
		int x = dungArray[slot][0];
		int y = dungArray[slot][1];
		return Location.locate(x + 8, y + 8, player.getLocation().getZ());
	}
	
	public static boolean handleObject(Player player, GameObject object, ClickOption actionId) {
		return handleObject(player, object, object.getId(), actionId, true);
	}
	
	public static boolean handleObject(Player player, GameObject object, int objectId, ClickOption actionId, boolean performAction) {
		if (object == null)
			performAction = false;
		switch (objectId) {
		case 49463:
		case 50343:
			if (performAction)
				openDoor(player, object);
			return true;
		}
		return false;
	}
	
	public static void openDoor(Player player, GameObject obj) {
		int slot = player.getDungeonIndex();
		int x = dungArray[slot][0];
		int y = dungArray[slot][1];
		int objX = obj.getLocation().getX();
		int objY = obj.getLocation().getY();
		int deltaX = (objX-(x+8));
		int deltaY = (objY-(y+8));
		Location loc = player.getLocation();
		int pX = (loc.getX() - (x+8));
		int pY = (loc.getY() - (y+8));
		
		//System.out.println("X: "+deltaX+" Y: "+deltaY);
		
		if (deltaX == -1  && deltaY == -8) {
			if (open[slot][0] == false) {
				if (pX == -1  && pY == -7) {
					Location Ploc = player.getLocation();
					RegionBuilder.copyAllPlanesMap(RegionBuilder.getRegion(64), 
							RegionBuilder.getRegion(4384), RegionBuilder.getRegion(x), 
							RegionBuilder.getRegion(y-16), 2);
					player.teleport(2980, 9910, 0, false);
					player.teleport(Ploc, false);
					open[slot][0] = true;
				}
			} else if (open[slot][0]) {
				System.out.println("X: "+pX+" Y: "+pY);
				if (pX == -1  && pY == -7) {
					player.teleport(loc.getX(), (loc.getY()-3), 0, false);
				}
			}
		} else if (deltaX == -1  && deltaY == -24) {
			if (open[slot][1] == false) {
				if (pX == -1  && pY == -23) {
					Location Ploc = player.getLocation();
					RegionBuilder.copyAllPlanesMap(RegionBuilder.getRegion(64), 
							RegionBuilder.getRegion(4448), RegionBuilder.getRegion(x), 
							RegionBuilder.getRegion(y-32), 2);
					player.teleport(2980, 9910, 0, false);
					player.teleport(Ploc, false);
					open[slot][1] = true;
				}
			} else if (open[slot][1]) {
				System.out.println("X: "+pX+" Y: "+pY);
				if (pX == -1  && pY == -23) {
					player.teleport(loc.getX(), (loc.getY()-3), 0, false);
				}
			}
		} else if (deltaX == -8  && deltaY == -1) {
			if (open[slot][2] == false) {
				if (pX == -7  && pY == -1) {
					Location Ploc = player.getLocation();
					RegionBuilder.copyAllPlanesMap(RegionBuilder.getRegion(80), 
							RegionBuilder.getRegion(4448), RegionBuilder.getRegion(x-16), 
							RegionBuilder.getRegion(y), 2);
					player.teleport(2980, 9910, 0, false);
					player.teleport(Ploc, false);
					open[slot][2] = true;
				}
			} else if (open[slot][2]) {
				System.out.println("X: "+pX+" Y: "+pY);
				if (pX == -7  && pY == -1) {
					player.teleport((loc.getX()-3), (loc.getY()), 0, false);
				}
			}
		} else if (deltaX == -1  && deltaY == -9) {
			System.out.println("X: "+pX+" Y: "+pY);
			if (pX == -1  && pY == -10) {
				player.teleport((loc.getX()), (loc.getY()+3), 0, false);
			}
		} else if (deltaX == -1  && deltaY == -25) {
			if (open[slot][1] == false) {
				if (pX == -1  && pY == -26) {
					Location Ploc = player.getLocation();
					RegionBuilder.copyAllPlanesMap(RegionBuilder.getRegion(64), 
							RegionBuilder.getRegion(4448), RegionBuilder.getRegion(x), 
							RegionBuilder.getRegion(y-32), 2);
					player.teleport(2980, 9910, 0, false);
					player.teleport(Ploc, false);
					open[slot][1] = true;
				}
			} else if (open[slot][1]) {
				System.out.println("X: "+pX+" Y: "+pY);
				if (pX == -1  && pY == -26) {
					player.teleport(loc.getX(), (loc.getY()+3), 0, false);
				}
			}
		} else if (deltaX == -8  && deltaY == -33) {
			if (open[slot][3] == false) {
				if (pX == -7  && pY == -33) {
					Location Ploc = player.getLocation();
					RegionBuilder.copyAllPlanesMap(RegionBuilder.getRegion(64), 
							RegionBuilder.getRegion(4416), RegionBuilder.getRegion(x-16), 
							RegionBuilder.getRegion(y-32), 2);
					player.teleport(2980, 9910, 0, false);
					player.teleport(Ploc, false);
					open[slot][3] = true;
				}
			} else if (open[slot][3]) {
				System.out.println("X: "+pX+" Y: "+pY);
				if (pX == -7  && pY == -33) {
					player.teleport(loc.getX()-3, (loc.getY()), 0, false);
				}
			}
		} else if (deltaX == 7  && deltaY == -33) {
			if (open[slot][4] == false) {
				if (pX == 6  && pY == -33) {
					Location Ploc = player.getLocation();
					RegionBuilder.copyAllPlanesMap(RegionBuilder.getRegion(64), 
							RegionBuilder.getRegion(4416), RegionBuilder.getRegion(x+16), 
							RegionBuilder.getRegion(y-32), 2);
					player.teleport(2980, 9910, 0, false);
					player.teleport(Ploc, false);
					open[slot][4] = true;
				}
			} else if (open[slot][4]) {
				System.out.println("X: "+pX+" Y: "+pY);
				if (pX == 6  && pY == -33) {
					player.teleport(loc.getX() + 3, (loc.getY()), 0, false);
				}
			}
		}
		/*else if (deltaX == -1  && deltaY == -40){
				You shall not pass :)
		}*/
	}
	
	
}
