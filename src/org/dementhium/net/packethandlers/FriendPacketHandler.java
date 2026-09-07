package org.dementhium.net.packethandlers;

import org.dementhium.model.World;
import org.dementhium.model.player.Player;
import org.dementhium.net.ActionSender;
import org.dementhium.net.PacketHandler;
import org.dementhium.net.message.Message;
import org.dementhium.util.Logger;
import org.dementhium.util.Misc;
import org.dementhium.util.TextUtils;

/**
 * @author 'Mystic Flow <Steven@rune-server.org>
 */
public final class FriendPacketHandler extends PacketHandler {

    public static final int ADD_FRIEND = 2, REMOVE_FRIEND = 77, ADD_IGNORE = 74, REMOVE_IGNORE = -1;

    public static final int SEND_PRIVATE_MESSAGE = 41;
    
    public static final int PRIVATE_STATUS = 44;

    @Override
    public void handlePacket(Player player, Message packet) {
        switch (packet.getOpcode()) {
        	case PRIVATE_STATUS:
        		
        		break;
            case ADD_IGNORE:
                if (!player.hasReceivedStarter()) {
                    //player.sendMessage("Please finish the tutorial first before you start ignoring people.");
                    return;
                }
                player.getFriendManager().addIgnore(packet.readRS2String());
                break;
            case ADD_FRIEND:
                if (!player.hasReceivedStarter()) {
                    //player.sendMessage("Please finish the tutorial first before you start making friends.");
                    return;
                }
                player.getFriendManager().addFriend(packet.readRS2String());
                break;
            case REMOVE_FRIEND:
                player.getFriendManager().removeFriend(packet.readRS2String());
                break;
            case REMOVE_IGNORE:
                player.getFriendManager().removeIgnore(packet.readRS2String());
                break;
            case SEND_PRIVATE_MESSAGE:
                String otherName = packet.readRS2String();
                if (otherName == null || otherName.equals(player.getDisplayName()))
                    return;
                int numChars = packet.readUnsignedByte();
                String outMessage = TextUtils.decompressHuffman(packet, numChars);
                if (outMessage == null)
                    return;
                if (World.getWorld().getOffencesHandler().isMuted(player)) {
    				player.sendMessage("You have been temporarily muted due to breaking a rule.");
    				//player.sendMessage("This mute will remain for a further 3 days.");
    				player.sendMessage("To prevent further mutes please read the rules.");
                    return;
                }
                outMessage = Misc.optimizeText(outMessage);
                Player sendPlayer = null;
                for (Player p2 : World.getWorld().getPlayers()) {
                    if (p2.getFormattedUsername().equals(otherName) || (p2.hasDisplayName() && p2.getDisplayName().equals(otherName))) {
                    	sendPlayer = p2;
                    	break;
                    }
                }
                if (sendPlayer == null) {
                    for (Player p2 : World.getWorld().getLobbyPlayers()) {
                        if (p2.getFormattedUsername().equals(otherName) || (p2.hasDisplayName() && p2.getDisplayName().equals(otherName))) {
                        	sendPlayer = p2;
                        	break;
                        }
                    }
                }
                if (sendPlayer != null) {
                    if (!sendPlayer.hasReceivedStarter()) {
                        player.sendMessage("Let this new adventurer finish his tutorial first.");
                        return;
                    }
                    ActionSender.sendPrivateMessage(player, otherName, outMessage);
                    ActionSender.receivePrivateMessage(sendPlayer, player.getFormattedUsername(), player.getDisplayName(), player.getRights(), outMessage);
                    Logger.writeChatLog(player, outMessage, 1, sendPlayer);
                    return;
                } else {
                    player.sendMessage("That player is not available at the moment.");//XXX Make this message like rs
                }
                break;
        }
    }

}
