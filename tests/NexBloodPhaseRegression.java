import java.lang.reflect.Method;
import java.util.*;
import org.dementhium.model.*;
import org.dementhium.model.combat.*;
import org.dementhium.model.map.Region;
import org.dementhium.model.npc.*;
import org.dementhium.model.npc.impl.Nex;
import org.dementhium.model.npc.impl.Nex.*;
import org.dementhium.model.player.Player;
import org.dementhium.tickable.Tick;

/** Blood specials driven by the real arena event and NPC task, with no forced cooldowns. */
public final class NexBloodPhaseRegression {
    static int checks;
    static final List<String> failures = new ArrayList<String>();
    static final NexAreaEvent event = NexAreaEvent.getNexAreaEvent();
    static void check(boolean ok, String message) { checks++; if (!ok && !failures.contains(message)) failures.add(message); }
    static void set(Object o, Class<?> c, String key, Object value) throws Exception { BossEncounterCompletionRegression.field(o,c,key,value); }
    static Object get(Object o, Class<?> c, String key) throws Exception { return BossEncounterCompletionRegression.field(o,c,key); }
    static int counter(Nex n, String key) throws Exception { return (Integer)get(n,Nex.class,key); }
    @SuppressWarnings("unchecked") static List<NPC> reavers() throws Exception { return new ArrayList<NPC>((List<NPC>)get(event,NexAreaEvent.class,"bloodReavers")); }
    static void call(String method, boolean arg) throws Exception { Method m=NexAreaEvent.class.getDeclaredMethod(method,boolean.class);m.setAccessible(true);m.invoke(event,arg); }
    static Player player;
    static Nex setup() throws Exception {
        call("resetEncounter",false);
        BossEncounterCompletionRegression.clean();
        BossEncounterCompletionRegression.landscape(2924,5202);
        Nex n=new Nex(13447);n.setLocation(Location.locate(2924,5202,0));n.setOriginalLocation(n.getLocation());n.setDoesWalk(false);
        World.getWorld().getNpcs().add(n);player=BossEncounterCompletionRegression.player(n);
        player.setLocation(Location.locate(2927,5202,0));
        set(event,NexAreaEvent.class,"nex",n);set(event,NexAreaEvent.class,"spawned",true);
        set(event,NexAreaEvent.class,"minionSpawnDelay",0);set(event,NexAreaEvent.class,"emptyRoomTicks",0);
        set(n,Nex.class,"phase",NexPhase.BLOOD);set(n,Nex.class,"specialPending",true);n.setHp(15000);
        n.getRandom().setSeed(41);((Random)get(null,Nex.class,"r")).setSeed(91);
        BossEncounterCompletionRegression.active.add(event);
        return n;
    }
    static void cycle() throws Exception { BossEncounterCompletionRegression.cycle(); }
    static int now() { return World.getTicks(); }
    static void cadence() throws Exception {
        Nex n=setup();int lastAuto=-1,siphonAt=-1,previousStep=0,previousAutos=0,siphons=0;
        List<NPC> killed=new ArrayList<NPC>();
        check(!player.getAttribute("godmode",false),"Cadence fixture must have godmode disabled");
        for(int t=0;t<170;t++) {
            boolean wasSiphoning=n.isSiphonMode();cycle();
            int step=counter(n,"specialStep"),autos=counter(n,"autoAttacksSinceSpecial");
            if(step!=previousStep) {
                System.out.println("Blood special "+step+" at tick "+now()+", last auto="+lastAuto+", cooldown="+n.getCombatExecutor().getTicks());
                if(lastAuto>=0)check(now()-lastAuto>=4,"Blood special must wait the full four-tick interval after its third auto");
                if(n.isSiphonMode()) {
                    siphonAt=now();siphons++;
                    List<NPC> adds=reavers();check(adds.size()==2,"Each siphon owns exactly two reavers");
                    for(int i=0;i<adds.size();i++) {
                        NPC add=adds.get(i);
                        check(!CombatMovement.standingOn(add.getLocation(),n.getLocation(),add.size(),n.size()),"Reaver footprint must not overlap Nex");
                        for(int j=0;j<i;j++)check(!CombatMovement.standingOn(add.getLocation(),adds.get(j).getLocation(),add.size(),adds.get(j).size()),"Reaver footprints must not overlap each other");
                        for(int x=0;x<add.size();x++)for(int y=0;y<add.size();y++) {
                            Location tile=add.getLocation().transform(x,y,0);
                            check(n.containsArena(tile)&&(Region.getClippingMask(tile.getX(),tile.getY(),0)&(256|0x200000|0x40000))==0,"Whole reaver footprint must be inside walkable arena");
                        }
                        // Ordinary typed damage through the production lethal-hit/removal path.
                        BossEncounterCompletionRegression.damage(player,add,add.getHitPoints());
                        check(add.isDead(),"A killed reaver is dead during siphon");killed.add(add);
                    }
                }
                previousAutos=0;
            }
            if(siphonAt>=0&&now()-siphonAt<8)check(n.isSiphonMode(),"Siphon must last eight full world ticks after its tell");
            if(wasSiphoning&&!n.isSiphonMode())check(now()-siphonAt==8,"Siphon must end exactly on its eighth subsequent tick");
            if(autos>previousAutos){lastAuto=now();System.out.println("Blood auto "+autos+" at tick "+now());}
            for(NPC add:killed)check(add.isDead(),"Killed reavers cannot revive between siphons");
            if(step==previousStep)for(NPC add:reavers())check(add.isDead(),"Reaver death alone cannot spawn a replacement");
            previousStep=step;previousAutos=autos;
        }
        check(siphons>=3,"Natural blood rotation must repeat without a pending-special stall");
        for(NPC add:killed)check(add.destroyed()&&!World.getWorld().getNpcs().contains(add),"Killed reavers finish removal without ordinary respawn");
    }
    static void healingAndCancellation() throws Exception {
        for(boolean god:new boolean[]{false,true}) {
            Nex n=setup();player.setAttribute("godmode",god);cycle();cycle();
            List<NPC> adds=reavers();check(adds.size()==2,"Siphon spawn for healing accounting");
            for(NPC add:adds) { for(int i=0;i<20&&!add.isDead();i++)BossEncounterCompletionRegression.damage(player,add,1000); }
            n.setHp(15000);call("clearBloodReavers",true);
            check(n.getHitPoints()==15000,"Killed reavers provide zero siphon healing, including godmode kills");
        }
        Nex n=setup();cycle();cycle();List<NPC> adds=reavers();
        adds.get(0).setHp(123);adds.get(1).setHp(234);n.setHp(15000);call("clearBloodReavers",true);
        check(n.getHitPoints()==15357,"Living reavers heal only their remaining LP");
        call("clearBloodReavers",true);check(n.getHitPoints()==15357,"Reaver absorption cannot heal twice");
        for(String mode:new String[]{"reset","departure","death"}) {
            n=setup();cycle();cycle();adds=reavers();Tick old=n.getTick("siphon");
            if(mode.equals("reset"))n.resetCombatState();
            else if(mode.equals("departure")){player.setOnline(false);for(int t=0;t<12;t++)cycle();}
            else {set(n,Nex.class,"siphonMode",false);n.setHp(500);BossEncounterCompletionRegression.damage(player,n,100000);for(int t=0;t<4;t++)cycle();}
            check(!n.isSiphonMode()&&!n.hasTick("siphon"),mode+" cancels blood healing/timer");
            for(NPC add:adds)check(!World.getWorld().getNpcs().contains(add),mode+" removes owned reavers");
            if(old!=null)for(int t=0;t<12;t++)old.run();
            check(reavers().isEmpty(),mode+" stale timer cannot create new adds");
        }
    }
    static void phaseCancellation() throws Exception {
        Nex n=setup();
        NPC cruor=new NPC(Nex.CRUOR,Location.locate(2937,5190,0));
        NPC glacies=new NPC(Nex.GLACIES,Location.locate(2912,5190,0));
        cruor.setUnrespawnable(true);glacies.setUnrespawnable(true);
        World.getWorld().getNpcs().add(cruor);World.getWorld().getNpcs().add(glacies);
        set(event,NexAreaEvent.class,"minions",new NPC[]{null,null,cruor,glacies});
        n.setHp(n.getMaximumHitPoints()*4/10);cycle();cycle();
        check(n.isProtectingMinion()&&n.isSiphonMode(),"Real HP threshold opens Cruor while a siphon is pending");
        List<NPC> adds=reavers();Tick old=n.getTick("siphon");
        for(int i=0;i<30&&!cruor.isDead();i++)BossEncounterCompletionRegression.damage(player,cruor,600);
        check(cruor.isDead(),"Actual vulnerable Cruor damage reaches death");
        cycle();cycle();
        check(!n.isSiphonMode()&&!n.hasTick("siphon")&&reavers().isEmpty(),"Cruor death cancels siphon/adds at phase-transition entry");
        for(NPC add:adds)check(add.destroyed(),"Blood-to-ice removes each owned reaver");
        for(int t=0;t<12;t++)cycle();
        check(n.getPhase()==NexPhase.ICE&&!n.isSiphonMode(),"Production phase transition reaches ice with no healing lock");
        if(old!=null)for(int t=0;t<12;t++)old.run();
        check(reavers().isEmpty(),"Cancelled blood timer cannot spawn reavers in ice");
    }
    static void reaverCombat() throws Exception {
        setup();cycle();cycle();List<NPC> adds=reavers();
        Map<NPC,Location> origins=new IdentityHashMap<NPC,Location>();
        for(NPC add:adds)origins.put(add,add.getLocation());
        // Leave normal aggression, combat cooldowns and movement enabled.
        for(int t=0;t<28;t++)cycle();
        // Reavers cast magic from range; a nearby target need not make them walk.
        // Move beyond casting reach to exercise pursuit separately.
        player.setLocation(Location.locate(2925,5211,0));
        for(int t=0;t<8;t++)cycle();
        boolean moved=false,hit=false;
        for(NPC add:adds) {
            moved|=!add.getLocation().equals(origins.get(add));
            hit|=player.getDamageManager().getEnemyHits().containsKey(add);
        }
        check(moved&&hit,"Reavers still acquire, pursue and damage a player without idle wandering");
    }
    public static void main(String[] args) throws Exception {
        CombatFixtures.init();
        org.dementhium.model.misc.GroundItemManager.load();
        cadence();healingAndCancellation();phaseCancellation();reaverCombat();
        System.out.println("Blood contract failures: "+failures);
        call("resetEncounter",false);
        // Existing complete-fight production-path proof, including Cruor -> ice and one fixture loot drop.
        BossEncounterCompletionRegression.nex();
        if(!failures.isEmpty())throw new AssertionError(failures.toString());
        System.out.println("Nex blood phase: "+checks+" checks passed");
    }
}
