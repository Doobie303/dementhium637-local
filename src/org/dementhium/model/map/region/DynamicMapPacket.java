package org.dementhium.model.map.region;

import java.util.LinkedHashSet;
import java.util.Set;
import org.dementhium.cache.format.LandscapeParser;
import org.dementhium.model.Location;
import org.dementhium.net.message.Message;
import org.dementhium.net.message.MessageBuilder;
import org.dementhium.net.message.Message.PacketType;
import org.dementhium.util.MapXTEA;

/** Pure packet construction: a failed map load never teleports or writes to a player. */
public final class DynamicMapPacket {
    private static int[] mapping(int plane, int x, int y) {
        DynamicRegion r = RegionBuilder.getDynamicRegion((x >> 3) << 8 | (y >> 3));
        return r == null ? new int[]{x, y, plane, 0} : r.getChunkMapping(plane, x & 7, y & 7);
    }
    private static int radius(int depth) {
        if (depth < 0 || depth >= Location.VIEWPORT_SIZES.length) throw new IllegalArgumentException("Invalid viewport depth");
        return Location.VIEWPORT_SIZES[depth] >> 4;
    }
    public static void validate(Location location, int depth) {
        synchronized (RegionBuilder.class) { sourceRegions(location, depth); }
    }
    private static Set<Integer> sourceRegions(Location location, int depth) {
        int radius = radius(depth);
        Set<Integer> ids = new LinkedHashSet<Integer>();
        for (int p = 0; p < 4; p++)
            for (int x = location.getRegionX() - radius; x <= location.getRegionX() + radius; x++)
                for (int y = location.getRegionY() - radius; y <= location.getRegionY() + radius; y++) {
                    int[] m = mapping(p, x, y);
                    if (m[0] != 0 && m[1] != 0) ids.add((m[0] >> 3) << 8 | (m[1] >> 3));
                }
        for (int id : ids) {
            if (!LandscapeParser.parseLandscape(id, MapXTEA.getKey(id)))
                throw new IllegalStateException("Unable to load landscape " + id);
        }
        return ids;
    }
    public static Message build(Location location, int depth) {
        synchronized (RegionBuilder.class) {
            Set<Integer> ids = sourceRegions(location, depth);
            int radius = radius(depth);
            MessageBuilder b = new MessageBuilder(31, PacketType.VAR_SHORT);
            b.writeByteA(1);
            b.writeByteA(depth);
            b.writeShortA(location.getRegionY());
            b.writeLEShort(location.getRegionX());
            b.writeByteA(1);
            b.startBitAccess();
            for (int p = 0; p < 4; p++)
                for (int x = location.getRegionX() - radius; x <= location.getRegionX() + radius; x++)
                    for (int y = location.getRegionY() - radius; y <= location.getRegionY() + radius; y++) {
                        int[] m = mapping(p, x, y);
                        boolean built = m[0] != 0 && m[1] != 0;
                        b.writeBits(1, built ? 1 : 0);
                        if (built) b.writeBits(26, (m[3] << 1) | (m[2] << 24) | (m[0] << 14) | (m[1] << 3));
                    }
            b.finishBitAccess();
            for (int id : ids) {
                int[] keys = MapXTEA.getKey(id);
                for (int i = 0; i < 4; i++) b.writeInt(keys == null ? 0 : keys[i]);
            }
            return b.toMessage();
        }
    }
    private DynamicMapPacket() { }
}
