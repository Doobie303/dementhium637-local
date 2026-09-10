package org.dementhium.net.packethandlers;

import org.dementhium.model.player.Player;
import org.dementhium.net.PacketHandler;
import org.dementhium.net.message.Message;

/** Handles the client's zero-payload "interfaces closed client-side" packet. */
public final class CloseInterfaceHandler extends PacketHandler {

    @Override
    public void handlePacket(Player player, Message packet) {
        // Close every modal slot and clear inBank/fromBank/bankScreen through
        // Player, including client-side closes that arrive without a walk.
        player.closeAll(false, true);
    }
}
