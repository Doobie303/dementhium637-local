package org.dementhium.content.skills.slayer;

import java.text.NumberFormat;

import org.dementhium.content.skills.slayer.SlayerTask.Master;
import org.dementhium.model.World;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.player.Player;
import org.dementhium.model.player.Skills;
import org.dementhium.util.Constants;

import static org.dementhium.content.DialogueManager.*;

/**
 * @author 'Mystic Flow <Steven@rune-server.org>
 */
public class Slayer {

    //Great, you're doing great. Your new task is to kill
    //60 ghouls.

    /**
     * Tips
     * Ghouls - Ghouls aren't undead, but they are stronger
     * and tougher then they look. They're also very cowardly and
     * will run if they're losing a fight. They wait to ambush
     * those entering Morytania, close to the River Salve.
     */

    private Player player;
    private SlayerTask task;

    public Slayer(Player player) {
        this.player = player;
    }


    public boolean handleDialouge(int stage) {
        if (!(stage >= 2000 && stage <= 3000)) {
            return false;
        }
        NPC talkingTo = player.getSettings().getSpeakingTo() == null ? null : player.getSettings().getSpeakingTo().getNPC();
        Master master = player.getSettings().getSpeakingTo() == null ? null : Master.forId(talkingTo.getId());
        NumberFormat nf1 = NumberFormat.getInstance();
        String slayerRewardXp = nf1.format((int) 1000*player.getSkills().getXpModifierForSkill(Skills.SLAYER));
        switch (stage) {
            case 2000:
                if (master == Master.VANNAKA) {
                    sendDialogue(player, CALM_TALK, talkingTo.getId(), 2001, "'Ello and what are you after then?");
                }
                return true;
            case 2001:
                sendOptionDialogue(player, new int[]{2002, 2003, 2004, 2005, 2006}, "I need another assignment.", "Do you have anything for trade?", "Slayer Areas..", "I am here to discuss any reward I might be eligble for.", "Er...nothing...");
                return true;
            case 2002:
                sendDialogue(player, CALM_TALK, -1, 2012, "I need another assignment.");
                return true;
            case 2003:
                sendDialogue(player, HAPPY_TALKING, -1, 2007, "Do you have anything for trade?");
                return true;
            case 2004:
                sendOptionDialogue(player, new int[]{2009, 2010}, "Where can I find monsters to slay?", "Sorry I was just leaving.");
                return true;
            case 2005:
                sendDialogue(player, HAPPY_TALKING, -1, 2063, "I am here to discuss any reward I might be eligible for.");
                return true; 
            case 2006:
                sendDialogue(player, CALM_TALK, -1, -1, "Er...nothing...");
                return true;
            case 2007:
                sendDialogue(player, HAPPY_TALKING, talkingTo.getId(), 2008, "I have a wide selection of Slayer equipment; take a look!");
                return true;
            case 2008:
            	World.getWorld().getShopManager().openShop(player, 1595);
                return false;
            case 2009:
                sendDialogue(player, CALM_TALK, -1, 2011, "Where can I find monsters to slay?");
                return true;
            case 2010:
                sendDialogue(player, HAPPY_TALKING, -1, -1, "Sorry I was just leaving.");
                return true;
            case 2011:
                sendDialogue(player, CALM_TALK, talkingTo.getId(), 2505, "Ahh, what a great question I can take you many places", "let me show you some of them!");
                return true;
            case 2505:
				sendOptionDialogue(player, new int[]{2222, 2223, 2224, 2225, -1}, "Kuradel's Dungeon", "Slayer Tower", "Fremennik Dungeon", "Mithril Dragons", "Nothing");
				return true;
			case 2222:
				player.teleport(1661, 5258, 0, false);
				return false;
			case 2223:
				player.teleport(3429, 3534, 0, false);
				return false;
			case 2224:
				player.teleport(2808, 10002, 0, false);
				return false;
			case 2225:
				player.teleport(1765, 5333, 0, false);
				return false;
            case 2012:
                if (task != null) {
                    sendDialogue(player, CONFUSED, talkingTo.getId(), 2060, "You're still hunting " + task.getName() + "s, would you like", "a new task?"/*"; come back when you've", "finished your task."*/);
                } else {
                    double slayerExperience = player.getSkills().getXp(Skills.SLAYER);
                    SlayerTask newTask = null;
                    if (slayerExperience == 0) {
                        newTask = new SlayerTask(master, 0, 30);
                        sendDialogue(player, CALM_TALK, talkingTo.getId(), -1, "For your first task I'm assigning you to", "kill 30 bats.");
                    } else {
                        newTask = SlayerTask.random(player, master);
                        sendDialogue(player, CALM_TALK, talkingTo.getId(), -1, "Great, you're doing great. Your new task is to kill", newTask.getTotalTaskAmount() + " " + newTask.getName() + "s.");
                    }
                    this.task = newTask;
                }
                return true;
            case 2013:
                sendDialogue(player, CONFUSED, Master.VANNAKA.getId(), 2014, "Hello there, @PLAYER_NAME@, what can I help you with?");
                return true;
            case 2014:
                sendOptionDialogue(player, new int[]{2015, 2016, 2017, 2018, 2019}, "How am I doing so far?", "Who are you?", "Where are you?", "Got any tips for me?", "Nothing really.");
                return true;
            case 2015:
                sendDialogue(player, CALM_TALK, -1, 2020, "How am I doing so far?");
                return true;
            case 2016:
                sendDialogue(player, CALM_TALK, -1, 2021, "Who are you?");
                return true;
            case 2017:
                sendDialogue(player, CALM_TALK, -1, 2022, "Where are you?");
                return true;
            case 2018:
                sendDialogue(player, CALM_TALK, -1, 2023, "Got any tips for me?");
                return true;
            case 2019:
                sendDialogue(player, CALM_TALK, -1, -1, "Nothing really.");
                return true;
            case 2020:
                if (task != null) {
                    sendDialogue(player, CALM_TALK, Master.VANNAKA.getId(), 2040, "You're currently assigned to kill " + task.getName().toLowerCase() + "s; only " + task.getCurrentTaskAmount() + " more", "to go.");
                } else {
                    sendDialogue(player, CALM_TALK, Master.VANNAKA.getId(), 2040, "You currently have no task, come to me so ", "I can assign you one.");
                }
                return true;
            case 2021:
                //TODO Support for different masters.
                sendDialogue(player, CALM_TALK, Master.VANNAKA.getId(), 2041, "My name's Vannaka; I'm a Slayer Master.");
                return true;
            case 2022:
                sendDialogue(player, CALM_TALK, Master.VANNAKA.getId(), 2042, "You'll find me in the default city used for "+Constants.SERVER_NAME+".", "I'll be here when you need a new task.");
                return true;
            case 2023:
                sendDialogue(player, CALM_TALK, Master.VANNAKA.getId(), 2043, "At the moment, no.");
                return true;
            case 2040:
                sendOptionDialogue(player, new int[]{2016, 2017, 2018, 2019}, "Who are you?", "Where are you?", "Got any tips for me?", "Nothing really.");
                return true;
            case 2041:
                sendOptionDialogue(player, new int[]{2015, 2017, 2018, 2019}, "How am I doing so far?", "Where are you?", "Got any tips for me?", "Nothing really.");
                return true;
            case 2042:
                sendOptionDialogue(player, new int[]{2015, 2016, 2018, 2019}, "How am I doing so far?", "Who are you?", "Got any tips for me?", "Nothing really.");
                return true;
            case 2043:
                sendOptionDialogue(player, new int[]{2015, 2016, 2017, 2019}, "How am I doing so far?", "Who are you?", "Where are you?", "Nothing really.");
                return true;
                
            case 2060:
            	sendOptionDialogue(player, new int[]{2061, 2062}, "Yes, please!", "No thank you, I'm fine.");
            	return true;
            case 2061:
                this.task = null;
                sendDialogue(player, HAPPY_TALKING, -1, 2012, "Yes, please!");
                return true;
            case 2062:
            	sendDialogue(player, CALM_TALK, -1, -1, "No thank you, I'm fine.");
            	return true;
            case 2063:
            	if (player.getSlayerPoints() < 400)
            		sendDialogue(player, CALM_TALK, talkingTo.getId(), -1, "You are not eligible for any reward. You need", "to have at least (400 slayer points)");
            	else
            		sendDialogue(player, CALM_TALK, talkingTo.getId(), 2064, "You can choose between a fancy slayer helmet or", slayerRewardXp+" slayer experience, both costing 400 slayer points.", "You currently have "+player.getSlayerPoints()+" slayer points.");
            	return true;
            case 2064:
            	sendOptionDialogue(player, new int[]{2065, 2066, 2062}, "Slayer helmet (400 slayer points)", slayerRewardXp+" experience (400 slayer points)", "No thank you, I'm fine.");
            	return true;
            case 2065:
            	if (player.getInventory().getFreeSlots() < 1) {
                	sendDialogue(player, CALM_TALK, talkingTo.getId(), -1, "You need at least one free inventory space.");
            	} else {
                	//player.addSlayerPoints(-400);
                	//player.getInventory().addItem(13263, 1);
                	//player.getInventory().refresh();
                	sendDialogue(player, CALM_TALK, talkingTo.getId(), -1, "Sorry this feature is still under construction.");
            	}
            	return true;
            case 2066:
            	//player.addSlayerPoints(-400);
            	//player.getSkills().addExperience(Skills.SLAYER, 10000);
            	sendDialogue(player, CALM_TALK, talkingTo.getId(), -1, "Sorry this feature is still under construction.");
            	return true;
            
        }
        return true;
    }


    public SlayerTask getSlayerTask() {
        return task;
    }

    public void killedTask() {
        player.getSkills().addExperience(Skills.SLAYER, task.getXPAmount());
        task.decreaseAmount();
        if (task.getCurrentTaskAmount() < 1) {
        	int slayerPoints = (int) Math.ceil(task.getXPAmount() * task.getTotalTaskAmount() * 0.01);
        	if (slayerPoints < 2)
        		slayerPoints = 2;
            player.addSlayerPoints(slayerPoints);
            player.sendMessage("You've completed your task and gain "+slayerPoints+" points. Return to a Slayer Master.");
            //player.sendMessage("You've completed ... tasks in a row and gain .. points. Return to a Slayer Master.");
            task = null;
        }
    }


    public void setSlayerTask(SlayerTask slayerTask) {
        this.task = slayerTask;
    }

}
