package org.dementhium.content.minigames.gambler;

import java.text.NumberFormat;
import java.util.*;
import org.dementhium.content.DialogueManager;
import org.dementhium.content.activity.*;
import org.dementhium.content.activity.impl.DuelActivity;
import org.dementhium.content.activity.impl.duel.DuelConfigurations.TeleportLocations;
import org.dementhium.model.*;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.player.Player;
import org.dementhium.net.ActionSender;
import org.dementhium.util.InputHandler;

/** Presentation owns no money: a confirmed result is durable before either throw. */
public final class GamblerSession extends Activity<Player> {
    public enum Phase { MENU, ITEMS, INPUT, REVIEW, ROLLING, RESULT, CLOSED }
    private static final Map<NPC,GamblerSession> ROLLERS = new IdentityHashMap<NPC,GamblerSession>();
    private final NPC npc;
    private final boolean custom;
    private Phase phase=Phase.MENU;
    private int deadline, acceptAfter, rollAt, slot=-1, item, amount;
    private long hash;
    private boolean committing,previewVisible;
    private GamblerRecovery.Record result;
    public GamblerSession(Player p,NPC npc) {
        super(p);this.npc=npc;custom=p.getAttribute("gamblerCustomUi")==null?p.getConnection().supportsGamblerInterface():Boolean.TRUE.equals(p.getAttribute("gamblerCustomUi"));p.setActivity(this);deadline=World.getTicks()+100;
        setActivityState(SessionStates.UPDATE_STATE);
    }
    public static void open(Player p,NPC npc) {
        if(p.getActivity() instanceof GamblerSession)((GamblerSession)p.getActivity()).endSession();
        if(!DuelActivity.available(p)) {
            p.sendMessage("Finish your current activity before playing the Gambler.");return;
        }
        if(!atStand(p,npc)) {
            p.sendMessage("Stand near the Gambler at his Neitiznot or Duel Arena stand to play.");return;
        }
        p.closeAll(false,true);
        if(!GamblerPolicy.INSTANCE.enabled){p.sendMessage("The Gambler is closed for now.");return;}
        GamblerSession session=new GamblerSession(p,npc);
        if(!ActivityManager.getSingleton().register(session)){session.endSession();return;}
        session.menu();
    }
    private static boolean atStand(Player p,NPC npc) {
        if(npc==null||npc.getId()!=2998||npc.isDead()
                ||World.getWorld().getNpcs().get(npc.getIndex())!=npc)return false;
        Location anchor=npc.getOriginalLocation();
        // His spawn is on the room boundary; normal +/-3 wandering crosses it.
        // Validate the stand anchor, then proximity, rather than the player's room membership.
        return anchor!=null&&anchor.getZ()==0&&(TeleportLocations.CHALLENGE_ROOM.getArea().contains(anchor)
                ||org.dementhium.content.home.HomeHub.isGamblerStand(npc))
                &&npc.getLocation().withinRange(anchor,6)&&p.getLocation().withinRange(npc.getLocation(),3);
    }
    private boolean present() {
        Player p=getPlayer();return phase!=Phase.CLOSED&&p.getActivity()==this&&p.isOnline()&&!p.isDead()
                &&World.getWorld().getPlayers().get(p.getIndex())==p
                &&World.getWorld().getNpcs().get(npc.getIndex())==npc&&!npc.isDead()
                &&atStand(p,npc);
    }
    public Phase phase(){return phase;}
    public boolean canCollect(){return phase==Phase.MENU||phase==Phase.RESULT;}
    public static String number(long n){return NumberFormat.getIntegerInstance(Locale.US).format(n);}
    private String asset(){return item==995?"GP":new Item(item).getDefinition().getName();}
    public void menu() {
        phase=Phase.MENU;deadline=World.getTicks()+100;
        if(custom)ActionSender.sendCloseInterface(getPlayer());
        DialogueManager.sendOptionDialogue(getPlayer(),new int[]{19101,19102,19103,19104,19109},
            "Play for coins", "Play for runes", "Rules and payouts", "Last result / collect reward", "Leave");
    }
    public static boolean dialogue(Player p,int stage) {
        if(!(p.getActivity() instanceof GamblerSession))return false;
        GamblerSession s=(GamblerSession)p.getActivity();
        if(!s.present()){s.endSession();return false;}
        if(stage==19109){s.endSession();return false;}
        if(s.phase==Phase.MENU) {
            if(stage==19101)s.request(995);
            else if(stage==19102){s.phase=Phase.ITEMS;DialogueManager.sendOptionDialogue(p,new int[]{19105,19106,19107,19108},"Death runes","Blood runes","Nature runes","Back");}
            else if(stage==19103)DialogueManager.sendDisplayBox(p,19109,"Both roll 1-100. Higher wins; ties refund.",
                "Win: twice your wager returned. No fee or hidden odds.","Win 49.5% / Lose 49.5% / Tie 1%.","Confirm each bet. Rewards survive closing or disconnecting.","Coins: "+number(GamblerPolicy.INSTANCE.minimumCoins)+" to "+number(GamblerPolicy.INSTANCE.caps.get(995))+" GP.");
            else if(stage==19104){s.result=GamblerRecovery.get(p);if(s.result==null){p.sendMessage("You have no previous Gambler result.");s.menu();}else s.showResult();}
            else if(stage==19108)s.menu();
        }else if(s.phase==Phase.ITEMS) {
            if(stage==19108)s.menu();else if(stage>=19105&&stage<=19107)s.request(new int[]{560,565,561}[stage-19105]);
        }
        return true;
    }
    private void request(int id) {
        Player p=getPlayer();GamblerRecovery.Record previous=GamblerRecovery.get(p);
        if(previous!=null&&previous.pending>0){p.sendMessage("Collect your saved reward before betting again.");menu();return;}
        if(!GamblerPolicy.INSTANCE.caps.containsKey(id)){p.sendMessage("That item is not currently accepted.");menu();return;}
        slot=-1;
        for(int n=0;n<28;n++){Item i=p.getInventory().get(n);if(i!=null&&i.getId()==id&&GamblerPolicy.INSTANCE.eligible(i)){slot=n;break;}}
        if(slot<0){p.sendMessage("Bring coins or ordinary, unnoted eligible runes in your inventory.");menu();return;}
        item=id;hash=p.getInventory().get(slot).getHash();phase=Phase.INPUT;
        InputHandler.requestIntegerInput(p,40,"Wager in "+asset()+" (max "+number(GamblerPolicy.INSTANCE.caps.get(id))+"):");
        p.setAttribute("gamblerInput",this);
    }
    public static void input(Player p,int amount) {
        Object owner=p.getAttribute("gamblerInput");p.removeAttribute("gamblerInput");
        if(!(owner instanceof GamblerSession)||p.getActivity()!=owner)return;
        GamblerSession s=(GamblerSession)owner;
        if(s.phase!=Phase.INPUT||!s.present())return;
        Item i=p.getInventory().get(s.slot);
        if(i==null||i.getHash()!=s.hash||i.getId()!=s.item||i.getAmount()<amount||!GamblerPolicy.INSTANCE.valid(s.item,amount)) {
            p.sendMessage("That wager is outside the limits or your inventory changed.");s.menu();return;
        }
        s.amount=amount;s.phase=Phase.REVIEW;s.acceptAfter=World.getTicks()+3;s.deadline=World.getTicks()+100;
        s.review();
    }
    private void text(int inter,int child,String text){ActionSender.sendString(getPlayer(),text,inter,child);}
    private void review() {
        if(custom){customScreen(false);return;}
        Player p=getPlayer();ActionSender.sendCloseChatBox(p);ActionSender.sendInterface(p,626);
        text(626,20,"Gambler - review your wager");text(626,23,"Your wager");text(626,24,"Total returned on a win");
        text(626,25,"");text(626,26,"");text(626,46,number(amount)+" x "+asset());text(626,47,"<col=00cc00>"+number(2L*amount)+" x "+asset()+"</col>");
        for(int n=27;n<=45;n++)text(626,n,"");
        text(626,27,"The game");text(626,41,"Two independent rolls from 1 to 100.");
        text(626,28,"Higher roll wins. Equal rolls refund.");text(626,29,"Win 49.5% / Lose 49.5% / Tie 1%.");
        text(626,30,"No house fee. No donor or admin bias.");text(626,32,"Your outcome");
        text(626,33,"Win profit: +"+number(amount)+" "+asset());text(626,34,"Tie: your original wager comes back.");
        text(626,35,"Loss: the entire wager is lost.");text(626,37,"Only Confirm roll places the bet.");
        text(626,38,"Closing later cannot change the rolls.");text(626,39,"Rewards are saved if you disconnect.");
        text(626,42,"Full inventory? Use ::gambleclaim.");text(626,45,"Check your wager and total return.");
        text(626,53,"Roll");text(626,55,"Cancel");showPreview();
    }
    private void previewGrid(int parent,int container,int columns,int rows){
        // Script 153 draws once; unlike 149 it does not replace inventory-transmit hooks.
        ActionSender.sendClientScript(getPlayer(),153,new Object[]{"","","","","","","","","",-1,0,rows,columns,container,626<<16|parent},"noooobsssssssss");
    }
    private void showPreview(){
        Player p=getPlayer();Container wager=new Container(9,false),reward=new Container(9,false);
        // Centre row leaves room above for the cache-generated name and exact quantity.
        wager.set(4,new Item(item,amount));reward.set(4,new Item(item,Math.multiplyExact(amount,2)));
        // Script 206 reads BOTH sides of container 134 to rebuild text components 46/47.
        ActionSender.sendItems(p,134,wager,false);ActionSender.sendItems(p,134,reward,true);
        ActionSender.sendItems(p,136,reward,false);
        previewGrid(21,134,3,3);previewGrid(22,136,3,3);previewVisible=true;
    }
    private void clearPreview(){
        if(!previewVisible)return;
        // Script 153 loops inclusively (i <= columns*rows). A negative product skips all cells.
        previewGrid(21,134,-1,1);previewGrid(22,136,-1,1);previewVisible=false;
    }
    public boolean authorizes(Player p,int slot,long hash,int item,int amount) {
        return committing&&present()&&phase==Phase.REVIEW&&getPlayer()==p&&this.slot==slot&&this.hash==hash
                &&this.item==item&&this.amount==amount&&World.getTicks()>=acceptAfter&&World.getTicks()<deadline
                &&p.getTradeSession()==null&&!Boolean.TRUE.equals(p.getAttribute("inBank"))
                &&ROLLERS.get(npc)==this;
    }
    public boolean confirm() {
        Player p=getPlayer();
        if(phase!=Phase.REVIEW||!present()||World.getTicks()>=deadline)return false;
        if(World.getTicks()<acceptAfter){p.sendMessage("Take a moment to review your wager.");return false;}
        if(ROLLERS.containsKey(npc)){p.sendMessage("The Gambler is rolling for someone else. Please wait a moment.");return false;}
        ROLLERS.put(npc,this);committing=true;
        boolean committed;
        try {committed=World.getWorld().getPlayerLoader().commitGamble(p,slot,hash,item,amount,this);}
        finally {committing=false;}
        if(!committed){ROLLERS.remove(npc);p.sendMessage("No bet was placed. Check your wager or try again later.");return false;}
        if(!custom){text(626,23,"You are about to stake:");text(626,24,"Your opponent will stake:");text(626,27,"Before the duel starts:");text(626,32,"During the duel:");text(626,53,"Accept");text(626,55,"Decline");clearPreview();}result=GamblerRecovery.get(p);phase=Phase.ROLLING;rollAt=World.getTicks();deadline=rollAt+10;
        p.getInventory().refresh();if(custom)customScreen(false);else ActionSender.sendCloseInterface(p);ActionSender.sendCloseChatBox(p);
        npc.turnTo(p,false);p.turnTo(npc,false);p.animate(11900);ActionSender.spawnPositionedGraphic(p.getLocation(),2075);
        p.sendMessage("Your dice are rolling...");return true;
    }
    public static boolean button(Player p,int inter,int button,int opcode) {
        if(!(p.getActivity() instanceof GamblerSession))return false;
        GamblerSession s=(GamblerSession)p.getActivity();
        if(inter==891){
            if(!s.custom||opcode!=6)return true;
            if(!s.present()){s.endSession();return true;}
            if(button==32||button==33){s.endSession();return true;}
            if(s.phase==Phase.REVIEW){if(button==29)s.confirm();else if(button==30)s.menu();}
            else if(s.phase==Phase.RESULT){if(button==31){GamblerRecovery.claim(p);s.customScreen(true);}else if(button==29||button==30)s.menu();}
            return true;
        }
        if(inter!=626&&inter!=634)return false;
        if(s.custom)return true;
        if(opcode!=6)return true;
        if(inter==626&&s.phase==Phase.REVIEW){if(button==53)s.confirm();else if(button==55||button==7)s.endSession();}
        if(inter==634&&s.phase==Phase.RESULT&&(button==17||button==14||button==26)){GamblerRecovery.claim(p);s.endSession();}
        return true;
    }
    private String resultColor(){return result.playerRoll>result.houseRoll?"62e68a":result.playerRoll<result.houseRoll?"ff6868":"efcf7d";}
    private void customItems(int container,int id,int quantity,int parent){
        Container c=new Container(1,false);if(quantity>0)c.set(0,new Item(id,quantity));
        ActionSender.sendItems(getPlayer(),container,c,false);
        ActionSender.sendClientScript(getPlayer(),153,new Object[]{"","","","","","","","","",-1,0,1,1,container,891<<16|parent},"noooobsssssssss");
    }
    private void customScreen(boolean finished){
        Player p=getPlayer();ActionSender.sendCloseChatBox(p);ActionSender.sendInterface(p,891);
        int id=finished?result.item:item,wager=finished?result.stake:amount;
        String units=id==995?"GP":new Item(id).getDefinition().getName();
        String color=finished?resultColor():"efcf7d";
        int returned=finished?result.returned():Math.multiplyExact(wager,2);
        boolean rolling=phase==Phase.ROLLING;
        text(891,5,"Both roll 1-100. Higher wins; ties refund.");
        text(891,8,"YOUR WAGER");text(891,9,finished?"TOTAL RETURN":"TOTAL RETURN ON A WIN");
        text(891,10,new Item(id).getDefinition().getName());text(891,11,new Item(id).getDefinition().getName());
        // The asset name has its own row, keeping long rune names out of quantity fields.
        text(891,12,number(wager)+(id==995?" GP":""));text(891,13,"<col="+color+">"+number(returned)+(id==995?" GP":"")+"</col>");
        customItems(134,id,wager,14);customItems(136,id,returned,15);
        text(891,20,finished?"<col="+color+">"+result.playerRoll+"</col>":rolling?"Rolling...":"--");
        text(891,21,finished?"<col="+color+">"+result.houseRoll+"</col>":rolling?"Waiting...":"--");
        text(891,22,"<col="+color+">"+(finished?(result.playerRoll>result.houseRoll?"YOU WON!":result.playerRoll<result.houseRoll?"YOU LOST":"TIE - WAGER REFUNDED"):rolling?"ROLLING - WAGER CONFIRMED":"REVIEW YOUR WAGER")+"</col>");
        long net=(long)returned-wager;
        text(891,23,finished?"Net result: "+(net>0?"+":"")+number(net)+" "+units:"Win profit: +"+number(wager)+" "+units+" | Ties refund");
        boolean pending=finished&&GamblerRecovery.get(p).pending>0;
        text(891,28,finished?(pending?"Reward saved - free inventory space, then Collect.":"Reward settled. Each new wager requires confirmation."):rolling?"Closing cannot cancel a committed wager. Rewards are saved.":"Win 49.5% | Lose 49.5% | Tie 1% - Confirm places the wager.");
        text(891,29,finished?"New wager":rolling?"Rolling...":"Confirm roll");
        text(891,30,"Change wager");text(891,31,"Collect");text(891,32,"Close");
        // Despite its parameter name, sendInterfaceConfig(true) sends hidden=0.
        ActionSender.sendInterfaceConfig(p,891,29,!rolling);ActionSender.sendInterfaceConfig(p,891,24,!rolling);
        ActionSender.sendInterfaceConfig(p,891,30,!rolling);ActionSender.sendInterfaceConfig(p,891,25,!rolling);
        ActionSender.sendInterfaceConfig(p,891,31,pending);ActionSender.sendInterfaceConfig(p,891,26,pending);
    }
    private void showResult() {
        Player p=getPlayer();phase=Phase.RESULT;deadline=World.getTicks()+100;
        if(ROLLERS.get(npc)==this){ROLLERS.remove(npc);npc.resetTurnTo();}
        GamblerRecovery.claim(p);
        if(custom){customScreen(true);p.sendMessage("<col="+resultColor()+">You rolled: "+result.playerRoll+" | Gambler rolled: "+result.houseRoll+"</col>");return;}
        boolean win=result.playerRoll>result.houseRoll,tie=result.playerRoll==result.houseRoll;
        // Script 1640 overwrites 634:33 from varc-string 274.
        ActionSender.sendSpecialString(p,274,"");ActionSender.sendCloseChatBox(p);
        ActionSender.sendInterface(p,634);
        String units=result.item==995?"GP":new Item(result.item).getDefinition().getName();
        String rolls="You rolled: "+result.playerRoll+" | Gambler rolled: "+result.houseRoll;
        // Full-width heading avoids clipping these phrases in the old small name/level fields.
        String color=win?"00cc00":tie?"ffcc00":"ff0000";
        text(634,15,"<col="+color+">"+rolls+"</col>");
        text(634,18,result.returned()>0?"Total return":"No winnings");
        // Cached script 1191 sets the font for the English client (496 is the heading font).
        ActionSender.sendClientScript(p,1191,new Object[]{496,634<<16|29},"ii");
        text(634,29,"<col="+color+">"+(win?"You won!":tie?"Tie - refunded":"You lost!")+"</col>");
        text(634,30,"");text(634,31,"");text(634,32,"");text(634,33,"");
        p.sendMessage("<col="+color+">"+rolls+"</col>");
        text(634,17,GamblerRecovery.get(p).pending>0?"Collect":"Close");
        ActionSender.sendInterfaceConfig(p,634,13,true);
        ActionSender.sendInterfaceConfig(p,634,25,true);
        Container rewards=new Container(1,false);if(result.returned()>0)rewards.set(0,new Item(result.item,result.returned()));
        ActionSender.sendAMask(p,0,634,28,0,35);
        ActionSender.sendClientScript(p,149,new Object[]{"","","","","",-1,0,6,6,136,634<<16|28},"noooobsssss");
        ActionSender.sendItems(p,136,rewards,false);
        p.sendMessage("Gambler returned: "+number(result.returned())+" "+units
            +". Net: "+(win?"+":"")+number((long)result.returned()-result.stake)+". Round "+result.id.toString().substring(0,8)+".");
    }
    @Override public boolean updateSession() {
        if(!present()){endSession();return false;}
        if(phase==Phase.ROLLING) {
            int elapsed=World.getTicks()-rollAt;
            if(elapsed==3){npc.turnTo(getPlayer(),false);npc.animate(11900);ActionSender.spawnPositionedGraphic(npc.getLocation(),2075);getPlayer().sendMessage("The Gambler rolls...");if(custom){text(891,20,String.valueOf(result.playerRoll));text(891,21,"Rolling...");}}
            if(elapsed>=6)showResult();
        }else if(World.getTicks()>=deadline)endSession();
        return true;
    }
    @Override public boolean endSession() {
        if(phase==Phase.CLOSED)return true;clearPreview();
        if(!custom&&phase==Phase.REVIEW){
            text(626,20,"Are you sure you want to fight this duel?");text(626,23,"You are about to stake:");
            text(626,24,"Your opponent will stake:");text(626,27,"Before the duel starts:");text(626,32,"During the duel:");
            text(626,53,"Accept");text(626,55,"Decline");
        }
        if(!custom&&phase==Phase.RESULT){
            ActionSender.sendClientScript(getPlayer(),1191,new Object[]{495,634<<16|29},"ii");
            text(634,15,"You are Victorious!");text(634,18,"The Spoils:");text(634,29,"The Defeated:");
            text(634,30,"Name:");text(634,31,"Combat Level:");text(634,17,"Close");
            ActionSender.sendInterfaceConfig(getPlayer(),634,13,false);ActionSender.sendInterfaceConfig(getPlayer(),634,25,false);
        }
        phase=Phase.CLOSED;
        if(ROLLERS.get(npc)==this){ROLLERS.remove(npc);npc.resetTurnTo();}
        Player p=getPlayer();if(p.getActivity()==this){p.setActivity(Mob.DEFAULT_ACTIVITY);ActionSender.sendCloseInterface(p);DialogueManager.resetDialouge(p);}
        p.removeAttribute("gamblerInput");if(p.<Integer>getAttribute("inputId",-1)==40)p.removeAttribute("inputId");
        stop(false);return true;
    }
    @Override public boolean forceEnd(Player p){return endSession();}
    @Override public boolean walkingUpdate(Player p){endSession();return true;}
    @Override public boolean onTeleport(Player p){endSession();return true;}
    @Override public boolean onDeath(Player p){endSession();return false;}
    @Override public boolean isCombatActivity(Mob source,Mob victim,boolean message){return false;}
    @Override public boolean initializeActivity(){return false;}
    @Override public boolean commenceSession(){return false;}
}





