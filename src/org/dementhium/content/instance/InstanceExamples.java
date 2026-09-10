package org.dementhium.content.instance;

import java.util.LinkedHashMap;
import java.util.Map;
import org.dementhium.model.Item;
import org.dementhium.model.Location;
import org.dementhium.model.instance.GameInstance;
import org.dementhium.model.instance.InstanceAccess;
import org.dementhium.model.instance.InstanceManager;
import org.dementhium.model.instance.InstanceTemplate;
import org.dementhium.model.map.GameObject;
import org.dementhium.model.mask.Animation;
import org.dementhium.model.player.Player;
import org.dementhium.model.player.Skills;

/** Admin-accessible content proofs, not a public quest or a migration of ordinary woodcutting. */
public final class InstanceExamples {
    public enum Kind { QUEST, SKILL }
    public enum QuestStage { FIND_CLUE, OPEN_CHEST, COMPLETE }
    public static final int HARVEST_CYCLES = 3, RESPAWN_CYCLES = 8;
    private static final String ATTRIBUTE = "instanceExample";
    private static final InstanceTemplate QUEST = template("proof.quest");
    private static final InstanceTemplate SKILL = template("proof.skilling");
    private final Player player;
    private final Kind kind;
    private GameInstance instance;
    private GameObject clue, chest, portal, resource;
    private QuestStage stage = QuestStage.FIND_CLUE;
    private boolean ended, harvesting, depleted;
    private int questRewards, harvests;

    private static InstanceTemplate template(String id) {
        Map<String, InstanceTemplate.Tile> anchors = new LinkedHashMap<String, InstanceTemplate.Tile>();
        anchors.put("entry", new InstanceTemplate.Tile(44, 19, 0));
        anchors.put("clue", new InstanceTemplate.Tile(42, 19, 0));
        anchors.put("chest", new InstanceTemplate.Tile(46, 19, 0));
        anchors.put("resource", new InstanceTemplate.Tile(48, 18, 0));
        anchors.put("exit", new InstanceTemplate.Tile(44, 20, 0));
        return new InstanceTemplate(id, 360, 648, 8, 8, 0, 0,
                InstanceTemplate.SourcePolicy.SNAPSHOT_LOADED_WORLD, anchors);
    }
    private InstanceExamples(Player player, Kind kind) { this.player = player; this.kind = kind; }
    public static InstanceExamples get(Player player) { return player.getAttribute(ATTRIBUTE); }
    public GameInstance getInstance() { return instance; }
    public QuestStage getQuestStage() { instance.getManager().checkThread(); return stage; }
    public int getQuestRewards() { instance.getManager().checkThread(); return questRewards; }
    public int getHarvests() { instance.getManager().checkThread(); return harvests; }
    public boolean isDepleted() { instance.getManager().checkThread(); return depleted; }

    /** Called on a world cycle. Test managers use the same admission and cleanup code. */
    public static InstanceExamples start(InstanceManager manager, Player player, Kind kind) {
        manager.checkThread();
        if (player == null || kind == null || player.getRights() < 2 || !player.isOnline() || player.isDead()
                || player.getConnection().isDisconnected() || player.getConnection().isInLobby()) return null;
        InstanceExamples existing = get(player);
        if (existing != null) return existing;
        if (InstanceAccess.owner(player) != null || player.getFamiliar() != null
                || (player.getActivity() != null && player.getActivity().isRunning())) {
            player.sendMessage("Leave your current activity before entering an example room.");
            return null;
        }
        InstanceExamples session = new InstanceExamples(player, kind);
        try {
            session.instance = (kind == Kind.QUEST ? QUEST : SKILL).create(manager, 1, player.getLocation(), map -> {
                session.instance = map;
                map.setDepartureHandler(session::departed);
                // Seed fresh owned resources on every entry. Their state is never copied from another room.
                session.portal = map.spawnObject(2465, map.location("exit"), 10, 0);
                if (kind == Kind.QUEST) {
                    // Cache object 1 has first-option Search; decorative crate 354 has no actions.
                    session.clue = map.spawnObject(1, map.location("clue"), 10, 0);
                    session.chest = map.spawnObject(375, map.location("chest"), 10, 0);
                } else session.resource = map.spawnObject(1276, map.location("resource"), 10, 0);
                if (!map.canOccupy(map.location("entry"), 1)) throw new IllegalStateException("Example entry is blocked");
            });
            if (session.instance == null) {
                player.sendMessage("No example room is available. Please try again shortly.");
                return null;
            }
            session.instance.enter(player, session.instance.location("entry"));
            player.setAttribute(ATTRIBUTE, session);
            player.sendMessage(kind == Kind.QUEST
                    ? "Search the crate for a clue, then open the chest. Use the portal to leave."
                    : "Click the tree to gather a log. It regrows after depletion. Use the portal to leave.");
            return session;
        } catch (RuntimeException failure) {
            if (session.instance != null) session.instance.close();
            System.err.println("Instance example entry failed: " + failure);
            player.sendMessage("The example room could not be started.");
            return null;
        }
    }

    public static void command(Player player, String[] args) {
        if (player.getRights() < 2) return;
        if (args.length != 2 || (!args[1].equalsIgnoreCase("quest") && !args[1].equalsIgnoreCase("skill")
                && !args[1].equalsIgnoreCase("exit"))) {
            player.sendMessage("Use ::instanceexample quest, skill or exit."); return;
        }
        InstanceManager manager = InstanceManager.getSingleton();
        Runnable action = () -> {
            if (player.getRights() < 2 || !player.isOnline() || player.getConnection().isDisconnected()) return;
            if (args[1].equalsIgnoreCase("exit")) {
                InstanceExamples session = get(player);
                if (session != null) InstanceAccess.depart(player, false);
            } else start(manager, player, args[1].equalsIgnoreCase("quest") ? Kind.QUEST : Kind.SKILL);
        };
        if (manager.isCycleThread()) action.run();
        else manager.submit(() -> { action.run(); return null; }).exceptionally(failure -> {
            System.err.println("Instance example request failed: " + failure); return null;
        });
    }

    private boolean active() {
        return !ended && instance.isActive() && instance.isMember(player) && get(player) == this
                && player.isOnline() && !player.isDead() && !player.getConnection().isDisconnected();
    }
    private boolean adjacent(GameObject object) {
        int width = object.getDefinition().getSizeX(), height = object.getDefinition().getSizeY();
        int dx = player.getLocation().getX() - object.getLocation().getX();
        int dy = player.getLocation().getY() - object.getLocation().getY();
        return dx >= -1 && dy >= -1 && dx <= width && dy <= height;
    }
    /** Called only after routing; also checks range, ownership and current object identity itself. */
    public void interact(Player caller, GameObject object, boolean firstOption) {
        instance.getManager().checkThread();
        if (caller != player || !active() || object == null || !InstanceAccess.canInteract(player, object)
                || !adjacent(object) || !firstOption) return;
        if (object == portal) { InstanceAccess.depart(player, false); return; }
        if (kind == Kind.QUEST) {
            if (object == clue && stage == QuestStage.FIND_CLUE) {
                stage = QuestStage.OPEN_CHEST;
                player.sendMessage("The crate contains a clue to the chest's lock.");
            } else if (object == chest && stage == QuestStage.FIND_CLUE) {
                player.sendMessage("Search the crate for the lock's clue first.");
            } else if (object == chest && stage == QuestStage.OPEN_CHEST) {
                if (!player.getInventory().addItem(995, 1)) {
                    player.sendMessage("Make room for your one-coin example reward."); return;
                }
                stage = QuestStage.COMPLETE; questRewards++;
                player.sendMessage("Scene complete. You receive one coin. Use the portal to leave.");
            }
        } else if (object == resource && !depleted && !harvesting) {
            harvesting = true;
            final Location startedAt = player.getLocation();
            final long revision = player.getInstanceRevision();
            try {
                player.animate(Animation.create(879));
                instance.schedule(HARVEST_CYCLES, () -> {
                    try {
                        if (!active() || player.getInstanceRevision() != revision || !startedAt.equals(player.getLocation())
                                || resource != object || !InstanceAccess.canInteract(player, object)) return;
                        if (!player.getInventory().addItem(1511, 1)) {
                            player.sendMessage("Make room in your inventory for a log."); return;
                        }
                        harvests++;
                        player.getSkills().addExperience(Skills.WOODCUTTING, 25);
                        replaceResource(1342); depleted = true;
                        instance.schedule(RESPAWN_CYCLES, () -> {
                            try {
                                if (instance.isActive() && !ended) { replaceResource(1276); depleted = false; }
                            } catch (RuntimeException failure) { instance.close(); throw failure; }
                        });
                    } catch (RuntimeException failure) { instance.close(); throw failure; }
                    finally { harvesting = false; if (active()) player.animate(Animation.create(-1)); }
                });
            } catch (RuntimeException failure) { harvesting = false; instance.close(); throw failure; }
        }
    }
    private void replaceResource(int id) {
        instance.removeObject(resource);
        resource = instance.spawnObject(id, instance.location("resource"), 10, 0);
    }
    private void departed(Player leaving) {
        ended = true; harvesting = false;
        if (get(leaving) == this) leaving.removeAttribute(ATTRIBUTE);
        leaving.animate(Animation.create(-1));
        if (instance.getMemberCount() == 0) instance.close();
    }
}
