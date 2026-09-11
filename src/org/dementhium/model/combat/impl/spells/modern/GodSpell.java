package org.dementhium.model.combat.impl.spells.modern;

import org.dementhium.model.Mob;
import org.dementhium.model.combat.Interaction;
import org.dementhium.model.combat.MagicFormulae;
import org.dementhium.model.combat.MagicSpell;
import org.dementhium.model.mask.Graphic;
import org.dementhium.model.player.Player;

/**
 * Common combat behavior for the three modern god spells.
 */
public abstract class GodSpell extends MagicSpell {

	@Override
	public boolean castSpell(Interaction interaction) {
		MagicFormulae.setDamage(interaction);
		interaction.getSource().animate(811);
		interaction.setEndGraphic(Graphic.create(getEndGraphicId(), 96 << 16));
		return true;
	}

	protected abstract int getEndGraphicId();

	@Override
	public double getExperience(Interaction interaction) {
		double xp = 35;
		if (interaction.getDamage().getHit() > 0) {
			xp += interaction.getDamage().getHit() * 0.2;
		}
		return xp;
	}

	@Override
	public int getStartDamage(Player source, Mob victim) {
		return 300;
	}

	@Override
	public int getNormalDamage() {
		return 30;
	}

	@Override
	public int getBaseDamage() {
		return 0;
	}

	@Override
	public int getRequiredLevel() {
		return 60;
	}
}
