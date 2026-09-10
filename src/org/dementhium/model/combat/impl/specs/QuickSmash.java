package org.dementhium.model.combat.impl.specs;

import org.dementhium.model.SpecialAttack;
import org.dementhium.model.combat.CombatType;
import org.dementhium.model.combat.Damage;
import org.dementhium.model.combat.Interaction;
import org.dementhium.model.combat.MeleeFormulae;
import org.dementhium.model.player.Equipment;

/**
 * Executes the Granite maul instant special attack: Quick smash
 * @author Emperor
 *
 */
public class QuickSmash extends SpecialAttack {

	/**
	 * The animation the player has to perform.
	 */
	private static final short ANIMATION = 1667;

	/**
	 * The graphics the player should cast when using the special.
	 */
	private static final short GRAPHICS = 340;
	
	@Override
	public boolean commenceSpecialAttack(Interaction interaction) {
        if(interaction.getDamage()!=null || interaction.getSource().getPlayer().getSpecialAmount()<500)return false;
		interaction.setDamage(Damage.getDamage(interaction.getSource(), 
				interaction.getVictim(), CombatType.MELEE, 
				MeleeFormulae.getDamage(interaction.getSource(), 
						interaction.getVictim())));
		interaction.getDamage().setMaximum(MeleeFormulae.getMeleeDamage(interaction.getSource(), 1.0));
		if (interaction.getVictim().isPlayer()) {
			interaction.setDeflected(interaction.getVictim().getPlayer().getPrayer().usingPrayer(1, 9));
		}
		
		interaction.getSource().animate(ANIMATION);
		interaction.getSource().graphics(GRAPHICS);
		interaction.getSource().getPlayer().setSpecialAmount(interaction.getSource().getPlayer().getSpecialAmount() - 500);
		interaction.getVictim().animate(interaction.isDeflected() ? 12573 : interaction.getVictim().getDefenceAnimation());
		if (interaction.isDeflected()) {
			interaction.getVictim().graphics(2230);
		}
		endSpecialAttack(interaction);
		return false;
	}

    /** Resolve exactly this instant hit; never recursively tick every pending combat action. */
    public static boolean activate(org.dementhium.model.player.Player player) {
        player.getSettings().setUsingSpecial(false);
        org.dementhium.net.ActionSender.sendConfig(player,301,0);
        org.dementhium.model.Mob victim=player.getCombatExecutor().getVictim();
        if(!(org.dementhium.model.SpecialAttackContainer.get(player.getEquipment().getSlot(3)) instanceof QuickSmash)
                || victim==null || player.getHitPoints()<=0 || victim.getHitPoints()<=0
                || player.getAttribute("stunned",false)
                || player.getSpecialAmount()<500 || !victim.isAttackable(player)
                || !org.dementhium.model.instance.InstanceAccess.canInteract(player,victim)
                || player.getLocation().getZ()!=victim.getLocation().getZ()
                || !org.dementhium.model.combat.CombatMovement.canMelee(player,victim)
                || !org.dementhium.model.map.path.ProjectilePathFinder.clearPath(player.getLocation(),victim.getLocation())
                || !org.dementhium.model.map.path.ProjectilePathFinder.clearPath(victim.getLocation(),player.getLocation()))return false;
        if(player.getActivity() instanceof org.dementhium.content.activity.impl.DuelActivity) {
            org.dementhium.content.activity.impl.DuelActivity duel=(org.dementhium.content.activity.impl.DuelActivity)player.getActivity();
            if(!duel.isCombatActivity(player,victim,false)
                    || duel.getDuelConfigurations().getRule(org.dementhium.content.activity.impl.duel.DuelConfigurations.Rules.SPECIAL_ATTACKS)
                    || duel.getDuelConfigurations().getRule(org.dementhium.content.activity.impl.duel.DuelConfigurations.Rules.MELEE)
                    || !duel.getDuelConfigurations().weaponAllowed(player,player.getEquipment().getSlot(3)))return false;
        }
        new QuickSmash().commenceSpecialAttack(new Interaction(player,victim));
        return true;
    }

	@Override
	public CombatType getCombatType() {
		return CombatType.MELEE;
	}
	
	@Override
	public int getSpecialEnergyAmount() {
		return 500;
	}

	@Override
	public int getCooldownTicks() {
		return 5;
	}

	@Override
	public boolean isInstant() {
		return true;
	}
}
