package org.dementhium.model.combat.impl.npc.npcspecs;

import org.dementhium.model.Projectile;
import org.dementhium.model.combat.CombatAction;
import org.dementhium.model.combat.CombatType;
import org.dementhium.model.combat.Damage;
import org.dementhium.model.combat.MagicFormulae;
import org.dementhium.model.mask.Animation;
import org.dementhium.model.mask.Graphic;
import org.dementhium.model.misc.ProjectileManager;
import org.dementhium.model.misc.DamageManager.DamageType;
import org.dementhium.model.player.Player;

/**
 * Handles the dradfowl's special move.
 * @author Emperor
 *
 */
public class DreadfowlStrike extends CombatAction {

	/**
	 * The attack animation.
	 */
	private static final Animation ANIMATION = Animation.create(7810);

	/**
	 * The graphic.
	 */
	private static final Graphic GRAPHIC = Graphic.create(1523);

	/**
	 * The projectile to send.
	 */
	private static final Projectile PROJECTILE = Projectile.create(null, null, 1318, 30, 32, 52, 74, 3, 1 << 6);

	/**
	 * Constructs a new {@code DreadfowlStrike} {@code Object}.
	 */
	public DreadfowlStrike() {
		super(false);
	}

	@Override
	public boolean commenceSession() {
		interaction.getSource().getCombatExecutor().setTicks(4);
		if (interaction.getSource().getAttribute("specialMove", false)) {
			interaction.getSource().setAttribute("specialMove", false);
			Player owner = interaction.getSource().getFamiliar().getOwner();
			if (!owner.getInventory().contains(12445)) {
				owner.sendMessage("You do not have enough scrolls left to do this special move.");
				return false;
			} else if (interaction.getSource().getFamiliar().getSpecialPoints() < interaction.getSource().getFamiliar().getSpecialCost()) {
				owner.sendMessage("Your familiar does not have enough special move points left.");
				return false;
			}
			interaction.getSource().getFamiliar().updateSpecialPoints(interaction.getSource().getFamiliar().getSpecialCost());
			owner.getInventory().deleteItem(12445, 1);
		}
		interaction.setDamage(Damage.getDamage(interaction.getSource(), 
				interaction.getVictim(), CombatType.MAGIC, 
				MagicFormulae.getDamage(interaction.getSource().getNPC(), 
						interaction.getVictim(), 1.5, 0.77, 1.0)));
		interaction.getDamage().setMaximum((int) MagicFormulae.getMaximumMagicDamage(interaction.getSource().getNPC(), 0.77));
		ProjectileManager.sendProjectile(PROJECTILE.transform(interaction.getSource(), interaction.getVictim(), true, 46, 10));
		interaction.getSource().animate(ANIMATION);
		interaction.getSource().graphics(GRAPHIC);
		interaction.setTicks((int) Math.floor(interaction.getSource().getLocation().distance(interaction.getVictim().getLocation()) * 0.5));
		return true;
	}

	@Override
	public boolean executeSession() {
		interaction.setTicks(interaction.getTicks() - 1);
		if (interaction.getTicks() < 2) {
			if (interaction.isDeflected()) {
				interaction.getVictim().graphics(2228);
			}
			interaction.getVictim().animate(interaction.isDeflected() ? 12573 : interaction.getVictim().getDefenceAnimation());
		}
		return interaction.getTicks() < 1;
	}

	@Override
	public boolean endSession() {
		if (interaction.getDamage().getHit() > -1) {
			interaction.getVictim().getDamageManager().damage(
					interaction.getSource(), interaction.getDamage(), DamageType.MAGE);
		} else {
			interaction.getVictim().graphics(85, 96 << 16);
		}



		interaction.getVictim().retaliate(interaction.getSource());
		return true;
	}

	@Override
	public CombatType getCombatType() {
		return CombatType.MAGIC;
	}

}