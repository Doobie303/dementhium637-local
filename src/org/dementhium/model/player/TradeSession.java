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
        ActionSender.sendString(p, "<col=00FFFF>Trading with:<br><col=00FFFF>" + Misc.formatPlayerNameForDisplay(p.equals(trader) ? partner.getDisplayName() : trader.getDisplayName()), 334, 54);
        ActionSender.sendString(p, "Are you sure you want to make this trade?", 334, 34);
    }

    public void offerItem(Player pl, int slot, int amt) {
        if (currentState.equals(TradeState.STATE_ONE)) {
            if (pl.equals(trader)) {
                if (pl.getInventory().getContainer().get(slot) == null) {
                    return;
                }
                Item item = new Item(pl.getInventory().getContainer().get(slot).getId(), amt);
                if (item != null) {
                    if (!item.getDefinition().isTradeable() && pl.getRights() < 2 && partner.getRights() < 2) {
                        pl.sendMessage("You can't trade this item.");
                        return;
                    }
                    if (pl.getInventory().getContainer().getItemCount(item.getId()) < amt) {
                        if (ItemDefinition.forId(item.getId()).isNoted()
                                || ItemDefinition.forId(item.getId()).isStackable()) {
                            amt = pl.getInventory().lookup(item.getId()).getAmount();
                        } else {
                            amt = pl.getInventory().getContainer().getItemCount(item.getId());
                        }
                        item.setAmount(amt);

                    }
                    if (0 >= amt) {
                        return;
                    }
                    if (traderItemsOffered.getFreeSlots() < amt && !pl.getInventory().getContainer().get(slot).getDefinition().isNoted() && !pl.getInventory().getContainer().get(slot).getDefinition().isStackable()) {
                        item.setAmount(traderItemsOffered.getFreeSlots());
                    }
                    traderItemsOffered.add(item);
                    pl.getInventory().getContainer().remove(new Item(pl.getInventory().getContainer().get(slot).getId(), amt));
                    pl.getInventory().refresh();
                    resetAccept();
                }
            } else if (pl.equals(partner)) {
                Item inventoryItem = pl.getInventory().getContainer().get(slot);
                Item item = inventoryItem != null ? new Item(inventoryItem.getId(), amt) : null;
                if (item != null) {
                    if (!item.getDefinition().isTradeable() && pl.getRights() < 2 && trader.getRights() < 2) {
                        pl.sendMessage("You can't trade this item.");
                        return;
                    }
                    if (pl.getInventory().getContainer().getItemCount(item.getId()) < amt) {
                        if (ItemDefinition.forId(item.getId()).isNoted()
                                || ItemDefinition.forId(item.getId()).isStackable()) {
                            amt = pl.getInventory().lookup(item.getId()).getAmount();
                        } else {
                            amt = pl.getInventory().getContainer().getItemCount(item.getId());
                        }
                        item.setAmount(amt);

                    }
                    if (0 >= amt) {
                        return;
                    }
                    if (partnerItemsOffered.getFreeSlots() < amt && !pl.getInventory().getContainer().get(slot).getDefinition().isNoted() && !pl.getInventory().getContainer().get(slot).getDefinition().isStackable()) {
                        item.setAmount(partnerItemsOffered.getFreeSlots());
                    }
                    partnerItemsOffered.add(item);
                    pl.getInventory().getContainer().remove(item);
                    pl.getInventory().refresh();
                    resetAccept();
                }
            }
            refreshScreen();
        }
    }

    public void removeItem(Player pl, int slot, int amt) {
        if (currentState.equals(TradeState.STATE_ONE)) {
            if (pl.equals(trader)) {
            	if (traderItemsOffered.get(slot) == null) {
            		return;
            	}
                Item item = new Item(traderItemsOffered.get(slot).getId(), amt);
                if (item != null) {
                    if (traderItemsOffered.getItemCount(item.getId()) < amt) {
                        if (ItemDefinition.forId(item.getId()).isNoted()
                                || ItemDefinition.forId(item.getId()).isStackable()) {
                            amt = traderItemsOffered.lookup(item.getId()).getAmount();
                        } else {
                            amt = traderItemsOffered.getItemCount(item.getId());
                        }
                        item.setAmount(amt);

                    }
                    if (0 >= amt) {
                        return;
                    }
                    if (pl.getInventory().getFreeSlots() < amt && !traderItemsOffered.get(slot).getDefinition().isNoted() && !traderItemsOffered.get(slot).getDefinition().isStackable()) {
                        item.setAmount(pl.getInventory().getFreeSlots());
                    }
                    trader.getInventory().getContainer().add(new Item(traderItemsOffered.get(slot).getId(), item.getAmount()));
                    trader.getInventory().refresh();
                    traderItemsOffered.remove(item);
                    ActionSender.sendTradeModified(partner, true, slot);
                    resetAccept();
                }
            } else if (pl.equals(partner)) {
            	if (partnerItemsOffered.get(slot) == null) {
            		return;
            	}
                Item item = new Item(partnerItemsOffered.get(slot).getId(), amt);
                if (item != null) {
                    if (partnerItemsOffered.getItemCount(item.getId()) < amt) {
                        if (ItemDefinition.forId(item.getId()).isNoted()
                                || ItemDefinition.forId(item.getId()).isStackable()) {
                            amt = partnerItemsOffered.lookup(item.getId()).getAmount();
                        } else {
                            amt = partnerItemsOffered.getItemCount(item.getId());
                        }
                        item.setAmount(amt);

                    }
                    if (0 >= amt) {
                        return;
                    }
                    if (pl.getInventory().getFreeSlots() < amt && !partnerItemsOffered.get(slot).getDefinition().isNoted() && !partnerItemsOffered.get(slot).getDefinition().isStackable()) {
                        item.setAmount(pl.getInventory().getFreeSlots());
                    }
                    partner.getInventory().getContainer().add(new Item(partnerItemsOffered.get(slot).getId(), item.getAmount()));
                    partner.getInventory().refresh();
                    partnerItemsOffered.remove(item);
                    ActionSender.sendTradeModified(trader, true, slot);
                    resetAccept();
                }
            }
            refreshScreen();
        }
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
        if (!trader.getInventory().getContainer().hasSpaceFor(partnerItemsOffered)) {
            trader.getMask().setFacePosition(null, 1, 1);
            if (trader.getMask().getInteractingEntity() != null) {
                trader.resetTurnTo();
            }
            partner.getMask().setFacePosition(null, 1, 1);
            if (partner.getMask().getInteractingEntity() != null) {
                partner.resetTurnTo();
            }
        	ActionSender.sendMessage(partner, "The other player does not have enough space in their inventory.");
            ActionSender.sendMessage(trader, "You do not have enough space in your inventory.");
            tradeFailed(null);
            return false;
        } else if (!partner.getInventory().getContainer().hasSpaceFor(traderItemsOffered)) {
            trader.getMask().setFacePosition(null, 1, 1);
            if (trader.getMask().getInteractingEntity() != null) {
                trader.resetTurnTo();
            }
            partner.getMask().setFacePosition(null, 1, 1);
            if (partner.getMask().getInteractingEntity() != null) {
                partner.resetTurnTo();
            }
        	ActionSender.sendMessage(trader, "The other player does not have enough space in their inventory.");
            ActionSender.sendMessage(partner, "You do not have enough space in your inventory.");
            tradeFailed(null);
            return false;
        }
        boolean stopTrade1 = false;
        boolean stopTrade2 = false;
        for (Item item : partnerItemsOffered.toArray()) {
        	if (item != null) {
            	int amount = item.getAmount();
            	int playerAmount = trader.getInventory().getContainer().getNumberOf(item);
    			if ((amount + playerAmount) < 0) {
    				stopTrade1 = true;
    			}
        	}
        }
        for (Item item : traderItemsOffered.toArray()) {
        	if (item != null) {
            	int amount = item.getAmount();
            	int playerAmount = partner.getInventory().getContainer().getNumberOf(item);
    			if ((amount + playerAmount) < 0) {
    				stopTrade2 = true;
    			}
        	}
        }
        if (stopTrade1) {
            trader.getMask().setFacePosition(null, 1, 1);
            if (trader.getMask().getInteractingEntity() != null) {
                trader.resetTurnTo();
            }
            partner.getMask().setFacePosition(null, 1, 1);
            if (partner.getMask().getInteractingEntity() != null) {
                partner.resetTurnTo();
            }
        	ActionSender.sendMessage(partner, "The other player does not have enough space in their inventory.");
            ActionSender.sendMessage(trader, "You do not have enough space in your inventory.");
            tradeFailed(null);
            return false;
        }
        if (stopTrade2) {
            trader.getMask().setFacePosition(null, 1, 1);
            if (trader.getMask().getInteractingEntity() != null) {
                trader.resetTurnTo();
            }
            partner.getMask().setFacePosition(null, 1, 1);
            if (partner.getMask().getInteractingEntity() != null) {
                partner.resetTurnTo();
            }
        	ActionSender.sendMessage(trader, "The other player does not have enough space in their inventory.");
            ActionSender.sendMessage(partner, "You do not have enough space in your inventory.");
            tradeFailed(null);
            return false;
        }
    	return true;
    }
    
    private boolean giveItems() {
        if (!trader.getInventory().getContainer().hasSpaceFor(partnerItemsOffered)) {
            ActionSender.sendMessage(partner, "The other player does not have enough space in their inventory.");
            ActionSender.sendMessage(trader, "You do not have enough space in your inventory.");
            tradeFailed(null);
            return false;
        } else if (!partner.getInventory().getContainer().hasSpaceFor(traderItemsOffered)) {
            ActionSender.sendMessage(trader, "The other player does not have enough space in their inventory.");
            ActionSender.sendMessage(partner, "You do not have enough space in your inventory.");
            tradeFailed(null);
            return false;
        }
        for (Item itemAtIndex : traderItemsOffered.toArray()) {
            if (itemAtIndex != null) {
                partner.getInventory().addDropable(new Item(itemAtIndex.getId(), itemAtIndex.getAmount()));
            }
        }
        for (Item itemAtIndex : partnerItemsOffered.toArray()) {
            if (itemAtIndex != null) {
                trader.getInventory().addDropable(new Item(itemAtIndex.getId(), itemAtIndex.getAmount()));
            }
        }
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
