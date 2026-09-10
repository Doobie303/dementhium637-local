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
 * Dragon dagger - Puncture<p>
 * 2 very quick strikes with increased attack and strength.<p>
 * Extra: The dragon dagger is the most common player killing weapon in the members <br>
 * version of RuneScape as it hits twice. <br>
 * This can easily kill an unaware player as it can deal well over, <br>
 * for example, 800 damage (hitting two 400s) using the special attack,<br>
 * along with a high enough Strength level and the proper prayers and/or potions. <p>
 * It is often used to "finish" an opponent off when its health is low. <br>
 * To equip the dagger requires the completion of Lost City. <br>
 * It can be poisoned to a dragon dagger (p), a dragon dagger (p+), 
 * or a dragon dagger (p++)
 * 
 * @author Emperor
 *
 */
public class Puncture extends SpecialAttack {

	/**
	 * The animation the player has to perform.
	 */
	private static final short ANIMATION = 1062;

	/**
	 * The graphics the player should cast when using the special.
	 */
	private static final short GRAPHICS = 252;
		
	@Override
	public boolean commenceSpecialAttack(Interaction interaction) {
		interaction.setDamage(Damage.getDamage(interaction.getSource(), 
				interaction.getVictim(), CombatType.MELEE, 
				MeleeFormulae.getDamage(interaction.getSource(), 
						interaction.getVictim(), 1.15, 1.15, 1)));
		interaction.getDamage().setMaximum(MeleeFormulae.getMeleeDamage(interaction.getSource(), 1.15));
		Damage secondHit = Damage.getDamage(interaction.getSource(), 
				interaction.getVictim(), CombatType.MELEE, 
				MeleeFormulae.getDamage(interaction.getSource(), 
						interaction.getVictim(), 1.15, 1.15, 1));
		secondHit.setMaximum(interaction.getDamage().getMaximum());
		org.dementhium.model.combat.SpecialHits.awardOnImpact(interaction.getSource().getPlayer(), 
				secondHit, DamageType.MELEE);
		interaction.setSecondaryDamage(secondHit);
		if (interaction.getVictim().isPlayer()) {
			interaction.setDeflected(interaction.getVictim().getPlayer().getPrayer().usingPrayer(1, 9));
		}
		if ((interaction.getSource().isPlayer() && interaction.getDamage().getHit() > 0 || secondHit.getHit() > 0)
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
	public boolean endSpecialAttack(final Interaction interaction) {
org.dementhium.model.combat.SpecialHits.apply(interaction, interaction.getDamage(), DamageType.MELEE, 0);
        org.dementhium.model.combat.SpecialHits.apply(interaction, interaction.getSecondaryDamage(), DamageType.MELEE, 1);
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
		return 4;
	}

}
