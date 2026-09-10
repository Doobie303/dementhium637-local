package org.dementhium.model.map.region;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.dementhium.cache.CacheManager;
import org.dementhium.cache.format.LandscapeParser;
import org.dementhium.model.Location;
import org.dementhium.model.map.ObjectManager;
import org.dementhium.model.map.Region;
import org.dementhium.model.misc.GroundItemManager;
import org.dementhium.model.player.Player;
import org.dementhium.util.MapXTEA;

/** Map infrastructure, not an activity/session manager.
 * Tiles = world coordinates; chunks = 8x8; cache regions = 64x64;
 * the server's spatial Region is 128x128. Publish map edits on the game thread. */
public final class RegionBuilder {
    public static final int CHUNK_SIZE = 8, CACHE_REGION_SIZE = 64, ALLOCATION_ALIGNMENT = 128;
    private static final int MIN_TILE = 2688, MAX_X = 9984, MAX_Y = 16000, GUARD = 128;
    private static final Map<Integer, DynamicRegion> dynamicRegions = new HashMap<Integer, DynamicRegion>();
    private static final Map<Integer, MapAllocation> owners = new HashMap<Integer, MapAllocation>();
    private static final Map<Long, MapAllocation> allocations = new HashMap<Long, MapAllocation>();
    private static long nextId = 1;

    /** Previously copied 800x800 tiles to (4000,4000), plus a Nex chunk.
     * No activity references those destinations. Runtime maps are now built explicitly. */
    public static void init() { }

    /** @deprecated Historical name means tile-to-chunk, not tile-to-cache-region. */
    @Deprecated public static int getRegion(int tile) { return tileToChunk(tile); }
    public static int tileToChunk(int tile) { return tile >> 3; }
    public static int cacheRegionId(int tileX, int tileY) { return (tileX >> 6) << 8 | (tileY >> 6); }

    public static synchronized MapAllocation reserveMap(int widthChunks, int heightChunks) {
        return reserve(widthChunks, heightChunks, false);
    }

    private static MapAllocation reserve(int width, int height, boolean legacy) {
        dimensions(width, height);
        int tilesX = ((width + 15) / 16) * 128, tilesY = ((height + 15) / 16) * 128;
        for (int x = MIN_TILE + GUARD; x + tilesX + GUARD <= MAX_X; x += 128) {
            for (int y = MIN_TILE + GUARD; y + tilesY + GUARD <= MAX_Y; y += 128) {
                boolean free = true;
                for (int rx = x - GUARD; free && rx < x + tilesX + GUARD; rx += 64)
                    for (int ry = y - GUARD; ry < y + tilesY + GUARD; ry += 64)
                        if (!emptyRegion(rx, ry)) { free = false; break; }
                if (!free) continue;
                MapAllocation a = new MapAllocation(nextId++, x, y, width, height, legacy);
                allocations.put(a.getId(), a);
                for (int rx = x - GUARD; rx < a.endX + GUARD; rx += 64)
                    for (int ry = y - GUARD; ry < a.endY + GUARD; ry += 64) {
                        int id = cacheRegionId(rx, ry);
                        owners.put(id, a);
                        dynamicRegions.put(id, new DynamicRegion(id));
                    }
                return a;
            }
        }
        return null; // Exhaustion is an ordinary admission failure.
    }

    private static boolean emptyRegion(int x, int y) {
        int id = cacheRegionId(x, y);
        return !dynamicRegions.containsKey(id)
                && !hasCacheMap(id) && !Region.hasRuntimeState(x, y, 64, 64)
                && !GroundItemManager.hasItemsInArea(x, y, 64, 64);
    }
    /** Blank cache space eligible for the allocator cannot be a durable login/teleport destination. */
    public static synchronized boolean isUnmappedInstanceSpace(Location location) {
        if (location.getX()<MIN_TILE || location.getX()>=MAX_X || location.getY()<MIN_TILE || location.getY()>=MAX_Y)
            return false;
        int id=cacheRegionId(location.getX(),location.getY());
        return !dynamicRegions.containsKey(id) && CacheManager.getFIT(5)!=null && !hasCacheMap(id);
    }
    private static boolean hasCacheMap(int id) {
        int x = id >> 8, y = id & 255;
        return CacheManager.getFIT(5).findName("m" + x + "_" + y) != -1
                || CacheManager.getFIT(5).findName("l" + x + "_" + y) != -1;
    }

    /** @deprecated Reserves immediately; legacy callers must retain/release the allocation.
     * New code must use reserveMap and the handle-based copy/release methods. */
    @Deprecated public static synchronized int[] findEmptyMap(int widthChunks, int heightChunks) {
        MapAllocation a = reserve(widthChunks, heightChunks, true);
        return a == null ? null : new int[]{a.getX(), a.getY()};
    }
    public static synchronized MapAllocation getAllocation(int tileX, int tileY) {
        return owners.get(cacheRegionId(tileX, tileY));
    }
    private static void live(MapAllocation a) {
        if (a == null || a.released || allocations.get(a.getId()) != a)
            throw new IllegalStateException("Map allocation is not live");
    }

    /** Release only after evacuating entities and removing drops. Object state is map-owned.
     * Session task cancellation and membership will be supplied by the instance lifecycle layer. */
    public static synchronized void releaseMap(MapAllocation a) {
        if (a == null) throw new IllegalArgumentException("Missing allocation");
        if (a.released) return;
        live(a);
        int x = a.getX() - GUARD, y = a.getY() - GUARD;
        int width = a.endX + GUARD - x, height = a.endY + GUARD - y;
        requireUnoccupied(x, y, width, height);
        for (int rx = x; rx < x + width; rx += 64) for (int ry = y; ry < y + height; ry += 64) {
            int id = cacheRegionId(rx, ry);
            ObjectManager.forgetDynamicRegion(id);
            dynamicRegions.remove(id);
            owners.remove(id);
        }
        allocations.remove(a.getId());
        a.released = true;
        Region.discardEmptyArea(x, y, width, height);
    }

    private static void requireUnoccupied(int x, int y, int width, int height) {
        if (Region.hasEntitiesInArea(x, y, width, height) || GroundItemManager.hasItemsInArea(x, y, width, height))
            throw new IllegalStateException("Evacuate entities and remove ground items before clearing a map");
    }
    private static void dimensions(int w, int h) {
        if (w <= 0 || h <= 0 || w > 512 || h > 512)
            throw new IllegalArgumentException("Map dimensions must be 1..512 chunks");
    }
    private static void rectangle(int x, int y, int w, int h) {
        dimensions(w, h);
        if (x <= 0 || y <= 0 || x > 2047 || y > 2047 || x + w > 2048 || y + h > 2048)
            throw new IllegalArgumentException("Chunk coordinates exceed the 14-bit world");
    }
    private static void planes(int[] from, int[] to) {
        if (from == null || to == null || from.length == 0 || from.length != to.length)
            throw new IllegalArgumentException("Plane lists must be nonempty and have equal length");
        Set<Integer> seen = new HashSet<Integer>();
        for (int i = 0; i < from.length; i++)
            if (from[i] < 0 || from[i] > 3 || to[i] < 0 || to[i] > 3 || !seen.add(to[i]))
                throw new IllegalArgumentException("Invalid or repeated destination plane");
    }

    /** Offsets and dimensions are chunks relative to the allocation's tile origin. */
    public static synchronized void copyMap(MapAllocation a, int sourceChunkX, int sourceChunkY,
            int offsetChunkX, int offsetChunkY, int widthChunks, int heightChunks, int[] fromPlanes, int[] toPlanes) {
        live(a);
        if (offsetChunkX < 0 || offsetChunkY < 0 || (long)offsetChunkX + widthChunks > a.getWidthChunks()
                || (long)offsetChunkY + heightChunks > a.getHeightChunks())
            throw new IllegalArgumentException("Copy exceeds the reserved footprint");
        copy(sourceChunkX, sourceChunkY, a.getX() / 8 + offsetChunkX, a.getY() / 8 + offsetChunkY,
                widthChunks, heightChunks, fromPlanes, toPlanes, a);
    }

    /** Append-only handle API. Map preparation is atomic with respect to validation/load failures. */
    public static synchronized void appendMap(MapAllocation a, int sx, int sy, int ox, int oy,
            int width, int height, int[] from, int[] to) {
        live(a); dimensions(width, height); planes(from, to);
        if (ox < 0 || oy < 0 || (long)ox + width > a.getWidthChunks() || (long)oy + height > a.getHeightChunks())
            throw new IllegalArgumentException("Room exceeds the reserved footprint");
        // Append only: even empty built chunks cannot be replaced by active content.
        // All planes and the full rectangle are checked before copy prepares/publishes anything.
        for (int p : to) for (int x = 0; x < width; x++) for (int y = 0; y < height; y++) {
            int tx = a.getX() + (ox + x) * 8, ty = a.getY() + (oy + y) * 8;
            DynamicRegion region = dynamicRegions.get(cacheRegionId(tx, ty));
            if (region == null || owners.get(cacheRegionId(tx, ty)) != a
                    || region.getChunkRevision(p, tx & 63, ty & 63) != 0)
                throw new IllegalStateException("Room must use only unbuilt owned chunks");
        }
        copyMap(a, sx, sy, ox, oy, width, height, from, to);
    }

    /** Legacy fixed-coordinate maps may only use uncached, unreserved destinations. */
    public static synchronized void copyMap(int sx, int sy, int dx, int dy, int w, int h, int[] from, int[] to) {
        copy(sx, sy, dx, dy, w, h, from, to, null);
    }

    private static void copy(int sx, int sy, int dx, int dy, int w, int h, int[] from, int[] to, MapAllocation owner) {
        rectangle(sx, sy, w, h); rectangle(dx, dy, w, h); planes(from, to);
        requireUnoccupied(dx * 8, dy * 8, w * 8, h * 8);
        if (sx + w > 1024) throw new IllegalArgumentException("Source X exceeds the dynamic packet field");
        Set<Integer> sources = new HashSet<Integer>();
        for (int x = 0; x < w; x++) for (int y = 0; y < h; y++) {
            int sourceId = cacheRegionId((sx + x) * 8, (sy + y) * 8);
            if (dynamicRegions.containsKey(sourceId) || !hasCacheMap(sourceId))
                throw new IllegalArgumentException("Source must be an existing ordinary cache map: " + sourceId);
            sources.add(sourceId);
            int tx = (dx + x) * 8, ty = (dy + y) * 8;
            int destId = cacheRegionId(tx, ty);
            MapAllocation reserved = owners.get(destId);
            if (reserved != null && reserved != owner
                    && !(owner == null && reserved.legacy && reserved.contains(tx, ty)))
                throw new IllegalStateException("Destination belongs to another allocation or its guard");
            if (hasCacheMap(destId)) throw new IllegalArgumentException("Cannot overwrite an ordinary cache map");
            if (!dynamicRegions.containsKey(destId) && Region.hasRuntimeState((tx >> 6) * 64, (ty >> 6) * 64, 64, 64))
                throw new IllegalStateException("Destination contains ordinary runtime map state");
        }
        // Load adjacent source regions as well: walls and large objects can clip across
        // a 64-tile cache boundary. Missing/undecodable dependencies abort the build.
        Set<Integer> dependencies = new HashSet<Integer>(sources);
        for (int id : sources) for (int ox = -1; ox <= 1; ox++) for (int oy = -1; oy <= 1; oy++) {
            int rx = (id >> 8) + ox, ry = (id & 255) + oy;
            if (rx < 0 || rx > 255 || ry < 0 || ry > 255) continue;
            int adjacent = (rx << 8) | ry;
            if (!dynamicRegions.containsKey(adjacent) && hasCacheMap(adjacent)) dependencies.add(adjacent);
        }
        // Validate all source maps before publishing any destination chunk.
        for (int id : dependencies)
            if (!LandscapeParser.parseLandscape(id, MapXTEA.getKey(id)))
                throw new IllegalStateException("Source landscape failed to load: " + id);
        DynamicRegion.Chunk[][][] prepared = new DynamicRegion.Chunk[from.length][w][h];
        for (int p = 0; p < from.length; p++) for (int x = 0; x < w; x++) for (int y = 0; y < h; y++)
            prepared[p][x][y] = new DynamicRegion.Chunk(sx + x, sy + y, from[p], dx + x, dy + y, to[p]);
        for (int p = 0; p < from.length; p++) for (int x = 0; x < w; x++) for (int y = 0; y < h; y++) {
            int id = cacheRegionId((dx + x) * 8, (dy + y) * 8);
            DynamicRegion r = dynamicRegions.get(id);
            if (r == null) { r = new DynamicRegion(id); dynamicRegions.put(id, r); }
            ObjectManager.forgetDynamicChunk((dx + x) * 8, (dy + y) * 8, to[p]);
            r.put(to[p], (dx + x) & 7, (dy + y) & 7, prepared[p][x][y]);
        }
    }

    public static void copyRegion(int sx, int sy, int sp, int dx, int dy, int dp, int rotation) {
        if (rotation != 0) throw new IllegalArgumentException("Rotated chunks are not supported yet");
        copyMap(sx, sy, dx, dy, 1, 1, new int[]{sp}, new int[]{dp});
    }
    public static void copyAllPlanesMap(int sx, int sy, int dx, int dy, int ratio) {
        copyMap(sx, sy, dx, dy, ratio, ratio, new int[]{0,1,2,3}, new int[]{0,1,2,3});
    }
    public static void copyMap(int sx, int sy, int dx, int dy, int ratio, int[] from, int[] to) {
        copyMap(sx, sy, dx, dy, ratio, ratio, from, to);
    }
    /** Legacy wrappers take tile origins and chunk dimensions. */
    public static void copyAllHeights(int sx, int sy, int dx, int dy, int ratio) {
        copyAllPlanesMap(sx / 8, sy / 8, dx / 8, dy / 8, ratio);
    }
    public static void copy(int sx, int sy, int dx, int dy, int ratio) {
        copyMap(sx / 8, sy / 8, dx / 8, dy / 8, ratio, new int[]{0}, new int[]{0});
    }
    public static void destroyAllPlanesMap(int dx, int dy, int ratio) {
        destroyMap(dx, dy, ratio, ratio, new int[]{0,1,2,3}, new int[]{0,1,2,3});
    }
    public static synchronized void destroyMap(int dx, int dy, int w, int h, int[] from, int[] to) {
        rectangle(dx, dy, w, h); planes(from, to);
        requireUnoccupied(dx * 8, dy * 8, w * 8, h * 8);
        for (int x = 0; x < w; x++) for (int y = 0; y < h; y++) {
            MapAllocation a = owners.get(cacheRegionId((dx + x) * 8, (dy + y) * 8));
            if (a != null && (!a.legacy || !a.contains((dx + x) * 8, (dy + y) * 8))) throw new IllegalStateException("Use the allocation handle to release this map");
        }
        for (int x = 0; x < w; x++) for (int y = 0; y < h; y++) {
            int id = cacheRegionId((dx + x) * 8, (dy + y) * 8);
            DynamicRegion r = dynamicRegions.get(id); if (r == null) continue;
            for (int p : to) {
                ObjectManager.forgetDynamicChunk((dx + x) * 8, (dy + y) * 8, p);
                r.put(p, (dx + x) & 7, (dy + y) & 7, null);
            }
            if (r.isEmpty() && !owners.containsKey(id)) dynamicRegions.remove(id);
        }
    }
    public static void cutRegion(int x, int y, int plane) {
        destroyMap(x, y, 1, 1, new int[]{plane}, new int[]{plane});
    }
    public static synchronized void destroyDynamicRegion(int id) {
        if (owners.containsKey(id)) throw new IllegalStateException("Release the complete allocation by handle");
        if (!dynamicRegions.containsKey(id)) return;
        requireUnoccupied((id >> 8) * 64, (id & 255) * 64, 64, 64);
        ObjectManager.forgetDynamicRegion(id);
        dynamicRegions.remove(id);
    }
    public static synchronized DynamicRegion getDynamicRegion(int id) { return dynamicRegions.get(id); }
    public static DynamicRegion getDynamicRegion(int x, int y) { return getDynamicRegion(cacheRegionId(x, y)); }
    public static synchronized int getAllocationCount() { return allocations.size(); }
    public static synchronized int getDynamicRegionCount() { return dynamicRegions.size(); }

    public static synchronized boolean isDynamicViewport(Location l, int depth) {
        int radius = Location.VIEWPORT_SIZES[depth] >> 4;
        for (int x = (l.getRegionX() - radius) >> 3; x <= (l.getRegionX() + radius) >> 3; x++)
            for (int y = (l.getRegionY() - radius) >> 3; y <= (l.getRegionY() + radius) >> 3; y++)
                if (dynamicRegions.containsKey((x << 8) | y)) return true;
        return false;
    }
    public static synchronized long sceneRevision(Location l, int depth) {
        long result = 1; int radius = Location.VIEWPORT_SIZES[depth] >> 4;
        for (int x = (l.getRegionX() - radius) >> 3; x <= (l.getRegionX() + radius) >> 3; x++)
            for (int y = (l.getRegionY() - radius) >> 3; y <= (l.getRegionY() + radius) >> 3; y++) {
                DynamicRegion r = dynamicRegions.get((x << 8) | y);
                if (r != null) result = result * 31 + r.getGeneration() * 31 + r.getRevision();
            }
        return result;
    }
    public static synchronized void refreshObjects(Player player) {
        for (int id : player.getMapRegionIds()) {
            DynamicRegion r = dynamicRegions.get(id);
            if (r != null) r.refreshObjects(player);
        }
    }
    private RegionBuilder() { }
}
