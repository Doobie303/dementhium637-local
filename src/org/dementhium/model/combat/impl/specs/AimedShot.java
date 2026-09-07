package org.dementhium.model.combat.impl.specs;

import org.dementhium.model.Projectile;
import org.dementhium.model.SpecialAttack;
import org.dementhium.model.combat.Ammunition;
import org.dementhium.model.combat.CombatType;
import org.dementhium.model.combat.Damage;
import org.dementhium.model.combat.Interaction;
import org.dementhium.model.combat.RangeData;
import org.dementhium.model.combat.RangeFormulae;
import org.dementhium.model.combat.RangeWeapon;
import org.dementhium.model.mask.Graphic;
import org.dementhium.model.misc.DamageManager.DamageType;
import org.dementhium.model.misc.ProjectileManager;
import org.dementhium.model.player.Equipment;

/**
 * Executes the Hand cannon's Aimed shot special attack.
 * @author Emperor
 *
 */
public class AimedShot extends SpecialAttack {
	
	/**
	 * The graphics.
	 */
	private static final Graphic GRAPHICS = Graphic.create(2141, 96 << 16);
	
	/**
	 * The special attack projectile GFX id.
	 */
	private static final short PROJECTILE_ID = 2143;
		
	@Override
	public boolean commenceSpecialAttack(Interaction interaction) {
		RangeData data = new RangeData(true);
		data.setWeapon(RangeWeapon.get(interaction.getSource().getPlayer().getEquipment().getSlot(3)));
		data.setAmmo(Ammunition.get(interaction.getSource().getPlayer().getEquipment().getSlot(13)));
		if (data.getAmmo() == null || !data.getWeapon().getAmmunition().contains(data.getAmmo().getItemId())) {
			interaction.getSource().getPlayer().sendMessage("You do not have enough ammo left.");
			interaction.getSource().getCombatExecutor().reset();
			return false;
		}
		if (interaction.getVictim().isPlayer()) {
			interaction.setDeflected(interaction.getVictim().getPlayer().getPrayer().usingPrayer(1, 8));
		}
		int maximum = RangeFormulae.getRangeDamage(interaction.getSource(), 1.0);
		data.setDamage(Damage.getDamage(interaction.getSource(), interaction.getVictim(), CombatType.RANGE,RangeFormulae.getDamage(interaction.getSource(), interaction.getVictim(), 1.49, 1.0, 0.952)));
		data.getDamage().setMaximum(maximum);
		interaction.getSource().setAttribute("aimingTicks", 3);
		interaction.setTicks((int) Math.floor(interaction.getSource().getLocation().distance(interaction.getVictim().getLocation()) * 0.3) + 3);
		interaction.getSource().getPlayer().getEquipment().deleteItem(data.getAmmo().getItemId(), 1);
		interaction.setRangeData(data);
		if (interaction.getSource().isPlayer()) {
			if ((interaction.getRangeData().getDamage() != null
					&& interaction.getRangeData().getDamage().getHit() > 0)
					|| (interaction.getRangeData().getDamage2() != null
							&& interaction.getRangeData().getDamage2().getHit() > 0)) {
				if (interaction.getSource().getPlayer().getEquipment().get(Equipment.SLOT_WEAPON) != null
						&& interaction.getSource().getPlayer().getEquipment().get(Equipment.SLOT_WEAPON).getDefinition().doesPoison()
						&& interaction.getRangeData().getWeapon().getItemId() == interaction.getSource().getPlayer().getEquipment().get(Equipment.SLOT_WEAPON).getId())
					interaction.getVictim().getPoisonManager().poison(interaction.getSource(), 
							interaction.getSource().getPlayer().getEquipment().get(Equipment.SLOT_WEAPON).getDefinition().getPoisonAmount());
				else if (interaction.getSource().getPlayer().getEquipment().get(Equipment.SLOT_ARROWS) != null
						&& interaction.getSource().getPlayer().getEquipment().get(Equipment.SLOT_WEAPON) != null
						&& interaction.getSource().getPlayer().getEquipment().get(Equipment.SLOT_ARROWS).getDefinition().doesPoison())
					interaction.getVictim().getPoisonManager().poison(interaction.getSource(), 
							interaction.getSource().getPlayer().getEquipment().get(Equipment.SLOT_ARROWS).getDefinition().getPoisonAmount());
			}
		}
		return true;
	}

	@Override
	public boolean tick(Interaction interaction) {
		int aimingTicks = interaction.getSource().getAttribute("aimingTicks");
		interaction.getSource().setAttribute("aimingTicks", aimingTicks - 1);
		if (aimingTicks < 1) {
			interaction.getSource().turnTo(interaction.getVictim(), false);
			interaction.getSource().animate(12175);
			interaction.getSource().graphics(GRAPHICS);
			int speed = (int) (32 + (interaction.getSource().getLocation().distance(interaction.getVictim().getLocation()) * 5));
			ProjectileManager.sendProjectile(Projectile.create(interaction.getSource(), interaction.getVictim(), PROJECTILE_ID, 31, 36, 45, speed, 0));
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
	public boolean endSpecialAttack(final Interaction interaction) {
		interaction.getVictim().getDamageManager().damage(
				interaction.getSource(), interaction.getRangeData().getDamage(), 
				DamageType.RANGE);
		if (interaction.getRangeData().getDamage().getVenged() > 0) {
			interaction.getVictim().submitVengeance(
					interaction.getSource(), interaction.getRangeData().getDamage().getVenged());
		}
		if (interaction.getRangeData().getDamage().getDeflected() > 0) {
			//interaction.getSource().getDamageManager().damage(interaction.getVictim(),
					//interaction.getRangeData().getDamage().getDeflected(), 
					//interaction.getRangeData().getDamage().getDeflected(), DamageType.DEFLECT);
			interaction.getSource().getDamageManager().miscDamage(interaction.getRangeData().getDamage().getDeflected(), DamageType.DEFLECT);
		}
		if (interaction.getRangeData().getDamage().getRecoiled() > 0) {
			//interaction.getSource().getDamageManager().damage(interaction.getVictim(),
					//interaction.getRangeData().getDamage().getRecoiled(), 
					//interaction.getRangeData().getDamage().getRecoiled(), DamageType.DEFLECT);
			interaction.getSource().getDamageManager().miscDamage(interaction.getRangeData().getDamage().getRecoiled(), DamageType.DEFLECT);
		}
		interaction.getVictim().retaliate(interaction.getSource());
		return true;
	}

	@Override
	public CombatType getCombatType() {
		return CombatType.RANGE;
	}
	
	@Override
	public int getSpecialEnergyAmount() {
		return 500;
	}

	@Override
	public int getCooldownTicks() {
		return 6;
	}

}