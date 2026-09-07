package org.dementhium.content.misc.treasuretrails;

import org.dementhium.content.activity.impl.DuelActivity;
import org.dementhium.content.activity.impl.DuelActivity.State;
import org.dementhium.model.Container;
import org.dementhium.model.Item;
import org.dementhium.model.definition.ItemDefinition;
import org.dementhium.model.player.Player;
import org.dementhium.net.ActionSender;
import org.dementhium.util.Misc;

public class Casket {
	
	private static int REWARD_INTERFACE = 364;
	
	private static final int EASY_CASKET = 2714;
	private static final int MEDIUM_CASKET = 2802;
	private static final int HARD_CASKET = 2724;
	private static final int ELITE_CASKET = 19039;
	
	
	public static void openCasket(Player player, int casketId) {
		if (!player.getInventory().contains(casketId))
			return;
		if (player.getActivity() instanceof DuelActivity) {
			DuelActivity duel = (DuelActivity) player.getActivity();
				if (duel.getCurrentState() == State.FIGHTING) {
					player.sendMessage("You can't open any caskets during a duel.");
					return;
				}
		}
		Container items = getRewards(casketId);
		if (items == null)
			return;
		player.getInventory().deleteItem(casketId, 1);
		ActionSender.sendInterface(player, REWARD_INTERFACE);
        //ActionSender.sendAMask(player, 1026, REWARD_INTERFACE, 9, 0, 35);
        //ActionSender.sendClientScript(player, 150, new Object[]{"", "", "", "", "", -1, 0, 6, 6, 141, REWARD_INTERFACE << 16 | 28}, "noooobsssss");
		ActionSender.sendItems(player, 141, items, false);
		if (player.interfaceItems == null)
			player.interfaceItems = items;
		else {
			player.getInventory().addAllDropable(player.interfaceItems);
			player.interfaceItems = items;
		}
		player.getInventory().refresh();
		player.sendMessage("Well done, you've completed the Treasure Trail!");
	}
	
	private static Container getRewards(int casketId) {
		int[][] rewardsHolder = null;
		int loopsAmount = 0;
		int random = 0;
		switch (casketId) {
		case EASY_CASKET:
			rewardsHolder = EasyRewards;
			loopsAmount = 7;
			random = 1;
			break;
		case MEDIUM_CASKET:
			rewardsHolder = MediumRewards;
			loopsAmount = 7;
			random = 2;
			break;
		case HARD_CASKET:
		case ELITE_CASKET:
			rewardsHolder = HardRewards;
			loopsAmount = 5;
			random = 3;
			break;
		}
		if (rewardsHolder == null)
			return null;
		Container rewards = new Container(9, true);
		int itemCount = 0;
		if (Misc.random(3) < 3) {
			for (int i = 0; i < loopsAmount; i++) {
				if (Misc.random(random) == random && itemCount < rewards.getSize()) {
					int[] reward = rewardsHolder[(int) (Math.random() * rewardsHolder.length)];
					if (casketId == ELITE_CASKET && Misc.random(1) == 1)
						reward = EliteRewards[(int) (Math.random() * EliteRewards.length)];
					if (((reward[0] >= 10330 && reward[0] <= 10352) || (reward[0] >= 19308 && reward[0] <= 19320)) && Misc.random(2) != 2) //third-age armours
						reward = RewardsForAll[(int) (Math.random() * RewardsForAll.length)];
					int amount = reward[1];
					if (amount > 1 && ItemDefinition.forId(reward[0]).isStackable())
						amount = Misc.random(1, amount);
					Item item = new Item(reward[0], amount);
					if (!rewards.contains(new Item(reward[0])))
						itemCount++;
					rewards.add(item);
				}
			}
		}
		int loopsForAll = 9 - itemCount;
		for (int i = 0; i < loopsForAll; i++) { //for all caskets
			if (Misc.random(1) == 1 && itemCount < rewards.getSize()) {
				int[] reward = RewardsForAll[(int) (Math.random() * RewardsForAll.length)];
				int amount = reward[1];
				if (ItemDefinition.forId(reward[0]).isStackable() && amount > 1)
					amount = Misc.random(1, amount);
				Item item = new Item(reward[0], amount);
				if (!rewards.contains(new Item(reward[0])))
					itemCount++;
				rewards.add(item);
			}
		}
		if (itemCount == 0) { //very unlikely
			int[] reward = rewardsHolder[(int) (Math.random() * rewardsHolder.length)];
			int amount = reward[1];
			if (ItemDefinition.forId(reward[0]).isStackable() && amount > 1)
				amount = Misc.random(1, amount);
			Item item = new Item(reward[0], amount);
			rewards.add(item);
		}
		if (itemCount > 1) {
			return getShuffledContainer(rewards);
		} else
			return rewards;
	}
	
	private static Container getShuffledContainer(Container toBeShuffled) {
		Container copy = toBeShuffled;
		Container finalContainer = new Container(toBeShuffled.getSize(), true);
		int loops = toBeShuffled.size();
		try {
			for (int i = 0; i < loops; i++) {
				if (copy.size() > 0) {
		            int index = Misc.random(copy.size()-1);
		            Item itemToAdd = copy.get(index);
		            if (itemToAdd != null) {
		            	finalContainer.add(itemToAdd);
		            	copy.remove(itemToAdd);
		            }
				}
			}
			return finalContainer;
		} catch (Exception e) {
			return toBeShuffled;
		}
	}
	
	private static int[][] RewardsForAll = { //19622 = meerkat pouch, but finish the pouch first before you add it here as reward.
		{995, 250000}, {556, 400}, {554, 350}, {557, 300}, {563, 150},
		{1694, 1}, {1696, 1}, {1698, 1}, {1700, 1}, {1702, 1},
		{10280, 1}, {10282, 1}, {10284, 1}
	};
	
	private static int[][] EasyRewards = {
		{1077, 1}, {1089, 1}, {1107, 1}, {1125, 1}, {1151, 1}, {1165, 1}, {1179, 1}, {1195, 1}, //black armours
		{1217, 1}, {1283, 1}, {1297, 1}, {1313, 1}, {1327, 1}, {1341, 1}, {1361, 1}, {1367, 1}, {1426, 1}, //black weapons
		{1059, 1}, {1061, 1}, {1063, 1}, {1095, 1}, {1129, 1}, {1167, 1}, {849, 1}, {857, 1}, {330, 30}, //leather + 2 shortbows + 30 (noted) salmon
		{2583, 1}, {2585, 1}, {2587, 1}, {2589, 1}, {2591, 1}, {2593, 1}, {2595, 1}, {2597, 1}, {3472, 1}, {3473, 1}, //black (g) and (t)
		{2631, 1}, {2633, 1}, {2635, 1}, {2637, 1}, {7386, 1}, {7388, 1}, {7390, 1}, {7392, 1}, {7394, 1}, {7396, 1}, //berets, highway masks and wizard (g) and (t)
		{7362, 1}, {7364, 1}, {7366, 1}, {7368, 1}, {7332, 1}, {7338, 1}, {7344, 1}, {7350, 1}, {7356, 1}, {10306, 1}, {10308, 1}, {10310, 1}, //leather (g) and (t) and black heraldic armour
		{10312, 1}, {10314, 1}, {19167, 1}, {19169, 1}, {19171, 1}, //end of black heraldic stuff
		{10404, 1}, {10406, 1}, {10408, 1}, {10410, 1}, {10412, 1}, {10414, 1}, //elegant costumes
		{10316, 1}, {10318, 1}, {10320, 1}, {10322, 1}, {10324, 1}, {10366, 1}, //bob shirts + ammy of magic (skipped the 4 emote enhancers)
		{10458, 1}, {10460, 1}, {10462, 1}, {10464, 1}, {10466, 1}, {10468, 1}, {13081, 1}, {13083, 120}, {13095, 1}, {13105, 1} //guthix, zammy, sara robes + black crossbow and bolts + black cane + spiked helmet
	};
	
	private static int[][] MediumRewards = {
		{1073, 1}, {1091, 1}, {1111, 1}, {1123, 1}, {1145, 1}, {1161, 1}, {1183, 1}, {1199, 1}, //adamant armours
		{1211, 1}, {1245, 1}, {1271, 1}, {1287, 1}, {1301, 1}, {1317, 1}, {1331, 1}, {1345, 1}, {1357, 1}, {1371, 1}, {1430, 1}, {9183, 1}, {9143, 75}, //adamant weapons
		{1393, 1}, {1099, 1}, {1135, 1}, {857, 1}, {374, 30}, {380, 45}, //fire battlestaff, green d'hide, yew shortbow, swordfish + lobster
		{2599, 1}, {2601, 1}, {2603, 1}, {2605, 1}, {2607, 1}, {2609, 1}, {2611, 1}, {2613, 1}, {3474, 1}, {3475, 1}, //adamant (t) and (g)
		{2577, 1}, {2579, 1}, {2647, 1}, {2645, 1}, {2649, 1}, {7319, 1}, {7321, 1}, {7323, 1}, {7325, 1}, {7327, 1}, {7378, 1}, {7380, 1}, {7370, 1}, {7372, 1}, //ranger boots, wizard boots, headbands, boaters (hats), green d'hide (t) and (g)
		{7334, 1}, {7340, 1}, {7346, 1}, {7352, 1}, {7358, 1}, {10296, 1}, {10298, 1}, {10300, 1}, {10302, 1}, {10304, 1}, {19173, 1}, {19175, 1}, {19177, 1}, //adamant heraldic armour
		{19194, 1}, {19196, 1}, {19198, 1}, {19215, 1}, {19217, 1}, {19219, 1}, {19236, 1}, {19238, 1}, {19240, 1}, {19257, 1}, {19259, 1}, {19261, 1}, //adamant heraldic armour
		{10400, 1}, {10402, 1}, {10416, 1}, {10418, 1}, {10420, 1}, {10422, 1}, {10364, 1}, {10446, 1}, {10448, 1}, {10450, 1}, {10452, 1}, {10454, 1}, {10456, 1}, {19380, 1}, {19382, 1}, {19384, 1}, {19386, 1}, {19388, 1}, {19390, 1}, //elegant costumes, str ammy (t), guthix sara and zammy mitres + cloaks / arma bandos and zaros (ancient) robes
		{13109, 1}, {13107, 1}, {13111, 1}, {13113, 1}, {13115, 1}, {13097, 1}, {13103, 1}, //animal masks, adamant cane and pith helmet
	};
	
	private static int[][] HardRewards = {
		{1079, 1}, {1093, 1}, {1113, 1}, {1127, 1}, {1147, 1}, {1163, 1}, {1185, 1}, {1201, 1}, {1213, 1}, {1247, 1}, {1275, 1}, {1289, 1}, {1303, 1}, {1319, 1}, {1333, 1}, {1347, 1}, {1359, 1}, {1373, 1}, {1432, 1}, {9185, 1}, {9144, 55}, //rune armours + weapons
		{2497, 1}, {2503, 1}, {861, 1}, {859, 1}, {380, 55}, {386, 30}, {2615, 1}, {2617, 1}, {2619, 1}, {2621, 1}, {2623, 1}, {2625, 1}, {2627, 1}, {2629, 1}, {3476, 1}, {3477, 1}, //black d'hide, magic short/longbow, lobsters + sharks, rune (g) and (t)
		{2653, 1}, {2655, 1}, {2657, 1}, {2659, 1}, {2661, 1}, {2663, 1}, {2665, 1}, {2667, 1}, {2669, 1}, {2671, 1}, {2673, 1}, {2675, 1}, {3478, 1}, {3479, 1}, {3480, 1}, //zammy sara and guthix trimmed rune armour
		{3481, 1}, {3483, 1}, {3485, 1}, {3486, 1}, {3488, 1}, {3677, 1}, {7374, 1}, {7376, 1}, {7382, 1}, {7384, 1}, {8950, 1}, {2581, 1}, {7398, 1}, {7399, 1}, {7400, 1}, {2639, 1}, {2641, 1}, {2643, 1}, //gilded armour, blue d'hide (g) and (t), pirate hat, robin hood hat, cavaliers,
		{7336, 1}, {7342, 1}, {7348, 1}, {7354, 1}, {7360, 1}, {10286, 1}, {10288, 1}, {10290, 1}, {10292, 1}, {10294, 1}, {19179, 1}, {19182, 1}, {19185, 1}, {19200, 1}, {19203, 1}, {19206, 1}, //rune heraldic armour
		{19221, 1}, {19224, 1}, {19227, 1}, {19242, 1}, {19245, 1}, {19248, 1}, {19263, 1}, {19266, 1}, {19269, 1}, //rune heraldic armour (end)
		{10330, 1}, {10332, 1}, {10334, 1}, {10336, 1}, {10338, 1}, {10340, 1}, {10342, 1}, {10344, 1}, {10346, 1}, {10348, 1}, {10350, 1}, {10352, 1}, {10362, 1}, //third-age melee ranged and mage (and third-age amulet?), amulet of glory (t)
		{10440, 1}, {10442, 1}, {10444, 1}, {10470, 1}, {10472, 1}, {10474, 1}, {19368, 1}, {19370, 1}, {19372, 1}, {19374, 1}, {19376, 1}, {19378, 1}, //zammy sara and guth croziers + stoles, armadyl bandos and zaros (ancient) cloaks + mitres
		{10368, 1}, {10370, 1}, {10372, 1}, {10374, 1}, {10376, 1}, {10378, 1}, {10380, 1}, {10382, 1}, {10384, 1}, {10386, 1}, {10388, 1}, {10390, 1}, //sara guthix and zammy blessed d'hide armours
		{19272, 1}, {19275, 1}, {19278, 1}, {19281, 1}, {19284, 1}, {19287, 1}, {13099, 1}, {13101, 1},//animal masks, rune cane, top hat
		{6889, 1}, {6914, 1}, {6585, 1}, {9470, 1}//fury etc
	};
	
	private static int[][] EliteRewards = {
		{1305, 1}, {1215, 1}, {2453, 15}, {2437, 15}, {2435, 15}, {2441, 15}, {2443, 15}, {2449, 15}, {3017, 15}, {3025, 15}, {2445, 15}, {3041, 15}, {1704, 1}, //d long + d dagger, potions, ammy of glory
		{19308, 1}, {19311, 1}, {19314, 1}, {19317, 1}, {19320, 1}, {19398, 1}, {19401, 1}, {19404, 1}, {19407, 1}, {19410, 1}, {19413, 1}, {19416, 1}, {19419, 1}, {19422, 1}, {19425, 1}, {19428, 1}, {19431, 1}, {19434, 1}, {19437, 1}, {19440, 1}, //third-age druidic, ancient armadyl and bandos rune armour
		{19459, 1}, {19461, 1}, {19463, 1}, {19465, 1}, {19451, 1}, {19453, 1}, {19455, 1}, {19457, 1}, {19443, 1}, {19445, 1}, {19447, 1}, {19449, 1}, //armadyl bandos and ancient (zaros) d'hide armours
		{19392, 1}, {19394, 1}, {19396, 1}, {19362, 1}, {19364, 1}, {19366, 1}, //armadyl bandos and ancient (zaros) stoles + croziers
		{19333, 1}, {19346, 1}, {19348, 1}, {19350, 1}, {19352, 1}, {19354, 1}, {19356, 1}, {19358, 1}, {19360, 1}, //ornament kits
		{19143, 1}, {19146, 1}, {19149, 1}, {19327, 1}, {19329, 1}, {19331, 1}, {19323, 1}, {19325, 1}, //sara guthix and zammy bow, animal staves (staff)
		{19290, 1}, {19296, 1}, {19299, 1}, {19302, 1}, {19305, 1}, {19293, 1}, //animal masks
		{6889, 1}, {6914, 1}, {6585, 1}, {9470, 1}//fury etc
	};
	
}
