import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.dementhium.content.areas.CoordinateEvent;
import org.dementhium.model.Location;
import org.dementhium.model.Mob;
import org.dementhium.model.World;
import org.dementhium.model.combat.CombatType;
import org.dementhium.model.definition.NPCDefinition;
import org.dementhium.model.map.Region;
import org.dementhium.model.misc.DamageManager.DamageType;
import org.dementhium.model.npc.NPCLoader;
import org.dementhium.model.npc.impl.Strykewyrm;
import org.dementhium.model.player.Player;
import org.dementhium.model.player.Skills;
import org.dementhium.net.message.MessageBuilder;
import org.dementhium.net.packethandlers.NpcOption;
import org.dementhium.task.impl.NPCTickTask;
import org.dementhium.tickable.Tick;

/** Real option packet/area event and task order. Only loot delivery is recorded. */
public final class StrykewyrmRegression {
    private static int checks;
    private static final List<Tick> active=new ArrayList<Tick>();
    private static Strykewyrm previous;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static Field field(Class<?> type,String name)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}
    @SuppressWarnings("unchecked") private static LinkedList<Tick> pending()throws Exception{return (LinkedList<Tick>)field(World.class,"ticksToAdd").get(World.getWorld());}
    private static class LootWyrm extends Strykewyrm {
        int rewards,paidId;Mob paid;
        LootWyrm(int id){super(id);}
        @Override public void loot(Mob killer){rewards++;paidId=getId();paid=killer;}
    }
    private static final class Fixture {
        final Strykewyrm npc;final Player player;int tick;
        Fixture(int id,boolean recorder)throws Exception{
            if(previous!=null)World.getWorld().getNpcs().remove(previous);
            CombatFixtures.clearPlayers();active.clear();pending().clear();
            npc=recorder?new LootWyrm(id):(Strykewyrm)NPCLoader.getNPC(id);previous=npc;
            npc.setLocation(Location.locate(3216,3224,0));npc.setOriginalLocation(npc.getLocation());npc.setDoesWalk(false);npc.getRandom().setSeed(824);
            Region region=npc.getLocation().getRegion();region.clippingMasks=new int[4][128][128];region.setClipped(true);
            World.getWorld().getNpcs().add(npc);
            player=CombatFixtures.player(npc);player.getSkills().set(Skills.SLAYER,99);player.getSkills().set(Skills.DEFENCE,1);player.getSkills().set(Skills.MAGIC,1);
            player.getSkills().setMaximumLifePoints(100000);player.getSkills().setHitPoints(100000);
        }
        int animation(){return npc.getMask().getLastAnimation()==null?-1:npc.getMask().getLastAnimation().getId();}
        void step()throws Exception{
            tick++;field(World.class,"ticksPassed").setInt(null,World.getTicks()+1);
            active.addAll(pending());pending().clear();for(Iterator<Tick> i=active.iterator();i.hasNext();)if(!i.next().run())i.remove();
            // Relevant PlayerTickTask order, then the complete production NPC task.
            player.processTicks();player.getWalkingQueue().getNextEntityMovement();
            npc.getMask().reset();new NPCTickTask(npc).execute();
        }
        void steps(int count)throws Exception{for(int i=0;i<count;i++)step();}
        void until(int end)throws Exception{while(tick<end)step();}
        void attack(){npc.getCombatExecutor().setVictim(player);}
        void investigate(){new NpcOption().handlePacket(player,new MessageBuilder(28).writeByteS(0).writeLEShort(npc.getIndex()).toMessage());}
        int warning()throws Exception{for(int i=0;i<100;i++){step();if(animation()==12794)return tick;}throw new AssertionError("No natural burrow warning");}
        void prayer(CombatType type)throws Exception{((boolean[][])field(player.getPrayer().getClass(),"onPrayers").get(player.getPrayer()))[0][type.getProtectionPrayer()]=true;}
        void wall(int x){for(int y=3200;y<=3250;y++)npc.getLocation().getRegion().clippingMasks[0][x&127][y&127]=256|0x20000;}
    }
    /** Reads the real coordinate event captured by World's scheduled wrapper. */
    private static CoordinateEvent areaEvent(Player player)throws Exception{
        Tick tick=player.getTick("area_event");check(tick!=null,"Option packet queued World area event");
        for(Field f:tick.getClass().getDeclaredFields())if(CoordinateEvent.class.isAssignableFrom(f.getType())){f.setAccessible(true);return (CoordinateEvent)f.get(tick);}
        throw new AssertionError("Missing packet-created coordinate event");
    }
    private static void activation()throws Exception{
        int[] mounds={9462,9464,9466},levels={93,77,73};
        for(int i=0;i<mounds.length;i++){
            Fixture f=new Fixture(mounds[i],false);
            check(!f.npc.isAttackable(f.player),"Dormant mound cannot be attacked "+mounds[i]);
            f.npc.retaliate(f.player);f.attack();f.steps(8);check(f.player.getHitPoints()==100000,"Dormant mound never fights a forced target");
            f.player.getSkills().set(Skills.SLAYER,levels[i]-1);f.investigate();
            check(areaEvent(f.player).inArea(),"Option1 retains unequal destination X/Y");
            f.player.processTicks();check(f.npc.getId()==mounds[i],"Real option route enforces Slayer level "+levels[i]);
            f.player.getSkills().set(Skills.SLAYER,levels[i]);f.investigate();f.player.processTicks();
            int id=mounds[i]+1;NPCDefinition expected=NPCDefinition.forId(id);
            check(f.npc.getId()==id&&f.npc.getDefinition()==expected,"Activation changes runtime identity and effective definition "+id);
            check(f.npc.getHp()==expected.getHitpoints()&&f.npc.getMaxHp()==expected.getHitpoints(),"Activation loads actual active HP "+id);
            check(f.npc.getMask().getSwitchId()==id&&f.animation()==12795,"Activation sends active model and emerge sequence");
            check(f.npc.getCombatExecutor().getVictim()==f.player,"Activation acquires investigating player");
            f.npc.setHp(expected.getHitpoints()-10);f.npc.activate(f.player);check(f.npc.getHp()==expected.getHitpoints()-10,"Repeated activation cannot heal a live wyrm");
        }
        Fixture stale=new Fixture(9466,false);stale.investigate();stale.player.markInstanceTransition();stale.player.processTicks();
        check(stale.npc.getId()==9466,"World area event rejects changed instance membership");
    }
    private static void ordinary()throws Exception{
        for(int id:new int[]{9463,9465,9467})for(boolean distant:new boolean[]{false,true}){
            Fixture f=new Fixture(id,false);check(f.npc.getId()==id&&f.npc.isAttackable(f.player),"Direct administrative active variant "+id);
            if(distant)f.player.setLocation(f.npc.getLocation().transform(8,0,0));
            f.attack();f.steps(2);check(f.player.getDamageManager().getHits().isEmpty(),"Ordinary initial cooldown remains intact");
            f.steps(120);check(f.player.getHitPoints()<100000,"Natural ordinary attacks land "+id);
            DamageType spell=id==9465?DamageType.RANGE:DamageType.MAGE;
            check(f.player.getDamageManager().getHits().stream().anyMatch(h->h.getType()==spell),"Family secondary style reaches impacts "+id);
            if(!distant)check(f.player.getDamageManager().getHits().stream().anyMatch(h->h.getType()==DamageType.MELEE),"Contact permits melee "+id);
            else check(f.player.getDamageManager().getHits().stream().noneMatch(h->h.getType()==DamageType.MELEE),"Distant attacks never use unreachable melee "+id);
        }
        Fixture covered=new Fixture(9465,false);covered.player.setLocation(covered.npc.getLocation().transform(8,0,0));covered.wall(3221);covered.attack();covered.steps(15);
        check(covered.player.getHitPoints()==100000,"Projectile-blocking terrain remains effective");
    }
    private static void burrow()throws Exception{
        for(boolean evade:new boolean[]{false,true}){
            Fixture f=new Fixture(9463,false);f.player.setLocation(f.npc.getLocation().transform(8,0,0));f.attack();
            int warning=f.warning();check(warning==3+6*f.npc.getAttackDelay(),"Burrow follows six naturally scheduled ordinary attacks");
            int before=f.player.getHitPoints();if(evade)f.player.setLocation(f.player.getLocation().transform(0,1,0));
            f.steps(2);check(f.player.getHitPoints()==before,"Three-tick warning cannot hit early");
            f.step();check(f.player.getHitPoints()==before-(evade?0:300),"Burrow lands once on original occupied tile");
            int after=f.player.getHitPoints();f.step();check(f.player.getHitPoints()==after,"Burrow impact is not replayed");
        }
    }
    private static void cancellation()throws Exception{
        for(boolean special:new boolean[]{false,true})for(int mode=0;mode<5;mode++){
            Fixture f=new Fixture(9463,false);f.player.setLocation(f.npc.getLocation().transform(8,0,0));f.attack();
            if(special)f.warning();else f.until(3);
            int before=f.player.getHitPoints();
            if(mode==0)f.npc.resetCombatState();
            if(mode==1)f.player.setOnline(false);
            if(mode==2)f.player.setLocation(f.player.getLocation().transform(0,0,1));
            if(mode==3)f.player.markInstanceTransition();
            if(mode==4)f.player.setLocation(f.npc.getLocation().transform(30,0,0));
            f.steps(5);check(f.player.getHitPoints()==before,"Reset/departure rejects delayed "+(special?"burrow":"ordinary")+" damage "+mode);
        }
    }
    private static void poison()throws Exception{
        for(boolean protectedMagic:new boolean[]{false,true}){
            Fixture f=new Fixture(9467,false);f.player.setLocation(f.npc.getLocation().transform(8,0,0));
            if(protectedMagic)f.prayer(CombatType.MAGIC);f.attack();f.steps(2);
            check(!f.player.getPoisonManager().isPoisoned(),"Jungle poison waits for a landed attack");
            for(int i=0;i<150&&!f.player.getPoisonManager().isPoisoned();i++)f.step();
            check(f.player.getPoisonManager().isPoisoned()!=protectedMagic,"Jungle poison requires positive ordinary damage");
            if(!protectedMagic){
                check(f.player.getPoisonManager().getPoisoner()==f.npc,"Jungle poison retains its attacker");
                check(field(f.player.getPoisonManager().getClass(),"amount").getInt(f.player.getPoisonManager())==40,"Jungle poison starts at the explicit 40-LP estimate");
            }
        }
    }
    private static void death()throws Exception{
        for(boolean moundOrigin:new boolean[]{false,true})for(boolean credited:new boolean[]{false,true}){
            Fixture f=new Fixture(moundOrigin?9462:9463,true);if(moundOrigin)check(f.npc.activate(f.player),"Mound activates before death scenario");
            f.player.setLocation(f.npc.getLocation().transform(8,0,0));f.attack();f.warning();int hp=f.player.getHitPoints();
            f.npc.getDamageManager().damage(credited?f.player:null,f.npc.getMaxHp(),f.npc.getMaxHp(),DamageType.MELEE);
            check(f.npc.isDead(),"Real lethal hit reaches ordinary death");f.steps(f.npc.getDeathTick());
            LootWyrm recorder=(LootWyrm)f.npc;
            check(recorder.rewards==(credited?1:0)&&(!credited||recorder.paid==f.player&&recorder.paidId==9463),"One permitted reward uses active identity before mound restoration");
            check(f.player.getHitPoints()==hp,"Death cancels burrow already in flight");
            f.steps(60);check(!f.npc.isDead()&&!f.npc.isHidden(),"Normal respawn completes");
            check(f.npc.getId()==(moundOrigin?9462:9463),"Respawn restores the original dormant/direct form");
            check(f.npc.getDefinition()==NPCDefinition.forId(f.npc.getId())&&f.npc.getHp()==f.npc.getMaxHp(),"Respawn identity, definition and HP agree");
            check(recorder.rewards==(credited?1:0),"Respawn does not repeat rewards");
            if(moundOrigin)check(!f.npc.isAttackable(f.player),"Respawned mound is dormant again");
        }
    }
    public static void main(String[] args)throws Exception{
        CombatFixtures.init();activation();ordinary();burrow();cancellation();poison();death();
        if(previous!=null)World.getWorld().getNpcs().remove(previous);CombatFixtures.clearPlayers();
        System.out.println("Strykewyrm regression: "+checks+" checks passed");
    }
}
