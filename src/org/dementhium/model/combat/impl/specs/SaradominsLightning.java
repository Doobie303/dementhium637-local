package org.dementhium.model.combat.impl.specs;

import org.dementhium.model.SpecialAttack;
import org.dementhium.model.combat.CombatType;
import org.dementhium.model.combat.CombatUtils;
import org.dementhium.model.combat.Damage;
import org.dementhium.model.combat.Interaction;
import org.dementhium.model.combat.MeleeFormulae;
import org.dementhium.model.misc.DamageManager.DamageType;
import org.dementhium.model.player.Equipment;

/**
 * Executes the Saradomin sword special attack - Saradomin's lightning.
 * @author Emperor
 *
 */
public class SaradominsLightning extends SpecialAttack {

	/**
	 * The animation the player has to perform.
	 */
	private static final short ANIMATION = 7072;

	/**
	 * The graphics the player should cast when using the special.
	 */
	private static final short GRAPHICS = 1224;
	
	/**
	 * The graphics the victim casts.
	 */
	private static final short END_GRAPHICS = 1194;
	
	@Override
	public boolean commenceSpecialAttack(Interaction interaction) {
		interaction.setDamage(Damage.getDamage(interaction.getSource(), 
				interaction.getVictim(), CombatType.MELEE, 
				MeleeFormulae.getDamage(interaction.getSource(), 
						interaction.getVictim(), 1.03, 1.1, 1)));
		interaction.getDamage().setMaximum(MeleeFormulae.getMeleeDamage(interaction.getSource(), 1.1));
		if (interaction.getVictim().isPlayer()) {
			interaction.setDeflected(interaction.getVictim().getPlayer().getPrayer().usingPrayer(1, 9));
		}
		
		int second = 50 + interaction.getSource().getRandom().nextInt(101);
        Damage secondHit = Damage.getDamage(interaction.getSource(), 
				interaction.getVictim(), CombatType.MAGIC, second);
		secondHit.setMaximum(150);
		org.dementhium.model.combat.SpecialHits.awardOnImpact(interaction.getSource().getPlayer(), 
				secondHit, DamageType.MAGE);
		interaction.setSecondaryDamage(secondHit);
		interaction.getSource().animate(ANIMATION);
		interaction.getSource().graphics(GRAPHICS);
		interaction.getVictim().animate(interaction.isDeflected() ? 12573 : interaction.getVictim().getDefenceAnimation());
		if (interaction.isDeflected()) {
			interaction.getVictim().graphics(2230);
		}
		interaction.getVictim().graphics(END_GRAPHICS);
		return true;
	}

	@Override
	public boolean endSpecialAttack(Interaction interaction) {
org.dementhium.model.combat.SpecialHits.apply(interaction, interaction.getDamage(), DamageType.MELEE, 0);
        org.dementhium.model.combat.SpecialHits.apply(interaction, interaction.getSecondaryDamage(), DamageType.MAGE, 1);
        return true;
    }

	@Override
	public CombatType getCombatType() {
		return CombatType.MELEE;
	}

	@Override
	public int getSpecialEnergyAmount() {
		return 1000;
	}

	@Override
	public int getCooldownTicks() {
		return 4;
	}

}
