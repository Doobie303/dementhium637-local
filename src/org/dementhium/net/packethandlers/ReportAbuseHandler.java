package org.dementhium.net.packethandlers;

import org.dementhium.model.player.Player;
import org.dementhium.net.PacketHandler;
import org.dementhium.net.message.Message;

/**
 *
 * @author Lumby <lumbyjr@hotmail.com>
 */
public class ReportAbuseHandler extends PacketHandler {

	@Override
	public void handlePacket(Player player, Message packet) {
		//int reportLocation = packet.readByte();
		String offender = packet.readRS2String();
		int offense = packet.readByte();
		packet.readByte();
		packet.readRS2String();
		String ruleName = "";
		switch(offense){
		case 6:
			ruleName = "Buying or selling account";
		case 9:
			ruleName = "Encouraging rule breaking";
		case 5:
			ruleName = "Staff impersonation";
		case 7:
			ruleName = "Macroing/use of bots";
		case 15:
			ruleName = "Scamming";
		case 4:
			ruleName = "Exploiting a bug";
		case 16:
			ruleName = "Seriously offensive language";
		case 17:
			ruleName = "Solicitation";
		case 18:
			ruleName = "Disruptive behaviour";
		case 19:
			ruleName = "Offensive account name";
		case 20:
			ruleName = "Real life threats";
		case 13:
			ruleName = "Asking for real life info";
		case 21:
			ruleName = "Breaking real world laws";
		case 11:
			ruleName = "Advertising websites";
			break;
		}
		System.out.println("Report: Offender: "+offender+" Offense: "+ruleName+" Reported By: "+player.getDisplayName());
		player.closeAll(false, false);
		player.sendMessage("Thank-you, your abuse report has been received."); 
	}

}
