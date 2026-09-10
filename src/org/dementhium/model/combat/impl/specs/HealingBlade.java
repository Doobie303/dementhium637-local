package org.dementhium.model.combat.impl.specs;

import org.dementhium.model.SpecialAttack;
import org.dementhium.model.combat.CombatType;
import org.dementhium.model.combat.Damage;
import org.dementhium.model.combat.Interaction;
import org.dementhium.model.combat.MeleeFormulae;
import org.dementhium.model.player.Equipment;

/**
 * Executes the Saradomin godsword special attack - Healing blade.
 * @author Emperor
 *
 */
public class HealingBlade extends SpecialAttack {

	/**
	 * The special attack animation used.
	 */
	private static final short ANIMATION = 7071;
	
	/**
	 * The graphics id.
	 */
	private static final short GRAPHICS = 2109;

	@Override
	public boolean commenceSpecialAttack(Interaction interaction) {
		interaction.setDamage(Damage.getDamage(interaction.getSource(), 
				interaction.getVictim(), CombatType.MELEE, 
				MeleeFormulae.getDamage(interaction.getSource(), 
						interaction.getVictim(), 2.0, 1.1, 1.0)));
		interaction.getDamage().setMaximum(MeleeFormulae.getMeleeDamage(interaction.getSource(), 1.1));
		if (interaction.getVictim().isPlayer()) {
			interaction.setDeflected(interaction.getVictim().getPlayer().getPrayer().usingPrayer(1, 9));
		}
		
		interaction.getDamage().onImpact(actual -> {
            interaction.getSource().getPlayer().getSkills().heal(Math.max(100, actual / 2));
            interaction.getSource().getPlayer().getSkills().restorePray(Math.max(5, actual * .025));
        });
        interaction.getSource().animate(ANIMATION);
		interaction.getSource().graphics(GRAPHICS);
		interaction.getVictim().animate(interaction.isDeflected() ? 12573 : interaction.getVictim().getDefenceAnimation());
		if (interaction.isDeflected()) {
			interaction.getVictim().graphics(2230);
		}
		return true;
	}
	
	@Override
	public CombatType getCombatType() {
		return CombatType.MELEE;
	}
	
	@Override
	public int getSpecialEnergyAmount() {
		return 500;
	}

	@Override
	public int getCooldownTicks() {
		return 6;
	}

}