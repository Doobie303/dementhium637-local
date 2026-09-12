package org.dementhium.model.npc.impl;

import org.dementhium.model.Mob;
import org.dementhium.model.combat.CombatAction;
import org.dementhium.model.combat.CombatType;
import org.dementhium.model.instance.InstanceAccess;
import org.dementhium.model.map.path.ProjectilePathFinder;
import org.dementhium.model.misc.DamageManager.DamageType;
import org.dementhium.model.npc.NPC;

/**
 * Killable Hunter prey, not an ordinary retaliating monster.
 * The pre-EoC death explosion is adapted as an immediate 30-LP contact blast
 * against its credited attacker; no area damage or forced movement is invented.
 * Amount is a conservative server estimate. Hunter capture/drop code is unchanged.
 * Compatible behavior reference: runescape.wiki/w/Carnivorous_chinchompa
 * (modern page retains the pre-EoC explosion; its current damage is not imported).
 */
public final class Chinchompa extends NPC {
    public Chinchompa(int id){super(id);}
    @Override public void retaliate(Mob other) { }
    @Override public CombatAction getCombatAction(){return new CombatAction(true){
        @Override public CombatType getCombatType(){return CombatType.MELEE;}
        @Override public boolean commenceSession(){getCombatExecutor().reset();return false;}
        @Override public boolean executeSession(){return true;}
        @Override public boolean endSession(){return true;}
    };}
    @Override public void sendDead(){
        if(isDead())return;
        Mob killer=getDamageManager().getKiller();
        if(killer!=null&&killer.isFamiliar())killer=killer.getFamiliar().getOwner();
        super.sendDead();
        if(killer!=null && !killer.isDead() && InstanceAccess.canInteract(this,killer)
                && getLocation().getZ()==killer.getLocation().getZ()
                && Math.abs(getLocation().getX()-killer.getLocation().getX())<=1
                && Math.abs(getLocation().getY()-killer.getLocation().getY())<=1
                && ProjectilePathFinder.clearMeleePath(getLocation(),killer.getLocation())) {
            killer.getDamageManager().damage(this,30,30,DamageType.RED_DAMAGE);
        }
    }
}
