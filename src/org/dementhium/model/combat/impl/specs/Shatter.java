package org.dementhium.model.combat.impl.specs;

import org.dementhium.model.SpecialAttack;
import org.dementhium.model.combat.CombatType;
import org.dementhium.model.combat.Damage;
import org.dementhium.model.combat.Interaction;
import org.dementhium.model.combat.MeleeFormulae;
import org.dementhium.model.player.Equipment;

/**
 * Dragon mace - Shatter<p>
 * 
 * Drastically increases Strength and decreases accuracy for one hit.<br>
 * Extra: Requires the completion of Heroes' Quest to equip.
 * 
 * @author Emperor
 * 
 */
public class Shatter extends SpecialAttack {

	/**
	 * The animation the player has to perform.
	 */
	private static final short ANIMATION = 1060;

	/**
	 * The graphics the player should cast when using the special.
	 */
	private static final short GRAPHICS = 251;
	
	@Override
	public boolean commenceSpecialAttack(Interaction interaction) {
		interaction.setDamage(Damage.getDamage(interaction.getSource(), 
				interaction.getVictim(), CombatType.MELEE, 
				MeleeFormulae.getDamage(interaction.getSource(), 
						interaction.getVictim(), .92, 1.3546, 1)));
		interaction.getDamage().setMaximum(MeleeFormulae.getMeleeDamage(interaction.getSource(), 1.3546));
		if (interaction.getVictim().isPlayer()) {
			interaction.setDeflected(interaction.getVictim().getPlayer().getPrayer().usingPrayer(1, 9));
		}
		if (interaction.getSource().isPlayer() && interaction.getDamage().getHit() > 0
				&& interaction.getSource().getPlayer().getEquipment().get(Equipment.SLOT_WEAPON) != null
				 && interaction.getSource().getPlayer().getEquipment().get(Equipment.SLOT_WEAPON).getDefinition().doesPoison()) {
			interaction.getVictim().getPoisonManager().poison(interaction.getSource(), 
					interaction.getSource().getPlayer().getEquipment().get(Equipment.SLOT_WEAPON).getDefinition().getPoisonAmount());
		}
		interaction.getSource().animate(ANIMATION);
		interaction.getSource().graphics(GRAPHICS, 96 << 16);
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
		return 250;
	}

	@Override
	public int getCooldownTicks() {
		return 5;
	}

}