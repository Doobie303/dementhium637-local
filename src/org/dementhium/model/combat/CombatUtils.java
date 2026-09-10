package org.dementhium.model.combat;

import java.util.ArrayList;
import java.util.List;

import org.dementhium.cache.format.CacheNPCDefinition;
import org.dementhium.content.activity.impl.DuelActivity;
import org.dementhium.content.skills.Prayer;
import org.dementhium.model.Container;
import org.dementhium.model.Item;
import org.dementhium.model.Mob;
import org.dementhium.model.Projectile;
import org.dementhium.model.definition.WeaponInterface;
import org.dementhium.model.map.Region;
import org.dementhium.model.mask.Graphic;
import org.dementhium.model.misc.DamageManager.DamageType;
import org.dementhium.model.misc.GroundItemManager;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.player.DegradingHandler;
import org.dementhium.model.player.Equipment;
import org.dementhium.model.player.Player;
import org.dementhium.model.player.Skills;
import org.dementhium.net.ActionSender;

/**
 * Holds combat-related utilities.
 * 
 * @author Emperor
 * @author 'Mystic Flow
 */
public class CombatUtils {

	/**
	 * Checks if an NPC is using protection prayers/curses.
	 * 
	 * @param npc
	 *            The npc.
	 * @param type
	 *            The combat type to protect from.
	 * @return {@code True} if so, {@code false} if not.
	 */
	public static boolean usingProtection(NPC npc, CombatType type) {
		CacheNPCDefinition def = npc.getDefinition().getCacheDefinition();
		if (type == CombatType.MELEE) {
			if (def.getHeadIcon() == 0 || def.getHeadIcon() == 8
					|| def.getHeadIcon() == 12 || def.getHeadIcon() == 16) {
				return true;
			}
			return false;
		} else if (type == CombatType.RANGE) {
			if (def.getHeadIcon() == 1 || def.getHeadIcon() == 6
					|| def.getHeadIcon() == 9 || def.getHeadIcon() == 14
					|| def.getHeadIcon() == 17) {
				return true;
			}
			return false;
		} else if (type == CombatType.MAGIC) {
			if (def.getHeadIcon() == 2 || def.getHeadIcon() == 6
					|| def.getHeadIcon() == 10 || def.getHeadIcon() == 13
					|| def.getHeadIcon() == 18) {
				return true;
			}
			return false;
		}
		return false;
	}

	/**
	 * Gets a projectile to send.
	 * 
	 * @param source
	 *            The attacking player.
	 * @param victim
	 *            The victim.
	 * @param weaponType
	 *            The range weapon type.
	 * @param projectileId
	 *            The projectile id.
	 * @return The constructed {@code Projectile}.
	 */
	public static Projectile getProjectile(Player source, Mob victim,
			int weaponType, int projectileId) {
		switch (weaponType) {
		case 0:
		case 2:
			int speed = (int) (46 + (source.getLocation().distance(
					victim.getLocation()) * 5));
			return Projectile.create(source, victim, projectileId, 40, 36, 41,
					speed, 5);
		case 1:
			speed = (int) (32 + (source.getLocation().distance(
					victim.getLocation()) * 5));
			return Projectile.create(source, victim, projectileId, 38, 36, 41,
					speed, 5);
		case 3:
			speed = (int) (32 + (source.getLocation().distance(
					victim.getLocation()) * 5));
			return Projectile.create(source, victim, projectileId, 40, 36, 32,
					speed, 15);
		case 4:
			speed = (int) (46 + (source.getLocation().distance(
					victim.getLocation()) * 5));
			return Projectile.create(source, victim, projectileId, 31, 36, 45,
					speed, 0);
		case 5:
			speed = (int) (46 + (source.getLocation().distance(
					victim.getLocation()) * 5));
			return Projectile.create(source, victim, projectileId, 40, 36, 45,
					speed, 5, 11);
		case 6:
			speed = (int) (46 + (source.getLocation().distance(
					victim.getLocation()) * 5));
			return Projectile.create(source, victim, projectileId, 40, 36, 45,
					speed, 5, 11);
		}
		return Projectile.ranged(source, victim, projectileId, 40, 36, 41, 5);
	}

	public static int getAttackAnimation(Mob mob) {
		// Hai I am Emperor and I declare this method: Extreme Shit.
		if (mob.isPlayer()) {
			Player player = mob.getPlayer();
			Item weapon = player.getEquipment().get(Equipment.SLOT_WEAPON);
			if (weapon != null) {
				String name = weapon.getDefinition().getName();
				if (weapon.getId() == 15403) {
					if (player.getSettings().getCombatType() == WeaponInterface.TYPE_CRUSH)
						return 12003;
					return 12002;
				}
				if (name.contains("maul")) {
					if (name.startsWith("Granite"))
						return 1665; // Granite Maul anim
					return 2661;
				}
				if (name.equals("Dragon scimitar")) {
					return 15071;
				}
				if (name.contains("scimitar") || name.contains("Darklight")
						&& !name.equals("Dragon scimitar")) {
					if (player.getSettings().getCombatStyle() == WeaponInterface.STYLE_CONTROLLED)
						return 12028;
					return 12029;
				}
				if (name.contains("spear")) {
					if (player.getSettings().getCombatType() == WeaponInterface.TYPE_STAB)
						return 381;
					else if (player.getSettings().getCombatType() == WeaponInterface.TYPE_SLASH)
						return 380;
					return 382;
				}
				switch (DegradingHandler.getCombatItemId(weapon.getId())) {
				case -1:
					if (player.getSettings().getCombatStyle() == WeaponInterface.STYLE_AGGRESSIVE) {
						return 423; // kick
					} else {
						return 422; // punch
					}
				case 19784: // korasi'
					return 12029;
				case 4726:
					return 2080;
				case 4718:
					if (player.getSettings().getCombatType() == WeaponInterface.TYPE_CRUSH)
						return 12003;
					return 12002;
				case 14484:
					return 393;
				case 15241:
					return 12152;
				case 10034:
					return 2779;
				case 6526:
				case 6908:
				case 6910:
				case 4675:
				case 6912:
				case 6914:
				case 13867:
				case 13869:
				case 13941:
				case 13943:
				case 18355:
					return 419;
				case 4068:
				case 4503:
				case 4508:
				case 18705:
					if (player.getSettings().getCombatStyle() == WeaponInterface.STYLE_CONTROLLED)
						return 12310;
					return 12311;
				case 11696:
				case 11694:
				case 11698:
				case 11700:
				case 11730:
				case 1307:
				case 1309:
				case 1311:
				case 1313:
				case 1315:
				case 1317:
				case 1319:
				case 10858:
					if (player.getSettings().getCombatStyle() == WeaponInterface.STYLE_DEFENSIVE)
						return 7049;
					else if (player.getSettings().getCombatType() == WeaponInterface.TYPE_CRUSH)
						return 7048;
					return 7041;
				case 18349:
					if (player.getSettings().getCombatStyle() == WeaponInterface.STYLE_CONTROLLED)
						return 13048;
					return 13049;
				case 6523:
					if (player.getSettings().getCombatStyle() == WeaponInterface.STYLE_CONTROLLED)
						return 13048;
					return 13049;
				case 18351:
					if (player.getSettings().getCombatStyle() == WeaponInterface.STYLE_CONTROLLED)
						return 13049;
					return 13048;
				case 14679:
					return 401;
				case 13899:
				case 13901:
				case 13923:
				case 13925:
				case 7806:
					if (player.getSettings().getCombatStyle() == WeaponInterface.STYLE_CONTROLLED)
						return 13049;
					return 13048;
				case 13902:
				case 13904:
				case 13926:
				case 13928:
					return 401;
				case 15486:
					if (player.getSettings().getCombatStyle() == WeaponInterface.STYLE_AGGRESSIVE)
						return 12029;
					else if (player.getSettings().getCombatStyle() == WeaponInterface.STYLE_DEFENSIVE)
						return 414;
					return 12028;
				case 11716:
					if (player.getSettings().getCombatType() == WeaponInterface.TYPE_STAB)
						return 12006;
					else if (player.getSettings().getCombatType() == WeaponInterface.TYPE_SLASH)
						return 12005;
					else if (player.getSettings().getCombatType() == WeaponInterface.TYPE_CRUSH)
						return 12009;
					return 12006;
				case 9174:
				case 9175:
				case 9176:
				case 9177:
				case 9178:
				case 9179:
				case 9180:
				case 9181:
				case 9182:
				case 9183:
				case 9184:
				case 9185:
				case 9186:
					return 4230;
				case 1265:
				case 1266:
				case 1267:
				case 1268:
				case 1269:
				case 1270:
				case 1271:
				case 1272:
				case 1273:
				case 1274:
				case 1275:
				case 1276:
					return 401;
				case 4755:
					return 2062;
				case 10887:
					return 5865;
				case 4151:
				case 15441:
				case 15442:
				case 15443:
				case 15444:
					return 11968; // Whip
				case 1215:
				case 1231:
				case 5680:
				case 5698:
				case 13465:
				case 13467:
				case 13976:
				case 13978:
					return 402; // Dragon daggers
				case 4214:
				case 6724:
				case 4212:
				case 4827:
				case 11235:
				case 841:
				case 843:
				case 849:
				case 853:
				case 856:
				case 861:
				case 839:
				case 845:
				case 847:
				case 851:
				case 855:
				case 859:
					return 426; // Bows
				case 18357:
					return 4230; // Crossbows
				case 4734:
					return 2075; // Karil x-bow
				case 6528:
					return 2661; // Obby maul
				case 1434:
					if (player.getSettings().getCombatType() == WeaponInterface.TYPE_STAB)
						return 400;
					return 401;
				case 1305:
					if (player.getSettings().getCombatType() == WeaponInterface.TYPE_STAB)
						return 12310;
					return 12311;
				}
				if (name.contains("staff") || name.contains("Staff")) {
					if (player.getSettings().getCombatType() == WeaponInterface.STYLE_DEFENSIVE)
						return 402;
					else if (player.getSettings().getCombatType() == WeaponInterface.TYPE_CRUSH)
						return 401;
					return 400;
				}
				if (name.contains("longsword")) {
					if (player.getSettings().getCombatStyle() == WeaponInterface.STYLE_CONTROLLED)
						return 13049;
					return 13048;
				}
				if (name.contains("hammers")) {
					return 2068;
				}
				if (name.contains("rapier")) {
					if (player.getSettings().getCombatStyle() == WeaponInterface.STYLE_CONTROLLED)
						return 13048;
					return 13049;
				}
			} else {
				return player.getSettings().getCombatStyle() == WeaponInterface.STYLE_AGGRESSIVE ? 423
						: 422;
			}
		} else {
			return 1403;
		}
		return 422;
	}

	/**
	 * Returns a list of targets in range.
	 * 
	 * @param attacker
	 *            The entity executing the attack.
	 * @param source
	 *            The entity to check from.
	 * @param distance
	 *            The maximum distance between the attacking entity and the
	 *            target.
	 * @param maximum
	 *            The maximum amount of targets to get.
	 * @return The list of targets.
	 */
    /** Maximum is the total accepted target count, including the selected victim. */
    public static List<ExtraTarget> getTargetList(Mob attacker, Mob source, int distance, int maximum) {
        java.util.List<Mob> candidates = new java.util.ArrayList<Mob>();
        if (source.isPlayer()) candidates.addAll(Region.getLocalPlayers(source.getLocation(), distance));
        else candidates.addAll(Region.getLocalNPCs(source.getLocation(), distance));
        return selectTargets(attacker, source, distance, maximum, candidates);
    }

    public static List<ExtraTarget> selectTargets(Mob attacker, Mob selected, int distance, int maximum,
            java.util.List<? extends Mob> candidates) {
        List<ExtraTarget> targets = new ArrayList<ExtraTarget>();
        if (maximum <= 0 || distance < 0) return targets;
        java.util.Set<Mob> seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Mob, Boolean>());
        java.util.List<Mob> ordered = new java.util.ArrayList<Mob>();
        ordered.add(selected); ordered.addAll(candidates);
        for (Mob target : ordered) {
            if (target == null || target == attacker || !seen.add(target) || target.getHitPoints() <= 0
                    || target.getLocation().getZ() != selected.getLocation().getZ()
                    || Math.floor(target.getLocation().distance(selected.getLocation())) > distance
                    || !target.isMulti() || !target.isAttackable(attacker)
                    || !org.dementhium.model.instance.InstanceAccess.canInteract(attacker, target)) continue;
            targets.add(new ExtraTarget(target));
            if (targets.size() == maximum) break;
        }
        return targets;
    }

	/**
	 * Gets the hit decreased by all dragonfire protection modifiers.
	 * 
	 * @param mob
	 *            The mob defending.
	 * @param source
	 *            The mob attacking.
	 * @param hit
	 *            The hit to decrease.
	 * @return The decreased hit.
	 */
	public static int getDragonProtection(Mob mob, Mob source, int hit) {
		if (!mob.isPlayer()) {
			return hit;
		}
		String message = "You are horribly burnt by the dragon's "
				+ source.getAttribute("dragonfireName") + ".";
		if (mob.getPlayer().getPrayer()
				.usingPrayer(0, Prayer.PROTECT_FROM_MAGIC)
				|| mob.getPlayer().getPrayer()
						.usingPrayer(1, Prayer.DEFLECT_MAGIC)) {
			hit *= 0.6;
			message = "Your magic prayer absorbs some of the dragon's "
					+ source.getAttribute("dragonfireName") + ".";
		}
		int itemId = mob.getPlayer().getEquipment()
				.getSlot(Equipment.SLOT_SHIELD);
		if (itemId == 11283 || itemId == 11284) {
			// TODO: Charging.
			message = "Your shield absorbs most of the dragon's "
					+ source.getAttribute("dragonfireName") + ".";
			hit *= 0.1;
		} else if (itemId == 1540 || itemId == 8282 || itemId == 16079
				|| itemId == 16933) {
			message = "Your shield absorbs most of the dragon's "
					+ source.getAttribute("dragonfireName") + ".";
			hit *= 0.1;
		}
		if (System.currentTimeMillis()
				- mob.getPlayer().getAttribute("antiFire", 0L) < 360000) {
			message = "Your antifire potion helps you defend the against the dragon's "
					+ source.getAttribute("dragonfireName") + ".";
			hit -= 250;
		} else if (System.currentTimeMillis()
				- mob.getPlayer().getAttribute("santiFire", 0L) < 360000) {
			message = "Your super antifire potion helps you defend the against the dragon's "
					+ source.getAttribute("dragonfireName") + ".";
			hit -= 450;
		}
		if (source.isNPC() && source.getAttribute("dragonfireName") != null) {
			mob.getPlayer().sendMessage(message);
		}
		return hit;
	}

	/**
	 * Gets the range damage to deal, this also activated enchanted bolt
	 * effects.
	 * 
	 * @param player
	 *            The attacking player.
	 * @param victim
	 *            The victim.
	 * @param ammo
	 *            The ammunition used.
	 * @return The {@code Damage} instance.
	 */
	public static Damage getRangeDamage(Player player, Mob victim,
			Ammunition ammo) {
		if (player.getRandom().nextInt(100) < 75) {
			return Damage.getDamage(player, victim, CombatType.RANGE,
					RangeFormulae.getDamage(player, victim));
		}
		switch (ammo.getItemId()) {
		case 9236: // Opal
			victim.graphics(Graphic.create(749));
			return Damage.getDamage(player, victim, CombatType.RANGE,
					RangeFormulae.getDamage(player, victim, 1.1, 1.1, .95));
        case 9237: // Jade: stun belongs to a damaging impact, not damage construction.
            return Damage.getDamage(player, victim, CombatType.RANGE, RangeFormulae.getDamage(player, victim))
                    .onImpact(applied -> {
                        victim.graphics(Graphic.create(755));
                        if (victim.isPlayer() && victim.getRandom().nextInt(120) > victim.getPlayer().getSkills().getLevel(Skills.AGILITY))
                            victim.stun(4, "You have been stunned.", false);
                    });
        case 9238: // Water-staff exclusion; fiery target tuning remains documented separately.
            if (victim.isPlayer()) {
                int staff=victim.getPlayer().getEquipment().getSlot(Equipment.SLOT_WEAPON);
                if (staff==1383 || staff==1395 || staff==1403 || staff==6562 || staff==6563 || staff==11736 || staff==11738) break;
            }
            return Damage.getDamage(player,victim,CombatType.RANGE,
                    RangeFormulae.getDamage(player,victim,1.1,1.15,.95))
                    .onImpact(applied -> victim.graphics(Graphic.create(750)));
		case 9239: // Topaz: no stat loss before impact or on a blocked hit.
            Damage d = Damage.getDamage(player, victim, CombatType.RANGE, RangeFormulae.getDamage(player, victim));
            return d.onImpact(applied -> {
                victim.graphics(Graphic.create(757));
                if (victim.isPlayer()) victim.getPlayer().getSkills().decreaseLevelToZero(Skills.MAGIC, (int)(applied * .05));
            });
        case 9240: // Sapphire
			victim.graphics(Graphic.create(751));
			d = Damage.getDamage(player, victim, CombatType.RANGE,
					RangeFormulae.getDamage(player, victim));
			if (victim.isPlayer()) {
				d.onImpact(applied -> {
                    double drained = Math.min(victim.getPlayer().getSkills().getPrayerPoints(), applied * 0.1);
                    victim.getPlayer().getSkills().drainPray(drained);
                    player.getSkills().restorePray(drained);
                });
			}
			return d;
		case 9241: // Emerald: respect target poison immunity when the bolt lands.
            final boolean poison = player.getRandom().nextInt(10) < 8;
            return Damage.getDamage(player, victim, CombatType.RANGE, RangeFormulae.getDamage(player, victim))
                    .onImpact(applied -> {
                        victim.graphics(Graphic.create(752));
                        if (poison) victim.getPoisonManager().poison(player, 68);
                    });
        case 9242: // Blood forfeit: a fraction of current HP, never triple weapon damage.
            if (player.getHitPoints() * 10 < player.getSkills().getMaximumLifePoints()) break;
            d = Damage.getDamage(player, victim, CombatType.RANGE, Math.min(1000, victim.getHitPoints()/5));
            return d.onImpact(applied -> {
                victim.graphics(Graphic.create(754));
                player.getDamageManager().miscDamage(Math.max(1, player.getHitPoints()/10), DamageType.RED_DAMAGE);
            });
        case 9243: // Historical reduction magnitude remains provisional; preserve existing tuning.
            return Damage.getDamage(player, victim, CombatType.RANGE,
                    RangeFormulae.getDamage(player, victim, 1.0, 1.0, .55));
        case 9244: // Protection negates the enchantment, not the underlying ordinary bolt.
            if (dragonstoneProtected(victim)) break;
            return Damage.getDamage(player,victim,CombatType.RANGE,
                    RangeFormulae.getDamage(player,victim,2.0,1.70,1.2))
                    .onImpact(applied -> victim.graphics(Graphic.create(756)));
		case 9245: // Onyx
            if (victim.isNPC() && EquipmentEffects.undead(victim.getNPC().getDefinition().getName().toLowerCase(java.util.Locale.ROOT))) break;
			victim.graphics(Graphic.create(753));
			d = Damage.getDamage(player, victim, CombatType.RANGE,
					RangeFormulae.getDamage(player, victim, 1.25, 1.3, 1.0));
			d.onImpact(applied -> player.heal(applied / 4));
			return d;
		}
		return Damage.getDamage(player, victim, CombatType.RANGE,
				RangeFormulae.getDamage(player, victim));
	}

    public static boolean dragonstoneProtected(Mob victim) {
        if (victim.isNPC()) {
            String name=victim.getNPC().getDefinition().getName().toLowerCase(java.util.Locale.ROOT);
            return name.endsWith(" dragon") || name.equals("dragon") || name.equals("elvarg");
        }
        int shield=victim.getPlayer().getEquipment().getSlot(Equipment.SLOT_SHIELD);
        long now=System.currentTimeMillis();
        return shield==1540 || shield==11283 || shield==11284 || shield==8282 || shield==16079 || shield==16933
                || now-victim.getAttribute("antiFire",0L)<360000 || now-victim.getAttribute("santiFire",0L)<360000;
    }

	/**
	 * Adds the experience gained when dealing a hit.
	 * 
	 * @param player
	 *            The player.
	 * @param damage
	 *            The damage dealt.
	 * @param type
	 *            The damage type.
	 */
	public static void appendExperience(Player player, int damage,
			DamageType type) {
        experienceAtLaunch(player,type).accept(damage);
    }
    public static java.util.function.IntConsumer experienceAtLaunch(final Player player, final DamageType type) {
        final int style=player.getSettings().getCombatStyle();
        final double rate=player.getPersonalCombatXpRate();
        return damage -> {
		if (damage < 1) {
			return;
		}
		double xp = damage * .4 * rate / 100;
		double hpXP = damage * .133 * rate / 100;
		player.getSkills().addExperience(Skills.CONSTITUTION, hpXP);
		if (type == DamageType.MELEE) {
			switch (style) {
			case WeaponInterface.STYLE_ACCURATE:
				player.getSkills().addExperience(Skills.ATTACK, xp);
				break;
			case WeaponInterface.STYLE_AGGRESSIVE:
				player.getSkills().addExperience(Skills.STRENGTH, xp);
				break;
			case WeaponInterface.STYLE_DEFENSIVE:
				player.getSkills().addExperience(Skills.DEFENCE, xp);
				break;
			case WeaponInterface.STYLE_CONTROLLED:
				for (int i = Skills.ATTACK; i <= Skills.STRENGTH; i++) {
					player.getSkills().addExperience(i, xp / 3);
				}
				break;
			}
		} else if (type == DamageType.RANGE) {
			switch (style) {
			case WeaponInterface.STYLE_LONG_RANGE:
				player.getSkills().addExperience(Skills.RANGED, xp / 2);
				player.getSkills().addExperience(Skills.DEFENCE, xp / 2);
				break;
			default:
				player.getSkills().addExperience(Skills.RANGED, xp);
				break;
			}
		}
	        };
    }

	/**
	 * Drops the player's arrows.
	 * 
	 * @param player
	 *            The player.
	 * @param victim
	 *            The victim.
	 * @param data
	 *            The range data.
	 */
	public static void dropArrows(Player player, Mob victim, RangeData data) {
        if (data == null || !consumeAmmunition(player, data)) return;
        int count = data.ammunitionToDrop;
        data.ammunitionToDrop = 0;
        if (count == 0) return;
        Item item = new Item(data.getAmmo().getItemId(), count);
        if (player.getActivity() instanceof DuelActivity) {
            Container record = player.getAttribute("droppedAmmo");
            if (record == null) record = new Container(29, false);
            Container one=new Container(1,false);one.set(0,item);
            if(!record.tryAddAll(one)){Container expanded=new Container(record.getSize()+1,false,false,true);expanded.addAll(record);expanded.addAll(one);record=expanded;}
            player.setAttribute("droppedAmmo", record);
        } else GroundItemManager.increaseAmount(player, item.getId(), victim.getLocation(), count);
    }

    public static boolean consumeAmmunition(Player player, RangeData data) {
        if (data == null) return false;
        if (data.ammunitionConsumed) return true;
        if (data.getWeaponType() == 5 || data.getWeapon().getAmmunitionSlot() < 0) {
            data.ammunitionConsumed = true;
            return true;
        }
        int slot = data.getWeapon().getAmmunitionSlot();
        Item equipped = player.getEquipment().get(slot);
        int count = data.getAmmunitionCount();
        if (equipped == null || equipped.getId() != data.getAmmo().getItemId() || equipped.getAmount() < count) return false;
        boolean ava = player.getEquipment().contains(10498) || player.getEquipment().contains(10499)
                || player.getEquipment().contains(20068);
        int consumed = 0;
        for (int i = 0; i < count; i++) {
            if (!data.isDropAmmo() || !ava || player.getRandom().nextInt(10) >= 7) consumed++;
        }
        if (consumed > 0) {
            player.getEquipment().getContainer().remove(new Item(equipped.getId(), consumed));
            player.getEquipment().refresh();
        }
        data.ammunitionConsumed = true;
        data.ammunitionToDrop = data.isDropAmmo() ? consumed : 0;
        return true;
    }
}

