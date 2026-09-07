package org.dementhium.model.combat.impl.npc.npcspecs;

import org.dementhium.model.Projectile;
import org.dementhium.model.combat.CombatAction;
import org.dementhium.model.combat.CombatMovement;
import org.dementhium.model.combat.CombatType;
import org.dementhium.model.combat.Damage;
import org.dementhium.model.combat.MeleeFormulae;
import org.dementhium.model.combat.RangeFormulae;
import org.dementhium.model.mask.Animation;
import org.dementhium.model.mask.Graphic;
import org.dementhium.model.misc.DamageManager.DamageType;
import org.dementhium.model.misc.ProjectileManager;
import org.dementhium.model.player.Player;

/**
 * Handles the Geyser titan's combat.
 * @author Emperor
 *
 */
public class GeyserTitanAction extends CombatAction {
	
	/**
	 * Represents the attacks.
	 * @author Emperor
	 *
	 */
	private static enum Attack {
		
		/**
		 * The melee attack.
		 */
		MELEE(Animation.create(7879), Graphic.create(-1), null, Graphic.create(-1)),
		
		/**
		 * The range attack.
		 */
		RANGE(Animation.create(7883), Graphic.create(1375), Projectile.create(null, null, 1374, 46, 36, 46, 1, 5, 1), Graphic.create(1377)),

		/**
		 * The special attack.
		 */
		SPECIAL(Animation.create(7883), Graphic.create(1373), Projectile.create(null, null, 1376, 72, 46, 46, 1, 5, 1), Graphic.create(1377));
		
		/**
		 * The attack animation.
		 */
		private final Animation anim;
		
		/**
		 * The start graphic.
		 */
		private final Graphic start;
		
		/**
		 * The projectile to send.
		 */
		private final Projectile projectile;
		
		/**
		 * The end graphic.
		 */
		private final Graphic end;
		
		/**
		 * Constructs a new {@code Attack} {@code Object}.
		 * @param anim The attack animation.
		 * @param start The start graphic.
		 * @param projectile The projectile.
		 * @param end The end graphic.
		 */
		private Attack(Animation anim, Graphic start, Projectile projectile, Graphic end) {
			this.anim = anim;
			this.start = start;
			this.projectile = projectile;
			this.end = end;
		}
	}
	
	/**
	 * The current combat type.
	 */
	private CombatType type = CombatType.RANGE;
	
	/**
	 * The current attack.
	 */
	private Attack attack = Attack.RANGE;
	
	/**
	 * Constructs a new {@code GeyserTitanAction} {@code Object}.
	 */
	public GeyserTitanAction() {
		super(false);
	}

	@Override
	public boolean commenceSession() {
		attack = Attack.RANGE;
		type = CombatType.RANGE;
		if (CombatMovement.canMelee(interaction.getSource(), interaction.getVictim())) {
			attack = Attack.MELEE;
			type = CombatType.MELEE;
		}
		interaction.getSource().getCombatExecutor().setTicks(4);
		if (interaction.getSource().getAttribute("specialMove", false)) {
			interaction.getSource().setAttribute("specialMove", false);
			Player owner = interaction.getSource().getFamiliar().getOwner();
			if (!owner.getInventory().contains(12833)) {
				owner.sendMessage("You do not have enough scrolls left to do this special move.");
				return false;
			} else if (interaction.getSource().getFamiliar().getSpecialPoints() < interaction.getSource().getFamiliar().getSpecialCost()) {
				owner.sendMessage("Your familiar does not have enough special move points left.");
				return false;
			}
			interaction.getSource().getFamiliar().updateSpecialPoints(interaction.getSource().getFamiliar().getSpecialCost());
			owner.getInventory().deleteItem(12833, 1);
			attack = Attack.SPECIAL;
			type = CombatType.values()[interaction.getSource().getRandom().nextInt(2)];
		}
		int currentHit = 0;
		if (attack == Attack.MELEE) {
			currentHit = MeleeFormulae.getDamage(interaction.getSource(), interaction.getVictim());
		} else if (attack == Attack.RANGE) {
			currentHit = RangeFormulae.getDamage(interaction.getSource(), interaction.getVictim());
		} else if (attack == Attack.SPECIAL) {
			interaction.getSource().getCombatExecutor().setTicks(0); //Seems to double hit on rs.
			currentHit = RangeFormulae.getDamage(interaction.getSource(), interaction.getVictim());
			double accuracy = RangeFormulae.getAccuracy(interaction.getSource(), 1.0);
			double defence = RangeFormulae.getDefence(interaction.getSource(), interaction.getVictim(), 1.0);
			double mod = defence / accuracy;
			if (mod > 1.4) {
				mod = 1.4;
			} else if (mod < 0.6) {
				mod = 0.6;
			}
			currentHit *= mod;
		}
		interaction.setDamage(Damage.getDamage(interaction.getSource(), 
				interaction.getVictim(), type, currentHit));
		interaction.getDamage().setMaximum(attack == Attack.SPECIAL ? 300 : 215);
		if (attack.projectile != null) {
			ProjectileManager.sendProjectile(attack.projectile.transform(interaction.getSource(), interaction.getVictim()));
		}
		interaction.getSource().animate(attack.anim);
		interaction.getSource().graphics(attack.start);
        int ticks = attack.projectile != null ? (int) Math.floor(attack.projectile.getSourceLocation().distance(interaction.getVictim().getLocation()) * 0.3)
        		: 1;
		interaction.setTicks(ticks);
		return true;
	}

	@Override
	public boolean executeSession() {
		if (interaction.getTicks() < 2) {
			if (interaction.isDeflected()) {
				interaction.getVictim().graphics(2230 - type.ordinal());
			}
			interaction.getVictim().animate(interaction.isDeflected() ? 12573 : interaction.getVictim().getDefenceAnimation());
		}
		interaction.setTicks(interaction.getTicks() - 1);
		return interaction.getTicks() < 1;
	}

	@Override
	public boolean endSession() {
		if (type != CombatType.MELEE) {
			interaction.getVictim().graphics(attack.end);
		}
		interaction.getVictim().getDamageManager().damage(
				interaction.getSource(), interaction.getDamage(), type.getDamageType());
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
