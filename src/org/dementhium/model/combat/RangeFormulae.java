package org.dementhium.model.combat;

import org.dementhium.model.Mob;
import org.dementhium.model.definition.WeaponInterface;
import org.dementhium.model.player.Bonuses;
import org.dementhium.model.player.Equipment;
import org.dementhium.model.player.Player;
import org.dementhium.model.player.Skills;

/**
 * Holds the range combat related formulae.
 *
 * @author Emperor
 */
public class RangeFormulae {

    /**
     * Gets the current damage to be dealt to the victim.
     *
     * @param source The attacking mob.
     * @param victim The entity being attacked.
     * @return The amount to hit.
     */
    public static final int getDamage(Mob source, Mob victim) {
        return getDamage(source, victim, 1.0, 1.0, 1.0);
    }

    /**
     * Gets the current range damage.
     *
     * @param source             The attacking mob.
     * @param victim             The mob being attacked.
     * @param accuracyMultiplier The amount to increase the accuracy with.
     * @param hitMultiplier      The amount to increase the hit with.
     * @param defenceMultiplier  The amount to increase the defence with.
     * @return The amount to hit.
     */
    public static int getDamage(Mob source, Mob victim, double accuracyMultiplier,
                                double hitMultiplier, double defenceMultiplier) {
        double accuracy = CombatRolls.roll(source.getRandom(), getAccuracy(source, accuracyMultiplier * EquipmentEffects.multiplier(source, victim, CombatType.RANGE)));
        double defence = CombatRolls.roll(victim.getRandom(), getDefence(source, victim, defenceMultiplier));
                if (accuracy > defence) {
            return (int) CombatRolls.roll(source.getRandom(), getRangeDamage(source, hitMultiplier * EquipmentEffects.multiplier(source, victim, CombatType.RANGE)));
        }
        return 0;
    }

    /**
     * Gets the current range damage.
     *
     * @param source             The attacking mob.
     * @param victim             The mob being attacked.
     * @param accuracyMultiplier The amount to increase the accuracy with.
     * @param damage             The amount of damage.
     * @param defenceMultiplier  The amount to increase the defence with.
     * @return The amount to hit.
     */
    public static int getDamage(Mob source, Mob victim, double accuracyMultiplier,
                                int damage, double defenceMultiplier) {
        double accuracy = CombatRolls.roll(source.getRandom(), getAccuracy(source, accuracyMultiplier * EquipmentEffects.multiplier(source, victim, CombatType.RANGE)));
        double defence = CombatRolls.roll(victim.getRandom(), getDefence(source, victim, defenceMultiplier));
                if (accuracy > defence) {
            return (int) CombatRolls.roll(source.getRandom(), damage);
        }
        return 0;
    }

    /**
     * Gets the maximum range damage.
     *
     * @param source        The attacking mob.
     * @param hitMultiplier The hit multiplier.
     * @return The maximum range damage.
     */
    public static int getRangeDamage(Mob source, double hitMultiplier) {
        int stance = source.isPlayer() && source.getPlayer().getSettings().getCombatStyle() == WeaponInterface.STYLE_ACCURATE ? 3 : 0;
        int level = source.isPlayer() ? source.getPlayer().getSkills().getLevel(Skills.RANGED) : source.getNPC().getCombatLevel(org.dementhium.model.player.Skills.RANGED);
        int bonus = source.isPlayer() ? source.getPlayer().getBonuses().getBonus(Bonuses.RANGED) : source.getNPC().getDefinition().getBonuses()[12];
        double modifier = source.isPlayer() ? source.getPlayer().getPrayer().getRangeStrengthModifier() : source.getNPC().getRangeModifier();
        // Period evidence disputes the advertised 10% damage. Retain 20% pending exact calibration.
        if (source.isPlayer() && source.getPlayer().getEquipment().voidSet(2)) hitMultiplier *= 1.2;
        return CombatFormula.maximumHit(CombatFormula.effectiveLevel(level, modifier, stance, 1), bonus, hitMultiplier);
    }

    /**
     * Gets the maximum range accuracy.
     *
     * @param source             The attacking mob.
     * @param accuracyMultiplier The accuracy multiplier.
     * @return The maximum range accuracy.
     */
    public static double getAccuracy(Mob source, double accuracyMultiplier) {
        int stance = source.isPlayer() && source.getPlayer().getSettings().getCombatStyle() == WeaponInterface.STYLE_ACCURATE ? 3 : 0;
        int level = source.isPlayer() ? source.getPlayer().getSkills().getLevel(Skills.RANGED) : source.getNPC().getCombatLevel(org.dementhium.model.player.Skills.RANGED);
        int bonus = source.isPlayer() ? source.getPlayer().getBonuses().getBonus(Bonuses.RANGED_ATTACK) : source.getNPC().getDefinition().getBonuses()[4];
        double modifier = source.isPlayer() ? source.getPlayer().getPrayer().getRangeAccuracyModifier() : source.getNPC().getRangeModifier();
        int effective = CombatFormula.effectiveLevel(level, modifier, stance,
                source.isPlayer() && source.getPlayer().getEquipment().voidSet(2) ? 1.1 : 1);
        return CombatFormula.accuracyRoll(effective, bonus, accuracyMultiplier);
    }

    /**
     * Gets the maximum range defence.
     *
     * @param source            The attacking mob.
     * @param victim            The mob being attacked.
     * @param defenceMultiplier The defence multiplier.
     * @return The maximum range defence.
     */
    public static double getDefence(Mob source, Mob victim, double defenceMultiplier) {
        int bonus = victim.isPlayer() ? victim.getPlayer().getBonuses().getBonus(Bonuses.RANGED_DEFENCE)
                : victim.getNPC().getDefinition().getDefenceBonus(Bonuses.RANGED_ATTACK);
        return CombatFormula.accuracyRoll(CombatFormula.defenceLevel(victim), bonus, defenceMultiplier);
    }
}