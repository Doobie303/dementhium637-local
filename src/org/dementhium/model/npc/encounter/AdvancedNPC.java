package org.dementhium.model.npc.encounter;

import org.dementhium.model.*;
import org.dementhium.model.combat.*;

/** Encounter-owned selection for the remaining ordinary-world boss controllers. */
public abstract class AdvancedNPC extends EncounterNPC {
    private AdvancedAttack.Kind selected;
    public AdvancedNPC(int id){super(id);}
    public AdvancedAttack.Kind selectAttack(){if(selected==null)selected=AdvancedAttack.choose(this,getCombatExecutor().getVictim());return selected;}
    public AdvancedAttack.Kind takeAdvancedAttack(){AdvancedAttack.Kind value=selectAttack();selected=null;return value;}
    public void clearSelection(){selected=null;}
    @Override public CombatAction getCombatAction(){return new AdvancedAttack(this);}
    @Override public void resetCombatState(){super.resetCombatState();selected=null;}
    @Override public int respawnDelay(){return Math.max(1,getDefinition().getRespawn())+getDeathTick();}
}
