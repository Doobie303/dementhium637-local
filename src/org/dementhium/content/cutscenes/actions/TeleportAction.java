package org.dementhium.content.cutscenes.actions;

import org.dementhium.content.cutscenes.CutsceneAction;
import org.dementhium.model.Location;
import org.dementhium.model.Mob;
import org.dementhium.model.player.Player;
import org.dementhium.util.Misc;

/**
 * @author Steve <golden_32@live.com>
 */
public class TeleportAction extends CutsceneAction {

    private int x;
    private int y;
    private int height;
    private int distance;
    private boolean all;

    public TeleportAction(Mob mob, int delay, int toX, int toY, int height, int randomDistance, boolean all) {
        super(mob, delay);
        this.x = toX;
        this.y = toY;
        this.height = height;
        this.all = all;
        this.distance = randomDistance;
    }
    
    public TeleportAction(Mob mob, int delay, Location location, int randomDistance, boolean all) {
        super(mob, delay);
        this.x = location.getX();
        this.y = location.getY();
        this.height = location.getZ();
        this.all = all;
        this.distance = randomDistance;
    }

    @Override
    public void execute(Player[] players) {
        if (distance > 0) {
            boolean positive = Math.random() == 1;
            int offset = Misc.random(distance);
            x += positive ? offset : -offset;
            positive = Math.random() == 1;
            offset = Misc.random(distance);
            y += positive ? offset : -offset;
        }
        if ((players == null || !all) && !getMob().isNPC()) {
            getMob().getPlayer().teleport(x, y, height, false);
        } else if(getMob().isNPC()){
        	getMob().getNPC().teleport(x, y, height, false);
        }else{
            for (Player p : players) {
                p.teleport(x, y, height, false);
            }
        }
    }


}
