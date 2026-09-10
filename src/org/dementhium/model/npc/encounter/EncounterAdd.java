package org.dementhium.model.npc.encounter;
import java.util.*;
import org.dementhium.model.*;
import org.dementhium.model.combat.*;
import org.dementhium.model.player.Player;
import org.dementhium.model.misc.DamageManager.DamageType;
public class EncounterAdd extends EncounterNPC {
 private final EncounterNPC owner;private final long ownerLife;private boolean removed;private int pulse,jump;private boolean pinned,dived;private int submerged;
 public EncounterAdd(int id){this(id,null);}
 public EncounterAdd(int id,EncounterNPC owner){super(id);this.owner=owner;ownerLife=owner==null?0:owner.getCombatGeneration();if(owner==null)bindArena(2894,4431,2937,4467,0);setDoesWalk(id==1156);submerged=isWater()?8:0;}
 @Override public boolean contains(Location l){return owner==null?super.contains(l):owner.contains(l);}
 @Override public List<Player> players(){return owner==null?super.players():owner.players();}
 @Override public int getAttackDelay(){return getId()==1156?4:5;}
 private boolean isSpin(){return getId()>=2891&&getId()<=2896;}
 private boolean isWater(){return isSpin()&&(getId()&1)==1;}
 @Override public boolean isDoesWalk(){return getId()==1156&&super.isDoesWalk();}
 @Override public boolean isAttackable(Mob source){return !isWater()&&super.isAttackable(source);}
 private void spinForm(int id){setId(id);setDefinition(org.dementhium.model.definition.NPCDefinition.forId(id));getMask().setSwitchId(id);resetCombatState();}
 @Override protected void restoreForm(){if(isSpin()){if(isWater())spinForm(getId()+1);dived=false;submerged=0;}}
 @Override public int aggression(){return 8;}
 @Override public boolean follow(Mob victim,CombatType style){if(getId()==8127||isWater()){getCombatExecutor().reset();return false;}if(isSpin()){getWalkingQueue().reset();return NPCCombatContext.validPair(this,victim)&&gap(this,victim)<=reach()&&org.dementhium.model.npc.godwars.GodWarsAction.clear(this,victim);}return super.follow(victim,style);}
 public void remove(){if(removed)return;removed=true;setDead(true);setHidden(true);getPoisonManager().removePoison();World.getWorld().getNpcs().remove(this);destroy();}
 @Override public void sendDead(){if(owner!=null)remove();else super.sendDead();}
 @Override public void tick(){
  if(removed)return;if(owner!=null&&(owner.isDead()||owner.isHidden()||owner.getCombatGeneration()!=ownerLife)){remove();return;}
    if(isSpin()&&!isDead()&&!isHidden()){
   if(isWater()){getCombatExecutor().reset();if(--submerged<=0)spinForm(getId()+1);return;}
   if(!dived&&getHp()<getMaxHp()/2){dived=true;submerged=8;spinForm(getId()-1);getPoisonManager().removePoison();return;}
  }
  if(getId()!=8127){super.tick();return;}
  getCombatExecutor().reset();List<Player> near=new ArrayList<Player>();for(Player p:players())if(near(p.getLocation(),getLocation(),1))near.add(p);
  if(near.isEmpty()){
   pinned=false;if(++jump>=3){jump=0;List<Player> targets=players();if(!targets.isEmpty()){Location tile=targets.get(getRandom().nextInt(targets.size())).getLocation();if(contains(tile)&&org.dementhium.model.map.Region.getClippingMask(tile.getX(),tile.getY(),tile.getZ())==0)teleport(tile,false);}}return;
  }
  jump=0;if(getPoisonManager().isPoisoned())pinned=true;if(++pulse<(pinned?20:2))return;pulse=0;
  for(Player p:near){if(!NPCCombatContext.validPair(this,p))continue;Damage d=Damage.getDamage(this,p,CombatType.MAGIC,1+getRandom().nextInt(100),true);d.setMaximum(100);d.onImpact(actual->{if(owner!=null&&!owner.isDead()&&ownerLife==owner.getCombatGeneration())owner.setHp(Math.min(owner.getMaxHp(),owner.getHp()+actual));});p.getDamageManager().damage(this,d,DamageType.MAGE);}
 }
}
