package org.dementhium.model.combat.impl.spells.modern;

import org.dementhium.model.Item;
import org.dementhium.model.Mob;
import org.dementhium.model.Projectile;
import org.dementhium.model.World;
import org.dementhium.model.combat.Interaction;
import org.dementhium.model.combat.MagicFormulae;
import org.dementhium.model.combat.MagicSpell;
import org.dementhium.model.mask.Graphic;
import org.dementhium.model.misc.ProjectileManager;
import org.dementhium.model.player.Player;

/**
 * Handles the TeleBlock magic spell.
 * @author Grumpy.
 *
 */
public class Teleblock extends MagicSpell {

	@Override
	public boolean castSpell(Interaction interaction) {
		MagicFormulae.setDamage(interaction);
		interaction.getSource().turnTo(interaction.getVictim(), false);
		int speed = (int) (46 + interaction.getSource().getLocation().getDistance(interaction.getVictim().getLocation()) * 10);
		ProjectileManager.sendProjectile(Projectile.create(interaction.getSource(), interaction.getVictim(), 1842, 22, 32, 60, speed, 0, 90));
		interaction.getSource().animate(10503);
		interaction.getSource().graphics(1841, 0);
		interaction.setEndGraphic(Graphic.create(1843, 96 << 30));
		if (interaction.getDamage().getHit() > -1 && interaction.getVictim().getAttribute("teleblockImmunity", -1) < World.getTicks()) {
			interaction.getVictim().setAttribute("teleblock", World.getTicks() + 550);
			interaction.getVictim().setAttribute("teleblockImmunity", World.getTicks() + 450);
		}
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
		return 0 + getBaseDamage();
	}

	@Override
	public int getNormalDamage() {
		return 0;
	}

	@Override
	public int getBaseDamage() {
		return 0;
	}

	@Override
	public Item[] getRequiredRunes() {
		return new Item[] { new Item(562, 1), new Item(563, 1), new Item(560, 1) };
	}

	@Override
	public int getRequiredLevel() {
		return 85;
	}

}
