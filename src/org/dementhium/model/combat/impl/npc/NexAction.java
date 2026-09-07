package org.dementhium.model.combat.impl.npc;


/**
 * Handles nex's combat action.
 * @author Eclipse
 *
 */
public class NexAction /*extends CombatAction*/ {
	
	/**
	 * The current combat type used.
	 */
	/*private CombatType type = CombatType.MAGIC;
	
	/**
	 *  nex's attacks.
	 * @author Eclipse
	 *
	 */
	/*private static enum Attack {
		MELEE(Animation.create(6354), Graphic.create(-1), null, null),
		RANGE(Animation.create(6986), Graphic.create(2244), null, null),
		MAGIC(Animation.create(6987), Graphic.create(2244), null, null);
		
		/**
		 * The attack animation.
		 */
		/*private final Animation anim;
		
		/**
		 * The start graphic.
		 */
		/*private final Graphic start;
		
		/**
		 * The projectile to send.
		 */
		/*private final Projectile projectile;
		
		/**
		 * The end graphic.
		 */
		/*private final Graphic end;
		
		/**
		 * Constructs a new {@code Attack} {@code Object}.
		 * @param anim The attack animation.
		 * @param start The start graphic.
		 * @param projectile The projectile.
		 * @param end The end graphic.
		 */
		/*private Attack(Animation anim, Graphic start, Projectile projectile, Graphic end) {
			this.anim = anim;
			this.start = start;
			this.projectile = projectile;
			this.end = end;
		}
	}
	
	/**
	 * The current attack.
	 */
	/*private Attack attack;
	
	/**
	 * Constructs a new {@code NexAction} {@code Object}.
	 * @param nex using this combat action.
	 */
	/*public NexAction() {
		super(false);
	}

	@Override
	public boolean commenceSession() {
		interaction.getSource().getCombatExecutor().setTicks(interaction.getSource().getAttackDelay());
		attack = Attack.values()[type.ordinal()];
		int currentHit = 0;
		int maximum = 200;
		if (attack == Attack.MELEE) {
			maximum = 200;
			currentHit = MeleeFormulae.getDamage(interaction.getSource(), interaction.getVictim());
		} else if (attack == Attack.RANGE) {
			currentHit = RangeFormulae.getDamage(interaction.getSource(), interaction.getVictim());
		} else {
			currentHit = MagicFormulae.getDamage(interaction.getSource().getNPC(), interaction.getVictim(), 1.0, 1.0, 1.0);
		}
		interaction.setDeflected(interaction.getVictim().getPlayer().getPrayer().usingPrayer(1, type.getDeflectCurse()));
		interaction.setDamage(Damage.getDamage(interaction.getSource(), 
				interaction.getVictim(), type, currentHit));
		interaction.getDamage().setMaximum(maximum);
		if (attack.projectile != null) {
			ProjectileManager.sendProjectile(attack.projectile.transform(interaction.getSource(), interaction.getVictim()));
		}
		interaction.getSource().animate(attack.anim);
		interaction.getSource().graphics(attack.start);
        int ticks = attack.projectile != null ? (int) Math.floor(attack.projectile.getSourceLocation().distance(interaction.getVictim().getLocation()) * 0.3)
        		: 1;
		interaction.setTicks(ticks);
		return true;
	}

	@Override
	public boolean executeSession() {
		if (interaction.getTicks() < 2 || type == CombatType.MELEE) {
			if (interaction.isDeflected()) {
				interaction.getVictim().graphics(2230 - type.ordinal());
			}
			interaction.getVictim().animate(interaction.isDeflected() ? 12573 : interaction.getVictim().getDefenceAnimation());
		}
		interaction.setTicks(interaction.getTicks() - 1);
		return interaction.getTicks() < 1;
	}

	@Override
	public boolean endSession() {
		if (attack != Attack.MELEE) {
			interaction.getVictim().graphics(attack.end);
		}
		if (interaction.getDamage().getHit() > -1) {
			interaction.getVictim().getDamageManager().damage(
					interaction.getSource(), interaction.getDamage(), type.getDamageType());
		} else {
			interaction.getVictim().graphics(85, 96 << 16);
		}
		if (interaction.getDamage().getVenged() > 0) {
			interaction.getVictim().submitVengeance(interaction.getSource(), interaction.getDamage().getVenged());
		}
		if (interaction.getDamage().getDeflected() > 0) {
			//interaction.getSource().getDamageManager().damage(interaction.getVictim(), 
					//interaction.getDamage().getDeflected(), 
					//interaction.getDamage().getDeflected(), DamageType.DEFLECT);
			interaction.getSource().getDamageManager().miscDamage(interaction.getDamage().getDeflected(), DamageType.DEFLECT);
		}
		if (interaction.getDamage().getRecoiled() > 0) {
			//interaction.getSource().getDamageManager().damage(interaction.getVictim(), 
					//interaction.getDamage().getRecoiled(), 
					//interaction.getDamage().getRecoiled(), DamageType.DEFLECT);
			interaction.getSource().getDamageManager().miscDamage(interaction.getDamage().getRecoiled(), DamageType.DEFLECT);
		}
		interaction.getVictim().retaliate(interaction.getSource());
		return true;
	}

	/**
	 * Sends the magic-based location attack.
	 * @param npc nex.
	 */
	/*public static void sendLocationAttack(final Nex nex) {
		List<Player> players = Region.getLocalPlayers(nex.getLocation(), 12);
		if (players.size() < 1) {
			return;
		}
		Player player = players.get(nex.getRandom().nextInt(players.size()));
		List<Location> locations = new ArrayList<Location>();
		int clippingMask;
		for (int x = player.getLocation().getX() - 5; x < player.getLocation().getX() + 6; x++) {
			for (int y = player.getLocation().getY() - 5; y < player.getLocation().getY() + 6; y++) {
				clippingMask = Region.getClippingMask(x, y, 0);
		        if ((clippingMask & 0x1280180) == 0 && (clippingMask & 0x1280108) == 0
		        		&& (clippingMask & 0x1280120) == 0 && (clippingMask & 0x1280102) == 0) {
		        	locations.add(Location.locate(x, y, 0));
		        }
			}
		}
		if (locations.size() < 1) {
			return;
		}
		final Location l = locations.get(nex.getRandom().nextInt(locations.size()));
		int ticks = (int) Math.floor(nex.getProjectile().getSourceLocation().distance(l) * 0.5);
		World.getWorld().submit(new Tick(ticks + 1) {
			@Override
			public void execute() {
				stop();
				List<Player> players = Region.getLocalPlayers(l, 5);
				for (Player p : players) {
					ActionSender.sendPositionedGraphic(p, l, 1883);
					int damage = 269;
					if (p.getPrayer().usingPrayer(0, Prayer.PROTECT_FROM_MAGIC)) {
						damage = 1;
						p.animate(p.getDefenceAnimation());
					} else if (p.getPrayer().usingPrayer(1, Prayer.DEFLECT_MAGIC)) {
						damage = 1;
						p.graphics(2228);
						p.animate(12573);
					}
					p.getDamageManager().damage(nex, p.getRandom().nextInt(damage), 269, DamageType.MAGE);
				}
			}			
		});
	}
	
	@Override
	public CombatType getCombatType() {
		return type;
	}

	/**
	 * Sets the current combat type.
	 * @param combatType The combat type.
	 */
	/*public void setType(CombatType combatType) {
		this.type = combatType;
	}*/

}