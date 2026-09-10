package org.dementhium.model.combat.impl.npc;

import org.dementhium.content.minigames.FightCaves;
import org.dementhium.model.Mob;
import org.dementhium.model.Projectile;
import org.dementhium.model.combat.CombatAction;
import org.dementhium.model.combat.CombatMovement;
import org.dementhium.model.combat.CombatType;
import org.dementhium.model.combat.Damage;
import org.dementhium.model.combat.MagicFormulae;
import org.dementhium.model.combat.MeleeFormulae;
import org.dementhium.model.combat.RangeFormulae;
import org.dementhium.model.misc.DamageManager.DamageType;
import org.dementhium.model.misc.ProjectileManager;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.npc.impl.FightCaveNPC;

/** Combat rules for the pre-EoC Fight Caves creatures. */
public final class FightCaveNPCAction extends CombatAction {

	private static final int TOK_XIL_PROJECTILE = 443;
	private static final int KET_ZEK_PROJECTILE = 445;
	private static final int KET_ZEK_IMPACT = 446;

	private final FightCaveNPC npc;
	private CombatType type = CombatType.MELEE;
	private int maximum;
	private int rolledHit;
	private int attacks;

	public FightCaveNPCAction(FightCaveNPC npc) {
		super(false);
		this.npc = npc;
		if (isXil()) {
			type = CombatType.RANGE;
		} else if (isZek()) {
			type = CombatType.MAGIC;
		}
	}

	@Override
	public boolean commenceSession() {
		Mob victim = interaction.getVictim();
		if (victim == null || victim.isDead()) {
			return false;
		}
		npc.getCombatExecutor().setTicks(4);
		npc.turnTo(victim, false);

		if (isMejKot() && CombatMovement.canMelee(npc, victim)) {
			attacks++;
			if ((attacks & 1) == 0) {
				NPC target = FightCaves.getMejKotHealTarget(npc);
				if (target != null) {
					npc.animate(9254);
					npc.graphics(444);
					target.heal(50 + npc.getRandom().nextInt(51));
					return false;
				}
			}
		}

		type = chooseType(victim);
		maximum = getMaximum(type);
		if (type == CombatType.RANGE) {
			rolledHit = RangeFormulae.getDamage(npc, victim, 1.0, maximum, 1.0);
		} else if (type == CombatType.MAGIC) {
			rolledHit = MagicFormulae.getDamage(npc, victim, 1.0, maximum, 1.0);
		} else {
			rolledHit = MeleeFormulae.getDamage(npc, victim, 1.0, maximum, 1.0);
		}
		interaction.setDamage(new Damage(Math.max(0, rolledHit)));
		interaction.getDamage().setMaximum(maximum);
		npc.animate(npc.getAttackAnimation());

		if (type == CombatType.RANGE) {
			ProjectileManager.sendProjectile(Projectile.ranged(npc, victim,
					TOK_XIL_PROJECTILE, 42, 36, 41, 5));
		} else if (type == CombatType.MAGIC) {
			ProjectileManager.sendProjectile(Projectile.magic(npc, victim,
					KET_ZEK_PROJECTILE, 43, 36, 72, 5));
		}

		// Tz-Kih drains one Prayer point on every attack, including a zero hit.
		if (isKih() && victim.isPlayer()) {
			victim.getPlayer().getSkills().drainPray(1);
		}
		interaction.setTicks(type == CombatType.MELEE ? 0 : 2);
		return true;
	}

	@Override
	public boolean executeSession() {
		interaction.setTicks(interaction.getTicks() - 1);
		return interaction.getTicks() < 1;
	}

	@Override
	public boolean endSession() {
		Mob victim = interaction.getVictim();
		if (victim == null || victim.isDead() || interaction.getDamage() == null) {
			return true;
		}
		Damage damage = Damage.getDamage(npc, victim, type, Math.max(0, rolledHit));
		damage.setMaximum(maximum);
		interaction.setDamage(damage);
		if (type == CombatType.MAGIC) {
			victim.graphics(KET_ZEK_IMPACT);
		}
		victim.animate(victim.getDefenceAnimation());
		victim.getDamageManager().damage(npc, interaction.getDamage(), type.getDamageType());



		victim.retaliate(npc);
		return true;
	}

	@Override
	public CombatType getCombatType() {
		Mob victim = npc.getCombatExecutor().getVictim();
		if (victim != null && (isXil() || isZek())) {
			return chooseType(victim);
		}
		return type;
	}

	private CombatType chooseType(Mob victim) {
		if (isXil()) {
			return CombatMovement.canMelee(npc, victim) ? CombatType.MELEE : CombatType.RANGE;
		}
		if (isZek()) {
			return CombatMovement.canMelee(npc, victim) ? CombatType.MELEE : CombatType.MAGIC;
		}
		return CombatType.MELEE;
	}

	private int getMaximum(CombatType style) {
		switch (npc.getId()) {
		case 2734:
		case 2735:
			return 40;
		case 2736:
		case 2737:
			return 70;
		case 2738:
			return 40;
		case 2739:
		case 2740:
			return style == CombatType.RANGE ? 140 : 130;
		case 2741:
		case 2742:
			return 250;
		case 2743:
		case 2744:
			return style == CombatType.MAGIC ? 520 : 550;
		case 2746:
			return 140;
		default:
			return 40;
		}
	}

	private boolean isKih() {
		return npc.getId() == 2734 || npc.getId() == 2735;
	}

	private boolean isXil() {
		return npc.getId() == 2739 || npc.getId() == 2740;
	}

	private boolean isMejKot() {
		return npc.getId() == 2741 || npc.getId() == 2742;
	}

	private boolean isZek() {
		return npc.getId() == 2743 || npc.getId() == 2744;
	}
}
