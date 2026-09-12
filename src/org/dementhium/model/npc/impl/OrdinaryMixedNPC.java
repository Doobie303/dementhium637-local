package org.dementhium.model.npc.impl;

import org.dementhium.model.Mob;
import org.dementhium.model.Projectile;
import org.dementhium.model.combat.CombatAction;
import org.dementhium.model.combat.CombatMovement;
import org.dementhium.model.combat.CombatStatus;
import org.dementhium.model.combat.CombatType;
import org.dementhium.model.combat.CombatUtils;
import org.dementhium.model.combat.Damage;
import org.dementhium.model.combat.MagicFormulae;
import org.dementhium.model.combat.MeleeFormulae;
import org.dementhium.model.combat.NPCCombatContext;
import org.dementhium.model.combat.RangeFormulae;
import org.dementhium.model.combat.SpiritShield;
import org.dementhium.model.misc.ProjectileManager;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.npc.encounter.AdvancedAttack;
import org.dementhium.model.npc.godwars.GodWarsAction;
import org.dementhium.model.player.Bonuses;
import org.dementhium.model.player.Equipment;
import org.dementhium.model.player.Player;
import org.dementhium.model.player.Skills;

/**
 * Ordinary-world family behavior selected only by the explicit custom_npcs registry.
 * Stats remain in NPCDefinition. These are conservative pre-EoC implementations,
 * not exact historical attack weights. Existing arena/boss handlers are independent.
 *
 * Behavior evidence (accessed 2026-09-11): tip.it/runescape/pages/view/
 * mithril_dragon_hunting.htm, waterfiends.htm and revenants.htm (later pre-EoC);
 * forum.tip.it/topic/294928-skeletal-wyverns-100-150-crimsons-per-hour-and-profit/
 * (May 2011). Native family sequences were checked against the local cache.
 */
public final class OrdinaryMixedNPC extends NPC {
    private enum Family { MITHRIL, BRUTAL_GREEN, WYVERN, REVENANT, WATERFIEND, FROST }
    private final Family family;
    private CombatType prepared;
    private Mob preparedTarget;
    private boolean preparedContact;
    private int healsRemaining = 5;

    public OrdinaryMixedNPC(int id) {
        super(id);
        if (id == 5363) family = Family.MITHRIL;
        else if (id == 5362) family = Family.BRUTAL_GREEN;
        else if (id >= 3068 && id <= 3071) family = Family.WYVERN;
        else if (id >= 13465 && id <= 13481) family = Family.REVENANT;
        else if (id == 5361 || id == 9054 || id >= 9056 && id <= 9059
                || id >= 9061 && id <= 9064) family = Family.WATERFIEND;
        else if (id >= 11633 && id <= 11636 || id >= 10770 && id <= 10775) family = Family.FROST;
        else throw new IllegalArgumentException("No ordinary mixed-combat family for NPC " + id);
    }

    @Override public CombatAction getCombatAction() { return new FamilyAttack(null); }

    @Override public boolean isAttackable(Mob attacker) {
        Player owner = attacker == null ? null : attacker.isPlayer() ? attacker.getPlayer()
                : attacker.isFamiliar() ? attacker.getFamiliar().getOwner() : null;
        if (family == Family.WYVERN && owner != null && owner.getSkills().getLevel(Skills.SLAYER) < 72) {
            owner.sendMessage("You need a slayer level of 72 to attack this creature.");
            return false;
        }
        return super.isAttackable(attacker);
    }

    @Override public Damage updateHit(Mob source, int hit, CombatType type) {
        if ((family == Family.MITHRIL || family == Family.BRUTAL_GREEN || family == Family.FROST) && type == CombatType.DRAGONFIRE)
            return new Damage(0); // Match the existing metal/frost dragon family immunity.
        return super.updateHit(source, hit, type);
    }

    @Override public void resetCombatState() {
        super.resetCombatState();
        prepared = null;
        preparedTarget = null;
        healsRemaining = 5;
    }

    private CombatType prepare() {
        Mob target = getCombatExecutor().getVictim();
        boolean contact = target != null && CombatMovement.canMelee(this, target);
        if (prepared == null || preparedTarget != target || preparedContact != contact) {
            preparedTarget = target;
            preparedContact = contact;
            prepared = choose(target);
        }
        return prepared;
    }

    private CombatType choose(Mob target) {
        boolean contact = target != null && CombatMovement.canMelee(this, target);
        if (family == Family.WATERFIEND)
            return getRandom().nextBoolean() ? CombatType.RANGE : CombatType.MAGIC;
        if (family == Family.REVENANT) return revenantStyle(target, contact);
        if (contact && getRandom().nextBoolean()) return CombatType.MELEE;
        if (family == Family.WYVERN)
            return getRandom().nextBoolean() ? CombatType.RANGE : CombatType.DRAGONFIRE;
        if (family == Family.FROST || family == Family.BRUTAL_GREEN)
            return getRandom().nextBoolean() ? CombatType.MAGIC : CombatType.DRAGONFIRE;
        if (contact) return getRandom().nextBoolean() ? CombatType.MAGIC : CombatType.DRAGONFIRE;
        return new CombatType[]{CombatType.RANGE, CombatType.MAGIC, CombatType.DRAGONFIRE}[getRandom().nextInt(3)];
    }

    /** Approximate the documented preference for weak defence and unprotected styles. */
    private CombatType revenantStyle(Mob target, boolean contact) {
        CombatType[] choices = contact
                ? new CombatType[]{CombatType.MELEE, CombatType.RANGE, CombatType.MAGIC}
                : new CombatType[]{CombatType.RANGE, CombatType.MAGIC};
        if (target == null || !target.isPlayer()) return choices[getRandom().nextInt(choices.length)];
        Player player = target.getPlayer();
        CombatType result = choices[0];
        int lowest = Integer.MAX_VALUE, ties = 0;
        for (CombatType type : choices) {
            int bonus = player.getBonuses().getBonus(type == CombatType.MAGIC ? Bonuses.MAGIC_DEFENCE
                    : type == CombatType.RANGE ? Bonuses.RANGED_DEFENCE : Bonuses.STAB_DEFENCE);
            if (player.getPrayer().usingPrayer(0, type.getProtectionPrayer())
                    || player.getPrayer().usingPrayer(1, type.getDeflectCurse())) bonus += 10000;
            if (bonus < lowest) { lowest = bonus; result = type; ties = 1; }
            else if (bonus == lowest && getRandom().nextInt(++ties) == 0) result = type;
        }
        return result;
    }

    private int maximum(CombatType type) {
        if (family == Family.FROST) {
            int cap = type == CombatType.MELEE ? 214 : type == CombatType.DRAGONFIRE ? 595 : 250;
            if (getId() >= 10770 && getId() <= 10772) {
                // Server-designed low tiers scale against ordinary frost's 103 Strength/116 Magic.
                // Preserve full caps on existing/high-tier profiles, including unusual existing 10775 stats.
                int level = getCombatLevel(type == CombatType.MELEE ? Skills.STRENGTH : Skills.MAGIC);
                cap = Math.min(cap, Math.max(1, cap * level / (type == CombatType.MELEE ? 103 : 116)));
            }
            return cap;
        }
        if (type == CombatType.DRAGONFIRE) return family == Family.WYVERN ? 500 : 595;
        if (family == Family.WATERFIEND) return 120;
        if (family == Family.MITHRIL && type != CombatType.MELEE) return 180;
        return type == CombatType.MELEE ? MeleeFormulae.getMeleeDamage(this, 1.0)
                : type == CombatType.RANGE ? RangeFormulae.getRangeDamage(this, 1.0)
                : (int)MagicFormulae.getMaximumMagicDamage(this, 1.0);
    }

    private int animation(CombatType type) {
        if (family == Family.WATERFIEND) return 299;
        if (family == Family.FROST) return type == CombatType.MELEE ? 13155 : 13152;
        if (family == Family.MITHRIL) return type == CombatType.MELEE ? getAttackAnimation() : 14246;
        if (family == Family.BRUTAL_GREEN) return type == CombatType.MELEE ? getAttackAnimation() : 14245;
        if (family == Family.WYVERN) return type == CombatType.MELEE ? getAttackAnimation() : 1593;
        return getAttackAnimation();
    }

    private int projectile(CombatType type) {
        if (type == CombatType.MELEE) return -1;
        // Local assets plus observed 2012 asset loop: rune-server.org/threads/revenant-projectiles.420727/
        if (family == Family.REVENANT) return type == CombatType.RANGE ? 1278 : 1276;
        // The existing Water Blast presentation suits both water attacks; exact range visual is unverified.
        if (family == Family.WATERFIEND) return 2705;
        if (family == Family.WYVERN) return 500;
        if (family == Family.FROST) return 2465;
        // Existing metal-dragon fire; ordinary arrow/water spell are explicit visual estimates.
        return type == CombatType.DRAGONFIRE ? 2464 : type == CombatType.RANGE ? 16 : 136;
    }

    private int endGraphic(CombatType type) {
        if (type == CombatType.MELEE) return -1;
        if (family == Family.REVENANT) return type == CombatType.RANGE ? 1279 : 1277;
        if (family == Family.WATERFIEND) return 2706;
        if (family == Family.WYVERN) return 501;
        return -1;
    }

    /** Ice breath is not ordinary dragonfire: antifire potions/anti-dragon shields do not protect. */
    private Damage iceBreath(Mob target, int raw) {
        if (!target.isPlayer()) return new Damage(raw);
        Player player = target.getPlayer();
        int shield = player.getEquipment().getSlot(Equipment.SLOT_SHIELD);
        boolean dfs = CombatUtils.isDragonfireShield(shield);
        boolean protectedBreath = dfs || shield == 2890 || shield == 9731 || shield == 18691;
        if (dfs && CombatStatus.statusAllowed(player)) CombatUtils.chargeDragonfireShield(player);
        // Conservative server estimate: shielded breath is capped at 140 LP, never total immunity.
        int hit = protectedBreath ? raw * 140 / 500 : raw;
        return new Damage(CombatStatus.statusAllowed(player) ? SpiritShield.reduce(player, hit) : 0);
    }

    private final class FamilyAttack extends CombatAction {
        private final CombatType selected;
        private int raw, maximum, remaining;
        private boolean launched, consumed, healing;

        FamilyAttack(CombatType selected) { super(true); this.selected = selected; }
        @Override public CombatType getCombatType() { return selected == null ? prepare() : selected; }
        @Override public CombatAction newSession() {
            CombatType type = prepare();
            prepared = null;
            return new FamilyAttack(type);
        }
        @Override public boolean commenceSession() {
            Mob target = interaction.getVictim();
            CombatType type = getCombatType();
            if (launched || !interaction.isNPCContextCurrent() || !GodWarsAction.clear(OrdinaryMixedNPC.this, target)
                    || type == CombatType.MELEE && !CombatMovement.canMelee(OrdinaryMixedNPC.this, target)) return false;
            launched = true;
            getCombatExecutor().setTicks(getAttackDelay());
            // Limited healing replaces a normal attack. Five uses/one-fifth HP are server estimates.
            if (family == Family.REVENANT && healsRemaining > 0 && getHp() < getMaxHp() / 2) {
                healsRemaining--;
                heal(Math.max(1, getMaxHp() / 5));
                getPoisonManager().removePoison();
                healing = true;
                return true;
            }
            maximum = maximum(type);
            raw = type == CombatType.DRAGONFIRE ? getRandom().nextInt(maximum + 1)
                    : type == CombatType.MELEE ? MeleeFormulae.getDamage(OrdinaryMixedNPC.this, target, 1.0, maximum, 1.0)
                    : type == CombatType.RANGE ? RangeFormulae.getDamage(OrdinaryMixedNPC.this, target, 1.0, maximum, 1.0)
                    : MagicFormulae.getDamage(OrdinaryMixedNPC.this, target, 1.0, maximum, 1.0);
            animate(animation(type));
            int projectile = projectile(type);
            remaining = projectile < 0 ? 1 : Math.max(1, (int)Math.ceil(getLocation().distance(target.getLocation()) * 0.3));
            if (projectile >= 0) ProjectileManager.sendProjectile(Projectile.create(OrdinaryMixedNPC.this, target,
                    projectile, 30, 32, 16, remaining * 30, 3));
            return true;
        }
        @Override public boolean executeSession() { return true; }
        @Override public boolean endSession() {
            if (consumed) return true;
            Mob target = interaction.getVictim();
            if (!interaction.isNPCContextCurrent() || !NPCCombatContext.validPair(OrdinaryMixedNPC.this, target)) {
                consumed = true;
                return true;
            }
            if (!healing && --remaining > 0) return false;
            consumed = true;
            if (healing) return true;
            CombatType type = getCombatType();
            Damage damage;
            if (type == CombatType.DRAGONFIRE && family == Family.WYVERN) damage = iceBreath(target, raw);
            else if (type == CombatType.DRAGONFIRE && target.isPlayer())
                damage = new Damage(AdvancedAttack.dragonfire(target.getPlayer(), AdvancedAttack.Kind.FROST_FIRE, raw));
            else damage = Damage.getDamage(OrdinaryMixedNPC.this, target, type, raw);
            damage.setMaximum(maximum);
            interaction.setDamage(damage);
            if (family == Family.WYVERN && type == CombatType.DRAGONFIRE && getRandom().nextInt(4) == 0)
                CombatStatus.freezeOnImpact(damage, target, 3);
            if (family == Family.REVENANT && type == CombatType.RANGE && getRandom().nextInt(5) == 0)
                CombatStatus.poisonOnImpact(damage, OrdinaryMixedNPC.this, target, 40);
            if (raw < 0) { if (type == CombatType.MAGIC) target.graphics(85); }
            else if (endGraphic(type) >= 0) target.graphics(endGraphic(type));
            target.animate(target.getDefenceAnimation());
            target.getDamageManager().damage(OrdinaryMixedNPC.this, damage, type.getDamageType());
            target.retaliate(OrdinaryMixedNPC.this);
            return true;
        }
    }
}
