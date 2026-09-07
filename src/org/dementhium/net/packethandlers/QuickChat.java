package org.dementhium.net.packethandlers;

import org.dementhium.content.misc.QuickChatMessage;
import org.dementhium.model.player.Player;
import org.dementhium.model.player.Skills;
import org.dementhium.net.PacketHandler;
import org.dementhium.net.message.Message;


/**
 * @author Lumby <lumbyjr@hotmail.com>
 */
public class QuickChat extends PacketHandler {

    @Override
    public void handlePacket(Player player, Message packet) {
        switch (packet.getOpcode()) {
            case 61:
                appendChat(player, packet);
                break;

        }
    }
    
    public void appendChat(Player player, Message packet) {
    	@SuppressWarnings("unused")
    	boolean secondClientScript = packet.readByte() == 1;//script 5059 or 5061
    	int fileId = packet.readUnsignedShort();
    	byte[] data = null;
    	if(packet.getLength() > 3) {
    		data = new byte[packet.getLength()-3];
    		packet.readBytes(data, data.length);
    	}
    	if(fileId == 1) {
			data = new byte[] {(byte) player.getSkills().getLevelForExperience(Skills.AGILITY)};
		} else if (fileId == 8)
    		data = new byte[] {(byte) player.getSkills().getLevelForExperience(Skills.ATTACK)};
    	else if (fileId == 13)
    		data = new byte[] {(byte) player.getSkills().getLevelForExperience(Skills.CONSTRUCTION)};
    	else if (fileId == 16)
    		data = new byte[] {(byte) player.getSkills().getLevelForExperience(Skills.COOKING)};
    	else if (fileId == 23)
    		data = new byte[] {(byte) player.getSkills().getLevelForExperience(Skills.CRAFTING)};
    	else if (fileId == 30)
    		data = new byte[] {(byte) player.getSkills().getLevelForExperience(Skills.DEFENCE)};
    	else if (fileId == 34)
    		data = new byte[] {(byte) player.getSkills().getLevelForExperience(Skills.FARMING)};
    	else if (fileId == 41)
    		data = new byte[] {(byte) player.getSkills().getLevelForExperience(Skills.FIREMAKING)};
    	else if (fileId == 47)
    		data = new byte[] {(byte) player.getSkills().getLevelForExperience(Skills.FISHING)};
    	else if (fileId == 55)
    		data = new byte[] {(byte) player.getSkills().getLevelForExperience(Skills.FLETCHING)};
    	else if (fileId == 62)
    		data = new byte[] {(byte) player.getSkills().getLevelForExperience(Skills.HERBLORE)};
    	else if (fileId == 70)
    		data = new byte[] {(byte) player.getSkills().getLevelForExperience(Skills.CONSTITUTION)};
    	else if (fileId == 74)
    		data = new byte[] {(byte) player.getSkills().getLevelForExperience(Skills.HUNTER)};
    	else if (fileId == 135)
    		data = new byte[] {(byte) player.getSkills().getLevelForExperience(Skills.MAGIC)};
    	else if (fileId == 127)
    		data = new byte[] {(byte) player.getSkills().getLevelForExperience(Skills.MINING)};
    	else if (fileId == 120)
    		data = new byte[] {(byte) player.getSkills().getLevelForExperience(Skills.PRAYER)};
    	else if (fileId == 116)
    		data = new byte[] {(byte) player.getSkills().getLevelForExperience(Skills.RANGED)};
    	else if (fileId == 111)
    		data = new byte[] {(byte) player.getSkills().getLevelForExperience(Skills.RUNECRAFTING)};
    	else if (fileId == 103)
    		data = new byte[] {(byte) player.getSkills().getLevelForExperience(Skills.SLAYER)};
    	else if (fileId == 96)
    		data = new byte[] {(byte) player.getSkills().getLevelForExperience(Skills.SMITHING)};
    	else if (fileId == 92)
    		data = new byte[] {(byte) player.getSkills().getLevelForExperience(Skills.STRENGTH)};
    	else if (fileId == 85)
    		data = new byte[] {(byte) player.getSkills().getLevelForExperience(Skills.SUMMONING)};
    	else if (fileId == 79)
    		data = new byte[] {(byte) player.getSkills().getLevelForExperience(Skills.THIEVING)};
    	else if (fileId == 142)
    		data = new byte[] {(byte) player.getSkills().getLevelForExperience(Skills.WOODCUTTING)};
    	else if (fileId == 990)
    		data = new byte[] {(byte) player.getSkills().getLevelForExperience(Skills.DUNGEONEERING)};
    	System.out.println("QuickChat: fileId: "+fileId+"   data: "+(data == null ? "null" : new String(data)));
    	if (data != null)
    		player.sendMessage("Quick Chat doesn't work, our apologies.");
    		//player.getMask().setLastChatMessage(new QuickChatMessage(fileId, data));
    }
    
    
/**
 * boolean secondClientScript = stream.readByte() == 1;//script 5059 or 5061
			int fileId = stream.readUnsignedShort();
			byte[] data = null;
			if(length > 3) {
				data = new byte[length-3];
				stream.readBytes(data);
			}
player.setNextPublicChatMessage(new QuickChatMessage(fileId, data));
 */
    /*private void appendChat(Player player, Message packet) {
        //int i1 = packet.readByte();
        //int i2 = packet.readByte();
        int i1 = packet.readUnsignedShort();
        int i2 = packet.readUnsignedByte();
        System.out.println("i1: " + i1 + " i2: " + i2);//+" i3: "+i3+" i4: "+i4);
        
    }*/
}
