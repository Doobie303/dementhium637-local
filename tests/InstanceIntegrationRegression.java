import java.lang.reflect.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.buffer.*;
import org.dementhium.cache.Cache;
import org.dementhium.content.areas.*;
import org.dementhium.content.misc.*;
import org.dementhium.model.*;
import org.dementhium.model.combat.*;
import org.dementhium.model.definition.*;
import org.dementhium.model.instance.*;
import org.dementhium.model.map.*;
import org.dementhium.model.map.region.*;
import org.dementhium.model.misc.*;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.player.*;
import org.dementhium.net.*;
import org.dementhium.net.message.*;
import org.dementhium.net.packethandlers.*;
import org.dementhium.tickable.Tick;
import org.dementhium.util.*;

public class InstanceIntegrationRegression {
    static int checks;
    static final Location EXIT=Location.locate(3200,3200,0);
    static final List<Tick> tasks=new ArrayList<Tick>();
    static final Map<Player,AtomicBoolean> connections=new IdentityHashMap<Player,AtomicBoolean>();
    static void check(boolean condition,String message) { checks++; if(!condition) throw new AssertionError(message); }
    static void rejects(Runnable action,String message) {
        try {action.run();} catch(IllegalStateException|IllegalArgumentException expected) {checks++;return;}
        throw new AssertionError(message);
    }
    static Player player(String name) {
        AtomicBoolean connected=new AtomicBoolean(true);
        Channel channel=(Channel)Proxy.newProxyInstance(Channel.class.getClassLoader(),new Class[]{Channel.class},(proxy,m,args)->{
            if(m.getName().equals("isConnected")||m.getName().equals("isOpen")) return connected.get();
            if(m.getReturnType()==boolean.class) return false;
            if(m.getReturnType()==int.class) return 0;
            return null;
        });
        Player p=new Player(new GameSession(channel),new PlayerDefinition(name,"unused"));
        p.setLocation(EXIT);p.setHasReceivedStarter(true);p.setOnline(true);
        connections.put(p,connected);
        return p;
    }
    static void copy(GameInstance i) {i.copyMap(360,648,0,0,8,8,new int[]{0,1,2,3},new int[]{0,1,2,3});}
    static GameInstance create(InstanceManager m,int capacity) {return m.create(16,16,capacity,EXIT,InstanceIntegrationRegression::copy);}
    static Location spawn(GameInstance i) {return i.location(44,13,0);}
    static void pulse(int count) {
        for(int n=0;n<count;n++) for(Tick tick:new ArrayList<Tick>(tasks)) if(!tick.run()) tasks.remove(tick);
    }
    static ChannelBuffer save(Player p) {ChannelBuffer data=ChannelBuffers.dynamicBuffer();p.save(data);return data;}
    static Player load(ChannelBuffer bytes,String name) {Player p=player(name);p.load(bytes.toByteBuffer());return p;}
    public static void main(String[] args)throws Exception {
        Cache.init();MapXTEA.loadPackedFile();NPCDefinition.init();ItemDefinition.init();GroundItemManager.load();
        Field areas=World.class.getDeclaredField("areaManager");areas.setAccessible(true);areas.set(World.getWorld(),new AreaManager());
        InstanceManager manager=new InstanceManager(tasks::add);manager.beginCycle();
        GameInstance a=create(manager,2),b=create(manager,1);
        Location oldA=spawn(a),oldB=spawn(b);
        Player p=player("member"),q=player("partner"),outsider=player("outsider");
        a.enter(p,oldA);a.enter(q,oldA);
        outsider.teleport(oldA,false);
        check(outsider.getLocation().equals(EXIT),"teleport cannot bypass membership");
        rejects(()->outsider.setLocation(oldA),"direct placement cannot bypass membership");
        p.teleport(oldB,false);
        check(p.getLocation().equals(oldA)&&a.isMember(p),"foreign instance teleport rejected");
        p.teleport(a.location(100,100,0),false);
        check(p.getLocation().equals(oldA),"unbuilt destination teleport rejected");
        p.teleport(a.location(44,13,1),false);
        check(p.getLocation().getZ()==1&&a.isMember(p),"same-instance plane teleport allowed");
        check(!InstanceAccess.canInteract(p,q),"cross-plane managed interaction rejected");
        p.teleport(oldA,false);
        NPC local=a.spawnNpc(new NPC(1),oldA),foreign=b.spawnNpc(new NPC(1),oldB);
        rejects(()->local.setLocation(EXIT),"owned NPC cannot escape into ordinary world");
        check(!InstanceAccess.canWalk(p,oldB)&&!InstanceAccess.canWalk(p,a.location(100,100,0)),"walking rejects foreign/unbuilt tiles");
        check(InstanceAccess.canWalk(p,oldA),"built walkable tile allowed");
        Location edge=a.location(63,13,0);
        // Open a two-tile corridor in this copy so the test exercises membership, not terrain clipping.
        Region.removeClipping(edge.getX(),edge.getY(),0,0x200100);
        Region.removeClipping(edge.getX()-1,edge.getY(),0,0x200100);
        p.teleport(edge,false);p.getRegion().setDidTeleport(false);
        p.getWalkingQueue().reset();p.addPoint(edge.getX()+1,edge.getY());
        p.getWalkingQueue().getNextEntityMovement();
        check(p.getLocation().equals(edge),"actual walking queue cannot enter unbuilt neighbour chunk");
        p.teleport(a.location(62,13,0),false);p.getRegion().setDidTeleport(false);
        p.getWalkingQueue().reset();p.getWalkingQueue().setIsRunning(true);p.addPoint(edge.getX()+1,edge.getY());
        p.getWalkingQueue().getNextEntityMovement();
        check(p.getLocation().equals(edge)&&p.getWalkingQueue().getWalkDir()!=-1&&p.getWalkingQueue().getRunDir()==-1,
            "blocked second run step retains first-step movement mask: location="+p.getLocation()+" edge="+edge+" walk="+p.getWalkingQueue().getWalkDir()+" run="+p.getWalkingQueue().getRunDir()+" mask="+Region.getClippingMask(edge.getX(),edge.getY(),0));
        p.getWalkingQueue().setIsRunning(false);
        p.teleport(oldA,false);
        p.getCombatExecutor().setVictim(foreign);
        check(p.getCombatExecutor().getVictim()==null,"cross-instance targeting rejected");
        p.getCombatExecutor().setVictim(local);
        check(p.getCombatExecutor().getVictim()==local,"same-instance targeting preserved");
        int hp=foreign.getHitPoints();
        foreign.getDamageManager().damage(p,1,1,DamageManager.DamageType.RED_DAMAGE);
        check(foreign.getHitPoints()==hp,"cross-instance direct impact rejected");
        GroundItem otherDrop=new GroundItem(null,new Item(995,5),oldB,true,false,1);
        b.spawnDrop(otherDrop);
        check(GroundItemManager.getQualifiedGroundItem(995,oldB,p)==null,"foreign ground item lookup rejected");
        new GroundItemActionHandler().pickup(p,otherDrop);
        check(GroundItemManager.getGroundItems().contains(otherDrop)&&!p.getInventory().contains(995),"direct stale pickup cannot cross instances");
        new NpcOption().handlePacket(outsider,new MessageBuilder(18).writeShort(local.getIndex()).writeByte(0).toMessage());
        check(outsider.getCombatExecutor().getVictim()==null,"forged NPC packet rejected");
        World.getWorld().getPlayers().add(p);
        new PlayerOption().handlePacket(outsider,new MessageBuilder(47).writeShort(p.getIndex()).writeByte(0).toMessage());
        check(outsider.getTradeSession()==null,"foreign trade packet cannot open session");
        // Do not invoke real save/unregister paths from a regression.
        World.getWorld().getPlayers().remove(p);p.setOnline(true);p.setLocation(oldA);
        Following.playerFollow(outsider,p);
        check(!outsider.hasTick("following_mob"),"foreign following request rejected");
        GameObject object=a.spawnObject(1,a.location(45,13,0),10,0);
        check(InstanceAccess.canInteract(p,object)&&!InstanceAccess.canInteract(outsider,object),"object membership policy");
        a.removeObject(object);
        check(!InstanceAccess.canInteract(p,object),"stale replaced object rejected");
        ChannelBuffer activeSave=save(p);
        ByteBuffer locationBytes=activeSave.toByteBuffer();BufferUtils.readRS2String(locationBytes);
        check(locationBytes.getShort()==EXIT.getX()&&locationBytes.getShort()==EXIT.getY()&&locationBytes.get()==EXIT.getZ(),"active save writes ordinary return coordinates");
        check(p.getLocation().equals(oldA)&&a.isMember(p),"save does not move/detach live member");
        Player recovered=load(activeSave,"restored");
        check(recovered.getLocation().equals(EXIT)&&InstanceAccess.owner(recovered)==null,"active save reloads without membership");
        // Simulate an old binary save containing coordinates now occupied by another run.
        ChannelBuffer oldSave=save(outsider);ByteBuffer pos=oldSave.toByteBuffer();BufferUtils.readRS2String(pos);int offset=pos.position();
        oldSave.setShort(offset,oldB.getX());oldSave.setShort(offset+2,oldB.getY());oldSave.setByte(offset+4,0);
        Player stale=load(oldSave,"stale");
        check(stale.getLocation().equals(Mob.DEFAULT),"old save cannot join an allocated/reused map");
        final int[] callbacks={0};
        World.getWorld().submitAreaEvent(p,new CoordinateEvent(p,oldA.getX(),oldA.getY(),1){public void execute(){callbacks[0]++;}});
        Tick area=p.getTick("area_event");
        Damage launched=Damage.getDamage(q,p,CombatType.MELEE,1);
        p.teleport(EXIT,false);
        check(InstanceAccess.owner(p)==null&&a.getMemberCount()==1&&a.isActive(),"teleport out detaches one member and preserves party");
        a.enter(p,oldA);
        area.run();
        check(callbacks[0]==0,"old area callback cannot act after leave/reentry to same instance");
        int pHp=p.getHitPoints();
        p.getDamageManager().damage(q,launched,DamageManager.DamageType.RED_DAMAGE);
        check(p.getHitPoints()==pHp,"pre-transition projectile cannot hit after reentry");
        p.teleport(EXIT,false);q.teleport(EXIT,false);
        check(a.getState()==GameInstance.State.CLOSED&&InstanceManager.at(oldA)==null,"last teleport out closes resources/map");
        outsider.teleport(oldA,false);
        check(outsider.getLocation().equals(EXIT),"stale teleport into released blank space rejected");
        check(InstanceAccess.readLocation(oldA.getX(),oldA.getY(),0).equals(Mob.DEFAULT),"restart recovers missing map without live registry");
        check(InstanceAccess.readLocation(3200,3200,4).equals(Mob.DEFAULT),"corrupt saved plane rejected before modulo");
        check(InstanceAccess.readLocation(3200,3200,0).equals(EXIT),"ordinary saves preserved");
        // Safe death at 5 cycles; early logout/close resolves it once before saving.
        GameInstance death=create(manager,2);
        Player victim=player("deadmember"),survivor=player("survivor");
        death.enter(victim,spawn(death));death.enter(survivor,spawn(death));
        victim.getInventory().addItem(995,50);
        victim.getSkills().setHitPoints(0);victim.getSkills().sendDead();
        check(victim.isDead()&&death.isMember(victim),"managed death remains in instance during animation");
        victim.teleport(EXIT,false);
        check(death.isMember(victim)&&death.contains(victim.getLocation()),"dying member cannot teleport around resolution");
        Player savedDead=load(save(victim),"deadrestore");
        check(savedDead.getLocation().equals(EXIT)&&savedDead.getHitPoints()>0&&savedDead.getInventory().contains(995),"safe pending-death autosave cannot become ordinary item-loss death");
        pulse(4);check(death.isMember(victim),"death delay preserved");
        pulse(1);
        check(InstanceAccess.owner(victim)==null&&victim.getLocation().equals(EXIT)&&victim.getHitPoints()>0,"death returns and restores player");
        check(victim.getInventory().getContainer().getNumberOf(new Item(995))==50&&death.isMember(survivor),"safe death preserves inventory and other members");
        survivor.getSkills().setHitPoints(0);survivor.getSkills().sendDead();
        connections.get(survivor).set(false);
        manager.maintain();
        check(death.getState()==GameInstance.State.CLOSED&&survivor.getHitPoints()>0&&survivor.getLocation().equals(EXIT),"disconnect during death resolves before cleanup/save");
        pulse(10);check(survivor.getHitPoints()>0,"cancelled death callback cannot repeat");
        // Standard policy uses the existing death-container/gravestone calculation at the safe exit.
        GameInstance risky=manager.create(16,16,1,EXIT,i->{copy(i);i.setDeathPolicy(GameInstance.DeathPolicy.STANDARD_AT_EXIT);});
        Player normal=player("normaldeath");risky.enter(normal,spawn(risky));
        for(int id:new int[]{1205,1277,1171,1351,995}) normal.getInventory().addItem(id,1);
        normal.getSkills().setHitPoints(0);normal.getSkills().sendDead();pulse(5);
        check(risky.getState()==GameInstance.State.CLOSED&&!normal.isDead(),"standard managed death completes and closes");
        GraveStone grave=GraveStoneManager.forName(normal.getUsername());
        check(grave!=null&&grave.getGrave().getLocation().equals(EXIT),"standard losses have an ordinary-world grave");
        check(!grave.getItems().isEmpty(),"standard policy retains ordinary item-loss calculation");
        grave.stop();World.getWorld().getNpcs().remove(grave.getGrave());GraveStoneManager.getGravestones().remove(normal.getUsername());
        for(GroundItem item:new ArrayList<GroundItem>(grave.getItems())) GroundItemManager.discardGroundItem(item);
        // Disconnect recovery does not depend on successfully writing to a closed channel.
        GameInstance disconnect=create(manager,1);Player disconnected=player("disconnected");disconnect.enter(disconnected,spawn(disconnect));
        connections.get(disconnected).set(false);manager.maintain();
        check(disconnect.getState()==GameInstance.State.CLOSED&&InstanceAccess.owner(disconnected)==null&&disconnected.getLocation().equals(EXIT),"disconnected member automatically evacuated");
        // Shutdown closes every instance, rejects new admission and completes pending futures.
        Player last=player("last");b.enter(last,spawn(b));
        last.getInventory().addItem(995,7);
        last.getSkills().setHitPoints(0);last.getSkills().sendDead();
        CompletableFuture<Integer> pending=manager.submit(()->123);
        check(manager.shutdown(),"shutdown releases instances");
        check(pending.isCompletedExceptionally()&&manager.getInstances().isEmpty()&&last.getLocation().equals(EXIT),"shutdown drains pending requests and evacuates before save");
        check(!last.isDead()&&last.getInventory().getContainer().getNumberOf(new Item(995))==7,"shutdown resolves pending safe death before save");
        check(manager.submit(()->1).isCompletedExceptionally(),"new shutdown request rejected");
        rejects(()->create(manager,1),"new shutdown creation rejected");
        check(RegionBuilder.getDynamicRegionCount()==0,"all test instance maps released");
        manager.endCycle();
        // Verify SessionLogoutTask dispatch does not mutate state from the background caller.
        InstanceManager world=InstanceManager.getSingleton();
        World.getWorld().submitTask(new org.dementhium.task.impl.SessionLogoutTask(outsider));
        CompletableFuture<Integer> ordered=world.submit(()->42);
        check(!ordered.isDone(),"session request queued until World.run");
        World.getWorld().run();
        check(ordered.get()==42,"production world loop processes queued session work");
        System.out.println("InstanceIntegrationRegression passed: "+checks+" checks");
    }
}
