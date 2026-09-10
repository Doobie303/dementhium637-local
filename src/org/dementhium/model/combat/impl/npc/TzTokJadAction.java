package org.dementhium.model.combat.impl.npc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dementhium.content.minigames.FightCaves;
import org.dementhium.model.Location;
import org.dementhium.model.Mob;
import org.dementhium.model.Projectile;
import org.dementhium.model.World;
import org.dementhium.model.instance.InstanceAccess;
import org.dementhium.model.combat.CombatAction;
import org.dementhium.model.combat.CombatMovement;
import org.dementhium.model.combat.CombatType;
import org.dementhium.model.combat.Damage;
import org.dementhium.model.combat.MagicFormulae;
import org.dementhium.model.combat.MeleeFormulae;
import org.dementhium.model.combat.RangeFormulae;
import org.dementhium.model.combat.NPCCombatContext;
import org.dementhium.model.map.path.ProjectilePathFinder;
import org.dementhium.model.misc.DamageManager.DamageType;
import org.dementhium.model.misc.ProjectileManager;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.npc.impl.FightCaveNPC;
import org.dementhium.model.npc.impl.TzTokJad;
import org.dementhium.tickable.Tick;

/** 2011-era TzTok-Jad combat, tells, protection prayers and healers. */
public final class TzTokJadAction extends CombatAction {

	private final TzTokJad jad;
	private final TzTokJadAction encounter;
	private final List<NPC> healers = new ArrayList<NPC>();
	private boolean healersSummoned;
	private boolean healersRestoredJad;
	private CombatType type = CombatType.MAGIC;
	private int maximum;
	private int rolledHit;
	private boolean consumed;
	private long healerGeneration;

	public TzTokJadAction(TzTokJad jad) {
		this(jad, null);
	}

	private TzTokJadAction(TzTokJad jad, TzTokJadAction encounter) {
		super(false);
		this.jad = jad;
		this.encounter = encounter == null ? this : encounter;
		this.healerGeneration = jad.getCombatGeneration();
	}

	@Override public CombatAction newSession() { return new TzTokJadAction(jad, encounter); }

	@Override
	public boolean commenceSession() {
		final Mob victim = interaction.getVictim();
		if (victim == null || !interaction.isNPCContextCurrent()
				|| !ProjectilePathFinder.clearPath(jad.getLocation(), victim.getLocation())
				|| !ProjectilePathFinder.clearPath(victim.getLocation(), jad.getLocation())) {
			return false;
		}
		consumed = false;
		jad.getCombatExecutor().setTicks(8);
		jad.turnTo(victim, false);
		encounter.pruneHealers();
		if (jad.getHp() <= jad.getMaximumHitPoints() / 2
				&& (!encounter.healersSummoned || (encounter.healers.isEmpty() && encounter.healersRestoredJad))) {
			encounter.spawnHealers(victim);
		}

		if (CombatMovement.canMelee(jad, victim)) {
			type = CombatType.MELEE;
			maximum = 970;
			jad.animate(9277);
		} else if (jad.getRandom().nextBoolean()) {
			type = CombatType.RANGE;
			maximum = 970;
			jad.animate(9276);
			jad.graphics(1625);
			final NPCCombatContext tellContext = new NPCCombatContext(jad, victim);
			submitTask(jad, new Tick(2) {
				@Override
				public void execute() {
					stop();
					if (tellContext.isCurrent()) {
						victim.graphics(451);
					}
				}
			});
		} else {
			type = CombatType.MAGIC;
			maximum = 950;
			jad.animate(9300);
			jad.graphics(1626);
			ProjectileManager.sendProjectile(Projectile.magic(jad, victim, 1627, 43, 36, 72, 5));
		}

		if (type == CombatType.MELEE) {
			rolledHit = MeleeFormulae.getDamage(jad, victim, 1.0, maximum, 1.0);
		} else if (type == CombatType.RANGE) {
			rolledHit = RangeFormulae.getDamage(jad, victim, 1.0, maximum, 1.0);
		} else {
			rolledHit = MagicFormulae.getDamage(jad, victim, 1.0, maximum, 1.0);
		}
		interaction.setDamage(new Damage(rolledHit));
		interaction.getDamage().setMaximum(maximum);
		interaction.setTicks(type == CombatType.MELEE ? 0 : 4);
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
		if (consumed || victim == null || !interaction.isNPCContextCurrent() || interaction.getDamage() == null) {
			return true;
		}
		consumed = true;
		if (rolledHit < 0) { victim.graphics(85, 96 << 16); return true; }
		Damage damage = Damage.getDamage(jad, victim, type, rolledHit);
		damage.setMaximum(maximum);
		interaction.setDamage(damage);
		interaction.setDeflected(victim.isPlayer()
				&& victim.getPlayer().getPrayer().usingPrayer(1, type.getDeflectCurse()));
		if (interaction.isDeflected()) {
			victim.graphics(2230 - type.ordinal());
			victim.animate(12573);
		} else {
			victim.animate(victim.getDefenceAnimation());
		}
		victim.getDamageManager().damage(jad, interaction.getDamage(), type.getDamageType());



		victim.retaliate(jad);
		return true;
	}

	@Override
	public CombatType getCombatType() {
		// The reusable controller supplies pursuit reach; a queued session retains its tell.
		return encounter == this ? CombatType.MAGIC : type;
	}

	private void spawnHealers(final Mob player) {
		if (!FightCaves.isCaveOpponent(jad, player)) return;
		healersSummoned = true;
		healersRestoredJad = false;
		if (!player.isPlayer()) {
			return;
		}
		int[][] offsets = {{0, 6}, {-6, 0}, {0, -6}, {6, 0}};
		for (int[] offset : offsets) {
			Location loc = Location.locate(jad.getLocation().getX() + offset[0],
					jad.getLocation().getY() + offset[1], jad.getLocation().getZ());
			final NPC healer = FightCaves.spawnCaveNpc(FightCaves.YT_HURKOT,
					loc, player.getPlayer(), true);
			if (healer == null) {
				continue;
			}
			healer.getCombatExecutor().reset();
			healers.add(healer);
			healer.turnTo(jad, false);
			healer.getMask().setInteractingEntity(jad, false);
			healer.requestClippedWalk(jad.getLocation().getX() - 1, jad.getLocation().getY());
			final long healerLife = healer.getCombatGeneration();
			final long jadLife = jad.getCombatGeneration();
			final NPCCombatContext playerContext = new NPCCombatContext(jad, player);
			submitTask(healer, new Tick(4) {
				@Override
				public void execute() {
					if (healerLife != healer.getCombatGeneration() || jadLife != jad.getCombatGeneration()
							|| healer.isDead() || healer.isHidden() || !InstanceAccess.canInteract(healer, jad)
							|| !playerContext.isCurrent()) {
						stop();
						return;
					}
					Mob target = healer.getCombatExecutor().getVictim();
					boolean distracted = target == player && !player.isDead()
							&& healer.getLocation().distance(player.getLocation()) <= 10;
					if (distracted) {
						return;
					}
					if (target == player) {
						healer.getCombatExecutor().reset();
					}
					healer.turnTo(jad, false);
					healer.getMask().setInteractingEntity(jad, false);
					if (healer.getLocation().distance(jad.getLocation()) > 5) {
						healer.requestClippedWalk(jad.getLocation().getX() - 1, jad.getLocation().getY());
						return;
					}
					healer.animate(9254);
					healer.graphics(444);
					jad.heal(50);
					if (jad.getHp() >= jad.getMaximumHitPoints()) {
						healersRestoredJad = true;
					}
				}
			});
		}
	}

	private void pruneHealers() {
		if (healerGeneration != jad.getCombatGeneration()) {
			for (NPC healer : healers) {
				if (healer.getOwningInstance() != null) healer.getOwningInstance().removeNpc(healer);
				else healer.destroy();
			}
			healers.clear();
			healersSummoned = false;
			healersRestoredJad = false;
			healerGeneration = jad.getCombatGeneration();
		}
		Iterator<NPC> iterator = healers.iterator();
		while (iterator.hasNext()) {
			NPC healer = iterator.next();
			if (healer == null || healer.isDead() || healer.isHidden()) {
				iterator.remove();
			}
		}
	}

	private void submitTask(NPC owner, Tick task) {
		if (owner.getOwningInstance() != null) owner.getOwningInstance().submitNpcTask(owner, task);
		else World.getWorld().submit(task);
	}
}
