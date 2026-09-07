/*package org.dementhium.net.packethandlers;

import org.dementhium.content.activity.impl.CastleWarsActivity;
import org.dementhium.content.areas.CoordinateEvent;
import org.dementhium.content.misc.Following;
import org.dementhium.model.World;
import org.dementhium.model.map.path.DefaultPathFinder;
import org.dementhium.model.player.Player;
import org.dementhium.model.player.TradeSession;
import org.dementhium.net.ActionSender;
import org.dementhium.net.PacketHandler;
import org.dementhium.net.message.Message;
import org.dementhium.util.Constants;

/**
 * @author Steve
 */
/*public class TradePacketHandler extends PacketHandler {

    private final int TRADE_PACKET_ID = 47;

    @Override
    public void handlePacket(Player player, Message packet) {
        if (packet.getOpcode() == TRADE_PACKET_ID) {
            handleTradeRequest(player, packet);
        }

    }

    private void handleTradeRequest(final Player player, Message packet) {
        packet.readByteS();
        int partnerIndex = packet.readShort();
        if (partnerIndex < 0 || partnerIndex >= Constants.MAX_AMT_OF_PLAYERS) {
            return;
        }
        final Player partner = World.getWorld().getPlayers().get(partnerIndex);
        if (partner == null || !partner.isOnline() || player.getIndex() == partner.getIndex()) {
            return;
        }
		player.closeAll(true, true);
        player.turnTo(partner, false);
        if (!World.getWorld().doPath(new DefaultPathFinder(), player, partner.getLocation().getX(), partner.getLocation().getY(), false, false).isRouteFound()) {
            player.sendMessage("I can't reach that!");
            return;
        } else {
            if ((player.isInDuelArenaDuel() || partner.isInDuelArenaDuel()) && player.getRights() < 2 && partner.getRights() < 2) {
            	if (player.isInDuelArenaDuel() || (player.isInDuelArenaDuel() && partner.isInDuelArenaDuel()))
            		player.sendMessage("You can't trade during a duel.");
            	else if (partner.isInDuelArenaDuel())
            		player.sendMessage("This player is in a duel and can't be traded.");
            	return;
            }
            if ((player.getActivity() instanceof CastleWarsActivity || partner.getActivity() instanceof CastleWarsActivity)
            	&& player.getRights() < 2 && partner.getRights() < 2) {
            	if (player.getActivity() instanceof CastleWarsActivity || (player.getActivity() instanceof CastleWarsActivity && partner.getActivity() instanceof CastleWarsActivity))
            		player.sendMessage("You can't trade during this minigame.");
            	else if (partner.getActivity() instanceof CastleWarsActivity)
            		player.sendMessage("This player is in a castle wars minigame and can't be traded.");
            	return;
            }
            Following.combatFollow(player, partner);
        }
        World.getWorld().submitAreaEvent(player, new CoordinateEvent(player, partner.getLocation().getX(), partner.getLocation().getY(), partner.size(), partner.size()) {

            @Override
            public void execute() {
                if (partner.getTradeSession() != null) {
                    ActionSender.sendMessage(player, "The other player is busy.");
                    return;
                }
                if (partner.getAttribute("didRequestTrade") == Boolean.TRUE && ((Short) partner.getAttribute("tradeWithIndex") == player.getIndex())) {
                    TradeSession session = new TradeSession(player, partner);
                    player.setTradeSession(session);
                    partner.setTradePartner(player);
                    session.start();
                }
            }
        });
    }


}*/
