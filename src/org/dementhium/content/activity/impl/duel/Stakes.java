package org.dementhium.content.activity.impl.duel;
import org.dementhium.content.activity.impl.DuelActivity;
import org.dementhium.model.*;
import org.dementhium.model.player.Player;
import org.dementhium.net.ActionSender;

/** Exact transfers; no inventory helper may report a debit which did not happen. */
public class Stakes {
    private final Container stake=new Container(9,false);
    private final Player player;
    private final DuelActivity duel;
    public Stakes(Player p,DuelActivity duel){player=p;this.duel=duel;}
    public Container getContainer(){return stake;}
    public boolean stake(int itemId,int slot,int amount){
        if(!duel.editable(player)||amount<=0)return false;
        if(!duel.isStaking()){player.sendMessage("Items cannot be staked in a friendly duel.");return false;}
        Item item=player.getInventory().getContainer().get(slot);
        if(item==null||item.getId()!=itemId||!item.getDefinition().isTradeable())return false;
        return transfer(player.getInventory().getContainer(),stake,slot,amount,"changed their stake");
    }
    public boolean remove(int id,int amount){return removeSlot(stake.lookupSlot(id),id,amount);}
    public boolean removeSlot(int slot,int id,int amount){
        if(!duel.editable(player)||amount<=0)return false;
        Item item=stake.get(slot);if(item==null||item.getId()!=id)return false;
        return transfer(stake,player.getInventory().getContainer(),slot,amount,"removed items from their stake");
    }
    private boolean transfer(Container from,Container to,int slot,int amount,String text){
        Container beforeFrom=DuelRecovery.copy(from),beforeTo=DuelRecovery.copy(to);
        Container work=DuelRecovery.copy(from),offered=new Container(from.getSize(),false,false,true);
        Item selected=work.get(slot);if(selected==null)return false;
        long left=amount;
        // Prefer the clicked slot; only combine items with identical charge/degradation metadata.
        for(int n=0;n<work.getSize()&&left>0;n++){
            int index=n==0?slot:n<=slot?n-1:n;Item item=work.get(index);
            if(item==null||item.getId()!=selected.getId()||item.getHealth()!=selected.getHealth())continue;
            int count=(int)Math.min(left,item.getAmount());Item moved=new Item(item);moved.setAmount(count);
            offered.forceAdd(moved);left-=count;
            if(count==item.getAmount())work.set(index,null);else {Item rest=new Item(item);rest.setAmount(item.getAmount()-count);work.set(index,rest);}
        }
        if(offered.size()==0||!to.tryAddAll(offered)){player.sendMessage("There is not enough room for that transfer.");return false;}
        DuelRecovery.replace(from,work);
        if(!World.getWorld().getPlayerLoader().save(player)){
            DuelRecovery.replace(from,beforeFrom);DuelRecovery.replace(to,beforeTo);
            player.sendMessage("The stake change could not be saved. No items were moved.");return false;
        }
        duel.changed(player,text);refresh();duel.stakeOf(duel.getOpponent(player)).refresh();return true;
    }
    public boolean refresh(){
        if(!duel.owns(player))return false;
        ActionSender.sendItems(player,134,stake,false);
        ActionSender.sendItems(player,134,duel.stakeOf(duel.getOpponent(player)).getContainer(),true);
        player.getInventory().refresh();return true;
    }
}
