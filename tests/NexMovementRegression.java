import java.lang.reflect.Field;
import java.util.LinkedList;
import org.dementhium.model.*;
import org.dementhium.model.combat.*;

import org.dementhium.tickable.Tick;

public final class NexMovementRegression {
    static final class TestMob extends Mob {
        boolean dead;
        @Override public void setLocation(Location value) { location = value; }
        @Override public void teleport(int x, int y, int z, boolean warning) { location = Location.locate(x, y, z); }
        @Override public boolean isDead() { return dead; }
        public boolean isAttackable(Mob mob) { return true; }
        public int getDefenceAnimation() { return -1; }
        public int getAttackAnimation() { return -1; }
        public int getAttackDelay() { return 4; }
        public CombatAction getCombatAction() { return null; }
        public void forceText(String value) { }
        public int getHitPoints() { return 100; }
        public int getMaximumHitPoints() { return 100; }
        public void retaliate(Mob mob) { }
        public Damage updateHit(Mob mob, int hit, CombatType type) { return null; }
        public RangeData getRangeData(Mob mob) { return null; }
    }
    @SuppressWarnings("unchecked")
    static LinkedList<Tick> pending() throws Exception {
        Field field = World.class.getDeclaredField("ticksToAdd");
        field.setAccessible(true);
        return (LinkedList<Tick>) field.get(World.getWorld());
    }
    static Tick take() throws Exception { return pending().removeLast(); }
    static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        TestMob mob = new TestMob();
        mob.getWalkingQueue();
        mob.setLocation(Location.locate(2924, 5202, 0));
        mob.forceMovement(null, 2927, 5202, 0, 60, -1, 2, true);
        require(mob.getForceWalk()[4] == 1, "East must encode as 1");
        Tick first = take();
        first.run();
        require(mob.getLocation().getX() == 2924, "Movement completed early");
        mob.forceMovement(null, 2928, 5202, 0, 60, -1, 2, true);
        Tick second = take();
        first.run();
        require(mob.getLocation().getX() == 2924, "Superseded movement teleported actor");
        require(mob.getAttribute("cantMove") != null, "Old movement released new lock");
        second.run(); second.run();
        require(mob.getLocation().getX() == 2928, "Latest movement did not complete");
        require(mob.getAttribute("cantMove") == null, "Movement lock was not released");
        mob.forceMovement(null, 2929, 5202, 0, 80, -1, 2, true);
        Tick longMove = take();
        longMove.run(); longMove.run();
        require(mob.getLocation().getX() == 2928, "80-frame movement completed before three ticks");
        longMove.run();
        require(mob.getLocation().getX() == 2929, "80-frame movement did not complete");
        mob.forceMovement(null, 2930, 5202, 0, 60, -1, 2, true);
        Tick escaped = take();
        mob.teleport(3200, 3200, 1, false);
        escaped.run(); escaped.run();
        require(mob.getLocation().equals(Location.locate(3200,3200,1)), "Movement pulled actor back after teleport");
        mob.forceMovement(null, 3201, 3200, 0, 60, -1, 2, true);
        Tick dead = take(); mob.dead = true; dead.run(); dead.run();
        require(mob.getLocation().getX() == 3200, "Dead actor was moved");
        mob.dead = false;
        mob.forceMovement(null, 3201, 3200, 0, 60, -1, 2, true);
        Tick stunned = take(); mob.setAttribute("stunned", Boolean.TRUE);
        stunned.run(); stunned.run();
        require(mob.getAttribute("cantMove") != null, "Movement cleared an active stun lock");
        mob.removeAttribute("stunned");
        mob.forceMovement(null, 3202, 3200, 0, 60, -1, 2, true);
        Tick cancelled = take(); mob.cancelForceMovement(); cancelled.run(); cancelled.run();
        require(mob.getLocation().getX() == 3201, "Cancelled encounter movement still teleported actor");
        System.out.println("PASS: forced movement direction, timing, replacement, departure, death and stun ownership");
    }
}
