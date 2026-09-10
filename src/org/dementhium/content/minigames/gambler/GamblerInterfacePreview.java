package org.dementhium.content.minigames.gambler;

import org.dementhium.content.activity.*;
import org.dementhium.content.activity.impl.DuelActivity;
import org.dementhium.model.*;
import org.dementhium.model.player.Player;
import org.dementhium.net.ActionSender;

/** Admin-only visual proof. Sample containers never enter inventory or recovery storage. */
public final class GamblerInterfacePreview extends Activity<Player> {
    public static final int INTERFACE=891;
    private final int expires;
    private boolean closed;
    private GamblerInterfacePreview(Player p){super(p);expires=World.getTicks()+200;p.setActivity(this);setActivityState(SessionStates.UPDATE_STATE);}
    public static void open(Player p){
        if(p.getRights()<2){p.sendMessage("This interface preview is for administrators.");return;}
        if(!DuelActivity.available(p)){p.sendMessage("Finish your current activity before previewing.");return;}
        p.closeAll(false,true);
        GamblerInterfacePreview s=new GamblerInterfacePreview(p);
        if(!ActivityManager.getSingleton().register(s)){s.endSession();return;}
        ActionSender.sendCloseChatBox(p);ActionSender.sendInterface(p,INTERFACE);
        s.sample(134,1000000,14);s.sample(136,2000000,15);
        p.sendMessage("Gambler interface preview: use the separate development client. No GP is wagered.");
        for(int i=0;i<4;i++)ActionSender.sendString(p,new String[]{"Show win","Show loss","Show tie","Review"}[i],891,29+i);
        for(int i=24;i<=31;i++)ActionSender.sendInterfaceConfig(p,891,i,true);
        ActionSender.sendString(p,"Coins",891,10);ActionSender.sendString(p,"Coins",891,11);
        ActionSender.sendString(p,"1,000,000 GP",891,12);
        ActionSender.sendString(p,"PREVIEW ONLY - sample items, no money is wagered",891,28);
        s.show(0);
    }
    private void sample(int container,int amount,int parent){
        Container c=new Container(1,false);if(amount>0)c.set(0,new Item(995,amount));
        ActionSender.sendItems(getPlayer(),container,c,false);
        ActionSender.sendClientScript(getPlayer(),153,new Object[]{"","","","","","","","","",-1,0,1,1,container,INTERFACE<<16|parent},"noooobsssssssss");
    }
    private void text(int child,String value){ActionSender.sendString(getPlayer(),value,INTERFACE,child);}
    private void show(int state){
        String color=state==1?"62e68a":state==2?"ff6868":"efcf7d";
        text(20,"<col="+color+">"+(state==0?"--":state==1?"87":state==2?"24":"42")+"</col>");
        text(21,"<col="+color+">"+(state==0?"--":state==1?"32":state==2?"76":"42")+"</col>");
        text(22,"<col="+color+">"+(state==0?"REVIEW YOUR WAGER":state==1?"YOU WON!":state==2?"YOU LOST":"TIE - WAGER REFUNDED")+"</col>");
        text(23,state==0?"Win profit: +1,000,000 GP | Ties refund your wager":state==1?"Profit: +1,000,000 GP | Original wager included in return":state==2?"Net result: -1,000,000 GP":"Your full 1,000,000 GP wager is returned");
        text(9,state==0?"TOTAL RETURN ON A WIN":"TOTAL RETURN");
        int returned=state==2?0:state==3?1000000:2000000;
        text(13,"<col="+color+">"+GamblerSession.number(returned)+" GP</col>");
        sample(136,returned,15);
    }
    public static boolean button(Player p,int inter,int child,int opcode){
        if(inter!=INTERFACE)return false;
        if(!(p.getActivity() instanceof GamblerInterfacePreview))return !(p.getActivity() instanceof GamblerSession);
        GamblerInterfacePreview s=(GamblerInterfacePreview)p.getActivity();
        if(s.closed)return true;
        if(p.getRights()<2||World.getTicks()>=s.expires){s.endSession();return true;}
        if(opcode!=6)return true;
        if(child==33)s.endSession();
        else if(child>=29&&child<=32)s.show(child==32?0:child-28);
        return true;
    }
    @Override public boolean updateSession(){if(!getPlayer().isOnline()||getPlayer().isDead()||getPlayer().getActivity()!=this||World.getTicks()>=expires){endSession();return false;}return true;}
    @Override public boolean endSession(){if(closed)return true;closed=true;Player p=getPlayer();if(p.getActivity()==this){p.setActivity(Mob.DEFAULT_ACTIVITY);ActionSender.sendCloseInterface(p);}stop(false);return true;}
    @Override public boolean walkingUpdate(Player p){endSession();return true;}
    @Override public boolean forceEnd(Player p){return endSession();}
    @Override public boolean initializeActivity(){return true;}
    @Override public boolean commenceSession(){return true;}
}



