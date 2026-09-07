package org.dementhium.content.skills.summoning;

import org.dementhium.model.player.Player;
import org.dementhium.net.ActionSender;


public class FamiliarDefaults {
	
	
	public static void sendInterface(Familiar familiar) {
		Player owner = familiar.getOwner();
		boolean res = owner.getConnection().getDisplayMode() > 1;
		switch (familiar.getId()) {
		case 6847:
		case 6848: //albino rat
			ActionSender.sendConfig(owner, 448, familiar.getPouchId());
			ActionSender.sendConfig(owner, 1174, familiar.getId());
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);//Gets sent alot on rs, check if needed.
			ActionSender.sendConfig(owner, 1171, 262144); //?
			ActionSender.sendConfig(owner, 1171, 262144); //?
			ActionSender.sendConfig(owner, 1176, 1920); //?
			ActionSender.sendConfig(owner, 2044, 0); //?
			ActionSender.sendConfig(owner, 1801, 5990636); //?
			ActionSender.sendConfig(owner, 1878, 0); //?
			ActionSender.sendConfig(owner, 1231, 595968); //?
			ActionSender.sendConfig(owner, 1160, 8388608); //?
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);//Gets sent alot on rs, check if needed.
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);//Gets sent alot on rs, check if needed.
			ActionSender.sendConfig(owner, 1160, 209715200); //?
			ActionSender.sendInterface(owner, 1, res ? 746 : 548, res ? 104 : 219, 662); //?
			ActionSender.sendAMask(owner, 2, 747, 17, 0, 0); //Special move thingy.
			ActionSender.sendAMask(owner, 2, 662, 74, 0, 0); //Special move thingy.
			ActionSender.sendBConfig(owner, 1436, 1); //?
			ActionSender.sendBConfig(owner, 1000, 66); //?
			ActionSender.sendConfig(owner, 1170, 2949120); //?
			ActionSender.sendConfig(owner, 1160, 8388608); //?
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);
			ActionSender.sendConfig(owner, 1160, 209715200); //?
			ActionSender.sendInterface(owner, 1, res ? 746 : 548, res ? 104 : 219, 662); //?
			ActionSender.sendAMask(owner, 2, 747, 17, 0, 0); //Special move thingy.
			ActionSender.sendAMask(owner, 2, 662, 74, 0, 0); //Special move thingy.
			ActionSender.sendBConfig(owner, 1436, 1); //?
			ActionSender.sendSpecialString(owner, 204, getSpecialName(familiar.getId()));
			ActionSender.sendSpecialString(owner, 205, getSpecialDescription(familiar.getId()));
			return;
		case 6813:
		case 6814: //bunyip
			ActionSender.sendConfig(owner, 448, familiar.getPouchId());
			ActionSender.sendConfig(owner, 1174, familiar.getId());
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);//Gets sent alot on rs, check if needed.
			ActionSender.sendConfig(owner, 1171, 1847296); //?
			ActionSender.sendConfig(owner, 1171, 1847296); //?
			ActionSender.sendConfig(owner, 1176, 4096); //?
			ActionSender.sendConfig(owner, 2044, 0); //?
			ActionSender.sendConfig(owner, 1801, 807463549); //?
			ActionSender.sendConfig(owner, 1878, 0); //?
			ActionSender.sendConfig(owner, 1231, 333824); //?
			ActionSender.sendConfig(owner, 1160, 8388608); //?
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);//Gets sent alot on rs, check if needed.
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);//Gets sent alot on rs, check if needed.
			ActionSender.sendConfig(owner, 1160, 1115684864); //?
			ActionSender.sendInterface(owner, 1, res ? 746 : 548, res ? 104 : 219, 662); //?
			ActionSender.sendAMask(owner, 65536, 747, 17, 0, 0); //Special move thingy.
			ActionSender.sendAMask(owner, 65536, 662, 74, 0, 0); //Special move thingy.
			ActionSender.sendBConfig(owner, 1436, 0); //?
			ActionSender.sendBConfig(owner, 1000, 136); //?
			ActionSender.sendConfig(owner, 1170, 31621120); //?
			ActionSender.sendConfig(owner, 1160, 8388608); //?
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);
			ActionSender.sendConfig(owner, 1160, 1115684864); //?
			ActionSender.sendInterface(owner, 1, res ? 746 : 548, res ? 104 : 219, 662); //?
			ActionSender.sendAMask(owner, 65536, 747, 17, 0, 0); //Special move thingy.
			ActionSender.sendAMask(owner, 65536, 662, 74, 0, 0); //Special move thingy.
			ActionSender.sendBConfig(owner, 1436, 0); //?
			ActionSender.sendSpecialString(owner, 204, getSpecialName(familiar.getId()));
			ActionSender.sendSpecialString(owner, 205, getSpecialDescription(familiar.getId()));
			return;
		case 6871:
		case 6872: //compost mound
			break;
		case 6825:
		case 6826: //dreadfowl
			break;
		case 7355:
		case 7356: //fire titan
			ActionSender.sendConfig(owner, 448, familiar.getPouchId());
			ActionSender.sendConfig(owner, 1174, familiar.getId());
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);//Gets sent alot on rs, check if needed.
			ActionSender.sendConfig(owner, 1171, 262144); //?
			ActionSender.sendConfig(owner, 1171, 262144); //?
			ActionSender.sendConfig(owner, 1176, 1920); //?
			ActionSender.sendConfig(owner, 2044, 0); //?
			ActionSender.sendConfig(owner, 1801, 5990636); //?
			ActionSender.sendConfig(owner, 1878, 0); //?
			ActionSender.sendConfig(owner, 1231, 595968); //?
			ActionSender.sendConfig(owner, 1160, 8388608); //?
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);//Gets sent alot on rs, check if needed.
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);//Gets sent alot on rs, check if needed.
			ActionSender.sendConfig(owner, 1160, 209715200); //?
			ActionSender.sendInterface(owner, 1, res ? 746 : 548, res ? 104 : 219, 662); //?
			ActionSender.sendAMask(owner, 2, 747, 17, 0, 0); //Special move thingy.
			ActionSender.sendAMask(owner, 2, 662, 74, 0, 0); //Special move thingy.
			ActionSender.sendBConfig(owner, 1436, 1); //?
			ActionSender.sendBConfig(owner, 1000, 66); //?
			ActionSender.sendConfig(owner, 1170, 2949120); //?
			ActionSender.sendConfig(owner, 1160, 8388608); //?
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);
			ActionSender.sendConfig(owner, 1160, 209715200); //?
			ActionSender.sendInterface(owner, 1, res ? 746 : 548, res ? 104 : 219, 662); //?
			ActionSender.sendAMask(owner, 2, 747, 17, 0, 0); //Special move thingy.
			ActionSender.sendAMask(owner, 2, 662, 74, 0, 0); //Special move thingy.
			ActionSender.sendBConfig(owner, 1436, 1); //?
			ActionSender.sendSpecialString(owner, 204, getSpecialName(familiar.getId()));
			ActionSender.sendSpecialString(owner, 205, getSpecialDescription(familiar.getId()));
			return;
		case 7339:
		case 7340: //geyser titan
			break;
		case 6796:
		case 6797: //granite crab
			ActionSender.sendConfig(owner, 448, familiar.getPouchId());
			ActionSender.sendConfig(owner, 1174, familiar.getId());
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);//Gets sent alot on rs, check if needed.
			ActionSender.sendConfig(owner, 1171, 262144); //?
			ActionSender.sendConfig(owner, 1171, 262144); //?
			ActionSender.sendConfig(owner, 1176, 1920); //?
			ActionSender.sendConfig(owner, 2044, 0); //?
			ActionSender.sendConfig(owner, 1801, 5990636); //?
			ActionSender.sendConfig(owner, 1878, 0); //?
			ActionSender.sendConfig(owner, 1231, 595968); //?
			ActionSender.sendConfig(owner, 1160, 8388608); //?
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);//Gets sent alot on rs, check if needed.
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);//Gets sent alot on rs, check if needed.
			ActionSender.sendConfig(owner, 1160, 209715200); //?
			ActionSender.sendInterface(owner, 1, res ? 746 : 548, res ? 104 : 219, 662); //?
			ActionSender.sendAMask(owner, 2, 747, 17, 0, 0); //Special move thingy.
			ActionSender.sendAMask(owner, 2, 662, 74, 0, 0); //Special move thingy.
			ActionSender.sendBConfig(owner, 1436, 1); //?
			ActionSender.sendBConfig(owner, 1000, 66); //?
			ActionSender.sendConfig(owner, 1170, 2949120); //?
			ActionSender.sendConfig(owner, 1160, 8388608); //?
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);
			ActionSender.sendConfig(owner, 1160, 209715200); //?
			ActionSender.sendInterface(owner, 1, res ? 746 : 548, res ? 104 : 219, 662); //?
			ActionSender.sendAMask(owner, 2, 747, 17, 0, 0); //Special move thingy.
			ActionSender.sendAMask(owner, 2, 662, 74, 0, 0); //Special move thingy.
			ActionSender.sendBConfig(owner, 1436, 1); //?
			ActionSender.sendSpecialString(owner, 204, getSpecialName(familiar.getId()));
			ActionSender.sendSpecialString(owner, 205, getSpecialDescription(familiar.getId()));
			return;
		case 6824: //magpie, there is no attackable version of him?
			ActionSender.sendConfig(owner, 448, familiar.getPouchId());
			ActionSender.sendConfig(owner, 1174, familiar.getId());
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);//Gets sent alot on rs, check if needed.
			ActionSender.sendConfig(owner, 1171, 1847296); //?
			ActionSender.sendConfig(owner, 1171, 1847296); //?
			ActionSender.sendConfig(owner, 1176, 4096); //?
			ActionSender.sendConfig(owner, 2044, 0); //?
			ActionSender.sendConfig(owner, 1801, 807463549); //?
			ActionSender.sendConfig(owner, 1878, 0); //?
			ActionSender.sendConfig(owner, 1231, 333824); //?
			ActionSender.sendConfig(owner, 1160, 8388608); //?
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);//Gets sent alot on rs, check if needed.
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);//Gets sent alot on rs, check if needed.
			ActionSender.sendConfig(owner, 1160, 1115684864); //?
			ActionSender.sendInterface(owner, 1, res ? 746 : 548, res ? 104 : 219, 662); //?
			ActionSender.sendAMask(owner, 65536, 747, 17, 0, 0); //Special move thingy.
			ActionSender.sendAMask(owner, 65536, 662, 74, 0, 0); //Special move thingy.
			ActionSender.sendBConfig(owner, 1436, 0); //?
			ActionSender.sendBConfig(owner, 1000, 136); //?
			ActionSender.sendConfig(owner, 1170, 31621120); //?
			ActionSender.sendConfig(owner, 1160, 8388608); //?
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);
			ActionSender.sendConfig(owner, 1160, 1115684864); //?
			ActionSender.sendInterface(owner, 1, res ? 746 : 548, res ? 104 : 219, 662); //?
			ActionSender.sendAMask(owner, 65536, 747, 17, 0, 0); //Special move thingy.
			ActionSender.sendAMask(owner, 65536, 662, 74, 0, 0); //Special move thingy.
			ActionSender.sendBConfig(owner, 1436, 0); //?
			ActionSender.sendSpecialString(owner, 204, getSpecialName(familiar.getId()));
			ActionSender.sendSpecialString(owner, 205, getSpecialDescription(familiar.getId()));
			return;
		case 6873:
		case 6874: //pack yak
			ActionSender.sendConfig(owner, 448, familiar.getPouchId());
			ActionSender.sendConfig(owner, 1174, familiar.getId());
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);//Gets sent alot on rs, check if needed.
			ActionSender.sendConfig(owner, 1171, 1847296); //?
			ActionSender.sendConfig(owner, 1171, 1847296); //?
			ActionSender.sendConfig(owner, 1176, 4096); //?
			ActionSender.sendConfig(owner, 2044, 0); //?
			ActionSender.sendConfig(owner, 1801, 807463549); //?
			ActionSender.sendConfig(owner, 1878, 0); //?
			ActionSender.sendConfig(owner, 1231, 333824); //?
			ActionSender.sendConfig(owner, 1160, 8388608); //?
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);//Gets sent alot on rs, check if needed.
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);//Gets sent alot on rs, check if needed.
			ActionSender.sendConfig(owner, 1160, 1115684864); //?
			ActionSender.sendInterface(owner, 1, res ? 746 : 548, res ? 104 : 219, 662); //?
			ActionSender.sendAMask(owner, 65536, 747, 17, 0, 0); //Special move thingy.
			ActionSender.sendAMask(owner, 65536, 662, 74, 0, 0); //Special move thingy.
			ActionSender.sendBConfig(owner, 1436, 0); //?
			ActionSender.sendBConfig(owner, 1000, 136); //?
			ActionSender.sendConfig(owner, 1170, 31621120); //?
			ActionSender.sendConfig(owner, 1160, 8388608); //?
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);
			ActionSender.sendConfig(owner, 1160, 1115684864); //?
			ActionSender.sendInterface(owner, 1, res ? 746 : 548, res ? 104 : 219, 662); //?
			ActionSender.sendAMask(owner, 65536, 747, 17, 0, 0); //Special move thingy.
			ActionSender.sendAMask(owner, 65536, 662, 74, 0, 0); //Special move thingy.
			ActionSender.sendBConfig(owner, 1436, 0); //?
			ActionSender.sendSpecialString(owner, 204, getSpecialName(familiar.getId()));
			ActionSender.sendSpecialString(owner, 205, getSpecialDescription(familiar.getId()));
			return;
		case 6802:
		case 6803: //spirit cobra
			ActionSender.sendConfig(owner, 448, familiar.getPouchId());
			ActionSender.sendConfig(owner, 1174, familiar.getId());
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);//Gets sent alot on rs, check if needed.
			ActionSender.sendConfig(owner, 1171, 1847296); //?
			ActionSender.sendConfig(owner, 1171, 1847296); //?
			ActionSender.sendConfig(owner, 1176, 4096); //?
			ActionSender.sendConfig(owner, 2044, 0); //?
			ActionSender.sendConfig(owner, 1801, 807463549); //?
			ActionSender.sendConfig(owner, 1878, 0); //?
			ActionSender.sendConfig(owner, 1231, 333824); //?
			ActionSender.sendConfig(owner, 1160, 8388608); //?
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);//Gets sent alot on rs, check if needed.
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);//Gets sent alot on rs, check if needed.
			ActionSender.sendConfig(owner, 1160, 1115684864); //?
			ActionSender.sendInterface(owner, 1, res ? 746 : 548, res ? 104 : 219, 662); //?
			ActionSender.sendAMask(owner, 65536, 747, 17, 0, 0); //Special move thingy.
			ActionSender.sendAMask(owner, 65536, 662, 74, 0, 0); //Special move thingy.
			ActionSender.sendBConfig(owner, 1436, 0); //?
			ActionSender.sendBConfig(owner, 1000, 136); //?
			ActionSender.sendConfig(owner, 1170, 31621120); //?
			ActionSender.sendConfig(owner, 1160, 8388608); //?
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);
			ActionSender.sendConfig(owner, 1160, 1115684864); //?
			ActionSender.sendInterface(owner, 1, res ? 746 : 548, res ? 104 : 219, 662); //?
			ActionSender.sendAMask(owner, 65536, 747, 17, 0, 0); //Special move thingy.
			ActionSender.sendAMask(owner, 65536, 662, 74, 0, 0); //Special move thingy.
			ActionSender.sendBConfig(owner, 1436, 0); //?
			ActionSender.sendSpecialString(owner, 204, getSpecialName(familiar.getId()));
			ActionSender.sendSpecialString(owner, 205, getSpecialDescription(familiar.getId()));
			return;
		case 7331:
		case 7332: //spirit mosquito
			break;
		case 6841:
		case 6842: //spirit spider
			ActionSender.sendConfig(owner, 448, familiar.getPouchId());
			ActionSender.sendConfig(owner, 1174, familiar.getId());
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);//Gets sent alot on rs, check if needed.
			ActionSender.sendConfig(owner, 1171, 262144); //?
			ActionSender.sendConfig(owner, 1171, 262144); //?
			ActionSender.sendConfig(owner, 1176, 1920); //?
			ActionSender.sendConfig(owner, 2044, 0); //?
			ActionSender.sendConfig(owner, 1801, 5990636); //?
			ActionSender.sendConfig(owner, 1878, 0); //?
			ActionSender.sendConfig(owner, 1231, 595968); //?
			ActionSender.sendConfig(owner, 1160, 8388608); //?
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);//Gets sent alot on rs, check if needed.
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);//Gets sent alot on rs, check if needed.
			ActionSender.sendConfig(owner, 1160, 209715200); //?
			ActionSender.sendInterface(owner, 1, res ? 746 : 548, res ? 104 : 219, 662); //?
			ActionSender.sendAMask(owner, 2, 747, 17, 0, 0); //Special move thingy.
			ActionSender.sendAMask(owner, 2, 662, 74, 0, 0); //Special move thingy.
			ActionSender.sendBConfig(owner, 1436, 1); //?
			ActionSender.sendBConfig(owner, 1000, 66); //?
			ActionSender.sendConfig(owner, 1170, 2949120); //?
			ActionSender.sendConfig(owner, 1160, 8388608); //?
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);
			ActionSender.sendConfig(owner, 1160, 209715200); //?
			ActionSender.sendInterface(owner, 1, res ? 746 : 548, res ? 104 : 219, 662); //?
			ActionSender.sendAMask(owner, 2, 747, 17, 0, 0); //Special move thingy.
			ActionSender.sendAMask(owner, 2, 662, 74, 0, 0); //Special move thingy.
			ActionSender.sendBConfig(owner, 1436, 1); //?
			ActionSender.sendSpecialString(owner, 204, getSpecialName(familiar.getId()));
			ActionSender.sendSpecialString(owner, 205, getSpecialDescription(familiar.getId()));
			return;
		case 6829:
		case 6830: //spirit wolf
			break;
		case 7343:
		case 7344: //steel titan
			break;
		case 6806:
		case 6807: //thorny snail
			break;
		case 6815:
		case 6816: //tortoise
			ActionSender.sendConfig(owner, 448, familiar.getPouchId());
			ActionSender.sendConfig(owner, 1174, familiar.getId());
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);//Gets sent alot on rs, check if needed.
			ActionSender.sendConfig(owner, 1171, 262144); //?
			ActionSender.sendConfig(owner, 1171, 262144); //?
			ActionSender.sendConfig(owner, 1176, 1920); //?
			ActionSender.sendConfig(owner, 2044, 0); //?
			ActionSender.sendConfig(owner, 1801, 5990636); //?
			ActionSender.sendConfig(owner, 1878, 0); //?
			ActionSender.sendConfig(owner, 1231, 595968); //?
			ActionSender.sendConfig(owner, 1160, 8388608); //?
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);//Gets sent alot on rs, check if needed.
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);//Gets sent alot on rs, check if needed.
			ActionSender.sendConfig(owner, 1160, 209715200); //?
			ActionSender.sendInterface(owner, 1, res ? 746 : 548, res ? 104 : 219, 662); //?
			ActionSender.sendAMask(owner, 2, 747, 17, 0, 0); //Special move thingy.
			ActionSender.sendAMask(owner, 2, 662, 74, 0, 0); //Special move thingy.
			ActionSender.sendBConfig(owner, 1436, 1); //?
			ActionSender.sendBConfig(owner, 1000, 66); //?
			ActionSender.sendConfig(owner, 1170, 2949120); //?
			ActionSender.sendConfig(owner, 1160, 8388608); //?
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);
			ActionSender.sendConfig(owner, 1160, 209715200); //?
			ActionSender.sendInterface(owner, 1, res ? 746 : 548, res ? 104 : 219, 662); //?
			ActionSender.sendAMask(owner, 2, 747, 17, 0, 0); //Special move thingy.
			ActionSender.sendAMask(owner, 2, 662, 74, 0, 0); //Special move thingy.
			ActionSender.sendBConfig(owner, 1436, 1); //?
			ActionSender.sendSpecialString(owner, 204, getSpecialName(familiar.getId()));
			ActionSender.sendSpecialString(owner, 205, getSpecialDescription(familiar.getId()));
			return;
		case 6822:
		case 6823: //unicorn stallion
			ActionSender.sendConfig(owner, 448, familiar.getPouchId());
			ActionSender.sendConfig(owner, 1174, familiar.getId());
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);//Gets sent alot on rs, check if needed.
			ActionSender.sendConfig(owner, 1171, 262144); //?
			ActionSender.sendConfig(owner, 1171, 262144); //?
			ActionSender.sendConfig(owner, 1176, 1920); //?
			ActionSender.sendConfig(owner, 2044, 0); //?
			ActionSender.sendConfig(owner, 1801, 5990636); //?
			ActionSender.sendConfig(owner, 1878, 0); //?
			ActionSender.sendConfig(owner, 1231, 595968); //?
			ActionSender.sendConfig(owner, 1160, 8388608); //?
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);//Gets sent alot on rs, check if needed.
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);//Gets sent alot on rs, check if needed.
			ActionSender.sendConfig(owner, 1160, 209715200); //?
			ActionSender.sendInterface(owner, 1, res ? 746 : 548, res ? 104 : 219, 662); //?
			ActionSender.sendAMask(owner, 2, 747, 17, 0, 0); //Special move thingy.
			ActionSender.sendAMask(owner, 2, 662, 74, 0, 0); //Special move thingy.
			ActionSender.sendBConfig(owner, 1436, 1); //?
			ActionSender.sendBConfig(owner, 1000, 66); //?
			ActionSender.sendConfig(owner, 1170, 2949120); //?
			ActionSender.sendConfig(owner, 1160, 8388608); //?
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);
			ActionSender.sendConfig(owner, 1160, 209715200); //?
			ActionSender.sendInterface(owner, 1, res ? 746 : 548, res ? 104 : 219, 662); //?
			ActionSender.sendAMask(owner, 2, 747, 17, 0, 0); //Special move thingy.
			ActionSender.sendAMask(owner, 2, 662, 74, 0, 0); //Special move thingy.
			ActionSender.sendBConfig(owner, 1436, 1); //?
			ActionSender.sendSpecialString(owner, 204, getSpecialName(familiar.getId()));
			ActionSender.sendSpecialString(owner, 205, getSpecialDescription(familiar.getId()));
			return;
		case 6869:
		case 6870: //wolpertinger
			ActionSender.sendConfig(owner, 448, familiar.getPouchId());
			ActionSender.sendConfig(owner, 1174, familiar.getId());
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);//Gets sent alot on rs, check if needed.
			ActionSender.sendConfig(owner, 1171, 262144); //?
			ActionSender.sendConfig(owner, 1171, 262144); //?
			ActionSender.sendConfig(owner, 1176, 1920); //?
			ActionSender.sendConfig(owner, 2044, 0); //?
			ActionSender.sendConfig(owner, 1801, 5990636); //?
			ActionSender.sendConfig(owner, 1878, 0); //?
			ActionSender.sendConfig(owner, 1231, 595968); //?
			ActionSender.sendConfig(owner, 1160, 8388608); //?
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);//Gets sent alot on rs, check if needed.
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);//Gets sent alot on rs, check if needed.
			ActionSender.sendConfig(owner, 1160, 209715200); //?
			ActionSender.sendInterface(owner, 1, res ? 746 : 548, res ? 104 : 219, 662); //?
			ActionSender.sendAMask(owner, 2, 747, 17, 0, 0); //Special move thingy.
			ActionSender.sendAMask(owner, 2, 662, 74, 0, 0); //Special move thingy.
			ActionSender.sendBConfig(owner, 1436, 1); //?
			ActionSender.sendBConfig(owner, 1000, 66); //?
			ActionSender.sendConfig(owner, 1170, 2949120); //?
			ActionSender.sendConfig(owner, 1160, 8388608); //?
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);
			ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);
			ActionSender.sendConfig(owner, 1160, 209715200); //?
			ActionSender.sendInterface(owner, 1, res ? 746 : 548, res ? 104 : 219, 662); //?
			ActionSender.sendAMask(owner, 2, 747, 17, 0, 0); //Special move thingy.
			ActionSender.sendAMask(owner, 2, 662, 74, 0, 0); //Special move thingy.
			ActionSender.sendBConfig(owner, 1436, 1); //?
			ActionSender.sendSpecialString(owner, 204, getSpecialName(familiar.getId()));
			ActionSender.sendSpecialString(owner, 205, getSpecialDescription(familiar.getId()));
			return;
		}
		ActionSender.sendConfig(owner, 448, familiar.getPouchId());
		ActionSender.sendConfig(owner, 1174, familiar.getId());
		ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);
		ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);//Gets sent alot on rs, check if needed.
		ActionSender.sendConfig(owner, 1171, 262144); //?
		ActionSender.sendConfig(owner, 1171, 262144); //?
		ActionSender.sendConfig(owner, 1176, 768); //?
		ActionSender.sendConfig(owner, 2044, 0); //?
		ActionSender.sendConfig(owner, 1801, 5990631); //?
		ActionSender.sendConfig(owner, 1878, 0); //?
		ActionSender.sendConfig(owner, 1231, 595968); //?
		ActionSender.sendConfig(owner, 1160, 8388608); //?
		ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);//Gets sent alot on rs, check if needed.
		ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);//Gets sent alot on rs, check if needed.
		ActionSender.sendConfig(owner, 1160, 243269632); //?
		ActionSender.sendInterface(owner, 1, res ? 746 : 548, res ? 104 : 219, 662); //?
		ActionSender.sendAMask(owner, 20480, 747, 17, 0, 0); //Special move thingy.
		ActionSender.sendAMask(owner, 20480, 662, 74, 0, 0); //Special move thingy.
		ActionSender.sendBConfig(owner, 1436, 0); //?
		ActionSender.sendBConfig(owner, 1000, 66); //?
		ActionSender.sendConfig(owner, 1160, 8388608); //?
		ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);
		ActionSender.sendConfig(owner, 1175, getSpecialCost(familiar.getId()) << 23);
		ActionSender.sendConfig(owner, 1160, 243269632); //?
		ActionSender.sendInterface(owner, 1, res ? 746 : 548, res ? 104 : 219, 662); //?
		ActionSender.sendAMask(owner, 20480, 747, 17, 0, 0); //Special move thingy.
		ActionSender.sendAMask(owner, 20480, 662, 74, 0, 0); //Special move thingy.
		ActionSender.sendBConfig(owner, 1436, 0); //?
		ActionSender.sendSpecialString(owner, 204, getSpecialName(familiar.getId()));
		ActionSender.sendSpecialString(owner, 205, getSpecialDescription(familiar.getId()));
		return;
	}
	
	public static int getPouchId(int familiarId, int pouchId) {
		switch (familiarId) {
		case 6847:
		case 6848: //albino rat
			return 12067;
		case 6813:
		case 6814: //bunyip
			return 12029;
		case 6871:
		case 6872: //compost mound
			return 12091;
		case 6825:
		case 6826: //dreadfowl
			return 12043;
		case 7355:
		case 7356: //fire titan
			return 12802;
		case 7339:
		case 7340: //geyser titan
			return 12786;
		case 6796:
		case 6797: //granite crab
			return 12069;
		case 6824: //magpie, there is no attackable version of him?
			return 12093;
		case 6873:
		case 6874: //pack yak
			return 12093;
		case 6802:
		case 6803: //spirit cobra
			return 12015;
		case 7331:
		case 7332: //spirit mosquito
			return 12778;
		case 6841:
		case 6842: //spirit spider
			return 12059;
		case 6829:
		case 6830: //spirit wolf
			return 12047;
		case 7343:
		case 7344: //steel titan
			return 12790;
		case 6806:
		case 6807: //thorny snail
			return 12019;
		case 6815:
		case 6816: //tortoise
			return 12039;
		case 6822:
		case 6823: //unicorn stallion
			return 12039;
		case 6869:
		case 6870: //wolpertinger
			return 12089;
		default:
			break;
		}
		return pouchId;
	}
	
	/**
	 * Gets the amount of special move cost.
	 * @return The amount.
	 */
	public static int getSpecialCost(int familiarId) {
		switch (familiarId) {
		case 6847:
		case 6848: //albino rat
			return 6;
		case 6813:
		case 6814: //bunyip
			return 3;
		case 6871:
		case 6872: //compost mound
			return 12;
		case 6825:
		case 6826: //dreadfowl
			return 3;
		case 7355:
		case 7356: //fire titan
			return 20;
		case 7339:
		case 7340: //geyser titan
			return 6;
		case 6796:
		case 6797: //granite crab
			return 12;
		case 6824: //magpie, there is no attackable version of him?
			return 12;
		case 6873:
		case 6874: //pack yak
			return 12;
		case 6802:
		case 6803: //spirit cobra
			return 12;
		case 7331:
		case 7332: //spirit mosquito
			return 3;
		case 6841:
		case 6842: //spirit spider
			return 6;
		case 6829:
		case 6830: //spirit wolf
			return 3;
		case 7343:
		case 7344: //steel titan
			return 12;
		case 6806:
		case 6807: //thorny snail
			return 3;
		case 6815:
		case 6816: //tortoise
			return 20;
		case 6822:
		case 6823: //unicorn stallion
			return 20;
		case 6869:
		case 6870: //wolpertinger
			return 20;
		}
		return 3;
	}
	
	/**
	 * Gets the name of the special move.
	 * @return The name.
	 */
	public static String getSpecialName(int familiarId) {
		switch (familiarId) {
		case 6847:
		case 6848: //albino rat
			return "Cheese Feast";
		case 6813:
		case 6814: //bunyip
			return "Swallow whole";
		case 6871:
		case 6872: //compost mound
			return "Generate Compost"; 
		case 6825:
		case 6826: //dreadfowl
			return "Dreadfowl Strike";
		case 7355:
		case 7356: //fire titan
			return "Titan's Constitution";
		case 7339:
		case 7340: //geyser titan
			return "Boil";
		case 6796:
		case 6797: //granite crab
			return "Stoney Shell"; 
		case 6824: //magpie, there is no attackable version of him?
			return "Winter Storage";
		case 6873:
		case 6874: //pack yak
			return "Winter Storage";
		case 6802:
		case 6803: //spirit cobra
			return "Healing Aura";
		case 7331:
		case 7332: //spirit mosquito
			return "Pester"; 
		case 6841:
		case 6842: //spirit spider
			return "Egg Spawn";
		case 6829:
		case 6830: //spirit wolf
			return "Howl";
		case 7343:
		case 7344: //steel titan
			return "Steel of Legends";
		case 6806:
		case 6807: //thorny snail
			return "Slime Spray";
		case 6815:
		case 6816: //tortoise
			return "Testudo";
		case 6822:
		case 6823: //unicorn stallion
			return "Healing Aura";
		case 6869:
		case 6870: //wolpertinger
			return "Magic Focus";
		}
		return "No special move";
	}
	
	/**
	 * Gets a description of the special attack.
	 * @return The special attack description.
	 */
	public static String getSpecialDescription(int familiarId) {
		switch (familiarId) {
		case 6847:
		case 6848: //albino rat
			return "Puts 4 cheese into the rat's inventory"; 
		case 6813:
		case 6814: //bunyip
			return "Allows you to eat an uncooked fish, assuming you have the level to cook it";
		case 6871:
		case 6872: //compost mound
			return "Fills a nearby compost bin with compost, with a small chance of producing supercompost";
		case 6825:
		case 6826: //dreadfowl
			return "Magic attack that inflicts up to 30 damage";
		case 7355:
		case 7356: //fire titan
			return "Boosts your Defence and life points significantly";
		case 7339:
		case 7340: //geyser titan
			return "Damages a player, doing more damage based on their armour";
		case 6796:
		case 6797: //granite crab
			return "Gives you a +4 Defence boost"; 
		case 6824: //magpie, there is no attackable version of him?
			return "Use special move on an item in your inventory to send it to your bank";
		case 6873:
		case 6874: //pack yak
			return "Use special move on an item in your inventory to send it to your bank";
		case 6802:
		case 6803: //spirit cobra
			return "Use special move on an item in your inventory to send it to your bank";
		case 7331:
		case 7332: //spirit mosquito
			return "Sends the mosquito to attack an enemy";
		case 6841:
		case 6842: //spirit spider
			return "Creates a random number of red spider eggs";
		case 6829:
		case 6830: //spirit wolf
			return "Causes NPC foes to flee";
		case 7343:
		case 7344: //steel titan
			return "The steel titan's next attack will be four powerful ranged attacks";
		case 6806:
		case 6807: //thorny snail
			return "Attack that inflicts up to 80 damage";
		case 6815:
		case 6816: //tortoise
			return "Raises your Defence by 9.";
		case 6822:
		case 6823: //unicorn stallion
			return "Heals up to 15% of your life points";
		case 6869:
		case 6870: //wolpertinger
			return "Boost's your magic by 7";
		}
		return "No special move";
	}

}






/*boolean fullScreen = owner.getConnection().getDisplayMode() >= 2;
/*ActionSender.sendConfig(owner, 448, getId());
ActionSender.sendConfig(owner, 1174, getId());
ActionSender.sendConfig(owner, 1175, 102025930);
ActionSender.sendConfig(owner, 1175, 102025930);
ActionSender.sendConfig(owner, 1171, 20480);
ActionSender.sendConfig(owner, 1171, 20480);
ActionSender.sendConfig(owner, 1176, 7424);
ActionSender.sendConfig(owner, 448, 12093);
ActionSender.sendConfig(owner, 1174, 6873);
ActionSender.sendConfig(owner, 1175, 60);
ActionSender.sendConfig(owner, 1801, 48);
ActionSender.sendConfig(owner, 1231, 333839);
ActionSender.sendConfig(owner, 1160, getHeadAnimConfig(getId()));
ActionSender.sendConfig(owner, 1175, 102025930);
ActionSender.sendConfig(owner, 1175, 102025930);
ActionSender.sendConfig(owner, 1160, getHeadAnimConfig(getId()));
ActionSender.sendConfig(owner, 108, 0);
ActionSender.sendInterface(owner,1, fullScreen ? 746 : 548, 51, 662);
ActionSender.sendInterface(owner, 1, fullScreen ? 746 : 548, 34, 884);
ActionSender.sendAMask(owner, -1, -1, fullScreen ? 746 : 548, 125, 0, 2);
ActionSender.sendAMask(owner, -1, -1, 884, 11, 0, 2);
ActionSender.sendAMask(owner, -1, -1, 884, 12, 0, 2);
ActionSender.sendAMask(owner, -1, -1, 884, 13, 0, 2);
ActionSender.sendConfig(owner, 1175, 102025930);
ActionSender.sendConfig(owner, 1175, 102025930);
ActionSender.sendInterface(owner, 1, fullScreen ? 746 : 548, 51, 662);*/
/*ActionSender.sendConfig(owner, 448, getPouchId()); //getPouchId>
ActionSender.sendConfig(owner, 1174, getId());
ActionSender.sendConfig(owner, 1175, getSpecialCost() << 23);
//ActionSender.sendConfig(owner, 1175, 102025930);
//ActionSender.sendConfig(owner, 1171, 20480);
ActionSender.sendConfig(owner, 1171, 262144); //?
//ActionSender.sendConfig(owner, 1171, 20480);
//ActionSender.sendConfig(owner, 1176, 7424);
ActionSender.sendConfig(owner, 1176, 768); //?
ActionSender.sendConfig(owner, 2044, 0); //?
//ActionSender.sendConfig(owner, 1801, 48);
ActionSender.sendConfig(owner, 1801, 5990631); //?
ActionSender.sendConfig(owner, 1878, 0); //?
//ActionSender.sendConfig(owner, 1231, 333839);
ActionSender.sendConfig(owner, 1231, 595968); //?
//ActionSender.sendConfig(owner, 1160, getHeadAnimConfig(getId()));
ActionSender.sendBConfig(owner, 1000, 66); //?
ActionSender.sendConfig(owner, 1160, 243269632); //?
//ActionSender.sendConfig(owner, 1175, 102025930);
//ActionSender.sendConfig(owner, 1175, 102025930);
//ActionSender.sendConfig(owner, 1160, getHeadAnimConfig(getId()));
//ActionSender.sendConfig(owner, 108, 1);
ActionSender.sendInterface(owner, 1, fullScreen ? 746 : 548, 51, 662);
ActionSender.sendInterface(owner, 1, fullScreen ? 746 : 548, 34, 884);
ActionSender.sendAMask(owner, -1, -1, 746, 125, 0, 2);
ActionSender.sendAMask(owner, -1, -1, 884, 11, 0, 2);
ActionSender.sendAMask(owner, -1, -1, 884, 12, 0, 2);
ActionSender.sendAMask(owner, -1, -1, 884, 13, 0, 2);
ActionSender.sendAMask(owner, 20480, 747, 17, 0, 0); //Special move thingy.
ActionSender.sendAMask(owner, 20480, 662, 74, 0, 0); //Special move thingy.
ActionSender.sendBConfig(owner, 1436, 0);
ActionSender.sendSpecialString(owner, 204, getSpecialName());
ActionSender.sendSpecialString(owner, 205, getSpecialDescription());
//ActionSender.sendConfig(owner, 1175, 102025930);
//ActionSender.sendConfig(owner, 1175, 102025930);
//ActionSender.sendInterface(owner, 1, fullScreen ? 746 : 548, 51, 662);
//ActionSender.sendConfig(owner, 1494, owner.getSettings().getSummoningOption());
//ActionSender.sendInterface(owner, 1, fullScreen ? 746 : 548, fullScreen ? 104 : 219, 880);
//ActionSender.sendBConfig(owner, 168, 95);
//Added this from FamiliarInterfaceListener:
ActionSender.sendInterface(owner, 1, fullScreen ? 746 : 548, fullScreen ? 104 : 219, 880);*/
