import java.util.*;
import java.lang.reflect.*;
import org.dementhium.cache.*;
import org.dementhium.cache.format.LandscapeParser;
import org.dementhium.model.*;
import org.dementhium.model.map.*;
import org.dementhium.model.definition.*;
import org.dementhium.model.npc.*;
import org.dementhium.model.npc.impl.*;
import org.dementhium.model.player.*;
import org.dementhium.model.combat.*;
import org.dementhium.net.*;
import org.dementhium.net.message.*;
import org.dementhium.content.activity.impl.*;
import org.dementhium.content.activity.impl.barrows.*;
import org.dementhium.util.MapXTEA;

public class BarrowsRegression {
    static int checks;
    static void check(boolean b,String message){checks++;if(!b)throw new AssertionError(message);}
    static Object field(Object o,String name)throws Exception{
        Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);
    }
    static void set(Object o,String name,Object v)throws Exception{
        Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);f.set(o,v);
    }
    public static void main(String[] args)throws Exception{
        Cache.init();NPCDefinition.init();ItemDefinition.init();MapXTEA.loadPackedFile();
        for(int x=55;x<=56;x++)for(int y=151;y<=152;y++){
            int region=x<<8|y;LandscapeParser.parseLandscape(region,MapXTEA.getMapKeys().get(region));
        }
        for(int layout=0;layout<32;layout++){
            BarrowsTunnels t=new BarrowsTunnels(layout);
            check(t.getGates().size()==64,"all 64 gates must survive lookup");
            int puzzle=0,openOuter=0;
            for(Gate g:t.getGates().values()){
                check(t.get(g.getId(),g.getLocation().getX(),g.getLocation().getY(),0)==g,"exact gate identity");
                if(t.isPuzzleGate(g))puzzle++;
                int edge=BarrowsTunnels.perimeterEdge(g);
                if(edge>=0&&!g.isClosed())openOuter++;
                if(edge<0&&BarrowsTunnels.centralSide(g)<0)check(g.isClosed(),"outer boundary must never open");
                GameObject object=g.getLocation().getGameObject(g.getId());
                check(object!=null,"door must exist in cache");
                Location a=g.getLocation();
                int rot=object.getRotation();
                Location b=a.transform(rot==0?-1:rot==2?1:0,rot==1?1:rot==3?-1:0,0);
                check(BarrowsRules.crossingDestination(object,a).equals(b),"outbound crossing "+a+" rotation "+rot);
                check(BarrowsRules.crossingDestination(object,b).equals(a),"return crossing");
                if(!g.isClosed()){
                    check((Region.getClippingMask(a.getX(),a.getY(),0)&0x1280100)==0,"open door tile solid");
                    check((Region.getClippingMask(b.getX(),b.getY(),0)&0x1280100)==0,"door destination solid");
                }
            }
            check(puzzle==4,"one central corridor must be open");
            check(openOuter==28,"seven outer links must be open");
            boolean[] visited=new boolean[8];visited[0]=true;
            for(int n=0;n<8;n++)for(int e=0;e<8;e++)if(e!=layout/4&&(visited[e]||visited[(e+1)%8]))visited[e]=visited[(e+1)%8]=true;
            for(boolean v:visited)check(v,"every outer room must remain reachable");
        }
        for(int id=6713;id<=6736;id++)check(CacheManager.getData(7,id,0)!=null,"puzzle model exists "+id);
        check(CacheManager.getRealContainerChildCount(3,25)==15,"native puzzle widget count");
        check(BarrowsRules.prayerDrain(0)==8 && BarrowsRules.prayerDrain(6)==14,"haunting strength");
        check(BarrowsRules.dharokMaximum(1000,1000)==290,"Dharok full HP");
        check(BarrowsRules.dharokMaximum(1,1000)==579,"Dharok low HP");
        check(BarrowsRules.addPotential(990,56)==1000,"potential cap");
        check(BarrowsRules.count(Arrays.asList(2025,2025,999))==1,"unique valid brothers only");
        List<Integer> all=Arrays.asList(2030,2026,2025,2027,2028,2029);
        check(BarrowsRules.rewardPotential(all,0)==656,"brothers alone contribute their combat levels");
        check(BarrowsRules.rewardPotential(all,344)==1000,"brother plus creature potential cap");
        Random highest=new Random(){public int nextInt(int bound){return bound-1;}};
        check(BarrowsRules.roll(highest,all,0).get(0).getId()==560,"brothers alone can yield death runes");
        check(BarrowsRules.roll(highest,all,1000).get(0).getId()==1149,"maximum potential can yield dragon med");
        check(BarrowsRules.roll(highest,all,0).get(0).getAmount()==57,"death rune stacks boosted 50 percent");
        check(BarrowsRules.roll(highest,all,1000).get(0).getAmount()==1,"dragon med quantity unchanged");
        Random coinRoll=new Random(){
            public int nextInt(int bound){return bound==668?0:bound-1;}
        };
        check(BarrowsRules.roll(coinRoll,all,0).get(0).getAmount()==3280,"coin stacks boosted 25 percent");
        Random rng=new Random(2011);
        int chests=0,multi=0;
        for(int n=0;n<100000;n++){
            int unique=0;
            for(Item item:BarrowsRules.roll(rng,all,n%1001)){
                check(item.getAmount()>0,"positive reward");
                if(item.getId()>=4708&&item.getId()<=4759&&item.getId()!=4740)unique++;
            }
            if(unique>0)chests++;
            if(unique>1)multi++;
        }
        double expected=1-Math.pow(67.0/68.0,7);
        check(Math.abs(chests/100000.0-expected)<0.003,"equipment probability");
        check(multi>0,"multiple equipment rolls possible");
        Random alwaysZero=new Random(){public int nextInt(int bound){return 0;}};
        for(Item i:BarrowsRules.roll(alwaysZero,Arrays.asList(2026),1000))
            check(i.getId()==4716,"only killed brother equipment");
        check(BarrowsRules.roll(alwaysZero,Collections.<Integer>emptyList(),1000).get(0).getId()==995,"zero brothers no equipment");

        Player p=new Player(new GameSession(null),new PlayerDefinition("barrowstest","unused")); p.setOnline(true);
        p.setLocation(Location.locate(3551,9694,0));
        BarrowsActivity activity=new BarrowsActivity(p);activity.initializeActivity();
        for(BarrowsCrypt c:activity.getEntities())check(c.getNPC() instanceof BarrowBrother,"specialized brother created");
        NPC brother=activity.getEntities().get(0).getNPC();
        brother.setAttribute("barrowsOwner",p);
        activity.recordKill(brother);activity.recordKill(brother);
        check(p.getSettings().getBarrowsKillcount()==1,"duplicate kill ignored");
        NPC foreign=new BarrowBrother(2026);foreign.setAttribute("barrowsOwner",p);activity.recordKill(foreign);
        check(p.getSettings().getBarrowsKillcount()==1,"foreign NPC cannot credit owner");
        p.setAttribute("looted_barrows_request_shake",true);
        activity.recordKill(new NPC(2031));
        check(p.getSettings().getBarrowsKillcount()==1,"looted run cannot gain potential");
        p.removeAttribute("looted_barrows_request_shake");
        p.getSkills().setPrayerPoints(99,false);
        p.getSkills().drainPray(14);
        check(p.getSkills().getPrayerPoints()==85,"normal prayer drain");
        p.setAttribute("godmode",true);p.getSkills().drainPray(14);
        check(p.getSkills().getPrayerPoints()==85,"godmode prayer immunity");
        check(p.updateHit(brother,200,CombatType.MELEE).getHit()==0,"godmode incoming hit immunity");
        p.removeAttribute("godmode");

        BarrowBrother verac=new BarrowBrother(2030);
        ((boolean[][])field(p.getPrayer(),"onPrayers"))[0][19]=true;
        check(p.updateHit(verac,200,CombatType.MELEE).getHit()==0,"ordinary Verac hit blocked");
        set(verac,"defilerHit",true);
        check(p.updateHit(verac,200,CombatType.MELEE).getHit()==200,"Verac proc bypasses prayer");
        p.setAttribute("godmode",true);
        check(p.updateHit(verac,200,CombatType.MELEE).getHit()==0,"Verac proc cannot bypass godmode");
        p.removeAttribute("godmode");
        // Closed/cancelled and stale puzzle answers cannot unlock a door.
        BarrowsTunnels tunnels=(BarrowsTunnels)field(activity,"tunnels");
        Gate puzzleGate=null;
        for(Gate g:tunnels.getGates().values())if(tunnels.isPuzzleGate(g)){puzzleGate=g;break;}
        GameObject door=puzzleGate.getLocation().getGameObject(puzzleGate.getId());
        p.setLocation(door.getLocation());
        Method show=BarrowsActivity.class.getDeclaredMethod("showPuzzle",GameObject.class);show.setAccessible(true);
        show.invoke(activity,door);
        int answer=(Integer)field(activity,"puzzleAnswer");
        ActionSender.sendCloseInterface(p);
        activity.answerPuzzle(answer);
        check(!(Boolean)field(activity,"puzzleSolved"),"cancelled answer ignored");
        show.invoke(activity,door);
        int wrong=2==(Integer)field(activity,"puzzleAnswer")?3:2;
        int old=p.getSettings().getTunnelId();
        activity.answerPuzzle(wrong);
        check(p.getSettings().getTunnelId()%4!=old%4,"wrong answer changes central route");
        tunnels=(BarrowsTunnels)field(activity,"tunnels");
        for(Gate g:tunnels.getGates().values())if(tunnels.isPuzzleGate(g)){puzzleGate=g;break;}
        door=puzzleGate.getLocation().getGameObject(puzzleGate.getId());p.setLocation(door.getLocation());
        show.invoke(activity,door);
        activity.answerPuzzle((Integer)field(activity,"puzzleAnswer"));
        check((Boolean)field(activity,"puzzleSolved"),"correct answer unlocks");
        p.getSettings().setBarrowsPotential(789);
        p.setAttribute("looted_barrows_request_shake",true);
        org.jboss.netty.buffer.ChannelBuffer save=org.jboss.netty.buffer.ChannelBuffers.dynamicBuffer();
        p.save(save);
        Player restored=new Player(new GameSession(null),new PlayerDefinition("restored","unused"));
        restored.load(save.toByteBuffer());
        check(restored.getSettings().getBarrowsPotential()==789,"potential save/load");
        check(restored.getAttribute("looted_barrows_request_shake",false),"looted marker save/load");
        Player legacy=new Player(new GameSession(null),new PlayerDefinition("legacy","unused"));
        legacy.load(save.slice(0,save.writerIndex()-8).toByteBuffer());
        check(legacy.getSettings().getBarrowsPotential()==0,"legacy save loads without extension");
        Method reset=BarrowsActivity.class.getDeclaredMethod("resetRun");reset.setAccessible(true);reset.invoke(activity);
        check(p.getSettings().getBarrowsKilled().isEmpty()&&p.getSettings().getBarrowsPotential()==0,"fresh run clears rewards");
        check(p.getSettings().getTunnelEntranceId()>=0&&p.getSettings().getTunnelEntranceId()<6,"fresh run valid entrance");
        check(!p.getAttribute("looted_barrows_request_shake",false),"fresh run can loot");
        // Force known successful procs to exercise each brother's actual impact path.
        p.getPrayer().closeAllPrayers();
        p.getSkills().setMaxLifePoints(900);
        for(BarrowsCrypt crypt:activity.getEntities()){
            BarrowBrother b=(BarrowBrother)crypt.getNPC();
            b.setAttribute("barrowsOwner",p);b.setAttribute("isSpawned",true);
            b.setLocation(p.getLocation());b.setHp(500);
            p.getSkills().setHitPoints(1000);
            p.getSkills().setLevel(Skills.STRENGTH,99);
            p.getSkills().setLevel(Skills.AGILITY,99);
            p.getWalkingQueue().setRunEnergy(100);
            CombatAction action=b.getCombatAction();
            action.setInteraction(new Interaction(b,p));
            check(action.commenceSession(),"brother starts "+b.getId());
            set(action,"roll",100);set(action,"debuff",-1);set(action,"proc",true);
            action.endSession();
            check(p.getHitPoints()==900,"brother impact "+b.getId()+" hp="+p.getHitPoints()+" immunity="+p.getAttribute("hitImmunity",-1));
            if(b.getId()==2027)check(b.getHitPoints()==600,"Guthan heals applied damage");
            if(b.getId()==2029)check(p.getWalkingQueue().getRunEnergy()==80,"Torag drains energy");
            if(b.getId()==2028)check(p.getSkills().getLevel(Skills.AGILITY)==80,"Karil drains agility");
            if(b.getId()==2025)check(p.getSkills().getLevel(Skills.STRENGTH)==94,"Ahrim drains strength");
            check(!b.isAttackable(restored),"foreign owner cannot attack");
            p.setAttribute("godmode",true);
            action.setInteraction(new Interaction(b,p));
            check(action.commenceSession(),"godmode brother starts");
            set(action,"roll",100);set(action,"debuff",-1);set(action,"proc",true);
            int hp=p.getHitPoints(),strength=p.getSkills().getLevel(Skills.STRENGTH),agility=p.getSkills().getLevel(Skills.AGILITY);
            double energy=p.getWalkingQueue().getRunEnergy();
            action.endSession();
            check(p.getHitPoints()==hp&&p.getSkills().getLevel(Skills.STRENGTH)==strength
                &&p.getSkills().getLevel(Skills.AGILITY)==agility&&p.getWalkingQueue().getRunEnergy()==energy,"godmode suppresses hit and procs");
            p.removeAttribute("godmode");
        }
        System.out.println("PASS "+checks+" checks; equipment chests="+chests+"/100000; multi="+multi);
    }
}

