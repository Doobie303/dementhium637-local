import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import org.dementhium.cache.*;
import org.dementhium.model.*;
import org.dementhium.model.combat.*;
import org.dementhium.model.definition.*;
import org.dementhium.model.npc.*;
import org.dementhium.model.npc.impl.*;
import org.dementhium.model.npc.encounter.*;
import org.dementhium.model.npc.encounter.EncounterAttack.Kind;
import org.dementhium.model.player.*;
import org.dementhium.model.map.Region;
import org.dementhium.model.misc.DamageManager.DamageType;
import org.dementhium.net.GameSession;

public class BossStepFourRegression {
 // Historical release-delta assertions remain available in the default mode.
 static boolean runtimeOnly;
 static int checks;
 static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 static final List<Player> online=new ArrayList<Player>();
 static class Fixture {
  EncounterNPC boss;
  Fixture(int id){for(Player p:online)p.setOnline(false);online.clear();boss=place((EncounterNPC)NPCLoader.getNPC(id));}
  <T extends EncounterNPC>T place(T n){n.setLocation(Location.locate(3200,3200,0));n.setOriginalLocation(n.getLocation());n.bindArena(3190,3190,3230,3230,0);n.setDoesWalk(false);n.getRandom().setSeed(812);return n;}
  Player player(){
   Player p=new Player(new GameSession(null){@Override public boolean isDisconnected(){return false;}},new PlayerDefinition("encounter-test","unused"));
   for(int s:new int[]{Skills.ATTACK,Skills.STRENGTH,Skills.DEFENCE,Skills.RANGED,Skills.MAGIC,Skills.PRAYER})p.getSkills().setLevelAndXP(s,99,13034431);
   p.getSkills().setMaximumLifePoints(1000);p.getSkills().setHitPoints(1000);p.getSkills().setPrayerPoints(99,false);p.getBonuses().calculate();p.setOnline(true);p.setLocation(boss.getLocation().transform(boss.size(),0,0));p.getRandom().setSeed(117);online.add(p);return p;
  }
  void step(int count){for(int i=0;i<count;i++)boss.tick();}
 }
 static EncounterAttack launch(EncounterNPC n,Player p,Kind k){EncounterAttack a=new EncounterAttack(n,k);a.setInteraction(new Interaction(n,p));n.getCombatExecutor().setCombatAction(a);return a;}
 static int id(Kind k){switch(k){case CORP_MELEE:case CORP_MAGIC:case CORP_DRAIN:case CORP_SPLIT:return 8133;case SUPREME:return 2881;case PRIME:return 2882;case REX:return 2883;case WORKER:return 1156;case SPIN_MAGIC:case SPIN_RANGE:return 2892;default:return 1158;}}
 static List<String> activeLines(Path file)throws Exception{
  List<String> out=new ArrayList<String>();boolean ignore=false;
  for(String line:Files.readAllLines(file)){if(line.startsWith("//")||line.isEmpty())continue;if(line.contains("/*")){ignore=true;continue;}if(ignore){if(line.contains("*/"))ignore=false;continue;}out.add(line);}return out;
 }
 static void profiles()throws Exception {
  if(!runtimeOnly){
  List<byte[]> old=BossBatchOneRegression.records(Paths.get("build/batches/boss-step4/before/source/NDE/NPCDefinitions.bin")),now=BossBatchOneRegression.records(Paths.get("NDE/NPCDefinitions.bin"));
  Set<Integer> changed=new HashSet<Integer>(Arrays.asList(8133,1158,1160,2891,2892,2893,2894,2895,2896));
  check(old.size()==13488&&now.size()==old.size(),"Packed count preserved");
  for(int n=0;n<old.size();n++)check(Arrays.equals(old.get(n),now.get(n))!=changed.contains(n),"Only declared packed profile differs: "+n);
  } else check(NPCDefinition.definitionSize()==13488,"Current packed record count");
  check(NPCLoader.getNPC(1158) instanceof KalphiteQueen&&NPCLoader.getNPC(1160) instanceof KalphiteQueen,"Both real registry forms");
  check(NPCDefinition.forId(8133).getHitpoints()==20000,"Corp LP");
  for(int n:new int[]{1158,1160}){NPC d=NPCLoader.getNPC(n);check(d.getMaxHp()==2550&&d.getAttackDelay()==4,"KQ LP/speed");check(d.getCombatLevel(Skills.ATTACK)==300&&d.getCombatLevel(Skills.MAGIC)==300,"KQ stats");}
  check(NPCDefinition.forId(1158).getBonuses()[8]==550&&NPCDefinition.forId(1160).getBonuses()[5]==550,"Opposite form defences");
  for(int n=2891;n<=2896;n++)check(NPCLoader.getNPC(n) instanceof EncounterAdd&&NPCDefinition.forId(n).getHitpoints()==750,"Spinolyp variants "+n);
  Map<Integer,Integer> counts=new HashMap<Integer,Integer>();Set<String> unique=new HashSet<String>();
  for(String line:activeLines(Paths.get("data/npcs/npcspawns.txt"))){String[] p=line.split(" ");int n=Integer.parseInt(p[0]);counts.put(n,counts.getOrDefault(n,0)+1);if(n>=2891&&n<=2896)check(unique.add(p[0]+","+p[1]+","+p[2]+","+p[3]),"No duplicate Spinolyp");}
  for(int n:new int[]{8133,1158,2881,2882,2883})check(counts.get(n)==1,"One whole encounter spawn "+n);
  for(Kind k:Kind.values()){
   check(CacheManager.getData(20,k.animation>>7,k.animation&127).length>0,"Native animation "+k.animation);
   for(int g:new int[]{k.projectile,k.graphic})if(g>=0)check(CacheManager.getData(21,g>>8,g&255).length>0,"Native graphic "+g);
  }
  for(int a:new int[]{9454,9458,6242,6237,2869,2870})check(CacheManager.getData(20,a>>7,a&127).length>0,"Native transition/defence "+a);
 }
 static void attacks()throws Exception {
  for(Kind k:Kind.values()){
   if(k==Kind.CORP_SPLIT)continue;int positive=0;
   for(int seed=0;seed<20;seed++)for(int mode=0;mode<4;mode++){
    Fixture f=new Fixture(id(k)==1156?1158:id(k));EncounterNPC n=k==Kind.WORKER?f.place(new EncounterAdd(1156,f.boss)):f.boss;Player p=f.player();p.setLocation(n.getLocation().transform(n.size(),0,0));n.getRandom().setSeed(seed+61);p.getRandom().setSeed(seed+91);
    if(mode==1)BossBatchOneRegression.prayers(p)[0][k.type.getProtectionPrayer()]=true;
    if(mode==2)p.setAttribute("godmode",true);
    if(mode==3){p.getEquipment().set(5,new Item(13740));p.getBonuses().calculate();}
    EncounterAttack a=launch(n,p,k);check(a.commenceSession(),"Launch "+k+" mode="+mode);Damage d=a.getInteraction().getDamage();check(d!=null&&d.getMaximum()==k.cap,"Typed bounded primary "+k);
    int raw=Math.max(0,d.getHit());check(p.getHitPoints()==1000,"No launch damage");
    for(int i=0;i<4;i++)n.tick();int expected=mode==2?0:mode==3?raw-(int)Math.ceil(raw*.30):raw;
    check(1000-p.getHitPoints()==expected,"Actual shield/protection result "+k+" mode="+mode+" raw="+raw+" actual="+(1000-p.getHitPoints()));
    if(mode==1&&k!=Kind.CORP_MAGIC&&k!=Kind.CORP_DRAIN)check(expected==0,"Ordinary matching protection "+k);
    if(expected>0)positive++;int hp=p.getHitPoints();check(!a.commenceSession(),"Launch cannot replay");a.endSession();for(int i=0;i<3;i++)n.tick();check(p.getHitPoints()==hp,"Impact cannot replay");
   }
   check(positive>0,"Positive damage coverage "+k);
  }
  for(int form:new int[]{1158,1160})for(CombatType type:CombatType.values()){
   Fixture f=new Fixture(form);Player p=f.player();check(Damage.getDamage(p,f.boss,type,100).getHit()==100,"KQ icons are accuracy, not immunity "+form+":"+type);
  }
 }
 static void boundaries()throws Exception {
  for(int mode=0;mode<8;mode++){
   Fixture f=new Fixture(1158);Player p=f.player(),q=f.player();EncounterAttack a=launch(f.boss,p,Kind.KQ_RANGE);f.boss.getRandom().setSeed(31);check(a.commenceSession(),"Area launch");
   if(mode==0)p.setOnline(false);if(mode==1)p.setLocation(Location.locate(3231,3200,0));if(mode==2)p.setLocation(Location.locate(3205,3200,1));if(mode==3)p.markInstanceTransition();if(mode==4)p.setHidden(true);if(mode==5)p.setInvisible(true);if(mode==6)f.boss.resetEncounter();if(mode==7)f.boss.setDead(true);
   f.step(4);check(p.getHitPoints()==1000,"Invalid primary cancelled "+mode);check(mode>=6?q.getHitPoints()==1000:q.getHitPoints()<1000,"Independent secondary/life cancellation "+mode);
  }
  Fixture f=new Fixture(2883);Player p=f.player();p.setLocation(Location.locate(3215,3200,0));check(!launch(f.boss,p,Kind.REX).commenceSession(),"No remote melee");
  p.setLocation(Location.locate(3231,3200,0));check(!NPCCombatContext.validPair(f.boss,p)&&!NPCCombatContext.validPair(p,f.boss),"Arena guard both directions");
  f=new Fixture(2882);p=f.player();p.setLocation(Location.locate(3209,3200,0));Region.addClipping(3207,3200,0,256|0x20000);check(!launch(f.boss,p,Kind.PRIME).commenceSession(),"Rock blocks launch");Region.removeClipping(3207,3200,0,256|0x20000);
  f.boss.getCombatExecutor().setVictim(p);Kind selected=f.boss.prepareAttack();check(((EncounterAttack)f.boss.getCombatAction().newSession()).kind()==selected,"Movement retains selected style");
 }
 static void corp()throws Exception {
  Fixture f=new Fixture(8133);CorporealBeast corp=(CorporealBeast)f.boss;Player p=f.player();
  p.getEquipment().set(3,new Item(11716));p.getSettings().setCombatType(WeaponInterface.TYPE_STAB);check(Damage.getDamage(p,corp,CombatType.MELEE,600).getHit()==600,"Stab spear full damage");
  Damage captured=Damage.getDamage(p,corp,CombatType.MELEE,600);p.getEquipment().set(3,new Item(4151));corp.getDamageManager().damage(p,captured,DamageType.MELEE);check(corp.getHp()==19400,"Weapon owned at construction");
  check(Damage.getDamage(p,corp,CombatType.MELEE,600).getHit()==300,"Non-spear half");p.getEquipment().set(3,new Item(11716));p.getSettings().setCombatType(WeaponInterface.TYPE_SLASH);check(Damage.getDamage(p,corp,CombatType.MELEE,600).getHit()==300,"Slash spear half");
  check(Damage.getDamage(p,corp,CombatType.RANGE,600).getHit()==300&&Damage.getDamage(p,corp,CombatType.MAGIC,600).getHit()==600,"Ranged half, magic full");
  check(Damage.getDamage(p,corp,CombatType.RANGE,4000).getHit()==1000&&Damage.getDamage(p,corp,CombatType.MAGIC,4000).getHit()==1000,"Defined per-hit ceiling");
  p.getEquipment().set(3,new Item(9185));p.getBonuses().calculate();boolean ruby=false;
  for(int i=0;i<100&&!ruby;i++){p.getRandom().setSeed(i+7000);Damage d=CombatUtils.getRangeDamage(p,corp,Ammunition.get(9242));if(d.getHit()==500){ruby=true;corp.getDamageManager().damage(p,d,DamageType.RANGE);check(p.getHitPoints()==900,"Real Ruby proc recoil");}}
  check(ruby,"Real Ruby proc honors existing global cap then Corp half");
  f=new Fixture(8133);corp=(CorporealBeast)f.boss;p=f.player();p.setLocation(Location.locate(3209,3200,0));Player diagonal=f.player();diagonal.setLocation(p.getLocation().transform(1,1,0));EncounterAttack a=launch(corp,p,Kind.CORP_SPLIT);check(a.commenceSession(),"Ground launch");Location ground=p.getLocation();p.setLocation(ground.transform(0,-3,0));f.step(3);check(p.getHitPoints()==1000&&diagonal.getHitPoints()<1000,"Dodge ground and square splash");Player splinter=f.player();splinter.setLocation(ground.transform(2,0,0));f.step(2);check(splinter.getHitPoints()<1000,"Delayed splinter hits current occupant");
  f=new Fixture(8133);corp=(CorporealBeast)f.boss;p=f.player();check(corp.spawnCore(p)&&!corp.spawnCore(p),"One owned core");EncounterAdd core=corp.getCore();check(core.getMaxHp()==250,"Core LP");corp.setHp(18000);Player q=f.player();q.setLocation(core.getLocation().transform(1,1,0));q.setAttribute("godmode",true);core.tick();core.tick();check(corp.getHp()==18000+1000-p.getHitPoints(),"Core heals only actual LP incl diagonal godmode");
  int hp=p.getHitPoints();core.getPoisonManager().poison(p,60);for(int i=0;i<19;i++)core.tick();check(p.getHitPoints()==hp,"Poisoned core slows pulse");core.tick();check(p.getHitPoints()<hp,"Slow core pulse occurs");
  p.setLocation(core.getLocation().transform(5,0,0));q.setOnline(false);Location before=core.getLocation();for(int i=0;i<3;i++)core.tick();check(!core.getLocation().equals(before)&&core.getLocation().equals(p.getLocation()),"Vacated core hops");
  core.getCombatExecutor().setVictim(p);check(!CombatMovement.combatFollow(core,p,CombatType.MELEE)&&!launch(core,p,Kind.WORKER).commenceSession(),"Core cannot add ordinary melee attack");
  corp.resetEncounter();check(core.isDead()&&core.isHidden()&&corp.getCore()==null,"Reset removes core");check(corp.spawnCore(p),"New life can spawn");core=corp.getCore();corp.sendDead();check(core.isDead()&&core.isHidden(),"Corp death removes core");
  f=new Fixture(8133);p=f.player();p.setLocation(f.boss.getLocation());BossBatchOneRegression.prayers(p)[0][CombatType.MELEE.getProtectionPrayer()]=true;f.step(1);check(p.getHitPoints()<1000,"Underfoot stomp bypasses melee prayer");
 }
  static void drainsAndMitigation()throws Exception {
  int corpHits=0,drains=0,emptyHits=0,spinHits=0;
  for(int seed=0;seed<40;seed++){
   int unprotected=0;
   for(int mode=0;mode<2;mode++){
    Fixture f=new Fixture(8133);Player p=f.player();f.boss.getRandom().setSeed(seed+100);p.getRandom().setSeed(seed+800);
    if(mode==1)BossBatchOneRegression.prayers(p)[0][CombatType.MAGIC.getProtectionPrayer()]=true;
    EncounterAttack a=launch(f.boss,p,Kind.CORP_MAGIC);check(a.commenceSession(),"Corp comparison launch");f.step(4);int lost=1000-p.getHitPoints();if(mode==0)unprotected=lost;else check(lost==unprotected/2,"Corp magic exactly halves with prayer");if(lost>0)corpHits++;
   }
   for(int mode=0;mode<3;mode++){
    Fixture f=new Fixture(8133);Player p=f.player();f.boss.getRandom().setSeed(seed+100);p.getRandom().setSeed(seed+800);
    if(mode>0){p.getSkills().decreaseLevelToZero(Skills.MAGIC,99);p.getSkills().setPrayerPoints(0,false);}if(mode==2)p.setAttribute("godmode",true);
    EncounterAttack a=launch(f.boss,p,Kind.CORP_DRAIN);check(a.commenceSession(),"Drain launch");int raw=Math.max(0,a.getInteraction().getDamage().getHit());check(p.getSkills().getLevel(Skills.MAGIC)==(mode==0?99:0),"No early drain");f.step(4);
    int lost=1000-p.getHitPoints();if(mode==0){double amount=(99-p.getSkills().getPrayerPoints())+(99-p.getSkills().getLevel(Skills.MAGIC));check(amount==(lost>0?Math.max(1,lost/10):0),"One actual-hit stat drained");if(amount>0)drains++;}
    if(mode==1){check(lost==raw+(raw>0?Math.max(1,raw/10):0),"Already empty stat converts drain to LP");if(lost>0)emptyHits++;}
    if(mode==2)check(lost==0,"Godmode suppresses drain and converted damage");
   }
   Fixture f=new Fixture(2892);Player p=f.player();f.boss.getRandom().setSeed(seed+28);EncounterAttack a=launch(f.boss,p,Kind.SPIN_MAGIC);check(a.commenceSession(),"Spin magic launches");f.step(4);double drained=99-p.getSkills().getPrayerPoints();check(drained==(p.getHitPoints()<1000?1:0),"Spin magic drains one server prayer point on damage");if(drained>0)spinHits++;
  }
  check(corpHits>0&&drains>0&&emptyHits>0&&spinHits>0,"Positive mitigation and status coverage");
 }
 static class Queen extends KalphiteQueen {int rewards;Mob winner;Queen(){super(1158);}@Override public void loot(Mob killer){rewards++;winner=killer;}}
 static void queen()throws Exception {
  Fixture f=new Fixture(1158);Queen n=f.place(new Queen());f.boss=n;Player p=f.player();p.setAttribute("godmode",true); // Nonlethal protection for workers; remove for credit hits.
  p.removeAttribute("godmode");Damage old=Damage.getDamage(p,n,CombatType.MELEE,100);n.getDamageManager().damage(p,Damage.getDamage(p,n,CombatType.MELEE,2550),DamageType.MELEE);
  check(n.isDead()&&n.getId()==1158&&n.rewards==0,"First kill has no reward");f.step(3);check(n.getId()==1158,"Transition delay");f.step(1);check(n.getId()==1160&&!n.isDead()&&n.getHp()==2550,"Second form full LP");n.getDamageManager().damage(p,old,DamageType.MELEE);check(n.getHp()==2550,"No old incoming hit across form");
  check(n.getDamageManager().getKiller()==p,"First-form lethal credit preserved");f.step(40);check(n.workerCount()==2,"Worker cap two");
  n.getDamageManager().damage(p,Damage.getDamage(p,n,CombatType.MELEE,2550),DamageType.MELEE);check(n.isDead()&&n.workerCount()==0,"Final death cleans workers");f.step(Math.max(1,n.getDeathTick()));check(n.rewards==1&&n.winner==p&&n.isHidden(),"Exactly one credited final reward");f.step(n.respawnDelay());check(n.getId()==1158&&!n.isDead()&&!n.isHidden()&&n.getHp()==2550&&n.rewards==1,"Respawn first form, no replay");
  n.sendDead();n.resetEncounter();f.step(5);check(n.getId()==1158&&!n.isDead()&&n.getHp()==2550,"Reset cancels pending transition");
  n.sendDead();f.step(4);n.setHp(1000);n.getCombatStats().drain(Skills.DEFENCE,20);p.setOnline(false);f.step(10);check(n.getId()==1158&&n.getHp()==2550&&n.getCombatLevel(Skills.DEFENCE)==300,"Empty arena resets second form and drains");
 }
 static void kings()throws Exception {
  for(Kind k:new Kind[]{Kind.SUPREME,Kind.PRIME,Kind.REX}){
   Fixture f=new Fixture(id(k));Player p=f.player(),q=f.player(),far=f.player();q.setLocation(p.getLocation().transform(1,1,0));far.setLocation(p.getLocation().transform(3,0,0));f.boss.getRandom().setSeed(89);EncounterAttack a=launch(f.boss,p,k);check(a.commenceSession(),"King launch");f.step(4);
   check(k==Kind.REX?q.getHitPoints()==1000:q.getHitPoints()<1000,"King secondary target "+k);check(k==Kind.SUPREME?far.getHitPoints()<1000:far.getHitPoints()==1000,"King area extent "+k);
  }
    Fixture fan=new Fixture(2881);Player front=fan.player(),behind=fan.player();behind.setLocation(Location.locate(3199,3200,0));check(launch(fan.boss,front,Kind.SUPREME).commenceSession(),"Fan launches");fan.step(4);check(behind.getHitPoints()==1000,"Supreme forward fan excludes rear player");
  Fixture f=new Fixture(2883);Player p=f.player();p.setLocation(Location.locate(3210,3200,0));f.boss.tick();check(f.boss.getCombatExecutor().getVictim()==null,"Rex local aggression");p.setLocation(Location.locate(3205,3200,0));f.boss.tick();check(f.boss.getCombatExecutor().getVictim()==p,"Rex acquires nearby");
  p.setLocation(Location.locate(3215,3200,0));f.boss.tick();check(f.boss.getCombatExecutor().getVictim()==p,"Retains lured tank inside arena");
  Location before=f.boss.getLocation();f.boss.setAttribute("freezeTime",World.getTicks()+20);check(!CombatMovement.combatFollow(f.boss,p,CombatType.MELEE),"Frozen distant Rex cannot attack");f.boss.getWalkingQueue().getNextEntityMovement();check(f.boss.getLocation().equals(before),"Freeze stops encounter step");f.boss.removeAttribute("freezeTime");
  for(int y=3199;y<3210;y++)Region.addClipping(3200+f.boss.size(),y,0,256);CombatMovement.combatFollow(f.boss,p,CombatType.MELEE);f.boss.getWalkingQueue().getNextEntityMovement();check(f.boss.getLocation().equals(before),"Whole NPC footprint blocked by rock");for(int y=3199;y<3210;y++)Region.removeClipping(3200+f.boss.size(),y,0,256);
  new org.dementhium.task.impl.NPCTickTask(f.boss).execute();check(f.boss.getCombatExecutor().getVictim()==p,"Real NPC task preserves lure");
  EncounterNPC other=f.place(new DagannothKing(2882));other.setHp(700);f.boss.sendDead();f.step(f.boss.respawnDelay());check(!f.boss.isDead()&&other.getHp()==700,"Independent King revival");
 }
 static void spinolyps()throws Exception {
  Fixture f=new Fixture(2891);Player p=f.player();check(!f.boss.isAttackable(p),"Suspicious water not attackable");f.step(8);check(f.boss.getId()==2892&&f.boss.isAttackable(p)&&f.boss.getHp()==750,"Water emerges");f.boss.setHp(300);f.step(1);check(f.boss.getId()==2891&&!f.boss.isAttackable(p),"Injured Spinolyp dives");f.step(8);check(f.boss.getId()==2892&&f.boss.getHp()==300,"Dive preserves HP");f.step(2);check(f.boss.getId()==2892,"Only one dive per life");f.boss.resetEncounter();check(f.boss.getHp()==750,"Spinolyp reset restores profile");
  f=new Fixture(2896);p=f.player();p.setLocation(Location.locate(3207,3200,0));f.boss.setDoesWalk(true);p.setLocation(Location.locate(3213,3200,0));Location before=f.boss.getLocation();check(!CombatMovement.combatFollow(f.boss,p,CombatType.RANGE),"Spinolyp does not chase out of reach");for(int i=0;i<30;i++){f.boss.tick();f.boss.getWalkingQueue().getNextEntityMovement();}check(f.boss.getLocation().equals(before),"Moat enemies remain stationary even with legacy walking flag");
 }
 static void maps()throws Exception {
  check(org.dementhium.util.MapXTEA.loadPackedFile(),"Map keys");Set<Integer> loaded=new HashSet<Integer>();
  for(String line:activeLines(Paths.get("data/npcs/npcspawns.txt"))){String[] p=line.split(" ");int id=Integer.parseInt(p[0]);if(id!=8133&&id!=1158&&(id<2881||id>2883))continue;int x=Integer.parseInt(p[1]),y=Integer.parseInt(p[2]),z=Integer.parseInt(p[3]),r=((x>>6)<<8)|(y>>6);if(loaded.add(r))check(org.dementhium.cache.format.LandscapeParser.parseLandscape(r,org.dementhium.util.MapXTEA.getMapKeys().get(r)),"Real map load "+r);EncounterNPC n=(EncounterNPC)NPCLoader.getNPC(id);n.setLocation(Location.locate(x,y,z));n.setOriginalLocation(n.getLocation());for(int xx=x;xx<x+n.size();xx++)for(int yy=y;yy<y+n.size();yy++){int mask=Region.getClippingMask(xx,yy,z);check(mask!=-1&&(mask&(256|0x200000))==0,"Real spawn walkable "+id+" at "+xx+","+yy+" mask="+mask);check(n.contains(Location.locate(xx,yy,z)),"Full footprint in arena "+id);}}
 }
 public static void main(String[] args)throws Exception {runtimeOnly=args.length>0&&args[0].equals("runtime");
  Cache.init();ItemDefinition.init();NPCDefinition.init();Field areas=World.class.getDeclaredField("areaManager");areas.setAccessible(true);areas.set(World.getWorld(),new org.dementhium.content.areas.AreaManager());Method load=NPCLoader.class.getDeclaredMethod("loadCustomizations");load.setAccessible(true);load.invoke(null);Region r=Region.forCoords(3200,3200);r.clippingMasks=new int[4][128][128];r.setClipped(true);
  profiles();attacks();boundaries();corp();drainsAndMitigation();queen();kings();spinolyps();maps();System.out.println("Boss step 4: "+checks+" checks passed.");
 }
}
