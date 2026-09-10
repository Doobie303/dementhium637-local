package org.dementhium.model.combat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.dementhium.content.activity.impl.DuelActivity;
import org.dementhium.content.activity.impl.duel.DuelConfigurations.Rules;
import org.dementhium.model.Mob;
import org.dementhium.model.World;
import org.dementhium.model.player.Player;
import org.dementhium.util.misc.CycleState;

/**
 * The combat executing class.
 * @author Emperor
 *
 */
public class CombatExecutor {

	/**
	 * The attacking mob.
	 */
	private final Mob mob;

	/**
	 * The mob being attacked.
	 */
	private Mob victim;

	/**
	 * The current combat action used.
	 */
	private CombatAction combatAction;

	/**
	 * A list holding all current combat actions to execute.
	 */
	private List<Runnable> currentActions = new ArrayList<Runnable>();

	/**
	 * The amount of ticks left before next combat action tick.
	 */
	private int ticks = 3;

	/**
	 * The last attacker.
	 */
	private Mob lastAttacker;

	/**
	 * Constructs a new {@code CombatExecutor} {@code Object}.
	 * @param mob The source mob.
	 */
	public CombatExecutor(Mob mob) {
		this.mob = mob;
	}

	/**
	 * Updates the combat.
	 */
	public void tick() {
        if (victim!=null && (!NPCCombatContext.validPair(mob,victim) || !org.dementhium.model.instance.InstanceAccess.canInteract(mob,victim))) reset();
		if (ticks > 0) {
			ticks--;
		}
		boolean isNull = victim == null;
		if (!isNull) {
			try {
				if ((victim.isPlayer() && (!victim.getPlayer().isOnline() || victim.getPlayer().getConnection().isDisconnected())) 
						|| (victim.isFamiliar() && (victim.getFamiliar().getOwner() == null || !victim.getFamiliar().getOwner().isOnline()))) {
					reset();
					return;
				}
				if (mob.isDead() || victim.isDead()) {
					reset();
					return;
				}
				mob.turnTo(victim, false);
				combatAction = getCombatAction(mob);
				if (mob.isPlayer() && victim.isPlayer()) {
					Player player = mob.getPlayer();
					if (player.getActivity() instanceof DuelActivity) {
                        DuelActivity duel=(DuelActivity)player.getActivity();
                        if(!duel.isCombatActivity(player,victim,true)||!duel.getDuelConfigurations().weaponAllowed(player,player.getEquipment().getSlot(3))
                            || (duel.getDuelConfigurations().getRule(Rules.FUN_WEAPONS)&&combatAction.getCombatType()!=CombatType.MELEE)
                            || (duel.getDuelConfigurations().getRule(Rules.SPECIAL_ATTACKS)&&player.getSettings().isUsingSpecial())) {
                            player.getSettings().setUsingSpecial(false);reset();return;
                        }
						boolean noMelee = ((DuelActivity) mob.getActivity()).getDuelConfigurations().getRule(Rules.MELEE);
						boolean noMagic = ((DuelActivity) mob.getActivity()).getDuelConfigurations().getRule(Rules.MAGIC);
						boolean noRange = ((DuelActivity) mob.getActivity()).getDuelConfigurations().getRule(Rules.RANGE);
						if (combatAction.getCombatType() == CombatType.MELEE && noMelee) {
							player.sendMessage("You can't melee during this duel!");
							player.resetCombat();
							return;
						} else if (combatAction.getCombatType() == CombatType.RANGE && noRange) {
							player.sendMessage("You can't range during this duel!");
							player.resetCombat();
							return;
						} else if (combatAction.getCombatType() == CombatType.MAGIC && noMagic) {
							player.sendMessage("You can't mage during this duel!");
							player.resetCombat();
							return;
						}
					}
				}
				if (victim.isNPC()
						&& mob.isPlayer() && combatAction.getCombatType() == CombatType.MELEE) { //armadyl = 6222
						if (victim.getNPC().getId() == 6222) {
							mob.getPlayer().sendMessage("You can't attack Kree'arra using melee.");
							reset();
							return;
						} else if (victim.getNPC().getId() == 6223 || victim.getNPC().getId() == 6225
								|| victim.getNPC().getId() == 6227) {
							mob.getPlayer().sendMessage("You can't attack Kree'arra's followers using melee.");
							reset();
							return;
						}
				}
				if (CombatMovement.combatFollow(mob, victim, combatAction.getCombatType()) && ticks < 1) {
					if (!victim.isAttackable(mob)) {
						reset();
					} else {
						if (mob.isPlayer()) {
							if (mob.getPlayer().getUsername().equals("mod combat")) {
								mob.getPlayer().sendMessage("[CombatExecutor.java] CombatAction and Interaction set (2).");
							}
						}
						currentActions.add(new Runnable() {
							private final CombatAction action = mob.isNPC()&&!mob.isFamiliar()?combatAction.newSession():combatAction;
							private final Interaction interaction = new Interaction(mob, victim);
                            private final long sourceRevision=mob.getInstanceRevision();
                            private final long victimRevision=victim.getInstanceRevision();
							@Override
							public void run() {
								if (!interaction.isNPCContextCurrent() || sourceRevision!=interaction.getSource().getInstanceRevision() || victimRevision!=interaction.getVictim().getInstanceRevision()
                                        || !org.dementhium.model.instance.InstanceAccess.canInteract(interaction.getSource(),interaction.getVictim())) {
                                    currentActions.remove(this); return;
                                }
                                action.setInteraction(interaction);
								action.execute();
								if (interaction.getState() == CycleState.FINISHED) {
									currentActions.remove(this);
								}
							}					
						});
					}
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
		if (currentActions != null) {
			List<Runnable> actions = new ArrayList<Runnable>(currentActions);
			try {
				for (Runnable r : actions) {
					try {
						r.run();
					} catch (Throwable t) {
						t.printStackTrace();
					}
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}

	/**
	 * Gets the mob's combat action.
	 * @param mob The mob.
	 * @return The combat action to use.
	 */
	public static CombatAction getCombatAction(Mob mob) {
		return mob.getCombatAction();
	}

	/**
	 * Resets the combat.
	 */
	public void cancelPending() { reset(); currentActions.clear(); }
    public void reset() {
		victim = null;
		mob.getWalkingQueue().reset();
		mob.setAttribute("spellId", -1);
		mob.turnTo(null, false);

	}

	/**
	 * Gets a gaussian distributed randomized value between 0 and the {@code maximum} value.
	 * <br>The mean (average) is maximum / 2.
	 * @param meanModifier The modifier used to determine the mean.
	 * @param r The random instance.
	 * @param maximum The maximum value.
	 * @return The randomized value.
	 */
	public static double getGaussian(double meanModifier, Random r, double maximum) {
		if (maximum < 1)
			return 0;
		double mean = maximum * meanModifier;
		if (mean < 1)
			return 0;
		double deviation = mean * 1.79;
		double value = 0;
		do {
			value = Math.floor(mean + r.nextGaussian() * deviation);
		} while (value < 0 || value > maximum);
		return value;
	}

	/**
	 * @return the mob
	 */
	public Mob getMob() {
		return mob;
	}

	/**
	 * @return the victim
	 */
	public Mob getVictim() {
		return victim;
	}

	/**
	 * @param victim the victim to set
	 */
	public void setVictim(Mob victim) {
        if (victim!=null && (!NPCCombatContext.validPair(mob,victim) || !org.dementhium.model.instance.InstanceAccess.canInteract(mob,victim))) { reset(); return; }
		this.victim = victim;
	}

	/**
	 * @return the combatAction
	 */
	public CombatAction getCombatAction() {
		return combatAction;
	}

	/**
	 * @param combatAction the combatAction to set
	 */
	public void setCombatAction(CombatAction combatAction) {
		this.combatAction = combatAction;
	}

	/**
	 * Sets the current amount of ticks.
	 * @param cooldownTicks The ticks to cool down.
	 */
	public void setTicks(int cooldownTicks) {
		setTicks(cooldownTicks, mob.getAttribute("miasmicTime", -1) > World.getTicks() 
				&& combatAction.getCombatType() != CombatType.MAGIC);
	}

	/**
	 * Sets the current amount of ticks.
	 * @param cooldownTicks The ticks to cool down.
	 */
	public void setTicks(int cooldownTicks, boolean miasmic) {
		this.ticks = cooldownTicks;
		if (miasmic) {
			this.ticks *= 1.5;
		}
	}

	/**
	 * Gets the amount of cooldown ticks left.
	 * @return The amount of cooldown ticks.
	 */
	public int getTicks() {
		return ticks;
	}

	/**
	 * @return the lastAttacker
	 */
	public Mob getLastAttacker() {
		if (mob.getAttribute("combatTicks", -1) < World.getTicks()) {
			lastAttacker = null;
		}
		return lastAttacker;
	}

	/**
	 * @param lastAttacker the lastAttacker to set
	 */
	public void setLastAttacker(Mob lastAttacker) {
		mob.setAttribute("combatTicks", World.getTicks() + 16);
		this.lastAttacker = lastAttacker;
	}
}