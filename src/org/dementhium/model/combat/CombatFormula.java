package org.dementhium.model.combat;

import org.dementhium.model.Mob;
import org.dementhium.model.definition.WeaponInterface;
import org.dementhium.model.player.Skills;

/** Period effective-level reconstruction. Evidence/limits: COMBAT_FORMULA_FIXES.md. */
public final class CombatFormula {
    private CombatFormula() { }

    public static int effectiveLevel(int level, double modifier, int stance, double setMultiplier) {
        double adjusted = Math.floor(Math.max(0, level) * Math.max(0, 1 + modifier) + 1e-9);
        return floor((adjusted + 8 + stance) * setMultiplier);
    }

    /** 2011 research expressed accuracy rolls at ten times effective level. */
    public static int accuracyRoll(int effective, int bonus, double multiplier) {
        return floor(effective * Math.max(0L, (long) bonus + 64) * 10.0 / 64 * multiplier);
    }

    /** Life points, not the old 10-HP units; caller effects apply after the base. */
    public static int maximumHit(int effective, int bonus, double multiplier) {
        return floor((5 + effective * Math.max(0L, (long) bonus + 64) / 64.0) * multiplier);
    }

    public static int defenceLevel(Mob victim) {
        int level = victim.isPlayer() ? victim.getPlayer().getSkills().getLevel(Skills.DEFENCE)
                : victim.getNPC().getCombatLevel(org.dementhium.model.player.Skills.DEFENCE);
        double modifier = victim.isPlayer() ? victim.getPlayer().getPrayer().getDefenceModifier()
                : victim.getNPC().getDefenceModifier();
        return effectiveLevel(level, modifier, defenceStance(victim) + (victim.isPlayer() ? victim.getPlayer().getPrayer().getTurmoilDefence() : 0), 1);
    }

    public static int defenceStance(Mob victim) {
        if (!victim.isPlayer()) return 0;
        int style = victim.getPlayer().getSettings().getCombatStyle();
        if (style == WeaponInterface.STYLE_DEFENSIVE || style == WeaponInterface.STYLE_LONG_RANGE) return 3;
        return style == WeaponInterface.STYLE_CONTROLLED ? 1 : 0;
    }

    static int floor(double value) {
        if (!(value > 0)) return 0;
        return (int) Math.min(Integer.MAX_VALUE - 1, Math.floor(value + 1e-9));
    }
}