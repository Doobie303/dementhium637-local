package org.dementhium.model.combat;

import org.dementhium.model.Mob;
import org.dementhium.model.player.Player;
import org.dementhium.model.player.Skills;

/** Player set effects. The complete usable set is captured when the hit is created. */
public final class BarrowsEquipmentEffects {
    private BarrowsEquipmentEffects() { }
    public static Damage melee(Mob source, Mob victim) {
        boolean defiler = source.isPlayer() && source.getPlayer().getEquipment().barrowsSet(6)
                && source.getRandom().nextInt(4) == 0;
        int maximum = MeleeFormulae.getMeleeDamage(source, EquipmentEffects.multiplier(source,victim,CombatType.MELEE));
        int roll = defiler ? CombatRolls.roll(source.getRandom(), maximum) : MeleeFormulae.getDamage(source,victim);
        return Damage.getDamage(source,victim,CombatType.MELEE,roll,defiler);
    }
    public static void attach(Damage damage, Mob source, Mob victim, CombatType style) {
        if (source == null || !source.isPlayer()) return;
        Player player = source.getPlayer();
        int set = style == CombatType.MAGIC ? 1 : style == CombatType.RANGE ? 4
                : style == CombatType.MELEE && player.getEquipment().barrowsSet(3) ? 3
                : style == CombatType.MELEE ? 5 : 0;
        if (set == 0 || !player.getEquipment().barrowsSet(set) || source.getRandom().nextInt(4) != 0) return;
        final int effect = set;
        damage.onImpact(actual -> {
            if (source.getHitPoints() <= 0) return;
            if (effect == 3) { player.getSkills().heal(actual); victim.graphics(398); }
            else if (victim.isPlayer() && CombatStatus.statusAllowed(victim)) {
                Player target = victim.getPlayer();
                if (effect == 1) { target.getSkills().decreaseLevelToMinimum(Skills.STRENGTH,5); victim.graphics(400); }
                if (effect == 4) { target.getSkills().decreaseLevelToMinimum(Skills.AGILITY,
                        Math.max(1,target.getSkills().getLevel(Skills.AGILITY)/5)); victim.graphics(401); }
                if (effect == 5) { target.getWalkingQueue().setRunEnergy(Math.max(0,
                        target.getWalkingQueue().getRunEnergy()-target.getWalkingQueue().getRunEnergy()/5));victim.graphics(399); }
            }
        });
    }
}
