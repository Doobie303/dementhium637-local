import java.lang.reflect.*;import java.util.*;
import org.dementhium.cache.Cache;import org.dementhium.content.skills.Prayer;import org.dementhium.model.*;
import org.dementhium.model.combat.*;import org.dementhium.model.combat.impl.specs.*;
import org.dementhium.model.definition.*;import org.dementhium.model.player.*;
import org.dementhium.model.misc.DamageManager.DamageType;import org.dementhium.tickable.Tick;
public class SpecialShieldRegression {
 static int checks;static void check(boolean ok,String m){checks++;if(!ok)throw new AssertionError(m);}
 static Player p(){Player p=CombatEnhancementsRegression.player();p.getSkills().setLevelAndXP(Skills.CONSTITUTION,99,13034431);p.getSkills().setMaximumLifePoints(1000);p.getSkills().setHitPoints(1000);return p;}
 static void gear(Player p,int slot,int id){CombatEnhancementsRegression.gear(p,slot,id);}
 static void flush()throws Exception {Field f=World.class.getDeclaredField("ticksToAdd");f.setAccessible(true);List<Tick> ticks=new ArrayList<Tick>((List<Tick>)f.get(World.getWorld()));((List<?>)f.get(World.getWorld())).clear();for(Tick t:ticks)t.execute();}
 public static void main(String[] args)throws Exception {
  Cache.init();ItemDefinition.init();NPCDefinition.init();org.dementhium.model.misc.GroundItemManager.load();Field area=World.class.getDeclaredField("areaManager");area.setAccessible(true);area.set(World.getWorld(),new org.dementhium.content.areas.AreaManager());
  Player a=p(),v=p();gear(v,5,13742);v.getRandom().setSeed(8491);
  for(CombatType type:new CombatType[]{CombatType.MELEE,CombatType.RANGE,CombatType.MAGIC})for(boolean prayer:new boolean[]{false,true}) {
   CombatBalanceRegression.prayers(v)[0][type.getProtectionPrayer()]=prayer;int proc=0,total=0;
   for(int i=0;i<4000;i++){v.getSkills().setHitPoints(1000);Damage d=Damage.getDamage(a,v,type,400);v.getDamageManager().damage(a,d,type.getDamageType());int dealt=1000-v.getHitPoints();int full=prayer?240:400,reduced=prayer?180:300;
    check(dealt==full||dealt==reduced,"Elysian exactly one reduction, "+type+" prayer="+prayer+" damage="+dealt);if(dealt==reduced)proc++;total+=dealt;v.getDamageManager().getHits().clear();}
   check(proc>2670&&proc<2930,"70 percent PvP procs: "+proc);check(v.getSkills().getPrayerPoints()==99,"Elysian consumes no prayer");System.out.println("PVP "+type+" protected="+prayer+" mean="+(total/4000.0)+" procs="+proc+"/4000");CombatBalanceRegression.prayers(v)[0][type.getProtectionPrayer()]=false;
  }
  gear(v,3,15486);v.setAttribute("staffOfLightEffect",World.getTicks()+100);check(Damage.getDamage(a,v,CombatType.MELEE,400).getHit()==200,"Staff halves damage while wielded");gear(v,3,4151);check(v.getAttribute("staffOfLightEffect",-1)==-1,"Staff removal clears actual attribute");v.setAttribute("staffOfLightEffect",World.getTicks()+100);check(Damage.getDamage(a,v,CombatType.MELEE,400).getHit()==400,"Stale staff effect cannot stack on shield");
  gear(v,5,13740);CombatBalanceRegression.prayers(v)[0][Prayer.PROTECT_FROM_MELEE]=true;v.getSkills().setPrayerPoints(99,false);v.getSkills().setHitPoints(1000);Damage d=Damage.getDamage(a,v,CombatType.MELEE,500);v.getDamageManager().damage(a,d,DamageType.MELEE);check(v.getHitPoints()==790,"Divine then PvP prayer 500->350->210");check(v.getSkills().getPrayerPoints()==91.5,"Divine cost uses pre-prayer damage");v.getDamageManager().damage(a,d,DamageType.MELEE);check(v.getHitPoints()==790,"Same hit cannot be applied twice");
  v.getSkills().setPrayerPoints(1,false);v.getSkills().setHitPoints(1000);d=Damage.getDamage(a,v,CombatType.MELEE,500);v.getDamageManager().damage(a,d,DamageType.MELEE);check(v.getHitPoints()==712&&v.getSkills().getPrayerPoints()==0,"Low Divine prayer 500->480->288");
  a=p();v=p();a.getSkills().setHitPoints(300);a.getSkills().setPrayerPoints(20,false);gear(a,3,11698);Interaction in=new Interaction(a,v);new HealingBlade().commenceSpecialAttack(in);check(a.getHitPoints()==300,"SGS no launch heal");in.getDamage().setHit(200);new HealingBlade().endSpecialAttack(in);check(a.getHitPoints()==400&&a.getSkills().getPrayerPoints()==25,"SGS actual impact restoration hp="+a.getHitPoints()+" prayer="+a.getSkills().getPrayerPoints());
  a=p();v=p();gear(a,3,1215);in=new Interaction(a,v);new Puncture().commenceSpecialAttack(in);in.getDamage().setHit(100);in.getSecondaryDamage().setHit(200);a.setAttribute("secondHit",new Damage(999));new Puncture().endSpecialAttack(in);check(v.getHitPoints()==900,"DDS only first hit on initial tick");flush();check(v.getHitPoints()==700,"DDS second hit is attack-owned and delayed");
  a=p();v=p();in=new Interaction(a,v);d=Damage.getDamage(a,v,CombatType.MELEE,300);SpecialHits.apply(in,d,DamageType.MELEE,1);v.setAttribute("godmode",true);flush();check(v.getHitPoints()==1000,"Delayed hit respects immunity");
  a=p();v=p();gear(a,3,14484);for(int i=0;i<1000;i++){in=new Interaction(a,v);new SliceAndDice().commenceSpecialAttack(in);check(in.getTargets().size()==4,"Claws always has four hits");int h0=in.getTargets().get(0).getDamage().getHit(),h1=in.getTargets().get(1).getDamage().getHit(),h2=in.getTargets().get(2).getDamage().getHit(),h3=in.getTargets().get(3).getDamage().getHit();if(h0>0)check(h1==h0/2&&h2+h3==h1,"Claws first-hit split");else if(h1>0)check(h2+h3==h1,"Claws second-hit split");else if(h2>0)check(h3==h2,"Claws third-hit split");}
  a=p();v=p();in=new Interaction(a,v);d=Damage.getDamage(a,v,CombatType.MELEE,500);v.getSkills().setHitPoints(10);double xp=a.getSkills().getXp(Skills.CONSTITUTION);SpecialHits.awardOnImpact(a,d,DamageType.MELEE);check(a.getSkills().getXp(Skills.CONSTITUTION)==xp,"No XP before impact");SpecialHits.apply(in,d,DamageType.MELEE,0);check(d.getHit()==10,"Overkill XP input actual loss");double after=a.getSkills().getXp(Skills.CONSTITUTION);check(after>xp,"XP awarded at impact");SpecialHits.apply(in,d,DamageType.MELEE,0);check(a.getSkills().getXp(Skills.CONSTITUTION)==after,"No duplicate XP");
    a=p();v=p();gear(a,3,19784);a.getRandom().setSeed(123);in=new Interaction(a,v);new Disrupt().commenceSpecialAttack(in);int unprotected=in.getDamage().getHit();CombatBalanceRegression.prayers(v)[0][Prayer.PROTECT_FROM_MAGIC]=true;a.getRandom().setSeed(123);Interaction protectedIn=new Interaction(a,v);new Disrupt().commenceSpecialAttack(protectedIn);check(protectedIn.getDamage().getHit()==(int)(unprotected*.6),"Korasi uses Magic protection in PvP");
  org.dementhium.model.npc.NPC large=new org.dementhium.model.npc.NPC(6260);in=new Interaction(a,large);check(new Disrupt().commenceSpecialAttack(in),"Korasi accepts large NPCs");
  a=p();v=p();in=new Interaction(a,v);d=Damage.getDamage(a,v,CombatType.MELEE,250);SpecialHits.apply(in,d,DamageType.MELEE,1);v.markInstanceTransition();flush();check(v.getHitPoints()==1000,"Departed context rejects pending special");
  a=p();v=p();gear(v,5,13742);gear(v,4,20135);int absorb=v.getBonuses().getAbsorptionBonus(0);check(absorb>0,"Torva absorption fixture");for(int i=0;i<50;i++){v.getSkills().setHitPoints(1000);d=Damage.getDamage(a,v,CombatType.MELEE,400);v.getDamageManager().damage(a,d,DamageType.MELEE);int dealt=1000-v.getHitPoints();check(dealt==300-(100*absorb/100)||dealt==400-(200*absorb/100),"Shield plus armour absorption stages");}
  System.out.println("PASS: "+checks+" special/shield checks, including 24,000 PvP hits");
 }
}






