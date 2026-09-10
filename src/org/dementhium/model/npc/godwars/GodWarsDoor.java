package org.dementhium.model.npc.godwars;

import java.util.HashMap;
import java.util.Map;
import org.dementhium.model.Location;
import org.dementhium.model.World;
import org.dementhium.model.map.GameObject;
import org.dementhium.model.map.ObjectManager;
import org.dementhium.model.player.Player;
import org.dementhium.tickable.Tick;

/** Timed God Wars entrance crossings. */
public final class GodWarsDoor {
    private static final Map<Location,GameObject> open=new HashMap<Location,GameObject>();
    private GodWarsDoor() { }
    public static boolean handle(Player player,GameObject door) {
        Location tile=door.getLocation();
        int id=door.getId();
        if(!(id==26425&&tile.equals(Location.locate(2863,5354,2))
                ||id==26426&&tile.equals(Location.locate(2839,5295,2))
                ||id==26427&&tile.equals(Location.locate(2908,5265,0))
                ||id==26428&&tile.equals(Location.locate(2925,5332,2))))return false;
        if(player.getLocation().getZ()!=tile.getZ()||player.getLocation().distance(tile)>2
                ||!org.dementhium.net.packethandlers.WalkingHandler.canMove(player)
                ||!org.dementhium.model.instance.InstanceAccess.canInteract(player,door))return true;
        int rotation=door.getRotation()&3;
        int dx=rotation==0?-1:rotation==2?1:0,dy=rotation==1?1:rotation==3?-1:0;
        int side=(player.getLocation().getX()-tile.getX())*dx+(player.getLocation().getY()-tile.getY())*dy;
        final Location destination=side>0?tile:tile.transform(dx,dy,0);
        if(open.containsKey(tile)){player.requestClippedWalk(destination.getX(),destination.getY());return true;}
        // Keep the cache object's location/rotation intact; generic swing-door code mutates it.
        if(!ObjectManager.hideObject(door))return true;
        open.put(tile,door);
        player.requestClippedWalk(destination.getX(),destination.getY());
        World.getWorld().submit(new Tick(3) {
            @Override public void execute() {
                stop();
                if(open.get(tile)!=door)return;
                open.remove(tile);
                // A later scene owner wins over this stale restoration callback.
                if(tile.getGameObjectType(door.getType())==null)
                    ObjectManager.addCustomObject(door.getId(),tile.getX(),tile.getY(),tile.getZ(),door.getType(),door.getRotation());
            }
        });
        return true;
    }
}
