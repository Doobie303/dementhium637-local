package org.dementhium.model.npc.impl;

import java.util.Map;
import java.util.WeakHashMap;
import org.dementhium.model.Mob;
import org.dementhium.model.Projectile;
import org.dementhium.model.World;
import org.dementhium.model.combat.*;
import org.dementhium.model.combat.impl.spells.modern.MagicDart;
import org.dementhium.model.misc.DamageManager.DamageType;
import org.dementhium.model.misc.ProjectileManager;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.npc.godwars.GodWarsAction;
import org.dementhium.model.player.Bonuses;
import org.dementhium.model.player.Equipment;
import org.dementhium.model.player.Player;
import org.dementhium.model.player.Skills;

/**
 * Explicit repaired Slayer families; no name matching or boss aliases.
 * Contact magic and equipment evidence: forum.tip.it/topic/203018-master-slayer-guide-by-axe-man-jack-aow/
 * and /topic/33471-master-slayer-guide-by-axe-man-jack-aow-updated-906/ (pre-EoC observations).
 * Gear identities are local-cache items. Existing NPC Slayer gates take precedence over task weights.
 * Unprotected damage floors and 10% combat-stat drains are conservative server estimates.
 * A carried rock hammer automatically finishes a lethal Gargoyle hit: an explicit server convenience.
 */
public class SlayerNPC extends NPC {
    private enum Family {
        SPECTRE(60), BANSHEE(15), BASILISK(40), BLOODVELD(50), COCKATRICE(25),
        GARGOYLE(75), JELLY(52), KURASK(70), PYREFIEND(30), TUROTH(55);
        final int level;
        Family(int level) { this.level = level; }
    }
    private final Family family;
    // Damage identity records the permitted weapon/spell at launch, before ammunition consumption.
    // Weak keys avoid retaining attacks abandoned by their source; life resets clear every approval.
    private final Map<Damage, Boolean> permittedHits = new WeakHashMap<Damage, Boolean>();

    public SlayerNPC(int id) {
        super(id);
        if (id >= 1604 && id <= 1607 || id >= 7801 && id <= 7804) family = Family.SPECTRE;
        else if (id == 1612 || id == 7786) family = Family.BANSHEE;
        else if (id == 1616) family = Family.BASILISK;
        else if (id == 1618 || id == 6215 || id == 7642 || id == 7643) family = Family.BLOODVELD;
        else if (id == 1620) family = Family.COCKATRICE;
        else if (id == 1610) family = Family.GARGOYLE;
        else if (id >= 1637 && id <= 1642 || id == 7459 || id == 7460) family = Family.JELLY;
        else if (id == 1608 || id == 1609) family = Family.KURASK;
        else if (id >= 1633 && id <= 1636 || id == 6216) family = Family.PYREFIEND;
        else if (id == 1623 || id >= 1626 && id <= 1629) family = Family.TUROTH;
        else throw new IllegalArgumentException("Unsupported Slayer family " + id);
    }

    private static Player owner(Mob source) {
        return source == null ? null : source.isPlayer() ? source.getPlayer()
                : source.isFamiliar() ? source.getFamiliar().getOwner() : null;
    }

    @Override public boolean isAttackable(Mob source) {
        Player player = owner(source);
        if (player != null && player.getSkills().getLevel(Skills.SLAYER) < family.level) {
            player.sendMessage("You need a Slayer level of " + family.level + " to attack this creature.");
            return false;
        }
        return super.isAttackable(source);
    }

    private boolean leafOnly() { return family == Family.TUROTH || family == Family.KURASK; }
    private boolean magicMelee() {
        return family == Family.BLOODVELD || family == Family.JELLY
                || family == Family.PYREFIEND || family == Family.BANSHEE;
    }

    /** Melee Attack accuracy versus the same weighted magic defence used by NPC magic. */
    public static int magicMeleeDamage(NPC source, Mob victim, int maximum) {
        int magic = victim.isPlayer() ? victim.getPlayer().getSkills().getLevel(Skills.MAGIC)
                : victim.getNPC().getCombatLevel(Skills.MAGIC);
        double modifier = victim.isPlayer() ? victim.getPlayer().getPrayer().getMagicDefenceModifier()
                : victim.getNPC().getMagicModifier();
        int effective = (int)Math.floor(Math.floor(Math.max(0, magic) * Math.max(0, 1 + modifier)) * 0.7 + 1e-9)
                + (int)Math.floor(CombatFormula.defenceLevel(victim) * 0.3 + 1e-9);
        int bonus = victim.isPlayer() ? victim.getPlayer().getBonuses().getBonus(Bonuses.MAGIC_DEFENCE)
                : victim.getNPC().getDefinition().getBonuses()[Bonuses.MAGIC_DEFENCE];
        return CombatRolls.hits(source.getRandom(), MeleeFormulae.getMeleeAccuracy(source, 1),
                victim.getRandom(), CombatFormula.accuracyRoll(effective, bonus, 1))
                ? source.getRandom().nextInt(Math.max(0, maximum) + 1) : -1;
    }

    private boolean permittedWeapon(Mob source, CombatType type) {
        if (source == null || !source.isPlayer()) return false;
        Player player = source.getPlayer();
        if (type == CombatType.MELEE) {
            int weapon = player.getEquipment().getSlot(Equipment.SLOT_WEAPON);
            return weapon == 4158 || weapon == 13290;
        }
        if (type == CombatType.RANGE) {
            RangeWeapon weapon = RangeWeapon.get(player.getEquipment().getSlot(Equipment.SLOT_WEAPON));
            int ammo = player.getEquipment().getSlot(Equipment.SLOT_ARROWS);
            return weapon != null && weapon.getAmmunitionSlot() == Equipment.SLOT_ARROWS
                    && weapon.getAmmunition().contains(ammo) && (ammo == 4160 || ammo == 13280);
        }
        CombatAction action = source.getCombatExecutor().getCombatAction();
        Interaction cast = action == null ? null : action.getInteraction();
        return type == CombatType.MAGIC && cast != null && cast.getSource() == source
                && cast.getVictim() == this && cast.getSpell() instanceof MagicDart;
    }

    @Override public Damage updateHit(Mob source, int hit, CombatType type) {
        if (!leafOnly()) return super.updateHit(source, hit, type);
        boolean permitted = permittedWeapon(source, type);
        Damage damage = permitted ? super.updateHit(source, hit, type) : new Damage(0);
        if (permitted) permittedHits.put(damage, Boolean.TRUE);
        return damage;
    }

    /** Called once by DamageManager before HP changes and contribution credit, for typed AND raw hits. */
    public int filterIncomingDamage(Mob source, Damage result, int amount, DamageType type) {
        if (type == DamageType.HEAL) return amount;
        boolean permitted = result != null && permittedHits.remove(result) != null;
        if (source != null && source.isPlayer() && source.getAttribute("godmode", false)) return amount;
        if (leafOnly() && !permitted) return 0;
        if (family == Family.GARGOYLE && amount >= getHp()) {
            Player player = owner(source);
            if (player == null || !player.getInventory().contains(4162)) {
                if (player != null) warning(player, "You need a rock hammer in your inventory to finish this gargoyle.");
                return Math.max(0, getHp() - 1);
            }
        }
        return amount;
    }

    @Override public void resetCombatState() {
        super.resetCombatState();
        if (permittedHits != null) permittedHits.clear();
    }

    private static boolean slayerHelmet(int head) {
        return head == 13263 || head == 14636 || head == 14637
                || head == 15492 || head == 15496 || head == 15497;
    }

    private boolean unprotected(Player player) {
        int head = player.getEquipment().getSlot(Equipment.SLOT_HAT);
        if (family == Family.SPECTRE) return head != 4168 && !slayerHelmet(head);
        if (family == Family.BANSHEE) return head != 13277 && !slayerHelmet(head)
                && (getId() == 7786 || head != 4166);
        return (family == Family.BASILISK || family == Family.COCKATRICE)
                && player.getEquipment().getSlot(Equipment.SLOT_SHIELD) != 4156;
    }

    private static void warning(Player player, String text) {
        if (player.getAttribute("slayerProtectionWarning", -1) > World.getTicks()) return;
        player.setAttribute("slayerProtectionWarning", World.getTicks() + 10);
        player.sendMessage(text);
    }

    private void drainUnprotected(Player player) {
        if (!CombatStatus.statusAllowed(player)) return;
        for (int skill : new int[]{Skills.ATTACK, Skills.STRENGTH, Skills.DEFENCE, Skills.RANGED, Skills.MAGIC}) {
            int level = player.getSkills().getLevel(skill);
            if (level > 1) player.getSkills().set(skill, Math.max(1, level - Math.max(1, level / 10)));
        }
        warning(player, "Your Slayer protection is missing; the creature's attack weakens you.");
    }

    @Override public CombatAction getCombatAction() { return new SlayerAttack(); }
    private final class SlayerAttack extends CombatAction {
        private int raw, maximum, remaining;
        private boolean launched, consumed;
        SlayerAttack() { super(true); }
        @Override public CombatAction newSession() { return new SlayerAttack(); }
        @Override public CombatType getCombatType() { return family == Family.SPECTRE ? CombatType.MAGIC : CombatType.MELEE; }
        @Override public boolean commenceSession() {
            Mob victim = interaction.getVictim();
            if (launched || !interaction.isNPCContextCurrent() || !GodWarsAction.clear(SlayerNPC.this, victim)
                    || getCombatType() == CombatType.MELEE && !CombatMovement.canMelee(SlayerNPC.this, victim)) return false;
            launched = true;
            getCombatExecutor().setTicks(getAttackDelay());
            maximum = family == Family.SPECTRE ? (int)MagicFormulae.getMaximumMagicDamage(SlayerNPC.this, 1)
                    : MeleeFormulae.getMeleeDamage(SlayerNPC.this, 1);
            raw = family == Family.SPECTRE ? MagicFormulae.getDamage(SlayerNPC.this, victim, 1, maximum, 1)
                    : magicMelee() ? magicMeleeDamage(SlayerNPC.this, victim, maximum)
                    : MeleeFormulae.getDamage(SlayerNPC.this, victim, 1, maximum, 1);
            animate(getAttackAnimation());
            remaining = family == Family.SPECTRE ? Math.max(1, (int)Math.ceil(getLocation().distance(victim.getLocation()) * .3)) : 1;
            if (family == Family.SPECTRE) {
                if (getDefinition().getStartGraphics() >= 0) graphics(getDefinition().getStartGraphics());
                if (getDefinition().getProjectileId() >= 0) ProjectileManager.sendProjectile(Projectile.create(SlayerNPC.this,
                        victim, getDefinition().getProjectileId(), 30, 32, 16, remaining * 30, 3));
            }
            return true;
        }
        @Override public boolean executeSession() { return true; }
        @Override public boolean endSession() {
            if (consumed) return true;
            Mob victim = interaction.getVictim();
            if (!interaction.isNPCContextCurrent() || !NPCCombatContext.validPair(SlayerNPC.this, victim)) { consumed = true; return true; }
            if (--remaining > 0) return false;
            consumed = true;
            boolean missing = victim.isPlayer() && unprotected(victim.getPlayer());
            int hit = missing ? Math.max(raw, family == Family.SPECTRE ? 120 : family == Family.BANSHEE ? 80 : 40) : raw;
            Damage damage = Damage.getDamage(SlayerNPC.this, victim, getCombatType(), hit, missing);
            damage.setMaximum(Math.max(maximum, missing ? hit : 0));
            interaction.setDamage(damage);
            if (missing) damage.onContact(() -> drainUnprotected(victim.getPlayer()));
            if (hit < 0) victim.graphics(85);
            else if (family == Family.SPECTRE && getDefinition().getEndGraphics() >= 0) victim.graphics(getDefinition().getEndGraphics());
            victim.animate(victim.getDefenceAnimation());
            victim.getDamageManager().damage(SlayerNPC.this, damage, getCombatType().getDamageType());
            victim.retaliate(SlayerNPC.this);
            return true;
        }
    }
}
