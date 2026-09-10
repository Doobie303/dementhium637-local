import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.dementhium.content.instance.*;
import org.dementhium.content.minigames.*;
import org.dementhium.model.*;
import org.dementhium.model.instance.*;
import org.dementhium.model.map.region.RegionBuilder;
import org.dementhium.model.misc.*;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.player.*;
import org.dementhium.tickable.Tick;

/** Sustained real-cache, packet-producing content workload. Simulated time; measured durations use System.nanoTime.
 * Does not start World.run, sockets, account IO or the combat executor; not a production throughput certification. */
public final class InstanceLoadRegression {
    static final List<Tick> tasks=new ArrayList<Tick>();
    static final AtomicLong clock=new AtomicLong();
    static final Player[] players=new Player[10];
    static final long[] durations=new long[46000]; static int measuredCycles;
    static int checks, batches, sessions, packets, maxNpcs, maxTasks, maxObjects, maxDrops, maxRegions;
    static boolean measure;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static void cycle(InstanceManager m,Runnable action) {
        long started=System.nanoTime();m.beginCycle();
        try {
            m.drainRequests();m.maintain();action.run();
            for(Tick t:new ArrayList<Tick>(tasks))if(!t.run())tasks.remove(t);
            for(Player p:players){new PlayerUpdate(p).sendUpdate();p.getMask().reset();}
            maxNpcs=Math.max(maxNpcs,(int)m.total("npcs"));maxTasks=Math.max(maxTasks,(int)m.total("tasks"));
            maxObjects=Math.max(maxObjects,(int)m.total("objects"));maxDrops=Math.max(maxDrops,(int)m.total("drops"));maxRegions=Math.max(maxRegions,RegionBuilder.getDynamicRegionCount());
        } finally {m.endCycle();clock.addAndGet(600000000L);}
        if(measure)durations[measuredCycles++]=System.nanoTime()-started;
        for(Player p:players){packets+=InstancePartyRegression.messages.get(p).size();InstancePartyRegression.messages.get(p).clear();}
    }
    static void batch(InstanceManager m,int sequence) {
        final InstanceExamples[] examples=new InstanceExamples[4];
        final PartyInstanceExample[] parties=new PartyInstanceExample[2];
        final FightCavesSession[] caves=new FightCavesSession[2];
        cycle(m,()->{
            for(int n=0;n<4;n++) {players[n].getInventory().getContainer().clear();examples[n]=InstanceExamples.start(m,players[n],n%2==0?InstanceExamples.Kind.QUEST:InstanceExamples.Kind.SKILL);check(examples[n]!=null,"example start");}
        });
        cycle(m,()->{
            for(int n=0;n<2;n++) {
                players[4+n*2].getInventory().getContainer().clear();players[5+n*2].getInventory().getContainer().clear();
                parties[n]=PartyInstanceExample.start(m,players[4+n*2],InstanceParty.LeaderDeparture.PROMOTE_OLDEST);check(parties[n]!=null,"party start");
                parties[n].getParty().invite(players[4+n*2],players[5+n*2],20);parties[n].join(players[5+n*2]);
                players[8+n].getInventory().getContainer().clear();caves[n]=FightCavesSession.start(m,players[8+n],sequence%2==0?0:2);check(caves[n]!=null,"caves start");
            }
            check(m.getInstances().size()==8&&m.total("members")==10,"eight simultaneous mixed sessions and ten members");
        });
        for(int tick=0;tick<20;tick++){
            final int phase=tick;
            cycle(m,()->{
                if(phase==0) {
                    for(int n=0;n<4;n++)InstanceContentRegression.click(examples[n],players[n],n%2==0?"clue":"resource");
                    for(int n=0;n<2;n++)parties[n].expand(players[4+n*2]);
                }
                if(phase==1) for(int n=0;n<4;n+=2) {InstanceContentRegression.click(examples[n],players[n],"chest");check(examples[n].getQuestRewards()==1,"quest progressed");}
                if(phase==2) for(int n=0;n<2;n++){GameInstance i=parties[n].getInstance();i.spawnDrop(new GroundItem(players[4+n*2],new Item(995,3),players[4+n*2].getLocation(),false,true,GroundItemManager.groundItemIndex++));}
                if(phase==4) for(int n=0;n<2;n++)FightCavesInstanceRegression.clearWave(caves[n]);
                if(phase==8) for(int n=0;n<2;n++){players[4+n*2].getSkills().setHitPoints(0);players[4+n*2].getSkills().sendDead();}
                if(phase==14) for(int n=0;n<2;n++)check(!players[4+n*2].isDead()&&parties[n].getDeaths(players[4+n*2])==1,"party internal respawn");
                if(phase==15) for(int n=1;n<4;n+=2)check(examples[n].getHarvests()==1&&!examples[n].isDepleted(),"harvest and regrowth");
                if(phase==16 && sequence%3==0) {InstancePartyRegression.connections.get(players[4]).set(false);m.maintain();check(parties[0].getParty().getLeader()==players[5],"disconnect promotes leader");InstancePartyRegression.connections.get(players[4]).set(true);}
            });
        }
        cycle(m,()->{
            check(m.closeAll(),"mixed cleanup completes");
            check(m.getInstances().isEmpty()&&RegionBuilder.getAllocationCount()==0&&RegionBuilder.getDynamicRegionCount()==0,"all allocations returned to baseline");
            check(World.getWorld().getNpcs().size()==0&&GroundItemManager.getGroundItems().isEmpty(),"world NPC/drop baseline");
            for(String resource:new String[]{"members","npcs","objects","drops","tasks","chunkPlanes","reservedRegions"})check(m.total(resource)==0,"resource baseline "+resource);
            for(Player p:players)check(InstanceAccess.owner(p)==null&&InstanceExamples.get(p)==null&&PartyInstanceExample.get(p)==null&&FightCaves.getSession(p)==null,"content references cleared");
        });
        check(tasks.isEmpty(),"scheduler drained");sessions+=8;batches++;
    }
    static long retainedHeap() throws Exception {System.gc();Thread.sleep(50);System.gc();return Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();}
    public static void main(String[] args)throws Exception {
        InstanceOperationsRegression.init();
        for(int n=0;n<players.length;n++)players[n]=InstancePartyRegression.player("load"+n);
        InstanceManager manager=new InstanceManager(tasks::add,InstanceLimits.load(),clock::get);
        for(int n=0;n<25;n++)batch(manager,n);
        long baseline=retainedHeap();measure=true;
        PrintWriter csv=new PrintWriter("build/instance-operations/load-memory.csv");csv.println("measured_batches,total_sessions,retained_bytes");csv.println("0,"+sessions+","+baseline);
        long end=baseline;long start=System.nanoTime();
        for(int n=0;n<2000;n++){
            batch(manager,n+25);
            if((n+1)%250==0){end=retainedHeap();csv.println((n+1)+","+sessions+","+end);csv.flush();System.out.println("Load progress: "+(n+1)+"/2000 batches; heap MiB="+(end/1048576));}
        }
        csv.close();Arrays.sort(durations);
        try(PrintWriter latency=new PrintWriter("build/instance-operations/load-cycle-nanos.csv")){latency.println("sorted_cycle_nanos");for(long n:durations)latency.println(n);}
        check(end-baseline<32L*1048576,"post-warmup retained heap growth under 32 MiB");
        for(Player p:players)World.getWorld().getPlayers().remove(p);
        System.out.printf(Locale.ROOT,"InstanceLoadRegression: %d checks; %d sessions (%d warmup), %d measured cycles; p50/p95/p99/max %.3f/%.3f/%.3f/%.3f ms; retained heap %.2f -> %.2f MiB; wall %.2fs; packets=%d; peak npc/task/object/drop/regions=%d/%d/%d/%d/%d%n",
            checks,sessions,200,measuredCycles,durations[measuredCycles/2]/1e6,durations[(int)(measuredCycles*.95)]/1e6,durations[(int)(measuredCycles*.99)]/1e6,durations[measuredCycles-1]/1e6,baseline/1048576.0,end/1048576.0,(System.nanoTime()-start)/1e9,packets,maxNpcs,maxTasks,maxObjects,maxDrops,maxRegions);
    }
}


