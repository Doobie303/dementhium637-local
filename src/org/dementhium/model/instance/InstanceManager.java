package org.dementhium.model.instance;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import org.dementhium.model.*;
import org.dementhium.model.map.region.*;
import org.dementhium.model.player.Player;
import org.dementhium.tickable.Tick;

/** Runtime sessions. World.run owns mutation; other threads enqueue bounded requests. */
public final class InstanceManager {
    private static final InstanceManager SINGLETON = new InstanceManager(tick -> World.getWorld().submit(tick), InstanceLimits.load(), System::nanoTime);
    private static final Map<Long, GameInstance> MAPS = new ConcurrentHashMap<Long, GameInstance>();
    private final Map<Long, GameInstance> instances = new LinkedHashMap<Long, GameInstance>();
    private final Map<Player, GameInstance> members = new IdentityHashMap<Player, GameInstance>();
    private final Map<String, Player> accounts = new LinkedHashMap<String, Player>();
    private final ArrayDeque<Runnable> requests = new ArrayDeque<Runnable>();
    private final LinkedHashMap<Player, Pending> logins = new LinkedHashMap<Player, Pending>();
    private final LinkedHashMap<Player, Pending> logouts = new LinkedHashMap<Player, Pending>();
    private final Map<String, Admission> admissions = new LinkedHashMap<String, Admission>();
    private final Consumer<Tick> scheduler;
    private final InstanceLimits limits;
    private final LongSupplier clock;
    private volatile Thread cycleThread;
    private volatile boolean shuttingDown;
    private int createsThisCycle, admitsThisCycle, buildsThisCycle;
    private long cycleStarted, cycles, rejected, created, closed, expired, overflowDrops;
    private final long[] cycleNanos = new long[512];
    private final ArrayDeque<String> recentClosures = new ArrayDeque<String>();
    private static final class Pending {
        final CompletableFuture<Void> result = new CompletableFuture<Void>();
        final Runnable action;
        Pending(Runnable action) { this.action = action; }
    }
    private static final class Admission {
        long since; int count;
        Admission(long since) { this.since = since; }
    }
    public InstanceManager(Consumer<Tick> scheduler) { this(scheduler, InstanceLimits.defaults(), System::nanoTime); }
    public InstanceManager(Consumer<Tick> scheduler, InstanceLimits limits, LongSupplier clock) {
        if (scheduler == null || limits == null || clock == null) throw new IllegalArgumentException("Missing instance dependency");
        this.scheduler = scheduler; this.limits = limits; this.clock = clock;
    }
    public static InstanceManager getSingleton() { return SINGLETON; }
    public InstanceLimits getLimits() { return limits; }
    long now() { return clock.getAsLong(); }
    public boolean isCycleThread() { return Thread.currentThread() == cycleThread; }
    public boolean isShuttingDown() { return shuttingDown; }
    public synchronized void beginCycle() {
        if (cycleThread != null) throw new IllegalStateException("Overlapping instance mutation cycles");
        cycleThread = Thread.currentThread(); cycleStarted = now();
        createsThisCycle = admitsThisCycle = buildsThisCycle = 0;
    }
    public synchronized void endCycle() {
        checkThread(); cycleNanos[(int)(cycles++ % cycleNanos.length)] = Math.max(0, now() - cycleStarted); cycleThread = null;
    }
    public void checkThread() {
        if (!isCycleThread()) throw new IllegalStateException("Instance mutation requires the world tick; use submit");
    }
    private synchronized IllegalStateException reject(String reason) {
        rejected++; return new IllegalStateException("Instance capacity: " + reason);
    }
    public synchronized <T> CompletableFuture<T> submit(final Callable<T> action) {
        if (action == null) throw new IllegalArgumentException("Missing action");
        CompletableFuture<T> result = new CompletableFuture<T>();
        if (shuttingDown || requests.size() >= limits.get("requests")) {
            result.completeExceptionally(reject(shuttingDown ? "server shutting down" : "request queue full")); return result;
        }
        requests.add(() -> {
            if (result.isCancelled()) return;
            if (shuttingDown) { result.completeExceptionally(new IllegalStateException("Server is shutting down")); return; }
            try { result.complete(action.call()); } catch (Exception e) { result.completeExceptionally(e); }
        });
        return result;
    }
    /** Separate bounded, coalescing lifecycle lanes. Logout cannot be starved by logins or instance creation.
     * A rejected lifecycle submission must disconnect the connection; World retries registered disconnects. */
    public synchronized CompletableFuture<Void> submitLifecycle(Player player, boolean logout, Runnable action) {
        if (player == null || action == null) throw new IllegalArgumentException("Missing lifecycle request");
        LinkedHashMap<Player, Pending> lane = logout ? logouts : logins;
        Pending existing = lane.get(player);
        if (existing != null) return existing.result;
        Pending pending = new Pending(action);
        // Two world/lobby lists each hold at most 2048 players. Fixed separate logout reserve.
        if (shuttingDown || lane.size() >= (logout ? 4096 : limits.get("loginRequests"))) {
            pending.result.completeExceptionally(reject("lifecycle queue full or shutdown")); return pending.result;
        }
        lane.put(player, pending); return pending.result;
    }
    private synchronized void drainLifecycle(LinkedHashMap<Player, Pending> lane, int budget) {
        int count = Math.min(budget, lane.size());
        for (int i = 0; i < count && !lane.isEmpty(); i++) {
            Iterator<Pending> it = lane.values().iterator(); Pending pending = it.next(); it.remove();
            if (pending.result.isCancelled()) continue;
            try {
                if (shuttingDown) throw new IllegalStateException("Server is shutting down");
                pending.action.run(); pending.result.complete(null);
            } catch (Exception e) { pending.result.completeExceptionally(e); }
        }
    }
    public synchronized void drainRequests() {
        checkThread();
        drainLifecycle(logouts, limits.get("lifecyclePerCycle"));
        drainLifecycle(logins, limits.get("lifecyclePerCycle"));
        int count = Math.min(limits.get("requestsPerCycle"), requests.size());
        for (int i = 0; i < count; i++) { Runnable action = requests.poll(); if (action != null) action.run(); }
    }
    public synchronized boolean shutdown() {
        checkThread(); shuttingDown = true;
        while (!requests.isEmpty() || !logins.isEmpty() || !logouts.isEmpty()) drainRequests();
        return closeAll();
    }
    public void maintain() {
        checkThread(); pruneAdmissions();
        for (GameInstance instance : new ArrayList<GameInstance>(instances.values())) {
            for (Player player : instance.getMembers()) {
                try {
                    if (!player.isOnline() || player.destroyed() || player.getConnection().isDisconnected()) InstanceAccess.depart(player, true);
                } catch (RuntimeException failure) { instance.close("disconnected member cleanup"); }
            }
            if (instance.getState() == GameInstance.State.CLOSING) instance.close();
            else if (instance.isActive() && (instance.getMemberCount() == 0
                    ? instance.getEmptySeconds() >= limits.get("emptySeconds")
                    : limits.get("idleSeconds") > 0 && instance.getIdleSeconds() >= limits.get("idleSeconds"))) {
                expired++; instance.close(instance.getMemberCount() == 0 ? "empty timeout" : "idle timeout");
            }
        }
    }
    void schedule(Tick task) { checkThread(); scheduler.accept(task); }
    public GameInstance create(int widthChunks, int heightChunks, int capacity, Location exit, Consumer<GameInstance> initializer) {
        checkThread();
        if (shuttingDown) throw new IllegalStateException("Server is shutting down");
        if (widthChunks < 1 || heightChunks < 1 || widthChunks > 512 || heightChunks > 512 || capacity < 1 || initializer == null)
            throw new IllegalArgumentException("Invalid instance configuration");
        validateReturn(exit);
        long footprint = (long)widthChunks * heightChunks * 4;
        long regions = ((widthChunks + 15) / 16 * 2 + 4) * ((heightChunks + 15) / 16 * 2 + 4);
        if (instances.size() >= limits.get("sessions") || capacity > limits.get("membersPerSession")
                || footprint > limits.get("chunkPlanesPerSession") || total("chunkPlanes") + footprint > limits.get("chunkPlanes")
                || total("reservedRegions") + regions > limits.get("reservedRegions") || createsThisCycle >= limits.get("createsPerCycle"))
            throw reject("session, reservation or creation rate limit");
        createsThisCycle++;
        MapAllocation allocation = RegionBuilder.reserveMap(widthChunks, heightChunks);
        if (allocation == null) { reject("allocator exhausted"); return null; }
        GameInstance instance = new GameInstance(this, allocation, capacity, exit);
        instances.put(instance.getId(), instance); MAPS.put(allocation.getId(), instance); created++;
        try { initializer.accept(instance); instance.activate(); return instance; }
        catch (RuntimeException failure) {
            if (!instance.close("initializer failed")) failure.addSuppressed(new IllegalStateException("Instance " + instance.getId() + " remains closing: " + instance.getCloseFailures()));
            throw failure;
        }
    }
    public long total(String resource) {
        checkThread(); long total = 0;
        for (GameInstance instance : instances.values()) total += instance.resourceCount(resource);
        return total;
    }
    boolean hasRoom(GameInstance instance, String resource) {
        checkThread();
        return instance.resourceCount(resource) < limits.get(resource + "PerSession") && total(resource) < limits.get(resource);
    }
    void requireRoom(GameInstance instance, String resource) { if (!hasRoom(instance, resource)) throw reject(resource + " limit"); }
    void chargeBuild(int width, int height, int[] planes) {
        checkThread();
        if (width < 1 || height < 1 || planes == null || planes.length < 1 || planes.length > 4) throw new IllegalArgumentException("Invalid build size");
        long chunks = (long)width * height * planes.length;
        if (chunks + buildsThisCycle > limits.get("buildChunksPerCycle")) throw reject("map build rate limit");
        buildsThisCycle += (int)chunks;
    }
    public GameInstance get(long id) { checkThread(); return instances.get(id); }
    public List<GameInstance> getInstances() { checkThread(); return Collections.unmodifiableList(new ArrayList<GameInstance>(instances.values())); }
    public GameInstance getInstance(Player player) { checkThread(); return members.get(player); }
    public static GameInstance at(Location location) {
        if (location == null) return null;
        MapAllocation map = RegionBuilder.getAllocation(location.getX(), location.getY());
        return map == null ? null : MAPS.get(map.getId());
    }
    private void pruneAdmissions() {
        long window = limits.get("admissionWindowSeconds") * 1000000000L;
        admissions.values().removeIf(value -> now() - value.since >= window);
    }
    void claim(GameInstance instance, Player player) {
        checkThread();
        if (members.containsKey(player) || accounts.containsKey(account(player))) throw new IllegalStateException("Player/account already belongs to an instance");
        pruneAdmissions();
        Admission record = admissions.get(account(player));
        if (members.size() >= limits.get("members") || admitsThisCycle >= limits.get("admissionsPerCycle")
                || (record != null && record.count >= limits.get("admissionsPerAccount"))
                || (record == null && admissions.size() >= limits.get("admissionAccounts"))) throw reject("admission limit; try later");
        if (record == null) { record = new Admission(now()); admissions.put(account(player), record); }
        record.count++; admitsThisCycle++;
        members.put(player, instance); accounts.put(account(player), player);
    }
    void unclaim(GameInstance instance, Player player) {
        if (members.get(player) == instance) { members.remove(player); accounts.remove(account(player), player); }
    }
    private static String account(Player p) { return p.getUsername().toLowerCase(Locale.ROOT); }
    static void validateReturn(Location exit) {
        if (exit == null || exit.getX() <= 0 || exit.getX() >= 16384 || exit.getY() <= 0 || exit.getY() >= 16384
                || exit.getZ() < 0 || exit.getZ() > 3 || RegionBuilder.isUnmappedInstanceSpace(exit) || RegionBuilder.getDynamicRegion(exit.getX(), exit.getY()) != null)
            throw new IllegalArgumentException("Return location must be in the ordinary world");
    }
    void overflowDrop() { checkThread(); overflowDrops++; }
    void closed(GameInstance instance) {
        instances.remove(instance.getId(), instance); MAPS.remove(instance.getId(), instance); closed++;
        if (recentClosures.size() == 32) recentClosures.removeFirst();
        recentClosures.addLast("#" + instance.getId() + " " + instance.getActivity() + ": " + instance.getCloseReason());
    }
    public List<String> recentClosures() { checkThread(); return new ArrayList<String>(recentClosures); }
    public synchronized List<String> diagnostics() {
        checkThread(); List<String> lines = new ArrayList<String>();
        lines.add("Instances " + instances.size() + "/" + limits.get("sessions") + ", members " + members.size() + "/" + limits.get("members")
                + ", queues requests/login/logout=" + requests.size() + "/" + logins.size() + "/" + logouts.size());
        for (String resource : new String[]{"chunkPlanes", "reservedRegions", "npcs", "objects", "drops", "tasks"})
            lines.add(resource + " " + total(resource) + "/" + limits.get(resource));
        int built = 0; for (GameInstance instance : instances.values()) built += instance.getBuiltChunkPlanes();
        lines.add("Built chunk-planes=" + built + "; reservation budget includes all four planes, including unbuilt space.");
        lines.add("Created=" + created + " closed=" + closed + " expired=" + expired + " rejected=" + rejected + " overflowDrops=" + overflowDrops);
        long[] samples = Arrays.copyOf(cycleNanos, (int)Math.min(cycles, cycleNanos.length)); Arrays.sort(samples);
        lines.add("World cycle samples=" + samples.length + " p95/max ms=" + (samples.length == 0 ? "n/a" :
            String.format(Locale.ROOT, "%.2f/%.2f", samples[(int)Math.ceil(samples.length * .95) - 1] / 1e6, samples[samples.length - 1] / 1e6)));
        Runtime runtime = Runtime.getRuntime();
        lines.add("JVM used/max MiB=" + ((runtime.totalMemory() - runtime.freeMemory()) / 1048576) + "/" + (runtime.maxMemory() / 1048576)
            + "; allocator maps/regions=" + RegionBuilder.getAllocationCount() + "/" + RegionBuilder.getDynamicRegionCount());
        return lines;
    }
    public boolean canInteract(Player player, Entity target) { checkThread(); return InstanceAccess.canInteract(player, target); }
    public boolean closeAll() {
        checkThread(); boolean complete = true;
        for (GameInstance instance : new ArrayList<GameInstance>(instances.values())) complete &= instance.close("manager close all");
        return complete;
    }
}
