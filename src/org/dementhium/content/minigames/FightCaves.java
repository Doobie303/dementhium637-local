package org.dementhium.content.minigames;

import org.dementhium.model.Location;
import org.dementhium.model.Mob;
import org.dementhium.model.instance.InstanceManager;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.player.Player;

/** Fight Caves entry points and the existing 63-wave template. Run state lives in FightCavesSession. */
public final class FightCaves {
    private FightCaves() {}
	public static final Location OUTSIDE_OF_CAVE = Location.locate(2438, 5168, 0);
	public static final Location CAVE_START = Location.locate(2410, 5111, 0);

	public static final int TZ_KIH = 2734;
	public static final int TZ_KEK = 2736;
	public static final int TZ_KEK_SPLIT = 2738;
	public static final int TOK_XIL = 2739;
	public static final int YT_MEJKOT = 2741;
	public static final int KET_ZEK = 2743;
	public static final int KET_ZEK_SPAWN = 2744;
	public static final int JAD = 2745;
	public static final int YT_HURKOT = 2746;
	public static final int TZHAAR_MEJ_JAL = 2617;

	static final Location[] SPAWNS = {
			// Clear areas on the 637 cache: NW, SW, south, centre, SE.
			// These are arena anchors, independent of the player's position.
			Location.locate(2378, 5106, 0),
			Location.locate(2378, 5074, 0),
			Location.locate(2404, 5070, 0),
			Location.locate(2400, 5088, 0),
			Location.locate(2416, 5080, 0)
	};
	static final int WAVE_DELAY_TICKS = 5;

	static final int[][] WAVES = {
			{2734},
			{2734,2734},
			{2736},
			{2736,2734},
			{2736,2734,2734},
			{2736,2736},
			{2739},
			{2739,2734},
			{2739,2734,2734},
			{2739,2736},
			{2739,2736,2734},
			{2739,2736,2734,2734},
			{2739,2736,2736},
			{2739,2739},
			{2741},
			{2741,2734},
			{2741,2734,2734},
			{2741,2736},
			{2741,2736,2734},
			{2741,2736,2734,2734},
			{2741,2736,2736},
			{2741,2739},
			{2741,2739,2734},
			{2741,2739,2734,2734},
			{2741,2739,2736},
			{2741,2739,2736,2734},
			{2741,2739,2736,2734,2734},
			{2741,2739,2736,2736},
			{2741,2739,2739},
			{2741,2741},
			{2743},
			{2743,2734},
			{2743,2734,2734},
			{2743,2736},
			{2743,2736,2734},
			{2743,2736,2734,2734},
			{2743,2736,2736},
			{2743,2739},
			{2743,2739,2734},
			{2743,2739,2734,2734},
			{2743,2739,2736},
			{2743,2739,2736,2734},
			{2743,2739,2736,2734,2734},
			{2743,2739,2736,2736},
			{2743,2739,2739},
			{2743,2741},
			{2743,2741,2734},
			{2743,2741,2734,2734},
			{2743,2741,2736},
			{2743,2741,2736,2734},
			{2743,2741,2736,2734,2734},
			{2743,2741,2736,2736},
			{2743,2741,2739},
			{2743,2741,2739,2734},
			{2743,2741,2739,2734,2734},
			{2743,2741,2739,2736},
			{2743,2741,2739,2736,2734},
			{2743,2741,2739,2736,2734,2734},
			{2743,2741,2739,2736,2736},
			{2743,2741,2739,2739},
			{2743,2741,2741},
			{2743,2744},
			{2745}
	};


    public static boolean isCaveNpc(int id) { return id >= TZ_KIH && id <= YT_HURKOT; }
    public static boolean isBlockingNpc(int id) { return isCaveNpc(id) && id != YT_HURKOT; }
    public static FightCavesSession getSession(Player player) {
        return player == null ? null : player.getAttribute("fightCavesSession");
    }
    private static FightCavesSession getSession(NPC npc) {
        return npc == null ? null : npc.getAttribute("fightCavesSession");
    }
    private static void onCycle(Runnable action) {
        InstanceManager manager = InstanceManager.getSingleton();
        if (manager.isCycleThread()) action.run();
        else manager.submit(() -> { action.run(); return null; });
    }
    public static void startCaves(Player player) { startCaves(player, 0); }
    public static void startCaves(Player player, int wave) {
        onCycle(() -> FightCavesSession.start(InstanceManager.getSingleton(), player, wave));
    }
    /** The admin wave command may deliberately replace only its caller's own run. */
    public static void restartCaves(Player player, int wave) {
        onCycle(() -> {
            FightCavesSession session = getSession(player);
            if (session != null) session.quit();
            FightCavesSession.start(InstanceManager.getSingleton(), player, wave);
        });
    }
    public static int getCurrentWave(Player player) {
        FightCavesSession session = getSession(player);
        return session == null ? 0 : session.getCurrentWave();
    }
    public static void quitCaves(Player player) { exitCave(player, 0); }
    public static void endCaves(Player player) { exitCave(player, 3); }
    public static void exitCave(Player player, int type) {
        onCycle(() -> {
            FightCavesSession session = getSession(player);
            if (session != null) {
                if (type == 3) session.finish(); else session.quit();
            }
        });
    }
    public static void pullRemaining(Player player) {
        onCycle(() -> {
            FightCavesSession session = getSession(player);
            if (session != null) session.pullRemaining();
        });
    }
    public static NPC spawnCaveNpc(int id, Location location, Player player, boolean extra) {
        FightCavesSession session = getSession(player);
        return session == null ? null : session.spawnNpc(id, location, extra);
    }
    public static boolean isCaveOpponent(Mob source, Mob target) {
        FightCavesSession session = source != null && source.isNPC() ? getSession(source.getNPC()) : null;
        return session != null && session.isOpponent(source, target);
    }
    public static Player getCombatTarget(NPC npc) {
        FightCavesSession session = getSession(npc);
        return session == null ? null : session.getCombatTarget(npc);
    }
    public static NPC getMejKotHealTarget(NPC npc) {
        FightCavesSession session = getSession(npc);
        return session == null ? null : session.getMejKotHealTarget(npc);
    }
    public static void onCaveNpcDeath(NPC npc, Player killer) {
        FightCavesSession session = getSession(npc);
        if (session != null) session.onNpcDeath(npc, killer);
    }
    public static boolean handleEntranceObject(Player player, int objectId) {
        if (objectId == 9356) {
            if (player.getFamiliar() != null) player.sendMessage("You can't bring your familiar into this minigame.");
            else startCaves(player);
            return true;
        }
        if (objectId == 9357) { quitCaves(player); return true; }
        return false;
    }
}
