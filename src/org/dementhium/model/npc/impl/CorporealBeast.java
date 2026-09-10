package org.dementhium.model.npc.impl;
import java.util.*;
import org.dementhium.model.*;
import org.dementhium.model.combat.*;
import org.dementhium.model.definition.WeaponInterface;
import org.dementhium.model.npc.encounter.*;
import org.dementhium.model.player.Player;
import org.dementhium.model.misc.DamageManager.DamageType;
public class CorporealBeast extends EncounterNPC {
 private EncounterAdd core;private int nextCore=10,nextStomp;
 public CorporealBeast(int id){super(id);bindArena(2974,4368,3000,4401,2);}
 @Override public int getAttackDelay(){return 6;}
 @Override public int reach(){return 12;}
 public EncounterAdd getCore(){return core;}
 public void attackLaunched(int animation){nextStomp=Math.max(nextStomp,clock()+(org.dementhium.model.mask.NPCAnimation.frames(animation)+29)/30);}
 @Override public boolean follow(Mob target,CombatType style){
  for(Player p:players())if(EncounterNPC.gap(this,p)==0){getWalkingQueue().reset();return false;}
  return super.follow(target,style);
 }
 @Override public Damage updateHit(Mob source,int hit,CombatType type){
  boolean full=type==CombatType.MAGIC;
  if(source!=null&&source.isPlayer()&&type==CombatType.MELEE){Item weapon=source.getPlayer().getEquipment().get(3);full=weapon!=null&&weapon.getDefinition().getName().toLowerCase(Locale.ROOT).contains("spear")&&source.getPlayer().getSettings().getCombatType()==WeaponInterface.TYPE_STAB;}
  return new Damage(Math.min(1000,Math.max(0,full?hit:hit/2)));
 }
 public boolean spawnCore(Player target){
  if(getHp()>=getMaxHp()*3/4||!NPCCombatContext.validPair(this,target)||!allows(target)||!target.isAttackable(this)||core!=null&&!core.isDead()&&!core.isHidden()||getOwningInstance()!=null)return false;
  Location tile=target.getLocation();if((org.dementhium.model.map.Region.getClippingMask(tile.getX(),tile.getY(),tile.getZ())&(256|0x200000))!=0)return false;
  if(core!=null)core.remove();
  core=new EncounterAdd(8127,this);core.setLocation(target.getLocation());core.setOriginalLocation(target.getLocation());World.getWorld().getNpcs().add(core);return true;
 }
 @Override protected void clearAdds(){if(core!=null){core.remove();core=null;}nextCore=clock()+10;nextStomp=clock()+6;}
 @Override protected void mechanics(){
  List<Player> targets=players();
  if(clock()>=nextCore){nextCore=clock()+10;if(getHp()<getMaxHp()*3/4&&(core==null||core.isDead()||core.isHidden())&&!targets.isEmpty()&&getRandom().nextInt(4)==0)spawnCore(targets.get(getRandom().nextInt(targets.size())));}
  if(clock()<nextStomp)return;
  boolean launched=false;
  for(Player p:targets)if(EncounterNPC.gap(this,p)==0){if(!launched){launched=true;nextStomp=clock()+6;getCombatExecutor().setTicks(6);animate(10057);}int raw=getRandom().nextInt(301);Damage d=Damage.getDamage(this,p,CombatType.MELEE,raw,true);d.setMaximum(300);p.getDamageManager().damage(this,d,DamageType.MELEE);}
 }
}
