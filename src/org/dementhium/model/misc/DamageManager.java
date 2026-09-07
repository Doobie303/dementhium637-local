package org.dementhium.model.misc;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.dementhium.model.Mob;
import org.dementhium.model.World;
import org.dementhium.model.combat.CombatType;
import org.dementhium.model.combat.Damage;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.npc.impl.Nex;
import org.dementhium.model.npc.impl.Nex.NexAreaEvent;
import org.dementhium.model.npc.impl.Nex.NexPhase;
import org.dementhium.model.player.DegradingHandler;


/**
 * @author Emperor
 * @author 'Mystic Flow <Steven@rune-server.org>
 */
public class DamageManager {

	/**
	 * @author 'Mystic Flow <Steven@rune-server.org>
	 */
	public enum DamageType {
		MELEE(0), RANGE(1), MAGE(2), RED_DAMAGE(3), DEFLECT(4), SOAK(5), POSION(6), DISEASED(7), MISS(8), HEAL(9);

		private int type;

		private DamageType(int type) {
			this.type = type;
		}

		public int toInteger() {
			return type;
		}
	}

	/**
	 * @author 'Mystic Flow <Steven@rune-server.org> (zach for adding 1 thing =D)
	 */
	public class DamageHit {
		private Mob victim, attacker;
		private int damage;
		private DamageType type;
		private int currentHealth;
		private boolean isMax;
		private int delay;

		private DamageHit partner;
		private boolean isPartner;

		public Mob getVictim() {
			return victim;
		}

		public Mob getAttacker() {
			return attacker;
		}

		public int getDamage() {
			return damage;
		}

		public DamageType getType() {
			return type;
		}

		public int getCurrentHealth() {
			return currentHealth;
		}

		public boolean isMax() {
			return isMax;
		}

		public DamageHit getPartner() {
			return partner;
		}

		public int getDelay() {
			return delay;
		}

		public boolean isPartner() {
			return isPartner;
		}
	}

	private final LinkedList<DamageHit> hits = new LinkedList<DamageHit>();
	private final Map<Mob, Integer> enemyHits = new HashMap<Mob, Integer>();
	private final Mob mob;

	public DamageManager(Mob mob) {
		this.mob = mob;
	}

	private boolean nexIsShielded(NPC npc) {
		if (npc == null) {
			return false;
		}
		int id = npc.getId();
		if (npc instanceof Nex && ((Nex) npc).isProtectingMinion()) {
			return true;
		}
		if (id >= 13447 && id <= 13450) {
			Nex areaNex = NexAreaEvent.getNexAreaEvent().getNex();
			if (areaNex != null && areaNex.isProtectingMinion()) {
				return true;
			}
		}
		if (id >= 13451 && id <= 13454
				&& !Boolean.TRUE.equals(npc.getAttribute("nex_vulnerable"))) {
			return true;
		}
		return false;
	}

	private int applyGodHits(Mob attacker, int damage, DamageType type) {
		if (attacker != null && attacker.isPlayer()
				&& Boolean.TRUE.equals(attacker.getAttribute("godmode"))
				&& type != DamageType.HEAL && type != DamageType.MISS) {
			if (mob.isNPC() && nexIsShielded(mob.getNPC())) {
				return 0;
			}
			return 500;
		}
		return damage;
	}


	public LinkedList<DamageHit> getHits() {
		return hits;
	}

	public Mob getMob() {
		return mob;
	}

	public void damage(Mob attacker, int damage, int maxDamage, DamageType type) {
		if (attacker != null) {
			if (mob.isPlayer() && mob.getAttribute("hitImmunity", -1) > World.getTicks())
				return;
			if (mob.isNPC()) {
				NPC npc = mob.getNPC();
				if (npc.getId() >= 13451 && npc.getId() <= 13454
						&& !Boolean.TRUE.equals(npc.getAttribute("nex_vulnerable"))) {
					damage = 0;
				}
				if (npc.isNex()) {
					Nex nex = NexAreaEvent.getNexAreaEvent().getNex();
					if (nex != null) {
						if (nex.isProtectingMinion()) {
							damage = 0;
						} else if (nex.isSiphonMode()) {
							type = DamageType.HEAL;
						}
					}
			/*} else if (npc.getId() >= 13451 && npc.getId() <= 13454) {
					Nex nex = NexAreaEvent.getNexAreaEvent().getNex();
					int hitpoints = nex.getHitPoints();
					int maxHitpoints = nex.getMaximumHitPoints();
					boolean weak = true;
					if (npc.getId() == Nex.FUMUS && hitpoints <= maxHitpoints * 0.8 && nex.getPhase() == NexPhase.SMOKE) {
						weak = false;
					}
					if (npc.getId() == Nex.UMBRA && hitpoints <= maxHitpoints * 0.6 && nex.getPhase() == NexPhase.SHADOW) {
						weak = false;
					}
					if (npc.getId() == Nex.CRUOR && (hitpoints <= maxHitpoints * 0.4 || nex.isProtectingCruor()) && nex.getPhase() == NexPhase.BLOOD) {
						weak = false;
					}
					if (npc.getId() == Nex.GLACIES && hitpoints <= maxHitpoints * 0.2 && nex.getPhase() == NexPhase.ICE) {
						weak = false;
					}
					if (weak) {
						damage = 0;
						if (attacker.isPlayer())
							attacker.getPlayer().sendMessage("The avatar is not weak enough to damage this minion.");
					}*/
				} /*else if (npc instanceof TormentedDemon) {
					TormentedDemon demon = (TormentedDemon) npc;
					if (demon.hasShield()) {
						demon.graphics(1885); //todo
						damage *= .75;
					}
					if (demon.usingCorrespondingPrayer(type)) {
						damage *= .80;
					}
					demon.increaseHitAmount(damage, type);
				} else if (npc instanceof SkeleHorror) {
					SkeleHorror horror = (SkeleHorror) npc;
					System.out.println("rarw");
					horror.hit(damage, attacker.getPlayer());
				}
			} else if (attacker.isNPC() && mob.isPlayer()) {
				if (attacker.getNPC().isNex()) {
					Nex nex = NexAreaEvent.getNexAreaEvent().getNex();
					if (nex != null && nex.getId() == Nex.SOUL_SPLIT_NEX) {
						CombatAfterEffect effect = IdentifierManager.getIdentifier("combat_after_effect");
						effect.cursePrayers(attacker, mob, damage);
					}
				}*/
			}
		}
		damage = applyGodHits(attacker, damage, type);
		if (damage > mob.getHitPoints()) {
			damage = mob.getHitPoints();
		}
		try{
			addEnemyHit(attacker, damage);
		}catch(NullPointerException exception){
			exception.printStackTrace();
		}
		DamageHit hit = new DamageHit();
		hit.victim = mob;
		hit.attacker = attacker;
		updateDamageAttributes(hit, type, damage);
		if (damage >= maxDamage && maxDamage != -1) {
			hit.isMax = true;
		}
		hits.add(hit);
	}

	public void damage(Mob attacker, int damage, int maxDamage, DamageType type, int delay) {
		if (attacker != null) { //lol @ who changed method params.
			if (mob.isPlayer() && mob.getAttribute("hitImmunity", -1) > World.getTicks())
				return;
			if (mob.isNPC()) {
				NPC npc = mob.getNPC();
				if (npc.getId() >= 13451 && npc.getId() <= 13454
						&& !Boolean.TRUE.equals(npc.getAttribute("nex_vulnerable"))) {
					damage = 0;
				}
				if (npc.isNex()) {
					Nex nex = NexAreaEvent.getNexAreaEvent().getNex();
					if (nex != null) {
						if (nex.isProtectingMinion()) {
							damage = 0;
						} else if (nex.isSiphonMode()) {
							type = DamageType.HEAL;
						}
					}
				/*} else if (npc.getId() >= 13451 && npc.getId() <= 13454) {
					Nex nex = NexAreaEvent.getNexAreaEvent().getNex();
					int hitpoints = nex.getHitPoints();
					int maxHitpoints = nex.getMaximumHitPoints();
					boolean weak = true;
					if (npc.getId() == Nex.FUMUS && hitpoints <= maxHitpoints * 0.8 && nex.getPhase() == NexPhase.SMOKE) {
						weak = false;
					}
					if (npc.getId() == Nex.UMBRA && hitpoints <= maxHitpoints * 0.6 && nex.getPhase() == NexPhase.SHADOW) {
						weak = false;
					}
					if (npc.getId() == Nex.CRUOR && (hitpoints <= maxHitpoints * 0.4 || nex.isProtectingCruor()) && nex.getPhase() == NexPhase.BLOOD) {
						weak = false;
					}
					if (npc.getId() == Nex.GLACIES && hitpoints <= maxHitpoints * 0.2 && nex.getPhase() == NexPhase.ICE) {
						weak = false;
					}
					if (weak) {
						damage = 0;
						if (attacker.isPlayer())
							attacker.getPlayer().sendMessage("The avatar is not weak enough to damage this minion.");
					}*/
				/*} else if (npc instanceof TormentedDemon) {
					TormentedDemon demon = (TormentedDemon) npc;
					if (demon.hasShield()) {
						demon.graphics(1885); //todo
						damage *= .75;
					}
					if (demon.usingCorrespondingPrayer(type)) {
						damage *= .80;
					}
					demon.increaseHitAmount(damage, type);
				} else if (npc instanceof SkeleHorror) {
					SkeleHorror horror = (SkeleHorror) npc;
					horror.hit(damage, attacker.getPlayer());
				}
			} else if (attacker.isNPC() && mob.isPlayer()) {
				if (attacker.getNPC().isNex()) {
					Nex nex = NexAreaEvent.getNexAreaEvent().getNex();
					if (nex != null && nex.getId() == Nex.SOUL_SPLIT_NEX) {
						CombatAfterEffect effect = IdentifierManager.getIdentifier("combat_after_effect");
						effect.cursePrayers(attacker, mob, damage);
					}*/
				}
			}
		}
		damage = applyGodHits(attacker, damage, type);
		if (damage > mob.getHitPoints()) {
			damage = mob.getHitPoints();
		}
		DamageHit hit = new DamageHit();
		hit.victim = mob;
		hit.attacker = attacker;
		hit.delay = delay;
		addEnemyHit(attacker, damage);
		updateDamageAttributes(hit, type, damage);
		if (damage >= maxDamage && maxDamage != -1) {
			hit.isMax = true;
		}
		hits.add(hit);
	}

	public void miscDamage(int damage, DamageType type) {
		if (mob.isPlayer() && mob.getAttribute("hitImmunity", -1) > World.getTicks())
			return;
		DamageHit hit = new DamageHit();
		hit.victim = mob;
		hit.attacker = mob;
		if (damage > mob.getHitPoints()) {
			damage = mob.getHitPoints();
		}
		updateDamageAttributes(hit, type, damage);
		hits.add(hit);
	}
	
	/*
	 * Used for Dragon Claws (Slice And Dice) and Ranged.
	 */
	public void miscDamage(int damage, DamageType type, int delay) {
		if (mob.isPlayer() && mob.getAttribute("hitImmunity", -1) > World.getTicks())
			return;
		DamageHit hit = new DamageHit();
		hit.victim = mob;
		hit.attacker = mob;
		hit.delay = delay;
		if (damage > mob.getHitPoints()) {
			damage = mob.getHitPoints();
		}
		updateDamageAttributes(hit, type, damage);
		hits.add(hit);
	}

	/**
	 * Applies damage to this mob.
	 * @param source The mob who dealt the damage.
	 * @param damage The damage to deal.
	 * @param type The damage type. 
	 */
	public void damage(Mob source, Damage damage, DamageType type) {
		damage(source, damage, type, 0);
	}

	/**
	 * Applies damage to this mob.
	 * @param source The mob who dealt the damage.
	 * @param damage The damage to deal.
	 * @param type The damage type.
	 * @param delay The delay before dealing the damage.
	 */
	public void damage(Mob source, Damage damage, DamageType type, int delay) {
		if (mob.isPlayer() && mob.getAttribute("hitImmunity", -1) > World.getTicks())
			return;
		if (source != null && source.isPlayer()
				&& Boolean.TRUE.equals(source.getAttribute("godmode"))) {
			int boosted = applyGodHits(source, 500, type);
			damage.setHit(boosted);
		}
		if (damage.getHit() > mob.getHitPoints()) {
			damage.setHit(mob.getHitPoints());
		}
		DamageHit hit = new DamageHit();
		hit.victim = mob;
		hit.attacker = source;
		hit.delay = delay;
		addEnemyHit(source, damage.getHit());
		updateDamageAttributes(hit, type, damage.getHit());
		if (damage.getHit() >= damage.getMaximum() && damage.getMaximum() != -1) {
			hit.isMax = true;
		}
		hits.add(hit);
	}

	//this is named soaking, gotta work on this
	/*public void soak(Mob attacker, int amount) {
		if (mob.isPlayer() && mob.getAttribute("hitImmunity", -1) > World.getTicks())
			return;
		DamageHit last = hits.peekLast();
		if (last == null || last.partner != null || last.isPartner) {
			return;
		}
		DamageHit hit = new DamageHit();
		hit.victim = mob;
		hit.attacker = attacker;
		hit.isPartner = true;
		hit.damage = amount;
		hit.type = DamageType.SOAK;
		last.partner = hit;
	}*/
	
	public boolean soak(DamageHit hit, Mob attacker, int damage) {
		if (hit == null || hit.isPartner)
			return false;
		DamageHit soak = new DamageHit();
		if (mob.isPlayer() && damage > 200) {
			int soaked = Damage.calculateSoaked(mob, damage, 
					attacker == null ? CombatType.MELEE : attacker.getCombatAction().getCombatType());
			if (soaked > 0) {
				soak.victim = mob;
				soak.attacker = attacker;
				soak.isPartner = true;
				soak.damage = soaked;
				soak.type = DamageType.SOAK;
				//updateDamageAttributes(hit2, DamageType.SOAK, soaked);
				hit.partner = soak;
				hit.damage = damage - soaked;
				return true;
			}
		}
		return false;
	}

	public void updateDamageAttributes(DamageHit hit, DamageType type, int damage) {
		if (mob.isPlayer() && mob.getAttribute("hitImmunity", -1) > World.getTicks())
			return;
		if (mob.isNPC() && nexIsShielded(mob.getNPC())) {
			damage = 0;
			type = DamageType.MISS;
		}
		hit.type = type;
		if (damage < 1) {
			type = DamageType.MISS;
			damage = 0;
		}
		if (!soak(hit, hit.attacker, damage))
			hit.damage = damage;
		if (mob.isPlayer()) {
			mob.getPlayer().getSkills().hit(damage);
		} else {
			if (type == DamageType.HEAL) {
				mob.getNPC().heal(damage);
			} else {
				mob.getNPC().hit(damage);
			}
		}
		int currentHitpoints = mob.getHitPoints();
		int maximumHitpoints = mob.getMaximumHitPoints();
		if (currentHitpoints > maximumHitpoints) {
			currentHitpoints = maximumHitpoints;
		}
		hit.currentHealth = currentHitpoints * 255 / maximumHitpoints;
		if (damage > 0) {
			if (hit.attacker != null && hit.attacker.isPlayer()) {
				DegradingHandler.process(hit.attacker.getPlayer());
			}
			if (mob.isPlayer()) {
				DegradingHandler.process(mob.getPlayer());
			}
		}
	}

	public void addEnemyHit(Mob enemy, int damage) {
		if (damage > 0) {
			if (!enemyHits.containsKey(enemy)) {
				enemyHits.put(enemy, damage);
			} else {
				enemyHits.put(enemy, enemyHits.get(enemy) + damage);
			}
		}
	}

	public Mob getKiller() {
		Mob killer = null;
		int mostDamage = 0;
		int familiarDamage = 0;
		for (Map.Entry<Mob, Integer> entry : enemyHits.entrySet()) {
			if (entry.getKey() != null && entry.getKey().isPlayer() && entry.getKey().getPlayer().getFamiliar() != null
					&& enemyHits.containsKey(entry.getKey().getPlayer().getFamiliar()))
				familiarDamage = enemyHits.get(entry.getKey().getFamiliar());
			if (entry.getValue() + familiarDamage > mostDamage) {
				if (entry.getKey() != null && (!entry.getKey().isFamiliar() 
						|| (entry.getKey().isFamiliar() && !enemyHits.containsKey(entry.getKey().getFamiliar().getOwner())))) {
					killer = entry.getKey();
					mostDamage = entry.getValue() + familiarDamage;
				}
			}
			familiarDamage = 0;
		}
		return killer;
	}

	public void clearEnemyHits() {
		enemyHits.clear();
	}

	public void clearHits() {
		hits.clear();
	}

	public Map<Mob, Integer> getEnemyHits() {
		return enemyHits;
	}

}
