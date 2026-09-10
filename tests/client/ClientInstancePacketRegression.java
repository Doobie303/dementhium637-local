import java.nio.file.*;
import java.util.*;
import org.dementhium.cache.Cache;
import org.dementhium.model.Location;
import org.dementhium.model.map.region.*;
import org.dementhium.net.message.*;
import org.dementhium.util.MapXTEA;

/** Cross-implementation test: real server payloads decoded with client code.
 * No applet, graphics, network connection, account or live server is started. */
public final class ClientInstancePacketRegression {
    static int checks, packets, rejectedMutations;
    static Path output;
    static final class Chunk {
        final int plane,x,y,sx,sy,sp;
        Chunk(int plane,int x,int y,int sx,int sy,int sp) {
            this.plane=plane; this.x=x; this.y=y; this.sx=sx; this.sy=sy; this.sp=sp;
        }
    }
    static void check(boolean value,String why) { checks++; if (!value) throw new AssertionError(why); }
    static void copy(MapAllocation map,List<Chunk> expected,int sx,int sy,int dx,int dy,int w,int h,int[] from,int[] to) {
        RegionBuilder.copyMap(map,sx,sy,dx,dy,w,h,from,to);
        for (int p=0;p<from.length;p++) for (int x=0;x<w;x++) for (int y=0;y<h;y++) {
            int gx=map.getX()/8+dx+x, gy=map.getY()/8+dy+y, plane=to[p];
            expected.removeIf(c->c.plane==plane && c.x==gx && c.y==gy);
            expected.add(new Chunk(plane,gx,gy,sx+x,sy+y,from[p]));
        }
    }
    static byte[] build(Location centre,int depth) {
        Message message=DynamicMapPacket.build(centre,depth);
        check(message.getOpcode()==31 && message.getType()==Message.PacketType.VAR_SHORT,"opcode 31 with two-byte length");
        byte[] bytes=new byte[message.getLength()]; message.getBuffer().getBytes(0,bytes);
        check(bytes.length<=65535,"payload fits variable-short frame");
        try {
            java.lang.reflect.Method encode=org.dementhium.net.codec.DefaultGameEncoder.class.getDeclaredMethod("encode",
                    org.jboss.netty.channel.ChannelHandlerContext.class,org.jboss.netty.channel.Channel.class,Object.class);
            encode.setAccessible(true);
            org.jboss.netty.buffer.ChannelBuffer frame=(org.jboss.netty.buffer.ChannelBuffer)encode.invoke(
                    new org.dementhium.net.codec.DefaultGameEncoder(),null,null,message);
            byte[] framed=new byte[frame.readableBytes()]; frame.getBytes(0,framed);
            Class98_Sub22 reader=new Class98_Sub22(framed);
            check(reader.readUnsignedByte((byte)39)==31,"actual server encoder emits opcode 31");
            check(reader.readShort((byte)127)==bytes.length,"binary client reads actual frame length correctly");
            check(Arrays.equals(bytes,Arrays.copyOfRange(framed,reader.anInt3991,framed.length)),"framing preserves payload exactly");
        } catch(ReflectiveOperationException e) {throw new AssertionError("Cannot exercise server frame encoder",e);}
        return bytes;
    }
    static void verify(byte[] bytes,Location centre,int depth,List<Chunk> expected) {
        ClientMapDecoder.decode(bytes);
        check(ClientMapDecoder.depth==depth,"client reads viewport index");
        check(ClientMapDecoder.centreX==centre.getRegionX() && ClientMapDecoder.centreY==centre.getRegionY(),"header coordinate order/endian/transforms");
        check(ClientMapDecoder.Class151_Sub9.anInt5028==1 && ClientMapDecoder.force,"dynamic mode and forced scene rebuild");
        check(ClientMapDecoder.transitions==1 && ClientMapDecoder.resets==1 && ClientMapDecoder.state==11,"client reaches scene transition once");
        check(ClientMapDecoder.consumed()==bytes.length,"client consumes exactly the entire payload");
        int width=Class246_Sub3_Sub4_Sub5.anIntArray6265[depth];
        check(width==Location.VIEWPORT_SIZES[depth],"binary client and server viewport tables agree");
        int count=width/8, baseX=centre.getRegionX()-width/16, baseY=centre.getRegionY()-width/16;
        int[][][] expectedChunks=new int[4][count][count];
        for (int[][] plane:expectedChunks) for(int[] row:plane) Arrays.fill(row,-1);
        for (Chunk c:expected) {
            int x=c.x-baseX,y=c.y-baseY;
            if(x>=0 && y>=0 && x<count && y<count)
                expectedChunks[c.plane][x][y]=(c.sp<<24)|(c.sx<<14)|(c.sy<<3);
        }
        LinkedHashSet<Integer> regionIds=new LinkedHashSet<Integer>();
        for(int p=0;p<4;p++) for(int x=0;x<count;x++) for(int y=0;y<count;y++) {
            int entry=expectedChunks[p][x][y];
            check(ClientMapDecoder.chunks()[p][x][y]==entry,"decoded source chunk/plane/rotation or blank tile");
            if(entry!=-1) regionIds.add((((entry>>>14)&1023)/8<<8)+((entry>>>3)&2047)/8);
        }
        check(ClientMapDecoder.regions().length==regionIds.size(),"one key group per distinct source region");
        int index=0;
        for(int region:regionIds) {
            check(ClientMapDecoder.regions()[index]==region,"client discovers source regions in server key order");
            int[] keys=MapXTEA.getKey(region);
            for(int k=0;k<4;k++) check(ClientMapDecoder.keys()[index][k]==(keys==null?0:keys[k]),"decoded XTEA word");
            String suffix=(region>>8)+"_"+(region&255);
            for(String prefix:new String[]{"m","l","um","ul"})
                check(ClientMapDecoder.archives.get(index*4+Arrays.asList("m","l","um","ul").indexOf(prefix)).equals(prefix+suffix),"client requests correct source archive name");
            index++;
        }
        check(ClientMapDecoder.archives.size()==regionIds.size()*4,"no phantom source archive requests");
    }
    static void packet(String name,Location centre,int depth,List<Chunk> expected)throws Exception {
        byte[] bytes=build(centre,depth); verify(bytes,centre,depth,expected); packets++;
        Files.write(output.resolve(name+"-depth"+depth+".bin"),bytes);
    }
    static void mutation(byte[] bytes,Location centre,int depth,List<Chunk> expected,String name) {
        try {verify(bytes,centre,depth,expected);} catch (AssertionError|RuntimeException rejected) {
            rejectedMutations++; return;
        }
        throw new AssertionError("Compatibility test failed to detect "+name);
    }
    public static void main(String[] args)throws Exception {
        output=Paths.get(args[0]); Cache.init(); check(MapXTEA.loadPackedFile(),"real packed keys loaded");
        // Verify opcode metadata from the actual client JAR as well as decoder source.
        check(Class150.aClass58_1212.method521((byte)100)==31 && Class150.aClass58_1212.anInt460==-2,"binary client registers opcode 31 with variable-short length");
        int baseline=RegionBuilder.getAllocationCount();
        MapAllocation cave=RegionBuilder.reserveMap(16,16), mixed=RegionBuilder.reserveMap(16,16);
        check(cave!=null && mixed!=null,"isolated test map reservations");
        List<Chunk> caveChunks=new ArrayList<Chunk>(), mixedChunks=new ArrayList<Chunk>();
        try {
            copy(cave,caveChunks,296,632,0,0,8,8,new int[]{0},new int[]{0});
            copy(mixed,mixedChunks,360,648,0,0,8,8,new int[]{0,1,2,3},new int[]{3,2,1,0});
            copy(mixed,mixedChunks,296,632,8,0,8,8,new int[]{0},new int[]{0});
            // A repeated source on another plane verifies deduplication and plane order.
            copy(mixed,mixedChunks,360,648,8,8,2,2,new int[]{2},new int[]{1});
            int[][] offsets={{42,55},{0,0},{63,63},{64,64},{95,35}};
            for(int depth=0;depth<4;depth++) {
                for(int n=0;n<offsets.length;n++) {
                    Location centre=Location.locate(cave.getX()+offsets[n][0],cave.getY()+offsets[n][1],depth);
                    packet("cave-position"+n,centre,depth,caveChunks);
                }
                packet("mixed-planes",Location.locate(mixed.getX()+60,mixed.getY()+60,depth),depth,mixedChunks);
            }
            // Rebuild the same destination chunks on one plane: no stale descriptor survives.
            Location centre=Location.locate(mixed.getX()+60,mixed.getY()+60,0);
            copy(mixed,mixedChunks,296,632,0,0,2,2,new int[]{0},new int[]{3});
            packet("changed-template",centre,3,mixedChunks);
            // Distinct signed key words catch endian errors even if the real cave keys happen to be zero.
            Map<Integer,int[]> savedKeys=new HashMap<Integer,int[]>();
            for(Chunk c:mixedChunks) {
                int id=(c.sx/8<<8)+c.sy/8;
                if(!savedKeys.containsKey(id)) savedKeys.put(id,MapXTEA.getKey(id));
            }
            try {
                for(int id:savedKeys.keySet()) MapXTEA.getMapKeys().put(id,new int[]{0x12345678^id,0x87654321^id,-1,id});
                packet("sentinel-keys",centre,3,mixedChunks);
                byte[] bytes=build(centre,3);
                byte[] swapped=bytes.clone(); byte temp=swapped[5]; swapped[5]=swapped[6]; swapped[6]=temp;
                mutation(swapped,centre,3,mixedChunks,"swapped coordinate endian");
                byte[] wrongKey=bytes.clone(); wrongKey[wrongKey.length-1]^=1;
                mutation(wrongKey,centre,3,mixedChunks,"corrupt map key");
                byte[] wrongChunk=bytes.clone(); wrongChunk[7]^=(byte)0x80;
                mutation(wrongChunk,centre,3,mixedChunks,"changed presence bit");
                mutation(Arrays.copyOf(bytes,bytes.length-1),centre,3,mixedChunks,"truncated payload");
                mutation(Arrays.copyOf(bytes,bytes.length+1),centre,3,mixedChunks,"trailing byte");
            } finally {
                for(Map.Entry<Integer,int[]> e:savedKeys.entrySet())
                    if(e.getValue()==null) MapXTEA.getMapKeys().remove(e.getKey()); else MapXTEA.getMapKeys().put(e.getKey(),e.getValue());
            }
            check(rejectedMutations==5,"negative controls detected");
        } finally {RegionBuilder.releaseMap(cave); RegionBuilder.releaseMap(mixed);}
        check(RegionBuilder.getAllocationCount()==baseline,"test allocations returned to baseline");
        System.out.println("PASS ClientInstancePacketRegression: "+packets+" server packets, "+checks+" assertions, "+rejectedMutations+" negative controls; real client binary readers + verbatim decoder source");
    }
}
