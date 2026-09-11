import java.lang.reflect.Field;

import org.dementhium.content.interfaces.LevelUp;
import org.dementhium.event.impl.interfaces.SkillGuideListener;
import org.dementhium.model.player.Skills;

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

        System.out.println("PASS: skill level-up icon mappings are aligned");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
