package org.dementhium.model;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.dementhium.DementhiumShutdownHook;
import org.dementhium.Highscores;
import org.dementhium.ServerThread;
import org.dementhium.content.minigames.FightCaves;
import org.dementhium.content.activity.ActivityManager;
import org.dementhium.content.activity.impl.CastleWarsActivity;
import org.dementhium.content.activity.impl.DuelActivity;
import org.dementhium.content.activity.impl.DuelActivity.State;
import org.dementhium.content.activity.impl.GodwarsActivity;
import org.dementhium.content.activity.impl.ImpetuousImpulses;
import org.dementhium.content.areas.AreaManager;
import org.dementhium.content.areas.CoordinateEvent;
import org.dementhium.content.clans.ClanManager;
import org.dementhium.content.dialogue.DialogueManager;
import org.dementhium.content.skills.agility.Agility;
import org.dementhium.content.skills.crafting.LeatherCrafting;
import org.dementhium.identifiers.IdentifierManager;
import org.dementhium.io.PlayerLoader;
import org.dementhium.io.PlayerLoader.PlayerLoadResult;
import org.dementhium.model.combat.SpellContainer;
import org.dementhium.model.definition.ItemDefinition;
import org.dementhium.model.definition.NPCDefinition;
import org.dementhium.model.definition.PlayerDefinition;
import org.dementhium.model.map.ObjectManager;
import org.dementhium.model.map.Position;
import org.dementhium.model.map.path.PathFinder;
import org.dementhium.model.map.path.PathState;
import org.dementhium.model.map.region.RegionBuilder;
import org.dementhium.model.misc.GroundItemManager;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.npc.NPCDropLoader;
import org.dementhium.model.npc.NPCLoader;
import org.dementhium.model.npc.impl.Nex;
import org.dementhium.model.player.Player;
import org.dementhium.model.player.ShopManager;
import org.dementhium.net.GameSession;
import org.dementhium.net.PacketManager;
import org.dementhium.net.message.MessageBuilder;
import org.dementhium.net.packethandlers.WalkingHandler;
import org.dementhium.task.ParallelTaskExecutor;
import org.dementhium.task.SequentialTaskExecutor;
import org.dementhium.task.Task;
import org.dementhium.task.impl.SessionLoginTask;
import org.dementhium.task.impl.SessionLogoutTask;
import org.dementhium.tickable.Tick;
import org.dementhium.tickable.impl.GroundItemUpdateTick;
import org.dementhium.tickable.impl.WGuildTick;
import org.dementhium.util.Constants;
import org.dementhium.util.DementhiumThreadFactory;
import org.dementhium.util.EntityList;
import org.dementhium.util.Misc;
import org.dementhium.util.handlers.DisplayNamesHandler;
import org.dementhium.util.handlers.OffencesHandler;
import org.dementhium.util.handlers.RightsHandler;
import org.jboss.netty.channel.ChannelFutureListener;

/**
 * @author Emperor
 * @author Lumby
 * @author Steve
 * @author 'Mystic Flow
 * @author Khaled
 * @author `Discardedx2
 */
public final class World implements Runnable {

	private static final World INSTANCE = new World();
	//private static final World INSTANCE2 = new World();

	public static World getWorld() { //change this to: getWorld1() if you want multiple worlds. (only use this in RS2ServerBootstrap)
		return INSTANCE;
	}

	//To make more worlds that INTERACT WITH EACH OTHER (if you want more world which don't have much to do with each other go to WorldList and add the world there instead):
	//Go to WorldRequestHandler.java and make it read the worldId (from WorldList).
	//After that make a getter and setter in Player.java for the WorldId.
	//After that you have to use the stuff below and change the above method from getWorld() to getWorld1().
	//And finally you have to go through the whole server to edit World.getWorld() to World.getWorld(player) but in RS@ServerBootstrap, change it to:
	//World.getWorld1().load();
	//World.getWorld2().load();
	//And uhm, after that you prolly have to do more =D

	/*public static World getWorld2() { //(only use this in RS2ServerBootstrap)
		return INSTANCE2;
	}

	public static World getWorld(Player player) {
	//if (player.getWorldId() == 1)
		return INSTANCE;
	//if (player.getWorldId() == 2)
		//return INSTANCE2;
	//else
		//return INSTANCE;
	}*/

	/**
	 * The amount of ticks passed, used for checking how long ago a player did a certain execution.
	 * Eg. for food, burrying bones, ...
	 */
	private static int ticksPassed;

	private EntityList<Player> players = new EntityList<Player>(Constants.MAX_AMT_OF_PLAYERS);
	public EntityList<Player> lobbyPlayers = new EntityList<Player>(Constants.MAX_AMT_OF_PLAYERS);
	private EntityList<NPC> npcs = new EntityList<NPC>(Constants.MAX_AMT_OF_NPCS);
	private ShopManager shopManager = new ShopManager();
	private NPCDropLoader npcDropLoader = new NPCDropLoader();
	private LinkedList<Tick> ticksToAdd = new LinkedList<Tick>();
	private LinkedList<Tick> ticks = new LinkedList<Tick>();

	private PacketManager packetManager = new PacketManager();
	private PlayerLoader playerLoader = new PlayerLoader();

	private SequentialTaskExecutor sequentialExecutor = new SequentialTaskExecutor();
	private ParallelTaskExecutor parallelExecutor = new ParallelTaskExecutor();

	private ClanManager clanManager;
	private AreaManager areaManager;

	private final ExecutorService backgroundLoader = Executors.newFixedThreadPool(1, new DementhiumThreadFactory("BackgroundLoader"));

	private OffencesHandler offencesHandler = new OffencesHandler();
	private RightsHandler rightsHandler = new RightsHandler();
	private DisplayNamesHandler displayNamesHandler;

	public static boolean print;

	private World() {

	}

	public void load() throws Exception {
		System.out.println("Loading World...");
		displayNamesHandler = new DisplayNamesHandler();
		clanManager = new ClanManager();
		areaManager = new AreaManager();
		npcDropLoader.load();
		GroundItemManager.load();
		SpellContainer.initialize();
		SpecialAttackContainer.initialize();
		DialogueManager.init();
		shopManager.load();
		offencesHandler.load();
		rightsHandler.load();
		ObjectManager.init();
		ItemDefinition.init();
		NPCDefinition.init();
		NPCLoader.load();
		Agility.init();
		LeatherCrafting.init();
		RegionBuilder.init();
		IdentifierManager.registerIdentifiers();
		registerEvents();
		registerGlobalActivities();
		new ServerThread();
		//Highscores.INSTANCE.init();
	}

	public Deque<Task> tickTasks = new ArrayDeque<Task>();
	public Deque<Task> updateTasks = new ArrayDeque<Task>();
	public Deque<Task> resetTasks = new ArrayDeque<Task>();

	@Override
	public synchronized void run() {
        org.dementhium.model.instance.InstanceManager instanceManager = org.dementhium.model.instance.InstanceManager.getSingleton();
        if (instanceManager.isShuttingDown()) return;
        boolean instanceCycle = false;
		try {
            instanceManager.beginCycle(); instanceCycle = true;
            instanceManager.drainRequests();
            instanceManager.maintain();
			long start = System.currentTimeMillis();
			if (ticksToAdd.size() > 0) {
				ticks.addAll(ticksToAdd);
				ticksToAdd = new LinkedList<Tick>();
			}
			for (Iterator<Tick> it = ticks.iterator(); it.hasNext(); ) {
				Tick tick = it.next();
				if (tick == null)
					continue;
				if (!tick.run()) {
					it.remove();
				}
			}
			Iterator<Player> worldPlayers = players.iterator();
			while (worldPlayers.hasNext()) {
				Player player = worldPlayers.next();
				if (player.isOnline() && !player.destroyed() && !player.getConnection().isDisconnected()) {
					tickTasks.add(player.tickTask);
					updateTasks.add(player.updateTask);
					resetTasks.add(player.resetTask);
				} else {
					submitTask(new SessionLogoutTask(player));
				}
			}
			Iterator<Player> lobbyIterator = lobbyPlayers.iterator();
			while (lobbyIterator.hasNext()) {
				Player player = lobbyIterator.next();
				if (player.getConnection().isInLobby() && !player.getConnection().isDisconnected()) {
					tickTasks.add(player.tickTask);
				} else {
					submitTask(new SessionLogoutTask(player));
				}
			}
			for (NPC npc : World.getWorld().getNpcs()) {
				tickTasks.add(npc.getNPCTasks()[0]);
				resetTasks.add(npc.getNPCTasks()[1]);
			}
			sequentialExecutor.performTasks(tickTasks);
			sequentialExecutor.performTasks(updateTasks);
			sequentialExecutor.performTasks(resetTasks);
			long elapsed = System.currentTimeMillis() - start;
			//System.err.println("Benchmark " + elapsed);
			ticksPassed++;
		} catch (Throwable e) {
			e.printStackTrace();
		} finally {
            if (instanceCycle) instanceManager.endCycle();
        }
	}

	public void registerEvents() {
		submit(GroundItemUpdateTick.getSingleton());
		submit(new WGuildTick(5));
		submit(Nex.NexAreaEvent.getNexAreaEvent());
	}

	/**
	 * Registers the global activities.
	 */
	private void registerGlobalActivities() {
		/*
		 * Please add new activities after castle wars, it has to be the first one.
		 */
		ActivityManager.getSingleton().register(CastleWarsActivity.getSingleton());
		ActivityManager.getSingleton().register(ImpetuousImpulses.getSingleton());
		ActivityManager.getSingleton().register(GodwarsActivity.getSingleton());
	}

	public void load(final GameSession connection, final PlayerDefinition definition) {
		if (connection != null && definition != null) {
			backgroundLoader.submit(new Runnable() {
				public void run() {
					PlayerLoadResult result = playerLoader.load(connection, definition);
					int code = result.getReturnCode();
					if (code != 2) {
						connection.write(new MessageBuilder().writeByte(code).toMessage()).addListener(ChannelFutureListener.CLOSE);
					} else {
						connection.setPlayer(result.getPlayer());
						if (playerLoader.load(result.getPlayer())) {
							submitTask(new SessionLoginTask(result.getPlayer()));
						} else {
							connection.write(new MessageBuilder().writeByte(Constants.ERROR_LOADING_PROFILE).toMessage()).addListener(ChannelFutureListener.CLOSE);
						}
					}
				}
			});
		}
	}

	public void load(final GameSession connection, final PlayerDefinition definition, long cacheVersion) {
		if (connection != null && definition != null) {
			backgroundLoader.submit(new Runnable() {
				public void run() {
					PlayerLoadResult result = playerLoader.load(connection, definition);
					result = new PlayerLoadResult(new Player(connection, definition), 2);
					int code = result.getReturnCode();
					if (code != 2) {
						connection.write(new MessageBuilder().writeByte(code).toMessage()).addListener(ChannelFutureListener.CLOSE);
					} else {
						connection.setPlayer(result.getPlayer());
						if (playerLoader.load(result.getPlayer())) {
							submitTask(new SessionLoginTask(result.getPlayer()));
						} else {
							connection.write(new MessageBuilder().writeByte(Constants.ERROR_LOADING_PROFILE).toMessage()).addListener(ChannelFutureListener.CLOSE);
						}
					}
				}
			});
		}
	}

	public void register(final Player player) {
        if (!org.dementhium.model.instance.InstanceManager.getSingleton().isCycleThread()) {
            submitTask(new SessionLoginTask(player)); return;
        }
        if (org.dementhium.model.instance.InstanceManager.getSingleton().isShuttingDown() || player.getConnection().isDisconnected()) return;
        if (players.get(player.getIndex())==player || lobbyPlayers.get(player.getIndex())==player) return;
        Player alreadyOnline=getPlayerInServer(player.getUsername());
        boolean lobbyTransfer=alreadyOnline!=null && !player.getConnection().isInLobby()
            && lobbyPlayers.get(alreadyOnline.getIndex())==alreadyOnline;
        if((alreadyOnline!=null&&alreadyOnline!=player&&!lobbyTransfer)||!playerLoader.isCurrentLoad(player)) {
            player.getConnection().write(new MessageBuilder().writeByte(Constants.ALREADY_ONLINE).toMessage());
            player.getConnection().getChannel().close();return;
        }
        if(lobbyTransfer){alreadyOnline.setAttribute("saveSessionClosed",true);alreadyOnline.setOnline(false);lobbyPlayers.remove(alreadyOnline);}
        player.setLocation(org.dementhium.model.instance.InstanceAccess.recoverLocation(player.getLocation()));
		int code = 2;





		if (player.getConnection().isInLobby()) {
			if (!lobbyPlayers.add(player)) {
				code = 7;
			}
		} else {
			if (!players.add(player)) {
				code = 7;
			}
		}
		player.getConnection().write(new MessageBuilder().writeByte(code).toMessage());
		if (code == 2) {
			player.loadPlayer();
		}
	}

	public NPC register(int npcId, Location spawnLoc) {
		NPC npc = NPCLoader.getNPC(npcId);
		npc.setDoesWalk(true);
		npc.setLocation(spawnLoc);
		npc.setOriginalLocation(spawnLoc);
		npc.loadEntityVariables();
		npcs.add(npc);
		return npc;
	}

	public void unregister(final Player player) {
        if (!org.dementhium.model.instance.InstanceManager.getSingleton().isCycleThread()) {
            submitTask(new SessionLogoutTask(player)); return;
        }
        if (players.get(player.getIndex())!=player && lobbyPlayers.get(player.getIndex())!=player) return;
        org.dementhium.model.instance.InstanceAccess.depart(player,true);
		if (DementhiumShutdownHook.getSingleton().activated) {
			return;
		}
		if (player.getActivity() instanceof DuelActivity) {
			DuelActivity duel = (DuelActivity) player.getActivity();
			if (!duel.depart(player)) { submitTask(new SessionLogoutTask(player)); return; }
		} else if (player.getActivity() != null) {
			player.getActivity().forceEnd(player);
		}
		player.closeAll(false, false); //trade + pricecheck + duel stake screen done in this method.
		if (player.interfaceItems != null) {
			player.getInventory().addAllDropable(player.interfaceItems);
			player.interfaceItems = null;
		}

		World.getWorld().getPlayerLoader().save(player); //keep this before familiar dismissal!!

		if (player.getFamiliar() != null) {
			player.getFamiliar().dismiss(false, null); //the only reason we dismiss is so that other players won't see the familiar anymore.
		}
		clanManager.leaveClan(player, true);
		player.setAttribute("saveSessionClosed",true);player.setOnline(false);
		if (player.getConnection().isInLobby()) {
			lobbyPlayers.remove(player);
		} else {
			players.remove(player);
		}
		String name = player.getFormattedUsername();
		for (Player p : World.getWorld().getPlayers()) {
			if (p.getFriendManager().getFriends().contains(name)) {
				p.getFriendManager().updateFriend(name);
			}
		}
		for (Player p : World.getWorld().getLobbyPlayers()) {
			if (p.getFriendManager().getFriends().contains(name)) {
				p.getFriendManager().updateFriend(name);
			}
		}
		
		//if (!Highscores.INSTANCE.players.contains(player))
			//Highscores.INSTANCE.players.offer(player);
	}

	/**
	 * Only insert Username, no DisplayNames.
	 */
	public boolean isOnList(String name) {
		name = Misc.formatPlayerNameForProtocol(name);
		for (Player p : players) {
			if (Misc.formatPlayerNameForProtocol(p.getUsername()).equals(name)) {
				return true;
			}
		}
		for (Player p : lobbyPlayers) {
			if (Misc.formatPlayerNameForProtocol(p.getUsername()).equals(name)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Only insert Username, no DisplayNames.
	 */
	public Player getPlayerInServer(String name) {
		name = Misc.formatPlayerNameForProtocol(name);
		for (Player p : players) {
			if (Misc.formatPlayerNameForProtocol(p.getUsername()).equals(name)) {
				return p;
			}
		}
		for (Player p : lobbyPlayers) {
			if (Misc.formatPlayerNameForProtocol(p.getUsername()).equals(name)) {
				return p;
			}
		}
		return null;
	}

	/**
	 * Only insert Username, no DisplayNames.
	 */
	public Player getPlayerOutOfLobby(String name) {
		name = Misc.formatPlayerNameForProtocol(name);
		for (Player pl : lobbyPlayers) {
			if (pl.getUsername().equals(name)) {
				return pl;
			}
		}
		return null;
	}

	public void submitAreaEvent(final Mob mob, final CoordinateEvent coordinateEvent) {
        final org.dementhium.model.instance.GameInstance instanceContext = org.dementhium.model.instance.InstanceAccess.owner(mob);
        final long instanceRevision=mob.getInstanceRevision();
		mob.submitTick("area_event", new Tick(1) {

			private int attempts;

			@Override
			public void execute() {
				if (mob.getInstanceRevision()!=instanceRevision || org.dementhium.model.instance.InstanceAccess.owner(mob)!=instanceContext
                        || (instanceContext!=null && !instanceContext.isActive())) { stop(); return; }
                if (++attempts >= 20) {
					stop();
					return;
				}
				if ((coordinateEvent.inArea() || !mob.getWalkingQueue().isMoving()) && mob.getAttribute("freezeTime", 0) < getTicks()) {
					stop();
					mob.getWalkingQueue().reset();
					coordinateEvent.execute();
				}
			}
		});
	}

	public void submit(Tick tick) {
		ticksToAdd.add(tick);
	}

	public void submitTask(final Task task) {
        if (task instanceof SessionLoginTask || task instanceof SessionLogoutTask) {
            final boolean logout = task instanceof SessionLogoutTask;
            final Player lifecyclePlayer = logout ? ((SessionLogoutTask)task).getPlayer() : ((SessionLoginTask)task).getPlayer();
            org.dementhium.model.instance.InstanceManager.getSingleton().submitLifecycle(lifecyclePlayer, logout, () -> task.execute())
                .whenComplete((result,failure) -> {
                    if (failure != null) {
                        if (lifecyclePlayer.getConnection().getChannel() != null) lifecyclePlayer.getConnection().getChannel().close();
                        System.err.println("Session lifecycle request failed: " + failure);
                    }
                });
            return;
        }
		ServerThread.service2.submit(new Runnable() {
			@Override
			public void run() {
				try {
					task.execute();
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		});
	}

	public PathState doPath(PathFinder pathFinder, Mob mob, int x, int y) {
		return doPath(pathFinder, mob, x, y, false, true, true);
	}

	public PathState doPath(final PathFinder pathFinder, final Mob mob, final int x, final int y, final boolean ignoreLastStep, boolean addToWalking) {
		return doPath(pathFinder, mob, x, y, ignoreLastStep, addToWalking, true);
	}

	public PathState doPath(final PathFinder pathFinder, final Mob mob, final int x, final int y, final boolean ignoreLastStep, boolean addToWalking, boolean nullOnFail) {
		Location destination = Location.locate(x, y, mob.getLocation().getZ());
		Location base = mob.getLocation();
		int srcX = mob.getViewportX();
		int srcY = mob.getViewportY();
		int destX = destination.getViewportX(base, mob.getViewportDepth());
		int destY = destination.getViewportY(base, mob.getViewportDepth());
		PathState state = pathFinder.findPath(mob, mob.getLocation(), srcX, srcY, destX, destY, mob.getLocation().getZ(), 0, mob.getWalkingQueue().isRunning(), ignoreLastStep, nullOnFail);
		if (state != null && addToWalking) {
			if (!WalkingHandler.canMove(mob)) {
				return state;
			}
			mob.getWalkingQueue().reset();
			for (Position step : state.getPoints()) {
				mob.addPoint(step.getX(), step.getY());
			}
		}
		return state;
	}

	public void doPath(Mob mob, PathState state) {
		if (state != null) {
			if (!WalkingHandler.canMove(mob)) {
				return;
			}
			mob.getWalkingQueue().reset();
			for (Position step : state.getPoints()) {
				mob.addPoint(step.getX(), step.getY());
			}
		}
	}

	public EntityList<NPC> getNpcs() {
		return npcs;
	}

	public EntityList<Player> getPlayers() {
		return players;
	}

	public PacketManager getPacketManager() {
		return packetManager;
	}

	public PlayerLoader getPlayerLoader() {
		return playerLoader;
	}

	public ClanManager getClanManager() {
		return clanManager;
	}

	public EntityList<Player> getLobbyPlayers() {
		return lobbyPlayers;
	}

	public AreaManager getAreaManager() {
		return areaManager;
	}

	public ShopManager getShopManager() {
		return shopManager;
	}


	public NPCDropLoader getNpcDropLoader() {
		return npcDropLoader;
	}

	public ExecutorService getBackgroundLoader() {
		return backgroundLoader;
	}

	/**
	 * @return the ticksPassed
	 */
	public static int getTicks() {
		return ticksPassed;
	}

	public OffencesHandler getOffencesHandler() {
		return offencesHandler;
	}

	public RightsHandler getRightsHandler() {
		return rightsHandler;
	}

	public DisplayNamesHandler getDisplayNamesHandler() {
		return displayNamesHandler;
	}

}


