package org.dementhium.model.instance;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.dementhium.model.*;
import org.dementhium.model.map.region.RegionBuilder;
import org.dementhium.model.player.Player;
import org.dementhium.content.misc.GraveStoneManager;

/** Shared boundaries and session transitions. Ordinary-world behaviour passes through. */
public final class InstanceAccess {
    private InstanceAccess() { }
    static final class Membership {
        final GameInstance instance;
        final Location returnTo;
        boolean transferring;
        volatile boolean deathPending;
        long deathSequence;
        Mob killer;
        Membership(GameInstance instance, Location returnTo) { this.instance=instance; this.returnTo=returnTo; }
    }
    private static final Map<Player, Membership> MEMBERS = new ConcurrentHashMap<Player, Membership>();
    static void bind(GameInstance instance, Player player, Location returnTo) {
        if (MEMBERS.putIfAbsent(player,new Membership(instance,returnTo)) != null)
            throw new IllegalStateException("Player already bound");
        player.markInstanceTransition();
    }
    static void unbind(GameInstance instance, Player player) {
        Membership member=MEMBERS.get(player);
        if (member!=null && member.instance==instance && MEMBERS.remove(player,member)) player.markInstanceTransition();
    }
    public static GameInstance owner(Entity entity) {
        if (entity==null) return null;
        if (entity.isPlayer()) {
            Membership member=MEMBERS.get(entity.getPlayer());
            return member==null ? null : member.instance;
        }
        return entity.isNPC() ? entity.getNPC().getOwningInstance() : null;
    }
    public static boolean canInteract(Entity source, Entity target) {
        if (source==null || target==null) return false;
        GameInstance a=owner(source), b=owner(target);
        GameInstance sourceMap=InstanceManager.at(source.getLocation()), targetMap=InstanceManager.at(target.getLocation());
        if (a==null && b==null && sourceMap==null && targetMap==null) return true;
        if (a==null || !a.getManager().isCycleThread() || !a.isActive() || sourceMap!=a || targetMap!=a
                || !a.contains(source.getLocation()) || !a.contains(target.getLocation())
                || source.getLocation().getZ()!=target.getLocation().getZ()) return false;
        if ((source.isPlayer() && !a.isMember(source.getPlayer())) || (source.isNPC() && !a.owns(source.getNPC()))) return false;
        if (target.isGameObject() && target.getLocation().getGameObjectType(target.getGameObject().getType())!=target) return false;
        boolean allowed = (!target.isPlayer() || (b==a && a.isMember(target.getPlayer())))
                && (!target.isNPC() || (b==a && a.owns(target.getNPC())));
        if (allowed && (source.isPlayer() || target.isPlayer())) a.touch();
        return allowed;
    }
    public static boolean canAccess(Player player, Location target) {
        GameInstance current=owner(player), destination=InstanceManager.at(target);
        if (current==null && destination==null) return InstanceManager.at(player.getLocation())==null;
        return current!=null && current.getManager().isCycleThread() && current.isActive()
                && destination==current && current.contains(player.getLocation()) && current.contains(target)
                && player.getLocation().getZ()==target.getZ();
    }
    /** Shared by direct placement and teleport preflight. Managed NPCs cannot leave their map. */
    public static boolean canRelocate(Entity entity, Location destination) {
        if (destination==null || (!entity.isPlayer() && !entity.isNPC())) return true;
        GameInstance owner=owner(entity), target=InstanceManager.at(destination);
        if (owner==null && target==null) return !entity.isPlayer() || !RegionBuilder.isUnmappedInstanceSpace(destination);
        if (owner==null || !owner.getManager().isCycleThread()) return false;
        if (entity.isPlayer()) {
            Membership member=MEMBERS.get(entity.getPlayer());
            if (member.deathPending && !member.transferring) return false;
            if (target==null) return member.transferring || (!member.deathPending && owner.isActive());
            return target==owner && owner.isActive() && owner.contains(destination);
        }
        return target==owner && owner.owns(entity.getNPC())
                && (owner.isActive() || owner.getState()==GameInstance.State.BUILDING) && owner.contains(destination);
    }
    public static boolean canWalk(Mob mob, Location destination) {
        GameInstance current=owner(mob), target=InstanceManager.at(destination);
        if (current==null && target==null) return true;
        return current!=null && current.getManager().isCycleThread() && current.isActive() && !mob.isDead() && target==current
                && current.canOccupy(destination,mob.size());
    }
    public static void checkLocation(Entity entity, Location destination) {
        if (!canRelocate(entity,destination)) throw new IllegalStateException("Location is outside this entity's instance membership");
    }
    public static void moved(Entity entity) {
        if (!entity.isPlayer()) return;
        Membership member=MEMBERS.get(entity.getPlayer());
        if (member!=null && member.instance.getManager().isCycleThread() && member.instance.isActive()) member.instance.touch();
        if (member!=null && !member.transferring && InstanceManager.at(entity.getLocation())!=member.instance)
            depart(entity.getPlayer(),false);
    }
    static void transfer(Player player, boolean active) {
        Membership member=MEMBERS.get(player);
        if (member!=null) member.transferring=active;
    }
    public static void depart(Player player, boolean disconnected) {
        Membership member=MEMBERS.get(player);
        if (member==null) return;
        GameInstance instance=member.instance;
        instance.getManager().checkThread();
        if (disconnected) instance.leaveDisconnected(player); else instance.leave(player);
        if (instance.getMemberCount()==0) instance.close();
    }
    /** Safe coordinates are serialized without moving the live player or changing the save layout. */
    public static int savedHitPoints(Player player) {
        Membership member=MEMBERS.get(player);
        if (member!=null && member.deathPending && member.instance.getDeathPolicyForSave()!=GameInstance.DeathPolicy.STANDARD_AT_EXIT)
            return player.getSkills().getMaximumLifePoints();
        return player.getHitPoints();
    }
    public static Location saveLocation(Player player) {
        Membership member=MEMBERS.get(player);
        return member!=null ? member.returnTo : recoverLocation(player.getLocation());
    }
    public static Location recoverLocation(Location saved) {
        if (saved==null || saved.getX()<=0 || saved.getY()<=0 || saved.getX()>=16384 || saved.getY()>=16384
                || InstanceManager.at(saved)!=null || RegionBuilder.isUnmappedInstanceSpace(saved))
            return Mob.DEFAULT;
        return saved;
    }
    public static Location readLocation(int x,int y,int plane) {
        if (plane<0 || plane>3 || x<=0 || y<=0 || x>=16384 || y>=16384) return Mob.DEFAULT;
        return recoverLocation(Location.locate(x,y,plane));
    }
    public static boolean beginDeath(final Player player) {
        final Membership member=MEMBERS.get(player);
        if (member==null) return false;
        member.instance.getManager().checkThread();
        if (member.deathPending) return true;
        member.deathPending=true;
        final long sequence = ++member.deathSequence;
        member.killer=player.getDamageManager().getKiller();
        player.getCombatExecutor().reset(); player.getWalkingQueue().reset();
        player.getActionManager().stopAction();
        player.removeTick("area_event"); player.removeTick("following_mob"); player.removeTick("teleport_tick");
        player.markInstanceTransition();
        player.animate(9055);
        try {
        member.instance.schedule(1, () -> {
            if (MEMBERS.get(player)!=member || !member.deathPending || member.deathSequence!=sequence) return;
            org.dementhium.content.skills.Prayer.wrathEffect(player,member.killer);
            org.dementhium.content.skills.Prayer.retributionEffect(player,member.killer);
        });
        member.instance.schedule(5, () -> {
            if (MEMBERS.get(player)!=member || !member.deathPending || member.deathSequence!=sequence) return;
            boolean disconnected = !player.isOnline() || player.destroyed() || player.getConnection().isDisconnected();
            if (!disconnected && member.instance.isActive()
                    && member.instance.getDeathPolicy()==GameInstance.DeathPolicy.RESPAWN_INSIDE)
                respawn(player, member);
            else depart(player, disconnected);
        });
        } catch (RuntimeException failure) {
            System.err.println("Instance death scheduling failed: " + failure);
            member.instance.close();
        }
        return true;
    }
    private static void respawn(Player player, Membership member) {
        member.transferring = true;
        try {
            Location destination = member.instance.respawnLocation(player);
            player.teleport(destination, false);
            if (MEMBERS.get(player)!=member || !member.deathPending || !destination.equals(player.getLocation()))
                throw new IllegalStateException("Internal respawn teleport failed");
            finishDeath(player);
            // Same membership, new life: old damage, actions and area events must expire.
            player.markInstanceTransition();
            member.instance.respawned(player);
        } catch (RuntimeException failure) {
            System.err.println("Instance respawn failed: " + failure);
            member.instance.close();
        } finally { member.transferring = false; }
    }
    /** Resolves the pending death at its validated internal spawn or ordinary exit. */
    static void finishDeath(Player player) {
        Membership member=MEMBERS.get(player);
        if (member==null || !member.deathPending) return;
        // Claim completion before invoking death effects; never debit items twice on retry.
        member.deathPending=false;
        try {
        if (member.instance.getDeathPolicy()==GameInstance.DeathPolicy.STANDARD_AT_EXIT) {
            GraveStoneManager.appendDeath(player,member.killer);
            player.getSkullManager().removeSkull();
        }
        } finally {
            member.killer=null;
            player.getSkills().finishInstanceDeath();
        }
    }
}
