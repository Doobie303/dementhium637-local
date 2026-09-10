package org.dementhium.content.minigames;

import static org.dementhium.content.minigames.FightCaves.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.dementhium.content.DialogueManager;
import org.dementhium.model.Item;
import org.dementhium.model.Location;
import org.dementhium.model.Mob;
import org.dementhium.model.instance.GameInstance;
import org.dementhium.model.instance.InstanceAccess;
import org.dementhium.model.instance.InstanceManager;
import org.dementhium.model.map.Region;
import org.dementhium.model.misc.GroundItem;
import org.dementhium.model.misc.GroundItemManager;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.npc.impl.FightCaveNPC;
import org.dementhium.model.npc.impl.TzTokJad;
import org.dementhium.model.player.Player;
import org.dementhium.net.ActionSender;

/** One ephemeral, solo run. Every position, monster, timer and reward belongs to this instance. */
public final class FightCavesSession {
    private final Player player;
    private GameInstance instance;
    private final List<NPC> live = new ArrayList<NPC>();
    private int wave;
    private boolean started, ended, waveQueued, waveFailed, finishing, won;
    private Location jadSpawn;

    private FightCavesSession(Player player, int wave) {
        this.player = player;
        this.wave = Math.max(0, Math.min(wave, WAVES.length - 1));
    }

    /** Must run on the supplied manager's cycle, also allowing an isolated test scheduler. */
    public static FightCavesSession start(InstanceManager manager, Player player, int wave) {
        manager.checkThread();
        if (player == null || !player.isOnline() || player.isDead() || player.getConnection().isDisconnected()) return null;
        if (getSession(player) != null || InstanceAccess.owner(player) != null
                || player.getFamiliar() != null || (player.getActivity() != null && player.getActivity().isRunning())) {
            player.sendMessage("You cannot enter the Fight Caves from your current activity.");
            return null;
        }
        FightCavesSession session = new FightCavesSession(player, wave);
        try {
            session.instance = manager.create(8, 8, 1, OUTSIDE_OF_CAVE, map -> {
                map.setActivity("fight-caves");
                // The complete 637 cave region, retaining source objects and clipping.
                map.copyMap(296, 632, 0, 0, 8, 8, new int[]{0}, new int[]{0});
                map.setDepartureHandler(session::departed);
            });
            if (session.instance == null) {
                player.sendMessage("No Fight Caves instance is available. Please try again shortly.");
                return null;
            }
            session.instance.enter(player, session.translate(CAVE_START));
            player.setAttribute("fightCavesSession", session);
            player.setAttribute("inFightCaves", Boolean.TRUE);
            player.setAttribute("teleblock", Integer.valueOf(999999999));
            DialogueManager.sendDialogue(player, DialogueManager.CALM_TALK, TZHAAR_MEJ_JAL, -1,
                    "You're on your own now, JalYt.", "Prepare to fight for your life!");
            session.instance.schedule(8, 8, () -> {
                if (session.active() && !session.waveQueued && !session.finishing && !session.waveFailed
                        && session.wave > 0 && session.wave < WAVES.length && session.countBlocking() == 0)
                    session.queueNextWave();
            });
            session.startWave();
            if (session.waveFailed) { session.instance.close(); return null; }
            session.started = true;
            return session;
        } catch (RuntimeException failure) {
            if (session.instance != null) session.instance.close();
            System.err.println("Fight Caves entry failed for " + player.getUsername() + ": " + failure);
            player.sendMessage("The Fight Caves could not be started. Please try again later.");
            return null;
        }
    }

    public GameInstance getInstance() { return instance; }
    public int getCurrentWave() { instance.getManager().checkThread(); return wave; }
    public List<NPC> getLiveNpcs() {
        instance.getManager().checkThread();
        return Collections.unmodifiableList(new ArrayList<NPC>(live));
    }
    private Location translate(Location source) {
        return instance.location(source.getX() - 2368, source.getY() - 5056, source.getZ());
    }
    private boolean active() {
        return !ended && instance.isActive() && instance.isMember(player) && player.isOnline()
                && !player.getConnection().isDisconnected() && !player.isDead() && getSession(player) == this;
    }
    private boolean owns(NPC npc) {
        return npc != null && npc.getOwningInstance() == instance && instance.owns(npc)
                && npc.getAttribute("fightCavesSession") == this;
    }
    public boolean isOpponent(Mob source, Mob target) {
        return active() && source != null && source.isNPC() && owns(source.getNPC()) && target == player
                && !source.isDead() && !source.isHidden() && !player.isHidden() && !player.isInvisible()
                && InstanceAccess.canInteract(source, target);
    }
    public Player getCombatTarget(NPC npc) {
        return npc != null && isBlockingNpc(npc.getId()) && isOpponent(npc, player) ? player : null;
    }
    public NPC getMejKotHealTarget(NPC healer) {
        if (!active() || !owns(healer)) return null;
        NPC best = null;
        for (NPC npc : live) {
            if (!owns(npc) || npc.isDead() || npc.isHidden() || healer.getLocation().distance(npc.getLocation()) > 8
                    || npc.getHitPoints() >= npc.getMaximumHitPoints() / 2) continue;
            if (best == null || npc.getHitPoints() * best.getMaximumHitPoints() < best.getHitPoints() * npc.getMaximumHitPoints())
                best = npc;
        }
        return best;
    }

    private void startWave() {
        if (!active() || finishing || waveFailed || countBlocking() > 0 || wave >= WAVES.length) return;
        waveQueued = false;
        int current = wave++;
        ActionSender.sendConfig(player, 639, wave);
        player.sendMessage("<col=ff0000>Wave " + wave);
        if (wave == WAVES.length) DialogueManager.sendDialogue(player, DialogueManager.CALM_TALK, TZHAAR_MEJ_JAL, -1,
                "Look out, here comes TzTok-Jad!");
        clearLive();
        List<Location> anchors = new ArrayList<Location>();
        for (Location source : SPAWNS) anchors.add(translate(source));
        Collections.shuffle(anchors);
        for (int i = 0; i < WAVES[current].length; i++) {
            int id = WAVES[current][i];
            Location location = anchors.get(i % anchors.size());
            if (current == 2 && id == TZ_KEK) jadSpawn = location;
            else if ((current == 61 && id == KET_ZEK_SPAWN || id == JAD) && jadSpawn != null) location = jadSpawn;
            else if (current == 61 && location.equals(jadSpawn)) location = anchors.get((i + 1) % anchors.size());
            if (spawnNpc(id, location, false) == null) { failWave(); return; }
        }
    }
    public NPC spawnNpc(int id, Location anchor, boolean extra) {
        if (!active() || waveFailed || finishing || !isCaveNpc(id)) return null;
        NPC npc = null;
        try {
            npc = id == JAD ? new TzTokJad(id) : new FightCaveNPC(id);
            Location location = findSpawnTile(anchor, npc.size(), extra ? 2 : 6);
            if (location == null) { npc.destroy(); return null; }
            npc.setDoesWalk(true);
            npc.setFaceDir(0);
            npc.setAttribute("fightcaves", Boolean.TRUE);
            npc.setAttribute("activity", "FightCavesActivity");
            npc.setAttribute("fightCavesSession", this);
            npc.setAttribute("cavesRun", Long.valueOf(instance.getId()));
            npc.setAttribute("cavesOwner", player.getUsername());
            npc.loadEntityVariables();
            instance.spawnNpc(npc, location);
            live.add(npc);
            npc.setAttribute("enemyIndex", Short.valueOf((short) player.getIndex()));
            npc.getCombatExecutor().setVictim(player);
            npc.getMask().setInteractingEntity(player, false);
            return npc;
        } catch (RuntimeException failure) {
            if (npc != null) {
                if (instance.owns(npc)) instance.removeNpc(npc); else npc.destroy();
            }
            System.err.println("Fight Caves " + instance.getId() + " spawn failed: " + failure);
            return null;
        }
    }
    private Location findSpawnTile(Location anchor, int size, int radius) {
        if (anchor == null || !instance.contains(anchor)) return null;
        for (int r = 0; r <= radius; r++) for (int dx = -r; dx <= r; dx++) for (int dy = -r; dy <= r; dy++) {
            if (Math.max(Math.abs(dx), Math.abs(dy)) != r) continue;
            Location tile = Location.locate(anchor.getX() + dx, anchor.getY() + dy, anchor.getZ());
            if (isClearSpawn(tile, size)) return tile;
        }
        return null;
    }
    private boolean isClearSpawn(Location tile, int size) {
        if (!instance.canOccupy(tile, size)) return false;
        for (int x = tile.getX(); x < tile.getX() + size; x++) for (int y = tile.getY(); y < tile.getY() + size; y++)
            if ((Region.getClippingMask(x, y, tile.getZ()) & 0x12801ff) != 0) return false;
        for (NPC npc : live) if (owns(npc) && !npc.isDead() && !npc.isHidden()
                && tile.getX() < npc.getLocation().getX() + npc.size() && tile.getX() + size > npc.getLocation().getX()
                && tile.getY() < npc.getLocation().getY() + npc.size() && tile.getY() + size > npc.getLocation().getY()) return false;
        return true;
    }
    private void failWave() {
        waveFailed = true;
        waveQueued = false;
        clearLive();
        player.sendMessage("This wave could not spawn correctly. Please leave and restart the Fight Caves.");
        System.err.println("Fight Caves " + instance.getId() + " wave " + wave + " failed; progression stopped.");
    }
    public void pullRemaining() {
        if (!active()) return;
        int count = 0;
        for (NPC npc : new ArrayList<NPC>(live)) {
            if (!owns(npc) || npc.isDead() || npc.isHidden() || !isBlockingNpc(npc.getId())) continue;
            Location destination = findSpawnTile(player.getLocation(), npc.size(), 6);
            if (destination == null) continue;
            npc.teleport(destination, false);
            npc.setOriginalLocation(destination);
            npc.getCombatExecutor().setVictim(player);
            npc.getMask().setInteractingEntity(player, false);
            count++;
        }
        if (count > 0) player.sendMessage("<col=ff0000>" + count + " cave monster(s) pulled next to you.");
    }
    public void onNpcDeath(NPC npc, Player killer) {
        if (!active() || !owns(npc) || !npc.isDead() || finishing || waveFailed
                || Boolean.TRUE.equals(npc.getAttribute("cavesCounted"))) return;
        // A foreign/stale callback cannot award completion or progress this run.
        if (killer != null && killer != player) return;
        npc.setAttribute("cavesCounted", Boolean.TRUE);
        live.remove(npc);
        if (npc.getId() == JAD) {
            if (wave != WAVES.length) return;
            finishing = true;
            instance.schedule(4, this::finish);
            return;
        }
        if (npc.getId() == TZ_KEK) {
            if (spawnNpc(TZ_KEK_SPLIT, npc.getLocation(), true) == null
                    || spawnNpc(TZ_KEK_SPLIT, npc.getLocation(), true) == null) { failWave(); return; }
            player.sendMessage("<col=ff0000>The Tz-Kek splits in two!");
        }
        int left = countBlocking();
        if (left > 0) player.sendMessage("<col=ff0000>Wave " + wave + " - " + left + " left");
        else queueNextWave();
    }
    private int countBlocking() {
        live.removeIf(npc -> !owns(npc) || npc.isDead() || npc.isHidden());
        int count = 0;
        for (NPC npc : live) if (isBlockingNpc(npc.getId())) count++;
        return count;
    }
    private void queueNextWave() {
        if (!active() || waveQueued || finishing || waveFailed || wave >= WAVES.length || countBlocking() > 0) return;
        waveQueued = true;
        instance.schedule(WAVE_DELAY_TICKS, () -> {
            waveQueued = false;
            startWave();
        });
    }
    void finish() {
        if (!active() || !finishing) return;
        won = true;
        instance.close();
    }
    public void quit() { instance.getManager().checkThread(); instance.close(); }

    /** Called after every successful departure, including safe death, logout and ordinary teleport. */
    private void departed(Player leaving) {
        if (leaving != player || ended) return;
        ended = true; // Claim once before any inventory or packet side effects.
        try {
            if (getSession(player) == this) {
                player.removeAttribute("fightCavesSession");
                player.removeAttribute("inFightCaves");
                player.removeAttribute("teleblock");
            }
            live.clear();
            if (!started) return;
            ActionSender.sendConfig(player, 639, 0);
            if (won) {
                giveOrDrop(6570, 1);
                giveOrDrop(6529, 16064);
                DialogueManager.sendDialogue(player, DialogueManager.CALM_TALK, TZHAAR_MEJ_JAL, -1,
                        "You have defeated TzTok-Jad, I am most impressed!", "Please accept this gift as a reward.");
            } else {
                int completed = Math.max(0, wave - 1);
                giveOrDrop(6529, 2 * completed * completed + 6 * completed + 4);
                DialogueManager.sendDialogue(player, DialogueManager.CALM_TALK, TZHAAR_MEJ_JAL, -1,
                        "Don't worry JalYt, I knew you couldn't do it.");
            }
        } finally {
            // Explicit GameInstance.leave also closes this solo content's empty allocation.
            instance.close();
        }
    }
    private void giveOrDrop(int id, int amount) {
        if (!player.getInventory().addItem(id, amount)) {
            GroundItemManager.createGroundItem(new GroundItem(player, new Item(id, amount),
                    OUTSIDE_OF_CAVE, false, false, amount));
        }
    }
    private void clearLive() {
        for (NPC npc : new ArrayList<NPC>(live)) instance.removeNpc(npc);
        live.clear();
    }
}
