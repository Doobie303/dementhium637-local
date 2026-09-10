package org.dementhium.content.instance;

import java.util.*;
import org.dementhium.model.instance.*;
import org.dementhium.model.player.Player;

/** Administrator-only operational controls. All output and mutations execute on the world cycle. */
public final class InstanceOperations {
    private InstanceOperations() { }
    public static void command(Player player, String[] args) { command(InstanceManager.getSingleton(), player, args); }
    public static void command(InstanceManager manager, Player player, String[] args) {
        if (player.getRights() < 2) { player.sendMessage("Administrator access required."); return; }
        if (!manager.isCycleThread()) {
            final String[] copy = args.clone();
            manager.submit(() -> { command(manager, player, copy); return null; })
                .whenComplete((ignored, failure) -> { if (failure != null) player.sendMessage("Instance request unavailable; try again shortly."); });
            return;
        }
        try {
            String option = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "stats";
            if (option.equals("stats")) { for (String line : manager.diagnostics()) player.sendMessage(line); }
            else if (option.equals("list")) {
                int page = args.length > 2 ? Integer.parseInt(args[2]) : 1;
                List<GameInstance> all = manager.getInstances();
                if (page < 1 || page > Math.max(1, (all.size() + 7) / 8)) throw new IllegalArgumentException("Invalid page");
                player.sendMessage("Instances page " + page + "/" + Math.max(1, (all.size() + 7) / 8));
                for (int n = (page - 1) * 8; n < Math.min(page * 8, all.size()); n++) player.sendMessage(all.get(n).describe());
            } else if (option.equals("recent")) {
                for (String line : manager.recentClosures()) player.sendMessage(line);
            } else if ((option.equals("inspect") || option.equals("close")) && args.length == 3) {
                GameInstance instance = manager.get(Long.parseLong(args[2]));
                if (instance == null) { player.sendMessage("No managed instance with that ID."); return; }
                if (option.equals("inspect")) { for (String line : instance.inspect()) player.sendMessage(line); }
                else {
                    boolean closed = instance.close("admin " + player.getUsername());
                    player.sendMessage(closed ? "Instance closed and released." : "Cleanup incomplete; allocation quarantined for retry.");
                    if (!closed) for (String line : instance.getCloseFailures()) player.sendMessage(line);
                    System.out.println("Instance admin close by " + player.getUsername() + ": #" + instance.getId() + " released=" + closed);
                }
            } else player.sendMessage("Use ::instances stats|list [page]|inspect <id>|close <id>|recent");
        } catch (IllegalArgumentException failure) { player.sendMessage("Invalid instance command: " + failure.getMessage()); }
    }
}
