package org.dementhium.util;

import org.dementhium.model.Container;
import org.dementhium.model.Item;
import org.dementhium.model.definition.ItemDefinition;
import org.dementhium.model.player.Player;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * @author Lumby <lumbyjr@hotmail.com>
 */
public class Logger {

	
	/*
	 * All the names in the log files are username, not displaynames (as those are changed anyways, too hard to track).
	 */
	
    public static void writeTradeLog(Player trader1, Player trader2, Container traderItems1, Container traderItems2) {
        try {
        	if (traderItems1.size() == 0 && traderItems2.size() == 0)
        		return;
        	if (trader1.getRights() >= 2 && trader2.getRights() >= 2)
        		return;
        	boolean negative = false;
        	int price = 0;
            for (int i = 0; i < traderItems1.size(); i++) {
            	if (wentOverMaxPrice(traderItems1.get(i).getId(), traderItems1.get(i).getAmount()))
            		negative = true;
            	price += getPrice(traderItems1.get(i).getId(), traderItems1.get(i).getAmount());
            }
        	boolean negative2 = false;
        	int price2 = 0;
            for (int i = 0; i < traderItems2.size(); i++) {
            	if (wentOverMaxPrice(traderItems2.get(i).getId(), traderItems2.get(i).getAmount()))
            		negative2 = true;
            	price2 += getPrice(traderItems2.get(i).getId(), traderItems2.get(i).getAmount());
            }
            if (price < 100000 && !negative && price2 < 100000 && !negative2) //those low values are not important enough to log
            	return;
            BufferedWriter bw = new BufferedWriter(new FileWriter("./data/logs/trade_logs.txt", true));
            bw.write("\n["+DateFormat.getDateTimeInstance().format(new Date())+" "+Calendar.getInstance().getTimeZone().getDisplayName()+"]");
            if (traderItems1.size() > 0) {
            	bw.write("\n "+Misc.formatPlayerNameForDisplay(trader1.getUsername())+" gave "+Misc.formatPlayerNameForDisplay(trader2.getUsername())+" in trade:");
                for (int i = 0; i < traderItems1.size(); i++) {
                    bw.write("\n"+(i > 0 ? "," : "")+" ("+(i+1)+"): "+traderItems1.get(i).getAmount()+" x "+traderItems1.get(i).getDefinition().getName()+" ("+traderItems1.get(i).getDefinition().getId()+")");
                }
            }
            if (traderItems2.size() > 0) {
            	if (traderItems1.size() > 0)
            		bw.write("\n and "+Misc.formatPlayerNameForDisplay(trader1.getUsername())+" received from "+Misc.formatPlayerNameForDisplay(trader2.getUsername())+" in trade:");
            	else
            		bw.write("\n "+Misc.formatPlayerNameForDisplay(trader2.getUsername())+" gave "+Misc.formatPlayerNameForDisplay(trader1.getUsername())+" in trade:");
                for (int i = 0; i < traderItems2.size(); i++) {
                    bw.write("\n"+(i > 0 ? "," : "")+" ("+(i+1)+"): "+traderItems2.get(i).getAmount()+" x "+traderItems2.get(i).getDefinition().getName()+" ("+traderItems2.get(i).getDefinition().getId()+")");
                }
                if (traderItems1.size() == 0)
                	bw.write("\n and got nothing in return.");
            } else
            	bw.write("\n and got nothing in return.");
            bw.write("\n");
            bw.write("\n");
        	bw.newLine();
            bw.flush();
            bw.close();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
    
    public static void writeStakeLog(Player winner, Player loser, Container wonItems, Container riskedItems) {
        try {
        	if (wonItems.size() == 0 && riskedItems.size() == 0)
        		return;
        	if (winner.getRights() >= 2 && loser.getRights() >= 2)
        		return;
        	boolean negative = false;
        	int price = 0;
            for (int i = 0; i < wonItems.size(); i++) {
            	if (wentOverMaxPrice(wonItems.get(i).getId(), wonItems.get(i).getAmount()))
            		negative = true;
            	price += getPrice(wonItems.get(i).getId(), wonItems.get(i).getAmount());
            }
        	boolean negative2 = false;
        	int price2 = 0;
            for (int i = 0; i < riskedItems.size(); i++) {
            	if (wentOverMaxPrice(riskedItems.get(i).getId(), riskedItems.get(i).getAmount()))
            		negative2 = true;
            	price2 += getPrice(riskedItems.get(i).getId(), riskedItems.get(i).getAmount());
            }
            if (price < 100000 && !negative && price2 < 100000 && !negative2) //those low values are not important enough to log
            	return;
            BufferedWriter bw = new BufferedWriter(new FileWriter("./data/logs/stake_logs.txt", true));
            bw.write("\n["+DateFormat.getDateTimeInstance().format(new Date())+" "+Calendar.getInstance().getTimeZone().getDisplayName()+"]");
            if (wonItems.size() > 0) {
            	bw.write("\n "+Misc.formatPlayerNameForDisplay(winner.getUsername())+" won from "+Misc.formatPlayerNameForDisplay(loser.getUsername())+" in a stake:");
                for (int i = 0; i < wonItems.size(); i++) {
                    bw.write("\n"+(i > 0 ? "," : "")+" ("+(i+1)+"): "+wonItems.get(i).getAmount()+" x "+wonItems.get(i).getDefinition().getName()+" ("+wonItems.get(i).getDefinition().getId()+")");
                }
            }
            if (riskedItems.size() > 0) {
                if (wonItems.size() > 0)
                	bw.write("\n while "+Misc.formatPlayerNameForDisplay(winner.getUsername())+" risked during the stake:");
                else
                	bw.write("\n "+Misc.formatPlayerNameForDisplay(winner.getUsername())+" won a stake from "+Misc.formatPlayerNameForDisplay(loser.getUsername())+" risking :");
                for (int i = 0; i < riskedItems.size(); i++) {
                    bw.write("\n"+(i > 0 ? "," : "")+" ("+(i+1)+"): "+riskedItems.get(i).getAmount()+" x "+riskedItems.get(i).getDefinition().getName()+" ("+riskedItems.get(i).getDefinition().getId()+")");
                }
                if (wonItems.size() == 0)
                	bw.write("\n while he won nothing because "+Misc.formatPlayerNameForDisplay(loser.getUsername())+" staked nothing.");
            } else
            	bw.write("\n while he himself risked nothing during the stake.");
            bw.write("\n");
            bw.write("\n");
        	bw.newLine();
            bw.flush();
            bw.close();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
    
    public static void writeDropLog(Player pickuper, Player dropper, Item droppedItem) {
        try {
        	if (droppedItem == null)
        		return;
        	if (dropper == null || pickuper.getUsername().equals(dropper.getUsername()))
        		return;
        	if (pickuper.getRights() >= 2 && dropper.getRights() >= 2)
        		return;
            if (getPrice(droppedItem.getId(), droppedItem.getAmount()) < 100000 && !wentOverMaxPrice(droppedItem.getId(), droppedItem.getAmount())) //those low values are not important enough to log
            	return;
        	String dropperText = " dropped by an unknown source.";
        	if (dropper != null)
        		dropperText = " dropped by "+Misc.formatPlayerNameForDisplay(dropper.getUsername())+".";
            BufferedWriter bw = new BufferedWriter(new FileWriter("./data/logs/drop_logs.txt", true));
            bw.write("\n["+DateFormat.getDateTimeInstance().format(new Date())+" "+Calendar.getInstance().getTimeZone().getDisplayName()+"]");
            bw.write("\n "+Misc.formatPlayerNameForDisplay(pickuper.getUsername())+" picked up "+droppedItem.getAmount()+" x "+droppedItem.getDefinition().getName()+" ("+droppedItem.getDefinition().getId()+")"+dropperText);
            bw.write("\n");
            bw.write("\n");
        	bw.newLine();
            bw.flush();
            bw.close();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
    
    public static void writeChristmasCrackerPullLog(Player puller, Player pulledOn, Item pullerReceive, Item pulledOnReceive) {
        try {
        	if (pullerReceive == null || pulledOnReceive == null)
        		return;
        	if (puller.getRights() >= 2 && pulledOn.getRights() >= 2)
        		return;
            BufferedWriter bw = new BufferedWriter(new FileWriter("./data/logs/christmas_cracker_logs.txt", true));
            bw.write("\n["+DateFormat.getDateTimeInstance().format(new Date())+" "+Calendar.getInstance().getTimeZone().getDisplayName()+"]");
            bw.write("\n "+Misc.formatPlayerNameForDisplay(puller.getUsername())+" pulled a Chrismas cracker on "+Misc.formatPlayerNameForDisplay(pulledOn.getUsername())+" and got "+pullerReceive.getAmount()+" x "+pullerReceive.getDefinition().getName()+" ("+pullerReceive.getDefinition().getId()+"), while the person he pulled the cracker on got "+pulledOnReceive.getAmount()+" x "+pulledOnReceive.getDefinition().getName()+" ("+pulledOnReceive.getDefinition().getId()+").");
            bw.write("\n");
            bw.write("\n");
        	bw.newLine();
            bw.flush();
            bw.close();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
    
	/**
	 * Saves a players chat.
	 * 
	 * @param player the player's chat to save.
	 * @param text the chat.
	 * @param messageType 0 = chat, 1 = pm, 2 = clan, 3 = yell
	 * @param receiver in case of pm, the person it was send to
	 */
	public static void writeChatLog(Player player, String text, int messageType, Player pmReceiver) {
		try {
			BufferedWriter bf = new BufferedWriter(new FileWriter(
					"data/logs/chat_logs.txt", true));
			String chatType = "";
			if (messageType == 1 && pmReceiver != null)
				chatType = " (Private Message to "+Misc.formatPlayerNameForDisplay(pmReceiver.getUsername())+")";
			if (messageType == 2 && player.getSettings().getCurrentClan().getOwner() != null)
				chatType = " (Clan Message in "+Misc.formatPlayerNameForDisplay(player.getSettings().getCurrentClan().getOwner())+"'s clan)";
			if (messageType == 3)
				chatType = " (Yell Message)";
			bf.write("["+DateFormat.getDateTimeInstance().format(new Date())
					+" "+Calendar.getInstance().getTimeZone().getDisplayName()+"]"+chatType+" "+Misc.formatPlayerNameForDisplay(player.getUsername())+": "
					+text);
			bf.newLine();
			bf.flush();
			bf.close();
		} catch (IOException ignored) {
		}
	}
	
	public static boolean wentOverMaxPrice(int itemId, int amount) {
    	ItemDefinition def = ItemDefinition.forId(itemId);
		int itemPrice = 0;
		int price = 1;
		if (def.getStorePrice() > def.getExchangePrice()) {
			itemPrice = def.getStorePrice() * amount;
			price = def.getStorePrice();
		} else {
			itemPrice = def.getExchangePrice() * amount;
			price =  def.getExchangePrice();
		}
		double itemAmount = amount;
		double maxAmount = (Integer.MAX_VALUE / price);
		if (itemAmount > maxAmount || itemPrice < 0) {
			return true;
		}
		return false;
	}
	
	public static int getPrice(int itemId, int amount) {
    	ItemDefinition def = ItemDefinition.forId(itemId);
		int itemPrice = 0;
		int price = 1;
		if (def.getStorePrice() > def.getExchangePrice()) {
			itemPrice = def.getStorePrice() * amount;
			price = def.getStorePrice();
		} else {
			itemPrice = def.getExchangePrice() * amount;
			price =  def.getExchangePrice();
		}
		double itemAmount = amount;
		double maxAmount = (Integer.MAX_VALUE / price);
		if (itemAmount > maxAmount || itemPrice < 0) {
			return -1;
		}
		return itemPrice;
	}
    
    /*public static void writeTradeLog(Player trader1, Player trader2, Container traderItems1, Container traderItems2) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter("./data/trade_logs.txt", true));
            bw.write("\n---------------------------------------------------------");
            bw.write("\n " + trader1.getUsername() + " is trading with " + trader2.getUsername());
            for (int i = 0; i < traderItems1.size(); i++) {
                bw.write("\n Item_" + i + " Id: " + traderItems1.get(i).getDefinition().getId() + " Name: " + traderItems1.get(i).getDefinition().getName());
                bw.write("\n Item_" + i + " Amount: " + traderItems1.get(i).getAmount());
            }
            for (int i = 0; i < traderItems2.size(); i++) {
                bw.write("\n Item_" + i + " Id: " + traderItems2.get(i).getDefinition().getId() + " Name: " + traderItems2.get(i).getDefinition().getName());
                bw.write("\n Item_" + i + " Amount: " + traderItems2.get(i).getAmount());
            }
            bw.write("\n");
            bw.write("\n");
            bw.flush();
            bw.close();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }*/

}
