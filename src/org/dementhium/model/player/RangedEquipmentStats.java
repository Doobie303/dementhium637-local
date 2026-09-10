package org.dementhium.model.player;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.dementhium.model.Item;
import org.dementhium.model.combat.RangeWeapon;
import org.dementhium.model.definition.ItemDefinition;

/** Corrections to known legacy packed ranged-strength data. See COMBAT_FOUNDATION_FIXES.md. */
public final class RangedEquipmentStats {
    private static final Map<String,Integer> STRENGTH;
    static {
        Map<String,Integer> values = new HashMap<String,Integer>();
        String[] metals = {"bronze","iron","steel","mithril","adamant","rune"};
        int[] arrows = {7,10,16,22,31,49};
        for (int i=0;i<metals.length;i++) values.put(metals[i]+" arrow",arrows[i]);
        values.put("dragon arrow",60);
        String[] bolts = {"bronze","blurite","iron","silver","bone","steel","black","mithril","adamant","rune",
                "opal","jade","pearl","topaz","sapphire","emerald","ruby","diamond","dragon","onyx"};
        int[] strengths = {10,28,46,36,49,64,75,82,100,115,14,30,48,66,83,85,103,105,117,120};
        for (int i=0;i<bolts.length;i++) values.put(bolts[i]+" bolts",strengths[i]);
        values.put("runite bolts",115);values.put("dragonstone bolts",117);values.put("red topaz bolts",66);
        values.put("bolt rack",55);values.put("kebbit bolts",28);values.put("long kebbit bolts",38);
        STRENGTH = Collections.unmodifiableMap(values);
    }
    private RangedEquipmentStats() {}
    public static int strength(ItemDefinition definition, int slot, Item weaponItem) {
        RangeWeapon weapon = weaponItem == null ? null : RangeWeapon.get(weaponItem.getId());
        // Quivered ammo cannot improve thrown or self-contained weapons.
        if (slot == Equipment.SLOT_ARROWS && (weapon == null || weapon.getAmmunitionSlot() != Equipment.SLOT_ARROWS)) return 0;
        String name = definition.getName().toLowerCase(Locale.ENGLISH)
                .replace("(p++)", "").replace("(p+)", "").replace("(p)", "").replace("(e)", "").trim();
        if (slot == Equipment.SLOT_WEAPON && weapon != null
                && weapon.getAmmunitionSlot() == Equipment.SLOT_ARROWS && name.contains("bow")) return 0;
        Integer corrected = STRENGTH.get(name);
        return corrected == null ? definition.getBonus()[Bonuses.RANGED] : corrected;
    }
}
