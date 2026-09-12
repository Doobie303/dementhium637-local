import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.netty.channel.Channel;
import org.dementhium.cache.Cache;
import org.dementhium.model.Item;
import org.dementhium.model.definition.ItemDefinition;
import org.dementhium.model.definition.PlayerDefinition;
import org.dementhium.model.player.Player;
import org.dementhium.net.ActionSender;
import org.dementhium.net.GameSession;
import org.dementhium.net.message.Message;
import org.dementhium.net.message.MessageBuilder;
import org.dementhium.net.packethandlers.ActionButtonHandler;
import org.dementhium.net.handler.DementhiumHandler;
import org.dementhium.task.impl.PlayerTickTask;

/** Covers the client-script tab transition without re-sending the cache-default bank title. */
public final class BankTabRegression {
    private static int checks;
    private static final Map<Integer, String> strings = new HashMap<Integer, String>();
    private static final List<String> channelCalls = new ArrayList<String>();
    private static final Map<Integer, Integer> varcs = new HashMap<Integer, Integer>();
    private static final Map<Integer, Integer> configs = new HashMap<Integer, Integer>();
    private static final Map<Integer, Boolean> visible = new HashMap<Integer, Boolean>();
    private static final List<String> events = new ArrayList<String>();

    private static int intV2(Message m) {
        int a = m.readByte() & 255, b = m.readByte() & 255;
        int c = m.readByte() & 255, d = m.readByte() & 255;
        return (b << 24) | (a << 16) | (d << 8) | c;
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static Player player() {
        Channel channel = (Channel) Proxy.newProxyInstance(Channel.class.getClassLoader(), new Class[] { Channel.class },
                (proxy, method, arguments) -> {
                    channelCalls.add(method.getName());
                    if (method.getName().equals("isConnected") || method.getName().equals("isOpen")) return true;
                    if (method.getName().equals("write") && arguments[0] instanceof Message) {
                        Message message = (Message) arguments[0];
                        if (message.getOpcode() == 33) {
                            String value = message.readRS2String();
                            strings.put(message.readLEInt(), value);
                        }
                        if (message.getOpcode() == 51) varcs.put(message.readLEShort(), (int) message.readByte());
                        if (message.getOpcode() == 5) varcs.put(message.readLEShortA(), message.readLEInt());
                        if (message.getOpcode() == 21) configs.put(message.readShortA(), message.readByteS() & 255);
                        if (message.getOpcode() == 27) {
                            int c = message.readByte() & 255, d = message.readByte() & 255;
                            int a = message.readByte() & 255, b = message.readByte() & 255;
                            configs.put(message.readShort(), (a << 24) | (b << 16) | (c << 8) | d);
                        }
                        if (message.getOpcode() == 3) visible.put(intV2(message), message.readByteC() == 0);
                        if (message.getOpcode() == 113) events.add("items:" + message.readShort());
                        if (message.getOpcode() == 50) {
                            intV2(message);
                            events.add("open:" + message.readLEShortA());
                        }
                        if (message.getOpcode() == 119) {
                            intV2(message); message.readShortA(); message.readShortA();
                            events.add("mask:" + message.readLEInt());
                        }
                    }
                    if (method.getReturnType() == boolean.class) return false;
                    if (method.getReturnType() == int.class) return 0;
                    return null;
                });
        return new Player(new GameSession(channel), new PlayerDefinition("bank-tab-test", "unused"));
    }

    public static void main(String[] args) throws Exception {
        Cache.init();
        ItemDefinition.init();
        Player player = player();
        player.setHasReceivedStarter(true);
        ActionSender.sendString(player, 762, 31, "direct");
        check("direct".equals(strings.get((762 << 16) | 31)), "Fixture captures client string packets: " + channelCalls);
        player.getBank().set(0, new Item(1511));
        player.getBank().set(1, new Item(995, 250));
        player.getBank().set(2, new Item(11732));
        // Exercise production packet-before-player-ticks ordering, with the
        // packet callback opening the bank once. No live client is simulated.
        Field handler = Player.class.getDeclaredField("handler");
        handler.setAccessible(true);
        handler.set(player, new DementhiumHandler() {
            private boolean opened;
            @Override public void processPackets(Player p) {
                if (!opened) {
                    opened = true;
                    p.getBank().openBank();
                    check(!varcs.containsKey(192) && !varcs.containsKey(1038) && !varcs.containsKey(1324),
                            "Bank open must not trigger native split-count script 1465");
                    check(Boolean.TRUE.equals(visible.get((762 << 16) | 22)), "Counter parent shown on first open");
                    check(events.contains("items:93"), "Bank inventory uses native container 93");
                    check(events.indexOf("mask:" + ((762 << 16) | 93)) > events.indexOf("open:762")
                            && events.indexOf("mask:" + (763 << 16)) > events.indexOf("open:763"),
                            "Access masks follow attachment of their panels");
                }
            }
        });
        PlayerTickTask cycle = new PlayerTickTask(player);
        cycle.execute();
        check("2".equals(strings.get((762 << 16) | 29)), "First open counts free-to-play item slots: " + strings);
        check("68".equals(strings.get((762 << 16) | 30)), "First open shows free bank capacity: " + strings);
        check("3".equals(strings.get((762 << 16) | 31)), "First open shows total used bank slots immediately: " + strings);
        check("516".equals(strings.get((762 << 16) | 32)), "First open shows total bank capacity: " + strings);
        check(!player.hasTick("bank_space_refresh"), "No delayed counter correction is scheduled");
        strings.clear(); cycle.execute(); cycle.execute();
        check(!strings.containsKey((762 << 16) | 31), "Later server cycles do not overwrite the counter");
        strings.clear();
        check(Boolean.TRUE.equals(player.getAttribute("inBank", Boolean.FALSE)), "Bank-open route marks the session as in-bank");

        MessageBuilder packet = new MessageBuilder(6);
        packet.writeShort(762).writeShort(62).writeLEShortA(-1).writeShort(-1);
        Method handle = ActionButtonHandler.class.getDeclaredMethod("handleButtons", Player.class, Message.class, int.class);
        handle.setAccessible(true);
        handle.invoke(new ActionButtonHandler(), player, packet.toMessage(), 0);

        check(player.getLastBankTab() == 10, "View all selects the complete bank");
        check("2".equals(strings.get((762 << 16) | 29)), "View all refreshes free-to-play item slots: " + strings);
        check("68".equals(strings.get((762 << 16) | 30)), "View all refreshes free bank capacity: " + strings);
        check("3".equals(strings.get((762 << 16) | 31)), "View all refreshes used bank slots: " + strings);
        check("516".equals(strings.get((762 << 16) | 32)), "View all refreshes bank capacity: " + strings);
        check(!strings.containsKey((762 << 16) | 45), "View all does not race the cache-native bank title");
        for(int click=0;click<4;click++) {
            varcs.remove(190);
            MessageBuilder search=new MessageBuilder(6);
            search.writeShort(762).writeShort(17).writeLEShortA(-1).writeShort(-1);
            handle.invoke(new ActionButtonHandler(),player,search.toMessage(),0);
            check(Integer.valueOf(1).equals(varcs.get(190)),"Search toggle acknowledged on every click, including reopening");
        }
        player.getInventory().set(0, new Item(1511, 1));
        ActionSender.sendCloseOverlay(player);
        check(Boolean.TRUE.equals(player.getAttribute("inBank", Boolean.FALSE)),
                "Closing an unrelated overlay must not invalidate the open bank session");
        MessageBuilder deposit = new MessageBuilder(6);
        deposit.writeShort(763).writeShort(0).writeLEShortA(0).writeShort(1511);
        handle.invoke(new ActionButtonHandler(), player, deposit.toMessage(), 0);
        check(player.getInventory().get(0) == null && player.getBank().getContainer().getNumberOf(new Item(1511)) == 2,
                "Deposit click transfers the item into the bank");
        MessageBuilder withdraw = new MessageBuilder(6);
        withdraw.writeShort(762).writeShort(93).writeLEShortA(0).writeShort(1511);
        handle.invoke(new ActionButtonHandler(), player, withdraw.toMessage(), 0);
        check(player.getInventory().getContainer().getNumberOf(new Item(1511)) == 1
                && player.getBank().getContainer().getNumberOf(new Item(1511)) == 1,
                "Withdraw click transfers the item back without loss or duplication");
        check("3".equals(strings.get((762 << 16) | 31)), "Partial stack withdrawal keeps occupied slot count");
        MessageBuilder lastMember = new MessageBuilder(6);
        lastMember.writeShort(762).writeShort(93).writeLEShortA(2).writeShort(11732);
        handle.invoke(new ActionButtonHandler(), player, lastMember.toMessage(), 0);
        check("2".equals(strings.get((762 << 16) | 31)) && "516".equals(strings.get((762 << 16) | 32)),
                "Removing the last member item decreases total slots by exactly one, not the free-item count");
        check(!varcs.containsKey(192) && !varcs.containsKey(1038) && !varcs.containsKey(1324),
                "Deposits and withdrawals never trigger the competing native counter");
        check(!player.hasTick("bank_space_refresh"), "Transfers need no delayed correction");
        for (int mode = 0; mode <= 3; mode++) {
            player.getConnection().setDisplayMode(mode);
            player.getBank().openBank();
            ActionSender.sendCloseOverlay(player);
            ActionSender.closeSideInterface(player);
            check(Boolean.TRUE.equals(player.getAttribute("inBank", false)), "Non-bank slots preserve session, mode " + mode);
            ActionSender.sendCloseInterface(player);
            check(!Boolean.TRUE.equals(player.getAttribute("inBank", false)), "Main modal close ends session, mode " + mode);
            int count = player.getBank().getContainer().getNumberOf(new Item(1511));
            player.getBank().addItem(0, 1);
            player.getBank().removeItem(0, 1);
            check(player.getBank().getContainer().getNumberOf(new Item(1511)) == count, "Closed bank rejects transfers");
            strings.clear(); cycle.execute(); cycle.execute();
            check(!strings.containsKey((762 << 16) | 31), "Closed bank suppresses delayed counter");
            player.getBank().openBank();
            ActionSender.closeInventoryInterface(player);
            check(!Boolean.TRUE.equals(player.getAttribute("inBank", false)), "Inventory modal close ends session, mode " + mode);
        }
        configs.clear(); player.getBank().openBank();
        check(Integer.valueOf(0).equals(configs.get(115)) && !player.getBank().noting(),
                "Opening retains the existing reset of note mode on both server and client");
        check(("Bank of " + org.dementhium.util.Constants.SERVER_NAME).equals(strings.get((762 << 16) | 45)),
                "Custom bank title retained");
        Player victim = player();
        victim.getBank().commandAdd(995, 1, 2);
        victim.getBank().commandAdd(1127, 1, 2);
        victim.getInventory().set(0, new Item(1163));
        String own = ItemTransactionRegression.image(player.getBank().getContainer());
        String other = ItemTransactionRegression.image(victim.getBank().getContainer());
        player.setLastBankTab(3);
        events.clear(); configs.clear(); strings.clear(); player.getBank().openPlayerBank(victim);
        check(events.contains("items:93") && "2".equals(strings.get((762 << 16) | 31))
                && Integer.valueOf(2).equals(configs.get(1246)), "Inspection displays the target inventory, counters and tabs");
        check(((configs.get(1248) >>> 27) & 15) == 1 && player.getLastBankTab() == 3,
                "Inspection starts in view-all without changing the owner's saved tab selection");
        check(events.indexOf("mask:" + ((762 << 16) | 93)) > events.indexOf("open:762"),
                "Inspection masks follow panel attachment");
        player.getBank().bankInv(); player.getBank().bankEquip(); player.getBank().removeItem(0, 1);
        check(own.equals(ItemTransactionRegression.image(player.getBank().getContainer()))
                && other.equals(ItemTransactionRegression.image(victim.getBank().getContainer())), "Inspection remains read-only");
        System.out.println("PASS: " + checks + " bank tab transition checks");
    }
}
