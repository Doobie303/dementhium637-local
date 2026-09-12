import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.dementhium.cache.Cache;
import org.dementhium.content.Commands;
import org.dementhium.content.dialogue.Dialogue;
import org.dementhium.content.dialogue.DialogueType;
import org.dementhium.content.areas.AreaManager;
import org.dementhium.content.interfaces.StaffTools;
import org.dementhium.model.Container;
import org.dementhium.model.Item;
import org.dementhium.model.Location;
import org.dementhium.model.World;
import org.dementhium.model.definition.ItemDefinition;
import org.dementhium.model.definition.PlayerDefinition;
import org.dementhium.model.player.Player;
import org.dementhium.net.GameSession;
import org.dementhium.net.message.Message;
import org.dementhium.util.BufferUtils;
import org.dementhium.util.InputHandler;
import org.dementhium.util.handlers.DisplayNamesHandler;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;

/** Focused authorization, stale-state, and item-picker checks. */
public final class StaffToolsRegression {
    private static int checks;
    private static int sequence;
    private static final Map<Player, List<Message>> messages = new IdentityHashMap<Player, List<Message>>();

    private static void check(boolean value, String label) {
        checks++;
        if (!value) throw new AssertionError(label);
    }

    private static Player p() {
        final List<Message> sent = new ArrayList<Message>();
        Channel channel = (Channel) Proxy.newProxyInstance(Channel.class.getClassLoader(), new Class[] { Channel.class },
                (proxy, method, args) -> {
                    if (method.getName().equals("isConnected") || method.getName().equals("isOpen")) return true;
                    if (method.getName().equals("write") && args[0] instanceof Message) sent.add((Message) args[0]);
                    if (method.getReturnType() == boolean.class) return false;
                    if (method.getReturnType() == int.class) return 0;
                    return null;
                });
        Player player = new Player(new GameSession(channel), new PlayerDefinition("staff" + (++sequence), "unused"));
        player.setOnline(true);
        player.setHasReceivedStarter(true);
        player.setLocation(Location.locate(3200, 3200, 0));
        World.getWorld().getPlayers().add(player);
        messages.put(player, sent);
        return player;
    }

    private static Map<Integer, String> text(Player player, int interfaceId) {
        Map<Integer, String> result = new java.util.HashMap<Integer, String>();
        for (Message message : messages.get(player)) if (message.getOpcode() == 33) {
            ChannelBuffer buffer = message.getBuffer().duplicate();
            String value = BufferUtils.readRS2String(buffer);
            int hash = buffer.readUnsignedByte() | buffer.readUnsignedByte() << 8
                    | buffer.readUnsignedByte() << 16 | buffer.readUnsignedByte() << 24;
            if (hash >>> 16 == interfaceId) result.put(hash & 65535, value);
        }
        return result;
    }

    private static long count(Container container, int itemId) {
        long count = 0;
        for (Item item : container.toArray()) if (item != null && item.getId() == itemId) count += item.getAmount();
        return count;
    }

    private static int chatCount(Player player) {
        int count = 0;
        for (Message message : messages.get(player)) if (message.getOpcode() == 53) count++;
        return count;
    }

    private static int interfaceCloseCount(Player player, int window, int slot) {
        int count = 0;
        int expected = window << 16 | slot;
        for (Message message : messages.get(player)) if (message.getOpcode() == 61) {
            ChannelBuffer buffer = message.getBuffer().duplicate();
            int hash = buffer.readUnsignedByte() | buffer.readUnsignedByte() << 8
                    | buffer.readUnsignedByte() << 16 | buffer.readUnsignedByte() << 24;
            if (hash == expected) count++;
        }
        return count;
    }
    private static void rights(Player player, int rights) {
        player.getDefinition().setRights(rights);
        player.getConnection().readClientSettings("|gambler-ui=1|staff-tools=1");
    }

    private static void click(Player player, int child) {
        StaffTools.button(player, StaffTools.INTERFACE, child, 6);
    }

    public static void main(String[] args) throws Exception {
        Cache.init();
        ItemDefinition.init();
        new DisplayNamesHandler();
        Field areas = World.class.getDeclaredField("areaManager");
        areas.setAccessible(true);
        areas.set(World.getWorld(), new AreaManager());

        GameSession capabilities = new GameSession(null);
        capabilities.readClientSettings("|gambler-ui=1|staff-tools=1");
        check(capabilities.supportsGamblerInterface() && capabilities.supportsStaffToolsInterface(), "capabilities coexist in either position");
        capabilities.readClientSettings("|gambler-ui=10|staff-tools=1-extra|");
        check(!capabilities.supportsGamblerInterface() && !capabilities.supportsStaffToolsInterface(), "partial capability markers are rejected");

        Player ordinary = p();
        rights(ordinary, 0);
        StaffTools.installTab(ordinary);
        check(!StaffTools.isInstalled(ordinary), "ordinary player cannot install staff tab");
        click(ordinary, 22);
        check(ordinary.getAttribute("staffToolsPending") == null, "forged ordinary click creates no state");

        Player oldClient = p();
        oldClient.getDefinition().setRights(2);
        StaffTools.installTab(oldClient);
        check(!StaffTools.isInstalled(oldClient), "staff on an old client keeps empty slot");

        Player moderator = p();
        rights(moderator, 1);
        StaffTools.installTab(moderator);
        check(StaffTools.isInstalled(moderator), "moderator with capability installs tab");
        check("Go to player".equals(text(moderator, StaffTools.INTERFACE).get(22)), "assist page is populated");
        click(moderator, 12);
        check("ADMIN - LOCKED".equals(text(moderator, StaffTools.INTERFACE).get(13)), "moderator sees admin boundary");
        check("".equals(text(moderator, StaffTools.INTERFACE).get(22)), "moderator receives no admin action");

        click(moderator, 10);
        click(moderator, 22);
        check(((Integer) moderator.getAttribute("inputId", -1)).intValue() == StaffTools.STRING_INPUT, "moderation action requests a name");
        StaffTools.clearPending(moderator);
        InputHandler.handleStringInput(moderator, "stale target");
        check(moderator.getAttribute("staffToolsPending") == null, "cancelled input cannot recreate pending action");

        click(moderator, 11);
        click(moderator, 23);
        check(((Integer) moderator.getAttribute("inputId", -1)).intValue() == StaffTools.INTEGER_INPUT, "coordinate flow starts numeric input");
        InputHandler.handleIntegerInput(moderator, -1);
        check(moderator.getAttribute("staffToolsPending") == null, "invalid coordinate consumes pending action");

        Player departingTarget = p();
        click(moderator, 10);
        click(moderator, 25);
        InputHandler.handleStringInput(moderator, departingTarget.getDisplayName());
        check(moderator.getAttribute("staffToolsPending") != null, "online ban target reaches confirmation");
        World.getWorld().getPlayers().remove(departingTarget);
        departingTarget.setOnline(false);
        click(moderator, 22);
        check(moderator.getAttribute("staffToolsPending") == null, "offline-before-confirm ban is cancelled");

        Player admin = p();
        rights(admin, 2);
        StaffTools.installTab(admin);
        click(admin, 12);
        check("Spawn item".equals(text(admin, StaffTools.INTERFACE).get(28)), "item spawn is on first admin page");
        click(admin, 27);
        check(Boolean.TRUE.equals(admin.getAttribute("godmode")), "God Mode button reuses existing command");

        click(admin, 28);
        check(admin.getAttribute("staffToolsPending") != null, "item picker owns explicit pending state");
        int searchCloseBefore = interfaceCloseCount(admin, 752, 7);
        int chatboxCloseBefore = interfaceCloseCount(admin, 752, 13);
        boolean externalConsumed = StaffTools.button(admin, 149, 0, 6);
        check(!externalConsumed && admin.getAttribute("staffToolsPending") == null
                && interfaceCloseCount(admin, 752, 7) == searchCloseBefore + 1
                && interfaceCloseCount(admin, 752, 13) == chatboxCloseBefore + 1,
                "external interface click closes both the GE child and expanded chatbox without being consumed");
        click(admin, 28);
        check(!StaffTools.handleItemSearchSelection(moderator, 995), "unrelated GE selection is not consumed");
        check(StaffTools.handleItemSearchSelection(admin, 995), "staff item selection is consumed");
        check(((Integer) admin.getAttribute("inputId", -1)).intValue() == StaffTools.INTEGER_INPUT, "selected item requests amount");
        InputHandler.handleIntegerInput(admin, 2);
        Dialogue confirmation = admin.getAttribute("dialogue");
        check(confirmation != null && confirmation.getType() == DialogueType.OPTION
                && confirmation.getMessage().size() == 2
                && confirmation.getMessage().get(0).contains("Coins x 2"), "stackable item uses modal spawn confirmation without a note choice");
        long before = count(admin.getInventory().getContainer(), 995);
        confirmation.getActions().get(0).handle(admin);
        check(count(admin.getInventory().getContainer(), 995) == before + 2, "confirmed item command executes once");
        confirmation.getActions().get(0).handle(admin);
        check(count(admin.getInventory().getContainer(), 995) == before + 2, "duplicate confirmation cannot spawn twice");

        click(admin, 28);
        check(StaffTools.handleItemSearchSelection(admin, 4151), "notable item selection is consumed");
        InputHandler.handleIntegerInput(admin, 3);
        Dialogue notedConfirmation = admin.getAttribute("dialogue");
        check(notedConfirmation != null && notedConfirmation.getMessage().size() == 3
                && notedConfirmation.getMessage().get(1).contains("Spawn noted Abyssal whip x 3"), "notable item offers an explicit noted form");
        long notedBefore = count(admin.getInventory().getContainer(), 4152);
        notedConfirmation.getActions().get(1).handle(admin);
        check(count(admin.getInventory().getContainer(), 4152) == notedBefore + 3, "noted selection spawns the validated note variant");

        click(admin, 28);
        StaffTools.handleItemSearchSelection(admin, 4151);
        InputHandler.handleIntegerInput(admin, 1);
        check(admin.getAttribute("staffToolsPending") != null, "spawn modal owns pending state until answered");
        click(admin, 9);
        check(admin.getAttribute("staffToolsPending") == null
                && "ASSIST".equals(text(admin, StaffTools.INTERFACE).get(13)), "panel remains usable after a dismissed spawn modal");
        click(admin, 12);

        click(admin, 28);
        admin.closeAll(false, true);
        check(admin.getAttribute("staffToolsPending") == null, "interface closure clears pending picker");
        check(!StaffTools.handleItemSearchSelection(admin, 4151), "closed picker cannot intercept future GE selection");
        int chatBefore = chatCount(admin);
        Commands.handle(admin, new String[] { "coords" });
        check(chatCount(admin) > chatBefore, "typed command dispatcher remains available");

        World.getWorld().getPlayers().remove(ordinary);
        World.getWorld().getPlayers().remove(oldClient);
        World.getWorld().getPlayers().remove(moderator);
        World.getWorld().getPlayers().remove(admin);
        System.out.println("PASS: " + checks + " staff tools checks");
    }
}
