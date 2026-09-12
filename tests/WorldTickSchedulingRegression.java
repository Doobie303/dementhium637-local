import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import org.dementhium.ServerThread;
import org.dementhium.WorldTickProbe;
import org.dementhium.WorldTickProbe.Executor;
import org.dementhium.WorldTickProbe.Row;
import org.dementhium.model.World;
import org.dementhium.model.Item;
import org.dementhium.model.Location;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.npc.NPCLoader;
import org.dementhium.model.player.Player;
import org.dementhium.tickable.Tick;

/** Bounded standalone production scheduler checks. See run-world-tick.ps1 for isolation. */
public final class WorldTickSchedulingRegression {
    static int checks;
    static final World world = World.getWorld();
    static final List<String> callbacks = new ArrayList<String>();
    static final List<String> violations = new ArrayList<String>();
    static final AtomicInteger interruptedOperations = new AtomicInteger();
    static final AtomicInteger observedInterrupt = new AtomicInteger();
    static final AtomicInteger calls = new AtomicInteger();
    static volatile int maxBlocked, maxWorkers;
    static volatile boolean stopObserver, stopDriver;
    static final CountDownLatch entered = new CountDownLatch(1);
    static final CountDownLatch release = new CountDownLatch(1);
    static final CountDownLatch asyncEntered = new CountDownLatch(1), asyncRelease = new CountDownLatch(1);
    static final AtomicInteger asyncActive = new AtomicInteger(), asyncOwnerFailures = new AtomicInteger();
    static final AtomicReference<Throwable> failure = new AtomicReference<Throwable>();
    static final List<NPC> combatNpcs = new ArrayList<NPC>();
    static final List<Player> combatPlayers = new ArrayList<Player>();
    static int moved, playerHpChanges, npcHpChanges, packets, projectiles;
    static int[] playerHp, npcHp;
    static Location[] positions;

    // Suppress automatic startup only; the driver invokes the unmodified production check().
    static final class ManualServer extends ServerThread {
        @Override public synchronized void start() { }
    }
    static void check(boolean ok, String why) {
        checks++;
        if (!ok) throw new AssertionError(why);
    }
    static void waitFor(java.util.function.BooleanSupplier condition, String why) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(12);
        while (!condition.getAsBoolean()) {
            if (failure.get() != null) throw new AssertionError("Driver failed", failure.get());
            if (System.nanoTime() > end) throw new AssertionError("Timeout: " + why);
            Thread.sleep(2);
        }
    }
    static void callback(String id, int tick) {
        WorldTickProbe.ownerCheck();
        callbacks.add(id + "@" + World.getTicks());
        if (World.getTicks() != tick) violations.add(id + " due=" + tick + " actual=" + World.getTicks());
    }
    static Tick once(final String id, int delay, final int due) {
        return new Tick(delay) {
            public void execute() { stop(); callback(id, due); }
        };
    }
    static void install(final String scenario) {
        final Tick cancelled = once("cancelled", 2, 1);
        world.submit(new Tick(1) {
            public void execute() {
                WorldTickProbe.ownerCheck();
                int tick = World.getTicks();
                if (scenario.startsWith("combat")) sampleCombat();
                if (tick == 0) {
                    callback("parent", 0);
                    cancelled.stop();
                    world.submit(once("child", 1, 1));
                    entered.countDown();
                    if (scenario.equals("async")) world.submitTask(() -> {
                        asyncActive.incrementAndGet();
                        if(org.dementhium.model.instance.InstanceManager.getSingleton().isCycleThread())asyncOwnerFailures.incrementAndGet();
                        asyncEntered.countDown();
                        try { if(!asyncRelease.await(5,TimeUnit.SECONDS))failure.set(new AssertionError("Async gate timed out")); }
                        catch(InterruptedException ex){failure.set(ex);}
                        finally {asyncActive.decrementAndGet();}
                    });
                    if (scenario.equals("interruptible")) {
                        try {
                            if (!release.await(10, TimeUnit.SECONDS)) violations.add("Operation timed out");
                            callback("operation-completed", 0);
                        } catch (InterruptedException expected) {
                            interruptedOperations.incrementAndGet();
                            // The operation cleared its status by specification, not by observation.
                        }
                    } else if (scenario.equals("slow") || scenario.equals("blocked")
                            || scenario.equals("shutdown")) {
                        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
                        while (release.getCount() != 0 && System.nanoTime() < end) {
                            if (Thread.currentThread().isInterrupted()) observedInterrupt.compareAndSet(0, 1);
                            // Monitor acquisition and CPU work do not respond to interruption.
                            if (!Thread.currentThread().isInterrupted()) LockSupport.parkNanos(1000000);
                            else Thread.yield();
                        }
                        if (release.getCount() != 0) violations.add("Gate timed out");
                    }
                    world.submit(once("during", 1, 1));
                }
                if (scenario.equals("cpu") && tick < 3) {
                    long end = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(1450);
                    long value = 17;
                    while (System.nanoTime() < end) {
                        value = value * 1664525 + 1013904223;
                        if (Thread.currentThread().isInterrupted()) observedInterrupt.compareAndSet(0, 1);
                    }
                    cpuSink = value;
                }
            }
        });
        world.submit(cancelled);
        world.submit(once("before", 2, 1));
        world.submit(new Tick(1) {
            int count;
            public void execute() {
                callback("repeat-" + count, count == 0 ? 0 : 2);
                if (++count == 2) stop(); else setTime(2);
            }
        });
        if (scenario.equals("failure")) {
            world.tickTasks.add(() -> { throw new IllegalStateException("expected isolated ordinary task failure"); });
            world.tickTasks.add(() -> callback("after-failure", 0));
        }
    }
    static volatile long cpuSink;

    static void combat(String scenario) throws Exception {
        int count = scenario.contains("32") ? 32 : 8;
        boolean god = scenario.contains("god");
        if (scenario.equals("combat-nex")) {
            NPC nex = NexBloodPhaseRegression.setup();
            combatNpcs.add(nex); combatPlayers.add(NexBloodPhaseRegression.player);
            Player second = BossEncounterCompletionRegression.player(nex);
            second.setLocation(nex.getLocation().transform(4,2,0)); combatPlayers.add(second);
            world.submit(NexBloodPhaseRegression.event);
        } else {
            for (int i=0; i<count; i++) {
                NPC npc = NPCLoader.getNPC(i%4==3 ? 54 : 84);
                npc.setLocation(Location.locate(3210+(i%8)*12,3210+(i/8)*14,0));
                npc.setOriginalLocation(npc.getLocation()); npc.setDoesWalk(false);
                npc.getRandom().setSeed(500+i); world.getNpcs().add(npc);
                Player player = BossEncounterCompletionRegression.player(npc);
                player.setLocation(npc.getLocation().transform(npc.size()+3,0,0));
                player.getEquipment().set(3,new Item(4151)); player.getBonuses().calculate();
                player.setAttribute("godmode",god);
                player.getCombatExecutor().setVictim(npc); npc.getCombatExecutor().setVictim(player);
                combatNpcs.add(npc); combatPlayers.add(player);
            }
        }
        playerHp=new int[combatPlayers.size()]; positions=new Location[combatPlayers.size()];
        npcHp=new int[combatNpcs.size()];
        for(int i=0;i<playerHp.length;i++){playerHp[i]=combatPlayers.get(i).getHitPoints();positions[i]=combatPlayers.get(i).getLocation();}
        for(int i=0;i<npcHp.length;i++)npcHp[i]=combatNpcs.get(i).getHitPoints();
    }
    static void sampleCombat() {
        for(int i=0;i<combatPlayers.size();i++) {
            Player player=combatPlayers.get(i);
            if(player.getHitPoints()!=playerHp[i])playerHpChanges++;
            playerHp[i]=player.getHitPoints();
            if(!player.getLocation().equals(positions[i]))moved++;
            positions[i]=player.getLocation();
            List<org.dementhium.net.message.Message> output=BossEncounterCompletionRegression.packets.get(player);
            packets+=output.size();
            for(org.dementhium.net.message.Message message:output)if(message.getOpcode()==15)projectiles++;
            output.clear();
        }
        for(int i=0;i<combatNpcs.size();i++) {
            NPC npc=combatNpcs.get(i);
            if(npc.getHitPoints()!=npcHp[i])npcHpChanges++;
            npcHp[i]=npc.getHitPoints();
        }
    }
    static void warmCombat() throws Exception {
        CombatFixtures.init(); org.dementhium.model.misc.GroundItemManager.load();
        world.getNpcDropLoader().load();
        combat("combat-8");
        for(int i=0;i<16;i++)world.run();
        BossEncounterCompletionRegression.clean();
        ((List<?>)BossEncounterCompletionRegression.field(world,World.class,"ticks")).clear();
        world.tickTasks.clear();world.updateTasks.clear();world.resetTasks.clear();
        combatNpcs.clear();combatPlayers.clear();
        moved=0;playerHpChanges=0;npcHpChanges=0;packets=0;projectiles=0;
    }

    static void observe() {
        while (!stopObserver) {
            int blocked = 0, alive = 0;
            synchronized (WorldTickProbe.class) {
                for (Executor executor : WorldTickProbe.executors) synchronized (executor.workers) {
                    for (Thread thread : executor.workers) {
                        if (thread.isAlive()) alive++;
                        if (thread.getState() == Thread.State.BLOCKED) blocked++;
                    }
                }
            }
            maxBlocked = Math.max(maxBlocked, blocked); maxWorkers = Math.max(maxWorkers, alive);
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(5));
        }
    }
    static double ms(long nanos) { return nanos / 1000000.0; }
    static String distribution(List<Long> values) {
        if (values.isEmpty()) return "n=0";
        Collections.sort(values);
        return String.format(java.util.Locale.ROOT, "n=%d p50=%.3f p95=%.3f max=%.3fms",
                values.size(), ms(values.get((values.size()-1)/2)),
                ms(values.get((int)Math.ceil(values.size()*.95)-1)), ms(values.get(values.size()-1)));
    }
    static void report(String scenario, Path output, long duration, int requests) throws Exception {
        List<Long> delay = new ArrayList<Long>(), execution = new ArrayList<Long>(), waiting = new ArrayList<Long>();
        Set<Long> owners = new HashSet<Long>();
        int returned = 0, replacements = 0, queue = 0, finished = 0, interruptedRows = 0, accepted=0;
        for (Executor executor : WorldTickProbe.executors) { returned += executor.returned; replacements += executor.replacements; }
        try (PrintWriter trace = new PrintWriter(Files.newBufferedWriter(output))) {
            trace.println("request,generation,queue,deadline_ns,submission_ns,worker_ns,entry_ns,completion_ns,tick,completed_tick,thread_id,interrupt_entry,interrupt_exit,discarded");
            for (Row row : WorldTickProbe.rows) {
                if(row.future!=null)accepted++;
                queue = Math.max(queue, row.queue);
                if (row.completion > 0) {
                    delay.add(row.entry-row.deadline); execution.add(row.completion-row.entry);
                    waiting.add(row.entry-row.workerStart); owners.add(row.threadId); finished++;
                    if (row.interruptAtEntry || row.interruptAtExit) interruptedRows++;
                    check(row.completedTick == row.tick + 1, "One logical increment per completed world update");
                }
                trace.printf(java.util.Locale.ROOT,"%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%s,%s,%s%n",
                        row.request,row.generation,row.queue,row.deadline,row.submission,row.workerStart,
                        row.entry,row.completion,row.tick,row.completedTick,row.threadId,
                        row.interruptAtEntry,row.interruptAtExit,row.discarded);
            }
        }
        System.out.println("TRIAL " + scenario + " requests=" + calls.get() + " accepted=" + accepted
                + " completed=" + finished + " logical=" + World.getTicks() + " durationMs=" + (long)ms(duration));
        System.out.println("deadlineDelay " + distribution(delay) + " execution " + distribution(execution)
                + " monitorWait " + distribution(waiting));
        System.out.println("backlog maxQueue=" + WorldTickProbe.maxQueue + " replacements=" + replacements + " returned=" + returned
                + " interruptRequests=" + WorldTickProbe.interruptRequests + " interruptedOperations=" + interruptedOperations
                + " observedInterrupt=" + observedInterrupt + " interruptedRows=" + interruptedRows
                + " executingThreads=" + owners + " maxWorkers=" + maxWorkers + " maxBlocked=" + maxBlocked
                + " maxProtected=" + WorldTickProbe.maxActive + " ownerFailures=" + WorldTickProbe.ownerFailures
                + " callbackViolations=" + violations.size());
        System.out.println("callbacks " + callbacks);
        if(scenario.startsWith("combat")) {
            int dead=0;for(NPC npc:combatNpcs)if(npc.isDead()||npc.destroyed())dead++;
            System.out.println("combat players="+combatPlayers.size()+" initialNpcs="+combatNpcs.size()+" deadOrRemoved="+dead
                    +" playerHpChanges="+playerHpChanges+" npcHpChanges="+npcHpChanges+" movementSteps="+moved
                    +" packets="+packets+" projectiles="+projectiles);
            check(packets>0,"Real player/NPC update packets generated");
            if(!scenario.equals("combat-nex"))check(npcHpChanges>0&&moved>0,"Real pursuit and player combat impacts");
            if(!scenario.contains("god"))check(playerHpChanges>0,"Real NPC damage impacts");
            if(scenario.equals("combat-nex"))check(projectiles>0,"Real encounter projectile callbacks");
        }
    }

    /** Negative control without any factory substitution or added World monitor wrapper. */
    static void nativePath(boolean baseline) throws Exception {
        final Set<Long> owners=Collections.synchronizedSet(new HashSet<Long>());
        final AtomicInteger aborted=new AtomicInteger();
        final CountDownLatch first=new CountDownLatch(1), unblock=new CountDownLatch(1);
        ManualServer server=new ManualServer();
        Method method=ServerThread.class.getDeclaredMethod("check");method.setAccessible(true);
        world.submit(new Tick(1) {
            public void execute() {
                check(org.dementhium.model.instance.InstanceManager.getSingleton().isCycleThread(),"Native owner");
                owners.add(Thread.currentThread().getId());
                if(World.getTicks()==0) {
                    first.countDown();
                    try { if(!unblock.await(5,TimeUnit.SECONDS))throw new AssertionError("Native gate timeout"); }
                    catch(InterruptedException ex){aborted.incrementAndGet();}
                }
            }
        });
        List<java.util.concurrent.ExecutorService> seen=new ArrayList<java.util.concurrent.ExecutorService>();
        try {
            for(int i=0;i<7;i++) {
                if(!seen.contains(ServerThread.service))seen.add(ServerThread.service);
                method.invoke(server);
                if(i==0)check(first.await(2,TimeUnit.SECONDS),"Native first entry");
                if(i==3)unblock.countDown();
            }
            check(World.getTicks()>=4,"Native world continues");
            System.out.println("NATIVE owners="+owners+" abortedOperations="+aborted+" generations="+seen.size());
            if(baseline)check(aborted.get()==1&&owners.size()>1,"Native baseline interruption and owner change reproduced");
            else check(aborted.get()==0&&owners.size()==1&&seen.size()==1,"Native stable backlog contract");
            System.out.println("PASS native loaded="+ServerThread.class.getProtectionDomain().getCodeSource().getLocation());
        } finally {
            unblock.countDown();
            if(!seen.contains(ServerThread.service))seen.add(ServerThread.service);
            for(java.util.concurrent.ExecutorService executor:seen)executor.shutdown();
            for(java.util.concurrent.ExecutorService executor:seen)check(executor.awaitTermination(3,TimeUnit.SECONDS),"Native cleanup");
            ServerThread.service2.shutdownNow();world.getBackgroundLoader().shutdownNow();
        }
    }

    public static void main(String[] args) throws Exception {
        final String scenario = args[0];
        final boolean baseline = args[1].equals("baseline");
        if(scenario.equals("native")){nativePath(baseline);return;}
        final int requests = scenario.startsWith("combat") ? 36 : scenario.equals("cpu") ? 14 : 9;
        // Class initialization creates the instrumented production service before any trial.
        check(ServerThread.service instanceof Executor, "Runner must load instrumented freshly compiled scheduler");
        if(scenario.startsWith("combat")){warmCombat();combat(scenario);}
        final ManualServer server = new ManualServer();
        final Method method = ServerThread.class.getDeclaredMethod("check"); method.setAccessible(true);
        install(scenario);
        final long epoch = System.nanoTime();
        Thread observer = new Thread(WorldTickSchedulingRegression::observe, "tick-probe-observer");
        Thread driver = new Thread(() -> {
            try {
                for (int i=0; i<requests && !stopDriver; i++) {
                    WorldTickProbe.request=i; WorldTickProbe.deadline=epoch+i*WorldTickProbe.PERIOD;
                    WorldTickProbe.submission=System.nanoTime(); calls.set(i+1);
                    method.invoke(server);
                }
            } catch (Throwable ex) { failure.set(ex); }
        }, "tick-probe-driver");
        try {
            observer.start(); driver.start();
            check(entered.await(5,TimeUnit.SECONDS), "First world tick entered");
            if (scenario.equals("slow")) {
                waitFor(() -> calls.get()>=2, "First queued tick");
                // Wait until the second request has actually been submitted (or coalesced).
                Thread.sleep(30); release.countDown();
            } else if (scenario.equals("interruptible") || scenario.equals("blocked")) {
                waitFor(() -> calls.get()>=6, "Sustained backlog"); release.countDown();
            } else if (scenario.equals("shutdown")) {
                waitFor(() -> calls.get()>=2, "Pending work at shutdown");
                Thread.sleep(30);
                // Explicit shutdown is tested separately from ordinary overload interruption.
                ServerThread.service.shutdown(); release.countDown();
            } else if (scenario.equals("async")) {
                check(asyncEntered.await(3,TimeUnit.SECONDS),"Generic World.submitTask entered service2");
                waitFor(() -> ((java.util.concurrent.ThreadPoolExecutor)ServerThread.service).getCompletedTaskCount()>0,"World finishes before generic async task");
                check(asyncActive.get()==1&&asyncOwnerFailures.get()==0,"Generic async task outlives World.run without owning its cycle");
                asyncRelease.countDown();
            }
            driver.join(requests * 600L + 6000L);
            check(!driver.isAlive(), "Bounded scheduler driver");
            if (!scenario.equals("shutdown")) check(failure.get()==null, "Scheduler continued after task failure");
            else check(failure.get()!=null&&failure.get().getCause() instanceof java.util.concurrent.RejectedExecutionException,
                    "Explicit executor shutdown retains rejection contract");
            waitFor(() -> {
                for (Executor e : WorldTickProbe.executors) if (e.getActiveCount()!=0 || !e.getQueue().isEmpty()) return false;
                return true;
            }, "Backlog recovery");
            stopObserver=true; observer.join(1000);
            // Futures establish visibility before reading completed trace/callback fields.
            for(Row row:WorldTickProbe.rows)if(row.future!=null&&!row.discarded)row.future.get(3,TimeUnit.SECONDS);
            report(scenario,Paths.get(args[2]),System.nanoTime()-epoch,requests);
            check(WorldTickProbe.maxActive==1, "No protected overlap");
            check(WorldTickProbe.ownerFailures==0, "Authoritative owner matches actual executing thread");
            check(violations.isEmpty(), "Callbacks honor logical deadlines: " + violations);
            check(!callbacks.contains("cancelled@1"), "Cancelled callback never executed");
            check(Collections.frequency(callbacks,"before@1")==1, "Pre-stall callback executes once");
            check(Collections.frequency(callbacks,"child@1")==1, "Nested callback executes once next cycle");
            check(Collections.frequency(callbacks,"during@1")==1, "During-stall callback retained");
            check(callbacks.indexOf("before@1")<callbacks.indexOf("child@1"), "Existing world list precedes next-cycle additions");
            if (!baseline) {
                for (Executor e : WorldTickProbe.executors) check(e.replacements==0, "No overload shutdownNow");
                check(WorldTickProbe.executors.size()==1, "Stable executor generation");
                check(WorldTickProbe.maxQueue<=1, "At most one pending world tick under sustained backlog");
                check(interruptedOperations.get()==0 && observedInterrupt.get()==0, "Backlog does not interrupt work");
                check(maxWorkers==1, "No accumulating workers");
            } else if (scenario.equals("interruptible")) {
                check(interruptedOperations.get()==1, "Baseline aborts the interruptible operation");
                check(!callbacks.contains("operation-completed@0"), "Aborted operation did not reach its completion");
            } else if (scenario.equals("blocked")) {
                check(maxWorkers>=3 && maxBlocked>=2, "Baseline accumulates live monitor waiters");
            }
            System.out.println("PASS " + checks + " checks; loaded=" + ServerThread.class.getProtectionDomain().getCodeSource().getLocation());
        } finally {
            release.countDown(); asyncRelease.countDown(); stopObserver=true; stopDriver=true;
            if (driver.isAlive()) driver.interrupt();
            driver.join(2000); observer.join(1000);
            for (Executor e : WorldTickProbe.executors) e.cleanup();
            ServerThread.service2.shutdownNow(); world.getBackgroundLoader().shutdownNow();
            for (Executor e : WorldTickProbe.executors) check(e.awaitTermination(3,TimeUnit.SECONDS), "Probe worker terminated");
            check(!driver.isAlive()&&!observer.isAlive(), "Probe threads terminated");
        }
    }
}
