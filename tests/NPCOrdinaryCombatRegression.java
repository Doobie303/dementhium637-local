import java.lang.reflect.Field;
import java.util.*;
import org.dementhium.model.*;
import org.dementhium.model.combat.*;
import org.dementhium.model.misc.DamageManager.DamageType;
import org.dementhium.model.npc.*;
import org.dementhium.model.npc.impl.*;
import org.dementhium.model.player.*;
import org.dementhium.task.impl.NPCTickTask;
import org.dementhium.tickable.Tick;

/** Representative canonical generic actions, contact-magic, poison and killable prey. */
public final class NPCOrdinaryCombatRegression {
    static int checks;
    static final List<Tick> active=new ArrayList<Tick>();
    static void check(boolean ok,String reason){checks++;if(!ok)throw new AssertionError(reason);}
    static Field field(Class<?> type,String name)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}
    @SuppressWarnings("unchecked") static LinkedList<Tick> pending()throws Exception{return (LinkedList<Tick>)field(World.class,"ticksToAdd").get(World.getWorld());}
    static final class Fixture {
        final NPC npc;final Player player;
        Fixture(int id)throws Exception{
            CombatFixtures.clearPlayers();active.clear();pending().clear();
            npc=NPCLoader.getNPC(id);npc.setLocation(Location.locate(3216,3216,0));npc.setOriginalLocation(npc.getLocation());npc.setDoesWalk(false);
            npc.getRandom().setSeed(941);
            player=CombatFixtures.player(npc);player.getSkills().set(Skills.SLAYER,99);
            player.getSkills().set(Skills.DEFENCE,1);player.getSkills().set(Skills.MAGIC,1);
            player.getSkills().setMaximumLifePoints(100000);player.getSkills().setHitPoints(100000);
            npc.getCombatExecutor().setVictim(player);
        }
        void step()throws Exception{
            field(World.class,"ticksPassed").setInt(null,World.getTicks()+1);
            active.addAll(pending());pending().clear();for(Iterator<Tick> i=active.iterator();i.hasNext();)if(!i.next().run())i.remove();
            npc.getMask().reset();new NPCTickTask(npc).execute();
        }
        void steps(int n)throws Exception{for(int i=0;i<n;i++)step();}
        void meleePrayer()throws Exception{((boolean[][])field(player.getPrayer().getClass(),"onPrayers").get(player.getPrayer()))[0][CombatType.MELEE.getProtectionPrayer()]=true;}
    }
    static void generic()throws Exception {
        int[] ids={125,2606,190};CombatType[] styles={CombatType.MELEE,CombatType.RANGE,CombatType.MAGIC};
        for(int i=0;i<ids.length;i++){
            Fixture f=new Fixture(ids[i]);
            check(f.npc.getClass()==NPC.class,"Ordinary profile keeps generic NPC "+ids[i]);
            check(f.npc.getCombatAction().getCombatType()==styles[i],"Canonical style reaches executor "+ids[i]);
            if(i>0)f.player.setLocation(f.npc.getLocation().transform(7,0,0));
            f.steps(2);check(f.player.getDamageManager().getHits().isEmpty(),"Initial cooldown has no hit "+ids[i]);
            List<Integer> launches=new ArrayList<Integer>();
            for(int tick=3;tick<=55;tick++){
                f.step();if(f.npc.getMask().getLastAnimation()!=null&&f.npc.getMask().getLastAnimation().getId()==f.npc.getAttackAnimation())launches.add(tick);
                if(tick==3&&i>0)check(f.player.getDamageManager().getHits().isEmpty(),"Generic projectile is delayed "+ids[i]);
            }
            check(launches.size()>5&&launches.get(0)==3,"Natural launches "+ids[i]);
            for(int j=1;j<launches.size();j++)check(launches.get(j)-launches.get(j-1)==f.npc.getAttackDelay(),"Generic cadence "+ids[i]);
            check(f.player.getHitPoints()<100000,"Actual generic damage "+ids[i]);
            final DamageType expected=styles[i].getDamageType();
            check(f.player.getDamageManager().getHits().stream().anyMatch(h->h.getType()==expected),"Delivered style "+ids[i]);
        }
    }
    static void horror()throws Exception {
        for(boolean ward:new boolean[]{false,true}){
            Fixture f=new Fixture(4356);check(f.npc instanceof CaveHorror,"Cave horror alias resolves script");
            f.player.getSkills().set(Skills.SLAYER,57);check(!f.npc.isAttackable(f.player),"Slayer gate");
            f.player.getSkills().set(Skills.SLAYER,58);check(f.npc.isAttackable(f.player),"Slayer boundary");
            if(ward)f.player.getEquipment().set(2,new Item(8923));
            boolean scream=false;for(int t=0;t<150;t++){f.step();if(f.npc.getMask().getLastAnimation()!=null&&f.npc.getMask().getLastAnimation().getId()==4237)scream=true;}
            check(scream!=ward,"Witchwood icon prevents unwarded scream");
            check(f.player.getHitPoints()<100000,"Contact-magic attack delivers damage");
        }
        Fixture protectedHorror=new Fixture(4353);protectedHorror.meleePrayer();protectedHorror.steps(80);
        check(protectedHorror.player.getHitPoints()==100000,"Melee protection blocks contact magic and scream");
        Fixture distant=new Fixture(4353);distant.player.setLocation(distant.npc.getLocation().transform(5,0,0));
        distant.npc.setAttribute("freezeTime",World.getTicks()+100);distant.steps(12);
        check(distant.player.getDamageManager().getHits().isEmpty(),"Horror cannot cast at range when approach is blocked");
    }
    static void poison()throws Exception {
        for(boolean protect:new boolean[]{false,true}){
            Fixture f=new Fixture(134);check(f.npc instanceof PoisonousNPC,"Poison species resolves script");if(protect)f.meleePrayer();
            f.steps(2);check(!f.player.getPoisonManager().isPoisoned(),"No poison before an attack lands");
            f.steps(90);check(f.player.getPoisonManager().isPoisoned()!=protect,"Poison requires actual positive damage");
        }
    }
    static void prey()throws Exception {
        for(int mode=0;mode<3;mode++){
            Fixture f=new Fixture(5080);check(f.npc instanceof Chinchompa,"Hunter prey registry");
            f.npc.retaliate(f.player);f.steps(12);check(f.player.getDamageManager().getHits().isEmpty(),"Prey does not attack, even with forced victim");
            f.npc.getCombatExecutor().reset();if(mode==1)f.player.setLocation(f.npc.getLocation().transform(5,0,0));
            if(mode==2)f.player.setAttribute("godmode",true);
            f.npc.getDamageManager().damage(f.player,10,10,DamageType.MELEE);
            check(f.npc.isDead(),"Prey enters ordinary death");
            check(f.player.getHitPoints()==100000-(mode==0?30:0),"Bounded contact-only prey explosion with immunity");
            int hp=f.player.getHitPoints();f.npc.sendDead();check(f.player.getHitPoints()==hp,"Death blast occurs once");
        }
        Fixture f=new Fixture(5080);f.npc.getCombatExecutor().reset();f.npc.getDamageManager().damage(null,10,10,DamageType.MELEE);
        f.steps(f.npc.getDeathTick()+60);check(!f.npc.isDead()&&f.npc.getHp()==10,"Uncredited prey death and normal respawn");
    }
    public static void main(String[] args)throws Exception{CombatFixtures.init();generic();horror();poison();prey();CombatFixtures.clearPlayers();System.out.println("Ordinary NPC combat: "+checks+" checks passed");}
}
