package org.dementhium.model.misc;

import org.dementhium.model.Mob;
import org.dementhium.model.World;
import org.dementhium.model.combat.CombatStatus;
import org.dementhium.model.misc.DamageManager.DamageType;
import org.dementhium.net.ActionSender;
import org.dementhium.tickable.Tick;

/** One owned poison stream per victim. Curing invalidates every older scheduled tick. */
public class PoisonManager {
    private static final int POISON_TIME = 30;
    private final Mob mob;
    private int amount, cycles;
    private Mob poisoner;
    private boolean canBe = true;
    private long generation;
    private Tick pending;

    public PoisonManager(Mob mob) { this.mob = mob; }
    public void poison(Mob attacker, int initialAmount) {
        if (initialAmount < 10 || !canBe || !CombatStatus.statusAllowed(mob)) return;
        if (isPoisoned()) {
            if (initialAmount > amount) { amount = initialAmount; cycles = 0; poisoner = attacker; }
            return;
        }
        poisoner = attacker;
        start(initialAmount);
        if (mob.isPlayer()) mob.getPlayer().sendMessage("You have been poisoned!");
    }
    public void continuePoison(int initialAmount) {
        if (initialAmount >= 10 && !isPoisoned()) start(initialAmount);
    }
    private void start(int initialAmount) {
        amount = initialAmount; cycles = 0;
        final long ownedGeneration = ++generation, revision = mob.getInstanceRevision();
        final long npcLife=mob.isNPC()&&!mob.isFamiliar()?mob.getNPC().getCombatGeneration():0;
        if (mob.isPlayer()) ActionSender.sendConfig(mob.getPlayer(), 102, 1);
        pending = new Tick(POISON_TIME) {
            public void execute() {
                if (ownedGeneration != generation) { stop(); return; }
                if (!isPoisoned() || cycles >= 8 || mob.getHitPoints() <= 0
                        || mob.getInstanceRevision() != revision || mob.isPlayer() && !mob.getPlayer().isOnline()
                        || mob.isNPC()&&!mob.isFamiliar()&&npcLife!=mob.getNPC().getCombatGeneration()) {
                    removePoison(); stop(); return;
                }
                mob.getDamageManager().damage(poisoner, amount, 1000, DamageType.POSION);
                if (ownedGeneration != generation) { stop(); return; }
                amount -= 2; cycles++;
                if (amount < 10 || cycles >= 8) { removePoison(); stop(); }
            }
        };
        // World ownership lets the revision guard also clear the saved poison state after departure.
        World.getWorld().submit(pending);
    }
    public void removePoison() {
        boolean hadPoison = amount > 0 || pending != null;
        generation++;
        if (pending != null) pending.stop();
        pending = null; amount = 0; cycles = 0; poisoner = null;
        if (mob.isPlayer() && hadPoison) {
            ActionSender.sendConfig(mob.getPlayer(), 102, 0);
            mob.getPlayer().sendMessage("The poison has worn off.");
        }
    }
    public Mob getPoisoner() { return poisoner; }
    public void setPoisoner(Mob poisoner) { this.poisoner = poisoner; }
    public boolean isPoisoned() { return amount >= 10; }
    public int getCurrentPoisonAmount() { return amount; }
    public boolean canBePoisoned() { return canBe; }
    public void setCanBePoisoned(boolean value) { canBe = value; }
}
