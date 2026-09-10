package org.dementhium.event.impl.interfaces;
import org.dementhium.content.activity.impl.DuelActivity;
import org.dementhium.content.activity.impl.DuelActivity.State;
import org.dementhium.content.activity.impl.duel.*;
import org.dementhium.content.activity.impl.duel.DuelConfigurations.Rules;
import org.dementhium.event.*;
import org.dementhium.model.*;
import org.dementhium.model.player.Player;
import org.dementhium.net.ActionSender;
import org.dementhium.util.InputHandler;
public class DuelArenaListener extends EventListener {
    public void register(EventManager manager){for(int id:new int[]{626,628,631,634,640})manager.registerInterfaceListener(id,this);}
    public boolean interfaceOption(Player p,int inter,int button,int slot,int item,int opcode){
        if(inter==640){
            if(button==18||button==21){p.setAttribute("isStaking",false);ActionSender.sendConfig(p,283,67108864);}
            else if(button==19||button==22){p.setAttribute("isStaking",true);ActionSender.sendConfig(p,283,134217728);}
            else if(button==20)DuelChallenge.send(p);return true;
        }
        if(inter==634){ActionSender.sendCloseInterface(p);DuelRecovery.claim(p);return true;}
        if(!(p.getActivity() instanceof DuelActivity))return false;
        DuelActivity duel=(DuelActivity)p.getActivity();if(!duel.owns(p))return false;
        if(inter==626){
            if(duel.getCurrentState()!=State.SECOND_SCREEN||opcode!=6)return true;
            if(button==53)duel.acceptSecond(p);else if(button==55||button==7)duel.decline(p,false);return true;
        }
        if(!duel.editable(p))return true;
        if(inter==628)return itemOption(p,duel,true,slot,item,opcode);
        if(inter!=631)return false;
        if(opcode==6){
            if(button==93)return duel.accept(p);
            if(button==21||button==100)return duel.decline(p,false);
            Rules rule=null;
            if(button>=27&&button<=50)rule=Rules.values()[(button-27)/2];
            else {
                int[] buttons={54,55,56,58,59,60,61,64,63,62,57};
                for(int n=0;n<buttons.length;n++)if(button==buttons[n])rule=Rules.values()[12+n];
            }
            if(rule!=null)return duel.getDuelConfigurations().swapRule(p,duel.getOpponent(p),rule);
        }
        if(button==94)return itemOption(p,duel,false,slot,item,opcode);
        return true;
    }
    private boolean itemOption(Player p,DuelActivity duel,boolean add,int slot,int id,int opcode){
        Stakes stake=duel.stakeOf(p);Item selected=add?p.getInventory().get(slot):stake.getContainer().get(slot);
        if(selected==null||selected.getId()!=id)return false;
        if(opcode==58){p.sendMessage(selected.getDefinition().getExamine());return true;}
        if(opcode==46){
            InputHandler.requestIntegerInput(p,add?5:6,"Please enter an amount:");
            p.setAttribute("duelInputSession",duel.getId());p.setAttribute("duelInputRevision",duel.getRevision());
            p.setAttribute("duelInputHash",selected.getHash());p.setAttribute("duelInputItem",id);
            p.setAttribute("duelInputSlot",slot);return true;
        }
        int amount=opcode==6?1:opcode==13?5:opcode==0?10:opcode==15?Integer.MAX_VALUE:0;
        return add?stake.stake(id,slot,amount):stake.removeSlot(slot,id,amount);
    }
    public static void input(Player p,int amount,boolean add){
        if(!(p.getActivity() instanceof DuelActivity))return;
        DuelActivity duel=(DuelActivity)p.getActivity();
        String session=p.getAttribute("duelInputSession");p.removeAttribute("duelInputSession");
        if(!duel.editable(p)||!duel.getId().equals(session)||duel.getRevision()!=p.<Long>getAttribute("duelInputRevision",-1L))return;
        int slot=p.getAttribute("duelInputSlot",-1),id=p.getAttribute("duelInputItem",-1);
        Item item=add?p.getInventory().get(slot):duel.stakeOf(p).getContainer().get(slot);
        if(item==null||item.getId()!=id||item.getHash()!=p.<Long>getAttribute("duelInputHash",-1L))return;
        if(add)duel.stakeOf(p).stake(id,slot,amount);else duel.stakeOf(p).removeSlot(slot,id,amount);
    }
}
