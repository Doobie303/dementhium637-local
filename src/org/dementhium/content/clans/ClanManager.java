package org.dementhium.content.clans;

import org.dementhium.io.XMLHandler;
import org.dementhium.model.World;
import org.dementhium.model.player.Player;
import org.dementhium.net.ActionSender;
import org.dementhium.tickable.Tick;
import org.dementhium.util.Logger;
import org.dementhium.util.Misc;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author 'Mystic Flow
 */
public class ClanManager {

	private static Map<String, Clan> clans;

	public ClanManager() {
		System.out.println("Loading clans....");
		try {
			clans = XMLHandler.fromXML("data/xml/clans.xml");
		} catch (Throwable e) {
			e.printStackTrace();
		}
		if (clans == null) {
			clans = new HashMap<String, Clan>();
		}
		for (Map.Entry<String, Clan> entries : clans.entrySet()) {
			entries.getValue().setTransient();
		}
		System.out.println("Loaded " + clans.size() + " clans.");
	}

	public Clan getClan(String s) {
		return clans.get(Misc.formatPlayerNameForProtocol(s));
	}

	public static Map<String, Clan> getClans() {
		return clans;
	}

	public void createClan(Player p, String name) {
		if (name.equals("")) {
			return;
		}
		String user = Misc.formatPlayerNameForProtocol(p.getUsername()).toLowerCase();
		//child 32 = coinshare below
		if (!clans.containsKey(user)) {
			Clan clan = new Clan(user, p.hasDisplayName() ? p.getDisplayName() : null, name);
			clans.put(Misc.formatPlayerNameForProtocol(p.getUsername()), clan);
			clan.rankUser(user, 8);
			refresh(clan);
		} else {
			Clan clan = clans.get(user);
			clan.setName(name);
			refresh(clan);
		}
	}

	public void joinClan(final Player p, final String user) {
		if (!p.hasReceivedStarter())
			return;
		p.sendMessage("Attempting to join channel...");
		Clan clan = null;
		for (Clan clanDP : clans.values()) { //clanDP, the DP is for DisplayName.
			if (clanDP.getOwnerDisplayName() != null && clanDP.getOwnerDisplayName().equals(user)) //not toLowerCase, as it's for displayNames.
				clan = clanDP;
		}
		if (clan == null)
			clan = clans.get(Misc.formatPlayerNameForProtocol(user.toLowerCase()));
		if (clan == null) {
			World.getWorld().submit(new Tick(1) {
				@Override
				public void execute() {
					p.sendMessage("The channel you tried to join does not exist.");
					stop();
				}
			});
			return;
		}
		final Clan clanFound = clan;
		World.getWorld().submit(new Tick(1) {
			@Override
			public void execute() {
				stop();
				if (clanFound.getMembers().size() >= 99) {
					p.sendMessage("This clan chat is currently full.");
					return;
				}
				if (clanFound.canJoin(p)) {
					p.getSettings().setCurrentClan(clanFound);
					clanFound.addMember(p);
					refresh(clanFound);
					ActionSender.sendConfig(p, 1083, clanFound.isLootsharing() ? 1 : 0);
					p.sendMessage("Now talking in the clan channel " + clanFound.getName());
					p.sendMessage("To talk, start each line of chat with the / symbol.");
				}
			}
		});
	}

	public void destroy(Player player, String username) {
		Clan c = clans.get(Misc.formatPlayerNameForProtocol(username));
		if (c != null) {
			for (Player p : c.getMembers()) {
				if (p == null) {
					continue;
				}
				ClanPacket.sendClanList(p, null);
			}
		}
		clans.remove(username);
	}

	public static void refresh(Clan clan) {
		for (Player p : clan.getMembers()) {
			if (p == null) {
				continue;
			}
			ClanPacket.sendClanList(p, clan);
		}
	}

	public void leaveClan(Player player, boolean logout) {
		Clan c = player.getSettings().getCurrentClan();
		if (c != null) {
			c.removeMember(player);
			refresh(c);
			if (!logout) {
				ClanPacket.sendClanList(player, null);
				ActionSender.sendConfig(player, 1083, 0);
				player.getSettings().setCurrentClan(null);
			}
		}
	}

	public void rankMember(Player player, String username, int rank) {
		//System.out.println("Setting player " + user + "'s rank to " + rank + ".");
		Clan c = clans.get(Misc.formatPlayerNameForProtocol(player.getUsername()));
		if (c == null) {
			return;
		}
		c.rankUser(username, rank);
		Player friend = World.getWorld().getPlayerInServer(username);
		try {
			ActionSender.sendFriend(player, username, friend == null ? username : friend.getDisplayName(), friend != null ? 0 : 1, friend != null, false, friend != null && friend.getConnection().isInLobby());
			refresh(c);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	public String getClanName(String user) {
		Clan c = clans.get(Misc.formatPlayerNameForProtocol(user));
		if (c == null) {
			return "Chat disabled";
		}
		return c.getName();
	}

	public void sendClanMessage(Player player, String text) {
		Clan c = player.getSettings().getCurrentClan();
		if (c == null) {
			return;
		}
		if (c.canTalk(player)) {
			for (Player pl : c.getMembers()) {
				if (pl.getIndex() == player.getIndex()) {
					continue;
				}
				ClanMessage.sendClanChatMessage(player, pl, c.getName(), c.getOwner(), text);
			}
			ClanMessage.sendClanChatMessage(player, null, c.getName(), c.getOwner(), text);
			Logger.writeChatLog(player, text, 2, null);
		} else {
			player.sendMessage("Your rank is not high enough to talk in this chat!");
		}
	}

	public void toggleLootshare(Player player) {
		Clan c = clans.get(Misc.formatPlayerNameForProtocol(player.getUsername()));
		if (c != null) {
			c.toggleLootshare();
		} else {
			player.sendMessage("You don't have a clan to active lootshare with.");
		}
	}

	public void toggleCoinshare(Player player) {
		Clan c = clans.get(Misc.formatPlayerNameForProtocol(player.getUsername()));
		if (c != null) {
			c.toggleCoinsharing();
		} else {
			player.sendMessage("You don't have a clan to activate coinshare with.");
		}

	}

	public void banMember(Player player, String playerName) {
		Clan c = clans.get(Misc.formatPlayerNameForProtocol(player.getUsername()));
		if(c != null){
			if (c.canBan(player)) {
				c.banMember(playerName);
				Player p = World.getWorld().getPlayerInServer(playerName);
				leaveClan(p, false);
				p.sendMessage("You have been kicked from this clan channel.");
				sendClanMessage(player, "[Attempting to kick/ban " + p.getDisplayName() + " from the clan chat channel]");
			}
		}
	}

	public void handleOption(Player p, int buttonOption, int menuIndex) {
		Clan c = clans.get(Misc.formatPlayerNameForProtocol(p.getUsername()));
		if (c != null) {
			c.handleOption(buttonOption, menuIndex);
		} else {
			p.sendMessage("You don't have a clan to activate coinshare with.");
		}

	}
}
