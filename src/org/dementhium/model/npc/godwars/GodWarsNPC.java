package org.dementhium.model.npc.godwars;

import java.util.List;
import org.dementhium.model.Mob;
import org.dementhium.model.combat.*;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.player.Player;

public class GodWarsNPC extends NPC {
    private final GodWarsType god;
    private final GodWarsAction action;
    private GodWarsRoom room;
    private GodWarsAction.Attack prepared;
    private Mob preparedTarget;
    public GodWarsNPC(int id){super(id);god=GodWarsType.forId(id);action=new GodWarsAction(this);}
    public GodWarsType getGodWarsType(){return god;}
    public GodWarsRoom attachedRoom(){return room;}
    public void attachRoom(GodWarsRoom room){this.room=room;prepared=null;}
    public GodWarsRoom getRoom(){if(room==null)room=GodWarsRoom.forSpawn(this);return room;}
    public boolean isBoss(){return getId()==god.boss;}
    @Override public int getAttackDelay(){return god!=null&&isBoss()?god.speed:super.getAttackDelay();}
    @Override public CombatAction getCombatAction(){return action;}
    public GodWarsAction.Attack prepareAttack(){
        Mob target=getCombatExecutor().getVictim();
        if(prepared==null||preparedTarget!=target||!GodWarsAction.eligible(this,target,prepared)) {
            prepared=GodWarsAction.choose(this,target);preparedTarget=target;
        }
        return prepared;
    }
    public GodWarsAction.Attack takeAttack(){GodWarsAction.Attack attack=prepareAttack();prepared=null;return attack;}
    @Override public void resetCombatState(){super.resetCombatState();prepared=null;}
    public boolean allows(Mob other){GodWarsRoom r=getRoom();return r!=null&&r.accepts(this,other);}
    public static boolean roomPair(Mob a,Mob b){
        boolean relevant=a instanceof GodWarsNPC||b instanceof GodWarsNPC;
        return relevant&&(!(a instanceof GodWarsNPC)||((GodWarsNPC)a).allows(b))&&(!(b instanceof GodWarsNPC)||((GodWarsNPC)b).allows(a));
    }
    public boolean hasAttacker(){GodWarsRoom r=getRoom();if(r==null)return false;for(Player p:r.targets(this))if(p.getCombatExecutor().getVictim()==this)return true;return false;}
    @Override public void tick(){
        GodWarsRoom r=getRoom();if(r==null)return;r.tick();
        if(isDead()||isHidden()||!r.owns(this))return;
        getCombatStats().tick();
        if(!r.contains(getLocation())){resetCombatState();if(getOriginalLocation()!=null)teleport(getOriginalLocation(),false);return;}
        Mob victim=getCombatExecutor().getVictim();
        if(victim!=null&&(!NPCCombatContext.validPair(this,victim)||!victim.isAttackable(this))){getCombatExecutor().reset();prepared=null;victim=null;}
        if(!isBoss()){
            GodWarsNPC boss=r.boss();Mob tank=null;
            if(boss!=null&&!boss.isDead()){
                tank=boss.getCombatExecutor().getLastAttacker();
                if(tank==null||!NPCCombatContext.validPair(this,tank)||!tank.isAttackable(this))tank=boss.getCombatExecutor().getVictim();
            }
            if((victim==null||r.currentTick()%5==0)&&tank!=null&&NPCCombatContext.validPair(this,tank)&&tank.isAttackable(this)){getCombatExecutor().setVictim(tank);return;}
        }
        if(victim==null){List<Player> targets=r.targets(this);if(!targets.isEmpty())getCombatExecutor().setVictim(targets.get(getRandom().nextInt(targets.size())));}
    }
    public void died(Mob killer){GodWarsRoom r=getRoom();if(r!=null)r.onDeath(this,killer);}
    @Override public void destroy(){if(room!=null)room.detach(this);super.destroy();}
}
