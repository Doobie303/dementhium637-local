package org.dementhium.model.definition;

/** Verified pre-EoC soaking corrections; see EQUIPMENT_ABSORPTION_FIXES.md. */
final class EquipmentAbsorption {
    private EquipmentAbsorption() { }

    static void apply() {
        // Rune: contemporary December 2010 equipment-screen observations.
        set(1163, 1, 0, 3);
        set(1127, 3, 0, 6);
        // Barrows: pristine definitions also serve the four usable worn stages.
        for (int id : new int[] {4716, 4724, 4745, 4753}) set(id, 2, 0, 5);
        for (int id : new int[] {4720, 4728, 4749, 4757}) set(id, 5, 0, 10);
        for (int id : new int[] {4722, 4730, 4751, 4759}) set(id, 3, 0, 7);
        set(4708, 5, 2, 0); set(4712, 10, 5, 0); set(4714, 7, 3, 0);
        set(4732, 0, 7, 3); set(4736, 0, 10, 5); set(4738, 0, 7, 3);
        // God Wars equipment.
        set(11724, 4, 0, 9); set(11726, 3, 0, 6);
        set(11718, 0, 5, 2); set(11720, 0, 10, 5); set(11722, 0, 7, 3);
        nex(20135, 3, 0, 6); nex(20139, 6, 0, 12); nex(20143, 4, 0, 8);
        nex(20147, 0, 6, 3); nex(20151, 0, 12, 6); nex(20155, 0, 8, 4);
        nex(20159, 6, 3, 0); nex(20163, 12, 6, 0); nex(20167, 8, 4, 0);
        // These reward shields have soaking; Daemonheim armour (including Primal) does not.
        set(18359, 7, 0, 14); set(18361, 0, 14, 7); set(18363, 14, 7, 0);
    }

    private static void nex(int id, int melee, int magic, int ranged) {
        set(id, melee, magic, ranged);
        set(id + 2, melee, magic, ranged); // Usable, untradeable version, not broken id + 3.
    }

    private static void set(int id, int melee, int magic, int ranged) {
        ItemDefinition.forId(id).setAbsorptionBonus(new int[] {melee, magic, ranged});
    }
}