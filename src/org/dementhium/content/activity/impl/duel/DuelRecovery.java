package org.dementhium.content.activity.impl.duel;

import java.nio.ByteBuffer;
import org.jboss.netty.buffer.ChannelBuffer;
import org.dementhium.content.activity.impl.DuelActivity;
import org.dementhium.model.*;
import org.dementhium.model.player.Player;

/** Unclaimed duel items live in the account save, never in a transient interface or ground drop. */
public final class DuelRecovery {
    private static final String KEY="duelPendingItems";
    public static Container pending(Player p) {
        Container c=p.getAttribute(KEY); return c==null ? new Container(0,false) : c;
    }
    public static Container copy(Container c) {
        Container result=new Container(c.getSize(),false,false,true);
        for(int n=0;n<c.getSize();n++) if(c.get(n)!=null) result.set(n,new Item(c.get(n)));
        return result;
    }
    public static void replace(Container target,Container source) {
        target.clear();for(int n=0;n<source.getSize();n++) if(source.get(n)!=null) target.set(n,new Item(source.get(n)));
    }
    public static void setPending(Player p,Container c) { Container compact=new Container(c.size(),false,false,true);int n=0;for(Item item:c.toArray())if(item!=null)compact.set(n++,new Item(item));p.setAttribute(KEY,compact); }
    public static void add(Player p,Container c) {
        Container old=pending(p), next=new Container(old.getSize()+c.getSize(),false,false,true);
        next.addAll(old);next.addAll(c);setPending(p,next);
    }
    public static Container savedItems(Player p) {
        Container result=copy(pending(p));
        if(p.getActivity() instanceof DuelActivity) {
            DuelActivity duel=(DuelActivity)p.getActivity();
            if(!duel.isSettled()) {
                Container stake=duel.stakeOf(p).getContainer();
                Container ammo=p.getAttribute("droppedAmmo");
                Container all=new Container(result.getSize()+stake.getSize()+(ammo==null?0:ammo.getSize()),false,false,true);
                all.addAll(result);all.addAll(stake);if(ammo!=null)all.addAll(ammo);result=all;
            }
        }
        return result;
    }
    public static void save(Player p,ChannelBuffer buffer) {
        Container c=savedItems(p);if(c.size()==0)return;buffer.writeInt(0x44554c32);buffer.writeInt(c.size());
        for(Item item:c.toArray()) if(item!=null){buffer.writeInt(item.getId());buffer.writeLong(item.getHash());}
    }
    public static void load(Player p,ByteBuffer buffer) {
        if(buffer.remaining()<4)return;
        buffer.mark();if(buffer.getInt()!=0x44554c32){buffer.reset();return;}
        int count=buffer.getInt();
        if(count<0 || count>100000 || count>buffer.remaining()/12)throw new IllegalArgumentException("Invalid duel recovery trailer");
        Container c=new Container(count,false,false,true);
        for(int n=0;n<count;n++) {
            Item item=new Item(buffer.getInt(),buffer.getLong());
            if(item.getId()<0 || item.getAmount()<=0 || item.getHealth()<0)throw new IllegalArgumentException("Invalid recovery item");
            c.set(n,item);
        }
        setPending(p,c);
    }
    public static Location saveLocation(Player p,Location ordinary) {
        return p.getActivity() instanceof DuelActivity ? Location.locate(3366,3266,0) : ordinary;
    }
    public static boolean claim(Player p) {
        if(p.getActivity() instanceof DuelActivity)return false;
        Container before=copy(p.getInventory().getContainer()), pending=copy(pending(p));
        Container left=new Container(pending.getSize(),false,false,true);
        for(Item item:pending.toArray())if(item!=null) {
            Container one=new Container(1,false);one.set(0,new Item(item));
            if(!p.getInventory().getContainer().tryAddAll(one)) left.forceAdd(new Item(item));
        }
        setPending(p,left);
        if(!World.getWorld().getPlayerLoader().save(p)) {
            replace(p.getInventory().getContainer(),before);setPending(p,pending);
            p.sendMessage("Duel items are safe; collection could not be saved. Please try again.");return false;
        }
        p.getInventory().refresh();
        if(left.size()>0)p.sendMessage("You have unclaimed duel items. Make inventory space and use ::duelclaim.");
        return true;
    }
}


