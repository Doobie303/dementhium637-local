package org.dementhium.model.combat;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.dementhium.model.player.DegradingHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A class holding all the range weapon definitions.
 * @author Emperor
 *
 */
public final class RangeWeapon {
	
	/**
	 * The range weapons mapping.
	 */
	private static final Map<Integer, RangeWeapon> RANGE_WEAPONS = new HashMap<Integer, RangeWeapon>();

	/**
	 * The mapping containing all the possible ammunition types.
	 */
	private final List<Integer> ammunition;
	
	/**
	 * The item id.
	 */
	private final int itemId;
	
	/**
	 * The attack animation id.
	 */
	private final int animationId;
	
	/**
	 * The attack speed.
	 */
	private final int attackSpeed;
    private int attackRange=10;
    public int getAttackRange(boolean longrange) { return Math.min(10,attackRange+(longrange?2:0)); }
	
	/**
	 * The equipment slot the ammunition uses, if any.
	 */
	private final int ammunitionSlot;
	
	/**
	 * Constructs a new {@code RangeWeapon} {@code Object}.
	 * @param itemId The item id.
	 * @param animationId The animation id.
	 * @param attackSpeed The attack speed.
	 * @param ammunitionSlot The ammunition's equipment slot, or -1 if not worn.
	 * @param ammunition The possible ammunition vector list.
	 */
	private RangeWeapon(int itemId, int animationId, int attackSpeed, int ammunitionSlot, List<Integer> ammunition) {
		this.itemId = itemId;
		this.animationId = animationId;
		this.attackSpeed = attackSpeed;
		this.ammunitionSlot = ammunitionSlot;
		this.ammunition = ammunition;
	}
	
	/**
	 * Populates the range weapons mapping.
	 * @return {@code True}.
	 */
	public static boolean initialize() {
		Document doc;
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			doc = builder.parse(new File("./data/xml/RangeWeaponData.xml"));
		} catch (Throwable e) {
			e.printStackTrace();
			return false;
		}
		NodeList nodeList = doc.getDocumentElement().getChildNodes();
		System.out.println("Loading range weapon info...");
		for (short i = 1; i < nodeList.getLength(); i += 2) {
			Node n = nodeList.item(i);
			if (n != null) {
				if (n.getNodeName().equalsIgnoreCase("Weapon")) {
					NodeList list = n.getChildNodes();
					int itemId = 0;
					int animationId = 426;
					int attackSpeed = 4;
                    int attackRange=10;
					int slot = 13;
					List<Integer> ammo = new ArrayList<Integer>();
					for (int a = 1; a < list.getLength(); a += 2) {
						Node node = list.item(a);
						if (node.getNodeName().equalsIgnoreCase("itemId")) {
							itemId = Integer.parseInt(node.getTextContent());
						} else if (node.getNodeName().equalsIgnoreCase("animationId")) {
							animationId = Integer.parseInt(node.getTextContent());
						} else if (node.getNodeName().equalsIgnoreCase("attackSpeed")) {
							attackSpeed = Integer.parseInt(node.getTextContent());
						} else if (node.getNodeName().equalsIgnoreCase("attackRange")) {
                            attackRange=Integer.parseInt(node.getTextContent());
                            if(attackRange<1 || attackRange>10)throw new IllegalArgumentException("Invalid attackRange for item "+itemId);
                        } else if (node.getNodeName().equalsIgnoreCase("ammunitionSlot")) {
							slot = Integer.parseInt(node.getTextContent());
						} else if (node.getNodeName().equalsIgnoreCase("ammunition")) {
							int ammunitionId = Integer.parseInt(node.getTextContent());
							ammo.add(ammunitionId);
						}
					}
					RangeWeapon weapon=new RangeWeapon(itemId,animationId,attackSpeed,slot,ammo);
                    weapon.attackRange=attackRange;
                    RANGE_WEAPONS.put(itemId,weapon);
				}
			}
		}
		System.out.println("Loaded " + RANGE_WEAPONS.size() + " range weapon definitions.");
		return true;
	}

	/**
	 * Gets a range weapon instance from the mapping.
	 * @param id The item id.
	 * @return The instance.
	 */
	public static RangeWeapon get(int id) {
		RangeWeapon weapon = RANGE_WEAPONS.get(id);
		if (weapon != null) {
			return weapon;
		}
		int combatId = DegradingHandler.getCombatItemId(id);
		RangeWeapon base = RANGE_WEAPONS.get(combatId);
		if (base == null || combatId == id) {
			return null;
		}
		weapon = new RangeWeapon(id, base.animationId, base.attackSpeed,
				base.ammunitionSlot, base.ammunition);
		weapon.attackRange=base.attackRange;
        RANGE_WEAPONS.put(id, weapon);
		return weapon;
	}

	/**
	 * @return the itemId
	 */
	public int getItemId() {
		return itemId;
	}

	/**
	 * @return the ammunitionSlot
	 */
	public int getAmmunitionSlot() {
		return ammunitionSlot;
	}

	/**
	 * @return the ammunition
	 */
	public List<Integer> getAmmunition() {
		return ammunition;
	}

	/**
	 * @return the animationId
	 */
	public int getAnimationId() {
		return animationId;
	}

	/**
	 * @return the attackSpeed
	 */
	public int getAttackSpeed() {
		return attackSpeed;
	}
}

