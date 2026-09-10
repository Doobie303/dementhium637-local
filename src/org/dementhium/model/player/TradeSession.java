package org.dementhium.model.player;


import org.dementhium.model.Container;
import org.dementhium.model.Item;
import org.dementhium.model.World;
import org.dementhium.model.definition.ItemDefinition;
import org.dementhium.net.ActionSender;
import org.dementhium.util.Logger;
import org.dementhium.util.Misc;

/**
 * Represents a trade session
 *
 * @author Stephen
 */
public class TradeSession {

    private final Player trader, partner;
    private TradeState currentState = TradeState.STATE_ONE;
    private Container traderItemsOffered = new Container(28, false);
    private Container partnerItemsOffered = new Container(28, false);
    private boolean traderDidAccept, partnerDidAccept;
    private boolean closed;

    /*
      * Some info for the future,
      * 44 = wealth transfer
      * 43 = left limit
      * 45 = right limit
      */


    public TradeSession(Player trader, Player partner) {
        this.trader = trader;
        this.partner = partner;
        trader.setAttribute("didRequestTrade", Boolean.FALSE);
        partner.setAttribute("didRequestTrade", Boolean.FALSE);
    }

    public void start() {
        refreshScreen();
        openFirstTradeScreen(trader);
        openFirstTradeScreen(partner);
    }

    public Player getPartner(Player p) {
    	if (p == partner)
    		return trader;
    	else
    		return partner;
    }

    public void openFirstTradeScreen(Player p) {
        ActionSender.sendTradeOptions(p);
        ActionSender.sendInterface(p, 335);
        ActionSender.sendInventoryInterface(p, 336);
        ActionSender.sendItems(p, 90, traderItemsOffered, false);
        ActionSender.sendItems(p, 90, partnerItemsOffered, true);
        ActionSender.sendString(p, "", 335, 37);
        String name = p.equals(trader) ? partner.getDisplayName() : trader.getDisplayName();
        ActionSender.sendString(p, "Trading with: " + name, 335, 15);
        ActionSender.sendString(p, Misc.formatPlayerNameForDisplay(name), 335, 22);
        refreshScreen();
    }




        //ActionSender.sendString(p, "Test: ", 335, 37);
        //ActionSender.sendString(p, "itemLoanName ", 335, 53); (Right side)
        //ActionSender.sendString(p, "itemLoanName ", 335, 57); (Left side)
    

    public void openSecondTradeScreen(Player p) {
        currentState = TradeState.STATE_TWO;
        partnerDidAccept = false;
        traderDidAccept = false;
        ActionSender.sendInterface(p, 334);
        // Component 53 already supplies "Trading with:"; component 54 is the name only.
        ActionSender.sendString(p, Misc.formatPlayerNameForDisplay(p.equals(trader) ? partner.getDisplayName() : trader.getDisplayName()), 334, 54);
        ActionSender.sendString(p, "Are you sure you want to make this trade?", 334, 34);
    }

    public void offerItem(Player pl, int slot, int amt) {
        if (closed || currentState != TradeState.STATE_ONE || !isParticipant(pl) || amt <= 0) return;
        Item selected = pl.getInventory().get(slot);
        if (selected == null) return;
        if (DegradingHandler.isDegradedForTrade(selected)) {
            pl.sendMessage("You can't trade degraded items.");
            return;
        }
        if (!selected.getDefinition().isTradeable() && pl.getRights() < 2 && getPartner(pl).getRights() < 2) {
            pl.sendMessage("You can't trade this item.");
            return;
        }
        if (!transfer(pl.getInventory().getContainer(), getPlayerItemsOffered(pl), slot, amt, true)) {
            pl.sendMessage("There is not enough space in the trade offer.");
            return;
        }
        pl.getInventory().refresh();
        resetAccept();
        refreshScreen();
    }

    public void removeItem(Player pl, int slot, int amt) {
        if (closed || currentState != TradeState.STATE_ONE || !isParticipant(pl) || amt <= 0) return;
        if (!transfer(getPlayerItemsOffered(pl), pl.getInventory().getContainer(), slot, amt, false)) {
            pl.sendMessage("Not enough space in your inventory.");
            return;
        }
        pl.getInventory().refresh();
        ActionSender.sendTradeModified(getPartner(pl), true, slot);
        resetAccept();
        refreshScreen();
    }

    private boolean isParticipant(Player player) {
        return player == trader || player == partner;
    }

    /** Prepare both images before publishing either; selected slot is consumed first. */
    private static boolean transfer(Container source, Container destination, int slot, int requested, boolean offering) {
        Item selected = source.get(slot);
        if (selected == null || requested <= 0) return false;
        Container remaining = source.deepCopy();
        Container result = destination.deepCopy();
        Container moved = new Container(source.getSize(), false);
        boolean stack = selected.getDefinition().isStackable() || selected.getDefinition().isNoted();
        int limit = requested;
        if (!stack) limit = Math.min(limit, destination.getFreeSlots());
        else {
            Item existing = destination.lookup(selected.getId());
            if (existing != null) limit = (int)Math.min((long)limit, Integer.MAX_VALUE - (long)existing.getAmount());
            else if (destination.getFreeSlots() == 0) return false;
        }
        int transferred = 0;
        for (int n = 0; n <= source.getSize() && transferred < limit; n++) {
            int index = n == 0 ? slot : n - 1;
            if (n > 0 && index == slot) continue;
            Item item = remaining.get(index);
            if (item == null || item.getId() != selected.getId() || item.getHealth() != selected.getHealth()
                    || item.getAmount() <= 0 || (offering && DegradingHandler.isDegradedForTrade(item))) continue;
            int count = Math.min(limit - transferred, item.getAmount());
            Item portion = new Item(item); portion.setAmount(count);
            if (!moved.forceAdd(portion)) return false;
            if (count == item.getAmount()) remaining.set(index, null);
            else { Item rest = new Item(item); rest.setAmount(item.getAmount() - count); remaining.set(index, rest); }
            transferred += count;
        }
        if (transferred == 0 || !result.tryAddAll(moved)) return false;
        source.replaceWith(remaining);
        destination.replaceWith(result);
        return true;
    }

    private void refreshScreen() {
        ActionSender.sendItems(trader, 90, traderItemsOffered, false);
        ActionSender.sendItems(partner, 90, partnerItemsOffered, false);
        ActionSender.sendItems(trader, 90, partnerItemsOffered, true);
        ActionSender.sendItems(partner, 90, traderItemsOffered, true);
        String name = trader.getDisplayName();
        ActionSender.sendString(partner, Misc.formatPlayerNameForDisplay(name), 335, 22);
        String name1 = partner.getDisplayName();
        ActionSender.sendString(trader, Misc.formatPlayerNameForDisplay(name1), 335, 22);
        ActionSender.sendString(trader, 335, 21, " has " + partner.getInventory().getFreeSlots() + " free inventory slots.");
        ActionSender.sendString(partner, 335, 21, " has " + trader.getInventory().getFreeSlots() + " free inventory slots.");
        ActionSender.sendBConfig(trader, 729, getTradersItemsValue());
        ActionSender.sendBConfig(trader, 697, getPartnersItemsValue());
        ActionSender.sendBConfig(partner, 729, getPartnersItemsValue());
        ActionSender.sendBConfig(partner, 697, getTradersItemsValue());
    }

    private int getTradersItemsValue() {
        int initialPrice = 0;
        int itemPrice = 0;
        boolean ignoreValue = false;
        for (Item item : traderItemsOffered.toArray()) {
            if (item != null) {
				initialPrice += item.getDefinition().getExchangePrice() * item.getAmount();
				itemPrice = item.getDefinition().getExchangePrice() == 0 ? 1 : item.getDefinition().getExchangePrice();
				double itemAmount = item.getAmount();
				double maxAmount = (Integer.MAX_VALUE / itemPrice);
				if (itemAmount > maxAmount) {
					ignoreValue = true;
				}
            }
        }
        if (ignoreValue)
        	initialPrice = -1; //returns Unknown as price.	
        return initialPrice;
    }

    private int getPartnersItemsValue() {
        int initialPrice = 0;
        int itemPrice = 0;
        boolean ignoreValue = false;
        for (Item item : partnerItemsOffered.toArray()) {
        	if (item != null) {
    			initialPrice += item.getDefinition().getExchangePrice() * item.getAmount();
    			itemPrice = item.getDefinition().getExchangePrice() == 0 ? 1 : item.getDefinition().getExchangePrice();
				double itemAmount = item.getAmount();
				double maxAmount = (Integer.MAX_VALUE / itemPrice);
				if (itemAmount > maxAmount || initialPrice < 0) {
					ignoreValue = true;
				}
        	}
        }
        if (ignoreValue == true)
        	initialPrice = -1; //returns Unknown as price.	
        return initialPrice;
    }

    @SuppressWarnings("unused")
    private void flashSlot(Player player, int slot) {
        ActionSender.sendClientScript(player, 143, new Object[]{slot, 7, 4, player.equals(trader) ? 21954591 : 21954593}, "Iiii"); //Guess this wouldn't work for both screens.
    }

    public void acceptPressed(Player pl) {
        if (closed || !isParticipant(pl)) return;
        if (!traderDidAccept && pl.equals(trader)) {
            traderDidAccept = true;
        } else if (!partnerDidAccept && pl.equals(partner)) {
            partnerDidAccept = true;
        }
        switch (currentState) {
            case STATE_ONE:
                if (pl.equals(trader)) {
                    if (partnerDidAccept && traderDidAccept) {
                    	if (continueTrade() == false)
                    		return;
                        openSecondTradeScreen(trader);
                        openSecondTradeScreen(partner);
                    } else {
                        ActionSender.sendString(trader, "Waiting for other player...", 335, 37);
                        ActionSender.sendString(partner, "The other player has accepted", 335, 37);
                    }
                } else if (pl.equals(partner)) {
                    if (partnerDidAccept && traderDidAccept) {
                    	if (continueTrade() == false)
                    		return;
                        openSecondTradeScreen(trader);
                        openSecondTradeScreen(partner);
                    } else {
                        ActionSender.sendString(partner, "Waiting for other player...", 335, 37);
                        ActionSender.sendString(trader, "The other player has accepted", 335, 37);
                    }
                }
                break;

            case STATE_TWO:
                if (pl.equals(trader)) {
                    if (partnerDidAccept && traderDidAccept) {
                        trader.getMask().setFacePosition(null, 1, 1);
                        if (trader.getMask().getInteractingEntity() != null) {
                            trader.resetTurnTo();
                        }
                        partner.getMask().setFacePosition(null, 1, 1);
                        if (partner.getMask().getInteractingEntity() != null) {
                            partner.resetTurnTo();
                        }
                    	if (giveItems() == false) { //second check, not really needed but for safety
                        	return;
                        }
                        ActionSender.sendMessage(trader, "Accepted trade.");
                        ActionSender.sendMessage(partner, "Accepted trade.");
                    } else {
                        ActionSender.sendString(trader, "Waiting for other player...", 334, 34);
                        ActionSender.sendString(partner, "The other player has accepted", 334, 34);
                    }
                } else if (pl.equals(partner)) {
                    if (partnerDidAccept && traderDidAccept) {
                        trader.getMask().setFacePosition(null, 1, 1);
                        if (trader.getMask().getInteractingEntity() != null) {
                            trader.resetTurnTo();
                        }
                        partner.getMask().setFacePosition(null, 1, 1);
                        if (partner.getMask().getInteractingEntity() != null) {
                            partner.resetTurnTo();
                        }
                    	if (giveItems() == false) { //second check, not really needed but for safety
                        	return;
                        }
                        ActionSender.sendMessage(partner, "Accepted trade.");
                        ActionSender.sendMessage(trader, "Accepted trade.");
                    } else {
                        ActionSender.sendString(partner, "Waiting for other player...", 334, 34);
                        ActionSender.sendString(trader, "The other player has accepted", 334, 34);
                    }
                }
                break;
        }

    }

    public void tradeFailed(Player playerWhoEndedTrade) {
        if (closed) return;
        closed = true;
        trader.getInventory().addAllDropable(traderItemsOffered);
        partner.getInventory().addAllDropable(partnerItemsOffered);
        if (playerWhoEndedTrade != null/* && currentState.equals(TradeState.STATE_TWO)*/) {
        	//getTradePartnet() method doesn't work
        	//playerWhoEndedTrade.getTradePartner().sendMessage("Declined trade.");
        	if (trader == playerWhoEndedTrade)
            	partner.sendMessage("<col=FF0000>Other player declined trade!");
        	else if (partner == playerWhoEndedTrade)
            	trader.sendMessage("<col=FF0000>Other player declined trade!");
        	//playerWhoEndedTrade.getTradeSession().getPartner().getPlayer().sendMessage("Declined trade.");
        }
        endSession();
        trader.getInventory().refresh();
        partner.getInventory().refresh();
        trader.stopAll();
        partner.stopAll();
    }

    public void endSession() {
        closed = true;
        traderItemsOffered = partnerItemsOffered = null;
        trader.setTradeSession(null);
        partner.setTradePartner(null); 
        //can't use closeAll() here, as that would loop?
        ActionSender.sendCloseInterface(trader);
        ActionSender.sendCloseInterface(partner);
        ActionSender.closeInventoryInterface(trader);
        ActionSender.closeInventoryInterface(partner);
    }

    private boolean continueTrade() {
        if (!trader.getInventory().getContainer().hasSpaceFor(partnerItemsOffered)
                || !partner.getInventory().getContainer().hasSpaceFor(traderItemsOffered)) {
            ActionSender.sendMessage(trader, "There is not enough inventory space to complete this trade.");
            ActionSender.sendMessage(partner, "There is not enough inventory space to complete this trade.");
            resetAccept();
            return false;
        }
        return true;
    }

    private boolean giveItems() {
        Container traderResult = trader.getInventory().getContainer().deepCopy();
        Container partnerResult = partner.getInventory().getContainer().deepCopy();
        if (!traderResult.tryAddAll(partnerItemsOffered) || !partnerResult.tryAddAll(traderItemsOffered)) {
            ActionSender.sendMessage(trader, "There is not enough inventory space to complete this trade.");
            ActionSender.sendMessage(partner, "There is not enough inventory space to complete this trade.");
            openFirstTradeScreen(trader);
            openFirstTradeScreen(partner);
            currentState = TradeState.STATE_ONE;
            resetAccept();
            return false;
        }
        trader.getInventory().getContainer().replaceWith(traderResult);
        partner.getInventory().getContainer().replaceWith(partnerResult);
        // Logging expects compact offers. It cannot veto or repeat a completed exchange.
        traderItemsOffered.shift(); partnerItemsOffered.shift();
        Logger.writeTradeLog(trader, partner, traderItemsOffered, partnerItemsOffered);
        endSession();
        partner.getInventory().refresh();
        trader.getInventory().refresh();
        return true;
    }

    public Container getPlayerItemsOffered(Player p) {
        return (p.equals(trader) ? traderItemsOffered : partnerItemsOffered);
    }

    public enum TradeState {

        STATE_ONE,
        STATE_TWO
    }

    public void resetAccept() {
        partnerDidAccept = traderDidAccept = false;
        switch (currentState) {
            case STATE_ONE:
                ActionSender.sendString(partner, "", 335, 37);
                ActionSender.sendString(trader, "", 335, 37);
                break;
            case STATE_TWO:
                ActionSender.sendString(partner, "", 334, 34);
                ActionSender.sendString(trader, "", 334, 34);
                break;
        }
    }

    public TradeState getState() {
        return currentState;
    }
}
