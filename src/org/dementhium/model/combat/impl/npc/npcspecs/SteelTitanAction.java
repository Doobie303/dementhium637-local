package org.dementhium.model.combat.impl.npc.npcspecs;

import java.util.ArrayList;
import java.util.List;

import org.dementhium.model.Projectile;
import org.dementhium.model.combat.CombatAction;
import org.dementhium.model.combat.CombatType;
import org.dementhium.model.combat.Damage;
import org.dementhium.model.combat.ExtraTarget;
import org.dementhium.model.combat.MagicFormulae;
import org.dementhium.model.combat.MeleeFormulae;
import org.dementhium.model.combat.RangeFormulae;
import org.dementhium.model.mask.Animation;
import org.dementhium.model.mask.Graphic;
import org.dementhium.model.misc.DamageManager.DamageType;
import org.dementhium.model.misc.ProjectileManager;
import org.dementhium.model.player.Player;

/**
 * Handles a steel titan's combat action.
 * @author Emperor
 *
 */
public class SteelTitanAction extends CombatAction {

	/**
	 * Represents the attacks.
	 * @author Emperor
	 *
	 */
	private static enum Attack {
		
		/**
		 * The melee attack.
		 */
		MELEE(Animation.create(8183), Graphic.create(-1), null, Graphic.create(1446)),
		
		/**
		 * The range attack.
		 */
		RANGE(Animation.create(8190), Graphic.create(1444), Projectile.create(null, null, 1445, 72, 36, 46, 1, 5, 1), Graphic.create(1448)),

		/**
		 * The magic attack.
		 */
		MAGIC(Animation.create(7694), Graphic.create(1451), Projectile.create(null, null, 1453, 64, 36, 56, 1, 5, 1), Graphic.create(1455)),

		/**
		 * The special attack.
		 */
		SPECIAL(Animation.create(8196), Graphic.create(-1), null, Graphic.create(1449));
		
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
	 * Constructs a new {@code SteelTitanAction} {@code Object}.
	 */
	public SteelTitanAction() {
		super(false);
	}

	@Override
	public boolean commenceSession() {
		interaction.getSource().getCombatExecutor().setTicks(4);
		if (interaction.getSource().getAttribute("specialMove", false)) {
			interaction.getSource().setAttribute("specialMove", false);
			Player owner = interaction.getSource().getFamiliar().getOwner();
			if (!owner.getInventory().contains(12825)) {
				owner.sendMessage("You do not have enough scrolls left to do this special move.");
				return false;
			} else if (interaction.getSource().getFamiliar().getSpecialPoints() < interaction.getSource().getFamiliar().getSpecialCost()) {
				owner.sendMessage("Your familiar does not have enough special move points left.");
				return false;
			}
			interaction.getSource().getFamiliar().updateSpecialPoints(interaction.getSource().getFamiliar().getSpecialCost());
			owner.getInventory().deleteItem(12825, 1);
			attack = Attack.SPECIAL;
			type = CombatType.values()[interaction.getSource().getRandom().nextInt(2)];
		}
		if (interaction.getVictim().isPlayer()) {
			interaction.setDeflected(interaction.getVictim().getPlayer().getPrayer().usingPrayer(1, type.getDeflectCurse()));
		}
		int currentHit = 0;
		if (attack == Attack.MELEE) {
			currentHit = MeleeFormulae.getDamage(interaction.getSource(), interaction.getVictim(), 1.0, 1.0008, 1.0);
		} else if (attack == Attack.RANGE) {
			currentHit = RangeFormulae.getDamage(interaction.getSource(), interaction.getVictim());
		} else if (attack == Attack.MAGIC) {
			currentHit = MagicFormulae.getDamage(interaction.getSource().getNPC(), interaction.getVictim(), 1.0, 1.0, 1.0);
		} else if (attack == Attack.SPECIAL) {
			List<ExtraTarget> targets = new ArrayList<ExtraTarget>();
			for (int i = 0; i < 4; i++) {
				ExtraTarget t = new ExtraTarget(interaction.getVictim());
				if (attack == Attack.MELEE) {
					t.setDamage(Damage.getDamage(interaction.getSource(), t.getVictim(), type, MeleeFormulae.getDamage(interaction.getSource(), t.getVictim(), 1.0, 1.0008, 1.0)));
				} else {
					t.setDamage(Damage.getDamage(interaction.getSource(), t.getVictim(), type, RangeFormulae.getDamage(interaction.getSource(), interaction.getVictim())));
				}
				t.getDamage().setMaximum(447);
				targets.add(t);
			}
			interaction.setTargets(targets);
		}
		interaction.setDamage(Damage.getDamage(interaction.getSource(), 
				interaction.getVictim(), type, currentHit));
		interaction.getDamage().setMaximum(attack == Attack.MAGIC ? 449 : 372);
		if (attack.projectile != null) {
			ProjectileManager.sendProjectile(attack.projectile.transform(interaction.getSource(), interaction.getVictim()));
		}
		interaction.getSource().animate(attack.anim);
		interaction.getSource().graphics(attack.start);
        int ticks = attack.projectile != null ? (int) Math.floor(attack.projectile.getSourceLocation().distance(interaction.getVictim().getLocation()) * (attack == Attack.MAGIC ? 0.5 : 0.3))
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
		if (type != CombatType.MAGIC) {
			interaction.getVictim().graphics(attack.end);
		} else if (!interaction.getSource().getAttribute("delayedMagic", false)) {
			interaction.getVictim().graphics(attack.end);
			interaction.getSource().setAttribute("delayedMagic", true);
			return false;
		}
		interaction.getSource().setAttribute("delayedMagic", false);
		if (interaction.getTargets() == null) {
			ExtraTarget victim = new ExtraTarget(interaction.getVictim());
			victim.setDamage(interaction.getDamage());
			List<ExtraTarget> target = new ArrayList<ExtraTarget>();
			target.add(victim);
			interaction.setTargets(target);
		}
		for (ExtraTarget e : interaction.getTargets()) {
			if (e.getDamage().getHit() > -1) {
				e.getVictim().getDamageManager().damage(
						interaction.getSource(), e.getDamage(), type.getDamageType());
			} else {
				e.getVictim().graphics(85, 96 << 16);
			}
			if (e.getDamage().getVenged() > 0) {
				e.getVictim().submitVengeance(interaction.getSource(), e.getDamage().getVenged());
			}
			if (e.getDamage().getDeflected() > 0) {
				interaction.getSource().getDamageManager().damage(e.getVictim(), 
						e.getDamage().getDeflected(), 
						e.getDamage().getDeflected(), DamageType.DEFLECT);
			}
			if (e.getDamage().getRecoiled() > 0) {
				interaction.getSource().getDamageManager().damage(e.getVictim(), 
						e.getDamage().getRecoiled(), 
						e.getDamage().getRecoiled(), DamageType.DEFLECT);
			}
		}
		interaction.getVictim().retaliate(interaction.getSource());
		type = CombatType.values()[interaction.getSource().getRandom().nextInt(2)];
		attack = Attack.values()[type.ordinal()];
		if (interaction.getSource().getRandom().nextInt(10) < 2) {
			type = CombatType.MAGIC;
			attack = Attack.MAGIC;
		}
		return true;
	}

	@Override
	public CombatType getCombatType() {
		return type;
	}

}