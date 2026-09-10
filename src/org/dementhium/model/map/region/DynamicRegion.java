package org.dementhium.model.map.region;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.dementhium.model.Location;
import org.dementhium.model.map.GameObject;
import org.dementhium.model.map.Region;
import org.dementhium.model.player.Player;
import org.dementhium.net.ActionSender;

/** One 64-tile cache region, containing independently owned 8-tile chunks.
 * No collision/object reads are forwarded to a mutable source after copying. */
public final class DynamicRegion {
    private static final java.util.concurrent.atomic.AtomicLong NEXT_GENERATION = new java.util.concurrent.atomic.AtomicLong();
    private final long generation = NEXT_GENERATION.incrementAndGet();
    public long getGeneration() { return generation; }
    private final int regionId;
    private final Chunk[][][] chunks = new Chunk[4][8][8];
    private long revision;

    static final class Chunk {
        final int sourceX, sourceY, sourcePlane;
        final long version = NEXT_GENERATION.incrementAndGet();
        final int[][] masks = new int[8][8];
        final int[][][] contributions = new int[8][8][];
        final List<GameObject>[][] objects;
        final Set<Integer> changes = new HashSet<Integer>();
        @SuppressWarnings("unchecked")
        Chunk(int sourceX, int sourceY, int sourcePlane, int destX, int destY, int plane) {
            this.sourceX = sourceX; this.sourceY = sourceY; this.sourcePlane = sourcePlane;
            objects = (List<GameObject>[][]) new List[8][8];
            for (int x = 0; x < 8; x++) for (int y = 0; y < 8; y++) {
                masks[x][y] = Region.getClippingMask(sourceX * 8 + x, sourceY * 8 + y, sourcePlane);
                for (int type : Location.locate(sourceX * 8 + x, sourceY * 8 + y, sourcePlane).getChangedObjectTypes())
                    changes.add((x << 8) | (y << 5) | type);
                for (GameObject source : Location.locate(sourceX * 8 + x, sourceY * 8 + y, sourcePlane).getObjectsSnapshot()) {
                    if (objects[x][y] == null) objects[x][y] = new ArrayList<GameObject>();
                    objects[x][y].add(new GameObject(source.getId(), destX * 8 + x, destY * 8 + y,
                            plane, source.getType(), source.getRotation()));
                }
            }
        }
    }

    public DynamicRegion(int regionId) { this.regionId = regionId; }
    void put(int plane, int x, int y, Chunk chunk) { chunks[plane][x][y] = chunk; revision++; }
    public long getRevision() { return revision; }
    public long getChunkRevision(int plane, int x, int y) {
        Chunk c = chunks[plane][x >> 3][y >> 3]; return c == null ? 0 : c.version;
    }
    public boolean isEmpty() {
        for (Chunk[][] plane : chunks) for (Chunk[] row : plane) for (Chunk c : row) if (c != null) return false;
        return true;
    }
    public int[] getChunkMapping(int plane, int x, int y) {
        Chunk c = chunks[plane][x][y];
        return c == null ? new int[4] : new int[]{c.sourceX, c.sourceY, c.sourcePlane, 0};
    }
    /** Compatibility snapshot. Mutating this array does not mutate a built map. */
    public int[][][][] getRegionCoords() {
        int[][][][] result = new int[4][8][8][];
        for (int p = 0; p < 4; p++) for (int x = 0; x < 8; x++) for (int y = 0; y < 8; y++)
            result[p][x][y] = getChunkMapping(p, x, y);
        return result;
    }
    public int getMask(int plane, int x, int y) {
        Chunk c = chunks[plane][x >> 3][y >> 3];
        return c == null ? -1 : c.masks[x & 7][y & 7];
    }
    public void changeMask(int plane, int x, int y, int mask, boolean add) {
        Chunk c = chunks[plane][x >> 3][y >> 3];
        if (c == null) return; // Unbuilt space stays blocked, including object spillover.
        int lx = x & 7, ly = y & 7;
        int[] counts = c.contributions[lx][ly];
        if (counts == null) {
            counts = c.contributions[lx][ly] = new int[32];
            for (int bit = 0; bit < 32; bit++) if ((c.masks[lx][ly] & (1 << bit)) != 0) counts[bit] = 1;
        }
        // Preserve pre-existing flags and overlapping additions when a temporary
        // object is removed. The source's packed mask is the initial contribution.
        for (int bit = 0; bit < 32; bit++) if ((mask & (1 << bit)) != 0) {
            if (add) counts[bit]++;
            else if (counts[bit] > 0) counts[bit]--;
            if (counts[bit] == 0) c.masks[lx][ly] &= ~(1 << bit);
            else c.masks[lx][ly] |= 1 << bit;
        }
    }
    public List<GameObject> getObjects(int plane, int x, int y) {
        Chunk c = chunks[plane][x >> 3][y >> 3];
        return c == null ? null : c.objects[x & 7][y & 7];
    }
    public void addObject(GameObject object) {
        Location l = object.getLocation(); int x = l.getX() & 63, y = l.getY() & 63;
        Chunk c = chunks[l.getZ()][x >> 3][y >> 3];
        if (c == null) throw new IllegalStateException("Cannot add an object to an unbuilt chunk");
        if (c.objects[x & 7][y & 7] == null) c.objects[x & 7][y & 7] = new ArrayList<GameObject>();
        c.objects[x & 7][y & 7].add(object);
        c.changes.add(((x & 7) << 8) | ((y & 7) << 5) | object.getType());
    }
    public void removeObject(GameObject object) {
        if (object == null) return;
        Location l = object.getLocation(); int x = l.getX() & 63, y = l.getY() & 63;
        Chunk c = chunks[l.getZ()][x >> 3][y >> 3];
        if (c != null && c.objects[x & 7][y & 7] != null && c.objects[x & 7][y & 7].remove(object))
            c.changes.add(((x & 7) << 8) | ((y & 7) << 5) | object.getType());
    }
    public GameObject getObject(int id, int x, int y, int plane) {
        List<GameObject> list = getObjects(plane, x, y);
        if (list != null) for (GameObject o : list) if (o.getId() == id) return o;
        return null;
    }
    /** Replay changed slots after the client rebuilds the cache scene. */
    public void refreshObjects(Player player) {
        int p = player.getLocation().getZ();
        for (int cx = 0; cx < 8; cx++) for (int cy = 0; cy < 8; cy++) {
            Chunk c = chunks[p][cx][cy]; if (c == null) continue;
            for (int key : c.changes) {
                int x = key >> 8, y = (key >> 5) & 7, type = key & 31;
                int wx = (regionId >> 8) * 64 + cx * 8 + x, wy = (regionId & 255) * 64 + cy * 8 + y;
                ActionSender.deleteObject(player, -1, wx, wy, p, type, 0);
                List<GameObject> list = c.objects[x][y];
                if (list != null) for (GameObject o : list) if (o.getType() == type) ActionSender.sendObject(player, o);
            }
        }
    }
}
