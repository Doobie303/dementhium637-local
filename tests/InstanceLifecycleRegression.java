import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import org.jboss.netty.channel.Channel;
import org.dementhium.cache.Cache;
import org.dementhium.content.activity.*;
import org.dementhium.content.activity.impl.DefaultActivity;
import org.dementhium.model.*;
import org.dementhium.model.definition.*;
import org.dementhium.model.instance.*;
import org.dementhium.model.map.*;
import org.dementhium.model.map.region.*;
import org.dementhium.model.misc.*;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.player.*;
import org.dementhium.net.*;
import org.dementhium.tickable.Tick;
import org.dementhium.util.MapXTEA;

public final class InstanceLifecycleRegression {
    static int checks;
    static Location exit = Location.locate(3200,3200,0);
    static List<Tick> scheduled = new ArrayList<Tick>();
    static void check(boolean ok, String message) { checks++; if (!ok) throw new AssertionError(message); }
    static void rejects(Runnable action, String message) {
        try { action.run(); } catch (IllegalStateException | IllegalArgumentException expected) { checks++; return; }
        throw new AssertionError(message);
    }
    static Map<Player, FaultRegion> faults = new IdentityHashMap<Player, FaultRegion>();
    static class FaultRegion extends RegionData {
        boolean rejectTeleport, throwAfterTeleport;
        FaultRegion(Player player) { super(player); }
        public void teleport(int x, int y, int z) {
            if (rejectTeleport) return;
            super.teleport(x,y,z);
            if (throwAfterTeleport) { throwAfterTeleport=false; throw new IllegalStateException("Injected post-teleport failure"); }
        }
    }
    static Channel channel() {
        return (Channel) Proxy.newProxyInstance(Channel.class.getClassLoader(), new Class[]{Channel.class}, (proxy,m,args) -> {
            if (m.getName().equals("isConnected") || m.getName().equals("isOpen")) return true;
            if (m.getReturnType() == boolean.class) return false;
            if (m.getReturnType() == int.class) return 0;
            return null;
        });
    }
    static Player player(String name) throws Exception {
        Player p = new Player(new GameSession(channel()),new PlayerDefinition(name,"unused"));
        FaultRegion region=new FaultRegion(p);
        Field field=Player.class.getDeclaredField("region"); field.setAccessible(true); field.set(p,region); faults.put(p,region); p.setHasReceivedStarter(true); p.setLocation(exit); p.setOnline(true); return p;
    }
    static void copy(GameInstance i) { i.copyMap(360,648,0,0,8,8,new int[]{0,1,2,3},new int[]{0,1,2,3}); }
    static GameInstance create(InstanceManager manager, int capacity) { return manager.create(16,16,capacity,exit,InstanceLifecycleRegression::copy); }
    static GroundItem drop(Location location) { return new GroundItem(null,new Item(995,1),location,true,false,GroundItemManager.groundItemIndex++); }
    static void pulse(int cycles) {
        for (int c=0;c<cycles;c++) for (Tick tick : new ArrayList<Tick>(scheduled)) if (!tick.run()) scheduled.remove(tick);
    }
    static class CountingNpc extends NPC {
        int ticks;
        CountingNpc() { super(1); }
        public void tick() { ticks++; }
    }
    static class TestActivity extends Activity<Player> {
        int ended, updates; boolean fail;
        public boolean initializeActivity() { return true; }
        public boolean commenceSession() { return true; }
        public boolean updateSession() { updates++; return true; }
        public boolean endSession() { ended++; stop(); if (fail) throw new IllegalStateException("Injected activity cleanup failure"); return true; }
    }
    @SuppressWarnings("unchecked")
    static void activities() throws Exception {
        ActivityManager m = new ActivityManager();
        TestActivity a = new TestActivity(), b = new TestActivity(), c = new TestActivity();
        check(a.getActivityId()==-1,"unregistered sentinel");
        check(m.register(a) && m.register(b) && m.register(c),"register activities");
        int idB = b.getActivityId(), idC = c.getActivityId();
        check(a.getActivityId()==0,"first activity retains Castle Wars convention");
        check(!m.register(b),"duplicate registration");
        new DefaultActivity().stop();
        check(m.getActivities().size()==3,"default activity cannot unregister ID zero");
        check(m.unregister(a),"first unregister");
        check(a.ended==1 && !a.isRunning(),"recursive stop ends once");
        check(b.getActivityId()==idB && c.getActivityId()==idC && m.getActivity(idC)==c,"remaining IDs stable");
        rejects(() -> b.setActivityId(77),"registered ID immutable");
        check(m.unregister(b) && b.ended==1,"stable ID removes correct activity after earlier removal");
        check(!m.unregister(b) && !m.unregister(1000,true),"repeat and unknown unregister");
        try { m.getActivities().clear(); throw new AssertionError("mutable activities"); } catch (UnsupportedOperationException expected) { checks++; }
        TestActivity d = new TestActivity();
        check(m.register(d) && d.getActivityId()>idC,"no ID reuse");
        // Stale scheduler wrapper must not execute a restarted registration.
        Field pending = World.class.getDeclaredField("ticksToAdd"); pending.setAccessible(true);
        List<Tick> old = new ArrayList<Tick>((Collection<Tick>)pending.get(World.getWorld()));
        m.unregister(d,false); d.start(); m.register(d);
        d.setActivityState(Activity.SessionStates.UPDATE_STATE);
        for (Tick t : old) t.run();
        check(d.updates==0,"stale registration callback cannot execute restarted activity");
        c.fail=true;
        rejects(() -> m.reset(),"reset surfaces callback failure");
        check(m.getActivities().isEmpty() && c.ended==1 && d.ended==1,"reset completes other cleanup despite failure");
        TestActivity natural = new TestActivity(); m.register(natural);
        natural.setActivityState(Activity.SessionStates.END_STATE); natural.execute();
        check(natural.ended==1 && m.getActivities().isEmpty(),"natural end recursive stop ends once");
    }
    public static void main(String[] args) throws Exception {
        Cache.init(); check(MapXTEA.loadPackedFile(),"keys"); GroundItemManager.load(); ItemDefinition.init(); NPCDefinition.init();
        RegionBuilder.init();
        InstanceManager manager = new InstanceManager(scheduled::add);
        rejects(() -> create(manager,1),"off-cycle creation rejected");
        CompletableFuture<GameInstance> queued = manager.submit(() -> create(manager,2));
        check(!queued.isDone(),"request deferred");
        manager.beginCycle(); manager.drainRequests();
        GameInstance a = queued.get(), b = create(manager,2);
        check(a.isActive() && b.isActive() && a.getId()!=b.getId(),"distinct active sessions");
        Location aTile=a.location(44,13,0), bTile=b.location(44,13,0);
        check(a.contains(aTile) && !a.contains(bTile),"built bounds isolation");
        check(!a.contains(a.location(100,100,0)),"unbuilt chunk excluded");
        check(a.canOccupy(aTile,1) && !a.canOccupy(a.location(39,12,0),1),"solid spawn rejected");
        rejects(() -> a.location(1,1,4),"invalid plane not silently modulo");
        rejects(() -> a.copyMap(360,648,0,0,1,1,new int[]{0},new int[]{0}),"active template immutable");
        Player p=player("one"), q=player("two"), duplicate=player("ONE"), third=player("three");
        a.enter(p,aTile);
        check(a.isMember(p) && manager.getInstance(p)==a && p.getLocation().equals(aTile),"entry claims and teleports");
        rejects(() -> b.enter(p,bTile),"duplicate player");
        rejects(() -> b.enter(duplicate,bTile),"duplicate account");
        a.enter(q,aTile);
        rejects(() -> a.enter(third,aTile),"capacity enforced");
        a.leave(q); b.enter(q,bTile);
        check(!manager.canInteract(p,q),"cross instance interaction policy");
        NPC npc = a.spawnNpc(new NPC(1),aTile);
        NPC other = b.spawnNpc(new NPC(1),bTile);
        CountingNpc dead = new CountingNpc(); b.spawnNpc(dead,bTile);
        int deathTasks=b.getTaskCount();
        dead.sendDead();
        check(b.getTaskCount()==deathTasks+2,"real NPC death loot/removal callbacks inherit ownership");
        pulse(Math.max(1,dead.getDeathTick()));
        check(!b.owns(dead) && World.getWorld().getNpcs().get(dead.getIndex())!=dead,"ordinary death removes owned NPC");
        new org.dementhium.task.impl.NPCTickTask(dead).execute();
        check(dead.ticks==0,"already queued NPC tick cannot run after removal");
        check(a.owns(npc) && !a.owns(other) && manager.canInteract(p,npc),"NPC ownership");
        rejects(() -> b.spawnNpc(npc,bTile),"foreign NPC cannot be adopted");
        final int[] actions={0};
        Tick task=a.schedule(2,3,() -> actions[0]++);
        pulse(1); check(actions[0]==0,"initial delay");
        pulse(1); check(actions[0]==1,"first execution");
        pulse(2); check(actions[0]==1,"period delay");
        pulse(1); check(actions[0]==2,"period execution");
        Tick local = new Tick(1) { public void execute() { actions[0]+=100; } };
        npc.submitTick("pending",local);
        Tick callback = a.submitNpcTask(npc,new Tick(20) { public void execute() { actions[0]+=1000; stop(); } });
        GroundItem ordinary=drop(aTile), otherDrop=drop(bTile);
        GroundItemManager.createGroundItem(ordinary); b.spawnDrop(otherDrop);
        RespawnableGroundItem respawn=new RespawnableGroundItem(new Item(995,1),aTile,20);
        a.spawnDrop(respawn); GroundItemManager.removeGroundItem(respawn);
        int pendingTasks=a.getTaskCount();
        check(pendingTasks>=3,"NPC and respawn callbacks owned");
        GameObject object=a.spawnObject(1,a.location(45,13,0),10,0);
        GameObject adjacent=a.spawnObject(1,a.location(45,13,0),9,0);
        a.removeObject(object);
        check(adjacent.getLocation().getGameObjectType(9)==adjacent,"exact object removal preserves neighbouring type");
        // Close blocked by evacuation failure, and no tasks may continue during retry.
        faults.get(p).rejectTeleport=true;
        check(!a.close() && a.getState()==GameInstance.State.CLOSING && manager.get(a.getId())==a,"failed evacuation quarantines instance");
        check(!a.getCloseFailures().isEmpty() && a.getMemberCount()==1,"failure diagnostics and member retained");
        check(!local.isRunning() && !callback.isRunning() && a.getNpcCount()==0,"NPC callbacks and local ticks cancelled");
        check(!GroundItemManager.getGroundItems().contains(ordinary),"naturally created drop tracked and cleaned");
        rejects(() -> a.enter(third,aTile),"closing admission rejected");
        rejects(() -> a.schedule(1,()->{}),"closing work rejected");
        rejects(() -> task.start(),"cancelled task cannot restart");
        pulse(30); task.execute(); callback.execute();
        check(actions[0]==2 && !GroundItemManager.getGroundItems().contains(respawn),"stale callbacks cannot run or respawn");
        check(b.isActive() && b.isMember(q) && b.owns(other) && GroundItemManager.getGroundItems().contains(otherDrop),"neighbour instance unchanged");
        faults.get(p).rejectTeleport=false;
        check(a.close() && a.close() && manager.get(a.getId())==null && p.getLocation().equals(exit),"retry and repeated close");
        check(InstanceManager.at(aTile)==null && manager.getInstance(p)==null,"registry and membership released");
        GameInstance reused=create(manager,1);
        check(reused.getId()>b.getId(),"reservation reuse has fresh session identity");
        pulse(30); check(reused.isActive() && actions[0]==2,"old tasks cannot affect later session");
        // Entry failure before and after moving must not leak membership.
        faults.get(third).rejectTeleport=true;
        rejects(() -> reused.enter(third,reused.location(44,13,0)),"failed entry detected");
        check(manager.getInstance(third)==null && reused.getMemberCount()==0,"failed entry rollback");
        faults.get(third).rejectTeleport=false; faults.get(third).throwAfterTeleport=true;
        rejects(() -> reused.enter(third,reused.location(44,13,0)),"partial entry failure");
        check(manager.getInstance(third)==null && third.getLocation().equals(exit),"partial entry evacuated");
        // Foreign occupants cause retryable close; never silently delete them.
        NPC foreign=new NPC(1);
        rejects(() -> foreign.setLocation(reused.location(44,13,0)),"step 3 rejects foreign placement");
        // Inject corrupt spatial state to retain the defensive cleanup regression.
        foreign.destroy();
        Field foreignLocation=Entity.class.getDeclaredField("location"); foreignLocation.setAccessible(true);
        foreignLocation.set(foreign,reused.location(44,13,0));
        foreign.getLocation().getRegion().addEntity(foreign);
        World.getWorld().getNpcs().add(foreign);
        check(!reused.close() && World.getWorld().getNpcs().get(foreign.getIndex())==foreign,"foreign occupant retains map");
        World.getWorld().getNpcs().remove(foreign);
        check(reused.close(),"retry after foreign occupant evacuated");
        int before=RegionBuilder.getDynamicRegionCount(), registered=manager.getInstances().size();
        final GameInstance[] failed={null};
        rejects(() -> manager.create(16,16,1,exit,i -> {
            failed[0]=i; copy(i); i.spawnNpc(new NPC(1),i.location(44,13,0));
            i.spawnDrop(drop(i.location(44,13,0))); i.schedule(1,()->actions[0]+=10000);
            throw new IllegalStateException("Injected initializer failure");
        }),"initializer failure propagated");
        check(failed[0].getState()==GameInstance.State.CLOSED && manager.getInstances().size()==registered
            && RegionBuilder.getDynamicRegionCount()==before,"creation rollback releases partial resources and map");
        // Explicit offline close supports evacuation; automatic logout integration is step 3.
        q.setOnline(false);
        check(b.close() && q.getLocation().equals(exit) && manager.closeAll(),"offline explicit evacuation and closeAll");
        manager.endCycle();
        rejects(() -> b.close(),"off-cycle mutation rejected");
        CompletableFuture<Integer> failure=manager.submit(() -> {throw new IllegalStateException("request failure");});
        CompletableFuture<Integer> next=manager.submit(() -> 7);
        CompletableFuture<Integer> cancelled=manager.submit(() -> {throw new AssertionError("cancelled task ran");});
        cancelled.cancel(false);
        manager.beginCycle(); manager.drainRequests();
        check(failure.isCompletedExceptionally() && next.get()==7 && cancelled.isCancelled(),"queued failures isolated and cancellation respected");
        manager.endCycle();
        InstanceManager rejecting=new InstanceManager(t -> {throw new IllegalStateException("Injected scheduler failure");});
        rejecting.beginCycle();
        rejects(() -> rejecting.create(16,16,1,exit,i -> {copy(i);i.schedule(1,()->{});}),"scheduler failure rolls creation back");
        check(rejecting.getInstances().isEmpty(),"scheduler failure leaves no session");
        rejecting.endCycle();
        InstanceManager world=InstanceManager.getSingleton();
        CompletableFuture<GameInstance> created=world.submit(() -> create(world,1));
        World.getWorld().run();
        check(created.isDone() && !created.isCompletedExceptionally(),"production world loop drains lifecycle requests");
        GameInstance live=created.get();
        CompletableFuture<Boolean> closed=world.submit(() -> live.close());
        World.getWorld().run();
        check(closed.get(),"production world loop closes requested session");
        rejects(() -> world.closeAll(),"production mutation window closed after world cycle");
        activities();
        check(RegionBuilder.getDynamicRegionCount()==0,"all reservations released");
        System.out.println("InstanceLifecycleRegression passed: " + checks + " checks");
    }
}
