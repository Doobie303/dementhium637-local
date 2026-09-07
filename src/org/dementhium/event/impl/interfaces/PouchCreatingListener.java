package org.dementhium.event.impl.interfaces;


import org.dementhium.content.skills.summoning.Summoning;
import org.dementhium.event.EventListener;
import org.dementhium.event.EventManager;
import org.dementhium.model.Location;
import org.dementhium.model.map.GameObject;
import org.dementhium.model.player.Player;
import org.dementhium.model.player.Skills;
import org.dementhium.util.InputHandler;


/**
 * The pouch creating/scroll transforming interface listener.
 * @author Emperor
 *
 */
public class PouchCreatingListener extends EventListener {

	@Override
	public void register(EventManager manager) {
		//Disabled so Mystic Flow doesn't leech again..
		//Enabled...
		manager.registerInterfaceListener(Summoning.POUCH_CREATING, this);
		manager.registerInterfaceListener(Summoning.SCROLL_CREATING, this);
		//Obelisks
		manager.registerObjectListener(2149, this);
		manager.registerObjectListener(6483, this);
		manager.registerObjectListener(6484, this);
		manager.registerObjectListener(6486, this);
		manager.registerObjectListener(6487, this);
		manager.registerObjectListener(6489, this);
		manager.registerObjectListener(6490, this);
		manager.registerObjectListener(6492, this);
		manager.registerObjectListener(6493, this);
		manager.registerObjectListener(14825, this);
		manager.registerObjectListener(14826, this);
		manager.registerObjectListener(14827, this);
		manager.registerObjectListener(14828, this);
		manager.registerObjectListener(14829, this);
		manager.registerObjectListener(14830, this);
		manager.registerObjectListener(14831, this);
		manager.registerObjectListener(16482, this);
		manager.registerObjectListener(21253, this);
		manager.registerObjectListener(28716, this);
		manager.registerObjectListener(28719, this);
		manager.registerObjectListener(28722, this);
		manager.registerObjectListener(28725, this);
		manager.registerObjectListener(28728, this);
		manager.registerObjectListener(28731, this);
		manager.registerObjectListener(28734, this);
		manager.registerObjectListener(48190, this);
		//Small obelisks
		manager.registerObjectListener(5787, this);
		manager.registerObjectListener(29882, this);
		manager.registerObjectListener(29938, this);
		manager.registerObjectListener(29939, this);
		manager.registerObjectListener(29940, this);
		manager.registerObjectListener(29941, this);
		manager.registerObjectListener(29942, this);
		manager.registerObjectListener(29943, this);
		manager.registerObjectListener(29944, this);
		manager.registerObjectListener(29945, this);
		manager.registerObjectListener(29946, this);
		manager.registerObjectListener(29947, this);
		manager.registerObjectListener(29948, this);
		manager.registerObjectListener(29949, this);
		manager.registerObjectListener(29950, this);
		manager.registerObjectListener(29951, this);
		manager.registerObjectListener(29952, this);
		manager.registerObjectListener(29953, this);
		manager.registerObjectListener(29954, this);
		manager.registerObjectListener(29955, this);
		manager.registerObjectListener(29956, this);
		manager.registerObjectListener(29957, this);
		manager.registerObjectListener(29958, this);
		manager.registerObjectListener(29959, this);
		manager.registerObjectListener(44837, this);
		manager.registerObjectListener(44838, this);
		manager.registerObjectListener(44839, this);
		manager.registerObjectListener(44840, this);
		manager.registerObjectListener(44841, this);
		manager.registerObjectListener(44842, this);

	}

	@Override
	public boolean interfaceOption(Player player, int interfaceId, int buttonId, int slot, int itemId, int opcode) {
		if (interfaceId == 672) {
			if (buttonId == 19) {
				Summoning.sendInterface(player, Summoning.SCROLL_CREATING);
				return true;
			}
			int amount = 0;
			switch (opcode) {
			case 6:
				amount = 1;
				break;
			case 13: 
				amount = 5;
				break;
			case 0:
				amount = 10;
				break;
			case 15:
				amount = Integer.MAX_VALUE - 1;
				break;
			case 46:
				InputHandler.requestIntegerInput(player, 8, "How many would you like to infuse?");
				player.setAttribute("inputId", 8);
				player.setAttribute("slotId", slot);
				player.setAttribute("inputXItemId", itemId);
				break;
			case 82:
				Summoning.sendRequiredItemsList(player, itemId, slot);
				break;
			default:
				System.out.println("Unhandled summoning interface button - opcode: " + opcode + ", interfaceId: " + interfaceId + "; " + buttonId + ", " + slot + ", " + itemId + ".");
				break;
			}
			if (amount > 0) {
				Summoning.createPouch(player, itemId, slot, amount);
				return true;
			}
		} else if (interfaceId == 666) {
			if (buttonId == 18) {
				Summoning.sendInterface(player, Summoning.POUCH_CREATING);
				return true;
			}
			int amount = 0;
			switch (opcode) {
			case 6:
				amount = 1;
				break;
			case 13: 
				amount = 5;
				break;
			case 0:
				amount = 10;
				break;
			case 15:
				amount = Integer.MAX_VALUE - 1;
				break;
			case 46:
				player.setAttribute("itemX", itemId);
				InputHandler.requestIntegerInput(player, 8, "How many would you like to infuse?");
				break;
			default:
				System.out.println("Unhandled summoning interface button - opcode: " + opcode + ", interfaceId: " + interfaceId + "; " + buttonId + ", " + slot + ", " + itemId + ".");
				break;
			}
			if (amount > 0) {
				Summoning.createScroll(player, itemId, amount);
				return true;
			}
		}
        return false;
    }
	
	@Override
	public boolean objectOption(Player player, int objectId, GameObject gameObject, Location location, ClickOption option) {
		String firstOption = gameObject.getDefinition().options[0];
		String secondOption = gameObject.getDefinition().options[1];
		if (option == ClickOption.FIRST && firstOption != null && firstOption.equals("Infuse-pouch")) {
			Summoning.sendInterface(player, Summoning.POUCH_CREATING);
			player.setAttribute("summoningObelisk", gameObject);
			return true;
		} else if (option == ClickOption.FIRST && firstOption.equals("Renew-points") || option == ClickOption.SECOND && secondOption.equals("Renew-points")) {
			System.out.println(option);
			if (player.getSkills().getLevel(Skills.SUMMONING) == player.getSkills().getLevelForExperience(Skills.SUMMONING)) {
				player.sendMessage("You already have full summoning points.");
				return true;
			}
			player.getSkills().increaseLevelToMaximum(Skills.SUMMONING, 99);
			player.sendMessage("You restore your summoning points.");
			return true;
		} else {
			StringBuilder sb = new StringBuilder("Game object id: ").append(objectId).append("; options: ");
			for (String s : gameObject.getDefinition().options) {
				sb.append(s).append(" ");
			}
			System.out.println(sb);
			//TODO: Renew points.
		}
        return false;
    }

}