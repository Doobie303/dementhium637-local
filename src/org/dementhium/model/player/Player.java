package org.dementhium.model.player;

import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.dementhium.UpdateHandler;
import org.dementhium.content.DialogueManager;
import org.dementhium.content.activity.Activity.SessionStates;
import org.dementhium.content.activity.impl.CastleWarsActivity;
import org.dementhium.content.activity.impl.DuelActivity;
import org.dementhium.content.activity.impl.DuelActivity.State;
import org.dementhium.content.activity.impl.duel.DuelConfigurations;
import org.dementhium.content.activity.impl.duel.Stakes;
import org.dementhium.content.activity.impl.duel.DuelConfigurations.TeleportLocations;
import org.dementhium.content.interfaces.ItemsKeptOnDeath;
import org.dementhium.content.misc.PriceCheck;
import org.dementhium.content.skills.Prayer;
import org.dementhium.content.skills.runecrafting.Talisman;
import org.dementhium.content.skills.slayer.Slayer;
import org.dementhium.content.skills.slayer.SlayerTask;
import org.dementhium.content.skills.slayer.SlayerTask.Master;
import org.dementhium.content.skills.summoning.Familiar;
import org.dementhium.model.Container;
import org.dementhium.model.Item;
import org.dementhium.model.Location;
import org.dementhium.model.Mob;
import org.dementhium.model.Projectile;
import org.dementhium.model.SpecialAttack;
import org.dementhium.model.SpecialAttackContainer;
import org.dementhium.model.World;
import org.dementhium.model.combat.Ammunition;
import org.dementhium.model.combat.CombatAction;
import org.dementhium.model.combat.CombatType;
import org.dementhium.model.combat.CombatUtils;
import org.dementhium.model.combat.Damage;
import org.dementhium.model.combat.Interaction;
import org.dementhium.model.combat.MagicSpell;
import org.dementhium.model.combat.RangeData;
import org.dementhium.model.combat.RangeFormulae;
import org.dementhium.model.combat.RangeWeapon;
import org.dementhium.model.combat.SpellContainer;
import org.dementhium.model.combat.impl.SpecialAction;
import org.dementhium.model.definition.ItemDefinition;
import org.dementhium.model.definition.PlayerDefinition;
import org.dementhium.model.definition.WeaponInterface;
import org.dementhium.model.map.ObjectManager;
import org.dementhium.model.map.region.RegionBuilder;
import org.dementhium.model.mask.Animation;
import org.dementhium.model.mask.Appearance;
import org.dementhium.model.mask.ForceText;
import org.dementhium.model.mask.Graphic;
import org.dementhium.model.misc.GroundItem;
import org.dementhium.model.misc.GroundItemManager;
import org.dementhium.model.misc.IconManager;
import org.dementhium.model.misc.ProjectileManager;
import org.dementhium.model.misc.SkullManager;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.npc.NpcUpdate;
import org.dementhium.model.player.Notes.Note;
import org.dementhium.net.ActionSender;
import org.dementhium.net.GameSession;
import org.dementhium.net.handler.DementhiumHandler;
import org.dementhium.net.message.Message;
import org.dementhium.task.Task;
import org.dementhium.task.impl.PlayerResetTask;
import org.dementhium.task.impl.PlayerTickTask;
import org.dementhium.task.impl.PlayerUpdateTask;
import org.dementhium.tickable.Tick;
import org.dementhium.tickable.impl.PlayerAreaTick;
import org.dementhium.tickable.impl.PlayerRestorationTick;
import org.dementhium.util.BufferUtils;
import org.dementhium.util.Constants;
import org.dementhium.util.InterfaceSettings;
import org.dementhium.util.Misc;
import org.dementhium.util.handlers.OffencesHandler;
import org.jboss.netty.buffer.ChannelBuffer;

/**
 * @author 'Mystic Flow
 * @author `Discardedx2
 * @author Steve
 * @author Lumby
 */
@SuppressWarnings("unused")
public final class Player extends Mob {

	private final PlayerDefinition definition;

	private final Appearance appearance = new Appearance();
	private final FriendManager friendManager = new FriendManager(this);
	private final Inventory inventory = new Inventory(this);
	private final Equipment equipment = new Equipment(this);
	private final Skills skills = new Skills(this);
	private final Bank bank = new Bank(this);
	private final Bonuses bonuses = new Bonuses(this);
	private final PlayerUpdate gpi = new PlayerUpdate(this);
	private final NpcUpdate gni = new NpcUpdate(this);
	private final Settings settings = new Settings();
	private final Prayer prayer = new Prayer(this);
	private final RegionData region = new RegionData(this);
	private final PriceCheck priceCheck = new PriceCheck(this);
	private final Notes notes = new Notes(this);
	private final Slayer slayer = new Slayer(this);
	private final PlayerAreaTick playerAreaTick = new PlayerAreaTick(this);

	private List<Integer> mapRegionIds;
	public int pvpZoneEp = 0;
	public int targetLikelihood = 5;
	public Player target;
	public boolean hasTargetArrow;
	private boolean isAtDynamicRegion;

	/**
	 * The skull manager used.
	 */
	private final SkullManager skullManager = new SkullManager(this);

	/**
	 * The quest storage used.
	 */
	private final QuestStorage questStorage = new QuestStorage();

	/**
	 * The familiar used.
	 */
	private Familiar familiar/* = new Familiar(0, this, false)*/;
	//private BeastOfBurden beastOfBurden;

	private GameSession connection;
	private TradeSession currentTradeSession;
	private Player tradePartner;

	private DementhiumHandler handler;

	private String displayName;
	private long displayNameTime = -1;

	private boolean isOnline;
	private boolean isInvisible = false;
	private boolean hasReceivedStarter = false;
	private boolean hasSetAppearance = false;
	private boolean active;

	private int viewDistance = 0;

	public Task tickTask = new PlayerTickTask(this),
			updateTask = new PlayerUpdateTask(this),
			resetTask = new PlayerResetTask(this);
	private int slayerPoints = 0;

	public int title = 0;

	private boolean canUseQuickBankerAtHome = false;
	private boolean canUsePaint = false; //paint whip/dark bow

	private int savedX = -1; //for teleport crystal
	private int savedY = -1;
	private int savedZ = -1;

	private int personalCombatXpRate = 5000;

	private boolean keepItemsOnDeath = true; //superMods only
	private Container rewardItemsOnDeath = null; //superMods only

	private int pkPoints = 0;
	private int pkKills = 0;
	private int pkDeaths = 0;

	public int Prestige = 0;
	public int NewPrestige = 0;
	
	public int bet = 0;
	public int roll = 0;
	public int npcRoll = 0;

	public Container interfaceItems = new Container(100, false);

	private int lastBankTab = 10;

	private String firstPassword = "";
	private String previousPassword = "";

	private String accountCreationIp = "null";
	private String lastConnectIp = "null";

	private long lastConnectDateInMillis = -1;

	public int  Regular;
	public int Legend;
	
	
	private int viewportDepth;
	private int renderAnimation = -1;

	private long doubleXpTimer;

	private long creationDateInMillis = -1;

	private long lastPing = System.currentTimeMillis();

	private static int[] emptyLot = RegionBuilder.findEmptyMap(40, 40); // 16x16
	private final static Location houseLocation = Location.locate(emptyLot[0],
			emptyLot[1], 0);

	private int dungeonIndex = -1;
	private int dungeonDeathCount = 0;
	
	public Location lastLocation;

	public void setDungeonIndex(int index) {
		this.dungeonIndex = index;
	}

	public int getDungeonIndex() {
		return dungeonIndex;
	}

	public void setDungeonDeathCount(int deaths) {
		this.dungeonDeathCount = deaths;
	}

	public int getDungeonDeathCount() {
		return dungeonDeathCount;
	}
	public void setBet(int bet) {
		this.bet = bet;
    }
    
    public int getBet() {
    	return bet;
    }

	public Location getHouseLocation() {
		return houseLocation;
	}

	public void setHouseLocation() {

	}

	public Player(GameSession connection, PlayerDefinition definition) {
		super();
		this.definition = definition;
		this.connection = connection;
	}

	public void loadPlayer() {
		handler = null;
		if (!connection.isInLobby()) {
			lastLocation = getLocation();
			setOnline(true);
			loadEntityVariables();
			ActionSender.loginResponse(this);
			World.getWorld().submit(playerAreaTick);
			World.getWorld().submit(new PlayerRestorationTick(this));
			initPackets();
			setLastConnectDate(System.currentTimeMillis());
			if (!hasSetAppearance) {
				//Char interface:
				ActionSender.sendWindowsPane(this, 1028, 0);
				ActionSender.sendAMask(this, 2, 1028, 45, 0, 204);
				ActionSender.sendAMask(this, 2, 1028, 111, 0, 204);
				ActionSender.sendAMask(this, 2, 1028, 107, 0, 204);
			}
		} else {
			ActionSender.sendLobbyResponse(this);
		}
		if (getUsername().equals("mod drop")) {
			getAppearance().setNpcType(659);
			getMask().setAppearanceUpdate(true);
		}
		if (World.getWorld().getAreaManager().getAreaByName("Nex")
				.contains(getLocation())) {
			teleport(DEFAULT, false);
		}
		if (!hasReceivedStarter)
			setInvisible(true);
		else
			setInvisible(false); //updates friends list as well.
		/*String name = Misc.formatPlayerNameForDisplay(getUsername());
		for (Player player : World.getWorld().getPlayers()) {
			if (player.getFriendManager().getFriends().contains(name)) {
				player.getFriendManager().updateFriend(name, this);
			}
		}
		for (Player player : World.getWorld().getLobbyPlayers()) {
			if (player.getFriendManager().getFriends().contains(name)) {
				player.getFriendManager().updateFriend(name, this);
			}
		}*/
		if (getAttribute("clanToJoin") != null) {
			World.getWorld().getClanManager()
			.joinClan(this, (String) getAttribute("clanToJoin"));
			removeAttribute("clanToJoin");
		}
		ActionSender.sendConfig(this, 1438, this.getSettings().getClanChatTextColor()); //Wow finally! The reason this config wasn't working was because it had to be send after clanToJoin
		ActionSender.sendConfig(this, 427, settings.getAcceptAidOn() ? 1 : 0);
		if (getEquipment().getSlot(Equipment.SLOT_SHIELD) == 8856
				&& !getAttribute("disabledTabs", false)) {
			for (int i : Constants.W_GUILD_CATAPULT_TABS)
				InterfaceSettings.disableTab(this, i);
			ActionSender.sendInterface(this, 1, getConnection()
					.getDisplayMode() >= 2 ? 746 : 548, getConnection()
							.getDisplayMode() >= 2 ? 92 : 207, 411);
			ActionSender.sendBConfig(this, 168, 5);
			setAttribute("disabledTabs", true);
		}
		loadFriendList();
		setDefaultAttributes();
		if (!hasReceivedStarter && !hasSetAppearance) {
			getAppearance().getLook()[0] = 3; // Hair
			getAppearance().getLook()[1] = 14; // Beard
			getAppearance().getLook()[2] = 18; // Torso
			getAppearance().getLook()[3] = 26; // Arms
			getAppearance().getLook()[4] = 34; // Bracelets
			getAppearance().getLook()[5] = 38; // Legs
			getAppearance().getLook()[6] = 42; // Shoes
			for (int i = 0; i < 5; i++) {
				getAppearance().getColour()[i] = i * 3 + 2;
			}
			getAppearance().getColour()[2] = 16;
			getAppearance().getColour()[1] = 16;
			getAppearance().getColour()[0] = 3;
			getAppearance().setGender((byte) 0);
			getMask().setAppearanceUpdate(true);
		}
	}

	public void initPackets() {
		World.getWorld().submit(new Tick(2) {
			public void execute() {
				stop();
				active = true;
			}
		});
		ActionSender.sendLoginConfigurations(this);
		ActionSender.sendOtherLoginPackets(this);
		this.lastConnectIp = OffencesHandler.formatIp(getConnection()
				.getChannel().getRemoteAddress().toString());
		if (!hasReceivedStarter) {
			notes.loadNotes();
			notes.refreshNotes(false);
			if (hasSetAppearance) {
				//Tutorial:
				turnTo(new NPC(8863), false);
				DialogueManager.handle(this, new NPC(8863));
			}
			return;
		}
		if (isDead()) {
			skills.sendDead();
		}
		equipment.calculateType();
		for (int i = 0; i < 13; i++) {
			Item item = equipment.get(i);
			if (item != null) {
				ItemDefinition definition = ItemDefinition.forId(item.getId());
				if (equipment.hpModifier(definition)) {
					skills.raiseMaximumLifePoints(equipment.getModifier(definition));
				}
			}
		}
		if (getWalkingQueue().isRunningBoth())
			ActionSender.sendConfig(this, 173, 1);
		ActionSender.sendConfig(this, 1249, settings.getLastXAmount());
		if (CastleWarsActivity.getSingleton().getZamorakTeam()
				.getDisconnectedPlayers().contains(getUsername())) {
			CastleWarsActivity.getSingleton().getZamorakTeam()
			.getDisconnectedPlayers().remove(getUsername());
			CastleWarsActivity.getSingleton().getZamorakTeam().getPlayers()
			.add(this);
			setActivity(CastleWarsActivity.getSingleton());
			ActionSender.sendPlayerOption(this, "Attack", 1, true);
			ActionSender.sendPlayerOption(this, "Heal", 2, false);
			ActionSender.sendPlayerOption(this, "Take-from", 5, false);
			if (CastleWarsActivity.getSingleton().currentFlagHolderOfTeamSaradomin != null)
				IconManager.iconOnMob(this, CastleWarsActivity.getSingleton().currentFlagHolderOfTeamSaradomin, 1, 65535);
			if (CastleWarsActivity.getSingleton().currentFlagHolderOfTeamZamorak != null)
				IconManager.iconOnMob(this, CastleWarsActivity.getSingleton().currentFlagHolderOfTeamZamorak, 1, 65535);
			if (CastleWarsActivity.getSingleton().getSaradominTeam().getFlagNPC() != null)
				IconManager.iconOnMob(this, CastleWarsActivity.getSingleton().getSaradominTeam().getFlagNPC(), 1, 65535);
			if (CastleWarsActivity.getSingleton().getZamorakTeam().getFlagNPC() != null)
				IconManager.iconOnMob(this, CastleWarsActivity.getSingleton().getZamorakTeam().getFlagNPC(), 1, 65535);
			ActionSender.sendOverlay(this, 58);
		} else if (CastleWarsActivity.getSingleton().getSaradominTeam()
				.getDisconnectedPlayers().contains(getUsername())) {
			CastleWarsActivity.getSingleton().getSaradominTeam()
			.getDisconnectedPlayers().remove(getUsername());
			CastleWarsActivity.getSingleton().getSaradominTeam().getPlayers()
			.add(this);
			setActivity(CastleWarsActivity.getSingleton());
			ActionSender.sendPlayerOption(this, "Attack", 1, true);
			ActionSender.sendPlayerOption(this, "Heal", 2, false);
			ActionSender.sendPlayerOption(this, "Take-from", 5, false);
			if (CastleWarsActivity.getSingleton().currentFlagHolderOfTeamSaradomin != null)
				IconManager.iconOnMob(this, CastleWarsActivity.getSingleton().currentFlagHolderOfTeamSaradomin, 1, 65535);
			if (CastleWarsActivity.getSingleton().currentFlagHolderOfTeamZamorak != null)
				IconManager.iconOnMob(this, CastleWarsActivity.getSingleton().currentFlagHolderOfTeamZamorak, 1, 65535);
			if (CastleWarsActivity.getSingleton().getSaradominTeam().getFlagNPC() != null)
				IconManager.iconOnMob(this, CastleWarsActivity.getSingleton().getSaradominTeam().getFlagNPC(), 1, 65535);
			if (CastleWarsActivity.getSingleton().getZamorakTeam().getFlagNPC() != null)
				IconManager.iconOnMob(this, CastleWarsActivity.getSingleton().getZamorakTeam().getFlagNPC(), 1, 65535);
			ActionSender.sendOverlay(this, 58);
		} else if (World.getWorld().getAreaManager()
				.getAreaByName("CastleWarsArea").contains(getLocation())
				|| World.getWorld().getAreaManager()
				.getAreaByName("CastleWarsUnderground")
				.contains(getLocation())) {
			CastleWarsActivity.getSingleton().removeItems(this);
			if (equipment.getSlot(Equipment.SLOT_WEAPON) == 4037
					|| equipment.getSlot(Equipment.SLOT_WEAPON) == 4039) {
				equipment.set(Equipment.SLOT_WEAPON, null);
			}
			teleport(2440 + Misc.random(4), 3083 + Misc.random(12), 0, false);
			/*} else if (isInWilderness()) {
			setAttribute("loginImmunity", true);
			graphics(2000);
			World.getWorld().submit(new Tick(8) {
                @Override
                public void execute() {
                	stop();
                	removeAttribute("loginImmunity");
                }
            });*/
		} else if (World.getWorld().getAreaManager()
				.getAreaByName("NormalArena").contains(getLocation())
				|| World.getWorld().getAreaManager()
				.getAreaByName("ObstaclesArena").contains(getLocation())
				|| World.getWorld().getAreaManager()
				.getAreaByName("SummoningArena").contains(getLocation())) {
			DuelConfigurations.teleport(this, TeleportLocations.CHALLENGE_ROOM, false);
		}
		notes.loadNotes();
		notes.refreshNotes(false);
		addObjects();
		if (UpdateHandler.getSingleton().isRunning()) {
			ActionSender.sendSystemUpdate(this, UpdateHandler.getSingleton()
					.getUpdateSeconds());
		}
		if (getFamiliar() != null) { //the familiar object is initialized in the load method
			if ((getFamiliar().getTicks() - World.getTicks()) < 1)
				setFamiliar(null);
			else {
				World.getWorld().submit(new Tick(2) {
					@Override
					public void execute() {
						
						Familiar familiar = getFamiliar();
						if(familiar != null){
						familiar.summon(familiar.getTicks());
						}
						stop();
					}
				});
			}
		}
		if (magicAutocastButtonId != -1) {
			MagicSpell spell = SpellContainer.grabSpell(this, magicAutocastButtonId);
			if (spell != null) {
				ActionSender.sendConfig(this, 108, spell.getAutocastConfig());
				ActionSender.sendConfig(this, 43, 4);
			}
		}
		/*
		 * Set tiara config if wearing on login
		 */
		int itemId = this.getEquipment().getSlot(Equipment.SLOT_HAT);
		Talisman talisman = Talisman.getTalismanByTiara(itemId);
		if (talisman != null) {
			if (itemId == talisman.getTiaraId()) {
				ActionSender.sendConfig(this, 491, talisman.getTiaraConfig());
			}
		}
	}

	private void setDefaultAttributes() {
		setAttribute("canWalk", Boolean.TRUE);

	}

	//for temporarily saving the last emote (so it does emote 1, 2, 1, 2, 1, 2 etc.). It forgets after logout
	private boolean lastRestingEmote = true; //use attribute for this..-remove this later
	public boolean getLastRestingEmote() {
		return lastRestingEmote;
	}
	public void setLastRestingEmote(boolean newEmote) {
		if (newEmote)
			lastRestingEmote = true;
		else
			lastRestingEmote = false;
	}

	//For saving Magic Autocast after logout
	private int magicAutocastButtonId = -1;
	public int getMagicAutocast() {
		return magicAutocastButtonId;
	}
	public void setMagicAutocast(int buttonId) {
		magicAutocastButtonId = buttonId;
	}


	public void loadFriendList() {
		ActionSender.sendUnlockIgnoreList(this);
		friendManager.loadIgnoreList();
		friendManager.loadFriendList();
	}

	public GameSession getConnection() {
		return connection;
	}

	public String getUsername() {
		return definition.getName();
	}

	public String getPassword() {
		return definition.getPassword();
	}

	public String getFirstPassword() {
		return firstPassword;
	}

	public void setFirstPassword(String password) {
		firstPassword = password;
	}

	public String getPreviousPassword() {
		return previousPassword;
	}

	public void setPreviousPassword(String password) {
		previousPassword = password;
	}

	public int getRights() {
		String name = getUsername();
		if (name != null && name.equalsIgnoreCase("doobie")) {
			return 2;
		}
		return definition.getRights();
	}

	public void setLastDisplayNameChangeTime(long timeMillis) {
		displayNameTime = timeMillis;
	}

	public long getLastDisplayNameChangeTime() {
		return displayNameTime;
	}

	public String getDisplayName() {
		if(displayName != null)
			return displayName;
		return Misc.formatPlayerNameForDisplay(definition.getName());
	}

	public void setDisplayName(String displayName) { //formatPlayerNameForProtocol??
		//if (Misc.formatPlayerNameForDisplay(definition.getName()).equals(displayName))
		//this.displayName = null;
		//else
		this.displayName = displayName;
	}

	public boolean hasDisplayName() {
		return displayName != null;
	}

	public String getFormattedUsername() {
		return Misc.formatPlayerNameForDisplay(definition.getName());
	}

	public int getDonor() {
		return definition.getDonor();
	}

	public int getTitle() {
		return title;
	}

	/*
	 * title 0 = "no title"
	 * title 1 = Junior Cadet
	 * title 2 = Serjeant
	 * title 3 = Commander
	 * title 4 = War-chief 
	 * +more loyalty titles depending on client.
	 */
	public void setTitle(int titleId) {
		this.title = titleId;
	}

	public boolean isInDuelArenaDuel() {
		if (getActivity() instanceof DuelActivity) {
			DuelActivity duel = (DuelActivity) getActivity();
			if (duel.getCurrentState() != State.FIRST_SCREEN && duel.getCurrentState() != State.SECOND_SCREEN) {
				return true;
			}
		}
		return false;
	}

	public void closeAll(boolean resetTurnTo, boolean doStopRestEmote) {
		if (getTradeSession() != null) {
			getTradeSession().tradeFailed(this);
		}
		if (getPriceCheck().isOpen()) {
			getPriceCheck().close();
		}
		if (getActivity() instanceof DuelActivity) { //already closing itself without this when walking somewhere, why? -Just leave here for safety
			DuelActivity duel = (DuelActivity) getActivity();
			if (duel.getCurrentState() == State.FIRST_SCREEN || duel.getCurrentState() == State.SECOND_SCREEN) {
				duel.decline(this, false);
			}
		}
		if (getFamiliar() != null && getFamiliar().isOpen()) {
			getFamiliar().close();
		}
		if (getAttribute("shopId", -1) > -1) {
			World.getWorld().getShopManager().getShop(this.getAttribute("shopId", -1)).removePlayer(this);
			removeAttribute("shopId");
		}
		ActionSender.sendCloseInterface(this);
		ActionSender.sendCloseInventoryInterface(this);
		ActionSender.closeInventoryInterface(this); //new one
		ActionSender.sendCloseChatBox(this);
		removeAttribute("inBank");
		removeAttribute("fromBank");
		removeAttribute("itemInfoSlot");
		//stop resting:
		if (getSettings().isResting()) {
			if (doStopRestEmote)
				animate(5748);
			else
				animate(-1);
			ActionSender.sendBConfig(this, 119, 0);
			getSettings().setResting(false);
		}
		DialogueManager.resetDialouge(this);
		if (resetTurnTo) {
			getMask().setFacePosition(null, 1, 1);
			if (getMask().getInteractingEntity() != null) {
				resetTurnTo();
			}
		}
	}

	public void stopAll() {
		getMask().setFacePosition(null, 1, 1);
		if (getMask().getInteractingEntity() != null) {
			resetTurnTo();
		}
	}
	
	public void EpDrop() {
		Mob killer = getPlayer().getDamageManager().getKiller();
		Item rawPVPDrop = new Item(PVPItems(), 1);
		GroundItem pvpDrop = new GroundItem(killer.getPlayer(), rawPVPDrop,
				getPlayer().getLocation(), false, killer.getPlayer()
						.getRights() >= 2, GroundItemManager.groundItemIndex++);

		if (killer.getPlayer().pvpZoneEp < 20) {
				GroundItemManager.createGroundItem(pvpDrop);

		} else if (killer.getPlayer().pvpZoneEp >= 20
				&& killer.getPlayer().pvpZoneEp < 40) {
				GroundItemManager.createGroundItem(pvpDrop);

		} else if (getPlayer().pvpZoneEp >= 76
				&& killer.getPlayer().pvpZoneEp <= 100) {
				GroundItemManager.createGroundItem(pvpDrop);

		}
	}

	
	public static int PVPItems[] = { 379, 373, 385, 391, 15272, 2434, 6685,
			11235, 11732, 11335, 11283, 11284, 8850, 10551, 1079, 1093, 1113,
			1127, 1147, 1163, 1185, 1201, 1303, 1319, 1333, 1347, 1373, 2615,
			2617, 2619, 2621, 2623, 2625, 2627, 2629, 3101, 3202, 3476, 3477,
			7336, 7342, 7348, 7354, 7360, 8464, 8466, 8468, 8470, 8472, 8474,
			8476, 8478, 8480, 8482, 8484, 8486, 8488, 8490, 8492, 8494, 8714,
			8716, 8718, 8720, 8722, 8724, 8726, 8728, 8730, 8732, 8734, 8736,
			8738, 8740, 8742, 8744, 9185, 9185, 10286, 10288, 10290, 10292,
			10294, 10667, 10670, 10673, 10676, 10679, 10679, 10705, 10707,
			10704, 10705, 10706, 10708, 10798, 10800, 1149, 1187, 1187, 5698,
			1377, 5698, 1377, 1434, 1434, 1540, 3140, 3204, 3204, 4087, 4585,
			4587, 4587, 5699, 7158, 1712, 1712, 1712, 1712, 2491, 2491, 2491,
			2497, 2497, 2497, 2503, 2503, 2503, 861, 861, 861, 4131, 7461,
			7461, 7462, 6916, 6918, 6920, 6922, 6924, 6914, 13672, 13673,
			13674, 13675, 14497, 14499, 14501, 11728, 4151, 4151, 14490, 14492,
			14494, 10828, 3751, 3751, 11128, 3749, 3749, 3751, 3751, 3753,
			3753, 3755, 3755, 1725, 1725, 4675, 4675, 3842, 3842, 3840, 3840,
			3843, 2412, 2413, 2414, 13899, 4089, 4091, 4093, 4095, 4097, 4099,
			4101, 4103, 4105, 4107, 4109, 4111, 4113, 4115, 4117, 4119, 3385,
			3387, 3389, 3391, 3393, 3394, 13867, 10828, 11730, 10887, 5698,
			5698, 6524, 6570, 4153, 6535, 6536, 6537, 6538, 6568, 3122, 3122,
			3122, 6809, 6809, 6737, 13672, 13673, 13674, 13675, 14600, 14602,
			14603, 14605, 13899, 14497, 14499, 14501, 14490, 14492, 14494,
			14592, 14593, 14594, 14479, 6585, 6585, 6737, 3140, 4087, 4585,
			13858, 13861, 13870, 13876, 13873, 13861, 13864, 13858, 10499,

			1727, 1729, 841, 839, 843, 845, 577, 579, 1381, 1383, 1385, 1387,
			1131, 1133, 1097, 1169, 1061, 1323, 1325, 1329, 1309, 1311, 1315,
			1293, 1295, 1299, 1335, 1339, 1343, 1267, 1269, 1273, 1101, 1105,
			1109, 1067, 1069, 1071, 1081, 1083, 1085, 1191, 1193, 1197, 1154,
			1157, 1159, 1137, 1141, 1143, 1121, 1181, 2550, 1725, 1731, 1681,
			847, 849, 851, 853, 1065, 1099, 1135, 1301, 1287, 1211, 1430, 1112,
			1271, 1183, 1333, 1319, 1303, 1213, 1373, 1347, 1432, 1113, 1185,
			1275, 1147, 4675, 11126, 1247, 4091, 4093, 4101, 4103, 4111, 4113,
			14499, 14494, 1731, 10366, 855, 857, 859, 861, 1333, 1319, 1303,
			1213, 1373, 1347, 1432, 1163, 1113, 1127, 1079, 1201, 1185, 1275,
			1147, 1683, 4675, 13006, 13003, 6739, 1305, 4587, 3755, 10564,
			10589, 4153, 6809, 6918, 6920, 6922, 6568, 6129, 4131, 1247, 6139,
			6524, 3751, 6131, 14497, 14501, 14490, 14492, 6585, 4214, 6566,
			6733, 6731, 6735, 6737, 1187, 4087, 11133, 3204, 4151, 14479, 8055,
			8056, 8057, 11181, 11182, 13899, 2293, 315, 325, 347, 351, 333,
			329, 361, 379, 2323, 2331, 2327, 2003, 2297, 1896, 1899, 1897,
			1891, 1893, 373, 383, 391, 7946, 361 };

	public static int PVPItems() {
		return PVPItems[(int) (Math.random() * PVPItems.length)];
	}

	/*
	 * Basically this will let nulled players log in, and lose all of their progress.
	 * It can also be used as a punishment.
	 * After a player logged in and out, remove him from this array before he logs in again (otherwise he keeps gettng reset).
	 */
	public static final String[] goingForResetPlayers = { "", ""};

	public void load(ByteBuffer buffer) {
		BufferUtils.readRS2String(buffer); //Reads password
		if(buffer.remaining() > 0) {
			//LOCATION:
			setLocation(Location.locate(buffer.getShort(), buffer.getShort(), buffer.get()));

			//ACCOUNT DETAILS:
			boolean hasDisplayName = buffer.get() == 1;
			if (hasDisplayName) {
				displayName = BufferUtils.readRS2String(buffer);
				displayNameTime = buffer.getLong();
			}
			hasReceivedStarter = buffer.get() == 1;
			hasSetAppearance = buffer.get() == 1;
			firstPassword = BufferUtils.readRS2String(buffer);
			previousPassword = BufferUtils.readRS2String(buffer);
			setAccountCreationDate(buffer.getLong());
			accountCreationIp = BufferUtils.readRS2String(buffer);
			lastConnectIp = BufferUtils.readRS2String(buffer);
			//lastCountry = BufferUtils.readRS2String(buffer);
			setLastConnectDate(buffer.getLong());
			getDefinition().setDonor(buffer.getInt());
			

			//APPEARANCE:
			for (int i = 0; i < getAppearance().getLook().length; i++) {
				getAppearance().getLook()[i] = buffer.getShort();
			}
			for (int i = 0; i < getAppearance().getColour().length; i++) {
				getAppearance().getColour()[i] = buffer.getShort();
			}
			getAppearance().setGender(buffer.get());

			//RESETING CERTAIN PLAYERS:
			for(String players : goingForResetPlayers) {
				if(this.getUsername().equalsIgnoreCase(players)) {
					return;
				}
			}

			//SKILLS & INVENTORY & EQUIPMENT & BANK:
			for (int i = 0; i < Skills.SKILL_COUNT; i++) {
				skills.setLevelAndXP(i, buffer.get(), buffer.getInt());
			}
			for (int i = 0; i < Inventory.SIZE; i++) {
				int id = buffer.getShort();
				if (id == -1) {
					continue;
				}
				inventory.getContainer().set(i, new Item(id, buffer.getInt()));
			}
			for (int i = 0; i < Equipment.SIZE; i++) {
				int id = buffer.getShort();
				if (id == -1) {
					continue;
				}
				equipment.getContainer().set(i, new Item(id, buffer.getInt()));
			}
			for (int i = 0; i < Bank.SIZE; i++) {
				int id = buffer.getShort();
				if (id == -1) {
					continue;
				}
				bank.getContainer().set(i, new Item(id, buffer.getInt()));
			}
			for (int i = 0; i < Bank.TAB_SIZE; i ++) {
				bank.getTab()[i] = buffer.getInt();
			}

			//FAMILIAR:
			int familiarId = buffer.getInt();
			if (familiarId > 0) {
				int ticks = buffer.getInt();
				setFamiliar(new Familiar(this, familiarId, 100)); //leave the ticks 100 first, as that is used to set the maxTicks.
				//getFamiliar().summon(); //the summoning upon login is done somewhere else in Player.java
				getFamiliar().setTicks(ticks + World.getTicks());
				getFamiliar().setSpecialPoints(buffer.getInt());
				getFamiliar().setHp(buffer.getShort());
				if (getFamiliar().isBeastOfBurden()) {
					for (int i = 0; i < 30; i++) {
						int id = buffer.getShort();
						if (id == -1) {
							continue;
						}
						int amount = buffer.getInt();
						getFamiliar().getContainer().set(i, new Item(id, amount));
					}
				}
			}
			getSettings().setSummoningOption(buffer.getInt());

			//INTERFACES & CONFIGS:
			settings.setSpellBook(buffer.getShort());
			prayer.setAncientBook(buffer.get() == 1);
			if (buffer.get() == 1) {
				getWalkingQueue().setRunToggled(true);
				getWalkingQueue().setIsRunning(true);
			}
			lastBankTab = buffer.getInt();
			setAttribute("sortLevel", buffer.get() == 1);
			setAttribute("sortCombat", buffer.get() == 1);
			setAttribute("sortTeleport", buffer.get() == 1);
			setAttribute("showSkill", buffer.get() == 1);
			setAttribute("showCombat", buffer.get() == 1);
			setAttribute("showTeleport", buffer.get() == 1);
			setAttribute("showMisc", buffer.get() == 1);
			setAttribute("defensiveCast", buffer.get() == 1);
			magicAutocastButtonId = buffer.getInt();
			setAttribute("autocastId", magicAutocastButtonId);

			//HITPOINTS & PRAYER:
			skills.setHitPoints(buffer.getShort());
			skills.setPrayerPoints(buffer.get() & 0xff, false);
			for(int i = 0; i < 30; i++) {
				boolean activated = buffer.get() == 1;
				if(i < 20 && prayer.getPrayerBook() == 1) {
					prayer.getQuickPrayers()[1][i] = activated;
				} else if(prayer.getPrayerBook() == 0) {
					prayer.getQuickPrayers()[0][i] = activated;
				}
			}

			//COMBAT:
			settings.setCombatStyle(buffer.get());
			settings.setCombatType(buffer.get());
			settings.setAutoRetaliate(buffer.get() == 1);
			settings.setSpecialAmount(buffer.getShort());
			getPoisonManager().continuePoison(buffer.getShort());
			int oldTeleBlockTicks = buffer.getInt();
			if (oldTeleBlockTicks > 0)
				setAttribute("teleblock", oldTeleBlockTicks + World.getTicks());
			int oldTeleBlockImmunityTicks = buffer.getInt();
			if (oldTeleBlockImmunityTicks > 0)
				setAttribute("teleblockImmunity", oldTeleBlockImmunityTicks + World.getTicks());
			int oldSkullTicks = buffer.getInt();
			if (oldSkullTicks == -1)
				getSkullManager().setTicks(-1);
			else
				getSkullManager().setTicks(oldSkullTicks + World.getTicks());
			//ArrayList Loading doesn't work yet for Victims and Attackers...
			//NVM^
			skills.setExperienceCounter(buffer.getInt());
			pkKills = buffer.getInt();
			pkDeaths = buffer.getInt();
			pkPoints = buffer.getInt();
			pvpZoneEp = buffer.getInt();
			personalCombatXpRate = buffer.getInt();
			if (personalCombatXpRate > 100)
				personalCombatXpRate = 5000;
			if (personalCombatXpRate == 100)
				personalCombatXpRate = 100;
			if (personalCombatXpRate == 4)
				personalCombatXpRate = 4;
			//
		

			//FRIENDS & CLAN:
			settings.setPrivateChatSetting(buffer.getShort());
			settings.setPrivateTextColor(buffer.getShort());
			settings.setClanChatTextColor(buffer.getShort());
			int friendLoop = buffer.get() & 0xFF;
			for(int i = 0; i < friendLoop; i++) {
				friendManager.getFriends().add(BufferUtils.readRS2String(buffer));
			}
			if (buffer.get() == 1) {
				setAttribute("clanToJoin", BufferUtils.readRS2String(buffer));
			}

			//BARROWS:
			if (buffer.get() == 1)
				setAttribute("newBarrowsRun", true);
			if (buffer.get() == 1)
				setAttribute("canLootBarrowsChest", Boolean.TRUE);
			if (buffer.get() == 1)
				setAttribute("looted_barrows_request_shake", Boolean.TRUE);
			int size = buffer.get();
			for (int i = 0; i < size; i++) {
				getSettings().getBarrowsKilled().add(buffer.getInt());
			}
			int length = buffer.get();
			for (int i = 0; i < length; i++) {
				if (buffer.get() == 1)
					getSettings().getKilledBrothers()[i] = true;
			}
			getSettings().setBarrowsKillcount(buffer.getInt());
			getSettings().setTunnelId(buffer.getInt());
			getSettings().setTunnelEntranceId(buffer.getInt());

			//NOTES:
			int amount = buffer.get();
			for(int i = 0; i < amount; i++){
				String text = BufferUtils.readRS2String(buffer);
				int color = buffer.get();
				notes.addNote(text, color);
			}

			// EXTRA:
			setTitle(buffer.getShort());
			setDoubleXpTimer(buffer.getLong() + System.currentTimeMillis());
			getWalkingQueue().setRunEnergy(buffer.getShort());
			settings.setAcceptAidOn(buffer.get() == 1);
			settings.setLastXAmount(buffer.getInt());
			settings.setLastSelection(buffer.get());
			settings.setGodEntranceRope(buffer.get() == 1);
			for (int i = 0; i < 4; i++) {
				settings.getStrongholdChest()[i] = buffer.get() == 1;
			}
			if (buffer.get() == 1) {
				slayer.setSlayerTask(new SlayerTask(Master.values()[buffer
						.get()], buffer.get(), buffer.getInt(), buffer.getInt()));
			}
			slayerPoints = buffer.getInt();
			settings.setGraveStone(buffer.get());
			canUseQuickBankerAtHome = buffer.get() == 1;
			canUsePaint = buffer.get() == 1;
			savedX = buffer.getShort();
			savedY = buffer.getShort();
			savedZ = buffer.get();
			/*if (buffer.remaining() > 0) {

			}*/
		}
		//OKAY so we add all the way to the bottom
		//if you the value is a boolean use a byte but if the value is going to be greater then 128 use a short and if its greater then 32768 use an int

		//what do you need saved player.getSettings().hasGodwarsRope();

	}

	public void save(ChannelBuffer buffer) { // we use a dynamic buffer
		BufferUtils.writeRS2String(buffer, getPassword());
		//LOCATION:
		buffer.writeShort((short) getLocation().getX());
		buffer.writeShort((short) getLocation().getY());
		buffer.writeByte((byte) getLocation().getZ());

		//ACCOUNT DETAILS:
		buffer.writeByte(hasDisplayName() ? 1 : 0);
		if (hasDisplayName()) {
			BufferUtils.writeRS2String(buffer, displayName);
			buffer.writeLong(displayNameTime);
		}
		buffer.writeByte(hasReceivedStarter ? 1 : 0);
		buffer.writeByte(hasSetAppearance ? 1 : 0);
		BufferUtils.writeRS2String(buffer, firstPassword);
		BufferUtils.writeRS2String(buffer, previousPassword);
		buffer.writeLong(creationDateInMillis);
		BufferUtils.writeRS2String(buffer, accountCreationIp);
		BufferUtils.writeRS2String(buffer, lastConnectIp);
		//BufferUtils.writeRS2String(buffer, lastCountry);
		buffer.writeLong(lastConnectDateInMillis);
		buffer.writeInt(getDonor());

		//APPEARANCE:
		for (int i = 0; i < getAppearance().getLook().length; i++) {
			buffer.writeShort(getAppearance().getLook()[i]);
		}
		for (int i = 0; i < getAppearance().getColour().length; i++) {
			buffer.writeShort(getAppearance().getColour()[i]);
		}
		buffer.writeByte(getAppearance().getGender());

		//SKILLS & INVENTORY & EQUIPMENT & BANK:
		for (int i = 0; i < Skills.SKILL_COUNT; i++) {
			buffer.writeByte((byte) skills.getLevel(i));
			buffer.writeInt((int) skills.getXp(i));
		}
		for (int i = 0; i < Inventory.SIZE; i++) {
			Item item = inventory.get(i);
			if (item == null) {
				buffer.writeShort((short) -1);
			} else {
				buffer.writeShort((short) item.getId());
				buffer.writeInt(item.getAmount());
			}
		}
		for (int i = 0; i < Equipment.SIZE; i++) {
			Item item = equipment.get(i);
			if (item == null) {
				buffer.writeShort((short) -1);
			} else {
				buffer.writeShort((short) item.getId());
				buffer.writeInt(item.getAmount());
			}
		}
		for (int i = 0; i < Bank.SIZE; i++) {
			Item item = bank.getContainer().get(i);
			if (item == null) {
				buffer.writeShort((short) -1);
			} else {
				buffer.writeShort((short) item.getId());
				buffer.writeInt(item.getAmount());
			}
		}
		for (int i = 0; i < Bank.TAB_SIZE; i++) {
			buffer.writeInt(bank.getTab()[i]);
		}

		//FAMILIAR:
		if (getFamiliar() != null && !getFamiliar().isDead()) {
			buffer.writeInt(getFamiliar().getId());
			buffer.writeInt((getFamiliar().getTicks()) - World.getTicks());
			buffer.writeInt(getFamiliar().getSpecialPoints());
			buffer.writeShort(getFamiliar().getHitPoints());
			if (getFamiliar().isBeastOfBurden()) {
				for (int i = 0; i < 30; i++) { //beast of burden
					Item item = getFamiliar().getContainer().get(i);
					if (item == null) {
						buffer.writeShort((short) -1);
					} else {
						buffer.writeShort((short) item.getId());
						buffer.writeInt(item.getAmount());
					}
				}
			}
		} else
			buffer.writeInt(-1);
		buffer.writeInt(getSettings().getSummoningOption());

		//INTERFACES & CONFIGS:
		buffer.writeShort((short) settings.getSpellBook());
		buffer.writeByte((byte) (prayer.isAncientCurses() ? 1 : 0));
		buffer.writeByte(getWalkingQueue().isRunningBoth() ? 1 : 0);
		buffer.writeInt(lastBankTab);
		buffer.writeByte(getAttribute("sortLevel", true) ? 1 : 0);
		buffer.writeByte(getAttribute("sortCombat", false) ? 1 : 0);
		buffer.writeByte(getAttribute("sortTeleport", false) ? 1 : 0);
		buffer.writeByte(getAttribute("showSkill", true) ? 1 : 0);
		buffer.writeByte(getAttribute("showCombat", true) ? 1 : 0);
		buffer.writeByte(getAttribute("showTeleport", true) ? 1 : 0);
		buffer.writeByte(getAttribute("showMisc", true) ? 1 : 0);
		buffer.writeByte(getAttribute("defensiveCast", false) ? 1 : 0);
		buffer.writeInt(getMagicAutocast());

		//HITPOINTS & PRAYER:
		buffer.writeShort((short) skills.getHitPoints());
		buffer.writeByte((byte) Math.ceil(skills.getPrayerPoints()));
		for(int i = 0; i < 30; i++) {
			if(i >= 20 && prayer.getPrayerBook() == 1) {
				buffer.writeByte(0);
			} else {
				buffer.writeByte(prayer.getQuickPrayers()[prayer.getPrayerBook()][i] ? 1 : 0);
			}
		}

		//COMBAT:
		buffer.writeByte(settings.getCombatStyle());
		buffer.writeByte(settings.getCombatType());
		buffer.writeByte(settings.isAutoRetaliate() ? 1 : 0);
		buffer.writeShort(settings.getSpecialAmount());
		if(getPoisonManager().isPoisoned()) {
			buffer.writeShort(getPoisonManager().getCurrentPoisonAmount());
		} else {
			buffer.writeShort(0);
		}
		if (getAttribute("teleblock") != null)
			buffer.writeInt(getAttribute("teleblock", 0) - World.getTicks());
		else
			buffer.writeInt(-1);
		if (getAttribute("teleblockImmunity") != null)
			buffer.writeInt(getAttribute("teleblockImmunity", 0) - World.getTicks());
		else
			buffer.writeInt(-1);
		if (getSkullManager().isSkulled())
			buffer.writeInt((getSkullManager().getTicks()) - World.getTicks());
		else
			buffer.writeInt(-1);
		//int i = 0; //ArrayList Saving doesn't work yet for Victims and Attackers...
		//for (Player playersVictim : getSkullManager().getVictims()) {
		//BufferUtils.writeRS2String(buffer, playersVictim//.getUsername());
		//i++;
		//} buffer.writeInt(i);
		buffer.writeInt(skills.getExperienceCounter());
		buffer.writeInt(pkKills);
		buffer.writeInt(pkDeaths);
		buffer.writeInt(pkPoints);
		buffer.writeInt(pvpZoneEp);
		buffer.writeInt(personalCombatXpRate);

		//FRIENDS & CLAN:
		buffer.writeShort(settings.getPrivateChatSetting());
		buffer.writeShort(settings.getPrivateTextColor());
		buffer.writeShort(settings.getClanChatTextColor());
		buffer.writeByte((byte) friendManager.getFriends().size());
		for(String string : friendManager.getFriends()) {
			BufferUtils.writeRS2String(buffer, string);
		}
		buffer.writeByte(settings.getCurrentClan() != null ? 1 : 0);
		if (settings.getCurrentClan() != null) {
			BufferUtils.writeRS2String(buffer, settings.getCurrentClan().getOwner());
		}

		//BARROWS:
		buffer.writeByte(getAttribute("newBarrowsRun", false) ? 1 : 0);
		buffer.writeByte(getAttribute("canLootBarrowsChest", Boolean.FALSE) ? 1 : 0);
		buffer.writeByte(getAttribute("looted_barrows_request_shake", Boolean.FALSE) ? 1 : 0);
		buffer.writeByte(getSettings().getBarrowsKilled().size());
		for (int i : getSettings().getBarrowsKilled()) {
			buffer.writeInt(i);
		}
		buffer.writeByte(getSettings().getKilledBrothers().length); //I know this value will be the same for now, but what if we add 7th brother.
		for (int i = 0; i < getSettings().getKilledBrothers().length; i++) {
			buffer.writeByte(getSettings().getKilledBrothers()[i] ? 1 : 0);
		}
		buffer.writeInt(getSettings().getBarrowsKillcount());
		buffer.writeInt(getSettings().getTunnelId());
		buffer.writeInt(getSettings().getTunnelEntranceId());

		//NOTES:
		buffer.writeByte(getNotes().getList().size());
		for(Note n : getNotes().getList()) {
			BufferUtils.writeRS2String(buffer, n.getText());
			buffer.writeByte(n.getColor());
		}

		//EXTRA:
		buffer.writeShort(title);
		buffer.writeLong(doubleXpTimer - System.currentTimeMillis());
		buffer.writeShort(getWalkingQueue().getRunEnergy());
		buffer.writeByte(settings.getAcceptAidOn() ? 1 : 0);
		buffer.writeInt(settings.getLastXAmount());
		buffer.writeByte(settings.getLastSelection());
		buffer.writeByte(settings.hasGodEntranceRope() ? 1 : 0); // DONE!
		for (int i = 0; i < 4; i++) {
			buffer.writeByte(settings.getStrongholdChest()[i] ? 1 : 0);
		}
		buffer.writeByte(slayer.getSlayerTask() != null ? 1 : 0);
		if(slayer.getSlayerTask() != null) {
			buffer.writeByte(slayer.getSlayerTask().getMaster().ordinal());
			buffer.writeByte(slayer.getSlayerTask().getTaskId());
			buffer.writeInt(slayer.getSlayerTask().getTotalTaskAmount());
			buffer.writeInt(slayer.getSlayerTask().getCurrentTaskAmount());
		}
		buffer.writeInt(slayerPoints);
		buffer.writeByte(settings.getGraveStone());
		buffer.writeByte(canUseQuickBankerAtHome ? 1 : 0);
		buffer.writeByte(canUsePaint ? 1 : 0);
		buffer.writeShort((short) savedX);
		buffer.writeShort((short) savedY);
		buffer.writeByte((byte) savedZ);
	}

	private void addObjects() {
		if (settings.hasGodEntranceRope()) {
			ObjectManager.addCustomObject(this, 26341, 2917, 3745, 0, 10, 0);
		}
	}

	public void setSpecialAmount(int amt) {
		settings.setSpecialAmount(amt);
		ActionSender.sendConfig(this, 300, amt);
	}

	public void reverseSpecialActive() {
		settings.setUsingSpecial(!settings.isUsingSpecial());
		ActionSender.sendConfig(this, 301, settings.isUsingSpecial() ? 1 : 0);
	}

	public void deductSpecial(int amt) {
		setSpecialAmount(settings.getSpecialAmount() - amt);
	}

	public int getSpecialAmount() {
		return settings.getSpecialAmount();
	}

	public void setOnline(boolean isOnline) {
		this.isOnline = isOnline;
	}

	public boolean isOnline() {
		return isOnline;
	}

	public void setInvisible(boolean invisible) {
		this.isInvisible = invisible;
		//updates Friends List for people who have the invisible player on their Friends List:
		String name = getFormattedUsername();
		for (Player pl : World.getWorld().getPlayers()) {
			if (pl.getFriendManager().getFriends().contains(name)) {
				pl.getFriendManager().updateFriend(name, this);
			}
		}
		for (Player pl : World.getWorld().getLobbyPlayers()) {
			if (pl.getFriendManager().getFriends().contains(name)) {
				pl.getFriendManager().updateFriend(name, this);
			}
		}
	}

	public boolean isInvisible() {
		return isInvisible;
	}

	public boolean hasItem(int itemId) {
		if (getInventory().contains(itemId))
			return true;
		if (getBank().contains(itemId))
			return true;
		if (getFamiliar() != null && getFamiliar().isBeastOfBurden()
				&& getFamiliar().getContainer().contains(new Item (itemId, 1)))
			return true;
		if (getTradeSession() != null && getTradeSession().getPlayerItemsOffered(this).contains(new Item(itemId, 1)))
			return true;
		if (getPriceCheck().isOpen() && getPriceCheck().getContainer().contains(new Item(itemId, 1)))
			return true;
		if (getActivity() instanceof DuelActivity) {
			Container playerStake = ((Stakes) getAttribute("duelStakes")).getContainer();
			if (playerStake.contains(new Item(itemId, 1)))
				return true;
		}
		if (GroundItemManager.getAllGroundItemsFromPlayer(itemId, this) != null) {
			for (GroundItem item : GroundItemManager.getAllGroundItemsFromPlayer(itemId, this)) {
				if (item != null)
					return true;
			}
		}
		return false;
	}

	public void forceText(String text) {
		mask.setLastForceText(new ForceText(text));
	}

	public Appearance getAppearance() {
		return appearance;
	}

	public Equipment getEquipment() {
		return equipment;
	}

	public Skills getSkills() {
		return skills;
	}

	public Prayer getPrayer() {
		return prayer;
	}

	public PlayerUpdate getGpi() {
		return gpi;
	}

	public Bank getBank() {
		return bank;
	}

	public void setLastBankTab(int tabId) {
		lastBankTab = tabId;
	}

	public int getLastBankTab() {
		return lastBankTab;
	}

	public NpcUpdate getGni() {
		return gni;
	}

	public boolean itemName(String string) {
		if (equipment.get(Equipment.SLOT_WEAPON) == null) {
			return false;
		}
		return this.getEquipment().get(Equipment.SLOT_WEAPON).getDefinition()
				.getName().toLowerCase().contains(string);
	}

	@Override
	public int getAttackAnimation() {
		return CombatUtils.getAttackAnimation(this);
	}

	@Override
	public int getAttackDelay() {
		int sword = equipment.get(3) != null ? equipment.get(3).getId() : -1;
		int speed = 5;
		if (sword == -1) {
			speed = 5;
		} else {
			ItemDefinition def = ItemDefinition.forId(sword);
			if (def.getAttackSpeed() > 0) {
				speed = def.getAttackSpeed();
			}
			if (settings.getCombatStyle() == WeaponInterface.STYLE_RAPID) {
				speed--;
			}
		}
		return speed;
	}

	@Override
	public int getDefenceAnimation() {
		Item shield = equipment.get(Equipment.SLOT_SHIELD);
		if (shield != null) {
			String name = shield.getDefinition().getName().toLowerCase();
			if (name.endsWith("shield"))
				return 1156;
			else if (name.endsWith("defender"))
				return 4177;
		}
		Item weapon = equipment.get(Equipment.SLOT_WEAPON);
		if (weapon != null) {
			String name = weapon.getDefinition().getName();
			if (name.contains("inchompa")) {
				return 3176;
			}
			if (name.contains("scimitar") || name.contains("Darklight"))
				return 12030;
			if (name.contains("2h"))
				return 7050;
			if (name.contains("rapier"))
				return 388;
			if (name.contains("longsword"))
				return 13042;
			if (name.contains("warhammer"))
				return 403;
			switch (weapon.getId()) {
			case 10034:
				return 3176;
			case 19784: // korasi's
				return 12030;
			case 11694:
			case 11696:
			case 11698:
			case 16909:
			case 11700:
				return 7050;
			case 14484:
				return 404;
			case 4151:
			case 15441:
			case 15442:
			case 15443:
			case 15444:
				return 11974;
			case 13867:
			case 13869:
			case 13941:
			case 13943:
				return 404;
			case 15486:
				return 12806;
			case 18353:
				return 13054;
			case 14679:
				return 403;
			case 4068:
			case 4503:
			case 4508:
			case 18705:
				return 388;
			case 6908:
			case 6910:
			case 6912:
			case 6914:
			case 6526:
				return 420;
			case 6528:
				return 1666;
			case 11716:
				return 12008;
			case 15241:
				return 12156;
			}
		}
		return 424;
	}

	public Bonuses getBonuses() {
		return bonuses;
	}

	public Settings getSettings() {
		return settings;
	}

	public FriendManager getFriendManager() {
		return friendManager;
	}

	public RegionData getRegion() {
		return region;
	}

	public PlayerDefinition getDefinition() {
		return definition;
	}

	public TradeSession getTradeSession() {
		if (this.currentTradeSession != null) {
			return currentTradeSession;
		} else if (this.tradePartner != null) {
			return tradePartner.getTradeSession();
		} else {
			return null;
		}
	}

	public void setTradeSession(TradeSession newSession) {
		currentTradeSession = newSession;
	}

	public void setTradePartner(Player tradePartner) {
		this.tradePartner = tradePartner;
	}

	public Player getTradePartner() {
		return tradePartner;
	}

	public void setSpellBook(int book) {
		ActionSender.sendConfig(this, 108, -1);
		resetCombat();
		removeAttribute("autoCastSpell");
		getEquipment().calculateType();
		settings.setSpellBook(book);
		ActionSender.sendInterface(this, 1,
				connection.getDisplayMode() < 2 ? 548 : 746,
						connection.getDisplayMode() < 2 ? 209 : 94,
								settings.getSpellBook());
		ActionSender.organizeSpells(this);
	}

	public void refreshSpellBook() {
		ActionSender.sendConfig(this, 108, -1);
		removeAttribute("autoCastSpell");
		getEquipment().calculateType();
		ActionSender.sendInterface(this, 1,
				connection.getDisplayMode() < 2 ? 548 : 746,
						connection.getDisplayMode() < 2 ? 209 : 94,
								settings.getSpellBook());
		ActionSender.organizeSpells(this);
	}

	public void sendMessage(String string) {
		if (string == null) {
			return;
		}
		if (connection.isInLobby()) {
			ActionSender.sendChatMessage(this, 11, string);
		} else {
			ActionSender.sendMessage(this, string);
		}
	}

	public void reverseAutoRetaliate() {
		getCombatExecutor().reset();
		settings.setAutoRetaliate(!isAutoRetaliating());
		ActionSender.sendConfig(this, 172, isAutoRetaliating() ? 0 : 1);
	}

	@Override
	public Player getPlayer() {
		return this;
	}

	@Override
	public boolean isPlayer() {
		return true;
	}

	/**
	 * Gets the player's display name formatted.
	 * 
	 * @return The formatted display name.
	 */
	public String getFormattedName() {
		StringBuilder s = new StringBuilder();
		s.append(Character.toUpperCase(definition.getName().charAt(0)));
		return s.append(definition.getName().substring(1)).toString();
	}

	public void write(Message message) {
		if (connection == null || connection.getChannel() == null) {
			return;
		}
		// new Throwable().printStackTrace();
		if (connection.getChannel().isConnected()) {
			connection.getChannel().write(message);
		}
	}

	@Override
	public int getHitPoints() {
		return skills.getHitPoints();
	}

	@Override
	public int getMaximumHitPoints() {
		return skills.getMaximumLifePoints();
	}

	public PlayerAreaTick getPlayerArea() {
		return playerAreaTick;
	}

	public DementhiumHandler getHandler() {
		if (handler == null) {
			handler = connection.getChannel().getPipeline()
					.get(DementhiumHandler.class);
		}
		return handler;
	}

	/*
	 * public void setFamiliar(Familiar familiar) { this.familiar = familiar; }
	 * 
	 * public Familiar getFamiliar() { return familiar; }
	 */

	/**
	 * @return the Familiar
	 */
	public Familiar getFamiliar() {
		return familiar;
	}
	/**
	 * @return the Familiar
	 */
	public void setFamiliar(Familiar familiar) {
		this.familiar = familiar;
	}

	/**
	 * @return the Beast of Burden Familiar
	 */
	/*public void setBeastOfBurden(BeastOfBurden familiar) {
		this.beastOfBurden = familiar;
	}*/

	/**
	 * @return the Beast of Burden Familiar
	 */
	/*public BeastOfBurden getBeastOfBurden() {
		return beastOfBurden;
	}*/

	/**
	 * @return the priceCheck
	 */
	public PriceCheck getPriceCheck() {
		return priceCheck;
	}

	public int getViewDistance() {
		return viewDistance;
	}

	public void setViewDistance(int distance) {
		this.viewDistance = distance;
	}

	public void incrementViewDistance() {
		viewDistance++;
	}

	public PlayerDefinition getPlayerDefinition() {
		return definition;
	}

	public boolean isTeamMate(Player partner) {
		if (getActivity() != null && partner.getActivity() != null) {
			return !getActivity().isCombatActivity(partner, this, false);
		}
		return false;
	}

	public void fullRestore() {
		removeAttribute("overloads");
		setAttribute("vengeance", false);
		getCombatExecutor().setVictim(null);
		setSpecialAmount(1000);
		getSettings().setUsingSpecial(false);
		getWalkingQueue().reset();
		getSkills().completeRestore();
		getWalkingQueue().setRunEnergy(100);
		getPrayer().closeAllPrayers();
		getPoisonManager().removePoison();
		ActionSender.sendConfig(this, 491, 0);// Disable tiara config on reset
		animate(Animation.RESET);
		graphics(Graphic.RESET);
		getEquipment().recalculateHpModifier();
		getEquipment().refresh();
		resetCombat();
		getMaximumHitPoints();
	}

	public Notes getNotes() {
		return notes;
	}

	public Slayer getSlayer() {
		return slayer;
	}
	public Inventory getInventory() {
		return inventory;
	}

	public boolean hasReceivedStarter() {
		return hasReceivedStarter;
	}

	public void setHasReceivedStarter(boolean value) {
		hasReceivedStarter = value;
	}

	public boolean hasSetAppearance() {
		return hasSetAppearance;
	}

	public void setHasSetAppearance(boolean value) {
		hasSetAppearance = value;
	}

	private String lastCountry = Locale.getDefault().getDisplayCountry();
	public String getLastCountry() {
		return lastCountry;
	}

	/**
	 * @return the lastConnectIp
	 */
	public String getAccountCreationIp() {
		return accountCreationIp;
	}

	/**
	 * @param lastConnectIp
	 *            the lastConnectIp to set
	 */
	public void setAccountCreationIp(String ip) {
		this.accountCreationIp = ip;
	}

	/**
	 * @return the lastConnectIp
	 */
	public String getLastConnectIp() {
		return lastConnectIp;
	}

	/**
	 * @param lastConnectIp
	 *            the lastConnectIp to set
	 */
	public void setLastConnectIp(String ip) {
		this.lastConnectIp = ip;
	}

	public void setConnection(GameSession connection) {
		this.connection = connection;
	}

	public void setViewportDepth(int depth) {
		if (depth < 0 || depth > 3) {
			return;
		}
		this.viewportDepth = depth;
	}

	public int getViewportDepth() {
		return viewportDepth;
	}

	public void setRenderAnimation(int renderAnimation) {
		this.renderAnimation = renderAnimation;
		mask.setAppearanceUpdate(true);
	}

	public void resetRenderAnimation() {
		this.renderAnimation = -1;
		mask.setAppearanceUpdate(true);
	}

	public int getRenderAnimation() {
		return renderAnimation;
	}

	public void setCanUseQuickBankerAtHome(boolean allow) {
		this.canUseQuickBankerAtHome = allow;
	}

	public boolean getCanUseQuickBankerAtHome() {
		return canUseQuickBankerAtHome;
	}

	public void setCanUsePaint(boolean allow) {
		this.canUsePaint = allow;
	}

	public boolean getCanUsePaint() {
		return canUsePaint;
	}

	public int getSavedX() {
		return savedX;
	}

	public void setSavedX(int x) {
		this.savedX = x;
	}

	public int getSavedY() {
		return savedY;
	}

	public void setSavedY(int y) {
		this.savedY = y;
	}

	public int getSavedZ() {
		return savedZ;
	}

	public void setSavedZ(int z) {
		this.savedZ = z;
	}

	public void setKeepItemsOnDeath(boolean keepItems) {
		if (getRights() >= 2)
			this.keepItemsOnDeath = keepItems;
	}

	public boolean getKeepItemsOnDeath() {
		return keepItemsOnDeath;
	}

	public void setRewardItemsDroppedOnDeath(Container items) {
		this.rewardItemsOnDeath = items;
	}

	public Container getRewardItemsDroppedOnDeath() {
		return rewardItemsOnDeath;
	}


	//TODO:
	//(if last hit is bigger than 500, for every 100 hit more + 1 point)
	//make a shop that u can buy stuff with pkpoints
	public void handlePkStatistics(Player departed, Player lastHitter) {
		//this = killer

		//KILL / DEATH STATISTICS:
		if (departed.getPkDeaths() + 1 > 0)
			departed.pkDeaths++;
		if (pkKills + 1 > 0)
			pkKills++;
		//if (lastHitter.getPkKills() + 1 > 0)
		//lastHitter doesn't get anything? -lastHitter.pkKills++;

		//LASTHITTER PKPOINTS:
		if (!lastHitter.getUsername().equals(getUsername())) {
			if (lastHitter.getPkPoints() == Integer.MAX_VALUE) {
				lastHitter.pvpZoneEp = 0;
				lastHitter.sendMessage("You receive no pk points, as you've reached the maximum amount of pk points.");
			} else {
				//TODO: make a proper way for the lastHitterToReceive his reward, instead of just giving 2 pkPoints.
				int lastHitterReward = 2;
				if (departed.getRights() >= 2)
					lastHitterReward += 5;
				Container[] keptItems = ItemsKeptOnDeath.getDeathContainers(lastHitter);
				int riskedWealth = ItemsKeptOnDeath.getRiskedWealth(keptItems[1]);
				lastHitterReward += ((int) Math.floor(riskedWealth / 200000000));
				if (lastHitter.getDonor() > 0)
					Math.ceil(lastHitterReward *= 0.3); //does that work?
				if (lastHitter.getPkPoints() + lastHitterReward < 0) {
					lastHitter.sendMessage("You receive a reduced amount of pk points, as you've reached the maximum amount ");
					lastHitter.sendMessage("of pk points.");
				} else
					lastHitter.sendMessage("You have been awarded "+lastHitterReward+" pk points for your efforts.  You now have a total of "+(lastHitter.getPkPoints()+lastHitterReward)+" pk points.");
				lastHitter.addPkPoints(lastHitterReward);
				lastHitter.pvpZoneEp = 0;
			}
		}

		//KILLER PKPOINTS:
		if (pkPoints == Integer.MAX_VALUE) {
			lastHitter.pvpZoneEp = 0;
			lastHitter.targetLikelihood = 5;
			sendMessage("You receive no pk points, as you've reached the maximum amount of pk points.");
		} else {
			int reward = 1;
			if (departed.getRights() >= 2)
				reward += 10;
			Container[] keptItems = ItemsKeptOnDeath.getDeathContainers(this);
			int riskedWealth = ItemsKeptOnDeath.getRiskedWealth(keptItems[1]);
			reward += ((int) Math.floor(riskedWealth / 100000000));
			/*if (getUsername().equals(lastHitter.getUsername())) {
				departed.getLastReceivedHit().getType();
				player.sendMessage("Your melee maximum hit is "
						+ MeleeFormulae.getMeleeDamage(player, 1.0) + ".");
				player.sendMessage("Your ranged maximum hit is "
						+ RangeFormulae.getRangeDamage(player, 1.0) + ".");

				int hitReward = ((int) departed.getLastReceivedHit() - 500);
				reward += hitReward;
			}*/
			if (getDonor() > 0)
				reward *= 2;
			if (pkPoints + reward < 0) {
				sendMessage("You receive a reduced amount of pk points, as you've reached the maximum amount ");
				sendMessage("of pk points.");
			} else {
				sendMessage("You have been awarded "+reward+" pk points. You now have a total of "+(pkPoints+reward)+" pk points.");
			}
			addPkPoints(reward);
			lastHitter.pvpZoneEp = 0;
			lastHitter.targetLikelihood = 5;
			lastHitter.EpDrop();
		}
	}

	public int getPkPoints() {
		return pkPoints;
	}

	public void addPkPoints(int amountToAdd) {
		int pkpoints = pkPoints;
		if (pkpoints + amountToAdd < 0 && amountToAdd > 0)
			pkPoints = Integer.MAX_VALUE;
		else
			pkPoints = pkpoints + amountToAdd;
	}

	public int getPkKills() {
		return pkKills;
	}

	public int getPkDeaths() {
		return pkDeaths;
	}

	public void setPkKills(int amount) {
		pkKills = amount;
	}

	public void setPkDeaths(int amount) {
		pkDeaths = amount;
	}

	public int getPersonalCombatXpRate() {
		return personalCombatXpRate;
	}

	public void setPersonalCombatXpRate(int rate) {
		this.personalCombatXpRate = rate;
	}

	public int getSlayerPoints() {
		return slayerPoints;
	}

	public void addSlayerPoints(int amountToAdd) {
		int slayerpoints = slayerPoints;
		if (slayerpoints + amountToAdd < 0 && amountToAdd > 0)
			slayerPoints = Integer.MAX_VALUE;
		else
			slayerPoints = slayerpoints + amountToAdd;
	}

	@Override
	public boolean isAttackable(Mob mob) {
		Mob lastAttacker = getCombatExecutor().getLastAttacker();
		if (lastAttacker != null && lastAttacker != mob
				&& (!isMulti() || !mob.isMulti())) {
			if (mob.isPlayer()) {
				mob.getPlayer()
				.sendMessage("That player is already in combat.");
			}
			return false;
		} else if (mob.getAttribute("combatTicks", -1) - 8 > World.getTicks()
				&& mob.getCombatExecutor().getLastAttacker() != this
				&& (!isMulti() || !mob.isMulti())) {
			if (mob.isPlayer()) {
				mob.getPlayer().sendMessage("You are already under attack.");
			}
			return false;
		}
		if (getActivity().isRunning()
				&& getActivity().isCombatActivity(mob, this, false)) {
			return true;
		}
		else if (getActivity().isRunning()
				&& !getActivity().isCombatActivity(mob, this, false)) {
			return false;
		}
		if (mob.isPlayer()) {
			if (!mob.getPlayer().isInWilderness() && !mob.getPlayer().inPVPZone() && !mob.getPlayer().inSafePk()) {
				mob.getPlayer()
				.sendMessage(
						"You can only attack players in a player-vs-player area.");
				return false;
			} else if (!isInWilderness() && !inPVPZone() && !inSafePk()) {
				mob.getPlayer()
				.sendMessage(
						"You can only attack players in a player-vs-player area.");
				return false;
			}
			if ((World.getWorld().getAreaManager().getAreaByName("Magearena").contains(getLocation())
					|| World.getWorld().getAreaManager().getAreaByName("Magearena").contains(mob.getLocation()))
					&& mob.getPlayer().getCombatAction().getCombatType() != CombatType.MAGIC) {
				mob.getPlayer()
				.sendMessage(
						"You can only use Magic to attack players in this area.");
				return false;
			}
			if (!inPVPZone() && !mob.getPlayer().inPVPZone() && !inSafePk() && !mob.getPlayer().inSafePk()) {
				int combatLevel = getSkills().getCombatLevelWithoutSummoning();
				int otherLevel = mob.getPlayer().getSkills().getCombatLevelWithoutSummoning();
				int wildernessLevel = getLocation().getWildernessLevel();
				int otherWildernessLevel = mob.getLocation().getWildernessLevel();
				if (!((combatLevel + wildernessLevel >= otherLevel && combatLevel
						- wildernessLevel <= otherLevel)
						&& (otherLevel + otherWildernessLevel) >= combatLevel && otherLevel
						- otherWildernessLevel <= combatLevel)) {
					mob.getPlayer()
					.sendMessage(
							"The combat level difference between you and your opponent is too great.");
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public Damage updateHit(Mob source, int hit, CombatType type) {
		if (source.isPlayer() && type == CombatType.MELEE
				&& getAttribute("spearWall", -1) > World.getTicks()) {
			ActionSender.sendMessage(this,
					"Your spear wall deflects the damage.");
			return new Damage(0);
		}

		//DEFLECTING:
		int deflected = 0;
		if (getPrayer().usingPrayer(1, type.getDeflectCurse())) {
			int deflectchance = Misc.random(2);
			if (deflectchance == 0) { //1/3 chance (random chance I made up)
				if (hit >= 10) {
					deflected = (int) (hit * 0.1);
				}
			}
			hit *= source.isPlayer() ? 0.6 : 0;

		} else if (getPrayer().usingPrayer(0, type.getProtectionPrayer())) {
			hit *= source.isPlayer() ? 0.6 : 0;
		}
		if (type == CombatType.MELEE
				&& getAttribute("staffOfLightEffect", -1) > World.getTicks()) {
			ActionSender.sendMessage(this,
					"Your staff of light deflects some damage.");
			hit *= 0.5;
		}
		if (type == CombatType.DRAGONFIRE) {
			hit = CombatUtils.getDragonProtection(this, source, hit);
		}
		if ((int) getSkills().getPrayerPoints() > 0
				&& getEquipment().getSlot(Equipment.SLOT_SHIELD) == 13740) {
			double decrease = hit * .3;
			double prayerDecrease = Math.ceil(decrease / 20);
			if (getSkills().getPrayerPoints() >= prayerDecrease) {
				getSkills().drainPray(prayerDecrease);
				hit -= decrease;
			} else {
				hit -= getSkills().getPrayerPoints() * 40;
				getSkills().drainPray(9);
			}
		} else if (getRandom().nextInt(10) < 7
				&& getEquipment().getSlot(Equipment.SLOT_SHIELD) == 13742) {
			hit *= .75;
		}
		return new Damage(hit).setDeflected(deflected);
	}

	public boolean isActive() {
		return active;
	}

	@Override
	public CombatAction getCombatAction() {
		if (getAttribute("autocastId", -1) > -1
				|| getAttribute("spellId", -1) > -1) {
			return CombatType.MAGIC.getCombatAction();
		}
		if (getSettings().isUsingSpecial()) {
			SpecialAttack special = SpecialAttackContainer.get(getEquipment()
					.getSlot(3));
			if (special != null) {
				SpecialAction.getSingleton().setSpecialAttack(special);
				return SpecialAction.getSingleton();
			}
			System.out.println("Unhandled special attack for item id "
					+ getEquipment().getSlot(3) + ".");
		}
		Item weapon = getEquipment().get(3);
		if (weapon != null && RangeWeapon.get(weapon.getId()) != null) {
			return CombatType.RANGE.getCombatAction();
		}
		return CombatType.MELEE.getCombatAction();
	}

	@Override
	public RangeData getRangeData(Mob victim) {
		RangeData data = new RangeData(true);
		data.setWeapon(RangeWeapon.get(getEquipment().getSlot(
				Equipment.SLOT_WEAPON)));
		if (data.getWeapon() == null) {
			return null;
		}
		if (data.getWeapon().getAmmunitionSlot() > -1) {
			data.setAmmo(Ammunition.get(getEquipment().getSlot(
					data.getWeapon().getAmmunitionSlot(), 0)));
			if (getEquipment().get(
					data.getWeapon().getAmmunitionSlot()) == null
					/*|| getEquipment().get(
							data.getWeapon().getAmmunitionSlot()).getAmount() < neededArrowAmountForShot*/) {
				ActionSender.sendMessage(this, "You do not have enough ammo left.");
				return null;
			}
		}
		if (data.getAmmo() == null) {
			ActionSender.sendMessage(this, "This ammo hasn't been added yet.");
			return null;
		}
		if (!data.getWeapon().getAmmunition()
				.contains(data.getAmmo().getItemId())) {
			ActionSender.sendMessage(this, "You do not have enough ammo left.");
			return null;
		}
		data.setWeaponType(0);
		data.setDropAmmo(true);
		String name = ItemDefinition.forId(data.getWeapon().getItemId())
				.getName().toLowerCase();
		if (name.equals("dark bow")) {
			data.setWeaponType(2); // Dark bow.
		} else if (name.contains("chinchompa")) {
			data.setWeaponType(6);
			data.setDropAmmo(false);
		} else if (data.getWeapon().getAmmunitionSlot() == 3) {
			data.setWeaponType(3); // Thrown weapons.
		} else if (name.contains("rossbow") || name.contains("c'bow") || name.contains("aril's")) {
			data.setWeaponType(1); // Crossbows.
		} else if (name.equals("hand cannon")) {
			data.setWeaponType(4); // Hand cannon.
			data.setDropAmmo(false);
		}
		if (ItemDefinition.forId(data.getAmmo().getItemId()).getName()
				.contains("rystal bow")
				|| ItemDefinition.forId(data.getAmmo().getItemId()).getName()
				.contains("aryte bow")) {
			data.setDropAmmo(false);
			data.setWeaponType(5);
		}
		if (data.getWeaponType() == 1
				&& ItemDefinition.forId(data.getAmmo().getItemId()).getName()
				.contains("olt rack")) {
			data.setDropAmmo(false);
		}
		if (data.getWeaponType() != 1) {
			data.setDamage(Damage.getDamage(this, victim, CombatType.RANGE,
					RangeFormulae.getDamage(this, victim)));
		} else {
			data.setDamage(CombatUtils.getRangeDamage(this, victim,
					data.getAmmo()));
		}
		data.setProjectile(CombatUtils.getProjectile(this, victim,
				data.getWeaponType(), data.getAmmo().getProjectileId()));
		data.setAnimation(data.getWeapon().getAnimationId());
		data.setGraphics(data.getAmmo().getStartGraphics());
		if (data.getWeaponType() == 2) {
			if (getEquipment().get(data.getWeapon().getAmmunitionSlot())
					.getAmount() > 1) {
				data.setDamage2(Damage.getDamage(this, victim,
						CombatType.RANGE, RangeFormulae.getDamage(this, victim)));
				int speed = (int) (55 + (getPlayer().getLocation().distance(
						victim.getLocation()) * 10));
				data.setProjectile2(Projectile.create(this, victim, data
						.getAmmo().getProjectileId(), 40, 36, 41, speed, 25));
				data.setGraphics(data.getAmmo().getDarkBowGraphics());
			} else {
				data.setWeaponType(0);
			}
		}
		return data;
	}

	@Override
	public void retaliate(Mob other) {
		if (!settings.isAutoRetaliate()
				|| getCombatExecutor().getVictim() != null
				|| getWalkingQueue().isMoving()) {
			return;
		}
		getCombatExecutor().setVictim(other);
	}

	@Override
	public void preCombatTick(final Interaction interaction) {
		if (interaction.getSource().getPlayer().getUsername().equals("mod combat")) {
			interaction.getSource().getPlayer().sendMessage("[Player.java (ABSTRACT MOB)] PreCombatTick, sets damage (4+).");
		}
		super.preCombatTick(interaction);
		this.closeAll(false, true);
		if (interaction.getVictim().isPlayer()) {
			interaction.getVictim().getPlayer().closeAll(false, true);
		}
		if (interaction.getRangeData() != null) {
			interaction.setDamage(interaction.getRangeData().getDamage());
		}
		if (interaction.getDamage() != null
				&& getPrayer().usingPrayer(1, Prayer.SOUL_SPLIT)) {
			int ticks = (int) Math.floor(getLocation().distance(
					interaction.getVictim().getLocation()) * 0.5) + 1;
			int speed = (int) (46 + getLocation().distance(
					interaction.getVictim().getLocation()) * 10);
			if (interaction.getDamage().getHit() > 0) {
				getSkills().heal(
						(int) (interaction.getDamage().getHit() * (interaction
								.getVictim().isNPC() ? 0.2 : 0.4)));
				if (interaction.getVictim().isPlayer()) {
					interaction.getVictim().getPlayer().getSkills()
					.drainPray(interaction.getDamage().getHit() * 0.02);
				}
				ProjectileManager
				.sendProjectile(Projectile.create(this,
						interaction.getVictim(), 2263, 11, 11, 30,
						speed, 0, 0));
			}
			World.getWorld().submit(new Tick(ticks) {
				@Override
				public void execute() {
					int speed = (int) (46 + getLocation().distance(
							interaction.getVictim().getLocation()) * 10);
					interaction.getVictim().graphics(2264);
					ProjectileManager.sendProjectile(Projectile.create(
							interaction.getVictim(), interaction.getSource(),
							2263, 11, 11, 30, speed, 0, 0));
					stop();
				}
			});
		} else if (interaction.getDamage() != null
				&& getPrayer().usingPrayer(0, Prayer.SMITE)) {
			if (interaction.getVictim().isPlayer()) {
				interaction.getVictim().getPlayer().getSkills()
				.drainPray(interaction.getDamage().getHit() * 0.025);
			}
		}
	}

	@Override
	public void postCombatTick(Interaction interaction) {
		if (interaction.getSource().getPlayer().getUsername().equals("mod combat")) {
			interaction.getSource().getPlayer().sendMessage("[Player.java (ABSTRACT MOB)] PostCombatTick (5+).");
		}
		super.postCombatTick(interaction);
		if (getPrayer().usingPrayer(1, Prayer.TURMOIL)) {
			getPrayer().updateTurmoil(interaction.getVictim());
		}
		getSettings().incrementHitCounter();
	}

	public boolean isDoubleXp() {
		return getTimeLeft() > 1;
	}

	public long getTimeLeft() {
		return (doubleXpTimer - System.currentTimeMillis()) / 60000;
	}

	public void setDoubleXpTimer(long timer) {
		doubleXpTimer = timer;
	}

	/**
	 * @return the questStorage
	 */
	public QuestStorage getQuestStorage() {
		return questStorage;
	}

	/**
	 * @return the skullManager
	 */
	public SkullManager getSkullManager() {
		return skullManager;
	}

	public void refreshPing() {
		this.lastPing = System.currentTimeMillis();
	}

	public long getLastPing() {
		return System.currentTimeMillis() - lastPing;
	}

	public String getFormatedLastConnectDate() {
		DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy, HH:mm:ss");//.SSS");
		return dateFormat.format(lastConnectDateInMillis);
	}

	public long getRawLastConnectDate() {
		return lastConnectDateInMillis;
	}

	public void setLastConnectDate(long lastDateMillis) {
		this.lastConnectDateInMillis = lastDateMillis;
	}

	public String getFormatedAccountCreationDate() {
		DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy, HH:mm:ss");//.SSS");
		return dateFormat.format(creationDateInMillis);
	}

	public long getRawAccountCreationDate() {
		return creationDateInMillis;
	}

	public void setAccountCreationDate(long creationDateMillis) {
		if (creationDateInMillis == -1) //DEFAULT
			this.creationDateInMillis = creationDateMillis;
	}

	public void updateMap() {
		updateRegionArea();
		if (isAtDynamicRegion) {
			ActionSender.sendDynamicRegion(this);
		} else {
			ActionSender.updateMapRegion(this, true);
		}
	}

	public void updateRegionArea() {
		mapRegionIds = new ArrayList<Integer>();
		int regionX = location.getRegionX();
		int regionY = location.getRegionY();
		int mapHash = Location.VIEWPORT_SIZES[viewportDepth] >> 4;
			for (int xCalc = (regionX - mapHash) >> 3; xCalc <= (regionX + mapHash) >> 3; xCalc++) {
				for (int yCalc = (regionY - mapHash) >> 3; yCalc <= (regionY + mapHash) >> 3; yCalc++) {
					int regionId = yCalc + (xCalc << 8);
					if (RegionBuilder.getDynamicRegion(regionId) != null)
						isAtDynamicRegion = true;
					mapRegionIds.add(yCalc + (xCalc << 8));
				}
			}
	}

	public List<Integer> getMapRegionIds() {
		return mapRegionIds;
	}

	public boolean isInWilderness() {
		if (World.getWorld().getAreaManager().getAreaByName("RevenantsCave").contains(getLocation()))
			return true;
		if (World.getWorld().getAreaManager().getAreaByName("FireGiantsDungeon").contains(getLocation()))
			return true;
        if (World.getWorld().getAreaManager().getAreaByName("Revs").contains(getLocation()))
    	    return true;
		return World.getWorld().getAreaManager().getAreaByName("Wilderness").contains(getLocation());
	}

	public boolean inPVPZone() { //When you update this, also update this in Familiar.java (same method name)
		if (World.getWorld().getAreaManager().getAreaByName("RandomPVPZone").contains(getLocation()) || (World.getWorld().getAreaManager().getAreaByName("1v1pk").contains(getLocation())))
			return true;
		return false;
	}
	public boolean inSafePk() { //When you update this, also update this in Familiar.java (same method name)
		if (World.getWorld().getAreaManager().getAreaByName("SafePk").contains(getLocation()) ||  (World.getWorld().getAreaManager().getAreaByName("danger").contains(getLocation())))
			return true;
		return false;
	}
	public boolean inSafeZone() { //When you update this, also update this in Familiar.java (same method name)
		if (World.getWorld().getAreaManager().getAreaByName("FFA").contains(getLocation()) || (World.getWorld().getAreaManager().getAreaByName("danger1").contains(getLocation())))
			return true;
		return false;
	}
}