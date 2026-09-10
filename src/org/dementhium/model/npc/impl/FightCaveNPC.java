package org.dementhium.model.npc.impl;

import org.dementhium.model.Mob;
import org.dementhium.model.combat.CombatAction;
import org.dementhium.model.combat.CombatType;
import org.dementhium.model.combat.Damage;
import org.dementhium.model.combat.impl.npc.FightCaveNPCAction;
import org.dementhium.model.misc.DamageManager.DamageType;
import org.dementhium.model.npc.NPC;

/**
 * A Fight Caves monster using the animations from the 637 cache.
 *
 * The legacy NPC definition pack has no entries for the wave monsters, so a
 * plain NPC falls back to the humanoid attack animation (422). Keeping the
 * overrides on this class prevents cave-specific animation data from changing
 * any unrelated NPC spawned elsewhere.
 */
public class FightCaveNPC extends NPC {
	private final CombatAction combatAction;

	public FightCaveNPC(int id) {
		super(id);
		setHp(getCaveHitpoints(id));
		combatAction = new FightCaveNPCAction(this);
	}

	private static int getCaveHitpoints(int id) {
		switch (id) {
		case 2734: // Tz-Kih
		case 2735: // Tz-Kih spawn variant
		case 2738: // Tz-Kek (split)
			return 100;
		case 2736: // Tz-Kek
		case 2737: // Tz-Kek spawn variant
			return 200;
		case 2739: // Tok-Xil
		case 2740: // Tok-Xil spawn variant
			return 400;
		case 2741: // Yt-MejKot
		case 2742: // Yt-MejKot spawn variant
			return 800;
		case 2743: // Ket-Zek
		case 2744: // Ket-Zek spawn variant
			return 1600;
		case 2746: // Yt-HurKot
			return 600;
		default:
			return 100;
		}
	}

	@Override
	public int getAttackDelay() {
		return 4;
	}

	@Override
	public CombatAction getCombatAction() {
		return combatAction;
	}

	@Override
	public Damage updateHit(Mob source, int hit, CombatType type) {
		Damage damage = super.updateHit(source, hit, type);
		int id = getId();
		if ((id == 2736 || id == 2737 || id == 2738) && source != null
				&& source.isPlayer() && type == CombatType.MELEE && damage.getHit() > 0) {
			source.getDamageManager().damage(this, 10, 10, DamageType.RED_DAMAGE);
		}
		return damage;
	}

	@Override
	public int getMaxHp() {
		return getCaveHitpoints(getId());
	}

	@Override
	public int getMaximumHitPoints() {
		return getCaveHitpoints(getId());
	}

	@Override
	public void heal(int amount) {
		setHp(Math.min(getMaximumHitPoints(), getHitPoints() + Math.max(0, amount)));
	}

	@Override
	public int getAttackAnimation() {
		switch (getId()) {
		case 2734: // Tz-Kih
		case 2735:
			return 9232;
		case 2736: // Tz-Kek
		case 2737:
		case 2738: // Tz-Kek (split)
			return 9233;
		case 2739: // Tok-Xil ranged attack
		case 2740:
			return 9243;
		case 2741: // Yt-MejKot
		case 2742:
			return 9246;
		case 2743: // Ket-Zek magic attack
		case 2744:
			return 9266;
		case 2746: // Yt-HurKot
			return 9252;
		default:
			return super.getAttackAnimation();
		}
	}

	@Override
	public int getDefenceAnimation() {
		switch (getId()) {
		case 2734:
		case 2735:
			return 9231;
		case 2736:
		case 2737:
		case 2738:
			return 9235;
		case 2739:
		case 2740:
			return 9242;
		case 2741:
		case 2742:
			return 9248;
		case 2743:
		case 2744:
			return 9268;
		case 2746:
			return 9253;
		default:
			return super.getDefenceAnimation();
		}
	}

	@Override
	public int getDeathAnimation() {
		switch (getId()) {
		case 2734: // Tz-Kih
		case 2735:
			return 9230;
		case 2736: // Tz-Kek
		case 2737:
		case 2738: // Tz-Kek (split)
			return 9234;
		case 2739: // Tok-Xil
		case 2740:
			return 9239;
		case 2741: // Yt-MejKot
		case 2742:
			return 9247;
		case 2743: // Ket-Zek
		case 2744:
			return 9269;
		case 2746: // Yt-HurKot
			return 9257;
		default:
			return super.getDeathAnimation();
		}
	}

}
