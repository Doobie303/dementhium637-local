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
import org.dementhium.model.map.GameObject;
import org.dementhium.model.map.ObjectManager;
import org.dementhium.model.map.Region;
import org.dementhium.model.mask.Animation;
import org.dementhium.model.mask.Graphic;
import org.dementhium.model.misc.DamageManager.DamageType;
import org.dementhium.model.misc.ProjectileManager;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.player.Player;
import org.dementhium.model.player.Skills;
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
	private static final Location NO_ESCAPE_CENTER = Location.locate(2924, 5202, 0);

	public static final int FUMUS = 13451, UMBRA = 13452, CRUOR = 13453, GLACIES = 13454;

	private NexPhase phase = NexPhase.SPAWNED;
	private NexCombatAction combatAction;
	private boolean changingPhase;
	private boolean noEscapeAttack;
	private long lastEscapeAttack;
	private long lastDragAttack;

	private boolean protectingMinion;
	private boolean protectingCruor;

	private boolean castedVirus;
	private boolean castedShadow;
	private boolean siphonMode;
	private int zarosAttackCount;
	private int autoAttacksSinceSpecial;
	private int specialStep;
	private boolean specialPending;
	private int specialRecoveryUntil;
	private boolean lastBloodSpecialWasSiphon = true;

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

    private Mob movementTarget;
    private NexPhase movementPhase;
    private CombatType movementStyle;
    private java.util.Deque<Location> pursuitRoute;
    private Location pursuitTarget, pursuitOrigin;
    public boolean containsArena(Location tile) {
        return tile!=null&&tile.getZ()==NexAreaEvent.AREA_CENTER.getZ()
            &&tile.getX()>=NexAreaEvent.ROOM_MIN_X&&tile.getX()<=NexAreaEvent.ROOM_MAX_X
            &&tile.getY()>=NexAreaEvent.ROOM_MIN_Y&&tile.getY()<=NexAreaEvent.ROOM_MAX_Y;
    }
    public static boolean arenaPair(Mob first,Mob second) {
        Nex nex=first instanceof Nex?(Nex)first:second instanceof Nex?(Nex)second:null;
        Mob player=nex==first?second:first;
        return nex!=null&&player!=null&&player.isPlayer()&&!player.getPlayer().isInvisible()
            &&nex.containsArena(nex.getLocation())&&nex.containsArena(player.getLocation());
    }
    public boolean canPursue(Mob target) {
        return target!=null&&target.isPlayer()&&NPCCombatContext.validPair(this,target)
            &&containsArena(target.getLocation())&&containsArena(getLocation())
            &&phase!=NexPhase.SPAWNED&&!noEscapeAttack&&!changingPhase&&!siphonMode&&!specialPending&&!hasTick("ice_attack");
    }
    @Override public void tick() {
        // The arena event owns targeting, empty-room reset and death/respawn.
        if(isDead()||isDying())return;
        getCombatStats().tick();
        if(!containsArena(getLocation())&&NexAreaEvent.getNexAreaEvent().getNex()==this)
            NexAreaEvent.getNexAreaEvent().resetEncounter(false);
    }
    @Override public void resetCombatState() {
        super.resetCombatState();movementTarget=null;movementPhase=null;movementStyle=null;
        pursuitRoute=null;pursuitTarget=null;pursuitOrigin=null;
        changingPhase=false;noEscapeAttack=false;siphonMode=false;specialPending=false;
        castedShadow=false;castedVirus=false;autoAttacksSinceSpecial=0;
        specialRecoveryUntil=0;
        for(String special:new String[]{"siphon","blood_sacrifice","shadow_darkness","ice_attack"})removeTick(special);
        setCanAnimate(true);
        NexAreaEvent event=NexAreaEvent.getNexAreaEvent();
        if(event.getNex()==this){event.clearForcedPlayers();event.clearShadows();event.clearIcePrison(false);event.clearContainment();event.clearBloodReavers(false);}
    }

    @Override public void hit(int damage) {
        super.hit(damage);
        // Player hits run after the arena event. Close a reached gate before
        // another hit in that same cycle can pass through the unshielded phase.
        NexAreaEvent event = NexAreaEvent.getNexAreaEvent();
        if (event.getNex() == this && !isDead() && !isDying()) event.checkLife();
    }

    private void recoverFromSpecial(int ticks) {
        // Arena callbacks run before the executor, which consumes a cooldown
        // tick immediately. The absolute deadline preserves the full interval.
        specialRecoveryUntil = World.getTicks() + ticks;
        getCombatExecutor().setTicks(ticks);
    }

    private void sendPhaseProjectile(final Player target, final int projectileId) {
        final long life = getCombatGeneration();
        final NexPhase attackPhase = phase;
        final NPCCombatContext context = new NPCCombatContext(this, target);
        World.getWorld().submit(new Tick(1) {
            @Override public void execute() {
                stop();
                if (isPhaseCurrent(life, attackPhase) && context.isCurrent())
                    ProjectileManager.sendGlobalProjectile(projectileId, Nex.this, target, 46, 35, 49);
            }
        });
    }
    private CombatType movementType(Mob target) {
        if(target==null)return phase==NexPhase.SHADOW?CombatType.RANGE:CombatType.MAGIC;
        if(phase==NexPhase.SHADOW)return CombatType.RANGE;
        if(CombatMovement.hasMeleeContact(this,target,true))return CombatType.MELEE;
        if(movementTarget!=target||movementPhase!=phase||movementStyle==null){
            movementTarget=target;movementPhase=phase;
            // One selection per approach/attack, never a path-dependent reroll each tick.
            movementStyle=pursuitRoute(target)&&getRandom().nextInt(3)<2?CombatType.MELEE:CombatType.MAGIC;
        }
        return movementStyle;
    }

    /** Arena-only routing around the pits; ordinary NPC safespots remain local-step based. */
    public void followTarget(Mob target) {
        if(!canPursue(target))return;
        getWalkingQueue().reset();
        if(!pursuitRoute(target)||pursuitRoute.isEmpty())return;
        Location next=pursuitRoute.peekFirst();
        if(CombatMovement.tryNpcStep(this,next.getX()-getLocation().getX(),next.getY()-getLocation().getY())){
            pursuitRoute.removeFirst();pursuitOrigin=next;
        }else pursuitRoute=null;
    }

    private boolean pursuitContact(Location from,Mob target) {
        Location to=target.getLocation();
        if(CombatMovement.standingOn(from,to,size(),target.size()))return false;
        for(int x=0;x<size();x++)for(int y=0;y<size();y++){
            Location tile=from.transform(x,y,0);
            if(tile.getDistance(to)<=1&&org.dementhium.model.map.path.ProjectilePathFinder.clearMeleePath(tile,to))return true;
        }
        return false;
    }

    private boolean pursuitRoute(Mob target) {
        if(!canPursue(target))return false;
        if(pursuitRoute!=null&&getLocation().equals(pursuitOrigin)&&target.getLocation().equals(pursuitTarget))return true;
        pursuitRoute=null;pursuitOrigin=getLocation();pursuitTarget=target.getLocation();
        int minX=NexAreaEvent.ROOM_MIN_X,minY=NexAreaEvent.ROOM_MIN_Y;
        int width=NexAreaEvent.ROOM_MAX_X-minX+1,height=NexAreaEvent.ROOM_MAX_Y-minY+1;
        Location[][] previous=new Location[width][height];
        java.util.ArrayDeque<Location> search=new java.util.ArrayDeque<Location>();
        previous[pursuitOrigin.getX()-minX][pursuitOrigin.getY()-minY]=pursuitOrigin;search.add(pursuitOrigin);
        int[][] steps={{-1,0},{1,0},{0,-1},{0,1},{-1,-1},{-1,1},{1,-1},{1,1}};
        while(!search.isEmpty()){
            Location from=search.removeFirst();
            if(pursuitContact(from,target)){
                pursuitRoute=new java.util.ArrayDeque<Location>();
                while(!from.equals(pursuitOrigin)){pursuitRoute.addFirst(from);from=previous[from.getX()-minX][from.getY()-minY];}
                return true;
            }
            for(int[] step:steps){
                Location next=from.transform(step[0],step[1],0);
                if(!containsArena(next)||previous[next.getX()-minX][next.getY()-minY]!=null
                        ||CombatMovement.standingOn(next,target.getLocation(),size(),target.size())
                        ||!CombatMovement.npcStepClear(this,from,next))continue;
                previous[next.getX()-minX][next.getY()-minY]=from;search.addLast(next);
            }
        }
        return false;
    }

	public boolean noEscapeAttack() {
		return noEscapeAttack;
	}

	@Override
	public boolean isAttackable() {
		return !isDead() && !isDying() && getHitPoints() > 0 && phase != NexPhase.SPAWNED && !noEscapeAttack && !changingPhase;
	}

	@Override
	public boolean isAttackable(Mob attacker) {
		return isAttackable() && super.isAttackable(attacker);
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

    /** A delayed phase effect belongs to this life, including transition entry. */
    public boolean isPhaseCurrent(long life, NexPhase expected) {
        return NexAreaEvent.getNexAreaEvent().getNex() == this
                && getCombatGeneration() == life && !isDead() && !isDying()
                && !changingPhase && phase == expected;
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
		private static final int ROOM_MIN_X = 2910;
		private static final int ROOM_MAX_X = 2941;
		private static final int ROOM_MIN_Y = 5188;
		private static final int ROOM_MAX_Y = 5220;

		public static NexAreaEvent getNexAreaEvent() {
			return INSTANCE;
		}

		private Nex nex;
		private int spawnDelay, minionSpawnDelay, minionSpawnStage;
		private boolean spawned;

		private NPC[] minions = new NPC[4];
		private final List<NPC> bloodReavers = new ArrayList<NPC>();
		private final List<Location> shadowLocations = new ArrayList<Location>();
        private int shadowSequence;
		private final List<Location> icePrisonLocations = new ArrayList<Location>();
		private final List<Location> containmentLocations = new ArrayList<Location>();
		private Player icePrisonTarget;
		private final List<Player> icePrisonTargets = new ArrayList<Player>();
        private final java.util.Map<Player, NPCCombatContext> prisonContexts = new java.util.IdentityHashMap<Player, NPCCombatContext>();
        private final java.util.Map<Player, int[]> forcedPlayers = new java.util.IdentityHashMap<Player, int[]>();
		private int icePrisonSequence;
        private int containmentSequence;
		private Random random = new Random();

		private int delay;
		private int emptyRoomTicks;

		private NexAreaEvent() {
			super(2);
		}

		@Override
		public void execute() {
			if(nex == null) {
				if(delay > 0) {
					delay--;
					return;
				}
				boolean startSpawn = hasLivingPlayers();
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
					// Removal advances the NPC generation and cancels its pending loot.
					if(nex.isDeathRewardComplete())resetEncounter(true);
					return;
				}
				if(nex.isDying())return; // Wrath owns the terminal transition.
				if (!hasLivingPlayers()) {
					if (++emptyRoomTicks >= 5) {
						resetEncounter(false);
					}
					return;
				}
				emptyRoomTicks = 0;
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
						performPendingSpecial();
						checkLife();
						if(!nex.changingPhase && !nex.noEscapeAttack && nex.phase != NexPhase.SPAWNED) {
							int closestDistance = -1;
							Mob closeMob = null;
								for(Player player : getPlayersInRoom()) {
									if(!player.hasReceivedStarter() || !NPCCombatContext.validPair(nex,player)) {
									continue;
								}
								int distance = Misc.getDistance(nex.getLocation().getX(), nex.getLocation().getY(), player.getLocation().getX(), player.getLocation().getY());
								if((closestDistance == -1 || closestDistance > distance) && player.getLocation().getZ() == 0) {
									closestDistance = distance;
									closeMob = player;
								}
							}
								Mob currentVictim = nex.getCombatExecutor().getVictim();
								boolean needsTarget = !NPCCombatContext.validPair(nex,currentVictim);
								if (needsTarget && closeMob != null && closeMob.isPlayer()
										&& closeMob.getPlayer().hasReceivedStarter()) {
									nex.getCombatExecutor().setVictim(closeMob);
								}
						}

						if(nex.phase == NexPhase.SMOKE) {
							if(!nex.changingPhase && !nex.noEscapeAttack) {
								if(random.nextInt(100) < 10 && dragAttack()) {
									return;
								}
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

		public boolean isInNexRoom(Player player) {
			if (player == null || player.getLocation() == null) {
				return false;
			}
			Location location = player.getLocation();
			return location.getZ() == AREA_CENTER.getZ()
					&& location.getX() >= ROOM_MIN_X && location.getX() <= ROOM_MAX_X
					&& location.getY() >= ROOM_MIN_Y && location.getY() <= ROOM_MAX_Y;
		}

		private List<Player> getPlayersInRoom() {
			List<Player> players = new ArrayList<Player>();
			for (Player player : World.getWorld().getPlayers()) {
				if (isInNexRoom(player)) {
					players.add(player);
				}
			}
			return players;
		}

		private int getLivingPlayerCount() {
			int count = 0;
			for (Player player : getPlayersInRoom()) {
				if (player.isOnline() && !player.isDead() && player.hasReceivedStarter()) {
					count++;
				}
			}
			return count;
		}

		private boolean hasLivingPlayers() {
			return getLivingPlayerCount() > 0;
		}

		private void removeNpc(NPC npc) {
			if (npc != null && World.getWorld().getNpcs().contains(npc)) {
				World.getWorld().getNpcs().remove(npc);
			}
		}

		private void clearBloodReavers(boolean healNex) {
			for (NPC reaver : new ArrayList<NPC>(bloodReavers)) {
				if (reaver != null) {
					if (healNex && nex != null && !reaver.isDead()) {
						nex.heal(reaver.getHitPoints());
					}
					removeNpc(reaver);
				}
			}
			bloodReavers.clear();
		}

        private void removeShadow(Player player, Location shadow) {
            ActionSender.deleteObject(player,57261,shadow.getX(),shadow.getY(),shadow.getZ(),10,0);
            // Shadow warnings only replace the client scene; the server retains the scenery.
            // Types 9 through 21 share the warning's scene-object layer.
            for (GameObject object : shadow.getObjectsSnapshot()) {
                if (object.getType() >= 9 && object.getType() <= 21)
                    ActionSender.sendObject(player, object);
            }
        }

        private void clearShadows() {
            shadowSequence++;
            for(Player player:getPlayersInRoom())for(Location shadow:shadowLocations)
                removeShadow(player, shadow);
            shadowLocations.clear();
            if(nex!=null)nex.castedShadow=false;
        }

		private void resetEncounter(boolean defeated) {
			clearShadows();
			clearIcePrison(false);
			clearContainment();
			clearBloodReavers(false);
			for (NPC minion : minions) {
				removeNpc(minion);
			}
			for (Player player : getPlayersInRoom()) {
				for (Location shadow : shadowLocations) {
					removeShadow(player, shadow);
				}
				player.cancelForceMovement();
                player.removeTick("nex_virus");
				player.removeTick("ice_prison");
				player.removeTick("nex_drag");
				player.removeAttribute("cantMove");
				ActionSender.resetCamera(player);
			}
			shadowLocations.clear();
			ObjectManager.clearArea(AREA_CENTER, 16);
			removeNpc(nex);
			nex = null;
			spawned = false;
			spawnDelay = 0;
			minionSpawnDelay = 0;
			minionSpawnStage = 0;
			emptyRoomTicks = 0;
			minions = new NPC[4];
			delay = defeated ? 75 : 10;
		}

		private Player getSpecialTarget() {
			if (nex != null && nex.getCombatExecutor().getVictim() != null
					&& nex.getCombatExecutor().getVictim().isPlayer()) {
				Player victim = nex.getCombatExecutor().getVictim().getPlayer();
				if (isInNexRoom(victim) && victim.isOnline() && !victim.isDead()
						&& victim.hasReceivedStarter()) {
					return victim;
				}
			}
			List<Player> players = new ArrayList<Player>();
			for (Player player : getPlayersInRoom()) {
				if (player.isOnline() && !player.isDead() && player.hasReceivedStarter()) {
					players.add(player);
				}
			}
			return players.isEmpty() ? null : players.get(random.nextInt(players.size()));
		}

		private Player getFarthestPlayer() {
			Player farthest = null;
			double farthestDistance = -1;
			for (Player player : getPlayersInRoom()) {
				if (!player.isOnline() || player.isDead() || !player.hasReceivedStarter()) {
					continue;
				}
				double distance = player.getLocation().distance(nex.getLocation());
				if (distance > farthestDistance) {
					farthest = player;
					farthestDistance = distance;
				}
			}
			return farthest;
		}

		private int attacksBetweenSpecials(NexPhase phase) {
			return phase == NexPhase.SMOKE || phase == NexPhase.SHADOW ? 5 : 3;
		}

		private void resetSpecialRotation(NexPhase phase) {
			if (nex == null) {
				return;
			}
			nex.autoAttacksSinceSpecial = 0;
			nex.specialStep = phase == NexPhase.ICE && !nex.lastBloodSpecialWasSiphon ? 1 : 0;
			nex.specialPending = phase != NexPhase.SPAWNED && phase != NexPhase.FINAL;
		}

		private void recordAutoAttack(Nex attacker) {
			if (attacker == null || attacker != nex || attacker.specialPending
					|| attacker.phase == NexPhase.SPAWNED || attacker.phase == NexPhase.FINAL) {
				return;
			}
			attacker.autoAttacksSinceSpecial++;
			if (attacker.autoAttacksSinceSpecial >= attacksBetweenSpecials(attacker.phase)) {
				attacker.specialPending = true;
			}
		}

		private void performPendingSpecial() {
			if (nex == null || !nex.specialPending || nex.changingPhase || nex.noEscapeAttack
					|| nex.siphonMode || nex.hasTick("ice_attack") || World.getTicks() < nex.specialRecoveryUntil) {
				return;
			}
			// The arena event runs before CombatExecutor decrements its cooldown.
			// Counting the last auto queues a special; it must still wait
			// for that attack's full interval before starting the next action.
			if (nex.autoAttacksSinceSpecial > 0
					&& nex.getCombatExecutor().getTicks() > 1) {
				return;
			}
			boolean performed = false;
			switch (nex.phase) {
			case SMOKE:
				if ((nex.specialStep & 1) == 0) {
					Player target = getFarthestPlayer();
					if (target != null) {
						nex.combatAction.castVirus(target);
						performed = true;
					}
				} else {
					performed = noEscapeAttack();
				}
				break;
			case SHADOW:
				performed = shadowAttack((nex.specialStep & 1) == 1);
				break;
			case BLOOD:
				boolean siphon = (nex.specialStep & 1) == 0;
				performed = bloodAttack(siphon);
				if (performed) {
					nex.lastBloodSpecialWasSiphon = siphon;
				}
				break;
			case ICE:
				performed = iceAttack((nex.specialStep & 1) == 1);
				break;
			default:
				break;
			}
			if (performed) {
				nex.autoAttacksSinceSpecial = 0;
				nex.specialStep++;
				nex.specialPending = false;
			}
		}

		private void zarosAttack() {
			if (nex == null)
				return;
			if (nex.zarosAttackCount == 0 && nex.getId() != DEFAULT_NEX_ID) {
				nex.getMask().setSwitchId(DEFAULT_NEX_ID);
			}
		}

		private GameObject addIceObject(Location location, List<Location> locations, int objectId) {
			if (location == null || location.getZ() != AREA_CENTER.getZ()
                    || location.getX() < ROOM_MIN_X || location.getX() > ROOM_MAX_X
                    || location.getY() < ROOM_MIN_Y || location.getY() > ROOM_MAX_Y
                    || location.hasObjectNoDecoration()
                    || (Region.getClippingMask(location.getX(), location.getY(), location.getZ()) & 0x1280100) != 0) {
				return null;
			}
			GameObject object = ObjectManager.addCustomObject(objectId, location.getX(),
					location.getY(), location.getZ(), 10, 0, false);
			if (object != null) {
				locations.add(location);
				for (Player player : getPlayersInRoom()) {
					ActionSender.sendObject(player, object);
				}
			}
			return object;
		}

		private void removeIceObjects(List<Location> locations) {
			for (Location location : new ArrayList<Location>(locations)) {
				GameObject ice = location.getGameObjectType(10);
                if (ice == null || (ice.getId() != 57262 && ice.getId() != 57263)) continue;
                ObjectManager.removeCustomObject(location.getX(), location.getY(),
						location.getZ(), 10, false);
				for (Player player : getPlayersInRoom()) {
					ActionSender.deleteObject(player, ice.getId(), location.getX(), location.getY(),
							location.getZ(), 10, 0);
				}
			}
			locations.clear();
		}

		private void disableProtectionPrayers(Player player) {
			if (player.getPrayer().isAncientCurses()) {
				player.getPrayer().closeOnPrayers(1, new int[] {Prayer.DEFLECT_MAGIC,
						Prayer.DEFLECT_MELEE, Prayer.DEFLECT_MISSILES, Prayer.DEFLECT_SUMMONING});
			} else {
				player.getPrayer().closeOnPrayers(0, new int[] {Prayer.PROTECT_FROM_MAGIC,
						Prayer.PROTECT_FROM_MISSILES, Prayer.PROTECT_FROM_MELEE,
						Prayer.PROTECT_FROM_SUMMONING});
			}
			player.getPrayer().recalculatePrayer();
			player.getMask().setAppearanceUpdate(true);
		}

		private void clearIcePrison(boolean applyDamage) {
			icePrisonSequence++;
			icePrisonTarget = null;
			removeIceObjects(icePrisonLocations);
			for (Player target : new ArrayList<Player>(icePrisonTargets)) {
				target.removeTick("ice_prison");
				target.removeTick("nex_drag");
				target.removeAttribute("stunned");
				target.removeAttribute("cantMove");
				if (applyDamage && prisonContexts.containsKey(target) && prisonContexts.get(target).isCurrent()) {
					target.sendMessage("The centre of the ice prison freezes you to the bone!");
					int damage = 500 + random.nextInt(201);
					target.getDamageManager().damage(nex, damage, 700, DamageType.RED_DAMAGE);
				}
			}
			icePrisonTargets.clear();
            prisonContexts.clear();
		}

		public boolean breakIcePrison(Player player, Location location) {
			if (player == null || location == null || icePrisonTarget == null
					|| !icePrisonLocations.contains(location)) {
				return false;
			}
			if (icePrisonTargets.contains(player)) {
				player.sendMessage("You cannot break the prison from inside it!");
				return true;
			}
			player.animate(422);
			player.sendMessage("You shatter the icicle and free the trapped player!");
			icePrisonTarget.sendMessage("The icicle shatters and releases you from the prison!");
			clearIcePrison(false);
			return true;
		}

		private boolean createIcePrison(final Player target) {
			if (nex == null || target == null || !isInNexRoom(target) || target.isDead()) {
				return false;
			}
			clearIcePrison(false);
			nex.forceText("Die now, in a prison of ice!");
			nex.playSound(Sounds.NexDieNowInPrison);
            nex.animate(CAST_ANIMATION);
			nex.recoverFromSpecial(6);
			nex.sendPhaseProjectile(target, 362);
			final Location centre = target.getLocation();
			icePrisonTarget = target;
			for (Player player : getPlayersInRoom()) {
				if (!player.isDead() && player.getLocation().equals(centre)) {
					icePrisonTargets.add(player);
                    prisonContexts.put(player, new NPCCombatContext(nex, player));
				}
			}
			for (int x = -1; x <= 1; x++) {
				for (int y = -1; y <= 1; y++) {
					if (x != 0 || y != 0) {
						addIceObject(centre.transform(x, y, 0), icePrisonLocations, 57263);
					}
				}
			}
			for (Player prisoner : icePrisonTargets) {
				disableProtectionPrayers(prisoner);
				prisoner.getWalkingQueue().reset();
				prisoner.stun(6, "The ice prison traps you and disables your protection prayers!", false);
				prisoner.removeTick("nex_drag");
				prisoner.submitTick("nex_drag", new CountdownTick(prisoner, 6, null));
			}
			final Nex prisonOwner = nex;
            final long prisonLife = nex.getCombatGeneration();
            final int sequence = ++icePrisonSequence;
			World.getWorld().submit(new Tick(6) {
				@Override
				public void execute() {
					stop();
					if (sequence == icePrisonSequence && target == icePrisonTarget) {
						clearIcePrison(prisonOwner.isPhaseCurrent(prisonLife, NexPhase.ICE));
					}
				}
			});
			return true;
		}

		private void clearContainment() {
            containmentSequence++;
			removeIceObjects(containmentLocations);
		}

		private boolean createContainment() {
			if (nex == null || nex.hasTick("ice_attack")) {
				return false;
			}
			clearContainment();
			nex.forceText("Contain this!");
			nex.playSound(Sounds.NexContainThis);
			nex.animate(SMASH_ANIMATION);
			nex.graphics(SMASH_SMOKE);
			nex.recoverFromSpecial(5);
			final Location base = nex.getLocation();
			final Nex containmentOwner = nex;
            final long containmentLife = nex.getCombatGeneration();
            final int containmentAt = World.getTicks() + 5;
            final int sequence = containmentSequence;
            nex.submitTick("ice_attack", new Tick(1) {
				@Override
				public void execute() {
                    if (!containmentOwner.isPhaseCurrent(containmentLife, NexPhase.ICE)
                            || sequence != containmentSequence) {
						stop();
						return;
					}
                    if (World.getTicks() < containmentAt) return;
                    stop();
					for (int x = -2; x <= 3; x++) {
						for (int y = -2; y <= 3; y++) {
							if (x == -2 || x == 3 || y == -2 || y == 3) {
								addIceObject(base.transform(x, y, 0), containmentLocations, 57262);
							}
						}
					}
					for (Player player : getPlayersInRoom()) {
						Location location = player.getLocation();
						int xOffset = location.getX() - base.getX();
						int yOffset = location.getY() - base.getY();
						boolean onRing = xOffset >= -2 && xOffset <= 3 && yOffset >= -2
								&& yOffset <= 3 && (xOffset == -2 || xOffset == 3
								|| yOffset == -2 || yOffset == 3);
						if (NPCCombatContext.validPair(nex, player) && onRing && containmentLocations.contains(location)) {
							int damage = random.nextInt(401);
							player.getDamageManager().damage(nex, damage, 400, DamageType.RED_DAMAGE);
							disableProtectionPrayers(player);
							player.stun(5, "The icicles spike you to the spot!", false);
							player.removeTick("nex_drag");
							player.submitTick("nex_drag", new CountdownTick(player, 5, null));
						}
					}
					World.getWorld().submit(new Tick(5) {
						@Override
						public void execute() {
							stop();
							if (containmentOwner.isPhaseCurrent(containmentLife, NexPhase.ICE) && sequence == containmentSequence) clearContainment();
						}
					});
				}
			});
			return true;
		}

		private boolean iceAttack(boolean containment) {
			Player target = getSpecialTarget();
			return containment ? createContainment() : createIcePrison(target);
		}

		private boolean bloodAttack(boolean siphon) {
			if (nex == null)
				return false;
			if (siphon) {
				if (nex.hasTick("siphon")) {
					return false;
				}
				clearBloodReavers(true);
				nex.forceText("A siphon will solve this!");
				nex.playSound(Sounds.NexSiphon);
				nex.siphonMode = true;
				nex.getWalkingQueue().reset();
				nex.animate(SIPHON_ANIMATION);
				nex.setCanAnimate(false);
				int reaverCount = 2;
				for (int i = 0; i < reaverCount; i++) {
					NPC bloodReaver = org.dementhium.model.npc.NPCLoader.getNPC(Nex.REAVER_ID);
					Location spawn = reaverSpawn(bloodReaver.size());
                    if(spawn==null)break;
					// Encounter adds pursue combat targets, without an idle random
					// step moving them back into Nex on the spawn cycle.
					bloodReaver.setDoesWalk(false);
					bloodReaver.setLocation(spawn);
					bloodReaver.setOriginalLocation(spawn);
					bloodReaver.loadEntityVariables();
					bloodReaver.setUnrespawnable(true);
					World.getWorld().getNpcs().add(bloodReaver);
					bloodReavers.add(bloodReaver);
				}
                final Nex siphonOwner=nex;
                final long siphonLife=nex.getCombatGeneration();
                // Named NPC ticks also run on the launch cycle. Measure eight
                // elapsed world ticks so that launch does not consume tick one.
                final int siphonEnds = World.getTicks() + 8;
                nex.submitTick("siphon",new Tick(1){
                    @Override public void execute(){
                        if(nex!=siphonOwner||siphonOwner.getCombatGeneration()!=siphonLife
                                ||siphonOwner.isDead()||siphonOwner.isDying()) {stop();return;}
                        if(World.getTicks() < siphonEnds)return;
                        stop();
                        siphonOwner.siphonMode=false;siphonOwner.setCanAnimate(true);
                    }
                });
				return true;
			}
			if (!nex.siphonMode && !nex.hasTick("blood_sacrifice")) {
				final Player player = getSpecialTarget();
				if (player == null) {
					return false;
				}
				nex.forceText("I demand a blood sacrifice!");
				nex.playSound(Sounds.NexBloodSacrifice);
                final int sacrificeDelay = 7;
				nex.recoverFromSpecial(sacrificeDelay);
				player.graphics(376);
				player.sendMessage("Nex has marked you as a sacrifice, RUN!");
				final Nex sacrificeOwner=nex;final NPCCombatContext sacrificeContext=new NPCCombatContext(nex,player);
                final int sacrificeAt = World.getTicks() + sacrificeDelay;
                final long sacrificeLife = nex.getCombatGeneration();
				nex.submitTick("blood_sacrifice", new Tick(1) {
                    private boolean castStarted;
					@Override
					public void execute() {
						if (!sacrificeOwner.isPhaseCurrent(sacrificeLife, NexPhase.BLOOD) || !sacrificeContext.isCurrent() || !player.isOnline() || !isInNexRoom(player) || player.isDead()) {
                            stop();
							return;
						}
                        // Use the ordinary spell's three-tick cast-to-hit interval inside the escape window.
                        if (!castStarted && World.getTicks() >= sacrificeAt - 3) {
                            castStarted = true;
                            sacrificeOwner.animate(CAST_ANIMATION);
                            sacrificeOwner.sendPhaseProjectile(player, 374);
                        }
                        if (World.getTicks() < sacrificeAt) return;
                        stop();
						for (Player roomPlayer : getPlayersInRoom()) {
							if (roomPlayer.isOnline() && CombatStatus.statusAllowed(roomPlayer)) {
								roomPlayer.getSkills().drainPray(roomPlayer.getSkills().getLevel(5) / 2.0);
							}
						}
						boolean escaped = player.getLocation().distance(nex.getLocation()) >= 5;
						int damage = escaped ? 50 + random.nextInt(51) : 600 + random.nextInt(161);
						Damage sacrifice=Damage.getDamage(nex,player,CombatType.MAGIC,damage,true);sacrifice.setMaximum(760);
						if(!escaped)sacrifice.onImpact(actual->sacrificeOwner.heal(actual));
						player.getDamageManager().damage(nex,sacrifice,DamageType.MAGE);
						if (escaped) {
							player.sendMessage("You escape the worst of Nex's blood sacrifice.");
						} else {
							player.sendMessage("You didn't make it far enough in time!");
							nex.graphics(377);
							// Healing is committed by the sacrifice hit callback.
						}
					}
				});
				return true;
			}
			return false;
		}

        private Location reaverSpawn(int size){
            for(int radius=1;radius<=4;radius++)for(int x=-radius;x<=radius;x++)for(int y=-radius;y<=radius;y++){
                Location tile=nex.getLocation().transform(x,y,0);
                if(CombatMovement.standingOn(tile,nex.getLocation(),size,nex.size()))continue;
                boolean blocked=false;
                for(int dx=0;dx<size&&!blocked;dx++)for(int dy=0;dy<size;dy++){
                    Location body=tile.transform(dx,dy,0);
                    if(!nex.containsArena(body)||(Region.getClippingMask(body.getX(),body.getY(),body.getZ())
                            &(256|0x200000|0x40000))!=0){blocked=true;break;}
                }
                if(blocked)continue;
                boolean occupied=false;for(NPC reaver:bloodReavers)if(!reaver.isDead()
                        &&CombatMovement.standingOn(tile,reaver.getLocation(),size,reaver.size())){occupied=true;break;}
                if(!occupied)return tile;
            }
            return null;
        }

		private boolean shadowAttack(boolean darkness) {
			if (nex == null)
				return false;
			if (darkness) {
                final Nex owner = nex;
                final long life = nex.getCombatGeneration();
                final int started = World.getTicks();
				nex.forceText("Embrace darkness!");
				nex.playSound(Sounds.NexEmbraceDarkness);
				nex.recoverFromSpecial(4);
				for (Player player : getPlayersInRoom()) {
					player.sendMessage("The shadows close in; move away from Nex!");
				}
				nex.submitTick("shadow_darkness", new Tick(1) {
					@Override
					public void execute() {
                        int ticks = World.getTicks() - started;
						if (!owner.isPhaseCurrent(life, NexPhase.SHADOW) || ticks > 8) {
							stop();
							return;
						}
						if (ticks > 0 && (ticks & 1) == 0) {
							for (Player player : getPlayersInRoom()) {
								int distance = (int) Math.ceil(player.getLocation().distance(nex.getLocation()));
								if (NPCCombatContext.validPair(owner, player) && distance <= 5) {
									int damage = Math.max(50, 350 - (distance * 50));
									player.getDamageManager().damage(nex, damage, 350, DamageType.RED_DAMAGE);
								}
							}
						}
					}
				});
				return true;
			}
			if (!nex.castedShadow) {
				final List<Player> localPlayers = new ArrayList<Player>();
				for(Player player : getPlayersInRoom()) {
					if (!player.isDead() && player.hasReceivedStarter()) {
						localPlayers.add(player);
					}
				}
				if(localPlayers.size() == 0) {
					return false;
				}
				nex.castedShadow = true;
				nex.recoverFromSpecial(4);
				final Nex owner = nex;
                final long shadowLife=owner.getCombatGeneration();
                final int sequence=++shadowSequence;
                final List<Location> locationArray = new ArrayList<Location>();
				nex.forceText("Fear the shadow!");
				nex.playSound(Sounds.NexFearTheShadow);
				for(Player player : localPlayers) {
					if (locationArray.contains(player.getLocation())) continue;
                    locationArray.add(player.getLocation());
					shadowLocations.add(player.getLocation());
					for(Player local : localPlayers) {
						ActionSender.sendObject(local, 57261, player.getLocation().getX(), player.getLocation().getY(), 0, 10, 0);
					}
				}
				localPlayers.clear();
				World.getWorld().submit(new Tick(3) {
					@Override
					public void execute() {
						stop();
						if (nex != owner || owner.isDead() || owner.isDying() || owner.getCombatGeneration()!=shadowLife
                                || sequence!=shadowSequence || owner.phase!=NexPhase.SHADOW || owner.changingPhase) return;
                        nex.castedShadow = false;
						for(Player player : getPlayersInRoom()) {
							for(Location loc : locationArray) {
								removeShadow(player, loc);
								ActionSender.sendPositionedGraphic(player, loc, 383);
								if(nex != null && player.getLocation().equals(loc) && player.hasReceivedStarter()
										&& isInNexRoom(player)) {
									int damageInflicted = 200 + Misc.random(400);
								player.getDamageManager().damage(nex, damageInflicted, 600, DamageType.RED_DAMAGE);
								}
							}
						}
						for (Location loc : locationArray) {
							shadowLocations.remove(loc);
						}
					}
				});
				return true;
			}
			return false;
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
            minion.setDoesWalk(false);
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
								nex.protectingMinion = false;
								minions[index] = null;
								changePhase(NexPhase.values()[next]);
							}
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
			if (nex == null || nex.changingPhase || nex.phase == phase)
				return;
			final Nex owner = nex;
            final long phaseLife=owner.getCombatGeneration();
            nex.changingPhase = true;
            nex.getWalkingQueue().reset();
            nex.noEscapeAttack=false;nex.cancelForceMovement();
            clearForcedPlayers();
            nex.removeTick("siphon");nex.removeTick("blood_sacrifice");nex.removeTick("shadow_darkness");nex.removeTick("ice_attack");
            nex.siphonMode=false;nex.setCanAnimate(true);
            clearShadows();clearBloodReavers(false);clearIcePrison(false);clearContainment();
			int ticks = 5;
			if(nex.phase == NexPhase.SPAWNED) {
				ticks = 2;
			}
			World.getWorld().submit(new Tick(ticks) {
				@Override
				public void execute() {
					if (nex != owner || owner.isDead() || owner.isDying() || owner.getCombatGeneration()!=phaseLife) {
						stop();
						return;
					}
					stop();
					if(nex.phase != phase) {
						nex.forceText(phase.initialMessage);
						nex.playSound(phase.soundId);
							if(phase != NexPhase.FINAL) {
							ProjectileManager.sendGlobalProjectile(2244, minions[phase.ordinal() - 1], nex, 46, 60, 50);
							} else {
								nex.heal(6000);
								nex.animate(TURMOIL_ANIMATION);
								nex.graphics(TURMOIL_GRAPHICS);
								nex.zarosAttackCount = 0;
								nex.getMask().setSwitchId(SOUL_SPLIT_NEX);
						}
							World.getWorld().submit(new Tick(3) {
							@Override
							public void execute() {
								if (nex != owner || owner.isDead() || owner.isDying() || owner.getCombatGeneration()!=phaseLife) {
									stop();
									return;
								}
								stop();
								nex.changingPhase = false;
							nex.phase = phase;
							nex.protectingMinion = false;
                                nex.protectingCruor = false;
								resetSpecialRotation(phase);
								if (phase != NexPhase.BLOOD) {
									clearBloodReavers(false);
                                    nex.removeTick("siphon");
                                    nex.siphonMode = false;
                                    nex.setCanAnimate(true);
								}
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

		private boolean noEscapeAttack() {
			if (nex == null)
				return false;
			if (nex.noEscapeAttack) {
				return false;
			}
				final Nex owner = nex;
                final long chargeLife=owner.getCombatGeneration();
                final NexPhase chargePhase=owner.phase;
                nex.lastEscapeAttack = System.currentTimeMillis();
				nex.noEscapeAttack = true;
				nex.getCombatExecutor().setVictim(null);
				nex.getWalkingQueue().reset();
				nex.forceText("There is...");
				nex.playSound(Sounds.NexThereIs);
				nex.teleport(NO_ESCAPE_CENTER, false);
				World.getWorld().submit(new Tick(2) {
					@Override
					public void execute() {
						if (!chargeCurrent(owner,chargeLife,chargePhase)) {
							stop();
							return;
						}
						stop();
						nex.animate(FLY_ANIMATION);
						nex.graphics(FLYING_PURPLE_SMOKE);

						World.getWorld().submit(new Tick(2) {
							@Override
							public void execute() {
								if (!chargeCurrent(owner,chargeLife,chargePhase)) {
									stop();
									return;
								}
								stop();
								final int index = random.nextInt(NO_ESCAPE_TELEPORTS.length);
								final Location noEscapePosition = NO_ESCAPE_TELEPORTS[index];

								nex.forceText("NO ESCAPE!");
								nex.playSound(Sounds.NexNoEscape);
								nex.getMask().setFacePosition(noEscapePosition, 1, 1);
								World.getWorld().submit(new Tick(2) {

									private List<Player> playersToHit;

									private int countdown = 3;

									@Override
									public void execute() {
										if (!chargeCurrent(owner,chargeLife,chargePhase)) {
											stop();
											return;
										}
										countdown--;
										if(countdown == 2) {
											// Let forceMovement derive the protocol direction. The special's
											// aisle index describes the route, not the movement direction.
											nex.forceMovement(FLY_ANIMATION, noEscapePosition.getX(), noEscapePosition.getY(), 0, 60, -1, 2, true, true);
											for(Player attack : playersToHit = attackablePlayers(index)) {
												attack.getMask().setFacePosition(noEscapePosition, 1, 1);
												// Players may dodge until the charge reaches them.
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

													if (movementX >= ROOM_MIN_X + 1 && movementX < ROOM_MAX_X
                                                        && movementY >= ROOM_MIN_Y + 1 && movementY < ROOM_MAX_Y
                                                        && attack.getAttribute("cantMove") == null
                                                        && (Region.getClippingMask(movementX, movementY, 0) & 0x1280100) == 0) {
                                                    attack.forceMovement(FALL_BACK_ANIMATION, movementX, movementY, 30, 60, dir, 2, true);
                                                    forcedPlayers.put(attack, attack.getForceWalk());
                                                }
												int maxDamage = nex.phase == NexPhase.FINAL ? 550 : 700;
												int damage = r.nextInt(maxDamage);
												// Movement completion releases the lock.
												disableProtectionPrayers(attack);
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
			return true;
		}

        private boolean chargeCurrent(Nex owner,long life,NexPhase phase) {
            return owner.isPhaseCurrent(life, phase)&&owner.noEscapeAttack;
        }
        private void clearForcedPlayers() {
            for (java.util.Map.Entry<Player, int[]> entry : forcedPlayers.entrySet()) {
                if (entry.getKey().getForceWalk() == entry.getValue()) entry.getKey().cancelForceMovement();
            }
            forcedPlayers.clear();
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
			List<Player> filtered = new CopyOnWriteArrayList<Player>();
			for (Player player : players) {
				if (player != null && player.isOnline() && !player.isDead()
						&& isInNexRoom(player) && !filtered.contains(player)) {
					filtered.add(player);
				}
			}
			return filtered;
		}

		public boolean drag(final Player victim) {
			if (nex == null || nex.isDead() || nex.noEscapeAttack || nex.changingPhase
					|| victim == null || !victim.isOnline() || victim.isDead()
					|| !isInNexRoom(victim) || victim.getAttribute("cantMove") != null
					|| victim.getMask().isForceMovementUpdate())
				return false;
			if(victim.hasTick("nex_drag") || victim.getHitPoints() < 100 || victim.getAttribute("superhit") != null || !victim.hasReceivedStarter() || !(victim.getLocation().distance(AREA_CENTER) < 16)) {
				return false;
			}
			Location destination = null;
			double nearest = Double.MAX_VALUE;
			int size = nex.size();
			for (int dx = -1; dx <= size; dx++) {
				for (int dy = -1; dy <= size; dy++) {
					if (dx >= 0 && dx < size && dy >= 0 && dy < size) continue;
					Location tile = nex.getLocation().transform(dx, dy, 0);
					if (tile.getZ() != 0 || tile.getX() < 2911 || tile.getX() > 2940
							|| tile.getY() < 5189 || tile.getY() > 5219
							|| (Region.getClippingMask(tile.getX(), tile.getY(), tile.getZ()) & 0x1280100) != 0) continue;
					double distance = tile.distance(victim.getLocation());
					if (distance < nearest) { destination = tile; nearest = distance; }
				}
			}
			if (destination == null) return false;
			final Nex owner = nex;
			final Location landing = destination;
            final long life = owner.getCombatGeneration();
            final NexPhase dragPhase = owner.phase;
            final long victimLife = victim.getCombatRevision();
            final long victimInstance = victim.getInstanceRevision();
			nex.getCombatExecutor().setVictim(victim);
			victim.sendMessage("Nex draws you in...");
			victim.forceMovement(DRAG_ANIMATION, destination.getX(), destination.getY(), 0, 60, -1, 2, true);
            final int[] movement = victim.getForceWalk();
            forcedPlayers.put(victim, movement);
			victim.submitTick("nex_drag", new Tick(1) {
				private int cycles = 0;
				@Override
				public void execute() {
					boolean landed = victim.getLocation().equals(landing) && victim.getForceWalk() == movement;
                    // The owned landing is itself one player teleport/revision change.
                    boolean current = victim.getInstanceRevision() == victimInstance
                            && (victim.getCombatRevision() == victimLife
                                || landed && victim.getCombatRevision() == victimLife + 1);
                    if(cycles >= 5 || !owner.isPhaseCurrent(life, dragPhase) || !current || !victim.isOnline()
							|| victim.isDead() || !isInNexRoom(victim)) {
                        if (victim.getForceWalk() == movement) victim.cancelForceMovement();
                        if (forcedPlayers.get(victim) == movement) forcedPlayers.remove(victim);
						stop();
						return;
					}
					if(cycles == 2) {
						if (victim.getLocation().equals(landing)) {
							disableProtectionPrayers(victim);
							victim.getDamageManager().damage(nex, 300 + random.nextInt(101), 400, DamageType.RED_DAMAGE);
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

		@Override public CombatAction newSession(){return new NexCombatAction();}
		public NexCombatAction() {
			super(true);
		}

		private int getShadowMaxDamage(Mob victim) {
			int distance = (int) Math.ceil(getLocation().distance(victim.getLocation()));
			distance = Math.max(1, Math.min(10, distance));
			return Math.max(50, 550 - (distance * 50));
		}

		private boolean prepareZarosPrayer() {
			if (phase != NexPhase.FINAL) {
				return false;
			}
			// Original Nex held each overhead for three attacks (roughly seven
			// seconds), beginning with Soul Split, then Deflect Melee, then none.
			int cycle = (zarosAttackCount / 3) % 3;
			zarosAttackCount++;
			int prayerId = cycle == 0 ? SOUL_SPLIT_NEX
					: cycle == 1 ? MELEE_DEFLECT_NEX : DEFAULT_NEX_ID;
			if (getId() != prayerId) {
				getMask().setSwitchId(prayerId);
			}
			return cycle == 0;
		}

		private void applySoulSplit(Mob victim, int damage) {
			if (victim == null || damage <= 0) {
				return;
			}
			int distance = Math.max(1, (int) getLocation().distance(victim.getLocation()));
			ProjectileManager.sendGlobalProjectile(2263, Nex.this, victim, 30, 32,
					distance >= 4 ? 20 : 10, 11);
			victim.graphics(2264);
			graphics(2264);
			heal(damage / 5);
			if (victim.isPlayer() && damage >= 50) {
				victim.getPlayer().getSkills().drainPray(damage / 50);
			}
		}

		private void applyTurmoil(Mob victim, int damage) {
			if (damage <= 0 || victim == null || !victim.isPlayer()) {
				return;
			}
			Player player = victim.getPlayer();
			for (int skill : new int[] {Skills.ATTACK, Skills.STRENGTH, Skills.DEFENCE}) {
				if (player.getSkills().getLevel(skill) > 1) {
					player.getSkills().decreaseLevelOnce(skill, 1);
				}
			}
		}

        /** Protection, shields, absorption and overkill settle before any attack effect. */
        private Damage applyAttackDamage(Mob victim,int raw,int maximum,CombatType style,NexPhase attackPhase,boolean soulSplitAttack){
            if(!NPCCombatContext.validPair(Nex.this,victim))return new Damage(0);
            double protection=victim.isPlayer()&&org.dementhium.model.npc.encounter.EncounterAttack.protects(victim.getPlayer(),style)?.6:1;
            Damage hit=Damage.getDamage(Nex.this,victim,style,raw<0?-1:(int)(raw*protection),true).withShieldInput(raw,protection,false);
            hit.setMaximum(maximum);
            final boolean poison=attackPhase==NexPhase.SMOKE&&r.nextInt(100)<25;
            hit.onImpact(actual->{
                if(isDead()||NexAreaEvent.getNexAreaEvent().getNex()!=Nex.this)return;
                if(attackPhase==NexPhase.FINAL)applyTurmoil(victim,actual);
                if(soulSplitAttack)applySoulSplit(victim,actual);
                if(poison&&CombatStatus.statusAllowed(victim)){victim.getPoisonManager().poison(Nex.this,60+r.nextInt(50));victim.graphics(AFTERMATH_GRAPHICS[NexPhase.SMOKE.ordinal()]);}
                if(attackPhase==NexPhase.BLOOD&&style!=CombatType.MELEE){heal(Math.round(actual*.10F));victim.graphics(AFTERMATH_GRAPHICS[NexPhase.BLOOD.ordinal()]);}
                if(attackPhase==NexPhase.ICE)victim.graphics(AFTERMATH_GRAPHICS[NexPhase.ICE.ordinal()]);
            });
            if(raw>=0)victim.getDamageManager().damage(Nex.this,hit,style.getDamageType());


            return hit;
        }
		@Override public boolean executeSession(){return true;}

		public void castVirus(Mob victim) {
			if (victim == null || !victim.isPlayer() || victim.isDead()
					|| !NexAreaEvent.getNexAreaEvent().isInNexRoom(victim.getPlayer())) {
				return;
			}
			animate(CAST_ANIMATION);
			recoverFromSpecial(4);
			forceText("Let the virus flow through you!");
			playSound(Sounds.NexVirus);
			if(victim.hasTick("nex_virus")) {
				victim.removeTick("nex_virus");
			}
			Player player = victim.getPlayer();
			victim.submitTick("nex_virus", new NexVirusTick(player));
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
			if(isDead() || isDying() || noEscapeAttack || changingPhase || siphonMode || specialPending || hasTick("ice_attack") || World.getTicks() < specialRecoveryUntil) {
				return false;
			}

			movementStyle=null;
			final boolean close = CombatMovement.hasMeleeContact(Nex.this,interaction.getVictim(),true);
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
			if (!usingMagic && !close) {
				return false;
			}
			if (!usingMagic && getCombatExecutor().getTicks() > 2) {
				return false;
			}
			final boolean soulSplitAttack = prepareZarosPrayer();

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
					damage = MagicFormulae.getDamage(Nex.this, interaction.getVictim(),
							maxDamage = getShadowMaxDamage(interaction.getVictim()));
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

				final NexPhase attackPhase = phase;
                final long attackLife = getCombatGeneration();
                final List<Player> attackTargets = new ArrayList<Player>();
                final java.util.Map<Player,NPCCombatContext> attackContexts=new java.util.IdentityHashMap<Player,NPCCombatContext>();
                for (Player target : NexAreaEvent.getNexAreaEvent().getPlayersInRoom()) {
                    if (!target.isOnline() || target.isDead() || target.isHidden() || target.isInvisible() || !target.hasReceivedStarter() || !NPCCombatContext.validPair(Nex.this,target)) continue;
                    attackTargets.add(target);attackContexts.put(target,new NPCCombatContext(Nex.this,target));
                    if (projectileId != -1) sendPhaseProjectile(target, projectileId);
                    if (attackTargets.size() == 20) break;
                }
                final int fMaxDamage = maxDamage;
				World.getWorld().submit(new Tick(3) {
					private int attacked;
					@Override
					public void execute() {
						stop();
						if (!isPhaseCurrent(attackLife, attackPhase)) return;
                        for(Player other : attackTargets) {
							if(attacked >= 20) {
								break;
							}
							if (!attackContexts.get(other).isCurrent() || other.isHidden() || other.isInvisible() || !other.isOnline() || other.isDead() || !other.hasReceivedStarter()
                                    || !NexAreaEvent.getNexAreaEvent().isInNexRoom(other))
								continue;
							//if(other == interaction.getVictim() && close) {
								//continue;
							//}
							int otherMaxDamage = attackPhase == NexPhase.SHADOW ? getShadowMaxDamage(other) : fMaxDamage;
							int castedDamage = attackPhase == NexPhase.SHADOW
                                ? RangeFormulae.getDamage(Nex.this, other, 1.0, otherMaxDamage, 1.0)
                                : MagicFormulae.getDamage(Nex.this, other, otherMaxDamage);
                            applyAttackDamage(other,castedDamage,otherMaxDamage,attackPhase==NexPhase.SHADOW?CombatType.RANGE:CombatType.MAGIC,attackPhase,soulSplitAttack);
                            other.retaliate(Nex.this);attacked++;
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
					applyAttackDamage(interaction.getVictim(),damage,maxDamage2,CombatType.MELEE,phase,soulSplitAttack);
					if (phase == NexPhase.FINAL) {
						for (Player nearby : NexAreaEvent.getNexAreaEvent().getPlayersInRoom()) {
							if (nearby == interaction.getVictim() || nearby.isDead()
									|| nearby.getLocation().distance(interaction.getVictim().getLocation()) > 1) {
								continue;
							}
							int cleave = MeleeFormulae.getDamage(Nex.this, nearby, maxDamage2);
							applyAttackDamage(nearby,cleave,maxDamage2,CombatType.MELEE,phase,soulSplitAttack);
						}
					}
					interaction.getVictim().retaliate(Nex.this);
				} else {
					return false;
				}
			}//
			//CombatType type = usingMagic ? phase == NexPhase.SHADOW ? CombatType.RANGE : CombatType.MAGIC : CombatType.MELEE;
			NexAreaEvent.getNexAreaEvent().recordAutoAttack(Nex.this);
			interaction.setDamage(new Damage(damage));
			return false;
		}

		@Override
		public boolean endSession() {
			return true;
		}

		@Override
		public CombatType getCombatType() {
			return movementType(interaction==null?getCombatExecutor().getVictim():interaction.getVictim());
		}

	}

}
