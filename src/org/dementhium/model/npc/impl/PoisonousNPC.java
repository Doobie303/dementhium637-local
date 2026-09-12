package org.dementhium.model.npc.impl;

import org.dementhium.model.combat.CombatStatus;
import org.dementhium.model.combat.Interaction;
import org.dementhium.model.npc.NPC;

/** Explicit poison-species registry entries; conservative 40 LP, one-in-four contact rate. */
public final class PoisonousNPC extends NPC {
    public PoisonousNPC(int id){super(id);}
    @Override public void preCombatTick(Interaction interaction){
        super.preCombatTick(interaction);
        if(interaction.getDamage()!=null && getRandom().nextInt(4)==0)
            CombatStatus.poisonOnImpact(interaction.getDamage(),this,interaction.getVictim(),40);
    }
}
