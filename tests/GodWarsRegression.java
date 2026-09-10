import java.lang.reflect.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.dementhium.cache.*;
import org.dementhium.model.*;
import org.dementhium.model.combat.*;
import org.dementhium.model.definition.*;
import org.dementhium.model.npc.*;
import org.dementhium.model.npc.godwars.*;
import org.dementhium.model.npc.godwars.GodWarsAction.Attack;
import org.dementhium.model.player.*;
import org.dementhium.model.map.Region;
public class GodWarsRegression {
 // Historical release-delta assertions remain available in the default mode.
 static boolean runtimeOnly;
 static int checks;static void check(boolean x,String msg){checks++;if(!x)throw new AssertionError(msg);}
 static final List<Player> players=new ArrayList<Player>();
 static class Fixture {
  AtomicInteger clock=new AtomicInteger();GodWarsRoom room;GodWarsNPC boss;
  Fixture(GodWarsType t){for(Player p:players)p.setOnline(false);players.clear();room=new GodWarsRoom(t,3190,3190,3230,3230,0,clock::get);boss=add(t.boss);}
  GodWarsNPC add(int id){GodWarsNPC n=(GodWarsNPC)NPCLoader.getNPC(id);n.setLocation(Location.locate(3200,3200,0));n.setOriginalLocation(n.getLocation());n.getRandom().setSeed(12345+id);room.attach(n);return n;}
  Player player(){Player p=BossBatchOneRegression.player(boss);players.add(p);p.getRandom().setSeed(9871);return p;}
  void step(int n){for(int i=0;i<n;i++){clock.incrementAndGet();room.tick();}}
 }
 static GodWarsAction launch(Fixture f,GodWarsNPC n,Player p,Attack a)throws Exception {
  Constructor<GodWarsAction> c=GodWarsAction.class.getDeclaredConstructor(GodWarsNPC.class,Attack.class);c.setAccessible(true);
  GodWarsAction action=c.newInstance(n,a);action.setInteraction(new Interaction(n,p));n.getCombatExecutor().setCombatAction(action);return action;
 }
 static void profiles()throws Exception {
  for(GodWarsType t:GodWarsType.values()){
   check(NPCLoader.getNPC(t.boss) instanceof GodWarsNPC,"Registered boss "+t);
   check(NPCDefinition.forId(t.boss).getHitpoints()==2550,"Boss LP "+t);
   check(NPCLoader.getNPC(t.boss).getAttackDelay()==t.speed,"Cadence "+t);
   for(int id:t.followers)check(NPCLoader.getNPC(id) instanceof GodWarsNPC,"Registered follower "+id);
  }
  if(!runtimeOnly){
  byte[] old=Files.readAllBytes(Paths.get("build/godwars-before/NDE/NPCDefinitions.bin")),now=Files.readAllBytes(Paths.get("NDE/NPCDefinitions.bin"));
  check(old.length==now.length,"Packed length unchanged");ByteBuffer b=ByteBuffer.wrap(old);Set<Integer> allowed=new HashSet<Integer>();int records=0,changes=0;
  while(b.hasRemaining()){int id=b.getShort();records++;if(id==-1)continue;b.getShort();while(b.get()!=0){};int pos=b.position();if(id==6260||id==6247||id==6203||id==6222){for(int j=0;j<30;j++)if(j!=20&&j!=21&&j!=26&&j!=27)allowed.add(pos+j);for(int j=37;j<=47;j++)allowed.add(pos+j);}b.position(pos+59);}
  check(records==13488,"Packed records preserved");for(int i=0;i<now.length;i++)if(old[i]!=now[i]){check(allowed.contains(i),"Only declared boss profile bytes changed "+i);changes++;}check(changes>3,"Actually patched data");
  } else check(NPCDefinition.definitionSize()==13488,"Current packed record count");
  for(Attack a:Attack.values()){
   if(a.animation>=0)check(CacheManager.getData(20,a.animation>>7,a.animation&127).length>0,"Native animation "+a.animation);
   for(int g:new int[]{a.projectile,a.graphic})if(g>=0)check(CacheManager.getData(21,g>>8,g&255).length>0,"Native graphic "+g);
  }
 }
 static void fights()throws Exception {
  for(Attack a:Attack.values()){
   GodWarsType t=a.name().startsWith("BANDOS")?GodWarsType.BANDOS:a.name().startsWith("SARA")?GodWarsType.SARADOMIN:a.name().startsWith("ZAM")?GodWarsType.ZAMORAK:GodWarsType.ARMADYL;
   int successes=0;
   for(int seed=0;seed<24;seed++)for(int mode=0;mode<4;mode++){
    Fixture f=new Fixture(t);GodWarsNPC n=f.boss;
    if(a.name().startsWith("FOLLOW"))n=f.add(a==Attack.FOLLOW_MELEE?6227:a==Attack.FOLLOW_RANGE?6225:6223);
    Player p=f.player();p.setLocation(Location.locate(3200+n.size(),3200,0));n.getRandom().setSeed(seed+773);p.getRandom().setSeed(seed+11);
    if(mode==1)BossBatchOneRegression.prayers(p)[0][a.type.getProtectionPrayer()]=true;
    if(mode==2)p.setAttribute("godmode",true);
    if(mode==3){p.getEquipment().set(5,new Item(13740));p.getBonuses().calculate();}
    if(a==Attack.ZAM_SPECIAL)BossBatchOneRegression.prayers(p)[0][CombatType.MELEE.getProtectionPrayer()]=true;
    GodWarsAction action=launch(f,n,p,a);check(action.commenceSession(),"Launch "+a+" mode "+mode);Damage d=action.getInteraction().getDamage();
    check(d!=null,"Primary owns typed hit");check(action.attack().type==a.type,"Captured style "+a);
    if(a.cap>0)check(d.getMaximum()==a.cap,"Max hit "+a);
    int raw=Math.max(0,d.getHit());check(p.getHitPoints()==1000,"Launch not impact");double pray=p.getSkills().getPrayerPoints();f.step(4);
    int expected=mode==2||(mode==1&&a!=Attack.ZAM_SPECIAL)?0:mode==3?raw-(int)Math.ceil(raw*0.30):raw;
    check(1000-p.getHitPoints()==expected,"Mitigated LP "+a+" mode="+mode+" raw="+raw+" got="+(1000-p.getHitPoints()));
    if(a==Attack.ZAM_SPECIAL&&mode!=3)check(Math.abs(p.getSkills().getPrayerPoints()-(expected>0?pray-Math.floor(pray/2.0):pray))<0.01,"Special prayer drain uses actual damage");
    if(expected>0)successes++;int hp=p.getHitPoints();f.room.tick();action.endSession();f.step(3);check(p.getHitPoints()==hp,"No replay "+a);
    f.room.close();
   }
   check(successes>0,"Positive coverage "+a);
  }
 }
 static void boundaries()throws Exception {
  for(Attack a:new Attack[]{Attack.SARA_MELEE,Attack.SARA_MAGIC,Attack.BANDOS_MELEE,Attack.BANDOS_RANGE,Attack.ZAM_MAGIC,Attack.ZAM_SPECIAL}){
   Fixture f=new Fixture(a.name().startsWith("SARA")?GodWarsType.SARADOMIN:a.name().startsWith("ZAM")?GodWarsType.ZAMORAK:GodWarsType.BANDOS);Player p=f.player();p.setLocation(Location.locate(3215,3200,0));BossBatchOneRegression.prayers(p)[0][CombatType.MELEE.getProtectionPrayer()]=true;
   check(!launch(f,f.boss,p,a).commenceSession(),"Contact attack cannot launch remotely "+a);
  }
  for(int mode=0;mode<7;mode++){
   Fixture f=new Fixture(GodWarsType.BANDOS);Player primary=f.player(),second=f.player();f.boss.getRandom().setSeed(881);GodWarsAction a=launch(f,f.boss,primary,Attack.BANDOS_RANGE);check(a.commenceSession(),"Area launch");List<?> pending=(List<?>)BossBatchOneRegression.field(a,"hits");check(pending.size()==2,"Both area victims captured");int secondRaw=0;for(Object h:pending)if(BossBatchOneRegression.field(h,"victim")==second)secondRaw=((Damage)BossBatchOneRegression.field(h,"damage")).getHit();
   if(mode==0)primary.setOnline(false);if(mode==1)primary.setLocation(Location.locate(3240,3200,0));if(mode==2)primary.setLocation(Location.locate(3205,3200,1));
   if(mode==3){f.boss.setDead(true);f.boss.setDead(false);}if(mode==4)f.room.close();if(mode==5)primary.markInstanceTransition();if(mode==6)primary.setHidden(true);
   f.step(4);check(primary.getHitPoints()==1000,"Boundary cancels affected hit "+mode);
   check(mode==3||mode==4?second.getHitPoints()==1000:second.getHitPoints()==1000-Math.max(0,secondRaw),"Independent area target "+mode);
  }
  Fixture f=new Fixture(GodWarsType.ARMADYL);Player p=f.player();check(GodWarsAction.choose(f.boss,p)==Attack.KREE_MELEE,"Idle contact Kree melee");p.setLocation(Location.locate(3215,3200,0));check(GodWarsAction.choose(f.boss,p)==Attack.KREE_MELEE,"Unattacked Kree chases distant target for melee");p.getCombatExecutor().setVictim(f.boss);
  for(int i=0;i<50;i++)check(GodWarsAction.choose(f.boss,p)!=Attack.KREE_MELEE,"Attacked Kree stays airborne");
  GodWarsAction proto=(GodWarsAction)f.boss.getCombatAction();f.boss.getCombatExecutor().setVictim(p);Attack selected=f.boss.prepareAttack();GodWarsAction first=(GodWarsAction)proto.newSession(),second=(GodWarsAction)proto.newSession();check(first!=second&&first.attack()==selected,"Movement choice retained by independent session");
  check(!f.room.accepts(f.boss,null),"Null target rejected");
  p.setLocation(Location.locate(3210,3200,0));Region.addClipping(3207,3200,0,256|0x20000);check(!launch(f,f.boss,p,Attack.KREE_RANGE).commenceSession(),"Wall blocks projectile");Region.removeClipping(3207,3200,0,256|0x20000);
 }
 static void lifecycle()throws Exception {
  for(GodWarsType t:GodWarsType.values()){
   Fixture f=new Fixture(t);Player tank=f.player();GodWarsNPC follower=f.add(t.followers[0]);f.boss.getCombatExecutor().setVictim(tank);follower.tick();check(follower.getCombatExecutor().getVictim()==tank,"Follower takes tank "+t);
   follower.setDead(true);follower.died(tank);f.step(24);check(follower.isDead(),"Follower waits");f.step(1);check(!follower.isDead()&&!follower.isHidden()&&follower.getHp()==follower.getMaxHp(),"Independent follower revival");
   follower.setHp(500);long gen=f.boss.getCombatGeneration();f.boss.setDead(true);f.boss.died(tank);f.step(99);check(f.boss.isDead(),"Boss waits independently");f.step(1);check(!f.boss.isDead()&&f.boss.getCombatGeneration()>gen,"Boss returns without killing followers");check(follower.getHp()==500,"Living follower not healed by boss revival");
   f.boss.setDead(true);f.boss.died(tank);follower.setDead(true);follower.died(tank);f.step(25);check(follower.isDead(),"Dead follower waits for boss");f.step(76);check(!f.boss.isDead()&&!follower.isDead(),"Both return");
   f.boss.setHp(500);f.boss.getCombatStats().drain(Skills.DEFENCE,20);f.room.tick();tank.setOnline(false);f.step(11);check(f.boss.getHp()==2550&&f.boss.getCombatLevel(Skills.DEFENCE)==f.boss.getCombatStats().base(Skills.DEFENCE),"Empty room resets HP and stats");
   f.boss.setHp(800);f.step(1);check(f.boss.getHp()==800,"No repeated idle healing");
   try{f.add(t.boss);throw new AssertionError("Duplicate accepted");}catch(IllegalStateException expected){checks++;}
   GodWarsRoom separate=new GodWarsRoom(t,3190,3190,3230,3230,0,()->0);GodWarsNPC copy=(GodWarsNPC)NPCLoader.getNPC(t.boss);copy.setLocation(Location.locate(3200,3200,0));separate.attach(copy);check(!NPCCombatContext.validPair(copy,f.boss),"Separate room ownership");separate.close();f.room.close();
  }
 }
 static void wikiAndRouting()throws Exception {
  Fixture f=new Fixture(GodWarsType.BANDOS);Player p=f.player();p.setLocation(Location.locate(3215,3200,0));f.boss.getCombatExecutor().setVictim(p);
  org.dementhium.task.impl.NPCTickTask task=new org.dementhium.task.impl.NPCTickTask(f.boss);task.execute();
  check(f.boss.getCombatExecutor().getVictim()==p,"Real NPC task preserves first-contact chase");
  f=new Fixture(GodWarsType.ZAMORAK);p=f.player();BossBatchOneRegression.prayers(p)[0][CombatType.MELEE.getProtectionPrayer()]=true;int magic=0,special=0;
  for(int i=0;i<27000;i++){Attack a=GodWarsAction.choose(f.boss,p);if(a==Attack.ZAM_MAGIC)magic++;if(a==Attack.ZAM_SPECIAL)special++;}
  check(magic>8700&&magic<9300&&special>1800&&special<2200,"Wiki rates: 1/3 magic and 2/27 smash");
  BossBatchOneRegression.prayers(p)[0][CombatType.MELEE.getProtectionPrayer()]=false;for(int i=0;i<100;i++)check(GodWarsAction.choose(f.boss,p)!=Attack.ZAM_SPECIAL,"No smash without melee prayer");
  for(GodWarsType t:GodWarsType.values())for(int id:t.followers){
   f=new Fixture(t);GodWarsNPC n=f.add(id);p=f.player();p.setLocation(Location.locate(3200+n.size(),3200,0));
   Attack a=GodWarsAction.choose(n,p);GodWarsAction action=launch(f,n,p,a);check(action.commenceSession(),"Every follower launches "+id);f.step(3);
   Method projectile=GodWarsAction.class.getDeclaredMethod("projectile",Attack.class);projectile.setAccessible(true);int g=(Integer)projectile.invoke(action,a);
   if(a.type!=CombatType.MELEE)check(g>=0&&CacheManager.getData(21,g>>8,g&255).length>0,"Native follower projectile "+id+":"+g);
  }
  f=new Fixture(GodWarsType.ARMADYL);p=f.player();Method roll=GodWarsAction.class.getDeclaredMethod("roll",Attack.class,Player.class,int.class);roll.setAccessible(true);GodWarsAction a=launch(f,f.boss,p,Attack.KREE_MAGIC);
  for(int i=0;i<100;i++){f.boss.getRandom().setSeed(i);p.getRandom().setSeed(i+3);int plain=(Integer)roll.invoke(a,Attack.KREE_MAGIC,p,210);p.getSkills().setLevelAndXP(Skills.MAGIC,1,0);f.boss.getRandom().setSeed(i);p.getRandom().setSeed(i+3);check((Integer)roll.invoke(a,Attack.KREE_MAGIC,p,210)==plain,"Kree blue tornado ignores magic defence level");p.getSkills().setLevelAndXP(Skills.MAGIC,99,13034431);}
 }
 static void maps()throws Exception {
  check(org.dementhium.util.MapXTEA.loadPackedFile(),"Map keys load");Set<Integer> loaded=new HashSet<Integer>();
  for(String line:Files.readAllLines(Paths.get("data/npcs/npcspawns.txt"))){
   if(!line.matches("(6260|6261|6263|6265|6247|6248|6250|6252|6203|6204|6206|6208|6222|6223|6225|6227) .*"))continue;
   String[] parts=line.split(" ");int id=Integer.parseInt(parts[0]),x=Integer.parseInt(parts[1]),y=Integer.parseInt(parts[2]),z=Integer.parseInt(parts[3]),region=((x>>6)<<8)|(y>>6);
   if(loaded.add(region))check(org.dementhium.cache.format.LandscapeParser.parseLandscape(region,org.dementhium.util.MapXTEA.getMapKeys().get(region)),"Actual landscape "+region);
   GodWarsNPC n=(GodWarsNPC)NPCLoader.getNPC(id);n.setLocation(Location.locate(x,y,z));n.setOriginalLocation(n.getLocation());GodWarsRoom room=n.getRoom();check(room!=null&&room.owns(n),"Actual spawn attaches "+id);
   for(int xx=x;xx<x+n.size();xx++)for(int yy=y;yy<y+n.size();yy++){int mask=Region.getClippingMask(xx,yy,z);check(mask!=-1&&(mask&(256|0x200000))==0,"Walkable spawn "+id+" at "+xx+","+yy+" mask="+mask);check(room.contains(Location.locate(xx,yy,z)),"Whole spawn inside room "+id);}
  }
 }
 static void realDeath()throws Exception {
  Fixture f=new Fixture(GodWarsType.BANDOS);f.player();f.boss.sendDead();check(f.boss.isDead(),"Real NPC death starts");f.step(100);check(!f.boss.isDead(),"Real NPC.sendDead routes to owned respawn with missing followers");
 }
 public static void main(String[] args)throws Exception {runtimeOnly=args.length>0&&args[0].equals("runtime");
  Cache.init();ItemDefinition.init();NPCDefinition.init();Field areas=World.class.getDeclaredField("areaManager");areas.setAccessible(true);areas.set(World.getWorld(),new org.dementhium.content.areas.AreaManager());Method m=NPCLoader.class.getDeclaredMethod("loadCustomizations");m.setAccessible(true);m.invoke(null);
  Region r=Region.forCoords(3200,3200);r.clippingMasks=new int[4][128][128];r.setClipped(true);
  if(args.length>0&&args[0].equals("death-only")){realDeath();System.out.println("God Wars death route passed.");return;}
  profiles();fights();boundaries();lifecycle();realDeath();wikiAndRouting();maps();System.out.println("God Wars: "+checks+" checks passed.");
 }
}
