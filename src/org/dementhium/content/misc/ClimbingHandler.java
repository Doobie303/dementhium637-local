package org.dementhium.content.misc;

import org.dementhium.content.DialogueManager;
import org.dementhium.event.EventListener.ClickOption;
import org.dementhium.model.Location;
import org.dementhium.model.World;
import org.dementhium.model.map.GameObject;
import org.dementhium.model.mask.Animation;
import org.dementhium.model.player.Player;
import org.dementhium.model.player.Skills;
import org.dementhium.tickable.Tick;

/**
 * @author Steve <golden_32@live.com>
 * @author 'Mystic Flow <Steven@rune-server.org>
 */
public class ClimbingHandler {

	public static final Animation CLIMB_ANIMATION =  Animation.create(828);

	public static boolean handleClimb(final Player player, GameObject object, ClickOption clickOption) {
		int option = 0;
		switch (clickOption) {
		case FIRST:
			option = 0;
			break;
		case SECOND:
			option = 1;
			break;
		case THIRD:
			option = 2;
			break;
		}
		String name = object.getDefinition().getName().toLowerCase();
		if (!name.contains("ladder") && !name.contains("stair") && !name.contains("staircase") && !name.contains("Trapdoor") && object.getId() != 4039) {
			return false;
		}
		Location teleport = null;
		if (name.contains("staircase") || name.contains("stairs")) { 
			int x = 0;
			int y = 0;
			switch (object.getRotation()) {
			case 3:
				y += 2;
				x++;
				break;
			case 1:
				y--;
				break;
			}
			String optionSelected = object.getDefinition().options[option].toLowerCase();
			boolean up = true;
			if (optionSelected.equals("climb-down")) {
				x = -x;
				y = -y;
				up = false;
				switch (object.getRotation()) {
				case 1:
					y--;
					x--;
					break;
				}
			}
			if(object.getId() == 4495)
				player.teleport(3418, 3541, 2, false);
			else if(object.getId() == 4496)
				player.teleport(3412, 3541, 1, false);
			else if(object.getId() == 45481)
				player.teleport(3214, 3239, 1, false);
			else if(object.getId() == 45482)
				player.teleport(3217, 3239, 0, false);
			else if(object.getId() == 1733)
				player.teleport(3044, 10327, 0, false);
			else if(object.getId() == 32048)
				player.teleport(3045, 3927, 0, false);
			else
				player.teleport(object.getLocation().transform(x, y, up ? 1 : -1), false);
		} else if (name.contains("trapdoor") || object.getId() == 4039) { //4039's name = null, so it isn't recoznized
			if (object.getId() == 4039)
				teleport = Location.locate(2209, 5348, 0);
			else
				teleport = player.getLocation().transform(0, 6400, 0);
		} else if (name.contains("ladder")) {
			if (object.getId() == 2641) {
				if (player.getSkills().getLevelForExperience(Skills.PRAYER) < 31) {
					DialogueManager.sendInfoDialogue(player, "The monks won't allow you up there.", "You need a Prayer level of at least 31 to go up this ladder.");
					return true;
				}
			}
			if (object.getId() == 28714)
				teleport = Location.locate(2926, 3444, 0);
			else if (object.getId() == 55404)
				teleport = player.getLocation().transform(0, 6400, 0);
			else if (player.getLocation().getY() > 8500) {
				teleport = player.getLocation().transform(0, -6400, 0);
			} else {
				String optionSelected = object.getDefinition().options[option].toLowerCase();
				if (optionSelected.contains("-up")) { //Means that option already looks like e.g.: Climb-up Ladder
					teleport = player.getLocation().transform(0, 0, 1);
				} else if (optionSelected.contains("-down")) {
					teleport = player.getLocation().transform(0, 0, -1);
				} else if (optionSelected.equals("climb")) {
					if (player.getLocation().getZ() == 0)
						teleport = player.getLocation().transform(0, 0, 1);
					else if (player.getLocation().getZ() == 3)
						teleport = player.getLocation().transform(0, 0, -1);
					else
						showInterfaceForClimb(player);
				}
			}
		}
		if (teleport != null) {
			final Location finalTeleport = teleport;
			player.animate(CLIMB_ANIMATION);
			World.getWorld().submit(new Tick(2) {
				public void execute() {
					stop();
					player.teleport(finalTeleport, false);
				}
			});
		}
		return true;
	}

	private static void showInterfaceForClimb(Player player) {
		DialogueManager.sendOptionDialogue(player, new int[]{233, 234, -1}, "Go up", "Go down", "Cancel");

	}
}
