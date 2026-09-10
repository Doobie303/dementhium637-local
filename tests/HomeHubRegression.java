import java.nio.file.*;
import java.util.*;
import org.dementhium.cache.format.LandscapeParser;
import org.dementhium.content.DialogueManager;
import org.dementhium.content.home.HomeHub;
import org.dementhium.content.home.HomeHub.*;
import org.dementhium.content.minigames.gambler.*;
import org.dementhium.io.PlayerLoader;
import org.dementhium.model.*;
import org.dementhium.model.instance.InstanceManager;
import org.dementhium.model.map.*;
import org.dementhium.model.map.path.*;
import org.dementhium.model.misc.GroundItemManager;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.player.*;
import org.dementhium.util.MapXTEA;

/** Real cache and existing service handlers; all money tests use an isolated account root. */
public class HomeHubRegression extends GamblerRegression {
    static NPC find(Service s){for(NPC n:World.getWorld().getNpcs())if(n!=null&&n.getId()==s.id&&n.getLocation().equals(s.location()))return n;throw new AssertionError(s);}
    static Player at(Location l){Player p=p();p.setLocation(l);return p;}
    static void click(Player p,Portal portal){
        // Separate visits after the normal post-teleport movement lock has ended.
        p.removeAttribute("cantMove");p.setLocation(portal.location().transform(1,0,0));
        objectAction(p,portal.location().getGameObjectType(10));check(p.getAttribute("homeMenu")!=null,"menu opened "+portal);
    }
    static void objectAction(Player p,GameObject o){
        try{
            java.lang.reflect.Method method=org.dementhium.net.packethandlers.ObjectPacketHandler.class.getDeclaredMethod("handleObject",Player.class,int.class,GameObject.class,Location.class,org.dementhium.cache.format.CacheObjectDefinition.class,org.dementhium.net.message.Message.class);
            method.setAccessible(true);method.invoke(new org.dementhium.net.packethandlers.ObjectPacketHandler(),p,o.getId(),o,o.getLocation(),o.getDefinition(),new org.dementhium.net.message.MessageBuilder(76).toMessage());
        }catch(Exception failure){throw new AssertionError("object dispatch",failure);}
    }
    static void pick(Player p,int slot){
        int[] stages=p.getAttribute("nextDialougeStage");check(stages!=null&&slot>=0&&slot<stages.length,"owned choice "+slot);
        org.dementhium.net.message.Message packet=new org.dementhium.net.message.MessageBuilder(4).writeShort(0).writeLEShort(slot+2).writeLEShort(224+stages.length*2).toMessage();
        new org.dementhium.net.packethandlers.DialogueHandler().handlePacket(p,packet);
    }
    static boolean path(Player p,Location l){
        if((Region.getClippingMask(l.getX(),l.getY(),0)&0x240100)!=0)return false;
        PathState state=World.getWorld().doPath(new DefaultPathFinder(),p,l.getX(),l.getY(),false,false,true);
        return state!=null&&state.isRouteFound()&&!state.getPoints().isEmpty()
            &&state.getPoints().getLast().equals(Position.create(l.getX(),l.getY(),l.getZ()));
    }
    static void approach(Player p,Location l,String label){
        boolean found=false;for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}})found|=path(p,l.transform(d[0],d[1],0));
        check(found,"reachable cardinal approach: "+label+" "+l);
    }
    static void geometry()throws Exception{
        Player fresh=new Player(new org.dementhium.net.GameSession(null),new org.dementhium.model.definition.PlayerDefinition("fresh-home","unused"));
        check(fresh.getLocation().equals(Mob.DEFAULT)&&Mob.DEFAULT.equals(Location.locate(2337,3803,0)),"new player starts at Neitiznot");
        java.lang.reflect.Field options=org.dementhium.cache.format.CacheNPCDefinition.class.getDeclaredField("options");options.setAccessible(true);
        for(Service s:Service.values()){
            String[] labels=(String[])options.get(org.dementhium.cache.format.CacheNPCDefinition.forID(s.id));
            check(labels!=null&&labels[0]!=null,"native first-click option present: "+s);
        }
        check((Region.getClippingMask(Mob.DEFAULT.getX(),Mob.DEFAULT.getY(),0)&0x240100)==0,"clear arrival");
        for(Service s:Service.values())check((Region.getClippingMask(s.x,s.y,0)&0x240100)==0,"clear NPC footprint "+s);
        for(Portal portal:Portal.values()){
            org.dementhium.cache.format.CacheObjectDefinition d=org.dementhium.cache.format.CacheObjectDefinition.forId(portal.id);
            for(int x=0;x<d.getSizeX();x++)for(int y=0;y<d.getSizeY();y++){
                Location l=portal.location().transform(x,y,0);
                check(l.getGameObjectType(10)==null&&(Region.getClippingMask(l.getX(),l.getY(),0)&0x240100)==0,"unoccupied object footprint "+portal+" "+l);
            }
        }
        HomeHub.spawnObjects();HomeHub.spawnObjects();HomeHub.spawnServices();int count=World.getWorld().getNpcs().size();HomeHub.spawnServices();
        check(count==World.getWorld().getNpcs().size(),"idempotent services");
        Player p=at(Mob.DEFAULT);check(!p.isInWilderness()&&!p.inPVPZone()&&!p.inSafePk(),"home is outside PvP areas");
        for(Service s:Service.values()){check(find(s)!=null,"service exists "+s);approach(p,s.location(),s.name());
            Location near=org.dementhium.net.packethandlers.NpcOption.getNearLocation(p.getLocation(),find(s));
            check(path(p,near),"normal NPC click from arrival: "+s+" approach "+near);
        }
        java.lang.reflect.Method finder=org.dementhium.net.packethandlers.ObjectPacketHandler.class.getDeclaredMethod("findObjectPath",Player.class,GameObject.class);finder.setAccessible(true);
        for(Portal portal:Portal.values()){
            approach(p,portal.location(),portal.name());
            PathState route=(PathState)finder.invoke(new org.dementhium.net.packethandlers.ObjectPacketHandler(),p,portal.location().getGameObjectType(10));
            check(route!=null&&route.isRouteFound(),"actual object click route "+portal);
            Position end=route.getPoints().getLast();check(Math.abs(end.getX()-portal.x)<=1&&Math.abs(end.getY()-portal.y)<=1,"object path reaches interaction range "+portal);
        }
        for(int x:new int[]{2334,2335,2338,2339}){
            Location l=Location.locate(x,3808,0);check(l.getGameObjectType(10).getId()==21301,"native chest retained");approach(p,l,"bank");
            objectAction(p,l.getGameObjectType(10));check(p.getAttribute("inBank",false),"native bank handler opens chest");p.closeAll(false,true);
        }
    }
    static void landings(){
        Set<Integer> parsed=new HashSet<Integer>();
        List<String> blocked=new ArrayList<String>();
        Player visitor=p();
        for(Portal portal:new Portal[]{Portal.PVM,Portal.PVP,Portal.ACTIVITIES})for(Destination d:HomeHub.destinations(portal)){
            Location l=d.location;int region=((l.getX()>>6)<<8)|(l.getY()>>6);
            if(parsed.add(region))LandscapeParser.parseLandscape(region,MapXTEA.getMapKeys().get(region));
            if((Region.getClippingMask(l.getX(),l.getY(),l.getZ())&0x240100)!=0){
                blocked.add(d.name+" "+l);
                for(int dx=-2;dx<=2;dx++)for(int dy=-2;dy<=2;dy++)if((Region.getClippingMask(l.getX()+dx,l.getY()+dy,l.getZ())&0x240100)==0)System.out.println("Clear nearby "+d.name+": "+l.transform(dx,dy,0));
            }
            visitor.setLocation(l);check(d.dangerous||!visitor.isInWilderness()&&!visitor.inPVPZone(),"dangerous area labelled: "+d.name);
            if(d.name.startsWith("Safe PvP"))check(visitor.inSafePk(),"safe PvP uses safe-death area");
        }
        check(blocked.isEmpty(),"clear destination landings: "+blocked);
    }
    static void menus()throws Exception{
        Player a=at(Mob.DEFAULT),b=at(Mob.DEFAULT);
        check(!HomeHub.select(a,0),"unsolicited stage rejected");
        click(a,Portal.PVM);pick(a,0);check(a.getLocation().equals(HomeHub.destinations(Portal.PVM).get(0).location),"PvM destination");
        check(!HomeHub.select(a,0),"replayed stage rejected");
        click(a,Portal.PVP);Location before=a.getLocation();pick(a,2);check(a.getLocation().equals(before),"danger requires confirmation");
        check(!HomeHub.select(b,0),"other player cannot confirm");pick(a,1);check(a.getLocation().equals(before),"danger cancel");
        click(a,Portal.PVP);pick(a,2);pick(a,0);check(a.getLocation().equals(HomeHub.destinations(Portal.PVP).get(2).location),"confirmed Wilderness arrival");
        click(a,Portal.PVM);before=a.getLocation();a.setAttribute("teleblock",World.getTicks()+10);pick(a,0);check(a.getLocation().equals(before),"teleblock respected");a.removeAttribute("teleblock");
        click(a,Portal.PVM);a.setLocation(Mob.DEFAULT.transform(30,0,0));check(!HomeHub.select(a,0),"remote choice rejected");
        click(a,Portal.PVM);advance(101);check(!HomeHub.select(a,0),"expired choice rejected");
        click(a,Portal.PVM);a.closeAll(false,true);check(!HomeHub.select(a,0),"close cancels choice");
        click(a,Portal.PVM);a.markInstanceTransition();check(!HomeHub.select(a,0),"instance transition invalidates menu");
        click(a,Portal.PVM);a.setAttribute("cantMove",true);check(!HomeHub.select(a,0),"movement lock invalidates menu");a.removeAttribute("cantMove");
        click(a,Portal.PVM);DialogueManager.processNextDialogue(a,99);check(a.getAttribute("homeMenu")==null,"invalid button closes owned menu without indexing outside array");
        click(a,Portal.PVM);a.setOnline(false);check(!HomeHub.select(a,0),"logout rejects choice");a.setOnline(true);
        click(a,Portal.ACTIVITIES);pick(a,3);pick(a,2);check(a.getLocation().equals(Location.locate(2344,3691,0)),"legacy services reachable");
        // Different simultaneous menus keep independent pages and destinations.
        click(a,Portal.PVM);click(b,Portal.PVP);pick(a,3);pick(b,0);
        check(b.getLocation().equals(HomeHub.destinations(Portal.PVP).get(0).location),"second player's page independent");pick(a,0);
        check(a.getLocation().equals(HomeHub.destinations(Portal.PVM).get(3).location),"first player's next page retained");
        check(!HomeHub.handleNpc(a,new NPC(Service.MELEE.id,Service.MELEE.location())),"unregistered NPC not accepted as home service");
        for(Service s:new Service[]{Service.MELEE,Service.RANGED,Service.MAGIC,Service.SUPPLIES}){
            int[] ids=s==Service.MELEE?new int[]{549,551,552}:s==Service.RANGED?new int[]{550,682,683}:s==Service.MAGIC?new int[]{553,546,461}:new int[]{520,521,519};
            for(int i=0;i<3;i++){a.removeAttribute("cantMove");a.setLocation(s.location().transform(0,-1,0));check(DialogueManager.handle(a,find(s)),"merchant menu");pick(a,i);check(a.getAttribute("shopId",-1)==ids[i],"existing shop identity "+ids[i]);a.closeAll(false,true);}
        }
        a.getSkills().setLevelAndXP(Skills.PRAYER,99,13034431);a.getSkills().setPrayerPoints(2,false);a.getSkills().setHitPoints(200);
        click(a,Portal.ALTAR);pick(a,0);check(a.getSkills().getPrayerPoints()==99&&a.getSkills().getHitPoints()==200,"altar restores prayer only");
        click(a,Portal.ALTAR);pick(a,1);pick(a,2);check(a.getSettings().getSpellBook()==430,"lunar selection");
        click(a,Portal.ALTAR);pick(a,1);pick(a,0);check(a.getSettings().getSpellBook()==192,"modern selection");
        click(a,Portal.ALTAR);pick(a,2);pick(a,1);check(a.getPrayer().isAncientCurses(),"curses selection");
        click(a,Portal.ALTAR);pick(a,2);pick(a,0);check(!a.getPrayer().isAncientCurses(),"standard prayers selection");
    }
    static void homeGambler()throws Exception{
        npc=find(Service.GAMBLER);check(HomeHub.isGamblerStand(npc),"registered home stand");
        Player a=at(npc.getLocation().transform(1,0,0));a.getInventory().getContainer().set(0,new Item(995,100000));
        long before=total(a,995);GamblerSession s=begin(a,995,10000);advance(4);check(s.confirm(),"home wager commits");advance(6);s.updateSession();
        check(total(a,995)==before,"home wager uses same conserved house/account flow");s.endSession();
        NPC fake=new NPC(2998,Mob.DEFAULT);World.getWorld().getNpcs().add(fake);check(!HomeHub.isGamblerStand(fake),"arbitrary spawned gambler not approved");
    }
    static void returns()throws Exception{
        Player p=at(Location.locate(3200,3200,0));
        for(int id:new int[]{38700,2274}){
            p.removeAttribute("cantMove");p.setLocation(Location.locate(3200,3200,0));
            objectAction(p,new GameObject(id,3201,3200,0,10,0));
            if(id==2274){pulse();for(int i=0;i<12;i++)p.processTicks();}
            check(p.getLocation().equals(Mob.DEFAULT),"existing home-return object "+id);
        }
    }
    public static void main(String[] args)throws Exception{
        root=Files.createTempDirectory(Paths.get("build/gambler"),"home-test-");CombatFixtures.init();MapXTEA.loadPackedFile();GroundItemManager.load();
        field(World.getWorld(),World.class,"playerLoader",new PlayerLoader(root));InstanceManager.getSingleton().beginCycle();
        LandscapeParser.parseLandscape((36<<8)|59,MapXTEA.getMapKeys().get((36<<8)|59));
        World.getWorld().getShopManager().load();geometry();landings();menus();homeGambler();returns();
        System.out.println("Home hub: "+checks+" checks passed; isolated storage "+root);
    }
}
