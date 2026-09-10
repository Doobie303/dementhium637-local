package org.dementhium.model.combat;

import org.dementhium.model.Mob;
import org.dementhium.model.definition.WeaponInterface;
import org.dementhium.model.player.Bonuses;
import org.dementhium.model.player.Skills;

/**
 * Holds all the melee-related formulae.
 * 
 * @author Emperor
 */
public class MeleeFormulae {

	/**
	 * Gets the current damage to be dealt to the victim.
	 * 
	 * @param source
	 *            The attacking mob.
	 * @param victim
	 *            The mob being attacked.
	 * @return The amount to hit.
	 */
	public static final int getDamage(Mob source, Mob victim) {
		return getDamage(source, victim, 1.0, 1.0, 1.0);
	}

	/**
	 * Only used for Nex
	 */
	public static final int getDamage(Mob source, Mob victim, int damage) {
		return getDamage(source, victim, 1.0, damage, 1.0);
	}

	/**
	 * Gets the current melee damage.
	 * 
	 * @param source
	 *            The attacking mob.
	 * @param victim
	 *            The mob being attacked.
	 * @param accuracyMultiplier
	 *            The amount to increase the accuracy with.
	 * @param hitMultiplier
	 *            The amount to increase the hit with.
	 * @param defenceMultiplier
	 *            The amount to increase the defence with.
	 * @return The amount to hit.
	 */
	public static int getDamage(Mob source, Mob victim,
			double accuracyMultiplier, double hitMultiplier,
			double defenceMultiplier) {
		double accuracy = CombatRolls.roll(source.getRandom(),
				getMeleeAccuracy(source, accuracyMultiplier * EquipmentEffects.multiplier(source, victim, CombatType.MELEE)));
		double defence = CombatRolls.roll(victim.getRandom(),
				getMeleeDefence(source, victim, defenceMultiplier));
		if (accuracy > defence) {
			return (int) CombatRolls.roll(source.getRandom(),
					getMeleeDamage(source, hitMultiplier * EquipmentEffects.multiplier(source, victim, CombatType.MELEE)));
		}
		return 0;
	}

	/**
	 * Gets the current melee damage.
	 * 
	 * @param source
	 *            The attacking player.
	 * @param victim
	 *            The entity being attacked.
	 * @param accuracyMultiplier
	 *            The amount to increase the accuracy with.
	 * @param damage
	 *            The amount of maximum damage.
	 * @param defenceMultiplier
	 *            The amount to increase the defence with.
	 * @return The amount to hit.
	 */
	public static int getDamage(Mob source, Mob victim,
			double accuracyMultiplier, int damage, double defenceMultiplier) {
		double accuracy = CombatRolls.roll(source.getRandom(),
				getMeleeAccuracy(source, accuracyMultiplier * EquipmentEffects.multiplier(source, victim, CombatType.MELEE)));
		double defence = CombatRolls.roll(victim.getRandom(),
				getMeleeDefence(source, victim, defenceMultiplier));
		if (accuracy > defence) {
			return (int) CombatRolls.roll(source.getRandom(),
					damage);
		}
		return 0;
	}

	/**
	 * Gets the maximum melee damage.
	 * 
	 * @param source
	 *            The attacking mob.
	 * @param hitMultiplier
	 *            The hit multiplier.
	 * @return The maximum melee damage.
	 */
	public static int getMeleeDamage(Mob source, double hitMultiplier) {
        int stance = 0;
        if (source.isPlayer()) {
            int style = source.getPlayer().getSettings().getCombatStyle();
            stance = style == WeaponInterface.STYLE_AGGRESSIVE ? 3 : style == WeaponInterface.STYLE_CONTROLLED ? 1 : 0;
        }
        int level = source.isPlayer() ? source.getPlayer().getSkills().getLevel(Skills.STRENGTH)
                : source.getNPC().getCombatLevel(org.dementhium.model.player.Skills.STRENGTH);
        int bonus = source.isPlayer() ? source.getPlayer().getBonuses().getBonus(Bonuses.STRENGTH)
                : source.getNPC().getDefinition().getBonuses()[11];
        double modifier = source.isPlayer() ? source.getPlayer().getPrayer().getStrengthModifier()
                : source.getNPC().getStrengthModifier();
        int effective = CombatFormula.effectiveLevel(level, modifier, stance + (source.isPlayer() ? source.getPlayer().getPrayer().getTurmoilStrength() : 0),
                source.isPlayer() && source.getPlayer().getEquipment().voidSet(1) ? 1.1 : 1);
        // Pre-EoC reconstruction: damage scales with the fraction of life points missing.
        if (source.isPlayer() && source.getPlayer().getEquipment().barrowsSet(2)) {
            int missing = Math.max(0, source.getPlayer().getSkills().getMaximumLifePoints() - source.getHitPoints());
            hitMultiplier *= 1 + missing / (double) Math.max(1, source.getPlayer().getSkills().getMaximumLifePoints());
        }
        return CombatFormula.maximumHit(effective, bonus, hitMultiplier * EquipmentEffects.obsidianDamage(source));
    }

	/**
	 * Gets the maximum melee accuracy.
	 * 
	 * @param source
	 *            The attacking mob.
	 * @param accuracyMultiplier
	 *            The accuracy multiplier.
	 * @return The maximum melee accuracy.
	 */
	public static double getMeleeAccuracy(Mob source, double accuracyMultiplier) {
        int stance = 0;
        if (source.isPlayer()) {
            int style = source.getPlayer().getSettings().getCombatStyle();
            stance = style == WeaponInterface.STYLE_ACCURATE ? 3 : style == WeaponInterface.STYLE_CONTROLLED ? 1 : 0;
        }
        int level = source.isPlayer() ? source.getPlayer().getSkills().getLevel(Skills.ATTACK)
                : source.getNPC().getCombatLevel(org.dementhium.model.player.Skills.ATTACK);
        int bonus = source.isPlayer() ? source.getPlayer().getBonuses().getBonus(getBonusType(source))
                : source.getNPC().getDefinition().getBonuses()[getBonusType(source)];
        double modifier = source.isPlayer() ? source.getPlayer().getPrayer().getAttackModifier()
                : source.getNPC().getAttackModifier();
        int effective = CombatFormula.effectiveLevel(level, modifier, stance + (source.isPlayer() ? source.getPlayer().getPrayer().getTurmoilAttack() : 0),
                source.isPlayer() && source.getPlayer().getEquipment().voidSet(1) ? 1.1 : 1);
        return CombatFormula.accuracyRoll(effective, bonus, accuracyMultiplier);
    }

	/**
	 * Gets the maximum melee defence.
	 * 
	 * @param source
	 *            The attacking mob.
	 * @param victim
	 *            The mob being attacked.
	 * @param defenceMultiplier
	 *            The defence multiplier.
	 * @return The maximum melee defence.
	 */
	public static double getMeleeDefence(Mob source, Mob victim,
			double defenceMultiplier) {
        int type = getBonusType(source);
        int bonus = victim.isPlayer() ? victim.getPlayer().getBonuses().getDefence(type)
                : victim.getNPC().getDefinition().getDefenceBonus(type);
        return CombatFormula.accuracyRoll(CombatFormula.defenceLevel(victim), bonus, defenceMultiplier);
    }

	/**
	 * Gets the bonus type used.
	 * 
	 * @param source
	 *            The attacking mob.
	 * @return The bonus type.
	 */
	public static int getBonusType(Mob source) {
		if (source.isPlayer()) {
			return source.getPlayer().getSettings().getCombatType();
		}
		int type = 0;
		int bonus = 0;
		for (int i = 0; i < 3; i++) {
			if (source.getNPC().getDefinition().getBonuses()[i] > bonus) {
				bonus = source.getNPC().getDefinition().getBonuses()[i];
				type = i;
			}
		}
		return type;
	}
}
