import java.lang.reflect.Field;

import org.dementhium.event.impl.interfaces.MagicBookListener;
import org.dementhium.model.Item;
import org.dementhium.model.Location;
import org.dementhium.model.World;
import org.dementhium.model.combat.MagicSpell;
import org.dementhium.model.combat.SpellContainer;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.player.Player;
import org.dementhium.model.player.Skills;
import org.dementhium.net.message.MessageBuilder;
import org.dementhium.net.packethandlers.CastAttackHandler;

/** Real manual-cast packet/autocast selection, followed by ordinary executor cooldowns. */
public class MagicCastRequirementsRegression {
    private static int checks;
    private static int tick;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void advance(Player player, int count) throws Exception {
        Field clock = World.class.getDeclaredField("ticksPassed");
        clock.setAccessible(true);
        for (int i = 0; i < count; i++) {
            clock.setInt(null, tick++);
            player.getCombatExecutor().tick();
        }
    }

    private static void request(Player player, NPC target, int book, int spellId, boolean auto) {
        if (auto) {
            new MagicBookListener().interfaceOption(player, book, spellId, -1, -1, 0);
            check(player.getAttribute("autocastId", -1) == spellId, "Autocast selected while qualified");
            player.getCombatExecutor().setVictim(target);
        } else {
            MessageBuilder packet = new MessageBuilder(14);
            packet.writeShort(spellId).writeShort(book).writeShort(0).writeShort(target.getIndex());
            packet.writeByteS(0).writeLEShort(-1);
            new CastAttackHandler().handlePacket(player, packet.toMessage());
        }
        check(player.getCombatExecutor().getVictim() == target, "Combat entry selected target");
    }

    private static void requirement(int book, int spellId, boolean auto, int level, boolean godmode) throws Exception {
        NPC target = new NPC(1);
        target.setLocation(Location.locate(3216, 3216, 0));
        target.setOriginalLocation(target.getLocation());
        target.setHp(10000);
        World.getWorld().getNpcs().add(target);
        Player player = CombatFixtures.player(target);
        player.setLocation(target.getLocation().transform(4, 0, 0));
        player.getSettings().setSpellBook(book);
        MagicSpell spell = SpellContainer.grabSpell(player, spellId);
        for (Item rune : spell.getRequiredRunes()) player.getInventory().addItem(rune.getId(), 1000);
        Item rune = spell.getRequiredRunes()[0];
        double xp = player.getSkills().getXp(Skills.MAGIC);
        try {
            request(player, target, book, spellId, auto);
            // A selected spell can become unusable before launch through a real stat drain.
            player.getSkills().setLevel(Skills.MAGIC, level);
            if (godmode) player.setAttribute("godmode", true);
            advance(player, 3);
            if (level < spell.getRequiredLevel()) {
                check(player.getInventory().getContainer().getNumberOf(rune) == 1000,
                        "Below-level cast must not consume runes: " + book + "/" + auto);
                check(player.getMask().getLastAnimation() == null, "Rejected cast must not animate");
                check(player.getCombatExecutor().getVictim() == null, "Rejected cast stops attacking");
                advance(player, 10);
                check(target.getHp() == 10000 && target.getAttribute("freezeTime", -1) < tick,
                        "Rejected cast cannot later hit or freeze");
                check(player.getSkills().getXp(Skills.MAGIC) == xp, "Rejected cast gives no experience");
            } else {
                check(player.getInventory().getContainer().getNumberOf(rune) == 1000 - rune.getAmount(),
                        "Requirement boundary launches exactly one cast");
                advance(player, 4);
                check(player.getSkills().getXp(Skills.MAGIC) > xp, "Qualified spell completes");
                check(player.getInventory().getContainer().getNumberOf(rune) == 1000 - rune.getAmount(),
                        "No early second cast");
                advance(player, 1);
                check(player.getInventory().getContainer().getNumberOf(rune) == 1000 - rune.getAmount() * (auto ? 2 : 1),
                        "Autocast repeats at five ticks; manual cast stops");
                if (auto) {
                    player.getSkills().setLevel(Skills.MAGIC, spell.getRequiredLevel() - 1);
                    advance(player, 5);
                    check(player.getInventory().getContainer().getNumberOf(rune) == 1000 - rune.getAmount() * 2,
                            "Drain between consecutive attacks stops the next launch");
                    check(player.getCombatExecutor().getVictim() == null, "Drained autocast disengages");
                }
            }
        } finally {
            player.getCombatExecutor().cancelPending();
            CombatFixtures.clearPlayers();
            World.getWorld().getNpcs().remove(target);
        }
    }

    public static void main(String[] args) throws Exception {
        CombatFixtures.init();
        SpellContainer.initialize();
        for (boolean auto : new boolean[] {false, true}) {
            requirement(192, 91, auto, 94, false);
            requirement(192, 91, auto, 95, false);
            requirement(193, 23, auto, 93, false);
            requirement(193, 23, auto, 94, false);
        }
        requirement(192, 91, true, 94, true);
        System.out.println("Magic cast requirements: " + checks + " checks passed");
    }
}

