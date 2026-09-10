package org.dementhium.model.npc;
import org.dementhium.model.definition.NPCDefinition;
import org.dementhium.model.player.Skills;

/** Instance-owned ordinary drains; percentage curses remain separate modifiers. */
public final class NPCCombatStats {
    private final NPC owner;
    private final int[] levels=new int[5];
    private static final int[] SKILLS={Skills.ATTACK,Skills.STRENGTH,Skills.DEFENCE,Skills.RANGED,Skills.MAGIC};
    private int recoveryTicks;
    public NPCCombatStats(NPC owner){this.owner=owner;reset();}
    private static int index(int skill){for(int i=0;i<SKILLS.length;i++)if(SKILLS[i]==skill)return i;throw new IllegalArgumentException("Unsupported NPC combat skill: "+skill);}
    public int base(int skill){
        NPCDefinition d=owner.getDefinition();
        switch(skill){
        case Skills.ATTACK:return d.getAttackLevel();
        case Skills.STRENGTH:return d.getStrengthLevel();
        case Skills.DEFENCE:return d.getDefenceLevel();
        case Skills.RANGED:return d.getRangeLevel();
        case Skills.MAGIC:return d.getMagicLevel();
        default:throw new IllegalArgumentException("Unsupported NPC combat skill: "+skill);
        }
    }
    public int get(int skill){return levels[index(skill)];}
    /** Return unused drain for ordered carry-over, retaining a floor of one (or base zero). */
    public int drain(int skill,int amount){
        if(amount<0)throw new IllegalArgumentException("Negative drain");
        int i=index(skill);
        if(!owner.canDrainCombatStats())return amount;
        int taken=Math.min(amount,Math.max(0,levels[i]-Math.min(1,base(skill))));
        levels[i]-=taken;return amount-taken;
    }
    public void reset(){for(int i=0;i<levels.length;i++)levels[i]=base(SKILLS[i]);recoveryTicks=0;}
    public void tick(){
        if(owner.isDead()||owner.isHidden())return;
        if(++recoveryTicks<100)return;
        recoveryTicks=0;
        for(int i=0;i<levels.length;i++)if(levels[i]<base(SKILLS[i]))levels[i]++;
    }
}
