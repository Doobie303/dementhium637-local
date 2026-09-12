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
	private static final int GODMODE_HIT = 750;

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
			return GODMODE_HIT;
		}
		return damage;
	}

	/**
	 * Applies the launch-era Nex hit caps. The ::god command deliberately bypasses
	 * these caps so that it remains useful as an owner/developer testing tool.
	 */
	private int applyNexHitCap(Mob attacker, int damage, DamageType type) {
		if (damage <= 0 || type == DamageType.HEAL || type == DamageType.MISS
				|| mob == null || !mob.isNPC()) {
			return damage;
		}
		if (attacker != null && attacker.isPlayer()
				&& Boolean.TRUE.equals(attacker.getAttribute("godmode"))) {
			return damage;
		}
		int id = mob.getNPC().getId();
		if (id >= Nex.DEFAULT_NEX_ID && id <= Nex.WRATH_NEX) {
			return Math.min(500, damage);
		}
		if (id >= Nex.FUMUS && id <= Nex.GLACIES) {
			return Math.min(600, damage);
		}
		return damage;
	}

	private void applyNexDeflect(Mob attacker, int damage, DamageType type) {
		if (damage <= 0 || type != DamageType.MELEE || attacker == null
				|| !attacker.isPlayer() || mob == null || !mob.isNPC()) {
			return;
		}
		NPC npc = mob.getNPC();
		if (npc.isNex() && npc.getId() == Nex.MELEE_DEFLECT_NEX) {
			int reflected = Math.min(300, Math.max(1, damage / 2));
			attacker.getDamageManager().damage(npc, reflected, 300, DamageType.DEFLECT);
		}
	}

	private void applyNexMinionEffect(Mob attacker, int damage) {
		if (damage <= 0 || attacker == null || !attacker.isNPC()
				|| mob == null || !mob.isPlayer()) {
			return;
		}
		int id = attacker.getNPC().getId();
		if (id == Nex.FUMUS) {
			mob.graphics(471);
			mob.getPoisonManager().poison(attacker, 60);
		} else if (id == Nex.UMBRA) {
			mob.graphics(383);
		} else if (id == Nex.CRUOR || id == 13458) {
			mob.graphics(376);
			Nex nex = NexAreaEvent.getNexAreaEvent().getNex();
			if (nex != null) {
				nex.heal(Math.max(1, damage / 2));
			}
		} else if (id == Nex.GLACIES
				&& mob.getAttribute("freezeImmunity", -1) < World.getTicks()) {
			mob.graphics(362);
			mob.getWalkingQueue().reset();
			mob.setAttribute("freezeTime", World.getTicks() + 8);
			mob.setAttribute("freezeImmunity", World.getTicks() + 13);
			mob.getPlayer().sendMessage("Glacies freezes you in place.");
		}
	}


	public LinkedList<DamageHit> getHits() {
		return hits;
	}

	public Mob getMob() {
		return mob;
	}

	public void damage(Mob attacker, int amount, int maximum, DamageType type) {
        apply(attacker, amount, maximum, type, 0, null);
    }

    /** delay is the client's hitsplat delay, not a server tick delay. */
    public void damage(Mob attacker, int amount, int maximum, DamageType type, int delay) {
        apply(attacker, amount, maximum, type, delay, null);
    }

    public void miscDamage(int amount, DamageType type) {
        apply(null, amount, -1, type, 0, null);
    }

    public void miscDamage(int amount, DamageType type, int delay) {
        apply(null, amount, -1, type, delay, null);
    }

    public void damage(Mob source, Damage damage, DamageType type) {
        damage(source, damage, type, 0);
    }

    public void damage(Mob source, Damage damage, DamageType type, int delay) {
        if (damage == null || damage.isResolved() || !damage.isInstanceContextCurrent(source,mob)) return;
        apply(source, damage.getHit(), damage.getMaximum(), type, delay, damage);
    }

    /** Inputs have already received any attack-specific prayer/shield reduction.
     * Never infer an incoming style from the attacker's current weapon or action.
     */
    private void apply(Mob source, int amount, int maximum, DamageType type, int delay, Damage result) {
        if (source!=null && !org.dementhium.model.instance.InstanceAccess.canInteract(source,mob)) return;
        if(source!=null && source.isPlayer() && mob.isPlayer()) {
            org.dementhium.content.activity.impl.DuelActivity duel=source.getActivity() instanceof org.dementhium.content.activity.impl.DuelActivity
                ? (org.dementhium.content.activity.impl.DuelActivity)source.getActivity()
                : mob.getActivity() instanceof org.dementhium.content.activity.impl.DuelActivity ? (org.dementhium.content.activity.impl.DuelActivity)mob.getActivity() : null;
            if(duel!=null && !duel.isCombatActivity(source,mob,false))return;
        }
        if (type == null || type == DamageType.SOAK) return; // Soak is a partner splat only.
        int before = mob.getHitPoints();
        int incoming = Math.max(0, amount);
        boolean immune = mob.isPlayer() && type != DamageType.HEAL
                && (Boolean.TRUE.equals(mob.getAttribute("godmode"))
                || mob.getAttribute("hitImmunity", -1) > World.getTicks());
        amount = immune ? 0 : applyGodHits(source, incoming, type);
        if (mob.isNPC() && type != DamageType.HEAL) {
            NPC npc = mob.getNPC();
            Nex nex = npc instanceof Nex ? (Nex)npc
                    : npc.isNex() ? NexAreaEvent.getNexAreaEvent().getNex() : null;
            if (nexIsShielded(npc) || nex != null && !nex.isAttackable()) amount = 0;
            else if (nex != null && nex.isSiphonMode()) type = DamageType.HEAL;
        }
        amount = applyNexHitCap(source, amount, type);
        if (mob instanceof org.dementhium.model.npc.impl.SlayerNPC)
            amount = ((org.dementhium.model.npc.impl.SlayerNPC)mob).filterIncomingDamage(source, result, amount, type);
        DamageHit hit = new DamageHit();
        hit.victim = mob; hit.attacker = source; hit.delay = Math.max(0, delay);
        int beforeShield = amount;
        if (mob.isPlayer() && !immune && (type == DamageType.MELEE || type == DamageType.MAGE || type == DamageType.RANGE))
            amount = result != null && source != null && !source.getAttribute("godmode", false)
                    ? result.applySpiritShield(mob.getPlayer()) : org.dementhium.model.combat.SpiritShield.reduce(mob.getPlayer(), amount);
        updateDamageAttributes(hit, type, amount);
        int applied = type == DamageType.HEAL ? 0 : Math.max(0, before - mob.getHitPoints());
        if (result != null) {
            result.setHit(applied);
            result.setSoaked(hit.partner == null ? 0 : hit.partner.damage);
            // A positive Nex hit may itself activate the next minion shield.
            // That new shield blocks later hits, not this hit's earned contact.
            result.finishEffects(source, mob, applied, !immune && type != DamageType.HEAL
                    && (applied > 0 || !(mob.isNPC() && nexIsShielded(mob.getNPC()))));
            if (immune || type == DamageType.HEAL) result.setDeflected(0);
        }
        hit.isMax = maximum > 0 && amount >= maximum && applied > 0;
        hits.add(hit);
        if (source != null && source != mob && source.isPlayer() && applied > 0
                && (type == DamageType.MELEE || type == DamageType.RANGE || type == DamageType.MAGE))
            source.getPlayer().applyOffensivePrayerEffects(mob, applied);
        applyNexDeflect(source, applied, type);
        applyNexMinionEffect(source, applied);
        if(result!=null)org.dementhium.model.combat.CombatReflection.deliver(source,mob,result);
        if (mob.isPlayer() && mob.getAttribute("combatDebug", false))
            mob.getPlayer().sendMessage("[Hit] " + type + " incoming=" + incoming
                    + " adjusted=" + amount + " shield=" + (beforeShield - amount) + " soaked=" + (hit.partner == null ? 0 : hit.partner.damage)
                    + " HP lost=" + applied + " HP=" + before + "->" + mob.getHitPoints());
        if (source != null && source != mob && source.isPlayer() && source.getAttribute("combatDebug", false))
            source.getPlayer().sendMessage("[Hit dealt] " + type + " incoming=" + incoming
                    + " soaked=" + (hit.partner == null ? 0 : hit.partner.damage) + " HP lost=" + applied);
    }

    public boolean soak(DamageHit hit, Mob attacker, int damage) {
        if (hit == null || hit.isPartner || !mob.isPlayer()) return false;
        CombatType style = hit.type == DamageType.MELEE ? CombatType.MELEE
                : hit.type == DamageType.RANGE ? CombatType.RANGE
                : hit.type == DamageType.MAGE ? CombatType.MAGIC : null;
        int absorbed = Damage.calculateSoaked(mob, damage, style);
        if (absorbed <= 0) return false;
        DamageHit partner = new DamageHit();
        partner.victim = mob; partner.attacker = attacker; partner.isPartner = true;
        partner.damage = absorbed; partner.type = DamageType.SOAK; partner.delay = hit.delay;
        hit.partner = partner; hit.damage = damage - absorbed;
        return true;
    }

    public void updateDamageAttributes(DamageHit hit, DamageType type, int amount) {
        int before = mob.getHitPoints();
        hit.type = type;
        amount = Math.max(0, amount);
        if (type != DamageType.HEAL) {
            if (mob.isPlayer() && (Boolean.TRUE.equals(mob.getAttribute("godmode"))
                    || mob.getAttribute("hitImmunity", -1) > World.getTicks())
                    || mob.isNPC() && nexIsShielded(mob.getNPC())) amount = 0;
            if (!soak(hit, hit.attacker, amount)) hit.damage = amount;
            hit.damage = Math.min(Math.max(0,before), hit.damage);
            // Both NPC and player death callbacks select credit synchronously.
            // The final reduced, HP-clamped amount must be recorded before HP mutation.
            if (hit.attacker != null && hit.attacker != mob && hit.damage > 0) addEnemyHit(hit.attacker,hit.damage);
            if (mob.isPlayer()) {
                mob.getPlayer().getSkills().hit(hit.damage);
            }
            else mob.getNPC().hit(hit.damage);
            hit.damage = Math.max(0, before - mob.getHitPoints());
            if (hit.damage == 0) hit.type = DamageType.MISS;
        } else {
            if (mob.isPlayer()) mob.getPlayer().getSkills().heal(amount);
            else mob.getNPC().heal(amount);
            hit.damage = Math.max(0, mob.getHitPoints() - before);
        }
        int maximum = Math.max(1, mob.getMaximumHitPoints());
        hit.currentHealth = Math.max(0,Math.min(maximum,mob.getHitPoints())) * 255 / maximum;
        if (type != DamageType.HEAL && hit.damage > 0) {
            if (hit.attacker != null && hit.attacker.isPlayer()) DegradingHandler.process(hit.attacker.getPlayer());
            if (mob.isPlayer()) DegradingHandler.process(mob.getPlayer());
        }
    }

    private int lastCreditTick = Integer.MIN_VALUE;
    private void expireCredit() {
        if (mob.isPlayer() && (long)World.getTicks()-lastCreditTick
                > org.dementhium.content.misc.PvpSystem.CREDIT_IDLE_TICKS) enemyHits.clear();
    }
    public void addEnemyHit(Mob enemy, int damage) {
        if (enemy==null || enemy==mob) return;
        expireCredit();
		if (damage > 0) {
            lastCreditTick=World.getTicks();
			if (!enemyHits.containsKey(enemy)) {
				enemyHits.put(enemy, damage);
			} else {
				enemyHits.put(enemy, (int)Math.min(Integer.MAX_VALUE,(long)enemyHits.get(enemy) + damage));
			}
		}
	}

	public Mob getKiller() {
        expireCredit();
		Mob killer = null;
		long mostDamage = 0;
		long familiarDamage = 0;
		for (Map.Entry<Mob, Integer> entry : enemyHits.entrySet()) {
			if (entry.getKey() != null && entry.getKey().isPlayer() && entry.getKey().getPlayer().getFamiliar() != null
					&& enemyHits.containsKey(entry.getKey().getPlayer().getFamiliar()))
				familiarDamage = enemyHits.get(entry.getKey().getPlayer().getFamiliar());
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
