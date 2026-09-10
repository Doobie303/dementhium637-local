package org.dementhium.model.combat;
import org.dementhium.model.Mob;
import org.dementhium.model.World;
import org.dementhium.net.ActionSender;
/** Status changes are committed at projectile impact, without cancelling the victim's attack. */
public final class CombatStatus {
    private CombatStatus() { }
    public static void freezeOnImpact(Damage damage,Mob victim,int ticks) {
        if(damage!=null && !damage.isResolved() && damage.getHit()>=0)damage.onContact(()->freeze(victim,ticks));
    }
    /** Implings can be bound without entering an HP/death/reward damage route. */
    public static boolean resolveImplingBinding(Interaction interaction) {
        Damage damage=interaction.getDamage();
        if(damage==null || damage.isResolved() || damage.getHit()<0 || !interaction.isNPCContextCurrent()
                || !damage.isInstanceContextCurrent(interaction.getSource(),interaction.getVictim()))return false;
        damage.setHit(0);
        damage.finishEffects(interaction.getSource(),interaction.getVictim(),0,statusAllowed(interaction.getVictim()));
        interaction.getVictim().graphics(interaction.getEndGraphic());
        return true;
    }
    public static void miasmicOnImpact(Damage damage, final Mob victim, final int ticks) {
        if (damage.getHit() < 0) return;
        damage.onContact(() -> {
            int now = World.getTicks();
            if (!statusAllowed(victim) || victim.getAttribute("miasmicImmunity", -1) > now) return;
            victim.setAttribute("miasmicTime", now + ticks);
            victim.setAttribute("miasmicImmunity", now + ticks + 15);
            if (victim.isPlayer()) ActionSender.sendMessage(victim.getPlayer(), "You feel slowed down.");
        });
    }
    public static boolean statusAllowed(Mob victim) {
        return victim.getHitPoints() > 0 && victim.getAttribute("hitImmunity", -1) <= World.getTicks()
                && !(victim.isPlayer() && victim.getAttribute("godmode", false));
    }
    public static void shadowOnImpact(Damage damage, final Mob victim, final double fraction) {
        if(damage.getHit()<0 || !victim.isPlayer())return;
        damage.onContact(() -> {
            if(!statusAllowed(victim))return;
            int skill=org.dementhium.model.player.Skills.ATTACK;
            int level=victim.getPlayer().getSkills().getLevel(skill);
            victim.getPlayer().getSkills().set(skill,Math.max(0,(int)(level-level*fraction)));
            victim.getPlayer().sendMessage("You have been blinded.");
        });
    }
    public static void poisonOnImpact(Damage damage, final Mob source, final Mob victim, final int amount) {
        if (damage != null && amount > 0) damage.onImpact(applied -> victim.getPoisonManager().poison(source, amount));
    }
    public static void weaponPoisonOnImpact(Damage damage, Mob source, Mob victim, CombatType type) {
        if (source == null || !source.isPlayer() || type != CombatType.MELEE && type != CombatType.RANGE) return;
        org.dementhium.model.player.Equipment gear = source.getPlayer().getEquipment();
        org.dementhium.model.Item item = gear.get(org.dementhium.model.player.Equipment.SLOT_WEAPON);
        if (type == CombatType.RANGE && item != null && !item.getDefinition().doesPoison()) {
            RangeWeapon weapon = RangeWeapon.get(item.getId());
            item = weapon == null || weapon.getAmmunitionSlot() != org.dementhium.model.player.Equipment.SLOT_ARROWS
                    ? null : gear.get(org.dementhium.model.player.Equipment.SLOT_ARROWS);
        }
        if (item != null && item.getDefinition().doesPoison()) {
            // Keep the existing activation policy until a dated proc table is established.
            poisonOnImpact(damage, source, victim, item.getDefinition().getPoisonAmount());
        }
    }
    public static boolean freeze(Mob victim, int ticks) {
        int now = World.getTicks();
        if (victim.getHitPoints() <= 0 || victim.getAttribute("hitImmunity", -1) > now
                || victim.isPlayer() && victim.getAttribute("godmode", false)
                || victim.getAttribute("freezeTime", -1) > now
                || victim.getAttribute("freezeImmunity", -1) > now) return false;
        victim.getWalkingQueue().reset();
        victim.setAttribute("freezeTime", now + ticks);
        victim.setAttribute("freezeImmunity", now + ticks + 5);
        if (victim.isPlayer()) ActionSender.sendMessage(victim.getPlayer(), "You have been frozen.");
        return true;
    }
}
