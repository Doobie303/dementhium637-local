package org.dementhium.task.impl;

import org.dementhium.content.activity.impl.CastleWarsActivity;
import org.dementhium.content.activity.impl.DuelActivity;
import org.dementhium.model.World;
import org.dementhium.model.player.Player;
import org.dementhium.task.Task;
import org.dementhium.util.SQL;

/**
 * @author Graham Edgecombe
 * @author Emperor (X-Log fix)
 */
public class SessionLogoutTask implements Task {

	/**
	 * The player.
	 */
    private final Player player;

    /**
     * Constructs a new {@code SessionLogoutTask} {@code Object}.
     * @param player The player logging out.
     */
    public SessionLogoutTask(Player player) {
        this.player = player;
    }

    @Override
    public void execute() {
    	if (player.getActivity() instanceof CastleWarsActivity) {
    		CastleWarsActivity cwars = (CastleWarsActivity) player.getActivity();
    		cwars.canLogout(player, false);
    	}
        World.getWorld().unregister(player);
    }

}
