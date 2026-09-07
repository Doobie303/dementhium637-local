package org.dementhium.content.activity.impl;

import org.dementhium.content.activity.Activity;
import org.dementhium.content.activity.impl.duel.DuelConfigurations;
import org.dementhium.content.activity.impl.duel.DuelConfigurations.Rules;
import org.dementhium.content.activity.impl.duel.DuelConfigurations.TeleportLocations;
import org.dementhium.content.activity.impl.duel.Stakes;
import org.dementhium.model.Container;
import org.dementhium.model.Item;
import org.dementhium.model.Mob;
import org.dementhium.model.World;
import org.dementhium.model.mask.ForceText;
import org.dementhium.model.misc.IconManager;
import org.dementhium.model.player.Player;
import org.dementhium.net.ActionSender;
import org.dementhium.tickable.Tick;
import org.dementhium.util.Logger;

/**
 * Handles the activity: <code>Duel Arena</code>.
 *
 * @author Emperor
 */
public class DuelActivity extends Activity<Player> { //Inter 637 for friendly duel

	/**
	 * The current duel arena state.
	 *
	 * @author Emperor
	 */
	public static enum State {
		FIRST_SCREEN, SECOND_SCREEN, FIGHTING
	}

	/**
	 * The duel rules interface id.
	 */
	public static final short DUEL_RULES_INTERFACE = 631;

	/**
	 * The second duel interface id.
	 */
	public static final short DUEL_SECOND_INTERFACE = 626;

	/**
	 * Represents the other player in a duel.
	 */
	private final Player otherPlayer;

	/**
	 * Represents the duel configurations used during this session.
	 */
	private DuelConfigurations duelConfigurations;

	/**
	 * Represents the total stake, which will be received by the winner of this duel.
	 */
	private Container stakes;

	/**
	 * If the activity has commenced.
	 */
	private boolean commenced = false;

	/**
	 * If the activity is finished.
	 */
	private boolean finished = false;

	/**
	 * The current dueling state.
	 */
	private State currentState;

	/**
	 * Constructs a new {@code Duel activity} instance.
	 *
	 * @param challenger  The player challenging.
	 * @param otherPlayer The other player.
	 * @param stake       The rewards for the player who wins the duel.
	 */
	public DuelActivity(Player challenger, Player otherPlayer) {
		super(challenger);
		this.otherPlayer = otherPlayer;
		this.stakes = new Container(56, false, false, true);
		reset(otherPlayer, getPlayer());
		getPlayer().setActivity(Mob.DEFAULT_ACTIVITY);
		otherPlayer.setActivity(Mob.DEFAULT_ACTIVITY);
		getPlayer().setAttribute("hasWonDuel", false);
		otherPlayer.setAttribute("hasWonDuel", false);
		getPlayer().setAttribute("droppedAmmo", null);
		otherPlayer.setAttribute("droppedAmmo", null);
		addEntity(otherPlayer);
	}

	@Override
	public boolean initializeActivity() {
		if (getPlayer() == null || otherPlayer == null) {
			this.stop();
			return false;
		}
		duelConfigurations = new DuelConfigurations();
		getPlayer().setActivity(this);
		otherPlayer.setActivity(this);
		getPlayer().sendMessage("<col=ff0000>Stake at your OWN risk. No refunds are EVER given.");
		otherPlayer.sendMessage("<col=ff0000>Stake at your OWN risk. No refunds are EVER given.");
		ActionSender.sendConfig(getPlayer(), 286, 0);
		ActionSender.sendConfig(otherPlayer, 286, 0);
		ActionSender.sendString(getPlayer(), "", 631, 28);
		ActionSender.sendString(otherPlayer, "", 631, 28);
		ActionSender.sendString(getPlayer(), "" + otherPlayer.getSkills().getCombatLevel(), 631, 25);
		ActionSender.sendString(otherPlayer, "" + getPlayer().getSkills().getCombatLevel(), 631, 25);
		ActionSender.sendString(getPlayer(), "" + otherPlayer.getDisplayName(), 631, 23);
		ActionSender.sendString(otherPlayer, "" + getPlayer().getDisplayName(), 631, 23);
		ActionSender.sendInventoryInterface(getPlayer(), 628);
		ActionSender.sendInventoryInterface(otherPlayer, 628);
		ActionSender.sendInterface(getPlayer(), DUEL_RULES_INTERFACE);
		ActionSender.sendInterface(otherPlayer, DUEL_RULES_INTERFACE);
		ActionSender.sendDuelOptions(getPlayer());
		ActionSender.sendDuelOptions(otherPlayer);
		getPlayer().setAttribute("duelStakes", new Stakes(getPlayer()));
		otherPlayer.setAttribute("duelStakes", new Stakes(otherPlayer));
		((Stakes) getPlayer().getAttribute("duelStakes")).refresh();
		((Stakes) otherPlayer.getAttribute("duelStakes")).refresh();
		setCurrentState(State.FIRST_SCREEN);
		setActivityState(SessionStates.PAUSE_STATE);
		return false;
	}

	@Override
	public boolean commenceSession() {
		ActionSender.sendCloseInterface(getPlayer());
		ActionSender.sendCloseInterface(otherPlayer);
		ActionSender.closeInventoryInterface(getPlayer());
		ActionSender.closeInventoryInterface(otherPlayer);
		ActionSender.sendCloseChatBox(getPlayer());
		ActionSender.sendCloseChatBox(otherPlayer);
		getPlayer().setAttribute("staffOfLightEffect", -1);
		otherPlayer.setAttribute("staffOfLightEffect", -1);
		getPlayer().stopAll();
		otherPlayer.stopAll();
		getPlayer().getCombatExecutor().reset();
		otherPlayer.getCombatExecutor().reset();
		transferStake(getPlayer());
		transferStake(otherPlayer);
		getPlayer().setAttribute("duelingWith", otherPlayer);
		otherPlayer.setAttribute("duelingWith", getPlayer());
		getPlayer().fullRestore();
		otherPlayer.fullRestore();
		TeleportLocations loc = TeleportLocations.NORMAL_ARENA;
		if (duelConfigurations.getRule(Rules.SUMMONING)) {
			loc = TeleportLocations.SUMMONING_ARENA;
		} else if (duelConfigurations.getRule(Rules.OBSTACLES)) {
			loc = TeleportLocations.OBSTACLES_ARENA;
		}
		ActionSender.sendPlayerOption(getPlayer(), "Fight", 1, true);
		ActionSender.sendPlayerOption(otherPlayer, "Fight", 1, true);
		DuelConfigurations.teleport(getPlayer(), loc, false);
		DuelConfigurations.teleport(otherPlayer, loc, duelConfigurations.getRule(Rules.MOVEMENT));
		World.getWorld().submit(new Tick(2) {
			int count = 3;

			@Override
			public void execute() {
				if (count == 3) {
					getPlayer().teleport(getPlayer().getLocation().getX(), getPlayer().getLocation().getY(), getPlayer().getLocation().getZ(), false);
					otherPlayer.teleport(otherPlayer.getLocation().getX(), otherPlayer.getLocation().getY(), otherPlayer.getLocation().getZ(), false);
				}
				if (count == 0) {
					getPlayer().getMask().setForceText(new ForceText("FIGHT!"));
					otherPlayer.getMask().setForceText(new ForceText("FIGHT!"));
					this.stop();
					commenced = true;
					return;
				}
				getPlayer().getMask().setForceText(new ForceText(count + ""));
				otherPlayer.getMask().setForceText(new ForceText((count--) + ""));
			}
		});
		IconManager.iconOnMob(getPlayer(), otherPlayer, 1, 65535);
		IconManager.iconOnMob(otherPlayer, getPlayer(), 1, 65535);
		return true;
	}

	@Override
	public boolean endSession() {
		if (finished || getPlayer().getActivity() != this || otherPlayer.getActivity() != this) {
			return true;
		}
		finished = true;
		if (getPlayer().getAttribute("duellingForfeit") == Boolean.TRUE) {
			otherPlayer.setAttribute("hasWonDuel", true);
			getPlayer().setAttribute("hasWonDuel", false);
		} else if (otherPlayer.getAttribute("duellingForfeit") == Boolean.TRUE) {
			otherPlayer.setAttribute("hasWonDuel", false);
			getPlayer().setAttribute("hasWonDuel", true);
		}
		ActionSender.sendPlayerOption(getPlayer(), "Challenge", 1, false);
		ActionSender.sendPlayerOption(otherPlayer, "Challenge", 1, false);
		getPlayer().getCombatExecutor().reset();
		otherPlayer.getCombatExecutor().reset();
		getPlayer().setAttribute("duelingWith", null);
		otherPlayer.setAttribute("duelingWith", null);
		IconManager.removeIcon(getPlayer(), otherPlayer);
		IconManager.removeIcon(otherPlayer, getPlayer());
		final Container spoils = (getPlayer().getAttribute("hasWonDuel") == Boolean.TRUE ?
				((Stakes) otherPlayer.getAttribute("duelStakes")).getContainer() :
					((Stakes) getPlayer().getAttribute("duelStakes")).getContainer());
		reset(getPlayer(), otherPlayer);
		setActivityState(SessionStates.PAUSE_STATE);
		World.getWorld().submit(new Tick(1) {
			@Override
			public void execute() {
				checkAmmunition(getPlayer());
				checkAmmunition(otherPlayer);
				final Player spoilsPlayer;
				if (getPlayer().getAttribute("hasWonDuel") == Boolean.TRUE) {
					otherPlayer.setAttribute("hasWonDuel", false);
					getPlayer().setAttribute("hasWonDuel", false);
					getPlayer().fullRestore();
					otherPlayer.fullRestore();
					spoilsPlayer = getPlayer();
				} else if (otherPlayer.getAttribute("hasWonDuel") == Boolean.TRUE) {
					getPlayer().setAttribute("hasWonDuel", false);
					otherPlayer.setAttribute("hasWonDuel", false);
					getPlayer().fullRestore();
					otherPlayer.fullRestore();
					spoilsPlayer = otherPlayer;
				} else {
					spoilsPlayer = null;
				}
				getPlayer().setActivity(Mob.DEFAULT_ACTIVITY);
				otherPlayer.setActivity(Mob.DEFAULT_ACTIVITY);
				stop();
				DuelConfigurations.teleport(getPlayer(), TeleportLocations.CHALLENGE_ROOM, false);
				DuelConfigurations.teleport(otherPlayer, TeleportLocations.CHALLENGE_ROOM, false);
				World.getWorld().submit(new Tick(1) {
					public void execute() {
						stop();
						getPlayer().teleport(getPlayer().getLocation().getX(), getPlayer().getLocation().getY(), getPlayer().getLocation().getZ(), false);
						otherPlayer.teleport(otherPlayer.getLocation().getX(), otherPlayer.getLocation().getY(), otherPlayer.getLocation().getZ(), false);
						addSpoils(spoilsPlayer, spoils);
					}
				});
				DuelActivity.this.stop(false); //Incase the endSession() method gets called in the SessionLogoutTask.
			}
		});
		return false;
	}

	@Override
	public boolean forceEnd(Player player) {
		if (getActivityState() != SessionStates.UPDATE_STATE) {
			return true;
		}
		if (getCurrentState() == State.FIRST_SCREEN || getCurrentState() == State.SECOND_SCREEN) {
			decline(player, false);
			return true;
		}
		Stakes stake = player.getAttribute("duelStakes");
		if (stake == null) {
			return true;
		}
		player.getInventory().addAllDropable(stake.getContainer());
		player.setAttribute("didRequestDuel", Boolean.FALSE);
	    player.setActivity(Mob.DEFAULT_ACTIVITY);
		player.fullRestore();
		DuelConfigurations.teleport(getPlayer(), TeleportLocations.CHALLENGE_ROOM, false);
		return true;
	}

	/**
	 * Adds any used ammunition in the duel arena.
	 *
	 * @param player The player.
	 */
	private void checkAmmunition(Player player) {
		Container ammoRecord = player.getAttribute("droppedAmmo");
		if (ammoRecord == null) {
			return;
		}
		player.getInventory().addAllDropable(ammoRecord);
	}

	@Override
	public boolean walkingUpdate(Player player) {
		boolean isInChallengeRoom = TeleportLocations.CHALLENGE_ROOM.getArea().contains(player.getLocation());
		if (!isInChallengeRoom && duelConfigurations.getRule(Rules.MOVEMENT)) {
			player.sendMessage("Movement has been disabled during this duel.");
			return false;
		} else if (isInChallengeRoom) {
			decline(player, false);
			getPlayer().setAttribute("didRequestDuel", Boolean.FALSE);
		    otherPlayer.setAttribute("didRequestDuel", Boolean.FALSE);
		}
		return true;
	}

	@Override
	public boolean canLogout(Player player, boolean logoutButton) {
		if (!finished) {
			if (logoutButton) {
				player.sendMessage("You can't logout in a duel.");
				return false;
			}
			player.setAttribute("duellingForfeit", true);
			setActivityState(SessionStates.END_STATE);
		}
		return false;
	}

	@Override
	public boolean onTeleport(Player player) {
		player.sendMessage("You can't teleport out of a duel!");
		return false;
	}

	@Override
	public boolean onDeath(Player player) {
		Player killer = getOpponent(player);
		player.setAttribute("hasWonDuel", false);
		killer.setAttribute("hasWonDuel", true);
		player.setAttribute("duellingForfeit", false);
		killer.setAttribute("duellingForfeit", false);
		player.setAttribute("didRequestDuel", Boolean.FALSE);
	    killer.setAttribute("didRequestDuel", Boolean.FALSE);
		endSession();
		stop(false);
		return true;
	}

	@Override
	public boolean isCombatActivity(Mob mob, Mob victim, boolean sendMessages) {
		if (mob.isNPC() || victim.isNPC()) {
			return false;
		}
		if (mob.getPlayer() != getPlayer() && mob.getPlayer() != otherPlayer) {
			return false;
		}
		if (!commenced) {
			if (sendMessages)
				mob.getPlayer().sendMessage("The duel hasn't started yet!");
			return false;
		}
		return victim.getPlayer() == getPlayer() || victim.getPlayer() == otherPlayer;
	}

	/**
	 * Declines the duel.
	 *
	 * @param player The player declining.
	 * @return {@code True}.
	 */
	public boolean decline(Player player, boolean fullInv) {
		Player other = getOpponent(player);
		ActionSender.sendCloseInterface(player);
		ActionSender.sendCloseInterface(other);
		ActionSender.closeInventoryInterface(player);
		ActionSender.closeInventoryInterface(other);
		player.stopAll();
		other.stopAll();
		Container playerStake = ((Stakes) player.getAttribute("duelStakes")).getContainer();
		player.getInventory().addAllDropable(playerStake);
		playerStake.clear();
		Container otherStake = ((Stakes) other.getAttribute("duelStakes")).getContainer();
		other.getInventory().addAllDropable(otherStake);
		otherStake.clear();
		player.getInventory().refresh();
		other.getInventory().refresh();
		reset(player, other);
		player.setActivity(Mob.DEFAULT_ACTIVITY);
		other.setActivity(Mob.DEFAULT_ACTIVITY);
		if (fullInv) {
			player.sendMessage("You do not have enough space in your inventory for the stake!");
			other.sendMessage("You do not have enough space in your inventory for the stake!");
		} else {
			other.sendMessage("The other player declined the stake and duel options.");
			player.sendMessage("You have declined the stake and duel options.");
		}
		stop(false);
		return true;
	}

	/**
	 * Resets all the attributes.
	 *
	 * @param player The first player.
	 * @param other  The second player.
	 * @return {@code True}.
	 */
	private boolean reset(Player player, Player other) {
		player.setAttribute("duelStake", null);
		other.setAttribute("duelStake", null);
		player.setAttribute("duelingWith", null);
		other.setAttribute("duelingWith", null);
		player.setAttribute("acceptedDuel", false);
		other.setAttribute("acceptedDuel", false);
		player.setAttribute("duellingForfeit", false);
		other.setAttribute("duellingForfeit", false);
		return true;
	}

	/**
	 * Accepts the duel.
	 *
	 * @param player The player accepting.
	 * @return {@code True}.
	 */
	public boolean accept(Player player) {
		if (duelConfigurations.canAccept(player)) {
			Player other = getOpponent(player);
			player.setAttribute("acceptedDuel", true);
			/*
			 * String: 22 = Opponent: 
			 * 23 = Opponent's Name
			 * 24 = Combat level:
			 * 26 = Text Under Accept And Decline Button
			 */
			ActionSender.sendString(other, "Other player has accepted.", 631, 26); //it was 44 first??
			ActionSender.sendString(player, "Accepted duel rules and stakes.", 631, 26);
			if (other.getAttribute("acceptedDuel") == Boolean.TRUE) {
				player.setAttribute("acceptedDuel", false);
				other.setAttribute("acceptedDuel", false);
				setCurrentState(State.SECOND_SCREEN);
				duelConfigurations.sendSecondInterface(player, other);
				duelConfigurations.sendSecondInterface(other, player);
			}
		}
		return true;
	}

	/**
	 * Accepts the second duel interface.
	 *
	 * @param player The plater accepting.
	 */
	public void acceptSecond(Player player) {
		Player other = getOpponent(player);
		player.setAttribute("acceptedDuel", true);
		ActionSender.sendString(other, "Other player has accepted.", 626, 45);
		ActionSender.sendString(player, "Accepted duel rules and stakes.", 626, 45);
		if (other.getAttribute("acceptedDuel") == Boolean.TRUE) {
			setActivityState(SessionStates.COMMENCE_STATE);
			setCurrentState(State.FIGHTING);
			player.fullRestore();
			other.fullRestore();
			duelConfigurations.removeEquipment(player);
			duelConfigurations.removeEquipment(other);
		}
	}

	/**
	 * Adds the stakes to the player's inventory.
	 *
	 * @param player The player.
	 * @return {@code True} if spoils got added, {@code false} if not.
	 */
	private boolean addSpoils(Player player, Container spoils) {
		Player other = getOpponent(player);
		ActionSender.sendString(player, 634, 33, other.getDisplayName());
		ActionSender.sendInterface(player, 634);
		Container stake = ((Stakes) other.getAttribute("duelStakes")).getContainer();
		Container riskedItems = ((Stakes) player.getAttribute("duelStakes")).getContainer();
		ActionSender.sendAMask(player, 1026, 634, 28, 0, 35);
		ActionSender.sendClientScript(player, 149, new Object[]{"", "", "", "", "", -1, 0, 6, 6, 136, 634 << 16 | 28}, "noooobsssss");
		ActionSender.sendItems(player, 136, stake, false);
		ActionSender.sendString(player, 634, 33, other.getDisplayName());
		ActionSender.sendString(player, 634, 32, Integer.toString(other.getSkills().getCombatLevel()));
		player.getInventory().addAllDropable(riskedItems);
		player.getInventory().refresh();
		if (player.interfaceItems == null)
			player.interfaceItems = stake;
		else {
			player.getInventory().addAllDropable(player.interfaceItems);
			player.interfaceItems = stake;
		}
		World.getWorld().getPlayerLoader().save(player);
		World.getWorld().getPlayerLoader().save(other);
		Logger.writeStakeLog(player, other, stake, riskedItems);
		return true;
	}

	/**
	 * Removes the stake from the player's inventory, and adds it to the stakes {@link Container}.
	 *
	 * @param player The player.
	 * @return Always {@code true}.
	 */
	private boolean transferStake(Player player) {
		Stakes stake = (Stakes) player.getAttribute("duelStakes");
		if (stake == null) {
			return false;
		}
		stakes.addAll(stake.getContainer());
		return true;
	}

	/**
	 * Gets the opponent.
	 *
	 * @param thisPlayer The player who's looking for the opponent.
	 * @return The opponent player.
	 */
	public Player getOpponent(Player thisPlayer) {
		return getPlayer().equals(thisPlayer) ? otherPlayer : getPlayer();
	}

	/**
	 * Gets one of both players.
	 *
	 * @return A player.
	 */
	public Player getOtherPlayer() {
		return otherPlayer;
	}

	/**
	 * @param duelConfigurations the duelConfigurations to set
	 */
	public void setDuelConfigurations(DuelConfigurations duelConfigurations) {
		this.duelConfigurations = duelConfigurations;
	}

	/**
	 * @return the duelConfigurations
	 */
	public DuelConfigurations getDuelConfigurations() {
		return duelConfigurations;
	}

	/**
	 * Sets the stakes.
	 *
	 * @param stake The stake {@code Item Container}.
	 */
	public void setStakes(Container stake) {
		this.stakes = stake;
	}

	/**
	 * Gets the {@code Item} {@link Container} of stakes.
	 *
	 * @return The stakes
	 */
	public Container getStakes() {
		return stakes;
	}

	/**
	 * @return the commenced
	 */
	public boolean isCommenced() {
		return commenced;
	}

	public State getCurrentState() {
		return currentState;
	}

	public void setCurrentState(State currentState) {
		this.currentState = currentState;
	}
}
