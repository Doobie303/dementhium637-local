import java.lang.reflect.*;
import org.dementhium.cache.Cache;
import org.dementhium.content.skills.Prayer;
import org.dementhium.content.skills.slayer.*;
import org.dementhium.model.*;
import org.dementhium.model.combat.*;
import org.dementhium.model.combat.impl.spells.ancient.*;
import org.dementhium.model.definition.*;
import org.dementhium.model.misc.DamageManager.DamageType;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.player.*;
public class CombatEnhancementsRegression {
 static int checks;
 static void check(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 static void near(double a,double b,String m){check(Math.abs(a-b)<.00001,m+": "+a+" vs "+b);}
 static Player player(){Player p=CombatFormulaRegression.player();p.getSkills().setMaximumLifePoints(1000);p.getSkills().setHitPoints(1000);p.getSkills().setLevelAndXP(Skills.PRAYER,99,13034431);p.getSkills().setPrayerPoints(99,false);return p;}
 static void gear(Player p,int slot,int id){CombatFormulaRegression.gear(p,slot,id);}
 static NPC npc(String name){for(int i=0;i<13488;i++){NPCDefinition d=NPCDefinition.forId(i);if(d!=null&&name.equalsIgnoreCase(d.getName()))return new NPC(i);}throw new AssertionError(name);}
 static RangeData ammo(Player p,int weapon,int item,boolean drop,int type,int count){gear(p,3,weapon);p.getEquipment().set(13,new Item(item,count));RangeData d=new RangeData(true);d.setWeapon(RangeWeapon.get(weapon));d.setAmmo(Ammunition.get(item));d.setDropAmmo(drop);d.setWeaponType(type);return d;}
 public static void main(String[] args)throws Exception{
  Cache.init();ItemDefinition.init();NPCDefinition.init();org.dementhium.model.misc.GroundItemManager.load();
  Player p=player(),v=player();
  gear(v,5,13740);near(SpiritShield.reduce(v,500),350,"Divine 30 percent");near(v.getSkills().getPrayerPoints(),91.5,"Prayer costs half absorbed in server units");
  v.getSkills().setPrayerPoints(1,false);check(SpiritShield.reduce(v,500)==480,"Low prayer partial reduction");near(v.getSkills().getPrayerPoints(),0,"Cannot overdraw prayer");
  check(SpiritShield.reduce(v,500)==500,"Empty prayer no protection");
  v.getSkills().setPrayerPoints(99,false);v.getDamageManager().damage(p,new Damage(500),DamageType.MELEE);
  check(v.getHitPoints()==650,"Shared shield applied once");near(v.getSkills().getPrayerPoints(),91.5,"Shared prayer charged once");
  v.setAttribute("godmode",true);v.getDamageManager().damage(p,new Damage(500),DamageType.RANGE);near(v.getSkills().getPrayerPoints(),91.5,"Godmode no shield drain");v.removeAttribute("godmode");
  gear(v,5,13742);v.getRandom().setSeed(20260907);int proc=0;for(int i=0;i<10000;i++)if(SpiritShield.reduce(v,400)==300)proc++;check(proc>6800&&proc<7200,"Elysian 70 percent: "+proc);
  p.getPrayer().setAncientBook(true);CombatBalanceRegression.prayers(p)[1][Prayer.TURMOIL]=true;p.getPrayer().modify(Prayer.TURMOIL,true);p.getPrayer().updateTurmoil(v);
  check(p.getPrayer().getTurmoilAttack()==14&&p.getPrayer().getTurmoilStrength()==9&&p.getPrayer().getTurmoilDefence()==14,"Turmoil flat caps");near(p.getPrayer().getStrengthModifier(),.23,"Turmoil does not multiply flat stolen levels");
  Field f=Prayer.class.getDeclaredField("attackModifier");f.setAccessible(true);f.setInt(p.getPrayer(),-5);p.getPrayer().updateTurmoil(null);check(f.getInt(p.getPrayer())==-5,"Clearing Turmoil preserves incoming curse");
  p=player();CombatBalanceRegression.prayers(p)[0][Prayer.PROTECT_FROM_MELEE]=true;double before=p.getSkills().getPrayerPoints();p.getPrayer().tick();double base=before-p.getSkills().getPrayerPoints();check(base>0,"Prayer drains");
  int[] bonuses=ItemDefinition.forId(1127).getBonus();int old=bonuses[Bonuses.PRAYER];try{bonuses[Bonuses.PRAYER]=30;gear(p,4,1127);before=p.getSkills().getPrayerPoints();p.getPrayer().tick();near(before-p.getSkills().getPrayerPoints(),base/2,"30 prayer doubles duration independent of defence bonus");}finally{bonuses[Bonuses.PRAYER]=old;}
  p=player();NPC bat=npc("Bat");p.getSlayer().setSlayerTask(new SlayerTask(SlayerTask.Master.VANNAKA,0,20));gear(p,0,15492);
  near(EquipmentEffects.multiplier(p,bat,CombatType.MELEE),7.0/6,"Full helm melee task");near(EquipmentEffects.multiplier(p,bat,CombatType.RANGE),1.15,"Full helm ranged task");near(EquipmentEffects.multiplier(p,bat,CombatType.MAGIC),1.15,"Full helm magic task");
  check(Damage.getDamage(p,bat,CombatType.MELEE,100).getHit()==100,"NPC mitigation does not double helmet boost");
  gear(p,0,8921);near(EquipmentEffects.multiplier(p,bat,CombatType.RANGE),1,"Black mask cannot boost ranged");near(EquipmentEffects.multiplier(p,v,CombatType.MELEE),1,"No PvP slayer bonus");
  gear(p,0,-1);gear(p,2,10588);NPC skeleton=npc("Skeleton");near(EquipmentEffects.multiplier(p,skeleton,CombatType.MELEE),1.2,"Salve enchanted undead melee");near(EquipmentEffects.multiplier(p,skeleton,CombatType.MAGIC),1,"Salve no magic bonus in 637");
  p=player();RangeData rd=ammo(p,4734,4740,false,1,3);check(CombatUtils.consumeAmmunition(p,rd),"Karil reserve");check(p.getEquipment().get(13).getAmount()==2,"Non-droppable bolt racks consumed");check(CombatUtils.consumeAmmunition(p,rd)&&p.getEquipment().get(13).getAmount()==2,"No double debit");
  rd=ammo(p,15241,15243,false,4,3);check(CombatUtils.consumeAmmunition(p,rd)&&p.getEquipment().get(13).getAmount()==2,"Hand cannon consumes shot");
  rd=ammo(p,11235,892,true,2,3);rd.setDamage2(new Damage(0));check(CombatUtils.consumeAmmunition(p,rd)&&p.getEquipment().get(13).getAmount()==1,"Dark bow reserves two arrows");
  rd=ammo(p,861,892,true,0,1);gear(p,13,882);check(!CombatUtils.consumeAmmunition(p,rd),"Ammo swap cannot debit wrong stack");
  p=player();v=player();final int[] effect={0};Damage d=new Damage(500).onImpact(actual->effect[0]+=actual);v.getSkills().setHitPoints(10);check(effect[0]==0,"No effect at launch");v.getDamageManager().damage(p,d,DamageType.RANGE);check(effect[0]==10,"Effect uses actual overkill-capped loss");d.finishEffects(p,v,10);check(effect[0]==10,"Effect executes once");
  v=player();v.setAttribute("godmode",true);d=new Damage(500).onImpact(actual->effect[0]+=actual);v.getDamageManager().damage(p,d,DamageType.RANGE);check(effect[0]==10,"Immunity suppresses offensive effects");
  v=player();check(CombatStatus.freeze(v,32),"Freeze at impact");check(v.getAttribute("freezeTime",-1)==World.getTicks()+32,"Barrage 32 ticks");check(!CombatStatus.freeze(v,32),"Cannot refresh an active freeze");v.removeAttribute("freezeTime");check(!CombatStatus.freeze(v,32),"Thaw immunity");v.removeAttribute("freezeImmunity");v.setAttribute("godmode",true);check(!CombatStatus.freeze(v,32),"Godmode freeze immunity");
  p=player();NPC dragon=npc("Green dragon");Item shield=new Item(11284);p.getEquipment().set(Equipment.SLOT_SHIELD,shield);Damage.getDamage(dragon,p,CombatType.DRAGONFIRE,500);shield=p.getEquipment().get(Equipment.SLOT_SHIELD);check(shield.getId()==11283&&shield.getHealth()==1,"Breath charges and activates an uncharged dragonfire shield");
  shield.setHealth(CombatUtils.MAX_DRAGONFIRE_SHIELD_CHARGES);Damage.getDamage(dragon,p,CombatType.DRAGONFIRE,500);check(shield.getHealth()==CombatUtils.MAX_DRAGONFIRE_SHIELD_CHARGES,"Dragonfire shield charge cap");
  p.getEquipment().set(Equipment.SLOT_SHIELD,new Item(1540));Damage.getDamage(dragon,p,CombatType.DRAGONFIRE,500);check(p.getEquipment().get(Equipment.SLOT_SHIELD).getHealth()==0,"Anti-dragon shield does not gain DFS charges");
  v=player();Interaction inter=new Interaction(p,v);inter.setSpell(new IceRush());inter.setDamage(new Damage(0));inter.setEndGraphic(org.dementhium.model.mask.Graphic.create(361));new IceRush().endSpell(inter);check(v.getAttribute("freezeTime",-1)==World.getTicks()+8,"Successful zero hit freezes");
  v=player();inter=new Interaction(p,v);inter.setSpell(new IceRush());inter.setDamage(new Damage(-1));new IceRush().endSpell(inter);check(v.getAttribute("freezeTime",-1)==-1,"Splash does not freeze");
  p.setLocation(Location.locate(3200,3200,0));v.setLocation(Location.locate(3200,3200,1));check(!CombatMovement.combatFollow(p,v,CombatType.RANGE),"No attacks across planes");
    p=player();v=player();gear(p,3,9185);gear(p,13,9242);p.getSkills().setHitPoints(500);
  long seed=0;while(new java.util.Random(seed).nextInt(100)<75)seed++;
  p.getRandom().setSeed(seed);d=CombatUtils.getRangeDamage(p,v,Ammunition.get(9242));check(d.getHit()==200,"Ruby uses 20 percent target HP");check(p.getHitPoints()==500,"Ruby cost waits for impact");v.getDamageManager().damage(p,d,DamageType.RANGE);check(p.getHitPoints()==450&&v.getHitPoints()==800,"Ruby charges 10 percent HP on impact");
  p=player();v=player();p.setLocation(Location.locate(3200,3200,0));v.setLocation(Location.locate(3202,3200,0));gear(p,3,861);p.getEquipment().set(13,new Item(892,10));
  inter=new Interaction(p,v);new org.dementhium.model.combat.impl.specs.SnapShot().commenceSpecialAttack(inter);check(p.getEquipment().get(13).getAmount()==8,"Magic shortbow special debits two arrows from its own data");
  gear(p,3,11235);p.getEquipment().set(13,new Item(892,10));inter=new Interaction(p,v);new org.dementhium.model.combat.impl.specs.DescentOfDragons().commenceSpecialAttack(inter);check(p.getEquipment().get(13).getAmount()==8,"Dark bow special debits two arrows");
  gear(p,3,15241);p.getEquipment().set(13,new Item(15243,10));inter=new Interaction(p,v);new org.dementhium.model.combat.impl.specs.AimedShot().commenceSpecialAttack(inter);check(p.getEquipment().get(13).getAmount()==9,"Hand cannon special debits one shot");
  System.out.println("PASS: "+checks+" combat enhancement checks; Elysian "+proc+"/10000");
 }
}

