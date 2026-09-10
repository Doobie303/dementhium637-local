package org.dementhium.model.npc.impl;
import java.util.*;
import org.dementhium.model.*;
import org.dementhium.model.combat.*;
import org.dementhium.model.definition.NPCDefinition;
import org.dementhium.model.npc.encounter.*;
public class KalphiteQueen extends EncounterNPC {
 private final List<EncounterAdd> workers=new ArrayList<EncounterAdd>();private int nextWorker;
 public KalphiteQueen(int id){super(id);bindArena(3455,9475,3501,9515,0);}
 @Override public int getAttackDelay(){return 4;}
 // Icons represent unusually high defence, not another incoming damage multiplier.
 @Override public Damage updateHit(Mob source,int hit,CombatType type){return new Damage(Math.max(0,hit));}
 private void form(int id){setId(id);setDefinition(NPCDefinition.forId(id));getMask().setSwitchId(id);}
 @Override protected void restoreForm(){form(1158);}
 @Override public void sendDead(){
  if(isDead())return;if(getId()==1160){super.sendDead();return;}
  setDead(true);getWalkingQueue().reset();getPoisonManager().removePoison();animate(6242);
  schedule(4,()->{form(1160);setDead(false);setHp(getMaxHp());animate(6237);nextWorker=clock()+20;});
 }
 // Completed encounter retains the existing 1158 reward table; transition grants nothing.
 @Override public void loot(Mob killer){int form=getId();setId(1158);try{super.loot(killer);}finally{setId(form);}}
 @Override protected void clearAdds(){if(workers!=null){for(EncounterAdd worker:workers)worker.remove();workers.clear();}}
 @Override protected void mechanics(){
  workers.removeIf(n->n.isDead()||n.isHidden());
  if(getId()!=1160||clock()<nextWorker||workers.size()>=2||getOwningInstance()!=null)return;nextWorker=clock()+20;
  if(players().isEmpty())return;Location tile=getLocation().transform(size(),0,0);
  if(!contains(tile)||org.dementhium.model.map.Region.getClippingMask(tile.getX(),tile.getY(),tile.getZ())!=0)return;
  EncounterAdd worker=new EncounterAdd(1156,this);worker.setLocation(tile);worker.setOriginalLocation(tile);workers.add(worker);World.getWorld().getNpcs().add(worker);
 }
 public int workerCount(){return workers.size();}
}
