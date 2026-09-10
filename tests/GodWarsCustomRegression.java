import java.lang.reflect.*;
import java.util.*;
import org.dementhium.model.*;
import org.dementhium.model.combat.*;
import org.dementhium.model.npc.godwars.*;
import org.dementhium.model.npc.godwars.GodWarsAction.Attack;
import org.dementhium.model.player.*;

/** Owner-selected fight rules: expected values deliberately independent of the enum. */
public class GodWarsCustomRegression {
 static int checks;
 static void check(boolean pass,String why){checks++;if(!pass)throw new AssertionError(why);}
 static int cap(GodWarsNPC n,Attack a)throws Exception {
  Method m=GodWarsAction.class.getDeclaredMethod("cap",Attack.class);m.setAccessible(true);
  return (Integer)m.invoke(new GodWarsAction(n),a);
 }
 static void caps()throws Exception {
  check(Attack.SARA_MELEE.cap==270&&Attack.SARA_MAGIC.cap==200,"Selected Saradomin caps");
  check(Attack.ZAM_MELEE.cap==460&&Attack.ZAM_SPECIAL.cap==490,"Selected Zamorak caps");
  for(GodWarsType t:GodWarsType.values()){
   GodWarsRegression.Fixture f=new GodWarsRegression.Fixture(t);
   List<GodWarsNPC> members=new ArrayList<GodWarsNPC>();members.add(f.boss);
   for(int id:t.followers)members.add(f.add(id));
   for(GodWarsNPC n:members){
    Attack a=n==f.boss?(t==GodWarsType.BANDOS?Attack.BANDOS_MELEE:t==GodWarsType.SARADOMIN?Attack.SARA_MELEE:t==GodWarsType.ZAMORAK?Attack.ZAM_MELEE:Attack.KREE_MELEE):GodWarsAction.choose(n,null);
    int base=cap(n,a),str=n.getCombatStats().base(Skills.STRENGTH);
    n.getCombatStats().drain(Skills.STRENGTH,str/2);
    check(cap(n,a)==(a.type==CombatType.MELEE?Math.max(1,(int)((long)base*n.getCombatLevel(Skills.STRENGTH)/str)):base),"Drained cap "+n.getId());
    Player p=f.player();p.setLocation(Location.locate(3200+n.size(),3200,0));
    GodWarsAction action=GodWarsRegression.launch(f,n,p,a);check(action.commenceSession(),"Drained launch "+n.getId());
    Damage d=action.getInteraction().getDamage();int captured=d.getMaximum();n.getCombatStats().reset();
    check(d.getMaximum()==captured,"Pending cap immutable across recovery");check(cap(n,a)==base,"Restored cap");
    n.getCombatStats().drain(Skills.STRENGTH,Integer.MAX_VALUE);
    check(cap(n,a)>=1&&cap(n,a)<=base,"Floor bounded");n.resetCombatState();check(cap(n,a)==base,"Combat reset restores cap");
   }
   if(t==GodWarsType.ZAMORAK){f.boss.getCombatStats().drain(Skills.STRENGTH,1000);check(cap(f.boss,Attack.ZAM_SPECIAL)==490,"Smash unaffected by Strength drain");check(cap(f.boss,Attack.ZAM_MAGIC)==300,"Magic unaffected by Strength drain");}
   f.room.close();
  }
 }
 static void poison()throws Exception {
  int misses=0,contacts=0,poison=0,protectedPoison=0;
  for(int seed=0;seed<512;seed++){
   GodWarsRegression.Fixture f=new GodWarsRegression.Fixture(GodWarsType.ZAMORAK);Player p=f.player();p.setLocation(Location.locate(3200+f.boss.size(),3200,0));
   f.boss.getRandom().setSeed(seed*7919L);p.getRandom().setSeed(seed*3571L);p.getSkills().setLevelAndXP(Skills.DEFENCE,300,13034431);
   BossBatchOneRegression.prayers(p)[0][CombatType.MELEE.getProtectionPrayer()]=true;
   GodWarsAction a=GodWarsRegression.launch(f,f.boss,p,Attack.ZAM_MELEE);check(a.commenceSession(),"Poison launch");
   int raw=a.getInteraction().getDamage().getHit();f.step(4);
   check(p.getHitPoints()==1000,"Protection prevents ordinary melee damage");
   if(raw<0){misses++;check(!p.getPoisonManager().isPoisoned(),"Accuracy miss cannot poison");}
   else {contacts++;if(p.getPoisonManager().isPoisoned()){poison++;protectedPoison++;Field amount=p.getPoisonManager().getClass().getDeclaredField("amount");amount.setAccessible(true);check(amount.getInt(p.getPoisonManager())==160,"Selected poison strength");}}
   f.room.close();
  }
  check(misses>20&&contacts>20,"Exercise misses and contacts: "+misses+" / "+contacts);
  check(poison>contacts*0.15&&poison<contacts*0.35&&protectedPoison>0,"One-in-four poison on protected contacts");
 }
 static void targets(){
  Set<Mob> selected=Collections.newSetFromMap(new IdentityHashMap<Mob,Boolean>());int split=0;
  for(int seed=0;seed<64;seed++){
   GodWarsRegression.Fixture f=new GodWarsRegression.Fixture(GodWarsType.ZAMORAK);
   Player killer=f.player(),other=f.player(),outside=f.player(),hidden=f.player();outside.setLocation(Location.locate(3300,3300,0));hidden.setHidden(true);
   List<GodWarsNPC> guards=new ArrayList<GodWarsNPC>();for(int id:GodWarsType.ZAMORAK.followers){GodWarsNPC n=f.add(id);n.getRandom().setSeed(seed*7919L+id*104729L);guards.add(n);}
   f.boss.setDead(true);f.room.onDeath(f.boss,seed%2==0?killer:null);
   Set<Mob> perDeath=Collections.newSetFromMap(new IdentityHashMap<Mob,Boolean>());
   for(GodWarsNPC n:guards){Mob victim=n.getCombatExecutor().getVictim();check(victim==killer||victim==other,"Eligible independent target with or without killer");perDeath.add(victim);selected.add(victim);}
   if(perDeath.size()>1)split++;
   f.room.close();
  }
  check(split>10&&selected.size()>2,"Followers do not all copy kill credit");
  GodWarsRegression.Fixture f=new GodWarsRegression.Fixture(GodWarsType.ZAMORAK);GodWarsNPC n=f.add(6204);f.boss.setDead(true);f.room.onDeath(f.boss,null);check(n.getCombatExecutor().getVictim()==null,"Empty room has no target");f.room.close();
  f=new GodWarsRegression.Fixture(GodWarsType.SARADOMIN);Player killer=f.player();n=f.add(6248);f.boss.setDead(true);f.room.onDeath(f.boss,killer);check(n.getCombatExecutor().getVictim()==killer,"Saradomin retains killer focus");f.room.close();
 }
 static void prayerSelection()throws Exception {
  GodWarsRegression.Fixture f=new GodWarsRegression.Fixture(GodWarsType.ZAMORAK);Player p=f.player();
  boolean[][] prayers=BossBatchOneRegression.prayers(p);prayers[0][CombatType.RANGE.getProtectionPrayer()]=true;
  for(int i=0;i<1000;i++)check(GodWarsAction.choose(f.boss,p)!=Attack.ZAM_SPECIAL,"Ranged prayer cannot trigger smash");
  prayers[0][CombatType.RANGE.getProtectionPrayer()]=false;prayers[1][9]=true;
  int special=0;for(int i=0;i<1000;i++)if(GodWarsAction.choose(f.boss,p)==Attack.ZAM_SPECIAL)special++;
  check(special>30,"Deflect melee can trigger smash");f.room.close();
 }
 public static void main(String[] args)throws Exception {
  GodWarsRegression.main(new String[]{"death-only"});caps();poison();targets();prayerSelection();
  System.out.println("God Wars custom: "+checks+" checks passed.");
 }
}
