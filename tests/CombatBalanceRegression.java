import java.lang.reflect.*;
import java.util.*;
import org.dementhium.cache.Cache;
import org.dementhium.content.skills.Prayer;
import org.dementhium.model.*;
import org.dementhium.model.combat.*;
import org.dementhium.model.combat.impl.RangeAction;
import org.dementhium.model.combat.impl.spells.ancient.BloodRush;
import org.dementhium.model.definition.*;
import org.dementhium.model.misc.DamageManager.DamageType;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.player.*;
import org.dementhium.net.GameSession;
public class CombatBalanceRegression {
 static int checks;
 static void check(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 static Player player(){Player p=new Player(new GameSession(null),new PlayerDefinition("balance","unused"));p.getSkills().setMaximumLifePoints(1000);p.getSkills().setHitPoints(1000);return p;}
 static boolean[][] prayers(Player p)throws Exception{Field f=Prayer.class.getDeclaredField("onPrayers");f.setAccessible(true);return (boolean[][])f.get(p.getPrayer());}
 static void gear(Player p,int slot,int id){p.getEquipment().set(slot,id<0?null:new Item(id));p.getBonuses().calculate();}
 public static void main(String[] args)throws Exception {
  Cache.init();ItemDefinition.init();NPCDefinition.init();
  Player p=player();p.getSkills().setLevel(Skills.RANGED,99);gear(p,3,9185);gear(p,13,9144);
  check(p.getBonuses().getBonus(Bonuses.RANGED_ATTACK)==90,"Rune crossbow accuracy");
  check(p.getBonuses().getBonus(Bonuses.RANGED)==115,"Rune bolt strength, no phantom crossbow strength");
  gear(p,4,2503);check(p.getBonuses().getBonus(Bonuses.RANGED_ATTACK)==120,"Armour adds accuracy");
  int[] expected={14,30,48,66,83,85,103,105,117,120};
  for(int i=0;i<10;i++){gear(p,13,9236+i);check(p.getBonuses().getBonus(Bonuses.RANGED)==expected[i],"Enchanted bolt "+(9236+i));}
  gear(p,4,-1);gear(p,3,861);gear(p,13,892);
  check(p.getBonuses().getBonus(Bonuses.RANGED)==49,"Magic shortbow with rune arrows");
  check(p.getBonuses().getBonus(Bonuses.RANGED_ATTACK)==69,"Magic shortbow accuracy");
  int previous=0;for(int id:new int[]{882,884,886,888,890,892}){gear(p,13,id);int hit=RangeFormulae.getRangeDamage(p,1);check(hit>previous,"Arrow upgrade increases max hit");previous=hit;}
  gear(p,3,20171);int zaryte=p.getBonuses().getBonus(Bonuses.RANGED);gear(p,13,9245);
  check(zaryte==115&&p.getBonuses().getBonus(Bonuses.RANGED)==115,"Zaryte ignores quivered bolts");
  p.getBonuses().calculate(p);check(p.getBonuses().getBonus(Bonuses.RANGED)==115,"Both bonus overloads agree");
  gear(p,3,4734);gear(p,13,4740);check(p.getBonuses().getBonus(Bonuses.RANGED)==55,"Karil rack strength");
  Random a=new Random(5),d=new Random(6),r=new Random(7);int hits=0,strongHits=0;long sum=0,hitSum=0,strongSum=0;
  for(int i=0;i<200000;i++){
   boolean landed=CombatRolls.hits(a,100,d,100);boolean strong=CombatRolls.hits(a,300,d,100);int hit=CombatRolls.roll(r,100);
   sum+=hit;if(landed){hits++;hitSum+=hit;}if(strong){strongHits++;strongSum+=hit;}
  }
  check(Math.abs(hits/200000.0-50.0/101)<0.005,"Uniform independent accuracy, defender wins ties");
  check(strongHits>hits,"Higher accuracy increases hit rate");
  check(Math.abs(sum/200000.0-50)<0.3,"Uniform damage mean");
  check(Math.abs(hitSum/(double)hits-strongSum/(double)strongHits)<0.5,"Damage mean independent of accuracy");
  check(!CombatRolls.hits(a,0,d,0),"Tie misses");
  check(CombatRolls.roll(a,-20)==0,"Negative roll bounds safe");
  Player attacker=player(),victim=player();
  victim.getSkills().setLevel(Skills.DEFENCE,99);victim.getSkills().setLevel(Skills.MAGIC,99);
  java.lang.reflect.Method magicDefence=MagicFormulae.class.getDeclaredMethod("getMaximumDefence",Player.class,Mob.class,MagicSpell.class);magicDefence.setAccessible(true);
  gear(victim,4,1127);double plateRange=RangeFormulae.getDefence(attacker,victim,1);double plateMelee=MeleeFormulae.getMeleeDefence(attacker,victim,1);
  double plateMagic=(Double)magicDefence.invoke(null,attacker,victim,new BloodRush());
  gear(victim,4,4091);check(plateRange>RangeFormulae.getDefence(attacker,victim,1),"Plate resists ranged better than robes");
  check(plateMelee>MeleeFormulae.getMeleeDefence(attacker,victim,1),"Plate resists melee better than robes");
  gear(victim,4,2503);check((Double)magicDefence.invoke(null,attacker,victim,new BloodRush())>plateMagic,"Dragonhide resists magic better than plate");
  gear(victim,4,-1);
  check(Damage.getDamage(attacker,victim,CombatType.MAGIC,-1).getHit()==-1,"Magic splash sentinel retained");
  int[] absorption=ItemDefinition.forId(1127).getAbsorptionBonus();int[] saved=absorption.clone();
  try{
   absorption[0]=20;absorption[1]=20;absorption[2]=20;gear(victim,4,1127);
   for(CombatType style:new CombatType[]{CombatType.MELEE,CombatType.RANGE,CombatType.MAGIC}){
    prayers(victim)[0][style.getProtectionPrayer()]=true;
    check(Damage.getDamage(attacker,victim,style,500).getHit()==300,"PvP protection "+style);
    check(Damage.getDamage(new NPC(1),victim,style,500).getHit()==0,"PvM protection "+style);
    prayers(victim)[0][style.getProtectionPrayer()]=false;
   }
   attacker.getSkills().setHitPoints(500);victim.getSkills().setPrayerPoints(100,true);prayers(attacker)[1][Prayer.SOUL_SPLIT]=true;
   Damage hit=Damage.getDamage(attacker,victim,CombatType.MELEE,500);
   check(attacker.getHitPoints()==500,"Soul Split does not heal at launch");
   victim.getDamageManager().damage(attacker,hit,DamageType.MELEE);
   check(attacker.getHitPoints()==588,"Soul Split heals one fifth of actual PvP damage");
   check(Math.abs(victim.getSkills().getPrayerPoints()-91.2)<0.01,"Soul Split LP/prayer conversion");
   victim.setAttribute("godmode",true);victim.getDamageManager().damage(attacker,new Damage(500),DamageType.MELEE);
   check(attacker.getHitPoints()==588,"No Soul Split healing from immune targets");victim.removeAttribute("godmode");
   prayers(attacker)[1][Prayer.SOUL_SPLIT]=false;prayers(attacker)[0][Prayer.SMITE]=true;
   victim.getDamageManager().damage(attacker,new Damage(500),DamageType.MELEE);
   check(Math.abs(victim.getSkills().getPrayerPoints()-80.2)<0.01,"Smite uses actual damage");
   prayers(attacker)[0][Prayer.SMITE]=false;
   victim.getSkills().setHitPoints(1000);attacker.getSkills().setHitPoints(500);
   Interaction inter=new Interaction(attacker,victim);inter.setDamage(new Damage(500));inter.setEndGraphic(org.dementhium.model.mask.Graphic.create(373));
   new BloodRush().endSpell(inter);check(attacker.getHitPoints()==610,"Blood heal uses absorbed impact damage");
   victim.getSkills().setHitPoints(1000);victim.getEquipment().set(Equipment.SLOT_RING,new Item(2550));victim.getSettings().setRecoilDamage(400);
   NPC ranger=new NPC(1);ranger.setHp(500);RangeData data=new RangeData(false);data.setWeaponType(0);data.setDamage(new Damage(500));
   Interaction ranged=new Interaction(ranger,victim);ranged.setRangeData(data);RangeAction action=new RangeAction();action.setInteraction(ranged);action.endSession();
   check(ranger.getHitPoints()==456,"Ranged recoil applies recoil value, not deflect value");
   check(MagicFormulae.getMaximumMagicDamage(ranger,1)>=0,"NPC 14-entry bonus schema preserved");
  }finally{System.arraycopy(saved,0,absorption,0,3);}
  System.out.println("PASS: "+checks+" ranged, roll-distribution, prayer and reflection checks (200,000 samples)");
 }
}
