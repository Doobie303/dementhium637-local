package org.dementhium.content.minigames;

import java.util.ArrayList;
import java.util.List;

import org.dementhium.content.DialogueManager;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.player.Player;
import org.dementhium.util.Misc;
import org.dementhium.model.World;
import org.dementhium.model.map.region.RegionBuilder;
import org.dementhium.model.Location;
import org.dementhium.net.ActionSender;
import org.dementhium.model.misc.GroundItem;
import org.dementhium.model.misc.GroundItemManager;
import org.dementhium.model.Item;


/**
 *
 * @author Wildking72
 *
 */

public class FightCaves {

	//Config - 639
	
	public static final Location OUTSIDE_OF_CAVE = Location.locate(2439, 5169, 0);
	public static Location SPAWN_COORDS = Location.locate(0, 0, 0); //Undefined

	private Player p;
	
	public FightCaves(Player player) {
		this.p = player;
	}
	
	private static final int[][] WAVES = {
			{2734}
			,{2734,2734}
			,{2736}
			,{2736,2734}
			,{2736,2734,2734}
			,{2736,2736}
			,{2739}
			,{2739,2734}
			,{2739,2734,2734}
			,{2739,2736}
			,{2739,2736,2734}
			,{2739,2736,2734,2734}
			,{2739,2736,2736}
			,{2739,2739}
			,{2741}
			,{2741,2734}
			,{2741,2734,2734}
			,{2741,2736}
			,{2741,2736,2734}
			,{2741,2736,2734,2734}
			,{2741,2736,2736}
			,{2741,2739}
			,{2741,2739,2734}
			,{2741,2739,2734,2734}
			,{2741,2739,2736}
			,{2741,2739,2736,2734}
			,{2741,2739,2736,2734,2734}
			,{2741,2739,2736,2736}
			,{2741,2739,2739}
			,{2741,2741}
			,{2743}
			,{2743,2734}
			,{2743,2734,2734}
			,{2743,2736}
			,{2743,2736,2734}
			,{2743,2736,2734,2734}
			,{2743,2736,2736}
			,{2743,2739}
			,{2743,2739,2734}
			,{2743,2739,2734,2734}
			,{2743,2739,2736}
			,{2743,2739,2736,2734}
			,{2743,2739,2736,2734,2734}
			,{2743,2739,2736,2736}
			,{2743,2739,2739}
			,{2743,2741}
			,{2743,2741,2734}
			,{2743,2741,2734,2734}
			,{2743,2741,2736}
			,{2743,2741,2736,2734}
			,{2743,2741,2736,2734,2734}
			,{2743,2741,2736,2736}
			,{2743,2741,2739}
			,{2743,2741,2739,2734}
			,{2743,2741,2739,2734,2734}
			,{2743,2741,2739,2736}
			,{2743,2741,2739,2736,2734}
			,{2743,2741,2739,2736,2734,2734}
			,{2743,2741,2739,2736,2736}
			,{2743,2741,2739,2739}
			,{2743,2741,2741}
			,{2743,2743}
			,{2745}
	};
	
	private static int waves = 0;
	
	public static int getCurrentWave() {
		return waves;
	}
	
	public static void setCurrentWave(int wave) {
		waves = wave;
	}
	
	
	public static void createCave() {
		RegionBuilder.findEmptyMap(70, 70);
		RegionBuilder.copyAllPlanesMap(
			RegionBuilder.getRegion(2367), RegionBuilder.getRegion(5055), 
			RegionBuilder.getRegion(2427), RegionBuilder.getRegion(5119), 8
		);
	}
	
	public static void destroyCave() {
		RegionBuilder.destroyAllPlanesMap(2427, 5119, 16);
	}

	public static void startWave(Player player) {
		int currentWave = getCurrentWave();
		if (getCurrentWave() > WAVES.length) {
			exitCave(player, 3);
			return;
		}
		setCurrentWave(getCurrentWave() + 1);
		if (currentWave > 0) {
			if (currentWave == WAVES.length) {
				DialogueManager.sendDialogue(player, DialogueManager.CALM_TALK, 2617, -1, "Look out, here comes TzTok-Jad!");
				return;
			}
			ActionSender.sendConfig(player, 639, currentWave);
		}
		for(int id : WAVES[currentWave-1]) {
			if(id == 2736)
				new NPC(id, player.getLocation());
			else if (id == 2745)
				new NPC(id, player.getLocation());
			else
				new NPC(id, player.getLocation());
		}
	}
	
	public static void startCaves(Player player) {
		if (player.getAttribute("familiar") != null) {
			DialogueManager.sendDialogue(player, DialogueManager.CALM_TALK, 2617, -1, "No Kimit-Zil in the pits! This is a fight for YOU, not your friends!");
			return;
		}
		//player.requestWalk(30, 30);
		createCave();
		player.teleport(30, 30, 0, false);
		player.setAttribute("teleblock", 999999999);
		DialogueManager.sendDialogue(player, DialogueManager.CALM_TALK, 2617, -1, "You're on your own now, " + player.getUsername() + ".", "Prepare to fight for your life!");
		startWave(player);
	}
	
	public static void exitCave(Player player, int type) {
		int tokkul = 251 * getCurrentWave();
		if (type == 0) { //Quit
			if (player.getInventory().getFreeSlots() < 27) {
				GroundItemManager.createGroundItem(new GroundItem(player, new Item(6529, tokkul), OUTSIDE_OF_CAVE, false, false, tokkul));
			} else {
				player.getInventory().addItem(6529, tokkul);
			}
			player.teleport(OUTSIDE_OF_CAVE, false);
			DialogueManager.sendDialogue(player, DialogueManager.CALM_TALK, 2617, -1, "Don't wory " + player.getUsername() + ", I knew you couldn't do it.");
			setCurrentWave(0);
		} else if (type == 1) { //Died
			if (player.getInventory().getFreeSlots() < 28) {
				GroundItemManager.createGroundItem(new GroundItem(player, new Item(6529, tokkul), OUTSIDE_OF_CAVE, false, false, tokkul));
			} else {
				player.getInventory().addItem(6529, tokkul);
			}
			player.teleport(OUTSIDE_OF_CAVE, false);
			DialogueManager.sendDialogue(player, DialogueManager.CALM_TALK, 2617, -1, "Don't wory " + player.getUsername() + ", I knew you couldn't do it.");
			setCurrentWave(0);
		} else { //Victorious
			if (player.getInventory().getFreeSlots() < 27) {
				GroundItemManager.createGroundItem(new GroundItem(player, new Item(6570, 1), OUTSIDE_OF_CAVE, false, false, tokkul));
				GroundItemManager.createGroundItem(new GroundItem(player, new Item(6529, 16064), OUTSIDE_OF_CAVE, false, false, tokkul));
			} else {
				player.getInventory().addItem(6529, 16064);
				player.getInventory().addItem(6570, 1);
			}
			player.teleport(OUTSIDE_OF_CAVE, false);
			DialogueManager.sendDialogue(player, DialogueManager.CALM_TALK, 2617, -1, "You have defeated TzTok-Jad, I am most impressed!", "Please accept this gift as a reward.");
			setCurrentWave(0);
		}
		destroyCave();
	}

	/**
	 * When the player wins or quits
	 */
	public static void endCaves(Player player) {
		player.teleport(2439, 5169, 0, false);
		player.removeAttribute("teleblock");
		DialogueManager.sendDialogue(player, DialogueManager.CALM_TALK, 2617, -1, "You have defeated TzTok-Jad, I am most impressed!", "Please accept this gift as a reward.");
	}
	public static void quitCaves(Player player) {
		player.teleport(2439, 5169, 0, false);
		player.removeAttribute("teleblock");
		DialogueManager.sendDialogue(player, DialogueManager.CALM_TALK, 2617, -1, "Don't wory " + player.getUsername() + ", I knew you couldn't do it.");
	}
}