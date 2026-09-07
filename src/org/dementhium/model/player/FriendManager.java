package org.dementhium.model.player;

import org.dementhium.io.FileUtilities;
import org.dementhium.io.PlayerLoader;
import org.dementhium.model.World;
import org.dementhium.net.ActionSender;
import org.dementhium.util.Misc;
import org.dementhium.util.handlers.DisplayNamesHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 'Mystic Flow
 */
public final class FriendManager {

    private final List<String> friends = new ArrayList<String>(200);
    private final List<String> ignores = new ArrayList<String>(100);
    private final Player player;

    public FriendManager(Player player) {
        this.player = player;
    }

    public void loadFriendList() {
        if (friends.size() > 0) {
            for (String friend : friends) {
                Player other = World.getWorld().getPlayerInServer(friend.toLowerCase());
                //Player otherDisplayName = World.getWorld().getPlayerInServerDisplayName(friend);
                //if (otherDisplayName != null)
                	//other = otherDisplayName;
                boolean isOnline = other != null;
                if (other != null && (other.isInvisible() || other.getSettings().getPrivateChatSetting() == 2
                		|| (other.getSettings().getPrivateChatSetting() == 1 && !other.getFriendManager().getFriends().contains(player.getFormattedUsername()))))
                	isOnline = false;
                String displayName = Misc.formatPlayerNameForDisplay(friend);
                if (other != null)
                	displayName = other.getDisplayName();
                else if (DisplayNamesHandler.getDisplayNameFromUsername(Misc.formatPlayerNameForDisplay(Misc.formatPlayerNameForDisplay(friend))) != null)
                	displayName = DisplayNamesHandler.getDisplayNameFromUsername(Misc.formatPlayerNameForDisplay(friend));
                ActionSender.sendFriend(player, other == null ? Misc.formatPlayerNameForDisplay(friend) : other.getFormattedUsername(), displayName, isOnline ? 0 : 1, isOnline, false, other != null && other.getConnection().isInLobby());
            }
        } else {
            ActionSender.sendUnlockFriendList(player);
        }
        ActionSender.sendPrivateChatSetting(player, player.getSettings().getPrivateChatSetting());
    }

    public void loadIgnoreList() {
        for (String Ignore : ignores) {
            ActionSender.sendIgnore(player, Ignore, Ignore);
        }
    }

    public void updateFriend(String friend) {
        Player other = World.getWorld().getPlayerInServer(friend.toLowerCase());
        //Player otherDisplayName = World.getWorld().getPlayerInServerDisplayName(friend); //delete this stuff, we use new display name system now
        //if (otherDisplayName != null)
        	//other = otherDisplayName;
        boolean isOnline = other != null;
        if (other != null && (other.isInvisible() || other.getSettings().getPrivateChatSetting() == 2
        		|| (other.getSettings().getPrivateChatSetting() == 1 && !other.getFriendManager().getFriends().contains(player.getFormattedUsername()))))
        	isOnline = false;
        String displayName = Misc.formatPlayerNameForDisplay(friend);
        if (other != null)
        	displayName = other.getDisplayName();
        else if (DisplayNamesHandler.getDisplayNameFromUsername(Misc.formatPlayerNameForDisplay(Misc.formatPlayerNameForDisplay(friend))) != null)
        	displayName = DisplayNamesHandler.getDisplayNameFromUsername(Misc.formatPlayerNameForDisplay(friend));
        ActionSender.sendFriend(player, other == null ? Misc.formatPlayerNameForDisplay(friend) : other.getFormattedUsername(), displayName, isOnline ? 0 : 1, isOnline, true, other != null && other.getConnection().isInLobby());
    }

    public void updateFriend(String friend, Player other) {
        boolean isOnline = other != null;
        if (other != null && (other.isInvisible() || other.getSettings().getPrivateChatSetting() == 2
        		|| (other.getSettings().getPrivateChatSetting() == 1 && !other.getFriendManager().getFriends().contains(player.getFormattedUsername()))))
        	isOnline = false;
        String displayName = Misc.formatPlayerNameForDisplay(friend);
        if (other != null)
        	displayName = other.getDisplayName();
        else if (DisplayNamesHandler.getDisplayNameFromUsername(Misc.formatPlayerNameForDisplay(Misc.formatPlayerNameForDisplay(friend))) != null)
        	displayName = DisplayNamesHandler.getDisplayNameFromUsername(Misc.formatPlayerNameForDisplay(friend));
        ActionSender.sendFriend(player, other == null ? Misc.formatPlayerNameForDisplay(friend) : other.getFormattedUsername(), displayName, isOnline ? 0 : 1, isOnline, true, other != null && other.getConnection().isInLobby());
    }


    public void addFriend(String name) {
        if (friends.size() >= 200
                || name == null
                || name.equals("")
                || friends.contains(name)
                || ignores.contains(name)
                || name.equals(player.getDisplayName())
                || Misc.formatPlayerNameForDisplay(name).equals(player.getFormattedUsername())
                )
            return;
        if (DisplayNamesHandler.getDisplayNames().containsKey(name))
        	name = Misc.formatPlayerNameForDisplay(DisplayNamesHandler.getDisplayNames().get(name));
        else if (FileUtilities.exists(PlayerLoader.DIRECTORY + name.toLowerCase() + PlayerLoader.EXTENSION))
        	name = Misc.formatPlayerNameForDisplay(name);
        else {
        	player.sendMessage("That is an invalid player name!"); //make this real message
        	return;
        }
        friends.add(name);
        updateFriend(name);
    }

    public void addIgnore(String ignore) {
        if (ignores.size() >= 100 || ignore == null || friends.contains(ignore) || ignores.contains(ignore))
            return;
        ignores.add(Misc.formatPlayerNameForDisplay(ignore));
        ActionSender.sendIgnore(player, ignore, Misc.formatPlayerNameForDisplay(ignore));
    }

    public void removeIgnore(String ignore) {
        if (ignore == null || !ignores.contains(ignore))
            return;
        ignores.remove(Misc.formatPlayerNameForDisplay(ignore));
    }

    public void removeFriend(String friend) {
        if (friend == null)
            return;
        friends.remove(friend);
    }

    public List<String> getFriends() {
        return friends;
    }

    public List<String> getIgnores() {
        return ignores;
    }

}
