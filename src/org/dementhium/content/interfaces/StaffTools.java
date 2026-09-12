package org.dementhium.content.interfaces;

import java.util.ArrayList;
import java.util.List;

import org.dementhium.content.Commands;
import org.dementhium.content.dialogue.Dialogue;
import org.dementhium.content.dialogue.DialogueType;
import org.dementhium.content.dialogue.OptionAction;
import org.dementhium.model.World;
import org.dementhium.model.definition.ItemDefinition;
import org.dementhium.model.player.Player;
import org.dementhium.net.ActionSender;
import org.dementhium.util.InputHandler;

/** Server-owned routing and state for the capability-gated Staff Tools tab. */
public final class StaffTools {

    public static final int INTERFACE = 1070;
    public static final int STRING_INPUT = 90;
    public static final int INTEGER_INPUT = 91;

    private static final int FIRST_CATEGORY_BUTTON = 9;
    private static final int FIRST_ACTION_BACKGROUND = 15;
    private static final int FIRST_ACTION_BUTTON = 22;
    private static final int ACTION_ROWS = 7;
    private static final int INPUT_TIMEOUT_TICKS = 200;
    private static final String INSTALLED = "staffToolsInstalled";
    private static final String CATEGORY = "staffToolsCategory";
    private static final String PENDING = "staffToolsPending";
    private static final String CONFIRM_GUARD = "staffToolsConfirmGuard";

    private enum Category {
        ASSIST("Assist"), MODERATION("Moderate"), TRAVEL("Travel"), ADMIN("Admin");

        private final String label;

        Category(String label) {
            this.label = label;
        }
    }

    private enum Action {
        TELE_TO(Category.ASSIST, "Go to player", "teleto", 1, Input.PLAYER, false),
        VIEW_BANK(Category.ASSIST, "View bank", "viewbank", 1, Input.PLAYER, false),
        VIEW_INVENTORY(Category.ASSIST, "View inventory", "viewinventory", 1, Input.PLAYER, false),
        VIEW_EQUIPMENT(Category.ASSIST, "View equipment", "viewequipment", 1, Input.PLAYER, false),
        CHECK_ITEM(Category.ASSIST, "Check item", "checkplayer", 1, Input.PLAYER_ITEM, false),
        VIEW_PK(Category.ASSIST, "View PK stats", "viewpkstats", 1, Input.PLAYER, false),
        CHECK_TOTAL(Category.ASSIST, "Check total value", "checktotal", 2, Input.PLAYER, false),

        KICK(Category.MODERATION, "Kick player", "kick", 1, Input.PLAYER, true),
        MUTE(Category.MODERATION, "Mute (24 hours)", "mute", 1, Input.PLAYER, true),
        UNMUTE(Category.MODERATION, "Unmute player", "unmute", 1, Input.NAME, false),
        BAN(Category.MODERATION, "Ban (24 hours)", "ban", 1, Input.ONLINE_PLAYER, true),
        UNBAN(Category.MODERATION, "Unban player", "unban", 1, Input.NAME, false),

        COORDS(Category.TRAVEL, "Show coordinates", "coords", 1, Input.NONE, false),
        TELE_COORDS(Category.TRAVEL, "Teleport to coords", "tele", 1, Input.COORDINATES, false),
        MOD_ZONE(Category.TRAVEL, "Moderator zone", "modzone", 1, Input.NONE, false),
        ADMIN_ZONE(Category.TRAVEL, "Administrator zone", "adminzone", 2, Input.NONE, false),
        OPEN_BANK(Category.TRAVEL, "Open bank", "bank", 2, Input.NONE, false),

        BRING(Category.ADMIN, "Bring player", "teletome", 2, Input.PLAYER, true),
        HEAL(Category.ADMIN, "Restore health", "heal", 2, Input.NONE, false),
        PRAYER(Category.ADMIN, "Restore prayer", "prayer", 2, Input.NONE, false),
        RUN(Category.ADMIN, "Restore run", "run", 2, Input.NONE, false),
        SPEC(Category.ADMIN, "Restore special", "spec", 2, Input.NONE, false),
        GOD(Category.ADMIN, "Toggle God Mode", "god", 2, Input.NONE, false),
        SPAWN_ITEM(Category.ADMIN, "Spawn item", "item", 2, Input.ITEM_SEARCH, true);

        private final Category category;
        private final String label;
        private final String command;
        private final int rights;
        private final Input input;
        private final boolean confirmation;

        Action(Category category, String label, String command, int rights, Input input, boolean confirmation) {
            this.category = category;
            this.label = label;
            this.command = command;
            this.rights = rights;
            this.input = input;
            this.confirmation = confirmation;
        }
    }

    private enum Input {
        NONE, PLAYER, ONLINE_PLAYER, NAME, PLAYER_ITEM, COORDINATES, ITEM_SEARCH
    }

    private static final class Pending {
        private final Action action;
        private final List<String> arguments = new ArrayList<String>();
        private int coordinate;
        private int expires;
        private boolean confirming;

        private Pending(Action action) {
            this.action = action;
            touch();
        }

        private void touch() {
            expires = World.getTicks() + INPUT_TIMEOUT_TICKS;
        }
    }

    private StaffTools() {
    }

    public static boolean isEligible(Player player) {
        return player != null && player.getRights() >= 1;
    }

    public static boolean isInstalled(Player player) {
        return isEligible(player) && player.getConnection().supportsStaffToolsInterface()
                && Boolean.TRUE.equals(player.getAttribute(INSTALLED));
    }

    public static void installTab(Player player) {
        int pane = player.getConnection().getDisplayMode() >= 2 ? 746 : 548;
        int slot = pane == 746 ? 95 : 210;
        int icon = pane == 746 ? 28 : 106;
        clearPending(player);
        if (!isEligible(player) || !player.getConnection().supportsStaffToolsInterface()) {
            player.removeAttribute(INSTALLED);
            // This legacy sender's boolean is effectively "visible" despite its parameter name.
            ActionSender.sendInterfaceConfig(player, pane, icon, false);
            return;
        }
        player.setAttribute(INSTALLED, true);
        ActionSender.sendInterface(player, 1, pane, slot, INTERFACE);
        ActionSender.sendInterfaceConfig(player, pane, icon, true);
        Category category = category(player);
        player.setAttribute(CATEGORY, category);
        refresh(player);
    }

    public static boolean button(Player player, int interfaceId, int childId, int opcode) {
        if (interfaceId != INTERFACE) {
            Pending pending = pending(player);
            if (pending != null && pending.action == Action.SPAWN_ITEM && pending.arguments.isEmpty()) {
                clearPending(player);
            }
            return false;
        }
        if (opcode != 6) {
            return true;
        }
        if (!isInstalled(player)) {
            clearPending(player);
            player.sendMessage("You are not authorized to use Staff Tools.");
            return true;
        }
        Pending pending = pending(player);
        if (pending != null && pending.confirming) {
            if (pending.action == Action.SPAWN_ITEM) {
                // If the chatbox was dismissed, a new panel choice should
                // cancel the stale modal state and work immediately.
                clearPending(player);
            } else {
                if (childId == FIRST_ACTION_BUTTON) {
                    executeConfirmed(player, pending);
                } else if (childId == FIRST_ACTION_BUTTON + 1) {
                    player.setAttribute(CONFIRM_GUARD, World.getTicks());
                    clearPending(player);
                    player.sendMessage("Staff Tools action cancelled.");
                    refresh(player);
                }
                return true;
            }
        }
        Object guard = player.getAttribute(CONFIRM_GUARD);
        if ((childId == FIRST_ACTION_BUTTON || childId == FIRST_ACTION_BUTTON + 1)
                && guard instanceof Integer && ((Integer) guard).intValue() == World.getTicks()) {
            return true;
        }
        if (childId >= FIRST_CATEGORY_BUTTON && childId < FIRST_CATEGORY_BUTTON + Category.values().length) {
            clearPending(player);
            player.setAttribute(CATEGORY, Category.values()[childId - FIRST_CATEGORY_BUTTON]);
            refresh(player);
            return true;
        }
        if (childId >= FIRST_ACTION_BUTTON && childId < FIRST_ACTION_BUTTON + ACTION_ROWS) {
            List<Action> actions = actions(player, category(player));
            int index = childId - FIRST_ACTION_BUTTON;
            if (index < actions.size()) {
                begin(player, actions.get(index));
            }
            return true;
        }
        return true;
    }

    public static boolean handleStringInput(Player player, String value) {
        if (((Integer) player.getAttribute("inputId", -1)).intValue() != STRING_INPUT) {
            return false;
        }
        player.removeAttribute("inputId");
        Pending pending = validPending(player);
        if (pending == null) {
            player.sendMessage("That Staff Tools prompt is no longer active.");
            return true;
        }
        String target = cleanName(value);
        if (target == null) {
            clearPending(player);
            player.sendMessage("Enter a valid player name.");
            refresh(player);
            return true;
        }
        if (pending.action.input != Input.NAME && findOnline(pending.action.command, target) == null) {
            clearPending(player);
            player.sendMessage("That player is not online.");
            refresh(player);
            return true;
        }
        pending.arguments.add(target);
        pending.touch();
        if (pending.action.input == Input.PLAYER_ITEM) {
            InputHandler.requestIntegerInput(player, INTEGER_INPUT, "Item ID to check on " + target + ":");
            return true;
        }
        finishInput(player, pending);
        return true;
    }

    public static boolean handleIntegerInput(Player player, int value) {
        if (((Integer) player.getAttribute("inputId", -1)).intValue() != INTEGER_INPUT) {
            return false;
        }
        player.removeAttribute("inputId");
        Pending pending = validPending(player);
        if (pending == null) {
            player.sendMessage("That Staff Tools prompt is no longer active.");
            return true;
        }
        if (pending.action == Action.CHECK_ITEM) {
            if (value < 0 || value >= ItemDefinition.MAX_SIZE) {
                rejectNumber(player, "That item ID is outside the valid item range.");
                return true;
            }
            pending.arguments.add(String.valueOf(value));
            finishInput(player, pending);
            return true;
        }
        if (pending.action == Action.SPAWN_ITEM) {
            if (value < 1) {
                rejectNumber(player, "Item amount must be at least 1.");
                return true;
            }
            pending.arguments.add(String.valueOf(value));
            finishInput(player, pending);
            return true;
        }
        if (pending.action != Action.TELE_COORDS) {
            clearPending(player);
            player.sendMessage("That Staff Tools number prompt is no longer valid.");
            refresh(player);
            return true;
        }
        if (pending.coordinate < 2 && (value < 0 || value > 16383)) {
            rejectNumber(player, "Coordinates must be between 0 and 16383.");
            return true;
        }
        if (pending.coordinate == 2 && (value < 0 || value > 3)) {
            rejectNumber(player, "Plane must be between 0 and 3.");
            return true;
        }
        pending.arguments.add(String.valueOf(value));
        pending.coordinate++;
        pending.touch();
        if (pending.coordinate < 3) {
            InputHandler.requestIntegerInput(player, INTEGER_INPUT,
                    pending.coordinate == 1 ? "Destination Y coordinate:" : "Destination plane (0-3):");
        } else {
            finishInput(player, pending);
        }
        return true;
    }

    public static void clearPending(Player player) {
        if (player == null) {
            return;
        }
        Pending pending = pending(player);
        player.removeAttribute(PENDING);
        if (pending != null && pending.action == Action.SPAWN_ITEM && pending.confirming
                && player.getAttribute("dialogue") != null) {
            player.setAttribute("dialogue", null);
            ActionSender.sendCloseChatBox(player);
        }
        if (pending != null && pending.action == Action.SPAWN_ITEM && pending.arguments.isEmpty()) {
            closeItemSearch(player);
        }
        int inputId = ((Integer) player.getAttribute("inputId", -1)).intValue();
        if (inputId == STRING_INPUT || inputId == INTEGER_INPUT) {
            player.removeAttribute("inputId");
        }
    }

    /** Consumes GE item-search selections only while the Staff Tools picker owns the context. */
    public static boolean handleItemSearchSelection(Player player, int itemId) {
        Pending pending = pending(player);
        if (pending == null || pending.action != Action.SPAWN_ITEM) {
            return false;
        }
        pending = validPending(player);
        if (pending == null) {
            player.sendMessage("That Staff Tools item search is no longer active.");
            return true;
        }
        if (itemId < 0 || itemId >= ItemDefinition.MAX_SIZE) {
            clearPending(player);
            player.sendMessage("That item is outside the valid item range.");
            refresh(player);
            return true;
        }
        ItemDefinition definition = ItemDefinition.forId(itemId);
        if (definition == null || definition.getName() == null || definition.getName().trim().length() == 0) {
            clearPending(player);
            player.sendMessage("That item cannot be spawned.");
            refresh(player);
            return true;
        }
        pending.arguments.add(String.valueOf(itemId));
        pending.touch();
        closeItemSearch(player);
        InputHandler.requestIntegerInput(player, INTEGER_INPUT, "Amount of " + definition.getName() + " to spawn:");
        return true;
    }

    private static void begin(Player player, Action action) {
        clearPending(player);
        if (!authorized(player, action)) {
            player.sendMessage("You are not authorized to use that Staff Tools action.");
            refresh(player);
            return;
        }
        Pending pending = new Pending(action);
        player.setAttribute(PENDING, pending);
        switch (action.input) {
        case NONE:
            execute(player, pending);
            break;
        case COORDINATES:
            InputHandler.requestIntegerInput(player, INTEGER_INPUT, "Destination X coordinate:");
            break;
        case ITEM_SEARCH:
            ActionSender.sendItemSearch(player, "");
            break;
        case PLAYER_ITEM:
        case PLAYER:
        case ONLINE_PLAYER:
        case NAME:
            InputHandler.requestStringInput(player, STRING_INPUT, prompt(action));
            break;
        }
    }

    private static String prompt(Action action) {
        switch (action) {
        case BAN:
        case UNBAN:
            return "Player name for 24-hour " + (action == Action.BAN ? "ban" : "unban") + ":";
        case MUTE:
        case UNMUTE:
            return "Player name to " + (action == Action.MUTE ? "mute for 24 hours" : "unmute") + ":";
        case CHECK_ITEM:
            return "Online player name to inspect:";
        default:
            return "Online player name for " + action.label + ":";
        }
    }

    private static void finishInput(Player player, Pending pending) {
        if (pending.action.confirmation) {
            pending.confirming = true;
            pending.touch();
            if (pending.action == Action.SPAWN_ITEM) {
                sendSpawnConfirmation(player, pending);
            } else {
                refresh(player);
            }
        } else {
            execute(player, pending);
        }
    }

    private static void sendSpawnConfirmation(final Player player, final Pending pending) {
        if (pending.arguments.size() != 2) {
            clearPending(player);
            player.sendMessage("That Staff Tools item request is incomplete.");
            refresh(player);
            return;
        }
        int selectedId = Integer.parseInt(pending.arguments.get(0));
        final int baseId = unnotedVariant(selectedId);
        final int noteId = notedVariant(baseId);
        final String amount = pending.arguments.get(1);
        ItemDefinition definition = ItemDefinition.forId(baseId);
        final Dialogue dialogue = new Dialogue();
        dialogue.setType(DialogueType.OPTION);
        dialogue.getMessage().add("Spawn " + definition.getName() + " x " + amount);
        dialogue.getActions().add(new OptionAction() {
            @Override
            public boolean handle(Player p) {
                chooseSpawnForm(p, pending, baseId);
                return true;
            }
        });
        if (noteId != -1) {
            dialogue.getMessage().add("Spawn noted " + definition.getName() + " x " + amount);
            dialogue.getActions().add(new OptionAction() {
                @Override
                public boolean handle(Player p) {
                    chooseSpawnForm(p, pending, noteId);
                    return true;
                }
            });
        }
        dialogue.getMessage().add("Cancel");
        dialogue.getActions().add(new OptionAction() {
            @Override
            public boolean handle(Player p) {
                cancelSpawn(p, pending);
                return true;
            }
        });
        dialogue.send(player);
    }

    private static void chooseSpawnForm(Player player, Pending expected, int itemId) {
        Pending pending = validPending(player);
        if (pending != expected || !expected.confirming) {
            player.sendMessage("That Staff Tools confirmation has expired.");
            return;
        }
        expected.arguments.set(0, String.valueOf(itemId));
        executeConfirmed(player, expected);
    }

    private static void cancelSpawn(Player player, Pending expected) {
        Pending pending = validPending(player);
        if (pending != expected || !expected.confirming) {
            player.sendMessage("That Staff Tools confirmation has expired.");
            return;
        }
        player.setAttribute(CONFIRM_GUARD, World.getTicks());
        clearPending(player);
        player.sendMessage("Staff Tools item spawn cancelled.");
        refresh(player);
    }

    private static int unnotedVariant(int itemId) {
        ItemDefinition definition = ItemDefinition.forId(itemId);
        if (!definition.isNoted()) {
            return itemId;
        }
        int candidateId = itemId == 10843 ? 10828 : itemId - 1;
        if (candidateId >= 0 && sameItemName(definition, ItemDefinition.forId(candidateId))
                && !ItemDefinition.forId(candidateId).isNoted()) {
            return candidateId;
        }
        return itemId;
    }

    private static int notedVariant(int itemId) {
        ItemDefinition definition = ItemDefinition.forId(itemId);
        if (definition.isNoted() || definition.isStackable()) {
            return -1;
        }
        int candidateId = itemId == 10828 ? 10843 : itemId + 1;
        if (candidateId >= ItemDefinition.MAX_SIZE) {
            return -1;
        }
        ItemDefinition candidate = ItemDefinition.forId(candidateId);
        return candidate.isNoted() && sameItemName(definition, candidate) ? candidateId : -1;
    }

    private static boolean sameItemName(ItemDefinition first, ItemDefinition second) {
        return first != null && second != null && first.getName() != null
                && first.getName().equals(second.getName());
    }

    private static void executeConfirmed(Player player, Pending pending) {
        if (pending != validPending(player) || !pending.confirming) {
            clearPending(player);
            player.sendMessage("That Staff Tools confirmation has expired.");
            refresh(player);
            return;
        }
        player.setAttribute(CONFIRM_GUARD, World.getTicks());
        execute(player, pending);
    }

    private static void execute(Player player, Pending pending) {
        Action action = pending.action;
        List<String> arguments = new ArrayList<String>(pending.arguments);
        clearPending(player); // Consume before dispatch so duplicate packets cannot execute twice.
        if (!authorized(player, action)) {
            player.sendMessage("You are no longer authorized to use that Staff Tools action.");
            refresh(player);
            return;
        }
        if (action.input == Input.ONLINE_PLAYER
                && (arguments.isEmpty() || findOnline(action.command, arguments.get(0)) == null)) {
            player.sendMessage("That player is no longer online; the action was cancelled.");
            refresh(player);
            return;
        }
        String[] command = new String[arguments.size() + 1];
        command[0] = action.command;
        for (int i = 0; i < arguments.size(); i++) {
            command[i + 1] = arguments.get(i);
        }
        Commands.handle(player, command);
        refresh(player);
    }

    private static void closeItemSearch(Player player) {
        ActionSender.sendCloseInterface(player, 752, 7);
        // The GE picker expands the chatbox outside the normal dialogue slot.
        // Closing only child 7 leaves that expanded container visibly blank
        // when the search is cancelled by another Staff Tools selection.
        ActionSender.sendCloseChatBox(player);
    }

    private static boolean authorized(Player player, Action action) {
        return isInstalled(player) && player.getRights() >= action.rights;
    }

    private static Pending validPending(Player player) {
        Pending pending = pending(player);
        if (pending == null || !authorized(player, pending.action) || pending.expires < World.getTicks()) {
            clearPending(player);
            return null;
        }
        return pending;
    }

    private static Pending pending(Player player) {
        Object value = player.getAttribute(PENDING);
        return value instanceof Pending ? (Pending) value : null;
    }

    private static Player findOnline(String command, String target) {
        String[] parts = target.split(" +");
        String[] full = new String[parts.length + 1];
        full[0] = command;
        System.arraycopy(parts, 0, full, 1, parts.length);
        return Commands.getPlayerFromCommand(full, 0);
    }

    private static String cleanName(String value) {
        if (value == null) {
            return null;
        }
        String name = value.trim().replaceAll(" +", " ");
        if (name.length() < 1 || name.length() > 24) {
            return null;
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isISOControl(c)) {
                return null;
            }
        }
        return name;
    }

    private static void rejectNumber(Player player, String message) {
        clearPending(player);
        player.sendMessage(message);
        refresh(player);
    }

    private static Category category(Player player) {
        Object value = player.getAttribute(CATEGORY);
        return value instanceof Category ? (Category) value : Category.ASSIST;
    }

    private static List<Action> actions(Player player, Category category) {
        List<Action> actions = new ArrayList<Action>();
        for (Action action : Action.values()) {
            if (action.category == category && player.getRights() >= action.rights) {
                actions.add(action);
            }
        }
        return actions;
    }

    private static void refresh(Player player) {
        if (!isInstalled(player)) {
            return;
        }
        Pending pending = pending(player);
        String role = player.getRights() >= 2 ? "Administrator actions enabled" : "Moderator actions enabled";
        ActionSender.sendString(player, role, INTERFACE, 5);
        if (pending != null && pending.confirming && pending.action != Action.SPAWN_ITEM) {
            ActionSender.sendString(player, "CONFIRM " + pending.action.label.toUpperCase(), INTERFACE, 13);
            ActionSender.sendString(player, confirmationDetail(pending), INTERFACE, 29);
            setRows(player, new String[] { "Confirm", "Cancel" });
            return;
        }
        Category category = category(player);
        ActionSender.sendString(player,
                category == Category.ADMIN && player.getRights() < 2 ? "ADMIN - LOCKED" : category.label.toUpperCase(),
                INTERFACE, 13);
        ActionSender.sendString(player, "Buttons use existing ::commands", INTERFACE, 29);
        List<Action> actions = actions(player, category);
        String[] labels = new String[actions.size()];
        for (int i = 0; i < actions.size(); i++) {
            labels[i] = actions.get(i).label;
        }
        setRows(player, labels);
    }

    private static String confirmationDetail(Pending pending) {
        if (pending.action == Action.SPAWN_ITEM && pending.arguments.size() == 2) {
            int itemId = Integer.parseInt(pending.arguments.get(0));
            ItemDefinition definition = ItemDefinition.forId(itemId);
            return definition.getName() + " x " + pending.arguments.get(1) + " (ID " + itemId + ")";
        }
        return "Target: " + (pending.arguments.isEmpty() ? "" : pending.arguments.get(0));
    }

    private static void setRows(Player player, String[] labels) {
        for (int i = 0; i < ACTION_ROWS; i++) {
            boolean visible = i < labels.length;
            ActionSender.sendString(player, visible ? labels[i] : "", INTERFACE, FIRST_ACTION_BUTTON + i);
            ActionSender.sendInterfaceConfig(player, INTERFACE, FIRST_ACTION_BACKGROUND + i, visible);
            ActionSender.sendInterfaceConfig(player, INTERFACE, FIRST_ACTION_BUTTON + i, visible);
        }
    }

}
