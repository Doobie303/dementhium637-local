import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import org.jboss.netty.channel.Channel;
import org.dementhium.cache.Cache;
import org.dementhium.cache.format.*;
import org.dementhium.model.*;
import org.dementhium.model.definition.*;
import org.dementhium.model.map.*;
import org.dementhium.model.map.region.*;
import org.dementhium.model.misc.*;
import org.dementhium.model.player.*;
import org.dementhium.net.*;
import org.dementhium.net.message.*;
import org.dementhium.tickable.Tick;
import org.dementhium.util.MapXTEA;

public final class InstanceFoundationRegression {
    static int checks;
    static List<Message> sent = new ArrayList<Message>();
    static void check(boolean value, String message) {
        checks++; if (!value) throw new AssertionError(message);
    }
    static void rejects(Runnable action, String message) {
        try { action.run(); } catch (IllegalArgumentException | IllegalStateException expected) { checks++; return; }
        throw new AssertionError(message);
    }
    static Player player(Location location) {
        Channel channel = (Channel) Proxy.newProxyInstance(Channel.class.getClassLoader(), new Class[]{Channel.class},
            (proxy, method, args) -> {
                if (method.getName().equals("isConnected") || method.getName().equals("isOpen")) return true;
                if (method.getName().equals("write") && args[0] instanceof Message) sent.add((Message) args[0]);
                if (method.getReturnType() == boolean.class) return false;
                if (method.getReturnType() == int.class) return 0;
                return null;
            });
        Player p = new Player(new GameSession(channel), new PlayerDefinition("instance-test", "unused"));
        p.setHasReceivedStarter(true);
        p.setLocation(location);
        p.setOnline(true);
        return p;
    }
    static void copy(MapAllocation a) {
        RegionBuilder.copyMap(a, 360, 648, 0, 0, 8, 8, new int[]{0,1,2,3}, new int[]{0,1,2,3});
    }
    static Location tile(MapAllocation a, int sourceX, int sourceY, int plane) {
        return Location.locate(a.getX() + sourceX - 2880, a.getY() + sourceY - 5184, plane);
    }
    static int mask(Location l) { return Region.getClippingMask(l.getX(), l.getY(), l.getZ()); }
    static int bits(byte[] data, int offset, int count) {
        int result = 0;
        for (int i = 0; i < count; i++) result = (result << 1) | ((data[(offset + i) >> 3] >> (7 - ((offset + i) & 7))) & 1);
        return result;
    }
    static void packet(MapAllocation a, int depth) {
        Location centre = Location.locate(a.getX() + 32, a.getY() + 32, 0);
        Message m = DynamicMapPacket.build(centre, depth);
        check(m.getOpcode() == 31, "dynamic opcode");
        byte[] bytes = new byte[m.getLength()]; m.getBuffer().getBytes(0, bytes);
        check(((bytes[1] - 128) & 255) == depth, "viewport size header");
        int offset = 7 * 8, radius = Location.VIEWPORT_SIZES[depth] >> 4, built = 0;
        Set<Integer> sources = new LinkedHashSet<Integer>();
        for (int p = 0; p < 4; p++)
            for (int x = centre.getRegionX() - radius; x <= centre.getRegionX() + radius; x++)
                for (int y = centre.getRegionY() - radius; y <= centre.getRegionY() + radius; y++) {
                    int present = bits(bytes, offset++, 1);
                    if (present != 0) {
                        int value = bits(bytes, offset, 26); offset += 26;
                        int sx = (value >> 14) & 1023, sy = (value >> 3) & 2047;
                        // This viewport is completely inside the reservation + blank guard.
                        check(x >= a.getX()/8 && x < a.getX()/8+8 && y >= a.getY()/8 && y < a.getY()/8+8, "only built chunks encoded");
                        check(sx == 360+x-a.getX()/8 && sy == 648+y-a.getY()/8 && (value >> 24) == p && (value & 6) == 0,
                            "source position, plane and rotation");
                        sources.add((sx >> 3) << 8 | (sy >> 3)); built++;
                    }
                }
        check(built == 256, "all 64 chunks on four planes encoded");
        check(bytes.length == (offset+7)/8 + sources.size()*16, "exact deduplicated XTEA payload length");
        int pos = (offset+7)/8;
        for (int id : sources) {
            int[] keys = MapXTEA.getKey(id);
            for (int key = 0; key < 4; key++) {
                int value = bits(bytes, pos*8, 32); pos += 4;
                check(value == (keys == null ? 0 : keys[key]), "XTEA source ordering");
            }
        }
    }
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        Cache.init(); check(MapXTEA.loadPackedFile(), "packed keys");
        GroundItemManager.load(); ItemDefinition.init(); NPCDefinition.init();
        RegionBuilder.init();
        check(RegionBuilder.getDynamicRegionCount() == 0, "startup does not overwrite hardcoded maps");
        MapAllocation a = RegionBuilder.reserveMap(16, 16), b = RegionBuilder.reserveMap(16, 16);
        check(a != null && b != null && a.getId() != b.getId(), "distinct reservations");
        check(a.getX()%128 == 0 && a.getY()%128 == 0, "128 tile alignment");
        check(a.getX()!=b.getX() || Math.abs(a.getY()-b.getY()) >= 384, "reserved guard");
        check(mask(Location.locate(a.getX(),a.getY(),0)) == -1, "unbuilt reservation blocked");
        check(mask(Location.locate(a.getX()-1,a.getY(),0)) == -1, "guard blocked");
        rejects(() -> RegionBuilder.reserveMap(0,1), "zero dimension");
        rejects(() -> RegionBuilder.reserveMap(Integer.MAX_VALUE,1), "oversized dimension");
        rejects(() -> RegionBuilder.copyRegion(360,648,0,a.getX()/8,a.getY()/8,0,1), "rotation rejected");
        rejects(() -> RegionBuilder.copyMap(360,648,a.getX()/8,a.getY()/8,1,new int[]{0},new int[]{0}), "raw API cannot overwrite handle reservation");
        rejects(() -> RegionBuilder.copyMap(a,360,648,15,0,2,1,new int[]{0},new int[]{0}), "copy overflow");
        rejects(() -> RegionBuilder.copyMap(a,Integer.MAX_VALUE,648,0,0,1,1,new int[]{0},new int[]{0}), "coordinate overflow");
        rejects(() -> RegionBuilder.copyMap(a,360,648,0,0,1,1,new int[]{0,1},new int[]{0,0}), "duplicate plane");
        copy(a); copy(b);
        for (int p=0;p<4;p++) for(int x=0;x<64;x++) for(int y=0;y<64;y++) {
            check(Region.getClippingMask(2880+x,5184+y,p)==Region.getClippingMask(a.getX()+x,a.getY()+y,p), "source/destination clipping parity");
        }
        Location source = Location.locate(2924,5197,0), first = tile(a,2924,5197,0), second = tile(b,2924,5197,0);
        GameObject original = source.getGameObjectType(22), clone = first.getGameObjectType(22);
        check(original != null && clone != null && clone != original, "source object independently cloned");
        check(clone.getId()==original.getId() && clone.getLocation().equals(first), "destination object location");
        int base = mask(first), secondBase = mask(second);
        Region.addClipping(source.getX(),source.getY(),0,0x100);
        check(mask(first)==base && mask(second)==secondBase, "source mutation cannot alter copied masks");
        Region.removeClipping(source.getX(),source.getY(),0,0x100);
        ObjectManager.addCustomObject(57263,first.getX(),first.getY(),0,10,0,false);
        check(mask(first)!=base && mask(second)==secondBase, "custom collision isolated");
        check(mask(tile(a,2919,5196,0))==Region.getClippingMask(2919,5196,0), "unrelated pit remains blocked");
        ObjectManager.removeCustomObject(first.getX(),first.getY(),0,10,false);
        check(mask(first)==base && first.getGameObjectType(22)==clone, "custom object removal restores floor");
        Region.removeObject(first.getX(),first.getY(),0,22);
        check(first.getGameObject(original.getId())==null && second.getGameObjectType(22)!=null && source.getGameObjectType(22)==original,
            "removed copied object stays removed only in that copy");
        copy(a);
        Location pit = tile(a,2919,5196,0); int pitBase = mask(pit);
        Region.addClipping(pit.getX(),pit.getY(),0,0x200000);
        Region.removeClipping(pit.getX(),pit.getY(),0,0x200000);
        check(mask(pit)==pitBase,"overlapping added collision preserves original terrain");
        Region.addClipping(first.getX(),first.getY(),0,0x100);
        Region.addClipping(first.getX(),first.getY(),0,0x100);
        Region.removeClipping(first.getX(),first.getY(),0,0x100);
        check((mask(first)&0x100)!=0,"overlapping additions retained until final removal");
        Region.removeClipping(first.getX(),first.getY(),0,0x100);
        check(mask(first)==base,"final overlapping removal restores original mask");
        int rectangular = -1;
        for(int candidate=1;candidate<10000;candidate++) {
            CacheObjectDefinition def=CacheObjectDefinition.forId(candidate);
            if(def!=null && def.getActionCount()!=0 && def.getSizeX()!=def.getSizeY()
                && def.getSizeX()>0 && def.getSizeY()>0 && def.getSizeX()<=4 && def.getSizeY()<=4) { rectangular=candidate; break; }
        }
        check(rectangular!=-1,"non-square object fixture");
        for(int rotation=0;rotation<4;rotation++) {
            int[][] beforeMasks=new int[5][5];
            for(int x=0;x<5;x++) for(int y=0;y<5;y++) beforeMasks[x][y]=Region.getClippingMask(first.getX()+x,first.getY()+y,0);
            Region.addObject(rectangular,first.getX(),first.getY(),0,10,rotation,false);
            CacheObjectDefinition def=CacheObjectDefinition.forId(rectangular);
            int width=rotation%2==0?def.getSizeX():def.getSizeY(), height=rotation%2==0?def.getSizeY():def.getSizeX();
            for(int x=0;x<5;x++) for(int y=0;y<5;y++) {
                int actual=Region.getClippingMask(first.getX()+x,first.getY()+y,0);
                check(x<width && y<height ? (actual&256)!=0 : actual==beforeMasks[x][y],"rotated object footprint");
            }
            Region.removeObject(first.getX(),first.getY(),0,10);
            for(int x=0;x<5;x++) for(int y=0;y<5;y++)
                check(Region.getClippingMask(first.getX()+x,first.getY()+y,0)==beforeMasks[x][y],"non-square removal restores complete footprint");
        }
        for (int d=0;d<4;d++) packet(a,d);
        Player boundaryA = player(Location.locate(a.getX()+127,a.getY()+32,0));
        Player boundaryB = player(Location.locate(a.getX()+128,a.getY()+32,0));
        check(Region.getLocalPlayers(boundaryA.getLocation()).contains(boundaryB),"player lookup crosses 128-tile spatial boundary");
        boundaryA.destroy(); boundaryB.destroy();
        Player p = player(first);
        check(RegionBuilder.getAllocationCount()==2,"Player class load does not allocate housing");
        p.updateMap();
        check(sent.stream().anyMatch(m -> m.getOpcode()==31),"player uses dynamic rebuild");
        Region.removeObject(first.getX(),first.getY(),0,22);
        sent.clear(); p.updateMap();
        check(sent.stream().anyMatch(m -> m.getOpcode()==19),"removed copied object replays a deletion after rebuild");
        sent.clear(); p.getRegion().teleport(first.getX(),first.getY(),1);
        check(sent.stream().anyMatch(m -> m.getOpcode()==31),"dynamic plane change rebuilds scene");
        sent.clear(); p.getRegion().teleport(first.getX(),first.getY(),0);
        check(sent.stream().anyMatch(m -> m.getOpcode()==31) && sent.stream().anyMatch(m -> m.getOpcode()==19),"revisited plane rebuilds and retains removed object");
        rejects(() -> RegionBuilder.releaseMap(a), "occupied release rejected");
        rejects(() -> copy(a), "occupied rebuild rejected");
        p.setLocation(Location.locate(3200,3200,0)); sent.clear(); p.updateMap();
        check(sent.stream().anyMatch(m -> m.getOpcode()==80) && sent.stream().noneMatch(m -> m.getOpcode()==31), "dynamic flag resets on ordinary map");
        p.setLocation(first); p.updateMap(); check(!p.getRegion().isSceneChanged(),"sent revision recorded");
        RegionBuilder.copyMap(a,360,648,8,0,1,1,new int[]{0},new int[]{0});
        check(p.getRegion().isSceneChanged(),"stationary player detects room expansion");
        p.setLocation(Location.locate(3200,3200,0));
        Field brokenField = LandscapeParser.class.getDeclaredField("broken"); brokenField.setAccessible(true);
        Map<Integer,Boolean> broken = (Map<Integer,Boolean>) brokenField.get(null);
        int id = (45<<8)|81; broken.put(id,Boolean.TRUE);
        long oldRevision = RegionBuilder.sceneRevision(first,0);
        rejects(() -> RegionBuilder.copyMap(a,360,648,8,8,1,1,new int[]{0},new int[]{0}),"failed source build rejected");
        check(RegionBuilder.sceneRevision(first,0)==oldRevision,"failed copy has no published changes");
        Location before = p.getLocation(); sent.clear();
        p.getRegion().teleport(first);
        check(p.getLocation()==before && sent.stream().noneMatch(m -> m.getOpcode()==31),"failed entry leaves player in place without map packet");
        broken.remove(id);
        // Generic temporary-object callback must not restore into reused map coordinates.
        Field ticksField = World.class.getDeclaredField("ticksToAdd"); ticksField.setAccessible(true);
        List<Tick> pending = (List<Tick>) ticksField.get(World.getWorld());
        int beforeTicks = pending.size();
        ObjectManager.addCustomObject(57263,first.getX(),first.getY(),0,10,0,false);
        ObjectManager.replaceObjectTemporarily(first,57262,5);
        check(pending.size()==beforeTicks+1,"temporary object scheduled");
        Tick stale = pending.get(pending.size()-1);
        RespawnableGroundItem drop = new RespawnableGroundItem(new Item(995,1),first,5);
        GroundItemManager.createGroundItem(drop);
        rejects(() -> RegionBuilder.releaseMap(a),"ground item blocks release");
        GroundItemManager.removeGroundItem(drop);
        Tick staleDrop = pending.get(pending.size()-1);
        int afterDrop = pending.size();
        GroundItemManager.removeGroundItem(drop);
        check(pending.size()==afterDrop,"repeated item removal does not schedule duplicate respawn");
        int originalX = a.getX(), originalY = a.getY();
        RegionBuilder.releaseMap(a); RegionBuilder.releaseMap(a);
        MapAllocation reused = RegionBuilder.reserveMap(16,16);
        check(reused.getX()==originalX && reused.getY()==originalY,"freed allocation reused");
        copy(reused); stale.execute(); staleDrop.execute();
        check(!GroundItemManager.getGroundItems().contains(drop),"old item respawn cannot enter reused map");
        check(tile(reused,2924,5197,0).getGameObjectType(10)==null,"old timer cannot restore into reused map");
        rejects(() -> copy(a),"stale handle cannot modify new allocation");
        check(!RegionBuilder.getDynamicRegion(reused.getX(),reused.getY()).isEmpty(),"stale release did not clear replacement");
        RegionBuilder.releaseMap(reused); RegionBuilder.releaseMap(b);
        check(RegionBuilder.getAllocationCount()==0 && RegionBuilder.getDynamicRegionCount()==0,"release returns registry to baseline");
        int ordinaryId = (50<<8)|50;
        RegionBuilder.destroyDynamicRegion(ordinaryId);
        check(source.getGameObjectType(22)==original,"ordinary source untouched by map cleanup");
        // Concurrent reservations must be unique before any chunks are built.
        ExecutorService pool = Executors.newFixedThreadPool(4);
        List<Future<MapAllocation>> futures = new ArrayList<Future<MapAllocation>>();
        for(int i=0;i<8;i++) futures.add(pool.submit(() -> RegionBuilder.reserveMap(1,1)));
        Set<String> origins = new HashSet<String>();
        for(Future<MapAllocation> f:futures) { MapAllocation r=f.get(); check(r!=null && origins.add(r.getX()+":"+r.getY()),"atomic reservation"); }
        for(Future<MapAllocation> f:futures) RegionBuilder.releaseMap(f.get());
        pool.shutdown();
        for(int i=0;i<50;i++) { MapAllocation r=RegionBuilder.reserveMap(1,1); check(r!=null,"repeated allocation"); RegionBuilder.releaseMap(r); }
        check(RegionBuilder.getAllocationCount()==0 && RegionBuilder.getDynamicRegionCount()==0,"repeated release has no registry leak");
                // Legacy discovery also reserves immediately. Empty destruction must not
        // create entries, while all-plane destruction preserves other chunks.
        int[] legacyOrigin = RegionBuilder.findEmptyMap(16,16);
        MapAllocation legacy = RegionBuilder.getAllocation(legacyOrigin[0],legacyOrigin[1]);
        RegionBuilder.copyAllPlanesMap(360,648,legacyOrigin[0]/8,legacyOrigin[1]/8,2);
        RegionBuilder.destroyMap(legacyOrigin[0]/8,legacyOrigin[1]/8,1,1,new int[]{0},new int[]{0});
        check(mask(Location.locate(legacyOrigin[0],legacyOrigin[1],0))==-1,"partial clear blocks removed chunk");
        check(RegionBuilder.getDynamicRegion(legacyOrigin[0],legacyOrigin[1]).getChunkMapping(0,1,1)[0]==361,"neighbor chunk retained");
        RegionBuilder.releaseMap(legacy);
        int registryBefore=RegionBuilder.getDynamicRegionCount();
        RegionBuilder.destroyAllPlanesMap(legacyOrigin[0]/8,legacyOrigin[1]/8,2);
        check(RegionBuilder.getDynamicRegionCount()==registryBefore,"destroy absent map is a no-op");
        List<MapAllocation> capacity = new ArrayList<MapAllocation>();
        MapAllocation available;
        while((available=RegionBuilder.reserveMap(128,128))!=null) capacity.add(available);
        int exhaustedCount=RegionBuilder.getAllocationCount();
        check(RegionBuilder.reserveMap(128,128)==null && RegionBuilder.getAllocationCount()==exhaustedCount,"capacity exhaustion has no partial reservation");
        for(MapAllocation allocated:capacity) RegionBuilder.releaseMap(allocated);
        p.destroy();
        System.out.println("PASS InstanceFoundationRegression: "+checks+" checks");
    }
}
