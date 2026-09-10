package org.dementhium.model.combat;

import java.util.LinkedList;
import java.util.List;
import org.dementhium.content.minigames.FightCaves;

import org.dementhium.model.Location;
import org.dementhium.model.Mob;
import org.dementhium.model.World;
import org.dementhium.model.definition.WeaponInterface;
import org.dementhium.model.map.Directions;
import org.dementhium.model.map.path.DefaultPathFinder;
import org.dementhium.model.map.path.PrimitivePathFinder;
import org.dementhium.model.map.path.ProjectilePathFinder;
import org.dementhium.model.npc.NPC;

/**
 * A class handling the combat movement.
 * @author Emperor
 *
 */
public class CombatMovement {

	/**
	 * Checks if the mob can attack the victim, if not the mob will walk to the victim.
	 * @param mob The attacking mob.
	 * @param other The victim.
	 * @param type The combat type used.
	 * @return {@code True} if the mob can proceed its attack, {@code false} if not.
	 */
	public static boolean combatFollow(Mob mob, Mob other, CombatType type) {
        if(mob instanceof org.dementhium.model.npc.impl.Nex
                && !((org.dementhium.model.npc.impl.Nex)mob).canPursue(other))return false;
        if(mob instanceof org.dementhium.model.npc.encounter.EncounterNPC)return ((org.dementhium.model.npc.encounter.EncounterNPC)mob).follow(other,type);
		if (mob.getLocation().getZ() != other.getLocation().getZ()) {
            mob.getCombatExecutor().reset();
            return false;
        }
        mob.turnTo(other, false);
		int distance = mob.getLocation().getDistance(other.getLocation());
		boolean caveOpponent = FightCaves.isCaveOpponent(mob, other);
		if (distance > 17 && !caveOpponent && !org.dementhium.model.npc.impl.Nex.arenaPair(mob,other)
                && !org.dementhium.model.npc.godwars.GodWarsNPC.roomPair(mob,other)) {
			mob.getCombatExecutor().reset();
			return false;
		}
        if(mob.isNPC()&&!mob.isFamiliar()) {
            if(mob instanceof org.dementhium.model.npc.godwars.GodWarsNPC) {
                org.dementhium.model.npc.godwars.GodWarsNPC g=(org.dementhium.model.npc.godwars.GodWarsNPC)mob;
                if(!g.isBoss()&&g.getRoom()!=null&&g.getRoom().overlap(g,mob.getLocation())>0){
                    mob.getWalkingQueue().reset();g.getRoom().repositionFollower(g,other);return false;
                }
            }
            boolean melee=type==CombatType.MELEE||type==CombatType.DEFAULT;
            boolean ready=melee?hasMeleeContact(mob,other,mob.size()>1||other.size()>1)
                :org.dementhium.model.npc.encounter.EncounterNPC.gap(mob,other)<=type.getDistance()
                    &&org.dementhium.model.npc.godwars.GodWarsAction.clear(mob,other);
            if(ready&&!standingOn(mob.getLocation(),other.getLocation(),mob.size(),other.size())){mob.getWalkingQueue().reset();return true;}
            followNpc(mob,other);return false;
        }
        boolean melee=type==CombatType.MELEE||type==CombatType.DEFAULT;
        if(melee) {
            if(canMelee(mob,other))return true;
            // Preserve the existing player moving-melee allowance, with movement walls.
            if(mob.isPlayer()&&mob.getWalkingQueue().isMoving()
                    &&ProjectilePathFinder.clearMeleePath(mob.getLocation(),other.getLocation())) {
                if(distance<=other.size()+2&&other.getWalkingQueue().isMoving())return true;
                if(distance<=other.size()+3&&other.getWalkingQueue().isRunningMoving())return true;
            }
        } else {
            int maximumDistance=type.getDistance();
            if(type==CombatType.RANGE&&mob.isPlayer()) {
                RangeWeapon weapon=RangeWeapon.get(mob.getPlayer().getEquipment().getSlot(3));
                if(weapon!=null)maximumDistance=weapon.getAttackRange(mob.getPlayer().getSettings().getCombatStyle()==WeaponInterface.STYLE_LONG_RANGE);
            }
            if(distance>0&&distance<=maximumDistance) {
                mob.getWalkingQueue().reset();
                return canSendProjectile(mob,other);
            }
        }

		if (mob.isPlayer()) {
			if (other.getWalkingQueue().getLastLocation() != null) {
				Location victimLocation = getNearest(mob, other)[1];
				int followX = victimLocation.getX();
				int followY = victimLocation.getY();
				World.getWorld().doPath(new DefaultPathFinder(), mob, followX, followY);
			}
		} else {
			Location[] locs = getNearest(mob, other);
			Location sourceLocation = locs[0];
			Location victimLocation = locs[1];
			int mobX = sourceLocation.getX();
			int mobY = sourceLocation.getY();
			int victimX = victimLocation.getX();
			int victimY = victimLocation.getY();
			victimX -= (mobX - mob.getLocation().getX());
			victimY -= (mobY - mob.getLocation().getY());
			int followX = victimX - (mob.getLocation().getRegionX() - 6) * 8;
			int followY = victimY - (mob.getLocation().getRegionY() - 6) * 8;
			mob.getWalkingQueue().reset();
			mob.getWalkingQueue().addClippedWalkingQueue(followX, followY);
		}
		return false;
	}

	/**
	 * Checks if the mob can send a projectile to the victim.
	 * @param mob The mob.
	 * @param other The victim.
	 * @return {@code True} if so, {@code false} if not.
	 */
	private static boolean canSendProjectile(Mob mob, Mob other) {
		int clip = ProjectilePathFinder.projectileClip(mob, other.getLocation());
		if (clip == 0) {
			mob.getCombatExecutor().reset();
			return false;
		}
		return clip == 1;
	}


	/**
	 * Checks if the mob can use melee combat to attack the victim.
	 *
	 * @param mob    The mob.
	 * @param victim The victim.
	 * @return {@code True} if so, {@code false} if not.
	 */
	public static boolean canMelee(Mob mob, Mob victim) {
        boolean contact=hasMeleeContact(mob,victim,mob.size()>1||victim.size()>1);
        if(contact)mob.getWalkingQueue().reset();
        return contact;
    }

    public static boolean hasMeleeContact(Mob source,Mob victim,boolean diagonal) {
        if(source.getLocation().getZ()!=victim.getLocation().getZ()
                ||standingOn(source.getLocation(),victim.getLocation(),source.size(),victim.size()))return false;
        for(Location a:getInternTiles(source,source.size()==1))for(Location b:getInternTiles(victim,victim.size()==1)){
            int dx=Math.abs(a.getX()-b.getX()),dy=Math.abs(a.getY()-b.getY());
            if(dx<=1&&dy<=1&&(diagonal||dx+dy==1)&&ProjectilePathFinder.clearMeleePath(a,b))return true;
        }
        return false;
    }

    /** One local step, with cardinal fallback. Does not search a route around cover. */
    public static void followNpc(Mob mob,Mob victim) {
        mob.getWalkingQueue().reset();
        if(standingOn(mob.getLocation(),victim.getLocation(),mob.size(),victim.size())) {
            int x=mob.getLocation().getX(),y=mob.getLocation().getY(),vx=victim.getLocation().getX(),vy=victim.getLocation().getY();
            int[][] exits={{-1,0,x+mob.size()-vx},{1,0,vx+victim.size()-x},{0,-1,y+mob.size()-vy},{0,1,vy+victim.size()-y}};
            java.util.Arrays.sort(exits,(a,b)->Integer.compare(a[2],b[2]));
            for(int[] exit:exits)if(tryNpcStep(mob,exit[0],exit[1]))return;
            return;
        }
        if(mob instanceof org.dementhium.model.npc.impl.Nex){((org.dementhium.model.npc.impl.Nex)mob).followTarget(victim);return;}
        // Overlapping coordinate intervals need no movement on that axis. Choosing
        // an arbitrary perimeter corner instead can make an NPC creep along cover.
        int dx=approachAxis(mob.getLocation().getX(),mob.size(),victim.getLocation().getX(),victim.size());
        int dy=approachAxis(mob.getLocation().getY(),mob.size(),victim.getLocation().getY(),victim.size());
        if(tryChaseStep(mob,victim,dx,dy))return;
        if(dx!=0&&dy!=0){if(tryChaseStep(mob,victim,dx,0))return;if(tryChaseStep(mob,victim,0,dy))return;}
        if(mob instanceof org.dementhium.model.npc.godwars.GodWarsNPC) {
            org.dementhium.model.npc.godwars.GodWarsNPC g=(org.dementhium.model.npc.godwars.GodWarsNPC)mob;
            if(!g.isBoss()&&g.getRoom()!=null)g.getRoom().repositionFollower(g,victim);
        }
    }

    private static int approachAxis(int from,int size,int target,int targetSize) {
        return target>from+size-1?1:target+targetSize-1<from?-1:0;
    }

    private static boolean tryChaseStep(Mob mob,Mob victim,int dx,int dy) {
        if(standingOn(mob.getLocation().transform(dx,dy,0),victim.getLocation(),mob.size(),victim.size()))return false;
        return tryNpcStep(mob,dx,dy);
    }

    public static boolean tryNpcStep(Mob mob,int dx,int dy) {
        if(dx==0&&dy==0||!org.dementhium.net.packethandlers.WalkingHandler.canMove(mob))return false;
        Location from=mob.getLocation(),to=from.transform(dx,dy,0);
        if(!npcStepClear(mob,from,to))return false;
        mob.requestClippedWalk(to.getX(),to.getY());return true;
    }

    /** Pure edge check, also used by the bounded room-follower route search. */
    public static boolean npcStepClear(Mob mob,Location from,Location to) {
        if(from.getZ()!=to.getZ()||from.getDistance(to)>1)return false;
        // Managed admission already checks size() from this origin. Applying that
        // whole-body check at every body tile would extend the footprint twice.
        if(!org.dementhium.model.instance.InstanceAccess.canWalk(mob,to))return false;
        boolean managed=org.dementhium.model.instance.InstanceAccess.owner(mob)!=null;
        if(mob instanceof org.dementhium.model.npc.godwars.GodWarsNPC) {
            org.dementhium.model.npc.godwars.GodWarsNPC g=(org.dementhium.model.npc.godwars.GodWarsNPC)mob;
            if(!g.isBoss()&&g.getRoom()!=null&&g.getRoom().overlap(g,to)>g.getRoom().overlap(g,from))return false;
        }
        for(int x=0;x<mob.size();x++)for(int y=0;y<mob.size();y++) {
            Location a=from.transform(x,y,0),b=to.transform(x,y,0);
            if(!ProjectilePathFinder.clearMeleePath(a,b)||!managed&&!org.dementhium.model.instance.InstanceAccess.canWalk(mob,b))return false;
            if(mob instanceof org.dementhium.model.npc.encounter.EncounterNPC&&!((org.dementhium.model.npc.encounter.EncounterNPC)mob).contains(b))return false;
            if(mob instanceof org.dementhium.model.npc.godwars.GodWarsNPC&&!((org.dementhium.model.npc.godwars.GodWarsNPC)mob).getRoom().contains(b))return false;
            if(mob instanceof org.dementhium.model.npc.impl.Nex&&!((org.dementhium.model.npc.impl.Nex)mob).containsArena(b))return false;
        }
        // WalkingQueue.reset uses the origin's viewport, never the nearest footprint tile's sector.
        return true;
    }

	/**
	 * Gets the locations to calculate the path with.
	 *
	 * @param source The source.
	 * @param victim The victim.
	 * @return {@code An array of 2 locations:
	 *         <br> 1 = the source's calculated location, 2 = the victim's calculated location.
	 */
	public static Location[] getNearest(Mob source, Mob victim) {
		Location victimLocation = victim.getLocation();
		Location sourceLocation = source.getLocation();
		boolean sizeOne = source.size() < 2 && victim.size() < 2;
		List<Location> mobTiles = getInternTiles(source, source.size() < 2);
		List<Location> victimTiles = getExternTiles(victim, sizeOne);
		int currentDistance = 999; //Random high number so we override first in the loop.
		for (Location sl : mobTiles) {
			for (Location vl : victimTiles) {
				int distance = sl.getDistance(vl);
				if (distance < currentDistance && !standingOn(source.getLocation(), vl, source.size(), 1)) {
					currentDistance = distance;
					victimLocation = vl;
					sourceLocation = sl;
				}
			}
		}
		return new Location[]{sourceLocation, victimLocation};
	}

	/**
	 * Checks if the mobs are standing on eachother.
	 * @param source The first mob.
	 * @param victim The second mob.
	 * @return {@code True} if so.
	 */
	public static boolean standingOn(Location sl, Location vl, int firstSize, int secondSize) {
		int x = sl.getX();
		int y = sl.getY();
		int vx = vl.getX();
		int vy = vl.getY();
		for (int i = x; i < x + firstSize; i++) {
			for (int j = y; j < y + firstSize; j++) {
				if (i >= vx && i < secondSize + vx && j >= vy && j < secondSize + vy) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Gets all the extern tiles around the mob, depending on the size.
	 *
	 * @return The array of external tiles.
	 */
	public static List<Location> getExternTiles(Mob mob, boolean sizeOne) {
		List<Location> tiles = new LinkedList<Location>();
		if (sizeOne) {
			Location l = mob.getLocation();
			tiles.add(Location.locate(l.getX() - 1, l.getY(), l.getZ()));
			tiles.add(Location.locate(l.getX() + 1, l.getY(), l.getZ()));
			tiles.add(Location.locate(l.getX(), l.getY() - 1, l.getZ()));
			tiles.add(Location.locate(l.getX(), l.getY() + 1, l.getZ()));
			return tiles;
		}
		int size = mob.size() + 1;
		for (int x = mob.getLocation().getX() - 1; x < mob.getLocation().getX() + size; x++) {
			tiles.add(Location.locate(x, mob.getLocation().getY() - 1, mob.getLocation().getZ()));
			tiles.add(Location.locate(x, mob.getLocation().getY() + mob.size(), mob.getLocation().getZ()));
		}
		for (int y = mob.getLocation().getY() - 1; y < mob.getLocation().getY() + size; y++) {
			tiles.add(Location.locate(mob.getLocation().getX() - 1, y, mob.getLocation().getZ()));
			tiles.add(Location.locate(mob.getLocation().getX() + mob.size(), y, mob.getLocation().getZ()));
		}
		return tiles;
	}

	/**
	 * Gets all the internal tiles from the mob (not the center locations), depending on the size.
	 *
	 * @return The array of internal tiles.
	 */
	public static List<Location> getInternTiles(Mob mob, boolean sizeOne) {
		List<Location> tiles = new LinkedList<Location>();
		if (sizeOne) {
			Location l = mob.getLocation();
			tiles.add(Location.locate(l.getX(), l.getY(), l.getZ()));
			return tiles;
		}
		for (int x = mob.getLocation().getX(); x < mob.getLocation().getX() + mob.size(); x++) {
			tiles.add(Location.locate(x, mob.getLocation().getY(), mob.getLocation().getZ()));
			tiles.add(Location.locate(x, mob.getLocation().getY() + (mob.size() - 1), mob.getLocation().getZ()));
		}
		for (int y = mob.getLocation().getY(); y < mob.getLocation().getY() + mob.size(); y++) {
			tiles.add(Location.locate(mob.getLocation().getX(), y, mob.getLocation().getZ()));
			tiles.add(Location.locate(mob.getLocation().getX() + (mob.size() - 1), y, mob.getLocation().getZ()));
		}
		return tiles;
	}
	
	/**
	 * Check's if we're facing in a diagonal direction
	 * @param l The attacker's location
	 * @param l1 The victim's location
	 * @return {@code True} If we're diagonal
	 * 		   {@code False} If we're facing each other directly
	 */
	public static boolean diagonal(Location l, Location l1) {
        int xDial = Math.abs(l.getX() - l1.getX());
        int yDial = Math.abs(l.getY() - l1.getY());
        return xDial == 1 && yDial == 1;
    }
}
