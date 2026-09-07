package org.dementhium.content.activity.impl;

import org.dementhium.content.activity.Activity;
import org.dementhium.content.skills.dungeoneering.Dungeoneering;
import org.dementhium.model.Mob;
import org.dementhium.model.player.Player;
import org.dementhium.net.ActionSender;

public class DungeoneeringActivity extends Activity<Mob> {
	
	/*
	 * Note that when you start adding NPC's to the dungeons, you should set 
	 * their "activity" attribute (npc.setAttribute("activity", "DungeoneeringActivity"))
	 * 
	 * 
	 * TO START THE ACTIVITY:
	 * 		if (player.getFamiliar() == null)
				ActivityManager.getSingleton().register(new DungeoneeringActivity(player));
			else
				player.sendMessage("You can't bring your familiar into the dungeon.");
	 */

	public DungeoneeringActivity(Player player) {
		super(player);
	}
	
	@Override
	public boolean initializeActivity() {
        if (getPlayer() == null) {
            this.stop();
            return false;
        }
        getPlayer().setActivity(this);
		return true;
	}

	@Override
	public boolean commenceSession() {
		Dungeoneering.startSingleDungeon(getPlayer());
		getPlayer().getSettings().setDungeoneeringSpellBook(true);
		getPlayer().refreshSpellBook();
		showDeathCount(getPlayer());
		return true;
	}

	@Override
	public boolean endSession() {
		for (Mob entity : getEntities()) {
			if (entity.isNPC())
				entity.getNPC().instantDeath();
		}
		Dungeoneering.quitDungeon(getPlayer());
		getPlayer().getSettings().setDungeoneeringSpellBook(false);
		getPlayer().refreshSpellBook();
		getPlayer().removeAttribute("inDung");
		ActionSender.sendCloseOverlay(getPlayer());
        getPlayer().setActivity(Mob.DEFAULT_ACTIVITY);
		return true;
	}
	
	@Override
    public boolean forceEnd(Player player) {
		player.teleport(3450, 3729, 0, false);
        return true;
    }
	
	@Override
	public boolean canLogout(Player player, boolean logoutButton) {
    	endSession();
        return true;
    }

    @Override
    public boolean onTeleport(Player player) {
        player.sendMessage("You can't teleport out of the dungeon.");
        return false;
    }

    @Override
    public boolean onDeath(Player player) {
    	player.teleport(Dungeoneering.getDungeonRespawnLocation(player), false);
    	player.setDungeonDeathCount(player.getDungeonDeathCount() + 1);
    	showDeathCount(player);
        return true;
    }

    @Override
    public boolean isCombatActivity(Mob mob, Mob victim, boolean sendMessages) {
        if (mob.isNPC() || victim.isNPC()) {
            return true;
        }
        return false;
    }
    
    public static void showDeathCount(Player player) {
		ActionSender.sendCloseOverlay(player);
		ActionSender.sendOverlay(player, 945);
		int childId = player.getConnection().getDisplayMode() < 2 ? 7 : 17; //TODO: fix this.
		ActionSender.sendString(player, "Deaths: "+player.getDungeonDeathCount(), 945, childId);
    }

}
