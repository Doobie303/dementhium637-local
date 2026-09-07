package org.dementhium.content;

import java.text.NumberFormat;

import org.dementhium.cache.format.CacheNPCDefinition;
import org.dementhium.content.activity.impl.DuelActivity;
import org.dementhium.content.activity.impl.WarriorsGuildMinigame;
import org.dementhium.content.cutscenes.Cutscene;
import org.dementhium.content.cutscenes.impl.TutorialScene;
import org.dementhium.content.misc.RepairItem;
import org.dementhium.content.skills.magic.TeleportHandler;
import org.dementhium.event.impl.SpiritTreeListener;
import org.dementhium.model.Item;
import org.dementhium.model.Mob;
import org.dementhium.model.World;
import org.dementhium.model.definition.ItemDefinition;
import org.dementhium.model.map.Position;
import org.dementhium.model.mask.Animation;
import org.dementhium.model.mask.Graphic;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.npc.impl.Nex.NexAreaEvent;
import org.dementhium.model.player.Equipment;
import org.dementhium.model.player.Player;
import org.dementhium.model.player.Skills;
import org.dementhium.net.ActionSender;
import org.dementhium.net.packethandlers.ObjectPacketHandler;
import org.dementhium.tickable.Tick;
import org.dementhium.util.Constants;
import org.dementhium.util.Logger;
import org.dementhium.util.Misc;
import org.dementhium.util.handlers.OffencesHandler;
import java.util.Random;

/**
 * @author 'Mystic Flow <Steven@rune-server.org>
 */
public class DialogueManager {
	

	public static boolean proceedDialogue(final Player player, int stage) {
		if (player.getSettings().getSpeakingTo() != null && player.getSettings().getSpeakingTo().isNPC()) { //could also speak to object.
			NPC npc = (NPC) player.getSettings().getSpeakingTo();
			npc.setDialogueStage(stage);
		}
		if (stage >= 17 && stage <= 19 || stage == 176) {
			sendDialogue(player, 9827, 1513, 20, "Have fun.");
		}
		if (stage >= 416 && stage <= 419) {
			sendDialogue(player, 9827, 5113, 20, "Happy Hunting.");
		}
		if (stage >= 235 && stage <= 245 || stage == 174) {
			sendDialogue(player, 9827, player.getSettings().getSpeakingTo().getNPC().getId(), 250, "Have fun.");
		}
		if ((stage >= 21 && stage <= 29) || (stage >= 57 && stage <= 69) || stage == 326 || stage == 411) {
			return SpiritTreeListener.handleDialogue(player, stage);
		}
		if ((stage >= 256 && stage <= 318) || (stage >= 420 && stage <= 560)) {
			return WarriorsGuildMinigame.handleDialogue(player, stage);
		}
		if (stage >= 2000 && stage <= 3000) {
			return player.getSlayer().handleDialouge(stage);
		}
		if(stage >= 579 && stage <= 583){
			return SkillCapes.handleDialogue(player, stage);
		}
		switch (stage) {
		case 4:
			sendDialogue(player, HAPPY_TALKING, 2244, -1, "Hello, @PLAYER_NAME@ what would you like?");
			return true;
		case 5:
			sendOptionDialogue(player, new int[]{6, 7, -1}, "Rule Book", "Guide Book", "Nevermind, I have both.");
			return true;
		case 6:
			if (!player.getInventory().contains(757) && !player.getBank().contains(757)) {
				player.getInventory().addItem(757, 1);
				player.sendMessage("You receive a rule book from Roddeck.");
			} else {
				player.sendMessage("You have already received a rule book! Check your bank or inventory.");
			}
			return false;
		case 9050:
			sendOptionDialogue(player, new int[]{9051, 6669, -1}, "Sure (random amount)", "Sure (set amount)", "No, I'm quite alright thank you!");
			return true;
		case 6669:
			sendDisplayBox(player, 6670, "Please select the amount of money you wish to bet!");
			return false;
		case 6670:
			sendOptionDialogue(player, new int[]{6671, 6672, 6673, 6674, -1}, "10M", "50M", "100M", "500M", "Nevermind..");
			return true;
		case 6671:
			if (!player.getInventory().contains(995, 10000000)) {
				sendDialogue(player, MEAN_FACE, 2998, -1, "You need 10m!");
				return true;
			} else { 
				sendDialogue(player, HAPPY_TALKING, 2998, 9053, "Okay, a 10M bet it is!");
				player.setBet(10000000);	
			   return true;
			}
		case 6672:
			if (!player.getInventory().contains(995, 50000000)) {
				sendDialogue(player, MEAN_FACE, 2998, -1, "You need 50m!");
				return true;
			} else { 
				sendDialogue(player, HAPPY_TALKING, 2998, 9053, "Okay, a 50M bet it is!");
				player.setBet(50000000);	
			   return true;
			}
		case 6673:
			if (!player.getInventory().contains(995, 100000000)) {
				sendDialogue(player, MEAN_FACE, 2998, -1, "You need 100m!");
				return true;
			} else { 
				sendDialogue(player, HAPPY_TALKING, 2998, 9053, "Okay, a 100M bet it is!");
				player.setBet(100000000);	
			   return true;
			}
		case 6674:
			if (!player.getInventory().contains(995, 500000000)) {
				sendDialogue(player, MEAN_FACE, 2998, -1, "You need 500m!");
				return true;
			} else { 
				sendDialogue(player, HAPPY_TALKING, 2998, 9053, "Okay, a 500M bet it is!");
				player.setBet(500000000);	
			   return true;
			}
		case 9051:
			sendDialogue(player, HAPPY_TALKING, -1, -1, "Sure, why not.");
			return false;
		case 9052:
		Random bet = new Random();
		for (int counter = 1; counter <= 1; counter++) {
			player.bet = 1 + bet.nextInt(9999999);
			
		if (!player.getInventory().contains(995, player.getBet())) {
			sendDialogue(player, MEAN_FACE, 2998, -1, "You need some money to play me!");
			return true;
		} else if (player.getBet() >= 1000) {
			sendDialogue(player, HAPPY_TALKING, 2998, 9053, "I'll start off with a bet of " + player.bet / 1000 + "K.");
			player.setBet(player.bet);
		}
	}
			return true;
		case 9053:
			World.getWorld().getNpcs().getById(2998).animate(Animation.create(11900, 0));
			World.getWorld().getNpcs().getById(2998).graphics(Graphic.create(2075, 0));
			sendDisplayBox(player, 9056, "Rolling...");
			player.animate(Animation.create(11900, 0));
			player.graphics(Graphic.create(2075, 0));
			return true;
		case 9056:
		for (int counter = 1; counter <= 1; counter++) {
			int chance = Misc.random(Commands.diceChance ? 60 : 1, 100);
			int chance_ = Misc.random(Commands.diceChance ? 50 : 1, 100);
			player.roll = chance;
			player.npcRoll = chance_;
			
		if (player.npcRoll < player.roll) {
			player.setAttribute("cantMove", Boolean.TRUE);
			sendDisplayBox(player, 9054, "You rolled a <col=FF0000>"+ player.roll +"</col> the Gambler rolled a <col=FF0000>"+ player.npcRoll +"</col> on the percentile dice,", "congratulations you have won!");
			player.getInventory().addDropable(new Item(995, player.getBet()));
			player.getInventory().refresh();
		} else if (player.npcRoll == player.roll) {
			player.setAttribute("cantMove", Boolean.TRUE);
			sendDisplayBox(player, -1, "You rolled a <col=FF0000>"+ player.roll +"</col> the Gambler rolled a <col=FF0000>"+ player.npcRoll +"</col> on the percentile dice,", "looks like it's a tie!");
		} else if (player.npcRoll > player.roll) {
			player.setAttribute("cantMove", Boolean.TRUE);
			player.getInventory().deleteItem(995, player.getBet());
			player.getInventory().refresh();
			sendDisplayBox(player, 9055, "You rolled a <col=FF0000>"+ player.roll +"</col> the Gambler rolled a <col=FF0000>"+ player.npcRoll +"</col> on the percentile dice,", "sorry you have lost!");
		}
	}
			return true;
		case 9054:
		if (!player.getInventory().contains(995, player.getBet())) {
			player.setAttribute("cantMove", Boolean.FALSE);
			player.sendMessage("You need that amount of cash to earn the money.");
			return true;
		} else if (player.getBet() >= 1000) {
			player.setAttribute("cantMove", Boolean.FALSE);
			//player.getInventory().addItem(995, player.getBet());
			sendDisplayBox(player, -1, "You have recieved " + player.getBet() / 1000 + "<col=FF0000>K</col>  from the gambler.");
		} else {
			player.setAttribute("cantMove", Boolean.FALSE);
			//player.getInventory().addItem(995, player.getBet());
			sendDisplayBox(player, -1, "You have recieved " + player.getBet() + "<col=FF0000>K</col>  from the gambler.");
		}
			return true;
		case 9055:
		if (!player.getInventory().contains(995, player.getBet())) {
			player.setAttribute("cantMove", Boolean.FALSE);
			player.sendMessage("You need that amount of cash to earn the money.");
			return true;
		} else if (player.getBet() >= 1000) {
			player.setAttribute("cantMove", Boolean.FALSE);
			//player.getInventory().deleteItem(995, player.getBet());
			sendDisplayBox(player, -1, "You have lost " + player.getBet() / 1000 + "<col=FF0000>K</col> from the gambler.");
		} else {
			player.setAttribute("cantMove", Boolean.FALSE);
			//player.getInventory().deleteItem(995, player.getBet());
			sendDisplayBox(player, -1, "You have lost " + player.getBet() + "<col=FF0000>K</col> from the gambler.");
		}
			return true;
		case 7:
			if (!player.getInventory().contains(1856) && !player.getBank().contains(1856)) {
				player.getInventory().addItem(1856, 1);
				player.sendMessage("You receive a guide book from Roddeck.");
			} else {
				player.sendMessage("You have already received a guide book! Check your bank or inventory.");
			}
			return false;
		case 13:
			sendOptionDialogue(player, new int[]{14, -1}, "Take me somewhere!", "Nothing.");
			return true;
		case 14:
			sendDialogue(player, HAPPY_TALKING, -1, 15, "Take me somewhere!");
			return true;
		case 17:
			player.setAttribute("teleportDestination", Position.create(3212, 3423, 0));
			return true;
		case 18:
			player.setAttribute("teleportDestination", Position.create(3087, 3491, 0));
			return true;
		case 19:
			player.setAttribute("teleportDestination", Position.create(3222, 3222, 0));
			return true;
		case 174:
			player.setAttribute("teleportDestination", Position.create(2804, 3419, 0));
			return true;
		case 175:
			sendOptionDialogue(player, new int[]{176, -1}, "Falador", "Nowhere");
			return true;
		case 176:
			player.setAttribute("teleportDestination", Position.create(2965, 3386, 0));
			return true;
		case 20:
			Position loc = player.getAttribute("teleportDestination");
			if (loc != null) {
				NPC mage = player.getSettings().getSpeakingTo().getNPC();
				mage.animate(1979);
				TeleportHandler.telePlayer(player, loc.getX(), loc.getY(), loc.getZ(), 2, 2, false, true);
			}
			return false;
		case 31:
			sendOptionDialogue(player, new int[]{32, 35, -1}, "Do you have any items for sale?", "Who are you?", "Nothing");
			return true;
		case 32:
			sendDialogue(player, CONFUSED, -1, 33, "Do you have any items for sale?");
			return true;
		case 33:
			sendDialogue(player, HAPPY_TALKING, 8009, 34, "Of course I do! Take a look!");
			return true;
		case 34:
			World.getWorld().getShopManager().openShop(player, 8009);
			return false;
		case 35:
			sendDialogue(player, CONFUSED, -1, 36, "Who are you?");
			return true;
		case 36:
			sendDialogue(player, HAPPY_TALKING, 8009, 37, "Who am I?!", "My dear boy, you have much to learn!", "I'm the great Max the Traveller!");
			return true;
		case 37:
			sendDialogue(player, TALKING_ALOT, 8009, 38, "I travel the world, exploring every forest,", "and every city! I've collected many items from my", " journeys, would you like to take a look?");
			return true;
		case 38:
			sendOptionDialogue(player, new int[]{34, -1}, "Yes please!", "No thanks.");
			return true;
		case 45:
			if (player.getSettings().getSpeakingTo().getNPC().getId() == 587)
				sendOptionDialogue(player, new int[]{46, 48, 712}, "Yeah I'll take a look!", "No thanks, but who are you?", "No, thank you.");
			else
				sendOptionDialogue(player, new int[]{46, 712}, "Yeah I'll take a look!", "No, thank you.");
			return true;
		case 46:
			sendDialogue(player, HAPPY_TALKING, -1, 47, "Yeah I'll take a look!");
			return true;
		case 47:
			World.getWorld().getShopManager().openShop(player, player.getSettings().getSpeakingTo().getNPC().getId());
			return false;
		case 48:
			sendDialogue(player, CONFUSED, -1, 49, "No thanks, but who are you?");
			return true;
		case 49:
			sendDialogue(player, HAPPY_TALKING, 587, 50, "Hahahaha! Young man, I'm the great...", "Jatix!");
			return true;
		case 50:
			sendDialogue(player, CONFUSED, -1, 51, "I've never heard of you?");
			return true;
		case 51:
			sendDialogue(player, CONFUSED, 587, 52, "Really? I guess I'm only well known back", "in Taverly. Oh well...");
			return true;
		case 52:
			sendDialogue(player, CONFUSED, -1, 53, "If you're so great, what are you doing here?!");
			return true;
		case 53:
			sendDialogue(player, TALKING_ALOT, 587, 54, "Well you see, I've been studying Herblore", "for all my life. I've gotten just about every herb and", "ingredient combination documented. There's just...");
			return true;
		case 54:
			sendDialogue(player, DEPRESSED, 587, 55, "One more herb that I have yet to document.", "I've heard rumors it is located around this area, but ", "I've had no luck in locating it.", "It seems like it might not even exist.");
			return true;
		case 55:
			sendDialogue(player, TALKING_ALOT, 587, 251, "Would you like to go to Taverly now though?");
			return true;
		case 97:
			sendOptionDialogue(player, new int[]{98, -1}, "Yeah I'll take a look!", "No thank you.");
			return true;
		case 98:
			World.getWorld().getShopManager().openShop(player, 550);
			return false;
		case 70:
			sendOptionDialogue(player, new int[]{71, -1}, "Of course!", "No I'm scared!");
			return true;
		case 71:
			NPC kol = player.getSettings().getSpeakingTo().getNPC();
			kol.animate(Animation.create(0x3172));
			kol.forceText("Abra-ca-dabra!");
			TeleportHandler.telePlayer(player, 2540, 4716, 0, 0, 2, false, false);
			return false;
		case 105:
			sendOptionDialogue(player, new int[]{106, -1}, "View Shop", "Exit");
			return true;
		case 106:
			World.getWorld().getShopManager().openShop(player, 520);
			return false;
		case 107:
		case 108:
			if (player.getInventory().addItem(9005 + (stage - 107), 1)) {
				player.getSettings().getStrongholdChest()[3] = true;
				return false;
			}
			player.setAttribute("closeInterface", true);
			ActionSender.sendChatboxInterface(player, 131);
			ActionSender.sendString(player, 131, 1, "You need atleast one inventory space to get your reward.");
			ActionSender.sendItemOnInterface(player, 131, 0, 1, 9005);
			ActionSender.sendItemOnInterface(player, 131, 2, 1, 9006);
			return true;
		case 109:
			final boolean hasBoots = player.getEquipment().contains(9005) || player.getEquipment().contains(9006)
			|| player.getInventory().contains(9005) || player.getInventory().contains(9006)
			|| player.getBank().contains(9005) || player.getBank().contains(9006);
			if (!hasBoots) {
				if (player.getSettings().getStrongholdChest()[3]) {
					sendDisplayBox(player, 110, "Welcome adventurer... you appear to have lost your boots.");
				} else {
					sendDisplayBox(player, 110, "Welcome adventurer... you will be rewarded by a pair of boots.");
				}
				return true;
			}
			sendDisplayBox(player, -1, "You already have a pair of boots.");
			return true;
		case 110:
			player.setAttribute("chooseBoots", true);
			ActionSender.sendChatboxInterface(player, 131);
			ActionSender.sendString(player, 131, 1, "You can choose between these two pairs of boots.");
			ActionSender.sendItemOnInterface(player, 131, 0, 1, 9005);
			ActionSender.sendItemOnInterface(player, 131, 2, 1, 9006);
			return true;
		case 111:
			sendOptionDialogue(player, new int[]{112, 113}, "I'll take the colourful ones.", "I'll take the fighting ones.");
			return true;
		case 112:
			sendDialogue(player, CALM_TALK, -1, 114, "I'll take the colourful ones.");
			return true;
		case 113:
			sendDialogue(player, CALM_TALK, -1, 115, "I'll take the fighting ones.");
			return true;
		case 114:
			if (!player.getInventory().addItem(9005, 1)) {
				sendDisplayBox(player, -1, "You need atleast one spot in your inventory to claim your reward.");
				return false;
			}
			player.getSettings().getStrongholdChest()[3] = true;
			player.setAttribute("chooseBoots", false);
			player.setAttribute("closeInterface", true);
			ActionSender.sendChatboxInterface(player, 131);
			ActionSender.sendString(player, 131, 1, "Enjoy your boots.");
			ActionSender.sendItemOnInterface(player, 131, 2, 1, 9005);
			return true;
		case 115:
			if (!player.getInventory().addItem(9006, 1)) {
				sendDisplayBox(player, -1, "You need atleast one spot in your inventory to claim your reward.");
				return false;
			}
			player.getSettings().getStrongholdChest()[3] = true;
			player.setAttribute("chooseBoots", false);
			player.setAttribute("closeInterface", true);
			ActionSender.sendChatboxInterface(player, 131);
			ActionSender.sendString(player, 131, 1, "Enjoy your boots.");
			ActionSender.sendItemOnInterface(player, 131, 2, 1, 9006);
			return true;

		case 117:
			DialogueManager.sendOptionDialogue(player, new int[]{118, 120, -1}, "How did you get here?", "Take me somewhere!", "Nothing, sorry for bothering you.");
			return true;
		case 118:
			DialogueManager.sendDialogue(player, DialogueManager.CONFUSED, -1, 119, "How did you get here?");
			return true;
		case 119:
			DialogueManager.sendDialogue(player, DialogueManager.DEPRESSED, 37, 22, "I'd prefer not to talk about it.");
			return true;
		case 120:
			DialogueManager.sendDialogue(player, DialogueManager.HAPPY_TALKING, -1, 121, "Take me somewhere!");
			return true;
		case 121:
			DialogueManager.sendDialogue(player, DialogueManager.HAPPY_TALKING, 37, 122, "Where would you like to go?");
			return true;
		case 122:
			DialogueManager.sendOptionDialogue(player, new int[]{123, -1}, "Stronghold of security", "Nevermind");
			return true;
		case 123:
			player.setAttribute("teleportDestination", Position.create(1860, 5244, 0));
			break;
		case 124:
			sendDialogue(player, CALM_TALK, -1, 127, "Who are you?");
			return true;
		case 127:
			sendDialogue(player, TOUGH, 705, 128, "My name is Harlan, a master of defence!");
			return true;
		case 128:
			sendDialogue(player, CONFUSED, -1, 129, "What do you do here?");
			return true;
		case 129:
			sendDialogue(player, CALM_TALK, 705, 130, "I assist new adventurers in learning the ways of melee",
					"combat. It is a dangerous but worthwile study. There",
					"is nothing like the feeling of wading into battle against",
					"many foes.");
			return true;
		case 130:
			sendOptionDialogue(player, new int[]{125, 126}, "What is this place?", "What is that cape you're wearing?");
			return true;
		case 125:
			sendDialogue(player, CONFUSED, -1, 131, "What is this place?");
			return true;
		case 131:
			sendDialogue(player, CALM_TALK, 705, 132, "This is a safe place for people to train combat. We have",
					"areas for each corner of the combat triangle. I'm in",
					"charge of the melee area, although I admit that the",
					"melee training isn't so complicated. Just click to attack");
			return true;
		case 132:
			sendDialogue(player, CALM_TALK, 705, 133, "the melee dummies.");
			return true;
		case 133:
			sendDialogue(player, HAPPY_TALKING, -1, 134, "That's great! This is much safer than fighting goblins or",
					"spiders.");
			return true;
		case 134:
			sendDialogue(player, CALM_TALK, 705, 135, "Well, that is true, but it is no replacement for real",
					"training. You won't get very much experience while",
					"training here. This place is just intended as a practice",
					"arena. When you feel ready, you should face real");
			return true;
		case 135:
			sendDialogue(player, CALM_TALK, 705, -1, "enemies to get better experience.");
			return true;
		case 126:
			sendDialogue(player, CONFUSED, -1, 136, "What is that cape you're wearing?");
			return true;
		case 136:
			sendDialogue(player, CALM_TALK, 705, 137, "Ah, this is a Skillcape of Defence. I have mastered the",
					"art of defence and wear it proudly to show others.");
			return true;
		case 137:
			sendDialogue(player, CALM_TALK, -1, 138, "Hmm, interesting.");
			return true;
		case 138:
			sendOptionDialogue(player, new int[]{139, 163, -1}, "Please tell me more about skillcapes.", "Could I have one?", "Bye.");
			return true;
		case 139:
			sendDialogue(player, CALM_TALK, -1, 140, "Please tell me more about skillcapes.");
			return true;
		case 140:
			sendDialogue(player, CALM_TALK, 705, -1, "Of course. Skillcapes are a symbol of achievement. Only",
					"people who have mastered a skill and reached level 99",
					"can get their hands on them and gain the benefits they",
					"carry.");
			return true;
		case 141:
			sendDialogue(player, CONFUSED, -1, 143, "Who are you?");
			return true;
		case 143:
			sendDialogue(player, CALM_TALK, 4707, 144, "My name is Mikasi.");
			return true;
		case 144:
			sendDialogue(player, CONFUSED, -1, 145, "What do you do here?");
			return true;
		case 145:
			sendDialogue(player, CALM_TALK, 4707, 146, "I travelled the world for many years, training my",
					"Magic, but I decided to settle down. So I founded this",
					"practice arena with Harlan and Nemart so we could",
					"help new adventurers on their journey to skill mastery.");
			return true;
		case 146:
			sendOptionDialogue(player, new int[]{142, -1}, "What is this place?", "Nothing.");
			return true;
		case 142:
			sendDialogue(player, CONFUSED, -1, 147, "What is this place?");
			return true;
		case 147:
			sendDialogue(player, CALM_TALK, 4707, 148, "This is a safe place for people to train combat. We have",
					"areas for each corner of the combat triangle. I'm in",
					"charge of the magic area. If you've not got any runes,",
					"I've heard Aubury's rune shop in Varrock sometimes");
			return true;
		case 148:
			sendDialogue(player, CALM_TALK, 4707, 149, "has some free samples. When you're ready, simply cast",
					"your combat spells at these blue dummies.");
			return true;
		case 149:
			sendDialogue(player, HAPPY_TALKING, -1, 150, "That's great! This is much safer than fighting goblins or",
					"spiders.");
			return true;
		case 150:
			sendDialogue(player, CALM_TALK, 4707, 151, "Well, that is true, but it is no replacement for real",
					"training. You won't get very much experience while",
					"training here. This place is just intended as a practice",
					"arena. When you feel ready, you should face real");
			return true;
		case 151:
			sendDialogue(player, CALM_TALK, 4707, -1, "enemies to get better experience.");
			return true;
		case 152:
			sendDialogue(player, CONFUSED, -1, 154, "Who are you?");
			return true;
		case 154:
			sendDialogue(player, CALM_TALK, 1861, 155, "My name is Nemarti.");
			return true;
		case 155:
			sendDialogue(player, CONFUSED, -1, 156, "What do you do here?");
			return true;
		case 156:
			sendDialogue(player, CALM_TALK, 1861, 157, "I am a skilled ranger, but felt I should contribute by",
					"helping others become skilled rangers.");
			return true;
		case 157:
			sendOptionDialogue(player, new int[]{153, -1}, "What is this place?", "Nothing.");
			return true;
		case 153:
			sendDialogue(player, CONFUSED, -1, 158, "What is this place?");
			return true;
		case 158:
			sendDialogue(player, CALM_TALK, 1861, 159, "This is a safe place for people to train combat. We have",
					"areas for each corner of the combat triangle. I'm in",
					"charge of the ranged area. To use this area you'll need",
					"some ranged equipment. I'd recommend Lowe's archery");
			return true;
		case 159:
			sendDialogue(player, CALM_TALK, 1861, 160, "store in Varrock. He may even have some free samples.",
					"When you have a bow and arrow wielded, simply click",
					"on a target to shoot at it.");
			return true;
		case 160:
			sendDialogue(player, HAPPY_TALKING, -1, 161, "That's great! This is much safer than fighting goblins or",
					"spiders.");
			return true;
		case 161:
			sendDialogue(player, CALM_TALK, 1861, 162, "Well, that is true, but it is no replacement for real",
					"training. You won't get very much experience while",
					"training here. This place is just intended as a practice",
					"arena. When you feel ready, you should face real");
			return true;
		case 162:
			sendDialogue(player, CALM_TALK, 1861, -1, "enemies to get better experience.");
			return true;
		case 163:
			boolean hasLevel = player.getSkills().getLevelForExperience(Skills.DEFENCE) >= 99;
			sendDialogue(player, CALM_TALK, 705, hasLevel ? 164 : -1, hasLevel ? "Why of course, you have 99 defence!" : "I'm sorry but only masters can obtain them.");
			return true;
		case 164: {
			int amt = 0;
			for (int i = 0; i < 24; i++) {
				if (player.getSkills().getLevelForExperience(i) >= 99) {
					amt++;
				}
			}
			if (player.getInventory().contains(995, 99000)) {
				player.getInventory().deleteItem(995, 99000);
				int itemId = amt >= 2 ? 9754 : 9753;
				player.getInventory().addDropable(new Item(itemId));
				player.getInventory().addDropable(new Item(9800));
				sendDialogue(player, HAPPY_TALKING, 705, -1, "Here you go! Enjoy it");
			} else {
				sendDialogue(player, SAD, 705, -1, "Sorry but these capes are 99,000 coins a piece.");
			}
		}
		return true;
		case 1000://Magic number for cutscenes, DONT USE IT
			((Cutscene) player.getAttribute("currentScene")).advanceAction();
			return true;
		case 1001://Magic number for cutscenes, DONT USE IT
			((Cutscene) player.getAttribute("currentScene")).advanceAction();
			return false;
		case 1002://Magic number for cutscenes, DONT USE IT
			((Cutscene) player.getAttribute("currentScene")).advanceToAction(2);
			return true;
		case 1003:
			((Cutscene) player.getAttribute("currentScene")).advanceToAction(1);
			return true;
		case 170:
			sendOptionDialogue(player, new int[]{171, -1}, "Yes please!", "No thanks");
			break;
		case 171:
			World.getWorld().getShopManager().openShop(player, 550);
			return false;
		case 172:
			sendOptionDialogue(player, new int[]{173, -1}, "Yes please!", "No thanks");
			break;
		case 173:
			World.getWorld().getShopManager().openShop(player, 553);
			return false;
		case 200:
			sendDialogue(player, CONFUSED, -1, 201, "What can you do for me?");
			return true;
		case 201:
			sendDialogue(player, CONFUSED, 8449, 202, "I could teleport you to the wests or easts,", "where my brothers will rip you limb from lim-", "Nevermind, would you like to go?");
			return true;
		case 202:
			sendOptionDialogue(player, new int[]{203, 204, -1}, "Wests please!", "Easts please!", "No thank you, I'd like to keep my limbs!");
			return true;
		case 203:
			player.getSettings().getSpeakingTo().getNPC().forceText("Weshah!");
			TeleportHandler.telePlayer(player, 2965, 3611, 0, 4, 1, false, false);
			return false;
		case 204:
			player.getSettings().getSpeakingTo().getNPC().forceText("Eashah!");
			TeleportHandler.telePlayer(player, 3332, 3681, 0, 4, 1, false, false);
			return false;
		case 205:
			sendOptionDialogue(player, new int[]{206, -1}, "Sure.", "No thanks!");
			return true;
		case 206:
			if (!player.getInventory().hasRoomFor(15098, 1)) {
				sendDialogue(player, SAD, 970, -1, "You need some space in your inventory first.");
			} else {
				player.getInventory().addItem(new Item(15098, 1));
				sendDialogue(player, HAPPY_TALKING, 970, -1, "Here you go.");
			}
			return true;
		case 210:
			sendOptionDialogue(player, new int[]{211, -1}, "Yes please!", "No thanks!");
			return true;
		case 211: {
			int amt = 0;
			for (int i = 0; i < 24; i++) {
				if (player.getSkills().getLevelForExperience(i) >= 99) {
					amt++;
				}
			}
			if (player.getInventory().contains(995, 99000)) {
				player.getInventory().deleteItem(995, 99000);
				int itemId = amt >= 2 ? 9799 : 9798;
				player.getInventory().addDropable(new Item(itemId));
				player.getInventory().addDropable(new Item(9800));
				sendDialogue(player, HAPPY_TALKING, 308, -1, "Here ya go!");
			} else {
				sendDialogue(player, SAD, 308, -1, "Sorry but these capes are 99,000 coins a piece.");
			}
		}
		return true;
		case 212:
			sendOptionDialogue(player, new int[]{213, -1}, "Yes please!", "No thanks!");
			return true;
		case 213: {
			int amt = 0;
			for (int i = 0; i < 24; i++) {
				if (player.getSkills().getLevelForExperience(i) >= 99) {
					amt++;
				}
			}
			if (player.getInventory().contains(995, 99000)) {
				player.getInventory().deleteItem(995, 99000);
				int itemId = amt >= 2 ? 9808 : 9807;
				player.getInventory().addDropable(new Item(itemId));
				player.getInventory().addDropable(new Item(9809));
				sendDialogue(player, HAPPY_TALKING, 4906, -1, "Here ya go!");
			} else {
				sendDialogue(player, SAD, 4906, -1, "Sorry but these capes are 99,000 coins a piece.");
			}
		}
		return true;
		case 214:
			sendOptionDialogue(player, new int[]{215, -1}, "Yes please!", "No thanks!");
			return true;
		case 215: {
			int amt = 0;
			for (int i = 0; i < 24; i++) {
				if (player.getSkills().getLevelForExperience(i) >= 99) {
					amt++;
				}
			}
			if (player.getInventory().contains(995, 99000)) {
				player.getInventory().deleteItem(995, 99000);
				int itemId = amt >= 2 ? 9784 : 9783;
				player.getInventory().addDropable(new Item(itemId));
				player.getInventory().addDropable(new Item(9785));
				sendDialogue(player, HAPPY_TALKING, 575, -1, "Here you go!");
			} else {
				sendDialogue(player, SAD, 575, -1, "Sorry but these capes are 99,000 coins a piece.");
			}
		}
		return true;
		case 216://
			sendOptionDialogue(player, new int[]{217, -1}, "Yes please!", "No thanks!");

			return true;
		case 217:
			World.getWorld().getShopManager().openShop(player, 575);
			return false;
		case 218:
			sendOptionDialogue(player, new int[]{219, -1}, "Yes please!", "No thanks!");
			return true;
		case 219: {
			int amt = 0;
			for (int i = 0; i < 24; i++) {
				if (player.getSkills().getLevelForExperience(i) >= 99) {
					amt++;
				}
			}
			if (player.getInventory().contains(995, 99000)) {
				player.getInventory().deleteItem(995, 99000);
				int itemId = amt >= 2 ? 9778 : 9777;
				player.getInventory().addDropable(new Item(itemId));
				player.getInventory().addDropable(new Item(9779));
				sendDialogue(player, SECRELTY_TALKING, 2270, -1, "Here you go, now get out of here.");
			} else {
				sendDialogue(player, MEAN_FACE, 2270, -1, "These capes are 99,000 coins a piece, now get out of here.");
			}
			return true;
		}
		case 220:
			sendOptionDialogue(player, new int[]{221, -1}, "Uhh sure!", "No thanks!");
			return true;
		case 221:
			World.getWorld().getShopManager().openShop(player, 2270);
			return true;
		case 222:
			sendOptionDialogue(player, new int[]{223, 225, -1}, "Yes please!", "No thanks, but what are you doing here?", "No thanks!");
			return true;
		case 223: {
			int amt = 0;
			for (int i = 0; i < 24; i++) {
				if (player.getSkills().getLevelForExperience(i) >= 99) {
					amt++;
				}
			}
			if (player.getInventory().contains(995, 99000)) {
				player.getInventory().deleteItem(995, 99000);
				int itemId = amt >= 2 ? 9775 : 9774;
				player.getInventory().addDropable(new Item(itemId));
				player.getInventory().addDropable(new Item(9776));
				sendDialogue(player, HAPPY_TALKING, 445, -1, "Here you go!");
			} else {
				sendDialogue(player, SAD, 455, -1, "Sorry but these capes are 99,000 coins a piece.");
			}
			return true;
		}
		case 224:
			sendOptionDialogue(player, new int[]{225, -1}, "What are you doing here?", "Nothing");
			return true;
		case 225:
			sendDialogue(player, CONFUSED, -1, 226, "What are you doing here?");
			return true;
		case 226:
			sendDialogue(player, TALKING_ALOT, 455, 227, "Well... I'm the only Master of Herblore in the world,", "and after doing it for all those years I got tired of it.", "So I decided to move down over here to start", "my own candle business.");
			return true;
		case 227:
			sendDialogue(player, DEPRESSED, 455, 228, "The only problem is Jatix.");
			return true;
		case 228:
			sendDialogue(player, CONFUSED, -1, 229, "Jatix, you mean the guy back in Ooglog?");
			return true;
		case 229:
			sendDialogue(player, DEPRESSED, 455, 230, "That's exactly who I mean, he's been terrorising me ever", "since I found the only un documented herb in the world.", "With that herb I mastered the skill. And now", "Jatix is trying to steal it from me!");
			return true;
		case 230:
			sendDialogue(player, TALKING_ALOT, 455, -1, "Well oh my, look at the time!", "I must get back to candle making. Goodbye!");
			return true;
		case 231:
			sendOptionDialogue(player, new int[]{232, -1}, "Yes please!", "No thanks!");
			return true;
		case 232: {
			int amt = 0;
			for (int i = 0; i < 24; i++) {
				if (player.getSkills().getLevelForExperience(i) >= 99) {
					amt++;
				}
			}
			if (player.getInventory().contains(995, 99000)) {
				player.getInventory().deleteItem(995, 99000);
				int itemId = amt >= 2 ? 9802 : 9801;
				player.getInventory().addDropable(new Item(itemId));
				player.getInventory().addDropable(new Item(9803));
				sendDialogue(player, SECRELTY_TALKING, 847, -1, "Here you go, have fun!");
			} else {
				sendDialogue(player, SAD, 847, -1, "Sorry but these capes are 99,000 coins a piece.");
			}
			return true;
		}
		case 233: //up
			player.teleport(player.getLocation().transform(0, 0, 1), false);
			return false;
		case 234: //down
			player.teleport(player.getLocation().transform(0, 0, -1), false);
			return false;
		case 235:
			sendOptionDialogue(player, new int[]{236, 237, -1}, "Look at the shop.", "Take a charter", "Nothing");
			return true;
		case 236:
			//World.getWorld().getShopManager().openShop(player, 4651);
			player.sendMessage("At this time the shop is not available.");
			return false;
		case 237:
			sendOptionDialogue(player, new int[]{174, 238, 240, -1}, "Catherby", "Karamja", "Ooglog", "Nowhere");
			return true;
		case 238://karmja
			player.setAttribute("teleportDestination", Position.create(2956, 3146, 0));
			return true;
		case 239: //brimmy
			player.setAttribute("teleportDestination", Position.create(2772, 3227, 0));
			return true;
		case 240://home
			player.setAttribute("teleportDestination", Position.create(2623, 2857, 0));
			return true;
		case 241:
			sendOptionDialogue(player, new int[]{242, 243, -1}, "Look at the shop.", "Take a charter", "Nothing");
			return true;
		case 242:
			//World.getWorld().getShopManager().openShop(player, 4652);
			return false;
		case 243:
			sendOptionDialogue(player, new int[]{174, 238, 239, -1}, "Catherby", "Karamja", "Brimhaven", "Nowhere");
			return true;
		case 244:
			sendOptionDialogue(player, new int[]{245, 246, -1}, "Look at the shop.", "Take a charter", "Nothing");
			return true;
		case 245:
			//World.getWorld().getShopManager().openShop(player, 4653);
			return false;
		case 246:
			sendOptionDialogue(player, new int[]{240, 238, 239, -1}, "Ooglog", "Karamja", "Brimhaven", "Nowhere");
			return true;
		case 247:
			sendOptionDialogue(player, new int[]{248, 249, -1}, "Look at the shop.", "Take a charter", "Nothing");
			return true;
		case 248:
			//World.getWorld().getShopManager().openShop(player, 4654);
			return false;
		case 249:
			sendOptionDialogue(player, new int[]{240, 174, 239, -1}, "Ooglog", "Catherby", "Brimhaven", "Nowhere");
			return true;
		case 250:
			final Position location = player.getAttribute("teleportDestination");
			if (location != null) {
				player.setAttribute("cantWalk", Boolean.TRUE);
				ActionSender.sendInterface(player, 115);
				World.getWorld().submit(new Tick(3) {
					int count = 0;

					public void execute() {
						if (count == 0) {
							player.teleport(location.getX(), location.getY(), location.getZ(), false);
							count++;
						} else {
							stop();
							player.removeAttribute("cantWalk");
							ActionSender.sendCloseInterface(player);
						}
					}
				});
			}
			return false;
		case 251:
			sendOptionDialogue(player, new int[]{252, -1}, "Yes please!", "No thank you");
			return true;
		case 252:
			sendDialogue(player, TALKING_ALOT, 587, 253, "Off you go!");
			return true;
		case 253:
			player.graphics(2676);
			player.animate(9599);
			World.getWorld().submit(new Tick(2) {

				@Override
				public void execute() {
					stop();
					player.teleport(2892, 3454, 0, false);

				}

			});
			return false;
		case 254:
			sendOptionDialogue(player, new int[]{255, 712}, "Sure!", "No, thank you.");
			return true;
		case 255:
			World.getWorld().getShopManager().openShop(player, 586);
			return false;
		case 319:
			sendOptionDialogue(player, new int[]{320, 321, 322}, "Can I have a different gravestone?", "Can you restore my prayer points?", "No, thank you");
			return true;
		case 320:
			sendDialogue(player, CALM_TALK, -1, 323, "Can I have a different gravestone?");
			return true;
		case 321:
			sendDialogue(player, CALM_TALK, -1, 324, "Can you restore my prayer points?");
			return true;
		case 322:
			sendDialogue(player, CALM_TALK, -1, -1, "No, thank you.");
			return true;
		case 323:
			sendDialogue(player, CALM_TALK, 456, 325, "Of course you can,", "have a look at this selection of gravestones.");
			return true;
		case 324:
			sendDialogue(player, CALM_TALK, 456, -1, "I think the Gods prefer it if you pray<br>to them at an altar dedicated to Their name.");
			return true;
		case 325:
			ActionSender.sendInterface(player, 652);
			ActionSender.sendAMask(player, 150, 652, 34, 0, 13);
			ActionSender.sendConfig(player, 1146, player.getSettings().getGraveStone() | 262112);
			return false;
		case 400:
			sendOptionDialogue(player, new int[]{401, 402}, "Here you go.", "That's too much for me!");
					return true;
		case 401:
			sendDialogue(player, CALM_TALK, -1, 403, "Here you go.");
			return true;
		case 402:
			sendDialogue(player, WHAT_THE_CRAP, -1, -1, "That's too much for me!");
			return true;
		case 403:
			int graveId = player.getAttribute("graveSelection", -1);
			int price = player.getAttribute("gravePrice", -1);
			if (!player.getInventory().contains(995, price)) {
				sendDialogue(player, CALM_TALK, 456, -1, "You don't seem to have enough coins.");
				return true;
			}
			player.getInventory().deleteItem(995, price);
			player.removeAttribute("graveSelection");
			player.removeAttribute("gravePrice");
			player.getSettings().setGraveStone(graveId);
			sendDialogue(player, CALM_TALK, 456, -1, "Thank you, dear adventurer.", "Let's hope you're not going to need it soon.");
			return true;
		case 404:
			sendOptionDialogue(player, new int[]{405, -1}, "Can you repair my barrows equipment?", "Nothing.");
			return true;
		case 405:
			sendDialogue(player, CONFUSED, -1, 406, "Can you repair my barrows equipment?");
			return true;
		case 406:
			sendDialogue(player, HAPPY_TALKING, 519, 407, "Of course! Let me see what you have.");
			RepairItem.checkPlayerForRepair(player);
			return true;
		case 407:
			if (RepairItem.getRepairItems().isEmpty()) {
				sendDialogue(player, SAD, 519, -1, "You don't have any items for repair.");
				RepairItem.resetRepair();
				return true;
			} else {
				sendDialogue(player, CONFUSED, 519, 408, "Would you like me to repair that for " + NumberFormat.getInstance().format(RepairItem.getRepairCost(player)) + " coins?");
				return true;
			}
		case 408:
			sendOptionDialogue(player, new int[]{409, 410}, "Yes, please.", "No, not at this time.");
			return true;
		case 409:
			if (player.getInventory().getContainer().getNumberOf(new Item(995)) < RepairItem.getCost()) {
				sendDialogue(player, SAD, 519, -1, "You don't have enough coins to repair these items.");
				RepairItem.resetRepair();
				return true;
			} else {
				RepairItem.repairItems(player);
				sendDialogue(player, HAPPY_TALKING, 519, -1, "I have repaired all of your equipment. Enjoy!");
				return true;
			}
		case 410:
			RepairItem.resetRepair();
			return false;
			//Don't use 411, spirit tree uses that.
		case 412:
			sendOptionDialogue(player, new int[]{413, 414, 415, -1}, "May I see your supply shop?", "Can you take me to a hunting ground?", "Can I buy a hunter skill cape?", "Nothing.");
			return true;
		case 413:
			World.getWorld().getShopManager().openShop(player, 5113);
			return false;
		case 414:
			sendOptionDialogue(player, new int[]{416, 417, 418, 419, 412}, "Crimson Swift", "Tropical Wagtail", "Red Chinchompa", "Red Salamander", "Go Back");
			return true;
		case 415:
			if (player.getSkills().getLevel(Skills.HUNTER) >= 99) {
				int amt = 0;
				for (int i = 0; i < 24; i++) {
					if (player.getSkills().getLevelForExperience(i) >= 99) {
						amt++;
					}
				}
				if (player.getInventory().contains(995, 99000)) {
					player.getInventory().deleteItem(995, 99000);
					int itemId = amt >= 2 ? 9949 : 9948;
					player.getInventory().addDropable(new Item(itemId));
					player.getInventory().addDropable(new Item(9950));
					sendDialogue(player, HAPPY_TALKING, 5113, -1, "Here you go! Enjoy it");
				} else {
					sendDialogue(player, SAD, 5113, -1, "Sorry but these capes are 99,000 coins a piece.");
				}
			} else {
				sendDialogue(player, TOUGH, 5113, -1, "Come back when you have achieved a hunter level of 99.");
			}
			return true;
		case 416:
			player.setAttribute("teleportDestination", Position.create(2591, 2889, 0));
			return true;
		case 417:
			if (player.getSkills().getLevel(Skills.HUNTER) >= 19) {
				player.setAttribute("teleportDestination", Position.create(2548, 2882, 0));
			} else {
				sendDialogue(player, TOUGH, 5113, -1, "You need a hunter level of 19 to teleport there.");
			}
			return true;
		case 418:
			if (player.getSkills().getLevel(Skills.HUNTER) >= 63) {
				player.setAttribute("teleportDestination", Position.create(2558, 2939, 0));
			} else {
				sendDialogue(player, TOUGH, 5113, -1, "You need a hunter level of 63 to teleport there.");
			}
			return true;
		case 419:
			if (player.getSkills().getLevel(Skills.HUNTER) >= 59) {
				player.setAttribute("teleportDestination", Position.create(2456, 3222, 0));
			} else {
				sendDialogue(player, TOUGH, 5113, -1, "You need a hunter level of 59 to teleport there.");
			}
			return true;
			//GO 560+
		case 561:
			sendOptionDialogue(player, new int[]{562, -1}, "Yes please, I'm starving!", "Nah, I'm not hungry.");
			return true;
		case 562:
			//World.getWorld().getShopManager().openShop(player, 3322);
			return false;
		case 563:
			TeleportHandler.telePlayer(player, 3087, 3496, 0, 0, 0, false, 1712, false);
			if(player.getInventory().getContainer().contains(new Item(1712, 1))){
				player.getInventory().deleteItem(1712, 1);
				player.getInventory().addItem(new Item(1710, 1));
				player.getInventory().refresh();
			}

			player.sendMessage("Your amulet has three charges left.");
			return false;
		case 564:
			TeleportHandler.telePlayer(player, 2918, 3176, 0, 0, 0, false, 1712, false);
			if(player.getInventory().getContainer().contains(new Item(1712, 1))){
				player.getInventory().deleteItem(1712, 1);
				player.getInventory().addItem(new Item(1710, 1));
				player.getInventory().refresh();
			}
			player.sendMessage("Your amulet has three charges left.");
			return false;
		case 565:
			TeleportHandler.telePlayer(player, 3105, 3251, 0, 0, 0, false, 1712, false);
			if(player.getInventory().getContainer().contains(new Item(1712, 1))){
				player.getInventory().deleteItem(1712, 1);
				player.getInventory().addItem(new Item(1710, 1));
				player.getInventory().refresh();
			}
			player.sendMessage("Your amulet has three charges left.");
			return false;
		case 566:
			TeleportHandler.telePlayer(player, 3293, 3163, 0, 0, 0, false, 1712, false);
			if(player.getInventory().getContainer().contains(new Item(1712, 1))){
				player.getInventory().deleteItem(1712, 1);
				player.getInventory().addItem(new Item(1710, 1));
				player.getInventory().refresh();
			}
			player.sendMessage("Your amulet has three charges left.");
			return false;
		case 567:
			TeleportHandler.telePlayer(player, 3087, 3496, 0, 0, 0, false, 1710, false);
			if(player.getInventory().getContainer().contains(new Item(1710, 1))){
				player.getInventory().deleteItem(1710, 1);
				player.getInventory().addItem(new Item(1708, 1));
				player.getInventory().refresh();
			}
			player.sendMessage("Your amulet has two charges left.");
			return false;
		case 568:
			TeleportHandler.telePlayer(player, 2918, 3176, 0, 0, 0, false, 1710, false);
			if(player.getInventory().getContainer().contains(new Item(1710, 1))){
				player.getInventory().deleteItem(1710, 1);
				player.getInventory().addItem(new Item(1708, 1));
				player.getInventory().refresh();
			}
			player.sendMessage("Your amulet has two charges left.");
			return false;
		case 569:
			TeleportHandler.telePlayer(player, 3105, 3251, 0, 0, 0, false, 1710, false);
			if(player.getInventory().getContainer().contains(new Item(1710, 1))){
				player.getInventory().deleteItem(1710, 1);
				player.getInventory().addItem(new Item(1708, 1));
				player.getInventory().refresh();
			}
			player.sendMessage("Your amulet has two charges left.");
			return false;
		case 570:
			TeleportHandler.telePlayer(player, 3293, 3163, 0, 0, 0, false, 1710, false);
			if(player.getInventory().getContainer().contains(new Item(1710, 1))){
				player.getInventory().deleteItem(1710, 1);
				player.getInventory().addItem(new Item(1708, 1));
				player.getInventory().refresh();
			}
			player.sendMessage("Your amulet has two charges left.");
			return false;
		case 571:
			TeleportHandler.telePlayer(player, 3087, 3496, 0, 0, 0, false, 1708, false);
			if(player.getInventory().getContainer().contains(new Item(1708, 1))){
				player.getInventory().deleteItem(1708, 1);
				player.getInventory().addItem(new Item(1706, 1));
				player.getInventory().refresh();
			}
			player.sendMessage("Your amulet has one charge left.");
			return false;
		case 572:
			TeleportHandler.telePlayer(player, 2918, 3176, 0, 0, 0, false, 1708, false);
			if(player.getInventory().getContainer().contains(new Item(1708, 1))){
				player.getInventory().deleteItem(1708, 1);
				player.getInventory().addItem(new Item(1706, 1));
				player.getInventory().refresh();
			}
			player.sendMessage("Your amulet has one charge left.");
			return false;
		case 573:
			TeleportHandler.telePlayer(player, 3105, 3251, 0, 0, 0, false, 1708, false);
			if(player.getInventory().getContainer().contains(new Item(1708, 1))){
				player.getInventory().deleteItem(1708, 1);
				player.getInventory().addItem(new Item(1706, 1));
				player.getInventory().refresh();
			}
			player.sendMessage("Your amulet has one charge left.");
			return false;
		case 574:
			TeleportHandler.telePlayer(player, 3293, 3163, 0, 0, 0, false, 1708, false);
			if(player.getInventory().getContainer().contains(new Item(1708, 1))){
				player.getInventory().deleteItem(1708, 1);
				player.getInventory().addItem(new Item(1706, 1));
				player.getInventory().refresh();
			}
			player.sendMessage("Your amulet has one charge left.");
			return false;
		case 575:
			TeleportHandler.telePlayer(player, 3087, 3496, 0, 0, 0, false, 1706, false);
			if(player.getInventory().getContainer().contains(new Item(1706, 1))){
				player.getInventory().deleteItem(1706, 1);
				player.getInventory().addItem(new Item(1704, 1));
				player.getInventory().refresh();
			}
			player.sendMessage("You use the amulet's last charge.");
			return false;
		case 576:
			TeleportHandler.telePlayer(player, 2918, 3176, 0, 0, 0, false, 1706, false);
			if(player.getInventory().getContainer().contains(new Item(1706, 1))){
				player.getInventory().deleteItem(1706, 1);
				player.getInventory().addItem(new Item(1704, 1));
				player.getInventory().refresh();
			}
			player.sendMessage("You use the amulet's last charge.");
			return false;
		case 577:
			TeleportHandler.telePlayer(player, 3105, 3251, 0, 0, 0, false, 1706, false);
			if(player.getInventory().getContainer().contains(new Item(1706, 1))){
				player.getInventory().deleteItem(1706, 1);
				player.getInventory().addItem(new Item(1704, 1));
				player.getInventory().refresh();
			}
			player.sendMessage("You use the amulet's last charge.");
			return false;
		case 578:
			TeleportHandler.telePlayer(player, 3293, 3163, 0, 0, 0, false, 1706, false);
			if(player.getInventory().getContainer().contains(new Item(1706, 1))){
				player.getInventory().deleteItem(1706, 1);
				player.getInventory().addItem(new Item(1704, 1));
				player.getInventory().refresh();
			}
			player.sendMessage("You use the amulet's last charge.");
			return false;
			//579 - 583 skill masters
		case 584:
			TeleportHandler.telePlayer(player, 2876, 3557, 0, 0, 0, false, 3853, false);
			if(player.getInventory().getContainer().contains(new Item(3853, 1))){
				player.getInventory().deleteItem(3853, 1);
				player.getInventory().addItem(new Item(3855, 1));
				player.getInventory().refresh();
			}
			player.sendMessage("Your necklace has seven charges left.");
			return false;
		case 585:
			TeleportHandler.telePlayer(player, 2518, 3570, 0, 0, 0, false, 3853, false);
			if(player.getInventory().getContainer().contains(new Item(3853, 1))){
				player.getInventory().deleteItem(3853, 1);
				player.getInventory().addItem(new Item(3855, 1));
				player.getInventory().refresh();
			}
			player.sendMessage("Your necklace has seven charges left.");
			return false;
		case 586:
			TeleportHandler.telePlayer(player, 2876, 3557, 0, 0, 0, false, 3855, false);
			if(player.getInventory().getContainer().contains(new Item(3855, 1))){
				player.getInventory().deleteItem(3855, 1);
				player.getInventory().addItem(new Item(3857, 1));
				player.getInventory().refresh();
			}
			player.sendMessage("Your necklace has six charges left.");
			return false;
		case 587:
			TeleportHandler.telePlayer(player, 2518, 3570, 0, 0, 0, false, 3855, false);
			if(player.getInventory().getContainer().contains(new Item(3855, 1))){
				player.getInventory().deleteItem(3855, 1);
				player.getInventory().addItem(new Item(3857, 1));
				player.getInventory().refresh();
			}
			player.sendMessage("Your necklace has six charges left.");
			return false;
		case 588:
			TeleportHandler.telePlayer(player, 2876, 3557, 0, 0, 0, false, 3857, false);
			if(player.getInventory().getContainer().contains(new Item(3857, 1))){
				player.getInventory().deleteItem(3857, 1);
				player.getInventory().addItem(new Item(3859, 1));
				player.getInventory().refresh();
			}
			player.sendMessage("Your necklace has five charges left.");
			return false;
		case 589:
			TeleportHandler.telePlayer(player, 2518, 3570, 0, 0, 0, false, 3857, false);
			if(player.getInventory().getContainer().contains(new Item(3857, 1))){
				player.getInventory().deleteItem(3857, 1);
				player.getInventory().addItem(new Item(3859, 1));
				player.getInventory().refresh();
			}
			player.sendMessage("Your necklace has five charges left.");
			return false;
		case 590:
			TeleportHandler.telePlayer(player, 2876, 3557, 0, 0, 0, false, 3859, false);
			if(player.getInventory().getContainer().contains(new Item(3857, 1))){
				player.getInventory().deleteItem(3859, 1);
				player.getInventory().addItem(new Item(3861, 1));
				player.getInventory().refresh();
			}
			player.sendMessage("Your necklace has four charges left.");
			return false;
		case 591:
			TeleportHandler.telePlayer(player, 2518, 3570, 0, 0, 0, false, 3859, false);
			if(player.getInventory().getContainer().contains(new Item(3859, 1))){
				player.getInventory().deleteItem(3859, 1);
				player.getInventory().addItem(new Item(3861, 1));
				player.getInventory().refresh();
			}
			player.sendMessage("Your necklace has four charges left.");
			return false;
		case 592:
			TeleportHandler.telePlayer(player, 2876, 3557, 0, 0, 0, false, 3861, false);
			if(player.getInventory().getContainer().contains(new Item(3861, 1))){
				player.getInventory().deleteItem(3861, 1);
				player.getInventory().addItem(new Item(3863, 1));
				player.getInventory().refresh();
			}
			player.sendMessage("Your necklace has three charges left.");
			return false;
		case 593:
			TeleportHandler.telePlayer(player, 2518, 3570, 0, 0, 0, false, 3861, false);
			if(player.getInventory().getContainer().contains(new Item(3861, 1))){
				player.getInventory().deleteItem(3861, 1);
				player.getInventory().addItem(new Item(3863, 1));
				player.getInventory().refresh();
			}
			player.sendMessage("Your necklace has three charges left.");
			return false;
		case 594:
			TeleportHandler.telePlayer(player, 2876, 3557, 0, 0, 0, false, 3863, false);
			if(player.getInventory().getContainer().contains(new Item(3863, 1))){
				player.getInventory().deleteItem(3863, 1);
				player.getInventory().addItem(new Item(3865, 1));
				player.getInventory().refresh();
			}
			player.sendMessage("Your necklace has two charges left.");
			return false;
		case 595:
			TeleportHandler.telePlayer(player, 2518, 3570, 0, 0, 0, false, 3863, false);
			if(player.getInventory().getContainer().contains(new Item(3863, 1))){
				player.getInventory().deleteItem(3863, 1);
				player.getInventory().addItem(new Item(3865, 1));
				player.getInventory().refresh();
			}
			player.sendMessage("Your necklace has two charges left.");
			return false;
		case 596:
			TeleportHandler.telePlayer(player, 2876, 3557, 0, 0, 0, false, 3865, false);
			if(player.getInventory().getContainer().contains(new Item(3865, 1))){
				player.getInventory().deleteItem(3865, 1);
				player.getInventory().addItem(new Item(3867, 1));
				player.getInventory().refresh();
			}
			player.sendMessage("Your necklace has one charges left.");
			return false;
		case 597:
			TeleportHandler.telePlayer(player, 2518, 3570, 0, 0, 0, false, 3865, false);
			if(player.getInventory().getContainer().contains(new Item(3865, 1))){
				player.getInventory().deleteItem(3865, 1);
				player.getInventory().addItem(new Item(3867, 1));
				player.getInventory().refresh();
			}
			player.sendMessage("Your necklace has one charges left.");
			return false;
		case 598:
			TeleportHandler.telePlayer(player, 2518, 3570, 0, 0, 0, false, 3867, false);
			if(player.getInventory().getContainer().contains(new Item(3867, 1))){
				player.getInventory().deleteItem(3867, 1);
				player.getInventory().refresh();
			}
			player.sendMessage("Your necklace crumbles into dust.");
			return false;
		case 599:
			TeleportHandler.telePlayer(player, 2876, 3557, 0, 0, 0, false, 3867, false);
			if(player.getInventory().getContainer().contains(new Item(3867, 1))){
				player.getInventory().deleteItem(3867, 1);
				player.getInventory().refresh();
			}
			player.sendMessage("Your necklace crumbles into dust.");
			return false;
		case 600:
			TeleportHandler.telePlayer(player, 3313, 3234, 0, 0, 0, false, 2552, false);
			if(player.getInventory().getContainer().contains(new Item(2552, 1))){
				player.getInventory().deleteItem(2552, 1);
				player.getInventory().addItem(new Item(2554, 1));
				player.getInventory().refresh();
			}
			player.sendMessage("Your ring has seven charges left.");
			return false;
		case 601:
			TeleportHandler.telePlayer(player, 2441, 3089, 0, 0, 0, false, 2552, false);
			if(player.getInventory().getContainer().contains(new Item(2552, 1))){
				player.getInventory().deleteItem(2552, 1);
				player.getInventory().addItem(new Item(2524, 1));
				player.getInventory().refresh();
			}
			player.sendMessage("Your ring has seven charges left.");
			return false;
		case 602:
			TeleportHandler.telePlayer(player, 3313, 3234, 0, 0, 0, false, 2554, false);
			if(player.getInventory().getContainer().contains(new Item(2554, 1))){
				player.getInventory().deleteItem(2554, 1);
				player.getInventory().addItem(new Item(2556, 1));
				player.getInventory().refresh();
			}
			player.sendMessage("Your ring has six charges left.");
			return false;
		case 603:
			TeleportHandler.telePlayer(player, 2441, 3089, 0, 0, 0, false, 2552, false);
			if(player.getInventory().getContainer().contains(new Item(2552, 1))){
				player.getInventory().deleteItem(2552, 1);
				player.getInventory().addItem(new Item(2554, 1));
				player.getInventory().refresh();
			}
			player.sendMessage("Your ring has six charges left.");
			return false;
		case 604:
			TeleportHandler.telePlayer(player, 3313, 3234, 0, 0, 0, false, 2554, false);
			if(player.getInventory().getContainer().contains(new Item(2554, 1))){
				player.getInventory().deleteItem(2554, 1);
				player.getInventory().addItem(new Item(2556, 1));
				player.getInventory().refresh();
			}
			player.sendMessage("Your ring has five charges left.");
			return false;
		case 605:
			TeleportHandler.telePlayer(player, 2441, 3089, 0, 0, 0, false, 2554, false);
			if(player.getInventory().getContainer().contains(new Item(2554, 1))){
				player.getInventory().deleteItem(2554, 1);
				player.getInventory().addItem(new Item(2556, 1));
				player.getInventory().refresh();
			}
			player.sendMessage("Your ring has five charges left.");
			return false;
		case 606:
			TeleportHandler.telePlayer(player, 3313, 3234, 0, 0, 0, false, 2556, false);
			if(player.getInventory().getContainer().contains(new Item(2556, 1))){
				player.getInventory().deleteItem(2556, 1);
				player.getInventory().addItem(new Item(2558, 1));
				player.getInventory().refresh();
			}
			player.sendMessage("Your ring has four charges left.");
			return false;
		case 607:
			TeleportHandler.telePlayer(player, 2441, 3089, 0, 0, 0, false, 2556, false);
			if(player.getInventory().getContainer().contains(new Item(2556, 1))){
				player.getInventory().deleteItem(2556, 1);
				player.getInventory().addItem(new Item(2558, 1));
				player.getInventory().refresh();
			}
			player.sendMessage("Your ring has four charges left.");
			return false;
		case 608:
			TeleportHandler.telePlayer(player, 3313, 3234, 0, 0, 0, false, 2558, false);
			if(player.getInventory().getContainer().contains(new Item(2558, 1))){
				player.getInventory().deleteItem(2558, 1);
				player.getInventory().addItem(new Item(2560, 1));
				player.getInventory().refresh();
			}
			player.sendMessage("Your ring has three charges left.");
			return false;
		case 609:
			TeleportHandler.telePlayer(player, 2441, 3089, 0, 0, 0, false, 2558, false);
			if(player.getInventory().getContainer().contains(new Item(2558, 1))){
				player.getInventory().deleteItem(2558, 1);
				player.getInventory().addItem(new Item(2560, 1));
				player.getInventory().refresh();
			}
			player.sendMessage("Your ring has three charges left.");
			return false;
		case 610:
			TeleportHandler.telePlayer(player, 3313, 3234, 0, 0, 0, false, 2560, false);
			if(player.getInventory().getContainer().contains(new Item(2560, 1))){
				player.getInventory().deleteItem(2560, 1);
				player.getInventory().addItem(new Item(2562, 1));
				player.getInventory().refresh();
			}
			player.sendMessage("Your ring has two charges left.");
			return false;
		case 611:
			TeleportHandler.telePlayer(player, 2441, 3089, 0, 0, 0, false, 2560, false);
			if(player.getInventory().getContainer().contains(new Item(2560, 1))){
				player.getInventory().deleteItem(2560, 1);
				player.getInventory().addItem(new Item(2562, 1));
				player.getInventory().refresh();
			}
			player.sendMessage("Your ring has two charges left.");
			return false;
		case 612:
			TeleportHandler.telePlayer(player, 3313, 3234, 0, 0, 0, false, 2562, false);
			if(player.getInventory().getContainer().contains(new Item(2562, 1))){
				player.getInventory().deleteItem(2562, 1);
				player.getInventory().addItem(new Item(2564, 1));
				player.getInventory().refresh();
			}
			player.sendMessage("Your ring has one charge left.");
			return false;
		case 613:
			TeleportHandler.telePlayer(player, 2441, 3089, 0, 0, 0, false, 2562, false);
			if(player.getInventory().getContainer().contains(new Item(2562, 1))){
				player.getInventory().deleteItem(2562, 1);
				player.getInventory().addItem(new Item(2564, 1));
				player.getInventory().refresh();
			}
			player.sendMessage("Your ring has one charge left.");
			return false;
		case 614:
			TeleportHandler.telePlayer(player, 3313, 3234, 0, 0, 0, false, 2564, false);
			if(player.getInventory().getContainer().contains(new Item(2564, 1))){
				player.getInventory().deleteItem(2564, 1);
				player.getInventory().refresh();
			}
			player.sendMessage("Your ring crumbles into dust.");
			return false;
		case 615:
			TeleportHandler.telePlayer(player, 2441, 3089, 0, 0, 0, false, 2564, false);
			if(player.getInventory().getContainer().contains(new Item(2564, 1))){
				player.getInventory().deleteItem(2564, 1);
				player.getInventory().refresh();
			}
			player.sendMessage("Your ring crumbles into dust.");
			return false;
		case 616:
			sendOptionDialogue(player, new int[]{617, 618, 619, 620}, "I'd like to access my bank account, please.", "I'd like to get a bank note please.", "I'd like to exchange my bank note(s).", "What is this place?");
			return true;
		case 617:
			if (player.getAttribute("fromBank") != null) {
				ActionSender.sendInterfaceConfig(player, 667, 49, true);
				ActionSender.sendInterfaceConfig(player, 667, 50, true);
				player.getBonuses().refreshEquipScreen();
				ActionSender.sendInterface(player, 667);
			} else {
				player.getBank().openBank();
				player.removeAttribute("fromBank");
			}
			return false;
			//BANK NOTES:
		case 618:
			sendDialogue(player, HAPPY_TALKING, 494, 913, "Why yes of course!", "How much are you looking to store?");
			//TODO: Pin Settings
			return true;
		case 913:
			sendOptionDialogue(player, new int[]{914, 915, 916, -1}, "500(M)", "1,000(M)", "2,000(M)", "Nevermind...");
			return true;
		case 914:
			if (player.getInventory().contains(995, 500000000)) {
				player.getInventory().addDropable(new Item(11341, 1));
				player.getInventory().deleteItem(995, 500000000);
				player.getInventory().refresh();
				sendDialogue(player, HAPPY_TALKING, 494, -1, "Thank you! Your money is safe with us!");
			} else if (!player.getInventory().contains(995, 500000000)) { 
				sendDialogue(player, SAD, 494, -1, "You don't have enough money for this.");
			}
			return true;
		case 915:
			if (player.getInventory().contains(995, 1000000000)) {
				player.getInventory().addDropable(new Item(11342, 1));
				player.getInventory().deleteItem(995, 1000000000);
				player.getInventory().refresh();
				sendDialogue(player, HAPPY_TALKING, 494, -1, "Thank you! Your money is safe with us!");
			} else if (!player.getInventory().contains(995, 1000000000)) {
				sendDialogue(player, SAD, 494, -1, "You don't have enough money for this.");
			}
			return true;
		case 916:
			if (player.getInventory().contains(995, 2000000000)) {
				player.getInventory().addDropable(new Item(11343, 1));
				player.getInventory().deleteItem(995, 2000000000);
				player.getInventory().refresh();
				sendDialogue(player, HAPPY_TALKING, 494, -1, "Thank you! Your money is safe with us!");
			} else if (!player.getInventory().contains(995, 2000000000)) {
				sendDialogue(player, SAD, 494, -1, "You don't have enough money for this.");
			}
			return true;
		case 619:
			sendOptionDialogue(player, new int[]{917, 918, 919, -1}, "500(M) Note", "1,000(M) Note", "2,000(M) Note", "Nevermind...");
			return true;
		case 917:
			if (player.getInventory().contains(11341, 1)) {
				player.getInventory().addDropable(new Item(995, 500000000));
				player.getInventory().deleteItem(11341, 1);
				player.getInventory().refresh();
				sendDialogue(player, HAPPY_TALKING, 494, -1, "Thank you! Here is your 500M GP!");
			} else if (!player.getInventory().contains(11341, 1)) { 
				sendDialogue(player, SAD, 494, -1, "You don't have a bank note.");
			}
			return true;
		case 918:
			if (player.getInventory().contains(11342, 1)) {
				player.getInventory().addDropable(new Item(995, 1000000000));
				player.getInventory().deleteItem(11342, 1);
				player.getInventory().refresh();
				sendDialogue(player, HAPPY_TALKING, 494, -1, "Thank you! Here is your 1,000M GP!");
			} else if (!player.getInventory().contains(11342, 1))  {
				sendDialogue(player, SAD, 494, -1, "You don't have a bank note.");
			}
			return true;
		case 919:
			if (player.getInventory().contains(11343, 1)) {
				player.getInventory().addDropable(new Item(995, 2000000000));
				player.getInventory().deleteItem(11343, 1);
				player.getInventory().refresh();
				sendDialogue(player, HAPPY_TALKING, 494, -1, "Thank you! Here is your 2,000M GP!");
			} else if (!player.getInventory().contains(11343, 1))  {
				sendDialogue(player, SAD, 494, -1, "You don't have a bank note.");
			}
			return true;
			//END OF BANK NOTES
		case 9126:
			sendOptionDialogue(player, new int[]{9127, 9128, 9129, -1}, "Dung shop", "Daemonheim", "Frost Dragons", "Nothing");
			return true;
		case 9128:
			TeleportHandler.telePlayer(player, 3448, 3708, 0, 0, 0, false, false);
			return false;
		case 5032:
			DialogueManager.sendDialogue(player, DialogueManager.CONFUSED, -1, 5033, "Can you take me to Lunar Isle?");
			return true;
		case 5033:
			sendDialogue(player, HAPPY_TALKING, 5512, 5034, "Of course I can!", "Are you sure you wish to go to Lunar Isle?");
			return true;
		case 5034:
			sendOptionDialogue(player, new int[]{5035, -1}, "Yes please I would love to!", "Sorry I can't go right now..");
			return true;
		case 5035:
			TeleportHandler.telePlayer(player, 2310, 3781, 0, 0, 0, false, false);
			return false;
		case 9129:
			if (player.getSkills().getLevel(Skills.DUNGEONEERING) > 84) {
				TeleportHandler.telePlayer(player, 1314, 4513, 0, 0, 0, false, false);
			} else {
				player.sendMessage("You need a dungeoneering level of 85 to go here.");
			}
			return false;
		case 4254:
			player.getBank().openBank();
            return false;
		case 13000:
			sendOptionDialogue(player, new int[]{13001, 13002, 13003, -1}, "Edgeville", "Multi", "Mage Bank", "None");
			return true;
		case 1994:
			sendOptionDialogue(player, new int[]{1995, -1}, "Yes I'm ready!", "No way!");
			return true;
		case 1995:
			TeleportHandler.telePlayer(player, 2387, 5069, 0, 0, 0, false, false);
			return false;
		case 13001:
			TeleportHandler.telePlayer(player, 3090, 3520, 0, 0, 0, false, false);
			return false;
		case 13002:
			TeleportHandler.telePlayer(player, 3006, 5511, 0, 0, 0, false, false);
			return false;
		case 13003:
			TeleportHandler.telePlayer(player, 2539, 4714, 0, 0, 0, false, false);
			return false;
		case 5790:
			sendOptionDialogue(player, new int[]{5791, 5792, 5793, 5794, 5795}, "Fishing", "Mining", "Hunter", "Agility", "More");
			return true;
		case 5791:
			player.teleport(2848, 3433, 0, false);
			return false;
		case 5792:
			player.teleport(3039, 9768, 0, false);
			return false;
		case 5793:
			sendOptionDialogue(player, new int[]{5919, 5920}, "Implings", "Feldip Hills");
			return true;
		case 5919:
			player.teleport(2589, 4321, 0, false);
			return false;
		case 5920:
			player.teleport(2564, 2939, 0, false);
			return false;
		case 5794:
			sendOptionDialogue(player, new int[]{5922, 5923}, "Gnome Course", "Barbarian Course");
			return true;
		case 5922:
			player.teleport(2470, 3435, 0, false);
			return false;
		case 5923:
			player.teleport(2552, 3561, 0, false);
			return false;
		case 5795:
			sendOptionDialogue(player, new int[]{5790, 5796, 5797, -1}, "Previous", "Woodcutting", "Construction", "None");
			return true;
		case 5796:
			player.teleport(2724, 3468, 0, false);
			return false;
		case 5797:
			player.teleport(3318, 3493, 0, false);
			return false;
		case 5560:
			sendOptionDialogue(player, new int[]{5561, 5562, 5563, 5564, 5565}, "King Black Dragon", "Nex", "Kalphite Queen", "Tormented Demons", "More");
			return true;
		case 5561:
			TeleportHandler.telePlayer(player, 2274, 4685, 0, 0, 0, false, false);
			return false;
		case 5562:
			TeleportHandler.telePlayer(player, 2903, 5204, 0, 0, 0, false, false);
			return false;
		case 5563:
			TeleportHandler.telePlayer(player, 3478, 9489, 0, 0, 0, false, false);
			return false;
		case 5564:
			TeleportHandler.telePlayer(player, 2601, 5745, 0, 0, 0, false, false);
			return false;
		case 9037:
			TeleportHandler.telePlayer(player, 2815, 5511, 0, 4, 1, false, false);
			return false;
		case 9038:
			TeleportHandler.telePlayer(player, 3806, 2975, 0, 4, 1, false, false);
			return false;
		case 5565:
			sendOptionDialogue(player, new int[]{5560, 5566, 5567, 5568, -1}, "Previous", "Chaos Elemental (Wilderness)", "Black Dragons", "God Wars Dungeon", "None");
			return true;
		case 5566:
			TeleportHandler.telePlayer(player, 3259, 3913, 0, 0, 0, false, false);
			return false;
		case 5567:
			TeleportHandler.telePlayer(player, 2836, 9820, 0, 0, 0, false, false);
			return false;
		case 5568:
			TeleportHandler.telePlayer(player, 2881, 5308, 2, 0, 0, false, false);
			return false;
		case 5077:
			sendOptionDialogue(player, new int[]{5078, -1}, "Yes indeed!", "Nevermind....");
			return true;
		case 5078:
			sendDialogue(player, HAPPY_TALKING, -1, 5079, "Yes indeed!");
			return true;
		case 5079: 
			sendDialogue(player, HAPPY_TALKING, 9712, -1, "Ahh.... Well that's alright then.", "You can either select a dungeon cave enterance","or can talk to Thok to start your mini game!","Have fun out there! *ITEMS SAFE ON DEATH* ");
			return false;
		case 5000:
			sendDialogue(player, TALKING_ALOT, 945, 5001, "Where would you like to go today.");
			return true;
		case 5001:
			sendOptionDialogue(player, new int[]{5002, 5003, 5004, 5005, 5006}, "Rock Crabs", "Castle Wars", "Green Dragons", "Barrows", "Warriors Guild");
			return true;
		case 5002:
			TeleportHandler.telePlayer(player, 2783, 10102, 0, 0, 0, false, false);
			return false;
		case 5003:
			TeleportHandler.telePlayer(player, 2441, 3089, 0, 0, 0, false, false);
			return false;
		case 5004:
			TeleportHandler.telePlayer(player, 2376, 3601, 0, 0, 0, false, false);
			return false;
		case 5006:
		     TeleportHandler.telePlayer(player, 2879, 3550, 0, 0, 0, false, false);
		return false;
		case 3050://safeportal
			sendOptionDialogue(player, new int[]{3051, 3052, 3053, -1}, "Safe PvP (FFA)", "PvP Island (Safe - FFA)", "Duel Arena", "Nowhere..");
			return true;
		case 3051:
			TeleportHandler.telePlayer(player, 2815, 5511, 0, 0, 2, true, false);
			player.sendMessage("You can fight other players here, but your items are SAFE here.");
			return false;
		case 3052:
			TeleportHandler.telePlayer(player, 3807, 2975, 0, 0, 2, true, false);
			player.sendMessage("You can fight other players here, but your items are SAFE here.");
			return false;
		case 3053:
			TeleportHandler.telePlayer(player, 3368, 3268, 0, 0, 2, true, false);
			return false;
		case 3029://niggers
			sendOptionDialogue(player, new int[]{3030, 3031, 3032, 3033, -1}, "Edgeville", "Mage Bank", "Rev Caves", "More options", "Nevermind..");
			return true;
		case 3030:
			TeleportHandler.telePlayer(player, 3087, 3492, 0, 0, 0, false, true);
			return false;
		case 3031:
			TeleportHandler.telePlayer(player, 2539, 4714, 0, 0, 0, false, true);
			return false;
		case 3032:
			TeleportHandler.telePlayer(player, 3077, 10058, 0, 0, 0, false, true);
			return false;
		case 3033:
			sendOptionDialogue(player, new int[]{3034, 3035, 3036, 3037, 3029}, "Green Dragons", "Varrock wild", "Obelisk (level 35 wild)", "Red Dragons (level 43 wild)", "Previous options..");
			return true;
		case 3034:
			TeleportHandler.telePlayer(player, 2986, 3601, 0, 0, 0, false, true);
			return false;
		case 3035:
			TeleportHandler.telePlayer(player, 3243, 3519, 0, 0, 0, false, true);
			return false;
		case 3036:
			TeleportHandler.telePlayer(player, 3106, 3794, 0, 0, 0, false, true);
			return false;
		case 3037:
			TeleportHandler.telePlayer(player, 3203, 3862, 0, 0, 0, false, true);
			return false;
		case 9789:
			sendOptionDialogue(player, new int[]{9790, -1}, "Why yes of course!", "I'm alright sir");
			return true;
		case 9790:
			player.teleport(3016, 9832, 0, false);
			player.sendMessage("<col=ff0000>*WARNING: BE SURE TO PROTECT RANGE*");
			player.sendMessage("Climb down the rope to get into the caverns.");
			return false;
		case 620:
			sendDialogue(player, SNOBBY, -1, 621, "What is this place?");
			return true;
		case 621:
			sendDialogue(player, HAPPY_TALKING, player.getSettings().getSpeakingTo().getNPC().getId(), 622, "This is a branch of the Bank of "+Constants.SERVER_NAME+". We have ", "branches in many towns.");
			return true;
		case 622:
			sendOptionDialogue(player, new int[]{623, 625}, "And what do you do?", "Didn't you used to be called the Bank of Varrock?");
			return true;
		case 623:
			sendDialogue(player, SNOBBY, -1, 624, "And what do you do?");
			return true;
		case 624:
			sendDialogue(player, HAPPY_TALKING, player.getSettings().getSpeakingTo().getNPC().getId(), -1, "We will look after your items and money for you.", "Leave your valuables with us if you want to keep them ", "safe.");
			return true;
		case 625:
			sendDialogue(player, SNOBBY, -1, 626, "Didn't you used to be called the Bank of Varrock?");
			return true;
		case 626:
			sendDialogue(player, CALM, player.getSettings().getSpeakingTo().getNPC().getId(), -1, "Yes we did, but people kept coming into our ", "branches outside of Varrock and telling us that our ", "signs were wrong. They acted as if we didn't know ", "what town we were in or something.");
			return true;
		//TUTORIAL SCENE:
		case 627:
		//	new TutorialScene(player).start();
			sendDialogue(player, CALM, player.getSettings().getSpeakingTo().getNPC().getId(), -1, "Your experience gained will be 5000x normal!","Good luck out there!");
			Cutscene.giveStarter(player);
			player.setPersonalCombatXpRate(5000);
			return false;
		case 628:
			sendDialogue(player, CALM, player.getSettings().getSpeakingTo().getNPC().getId(), -1, "Your experience gained will be normal!","Good luck out there!");
			player.setPersonalCombatXpRate(100);
			Cutscene.giveStarter(player);
			player.title = 54;
			player.getMask().setAppearanceUpdate(true);
			return false;
			
		case 9943:
			sendDialogue(player, CALM, player.getSettings().getSpeakingTo().getNPC().getId(), -1, "Your experience gained will be 2x normal than RuneScape's!","Good luck out there!");
			Cutscene.giveStarter(player);
			player.setPersonalCombatXpRate(4);
			player.title = 33;
			player.getMask().setAppearanceUpdate(true);
			return false;
			
			//christmas cracker pull:
		case 629:
			if (player.getInventory().contains(962)) {
				Player other = player.getAttribute("itemUsedOn", null);
				if (other == null || !other.isOnline())
					return false;
				if (other.getInventory().getFreeSlots() < 1) {
					player.sendMessage("The other player does not have enough inventory space to perform this action.");
					return false;
				} else {
					other.turnTo(player, false);
					int randomPhat = 1038 + (Misc.random(5) * 2);
					int randomItem = CRACKERREWARDS[Misc.random(17)];
					int amount = 1;
					if (randomItem == 995)
						amount = 100000 + Misc.random(150000);
					else if (randomItem >= 554 && randomItem <= 563)
						amount = 200 + Misc.random(300);
					int winner = Misc.random(1);
					//player.animate(animation);
					player.sendMessage("You pull a Christmas cracker...");
					player.getInventory().deleteItem(962, 1, player.getAttribute("itemSlot", 0), true);
					if (winner == 0) {
						if (ItemDefinition.forId(randomItem).isStackable())
							player.getInventory().addItem(randomItem, amount);
						else
							player.getInventory().set(player.getAttribute("itemSlot", 0), new Item(randomItem, amount));
						other.getInventory().addItem(randomPhat, 1);
						other.forceText("Hey! I got the cracker!");
						Logger.writeChristmasCrackerPullLog(player, other, new Item(randomItem, amount), new Item(randomPhat, 1));
					} else {
						player.getInventory().set(player.getAttribute("itemSlot", 0), new Item(randomPhat, 1));
						other.getInventory().addItem(randomItem, amount);
						player.forceText("Hey! I got the cracker!");
						Logger.writeChristmasCrackerPullLog(player, other, new Item(randomPhat, 1), new Item(randomItem, amount));
					}
					player.getInventory().refresh();
					other.getInventory().refresh();
					World.getWorld().getPlayerLoader().save(player);
					World.getWorld().getPlayerLoader().save(other);
				}
			}
			return false;
			
		case 630: //quick banker at home
			sendOptionDialogue(player, new int[]{631, 632, 633}, "I'd like to access my bank account, please.", "I'd like to check my PIN settings.", "I'd like to see my collection box.");
			return true;
		case 631:
			if (player.getAttribute("fromBank") != null) {
				ActionSender.sendInterfaceConfig(player, 667, 49, true);
				ActionSender.sendInterfaceConfig(player, 667, 50, true);
				player.getBonuses().refreshEquipScreen();
				ActionSender.sendInterface(player, 667);
			} else {
				player.getBank().openBank();
				player.removeAttribute("fromBank");
			}
			return false;
		case 632:
			player.sendMessage("Setting a pin doesn't work yet.");
			//TODO: Pin Settings
			return false;
		case 633:
			ActionSender.sendInterface(player, 109);
			return false;
		case 634:
			sendOptionDialogue(player, new int[]{635, 637}, "Sure.", "No thank you.");
			return true;
		case 635:
			sendDialogue(player, HAPPY_TALKING, -1, 636, "Sure, that seems like a fine price for a bank like this.");
			return true;
		case 636:
			if (player.getInventory().contains(995, 5000000)) {
				player.setCanUseQuickBankerAtHome(true);
				player.getInventory().deleteItem(995, 5000000);
				sendDialogue(player, HAPPY_TALKING, 7605, -1, "Thank you, I hope I may be of good use to you!");
				return true;
			} else {
				sendDialogue(player, SNOBBY_HEAD_MOVE, 7605, -1, "You don't seem to have 5,000,000 coins, ", "get back to me once you have it.");
				return true;
			}
		case 637:
			sendDialogue(player, HAPPY_TALKING, -1, -1, "No thank you, I will just walk a little extra ", "and use the free bankers over there.");
			return true;
			
			//General store portals:
		case 638: //nardah
			player.sendMessage("You step through the portal..");
			player.teleport(3418, 2905, 1, false);
			player.sendMessage(".. and arrive at the second floor of the general store of Nardah.");
			return false;
		case 639: //lummy
			player.sendMessage("You step through the portal..");
			player.teleport(3212, 3242, 1, false);
			player.sendMessage(".. and arrive at the second floor of the general store of Lumbridge.");
			return false;
		case 640: //varrock
			player.sendMessage("You step through the portal..");
			player.teleport(3215, 3412, 1, false);
			player.sendMessage(".. and arrive at the second floor of the general store of Varrock.");
			return false;
		case 641: //fally
			player.sendMessage("You step through the portal..");
			player.teleport(2954, 3388, 3, false);
			player.sendMessage(".. and arrive at the roof of the general store of Falador.");
			return false;
		case 642: //edge
			player.sendMessage("You step through the portal..");
			player.teleport(3078, 3511, 1, false);
			player.sendMessage(".. and arrive at the second floor of the general store of Edgeville.");
			return false;
			
			
		case 643: //Mandrith (for xp change)
			sendOptionDialogue(player, new int[]{655, 9549, 9511}, "Exchange PVP points.", "Cash in Ancient Artifacts", "Nevermind.");
			return true;
		/*case 644:
			sendDialogue(player, HAPPY_TALKING, -1, 645, "Could you change my combat experience rate, please?");
			return true;
		case 645:
			sendDialogue(player, CALM_TALK, player.getSettings().getSpeakingTo().getNPC().getId(), 646, "Certainly! Experience is an important aspect of fighting.");
			return true;
		case 646:
			//if (player.getPersonalCombatXpRate() == 100)
			//	sendDialogue(player, CALM_TALK, player.getSettings().getSpeakingTo().getNPC().getId(), 647, "Right now you have the maximum combat experience rate ", "that is available.");
		//	else
		//		sendOptionDialogue(player, new int[]{654, 650, 651, 653}, "Give me a 100% combat experience rate.", "Give me a 50% combat experience rate.", "Give me a 10% combat experience rate.", "Stop me from getting any combat experience.");
			/*return true;
		case 647:
			sendOptionDialogue(player, new int[]{648, 655}, "Ok, lower it.", "Nevermind.");
			return true;
		case 648:
			sendDialogue(player, HAPPY_TALKING, -1, 649, "Ok, lower it!");
			return true;
		case 649:
			sendOptionDialogue(player, new int[]{650, 651, 652, 653}, "Give me a 50% combat experience rate.", "Give me a 10% combat experience rate.", "Give me a 1% combat experience rate (realistic).", "Stop me from getting any combat experience.");
			return true;
		case 650:
			player.setPersonalCombatXpRate(50);
			sendDialogue(player, CALM_TALK, player.getSettings().getSpeakingTo().getNPC().getId(), -1, "When gaining experience in combat you will now receive ", "half of the normal experience.");
			return true;
		case 651:
			player.setPersonalCombatXpRate(10);
			sendDialogue(player, CALM_TALK, player.getSettings().getSpeakingTo().getNPC().getId(), -1, "When gaining experience in combat you will now receive ", "1/10 of the normal experience.");
			return true;
		case 652:
			player.setPersonalCombatXpRate(1);
			sendDialogue(player, CALM_TALK, player.getSettings().getSpeakingTo().getNPC().getId(), -1, "When gaining experience in combat you will now receive ", "1/100 of the normal experience.");
			return true;*/
	//	case 653:
		//	player.setPersonalCombatXpRate(0);
		//	sendDialogue(player, CALM_TALK, player.getSettings().getSpeakingTo().getNPC().getId(), -1, "From now on you won't receive any experience ", "from combat.");
		//	return true;
	//	case 654:
		//	player.setPersonalCombatXpRate(100);
		//	sendDialogue(player, HAPPY_TALKING, player.getSettings().getSpeakingTo().getNPC().getId(), -1, "When gaining experience in combat you will now receive ", "the maximum amount of experience possible.");
		//	return true;
		case 655:
			sendDialogue(player, CALM, -1, 9550, "I wish to make an exchange of pvp points please.");
			return true;
			
		case 9550:
			if (player.getPkPoints() > 0) {
			int amount = player.getPkPoints();
				player.sendMessage("You exchanged " + player.getPkPoints() + " pk points for " + player.getPkPoints() + " pvp tickets.");
				player.getInventory().addDropable(new Item(19864, player.getPkPoints()));
				player.addPkPoints(-amount);
				player.getInventory().refresh();
				sendDialogue(player, HAPPY_TALKING, player.getSettings().getSpeakingTo().getNPC().getId(), -1, "Thank you very much @PLAYER_NAME@!");
			} else {
			sendDialogue(player, SAD, player.getSettings().getSpeakingTo().getNPC().getId(), -1, "I'm very sorry, it seems that you have no pvp points", "to exchange.");
			}
			return true;
		case 9549:
			int[] PvpItems = {14876, 14877, 14878, 14879, 14880, 14881, 14882, 14883, 14884, 14885, 14886, 14887, 14888, 14889, 14890, 14891, 14892};
			int[] PvpPrices = {7000000, 3000000, 150000, 500000, 500000, 450000, 350000, 250000, 150000, 100000, 75000, 50000, 40000, 30000, 20000, 10000, 5000};
				for (int i = 0; i < PvpItems.length; i++) {
					if (player.getInventory().contains(PvpItems[i])) {
					int amount = player.getInventory().numberOf(PvpItems[i]);
						player.getInventory().deleteItem(PvpItems[i], amount);
						player.getInventory().addDropable(new Item(995, PvpPrices[i]*amount));
						player.getInventory().refresh();  
					} else {
					    	sendDialogue(player, SAD, player.getSettings().getSpeakingTo().getNPC().getId(), -1, "I'm very sorry, it seems that you have no pvp items", "to exchange.");
					}
				}
					return true;
	    case 9511:			
			sendDialogue(player, CALM, -1, -1, "Nevermind.");
			return true; //END OF NASTROTH PVP POINT SYSTEM.
			
		case 656: //nex landslide 
			String title = "There is no one currently fighting.";
			String lowerTitle = "Do you wish to climb down?";
			int count = 0;
			for (Player pl : World.getWorld().getPlayers()) {
				if (pl.getLocation().distance(NexAreaEvent.AREA_CENTER) < 16)
					count++;
			}
			if (count > 0) {
				title = "There "+(count == 1 ? "is" : "are")+" currently "+(count == 1 ? "one person" : count+" people")+" fighting.";
				lowerTitle = "Do you wish to join them?";
			}
			send2OptionDialogueWithLongTitle(player, new int[]{657, -1}, "Climb down.", "Stay here.");
			ActionSender.sendString(player, title+"<br>"+lowerTitle, 718, 0);
			return true;
		case 657:
			ObjectPacketHandler.forceWalk(player, Animation.create(9095), 2911, 5204, 3, new int[]{30, 90, -1}, null, 12.0);
			return false;
		case 658: //beast of burden
			int random = Misc.random(2);
			if (random == 0)
				sendDialogue(player, CALM_TALK, player.getSettings().getSpeakingTo().getNPC().getId(), -1, "Don't put over 100 kg of items on me, or I'll collapse.");
			else if (random == 1) {
				String hatName = "";
				int hatId = -1;
				if (player.getEquipment().get(0) != null) {
					player.getEquipment().get(0).getDefinition().getName();
					hatId = player.getEquipment().get(0).getId();
				}
				if (hatName.contains("partyhat") || hatId == 1050 || hatName.contains("h'ween mask"))
					sendDialogue(player, CALM_TALK, player.getSettings().getSpeakingTo().getNPC().getId(), -1, "Nice rare you're wearing there. Please let me hold it ", "for you, I really won't teleport away with it, boss..");
				else
					sendDialogue(player, CALM_TALK, player.getSettings().getSpeakingTo().getNPC().getId(), -1, "*burp* - Ah, that was a good fly!");
			} else if (random == 2)
				sendDialogue(player, CALM_TALK, player.getSettings().getSpeakingTo().getNPC().getId(), -1, "They call me beast of burden, ", "but I am more like a ninja chicken.");
			return true;
		case 659: //beast of burden
			if (player.getFamiliar() != null && player.getFamiliar().isBeastOfBurden())
				player.getFamiliar().open();
			return false;
		case 660: //familiar
			sendDialogue(player, HAPPY_TALKING, -1, 661, "Is that a proposal!? Yes, yes!!");
			return true;
		case 661: //familiar
			sendDialogue(player, HAPPY_TALKING, player.getSettings().getSpeakingTo().getNPC().getId(), -1, "I was joking, boss. I am feeling unpleasant now, ", "could you dismiss me please?");
			return true;
			
		case 662: //Richard the team cape seller
			sendOptionDialogue(player, new int[]{663, 669, 670}, "What do team capes do?", "Yes please!", "No thanks.");
			return true;
		case 663:
			sendDialogue(player, CONFUSED, -1, 664, "What do team capes do?");
			return true;
		case 664:
			sendDialogue(player, HAPPY_TALKING, 1783, 665, "If you and your friends all wear the same team cape, ", "you'll be less likely to attack your friends by accident, and ", "you'll find it easier to attack everyone else.");
			return true;
		case 665:
			sendDialogue(player, HAPPY_TALKING, 1783, 667, "They're very useful in Clan Wars and other player-vs-", "player combat areas where you might come across ", "friends you don't want to harm.");
			return true;
		case 667:
			sendDialogue(player, CONFUSED, 1783, 668, "So would you like to buy one?");
			return true;
		case 668:
			sendOptionDialogue(player, new int[]{669, 670}, "Yes please!", "No thanks.");
			return true;
		case 669:
			World.getWorld().getShopManager().openShop(player, 1783);
			return false;
		case 670:
			sendDialogue(player, CALM_TALK, -1, -1, "No thanks.");
			return true;
			
		case 671: //monks' talk
			sendOptionDialogue(player, new int[]{672, 675}, "Can you heal me? I'm injured.", "Isn't this place built a bit out the way?");
			return true;
		case 672:
			sendDialogue(player, CONFUSED, -1, 673, "Can you heal me? I'm injured.");
			return true;
		case 673:
			sendDialogue(player, CALM_TALK, player.getSettings().getSpeakingTo().getNPC().getId(), 674, "Ok.");
			return true;
		case 674:
			player.getSettings().getSpeakingTo().getNPC().animate(710);
			player.graphics(84);
			player.getSkills().heal((int) Math.ceil(player.getSkills().getLevelForExperience(Skills.CONSTITUTION) * 3)); //* 30% * 10
			player.sendMessage("You feel a little better.");
			return false;
		case 675:
			sendDialogue(player, CONFUSED, -1, 676, "Isn't this place built a bit out of the way?");
			return true;
		case 676:
			sendDialogue(player, CALM_TALK, player.getSettings().getSpeakingTo().getNPC().getId(), -1, 
					"We like it that way actually! We get disturbed less. We still ", 
					"get rather a large amount of travellers looking for ",
					"sanctuary and healing here as it is!");
			return true;
			
		case 677: //barbarians
			sendOptionDialogue(player, new int[]{678, 680}, "Let's fight!", "Goodbye.");
			return true;
		case 678:
			sendDialogue(player, EVIL, -1, 679, "Let's fight!");
			return true;
		case 679:
			NPC attackingBarbarian = player.getSettings().getSpeakingTo().getNPC();
			if (attackingBarbarian != null && !attackingBarbarian.isHidden())
				attackingBarbarian.getCombatExecutor().setVictim(player);
			return false;
		case 680:
			sendDialogue(player, SCARED, -1, -1, "Goodbye.");
			return true;
		case 681: //barbarian champion
			sendOptionDialogue(player, new int[]{682, 683}, "I challenge you!", "Er, no.");
			return true;
		case 682:
			sendDialogue(player, WHAT_THE_CRAP, 3090, 679, "Make peace with your god, outerlander!");
			return true;
		case 683:
			sendDialogue(player, SCARED, -1, -1, "Er, no.");
			return true;
		case 684: //home teleport to Nardah
			TeleportHandler.homeTeleport(player, true);
			return false;
		case 685: //home teleport to Lumbridge
			TeleportHandler.homeTeleport(player, false);
			return false;
			
		case 686:
			Commands.dropInventory(player);
			return false;
			
		case 687: //whip/dark bow colouring
			if (player.getCanUsePaint()) {
				if (player.getInventory().contains(4151) && !player.getInventory().contains(11235))
					sendDialogue(player, CONFUSED, 5029, 688, "Would you like me to paint your abyssal whip or dark bow?", "Or would you like to view my shop?");
				else
					sendDialogue(player, CONFUSED, 5029, 705, "If you bring me an abyssal whip or dark bow I ", "can paint it a different colour if you'd like.", "Or would you like to view my shop?");
			} else {
				sendDialogue(player, CONFUSED, 5029, 706, "Would you like to purchase my abyssal whip", "and dark bow painting services?", "Or would you like to view my shop?");
			}
			return true;
		case 688:
			sendOptionDialogue(player, new int[]{689, 696, 703, 704}, "Paint abyssal whip.", "Paint dark bow.", "View shop.", "Nevermind, bye.");
			return true;
		case 689:
			sendDialogue(player, CONFUSED, -1, 690, "Could you paint my abyssal whip, please?");
			return true;
		case 690:
			sendDialogue(player, CONFUSED, 5029, 691, "What colour would you like me to paint your abyssal whip?");
			return true;
		case 691:
			sendOptionDialogue(player, new int[]{692, 693, 694, 695}, "Yellow", "Blue", "White", "Green");
			return true;
		case 692:
			if (player.getInventory().contains(4151)) {
				player.getInventory().deleteItem(4151, 1);
				player.getInventory().addDropable(new Item(15441, 1));
				player.getInventory().refresh();
				sendDialogue(player, HAPPY_TALKING, 5029, -1, "Have fun!");
			} else {
				sendDialogue(player, SAD, 5029, -1, "For some reason the abyssal whip that I saw before ", "in your inventory is gone.");
			}
			return true;
		case 693:
			if (player.getInventory().contains(4151)) {
				player.getInventory().deleteItem(4151, 1);
				player.getInventory().addDropable(new Item(15442, 1));
				player.getInventory().refresh();
				sendDialogue(player, HAPPY_TALKING, 5029, -1, "Have fun!");
			} else {
				sendDialogue(player, SAD, 5029, -1, "For some reason the abyssal whip that I saw before ", "in your inventory is gone.");
			}
			return true;
		case 694:
			if (player.getInventory().contains(4151)) {
				player.getInventory().deleteItem(4151, 1);
				player.getInventory().addDropable(new Item(15443, 1));
				player.getInventory().refresh();
				sendDialogue(player, HAPPY_TALKING, 5029, -1, "Have fun!");
			} else {
				sendDialogue(player, SAD, 5029, -1, "For some reason the abyssal whip that I saw before ", "in your inventory is gone.");
			}
			return true;
		case 695:
			if (player.getInventory().contains(4151)) {
				player.getInventory().deleteItem(4151, 1);
				player.getInventory().addDropable(new Item(15444, 1));
				player.getInventory().refresh();
				sendDialogue(player, HAPPY_TALKING, 5029, -1, "Have fun!");
			} else {
				sendDialogue(player, SAD, 5029, -1, "For some reason the abyssal whip that I saw before ", "in your inventory is gone.");
			}
			return true;
		case 696:
			sendDialogue(player, CONFUSED, -1, 697, "Could you paint my dark bow, please?");
			return true;
		case 697:
			sendDialogue(player, CONFUSED, 5029, 698, "What colour would you like me to paint your dark bow?");
			return true;
		case 698:
			sendOptionDialogue(player, new int[]{699, 700, 701, 702}, "Yellow", "Blue", "White", "Green");
			return true;
		case 699:
			if (player.getInventory().contains(11235)) {
				player.getInventory().deleteItem(11235, 1);
				player.getInventory().addDropable(new Item(15701, 1));
				player.getInventory().refresh();
				sendDialogue(player, HAPPY_TALKING, 5029, -1, "Have fun!");
			} else {
				sendDialogue(player, SAD, 5029, -1, "For some reason the dark bow that I saw before ", "in your inventory is gone.");
			}
			return true;
		case 700:
			if (player.getInventory().contains(11235)) {
				player.getInventory().deleteItem(11235, 1);
				player.getInventory().addDropable(new Item(15702, 1));
				player.getInventory().refresh();
				sendDialogue(player, HAPPY_TALKING, 5029, -1, "Have fun!");
			} else {
				sendDialogue(player, SAD, 5029, -1, "For some reason the dark bow that I saw before ", "in your inventory is gone.");
			}
			return true;
		case 701:
			if (player.getInventory().contains(11235)) {
				player.getInventory().deleteItem(11235, 1);
				player.getInventory().addDropable(new Item(15703, 1));
				player.getInventory().refresh();
				sendDialogue(player, HAPPY_TALKING, 5029, -1, "Have fun!");
			} else {
				sendDialogue(player, SAD, 5029, -1, "For some reason the dark bow that I saw before ", "in your inventory is gone.");
			}
			return true;
		case 702:
			if (player.getInventory().contains(11235)) {
				player.getInventory().deleteItem(11235, 1);
				player.getInventory().addDropable(new Item(15704, 1));
				player.getInventory().refresh();
				sendDialogue(player, HAPPY_TALKING, 5029, -1, "Have fun!");
			} else {
				sendDialogue(player, SAD, 5029, -1, "For some reason the dark bow that I saw before ", "in your inventory is gone.");
			}
			return true;
		case 703:
			World.getWorld().getShopManager().openShop(player, 105);
			//sendDialogue(player, SAD, 5029, -1, "I'm sorry this feature is still under construction.");
			return false;
		case 704:
			sendDialogue(player, CALM_TALK, -1, 5728, "I was wondering if you could help me?", "I need to reset my combat stats!");
	   return true;
			// Level reset
		case 5728:
			sendDialogue(player, HAPPY_TALKING, 5029, 5729,
					"Why yes of course!",
					"What combat skill would you like to reset?");
			return true;
			// COMBAT RESET NIGGER
		case 5729:
			sendOptionDialogue(player,
					new int[] { 5730, 5731, 5732, 5733, 5734 }, "Defence",
					"Attack", "Constitution", "Strength", "More options...");
			return true;
		case 5730:
			sendDialogue(player, CALM_TALK, 5029, 5799,
					"Are you sure you wish to reset your: Defence to level 1?",
					"This action can not be changed once complete!");
			return true;
		case 5799:
			sendOptionDialogue(player, new int[] { 5800, -1 },
					"Yes reset my Defence to level one!",
					"Nevermind I don't wish to do this!");
			return true;
		case 5800:
			player.getSkills().setLevelAndXP(1, 1, 1);
			player.getSkills().refresh();
			player.sendMessage("Success! Your Defence level has been set to 1.");
			return false; // TODO FINISH THIS
		case 5731:
			sendDialogue(player, CALM_TALK, 5029, 5909,
					"Are you sure you wish to reset your: Attack to level 1?",
					"This action can not be changed once complete!");
			return true;
		case 5909:
			sendOptionDialogue(player, new int[] { 5910, -1 },
					"Yes reset my Attack to level one!",
					"Nevermind I don't wish to do this!");
			return true;
		case 5910:
			player.getSkills().setLevelAndXP(0, 1, 1);
			player.getSkills().refresh();
			player.sendMessage("Success! Your Attack level has been set to 1.");
			return false;
		case 5732:
			sendDialogue(
					player,
					CALM_TALK,
					5029,
					5911,
					"Are you sure you wish to reset your: Constitution to level 1?",
					"This action can not be changed once complete!");
			return true;
		case 5911:
			sendOptionDialogue(player, new int[] { 5912, -1 },
					"Yes reset my Constitution to level one!",
					"Nevermind I don't wish to do this!");
			return true;
		case 5912:
			player.getSkills().setLevelAndXP(0, 1, 1);
			player.getSkills().refresh();
			player.sendMessage("Success! Your Constitution level has been set to 1.");
			return false;
		case 5734:
			sendDialogue(
					player,
					CALM_TALK,
					5029,
					5913,
					"Are you sure you wish to reset your: Strength to level 1?",
					"This action can not be changed once complete!");
			return true;
		case 5913:
			sendOptionDialogue(player, new int[] { 5914, -1 },
					"Yes reset my Strength to level one!",
					"Nevermind I don't wish to do this!");
			return true;
		case 5914:
			player.getSkills().setLevelAndXP(2, 1, 1);
			player.getSkills().refresh();
			player.sendMessage("Success! Your Constitution level has been set to 1.");
			return false;
		case 705:
			sendOptionDialogue(player, new int[]{703, 704}, "View shop.", "Nevermind, bye.");
			return true;
		case 706:
			sendOptionDialogue(player, new int[]{707, 703, 704, -1}, "Purchase painting services.", "View shop.", "Ask about level reset.", "Nevermind.");
			return true;
		case 707:
			sendDialogue(player, CONFUSED, -1, 708, "I would like to purchase your painting services.", "How much will that cost me?");
			return true;
		case 708:
			sendDialogue(player, HAPPY_TALKING, 5029, 709, "Unlimited access to my abyssal whip and dark bow ", "painting services will only cost you 250 pk points.");
			return true;
		case 709:
			sendOptionDialogue(player, new int[]{710, 712}, "Sure.", "No, thank you.");
			return true;
		case 710:
			sendDialogue(player, HAPPY_TALKING, -1, 711, "Sure.");
			return true;
		case 711:
			if (player.getPkPoints() >= 250) {
				player.addPkPoints(-250);
				player.setCanUsePaint(true);
				sendDialogue(player, HAPPY_TALKING, 5029, 688, "Thank you. Your weapons will be that of a ", "true warrior when I have painted them!", "Would you like me to pain them now?");
			} else {
				sendDialogue(player, SAD, 5029, -1, "I am afraid you don't have 250 pk points.");
			}
			return true;
		case 712:
			sendDialogue(player, CALM_TALK, -1, -1, "No, thank you.");
			return true;
			
		case 713: //ghost in stonghold of security
			sendDialogue(player, MEAN_FACE, 11246, 714, "Hmph, mortals..");
			return true;
		case 714:
			sendOptionDialogue(player, new int[]{718, 715}, "Could you teleport me somewhere?", "Hmph, ghosts..");
			return true;
		case 715:
			sendDialogue(player, MEAN_FACE, -1, 716, "Hmph, ghosts..");
			return true;
		case 716:
			sendDialogue(player, MEAN_FACE, 11246, 717, "You don't like ghosts?! Taken to my lands you ", "will be, full of ghosts, foolish mortal!");
			return true;
		case 717:
			final NPC ghost = player.getSettings().getSpeakingTo().getNPC();
			ghost.animate(TeleportHandler.ANCIENT_ANIM);
            ghost.graphics(TeleportHandler.ANCIENT_GRAPHIC);
			World.getWorld().submit(new Tick(5) {
                @Override
                public void execute() {
                	if (ghost.isHidden()) {
                		ghost.setHidden(false);
                		stop();
                	} else
                		ghost.setHidden(true);
                }
            });
			player.teleportWithAnimAndGfx(3232, 9317, 0, true, false);
			return false;
		case 718:
			sendDialogue(player, CONFUSED, -1, 719, "Could you teleport me somewhere?");
			return true;
		case 719:
			sendDialogue(player, CALM_TALK, 11246, -1, "Too many questions..");
			return true;
			
		case 720: //ghost shop
			sendOptionDialogue(player, new int[]{721, 712}, "Yeah I'll take a look!", "No, thank you.");
			return true;
		case 721:
			//World.getWorld().getShopManager().openShop(player, 1699);
			return false;
			
		case 722: //restless ghost
			if (player.hasItem(4250))
				sendOptionDialogue(player, new int[]{727, 731}, "Is there anything cool to do out here?", "I was teleported here.");
			else
				sendOptionDialogue(player, new int[]{727, 723, 731}, "Is there anything cool to do out here?", "How can I speak with the other ghosts?", "I was teleported here.");
			return true;
		case 723:
			sendDialogue(player, CALM_TALK, 457, 724, "Green ghosts like that can only be communicated to", "when you're wearing a ghostspeak amulet.", "Would you like one?");
			return true;
		case 724:
			sendOptionDialogue(player, new int[]{725, 712}, "Sure.", "No, thank you.");
			return true;
		case 725:
			sendDialogue(player, HAPPY_TALKING, -1, 726, "Sure.");
			return true;
		case 726:
			if (player.getInventory().getFreeSlots() < 1)
				sendDialogue(player, SAD, 457, -1, "You'll need one free inventory space, sorry dude.");
			else if (player.hasItem(4250))
				sendDialogue(player, CALM_TALK, 457, -1, "I see you already have an amulet.");
			else {
				player.getInventory().addItem(new Item(4250, 1));
				player.getInventory().refresh();
				sendDialogue(player, HAPPY_TALKING, 457, -1, "Have fun with it!");
			}
				
			return true;
		case 727:
			sendDialogue(player, HAPPY_TALKING, -1, 728, "Is there anything cool to do out here?");
			return true;
		case 728:
			sendDialogue(player, HAPPY_TALKING, 457, 729, "There is a rumour that this pyramid contains", " a hidden sword.");
			return true;
		case 729:
			sendDialogue(player, HAPPY_TALKING, 457, 730, "So finding that sword would be a fun thing", "to do I suppose!");
			return true;
		case 730:
			sendDialogue(player, HAPPY_TALKING, -1, -1, "Thanks for the information, I might search", "for this sword.");
			return true;
		case 731:
			sendDialogue(player, CALM_TALK, -1, 732, "I was teleported here.");
			return true;
		case 732:
			sendDialogue(player, CALM_TALK, 457, 733, "Ah, by the mean ghost under the village of the", "barbarians I suppose. He is afraid of nothing.");
			return true;
		case 733:
			sendDialogue(player, CALM_TALK, 457, -1, "Except for the hidden sword of course, ", "but every ghost is afraid of that.");
			return true;
			
		case 734: //ghost in stonghold of security (part 2)
			sendDialogue(player, HAPPY_TALKING, -1, 735, "I found it in the pyramid which you teleported", "me too, when you said that I would see a lot", "of ghosts. I didn't see more than here though.");
			return true;
		case 735:
			sendDialogue(player, CALM_TALK, 11246, 736, "Hmph, a sword like that shouldn't be in", "the hands of mortals.");
			return true;
		case 736:
			sendDialogue(player, CALM_TALK, 11246, 737, "I've found a mortal sword some time back.", "I can't use it, so I could give it to you", "if you hand me over that sword.");
			return true;
		case 737:
			sendOptionDialogue(player, new int[]{738, 712}, "Deal!", "No, thank you.");
			return true;
		case 738:
			sendDialogue(player, HAPPY_TALKING, -1, 739, "Deal!");
			return true;
		case 739:
			if (player.getEquipment().get(3) != null && player.getEquipment().get(3).getId() == 10858) {
				player.getEquipment().set(3, null);
				player.getEquipment().set(3, new Item(7806, 1));
				player.getMask().setAppearanceUpdate(true);
			} else if (player.getInventory().contains(new Item(10858, 1))) {
				player.getInventory().deleteItem(10858, 1);
				player.getInventory().addItem(new Item(7806, 1));
				player.getInventory().refresh();
			} else {
				sendDialogue(player, EVIL, 11246, -1, "Haha, you appear to have lost the sword,", "foolish mortal!");
				return true;
			}
			sendDialogue(player, CALM_TALK, 11246, -1, "Have fun with it, foolish mortal!");
			return true;
			
		case 740: //pikkupstix no. 2
			sendDialogue(player, CALM_TALK, 6971, 741, "I am so secret that there is not even a right-click", "option on me to trade me. But I do have a ", "secret shop!");
			return true;
		case 741:
			sendDialogue(player, CALM_TALK, 6971, 742, "It contains items that I found in Pikkupstix's", "house, he could probably not sell them because his", "shop is almost full you know.");
			return true;
		case 742:
			sendDialogue(player, CONFUSED, 6971, 743, "Would you like to see my shop?");
			return true;
		case 743:
			sendOptionDialogue(player, new int[]{744, 712, 745}, "View shop.", "No, thank you.", "You piece of scum, I'm gonna call the police!");
			return true;
		case 744:
			//World.getWorld().getShopManager().openShop(player, 6971);
			return false;
		case 745:
			sendDialogue(player, MEAN_FACE, -1, 746, "You piece of scum, I'm gonna call the police!");
			return true;
		case 746:
			sendDialogue(player, CONFUSED, 6971, 747, "I'm what? And who will you call with what??", "I'm confused.");
			return true;
		case 747:
			sendDialogue(player, HAPPY_TALKING, -1, -1, "No you're not confused. You don't have the ", "brains to be so. Bald fatty with botted 99 cape!");
			return true;
			
		case 748: //duel forfeit
			if (player.getActivity() instanceof DuelActivity) {
				DuelActivity duel = (DuelActivity) player.getActivity();
				player.setAttribute("duellingForfeit", Boolean.TRUE);
				duel.endSession();
				player.setActivity(Mob.DEFAULT_ACTIVITY);
			}
			return false;
			
		case 749: //ghaslor the elder
			sendDialogue(player, HAPPY_TALKING, -1, 750, "A pleasure to meet you.");
			return true;
		case 750:
			sendDialogue(player, CONFUSED, 3029, 751, "So what can I do for you on this fine day?");
			return true;
		case 751:
			sendOptionDialogue(player, new int[]{752, 763}, "Do you have anything for sale?", "Nothing, thanks.");
			return true;
		case 752:
			sendDialogue(player, CONFUSED, -1, 753, "Do you have anything for sale?");
			return true;
		case 753:
			sendDialogue(player, CALM_TALK, 3029, 754, "Knowledge, such as the book I am weilding. Books that", "are full of secrets and can guide you to secret locations.");
			return true;
		case 754:
			sendDialogue(player, CONFUSED, 3029, 755, "Would you like such a book?");
			return true;
		case 755:
			sendOptionDialogue(player, new int[]{756, 712}, "Most certainly!", "No, thank you.");
			return true;
		case 756:
			sendDialogue(player, HAPPY_TALKING, -1, 757, "Most certainly!");
			return true;
		case 757:
			sendDialogue(player, CALM_TALK, 3029, 758, "I have two books that I can sell to you. The one", "being a book of knowledge, costing 300 pk points,", "the other being a book o' piracy, 500 pk points.");
			return true;
		case 758:
			sendOptionDialogue(player, new int[]{759, 761, 712}, "Book of knowledge (300 pk points)", "Book o' piracy (500 pk points)", "No, thank you.");
			return true;
		case 759:
			sendDialogue(player, HAPPY_TALKING, -1, 760, "A book of knowledge, please.");
			return true;
		case 760:
			if (player.getPkPoints() < 300)
				sendDialogue(player, SAD, 3029, -1, "You do not have enough pk points.");
			else if (player.getInventory().getFreeSlots() < 1)
				sendDialogue(player, CALM_TALK, 3029, -1, "You will need at least one free inventory space.");
			else {
				player.addPkPoints(-300);
				player.getInventory().addItem(11640, 1);
				player.getInventory().refresh();
				sendDialogue(player, HAPPY_TALKING, 3029, -1, "Excellent choice. You should know that the books", "can be magically updated with new information", " every now and then.");
			}
			return true;
		case 761:
			sendDialogue(player, HAPPY_TALKING, -1, 762, "A book o' piracy, please.");
			return true;
		case 762:
			if (player.getPkPoints() < 500)
				sendDialogue(player, SAD, 3029, -1, "You do not have enough pk points.");
			else if (player.getInventory().getFreeSlots() < 1)
				sendDialogue(player, CALM_TALK, 3029, -1, "You will need at least one free inventory space.");
			else {
				player.addPkPoints(-500);
				player.getInventory().addItem(7144, 1);
				player.getInventory().refresh();
				sendDialogue(player, HAPPY_TALKING, 3029, -1, "Excellent choice. You should know that the books", "can be magically updated with new information", " every now and then.");
			}
			return true;
		case 763:
			sendDialogue(player, CALM_TALK, -1, -1, "Nothing, thanks.");
			return true;
			
		case 764: //Rokuh shop
			sendOptionDialogue(player, new int[]{765, 712}, "Of course, you made me curious.", "No, thank you.");
			return true;
		case 765:
			sendDialogue(player, HAPPY_TALKING, -1, 766, "Of course, you made me curious.");
			return true;
		case 766:
			World.getWorld().getShopManager().openShop(player, 13180);
			return false;
			
		case 767: //mime
			sendOptionDialogue(player, new int[]{768, 712}, "Free? Sure!", "No, thank you.");
			return true;
		case 768:
			sendDialogue(player, HAPPY_TALKING, -1, 769, "Free? Sure!");
			return true;
		case 769:
			if (player.getInventory().getFreeSlots() < 5)
				sendDialogue(player, SAD, 12189, -1, "You will need at least five free inventory spaces.");
			else {
				player.getInventory().addItem(3061, 1);
				player.getInventory().addItem(3059, 1);
				player.getInventory().addItem(3058, 1);
				player.getInventory().addItem(3057, 1);
				player.getInventory().addItem(3060, 1);
				player.getInventory().refresh();
				sendDialogue(player, HAPPY_TALKING, 12189, -1, "You will look just like me.");
			}
			return true;
			
		case 770: //freaky forester
			sendOptionDialogue(player, new int[]{771, 712}, "Free? Sure!", "No, thank you.");
			return true;
		case 771:
			sendDialogue(player, HAPPY_TALKING, -1, 772, "Free? Sure!");
			return true;
		case 772:
			if (player.getInventory().getFreeSlots() < 3)
				sendDialogue(player, SAD, 2458, -1, "You will need at least three free inventory spaces.");
			else {
				player.getInventory().addItem(6181, 1);
				player.getInventory().addItem(6180, 1);
				player.getInventory().addItem(6182, 1);
				player.getInventory().refresh();
				sendDialogue(player, HAPPY_TALKING, 2458, -1, "That will look great on you.");
			}	
			return true;
			
		case 773: //teleport crystal
			if (player.getActivity().getActivityId() != -1) {
				player.sendMessage("You can't save your location here.");
				return false;
			}
			player.setSavedX(player.getLocation().getX());
			player.setSavedY(player.getLocation().getY());
			player.setSavedZ(player.getLocation().getZ());
			player.sendMessage("Your location has been saved. You can teleport to it at any time by activating your");
			player.sendMessage("teleport crystal and selecting 'Teleport to Saved Area'.");
			return false;
		case 774:
			int itemId = player.getAttribute("teleportCrystal");
			if (!player.getInventory().contains(itemId))
				return false;
            if (player.getSkills().isDead())
            	return false;
            if (player.getAttribute("teleblock", 0) > World.getTicks()) {
                player.sendMessage("A teleport block has been cast on you!");
                return false;
            }
            if (!player.getActivity().onTeleport(player))
                return false;
            if (player.getSavedX() == -1 && player.getSavedY() == -1 && player.getSavedZ() == -1) {
            	player.sendMessage("You haven't saved any destination yet.");
            	return false;
            }
			//We don't show the wildi warning, because if the player declines it, a charge is used up.
			if (itemId == 6102)
				player.getInventory().getContainer().set(
						player.getInventory().getContainer().getThisItemSlot(new Item(itemId, 1)), null);
			else
				player.getInventory().getContainer().set(
						player.getInventory().getContainer().getThisItemSlot(new Item(itemId, 1)), new Item(itemId + 1, 1));
			player.getInventory().refresh();
			player.teleportWithAnimAndGfx(player.getSavedX(), player.getSavedY(), player.getSavedZ(), false, false);
			if (itemId == 6102)
				player.sendMessage("Your teleport crystal is destroyed.");
			else {
				int charges = ((itemId - 6102) * -1);
				player.sendMessage("Your teleport crystal has "+charges+" charge"+(charges > 1 ? "s" : "")+" left.");
			}
			return false;
		case 775:
			player.teleportWithAnimAndGfx(3093, 3249, 0, false, true);
			return false;
		case 776:
			player.teleportWithAnimAndGfx(3293, 3183, 0, false, true);
			return false;
			
		case 778: //Lanthus
			sendOptionDialogue(player, new int[]{779, 712}, "Certainly!", "No, thank you.");
			return true;
		case 779:
			ActionSender.sendInterface(player, 60);
			return false;
			
		case 780: //kick
			Player other = player.getAttribute("reportedPlayer");
			if (other != null) {
				if (other.getRights() >= 2) {
					player.sendMessage("You can't kick another administrator.");
					return false;
				}
				ActionSender.sendLogout(other, 10);
				player.sendMessage("You kicked "+other.getDisplayName()+" out of the game.");
			}
			return false;
		case 781: //mute
			Player other1 = player.getAttribute("reportedPlayer");
			if (other1 != null) {
				if (other1.getRights() >= 2) {
					player.sendMessage("You can't mute another administrator.");
					return false;
				}
				World.getWorld().getOffencesHandler().addMuted(other1, false);
				//SAVING OFFENCES HERE
				player.sendMessage("You muted "+other1.getDisplayName()+" for 24 hours.");
			}
			return false;
		case 782: //unmute
			Player other2 = player.getAttribute("reportedPlayer");
			if (other2 != null) {
				if (!World.getWorld().getOffencesHandler().getMutedPlayers().contains(other2.getUsername())) {
					player.sendMessage("The muted players list doesn't contain the player: "+other2.getDisplayName()+".");
					return false;
				}
				World.getWorld().getOffencesHandler().unMute(other2, false);
				//SAVING OFFENCES HERE
				player.sendMessage("You removed "+other2.getDisplayName()+"'s mute.");
			}
			return false;
		case 783: //ban
			Player other3 = player.getAttribute("reportedPlayer");
			if (other3 != null) {
				if (other3.getRights() >= 2) {
					player.sendMessage("You can't ban another administrator.");
					return false;
				}
				World.getWorld().getOffencesHandler().addBan(other3, false);
				other3.getConnection().getChannel().disconnect();
				//SAVING OFFENCES HERE
				player.sendMessage("You banned "+other3.getDisplayName()+" for 24 hours.");
			}
			return false;
		case 784: //ipban
			Player other4 = player.getAttribute("reportedPlayer");
			if (other4 != null) {
				World.getWorld().getOffencesHandler().addBan(other4, true);
				for (Player pl : World.getWorld().getPlayers()) {
					if (OffencesHandler.formatIp(pl.getConnection().getChannel().getRemoteAddress().toString()).equals(OffencesHandler.formatIp(other4.getConnection().getChannel().getRemoteAddress().toString()))) {
						pl.getConnection().getChannel().disconnect();
					}
				}
				//SAVING OFFENCES HERE
				player.sendMessage("You permanently banned "+other4.getDisplayName()+" and all other accounts from "+(other4.getAppearance().getGender() == 1 ? "her" : "his")+" ip address.");
			}
			return false;
		case 785: //ipmute
			Player other5 = player.getAttribute("reportedPlayer");
			if (other5 != null) {
				if (other5.getRights() >= 2) {
					player.sendMessage("You can't mute another administrator.");
					return false;
				}
				World.getWorld().getOffencesHandler().addMuted(other5, true);
				//SAVING OFFENCES HERE
				player.sendMessage("You permanently muted "+other5.getDisplayName()+" and all other accounts from "+(other5.getAppearance().getGender() == 1 ? "her" : "his")+" ip address.");
			}
			return false;
		case 786: //more options (1)
			DialogueManager.sendOptionDialogue(player, new int[]{795, 790, 791, 792, 787}, "Ipmute this player.", "Unipmute this player.", "Teleport this player home.", "Copy this player.", "Go back.");
			return true;
		case 787: //first options (1)
			DialogueManager.sendOptionDialogue(player, new int[]{780, 793, 782, 794, 786}, "Kick this player.", "Mute this player.", "Unmute this player.", "Ban this player.", "More options.");
			return true;
		case 788: //more options (2) > owner only
			DialogueManager.sendOptionDialogue(player, new int[]{795, 790, 791, 792, 796}, "Ipmute this player.", "Unipmute this player.", "Teleport this player home.", "Copy this player.", "Ipban this player.");
			return true;
		case 789: //first options (2) > owner only
			DialogueManager.sendOptionDialogue(player, new int[]{780, 793, 782, 794, 788}, "Kick this player.", "Mute this player.", "Unmute this player.", "Ban this player.", "More options.");
			return true;
		case 790: //unipmute
			Player other6 = player.getAttribute("reportedPlayer");
			String ip = OffencesHandler.formatIp(other6.getConnection().getChannel().getRemoteAddress().toString());
			if (other6 != null) {
				if (!World.getWorld().getOffencesHandler().getMutedIps().contains(ip)) {
					player.sendMessage("The muted ip addresses list doesn't contain "+other6.getDisplayName()+"'s ip address.");
					return false;
				}
				World.getWorld().getOffencesHandler().unMute(ip, true);
				//SAVING OFFENCES HERE
				player.sendMessage("You removed the ip address mute from "+other6.getDisplayName()+"'s ip address.");
			}
			return false;
		case 791: //tele player home
			Player other7 = player.getAttribute("reportedPlayer");
			if (other7 != null) {
				if (other7.getRights() == 2 && Commands.teleToAdminDisabled) {
					player.sendMessage("This administrator has disabled teleporting him using the 'teletoadmin' command.");
					other7.sendMessage(player.getDisplayName()+" tried to teleport you to home.");
					return false;
				}
	            if (other7.getAttribute("teleblock", 0) > World.getTicks()) {
	                player.sendMessage("A teleport block has been cast on this player.");
	                return false;
	            }
	            if (!other7.getActivity().onTeleport(other7)) {
	            	player.sendMessage("This players activity doesn't allow "+(other7.getAppearance().getGender() == 1 ? "her" : "him")+" to be teleported.");
	    			return false;
	            }
				if (!other7.hasReceivedStarter()) {
					player.sendMessage("Let this new adventurer finish "+(other7.getAppearance().getGender() == 1 ? "her" : "his")+" tutorial first, before "+(other7.getAppearance().getGender() == 1 ? "she" : "he")+" gets teleported away.");
					return false;
				}
		        if (other7.getSkills().isDead()) {
		        	player.sendMessage("This player is busy dying, please wait till he's death before you teleport him.");
		        	return false;
		        }
            	player.animate(1818);
            	player.graphics(343);
				other7.teleportWithAnimAndGfx(Mob.DEFAULT, true, false);
				other7.sendMessage("You have been teleported home by an administrator.");
			}
			return false;
		case 792: //copy player
			Player other8 = player.getAttribute("reportedPlayer");
			if (other8 != null) {
				for(int skill = 0; skill < 25; skill++) {
					double othersXp = other8.getSkills().getXp(skill);
					int othersLevel = other8.getSkills().getLevel(skill);
					player.getSkills().setXp(skill, othersXp);
					player.getSkills().setLevel(skill, othersLevel);
					player.getSkills().refresh();
				}
				player.heal(1555);
				player.getSkills().restorePray(120);
				for (int i = 0; i < Equipment.SIZE; i++) {
					Item item = other8.getEquipment().get(i);
					player.getEquipment().getContainer().set(i, item);
				}
				player.getEquipment().recalculateHpModifier();
				for (int i = 0; i < other8.getAppearance().getLook().length; i++) {
					player.getAppearance().getLook()[i] = other8.getAppearance().getLook()[i];
				}
				for (int i = 0; i < other8.getAppearance().getColour().length; i++) {
					player.getAppearance().getColour()[i] = other8.getAppearance().getColour()[i];
				}
				player.getAppearance().setGender((byte) other8.getAppearance().getGender());
				
			} else if (player.getRights() < 2) {
				player.sendMessage("Only administrators can use this function.");
			}
			return false;
		case 793: //are you sure: mute
			Player other9 = player.getAttribute("reportedPlayer");
			if (other9 != null) {
				send2OptionDialogueWithLongTitle(player, new int[]{781, -1}, "Mute "+(other9.getAppearance().getGender() == 1 ? "her" : "him")+".", "Nevermind, I forgave "+(other9.getAppearance().getGender() == 1 ? "her" : "him")+".");
				ActionSender.sendString(player, "Are you sure you want to mute this player?", 718, 0);
			} else {
				player.sendMessage("Couldn't find the player.");
				return false;
			}
			return true;
		case 794: //are you sure: ban
			Player other10 = player.getAttribute("reportedPlayer");
			if (other10 != null) {
				send2OptionDialogueWithLongTitle(player, new int[]{783, -1}, "Ban "+(other10.getAppearance().getGender() == 1 ? "her" : "him")+".", "Nevermind, I forgave "+(other10.getAppearance().getGender() == 1 ? "her" : "him")+".");
				ActionSender.sendString(player, "Are you sure you want to ban this player?", 718, 0);
			} else {
				player.sendMessage("Couldn't find the player.");
				return false;
			}
			return true;
		case 795: //are you sure: ipmute
			Player other11 = player.getAttribute("reportedPlayer");
			if (other11 != null) {
				send2OptionDialogueWithLongTitle(player, new int[]{785, -1}, "Ipmute "+(other11.getAppearance().getGender() == 1 ? "her" : "him")+".", "Nevermind, I forgave "+(other11.getAppearance().getGender() == 1 ? "her" : "him")+".");
				ActionSender.sendString(player, "Are you sure you want to ipmute this player?", 718, 0);
			} else {
				player.sendMessage("Couldn't find the player.");
				return false;
			}
			return true;
		case 796: //are you sure: ipban
			Player other12 = player.getAttribute("reportedPlayer");
			if (other12 != null) {
				send2OptionDialogueWithLongTitle(player, new int[]{784, -1}, "Ipban "+(other12.getAppearance().getGender() == 1 ? "her" : "him")+".", "Nevermind, I forgave "+(other12.getAppearance().getGender() == 1 ? "her" : "him")+".");
				ActionSender.sendString(player, "Are you sure you want to ipban this player?", 718, 0);
			} else {
				player.sendMessage("Couldn't find the player.");
				return false;
			}
			return true;
			
		case 797: //strange old man (barrows)
			DialogueManager.sendOptionDialogue(player, new int[]{798, 712}, "Yes, please!", "No, thank you.");
			return true;
		case 798:
			sendDialogue(player, HAPPY_TALKING, -1, 799, "Yes, please!");
			return true;
		case 799:
			sendDialogue(player, CALM_TALK, 2024, 800, "Becase I've got 5 grandchildren to feed, I'm afraid", "I have to charge you 5 gp.");
			return true;
		case 800:
			DialogueManager.sendOptionDialogue(player, new int[]{801, 803}, "Of course.", "That's too much for me.");
			return true;
		case 801:
			sendDialogue(player, HAPPY_TALKING, -1, 802, "Of course.");
			return true;
		case 802:
			if (player.getInventory().contains(995, 5)) {
				player.getInventory().deleteItem(995, 5);
				player.getInventory().addDropable(new Item(952));
				player.getInventory().refresh();
				sendDialogue(player, HAPPY_TALKING, 2024, -1, "Have fun with it!");
			} else
				sendDialogue(player, SAD, 2024, -1, "You don't seem to have 5 gp.");
			return true;
		case 803:
			sendDialogue(player, SAD, -1, 804, "That's too much for me.");
			return true;
		case 804:
			if (player.getInventory().contains(995, 5))
				sendDialogue(player, EVIL, 2024, -1, "No it isn't..");
			else {
				player.getInventory().addDropable(new Item(995, 2));
				player.getInventory().refresh();
				sendDialogue(player, HAPPY_TALKING, 2024, -1, "Haha, poor "+(player.getAppearance().getGender() == 1 ? "woman" : "man")+"! Here take this.");
			}
			return true;
			
//		case 805: //Fight Caves exit dialogue.
//			if (player.getActivity() instanceof FightCavesActivity) {
//				player.getActivity().endSession();
//				World.getWorld().submit(new Tick(1) {
//		            @Override
//		            public void execute() {
//		            	stop();
//						sendDialogue(player, CALM_TALK, 2617, -1, "Do not feel bad human,", "I knew you couldn't do it.");
//		            }
//		        });
//			}
//			return true;
			
		case 5100: //ancients effigies
			sendOptionDialogue(player, new int[]{5101, 5102}, "Summoning", "Agility");
			return true;
		case 5101:
			if(player.getSkills().getLevel(23) >= 97) {
				player.getInventory().addItem(18782, 1);
				player.getInventory().deleteItem(18781, 1);
				player.getSkills().addExperience(23, 1000);
				player.sendMessage("Your Gorged Effigy splits in the middle, and revealed a Dragonkin Lamp!");
			} else if(player.getSkills().getLevel(23) <= 97) {
				player.sendMessage("You need 97 Summoning to open this up.");
			}
			return false;
		case 5102:
			if(player.getSkills().getLevel(16) >= 97) {
				player.getInventory().addItem(18782, 1);
				player.getInventory().deleteItem(18781, 1);
				player.getSkills().addExperience(16, 1000);
				player.sendMessage("Your Gorged Effigy splits in the middle, and revealed a Dragonkin Lamp!");
			} else if(player.getSkills().getLevel(16) <= 97) {
				player.sendMessage("You need 97 Agility to open this up.");
			}
			return false;
		case 6100:
			sendOptionDialogue(player, new int[]{6101, 6102}, "Woodcutting", "Fletching");
			return true;
		case 6101:
			if(player.getSkills().getLevel(8) >= 95) {
				player.getInventory().addItem(18781, 1);
				player.getInventory().deleteItem(18780, 1);
				player.getSkills().addExperience(8, 750);
				player.sendMessage("Your Sated Effigy splits in the middle!");
			} else if(player.getSkills().getLevel(8) <= 95) {
				player.sendMessage("You need 95 Woodcutting to open this up.");
			}
			return false;
		case 6102:
			if(player.getSkills().getLevel(9) >= 95) {
				player.getInventory().addItem(18781, 1);
				player.getInventory().deleteItem(18780, 1);
				player.getSkills().addExperience(9, 750);
				player.sendMessage("Your Sated Effigy splits in the middle!");
			} else if(player.getSkills().getLevel(9) <= 95) {
				player.sendMessage("You need 95 Fletching to open this up.");
			}
			return false;
		case 7100:
			sendOptionDialogue(player, new int[]{7101, 7102}, "Herblore", "Fishing");
			return true;
		case 7101:
			if(player.getSkills().getLevel(15) >= 93) {
				player.getInventory().addItem(18780, 1);
				player.getInventory().deleteItem(18779, 1);
				player.getSkills().addExperience(15, 500);
				player.sendMessage("Your Nourished Effigy splits in the middle!");
			} else if(player.getSkills().getLevel(15) <= 93) {
				player.sendMessage("You need 93 Herblore to open this up.");
			}
			return false;
		case 7102:
			if(player.getSkills().getLevel(10) >= 93) {
				player.getInventory().addItem(18780, 1);
				player.getInventory().deleteItem(18779, 1);
				player.getSkills().addExperience(10, 500);
				player.sendMessage("Your Nourished Effigy splits in the middle!");
			} else if(player.getSkills().getLevel(10) <= 95) {
				player.sendMessage("You need 93 Fishing to open this up.");
			}
			return false;
		case 8100:
			sendOptionDialogue(player, new int[]{8101, 8102}, "Thieving", "Hunter");
			return true;
		case 8101:
			if(player.getSkills().getLevel(17) >= 90) {
				player.getInventory().addItem(18779, 1);
				player.getInventory().deleteItem(18778, 1);
				player.getSkills().addExperience(17, 250);
				player.sendMessage("Your Starved Effigy splits in the middle!");
			} else if(player.getSkills().getLevel(17) <= 90) {
				player.sendMessage("You need 90 Thieving to open this up.");
			}
			return false;
		case 8102:
			if(player.getSkills().getLevel(21) >= 90) {
				player.getInventory().addItem(18779, 1);
				player.getInventory().deleteItem(18778, 1);
				player.getSkills().addExperience(21, 250);
				player.sendMessage("Your Starved Effigy splits in the middle!");
			} else if(player.getSkills().getLevel(21) <= 90) {
				player.sendMessage("You need 90 Hunter to open this up.");
			}
			return false;
			
			//gambler:
		/*case 630:
			sendOptionDialogue(player, new int[]{631, 635}, "Enter lottery", "Dice duel");
			return true;
		case 631:
			sendDialogue(player, HAPPY_TALKING, -1, 632, "I would like to buy a ticket for the lottery.");
			return true;
		case 632:
			//if (player.getLottery().isInCurrentLottery())
				sendDialogue(player, CALM, 2998, -1, "You can only buy one lottery ticket at a time. Please wait till the lottery is over.");
			//else
				sendDialogue(player, HAPPY_TALKING, 2998, 633, "That will cost 5,000,000 coins. But with huge prizes as well.");
			return true;
		case 633:
			sendOptionDialogue(player, new int[]{634, -1}, "Enter and pay", "No thanks");
			return true;
		case 634:
			/*if player.getInv.contains(5m) {
			player.getInv.delete(5m);
			player.getLottery().setInCurrentLottery(true);
			sendDialogue(player, HAPPY_TALKING, 2998, -1, "Ticket number "+player.getLottery().getTicketNumber()+" I wish you lots of luck. Other players will be buying tickets.", "Once we've sold 20 tickets we draw the winning number!");
			} else
				sendDialogue(player, HAPPY_TALKING, 2998, -1, "You need 5 million coins to buy a ticket.");*/
			/*return true;
		case 635:
			//sendDialogue(player, CALM_TALK, 2998, 636, "So how much would you like to dice?");
			return true;
		case 636:
			//send enter-x chat interface here, than in ActionButtonHandler or somewhere make it continue the dialogue to case 636;
			InputHandler.requestIntegerInput(player, 8, "Please enter an amount:"); //?
			return true;
		case 637:
			//if (x <= 0) {
				//sendDialogue(player, CALM, 2998, 636, "Please enter a valid amount that you would like to dice.");
				//return true;
			//} else if (Lottery.getGamblersSavings < x) {
				//sendDialogue(player, SAD, 2998, 636, "I don't have enough money to pay you back if you would win that.", "Please dom't dice more than "+/*Lottery.getGamblersSavings+*///".");
				//return true;
			//} else if (x > 1000000000) {
				//sendDialogue(player, LAUGH_EXCITED, 2998, 636, "You can only dice a maximum of 1,000,000,000 million coins.");
				//return true;
			//}
			//sendDialogue(player, LAUGH_EXCITED, 2998, 638, "I wish you lots of luck!");
			//return true;
		/*case 638:
			/*
			 * Some Rune-Server shit:
			 * 		case 12002:
			World.getWorld().getNpcs().getById(2998).animate(Animation.create(11900, 0));
			World.getWorld().getNpcs().getById(2998).graphics(Graphic.create(2075, 0));
			sendDisplayBox(player, 12003, "Rolling...");
			player.animate(Animation.create(11900, 0));
			player.graphics(Graphic.create(2075, 0));
			return true;
		case 12003:
		Random dice = new Random();
		
		int roll = 0;
		int npcRoll = 0;
		
		for (int counter = 1; counter <= 1; counter++) {
			roll = 1 + dice.nextInt(100);
			npcRoll = 1 + dice.nextInt(100);

		if (npcRoll < roll) {
			sendDialogue(player, HAPPY_TALKING, -1, 12004, "I win! I rolled " + roll + " and you got " + npcRoll);
		} else if (npcRoll == roll) {
			sendDialogue(player, SAD, 2998, -1, "Dang. We tied with " + npcRoll + "to" + roll +".");
		} else if (npcRoll > roll) {
			sendDialogue(player, SAD, -1, 12005, "No! How did you get " + npcRoll + " and I got " + roll + "?!");
		}
	}*/
			/*return true;*/
			 
			
			
			//579 - 583 Skillcape masters dont use!
		}
		return false;
	}

	public static boolean handle(Player player, NPC npc) {
		
		int stage = player.getAttribute("dialougeStage", -1);
		if (stage == -1) {
			int id = npc.getId();
			player.getSettings().setSpeakingTo(npc);
			npc.setSpeakingTo(player);
			if(SkillCapes.handleFirstOption(player, id)){
				return true;
			}
			if (npc.isFamiliar()) { //note that the familiar messages are not like real RS
				if (player.getFamiliar() != null && player.getFamiliar().equals(npc.getFamiliar())) {
					if (npc.getFamiliar().isBeastOfBurden())
						sendOptionDialogue(player, new int[]{658, 659}, "Chat", "Store");
					else {
						if (npc.getCombatExecutor().getVictim() != null) {
							sendDialogue(player, WHAT_THE_CRAP, id, -1, "Not now, boss. I'm owning!");
							return true;
						}
						int random = Misc.random(3);
						if (random == 0)
							sendDialogue(player, REALLY_SAD, id, -1, "People say working Summoning is good for a server.", "I think it's bad, I like to be free.");
						else if (random == 1)
							sendDialogue(player, HAPPY_TALKING, id, -1, "Hey boss! What's our next adventure?");
						else if (random == 2)
							sendDialogue(player, WHAT_THE_CRAP, id, -1, "Please stop farting when I'm walking behind you, boss.");
						else if (random == 3) {
							String hatName = "";
							int hatId = -1;
							if (player.getEquipment().get(0) != null) {
								player.getEquipment().get(0).getDefinition().getName();
								hatId = player.getEquipment().get(0).getId();
							}
							if (hatName.contains("partyhat") || hatId == 1050 || hatName.contains("h'ween mask"))
								sendDialogue(player, HAPPY_TALKING, id, -1, "Nice rare on your head, boss!");
							else
								sendDialogue(player, HAPPY_TALKING, id, 660, "I love you, boss. When are we going to marry again?");
						}
					}
				} else
					player.sendMessage("This is not your familiar.");
				return true;
			}
			switch (id) {
			case 2998:
				sendDialogue(player, HAPPY_TALKING, 2998, 9050, "Hello, would you like to play a game of dice?");
				return true;
			case 11427:
			case 11428:
			case 11429:
			case 11430:
			case 11431:
			case 11432:
			case 11433:
			case 11434:
				sendDialogue(player, HAPPY_TALKING, id, -1, "Greetings, @PLAYER_NAME@, welcome to "+Constants.SERVER_NAME+"!");
				return true;
			case 9711:
				sendDialogue(player, CONFUSED, id, 9126, "How may I help you today?");
				return true;
			case 6970:
				sendDialogue(player, HAPPY_TALKING, 6970, 6999, "So you wan't to train summoning ehh?");
				return true;
			case 1513:
				sendDialogue(player, HAPPY_TALKING, 1513, 5790, "Hello @PLAYER_NAME@ I can take you to many skilling areas!", "what skill would you like to train?");
				return true;
			case 460:
				sendDialogue(player, HAPPY_TALKING, 460, 13000, "Why Hello, @PLAYER_NAME@, This is a PvP based server",  "would you like to pk?");
				return true;
			case 5512:
				sendDialogue(player, CALM_TALK, id, 5032, "What can I do for you @PLAYER_NAME@?");
				return true;
			case 2620:
				sendDialogue(player, HAPPY_TALKING, 2617, 1994, "Hello, @PLAYER_NAME@, Would you like to fight",  "the great monster called jad?");
				return true;
			case 8029:
				sendDialogue(player, HAPPY_TALKING, 8029, 5560, "There are many Monsters to fight on", "around DyNamic's which one would you like to fight?");
				return true;
			case 6135:
				sendDialogue(player, DialogueManager.HAPPY_TALKING, 6135, 9789, "Why hello @PLAYER_NAME@!", "Would you like me to take you to Living Rock caverns?");
				return true;
			case 9710:
				sendDialogue(player, DialogueManager.HAPPY_TALKING, 9710, 4254, "Let me open your bank for you.");
				return true;
			case 9712:
				sendDialogue(player, DialogueManager.HAPPY_TALKING, 9712, 5077, "Why hello there @PLAYER_NAME@!","I take it you need some help with dungeoneering?");
				return true;
			case 498: //bankers
			case 909:
			case 494:
			case 2619:
			case 3046:
			case 6200:
			case 0:
				sendDialogue(player, CALM_TALK, id, 616, "Good day. How may I help you?");
				return true;
			case 13455: //nex banker (add all bankers here that are not from Bank Of Varrock)
				sendDialogue(player, CALM_TALK, id, 630, "Good day. How may I help you?");
				return true;
			case 1702: //ghost banker
				if (player.getEquipment().get(2) != null && player.getEquipment().get(2).getId() == 4250)
					sendDialogue(player, CALM_TALK, id, 630, "Good day. How may I help you?");
				else
					sendDialogue(player, CALM_TALK, id, -1, "?uoy pleh I yam woH .yad dooG");
				return true;
			case 7605: //quick banker at home
				if (player.getCanUseQuickBankerAtHome())
					sendDialogue(player, HAPPY_TALKING, id, 630, "Good day. How may I help you?");
				else
					sendDialogue(player, BAD_ASS, id, 634, "Would you like acces to my wonderful and ", "well located bank for just 5,000,000 coins?");
				return true;
			case 11246: //ghost at barbarian stronghold
				if (player.getEquipment().get(3) != null && player.getEquipment().get(3).getId() == 10858
						&& !player.hasItem(7806))
					sendDialogue(player, SCARED, id, 734, "Ho. how.. how did you ge-get that s-sword, m-mortal?");
				else
					sendDialogue(player, CONFUSED, -1, 713, "Hey, what are you doing here?");
				return true;
			case 1699: //ghost shop
				if (player.getEquipment().get(2) != null && player.getEquipment().get(2).getId() == 4250)
					sendDialogue(player, HAPPY_TALKING, id, 720, "Well hello there! Are you interested in", "seeing my ghostly wares?");
				else
					sendDialogue(player, HAPPY_TALKING, id, -1, "?seraw yltsohg ym gniees", "ni detseretni uoy erA !ereht olleh lleW");
				return true;
			case 457: //restless ghost
				sendDialogue(player, HAPPY_TALKING, id, 722, "Hey traveller, welcome to our pyramid!");
				return true;
			case 456: //Father aereck.
				sendDialogue(player, THINKING, id, 319, "Greetings, @PLAYER_NAME@, how can I help you on this fine day?");
				return true;
			case 6971: //pikkupstix no. 2
				sendDialogue(player, WORRIED, id, 740, "Shh, the real Pikkupstix musn't know that I'm here.");
				return true;
			case 12189: //mime
				sendDialogue(player, HAPPY_TALKING, id, 767, "Would you like a free costume?");
				return true;
			case 2458: //freaky forester
				sendDialogue(player, HAPPY_TALKING, id, 770, "In a forest without a lederhosen outfit! Would", "you like a free lederhosen outfit?");
				return true;
			case 586:
				sendDialogue(player, TALKING_ALOT, id, 254, "Fancy a gander at me two handers?");
				return true;
			case 2024: //strange old man (barrows)
				sendDialogue(player, CONFUSED, id, 797, "Hello adventurer, could I help you with a spade?");
				return true;
			case 3033: //Garai
				sendDialogue(player, HAPPY_TALKING, id, -1, "It's always nice and sunny here!");
				return true;
			case 13180: //Rokuh
				sendDialogue(player, CALM_TALK, id, 764, "Most people think that I'm just an ordinary chocolate", "seller. But in fact I am a weapon specialist.", "Do you want so see my shop?");
				return true;
			case 1597: //Vannaka
				player.getSlayer().handleDialouge(2000);
				return true;
			case 970: // Diango
				sendDialogue(player, HAPPY_TALKING, 970, 205, "Hello @PLAYER_NAME@, care to have some dice?");
				return true;
			case 8863: // Sage, Roddeck (Tutorial guy)
				if (player.hasReceivedStarter())
					sendDialogue(player, HAPPY_TALKING, 8863, 5, "Hello @PLAYER_NAME@, what would you like?");
				else 
					sendOptionDialogue(player, new int[]{627, 628, 9943}, "Novice (Easy)", "Legend (Medium)", "Master (Very hard)");
				//sendDialouge(player, HAPPY_TALKING, id, 4, "Welcome to "+Constants.SERVER_NAME+" young warrior!", "Is there anything I can help you with?");
				return true;
			case 3029: //ghaslor the elder
				sendDialogue(player, HAPPY_TALKING, id, 749, "Welcome to our village. I am the oldest in this town.");
				return true;
			case 5029: //whip/dark bow colouring
				sendDialogue(player, HAPPY_TALKING, id, 687, "Haha! Someone actually found me here!");
				return true;
			case 6537: //Mandrith
			case 6539: //Nastroth
				sendDialogue(player, CONFUSED, id, 643, "Hello stranger, anything I can do for you?");
				return true;
			case 1526: //Lanthus
				sendDialogue(player, CONFUSED, id, 778, "Would you like to take a look at my", "castle wars reward shop?");
				return true;
			case 801: //abbot langley
			case 7727: //monk
				sendDialogue(player, CALM_TALK, id, 671, "Greetings traveller.");
				return true;
			case 3246: //Barbarians
			case 3247:
			case 3248:
			case 3249:
			case 3250:
			case 3251:
			case 3252:
			case 3253:
			case 3255:
			case 3256:
			case 3257:
			case 3258:
			case 3259:
			case 3260:
			case 3261:
			case 3262:
			case 3263:
				int random = Misc.random(3);
				if (random == 0)
					sendDialogue(player, EVIL, id, 677, "You don't belong here, outerlander.");
				else if (random == 1)
					sendDialogue(player, EVIL, id, 677, "You looking for a fight?");
				else if (random == 2)
					sendDialogue(player, EVIL, id, 677, "What?");
				else if (random == 3)
					sendDialogue(player, EVIL, id, 677, "What do you want, outerlander?");
				return true;
			case 3090: //barbarian champion
				sendDialogue(player, EVIL, id, 681, "I am Haakon, champion of this village. Do you seek to ", "challenge me?");
				return true;
			case 2877: //barbarian chief
				sendDialogue(player, EVIL, id, -1, "Begone, outerlander! Your kind are not welcome here!");
				return true;
			case 6742: //some barbarian snowman, u could use it for some x-mas event
				sendDialogue(player, HAPPY_TALKING, id, -1, "Haha, don't I look funny!");
				return true;
			case 1783: //Richard the team cape seller
				sendDialogue(player, HAPPY_TALKING, id, 662, "Hello there! Are you interested in buying one of my special ", "team capes?");
				return true;
			//case 2998: //Gambler
				//sendDialogue(player, HAPPY_TALKING, 2998, 630, "Would you like to gamble some money? You can enter in the server lottery or battle me in a dice duel!");
				//return true;
			case 3321: // Space guy
				/*
				 * The quest: "Double Intruders"
				 * 
				 * Talk to Shiratti the Custodian (prayer altar).
				 * She first doesn't trust you, then she tells that
				 * she suspects that someone is planning to kill the head of the town: Awusah the Mayor
				 * 
				 * You find out it is the dungeoneering smuggler. He tells that he is just there to 
				 * smuggle some stuff and that he needs to hide. If you agree he will say:
				 * "But I do think that there is some other strange stuff going around here."
				 * "I have heard strange noises on the roof above, you should check it out."
				 * "How do I get there?"
				 * "I don't know, there is no real way to get there. I think you need magics."
				 * -end conversation (if players talks again to smuggler: "Did you find out yet what's on the roof?"
				 * 
				 * Talk to Shiratti the Custodian to teleport you on the roof.
				 * -teleport anim for Shiratti
				 * 
				 * When you are on the roof:
				 */
				sendDialogue(player, WORRIED, 3321, -1, "Gnoif bum trii tok gnoo, stupied biep spjeessjiep.");
				player.sendMessage("Maybe you should get some help on translating.");
				/*
				 * Talk to the smuggler, he gives you a strange ammy that helps you translating what the space guy says.
				 * 
				 * If you wear the amulet:
				 * Space guy: "Oh noes! My spaceship crashed!"
				 * You: Option: "Can I help you?", "I've got no time for this"
				 * Space guy: "No it's not fixeable.. Totally broken."
				 * You: "How did this happen?"
				 * Space guy: "Well, I was on my planet pressing some buttons like a baws."
				 * "I am a guard on my planet, and there is nothing to guard so I'm bored every day.
				 * I like pressing buttons though, there are so many of them on my planet."
				 * "But that day I had some bad luck: after pressing a certain button some machine said:"
				 * "Explosion scheduled within 10 seconds. So I run as quickly as I could to my spacehip, it only took me 2 minutes of running to get there,
				 * and press some buttons like a baws to get outta there."
				 * "Like I said: I had bad luck that day, so I crashed here of course."
				 * You: option: "Wait, it took you 2 minutes of running?", "You're a weirdo, bye!"
				 * Space guy: "Yeah, like a baws!"
				 * You: "If there was an explosion scheduled within the next 10 seconds, and you ran for 2 minutes, how can you still live?"
				 * "I think you made a mistake and nothing exploded at all!"
				 * Space guy: "You think so? Ah, I had a feeling something was wrong."
				 * "But I can't go back anyways with a broken space ship."
				 * You: option: "You're welcome to stay in this world", "I can't hrlp you with that"
				 * Space guy: "How nice of you! Here, take this gift as a sign of our friendship!"
				 * Note: that gift should just be a cool looking item or something.
				 * 
				 * If you go to the smuggler and talk about what happened (OR YOU CAN GO
				 * TO Shiratti the Custodian RIGHT AWAY), he will say that you have to go to
				 * Shiratti the Custodian, to tell her everything is all right for the head of town (so no one 
				 * will start looking for intruders, like the smuggler).
				 *  
				 * Once you talk to Shiratti the Custodian:
				 * You: "Hey, it's just some spaceguy up there, nothing to worry about. The head of town is safe!"
				 * Shiratti the Custodian: "A space guy huh. Sounds fair to me."
				 * You: "It does?"
				 * Shiratti the Custodian: "Of course not silly, now get out of here, the head of town is safe."
				 * You: option: "Let's talk about rewards (1)", ""
				 * (1)"Can I get acces to other magic books as well? I mean this quest involved quite some magic
				 * with you teleporting me on some roof and that stuff."
				 *  //Shiratti the Custodian: "You can shut your mouth. These powers aren't just given at will, they have to be earned!"
				 * OR: "Haha, you'll never get those powers! Now get out of my face!" -> the 2nd quest is with Ghaslor the Elder, he will do something and be nice to you and you earn Shiratti the Custodian's respect, but he himself gives the powers first
				 * Shiratti the Custodian doesnt have 2 be in the 2nd quest, but once you talk to her after the 2nd quest she will have respect for you and your magics
				 * During the quest you battle a monster with the help of Ghaslor the Elder. He uses magic to aid you here and there (he is a very nice guy).
				 * 
				 * (1) or (2):
				 * !!
				 * INTERFACE ID = 277
				 * !!
				 * Well Done! You've completed Double Intruder(s)
				 * You have been awarded with:
				 * - Acces to Smuggler's Pk Points shop -> dungeoneering weapons?
				 * - Acces to new quest (some boss quest, or rs like) (questname here) (for curses and prayer books)
				 * 
				 * Note: there isn't really use to give an extra reward that has to do with the space guy
				 * I mean he is on some roof: you only see him during the quest.
				 *
				 * If you talk to the head of town somewhere during the quest, he will say something like:
				 * "???.. not made up yet"
				 * 
				 * Bug (FIXED ALREADY):
				 * If you tele from ground level to e.g. 3445 2916 2, you don't see the object (you have to relog), because the height is 2 (I think), other way around sometimes as well
				 */
				return true;
			//case 1513: // Mage
				//sendDialogue(player, HAPPY_TALKING, id, 13, "Hello there @PLAYER_NAME@, is there anything I can do for you?");
				//return true;
			case 8009: //this shop is deleted by the Dementhium team I guess
				sendDialogue(player, TOUGH, 8009, 31, "Hey, what do you want? I'm very busy.");
				return true;
			case 794:
				sendDialogue(player, LAUGH_EXCITED, id, -1, "I would talk matey!", "But I have to cook this meal for the boss!");
				return true;
			case 4285:
				sendDialogue(player, HAPPY_TALKING, id, 256, "Ghommal welcome you to Warriors Guild!");
				return true;
			case 4286:
				sendDialogue(player, HAPPY_TALKING, id, 257, "Welcome to my humble guild, @PLAYER_NAME@.");
				return true;
			case 3322:
				sendDialogue(player, HAPPY_TALKING, id, 561, "Hello @PLAYER_NAME@, would you like to see what i've","been cooking?");
				return true;
			case 3167:
			case 3168:
			case 3169:
			case 3170:
			case 3171:
			case 3172:
			case 3173:
				int randomDialogue = Misc.random(2);
				switch (randomDialogue) {
				case 0:
					sendDialogue(player, TOUGH, id, -1, "Hey, what are you doing here scallywag!", "Get out now!");
					break;
				case 1:
					sendDialogue(player, TOUGH, id, -1, "Argh matey, I be busy!", "Now leave me alone!");
					break;
				case 2:
					sendDialogue(player, TOUGH, id, -1, "Are ye looking to get stuck?", "I thought not! Get out of my face!");
					break;
				}
				return true;
			case 3180:
			case 3181:
			case 3182:
			case 3183:
			case 3184:
			case 3185:
				int randomDi = Misc.random(2);
				switch (randomDi) {
				case 0:
					sendDialogue(player, DRUNK_HAPPY_TIRED, id, -1, "Aren't these *hic* drinks just", "the best! Ahaha!");
					break;
				case 1:
					sendDialogue(player, DRUNK_HAPPY_TIRED, id, -1, "You better watch out *hic* I, I, I", "think im gonna blow!");
					break;
				case 2:
					sendDialogue(player, DRUNK_HAPPY_TIRED, id, -1, "*hic* Boy am I having a *hic* great time.", "Buy a beer and *hic* have one too!");
					break;
				}
				return true;
			case 731:
				sendDialogue(player, DEPRESSED, id, -1, "Oh my... These drunken pirates just about ran me out", "of beer. Sorry about this.");
				return true;
			case 905:
				sendDialogue(player, HAPPY_TALKING, id, 70, "Hello warrior!", "Would you like to take a trip to the Mage Bank?");
				return true;
			case 308:
				if (player.getSkills().getLevelForExperience(Skills.FISHING) >= 99) {
					sendDialogue(player, HAPPY_TALKING, id, 210, "Oh my!", "It's an honor to be in the presence of another", "Master Fisher! Would you like a cape?");
				} else {
					sendDialogue(player, MEAN_FACE, id, -1, "I have no time for someone that is not", "as advanced as me! Get out!");
				}
				return true;
			case 4906:
				if (player.getSkills().getLevelForExperience(Skills.WOODCUTTING) >= 99) {
					sendDialogue(player, HAPPY_TALKING, id, 212, "Oh my!", "Good to see another Master Woodcutter", "around here! Would you like a cape?");
				} else {
					sendDialogue(player, SAD, id, -1, "I'm sorry, I'm a little busy", "at the moment. ");
				}
				return true;
			case 455:
				if (player.getSkills().getLevelForExperience(Skills.HERBLORE) >= 99) {
					sendDialogue(player, HAPPY_TALKING, id, 222, "Woah! I thought I was the only Master of Herblore!", "Would you fancy a cape like mine?");
				} else {
					sendDialogue(player, TALKING_ALOT, id, 224, "Hello, What do you need?");
				}
				return true;
			case 575:
				if (player.getSkills().getLevelForExperience(Skills.FLETCHING) >= 99) {
					sendDialogue(player, HAPPY_TALKING, id, 214, "Oh my!", "You don't need any more work on Fletching!", "Would you like a cape?");
				} else {
					sendDialogue(player, TALKING_ALOT, id, 216, "Hm, looks like you could work on fletching", "Would you like to look at my shop?");
				}
				return true;
			case 2270:
				if (player.getSkills().getLevelForExperience(Skills.THIEVING) >= 99) {
					sendDialogue(player, SECRELTY_TALKING, id, 218, "Hey, hey you. I uhh, have some capes.", "Would you like one?");
				} else {
					sendDialogue(player, SECRELTY_TALKING, id, 220, "Hey over here, you wanna take", "a look at some legal material?");
				}
				return true;
			case 847:
				if (player.getSkills().getLevelForExperience(Skills.COOKING) >= 99) {
					sendDialogue(player, LAUGH_EXCITED, id, 231, "Mama mia! I've never seen someone as ", "advanced as me in Cooking. Would you like a cape?");
				} else {
					sendDialogue(player, MEAN_FACE, id, -1, "Sorry, but I only talk to the chefs of the ", "highest caliber! Be gone!");
				}
				return true;
			case 4287:
				sendDialogue(player, TALKING_ALOT, id, 420, "Ello there. I'm Gamfred, the engineer here in this guild.", "Have you seen my catapult?");
				return true;
			case 4651: //shops have been removed by Dementhium team I think:
				sendDialogue(player, HAPPY_TALKING, id, 235, "Hello young chap, How can I help you?");
				return true;
			case 4652: //244
				sendDialogue(player, HAPPY_TALKING, id, 241, "Hello young chap, How can I help you?");
				return true;
			case 4653:
				sendDialogue(player, HAPPY_TALKING, id, 244, "Hello young chap, How can I help you?");
				return true;
			case 4654:
				sendDialogue(player, HAPPY_TALKING, id, 247, "Hello young chap, How can I help you?");
				return true;
			case 520:
			case 521:
			case 534:
			case 535:
			case 3039:
				sendDialogue(player, HAPPY_TALKING, id, 105, "Hello, How can I help you?");
				return true;
			case 550:
				sendDialogue(player, HAPPY_TALKING, id, 97, "Hello, are you interested in any range supplies?");
				return true;
			//case 587:
			case 3030: //ali the carter
				sendDialogue(player, HAPPY_TALKING, id, 45, "Well hello there! Are you interested in", "seeing my wares?");
				return true;
			case 37:
				sendDialogue(player, DialogueManager.DEPRESSED, 37, 117, "What do you need?");
				return true;
			case 705: //Melee instructor.
				sendOptionDialogue(player, new int[]{124, 125, 126}, "Who are you?", "What is this place?", "What is that cape you're wearing?");
				return true;
			case 4707: //Magic instructor.
				sendOptionDialogue(player, new int[]{141, 142}, "Who are you?", "What is this place?");
				return true;
			case 1861: //Range instructor.
				sendOptionDialogue(player, new int[]{152, 153}, "Who are you?", "What is this place?");
				return true;
			case 8449:
				sendDialogue(player, CONFUSED, id, 200, "What do you want?");
				return true;
			case 519://Bob
				/*if (player.getRights() == 2) {*/
					sendDialogue(player, HAPPY_TALKING, id, 404, "Hello, how may I help you?");
					RepairItem.resetRepair();
				/*} else {
					sendDialogue(player, SNOBBY, id, -1, "There is nothing to see here yet.");
				}*/
				return true;
			case 5113://Hunting expert
				sendDialogue(player, HAPPY_TALKING, id, 412, "I am the Hunting Expert, what can I assist you with?");
				return true;
			default:
				sendDialogue(player, HAPPY_TALKING, id, -1, "Greetings, @PLAYER_NAME@, welcome to "+Constants.SERVER_NAME+"!");
				return true;
			}
			//player.getSettings().setSpeakingTo(null);
		} else {
			resetDialouge(player);
			return handle(player, npc);
		}
		//return false;
	}

	public static void processNextDialogue(Player player, int button) {
		int stage;
		Object attribute = player.getAttribute("nextDialougeStage", -1);
		if (attribute instanceof int[]) {
			stage = ((int[]) attribute)[button];
		} else {
			stage = (Integer) attribute;
			if (stage == -1) {
				resetDialouge(player);
				return;
			}
		}
		if (!proceedDialogue(player, stage)) {
			resetDialouge(player);
		} /*else {
			if (player.getSettings().getSpeakingTo() != null && player.getSettings().getSpeakingTo().isNPC()) { //could also be object
				NPC npc = (NPC) player.getSettings().getSpeakingTo();
				//if (npc.isDoesWalk()) {
					npc.turnTo(player, true);
				//}
			}
		}*/
	}

	public static void sendDialogue(Player player, int anim, int face, int nextStage, String... dialogue) {
		if (dialogue.length == 0 || dialogue.length > 4) {
			return;
		}
		int interfaceId = (face == -1 ? 63 : 240) + dialogue.length;
		int index = 4;
		ActionSender.sendString(player, interfaceId, 3, face == -1 ? player.getDisplayName() : CacheNPCDefinition.forID(face).getName());
		for (String s : dialogue) {
			ActionSender.sendString(player, interfaceId, index, s.replaceAll("@PLAYER_NAME@", player.getDisplayName()));
			index++;
		}
		ActionSender.sendChatboxInterface(player, interfaceId);
		ActionSender.sendEntityOnInterface(player, face == -1, face, interfaceId, 2);
		ActionSender.sendInterAnimation(player, anim, interfaceId, 2);
		player.setAttribute("nextDialougeStage", nextStage);
	}
	


	public static void sendOptionDialogue(Player player, int[] nextStages, String... dialouge) {
		if (dialouge.length < 2 || dialouge.length > 5 || nextStages.length != dialouge.length) { //cant have 1 option
			return;
		}
		int interfaceId = 224 + (dialouge.length * 2);
		int index = 2;
		for (String string : dialouge) {
			ActionSender.sendString(player, string, interfaceId, index);
			index++;
		}
		ActionSender.sendChatboxInterface(player, interfaceId);
		player.setAttribute("nextDialougeStage", nextStages);
	}

	public static void sendDisplayBox(Player player, int nextStage, String... display) {
		if (display.length < 1 || display.length > 5) {
			return;
		}
		int interfaceId = 209 + display.length;
		int index = 1;
		for (String string : display) {
			ActionSender.sendString(player, string, interfaceId, index);
			index++;
		}
		ActionSender.sendChatboxInterface(player, interfaceId);
		player.setAttribute("nextDialougeStage", nextStage);
	}
	
	/**
	 * Used for Christmas Cracker, which needs a long title (for example)
	 * 
	 * Make sure to send the title of this yourself, using:
	 * ActionSender.sendString(player, title, interfaceId, lineId);
	 * interfaceId is often 718 or 554, title is often 0 or 1 (subtitle).
	 */
	public static void send2OptionDialogueWithLongTitle(Player player, int[] nextStages, String... dialouge) {
		if (dialouge.length != 2 || nextStages.length != dialouge.length) { //cant have 1 option
			return;
		}
		int interfaceId = 718;
		int index = 1;
		for (String string : dialouge) {
			ActionSender.sendString(player, string, interfaceId, index);
			index++;
		}
		ActionSender.sendChatboxInterface(player, interfaceId);
		player.setAttribute("nextDialougeStage", nextStages);
	}

	public static void resetDialouge(Player player) {
		ActionSender.sendCloseChatBox(player);
		player.removeAttribute("nextDialougeStage");
		player.removeAttribute("dialougeStage");
		player.removeAttribute("teleportDestination");
		if (player.getSettings().getSpeakingTo() != null && player.getSettings().getSpeakingTo().isNPC() 
				&& player.getSettings().getSpeakingTo().getNPC().getSpeakingTo() != null
				&& player.getSettings().getSpeakingTo().getNPC().getSpeakingTo().equals(player))
			player.getSettings().getSpeakingTo().getNPC().setSpeakingTo(null);
		player.getSettings().setSpeakingTo(null);
	}

	/*
	 * 9755 or lower = nothing just more glitchy
 9760 - downer
 9765 - real sad
 9770 - depressed
 9775 - worried
 9780 - scared from something on teh ground
 9785 - umm mean face
 9790 - evil headbang
 9795 - evil plan
 9800 - what the crap
 9805 - calm
 9810 - calm talk
 9815 - all tough
 9820 - snobby
 9825 - moving head snobby
 9830 - confuse
 9835 - drunk happy tired
 9840 - real happy
 9845 - talking mouth off
 9850 - happy talking mouth off
 9855 - something stuck in his mouth
 9860 - something stuck in his mouth eyes move
 9865 - something stuck in his mouth, chin up high
 9870 or higher = idk but looks funny
	 */
	
	public static boolean  Regular = false;
	public static boolean Legend = false;

	public static final int REALLY_SAD = 9760, SAD = 9765, DEPRESSED = 9770, WORRIED = 9775, SCARED = 9780, MEAN_FACE = 9785,
			MEAN_HEAD_BANG = 9790, EVIL = 9795, WHAT_THE_CRAP = 9800, CALM = 9805, CALM_TALK = 9810, TOUGH = 9815, SNOBBY = 9820,
			SNOBBY_HEAD_MOVE = 9825, CONFUSED = 9830, DRUNK_HAPPY_TIRED = 9835, TALKING_ALOT = 9845, HAPPY_TALKING = 9850, BAD_ASS = 9855,
			THINKING = 9860, COOL_YES = 9864, LAUGH_EXCITED = 9851, SECRELTY_TALKING = 9838;

	public static void sendInfoDialogue(Player player, String line_1, String line_2) {
		ActionSender.sendChatboxInterface(player, 211);
		ActionSender.sendString(player, 211, 1, line_1);
		ActionSender.sendString(player, 211, 2, line_2);
	}
	
    public static final int[] CRACKERREWARDS = {
        1079, 1093, 1127, 1201, 1163, 1333, 1319, 1303, 
        995, 554, 555, 556, 557, 558, 559, 561, 562, 563
    };

}
