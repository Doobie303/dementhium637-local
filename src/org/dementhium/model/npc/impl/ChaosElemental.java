package org.dementhium.model.npc.impl;
import java.util.*;
import org.dementhium.model.*;
import org.dementhium.model.combat.*;
import org.dementhium.model.map.Region;
import org.dementhium.model.instance.InstanceAccess;
import org.dementhium.model.npc.encounter.AdvancedNPC;
import org.dementhium.model.player.*;

public class ChaosElemental extends AdvancedNPC {
    public ChaosElemental(int id){super(id);bindArena(3223,3905,3267,3929,0);}
    @Override public int getAttackDelay(){return 5;}
    // Its melee-labelled multicolour projectile deliberately has ranged reach.
    @Override public boolean follow(Mob target,CombatType style){return super.follow(target,CombatType.MAGIC);}
    public boolean disarm(Player p){
        if(!NPCCombatContext.validPair(this,p)||!CombatStatus.statusAllowed(p)||p.getInventory().getFreeSlots()<1)return false;
        List<Integer> slots=new ArrayList<Integer>();
        for(int slot=0;slot<p.getEquipment().getContainer().toArray().length;slot++)if(p.getEquipment().get(slot)!=null)slots.add(slot);
        if(slots.isEmpty())return false;
        int slot=slots.get(getRandom().nextInt(slots.size()));Item item=p.getEquipment().get(slot);
        Container incoming=new Container(1,false);incoming.set(0,new Item(item));
        Container prepared=p.getInventory().getContainer().deepCopy();if(!prepared.tryAddAll(incoming))return false;
        // Publish a complete metadata-preserving transfer on the game thread.
        p.getInventory().getContainer().replaceWith(prepared);p.getEquipment().set(slot,null);
        if(p.getEquipment().hpModifier(item.getDefinition()))p.getSkills().decreaseMaximumLifePoints(p.getEquipment().getModifier(item.getDefinition()));
        p.getEquipment().calculateType();p.getEquipment().refresh();p.getInventory().refresh();return true;
    }
    public boolean relocate(Player p){
        if(!NPCCombatContext.validPair(this,p)||!CombatStatus.statusAllowed(p)||p.getAttribute("cantMove",false)||InstanceAccess.owner(p)!=null)return false;
        List<Location> candidates=new ArrayList<Location>();Location from=p.getLocation();List<org.dementhium.model.npc.NPC> blockers=Region.getLocalNPCs(from,20);
        for(int x=-6;x<=6;x++)for(int y=-6;y<=6;y++){
            if(Math.max(Math.abs(x),Math.abs(y))<3)continue;Location to=from.transform(x,y,0);
            if(!contains(to)||!InstanceAccess.canWalk(p,to)||!org.dementhium.model.map.path.ProjectilePathFinder.clearPath(from,to))continue;
            int mask=Region.getClippingMask(to.getX(),to.getY(),to.getZ());if(mask==-1||(mask&(256|0x200000))!=0)continue;
            boolean occupied=false;for(org.dementhium.model.npc.NPC n:blockers)if(to.getX()>=n.getLocation().getX()&&to.getX()<n.getLocation().getX()+n.size()&&to.getY()>=n.getLocation().getY()&&to.getY()<n.getLocation().getY()+n.size()&&!n.isHidden()){occupied=true;break;}
            if(!occupied)candidates.add(to);
        }
        if(candidates.isEmpty())return false;p.getWalkingQueue().reset();p.teleport(candidates.get(getRandom().nextInt(candidates.size())),false);return true;
    }
}
