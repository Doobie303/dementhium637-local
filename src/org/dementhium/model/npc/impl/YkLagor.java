package org.dementhium.model.npc.impl;

import org.dementhium.model.Location;
import org.dementhium.model.Mob;
import org.dementhium.model.Projectile;
import org.dementhium.model.World;
import org.dementhium.model.combat.CombatAction;
import org.dementhium.model.combat.CombatMovement;
import org.dementhium.model.combat.CombatStatus;
import org.dementhium.model.combat.CombatType;
import org.dementhium.model.combat.Damage;
import org.dementhium.model.combat.MagicFormulae;
import org.dementhium.model.combat.MeleeFormulae;
import org.dementhium.model.combat.NPCCombatContext;
import org.dementhium.model.combat.SpiritShield;
import org.dementhium.model.instance.InstanceAccess;
import org.dementhium.model.map.Region;
import org.dementhium.model.map.path.ProjectilePathFinder;
import org.dementhium.model.misc.DamageManager.DamageType;
import org.dementhium.model.misc.ProjectileManager;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.npc.godwars.GodWarsAction;
import org.dementhium.model.player.Player;

/**
 * Simplified fight for the existing ordinary-world spawn, retaining NPC rewards and respawn.
 * Observed mechanics: forum.tip.it/topic/275641-yklagor-the-thunderous-melee/ (29 Aug 2010).
 * Assets: rune-server.org/threads/complete-thunderous-config-pack.492315/ (2013 asset loop),
 * checked in the local cache: Yk sequences share walk14397's frame group3365.
 *
 * This is an explicit server design: six attacks between alternating specials, a five-tick
 * warning, bounded damage and a six-tile footprint radius. Cover OR leaving that radius
 * avoids specials, making counterplay possible outside Daemonheim's four-pillar room.
 * Only the current combat target is affected. Mage release/scaling, party-wide attacks,
 * gatestone restrictions and Daemonheim door progression are not implemented here.
 */
public class YkLagor extends NPC {
    private enum Attack { MELEE, MAGIC, GRAB, QUAKE }
    private static final int WARNING_TICKS = 5;
    private int ordinaryAttacks;
    private boolean quakeNext;

    public YkLagor(int id) {
        super(id);
        if (id != 11886) throw new IllegalArgumentException("Unsupported Yk'Lagor form " + id);
    }

    @Override public CombatAction getCombatAction() { return new ThunderAttack(null); }

    @Override public void resetCombatState() {
        super.resetCombatState();
        ordinaryAttacks = 0;
        quakeNext = false;
    }

    private Attack choose() {
        if (ordinaryAttacks >= 6) return quakeNext ? Attack.QUAKE : Attack.GRAB;
        Mob target = getCombatExecutor().getVictim();
        return target != null && CombatMovement.hasMeleeContact(this, target, true)
                ? Attack.MELEE : Attack.MAGIC;
    }

    private int footprintGap(Mob target) {
        int x = target.getLocation().getX(), y = target.getLocation().getY();
        int lowX = getLocation().getX(), lowY = getLocation().getY();
        return Math.max(Math.max(lowX - x, x - (lowX + size() - 1)),
                Math.max(lowY - y, y - (lowY + size() - 1)));
    }

    /** Short immediate pull: every crossed tile must be walkable and outside NPC bodies. */
    private void pull(Player player) {
        if (player.getAttribute("cantMove", false)) return;
        Location start = player.getLocation(), destination = start;
        for (int step = 0; step < 2; step++) {
            int nx = Math.max(getLocation().getX(), Math.min(destination.getX(), getLocation().getX() + size() - 1));
            int ny = Math.max(getLocation().getY(), Math.min(destination.getY(), getLocation().getY() + size() - 1));
            int dx = Integer.signum(nx - destination.getX()), dy = Integer.signum(ny - destination.getY());
            if (dx == 0 && dy == 0) break;
            Location next = destination.transform(dx, dy, 0);
            if (!ProjectilePathFinder.clearMeleePath(destination, next)
                    || !InstanceAccess.canWalk(player, next) || occupies(this, next)) break;
            boolean occupied = false;
            for (NPC npc : Region.getLocalNPCs(next, 10))
                if (!npc.isHidden() && !npc.isDead() && occupies(npc, next)) { occupied = true; break; }
            if (occupied) break;
            destination = next;
        }
        player.getWalkingQueue().reset();
        if (!destination.equals(start)) player.teleport(destination, false);
        player.animate(14388);
    }

    private static boolean occupies(NPC npc, Location location) {
        return location.getX() >= npc.getLocation().getX() && location.getX() < npc.getLocation().getX() + npc.size()
                && location.getY() >= npc.getLocation().getY() && location.getY() < npc.getLocation().getY() + npc.size();
    }

    private final class ThunderAttack extends CombatAction {
        private final Attack selected;
        private int remaining, raw, maximum;
        private boolean launched, consumed;
        ThunderAttack(Attack selected) { super(true); this.selected = selected; }
        private Attack attack() { return selected == null ? choose() : selected; }
        @Override public CombatType getCombatType() { return attack() == Attack.MELEE ? CombatType.MELEE : CombatType.MAGIC; }
        @Override public CombatAction newSession() { return new ThunderAttack(choose()); }
        @Override public boolean commenceSession() {
            Mob victim = interaction.getVictim();
            Attack attack = attack();
            if (launched || !interaction.isNPCContextCurrent() || !GodWarsAction.clear(YkLagor.this, victim)
                    || attack == Attack.MELEE && !CombatMovement.hasMeleeContact(YkLagor.this, victim, true)) return false;
            launched = true;
            if (attack == Attack.GRAB || attack == Attack.QUAKE) {
                ordinaryAttacks = 0;
                quakeNext = attack == Attack.GRAB;
                remaining = WARNING_TICKS;
                // Native movement honors this tick deadline; reset/death clears it normally.
                setAttribute("freezeTime", Math.max(getAttribute("freezeTime", -1), World.getTicks() + remaining + 1));
                getWalkingQueue().reset();
                getCombatExecutor().setTicks(Math.max(getAttackDelay(), remaining + 2));
                animate(14370);
                forceText(attack == Attack.GRAB ? "Come closer!" : "This is...");
                if (victim.isPlayer()) victim.getPlayer().sendMessage("Take cover or run well clear of the Thunderous!");
                maximum = attack == Attack.GRAB ? 200 : 450;
                raw = attack == Attack.GRAB ? 100 + getRandom().nextInt(101) : 300 + getRandom().nextInt(151);
            } else {
                ordinaryAttacks++;
                getCombatExecutor().setTicks(getAttackDelay());
                maximum = attack == Attack.MELEE ? 300 : 280;
                raw = attack == Attack.MELEE ? MeleeFormulae.getDamage(YkLagor.this, victim, 1.0, maximum, 1.0)
                        : MagicFormulae.getDamage(YkLagor.this, victim, 1.0, maximum, 1.0);
                animate(attack == Attack.MELEE ? 14374 : 14396);
                remaining = attack == Attack.MELEE ? 1
                        : Math.max(1, (int)Math.ceil(getLocation().distance(victim.getLocation()) * 0.3));
                if (attack == Attack.MAGIC) {
                    graphics(2754);
                    ProjectileManager.sendProjectile(Projectile.create(YkLagor.this, victim, 2735, 45, 30, 16, remaining * 30, 0));
                }
            }
            return true;
        }
        @Override public boolean executeSession() { return true; }
        @Override public boolean endSession() {
            if (consumed) return true;
            Mob victim = interaction.getVictim();
            if (!interaction.isNPCContextCurrent() || !NPCCombatContext.validPair(YkLagor.this, victim)) {
                consumed = true;
                return true;
            }
            if (--remaining > 0) return false;
            consumed = true;
            Attack attack = attack();
            boolean special = attack == Attack.GRAB || attack == Attack.QUAKE;
            if (special) {
                animate(attack == Attack.GRAB ? 14390 : 14412);
                graphics(attack == Attack.GRAB ? 2768 : 2776);
                if (attack == Attack.QUAKE) forceText("TRUE POWER!");
                if (footprintGap(victim) > 6 || !GodWarsAction.clear(YkLagor.this, victim)) return true;
            }
            // Magic pierces prayer; melee remains protectable. Specials are bounded typeless hits.
            Damage damage = special ? new Damage(victim.isPlayer()
                    ? CombatStatus.statusAllowed(victim) ? SpiritShield.reduce(victim.getPlayer(), raw) : 0 : raw)
                    : Damage.getDamage(YkLagor.this, victim, getCombatType(), raw, attack != Attack.MELEE);
            damage.setMaximum(maximum);
            interaction.setDamage(damage);
            if (victim.isPlayer() && raw >= 0) {
                Player player = victim.getPlayer();
                damage.onContact(() -> {
                    if (!CombatStatus.statusAllowed(player)) return;
                    player.getSkills().drainPray(attack == Attack.GRAB ? 10 : 3);
                    if (attack == Attack.GRAB) {
                        pull(player);
                        player.getPrayer().closeAllPrayers();
                        player.stun(3, "The Thunderous catches you and draws strength from you!", false);
                        heal(Math.max(1, getMaxHp() * 15 / 100));
                    }
                });
            }
            if (attack == Attack.MAGIC) victim.graphics(raw < 0 ? 85 : 2755);
            if (!special) victim.animate(victim.getDefenceAnimation());
            victim.getDamageManager().damage(YkLagor.this, damage, special ? DamageType.RED_DAMAGE : getCombatType().getDamageType());
            victim.retaliate(YkLagor.this);
            return true;
        }
    }
}
