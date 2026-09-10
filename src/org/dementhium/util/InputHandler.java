package org.dementhium.util;

import org.dementhium.content.activity.impl.DuelActivity;
import org.dementhium.content.activity.impl.DuelActivity.State;
import org.dementhium.content.activity.impl.duel.Stakes;
import org.dementhium.content.skills.summoning.Summoning;
import org.dementhium.model.Item;
import org.dementhium.model.World;
import org.dementhium.model.player.Player;
import org.dementhium.model.player.TradeSession.TradeState;
import org.dementhium.net.ActionSender;

/**
 * @author 'Mystic Flow
 */
public class InputHandler {

    public static void handleStringInput(Player player, String string) {
        int inputId = (Integer) player.getAttribute("inputId", -1);
        if (inputId > -1) {
            switch (inputId) {
                case 0: //enter clan name
                    String clan = string.replaceAll("_", " ");
                    ActionSender.sendString(player, clan, 590, 22);
                    World.getWorld().getClanManager().createClan(player, clan);
                    break;
                case 1:
                    if (player.getAttribute("noteSlot", -1) == -1)
                        player.getNotes().addNote(string);
                    else {
                        player.getNotes().editNote(string, player.getAttribute("noteSlot", -1));
                        player.removeAttribute("noteSlot");
                    }
                    break;
            }
        }
        resetInput(player);
    }

    public static void resetInput(Player player) {
        player.removeAttribute("inputId");
    }

    public static void requestStringInput(Player player, int inputId, String question) {
        player.setAttribute("inputId", inputId);
        ActionSender.sendClientScript(player, 109, new Object[]{question}, "s");
    }

    public static void requestLongStringInput(Player player, int inputId, String question) {
        player.setAttribute("inputId", inputId);
        ActionSender.sendClientScript(player, 110, new Object[]{question}, "s");
    }

    public static void requestIntegerInput(Player player, int inputId, String question) {
        player.setAttribute("inputId", inputId);
        ActionSender.sendClientScript(player, 108, new Object[]{question}, "s");
    }

    public static void handleIntegerInput(Player player, int value) {
        int inputId = (Integer) player.getAttribute("inputId", -1);

        if (inputId > -1) {
            switch (inputId) {
                case 40: org.dementhium.content.minigames.gambler.GamblerSession.input(player,value); break;
                case 1:
                    int tradeInventSlot = (Integer) player.getAttribute("slotId", -1);
                    if (tradeInventSlot > -1) {
                        if (player.getTradeSession() != null && player.getTradeSession().getState() == TradeState.STATE_ONE) {
                            player.getTradeSession().offerItem(player, tradeInventSlot, value);
                            player.setAttribute("slotId", 0);
                        } else if (player.getTradePartner() != null && player.getTradePartner().getTradeSession().getState() == TradeState.STATE_ONE) {
                            player.getTradePartner().getTradeSession().offerItem(player, tradeInventSlot, value);
                            player.setAttribute("slotId", 0);
                        }
                    }
                    break;
                case 2:
                    int tradeSlot = (Integer) player.getAttribute("slotId", -1);
                    if (tradeSlot > -1) {
                        if (player.getTradeSession() != null && player.getTradeSession().getState() == TradeState.STATE_ONE) {
                            player.getTradeSession().removeItem(player, tradeSlot, value);
                            player.setAttribute("slotId", 0);
                        } else if (player.getTradePartner() != null && player.getTradePartner().getTradeSession().getState() == TradeState.STATE_ONE) {
                            player.getTradePartner().getTradeSession().removeItem(player, tradeSlot, value);
                            player.setAttribute("slotId", 0);
                        }
                    }
                    break;
                case 3:
                    int bankSlot = (Integer) player.getAttribute("slotId", -1);
                    if (bankSlot > -1) {
                        player.getSettings().setLastXAmount(value);
                        ActionSender.sendConfig(player, 1249, value);
                        player.getBank().removeItem(bankSlot, value);
                    }
                    break;
                case 4:
                    int bankInventorySlot = (Integer) player.getAttribute("slotId", -1);
                    if (bankInventorySlot > -1) {
                        player.getSettings().setLastXAmount(value);
                        ActionSender.sendConfig(player, 1249, value);
                        player.getBank().addItem(bankInventorySlot, value);
                    }
                    break;
                case 5:
                    org.dementhium.event.impl.interfaces.DuelArenaListener.input(player,value,true);break;
                case 6:
                    org.dementhium.event.impl.interfaces.DuelArenaListener.input(player,value,false);break;                case 7:
                    boolean inventoryAction = player.getAttribute("inventoryAction", false);
                    int slot = player.getAttribute("slotId", -1);
                    int itemId = player.getAttribute("itemPriceCheckId", -1);
                    if (inventoryAction) {
                        player.getPriceCheck().checkPrice(itemId, slot, value);
                    } else {
                        player.getPriceCheck().remove(itemId, slot, value);
                    }
                	break;
                case 8: //summoning pouch creation enter-x
                    int slotId = player.getAttribute("slotId", -1);
                    int pouchId = player.getAttribute("inputXItemId", -1);
                    Summoning.createPouch(player, pouchId, slotId, value);
                	break;
            }
        }
        player.setAttribute("slotId", 0);
        player.setAttribute("inputId", 0);
        player.setAttribute("itemX", 0);
        player.setAttribute("inputXItemId", 0);
    }

}
