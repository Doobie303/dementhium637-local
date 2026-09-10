package org.dementhium.tickable.impl;

import java.util.ArrayList;
import java.util.List;

import org.dementhium.content.activity.ActivityManager;
import org.dementhium.content.activity.impl.BarrowsActivity;
import org.dementhium.content.activity.impl.CastleWarsActivity;
import org.dementhium.content.activity.impl.DungeoneeringActivity;
import org.dementhium.content.activity.impl.barrows.BarrowsConstants;
import org.dementhium.content.misc.Drinking;
import org.dementhium.content.misc.Drinking.Drink;
import org.dementhium.model.World;
import org.dementhium.model.misc.IconManager;
import org.dementhium.model.player.Player;
import org.dementhium.model.player.Skills;
import org.dementhium.net.ActionSender;
import org.dementhium.tickable.Tick;
import org.dementhium.util.Constants;
import org.dementhium.util.Misc;

/**
 * @author 'Mystic Flow
 */
public class PlayerAreaTick extends Tick {

	private Player player;

	private boolean inWilderness, inBarrows, inDuelArena, inMulti, inPVPZone, inSafePk, inSafeZone;
	int pvpZoneRestoreSpecTick = 100;
	int pvpZoneEpRestoreTick = org.dementhium.content.misc.PvpSystem.EP_INTERVAL;
	int lastWildernessLevel = 0;
	int lastCombatLevel = 0;
	//test it now
	private boolean updateBarrows;

	private int barrowsDrainTime = 30;
    /** Clear the selected model so reopening interface 24 cannot replay an old haunt. */
    private void updateBarrowsFace(boolean underground) {
        if (!underground) {
            barrowsDrainTime = 30;
            if (barrowsFaceTicks > 0) ActionSender.sendBConfig(player, 1043, -1);
            barrowsFaceTicks = 0;
        } else if (barrowsFaceTicks > 0 && --barrowsFaceTicks == 0) {
            ActionSender.sendBConfig(player, 1043, -1);
        }
    }
    private int barrowsCollapseTime = 8;
    private int barrowsFaceTicks;
    private static final int BARROWS_FACE_DURATION = 5; // Brief 3-second appearance, not a looping overlay.

	private int currentBlackout, lastBlackout;

	private boolean multi;

	public PlayerAreaTick(Player player) {
		super(1);
		this.player = player;
	}

	@Override
	public void execute() {
		if (player.isOnline()) {
            updateBarrowsFace(BarrowsConstants.isInBarrowsZone(player)
                    && !BarrowsConstants.BARROWS_AREA.isInArea(player.getLocation())
                    && !player.getAttribute("looted_barrows_request_shake", false) && !player.isDead());
            org.dementhium.content.misc.PvpSystem.tick(player);
			if (BarrowsConstants.isInBarrowsZone(player)) {
				if (!(player.getActivity() instanceof BarrowsActivity))
					ActivityManager.getSingleton().register(new BarrowsActivity(player));
			}
			if (player.getAttribute("looted_barrows_request_shake", Boolean.FALSE) == Boolean.TRUE && !World.getWorld().getAreaManager().getAreaByName("BarrowsUnderground").contains(player.getLocation())) {
				// Keep the looted run marker until the next dig resets it.
				ActionSender.resetCamera(player);
			}
			if (World.getWorld().getAreaManager().getAreaByName("Duel").contains(player.getLocation()) && !World.getWorld().getAreaManager().getAreaByName("AreaNotBeloningToDuel").contains(player.getLocation())) {
				if (!inDuelArena) {
					player.removeAttribute("inGod");
					player.removeAttribute("inWGuild");
					inDuelArena = true;
					inPVPZone = false;
					enablePVPZoneInterface(false);
					updateWildernessState(false);
					inBarrows = false;
					ActionSender.sendPlayerOption(player, "Challenge", 1, false);
					ActionSender.sendCloseOverlay(player);
					ActionSender.sendOverlay(player, 638);
					ActionSender.sendInterfaceConfig(player, 638, 1, true);
				} else {
					if (player.getAttribute("duelingWith") != null) {
						ActionSender.sendPlayerOption(player, "Fight", 1, true);
					}
				}
			} else if (World.getWorld().getAreaManager().getAreaByName("Godwars").contains(player.getLocation())) {
				if (player.getAttribute("inGod", Boolean.FALSE) == Boolean.FALSE) {
					ActionSender.sendCloseOverlay(player);
					ActionSender.sendOverlay(player, 601);
					inDuelArena = false;
					inWilderness = false;
					inPVPZone = false;
					enablePVPZoneInterface(false);
					inBarrows = false;
					player.removeAttribute("inWGuild");
					player.setAttribute("inGod", Boolean.TRUE);
					ActionSender.sendPlayerOption(player, "null", 1, true);
				}
				ActionSender.sendString(player, "" + player.getSettings().getKillCount()[Constants.ARMADYL_KILL_COUNT], 601, 8);
				ActionSender.sendString(player, "" + player.getSettings().getKillCount()[Constants.BANDOS_KILL_COUNT], 601, 9);
				ActionSender.sendString(player, "" + player.getSettings().getKillCount()[Constants.SARA_KILL_COUNT], 601, 10);
				ActionSender.sendString(player, "" + player.getSettings().getKillCount()[Constants.ZAMMY_KILL_COUNT], 601, 11);
			} else if (World.getWorld().getAreaManager().getAreaByName("WGuild").contains(player.getLocation()) || World.getWorld().getAreaManager().getAreaByName("WGuildCatapult").contains(player.getLocation())) {
				if (player.getAttribute("inWGuild", Boolean.FALSE) == Boolean.FALSE) {
					ActionSender.sendCloseOverlay(player);
					ActionSender.sendOverlay(player, 1057);
					inDuelArena = false;
					inWilderness = false;
					inPVPZone = false;
					enablePVPZoneInterface(false);
					inBarrows = false;
					player.removeAttribute("inGod");
					player.setAttribute("inWGuild", Boolean.TRUE);
					ActionSender.sendPlayerOption(player, "null", 1, true);
				}
				ActionSender.sendString(player, "" + player.getSettings().getTokens()[0], 1057, 13);
				ActionSender.sendString(player, "" + player.getSettings().getTokens()[1], 1057, 16);
				ActionSender.sendString(player, "" + player.getSettings().getTokens()[2], 1057, 19);
				ActionSender.sendString(player, "" + player.getSettings().getTokens()[3], 1057, 22);
				ActionSender.sendString(player, "" + player.getSettings().getTokens()[4], 1057, 25);
				/* } else if (World.getWorld().getAreaManager().getAreaByName("BarrowsUnderground").contains(player.getLocation())) {
                if (player.getAttribute("looted_barrows_request_shake", Boolean.FALSE) == Boolean.TRUE) {
                    ActionSender.shakeCamera(player, 10);
                    return;
                }
                if (player.getLocation().getZ() == 0 && player.getAttribute(Barrows.TUNNEL_CRYPT) == null && player.getAttribute("canLootBarrowsChest") != Boolean.TRUE) {
                    player.teleport(3565, 3307, 0, false);
                    inBarrows = false;
                }
                if (!inBarrows || updateBarrows) {
                    inBarrows = true;
                    ActionSender.sendCloseOverlay(player);
                    ActionSender.sendOverlay(player, 24);
                    ActionSender.sendBConfig(player, 1043, -1);
                    ActionSender.sendConfig(player, 453, slayedBrothers());
                    //ActionSender.sendString(player, Integer.toString(player.getSettings().getKillCount()[Constants.BARROW_KILL_COUNT]), 24, 6);
                    currentBlackout = ActionSender.BLACKOUT_MAP;
                    updateBarrows = false;
                } else {
                    if (barrowsDrainTime > 0) {
                        barrowsDrainTime--;
                    } else {
                        List<Integer> killedBrothers = null;
                        for (int i = 0; i < player.getSettings().getKilledBrothers().length; i++) {
                            if (!player.getSettings().getKilledBrothers()[i]) {
                                continue;
                            }
                            if (killedBrothers == null) {
                                killedBrothers = new ArrayList<Integer>();
                            }
                            killedBrothers.add(i);
                        }
                        if (killedBrothers != null) {
                            int head = 4761 + (killedBrothers.get(player.getRandom().nextInt(killedBrothers.size())) * 2);
                            if (player.getLocation().getZ() == 0) {
                                head++;
                            }
                            ActionSender.sendCloseOverlay(player);
                            ActionSender.sendOverlay(player, 24);
                            ActionSender.sendBConfig(player, 1043, head);
                        }
                        barrowsDrainTime = 13 + Misc.random(5);
                    }
                }*/
			} else if (BarrowsConstants.isInBarrowsZone(player) && !World.getWorld().getAreaManager().getAreaByName("BarrowsSurface").contains(player.getLocation())) {
				if (player.getAttribute("looted_barrows_request_shake", Boolean.FALSE) == Boolean.TRUE) {
					if (BarrowsConstants.TUNNELS.isInArea(player.getLocation())) {
                        if (--barrowsCollapseTime <= 0) {
                            barrowsCollapseTime = 8;
                            ActionSender.shakeCamera(player, 3);
                            if (!player.isDead() && !Boolean.TRUE.equals(player.getAttribute("godmode")))
                                player.getDamageManager().damage(null, 5 + player.getRandom().nextInt(16), 20,
                                        org.dementhium.model.misc.DamageManager.DamageType.RED_DAMAGE);
                        }
                    }
					ActionSender.sendCloseOverlay(player);
					return;
				}
				if (!inBarrows) {
					ActionSender.sendCloseOverlay(player);
					ActionSender.sendConfig(player, 1270, BarrowsConstants.isInMiniTunnel(player) ? 1 : 0);
					ActionSender.sendOverlay(player, BarrowsActivity.MAIN_INTERFACE);
					currentBlackout = ActionSender.BLACKOUT_MAP;
					inDuelArena = false;
					inWilderness = false;
					inPVPZone = false;
					enablePVPZoneInterface(false);
					inBarrows = true;
					player.removeAttribute("inGod");
					player.removeAttribute("inWGuild");
					ActionSender.sendPlayerOption(player, "null", 1, true);
				} else {
					// One haunt every 30 game ticks (18 seconds).
					if (barrowsDrainTime > 0) {
						barrowsDrainTime--;
					} else {
						List<Integer> killedBrothers = player.getSettings().getBarrowsKilled();
                        player.getSkills().drainPray(org.dementhium.content.activity.impl.barrows.BarrowsRules.prayerDrain(
                                org.dementhium.content.activity.impl.barrows.BarrowsRules.count(killedBrothers)));
						if (killedBrothers != null && killedBrothers.size() > 0) {
							int head = 4761 + ((killedBrothers.get(player.getRandom().nextInt(killedBrothers.size())) - 2025) * 2);
							if (player.getLocation().getZ() == 0) {
								head++;
							}
							ActionSender.sendCloseOverlay(player);
							ActionSender.sendOverlay(player, BarrowsActivity.MAIN_INTERFACE);
							ActionSender.sendBConfig(player, 1043, head);
                            barrowsFaceTicks = BARROWS_FACE_DURATION;
						}
						barrowsDrainTime = 29;
					}
				}
			} else if (player.isInWilderness()) {
                if (!player.isDead() && --pvpZoneEpRestoreTick <= 0) {
                    pvpZoneEpRestoreTick = org.dementhium.content.misc.PvpSystem.EP_INTERVAL;
                    player.pvpZoneEp = Math.min(100,player.pvpZoneEp + org.dementhium.content.misc.PvpSystem.EP_GAIN);
                    player.targetLikelihood = Math.min(60,player.targetLikelihood + 5);
                    refreshPvpStrings();
                }
                org.dementhium.content.misc.PvpSystem.findTarget(player);
				if (lastCombatLevel != player.getSkills()
						.getCombatLevelWithoutSummoning()) {
					enablePVPZoneInterface(true);
					enablePvPStrings(true);
					lastCombatLevel = player.getSkills()
							.getCombatLevelWithoutSummoning();
				}
				if (player.inWilderness()
						&& lastWildernessLevel != player.getLocation()
						.getWildernessLevel()) {
					enablePVPZoneInterface(true);
					enablePvPStrings(true);
					lastWildernessLevel = player.getLocation()
							.getWildernessLevel();
				}
				if (!inWilderness) {
					inDuelArena = false;
					inBarrows = false;
					player.removeAttribute("inGod");
					player.removeAttribute("inWGuild");
					inPVPZone = false;
					enablePVPZoneInterface(true);
					updateWildernessState(true);
					ActionSender.sendPlayerOption(player, "Attack", 1, true);
					sendWildyInterface(player);
					enablePvPStrings(true);
					player.getMask().setAppearanceUpdate(true); //to update summoning combat display
				}
			} else if (player.inSafePk()) {
				if (!inSafePk) {
					inDuelArena = false;
					inBarrows = false;
					inWilderness = false;
					inPVPZone = false;
					inSafeZone = false;
					player.removeAttribute("inGod");
					player.removeAttribute("inWGuild");
					inSafePk = true;
					sendSafePkInterface(player);
					enablePVPZoneInterface(true);
					updateWildernessState(true);
					ActionSender.sendPlayerOption(player, "Attack", 1, true);
					player.getMask().setAppearanceUpdate(true); //to update summoning combat display
				}
			} else if (player.inSafeZone()) {
				if (!inSafeZone) {
					inSafePk = false;
					inDuelArena = false;
					inBarrows = false;
					inWilderness = false;
					inPVPZone = false;
					player.removeAttribute("inGod");
					player.removeAttribute("inWGuild");
					inSafeZone = true;
					enablePVPZoneInterface(false);
					sendSafePkInterface(player);
					updateWildernessState(false);
					ActionSender.sendPlayerOption(player, "null", 1, true);
					player.getMask().setAppearanceUpdate(true); //to update summoning combat display
				} //kk
			} else if (player.inPVPZone()) {
				pvpZoneRestoreSpecTick--;
				if (pvpZoneRestoreSpecTick < 0) {
					pvpZoneRestoreSpecTick = 100;
					if (player.getSpecialAmount() < 1000) {
						player.setSpecialAmount(1000);
						player.graphics(446);
					}
				}
				if (!inPVPZone) {
					inDuelArena = false;
					inBarrows = false;
					inPVPZone = true;
					inWilderness = false;
					inSafeZone = false;
					inSafePk = false;
					player.removeAttribute("inGod");
					player.removeAttribute("inWGuild");
					ActionSender.sendCloseOverlay(player);
					enablePVPZoneInterface(true);
					ActionSender.sendPlayerOption(player, "Attack", 1, true);
					player.getMask().setAppearanceUpdate(true); //to update summoning combat display
				}
			} else if (World.getWorld().getAreaManager().getAreaByName("Puro-Puro").contains(player.getLocation())) {
				currentBlackout = ActionSender.BLACKOUT_MAP;
			}else if (player.getActivity() instanceof DungeoneeringActivity) {
				if (!player.getAttribute("inDung", false)) {
					player.setAttribute("inDung", true);
					inDuelArena = false;
					inBarrows = false;
					inPVPZone = false;
					inWilderness = false;
					player.removeAttribute("inGod");
					player.removeAttribute("inWGuild");
					enablePVPZoneInterface(false);
					player.getMask().setAppearanceUpdate(true); //to update summoning combat display
					DungeoneeringActivity.showDeathCount(player);
				}
			} else if (!(player.getActivity() instanceof CastleWarsActivity)) {
				currentBlackout = ActionSender.NO_BLACKOUT;
				if (inWilderness || inPVPZone || inBarrows || inDuelArena || player.getAttribute("inGod", false) || player.getAttribute("inWGuild", false)) {
					ActionSender.sendCloseOverlay(player);
					player.removeAttribute("inGod");
					player.removeAttribute("inWGuild");
					if (inWilderness)
						player.getMask().setAppearanceUpdate(true); //to remove summoning combat display
				}
				inDuelArena = false;
				inBarrows = false;
				enablePVPZoneInterface(false);
				updateWildernessState(false);
				if (World.getWorld().getAreaManager().getAreaByName("Nex").contains(player.getLocation())) {
					enableInterface(false);
				} else if (!player.isMulti()) { //lol.. that's why it kept closing multi icon (fixed)
					enableInterface(true);
				}
				ActionSender.sendPlayerOption(player, "null", 1, true);
				ActionSender.sendInterfaceConfig(player, 381, 1, true);
				ActionSender.sendInterfaceConfig(player, 381, 2, true);
			}
			if (lastBlackout != currentBlackout) {
				lastBlackout = currentBlackout;
				ActionSender.updateMinimap(player, currentBlackout);
			}
			/*if (!player.isMulti()) {
                enableInterface(false);
                multi = false;
            } else if (player.isMulti()) {
                multi = true;
                enableInterface(true);
            }*/
			if (!player.isMulti() && multi) {
				multi = false;
				enableInterface(true);
			} else if (!multi && player.isMulti()) {
				multi = true;
				enableInterface(false);
			}
			if (player.isInWilderness()) {
				if (player.getAttribute("overloads", Boolean.FALSE) == Boolean.TRUE) {
					for (int i = 0; i < Drinking.Drink.OVERLOAD.getSkills().length; i++) { //HOLY SHIT IM DUMBBB
						int skill = Drinking.Drink.OVERLOAD.getSkill(i);
						if (skill == Skills.RANGED || skill == Skills.MAGIC) {
							player.getSkills().increaseLevelToMaximumModification(skill, 0);
						} else {
							int modification = (int) Math.floor(5 + (player.getSkills().getLevelForExperience(skill) * 0.15));
							player.getSkills().set(skill, player.getSkills().getLevelForExperience(skill) + modification);
						}
					}
					player.removeAttribute("overloads");
					player.sendMessage("Your overload potion has ran out!");
				} else if (player.getAttribute("extremeType") != null) {
					Drink drink = player.getAttribute("extremeType");
					for (int i = 0; i < drink.getSkills().length; i++) { //HOLY SHIT IM DUMBBB
						int skill = drink.getSkill(i);
						if (skill == Skills.RANGED || skill == Skills.MAGIC) {
							player.getSkills().increaseLevelToMaximumModification(skill, 0);
						} else {
							int modification = (int) Math.floor(5 + (player.getSkills().getLevelForExperience(skill) * 0.15));
							player.getSkills().set(skill, player.getSkills().getLevelForExperience(skill) + modification);
						}
					}
					player.removeAttribute("extremeType");
					player.sendMessage("Your extreme potion has been reset.");
				}
			}
		} else {
			this.stop();
		}
	}

	private void sendWildyInterface(Player p) {
		ActionSender.sendCloseOverlay(player);
		ActionSender.sendOverlay(p, 591);
		//ActionSender.sendInterfaceConfig(p, 381, 1, false);
		//ActionSender.sendInterfaceConfig(p, 381, 2, false);

	}

	public int slayedBrothers() {
		int config = 0;
		for (int i = 0; i < player.getSettings().getKilledBrothers().length; i++) {
			if (!player.getSettings().getKilledBrothers()[i]) {
				continue;
			}
			config |= 1 << i;
		}
		int killCount = player.getSettings().getKillCount()[Constants.BARROW_KILL_COUNT];
		return (killCount << 1) << 16 | config;
	}

	private void enablePvPStrings(boolean enable) {
		boolean fixed = player.getConnection().getDisplayMode() < 2;
		int winId = fixed ? 548 : 746;
		int childId = fixed ? 9 : 14;
		if (enable) {
			String epColour = "<col=FFFF00>";
			if (player.pvpZoneEp < 20)
				epColour = "<col=7E2217>";
			else if (player.pvpZoneEp >= 20 && player.pvpZoneEp < 40)
				epColour = "<col=800517>";
			else if (player.pvpZoneEp >= 76 && player.pvpZoneEp <= 100)
				epColour = "<col=00FF00>";
			if (fixed) {
				ActionSender.sendString(player, winId, childId,
						getPVPZoneCombatRange()[0] + " - "
								+ getPVPZoneCombatRange()[1]);
				ActionSender.sendString(player, winId, 10, "EP: " + epColour
						+ player.pvpZoneEp + "%");
			} else {
				ActionSender.sendString(player, winId, childId,
						getPVPZoneCombatRange()[0] + " - "
								+ getPVPZoneCombatRange()[1] + "<br> EP: "
								+ epColour + player.pvpZoneEp + "%");
			}
			ActionSender.sendConfig(player, 1410, player.targetLikelihood);
		} else {
			ActionSender.sendString(player, winId, childId, "");
			if (fixed)
				ActionSender.sendString(player, winId, 10, "");
		}
	}

    public int[] getPVPZoneCombatRange() {
        return org.dementhium.content.misc.PvpSystem.combatRange(player);
    }
    public void refreshPvpStrings() {
        enablePvPStrings(player.isInWilderness() || player.inPVPZone() || player.inSafePk());
    }
	public void updateWildernessState(boolean inWildy) {
		if (inWildy && !inWilderness) {
			inWilderness = true; // so we don't constantly add an attribute
		} else if (!inWildy && inWilderness) {
			inWilderness = false;
		}
	}

	public void enableInterface(boolean multi) {
		/*if (inMulti == multi) {
            return;
        }
        inMulti = multi;*/
		//ActionSender.sendInterfaceConfig(player, 745, 3, false);
		//ActionSender.sendInterfaceConfig(player, 745, 6, false);
		ActionSender.sendInterfaceConfig(player, 745, 0, true);
		ActionSender.sendInterfaceConfig(player, 745, 1, !multi); // multi zone
	}//test


	//http://www.rune-server.org/runescape-development/rs-503-client-server/configuration/158872-release-pvp-multi-icons-etc.html
	public void enablePVPZoneInterface(boolean pvpZone) {
		ActionSender.sendInterfaceConfig(player, 745, 0, true);
		ActionSender.sendInterfaceConfig(player, 745, 6, pvpZone);
		// ActionSender.sendInterfaceConfig(player, 745, 3, inSafeZone); //= safe PVP zone icon
		// if (inSafeZone)
		//ActionSender.sendInterfaceConfig(player, 745, 4, false);
        enablePvPStrings(pvpZone);
	}
	public void sendSafePkInterface(Player player) {
		ActionSender.sendCloseOverlay(player);
		ActionSender.sendOverlay(player, 789);
		// ActionSender.sendInterfaceConfig(player, 789, 3, true);
		// ActionSender.sendInterfaceConfig(player, 789, 6, safepk);
	}

	public boolean inWilderness() {
		return inWilderness;
	}

	public void updateBarrowsInterface() {
		updateBarrows = true;
	}

	public boolean isInArdougne() {
		return World.getWorld().getAreaManager().getAreaByName("Ardougne").contains(player.getLocation());
	}

	/**
	 * Used for displaymode switching (only reset things that have to do with configs that are different for displaymodes).
	 */
	public void resetAllBooleanTrackers() {
		inDuelArena = (inDuelArena ? false : true);
		//if (player.getAttribute("inGod", Boolean.FALSE) == Boolean.FALSE) //?
		//player.setAttribute("inGod", Boolean.TRUE);
		player.removeAttribute("inGod");
		//if (player.getAttribute("inWGuild", Boolean.FALSE) == Boolean.FALSE) //?
		//  player.setAttribute("inWGuild", Boolean.TRUE);
		player.removeAttribute("inWGuild");
		inWilderness = (inWilderness ? false : true);
		inPVPZone = (inPVPZone ? false : true);
		inBarrows = (inBarrows ? false : true);
		player.removeAttribute("inDung");
		multi = (multi ? false : true);
	}
}
