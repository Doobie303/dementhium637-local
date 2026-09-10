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

	private static final int FIGHT_CAVES_HITPOINTS = 2500;

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
		setHp(FIGHT_CAVES_HITPOINTS);
	}

	@Override
	public int getMaxHp() {
		return FIGHT_CAVES_HITPOINTS;
	}

	@Override
	public int getMaximumHitPoints() {
		return FIGHT_CAVES_HITPOINTS;
	}

	@Override
	public void heal(int amount) {
		setHp(Math.min(FIGHT_CAVES_HITPOINTS, getHitPoints() + Math.max(0, amount)));
	}

	@Override
	public int getAttackDelay() {
		return 8;
	}
	
	@Override
	public CombatAction getCombatAction() {
		return combatAction;
	}

}
