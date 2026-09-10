package org.dementhium.model.combat.impl.specs;

import org.dementhium.model.SpecialAttack;
import org.dementhium.model.combat.CombatType;
import org.dementhium.model.combat.Damage;
import org.dementhium.model.combat.Interaction;
import org.dementhium.model.combat.MeleeFormulae;
import org.dementhium.model.player.Equipment;

/**
 * Executes the Abyssal whip's special attack: Energy drain.
 * @author Emperor
 *
 */
public class EnergyDrain extends SpecialAttack {

	/**
	 * The special attack animation used.
	 */
	private static final short ANIMATION = 11971;
	
	/**
	 * The graphics id.
	 */
	private static final short GRAPHICS = 2108;
	
	@Override
	public boolean commenceSpecialAttack(Interaction interaction) {
		interaction.setDamage(Damage.getDamage(interaction.getSource(), 
				interaction.getVictim(), CombatType.MELEE, 
				MeleeFormulae.getDamage(interaction.getSource(), 
						interaction.getVictim(), 1.2, 1.0, 1.0)));
		interaction.getDamage().setMaximum(MeleeFormulae.getMeleeDamage(interaction.getSource(), 1.0));
		if (interaction.getVictim().isPlayer()) {
			interaction.setDeflected(interaction.getVictim().getPlayer().getPrayer().usingPrayer(1, 9));
		}
		
        org.dementhium.model.combat.SpecialEffects.energyDrain(interaction.getDamage(),interaction.getSource(),interaction.getVictim());

		interaction.getSource().animate(ANIMATION);
		interaction.getVictim().animate(interaction.isDeflected() ? 12573 : interaction.getVictim().getDefenceAnimation());
		if (interaction.isDeflected()) {
			interaction.getVictim().graphics(2230);
		}
		interaction.getVictim().graphics(GRAPHICS, 96 << 16);
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
		return 4;
	}

}