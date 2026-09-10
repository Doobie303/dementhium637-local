import java.lang.reflect.*;
import java.util.*;
import org.dementhium.model.*;
import org.dementhium.model.combat.*;
import org.dementhium.model.combat.impl.*;
import org.dementhium.model.combat.impl.spells.NPCSpell;
import org.dementhium.model.misc.DamageManager.DamageType;
import org.dementhium.model.npc.*;
import org.dementhium.model.npc.impl.Nex;
import org.dementhium.model.npc.godwars.*;
import org.dementhium.model.player.*;
import org.dementhium.task.impl.NPCTickTask;
import org.dementhium.tickable.Tick;

/** Standalone lifecycle tests. Loot recorders never create real ground items or save accounts. */
public class NPCDeathRewardRegression {
 static int checks;
 static void check(boolean value,String reason){checks++;if(!value)throw new AssertionError(reason);}
 static Object field(Object o,Class<?> c,String name)throws Exception{Field f=c.getDeclaredField(name);f.setAccessible(true);return f.get(o);}
 static void field(Object o,Class<?> c,String name,Object value)throws Exception{Field f=c.getDeclaredField(name);f.setAccessible(true);f.set(o,value);}
 static final List<Tick> active=new ArrayList<Tick>();
 @SuppressWarnings("unchecked") static void worldTick()throws Exception{
  LinkedList<Tick> queue=(LinkedList<Tick>)field(World.getWorld(),World.class,"ticksToAdd");active.addAll(queue);queue.clear();
  for(Iterator<Tick> it=active.iterator();it.hasNext();)if(!it.next().run())it.remove();
 }
 static void ticks(int n)throws Exception{for(int i=0;i<n;i++)worldTick();}
 @SuppressWarnings("unchecked") static void clean()throws Exception{
  CombatFixtures.clearPlayers();active.clear();((LinkedList<Tick>)field(World.getWorld(),World.class,"ticksToAdd")).clear();
 }
 static void place(NPC n){n.setLocation(Location.locate(3216,3216,0));n.setOriginalLocation(n.getLocation());n.setDoesWalk(false);}
 static class LootNPC extends NPC{
  int calls;Mob paid;LootNPC(){super(1);place(this);}public void loot(Mob m){calls++;paid=m;}
 }
 static void credit()throws Exception{
  for(boolean typed:new boolean[]{false,true})for(boolean two:new boolean[]{false,true}){
   clean();LootNPC n=new LootNPC();Player a=CombatFixtures.player(n),b=CombatFixtures.player(n);n.setHp(30);
   if(two)n.getDamageManager().damage(a,10,30,DamageType.MELEE);Player winner=two?b:a;
   if(typed)n.getDamageManager().damage(winner,Damage.getDamage(winner,n,CombatType.MELEE,100),DamageType.MELEE);
   else n.getDamageManager().damage(winner,100,100,DamageType.MELEE);
   check(n.isDead(),"Lethal hit enters death");check(n.getDamageManager().getKiller()==winner,"Final contribution wins");
   check(n.getDamageManager().getEnemyHits().get(winner)==(two?20:30),"Only actual damage credited once");
   ticks(n.getDeathTick());check(n.calls==1&&n.paid==winner,"Death captured complete contribution before HP mutation");
   ticks(5);check(n.calls==1,"No duplicate loot");
  }
  clean();LootNPC n=new LootNPC();Player p=CombatFixtures.player(n);n.setHp(50);
  n.getDamageManager().damage(p,0,100,DamageType.MELEE);n.getDamageManager().damage(p,20,20,DamageType.HEAL);
  check(n.getDamageManager().getEnemyHits().isEmpty(),"Zero and healing never earn damage credit");
 }
 static void deathLeash()throws Exception{
  for(int offset:new int[]{12,13}){
   clean();LootNPC n=new LootNPC();n.setLocation(n.getOriginalLocation().transform(offset,0,0));Player p=CombatFixtures.player(n);n.setHp(30);
   n.getDamageManager().damage(p,30,30,DamageType.MELEE);long life=n.getCombatGeneration();new NPCTickTask(n).execute();
   check(!n.isReturningHome()&&life==n.getCombatGeneration(),"Corpse cannot enter leash or invalidate death callbacks");
   for(int t=0;t<80;t++){worldTick();new NPCTickTask(n).execute();}
   check(n.calls==1&&n.paid==p&&!n.isDead()&&n.getHp()==n.getMaxHp(),"Loot and respawn survive full corpse task progression");
   check(n.getDamageManager().getEnemyHits().isEmpty()&&!n.isDeathRewardComplete(),"Revived life starts without previous credit/completion");
   n.getDamageManager().damage(p,1000,1000,DamageType.MELEE);ticks(n.getDeathTick());check(n.calls==2,"Second life earns one independent reward");
  }
  clean();LootNPC n=new LootNPC();World.getWorld().getNpcs().add(n);n.setUnrespawnable(true);n.setLocation(n.getOriginalLocation().transform(13,0,0));Player p=CombatFixtures.player(n);
  n.getDamageManager().damage(p,1000,1000,DamageType.MELEE);new NPCTickTask(n).execute();ticks(80);
  check(n.calls==1&&!World.getWorld().getNpcs().contains(n),"Unrespawnable corpse rewards before removal");
 }
 static Player recoil(NPC n){Player p=CombatFixtures.player(n);p.getEquipment().set(Equipment.SLOT_RING,new Item(2550));p.getSettings().setRecoilDamage(400);return p;}
 static void reflection()throws Exception{
  for(int style=0;style<3;style++){
   clean();LootNPC n=new LootNPC();n.setHp(10);Player p=recoil(n);Interaction i=new Interaction(n,p);
   Damage d=Damage.getDamage(n,p,style==0?CombatType.MELEE:style==1?CombatType.RANGE:CombatType.MAGIC,100);i.setDamage(d);
   if(style==0){MeleeAction a=new MeleeAction();a.setInteraction(i);a.endSession();}
   else if(style==1){RangeData r=new RangeData(false);r.setDamage(d);i.setRangeData(r);RangeAction a=new RangeAction();a.setInteraction(i);a.endSession();}
   else new NPCSpell().endSpell(i);
   ticks(n.getDeathTick());check(n.isDead()&&n.calls==1&&n.paid==p,"Reflection-only kill preserves owner, style="+style);
   check(p.getSettings().getRecoilDamage()==390,"Single reflection consumes exactly ten charges");
  }
  clean();LootNPC n=new LootNPC(){final CombatAction a=new MeleeAction(){@Override public boolean commenceSession(){interaction.setDamage(Damage.getDamage(interaction.getSource(),interaction.getVictim(),CombatType.MELEE,100));interaction.getSource().getCombatExecutor().setTicks(4);return true;}};@Override public CombatAction getCombatAction(){return a;}};
  n.setHp(10);Player p=recoil(n);n.getCombatExecutor().setVictim(p);
  // Real executor/session order and initial cooldown; the roll alone is controlled.
  for(int t=0;t<8&&!n.isDead();t++)n.getCombatExecutor().tick();
  ticks(n.getDeathTick());check(n.calls==1&&n.paid==p,"Executor reflection kill has correct loot owner");
 }
 static class LootNex extends Nex{
  int calls;Mob paid;LootNex(){super(DEFAULT_NEX_ID);place(this);}public void loot(Mob m){calls++;paid=m;}
 }
 static void nex()throws Exception{
  clean();LootNex n=new LootNex();field(n,Nex.class,"phase",Nex.NexPhase.FINAL);n.setHp(30);n.setUnrespawnable(true);
  n.setLocation(Location.locate(2924,5202,0));n.setOriginalLocation(n.getLocation());
  org.dementhium.model.map.Region region=n.getLocation().getRegion();region.clippingMasks=new int[4][128][128];region.setClipped(true);World.getWorld().getNpcs().add(n);
  Nex.NexAreaEvent event=Nex.NexAreaEvent.getNexAreaEvent();field(event,Nex.NexAreaEvent.class,"nex",n);
  Player p=CombatFixtures.player(n);World.getWorld().getPlayers().add(p);n.getCombatExecutor().setVictim(p);NPCCombatContext old=new NPCCombatContext(n,p);
  n.getDamageManager().damage(p,30,30,DamageType.MELEE);
  check(n.isDying()&&!n.isDead()&&!n.isAttackable(),"Wrath is terminal but retains its three-tick tell");
  check(!old.isCurrent()&&!NPCCombatContext.validPair(n,p)&&n.getCombatExecutor().getVictim()==null,"Wrath cancels pending combat and target");
  n.heal(500);n.hit(1);check(n.getHp()==0,"Wrath cannot heal or schedule another lethal transition");
  n.setLocation(n.getOriginalLocation().transform(13,0,0));p.setLocation(n.getLocation().transform(2,0,0));new NPCTickTask(n).execute();check(!n.isReturningHome(),"Wrath cannot start leash recovery");
  for(int t=1;t<=3;t++){worldTick();event.execute();check(event.getNex()==n,"Arena retains wrath/death owner");if(t<3){check(!n.isDead(),"Wrath does not finish early");check(p.getHitPoints()==1000,"No early wrath damage");}}
  int afterWrath=p.getHitPoints();check(afterWrath>=700&&afterWrath<=900,"Wrath deals its retained 100-300 LP once at tick three");
  check(n.isDead()&&!n.isDying()&&n.calls==0,"Wrath enters one death, reward still delayed");
  for(int t=1;t<=n.getDeathTick();t++){worldTick();event.execute();if(t<n.getDeathTick())check(n.calls==0&&event.getNex()==n,"Arena waits for due reward");}
  check(n.calls==1&&n.paid==p&&event.getNex()==null&&!World.getWorld().getNpcs().contains(n),"Loot once before arena disposal");
  ticks(15);event.execute();check(n.calls==1&&p.getHitPoints()==afterWrath,"Later ticks cannot repeat wrath or reward");
 }
 public static void main(String[] args)throws Exception{CombatFixtures.init();credit();deathLeash();reflection();nex();clean();System.out.println("NPC death/reward: "+checks+" checks passed");}
}
