package org.dementhium.model.combat.impl.specs;

import org.dementhium.model.Projectile;
import org.dementhium.model.SpecialAttack;
import org.dementhium.model.World;
import org.dementhium.model.combat.Ammunition;
import org.dementhium.model.combat.CombatType;
import org.dementhium.model.combat.CombatUtils;
import org.dementhium.model.combat.Damage;
import org.dementhium.model.combat.Interaction;
import org.dementhium.model.combat.RangeData;
import org.dementhium.model.combat.RangeFormulae;
import org.dementhium.model.combat.RangeWeapon;
import org.dementhium.model.misc.DamageManager.DamageType;
import org.dementhium.model.misc.ProjectileManager;
import org.dementhium.model.player.Equipment;
import org.dementhium.tickable.Tick;

/**
 * Executes the Guthix bow's Balanced shot special attack.
 * @author Emperor
 *
 */
public class BalancedShot extends SpecialAttack {

	/**
	 * The end graphics.
	 */
	private static final short END_GRAPHICS = 127;

	@Override
	public boolean commenceSpecialAttack(Interaction interaction) {
		RangeData data = new RangeData(true);
		data.setWeapon(RangeWeapon.get(interaction.getSource().getPlayer().getEquipment().getSlot(3)));
		data.setAmmo(Ammunition.get(interaction.getSource().getPlayer().getEquipment().getSlot(13)));
        if (data.getWeapon() == null || data.getAmmo() == null) return false;
        int ammoSlot = data.getWeapon().getAmmunitionSlot();
        if (ammoSlot >= 0 && (interaction.getSource().getPlayer().getEquipment().get(ammoSlot) == null
                || interaction.getSource().getPlayer().getEquipment().get(ammoSlot).getAmount() < 1)) return false;
		if (data.getAmmo() == null || !data.getWeapon().getAmmunition().contains(data.getAmmo().getItemId())) {
			interaction.getSource().getPlayer().sendMessage("You do not have enough ammo left.");
			interaction.getSource().getCombatExecutor().reset();
			return false;
		}
		if (interaction.getVictim().isPlayer()) {
			interaction.setDeflected(interaction.getVictim().getPlayer().getPrayer().usingPrayer(1, 8));
		}

		int maximum = RangeFormulae.getRangeDamage(interaction.getSource(), 1.5);
		data.setDamage(Damage.getDamage(interaction.getSource(), interaction.getVictim(), CombatType.RANGE,RangeFormulae.getDamage(interaction.getSource(), interaction.getVictim(), 1.2, 1.5, 1.0)));
		data.getDamage().setMaximum(maximum);
		int speed = (int) (46 + (interaction.getSource().getLocation().distance(interaction.getVictim().getLocation()) * 5));
		ProjectileManager.sendProjectile(Projectile.create(interaction.getSource(), interaction.getVictim(), data.getAmmo().getProjectileId(), 40, 36, 41, speed, 5, 11));
		interaction.setTicks((int) Math.floor(interaction.getSource().getLocation().distance(interaction.getVictim().getLocation()) * 0.3));
		interaction.getSource().animate(data.getWeapon().getAnimationId());
		interaction.getSource().graphics(data.getAmmo().getStartGraphics());
		data.setDropAmmo(data.getAmmo().getItemId() != 4740 && data.getAmmo().getItemId() != 15243);
        interaction.setRangeData(data);
        CombatUtils.dropArrows(interaction.getSource().getPlayer(), interaction.getVictim(), data);

		return true;
	}

	@Override
	public boolean tick(Interaction interaction) {
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
        final Damage owned=interaction.getRangeData().getDamage();
        if(owned==null || owned.isResolved())return true;

		interaction.getVictim().graphics(END_GRAPHICS);
		interaction.getVictim().getDamageManager().damage(
				interaction.getSource(), interaction.getRangeData().getDamage(), 
				DamageType.RANGE);



        if(owned.isResolved() && owned.getHit()>0)
            org.dementhium.model.combat.SpecialEffects.healing(interaction,owned,owned.getHit()/15,10,10);
		interaction.getVictim().retaliate(interaction.getSource());
		return true;
	}

	@Override
	public CombatType getCombatType() {
		return CombatType.RANGE;
	}

	@Override
	public int getSpecialEnergyAmount() {
		return 550;
	}

	@Override
	public int getCooldownTicks() {
		return 6;
	}

}