package org.dementhium.model.npc.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import org.dementhium.content.skills.Prayer;
import org.dementhium.model.Location;
import org.dementhium.model.Mob;
import org.dementhium.model.Projectile;
import org.dementhium.model.World;
import org.dementhium.model.combat.*;
import org.dementhium.model.map.ObjectManager;
import org.dementhium.model.map.Region;
import org.dementhium.model.map.path.DefaultPathFinder;
import org.dementhium.model.mask.Animation;
import org.dementhium.model.mask.Graphic;
import org.dementhium.model.misc.DamageManager.DamageType;
import org.dementhium.model.misc.ProjectileManager;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.player.Player;
import org.dementhium.net.ActionSender;
import org.dementhium.tickable.Tick;
import org.dementhium.tickable.impl.CountdownTick;
import org.dementhium.tickable.impl.NexVirusTick;
import org.dementhium.util.Misc;
import org.dementhium.util.misc.Sounds;

/**
 * www.Neuro-X.org
 * www.RuneDream.org
 */


/**
 * Informative vid about Nex: http://www.youtube.com/watch?v=RtUJuPGedQ4
 */

public class Nex extends NPC {
	
	/**
	 * remove this later
	 */
	private final Projectile projectile = 
			Projectile.create(this, null, 2244, 43, 0, 56, 76, 3, size());
	/**
	 * remove this later
	 */
	public Projectile getProjectile() {
		return projectile;
	}

	private static final Random r = new Random();

	public static enum NexPhase {
		SPAWNED(null, null, -1, -1), // we can't attack at this stage
		SMOKE("Fill my soul with smoke!", "fumus", Sounds.NexFumus, Sounds.NexFillSmoke), // When we gain powers from Fumus
		SHADOW("Darken my shadow!", "umbra", Sounds.NexUmbra, Sounds.NexEmbraceDarkness), //When we gain powers from Umbra
		BLOOD("Flood my lungs with blood!", "cruor", Sounds.NexCrour, Sounds.NexFloodBlood),  //When we gain powers from Cruor
		ICE("Infuse me with the power of ice!", "glacies", Sounds.NexGlacies, Sounds.NexInfuseMeIce),    //When we gain powers from Glacies
		FINAL("NOW, THE POWER OF ZAROS!", null, -1, Sounds.NexPowerOfZaros);  //The last phase Nex has

		private String initialMessage, minionName;
		private int initialSoundId, soundId;

		private NexPhase(String initialMessage, String minionName, int initialSoundId, int soundId) {
			this.initialMessage = initialMessage;
			this.minionName = minionName;
			this.initialSoundId = initialSoundId;
			this.soundId = soundId;
		}
	}

	public static final int DEFAULT_NEX_ID = 13447, SOUL_SPLIT_NEX = 13448, MELEE_DEFLECT_NEX = 13449, WRATH_NEX = 13450;

	private static final int REAVER_ID = 13458;

	private static final Animation SPAWN_ANIMATION = Animation.create(6355);
	private static final Animation THROW_ANIMATION = Animation.create(6986);
	private static final Animation CAST_ANIMATION = Animation.create(6987);
	private static final Animation ATTACK_ANIMATION = Animation.create(6354);
	private static final Animation FLY_ANIMATION = Animation.create(6321);
	private static final Animation SIPHON_ANIMATION = Animation.create(6948);
	private static final Animation SMASH_ANIMATION = Animation.create(6984);
	private static final Animation TURMOIL_ANIMATION = Animation.create(6326);

	private static final Animation FALL_BACK_ANIMATION = Animation.create(10070);
	private static final Animation DRAG_ANIMATION = Animation.create(14388);

	private static final Graphic PURPLE_SMOKE_GRAPHIC = Graphic.create(1217);
	private static final Graphic FLYING_PURPLE_SMOKE = Graphic.create(1216);
	private static final Graphic CAST_GRAPHICS = Graphic.create(1214);
	private static final Graphic SMASH_SMOKE = Graphic.create(1215);
	private static final Graphic TURMOIL_GRAPHICS = Graphic.create(1204);

	private static final Graphic[] AFTERMATH_GRAPHICS =
		{
		null, Graphic.create(471), null, Graphic.create(376), Graphic.create(362), null
		};

	private static final Location[] NO_ESCAPE_TELEPORTS = 
		{
		Location.locate(2924, 5213, 0), //north, left side from area
		Location.locate(2934, 5202, 0), //east, upwards from area
		Location.locate(2924, 5192, 0), //south, right side from area
		Location.locate(2913, 5202, 0), //west
		};

	public static final int FUMUS = 13451, UMBRA = 13452, CRUOR = 13453, GLACIES = 13454;

	private NexPhase phase = NexPhase.SPAWNED;
	private NexCombatAction combatAction;
	private boolean changingPhase;
	private boolean noEscapeAttack;
	private long lastEscapeAttack;
	private long lastDragAttack;
	private long lastShadowAttack;
	private long lastPrayerSwitch;
	private long lastSpecialAttack;

	private boolean protectingMinion;
	private boolean protectingCruor;

	private boolean castedVirus;
	private boolean castedShadow;
	private boolean siphonMode;

	public Nex(int id) {
		super(id);
		combatAction = new NexCombatAction();
	}
	
	public Nex getNex(){
		return this;
	}

	@Override
	public boolean isNex() {
		return true;
	}

	@Override
	public CombatAction getCombatAction() {
		return combatAction;
	}

	public boolean noEscapeAttack() {
		return noEscapeAttack;
	}

	@Override
	public boolean isAttackable() {
		return phase != NexPhase.SPAWNED && !noEscapeAttack;
	}

	public boolean isProtectingMinion() {
		return protectingMinion;
	}

	public boolean isProtectingCruor() {
		return protectingCruor;
	}

	public boolean isSiphonMode() {
		return siphonMode;
	}

	public NexPhase getPhase() {
		return phase;
	}

	public void playSound(int sound) {
		for(Player player : World.getWorld().getPlayers()) {
			if(player.getLocation().distance(getLocation()) < 18) {
				ActionSender.sendSound(player, sound, 100, 255, true);
			}
		}
	}

	public static class NexAreaEvent extends Tick {

		private static final NexAreaEvent INSTANCE = new NexAreaEvent();
		public static final Location AREA_CENTER = Location.locate(2925, 5203, 0);

		public static NexAreaEvent getNexAreaEvent() {
			return INSTANCE;
		}

		private Nex nex;
		private int spawnDelay, minionSpawnDelay, minionSpawnStage;
		private boolean spawned;

		private NPC[] minions = new NPC[4];
		private Random random = new Random();

		private int delay;

		public NexAreaEvent() {
			super(2);
		}

		@Override
		public void execute() {
			if(nex == null) {
				if(delay > 0) {
					delay--;
					return;
				}
				boolean startSpawn = false;
				for(Player player : Region.getLocalPlayers(AREA_CENTER, 12)) {
					if(player.isOnline() && player.hasReceivedStarter()) {
						if(player.getLocation().distance(AREA_CENTER) < 16) {
							startSpawn = true;
							break;
						}
					}
				}
				if(startSpawn) {
					nex = new Nex(DEFAULT_NEX_ID);
					nex.setLocation(Location.locate(2924, 5202, 0)); //AREA_CENTER
					nex.setOriginalLocation(Location.locate(2924, 5202, 0)); //AREA_CENTER
					nex.setDoesWalk(false); // custom movements
					nex.loadEntityVariables();
					nex.setUnrespawnable(true);
					spawnDelay = 10; //20 ticks
				}
			} else {
				if(nex.isDead()) {
					nex = null;
					spawned = false;
					minionSpawnStage = 0;
					delay = 75;
					for(NPC minion : minions) {
						if(minion != null) {
							minion.sendDead();
						}
					}
					checkLife();
					return;
				}
				if(spawned) {
					checkLife();
					if(minionSpawnDelay > 0) {
						if(minionSpawnDelay % 4 == 0 && minionSpawnDelay != 20) {
							try {
								spawnMinion(NexPhase.values()[++minionSpawnStage]);
							} catch (Exception e) {
								System.out.println("Nex's minions probably didn't spawn.");
							}
						}
						minionSpawnDelay--;
					} else {
						for(NPC minion : minions) {
							if(minion != null) {
								minion.turnTo(nex, false);
							}
						}
						checkLife();
						if(nex.phase == NexPhase.SPAWNED) {
							changePhase(NexPhase.SMOKE);
						}
						checkLife();
						if (System.currentTimeMillis() - nex.lastSpecialAttack >= 4800) {
							boolean usedSpecial = false;
							switch(nex.phase) {
							case SMOKE:
								smokeAttack();
								usedSpecial = true;
								break;
							case SHADOW:
								shadowAttack();
								usedSpecial = true;
								break;
							case BLOOD:
								bloodAttack();
								usedSpecial = true;
								break;
							case ICE:
								iceAttack();
								usedSpecial = true;
								break;
							case FINAL:
								zarosAttack();
								usedSpecial = true;
								break;
							}
							if (usedSpecial) {
								nex.lastSpecialAttack = System.currentTimeMillis();
							}
						}
						checkLife();
						if(!nex.changingPhase && !nex.noEscapeAttack && nex.phase != NexPhase.SPAWNED) {
							int closestDistance = -1;
							Mob closeMob = null;
							for(Player player : World.getWorld().getPlayers()) {
								if(nex.getLocation().distance(player.getLocation()) > 16) {
									continue;
								}
								int distance = Misc.getDistance(nex.getLocation().getX(), nex.getLocation().getY(), player.getLocation().getX(), player.getLocation().getY());
								if((closestDistance == -1 || closestDistance > distance) && player.getLocation().getZ() == 0) {
									closestDistance = distance;
									closeMob = player;
								}
							}
							if (closeMob != null && (!closeMob.isPlayer() || (closeMob.isPlayer() && closeMob.getPlayer().hasReceivedStarter()))) {
								nex.getCombatExecutor().setVictim(closeMob);
							}
							if (closeMob != null && closeMob.isPlayer() && !closeMob.hasTick("nex_drag") && closeMob.getLocation().distance(AREA_CENTER) < 16 && closeMob.getPlayer().hasReceivedStarter()) {
								int distance = Misc.getDistance(nex.getLocation().getX(), nex.getLocation().getY(), closeMob.getLocation().getX(), closeMob.getLocation().getY());
								if (distance > 1 && Misc.random(30) == 1 && closeMob.getLocation().getZ() == 0) {
									//long currentTime = System.currentTimeMillis();
									//if(currentTime - nex.lastEscapeAttack >= 16000 && random.nextInt(12) == 0) {
									int xDiff = nex.getLocation().getX() - closeMob.getLocation().getX();
									int yDiff = nex.getLocation().getY() - closeMob.getLocation().getY();
									int x = closeMob.getLocation().getX();
									int y = closeMob.getLocation().getY();
									if (xDiff > 1 && yDiff > 1) { //Nex is NE of player
										x += 2;
										y += 2;
									} else if (xDiff < -1 && yDiff < -1) { //Nex is SW of player
										x -= 2;
										y -= 2;
									} else if (xDiff > 1 && yDiff < -1) { //Nex is SE of player
										x += 2;
										y -= 2;
									} else if (xDiff < -1 && yDiff > 1) { //Nex is NW of player
										x -= 2;
										y += 2;
									} else if (xDiff > 1)
										x += 2;
									else if (yDiff > 1)
										y += 2;
									else if (xDiff < -1)
										x -= 2;
									else if (yDiff < -1)
										y -= 2;	
									Location locToTele = Location.locate(x, y, 0);
									if (locToTele.getRegion().isClipped())
										locToTele = Location.locate(closeMob.getLocation().getX(), closeMob.getLocation().getY(), 0);
									nex.getWalkingQueue().reset();
									nex.teleport(locToTele, false);
									//nex.animate(??);
									//nex.lastEscapeAttack = currentTime;
								}
							}
						}

						if(nex.phase == NexPhase.SMOKE || nex.phase == NexPhase.FINAL) {
							if(!nex.changingPhase && !nex.noEscapeAttack) {
								if(random.nextInt(100) < (nex.phase == NexPhase.FINAL ? 5 : 10) && dragAttack()) {
									return;
								}
								noEscapeAttack();
							}
						}
					}
					return;
				}
				if(spawnDelay > 0) {
					spawnDelay--;
				} else {
					spawned = true;
					nex.animate(SPAWN_ANIMATION);
					nex.graphics(PURPLE_SMOKE_GRAPHIC);
					nex.forceText("AT LAST!");
					nex.playSound(Sounds.NexAtLast);
					World.getWorld().getNpcs().add(nex);
					minionSpawnDelay = 20;
					setTime(1);
				}
			}
		}

		private void zarosAttack() {
			if (nex == null)
				return;
			if(System.currentTimeMillis() - nex.lastPrayerSwitch > 10000 + Misc.random(10000)) {
				int switchId = DEFAULT_NEX_ID + Misc.random(2);
				while(switchId == nex.getId()) { // so we don't see the same phase
					switchId = DEFAULT_NEX_ID + Misc.random(2);
				}
				if(nex.lastPrayerSwitch == 0L) {
					switchId = SOUL_SPLIT_NEX;
				}
				nex.lastPrayerSwitch = System.currentTimeMillis();
				nex.getMask().setSwitchId(switchId);
			}
		}

		private void iceAttack() {
			if (nex == null)
				return;
			boolean attacking = nex.hasTick("ice_attack");
			if(!attacking) {
				nex.getCombatExecutor().setTicks(4);
				if(random.nextInt(100) <= 45) {
					nex.forceText("Contain this!");
					nex.playSound(Sounds.NexContainThis);
					nex.animate(SMASH_ANIMATION);
					nex.graphics(SMASH_SMOKE);
					final Location currentLocation = nex.getLocation().transform(1, 1, 0);
					nex.submitTick("ice_attack", new Tick(3) {
						private boolean done;
						public void execute() {
							if(!done) {
								done = true;
								if (nex == null) {
									stop();
									return;
								}
								setTime(25);
								for(int x = -1; x <= 1; x++) {
									for(int y = -1; y <= 1; y++) {
										if(x == y) {
											continue;
										}
										final Location loc = currentLocation.transform(x, y, 0);
										if(loc != currentLocation && !loc.hasObjects()) {
											if(loc.containsPlayers()) {
												for(Player attack : loc.getPlayers()) {
													if (attack.hasReceivedStarter() && attack.getLocation().distance(AREA_CENTER) < 16) {
														attack.getDamageManager().damage(nex, random.nextInt(350), 350, DamageType.RED_DAMAGE);
														attack.sendMessage("The icicle spike you to the spot!");
														if(attack.getPrayer().isAncientCurses()) {
															attack.getPrayer().closeOnPrayers(1, new int[] {Prayer.DEFLECT_MAGIC, Prayer.DEFLECT_MELEE, Prayer.DEFLECT_MISSILES, Prayer.DEFLECT_SUMMONING});
														} else {
															attack.getPrayer().closeOnPrayers(0, new int[] {Prayer.PROTECT_FROM_MAGIC, Prayer.PROTECT_FROM_MISSILES, Prayer.PROTECT_FROM_MELEE, Prayer.PROTECT_FROM_SUMMONING});
														}
														attack.getPrayer().recalculatePrayer();
														attack.getMask().setAppearanceUpdate(true);
														attack.stun(5, "You've been injured and can't use " + (attack.getPrayer().isAncientCurses() ? "deflect curses" :  "protection prayers ") + "!", false);
														attack.submitTick("nex_drag", new CountdownTick(attack, 15, null)); // only way to prevent prayers :s
													}
												}
											}
											ObjectManager.addCustomObject(57262, loc.getX(), loc.getY(), 0, 10, 0);
											World.getWorld().submit(new Tick(5) {
												public void execute() {
													stop();
													if (nex == null) {
														stop();
														return;
													}
													ObjectManager.clearArea(nex.getLocation(), 16);
												}
											});
										}
									}
								}
								return;
							}
							stop();
						}
					});
				} else {
					//You managed to destroy the icicle!
					nex.forceText("Die now, in a prison of ice!");
					nex.playSound(Sounds.NexDieNowInPrison);
					nex.submitTick("ice_attack", new Tick(2) {
						private boolean done;
						public void execute() {
							if(!done) {
								done = true;
								if (nex == null) {
									stop();
									return;
								}
								setTime(25);
								List<Player> locPlayers = Region.getLocalPlayers(nex.getLocation(), 14);
								if(locPlayers.size() > 0) {
									final Player player = locPlayers.get(random.nextInt(locPlayers.size()));
									if(player != null && !player.isDead() && player.getLocation().distance(nex.getLocation()) <= 10 && player.getLocation().distance(AREA_CENTER) < 16) {
										final Location currentLocation = player.getLocation();
										for(int x = -1; x <= 1; x++) {
											for(int y = -1; y <= 1; y++) {
												final Location loc = currentLocation.transform(x, y, 0);
												if(!loc.hasObjects() && player.getLocation().distance(nex.getLocation()) <= 10 && player.getLocation().distance(AREA_CENTER) < 16) {
													ObjectManager.addCustomObject(57262, loc.getX(), loc.getY(), 0, 10, 0);//was making ice things spawn in wrong place >.<
													player.submitTick("ice_prison", new Tick(4) {

														private boolean remove = true;

														public void execute() {
															if (nex == null) {
																stop();
																return;
															}
															if(remove) {
																ObjectManager.removeCustomObject(loc.getX(), loc.getY(), 0, 10);
																stop();
															}
															setTime(1);
															remove = true;
															if(player.getLocation() == currentLocation && player.hasReceivedStarter() && player.getLocation().distance(AREA_CENTER) < 16) {
																player.sendMessage("The centre of the ice prison freezes you to the bone!");
																player.getDamageManager().damage(nex, random.nextInt(600), 600, DamageType.RED_DAMAGE);
															}
														}
													});
												}
											}
										}
									}
								}
							}
							stop();
						}
					});
				}
			}
		}

		private void bloodAttack() {
			if (nex == null)
				return;
			if(!nex.hasTick("siphon")) {
				nex.forceText("A siphon will solve this!");
				nex.playSound(Sounds.NexSiphon);
				nex.siphonMode = true;
				nex.animate(SIPHON_ANIMATION);
				nex.setCanAnimate(false);
				final NPC bloodReaver = World.getWorld().register(Nex.REAVER_ID, nex.getLocation());
				bloodReaver.setUnrespawnable(true);
				nex.submitTick("siphon", new Tick(8) {
					private boolean done = false;
					public void execute() {
						if(done) {
							if (nex == null) {
								stop();
								return;
							}
							if(bloodReaver != null && !bloodReaver.isDead()) {
								nex.heal(bloodReaver.getHitPoints());
								bloodReaver.setHidden(true);
								bloodReaver.sendDead();
							}
							stop();
						} else {
							done = true;
							if (nex == null) {
								stop();
								return;
							}
							nex.siphonMode = false;
							nex.setCanAnimate(true);
							setTime(50);
						}
					}
				});
				return;
			} 
			if(!nex.siphonMode && !nex.hasTick("blood_sacrifice")) {
				nex.forceText("I demand a blood sacrifice!");
				nex.playSound(Sounds.NexBloodSacrifice);
				nex.submitTick("blood_sacrifice", new Tick(2) {
					private boolean done;
					public void execute() {
						if(!done) {
							done = true;
							if (nex == null) {
								stop();
								return;
							}
							for(final Player player : Region.getLocalPlayers(nex.getLocation(), 2)) {
								if(!player.isDead() && player.hasReceivedStarter() && player.getLocation().distance(nex.getLocation()) <= 10 && player.getLocation().distance(AREA_CENTER) < 16) {
									player.sendMessage("Nex has marked you as a sacrifice, RUN!");
									final Location currentLocation = player.getLocation();
									World.getWorld().submit(new Tick(2) {
										@Override
										public void execute() {
											if (nex == null) {
												stop();
												return;
											}
											stop();
											if(player.getLocation() == currentLocation) {
												player.sendMessage("You didn't make it far enough in time - Nex fires a punishing attack!");
												for(final Player pl : World.getWorld().getPlayers()) {
													if(pl.getLocation().distance(player.getLocation()) < 18 && pl.hasReceivedStarter() && pl.getLocation().distance(AREA_CENTER) < 16) {
														nex.animate(CAST_ANIMATION);
														ProjectileManager.sendDelayedProjectile(nex, pl, 374, false);
														World.getWorld().submit(new Tick(3) {
															@Override
															public void execute() {
																if (nex == null || pl.getLocation().distance(AREA_CENTER) >= 16) {
																	stop();
																	return;
																}
																stop();

																int damage = random.nextInt(300);
																pl.getDamageManager().damage(nex, damage, 300, DamageType.MAGE);
																pl.getSkills().drainPray(pl.getSkills().getLevel(5) / 2);

																nex.graphics(377);
																nex.heal(Math.round(damage * 0.15F));
															}
														});
													}
												}
											}
										}
									});
									break;
								}
							}
							setTime(10);
							return;
						}
						stop();
					}
				});
			}
		}

		private void smokeAttack() {
			if (nex == null)
				return;
			if(nex.castedVirus) {
				boolean noVirus = true;
				for(Player player : World.getWorld().getPlayers()) { // prefer this over region
					if(player.getLocation().distance(AREA_CENTER) < 16) {
						if(player.hasTick("nex_virus")) {
							noVirus = false;
							break;
						}
					}
				}
				if(noVirus) {
					nex.castedVirus = false;
				}
			}
		}

		private void shadowAttack() {
			if (nex == null)
				return;
			if(System.currentTimeMillis() - nex.lastShadowAttack >= 5400 && !nex.castedShadow) {
				final List<Player> localPlayers = new ArrayList<Player>();
				for(Player player : World.getWorld().getPlayers()) {
					if(player.getLocation().distance(nex.getLocation()) <= 10 && player.getLocation().distance(AREA_CENTER) < 16) {
						localPlayers.add(player);
					}
				}
				if(localPlayers.size() == 0) {
					return;
				}
				nex.castedShadow = true;
				nex.lastShadowAttack = System.currentTimeMillis();
				final Location[] locationArray = new Location[localPlayers.size()];
				final boolean distanceAttack = random.nextInt(100) < 75;
				if(distanceAttack) {
					nex.forceText("Embrace darkness!");
					nex.playSound(Sounds.NexEmbraceDarkness);
				} else {
					nex.forceText("Fear the shadow!");
					nex.playSound(Sounds.NexFearTheShadow);
				}
				int index = 0;
				for(Player player : localPlayers) {
					locationArray[index++] = player.getLocation();
					for(Player local : localPlayers) {
						ActionSender.sendObject(local, 57261, player.getLocation().getX(), player.getLocation().getY(), 0, 10, 0);
					}
				}
				localPlayers.clear();
				World.getWorld().submit(new Tick(3) {
					@Override
					public void execute() {
						if (nex == null) {
							stop();
							return;
						}
						nex.castedShadow = false;
						stop();
						for(Player player : World.getWorld().getPlayers()) {
							if(player.getLocation().distance(nex.getLocation()) <= 10 && player.getLocation().distance(AREA_CENTER) < 16) {
								localPlayers.add(player);
							}	
						}
						for(Player player : localPlayers) {
							for(Location loc : locationArray) {
								ActionSender.deleteObject(player, 57261, loc.getX(), loc.getY(), 0, 10, 0);
								ActionSender.sendPositionedGraphic(player, loc, 383);
								if(player.getLocation() == loc && player.hasReceivedStarter() && player.getLocation().distance(AREA_CENTER) < 16) { //TEST NIGGG
									int damageInflicted = 200 + Misc.random(distanceAttack ? player.getLocation().distance(nex.getLocation()) * 50 : 400);
									player.getDamageManager().damage(nex, damageInflicted, 1000, DamageType.MAGE);
								}
							}
						}
					}
				});
			}
		}

		public void spawnMinion(NexPhase phase) {
			NPC minion;
			switch(phase) {
			case SMOKE: 
				minion = new NPC(FUMUS, Location.locate(2912, 5216, 0));
				break;
			case SHADOW:
				minion = new NPC(UMBRA, Location.locate(2937, 5216, 0));
				break;
			case BLOOD:
				minion = new NPC(CRUOR, Location.locate(2937, 5190, 0));
				break;
			default:
				minion = new NPC(GLACIES, Location.locate(2912, 5190, 0));
			}
			nex.forceText(Misc.upperFirst(phase.minionName) + "!");
			nex.playSound(phase.initialSoundId);
			nex.animate(THROW_ANIMATION);
			nex.getMask().setFacePosition(minion.getLocation(), 1, 1);
			minion.turnTo(nex, false);
			minion.setAttribute("cantMove", Boolean.TRUE);
			minion.setAttribute("nex_vulnerable", Boolean.FALSE);
			minion.setUnrespawnable(true);
			World.getWorld().getNpcs().add(minion);
			ProjectileManager.sendGlobalProjectile(2244, minion, nex, 37, 60, 50);
			minions[phase.ordinal() - 1] = minion;
		}

		private void checkLife() {
			if (nex == null) {
				return;
			}
			if (nex.protectingMinion) {
				int index = nex.phase.ordinal() - 1;
				if (index >= 0 && index < minions.length && minions[index] != null) {
					if (minions[index].isDead() || minions[index].destroyed()) {
						int next = nex.phase.ordinal() + 1;
						if (next < NexPhase.values().length) {
							changePhase(NexPhase.values()[next]);
						}
						minions[index] = null;
					}
				}
				return;
			}
			if (nex.changingPhase) {
				return;
			}
			int hitpoints = nex.getHitPoints();
			int maxHitpoints = nex.getMaximumHitPoints();
			if (maxHitpoints <= 0) {
				return;
			}
			if (hitpoints <= (maxHitpoints * 0.2) && nex.phase == NexPhase.ICE) {
				nex.protectingMinion = true;
				if (minions[3] != null) {
					minions[3].setAttribute("nex_vulnerable", Boolean.TRUE);
				}
				nex.forceText("Glacies, don't fail me!");
				nex.playSound(Sounds.NexGlaciesDontFail);
			} else if (hitpoints <= (maxHitpoints * 0.4) && nex.phase == NexPhase.BLOOD) {
				nex.protectingCruor = true;
				nex.protectingMinion = true;
				if (minions[2] != null) {
					minions[2].setAttribute("nex_vulnerable", Boolean.TRUE);
				}
				nex.forceText("Cruor, don't fail me!");
				nex.playSound(Sounds.NexCrourDontFail);
			} else if (hitpoints <= (maxHitpoints * 0.6) && nex.phase == NexPhase.SHADOW) {
				nex.protectingMinion = true;
				if (minions[1] != null) {
					minions[1].setAttribute("nex_vulnerable", Boolean.TRUE);
				}
				nex.forceText("Umbra, don't fail me!");
				nex.playSound(Sounds.NexUmbraDontFail);
			} else if (hitpoints <= (maxHitpoints * 0.8) && nex.phase == NexPhase.SMOKE) {
				nex.protectingMinion = true;
				if (minions[0] != null) {
					minions[0].setAttribute("nex_vulnerable", Boolean.TRUE);
				}
				nex.forceText("Fumus, don't fail me!");
				nex.playSound(Sounds.NexFumusDontFail);
			}
		}

		public boolean isSpawned() {
			return nex != null;
		}

		public boolean ableToAttack() {
			return nex.phase != NexPhase.SPAWNED;
		}

		public void changePhase(final NexPhase phase) {
			if (nex == null)
				return;
			int ticks = 5;
			if(nex.phase == NexPhase.SPAWNED) {
				ticks = 2;
			}
			World.getWorld().submit(new Tick(ticks) {
				@Override
				public void execute() {
					if (nex == null) {
						stop();
						return;
					}
					stop();
					if(nex.phase != phase && !nex.changingPhase) {
						nex.changingPhase = true;
						nex.forceText(phase.initialMessage);
						nex.playSound(phase.soundId);
						if(phase != NexPhase.FINAL) {
							ProjectileManager.sendGlobalProjectile(2244, minions[phase.ordinal() - 1], nex, 46, 60, 50);
						} else {
							nex.heal(6000);
							nex.animate(TURMOIL_ANIMATION);
							nex.graphics(TURMOIL_GRAPHICS);
						}
						ObjectManager.clearArea(nex.getLocation(), 16);
						World.getWorld().submit(new Tick(3) {
							@Override
							public void execute() {
								if (nex == null) {
									stop();
									return;
								}
								stop();
								nex.changingPhase = false;
								nex.phase = phase;
								nex.protectingMinion = false;
							}
						});
					}
				}
			});
		}

		private boolean dragAttack() {
			if (nex == null)
				return false;
			if(System.currentTimeMillis() - nex.lastDragAttack > 12000 && !nex.isAnimating() && nex.getCombatExecutor().getTicks() < 2) {
				nex.lastDragAttack = System.currentTimeMillis();
				List<Player> locPlayers = Region.getLocalPlayers(nex.getLocation(), 14);
				if(locPlayers.size() > 0) {
					int attempts = locPlayers.size();
					while(--attempts != -1) {
						if(drag(locPlayers.get(random.nextInt(locPlayers.size())))) {
							return true;
						}
					}
				}
			}
			return false;
		}

		private void noEscapeAttack() {
			if (nex == null)
				return;
			long currentTime = System.currentTimeMillis();
			if(currentTime - nex.lastEscapeAttack >= 16000 && random.nextInt(12) == 0) {
				nex.lastEscapeAttack = currentTime;
				nex.noEscapeAttack = true;
				nex.getCombatExecutor().setVictim(null);
				nex.getWalkingQueue().reset();
				nex.forceText("There is...");
				nex.playSound(Sounds.NexThereIs);
				nex.setLocation(AREA_CENTER);
				World.getWorld().submit(new Tick(2) {
					@Override
					public void execute() {
						if (nex == null) {
							stop();
							return;
						}
						stop();
						nex.animate(FLY_ANIMATION);
						nex.graphics(FLYING_PURPLE_SMOKE);

						World.getWorld().submit(new Tick(2) {
							@Override
							public void execute() {
								if (nex == null) {
									stop();
									return;
								}
								stop();
								final int index = random.nextInt(NO_ESCAPE_TELEPORTS.length);
								final Location noEscapePosition = NO_ESCAPE_TELEPORTS[index];

								nex.teleport(noEscapePosition, false);
								//squid, below was commented out?
								nex.forceMovement(null, noEscapePosition.getX(), noEscapePosition.getY(), 1, 2, -1, 2, true, false);
								nex.forceText("NO ESCAPE!");
								nex.playSound(Sounds.NexNoEscape);
								nex.getMask().setFacePosition(AREA_CENTER, 1, 1);
								World.getWorld().submit(new Tick(2) {

									private List<Player> playersToHit;

									private int countdown = 3;

									@Override
									public void execute() {
										if (nex == null) {
											stop();
											return;
										}
										countdown--;
										if(countdown == 2) {
											nex.forceMovement(null, 2924, 5203, 0, 60, -1, 2, true, true);
											for(Player attack : playersToHit = attackablePlayers(index)) {
												attack.getMask().setFacePosition(noEscapePosition, 1, 1);
												attack.setAttribute("cantMove", Boolean.TRUE);
												//doCamera(attack, index);
											}
										} else if(countdown == 1) {
											for(Player attack : playersToHit = attackablePlayers(index)) {
												if (attack.getLocation().distance(AREA_CENTER) < 16) {
													int movementX = attack.getLocation().getX();
													int movementY = attack.getLocation().getY();

													switch(index) {
													case 0:
														movementY -= 2;
														break;
													case 1:
														movementX -= 2;
														break;
													case 2:
														movementY += 2;
														break;
													case 3:
														movementX += 2;
														break;
													}
													int dir = 0;
													if (attack.getLocation().getX() > noEscapePosition.getX())
														dir = 3;
													if (attack.getLocation().getX() < noEscapePosition.getX())
														dir = 1;
													if (attack.getLocation().getY() > noEscapePosition.getY())
														dir = 2;
													if (attack.getLocation().getY() < noEscapePosition.getY())
														dir = 0;

													attack.forceMovement(FALL_BACK_ANIMATION, movementX, movementY, 30, 60, dir, 1, true);
													int maxDamage = nex.phase == NexPhase.FINAL ? 550 : 400;
													int damage = r.nextInt(maxDamage);
													attack.removeAttribute("cantMove");
													attack.getDamageManager().damage(nex, damage, maxDamage, DamageType.RED_DAMAGE);
												}
											}
										} else if(countdown == 0) {
											nex.noEscapeAttack = false;
											stop();
										}
									}

									private void doCamera(final Player attack, int dir) {
										int movementX = AREA_CENTER.getX();
										int movementY = AREA_CENTER.getY();
										int rotateX = AREA_CENTER.getX();
										int rotateY = AREA_CENTER.getY();
										switch (dir) {
										case 3:
											rotateY -= 8;
											movementY += 9;
											break;
										case 2:
											rotateY -= 8;
											movementY += 9;
											break;
										case 1:
											rotateX += 8;
											movementX -= 9;
											break;
										case 0:
											rotateY += 8;
											movementY -= 9;
											break;
										}
										//World.getWorld().getGroundItemManager().sendGlobalGroundItem(World.getWorld().getGroundItemManager().create(attack, new Item(391, 1), Location.locate(rotateX, rotateY, 0)), false);
										movementX = (movementX - (attack.getLocation().getRegionX() - 6) * 8);
										movementY = (movementY - (attack.getLocation().getRegionY() - 6) * 8);
										rotateX = (rotateX - (attack.getLocation().getRegionX() - 6) * 8);
										rotateY = (rotateY - (attack.getLocation().getRegionY() - 6) * 8);
										ActionSender.moveCamera(attack, 100, movementX, movementY, 3, 2);
										ActionSender.rotateCamera(attack, rotateX, rotateY, 100, 50);
										World.getWorld().submit(new Tick(3) {
											int counter = 0;
											@Override
											public void execute() {
												if (counter == 0) {
													Location loc = Location.locate(attack.getLocation().getX(), attack.getLocation().getY() - 4, 0);
													ActionSender.moveCamera(attack, 5, loc.getLocalX(), loc.getLocalX(), 3, 5);
													ActionSender.rotateCamera(attack, attack.getLocation().getLocalX(), attack.getLocation().getLocalY(), 7, 50);
													counter++;
												} else if (counter == 1) {//lemme do this part1sec
													ActionSender.resetCamera(attack);
													stop();
												}
											}

										});
									}
								});
							}
						});
					}
				});
			}
		}

		private List<Player> attackablePlayers(int direction) {
			if(direction < 0 || direction > 3) {
				return null;
			}
			List<Player> players = new ArrayList<Player>();
			int startX = -1, endX = -1;
			int startY = -1, endY = -1;
			switch(direction) {
			case 0:
				startX = 2924;
				endX = 2926;
				startY = 5202;
				endY = 5211;
				break;
			case 1:
				startX = 2924;
				endX = 2933;
				startY = 5202;
				endY = 5204;
				break;
			case 2:
				startX = 2924;
				endX = 2926;
				startY = 5195;
				endY = 5204;
				break;
			case 3:
				startX = 2916;
				endX = 2923;
				startY = 5202;
				endY = 5204;
				break;
			}
			for(int x = startX; x <= endX; x++) {
				for(int y = startY; y <= endY; y++) {
					Location loc = Location.locate(x, y, 0);
					if(loc.containsPlayers()) {
						players.addAll(loc.getPlayers());
					}
				}
			}
			List<Player> players2 = new CopyOnWriteArrayList<Player>(players); //avoids ConcurrentModificationException
			for (Player player : players2) {
				if (player != null) {
					if (!player.isOnline() || !(player.getLocation().distance(AREA_CENTER) < 16)) {
						players.remove(player);
					}
				}
			}
			players = players2;

			return players;
		}

		public boolean drag(final Player victim) {
			if (nex == null)
				return false;
			if(victim.hasTick("nex_drag") || victim.getHitPoints() < 100 || victim.getAttribute("superhit") != null || !victim.hasReceivedStarter() || !(victim.getLocation().distance(AREA_CENTER) < 16)) {
				return false;
			}
			nex.getCombatExecutor().setVictim(victim);

			victim.sendMessage("Nex draws you in...");
			victim.forceMovement(DRAG_ANIMATION, nex.getLocation().getX(), nex.getLocation().getY(), 0, 80, -1, 2, true);
			victim.submitTick("nex_drag", new Tick(3) {
				private int cycles = 0;
				@Override
				public void execute() {
					if(cycles == 15) {
						stop();
					}
					if(cycles == 0) {
						if (victim.getLocation().distance(AREA_CENTER) < 16) {
							if(victim.getPrayer().isAncientCurses()) {
								victim.getPrayer().closeOnPrayers(1, new int[] {Prayer.DEFLECT_MAGIC, Prayer.DEFLECT_MELEE, Prayer.DEFLECT_MISSILES, Prayer.DEFLECT_SUMMONING});
							} else {
								victim.getPrayer().closeOnPrayers(0, new int[] {Prayer.PROTECT_FROM_MAGIC, Prayer.PROTECT_FROM_MISSILES, Prayer.PROTECT_FROM_MELEE, Prayer.PROTECT_FROM_SUMMONING});
							}
							victim.getPrayer().recalculatePrayer();
							victim.getMask().setAppearanceUpdate(true);
							victim.stun(5, "You've been injured and can't use " + (victim.getPrayer().isAncientCurses() ? "deflect curses" :  "protection prayers ") + "!", false);
						}
					}
					cycles++;
				}
			});
			return true;
		}

		public Nex getNex() {
			return nex;
		}

	}

	private final class NexCombatAction extends CombatAction {

		private final Location AREA_CENTER = NexAreaEvent.AREA_CENTER;
		
		public NexCombatAction() {
			super(true);
		}

		@Override
		public boolean executeSession() {
			if(noEscapeAttack || changingPhase || siphonMode) {
				return false;
			}

			if(phase == NexPhase.SMOKE) {
				if(!castedVirus || r.nextInt(100) < 8) {
					castedVirus = true;
					castVirus(interaction.getVictim());
					return false;
				}
			}

			final boolean close = interaction.getSource().getLocation().withinDistance(interaction.getVictim().getLocation(), size());
			boolean usingMagic = !close;

			@SuppressWarnings("unused")
			int cycles = 1;
			int damage = 0;
			int maxDamage = 0;

			if(!usingMagic && (r.nextInt(phase == NexPhase.FINAL ? 10 : 3) == 0 || phase == NexPhase.SHADOW)) {
				usingMagic = true;
			}
			if(getCombatExecutor().getTicks() != 0) {
				usingMagic = false;
			}

			if(usingMagic) {//
			final Mob NX = interaction.getSource();
				cycles = 3;
				getCombatExecutor().setTicks(4);
				animate(CAST_ANIMATION);
				turnTo(interaction.getVictim(), false);
				int projectileId = -1;
				switch(phase) {
				case FINAL:
				case SMOKE:
					projectileId = 306;
					graphics(CAST_GRAPHICS);
					damage = MagicFormulae.getDamage(Nex.this, interaction.getVictim(), maxDamage = (phase == NexPhase.FINAL ? 350 : 251));
					break;
				case SHADOW:
					projectileId = 380;
					damage = MagicFormulae.getDamage(Nex.this, interaction.getVictim(), maxDamage = 301);
					break;
				case BLOOD:
					projectileId = 374;
					damage = MagicFormulae.getDamage(Nex.this, interaction.getVictim(), maxDamage = 301);
					break;
				case ICE:
					projectileId = 362;
					damage = MagicFormulae.getDamage(Nex.this, interaction.getVictim(), maxDamage = 301);
					break;
				}
				if(projectileId != -1 && !close) {
					ProjectileManager.sendDelayedProjectile(Nex.this, interaction.getVictim(), projectileId, false);
				}
				final int fMaxDamage = maxDamage, fProjectileId = projectileId;
				World.getWorld().submit(new Tick(3) {
					private int attacked;
					@Override
					public void execute() {
						stop();
						for(Player other : Region.getLocalPlayers(getLocation(), 13)) {
							if(attacked > 20) {
								break;
							}
							if (!(other.getLocation().distance(AREA_CENTER) < 16))
								break;
							//if(other == interaction.getVictim() && close) {
								//continue;
							//}
							int castedDamage = MagicFormulae.getDamage(Nex.this, other, fMaxDamage);
							if (phase == NexPhase.SHADOW) {
								if (other.getPrayer().usingPrayer(1, 8) || other.getPrayer().usingPrayer(0, 18))
									castedDamage *= 0.6;
							} else {
								if (other.getPrayer().usingPrayer(1, 7) || other.getPrayer().usingPrayer(0, 17))
									castedDamage *= 0.6;
							}
							ProjectileManager.sendDelayedProjectile(Nex.this, other, fProjectileId, false);
							other.getDamageManager().damage(Nex.this, castedDamage, fMaxDamage, phase == NexPhase.SHADOW ? DamageType.RANGE : DamageType.MAGE);
							other.retaliate(Nex.this);
							attacked++;
							switch(phase) {
							case SMOKE:
								boolean poison = r.nextInt(100) <= 25;
								if(poison) {
									other.getPoisonManager().poison(Nex.this, 60 + r.nextInt(50));
									other.graphics(AFTERMATH_GRAPHICS[phase.ordinal()]);
								}
								break;
							case BLOOD:
								heal(Math.round(castedDamage * 0.10F));
								other.graphics(AFTERMATH_GRAPHICS[phase.ordinal()]);
								break;
							case ICE:
								if(other.getAttribute("freezeImmunity", -1) < World.getTicks() && castedDamage > 0) {
									/*other.getCombatExecutor().setFrozenTime(5000);*/
									other.getWalkingQueue().reset();
									other.submitTick("freeze_immunity", new CountdownTick(other, 10, null));
									other.graphics(AFTERMATH_GRAPHICS[phase.ordinal()]);
								}
								break;
							}
						}
					}
				});
			} else {//
				if(close) {
					if(getCombatExecutor().getTicks() > 2) {
						return false;
					}
					turnTo(interaction.getVictim(), false);
					animate(ATTACK_ANIMATION);
					getCombatExecutor().setTicks(4);
					maxDamage = (phase == NexPhase.FINAL ? 550 : 369);
					damage = MeleeFormulae.getDamage(Nex.this, interaction.getVictim(), maxDamage);
					if (interaction.getVictim().isPlayer() 
							&& (interaction.getVictim().getPlayer().getPrayer().usingPrayer(1, 9) 
									|| interaction.getVictim().getPlayer().getPrayer().usingPrayer(0, 19)))
						damage *= 0.6;
				} else {
					return false;
				}
			}//
			switch(phase) {
			case SMOKE:
				boolean poison = r.nextInt(100) <= 25;
				if(poison) {
					interaction.getVictim().getPoisonManager().poison(Nex.this, 60 + r.nextInt(50));
				}
				break;
			case BLOOD:
				heal(Math.round(damage * 0.15F));
				break;
			case ICE:
				if(!interaction.getVictim().hasTick("freeze_immunity") && interaction.getVictim().getAttribute("freezeImmunity", -1) < World.getTicks() && damage > 0) {
					interaction.getVictim().getWalkingQueue().reset();
					interaction.getVictim().submitTick("freeze_immunity", new CountdownTick(interaction.getVictim().getPlayer(), 20, null));
				}
				break;
			}

			//CombatType type = usingMagic ? phase == NexPhase.SHADOW ? CombatType.RANGE : CombatType.MAGIC : CombatType.MELEE;
			interaction.setDamage(new Damage(damage));
			return false;
		}

		public void castVirus(Mob victim) {
			animate(CAST_ANIMATION);
			getCombatExecutor().setTicks(4);
			forceText("Let the virus flow through you!");
			playSound(Sounds.NexVirus);
			if(interaction.getVictim().hasTick("nex_virus")) {
				interaction.getVictim().removeTick("nex_virus");
			}
			if (interaction.getVictim().getLocation().distance(AREA_CENTER) < 16){
				
				Player player = interaction.getVictim().getPlayer();
				if(player != null){
				interaction.getVictim().submitTick("nex_virus", new NexVirusTick(player));
				}
			}
		}

		/*		@Override
		public boolean canAttack(Mob mob, Mob victim) {
			if(noEscapeAttack || changingPhase || siphonMode) {
				return false;
			}
			if(mob.getLocation().distance(interaction.getVictim().getLocation()) > interaction.getVictim().size() && r.nextBoolean() && r.nextBoolean()) {
				Following.combatFollow(mob, victim);
				return false;
			}
			return true;
		}*/

		@Override
		public boolean commenceSession() {
			if(noEscapeAttack || changingPhase || siphonMode) {
				return false;
			}

			if(phase == NexPhase.SMOKE) {
				if(!castedVirus || r.nextInt(100) < 8) {
					castedVirus = true;
					castVirus(interaction.getVictim());
					return false;
				}
			}

			final boolean close = interaction.getSource().getLocation().withinDistance(interaction.getVictim().getLocation(), size());
			boolean usingMagic = !close;

			@SuppressWarnings("unused")
			int cycles = 1;
			int damage = 0;
			int maxDamage = 0;

			if(!usingMagic && (r.nextInt(phase == NexPhase.FINAL ? 10 : 3) == 0 || phase == NexPhase.SHADOW)) {
				usingMagic = true;
			}
			if(getCombatExecutor().getTicks() != 0) {
				usingMagic = false;
			}

			if(usingMagic) {//
				cycles = 3;
				getCombatExecutor().setTicks(4);
				animate(CAST_ANIMATION);
				turnTo(interaction.getVictim(), false);
				int projectileId = -1;
				switch(phase) {
				case FINAL:
				case SMOKE:
					projectileId = 306;
					graphics(CAST_GRAPHICS);
					damage = MagicFormulae.getDamage(Nex.this, interaction.getVictim(), maxDamage = (phase == NexPhase.FINAL ? 350 : 251));
					break;
				case SHADOW:
					projectileId = 380;
					damage = MagicFormulae.getDamage(Nex.this, interaction.getVictim(), maxDamage = 301);
					break;
				case BLOOD:
					projectileId = 374;
					damage = MagicFormulae.getDamage(Nex.this, interaction.getVictim(), maxDamage = 301);
					break;
				case ICE:
					projectileId = 362;
					damage = MagicFormulae.getDamage(Nex.this, interaction.getVictim(), maxDamage = 301);
					break;
				}
				if(projectileId != -1 && !close) {
					ProjectileManager.sendDelayedProjectile(Nex.this, interaction.getVictim(), projectileId, false);
				}
				final int fMaxDamage = maxDamage, fProjectileId = projectileId;
				World.getWorld().submit(new Tick(3) {
					private int attacked;
					@Override
					public void execute() {
						stop();
						for(Player other : Region.getLocalPlayers(getLocation(), 13)) {
							if(attacked > 20) {
								break;
							}
							if (!(other.getLocation().distance(AREA_CENTER) < 16))
								break;
							//if(other == interaction.getVictim() && close) {
								//continue;
							//}
							int castedDamage = MagicFormulae.getDamage(Nex.this, other, fMaxDamage);
							if (phase == NexPhase.SHADOW) {
								if (other.getPrayer().usingPrayer(1, 8) || other.getPrayer().usingPrayer(0, 18))
									castedDamage *= 0.6;
							} else {
								if (other.getPrayer().usingPrayer(1, 7) || other.getPrayer().usingPrayer(0, 17))
									castedDamage *= 0.6;
							}
							ProjectileManager.sendDelayedProjectile(Nex.this, other, fProjectileId, false);
							other.getDamageManager().damage(Nex.this, castedDamage, fMaxDamage, phase == NexPhase.SHADOW ? DamageType.RANGE : DamageType.MAGE);
							other.retaliate(Nex.this);
							attacked++;
							switch(phase) {
							case SMOKE:
								boolean poison = r.nextInt(100) <= 25;
								if(poison) {
									other.getPoisonManager().poison(Nex.this, 60 + r.nextInt(50));
									other.graphics(AFTERMATH_GRAPHICS[phase.ordinal()]);
								}
								break;
							case BLOOD:
								heal(Math.round(castedDamage * 0.10F));
								other.graphics(AFTERMATH_GRAPHICS[phase.ordinal()]);
								break;
							case ICE:
								if(other.getAttribute("freezeImmunity", -1) < World.getTicks() && castedDamage > 0) {
									/*other.getCombatExecutor().setFrozenTime(5000);*/
									other.getWalkingQueue().reset();
									other.submitTick("freeze_immunity", new CountdownTick(other, 10, null));
									other.graphics(AFTERMATH_GRAPHICS[phase.ordinal()]);
								}
								break;
							}
						}
					}
				});
			} else {//
				if(close) {
					if(getCombatExecutor().getTicks() > 2) {
						return false;
					}
					turnTo(interaction.getVictim(), false);
					animate(ATTACK_ANIMATION);
					getCombatExecutor().setTicks(4);
					int maxDamage2 = phase == NexPhase.FINAL ? 550 : 369;
					damage = MeleeFormulae.getDamage(Nex.this, interaction.getVictim(), maxDamage2);
					if (interaction.getVictim().isPlayer() 
							&& (interaction.getVictim().getPlayer().getPrayer().usingPrayer(1, 9) 
									|| interaction.getVictim().getPlayer().getPrayer().usingPrayer(0, 19)))
						damage *= 0.6;
					interaction.getVictim().getDamageManager().damage(Nex.this, damage, maxDamage2, DamageType.MELEE);
					interaction.getVictim().retaliate(Nex.this);
				} else {
					return false;
				}
			}//
			switch(phase) {
			case SMOKE:
				boolean poison = r.nextInt(100) <= 25;
				if(poison) {
					interaction.getVictim().getPoisonManager().poison(Nex.this, 60 + r.nextInt(50));
				}
				break;
			case BLOOD:
				heal(Math.round(damage * 0.15F));
				break;
			case ICE:
				if(!interaction.getVictim().hasTick("freeze_immunity") && interaction.getVictim().getAttribute("freezeImmunity", -1) < World.getTicks() && damage > 0) {
					interaction.getVictim().getWalkingQueue().reset();
					interaction.getVictim().submitTick("freeze_immunity", new CountdownTick(interaction.getVictim().getPlayer(), 20, null));
				}
				break;
			}

			//CombatType type = usingMagic ? phase == NexPhase.SHADOW ? CombatType.RANGE : CombatType.MAGIC : CombatType.MELEE;
			interaction.setDamage(new Damage(damage));
			return false;
		}

		@Override
		public boolean endSession() {
			return true;
		}

		@Override
		public CombatType getCombatType() {
			if (interaction != null && interaction.getVictim() != null && interaction.getSource() != null) {
				boolean close = interaction.getSource().getLocation().withinDistance(interaction.getVictim().getLocation(), size());
				if (!close && !World.getWorld().doPath
						(new DefaultPathFinder(), Nex.this, 
								interaction.getVictim().getLocation().getX(), interaction.getVictim().getLocation().getY(), false, false)
								.isRouteFound()) {
					if (phase == NexPhase.SHADOW)
						return CombatType.RANGE;
					return CombatType.MAGIC;
				}
				else if (!close && World.getWorld().doPath
						(new DefaultPathFinder(), Nex.this, 
								interaction.getVictim().getLocation().getX(), interaction.getVictim().getLocation().getY(), false, false)
								.isRouteFound()) {
					int random = Misc.random(2);
					if (random <= 1)
						return CombatType.MELEE;
					else {
						if (phase == NexPhase.SHADOW)
							return CombatType.RANGE;
						return CombatType.MAGIC;
					}
				}
				else if (close && World.getWorld().doPath
						(new DefaultPathFinder(), Nex.this, 
								interaction.getVictim().getLocation().getX(), interaction.getVictim().getLocation().getY(), false, false)
								.isRouteFound()) {
					return CombatType.MELEE;
				}
			}
			if (phase == NexPhase.SHADOW)
				return CombatType.RANGE;
			return CombatType.MAGIC;
		}

	}

}