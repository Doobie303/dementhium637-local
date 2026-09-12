import java.lang.reflect.*;
import java.util.*;
import org.dementhium.model.*;
import org.dementhium.model.combat.*;
import org.dementhium.model.npc.impl.Nex;
import org.dementhium.model.npc.impl.Nex.*;
import org.dementhium.model.player.*;
import org.dementhium.tickable.Tick;
public class NexDamageRegression {
 static int checks;static void check(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 static void field(Object o,String name,Object v)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);f.set(o,v);}
 static class F {
  Nex n;Player p;CombatAction a;
  F(NexPhase phase)throws Exception{CombatFixtures.clearPlayers();n=new Nex(13447);n.setLocation(Location.locate(2924,5202,0));n.setOriginalLocation(n.getLocation());field(n,"phase",phase);field(NexAreaEvent.getNexAreaEvent(),"nex",n);n.setHp(10000);p=CombatFixtures.player(n);p.setLocation(Location.locate(2926,5202,0));World.getWorld().getPlayers().add(p);a=n.getCombatAction().newSession();CombatFixtures.readyAttack(n,p);}
  Damage hit(int raw,CombatType type,NexPhase phase,boolean ss)throws Exception{Method m=a.getClass().getDeclaredMethod("applyAttackDamage",Mob.class,int.class,int.class,CombatType.class,NexPhase.class,boolean.class);m.setAccessible(true);return (Damage)m.invoke(a,p,raw,760,type,phase,ss);}
 }
 static void effects()throws Exception{
  for(NexPhase phase:new NexPhase[]{NexPhase.BLOOD,NexPhase.FINAL,NexPhase.SMOKE,NexPhase.ICE})for(int mode=0;mode<7;mode++)for(int raw:new int[]{-1,0,49,500}){
   F f=new F(phase);if(mode==1){f.p.getEquipment().set(5,new Item(13740));f.p.getBonuses().calculate();}if(mode==2)BossBatchOneRegression.prayers(f.p)[0][CombatType.MAGIC.getProtectionPrayer()]=true;if(mode==3)f.p.setAttribute("godmode",true);if(mode==4)f.p.getSkills().setHitPoints(25);if(mode==5){f.p.getEquipment().set(4,new Item(20135));f.p.getBonuses().calculate();}if(mode==6)f.p.setOnline(false);
   int hp=f.p.getHitPoints();Damage d=f.hit(raw,CombatType.MAGIC,phase,phase==NexPhase.FINAL);int lost=hp-f.p.getHitPoints();check(lost>=0&&lost<=Math.max(0,raw)&&lost<=hp,"Resolved damage cap");int heal=phase==NexPhase.BLOOD?Math.round(lost*.1F):phase==NexPhase.FINAL?lost/5:0;check(f.n.getHp()==10000+heal,"Healing uses actual LP "+phase+":"+mode+":"+raw);check(f.p.getSkills().getLevel(Skills.ATTACK)==99-(phase==NexPhase.FINAL&&lost>0?1:0),"Turmoil positive only");if(lost==0)check(!f.p.getPoisonManager().isPoisoned(),"No zero/miss/immune poison");if(mode!=1)check(f.p.getSkills().getPrayerPoints()==99-(phase==NexPhase.FINAL?lost/50:0),"Soul Split actual drain");
  }
  F f=new F(NexPhase.BLOOD);f.hit(500,CombatType.MELEE,NexPhase.BLOOD,false);check(f.n.getHp()==10000,"Blood melee does not leech");check(f.a.newSession()!=f.a,"Attack sessions independent");
 }
 @SuppressWarnings("unchecked") static List<Tick> queue()throws Exception{Field f=World.class.getDeclaredField("ticksToAdd");f.setAccessible(true);return (List<Tick>)f.get(World.getWorld());}
 static void delayed()throws Exception{
  int positive=0;
  for(int mode=0;mode<7;mode++)for(int seed=0;seed<16;seed++){
   F f=new F(NexPhase.BLOOD);f.p.setLocation(Location.locate(2931,5202,0));f.p.getRandom().setSeed(seed);f.n.getRandom().setSeed(seed);queue().clear();f.a.setInteraction(new Interaction(f.n,f.p));f.a.commenceSession();List<Tick> pending=new ArrayList<Tick>(queue());check(!pending.isEmpty()&&f.p.getHitPoints()==1000,"Delayed auto launch");
   if(mode==1)f.p.setOnline(false);if(mode==2)f.n.resetCombatState();if(mode==3)f.p.markInstanceTransition();if(mode==4)field(NexAreaEvent.getNexAreaEvent(),"nex",new Nex(13447));if(mode==5)f.p.setLocation(Location.locate(3200,3200,0));if(mode==6)field(f.n,"phase",NexPhase.ICE);
   for(int i=0;i<3;i++)for(Tick t:pending)t.run();int lost=1000-f.p.getHitPoints();if(mode>=1&&mode<=5)check(lost==0&&f.n.getHp()==10000,"Stale ranged impact cancelled "+mode);else{check(f.n.getHp()==10000+Math.round(lost*.1F),"Captured blood phase uses actual impact");if(lost>0)positive++;}
  }check(positive>0,"Actual ranged dispatch positive coverage");
 }
 static void sacrifice()throws Exception{
  for(int mode=0;mode<5;mode++){
   F f=new F(NexPhase.BLOOD);if(mode==1)f.p.setAttribute("godmode",true);if(mode==2)f.p.getSkills().setHitPoints(25);Method m=NexAreaEvent.class.getDeclaredMethod("bloodAttack",boolean.class);m.setAccessible(true);check((Boolean)m.invoke(NexAreaEvent.getNexAreaEvent(),false),"Sacrifice launch");Tick t=f.n.getTick("blood_sacrifice");if(t==null){f.n.processTicks();t=f.n.getTick("blood_sacrifice");}check(t!=null,"Sacrifice owned timer");int hp=f.p.getHitPoints();if(mode==3)f.n.resetCombatState();if(mode==4)field(NexAreaEvent.getNexAreaEvent(),"nex",new Nex(13447));int start=World.getTicks();for(int i=0;i<7;i++){Field clock=World.class.getDeclaredField("ticksPassed");clock.setAccessible(true);clock.setInt(null,start+i+1);t.run();}int lost=hp-f.p.getHitPoints();check(f.n.getHp()==10000+lost,"Sacrifice heals actual LP");if(mode==1||mode>=3)check(lost==0,"Immune/stale sacrifice");if(mode==2)check(lost==25,"Sacrifice overkill clamped");
  }
 }
 public static void main(String[] args)throws Exception{CombatFixtures.init();effects();delayed();sacrifice();System.out.println("Nex damage: "+checks+" checks passed.");}
}


