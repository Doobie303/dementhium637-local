package org.dementhium.net.packethandlers;

import org.dementhium.model.Item;
import org.dementhium.model.player.Bank;
import org.dementhium.model.player.Inventory;
import org.dementhium.model.player.Player;
import org.dementhium.net.PacketHandler;
import org.dementhium.net.message.Message;

/**
 * @author 'Mystic Flow <Steven@rune-server.org>
 */
@SuppressWarnings("unused")
public class SwitchItemHandler extends PacketHandler {

    public static final int SWITCH_ITEMS = 10;

    @Override
    public void handlePacket(Player player, Message packet) {
        switch (packet.getOpcode()) {
            case SWITCH_ITEMS:
                switchItems(player, packet);
                break;
        }
    }

    public void switchItems(Player player, Message packet) {
        int fromInterfaceHash = packet.readInt();
        int fromInterfaceId = fromInterfaceHash >> 16;
        int toItemId = packet.readShortA();
        int fromItemId = packet.readShort();
        int toInterfaceHash = packet.readLEInt();
        int toInterfaceId = toInterfaceHash >> 16;
        int tabId = (toInterfaceHash & 0xFFFF);
        int fromId = packet.readLEShortA();
        int toId = packet.readLEShort();
        int tabIndex = Bank.getArrayIndex(tabId);
        int fromTab;
        switch (fromInterfaceId) {
            case 762:
                if (fromInterfaceHash != ((762 << 16) | 93) || toInterfaceId != 762
                        || !player.getBank().matchesItem(fromId, fromItemId, false)) return;
                if (tabId == 93) {
                    if (!player.getBank().matchesItem(toId, toItemId, false)) return;
                    player.getBank().moveItem(fromId, toId, isInserting(player));
                } else if (tabIndex >= 2 && tabIndex <= 10) {
                    player.getBank().moveToTab(fromId, tabIndex);
                }
                break;
            case 149:
            case 763:
                if ((fromInterfaceId == 763 || toInterfaceId == 763) && !player.getBank().isOpen()) return;
                if ((fromInterfaceHash & 0xFFFF) != 0 || (toInterfaceHash & 0xFFFF) != 0) return;
                switch (toInterfaceId) {
                    case 149:
                    case 763:
                        if (fromInterfaceId == 149 && toInterfaceId == 149)
                            toId -= 28;
                        if (fromId < 0 || fromId >= Inventory.SIZE || player.getInventory().getContainer().get(fromId) == null)
                            return;
                        if (toId < 0 || toId >= Inventory.SIZE)
                            return;
                        Item toSlotItem = player.getInventory().getContainer().get(toId);
                        if (player.getInventory().get(fromId).getId() != fromItemId
                                || (toSlotItem == null ? toItemId != -1 && toItemId != 65535 : toSlotItem.getId() != toItemId)) return;
                        player.getInventory().getContainer().set(toId, player.getInventory().getContainer().get(fromId));
                        player.getInventory().getContainer().set(fromId, toSlotItem);
                        player.getInventory().refresh();
                        break;
                }
                break;
        }
    }

    private boolean isInserting(Player player) {
        return (Boolean) player.getAttribute("inserting", Boolean.FALSE);
    }
}
