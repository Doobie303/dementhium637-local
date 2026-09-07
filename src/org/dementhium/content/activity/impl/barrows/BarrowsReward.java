package org.dementhium.content.activity.impl.barrows;

import org.dementhium.model.Container;
import org.dementhium.model.player.Player;
import org.dementhium.net.ActionSender;

/**
 * Barrows chest reward window. Same packet pattern as duel spoils (script 149),
 * on interface 155 instead of 634. Items move to inventory when the interface closes.
 */
public class BarrowsReward {

	public static final int INTERFACE = 364;
	public static final int CHILD = 1;
	public static final int CONTAINER = 141;

	public static void open(Player player, Container rewards) {
		if (player == null || rewards == null) {
			return;
		}
		if (player.interfaceItems != null) {
			player.getInventory().addAllDropable(player.interfaceItems);
			player.interfaceItems = null;
		}
		ActionSender.sendInterface(player, INTERFACE);
		ActionSender.sendAMask(player, 1026, INTERFACE, CHILD, 0, 35);
		ActionSender.sendClientScript(player, 149,
				new Object[] { "", "", "", "", "", Integer.valueOf(-1),
						Integer.valueOf(0), Integer.valueOf(6), Integer.valueOf(6),
						Integer.valueOf(CONTAINER),
						Integer.valueOf(INTERFACE << 16 | CHILD) },
				"noooobsssss");
		ActionSender.sendItems(player, CONTAINER, rewards, false);
		player.interfaceItems = rewards;
		player.sendMessage("You search the chest...");
	}
}
