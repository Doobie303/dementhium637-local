package org.dementhium.model.combat.impl.npc.npcspecs;

import org.dementhium.model.Projectile;
import org.dementhium.model.combat.CombatAction;
import org.dementhium.model.combat.CombatType;
import org.dementhium.model.combat.Damage;
import org.dementhium.model.combat.RangeFormulae;
import org.dementhium.model.mask.Animation;
import org.dementhium.model.mask.Graphic;
import org.dementhium.model.misc.ProjectileManager;
import org.dementhium.model.misc.DamageManager.DamageType;
import org.dementhium.model.player.Player;

/**
 * Handles the thorny snail's special move: slime spray.
 * @author Emperor
 *
 */
public class SlimeSprayAction extends CombatAction {

	/**
	 * The attack animation.
	 */
	private static final Animation ANIMATION = Animation.create(8148);
	
	/**
	 * The graphic.
	 */
	private static final Graphic GRAPHIC = Graphic.create(1385);
	
	/**
	 * The projectile to send.
	 */
	private static final Projectile PROJECTILE = Projectile.create(null, null, 1386, 30, 32, 46, 74, 3, 1 << 6);

	/**
	 * The graphic.
	 */
	private static final Graphic END_GRAPHIC = Graphic.create(1387);
	
	/**
	 * Constructs a new {@code SlimeSprayAction} {@code Object}.
	 */
	public SlimeSprayAction() {
		super(false);
	}

	@Override
	public boolean commenceSession() {
		interaction.getSource().getCombatExecutor().setTicks(4);
		if (interaction.getSource().getAttribute("specialMove", false)) {
			interaction.getSource().setAttribute("specialMove", false);
			Player owner = interaction.getSource().getFamiliar().getOwner();
			if (!owner.getInventory().contains(12459)) {
				owner.sendMessage("You do not have enough scrolls left to do this special move.");
				return false;
			} else if (interaction.getSource().getFamiliar().getSpecialPoints() < interaction.getSource().getFamiliar().getSpecialCost()) {
				owner.sendMessage("Your familiar does not have enough special move points left.");
				return false;
			}
			interaction.getSource().getFamiliar().updateSpecialPoints(interaction.getSource().getFamiliar().getSpecialCost());
			owner.getInventory().deleteItem(12459, 1);
		}
		interaction.setDamage(Damage.getDamage(interaction.getSource(), 
				interaction.getVictim(), CombatType.RANGE, 
				RangeFormulae.getDamage(interaction.getSource().getNPC(), 
						interaction.getVictim(), 1.5, 2.0, 1.0)));
		interaction.getDamage().setMaximum((int) RangeFormulae.getRangeDamage(interaction.getSource().getNPC(), 2.0));
		ProjectileManager.sendProjectile(PROJECTILE.transform(interaction.getSource(), interaction.getVictim()));
		interaction.getSource().animate(ANIMATION);
		interaction.getSource().graphics(GRAPHIC);
		interaction.setTicks((int) Math.floor(interaction.getSource().getLocation().distance(interaction.getVictim().getLocation()) * 0.3));
		return true;
	}

	@Override
	public boolean executeSession() {
		if (interaction.getTicks() < 2) {
			if (interaction.isDeflected()) {
				interaction.getVictim().graphics(2229);
			}
			interaction.getVictim().animate(interaction.isDeflected() ? 12573 : interaction.getVictim().getDefenceAnimation());
		}
		interaction.setTicks(interaction.getTicks() - 1);
		return interaction.getTicks() < 1;
	}

	@Override
	public boolean endSession() {
		interaction.getVictim().graphics(END_GRAPHIC);
		interaction.getVictim().getDamageManager().damage(
				interaction.getSource(), interaction.getDamage(), DamageType.RANGE);
		if (interaction.getDamage().getVenged() > 0) {
			interaction.getVictim().submitVengeance(interaction.getSource(), interaction.getDamage().getVenged());
		}
		if (interaction.getDamage().getDeflected() > 0) {
			interaction.getSource().getDamageManager().damage(interaction.getVictim(), 
					interaction.getDamage().getDeflected(), 
					interaction.getDamage().getDeflected(), DamageType.DEFLECT);
		}
		if (interaction.getDamage().getRecoiled() > 0) {
			interaction.getSource().getDamageManager().damage(interaction.getVictim(), 
					interaction.getDamage().getRecoiled(), 
					interaction.getDamage().getRecoiled(), DamageType.DEFLECT);
		}
		interaction.getVictim().retaliate(interaction.getSource());
		return true;
	}

	@Override
	public CombatType getCombatType() {
		return CombatType.RANGE;
	}

}
