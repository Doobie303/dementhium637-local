package org.dementhium.model.npc.impl;
import org.dementhium.model.npc.encounter.EncounterNPC;
public class DagannothKing extends EncounterNPC {
 public DagannothKing(int id){super(id);if(id<2881||id>2883)throw new IllegalArgumentException("King");bindArena(2897,4431,2937,4467,0);}
 @Override public int getAttackDelay(){return 4;}
 @Override public int aggression(){return getId()==2883?5:7;}
}