package org.dementhium.model.combat.impl.specs;

import java.util.List;

import org.dementhium.model.Projectile;
import org.dementhium.model.SpecialAttack;
import org.dementhium.model.World;
import org.dementhium.model.combat.Ammunition;
import org.dementhium.model.combat.CombatType;
import org.dementhium.model.combat.CombatUtils;
import org.dementhium.model.combat.Damage;
import org.dementhium.model.combat.ExtraTarget;
import org.dementhium.model.combat.Interaction;
import org.dementhium.model.combat.RangeData;
import org.dementhium.model.combat.RangeFormulae;
import org.dementhium.model.combat.RangeWeapon;
import org.dementhium.model.mask.Graphic;
import org.dementhium.model.misc.DamageManager.DamageType;
import org.dementhium.model.misc.ProjectileManager;
import org.dementhium.model.player.Equipment;
import org.dementhium.tickable.Tick;

/**
 * Executes the Rune thrownaxe special attack: -Chain hit.
 * @author Emperor
 *
 */
public class ChainHit extends SpecialAttack {
	
	/**
	 * The graphics.
	 */
	private static final Graphic GRAPHICS = Graphic.create(257, 96 << 16);
	
	/**
	 * The special attack projectile GFX id.
	 */
	private static final short ANIMATION = 1068;
	
	/**
	 * The special attack projectile GFX id.
	 */
	private static final short PROJECTILE_ID = 258;
		
	@Override
	public boolean commenceSpecialAttack(Interaction interaction) {
		RangeData data = new RangeData(true);
		data.setWeapon(RangeWeapon.get(interaction.getSource().getPlayer().getEquipment().getSlot(3)));
		data.setAmmo(Ammunition.get(interaction.getSource().getPlayer().getEquipment().getSlot(3)));
        if (data.getWeapon() == null || data.getAmmo() == null) return false;
        int ammoSlot = data.getWeapon().getAmmunitionSlot();
        if (ammoSlot >= 0 && (interaction.getSource().getPlayer().getEquipment().get(ammoSlot) == null
                || interaction.getSource().getPlayer().getEquipment().get(ammoSlot).getAmount() < 1)) return false;
		if (data.getAmmo() == null || !data.getWeapon().getAmmunition().contains(data.getAmmo().getItemId())) {
			interaction.getSource().getPlayer().sendMessage("You do not have enough ammo left.");
			interaction.getSource().getCombatExecutor().reset();
			return false;
		}
		if (interaction.getVictim().isPlayer()) {
			interaction.setDeflected(interaction.getVictim().getPlayer().getPrayer().usingPrayer(1, 8));
		}
		int maximum = RangeFormulae.getRangeDamage(interaction.getSource(), 1.0);
		if (interaction.getSource().isMulti() && interaction.getVictim().isMulti()) {
			List<ExtraTarget> targets = CombatUtils.getTargetList(interaction.getSource(), interaction.getVictim(), 14, 10);
			ExtraTarget toRemove = null;
			for (ExtraTarget e : targets) {
				if (e.getVictim() == interaction.getVictim()) {
					toRemove = e;
					continue;
				}
				e.setDamage(Damage.getDamage(interaction.getSource(), e.getVictim(), CombatType.RANGE, RangeFormulae.getDamage(interaction.getSource(), e.getVictim())));
				e.getDamage().setMaximum(maximum);
				org.dementhium.model.combat.SpecialHits.awardOnImpact(interaction.getSource().getPlayer(), e.getDamage(), DamageType.RANGE);
			}
			if (toRemove != null) {
				targets.remove(toRemove);
			}
			interaction.setTargets(targets);
		}
		data.setDamage(Damage.getDamage(interaction.getSource(), interaction.getVictim(), CombatType.RANGE,RangeFormulae.getDamage(interaction.getSource(), interaction.getVictim())));
		data.getDamage().setMaximum(maximum);
		int speed = (int) (46 + (interaction.getSource().getLocation().distance(interaction.getVictim().getLocation()) * 5));
		ProjectileManager.sendProjectile(Projectile.create(interaction.getSource(), interaction.getVictim(), PROJECTILE_ID, 40, 36, 56, speed, 0, 11));
		interaction.setTicks((int) Math.floor(interaction.getSource().getLocation().distance(interaction.getVictim().getLocation()) * 0.3));
		interaction.getSource().animate(ANIMATION);
		interaction.getSource().graphics(GRAPHICS);
		
		data.setDropAmmo(data.getAmmo().getItemId() != 4740 && data.getAmmo().getItemId() != 15243);
        interaction.setRangeData(data);
        CombatUtils.dropArrows(interaction.getSource().getPlayer(), interaction.getVictim(), data);
		
		return true;
	}

	@Override
	public boolean tick(Interaction interaction) {
		if (interaction.getTicks() < 2) {
			interaction.getVictim().animate(interaction.isDeflected() ? 12573 
					: interaction.getVictim().getDefenceAnimation());
			if (interaction.isDeflected()) {
				interaction.getVictim().graphics(2229);
			}
		}
		interaction.setTicks(interaction.getTicks() - 1);
		return interaction.getTicks() < 1;
	}

	@Override
    public boolean endSpecialAttack(final Interaction interaction) {
        Damage primary=interaction.getRangeData().getDamage();
        if(primary==null || primary.isResolved())return true;
        org.dementhium.model.combat.SpecialHits.apply(interaction,primary,DamageType.RANGE,0);
        if(!primary.isResolved() || interaction.getTargets()==null || interaction.getTargets().isEmpty())return true;
        final List<ExtraTarget> targets=new java.util.ArrayList<ExtraTarget>(interaction.getTargets());
        org.dementhium.model.combat.SpecialEffects.submit(interaction.getSource(),new Tick(1) {
            int index;
            org.dementhium.model.Mob previous=interaction.getVictim();
            public void execute() {
                if(index>=targets.size()){stop();return;}
                ExtraTarget next=targets.get(index++);
                Interaction bounce=new Interaction(interaction.getSource(),next.getVictim());
                if(!org.dementhium.model.combat.SpecialEffects.current(bounce,next.getDamage(),true)
                        || !interaction.getSource().isMulti() || !next.getVictim().isMulti()
                        || !next.getVictim().isAttackable(interaction.getSource())
                        || interaction.getSource().getPlayer().getSpecialAmount()<getSpecialEnergyAmount()){stop();return;}
                interaction.getSource().getPlayer().deductSpecial(getSpecialEnergyAmount());
                ProjectileManager.sendProjectile(Projectile.create(previous,next.getVictim(),PROJECTILE_ID,40,36,32,46,5,0));
                org.dementhium.model.combat.SpecialHits.apply(bounce,next.getDamage(),DamageType.RANGE,0);
                previous=next.getVictim();
                if(index>=targets.size())stop();
            }
        });
        return true;
    }

	@Override
	public CombatType getCombatType() {
		return CombatType.RANGE;
	}

	@Override
	public int getSpecialEnergyAmount() {
		return 100;
	}

	@Override
	public int getCooldownTicks() {
		return 5;
	}
	
}