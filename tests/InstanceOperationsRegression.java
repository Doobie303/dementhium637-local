import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import org.dementhium.cache.Cache;
import org.dementhium.content.areas.AreaManager;
import org.dementhium.content.instance.InstanceOperations;
import org.dementhium.model.*;
import org.dementhium.model.definition.*;
import org.dementhium.model.instance.*;
import org.dementhium.model.map.*;
import org.dementhium.model.map.region.*;
import org.dementhium.model.misc.*;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.player.Player;
import org.dementhium.tickable.Tick;
import org.dementhium.util.MapXTEA;

/** Fault, overload and timeout tests use real cache maps and in-memory players; never call World.unregister. */
public final class InstanceOperationsRegression {
    static int checks;
    static final Location EXIT = InstancePartyRegression.EXIT;
    static final AtomicLong time = new AtomicLong();
    static final List<Tick> tasks = new ArrayList<Tick>();
    static void check(boolean ok, String why) { checks++; if (!ok) throw new AssertionError(why); }
    static void rejects(Runnable run, String why) { try { run.run(); } catch (IllegalArgumentException | IllegalStateException e) { checks++; return; } throw new AssertionError(why); }
    static InstanceLimits limits(String... pairs) { Properties p = new Properties(); for (int n=0;n<pairs.length;n+=2) p.setProperty(pairs[n],pairs[n+1]); return new InstanceLimits(p); }
    static InstanceManager manager(String... pairs) { InstanceManager m = new InstanceManager(tasks::add,limits(pairs),time::get); m.beginCycle(); return m; }
    static GameInstance room(InstanceManager m) { return m.create(8,8,2,EXIT,InstancePartyRegression::map); }
    static void next(InstanceManager m) { m.endCycle(); m.beginCycle(); }
    static void finish(InstanceManager m) { check(m.closeAll(),"cleanup complete"); tasks.removeIf(t -> !t.isRunning()); check(tasks.isEmpty(),"no live tasks"); check(m.getInstances().isEmpty(),"registry empty"); m.endCycle(); }
    static Player player(String name) throws Exception { return InstancePartyRegression.player(name); }
    static void queues() throws Exception {
        InstanceManager m=manager("requests","2","requestsPerCycle","1","loginRequests","1","lifecyclePerCycle","1");
        List<String> ran=new ArrayList<String>();
        CompletableFuture<Void> first=m.submit(()->{ran.add("general");return null;});
        CompletableFuture<Void> cancelled=m.submit(()->{ran.add("cancelled");return null;});cancelled.cancel(false);
        check(m.submit(()->null).isCompletedExceptionally(),"bounded ordinary queue");
        Player p=player("opsqueue"),q=player("opsqueueq");
        CompletableFuture<Void> login=m.submitLifecycle(p,false,()->ran.add("login"));
        check(m.submitLifecycle(q,false,()->ran.add("badlogin")).isCompletedExceptionally(),"login bound");
        CompletableFuture<Void> logout=m.submitLifecycle(p,true,()->ran.add("logout"));
        check(m.submitLifecycle(p,true,()->ran.add("duplicate"))==logout,"coalesced logout identity");
        m.drainRequests();check(ran.equals(Arrays.asList("logout","login","general")),"logout priority and separate lane despite full queue");
        check(login.isDone()&&logout.isDone()&&first.isDone(),"futures resolve");m.drainRequests();check(ran.size()==3,"cancelled action skipped");
        CompletableFuture<Void> failure=m.submitLifecycle(p,true,()->{throw new IllegalStateException("injected");});m.drainRequests();check(failure.isCompletedExceptionally(),"lifecycle exception resolves future");
        CompletableFuture<Void> pending=m.submit(()->null),pendingLogin=m.submitLifecycle(q,false,()->ran.add("shutdownlogin"));
        check(m.shutdown(),"shutdown drains");check(pending.isCompletedExceptionally()&&pendingLogin.isCompletedExceptionally(),"shutdown resolves all lanes");
        check(m.submit(()->null).isCompletedExceptionally(),"shutdown rejects");finish(m);
        InstanceManager concurrent=manager("requests","16");
        List<CompletableFuture<Integer>> results=Collections.synchronizedList(new ArrayList<CompletableFuture<Integer>>());
        List<Thread> workers=new ArrayList<Thread>();
        for(int n=0;n<8;n++){Thread worker=new Thread(()->{for(int j=0;j<100;j++)results.add(concurrent.submit(()->1));});workers.add(worker);worker.start();}
        for(Thread worker:workers)worker.join();
        check(results.stream().filter(f->!f.isCompletedExceptionally()).count()==16,"concurrent queue bound exact");
        concurrent.drainRequests();check(results.stream().allMatch(CompletableFuture::isDone),"all concurrent submissions resolved");finish(concurrent);
    }
    static void reservations() {
        rejects(()->limits("typo","1"),"unknown setting");rejects(()->limits("sessions","0"),"invalid setting");rejects(()->limits("idleSeconds","-1"),"negative timeout");
        InstanceManager m=manager("sessions","1");GameInstance one=room(m);int regions=RegionBuilder.getDynamicRegionCount();
        check(m.total("reservedRegions")==regions,"region accounting includes guard");check(one.getBuiltChunkPlanes()==64,"built chunk-plane diagnostics");rejects(()->room(m),"session cap");check(RegionBuilder.getAllocationCount()==1,"rejection does not allocate");finish(m);
        for(String[] pair:new String[][]{{"chunkPlanesPerSession","128"},{"chunkPlanes","128"},{"reservedRegions","35"},{"membersPerSession","1"}}){
            InstanceManager limited=manager(pair);rejects(()->room(limited),"reservation cap "+pair[0]);check(RegionBuilder.getAllocationCount()==0,"reservation rejected before allocation");finish(limited);
        }
        InstanceManager builds=manager("createsPerCycle","1","buildChunksPerCycle","64");GameInstance a=room(builds);
        rejects(()->room(builds),"creation rate");next(builds);GameInstance b=builds.create(16,16,2,EXIT,InstancePartyRegression::map);
        rejects(()->b.buildRoom(360,648,8,0,8,8,new int[]{0},new int[]{0}),"map work bound");check(!b.contains(b.location(108,19,0)),"failed build unpublished");next(builds);
        b.buildRoom(360,648,8,0,8,8,new int[]{0},new int[]{0});check(b.contains(b.location(108,19,0)),"build budget resets");check(b.getBuiltChunkPlanes()==128,"expanded built chunk-plane diagnostics");finish(builds);
        InstanceManager rollback=manager("objectsPerSession","1");
        rejects(()->rollback.create(8,8,1,EXIT,i->{InstancePartyRegression.map(i);i.spawnObject(1,i.location(45,13,0),10,0);i.spawnObject(1,i.location(46,13,0),10,0);}),"initializer limit rolls back");
        check(rollback.total("objects")==0&&RegionBuilder.getAllocationCount()==0,"initializer rollback all resources");finish(rollback);
    }
    static void resources() throws Exception {
        InstanceManager m=manager("npcsPerSession","1","npcs","1","objectsPerSession","1","objects","1","tasksPerSession","1","tasks","1","dropsPerSession","1","drops","1");
        GameInstance a=room(m),b=room(m);Location spawn=a.location(44,13,0);Player p=player("opsloot");a.enter(p,spawn);
        NPC npc=a.spawnNpc(new NPC(1),spawn);rejects(()->a.spawnNpc(new NPC(1),spawn),"per session NPC bound");rejects(()->b.spawnNpc(new NPC(1),b.location(44,13,0)),"global NPC bound");a.removeNpc(npc);b.spawnNpc(new NPC(1),b.location(44,13,0));
        GameObject object=a.spawnObject(1,a.location(45,13,0),10,0);rejects(()->a.spawnObject(1,a.location(46,13,0),10,0),"object bound");rejects(()->b.spawnObject(1,b.location(45,13,0),10,0),"global object bound");a.removeObject(object);b.spawnObject(1,b.location(45,13,0),10,0);
        Tick task=a.schedule(10,()->{});rejects(()->a.schedule(1,()->{}),"task bound");rejects(()->b.schedule(1,()->{}),"global task bound");task.stop();b.schedule(10,()->{});
        GroundItem first=new GroundItem(p,new Item(995,7),spawn,false,true,GroundItemManager.groundItemIndex++);a.spawnDrop(first);
        GroundItem excess=new GroundItem(p,new Item(995,19),spawn,false,true,GroundItemManager.groundItemIndex++);a.spawnDrop(excess);
        check(a.getDropCount()==1&&m.total("drops")==1,"drop cap");
        GroundItem outside=null;for(GroundItem drop:GroundItemManager.getGroundItems())if(drop.getLocation().equals(EXIT)&&drop.getItem().getAmount()==19)outside=drop;
        check(outside!=null&&outside.getPlayer()==p&&!outside.isPublic()&&outside.isAdminDrop(),"overflow value/owner/visibility preserved at exit");
        GroundItemManager.discardGroundItem(outside);GroundItemManager.discardGroundItem(first);check(a.getDropCount()==0,"drop capacity released");
        String stats=m.diagnostics().toString();check(stats.contains("overflowDrops=1")&&stats.contains("npcs 1/1"),"diagnostic counters");finish(m);
    }
    static void admissionAndExpiry() throws Exception {
        InstanceManager m=manager("members","1","admissionsPerAccount","1","admissionAccounts","1","admissionWindowSeconds","10");
        Player p=player("opsadmit"),q=player("opsadmitq");GameInstance a=room(m),b=room(m);a.enter(p,InstancePartyRegression.spawn(a));
        rejects(()->b.enter(q,InstancePartyRegression.spawn(b)),"global member cap");a.leave(p);
        rejects(()->a.enter(p,InstancePartyRegression.spawn(a)),"per-account throttle");rejects(()->a.enter(q,InstancePartyRegression.spawn(a)),"bounded throttle records");
        time.addAndGet(10000000000L);a.enter(q,InstancePartyRegression.spawn(a));check(a.isMember(q),"admission window expiry");finish(m);
        InstanceManager expiry=manager("emptySeconds","3");GameInstance empty=room(expiry),occupied=room(expiry);occupied.enter(p,InstancePartyRegression.spawn(occupied));
        time.addAndGet(3000000000L);expiry.maintain();check(empty.getState()==GameInstance.State.CLOSED&&empty.getCloseReason().equals("empty timeout"),"abandoned map expires");
        time.addAndGet(100000000000L);expiry.maintain();check(occupied.isActive(),"occupied idle disabled by default");occupied.leave(p);time.addAndGet(2000000000L);expiry.maintain();check(occupied.isActive(),"empty age begins at last departure");time.addAndGet(1000000000L);expiry.maintain();check(occupied.getState()==GameInstance.State.CLOSED,"last departure expiry");finish(expiry);
        InstanceManager idle=manager("idleSeconds","3");GameInstance active=room(idle);active.enter(p,InstancePartyRegression.spawn(active));
        time.addAndGet(2000000000L);p.teleport(active.location(44,13,0),false);time.addAndGet(2000000000L);idle.maintain();check(active.isActive(),"movement refreshes activity");
        time.addAndGet(1000000000L);idle.maintain();check(active.getState()==GameInstance.State.CLOSED&&p.getLocation().equals(EXIT),"opt-in idle safely evacuates");finish(idle);
        InstanceManager faults=manager();GameInstance retained=room(faults);retained.enter(p,InstancePartyRegression.spawn(retained));
        InstancePartyRegression.regions.get(p).rejectAll=true;check(!retained.close("operator test"),"failed evacuation quarantines");
        check(retained.inspect().toString().contains("attempts=1")&&!retained.getCloseFailures().isEmpty(),"actionable cleanup diagnostics");
        faults.maintain();check(retained.inspect().toString().contains("attempts=2"),"maintenance retries cleanup");InstancePartyRegression.regions.get(p).rejectAll=false;faults.maintain();check(retained.getState()==GameInstance.State.CLOSED,"quarantine eventually released");
        check(faults.recentClosures().toString().contains("operator test"),"original close reason retained");
        GameInstance admin=room(faults);p.getDefinition().setRights(0);InstanceOperations.command(faults,p,new String[]{"instances","close",Long.toString(admin.getId())});check(admin.isActive(),"nonadmin close denied");
        p.getDefinition().setRights(2);InstanceOperations.command(faults,p,new String[]{"instances","inspect",Long.toString(admin.getId())});InstanceOperations.command(faults,p,new String[]{"instances","list","0"});check(admin.isActive(),"inspection and malformed command preserve session");
        InstanceOperations.command(faults,p,new String[]{"instances","close",Long.toString(admin.getId())});check(admin.getState()==GameInstance.State.CLOSED,"admin close succeeds");finish(faults);
    }
    static void productionCapacity() throws Exception {
        InstanceManager m=new InstanceManager(tasks::add,InstanceLimits.load(),time::get);m.beginCycle();
        List<GameInstance> rooms=new ArrayList<GameInstance>();
        for(int group=0;group<8;group++){
            if(group>0)next(m);
            for(int n=0;n<4;n++){
                GameInstance room=m.create(8,8,16,EXIT,InstancePartyRegression::map);rooms.add(room);
                for(int member=0;member<4;member++){Player p=player("cap"+group+"x"+n+"x"+member);room.enter(p,InstancePartyRegression.spawn(room));}
            }
        }
        check(m.getInstances().size()==32&&m.total("members")==128,"configured session/member ceiling reached");
        check(m.total("reservedRegions")==1152&&RegionBuilder.getDynamicRegionCount()==1152,"production ceiling reservation accounting");
        next(m);rejects(()->room(m),"session 33 rejected cleanly");Player extra=player("capextra");
        rejects(()->rooms.get(0).enter(extra,InstancePartyRegression.spawn(rooms.get(0))),"member 129 rejected cleanly");
        check(InstanceAccess.owner(extra)==null&&extra.getLocation().equals(EXIT),"rejected member stays ordinary world");
        InstanceOperations.command(m,extra,new String[]{"instances","close",Long.toString(rooms.get(0).getId())});
        check(m.total("members")==124&&m.getInstances().size()==31,"admin close works at capacity");
        GameInstance replacement=room(m);replacement.enter(extra,InstancePartyRegression.spawn(replacement));check(replacement.isMember(extra),"capacity immediately reusable");finish(m);
        InstanceManager history=manager();
        for(int n=0;n<40;n++)history.create(1,1,1,EXIT,i->{}).close("history "+n);
        check(history.recentClosures().size()==32&&history.recentClosures().get(0).contains("history 8"),"bounded closure history");finish(history);
        InstanceManager nested=manager();Player p=player("nestedshutdown");
        CompletableFuture<Void> first=nested.submitLifecycle(p,true,()->nested.shutdown());
        CompletableFuture<Void> second=nested.submitLifecycle(extra,true,()->{throw new AssertionError("shutdown action ran");});
        nested.drainRequests();check(first.isDone()&&second.isCompletedExceptionally(),"shutdown during lifecycle drain resolves remaining requests");finish(nested);
    }
    public static void init() throws Exception {
        Cache.init();MapXTEA.loadPackedFile();ItemDefinition.init();NPCDefinition.init();GroundItemManager.load();
        Field areas=World.class.getDeclaredField("areaManager");areas.setAccessible(true);areas.set(World.getWorld(),new AreaManager());
    }
    public static void main(String[] args) throws Exception {
        init();queues();reservations();resources();admissionAndExpiry();productionCapacity();
        check(RegionBuilder.getAllocationCount()==0&&RegionBuilder.getDynamicRegionCount()==0,"allocator baseline");
        for(Player p:InstancePartyRegression.players)World.getWorld().getPlayers().remove(p);
        System.out.println("InstanceOperationsRegression: "+checks+" checks passed");
    }
}


