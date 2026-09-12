import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.dementhium.model.Location;
import org.dementhium.model.Mob;
import org.dementhium.model.World;
import org.dementhium.model.combat.CombatType;
import org.dementhium.model.map.Region;
import org.dementhium.model.misc.DamageManager.DamageType;
import org.dementhium.model.npc.NPCLoader;
import org.dementhium.model.npc.impl.YkLagor;
import org.dementhium.model.player.Player;
import org.dementhium.model.player.Skills;
import org.dementhium.task.impl.NPCTickTask;
import org.dementhium.tickable.Tick;

/** Native registry/task/executor order; fixture clipping and loot recording do not alter the live world. */
public final class YkLagorRegression {
    private static int checks;
    private static final List<Tick> active = new ArrayList<Tick>();
    private static void check(boolean result, String reason) {
        checks++;
        if (!result) throw new AssertionError(reason);
    }
    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name); field.setAccessible(true); return field;
    }
    @SuppressWarnings("unchecked") private static void callbacks() throws Exception {
        LinkedList<Tick> queue = (LinkedList<Tick>)field(World.class, "ticksToAdd").get(World.getWorld());
        active.addAll(queue); queue.clear();
        for (Iterator<Tick> it = active.iterator(); it.hasNext();) if (!it.next().run()) it.remove();
    }
    private static class LootYk extends YkLagor {
        int rewards; Mob paid;
        LootYk() { super(11886); }
        @Override public void loot(Mob killer) { rewards++; paid = killer; }
    }
    private static final class Fixture {
        final YkLagor npc;
        final Player player;
        int tick;
        Fixture(boolean distant, boolean lootRecorder) throws Exception {
            CombatFixtures.clearPlayers(); active.clear();
            ((List<?>)field(World.class, "ticksToAdd").get(World.getWorld())).clear();
            npc = lootRecorder ? new LootYk() : (YkLagor)NPCLoader.getNPC(11886);
            npc.setLocation(Location.locate(3216, 3216, 0)); npc.setOriginalLocation(npc.getLocation());
            npc.setDoesWalk(false); npc.getRandom().setSeed(823);
            Region region = npc.getLocation().getRegion();
            region.clippingMasks = new int[4][128][128]; region.setClipped(true);
            player = CombatFixtures.player(npc);
            player.getSkills().setMaximumLifePoints(100000); player.getSkills().setHitPoints(100000);
            if (distant) player.setLocation(npc.getLocation().transform(8, 0, 0));
            npc.getCombatExecutor().setVictim(player);
        }
        int animation() { return npc.getMask().getLastAnimation() == null ? -1 : npc.getMask().getLastAnimation().getId(); }
        void step() throws Exception {
            tick++;
            field(World.class, "ticksPassed").setInt(null, World.getTicks() + 1);
            callbacks(); npc.getMask().reset(); new NPCTickTask(npc).execute();
        }
        void steps(int n) throws Exception { for (int i = 0; i < n; i++) step(); }
        void until(int n) throws Exception { while (tick < n) step(); }
        int grabTick() { return 3 + 6 * npc.getAttackDelay(); }
        int quakeTick() { return grabTick() + 7 + 6 * npc.getAttackDelay(); }
        void prayer(CombatType type) throws Exception {
            boolean[][] prayers = (boolean[][])field(player.getPrayer().getClass(), "onPrayers").get(player.getPrayer());
            prayers[0][type.getProtectionPrayer()] = true;
        }
        void wall(int x, int mask) {
            Region region = npc.getLocation().getRegion();
            for (int y = 3200; y <= 3250; y++) region.clippingMasks[0][x & 127][y & 127] = mask;
        }
    }
    private static void basics() throws Exception {
        check(NPCLoader.getNPC(11886) instanceof YkLagor, "Attackable Yk resolves through production registry");
        check(!(NPCLoader.getNPC(12846) instanceof YkLagor), "Bound display form excluded");
        for (boolean distant : new boolean[]{false, true}) {
            Fixture f = new Fixture(distant, false);
            f.prayer(distant ? CombatType.MAGIC : CombatType.MELEE);
            List<Integer> launches = new ArrayList<Integer>();
            for (int t = 1; t < f.grabTick(); t++) {
                f.step();
                if (f.animation() == (distant ? 14396 : 14374)) launches.add(t);
            }
            check(launches.size() == 6 && launches.get(0) == 3, "Six natural attacks after initial cooldown");
            for (int i = 1; i < launches.size(); i++) check(launches.get(i) - launches.get(i-1) == f.npc.getAttackDelay(), "Natural ordinary cadence");
            if (distant) check(f.player.getHitPoints() < 100000, "Fire magic pierces magic prayer");
            else check(f.player.getHitPoints() == 100000, "Melee prayer fully protects ordinary melee");
            check(f.player.getSkills().getPrayerPoints() < 99, "Ordinary hits drain prayer on contact");
        }
    }
    private static void rotationAndImpact() throws Exception {
        Fixture f = new Fixture(true, false);
        f.until(f.grabTick());
        check(f.animation() == 14370, "Grab starts with natural warning");
        f.prayer(CombatType.MAGIC);
        double prayer = f.player.getSkills().getPrayerPoints();
        f.npc.setHp(f.npc.getMaxHp() / 2);
        int hp = f.npc.getHp(), before = f.player.getHitPoints();
        Location original = f.npc.getLocation(), victimStart = f.player.getLocation();
        for (int i = 1; i <= 5; i++) {
            f.step();
            check(f.npc.getLocation().equals(original), "Boss holds warning origin");
            if (i < 5) check(f.player.getHitPoints() == before && f.npc.getHp() == hp, "No early grab damage/healing");
        }
        check(f.animation() == 14390 && before - f.player.getHitPoints() >= 100
                && before - f.player.getHitPoints() <= 200, "Grab lands exactly on fifth tick within bounded damage");
        check(f.npc.getHp() == hp + f.npc.getMaxHp() * 15 / 100, "Grab heals bounded documented fraction");
        check(f.player.getLocation().equals(victimStart.transform(-2, 0, 0)), "Grab pulls two clipped tiles");
        check(f.player.getAttribute("stunned", false), "Grab applies timed stun");
        check(!f.player.getPrayer().usingPrayer(0, CombatType.MAGIC.getProtectionPrayer())
                && f.player.getSkills().getPrayerPoints() == Math.max(0, prayer - 10), "Grab disables prayer and drains once at impact");
        int after = f.player.getHitPoints();
        f.step(); check(f.player.getHitPoints() == after, "Grab impact is not repeated");
        f.until(f.quakeTick());
        check(f.animation() == 14370, "Second special starts with quake warning");
        before = f.player.getHitPoints();
        f.steps(4); check(f.player.getHitPoints() == before, "No early quake");
        f.step(); check(f.animation() == 14412 && before - f.player.getHitPoints() >= 300
                && before - f.player.getHitPoints() <= 450, "Alternating quake lands on fifth tick within bounded damage");
        check(!f.player.getAttribute("stunned", false), "Grab stun expires through native callback");
    }
    private static void counterplay() throws Exception {
        for (int mode = 0; mode < 4; mode++) {
            Fixture f = new Fixture(true, false);
            f.until(f.grabTick()); f.npc.setHp(f.npc.getMaxHp() / 2);
            int hp = f.npc.getHp(), before = f.player.getHitPoints();
            Location original = f.npc.getLocation(), victim = f.player.getLocation();
            if (mode == 0) f.player.setLocation(original.transform(12, 0, 0));
            if (mode == 1) f.wall(3222, 256 | 0x20000);
            if (mode == 2) f.wall(3223, 256); // Movement barrier that does not block projectiles.
            if (mode == 3) f.player.setAttribute("godmode", true);
            f.steps(5);
            check(f.npc.getLocation().equals(original), "Pursuit cannot defeat the warning's escape route");
            if (mode != 2) check(f.player.getHitPoints() == before && f.npc.getHp() == hp
                    && !f.player.getAttribute("stunned", false), "Radius, cover and immunity avoid grab effects");
            else check(f.player.getLocation().equals(victim) && f.player.getHitPoints() < before, "Pull cannot cross movement-only wall");
        }
        Fixture f = new Fixture(true, false);
        f.until(f.quakeTick()); int before = f.player.getHitPoints();
        f.player.setLocation(f.npc.getLocation().transform(12, 0, 0));
        f.steps(5); check(f.player.getHitPoints() == before, "Quake has the same visible escape counterplay");
    }
    private static void cancellation() throws Exception {
        for (boolean special : new boolean[]{false, true}) for (int mode = 0; mode < 4; mode++) {
            Fixture f = new Fixture(true, false); f.until(special ? f.grabTick() : 3);
            int before = f.player.getHitPoints();
            if (mode == 0) f.npc.resetCombatState();
            if (mode == 1) f.player.setOnline(false);
            if (mode == 2) f.player.setLocation(f.player.getLocation().transform(0, 0, 1));
            if (mode == 3) f.player.markInstanceTransition();
            f.steps(6);
            check(f.player.getHitPoints() == before && !f.player.getAttribute("stunned", false), "Reset/departure cancels projectile or special and statuses");
        }
    }
    private static void projectiles() throws Exception {
        Fixture f = new Fixture(true, false);
        f.player.setLocation(f.npc.getLocation().transform(15, 0, 0));
        for (int tick = 1; tick <= 20; tick++) {
            f.step();
            int expected = tick < 8 ? 0 : (tick - 8) / f.npc.getAttackDelay() + 1;
            check(f.player.getDamageManager().getHits().size() == expected,
                    "Consecutive fire sessions retain independent five-tick flight at " + tick);
        }
    }
    private static void death() throws Exception {
        for (boolean credited : new boolean[]{false, true}) {
            Fixture f = new Fixture(true, true); f.until(f.grabTick());
            double xp = f.player.getSkills().getXp(Skills.DUNGEONEERING);
            int before = f.player.getHitPoints();
            f.npc.getDamageManager().damage(credited ? f.player : null, f.npc.getMaxHp(), f.npc.getMaxHp(), DamageType.MELEE);
            check(f.npc.isDead(), "Real lethal hit enters ordinary death even without a killer");
            check(f.player.getSkills().getXp(Skills.DUNGEONEERING) == xp
                    + (credited ? 18 * f.player.getSkills().getXpModifierForSkill(Skills.DUNGEONEERING) : 0), "Existing custom Dungeoneering XP hook preserved");
            f.steps(f.npc.getDeathTick());
            LootYk npc = (LootYk)f.npc;
            check(npc.isHidden() && npc.rewards == (credited ? 1 : 0), "Ordinary corpse callback completes one permitted reward");
            check(f.player.getHitPoints() == before, "Death cancels outstanding special");
            f.steps(60);
            check(!npc.isDead() && !npc.isHidden() && npc.getHp() == npc.getMaxHp(), "Normal respawn restores Yk");
            check(npc.rewards == (credited ? 1 : 0), "Respawn does not repeat rewards");
            check(!npc.getAttribute("stunned", false) && npc.getAttribute("freezeTime", -1) <= World.getTicks(), "New life has no old movement lock");
        }
    }
    public static void main(String[] args) throws Exception {
        CombatFixtures.init();
        basics(); projectiles(); rotationAndImpact(); counterplay(); cancellation(); death();
        CombatFixtures.clearPlayers();
        System.out.println("Yk'Lagor combat: " + checks + " checks passed");
    }
}
