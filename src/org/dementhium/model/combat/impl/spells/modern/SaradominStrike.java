package org.dementhium.model.combat.impl.spells.modern;

import org.dementhium.model.Item;

/**
 * Handles the Saradomin Strike spell.
 */
public class SaradominStrike extends GodSpell {

	@Override
	protected int getEndGraphicId() {
		return 76;
	}

	@Override
	public int getAutocastConfig() {
		return 41;
	}

	@Override
	public Item[] getRequiredRunes() {
		return new Item[] { new Item(556, 4), new Item(554, 1), new Item(565, 2) };
	}

	@Override
	public int getRequiredStaff() {
		return 2415;
	}

	@Override
	public String getRequiredStaffName() {
		return "a Saradomin staff";
	}
}
