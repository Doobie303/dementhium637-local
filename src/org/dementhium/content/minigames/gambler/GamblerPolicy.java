package org.dementhium.content.minigames.gambler;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.dementhium.model.Item;

/** Restart-loaded policy. No administrative odds overrides. */
public final class GamblerPolicy {
    public static final int VERSION = 1;
    public static final GamblerPolicy INSTANCE = load(Paths.get("settings/gambler.properties"));
    public final Map<Integer, Long> seeds;
    public final Map<Integer, Integer> caps;
    public final int minimumCoins;
    public final boolean enabled;
    private GamblerPolicy(Properties p) {
        enabled = Boolean.parseBoolean(p.getProperty("enabled", "true"));
        minimumCoins = Integer.parseInt(p.getProperty("minimum.coins", "10000"));
        Map<Integer, Long> balances = new LinkedHashMap<Integer, Long>();
        Map<Integer, Integer> limits = new LinkedHashMap<Integer, Integer>();
        for (String entry : p.getProperty("assets", "995:1000000000:1000000000").split(",")) {
            String[] fields = entry.trim().split(":");
            if (fields.length != 3) throw new IllegalArgumentException("Invalid gambler asset");
            int id = Integer.parseInt(fields[0]), cap = Integer.parseInt(fields[1]);
            long reserve = Long.parseLong(fields[2]);
            // Explicit, plain stackable commodities only. No arbitrary item IDs or pricing.
            if ((id != 995 && id != 560 && id != 565 && id != 561) || cap <= 0
                    || cap > Integer.MAX_VALUE / 2 || reserve < cap || limits.containsKey(id))
                throw new IllegalArgumentException("Invalid gambler asset limits");
            balances.put(id, reserve); limits.put(id, cap);
        }
        if (minimumCoins <= 0 || !limits.containsKey(995) || minimumCoins > limits.get(995))
            throw new IllegalArgumentException("Invalid minimum stake");
        seeds = Collections.unmodifiableMap(balances); caps = Collections.unmodifiableMap(limits);
    }
    public static GamblerPolicy load(Path path) {
        Properties p = new Properties();
        try {
            if (Files.exists(path)) try (InputStream in = Files.newInputStream(path)) { p.load(in); }
            return new GamblerPolicy(p);
        } catch (Exception e) { throw new IllegalStateException("Cannot load gambler policy", e); }
    }
    public boolean eligible(Item item) {
        return item != null && caps.containsKey(item.getId()) && item.getHealth() == 0
                && !item.getDefinition().isNoted() && item.getDefinition().isStackable()
                && item.getDefinition().isTradeable();
    }
    public boolean valid(int id, int amount) {
        Integer cap = caps.get(id);
        return enabled && cap != null && amount >= (id == 995 ? minimumCoins : 1) && amount <= cap;
    }
}
