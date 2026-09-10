package org.dementhium.model.npc.godwars;

import java.util.*;
import java.util.function.IntSupplier;
import org.dementhium.model.*;
import org.dementhium.model.map.Region;
import org.dementhium.model.player.Player;
import org.dementhium.model.instance.InstanceAccess;

/** One physical room owns its members and death timers; never looks up NPCs globally by ID. */
public final class GodWarsRoom {
    public static final int BOSS_RESPAWN=100, FOLLOWER_RESPAWN=25, EMPTY_RESET=10;
    private static final Map<GodWarsType,GodWarsRoom> SHARED=new EnumMap<GodWarsType,GodWarsRoom>(GodWarsType.class);
    private final GodWarsType type;
    private final int minX,minY,maxX,maxY,plane;
    private final IntSupplier clock;
    private final Map<Integer,GodWarsNPC> members=new LinkedHashMap<Integer,GodWarsNPC>();
    private final Map<GodWarsNPC,Death> deaths=new IdentityHashMap<GodWarsNPC,Death>();
    private static final class Pending {final int due;final Runnable action;Pending(int due,Runnable action){this.due=due;this.action=action;}}
    private final List<Pending> pending=new ArrayList<Pending>();
    public void schedule(int delay,Runnable action){if(closed)throw new IllegalStateException("Closed room");pending.add(new Pending(clock.getAsInt()+Math.max(1,delay),action));}
    private int lastTick=Integer.MIN_VALUE,emptySince=-1;
    private long revision;
    private boolean closed,engaged;
    private static final class Death {
        final long generation;final int at;
        Death(GodWarsNPC npc,int at){generation=npc.getCombatGeneration();this.at=at;}
    }
    public GodWarsRoom(GodWarsType type,int minX,int minY,int maxX,int maxY,int plane,IntSupplier clock){
        if(type==null||clock==null||minX>maxX||minY>maxY||plane<0||plane>3)throw new IllegalArgumentException("Invalid room");
        this.type=type;this.minX=minX;this.minY=minY;this.maxX=maxX;this.maxY=maxY;this.plane=plane;this.clock=clock;
    }
    public static GodWarsRoom forSpawn(GodWarsNPC npc){
        GodWarsType t=npc.getGodWarsType();Location l=npc.getOriginalLocation();
        // Copied maps need explicit room attachment by their content owner.
        if(l==null||npc.getOwningInstance()!=null||l.getZ()!=t.plane||l.getX()<t.minX||l.getX()>t.maxX||l.getY()<t.minY||l.getY()>t.maxY)return null;
        GodWarsRoom room=SHARED.get(t);
        if(room==null||room.closed){room=new GodWarsRoom(t,t.minX,t.minY,t.maxX,t.maxY,t.plane,World::getTicks);SHARED.put(t,room);}
        room.attach(npc);return room;
    }
    public void attach(GodWarsNPC npc){
        if(closed||!type.has(npc.getId())||!contains(npc.getLocation()))throw new IllegalArgumentException("NPC outside its room");
        GodWarsNPC old=members.get(npc.getId());
        if(old!=null&&old!=npc)throw new IllegalStateException("Duplicate room member "+npc.getId());
        if(npc.attachedRoom()!=null&&npc.attachedRoom()!=this)throw new IllegalStateException("NPC already owned");
        members.put(npc.getId(),npc);npc.attachRoom(this);
    }
    public void detach(GodWarsNPC npc){if(members.get(npc.getId())==npc)members.remove(npc.getId());deaths.remove(npc);}
    public boolean contains(Location l){return l!=null&&l.getZ()==plane&&l.getX()>=minX&&l.getX()<=maxX&&l.getY()>=minY&&l.getY()<=maxY;}
    public boolean owns(GodWarsNPC npc){return !closed&&members.get(npc.getId())==npc;}
    public long getRevision(){return revision;}
    public int currentTick(){return clock.getAsInt();}
    public GodWarsNPC boss(){return members.get(type.boss);}

    /** Room-local actors only: bosses keep their pursuit; followers give them space. */
    public int overlap(GodWarsNPC npc,Location tile){
        int area=0;
        for(GodWarsNPC other:members.values())if(other!=npc&&!other.isDead()&&!other.isHidden()){
            int x=Math.max(0,Math.min(tile.getX()+npc.size(),other.getLocation().getX()+other.size())-Math.max(tile.getX(),other.getLocation().getX()));
            int y=Math.max(0,Math.min(tile.getY()+npc.size(),other.getLocation().getY()+other.size())-Math.max(tile.getY(),other.getLocation().getY()));
            area+=x*y;
        }
        return area;
    }

    /** Shortest clipped escape/approach around room actors. No teleport or global crowd lock. */
    public boolean repositionFollower(GodWarsNPC npc,Mob target){
        if(npc.isBoss()||target==null)return false;
        Location start=npc.getLocation();boolean escaping=overlap(npc,start)>0;
        int initialGap=tileGap(start,npc.size(),target);
        ArrayDeque<int[]> queue=new ArrayDeque<int[]>();Set<Long> seen=new HashSet<Long>();
        queue.add(new int[]{0,0,0,0});seen.add(0L);
        while(!queue.isEmpty()){
            int[] point=queue.remove();Location from=start.transform(point[0],point[1],0);
            for(int[] dir:new int[][]{{-1,0},{1,0},{0,-1},{0,1},{-1,-1},{-1,1},{1,-1},{1,1}}){
                int x=point[0]+dir[0],y=point[1]+dir[1];if(Math.abs(x)>8||Math.abs(y)>8)continue;
                long key=((long)x<<32)^(y&0xffffffffL);if(seen.contains(key))continue;
                Location to=start.transform(x,y,0);
                if(org.dementhium.model.combat.CombatMovement.standingOn(to,target.getLocation(),npc.size(),target.size())
                        ||!org.dementhium.model.combat.CombatMovement.npcStepClear(npc,from,to))continue;
                seen.add(key);int firstX=point[0]==0&&point[1]==0?dir[0]:point[2],firstY=point[0]==0&&point[1]==0?dir[1]:point[3];
                if(overlap(npc,to)==0&&(escaping||tileGap(to,npc.size(),target)<initialGap))
                    return org.dementhium.model.combat.CombatMovement.tryNpcStep(npc,firstX,firstY);
                queue.add(new int[]{x,y,firstX,firstY});
            }
        }
        return false;
    }
    private static int tileGap(Location tile,int size,Mob target){
        return Math.max(Math.max(0,Math.max(target.getLocation().getX()-tile.getX()-size+1,tile.getX()-target.getLocation().getX()-target.size()+1)),
            Math.max(0,Math.max(target.getLocation().getY()-tile.getY()-size+1,tile.getY()-target.getLocation().getY()-target.size()+1)));
    }
    public boolean accepts(GodWarsNPC npc,Mob other){
        if(other==null||!owns(npc)||!contains(npc.getLocation())||!contains(other.getLocation())||other.isPlayer()&&other.getPlayer().isInvisible()||!InstanceAccess.canInteract(npc,other))return false;
        return !(other instanceof GodWarsNPC)||((GodWarsNPC)other).attachedRoom()==this;
    }
    public List<Player> players(){
        List<Player> found=new ArrayList<Player>();
        Location center=Location.locate((minX+maxX)/2,(minY+maxY)/2,plane);
        for(Player p:Region.getLocalPlayers(center,Math.max(maxX-minX,maxY-minY)+2))
            if(contains(p.getLocation())&&p.isOnline()&&!p.isDead()&&!p.isHidden()&&!p.isInvisible())found.add(p);
        return found;
    }
    public List<Player> targets(GodWarsNPC npc){
        List<Player> result=players();result.removeIf(p -> !accepts(npc,p)||!p.isAttackable(npc));return result;
    }
    public void onDeath(GodWarsNPC npc,Mob killer){
        if(!owns(npc)||!npc.isDead()||deaths.containsKey(npc))return;
        deaths.put(npc,new Death(npc,clock.getAsInt()));engaged=true;
        if(npc.getId()==type.boss&&type==GodWarsType.ZAMORAK){
            // Each surviving Zamorak follower makes its own eligible room selection.
            for(GodWarsNPC follower:members.values())if(follower!=npc&&!follower.isDead()&&!follower.isHidden()){
                List<Player> candidates=targets(follower);
                follower.getCombatExecutor().setVictim(candidates.isEmpty()?null:candidates.get(follower.getRandom().nextInt(candidates.size())));
            }
        }else if(npc.getId()==type.boss&&killer!=null&&contains(killer.getLocation()))
            for(GodWarsNPC follower:members.values())if(follower!=npc&&!follower.isDead()&&accepts(follower,killer))follower.getCombatExecutor().setVictim(killer);
    }
    public void tick(){
        int now=clock.getAsInt();if(closed||lastTick==now)return;lastTick=now;
        List<Pending> due=new ArrayList<Pending>();
        for(Iterator<Pending> it=pending.iterator();it.hasNext();){Pending p=it.next();if(now>=p.due){it.remove();due.add(p);}}
        for(Pending p:due)try{p.action.run();}catch(RuntimeException failure){failure.printStackTrace();}
        for(Iterator<Map.Entry<GodWarsNPC,Death>> it=deaths.entrySet().iterator();it.hasNext();){
            Map.Entry<GodWarsNPC,Death> entry=it.next();GodWarsNPC npc=entry.getKey();Death d=entry.getValue();
            if(!owns(npc)||!npc.isDead()||npc.getCombatGeneration()!=d.generation){it.remove();continue;}
            int elapsed=now-d.at;if(elapsed>=npc.getDeathTick())npc.setHidden(true);
            GodWarsNPC boss=boss();boolean ready=npc.getId()==type.boss?elapsed>=BOSS_RESPAWN:boss!=null&&!boss.isDead()&&elapsed>=FOLLOWER_RESPAWN;
            if(ready&&elapsed>=npc.getDeathTick()){revive(npc);it.remove();}
        }
        for(GodWarsNPC npc:members.values())if(!npc.isDead()&&(npc.getCombatExecutor().getVictim()!=null||npc.getHp()<npc.getMaxHp()))engaged=true;
        if(!players().isEmpty()){emptySince=-1;return;}
        if(!engaged)return;
        if(emptySince<0)emptySince=now;
        if(now-emptySince<EMPTY_RESET)return;
        revision++;engaged=false;emptySince=-1;
        for(GodWarsNPC npc:members.values())if(!npc.isDead())resetLiving(npc);
    }
    private void resetLiving(GodWarsNPC npc){
        npc.resetCombatState();npc.getPoisonManager().removePoison();npc.removeAttribute("freezeTime");npc.removeAttribute("miasmicTime");
        npc.getDamageManager().clearEnemyHits();npc.setHp(npc.getMaxHp());
        if(npc.getOriginalLocation()!=null)npc.teleport(npc.getOriginalLocation(),false);
    }
    private void revive(GodWarsNPC npc){resetLiving(npc);npc.setDead(false);npc.setHidden(false);}
    public void close(){if(closed)return;closed=true;revision++;deaths.clear();pending.clear();for(GodWarsNPC npc:members.values())npc.resetCombatState();members.clear();}
}
