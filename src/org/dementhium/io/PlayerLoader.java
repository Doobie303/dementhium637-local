package org.dementhium.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.dementhium.model.World;
import org.dementhium.model.definition.PlayerDefinition;
import org.dementhium.model.player.Player;
import org.dementhium.mysql.ForumIntegration;
import org.dementhium.net.GameSession;
import org.dementhium.util.BufferUtils;
import org.dementhium.util.Constants;
import org.dementhium.util.Misc;
import org.dementhium.util.handlers.DisplayNamesHandler;
import org.dementhium.util.handlers.OffencesHandler;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;


/**
 * Class that handles player loading
 * @author 'Mystic Flow
 */
public final class PlayerLoader {
	
	//Zach, DO NOT EDIT THIS
	public static final String DIRECTORY = Misc.isVPS() ? "C:/Users/root/ecogames/" : "./data/games/";
	public static final String EXTENSION = ".bin";  
	
	public static final String[] invalidNames = {"mod ", "moderator", "admin", "  ", "fuck", "gay"};
	public static final String[] playerModerators = {"branden", "sam bever", ""};
	public static final String[] administrators = {"doobie", "julia", "i duh", "king kyle", "haha22", "test2", ""};
	public static final String[] superMods = { "doobie", "i duh", "julia", "king kyle", "test2"}; //moderators with the ability to transfer items to normal players.

	public static class PlayerLoadResult {

		private final Player player;
		private final int returnCode;

		public PlayerLoadResult(Player player, int returnCode) {
			this.returnCode = returnCode;
			this.player = player;
		}

		public Player getPlayer() {
			return player;
		}

		public int getReturnCode() {
			return returnCode;
		}

	}

	private static final Object fileLock = new Object();

	public PlayerLoadResult load(GameSession connection, PlayerDefinition def) {
		Player player = null;
		int code = 2;
		if(def.getName() == null || def.getPassword() == null) {
			code = Constants.INVALID_PASSWORD;
		}
		if (def.getPassword().length() < 2 || def.getName().length() > 12 || def.getName().startsWith(" ") || def.getName().endsWith(" ")) {
			code = 11; //Too weak password.
		}
		Player lobbyPlayer = World.getWorld().getPlayerOutOfLobby(def.getName());
		if (World.getWorld().isOnList(def.getName()) && lobbyPlayer == null
				&& (World.getWorld().getPlayers().size() > 0 || World.getWorld().getLobbyPlayers().size() > 0)) {
			code = Constants.ALREADY_ONLINE;
		}
		if(lobbyPlayer == null) {
			if(!Constants.CONNECTING_TO_FORUMS){
				boolean first = ForumIntegration.verify(def.getName(), def.getPassword());
				boolean second = ForumIntegration.verify(def.getName(), def.getPassword());
				if(code == 2 && !first && !second) {
					code = Constants.INVALID_PASSWORD;
				}
			} else {
				if(FileUtilities.exists(DIRECTORY + def.getName() + EXTENSION) && !loadPassword(def.getName(), def.getPassword())) {
					code = Constants.INVALID_PASSWORD;
				}
			}
		}
		/*if (code == Constants.ALREADY_ONLINE) {
			Player online = World.getWorld().getPlayerInServer(def.getName());
			if (online != null) {
				online.setConnection(connection);
				online.removeAttribute("xlogged");
				System.out.println("Re-connected after x-log.");
				return new PlayerLoadResult(online, code);
			}
		}*/
		if(code == 2) {
			def.setRights(getPreSetRights(def));
			player = new Player(connection, def);
		}
		if (player != null) {
			int count = 2;
			String ip = OffencesHandler.formatIp(player.getConnection().getChannel().getRemoteAddress().toString());
			for (Player pl : World.getWorld().getPlayers()) {
				if (pl != null && pl.getLastConnectIp().equals(ip)) {
					if (--count == 0 && pl.getRights() < 2 && def.getRights() < 2/*!ip.equals("127.0.0.1")*/) {
						return new PlayerLoadResult(null, 9); //Too many connects from your ip
					}
				}
			}
			if (World.getWorld().getOffencesHandler().isIpBanned(player)) {
				code = Constants.IP_BANNED;
				return new PlayerLoadResult(null, code);
			}
			if (World.getWorld().getOffencesHandler().isBanned(player)) {
				code = Constants.BANNED;
				return new PlayerLoadResult(null, code);
			}
			
			
			if(!FileUtilities.exists(DIRECTORY + def.getName() + EXTENSION)) {
				if (def.getRights() < 1) {
					for(String invalidName : invalidNames) {
						if(def.getName().contains(invalidName) || def.getName().equals("mod")) {
							code = Constants.INVALID_PASSWORD;
							return new PlayerLoadResult(null, code); //I forgot this line.. otherwise it would create the acc anyways
						}
					}
				}
				if (DisplayNamesHandler.getDisplayNames().containsKey(Misc.formatPlayerNameForDisplay(def.getName()))) {
					code = Constants.INVALID_PASSWORD;
					return new PlayerLoadResult(null, code);
				}
				if(code == 2) {
					player.setFirstPassword(def.getPassword());
					player.setAccountCreationIp(ip);
					player.setAccountCreationDate(System.currentTimeMillis());
				}
				save(player);
			}
		}
		return new PlayerLoadResult(player, code);
	}
	
	private int getPreSetRights(PlayerDefinition def) {
		if (def.getName().equalsIgnoreCase("mystic flow")) {
			return 2;
		}
		for(String admin : administrators) {
			if(def.getName().equalsIgnoreCase(admin)) {
				return 2;
			}
		}
		for(String pmod : playerModerators) {
			if(def.getName().equalsIgnoreCase(pmod)) {
				return 1;
			}
		}
		for(String smod : superMods) {
			if(def.getName().equalsIgnoreCase(smod)) {
				return 2;
			}
		}
		return 0;
	}

	private boolean loadPassword(String name, String password) {
		try {
			ByteBuffer data;
			synchronized(fileLock) {
				data = FileUtilities.fileBuffer(DIRECTORY + name + EXTENSION);
			}
			if(data != null) {
				//System.out.println(BufferUtils.readRS2String(data));
				return password.equalsIgnoreCase(BufferUtils.readRS2String(data));
			}
		} catch(Throwable e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public String loadPassword(String name) {
		try {
			ByteBuffer data;
			synchronized(fileLock) {
				data = FileUtilities.fileBuffer(DIRECTORY + name + EXTENSION);
			}
			if(data != null) {
				return BufferUtils.readRS2String(data);
			}
		} catch(Throwable e) {
			e.printStackTrace();
		}
		return null;
	}

	public boolean load(Player player) {
		try {
			ByteBuffer data;
			synchronized(fileLock) {
				data = FileUtilities.fileBuffer(DIRECTORY + player.getUsername() + EXTENSION);
			}
			if(data != null) {
				player.load(data);
				return true;
			}
		} catch(Throwable e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean save(Player player) {
		try {
			ChannelBuffer saveBuffer = ChannelBuffers.dynamicBuffer();
			player.save(saveBuffer);
			synchronized(fileLock) {
				FileUtilities.writeBufferToFile(DIRECTORY + player.getUsername() + EXTENSION, saveBuffer.toByteBuffer());
			}
			return true;
		} catch(Throwable e) {
			e.printStackTrace();
			String name = e.getCause() == null ? "NullCause" : e.getCause().toString();
			File file = new File("./data/saving errors/" + name + ".txt");
			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter(file));
				for (StackTraceElement s : e.getStackTrace()) {
					bw.append(s.toString() + "\n");
				}
				bw.append("\n Player " + player.getUsername());
				bw.flush();
				bw.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			return false;
		}
	}
}
