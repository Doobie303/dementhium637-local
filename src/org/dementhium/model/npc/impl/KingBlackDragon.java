package org.dementhium.model.npc.impl;
import org.dementhium.model.npc.encounter.AdvancedNPC;
public class KingBlackDragon extends AdvancedNPC {
    public KingBlackDragon(int id){super(id);bindArena(2250,4675,2300,4725,0);}
    @Override public int getAttackDelay(){return 4;}
    @Override public int aggression(){return 8;}
}
