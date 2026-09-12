import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import org.jboss.netty.channel.Channel;
import org.dementhium.content.interfaces.LevelUp;
import org.dementhium.event.impl.interfaces.SkillGuideListener;
import org.dementhium.model.definition.PlayerDefinition;
import org.dementhium.model.player.Player;
import org.dementhium.model.player.Skills;
import org.dementhium.net.GameSession;
import org.dementhium.net.message.Message;

public class SkillLevelUpIconRegression {

    public static void main(String[] args) throws Exception {
        Field field = SkillGuideListener.class.getDeclaredField("SKILL_GUIDE_DATA");
        field.setAccessible(true);
        int[][] skillGuideData = (int[][]) field.get(null);

        for (int skill = 0; skill < Skills.SKILL_COUNT; skill++) {
            check(skillGuideData[skill][2] == LevelUp.CONFIG_VALUES[skill],
                    Skills.SKILL_NAME[skill] + " click mapping is aligned with its level-up config");
        }
        check(skillGuideData[Skills.HUNTER][0] == 142, "Hunter acknowledges the Hunter flash");
        check(skillGuideData[Skills.CONSTRUCTION][0] == 134, "Construction acknowledges the Construction flash");

        Map<Integer, Integer> configs = new HashMap<Integer, Integer>();
        Channel channel = (Channel) Proxy.newProxyInstance(Channel.class.getClassLoader(), new Class[]{Channel.class},
                (proxy, method, methodArgs) -> {
                    if (method.getName().equals("isConnected") || method.getName().equals("isOpen")) {
                        return true;
                    }
                    if (method.getName().equals("write") && methodArgs[0] instanceof Message) {
                        Message message = (Message) methodArgs[0];
                        if (message.getOpcode() == 27) {
                            int a = message.readByte() & 255;
                            int b = message.readByte() & 255;
                            int c = message.readByte() & 255;
                            int d = message.readByte() & 255;
                            configs.put(message.readShort(), (c << 24) | (d << 16) | (a << 8) | b);
                        } else if (message.getOpcode() == 21) {
                            configs.put(message.readShortA(), message.readByteS() & 255);
                        }
                    }
                    if (method.getReturnType() == boolean.class) {
                        return false;
                    }
                    if (method.getReturnType() == int.class) {
                        return 0;
                    }
                    return null;
                });
        Player player = new Player(new GameSession(channel), new PlayerDefinition("level-up-test", "unused"));
        SkillGuideListener listener = new SkillGuideListener();
        player.getSettings().getLeveledUpConfig()[Skills.RANGED] = true;
        listener.interfaceOption(player, 320, 52, -1, -1, 6);
        check(Integer.valueOf(LevelUp.CONFIG_VALUES[Skills.RANGED] << 3).equals(configs.get(1230)),
                "Ranged details hide default total and combat milestones");

        player.getSettings().getLeveledUpConfig()[Skills.STRENGTH] = true;
        listener.interfaceOption(player, 320, 11, -1, -1, 6);
        check(Integer.valueOf(LevelUp.CONFIG_VALUES[Skills.STRENGTH] << 3).equals(configs.get(1230)),
                "Strength details hide the default total milestone");

        System.out.println("PASS: skill level-up interface mappings and milestone flags are correct");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
