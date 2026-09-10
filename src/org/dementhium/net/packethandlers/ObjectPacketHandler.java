package org.dementhium.net.packethandlers;

import org.dementhium.cache.format.CacheObjectDefinition;
import org.dementhium.content.DialogueManager;
import org.dementhium.content.activity.impl.BarrowsActivity;
import org.dementhium.content.activity.impl.CastleWarsActivity;
import org.dementhium.content.activity.impl.DungeoneeringActivity;
import org.dementhium.content.activity.impl.ImpetuousImpulses;
import org.dementhium.content.areas.CoordinateEvent;
import org.dementhium.content.misc.ClimbingHandler;
import org.dementhium.content.misc.DoorManager;
import org.dementhium.content.misc.Obelisks;
import org.dementhium.content.misc.WebManager;
import org.dementhium.content.skills.agility.Agility;
import org.dementhium.content.skills.agility.obstacles.BarbarianCourse;
import org.dementhium.content.skills.dungeoneering.Dungeoneering;
import org.dementhium.content.skills.magic.TeleportHandler;
import org.dementhium.content.skills.runecrafting.Runecrafting;
import org.dementhium.content.skills.runecrafting.Talisman;
import org.dementhium.content.skills.smithing.Smithing;
import org.dementhium.content.skills.thieving.Thieving;
import org.dementhium.event.EventListener.ClickOption;
import org.dementhium.event.EventManager;
import org.dementhium.model.Location;
import org.dementhium.model.Mob;
import org.dementhium.model.World;
import org.dementhium.model.map.GameObject;
import org.dementhium.model.map.path.DefaultPathFinder;
import org.dementhium.model.map.path.PathState;
import org.dementhium.model.mask.Animation;
import org.dementhium.model.mask.Graphic;
import org.dementhium.model.npc.impl.Nex.NexAreaEvent;
import org.dementhium.model.player.Equipment;
import org.dementhium.model.player.Player;
import org.dementhium.model.player.Skills;
import org.dementhium.net.ActionSender;
import org.dementhium.net.PacketHandler;
import org.dementhium.net.message.Message;
import org.dementhium.tickable.Tick;

/**
 * @author `Discardedx2 <the_shawn@discardedx2.info>
 * @author 'Mystic Flow <Steven@rune-server.org>
 */
public class ObjectPacketHandler extends PacketHandler {

	public static final int OPTION_1 = 76, OPTION_2 = 55, OPTION_3 = 48, OPTION_4 = 25;

	private EventManager eventManager = EventManager.getEventManager();

	@Override
	public void handlePacket(final Player player, final Message packet) {
		if (player.getAttribute("busy") == Boolean.TRUE) {
			return;
		}
		if(!player.hasReceivedStarter()){
			return;
		}
		WalkingHandler.reset(player);
		int objectId = -1, x = -1, y = -1;
		boolean running = false;
		switch (packet.getOpcode()) {
		case OPTION_1:
			objectId = packet.readLEShort();
			running = packet.readByte() == 1;
			x = packet.readLEShortA();
			y = packet.readShortA();
			break;
		case OPTION_2:
			y = packet.readShort();
			objectId = packet.readLEShort();
			x = packet.readShort();
			running = packet.readByteS() == 1;
			break;
		case OPTION_3:
			handleExamine(player, packet.readShort());
			return;
		case OPTION_4:
			x = packet.readShortA();
			objectId = packet.readLEShortA();
			running = packet.readByteA() == 1;
			y = packet.readLEShortA();
			break;
		}
		if (player.getRights() > 1) {
			player.sendMessage("Incoming object opcode - id: " + objectId + ", x: " + x + ", y:" + y + ".");
		}
		if (objectId == -1 && x == -1 && y == -1) {
			return;
		}
		final Location location = Location.locate(x, y, player.getLocation().getZ());
		GameObject gameObj = location.getGameObject(objectId);
		if (gameObj == null) {
			if (objectId == 6775 && player.getActivity() instanceof BarrowsActivity) {
				gameObj = new GameObject(6775, x, y, player.getLocation().getZ(), 10, 0);
			} else {
				return;
			}
		}
		final GameObject gameObject = gameObj;
        if (!org.dementhium.model.instance.InstanceAccess.canInteract(player,gameObject)) return;
        final org.dementhium.content.instance.InstanceExamples example =
                org.dementhium.content.instance.InstanceExamples.get(player);
        if (example != null) {
            // Example scenes consume their own clicks before ordinary object scripts can run.
            if (!WalkingHandler.canMove(player)) return;
            PathState examplePath = findObjectPath(player, gameObject);
            if (examplePath == null || !examplePath.isRouteFound()) {
                player.sendMessage("I can't reach that!"); return;
            }
            final boolean firstOption = packet.getOpcode() == OPTION_1;
            player.getActionManager().stopAction();
            World.getWorld().submitAreaEvent(player, new CoordinateEvent(player, x, y,
                    gameObject.getDefinition().getSizeX(), gameObject.getDefinition().getSizeY()) {
                @Override public void execute() { example.interact(player, gameObject, firstOption); }
            });
            return;
        }
		if (gameObject.getId() != objectId) {
			return;
		}
		player.getWalkingQueue().setIsRunning(running);
		player.getActionManager().stopAction();
		final CacheObjectDefinition definition = gameObject.getDefinition();
		//This messes up the walking to some objects
		//x = getNearestX(player.getLocation().getX(), x, definition.getSizeX());
		//y = getNearestY(player.getLocation().getX(), y, definition.getSizeY());
		final int objectClicked = objectId;
		System.out.println(gameObject.getId() + ", " + gameObject.getType() + ", " + gameObject.getRotation() + ", " + gameObject.getLocation());
		if ((objectId >= 1440 && objectId <= 1444) || objectId == 1816 || objectId == 2295 || objectId == 43581) {
			player.setAttribute("costValue", 4);
		} else if (objectId == 43529) {
			player.setAttribute("costValue", 17);
		} else if (objectId == 20210) {
			player.setAttribute("costValue", 2);
		}
		switch (objectId) {
		case 43544:
		case 43543:
		case 47120:
			if (!player.getPrayer().isAncientCurses()) {
				player.animate(Animation.create(645));
				player.getPrayer().setAnctientCurses(true);
				player.getPrayer().setAncientBook(true);
				player.getPrayer().switchPrayBook(true);
			} else {
				player.animate(Animation.create(645));
				player.getPrayer().setAnctientCurses(false);
				player.getPrayer().setAncientBook(false);
				player.getPrayer().switchPrayBook(false);
				break;
			}
			case 8749:
				if (player.getAttribute("SpecAltar", -1) > World.getTicks()) {
					player.sendMessage("You must wait before you use this altar again!");
					return;
				}
				if (player.getSpecialAmount() < 1000) {
					player.setSpecialAmount(1000);
					player.setAttribute("SpecAltar", World.getTicks() + 10000);
					player.sendMessage("You have restored your special energy, You now have 100%");
				} else {
					player.sendMessage("You can't charge your special energy when it is 100%!");

			}
			if (gameObject.getRotation() == 0) {
				player.setAttribute("costValue", 2);
			}
			break;
		}
		if (!findObjectPath(player, gameObject).isRouteFound() && objectId != 4496 && objectId != 4388 && objectId != 38811
				&& objectId != 32048 && objectId != 9356) {
			player.sendMessage("I can't reach that!");
			return;
		}
		if (player.getAttribute("cantMove") == Boolean.TRUE) {
			player.sendMessage("You are already doing something!");
			return;
		}
		
		/*
		 * Note that for the objects you change the coords for, you'd have to change them as well in the method at the bottom of this file:
		 * findObjectPath(Player player, GameObject object)
		 */
		boolean coordChange = false;
		if (objectId == 57225 && player.getLocation().getX() <= 2907) {
			x = 2908; y = 5204; coordChange = true;
		} else if (objectId == 1733) {
			x = 3045; y = 3926; coordChange = true;
		}
		
		World.getWorld().submitAreaEvent(player, new CoordinateEvent(player, x, y, coordChange ? 1 : definition.getSizeX(), coordChange ? 1 : definition.getSizeY()) {
			@Override
			public void execute() {
				handleObject(player, objectClicked, gameObject, location, definition, packet);
			}
		});
	}

	private void handleObject(final Player player, int objectClicked, final GameObject gameObject, Location location, CacheObjectDefinition definition, Message packet) {
        if (!org.dementhium.model.instance.InstanceAccess.canInteract(player,gameObject)) return;
		int transformX = 0, transformY = 0;
		if (gameObject.getType() == 0) {
			switch (gameObject.getRotation()) {
			case 0:
				transformX--;
				break;
			case 1:
				transformY++;
				break;
			case 2:
				transformX++;
				break;
			case 3:
				transformY--;
				break;
			}
		}
		player.closeAll(true, true);
		//player.resetTurnTo();
		player.getMask().setFacePosition(location.transform(transformX, transformY, 0), definition.getSizeX(), definition.getSizeY());
		String name = definition.getName().toLowerCase();
		if (org.dementhium.content.home.HomeHub.handleObject(player, gameObject)) return;
		if (name.equals("bank booth") || name.equals("bank chest")) {
			// A bank interaction must always open the bank.  Reusing the
			// equipment-screen marker here can trap the client on interface 667
			// when that marker survives a movement/interface-close race.
			player.getBank().openBank();
			return;
		} else if (name.equals("furnace")) {
			Smithing.furnaceInteraction(player);
			return;
		} else if (name.contains("altar") && !Talisman.alterIds(gameObject.getId())) {
			if (name.startsWith("bandos") || name.startsWith("armadyl") || name.startsWith("saradomin") || name.startsWith("zamorak")) {
				if (player.getSkills().getPrayerPoints() >= player.getSkills().getLevelForExperience(Skills.PRAYER) + 2) {
					player.sendMessage("You already have full prayer points.");
					return;
				}
				if (player.getAttribute("godwarsAltarTicks", -1) > World.getTicks()) {
					player.sendMessage("The gods have already restored your prayer.");
					return;
				}
				player.getSkills().setPrayerPoints(player.getSkills().getLevelForExperience(Skills.PRAYER) + 2, true);
				player.setAttribute("godwarsAltarTicks", World.getTicks() + 1000);
				player.animate(Animation.create(645)); //1651
				player.sendMessage("The gods restore your prayer points.");
				//TODO: Check the amount of respective items of the god the player has and increase by that amount.
				return;
			}
			if (gameObject.getId() == 2640) {
				if (player.getSkills().getPrayerPoints() >= player.getSkills().getLevelForExperience(Skills.PRAYER) + 2) {
					player.sendMessage("You already have full prayer points.");
					return;
				}
			} else {
				if (player.getSkills().getPrayerPoints() >= player.getSkills().getLevelForExperience(Skills.PRAYER)) {
					player.sendMessage("You already have full prayer points.");
					return;
				}
			}
			if (gameObject.getId() == 2640)
				player.getSkills().setPrayerPoints(player.getSkills().getLevelForExperience(Skills.PRAYER) + 2, true);
			else
				player.getSkills().restorePray(99.0);
			player.animate(Animation.create(645)); //1651
			player.sendMessage("You restore your prayer points.");
			return;
		} else if (Obelisks.handle(player, gameObject)) {
			return;
		
		}
		if (objectClicked == 16050) { //this object is also used in the stronghold of security, that's why I decided to put it here
			if(gameObject.getLocation().getX() == 3419 && gameObject.getLocation().getY() == 2905){ //Nardah gen store
				DialogueManager.sendOptionDialogue(player, new int[]{639, 640 ,641, 642}, "Lumbridge", "Varrock", "Falador", "Edgeville");
				ActionSender.sendString(player, "Select a destination.", 232, 1);
				return;
			} else if(gameObject.getLocation().getX() == 3212 && gameObject.getLocation().getY() == 3243){ //lummy gen store
				DialogueManager.sendOptionDialogue(player, new int[]{638, 640 ,641, 642}, "Home", "Varrock", "Falador", "Edgeville");
				ActionSender.sendString(player, "Select a destination.", 232, 1);
				return;
			} else if(gameObject.getLocation().getX() == 3214 && gameObject.getLocation().getY() == 3412){ //varrock gen store
				DialogueManager.sendOptionDialogue(player, new int[]{638, 639, 641, 642}, "Home", "Lumbridge", "Falador", "Edgeville");
				ActionSender.sendString(player, "Select a destination.", 232, 1);
				return;
			} else if(gameObject.getLocation().getX() == 2955 && gameObject.getLocation().getY() == 3388){ //falador gen store
				DialogueManager.sendOptionDialogue(player, new int[]{638, 639 ,640, 642}, "Home", "Lumbridge", "Varrock", "Edgeville");
				ActionSender.sendString(player, "Select a destination.", 232, 1);
				return;
			} else if(gameObject.getLocation().getX() == 3077 && gameObject.getLocation().getY() == 3511){ //edge gen store
				DialogueManager.sendOptionDialogue(player, new int[]{638, 639 ,640, 641}, "Home", "Lumbridge", "Varrock", "Falador");
				ActionSender.sendString(player, "Select a destination.", 232, 1);
				return;
			} else if(gameObject.getLocation().getX() == 3425 && gameObject.getLocation().getY() == 2906){ //nardah 'secret' portal
				if (player.getEquipment().get(0) != null && player.getEquipment().get(0).getId() == 3057
						&& player.getEquipment().get(4) != null && player.getEquipment().get(4).getId() == 3058
						&& player.getEquipment().get(7) != null && player.getEquipment().get(7).getId() == 3059
						&& player.getEquipment().get(9) != null && player.getEquipment().get(9).getId() == 3060
						&& player.getEquipment().get(10) != null && player.getEquipment().get(10).getId() == 3061)
					player.teleport(2604, 4776, 0, true);
				else
					player.teleport(2010, 4752, 0, true);
				return;
			} else if (eventManager.handleObjectOption(player, objectClicked, gameObject, location, forId(packet.getOpcode()))) {
				return;
			}
		}
		if (eventManager.handleObjectOption(player, objectClicked, gameObject, location, forId(packet.getOpcode()))) {
			return;
		} else if (Dungeoneering.handleObject(player, gameObject, forId(packet.getOpcode()))) {
			return;
		} else if (DoorManager.handleDoor(player, gameObject)) {
			return;
		} else if (WebManager.handleWeb(player, gameObject)) {
			return;
		} else if (player.getActivity().objectAction(player, gameObject, forId(packet.getOpcode()))) {
			return;
		} else if (ClimbingHandler.handleClimb(player, gameObject, forId(packet.getOpcode()))) {
			return;
		} else if (Agility.handleObject(player, gameObject)) {
			return;
		} else if(Runecrafting.handleObject(player, gameObject)) {
			return;
		} else {
			Thieving thieving = Thieving.isAction(player, gameObject);
			if (thieving != null) {
				thieving.commence();
				return;
			}
			switch (gameObject.getId()) {
			case 9356:
			case 9357:
				if (org.dementhium.content.minigames.FightCaves.handleEntranceObject(player, gameObject.getId())) {
					return;
				}
				break;
			case 3203: //duel forfeit
                if(!(player.getActivity() instanceof org.dementhium.content.activity.impl.DuelActivity))return;
                org.dementhium.content.activity.impl.DuelActivity duel=(org.dementhium.content.activity.impl.DuelActivity)player.getActivity();
                if(duel.getCurrentState()!=org.dementhium.content.activity.impl.DuelActivity.State.FIGHTING)return;
                if(duel.getDuelConfigurations().getRule(org.dementhium.content.activity.impl.duel.DuelConfigurations.Rules.FORFEIT)){
                    player.sendMessage("Forfeiting is disabled for this duel.");return;
                }
				DialogueManager.send2OptionDialogueWithLongTitle(player, new int[]{748, -1}, "Yes", "No");
				ActionSender.sendString(player, "Do you wish to forfeit?", 718, 0); //inter 554 will do as well?
				break;
			case 57225: //nex landslide
				if (player.getLocation().getX() <= 2907) {
					player.getMask().setFacePosition(Location.locate(2908, 5204, 0), 1, 1);
					DialogueManager.sendDisplayBox(player, 656, "<col=A00000>The room beyond this point is a prison!", 
							"<col=A00000>There is no way out other than death or teleport.", 
							"<col=A00000>Only those who can endure dangerous encounters should proceed.");
				} else if (player.getLocation().distance(NexAreaEvent.AREA_CENTER) < 16) {
					//The 2/3 messages below about Nex are not real ones I think
					/*if (NexAreaEvent.getNexAreaEvent().isSpawned()) {
						player.sendMessage("You attempt to escape..");
						forceWalk(player, Animation.create(10070), 2923, 5204, 3, new int[]{30, 90, -1}, ".. but Nex draws you back in!", 0);
					} else */
						player.sendMessage("You find yourself unable to climb up the slippery landslide.");
				}
				break;
			case 1814: //edge lever
				player.getMask().setFacePosition(Location.locate(3089, 3474, 0), 0, 0);
				player.setAttribute("teleportingToWildLever", true);
				TeleportHandler.telePlayer(player, 3154, 3924, 0, 0, 2, true, true);
				break;
			case 3311: //ardougne lever
				player.getMask().setFacePosition(Location.locate(2560, 3311, 0), 0, 0);
				player.setAttribute("teleportingToWildLever", true);
				TeleportHandler.telePlayer(player, 3154, 3924, 0, 0, 2, true, true);
				break;
			case 1815: //Deserted Keep lever
				player.getMask().setFacePosition(Location.locate(3152, 3923, 0), 0, 0);
				player.animate(835);
				player.sendMessage("You pull the lever ...");
				World.getWorld().submit(new Tick(1) {
					@Override
					public void execute() {
						stop();
						TeleportHandler.telePlayer(player, 2562, 3311, 0, 0, 2, true, true);
						player.sendMessage("... and teleport out of the Wilderness.");
					}
				});
				break;
			case 5960: //inside magebank lever
				player.getMask().setFacePosition(Location.locate(2539, 4711, 0), 0, 0);
				player.setAttribute("teleportingToWildLever", true);
				TeleportHandler.telePlayer(player, 3090, 3956, 0, 0, 2, true, true);
				break;
			case 5959: //outside magebank lever
				player.getMask().setFacePosition(Location.locate(3089, 3956, 0), 0, 0);
				player.setAttribute("teleportingToWildLever", true);
				TeleportHandler.telePlayer(player, 2539, 4716, 0, 0, 2, true, true);
				break;
			case 28891:
				TeleportHandler.telePlayer(player, 3145, 5555, 0, 0, 2, true, true);
				break;
			case 15314:
				
				break;
			case 28716:
				//summ obelisk now done in PouchCreationListener
				break;
			case 4388:
			case 4408:
			case 4387:
				CastleWarsActivity.getSingleton().register(player, gameObject.getId());
				break;
			case 38811:
				//player.teleport(player.getLocation().getX() > 2972 ? 2970 : 2974, 4384, 2, false);
				if (player.getLocation().getX() < 2973)
					ActionSender.sendInterface(player, 650);
				else
					player.teleport(2970, 4384, 2, false);
					//player.sendMessage("Beware, this monster is very powerful!");
				break;
			case 20608:
					DialogueManager.proceedDialogue(player, 5000);
					break;
			case 48496:
				TeleportHandler.telePlayer(player, 1664, 5696, 0, 0, 0, false, false);
				break;
			case 36972:
				player.animate(645);
				player.getSkills().restorePray(99);
				player.sendMessage("You pray to the gods, and restore your prayer.");
				break;
			/*case 2273:
				TeleportHandler.telePlayer(player, 3105, 3933, 0, 0, 2, true, false);
				player.sendMessage("YOU TELEPORT TO THE WILDERNESS! ITEMS LOST ON DEATH!");
                break;*/
			 case 2274:
			    TeleportHandler.telePlayer(player, Mob.DEFAULT.getX(), Mob.DEFAULT.getY(), Mob.DEFAULT.getZ(), 0, 2, true, false);
                break;
			 case 38700:
				 player.teleport(Mob.DEFAULT, false);
				 break;
			 case 45077:
				 player.teleport(3657, 5114, 0, false);
				 break;
			 case 38699:
				 DialogueManager.proceedDialogue(player, 3029);
				 break;
				case 38698: 
				 DialogueManager.proceedDialogue(player, 3050);
				break;
			case 3804:
				player.teleport(Mob.DEFAULT, false);
				break;
			case 47120:
				if (!player.getPrayer().isAncientCurses()) {
					player.animate(Animation.create(645));
					player.getPrayer().setAnctientCurses(true);
					player.getPrayer().setAncientBook(true);
					player.getPrayer().switchPrayBook(true);
				} else {
					player.animate(Animation.create(645));
					player.getPrayer().setAnctientCurses(false);
					player.getPrayer().setAncientBook(false);
					player.getPrayer().switchPrayBook(false);
				}
					break;
			/*case 27254:
				if (player.getSkills().getCombatLevel() < 110) {
					player.sendMessage("You need a combat level of 110 to enter this portal.");
					return;
				}
				if (gameObject.getLocation().getX() == 3782 && gameObject.getLocation().getY() == 3549) {
					player.teleport(3095, 3497, 0, false);
				} else {
					player.teleport(3784, 3549, 0, false);
				}
				break;*/
			case 5084:
				player.teleport(2744, 3153, 0, false);
				break;
			case 5083:
				player.teleport(2712, 9564, 0, false);
				break;
			case 9706:
				player.animate(2140);
				TeleportHandler.telePlayer(player, 3105, 3951, 0, 0, 2, true, true);
				break;
			case 9707:
				player.animate(2140);
				TeleportHandler.telePlayer(player, 3105, 3956, 0, 0, 2, true, true);
				break;
			case 8783:
				player.teleport(2044, 4649, 0, false);
				break;
			case 8785:
				player.teleport(2543, 3327, 0, false);
				break;
			case 2452:
				if(player.getSkills().getLevel(Skills.RUNECRAFTING) < Talisman.AIR_TALISMAN.getLevel()){
					player.sendMessage("You need a higher runecrafting level to enter.");
					return;
				}
				if(player.getEquipment().getContainer().get(Equipment.SLOT_HAT).getId() == Talisman.AIR_TALISMAN.getTiaraId()){
					player.teleport(Talisman.AIR_TALISMAN.getInsideLocation(), false);
					player.sendMessage("You feel a powerful force take hold of you...");
				}else{
					player.sendMessage("You are not wearing the correct tiara.");
				}
				break;
			case 2453:
				if(player.getSkills().getLevel(Skills.RUNECRAFTING) < Talisman.MIND_TALISMAN.getLevel()){
					player.sendMessage("You need a higher runecrafting level to enter.");
					return;
				}
				if(player.getEquipment().getContainer().get(Equipment.SLOT_HAT).getId() == Talisman.MIND_TALISMAN.getTiaraId()){
					player.teleport(Talisman.MIND_TALISMAN.getInsideLocation(), false);
					player.sendMessage("You feel a powerful force take hold of you...");
				}else{
					player.sendMessage("You are not wearing the correct tiara.");
				}
				break;
			case 2454:
				if(player.getSkills().getLevel(Skills.RUNECRAFTING) < Talisman.WATER_TALISMAN.getLevel()){
					player.sendMessage("You need a higher runecrafting level to enter.");
					return;
				}
				if(player.getEquipment().getContainer().get(Equipment.SLOT_HAT).getId() == Talisman.WATER_TALISMAN.getTiaraId()){
					player.teleport(Talisman.WATER_TALISMAN.getInsideLocation(), false);
					player.sendMessage("You feel a powerful force take hold of you...");
				}else{
					player.sendMessage("You are not wearing the correct tiara.");
				}
				break;
			case 2455:
				if(player.getSkills().getLevel(Skills.RUNECRAFTING) < Talisman.EARTH_TALISMAN.getLevel()){
					player.sendMessage("You need a higher runecrafting level to enter.");
					return;
				}
				if(player.getEquipment().getContainer().get(Equipment.SLOT_HAT).getId() == Talisman.EARTH_TALISMAN.getTiaraId()){
					player.teleport(Talisman.EARTH_TALISMAN.getInsideLocation(), false);
					player.sendMessage("You feel a powerful force take hold of you...");
				}else{
					player.sendMessage("You are not wearing the correct tiara.");
				}
				break;
			case 2456:
				if(player.getSkills().getLevel(Skills.RUNECRAFTING) < Talisman.FIRE_TALISMAN.getLevel()){
					player.sendMessage("You need a higher runecrafting level to enter.");
					return;
				}
				if(player.getEquipment().getContainer().get(Equipment.SLOT_HAT).getId() == Talisman.FIRE_TALISMAN.getTiaraId()){
					player.teleport(Talisman.FIRE_TALISMAN.getInsideLocation(), false);
					player.sendMessage("You feel a powerful force take hold of you...");
				}else{
					player.sendMessage("You are not wearing the correct tiara.");
				}
				break;
			case 2457:
				if(player.getSkills().getLevel(Skills.RUNECRAFTING) < Talisman.BODY_TALISMAN.getLevel()){
					player.sendMessage("You need a higher runecrafting level to enter.");
					return;
				}
				if(player.getEquipment().getContainer().get(Equipment.SLOT_HAT).getId() == Talisman.BODY_TALISMAN.getTiaraId()){
					player.teleport(Talisman.BODY_TALISMAN.getInsideLocation(), false);
					player.sendMessage("You feel a powerful force take hold of you...");
				}else{
					player.sendMessage("You are not wearing the correct tiara.");
				}
				break;
			case 2458:
				if(player.getSkills().getLevel(Skills.RUNECRAFTING) < Talisman.COSMIC_TALISMAN.getLevel()){
					player.sendMessage("You need a higher runecrafting level to enter.");
					return;
				}
				if(player.getEquipment().getContainer().get(Equipment.SLOT_HAT).getId() == Talisman.COSMIC_TALISMAN.getTiaraId()){
					player.teleport(Talisman.COSMIC_TALISMAN.getInsideLocation(), false);
					player.sendMessage("You feel a powerful force take hold of you...");
				}else{
					player.sendMessage("You are not wearing the correct tiara.");
				}
				break;
			case 2459:
				if(player.getSkills().getLevel(Skills.RUNECRAFTING) < Talisman.LAW_TALISMAN.getLevel()){
					player.sendMessage("You need a higher runecrafting level to enter.");
					return;
				}
				if(player.getEquipment().getContainer().get(Equipment.SLOT_HAT).getId() == Talisman.LAW_TALISMAN.getTiaraId()){
					player.teleport(Talisman.LAW_TALISMAN.getInsideLocation(), false);
					player.sendMessage("You feel a powerful force take hold of you...");
				}else{
					player.sendMessage("You are not wearing the correct tiara.");
				}
				break;
			case 2460:
				if(player.getSkills().getLevel(Skills.RUNECRAFTING) < Talisman.NATURE_TALISMAN.getLevel()){
					player.sendMessage("You need a higher runecrafting level to enter.");
					return;
				}
				if(player.getEquipment().getContainer().get(Equipment.SLOT_HAT).getId() == Talisman.NATURE_TALISMAN.getTiaraId()){
					player.teleport(Talisman.NATURE_TALISMAN.getInsideLocation(), false);
					player.sendMessage("You feel a powerful force take hold of you...");
				}else{
					player.sendMessage("You are not wearing the correct tiara.");
				}
				break;
			case 2461:
				if(player.getSkills().getLevel(Skills.RUNECRAFTING) < Talisman.CHAOS_TALISMAN.getLevel()){
					player.sendMessage("You need a higher runecrafting level to enter.");
					return;
				}
				if(player.getEquipment().getContainer().get(Equipment.SLOT_HAT).getId() == Talisman.CHAOS_TALISMAN.getTiaraId()){
					player.teleport(Talisman.CHAOS_TALISMAN.getInsideLocation(), false);
					player.sendMessage("You feel a powerful force take hold of you...");
				}else{
					player.sendMessage("You are not wearing the correct tiara.");
				}
				break;
			case 2465:
				if(gameObject.getLocation().getX() == 2841 && gameObject.getLocation().getY() == 4828){
					player.sendMessage("You step through the portal...");
					player.teleport(Talisman.AIR_TALISMAN.getOutsideLocation(), false);
				}else{
					player.teleport(2687, 9506, 0, false);
				}
				break;
			case 2466:
				if(gameObject.getLocation().getX() == 2793 && gameObject.getLocation().getY() == 4827){
					player.sendMessage("You step through the portal...");
					player.teleport(Talisman.MIND_TALISMAN.getOutsideLocation(), false);
				}else{
					player.teleport(2682, 9506, 0, false);
				}
				break;
			case 2467:
				if(gameObject.getLocation().getX() == 3495 && gameObject.getLocation().getY() == 4832){
					player.sendMessage("You step through the portal...");
					player.teleport(Talisman.WATER_TALISMAN.getOutsideLocation(), false);
				}
				break;
			case 2468:
				if(gameObject.getLocation().getX() == 2655 && gameObject.getLocation().getY() == 4829){
					player.sendMessage("You step through the portal...");
					player.teleport(Talisman.EARTH_TALISMAN.getOutsideLocation(), false);
				}
				break;
			case 2469:
				if(gameObject.getLocation().getX() == 2576 && gameObject.getLocation().getY() == 4846){
					player.sendMessage("You step through the portal...");
					player.teleport(Talisman.FIRE_TALISMAN.getOutsideLocation(), false);
				}
				break;
			case 2470:
				if(gameObject.getLocation().getX() == 2521 && gameObject.getLocation().getY() == 4833){
					player.sendMessage("You step through the portal...");
					player.teleport(Talisman.BODY_TALISMAN.getOutsideLocation(), false);
				}
				break;
			case 2471:
				if(gameObject.getLocation().getX() == 2121 && gameObject.getLocation().getY() == 4833){
					player.sendMessage("You step through the portal...");
					player.teleport(Talisman.COSMIC_TALISMAN.getOutsideLocation(), false);
				}
				break;
			case 2472:
				if(gameObject.getLocation().getX() == 2464 && gameObject.getLocation().getY() == 4817){
					player.sendMessage("You step through the portal...");
					player.teleport(Talisman.LAW_TALISMAN.getOutsideLocation(), false);
				}
				break;
			case 2473:
				if(gameObject.getLocation().getX() == 2400 && gameObject.getLocation().getY() == 4834){
					player.sendMessage("You step through the portal...");
					player.teleport(Talisman.NATURE_TALISMAN.getOutsideLocation(), false);
				}
				break;
			case 2474:
				if(gameObject.getLocation().getX() == 2282 && gameObject.getLocation().getY() == 4837){
					player.sendMessage("You step through the portal...");
					player.teleport(Talisman.CHAOS_TALISMAN.getOutsideLocation(), false);
				}
				break;
			case 2475:
				if(gameObject.getLocation().getX() == 2208 && gameObject.getLocation().getY() == 4829){
					player.sendMessage("You step through the portal...");
					player.teleport(Talisman.DEATH_TALISMAN.getOutsideLocation(), false);
					DialogueManager.sendDisplayBox(player, -1, "You have been teleported to Edgeville, because we do not have","the coordinates for the outside altar.");
				}
				break;
			case 2477:
				if(gameObject.getLocation().getX() == 2468 && gameObject.getLocation().getY() == 4888){
					player.sendMessage("You step through the portal...");
					player.teleport(Talisman.BLOOD_TALISMAN.getOutsideLocation(), false);
					DialogueManager.sendDisplayBox(player, -1, "You have been teleported to Edgeville, because we do not have","the coordinates for the outside altar.");
				}
				break;
			case 2492:
				if(gameObject.getLocation().getX() == 2889 && gameObject.getLocation().getY() == 4813){
					player.sendMessage("You step through the portal...");
					player.teleport(Location.locate(2343, 3684, 0), false);
				}
				if(gameObject.getLocation().getX() == 2933 && gameObject.getLocation().getY() == 4815){
					player.sendMessage("You step through the portal...");
					player.teleport(Location.locate(2343, 3684, 0), false);
				}
				if(gameObject.getLocation().getX() == 2932 && gameObject.getLocation().getY() == 4854){
					player.sendMessage("You step through the portal...");
					player.teleport(Location.locate(2343, 3684, 0), false);
				}
				if(gameObject.getLocation().getX() == 2885 && gameObject.getLocation().getY() == 4850){
					player.sendMessage("You step through the portal...");
					player.teleport(Location.locate(2343, 3684, 0), false);
				}
				break;
			case 5103:
				player.teleport(player.getLocation().getX() == 2691 ? 2689 : 2691, 9564, 0, false);
				break;
			case 5105:
				player.teleport(player.getLocation().getX() == 2672 ? 2674 : 2672, 9499, 0, false);
				break;
			case 5107:
				player.teleport(player.getLocation().getX() == 2695 ? 2693 : 2695, 9482, 0, false);
				break;
			case 5106:
				player.teleport(player.getLocation().getX() == 2676 ? 2674 : 2676, 9479, 0, false);
				break;
			case 5104:
				player.teleport(2683, player.getLocation().getY() == 9570 ? 9568 : 9570, 0, false);
				break;
			case 7129:
				player.teleport(Talisman.FIRE_TALISMAN.getInsideLocation(), false);
				break;
			case 7130:
				player.teleport(Talisman.EARTH_TALISMAN.getInsideLocation(), false);
				break;
			case 7131:
				player.teleport(Talisman.BODY_TALISMAN.getInsideLocation(), false);
				break;
			case 7132:
				player.teleport(Talisman.COSMIC_TALISMAN.getInsideLocation(), false);
				break;
			case 7133:
				player.teleport(Talisman.NATURE_TALISMAN.getInsideLocation(), false);
				break;
			case 7134:
				player.teleport(Talisman.CHAOS_TALISMAN.getInsideLocation(), false);
				break;
			case 7135:
				player.teleport(Talisman.LAW_TALISMAN.getInsideLocation(), false);
				break;
			case 7136:
				player.teleport(Talisman.DEATH_TALISMAN.getInsideLocation(), false);
				break;
			case 7137:
				player.teleport(Talisman.WATER_TALISMAN.getInsideLocation(), false);
				break;
			case 7138:
				DialogueManager.sendDisplayBox(player, -1, "Runescape doesn't have a soul altar silly.");
				break;
			case 7139:
				player.teleport(Talisman.AIR_TALISMAN.getInsideLocation(), false);
				break;
			case 7140:
				player.teleport(Talisman.MIND_TALISMAN.getInsideLocation(), false);
				break;
			case 7141:
				player.teleport(Talisman.BLOOD_TALISMAN.getInsideLocation(), false);
				//DialogueManager.sendDisplayBox(player, -1, "At this time we do not have the location of the blood altar.","If you do, please message an administrator");
				break;
			case 7153:
				player.teleport(Location.locate(3046, 4822, 0), false);
				break;
			case 7152:
				player.teleport(Location.locate(3051, 4824, 0), false);
				break;
			case 7151:
				player.teleport(Location.locate(3053, 4831, 0), false);
				break;
			case 7150:
				player.teleport(Location.locate(3052, 4838, 0), false);
				break;
			case 7149:
				player.teleport(Location.locate(3046, 4843, 0), false);
				break;
			case 7148:
				player.teleport(Location.locate(3038, 4844, 0), false);
				break;
			case 7147:
				player.teleport(Location.locate(3031, 4843, 0), false);
				break;
			case 7146:
				player.teleport(Location.locate(3028, 4840, 0), false);
				break;
			case 7145:
				player.teleport(Location.locate(3025, 4833, 0), false);
				break;
			case 7144:
				player.teleport(Location.locate(3028, 4825, 0), false);
				break;
			case 7143:
				player.teleport(Location.locate(3031, 4822, 0), false);
				break;
			/*case 7152:
				ActionSender.sendConfig(player, 1787, 710180864);
				ActionSender.sendConfig(player, 491, 1077731328);
				player.getWalkingQueue().addToWalkingQueue(3051, 4825);
				break;*/
			case 30557:
				player.teleport(3545, 9826, 0, false);
				break;
			case 30556:
				player.teleport(3549, 9826, 0, false);
				break;
			case 30560:
				player.sendMessage("BEWARE OF THIS MONSTER!");
				player.teleport(2807, 10105, 0, false);
				break;
			case 18050:
				player.teleport(Mob.DEFAULT, false);
				break;
			case 5858:
				player.teleport(3511, 9811, 0, false);
				break;
			case 47236:
				int x = gameObject.getLocation().getX();
				int y = gameObject.getLocation().getY();
				int dir = 0;
				/*if (x > player.getLocation().getX()) {
		            dir = 1;
		        } else if (y < player.getLocation().getY()) {
		            dir = 2;
		        } else if (x < player.getLocation().getX()) {
		            dir = 3;
		        }
		        if(y == player.getLocation().getY() && x != player.getLocation().getX()){
		        	dir = 6;
		        	y = y - 1;
		        }
		        if(x == player.getLocation().getX() && y != player.getLocation().getY()){
		        	dir = 1;
		        	x = x + 1;
		        }
		        if(x == player.getLocation().getX() && y == player.getLocation().getY()){
		        	dir = 1;
		        	y = y -1;
		        }*/
				if(player.getLocation().getX() == x && player.getLocation().getY() < y){
					y = y + 1;
					dir = 2;
				}else if(player.getLocation().getX() == x && player.getLocation().getY() > y){
					y = y - 1;
					dir = 4;
				}else if(player.getLocation().getY() == y && player.getLocation().getX() < x){
					x = x + 1;
					dir = 3;
				}else if(player.getLocation().getY() == y && player.getLocation().getX() > x){
					x = x - 1;
					dir = 6;
				}else if(player.getLocation().getY() == y && player.getLocation().getX() == x){
					y = y - 1;
					x = x + 1;
				}
				player.forceMovement(Animation.create(9516), x, y, 15, 20, dir, 1, true);
				break;
			case 35549:
				break;
			case 11844:
				x = gameObject.getLocation().getX();
				y = gameObject.getLocation().getY();
				dir = 0;
				if (player.getLocation().getY() == y && player.getLocation().getX() < x) {
					x = x + 1;
					dir = 3;
				} else if (player.getLocation().getY() == y && player.getLocation().getX() > x) {
					x = x - 1;
					dir = 6;
				}
				final int toX = x, toY = y, currentDir = dir;
				World.getWorld().submit(new Tick(1) {
					@Override
					public void execute() {
						stop();
						player.forceMovement(toX > gameObject.getLocation().getX() ? Animation.create(12915) : Animation.create(12916), toX, toY, 15, 105, currentDir, 2, true);
					}					
				});
				break;
			case 25016:
			case 25029:
				x = gameObject.getLocation().getX();
				y = gameObject.getLocation().getY();
				dir = 0;
				if (player.getLocation().getX() == x && player.getLocation().getY() < y) {
					y = y + 1;
					dir = 2;
				} else if(player.getLocation().getX() == x && player.getLocation().getY() > y) {
					y = y - 1;
					dir = 4;
				} else if(player.getLocation().getY() == y && player.getLocation().getX() < x) {
					x = x + 1;
					dir = 3;
				} else if(player.getLocation().getY() == y && player.getLocation().getX() > x) {
					x = x - 1;
					dir = 6;
				} else if(player.getLocation().getY() == y && player.getLocation().getX() == x) {
					y = y - 1;
					x = x + 1;
				}
				if (player.getRandom().nextInt(2) == 0) {
					player.sendMessage("You use your strength to push through the wheat in the most efficient fashion.");
				} else {
					player.sendMessage("You use your strength to push through the wheat.");
				}
				final int goX = x, goY = y, curDir = dir;
				player.setAttribute("cantMove", true);
				World.getWorld().submit(new Tick(1) {
					@Override
					public void execute() {
						stop();
						player.forceMovement(Animation.create(6594), goX, goY, 25, 200, -curDir, 6, true);
					}					
				});
				break;
			case 24991:
			case 25014:
				Location destination = Location.locate(2427, 4446, 0);
				if (!ImpetuousImpulses.inPuroPuro(player.getLocation())) {
					destination = Location.locate(2591, 4320, 0);
				}
				TeleportHandler.teleport(player, destination, Animation.create(6601), Graphic.create(1118), Animation.create(-1), Graphic.create(-1), 9, false);
				break;
			default:
				System.out.println(gameObject.getId());
			}
		}
	}

	/*private int getNearestY(int fromY, int toY, int sizeY) {
         int yToWalk = toY;
         int distance = (fromY < toY ? toY - fromY : fromY - toY);
         for (int checkY = toY; checkY < (toY + sizeY + 1); checkY++) {
             if (checkY > fromY && (distance > (checkY - fromY) || checkY - fromY == 0)) {
                 distance = checkY - fromY;
                 yToWalk = checkY;
             } else if (checkY < fromY && (distance > (fromY - checkY)|| fromY - checkY == 0)) {
                 distance = fromY - checkY;
                 yToWalk = checkY;
             }

         }
         return yToWalk;
     }

     private int getNearestX(int fromX, int toX, int sizeX) {
         int xToWalk = toX;
         int distance = (fromX < toX ? toX - fromX : fromX - toX);
         for (int checkX = toX; checkX < (toX + sizeX + 1); checkX++) {
             if (checkX > fromX && (distance > (checkX - fromX)|| checkX - fromX == 0)) {
                 distance = checkX - fromX;
                 xToWalk = checkX;
             } else if (checkX < fromX && (distance > (fromX - checkX)|| fromX - checkX == 0)) {
                 distance = fromX - checkX;
                 xToWalk = checkX;
             }
         }
         return xToWalk;
     }*/


	private PathState findObjectPath(Player player, GameObject object) {
		/*int rotation = object.getRotation();
          int type = object.getType();
          if (type != 10 && type != 11 && type != 22) {
              //Class68_Sub13_Sub3.method713(rotation, type + 1, Class68_Sub7.aClass1_Sub6_Sub2_2860.anIntArray2523[0], Class68_Sub7.aClass1_Sub6_Sub2_2860.anIntArray2570[0], 0, 0, i, true, 0, -3, 2, i_2_);
          } else {
              CacheObjectDefinition def = object.getDefinition();
              int sizeX;
              int sizeY;
              if (rotation == 0 || rotation == 2) {
                  sizeY = def.getSizeX();
                  sizeX = def.getSizeY();
              } else {
                  sizeX = def.getSizeX();
                  sizeY = def.getSizeY();
              }
              int flag = def.getWalkBit();
              if (rotation != 0)
                  flag = (0xf & flag << rotation) + (flag >> 4 - rotation);
              //Class68_Sub13_Sub3.method713(0, 0, Class68_Sub7.aClass1_Sub6_Sub2_2860.anIntArray2523[0], Class68_Sub7.aClass1_Sub6_Sub2_2860.anIntArray2570[0], sizeY, sizeX, i, true, flag, -3, 2, i_2_);
          }*/
		Location to = object.getLocation();
		if (to == BarbarianCourse.PIPE_OBJECT_LOCATION && player.getLocation().getY() >= 3560) {
			to = BarbarianCourse.PIPE_LOCATIONS[0];
		} else if (object.getId() == 43526 && to.getY() == 3550) {
			to = to.transform(0, 4, 0);
		} else if (object.getId() == 57225) {
			if (player.getLocation().getX() <= 2907)
				to = to.transform(0, 2, 0);
			else
				to = to.transform(3, 0, 0);
		}  else if (object.getId() == 1733)
			to = Location.locate(3045, 3926, 0);
		return World.getWorld().doPath(new DefaultPathFinder(), player, to.getX(), to.getY());
	}
	
	public void forceWalk(final Player player, Animation animation, int toX, int toY, int ticks, int[] forceMovement, final String finishMessage) {
		forceWalk(player, animation, toX, toY, ticks, forceMovement, finishMessage, 0.0);
	}
	
	public static void forceWalk(final Player player, Animation animation, int toX, int toY, int ticks, int[] forceMovement, final String finishMessage, final double experience) {
		final boolean running;
		if (forceMovement != null) {
			player.forceMovement(animation, toX, toY, forceMovement[0], forceMovement[1], forceMovement[2], ticks, forceMovement.length < 4);
			reverseActions(player, false);
			running = player.getWalkingQueue().isRunning();
		} else {
			running = player.getWalkingQueue().isRunning();
			player.animate(animation);
			player.getWalkingQueue().setRunToggled(false);
			player.getWalkingQueue().setIsRunning(false);
			player.requestWalk(toX, toY);
			reverseActions(player, false);
		}
		if (experience > 0.0) {
			player.submitTick("restoreAgility", new Tick(ticks) {
				@Override
				public void execute() {
					stop();
					if (experience > 0)
						player.getSkills().addExperience(Skills.AGILITY, experience);
					reverseActions(player, true);
					if (player.getRenderAnimation() > 0) {
						player.resetRenderAnimation();
						player.getMask().setAppearanceUpdate(true);
					}
					if (finishMessage != null)
						player.sendMessage(finishMessage);
					player.animate(Animation.RESET);
					player.getWalkingQueue().setRunToggled(running);
				}
			}, true);
		}
	}
	
	public static void reverseActions(Player player, boolean allowed) {
		if (!allowed) {
			player.setAttribute("cantMove", Boolean.TRUE);
			player.setAttribute("busy", Boolean.TRUE);
		} else {
			player.removeAttribute("cantMove");
			player.removeAttribute("busy");
		}
	}

	private void handleExamine(Player player, int objectId) {
		///player.sendMessage(GameObjectDefinition.forId(objectId).)

	}

	public ClickOption forId(int id) {
		switch (id) {
		case OPTION_1:
			return ClickOption.FIRST;
		case OPTION_2:
			return ClickOption.SECOND;
		case OPTION_3:
			return ClickOption.THIRD;
		case OPTION_4:
			return ClickOption.FOURTH;
		}
		return null;
	}

}
