package org.dementhium.task.impl;

import org.dementhium.content.activity.impl.CastleWarsActivity;
import org.dementhium.content.activity.impl.DuelActivity;
import org.dementhium.content.minigames.FightCaves;
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
    public Player getPlayer() { return player; }

    /**
     * Constructs a new {@code SessionLogoutTask} {@code Object}.
     * @param player The player logging out.
     */
    public SessionLogoutTask(Player player) {
        this.player = player;
    }

    @Override
    public void execute() {
        if (!org.dementhium.model.instance.InstanceManager.getSingleton().isCycleThread()) {
            World.getWorld().submitTask(this); return;
        }
        if (World.getWorld().getPlayers().get(player.getIndex())!=player
                && World.getWorld().getLobbyPlayers().get(player.getIndex())!=player) return;
        org.dementhium.content.misc.PvpSystem.depart(player);
        org.dementhium.model.instance.InstanceAccess.depart(player,true);
    	if (player.getActivity() instanceof CastleWarsActivity) {
    		CastleWarsActivity cwars = (CastleWarsActivity) player.getActivity();
    		cwars.canLogout(player, false);
    	}
    	if (Boolean.TRUE.equals(player.getAttribute("inFightCaves"))) {
    		FightCaves.quitCaves(player);
    	}
        World.getWorld().unregister(player);
    }

}
