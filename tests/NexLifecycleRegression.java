import java.lang.reflect.Method;
import java.util.*;
import org.dementhium.model.*;
import org.dementhium.model.npc.impl.Nex;
import org.dementhium.model.npc.impl.Nex.*;
import org.dementhium.model.player.Player;
import org.dementhium.tickable.Tick;

/** Focused phase/callback boundaries; full progression lives in the existing encounter suite. */
public final class NexLifecycleRegression {
    static final NexAreaEvent event = NexAreaEvent.getNexAreaEvent();
    static int checks;
    static List<String> failures = new ArrayList<String>();
    static void check(boolean ok, String message) { checks++; if (!ok && !failures.contains(message)) failures.add(message); }
    static Object get(Object o, Class<?> c, String key) throws Exception { return BossEncounterCompletionRegression.field(o,c,key); }
    static void set(Object o, Class<?> c, String key, Object value) throws Exception { BossEncounterCompletionRegression.field(o,c,key,value); }
    static Object call(String method, Class<?>[] types, Object... args) throws Exception { Method m=NexAreaEvent.class.getDeclaredMethod(method,types);m.setAccessible(true);return m.invoke(event,args); }
    static int value(Nex n,String key) throws Exception { return (Integer)get(n,Nex.class,key); }
    static Player player;
    static Nex setup(NexPhase phase) throws Exception {
        Nex n=NexBloodPhaseRegression.setup();player=NexBloodPhaseRegression.player;player.getWalkingQueue();
        set(n,Nex.class,"phase",phase);n.setHp(n.getMaximumHitPoints());
        set(event,NexAreaEvent.class,"random",new Random(918));event.setTime(1);
        // Only phase setup is synthetic. Attacks, cooldowns and task ordering are production paths.
        return n;
    }
    static void cycle() throws Exception { BossEncounterCompletionRegression.cycle(); }
    static void cadence() throws Exception {
        for(NexPhase phase:new NexPhase[]{NexPhase.SMOKE,NexPhase.SHADOW,NexPhase.ICE}) {
            Nex n=setup(phase);int lastAuto=-1,previousStep=0,previousAutos=0;
            for(int t=0;t<170;t++) {
                cycle();int step=value(n,"specialStep"),autos=value(n,"autoAttacksSinceSpecial");
                if(step!=previousStep) {
                    if(lastAuto>=0)check(World.getTicks()-lastAuto>=4,phase+" special waits four ticks after last auto");
                    previousAutos=0;
                }
                if(autos>previousAutos)lastAuto=World.getTicks();
                previousStep=step;previousAutos=autos;
            }
            check(previousStep>=4 && lastAuto>0,phase+" repeatedly returns from specials to normal attacks");
        }
    }
    static void containment() throws Exception {
        Nex n=setup(NexPhase.ICE);set(n,Nex.class,"specialStep",1);
        cycle();int start=World.getTicks();
        for(int elapsed=1;elapsed<=5;elapsed++) {
            cycle();List<?> ice=(List<?>)get(event,NexAreaEvent.class,"containmentLocations");
            check(elapsed==5 ? !ice.isEmpty() : ice.isEmpty(),"Containment appears exactly five ticks after tell");
            check(value(n,"autoAttacksSinceSpecial")==0,"Containment resolves before next ordinary attack");
        }
        for(int t=0;t<6;t++)cycle();
        check(value(n,"autoAttacksSinceSpecial")>0,"Containment releases normal combat");
        check(((List<?>)get(event,NexAreaEvent.class,"containmentLocations")).isEmpty(),"Containment expires after five further ticks");
    }
    static void invalidate(Nex n,String mode) throws Exception {
        if(mode.equals("phase"))event.changePhase(NexPhase.FINAL);
        else if(mode.equals("reset"))n.resetCombatState();
        else if(mode.equals("death")){n.setHp(500);BossEncounterCompletionRegression.damage(player,n,100000);}
        else if(mode.equals("departure")){player.teleport(Location.locate(3200,3200,0), false);player.teleport(Location.locate(2927,5202,0), false);}
        else {call("resetEncounter",new Class<?>[]{boolean.class},false);}
    }
    static void positivePullAndPhaseImpact() throws Exception {
        Nex n=setup(NexPhase.SMOKE);set(n,Nex.class,"specialPending",false);
        BossEncounterCompletionRegression.active.remove(event);
        int hp=player.getHitPoints();check(event.drag(player),"Positive pull starts");n.getCombatExecutor().setVictim(null);
        cycle();cycle();check(player.getHitPoints()==hp,"Pull damage waits until after landing");
        cycle();int hit=hp-player.getHitPoints();check(hit>=300&&hit<=400,"Owned pull landing still deals its one hit");
        for(int t=0;t<3;t++)cycle();check(hp-player.getHitPoints()==hit,"Pull damage occurs exactly once");
        n=setup(NexPhase.SHADOW);set(n,Nex.class,"specialPending",false);
        for(int t=0;t<10 && value(n,"autoAttacksSinceSpecial")==0;t++)cycle();
        check(value(n,"autoAttacksSinceSpecial")==1,"Normal executor launches shadow ranged attack");
        hp=player.getHitPoints();event.changePhase(NexPhase.FINAL);
        BossEncounterCompletionRegression.active.remove(event);n.getCombatExecutor().setVictim(null);
        for(int t=0;t<3;t++)cycle();check(player.getHitPoints()==hp,"Phase entry cancels launched ordinary projectile");
        n=setup(NexPhase.SHADOW);set(n,Nex.class,"specialStep",1);cycle();hp=player.getHitPoints();
        cycle();check(player.getHitPoints()==hp,"Darkness waits two full ticks before first pulse");
        cycle();check(player.getHitPoints()<hp,"Darkness first pulse lands on tick two");
        event.changePhase(NexPhase.FINAL);hp=player.getHitPoints();
        for(int t=0;t<3;t++)cycle();check(player.getHitPoints()==hp,"Phase entry cancels remaining darkness pulses");
    }
    static void callbacks() throws Exception {
        for(String mode:new String[]{"phase","reset","death","departure","replacement"}) {
            Nex n=setup(NexPhase.SMOKE);set(n,Nex.class,"specialPending",false);
            check(event.drag(player),"Pull starts for "+mode);
            invalidate(n,mode);int hp=player.getHitPoints();
            // Disable arena selection, retaining actual world/player/NPC scheduler order.
            BossEncounterCompletionRegression.active.remove(event);n.getCombatExecutor().setVictim(null);
            for(int t=0;t<4;t++)cycle();
            if (!mode.equals("death")) check(player.getHitPoints()==hp,mode+" cancels pending pull damage");
            check(!player.hasTick("nex_drag"),mode+" cancels pull callback before damage (Wrath may still hit)");
            check(player.getForceWalk()==null,mode+" releases pull movement");

            n=setup(NexPhase.SMOKE);cycle();Tick virus=player.getTick("nex_virus");
            check(virus!=null,"Natural opening casts virus");invalidate(n,mode);
            BossEncounterCompletionRegression.active.remove(event);n.getCombatExecutor().setVictim(null);
            double prayer=player.getSkills().getPrayerPoints();
            cycle();check(player.getSkills().getPrayerPoints()==prayer,mode+" cancels virus drain");
        }
        Nex n=setup(NexPhase.ICE);cycle();
        // Prison target leaves and returns before the six-tick expiry; location alone is insufficient.
        player.teleport(Location.locate(3200,3200,0), false);player.teleport(Location.locate(2927,5202,0), false);
        BossEncounterCompletionRegression.active.remove(event);n.getCombatExecutor().setVictim(null);
        int hp=player.getHitPoints();for(int t=0;t<6;t++)cycle();
        check(player.getHitPoints()==hp,"Out-and-back departure cancels prison damage");
        check(((List<?>)get(event,NexAreaEvent.class,"icePrisonLocations")).isEmpty(),"Departed prison still cleans up shared ice");
    }
    public static void main(String[] args) throws Exception {
        CombatFixtures.init();org.dementhium.model.misc.GroundItemManager.load();
        cadence();containment();positivePullAndPhaseImpact();callbacks();
        call("resetEncounter",new Class<?>[]{boolean.class},false);
        if(!failures.isEmpty())throw new AssertionError(failures.toString());
        System.out.println("Nex lifecycle: "+checks+" checks passed");
    }
}
