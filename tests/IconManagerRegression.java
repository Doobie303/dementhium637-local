import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.dementhium.cache.Cache;
import org.dementhium.content.areas.AreaManager;
import org.dementhium.content.misc.PvpSystem;
import org.dementhium.model.Location;
import org.dementhium.model.World;
import org.dementhium.model.definition.ItemDefinition;
import org.dementhium.model.definition.NPCDefinition;
import org.dementhium.model.definition.PlayerDefinition;
import org.dementhium.model.misc.IconManager;
import org.dementhium.model.misc.IconManager.Icon;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.player.Player;
import org.dementhium.net.GameSession;
import org.dementhium.net.message.Message;
import org.jboss.netty.channel.Channel;

/** Standalone packet/ownership regression; requires an isolated data/cache fixture. */
public class IconManagerRegression {
    private static int checks;
    private static int sequence;

    private static final Map<Player, List<Message>> packets = new IdentityHashMap<Player, List<Message>>();

    private static Player player() {
        List<Message> icons = new ArrayList<Message>();
        Channel channel = (Channel) Proxy.newProxyInstance(Channel.class.getClassLoader(), new Class[]{Channel.class}, (proxy, method, args) -> {
            if (method.getName().equals("isConnected") || method.getName().equals("isOpen")) return true;
            if (method.getName().equals("write") && args[0] instanceof Message) {
                Message message = (Message) args[0];
                if (message.getOpcode() == 0) icons.add(message);
            }
            if (method.getReturnType() == boolean.class) return false;
            if (method.getReturnType() == int.class) return 0;
            return null;
        });
        Player player = new Player(new GameSession(channel), new PlayerDefinition("icon" + (++sequence), "unused"));
        check(World.getWorld().getPlayers().add(player), "fixture player admitted to world");
        player.setOnline(true);
        player.setHasReceivedStarter(true);
        player.setLocation(Location.locate(3100, 3550, 0));
        player.targetLikelihood = 60;
        packets.put(player, icons);
        return player;
    }

    public static void main(String[] args) throws Exception {
        check(Files.isRegularFile(Paths.get(".isolated-quality-fixture")), "isolated fixture required");
        Cache.init();
        ItemDefinition.init();
        NPCDefinition.init();
        Field field = World.class.getDeclaredField("areaManager");
        field.setAccessible(true);
        field.set(World.getWorld(), new AreaManager());
        if (args[0].equals("gaps")) gaps();
        else if (args[0].equals("identity")) identity();
        else if (args[0].equals("full")) full();
        else if (args[0].equals("pvp")) pvp();
        else throw new IllegalArgumentException(args[0]);
        System.out.println("PASS icons-" + args[0] + ": " + checks + " checks");
    }

    private static void gaps() {
        Player viewer = player();
        Player target = player();
        Location location = Location.locate(3200, 3200, 0);
        check(IconManager.iconOnCoordinate(viewer, location, 1, -1) == 0, "first free slot");
        check(IconManager.iconOnMob(viewer, target, 4, -1) == 1, "second free slot");
        IconManager.removeIcon(viewer, location);
        packet(viewer, 0, 0);
        IconManager.removeIcon(viewer, target);
        check(icon(viewer, 1) == null, "mob removed beyond earlier hole");
        packet(viewer, 1, 0);
        int sent = packets.get(viewer).size();
        IconManager.removeIcon(viewer, target);
        check(packets.get(viewer).size() == sent, "repeated removal sends no packet");

        IconManager.iconOnMob(viewer, target, 4, -1);
        IconManager.iconOnCoordinate(viewer, location, 1, -1);
        IconManager.removeIcon(viewer, target);
        check(IconManager.iconOnCoordinate(viewer, location, 3, 42) == 1, "existing coordinate updated beyond hole");
        check(icon(viewer, 0) == null && icon(viewer, 1).getModelId() == 42, "update creates no duplicate");
        IconManager.removeIcon(viewer, location);
        check(icon(viewer, 1) == null, "coordinate removed beyond hole");
        packet(viewer, 1, 0);
    }

    private static void identity() {
        Player viewer = player();
        Player target = player();
        NPC npc = new NPC(1);
        npc.setIndex(target.getIndex());
        check(IconManager.iconOnMob(viewer, target, 4, -1) == 0, "player icon installed");
        check(IconManager.iconOnMob(viewer, npc, 1, -1) == 1, "NPC with same index has separate icon");
        packet(viewer, 1, 1);
        IconManager.removeIcon(viewer, npc);
        check(icon(viewer, 0).getTargetType() == 10 && icon(viewer, 1) == null, "NPC removal preserves player icon");
        check(IconManager.iconOnMob(viewer, target, 3, 42) == 0, "same player reuses its slot");
        packet(viewer, 0, 10);
    }

    private static void full() {
        Player viewer = player();
        for (int i = 0; i < IconManager.MAX_ICONS; i++) {
            check(IconManager.iconOnCoordinate(viewer, Location.locate(3200 + i, 3200, 0), 1, -1) == i,
                    "fills slot " + i);
        }
        Icon first = icon(viewer, 0);
        int sent = packets.get(viewer).size();
        IconManager.removeIcon(viewer, Location.locate(3300, 3300, 0));
        IconManager.removeIcon(viewer, player());
        check(icon(viewer, 0) == first && packets.get(viewer).size() == sent, "absent target removal cannot evict occupied slot");
        check(IconManager.iconOnCoordinate(viewer, Location.locate(3400, 3400, 0), 3, 42) == 0,
                "existing full-capacity slot-zero replacement policy preserved");
    }

    private static void pvp() {
        Player a = player(), b = player();
        Location marker = Location.locate(3200, 3200, 0);
        IconManager.iconOnCoordinate(a, marker, 1, -1);
        check(PvpSystem.pair(a, b), "production target pairing");
        check(icon(a, 1).getIndex() == b.getIndex(), "target arrow follows pre-existing marker");
        IconManager.removeIcon(a, marker);
        PvpSystem.depart(a);
        check(a.target == null && b.target == null && !a.hasTargetArrow && !b.hasTargetArrow, "departure clears pair state");
        check(icon(a, 1) == null && icon(b, 0) == null, "departure actually clears both stored target icons");
        packet(a, 1, 0);
        packet(b, 0, 0);
        check(a.targetLikelihood == 5 && b.targetLikelihood == 60, "custom departure progress policy retained");
        int sent = packets.get(a).size() + packets.get(b).size();
        PvpSystem.depart(a);
        check(packets.get(a).size() + packets.get(b).size() == sent, "repeated departure does not clear unrelated icons");
    }

    private static Icon icon(Player player, int slot) { return player.getAttribute("icon_slot" + slot); }

    private static void packet(Player player, int slot, int type) {
        List<Message> icons = packets.get(player);
        Message message = icons.get(icons.size() - 1);
        check(message.getBuffer().getUnsignedByte(0) == ((slot << 5) | type), "encoded hint packet slot/type " + slot + "/" + type);
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
