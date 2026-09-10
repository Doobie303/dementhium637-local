package org.dementhium.model.combat;
import org.dementhium.model.Mob;
import org.dementhium.model.instance.InstanceAccess;

/** NPC life ownership, separate from managed-instance membership. PvP passes through. */
public final class NPCCombatContext {
    private final Mob source,victim;
    private final long sourceLife,victimLife,sourceInstance,victimInstance;
    private final long sourceCombat,victimCombat;
    private static boolean ordinary(Mob m){return m!=null&&m.isNPC()&&!m.isFamiliar();}
    public NPCCombatContext(Mob source,Mob victim){
        this.source=source;this.victim=victim;
        sourceCombat=source==null?0:source.getCombatRevision();victimCombat=victim.getCombatRevision();
        sourceLife=life(source);victimLife=life(victim);
        sourceInstance=source==null?0:source.getInstanceRevision();victimInstance=victim.getInstanceRevision();
    }
    private static long life(Mob m){return ordinary(m)?m.getNPC().getCombatGeneration():0;}
    public boolean isSourceCurrent(){return source==null || sourceLife==life(source)
            && sourceInstance==source.getInstanceRevision() && sourceCombat==source.getCombatRevision();}
    public boolean isCurrent(){
        if(!isSourceCurrent() || victimLife!=life(victim) || victimInstance!=victim.getInstanceRevision()
                || victimCombat!=victim.getCombatRevision())return false;
        if(!org.dementhium.model.npc.encounter.EncounterNPC.pair(source,victim))return false;
        if(source!=null && (source instanceof org.dementhium.model.npc.godwars.GodWarsNPC || victim instanceof org.dementhium.model.npc.godwars.GodWarsNPC)
                && !org.dementhium.model.npc.godwars.GodWarsNPC.roomPair(source,victim))return false;
        if(!ordinary(source)&&!ordinary(victim))return true;
        return sourceLife==life(source)&&victimLife==life(victim)
            && sourceInstance==(source==null?0:source.getInstanceRevision())&&victimInstance==victim.getInstanceRevision()
            && (source==null||validPair(source,victim));
    }
    public static boolean validPair(Mob source,Mob victim){
        if(source==null||victim==null)return false;
        boolean nex=source instanceof org.dementhium.model.npc.impl.Nex||victim instanceof org.dementhium.model.npc.impl.Nex;
        if(nex&&!org.dementhium.model.npc.impl.Nex.arenaPair(source,victim))return false;
        if(!org.dementhium.model.npc.encounter.EncounterNPC.pair(source,victim))return false;
        if(source!=null && (source instanceof org.dementhium.model.npc.godwars.GodWarsNPC || victim instanceof org.dementhium.model.npc.godwars.GodWarsNPC)
                && !org.dementhium.model.npc.godwars.GodWarsNPC.roomPair(source,victim))return false;
        if(!ordinary(source)&&!ordinary(victim))return true;
        return !source.isDead()&&!victim.isDead()&&!source.isHidden()&&!victim.isHidden()
            && (!ordinary(source)||!source.getNPC().isDying())
            && (!ordinary(victim)||!victim.getNPC().isDying())
            && (!source.isPlayer()||source.getPlayer().isOnline())&&(!victim.isPlayer()||victim.getPlayer().isOnline())
            && (!ordinary(source)||!source.getNPC().isReturningHome())
            && (!ordinary(victim)||!victim.getNPC().isReturningHome())
            && source.getLocation().getZ()==victim.getLocation().getZ()
            && (nex || source.getLocation().distance(victim.getLocation())<=17 || org.dementhium.content.minigames.FightCaves.isCaveOpponent(source,victim) || org.dementhium.model.npc.godwars.GodWarsNPC.roomPair(source,victim)) && InstanceAccess.canInteract(source,victim);
    }
}
