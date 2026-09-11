package org.dementhium.model.combat.impl.spells.modern;

import org.dementhium.model.Item;

/**
 * Handles the Claws of Guthix spell.
 */
public class ClawsOfGuthix extends GodSpell {

	@Override
	protected int getEndGraphicId() {
		return 77;
	}

	@Override
	public int getAutocastConfig() {
		return 39;
	}

	@Override
	public Item[] getRequiredRunes() {
		return new Item[] { new Item(556, 4), new Item(554, 1), new Item(565, 2) };
	}

	@Override
	public int getRequiredStaff() {
		return 2416;
	}

	@Override
	public String getRequiredStaffName() {
		return "a Guthix staff";
	}
}
