package org.dementhium.model.combat.impl.npc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dementhium.model.Projectile;
import org.dementhium.model.combat.CombatAction;
import org.dementhium.model.combat.CombatMovement;
import org.dementhium.model.combat.CombatType;
import org.dementhium.model.combat.Damage;
import org.dementhium.model.combat.MagicFormulae;
import org.dementhium.model.combat.MeleeFormulae;
import org.dementhium.model.combat.RangeFormulae;
import org.dementhium.model.mask.Animation;
import org.dementhium.model.mask.Graphic;
import org.dementhium.model.misc.ProjectileManager;
import org.dementhium.model.misc.DamageManager.DamageType;
import org.dementhium.model.npc.impl.TzTokJad;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.World;
import org.dementhium.tickable.Tick;

/**
 * Handles TzTokJad's combat actions
 * @author Wildking72
 *
 */
public class TzTokJadAction extends CombatAction {


	private TzTokJad jad;
	private final List<NPC> healers = new ArrayList<NPC>();
	private boolean addedHealers = false;
	private boolean healersNeeded = false;
	
	/**
	 * The current combat type.
	 */
	private CombatType type = CombatType.MAGIC;
	
	/**
	 * The maximum hit
	 */
	private int maximum;
	
	/**
	 * Constructs a new {@code TzTokJad} {@code Object}.
	 * @param jad The jad NPC.
	 */
	public TzTokJadAction(TzTokJad jad) {
		super(false);
		this.jad = jad;
	}

	@Override
	public boolean commenceSession() {
	jad.getCombatExecutor().setTicks(7);
	interaction.getSource().turnTo(interaction.getVictim(), false);
	if (jad.getHp() <= 1250) {
		healersNeeded = true;
	}
		if (healersNeeded == true) {
			if (!addedHealers) {
				addedHealers = true;
				NPC healer1 = new NPC(2746, jad.getLocation().getX(), jad.getLocation().getY() + 2, jad.getLocation().getZ());
				NPC healer2 = new NPC(2746, healer1.getLocation().getX() - 1, healer1.getLocation().getY() + 1, healer1.getLocation().getZ());
				NPC healer3 = new NPC(2746, healer2.getLocation().getX() - 1, healer2.getLocation().getY() + 1, healer1.getLocation().getZ());
				NPC healer4 = new NPC(2746, healer3.getLocation().getX() + 1, healer1.getLocation().getY(), healer1.getLocation().getZ());
				healers.add(healer1);
				healers.add(healer2);
				healers.add(healer3);
				healers.add(healer4);
				for (NPC heal : healers) {
					World.getWorld().getNpcs().add(heal);
					heal.turnTo(jad, false);
					heal.getMask().setInteractingEntity(jad, false);
					heal.requestClippedWalk(jad.getLocation().getX() - 1, jad.getLocation().getY());
				}
			}
			Iterator<NPC> it = healers.iterator();
			while(it.hasNext()) {
				NPC heal = it.next();
				if (heal.isDead()) {
					World.getWorld().getNpcs().remove(heal);
					it.remove();
					continue;
				}
				if (jad.getHp() >= jad.getMaximumHitPoints() || jad.isDead() || heal.getMask().getInteractingEntity() != jad || CombatMovement.canMelee(heal, interaction.getVictim())) {
					heal.setAttribute("enemyIndex", interaction.getVictim().getPlayer().getIndex());
					heal.getMask().setInteractingEntity(interaction.getVictim(), false);
					heal.getCombatExecutor().setVictim(interaction.getVictim());
				} else {
					heal.turnTo(jad, false);
					heal.getMask().setInteractingEntity(jad, false);
					heal.requestClippedWalk(jad.getLocation().getX() - 1, jad.getLocation().getY());
					heal.animate(9254);
					heal.graphics(444);
					jad.setHp(jad.getHp() + 50);
				}
			}
		}
		if (CombatMovement.canMelee(jad, interaction.getVictim()) && jad.getRandom().nextInt(10) < 5) {
            type = CombatType.MELEE;
        } else {
        	type = CombatType.values()[1 + jad.getRandom().nextInt(2)];
        }
		int ticks = 1;
		switch (type) {
		case MELEE:
			jad.animate(9277);
			maximum = MeleeFormulae.getMeleeDamage(jad, 1.0);
			break;
		case RANGE:
			maximum = 990;
			jad.animate(9276);
			jad.graphics(1625);
			break;
		case MAGIC:
			maximum = 970;
			jad.animate(9300);
			jad.graphics(1626);
			ProjectileManager.sendProjectile(Projectile.magic(jad, interaction.getVictim(), 1627, 43, 36, 72, 5));
			break;
		}
		interaction.setTicks(ticks);
		return true;
	}

	@Override
	public boolean executeSession() {
		if (interaction.getTicks() < 2) {
			int current = type == CombatType.MELEE ? MeleeFormulae.getDamage(jad, interaction.getVictim()) : 
				type == CombatType.RANGE ? RangeFormulae.getDamage(jad, interaction.getVictim()) :
					MagicFormulae.getDamage(jad, interaction.getVictim(), 1.0, 1.0, 1.0);
	        interaction.setDamage(Damage.getDamage(jad, interaction.getVictim(), type, current));
	        interaction.getDamage().setMaximum(maximum);
		}
		interaction.setTicks(interaction.getTicks() - 1);
		return interaction.getTicks() < 1;
	}

	@Override
	public boolean endSession() {
		if (type == CombatType.RANGE) {
			World.getWorld().submit(new Tick(2) {
				@Override
				public void execute() {
					interaction.getVictim().graphics(451);
					this.stop();
				}
			});
			World.getWorld().submit(new Tick(4) {
				@Override
				public void execute() {
					if (interaction.getVictim().getPlayer().getPrayer().usingPrayer(1, 9 - type.ordinal())) {
						interaction.setDeflected(true);
						interaction.getVictim().graphics(2230 - type.ordinal());
						interaction.getVictim().animate(12573);
					} else {
						interaction.getVictim().animate(interaction.getVictim().getDefenceAnimation());
					}
					interaction.getVictim().getDamageManager().damage(interaction.getSource(), interaction.getDamage(), type.getDamageType());
					this.stop();
				}
			});
		} else if (type == CombatType.MAGIC) {
			World.getWorld().submit(new Tick(4) {
				@Override
				public void execute() {
					if (interaction.getVictim().getPlayer().getPrayer().usingPrayer(1, 9 - type.ordinal())) {
						interaction.setDeflected(true);
						interaction.getVictim().graphics(2230 - type.ordinal());
						interaction.getVictim().animate(12573);
					} else {
						interaction.getVictim().animate(interaction.getVictim().getDefenceAnimation());
					}
					interaction.getVictim().getDamageManager().damage(interaction.getSource(), interaction.getDamage(), type.getDamageType());
					this.stop();
				}
			});
		} else {
			if (interaction.getVictim().getPlayer().getPrayer().usingPrayer(1, 9 - type.ordinal())) {
						interaction.setDeflected(true);
						interaction.getVictim().graphics(2230 - type.ordinal());
						interaction.getVictim().animate(12573);
			} else {
				interaction.getVictim().animate(interaction.getVictim().getDefenceAnimation());
			}
			interaction.getVictim().getDamageManager().damage(interaction.getSource(), interaction.getDamage(), type.getDamageType());
		}
		if (interaction.getDamage().getVenged() > 0) {
			interaction.getVictim().submitVengeance(
					interaction.getSource(), interaction.getDamage().getVenged());
		}
		if (interaction.getDamage().getDeflected() > 0) {
			interaction.getSource().getDamageManager().damage(interaction.getVictim(),
					interaction.getDamage().getDeflected(), interaction.getDamage().getDeflected(), DamageType.DEFLECT);
		}
		if (interaction.getDamage().getRecoiled() > 0) {
			interaction.getSource().getDamageManager().damage(interaction.getVictim(),
					interaction.getDamage().getRecoiled(), interaction.getDamage().getRecoiled(), DamageType.DEFLECT);
		}
		interaction.getVictim().retaliate(interaction.getSource());
		return true;
	}

	@Override
	public CombatType getCombatType() {
		return type;
	}

}