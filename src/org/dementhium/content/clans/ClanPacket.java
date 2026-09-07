package org.dementhium.content.clans;

import org.dementhium.model.player.Player;
import org.dementhium.net.message.Message.PacketType;
import org.dementhium.net.message.MessageBuilder;
import org.dementhium.util.Constants;
import org.dementhium.util.Misc;


/**
 * @author 'Mystic Flow
 */
public class ClanPacket {
    public static void sendClanList(Player p, Clan clan) {
        MessageBuilder bldr = new MessageBuilder(28, PacketType.VAR_SHORT);
        if (clan != null) {
        	String ownerName = Misc.formatPlayerNameForDisplay(clan.getOwner());
        	if (clan.getOwnerDisplayName() != null)
        		ownerName = clan.getOwnerDisplayName();
            bldr.writeRS2String(ownerName);
            bldr.writeByte(0);
            bldr.writeLong(Misc.stringToLong(clan.getName()));
            bldr.writeByte(clan.getKickReq());
            bldr.writeByte(clan.getMembers().size());
            for (Player pl : clan.getMembers()) {
            	String username = Misc.formatPlayerNameForDisplay(pl.getUsername());
            	String displayName = Misc.formatPlayerNameForDisplay(pl.getDisplayName());
                bldr.writeRS2String(displayName); //to make display names work this is about the only line I changed
                bldr.writeByte(1); // display name (leave it like that)
                bldr.writeRS2String(username);
                bldr.writeShort(1); // idk tbh ;s
                bldr.writeByte(clan.getRank(pl));
                bldr.writeRS2String(pl.getConnection().isInLobby() ? "<col=00FF00>Lobby 1" : "<col=00FF00>"+Constants.SERVER_NAME + " 1");
            }
        }
        p.write(bldr.toMessage());
    }
}
