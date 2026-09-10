package org.dementhium.model.combat.impl.npc;

import org.dementhium.model.Mob;
import org.dementhium.model.Projectile;
import org.dementhium.model.combat.*;
import org.dementhium.model.instance.InstanceAccess;
import org.dementhium.model.map.path.ProjectilePathFinder;
import org.dementhium.model.misc.DamageManager.DamageType;
import org.dementhium.model.misc.ProjectileManager;

/** Single-target boss foundation. Room targeting and area attacks belong to encounter handlers. */
public class BasicBossAttack extends CombatAction {
    private final CombatType movementType;
    private final int maximum, animation, projectile, endGraphic;
    public BasicBossAttack(CombatType type, int maximum, int animation, int projectile, int endGraphic) {
        // FINALIZE repeats while endSession waits; no unowned world timer is needed.
        super(true);
        movementType = type;
        this.maximum = maximum; this.animation = animation;
        this.projectile = projectile; this.endGraphic = endGraphic;
    }
    protected CombatType selectStyle(Mob source) { return movementType; }
    protected int maximum(CombatType type) { return maximum; }
    protected int animation(CombatType type) { return animation; }
    protected int projectile(CombatType type) { return projectile; }
    protected int endGraphic(CombatType type) { return endGraphic; }

    /** Mutable attack state travels with its Interaction, including overlapping attacks. */
    private static final class Hit extends Damage {
        final CombatType type;
        final Damage damage;
        final int graphic;
        boolean consumed;
        Hit(CombatType type, Damage damage, int graphic) {
            super(damage.getHit());
            this.type = type; this.damage = damage; this.graphic = graphic;
            setMaximum(damage.getMaximum());
        }
    }
    private boolean eligible() {
        Mob s = interaction.getSource(), v = interaction.getVictim();
        return !s.isDead() && !v.isDead() && !s.isHidden() && !v.isHidden()
            && (!v.isPlayer() || v.getPlayer().isOnline())
            && s.getLocation().getZ() == v.getLocation().getZ()
            && s.getLocation().distance(v.getLocation()) <= 17
            && InstanceAccess.canInteract(s, v);
    }
    @Override public boolean commenceSession() {
        if (!eligible()) return false;
        Mob s = interaction.getSource(), v = interaction.getVictim();
        CombatType type = selectStyle(s);
        // Reject the generic follower's moving-melee shortcut for these bosses.
        if (type == CombatType.MELEE && !CombatMovement.canMelee(s, v)) return false;
        if (type != CombatType.MELEE && s.getLocation().distance(v.getLocation()) > type.getDistance()) return false;
        if (!ProjectilePathFinder.clearPath(s.getLocation(), v.getLocation())
                || !ProjectilePathFinder.clearPath(v.getLocation(), s.getLocation())) return false;
        int cap = maximum(type);
        int raw = type == CombatType.MELEE ? MeleeFormulae.getDamage(s, v, 1.0, cap, 1.0)
            : type == CombatType.RANGE ? RangeFormulae.getDamage(s, v, 1.0, cap, 1.0)
            : MagicFormulae.getDamage(s.getNPC(), v, 1.0, cap, 1.0);
        Damage damage = Damage.getDamage(s, v, type, raw);
        damage.setMaximum(cap);
        interaction.setDamage(new Hit(type, damage, endGraphic(type)));
        interaction.setTicks(type == CombatType.MELEE ? 1
            : Math.max(1, (int)Math.floor(s.getLocation().distance(v.getLocation()) * 0.3)));
        s.getCombatExecutor().setTicks(getCooldownTicks());
        s.animate(animation(type));
        if (projectile(type) >= 0) ProjectileManager.sendProjectile(
            Projectile.create(s, v, projectile(type), 30, 32, 52, 80, 3, 11));
        return true;
    }
    @Override public boolean executeSession() { return true; }
    @Override public boolean endSession() {
        if (!(interaction.getDamage() instanceof Hit)) return true;
        Hit hit = (Hit)interaction.getDamage();
        if (hit.consumed) return true;
        if (!eligible() || !hit.damage.isInstanceContextCurrent(interaction.getSource(), interaction.getVictim())) {
            hit.consumed = true;
            return true;
        }
        interaction.setTicks(interaction.getTicks() - 1);
        if (interaction.getTicks() > 0) return false;
        hit.consumed = true;
        Mob s = interaction.getSource(), v = interaction.getVictim();
        if (hit.damage.getHit() < 0) { v.graphics(85, 96 << 16); return true; }
        if (hit.graphic >= 0) v.graphics(hit.graphic);
        v.animate(v.getDefenceAnimation());
        v.getDamageManager().damage(s, hit.damage, hit.type.getDamageType());


        v.retaliate(s);
        return true;
    }
    @Override public CombatType getCombatType() { return movementType; }
}
