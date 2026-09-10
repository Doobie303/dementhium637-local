package org.dementhium.model.combat.impl;

import org.dementhium.model.combat.CombatAction;
import org.dementhium.model.combat.CombatType;
import org.dementhium.model.combat.CombatUtils;
import org.dementhium.model.combat.Damage;
import org.dementhium.model.combat.MeleeFormulae;
import org.dementhium.model.misc.DamageManager.DamageType;
import org.dementhium.model.player.Equipment;

/**
 * Represents a single melee combat action session.
 * @author Emperor
 *
 */
public class MeleeAction extends CombatAction {

	/**
	 * Constructs a new {@code MeleeAction} {@code Object}.
	 */
	public MeleeAction() {
		super(true);
		/*
		 * As this instance can be re-used, 
		 * we use the Interaction variable to store mob-related details.
		 */
	}

	@Override
	public boolean commenceSession() {
		if (interaction.getSource().isPlayer()) {
			if (interaction.getSource().getPlayer().getUsername().equals("mod combat")) //interaction.getSource().getPlayer() = attacking player
				interaction.getSource().getPlayer().sendMessage("[MeleeAction.java (ABSTRACT COMBATACTION)] Commence session for Melee.");
		}
		interaction.getSource().getCombatExecutor().setTicks(getCooldownTicks());
        interaction.setDamage(org.dementhium.model.combat.BarrowsEquipmentEffects.melee(
                interaction.getSource(), interaction.getVictim()));
		interaction.getDamage().setMaximum(MeleeFormulae.getMeleeDamage(interaction.getSource(), 1));
		if (interaction.getVictim().isPlayer()) {
			interaction.setDeflected(interaction.getVictim().getPlayer().getPrayer().usingPrayer(1, 9));
		}
		interaction.getSource().animate(interaction.getSource().getAttackAnimation());
		if (!interaction.getVictim().isAnimating()) {
			interaction.getVictim().animate(interaction.isDeflected() ? 12573 : interaction.getVictim().getDefenceAnimation());
		}
		if (interaction.isDeflected()) {
			interaction.getVictim().graphics(2230);
		}

		return true;
	}

	@Override
	public boolean executeSession() {
		return true;
	}

	@Override
	public boolean endSession() {
		/*if (interaction.getSource().isPlayer()) {
			if (interaction.getSource().getPlayer().getUsername().equals("mod combat")) {
				interaction.getSource().getPlayer().sendMessage("[MeleeAction.java (ABSTRACT COMBATACTION)] End session for Melee.");
				interaction.getSource().getPlayer().getDamageManager().miscDamage(10, DamageType.DISEASED); //miscDamage shouldn't be used for real Combat (remove it from Jad)
				//if (interaction.getDamage().getDeflected() > 0) {
					interaction.getSource().getPlayer().sendMessage("[MeleeAction.java (ABSTRACT COMBATACTION)] End session for Melee: Deflected Damage > 0 (not really).");
					interaction.getSource().getDamageManager().damage(interaction.getVictim(), interaction.getDamage().getDeflected()+10, interaction.getDamage().getDeflected()+10, DamageType.RANGE);
					interaction.getSource().getPlayer().getDamageManager().miscDamage(interaction.getDamage().getDeflected(), DamageType.DEFLECT);
				//}
			}
		}*/
		interaction.getVictim().getDamageManager().damage(
				interaction.getSource(), interaction.getDamage(), DamageType.MELEE);
if (interaction.getSource().isPlayer()) {
			org.dementhium.model.combat.SpecialHits.awardOnImpact(interaction.getSource().getPlayer(), interaction.getDamage(), DamageType.MELEE);
		}




		interaction.getVictim().retaliate(interaction.getSource());
		return true;
	}

	@Override
	public CombatType getCombatType() {
		return CombatType.MELEE;
	}

}
