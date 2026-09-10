package org.dementhium.model.combat;
import org.dementhium.model.*;
import org.dementhium.model.player.Player;
import org.dementhium.model.misc.DamageManager.DamageType;
import org.dementhium.tickable.Tick;
/** Per-hit impact resolution. Delays here are server ticks, not client hitsplat delays. */
public final class SpecialHits {
    private SpecialHits() { }
    public static void awardOnImpact(final Player player, final Damage damage, final DamageType type) {
        if (damage == null || !damage.claimExperience()) return;
        java.util.function.IntConsumer xp=damage.experience(player,type);
        if (damage.isResolved()) xp.accept(damage.getHit());
        else damage.onImpact(xp);
    }
    public static void apply(final Interaction interaction, final Damage damage, final DamageType type, int ticks) {
        if (damage == null || damage.isResolved()) return;
        if (ticks > 0) {
            Tick pending = new Tick(ticks) {
                public void execute() { stop(); apply(interaction, damage, type, 0); }
            };
            org.dementhium.model.instance.GameInstance owner = org.dementhium.model.instance.InstanceAccess.owner(interaction.getSource());
            if (owner == null) World.getWorld().submit(pending); else owner.submitTask(pending);
            return;
        }
        Mob source = interaction.getSource(), victim = interaction.getVictim();
        if (source.getHitPoints() <= 0 || victim.getHitPoints() <= 0 || !damage.isInstanceContextCurrent(source, victim)
                || !org.dementhium.model.instance.InstanceAccess.canInteract(source, victim)) return;
        if (source.isPlayer()) awardOnImpact(source.getPlayer(), damage, type);
        victim.getDamageManager().damage(source, damage, type);
        if (!damage.isResolved()) return;



        victim.retaliate(source);
    }
}
