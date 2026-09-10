package org.dementhium.model.combat;
import org.dementhium.model.player.Player;
import org.dementhium.model.player.Equipment;
/** Life-point damage; Prayer uses 0..99 units on this server. */
public final class SpiritShield {
    private SpiritShield() { }
    private static void notice(Player player, String name, int reduced) {
        if (reduced > 0 && player.getAttribute("combatDebug", false))
            player.sendMessage("[Shield] " + name + " activated.");
    }
    public static int reduce(Player player, int hit) {
        if (hit <= 0 || player.getAttribute("godmode", false)) return Math.max(0,hit);
        int shield = player.getEquipment().getSlot(Equipment.SLOT_SHIELD);
        if (shield == 13740) {
            double prayer = Math.max(0, player.getSkills().getPrayerPoints());
            int reduced = Math.min((int)Math.ceil(hit * .30), (int)Math.floor(prayer * 20));
            if (reduced > 0) player.getSkills().drainPray(Math.min(prayer, (reduced / 20.0)));
            notice(player, "Divine", reduced);
            return hit - reduced;
        }
        if (shield == 13742 && player.getRandom().nextInt(100) < 70) {
            int remaining = (int)Math.floor(hit * .75);
            notice(player, "Elysian", hit - remaining);
            return remaining;
        }
        return hit;
    }
}