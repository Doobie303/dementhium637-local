package org.dementhium.model.map.region;

/** A live reservation. Coordinates are tiles; dimensions are 8-tile chunks.
 * Keep this handle: a released handle can never modify a later reservation. */
public final class MapAllocation {
    private final long id;
    private final int x, y, widthChunks, heightChunks;
    final int endX, endY;
    final boolean legacy;
    volatile boolean released;

    MapAllocation(long id, int x, int y, int widthChunks, int heightChunks, boolean legacy) {
        this.id = id; this.x = x; this.y = y;
        this.widthChunks = widthChunks; this.heightChunks = heightChunks;
        this.endX = x + ((widthChunks + 15) / 16) * 128;
        this.endY = y + ((heightChunks + 15) / 16) * 128;
        this.legacy = legacy;
    }
    public long getId() { return id; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidthChunks() { return widthChunks; }
    public int getHeightChunks() { return heightChunks; }
    public boolean isReleased() { return released; }
    public boolean contains(int tileX, int tileY) {
        return tileX >= x && tileY >= y && tileX < x + widthChunks * 8 && tileY < y + heightChunks * 8;
    }
    boolean protects(int tileX, int tileY) {
        return tileX >= x - 128 && tileY >= y - 128 && tileX < endX + 128 && tileY < endY + 128;
    }
}
