import java.lang.reflect.*;
import java.util.*;
import org.dementhium.content.minigames.*;
import org.dementhium.model.*;
import org.dementhium.model.instance.*;
import org.dementhium.model.map.region.*;
import org.dementhium.model.npc.*;
import org.dementhium.model.player.*;
import org.dementhium.net.*;
import org.dementhium.net.message.*;
import org.dementhium.task.impl.*;
import org.dementhium.tickable.Tick;

/** Real first-wave monsters and server rebuild/NPC packets, decoded using supplied client methods. */
public final class ClientNpcSceneRegression {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static final List<Tick> tasks=new ArrayList<Tick>();
    static byte[] bytes(Message packet){byte[] bytes=new byte[packet.getLength()];packet.getBuffer().getBytes(0,bytes);return bytes;}
    static void update(Player p,ClientNpcSceneDecoder.Context client){client.packet(bytes(p.getGni().createPacket()),p.getLocation().getX(),p.getLocation().getY());}
    static void rebuild(Player p,ClientNpcSceneDecoder.Context client){ActionSender.sendDynamicRegion(p);client.map(1);}
    static void nearby(GameInstance instance,NPC npc,Player p){
        for(int dx=-3;dx<=3;dx++)for(int dy=-3;dy<=3;dy++){
            Location tile=p.getLocation().transform(dx,dy,0);
            if(instance.canOccupy(tile,npc.size())){npc.setLocation(tile);new NPCResetTask(npc).execute();return;}
        }
        throw new AssertionError("No nearby tile");
    }
    public static void main(String[] args)throws Exception{
        InstanceOperationsRegression.init();InstanceManager m=new InstanceManager(tasks::add);m.beginCycle();
        Player p=InstancePartyRegression.player("sceneone"),q=InstancePartyRegression.player("scenetwo");p.setViewDistance(15);q.setViewDistance(15);
        ClientNpcSceneDecoder.Context first=new ClientNpcSceneDecoder.Context(),second=new ClientNpcSceneDecoder.Context();
        try{
            // An ordinary nearby NPC is already known before entering the cave.
            NPC lobby=new NPC(1);lobby.setLocation(p.getLocation());World.getWorld().getNpcs().add(lobby);
            update(p,first);check(first.contains(lobby.getIndex()),"ordinary initial list");
            FightCavesSession a=FightCavesSession.start(m,p,0);check(a!=null,"first cave starts");first.map(1);
            NPC bat=a.getLiveNpcs().get(0);nearby(a.getInstance(),bat,p);
            update(p,first);check(first.count==0,"entry packet must match client-cleared list while teleporting");
            p.getWalkingQueue().getNextEntityMovement();new PlayerResetTask(p).execute();
            update(p,first);check(first.contains(bat.getIndex()),"first wave added after teleport");
            check(first.x(bat.getIndex())==bat.getLocation().getX()&&first.y(bat.getIndex())==bat.getLocation().getY(),"first-wave coordinates decoded correctly");
            FightCavesSession b=FightCavesSession.start(m,q,0);check(b!=null,"second cave starts");second.map(1);
            NPC other=b.getLiveNpcs().get(0);nearby(b.getInstance(),other,q);q.getWalkingQueue().getNextEntityMovement();new PlayerResetTask(q).execute();update(q,second);
            check(second.contains(other.getIndex())&&!second.contains(bat.getIndex()),"second client's independent NPC list");
            // Rebuild while the first wave remains alive; the exact client reset forgets it.
            for(int n=0;n<12;n++){
                rebuild(p,first);check(first.count==0,"client dynamic rebuild clears list");
                update(p,first);check(first.contains(bat.getIndex()),"live first-wave NPC is re-added after rebuild");
                byte[] otherPacket=bytes(q.getGni().createPacket());check((otherPacket[0]&255)==second.count,"rebuild does not clear other viewer's retained list");
                second.packet(otherPacket,q.getLocation().getX(),q.getLocation().getY());check(second.contains(other.getIndex()),"other viewer remains synchronized");
                check(first.count==1&&!first.contains(other.getIndex()),"no duplicates or cross-instance additions");
            }
            // A normal change in the player's visible region set also rebuilds the scene.
            Location origin=p.getLocation(),shifted=null;long revision=RegionBuilder.sceneRevision(origin,p.getViewportDepth());
            for(int dx=-8;dx<=8&&shifted==null;dx++)for(int dy=-8;dy<=8;dy++){
                Location candidate=origin.transform(dx,dy,0);
                if(a.getInstance().canOccupy(candidate,1)&&candidate.withinDistance(bat.getLocation(),14)
                        &&RegionBuilder.sceneRevision(candidate,p.getViewportDepth())!=revision){shifted=candidate;break;}
            }
            check(shifted!=null,"cave movement fixture crosses a scene-revision boundary");
            long rebuilds=InstancePartyRegression.messages.get(p).stream().filter(packet->packet.getOpcode()==31).count();
            p.setLocation(shifted);p.getGpi().sendUpdate();
            check(InstancePartyRegression.messages.get(p).stream().filter(packet->packet.getOpcode()==31).count()==rebuilds+1,"ordinary position change triggers actual map publication");
            first.map(1);update(p,first);check(first.contains(bat.getIndex()),"first-wave monster survives movement-triggered rebuild");
            check(first.x(bat.getIndex())==bat.getLocation().getX()&&first.y(bat.getIndex())==bat.getLocation().getY(),"rebuilt monster retains exact coordinates");
            a.getInstance().close();first.map(0);p.getWalkingQueue().getNextEntityMovement();new PlayerResetTask(p).execute();
            update(p,first);check(!first.contains(bat.getIndex()),"ordinary return drops instance NPC");
            // Ordinary-to-ordinary rebuild retains nearby NPCs, matching client mode zero.
            lobby.setLocation(p.getLocation());update(p,first);check(first.contains(lobby.getIndex()),"ordinary return adds lobby NPC");
            ActionSender.updateMapRegion(p,true);first.map(0);byte[] ordinary=bytes(p.getGni().createPacket());
            check((ordinary[0]&255)==first.count,"ordinary rebuild preserves retained list rather than re-adding");
            first.packet(ordinary,p.getLocation().getX(),p.getLocation().getY());check(first.contains(lobby.getIndex()),"ordinary same-mode rebuild stays synchronized");
            World.getWorld().getNpcs().remove(lobby);b.getInstance().close();
            // Shared map expansion refreshes both stationary viewers while retaining an owned NPC.
            GameInstance party=m.create(16,16,2,InstancePartyRegression.EXIT,InstancePartyRegression::map);
            party.enter(p,party.location(44,19,0));party.enter(q,party.location(44,19,0));first.map(1);second.map(1);
            NPC shared=party.spawnNpc(new NPC(1),party.location(44,13,0));new NPCResetTask(shared).execute();
            for(Player viewer:new Player[]{p,q}){viewer.getWalkingQueue().getNextEntityMovement();new PlayerResetTask(viewer).execute();}
            update(p,first);update(q,second);check(first.contains(shared.getIndex())&&second.contains(shared.getIndex()),"shared NPC known to both viewers");
            party.buildRoom(360,648,8,0,8,8,new int[]{0},new int[]{0});
            p.getGpi().sendUpdate();first.map(1);update(p,first);q.getGpi().sendUpdate();second.map(1);update(q,second);
            check(first.contains(shared.getIndex())&&second.contains(shared.getIndex()),"stationary expansion resends NPC to both viewers");
            // Failed packet construction must not discard the old server list.
            int old=first.count;Field depth=Player.class.getDeclaredField("viewportDepth");depth.setAccessible(true);depth.setInt(p,4);
            boolean rejected=false;
            try{ActionSender.sendDynamicRegion(p);}catch(IllegalArgumentException expected){rejected=true;}finally{depth.setInt(p,0);}
            check(rejected,"invalid rebuild failed before publication");
            byte[] unchanged=bytes(p.getGni().createPacket());check((unchanged[0]&255)==old,"failed rebuild preserves retained list count");
            first.packet(unchanged,p.getLocation().getX(),p.getLocation().getY());check(first.count==old,"failed rebuild leaves list intact");
        }finally{
            m.closeAll();for(Player player:InstancePartyRegression.players)World.getWorld().getPlayers().remove(player);m.endCycle();
        }
        System.out.println("ClientNpcSceneRegression: "+checks+" checks passed using actual client reset/list methods and binary bit readers");
    }
}


