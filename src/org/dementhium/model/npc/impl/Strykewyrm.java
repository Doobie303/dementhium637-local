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
import org.dementhium.model.combat.RangeFormulae;
import org.dementhium.model.combat.SpiritShield;
import org.dementhium.model.definition.NPCDefinition;
import org.dementhium.model.misc.DamageManager.DamageType;
import org.dementhium.model.misc.ProjectileManager;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.npc.godwars.GodWarsAction;
import org.dementhium.model.player.Player;
import org.dementhium.model.player.Skills;

/**
 * Mound/active pairs 9462-9467 use their real identity and packed combat profile.
 * Pre-EoC burrow counterplay: forum.tip.it/topic/257859-desert-strykewyrm-drops/
 * (8 February 2010). Jungle styles/poison: tip.it/runescape/bestiary/view/1384-jungle-strykewyrm
 * (modern page; no modern stats imported). Existing stats/projectiles are preserved.
 * Six attacks between a three-tick warning and 300-LP avoidable burrow, equal contact
 * style weights and jungle's 40-LP poison chance are explicit server estimates.
 * Burrow targets one marked tile, without teleporting through terrain. The server's
 * existing free-combat access remains; historical task/fire-cape requirements are not added.
 */
public class Strykewyrm extends NPC {
    private enum Attack { MELEE, DISTANCE, BURROW }
    private final int moundId, activeId;
    private final boolean moundOrigin;
    private boolean awake;
    private int ordinaryAttacks;
    private Attack prepared;
    private Mob preparedTarget;
    private boolean preparedContact;

    public Strykewyrm(int id) {
        super(id);
        if (id < 9462 || id > 9467) throw new IllegalArgumentException("Unsupported Strykewyrm " + id);
        moundId = id & ~1;
        activeId = moundId + 1;
        moundOrigin = id == moundId;
        awake = !moundOrigin;
    }

    private boolean slayerAllowed(Player player) {
        int level = activeId == 9463 ? 93 : activeId == 9465 ? 77 : 73;
        if (player.getSkills().getLevel(Skills.SLAYER) >= level) return true;
        player.sendMessage("You need a slayer level of " + level + " to disturb this creature.");
        return false;
    }

    /** Called by the normal Investigate packet after its approach event. */
    public boolean activate(Player player) {
        if (awake || isDead() || isHidden() || player == null || !slayerAllowed(player)
                || !GodWarsAction.clear(this, player)
                || !CombatMovement.hasMeleeContact(this, player, true)) return false;
        resetCombat();
        resetCombatState();
        setId(activeId);
        setDefinition(NPCDefinition.forId(activeId));
        setHp(getMaxHp());
        awake = true;
        getMask().setSwitchId(activeId);
        player.turnTo(this, false);
        player.animate(4278);
        turnTo(player, false);
        animate(12795);
        getCombatExecutor().setVictim(player);
        getCombatExecutor().setTicks(getAttackDelay());
        return true;
    }

    @Override public boolean isAttackable(Mob attacker) {
        if (!awake) return false;
        Player owner = attacker == null ? null : attacker.isPlayer() ? attacker.getPlayer()
                : attacker.isFamiliar() ? attacker.getFamiliar().getOwner() : null;
        return (owner == null || slayerAllowed(owner)) && super.isAttackable(attacker);
    }

    @Override public void retaliate(Mob attacker) { if (awake) super.retaliate(attacker); }
    @Override public Damage updateHit(Mob source, int hit, CombatType type) {
        return awake ? super.updateHit(source, hit, type) : new Damage(0);
    }
    @Override public CombatAction getCombatAction() { return new WyrmAttack(null); }

    @Override public void resetCombatState() {
        super.resetCombatState();
        ordinaryAttacks = 0;
        prepared = null;
        preparedTarget = null;
    }

    @Override public void setDead(boolean dead) {
        boolean respawning = isDead() && !dead;
        super.setDead(dead);
        // Keep the active identity throughout the existing death/reward callback.
        if (respawning && moundOrigin) {
            awake = false;
            setId(moundId);
            setDefinition(NPCDefinition.forId(moundId));
            setHp(getMaxHp());
            getMask().setSwitchId(moundId);
        }
    }

    private Attack prepare() {
        Mob target = getCombatExecutor().getVictim();
        boolean contact = target != null && CombatMovement.canMelee(this, target);
        if (prepared == null || preparedTarget != target || preparedContact != contact) {
            preparedTarget = target;
            preparedContact = contact;
            prepared = ordinaryAttacks >= 6 ? Attack.BURROW
                    : contact && getRandom().nextBoolean() ? Attack.MELEE : Attack.DISTANCE;
        }
        return prepared;
    }

    private final class WyrmAttack extends CombatAction {
        private final Attack selected;
        private int remaining, raw, maximum;
        private boolean launched, consumed;
        private Location marked;
        WyrmAttack(Attack selected) { super(true); this.selected = selected; }
        private Attack attack() { return selected == null ? prepare() : selected; }
        @Override public CombatType getCombatType() {
            return attack() == Attack.MELEE ? CombatType.MELEE
                    : activeId == 9465 ? CombatType.RANGE : CombatType.MAGIC;
        }
        @Override public CombatAction newSession() {
            Attack chosen = prepare();
            prepared = null;
            return new WyrmAttack(chosen);
        }
        @Override public boolean commenceSession() {
            Mob target = interaction.getVictim();
            if (!awake) { getCombatExecutor().reset(); return false; }
            if (launched || !interaction.isNPCContextCurrent() || !GodWarsAction.clear(Strykewyrm.this, target)
                    || attack() == Attack.MELEE && !CombatMovement.canMelee(Strykewyrm.this, target)) return false;
            launched = true;
            getCombatExecutor().setTicks(getAttackDelay());
            if (attack() == Attack.BURROW) {
                ordinaryAttacks = 0;
                marked = target.getLocation();
                remaining = 3;
                maximum = raw = 300;
                setAttribute("freezeTime", Math.max(getAttribute("freezeTime", -1), World.getTicks() + remaining + 1));
                getWalkingQueue().reset();
                animate(12794);
                if (target.isPlayer()) target.getPlayer().sendMessage("The ground beneath you trembles. Move away!");
            } else {
                ordinaryAttacks++;
                CombatType type = getCombatType();
                maximum = type == CombatType.MELEE ? MeleeFormulae.getMeleeDamage(Strykewyrm.this, 1.0)
                        : type == CombatType.RANGE ? RangeFormulae.getRangeDamage(Strykewyrm.this, 1.0)
                        : (int)MagicFormulae.getMaximumMagicDamage(Strykewyrm.this, 1.0);
                raw = type == CombatType.MELEE ? MeleeFormulae.getDamage(Strykewyrm.this, target, 1.0, maximum, 1.0)
                        : type == CombatType.RANGE ? RangeFormulae.getDamage(Strykewyrm.this, target, 1.0, maximum, 1.0)
                        : MagicFormulae.getDamage(Strykewyrm.this, target, 1.0, maximum, 1.0);
                animate(getAttackAnimation());
                remaining = type == CombatType.MELEE ? 1
                        : Math.max(1, (int)Math.ceil(getLocation().distance(target.getLocation()) * 0.3));
                if (type != CombatType.MELEE)
                    ProjectileManager.sendProjectile(Projectile.create(Strykewyrm.this, target,
                            getDefinition().getProjectileId(), 40, 32, 16, remaining * 30, 3));
            }
            return true;
        }
        @Override public boolean executeSession() { return true; }
        @Override public boolean endSession() {
            if (consumed) return true;
            Mob target = interaction.getVictim();
            if (!interaction.isNPCContextCurrent() || !NPCCombatContext.validPair(Strykewyrm.this, target)) {
                consumed = true;
                return true;
            }
            if (--remaining > 0) return false;
            consumed = true;
            boolean burrow = attack() == Attack.BURROW;
            if (burrow) {
                animate(12795);
                if (!marked.equals(target.getLocation()) || !GodWarsAction.clear(Strykewyrm.this, target)) return true;
            }
            Damage damage = burrow ? new Damage(target.isPlayer()
                    ? CombatStatus.statusAllowed(target) ? SpiritShield.reduce(target.getPlayer(), raw) : 0 : raw)
                    : Damage.getDamage(Strykewyrm.this, target, getCombatType(), raw);
            damage.setMaximum(maximum);
            interaction.setDamage(damage);
            if (!burrow && activeId == 9467 && getRandom().nextInt(4) == 0)
                CombatStatus.poisonOnImpact(damage, Strykewyrm.this, target, 40);
            if (!burrow && attack() == Attack.DISTANCE) target.graphics(raw < 0 ? 85 : getDefinition().getEndGraphics());
            target.animate(target.getDefenceAnimation());
            target.getDamageManager().damage(Strykewyrm.this, damage, burrow ? DamageType.RED_DAMAGE : getCombatType().getDamageType());
            target.retaliate(Strykewyrm.this);
            return true;
        }
    }
}
