package org.dementhium.model.npc.impl;

import org.dementhium.model.combat.CombatAction;
import org.dementhium.model.combat.impl.npc.TzTokJadAction;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.World;
import org.dementhium.model.Mob;
import org.dementhium.model.combat.Damage;
import org.dementhium.model.combat.CombatUtils;
import org.dementhium.model.combat.CombatType;

/**
 * Represents the TzTok-Jad.
 * @author Wildking72
 *
 */
public class TzTokJad extends NPC {


	/**
	 * The combat action used.
	 */
	private final CombatAction combatAction = new TzTokJadAction(this);
	
	/**
	 * Constructs a new {@code TzTokJad} {@code Object}.
	 * @param id The NPC id.
	 */
	public TzTokJad(int id) {
		super(id);
	}
	
	@Override
	public CombatAction getCombatAction() {
		return combatAction;
	}

}