package org.dementhium.model.instance;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import org.dementhium.model.Location;
import org.dementhium.model.player.Player;
import org.dementhium.tickable.Tick;

/** Invitation-only admission and deterministic leadership for one managed session. */
public final class InstanceParty {
    public enum LeaderDeparture { PROMOTE_OLDEST, CLOSE_SESSION }
    private final GameInstance instance;
    private final LeaderDeparture departure;
    private final List<Player> joinOrder = new ArrayList<Player>();
    private final Map<Player, Invitation> invitations = new IdentityHashMap<Player, Invitation>();
    private Player leader, admitting;
    private boolean started;
    private static final class Invitation { Tick expiry; }

    InstanceParty(GameInstance instance, Player leader, LeaderDeparture departure) {
        if (leader == null || departure == null) throw new IllegalArgumentException("Missing party leader/policy");
        this.instance = instance; this.leader = leader; this.departure = departure;
    }
    public GameInstance getInstance() { return instance; }
    public Player getLeader() { instance.getManager().checkThread(); return leader; }
    public LeaderDeparture getLeaderDeparturePolicy() { return departure; }
    public int getInvitationCount() { instance.getManager().checkThread(); return invitations.size(); }
    public boolean isInvited(Player player) { instance.getManager().checkThread(); return invitations.containsKey(player); }
    boolean isAdmitting(Player player) { return admitting != null && admitting == player; }
    private boolean connected(Player player) {
        return player != null && player.isOnline() && !player.destroyed() && !player.getConnection().isDisconnected()
                && !player.getConnection().isInLobby();
    }
    private void active() {
        instance.getManager().checkThread();
        if (!instance.isActive()) throw new IllegalStateException("Party is not active");
    }
    private void requireLeader(Player actor) {
        active();
        if (actor != leader || !instance.isMember(actor) || !connected(actor) || actor.isDead())
            throw new IllegalStateException("Only the active party leader can do that");
    }
    /** Invitations bind the current Player identity, not a future login with the same name.
     * Expiry is measured by the instance scheduler. Invitations do not reserve membership capacity. */
    public void invite(Player actor, Player target, int lifetimeCycles) {
        requireLeader(actor);
        if (!connected(target) || target.isDead() || target == actor || InstanceAccess.owner(target) != null
                || target.getFamiliar() != null || lifetimeCycles < 1 || lifetimeCycles > 10000
                || (target.getActivity() != null && target.getActivity().isRunning()))
            throw new IllegalArgumentException("Invalid party invitation");
        if (!invitations.containsKey(target) && invitations.size() >= instance.getCapacity() - 1)
            throw new IllegalStateException("Too many outstanding invitations");
        revoke(target);
        Invitation invitation = new Invitation(); invitations.put(target, invitation);
        try {
            invitation.expiry = instance.schedule(lifetimeCycles, () -> {
                if (invitations.get(target) == invitation) invitations.remove(target);
            });
        } catch (RuntimeException failure) { invitations.remove(target, invitation); throw failure; }
    }
    public void revoke(Player actor, Player target) { requireLeader(actor); revoke(target); }
    private void revoke(Player target) {
        Invitation old = invitations.remove(target);
        if (old != null && old.expiry != null) old.expiry.stop();
    }
    private void clearInvitations() {
        for (Player target : new ArrayList<Player>(invitations.keySet())) revoke(target);
    }
    /** The first admission must be the configured leader. Later admissions require a live invitation. */
    public void join(Player player, Location spawn, Location returnTo) {
        active();
        if (admitting != null || player == null || instance.isMember(player)
                || (started && (!connected(leader) || !instance.isMember(leader)))
                || (!started ? player != leader : !invitations.containsKey(player)))
            throw new IllegalStateException("No party admission authorization");
        admitting = player;
        try {
            instance.enter(player, spawn, returnTo);
            joinOrder.add(player); started = true;
            revoke(player);
        } catch (RuntimeException failure) {
            // Partial entry that cannot evacuate remains owned and must be quarantined.
            if (instance.isMember(player)) instance.close();
            throw failure;
        } finally { admitting = null; }
    }
    public void transferLeadership(Player actor, Player successor) {
        requireLeader(actor);
        if (!instance.isMember(successor) || !connected(successor))
            throw new IllegalArgumentException("Successor must be a connected party member");
        if (successor == leader) return;
        clearInvitations(); leader = successor;
    }
    public void kick(Player actor, Player target) {
        requireLeader(actor);
        if (target == actor || !instance.isMember(target)) throw new IllegalArgumentException("Invalid kick target");
        InstanceAccess.depart(target, false);
    }
    public void leave(Player member) {
        active();
        if (!instance.isMember(member)) throw new IllegalArgumentException("Not a party member");
        InstanceAccess.depart(member, false);
    }
    public boolean close(Player actor) { requireLeader(actor); return instance.close(); }

    /** Called after successful unbinding on every departure path, including direct leave/teleport/logout. */
    void departed(Player player) {
        if (!joinOrder.remove(player)) return; // An entry rollback was never a successful join.
        revoke(player);
        if (!instance.isActive()) return;
        if (instance.getMemberCount() == 0) { instance.close(); return; }
        if (player != leader) return;
        clearInvitations();
        if (departure == LeaderDeparture.PROMOTE_OLDEST) {
            for (Player candidate : joinOrder) if (instance.isMember(candidate) && connected(candidate)) {
                leader = candidate; return;
            }
        }
        instance.close();
    }
    void closing() {
        clearInvitations(); leader = null;
        // Membership remains tracked by GameInstance until evacuation succeeds on a retry.
        joinOrder.clear();
    }
}
