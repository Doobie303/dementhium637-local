package org.dementhium.model.npc.impl;
import org.dementhium.model.*;
import org.dementhium.model.combat.*;
import org.dementhium.model.npc.encounter.AdvancedNPC;
/** Early-2011 melee, magic and dragonfire; later recoil orb/ranged are deliberately excluded. */
public class FrostDragon extends AdvancedNPC {
    public FrostDragon(int id){super(id);bindArena(1280,4480,1343,4543,0);}
    @Override public int getAttackDelay(){return 4;}
    @Override public Damage updateHit(Mob source,int hit,CombatType type){return type==CombatType.DRAGONFIRE?new Damage(0):super.updateHit(source,hit,type);}
}
