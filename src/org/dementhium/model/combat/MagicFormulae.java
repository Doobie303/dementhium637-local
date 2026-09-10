package org.dementhium.model.combat;

import org.dementhium.model.Mob;
import org.dementhium.model.definition.WeaponInterface;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.player.Bonuses;
import org.dementhium.model.player.Equipment;
import org.dementhium.model.player.Player;
import org.dementhium.model.player.Skills;

/**
 * Holds all the magic combat related formulae.
 * 
 * @author Emperor
 */
public class MagicFormulae {

	/**
	 * Gets the current magic damage.
	 * 
	 * @param source
	 *            The attacking NPC.
	 * @param victim
	 *            The entity being attacked.
	 * @param accuracyMultiplier
	 *            The amount to increase the accuracy with.
	 * @param hitMultiplier
	 *            The amount to increase the hit with.
	 * @param defenceMultiplier
	 *            The amount to increase the defence with.
	 * @return The amount to hit.
	 */
	public static int getDamage(NPC source, Mob victim,
			double accuracyMultiplier, double hitMultiplier,
			double defenceMultiplier) {
		double accuracy = CombatRolls.roll(source.getRandom(),
				getMaximumMagicAccuracy(source, accuracyMultiplier));
		double defence = CombatRolls.roll(victim.getRandom(),
				getMaximumMagicDefence(victim, defenceMultiplier));
		if (accuracy > defence) {
			return (int) CombatRolls.roll(source.getRandom(),
					getMaximumMagicDamage(source, hitMultiplier));
		}
		return -1;
	}

	public static int getDamage(NPC source, Mob victim, int damage) {
		return getDamage(source, victim, 1.0, damage, 1.0);
	}

	/**
	 * Gets the current magic damage.
	 * 
	 * @param source
	 *            The attacking NPC.
	 * @param victim
	 *            The entity being attacked.
	 * @param accuracyMultiplier
	 *            The amount to increase the accuracy with.
	 * @param hitMultiplier
	 *            The amount to increase the hit with.
	 * @param defenceMultiplier
	 *            The amount to increase the defence with.
	 * @return The amount to hit.
	 */
	public static int getDamage(NPC source, Mob victim,
			double accuracyMultiplier, int damage, double defenceMultiplier) {
		double accuracy = CombatRolls.roll(source.getRandom(),
				getMaximumMagicAccuracy(source, accuracyMultiplier));
		double defence = CombatRolls.roll(victim.getRandom(),
				getMaximumMagicDefence(victim, defenceMultiplier));
		if (accuracy > defence) {
			return (int) CombatRolls.roll(source.getRandom(),
					damage);
		}
		return -1;
	}

	/**
	 * Gets the maximum magic accuracy.
	 * 
	 * @param source
	 *            The attacking NPC.
	 * @param accuracyMultiplier
	 *            The accuracy multiplier.
	 * @return The maximum magic accuracy.
	 */
	private static double getMaximumMagicAccuracy(NPC source,
			double accuracyMultiplier) {
        int effective = CombatFormula.effectiveLevel(source.getCombatLevel(org.dementhium.model.player.Skills.MAGIC), source.getMagicModifier(), 0, 1);
        return CombatFormula.accuracyRoll(effective, source.getDefinition().getBonuses()[Bonuses.MAGIC_ATTACK], accuracyMultiplier);
    }

	/**
	 * Gets the maximum magic defence.
	 * 
	 * @param source
	 *            The attacking NPC.
	 * @param victim
	 *            The entity being attacked.
	 * @param defenceMultiplier
	 *            The defence multiplier.
	 * @return The maximum magic defence.
	 */
	private static double getMaximumMagicDefence(Mob victim,
			double defenceMultiplier) {
        int magic = victim.isPlayer() ? victim.getPlayer().getSkills().getLevel(Skills.MAGIC)
                : victim.getNPC().getCombatLevel(org.dementhium.model.player.Skills.MAGIC);
        // Defence prayer boosts only the Defence contribution, not the whole magic-weighted level.
        double magicModifier = victim.isPlayer() ? victim.getPlayer().getPrayer().getMagicDefenceModifier()
                : victim.getNPC().getMagicModifier();
        int effective = CombatFormula.floor(Math.floor(Math.max(0, magic) * Math.max(0,1 + magicModifier)) * 0.7)
                + CombatFormula.floor(CombatFormula.defenceLevel(victim) * 0.3);
        int bonus = victim.isPlayer() ? victim.getPlayer().getBonuses().getBonus(Bonuses.MAGIC_DEFENCE)
                : victim.getNPC().getDefinition().getBonuses()[8];
        return CombatFormula.accuracyRoll(effective, bonus, defenceMultiplier);
    }

	/**
	 * Gets the maximum magic damage.
	 * 
	 * @param source
	 *            The attacking NPC.
	 * @param hitMultiplier
	 *            The hit multiplier.
	 * @return The maximum magic damage.
	 */
	public static double getMaximumMagicDamage(NPC source, double hitMultiplier) {
		int mageLvl = source.getCombatLevel(org.dementhium.model.player.Skills.MAGIC) + 1;
		int magicBonus = source.getDefinition().getBonuses()[13];
		return (14 + mageLvl + (magicBonus / 8) + ((mageLvl * magicBonus) / 64))
				* hitMultiplier;
	}

	/**
	 * Sets the current damage on the interaction.
	 * 
	 * @param interaction
	 *            The interaction.
	 */
	public static void setDamage(Interaction interaction) {
		double accuracy = CombatRolls.roll(interaction.getSource().getRandom(),
				getMaximumAccuracy(interaction.getSource().getPlayer(),
						interaction.getSpell()) * EquipmentEffects.multiplier(interaction.getSource(), interaction.getVictim(), CombatType.MAGIC));
		double defence = CombatRolls.roll(interaction.getVictim().getRandom(),
				getMaximumDefence(interaction.getSource().getPlayer(),
						interaction.getVictim(), interaction.getSpell()));
		double maximum = getMaximumDamage(interaction.getSource().getPlayer(),
				interaction.getVictim(), interaction.getSpell());
		if (accuracy > defence) {
			int hit = (int) CombatRolls.roll(interaction
					.getSource().getRandom(), maximum);
			interaction.setDamage(Damage.getDamage(interaction.getSource(),
					interaction.getVictim(), CombatType.MAGIC, hit));
		} else {
			interaction.setDamage(new Damage(-1));
		}
		interaction.getDamage().setMaximum((int) maximum);
	}

	/**
	 * Gets the current magic damage.
	 * 
	 * @param source
	 *            The attacking player.
	 * @param victim
	 *            The mob being attacked.
	 * @param spell
	 *            The spell used.
	 * @return The current damage.
	 */
	public static int getDamage(Player source, Mob victim, MagicSpell spell) {
		double accuracy = CombatRolls.roll(source.getRandom(),
				getMaximumAccuracy(source, spell) * EquipmentEffects.multiplier(source, victim, CombatType.MAGIC));
		double defence = CombatRolls.roll(victim.getRandom(),
				getMaximumDefence(source, victim, spell));
		double maximum = getMaximumDamage(source, victim, spell);
		if (accuracy > defence) {
			return (int) CombatRolls.roll(source.getRandom(),
					maximum);
		}
		return -1;
	}

	/**
	 * Gets the maximum magic accuracy.
	 * 
	 * @param source
	 *            The attacking player.
	 * @param accuracyMultiplier
	 *            The accuracy multiplier.
	 * @return The maximum magic accuracy.
	 */
	private static double getMaximumAccuracy(Player source, MagicSpell spell) {
        int effective = CombatFormula.effectiveLevel(source.getSkills().getLevel(Skills.MAGIC),
                source.getPrayer().getMagicModifier(), 0, source.getEquipment().voidSet(3) ? 1.3 : 1);
        return CombatFormula.accuracyRoll(effective, source.getBonuses().getBonus(Bonuses.MAGIC_ATTACK), 1);
    }

	/**
	 * Gets the maximum magic defence.
	 * 
	 * @param source
	 *            The attacking player.
	 * @param spellType
	 *            The spell type used.
	 * @param victim
	 *            The entity being attacked.
	 * @param defenceMultiplier
	 *            The defence multiplier.
	 * @return The maximum magic defence.
	 */
	private static double getMaximumDefence(Player source, Mob victim,
			MagicSpell spell) {
        return getMaximumMagicDefence(victim, 1);
    }

	/**
	 * Gets the maximum magic damage.
	 * 
	 * @param source
	 *            The attacking player.
	 * @param victim
	 *            The entity being attacked.
	 * @param spellType
	 *            The spell type used.
	 * @param hitMultiplier
	 *            The hit multiplier.
	 * @return The maximum magic damage.
	 */
	public static double getMaximumDamage(Player source, Mob victim,
			MagicSpell spell) {
		int damage = spell.getStartDamage(source, victim);
		double multiplier = EquipmentEffects.multiplier(source, victim, CombatType.MAGIC);
		multiplier *= (source.getBonuses().getBonus(14) * 0.01) + 1;
		if (source.getSkills().getLevel(Skills.MAGIC) > source.getSkills()
				.getLevelForExperience(Skills.MAGIC)) {
			multiplier *= 1 + ((source.getSkills().getLevel(Skills.MAGIC) - source
					.getSkills().getLevelForExperience(Skills.MAGIC)) * 0.03);
		}
		if (victim.isNPC() && victim.getNPC().getId() == 9463) {
			boolean isFireSpell = spell.getClass().getSimpleName()
					.contains("Fire");
			if (source.getEquipment().getSlot(Equipment.SLOT_CAPE) == 6570
					&& isFireSpell) {
				damage += 40;
				multiplier *= 2.0;
			} else if (isFireSpell) {
				multiplier *= 1.5;
			} else if (source.getEquipment().getSlot(Equipment.SLOT_CAPE) == 6570) {
				damage += 40;
			}
		}
		return CombatFormula.floor(damage * multiplier);
	}

	/**
	 * Gets the normal damage from the spell?
	 * 
	 * @param source
	 *            The attacking player.
	 * @param victim
	 *            The entity being attacked.
	 * @param spellType
	 *            The spell type used.
	 * @return The normal damage.
	 */
	/*
	 * private static int getNormalDamage(Player source, Entity victim,
	 * SpellType spellType) { int t = spellType.getBaseDamage(); String name =
	 * spellType.name(); if (name.endsWith("STRIKE")) { return victim instanceof
	 * Npc && ((Npc) victim).getId() == 205 ? (80 + t) : (2 * t); } else if
	 * (name.endsWith("BOLT")) { return
	 * source.getEquipment().getItem(Equipment.SLOT_HANDS).getId() == 777 ? 110
	 * + t : 80 + t; } else if (name.endsWith("BLAST")) { return 120 + t; } else
	 * if (name.endsWith("WAVE")) { return 160 + t; } else if
	 * (name.endsWith("SURGE")) { return 200 + (2 * t); } else if
	 * (name.equals("CRUMBLE_UNDEAD")) { return 150; } else if
	 * (name.equals("MAGIC_DART")) { return 100 +
	 * source.getSkills().getLevel(Skills.MAGIC); } else if
	 * (name.equals("IBAN_BLAST")) { return 250; } else if
	 * (name.endsWith("GODSPELL")) { return
	 * source.getIntegerAttribute("godSpellCharge") > /*GameEngine.getTicks()*1
	 * ? 300 : 200; /*} else if (name.endsWith("RUSH")) { return 140 + t; } else
	 * if (name.endsWith("BURST")) { return 180 + t; } else if
	 * (name.endsWith("BLITZ")) { return 220 + t; } else if
	 * (name.endsWith("BARRAGE")) { return 260 + t; } return t; }
	 */
}