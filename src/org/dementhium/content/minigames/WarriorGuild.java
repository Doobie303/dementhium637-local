package org.dementhium.content.minigames;

import org.dementhium.model.Item;
import org.dementhium.model.Location;
import org.dementhium.model.World;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.player.Player;
import org.dementhium.tickable.Tick;
import org.dementhium.util.Misc;

/**
 * Warriors' Guild coordinator.
 * Dummy hitting, animated armour and the token shop already live under
 * {@code org.dementhium.content.activity.impl.warriorsguild} and
 * {@code WGuildTick}. This class adds the cyclops / defender room.
 */
public class WarriorGuild {

	public static final int TOKEN_ID = 8851;
	public static final int CYCLOPS_COST = 100;
	public static final Location CYCLOPS_ENTER = Location.locate(2843, 3535, 2);
	public static final Location CYCLOPS_EXIT = Location.locate(2843, 3534, 2);

	private static final int[] CYCLOPS_IDS = { 4291, 4292, 6078, 6080, 4291 };
	private static final int[] DEFENDERS = { 8844, 8845, 8846, 8847, 8848, 8849, 8850 };

	public static boolean isCyclops(int npcId) {
		for (int id : CYCLOPS_IDS) {
			if (id == npcId) {
				return true;
			}
		}
		return false;
	}

	public static boolean inCyclopsRoom(Player player) {
		if (player == null || player.getLocation() == null) {
			return false;
		}
		Location loc = player.getLocation();
		return loc.getZ() == 2 && loc.getX() >= 2837 && loc.getX() <= 2877
				&& loc.getY() >= 3534 && loc.getY() <= 3556;
	}

	public static boolean canEnterCyclops(Player player) {
		return player.getInventory().contains(new Item(TOKEN_ID, CYCLOPS_COST))
				|| player.getInventory().numberOf(TOKEN_ID) >= CYCLOPS_COST;
	}

	public static void enterCyclopsRoom(Player player) {
		if (!canEnterCyclops(player)) {
			player.sendMessage("You need at least " + CYCLOPS_COST + " warrior guild tokens to enter.");
			return;
		}
		player.getInventory().deleteItem(TOKEN_ID, CYCLOPS_COST);
		player.teleport(CYCLOPS_ENTER, false);
		player.setAttribute("wgCyclops", Boolean.TRUE);
		player.sendMessage("The cyclops will keep taking tokens while you stay in this room.");
		startTokenDrain(player);
	}

	public static void leaveCyclopsRoom(Player player) {
		player.removeAttribute("wgCyclops");
		player.teleport(CYCLOPS_EXIT, false);
	}

	private static void startTokenDrain(final Player player) {
		player.submitTick("wgTokenDrain", new Tick(100) {
			@Override
			public void execute() {
				if (player == null || !player.isOnline() || !Boolean.TRUE.equals(player.getAttribute("wgCyclops"))
						|| !inCyclopsRoom(player)) {
					stop();
					return;
				}
				if (player.getInventory().numberOf(TOKEN_ID) < 10) {
					player.sendMessage("You have run out of tokens.");
					leaveCyclopsRoom(player);
					stop();
					return;
				}
				player.getInventory().deleteItem(TOKEN_ID, 10);
				player.sendMessage("10 warrior guild tokens were taken.");
			}
		});
	}

	public static void onCyclopsDeath(Player killer, NPC npc) {
		if (killer == null || npc == null || !isCyclops(npc.getId())) {
			return;
		}
		if (!Boolean.TRUE.equals(killer.getAttribute("wgCyclops")) && !inCyclopsRoom(killer)) {
			return;
		}
		int current = killer.getAttribute("wgDefender", 0);
		if (current < 0) {
			current = 0;
		}
		if (current >= DEFENDERS.length) {
			current = DEFENDERS.length - 1;
		}
		if (Misc.random(20) == 0) {
			int drop = DEFENDERS[current];
			killer.getInventory().addDropable(new Item(drop, 1));
			killer.sendMessage("A defender falls to the ground.");
			if (current < DEFENDERS.length - 1) {
				killer.setAttribute("wgDefender", current + 1);
			}
		}
	}
}
