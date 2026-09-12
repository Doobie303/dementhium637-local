package org.dementhium.net;

import org.dementhium.model.player.Player;
import org.dementhium.net.message.Message;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;

public class GameSession {

    public GameSession(Channel channel) {
        this.channel = channel;
    }

    private Channel channel;
    private Player player;
    private int displayMode;
    private boolean inLobby;
    private boolean gamblerInterface;
    private boolean staffToolsInterface;
    private boolean infernalCape;
    private boolean osrsEquipment;
    public boolean supportsInfernalCape(){return infernalCape;}
    public boolean supportsOsrsEquipment(){return osrsEquipment;}
    /** Client feature markers only; they never grant gameplay or staff permission. */
    public void readClientSettings(String settings) {
        infernalCape = hasCapability(settings, "infernal-cape");
        osrsEquipment = hasCapability(settings, "osrs-equipment");
        gamblerInterface = hasCapability(settings, "gambler-ui");
        staffToolsInterface = hasCapability(settings, "staff-tools");
    }
    public boolean supportsGamblerInterface() { return gamblerInterface; }
    public boolean supportsStaffToolsInterface() { return staffToolsInterface; }

    private static boolean hasCapability(String settings, String capability) {
        return settings != null && (settings + "|").contains("|" + capability + "=1|");
    }

    public Channel getChannel() {
        return channel;
    }

    public ChannelFuture write(Message message) {
        if (channel != null && channel.isConnected() && message.isRaw()) {
            return channel.write(message);
        }
        return null;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public boolean isDisconnected() {
        if (channel == null) {
            return true;
        }
        return !channel.isOpen() || !channel.isConnected();
    }

    public int getDisplayMode() {
        return displayMode;
    }

    public void setDisplayMode(int mode) {
        this.displayMode = mode;
    }

    public void setInLobby(boolean inLobby) {
        this.inLobby = inLobby;
    }

    public boolean isInLobby() {
        return inLobby;
    }
}


