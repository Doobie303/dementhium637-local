package org.dementhium.model.combat.impl.specs;

import java.util.ArrayList;
import java.util.List;

import org.dementhium.model.SpecialAttack;
import org.dementhium.model.World;
import org.dementhium.model.combat.CombatType;
import org.dementhium.model.combat.CombatUtils;
import org.dementhium.model.combat.Damage;
import org.dementhium.model.combat.ExtraTarget;
import org.dementhium.model.combat.Interaction;
import org.dementhium.model.combat.MeleeFormulae;
import org.dementhium.model.mask.Animation;
import org.dementhium.model.mask.Graphic;
import org.dementhium.model.misc.DamageManager.DamageType;
import org.dementhium.model.player.Equipment;
import org.dementhium.model.player.Skills;
import org.dementhium.tickable.Tick;

/**
 * Executes the {@code Korasi's sword} special attack: Disrupt.<br>
 * Korasi's sword has a special attack, 
 * Disrupt, which drains 60% of the special attack bar. 
 * The attack is Magic-based and not only automatically hits 
 * (even through protection prayers, but not the Disruption Shield), 
 * but in a single-combat area, will deal anywhere between 
 * 50% and 150% of the wielder's maximum melee hit in damage.
 * @author Emperor
 *
 */
public class Disrupt extends SpecialAttack {

	/**
	 * The animation.
	 */
	private static final Animation ANIMATION = Animation.create(14788);

	/**
	 * The end graphic to execute.
	 */
	private static final Graphic GRAPHIC = Graphic.create(2795);

	@Override
	public boolean commenceSpecialAttack(Interaction interaction) {
		int maximumHit = MeleeFormulae.getMeleeDamage(interaction.getSource(), 1.4);
		int currentHit = interaction.getSource().getRandom().nextInt(Math.max(1, maximumHit));
		interaction.getSource().animate(ANIMATION);
		if (interaction.getSource().isMulti() && interaction.getVictim().isMulti()) {
			List<ExtraTarget> targets = CombatUtils.getTargetList(interaction.getSource(), interaction.getVictim(), 15, 3);
			List<ExtraTarget> finalTargets = new ArrayList<ExtraTarget>();
			ExtraTarget victim = new ExtraTarget(interaction.getVictim());
			victim.setDamage(Damage.getDamage(interaction.getSource(), interaction.getVictim(), CombatType.MAGIC, currentHit));
			victim.getDamage().setMaximum(maximumHit);
			finalTargets.add(victim);
			for (int i = 0; i < targets.size(); i++) {
				ExtraTarget e = targets.get(i);
				if (e != null && e.getVictim() != interaction.getVictim()) {
					Damage damage = Damage.getDamage(interaction.getSource(), e.getVictim(), CombatType.MAGIC, currentHit / 2);
					e.setDamage(damage);
					e.getDamage().setMaximum(maximumHit);
					currentHit /= 2;

					finalTargets.add(e);
					if (finalTargets.size() == 3) {
						break;
					}
				}
			}
			for (ExtraTarget target : finalTargets) attachExperience(interaction, target.getDamage());
			interaction.setTargets(finalTargets);
			return true;
		}
		maximumHit = MeleeFormulae.getMeleeDamage(interaction.getSource(), 1.5);
		int minimum = maximumHit / 3;
        currentHit = minimum + interaction.getSource().getRandom().nextInt(maximumHit - minimum + 1);
        interaction.setDamage(Damage.getDamage(interaction.getSource(), interaction.getVictim(), CombatType.MAGIC, currentHit));
        attachExperience(interaction, interaction.getDamage());

		return true;
	}

	@Override
	public boolean tick(Interaction interaction) {
		return true;
	}

    private void attachExperience(Interaction interaction, Damage damage) {
        final org.dementhium.model.player.Player player = interaction.getSource().getPlayer();
        final double rate = player.getPersonalCombatXpRate();
        org.dementhium.model.combat.SpecialHits.awardOnImpact(player, damage, DamageType.MAGE);
        damage.onImpact(actual -> player.getSkills().addExperience(Skills.MAGIC, actual * 0.4 * rate / 100));
    }

    private void impact(Interaction interaction, Damage damage) {
        if (damage.isResolved() || !org.dementhium.model.combat.SpecialEffects.current(interaction, damage, true)) return;
        interaction.getVictim().graphics(GRAPHIC);
        interaction.getVictim().getDamageManager().damage(interaction.getSource(), damage, DamageType.MAGE);
        if (damage.isResolved()) interaction.getVictim().retaliate(interaction.getSource());
    }

    @Override
    public boolean endSpecialAttack(final Interaction interaction) {
        if (interaction.getDamage() != null) {
            impact(interaction, interaction.getDamage());
            return true;
        }
        final List<ExtraTarget> targets = new ArrayList<ExtraTarget>(interaction.getTargets());
        Tick hitTick = new Tick(1) {
            private int index;
            @Override public void execute() {
                if (index >= targets.size() || index == 3) { stop(); return; }
                ExtraTarget target = targets.get(index++);
                impact(new Interaction(interaction.getSource(), target.getVictim()), target.getDamage());
                if (index >= targets.size() || index == 3) stop();
            }
        };
        hitTick.execute();
        if (hitTick.isRunning()) org.dementhium.model.combat.SpecialEffects.submit(interaction.getSource(), hitTick);
        return true;
    }
	@Override
	public CombatType getCombatType() {
		return CombatType.MELEE;
	}

	@Override
	public int getSpecialEnergyAmount() {
		return 600;
	}

	@Override
	public int getCooldownTicks() {
		return 4;
	}

}
