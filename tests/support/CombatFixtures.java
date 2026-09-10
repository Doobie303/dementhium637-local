import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.dementhium.cache.Cache;
import org.dementhium.content.areas.AreaManager;
import org.dementhium.model.Location;
import org.dementhium.model.World;
import org.dementhium.model.definition.ItemDefinition;
import org.dementhium.model.definition.NPCDefinition;
import org.dementhium.model.map.Region;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.npc.NPCLoader;
import org.dementhium.model.player.Player;
import org.dementhium.model.definition.PlayerDefinition;
import org.dementhium.model.player.Skills;
import org.dementhium.net.GameSession;

/** Test-only setup for standalone headless combat suites. Never stage into runtime. */
public final class CombatFixtures {
    private static final List<Player> players = new ArrayList<Player>();
    private static boolean initialized;
    private CombatFixtures() {}

    public static void init() throws Exception {
        if (initialized) return;
        Cache.init();
        ItemDefinition.init();
        NPCDefinition.init();
        Field field = World.class.getDeclaredField("areaManager");
        field.setAccessible(true);
        field.set(World.getWorld(), new AreaManager());
        Method loader = NPCLoader.class.getDeclaredMethod("loadCustomizations");
        loader.setAccessible(true);
        loader.invoke(null);
        Region region = Region.forCoords(3200, 3200);
        region.clippingMasks = new int[4][128][128];
        region.setClipped(true);
        initialized = true;
    }

    /** Online, connected, ordinary level-99 player; no godmode or World-list admission. */
    public static Player player(NPC npc) {
        Player player = new Player(new GameSession(null) {
            @Override public boolean isDisconnected() { return false; }
        }, new PlayerDefinition("advanced-test", "unused"));
        for (int skill : new int[] {Skills.ATTACK, Skills.STRENGTH, Skills.DEFENCE,
                Skills.RANGED, Skills.MAGIC, Skills.PRAYER}) {
            player.getSkills().setLevelAndXP(skill, 99, 13034431);
        }
        player.getSkills().setMaximumLifePoints(1000);
        player.getSkills().setHitPoints(1000);
        player.getSkills().setPrayerPoints(99, false);
        player.getBonuses().calculate();
        player.setHasReceivedStarter(true);
        player.setOnline(true);
        player.setLocation(npc.getLocation().transform(npc.size(), 0, 0));
        player.getRandom().setSeed(903);
        players.add(player);
        return player;
    }

    /** Only players created by this helper are affected. */
    public static void clearPlayers() {
        for (Player player : players) {
            player.setOnline(false);
            World.getWorld().getPlayers().remove(player);
        }
        players.clear();
    }

    /** Direct action tests must explicitly bypass the executor's initial three-tick cooldown. */
    public static void readyAttack(NPC npc, Player target) {
        npc.getCombatExecutor().setVictim(target);
        npc.getCombatExecutor().setTicks(0);
    }
}

