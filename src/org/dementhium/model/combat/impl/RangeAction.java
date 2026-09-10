package org.dementhium.model.combat.impl;

import org.dementhium.model.combat.CombatAction;
import org.dementhium.model.combat.CombatType;
import org.dementhium.model.combat.CombatUtils;
import org.dementhium.model.combat.RangeFormulae;
import org.dementhium.model.definition.WeaponInterface;
import org.dementhium.model.misc.DamageManager.DamageType;
import org.dementhium.model.misc.ProjectileManager;
import org.dementhium.model.player.Equipment;

/**
 * Represents a range combat action session.
 * @author Emperor
 *
 */
public class RangeAction extends CombatAction {

	/**
	 * Constructs a new {@code RangeAction} {@code Object}.
	 */
	public RangeAction() {
		super(false);
		/*
		 * As this instance can be re-used, 
		 * we use the Interaction variable to store mob-related details.
		 */
	}

	@Override
	public boolean commenceSession() {
        interaction.setRangeData(interaction.getSource().getRangeData(interaction.getVictim()));
		interaction.getSource().getCombatExecutor().setTicks(getCooldownTicks());
		if (interaction.getRangeData() == null) {
			interaction.getSource().getCombatExecutor().reset();
			return false;
		}
		if (interaction.getRangeData().getWeaponType() == 6) {
			ChinchompaAction.getSingleton().setInteraction(interaction);
			return ChinchompaAction.getSingleton().commenceSession();
		}
		int maximum = RangeFormulae.getRangeDamage(interaction.getSource(), 1);
		interaction.getRangeData().getDamage().setMaximum(maximum);
		if (interaction.getRangeData().getDamage2() != null) {
			interaction.getRangeData().getDamage2().setMaximum(maximum);
		}
		if (interaction.getVictim().isPlayer()) {
			interaction.setDeflected(interaction.getVictim().getPlayer().getPrayer().usingPrayer(1, 8));
		}

		if (interaction.getSource().isPlayer() && !CombatUtils.consumeAmmunition(interaction.getSource().getPlayer(), interaction.getRangeData())) return false;
        ProjectileManager.sendProjectile(interaction.getRangeData().getProjectile(), interaction.getRangeData().getProjectile2());
		interaction.setTicks((int) Math.floor(getInteraction().getSource().getLocation().distance(getInteraction().getVictim().getLocation()) * 0.3));
		interaction.getSource().turnTo(getInteraction().getVictim(), false);
		interaction.getSource().animate(getInteraction().getRangeData().getAnimation());
		interaction.getSource().graphics(getInteraction().getRangeData().getGraphics());
		return true;
	}

	@Override
	public boolean executeSession() {
		if (interaction.getRangeData().getWeaponType() == 6) {
			ChinchompaAction.getSingleton().setInteraction(interaction);
			return ChinchompaAction.getSingleton().executeSession();
		}
		if (interaction.getTicks() < 2) {
			interaction.getVictim().animate(interaction.isDeflected() ? 12573 
					: interaction.getVictim().getDefenceAnimation());
			if (interaction.isDeflected()) {
				interaction.getVictim().graphics(2229);
			}
		}
		interaction.setTicks(interaction.getTicks() - 1);
		return interaction.getTicks() < 1;
	}

	@Override
	public boolean endSession() {
		if (interaction.getRangeData().getWeaponType() == 6) {
			ChinchompaAction.getSingleton().setInteraction(interaction);
			return ChinchompaAction.getSingleton().endSession();
		}
		interaction.getVictim().getDamageManager().damage(
				interaction.getSource(), interaction.getRangeData().getDamage(), DamageType.RANGE);
        if (interaction.getSource().isPlayer()) org.dementhium.model.combat.SpecialHits.awardOnImpact(interaction.getSource().getPlayer(), interaction.getRangeData().getDamage(), DamageType.RANGE);



		if (interaction.getSource().isPlayer()) {
			CombatUtils.dropArrows(interaction.getSource().getPlayer(), interaction.getVictim(), interaction.getRangeData());
		}
        if(interaction.getRangeData().getWeaponType()==2)
            org.dementhium.model.combat.SpecialHits.apply(interaction,interaction.getRangeData().getDamage2(),DamageType.RANGE,1);

		interaction.getVictim().retaliate(interaction.getSource());
		return true;
	}

	@Override
	public CombatType getCombatType() {
		return CombatType.RANGE;
	}

	@Override
	public int getCooldownTicks() {

		int ticks = interaction.getRangeData() != null && interaction.getRangeData().getWeapon() != null ? 
				interaction.getRangeData().getWeapon().getAttackSpeed()
				: interaction.getSource().getAttackDelay();
		if (interaction.getSource().isPlayer() && interaction.getSource().getPlayer().getSettings().getCombatStyle() == WeaponInterface.STYLE_RAPID) {
			ticks--;
		}
		return ticks;
	}

}
