package org.dementhium.model.combat;
import java.util.Locale;
import org.dementhium.model.Mob;
import org.dementhium.model.player.Player;
import org.dementhium.model.player.Equipment;
import org.dementhium.content.skills.slayer.SlayerTask;
/** Target-dependent pre-EoC effects, separate from displayed equipment bonuses. */
public final class EquipmentEffects {
    private EquipmentEffects() { }
    public static double multiplier(Mob source, Mob victim, CombatType style) {
        if (!source.isPlayer() || victim == null || !victim.isNPC()) return 1;
        Player p = source.getPlayer();
        SlayerTask task = p.getSlayer().getSlayerTask();
        String name = victim.getNPC().getDefinition().getName().toLowerCase(Locale.ROOT);
        String helm = p.getEquipment().get(Equipment.SLOT_HAT) == null ? ""
                : p.getEquipment().get(Equipment.SLOT_HAT).getDefinition().getName().toLowerCase(Locale.ROOT);
        boolean onTask = task != null && task.getCurrentTaskAmount() > 0 && matchesTask(task.getName(),name);
        boolean full = helm.equals("full slayer helmet");
        if (onTask && (style == CombatType.MELEE && (full || helm.equals("slayer helmet") || helm.startsWith("black mask"))
                || style == CombatType.RANGE && (full || helm.equals("focus sight"))
                || style == CombatType.MAGIC && (full || helm.equals("hexcrest"))))
            return style == CombatType.MELEE ? 7.0/6 : 1.15;
        // Salve is melee-only in this era and never stacks with the task helmet bonus.
        if (style == CombatType.MELEE && undead(name)) {
            int amulet = p.getEquipment().getSlot(Equipment.SLOT_AMULET);
            if (amulet == 10588) return 1.2;
            if (amulet == 4081) return 7.0/6;
        }
        return 1;
    }
    private static String singular(String name) {
        String n = name.toLowerCase(Locale.ROOT).trim();
        return n.endsWith("s") ? n.substring(0,n.length()-1) : n;
    }
    public static boolean matchesTask(String task, String name) {
        String t=singular(task), n=singular(name);
        if(t.equals(n))return true;
        if(t.equals("bat"))return n.equals("giant bat");
        if(t.equals("bloodveld"))return n.equals("mutated bloodveld");
        if(t.equals("banshee"))return n.equals("mighty banshee");
        if(t.equals("aberrant spectre"))return n.equals("aberrant specter");
        return false;
    }
    /** Damage only: do not add this to the accuracy/Slayer multiplier. */
    public static double obsidianDamage(Mob source) {
        if(!source.isPlayer() || source.getPlayer().getEquipment().getSlot(Equipment.SLOT_AMULET)!=11128)return 1;
        int weapon=source.getPlayer().getEquipment().getSlot(Equipment.SLOT_WEAPON);
        return weapon==6523 || weapon==6525 || weapon==6527 || weapon==6528 ? 1.2 : 1;
    }
    public static boolean undead(String n) {
        n=n.toLowerCase(Locale.ROOT).trim();
        return n.equals("skeleton") || n.equals("zombie") || n.equals("ghost") || n.equals("ankou")
                || n.equals("banshee") || n.equals("aberrant spectre") || n.equals("armoured zombie")
                || n.equals("shade") || n.equals("ghast") || n.startsWith("revenant ")
                || n.equals("giant skeleton") || n.equals("skeleton mage") || n.equals("skeleton warrior")
                || n.equals("zombie hand") || n.equals("skeletal hand") || n.equals("mighty banshee")
                || n.equals("aberrant specter") || n.equals("crawling hand");
    }
}
