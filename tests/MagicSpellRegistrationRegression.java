import java.lang.reflect.Field;
import java.util.Map;

import org.dementhium.model.Item;
import org.dementhium.model.combat.MagicSpell;
import org.dementhium.model.combat.SpellContainer;
import org.dementhium.model.combat.impl.spells.modern.ClawsOfGuthix;
import org.dementhium.model.combat.impl.spells.modern.FlamesOfZamorak;
import org.dementhium.model.combat.impl.spells.modern.IbanBlast;
import org.dementhium.model.combat.impl.spells.modern.MagicDart;
import org.dementhium.model.combat.impl.spells.modern.SaradominStrike;

public class MagicSpellRegistrationRegression {

	public static void main(String[] args) throws Exception {
		SpellContainer.initialize();

		Field field = SpellContainer.class.getDeclaredField("MODERN");
		field.setAccessible(true);
		@SuppressWarnings("unchecked")
		Map<Integer, MagicSpell> modern = (Map<Integer, MagicSpell>) field.get(null);

		checkSpell(modern, 54, IbanBlast.class, 50, 45, 1409,
				new Item[] { new Item(554, 5), new Item(560, 1) });
		checkSpell(modern, 56, MagicDart.class, 50, 37, 4170,
				new Item[] { new Item(560, 1), new Item(558, 4) });
		checkSpell(modern, 66, SaradominStrike.class, 60, 41, 2415,
				new Item[] { new Item(556, 4), new Item(554, 1), new Item(565, 2) });
		checkSpell(modern, 67, ClawsOfGuthix.class, 60, 39, 2416,
				new Item[] { new Item(556, 4), new Item(554, 1), new Item(565, 2) });
		checkSpell(modern, 68, FlamesOfZamorak.class, 60, 43, 2417,
				new Item[] { new Item(556, 1), new Item(554, 4), new Item(565, 2) });

		System.out.println("PASS: missing modern combat spells are registered with their requirements");
	}

	private static void checkSpell(Map<Integer, MagicSpell> spells, int id,
			Class<? extends MagicSpell> type, int level, int autocast, int staff, Item[] runes) {
		MagicSpell spell = spells.get(id);
		check(type.isInstance(spell), "spell " + id + " uses " + type.getSimpleName());
		check(spell.getRequiredLevel() == level, "spell " + id + " level requirement");
		check(spell.getAutocastConfig() == autocast, "spell " + id + " autocast config");
		check(spell.getRequiredStaff() == staff, "spell " + id + " staff requirement");
		Item[] actualRunes = spell.getRequiredRunes();
		check(actualRunes.length == runes.length, "spell " + id + " rune count");
		for (int i = 0; i < runes.length; i++) {
			check(actualRunes[i].getId() == runes[i].getId()
					&& actualRunes[i].getAmount() == runes[i].getAmount(),
					"spell " + id + " rune requirement " + i);
		}
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
