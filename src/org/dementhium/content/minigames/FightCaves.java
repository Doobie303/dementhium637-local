package org.dementhium.content.minigames;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dementhium.content.DialogueManager;
import org.dementhium.model.Item;
import org.dementhium.model.Location;
import org.dementhium.model.World;
import org.dementhium.model.misc.GroundItem;
import org.dementhium.model.misc.GroundItemManager;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.player.Player;
import org.dementhium.net.ActionSender;
import org.dementhium.tickable.Tick;

/**
 * 2011 / 637 Fight Caves on the real cave map.
 * Waves: Tz-Kih 2734, Tz-Kek 2736, Tok-Xil 2739, Yt-MejKot 2741, Ket-Zek 2743, Jad 2745.
 */
public class FightCaves {

	public static final Location OUTSIDE_OF_CAVE = Location.locate(2438, 5168, 0);
	public static final Location CAVE_START = Location.locate(2410, 5111, 0);

	private static final Location[] SPAWNS = {
			Location.locate(2400, 5088, 0),
			Location.locate(2416, 5080, 0),
			Location.locate(2430, 5088, 0),
			Location.locate(2404, 5100, 0),
			Location.locate(2424, 5100, 0)
	};

	private static final int[][] WAVES = {
			{2734},
			{2734,2734},
			{2736},
			{2736,2734},
			{2736,2734,2734},
			{2736,2736},
			{2739},
			{2739,2734},
			{2739,2734,2734},
			{2739,2736},
			{2739,2736,2734},
			{2739,2736,2734,2734},
			{2739,2736,2736},
			{2739,2739},
			{2741},
			{2741,2734},
			{2741,2734,2734},
			{2741,2736},
			{2741,2736,2734},
			{2741,2736,2734,2734},
			{2741,2736,2736},
			{2741,2739},
			{2741,2739,2734},
			{2741,2739,2734,2734},
			{2741,2739,2736},
			{2741,2739,2736,2734},
			{2741,2739,2736,2734,2734},
			{2741,2739,2736,2736},
			{2741,2739,2739},
			{2741,2741},
			{2743},
			{2743,2734},
			{2743,2734,2734},
			{2743,2736},
			{2743,2736,2734},
			{2743,2736,2734,2734},
			{2743,2736,2736},
			{2743,2739},
			{2743,2739,2734},
			{2743,2739,2734,2734},
			{2743,2739,2736},
			{2743,2739,2736,2734},
			{2743,2739,2736,2734,2734},
			{2743,2739,2736,2736},
			{2743,2739,2739},
			{2743,2741},
			{2743,2741,2734},
			{2743,2741,2734,2734},
			{2743,2741,2736},
			{2743,2741,2736,2734},
			{2743,2741,2736,2734,2734},
			{2743,2741,2736,2736},
			{2743,2741,2739},
			{2743,2741,2739,2734},
			{2743,2741,2739,2734,2734},
			{2743,2741,2739,2736},
			{2743,2741,2739,2736,2734},
			{2743,2741,2739,2736,2734,2734},
			{2743,2741,2739,2736,2736},
			{2743,2741,2739,2739},
			{2743,2741,2741},
			{2743,2743},
			{2745}
	};

	private static int waves = 0;
	private static Player currentPlayer;
	private static final List<NPC> live = new ArrayList<NPC>();
	private static int remaining = 0;

	public static int getCurrentWave() {
		return waves;
	}

	public static void setCurrentWave(int wave) {
		waves = wave;
	}

	public static void startCaves(Player player) {
		clearLive();
		setCurrentWave(0);
		currentPlayer = player;
		player.setAttribute("inFightCaves", Boolean.TRUE);
		player.setAttribute("teleblock", Integer.valueOf(999999999));
		player.teleport(CAVE_START, false);
		DialogueManager.sendDialogue(player, DialogueManager.CALM_TALK, 2617, -1,
				"You're on your own now, JalYt.",
				"Prepare to fight for your life!");
		startWave(player);
	}

	public static void startWave(Player player) {
		if (player == null) {
			return;
		}
		int currentWave = getCurrentWave();
		if (currentWave >= WAVES.length) {
			exitCave(player, 3);
			return;
		}
		int[] wave = WAVES[currentWave];
		setCurrentWave(currentWave + 1);
		ActionSender.sendConfig(player, 639, getCurrentWave());
		player.sendMessage("<col=ff0000>Wave " + getCurrentWave());
		if (wave.length == 1 && wave[0] == 2745) {
			DialogueManager.sendDialogue(player, DialogueManager.CALM_TALK, 2617, -1,
					"Look out, here comes TzTok-Jad!");
		}
		clearLive();
		remaining = 0;
		for (int i = 0; i < wave.length; i++) {
			Location loc = SPAWNS[i % SPAWNS.length];
			NPC npc = spawnCaveNpc(wave[i], loc);
			if (npc != null) {
				live.add(npc);
				remaining++;
			} else {
				player.sendMessage("Could not spawn cave npc " + wave[i]);
			}
		}
	}

	private static NPC spawnCaveNpc(int id, Location loc) {
		try {
			NPC npc = new NPC(id);
			npc.setLocation(loc);
			npc.setOriginalLocation(loc);
			npc.setDoesWalk(true);
			npc.setFaceDir(0);
			npc.setUnrespawnable(true);
			npc.setAttribute("fightcaves", Boolean.TRUE);
			npc.loadEntityVariables();
			if (!World.getWorld().getNpcs().add(npc)) {
				return null;
			}
			return npc;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static void onCaveNpcDeath(NPC npc) {
		onCaveNpcDeath(npc, currentPlayer);
	}

	public static void onCaveNpcDeath(NPC npc, Player killer) {
		if (npc == null || Boolean.TRUE.equals(npc.getAttribute("cavesCounted"))) {
			return;
		}
		int id = npc.getId();
		if (id < 2734 || id > 2746) {
			return;
		}
		Player player = killer != null ? killer : currentPlayer;
		if (player == null || !Boolean.TRUE.equals(player.getAttribute("inFightCaves"))) {
			return;
		}
		npc.setAttribute("cavesCounted", Boolean.TRUE);
		live.remove(npc);
		if (remaining > 0) {
			remaining--;
		}
		if (remaining > 0) {
			player.sendMessage("<col=ff0000>Wave " + getCurrentWave() + " - " + remaining + " left");
			return;
		}
		live.clear();
		final Player next = player;
		World.getWorld().submit(new Tick(5) {
			@Override
			public void execute() {
				stop();
				if (next != null && Boolean.TRUE.equals(next.getAttribute("inFightCaves"))) {
					startWave(next);
				}
			}
		});
	}

	public static void exitCave(Player player, int type) {
		clearLive();
		int tokkul = 251 * Math.max(1, getCurrentWave());
		if (type == 0 || type == 1) {
			giveOrDrop(player, 6529, tokkul);
			player.teleport(OUTSIDE_OF_CAVE, false);
			DialogueManager.sendDialogue(player, DialogueManager.CALM_TALK, 2617, -1,
					"Don't worry JalYt, I knew you couldn't do it.");
		} else {
			giveOrDrop(player, 6570, 1);
			giveOrDrop(player, 6529, 16064);
			player.teleport(OUTSIDE_OF_CAVE, false);
			DialogueManager.sendDialogue(player, DialogueManager.CALM_TALK, 2617, -1,
					"You have defeated TzTok-Jad, I am most impressed!",
					"Please accept this gift as a reward.");
		}
		if (player != null) {
			player.removeAttribute("teleblock");
			player.removeAttribute("inFightCaves");
		}
		setCurrentWave(0);
		currentPlayer = null;
	}

	public static void endCaves(Player player) {
		exitCave(player, 3);
	}

	public static void quitCaves(Player player) {
		exitCave(player, 0);
	}

	private static void giveOrDrop(Player player, int id, int amount) {
		if (player.getInventory().getFreeSlots() < 1) {
			GroundItemManager.createGroundItem(new GroundItem(player, new Item(id, amount),
					OUTSIDE_OF_CAVE, false, false, amount));
		} else {
			player.getInventory().addItem(id, amount);
		}
	}

	private static void clearLive() {
		Iterator<NPC> it = live.iterator();
		while (it.hasNext()) {
			NPC npc = it.next();
			it.remove();
			if (npc != null && !npc.isDead()) {
				try {
					npc.setHidden(true);
				} catch (Exception ignored) {
				}
			}
		}
		live.clear();
	}
}
