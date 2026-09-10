package org.dementhium.content.minigames.gambler;

import java.nio.ByteBuffer;
import java.util.UUID;
import org.jboss.netty.buffer.ChannelBuffer;
import org.dementhium.model.*;
import org.dementhium.model.player.Player;
import org.dementhium.content.activity.impl.duel.DuelRecovery;

/** Immutable committed outcome plus independently consumed, saved entitlement. */
public final class GamblerRecovery {
    private static final String KEY = "gamblerRecord";
    public static final class Record {
        public final UUID id;
        public final int item, stake, playerRoll, houseRoll, pending;
        public Record(UUID id, int item, int stake, int playerRoll, int houseRoll, int pending) {
            if (id == null || item < 0 || stake <= 0 || stake > Integer.MAX_VALUE / 2
                    || playerRoll < 1 || playerRoll > 100 || houseRoll < 1 || houseRoll > 100
                    || pending < 0 || pending > payout(stake, playerRoll, houseRoll))
                throw new IllegalArgumentException("Invalid gambler record");
            this.id=id;this.item=item;this.stake=stake;this.playerRoll=playerRoll;this.houseRoll=houseRoll;this.pending=pending;
        }
        public int returned() { return payout(stake, playerRoll, houseRoll); }
        public Record claimed() { return new Record(id,item,stake,playerRoll,houseRoll,0); }
    }
    public static int payout(int stake, int playerRoll, int houseRoll) {
        return playerRoll > houseRoll ? Math.multiplyExact(stake,2) : playerRoll == houseRoll ? stake : 0;
    }
    public static Record get(Player p) { return p.getAttribute(KEY); }
    public static void set(Player p, Record r) { if(r==null)p.removeAttribute(KEY);else p.setAttribute(KEY,r); }
    public static void save(Player p, ChannelBuffer b) {
        Record r=get(p);if(r==null)return;
        b.writeInt(0x47414d31);b.writeLong(r.id.getMostSignificantBits());b.writeLong(r.id.getLeastSignificantBits());
        b.writeInt(r.item);b.writeInt(r.stake);b.writeInt(r.playerRoll);b.writeInt(r.houseRoll);b.writeInt(r.pending);
    }
    public static void load(Player p, ByteBuffer b) {
        if(b.remaining()<4)return;
        b.mark();if(b.getInt()!=0x47414d31){b.reset();return;}
        set(p,new Record(new UUID(b.getLong(),b.getLong()),b.getInt(),b.getInt(),b.getInt(),b.getInt(),b.getInt()));
    }
    public static boolean claim(Player p) { synchronized(World.getWorld()) {
        Record r=get(p);if(r==null||r.pending==0)return true;
        if(p.getTradeSession()!=null || !p.isOnline() || Boolean.TRUE.equals(p.getAttribute("saveSessionClosed"))
                || World.getWorld().getPlayerInServer(p.getUsername())!=p
                || (p.getActivity()!=Mob.DEFAULT_ACTIVITY && !(p.getActivity() instanceof GamblerSession)))return false;
        if(p.getActivity() instanceof GamblerSession && !((GamblerSession)p.getActivity()).canCollect())return false;
        Container before=DuelRecovery.copy(p.getInventory().getContainer());
        Container reward=new Container(1,false);reward.set(0,new Item(r.item,r.pending));
        if(!p.getInventory().getContainer().tryAddAll(reward)) {
            p.sendMessage("Your Gambler reward is saved. Make space and use ::gambleclaim.");return false;
        }
        set(p,r.claimed());
        if(!World.getWorld().getPlayerLoader().save(p)) {
            DuelRecovery.replace(p.getInventory().getContainer(),before);set(p,r);
            p.sendMessage("Your reward is safe; collection could not be saved. Try ::gambleclaim later.");return false;
        }
        p.getInventory().refresh();return true;
    }}
}
