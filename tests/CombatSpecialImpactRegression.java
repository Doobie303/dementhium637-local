import java.util.ArrayList;
import java.util.List;
import org.dementhium.model.*;
import org.dementhium.model.combat.*;
import org.dementhium.model.combat.impl.SpecialAction;
import org.dementhium.model.combat.impl.specs.*;
import org.dementhium.model.map.Region;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.player.*;

/** Real special selection/executor phases on synthetic open terrain; no live server. */
public class CombatSpecialImpactRegression {
    private static int checks;
    private static final List<Mob> fixtures = new ArrayList<Mob>();
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    private static NPC npc(int x, int y) {
        NPC npc = new NPC(1);
        npc.setLocation(Location.locate(x, y, 0));
        npc.setAttribute("fightcaves", true); // Explicit multi-area fixture, not a cave encounter.
        npc.setHp(10000);
        fixtures.add(npc);
        return npc;
    }
    private static Player player(int x, int y) {
        NPC anchor = npc(x - 1, y);
        Player player = CombatFixtures.player(anchor);
        anchor.destroy();
        player.setLocation(Location.locate(x, y, 0));
        player.setAttribute("inFightCaves", true); // Multi flag only; real wilderness legality below.
        player.getSkills().setLevelAndXP(Skills.CONSTITUTION, 99, 13034431);
        player.getSkills().setLevel(Skills.ATTACK, 9999);
        player.getSettings().setCombatType(0);
        player.getSettings().setCombatStyle(org.dementhium.model.definition.WeaponInterface.STYLE_ACCURATE);
        player.getRandom().setSeed(7);
        fixtures.add(player);
        return player;
    }
    private static Interaction launch(Player source, Mob victim, int weapon) {
        source.getEquipment().set(Equipment.SLOT_WEAPON, new Item(weapon));
        source.getBonuses().calculate();
        source.setSpecialAmount(1000);
        source.getSettings().setUsingSpecial(true);
        source.getCombatExecutor().setVictim(victim);
        // Keep the executor's real initial three-tick cooldown.
        for (int tick = 0; tick < 3; tick++) source.getCombatExecutor().tick();
        Interaction interaction = SpecialAction.getSingleton().getInteraction();
        check(interaction != null && interaction.getSource() == source,
                "Special launched through normal executor selection: " + weapon);
        check(interaction.getSpecialAttack() == SpecialAttackContainer.get(weapon),
                "Registered weapon handler selected: " + weapon);
        return interaction;
    }
    private static void finish(Player source) {
        source.getCombatExecutor().tick();
    }
    private static Damage hit(Interaction interaction, Mob victim) {
        for (ExtraTarget target : interaction.getTargets())
            if (target.getVictim() == victim) return target.getDamage();
        throw new AssertionError("Selected target absent from area special");
    }
    private static void areaNpcs(int weapon) {
        Player source = player(3200, 3600);
        NPC selected = npc(3201, 3600), secondary = npc(3200, 3601), far = npc(3202, 3600);
        Interaction interaction = launch(source, selected, weapon);
        check(interaction.getTargets().size() == 2, "Area special selects adjacent NPCs: " + weapon);
        check(hit(interaction, selected).getHit() > 0 && hit(interaction, secondary).getHit() > 0,
                "Each NPC receives a generated attack: " + weapon);
        check(selected.getHp() == 10000 && secondary.getHp() == 10000, "No launch damage");
        finish(source);
        check(selected.getHp() < 10000 && secondary.getHp() < 10000 && far.getHp() == 10000,
                "Attacker-centered area reaches both NPCs and excludes distance two: " + weapon);
    }
    private static void areaPlayers(int weapon) {
        Player source = player(3200, 3600), selected = player(3201, 3600), secondary = player(3200, 3601);
        selected.setAttribute("godmode", true);
        check(selected.isAttackable(source) && secondary.isAttackable(source), "Real wilderness PvP permission");
        Interaction interaction = launch(source, selected, weapon);
        check(hit(interaction, selected).getHit() == 0, "Primary immunity preserved: " + weapon);
        check(hit(interaction, secondary).getHit() > 0, "Secondary does not inherit primary immunity: " + weapon);
        finish(source);
        check(selected.getHitPoints() == 1000 && secondary.getHitPoints() < 1000,
                "PvP executor applies the secondary hit with its own protection: " + weapon);
        // An unrelated primary context change must not invalidate a secondary's captured damage.
        Damage secondaryHit = hit(interaction, secondary);
        selected.markInstanceTransition();
        check(secondaryHit.isInstanceContextCurrent(source, secondary), "Each target owns its context: " + weapon);
    }
    private static void areaCap(int weapon, int cap) {
        Player source = player(3200, 3600);
        NPC selected = npc(3201, 3600);
        for (int i = 0; i < 16; i++) npc(3200, 3601);
        NPC otherPlane = npc(3200, 3601);
        otherPlane.setLocation(Location.locate(3200, 3601, 1));
        Interaction interaction = launch(source, selected, weapon);
        check(interaction.getTargets().size() == cap, "Retained total area cap: " + weapon);
        check(interaction.getTargets().get(0).getVictim() == selected, "Selected victim keeps priority at cap");
        for (ExtraTarget target : interaction.getTargets())
            check(target.getVictim().isNPC() && target.getVictim().getLocation().getZ() == 0,
                    "Area target kind and plane preserved");
    }
    private static int[] levels(Player target) {
        int[] result = new int[7];
        for (int skill = 0; skill < result.length; skill++) result[skill] = target.getSkills().getLevel(skill);
        return result;
    }
    private static void status(int weapon, int mode) {
        Player source = player(3200, 3600), victim = player(3201, 3600);
        if (mode == 1) victim.setAttribute("hitImmunity", World.getTicks() + 5);
        if (weapon == 10887) {
            victim.getEquipment().set(Equipment.SLOT_SHIELD, new Item(13740));
            victim.getBonuses().calculate();
        }
        int[] before = levels(victim);
        Interaction interaction = launch(source, victim, weapon);
        check(interaction.getDamage().getHit() > 0 || interaction.getSecondaryDamage() != null
                && interaction.getSecondaryDamage().getHit() > 0, "Positive rolled hit fixture: " + weapon);
        check(java.util.Arrays.equals(before, levels(victim)), "No stat drain before impact: " + weapon);
        check(!victim.getPoisonManager().isPoisoned(), "No weapon poison before impact: " + weapon);
        if (mode == 2) source.getCombatExecutor().cancelPending();
        finish(source);
        if (mode != 0) {
            check(victim.getHitPoints() == 1000 && java.util.Arrays.equals(before, levels(victim))
                    && !victim.getPoisonManager().isPoisoned(), "Rejected/cancelled hit cannot drain or poison: " + weapon);
        } else {
            check(victim.getHitPoints() < 1000, "Accepted hit loses HP: " + weapon);
            check(weapon == 5698 ? victim.getPoisonManager().isPoisoned()
                    : !java.util.Arrays.equals(before, levels(victim)), "Accepted hit commits effect: " + weapon);
            if (weapon == 10887) {
                int actual = 1000 - victim.getHitPoints();
                int[] after = levels(victim);
                for (int skill = 0; skill < before.length; skill++) if (before[skill] != after[skill])
                    check(after[skill] == (int)(before[skill] - actual * 0.1), "Sunder uses shield-reduced actual damage");
            }
        }
    }
    private static void clean() {
        for (Mob mob : fixtures) {
            mob.getCombatExecutor().cancelPending();
            mob.getPoisonManager().removePoison();
            if (mob.isPlayer()) mob.getPlayer().setOnline(false);
            mob.destroy();
        }
        fixtures.clear();
        CombatFixtures.clearPlayers();
    }
    public static void main(String[] args) throws Exception {
        CombatFixtures.init();
        SpecialAttackContainer.initialize();
        Region region = Region.forCoords(3200, 3600);
        region.clippingMasks = new int[4][128][128];
        region.setClipped(true);
        int failures = 0;
        for (int weapon : new int[] {13905, 7158}) {
            try { areaNpcs(weapon); } catch (AssertionError error) { failures++; System.out.println("FAIL: " + error.getMessage()); } finally { clean(); }
            try { areaPlayers(weapon); } catch (AssertionError error) { failures++; System.out.println("FAIL: " + error.getMessage()); } finally { clean(); }
            try { areaCap(weapon, weapon == 13905 ? 8 : 13); } catch (AssertionError error) { failures++; System.out.println("FAIL: " + error.getMessage()); } finally { clean(); }
        }
        for (int weapon : new int[] {10887, 13902, 6746, 5698}) for (int mode = 0; mode < 3; mode++) {
            try { status(weapon, mode); } catch (AssertionError error) { failures++; System.out.println("FAIL: " + error.getMessage()); } finally { clean(); }
        }
        if (failures != 0) throw new AssertionError(failures + " special combat scenarios failed");
        System.out.println("PASS: " + checks + " special combat impact/target checks");
    }
}
