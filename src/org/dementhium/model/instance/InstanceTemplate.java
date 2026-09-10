package org.dementhium.model.instance;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.dementhium.model.Location;

/** Immutable unrotated map definition. Coordinates are chunks for maps, tiles for anchors. */
public final class InstanceTemplate {
    /** Copies loaded collision/objects at creation time; not an immutable cache baseline. */
    public enum SourcePolicy { SNAPSHOT_LOADED_WORLD }

    public static final class Tile {
        public final int x, y, plane;
        public Tile(int x, int y, int plane) {
            if (x < 0 || y < 0 || plane < 0 || plane > 3)
                throw new IllegalArgumentException("Invalid local tile");
            this.x = x; this.y = y; this.plane = plane;
        }
    }

    private final String id;
    private final int sourceX, sourceY, width, height, sourcePlane, destinationPlane;
    private final SourcePolicy policy;
    private final Map<String, Tile> anchors;

    public InstanceTemplate(String id, int sourceX, int sourceY, int width, int height,
            int sourcePlane, int destinationPlane, SourcePolicy policy, Map<String, Tile> anchors) {
        if (id == null || id.trim().isEmpty() || sourceX < 0 || sourceY < 0 || width < 1 || height < 1
                || width > 2048 || height > 2048 || sourceX > 2048 - width || sourceY > 2048 - height
                || sourcePlane < 0 || sourcePlane > 3 || destinationPlane < 0 || destinationPlane > 3
                || policy == null || anchors == null || anchors.isEmpty())
            throw new IllegalArgumentException("Invalid template");
        Map<String, Tile> copy = new LinkedHashMap<String, Tile>();
        for (Map.Entry<String, Tile> entry : anchors.entrySet()) {
            Tile tile = entry.getValue();
            if (entry.getKey() == null || entry.getKey().trim().isEmpty() || tile == null
                    || tile.x >= width * 8 || tile.y >= height * 8 || tile.plane != destinationPlane)
                throw new IllegalArgumentException("Anchor outside template");
            copy.put(entry.getKey(), tile);
        }
        this.id = id; this.sourceX = sourceX; this.sourceY = sourceY;
        this.width = width; this.height = height; this.sourcePlane = sourcePlane;
        this.destinationPlane = destinationPlane; this.policy = policy;
        this.anchors = Collections.unmodifiableMap(copy);
    }

    public String getId() { return id; }
    public SourcePolicy getSourcePolicy() { return policy; }
    public Map<String, Tile> getAnchors() { return anchors; }
    public Tile getAnchor(String name) {
        Tile tile = anchors.get(name);
        if (tile == null) throw new IllegalArgumentException("Unknown template anchor: " + name);
        return tile;
    }

    /** The manager owns rollback, including failures in content initialization. */
    public GameInstance create(InstanceManager manager, int capacity, Location exit, Consumer<GameInstance> populate) {
        if (manager == null || populate == null) throw new IllegalArgumentException("Missing manager/initializer");
        return manager.create(width, height, capacity, exit, instance -> {
            instance.attachTemplate(this);
            instance.copyMap(sourceX, sourceY, 0, 0, width, height,
                    new int[]{sourcePlane}, new int[]{destinationPlane});
            populate.accept(instance);
        });
    }
}
