import java.lang.reflect.*;
import java.util.*;
import org.dementhium.model.*;
import org.dementhium.model.combat.*;
import org.dementhium.model.map.*;
import org.dementhium.model.npc.*;
import org.dementhium.model.npc.impl.*;
import org.dementhium.model.npc.impl.Nex.*;
import org.dementhium.model.npc.encounter.*;
import org.dementhium.model.npc.godwars.*;
import org.dementhium.model.player.*;
import org.dementhium.tickable.Tick;
import org.dementhium.task.impl.*;

/** Isolated production entry/order checks; no server or saved accounts. */
public final class BossEncounterCompletionRegression {
 static int checks,clock;static List<Tick> active=new ArrayList<Tick>();
 static Map<Player,List<org.dementhium.net.message.Message>> packets=new IdentityHashMap<Player,List<org.dementhium.net.message.Message>>();
 static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
 static Object field(Object object,Class<?> owner,String name)throws Exception{Field f=owner.getDeclaredField(name);f.setAccessible(true);return f.get(object);}
 static void field(Object object,Class<?> owner,String name,Object value)throws Exception{Field f=owner.getDeclaredField(name);f.setAccessible(true);f.set(object,value);}
 @SuppressWarnings("unchecked") static List<Tick> pending()throws Exception{return (List<Tick>)field(World.getWorld(),World.class,"ticksToAdd");}
 static void clean()throws Exception{CombatFixtures.clearPlayers();packets.clear();List<NPC> copy=new ArrayList<NPC>();for(NPC n:World.getWorld().getNpcs())if(n!=null)copy.add(n);for(NPC n:copy)World.getWorld().getNpcs().remove(n);active.clear();pending().clear();clock=0;field(null,World.class,"ticksPassed",0);}
 static Player player(NPC n)throws Exception{Player p=CombatFixtures.player(n);field(p,Player.class,"handler",new org.dementhium.net.handler.DementhiumHandler());List<org.dementhium.net.message.Message> output=new ArrayList<org.dementhium.net.message.Message>();packets.put(p,output);org.jboss.netty.channel.Channel channel=(org.jboss.netty.channel.Channel)Proxy.newProxyInstance(org.jboss.netty.channel.Channel.class.getClassLoader(),new Class[]{org.jboss.netty.channel.Channel.class},(proxy,method,args)->{if(method.getName().equals("write")){output.add((org.dementhium.net.message.Message)args[0]);return null;}if(method.getReturnType()==boolean.class)return true;if(method.getReturnType()==int.class)return 0;return null;});field(p.getConnection(),org.dementhium.net.GameSession.class,"channel",channel);p.getConnection().setInLobby(false);p.getSkills().setMaximumLifePoints(100000);p.getSkills().setHitPoints(100000);p.setViewDistance(15);World.getWorld().getPlayers().add(p);return p;}
 static void cycle()throws Exception{
  field(null,World.class,"ticksPassed",++clock);active.addAll(pending());pending().clear();for(Iterator<Tick> it=active.iterator();it.hasNext();)if(!it.next().run())it.remove();
  for(Player p:World.getWorld().getPlayers()){p.getCombatExecutor().setTicks(100000);new PlayerTickTask(p).execute();}
  List<NPC> order=new ArrayList<NPC>();for(NPC n:World.getWorld().getNpcs())if(n!=null)order.add(n);for(NPC n:order)if(World.getWorld().getNpcs().contains(n))new NPCTickTask(n).execute();
  for(Player p:World.getWorld().getPlayers()){new NpcUpdate(p).createPacket();p.getMask().reset();}for(NPC n:order)n.getMask().reset();
 }
 static void landscape(int x,int y)throws Exception{org.dementhium.util.MapXTEA.loadPackedFile();int id=((x>>6)<<8)|(y>>6);check(org.dementhium.cache.format.LandscapeParser.parseLandscape(id,org.dementhium.util.MapXTEA.getMapKeys().get(id)),"Native landscape loads");}
 static void assets()throws Exception{
  int coreModel=((int[])field(org.dementhium.cache.format.CacheNPCDefinition.forID(8127),org.dementhium.cache.format.CacheNPCDefinition.class,"anIntArray3230"))[0];
  check(graphicModel(1826)==coreModel,"Reported extra silhouettes resolve to the native core model");
  check(graphicModel(EncounterAttack.Kind.CORP_SPLIT.graphic)!=coreModel,"Split warnings use energy, not duplicate core models");
 }
 static int graphicModel(int id)throws Exception{byte[] data=org.dementhium.cache.CacheManager.getData(21,id>>8,id&255);check(data[0]==1,"Native graphic model field");return ((data[1]&255)<<8)|(data[2]&255);}
 static void clickDoor(Player p,GameObject door)throws Exception{
  int id=door.getId(),x=door.getLocation().getX(),y=door.getLocation().getY();
  org.jboss.netty.buffer.ChannelBuffer bytes=org.jboss.netty.buffer.ChannelBuffers.dynamicBuffer();bytes.writeByte(id&255);bytes.writeByte(id>>8);bytes.writeByte(0);bytes.writeByte((x+128)&255);bytes.writeByte(x>>8);bytes.writeByte(y>>8);bytes.writeByte((y+128)&255);
  new org.dementhium.net.packethandlers.ObjectPacketHandler().handlePacket(p,new org.dementhium.net.message.Message(76,org.dementhium.net.message.Message.PacketType.STANDARD,bytes));
 }
 static void doors()throws Exception{
  for(int[] spec:new int[][]{{26425,2863,5354,2,1,0},{26427,2908,5265,0,-1,0},{26428,2925,5332,2,0,-1},{26426,2839,5295,2,0,1}}){
   clean();landscape(spec[1],spec[2]);Location tile=Location.locate(spec[1],spec[2],spec[3]),inside=tile.transform(spec[4],spec[5],0);
   GameObject door=tile.getGameObjectType(0);check(door!=null&&door.getId()==spec[0],"Native door "+spec[0]);int rotation=door.getRotation();
   NPC seed=new NPC(1);seed.setLocation(tile);Player p=player(seed);p.setLocation(tile);Player observer=player(seed);observer.setLocation(inside);observer.updateRegionArea();
   for(int crossing=0;crossing<3;crossing++){
    Location destination=crossing%2==0?inside:tile;clickDoor(p,door);
    for(int t=0;t<4&&tile.getGameObjectType(0)!=null;t++)cycle();check(tile.getGameObjectType(0)==null,"Opening removes native door and collision "+spec[0]);
    packets.get(observer).clear();ObjectManager.refresh(observer);check(packets.get(observer).stream().anyMatch(m->m.getOpcode()==19),"Late scene refresh includes temporary deletion");
    for(int t=0;t<7;t++)cycle();door=tile.getGameObjectType(0);
    check(door!=null&&door.getId()==spec[0]&&door.getRotation()==rotation,"Door identity/orientation restored "+spec[0]);check(p.getLocation().equals(destination),"Unrestricted entrance/exit/reentry "+spec[0]+" crossing="+crossing+" actual="+p.getLocation());
    check(!org.dementhium.model.map.path.ProjectilePathFinder.clearMeleePath(inside,tile),"Closed entrance restores collision "+spec[0]);
   }
  }
 }
 static void core()throws Exception{
  clean();CorporealBeast n=new CorporealBeast(8133);n.setLocation(Location.locate(2980,4380,2));n.setOriginalLocation(n.getLocation());World.getWorld().getNpcs().add(n);landscape(2980,4380);Player p=player(n);p.setLocation(Location.locate(2991,4380,2));
  check(!n.spawnCore(p),"Core cannot spawn above its HP threshold");n.setHp(14000);check(n.spawnCore(p),"Eligible core spawn");EncounterAdd old=n.getCore();old.setHidden(true);check(n.spawnCore(p),"Replacement for hidden core");check(!World.getWorld().getNpcs().contains(old),"Replacing an inactive core removes old world entity");
  EncounterAdd core=n.getCore();Location origin=core.getLocation();p.setLocation(origin.transform(4,0,0));n.getCombatExecutor().setTicks(100000);
  for(int t=0;t<3;t++)cycle();check(core.getLocation().equals(origin)&&core.getAttribute("cantMove")==Boolean.TRUE,"Core hop emits movement before changing server position");Location landing=p.getLocation();int hp=p.getHitPoints();cycle();check(core.getLocation().equals(origin)&&p.getHitPoints()==hp,"Airborne core cannot drain or land early");cycle();check(core.getLocation().equals(landing),"Core lands after two flight ticks");
  long count=0;for(NPC npc:World.getWorld().getNpcs())if(npc.getId()==8127&&!npc.isDead())count++;check(count==1,"Exactly one real live core");n.resetEncounter();for(int t=0;t<4;t++)cycle();check(core.destroyed()&&!World.getWorld().getNpcs().contains(core),"Reset cancels core movement/drain and removes entity");
 }
 static void damage(Player p,NPC npc,int amount){npc.getDamageManager().damage(p,Damage.getDamage(p,npc,CombatType.MAGIC,amount,true),org.dementhium.model.misc.DamageManager.DamageType.MAGE);}
 static void nex()throws Exception{
  clean();landscape(2924,5202);org.dementhium.model.misc.GroundItemManager.load();NPC seed=new NPC(1);seed.setLocation(Location.locate(2924,5202,0));Player p=player(seed);p.setLocation(Location.locate(2927,5202,0));Player q=player(seed);q.setLocation(Location.locate(2926,5195,0));NexAreaEvent event=NexAreaEvent.getNexAreaEvent();field(event,NexAreaEvent.class,"nex",null);field(event,NexAreaEvent.class,"delay",0);field(event,NexAreaEvent.class,"random",new Random(918));((Random)field(null,Nex.class,"r")).setSeed(127);active.add(event);
  Set<NexPhase> phases=new LinkedHashSet<NexPhase>(),advance=new HashSet<NexPhase>();Nex dying=null;int stalled=0,finishedAt=-1;NexPhase previous=null;
  World.getWorld().getNpcDropLoader().load();
  check(World.getWorld().getNpcDropLoader().getDropMap().containsKey(Nex.WRATH_NEX),"Native drop table covers the terminal Nex form");
  World.getWorld().getNpcDropLoader().getDropMap().clear();
  World.getWorld().getNpcDropLoader().getDropMap().put(Nex.WRATH_NEX,new ArrayList<NPCDropLoader.Drop>(Arrays.asList(NPCDropLoader.Drop.create(565,100,7,7,false))));
  for(int t=0;t<2200;t++){
   cycle();Nex n=event.getNex();if(n==null){if(dying!=null){finishedAt=clock;break;}continue;}
   NexPhase phase=n.getPhase();if(phases.add(phase))System.out.println("Natural Nex phase "+phase+" at tick "+clock);
   if(phase==NexPhase.SPAWNED)continue;
   boolean pending=(Boolean)field(n,Nex.class,"specialPending"),changing=(Boolean)field(n,Nex.class,"changingPhase");int steps=(Integer)field(n,Nex.class,"specialStep"),autos=(Integer)field(n,Nex.class,"autoAttacksSinceSpecial");
   if(pending&&!changing&&!n.noEscapeAttack()&&!n.isSiphonMode()&&!n.hasTick("ice_attack")){if(previous==phase)stalled++;else stalled=0;check(stalled<=12,"Nex special rotation cannot idle-lock ordinary attacks: "+phase+" step="+steps+" tick="+clock);}else stalled=0;previous=phase;
   if(changing||n.noEscapeAttack()||n.isSiphonMode())continue;
   if(phase==NexPhase.FINAL){if((Integer)field(n,Nex.class,"zarosAttackCount")<6)continue;dying=n;damage(p,n,1000);continue;}
   if(steps>=3&&autos>=1)advance.add(phase);
   if(!advance.contains(phase))continue;
   if(n.isProtectingMinion()){
    NPC minion=((NPC[])field(event,NexAreaEvent.class,"minions"))[phase.ordinal()-1];check(minion!=null,"Threshold exposes the matching live minion");
    p.setLocation(minion.getLocation().transform(1,0,0));damage(p,minion,1000);
    if(minion.isDead())p.setLocation(Location.locate(2927,5202,0));
   }else damage(p,n,1000);
  }
  check(phases.size()==6&&finishedAt>0,"Natural encounter reaches all phases, wrath, reward completion and removal; seen="+phases);
  long rewarded=org.dementhium.model.misc.GroundItemManager.getGroundItems().stream().filter(item->item.getItem().getId()==565).count();check(rewarded==1,"Actual NPC loot path creates exactly one guaranteed fixture drop");
  for(int t=0;t<170;t++)cycle();check(event.getNex()!=null&&event.getNex()!=dying,"Natural post-kill arena respawn creates a new Nex life");
 }
 public static void main(String[] args)throws Exception{CombatFixtures.init();List<String> failures=new ArrayList<String>();for(String group:new String[]{"assets","doors","core","nex"})try{if(group.equals("assets"))assets();else if(group.equals("doors"))doors();else if(group.equals("core"))core();else nex();}catch(Throwable failure){failures.add(group+": "+failure);failure.printStackTrace();}if(!failures.isEmpty())throw new AssertionError(failures.toString());System.out.println("Boss encounter completion: "+checks+" checks passed");}
}
