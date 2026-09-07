package org.dementhium.model.player;

import org.dementhium.content.activity.impl.DuelActivity;
import org.dementhium.content.misc.GraveStoneManager;
import org.dementhium.content.skills.Prayer;
import org.dementhium.model.Item;
import org.dementhium.model.Location;
import org.dementhium.model.Mob;
import org.dementhium.model.World;
import org.dementhium.model.mask.Animation;
import org.dementhium.model.misc.GroundItem;
import org.dementhium.model.misc.GroundItemManager;
import org.dementhium.net.ActionSender;
import org.dementhium.tickable.Tick;
import org.dementhium.util.Misc;

public class Skills {

	public static final int SKILL_COUNT = 25;
	public static final double MAXIMUM_EXP = 200000000;

	private final Player player;
	private final int level[] = new int[SKILL_COUNT];
	private final double xp[] = new double[SKILL_COUNT];
	private int hitPoints;
	//private int hitPointsRaised;
	private int maxLifePoints;
	private int experienceCounter;


	private boolean dead = false;

	//1801
	private double prayerPoints; //for flashing or w.e it's called

	public static final String[] SKILL_NAME = {"Attack", "Defence",
		"Strength", "Constitution", "Ranged", "Prayer", "Magic", "Cooking",
		"Woodcutting", "Fletching", "Fishing", "Firemaking", "Crafting",
		"Smithing", "Mining", "Herblore", "Agility", "Thieving", "Slayer",
		"Farming", "Runecrafting", "Hunter", "Construction", "Summoning",
	"Dungeoneering"};

	// 0 - 1, 1 - 4, 2 - 2,

	public static final int ATTACK = 0, DEFENCE = 1, STRENGTH = 2,
	CONSTITUTION = 3, RANGED = 4, PRAYER = 5, MAGIC = 6, COOKING = 7,
	WOODCUTTING = 8, FLETCHING = 9, FISHING = 10, FIREMAKING = 11,
	CRAFTING = 12, SMITHING = 13, MINING = 14, HERBLORE = 15,
	AGILITY = 16, THIEVING = 17, SLAYER = 18, FARMING = 19,
	RUNECRAFTING = 20, CONSTRUCTION = 22, HUNTER = 21, SUMMONING = 23,
	DUNGEONEERING = 24;

	public static final int[] COMBAT_SKILLS =
	{
		ATTACK, DEFENCE, STRENGTH, CONSTITUTION, RANGED, PRAYER, MAGIC, SUMMONING
	};


	public Skills(Player player) {
		this.player = player;
		for (int i = 0; i < SKILL_COUNT; i++) {
			level[i] = 1;
			xp[i] = 0;
		}
		level[3] = 10;
		xp[3] = 1184;
		hitPoints = 100;
		prayerPoints = 1;
	}

	public void hit(int hitDiff) {
		if (Boolean.TRUE.equals(player.getAttribute("godmode"))) {
			return;
		}
		if (hitDiff > hitPoints)
			hitDiff = hitPoints;
		hitPoints -= hitDiff;
		if (hitPoints < 1)
			sendDead();
		if (hitPoints > getMaximumLifePoints()) {
			hitPoints = getMaximumLifePoints();
		}
		ActionSender.sendConfig(player, 1240, hitPoints * 2);
	}

	public boolean isDead() {
		return hitPoints <= 0;
	}

	/*public int getMaxHitpoints() {
		return (getLevelForExperience(Skills.HITPOINTS) * 10) + hitPointsRaised;
	}*/
	
	public int getMaximumLifePoints() {
		return (getLevelForExperience(Skills.CONSTITUTION) * 10) + maxLifePoints;
	}
	
	/**
	 * Used for saving the lifePoints
	 * @return Returns the LifePoints unedited
	 */
	public int getMaxLifePoints() {
		return maxLifePoints;
	}
	
	/**
	 * Used for saving the lifePoints
	 * @return Returns the LifePoints unedited
	 */
	public void setMaxLifePoints(int maxLp) {
		maxLifePoints = maxLp;
	}
	
	public void setMaximumLifePoints(int lp) {
		maxLifePoints = lp - (getLevelForExperience(Skills.CONSTITUTION) * 10);
		//heal(0);
	}
	
	public void raiseMaximumLifePoints(int lpRaise) {
		maxLifePoints += lpRaise;
		//heal(0);
	}
	
	public void decreaseMaximumLifePoints(int lpDecrease) {
		maxLifePoints -= lpDecrease;
		//if (hitPoints <= 0) {
			//hitPoints = 1;
		//}
		//heal(0);
	}
	
	public void decreaseLifePointsTick(int hitDiff) {
		if (Boolean.TRUE.equals(player.getAttribute("godmode"))) {
			return;
		}
		if (isDead()) {
			sendDead();
			return;
		}
		hitPoints -= hitDiff;
		int minimum = getLevelForExperience(Skills.CONSTITUTION) * 10;
		if (hitPoints < minimum) {
			hitPoints = minimum;
		}
		ActionSender.sendConfig(player, 1240, hitPoints * 2);
	}

	//@SuppressWarnings("unused")
	public void sendDead() {
		/*if (true) {
              hitPoints = 990;
              return;
          }*/
          if (dead) {
        	  return;
          }
          dead = true;
          Mob last = player.getCombatExecutor().getLastAttacker();
          if (last != null) {
          		last.setAttribute("combatTicks", 0);
          }
          World.getWorld().submit(new Tick(1) {
        	  public void execute() {
        		  stop();
        		  if (!player.isOnline() || player.destroyed()) {
        			  return;
        		  }
        		  player.animate(9055);
        		  Prayer.wrathEffect(player, player.getCombatExecutor().getLastAttacker());
        		  Prayer.retributionEffect(player, player.getCombatExecutor().getLastAttacker());
        		  player.removeTick("nex_virus");
        		  if (player.getActivity() instanceof DuelActivity) {
        			  DuelActivity duel = (DuelActivity) player.getActivity();
        			  duel.getOpponent(player).setAttribute("hitImmunity", World.getTicks() + 5);
        		  }
        		  player.submitTick("death_tick", new Tick(4) {
        			  @Override
        			  public void execute() {
        				  stop();
        				  dead = false;
        				  player.setAttribute("hitImmunity", World.getTicks() + 5);
        				  for (int i = 0; i < SKILL_COUNT; i++) {
        					  set(i, getLevelForExperience(i));
        				  }
        				  hitPoints = getMaximumLifePoints();
        				  player.animate(Animation.RESET);
        				  player.sendMessage("Oh dear, you are dead!");
        				  ActionSender.sendConfig(player, 1240, hitPoints * 2);
        				  player.setSpecialAmount(1000);
        				  player.resetCombat();
        				  player.getPoisonManager().removePoison();
        				  player.getEquipment().recalculateHpModifier(); //CHECK THIS LATER
        				  player.getEquipment().refresh();
        				  player.setAttribute("teleblock", 0);
        				  player.setAttribute("teleblockImmunity", 0);
        				  player.setAttribute("freezeTime", 0);
        				  Mob killer = player.getDamageManager().getKiller();
        				  if (World.getWorld().getAreaManager().getAreaByName("RandomPVPZone").contains(player.getLocation())) {
        					  if (killer != null && killer != player && killer.isPlayer()) {
        						 // Item rawPVPDrop = new Item(PVPItems(), 1);
        						 // GroundItem pvpDrop = new GroundItem(killer.getPlayer(), rawPVPDrop, player.getLocation(), false, killer.getPlayer().getRights() >= 2, GroundItemManager.groundItemIndex++);
        						//for (int i = 0; i < Misc.random(1); i++)	
        							//  GroundItemManager.createGroundItem(pvpDrop);

        					  }
        					  player.teleport(getRandomPVPZoneRespawnLocation(), false);
        				  } else if (World.getWorld().getAreaManager().getAreaByName("SafePk").contains(player.getLocation())) { //In a safe activity we don't drop items or teleport to the DEFAULT_LOCATION, this will be done in the onDeath method.
        					  if (killer != null && killer != player && killer.isPlayer()) {
        						 // Item rawPVPDrop = new Item(PVPItems(), 1);
        						 // GroundItem pvpDrop = new GroundItem(killer.getPlayer(), rawPVPDrop, player.getLocation(), false, killer.getPlayer().getRights() >= 2, GroundItemManager.groundItemIndex++);
        						 //for (int i = 0; i < Misc.random(1); i++)	
        							 // GroundItemManager.createGroundItem(pvpDrop);
        					  }       
        					  player.teleport(2815, 5511, 0, false); //safepk spawns
        				  } else if (World.getWorld().getAreaManager().getAreaByName("FFA").contains(player.getLocation())) { //In a safe activity we don't drop items or teleport to the DEFAULT_LOCATION, this will be done in the onDeath method.
        					  if (killer != null && killer != player && killer.isPlayer()) {
        						 // Item rawPVPDrop = new Item(PVPItems(), 1);
        						 // GroundItem pvpDrop = new GroundItem(killer.getPlayer(), rawPVPDrop, player.getLocation(), false, killer.getPlayer().getRights() >= 2, GroundItemManager.groundItemIndex++);
        						 //for (int i = 0; i < Misc.random(1); i++)	
        							 // GroundItemManager.createGroundItem(pvpDrop);
        					  }       
        					  player.teleport(2815, 5511, 0, false); //safepk spawns
        				  } else if (!player.getActivity().onDeath(player)) { //In a safe activity we don't drop items or teleport to the DEFAULT_LOCATION, this will be done in the onDeath method.
        					  if (/*killer != null && */killer != player) {
        						  GraveStoneManager.appendDeath(player, killer);
        					  }
            				  player.getSkullManager().removeSkull();
            				  if (player.isInWilderness())
            					  player.teleport(3101+Misc.random(2), 3495+Misc.random(1), 0, false); //edgeville
            				  else
            					  player.teleport(Mob.DEFAULT, false);
        				  } else if (killer != null && killer != player && killer.isPlayer()) {
        					  if (killer.getPlayer().getActivity() instanceof DuelActivity)
        						  killer.getPlayer().sendMessage("Well done! You have defeated "+player.getDisplayName()+".");
        					  else
        				    
        				    killer.getPlayer().sendMessage(GraveStoneManager.getRandomDeathMessage(player));
        				  }
        				  player.getPrayer().closeAllPrayers(); //this is needed after the dropItemsOnDeath above for item protection prayer.
        				  player.getDamageManager().clearEnemyHits();
        				  //player.getDamageManager().clearHits(); //?
        			  }

        		  }, true);
        	  }
          });
	}

	public void heal(int hitDiff) {
		if (isDead()) {
			sendDead();
			return;
		}
		if (hitPoints >= getMaximumLifePoints())
			return;
		hitPoints += hitDiff;
		int max = getMaximumLifePoints();
		if (hitPoints > max) {
			hitPoints = max;
		}
		ActionSender.sendConfig(player, 1240, hitPoints * 2);
	}


	public void healRocktail(int healAmount) {
		if (hitPoints >= getMaximumLifePoints() + 100)
			return;
		hitPoints += healAmount;
		int max = getMaximumLifePoints() + 100;
		if (hitPoints > max) {
			hitPoints = max;
		}
		ActionSender.sendConfig(player, 1240, hitPoints * 2);
	}


	public void heal(int hitDiff, int type) {
		if (hitPoints >= getMaximumLifePoints())
			return;
		hitPoints += hitDiff;
		int max = type;
		if (hitPoints > max) {
			hitPoints = max;
		}
		ActionSender.sendConfig(player, 1240, hitPoints * 2);
	}

	/*public void raiseTotalHp(int raise) {
		hitPointsRaised += raise;

		heal(0);
	}

	public void lowerTotalHp(int lower) {
		hitPointsRaised -= lower;

		if (hitPoints <= 0) {
			hitPoints = 1;
		}
		heal(0);
	}*/

	public void restorePray(double restore) {
		setPrayerPoints(prayerPoints + restore, true);
		int max = getLevelForExperience(5);
		if (prayerPoints > max) {
			prayerPoints = max;
		}
		ActionSender.sendSkillLevel(player, 5);
	}

	public void drainPray(double drain) {
		if (Boolean.TRUE.equals(player.getAttribute("godmode"))) {
			return;
		}
		setPrayerPoints(prayerPoints - drain, true);
		if (prayerPoints <= 0) {
			prayerPoints = 0;
		}
		ActionSender.sendSkillLevel(player, 5);
	}

	public void reset() {
		for (int i = 0; i < SKILL_COUNT; i++) {
			level[i] = 1;
			xp[i] = 0;
		}
		level[3] = 10;
		xp[3] = 1184;
		hitPoints = 100;
		prayerPoints = 1;
		refresh();
	}

	public int getCombatLevel() {
		int attack = getLevelForExperience(0);
		int defence = getLevelForExperience(1);
		int strength = getLevelForExperience(2);
		int hp = getLevelForExperience(3);
		int prayer = getLevelForExperience(5);
		int ranged = getLevelForExperience(4);
		int magic = getLevelForExperience(6);
		int combatLevel = 3;
		combatLevel = (int) ((defence + hp + Math.floor(prayer / 2)) * 0.25) + 1;
		double melee = (attack + strength) * 0.325;
		double ranger = Math.floor(ranged * 1.5) * 0.325;
		double mage = Math.floor(magic * 1.5) * 0.325;
		if (melee >= ranger && melee >= mage) {
			combatLevel += melee;
		} else if (ranger >= melee && ranger >= mage) {
			combatLevel += ranger;
		} else if (mage >= melee && mage >= ranger) {
			combatLevel += mage;
		}
		int summoning = getLevelForExperience(Skills.SUMMONING);
		summoning /= 8;
		return combatLevel + summoning;
	}
	
	public int getCombatLevelWithoutSummoning() {
		return getCombatLevel() - (getLevelForExperience(Skills.SUMMONING) / 8);
	}

	public int getLevel(int skill) {
		return level[skill];
	}

	public double getXp(int skill) {
		return xp[skill];
	}

	public int getXPForLevel(int level) {
		int points = 0;
		int output = 0;
		for (int lvl = 1; lvl <= level; lvl++) {
			points += Math.floor(lvl + 300.0
					* Math.pow(2.0, lvl / 7.0));
			if (lvl >= level) {
				return output;
			}
			output = (int) Math.floor(points / 4);
		}
		return 0;
	}

	public int getLevelForExperience(int skill) {
		double exp = xp[skill];
		int points = 0;
		int output = 0;
		for (int lvl = 1; lvl < (skill == 24 ? 121 : 100); lvl++) {
			points += Math.floor(lvl + 300.0
					* Math.pow(2.0, lvl / 7.0));
			output = (int) Math.floor(points / 4);
			if ((output - 1) >= exp) {
				return lvl;
			}
		}
		return skill == 24 ? 120 : 99;
	}

	public void setXp(int skill, double exp) {
		xp[skill] = exp;
		ActionSender.sendSkillLevel(player, skill);
	}

	public static final double XP_MODIFIER = 89.0;
	public double getXpModifierForSkill(int skill) {
		if (skill == ATTACK || skill == DEFENCE || skill == STRENGTH || skill == RANGED || skill == MAGIC || skill == CONSTITUTION)
			return (player.isDoubleXp() ? (XP_MODIFIER * 1.5) : XP_MODIFIER);
		else
			return (player.isDoubleXp() ? (XP_MODIFIER * 3) : (XP_MODIFIER * 2));
	}

	public void addExperience(int skill, double exp) {
		int oldLevel = getLevelForExperience(skill);
		int oldCombat = getCombatLevel();
		double experience = exp * getXpModifierForSkill(skill);
		xp[skill] += experience;
		experienceCounter += experience;
		if (xp[skill] > MAXIMUM_EXP) {
			xp[skill] = MAXIMUM_EXP;
		}
		int newLevel = getLevelForExperience(skill);
		int levelDiff = newLevel - oldLevel;
		if (newLevel > oldLevel) {
			level[skill] += levelDiff;
			player.getSettings().getLeveledUp()[skill] = true;
			player.getSettings().getLeveledUpConfig()[skill] = true;
			player.setAttribute("leveledUp", Boolean.TRUE);
			if (skill == CONSTITUTION) {
				heal(levelDiff * 10);
			}
			if(skill == PRAYER) {
				restorePray(levelDiff);
			}
			if (player.getAttribute("combatLevelMsg") == null)
				player.setAttribute("combatLevelMsg", true);
			boolean canSendCombatLevelMessage = player.getAttribute("combatLevelMsg"); //if this method is called 2 or more times (for str xp and hp xp for example, it will show the message 2 times, this fixes it)
			if (oldCombat != getCombatLevel() && canSendCombatLevelMessage) {
				player.setAttribute("combatLevelMsg", false);
				World.getWorld().submit(new Tick(1) { //didn't work without a tick?
					@Override
					public void execute() {
						player.setAttribute("combatLevelMsg", true);
						player.sendMessage("Congratulations! You've just reached Combat level "+getCombatLevel()+"!");
						player.getMask().setAppearanceUpdate(true);
						stop();
					}
				});
			}
		}
		ActionSender.sendSkillLevel(player, skill);
	}


	public void set(int skill, int val) {
		if (skill == Skills.PRAYER) {
			prayerPoints = val;
		}
		if (val < 0) {
			val = 0;
		}
		level[skill] = val;
		ActionSender.sendSkillLevel(player, skill);
	}

	public void setLevelAndXP(int skill, int level, double xp) {
		if (skill == Skills.PRAYER) {
			prayerPoints = level;
		}
		this.level[skill] = level;
		this.xp[skill] = xp;
	}

	public void sendSkillLevels() {
		for (int i = 0; i < Skills.SKILL_COUNT; i++)
			ActionSender.sendSkillLevel(player, i);
	}

	public void refresh() {
		sendSkillLevels();
		ActionSender.sendConfig(player, 1240, hitPoints * 2);
		this.player.getMask().setAppearanceUpdate(true);
	}

	public int getAmountOf99s() {
		int count = 0;
		for (int i = 0; i < 25; i++) {
			if (player.getSkills().getLevelForExperience(i) >= 99)
				count++;
		}
		return count;
	}
	
	public int getTotalLevel() {
		int totalLevel = 0;
		for (int i = 0; i < 25; i++) {
			totalLevel += player.getSkills().getLevelForExperience(i);
		}
		return totalLevel;
	}
	
	public boolean isLevelBelowOriginal(int skill) {
		return level[skill] < getLevelForExperience(skill);
	}

	public boolean isLevelBelowOriginalModification(int skill, int modification) {
		return level[skill] < (getLevelForExperience(skill) + modification);
	}

	public void increaseLevelToMaximum(int skill, int modification) {
		if (isLevelBelowOriginal(skill)) {
			setLevel(skill, level[skill] + modification >= getLevelForExperience(skill) ? getLevelForExperience(skill) : level[skill] + modification);
		}
	}

	public void increaseLevelToMaximumModification(int skill, int modification) {
		if (isLevelBelowOriginalModification(skill, modification)) {
			setLevel(skill, level[skill] + modification >= (getLevelForExperience(skill) + modification) ? (getLevelForExperience(skill) + modification) : level[skill] + modification);
		}
	}

	public void decreaseLevelToMinimum(int skill, int modification) {
		if (level[skill] > 1) {
			setLevel(skill, level[skill] - modification <= 1 ? 1 : level[skill] - modification);
		}
	}

	public void decreaseLevelToZero(int skill, int modification) {
		if (level[skill] > 0) {
			setLevel(skill, level[skill] - modification <= 0 ? 0 : level[skill] - modification);
		}
	}

	public void decreaseLevelOnce(int skill, int amount) {
		if (level[skill] > (getLevelForExperience(skill) - amount)) { //this stops resetting levels. EG if I have 87/95 str and it decreases 5, normally it would reset it to 90/95, however this line prevents it!
			if (level[skill] - amount <= (getLevelForExperience(skill) - amount)) {
				level[skill] = (getLevelForExperience(skill) - amount);
			} else {
				level[skill] -= amount;
			}
			ActionSender.sendSkillLevel(player, skill);
		}
	}

	public void setLevel(int skill, int level) {
		this.level[skill] = level;
		ActionSender.sendSkillLevel(player, skill);
	}

	public void setHitPoints(int hitPoints) {
		this.hitPoints = hitPoints;
	}

	public int getHitPoints() {
		return hitPoints;
	}

	public double getPrayerPoints() {
		return prayerPoints;
	}

	public void setPrayerPoints(double prayerPoints, boolean update) {
		int lvlBefore = (int) Math.ceil(this.prayerPoints);
		this.prayerPoints = prayerPoints;
		int lvlAfter = (int) Math.ceil(this.prayerPoints);
		if (update && (lvlBefore - lvlAfter >= 1 || lvlAfter - lvlBefore >= 1)) {
			ActionSender.sendSkillLevel(player, Skills.PRAYER);
		}
	}

	public int getExperienceCounter() {
		return experienceCounter;
	}

	public void setExperienceCounter(int experienceCounter) {
		this.experienceCounter = experienceCounter;
	}

	public void completeRestore() {
		for (int i = 0; i < SKILL_COUNT; i++) {
			set(i, getLevelForExperience(i));
		}
		hitPoints = getMaximumLifePoints();
		refresh();
		//		for (int i = 0; i < level.length; i++) {
			//			level[i] = getLevelForExperience(i);
			//			ActionSender.sendSkillLevel(player, i);
		//		}
		//		heal(990);
	}

	/*public void setHitpointsRaised(int amount) {
		this.hitPointsRaised = amount;
	}*/

	/**
	 * Drains a skill and returns the amount that couldn't be drained.
	 * @param skill The skill id.
	 * @param drain The value to decrease.
	 * @return The amount left to decrease. (ex. if attackLevel - drain < 0,
	 * <br>		we return drain - attackLevel)
	 */
	public int drainLevel(int skill, int drain) {
		int drainLeft = drain - level[skill];
		if (drainLeft < 0) {
			drainLeft = 0;
		}
		level[skill] -= drain;
		if (level[skill] < 0) {
			level[skill] = 0;
		}
		ActionSender.sendSkillLevel(player, skill);
		return drainLeft;
	}

	
	
    public static int PVPItems[] = {
    	/*379, 373, 385, 391, 15272, 2434, 6685,
         11235, 11732, 11335, 11283, 11284, 
        8850, 10551, 1079, 1093, 1113, 1127, 
        1147, 1163, 1185, 1201, 1303, 1319, 1333, 1347, 1373, 2615, 
        2617, 2619, 2621, 2623, 2625, 2627, 2629, 3101, 3202, 3476, 
        3477, 7336, 7342, 7348, 7354, 7360, 8464, 8466, 8468, 8470, 
        8472, 8474, 8476, 8478, 8480, 8482, 8484, 8486, 8488, 8490, 
        8492, 8494, 8714, 8716, 8718, 8720, 8722, 8724, 8726, 8728, 
        8730, 8732, 8734, 8736, 8738, 8740, 8742, 8744, 9185, 9185, 
        10286, 10288, 10290, 10292, 10294, 10667, 10670, 10673, 10676, 10679, 
        10679, 10705, 10707, 10704, 10705, 10706, 10708, 10798, 10800, 1149, 
        1187, 1187, 5698, 1377, 5698, 1377, 1434, 1434, 1540, 3140, 
        3204, 3204, 4087, 4585, 4587, 4587, 5699, 7158, 1712, 1712, 
        1712, 1712, 2491, 2491, 2491, 2497, 2497, 2497, 2503, 2503, 
        2503, 861, 861, 861, 4131, 7461, 7461, 7462, 6916, 6918, 
        6920, 6922, 6924, 6914, 13672, 13673, 13674, 13675, 14497, 14499, 
        14501, 11728, 4151, 4151, 14490, 14492, 14494, 10828, 
        3751, 3751, 11128, 3749, 3749, 3751, 3751, 3753, 3753, 3755, 
        3755, 1725, 1725, 4675, 4675, 3842, 3842, 3840, 3840, 3843, 
        2412, 2413, 2414, 13899, 4089, 4091, 4093, 
        4095, 4097, 4099, 4101, 4103, 4105, 4107, 4109, 4111, 4113, 
        4115, 4117, 4119, 3385, 3387, 3389, 3391, 3393, 3394, 13867,
        10828, 11730, 10887, 5698, 5698, 6524, 
        6570, 4153, 6535, 6536, 6537, 6538, 6568, 3122, 3122, 3122, 
        6809, 6809, 6737, 13672, 13673, 13674, 13675, 14600, 14602, 14603, 
        14605,  13899, 14497, 14499, 14501, 14490, 14492, 
        14494, 14592, 14593, 14594, 14479, 6585, 6585, 6737, 3140, 4087, 
        4585, 13858, 13861, 13870, 13876, 13873,
        13861, 13864, 13858, 10499,
        
        1727, 1729, 841, 839, 843, 845, 577, 579, 1381, 1383, 1385, 1387, 1131, 1133, 1097, 1169, 1061, 1323, 1325, 1329,1309,1311,1315, 1293, 1295, 1299, 1335, 1339, 1343, 1267, 1269, 1273, 1101, 1105, 1109, 1067, 1069, 1071, 1081, 1083, 1085, 1191, 1193, 1197, 1154, 1157, 1159, 1137, 1141, 1143, 1121, 1181, 2550,
        1725, 1731, 1681, 847, 849, 851, 853, 1065, 1099, 1135, 1301, 1287, 1211, 1430,1112, 1271, 1183, 1333, 1319, 1303, 1213, 1373, 1347, 1432, 1113, 1185, 1275, 1147, 4675, 11126, 1247, 4091, 4093, 4101, 4103, 4111, 4113, 14499, 14494
        ,1731,10366, 855, 857, 859, 861, 1333, 1319, 1303, 1213, 1373, 1347, 1432, 1163, 1113, 1127, 1079, 1201, 1185, 1275, 1147, 1683, 4675, 13006, 13003, 6739, 1305, 4587, 3755, 10564, 10589, 4153, 6809, 6918, 6920, 6922, 6568, 6129, 4131, 1247, 6139, 6524, 3751, 6131, 14497,14501, 14490, 14492
        ,6585, 4214, 6566,6733,6731,6735,6737, 1187, 4087, 11133, 3204, 4151, 14479
        ,8055,8056,8057,11181,11182, 13899 ,
        2293, 315, 325, 347,351, 333, 329,361, 379,2323, 2331, 2327,2003,2297,1896,1899,1897,1891,1893, 373, 383, 391, 7946, 361*/};


    public static int PVPItems(){
        return PVPItems[(int)(Math.random() * PVPItems.length)];
    }
    
    public static Location getRandomPVPZoneRespawnLocation() {
		  int random = Misc.random(6);
		  Location respawnLoc = Location.locate(3807, 3004, 0);
		  switch (random) {
		  case 0:
			  respawnLoc = Location.locate(3817, 2974, 0);
			  break;
		  case 1:
			  respawnLoc = Location.locate(3822, 3004, 0);
			  break;
		  case 2:
			  respawnLoc = Location.locate(3834, 2994, 0);
			  break;
		  case 3:
			  respawnLoc = Location.locate(3834, 2977, 0);
			  break;
		  case 4:
			  respawnLoc = Location.locate(3830, 2963, 0);
			  break;
		  case 5:
			  respawnLoc = Location.locate(3807, 2947, 0);
			  break;
		  case 6:
			  respawnLoc = Location.locate(3796, 2955, 0);
			  break;
		  }
		  return respawnLoc;
   }
    
    public static Location getRandomSafePkspawn() {
		  int random = Misc.random(6);
		  Location respawnLoc = Location.locate(2836, 5577, 0);
		  switch (random) {
		  case 0:
			  respawnLoc = Location.locate(2838, 5608, 0);
			  break;
		  case 1:
			  respawnLoc = Location.locate(2855, 5568, 0);
			  break;
		  case 2:
			  respawnLoc = Location.locate(2838, 5585, 2);
			  break;
		  case 3:
			  respawnLoc = Location.locate(2800, 5597, 0);
			  break;
		  case 4:
			  respawnLoc = Location.locate(2780, 5608, 0);
			  break;
		  case 5:
			  respawnLoc = Location.locate(2784, 5599, 0);
			  break;
		  case 6:
			  respawnLoc = Location.locate(2810, 5613, 0);
			  break;
    }
		return respawnLoc;

}
}
