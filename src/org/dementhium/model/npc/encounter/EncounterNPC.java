package org.dementhium.model.npc.encounter;

import java.util.*;
import org.dementhium.model.*;
import org.dementhium.model.combat.*;
import org.dementhium.model.instance.InstanceAccess;
import org.dementhium.model.map.Region;
import org.dementhium.model.map.path.PrimitivePathFinder;
import org.dementhium.model.map.Directions;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.player.Player;
import org.dementhium.model.npc.godwars.GodWarsAction;

/** Local encounter clock owns hits, transitions, loot and respawn. No global ID lookups. */
public abstract class EncounterNPC extends NPC {
    private int minX,minY,maxX,maxY,plane,time,empty;
    private boolean engaged,destroyed,combatTickStarted;
    private EncounterAttack.Kind prepared;
    private final List<Pending> pending=new ArrayList<Pending>();
    private static final class Pending {final int at,worldAt;final long life;final Runnable run;Pending(int at,int worldAt,long life,Runnable run){this.at=at;this.worldAt=worldAt;this.life=life;this.run=run;}}
    public EncounterNPC(int id){super(id);setDoesWalk(true);}
    public final void bindArena(int x,int y,int xx,int yy,int z){if(x>xx||y>yy||z<0||z>3)throw new IllegalArgumentException("Arena");minX=x;minY=y;maxX=xx;maxY=yy;plane=z;}
    public boolean contains(Location l){return l!=null&&l.getZ()==plane&&l.getX()>=minX&&l.getX()<=maxX&&l.getY()>=minY&&l.getY()<=maxY;}
    public boolean allows(Mob other){return !destroyed&&other!=null&&contains(getLocation())&&contains(other.getLocation())&&InstanceAccess.canInteract(this,other)&&(!other.isPlayer()||!other.getPlayer().isInvisible());}
    public static boolean pair(Mob a,Mob b){return a==null||b==null||((!(a instanceof EncounterNPC)||((EncounterNPC)a).allows(b))&&(!(b instanceof EncounterNPC)||((EncounterNPC)b).allows(a)));}
    public static boolean near(Location a,Location b,int radius){return a.getZ()==b.getZ()&&Math.abs(a.getX()-b.getX())<=radius&&Math.abs(a.getY()-b.getY())<=radius;}
    public int clock(){return time;}
    public int reach(){return 8;}
    public int aggression(){return 8;}
    public int respawnDelay(){return 60+getDeathTick();}
    public List<Player> players(){
        List<Player> list=new ArrayList<Player>();
        for(Player p:Region.getLocalPlayers(Location.locate((minX+maxX)/2,(minY+maxY)/2,plane),Math.max(maxX-minX,maxY-minY)+2))
            if(allows(p)&&p.isOnline()&&!p.isDead()&&!p.isHidden()&&p.isAttackable(this))list.add(p);
        return list;
    }
    public static int gap(Mob a,Mob b){int dx=Math.max(a.getLocation().getX()-b.getLocation().getX()-b.size()+1,b.getLocation().getX()-a.getLocation().getX()-a.size()+1);int dy=Math.max(a.getLocation().getY()-b.getLocation().getY()-b.size()+1,b.getLocation().getY()-a.getLocation().getY()-a.size()+1);return Math.max(0,Math.max(dx,dy));}
    public boolean follow(Mob victim,CombatType style){
        if(!NPCCombatContext.validPair(this,victim)||!allows(victim)){getCombatExecutor().reset();return false;}
        boolean contact=GodWarsAction.contact(this,victim);
        if((style==CombatType.MELEE?contact:gap(this,victim)<=reach())&&GodWarsAction.clear(this,victim)){getWalkingQueue().reset();turnTo(victim,false);return true;}
        CombatMovement.followNpc(this,victim);return false;
    }
    protected void step(int dx,int dy){
        CombatMovement.tryNpcStep(this,dx,dy);
    }
    /** Called before combat; tick() dispatches due work without advancing a second time. */
    public final void beginCombatTick(){if(!combatTickStarted){time++;combatTickStarted=true;}}
    public final void endCombatTick(){combatTickStarted=false;}
    // The world deadline also covers player/world callbacks before this NPC task in the same cycle.
    public void schedule(int delay,Runnable task){if(!destroyed)pending.add(new Pending(time+Math.max(1,delay),World.getTicks()+Math.max(1,delay),getCombatGeneration(),task));}
    @Override public CombatAction getCombatAction(){return new EncounterAttack(this);}
    public EncounterAttack.Kind prepareAttack(){if(prepared==null)prepared=EncounterAttack.choose(this,getCombatExecutor().getVictim());return prepared;}
    public EncounterAttack.Kind takeAttack(){EncounterAttack.Kind k=prepareAttack();prepared=null;return k;}
    @Override public void resetCombatState(){super.resetCombatState();prepared=null;if(pending!=null)pending.clear();}
    protected void clearAdds(){}
    protected void restoreForm(){}
    protected void mechanics(){}
    public void resetEncounter(){clearAdds();resetCombatState();restoreForm();setDead(false);setHidden(false);getPoisonManager().removePoison();removeAttribute("freezeTime");removeAttribute("miasmicTime");getDamageManager().clearEnemyHits();setHp(getMaxHp());if(getOriginalLocation()!=null)teleport(getOriginalLocation(),false);getWalkingQueue().reset();engaged=false;empty=0;}
    @Override public void tick(){
        if(!combatTickStarted)time++;
        List<Pending> due=new ArrayList<Pending>();for(Iterator<Pending> it=pending.iterator();it.hasNext();){Pending p=it.next();if(time>=p.at&&(!combatTickStarted||World.getTicks()>=p.worldAt)){it.remove();due.add(p);}}
        for(Pending p:due)if(!destroyed&&p.life==getCombatGeneration())p.run.run();
        if(destroyed||isDead()||isHidden())return;
        getCombatStats().tick();
        if(!contains(getLocation())){resetEncounter();return;}
        List<Player> candidates=players();
        if(getHp()<getMaxHp()||getCombatExecutor().getVictim()!=null)engaged=true;
        if(candidates.isEmpty()){if(engaged&&++empty>=10)resetEncounter();return;}empty=0;
        Mob victim=getCombatExecutor().getVictim();
        if(victim!=null&&(!NPCCombatContext.validPair(this,victim)||!victim.isAttackable(this))){getCombatExecutor().reset();prepared=null;victim=null;}
        if(victim==null){Player closest=null;int distance=Integer.MAX_VALUE;for(Player p:candidates){int d=gap(this,p);if(d<=aggression()&&d<distance&&GodWarsAction.clear(this,p)){closest=p;distance=d;}}if(closest!=null)getCombatExecutor().setVictim(closest);}
        mechanics();
        if(getCombatExecutor().getVictim()==null&&isDoesWalk()&&getOriginalLocation()!=null&&getRandom().nextInt(10)==0){int dx=getRandom().nextInt(3)-1,dy=getRandom().nextInt(3)-1;if(getLocation().transform(dx,dy,0).distance(getOriginalLocation())<=3)step(dx,dy);}
    }
    @Override public void sendDead(){
        if(isDead()||destroyed)return;
        Mob last=getCombatExecutor().getLastAttacker();if(last!=null)last.setAttribute("combatTicks",0);
        setDead(true);clearAdds();getPoisonManager().removePoison();getWalkingQueue().reset();resetTurnTo();animate(getDeathAnimation());
        // DamageManager has recorded the lethal contribution before entering death.
        schedule(Math.max(1,getDeathTick()),()->{Mob killer=getDamageManager().getKiller();if(killer!=null&&killer.isFamiliar())killer=killer.getFamiliar().getOwner();if(killer!=null)loot(killer);getDamageManager().clearEnemyHits();setHidden(true);});
        schedule(respawnDelay(),()->{restoreForm();setDead(false);setHidden(false);resetEncounter();});
    }
    @Override public boolean isAttackable(Mob source){return !destroyed&&!isDead()&&!isHidden()&&allows(source)&&super.isAttackable(source);}
    @Override public void destroy(){destroyed=true;clearAdds();if(pending!=null)pending.clear();super.destroy();}
}
