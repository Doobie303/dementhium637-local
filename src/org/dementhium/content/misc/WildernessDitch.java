package org.dementhium.content.misc;

import org.dementhium.event.EventListener;
import org.dementhium.event.EventManager;
import org.dementhium.model.Location;
import org.dementhium.model.map.GameObject;
import org.dementhium.model.mask.Animation;
import org.dementhium.model.player.Player;
import org.dementhium.net.ActionSender;

/**
 * @author 'Mystic Flow <Steven@rune-server.org>
 */
public class WildernessDitch extends EventListener {

    public static final Animation CROSS_ANIMATION = Animation.create(6703);

    public void cross(Player player) {
        boolean north = player.getLocation().getY() > 3520;
    	if (north || player.getSkills().getTotalLevel() >= 100) {
    		player.closeAll(true, true);
            player.setAttribute("busy", Boolean.TRUE);
            player.setAttribute("stallRegion", Boolean.TRUE);

            int y = north ? -3 : 3;
            int dir = north ? 2 : 0;
            //Animation.create(6132)
            player.forceMovement(CROSS_ANIMATION, player.getLocation().getX(), y < 0 ? 3520 : 3523, 33, 60, dir, 2, true);
    	} else {
    		player.removeAttribute("teleportingToWild");
       		ActionSender.sendInterface(player, 382);
    	}
    }


    public boolean objectOption(Player player, int objectId, GameObject gameObject, Location location, ClickOption option) {
        if (option == ClickOption.FIRST) {
            if (gameObject.getId() >= 1440 && gameObject.getId() <= 1444) {
                cross(player);
                return true;
            }
        }
        return false;
    }

    @Override
    public void register(EventManager manager) {
        for (int i = 1440; i <= 1444; i++) {
            manager.registerObjectListener(i, this);
        }
    }

}
