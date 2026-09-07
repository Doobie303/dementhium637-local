package org.dementhium.content;

import org.dementhium.model.player.Player;
import org.dementhium.net.ActionSender;
import org.dementhium.util.Constants;

/**
 * @author Lumby <lumbyjr@hotmail.com>
 */
public class BookManager {

    public static boolean proceedBook(Player player, int stage) {
        /*
           * Start and end all books from here! First value is the previous button
           * stage Second value is next button stage First Boolean is enable back
           * button Second Boolean is enable next button
           */
        switch (stage) {
            case 1:
                sendBook(player, -1, 2, false, true, Constants.SERVER_NAME+" Rule Book",
                        "<shad=6081134>"+Constants.SERVER_NAME+" Rules</shad>", "",
                        "In this book you will find the",
                        "rules that need to be followed",
                        "when playing the server.", "", "FAILURE TO FOLLOW",
                        "THESE RULES COULD", "RESULT IN YOUR", "ACCOUNT BEING",
                        "BANNED OR RESET.", "~RULE 1:~", "Offensive Language",
                        "You are in no way allowed", "to use offensive language",
                        "towards other players ingame.", "", "~RULE 2:~",
                        "Hacking/Duping", "Anyone partaking in a dupe",
                        "or hack risks getting banned.", "");
                return true;
            case 2:
                sendBook(player, 1, 3, false, false, Constants.SERVER_NAME+" Rule Book",
                        "~RULE 3 - Spamming~:", "Autotypers must be set with",
                        "an interval of 10 seconds.", "Anyone using them more",
                        "frequently risks recieving a", "temporary mute.", "",
                        "~RULE 4 - Botting/Macro:~", "Anyone using a bot to gain",
                        "an unfair advantage will be", "considered intelligent.",
                        "~RULE 5 - Bug Abuse:~", "If you find a bug please",
                        "report it on the forums.", "Anyone who is abusing a bug",
                        "could get banned.", "", "~RULE 6 - Scamming:~",
                        "You may not trick or scam", "a player in any way. You",
                        "could recieve a ban (luring ", "is mean but allowed).");
                return true;
            case 3:
                sendBook(player, 2, -1, true, false, Constants.SERVER_NAME+" Rule Book",
                        "~RULE 7 Disrespecting Staff:~",
                        "You may not disrespect any", "staff member in any way.",
                        "You have to treat the staff", "as you would like to be ", "treated yourself.",
                        "", "~RULE 8 Aggravating Users:~",
                        "You may not aggravate a", "user and pick on them to",
                        "make them mad. This is", "immature and could result",
                        "in a mute.", "", "~RULE 8 - Trolling:~",
                        "Anyone who is trolling", "risks recieving a mute", 
                        "if he's not funny enough.", "",
                        "Thank you for reading and", "we hope you enjoy playing",
                        Constants.SERVER_NAME+" safely.");
                return false;
            case 4:
            	sendBook(player, 2, -1, true, true, Constants.SERVER_NAME+" Guide Book",
                        "",
                        "Fairy Ring Codes", "------------------------",
                        "AIQ - Mudskipper Point", "BJR - Fisher Realm",
                        "DKR - Edgeville", "AKS - Feldip Hunter area",
                        "CIS - Dungeon Training Area", "ALS - McGrubor's Wood",
                        "DIP - Slayer Tower",
                        "BIP - Skeletal Horror cave",
                        "DJS - Zanaris",
                        "AJR - Fremennik Slayer Dungeon",
                        "CIP - Taverly Dungeon",
                        "BLP - Tzhaars"
                        );
            	return false;
            case 5:
                sendBook(player, -1, 6, false, true, Constants.SERVER_NAME+" Advice",
                        "<shad=6081134>"+Constants.SERVER_NAME+" Advice</shad>", "",
                        "In this book you can find",
                        "some advice. To type ", "commands start your line ", 
                        "with :: For a list of ", "commands type ::commandlist",
                        "", "~TRAINING:~", "A good place to train",
                        "your combat levels is ", "the Stronghold of Security.", "",
                        "You can get there by ", "going to Edgeville and",
                        "from there walking south", "till you reach Barbarian",
                        "Village. Once there you ", "can climb down the entrance",
                        "located in the middle of", "the village.");
                return false;
            case 6:
                sendBook(player, 5, -1, true, false, Constants.SERVER_NAME+" Advice",
                        "In the Stronghold you", "can fight monsters and",
                        "go through the portals to", "advance to the next floors",
                        "and fight even stronger", "monsters. There is also a",
                        "reward for great warriors", "who reach the highest floor.");
                return false;
                
                
            case 20:
                sendBook(player, -1, 21, false, true, "Book of knowledge",
                        "<shad=6081134>Book of knowledge</shad>", "",
                        "Dear reader, I hope this", "Book serves you as well as", 
                        "it has served me.", "",
                        "Gunnarsgrunn:", "1- On the top of the ",
                        "village, poison can be found", "on two boxes.",
                        "2- Deep under the grounds", "of this village a ghost",
                        "stands still between the ", "other moving ghosts.",
                        "He holds hidden a secret ", "sword.",
                        "3- In an outpost far", "outside the town, a",
                        "commander dwells. He can", "paint your items.");
            	return false;
            case 21:
                sendBook(player, 20, -1, true, false, "Book of knowledge",
                        "<shad=6081134>Book of knowledge</shad>", "",
                        "Taverly:", "Pikkupstix has an intruder",
                        "in his house. There are some", "cool items to get there,",
                        "go visit it sometime.", "",
                        "General:", "You must have noticed the", 
                        "general stores around", Constants.SERVER_NAME+", but did you", 
                        "ever go upstairs?", "");
            	return false;
            	
            case 40:
                sendBook(player, -1, 41, false, true, "Book o' piracy",
                        "<shad=6081134>Book o' piracy</shad>", "",
                        "An egg with poison, oh so", "small, the book of ",
                        "knowledge knows it all!", "This egg is a might weapon.",
                        "A weapon to lure!", "Let's stop the riddles. Once",
                        "You possess this egg, ", "find your victim and make",
                        "sure that their inventory", "is completely full.",
                        "Now let them price check a", "valuable item. Once their",
                        "item is in the price checker,", "you use the egg on",
                        "them. Their valuable item", "will be dropped!",
                        "", "Items deliberately dropped",
                        "in the wilderness appear", "immediately if tradeable.");
            	return false;
            case 41:
                sendBook(player, 40, -1, true, false, "Book o' piracy",
                		"Let your victim have ", "some explosive potions",
                        "in their inventory,", "together with some junk",
                        "that they don't need.", "Tell them to do",
                        "::dropall. The explosive", "potions will do their",
                        "work. Make sure that", "your victim is in the", 
                        "wilderness. Oh, and make", "sure that they are",
                        "WEARING something", "expensive. Most of them",
                        "will be clever enough", "to not have valuables",
                        "in their inventory when", "doing a command such ", 
                        "as ::dropall.");
            	return false;
            case 42:
            	 sendBook(player, -1, 43, true, false, "Dicer rules & Regulations",
                 		"When dicing right click the dice", "and use clan roll for all to see.",
                         "If you scam ANYONE", "your dicing privileges",
                         " will be taken away immediately.", "ALWAYS make sure to",
                         "clarify the game type", "before rolling,",
                         "For instance 55x2 or Dice duel.", " Don’t take bets", 
                         "you can’t payout.", "When receiving a bet",
                         "only roll once DO NOT", "roll multiple times.",
                         "Don’t try to force", " players into betting you.",
                         "Always say pots before rolling.", " Always payout bets if you lose.", 
                         "Try to stick to 55x2.", " Written by - I Duh");
             	return false;
            default:
                return false;
        }
    }

    public static void processNextPage(Player player) {
        int stage;
        Object attribute = player.getAttribute("nextBookStage");
        stage = (Integer) attribute;
        if (stage == -1) {
            resetBook(player, false);
            return;
        }
        if (!proceedBook(player, stage)) {
            resetBook(player, false);
        }
    }

    public static void processPreviousPage(Player player) {
        int stage;
        Object attribute = player.getAttribute("previousBookStage");
        stage = (Integer) attribute;
        if (stage == -1) {
            resetBook(player, true);
            return;
        }
        if (!proceedBook(player, stage)) {
            resetBook(player, true);
        }
    }

    public static void sendBook(Player player, int previousBookStage,
                                int nextBookStage, boolean hideNext, boolean hideBack,
                                String title, String... content) {
        if (content.length == 0 || content.length > 22) {
            return;
        }
        ActionSender.sendString(player, 959, 5, title);
        int index = 30;
        for (String s : content) {
            ActionSender.sendString(player, 959, index, s);
            ActionSender.sendInterfaceConfig(player, 959, index, true);
            index++;
        }
        if (content.length < 22) {
            int blankContentNumber = 22 - content.length;
            int blankIndex = 30 + content.length;
            for (int i = 0; i < blankContentNumber; i++) {
                ActionSender.sendInterfaceConfig(player, 959, blankIndex + i,
                        false);
            }
        }
        ActionSender.sendString(player, 959, 53, "Next Page");
        ActionSender.sendString(player, 959, 52, "Previous Page");
        if (hideNext) {
            ActionSender.sendInterfaceConfig(player, 959, 29, false);
            ActionSender.sendInterfaceConfig(player, 959, 53, false);
        } else {
            ActionSender.sendInterfaceConfig(player, 959, 29, true);
            ActionSender.sendInterfaceConfig(player, 959, 53, true);
        }
        if (hideBack) {
            ActionSender.sendInterfaceConfig(player, 959, 28, false);
            ActionSender.sendInterfaceConfig(player, 959, 52, false);
        } else {
            ActionSender.sendInterfaceConfig(player, 959, 28, true);
            ActionSender.sendInterfaceConfig(player, 959, 52, true);
        }
        ActionSender.sendInterface(player, 959);
        player.removeAttribute("nextBookStage");
        player.removeAttribute("previousBookStage");
        player.setAttribute("nextBookStage", nextBookStage);
        player.setAttribute("previousBookStage", previousBookStage);
    }

    public static void resetBook(Player player, boolean resetPrevious) {
        if (!resetPrevious)
            player.removeAttribute("nextBookStage");
        else
            player.removeAttribute("previousBookStage");
    }
}
