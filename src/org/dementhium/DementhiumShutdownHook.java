package org.dementhium;

import org.dementhium.io.XMLHandler;
import org.dementhium.model.World;
import org.dementhium.model.player.Player;
import org.dementhium.util.Constants;
import org.dementhium.util.handlers.DisplayNamesHandler;
import org.dementhium.util.handlers.OffencesHandler;


/**
 * Our shutdown hook.
 *
 * @author Emperor
 */
public class DementhiumShutdownHook extends Thread {

    /**
     * The singleton.
     */
    private static final DementhiumShutdownHook SINGLETON = new DementhiumShutdownHook();

    /**
     * Constructs a new {@code DementhiumShutdownHook} {@code Object}.
     */
    private DementhiumShutdownHook() {
        System.out.println("Shutdown hook initialized!");
    }

    /**
     * If the shutdown hook is/has running/runned.
     */
    public boolean activated = false;

    @Override
    public void run() {
        activated = true;
        System.out.println("Shutting down "+Constants.SERVER_NAME+"...");
        int failCount = 0;
        System.out.println("Preparing players for shutdown...");
        for (Player player : World.getWorld().getPlayers()) {
            if (player == null) {
                continue;
            }
            try {
                if (player.getActivity() != null) {
                    player.getActivity().forceEnd(player);
                }
                player.closeAll(false, false);
        		if (player.interfaceItems != null) {
        			player.getInventory().addAllDropable(player.interfaceItems);
        			player.interfaceItems = null;
        		}
                failCount = 0;
                while (!World.getWorld().getPlayerLoader().save(player)) {
                    if (failCount++ > 2) {
                        System.out.println("Player "+player.getUsername()+" could not be saved!");
                        break;
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        System.out.println("Players saved.");
        System.out.println("Saving clans...");
        try {
            XMLHandler.toXML(OffencesHandler.DIRECTORY + "clans.xml",
                    World.getWorld().getClanManager().getClans());
            System.out.println("Clans succesfully saved.");
        } catch (Throwable e) {
            e.printStackTrace();
        }
        System.out.println("Saving DisplayNames...");
        try {
            XMLHandler.toXML(DisplayNamesHandler.DIRECTORY + "DisplayNames.xml",
                    World.getWorld().getDisplayNamesHandler().getDisplayNames());
            System.out.println("DisplayNames succesfully saved.");
        } catch (Throwable e) {
            e.printStackTrace();
        }
        System.out.println("Saving punishments...");
        World.getWorld().getOffencesHandler().save();
        System.out.println("Saved punishments.");
        System.out.println("Saving rights...");
        World.getWorld().getRightsHandler().save();
        System.out.println("Saved rights.");
    }

    /**
     * @return the singleton
     */
    public static DementhiumShutdownHook getSingleton() {
        return SINGLETON;
    }
}
