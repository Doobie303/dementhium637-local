package org.dementhium.model.combat.impl.spells.modern;

import org.dementhium.model.Item;

/**
 * Handles the Flames of Zamorak spell.
 */
public class FlamesOfZamorak extends GodSpell {

	@Override
	protected int getEndGraphicId() {
		return 78;
	}

	@Override
	public int getAutocastConfig() {
		return 43;
	}

	@Override
	public Item[] getRequiredRunes() {
		return new Item[] { new Item(556, 1), new Item(554, 4), new Item(565, 2) };
	}

	@Override
	public int getRequiredStaff() {
		return 2417;
	}

	@Override
	public String getRequiredStaffName() {
		return "a Zamorak staff";
	}
}
