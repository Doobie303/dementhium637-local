import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.dementhium.cache.Cache;
import org.dementhium.cache.format.CacheItemDefinition;
import org.dementhium.content.skills.summoning.Familiar;
import org.dementhium.model.*;
import org.dementhium.model.definition.*;
import org.dementhium.model.player.*;
import org.dementhium.net.message.MessageBuilder;
import org.dementhium.net.packethandlers.*;
import org.jboss.netty.buffer.*;

/** Real bank/packet/container paths; no live client or production accounts. */
public class BankSafetyRegression extends ItemTransactionRegression {
    interface Scenario { void run() throws Exception; }
    static int failed;
    static Player banker() {
        Player p = p();
        p.setHasReceivedStarter(true);
        p.getBank().openBank();
        return p;
    }
    static void click(Player p, int opcode, int ui, int component, int slot, int id) {
        MessageBuilder b = new MessageBuilder(opcode);
        b.writeShort(ui).writeShort(component).writeLEShortA(slot).writeShort(id);
        new ActionButtonHandler().handlePacket(p, b.toMessage());
    }
    static void drag(Player p, int fromHash, int from, int fromItem, int toHash, int to, int toItem) {
        MessageBuilder b = new MessageBuilder(10);
        b.writeInt(fromHash).writeShortA(toItem).writeShort(fromItem).writeLEInt(toHash)
                .writeLEShortA(from).writeLEShort(to);
        new SwitchItemHandler().handlePacket(p, b.toMessage());
    }
    static void input(Player p, int value) {
        MessageBuilder b = new MessageBuilder(34); b.writeInt(value);
        new InputPacketHandler().handlePacket(p, b.toMessage());
    }
    static void tab(Player p, int tab, int... ids) {
        for (int id : ids) p.getBank().commandAdd(id, 1, tab);
    }
    static Familiar yak(Player p) {
        Familiar f = new Familiar(p, 6873, 100); p.setFamiliar(f); return f;
    }
    static void run(String name, Scenario scenario) throws Exception {
        try { scenario.run(); System.out.println("PASS scenario: " + name); }
        catch (AssertionError | RuntimeException e) { failed++; System.out.println("FAIL scenario: " + name + " - " + e); }
    }
    static void quantity() {
        for (int id : new int[] {995, 1127}) for (int amount : new int[] {1, 5, 10, 20, Integer.MAX_VALUE}) {
            Player p = banker(); p.getBank().set(0, new Item(id, 10));
            p.getBank().removeItem(0, amount);
            int moved = Math.min(10, amount);
            check(count(p.getInventory().getContainer(), id) == moved && count(p.getBank().getContainer(), id) == 10 - moved,
                    "withdraw clamps to stock id=" + id + " requested=" + amount);
        }
    }
    static void noteOverflow() {
        Player p = banker(); p.setAttribute("noting", true);
        p.getBank().set(0, new Item(1127, 10)); p.getInventory().set(0, new Item(1128, Integer.MAX_VALUE - 3));
        p.getBank().removeItem(0, 10);
        check(count(p.getInventory().getContainer(), 1128) == Integer.MAX_VALUE && count(p.getBank().getContainer(), 1127) == 7,
                "note withdrawal transfers only the three notes that fit");
    }
    static void noteFull() {
        Player p = banker(); p.setAttribute("noting", true); p.getBank().set(0, new Item(1127, 10));
        fill(p.getInventory().getContainer(), 0); p.getInventory().set(0, new Item(1128, 5));
        p.getBank().removeItem(0, 5);
        check(count(p.getInventory().getContainer(), 1128) == 10 && count(p.getBank().getContainer(), 1127) == 5,
                "full inventory can receive existing notes");
        p.getInventory().set(0, new Item(1127)); String before = image(p.getBank().getContainer());
        p.getBank().removeItem(0, 5);
        check(before.equals(image(p.getBank().getContainer())), "failed note addition never debits the bank");
    }
    static void selectedMetadata() {
        Player p = banker(); p.getBank().set(0, worn(4716, 42)); p.getBank().set(1, new Item(4716));
        p.getBank().removeItem(1, 1);
        check(p.getBank().get(0).getHealth() == 42 && p.getBank().get(1) == null,
                "ordinary withdrawal cannot consume a different charged slot of the same ID");
        p.getInventory().set(1, new Item(4716)); p.getBank().bankInv();
        check(p.getBank().get(0).getHealth() == 42 && count(p.getBank().getContainer(), 4716) == 3,
                "ordinary deposits never merge into a charged stack");
    }
    static void chargedTab() {
        Player p = banker(); tab(p, 2, 995); tab(p, 10, 1127); p.setLastBankTab(2);
        p.getInventory().set(0, worn(4716, 42)); p.getBank().addItem(0, 1);
        check(p.getBank().getItemsInTab(2) == 2 && p.getBank().getTabByItemSlot(1) == 2 && p.getBank().get(1).getHealth() == 42,
                "charged deposit respects selected tab");
    }
    static void bulkNotes() {
        Player p = banker(); Familiar f = yak(p); fill(p.getBank().getContainer(), 0);
        p.getBank().set(0, new Item(1127, Integer.MAX_VALUE - 2));
        f.getContainer().set(0, new Item(1128, 5)); p.getBank().bankBob();
        check(p.getBank().get(0).getId() == 1127 && p.getBank().get(0).getAmount() == Integer.MAX_VALUE
                && f.getContainer().get(0).getAmount() == 3 && p.getBank().get(1).getId() == 4151,
                "full-bank bulk notes merge into canonical stack with exact remainder");
    }
    static void sparseBob() {
        Player p = banker(); Familiar f = yak(p); f.getContainer().set(4, new Item(1127));
        p.getBank().bankBob();
        check(f.getContainer().size() == 0 && p.getBank().contains(1127), "bulk BoB deposit handles empty slot zero");
    }
    static void quickBob() {
        Player p = banker(); Familiar f = yak(p); f.getContainer().set(0, new Item(1127));
        p.getBank().bankBob(); p.getInventory().set(0, new Item(995, 100));
        long before = count(p.getInventory().getContainer(), 1); f.quickWithdraw();
        check(count(p.getInventory().getContainer(), 1) == before && f.getContainer().size() == 0,
                "quick withdrawal cannot create item ID derived from deposited quantity");
    }
    static void staleClick() {
        Player p = banker(); p.getBank().set(0, new Item(995)); p.getBank().set(1, new Item(1127));
        click(p, 6, 762, 93, 0, 995); click(p, 6, 762, 93, 0, 995);
        check(p.getBank().contains(1127) && !p.getInventory().contains(1127), "replayed click cannot withdraw shifted neighbour");
        p.getInventory().set(5, new Item(1127)); click(p, 6, 763, 0, 5, 995);
        check(p.getInventory().get(5) != null, "deposit packet must match selected item");
    }
    static void staleX() {
        Player p = banker(); p.getBank().set(0, new Item(995)); p.getBank().set(1, new Item(1127));
        click(p, 46, 762, 93, 0, 995); p.getBank().removeItem(0, 1); input(p, 1);
        check(p.getBank().contains(1127), "numeric response cannot consume replacement slot");
        click(p, 46, 762, 93, 0, 1127); p.getBank().openBank(); input(p, 1);
        check(p.getBank().contains(1127), "reopening cancels outstanding numeric action");
    }
    static void dragValidation() {
        Player p = banker(); p.getInventory().set(0, new Item(1127));
        drag(p, 763 << 16, 0, 1127, 763 << 16, 28, -1);
        check(p.getInventory().get(0) != null, "out-of-range inventory drag cannot delete source");
        tab(p, 10, 995, 1127); String before = image(p.getBank().getContainer());
        drag(p, (762 << 16) | 93, 0, 4151, (762 << 16) | 93, 1, 1127);
        check(before.equals(image(p.getBank().getContainer())), "stale drag source rejected");
        p.closeAll(false, false);
        drag(p, (762 << 16) | 93, 0, 995, (762 << 16) | 60, -1, -1);
        check(before.equals(image(p.getBank().getContainer())) && p.getBank().getItemsInTab(2) == 0,
                "closed bank rejects tab drag");
    }
    static void fullTabMove() {
        Player p = banker(); tab(p, 2, 995); fill(p.getBank().getContainer(), 1);
        drag(p, (762 << 16) | 93, 0, 995, (762 << 16) | 62, -1, -1);
        check(p.getBank().get(515).getId() == 995 && count(p.getBank().getContainer(), 4151) == 515
                && p.getBank().getItemsInTab(2) == 0, "moving to main tab of full bank appends item safely");
    }
    static void collapseSelection() {
        Player p = banker(); tab(p, 2, 995); tab(p, 3, 1127); p.setLastBankTab(3);
        p.getBank().removeItem(0, 1);
        check(p.getLastBankTab() == 2 && p.getBank().getItemsInTab(2) == 1,
                "removing earlier tab keeps the viewed tab attached to its items");
        p.getBank().collapseTab(10);
        check(p.getBank().contains(1127), "main tab cannot be collapsed");
    }
    static void noteLinks() {
        check(ItemDefinition.forId(1127).getCacheDefinition().getCertId() == 1128
                && ItemDefinition.forId(1128).getCacheDefinition().getCertId() == 1127,
                "decoder retains real reciprocal note links");
        int nonAdjacent = -1;
        for (ItemDefinition definition : ItemDefinition.getDefinitions()) {
            if (definition == null || !definition.isNoted() || definition.getId() == 10843) continue;
            int linked = definition.getCacheDefinition().getCertId();
            if (linked >= 0 && linked < ItemDefinition.MAX_SIZE && linked != definition.getId() - 1
                    && ItemDefinition.forId(linked).getCacheDefinition().getCertId() == definition.getId()) { nonAdjacent = definition.getId(); break; }
        }
        check(nonAdjacent >= 0, "cache contains a non-adjacent note pair beyond the old special case");
        int unnoted = ItemDefinition.forId(nonAdjacent).getCacheDefinition().getCertId();
        Player p = banker(); p.getInventory().set(0, new Item(nonAdjacent, 5)); p.getBank().addItem(0, 5);
        check(p.getBank().contains(unnoted, 5) && p.getInventory().get(0) == null, "non-adjacent note deposit uses cache link " + nonAdjacent + " -> " + unnoted);
        p.setAttribute("noting", true); p.getBank().removeItem(0, 5);
        check(p.getInventory().numberOf(nonAdjacent) == 5 && p.getBank().getContainer().size() == 0, "non-adjacent note round trip conserves items");
        System.out.println("Verified non-adjacent cache note pair: " + unnoted + "/" + nonAdjacent);
    }
    static void familiarMetadata() {
        Player p = banker(); Familiar f = yak(p); p.getInventory().set(0, worn(4716, 42));
        check(f.store(4716, 0, 1) && f.getContainer().get(0).getHealth() == 42,
                "familiar storage preserves charge metadata before banking");
        p.getBank().bankBob();
        check(p.getBank().get(0).getHealth() == 42, "familiar-to-bank preserves charges");
    }
    static void familiarCapacity() {
        for (int free : new int[] {0, 1, 3, 28}) {
            Player p = banker(); Familiar f = yak(p); fill(f.getContainer(), free);
            for (int slot = 0; slot < 5; slot++) p.getInventory().set(slot, new Item(1127));
            f.store(1127, 0, Integer.MAX_VALUE);
            int moved = Math.min(5, free);
            check(count(f.getContainer(), 1127) == moved && p.getInventory().numberOf(1127) == 5 - moved,
                    "familiar partial storage conserves nonstack items with free slots " + free);
            p = banker(); f = yak(p); fill(p.getInventory().getContainer(), free);
            for (int slot = 0; slot < 5; slot++) f.getContainer().set(slot, new Item(1127));
            f.withdraw(1127, 0, Integer.MAX_VALUE, true);
            check(count(f.getContainer(), 1127) == 5 - moved && p.getInventory().numberOf(1127) == moved,
                    "familiar partial withdrawal conserves nonstack items with free slots " + free);
        }
        Player p = banker(); Familiar f = yak(p); fill(f.getContainer(), 1);
        f.getContainer().set(0, new Item(995, Integer.MAX_VALUE - 2)); p.getInventory().set(0, new Item(995, 10));
        f.store(995, 0, Integer.MAX_VALUE);
        check(f.numberOf(995) == Integer.MAX_VALUE && p.getInventory().numberOf(995) == 8,
                "full familiar accepts existing stack up to MAX and retains source remainder");
        p = banker(); f = yak(p); fill(p.getInventory().getContainer(), 1);
        p.getInventory().set(0, new Item(995, Integer.MAX_VALUE - 2)); f.getContainer().set(0, new Item(995, 10));
        f.withdraw(995, 0, Integer.MAX_VALUE, true);
        check(p.getInventory().numberOf(995) == Integer.MAX_VALUE && f.numberOf(995) == 8,
                "full inventory accepts existing familiar stack up to MAX");
        p = banker(); f = yak(p); fill(p.getInventory().getContainer(), 2);
        Item charged = worn(4716, 42); charged.setAmount(5); f.getContainer().set(0, charged);
        f.withdraw(4716, 0, 5, true);
        check(count(f.getContainer(), 4716) == 3 && f.getContainer().get(0).getHealth() == 42
                && f.getContainer().get(1).getHealth() == 42 && f.getContainer().get(2).getHealth() == 42
                && p.getInventory().get(0).getHealth() == 42 && p.getInventory().get(1).getHealth() == 42,
                "partial familiar withdrawal preserves charges on both sides");
        p = banker(); f = yak(p); f.getContainer().set(4, new Item(995, 100)); f.getContainer().set(7, worn(4716, 42));
        check(f.quickWithdraw() && f.getContainer().size() == 0 && p.getInventory().numberOf(995) == 100
                && p.getInventory().get(1).getHealth() == 42, "quick withdrawal collects actual held items and charges");
    }
    static void familiarScroll() {
        Player p = banker(); Familiar f = yak(p); fill(p.getBank().getContainer(), 0);
        p.getInventory().set(0, new Item(1127)); p.getInventory().set(1, new Item(12435, 5));
        p.setAttribute("itemId", 1127); p.setAttribute("itemSlot", 0);
        int points = f.getSpecialPoints(); f.specialMove(null);
        check(p.getInventory().numberOf(12435) == 5 && p.getInventory().get(0) != null && f.getSpecialPoints() == points,
                "rejected familiar banking does not consume scroll or special points");
        p.getBank().set(515, null); f.specialMove(null);
        check(p.getInventory().get(0) == null && p.getBank().contains(1127) && p.getInventory().numberOf(12435) == 4,
                "successful familiar banking transfers exactly one item and consumes one scroll");
        check(Boolean.TRUE.equals(p.getAttribute("inBank", false)), "scroll cannot close an existing bank session");
    }
    static void holes() {
        Player p = banker(); tab(p, 2, 995, 1127); tab(p, 3, 1163); tab(p, 10, 4151);
        // Same raw slot removal used by existing administrative item-removal commands.
        p.getBank().set(0, null); p.getBank().refresh();
        check(p.getBank().get(0).getId() == 1127 && p.getBank().getItemsInTab(2) == 1
                && p.getBank().getTabByItemSlot(1) == 3, "refresh compacts removed slots and updates tab boundaries");
    }
    static void familiarX() {
        Player p = banker(); Familiar f = yak(p); f.open(); p.getInventory().set(0, new Item(995, 100));
        click(p, 46, 665, 0, 0, 995); input(p, 25);
        check(p.getInventory().numberOf(995) == 75 && f.numberOf(995) == 25, "Store-X commits the requested transfer");
        click(p, 46, 671, 27, 0, 995); input(p, 5);
        check(p.getInventory().numberOf(995) == 80 && f.numberOf(995) == 20, "Withdraw-X commits the requested transfer");
        click(p, 46, 671, 27, 0, 995); f.getContainer().set(0, new Item(1127)); input(p, 1);
        check(f.getContainer().get(0) != null, "familiar X rejects stale item snapshot");
    }
    static void familiarPersistence() throws Exception {
        Player p = banker(); Familiar f = yak(p); f.getContainer().set(0, worn(4716, 42));
        ChannelBuffer b = ChannelBuffers.dynamicBuffer(); p.save(b);
        Player loaded = p(); loaded.load(ByteBuffer.wrap(b.array(), 0, b.writerIndex()));
        check(loaded.getFamiliar().getContainer().get(0).getHealth() == 42,
                "save/load preserves familiar item charges before depositing them into the bank");
    }
    static void chargedQuantity() {
        Player p = banker(); p.getInventory().set(0, worn(4716, 42)); p.getInventory().set(1, worn(4716, 43));
        click(p, 67, 763, 0, 0, 4716);
        check(p.getInventory().getContainer().size() == 0 && p.getBank().get(0).getHealth() == 42
                && p.getBank().get(1).getHealth() == 43, "Deposit-All transfers individual charged copies without merging them");
        p.getBank().get(0).setAmount(5); fill(p.getInventory().getContainer(), 2); p.getBank().removeItem(0, 5);
        check(p.getInventory().get(0).getHealth() == 42 && p.getInventory().get(1).getHealth() == 42
                && p.getBank().get(0).getAmount() == 3, "charged stack withdrawals preserve metadata and exact partial quantity");
    }
    static void corruptBank() {
        Player p = banker(); p.getBank().set(0, new Item(ItemDefinition.MAX_SIZE, 10));
        long hash = p.getBank().get(0).getHash(); p.getBank().openBank();
        check(!Boolean.TRUE.equals(p.getAttribute("inBank", false)) && p.getBank().get(0).getHash() == hash,
                "invalid saved item blocks opening without deleting or mutating its record");
        p.getBank().set(0, new Item(995, 100)); p.getBank().getTab()[3] = -1; p.setLastBankTab(999);
        p.getBank().openBank(); p.getInventory().set(0, new Item(1127)); p.getBank().addItem(0, 1);
        check(p.getBank().contains(995, 100) && p.getBank().contains(1127) && p.getLastBankTab() == 10,
                "invalid tab metadata recovers to main tab without losing items");
    }
    static void boundaries() {
        for (int requested : new int[] {Integer.MIN_VALUE, -1, 0, 1, 5, 10, 20, Integer.MAX_VALUE}) {
            for (int id : new int[] {995, 1127, 1128}) {
                Player p = banker();
                if (id == 1127) for (int slot = 0; slot < 10; slot++) p.getInventory().set(slot, new Item(id));
                else p.getInventory().set(0, new Item(id, 10));
                int canonical = id == 1128 ? 1127 : id;
                p.getBank().addItem(0, requested);
                int moved = Math.max(0, Math.min(10, requested));
                check(count(p.getInventory().getContainer(), id) == 10 - moved
                        && count(p.getBank().getContainer(), canonical) == moved, "deposit quantity boundary " + id + "/" + requested);
            }
        }
        for (int free : new int[] {0, 1, 3, 28}) {
            Player p = banker(); p.getBank().set(0, new Item(1127, 50)); fill(p.getInventory().getContainer(), free);
            p.getBank().removeItem(0, Integer.MAX_VALUE);
            check(count(p.getInventory().getContainer(), 1127) == free && count(p.getBank().getContainer(), 1127) == 50 - free,
                    "nonstack withdrawal fits exactly " + free + " free slots");
        }
        Player p = banker(); p.getBank().set(0, new Item(995, Integer.MAX_VALUE - 2)); p.getInventory().set(0, new Item(995, 10));
        p.getBank().bankInv();
        check(p.getBank().get(0).getAmount() == Integer.MAX_VALUE && p.getInventory().numberOf(995) == 8,
                "bulk inventory deposit preserves max-stack overflow remainder");
        String before = image(p.getBank().getContainer()); p.getBank().bankInv();
        check(before.equals(image(p.getBank().getContainer())) && p.getInventory().numberOf(995) == 8, "repeated capped deposit is harmless");
        fill(p.getBank().getContainer(), 1); p.getInventory().set(1, new Item(1128, 5));
        p.getBank().bankInv(); check(p.getInventory().get(1).getAmount() == 5, "new noted item remains in inventory when bank is full");
        p.getBank().set(515, null); p.getInventory().set(2, new Item(1163)); p.getBank().bankInv();
        check(p.getBank().get(515).getId() == 1127 && p.getInventory().get(1) == null && p.getInventory().get(2) != null,
                "bulk deposit uses last bank slot once and retains later rejected item");
        p = banker(); p.getBank().set(0, new Item(995, 10)); p.getInventory().set(0, new Item(995, Integer.MAX_VALUE - 2));
        p.getBank().removeItem(0, Integer.MAX_VALUE);
        check(p.getBank().get(0).getAmount() == 8 && p.getInventory().numberOf(995) == Integer.MAX_VALUE, "stack withdrawal clips to exact maximum");
        p = banker(); p.getBank().set(0, new Item(6570, 10)); p.setAttribute("noting", true);
        before = image(p.getBank().getContainer()); p.getBank().removeItem(0, Integer.MAX_VALUE);
        check(before.equals(image(p.getBank().getContainer())) && p.getInventory().getContainer().size() == 0, "unnotable item rejects safely even with excessive request");
        p.setAttribute("noting", false); p.getBank().set(0, new Item(1128, 10)); p.getBank().removeItem(0, 5);
        check(p.getInventory().numberOf(1128) == 5 && p.getBank().get(0).getAmount() == 5, "legacy already-noted bank entry retains its representation");
    }
    static List<Integer> flatten(List<List<Integer>> tabs, List<Integer> main) {
        List<Integer> result = new ArrayList<Integer>(); for (List<Integer> tab : tabs) result.addAll(tab); result.addAll(main); return result;
    }
    static List<Integer> containing(List<List<Integer>> tabs, List<Integer> main, int id) {
        for (List<Integer> tab : tabs) if (tab.contains(id)) return tab; return main;
    }
    static void insertRoundTrip() {
        Player p = banker(); tab(p, 10, 1042, 18353, 995, 1127);
        p.setAttribute("inserting", true); String before = image(p.getBank().getContainer());
        drag(p, (762 << 16) | 93, 1, 18353, (762 << 16) | 93, 0, 1042);
        check(p.getBank().get(0).getId() == 18353, "chaotic maul inserts onto blue partyhat slot");
        drag(p, (762 << 16) | 93, 0, 18353, (762 << 16) | 93, 1, 1042);
        check(before.equals(image(p.getBank().getContainer())), "chaotic maul inserts back past blue partyhat");
        for (int from = 0; from < 4; from++) for (int to = 0; to < 4; to++) {
            int id = p.getBank().get(from).getId(), target = p.getBank().get(to).getId();
            drag(p, (762 << 16) | 93, from, id, (762 << 16) | 93, to, target);
            check(p.getBank().get(to).getId() == id, "insert reaches the requested slot " + from + " -> " + to);
            drag(p, (762 << 16) | 93, to, id, (762 << 16) | 93, from, p.getBank().get(from).getId());
            check(before.equals(image(p.getBank().getContainer())), "reverse insert restores order " + from + " -> " + to);
        }
    }
    static void customItems() throws Exception {
        List<Integer> ids = new ArrayList<Integer>();
        ids.add(org.dementhium.content.items.CustomItems.INFERNAL_CAPE);
        ids.addAll(org.dementhium.content.items.CustomItems.osrsEquipmentIds());
        for (int id : ids) {
            ItemDefinition definition = ItemDefinition.forId(id);
            check(!definition.isStackable() && !definition.isNoted() && definition.getCacheDefinition().getCertId() == -1,
                    "custom equipment has no invented note form: " + definition.getName());
            Player p = banker(); p.setLastBankTab(2);
            for (int slot = 0; slot < 3; slot++) p.getInventory().set(slot, new Item(id));
            click(p, 67, 763, 0, 0, id);
            check(p.getInventory().getContainer().size() == 0 && p.getBank().contains(id, 3)
                    && p.getBank().getItemsInTab(2) == 1, "custom Deposit-All stacks in selected tab: " + id);
            p.setAttribute("noting", true); String saved = image(p.getBank().getContainer());
            click(p, 6, 762, 93, 0, id);
            check(saved.equals(image(p.getBank().getContainer())) && p.getInventory().getContainer().size() == 0,
                    "custom note-mode rejection preserves item: " + id);
            p.setAttribute("noting", false); fill(p.getInventory().getContainer(), 1);
            click(p, 67, 762, 93, 0, id);
            check(p.getInventory().numberOf(id) == 1 && p.getBank().contains(id, 2), "custom withdrawal fits one free slot: " + id);
            saved = image(p.getBank().getContainer()); click(p, 6, 762, 93, 0, id);
            check(saved.equals(image(p.getBank().getContainer())), "custom full-inventory withdrawal retains bank: " + id);
            p.getInventory().getContainer().reset(); p.getEquipment().set(definition.getEquipmentSlot(), new Item(id));
            p.getBank().bankEquip();
            check(p.getEquipment().get(definition.getEquipmentSlot()) == null && p.getBank().contains(id, 3),
                    "custom equipment deposit retains original ID: " + id);
            p.getBank().commandAdd(995, 1, 10); p.setAttribute("inserting", true);
            drag(p, (762 << 16) | 93, 0, id, (762 << 16) | 93, 1, 995);
            check(p.getBank().get(1).getId() == id && p.getBank().getTabByItemSlot(1) == 10,
                    "custom item inserts across tab boundary: " + id);
            saved = image(p.getBank().getContainer()); int[] tabs = p.getBank().getTab().clone();
            ChannelBuffer buffer = ChannelBuffers.dynamicBuffer(); p.save(buffer);
            Player loaded = p(); loaded.load(ByteBuffer.wrap(buffer.array(), 0, buffer.writerIndex())); loaded.getBank().openBank();
            check(saved.equals(image(loaded.getBank().getContainer())) && Arrays.equals(tabs, loaded.getBank().getTab()),
                    "custom bank ID, amount, order and tabs survive save/load: " + id);
            p = banker(); fill(p.getBank().getContainer(), 0); p.getInventory().set(0, new Item(id));
            saved = image(p.getBank().getContainer()); p.getBank().bankInv();
            check(saved.equals(image(p.getBank().getContainer())) && p.getInventory().numberOf(id) == 1,
                    "custom new item rejected safely by full bank: " + id);
            p.getBank().set(0, new Item(id, 2)); p.getBank().bankInv();
            check(p.getBank().contains(id, 3) && p.getInventory().getContainer().size() == 0,
                    "custom existing stack accepts deposit into full bank: " + id);
            System.out.println("Verified custom bank item: " + id + " " + definition.getName());
        }
    }
    static void rearrangements() {
        Player p = banker(); List<List<Integer>> tabs = new ArrayList<List<Integer>>(); List<Integer> main = new ArrayList<Integer>();
        int[] ids = {995,1511,1127,1163,4151,11732,11724,11726,1187,11283,1149,4587,1215,5698,1305,1434,1377,1249,10828,3749};
        for (int t = 0; t < 4; t++) { List<Integer> group = new ArrayList<Integer>(); tabs.add(group);
            for (int n = 0; n < 4; n++) { int id = ids[t * 4 + n]; group.add(id); tab(p, t + 2, id); } }
        for (int n = 16; n < ids.length; n++) { main.add(ids[n]); tab(p, 10, ids[n]); }
        Random random = new Random(637);
        for (int step = 0; step < 1000; step++) {
            List<Integer> flat = flatten(tabs, main); int from = random.nextInt(flat.size()), to = random.nextInt(flat.size());
            int sourceId = flat.get(from), targetId = flat.get(to); List<Integer> source = containing(tabs, main, sourceId);
            int op = random.nextInt(4);
            if (op < 2) {
                p.setAttribute("inserting", op == 1);
                drag(p, (762 << 16) | 93, from, sourceId, (762 << 16) | 93, to, targetId);
                if (from != to) {
                    List<Integer> target = containing(tabs, main, targetId);
                    if (op == 0) { int si = source.indexOf(sourceId), ti = target.indexOf(targetId); source.set(si, targetId); target.set(ti, sourceId); }
                    else { source.remove(Integer.valueOf(sourceId)); target.add(target.indexOf(targetId) + (to > from ? 1 : 0), sourceId); if (source != main && source.isEmpty()) tabs.remove(source); }
                }
            } else if (op == 2) {
                int selection = random.nextInt(tabs.size() + (tabs.size() < 8 ? 2 : 1));
                List<Integer> target;
                int targetTab;
                if (selection == tabs.size()) { target = main; targetTab = 10; }
                else if (selection > tabs.size()) { target = new ArrayList<Integer>(); targetTab = tabs.size() + 2; tabs.add(target); }
                else { target = tabs.get(selection); targetTab = selection + 2; }
                drag(p, (762 << 16) | 93, from, sourceId, (762 << 16) | (targetTab == 10 ? 62 : 64 - targetTab * 2), -1, -1);
                source.remove(Integer.valueOf(sourceId)); target.add(sourceId); if (source != main && source.isEmpty()) tabs.remove(source);
            } else if (!tabs.isEmpty()) {
                int t = random.nextInt(tabs.size()); p.getBank().collapseTab(t + 2); p.getBank().refresh(); main.addAll(tabs.remove(t));
            }
            List<Integer> expected = flatten(tabs, main); check(expected.size() == p.getBank().getContainer().size(), "rearrangement preserves occupied count " + step);
            int slot = 0;
            for (int t = 0; t < tabs.size(); t++) for (int id : tabs.get(t)) {
                check(p.getBank().get(slot).getId() == id && p.getBank().getTabByItemSlot(slot) == t + 2, "tab order and membership " + step + "/" + slot); slot++;
            }
            for (int id : main) { check(p.getBank().get(slot).getId() == id && p.getBank().getTabByItemSlot(slot) == 10, "main order and membership " + step + "/" + slot); slot++; }
            for (int t = 2; t < 10; t++) check(p.getBank().getItemsInTab(t) == (t - 2 < tabs.size() ? tabs.get(t - 2).size() : 0), "tab size " + step + "/" + t);
        }
    }
    static void diskPersistence() throws Exception {
        java.nio.file.Path root = java.nio.file.Files.createTempDirectory("bank-regression-");
        try {
            org.dementhium.io.PlayerLoader loader = new org.dementhium.io.PlayerLoader(root);
            Player p = banker(); tab(p, 2, 995, 1127); tab(p, 3, 4716); p.getBank().get(2).setHealth(42);
            Familiar f = yak(p); f.getContainer().set(4, worn(4716, 71));
            String bank = image(p.getBank().getContainer()), familiar = image(f.getContainer()); int[] tabs = p.getBank().getTab().clone();
            check(loader.save(p), "actual account save commits successfully to isolated storage");
            Player loaded = new Player(p.getConnection(), new PlayerDefinition(p.getUsername(), "unused"));
            check(loader.load(loaded) && bank.equals(image(loaded.getBank().getContainer())) && Arrays.equals(tabs, loaded.getBank().getTab())
                    && familiar.equals(image(loaded.getFamiliar().getContainer())), "actual disk reload preserves bank and familiar images");
            loaded.getBank().openBank(); loaded.getBank().removeItem(0, 1); check(loader.save(loaded), "post-withdrawal save commits");
            Player again = new Player(p.getConnection(), new PlayerDefinition(p.getUsername(), "unused"));
            check(loader.load(again) && again.getInventory().numberOf(995) == 1 && !again.getBank().contains(995), "reload records exactly one withdrawal");
        } finally {
            try (java.util.stream.Stream<java.nio.file.Path> paths = java.nio.file.Files.walk(root)) {
                for (java.nio.file.Path path : (Iterable<java.nio.file.Path>)paths.sorted(java.util.Comparator.reverseOrder())::iterator) java.nio.file.Files.delete(path);
            }
        }
    }
    static java.util.Map<Integer,Integer> cacheEnum(int id) throws Exception {
        byte[] data = org.dementhium.cache.CacheManager.getData(17, id >>> 8, id & 255);
        ByteBuffer b = ByteBuffer.wrap(data); java.util.Map<Integer,Integer> mapping = new java.util.LinkedHashMap<Integer,Integer>();
        while (b.hasRemaining()) {
            int opcode = b.get() & 255;
            if (opcode == 0) break;
            if (opcode == 1 || opcode == 2) b.get();
            else if (opcode == 4) b.getInt();
            else if (opcode == 6) { int size = b.getShort() & 65535; for (int n = 0; n < size; n++) mapping.put(b.getInt(), b.getInt()); }
            else throw new AssertionError("Unexpected bank enum opcode " + opcode);
        }
        return mapping;
    }
    static void cacheTabMappings() throws Exception {
        java.util.Map<Integer,Integer> mapping = cacheEnum(1613);
        check(!mapping.isEmpty(), "native bank drag-target enum decoded");
        for (java.util.Map.Entry<Integer,Integer> entry : mapping.entrySet()) {
            int component = entry.getKey() & 65535, tab = entry.getValue() == 1 ? 10 : entry.getValue();
            System.out.println("Cache bank drop target " + component + " -> " + tab);
            check(Bank.getArrayIndex(component) == tab, "server tab target matches cache enum: " + component + " -> " + tab);
        }
        for (java.util.Map.Entry<Integer,Integer> entry : cacheEnum(1612).entrySet()) {
            // Key 1 is the whole-bank spacer, not one of the existing drop targets.
            if (entry.getKey() == 1) continue;
            int tab = entry.getKey() == 0 ? 10 : entry.getKey();
            int component = entry.getValue() & 65535;
            check(Bank.getArrayIndex(component) == tab, "section-end drop target matches cache: " + component + " -> " + tab);
            Player p = banker(); tab(p, 10, 995);
            drag(p, (762 << 16) | 93, 0, 995, (762 << 16) | component, -1, -1);
            check(p.getBank().getTabByItemSlot(0) == tab, "section-end packet moves to intended tab " + tab);
        }
    }
    static void quantityPackets() {
        int[] opcodes = {6,13,0,15,67,82,46}, amounts = {1,5,10,7,10,9,4};
        for (int n = 0; n < opcodes.length; n++) {
            Player p = banker(); p.getSettings().setLastXAmount(7); p.getBank().set(0, new Item(995,10));
            click(p,opcodes[n],762,93,0,995); if(opcodes[n]==46) input(p,4);
            check(p.getInventory().numberOf(995)==amounts[n] && count(p.getBank().getContainer(),995)==10-amounts[n], "withdraw quantity packet " + opcodes[n]);
            if (opcodes[n] == 82) continue;
            p = banker(); p.getSettings().setLastXAmount(7); p.getInventory().set(0,new Item(995,10));
            click(p,opcodes[n],763,0,0,995); if(opcodes[n]==46) input(p,4);
            check(count(p.getBank().getContainer(),995)==amounts[n] && p.getInventory().numberOf(995)==10-amounts[n], "deposit quantity packet " + opcodes[n]);
        }
        Player p = banker(); p.getInventory().set(0,new Item(995,Integer.MAX_VALUE-1)); p.getInventory().set(1,new Item(995,5));
        click(p,67,763,0,0,995);
        check(p.getBank().get(0).getAmount()==Integer.MAX_VALUE && count(p.getInventory().getContainer(),995)==4,
                "Deposit-All does not overflow when duplicate source stacks total more than MAX_VALUE");
    }
    static void persistence() throws Exception {
        Player p = banker(); tab(p, 2, 995, 1127); tab(p, 3, 4716); p.getBank().get(2).setHealth(42);
        p.setLastBankTab(3); p.getSettings().setLastXAmount(123);
        String before = image(p.getBank().getContainer()); int[] tabs = p.getBank().getTab().clone();
        ChannelBuffer b = ChannelBuffers.dynamicBuffer(); p.save(b);
        Player loaded = p(); loaded.load(ByteBuffer.wrap(b.array(), 0, b.writerIndex()));
        check(before.equals(image(loaded.getBank().getContainer())) && Arrays.equals(tabs, loaded.getBank().getTab()),
                "production save/load preserves slots, amounts, charges and tab boundaries");
        check(loaded.getLastBankTab() == 3 && loaded.getSettings().getLastXAmount() == 123, "saved bank preferences survive");
        loaded.getBank().openBank(); check(before.equals(image(loaded.getBank().getContainer())), "reopen preserves bank image");
    }
    public static void main(String[] args) throws Exception {
        Cache.init(); ItemDefinition.init(); NPCDefinition.init();
        new org.dementhium.event.impl.interfaces.FamiliarInterfaceListener().register(org.dementhium.event.EventManager.getEventManager());
        run("insert round trip", BankSafetyRegression::insertRoundTrip);
        run("custom bank items", BankSafetyRegression::customItems);
        run("quantity", BankSafetyRegression::quantity);
        run("note overflow", BankSafetyRegression::noteOverflow);
        run("note full inventory", BankSafetyRegression::noteFull);
        run("selected metadata", BankSafetyRegression::selectedMetadata);
        run("charged tab", BankSafetyRegression::chargedTab);
        run("bulk notes", BankSafetyRegression::bulkNotes);
        run("sparse BoB", BankSafetyRegression::sparseBob);
        run("quick BoB", BankSafetyRegression::quickBob);
        run("stale clicks", BankSafetyRegression::staleClick);
        run("stale X", BankSafetyRegression::staleX);
        run("drag validation", BankSafetyRegression::dragValidation);
        run("full tab movement", BankSafetyRegression::fullTabMove);
        run("collapse selection", BankSafetyRegression::collapseSelection);
        run("note links", BankSafetyRegression::noteLinks);
        run("familiar metadata", BankSafetyRegression::familiarMetadata);
        run("familiar capacity", BankSafetyRegression::familiarCapacity);
        run("familiar scroll", BankSafetyRegression::familiarScroll);
        run("bank holes", BankSafetyRegression::holes);
        run("familiar X", BankSafetyRegression::familiarX);
        run("familiar persistence", BankSafetyRegression::familiarPersistence);
        run("charged quantity", BankSafetyRegression::chargedQuantity);
        run("corrupt bank", BankSafetyRegression::corruptBank);
        run("quantity and capacity boundaries", BankSafetyRegression::boundaries);
        run("rearrangements", BankSafetyRegression::rearrangements);
        run("disk persistence", BankSafetyRegression::diskPersistence);
        run("cache tab mappings", BankSafetyRegression::cacheTabMappings);
        run("quantity packets", BankSafetyRegression::quantityPackets);
        run("persistence", BankSafetyRegression::persistence);
        if (failed > 0) throw new AssertionError(failed + " bank scenarios failed (" + checks + " checks reached)");
        System.out.println("PASS: " + checks + " bank safety checks");
    }
}
