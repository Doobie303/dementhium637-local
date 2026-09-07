package org.dementhium.model.npc.impl;

import org.dementhium.model.Mob;
import org.dementhium.model.combat.CombatAction;
import org.dementhium.model.combat.CombatType;
import org.dementhium.model.combat.Damage;
import org.dementhium.model.combat.impl.npc.CorporealBeastAction;
import org.dementhium.model.npc.NPC;

public class CorporealBeast extends NPC {


	private static CombatAction combatAction = new CorporealBeastAction();
    public CorporealBeast(int id) {
        super(id);
    }
    @Override
    public int getAttackDelay() {
    	return 4;
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