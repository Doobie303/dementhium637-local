import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.dementhium.model.Item;
import org.dementhium.model.Location;
import org.dementhium.model.World;
import org.dementhium.model.combat.CombatType;
import org.dementhium.model.combat.Damage;
import org.dementhium.model.misc.DamageManager.DamageHit;
import org.dementhium.model.misc.DamageManager.DamageType;
import org.dementhium.model.npc.impl.OrdinaryMixedNPC;
import org.dementhium.model.npc.NPCLoader;
import org.dementhium.model.player.Player;
import org.dementhium.task.impl.NPCTickTask;
import org.dementhium.tickable.Tick;

/** Native task/executor sessions plus isolated protection-table checks; no server or account saves. */
public final class NPCMixedCombatRegression {
    private static int checks;
    private static boolean registryMode;
    private static final List<Tick> active = new ArrayList<Tick>();
    private static void check(boolean value, String why) {
        checks++;
        if (!value) throw new AssertionError(why);
    }
    private static Field field(Class<?> type, String name) throws Exception {
        Field value = type.getDeclaredField(name); value.setAccessible(true); return value;
    }
    @SuppressWarnings("unchecked") private static void worldCallbacks() throws Exception {
        LinkedList<Tick> queue = (LinkedList<Tick>)field(World.class, "ticksToAdd").get(World.getWorld());
        active.addAll(queue); queue.clear();
        for (Iterator<Tick> it = active.iterator(); it.hasNext();) if (!it.next().run()) it.remove();
    }
    private static final class Fixture {
        final OrdinaryMixedNPC npc;
        final Player player;
        Fixture(int id, boolean distant) throws Exception {
            CombatFixtures.clearPlayers();
            active.clear();
            ((List<?>)field(World.class, "ticksToAdd").get(World.getWorld())).clear();
            npc = registryMode ? (OrdinaryMixedNPC)NPCLoader.getNPC(id) : new OrdinaryMixedNPC(id);
            npc.setLocation(Location.locate(3216, 3216, 0));
            npc.setOriginalLocation(npc.getLocation()); npc.setDoesWalk(false);
            npc.getRandom().setSeed(923);
            player = CombatFixtures.player(npc);
            player.getSkills().setMaximumLifePoints(100000);
            player.getSkills().setHitPoints(100000);
            if (distant) player.setLocation(npc.getLocation().transform(8, 0, 0));
            npc.getCombatExecutor().setVictim(player);
        }
        void step() throws Exception {
            field(World.class, "ticksPassed").setInt(null, World.getTicks() + 1);
            worldCallbacks();
            npc.getMask().reset();
            new NPCTickTask(npc).execute();
        }
        void steps(int count) throws Exception { for (int i = 0; i < count; i++) step(); }
    }
    private static void naturalSelection() throws Exception {
        for (int id : new int[]{5363, 5362, 3068, 13465, 5361, 11633, 10770}) {
            Fixture f = new Fixture(id, false);
            // Low defence makes delivered-style coverage independent of untouched historical stat calibration.
            f.player.getSkills().set(org.dementhium.model.player.Skills.DEFENCE, 1);
            f.player.getSkills().set(org.dementhium.model.player.Skills.MAGIC, 1);
            EnumSet<DamageType> actual = EnumSet.noneOf(DamageType.class);
            List<Integer> launches = new ArrayList<Integer>();
            for (int tick = 1; tick <= 180; tick++) {
                f.step();
                if (f.npc.getMask().getLastAnimation() != null
                        && f.npc.getMask().getLastAnimation().getId() != f.npc.getDefenceAnimation()) launches.add(tick);
                for (DamageHit hit : f.player.getDamageManager().getHits()) actual.add(hit.getType());
            }
            check(!launches.isEmpty() && launches.get(0) == 3, "Initial executor cooldown: " + id);
            for (int index = 1; index < launches.size(); index++)
                check(launches.get(index) - launches.get(index - 1) == f.npc.getAttackDelay(), "Natural cadence: " + id);
            if (id != 5361) check(actual.contains(DamageType.MELEE), "Natural contact melee: " + id);
            else check(!actual.contains(DamageType.MELEE), "Waterfiend never chooses melee");
            if (id != 11633 && id != 10770 && id != 5363 && id != 5362) check(actual.contains(DamageType.RANGE), "Natural ranged selection: " + id);
            if (id == 5362) check(!actual.contains(DamageType.RANGE), "Brutal green uses melee, magic and fire");
            if (id == 5363) check(!actual.contains(DamageType.RANGE), "Mithril uses ranged only outside melee contact");
            if (id != 3068) check(actual.contains(DamageType.MAGE), "Natural magic selection: " + id);
            if (id == 5363 || id == 5362 || id == 3068 || id == 11633 || id == 10770)
                check(actual.contains(DamageType.RED_DAMAGE), "Natural breath selection: " + id);
        }
    }
    private static void projectileOwnership() throws Exception {
        for (int mode = 0; mode < 5; mode++) {
            Fixture f = new Fixture(5361, true);
            f.steps(3);
            check(f.npc.getMask().getLastAnimation() != null, "Projectile launched through ordinary task");
            check(f.player.getDamageManager().getHits().isEmpty(), "No damage on projectile launch");
            if (mode == 1) { f.npc.resetCombatState(); f.npc.resetCombat(); }
            if (mode == 2) f.npc.setDead(true);
            if (mode == 3) f.player.setLocation(f.player.getLocation().transform(0, 0, 1));
            if (mode == 4) f.player.markInstanceTransition();
            f.steps(2);
            check(f.player.getDamageManager().getHits().isEmpty(), "No early/stale projectile impact");
            f.step();
            check(f.player.getDamageManager().getHits().size() == (mode == 0 ? 1 : 0), "Due impact once or cancellation: " + mode);
        }
    }
    private static void contactChangesBeforeLaunch() throws Exception {
        for (boolean initiallyDistant : new boolean[]{false, true}) {
            Fixture f = new Fixture(5363, initiallyDistant);
            f.npc.getRandom().setSeed(0);
            f.step(); // Production follow prepares a style while initial cooldown is still active.
            check(f.npc.getCombatAction().getCombatType() == (initiallyDistant ? CombatType.RANGE : CombatType.MELEE),
                    "Natural first preparation covers a distance-dependent Mithril style");
            Location origin = f.npc.getLocation();
            f.player.setLocation(origin.transform(initiallyDistant ? f.npc.size() : 8, 0, 0));
            CombatType revised = f.npc.getCombatAction().getCombatType();
            check(revised != (initiallyDistant ? CombatType.RANGE : CombatType.MELEE),
                    "Same-target contact change invalidates an ineligible prepared style");
            check(f.npc.getCombatAction().getCombatType() == revised,
                    "Unchanged contact keeps the replacement preparation stable");
            f.steps(2);
            check(f.npc.getMask().getLastAnimation() != null && f.npc.getLocation().equals(origin),
                    "Updated style launches on the original due tick without unnecessary pursuit");
            f.steps(3);
            check(f.player.getDamageManager().getHits().size() == 1,
                    "Revised preparation delivers one normal executor impact");
        }
    }
    private static void revenantHealing() throws Exception {
        Fixture f = new Fixture(13479, true);
        f.npc.setHp(100);
        f.npc.getPoisonManager().poison(f.player, 40);
        f.steps(3);
        check(f.npc.getHp() == 100 + f.npc.getMaxHp() / 5, "Revenant heals instead of launching damage");
        check(!f.npc.getPoisonManager().isPoisoned(), "Healing consumes one poison cure");
        check(f.player.getDamageManager().getHits().isEmpty(), "Healing cycle has no attack");
        check(field(OrdinaryMixedNPC.class, "healsRemaining").getInt(f.npc) == 4, "Bounded heal supply consumed");
        f.npc.setHp(100);
        f.steps(f.npc.getAttackDelay() - 1);
        check(f.npc.getHp() == 100, "Healing observes the normal attack interval");
        f.step();
        check(f.npc.getHp() > 100, "Next heal is due on the normal interval");
        for (int i = 0; i < 3; i++) { f.npc.setHp(100); f.steps(f.npc.getAttackDelay()); }
        check(field(OrdinaryMixedNPC.class, "healsRemaining").getInt(f.npc) == 0, "Finite heal budget exhausts");
        f.npc.setHp(100); f.steps(f.npc.getAttackDelay());
        check(f.npc.getHp() == 100, "Exhausted revenant resumes attacking without healing");
        f.npc.resetCombatState();
        check(field(OrdinaryMixedNPC.class, "healsRemaining").getInt(f.npc) == 5, "New life/leash resets family state");
    }
    private static void overlappingSessions() throws Exception {
        Fixture f = new Fixture(11633, true);
        f.player.setLocation(f.npc.getLocation().transform(15, 0, 0));
        // Five ticks of flight overlaps (or coincides with) the next ordinary frost launch.
        int interval = f.npc.getAttackDelay();
        for (int tick = 1; tick <= 20; tick++) {
            f.step();
            int expected = tick < 8 ? 0 : (tick - 8) / interval + 1;
            check(f.player.getDamageManager().getHits().size() == expected,
                    "Overlapping launches retain independent due times at tick " + tick);
        }
    }
    private static void revenantPrayers() throws Exception {
        for (CombatType protectedType : new CombatType[]{CombatType.RANGE, CombatType.MAGIC}) {
            Fixture f = new Fixture(13479, true);
            boolean[][] prayers = (boolean[][])field(f.player.getPrayer().getClass(), "onPrayers").get(f.player.getPrayer());
            prayers[0][protectedType.getProtectionPrayer()] = true;
            f.steps(3);
            f.steps(3);
            check(f.player.getDamageManager().getHits().size() == 1, "Adaptive revenant fires normally");
            DamageType actual = f.player.getDamageManager().getHits().getFirst().getType();
            check(actual == (protectedType == CombatType.MAGIC ? DamageType.RANGE : DamageType.MAGE)
                    || actual == DamageType.MISS, "Revenant avoids the current protection prayer");
        }
    }
    private static void iceProtection() throws Exception {
        Fixture f = new Fixture(3068, true);
        Method breath = OrdinaryMixedNPC.class.getDeclaredMethod("iceBreath", org.dementhium.model.Mob.class, int.class);
        breath.setAccessible(true);
        for (int shield : new int[]{-1, 1540, 2890, 9731, 11283, 18584, 18691}) {
            f.player.getEquipment().set(5, shield < 0 ? null : new Item(shield));
            f.player.setAttribute("antiFire", System.currentTimeMillis());
            f.player.setAttribute("santiFire", System.currentTimeMillis());
            Damage damage = (Damage)breath.invoke(f.npc, f.player, 500);
            check(damage.getHit() == (shield < 0 || shield == 1540 || shield == 18584 ? 500 : 140), "Wyvern shield/potion distinction: " + shield);
        }
        f.player.setAttribute("godmode", true);
        check(((Damage)breath.invoke(f.npc, f.player, 500)).getHit() == 0, "God mode still protects from ice breath");
        f.player.getSkills().set(org.dementhium.model.player.Skills.SLAYER, 71);
        check(!f.npc.isAttackable(f.player), "Wyvern requires the documented Slayer level");
        f.player.getSkills().set(org.dementhium.model.player.Skills.SLAYER, 72);
        check(f.npc.isAttackable(f.player), "Wyvern admits the Slayer-level boundary");
    }
    private static void death() throws Exception {
        Fixture f = new Fixture(5363, true);
        f.steps(3);
        f.npc.getDamageManager().damage(null, f.npc.getMaxHp(), f.npc.getMaxHp(), DamageType.MELEE);
        check(f.npc.isDead(), "Actual lethal damage enters ordinary death");
        f.steps(f.npc.getDeathTick());
        check(f.npc.isHidden(), "Ordinary corpse callback hides NPC");
        check(f.player.getDamageManager().getHits().isEmpty(), "Death invalidates outstanding attack");
        f.steps(60);
        check(!f.npc.isDead() && !f.npc.isHidden() && f.npc.getHp() == f.npc.getMaxHp(), "Ordinary respawn restores combat NPC");
    }
    private static void distantMithril() throws Exception {
        Fixture f = new Fixture(5363, true);
        f.player.getSkills().set(org.dementhium.model.player.Skills.DEFENCE, 1);
        f.player.getSkills().set(org.dementhium.model.player.Skills.MAGIC, 1);
        f.steps(180);
        EnumSet<DamageType> actual = EnumSet.noneOf(DamageType.class);
        for (DamageHit hit : f.player.getDamageManager().getHits()) actual.add(hit.getType());
        check(actual.contains(DamageType.RANGE) && actual.contains(DamageType.MAGE)
                && actual.contains(DamageType.RED_DAMAGE) && !actual.contains(DamageType.MELEE), "Distant mithril selects its three projectiles");
        check(f.npc.updateHit(f.player, 500, CombatType.DRAGONFIRE).getHit() == 0, "Mithril shares metal-dragon fire immunity");
        Fixture frost = new Fixture(11633, true);
        check(frost.npc.updateHit(frost.player, 500, CombatType.DRAGONFIRE).getHit() == 0, "Ordinary frost shares existing fire immunity");
    }
    private static void registry() {
        for (int id : new int[]{5363,5362,3068,3069,3070,3071,5361,9054,9056,9057,9058,9059,
                9061,9062,9063,9064,11633,11634,11635,11636,
                10770,10771,10772,10773,10774,10775,
                13465,13466,13467,13468,13469,13470,13471,13472,13473,13474,13475,13476,13477,13478,13479,13480,13481})
            check(NPCLoader.getNPC(id) instanceof OrdinaryMixedNPC, "Production registry resolves mixed family " + id);
        check(NPCLoader.getNPC(51) instanceof org.dementhium.model.npc.impl.FrostDragon, "Existing arena frost handler preserved");
        for (int id : new int[]{494,8424,9078})
            check(!(NPCLoader.getNPC(id) instanceof OrdinaryMixedNPC), "Non-combat/display NPC excluded: " + id);
    }
    private static void frostTiers() throws Exception {
        Method maximum = OrdinaryMixedNPC.class.getDeclaredMethod("maximum", CombatType.class);
        maximum.setAccessible(true);
        int[][] expected = {{124,140,333},{155,172,410},{186,204,487},{214,250,595},{214,250,595},{214,250,595}};
        CombatType[] styles = {CombatType.MELEE, CombatType.MAGIC, CombatType.DRAGONFIRE};
        for (int tier = 0; tier < expected.length; tier++) {
            Fixture f = new Fixture(10770 + tier, true);
            for (int style = 0; style < styles.length; style++)
                check((Integer)maximum.invoke(f.npc, styles[style]) == expected[tier][style],
                        "Explicit frost tier damage budget: " + f.npc.getId() + "/" + styles[style]);
            f.steps(90);
            for (DamageHit hit : f.player.getDamageManager().getHits())
                if (hit.getType() == DamageType.MAGE || hit.getType() == DamageType.RED_DAMAGE)
                    check(hit.getDamage() <= expected[tier][hit.getType() == DamageType.MAGE ? 1 : 2],
                            "Natural frost impact respects tier cap");
        }
    }
    public static void main(String[] args) throws Exception {
        CombatFixtures.init();
        registryMode = args.length > 0 && "registry".equals(args[0]);
        if (registryMode) registry();
        naturalSelection(); distantMithril(); contactChangesBeforeLaunch(); overlappingSessions(); projectileOwnership();
        revenantHealing(); revenantPrayers(); iceProtection(); frostTiers(); death();
        CombatFixtures.clearPlayers();
        System.out.println("NPC mixed combat: " + checks + " checks passed");
    }
}



