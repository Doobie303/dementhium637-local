package org.dementhium.model.map.path;

import org.dementhium.content.activity.impl.ImpetuousImpulses;
import org.dementhium.model.Location;
import org.dementhium.model.Mob;
import org.dementhium.model.World;
import org.dementhium.model.map.Region;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 'Mystic Flow <Steven@rune-server.org>
 */
public class ProjectilePathFinder {

    public static final int SOLID_FLAG = 256;

    public static int projectileClip(Mob mob, Location victimLoc) {
    	return projectileClip(mob, victimLoc, true);
    }
    
    public static int projectileClip(Mob mob, Location victimLoc, boolean walkPath) {
        if (mob == null || victimLoc == null) {
            return 0;
        }
        if (ImpetuousImpulses.inPuroPuro(mob.getLocation())) {
        	return 1;
        }
        if(clearPath(mob.getLocation(),victimLoc))return 1;
        Location loc = mob.getLocation();
        List<Location> available = new ArrayList<Location>();
        for (int x = -15; x <= 15; x++) {
            for (int y = -15; y <= 15; y++) {
                loc = mob.getLocation().transform(x, y, 0);
                if (clearPath(loc, victimLoc)) {
                    available.add(loc);
                }
            }
        }
        Location to = null;
        int leastDistance = -1;
        for (Location l : available) {
            if (leastDistance == -1 || l.distance(victimLoc) < leastDistance) {
                to = l;
                leastDistance = l.getDistance(victimLoc);
            }
        }
        if (to == null) {
            return 0;
        }
        PathState state = World.getWorld().doPath(new DefaultPathFinder(), mob, to.getX(), to.getY(), false, false);
        if (to != null && state != null && state.isRouteFound()) {
            if (clearPath(mob.getLocation(), victimLoc) && state.isRouteFound()) {
                return 1;
            }
            if (walkPath)
            	World.getWorld().doPath(mob, state);
            return 2;
        } else {
            if (mob.isPlayer()) {
                mob.getPlayer().sendMessage("You can't reach that.");
            }
            if (to != null && walkPath) {
                World.getWorld().doPath(new DefaultPathFinder(), mob, to.getX(), to.getY());
            }
            return 0;
        }
    }


    /** A symmetric centre-to-centre ray, including the first/last boundary. */
    public static boolean clearPath(Location from, Location to) {
        return ray(from,to,true);
    }

    /** Walking boundaries also govern melee; projectile-passable cover is not melee contact. */
    public static boolean clearMeleePath(Location from, Location to) {
        return ray(from,to,false);
    }

    private static boolean ray(Location from,Location to,boolean projectile) {
        if(from==null||to==null||from.getZ()!=to.getZ())return false;
        int x=from.getX(),y=from.getY(),z=from.getZ();
        int nx=Math.abs(to.getX()-x),ny=Math.abs(to.getY()-y);
        int sx=Integer.signum(to.getX()-x),sy=Integer.signum(to.getY()-y),ix=0,iy=0;
        if(blocked(x,y,z,projectile))return false;
        while(ix<nx||iy<ny){
            long crossX=(1L+2*ix)*ny,crossY=(1L+2*iy)*nx;
            int dx=0,dy=0;
            if(crossX<=crossY&&ix<nx){dx=sx;ix++;}
            if(crossY<=crossX&&iy<ny){dy=sy;iy++;}
            if(!boundary(x,y,z,dx,dy,projectile))return false;
            x+=dx;y+=dy;
        }
        return true;
    }

    private static boolean blocked(int x,int y,int z,boolean projectile) {
        // Region writes movement solidity separately from projectile solidity.
        int mask=projectile?0x20000:(256|0x200000|0x40000);
        return (Region.getClippingMask(x,y,z)&mask)!=0;
    }

    private static boolean boundary(int x,int y,int z,int dx,int dy,boolean projectile) {
        if(blocked(x+dx,y+dy,z,projectile))return false;
        int shift=projectile?9:0;
        int out=dx<0?(dy<0?64:dy>0?1:128):dx>0?(dy<0?16:dy>0?4:8):(dy<0?32:2);
        int in=dx<0?(dy<0?4:dy>0?16:8):dx>0?(dy<0?1:dy>0?64:128):(dy<0?2:32);
        if((Region.getClippingMask(x,y,z)&(out<<shift))!=0
                ||(Region.getClippingMask(x+dx,y+dy,z)&(in<<shift))!=0)return false;
        if(dx!=0&&dy!=0){
            // Both sides of a corner must be open; no shooting between touching walls.
            return boundary(x,y,z,dx,0,projectile)&&boundary(x,y,z,0,dy,projectile)
                &&boundary(x+dx,y,z,0,dy,projectile)&&boundary(x,y+dy,z,dx,0,projectile);
        }
        return true;
    }
}
