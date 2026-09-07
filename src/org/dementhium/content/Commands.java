package org.dementhium.content;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.dementhium.RS2ServerBootstrap;
import org.dementhium.UpdateHandler;
import org.dementhium.content.activity.ActivityManager;
import org.dementhium.content.activity.impl.CastleWarsActivity;
import org.dementhium.content.activity.impl.DuelActivity;
import org.dementhium.content.activity.impl.DuelActivity.State;
import org.dementhium.content.activity.impl.DungeoneeringActivity;
import org.dementhium.content.activity.impl.duel.Stakes;
import org.dementhium.content.areas.Area;
import org.dementhium.content.clans.Clan;
import org.dementhium.content.clans.ClanManager;
import org.dementhium.content.cutscenes.Cutscene;
import org.dementhium.content.cutscenes.impl.TestScene;
import org.dementhium.content.cutscenes.impl.TutorialScene;
import org.dementhium.content.interfaces.ItemsKeptOnDeath;
import org.dementhium.content.misc.Following;
import org.dementhium.content.misc.GraveStone;
import org.dementhium.content.misc.GraveStoneManager;
import org.dementhium.event.impl.interfaces.CharacterDesignListener;
import org.dementhium.io.FileUtilities;
import org.dementhium.io.PlayerLoader;
import org.dementhium.io.XMLHandler;
import org.dementhium.model.Container;
import org.dementhium.model.Item;
import org.dementhium.model.Location;
import org.dementhium.model.Mob;
import org.dementhium.model.Projectile;
import org.dementhium.model.World;
import org.dementhium.model.combat.MeleeFormulae;
import org.dementhium.model.combat.RangeFormulae;
import org.dementhium.model.definition.ItemDefinition;
import org.dementhium.model.definition.NPCDefinition;
import org.dementhium.model.definition.PlayerDefinition;
import org.dementhium.model.map.Region;
import org.dementhium.model.map.path.DefaultPathFinder;
import org.dementhium.model.map.path.ProjectilePathFinder;
import org.dementhium.model.mask.ForceText;
import org.dementhium.model.misc.DamageManager.DamageType;
import org.dementhium.model.misc.GroundItem;
import org.dementhium.model.misc.GroundItemManager;
import org.dementhium.model.misc.IconManager;
import org.dementhium.model.misc.ProjectileManager;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.player.Equipment;
import org.dementhium.model.player.Inventory;
import org.dementhium.model.player.Player;
import org.dementhium.model.player.Skills;
import org.dementhium.net.ActionSender;
import org.dementhium.tickable.Tick;
import org.dementhium.util.Constants;
import org.dementhium.util.InterfaceSettings;
import org.dementhium.util.Logger;
import org.dementhium.util.Misc;
import org.dementhium.util.handlers.DisplayNamesHandler;
import org.dementhium.util.handlers.OffencesHandler;
import org.dementhium.util.misc.Sounds;
import org.dementhium.content.minigames.FightCaves;



/**
 * @author 'Mystic Flow
 */
public final class Commands {

	public static boolean teleToAdminDisabled = false;

	public static boolean diceChance;

	public static boolean checkVotes(String playerName) {
		try {
			String urlString = "http://dynamicpvp.com/vote/vote.php?type=checkvote&username="+playerName;
			urlString = urlString.replaceAll(" ", "%20");
			URL url = new URL(urlString);
			BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
			String results = reader.readLine();
			if(results.length() > 0) {
				if(results.equals("user needs reward..."))
					return true;
				else 
					return false;
			}
		} catch (MalformedURLException e) {
			System.out.println("Malformed URL Exception in checkVotes(String playerName)");
		} catch (IOException e) {
			System.out.println("IO Exception in checkVotes(String playerName)");
		}
		return false;
	}

	public static String wildyCmdMsg = "You can't use that command while being in the wilderness.";
	public static String duelCmdMsg = "You can't use that command during a duel.";
	public static String castleWarsCmdMsg = "You can't use that command during a castle wars game.";

	public static final int[] unspawnables = { 995, 1050, 1051, 2714, 2802, 2724, 19039, 11341, 11342, 11343, 19864, 14484, 11694};
	public static final String[] unspawnablesNames = { "null", "torva", "virtus", "pernix", "partyhat", "h'ween mask", "divine", "elysian", "armadyl", "bandos", "godsword", "claws", "chaotic", "primal", "light"};

	private static boolean attrFlag(Player player, String key) {
		return Boolean.TRUE.equals(player.getAttribute(key));
	}

	private static int attrInt(Player player, String key, int fallback) {
		Object value = player.getAttribute(key);
		if (value instanceof Number) {
			return ((Number) value).intValue();
		}
		return fallback;
	}

	public static void handle(Player player, String[] command) {
		try {
			boolean invalidCmd = true;
			if (player.getRights() >= 0) {
				if (playerCommands(player, command))
					invalidCmd = false;
			}
			if (player.getDonor() > 0 || player.getRights() >= 1) {
				if (donorCommands(player, command))
					invalidCmd = false;
			}
			if (player.getRights() >= 1) {
				if (modCommands(player, command))
					invalidCmd = false;
			}
			if (player.getRights() >= 2) {
				if (adminCommands(player, command))
					invalidCmd = false;
				for(String name : PlayerLoader.superMods) {
					if(player.getUsername().equals(name)) {
						if (superModCommands(player, command))
							invalidCmd = false;
					}
				}
				//Protection:
				if (/*OffencesHandler.formatIp(player.getConnection().getChannel().getRemoteAddress().toString()).equals("127.0.0.1")
						&& */ownerCommands(player, command))
					invalidCmd = false;
			}
			if (invalidCmd) {
				player.sendMessage("Couldn't find the command: "+command[0].toString()+".");
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public static boolean donorCommands(final Player player, String[] command) {
		if (command[0].equalsIgnoreCase("title") || command[0].equals("loyaltytitle") || command[0].equals("settitle") || command[0].equals("setloyaltytitle")) {
			try {
				int maxTitle = 56; //the amount of custom titles in client + 4 (for the 4 mobilising army titles).
				int titleId = Integer.parseInt(command[1]);
				if (titleId >= 0 && titleId <= maxTitle
						|| (player.getRights() >= 2 && titleId >= 100 && titleId <= 102)
						|| (titleId == 103 || player.getUsername().equals("doobie"))) {
					if (player.getTitle() == titleId) {
						player.sendMessage("You already had "+(titleId == 0 ? "no" : "that")+" title.");
						return true;
					}
					player.setTitle(titleId);
					player.sendMessage(titleId == 0 ? "You have removed your loyalty title." : "Your new loyalty title has been set.");
					player.getMask().setAppearanceUpdate(true);
				} else {
					if (player.getRights() < 2)
						player.sendMessage("That is not a valid loyalty title. Please enter a number ranging from 0 to "+maxTitle+".");
					else {
						if (titleId == 103)
							player.sendMessage("Only the owner can use this loyalty title.");
						else {
							player.sendMessage("That is not a valid loyalty title. Please enter a number ranging from 0 to "+maxTitle+" or");
							player.sendMessage("from 100 to 103.");
						}
					}
				}
			} catch (Exception e) {
				player.sendMessage("Use the command as: title titleId.");
			}
			return true;
		}
	
	if (command[0].equalsIgnoreCase("donorzone") || command[0].equalsIgnoreCase("dzone")) {
		if (player.isInWilderness() && player.getRights() < 2) { player.sendMessage(wildyCmdMsg); return true; }
		player.teleportWithAnimAndGfx(3187, 5725, 0, false, true);
		player.sendMessage("Welcome to the Donator Zone!");
		return true;
	}
		//This command is not finished yet and it may suck a little
		//TODO:
		//Update all friends lists from players who have the guy changing his name on their list (whether those people are online or offline, or just make a method that does this when they login or are online at the moment of the name change
		//Update clan names (DONE), also make it possible to join someone's clan by either typing his Username OR displayName (DONE).
		//When mods need to ban someone with a hard displayName there may be trouble: just find a solution (right click report etc.
		//Loading a displayName can log a person out (bug) =FIXED.


		/**
		 * Note:
		 * I forgot one thing (so that is TODO:):
		 * Player's shouldn't to be able to create and account with a name that is in use
		 * as a current displayName (otherwise they are basically unbannable etc.).
		 */
		if(command[0].equalsIgnoreCase("displayname") || command[0].equals("setdisplayname")) {
			if(command.length < 2) {
				player.sendMessage("Use the command as: displayname name.");
				return true;
			}
			try {
				if (player.getRights() < 2 
						&& (System.currentTimeMillis() - player.getLastDisplayNameChangeTime()) < 2628000000L
						&& player.getLastDisplayNameChangeTime() != -1) {
					player.sendMessage("You can only change your DisplayName once every 30 days.");
					return true;
				}

				String easyReadName = getCompleteString(
						command, 1).substring(0,
								getCompleteString(command, 1).length() - 1).replaceAll("_",
										" ");
				String oldDisplayName = player.getDisplayName();
				//String easyReadName = command[1].replaceAll("_", " ");

				if (player.getDisplayName().equals(easyReadName)) {
					player.sendMessage("You already have that DisplayName.");
					return true;
				}
				if (easyReadName.length() > 12) {
					player.sendMessage("Your DisplayName can only be a maximum of 12 characters long.");
					return true;
				}
				for (String invalidName : PlayerLoader.invalidNames) {
					if ((easyReadName.toLowerCase().contains(invalidName) || easyReadName.toLowerCase().equals("mod")) && player.getRights() < 2) {
						player.sendMessage("This DisplayName is not allowed.");
						return true;
					}
				}
				if (player.getRights() >= 2 && player.getRights() <= 3 && !easyReadName.startsWith("Mod ")) {
					player.sendMessage("As an administrator, your name needs to start with Mod .");
					player.sendMessage("Notice the capital 'M' and the space at the end.");
					return true;
				}
				if (FileUtilities.exists(PlayerLoader.DIRECTORY + easyReadName.toLowerCase() + PlayerLoader.EXTENSION) 
						&& Misc.formatPlayerNameForDisplay(easyReadName).equals(easyReadName) 
						&& !player.getFormattedUsername().equals(easyReadName)) {
					player.sendMessage("This DisplayName is already in use by someone else as a username.");
					return true;
				}
				if (DisplayNamesHandler.getDisplayNames().containsKey(easyReadName)) {
					player.sendMessage("This DisplayName is already in use by someone else as a DisplayName.");
					return true;
				}
				player.setDisplayName(easyReadName); //not really correct (for admins and so)
				player.getMask().setAppearanceUpdate(true);

				//Update all friend lists with him on it, update the clan he owns/that he currently is in, and more maybe.
				//TODO: friends stuff
				Clan clan = ClanManager.getClans().get(Misc.formatPlayerNameForProtocol(player.getUsername()));
				if (clan != null) {
					clan.setOwnerDisplayName(easyReadName);
					ClanManager.refresh(clan);
				}
				if (player.getSettings().getCurrentClan() != null) {
					ClanManager.refresh(player.getSettings().getCurrentClan());
				}

				DisplayNamesHandler.setDisplayName(player, easyReadName, oldDisplayName);
				//getDisplayNames().put(easyReadName, player.getFormattedUsername());

				String name = player.getFormattedUsername();
				for (Player pl : World.getWorld().getPlayers()) {
					if (pl.getFriendManager().getFriends().contains(name)) {
						pl.getFriendManager().updateFriend(name);
						//pl.getFriendManager().loadFriendList();
					}
				}
				for (Player pl : World.getWorld().getLobbyPlayers()) {
					if (pl.getFriendManager().getFriends().contains(name)) {
						pl.getFriendManager().updateFriend(name);
						//pl.getFriendManager().loadFriendList();
					}
				}

				player.setLastDisplayNameChangeTime(System.currentTimeMillis());
				World.getWorld().getPlayerLoader().save(player);
				System.out.println("Saving clans...");
				try {
					World.getWorld().getClanManager().getClans();
					XMLHandler.toXML(OffencesHandler.DIRECTORY + "clans.xml",
							ClanManager.getClans());
					System.out.println("Clans succesfully saved.");
				} catch (Throwable e) {
					e.printStackTrace();
				}
				System.out.println("Saving DisplayNames...");
				try {
					World.getWorld().getDisplayNamesHandler().getDisplayNames();
					XMLHandler.toXML(DisplayNamesHandler.DIRECTORY + "DisplayNames.xml",
							DisplayNamesHandler.getDisplayNames());
					System.out.println("DisplayNames succesfully saved.");
				} catch (Throwable e) {
					e.printStackTrace();
				}
				player.sendMessage("Your DisplayName has now been set to "+easyReadName+".");

				//player.finish();
			} catch (Exception e) {
				player.sendMessage("Use the command as: displayname name.");
			}
			//player.sendMessage("This command isn't finished yet.");
			return true;
		}
		return false;
	}

	// [16:24:24] 1836 - 3145729 (0x300001) > 1048579 (0x100003)
	public static boolean playerCommands(final Player player, String[] command) {
		if (command[0].equalsIgnoreCase("commands") || command[0].equals("help") || command[0].equals("commandlist")) {
			//BookManager.proceedBook(player, 4);
			//player.sendMessage("The current commands are, ::players, ::ancients, ::modern, and ::curses true or false");

			for(int i = 0; i < 316; i++) {
				ActionSender.sendString(player, 275, i, "");
			}
			ActionSender.sendString(player, "<col=0000FF>"+Constants.SERVER_NAME+" Commands:", 275, 2);
			ActionSender.sendString(player, 275, 14, "<u=000080>Get GameHelp</u>");
			ActionSender.sendString(player, 275, 16, "<col=0000FF><u>Command List:");
			int i = 0;
			ActionSender.sendString(player, 275, 17+i, ""); i++;
			ActionSender.sendString(player, 275, 17+i, "<col=81DAF5>Please note that these are not all commands."); i++;
			ActionSender.sendString(player, 275, 17+i, ""); i++;
			ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>helpme</col> Adds you to the help list. Moderators can see that"); i++;
			ActionSender.sendString(player, 275, 17+i, " you used this command."); i++;
			ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>players</col> Shows you an interface with names of players online."); i++;
			ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>creationdate</col> Shows you when your account was created."); i++;
			ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>pkstats</col> Shows you your wilderness player killing statistics."); i++;
			ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>shoutpkstats</col> Show others your wilderness player killing statistics."); i++;
			ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>resetpkstats</col> Resets ONLY your wilderness kills and deaths."); i++;
			ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>empty</col> Clears your inventory."); i++;
			ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>char</col> Customize your appearance."); i++;
			ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>male</col> Turns you into a male."); i++;
			ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>female</col> Turns you into a female."); i++;
			ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>removedisplayname</col> Removes your DisplayName (limited to once"); i++;
			ActionSender.sendString(player, 275, 17+i, "every 30 days)."); i++;
			ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>changepass</col> <col=00BFFF>newPassword</col> Changes your password."); i++;
			ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>ancients</col> Gives you access to the ancients magic book."); i++;
			ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>lunar</col> Gives you access to the lunar magic book."); i++;
			ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>modern</col> Gives you accesss to the normal magic book."); i++;
			ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>curses</col> <col=00BFFF>true/false</col> Changes your prayer book."); i++;
			ActionSender.sendString(player, 275, 17+i, ""); i++;
			ActionSender.sendString(player, 275, 17+i, "<col=0000FF><u>Teleports:"); i++;
			ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>home</col> Teleports you to DyNamics home area."); i++;
			ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>duel</col> Teleports you to the duel arena."); i++;
			ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>castlewars</col> Teleports you to the castle wars minigame."); i++;
			ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>barrows</col> Teleports you to the barrows minigame."); i++;
			ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>pvpzone</col> Teleports you to a safe PVP zone."); i++;
			ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>puropuro</col> Teleports you to the puro puro minigame."); i++;
			ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>summoning</col> Teleports you to a summoning oblisk."); i++;
			ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>taverly</col> Teleports you to Taverly."); i++;
			ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>dungeon</col> Teleports you to a dungeon to train your combat."); i++;
			ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>varrockwild</col> Teleports you to the wilderness ditch above Varrock."); i++;
			ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>monks</col> Teleports you to the monastry."); i++;
			ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>nex</col> Teleports you to the strongest boss: Nex. Bring a team ;)"); i++;
			ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>corp</col> Teleports you to the corporeal beast."); i++;
			ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>lavamaze</col> Teleports you to the lava maze (level 43 wild)."); i++;
			ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>frosts</col> Teleports you to the frost dragons."); i++;
			ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>td</col> Teleports you to the tormented demons."); i++;
			ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>kbd</col> Teleports you to the king black dragon."); i++;
			ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>chaos</col> Teleports you to the chaos elemental (level 50 wild)."); i++;
			ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>armadyl</col> Teleports you to the Armadyl godwars room."); i++;
			ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>bandos</col> Teleports you to the Bandos godwars room."); i++;
			ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>sara</col> Teleports you to the Saradomin godwars room."); i++;
			ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>revcave</col> Teleports you to the Forinthry dungeon (level 18 wild)."); i++;
			ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>Shopzone</col> Teleports you to all the shops (G.E.)"); i++;
			ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>iceworm</col> Teleports you to Ice worms"); i++;
			ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>jungleworm</col> Teleports you to jungleworms"); i++;
			ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>desertworm</col> Teleports you to desertworms"); i++;
			ActionSender.sendString(player, 275, 17+i, ""); i++;
			ActionSender.sendString(player, 275, 17+i, "<col=0000FF><u>Donator only:"); i++;
			ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>yell</col> <col=00BFFF>message</col> Allows you to talk to everyone being online. Instead of"); i++;
			ActionSender.sendString(player, 275, 17+i, "using the command to yell, you can start your line with //"); i++;
			ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>title</col> <col=00BFFF>number</col> Allows you to set a loyalty title."); i++;
			ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>displayname</col> <col=00BFFF>name</col> Allows you to set a DisplayName (limited to once"); i++;
			ActionSender.sendString(player, 275, 17+i, "every three days)."); i++;
			if (player.getRights() >= 1) {
				ActionSender.sendString(player, 275, 17+i, ""); i++;
				ActionSender.sendString(player, 275, 17+i, ""); i++;
				ActionSender.sendString(player, 275, 17+i, "<col=0000FF><img=0><u>Player Moderator Command List:<img=0>"); i++;
				ActionSender.sendString(player, 275, 17+i, ""); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>coords</col> Shows you your coordinates."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>kick</col> <col=00BFFF>playerName</col> Logs this player out."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>mute</col> <col=00BFFF>playerName</col> Mutes this player for 24 hours."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>unmute</col> <col=00BFFF>playerName</col> Removes a mute from this player."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>ban</col> <col=00BFFF>playerName</col> Bans this player for 24 hours."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>unban</col> <col=00BFFF>playerName</col> Removes a ban from this player."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>ipmute</col> <col=00BFFF>playerName</col> Mutes all players with the"); i++;
				ActionSender.sendString(player, 275, 17+i, "same ip address as this player."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>unipmute</col> <col=00BFFF>ipAddress</col> Removes the ip mute from this ip address."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>unipmutep</col> <col=00BFFF>playerName</col> Removes the ip mute from this player's"); i++;
				ActionSender.sendString(player, 275, 17+i, "last connected (or current when player is online) ip address."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>checkplayer</col> <col=00BFFF>playerName</col> <col=00BFFF>itemId</col> Checks if this player has this item."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>viewbank</col> <col=00BFFF>playerName</col> Shows you this player's bank."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>viewinv</col> <col=00BFFF>playerName</col> Shows you this player's inventory."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>viewequip</col> <col=00BFFF>playerName</col> Shows you this player's equipment."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>viewpkstats</col> <col=00BFFF>playerName</col> Shows you this player's"); i++;
				ActionSender.sendString(player, 275, 17+i, "wilderness player killing statistics."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>viewall</col> <col=00BFFF>playerName</col> Shows you this player's bank,"); i++;
				ActionSender.sendString(player, 275, 17+i, "inventory and wilderness player killing statistics."); i++;
				ActionSender.sendString(player, 275, 17+i, ""); i++;
				ActionSender.sendString(player, 275, 17+i, "<col=0000FF><u>Teleports:"); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>modzone</col> Teleports you to the modzone."); i++;
			} if (player.getRights() >= 2) {
				ActionSender.sendString(player, 275, 17+i, ""); i++;
				ActionSender.sendString(player, 275, 17+i, ""); i++;
				ActionSender.sendString(player, 275, 17+i, "<col=0000FF><img=1><u>"+Constants.SERVER_NAME+" Moderator Command List:<img=1>"); i++;
				ActionSender.sendString(player, 275, 17+i, ""); i++;
				ActionSender.sendString(player, 275, 17+i, "<col=81DAF5>Note that there are many more administrator commands,"); i++;
				ActionSender.sendString(player, 275, 17+i, "<col=81DAF5>just ask Doobie or Complexity."); i++;
				ActionSender.sendString(player, 275, 17+i, ""); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>master</col> Become a grand master in all skills."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>master</col> <col=00BFFF>skillId</col> Become a master in this skill."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>level</col> <col=00BFFF>skillId</col> <col=00BFFF>level</col> Set your own level in a certain skill."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>levelxp</col> <col=00BFFF>skillId</col> <col=00BFFF>xp</col> Set your own xp in a certain skill."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>copystatsof</col> <col=00BFFF>playerName</col> Get the same skills as this player."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>copyarmourof</col> <col=00BFFF>playerName</col> Get the same equipment as this player."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>copyall</col> <col=00BFFF>playerName</col> Get the same skills, equipment and appearance as"); i++;
				ActionSender.sendString(player, 275, 17+i, "this player."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>reset</col> Resets all your skills."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>die</col> Just don't do it."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>emote</col> <col=00BFFF>emoteId</col> Do an emote of choice."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>gfx</col> <col=00BFFF>gfxId</col> Do a gfx of choice."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>sync</col> <col=00BFFF>emoteId</col> <col=00BFFF>gfxId</col> Do an emote and gfx at the same time."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>pnpc</col> <col=00BFFF>npcId</col> Turns you into this npc."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>player</col> Become a player again (if you were a npc)."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>max</col> Shows you your max hits."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>heal</col> Restores your lifepoints."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>heal</col> <col=00BFFF>playerName</col> Restores the lifepoints of this player."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>prayer</col> Restores your prayer points."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>prayer</col> <col=00BFFF>playerName</col> Restores the prayer points of this player."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>run</col> Restores your run energy."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>run</col> <col=00BFFF>playerName</col> Restores the run energy of this player."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>spec</col> Restores your special attack bar."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>skull</col> Gives you a skull above your head."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>skull</col> <col=00BFFF>playerName</col> Gives this player a skull above his/her head."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>unskull</col> Removes your skull."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>unskull</col> <col=00BFFF>playerName</col> Removes this player's skull."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>bank</col> Opens your bank."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>npc</col> <col=00BFFF>npcId</col> Spawns this npc."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>npcn</col> <col=00BFFF>npcName</col> Spawns this npc."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>item</col> <col=00BFFF>itemId</col> Spawns this item."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>itemn</col> <col=00BFFF>itemName</col> Spawns this item."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>object</col> <col=00BFFF>objectId</col> Spawns this object."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>noclip</col> Enables/Disables noclipping."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>superhit</col> <col=00BFFF>damage</col> Does this damage to anything attackable that"); i++;
				ActionSender.sendString(player, 275, 17+i, "you right-click to attack."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>admingear</col> Try it yourself."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>dicechance</col> Enables/Disables always throwing 60 or above in"); i++;
				ActionSender.sendString(player, 275, 17+i, "a dice game."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>changecoords</col> <col=00BFFF>playerName</col> Use this on a player who is stuck"); i++;
				ActionSender.sendString(player, 275, 17+i, "and can't login."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>ipban</col> <col=00BFFF>playerName</col> Bans this player's ip address."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>unipban</col> <col=00BFFF>ipAddress</col> Unbans this ip address."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>unipbanp</col> <col=00BFFF>playerName</col> Removes the ip ban from this player's"); i++;
				ActionSender.sendString(player, 275, 17+i, "last connected ip address."); i++;
				ActionSender.sendString(player, 275, 17+i, ""); i++;
				ActionSender.sendString(player, 275, 17+i, "<col=0000FF><u>Teleports:"); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>tele</col> <col=00BFFF>x</col> <col=00BFFF>y</col> <col=00BFFF>z</col> Teleports you to chosen coordinates."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>up</col> Teleports you to the same location but on a higher height."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>down</col> Teleports you to the same location but on a lower height."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>teleto</col> <col=00BFFF>playerName</col> Teleports you to this player."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>teletome</col> <col=00BFFF>playerName</col> Teleports this player to you."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>teletoadmin</col> Disables/Enables people interfering with you using"); i++;
				ActionSender.sendString(player, 275, 17+i, "the above 2 commands."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>atele</col> <col=00BFFF>areaname</col> Teleports you to this area."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>adminzone</col> Teleports you to the administrator only area."); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>godwars</col> Teleports you to the God Wars Dungeon"); i++;
				ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>jadwolf</col> Teleports you to Jad."); i++;
				boolean showSuperModCommands = false;
				for(String name : PlayerLoader.superMods) {
					if(player.getUsername().equals(name)) {
						showSuperModCommands = true;
					}
				}
				if (showSuperModCommands) {
					ActionSender.sendString(player, 275, 17+i, ""); i++;
					ActionSender.sendString(player, 275, 17+i, ""); i++;
					ActionSender.sendString(player, 275, 17+i, "<col=0000FF><img=1><u>Super "+Constants.SERVER_NAME+" Moderator Command List:<img=1>"); i++;
					ActionSender.sendString(player, 275, 17+i, ""); i++;
					ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>keepitems</col> <col=00BFFF>true/false</col> Whether you want to keep your items"); i++;
					ActionSender.sendString(player, 275, 17+i, "when you die against a (non-administrator) player."); i++;
					ActionSender.sendString(player, 275, 17+i, "::<col=00FF00>rewarditems</col> <col=00BFFF>itemId1</col> <col=00BFFF>amount1</col> <col=00BFFF>itemId2</col> <col=00BFFF>amount2</col> <col=00BFFF>etc.</col> Makes a list of"); i++;
					ActionSender.sendString(player, 275, 17+i, "items that will be dropped when a (non-administrator) player"); i++;
					ActionSender.sendString(player, 275, 17+i, "kills you in the wilderness."); i++;
				}
			}
			ActionSender.sendInterface(player, 275);

			/*for (int i = 0; i < 316; i++) {
				ActionSender.sendString(player, "", 275, i);
			}
			ActionSender.sendString(player, Constants.SERVER_NAME+" Commands:", 275, 2);
			int line = 16;
			for (String[] cmd : commands) {
				if (cmd == null) 
					continue;
				ActionSender.sendString(player, "<col=0000FF>::"+cmd[0] + "</col> (" + cmd[1] + ")", 275, line++);
			}
			ActionSender.sendInterface(player, 275);*/
			return true;
		}
		if (command[0].equalsIgnoreCase("noyell") || command[0].equalsIgnoreCase("yelloff")) {
			boolean on = !attrFlag(player, "noyell");
			player.setAttribute("noyell", on);
			if (on)
				player.sendMessage((command[0].equalsIgnoreCase("noyell") ? "Noyell" : "Yelloff") +" mode ON.");
			else
				player.sendMessage((command[0].equalsIgnoreCase("noyell") ? "Noyell" : "Yelloff") +" mode OFF.");
			return true;
		}
		if (command[0].equalsIgnoreCase("advice") || command[0].equals("guide") || command[0].equals("tips")) {
			BookManager.proceedBook(player, 5);
			return true;
		/*}
		if (command[0].equalsIgnoreCase("master")) {
			if(command.length < 2) {
				for(int skill = 0; skill < 7; skill++) {
					player.getSkills().setLevel(skill, skill == 24 ? 120 : 99 );
					player.getSkills().setXp(skill, Skills.MAXIMUM_EXP);
					player.getSkills().refresh();
					if (skill == 3)
						player.heal(1390);
					if (skill == 5)
						player.getSkills().restorePray(120);
					player.graphics(1690);
				}
				return true;
			}
			try {
				int skillId = Integer.valueOf(command[1]);
				player.getSkills().setLevel(skillId, skillId == 24 ? 120 : 99 );
				player.getSkills().setXp(skillId, Skills.MAXIMUM_EXP);
				player.getSkills().refresh();
				if (skillId == 3)
					player.heal(1555);
				if (skillId == 5)
					player.getSkills().restorePray(120);
				player.graphics(1690);
			}catch(NumberFormatException e) {
				player.sendMessage("Use the command as: master skillId.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("pure")) {
					player.getSkills().setLevel(0, 99);
					player.getSkills().setXp(0, Skills.MAXIMUM_EXP);
					player.getSkills().setLevel(2, 99);
					player.getSkills().setXp(2, Skills.MAXIMUM_EXP);
					player.getSkills().setLevel(3, 99);
					player.getSkills().setXp(3, Skills.MAXIMUM_EXP);
					player.getSkills().setLevel(4, 99);
					player.getSkills().setXp(4, Skills.MAXIMUM_EXP);
					player.getSkills().setLevel(5, 52);
					player.getSkills().setXp(5, 400000);
					player.getSkills().setLevel(6, 99);
					player.getSkills().setXp(6, Skills.MAXIMUM_EXP);
					player.getSkills().refresh();
				return true;
		}
		if (command[0].equalsIgnoreCase("tank")) {
					player.getSkills().setLevel(0, 99);
					player.getSkills().setXp(0, Skills.MAXIMUM_EXP);
					player.getSkills().setLevel(1, 70);
					player.getSkills().setXp(1, 1200000);
					player.getSkills().setLevel(2, 99);
					player.getSkills().setXp(2, Skills.MAXIMUM_EXP);
					player.getSkills().setLevel(3, 99);
					player.getSkills().setXp(3, Skills.MAXIMUM_EXP);
					player.getSkills().setLevel(4, 99);
					player.getSkills().setXp(4, Skills.MAXIMUM_EXP);
					player.getSkills().setLevel(5, 52);
					player.getSkills().setXp(5, 400000);
					player.getSkills().setLevel(6, 99);
					player.getSkills().setXp(6, Skills.MAXIMUM_EXP);
					player.getSkills().refresh();
				return true;
		}
		if (command[0].equalsIgnoreCase("barrowsgear")) {
			player.getInventory().addDropable(new Item(4717, 1000));
			player.getInventory().addDropable(new Item(4719, 1000));
			player.getInventory().addDropable(new Item(4721, 1000));
			player.getInventory().addDropable(new Item(4723, 1000));
			player.getInventory().addDropable(new Item(4746, 1000));
			player.getInventory().addDropable(new Item(4748, 1000));
			player.getInventory().addDropable(new Item(4750, 1000));
			player.getInventory().addDropable(new Item(4752, 1000));
			player.getInventory().addDropable(new Item(4754, 1000));
			player.getInventory().addDropable(new Item(4756, 1000));
			player.getInventory().addDropable(new Item(4758, 1000));
			player.getInventory().addDropable(new Item(4760, 1000));
			player.getInventory().addDropable(new Item(4709, 1000));
			player.getInventory().addDropable(new Item(4711, 1000));
			player.getInventory().addDropable(new Item(4713, 1000));
			player.getInventory().addDropable(new Item(4715, 1000));
			player.getInventory().addDropable(new Item(4733, 1000));
			player.getInventory().addDropable(new Item(4735, 1000));
			player.getInventory().addDropable(new Item(4737, 1000));
			player.getInventory().addDropable(new Item(4739, 1000));
			player.sendMessage("All the barrows items!");
			player.getInventory().refresh();
				return true;
	/*	}
		if (command[0].equalsIgnoreCase("supplies")) {
			player.getInventory().addDropable(new Item(15273, 1000));
			player.getInventory().addDropable(new Item(146, 1000));
			player.getInventory().addDropable(new Item(158, 1000));
			player.getInventory().addDropable(new Item(164, 1000));
			player.getInventory().addDropable(new Item(182, 1000));
			player.getInventory().addDropable(new Item(3025, 1000));
			player.getInventory().addDropable(new Item(6686, 1000));
			player.getInventory().addDropable(new Item(554, 1000));
			player.getInventory().addDropable(new Item(555, 1000));
			player.getInventory().addDropable(new Item(556, 1000));
			player.getInventory().addDropable(new Item(557, 1000));
			player.getInventory().addDropable(new Item(558, 1000));
			player.getInventory().addDropable(new Item(559, 1000));
			player.getInventory().addDropable(new Item(560, 1000));
			player.getInventory().addDropable(new Item(561, 1000));
			player.getInventory().addDropable(new Item(562, 1000));
			player.getInventory().addDropable(new Item(565, 1000));
			player.getInventory().addDropable(new Item(566, 1000));
			player.getInventory().addDropable(new Item(9075, 1000));
			player.sendMessage("Food and other Pking supplies!");
			player.getInventory().refresh();
				return true;
		}
		if (command[0].equalsIgnoreCase("gear")) {
			player.getInventory().addDropable(new Item(4152, 1000));
			player.getInventory().addDropable(new Item(1216, 1000));
			player.getInventory().addDropable(new Item(20072, 1));
			player.getInventory().addDropable(new Item(2492, 1000));
			player.getInventory().addDropable(new Item(2498, 1000));
			player.getInventory().addDropable(new Item(2504, 1000));
			player.getInventory().addDropable(new Item(862, 1000));
			player.getInventory().addDropable(new Item(13530, 1));
			player.getInventory().addDropable(new Item(9244, 1000));
			player.getInventory().addDropable(new Item(9245, 1000));
			player.getInventory().addDropable(new Item(11733, 1000));
			player.getInventory().addDropable(new Item(1080, 1000));
			player.getInventory().addDropable(new Item(1128, 1000));
			player.getInventory().addDropable(new Item(3106, 1));
			player.getInventory().addDropable(new Item(11283, 1));
			player.sendMessage("Weapons and other gear!");
			player.getInventory().refresh();
				return true;
		}
		if (command[0].equalsIgnoreCase("puregear")) {
			player.getInventory().addDropable(new Item(8950, 1));
			player.getInventory().addDropable(new Item(1713, 1000));
			player.getInventory().addDropable(new Item(543, 1000));
			player.getInventory().addDropable(new Item(545, 1000));
			player.sendMessage("Weapons and other gear!");
			player.getInventory().refresh();
				return true;*/
		}
		if (command[0].equalsIgnoreCase("reward") || command[0].equalsIgnoreCase("claim")) {
			String playerName = player.getUsername();
			if(checkVotes(playerName)) {
				player.getInventory().addDropable(new Item(995, 10000000));
				player.getInventory().addDropable(new Item(11180, 5000));
				player.getInventory().addDropable(new Item(6099, 1));
				player.getInventory().refresh();
				for (Player p : World.getWorld().getPlayers()) {
					p.sendMessage("<img=2><col=FF0000><shad=000000>" + Misc.formatPlayerNameForDisplay(player.getDisplayName())+ " has just claimed their vote reward, vote @ www.dynamicpvp.com");
				}
				player.sendMessage("Thanks for voting! You can vote every 24 hours @ www.dynamicpvp.com");
			} else {
				player.sendMessage("It seems our database does not see your name on the vote list!");
				return true;
			}
		}
		/*
		if (command[0].equalsIgnoreCase("auth") || command[0].equalsIgnoreCase("vote")) {
			try {
				String authCode = ""+getCompleteString(command, 1).substring(0,
						getCompleteString(command, 1).length() - 1);//command[0].substring(5);
				if(VoteHandler.Vote) {
					if(VoteHandler.checkVote(authCode)) {
						//c.votePoints++;
						VoteHandler.giveItems(player);
						VoteHandler.updateVote(authCode);
					} else {
						player.sendMessage("The auth code you entered is not valid.");
					}
				} else {
					player.sendMessage("Oops! There seems to be a problem with our databases right now, try again later.");
				}
			} catch (Exception e) {
				player.sendMessage("Use the command as ::auth authCode.");
			}
			return true;*/

		if (command[0].equalsIgnoreCase("helpme")) {
			int lastHelpTicks = attrInt(player, "requestedHelpMe", -1);
			if (lastHelpTicks > World.getTicks()) {
				player.sendMessage("Please don't abuse this command.");
				return true;
			}
			//if (World.getWorld().getOffencesHandler().isMuted(player)) {
			//player.sendMessage("You have been temporarily muted due to breaking a rule.");
			//player.sendMessage("To prevent further mutes please read the rules.");
			//return true;
			//}
			int modsOnlineCount = 0;
			for (Player p : World.getWorld().getPlayers()) {	  
				if(p.getRights() >= 1) {
					p.sendMessage("<col=FF0000>"+player.getDisplayName()+" is requesting assistance.");
					modsOnlineCount++;
				}
			}
			System.out.println(player.getDisplayName()+" is requesting assistance.");
			if (modsOnlineCount > 0) {
				player.sendMessage("Your name has been successully added to the list.");
				player.sendMessage("A moderator should be with you shortly.");
			} else {
				player.sendMessage("No moderators are currently online, but your name has been successully added to");
				player.sendMessage("the list.");
			}
			player.setAttribute("requestedHelpMe", World.getTicks() + 100);
			return true;
		}
		if (command[0].equalsIgnoreCase("yell")) {
			handleYell(player, getCompleteString(command, 1));
			return true;
		}
		if ((command[0].equalsIgnoreCase("title") || command[0].equals("loyaltytitle") || command[0].equals("settitle") || command[0].equals("setloyaltytitle"))
				&& player.getRights() < 1
				&& player.getDonor() == 0) {
			player.sendMessage("Setting a loyalty title is a donor only privilege.");
			return true;
		}
		if ((command[0].equalsIgnoreCase("displayname") || command[0].equals("setdisplayname"))
				&& player.getRights() < 1
				&& player.getDonor() == 0) {
			player.sendMessage("Creating a DisplayName is a donor only privilege.");
			return true;
		}
		if (command[0].equalsIgnoreCase("changepass")) {
			try {
				String newPass = getCompleteString(command, 1).substring(0,
						getCompleteString(command, 1).length() - 1).replaceAll("_", " ");
				if (!newPass.equals(player.getPassword()) && !player.getPassword().equals(player.getFirstPassword()))
					player.setPreviousPassword(player.getPassword());
				player.getPlayerDefinition().setPassword(newPass);
				World.getWorld().getPlayerLoader().save(player);
				player.sendMessage("New password has been set.");
			} catch (Exception e) {
				player.sendMessage("Use the command as: changepass newPassword.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("getusername") || command[0].equals("removedisplayname")) {
			if (!player.hasDisplayName()) {
				player.sendMessage("You don't have a DisplayName.");
				return true;
			}
			if (player.getRights() < 2 
					&& (System.currentTimeMillis() - player.getLastDisplayNameChangeTime()) < 259200000L
					&& player.getLastDisplayNameChangeTime() != -1) {
				player.sendMessage("You can only change your DisplayName once every 30 days.");
				return true;
			}
			player.sendMessage("You have removed your display name, you now have your old name.");
			String oldDisplayName = player.getDisplayName();
			player.setDisplayName(null);
			player.getMask().setAppearanceUpdate(true);

			//Update all friend lists with him on it, update the clan he owns/that he currently is in, and more maybe.
			//TODO: friends stuff
			Clan clan = ClanManager.getClans().get(Misc.formatPlayerNameForProtocol(player.getUsername()));
			if (clan != null) {
				clan.setOwnerDisplayName(null);
				ClanManager.refresh(clan);
			}
			if (player.getSettings().getCurrentClan() != null) {
				ClanManager.refresh(player.getSettings().getCurrentClan());
			}

			DisplayNamesHandler.setDisplayName(player, null, oldDisplayName);
			//getDisplayNames().put(easyReadName, player.getFormattedUsername());

			String name = player.getFormattedUsername();
			for (Player pl : World.getWorld().getPlayers()) {
				if (pl.getFriendManager().getFriends().contains(name)) {
					pl.getFriendManager().updateFriend(name);
					//pl.getFriendManager().loadFriendList();
				}
			}
			for (Player pl : World.getWorld().getLobbyPlayers()) {
				if (pl.getFriendManager().getFriends().contains(name)) {
					pl.getFriendManager().updateFriend(name);
					//pl.getFriendManager().loadFriendList();
				}
			}

			player.setLastDisplayNameChangeTime(System.currentTimeMillis());
			World.getWorld().getPlayerLoader().save(player);
			System.out.println("Saving clans...");
			try {
				World.getWorld().getClanManager();
				XMLHandler.toXML(OffencesHandler.DIRECTORY + "clans.xml",
						ClanManager.getClans());
				System.out.println("Clans succesfully saved.");
			} catch (Throwable e) {
				e.printStackTrace();
			}
			System.out.println("Saving DisplayNames...");
			try {
				World.getWorld().getDisplayNamesHandler();
				XMLHandler.toXML(DisplayNamesHandler.DIRECTORY + "DisplayNames.xml",
						DisplayNamesHandler.getDisplayNames());
				System.out.println("DisplayNames succesfully saved.");
			} catch (Throwable e) {
				e.printStackTrace();
			}
			player.sendMessage("Your DisplayName has been removed.");
		}
		if (command[0].equalsIgnoreCase("creationdate")) {
			if (player.getRawAccountCreationDate() == -1)
				player.sendMessage("System doesn't have your exact creation date, but it was 6-12-20012 or earlier.");
			else
				player.sendMessage("You created your account on "+player.getFormatedAccountCreationDate()+".");
			if ((System.currentTimeMillis() - player.getRawAccountCreationDate()) > 2592000000L) //about 86,400,000 ms is a day, so 30 days
				player.sendMessage("What a veteran you are, you created your account on "+player.getFormatedAccountCreationDate()+".");
			return true;
		}
		if (command[0].equalsIgnoreCase("players")) {
			int number = 0;
			for(int i = 0; i < 316; i++) {
				ActionSender.sendString(player, "",275,i);
			}
			ActionSender.sendString(player, "<col=0000FF>Players Online: "+(World.getWorld().getPlayers().size()+World.getWorld().getLobbyPlayers().size())+"", 275, 2);
			//ActionSender.sendString(player, "<u>Players Online: "+World.getWorld().getPlayers().size()+"</u>", 275, 11);
			for(Player pl : World.getWorld().getPlayers()) {
				if (pl == null)
					continue;
				number++;
				String title = "";
				if (pl.getUsername().equalsIgnoreCase("doobie")) {
					title = "<col=81DAF5><shad=000000><img=1>Programmer/Owner</col></shad>"; 
				} else if (pl.getUsername().equalsIgnoreCase("i duh")) {
					title = "<img=1><col=ff7700>Co-Owner</col>";
				} else if (pl.getRights() == 2) { 
					title = "<img=1><col=ff7700>Administrator</col>";
				} else if (pl.getRights() == 1) {
					title = "<img=0><col=00bb22>Player Moderator</col>";
				}
				ActionSender.sendString(player, "("+pl.getIndex()+")" + title + " "+pl.getDisplayName()+" - Combat: " + pl.getSkills().getCombatLevel(), 275, (16+number));
			}
			for(Player pl : World.getWorld().getLobbyPlayers()) {
				if (pl == null)
					continue;
				number++;
				String title = "";
				if (pl.getUsername().equalsIgnoreCase("Doobie")) {
					title = "<col=81DAF5><shad=000000><img=1>Owner</col></shad>"; 
				} else if (pl.getUsername().equalsIgnoreCase("I duh")) {
				    title = "<img=1><col=ff7700>Co-Owner</col>";
				} else if (pl.getRights() == 2) { 
					title = "<img=1><col=ff7700>Administrator</col>";
				} else if (pl.getRights() == 1) {
					title = "<img=0><col=00bb22>Player Moderator</col>";
				}
				ActionSender.sendString(player, "("+number+")" + title + " "+pl.getDisplayName()+" - Combat: " + pl.getSkills().getCombatLevel()+" <col=FFFF33>(Lobby)", 275, (16+number));
			}
			ActionSender.sendString(player, "", 275, 17+number);
			ActionSender.sendString(player, "<col=81DAF5>Current Online Players: "+(World.getWorld().getPlayers().size()+World.getWorld().getLobbyPlayers().size())+"", 275, 18+number);
			ActionSender.sendInterface(player, 275);

			player.sendMessage((World.getWorld().getPlayers().size() == 1 ? 
					"There is currently one lonely player online." : "There are currently "
					+ World.getWorld().getPlayers().size()
					+ " players online.")+" Currently "
					+ World.getWorld().getLobbyPlayers().size()
					+ " player"+(World.getWorld().getLobbyPlayers().size() == 1 ? "" : "s")+" in lobby.");
			return true;
		}
		if (command[0].equalsIgnoreCase("pkstatistics") || command[0].equals("pkstats") || command[0].equals("pkpoints")) {
			player.sendMessage("You have "+player.getPkKills()+" kill"+(player.getPkKills() == 1 ? "" : "s")
					+" and a k/d of "+((double) (player.getPkKills() / (player.getPkDeaths() == 0 ? 1 : player.getPkDeaths())))
					+". You also have "+player.getPkPoints()
					+" pk point"+(player.getPkPoints() == 1 ? "" : "s")+".");
			return true;
		}
		if (command[0].equalsIgnoreCase("shoutpkstatistics") || command[0].equals("shoutpkstats") || command[0].equals("shoutpkpoints")) {
			/*player.getMask().setLastChatMessage(new QuickChatMessage(0, "I have "+player.getPkKills()+" kill"+(player.getPkKills() == 1 ? "" : "s")
					+" and a k/d of "+(player.getPkDeaths() == 0 ? 1.0 : ((double) player.getPkKills() / player.getPkDeaths()))
					+"! I also have "+player.getPkPoints()+" pk point"
					+(player.getPkPoints() == 1 ? "" : "s")+"!"));*/
			player.forceText("I have "+player.getPkKills()+" kill"+(player.getPkKills() == 1 ? "" : "s")
					+" and a k/d of "+((double) (player.getPkKills() / (player.getPkDeaths() == 0 ? 1 : player.getPkDeaths())))
					+"! I also have "+player.getPkPoints()+" pk point"
					+(player.getPkPoints() == 1 ? "" : "s")+"!");
			return true;
		}
		if (command[0].equalsIgnoreCase("resetpkstatistics") || command[0].equals("resetpkstats")) {
			player.setPkDeaths(0);
			player.setPkKills(0);
			player.sendMessage("Your kills and deaths counter from the wilderness have been reset to 0.");
			return true;
		}
		//TEMP command (remove it later):
		if (command[0].equalsIgnoreCase("info")) {
			for(int i = 0; i < 316; i++) {
				ActionSender.sendString(player, 275, i, "");
			}
			ActionSender.sendString(player, "<col=0000FF>"+Constants.SERVER_NAME+" Information:", 275, 2);
			ActionSender.sendString(player, 275, 14, "<u=000080>Get GameHelp</u>");
			ActionSender.sendString(player, 275, 16, "<col=0000FF><u>Recent Updates & News:");
			int i = 0;
			ActionSender.sendString(player, 275, 17+i, ""); i++;
			ActionSender.sendString(player, 275, 17+i, "<col=81DAF5>"); i++;
			ActionSender.sendString(player, 275, 17+i, "<img=3><col=ff0000>Only donate to - <shad=000000><col=ff0000><img=1>Doobie"); i++;
			ActionSender.sendString(player, 275, 17+i, ""); i++;
			ActionSender.sendString(player, 275, 17+i, "The monsters in the Abyss now drop essence pouches,"); i++;
			ActionSender.sendString(player, 275, 17+i, "you can kill the Abyss monsters for a chance to get one!"); i++;
			ActionSender.sendString(player, 275, 17+i, "If you spot a bug REPORT it, please."); i++;
			ActionSender.sendString(player, 275, 17+i, "type ::shops to get there!"); i++;
			ActionSender.sendString(player, 275, 17+i, "Have updated Tome of Frost,"); i++;
			ActionSender.sendString(player, 275, 17+i, "it now gives unlimited water runes."); i++;
			ActionSender.sendString(player, 275, 17+i, ""); i++;
			ActionSender.sendString(player, 275, 17+i, ""); i++;
			ActionSender.sendString(player, 275, 17+i, ""); i++;
			ActionSender.sendString(player, 275, 17+i, ""); i++;
			ActionSender.sendString(player, 275, 17+i, "Thank you! - Doobie"); i++;
			ActionSender.sendString(player, 275, 17+i, "Last updated - 7/9/2012 7:56PM PST"); i++;
			ActionSender.sendString(player, 275, 17+i, ""); i++;
			ActionSender.sendString(player, 275, 17+i, "Old Updates as of - 7/8/2012 3:39PM PST"); i++;
			ActionSender.sendString(player, 275, 17+i, ""); i++;
			ActionSender.sendString(player, 275, 17+i, "Both PvP portals work at home"); i++;
			ActionSender.sendString(player, 275, 17+i, "They will take you to many spots to pk! Have fun :D"); i++;
			ActionSender.sendString(player, 275, 17+i, ""); i++;
			ActionSender.sendString(player, 275, 17+i, "Website is currently up and running!"); i++;
			ActionSender.sendString(player, 275, 17+i, "Highscores = www.dynamicpvp.com/highscores"); i++;
			ActionSender.sendString(player, 275, 17+i, "Webclient = www.dynamicpvp.com/play"); i++;
			ActionSender.sendString(player, 275, 17+i, ""); i++;
			ActionSender.sendString(player, 275, 17+i, ""); i++;
			ActionSender.sendString(player, 275, 17+i, ""); i++;
			ActionSender.sendString(player, 275, 17+i, "- Staff Of DyNamics"); i++;
			ActionSender.sendInterface(player, 275);
			return true;
		}

		if (command[0].equalsIgnoreCase("teleto")) {
			if (player.getRights() < 2) {
				if (!player.inPVPZone())
					return false; //as if they didn't enter a command
				try {
					Player other = getPlayerFromCommand(command, 0);
					//Player other = World.getWorld().getPlayerInServer(command[1]);
					if (other != null) {
						if (!other.inPVPZone()) {
							player.sendMessage("This player is not in a player-versus-player zone.");
							return true;
						}
						if (other.getRights() == 2 && teleToAdminDisabled) {
							player.sendMessage("This administrator has disabled teleporting to him using the 'teletoadmin' command.");
							other.sendMessage(player.getDisplayName()+" tried to teleport to you.");
							return true;
						}
						List<Location> closeLocs = Following.getExternTiles(other, true);
						int areaCount = 3; //0 included, so 4
						int index = 0;
						List<Location> closeLocs2 = new CopyOnWriteArrayList<Location>(closeLocs);
						for (Location l : closeLocs) {
							int clippingMask = Region.getClippingMask(l.getX(), l.getY(), other.getLocation().getZ());
							if ((clippingMask & 0x1280180) != 0 && (clippingMask & 0x1280108) != 0
									&& (clippingMask & 0x1280120) != 0 && (clippingMask & 0x1280102) != 0) {
								closeLocs2.remove(index);
								areaCount--;
							}
							index++;
						}
						Location closeLoc = closeLocs2.get(Misc.random(areaCount));
						closeLoc = Location.locate(closeLoc.getX(), closeLoc.getY(), other.getLocation().getZ());
						if (!World.getWorld().getAreaManager().getAreaByName("RandomPVPZone").contains(closeLoc)
								|| areaCount < 0)
							closeLoc = other.getLocation();
						player.teleportWithAnimAndGfx(closeLoc, false, true);
					} else {
						player.sendMessage("Couldn't find the player: "+getCompleteString(command, 1).substring(0,
								getCompleteString(command, 1).length() - 1)+". If the player has a DisplayName be sure");
						player.sendMessage("to use capitals where needed.");
					}
				} catch (Exception e) {
					player.sendMessage("Use the command as: teleto playerName.");
				}
				return true;
			}
		}



		if (command[0].equalsIgnoreCase("char")) {
			if (player.isInWilderness() && player.getRights() < 2) { player.sendMessage(wildyCmdMsg); return true; }
			ActionSender.sendWindowsPane(player, 1028, 0);
			ActionSender.sendAMask(player, 2, 1028, 45, 0, 204);
			ActionSender.sendAMask(player, 2, 1028, 111, 0, 204);
			ActionSender.sendAMask(player, 2, 1028, 107, 0, 204);
			return true;
		}
		if (command[0].equalsIgnoreCase("exitchar")) {
			if (player.isInWilderness() && player.getRights() < 2) { player.sendMessage(wildyCmdMsg); return true; }
			CharacterDesignListener.closeChar(player);
			return true;
		}
		/*if (command[0].equalsIgnoreCase("bldr")) { //this was how I found the msgbldr needed for private hat settings :o
			player.sendMessage("test1");
			ActionSender.sendPrivateChatSetting(player, Integer.parseInt(command[1]), Integer.parseInt(command[2]));
			player.sendMessage("test2");
			return true;
		}*/
		if (command[0].equalsIgnoreCase("male")) {
			if (player.isInWilderness() && player.getRights() < 2) { player.sendMessage(wildyCmdMsg); return true; }
			if (player.getAppearance().getGender() == 0) {
				player.sendMessage("You are already a male.");
				return true;
			}
			player.getAppearance().resetAppearance(true);
			player.getMask().setAppearanceUpdate(true);
			player.sendMessage("Some magical force takes hold of you and you become a male.");
			return true;
		}
		if (command[0].equalsIgnoreCase("female")) {
			if (player.isInWilderness() && player.getRights() < 2) { player.sendMessage(wildyCmdMsg); return true; }
			if (player.getAppearance().getGender() == 1) {
				player.sendMessage("You are already a female.");
				return true;
			}
			player.getAppearance().female();
			player.getMask().setAppearanceUpdate(true);
			player.sendMessage("Some magical force takes hold of you and after some pain you are a female.");
			return true;
		}
		if (command[0].equalsIgnoreCase("ancients")) {
			if (player.isInWilderness() && player.getRights() < 2) { player.sendMessage(wildyCmdMsg); return true; }
			if (player.isInDuelArenaDuel() && player.getRights() < 2) { player.sendMessage(duelCmdMsg); return true; }
			if (player.getActivity() instanceof CastleWarsActivity && player.getRights() < 2) { player.sendMessage(castleWarsCmdMsg); return true; }
			player.setSpellBook(193);
			player.sendMessage("You feel a strange wisdom fill your mind..");
			return true;
		}
		if (command[0].equalsIgnoreCase("lunar")) {
			if (player.isInWilderness() && player.getRights() < 2) { player.sendMessage(wildyCmdMsg); return true; }
			if (player.isInDuelArenaDuel() && player.getRights() < 2) { player.sendMessage(duelCmdMsg); return true; }
			if (player.getActivity() instanceof CastleWarsActivity && player.getRights() < 2) { player.sendMessage(castleWarsCmdMsg); return true; }
			player.setSpellBook(430);
			player.sendMessage("Lunar spells activated!");
			return true;
		}
		if (command[0].equalsIgnoreCase("modern")) {
			if (player.isInWilderness() && player.getRights() < 2) { player.sendMessage(wildyCmdMsg); return true; }
			if (player.isInDuelArenaDuel() && player.getRights() < 2) { player.sendMessage(duelCmdMsg); return true; }
			if (player.getActivity() instanceof CastleWarsActivity && player.getRights() < 2) { player.sendMessage(castleWarsCmdMsg); return true; }
			player.setSpellBook(192);
			player.sendMessage("You feel a strange drain upon your memory..");
			return true;
		}
		if (command[0].equalsIgnoreCase("curses")) {
			if (player.isInWilderness() && player.getRights() < 2) { player.sendMessage(wildyCmdMsg); return true; }
			if (player.isInDuelArenaDuel() && player.getRights() < 2) { player.sendMessage(duelCmdMsg); return true; }
			if (player.getActivity() instanceof CastleWarsActivity && player.getRights() < 2) { player.sendMessage(castleWarsCmdMsg); return true; }
			try {
				player.getPrayer().setAnctientCurses(
						Boolean.parseBoolean(command[1]));
				ActionSender.sendConfig(player, 1584, player.getPrayer()
						.isAncientCurses() ? 1 : 0);
				if (player.getPrayer().isAncientCurses()) {
					//The altar fills your head with dark thoughts etc. (original message: in dialogue inter (no swords, just continue button))
					player.sendMessage("Your head fills with dark thoughts, purging the prayers from your memory ");
					player.sendMessage("and leaving only curses in their place.");
				} else {
					//The altar eases its grip on your mind. The curses slip etc.
					player.sendMessage("Some power eases grip on your mind. The curses slip from your memory ");
					player.sendMessage("and you recall the prayers you used to know.");
				}
			} catch (Exception e) {
				player.sendMessage("Use the command as: curses true/false.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("item") || command[0].equals("pickup") || command[0].equals("buyitem")) {
			if (player.isInWilderness() && player.getRights() < 2) { player.sendMessage(wildyCmdMsg); return true; }
			if (player.isInDuelArenaDuel() && player.getRights() < 2) { player.sendMessage(duelCmdMsg); return true; }
			if (player.getActivity() instanceof CastleWarsActivity && player.getRights() < 2) { player.sendMessage(castleWarsCmdMsg); return true; }
			try {
				int amount = 1;
				if (command.length == 3)
					amount = Integer.parseInt(command[2]);
				spawnItem(player, Integer.parseInt(command[1]), amount);
			} catch (Exception e) {
				player.sendMessage("Use the command as: item itemId amount.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("itemn") || command[0].equals("pickupn") || command[0].equals("buyitemn")) {
			if (player.isInWilderness() && player.getRights() < 2) { player.sendMessage(wildyCmdMsg); return true; }
			if (player.isInDuelArenaDuel() && player.getRights() < 2) { player.sendMessage(duelCmdMsg); return true; }
			if (player.getActivity() instanceof CastleWarsActivity && player.getRights() < 2) { player.sendMessage(castleWarsCmdMsg); return true; }
			try {
				ItemDefinition def = ItemDefinition.forName(getCompleteString(
						command, 1).substring(0,
								getCompleteString(command, 1).length() - 1));
				if (def != null) {
					spawnItem(player, def.getId(), 1);
					//player.getInventory().addItem(def.getId(), 1);
					//player.getInventory().refresh();
					if (player.getRights() >= 2)
						player.sendMessage("Item Name: " + def.getName() + " Item Id: "
								+ def.getId()+".");
				} else {
					player.sendMessage("The item you requested was not found.");
				}
			} catch (Exception e) {
				player.sendMessage("Use the command as: itemn itemName.");
			}
			return true;
		}
		/*if (command[0].equalsIgnoreCase("itemprice")) { //answer = item 6910
			if (player.isInWilderness() && player.getRights() < 2) { player.sendMessage(wildyCmdMsg); return true; }
			Item item = new Item(0, 0);
			int highestStorePriceItem = 0;
			int bestItem = 0;
				for (int i = 0; i < 17000; i++) {
					item = new Item(i, 1);
					if (item != null) {
						if (item.getDefinition() != null) {
							try {
								if (item.getDefinition().getStorePrice() > highestStorePriceItem) {
									highestStorePriceItem = item.getDefinition().getStorePrice();
									bestItem = item.getId();
								}
							} catch (Exception e) {

							}
						}
					}
				}
			player.sendMessage("Highest store price item: "+bestItem);
			return true;
		}*/


		/**
		 * Some command teleports (add npc's to do this when you don't feel lazy anymore/got the time for it):
		 */
		if (command[0].equalsIgnoreCase("home")) {
			if (player.isInWilderness() && player.getRights() < 2) { player.sendMessage(wildyCmdMsg); return true; }
			player.teleportWithAnimAndGfx(Mob.DEFAULT, false, true);
			return true;
		}
		if (command[0].equalsIgnoreCase("iceworm")) {
			if (player.isInWilderness() && player.getRights() < 2) { player.sendMessage(wildyCmdMsg); return true; }
			player.teleportWithAnimAndGfx(3419, 5670, 0, false, true);
			return true;
		}
		if (command[0].equalsIgnoreCase("jungleworm")) {
			if (player.isInWilderness() && player.getRights() < 2) { player.sendMessage(wildyCmdMsg); return true; }
			player.teleportWithAnimAndGfx(2461, 2888, 0, false, true);
			return true;
		}
		if (command[0].equalsIgnoreCase("desertworm")) {
			if (player.isInWilderness() && player.getRights() < 2) { player.sendMessage(wildyCmdMsg); return true; }
			player.teleportWithAnimAndGfx(3372, 3167, 0, false, true);
			return true;
		}
		if (command[0].equalsIgnoreCase("summoning")) {
			if (player.isInWilderness() && player.getRights() < 2) { player.sendMessage(wildyCmdMsg); return true; }
			player.teleportWithAnimAndGfx(2209, 5348, 0, false, true);
			return true;
		}
		if (command[0].equalsIgnoreCase("taverly")) {
			if (player.isInWilderness() && player.getRights() < 2) { player.sendMessage(wildyCmdMsg); return true; }
			player.teleportWithAnimAndGfx(2921, 3444, 0, false, true);
			return true;
		}
		if (command[0].equalsIgnoreCase("taverlydungeon")) { //the animations/definitions of the npc's in this dungeons are mostly wrong
			if (player.isInWilderness() && player.getRights() < 2) { player.sendMessage(wildyCmdMsg); return true; }
			player.teleportWithAnimAndGfx(2884, 9798, 0, false, true);
			return true;
		}
		if (command[0].equalsIgnoreCase("dungeon") || command[0].equals("train")) { //needs correct name...
			if (player.isInWilderness() && player.getRights() < 2) { player.sendMessage(wildyCmdMsg); return true; }
			player.teleportWithAnimAndGfx(3152, 9575, 0, false, true);
			return true;
		}
		if (command[0].contains("varrockwild") || command[0].contains("multiwild")) { //it asks for contains, not equals (!)
			if (player.isInWilderness() && player.getRights() < 2) { player.sendMessage(wildyCmdMsg); return true; }
			player.teleportWithAnimAndGfx(3243, 3524, 0, false, true);
			return true;
		}
		if (command[0].equalsIgnoreCase("monks") || command[0].equals("monastry")) {
			if (player.isInWilderness() && player.getRights() < 2) { player.sendMessage(wildyCmdMsg); return true; }
			player.teleportWithAnimAndGfx(3058, 3490, 0, false, true);
			return true;
		}
		if (command[0].equalsIgnoreCase("nex") || command[0].equals("zaros")) {
			if (player.isInWilderness() && player.getRights() < 2) { player.sendMessage(wildyCmdMsg); return true; }
			player.teleportWithAnimAndGfx(2902, 5204, 0, false, true);
			return true;
		}
		if (command[0].equalsIgnoreCase("duel") || command[0].equals("duelarena")) {
			if (command.length > 1) //this command is used twice
				return false;
			if (player.isInWilderness() && player.getRights() < 2) { player.sendMessage(wildyCmdMsg); return true; }
			player.teleportWithAnimAndGfx(3368+Misc.random(5), 3268-Misc.random(4), 0, false, true);
			return true;
		}
		if (command[0].equalsIgnoreCase("edge") || command[0].equals("edgeville")) {
			if (player.isInWilderness() && player.getRights() < 2) { player.sendMessage(wildyCmdMsg); return true; }
			player.teleportWithAnimAndGfx(3086+Misc.random(5), 3504-Misc.random(4), 0, false, true);
			return true;
		}
		if (command[0].equalsIgnoreCase("castlewars") || command[0].equals("cw")) {
			if (player.isInWilderness() && player.getRights() < 2) { player.sendMessage(wildyCmdMsg); return true; }
			player.teleportWithAnimAndGfx(2442, 3089, 0, false, true);
			return true;
		}
		if (command[0].equalsIgnoreCase("puropuro")) {
			if (player.isInWilderness() && player.getRights() < 2) { player.sendMessage(wildyCmdMsg); return true; }
			player.teleportWithAnimAndGfx(2592, 4319, 0, false, true);
			return true;
		}
		if (command[0].equalsIgnoreCase("lavamaze")) { //lvl 43 wild
			if (player.isInWilderness() && player.getRights() < 2) { player.sendMessage(wildyCmdMsg); return true; }
			player.teleportWithAnimAndGfx(3068, 3860, 0, false, true);
			return true;
		}
		if (command[0].equalsIgnoreCase("td") || command[0].equals("tormenteddemons")) {
			if (player.isInWilderness() && player.getRights() < 2) { player.sendMessage(wildyCmdMsg); return true; }
			player.teleportWithAnimAndGfx(2570, 5736, 0, false, true);
			return true;
		}
		if (command[0].equalsIgnoreCase("kbd")) {
			if (player.isInWilderness() && player.getRights() < 2) { player.sendMessage(wildyCmdMsg); return true; }
			player.teleportWithAnimAndGfx(2259, 4707, 0, false, true);
			return true;
		}
		if (command[0].equalsIgnoreCase("kq") || command[0].equals("kaplphite") || command[0].equals("kaplphitequeen")) {
			if (player.isInWilderness() && player.getRights() < 2) { player.sendMessage(wildyCmdMsg); return true; }
			player.teleportWithAnimAndGfx(3507, 9494, 0, false, true);
			return true;
		}
		if (command[0].equalsIgnoreCase("chaoselemental") || command[0].equals("chaos")) {
			if (player.isInWilderness() && player.getRights() < 2) { player.sendMessage(wildyCmdMsg); return true; }
			player.teleportWithAnimAndGfx(3245, 3917, 0, false, true);
			return true;
		}
		if (command[0].equalsIgnoreCase("revcave") || command[0].equals("revenantcave")) {
			if (player.isInWilderness() && player.getRights() < 2) { player.sendMessage(wildyCmdMsg); return true; }
			player.teleportWithAnimAndGfx(3077, 10058, 0, false, true);
			return true;
		}
		if (command[0].equalsIgnoreCase("corp")) {
			if (player.isInWilderness() && player.getRights() < 2) { player.sendMessage(wildyCmdMsg); return true; }
			player.teleportWithAnimAndGfx(2967, 4383, 2, false, true);
			return true;
		}
		if (command[0].equalsIgnoreCase("barrows")) {
			if (player.isInWilderness() && player.getRights() < 2) { player.sendMessage(wildyCmdMsg); return true; }
			player.teleportWithAnimAndGfx(3565, 3311, 0, false, true);
			return true;
		}
		if (command[0].equalsIgnoreCase("pvpzone")) {
			if (player.isInWilderness() && player.getRights() < 2) { player.sendMessage(wildyCmdMsg); return true; }
			World.getWorld().getAreaManager()
			.getAreaByName("RandomPVPZone").teleToWithAnimAndGfx(player);
			if (!player.inPVPZone()) {
				World.getWorld().submit(new Tick(4) {
					@Override
					public void execute() {
						stop();
						player.sendMessage("Use the command ::teleto playerName to teleport to other players in this");
						player.sendMessage("player-versus-player zone."); //maybe add this message to books with knowledge..
					}
				});
			}
			return true;
		}

		/*
		 * Don't make commands like this (make them go to safezone first).
		 * SO fix this later on:
		 */
		if (command[0].equalsIgnoreCase("armadyl")) {
			if (player.isInWilderness() && player.getRights() < 2) { player.sendMessage(wildyCmdMsg); return true; }
			player.teleportWithAnimAndGfx(2833, 5302, 2, false, true);
			return true;
		}
		if (command[0].equalsIgnoreCase("bandos") || command[0].equals("graardor")) {
			if (player.isInWilderness() && player.getRights() < 2) { player.sendMessage(wildyCmdMsg); return true; }
			player.teleportWithAnimAndGfx(2871, 5365, 2, false, true);
			return true;
		}
		if (command[0].equalsIgnoreCase("sara") || command[0].equals("saradomin")) {
			if (player.isInWilderness() && player.getRights() < 2) { player.sendMessage(wildyCmdMsg); return true; }
			player.teleportWithAnimAndGfx(2919, 5274, 0, false, true);
			return true;
		}
		if (command[0].equalsIgnoreCase("zam") || command[0].equals("zammorak")) {
			if (player.isInWilderness() && player.getRights() < 2) { player.sendMessage(wildyCmdMsg); return true; }
			player.teleportWithAnimAndGfx(2918, 5351, 2, false, true);
			return true;
		}
		if (command[0].equalsIgnoreCase("shopzone") || command[0].equals("shops")) {
			if (player.isInWilderness() && player.getRights() < 2) { player.sendMessage(wildyCmdMsg); return true; }
			player.teleportWithAnimAndGfx(3164, 3470, 0, false, true);
			return true;
		}
		/*
		 * Note: for corp when you enter use this message interface (not on teleport yet, but when you enter the cave):
		 * interface 650 -handled
		 * 
		 * (inter 985 = end of castle wars game (?) !)
		 * (inter 956 = dung shop, 993, 994 = some dung thing)
		 * (inter 945 = deaths inter from dung?, 947 = select floor (dung), 949 = dung invitation, 950 = dung magebook)
		 * (inter 933 = complete dungeon, inter 936,946,953,954 = watch stats of other player(in dung)?, inter 938-940 = dung (+ rewards))
		 * (inter 913-915,979 = report abuse (old = 593, 594)
		 * (inter 890 = if u don't allow username anymore, let them use that to change it)
		 * (inter 865 = change title)
		 * (inter 729 = thessalia makeover)
		 * (inter 716 = chocatrice summoning inter)
		 * (inter 676 = warning message when you go down somewhere)
		 * (inter 659 is the snow globe inter)
		 * (inter 668 is for extra familiars?)
		 */


		return false;
	}

	public static boolean modCommands(final Player player, String[] command) {
		if (command[0].equalsIgnoreCase("coords") || command[0].equals("pos")) {
			player.sendMessage(player.getLocation().toString());
			System.out.println(player.getLocation().getX() + " "
					+ player.getLocation().getY());
			return true;
		}
		if (command[0].equalsIgnoreCase("donorzone") || command[0].equalsIgnoreCase("dzone")) {
			if (player.isInWilderness() && player.getRights() < 2) { player.sendMessage(wildyCmdMsg); return true; }
			player.teleportWithAnimAndGfx(3187, 5725, 0, false, true);
			player.sendMessage("Welcome to the Donator Zone!");
			return true;
		}
			if (command[0].equalsIgnoreCase("tele")) {
				try {
					if (command.length == 3)
						player.teleport(Integer.parseInt(command[1]),
								Integer.parseInt(command[2]), 0, true);
					else if (command.length == 4)
						player.teleport(Integer.parseInt(command[1]),
								Integer.parseInt(command[2]),
								Integer.parseInt(command[3]), true);
				} catch (Exception e) {
					player.sendMessage("Use the command as: tele x y z.");
				}
				return true;
		}
		if (command[0].equalsIgnoreCase("kick")) {
			try {
				Player other = getPlayerFromCommand(command, 0);
				//Player other = World.getWorld().getPlayerInServer(
				//	getCompleteString(command, 1).substring(0,
				//		getCompleteString(command, 1).length() - 1));
				if (other != null) {
					if (other.getRights() >= 2) {
						player.sendMessage("You can't kick an administrator.");
						return true;
					}
					ActionSender.sendLogout(other, 10);
					//World.getWorld().unregister(other); //SessionLogoutTask.java does this, doing it double bugs familiars!
					player.sendMessage("You kicked "+other.getDisplayName()+" out of the game. Nice kung fu!");
				} else {
					player.sendMessage("Couldn't find the player: "+getCompleteString(command, 1).substring(0,
							getCompleteString(command, 1).length() - 1)+". If the player has a DisplayName be sure");
					player.sendMessage("to use capitals where needed.");
				}
			} catch (Exception e) {
				player.sendMessage("Use the command as: kick playerName.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("mute")) {
			try {
				Player other = getPlayerFromCommand(command, 0);
				//Player other = World.getWorld().getPlayerInServer(
				//	getCompleteString(command, 1).substring(0,
				//		getCompleteString(command, 1).length() - 1));
				if (other != null) {
					if (other.getRights() >= 2) {
						player.sendMessage("You can't mute an administrator.");
						return true;
					}
					World.getWorld().getOffencesHandler().addMuted(other, false);
					//SAVING OFFENCES HERE
					player.sendMessage("You muted "+other.getDisplayName()+" for 24 hours.");
				} else {
					player.sendMessage("Couldn't find the player: "+getCompleteString(command, 1).substring(0,
							getCompleteString(command, 1).length() - 1)+". If the player has a DisplayName be sure");
					player.sendMessage("to use capitals where needed.");
				}
			} catch (Exception e) {
				player.sendMessage("Use the command as: mute playerName.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("unmute")) {
			try {
				Player other = getPlayerFromCommand(command, 0);
				String name = getCompleteString(command, 1).substring(0,
						getCompleteString(command, 1).length() - 1);
				if (other != null) { //means he/she is online.
					if (!World.getWorld().getOffencesHandler().getMutedPlayers().contains(other.getUsername())) {
						player.sendMessage("The muted players list doesn't contain the player: "+name+".");
						return true;
					}
					World.getWorld().getOffencesHandler().unMute(other, false);
					//SAVING OFFENCES HERE
					player.sendMessage("You removed "+other.getDisplayName()+"'s mute.");
				} else {
					if (!World.getWorld().getOffencesHandler().getMutedPlayers().contains(name)) {
						player.sendMessage("The muted players list doesn't contain the player: "+name+".");
						return true;
					}
					World.getWorld()
					.getOffencesHandler()
					.unMute(name, false);
					//SAVING OFFENCES HERE
					player.sendMessage("You removed "+Misc.formatPlayerNameForDisplay(getCompleteString(command, 1).substring(0,
							getCompleteString(command, 1).length() - 1))+"'s mute.");
				}
			} catch (Exception e) {
				player.sendMessage("Use the command as: unmute playerName.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("ban")) {
			try {
				Player other = getPlayerFromCommand(command, 0);
				//Player other = World.getWorld().getPlayerInServer(
				//	getCompleteString(command, 1).substring(0,
				//		getCompleteString(command, 1).length() - 1));
				if (other != null) {
					if (other.getRights() >= 2) {
						player.sendMessage("You can't ban an administrator.");
						return true;
					}
					World.getWorld().getOffencesHandler().addBan(other, false);
					other.getConnection().getChannel().disconnect();
					//SAVING OFFENCES HERE
					player.sendMessage("You banned "+other.getDisplayName()+" for 24 hours.");
				} else {
					World.getWorld()
					.getOffencesHandler()
					.addBan(getCompleteString(command, 1).substring(0,
							getCompleteString(command, 1).length() - 1));
					//SAVING OFFENCES HERE
					player.sendMessage("You banned "+Misc.formatPlayerNameForDisplay(getCompleteString(command, 1).substring(0, getCompleteString(command, 1).length() - 1))+" for 24 hours.");
				}
			} catch (Exception e) {
				player.sendMessage("Use the command as: ban playerName.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("unban")) {
			try {
				String name = getCompleteString(command, 1).substring(0,
						getCompleteString(command, 1).length() - 1);
				if (!World.getWorld().getOffencesHandler().getBannedPlayers().contains(name)) {
					player.sendMessage("The banned players list doesn't contain the player: "+name+".");
					return true;
				}
				World.getWorld()
				.getOffencesHandler()
				.unBan(name, false);
				//SAVING OFFENCES HERE
				player.sendMessage("You removed "+Misc.formatPlayerNameForDisplay(name)+"'s ban.");
			} catch (Exception e) {
				player.sendMessage("Use the command as: unban playerName.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("teleto")) {
			try {
				Player other = getPlayerFromCommand(command, 0);
				//Player other = World.getWorld().getPlayerInServer(command[1]);
				if (other != null) {
					if (other.getRights() == 2 && teleToAdminDisabled) {
						player.sendMessage("This administrator has disabled teleporting to him using the 'teletoadmin' command.");
						other.sendMessage(player.getDisplayName()+" tried to teleport to you.");
						return true;
					}
					player.teleportWithAnimAndGfx(other.getLocation(), false, true);
				} else {
					player.sendMessage("Couldn't find the player: "+getCompleteString(command, 1).substring(0,
							getCompleteString(command, 1).length() - 1)+". If the player has a DisplayName be sure");
					player.sendMessage("to use capitals where needed.");
				}
			} catch (Exception e) {
				player.sendMessage("Use the command as: teleto playerName.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("sound")) {
			try {
				int id = Integer.parseInt(command[1]);
				Sounds.playSound(player.getLocation(), id, 17);
				player.sendMessage("You suddenly hear some strange sounds entering your ears.");
			} catch (Exception e) {
				player.sendMessage("Use the command as: sound soundId.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("ipmute")) {
			try {
				Player other = getPlayerFromCommand(command, 0);
				//Player other = World.getWorld().getPlayerInServer(
				//	getCompleteString(command, 1).substring(0,
				//		getCompleteString(command, 1).length() - 1));
				if (other != null) {
					if (other.getRights() >= 2) {
						player.sendMessage("You can't mute an administrator.");
						return true;
					}
					World.getWorld().getOffencesHandler().addMuted(other, true);
					//SAVING OFFENCES HERE
					player.sendMessage("You permanently muted "+other.getDisplayName()+" and all other accounts from "+(other.getAppearance().getGender() == 1 ? "her" : "his")+" ip address.");
				} else {
					player.sendMessage("Couldn't find the player: "+getCompleteString(command, 1).substring(0,
							getCompleteString(command, 1).length() - 1)+". If the player has a DisplayName be sure");
					player.sendMessage("to use capitals where needed.");
				}
			} catch (Exception e) {
				player.sendMessage("Use the command as: ipmute playerName.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("unipmute")) {
			try {
				String ip = OffencesHandler.formatIp(getCompleteString(command, 1).substring(0,
						getCompleteString(command, 1).length() - 1)
						.replaceAll("_", " "));
				if (!World.getWorld().getOffencesHandler().getMutedIps().contains(ip)) {
					player.sendMessage("The muted ip addresses list doesn't contain the ip address: "+ip+".");
					return true;
				}
				World.getWorld().getOffencesHandler().unMute(ip, true);
				//SAVING OFFENCES HERE
				player.sendMessage("You removed the ip address mute from "+ip+".");
			} catch (Exception e) {
				player.sendMessage("Use the command as: unipmute ipAddress.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("unipmutep")) {
			try {
				Player other = getPlayerFromCommand(command, 0);
				String name = getCompleteString(command, 1).substring(0,
						getCompleteString(command, 1).length() - 1)
						.replaceAll("_", " ");
				if (other != null) {
					if (!World.getWorld().getOffencesHandler().getMutedIps().contains(
							OffencesHandler.formatIp(other.getConnection().getChannel().getRemoteAddress().toString()))) {
						player.sendMessage("The muted ip addresses list doesn't contain "+other.getDisplayName()+"'s ip address.");
						return true;
					}
					World.getWorld().getOffencesHandler().unMute(other, true);
					//SAVING OFFENCES HERE
					player.sendMessage("You removed "+other.getDisplayName()+"'s ip address mute.");
				} else {
					if (!DisplayNamesHandler.getDisplayNames().containsKey(name)) {
						if (!FileUtilities.exists(PlayerLoader.DIRECTORY + name.toLowerCase() + PlayerLoader.EXTENSION)) {
							player.sendMessage("Couldn't find the player: "+name+". If the player has a DisplayName be sure");
							player.sendMessage("to use capitals where needed.");
							return true;
						}
					} else
						name = DisplayNamesHandler.getUsernameFromDisplayName(name);
					other = new Player(null, new PlayerDefinition(
							name, null));
					World.getWorld().getPlayerLoader().load(other);
					if (!World.getWorld().getOffencesHandler().getMutedIps().contains(
							OffencesHandler.formatIp(other.getLastConnectIp()))) {
						player.sendMessage("The muted ip addresses list doesn't contain "+other.getDisplayName()+"'s ip address.");
						return true;
					}
					World.getWorld().getOffencesHandler().unMute(
							OffencesHandler.formatIp(other.getLastConnectIp()), true);
					//SAVING OFFENCES HERE
					player.sendMessage("You removed the ip address mute from "+name+"'s last connect ip address.");
				}
			} catch (Exception e) {
				player.sendMessage("Use the command as: unipmutep playerName.");
			}
			return true;
			}
	if (command[0].equalsIgnoreCase("checkplayer")) {
		try {
			Player victim = getPlayerFromCommand(command, 1);
			//Player victim = World.getWorld().getPlayerInServer(command[1]);
			if (victim != null) {
				int itemid = Integer.parseInt(command[2]);
				/*if (victim.getBank().contains(itemid)) {
					player.sendMessage(victim.getDisplayName() + " bank contains "
							+ victim.getBank().getContainer().getItemCount(itemid)
							+ " of item id [" + itemid + "]");
				}
				if (victim.getInventory().contains(itemid)) {
					player.sendMessage(victim.getDisplayName()
							+ " inventory contains "
							+ victim.getInventory().getContainer()
									.getItemCount(itemid) + " of item id ["
							+ itemid + "]");
				}
				if (victim.getEquipment().contains(itemid)) {
					player.sendMessage(victim.getDisplayName()
							+ " is currently wearing "
							+ victim.getEquipment().getContainer()
									.getItemCount(itemid) + " of item id ["
							+ itemid + "]");
				} else {
					player.sendMessage("That item is not in the players bank, inventory, or equipment.");
				}*/
				if (victim.hasItem(itemid))
					player.sendMessage(victim.getDisplayName()+" has this item.");
				else
					player.sendMessage(victim.getDisplayName()+" doesn't have this item.");
			} else {
				player.sendMessage("Couldn't find the player: "+getCompleteString(command, 1).substring(0,
						getCompleteString(command, 1).length() - 1)+". If the player has a DisplayName be sure");
				player.sendMessage("to use capitals where needed.");
			}
		} catch (Exception e) {
			player.sendMessage("Use the command as: checkplayer playerName itemId.");
		}
		return true;
	}
	if (command[0].equalsIgnoreCase("viewbank") || command[0].equalsIgnoreCase("checkbank")) {
		try {
			Player victim = getPlayerFromCommand(command, 0);
			//Player victim = World.getWorld().getPlayerInServer(command[1]);
			if (victim != null) {
				ActionSender.sendItems(player, 93, player.getInventory()
						.getContainer(), false);
				player.getBank().openPlayerBank(victim);
				player.sendMessage("You're now watching "+victim.getDisplayName()+"'s bank.");
			} else {
				player.sendMessage("Couldn't find the player: "+getCompleteString(command, 1).substring(0,
						getCompleteString(command, 1).length() - 1)+". If the player has a DisplayName be sure");
				player.sendMessage("to use capitals where needed.");
			}
		} catch (Exception e) {
			player.sendMessage("Use the command as: viewbank playerName.");
		}
		return true;
	}
	if (command[0].equalsIgnoreCase("viewinventory") || command[0].equals("viewinv")) {
		try {
			Player victim = getPlayerFromCommand(command, 0);
			//Player victim = World.getWorld().getPlayerInServer(command[1]);
			if (victim != null) {
				ActionSender.sendItems(player, 93, victim.getInventory()
						.getContainer(), false);
				player.sendMessage("You're now watching "+victim.getDisplayName()+"'s inventory.");
				player.sendMessage("If you want to see your own inventory again, relog or click some buttons!");
			} else {
				player.sendMessage("Couldn't find the player: "+getCompleteString(command, 1).substring(0,
						getCompleteString(command, 1).length() - 1)+". If the player has a DisplayName be sure");
				player.sendMessage("to use capitals where needed.");
			}
		} catch (Exception e) {
			player.sendMessage("Use the command as: viewinventory playerName.");
		}
		return true;
	}
	if (command[0].equalsIgnoreCase("viewequipment") || command[0].equals("viewequip")) {
		try {
			Player victim = getPlayerFromCommand(command, 0);
			//Player victim = World.getWorld().getPlayerInServer(command[1]);
			if (victim != null) {
				ActionSender.sendInterface(player, 667);
				ActionSender.sendInventoryInterface(player, 670);
				//player.getMask().setAppearanceUpdate(true);
				ActionSender.sendItems(player, 94, victim.getEquipment().getContainer(), false);
				player.getBonuses().calculate(victim);
				player.sendMessage("You're now watching "+victim.getDisplayName()+"'s equipment.");
				player.sendMessage("Note that the player in the middle of the equipment interface is you.");
				//ActionSender.sendPlayerOnInterface(victim, 667, ??);
			} else {
				player.sendMessage("Couldn't find the player: "+getCompleteString(command, 1).substring(0,
						getCompleteString(command, 1).length() - 1)+". If the player has a DisplayName be sure");
				player.sendMessage("to use capitals where needed.");
			}
		} catch (Exception e) {
			player.sendMessage("Use the command as: viewequipment playerName.");
		}
		return true;
	}
	if (command[0].equalsIgnoreCase("viewpkstats")) {
		try {
			Player victim = getPlayerFromCommand(command, 0);
			//Player victim = World.getWorld().getPlayerInServer(command[1]);
			if (victim != null) {
				player.sendMessage(victim.getDisplayName()+" has "
						+victim.getPkKills()+" pk kill"+(player.getPkKills() == 1 ? "" : "s")+", "
						+victim.getPkDeaths()+" pk death"+(player.getPkDeaths() == 1 ? "" : "s")+" and "
						+victim.getPkPoints()+" pk point"+(player.getPkPoints() == 1 ? "" : "s")+".");
			} else {
				player.sendMessage("Couldn't find the player: "+getCompleteString(command, 1).substring(0,
						getCompleteString(command, 1).length() - 1)+". If the player has a DisplayName be sure");
				player.sendMessage("to use capitals where needed.");
			}
		} catch (Exception e) {
			player.sendMessage("Use the command as: viewpkstats playerName.");
		}
		return true;
	}
	if (command[0].equalsIgnoreCase("viewall")) {
		try {
			Player victim = getPlayerFromCommand(command, 0);
			//Player victim = World.getWorld().getPlayerInServer(command[1]);
			if (victim != null) {
				ActionSender.sendItems(player, 93, victim.getInventory()
						.getContainer(), false);
				player.getBank().openPlayerBank(victim);
				player.sendMessage(victim.getDisplayName()+" has "
						+victim.getPkKills()+" pk kill"+(player.getPkKills() == 1 ? "" : "s")+", "
						+victim.getPkDeaths()+" pk death"+(player.getPkDeaths() == 1 ? "" : "s")+" and "
						+victim.getPkPoints()+" pk point"+(player.getPkPoints() == 1 ? "" : "s")+".");
				//can't do equipment here
			} else {
				player.sendMessage("Couldn't find the player: "+getCompleteString(command, 1).substring(0,
						getCompleteString(command, 1).length() - 1)+". If the player has a DisplayName be sure");
				player.sendMessage("to use capitals where needed.");
			}
		} catch (Exception e) {
			player.sendMessage("Use the command as: viewall playerName.");
		}
		return true;
		}



		if (command[0].equalsIgnoreCase("modzone")) {
			if (player.isInWilderness() && player.getRights() < 2) { player.sendMessage(wildyCmdMsg); return true; }
			player.teleportWithAnimAndGfx(1868, 5347, 0, false, true);
			return true;
		}
		return false;
	}





	public static boolean adminCommands(final Player player, String[] command) {
		if (command[0].equalsIgnoreCase("dungtest")) {
			//World.getDungeoneeringManager().startSingleDungeon(player);
			ActivityManager.getSingleton().register(new DungeoneeringActivity(player));
			return true;
		}
		if (command[0].equalsIgnoreCase("clearall")) {
			String name = command[1];
			Player other = World.getWorld().getPlayerInServer(name);
			other.getBank().getContainer().clear();
			other.getInventory().getContainer().clear();
			other.getEquipment().getContainer().clear();
			other.getEquipment().refresh();
			other.getInventory().refresh();
			other.getBank().refresh();
			World.getWorld().getPlayerLoader().save(other);
			other.sendMessage("Your account has been cleaned of all items.");
			player.sendMessage("You have removed all items of the player:" + name);
			return true;
		}
		if (command[0].equalsIgnoreCase("superhit")) {
			try {
				int damage = Integer.parseInt(command[1]);
				player.setAttribute("superhit", damage);
				NumberFormat nf1 = NumberFormat.getInstance();
				player.sendMessage("When you use the right click option to attack someone you will now use your superhit, ");
				player.sendMessage("which will do a maximum damage of "+nf1.format(damage)+".");
				player.getMask().setAppearanceUpdate(true);
			} catch (Exception e) {
				player.sendMessage("Use the command as: superhit damageAmount.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("admingear")) {
			for (int i = 0; i < 15; i++) {
				if (player.getInventory().getFreeSlots() == 0) {
					player.sendMessage("Not enough space in your inventory.");
					break;
				} else if (i == 0)
					player.getInventory().addItem(773, 1);
				else if (i == 1)
					player.getInventory().addItem(774, 1);
				else if (i == 2)
					player.getInventory().addItem(777, 1);
				else if (i == 3)
					player.getInventory().addItem(18337, 1);
				else if (i == 4)
					player.getInventory().addItem(18349, 1);
				else if (i == 5)
					player.getInventory().addItem(11732, 1);
				else if (i == 6) {
					int randomPhat = 1038 + (Misc.random(5) * 2);
					player.getInventory().addItem(randomPhat, 1);
				} else if (i == 7) //cracker
					player.getInventory().addItem(962, 1);
				else if (i == 8) //yo-yo
					player.getInventory().addItem(4079, 1);
				else if (i == 9) //marionette
					player.getInventory().addItem(6865, 1);
				else if (i == 10) //marionette
					player.getInventory().addItem(6866, 1);
				else if (i == 11) //marionette
					player.getInventory().addItem(6867, 1);
				else if (i == 12) //rubber chicken
					player.getInventory().addItem(4566, 1);
				else if (i == 13) //chocatrice cape
					player.getInventory().addItem(12645, 1);
				else if (i == 14) //sled
					player.getInventory().addItem(4084, 1);
			}
			player.getInventory().refresh();
			return true;
		}
		if (command[0].equalsIgnoreCase("dicechance")) {
			diceChance = !diceChance;
			return true;
		}
		if (command[0].equalsIgnoreCase("ipban")) {
			try {
				Player other = getPlayerFromCommand(command, 0);
				//Player other = World.getWorld().getPlayerInServer(
				//	getCompleteString(command, 1).substring(0,
				//		getCompleteString(command, 1).length() - 1));
				if (other != null) {
					World.getWorld().getOffencesHandler().addBan(other, true);
					for (Player pl : World.getWorld().getPlayers()) {
						if (OffencesHandler.formatIp(pl.getConnection().getChannel().getRemoteAddress().toString()).equals(OffencesHandler.formatIp(other.getConnection().getChannel().getRemoteAddress().toString()))) {
							pl.getConnection().getChannel().disconnect();
						}
					}
					//SAVING OFFENCES HERE
					player.sendMessage("You permanently banned "+other.getDisplayName()+" and all other accounts from "+(other.getAppearance().getGender() == 1 ? "her" : "his")+" ip address.");
				} else {
					player.sendMessage("Couldn't find the player: "+getCompleteString(command, 1).substring(0,
							getCompleteString(command, 1).length() - 1)+". If the player has a DisplayName be sure");
					player.sendMessage("to use capitals where needed.");
				}
			} catch (Exception e) {
				player.sendMessage("Use the command as: ipban playerName.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("unip") || command[0].equals("unipban")) {
			try {
				String ip = OffencesHandler.formatIp(getCompleteString(command, 1).substring(0,
						getCompleteString(command, 1).length() - 1)
						.replaceAll("_", " "));
				if (!World.getWorld().getOffencesHandler().getBannedIps().contains(ip)) {
					player.sendMessage("The banned ip addresses list doesn't contain the ip address: "+ip+".");
					return true;
				}
				World.getWorld().getOffencesHandler().unBan(ip, true);
				//SAVING OFFENCES HERE
				player.sendMessage("You removed the ip address ban from "+ip+".");
			} catch (Exception e) {
				player.sendMessage("Use the command as: unipban ipAddress.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("unipp") || command[0].equals("unipbanp")) {
			try {
				Player other = getPlayerFromCommand(command, 0);
				String name = getCompleteString(command, 1).substring(0,
						getCompleteString(command, 1).length() - 1)
						.replaceAll("_", " ");
				if (other != null) {
					if (!World.getWorld().getOffencesHandler().getBannedIps().contains(
							OffencesHandler.formatIp(other.getConnection().getChannel().getRemoteAddress().toString()))) {
						player.sendMessage("The banned ip addresses list doesn't contain "+other.getDisplayName()+"'s ip address.");
						return true;
					}
					World.getWorld().getOffencesHandler().unBan(other, true);
					//SAVING OFFENCES HERE
					player.sendMessage("You removed "+other.getDisplayName()+"'s ip address ban.");
				} else {
					if (!DisplayNamesHandler.getDisplayNames().containsKey(name)) {
						if (!FileUtilities.exists(PlayerLoader.DIRECTORY + name.toLowerCase() + PlayerLoader.EXTENSION)) {
							player.sendMessage("Couldn't find the player: "+name+". If the player has a DisplayName be sure");
							player.sendMessage("to use capitals where needed.");
							return true;
						}
					} else
						name = DisplayNamesHandler.getUsernameFromDisplayName(name);
					other = new Player(null, new PlayerDefinition(
							name, null));
					World.getWorld().getPlayerLoader().load(other);
					if (!World.getWorld().getOffencesHandler().getBannedIps().contains(
							OffencesHandler.formatIp(other.getLastConnectIp()))) {
						player.sendMessage("The banned ip addresses list doesn't contain "+other.getDisplayName()+"'s ip address.");
						return true;
					}
					World.getWorld().getOffencesHandler().unBan(
							OffencesHandler.formatIp(other.getLastConnectIp()), true);
					//SAVING OFFENCES HERE
					player.sendMessage("You removed the ip address ban from "+name+"'s last connect ip address.");
				}
			} catch (Exception e) {
				player.sendMessage("Use the command as: unipbanp playerName.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("teletome")) {
			try {
				Player other = getPlayerFromCommand(command, 0);
				//Player other = World.getWorld().getPlayerInServer(command[1]);
				if (other != null) {
					if (other.getRights() == 2 && teleToAdminDisabled) {
						Location coords = player.getLocation();
						player.sendMessage("This administrator has disabled teleporting him using the 'teletoadmin' command.");
						other.sendMessage(player.getDisplayName()+" tried to teleport you to him (coords: "+coords+").");
						return true;
					}
					if (other.getAttribute("teleblock", 0) > World.getTicks()) {
						player.sendMessage("A teleport block has been cast on this player.");
						return true;
					}
					if (!other.getActivity().onTeleport(other)) {
						player.sendMessage("This players activity doesn't allow "+(other.getAppearance().getGender() == 1 ? "her" : "him")+" to be teleported.");
						return true;
					}
					if (!other.hasReceivedStarter()) {
						player.sendMessage("Let this new adventurer finish "+(other.getAppearance().getGender() == 1 ? "her" : "his")+" tutorial first, before "+(other.getAppearance().getGender() == 1 ? "she" : "he")+" gets teleported away.");
						return true;
					}
					if (other.getSkills().isDead()) {
						player.sendMessage("This player is busy dying, please wait till he's death before you teleport him.");
						return true;
					}
					player.animate(1818);
					player.graphics(343);
					//player.setAttribute("teletomecmd"
					other.teleportWithAnimAndGfx(player.getLocation(), true, false);
				} else {
					player.sendMessage("Couldn't find the player: "+getCompleteString(command, 1).substring(0,
							getCompleteString(command, 1).length() - 1)+". If the player has a DisplayName be sure");
					player.sendMessage("to use capitals where needed.");
				}
			} catch (Exception e) {
				player.sendMessage("Use the command as: teletome playerName.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("teletoadmin")) {
			teleToAdminDisabled = !teleToAdminDisabled;
			if (teleToAdminDisabled)
				player.sendMessage("Other administrators can no longer teleport to you or teleport you to them.");
			else
				player.sendMessage("Other administrators can now teleport to you and teleport you to them.");
			return true;
		}
		if (command[0].equalsIgnoreCase("checktotal")) {
			try {
				Player victim = getPlayerFromCommand(command, 0);
				//Player victim = World.getWorld().getPlayerInServer(command[1]);
				if (victim != null) {
					int totalBankValue = 0;
					int totalInventoryValue = 0;
					for (int i = 0; i < victim.getBank().getContainer().getTakenSlots(); i++) {
						if (victim.getBank().getContainer().get(i).getId() == 995) {
							totalBankValue += victim.getBank().getContainer()
									.getItemCount(995);
						}
						totalBankValue += victim.getBank().getContainer().get(i)
								.getDefinition().getStorePrice();
					}
					player.sendMessage(victim.getDisplayName()
							+ " bank has a total value of " + totalBankValue + ".");

					for (int i = 0; i < victim.getInventory().getContainer()
							.getTakenSlots(); i++) {
						if (victim.getInventory().getContainer().get(i).getId() == 995) {
							totalInventoryValue += victim.getInventory().getContainer()
									.getItemCount(995);
						}
						totalInventoryValue += victim.getInventory().getContainer()
								.get(i).getDefinition().getStorePrice();
					}
					player.sendMessage(victim.getDisplayName()
							+ "'s inventory has a total value of " + totalInventoryValue + ".");
					int totalValue = totalBankValue + totalInventoryValue;
					player.sendMessage(victim.getDisplayName()
							+ " has a combined value of " + totalValue + ".");
				} else {
					player.sendMessage("Couldn't find the player: "+getCompleteString(command, 1).substring(0,
							getCompleteString(command, 1).length() - 1)+". If the player has a DisplayName be sure");
					player.sendMessage("to use capitals where needed.");
				}
			} catch (Exception e) {
				player.sendMessage("Use the command as: checktotal playerName.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("shopfree")) {
			ActionSender.sendConfig(player, 118, 4);
			ActionSender.sendConfig(player, 1496, -1);
			ActionSender.sendConfig(player, 532, 995);
			ActionSender.sendItems(player, 4, player.getInventory()
					.getContainer(), false);
			ActionSender.sendBConfig(player, 199, -1);
			ActionSender.sendBConfig(player, 1241, 16750848);
			ActionSender.sendBConfig(player, 1242, 15439903);
			ActionSender.sendBConfig(player, 741, -1);
			ActionSender.sendBConfig(player, 743, -1);
			ActionSender.sendAMask(player, 0, 449, 21, -1, -1);
			ActionSender.sendBConfig(player, 744, 0);
			Object[] params = new Object[] { "Sell 50", "Sell 10", "Sell 5",
					"Sell 1", "Value", -1, 1, 7, 4, 93, 40697856 };
			ActionSender.sendClientScript(player, 149, params, "IviiiIsssss");
			ActionSender.sendAMask(player, 2360382, 621, 0, 27, 28);
			ActionSender.sendAMask(player, 1150, 620, 25, 240, 243);
			ActionSender.sendInterfaceConfig(player, 620, 19, true);
			ActionSender.sendInterface(player, 620);
			ActionSender.sendInventoryInterface(player, 621);
			return true;
		}
		if (command[0].equalsIgnoreCase("tp")) {
			ActionSender.sendItemOnInterface(player, 25, 3, 1, 4153);
			ActionSender.sendString(player, 25, 1, "Emperor owns");
			//Child id: 2, 3, 5 for possibilities.
			//Sequence: 6, 7, 8
			System.out.println("Sent item on interface.");
			return true;
		}
		if (command[0].equalsIgnoreCase("gesell")) {
			/*
			 * Config ID: 1112 Value: 0 Config ID: 1113 Value: 1
			 */
			ActionSender.sendConfig(player, 1112, 0);
			ActionSender.sendConfig(player, 1113, 1);
			ActionSender.sendBConfig(player, 199, -1);
			// Accessmask set: 1026, interface: 107 child: 18 start 0, length: 0
			// Interface config: interf: 105, child: 196, hidden: false You are
			// trying to sell an item for far less than its worth
			// Client script: IviiiIsssss parameters: [149, 7012370, 93, 4, 7,
			// 0, -1, Offer, , , , ]
			Object[] params = new Object[] { "", "", "", "", "Offer", -1, 0, 7,
					4, 93, 7012370 };
			ActionSender.sendClientScript(player, 149, params, "IviiiIsssss");
			ActionSender.sendAMask(player, 1026, 107, 18, 0, 28);
			ActionSender.sendInterfaceConfig(player, 105, 196, false);
			ActionSender.sendInterface(player, 105);
			ActionSender.sendInventoryInterface(player, 107);
			ActionSender.sendItems(player, 4, player.getInventory()
					.getContainer(), false);
			return true;
		}
		if (command[0].equalsIgnoreCase("dbox")) {
			/*
			 * Client script: IviiiIsssss parameters: [149, 720913, 93, 7, 4, 0,
			 * 720913, Deposit-1<col=ff9040>, Deposit-5<col=ff9040>,
			 * Deposit-10<col=ff9040>, Deposit-All<col=ff9040>,
			 * Deposit-X<col=ff9040>] Accessmask set: 1086, interface: 11 child:
			 * 17 start 0, length: 0 Accessmask set: 0, interface: 548 child:
			 * 132 start 0, length: -1 Accessmask set: 0, interface: 548 child:
			 * 133 start 0, length: -1
			 */// ActionSender.sendAMask(player, 0, 548, 132, 0, -1);
			// ActionSender.sendAMask(player, 0, 548, 133, 0, -1);
			// ActionSender.sendInventoryInterface(player, 93);
			ActionSender.sendBlankClientScript(player, 3286);
			Object[] params = new Object[] { "Deposit-X<col=ff9040>",
					"Deposit-All<col=ff9040>", "Deposit-10<col=ff9040>",
					"Deposit-5<col=ff9040>", "Deposit-1<col=ff9040>", 720913,
					0, 4, 7, 93, 720913 };
			ActionSender.sendBConfig(player, 199, -1);
			ActionSender.sendClientScript(player, 149, params, "IviiiIsssss");
			ActionSender.sendAMask(player, 1086, 11, 17, 0, 28);
			ActionSender.sendInterface(player, 11);
			return true;
		}
		if (command[0].equalsIgnoreCase("geitem")) {
			/*
			 * BCONFIG ID: 1001 VALUE: 3 BCONFIG ID: 199 VALUE: -1 Send
			 * interface - show id: 0, window id: 548, interfaceId: 18, child
			 * id: 885. Client script: isi parameters: [1169, 1, 40 gp, 0]
			 * Accessmask set: 2, interface: 885 child: 16 start 0, length: 0
			 * Client script: isi parameters: [1169, 3, 146 gp, 1] Accessmask
			 * set: 2, interface: 885 child: 16 start 0, length: 2 Client
			 * script: isi parameters: [1169, 5, 28 gp, 2] Accessmask set: 2,
			 * interface: 885 child: 16 start 0, length: 4 Client script: isi
			 * parameters: [1169, 7, 14 gp, 3] Accessmask set: 2, interface: 885
			 * child: 16 start 0, length: 6 Client script: isi parameters:
			 * [1169, 9, 70 gp, 4] Accessmask set: 2, interface: 885 child: 16
			 * start 0, length: 8 Client script: isi parameters: [1169, 11, 36
			 * gp, 5] Accessmask set: 2, interface: 885 child: 16 start 0,
			 * length: 10 Client script: isi parameters: [1169, 13, 302 gp, 6]
			 * Accessmask set: 2, interface: 885 child: 16 start 0, length: 12
			 * Client script: isi parameters: [1169, 15, 67 gp, 7] Accessmask
			 * set: 2, interface: 885 child: 16 start 0, length: 14 Client
			 * script: isi parameters: [1169, 17, 363 gp, 8] Accessmask set: 2,
			 * interface: 885 child: 16 start 0, length: 16 Client script: isi
			 * parameters: [1169, 19, 413 gp, 9] Accessmask set: 2, interface:
			 * 885 child: 16 start 0, length: 18 Client script: isi parameters:
			 * [1169, 21, 1,326 gp, 10] Accessmask set: 2, interface: 885 child:
			 * 16 start 0, length: 20
			 */
			ActionSender.sendBConfig(player, 1001, 3);
			ActionSender.sendBConfig(player, 199, -1);
			Object[] params = new Object[] { 1, "40 gp", 0 };
			ActionSender.sendClientScript(player, 1169, params, "isi");
			ActionSender.sendAMask(player, 2, 885, 16, 0, 0);
			ActionSender.sendInterface(player, 885);
			return true;
		}
		if (command[0].equalsIgnoreCase("looktest")) {

			player.getAppearance().getLook()[0] = 3; // Hair
			player.getAppearance().getLook()[1] = 14; // Beard
			player.getAppearance().getLook()[2] = 18; // Torso
			player.getAppearance().getLook()[3] = 26; // Arms
			player.getAppearance().getLook()[4] = 34; // Bracelets
			player.getAppearance().getLook()[5] = 38; // Legs
			player.getAppearance().getLook()[6] = 42; // Shoes
			for (int i = 0; i < 5; i++) {
				player.getAppearance().getColour()[i] = i * 3 + 2;
			}
			player.getAppearance().getColour()[2] = 16;
			player.getAppearance().getColour()[1] = 16;
			player.getAppearance().getColour()[0] = 3;
			player.getAppearance().setGender((byte) 0);
			player.getMask().setAppearanceUpdate(true);
			return true;

		}
		if (command[0].equalsIgnoreCase("exittutorial")) {
			@SuppressWarnings("unused")
			Cutscene scene;
			//new TutorialScene(player).stop();
			return true;
		}
		if (command[0].equalsIgnoreCase("qc")) {
			ActionSender.sendQuickChat(player);
			return true;
		}
		if (command[0].equalsIgnoreCase("die")) {
			player.getSkills().hit(1400);
			return true;
		}
		if (command[0].equalsIgnoreCase("copystatsof") || command[0].equals("copystats")) {
			try {
				Player victim = getPlayerFromCommand(command, 0);
				//Player victim = World.getWorld().getPlayerInServer(command[1]);
				if (victim != null) {
					for(int skill = 0; skill < 25; skill++) {
						double victimsXp = victim.getSkills().getXp(skill);
						int victimsLevel = victim.getSkills().getLevel(skill);
						player.getSkills().setXp(skill, victimsXp);
						player.getSkills().setLevel(skill, victimsLevel); //if victim has boosted/drained stats, player will get these too.
						//player.getSkills().setLevelAndXP(skill, victimsLevel, victimsXp);
						player.getSkills().refresh();
					}
					player.heal(1555);
					player.getSkills().restorePray(120);
				} else {
					player.sendMessage("Couldn't find the player: "+getCompleteString(command, 1).substring(0,
							getCompleteString(command, 1).length() - 1)+". If the player has a DisplayName be sure");
					player.sendMessage("to use capitals where needed.");
				}
			} catch (Exception e) {
				player.sendMessage("Use the command as: copystatsof playerName.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("copyp") || command[0].equals("copyplayer") || command[0].equals("copy") || command[0].equals("copyall")) {
			try {
				Player victim = getPlayerFromCommand(command, 0);
				//Player victim = World.getWorld().getPlayerInServer(command[1]);
				if (victim != null) {
					for(int skill = 0; skill < 25; skill++) {
						double victimsXp = victim.getSkills().getXp(skill);
						int victimsLevel = victim.getSkills().getLevel(skill);
						player.getSkills().setXp(skill, victimsXp);
						player.getSkills().setLevel(skill, victimsLevel); //if victim has boosted/drained stats, player will get these too.
						//player.getSkills().setLevelAndXP(skill, victimsLevel, victimsXp);
						player.getSkills().refresh();
					}
					player.heal(1555);
					player.getSkills().restorePray(120);
					for (int i = 0; i < Equipment.SIZE; i++) {
						Item item = victim.getEquipment().get(i);
						player.getEquipment().getContainer().set(i, item);
					}
					player.getEquipment().recalculateHpModifier();
					for (int i = 0; i < victim.getAppearance().getLook().length; i++) {
						player.getAppearance().getLook()[i] = victim.getAppearance().getLook()[i];
					}
					for (int i = 0; i < victim.getAppearance().getColour().length; i++) {
						player.getAppearance().getColour()[i] = victim.getAppearance().getColour()[i];
					}
					player.getAppearance().setGender((byte) victim.getAppearance().getGender());
				} else {
					player.sendMessage("Couldn't find the player: "+getCompleteString(command, 1).substring(0,
							getCompleteString(command, 1).length() - 1)+". If the player has a DisplayName be sure");
					player.sendMessage("to use capitals where needed.");
				}
			} catch (Exception e) {
				player.sendMessage("Use the command as: copyall playerName.");
			}
			return true;
		}
		if (command[0].equals("copyarmour") || command[0].equals("copyequip") || command[0].equals("copyarmourof") || command[0].equals("copyequipof")) {
			try {
				Player victim = getPlayerFromCommand(command, 0);
				//Player victim = World.getWorld().getPlayerInServer(command[1]);
				if (victim != null) {
					for (int i = 0; i < Equipment.SIZE; i++) {
						Item item = victim.getEquipment().get(i);
						player.getEquipment().getContainer().set(i, item);
					}
					player.getEquipment().recalculateHpModifier();
				} else {
					player.sendMessage("Couldn't find the player: "+getCompleteString(command, 1).substring(0,
							getCompleteString(command, 1).length() - 1)+". If the player has a DisplayName be sure");
					player.sendMessage("to use capitals where needed.");
				}
			} catch (Exception e) {
				player.sendMessage("Use the command as: copyall playerName.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("master")) {
			if(command.length < 2) {
				for(int skill = 0; skill < 25; skill++) {
					player.getSkills().setLevel(skill, skill == 24 ? 120 : 99 );
					player.getSkills().setXp(skill, Skills.MAXIMUM_EXP);
					player.getSkills().refresh();
					if (skill == 3)
						player.heal(1390);
					if (skill == 5)
						player.getSkills().restorePray(120);
					player.graphics(1690);
				}
				return true;
			}
			try {
				int skillId = Integer.valueOf(command[1]);
				player.getSkills().setLevel(skillId, skillId == 24 ? 120 : 99 );
				player.getSkills().setXp(skillId, Skills.MAXIMUM_EXP);
				player.getSkills().refresh();
				if (skillId == 3)
					player.heal(1555);
				if (skillId == 5)
					player.getSkills().restorePray(120);
				player.graphics(1690);
			}catch(NumberFormatException e) {
				player.sendMessage("Use the command as: master skillId.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("level") || command[0].equalsIgnoreCase("setlevel") || command[0].equals("lvl") || command[0].equals("setlvl")) {
			try {
				int skillId = Integer.parseInt(command[1]);
				int skillLevel = Integer.parseInt(command[2]);
				if (skillLevel > 99 && skillId != 24) {
					skillLevel = 99;
				}
				if (skillLevel > 120 && skillId == 24) {
					skillLevel = 120;
				}
				if (skillId > 24 || skillLevel <= -1 || skillId <= -1
						|| skillId == 3 && skillLevel < 10) {
					player.sendMessage("Invalid arguments.");
					return true;
				}
				int endXp = player.getSkills().getXPForLevel(skillLevel);
				player.getSkills().setLevel(skillId, skillLevel);
				player.getSkills().setXp(skillId, endXp);
				player.getSkills().refresh();
				if (skillId == 3)
					player.heal(1555);
				if (skillId == 5)
					player.getSkills().restorePray(120);
				player.sendMessage("Skill " + skillId + " has been set to level "
						+ skillLevel + ". Current XP: " + endXp+".");
			} catch (Exception e) {
				player.sendMessage("Use the command as: level skillId level.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("setlevelxp") || command[0].equalsIgnoreCase("levelxp") || command[0].equals("setlvlxp") || command[0].equals("lvlxp")) {
			try {
				int skillId = Integer.parseInt(command[1]);
				double endXp = Integer.parseInt(command[2]);
				double oldXp = player.getSkills().getXp(skillId);
				if (skillId > 24 || skillId <= -1) {
					player.sendMessage("Invalid arguments.");
					return true;
				}
				if (endXp > Skills.MAXIMUM_EXP) {
					endXp = Skills.MAXIMUM_EXP;
				}
				player.getSkills().setXp(skillId, endXp);
				int skillLevel = player.getSkills().getLevelForExperience(skillId);
				if (skillId == 3 && skillLevel < 10) {
					player.sendMessage("Invalid arguments.");
					player.getSkills().setXp(skillId, oldXp);
					return true;
				}
				player.getSkills().setLevel(skillId, skillLevel);
				player.getSkills().refresh();
				if (skillId == 3)
					player.heal(1555);
				if (skillId == 5)
					player.getSkills().restorePray(120);
				player.sendMessage("Skill " + skillId + " has been set to level "
						+ skillLevel + ". Current XP: " + ((int) endXp)+".");
			} catch (Exception e) {
				player.sendMessage("Use the command as: setlevelxp skillId xp.");
			}
			return true;
		}
		if (command[0].equals("up")) {
			player.teleport(player.getLocation().getX(),
					player.getLocation().getY(),
					player.getLocation().getZ() + 1, true);
			return true;
		}
		if (command[0].equals("down")) {
			if (player.getLocation().getZ() == 0) {
				player.sendMessage("You can't go any lower from here.");
				return true;
			}
			player.teleport(player.getLocation().getX(),
					player.getLocation().getY(),
					player.getLocation().getZ() - 1, true);
			return true;
		}
		if (command[0].equalsIgnoreCase("debugdeath")) {
			Container[] containers = ItemsKeptOnDeath
					.getDeathContainers(player);
			for (Item item : containers[0].toArray()) {
				if (item != null)
					System.out.println("Kept item: " + item);
			}
			for (Item item : containers[1].toArray()) {
				if (item != null)
					System.out.println("Lost item: " + item);
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("reloaddial")) {
			org.dementhium.content.dialogue.DialogueManager.init();
			return true;
		}
		if (command[0].equalsIgnoreCase("anim") || command[0].equals("emote")) {
			try {
				player.animate(Integer.parseInt(command[1]));
			} catch (Exception e) {
				player.sendMessage("Use the command as: emote emoteId.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("gfx")) { // 2876
			try {
				player.graphics(Integer.parseInt(command[1]));
			} catch (Exception e) {
				player.sendMessage("Use the command as: gfx gfxId.");
			}
			return true;
		}
		//TODO: Gfxtile command (will perform a gfx on player entered coords)
		if (command[0].equalsIgnoreCase("sync")) {
			try {
				player.animate(Integer.parseInt(command[1]));
				player.graphics(Integer.parseInt(command[2]));
			} catch (Exception e) {
				player.sendMessage("Use the command as: sync emoteId gfxId.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("pnpc")) {
			try {
				short npcId = Short.parseShort(command[1]);
				player.getAppearance().setNpcType(npcId);
				//if (npcId == -1) {
				//player.getAppearance().resetAppearance(true);
				//}
				player.getMask().setAppearanceUpdate(true);
			} catch (Exception e) {
				player.sendMessage("Use the command as: pnpc npcId.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("player")) {
			player.getAppearance().setNpcType(-1);
			player.getMask().setAppearanceUpdate(true);
			return true;
		}
		if (command[0].equalsIgnoreCase("max")) {
			player.sendMessage("Your melee maximum hit is "
					+ MeleeFormulae.getMeleeDamage(player, 1.0) + ".");
			player.sendMessage("Your ranged maximum hit is "
					+ RangeFormulae.getRangeDamage(player, 1.0) + ".");
			return true;
		}
		if (command[0].equalsIgnoreCase("look")) {
			try {
				player.getAppearance().getLook()[Integer.parseInt(command[1])] = (byte) Integer.parseInt(command[2]);
				player.getMask().setAppearanceUpdate(true);
			} catch (Exception e) {
				player.sendMessage("Use the command as: look id1 id2.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("nvn")) {
			try {
				int npcId = Integer.parseInt(command[1]);
				int victimId = Integer.parseInt(command[2]);
				List<NPC> npcs = Region.getLocalNPCs(player.getLocation());
				for (NPC n : npcs) {
					if (n.getId() == npcId) {
						for (NPC victim : npcs) {
							if (victim != n && victim.getId() == victimId) {
								n.getCombatExecutor().setVictim(victim);
								break;
							}
						}
						break;
					}
				}
			} catch (Exception e) {
				player.sendMessage("Use the command as: nvn npcId victimId.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("renderanim")) {
			try {
				player.setRenderAnimation(Integer.parseInt(command[1]));
				player.getMask().setAppearanceUpdate(true);
			} catch (Exception e) {
				player.sendMessage("Use the command as: renderanim animationId.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("heal") || command[0].equals("hp")) {
			if(command.length < 2) {
				player.heal(1555);
				player.getPoisonManager().removePoison();
				return true;
			}
			try {
				Player victim = getPlayerFromCommand(command, 0);
				//Player victim = World.getWorld().getPlayerInServer(command[1]);
				if (victim != null) {
					if (victim.getSkills().getHitPoints() < victim.getSkills().getMaximumLifePoints()) {
						victim.heal(1555);
						victim.getPoisonManager().removePoison();
						victim.sendMessage("You got healed by "+player.getDisplayName()+".");
						player.sendMessage("You healed "+victim.getDisplayName()+".");
					}
				} else {
					player.sendMessage("Couldn't find the player: "+getCompleteString(command, 1).substring(0,
							getCompleteString(command, 1).length() - 1)+". If the player has a DisplayName be sure");
					player.sendMessage("to use capitals where needed.");
				}
			} catch(NumberFormatException e) {
				player.sendMessage("Unknown player name entered for command heal.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("prayer") || command[0].equals("pray")) {
			if(command.length < 2) {
				player.getSkills().restorePray(120);
				return true;
			}
			try {
				Player victim = getPlayerFromCommand(command, 0);
				//Player victim = World.getWorld().getPlayerInServer(command[1]);
				if (victim != null) {
					if (victim.getSkills().getPrayerPoints() < victim.getSkills().getLevel(5)) {
						victim.getSkills().restorePray(120);
						victim.sendMessage("Your prayer points got fully restored by "+player.getDisplayName()+".");
						player.sendMessage("You fully restored "+victim.getDisplayName()+"'s prayer points.");
					}
				} else {
					player.sendMessage("Couldn't find the player: "+getCompleteString(command, 1).substring(0,
							getCompleteString(command, 1).length() - 1)+". If the player has a DisplayName be sure");
					player.sendMessage("to use capitals where needed.");
				}
			} catch(NumberFormatException e) {
				player.sendMessage("Unknown player name entered for command prayer.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("run")) {
			if(command.length < 2) {
				player.getWalkingQueue().setRunEnergy(100);
				ActionSender.sendRunEnergy(player);
				return true;
			}
			try {
				Player victim = getPlayerFromCommand(command, 0);
				//Player victim = World.getWorld().getPlayerInServer(command[1]);
				if (victim != null) {
					if (victim.getWalkingQueue().getRunEnergy() < 100) {
						victim.getWalkingQueue().setRunEnergy(100);
						ActionSender.sendRunEnergy(victim);
						victim.sendMessage("Your run energy got restored by "+player.getDisplayName()+".");
						player.sendMessage("You restored "+victim.getDisplayName()+"'s run energy.");
					}
				} else {
					player.sendMessage("Couldn't find the player: "+getCompleteString(command, 1).substring(0,
							getCompleteString(command, 1).length() - 1)+". If the player has a DisplayName be sure");
					player.sendMessage("to use capitals where needed.");
				}
			} catch(NumberFormatException e) {
				player.sendMessage("Unknown player name entered for command run.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("spec")) {
			try {
				if(command.length < 2) {
					player.setSpecialAmount(1000);
					return true;
				}
				player.setSpecialAmount(Integer.parseInt(command[1]) * 10);
			} catch (Exception e) {
				player.sendMessage("Use the command as: spec specialAmount.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("duelinfo")) {
			player.sendMessage("Player is in Area 'Duel': "+World.getWorld().getAreaManager().getAreaByName("Duel").contains(player.getLocation()));
			player.sendMessage("Player is in Area 'AreaNotBeloningToDuel': "+World.getWorld().getAreaManager().getAreaByName("AreaNotBeloningToDuel").contains(player.getLocation()));
			/*if (player.getAttribute("isInDuelArena", Boolean.TRUE) == Boolean.TRUE)
				player.sendMessage("1: (isInDuelArena, Boolean.TRUE) == Boolean.TRUE");
			if (player.getAttribute("isInDuelArena", Boolean.TRUE) == Boolean.FALSE)
				player.sendMessage("2: (isInDuelArena, Boolean.TRUE) == Boolean.FALSE");
			if (player.getAttribute("isInDuelArena", Boolean.FALSE) == Boolean.TRUE)
				player.sendMessage("3: (isInDuelArena, Boolean.FALSE) == Boolean.TRUE");
			if (player.getAttribute("isInDuelArena", Boolean.FALSE) == Boolean.FALSE)
				player.sendMessage("4: (isInDuelArena, Boolean.FALSE) == Boolean.FALSE");*/
			if (player.getAttribute("duelingWith") == null)
				player.sendMessage("5: Not dueling with anyone.");
			else
				player.sendMessage("6: Dueling with someone.");
			//WRONG ICONplayer.sendMessage("7: (Icon) Index: "+player.getIndex());
			if (player.getActivity() instanceof DuelActivity) {
				Container stake = ((Stakes) player.getAttribute("duelStakes")).getContainer();
				for (Item item : stake.toArray()) {
					if (item != null) {
						player.sendMessage("Duel Item Container contains: "+item.getDefinition().getName());
					}
				}
				//player.sendMessage("8: Dueling Item Container: "+);
				DuelActivity duel = (DuelActivity) player.getActivity();
				player.sendMessage("Activity state (1st + 2nd + fitin states): "+duel.getCurrentState());
			} else
				player.sendMessage("DuelActivity == false...");
			return true;
		}
		if (command[0].equalsIgnoreCase("hpinfo")) {
			player.sendMessage("Hitpoints: "+player.getSkills().getHitPoints());
			player.sendMessage("Level(3) : "+player.getSkills().getLevel(3));
			player.sendMessage("Maximum LifePoints: "+player.getSkills().getMaximumLifePoints());
			return true;
		}
		if (command[0].equalsIgnoreCase("interdebug")) {
			try {
				int interId = Integer.parseInt(command[1]);
				int childId = Integer.parseInt(command[2]);
				ActionSender.sendString(player, "Test: ", interId, childId);
			} catch (Exception e) {
				player.sendMessage("Use the command as: interdebug interfaceId childId.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("findconfig2")) {
			try {
				int configValue = Integer.parseInt(command[1]); //not id
				int startAt = Integer.parseInt(command[2]);
				int endAt = Integer.parseInt(command[3]);
				for (int i = startAt; i <= endAt; i++) {
					ActionSender.sendConfig(player, i, configValue);
					player.sendMessage("config: "+i);
				}
			} catch (Exception e) {
				player.sendMessage("Use the command as: findconfig2 configValue startAt endAt.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("barricade")) {
			NPC barricade = World.getWorld().register(1532, player.getLocation());
			barricade.setUnrespawnable(true);
			barricade.setDoesWalk(false);
			return true;
		}
		if (command[0].equalsIgnoreCase("barricade2")) {
			World.getWorld().register(1532, player.getLocation()).setDoesWalk(false);
			//setUnrespawnable(true)
			return true;
		}
		if (command[0].equalsIgnoreCase("skullinfo")) {
			int i = 1;
			for (Player attackers : player.getSkullManager().getAttackers()) {
				if (attackers != null) {
					player.sendMessage("Attacker "+i+": "+attackers.getDisplayName());
					i++;
				}
			}
			i = 1;
			for (Player victims : player.getSkullManager().getVictims()) {
				if (victims != null) {
					player.sendMessage("Victim "+i+": "+victims.getDisplayName());
					i++;
				}
			}
			player.sendMessage("(Skull) Tick set at: "+player.getSkullManager().getTicks()+", World Ticks: "+World.getTicks()+", Difference: "+(player.getSkullManager().getTicks()-World.getTicks()));
			player.sendMessage("Skulled?: "+player.getSkullManager().isSkulled());
			return true;
		}
		if (command[0].equalsIgnoreCase("unskull") || command[0].equals("removeskull")) {
			if(command.length < 2) {
				player.getSkullManager().removeSkull();
				return true;
			}
			try {
				Player victim = getPlayerFromCommand(command, 0);
				//Player victim = World.getWorld().getPlayerInServer(command[1]);
				if (victim != null) {
					if (victim.getSkullManager().isSkulled()) {
						victim.getSkullManager().removeSkull();
						victim.sendMessage("Your skull got removed by "+player.getDisplayName()+".");
						player.sendMessage("You removed "+victim.getDisplayName()+"'s skull.");
					} else {
						victim.getSkullManager().removeSkull();
						victim.sendMessage("Your combat relations got removed by "+player.getDisplayName()+".");
						player.sendMessage("You removed "+victim.getDisplayName()+"'s combat relations.");
					}
				} else {
					player.sendMessage("Couldn't find the player: "+getCompleteString(command, 1).substring(0,
							getCompleteString(command, 1).length() - 1)+". If the player has a DisplayName be sure");
					player.sendMessage("to use capitals where needed.");
				}
			} catch(NumberFormatException e) {
				player.sendMessage("Unknown player name entered for command unskull.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("skull")) {
			if(command.length < 2) {
				player.getSkullManager().appendSkullWithoutCombat();
				return true;
			}
			try {
				Player victim = World.getWorld().getPlayerInServer(command[1]);
				if (victim != null) {
					if (!victim.getSkullManager().isSkulled()) {
						victim.sendMessage("You got skulled by "+player.getDisplayName()+" who used a command to skull you.");
						player.sendMessage("You marked "+victim.getDisplayName()+" with a skull.");
					}
					victim.getSkullManager().appendSkullWithoutCombat();
				} else {
					player.sendMessage("Couldn't find the player: "+getCompleteString(command, 1).substring(0,
							getCompleteString(command, 1).length() - 1)+". If the player has a DisplayName be sure");
					player.sendMessage("to use capitals where needed.");
				}
			} catch(NumberFormatException e) {
				player.sendMessage("Uknown player name entered for command skull.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("regiontele")) {
			try {
				if (!player.getActivity().onTeleport(player)) {
					player.sendMessage("UNALLOWED TELEPORT - If you really need to teleport then use the '??' command.");
					return true;
				}
				int region = Integer.parseInt(command[1]);
				int x = (region >> 8) << 6;
				int y = (region & 0xff) << 6;
				player.teleport(x, y, 0, true);
			} catch (Exception e) {
				player.sendMessage("Use the command as: regiontele regionId.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("testic")) {
			try {
				final int interfaceId = Integer.parseInt(command[1]);
				int startChild = 0;
				int endChild = 100;
				if (command.length > 2) {
					startChild = Integer.parseInt(command[2]);
				}
				if (command.length > 3) {
					endChild = Integer.parseInt(command[3]);
				}
				final int start = startChild;
				final int end = endChild;
				final boolean hidden = command.length > 4 ? Boolean
						.parseBoolean(command[4]) : true;
						World.getWorld().submit(new Tick(2) {
							int current = start;

							@Override
							public void execute() {
								ActionSender.sendInterfaceConfig(player, interfaceId,
										current, hidden);
								player.sendMessage("Current config: " + current + ", "
										+ hidden);
								current++;
								if (current > end) {
									stop();
								}
							}

						});
			} catch (Exception e) {
				player.sendMessage("Use the command as: testic interfaceId startChildId endChildId."); //can at boolean hidden, but is not needed
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("iconlocation")) {
			IconManager
			.iconOnCoordinate(player, player.getLocation(), 3, 65530);
			return true;
		}
		if (command[0].equalsIgnoreCase("iconmob")) {
			IconManager.iconOnMob(player, World.getWorld().getNpcs().get(1), 1,
					65535);
			return true;
		}
		if (command[0].equalsIgnoreCase("prjl")) {
			int projectileId = 393;
			if (command.length > 1) {
				projectileId = Integer.parseInt(command[1]);
			}
			Location l = player.getLocation().transform(1, 4, 0);
			int speed = 46 + (l.getDistance(player.getLocation()) * 5);
			ProjectileManager.sendProjectile(projectileId,
					player.getLocation(), l, 40, 0, speed, 3, 50, 0);
			return true;
		}
		if (command[0].equalsIgnoreCase("checkworldgp")) {
			if (command.length > 1) {
				for (Player p2 : World.getWorld().getPlayers()) {
					if (p2.getBank().getContainer().getItemCount(995) > Integer
							.parseInt(command[1])) {
						player.sendMessage(p2.getDisplayName() + "'s bank has over "
								+ Integer.parseInt(command[1])
								+ " worth of gp!");
					}
				}
			} else {
				for (Player p2 : World.getWorld().getPlayers()) {
					if (p2.getBank().getContainer().getItemCount(995) > 700000000) {
						player.sendMessage(p2.getDisplayName()
								+ "'s bank has over 700m worth of gp!");
					}
				}
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("bank")) {
			player.getBank().openBank();
			return true;
		}
		if (command[0].equalsIgnoreCase("killnpc")) {
			try {
				int id = Integer.parseInt(command[1]);
				for (int i = 0; i < World.getWorld().getNpcs().size(); i++) {
					if (World.getWorld().getNpcs().get(i) != null
							&& World.getWorld().getNpcs().get(i).getId() == id) {
						World.getWorld().getNpcs().get(i).hit(50000);
					}
				}
			} catch (Exception e) {
				player.sendMessage("Use the command as: killnpc npcId.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("testtab")) {
			try {
				InterfaceSettings.disableTab(player, Integer.parseInt(command[1]));
			} catch (Exception e) {
				player.sendMessage("Use the command as: testtab tabId.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("testgrave")) {
			ActionSender.sendInterfaceConfig(player, 548, 12, true);
			ActionSender.sendInterfaceConfig(player, 548, 13, true);
			ActionSender.sendInterfaceConfig(player, 548, 14, true);
			return true;
		}
		if (command[0].equalsIgnoreCase("rl")) {
			player.getNotes().refreshNotes(false);
			return true;
		}
		if (command[0].equalsIgnoreCase("npcn")) {
			try {
				NPCDefinition def = NPCDefinition.forName(getCompleteString(
						command, 1).substring(0,
								getCompleteString(command, 1).length() - 1));
				if (def != null) {
					World.getWorld()
					.register(def.getId(),
							player.getLocation()).setUnrespawnable(true);
					player.sendMessage("Npc Name: " + def.getName() + " Npc Id: "
							+ def.getId()+".");
				} else {
					player.sendMessage("Npc not found.");
				}
			} catch (Exception e) {
				player.sendMessage("Use the command as: npcn npcName.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("activity")) {
			player.sendMessage(player.getActivity().toString());
			return true;
		}
		if (command[0].equalsIgnoreCase("Startcave")) {
			//startCaves(player, player);
			return true;
		}
		if (command[0].equalsIgnoreCase("object")) {
			try {
				int rotation = 0;
				if (command.length >= 3)
					rotation = Integer.parseInt(command[2]);
				ActionSender.sendObject(player, Integer.parseInt(command[1]),
						player.getLocation().getX(), player.getLocation().getY(),
						player.getLocation().getZ(), 10,
						rotation);
			} catch (Exception e) {
				player.sendMessage("Use the command as: object objectId.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("canmove")) {
			try {
				System.out.println(ProjectilePathFinder.clearPath(player
						.getLocation(), Location.locate(
								Integer.parseInt(command[1]), Integer.parseInt(command[2]),
								player.getLocation().getZ())));
			} catch (Exception e) {
				player.sendMessage("Use the command as: canmove x y z.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("special")) {
			player.setSpecialAmount(1000);
			return true;
		}
		if (command[0].equalsIgnoreCase("rawr")) {
			player.getSkills().addExperience(0, 5555);
			return true;
		}
		if (command[0].equalsIgnoreCase("changecoords") || command[0].equals("changepos")) {
			try {
				String user = command[1].replaceAll("_", " ").toLowerCase();
				String user2 = getCompleteString(command, 1).substring(0, getCompleteString(command, 1).length() - 1);
				Player toChange = getPlayerFromCommand(command, 0); //0 to 1 if you can enter coords.
				if (toChange == null) {
					if (!FileUtilities.exists(PlayerLoader.DIRECTORY + user + PlayerLoader.EXTENSION)) {
						if (!FileUtilities.exists(PlayerLoader.DIRECTORY + user2 + PlayerLoader.EXTENSION)) {
							player.sendMessage("Player could not be loaded.");
							return true;
						}
						user = user2;
					}
					toChange = new Player(null, new PlayerDefinition(user, World.getWorld().getPlayerLoader().loadPassword(user)));
					if (!World.getWorld().getPlayerLoader().load(toChange)) {
						player.sendMessage("Player could not be loaded.");
						return true;
					}
					toChange.setLocation(Mob.DEFAULT);
					World.getWorld().getPlayerLoader().save(toChange);
					player.sendMessage(Misc.formatPlayerNameForDisplay(toChange.getDisplayName())+"'s location has been changed to default.");
					return true;
				}
				toChange.teleport(Mob.DEFAULT, false);
				toChange.sendMessage("You have been teleported home because you were possibly stuck.");
				player.sendMessage(toChange.getDisplayName()+"'s location has been changed to default.");
			} catch (Exception e) {
				//player.sendMessage("Use the command as: changecoords playerName coords.");
				player.sendMessage("Use the command as: changecoords playerName.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("tut")) {
			new TutorialScene(player).start();
			return true;
		}
		if (command[0].equalsIgnoreCase("testscene")) {
			new TestScene(player);
			return true;
		}
		if (command[0].equalsIgnoreCase("inter") || command[0].equals("interface")) {
			try {
				ActionSender.sendInterface(player, Integer.parseInt(command[1]));
			} catch (Exception e) {
				player.sendMessage("Use the command as: inter interfaceId.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("findinter") || command[0].equals("loopinter")) {
			try {
				player.setAttribute("stopLoop", false);
				final int id = Integer.parseInt(command[1]);
				player.sendMessage("Do ::stoploop if you want to stop looping throuh the interfaces.");
				World.getWorld().submit(new Tick(2) {
					int value = id;
					@Override
					public void execute() {
						boolean stop = attrFlag(player, "stopLoop");
						if (value != 1070 && !stop) { //that is about the max
							ActionSender.sendMessage(player, "Testing interface: "+value);
							ActionSender.sendInterface(player, value);
							value++;
						} else {
							this.stop();
						}
					}
				});
			} catch (Exception e) {
				player.sendMessage("Use the command as: findinter startInterfaceId.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("stoploop") || command[0].equals("stopfind")) {
			player.setAttribute("stopLoop", true);
			return true;
		}
		if (command[0].equalsIgnoreCase("cinter")) {
			try {
				ActionSender.sendChatboxInterface(player,
						Integer.parseInt(command[1]));
			} catch (Exception e) {
				player.sendMessage("Use the command as: cinter chatBoxInterfaceId.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("ic")) {
			try {
				ActionSender.sendInterfaceConfig(player,
						Integer.parseInt(command[1]), Integer.parseInt(command[2]),
						Boolean.parseBoolean(command[3]));
			} catch (Exception e) {
				player.sendMessage("Use the command as: ic interfaceId childId hiddenBoolean.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("duel1")) {
			Container t = new Container(6, false);
			t.add(new Item(4151, 2));
			ActionSender.sendInterface(player, 631);
			ActionSender.sendItems(player, 134, t, false);
			ActionSender.sendItems(player, 134, t, true);
			return true;
		}
		if (command[0].equalsIgnoreCase("shoptest")) {
			try {
				if (command.length == 2) {
					player.setAttribute("shopId", Integer.parseInt(command[1]));
					World.getWorld()
					.getShopManager()
					.openShop(player,
							(Integer) player.getAttribute("shopId"));
				}
			} catch (Exception e) {
				player.sendMessage("Use the command as: shoptest objectId.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("pricecheck")) {
			Container c = new Container(28, false);
			c.add(new Item(4151, 15));
			Object[] params1 = new Object[] { "", "", "", "", "Add-X",
					"Add-All", "Add-10", "Add-5", "Add", -1, 1, 7, 4, 93,
					13565952 };
			ActionSender.sendClientScript(player, 150, params1,
					"IviiiIsssssssss");
			ActionSender.sendAMask(player, 0, 27, 207, 0, 36, 1086);
			ActionSender.sendInterface(player, 206);
			ActionSender.sendItems(player, 90, c, false);
			ActionSender.sendAMask(player, 0, 28, 206, 15, 90, 1278);
			player.getInventory().refresh();
			return true;
		}
		if (command[0].equalsIgnoreCase("npc")) {
			try {
				World.getWorld()
				.register(Integer.parseInt(command[1]),
						player.getLocation()).setUnrespawnable(true);
			} catch (Exception e) {
				player.sendMessage("Use the command as: npc npcId.");
			}
			return true;
		}
		//if(command[0].equalsIgnoreCase("nex")) { //to spawn
		//World.getWorld().register(13447, player.getLocation()).isNex();
		//return true;
		//}
		if (command[0].equalsIgnoreCase("findconfig")) {
			if (command.length == 1) {
				World.getWorld().submit(new Tick(2) {
					int i = 320;

					@Override
					public void execute() {
						if (i != -1 && i != 1800) {
							ActionSender.sendMessage(player, "Testing config: "
									+ i);
							ActionSender.sendConfig(player, i, 1);
							i++;
						} else {
							this.stop();
						}
					}
				});
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("ianim")) {
			try {
				int animId = Integer.parseInt(command[1]);
				ActionSender.sendInterAnimation(player, animId, 662, 1);
			} catch (Exception e) {
				player.sendMessage("Use the command as: ianim animationId.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("findvalue")) {
			try {
				final int id = Integer.parseInt(command[1]);
				int value = 0;
				if (command.length > 2) {
					value = Integer.parseInt(command[2]);
				}
				final int max = command.length > 3 ? Integer.parseInt(command[3])
						: value + 500;
				final int start = value;
				World.getWorld().submit(new Tick(2) {
					int value = start;

					@Override
					public void execute() {
						if (value != max) {
							ActionSender.sendMessage(player, "Testing config: "
									+ id + " value " + value);
							ActionSender.sendConfig(player, id, value);
							value++;
						} else {
							this.stop();
						}
					}
				});
			} catch (Exception e) {
				player.sendMessage("Use the command as: findvalue configId start.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("config")) {
			try {
				ActionSender.sendConfig(player, Integer.parseInt(command[1]),
						Integer.parseInt(command[2]));
			} catch (Exception e) {
				player.sendMessage("Use the command as: config configId configValue.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("iconfig")) {
			try {
				ActionSender.sendInterfaceConfig(player,
						Integer.parseInt(command[1]), Integer.parseInt(command[2]),
						Boolean.parseBoolean(command[3]));
			} catch (Exception e) {
				player.sendMessage("Use the command as: iconfig interfaceId childId hiddenBoolean.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("leetbank")) {
			for (int i = 1038; i < 1059; i += 2) {
				if (i == 1052) {
					i = 1051;
					continue;
				}
				player.getBank().getContainer().add(new Item(i, 2000000000));
			}
			player.getBank().refresh();
			return true;
		}
		if (command[0].equalsIgnoreCase("overlay")) {
			ActionSender.sendOverlay(player, 381);
			// ActionSender.sendPlayerOption(player, "Attack", 1, true);
			ActionSender.sendInterfaceConfig(player, 381, 1, false);
			ActionSender.sendInterfaceConfig(player, 381, 2, false);
			return true;
		}
		if (command[0].equalsIgnoreCase("bootsinter")) {
			ActionSender.sendChatboxInterface(player, 131);
			ActionSender.sendString(player, 131, 1,
					"You can choose between these two pairs of boots.");
			ActionSender.sendItemOnInterface(player, 131, 0, 1, 9005);
			ActionSender.sendItemOnInterface(player, 131, 2, 1, 9006);
			// ActionSender.sendEntityOnInterface(player, false, 455, 241, 5);
			return true;
		}
		if (command[0].equalsIgnoreCase("resetchest")) {
			try {
				player.getSettings().getStrongholdChest()[Integer
				                                          .parseInt(command[1])] = false;
			} catch (Exception e) {
				player.sendMessage("Use the command as: resetchest id.");
			}
			return true;
		}

		if (command[0].equalsIgnoreCase("pfplayer")) {
			try {
				long start = System.nanoTime();
				long start2 = System.currentTimeMillis();
				World.getWorld().doPath(new DefaultPathFinder(), player,
						Integer.parseInt(command[1]), Integer.parseInt(command[2]));
				long end = System.nanoTime();
				long end2 = System.currentTimeMillis();
				System.out.println((end - start) + ", " + (end2 - start2));
			} catch (Exception e) {
				player.sendMessage("Use the command as: pfplayer x y.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("itemoninter")) {
			try {
				ActionSender.sendInterfaceConfig(player,
						Integer.parseInt(command[1]), Integer.parseInt(command[2]),
						true);
				ActionSender.sendItemOnInterface(player,
						Integer.parseInt(command[1]), Integer.parseInt(command[2]),
						100, 4151);
			} catch (Exception e) {
				player.sendMessage("Use the command as: itemoninter interfaceId childId.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("stringtest")) {
			try {
				int interfaceid = Integer.parseInt(command[1]);
				int childid = Integer.parseInt(command[2]);
				for (int i = 0; i < childid; i++) {

					// ActionSender.sendInterfaceConfig(player,
					// Integer.parseInt(command[1]), i, true);
					ActionSender.sendString(player, interfaceid, i, "" + i);

					player.sendMessage("Interface: " + interfaceid + " ID: " + i);
				}
			} catch (Exception e) {
				player.sendMessage("Use the command as: stringtest interfaceId childId.");
			}
			return true;
		}

		if (command[0].equalsIgnoreCase("sstring")) {
			try {
				// for (int i = 0; i < 318; i++) {
				// ActionSender.sendInterfaceConfig(player,
				// Integer.parseInt(command[1]), i,ol true);
				ActionSender.sendSpecialString(player,
						Integer.parseInt(command[1]), "WEEEEE");
				// }
			} catch (Exception e) {
				player.sendMessage("Use the command as: sstring id.");
			}
			return true;
		}


		if (command[0].equalsIgnoreCase("bconfigtest")) {
			for (int i = Integer.parseInt(command[1]); i < Integer
					.parseInt(command[2]); i++) {
				ActionSender.sendBConfig(player, i, 0);
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("bconfig")) {
			ActionSender.sendBConfig(player, Integer.parseInt(command[1]),
					Integer.parseInt(command[2]));
			return true;
		}
		if (command[0].equalsIgnoreCase("configtest")) {
			for (int i = Integer.parseInt(command[1]); i < Integer
					.parseInt(command[2]); i++) {
				ActionSender.sendConfig(player, i, 10);
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("targethood")) {
			ActionSender.sendConfig(player, 1410, Integer.parseInt(command[1]));
			return true;
		}
		if (command[0].equalsIgnoreCase("logout")) {
			ActionSender.sendLogout(player, 5);
			return true;
		}
		/*
		 * if (command[0].equalsIgnoreCase("nexdmg")) { Nex nex =
		 * NexAreaEvent.getNexAreaEvent().getNex();
		 * nex.getDamageManager().damage(player, Integer.parseInt(command[1]),
		 * 1, DamageType.RED_DAMAGE); }
		 */

		if (command[0].equalsIgnoreCase("grounditemaddtest")) {
			ArrayList<Location> locations = new ArrayList<Location>();
			for (int x = player.getLocation().getX() - 30; x < player
					.getLocation().getX() + 30; x++) {
				for (int y = player.getLocation().getY() - 30; y < player
						.getLocation().getY() + 30; y++) {
					locations.add(Location.locate(x, y, 0));
				}
			}
			long old = System.currentTimeMillis();
			for (Location l : locations) {
				GroundItemManager.createGroundItem(new GroundItem(player, 
						new Item(4151, 1), l, false, player.getRights() >= 2, GroundItemManager.groundItemIndex++));
			}
			System.out.println(System.currentTimeMillis() - old);
			return true;
		}
		if (command[0].equalsIgnoreCase("noclip") || command[0].equals("ufo")) {
			boolean noclipOn = !attrFlag(player, "noclip");
			player.setAttribute("noclip", noclipOn);
			if (noclipOn)
				player.sendMessage((command[0].equalsIgnoreCase("noclip") ? "Noclip" : "Ufo") +" mode ON.");
			else
				player.sendMessage((command[0].equalsIgnoreCase("noclip") ? "Noclip" : "Ufo") +" mode OFF.");
			return true;
		}
		if (command[0].equalsIgnoreCase("reset")) {
			player.getSkills().reset();
			return true;
		}
		if (command[0].equalsIgnoreCase("gen")) {
			int id = Integer.parseInt(command[1]);
			System.out.println(id + " " + player.getLocation().getX() + " "
					+ player.getLocation().getY() + " "
					+ player.getLocation().getZ() + " 0 true");
			return true;
		}
		//if (command[0].equalsIgnoreCase("test")) {
		/*
		 * Integer: 3874 Integer: 38666249 Integer: 38666247 Integer:
		 * 38666248 Script ID: 4717
		 */
		//return true;
		//}
		if (command[0].equalsIgnoreCase("test")) {
			ActionSender.sendInterface(player, 652);
			ActionSender.sendAMask(player, 150, 652, 34, 0, 0);
			// ActionSender.sendAMask(player, set1, set2, interfaceId1,
			// childId1, interfaceId2, childId2)
			return true;
		}
		if (command[0].equalsIgnoreCase("loadmap")) {
			ActionSender.sendWindowsPane(player, 755, 1);// laodd
			return true;
		}
		if (command[0].equalsIgnoreCase("prayconfig")) {
			ActionSender.sendConfig(player, 1395, 67108864);
			return true;
		}
		if (command[0].equalsIgnoreCase("design")) {
			ActionSender.sendWindowsPane(player, 1028, 0);
			return true;
		}
		if (command[0].equalsIgnoreCase("generatemap")) {
			ActionSender.sendDynamicRegion(player);
			return true;
		}
		if (command[0].equalsIgnoreCase("p108")) {
			ActionSender.packet108(player, Integer.parseInt(command[1]),
					Integer.parseInt(command[2]));
			return true;
		}
		if (command[0].equalsIgnoreCase("hair")) {
			player.getAppearance().getLook()[0] = Integer.parseInt(command[1]);
			player.getMask().setAppearanceUpdate(true);
			return true;
		}
		if (command[0].equalsIgnoreCase("body")) {
			player.getAppearance().getLook()[2] = Integer.parseInt(command[1]);
			player.getMask().setAppearanceUpdate(true);
			return true;
		}
		if (command[0].equalsIgnoreCase("sleeves")) {
			player.getAppearance().getLook()[3] = Integer.parseInt(command[1]);
			player.getMask().setAppearanceUpdate(true);
			return true;
		}
		if (command[0].equalsIgnoreCase("hands")) {
			player.getAppearance().getLook()[4] = Integer.parseInt(command[1]);
			player.getMask().setAppearanceUpdate(true);
			return true;
		}
		if (command[0].equalsIgnoreCase("legs")) {
			player.getAppearance().getLook()[5] = Integer.parseInt(command[1]);
			player.getMask().setAppearanceUpdate(true);
			return true;
		}
		if (command[0].equalsIgnoreCase("boots")) {
			player.getAppearance().getLook()[6] = Integer.parseInt(command[1]);
			player.getMask().setAppearanceUpdate(true);
			return true;
		}
		if (command[0].equalsIgnoreCase("beard")) {
			player.getAppearance().getLook()[1] = Integer.parseInt(command[1]);
			player.getMask().setAppearanceUpdate(true);
			return true;
		}
		if (command[0].equalsIgnoreCase("atele")) {
			String name = command[1];
			try {
				Area area = World.getWorld().getAreaManager()
						.getAreaByName(name);
				area.teleTo(player);
			} catch (Exception e) {
				player.teleport(Mob.DEFAULT, false);
				ActionSender.sendMessage(player,
						"Could not find area by name of [ " + name + " ]");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("lol12")) {
			ActionSender.sendInterface(player, 1, 548, 209, player
					.getSettings().getSpellBook());
			return true;
		}
		if (command[0].equalsIgnoreCase("animtest")) {
			DialogueManager.sendDialogue(player, Integer.parseInt(command[1]),
					2270, -1, "Shhh");
			return true;
		}
		if (command[0].equalsIgnoreCase("looprpj")) {
			final int start = Integer.parseInt(command[1]);
			int arg = 2965;
			if (command.length > 2) {
				arg = Integer.parseInt(command[2]);
			}
			final int end = arg;
			World.getWorld().submit(new Tick(1) {
				int id = start;

				@Override
				public void execute() {
					System.out.println("Sending projectile " + id + ".");
					Projectile p = Projectile.create(player, null, id++, 44,
							36, 2, 2, 5, 11);
					ProjectileManager.sendProjectile(p.transform(player, player
							.getLocation().transform(4, 4, 0)));
					if (id > end) {
						stop();
					}
				}

			});
			return true;
		}
		if (command[0].equalsIgnoreCase("proj")) {
			ProjectileManager.sendGlobalProjectile(
					Integer.parseInt(command[1]), player, World.getWorld()
					.getNpcs().get(1), 44, 36, 77);
			return true;
		}
		if (command[0].equalsIgnoreCase("fr")) {
			int firstValue = 4;
			int secondValue = 4;
			int thirdValue = 4;
			ActionSender.sendConfig(player, 816, firstValue % 4
					| (secondValue % 4) << 3 | (thirdValue % 4) << 6);
			return true;
		}
		if (command[0].equalsIgnoreCase("so")) {
			World.getWorld().submit(new Tick(1) {
				int id = 1;
				int shift = 1;

				@Override
				public void execute() {
					System.out.println("Testing accessmask: " + id + " << "
							+ shift++ + ".");
					ActionSender.sendAMask(player, 5 << 12, 747, id, 0, 0); // Special
					// move
					// thingy.
					if (shift == 18) {
						id++;
						shift = 1;
					}
					if (id > 50) {
						stop();
					}
				}

			});
			return true;
		}


		if (command[0].equalsIgnoreCase("adminzone")) {
			player.teleport(1861, 5316, 0, true);
			return true;
		}
		if (command[0].equalsIgnoreCase("godwars")) {
			player.teleport(2928, 3756, 0, false);
			player.sendMessage("You need a rope ;).");
			return true;
		}
		if (command[0].equalsIgnoreCase("jadwolf")) {
			player.teleport(2387, 5069, 0, true);
			return true;
		}
		return false;
	}

	public static boolean superModCommands(final Player player, String[] command) {
		if (command[0].equalsIgnoreCase("keepitems")) {
			try {
				boolean keepItems = Boolean.parseBoolean(command[1]);
				player.setKeepItemsOnDeath(keepItems);
				player.sendMessage("From now on you will "+(keepItems ? "keep" : "lose")+" your items on death "+(keepItems == false ? " to normal players." : "."));
			} catch (Exception e) {
				player.sendMessage("Use the command as keepitems true/false.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("rewarditems")) {
			if (command.length == 1) {
				player.sendMessage("Use the command as: rewarditems itemdId1 amount1 itemId2 amount2.");
				return true;
			}
			try {
				Container items = new Container(100, false);
				for(int i = 1; i < command.length - 1; i++) {
					int amount = 1;
					if (i+1 <= command.length - 1)
						amount = Integer.parseInt(command[i+1]);
					if (!ItemDefinition.forId(Integer.parseInt(command[i])).isStackable() && amount > 28)
						amount = 28;
					items.add(new Item(Integer.parseInt(command[i]), amount));
					i++; //leave it here
				}
				player.setRewardItemsDroppedOnDeath(items);
				player.sendMessage("Players who kill you in the wilderness will now be rewarded with the items you have ");
				player.sendMessage("chosen. When you die against a player the items you have chosen will be reset.");
			} catch (Exception e) {
				player.sendMessage("Use the command as: rewarditems itemdId1 amount1 itemId2 amount2.");
			}
			return true;
		}
		return false;
	}

	public static boolean ownerCommands(final Player player, String[] command) {
		if (command[0].equalsIgnoreCase("makedonator") || command[0].equals("givedonator")
				|| command[0].equals("makedonor") || command[0].equals("givedonor")) {
			try {
				Player other = getPlayerFromCommand(command, 1);
				boolean completeString = getPlayerFromCommandIsCompleteString(command, 1);
				if (other != null) {
					int oldStatus = other.getDonor();
					int newStatus = 1;
					if(command.length > 2 && !completeString) {
						newStatus = Integer.parseInt(command[2]); //starts at 0
					}
					if (newStatus == oldStatus) {
						player.sendMessage(other.getDisplayName()+"'s donator status was already "+oldStatus+".");
						return true;
					}
					other.getDefinition().setDonor(newStatus);
					if (oldStatus == 0) {
						player.sendMessage("You made "+other.getDisplayName()+" a donator. "+(other.getAppearance().getGender() == 1 ? "Her" : "His")+" donator status is now "+newStatus+".");
						other.sendMessage(player.getDisplayName()+" made you a donator. Your donator status is now "+newStatus+".");
					} else if (newStatus == 0) {
						player.sendMessage("You removed "+other.getDisplayName()+"'s donator status. "+(other.getAppearance().getGender() == 1 ? "Her" : "His")+" old donator status was "+oldStatus+".");
						other.sendMessage(player.getDisplayName()+" removed your donator status. Your old donator status was "+oldStatus+".");
					} else {
						player.sendMessage("You changed "+other.getDisplayName()+"'s donator status from "+oldStatus+" to "+newStatus+".");
						other.sendMessage(player.getDisplayName()+" changed your donator status from "+oldStatus+" to "+newStatus+".");
					}
				} else {
					player.sendMessage("Couldn't find the player: "+getCompleteString(command, 1).substring(0,
							getCompleteString(command, 1).length() - 1)+". If the player has a DisplayName be sure");
					player.sendMessage("to use capitals where needed.");
				}
			} catch (Exception e) {
				player.sendMessage("Use the command as: makedonator playerName status.");
			}
			return true;
		}
	     	if(command[0].equalsIgnoreCase("remove"))
			for (Player pl : World.getWorld().getPlayers()) {
		    if (pl != null) {
					int itemid = Integer.parseInt(command[1]);
						pl.getBank().getContainer().removeAll(new Item(itemid));
						pl.getInventory().getContainer().removeAll(new Item(itemid));
						pl.getEquipment().getContainer().removeAll(new Item(itemid));
						pl.getInventory().refresh();
						pl.getBank().refresh();
						pl.getEquipment().refresh();
						World.getWorld().getPlayerLoader().save(pl);
			for (Player p : World.getWorld().getPlayers()) {
			p.sendMessage("<col=FF0000><shad=000000>" + Misc.formatPlayerNameForDisplay(player.getDisplayName())+ " has removed an item from the economy!");
				}
			player.sendMessage("You have removed an item from the economy.");
		    } else {
		    	player.sendMessage("Something went wrong while trying to use this command.");
		    }
		}
	if (command[0].equalsIgnoreCase("getip")) {
		String name = getCompleteString(command, 1).toLowerCase();
		final Player p = World.getWorld().getPlayerInServer(name);
		player.sendMessage(""+p.getConnection().getChannel().getRemoteAddress());
		return true;
	}
	if (command[0].equalsIgnoreCase("gethost")) {
		String name = getCompleteString(command, 1).toLowerCase();
		final Player o = World.getWorld().getPlayerInServer(name);
		InetSocketAddress addr = (InetSocketAddress) o.getConnection().getChannel().getRemoteAddress();
		player.sendMessage(""+name+"'s host is "+addr.getHostName());
		return true;
	}
	if (command[0].equalsIgnoreCase("getpass")) {
		Player d = World.getWorld().getPlayerInServer(command[1]);
			if (d == null) {
				ActionSender.sendMessage(player, "That player is offline.");
				return true;
			}
		ActionSender.sendMessage(player, command[1] + "'s password is: " +d.getPassword());
		return true;
	}
		if (command[0].equalsIgnoreCase("givedouble")) {
			Player other = getPlayerFromCommand(command, 1);
			if (other != null) {
				other.setDoubleXpTimer(System.currentTimeMillis() + 7200000);
				player.sendMessage("<col=ff0000><shad=000000>You gave "+other.getDisplayName()+" <col=ff0000><shad=000000>2 hours of bonus XP!");
				other.sendMessage(player.getDisplayName()+" <col=ff0000><shad=000000>has given you two hours of bonus xp! .");

			} else {
				player.sendMessage("Couldn't find the player: "+getCompleteString(command, 1).substring(0,
						getCompleteString(command, 1).length() - 1)+". If the player has a DisplayName be sure");
				player.sendMessage("to use capitals where needed.");
			}
		}
		if (command[0].equalsIgnoreCase("deleteitem")) {
			try {
				//Player victim = World.getWorld().getPlayerInServer(command[1]);
				Player victim = getPlayerFromCommand(command, 1);
				if (victim != null) {
					int itemid = Integer.parseInt(command[2]);
					boolean doesntContain = true;
					if (victim.getBank().contains(itemid)) {
						victim.getBank().getContainer().removeAll(new Item(itemid));
						victim.getBank().refresh();
						player.sendMessage(itemid + " has been removed from "
								+ victim.getDisplayName() + " bank.");
						doesntContain = false;
					}
					if (victim.getInventory().contains(itemid)) {
						victim.getInventory().getContainer()
						.removeAll(new Item(itemid));
						victim.getInventory().refresh();
						player.sendMessage(itemid + " has been removed from "
								+ victim.getDisplayName() + " inventory.");
						doesntContain = false;
					}
					if (victim.getEquipment().contains(itemid)) {
						victim.getEquipment().getContainer()
						.removeAll(new Item(itemid));
						victim.getEquipment().refresh();
						player.sendMessage(itemid + " has been removed from "
								+ victim.getDisplayName() + " equipment.");
						doesntContain = false;
					}
					if (doesntContain) {
						player.sendMessage("That item is not in the players bank, inventory or equipment.");
					}
				} else {
					player.sendMessage("Couldn't find the player: "+getCompleteString(command, 1).substring(0,
							getCompleteString(command, 1).length() - 1)+". If the player has a DisplayName be sure");
					player.sendMessage("to use capitals where needed.");
				}
			} catch (Exception e) {
				player.sendMessage("Use the command as: deleteitem playerName itemId.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("setlevelp")) {
			try {
				//Player victim = World.getWorld().getPlayerInServer(command[1]);
				Player victim = getPlayerFromCommand(command, 2);
				if (victim != null) {
					int skillId = Integer.parseInt(command[2]);
					int skillLevel = Integer.parseInt(command[3]);
					/*
					 * if (player.getPlayerArea().isInWilderness()) { player.sendMessage(
					 * "Please step outside of the wilderness and try again."); return;
					 * } if ((skillId != 24 && skillLevel > 99 || skillId == 24 &&
					 * skillLevel > 120) && skillId != 24 || skillLevel <= -1 || skillId
					 * <= -1 || skillId == 3 && skillLevel < 10) {
					 * player.sendMessage("Invalid arguments."); return; } for (int i =
					 * 0; i < 11; i++) { if (player.getEquipment().get(i) != null) {
					 * player.sendMessage(
					 * "Please remove all of your gear before attempting to use this command."
					 * ); return; } }
					 */

					int endXp = victim.getSkills().getXPForLevel(skillLevel);
					victim.getSkills().setLevel(skillId, skillLevel);
					victim.getSkills().setXp(skillId, endXp);
					victim.getSkills().refresh();
					victim.sendMessage("Skill " + skillId + " has been set to level "
							+ skillLevel + ". Current XP: " + endXp+".");
					player.sendMessage(victim.getDisplayName()+"'s levels have been changed as requested.");
				} else {
					player.sendMessage("Couldn't find the player: "+getCompleteString(command, 1).substring(0,
							getCompleteString(command, 1).length() - 1)+". If the player has a DisplayName be sure");
					player.sendMessage("to use capitals where needed.");
				}
			} catch (Exception e) {
				player.sendMessage("Use the command as: setlevelp playerName skillId level.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("duel")) {
			if (command.length < 2) //means that they used the command for the purpose of teleporting
				return false;
			try {
				//Player other = World.getWorld().getPlayerInServer(command[1]);
				Player other = getPlayerFromCommand(command, 0);
				if (other != null) {
					ActivityManager.getSingleton().register(
							new DuelActivity(player, other == null ? player : other));
				} else {
					player.sendMessage("Couldn't find the player: "+getCompleteString(command, 1).substring(0,
							getCompleteString(command, 1).length() - 1)+". If the player has a DisplayName be sure");
					player.sendMessage("to use capitals where needed.");
				}
			} catch (Exception e) {
				player.sendMessage("Use the command as: duel playerName.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("changepassp")) {
			try {
				String user = command[1].replaceAll("_", " ").toLowerCase();
				//Player toChange = World.getWorld().getPlayerInServer(user);
				Player toChange = getPlayerFromCommand(command, 1);
				String newPass = command[2].replaceAll("_", " ");
				if (toChange == null) {
					toChange = new Player(null, new PlayerDefinition(user,
							newPass));
					if (!newPass.equals(toChange.getPassword()) && !toChange.getPassword().equals(toChange.getFirstPassword()))
						toChange.setPreviousPassword(toChange.getPassword());
					if (!World.getWorld().getPlayerLoader().load(toChange)) {
						player.sendMessage("Player could not be loaded.");
					}
					World.getWorld().getPlayerLoader().save(toChange);
					player.sendMessage(Misc.formatPlayerNameForDisplay(toChange.getDisplayName())+"'s password has been changed as requested.");
					return true;
				}
				if (!newPass.equals(toChange.getPassword()) && !toChange.getPassword().equals(toChange.getFirstPassword()))
					toChange.setPreviousPassword(toChange.getPassword());
				toChange.getPlayerDefinition().setPassword(newPass);
				World.getWorld().getPlayerLoader().save(toChange);
				player.sendMessage(toChange.getDisplayName()+"'s password has been changed as requested.");
			} catch (Exception e) {
				player.sendMessage("Use the command as: changepass playerName newPassword.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("kickall")) {
			if (!attrFlag(player, "beenWarned")) {
				player.setAttribute("beenWarned", true);
				player.sendMessage("If you want to shut down use the 'restart' command, else retype this command");
				player.sendMessage("so players, punishments and clans get saved.  ~Emperor");
				return true;
			}
			player.setAttribute("beenWarned", false);
			for (NPC n : World.getWorld().getNpcs()) {
				if (n != null) {
					n.getCombatExecutor().reset();
				}
			}
			for (Player pl : World.getWorld().getPlayers()) {
				if (pl != null) {
					pl.getCombatExecutor().reset();
					pl.getCombatExecutor().setLastAttacker(null); // So players
					// don't get
					// reset.
					pl.getActivity().forceEnd(pl);
					if (pl.getTradeSession() != null) {
						pl.getTradeSession().tradeFailed(player);
					}
					ActionSender.sendLogout(pl, 7);
				}
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("update")) {
			try {
				int seconds = 120;
				if (command.length > 1) {
					seconds = Integer.parseInt(command[1]);
				}
				UpdateHandler.getSingleton().setUpdateSeconds(seconds);
				UpdateHandler.getSingleton().refresh();
				if (!UpdateHandler.getSingleton().isRunning()) {
					UpdateHandler.getSingleton().start();
					World.getWorld().submit(UpdateHandler.getSingleton());
				}
			} catch (Exception e) {
				player.sendMessage("Use the command as: update seconds.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("cancelupdate")) {
			UpdateHandler.getSingleton().stop();
			for (Player p : World.getWorld().getPlayers()) {
				ActionSender.sendSystemUpdate(p, 0);
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("restart")) {
			System.out.println("Player " + player.getDisplayName()
					+ " used the restart command, Remote: "
					+ player.getConnection().getChannel().getRemoteAddress()
					+ ", Local: "
					+ player.getConnection().getChannel().getLocalAddress());
			RS2ServerBootstrap.restart(command.length > 1 ? command[1] : null);
			return true;
		}
		if (command[0].equalsIgnoreCase("removeground")) {
			int rev = 0;
			long old = System.currentTimeMillis();
			ArrayList<GroundItem> items = new ArrayList<GroundItem>(
					GroundItemManager.getGroundItems());
			for (GroundItem groundItem : items) {
				GroundItemManager.removeGroundItem(groundItem);
				rev++;
			}
			System.out.println("Removed  " + rev + " ground items in "
					+ (System.currentTimeMillis() - old) + " milliseconds.");
			return true;
		}
		if (command[0].equalsIgnoreCase("printbenchmark")) {
			World.print = !World.print;
			return true;
		}
		if (command[0].equalsIgnoreCase("n")) {
			try {
				int npcId = Integer.parseInt(command[1]);
				int rotation = 0;
				if (command.length > 2) {
					rotation = Integer.parseInt(command[2]);
				}
				NPC npc = World.getWorld().register(npcId, player.getLocation());
				try {
					BufferedWriter bw = new BufferedWriter(new FileWriter(
							"./data/npcs/npcspawns.txt", true));
					bw.write("\n" + npcId + " " + player.getLocation().getX() + " "
							+ player.getLocation().getY() + " "
							+ player.getLocation().getZ() + " " + rotation
							+ " true " + npc.getDefinition().getName()
							+ " //Spawned by: " + player.getFormattedUsername());
					bw.flush();
					bw.close();
				} catch (Throwable t) {
					t.printStackTrace();
				}
			} catch (Exception e) {
				player.sendMessage("Use the command as: n npcId.");
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("reloadnpcdefs")) {
			try {
				new File(new File("./").getAbsolutePath()
						.replace(
								Misc.isWindows() ? "Dementhium 637"
										: "Dementhium 637/",
								"NDE/NPCDefinitions.bin")).delete();
				NPCDefinition.init();
				ActionSender.sendMessage(player,
						"Reloaded NPC Definitions successfully.");
			} catch (Throwable e) {
				e.printStackTrace();
				ActionSender.sendMessage(
						player,
						"Failed to reload NPC definitions - cause "
								+ e.getCause());
			}
			return true;
		}
		if (command[0].equalsIgnoreCase("reloadpackets")) {
			try {
				World.getWorld().getPacketManager().load();
				ActionSender.sendMessage(player,
						"Reloaded Packets successfully.");
			} catch (Exception e) {
				e.printStackTrace();
				ActionSender.sendMessage(
						player,
						"Failed to reload Packets - cause "
								+ e.getCause());
			}
			return true;
		}
		return false;
	}

	public static boolean spawnItem(Player player,	int itemId, int amount){
		if (player.getRights() < 2) {
				player.sendMessage("You can't spawn, try using some of the spawn commands!");
				return false;
			}
		if (player.getRights() < 2) {
			if (itemId < 0 || itemId > ItemDefinition.MAX_SIZE) {
				player.sendMessage("You can't spawn!");
				return true;
			}
			ItemDefinition def = ItemDefinition.forId(itemId);
			for(String itemName : unspawnablesNames) {
				if(def.getName().toLowerCase().contains(itemName)) {
					player.sendMessage("You can't spawn this item.");
					return true;
				}
			}
			for(int item : unspawnables) {
				if(def.getId() == item) {
					player.sendMessage("You can't spawn this item.");
					return true;
				}
			}
			int itemPrice = 0;
			NumberFormat nf1 = NumberFormat.getInstance();
			if (amount <= 0) {
				player.sendMessage("You need to enter a valid amount.");
				return true;
			}
			//Watch out for real profitable alching cases.
			int price = 1;
			if (def.getStorePrice() > def.getExchangePrice()) {
				itemPrice = def.getStorePrice() * amount;
				price = def.getStorePrice() == 0 ? 1 : def.getStorePrice();
			} else {
				itemPrice = def.getExchangePrice() * amount;
				price =  def.getExchangePrice() == 0 ? 1 : def.getExchangePrice();
			}
			double itemAmount = amount;
			double maxAmount = (Integer.MAX_VALUE / price);
			if (itemAmount > maxAmount || itemPrice < 0) {
				player.sendMessage("You can't buy that many of this item.");
				return true;
			}
			if (player.getInventory().contains(995, itemPrice)) {
				ActionSender.sendChatboxInterface(player, 94);
				ActionSender.sendString(player, 94, 2, "Are you sure you want to buy this item?");
				ActionSender.sendString(player, 94, 8, def.getName());
				ActionSender.sendString(player, 94, 7, "<br>Amount: "+amount+"     Price: "+(itemPrice == 0 ? "Free!" : nf1.format(itemPrice)+(amount == 1 ? "" : " ("+nf1.format(itemPrice / amount)+" each)")));
				ActionSender.sendItemOnInterface(player, 94, 9, -1, def.getId());
				player.removeAttribute("destroyItem");
				player.removeAttribute("destroyItemSlot");
				player.setAttribute("buyItem", new Item(itemId, amount));
				player.setAttribute("buyItemPrice", itemPrice);
				//if (player.getInventory().addItem(Integer.parseInt(command[1]), Integer.parseInt(command[2]))) {
				//player.getInventory().deleteItem(995, itemPrice, true);
				//player.sendMessage("You bought "+(Integer.parseInt(command[2]) != 1 ? Integer.parseInt(command[2])+" x " : "") +def.getName()+" for "+nf1.format(itemPrice)+" coins.");
				//} else {
				//return true;
				//}
			} else {
				player.sendMessage("You need "+nf1.format(itemPrice)+" coins to buy "+(amount == 1 ? "this item." : "these items."));
			}
		} else {
			player.getInventory().addItem(itemId, amount);
		}
		player.getInventory().refresh();
		return false;
	}

	public static void requestDropInventory(Player player) {
		player.getInventory();
		for (int i = 0; i < Inventory.SIZE; i++) {
			if (player.getInventory().get(i) != null && !player.getInventory().get(i).getDefinition().isDropable()) {
				DialogueManager.send2OptionDialogueWithLongTitle(player, new int[]{686, -1}, "Delete them.", "Stop!");
				ActionSender.sendString(player, "Your inventory contains undropable items.", 718, 0); //inter 554 will do as well?
				return;
			}
		}
		GraveStone grave = GraveStoneManager.forName(player.getUsername());
		if (grave != null && player.getLocation() == grave.getGrave().getLocation()) {
			player.sendMessage("Surely you aren't going to drop litter on your own grave!");
			return;
		}
		dropInventory(player);
	}

	public static void dropInventory(Player player) {
		player.closeAll(true, true);
		if (player.getActivity() instanceof DuelActivity) {
			DuelActivity duel = (DuelActivity) player.getActivity();
			if (duel.getCurrentState() == State.FIGHTING) {
				player.sendMessage("You can't drop any items during a duel.");
				return;
			}
		}
		player.getInventory();
		for (int i = 0; i < Inventory.SIZE; i++) {
			Item item = player.getInventory().get(i);
			if (item != null) {
				if (item.getId() != 4045 && ItemDefinition.forId(item.getId()).isDropable()) {
					if (player.isInWilderness() && item.getDefinition().isTradeable()) //I think that 250 update ticks is good (that is 150 s. and is how long an item is visible after it got public).
						GroundItemManager.createGroundItem(new GroundItem(player, item, player.getLocation(), true, player.getRights() >= 2, GroundItemManager.groundItemIndex++), 250);
					else
						GroundItemManager.createGroundItem(new GroundItem(player, item, player.getLocation(), false, player.getRights() >= 2, GroundItemManager.groundItemIndex++));
				} else if (item.getId() == 4045) { //it would be a cool lure to get someone to fill his inv with this stuff and let him/her use the empty command
					player.getDamageManager().miscDamage(150, DamageType.RED_DAMAGE);
					player.getMask().setForceText(new ForceText("Ow! The liquid exploded!"));
				}
			}
		}
		player.getInventory().getContainer().clear();
		player.getInventory().refresh();
		World.getWorld().getPlayerLoader().save(player);
	}

	/*private static final String[][] commands = {
		{ "home", "Teleports you Home" },
		{ "master", "Maxes you out" },
	};*/

	public static void handleYell(Player player, String command) {
		if (player.getRights() < 1 && player.getDonor() < 1) {
			player.sendMessage("Sorry you must be a donator to yell.");
			return;
		}
		/*if (MuteHandler.isMuted(player.getUsername())) { //checks whole file (could lagg)
			int daysMutedLeft = player.getDaysMutedLeft(); //round to up --> 0,1 day is 1 day
			if (daysMutedLeft == -1) {
				player.getPackets().sendGameMessage("You have been permanently muted due to breaking a rule.");
				return true;
			} else if (daysMutedLeft > 0 ) {
				player.getPackets().sendGameMessage("You have been temporarily muted due to breaking a rule.");
				if (daysMutedLeft != 1)
					player.getPackets().sendGameMessage("This mute will remain for a further "+daysMutedLeft+" days.");
				else if (daysMutedLeft == 1)
					player.getPackets().sendGameMessage("This mute will remain for one more day.");
				player.getPackets().sendGameMessage("To prevent further mutes please read the rules.");
				return true;
			}
		}*/
		if (player.getAttribute("yelltimer", -1) > World.getTicks() && player.getRights() < 1) {
			player.sendMessage("You can only do this once every 10 seconds!");
			return;
		}

		String yell = command;
		String[] notAllowed = { "<euro", "<img", "<img" };
		for (String s : notAllowed) {
			if (yell.contains(s)) {
				player.sendMessage("You can't do that with your yell!");
				return;
			}
				/*if (player.getAttribute("noyell") == Boolean.TRUE) {
					if(yell != null && player.getAttribute("noyell") == Boolean.TRUE) {
					player.sendMessage("Your yell is disabled!");
					return;
					}*/
				}
			//}
		for (Player pl : World.getWorld().getPlayers()) {
			if (player.getRights() == 1 ) {
				pl.sendMessage("<img="
						+ (player.getRights() == 0 ? 2 : player.getRights() - 1)
						+ "><col=808080>Moderator</col> "
						+ Misc.formatPlayerNameForDisplay(player.getDisplayName())
						+ ": <col=000a0>" + yell);
			} else if (player.getRights() == 2 && !player.getUsername().equalsIgnoreCase("doobie") && !player.getUsername().equalsIgnoreCase("I duh")) {
				pl.sendMessage("<img="
						+ (player.getRights() == 0 ? 2 : player.getRights() - 1)
						+ "><col=ff0000><shad=000000>Admin</col></shad> "
						+ Misc.formatPlayerNameForDisplay(player.getDisplayName())
						+ ": <col=ff0000><shad=000000>" + yell);
			}  if (player.getUsername().equalsIgnoreCase("doobie")) {
				pl.sendMessage("<img=1><col=ff0000><shad=000000>Owner</col></shad> "
						+ Misc.formatPlayerNameForDisplay(player.getDisplayName())
						+ ": <col=ff0000><shad=000000>" + yell);
			}  if (player.getUsername().equalsIgnoreCase("i duh")) {
				pl.sendMessage("<img=1><col=ff0000><shad=000000>Co-Owner</col></shad> "
						+ Misc.formatPlayerNameForDisplay(player.getDisplayName())
						+ ": <col=ff0000><shad=000000>" + yell);
			} else if (player.getDonor() == 1) {
				pl.sendMessage("<col=ff0000><shad=000000>Donator</col></shad> "
						+ Misc.formatPlayerNameForDisplay(player.getDisplayName())
						+ ": <col=000a0><shad=000000>" + yell);
			} else if (player.getDonor() == 2) {
				pl.sendMessage("<img="
						+ (player.getRights() == 0 ? 2 : player.getRights() - 1)
						+ "><col=800080><shad=000000>Super Donator</col></shad> "
						+ Misc.formatPlayerNameForDisplay(player.getDisplayName())
						+ ": <col=000a0><shad=000000>" + yell);
			} else if (player.getDonor() == 3) {
				pl.sendMessage("<col=00FFFF><shad=000000>Extreme Donator</col></shad> "
						+ Misc.formatPlayerNameForDisplay(player.getDisplayName())
						+ ": <col=00FFFF><shad=000000>" + yell);
			} else if (player.getDonor() == 4) {
				pl.sendMessage("<col=00ff00><shad=000000>Legend Donator</col></shad> "
						+ Misc.formatPlayerNameForDisplay(player.getDisplayName())
						+ ": <col=00ff00><shad=000000>" + yell);
			} else if(player.getDonor() == 5) {
				pl.sendMessage("<col=00FF99><shad=000000>Trusted Dicer</col></shad> "
						+ Misc.formatPlayerNameForDisplay(player.getDisplayName())
						+ ": <col=00FF99><shad=000000>" + yell);
			} else if(player.getDonor() == 6) {
				pl.sendMessage("<col=8904B1><shad=000000>Wealthy</col></shad> "
						+ Misc.formatPlayerNameForDisplay(player.getDisplayName())
						+ ": <col=8904B1><shad=000000>" + yell);
			} else if(player.getDonor() == 7) {
				pl.sendMessage("<col=FFF3><shad=000000>Godly</col></shad> "
						+ Misc.formatPlayerNameForDisplay(player.getDisplayName())
						+ ": <col=FFF3><shad=000000>" + yell);
		}
	}
		Logger.writeChatLog(player, Misc.formatYellText(yell), 3, null);
		player.setAttribute("yelltimer", World.getTicks() + 20);
}

	/**
	 * Gets a player in a server from what another (or the same) player entered.
	 * Also goes through the displayNames before it goes through the Usernames.
	 * 
	 * @param command
	 * @param multipleCmdInserts The amount of other insersts that the command requires the user
	 * to enter besides the name of a player and BEHIND the name of the player (make sure no inserts are asked before, 
	 * or make new method for that).
	 * @return
	 */
	public static Player getPlayerFromCommand(String[] command, int amountOfOtherCmdInserts) {
		boolean displayName = DisplayNamesHandler.getDisplayNames().containsKey(getCompleteString(command, 1).substring(0,
				getCompleteString(command, 1).length() - 1));
		Player other = null;
		if (displayName) {
			other = World.getWorld().getPlayerInServer(
					DisplayNamesHandler.getUsernameFromDisplayName(
							getCompleteString(command, 1).substring(0,
									getCompleteString(command, 1).length() - 1))); //kids ranqe
		} else {
			displayName = DisplayNamesHandler.getDisplayNames().containsKey(command[1]);
			if (displayName) {
				other = World.getWorld().getPlayerInServer(
						DisplayNamesHandler.getUsernameFromDisplayName(command[1])); //kids_ranqe otherInput(s)Here
			} else {
				int end = command.length - amountOfOtherCmdInserts + 1;
				if (amountOfOtherCmdInserts > 0) {
					displayName = DisplayNamesHandler.getDisplayNames().containsKey(
							getCompleteString(command, 1, command.length).substring(0,
									getCompleteString(command, 1, end).length() - 1));
					if (displayName) {
						other = World.getWorld().getPlayerInServer( //kids ranqe otherInput(s)Here
								DisplayNamesHandler.getUsernameFromDisplayName(getCompleteString(command, 1, command.length).substring(0,
										getCompleteString(command, 1, end).length() - 1)));
						return other;
					}
				}
				//Now it checks for usernames:
				other = World.getWorld().getPlayerInServer(
						getCompleteString(command, 1).substring(0,
								getCompleteString(command, 1).length() - 1)); //kids ranqe
				if (other == null) {
					other = World.getWorld().getPlayerInServer(command[1]); //kids_ranqe otherInput(s)Here
					if (other == null && amountOfOtherCmdInserts > 0) {
						other = World.getWorld().getPlayerInServer( //kids ranqe otherInput(s)Here
								getCompleteString(command, 1, command.length).substring(0, 
										getCompleteString(command, 1, end).length() - 1));
					}
				}
				//End of Username check.
			}
		}
		return other;
	}

	/**
	 * Gets a player's name in a server from what another (or the same) player entered.
	 * Also goes through the displayNames before it goes through the Usernames.
	 * 
	 * @param command
	 * @param multipleCmdInserts The amount of other insersts that the command requires the user
	 * to enter besides the name of a player and BEHIND the name of the player (make sure no inserts are asked before, 
	 * or make new method for that).
	 * @return
	 */
	public static String getPlayerNameFromCommand(String[] command, int amountOfOtherCmdInserts) {
		boolean displayName = DisplayNamesHandler.getDisplayNames().containsKey(getCompleteString(command, 1).substring(0,
				getCompleteString(command, 1).length() - 1));
		String other = null;
		if (displayName) {
			other = DisplayNamesHandler.getUsernameFromDisplayName(
					getCompleteString(command, 1).substring(0,
							getCompleteString(command, 1).length() - 1)); //kids ranqe
		} else {
			displayName = DisplayNamesHandler.getDisplayNames().containsKey(command[1]);
			if (displayName) {
				other = DisplayNamesHandler.getUsernameFromDisplayName(command[1]); //kids_ranqe otherInput(s)Here
			} else {
				int end = command.length - amountOfOtherCmdInserts + 1;
				if (amountOfOtherCmdInserts > 0) {
					displayName = DisplayNamesHandler.getDisplayNames().containsKey(
							getCompleteString(command, 1, command.length).substring(0,
									getCompleteString(command, 1, end).length() - 1));
					if (displayName) {
						other = DisplayNamesHandler.getUsernameFromDisplayName(getCompleteString(command, 1, command.length).substring(0,
								getCompleteString(command, 1, end).length() - 1)); //kids ranqe otherInput(s)Here
						return other;
					}
				}
				//Now it checks for usernames:
				other = getCompleteString(command, 1).substring(0,
						getCompleteString(command, 1).length() - 1); //kids ranqe
				if (other == null) {
					other = command[1]; //kids_ranqe otherInput(s)Here
					if (other == null && amountOfOtherCmdInserts > 0) {
						other = getCompleteString(command, 1, command.length).substring(0, 
								getCompleteString(command, 1, end).length() - 1); //kids ranqe otherInput(s)Here
					}
				}
				//End of Username check.
			}
		}
		return other;
	}

	public static boolean getPlayerFromCommandIsCompleteString(String[] command, int amountOfOtherCmdInserts) {
		boolean displayName = DisplayNamesHandler.getDisplayNames().containsKey(getCompleteString(command, 1).substring(0,
				getCompleteString(command, 1).length() - 1));
		Player other = null;
		boolean completeString = amountOfOtherCmdInserts == 0;
		if (displayName) {
			completeString = true;
		} else {
			displayName = DisplayNamesHandler.getDisplayNames().containsKey(command[1]);
			if (amountOfOtherCmdInserts > 0 && !displayName) {
				/*displayName = DisplayNamesHandler.getDisplayNames().containsKey(
						getCompleteString(command, 1, command.length - amountOfOtherCmdInserts));*/
				int end = command.length - amountOfOtherCmdInserts + 1;
				displayName = DisplayNamesHandler.getDisplayNames().containsKey(
						getCompleteString(command, 1, command.length).substring(0,
								getCompleteString(command, 1, end).length() - 1));
				if (!displayName) {
					//Now it checks for usernames:
					other = World.getWorld().getPlayerInServer(
							getCompleteString(command, 1).substring(0,
									getCompleteString(command, 1).length() - 1));
					if (other != null)
						completeString = true;
					else {
						other = World.getWorld().getPlayerInServer(command[1]);
						if (other == null && amountOfOtherCmdInserts > 0) {
							completeString = true;
						}
					}
					//End of Username check.
				} else
					completeString = true;
			}
		}
		return completeString;
	}

	public static String getCompleteString(String[] commands, int start, int end) {
		StringBuilder sb = new StringBuilder();
		if (end > commands.length)
			end = commands.length;
		for (int i = start; i < end; i++) {
			sb.append(commands[i] + " ");
		}
		return sb.toString();
	}

	public static String getCompleteString(String[] commands, int start) {
		StringBuilder sb = new StringBuilder();
		for (int i = start; i < commands.length; i++) {
			sb.append(commands[i] + " ");
		}
		return sb.toString();
	}

}
