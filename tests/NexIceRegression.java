import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.dementhium.cache.Cache;
import org.dementhium.cache.format.CacheObjectDefinition;
import org.dementhium.cache.format.LandscapeParser;
import org.dementhium.model.Location;
import org.dementhium.model.map.GameObject;
import org.dementhium.model.map.Region;
import org.dementhium.model.npc.impl.Nex.NexAreaEvent;
import org.dementhium.util.MapXTEA;

public final class NexIceRegression {
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        Cache.init();
        require(MapXTEA.loadPackedFile(), "Packed map keys missing");
        int region = ((2924 >> 6) << 8) | (5202 >> 6);
        require(LandscapeParser.parseLandscape(region, MapXTEA.getMapKeys().get(region)), "Arena map failed to load");
        NexAreaEvent area = NexAreaEvent.getNexAreaEvent();
        Method add = NexAreaEvent.class.getDeclaredMethod("addIceObject", Location.class, List.class, int.class);
        Method remove = NexAreaEvent.class.getDeclaredMethod("removeIceObjects", List.class);
        add.setAccessible(true); remove.setAccessible(true);
        Location walkway = Location.locate(2924, 5197, 0);
        Location pit = Location.locate(2919, 5196, 0);
        GameObject floor = walkway.getGameObjectType(22);
        require(floor != null, "Test requires a decorated walkway");
        int originalMask = Region.getClippingMask(2924, 5197, 0);
        List<Location> prison = new ArrayList<Location>();
        GameObject ice = (GameObject) add.invoke(area, walkway, prison, 57263);
        require(ice != null && ice.getId() == 57263, "Prison missing on decorated walkway");
        require("Attack".equals(CacheObjectDefinition.forId(ice.getId()).options[0]), "Prison lacks rescue action");
        require(walkway.getGameObjectType(22) == floor, "Ice replaced arena floor");
        List<Location> containment = new ArrayList<Location>();
        require(add.invoke(area, pit, containment, 57262) == null, "Ice spawned in pit");
        require(add.invoke(area, walkway, containment, 57262) == null, "Containment replaced active prison");
        remove.invoke(area, containment);
        require(walkway.getGameObjectType(10) == ice, "Unowned cleanup removed prison");
        remove.invoke(area, prison);
        require(walkway.getGameObjectType(10) == null && walkway.getGameObjectType(22) == floor, "Cleanup damaged scene");
        require(Region.getClippingMask(2924,5197,0) == originalMask, "Cleanup left collision behind");
        require(add.invoke(area, walkway, containment, 57262) != null, "Containment missing on decorated walkway");
        remove.invoke(area, containment);
        require(prison.isEmpty() && containment.isEmpty(), "Cleanup retained locations");
        System.out.println("PASS: decorated floor placement, pit exclusion, prison action, overlap ownership and collision cleanup");
    }
}
