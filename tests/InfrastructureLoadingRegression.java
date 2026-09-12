import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

import com.thoughtworks.xstream.XStream;
import org.dementhium.content.areas.impl.CircularArea;
import org.dementhium.content.areas.impl.IrregularArea;
import org.dementhium.content.areas.impl.RectangularArea;
import org.dementhium.event.EventListener;
import org.dementhium.event.EventManager;
import org.dementhium.identifiers.Identifier;
import org.dementhium.io.XMLHandler;
import org.dementhium.model.Item;
import org.dementhium.model.Location;
import org.dementhium.model.definition.WeaponInterface;
import org.dementhium.model.player.Player;
import org.dementhium.model.player.Shop;
import org.dementhium.model.player.ShopManager;
import org.dementhium.tools.converters.NPCConverter.NPCDefintion;

/** Run in a disposable build fixture, never the server working directory. */
public class InfrastructureLoadingRegression {
    private static int checks;
    private static int callbacks;

    public static void main(String[] args) throws Exception {
        check(Files.isRegularFile(Paths.get(".isolated-quality-fixture")), "isolated fixture required");
        Files.createDirectories(Paths.get("data"));
        String scenario = args[0];
        if (scenario.equals("xml")) xml();
        else if (scenario.startsWith("events-")) events(scenario);
        else if (scenario.startsWith("shops-")) shops(scenario);
        else throw new IllegalArgumentException(scenario);
        System.out.println("PASS " + scenario + ": " + checks + " checks");
    }

    private static void xml() throws Exception {
        Field field = XMLHandler.class.getDeclaredField("xmlHandler");
        field.setAccessible(true);
        XStream original = (XStream) field.get(null);
        aliases(original);
        ArrayList<Object> values = new ArrayList<Object>();
        values.add(new Item(995, 37));
        values.add(Location.locate(3200, 3201, 2));
        values.add("example_user");
        values.add(new RectangularArea(new int[]{3200, 3202, 3201, 3203}));
        XMLHandler.toXML("data/initial.xml", values);
        XStream replacement = new XStream();
        XMLHandler.setXmlHandler(replacement);
        aliases(replacement);
        XMLHandler.toXML("data/replacement.xml", values);
        check(Arrays.equals(Files.readAllBytes(Paths.get("data/initial.xml")),
                Files.readAllBytes(Paths.get("data/replacement.xml"))), "initial and replacement serialization agree");
        ArrayList<?> loaded = XMLHandler.fromXML("data/initial.xml");
        check(((Item) loaded.get(0)).getId() == 995 && ((Item) loaded.get(0)).getAmount() == 37, "item round trip");
        check(loaded.get(1).equals(values.get(1)), "location round trip");
        check(loaded.get(2).equals("example_user"), "ban string round trip");
        check(loaded.get(3) instanceof RectangularArea, "area alias round trip");
        XMLHandler.toXML("data/bans.xml", new ArrayList<String>(Arrays.asList("example_user")));
        check(((ArrayList<?>) XMLHandler.fromXML("data/bans.xml")).equals(Arrays.asList("example_user")),
                "offence string lists retain their save format");
    }

    private static void aliases(XStream stream) {
        String[] names = {"item", "rectangle", "circle", "irregular", "position", "identifier", "ban", "npcDefinition", "weaponInterface"};
        Class<?>[] types = {Item.class, RectangularArea.class, CircularArea.class, IrregularArea.class,
                Location.class, Identifier.class, String.class, NPCDefintion.class, WeaponInterface.class};
        for (int i = 0; i < names.length; i++) {
            check(stream.getMapper().serializedClass(types[i]).equals(names[i]), "write alias " + names[i]);
            check(stream.getMapper().realClass(names[i]) == types[i], "read alias " + names[i]);
        }
    }

    public static class FirstListener extends EventListener {
        public void register(EventManager manager) { manager.registerInterfaceListener(123, this); }
        public boolean interfaceOption(Player p, int id, int button, int slot, int item, int opcode) {
            callbacks = callbacks * 10 + 1;
            return true;
        }
    }

    public static class SecondListener extends EventListener {
        public void register(EventManager manager) { manager.registerInterfaceListener(123, this); }
        public boolean interfaceOption(Player p, int id, int button, int slot, int item, int opcode) {
            callbacks = callbacks * 10 + 2;
            return false;
        }
    }

    public static class FailingListener extends EventListener {
        public void register(EventManager manager) { throw new IllegalStateException("expected registration failure"); }
    }

    private static void events(String scenario) throws Exception {
        Path input = Paths.get("data/eventlisteners.txt");
        String listener = scenario.equals("events-class-failure") ? "missing.FixtureListener"
                : scenario.equals("events-register-failure") ? FailingListener.class.getName()
                : SecondListener.class.getName();
        Files.write(input, Arrays.asList("ignored comment", ">" + FirstListener.class.getName(), ">" + listener), StandardCharsets.UTF_8);
        EventManager manager = new EventManager();
        Exception failure = null;
        try { manager.load(); } catch (Exception e) { failure = e; }
        if (scenario.equals("events-class-failure")) check(failure instanceof ClassNotFoundException, "class failure propagates");
        else if (scenario.equals("events-register-failure")) check(failure instanceof IllegalStateException
                && failure.getMessage().equals("expected registration failure"), "registration failure propagates");
        else check(failure == null, "successful load");
        check(manager.handleInterfaceOption(null, 123, 0, 0, 0, 0), "successful registration remains usable");
        check(callbacks == (failure == null ? 12 : 1), "listener order and partial registration preserved");
        check(!manager.handleInterfaceOption(null, 124, 0, 0, 0, 0), "unregistered interface remains unhandled");
        released(input);
    }

    private static void shops(String scenario) throws Exception {
        Path input = Paths.get("data/shops.bin");
        int[] ids = {1, 105, 9711, 1282, 2620, 603};
        int[] currencies = {995, 19864, 11180, 8851, 6529, 5020};
        try (DataOutputStream output = new DataOutputStream(Files.newOutputStream(input))) {
            output.writeShort(ids.length);
            for (int i = 0; i < ids.length; i++) {
                if (scenario.equals("shops-failure") && i == 1) break;
                output.writeShort(ids[i]);
                output.writeByte(1);
                output.writeByte(i == 0 ? 1 : 0);
                output.writeShort(995);
                output.writeInt(17 + i);
            }
        }
        ShopManager manager = new ShopManager();
        PrintStream stderr = System.err;
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        try (PrintStream capture = new PrintStream(errors)) {
            System.setErr(capture);
            try { manager.load(); } finally { System.setErr(stderr); }
        }
        int count = scenario.equals("shops-failure") ? 1 : ids.length;
        check(manager.shops.size() == count, "loaded shop count including partial failure");
        check(count == ids.length ? errors.size() == 0 : errors.toString("UTF-8").contains("EOFException"), "load error reporting preserved");
        for (int i = 0; i < count; i++) {
            Shop shop = manager.getShop(ids[i]);
            check(shop.isGeneralStore() == (i == 0), "shop kind " + ids[i]);
            check((Integer) field(shop, "currency") == currencies[i], "custom currency " + ids[i]);
            check(Arrays.equals((int[]) field(shop, "origItems"), new int[]{995}), "stock identity " + ids[i]);
            check(Arrays.equals((int[]) field(shop, "origAmounts"), new int[]{17 + i}), "stock amount " + ids[i]);
        }
        released(input);
    }

    private static Object field(Object object, String name) throws Exception {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(object);
    }

    private static void released(Path input) throws Exception {
        // Windows refuses this rename while these Java 8 input handles remain open.
        // On other platforms this is only a file-usability check, not a leak detector.
        Path moved = input.resolveSibling(input.getFileName() + ".closed");
        Files.move(input, moved);
        Files.move(moved, input);
        check(Files.exists(input), "input released after load");
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
