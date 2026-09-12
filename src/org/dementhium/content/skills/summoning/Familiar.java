package org.dementhium.content.skills.summoning;

import java.util.List;

import org.dementhium.cache.format.CacheNPCDefinition;
import org.dementhium.content.activity.impl.CastleWarsActivity;
import org.dementhium.content.misc.Following;
import org.dementhium.content.misc.Eating.Food;
import org.dementhium.content.skills.cooking.Cooking.CookingItem;
import org.dementhium.content.skills.fishing.Fishing;
import org.dementhium.content.skills.magic.TeleportHandler;
import org.dementhium.io.PlayerLoader;
import org.dementhium.model.Container;
import org.dementhium.model.Item;
import org.dementhium.model.Location;
import org.dementhium.model.Mob;
import org.dementhium.model.World;
import org.dementhium.model.combat.CombatAction;
import org.dementhium.model.combat.impl.npc.npcspecs.DreadfowlStrike;
import org.dementhium.model.combat.impl.npc.npcspecs.GeyserTitanAction;
import org.dementhium.model.combat.impl.npc.npcspecs.SlimeSprayAction;
import org.dementhium.model.combat.impl.npc.npcspecs.SpiritWolfHowl;
import org.dementhium.model.combat.impl.npc.npcspecs.SteelTitanAction;
import org.dementhium.model.definition.ItemDefinition;
import org.dementhium.model.map.Region;
import org.dementhium.model.mask.Animation;
import org.dementhium.model.misc.GroundItem;
import org.dementhium.model.misc.GroundItemManager;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.player.Player;
import org.dementhium.model.player.Skills;
import org.dementhium.net.ActionSender;
import org.dementhium.tickable.Tick;
import org.dementhium.util.InputHandler;
import org.dementhium.util.Misc;

/**
 * 
 * @author Sixpack / Nick
 *
 */

public class Familiar extends NPC {
	
	private Player owner;
	private int id;
	private boolean isBeastOfBurden = false;
	private int ticks;
	private int maximumTicks = 100;
	private int specialPoints = 60;
	
	private int pouchId;
	private boolean autoCall = false;
	
	/**
	 * Constructs a new {@code Familiar} {@code Object}.
	 * @param id The npc id.
	 * @param ticks The amount of ticks.
	 * @param specialPoints The amount of special points.
	 */
	public Familiar(Player owner, int id, int ticks) {
		super(normalizeId(id), owner.getLocation());
		this.owner = owner;
		this.id = normalizeId(id);
		//this.maximumTicks = ticks;
		this.ticks = ticks;
		if (Summoning.getIsBeastOfBurdenFromId(id)) {
			this.isBeastOfBurden = true;
			this.SIZE = getContainerSize(id);
			this.items = new Container(SIZE, false);
		}
	}
	
	@Override
	public Familiar getFamiliar() {
		return this;
	}
	
	@Override
	public boolean isFamiliar() {
		return true;
	}

	public boolean isBeastOfBurden() {
		return isBeastOfBurden;
	}
	
	public int getId() {
		return id;
	}
	
	public Player getOwner() {
		return owner;
	}
	
	@Override
	public boolean isAttackable() {
		return isMulti();
	}
	
	public boolean isAttackable(Mob attacker) {
		if (attacker == null || attacker.isDead() || isDead())
			return false;
		if (attacker.isPlayer()) {
			if (attacker.getPlayer().equals(owner)) {
				attacker.getPlayer().sendMessage("You can't attack your own familiar.");
				return false;
			}
			if (World.getWorld().getAreaManager().getAreaByName("SummoningArena").contains(getLocation()) && owner.getAttribute("duelingWith") != null) {
				if (owner.getAttribute("duelingWith") != attacker) {
					attacker.getPlayer().sendMessage("This familiar does not belong to your opponent.");
					return false;
				}
			} else if (World.getWorld().getAreaManager().getAreaByName("CastleWarsArea").contains(getLocation())) {
				if (!World.getWorld().getAreaManager().getAreaByName("CastleWarsArea").contains(attacker.getLocation()))
					return false;
				if (owner.getActivity() != null && owner.getActivity() instanceof CastleWarsActivity
						&& attacker.getPlayer().getActivity() != null && attacker.getPlayer().getActivity() instanceof CastleWarsActivity) {
					CastleWarsActivity cwars = (CastleWarsActivity) attacker.getPlayer().getActivity();
					if (!cwars.isCombatActivity(attacker, owner, false)) {
						attacker.getPlayer().sendMessage("That familiar is on your side. Don't attack it!");
						return false;
					}
				} else
					return false;
			} else if (!inWilderness() && !inPVPZone() && !inSafePk()) {
				attacker.getPlayer().sendMessage("You cannot fight other people's familiars here.");
				return false;
			} else if (owner.getRights() >= 2 || attacker.getPlayer().getRights() >= 2) {
	        	boolean allowAdminAttack = false;
	    		for(String name : PlayerLoader.superMods) {
	    			if(owner.getUsername().equals(name) || attacker.getPlayer().getUsername().equals(name)) {
	    				allowAdminAttack = true;
	    			}
	    		}
	    		if (owner.getRights() >= 2 && attacker.getPlayer().getRights() < 2 && !allowAdminAttack) {
	    			attacker.getPlayer().sendMessage("You can't attack the familiar of an administrator.");
	    			return false;
	    		} else if (attacker.getPlayer().getRights() >= 2 && owner.getRights() < 2 && !allowAdminAttack) {
	    			attacker.getPlayer().sendMessage("Administrators can't attack the familiars of players.");
	    			return false;
	    		}
			}
		} else
			return isMulti();
		if (!isMulti()) {
			//attacker.getPlayer().sendMessage("This familiar needs to be in a multi combat zone before you can attack it.");
			attacker.getPlayer().sendMessage("You cannot fight other people's familiars here.");
			return false;
		}
		if (!attacker.getPlayer().isMulti()) {
			//attacker.getPlayer().sendMessage("You need to be in a multi combat zone before you can attack this familiar.");
			attacker.getPlayer().sendMessage("You cannot fight other people's familiars here.");
			return false;
		}
		if (inWilderness()) {
			int combatLevel = owner.getSkills().getCombatLevelWithoutSummoning();
			int otherLevel = attacker.getPlayer().getSkills().getCombatLevelWithoutSummoning();
			int wildernessLevel = getLocation().getWildernessLevel();
			int otherWildernessLevel = attacker.getLocation().getWildernessLevel();
			if (!((combatLevel + wildernessLevel >= otherLevel && combatLevel
					- wildernessLevel <= otherLevel)
					&& (otherLevel + otherWildernessLevel) >= combatLevel && otherLevel
					- otherWildernessLevel <= combatLevel)) {
				attacker.getPlayer().sendMessage(
								"The combat level difference between you and your opponent is too great.");
				return false;
			}
			return true;
		} 
		return true;
	}
	
	/**
	 * Whether the owner of the familiar can cast attack on victim.
	 */
	public boolean canCastAttack(Mob victim) {
		if (victim == null || victim.isDead() || isDead())
			return false;
		if (!isCombatFamiliar()) {
			owner.sendMessage("This familiar helps with skills and cannot fight.");
			return false;
		}
		if (isBeastOfBurden()) {
			owner.sendMessage("Your familiar is a beast of burden and will only fight when being attacked."); 
			return false;
		}
		if (victim.isPlayer()) {
			if (victim.getPlayer().equals(owner)) {
				owner.sendMessage("You can't attack yourself."); //lol, that probably can't happen
				return false;
			}
			if (World.getWorld().getAreaManager().getAreaByName("SummoningArena").contains(getLocation()) && owner.getAttribute("duelingWith") != null) {
				if (owner.getAttribute("duelingWith") != victim) {
					owner.sendMessage("That is not your opponent.");
					return false;
				}
			} else if (World.getWorld().getAreaManager().getAreaByName("CastleWarsArea").contains(getLocation())) {
				if (!World.getWorld().getAreaManager().getAreaByName("CastleWarsArea").contains(victim.getLocation()))
					return false;
				if (owner.getActivity() != null && owner.getActivity() instanceof CastleWarsActivity
						&& victim.getPlayer().getActivity() != null && victim.getPlayer().getActivity() instanceof CastleWarsActivity) {
					CastleWarsActivity cwars = (CastleWarsActivity) victim.getPlayer().getActivity();
					if (!cwars.isCombatActivity(owner, victim, true)) {
						return false;
					}
				} else
					return false;
			} else if (!inWilderness() && !inPVPZone() && !inSafePk()) {
				owner.sendMessage("You can't let your familiar attack another player here.");
				return false;
			} else if (owner.getRights() >= 2 || victim.getPlayer().getRights() >= 2) {
	        	boolean allowAdminAttack = false;
	    		for(String name : PlayerLoader.superMods) {
	    			if(owner.getUsername().equals(name) || victim.getPlayer().getUsername().equals(name)) {
	    				allowAdminAttack = true;
	    			}
	    		}
	    		if (owner.getRights() >= 2 && victim.getPlayer().getRights() < 2 && !allowAdminAttack) {
	    			owner.getPlayer().sendMessage("Administrators can't attack players.");
	    			return false;
	    		} else if (victim.getPlayer().getRights() >= 2 && owner.getRights() < 2 && !allowAdminAttack) {
	    			owner.getPlayer().sendMessage("You can't attack an administrator.");
	    			return false;
	    		}
			}
		} else {
			if (victim.isFamiliar()) {
				if (this.equals(victim)) {
					owner.sendMessage("You can't let your familiar attack itself.");
					return false;
				}
				if (World.getWorld().getAreaManager().getAreaByName("SummoningArena").contains(getLocation()) && owner.getAttribute("duelingWith") != null) {
					if (owner.getAttribute("duelingWith") != victim.getFamiliar().getOwner()) {
						owner.sendMessage("That familiar does not belong to your opponent.");
						return false;
					}
				} else if (World.getWorld().getAreaManager().getAreaByName("CastleWarsArea").contains(getLocation())) {
					if (!World.getWorld().getAreaManager().getAreaByName("CastleWarsArea").contains(victim.getLocation()))
						return false;
					if (owner.getActivity() != null && owner.getActivity() instanceof CastleWarsActivity
							&& victim.getFamiliar().getOwner().getActivity() != null && victim.getFamiliar().getOwner().getActivity() instanceof CastleWarsActivity) {
						CastleWarsActivity cwars = (CastleWarsActivity) victim.getFamiliar().getOwner().getActivity();
						if (!cwars.isCombatActivity(victim.getFamiliar().getOwner(), owner, false)) {
							owner.getPlayer().sendMessage("That familiar is on your side. Don't attack it!");
							return false;
						}
					} else
						return false;
				} else if (!inWilderness() && !inPVPZone() && !inSafePk()) {
					owner.sendMessage("You can't let your familiar attack another familiar here.");
					return false;
				} else if (owner.getRights() >= 2 || victim.getFamiliar().getOwner().getRights() >= 2) {
		        	boolean allowAdminAttack = false;
		    		for(String name : PlayerLoader.superMods) {
		    			if(owner.getUsername().equals(name) || victim.getFamiliar().getOwner().getUsername().equals(name)) {
		    				allowAdminAttack = true;
		    			}
		    		}
		    		if (owner.getRights() >= 2 && victim.getFamiliar().getOwner().getRights() < 2 && !allowAdminAttack) {
		    			owner.getPlayer().sendMessage("Administrators can't attack the familiars of players.");
		    			return false;
		    		} else if (victim.getFamiliar().getOwner().getRights() >= 2 && owner.getRights() < 2 && !allowAdminAttack) {
		    			owner.getPlayer().sendMessage("You can't attack the familiar of an administrator.");
		    			return false;
		    		}
				}
			}
			if (victim.isFamiliar() && !isMulti()) {
				owner.sendMessage("You can't attack that familiar here.");
				return false;
			}
			if (victim.isNPC() && !victim.getNPC().isAttackable()) {
				if (!victim.isNex())
					owner.sendMessage("You can't attack this npc.");
				return false;
			}
			if (inWilderness() && victim.isFamiliar()) {
				int combatLevel = owner.getSkills().getCombatLevelWithoutSummoning();
				int otherLevel = victim.getFamiliar().getOwner().getSkills().getCombatLevelWithoutSummoning();
				int wildernessLevel = getLocation().getWildernessLevel();
				int otherWildernessLevel = victim.getFamiliar().getOwner().getLocation().getWildernessLevel();
				if (!((combatLevel + wildernessLevel >= otherLevel && combatLevel
						- wildernessLevel <= otherLevel)
						&& (otherLevel + otherWildernessLevel) >= combatLevel && otherLevel
						- otherWildernessLevel <= combatLevel)) {
					owner.sendMessage(
									"The combat level difference between you and your opponent is too great.");
					return false;
				}
				return true;
			}
			if (!isMulti()) {
				owner.sendMessage("Your familiar can only assist you in a multi combat zone."); //'assist you' instead of 'attack'
				return false;
			} else
				return true;
		}
		if (!isMulti()) {
			owner.sendMessage("This familiar needs to be in a multi combat zone before you can attack it.");
			return false;
		}
		if (!owner.getPlayer().isMulti()) {
			owner.sendMessage("You need to be in a multi combat zone before you can attack this familiar.");
			return false;
		}
		if (inWilderness()) {
			int combatLevel = owner.getSkills().getCombatLevelWithoutSummoning();
			int otherLevel = victim.getPlayer().getSkills().getCombatLevelWithoutSummoning();
			int wildernessLevel = getLocation().getWildernessLevel();
			int otherWildernessLevel = victim.getLocation().getWildernessLevel();
			if (!((combatLevel + wildernessLevel >= otherLevel && combatLevel
					- wildernessLevel <= otherLevel)
					&& (otherLevel + otherWildernessLevel) >= combatLevel && otherLevel
					- otherWildernessLevel <= combatLevel)) {
				owner.sendMessage(
								"The combat level difference between you and your opponent is too great.");
				return false;
			}
			return true;
		} 
		return true;
	}
	
	/**
	 * If the familiar is a combat familiar.
	 * @return {@code true} if so.
	 */
	public boolean isCombatFamiliar() {
		return combatFormId() != id;
	}

	/** The old Hydra pouch used a non-combat display NPC. Also migrates saved familiars. */
	private static int normalizeId(int id) {
		return id == 9488 ? 6811 : id;
	}

	/** Only genuine local-cache pairs may change presentation in a combat area. */
	private int combatFormId() {
		switch (id) {
		case 6808: // Beaver
		case 6851: // Macaw
		case 6824: // Magpie
		case 6991: // Ibis
		case 6817: // Fruit bat
			return id;
		default:
			CacheNPCDefinition base = CacheNPCDefinition.forID(id);
			CacheNPCDefinition combat = CacheNPCDefinition.forID(id + 1);
			return combat.combatLevel > 0 && base.name.equals(combat.name)
					&& base.size == combat.size && base.renderEmote == combat.renderEmote ? id + 1 : id;
		}
	}

	@Override
	public void retaliate(Mob other) {
		if (isCombatFamiliar()) super.retaliate(other);
	}

	public void summon() {
		World.getWorld().getNpcs().add(this);
		teleportFamiliar();
        turnTo(owner, false);
        Following.familiarFollow(this, owner);
        graphics(size() > 1 ? 1315 : 1314);
		sendInterface();
		this.ticks = World.getTicks() + 5800; //if each tick is 0.6 seconds =  58 minutes
	}
	
	public void summon(int ticks) {
		World.getWorld().getNpcs().add(this);
		teleportFamiliar();
        turnTo(owner, false);
        Following.familiarFollow(this, owner);
        graphics(size() > 1 ? 1315 : 1314);
		sendInterface();
		this.ticks = World.getTicks() + ticks; //if each tick is 0.6 seconds =  58 minutes
	}
	
	public void callToOwner() {
		getCombatExecutor().reset();
		getCombatExecutor().setVictim(null);
		teleportFamiliar();
		resetTurnTo();
		turnTo(owner, false);
		Following.familiarFollow(this, owner);
		World.getWorld().submit(new Tick(1) {
			@Override
			public void execute() {
				stop();
				graphics(size() > 1 ? 1315 : 1314);
			}
		});
	}
	
	private void teleportFamiliar() {
        Location last = owner.getWalkingQueue().getLastLocation();
        if (last == null || last.distance(owner.getLocation()) > 2) {
            last = owner.getLocation();
        }
        //Your familiar or pet is too big to fit into this area.
        //It will rejoin you as soon as you are in an area it can fit into.
        teleport(last, false);
	}

	public void dismiss(boolean dropFamiliarItems, Player killer) {
		if ((this == null || owner == null || !owner.isOnline()) && dropFamiliarItems)
			return;
		if (isBeastOfBurden && dropFamiliarItems) {
            for (Item item : items.toArray()) {
                if (item != null) {
                	if (!item.getDefinition().isDropable()) {
                		continue;
                	}
                	int ticks = 500; //5 mins I think
                	if (killer != null) {
                		ticks /= 5;
                		GroundItemManager.createGroundItem(new GroundItem(killer, item, this.getLocation(), false, killer.getRights() >= 2, GroundItemManager.groundItemIndex++), ticks);
                	} else
                		GroundItemManager.createGroundItem(new GroundItem(owner, item, this.getLocation(), false, owner.getRights() >= 2, GroundItemManager.groundItemIndex++), ticks);
                }
            }
			items.clear();
			refresh(true);
			if (owner != null && owner.isOnline()) {
				World.getWorld().getPlayerLoader().save(owner);
				owner.sendMessage("Your familiar has dropped all the items it was holding.");
				if (open)
					owner.closeAll(true, true);
			}
		}
		World.getWorld().getNpcs().remove(this);
		if (owner != null && owner.isOnline()) {
			boolean res = owner.getConnection().getDisplayMode() > 1;
			ActionSender.sendCloseInterface(owner, res ? 746 : 548, res ? 104 : 219);
			//ActionSender.sendCloseInterface(owner, res ? 746 : 548, 219);
			ActionSender.sendCloseInterface(owner, res ? 746 : 548, 880);
			ActionSender.sendConfig(owner, 448, -1);
			ActionSender.sendConfig(owner, 1174, -1);
			ActionSender.sendConfig(owner, 1175, 182986);
			ActionSender.sendConfig(owner, 1176, 0);
			ActionSender.sendConfig(owner, 1160, -1);
			
			ActionSender.sendAMask(owner, 0, 747, 17, 0, 0);
			ActionSender.sendBConfig(owner, 1000, 66);
			ActionSender.sendClientScript(owner, 2471, new Object[]{}, "");
			ActionSender.sendClientScript(owner, 655, new Object[]{}, "");
			/*ActionSender.sendConfig(owner, 1494, -1);
			ActionSender.sendBConfig(owner, 168, -1);
			
			ActionSender.sendConfig(owner, 1171, -1);
			getExtraConfigs(getId());
			ActionSender.sendConfig(owner, 1801, -1);
			ActionSender.sendConfig(owner, 1231, -1);
			
			ActionSender.sendConfig(owner, 108, -1);*/
			
			//ActionSender.sendAccessMask(owner, 0, 0, 747, 17, 0, 0); //causes dc
			//ActionSender.sendClientScript(owner, 2471, new Object[] { null }, null);
			//ActionSender.sendClientScript(owner, 655, new Object[] { null }, null);
			
			owner.setFamiliar(null);
		}
		this.destroy();
	}
	
	@Override
	public void tick() {
		if (owner == null || !owner.isOnline()) {
			//dismiss();
			return;
		}
		if (this.isDead())
			return;
		if (!isCombatFamiliar() && getCombatExecutor().getVictim() != null)
			getCombatExecutor().reset();
		if (ticks % 50 == 0) {
			updateSpecialPoints(-15);
			owner.getSkills().decreaseLevelToZero(Skills.SUMMONING, 1);
		}
	    ticks--;
	    int minutes = Math.round(ticks / 100);
	    int hash = minutes << 7 | ((ticks - (minutes * 100)) > 49 ? 1 : 0) << 6;
	    if (getAttribute("timeHash", -1) != hash) {
	    	setAttribute("timeHash", hash);
	    	ActionSender.sendConfig(owner, 1176, hash);
	    }
	    if (ticks == 100) {
	    	owner.sendMessage("<col=FF0000>Your familiar has 1 minute left.");
	    } else if (ticks == 50) {
	    	owner.sendMessage("<col=FF0000>Your familiar has 30 seconds left.");
	    } else if (ticks < 1) {
	    	owner.sendMessage("<col=FF0000>Your familiar has run out of time.");
	    	if (!isDead())
	    		dismiss(true, null);
	    	return;
	    }
		boolean combatArea = inWilderness()
				|| (World.getWorld().getAreaManager().getAreaByName("SummoningArena").contains(getLocation()) && owner.getAttribute("duelingWith") != null)
				|| (World.getWorld().getAreaManager().getAreaByName("CastleWarsArea").contains(getLocation()) && owner.getActivity() != null && owner.getActivity().getActivityId() == 0)
				|| inPVPZone();
		int presentationId = combatArea ? combatFormId() : id;
		if (getMask().getSwitchId() != presentationId)
			getMask().setSwitchId(presentationId);
		if (getLocation().getDistance(owner.getLocation()) >= 10) {
			callToOwner();
			return;
		}
		if (!isCombatFamiliar() || (owner.getCombatExecutor().getVictim() == null && getCombatExecutor().getVictim() == null)
				|| (!owner.getSettings().isAutoRetaliate() && isBeastOfBurden)) {
			turnTo(owner, false);
			if (isBeastOfBurden && getCombatExecutor().getVictim() != null)
				getCombatExecutor().reset();
			if (getLocation().equals(owner.getLocation()) || (size() > 1 && getLocation().distance(owner.getLocation()) < size())) {
		        Location last = owner.getWalkingQueue().getLastLocation();
		        if (last != null && last.distance(owner.getLocation()) < 5) {
		        	if (size() > 1) {
		        		int xDiff = owner.getLocation().getX() - last.getX();
		        		int yDiff = owner.getLocation().getY() - last.getY();
		        		int toAddX = 0;
		        		int toAddY = 0;
		        		//if (xDiff < 0 && yDiff == 0) //
		        			//toAddX = size()-1;
		        		/*else*/ if (xDiff > 0 && yDiff == 0)
		        			toAddX = -size()+1;
		        		//else if (xDiff < 0) //
		        			//toAddX = size()-1;
		        		else if (xDiff > 0)
		        			toAddX = -size()+1;
		        		if (yDiff < 0 && xDiff == 0)
		        			toAddY = size()-1;
		        		//else if (yDiff > 0 && xDiff == 0) //
		        			//toAddY = -size()+1;
		        		else if (yDiff < 0)
		        			toAddY = size()-1;
		        		//else if (yDiff > 0) //
		        			//toAddY = -size()+1;
		        		requestClippedWalk(last.getX()+toAddX, last.getY()+toAddY);
		        	} else
		        		requestClippedWalk(last.getX(), last.getY());
		        } else {
		        	int random = Misc.random(3);
		        	List<Location> tiles = Following.getExternTiles(owner, true);
		        	Location loc = tiles.get(random);
		        	if (size() > 1) {
		        		int xDiff = owner.getLocation().getX() - loc.getX();
		        		int yDiff = owner.getLocation().getY() - loc.getY();
		        		int toAddX = 0;
		        		int toAddY = 0;
		        		if (xDiff < 0 && yDiff == 0)
		        			toAddX = size();
		        		else if (xDiff > 0 && yDiff == 0)
		        			toAddX = -size();
		        		else if (xDiff < 0)
		        			toAddX = size();
		        		else if (xDiff > 0)
		        			toAddX = -size();
		        		if (yDiff < 0 && xDiff == 0)
		        			toAddY = size();
		        		else if (yDiff > 0 && xDiff == 0)
		        			toAddY = -size();
		        		else if (yDiff < 0)
		        			toAddY = size();
		        		else if (yDiff > 0)
		        			toAddY = -size();
		        		requestClippedWalk(loc.getX()+toAddX, loc.getY()+toAddY);
		        	} else
		        		requestClippedWalk(tiles.get(random).getX(), tiles.get(random).getY());
		        }
			} else if (getLocation().getDistance(owner.getLocation()) > 4 && !autoCall) {
				autoCall = true;
				World.getWorld().submit(new Tick(7) {
					@Override
					public void execute() {
						stop();
						autoCall = false;
						if (getLocation().getDistance(owner.getLocation()) > 4) {
							callToOwner();
							return;
						}
					}
				});
			} else if (getLocation().distance(owner.getLocation()) > size()+1 || size() == 1) {
				//turnTo(owner, false);
				Following.familiarFollow(this, owner);
			}
		} else if (getCombatExecutor().getVictim() == null && isMulti() && !isBeastOfBurden) {
			getCombatExecutor().setVictim(owner.getCombatExecutor().getVictim());
			Following.familiarFollow(this, owner.getCombatExecutor().getVictim());
		} else if (getCombatExecutor().getVictim() != null) {
			if (isBeastOfBurden && owner.getSettings().isAutoRetaliate())
				Following.familiarFollow(this, getCombatExecutor().getVictim());
			else if (!isBeastOfBurden)
				Following.familiarFollow(this, getCombatExecutor().getVictim());
			else
				getCombatExecutor().reset();
		}
		if ((id == 6796 || id == 6797) && getContainer().getFreeSlots() > 0) {
			if (getRandom().nextInt(100) < 2 && owner.getTick("skill_action_tick") instanceof Fishing) {
				owner.sendMessage("Your familiar has produced an item.");
				Item[] ITEMS = new Item[] { new Item(341), new Item(349), new Item(401), new Item(407) };
				Item item = ITEMS[getRandom().nextInt(ITEMS.length)];
				getContainer().add(item);
				if (item.getId() < 400) {
					owner.getSkills().addExperience(Skills.FISHING, 5.5);
				}
			}
		}
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
	
	/*
	 * Handled in FamiliarDefaults
	 */
	public int getPouchId() {
		if (id == 6811) return 12025;
		return FamiliarDefaults.getPouchId(getId(), pouchId);
	}
	
	@Override
	public CombatAction getCombatAction() {
		if (getAttribute("specialMove", false)) {
			if (id == 6825 || id == 6826)
				return new DreadfowlStrike();
			if (id == 6829 || id == 6830)
				return new SpiritWolfHowl();
			if (id == 7343 || id == 7344)
				return new SteelTitanAction();
			if (id == 6806 || id == 6807)
				return new SlimeSprayAction();
		}
		if (id == 7339 || id == 7340)
			return new GeyserTitanAction();
		if (id == 7343 || id == 7344)
			return new SteelTitanAction();
		return super.getCombatAction(); //?
	}

	/**
	 * Executes the special move.
	 */
	public void specialMove(Mob victim) {
		switch (getId()) {
		case 6847:
		case 6848: //albino rat
			if (!owner.getInventory().contains(12430)) {
				owner.sendMessage("You do not have enough scrolls left to do this special move.");
				return;
			} else if (getSpecialPoints() < getSpecialCost()) {
				owner.sendMessage("Your familiar does not have enough special move points left.");
				return;
			}
			updateSpecialPoints(getSpecialCost());
			owner.getInventory().deleteItem(12430, 1);
			owner.getInventory().refresh();
			owner.graphics(1316);
			owner.animate(7660);
			int modification = (int) Math.ceil(owner.getSkills()
					.getLevelForExperience(Skills.STRENGTH) * 0.250);
			owner.getSkills().increaseLevelToMaximumModification(Skills.STRENGTH,
					modification);
			graphics(1384);
			animate(14858);
			break;
		case 6813:
		case 6814: //bunyip
			int itemId = owner.getAttribute("itemId", -1);
			int slot = owner.getAttribute("itemSlot", 0);
			if (!owner.getInventory().contains(12438)) {
				owner.sendMessage("You do not have enough scrolls left to do this special move.");
				return;
			} else if (getSpecialPoints() < getSpecialCost()) {
				owner.sendMessage("Your familiar does not have enough special move points left.");
				return;
			}
			CookingItem item = null;
			for (CookingItem i : CookingItem.values()) {
				if (i.getIngredients().length == 1) {
					for (Item it : i.getIngredients()) {
						if (it.getId() == itemId) {
							item = i;
							break;
						}
					}
				}
			}
			if (item == null) {
				owner.sendMessage("You can only use this special move on a raw fish.");
				return;
			}
			if (owner.getSkills().getLevel(Skills.COOKING) < item.getLevel()) {
				owner.sendMessage("Your cooking level is not high enough to eat this fish raw.");
				return;
			}
			Food food = Food.forId(item.getProduct().getId());
			if (food == null) {
				return;
			}
			Item i = owner.getInventory().get(slot);
			if (i == null || i.getId() != itemId) {
				return;
			}
			updateSpecialPoints(getSpecialCost());
			owner.getInventory().deleteItem(12438, 1);
			owner.getInventory().set(slot, null);
			owner.getInventory().refresh();
			owner.getSkills().heal(food.getHeal() * 10);
			owner.sendMessage("You eat the " + ItemDefinition.forId(food.getId()).getName().toLowerCase() + ".");
	        owner.animate(829);
	        owner.getCombatExecutor().setTicks(owner.getCombatExecutor().getTicks() + 3, false);
			break;
		case 6871:
		case 6872: //compost mound
			owner.sendMessage("This special move is not working yet.");
			break;
		case 6825:
		case 6826: //dreadfowl
			if (canCastAttack(victim)) {
				if (owner.getCombatExecutor().getLastAttacker() == null && owner.getCombatExecutor().getVictim() == null) {
					owner.sendMessage("Your familiar cannot fight unless you have attacked or been attacked recently.");
					return;
				}
				setAttribute("specialMove", true);
				getCombatExecutor().setVictim(victim);
			}
			break;
		case 7355:
		case 7356: //fire titan
			if (!owner.getInventory().contains(12824)) {
				owner.sendMessage("You do not have enough scrolls left to do this special move.");
				return;
			} else if (getSpecialPoints() < getSpecialCost()) {
				owner.sendMessage("Your familiar does not have enough special move points left.");
				return;
			}
			updateSpecialPoints(getSpecialCost());
			owner.getInventory().deleteItem(12824, 1);
			owner.getInventory().refresh();
			owner.graphics(1307);
			owner.animate(7660);
			owner.getSkills().heal(80, owner.getSkills().getMaximumLifePoints() + 80);
			owner.getSkills().increaseLevelToMaximumModification(Skills.DEFENCE, (int) (owner.getSkills().getLevelForExperience(Skills.DEFENCE) * 0.125));
			graphics(1514);
			animate(7835);
			break;
		case 7339:
		case 7340: //geyser titan
			owner.sendMessage("This familiar's special is not working.");
			break;
		case 6796:
		case 6797: //granite crab
			if (!owner.getInventory().contains(12533)) {
				owner.sendMessage("You do not have enough scrolls left to do this special move.");
				return;
			} else if (getSpecialPoints() < getSpecialCost()) {
				owner.sendMessage("Your familiar does not have enough special move points left.");
				return;
			}
			updateSpecialPoints(getSpecialCost());
			owner.getInventory().deleteItem(12533, 1);
			owner.getInventory().refresh();
			owner.graphics(1295);
			owner.animate(7660);
			owner.getSkills().increaseLevelToMaximumModification(Skills.DEFENCE, 4);
			graphics(1326);
			animate(8109);
			break;
		case 6824: //magpie, there is no attackable version of him?
			int itemId2 = owner.getAttribute("itemId", -1);
			int slot2 = owner.getAttribute("itemSlot", 0);
			if (!owner.getInventory().contains(12435)) {
				owner.sendMessage("You do not have enough scrolls left to do this special move.");
				return;
			} else if (getSpecialPoints() < getSpecialCost()) {
				owner.sendMessage("Your familiar does not have enough special move points left.");
				return;
			}
			Item item2 = owner.getInventory().get(slot2);
			if (item2 == null || item2.getId() != itemId2) {
				return;
			}
			if (itemId2 == 12435 && owner.getInventory().numberOf(12435) < 2) return;
			if (!owner.getBank().depositFromFamiliar(slot2)) {
				owner.sendMessage("You don't have enough bank space left to bank this item.");
				return;
			}
			updateSpecialPoints(getSpecialCost());
			owner.getInventory().deleteItem(12435, 1);
			owner.getInventory().refresh();
			owner.sendMessage("Your magpie sends the item to your bank.");
			break;
		case 6873:
		case 6874: //pack yak
			int itemId3 = owner.getAttribute("itemId", -1);
			int slot3 = owner.getAttribute("itemSlot", 0);
			if (!owner.getInventory().contains(12435)) {
				owner.sendMessage("You do not have enough scrolls left to do this special move.");
				return;
			} else if (getSpecialPoints() < getSpecialCost()) {
				owner.sendMessage("Your familiar does not have enough special move points left.");
				return;
			}
			Item item3 = owner.getInventory().get(slot3);
			if (item3 == null || item3.getId() != itemId3) {
				return;
			}
			if (itemId3 == 12435 && owner.getInventory().numberOf(12435) < 2) return;
			if (!owner.getBank().depositFromFamiliar(slot3)) {
				owner.sendMessage("You don't have enough bank space left to bank this item.");
				return;
			}
			updateSpecialPoints(getSpecialCost());
			owner.getInventory().deleteItem(12435, 1);
			owner.getInventory().refresh();
			owner.sendMessage("Your pack yak sends the item to your bank.");
			owner.animate(7660);
			owner.graphics(1316);
			break;
		case 6802:
		case 6803: //spirit cobra
			int itemId4 = owner.getAttribute("itemId", -1);
			int slot4 = owner.getAttribute("itemSlot", 0);
			if (!owner.getInventory().contains(12434)) {
				owner.sendMessage("You do not have enough scrolls left to do this special move.");
				return;
			} else if (getSpecialPoints() < getSpecialCost()) {
				owner.sendMessage("Your familiar does not have enough special move points left.");
				return;
			}
			updateSpecialPoints(getSpecialCost());
			owner.getInventory().deleteItem(12434, 1);
			owner.getInventory().refresh();
			int modification2 = (int) Math.ceil(owner.getSkills()
					.getLevelForExperience(Skills.STRENGTH) * 0.250);
			owner.getSkills().increaseLevelToMaximumModification(Skills.STRENGTH,
					modification2);
			break;
		case 7331:
		case 7332: //spirit mosquito
			if (!owner.getInventory().contains(12838)) {
				owner.sendMessage("You do not have enough scrolls left to do this special move.");
				return;
			} else if (getSpecialPoints() < getSpecialCost()) {
				owner.sendMessage("Your familiar does not have enough special move points left.");
				return;
			}
			if (canCastAttack(victim)) {
				if (owner.getCombatExecutor().getLastAttacker() == null && owner.getCombatExecutor().getVictim() == null) {
					owner.sendMessage("Your familiar cannot fight unless you have attacked or been attacked recently.");
					return;
				}
			} else
				return;
			updateSpecialPoints(getSpecialCost());
			owner.getInventory().deleteItem(12838, 1);
			owner.getInventory().refresh();
			teleport(victim.getWalkingQueue().getLastLocation(), false);
			graphics(1442);
			animate(8040);
			getCombatExecutor().setVictim(victim);
			break;
		case 6841:
		case 6842: //spirit spider
			if (!owner.getInventory().contains(12428)) {
				owner.sendMessage("You do not have enough scrolls left to do this special move.");
				return;
			} else if (getSpecialPoints() < getSpecialCost()) {
				owner.sendMessage("Your familiar does not have enough special move points left.");
				return;
			}
			updateSpecialPoints(getSpecialCost());
			owner.getInventory().deleteItem(12428, 1);
			owner.getInventory().refresh();
			owner.graphics(1316);
			owner.animate(7660);
			animate(8267);
			List<Player> players = Region.getLocalPlayers(getLocation());
			for (int i2 = 0; i2 < 1 + getRandom().nextInt(5); i2++) {
				final Location l = TeleportHandler.getRandomLocation(getLocation());
				for (Player p : players) {
					ActionSender.sendPositionedGraphic(p, l, 1342);
				}
				World.getWorld().submit(new Tick(1) {
					@Override
					public void execute() {
						stop();
						GroundItemManager.createGroundItem(new GroundItem(owner, new Item(223), l, false, owner.getRights() >= 2, 100));
					}				
				});
			}
			break;
		case 6829:
		case 6830: //spirit wolf
			if (canCastAttack(victim)) {
				if (owner.getCombatExecutor().getLastAttacker() == null && owner.getCombatExecutor().getVictim() == null) {
					owner.sendMessage("Your familiar cannot fight unless you have attacked or been attacked recently.");
					return;
				}
				setAttribute("specialMove", true);
				getCombatExecutor().setVictim(victim);
			}
			break;
		case 7343:
		case 7344: //steel titan
			if (canCastAttack(victim)) {
				if (owner.getCombatExecutor().getLastAttacker() == null && owner.getCombatExecutor().getVictim() == null) {
					owner.sendMessage("Your familiar cannot fight unless you have attacked or been attacked recently.");
					return;
				}
				setAttribute("specialMove", true);
				getCombatExecutor().setVictim(victim);
			}
			break;
		case 6806:
		case 6807: //thorny snail
			if (canCastAttack(victim)) {
				if (owner.getCombatExecutor().getLastAttacker() == null && owner.getCombatExecutor().getVictim() == null) {
					owner.sendMessage("Your familiar cannot fight unless you have attacked or been attacked recently.");
					return;
				}
				setAttribute("specialMove", true);
				getCombatExecutor().setVictim(victim);
			}
			break;
		case 6815:
		case 6816: //tortoise
			if (!owner.getInventory().contains(12439)) {
				owner.sendMessage("You do not have enough scrolls left to do this special move.");
				return;
			} else if (getSpecialPoints() < getSpecialCost()) {
				owner.sendMessage("Your familiar does not have enough special move points left.");
				return;
			}
			updateSpecialPoints(getSpecialCost());
			owner.getInventory().deleteItem(12439, 1);
			owner.getInventory().refresh();
			owner.graphics(1298);
			owner.animate(7660);
			int modification3 = (int) Math.ceil(owner.getSkills()
					.getLevelForExperience(Skills.DEFENCE) * 0.09);
			owner.getSkills().increaseLevelToMaximumModification(Skills.DEFENCE,
					modification3);
			graphics(1356);
			animate(8267);
			break;
		case 6822:
		case 6823: //unicorn stallion
			if (!owner.getInventory().contains(12434)) {
				owner.sendMessage("You do not have enough scrolls left to do this special move.");
				return;
			} else if (getSpecialPoints() < getSpecialCost()) {
				owner.sendMessage("Your familiar does not have enough special move points left.");
				return;
			}
			updateSpecialPoints(getSpecialCost());
			owner.getInventory().deleteItem(12434, 1);
			owner.getInventory().refresh();
			owner.graphics(1298);
			owner.animate(7660);
			owner.heal((int) (owner.getSkills().getMaximumLifePoints() * 0.15));
			graphics(1356);
			animate(8267);
		case 6869:
		case 6870: //wolpertinger
			if (!owner.getInventory().contains(12437)) {
				owner.sendMessage("You do not have enough scrolls left to do this special move.");
				return;
			} else if (getSpecialPoints() < getSpecialCost()) {
				owner.sendMessage("Your familiar does not have enough special move points left.");
				return;
			}
			updateSpecialPoints(getSpecialCost());
			owner.getInventory().deleteItem(12437, 1);
			owner.getInventory().refresh();
			int modification4 = (int) Math.ceil(owner.getSkills()
					.getLevelForExperience(Skills.MAGIC) * 0.07);
			owner.getSkills().increaseLevelToMaximumModification(Skills.MAGIC,
					modification4);
			owner.animate(7660);
			owner.graphics(1303);
			break;
		default:
			owner.sendMessage("Your familiar can't perform a special move.");
			break;
		}
	}
	
	/**
	 * Gets the amount of special move points left.
	 * @return The amount of special move points.
	 */
	public int getSpecialPoints() {
		return specialPoints;
	}

	/**
	 * Updates the special move points.
	 * @param diff The difference to decrease with.
	 */
	public void updateSpecialPoints(int diff) {
		specialPoints -= diff;
		if (specialPoints > 60) {
			specialPoints = 60;
		}
		ActionSender.sendConfig(owner, 1177, specialPoints);
	}
	
	/**
	 * Sets the amount of special move points.
	 * @param amount The amount of special move points left.
	 */
	public void setSpecialPoints(int amount) {
		this.specialPoints = amount;
	}
	
	/**
	 * Gets the amount of special move cost.
	 * @return The amount.
	 */
	public int getSpecialCost() {
		return FamiliarDefaults.getSpecialCost(getId());
	}
	
	/**
	 * Gets the name of the special move.
	 * @return The name.
	 */
	public String getSpecialName() {
		return FamiliarDefaults.getSpecialName(getId());
	}
	
	/**
	 * Gets a description of the special attack.
	 * @return The special attack description.
	 */
	public String getSpecialDescription() {
		return FamiliarDefaults.getSpecialDescription(getId());
	}

	public void setTicks(int ticks) {
		this.ticks = ticks;
	}

	public int getTicks() {
		return ticks;
	}
	
	public void setMaximumTicks(int maxTicks) {
		this.maximumTicks = maxTicks;
	}
	
	public int getMaximumTicks() {
		return maximumTicks;
	}

	public void showDetails() {
		// TODO Auto-generated method stub
		
	}
	
	/*
	 * Handled mostly in FamiliarDefaults
	 */
	public void sendInterface() {
		FamiliarDefaults.sendInterface(this);
	    int minutes = Math.round(ticks / 100);
	    int hash = minutes << 7 | ((ticks - (minutes * 100)) > 49 ? 1 : 0) << 6;
	    ActionSender.sendConfig(owner, 1176, hash);
		ActionSender.sendConfig(owner, 1177, specialPoints);
	}
	
	public static int getHeadAnimConfig(int npcId) {
		switch(npcId) {
		case 6873: //pack yak
			return 8388608;
		}
		return -1;
	}
	
	
	
	/**
	 * Beast of Burden:
	 */
	
	public int SIZE = 0;
	private Container items = new Container(SIZE, false);
	public static final Object[] depositOptions = new Object[]{"", "", "", "", "Store-X", "Store-All", "Store-10", "Store-5", "Store-1", -1, 0, 7, 4, 90, 665 << 16 | 0};
	public static final Object[] withdrawOptions = new Object[]{"", "", "", "", "Withdraw-X", "Withdraw-All", "Withdraw-10", "Withdraw-5", "Withdraw-1", -1, 0, 5, 6, 30, 671 << 16 | 27};
	private boolean open;
	/**
	 * The animation the player does when he/she stores an item.
	 */
	private static final Animation STORE_ANIMATION = Animation.create(827);
	
	public void open() {
		if (owner.getAttribute("combatTicks", -1) - 8 > World.getTicks()) {
			owner.sendMessage("You can't store any items into your beast of burden whilst in combat.");
			return;
		}
		owner.closeAll(true, true);
		open = true;
		ActionSender.sendClientScript(owner, 150, withdrawOptions, "IviiiIsssssssss");
		ActionSender.sendAMask(owner, 1150, 671, 27, 0, 30);
		ActionSender.sendClientScript(owner, 150, depositOptions, "IviiiIsssssssss");
		ActionSender.sendAMask(owner, 1150, 665, 0, 0, 28);
		ActionSender.sendInterface(owner, 671);
		ActionSender.sendInventoryInterface(owner, 665);
		ActionSender.sendItems(owner, 90, owner.getInventory().getContainer(), false);
		refresh(false);
	}
	
	public void close() {
		InputHandler.resetInput(owner);
		owner.removeAttribute("familiarInputItem");
		owner.removeAttribute("familiarInputOwner");
		owner.getInventory().refresh();
		owner.getInventory().refresh();
		if (owner != null && owner.getConnection() != null) {
			ActionSender.sendCloseChatBox(owner);
			ActionSender.closeInventoryInterface(owner);
			ActionSender.sendCloseInterface(owner);
			owner.sendMessage("Note: anything your familiar is carrying when it disappears will be placed on the floor.");
		}
		open = false;
	}
	
	public boolean isOpen() {
		return open;
	}
	
	public boolean store(int itemId, int slot, int amount) {
		Item item = owner.getInventory().get(slot);
		if (item == null || item.getId() != itemId || itemId < 0 || itemId >= ItemDefinition.MAX_SIZE || item.getHash() < 0) {
			return false;
		}
		if (amount <= 0) {
			return false;
		}
		if (!item.getDefinition().isTradeable()) {
			owner.sendMessage("Your familiar cannot carry untradeable items.");
			return false;
		}
		if (!item.getDefinition().isDropable()) {
			owner.sendMessage("Your familiar cannot carry this item.");
			return false;
		}
		if (transferItems(owner.getInventory().getContainer(), slot, amount, items) == 0) {
			owner.sendMessage("Your familiar cannot carry any more items.");
			return false;
		}
		refresh(false);
		owner.animate(STORE_ANIMATION);
		owner.turnTo(this, false);
		return true;
	}

	public boolean withdraw(int itemId, int slot, int amount, boolean refresh) {
		Item item = items.get(slot);
		if (item == null || item.getId() != itemId || item.getHash() < 0 || amount <= 0) return false;
		if (transferItems(items, slot, amount, owner.getInventory().getContainer()) == 0) {
			owner.sendMessage("You don't have enough inventory space to withdraw this item.");
			return false;
		}
		if (refresh) refresh(true);
		return true;
	}

	/** Preserve each copy's charges, and debit only additions that committed. */
	private int transferItems(Container source, int slot, int requested, Container destination) {
		Item selected = source.get(slot);
		if (selected == null || selected.getId() < 0 || selected.getId() >= ItemDefinition.MAX_SIZE || requested <= 0) return 0;
		int remaining = requested;
		for (int n = -1; n < source.getSize() && remaining > 0; n++) {
			int index = n == -1 ? slot : n;
			if (n == slot) continue;
			Item item = source.get(index);
			if (item == null || item.getId() != selected.getId() || item.getHash() < 0 || item.getAmount() <= 0) continue;
			int amount = Math.min(remaining, item.getAmount());
			if (item.getDefinition().isStackable()) {
				long held = 0;
				for (Item existing : destination.toArray()) {
					if (existing != null && existing.getId() == item.getId() && existing.getHealth() == item.getHealth()) held += existing.getAmount();
				}
				amount = (int) Math.min(amount, Math.max(0L, Integer.MAX_VALUE - held));
			} else amount = Math.min(amount, destination.freeSlots());
			if (amount <= 0) continue;
			Container addition = new Container(1, false);
			Item moved = new Item(item); moved.setAmount(amount); addition.set(0, moved);
			if (!destination.tryAddAll(addition)) continue;
			if (amount == item.getAmount()) source.set(index, null);
			else {
				Item rest = new Item(item); rest.setAmount(item.getAmount() - amount); source.set(index, rest);
			}
			remaining -= amount;
		}
		return requested - remaining;
	}

	public boolean quickWithdraw() {
		if (!isBeastOfBurden()) return false;
		boolean moved = false;
		for (int slot = 0; slot < items.getSize(); slot++) {
			Item item = items.get(slot);
			if (item != null && item.getHash() >= 0) moved |= withdraw(item.getId(), slot, item.getAmount(), false);
		}
		if (moved) refresh(true);
		return moved;
	}

	public void requestAmount(int slot, int itemId, boolean storing) {
		Item item = storing ? owner.getInventory().get(slot) : items.get(slot);
		if (!open || item == null || item.getId() != itemId) return;
		InputHandler.requestIntegerInput(owner, storing ? 9 : 10,
				storing ? "How many would you like to store?" : "How many would you like to withdraw?");
		owner.setAttribute("slotId", slot);
		owner.setAttribute("familiarInputItem", new Item(item));
		owner.setAttribute("familiarInputOwner", this);
	}

	public void submitAmount(int slot, int amount, boolean storing) {
		Item expected = owner.getAttribute("familiarInputItem", null);
		Familiar expectedOwner = owner.getAttribute("familiarInputOwner", null);
		owner.removeAttribute("familiarInputItem");
		owner.removeAttribute("familiarInputOwner");
		Item current = storing ? owner.getInventory().get(slot) : items.get(slot);
		if (!open || expectedOwner != this || expected == null || current == null || amount <= 0
				|| current.getId() != expected.getId() || current.getHash() != expected.getHash()) return;
		if (storing) store(current.getId(), slot, amount);
		else withdraw(current.getId(), slot, amount, true);
	}

	public void withdrawAll() {
		if (items.size() < 1) {
			owner.sendMessage("Your familiar isn't carrying any items.");
			return;
		}
		for (int i = 0; i < SIZE; i++) {
			Item item = items.get(i);
			if (item != null && item.getHash() >= 0) {
				withdraw(item.getId(), i, item.getAmount(), false);
			}
		}
		refresh(true);
		owner.getInventory().refresh();
	}
	
	public Container getContainer() {
		return items;
	}
	
	public int getContainerSize(int id) {
		int size = 0;
		switch (id) {
		case 6806: //thorny snail
			size = 3;
			break;
		case 6994: //spirit kalphite
			size = 6;
			break;
		case 6867: //bull ant
			size = 9;
			break;
		case 6794: //spirit terrorbird
			size = 12;
			break;
		case 6818: //abyssal parasite
			size = 7; //essence only
			break;
		case 6820: //abyssal lurker
			size = 7; //essence only
			break;
		case 6815: //war toirtoise
			size = 18;
			break;
		case 7349: //abyssal titan
			size = 7; //essence only
			break;
		case 6873: //pack yak
			size = 30;
			break;
		}
		return size;
	}

	public int numberOf(int id) {
		return items.getItemCount(id);
	}
	
	public void refresh(boolean reArrange) {
		if (reArrange) {
			Container cont = new Container(SIZE, false);
			cont.addAll(items);
			items = cont;
		}
		owner.getInventory().refresh();
		ActionSender.sendItems(owner, 90, owner.getInventory().getContainer(), false);
		ActionSender.sendItems(owner, 30, items, false);
	}
	
}
