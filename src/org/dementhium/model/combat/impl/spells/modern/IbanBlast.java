package org.dementhium.model.combat.impl.spells.modern;

import org.dementhium.model.Item;
import org.dementhium.model.Mob;
import org.dementhium.model.Projectile;
import org.dementhium.model.combat.Interaction;
import org.dementhium.model.combat.MagicFormulae;
import org.dementhium.model.combat.MagicSpell;
import org.dementhium.model.mask.Graphic;
import org.dementhium.model.misc.ProjectileManager;
import org.dementhium.model.player.Player;

/**
 * Handles the Iban Blast spell.
 */
public class IbanBlast extends MagicSpell {

	@Override
	public boolean castSpell(Interaction interaction) {
		MagicFormulae.setDamage(interaction);
		int speed = (int) (46 + interaction.getSource().getLocation().getDistance(interaction.getVictim().getLocation()) * 10);
		ProjectileManager.sendProjectile(Projectile.create(interaction.getSource(), interaction.getVictim(), 88, 40, 36, 51, speed, 5, 64));
		interaction.getSource().animate(708);
		interaction.getSource().graphics(87, 96 << 16);
		interaction.setEndGraphic(Graphic.create(89, 96 << 16));
		return true;
	}

	@Override
	public double getExperience(Interaction interaction) {
		double xp = 42.5;
		if (interaction.getDamage().getHit() > 0) {
			xp += interaction.getDamage().getHit() * 0.2;
		}
		return xp;
	}

	@Override
	public int getStartDamage(Player source, Mob victim) {
		return 250;
	}

	@Override
	public int getNormalDamage() {
		return 25;
	}

	@Override
	public int getBaseDamage() {
		return 0;
	}

	@Override
	public int getAutocastConfig() {
		return 45;
	}

	@Override
	public Item[] getRequiredRunes() {
		return new Item[] { new Item(554, 5), new Item(560, 1) };
	}

	@Override
	public int getRequiredLevel() {
		return 50;
	}

	@Override
	public int getRequiredStaff() {
		return 1409;
	}

	@Override
	public String getRequiredStaffName() {
		return "Iban's staff";
	}
}
