import org.dementhium.model.*;
import org.dementhium.model.combat.*;
import org.dementhium.model.instance.*;
import org.dementhium.model.map.Region;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.player.Player;
import org.dementhium.task.impl.NPCTickTask;

/** Synthetic clear source chunk copied/admitted through the real managed-instance APIs. */
public class NPCMovementInstanceRegression {
 static int checks;
 static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 public static void main(String[] args)throws Exception{
  CombatFixtures.init();org.dementhium.model.misc.GroundItemManager.load();check(org.dementhium.util.MapXTEA.loadPackedFile(),"Existing map keys");
  for(int x=49;x<=51;x++)for(int y=49;y<=51;y++){int id=(x<<8)|y;check(org.dementhium.cache.format.LandscapeParser.parseLandscape(id,org.dementhium.util.MapXTEA.getKey(id)),"Source dependency loaded");}
  Region.forCoords(3200,3200).clippingMasks=new int[4][128][128];
  InstanceManager manager=new InstanceManager(t->{});manager.beginCycle();
  GameInstance instance=null;
  try {
   instance=manager.create(1,1,1,Location.locate(3216,3216,0),i->i.copyMap(402,402,0,0,1,1,new int[]{0},new int[]{0}));
   check(instance!=null,"Managed source chunk copied");
   NPC ordinary=new NPC(1);ordinary.setLocation(Location.locate(3216,3216,0));
   Player p=CombatFixtures.player(ordinary);World.getWorld().getPlayers().add(p);
   instance.enter(p,instance.location(7,6,0));check(InstanceAccess.owner(p)==instance,"Normal instance admission");
   NPC n=instance.spawnNpc(new NPC(2883),instance.location(1,1,0));n.setDoesWalk(false);check(n.size()==3,"Real size-three NPC definition");
   // Expected body edges end at coordinates 7/0, not one body's width short of them.
   for(int[] sample:new int[][]{{4,2,7,6,5,3},{1,2,0,6,0,3},{2,4,6,7,3,5},{2,1,6,0,3,0}}){
    n.getCombatExecutor().reset();n.setLocation(instance.location(sample[0],sample[1],0));n.setOriginalLocation(n.getLocation());
    p.setLocation(instance.location(sample[2],sample[3],0));n.getCombatExecutor().setVictim(p);
    Location expected=instance.location(sample[4],sample[5],0);check(InstanceAccess.canWalk(n,expected),"Full destination body fits managed boundary");
    new NPCTickTask(n).execute();check(n.getLocation().equals(expected),"Actual NPC task reaches permitted instance edge: "+expected+" actual="+n.getLocation());
   }
   n.getWalkingQueue().reset();n.setLocation(instance.location(5,2,0));
   check(!CombatMovement.tryNpcStep(n,1,0),"Full body cannot leave built map");n.getWalkingQueue().getNextEntityMovement();check(n.getLocation().equals(instance.location(5,2,0)),"Rejected edge step stays put");
   n.setLocation(instance.location(2,2,0));Location rock=instance.location(5,2,0);Region.addClipping(rock.getX(),rock.getY(),0,256);
   check(!CombatMovement.tryNpcStep(n,1,0),"Managed full footprint still rejects solid cover");Region.removeClipping(rock.getX(),rock.getY(),0,256);
  }finally{if(instance!=null)instance.close();manager.endCycle();CombatFixtures.clearPlayers();}
  System.out.println("NPC instance movement: "+checks+" checks passed");
 }
}
