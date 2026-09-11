import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import org.dementhium.cache.*;
import org.dementhium.model.*;
import org.dementhium.model.combat.*;
import org.dementhium.model.definition.*;
import org.dementhium.model.map.Region;
import org.dementhium.model.npc.*;
import org.dementhium.model.npc.impl.*;
import org.dementhium.model.npc.encounter.*;
import org.dementhium.model.npc.encounter.AdvancedAttack.Kind;
import org.dementhium.model.player.*;
import org.dementhium.model.misc.DamageManager.DamageType;
import org.dementhium.net.GameSession;

public class AdvancedBossRegression {
 // Historical release-delta assertions remain available in the default mode.
 static boolean runtimeOnly;
 static int checks;static void check(boolean ok,String msg){checks++;if(!ok)throw new AssertionError(msg);}
 static Player player(NPC n){return CombatFixtures.player(n);}
 static class Fixture {
  AdvancedNPC n;Player p;
  Fixture(int id){CombatFixtures.clearPlayers();n=(AdvancedNPC)NPCLoader.getNPC(id);n.setLocation(Location.locate(3200,3200,0));n.setOriginalLocation(n.getLocation());n.bindArena(3190,3190,3230,3230,0);n.setDoesWalk(false);n.getRandom().setSeed(923);p=player(n);}
  void step(int amount){for(int i=0;i<amount;i++)n.tick();}
 }
 static void init()throws Exception{CombatFixtures.init();}
 static AdvancedAttack action(Fixture f,Kind k){AdvancedAttack a=new AdvancedAttack(f.n,k);a.setInteraction(new Interaction(f.n,f.p));return a;}
 static int id(Kind k){if(k.name().startsWith("TD_"))return 8349;if(k.name().startsWith("FROST_"))return 51;if(k.name().startsWith("CHAOS_")||k==Kind.TELEPORT||k==Kind.DISARM)return 3200;return 50;}
 static void profiles()throws Exception{
  if(!runtimeOnly){
  List<byte[]> before=BossBatchOneRegression.records(Paths.get("build/batches/boss-step5/before/source/NDE/NPCDefinitions.bin")),after=BossBatchOneRegression.records(Paths.get("NDE/NPCDefinitions.bin"));check(before.size()==after.size(),"Packed record count");for(int i=0;i<before.size();i++)check(Arrays.equals(before.get(i),after.get(i))!=(i==51),"Only frost packed record differs "+i);
  } else check(NPCDefinition.definitionSize()==13488,"Current packed record count");
  check(NPCDefinition.forId(51).getProjectileId()==2465&&NPCLoader.getNPC(51) instanceof FrostDragon,"Real frost registry/projectile");
  if(!runtimeOnly){check(Arrays.equals(Files.readAllBytes(Paths.get("data/npcs/npcspawns.txt")),Files.readAllBytes(Paths.get("build/batches/boss-step5/before/source/data/npcs/npcspawns.txt"))),"Spawns and access preserved");}

  for(Kind k:Kind.values()){check(CacheManager.getData(20,k.animation>>7,k.animation&127).length>0,"Native animation "+k);for(int g:new int[]{k.projectile,k.graphic})if(g>=0)check(CacheManager.getData(21,g>>8,g&255).length>0,"Native gfx "+g);}
  for(int id:new int[]{8349,8353,8357,8361}){TormentedDemon n=(TormentedDemon)NPCLoader.getNPC(id);check(n.getMaxHp()==3260&&n.protection()==CombatType.MELEE&&n.getId()==id+3,"Each TD starts protected with full profile "+id);}
 }
 static void attacks()throws Exception{
  for(Kind k:Kind.values()){
   int positive=0;
   for(int seed=0;seed<16;seed++)for(int mode=0;mode<4;mode++){
    Fixture f=new Fixture(id(k));f.n.getRandom().setSeed(seed+10);f.p.getRandom().setSeed(seed+101);
    if(mode==1)BossBatchOneRegression.prayers(f.p)[0][k.style.getProtectionPrayer()]=true;
    if(mode==2)f.p.setAttribute("godmode",true);
    if(mode==3){f.p.getEquipment().set(5,new Item(13740));f.p.getBonuses().calculate();}
    AdvancedAttack a=action(f,k);check(a.commenceSession(),"Launch "+k);Damage d=a.getInteraction().getDamage();check(d!=null&&d.getMaximum()==k.cap,"Captured cap "+k);int raw=Math.max(0,d.getHit());check(f.p.getHitPoints()==1000&&!f.p.getPoisonManager().isPoisoned(),"No early impact "+k);double prayer=f.p.getSkills().getPrayerPoints();check(prayer==99,"No early shield charge "+k);
    f.step(4);int lost=1000-f.p.getHitPoints();
    if(mode==2||k.cap==0)check(lost==0,"Immune/non-damage branch "+k);else if(k.style!=CombatType.DRAGONFIRE)check(lost==(mode==3?raw-(int)Math.ceil(raw*.3):raw),"Resolved LP "+k+" raw="+raw+" got="+lost);
    if(mode==1&&k.style!=CombatType.DRAGONFIRE)check(lost==0,"Matching prayer "+k);if(lost>0)positive++;
    check(!a.commenceSession(),"Cannot replay launch");int hp=f.p.getHitPoints();a.endSession();f.step(3);check(f.p.getHitPoints()==hp,"Cannot replay impact "+k);
   }
   if(k.cap>0)check(positive>0,"Positive coverage "+k);
  }
  for(Kind k:new Kind[]{Kind.KBD_MELEE,Kind.FROST_MELEE,Kind.TD_MELEE}){Fixture f=new Fixture(id(k));f.p.setLocation(Location.locate(3212,3200,0));check(!action(f,k).commenceSession(),"Contact-only melee "+k);}
  Fixture f=new Fixture(3200);f.p.setLocation(Location.locate(3208,3200,0));check(action(f,Kind.CHAOS_MELEE).commenceSession(),"Chaos projectile has deliberately distant melee damage");
 }
 static void boundaries()throws Exception{
  for(Kind k:new Kind[]{Kind.TD_MAGIC,Kind.TOXIC,Kind.ICE,Kind.DISARM,Kind.TELEPORT,Kind.FROST_MAGIC})for(int mode=0;mode<7;mode++){
   Fixture f=new Fixture(id(k));f.p.getEquipment().set(3,new Item(4151));AdvancedAttack a=action(f,k);check(a.commenceSession(),"Owned launch");
   if(mode==0)f.p.setOnline(false);if(mode==1)f.p.setLocation(Location.locate(3205,3200,1));if(mode==2)f.p.setLocation(Location.locate(3231,3200,0));if(mode==3)f.p.setHidden(true);if(mode==4)f.n.resetEncounter();if(mode==5)f.n.setDead(true);if(mode==6)f.p.markInstanceTransition();Location before=f.p.getLocation();
   f.step(5);check(f.p.getHitPoints()==1000&&!f.p.getPoisonManager().isPoisoned()&&f.p.getAttribute("freezeTime",-1)<=World.getTicks(),"Old hit/status cancelled "+k+":"+mode);check(f.p.getLocation().equals(before)&&f.p.getEquipment().getSlot(3)==4151,"Old teleport/disarm cancelled");
  }
 }
 static void demons()throws Exception{
  Fixture f=new Fixture(8349);TormentedDemon d=(TormentedDemon)f.n;
  Damage protectedHit=Damage.getDamage(f.p,d,CombatType.MELEE,400);check(protectedHit.getHit()==0,"TD overhead blocks instead of subtractive shield arithmetic");
  Damage range=Damage.getDamage(f.p,d,CombatType.RANGE,400);check(range.getHit()==100&&d.protection()==CombatType.MELEE,"Shield quarter, no early switch");d.getDamageManager().damage(f.p,range,DamageType.RANGE);check(d.protection()==CombatType.RANGE,"310 pre-shield contact damage switches");
  f.p.getEquipment().set(3,new Item(6746));Damage dark=Damage.getDamage(f.p,d,CombatType.MELEE,100);f.p.getEquipment().set(3,new Item(4151));check(d.shieldActive(),"No early Darklight removal");d.getDamageManager().damage(f.p,dark,DamageType.MELEE);check(!d.shieldActive(),"Captured Darklight removes shield on actual impact");f.p.setAttribute("godmode",true);f.step(99);check(!d.shieldActive(),"Shield timeout waits");f.step(1);check(d.shieldActive(),"Shield restored at 100 ticks");
  d.resetEncounter();f.p.removeAttribute("godmode");Damage bypass=Damage.getDamage(f.p,d,CombatType.MELEE,400,true);check(bypass.getHit()==100,"Verac bypass preserves fire shield");
  Damage stale=Damage.getDamage(f.p,d,CombatType.RANGE,1000);d.resetEncounter();d.getDamageManager().damage(f.p,stale,DamageType.RANGE);check(d.getHp()==3260&&d.protection()==CombatType.MELEE,"Old hit cannot change new life prayer");
  d.getCombatStats().drain(Skills.DEFENCE,25);int defence=d.getCombatLevel(Skills.DEFENCE);Damage switcher=Damage.getDamage(f.p,d,CombatType.RANGE,320);d.getDamageManager().damage(f.p,switcher,DamageType.RANGE);check(d.getCombatLevel(Skills.DEFENCE)==defence,"Prayer model transform preserves NPC stat drain");
  f=new Fixture(8349);d=(TormentedDemon)f.n;f.p.setAttribute("godmode",true);CombatType old=d.offence();f.step(27);check(d.offence()!=old,"Roar changes to another attack style");Location tile=f.p.getLocation();f.p.removeAttribute("godmode");f.p.setLocation(tile.transform(3,0,0));Player bystander=player(d);bystander.setLocation(tile.transform(1,1,0));f.step(3);check(f.p.getHitPoints()==1000&&bystander.getHitPoints()<1000,"Ground splash is dodgeable 3x3, not eleven-tile sweep");
  f=new Fixture(8349);f.p.setAttribute("godmode",true);f.step(27);f.n.resetEncounter();f.p.removeAttribute("godmode");f.step(4);check(f.p.getHitPoints()==1000,"Reset cancels roar splash");
 }
 static void dragonfire()throws Exception{
  for(Kind k:new Kind[]{Kind.FIRE,Kind.ICE,Kind.TOXIC,Kind.SHOCK,Kind.FROST_FIRE}){
   Fixture f=new Fixture(id(k));boolean special=k!=Kind.FIRE&&k!=Kind.FROST_FIRE;check(AdvancedAttack.dragonfire(f.p,k,k.cap)==k.cap,"Unprotected breath cap");f.p.getEquipment().set(5,new Item(1540));check(AdvancedAttack.dragonfire(f.p,k,k.cap)==(special?200:100),"Single layer cap");f.p.setAttribute("antiFire",System.currentTimeMillis());check(AdvancedAttack.dragonfire(f.p,k,k.cap)==(special?100:0),"Combined protection keeps KBD special residual");
  }
  Fixture charged=new Fixture(50);Item shield=new Item(11283);charged.p.getEquipment().set(5,shield);AdvancedAttack.dragonfire(charged.p,Kind.FIRE,Kind.FIRE.cap);check(shield.getHealth()==1,"Advanced dragonfire charges DFS at impact");
  Fixture f=new Fixture(50);AdvancedAttack a=action(f,Kind.FIRE);check(a.commenceSession(),"Late shield setup");f.p.getEquipment().set(5,new Item(1540));f.p.setAttribute("antiFire",System.currentTimeMillis());f.step(4);check(f.p.getHitPoints()==1000,"Fire protection selected at impact");
  f=new Fixture(50);a=action(f,Kind.TOXIC);check(a.commenceSession(),"Poison launch");f.p.getPoisonManager().setCanBePoisoned(false);f.step(4);check(!f.p.getPoisonManager().isPoisoned(),"Antipoison gained in flight respected");
  int freezes=0;for(int seed=0;seed<20;seed++){f=new Fixture(50);f.n.getRandom().setSeed(seed);a=action(f,Kind.ICE);check(a.commenceSession(),"Ice launch");check(f.p.getAttribute("freezeTime",-1)<=World.getTicks(),"No launch freeze");f.step(4);if(f.p.getAttribute("freezeTime",-1)>World.getTicks()){check(f.p.getAttribute("freezeTime",-1)==World.getTicks()+5,"Five-tick ice binding");freezes++;}}check(freezes>0,"Freeze coverage");
 }
 static void chaos()throws Exception{
  Fixture f=new Fixture(3200);ChaosElemental n=(ChaosElemental)f.n;Item item=new Item(4718);item.setHealth(713);f.p.getEquipment().set(5,item);check(n.disarm(f.p),"Disarm actual occupied slot despite item metadata slot mismatch");check(f.p.getEquipment().get(5)==null&&f.p.getInventory().get(0).getId()==4718&&f.p.getInventory().get(0).getHealth()==713,"Disarm preserves wear and identity");
  f.p.getInventory().getContainer().clear();for(int i=0;i<28;i++)f.p.getInventory().set(i,new Item(385));f.p.getEquipment().set(3,new Item(4151));check(!n.disarm(f.p)&&f.p.getEquipment().getSlot(3)==4151,"Full inventory resists disarm");
  f.p.getInventory().set(27,null);f.p.getEquipment().set(3,new Item(385,2));check(!n.disarm(f.p)&&f.p.getEquipment().get(3).getAmount()==2&&f.p.getInventory().get(27)==null,"Atomic failure for oversized nonstackable copy");
  f=new Fixture(3200);n=(ChaosElemental)f.n;Location before=f.p.getLocation();check(n.relocate(f.p)&&n.contains(f.p.getLocation())&&!f.p.getLocation().equals(before),"Safe in-arena teleport");f.p.setLocation(before);
  for(int x=before.getX()-6;x<=before.getX()+6;x++)for(int y=before.getY()-6;y<=before.getY()+6;y++)if(Math.max(Math.abs(x-before.getX()),Math.abs(y-before.getY()))>=3)Region.addClipping(x,y,0,256);
  check(!n.relocate(f.p)&&f.p.getLocation().equals(before),"No legal landing leaves player in place");for(int x=before.getX()-6;x<=before.getX()+6;x++)for(int y=before.getY()-6;y<=before.getY()+6;y++)if(Math.max(Math.abs(x-before.getX()),Math.abs(y-before.getY()))>=3)Region.removeClipping(x,y,0,256);
 }
 static void routing()throws Exception{
  Field worldClock=World.class.getDeclaredField("ticksPassed");worldClock.setAccessible(true);
  for(int id:new int[]{8349,50,3200,51}){Fixture f=new Fixture(id);f.p.setAttribute("godmode",true);f.n.getCombatExecutor().setVictim(f.p);for(int i=0;i<40;i++){f.n.getNPCTasks()[0].execute();worldClock.setInt(null,World.getTicks()+1);}check(f.n.getCombatExecutor().getVictim()==f.p,"Actual tick retains valid target "+id);check(f.p.getDamageManager().getHits().size()>0,"Actual executor launches/resolves attacks "+id);f.n.setHp(500);f.p.setOnline(false);f.step(10);check(f.n.getHp()==f.n.getMaxHp(),"Empty arena restores boss "+id);}
 }
 static void maps()throws Exception{
  check(org.dementhium.util.MapXTEA.loadPackedFile(),"Map keys");Set<Integer> loaded=new HashSet<Integer>();int frost=0;
  for(String line:Files.readAllLines(Paths.get("data/npcs/npcspawns.txt"))){if(!line.matches("(50|51|3200|8349|8353|8357|8361) .*"))continue;String[] p=line.split(" ");int id=Integer.parseInt(p[0]),x=Integer.parseInt(p[1]),y=Integer.parseInt(p[2]),z=Integer.parseInt(p[3]),region=((x>>6)<<8)|(y>>6);if(loaded.add(region))check(org.dementhium.cache.format.LandscapeParser.parseLandscape(region,org.dementhium.util.MapXTEA.getMapKeys().get(region)),"Real map "+region);AdvancedNPC n=(AdvancedNPC)NPCLoader.getNPC(id);n.setLocation(Location.locate(x,y,z));n.setOriginalLocation(n.getLocation());if(id==51)frost++;check(n.contains(n.getLocation()),"Spawn in controller bounds "+id);}
  check(frost==19,"All 19 existing frost spawns wired");
 }
 public static void main(String[] args)throws Exception{runtimeOnly=args.length>0&&args[0].equals("runtime");init();profiles();attacks();boundaries();demons();dragonfire();chaos();routing();maps();System.out.println("Advanced bosses: "+checks+" checks passed.");}
}


