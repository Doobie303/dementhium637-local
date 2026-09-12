import java.lang.reflect.*;
import java.util.*;
import org.dementhium.model.*;
import org.dementhium.model.combat.*;
import org.dementhium.model.combat.impl.spells.modern.*;
import org.dementhium.model.combat.impl.spells.ancient.*;
import org.dementhium.model.misc.DamageManager.DamageType;
import org.dementhium.model.npc.*;
import org.dementhium.model.npc.encounter.*;
import org.dementhium.model.npc.impl.*;
import org.dementhium.model.player.*;
import org.dementhium.task.impl.NPCTickTask;
import org.dementhium.tickable.Tick;

/** Isolated fixtures and production entry paths; no real rewards/accounts/network server. */
public class SharedCombatRepairRegression {
 static int checks;
 static void check(boolean ok,String reason){checks++;if(!ok)throw new AssertionError(reason);}
 static Object get(Object o,Class<?> c,String n)throws Exception{Field f=c.getDeclaredField(n);f.setAccessible(true);return f.get(o);}
 static void set(Object o,Class<?> c,String n,Object v)throws Exception{Field f=c.getDeclaredField(n);f.setAccessible(true);f.set(o,v);}
 static Object call(Object o,String n,Class<?>[] types,Object...args)throws Exception{Method m=o.getClass().getDeclaredMethod(n,types);m.setAccessible(true);return m.invoke(o,args);}
 static final List<Tick> active=new ArrayList<Tick>();
 @SuppressWarnings("unchecked") static void worldTicks(int count)throws Exception{
  for(int i=0;i<count;i++){LinkedList<Tick> q=(LinkedList<Tick>)get(World.getWorld(),World.class,"ticksToAdd");active.addAll(q);q.clear();for(Iterator<Tick> it=active.iterator();it.hasNext();)if(!it.next().run())it.remove();}
 }
 @SuppressWarnings("unchecked") static void clean()throws Exception{CombatFixtures.clearPlayers();active.clear();((LinkedList<Tick>)get(World.getWorld(),World.class,"ticksToAdd")).clear();set(null,World.class,"ticksPassed",0);}
 static NPC npc(){NPC n=new NPC(1);place(n);return n;}
 static void place(NPC n){n.setLocation(Location.locate(3216,3216,0));n.setOriginalLocation(n.getLocation());n.setDoesWalk(false);}
 static Player recoil(NPC n){Player p=CombatFixtures.player(n);p.getEquipment().set(Equipment.SLOT_RING,new Item(2550));p.getSettings().setRecoilDamage(400);return p;}
 static Object curse(Player p,int id)throws Exception{return call(p.getPrayer(),"createCurse",new Class<?>[]{int.class},id);}
 static void curse(Object c,Mob v)throws Exception{call(c,"curse",new Class<?>[]{Mob.class},v);}
 static void reset()throws Exception{
  clean();NPC n=npc();World.getWorld().getNpcs().add(n);Player a=CombatFixtures.player(n),b=CombatFixtures.player(n);n.setHp(100);n.getDamageManager().damage(a,50,100,DamageType.MELEE);
  n.getPoisonManager().poison(a,20);n.setAttribute("freezeTime",10);n.setAttribute("freezeImmunity",15);n.setAttribute("miasmicTime",12);n.setAttribute("miasmicImmunity",27);
  Object first=curse(a,org.dementhium.content.skills.Prayer.LEECH_ATTACK),second=curse(b,org.dementhium.content.skills.Prayer.LEECH_ATTACK);
  for(int i=0;i<4;i++){curse(first,n);curse(second,n);}check(n.getAttackModifier()<0,"Two casters drain NPC");
  n.setLocation(n.getOriginalLocation().transform(13,0,0));new NPCTickTask(n).execute();int t=1;
  while(n.isReturningHome()&&t++<40)new NPCTickTask(n).execute();
  check(!n.isReturningHome()&&n.getHp()==100,"Real leash returns and restores LP");
  check(n.getDamageManager().getEnemyHits().isEmpty()&&!n.getPoisonManager().isPoisoned(),"Reset discards old credit and poison");
  check(n.getAttribute("freezeTime",-1)==-1&&n.getAttribute("miasmicImmunity",-1)==-1,"Reset clears old status and immunity state");
  check(n.getAttackModifier()==0&&a.getPrayer().getAttackModifier()==0&&b.getPrayer().getAttackModifier()==0,"Owned curse drains and boosts released on reset");
  call(first,"deactivate",new Class<?>[]{});call(second,"deactivate",new Class<?>[]{});check(n.getAttackModifier()==0,"Later deactivation cannot over-restore");
  worldTicks(30);check(n.getHp()==100,"Old poison stream cannot damage restored fight");
  n.getPoisonManager().poison(b,20);worldTicks(30);check(n.getHp()==80&&n.getDamageManager().getKiller()==b,"Fresh poison owns fresh fight");
  n.stun(2,null,false);n.resetCombatState();n.stun(5,null,false);worldTicks(2);check(n.getAttribute("stunned",false),"Old stun expiry cannot clear new stun");worldTicks(3);check(!n.getAttribute("stunned",false),"Current stun expires");World.getWorld().getNpcs().remove(n);
 }
 static void vengeance()throws Exception{
  for(String transition:new String[]{"none","reset","empty","teleport","logout","death","instance"}){
   clean();CorporealBeast n=new CorporealBeast(8133);place(n);n.bindArena(3190,3190,3250,3250,0);Player p=CombatFixtures.player(n);
   p.setAttribute("vengeance",true);p.submitVengeance(n,30);int before=n.getHp();check(before==20000,"No immediate Vengeance");
   if(transition.equals("reset"))n.resetEncounter();
   if(transition.equals("empty")){n.setHp(19999);n.getCombatExecutor().setVictim(p);p.setOnline(false);for(int t=0;t<10;t++)new NPCTickTask(n).execute();check(n.getCombatGeneration()>0,"Empty-room entry really reset");}
   if(transition.equals("teleport")){Location old=p.getLocation();p.teleport(old.transform(1,0,0),false);p.teleport(old,false);}
   if(transition.equals("logout")){p.setOnline(false);p.setOnline(true);}
   if(transition.equals("death")){p.getSkills().hit(1000);p.getSkills().setHitPoints(1000);}
   if(transition.equals("instance"))p.markInstanceTransition();
   worldTicks(1);check(n.getHp()==before-(transition.equals("none")?30:0),"Vengeance lifetime "+transition);worldTicks(2);check(n.getHp()==before-(transition.equals("none")?30:0),"No repeated Vengeance "+transition);n.destroy();
  }
 }
 static void reflection()throws Exception{
  clean();NPC n=npc();n.setHp(100);Player p=recoil(n);p.setAttribute("vengeance",true);Damage d=Damage.getDamage(n,p,CombatType.MELEE,100);
  p.getDamageManager().damage(n,d,DamageType.MELEE);check(n.getHp()==90&&p.getHitPoints()==900&&p.getSettings().getRecoilDamage()==390,"Typed impact delivers recoil once");
  check(n.getDamageManager().getKiller()==p,"Reflector keeps credit");CombatReflection.deliver(n,p,d);p.getDamageManager().damage(n,d,DamageType.MELEE);check(n.getHp()==90&&p.getSettings().getRecoilDamage()==390,"Replay cannot reflect or debit charges");
  worldTicks(1);check(n.getHp()==15&&!p.getAttribute("vengeance",true),"One owned Vengeance at next tick");
  clean();n=npc();n.setHp(7);p=recoil(n);d=Damage.getDamage(n,p,CombatType.MELEE,100);d.setDeflected(5);p.getDamageManager().damage(n,d,DamageType.MELEE);
  check(n.getHp()==0&&d.getRecoiled()==2&&p.getSettings().getRecoilDamage()==398,"Deflect leaves only two deliverable recoil LP/charges");
  clean();final NPC resetNpc=npc();p=recoil(resetNpc);d=Damage.getDamage(resetNpc,p,CombatType.MELEE,100);d.onImpact(actual->resetNpc.resetCombatState());p.getDamageManager().damage(resetNpc,d,DamageType.MELEE);
  check(resetNpc.getHp()==100&&p.getSettings().getRecoilDamage()==400,"Reset during effect cannot spend charges on a new life");
  clean();n=npc();p=recoil(n);p.setAttribute("godmode",true);int hp=n.getHp();p.getDamageManager().damage(n,Damage.getDamage(n,p,CombatType.MELEE,100),DamageType.MELEE);check(n.getHp()==hp&&p.getSettings().getRecoilDamage()==400,"Immunity neither reflects nor spends charges");
  // Core's direct typed path used to debit charges without any reply.
  clean();CorporealBeast c=new CorporealBeast(8133);place(c);c.bindArena(3190,3190,3250,3250,0);p=recoil(c);World.getWorld().getPlayers().add(p);c.setHp(10000);
  check(c.spawnCore(p),"Core spawned");EncounterAdd core=c.getCore();core.getRandom().setSeed(7);p.setAttribute("vengeance",true);hp=core.getHp();
  new NPCTickTask(core).execute();new NPCTickTask(core).execute();int loss=1000-p.getHitPoints(),recoil=loss/10;
  check(loss>0&&core.getHp()==hp-recoil&&p.getSettings().getRecoilDamage()==400-recoil,"Core reflects actual drain exactly once");
  check(c.getHp()==10000+loss,"Core healing remains actual LP loss");worldTicks(1);check(core.getHp()==hp-recoil-(int)(loss*.75),"Core Vengeance delivered once");c.destroy();
  // Underfoot stomp uses the same settlement despite having no action endSession.
  clean();c=new CorporealBeast(8133);place(c);c.bindArena(3190,3190,3250,3250,0);p=recoil(c);p.setLocation(c.getLocation());World.getWorld().getPlayers().add(p);c.getRandom().setSeed(2);hp=c.getHp();new NPCTickTask(c).execute();loss=1000-p.getHitPoints();
  check(loss>0&&c.getHp()==hp-loss/10,"Stomp reflection delivered");c.destroy();
 }
 static Interaction cast(MagicSpell s,Player p,Mob v)throws Exception{
  Interaction i=null;for(int retry=0;retry<100;retry++){i=new Interaction(p,v);i.setSpell(s);s.castSpell(i);if(i.getDamage().getHit()>=0)return i;}throw new AssertionError("Cannot obtain successful accuracy");
 }
 static void bindings()throws Exception{
  MagicSpell[] spells={new Bind(),new Snare(),new Entangle(),new IceRush(),new IceBlitz()};int[] duration={8,16,25,8,25};
  for(int index=0;index<spells.length;index++)for(String state:new String[]{"normal","immune","godmode","teleport","logout","death","miss","thaw"}){
   clean();NPC n=npc();Player p=CombatFixtures.player(n),v=CombatFixtures.player(n);v.getSkills().setLevelAndXP(Skills.MAGIC,1,0);v.getSkills().setLevelAndXP(Skills.DEFENCE,1,0);
   Interaction i=cast(spells[index],p,v);check(v.getAttribute("freezeTime",-1)<0,"No cast-time freeze "+index);
   if(state.equals("immune"))v.setAttribute("hitImmunity",100);if(state.equals("godmode"))v.setAttribute("godmode",true);
   if(state.equals("teleport")){Location old=v.getLocation();v.teleport(old.transform(1,0,0),false);v.teleport(old,false);}
   if(state.equals("logout")){v.setOnline(false);v.setOnline(true);}if(state.equals("death")){v.getSkills().setHitPoints(0);v.getSkills().setHitPoints(1000);}
   if(state.equals("miss"))i.setDamage(new Damage(-1));else i.getDamage().setHit(0);
   if(state.equals("thaw"))v.setAttribute("freezeImmunity",5);
   spells[index].endSpell(i);boolean expected=state.equals("normal");check((v.getAttribute("freezeTime",-1)>0)==expected,"Valid zero-contact freeze only: "+index+" "+state);
   if(expected){check(v.getAttribute("freezeTime",-1)==duration[index],"Retained duration");set(null,World.class,"ticksPassed",duration[index]);check(!CombatStatus.freeze(v,5),"Thaw immunity holds after expiry");set(null,World.class,"ticksPassed",duration[index]+5);check(CombatStatus.freeze(v,5),"Freeze allowed at immunity boundary");}
  }
  clean();Impling imp=new Impling(6055);place(imp);Player p=CombatFixtures.player(imp);int hp=imp.getHp();Interaction i=cast(new Bind(),p,imp);new Bind().endSpell(i);check(imp.getHp()==hp&&!imp.isDead()&&imp.getAttribute("freezeTime",-1)>0,"Impling binding keeps no-damage behavior");
  clean();NPC n=npc();p=CombatFixtures.player(n);i=cast(new Entangle(),p,n);i.getDamage().setHit(0);new Entangle().endSpell(i);check(n.getAttribute("freezeTime",-1)==25,"NPC zero contact can bind");
  for(MagicSpell area:new MagicSpell[]{new IceBurst(),new IceBarrage()}){
   clean();n=npc();p=CombatFixtures.player(n);Player one=CombatFixtures.player(n),two=CombatFixtures.player(n);i=new Interaction(p,one);i.setSpell(area);
   ExtraTarget first=new ExtraTarget(one),second=new ExtraTarget(two);first.setDamage(Damage.getDamage(p,one,CombatType.MAGIC,0));second.setDamage(Damage.getDamage(p,two,CombatType.MAGIC,0));i.setTargets(Arrays.asList(first,second));
   Location old=two.getLocation();two.teleport(old.transform(1,0,0),false);two.teleport(old,false);area.endSpell(i);
   check(one.getAttribute("freezeTime",-1)>0&&two.getAttribute("freezeTime",-1)<0,"Area freeze commits only eligible target: "+area.getClass().getSimpleName());
  }
 }
 static void spellEntry()throws Exception{
  SpellContainer.initialize();
  for(boolean auto:new boolean[]{false,true}){
   clean();NPC n=npc();n.setHp(1000);Player p=CombatFixtures.player(n);p.setLocation(n.getLocation().transform(4,0,0));
   p.getInventory().addItem(555,100);p.getInventory().addItem(557,100);p.getInventory().addItem(561,100);p.getSettings().setSpellBook(192);p.getRandom().setSeed(3);
   p.setAttribute(auto?"autocastId":"spellId",36);p.getCombatExecutor().setVictim(n);int launch=-1,impact=-1;
   for(int t=0;t<20;t++){set(null,World.class,"ticksPassed",t);p.getMask().reset();p.getCombatExecutor().tick();if(launch<0&&p.getMask().getLastAnimation()!=null){launch=t;check(n.getAttribute("freezeTime",-1)<=t,"No freeze in cast launch task");}if(n.getAttribute("freezeTime",-1)>t){impact=t;break;}}
   check(launch>=0&&impact>launch,"Real executor "+(auto?"autocast":"manual")+" reaches delayed binding");check(!p.getInventory().contains(new Item(561,100)),"Runes spent at spell entry");
  }
 }
 static void stunMovement()throws Exception{
  for(boolean running:new boolean[]{false,true}){
   clean();NPC n=npc();Player p=CombatFixtures.player(n),v=CombatFixtures.player(n);p.setLocation(n.getLocation().transform(0,-2,0));
   set(v,Player.class,"handler",new org.dementhium.net.handler.DementhiumHandler());
   p.getEquipment().set(Equipment.SLOT_WEAPON,new Item(9185));p.getEquipment().set(Equipment.SLOT_ARROWS,new Item(9237,100));p.getBonuses().calculate();
   v.getSkills().setLevelAndXP(Skills.DEFENCE,1,0);v.getSkills().setLevelAndXP(Skills.AGILITY,1,0);
   v.getWalkingQueue().setIsRunning(running);Location destination=n.getLocation().transform(11,0,0),initial=v.getLocation();
   World.getWorld().doPath(new org.dementhium.model.map.path.DefaultPathFinder(),v,destination.getX(),destination.getY());
   org.dementhium.task.impl.PlayerTickTask task=new org.dementhium.task.impl.PlayerTickTask(v);task.execute();
   check(!v.getLocation().equals(initial)&&v.getWalkingQueue().isMoving(),"Real player task advances queued "+(running?"run":"walk"));
   // The real Jade bolt roll/impact supplies the stun; movement is already in flight.
   for(int attempt=0;attempt<100&&!v.getAttribute("stunned",false);attempt++){
    Damage hit=CombatUtils.getRangeDamage(p,v,Ammunition.get(9237));v.getDamageManager().damage(p,hit,DamageType.RANGE);
   }
   check(v.getAttribute("stunned",false)&&v.getHitPoints()>0,"Damaging Jade proc stuns a live moving player");Location stopped=v.getLocation();
   task.execute();check(v.getLocation().equals(stopped),"Jade stun cancels previously queued movement");
   for(int tick=1;tick<=3;tick++){
    worldTicks(1);World.getWorld().doPath(new org.dementhium.model.map.path.DefaultPathFinder(),v,destination.getX(),destination.getY());task.execute();
    check(v.getAttribute("stunned",false)&&v.getLocation().equals(stopped),"Stun rejects movement before expiry tick "+tick);
   }
   worldTicks(1);task.execute();check(!v.getAttribute("stunned",false)&&!v.getAttribute("cantMove",false)&&v.getLocation().equals(stopped),"Due stun expiry unlocks without resuming discarded route");
   World.getWorld().doPath(new org.dementhium.model.map.path.DefaultPathFinder(),v,destination.getX(),destination.getY());task.execute();
   check(!v.getLocation().equals(stopped),"Fresh movement resumes after stun expiry");
  }
 }
 static void npcMagicCadence()throws Exception{
  // Waterfiends now have a mixed-family handler; use the ordinary Monk of Zamorak.
  for(int id:new int[]{1643,190}){
   clean();NPC n=NPCLoader.getNPC(id);place(n);n.getRandom().setSeed(903);Player p=CombatFixtures.player(n);p.getSkills().setMaximumLifePoints(10000);p.getSkills().setHitPoints(10000);
   p.getSkills().setLevelAndXP(Skills.DEFENCE,1,0);p.getSkills().setLevelAndXP(Skills.MAGIC,1,0);
   check(n.getClass()==NPC.class&&n.getCombatAction().getClass()==org.dementhium.model.combat.impl.MagicAction.class,"Loaded NPC uses default magic action: "+id);
   int delay=n.getDefinition().getAttackDelay();check(delay==(id==1643?4:5),"Retained real NPC attack delay: "+id);
   n.getCombatExecutor().setVictim(p);List<Integer> launches=new ArrayList<Integer>();
   // Natural initial cooldown and real task/action prototype; no attack roll or timer is supplied.
   for(int tick=1;tick<=25&&launches.size()<4;tick++){
    set(null,World.class,"ticksPassed",tick);n.getMask().reset();new NPCTickTask(n).execute();
    if(n.getMask().getLastAnimation()!=null)launches.add(tick);
    if(tick<5)check(p.getDamageManager().getHits().isEmpty()&&p.getHitPoints()==10000,"NPC spell has no pre-impact damage: "+id+" tick="+tick);
    if(tick==5)check(p.getDamageManager().getHits().size()==1,"NPC spell settles once on its first due tick: "+id);
   }
   check(launches.size()==4&&launches.get(0)==3,"Default NPC magic launches naturally: "+id+" "+launches);
   for(int index=1;index<launches.size();index++)check(launches.get(index)-launches.get(index-1)==delay,"NPC magic cadence follows configured delay "+delay+": "+id+" "+launches);
   check(p.getHitPoints()<10000&&p.getHitPoints()>0,"Default NPC spells really reach damage settlement: "+id);
  }
 }
 public static void main(String[] args)throws Exception{CombatFixtures.init();reset();vengeance();reflection();bindings();spellEntry();stunMovement();npcMagicCadence();clean();System.out.println("Shared combat repairs: "+checks+" checks passed");}
}
