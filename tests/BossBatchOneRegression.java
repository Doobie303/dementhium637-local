import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.lang.reflect.*;
import java.util.*;
import org.dementhium.cache.*;
import org.dementhium.model.*;
import org.dementhium.model.combat.*;
import org.dementhium.model.combat.impl.npc.*;
import org.dementhium.model.definition.*;
import org.dementhium.model.npc.*;
import org.dementhium.model.npc.impl.*;
import org.dementhium.model.player.*;
import org.dementhium.net.GameSession;
import org.dementhium.content.skills.Prayer;

public class BossBatchOneRegression {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static Player player(NPC n){
        Player p=new Player(new GameSession(null),new PlayerDefinition("boss-test","unused"));
        for(int skill:new int[]{Skills.ATTACK,Skills.STRENGTH,Skills.DEFENCE,Skills.RANGED,Skills.MAGIC,Skills.PRAYER})p.getSkills().setLevelAndXP(skill,99,13034431);
        p.getSkills().setMaximumLifePoints(1000);p.getSkills().setHitPoints(1000);p.getSkills().setPrayerPoints(99,false);
        p.getBonuses().calculate();p.setOnline(true);p.setLocation(Location.locate(n.getLocation().getX()+n.size(),n.getLocation().getY(),0));return p;
    }
    static NPC npc(int id){NPC n=NPCLoader.getNPC(id);n.setLocation(Location.locate(3200,3200,0));n.setOriginalLocation(n.getLocation());if(n instanceof org.dementhium.model.npc.encounter.EncounterNPC)((org.dementhium.model.npc.encounter.EncounterNPC)n).bindArena(3190,3190,3230,3230,0);if(n instanceof org.dementhium.model.npc.godwars.GodWarsNPC){org.dementhium.model.npc.godwars.GodWarsNPC g=(org.dementhium.model.npc.godwars.GodWarsNPC)n;new org.dementhium.model.npc.godwars.GodWarsRoom(g.getGodWarsType(),3190,3190,3230,3230,0,()->0).attach(g);}return n;}
    static boolean[][] prayers(Player p)throws Exception{Field f=Prayer.class.getDeclaredField("onPrayers");f.setAccessible(true);return (boolean[][])f.get(p.getPrayer());}
    static Object field(Object o,String name)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
    static void finish(CombatAction a){for(int i=0;i<10;i++)if(a.endSession())return;throw new AssertionError("Never finalized");}
    static List<byte[]> records(Path file)throws Exception {
        byte[] data=Files.readAllBytes(file);ByteBuffer b=ByteBuffer.wrap(data);List<byte[]> result=new ArrayList<byte[]>();
        while(b.hasRemaining()) {int start=b.position(),id=b.getShort();if(id!=-1){check(id==result.size(),"Record index");b.getShort();while(b.get()!=0){};b.position(b.position()+59);}result.add(Arrays.copyOfRange(data,start,b.position()));}
        return result;
    }
    static void data()throws Exception {
        List<byte[]> before=records(Paths.get("build/boss-batch1-before/NDE/NPCDefinitions.bin")),after=records(Paths.get("NDE/NPCDefinitions.bin"));
        check(before.size()==13488&&after.size()==before.size(),"Definition count preserved");
        Set<Integer> changed=new HashSet<Integer>(Arrays.asList(2881,2882,2883,6265,6260,6247,6203,6222));
        for(int id=0;id<before.size();id++)check(Arrays.equals(before.get(id),after.get(id))!=changed.contains(id),"Only intended packed records differ: "+id);
        check(NPCDefinition.forId(6265).getHitpoints()==1460,"Grimspike LP");
        int[] ids={2881,2882,2883};CombatType[] styles={CombatType.RANGE,CombatType.MAGIC,CombatType.MELEE};
        for(int i=0;i<ids.length;i++){NPC n=npc(ids[i]);check(n instanceof DagannothKing,"Actual registry dispatch");check(n.getHitPoints()==(i==0?2560:2550),"Loaded King HP");check(n.getAttackDelay()==4,"King cadence");check(n.getCombatAction().getCombatType()==styles[i],"Distinct style");}
        Map<Integer,Integer> count=new HashMap<Integer,Integer>();boolean ignore=false;
        for(String line:Files.readAllLines(Paths.get("data/npcs/npcspawns.txt"))) {
            if(line.startsWith("//")||line.isEmpty())continue;
            if(line.contains("/*")){ignore=true;continue;}if(ignore){if(line.contains("*/"))ignore=false;continue;}
            int id=Integer.parseInt(line.split(" ")[0]);if(id==2881||id==2882||id==2883||id==8133)count.put(id,count.containsKey(id)?count.get(id)+1:1);
        }
        for(int id:new int[]{2881,2882,2883,8133})check(count.get(id)==1,"One spawn for "+id);
        for(int id:new int[]{2851,2852,2854,2855,2856,7060,7063})check(CacheManager.getData(20,id>>7,id&127).length>0,"Native animation "+id);
        for(int id:new int[]{162,163,475,1200,1218})check(CacheManager.getData(21,id>>8,id&255).length>0,"Native graphic "+id);
    }
    static void attacks()throws Exception {
        for(int id:new int[]{2881,2882,2883}){
            NPC n=npc(id);CombatAction a=n.getCombatAction();Set<CombatType> seen=new HashSet<CombatType>();int successes=0;
            for(int seed=0;seed<200;seed++){
                Player p=player(n);n.getRandom().setSeed(seed);p.getRandom().setSeed(seed+11);
                Interaction it=new Interaction(n,p);a.setInteraction(it);check(a.commenceSession(),"Launch "+id+" source="+n.getLocation()+" victim="+p.getLocation()+" dead="+n.isDead()+","+p.isDead()+" hidden="+n.isHidden()+","+p.isHidden()+" path="+org.dementhium.model.map.path.ProjectilePathFinder.clearPath(n.getLocation(),p.getLocation()));
                check(it.getDamage()!=null,"Every attack owns damage "+id);
                CombatType type=(CombatType)field(it.getDamage(),"type");seen.add(type);
                int cap=id==2881?300:id==2882?610:id==2883?280:type==CombatType.MELEE?600:350;
                check(it.getDamage().getMaximum()==cap,"Correct maximum "+id);
                check(it.getDamage().getHit()>=-1&&it.getDamage().getHit()<=cap,"Damage bounded");
                int hp=p.getHitPoints();check(hp==1000,"No launch damage");finish(a);
                check(p.getHitPoints()==hp-Math.max(0,it.getDamage().getHit()),"Actual typed damage "+id);
                if(p.getHitPoints()<hp)successes++;
                hp=p.getHitPoints();finish(a);check(p.getHitPoints()==hp,"Cannot replay");
            }
            check(successes>0,"Boss deals damage "+id);
            check(seen.size()==(id==6260?2:1),"All intended styles only "+id);
        }
    }
    static void boundaries()throws Exception {
        NPC n=npc(2882);CombatAction a=n.getCombatAction();
        for(int mode=0;mode<6;mode++){
            Player p=player(n);Interaction it=new Interaction(n,p);a.setInteraction(it);check(a.commenceSession(),"Cancel launch");
            if(mode==0)p.setOnline(false);if(mode==1)n.setDead(true);if(mode==2)p.setLocation(Location.locate(3300,3300,0));
            if(mode==3)p.setLocation(Location.locate(3200,3200,1));if(mode==4)p.markInstanceTransition();if(mode==5)p.setHidden(true);
            int hp=p.getHitPoints();finish(a);check(p.getHitPoints()==hp,"Cancelled boundary "+mode);n.setDead(false);
        }
        n=npc(2883);a=n.getCombatAction();Player p=player(n);p.setLocation(Location.locate(3210,3200,0));a.setInteraction(new Interaction(n,p));check(!a.commenceSession(),"No remote melee");
        n=npc(2882);a=n.getCombatAction();p=player(n);p.setLocation(Location.locate(3216,3200,0));a.setInteraction(new Interaction(n,p));check(!a.commenceSession(),"No excessive magic reach");
        // Interleave two targets on the same reusable action, with independently owned rolls.
        n=npc(2881);a=n.getCombatAction();Player first=player(n),second=player(n);
        n.getRandom().setSeed(1);Interaction one=new Interaction(n,first);a.setInteraction(one);check(a.commenceSession(),"First overlapping attack");
        n.getRandom().setSeed(2);Interaction two=new Interaction(n,second);a.setInteraction(two);check(a.commenceSession(),"Second overlapping attack");
        a.setInteraction(one);finish(a);check(second.getHitPoints()==1000,"First attack keeps victim");
        check(first.getHitPoints()==1000-Math.max(0,one.getDamage().getHit()),"First damage survives interleave");
        a.setInteraction(two);finish(a);check(second.getHitPoints()==1000-Math.max(0,two.getDamage().getHit()),"Second damage survives interleave");
        for(int id:new int[]{2881,2882,2883})for(boolean immune:new boolean[]{false,true}){
            n=npc(id);a=n.getCombatAction();p=player(n);CombatType style=a.getCombatType();
            if(immune)p.setAttribute("godmode",true);else prayers(p)[0][style.getProtectionPrayer()]=true;
            a.setInteraction(new Interaction(n,p));check(a.commenceSession(),"Protected launch");finish(a);check(p.getHitPoints()==1000,"Matching prayer/godmode "+id);
        }
    }
    static void maps()throws Exception {
        check(org.dementhium.util.MapXTEA.loadPackedFile(),"Map keys load");
        for(int[] spawn:new int[][]{{2881,2903,4447,0},{2882,2915,4449,0},{2883,2923,4438,0},{8133,2988,4384,2}}){
            int region=((spawn[1]>>6)<<8)|(spawn[2]>>6);
            check(org.dementhium.cache.format.LandscapeParser.parseLandscape(region,org.dementhium.util.MapXTEA.getMapKeys().get(region)),"Spawn landscape "+spawn[0]);
            NPC n=npc(spawn[0]);
            for(int x=spawn[1];x<spawn[1]+n.size();x++)for(int y=spawn[2];y<spawn[2]+n.size();y++){
                int mask=org.dementhium.model.map.Region.getClippingMask(x,y,spawn[3]);
                check(mask!=-1&&(mask&(256|0x200000))==0,"Walkable boss footprint "+spawn[0]+" "+x+","+y+" mask="+mask);
            }
        }
    }
    static void mitigationAndTiming()throws Exception {
        NPC n=npc(2881);CombatAction a=n.getCombatAction();Player p=player(n);
        p.setLocation(Location.locate(3210,3200,0));Interaction it=new Interaction(n,p);a.setInteraction(it);a.execute();
        check(it.getState()==org.dementhium.util.misc.CycleState.FINALIZE,"Real lifecycle launch");
        a.execute();check(p.getHitPoints()==1000,"Projectile waits first tick");a.execute();check(p.getHitPoints()==1000,"Projectile waits second tick");
        a.execute();check(it.getState()==org.dementhium.util.misc.CycleState.FINISHED,"Real lifecycle finalizes third tick");
        check(p.getHitPoints()==1000-Math.max(0,it.getDamage().getHit()),"One lifecycle impact");
        for(int id:new int[]{2881,2882,2883}){
            n=npc(id);a=n.getCombatAction();int positive=0;
            for(int seed=0;seed<25;seed++){
                p=player(n);n.getRandom().setSeed(seed);p.getRandom().setSeed(seed+11);
                p.getEquipment().set(5,new Item(13740));p.getBonuses().calculate();
                it=new Interaction(n,p);a.setInteraction(it);check(a.commenceSession(),"Shield launch");
                int raw=it.getDamage().getHit();finish(a);
                check(p.getHitPoints()==1000-(raw<0?0:raw-(int)Math.ceil(raw*0.30)),"Divine applied once at impact "+id);
                if(raw>0)positive++;
            }
            check(positive>0,"Shield exercises successful attacks "+id);
        }
        n=npc(2881);a=n.getCombatAction();p=player(n);p.setLocation(Location.locate(3210,3200,0));
        org.dementhium.model.map.Region.addClipping(3206,3200,0,256);
        a.setInteraction(new Interaction(n,p));check(!a.commenceSession(),"Solid obstacle blocks launch");
        org.dementhium.model.map.Region.removeClipping(3206,3200,0,256);
    }

    public static void main(String[] args)throws Exception {
        Cache.init();ItemDefinition.init();NPCDefinition.init();
        Method load=NPCLoader.class.getDeclaredMethod("loadCustomizations");load.setAccessible(true);load.invoke(null);
        data();maps();org.dementhium.model.map.Region testRegion=org.dementhium.model.map.Region.forCoords(3200,3200);testRegion.clippingMasks=new int[4][128][128];Field clipped=org.dementhium.model.map.Region.class.getDeclaredField("clipped");clipped.setAccessible(true);clipped.setBoolean(testRegion,true);attacks();boundaries();mitigationAndTiming();System.out.println("Boss batch 1: "+checks+" checks passed.");
    }
}
