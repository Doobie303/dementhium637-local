package org.dementhium.content.activity.impl.duel;
import org.dementhium.content.activity.ActivityManager;
import org.dementhium.content.activity.impl.DuelActivity;
import org.dementhium.model.*;
import org.dementhium.model.player.Player;
import org.dementhium.net.ActionSender;

/** Requests retain object identity, mode and expiry; an index is never authority to admit a player. */
public final class DuelChallenge {
    final Player source,target;
    final boolean staking;
    final int expires;
    DuelChallenge(Player a,Player b,boolean staking){source=a;target=b;this.staking=staking;expires=World.getTicks()+100;}
    public static boolean inLobby(Player p){
        return p!=null&&DuelConfigurations.TeleportLocations.CHALLENGE_ROOM.getArea().contains(p.getLocation());
    }
    public static boolean eligible(Player a,Player b){
        return a!=b&&DuelActivity.available(a)&&DuelActivity.available(b)&&inLobby(a)&&inLobby(b)
            &&World.getWorld().getPlayers().get(a.getIndex())==a&&World.getWorld().getPlayers().get(b.getIndex())==b;
    }
    public static void select(Player a,Player b){
        if(!eligible(a,b)){a.sendMessage("Both players must be available in the Duel Arena challenge area.");return;}
        DuelChallenge incoming=b.getAttribute("duelChallenge");
        if(incoming!=null&&incoming.source==b&&incoming.target==a&&World.getTicks()<incoming.expires){
            DuelActivity activity=new DuelActivity(a,b,incoming.staking);
            if(!ActivityManager.getSingleton().register(activity))activity.forceEnd(a);
            return;
        }
        a.setAttribute("duelChallengeTarget",b);a.setAttribute("isStaking",false);
        ActionSender.sendInterface(a,640);ActionSender.sendConfig(a,283,67108864);
    }
    public static void send(Player a){
        Player b=a.getAttribute("duelChallengeTarget");
        if(!eligible(a,b)){DuelActivity.clearRequest(a);a.sendMessage("That duel request is no longer available.");return;}
        boolean staking=Boolean.TRUE.equals(a.getAttribute("isStaking"));
        a.setAttribute("duelChallenge",new DuelChallenge(a,b,staking));
        ActionSender.sendDuelReq(b,a.getDisplayName(),"wishes to duel with you ("+(staking?"stake":"friendly")+").");
        a.sendMessage("Sending duel request...");ActionSender.sendCloseInterface(a);
    }
}
