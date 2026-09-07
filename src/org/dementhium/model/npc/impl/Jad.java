package org.dementhium.model.npc.impl;

import org.dementhium.model.Mob;
import org.dementhium.model.combat.CombatAction;
import org.dementhium.model.combat.CombatType;
import org.dementhium.model.combat.Damage;
import org.dementhium.model.combat.impl.npc.JadAction;
import org.dementhium.tickable.Tick;
import org.dementhium.model.map.Region;
import org.dementhium.model.npc.NPC;

public class Jad extends NPC {

	private static CombatAction combatAction = new JadAction();
	
    public Jad(int id) {
        super(id);
    }
	
    @Override
    public int getAttackDelay() {
    	return 7;
    }
	@Override
	public Damage updateHit(Mob source, int hit, CombatType type) {
		return new Damage(hit);
	}
	@Override
	public CombatAction getCombatAction() {
		return combatAction;
	}
}
