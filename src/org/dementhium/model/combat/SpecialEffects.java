package org.dementhium.model.combat;

import org.dementhium.model.*;
import org.dementhium.model.player.Player;
import org.dementhium.model.misc.DamageManager.DamageType;
import org.dementhium.tickable.Tick;

/** Attack-owned secondary effects; no shared victim amount or mutable action lookup. */
public final class SpecialEffects {
    private SpecialEffects() { }
    public static boolean current(Interaction interaction, Damage original, boolean requireVictimAlive) {
        Mob source=interaction.getSource(), victim=interaction.getVictim();
        return source.getHitPoints()>0 && (!requireVictimAlive || victim.getHitPoints()>0)
                && (!source.isPlayer() || source.getPlayer().isOnline())
                && (!victim.isPlayer() || victim.getPlayer().isOnline())
                && original.isInstanceContextCurrent(source,victim)
                && org.dementhium.model.instance.InstanceAccess.canInteract(source,victim);
    }
    public static void submit(Mob source,Tick task) {
        org.dementhium.model.instance.GameInstance owner=org.dementhium.model.instance.InstanceAccess.owner(source);
        if(owner==null)World.getWorld().submit(task);else owner.submitTask(task);
    }
    public static Tick healing(Interaction interaction,Damage original,int amount,int pulses,int interval) {
        Tick task=new Tick(interval) {
            int left=pulses;
            public void execute() {
                if(left<=0 || !current(interaction,original,false)){stop();return;}
                interaction.getSource().heal(Math.max(0,amount));
                if(--left==0)stop();
            }
        };
        if(amount>0 && pulses>0)submit(interaction.getSource(),task);else task.stop();
        return task;
    }
    public static Tick bleed(Interaction interaction,Damage original,int amount) {
        final java.util.function.IntConsumer xp=original.experience(interaction.getSource().getPlayer(),DamageType.RANGE);
        Tick task=new Tick(1) {
            int left=amount;
            public void execute() {
                if(left<=0 || !current(interaction,original,true) || !CombatStatus.statusAllowed(interaction.getVictim())){stop();return;}
                int hit=Math.min(50,left);left-=hit;
                Damage damage=new Damage(hit).onImpact(xp);
                interaction.getVictim().getDamageManager().damage(interaction.getSource(),damage,DamageType.RED_DAMAGE);
                if(left<=0)stop();
            }
        };
        if(amount>0)submit(interaction.getSource(),task);else task.stop();
        return task;
    }
    public static void energyDrain(Damage damage,Mob source,Mob victim) {
        if(!victim.isPlayer())return;
        damage.onImpact(actual -> {
            int wanted=Math.min(35,Math.max(10,actual/10));
            int transferred=Math.min(victim.getWalkingQueue().getRunEnergy(),wanted);
            victim.getWalkingQueue().setRunEnergy(victim.getWalkingQueue().getRunEnergy()-transferred);
            source.getWalkingQueue().setRunEnergy(Math.min(100,source.getWalkingQueue().getRunEnergy()+transferred));
        });
    }
}
