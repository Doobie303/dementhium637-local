import java.util.*;
import org.dementhium.content.minigames.*;
import org.dementhium.model.*;
import org.dementhium.model.instance.*;
import org.dementhium.model.misc.*;
import org.dementhium.model.player.Player;
import org.dementhium.net.ActionSender;
import org.dementhium.net.message.Message;
import org.dementhium.net.packethandlers.GroundItemActionHandler;
import org.dementhium.tickable.Tick;

/** Real map/spawn/pickup packets; a headless ground-list model, not a rendering test.
 * Client reference: Class98_Sub10_Sub22.method1070 (29 adds, 59 removes one ID),
 * Class48_Sub1.method458 (separate value-sorted entries), Class251 (retains overlap).
 */
public final class GroundItemSceneRegression {
    static int checks;
    static void check(boolean ok, String why) { checks++; if (!ok) throw new AssertionError(why); }
    static final class Scene {
        final Player player;
        final Map<String,List<Integer>> piles = new HashMap<String,List<Integer>>();
        int x,y,z;
        Scene(Player p) { player=p; InstancePartyRegression.messages.get(p).clear(); }
        String key(Location tile,int id) { return tile.getX()+":"+tile.getY()+":"+tile.getZ()+":"+id; }
        void drain() {
            for (Message m:InstancePartyRegression.messages.get(player)) {
                org.jboss.netty.buffer.ChannelBuffer b=m.getBuffer();
                if(m.getOpcode()==114) {
                    x=((b.getUnsignedByte(0)-128)&255)+player.getRegion().getLastMapRegion().getRegionX()-6;
                    z=(-b.getUnsignedByte(1))&255;
                    y=((b.getUnsignedByte(2)-128)&255)+player.getRegion().getLastMapRegion().getRegionY()-6;
                } else if(m.getOpcode()==29 || m.getOpcode()==59) {
                    boolean add=m.getOpcode()==29;
                    int packed=(128-b.getUnsignedByte(add?2:0))&255;
                    int id=add?((b.getUnsignedByte(0)-128)&255)|(b.getUnsignedByte(1)<<8)
                        :(b.getUnsignedByte(1)<<8)|((b.getUnsignedByte(2)-128)&255);
                    Location tile=Location.locate((x<<3)+(packed>>4&7),(y<<3)+(packed&7),z);
                    String key=key(tile,id);
                    List<Integer> amounts=piles.computeIfAbsent(key,k->new ArrayList<Integer>());
                    if(add) {
                        amounts.add(((b.getUnsignedByte(3)-128)&255)|(b.getUnsignedByte(4)<<8));
                        // Coins' highest-value entry is removed first in the supplied client.
                        amounts.sort(Collections.reverseOrder());
                    } else if(!amounts.isEmpty()) amounts.remove(0);
                }
            }
            InstancePartyRegression.messages.get(player).clear();
        }
        void expect(Location tile,int id,Integer... expected) {
            drain(); List<Integer> want=new ArrayList<Integer>(Arrays.asList(expected));
            want.sort(Collections.reverseOrder());
            check(piles.getOrDefault(key(tile,id),Collections.emptyList()).equals(want),
                "client pile "+key(tile,id)+" expected "+want+" got "+piles.get(key(tile,id)));
        }
    }
    static GroundItem drop(Player p,Location tile,int id,int amount,boolean global) {
        GroundItem item=new GroundItem(p,new Item(id,amount),tile,global,false,GroundItemManager.groundItemIndex++);
        GroundItemManager.createGroundItem(item); return item;
    }
    static void pickup(Player p,GroundItem item) { new GroundItemActionHandler().pickup(p,item); }
    public static void main(String[] args) throws Exception {
        InstanceOperationsRegression.init(); List<Tick> tasks=new ArrayList<Tick>();
        InstanceManager manager=new InstanceManager(tasks::add); manager.beginCycle();
        Player p=InstancePartyRegression.player("groundscene"),q=InstancePartyRegression.player("groundother");
        p.getDefinition().setRights(0); q.getDefinition().setRights(0);
        try {
            FightCavesSession a=FightCavesSession.start(manager,p,0),b=FightCavesSession.start(manager,q,0);
            check(a!=null && b!=null,"two real caves started");
            Scene first=new Scene(p),second=new Scene(q); Location tile=p.getLocation();
            GroundItem coins=drop(p,tile,995,7,false); first.expect(tile,995,7);
            // Old refresh sends a second add here, although the client retains the first entry.
            ActionSender.sendDynamicRegion(p); first.expect(tile,995,7);
            for(int n=0;n<8;n++) { p.updateMap(); first.expect(tile,995,7); }
            Location shifted=null;
            long revision=org.dementhium.model.map.region.RegionBuilder.sceneRevision(tile,p.getViewportDepth());
            for(int dx=-8;dx<=8&&shifted==null;dx++) for(int dy=-8;dy<=8;dy++) {
                Location candidate=tile.transform(dx,dy,0);
                if(a.getInstance().canOccupy(candidate,1)
                    &&org.dementhium.model.map.region.RegionBuilder.sceneRevision(candidate,p.getViewportDepth())!=revision) {
                    shifted=candidate; break;
                }
            }
            check(shifted!=null,"movement fixture crosses a scene boundary");
            p.setLocation(shifted); p.getGpi().sendUpdate();
            check(InstancePartyRegression.messages.get(p).stream().anyMatch(m->m.getOpcode()==31),
                "player update publishes a movement-triggered dynamic rebuild");
            first.expect(tile,995,7);
            p.setLocation(tile); p.getGpi().sendUpdate(); first.expect(tile,995,7);
            GroundItemManager.increaseAmount(p,995,tile,4); first.expect(tile,995,11);
            p.updateMap(); first.expect(tile,995,11);
            pickup(p,coins); first.expect(tile,995);
            check(p.getInventory().getContainer().getNumberOf(new Item(995))==11,"pickup credits once");
            check(GroundItemManager.getQualifiedGroundItem(995,tile,p)==null,"no second collectible item");

            // Same ID/tile, different amounts: all removals must precede all additions.
            GroundItem small=drop(p,tile,995,3,true),large=drop(p,tile,995,20,true);
            first.expect(tile,995,3,20);
            for(int n=0;n<3;n++) { p.updateMap(); first.expect(tile,995,3,20); }
            // A fresh scene must also reconstruct both entries, not remove an entry just added.
            first.piles.clear(); p.updateMap(); first.expect(tile,995,3,20);
            GroundItemManager.discardGroundItem(large); first.expect(tile,995,3);
            pickup(p,small); first.expect(tile,995);
            GroundItem sword1=drop(p,tile,1277,1,false),sword2=drop(p,tile,1277,1,false);
            p.updateMap(); first.expect(tile,1277,1,1);
            pickup(p,sword1); first.expect(tile,1277,1);
            pickup(p,sword2); first.expect(tile,1277);
            check(p.getInventory().getContainer().getNumberOf(new Item(1277))==2,"real unstackable piles preserved");
            GroundItem privateItem=drop(p,tile,995,9,false);
            GroundItemManager.setPublic(privateItem); first.expect(tile,995,9);
            p.updateMap(); first.expect(tile,995,9);
            q.updateMap(); second.expect(tile,995);
            check(GroundItemManager.getQualifiedGroundItem(995,tile,q)==null,"foreign instance lookup blocked");
            a.getInstance().leave(p);
            check(!GroundItemManager.getGroundItems().contains(privateItem),"cave exit removes owned drop");

            // Ordinary-world refresh uses the same contract, including observer privacy.
            Location outside=p.getLocation(); b.getInstance().leave(q); q.setLocation(outside);
            first=new Scene(p); second=new Scene(q);
            GroundItem owned=drop(p,outside,995,5,false);
            ActionSender.updateMapRegion(p,true); first.expect(outside,995,5);
            ActionSender.updateMapRegion(q,true); second.expect(outside,995);
            GroundItem admin=new GroundItem(p,new Item(1277,1),outside,true,true,GroundItemManager.groundItemIndex++);
            GroundItemManager.createGroundItem(admin);
            ActionSender.updateMapRegion(q,true); second.expect(outside,1277);
            GroundItemManager.discardGroundItem(admin);
            GroundItemManager.setPublic(owned); first.expect(outside,995,5); second.expect(outside,995,5);
            ActionSender.updateMapRegion(q,true); second.expect(outside,995,5);
            pickup(p,owned); first.expect(outside,995); second.expect(outside,995);
        } finally {
            manager.closeAll(); manager.endCycle();
            for(Player player:InstancePartyRegression.players) { player.destroy(); World.getWorld().getPlayers().remove(player); }
        }
        System.out.println("GroundItemSceneRegression: "+checks+" checks passed");
    }
}
