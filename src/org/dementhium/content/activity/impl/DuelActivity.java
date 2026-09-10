package org.dementhium.content.activity.impl;

import java.util.*;
import org.dementhium.content.activity.Activity;
import org.dementhium.content.activity.impl.duel.*;
import org.dementhium.content.activity.impl.duel.DuelConfigurations.Rules;
import org.dementhium.content.activity.impl.duel.DuelConfigurations.TeleportLocations;
import org.dementhium.model.*;
import org.dementhium.model.mask.ForceText;
import org.dementhium.model.misc.IconManager;
import org.dementhium.model.player.Player;
import org.dementhium.net.ActionSender;
import org.dementhium.tickable.Tick;

/** A single owner for both participants, their consent and escrow. All mutations run on the world cycle. */
public class DuelActivity extends Activity<Player> {
    public enum State { FIRST_SCREEN, SECOND_SCREEN, FIGHTING, SETTLING, FINISHED }
    public static final short DUEL_RULES_INTERFACE=631, DUEL_SECOND_INTERFACE=626;
    private final Player otherPlayer;
    private final boolean staking;
    private final Stakes firstStake,secondStake;
    private final DuelConfigurations duelConfigurations;
    private final String id=UUID.randomUUID().toString();
    private State currentState=State.FIRST_SCREEN;
    private boolean commenced,settled;
    private long revision,firstAccepted=-1,secondAccepted=-1,confirmedRevision=-1;
    private int acceptAfter;
    private Tick countdown,deathTimer;
    private Player winner;
    private boolean resultChosen;
    private boolean consentStarted;
    private final boolean[] needsReview=new boolean[2];
    private final BitSet[] changedRules={new BitSet(),new BitSet()};
    private final BitSet[] changedStakes={new BitSet(),new BitSet()};
    private int viewer(Player p){return p==getPlayer()?0:1;}
    public boolean highlightRule(Player p,int rule){return changedRules[viewer(p)].get(rule);}
    private void refreshConsent(Player p){
        int v=viewer(p);
        // 631:26 belongs to client hover help. These fields survive hover enter/leave.
        ActionSender.sendString(p,hasAccepted(p)?"Accepted":needsReview[v]?"<col=ffb000>Review</col>":"Accept",631,92);
        ActionSender.sendString(p,(changedStakes[v].get(v)?"<col=ffb000>* ":"")+(staking?"Your stake:":"Friendly duel"),631,89);
        ActionSender.sendString(p,(changedStakes[v].get(1-v)?"<col=ffb000>* ":"")+"Opponent's stake:",631,90);
        duelConfigurations.refreshLabels(p);
    }
    private Container firstSnapshot,secondSnapshot;
    private BitSet rulesSnapshot;

    public DuelActivity(Player a,Player b) { this(a,b,true); }
    public DuelActivity(Player a,Player b,boolean staking) {
        super(a);
        if(a==null||b==null||a==b||a.getUsername().equalsIgnoreCase(b.getUsername())||!available(a)||!available(b))
            throw new IllegalArgumentException("Duel participants are busy");
        otherPlayer=b;this.staking=staking;
        firstStake=new Stakes(a,this);secondStake=new Stakes(b,this);
        duelConfigurations=new DuelConfigurations(this);
        a.setActivity(this);b.setActivity(this);addEntity(b);
        a.setAttribute("duelStakes",firstStake);b.setAttribute("duelStakes",secondStake);
        clearRequest(a);clearRequest(b);
        a.setAttribute("droppedAmmo",null);b.setAttribute("droppedAmmo",null);
        acceptAfter=World.getTicks();
    }
    public static boolean available(Player p) {
        return p!=null && p.isOnline() && p.getActivity()==Mob.DEFAULT_ACTIVITY && !p.isDead()
            && p.getTradeSession()==null && p.getAttribute("duelingWith")==null;
    }
    public static void clearRequest(Player p) {
        p.removeAttribute("duelChallenge");p.removeAttribute("duelChallengeTarget");p.removeAttribute("duelWithIndex");
        p.setAttribute("didRequestDuel",false);
    }
    public boolean owns(Player p) { return (p==getPlayer()||p==otherPlayer) && p.getActivity()==this; }
    public boolean editable(Player p) { return owns(p)&&!settled&&currentState==State.FIRST_SCREEN; }
    public String getId(){return id;}
    public boolean isStaking(){return staking;}
    public boolean isSettled(){return settled;}
    public Stakes stakeOf(Player p){if(p==getPlayer())return firstStake;if(p==otherPlayer)return secondStake;throw new IllegalArgumentException("Not a participant");}
    public Player getOpponent(Player p){if(p==getPlayer())return otherPlayer;if(p==otherPlayer)return getPlayer();throw new IllegalArgumentException("Not a participant");}
    public Player getOtherPlayer(){return otherPlayer;}
    public DuelConfigurations getDuelConfigurations(){return duelConfigurations;}
    public State getCurrentState(){return currentState;}
    public boolean isCommenced(){return commenced&&!settled&&currentState==State.FIGHTING;}
    public Container getStakes(){Container c=new Container(18,false,false,true);c.addAll(firstStake.getContainer());c.addAll(secondStake.getContainer());return c;}
    public long getRevision(){return revision;}
    public boolean hasAccepted(Player p){return (p==getPlayer()?firstAccepted:secondAccepted)==revision;}
    private Player[] players(){return new Player[]{getPlayer(),otherPlayer};}
    public void changed(Player actor,String description) { changed(actor,description,null); }
    public void changed(Player actor,String description,BitSet ruleChanges) {
        boolean cleared=firstAccepted==revision||secondAccepted==revision;
        revision++;firstAccepted=secondAccepted=-1;confirmedRevision=-1;acceptAfter=World.getTicks()+3;
        for(Player p:players()) {
            int v=viewer(p);p.setAttribute("acceptedDuel",false);
            if(consentStarted){
                needsReview[v]=true;
                if(ruleChanges==null)changedStakes[v].set(viewer(actor));else changedRules[v].or(ruleChanges);
                if(p!=actor)p.sendMessage("<col=ffb000>Opponent: "+description+" Review before accepting.</col>");
                else if(cleared)p.sendMessage("Your change cleared acceptance. Both players must review again.");
            }
            ActionSender.sendString(p,"",631,26);
            refreshConsent(p);
        }
    }
    @Override public boolean initializeActivity(){
        if(!owns(getPlayer())||!owns(otherPlayer)){forceEnd(getPlayer());return false;}
        setActivityState(SessionStates.PAUSE_STATE);
        for(Player p:players()) {
            // Cached script 1640 reads varc-string 274 and can overwrite component text.
            ActionSender.sendSpecialString(p,274,getOpponent(p).getDisplayName());
            ActionSender.sendInventoryInterface(p,628);ActionSender.sendInterface(p,631);
            ActionSender.sendDuelOptions(p);duelConfigurations.refresh(p);
            ActionSender.sendString(p,getOpponent(p).getDisplayName(),631,23);
            ActionSender.sendString(p,""+getOpponent(p).getSkills().getCombatLevel(),631,25);
            ActionSender.sendString(p,staking?"Review rules and stakes.":"Friendly duel - no items can be staked.",631,26);
            ActionSender.sendString(p,staking?"Your stake:":"Friendly duel",631,89);
            stakeOf(p).refresh();refreshConsent(p);
        }
        return false;
    }
    private boolean canAccept(Player p,State state){
        if(!owns(p)||settled||currentState!=state)return false;
        if(World.getTicks()<acceptAfter){p.sendMessage("Please review the changed offer before accepting.");return false;}
        return duelConfigurations.canAccept(getPlayer())&&duelConfigurations.canAccept(otherPlayer);
    }
    private void recordAccept(Player p,int inter,int child){
        if(p==getPlayer())firstAccepted=revision;else secondAccepted=revision;
        p.setAttribute("acceptedDuel",true);
        if(inter==631){
            consentStarted=true;int v=viewer(p);needsReview[v]=false;changedRules[v].clear();changedStakes[v].clear();refreshConsent(p);
        }
        ActionSender.sendString(p,"Accepted. Waiting for opponent.",inter,child);
        ActionSender.sendString(getOpponent(p),"Other player has accepted.",inter,child);
    }
    public boolean accept(Player p){
        if(!canAccept(p,State.FIRST_SCREEN))return false;
        recordAccept(p,631,26);
        if(firstAccepted==revision&&secondAccepted==revision){
            firstSnapshot=DuelRecovery.copy(firstStake.getContainer());secondSnapshot=DuelRecovery.copy(secondStake.getContainer());
            rulesSnapshot=duelConfigurations.snapshot();confirmedRevision=revision;
            currentState=State.SECOND_SCREEN;firstAccepted=secondAccepted=-1;acceptAfter=World.getTicks()+3;
            for(Player q:players()){q.setAttribute("acceptedDuel",false);duelConfigurations.sendSecondInterface(q,getOpponent(q));}
        }
        return true;
    }
    private boolean snapshotCurrent(){
        return confirmedRevision==revision && rulesSnapshot.equals(duelConfigurations.snapshot())
            && equal(firstSnapshot,firstStake.getContainer())&&equal(secondSnapshot,secondStake.getContainer());
    }
    private static boolean equal(Container a,Container b){
        if(a==null||a.getSize()!=b.getSize())return false;
        for(int n=0;n<a.getSize();n++){Item x=a.get(n),y=b.get(n);if(x==null?y!=null:y==null||x.getId()!=y.getId()||x.getHash()!=y.getHash())return false;}return true;
    }
    public void acceptSecond(Player p){
        if(!canAccept(p,State.SECOND_SCREEN))return;
        if(!snapshotCurrent()){decline(p,false);return;}
        recordAccept(p,626,45);
        if(firstAccepted==revision&&secondAccepted==revision){
            currentState=State.FIGHTING;setActivityState(SessionStates.COMMENCE_STATE);
        }
    }
    @Override public boolean commenceSession(){
        if(settled||currentState!=State.FIGHTING||!snapshotCurrent())return false;
        if(!duelConfigurations.canAccept(getPlayer())||!duelConfigurations.canAccept(otherPlayer)){forceEnd(getPlayer());return false;}
        Location[] locations=duelConfigurations.startLocations();
        if(locations==null){for(Player p:players())p.sendMessage("No safe duel starting tiles are available.");forceEnd(getPlayer());return false;}
        for(Player p:players()) {
            p.stopAll();p.getCombatExecutor().cancelPending();p.setAttribute("staffOfLightEffect",-1);
            ActionSender.sendCloseInterface(p);ActionSender.closeInventoryInterface(p);ActionSender.sendCloseChatBox(p);
            duelConfigurations.removeEquipment(p);p.fullRestore();
            p.setAttribute("duelingWith",getOpponent(p));ActionSender.sendPlayerOption(p,"Fight",1,true);
        }
        getPlayer().teleport(locations[0],false);otherPlayer.teleport(locations[1],false);
        IconManager.iconOnMob(getPlayer(),otherPlayer,1,65535);IconManager.iconOnMob(otherPlayer,getPlayer(),1,65535);
        countdown=new Tick(2){int count=3;public void execute(){
            if(settled||currentState!=State.FIGHTING||!owns(getPlayer())||!owns(otherPlayer)){stop();return;}
            for(Player p:players())p.getMask().setForceText(new ForceText(count==0?"FIGHT!":""+count));
            if(count--==0){commenced=true;stop();}
        }};World.getWorld().submit(countdown);return true;
    }
    public boolean forfeit(Player p){
        if(!owns(p)||currentState!=State.FIGHTING||settled)return false;
        if(duelConfigurations.getRule(Rules.FORFEIT)){p.sendMessage("Forfeiting is disabled for this duel.");return false;}
        return finish(getOpponent(p));
    }
    public boolean depart(Player p){
        if(!owns(p)||settled)return true;
        return finish(currentState==State.FIGHTING?getOpponent(p):null);
    }
    @Override public boolean forceEnd(Player p){return settled||finish(null);}
    @Override public boolean endSession(){return finish(resultChosen?winner:null);}
    public boolean decline(Player p,boolean full){
        if(!owns(p)||!(currentState==State.FIRST_SCREEN||currentState==State.SECOND_SCREEN))return false;
        return finish(null);
    }
    /** Commit before detaching. No delayed callback ever saves an old Player object. */
    private boolean finish(Player selected){
        if(settled)return true;
        if(!resultChosen){winner=selected;resultChosen=true;}
        currentState=State.SETTLING;commenced=false;if(countdown!=null)countdown.stop();if(deathTimer!=null)deathTimer.stop();
        setActivityState(SessionStates.END_STATE);
        for(Player p:players())p.getCombatExecutor().cancelPending();
        Container a=DuelRecovery.copy(firstStake.getContainer()),b=DuelRecovery.copy(secondStake.getContainer());
        Container pa=DuelRecovery.copy(DuelRecovery.pending(getPlayer())),pb=DuelRecovery.copy(DuelRecovery.pending(otherPlayer));
        Container aa=getPlayer().getAttribute("droppedAmmo"),ab=otherPlayer.getAttribute("droppedAmmo");
        if(winner==null){DuelRecovery.add(getPlayer(),a);DuelRecovery.add(otherPlayer,b);}
        else {DuelRecovery.add(winner,a);DuelRecovery.add(winner,b);}
        if(aa!=null)DuelRecovery.add(getPlayer(),aa);if(ab!=null)DuelRecovery.add(otherPlayer,ab);
        firstStake.getContainer().clear();secondStake.getContainer().clear();
        getPlayer().removeAttribute("droppedAmmo");otherPlayer.removeAttribute("droppedAmmo");
        if(!World.getWorld().getPlayerLoader().saveDuel(getPlayer(),otherPlayer)){
            DuelRecovery.replace(firstStake.getContainer(),a);DuelRecovery.replace(secondStake.getContainer(),b);
            DuelRecovery.setPending(getPlayer(),pa);DuelRecovery.setPending(otherPlayer,pb);
            getPlayer().setAttribute("droppedAmmo",aa);otherPlayer.setAttribute("droppedAmmo",ab);
            for(Player p:players())p.sendMessage("Duel settlement is waiting for storage. Your items remain protected.");return false;
        }
        settled=true;currentState=State.FINISHED;
        World.getWorld().getPlayerLoader().recordDuel(id,winner,getPlayer(),otherPlayer,a,b);
        for(Player p:players()){
            if(p.getActivity()==this)p.setActivity(Mob.DEFAULT_ACTIVITY);
            p.removeAttribute("duelStakes");p.removeAttribute("duelStake");p.removeAttribute("duelingWith");
            p.removeAttribute("duellingForfeit");p.removeAttribute("hasWonDuel");p.setAttribute("acceptedDuel",false);clearRequest(p);
            p.removeAttribute("duelInputSession");
            p.getSkills().resetDuelDeath();p.fullRestore();p.removeTick("death_tick");
            p.setAttribute("freezeTime",0);p.setAttribute("teleblock",0);p.setAttribute("teleblockImmunity",0);p.getDamageManager().clearEnemyHits();
            IconManager.removeIcon(p,getOpponent(p));
            ActionSender.sendCloseInterface(p);ActionSender.closeInventoryInterface(p);
            ActionSender.sendPlayerOption(p,"Challenge",1,false);
            p.teleport(Location.locate(3366,3266,0),false);
            DuelRecovery.claim(p);
        }
        if(winner!=null && winner.isOnline()){
            Player loser=getOpponent(winner);
            ActionSender.sendSpecialString(winner,274,loser.getDisplayName());
            ActionSender.sendInterface(winner,634);
            ActionSender.sendString(winner,loser.getDisplayName(),634,33);ActionSender.sendString(winner,"Close",634,17);
            ActionSender.sendString(winner,""+loser.getSkills().getCombatLevel(),634,32);
            Container prize=winner==getPlayer()?b:a;
            ActionSender.sendAMask(winner,1026,634,28,0,35);
            ActionSender.sendClientScript(winner,149,new Object[]{"","","","","",-1,0,6,6,136,634<<16|28},"noooobsssss");
            ActionSender.sendItems(winner,136,prize,false);
            winner.sendMessage("Duel winnings returned to your inventory. Any overflow is safe: ::duelclaim.");
        }
        stop(false);return true;
    }
    public void beginDeath(Player p) {
        if(!owns(p)||settled||resultChosen)return;
        winner=currentState==State.FIGHTING?getOpponent(p):null;resultChosen=true;
        currentState=State.SETTLING;commenced=false;setActivityState(SessionStates.PAUSE_STATE);
        if(countdown!=null)countdown.stop();
        for(Player q:players())q.getCombatExecutor().cancelPending();
        p.animate(9055);
        deathTimer=new Tick(4){public void execute(){stop();finish(winner);}};
        World.getWorld().submit(deathTimer);
    }
    @Override public boolean onDeath(Player p){if(owns(p)&&!settled&&currentState==State.FIGHTING)finish(getOpponent(p));return true;}
    @Override public boolean canLogout(Player p,boolean button){if(button&&currentState==State.FIGHTING){p.sendMessage("You cannot logout during a duel.");return false;}return depart(p);}
    @Override public boolean onTeleport(Player p){p.sendMessage("You cannot teleport during a duel.");return false;}
    @Override public boolean walkingUpdate(Player p){
        if(currentState==State.FIRST_SCREEN||currentState==State.SECOND_SCREEN)return decline(p,false);
        if(currentState==State.SETTLING||(!commenced)||duelConfigurations.getRule(Rules.MOVEMENT))return false;
        return true;
    }
    @Override public boolean isCombatActivity(Mob a,Mob b,boolean messages){
        return a.isPlayer()&&b.isPlayer()&&isCommenced()&&owns(a.getPlayer())&&owns(b.getPlayer())&&a!=b;
    }
}
