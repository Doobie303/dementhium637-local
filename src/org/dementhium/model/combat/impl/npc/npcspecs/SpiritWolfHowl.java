package org.dementhium.model.combat.impl.npc.npcspecs;

import org.dementhium.model.Projectile;
import org.dementhium.model.World;
import org.dementhium.model.combat.CombatAction;
import org.dementhium.model.combat.CombatType;
import org.dementhium.model.map.path.PrimitivePathFinder;
import org.dementhium.model.mask.Animation;
import org.dementhium.model.mask.Graphic;
import org.dementhium.model.misc.ProjectileManager;
import org.dementhium.model.player.Player;

/**
 * Handles the Spirit wolf's special move: Howl.
 * @author Emperor
 *
 */
public class SpiritWolfHowl extends CombatAction {

	/**
	 * The howl animation.
	 */
	private static final Animation HOWL = Animation.create(8293);
	
	/**
	 * The graphic.
	 */
	private static final Graphic GRAPHIC = Graphic.create(1334, 96 << 16);
	
	/**
	 * The projectile to send.
	 */
	private static final Projectile PROJECTILE = Projectile.create(null, null, 1333, 38, 36, 56, 74, 12, 2 << 6);
	
	/**
	 * The owner's special move animation.
	 */
	private static final Animation OWNER_ANIMATION = Animation.create(7660);

	/**
	 * The owner's special move graphic.
	 */
	private static final Graphic OWNER_GRAPHIC = Graphic.create(1316);
	
	/**
	 * Constructs a new {@code SpiritWolfHowl} {@code Object}.
	 */
	public SpiritWolfHowl() {
		super(false);
	}

	@Override
	public boolean commenceSession() {
		interaction.getSource().getCombatExecutor().setTicks(5);
		interaction.getSource().setAttribute("specialMove", false);
		Player owner = interaction.getSource().getFamiliar().getOwner();
		if (!owner.getInventory().contains(12425)) {
			owner.sendMessage("You do not have enough scrolls left to do this special move.");
			return false;
		} else if (interaction.getSource().getFamiliar().getSpecialPoints() < 3) {
			owner.sendMessage("Your familiar does not have enough special move points left.");
			return false;
		} else if (interaction.getVictim().isPlayer()) {
			owner.sendMessage("Your familiar can't scare players.");
			return false;
		}
		//For now, we check if the NPC has a custom combat action.
		CombatAction action = interaction.getVictim().getCombatAction();
		if (action != CombatType.MELEE.getCombatAction() && 
				action != CombatType.MAGIC.getCombatAction() && 
				action != CombatType.RANGE.getCombatAction()) {
			owner.sendMessage("Your familiar can't scare that monster.");
			return false;
		} else if (interaction.getVictim().isFamiliar()) {
			owner.sendMessage("Your familiar can't scare someone's familiar.");
			return false;
		}
		owner.graphics(OWNER_GRAPHIC);
		owner.animate(OWNER_ANIMATION);
		owner.turnTo(interaction.getVictim(), false);
		owner.getInventory().deleteItem(12425, 1);
		interaction.getSource().turnTo(interaction.getVictim(), false);
		interaction.getSource().getFamiliar().updateSpecialPoints(interaction.getSource().getFamiliar().getSpecialCost());
		interaction.getSource().animate(HOWL);
		interaction.getSource().graphics(GRAPHIC);
		Projectile p = PROJECTILE.transform(interaction.getSource(), interaction.getVictim());
		ProjectileManager.sendProjectile(p);
		interaction.setTicks((int) Math.floor(interaction.getSource().getLocation().distance(interaction.getVictim().getLocation()) * 0.5));
		return true;
	}

	@Override
	public boolean executeSession() {
		interaction.setTicks(interaction.getTicks() - 1);
		return interaction.getTicks() < 1;
	}

	@Override
	public boolean endSession() {
		interaction.getSource().getCombatExecutor().reset();
		interaction.getVictim().getCombatExecutor().reset();
		interaction.getVictim().turnTo(interaction.getSource(), false);
		int x = interaction.getVictim().getLocation().getX() - interaction.getSource().getLocation().getX();
		int y = interaction.getVictim().getLocation().getY() - interaction.getSource().getLocation().getY();
		World.getWorld().doPath(new PrimitivePathFinder(), interaction.getVictim(), interaction.getVictim().getLocation().getX() + x, interaction.getVictim().getLocation().getY() + y);
		return true;
	}

	@Override
	public CombatType getCombatType() {
		return CombatType.MAGIC;
	}

}