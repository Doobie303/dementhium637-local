package org.dementhium.model.combat;
import java.util.Random;

/** Independent uniform accuracy and damage rolls. Ties favour the defender.
 * Roll maxima remain in the formula classes for separate historical calibration.
 */
public final class CombatRolls {
    private CombatRolls() {}
    public static int roll(Random random, double maximum) {
        if (!(maximum > 0)) return 0;
        int cap = (int)Math.min(Integer.MAX_VALUE - 1, Math.floor(maximum));
        return random.nextInt(cap + 1);
    }
    public static boolean hits(Random attackRandom, double attack, Random defenceRandom, double defence) {
        return roll(attackRandom, attack) > roll(defenceRandom, defence);
    }
}
