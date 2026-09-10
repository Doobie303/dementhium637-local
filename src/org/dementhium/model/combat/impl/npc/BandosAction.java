package org.dementhium.model.combat.impl.npc;
import org.dementhium.model.Mob;
import org.dementhium.model.combat.CombatType;
/** Urgent attack repair. Room-wide ranged targeting remains in the GWD encounter batch. */
public class BandosAction extends BasicBossAttack {
    public BandosAction() { super(CombatType.MELEE, 600, 7060, -1, -1); }
    @Override protected CombatType selectStyle(Mob source) {
        return source.getRandom().nextInt(3) == 0 ? CombatType.RANGE : CombatType.MELEE;
    }
    @Override protected int maximum(CombatType type) { return type == CombatType.MELEE ? 600 : 350; }
    @Override protected int animation(CombatType type) { return type == CombatType.MELEE ? 7060 : 7063; }
    @Override protected int projectile(CombatType type) { return type == CombatType.MELEE ? -1 : 1200; }
    @Override protected int endGraphic(CombatType type) { return type == CombatType.MELEE ? -1 : 1218; }
}
