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
	public static final String[] administrators = {"doobie", "julia", "i duh", "king kyle", "haha22", "test2", "test123", ""};
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
    private final GamblerJournal gamblerJournal; private final DuelJournal duelJournal; private final String directory;
    public PlayerLoader(){this(java.nio.file.Paths.get(DIRECTORY));}
    public PlayerLoader(java.nio.file.Path storage){ directory=storage.toString()+java.io.File.separator;duelJournal=new DuelJournal(storage);gamblerJournal=new GamblerJournal(storage); }
    private static byte[] image(Player player) {
        ChannelBuffer buffer=ChannelBuffers.dynamicBuffer(); player.save(buffer);
        byte[] bytes=new byte[buffer.readableBytes()];buffer.getBytes(0,bytes);return bytes;
    }
    private void recoverTransactions() throws IOException {
        duelJournal.recover();gamblerJournal.recover();
    }
    /** Called only by the owning round; disk state is read under the same account-I/O lock. */
    public boolean commitGamble(Player p, int slot, long expectedHash, int item, int amount,
            org.dementhium.content.minigames.gambler.GamblerSession owner) {
        synchronized(World.getWorld()) { synchronized(fileLock) {
            org.dementhium.model.Container before=null;
            org.dementhium.content.minigames.gambler.GamblerRecovery.Record previous=null;
            boolean mutated=false;
            try {
                if(owner==null||!owner.authorizes(p,slot,expectedHash,item,amount)
                        || Boolean.TRUE.equals(p.getAttribute("saveSessionClosed"))
                        || World.getWorld().getPlayerInServer(p.getUsername())!=p)return false;
                recoverTransactions();
                org.dementhium.content.minigames.gambler.GamblerPolicy policy=org.dementhium.content.minigames.gambler.GamblerPolicy.INSTANCE;
                org.dementhium.model.Item selected=p.getInventory().get(slot);
                if(!policy.valid(item,amount)||!policy.eligible(selected)||selected.getId()!=item
                        ||selected.getHash()!=expectedHash||selected.getAmount()<amount)return false;
                previous=org.dementhium.content.minigames.gambler.GamblerRecovery.get(p);
                if(previous!=null && previous.pending>0)return false;
                java.util.Map<Integer,Long> stock=gamblerJournal.balances(policy.seeds);
                Long available=stock.get(item);
                if(available==null||available<amount){p.sendMessage("The Gambler cannot cover that wager. Try a smaller amount.");return false;}
                // Independent unbiased bounded rolls; no command/donor override.
                int a=GAMBLER_RANDOM.nextInt(100)+1,b=GAMBLER_RANDOM.nextInt(100)+1;
                int payout=org.dementhium.content.minigames.gambler.GamblerRecovery.payout(amount,a,b);
                stock.put(item,Math.subtractExact(Math.addExact(available,(long)amount),(long)payout));
                before=org.dementhium.content.activity.impl.duel.DuelRecovery.copy(p.getInventory().getContainer());
                org.dementhium.content.minigames.gambler.GamblerRecovery.Record result=
                    new org.dementhium.content.minigames.gambler.GamblerRecovery.Record(java.util.UUID.randomUUID(),item,amount,a,b,payout);
                mutated=true;
                if(selected.getAmount()==amount)p.getInventory().getContainer().set(slot,null);
                else {org.dementhium.model.Item remainder=new org.dementhium.model.Item(selected);remainder.setAmount(selected.getAmount()-amount);p.getInventory().getContainer().set(slot,remainder);}
                org.dementhium.content.minigames.gambler.GamblerRecovery.set(p,result);
                String receipt="round="+result.id+" account="+p.getUsername()+" policy=1 item="+item+" stake="+amount
                    +" playerRoll="+a+" houseRoll="+b+" returned="+payout+" houseStock="+stock.get(item)+" time="+System.currentTimeMillis()+"\n";
                gamblerJournal.commit(p.getUsername(),image(p),stock,result.id,receipt);
                return true;
            } catch(Exception failure) {
                if(mutated){org.dementhium.content.activity.impl.duel.DuelRecovery.replace(p.getInventory().getContainer(),before);
                    org.dementhium.content.minigames.gambler.GamblerRecovery.set(p,previous);}
                System.err.println("Gambler commit failed: "+failure.getClass().getSimpleName());return false;
            }
        }}
    }
    private static final java.security.SecureRandom GAMBLER_RANDOM=new java.security.SecureRandom();
    public boolean saveDuel(Player a,Player b) {
        try { synchronized(World.getWorld()) { synchronized(fileLock) { recoverTransactions(); duelJournal.commit(a.getUsername(),image(a),b.getUsername(),image(b)); } } return true; }
        catch(Exception failure) { System.err.println("Duel settlement could not be committed: "+failure.getClass().getSimpleName());return false; }
    }

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
				if(FileUtilities.exists(directory + def.getName() + EXTENSION) && !loadPassword(def.getName(), def.getPassword())) {
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
			
			
			if(!FileUtilities.exists(directory + def.getName() + EXTENSION)) {
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
				recoverTransactions(); data = FileUtilities.fileBuffer(directory + name + EXTENSION);
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
				recoverTransactions(); data = FileUtilities.fileBuffer(directory + name + EXTENSION);
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
				recoverTransactions(); data = FileUtilities.fileBuffer(directory + player.getUsername() + EXTENSION);
			}
			if(data != null) {
				byte[] loaded=new byte[data.remaining()];data.duplicate().get(loaded);
                player.setAttribute("loadedAccountDigest",java.security.MessageDigest.getInstance("SHA-256").digest(loaded));
                player.load(data);
				return true;
			}
		} catch(Throwable e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean isCurrentLoad(Player player) {
        byte[] expected=player.getAttribute("loadedAccountDigest");if(expected==null)return true;
        try { synchronized(fileLock) {
            recoverTransactions();byte[] actual=java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(directory+player.getUsername()+EXTENSION));
            return java.util.Arrays.equals(expected,java.security.MessageDigest.getInstance("SHA-256").digest(actual));
        }} catch(Exception failure){return false;}
    }
    public void recordDuel(String id,Player winner,Player a,Player b,org.dementhium.model.Container first,org.dementhium.model.Container second) {
        StringBuilder entry=new StringBuilder().append(System.currentTimeMillis()).append(' ').append(id)
            .append(" winner=").append(winner==null?"REFUND":winner.getUsername());
        Player[] people={a,b};org.dementhium.model.Container[] stakes={first,second};
        for(int n=0;n<2;n++) {
            entry.append(" account=").append(people[n].getUsername()).append(" stake=");
            for(org.dementhium.model.Item item:stakes[n].toArray())if(item!=null)
                entry.append(item.getId()).append(':').append(item.getAmount()).append(':').append(item.getHealth()).append(',');
        }
        entry.append(System.lineSeparator());
        try { java.nio.file.Files.write(java.nio.file.Paths.get(directory,"duel-results.log"),entry.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8),
            java.nio.file.StandardOpenOption.CREATE,java.nio.file.StandardOpenOption.APPEND); }
        catch(IOException failure){System.err.println("Committed duel "+id+" could not append its audit record.");}
    }
    public boolean save(Player player) { synchronized(World.getWorld()) {
        if(Boolean.TRUE.equals(player.getAttribute("saveSessionClosed")))return true;
        Player current=World.getWorld().getPlayerInServer(player.getUsername());
        if(current!=null&&current!=player)return false;
		try {
			ChannelBuffer saveBuffer = ChannelBuffers.dynamicBuffer();
			player.save(saveBuffer);
			synchronized(fileLock) {
				recoverTransactions(); byte[] bytes=new byte[saveBuffer.readableBytes()];saveBuffer.getBytes(0,bytes);DuelJournal.atomicWrite(java.nio.file.Paths.get(directory + player.getUsername() + EXTENSION),bytes);
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
}


