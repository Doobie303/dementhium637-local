package org.dementhium.net.packethandlers;

import org.dementhium.cache.format.CacheObjectDefinition;
import org.dementhium.content.activity.ActivityManager;
import org.dementhium.content.activity.impl.warriorsguild.AnimationGame;
import org.dementhium.content.areas.CoordinateEvent;
import org.dementhium.content.home.HomeHub;
import org.dementhium.content.dialogue.Dialogue;
import org.dementhium.content.dialogue.DialogueManager;
import org.dementhium.content.dialogue.DialogueType;
import org.dementhium.content.dialogue.OptionAction;
import org.dementhium.content.dialogue.OptionAction.ActionType;
import org.dementhium.content.misc.WaterFilling;
import org.dementhium.content.misc.Burying.Bone;
import org.dementhium.content.skills.cooking.Cooking;
import org.dementhium.content.skills.runecrafting.Talisman;
import org.dementhium.content.skills.smithing.Smithing;
import org.dementhium.model.Item;
import org.dementhium.model.Location;
import org.dementhium.model.World;
import org.dementhium.model.definition.ItemDefinition;
import org.dementhium.model.map.GameObject;
import org.dementhium.model.map.path.DefaultPathFinder;
import org.dementhium.model.map.path.PathState;
import org.dementhium.model.mask.Animation;
import org.dementhium.model.player.Player;
import org.dementhium.model.player.Skills;
import org.dementhium.net.ActionSender;
import org.dementhium.net.PacketHandler;
import org.dementhium.net.message.Message;
import org.dementhium.tickable.Tick;

/**
 * @author 'Mystic Flow <Steven@rune-server.org>
 * @author Steve <golden_32@live.com>
 * @author Lumby <lumbyjr@hotmail.com>
 */
public class ItemOnObjectHandler extends PacketHandler {

	private static final int ITEM_ON_OBJECT = 11;
	private static final class EdgePath {
		private final PathState path;
		private final int distance;
		private final int x;
		private final int y;

		private EdgePath(PathState path, int distance, int x, int y) {
			this.path = path;
			this.distance = distance;
			this.x = x;
			this.y = y;
		}
	}

	@Override
	public void handlePacket(Player player, Message packet) {
		switch (packet.getOpcode()) {
		case ITEM_ON_OBJECT:
			handleItemOnObject(player, packet);
			break;
		}

	}

	private void handleItemOnObject(final Player player, final Message packet) {
		packet.readLEInt(); //not sure
		packet.readLEShortA();
		final int itemUsed = packet.readShort();
		final int objX = packet.readShort();
		packet.readByteS();
		int objectId = packet.readShortA();
		final int objY = packet.readLEShortA();
		final Location location = Location.locate(objX, objY, player.getLocation().getZ());
		if (!player.getInventory().contains(itemUsed))
			return;
		player.closeAll(true, true);
		if (player.getRights() > 1) {
			player.sendMessage("Incoming item on object opcode - id: " + objectId + ", item id: " + itemUsed + ", x: " + objX + ", y:" + objY + ".");
		}
		final GameObject gameObject = location.getGameObject(objectId);
        if (!org.dementhium.model.instance.InstanceAccess.canInteract(player,gameObject)) return;

		if (gameObject == null) {
			return;
		}
		if (gameObject.getId() != objectId) {
			return;
		}
		final CacheObjectDefinition definition = gameObject.getDefinition();
		int eventX = objX;
		int eventY = objY;
		int eventSizeX = definition.getSizeX();
		int eventSizeY = definition.getSizeY();
		if (objectId == HomeHub.Portal.ALTAR.id && location.equals(HomeHub.Portal.ALTAR.location())) {
			EdgePath edge = findObjectEdgePath(player, gameObject, definition);
			if (edge == null) {
				player.sendMessage("I can't reach that!");
				return;
			}
			World.getWorld().doPath(player, edge.path);
			eventX = edge.x;
			eventY = edge.y;
			eventSizeX = 1;
			eventSizeY = 1;
		} else {
			World.getWorld().doPath(new DefaultPathFinder(), player, objX, objY);
		}
		final int objectClicked = objectId;
		final int areaX = eventX;
		final int areaY = eventY;
		final int areaSizeX = eventSizeX;
		final int areaSizeY = eventSizeY;
		World.getWorld().submitAreaEvent(player, new CoordinateEvent(player, areaX, areaY, areaSizeX, areaSizeY) {

			@Override
			public void execute() {
				player.getMask().setFacePosition(gameObject.getLocation(), definition.getSizeX(), definition.getSizeY());
				doObjectAction(player, packet, gameObject, itemUsed, objX, objY, objectClicked, definition);
			}

		});

	}

	private EdgePath findObjectEdgePath(Player player, GameObject object, CacheObjectDefinition definition) {
		int width = definition.getSizeX();
		int height = definition.getSizeY();
		if ((object.getRotation() & 1) != 0) {
			int swap = width;
			width = height;
			height = swap;
		}
		int minX = object.getLocation().getX();
		int minY = object.getLocation().getY();
		EdgePath best = null;
		for (int x = minX; x < minX + width; x++) {
			best = nearerEdgePath(player, x, minY - 1, best);
			best = nearerEdgePath(player, x, minY + height, best);
		}
		for (int y = minY; y < minY + height; y++) {
			best = nearerEdgePath(player, minX - 1, y, best);
			best = nearerEdgePath(player, minX + width, y, best);
		}
		return best;
	}

	private EdgePath nearerEdgePath(Player player, int x, int y, EdgePath best) {
		int distance = Math.abs(player.getLocation().getX() - x) + Math.abs(player.getLocation().getY() - y);
		if (best != null && distance >= best.distance) {
			return best;
		}
		PathState path = World.getWorld().doPath(new DefaultPathFinder(), player, x, y, false, false, true);
		if (!reaches(path, player, x, y)) {
			return best;
		}
		return new EdgePath(path, distance, x, y);
	}

	private boolean reaches(PathState path, Player player, int x, int y) {
		if (path == null || !path.isRouteFound()) {
			return false;
		}
		if (player.getLocation().getX() == x && player.getLocation().getY() == y) {
			return true;
		}
		if (path.getPoints().isEmpty()) {
			return false;
		}
		org.dementhium.model.map.Position destination = path.getPoints().getLast();
		return destination.getX() == x && destination.getY() == y;
	}

	protected void doObjectAction(final Player player, Message packet, GameObject object, int itemUsed, int objX, int objY, int objId, CacheObjectDefinition definition) {
        if (!org.dementhium.model.instance.InstanceAccess.canInteract(player,object)) return;
		ActionSender.sendCloseChatBox(player);
		String name = definition.getName().toLowerCase();
		if (player.getActivity().itemAction(player, new Item(itemUsed, 1), 0, "ItemOnObject", object)) {
			return;
		}
		CacheObjectDefinition def = object.getDefinition();
		if (def.getName().toLowerCase().contains("anvil")) {
			if (itemUsed == 11286 || itemUsed == 1540) {
				DialogueManager.sendDialogue(player, 0);
				return;
			} else if (itemUsed == 13736) {
				if (player.getInventory().contains(13746)) {
					DialogueManager.sendDialogue(player, 17);
					return;
				} else if (player.getInventory().contains(13748)) {
					DialogueManager.sendDialogue(player, 27);
					return;
				} else if (player.getInventory().contains(13750)) {
					DialogueManager.sendDialogue(player, 29);
					return;
				} else {
					DialogueManager.sendDialogue(player, 31);
					return;
				}
			} else if (itemUsed == 13746) {
				DialogueManager.sendDialogue(player, 17);
				return;
			} else if (itemUsed == 13748) {
				DialogueManager.sendDialogue(player, 27);
				return;
			} else if (itemUsed == 13750) {
				DialogueManager.sendDialogue(player, 29);
				return;
			} else if (itemUsed == 13752) {
				DialogueManager.sendDialogue(player, 31);
				return;
				
			//Godsword Blade making (attach Hilt for Godsword):	
			} else if (itemUsed == 11710) { //gs shard 1
				if (player.getInventory().contains(11712)) { //shard 1 + 2
					DialogueManager.sendDialogue(player, 35);
					return;
				} else if (player.getInventory().contains(11714)) { //shard 1 + 3
					DialogueManager.sendDialogue(player, 44);
					return;
				} else if (player.getInventory().contains(11692)) { //shard 1 + shards (2 + 3)
					DialogueManager.sendDialogue(player, 46);
					return;
				} else {
					DialogueManager.sendDialogue(player, 35);
					return;
				}
			} else if (itemUsed == 11712) { //gs shard 2
				if (player.getInventory().contains(11714)) { //shard 2 + 3
					DialogueManager.sendDialogue(player, 45);
					return;
				} else if (player.getInventory().contains(11710)) { //shard 2 + 1
					DialogueManager.sendDialogue(player, 35);
					return;
				} else if (player.getInventory().contains(11688)) { //shard 2 + shards (1 + 3)
					DialogueManager.sendDialogue(player, 47);
					return;
				} else {
					DialogueManager.sendDialogue(player, 35);
					return;
				}
			} else if (itemUsed == 11714) { //gs shard 3
				if (player.getInventory().contains(11712)) { //shard 3 + 2
					DialogueManager.sendDialogue(player, 45);
					return;
				} else if (player.getInventory().contains(11714)) { //shard 3 + 1
					DialogueManager.sendDialogue(player, 44);
					return;
				} else if (player.getInventory().contains(11686)) { //shard 3 + shards (1 + 2)
					DialogueManager.sendDialogue(player, 36);
					return;
				} else {
					DialogueManager.sendDialogue(player, 45);
					return;
				}
			} else if (itemUsed == 11686) { //gs shards (1 + 2)
				DialogueManager.sendDialogue(player, 36);
				return;
			} else if (itemUsed == 11688) { //gs shards (1 + 3)
				DialogueManager.sendDialogue(player, 47);
				return;
			}else if (itemUsed == 11692) { //gs shards (2 + 3)
				DialogueManager.sendDialogue(player, 46);
				return;
			}
			
		} else if (def.getName().toLowerCase().equals("altar")
				|| objId == HomeHub.Portal.ALTAR.id && object.getLocation().equals(HomeHub.Portal.ALTAR.location())) {
			if (itemUsed == 13734 || itemUsed == 13754) {
				if (player.getSkills().getLevel(Skills.PRAYER) < 85) {
					Dialogue dial = new Dialogue();
					dial.setType(DialogueType.DISPLAY_BOX);
					dial.getMessage().add("You need a prayer level of 85 to bless this shield.");
					dial.getActions().add(OptionAction.create(ActionType.CLOSE_DIALOGUE));
					dial.send(player);
					return;
				}
				if (!player.getInventory().contains(13734)) {
					player.sendMessage("You do not have a spirit shield to bless.");
					return;
				} else if (!player.getInventory().contains(13754)) {
					player.sendMessage("You need holy elixir to bless a spirit shield.");
					return;
				}
				player.getSkills().addExperience(Skills.PRAYER, 1500);
				player.animate(Animation.create(645));
				player.getInventory().getContainer().remove(new Item(13734));
				player.getInventory().getContainer().remove(new Item(13754));
				player.getInventory().addItem(new Item(13736));
				player.sendMessage("You bless the spirit shield.");
				return;
			}
			//Bones on altar:
			final Bone bone = Bone.bones.get(itemUsed);
			if (bone != null) {
				if (!player.hasTick("boneOnAltar") && player.getInventory().contains(itemUsed)) {
					//TODO:
					/*
					 * Send interface with: 
					 * -sword here- "How many would you like to offer?" -sword here-
					 * Right click the object to see more options
					 * 
					 * -bone image here (clickable)-
					 * -bone name here-
					 */
					//player.getMask().setFacePosition(object.getLocation(), object.getDefinition().getSizeX(), object.getDefinition().getSizeY());
					player.animate(3705);
					ActionSender.spawnPositionedGraphic(object.getLocation(), 624);
					player.getInventory().deleteItem(itemUsed, 1);
					player.getInventory().refresh();
	                player.submitTick("burying", new Tick(2) {
	                    @Override
	                    public void execute() {
	                        stop();
	    					player.getSkills().addExperience(Skills.PRAYER, bone.getExperience() * 2);
	                        player.sendMessage("The gods are very pleased with your offering.");
	                    }
	                });
				}
				return;
			}
		}
		if (Smithing.itemOnObjectInteraction(player, itemUsed, objId)) {
			return;
		}
		if(WaterFilling.isWaterItem(itemUsed) && name.contains("sink") || WaterFilling.isWaterItem(itemUsed) && name.contains("fountain") || WaterFilling.isWaterItem(itemUsed) && name.contains("well") || WaterFilling.isWaterItem(itemUsed) && name.contains("geyser") || WaterFilling.isWaterItem(itemUsed) && name.contains("waterpump")){
			WaterFilling waterfilling = new WaterFilling(player, itemUsed);
			player.submitTick("skill_action_tick", waterfilling, true);
			return;
		}
		if (objId == 15621 && AnimationGame.isArmourPiece(itemUsed)) {
			player.setActivity(new AnimationGame(player, itemUsed, objId));
			ActivityManager.getSingleton().register(player.getActivity());
			return;
		}
		if (Cooking.itemForId(player, itemUsed, objId) != null) {
			player.setAttribute("cookingObj", objId);
			Cooking.showInterface(player, Cooking.itemForId(player, itemUsed, objId), itemUsed);
			return;
		}
		Talisman talisman = Talisman.forId(itemUsed);
		if (talisman != null) {
			if(talisman.getObjectId() == objId){
				if(player.getSkills().getLevel(Skills.RUNECRAFTING) < talisman.getLevel()){
					player.sendMessage("You need a higher runecrafting level to enter.");
					return;
				}
				player.sendMessage("You hold the "+ItemDefinition.forId(talisman.getId()).getName()+" towards the mysterious ruins.");
				player.teleport(talisman.getInsideLocation(), false);
				player.sendMessage("You feel a powerful force take hold of you...");
				return;
			}
		}
		switch (objId) {
		}
		player.sendMessage("Nothing interesting happens.");
		System.out.println("Item on Object ID: "+objId);
	}


}
