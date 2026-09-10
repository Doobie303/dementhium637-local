import java.lang.reflect.*;
import java.util.*;
import org.dementhium.model.*;
import org.dementhium.model.combat.*;
import org.dementhium.model.combat.impl.MeleeAction;
import org.dementhium.model.mask.*;
import org.dementhium.model.npc.*;
import org.dementhium.model.npc.godwars.*;
import org.dementhium.model.npc.impl.Nex;
import org.dementhium.model.npc.impl.Nex.*;
import org.dementhium.model.player.*;
import org.dementhium.task.impl.NPCTickTask;

/** Decisive production task/update cases; no server, accounts or real loot. */
public class NPCPresentationRegression {
 static int checks;
 static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 static void field(Object o,Class<?> type,String key,Object value)throws Exception{Field f=type.getDeclaredField(key);f.setAccessible(true);f.set(o,value);}
 static void time(int tick)throws Exception{field(null,World.class,"ticksPassed",tick);}
 static void clean()throws Exception{CombatFixtures.clearPlayers();for(NPC n:World.getWorld().getNpcs())if(n!=null)World.getWorld().getNpcs().remove(n);time(0);}
 static GodWarsNPC god(int id,GodWarsRoom room,int x,int y){return god(id,room,x,y,0);}
 static GodWarsNPC god(int id,GodWarsRoom room,int x,int y,int z){GodWarsNPC n=(GodWarsNPC)NPCLoader.getNPC(id);n.setLocation(Location.locate(x,y,z));n.setOriginalLocation(n.getLocation());n.setDoesWalk(false);room.attach(n);World.getWorld().getNpcs().add(n);return n;}
 static Player player(NPC n){Player p=CombatFixtures.player(n);p.getSkills().setMaximumLifePoints(100000);p.getSkills().setHitPoints(100000);World.getWorld().getPlayers().add(p);p.setViewDistance(15);return p;}
 static void task(NPC n){n.getMask().reset();new NPCTickTask(n).execute();}
 static void animations()throws Exception{
  for(int variant=0;variant<3;variant++){
   int id=variant==0?6260:6247;
   clean();GodWarsRoom room=new GodWarsRoom(GodWarsType.forId(id),3190,3190,3260,3260,0,World::getTicks);
   GodWarsNPC n=god(id,room,3216,3216);Player p=player(n);p.setLocation(n.getLocation().transform(n.size(),0,0));
   n.getCombatExecutor().setVictim(p);n.getCombatExecutor().setTicks(0);
   GodWarsAction.Attack attack=variant==0?GodWarsAction.Attack.BANDOS_RANGE:variant==1?GodWarsAction.Attack.SARA_MAGIC:GodWarsAction.Attack.SARA_MELEE;
   field(n,GodWarsNPC.class,"prepared",attack);field(n,GodWarsNPC.class,"preparedTarget",p);
   time(1);task(n);check(n.getMask().getLastAnimation()!=null&&n.getMask().getLastAnimation().getId()==attack.animation,"Native special launches through NPC task");
   NpcUpdate updates=new NpcUpdate(p);check(updates.createPacket()!=null,"Actual outgoing NPC addition/update");
   int frames=NPCAnimation.frames(attack.animation);System.out.println("sequence "+attack.animation+" frames="+frames+" defence="+n.getDefenceAnimation());check(frames>0,"Native frame lengths available");
   org.dementhium.model.combat.impl.RangeAction ranged=new org.dementhium.model.combat.impl.RangeAction();Interaction flight=new Interaction(p,n);flight.setRangeData(new RangeData(true));flight.setTicks(1);ranged.setInteraction(flight);ranged.executeSession();check(n.getMask().getLastAnimation().getId()==attack.animation,"Ranged flinch cannot overwrite same-tick boss tell");
   n.getMask().reset();time(2);
   MeleeAction incoming=new MeleeAction();incoming.setInteraction(new Interaction(p,n));incoming.commenceSession();
   check(frames>30?n.getMask().getLastAnimation()==null:n.getMask().getLastAnimation()!=null,"Flinch respects actual remaining sequence duration "+id);
   updates.createPacket();
   time(1+(frames+29)/30);n.animate(n.getDefenceAnimation());check(n.getMask().getLastAnimation()!=null,"Defence resumes after native sequence duration");
   n.animate(attack.animation);n.animate(Animation.RESET,true);n.animate(n.getDefenceAnimation());check(n.getMask().getLastAnimation().getId()==n.getDefenceAnimation(),"Explicit animation cancellation clears protection");
   n.animate(attack.animation);n.resetCombatState();n.animate(n.getDefenceAnimation());check(n.getMask().getLastAnimation().getId()==n.getDefenceAnimation(),"Encounter reset invalidates old action protection");
   n.animate(attack.animation);n.animate(n.getDeathAnimation());check(n.getMask().getLastAnimation().getId()==n.getDeathAnimation(),"Death takes priority over active attack");room.close();
  }
 }
 static void overlap()throws Exception{
  for(int id:new int[]{6263,6250}){
   clean();GodWarsRoom room=new GodWarsRoom(GodWarsType.forId(id),3190,3190,3260,3260,0,World::getTicks);
   GodWarsNPC n=god(id,room,3216,3216);Player p=player(n);p.setLocation(n.getLocation());n.getCombatExecutor().setVictim(p);
   for(int t=1;t<=8;t++){time(t);task(n);}
   check(!CombatMovement.standingOn(n.getLocation(),p.getLocation(),n.size(),p.size()),"Ranged follower escapes player overlap "+id);
   NpcUpdate update=new NpcUpdate(p);update.createPacket();Field locals=NpcUpdate.class.getDeclaredField("localNpcs");locals.setAccessible(true);check(((List<?>)locals.get(update)).contains(n),"Follower remains registered/visible to update stream");room.close();
  }
  clean();GodWarsRoom room=new GodWarsRoom(GodWarsType.SARADOMIN,3190,3190,3260,3260,0,World::getTicks);
  GodWarsNPC boss=god(6247,room,3216,3216),follower=god(6248,room,3216,3216);Player p=player(boss);p.setLocation(boss.getLocation().transform(boss.size(),0,0));
  boss.getCombatExecutor().setVictim(p);follower.getCombatExecutor().setVictim(p);
  for(int t=1;t<=12;t++){time(t);task(boss);task(follower);}
  check(!CombatMovement.standingOn(boss.getLocation(),follower.getLocation(),boss.size(),follower.size()),"Sara unicorn separates from boss footprint");room.close();
 }
 static void nex()throws Exception{
  clean();Nex n=new Nex(13447);n.setLocation(Location.locate(2924,5202,0));n.setOriginalLocation(n.getLocation());n.setDoesWalk(false);World.getWorld().getNpcs().add(n);
  field(n,Nex.class,"phase",NexPhase.SMOKE);field(NexAreaEvent.getNexAreaEvent(),NexAreaEvent.class,"nex",n);Player p=player(n);p.setLocation(n.getLocation().transform(8,0,0));n.getCombatExecutor().setVictim(p);
  org.dementhium.model.map.Region region=org.dementhium.model.map.Region.forCoords(2924,5202);region.clippingMasks=new int[4][128][128];region.setClipped(true);
  // Target-aware movement must run before an Interaction is ever attached to a session.
  n.getRandom().setSeed(1);int start=n.getLocation().getDistance(p.getLocation());
  for(int t=1;t<=5;t++){time(t);task(n);}
  check(n.getLocation().getDistance(p.getLocation())<start,"Fresh Nex prototype pursues valid target");
  n.setLocation(n.getOriginalLocation().transform(13,0,0));p.setLocation(n.getLocation().transform(-1,0,0));n.getCombatExecutor().setVictim(p);long generation=n.getCombatGeneration();
  time(6);task(n);check(!n.isReturningHome()&&n.getCombatGeneration()==generation,"Nex in-arena combat is not ordinary home-leash reset");
  Location before=n.getLocation();p.setLocation(before.transform(-8,0,0));int tick=7;
  for(String lock:new String[]{"noEscapeAttack","changingPhase","siphonMode","specialPending"}){field(n,Nex.class,lock,true);n.getWalkingQueue().reset();time(tick++);task(n);check(n.getLocation().equals(before),"Special lock prevents ordinary pursuit: "+lock);field(n,Nex.class,lock,false);}
  for(String lock:new String[]{"stunned","cantMove","busy"}){n.setAttribute(lock,true);time(tick++);task(n);check(n.getLocation().equals(before),"Movement lock prevents pursuit: "+lock);n.removeAttribute(lock);}
  p.setLocation(n.getLocation());for(int t=0;t<4;t++){time(tick++);task(n);}check(!CombatMovement.standingOn(n.getLocation(),p.getLocation(),n.size(),1),"Nex walks clear when target is underneath her");n.destroy();
 }
 static Object construct(Class<?> type,Class<?>[] signature,Object...args)throws Exception{Constructor<?> c=type.getDeclaredConstructor(signature);c.setAccessible(true);return c.newInstance(args);}
 static Object get(Object o,Class<?> type,String name)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);return f.get(o);}
 static void nativeClient()throws Exception{
  // Use the packaged developer client's own decoder and NPC sequence replacement function.
  // This proves interruption semantics, not renderer/network playback or model identity.
  try(java.net.URLClassLoader loader=new java.net.URLClassLoader(new java.net.URL[]{new java.io.File("build/gambler-interface/dev-client/Gambler-live-v7.jar").toURI().toURL()},null)){
   Class<?> sequence=loader.loadClass("Class97"),buffer=loader.loadClass("Class98_Sub22"),catalog=loader.loadClass("Class183"),lru=loader.loadClass("Class79");
   Object cache=construct(catalog,new Class<?>[]{loader.loadClass("Class279"),int.class,loader.loadClass("Class207"),loader.loadClass("Class207"),loader.loadClass("Class207")},null,0,null,null,null);
   Object entries=get(cache,catalog,"aClass79_1442");Method put=lru.getDeclaredMethod("method805",long.class,Object.class,byte.class);put.setAccessible(true);
   Method decode=sequence.getDeclaredMethod("method933",buffer,int.class);decode.setAccessible(true);
   for(int id:new int[]{7063,7061,6964,6970,6966,10053,10057,6354,6987}){
    Object seq=construct(sequence,new Class<?>[]{});Object bytes=construct(buffer,new Class<?>[]{byte[].class},org.dementhium.cache.CacheManager.getData(20,id>>7,id&127));decode.invoke(seq,bytes,-125);
    int total=0;for(int length:(int[])get(seq,sequence,"anIntArray811"))total+=length;
    check(total==NPCAnimation.frames(id),"Server protection length matches packaged client decoder "+id);put.invoke(entries,(long)id,seq,(byte)-80);
    System.out.println("native sequence "+id+" frames="+total+" priority="+get(seq,sequence,"anInt829"));
   }
   field(null,loader.loadClass("Class151_Sub7"),"aClass183_5001",cache);
   Class<?> actor=loader.loadClass("Class246_Sub3_Sub4_Sub2_Sub1"),base=loader.loadClass("Class246_Sub3_Sub4_Sub2");Object npc=construct(actor,new Class<?>[]{});
   Method replace=loader.loadClass("Class98_Sub43").getDeclaredMethod("method1483",int.class,actor,int.class,int[].class);replace.setAccessible(true);
   replace.invoke(null,0,npc,1,new int[]{7063,7063,7063,7063});check(((int[])get(npc,base,"anIntArray6373"))[0]==7063,"Packaged client starts Bandos smash");
   replace.invoke(null,0,npc,1,new int[]{7061,7061,7061,7061});check(((int[])get(npc,base,"anIntArray6373"))[0]==7061,"Negative control: old defence packet replaces unfinished smash in actual client code");
  }
 }
 static void nexTerrain()throws Exception{
  clean();check(org.dementhium.util.MapXTEA.loadPackedFile(),"Nex map keys");int region=(2924>>6)<<8|(5202>>6);
  check(org.dementhium.cache.format.LandscapeParser.parseLandscape(region,org.dementhium.util.MapXTEA.getMapKeys().get(region)),"Actual Nex landscape loaded");
  for(int[] target:new int[][]{{2934,5202},{2924,5214},{2920,5214},{2914,5214},{2935,5214},{2912,5202},{2924,5190},{2920,5192},{2935,5214,2914,5192}}){
   CombatFixtures.clearPlayers();Nex n=new Nex(13447);n.setLocation(Location.locate(2924,5202,0));n.setOriginalLocation(n.getLocation());n.setDoesWalk(false);World.getWorld().getNpcs().add(n);field(n,Nex.class,"phase",NexPhase.SMOKE);field(NexAreaEvent.getNexAreaEvent(),NexAreaEvent.class,"nex",n);
   if(target.length==4){n.setLocation(Location.locate(target[2],target[3],0));n.setOriginalLocation(n.getLocation());}
   Player p=player(n);p.setLocation(Location.locate(target[0],target[1],0));n.getRandom().setSeed(1);n.getCombatExecutor().setVictim(p);
   check((org.dementhium.model.map.Region.getClippingMask(target[0],target[1],0)&(256|0x200000|0x40000))==0,"Target is on real walkable arena floor");
   System.out.println("Nex terrain target="+p.getLocation()+" size="+n.size()+" admitted="+NPCCombatContext.validPair(n,p));
   boolean reached=false;for(int tick=1;tick<=50;tick++){time(tick);task(n);if(CombatMovement.hasMeleeContact(n,p,true)){reached=true;break;}}
   check(reached,"Nex natural melee pursuit reaches real arena lane "+Arrays.toString(target)+" ended="+n.getLocation());n.destroy();
  }
 }
 static void concurrent()throws Exception{
  long begin=System.nanoTime();int ticks=0;
  for(GodWarsType type:new GodWarsType[]{GodWarsType.BANDOS,GodWarsType.SARADOMIN})for(boolean reverse:new boolean[]{false,true}){
   clean();int x=type.minX+3,y=type.minY+6;
   org.dementhium.model.map.Region region=org.dementhium.model.map.Region.forCoords(x,y);region.clippingMasks=new int[4][128][128];region.setClipped(true);
   GodWarsRoom room=new GodWarsRoom(type,type.minX,type.minY,type.maxX,type.maxY,type.plane,World::getTicks);List<GodWarsNPC> actors=new ArrayList<GodWarsNPC>();
   GodWarsNPC boss=god(type.boss,room,x,y,type.plane);actors.add(boss);for(int id:type.followers)actors.add(god(id,room,x,y,type.plane));
   Player p=player(boss);p.setLocation(Location.locate(x+6,y+5,type.plane));for(GodWarsNPC n:actors)n.getCombatExecutor().setVictim(p);
   check(p.isMulti()&&boss.isMulti(),"Production God Wars multicombat admission (terrain isolated flat)");
   List<GodWarsNPC> order=new ArrayList<GodWarsNPC>(actors);if(reverse)Collections.reverse(order);NpcUpdate outgoing=new NpcUpdate(p);
   for(int t=1;t<=60;t++){
    time(t);if(t==16)p.setLocation(Location.locate(x+6,y-3,type.plane));if(t==32)p.setLocation(Location.locate(x,y-3,type.plane));
    for(GodWarsNPC n:order)task(n);outgoing.createPacket();ticks++;
    for(GodWarsNPC n:actors)check(World.getWorld().getNpcs().contains(n)&&!n.isHidden()&&room.contains(n.getLocation()),"Actors retain room/world ownership during concurrent chase");
   }
   for(GodWarsNPC n:actors){check(n.getCombatExecutor().getVictim()==p,"All actors retain eligible multicombat target");check(!CombatMovement.standingOn(n.getLocation(),p.getLocation(),n.size(),1),"Concurrent chase ends off player tile");if(n!=boss)check(room.overlap(n,n.getLocation())==0,"Followers finish with distinct valid positions "+type+":"+n.getId()+" reverse="+reverse);}
   GodWarsNPC follower=actors.get(1);follower.setLocation(p.getLocation());follower.setAttribute("freezeTime",100);Location frozen=follower.getLocation();time(61);task(follower);check(follower.getLocation().equals(frozen),"Frozen follower does not escape via crowd route");room.close();
  }
  System.out.println("Concurrent NPC/update cycles="+ticks+", milliseconds="+(System.nanoTime()-begin)/1000000.0+" (isolated tasks, not live World load)");
 }
 static void roomDistance()throws Exception{
  clean();GodWarsType type=GodWarsType.SARADOMIN;int x=type.minX+1,y=type.minY+1;
  org.dementhium.model.map.Region region=org.dementhium.model.map.Region.forCoords(x,y);region.clippingMasks=new int[4][128][128];region.setClipped(true);
  GodWarsRoom room=new GodWarsRoom(type,type.minX,type.minY,type.maxX,type.maxY,type.plane,World::getTicks);GodWarsNPC n=god(type.boss,room,x,y,type.plane);Player p=player(n);p.setLocation(Location.locate(type.maxX-1,type.maxY-1,type.plane));n.getCombatExecutor().setVictim(p);
  int distance=n.getLocation().getDistance(p.getLocation());check(distance>17&&NPCCombatContext.validPair(n,p),"Far-room target is eligible");time(1);task(n);check(n.getLocation().getDistance(p.getLocation())<distance,"Sara approaches far-room target without executor reset loop");room.close();
 }
 static void nexEvent()throws Exception{
  clean();Nex n=new Nex(13447);n.setLocation(Location.locate(2914,5192,0));n.setOriginalLocation(n.getLocation());n.setDoesWalk(false);World.getWorld().getNpcs().add(n);NexAreaEvent event=NexAreaEvent.getNexAreaEvent();field(event,NexAreaEvent.class,"nex",n);field(event,NexAreaEvent.class,"spawned",true);field(event,NexAreaEvent.class,"minionSpawnDelay",0);field(event,NexAreaEvent.class,"emptyRoomTicks",0);field(n,Nex.class,"phase",NexPhase.ICE);
  Player p=player(n);p.setLocation(Location.locate(2935,5214,0));n.getRandom().setSeed(1);boolean reached=false;
  for(int t=1;t<=50;t++){time(t);n.getMask().reset();event.run();new NPCTickTask(n).execute();if(CombatMovement.hasMeleeContact(n,p,true)){reached=true;break;}}
  check(reached,"Arena event acquires distant player before NPC task pursuit");NPCCombatContext old=new NPCCombatContext(n,p);p.teleport(2942,5214,0,false);time(51);task(n);check(n.getCombatExecutor().getVictim()==null&&!old.isCurrent(),"Arena departure cancels target and captured attack context");
  for(int t=52;t<=63;t++){time(t);n.getMask().reset();event.run();if(World.getWorld().getNpcs().contains(n))new NPCTickTask(n).execute();}
  check(event.getNex()==null&&!World.getWorld().getNpcs().contains(n),"Arena event owns empty-room removal without ordinary home walk");
 }
 public static void main(String[] args)throws Exception{
  CombatFixtures.init();List<String> failures=new ArrayList<String>();
  for(String group:new String[]{"animations","overlap","nex","native","concurrent","terrain","roomDistance","nexEvent"})try{if(group.equals("animations"))animations();else if(group.equals("overlap"))overlap();else if(group.equals("nex"))nex();else if(group.equals("native"))nativeClient();else if(group.equals("terrain"))nexTerrain();else if(group.equals("roomDistance"))roomDistance();else if(group.equals("nexEvent"))nexEvent();else concurrent();}catch(Throwable failure){failures.add(group+": "+failure);failure.printStackTrace();}
  if(!failures.isEmpty())throw new AssertionError(failures.toString());System.out.println("NPC presentation: "+checks+" checks passed");
 }
}
