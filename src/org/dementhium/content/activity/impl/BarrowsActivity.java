package org.dementhium.content.activity.impl;

import java.util.*;
import org.dementhium.content.activity.Activity;
import org.dementhium.content.activity.impl.barrows.*;
import org.dementhium.content.dialogue.*;
import org.dementhium.event.EventListener.ClickOption;
import org.dementhium.model.*;
import org.dementhium.model.map.*;
import org.dementhium.model.misc.IconManager;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.npc.impl.BarrowBrother;
import org.dementhium.model.player.Player;
import org.dementhium.net.ActionSender;
import org.dementhium.tickable.Tick;

/** One owner's six-brother run. A looted run remains valid until the next dig. */
public class BarrowsActivity extends Activity<BarrowsCrypt> {
    public static final int MAIN_INTERFACE=24;
    private BarrowsTunnels tunnels;
    private int generation;

    private boolean puzzleSolved;
    private boolean crossing;
    private GameObject puzzleDoor;
    private Location puzzleOrigin;
    private int puzzleAnswer = -1;
    private static final int[][] ENTRY={{3534,9712},{3568,9712},{3568,9678},{3534,9678}};
    public BarrowsActivity(Player player){super(player);player.setActivity(this);}
    private boolean looted(){return getPlayer().getAttribute("looted_barrows_request_shake",false);}
    public boolean initializeActivity(){
        getEntities().clear();
        for(BarrowsCrypt crypt:BarrowsConstants.BARROWS_CRYPT) getEntities().add(crypt.duplicate());
        int entrance=getPlayer().getSettings().getTunnelEntranceId();
        if(entrance<0||entrance>=6){entrance=getPlayer().getRandom().nextInt(6);getPlayer().getSettings().setTunnelEntranceId(entrance);}
        getEntities().get(entrance).setTunnelsEntrance(true);
        int layout=getPlayer().getSettings().getTunnelId();
        if(layout<0||layout>=32){layout=getPlayer().getRandom().nextInt(32);getPlayer().getSettings().setTunnelId(layout);}
        tunnels=new BarrowsTunnels(layout);
        generation++;puzzleSolved=false;crossing=false;
        getPlayer().removeAttribute("barrowsPuzzleOpen"); puzzleDoor=null;
        return true;
    }
    public boolean commenceSession(){updateOverlay();return true;}
    public boolean updateSession(){
        if(!BarrowsConstants.isInBarrowsZone(getPlayer())) setActivityState(SessionStates.END_STATE);
        // A brother left in a crypt must be summonable again on return.
        for(BarrowsCrypt crypt:getEntities()){
            NPC npc=crypt.getNPC();
            if(npc.getAttribute("isSpawned",false) && !npc.isDead()
                    && (npc.getLocation().getZ()!=getPlayer().getLocation().getZ()
                    || npc.getLocation().distance(getPlayer().getLocation())>24)) removeBrother(npc);
        }
        return true;
    }
    private void removeBrother(NPC npc){
        IconManager.removeIcon(getPlayer(),npc);
        npc.getCombatExecutor().setVictim(null);
        npc.removeAttribute("isSpawned");
        npc.destroy();World.getWorld().getNpcs().remove(npc);
    }
    public boolean endSession(){
        boolean wasCrossing = crossing;
        generation++;crossing=false;
        getPlayer().removeAttribute("barrows_crossing");
        getPlayer().removeAttribute("barrowsPuzzleOpen");
        if (wasCrossing && !Boolean.TRUE.equals(getPlayer().getAttribute("stunned"))) getPlayer().removeAttribute("cantMove");
        for(BarrowsCrypt crypt:getEntities())removeBrother(crypt.getNPC());
        ActionSender.sendCloseOverlay(getPlayer());
        ActionSender.updateMinimap(getPlayer(),ActionSender.NO_BLACKOUT);
        getPlayer().setActivity(Mob.DEFAULT_ACTIVITY);return true;
    }
    public boolean onDeath(Player player){setActivityState(SessionStates.END_STATE);return false;}
    public boolean canLogout(Player player,boolean button){stop(true);return true;}
    public boolean isCombatActivity(Mob source,Mob target,boolean messages){
        if(!source.isNPC()&&!target.isNPC())return false;
        Player owner=target.getAttribute("barrowsOwner");
        if(owner!=null && owner!=source){if(messages&&source.isPlayer())source.getPlayer().sendMessage("This monster is not after you.");return false;}
        owner=source.getAttribute("barrowsOwner");
        return owner==null||owner==target;
    }
    public void updateOverlay(){
        Player p=getPlayer();int mask=0;
        for(int id:BarrowsRules.BROTHERS)if(p.getSettings().getBarrowsKilled().contains(id))mask|=1<<(id-2025);
        ActionSender.sendConfig(p,453,(p.getSettings().getBarrowsKillcount()<<17)|mask);
        if(!BarrowsConstants.BARROWS_AREA.isInArea(p.getLocation())){
            ActionSender.sendConfig(p,1270,BarrowsConstants.isInMiniTunnel(p)?1:0);
            ActionSender.sendBConfig(p,1043,-1); // Do not replay a previous face on door/kill refresh.
            ActionSender.sendOverlay(p,MAIN_INTERFACE);
            ActionSender.updateMinimap(p,ActionSender.BLACKOUT_MAP);
        }
    }
    private void resetRun(){
        for(BarrowsCrypt crypt:getEntities())removeBrother(crypt.getNPC());
        Player p=getPlayer();
        p.getSettings().getBarrowsKilled().clear();
        Arrays.fill(p.getSettings().getKilledBrothers(),false);
        p.getSettings().setBarrowsKillcount(0);
        p.getSettings().setBarrowsPotential(0);
        p.getSettings().setTunnelId(-1);p.getSettings().setTunnelEntranceId(-1);
        p.removeAttribute("newBarrowsRun");p.removeAttribute("canLootBarrowsChest");
        p.removeAttribute("looted_barrows_request_shake");
        ActionSender.resetCamera(p);
        initializeActivity();updateOverlay();
    }
    public void enterTunnels(){
        if(getPlayer().getActivity()!=this)return;
        int[] entry=ENTRY[Math.floorMod(getPlayer().getSettings().getTunnelId()/8,4)];
        getPlayer().teleport(entry[0],entry[1],0,false);
        getPlayer().setAttribute("newBarrowsRun",true);updateOverlay();
    }
    public void leaveTunnels(){
        int index=getPlayer().getSettings().getTunnelEntranceId();
        if(index<0||index>=6)return;
        getPlayer().teleport(BarrowsConstants.CRYPT_TELEPORT_LOCATIONS[index],false);
        ActionSender.resetCamera(getPlayer());updateOverlay();
    }
    /** Called only by the death of a live NPC, never by encounter cleanup. */
    public void recordKill(NPC npc){
        Player p=getPlayer();
        if(looted())return;
        int index=BarrowsRules.index(npc.getId());
        if(index>=0){
            if(npc.getAttribute("barrowsOwner")!=p || !owns(npc)
                    || p.getSettings().getBarrowsKilled().contains(npc.getId()))return;
            p.getSettings().getBarrowsKilled().add(npc.getId());
            p.getSettings().getKilledBrothers()[index]=true;
            npc.removeAttribute("isSpawned");IconManager.removeIcon(p,npc);
            if(index==p.getSettings().getTunnelEntranceId())p.setAttribute("canLootBarrowsChest",true);
        }else{
            if(!BarrowsConstants.TUNNELS.isInArea(p.getLocation())||!BarrowsRules.tunnelMonster(npc.getId()))return;
            p.getSettings().setBarrowsPotential(BarrowsRules.addPotential(p.getSettings().getBarrowsPotential(),
                    org.dementhium.cache.format.CacheNPCDefinition.forID(npc.getId()).combatLevel));
        }
        p.getSettings().setBarrowsKillcount(p.getSettings().getBarrowsKillcount()+1);
        updateOverlay();
    }
    public boolean owns(NPC npc){
        for(BarrowsCrypt crypt:getEntities())if(crypt.getNPC()==npc)return true;
        return false;
    }
    private boolean spawnBrother(BarrowsCrypt crypt){
        Player p=getPlayer();NPC npc=crypt.getNPC();
        if(looted()||p.getSettings().getBarrowsKilled().contains(npc.getId())||npc.getAttribute("isSpawned",false))return false;
        Location place=p.getLocation();
        for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){
            Location candidate=place.transform(d[0],d[1],0);
            if((Region.getClippingMask(candidate.getX(),candidate.getY(),candidate.getZ())&0x1280100)==0){place=candidate;break;}
        }
        npc.setDead(false);npc.setHp(npc.getMaximumHitPoints());npc.setLocation(place);
        npc.setAttribute("activity","BarrowsActivity");npc.setAttribute("barrowsOwner",p);
        npc.setAttribute("isSpawned",true);npc.setUnrespawnable(true);
        npc.getCombatExecutor().setVictim(p);npc.turnTo(p,false);
        npc.forceText("You dare disturb my rest!");World.getWorld().getNpcs().add(npc);
        IconManager.iconOnMob(p,npc,1,65535);return true;
    }
    private void chest(GameObject object){
        Player p=getPlayer();
        if(looted()){p.sendMessage("The chest is empty.");return;}
        if(!BarrowsConstants.TUNNELS.isInArea(p.getLocation())
                || p.getLocation().distance(object.getLocation())>3)return;
        int index=p.getSettings().getTunnelEntranceId();
        if(index<0||index>=getEntities().size())return;
        // Searching triggers the missing brother, but does not require killing all six.
        if(spawnBrother(getEntities().get(index)))return;
        // Mark first: another click can never generate this run's reward again.
        p.setAttribute("looted_barrows_request_shake",true);
        BarrowsReward.open(p,BarrowsRules.rewards(p.getRandom(),p.getSettings().getBarrowsKilled(),p.getSettings().getBarrowsPotential()));
        ActionSender.sendObject(p,6775,3551,9695,0,10,0);
    }
    public boolean objectAction(final Player player,final GameObject object,ClickOption option){
        int id=object.getId();
        if(id==10284||id==6775){chest(object);return true;}
        if(id>=6702&&id<=6707){
            for(int i=0;i<6;i++)if(BarrowsConstants.CRYPT_AREA[i].isInArea(player.getLocation())){
                Location l=BarrowsConstants.HILL_AREA[i].getSouthWest();
                player.teleport(l.transform(1,1,0),false);
                ActionSender.sendCloseOverlay(player);ActionSender.updateMinimap(player,ActionSender.NO_BLACKOUT);return true;
            }
        }
        if(id==6823||id==6771||id==6821||id==6773||id==6822||id==6772){
            for(int i=0;i<6;i++)if(BarrowsConstants.CRYPT_AREA[i].isInArea(player.getLocation())){
                final BarrowsCrypt crypt=getEntities().get(i);
                if(crypt.isTunnelsEntrance()){
                    Dialogue d=new Dialogue();d.setType(DialogueType.OPTION);
                    d.getMessage().add("Yes, enter the hidden tunnel.");d.getMessage().add("No, stay here.");
                    final int run=generation;final Location origin=player.getLocation();
                    d.getActions().add(new OptionAction(){public boolean handle(Player p){
                        if(run==generation&&p.getActivity()==BarrowsActivity.this&&p.getLocation().equals(origin))enterTunnels();
                        return true;
                    }});
                    d.getActions().add(new OptionAction(){public boolean handle(Player p){return true;}});d.send(player);
                }else if(!spawnBrother(crypt))player.sendMessage("You don't find anything.");
                return true;
            }
        }
        Gate gate=tunnels.get(id,object.getLocation().getX(),object.getLocation().getY(),object.getLocation().getZ());
        if(gate==null)return false;
        if(crossing||player.isDead()||!BarrowsConstants.TUNNELS.isInArea(player.getLocation()))return true;
        if(gate.isClosed()){player.sendMessage("The door seems to be locked.");return true;}
        if(tunnels.isPuzzleGate(gate)&&!puzzleSolved){showPuzzle(object);return true;}
        cross(object,gate);return true;
    }
    private void showPuzzle(final GameObject object) {
        if (getPlayer().getLocation().distance(object.getLocation()) > 2) return;
        int base = 6713 + 6 * getPlayer().getRandom().nextInt(4);
        List<Integer> options = new ArrayList<Integer>(Arrays.asList(base, base + 1, base + 2));
        Collections.shuffle(options, getPlayer().getRandom());
        int[] children = {2, 3, 5};
        puzzleDoor = object;
        puzzleOrigin = getPlayer().getLocation();
        puzzleAnswer = children[options.indexOf(base)];
        ActionSender.sendInterface(getPlayer(), 25);
        for (int i = 0; i < 3; i++) {
            ActionSender.sendModelOnInterface(getPlayer(), 25, 6 + i, base + 3 + i);
            ActionSender.sendModelOnInterface(getPlayer(), 25, children[i], options.get(i));
        }
        getPlayer().setAttribute("barrowsPuzzleOpen", true);
    }
    public void answerPuzzle(int child) {
        Player p = getPlayer();
        if (child != 2 && child != 3 && child != 5) return;
        if (!p.getAttribute("barrowsPuzzleOpen", false) || puzzleDoor == null
                || p.getActivity() != this || p.isDead() || !p.getLocation().equals(puzzleOrigin)) return;
        GameObject door = puzzleDoor;
        boolean correct = child == puzzleAnswer;
        puzzleDoor = null;
        ActionSender.sendCloseInterface(p);
        if (correct) {
            puzzleSolved = true;
            p.sendMessage("You hear the door unlock.");
            Gate gate = tunnels.get(door.getId(), door.getLocation().getX(), door.getLocation().getY(), 0);
            if (gate != null && !gate.isClosed()) cross(door, gate);
        } else {
            int old = p.getSettings().getTunnelId();
            int layout = (old / 4) * 4 + ((old + 1 + p.getRandom().nextInt(3)) % 4);
            p.getSettings().setTunnelId(layout);
            tunnels = new BarrowsTunnels(layout);
            p.sendMessage("You hear the doors rearranging around you.");
        }
    }
    private void cross(final GameObject object,final Gate gate){
        final Player p=getPlayer();
        if (crossing || p.getAttribute("cantMove", false)
                || p.getAttribute("freezeTime", -1) > World.getTicks()) return;
        if(p.getLocation().getZ()!=0||p.getLocation().distance(object.getLocation())>2)return;
        final Location origin=p.getLocation();
        final Location destination=BarrowsRules.crossingDestination(object,origin);
        if(!BarrowsConstants.TUNNELS.isInArea(destination)||origin.distance(destination)>2.5)return;
        crossing=true;p.setAttribute("barrows_crossing",true);p.setAttribute("cantMove",true);
        p.getWalkingQueue().reset();p.getMask().setFacePosition(object.getLocation(),1,1);
        ActionSender.deleteObject(p,object.getId(),object.getLocation().getX(),object.getLocation().getY(),0,object.getType(),object.getRotation());
        ActionSender.sendObject(p,gate.getToReplace());
        final int run=generation;
        World.getWorld().submit(new Tick(1){public void execute(){
            stop();
            if(run!=generation)return;
            crossing=false;p.removeAttribute("barrows_crossing");
            if(!Boolean.TRUE.equals(p.getAttribute("stunned")))p.removeAttribute("cantMove");
            if(!p.isOnline())return;
            ActionSender.deleteObject(p,gate.getToReplace().getId(),gate.getToReplace().getLocation().getX(),
                    gate.getToReplace().getLocation().getY(),0,gate.getToReplace().getType(),gate.getToReplace().getRotation());
            ActionSender.sendObject(p,object);
            if(p.isDead()||p.getActivity()!=BarrowsActivity.this||!p.getLocation().equals(origin))return;
            p.teleport(destination,false);updateOverlay();
            if(!looted()&&p.getRandom().nextInt(15)<2){
                List<BarrowsCrypt> remaining=new ArrayList<BarrowsCrypt>();
                for(BarrowsCrypt crypt:getEntities())if(!p.getSettings().getBarrowsKilled().contains(crypt.getNPC().getId()))remaining.add(crypt);
                if(!remaining.isEmpty())spawnBrother(remaining.get(p.getRandom().nextInt(remaining.size())));
            }
        }});
    }
    public boolean itemAction(Player p,Item item,int actionId,String action,Object...params){
        if(item.getId()!=952)return false;
        p.animate(830);
        for(int i=0;i<6;i++)if(BarrowsConstants.HILL_AREA[i].isInArea(p.getLocation())){
            if(p.getFamiliar()!=null){p.sendMessage("You'll have to dismiss your familiar if you want to enter this area.");return true;}
            if(looted())resetRun();
            p.teleport(BarrowsConstants.CRYPT_TELEPORT_LOCATIONS[i],false);updateOverlay();return true;
        }
        p.sendMessage("You find nothing.");return true;
    }
}
