import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import org.dementhium.model.Location;
import org.dementhium.model.World;
import org.dementhium.model.combat.impl.npc.ChromaticDragonAction;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.npc.NPCLoader;

/** Natural executor launches and cadence; cache masks do not prove client playback. */
public final class DungeonDragonCombatRegression {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static void dragon(int id,boolean nativeRig)throws Exception{
        NPC dragon=NPCLoader.getNPC(id),target=new NPC(1);
        dragon.setLocation(Location.locate(3200,3200,0));dragon.setAttribute("fightcaves",true);
        target.setLocation(dragon.getLocation().transform(dragon.size(),0,0));target.setAttribute("fightcaves",true);target.setHp(100000);
        check(dragon.getCombatAction() instanceof ChromaticDragonAction,"Registered chromatic handler "+id);
        dragon.getRandom().setSeed(1234);dragon.getCombatExecutor().setVictim(target);
        Field clock=World.class.getDeclaredField("ticksPassed");clock.setAccessible(true);
        int first=-1,previous=-1,launches=0;Set<Integer> sequences=new HashSet<Integer>();
        for(int tick=1;tick<=100;tick++){
            clock.setInt(null,tick);dragon.getMask().reset();dragon.getCombatExecutor().tick();
            if(dragon.getMask().getLastAnimation()==null)continue;
            int sequence=dragon.getMask().getLastAnimation().getId();
            check(sequence==(nativeRig?13155:12252)||sequence==(nativeRig?13152:14245),"Correct native/ordinary sequence "+id+"/"+sequence);
            if(sequence==(nativeRig?13152:14245))check(dragon.getMask().getLastGraphics()!=null&&dragon.getMask().getLastGraphics().getId()==2465,"Dragonfire retains existing graphic "+id);
            if(first<0)first=tick;
            if(previous>=0)check(tick-previous==dragon.getAttackDelay(),"Unchanged natural executor cadence "+id+"/"+(tick-previous));
            previous=tick;launches++;sequences.add(sequence);
        }
        check(first==3,"Initial cooldown preserved "+id+"/"+first);
        check(launches>=15&&sequences.size()==2,"Consecutive melee and breath launches "+id);
        dragon.getCombatExecutor().reset();
        target.setLocation(dragon.getLocation().transform(dragon.size()+4,0,0));dragon.getCombatExecutor().setVictim(target);
        for(int tick=101;tick<=110;tick++){
            clock.setInt(null,tick);dragon.getMask().reset();dragon.getCombatExecutor().tick();
            check(dragon.getMask().getLastAnimation()==null,"Chromatic contact boundary remains intact "+id);
        }
    }
    public static void main(String[] args)throws Exception{
        CombatFixtures.init();
        for(int id=10219;id<=10224;id++)dragon(id,true);
        for(int id=10815;id<=10820;id++)dragon(id,true);
        dragon(53,false);dragon(54,false);dragon(742,false);CombatFixtures.clearPlayers();
        System.out.println("Dungeon dragon combat regression: "+checks+" checks passed");
    }
}
