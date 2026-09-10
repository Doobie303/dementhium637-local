package org.dementhium.model.combat;

import org.dementhium.model.Mob;
import org.dementhium.model.misc.DamageManager.DamageType;
import org.dementhium.model.player.Equipment;
import org.dementhium.model.player.Player;
import org.dementhium.net.ActionSender;

/**
 * Represents a damage to hit.
 * @author Emperor
 * @author CjayII (aka Mystic Flow/Steven) <font size="2" color="red"><b>Did pretty much nothing</b></font>.
 */
public class Damage {
    private NPCCombatContext npcContext;
    private boolean instanceContext;
    private Object sourceDuel,victimDuel;
    private static Object duel(Mob p){return p!=null && p.getActivity() instanceof org.dementhium.content.activity.impl.DuelActivity?p.getActivity():null;}
    private long sourceRevision, victimRevision;
    private Damage captureInstanceContext(Mob source,Mob victim) {
        npcContext=new NPCCombatContext(source,victim);instanceContext=true;sourceDuel=duel(source);victimDuel=duel(victim);
        sourceRevision=source==null ? 0 : source.getInstanceRevision();
        victimRevision=victim.getInstanceRevision();
        return this;
    }
    public boolean isInstanceContextCurrent(Mob source,Mob victim) {
        return (npcContext==null||npcContext.isCurrent()) && (!instanceContext || (sourceDuel==duel(source) && victimDuel==duel(victim) && sourceRevision==(source==null ? 0 : source.getInstanceRevision())
                && victimRevision==victim.getInstanceRevision()));
    }
	
	/**
	 * The hit to be dealt to the opponent.
	 */
	private int hit;
    private boolean resolved, experienceAwarded;
    private boolean reflectionDelivered;
    public boolean claimReflection(){if(!resolved || reflectionDelivered)return false;reflectionDelivered=true;return true;}
    public boolean isReflectionSourceCurrent(){return npcContext==null || npcContext.isSourceCurrent();}
    private java.util.function.IntConsumer capturedExperience;
    private org.dementhium.model.misc.DamageManager.DamageType experienceType;
    public java.util.function.IntConsumer experience(Player player, org.dementhium.model.misc.DamageManager.DamageType type) {
        return capturedExperience != null && experienceType == type ? capturedExperience : CombatUtils.experienceAtLaunch(player,type);
    }
    private int shieldInput = -1;
    private double protectionMultiplier = 1;
    private boolean staffReduction;
    public Damage withShieldInput(int raw, double protection, boolean staff) {
        shieldInput = raw; protectionMultiplier = protection; staffReduction = staff; return this;
    }
    public int applySpiritShield(Player victim) {
        int shield = victim.getEquipment().getSlot(Equipment.SLOT_SHIELD);
        if (shield != 13740 && shield != 13742) return hit;
        if (shieldInput < 0) return SpiritShield.reduce(victim, hit);
        int reduced = SpiritShield.reduce(victim, shieldInput);
        int protectedHit = (int)(reduced * protectionMultiplier);
        return staffReduction ? protectedHit / 2 : protectedHit;
    }
    public boolean isResolved() { return resolved; }
    public boolean claimExperience() { if (experienceAwarded) return false; experienceAwarded = true; return true; }
    private java.util.function.IntConsumer impactEffect;
    private Runnable contactEffect;
    /** Successful accuracy, including zero damage; DamageManager checks eligibility. */
    public Damage onContact(Runnable effect) {
        Runnable previous = contactEffect;
        contactEffect = previous == null ? effect : () -> { previous.run(); effect.run(); };
        return this;
    }
    public Damage onImpact(java.util.function.IntConsumer effect) {
        this.impactEffect = impactEffect == null ? effect : impactEffect.andThen(effect);
        return this;
    }
	
	/**
	 * The amount of soaked damage.
	 */
	private int soaked;
	
	/**
	 * The deflected hit to be dealt to the attacker.
	 */
	private int deflected;
	
	/**
	 * The venged hit to be dealt to the attacker.
	 */
	private int venged;
	
	/**
	 * The recoiled hit to be dealt to the attacker.
	 */
	private int recoiled;
	
	/**
	 * The maximum hit of the mob.
	 */
	private int maximum = -1;
	
	/**
	 * Constructs a new {@code Damage} {@code Object}.
	 * @param hit The standard hit.
	 */
	public Damage(int hit) {
		this.hit = hit;
	}
	
	/**
	 * Returns the damage instance of this hit, keeping damage modifiers in mind.
	 * @param source The attacking entity.
	 * @param victim The entity being hit.
	 * @param hit The hit.
	 * @return The damage.
	 */
	public static Damage getDamage(Mob source, Mob victim, CombatType type, int hit) {
        return getDamage(source, victim, type, hit, false);
    }
    public static Damage getDamage(Mob source, Mob victim, CombatType type, int hit, boolean bypassProtection) {
        if (hit < 0) return new Damage(-1); // Magic uses -1 for a splash; zero is a successful zero-damage hit.
        Damage damage = (source == null ? new Damage(hit) : bypassProtection && victim.isPlayer()
                ? victim.getPlayer().updateHit(source, hit, type, bypassProtection)
                : victim instanceof org.dementhium.model.npc.impl.TormentedDemon ? ((org.dementhium.model.npc.impl.TormentedDemon)victim).updateHit(source,hit,type,bypassProtection) : victim.updateHit(source, hit, type)).captureInstanceContext(source,victim);
        if(source != null && source.isPlayer() && (type == CombatType.MELEE || type == CombatType.RANGE)) {
            damage.experienceType=type.getDamageType();
            damage.capturedExperience=CombatUtils.experienceAtLaunch(source.getPlayer(),damage.experienceType);
        }
        CombatStatus.weaponPoisonOnImpact(damage, source, victim, type);
        BarrowsEquipmentEffects.attach(damage, source, victim, type);
        return damage;
    }

    /** Reflection is calculated only after absorption and actual HP loss are known. */
    public void finishEffects(Mob source, Mob victim, int applied) {
        finishEffects(source, victim, applied, true);
    }
    public void finishEffects(Mob source, Mob victim, int applied, boolean contactAllowed) {
        if (resolved) return;
        resolved = true;
        Runnable contact = contactEffect;
        contactEffect = null;
        if (contact != null && contactAllowed) contact.run();
        java.util.function.IntConsumer effect = impactEffect;
        impactEffect = null;
        if (effect != null && applied > 0) effect.accept(applied);
        boolean liveSource=isReflectionSourceCurrent();
        setRecoiled(source == null || applied <= 0 || !liveSource ? 0 : getRecoilDamage(source, victim, applied));
        setVenged(source == null || applied <= 0 || !liveSource ? 0 : getVengDamage(victim, applied));
        if(!liveSource)setDeflected(0);
    }

    /**
	 * Gets the recoiled damage.
	 * @param victim The victim.
	 * @param hit The hit.
	 * @return The recoiled damage if the victim is recoiling, or -1 if not.
	 */
	private static int getRecoilDamage(Mob source, Mob victim, int hit) {
		int recoiled = (int) Math.floor(hit * 0.1);
		if (recoiled < 1) {
			return -1;
		}
		if (victim.isPlayer()) {
			Player p = victim.getPlayer();
			if (p.getEquipment().getSlot(Equipment.SLOT_RING) != 2550) {
				return -1;
			}
			if (recoiled > p.getSettings().getRecoilDamage()) {
				recoiled = p.getSettings().getRecoilDamage();
			}
			int hitpoints = source.getHitPoints();
			if (recoiled > hitpoints) {
				recoiled = hitpoints;
			}
			return recoiled;
		}
		//TODO: NPC recoiling.
		return -1;
	}

    /** Debit only the recoil actually delivered, after Deflect and overkill. */
    static void consumeRecoil(Mob victim,int applied) {
        if(!victim.isPlayer() || applied<=0)return;
        Player p=victim.getPlayer();
        p.getSettings().setRecoilDamage(Math.max(0,p.getSettings().getRecoilDamage()-applied));
        if(p.getSettings().getRecoilDamage()<1){
            ActionSender.sendMessage(p,"Your ring of recoil has turned to dust.");
            p.getEquipment().set(Equipment.SLOT_RING,null);p.getSettings().setRecoilDamage(400);
        }
    }

	/**
	 * Gets the vengeance damage.
	 * @param victim The victim.
	 * @param hit The hit.
	 * @return The vengeance damage if the spell is casted and the victim is a player, or -1 if not.
	 */
	private static int getVengDamage(Mob victim, int hit) {
		if (victim.isNPC()) {
			return -1;
		}
		if (hit > 0 && victim.getAttribute("vengeance", false)) {
			return (int) (hit * 0.75);
		}
		return -1;
	}
	
	/**
	 * Gets the soaked damage amount.
	 * @param victim The victim.
	 * @param hit The hit to soak.
	 * @param type The combat type used.
	 * @return The amount of absorbed damage.
	 */
    public static int calculateSoaked(Mob victim, int hit, CombatType type) {
        if (victim == null || !victim.isPlayer() || type == null || hit <= 200
                || type.getAbsorbtion() < 0 || type.getAbsorbtion() > 2) return 0;
        int percent = Math.max(0,Math.min(100,victim.getPlayer().getBonuses().getAbsorptionBonus(type.getAbsorbtion())));
        return (int)((long)(hit - 200) * percent / 100);
    }

    /**
	 * @param hit the hit
	 */
	public void setHit(int hit) {
        shieldInput = -1;
		this.hit = hit;
	}
	
	/**
	 * @return the hit
	 */
	public int getHit() {
		return hit;
	}

	/**
	 * @param deflected the deflected to set
	 * @return The Damage instance.
	 */
	public Damage setDeflected(int deflected) {
		this.deflected = deflected;
		return this;
	}

	/**
	 * @return the deflected
	 */
	public int getDeflected() {
		return deflected;
	}

	/**
	 * @param venged the venged to set
	 */
	public void setVenged(int venged) {
		this.venged = venged;
	}

	/**
	 * @return the venged
	 */
	public int getVenged() {
		return venged;
	}

	/**
	 * @param recoiled the recoiled to set
	 */
	public void setRecoiled(int recoiled) {
		this.recoiled = recoiled;
	}

	/**
	 * @return the recoiled
	 */
	public int getRecoiled() {
		return recoiled;
	}

	/**
	 * @return the soaked
	 */
	public int getSoaked() {
		return soaked;
	}

	/**
	 * @param soaked the soaked to set
	 */
	public void setSoaked(int soaked) {
		this.soaked = soaked;
	}

	/**
	 * @return the maximum
	 */
	public int getMaximum() {
		return maximum;
	}

	/**
	 * @param maximum the maximum to set
	 */
	public void setMaximum(int maximum) {
		this.maximum = maximum;
	}
	
}
