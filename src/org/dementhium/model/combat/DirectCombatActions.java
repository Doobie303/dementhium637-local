package org.dementhium.model.combat;

import org.dementhium.model.Mob;
import org.dementhium.model.Projectile;
import org.dementhium.model.World;
import org.dementhium.model.instance.InstanceAccess;
import org.dementhium.model.misc.DamageManager.DamageType;
import org.dementhium.model.misc.ProjectileManager;
import org.dementhium.model.player.Player;
import org.dementhium.tickable.Tick;
import org.dementhium.util.Misc;

/** Bespoke attacks retain their raw damage and no-XP rules, with launch ownership. */
public final class DirectCombatActions {
    private DirectCombatActions() { }

    public static boolean discharge(final Player player, final Mob victim) {
        final NPCCombatContext context = new NPCCombatContext(player, victim);
        final Object sourceActivity = player.getActivity(), victimActivity = victim.getActivity();
        if (!current(player, victim, context, sourceActivity, victimActivity)) return false;
        if (!CombatUtils.consumeDragonfireShieldCharge(player)) {
            player.sendMessage("Your dragonfire shield has no charges left.");
            return false;
        }
        player.setAttribute("dischargeDelay", World.getTicks() + 200);
        player.animate(6696);
        player.graphics(1165);
        player.turnTo(victim, false);
        World.getWorld().submit(new Tick(1) {
            private int elapsed;
            public void execute() {
                if (!current(player, victim, context, sourceActivity, victimActivity)) { stop(); return; }
                if (++elapsed == 3) {
                    int speed = (int) (27 + player.getLocation().distance(victim.getLocation()) * 5);
                    ProjectileManager.sendProjectile(Projectile.create(player, victim, 1166, 40, 36, 20, speed, 15, 11));
                } else if (elapsed == 4) {
                    stop();
                    victim.getDamageManager().damage(player, Misc.random(290), -1, DamageType.MAGE);
                }
            }
        });
        return true;
    }

    public static void superhit(final Player player, final Mob victim, final int amount) {
        final NPCCombatContext context = new NPCCombatContext(player, victim);
        final Object sourceActivity = player.getActivity(), victimActivity = victim.getActivity();
        if (!current(player, victim, context, sourceActivity, victimActivity)) return;
        Tick task = new Tick(1) {
            private int elapsed;
            @Override public void stop() {
                super.stop();
                if (player.getAttribute("superhitAnimation") == this) {
                    player.removeAttribute("superhitAnimation");
                    player.setCanAnimate(true);
                }
            }
            public void execute() {
                if (!current(player, victim, context, sourceActivity, victimActivity)) { stop(); return; }
                elapsed++;
                boolean ownsAnimation = player.getAttribute("superhitAnimation") == this;
                if (ownsAnimation && (elapsed == 3 || elapsed == 5 || elapsed == 8)) {
                    player.setCanAnimate(true);
                    player.animate(elapsed == 3 ? 1500 : elapsed == 5 ? 1501 : 1502);
                    player.setCanAnimate(false);
                } else if (elapsed == 9) {
                    stop();
                    if (ownsAnimation) {
                        player.getMask().setAppearanceUpdate(true);
                        player.graphics(287);
                    }
                    victim.getDamageManager().damage(player, amount, -1, DamageType.MAGE);
                    if (ownsAnimation && current(player, victim, context, sourceActivity, victimActivity))
                        player.getCombatExecutor().setVictim(victim);
                }
            }
        };
        player.setAttribute("superhitAnimation", task);
        player.setCanAnimate(true);
        player.animate(842);
        player.setCanAnimate(false);
        // World ownership also runs cleanup after leaving an instance.
        World.getWorld().submit(task);
    }

    private static boolean current(Player source, Mob victim, NPCCombatContext context,
                                   Object sourceActivity, Object victimActivity) {
        return source != victim && source.isOnline() && !source.isDead() && source.getHitPoints() > 0
                && !victim.isDead() && victim.getHitPoints() > 0
                && (!victim.isPlayer() || victim.getPlayer().isOnline())
                && source.getActivity() == sourceActivity && victim.getActivity() == victimActivity
                && context.isCurrent() && InstanceAccess.canInteract(source, victim);
    }
}
