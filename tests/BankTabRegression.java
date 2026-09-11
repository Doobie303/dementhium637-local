import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.netty.channel.Channel;
import org.dementhium.model.Item;
import org.dementhium.model.definition.PlayerDefinition;
import org.dementhium.model.player.Player;
import org.dementhium.net.ActionSender;
import org.dementhium.net.GameSession;
import org.dementhium.net.message.Message;
import org.dementhium.net.message.MessageBuilder;
import org.dementhium.net.packethandlers.ActionButtonHandler;

/** Covers the client-script tab transition without re-sending the cache-default bank title. */
public final class BankTabRegression {
    private static int checks;
    private static final Map<Integer, String> strings = new HashMap<Integer, String>();
    private static final List<String> channelCalls = new ArrayList<String>();

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
                    }
                    if (method.getReturnType() == boolean.class) return false;
                    if (method.getReturnType() == int.class) return 0;
                    return null;
                });
        return new Player(new GameSession(channel), new PlayerDefinition("bank-tab-test", "unused"));
    }

    public static void main(String[] args) throws Exception {
        Player player = player();
        player.setHasReceivedStarter(true);
        ActionSender.sendString(player, 762, 31, "direct");
        check("direct".equals(strings.get((762 << 16) | 31)), "Fixture captures client string packets: " + channelCalls);
        player.getBank().openBank();
        player.getBank().set(0, new Item(1511));
        player.getBank().set(1, new Item(995, 250));
        strings.clear();
        check(Boolean.TRUE.equals(player.getAttribute("inBank", Boolean.FALSE)), "Bank-open route marks the session as in-bank");

        MessageBuilder packet = new MessageBuilder(6);
        packet.writeShort(762).writeShort(62).writeLEShortA(-1).writeShort(-1);
        Method handle = ActionButtonHandler.class.getDeclaredMethod("handleButtons", Player.class, Message.class, int.class);
        handle.setAccessible(true);
        handle.invoke(new ActionButtonHandler(), player, packet.toMessage(), 0);

        check(player.getLastBankTab() == 10, "View all selects the complete bank");
        check("2".equals(strings.get((762 << 16) | 31)), "View all refreshes used bank slots: " + strings);
        check("516".equals(strings.get((762 << 16) | 32)), "View all refreshes bank capacity: " + strings);
        check(!strings.containsKey((762 << 16) | 45), "View all does not race the cache-native bank title");
        System.out.println("PASS: " + checks + " bank tab transition checks");
    }
}
