package org.dementhium.model.instance;

import java.util.*;
import java.util.function.Consumer;
import org.dementhium.cache.format.CacheObjectDefinition;
import org.dementhium.model.*;
import org.dementhium.model.map.*;
import org.dementhium.model.map.region.*;
import org.dementhium.model.misc.*;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.player.Player;
import org.dementhium.tickable.Tick;

/** An ephemeral, world-cycle-owned session. Content must use these resource APIs. */
public final class GameInstance {
    public enum State { BUILDING, ACTIVE, CLOSING, CLOSED }
    public enum DeathPolicy { SAFE_RETURN, STANDARD_AT_EXIT, RESPAWN_INSIDE }
    private java.util.function.Function<Player, Location> respawnLocation;
    private Consumer<Player> respawnHandler = player -> {};
    /** An explicit content policy; callback runs once after successful internal recovery. */
    public void setRespawnPolicy(java.util.function.Function<Player, Location> location, Consumer<Player> recovered) {
        manager.checkThread();
        if (state != State.BUILDING || location == null || recovered == null)
            throw new IllegalStateException("Choose respawn policy during creation");
        respawnLocation = location; respawnHandler = recovered; deathPolicy = DeathPolicy.RESPAWN_INSIDE;
    }
    Location respawnLocation(Player player) {
        manager.checkThread();
        Location target = respawnLocation.apply(player);
        if (!isActive() || !isMember(player) || !canOccupy(target, 1))
            throw new IllegalStateException("Invalid internal respawn location");
        return target;
    }
    void respawned(Player player) { manager.checkThread(); respawnHandler.accept(player); }
    private volatile DeathPolicy deathPolicy = DeathPolicy.SAFE_RETURN;
    public InstanceManager getManager() { return manager; }
    DeathPolicy getDeathPolicyForSave() { return deathPolicy; }
    public DeathPolicy getDeathPolicy() { manager.checkThread(); return deathPolicy; }
    public void setDeathPolicy(DeathPolicy policy) {
        manager.checkThread();
        if (state!=State.BUILDING || policy==null) throw new IllegalStateException("Choose death policy during creation");
        deathPolicy=policy;
        if (policy != DeathPolicy.RESPAWN_INSIDE) { respawnLocation = null; respawnHandler = player -> {}; }
    }
    private final InstanceManager manager;
    private final MapAllocation map;
    private final int capacity;
    private final Location exit;
    private InstanceTemplate template;
    void attachTemplate(InstanceTemplate value) {
        manager.checkThread();
        if (state != State.BUILDING || template != null || value == null)
            throw new IllegalStateException("Choose a template once during creation");
        template = value; setActivity(value.getId());
    }
    public InstanceTemplate getTemplate() { manager.checkThread(); return template; }
    public Location location(String anchor) {
        manager.checkThread();
        if (template == null) throw new IllegalStateException("Instance has no named template");
        InstanceTemplate.Tile tile = template.getAnchor(anchor);
        return location(tile.x, tile.y, tile.plane);
    }
    public InstanceTemplate.Tile toLocal(Location location) {
        manager.checkThread();
        if (!contains(location)) throw new IllegalArgumentException("Tile is outside the built instance");
        return new InstanceTemplate.Tile(location.getX() - map.getX(), location.getY() - map.getY(), location.getZ());
    }
    private State state = State.BUILDING;
    private boolean closingNow;
    private String activity = "custom";
    private final long createdAt;
    private long lastActivity, emptySince, closingSince;
    private int closeAttempts;
    private String closeReason = "content close";
    public void setActivity(String value) {
        manager.checkThread();
        if (state != State.BUILDING || value == null || !value.matches("[A-Za-z0-9_.-]{1,64}"))
            throw new IllegalArgumentException("Choose a short activity ID during creation");
        activity = value;
    }
    public String getActivity() { manager.checkThread(); return activity; }
    public void touch() { manager.checkThread(); if (state == State.ACTIVE) lastActivity = manager.now(); }
    public long getAgeSeconds() { manager.checkThread(); return Math.max(0, manager.now() - createdAt) / 1000000000L; }
    public long getIdleSeconds() { manager.checkThread(); return Math.max(0, manager.now() - lastActivity) / 1000000000L; }
    public long getEmptySeconds() { manager.checkThread(); return members.isEmpty() ? Math.max(0, manager.now() - emptySince) / 1000000000L : 0; }
    public String getCloseReason() { manager.checkThread(); return closeReason; }
    public int getObjectCount() { manager.checkThread(); return objects.size(); }
    public int getDropCount() { manager.checkThread(); return drops.size(); }
    long resourceCount(String resource) {
        manager.checkThread();
        switch (resource) {
            case "chunkPlanes": return (long)map.getWidthChunks() * map.getHeightChunks() * 4;
            case "reservedRegions": return ((map.getWidthChunks() + 15) / 16 * 2 + 4) * ((map.getHeightChunks() + 15) / 16 * 2 + 4);
            case "members": return members.size();
            case "npcs": return npcs.size();
            case "objects": return objects.size();
            case "drops": return drops.size();
            case "tasks": return tasks.size();
            default: throw new IllegalArgumentException("Unknown resource: " + resource);
        }
    }
    public int getBuiltChunkPlanes() {
        manager.checkThread(); int count = 0;
        if (state == State.CLOSED) return 0;
        for (int x = 0; x < map.getWidthChunks(); x++) for (int y = 0; y < map.getHeightChunks(); y++) {
            int tileX = map.getX() + x * 8, tileY = map.getY() + y * 8;
            DynamicRegion region = RegionBuilder.getDynamicRegion(tileX, tileY);
            if (region != null) for (int plane = 0; plane < 4; plane++)
                if (region.getChunkRevision(plane, tileX & 63, tileY & 63) != 0) count++;
        }
        return count;
    }
    public String describe() {
        manager.checkThread();
        return "#" + getId() + " " + activity + " " + state + " members=" + members.size() + "/" + capacity
            + " npc/object/drop/task=" + npcs.size() + "/" + objects.size() + "/" + drops.size() + "/" + tasks.size()
            + " age/idle=" + getAgeSeconds() + "/" + getIdleSeconds() + "s";
    }
    public List<String> inspect() {
        manager.checkThread(); List<String> lines = new ArrayList<String>(); lines.add(describe());
        lines.add("Map=" + map.getX() + "," + map.getY() + " size=" + map.getWidthChunks() + "x" + map.getHeightChunks()
            + " chunks; built chunk-planes=" + getBuiltChunkPlanes() + "; reserved chunk-planes/regions=" + resourceCount("chunkPlanes") + "/" + resourceCount("reservedRegions"));
        lines.add("Exit=" + exit + "; death=" + deathPolicy + "; empty=" + getEmptySeconds() + "s");
        if (party != null) lines.add("Party leader=" + (party.getLeader() == null ? "none" : party.getLeader().getUsername()));
        for (Player player : members.keySet()) lines.add("Member " + player.getUsername() + " at " + player.getLocation() + " return=" + members.get(player));
        lines.add("Close reason=" + closeReason + " attempts=" + closeAttempts + " closingAge="
            + (state == State.CLOSING ? Math.max(0, manager.now() - closingSince) / 1000000000L : 0) + "s");
        lines.addAll(closeFailures); return lines;
    }
    public boolean canTrackDrop(GroundItem drop) {
        writable();
        if (drop == null || !contains(drop.getLocation())) throw new IllegalArgumentException("Drop outside built instance");
        return drops.contains(drop) || manager.hasRoom(this, "drops");
    }
    /** Overflow remains ordinary ground loot, preserving item/owner/visibility at a safe return tile. */
    public Location overflowDropLocation(Player player) {
        manager.checkThread(); manager.overflowDrop();
        Location target = members.get(player); return target == null ? exit : target;
    }
    private InstanceParty party;
    public InstanceParty createParty(Player leader, InstanceParty.LeaderDeparture policy) {
        manager.checkThread();
        if (state != State.BUILDING || party != null) throw new IllegalStateException("Choose party during creation");
        party = new InstanceParty(this, leader, policy);
        return party;
    }
    public InstanceParty getParty() { manager.checkThread(); return party; }
    private Consumer<Player> departureHandler = player -> {};
    /** Runs once after evacuation/death resolution and unbinding, including close/logout.
     * Content must handle its own idempotent rewards and must not read old membership here. */
    public void setDepartureHandler(Consumer<Player> handler) {
        manager.checkThread();
        if (state != State.BUILDING || handler == null)
            throw new IllegalStateException("Choose departure handler during creation");
        departureHandler = handler;
    }
    private final Map<Player, Location> members = new IdentityHashMap<Player, Location>();
    private final Set<NPC> npcs = identitySet();
    private final Set<GameObject> objects = identitySet();
    private final Set<GroundItem> drops = identitySet();
    private final Set<OwnedTask> tasks = identitySet();
    private final List<String> closeFailures = new ArrayList<String>();

    private static <T> Set<T> identitySet() {
        return Collections.newSetFromMap(new IdentityHashMap<T, Boolean>());
    }
    GameInstance(InstanceManager manager, MapAllocation map, int capacity, Location exit) {
        this.manager = manager; this.map = map; this.capacity = capacity; this.exit = exit;
        createdAt = lastActivity = emptySince = manager.now();
    }
    public long getId() { return map.getId(); }
    public State getState() { manager.checkThread(); return state; }
    public boolean isActive() { manager.checkThread(); return state == State.ACTIVE; }
    public int getCapacity() { return capacity; }
    public int getMemberCount() { manager.checkThread(); return members.size(); }
    public int getNpcCount() { manager.checkThread(); return npcs.size(); }
    public int getTaskCount() { manager.checkThread(); return tasks.size(); }
    public List<Player> getMembers() { manager.checkThread(); return Collections.unmodifiableList(new ArrayList<Player>(members.keySet())); }
    public List<String> getCloseFailures() { manager.checkThread(); return Collections.unmodifiableList(new ArrayList<String>(closeFailures)); }
    public boolean isMember(Player player) { manager.checkThread(); return members.containsKey(player); }
    public boolean owns(NPC npc) { manager.checkThread(); return npcs.contains(npc); }
    private void writable() {
        manager.checkThread();
        if (state != State.BUILDING && state != State.ACTIVE) throw new IllegalStateException("Instance is closing/closed");
    }
    void activate() {
        manager.checkThread();
        if (state != State.BUILDING) throw new IllegalStateException("Instance was closed during creation");
        if (deathPolicy == DeathPolicy.RESPAWN_INSIDE && respawnLocation == null)
            throw new IllegalStateException("Internal respawning requires a location policy");
        state = State.ACTIVE;
    }
    /** Only the initializer may alter the template, before resources or members exist. */
    public void copyMap(int sourceX, int sourceY, int offsetX, int offsetY, int width, int height,
            int[] sourcePlanes, int[] destinationPlanes) {
        manager.checkThread();
        if (state != State.BUILDING || !npcs.isEmpty() || !objects.isEmpty() || !drops.isEmpty())
            throw new IllegalStateException("Copy maps before populating the instance");
        manager.chargeBuild(width, height, destinationPlanes);
        RegionBuilder.copyMap(map, sourceX, sourceY, offsetX, offsetY, width, height, sourcePlanes, destinationPlanes);
    }
    public Location location(int localX, int localY, int plane) {
        manager.checkThread();
        if (state == State.CLOSED || localX < 0 || localY < 0 || localX >= map.getWidthChunks() * 8
                || localY >= map.getHeightChunks() * 8 || plane < 0 || plane > 3)
            throw new IllegalArgumentException("Invalid local instance coordinate");
        return Location.locate(map.getX() + localX, map.getY() + localY, plane);
    }
    /** Adds a fully validated rectangle to unused chunks. Existing maps/resources stay intact on failure.
     * Populate using owned resource APIs after success; close the session if content population fails. */
    public void buildRoom(int sourceX, int sourceY, int offsetX, int offsetY, int width, int height,
            int[] sourcePlanes, int[] destinationPlanes) {
        manager.checkThread();
        if (state != State.ACTIVE) throw new IllegalStateException("Room expansion requires an active instance");
        manager.chargeBuild(width, height, destinationPlanes);
        RegionBuilder.appendMap(map, sourceX, sourceY, offsetX, offsetY, width, height, sourcePlanes, destinationPlanes);
        // PlayerUpdate observes the changed scene revision, including stationary members.
    }
    public boolean contains(Location location) {
        manager.checkThread();
        if (state == State.CLOSED || location == null || !map.contains(location.getX(), location.getY())
                || location.getZ() < 0 || location.getZ() > 3) return false;
        DynamicRegion region = RegionBuilder.getDynamicRegion(location.getX(), location.getY());
        return region != null && region.getChunkRevision(location.getZ(), location.getX() & 63, location.getY() & 63) != 0;
    }
    public boolean canOccupy(Location location, int size) {
        manager.checkThread();
        if (size < 1 || size > Math.min(map.getWidthChunks(), map.getHeightChunks()) * 8 || !contains(location)) return false;
        for (int x = 0; x < size; x++) for (int y = 0; y < size; y++) {
            Location tile = Location.locate(location.getX() + x, location.getY() + y, location.getZ());
            if (!contains(tile) || (Region.getClippingMask(tile.getX(), tile.getY(), tile.getZ()) & 0x200100) != 0) return false;
        }
        return true;
    }
    public void enter(Player player, Location spawn) { enter(player, spawn, exit); }
    public void enter(Player player, Location spawn, Location returnTo) {
        manager.checkThread();
        if (party != null && !party.isAdmitting(player)) throw new IllegalStateException("Use party admission");
        if (state != State.ACTIVE || player == null || !player.isOnline() || player.getFamiliar() != null
                || player.isDead() || player.getConnection().isDisconnected() || player.getConnection().isInLobby()
                || InstanceAccess.owner(player)!=null || members.size() >= capacity || !canOccupy(spawn, 1)
                || (player.getActivity() != null && player.getActivity().isRunning())
                || InstanceManager.at(player.getLocation()) != null)
            throw new IllegalStateException("Player cannot enter this instance");
        InstanceManager.validateReturn(returnTo);
        manager.claim(this, player);
        members.put(player, returnTo);
        InstanceAccess.bind(this,player,returnTo);
        InstanceAccess.transfer(player,true);
        try {
            player.getCombatExecutor().reset();
            player.teleport(spawn, false);
            if (!spawn.equals(player.getLocation())) throw new IllegalStateException("Entry teleport failed");
            touch();
        } catch (RuntimeException failure) {
            // A partially completed teleport remains owned until evacuation succeeds.
            try { leave(player); } catch (RuntimeException cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        } finally { InstanceAccess.transfer(player,false); }
    }
    public boolean leave(Player player) { return leave(player,false); }
    public boolean leaveDisconnected(Player player) { return leave(player,true); }
    private boolean leave(Player player, boolean disconnected) {
        manager.checkThread();
        Location returnTo = members.get(player);
        if (returnTo == null) return false;
        InstanceAccess.transfer(player,true);
        try {
            player.getCombatExecutor().reset(); player.getWalkingQueue().reset();
            player.getActionManager().stopAction();
            player.removeTick("following_mob"); player.removeTick("teleport_tick");
            player.closeAll(true,false);
            if (InstanceManager.at(player.getLocation()) == this) {
                InstanceManager.validateReturn(returnTo);
                if (player.isOnline() && !disconnected && !player.getConnection().isDisconnected()) player.teleport(returnTo, false);
                else player.setLocation(returnTo);
                if (!returnTo.equals(player.getLocation())) throw new IllegalStateException("Evacuation teleport failed");
            }
            InstanceAccess.finishDeath(player);
            members.remove(player); manager.unclaim(this, player); InstanceAccess.unbind(this,player);
            if (members.isEmpty()) emptySince = manager.now();
        } finally { InstanceAccess.transfer(player,false); }
        Consumer<Player> departed = departureHandler;
        try {
            // Party policy may recursively close the other members and clear the stored handler.
            try { if (party != null) party.departed(player); }
            finally { departed.accept(player); }
        }
        catch (RuntimeException failure) {
            // Membership has already been released; a failed content callback must
            // not strand an empty active allocation outside maintain()'s retry path.
            close();
            throw failure;
        }
        return true;
    }
    /** Claims a fresh NPC, never an existing world NPC or familiar. */
    public NPC spawnNpc(NPC npc, Location spawn) {
        writable();
        if (npc == null || npc.isFamiliar() || npc.getOwningInstance() != null
                || World.getWorld().getNpcs().get(npc.getIndex()) == npc || !canOccupy(spawn, npc.size()))
            throw new IllegalArgumentException("Invalid NPC or spawn");
        manager.requireRoom(this, "npcs");
        npcs.add(npc);
        npc.attachInstance(this);
        try {
            npc.setUnrespawnable(true);
            npc.setOriginalLocation(spawn);
            npc.setLocation(spawn);
            if (!World.getWorld().getNpcs().add(npc)) throw new IllegalStateException("World NPC capacity exhausted");
            return npc;
        } catch (RuntimeException failure) {
            try { removeNpc(npc); } catch (RuntimeException cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
    }
    public boolean removeNpc(NPC npc) {
        manager.checkThread();
        if (!npcs.contains(npc)) return false;
        cancelNpcTasks(npc);
        npc.setUnrespawnable(true); npc.setHidden(true);
        npc.getCombatExecutor().reset(); npc.getWalkingQueue().reset(); npc.cancelTicks();
        if (World.getWorld().getNpcs().get(npc.getIndex()) == npc) World.getWorld().getNpcs().remove(npc);
        else npc.destroy();
        npcs.remove(npc);
        return true;
    }
    /** Called by ordinary world removal too, retaining the NPC's owner tombstone. */
    public void onNpcRemoved(NPC npc) {
        manager.checkThread();
        cancelNpcTasks(npc); npcs.remove(npc);
    }
    private void cancelNpcTasks(NPC npc) {
        for (OwnedTask task : new ArrayList<OwnedTask>(tasks)) if (task.npc == npc) task.stop();
    }
    public GameObject spawnObject(int id, Location tile, int type, int rotation) {
        writable();
        if (!contains(tile) || type < 0 || type > 22 || rotation < 0 || rotation > 3)
            throw new IllegalArgumentException("Invalid instance object");
        CacheObjectDefinition definition = CacheObjectDefinition.forId(id);
        int width = (rotation & 1) == 0 ? definition.getSizeX() : definition.getSizeY();
        int height = (rotation & 1) == 0 ? definition.getSizeY() : definition.getSizeX();
        // Include wall-neighbour clipping; no object can touch an unbuilt/foreign tile.
        int margin = type <= 3 ? 1 : 0;
        for (int x = -margin; x < width + margin; x++) for (int y = -margin; y < height + margin; y++)
            if (!contains(Location.locate(tile.getX() + x, tile.getY() + y, tile.getZ())))
                throw new IllegalArgumentException("Object footprint leaves the built map");
        manager.requireRoom(this, "objects");
        GameObject object = ObjectManager.addCustomObject(id, tile.getX(), tile.getY(), tile.getZ(), type, rotation, false);
        if (object == null) throw new IllegalStateException("Object placement failed");
        objects.add(object);
        ObjectManager.refresh(object);
        return object;
    }
    public boolean removeObject(GameObject object) {
        manager.checkThread();
        if (!objects.contains(object)) return false;
        ObjectManager.discardCustomObject(object);
        objects.remove(object);
        return true;
    }
    public void spawnDrop(GroundItem drop) {
        writable();
        if (drop == null || !contains(drop.getLocation())) throw new IllegalArgumentException("Invalid instance drop");
        GroundItemManager.createGroundItem(drop);
    }
    /** GroundItemManager calls this before publishing any drop, including ordinary NPC loot. */
    public void trackDrop(GroundItem drop) {
        writable();
        if (drop == null || !contains(drop.getLocation())) throw new IllegalArgumentException("Drop outside built instance");
        if (!drops.contains(drop)) manager.requireRoom(this, "drops");
        drops.add(drop);
    }
    public void untrackDrop(GroundItem drop) { manager.checkThread(); drops.remove(drop); }

    public Tick schedule(int delay, Runnable action) { return schedule(delay, 0, action); }
    /** period=0 runs once; otherwise repeats after period world cycles. */
    public Tick schedule(int delay, int period, Runnable action) {
        writable();
        if (delay < 1 || period < 0 || action == null) throw new IllegalArgumentException("Invalid task");
        return addTask(new Tick(delay) {
            public void execute() {
                try { action.run(); } catch (RuntimeException e) { stop(); throw e; }
                if (period == 0) stop(); else setTime(period);
            }
        }, null);
    }
    public Tick submitNpcTask(NPC npc, Tick task) {
        manager.checkThread();
        if (!npcs.contains(npc) || (state != State.ACTIVE && state != State.BUILDING)) {
            task.stop(); return task;
        }
        return addTask(task, npc);
    }
    public Tick submitTask(Tick task) {
        writable();
        if (task == null || !task.isRunning()) throw new IllegalArgumentException("Invalid task");
        return addTask(task, null);
    }
    private Tick addTask(Tick task, NPC npc) {
        try { manager.requireRoom(this, "tasks"); } catch (RuntimeException failure) { task.stop(); throw failure; }
        OwnedTask owned = new OwnedTask(task, npc);
        tasks.add(owned);
        try { manager.schedule(owned); } catch (RuntimeException failure) { owned.stop(); throw failure; }
        return owned;
    }
    private final class OwnedTask extends Tick {
        private final Tick delegate;
        private final NPC npc;
        private boolean cancelled;
        OwnedTask(Tick delegate, NPC npc) { super(1); this.delegate = delegate; this.npc = npc; }
        public void execute() {
            manager.checkThread();
            if (!isRunning()) return;
            if (state == State.BUILDING) return;
            if (state != State.ACTIVE || (npc != null && !npcs.contains(npc))) { stop(); return; }
            if (!delegate.run()) stop();
        }
        public void stop() {
            manager.checkThread();
            if (cancelled) return;
            super.stop(); cancelled = true;
            try { delegate.stop(); } finally { tasks.remove(this); }
        }
        public void start() {
            manager.checkThread();
            if (cancelled || state == State.CLOSING || state == State.CLOSED)
                throw new IllegalStateException("Cancelled instance task cannot restart");
            super.start();
        }
    }
    /** A failed cleanup keeps the allocation and registry entry quarantined for retry. */
    public boolean close(String reason) {
        manager.checkThread();
        if (state != State.CLOSING && state != State.CLOSED) {
            if (reason == null || reason.length() > 160) throw new IllegalArgumentException("Invalid close reason");
            closeReason = reason;
        }
        return close();
    }
    public boolean close() {
        manager.checkThread();
        if (state == State.CLOSED) return true;
        if (closingNow) return false;
        closingNow = true;
        try {
            if (state != State.CLOSING) closingSince = manager.now();
            state = State.CLOSING; closeAttempts++; closeFailures.clear();
            if (party != null) attempt("party invitations", () -> party.closing());
            for (OwnedTask task : new ArrayList<OwnedTask>(tasks)) attempt("task", () -> task.stop());
            for (Player player : new ArrayList<Player>(members.keySet())) attempt("player " + player.getUsername(), () -> leave(player));
            for (NPC npc : new ArrayList<NPC>(npcs)) attempt("NPC " + npc.getId(), () -> removeNpc(npc));
            for (GroundItem drop : new ArrayList<GroundItem>(drops)) attempt("drop", () -> {
                GroundItemManager.discardGroundItem(drop); drops.remove(drop);
            });
            for (GameObject object : new ArrayList<GameObject>(objects)) attempt("object " + object.getId(), () -> removeObject(object));
            if (closeFailures.isEmpty()) attempt("map release", () -> {
                RegionBuilder.releaseMap(map);
            });
            if (!closeFailures.isEmpty()) return false;
            state = State.CLOSED; manager.closed(this);
            departureHandler = player -> {};
            respawnLocation = null; respawnHandler = player -> {};
            return true;
        } finally { closingNow = false; }
    }
    private void attempt(String resource, Runnable cleanup) {
        try { cleanup.run(); } catch (RuntimeException failure) {
            closeFailures.add(resource + ": " + failure.toString());
        }
    }
}
