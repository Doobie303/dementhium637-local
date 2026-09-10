package org.dementhium.model.instance;

import java.io.*;
import java.util.*;

/** Immutable startup policy. Unknown keys and invalid limits fail startup rather than silently disabling protection. */
public final class InstanceLimits {
    private final Properties values = new Properties();
    private static final String[][] DEFAULTS = {
        {"sessions","32"}, {"members","128"}, {"membersPerSession","16"},
        {"chunkPlanes","16384"}, {"chunkPlanesPerSession","4096"}, {"reservedRegions","2048"},
        {"npcs","2048"}, {"npcsPerSession","128"}, {"objects","8192"}, {"objectsPerSession","1024"},
        {"drops","8192"}, {"dropsPerSession","512"}, {"tasks","16384"}, {"tasksPerSession","512"},
        {"requests","1024"}, {"requestsPerCycle","128"}, {"loginRequests","1024"},
        {"lifecyclePerCycle","256"}, {"createsPerCycle","1024"}, {"buildChunksPerCycle","65536"},
        {"admissionsPerCycle","1024"}, {"admissionsPerAccount","64"}, {"admissionWindowSeconds","60"},
        {"admissionAccounts","4096"}, {"emptySeconds","60"}, {"idleSeconds","0"}
    };
    public InstanceLimits(Properties overrides) {
        for (String[] pair : DEFAULTS) values.setProperty(pair[0], pair[1]);
        for (String key : overrides.stringPropertyNames()) {
            if (!values.containsKey(key)) throw new IllegalArgumentException("Unknown instance setting: " + key);
            values.setProperty(key, overrides.getProperty(key).trim());
        }
        for (String key : values.stringPropertyNames()) {
            int n;
            try { n = Integer.parseInt(values.getProperty(key)); }
            catch (NumberFormatException e) { throw new IllegalArgumentException("Invalid instance setting: " + key, e); }
            if (n < (key.equals("idleSeconds") ? 0 : 1) || n > 1000000)
                throw new IllegalArgumentException("Instance setting out of range: " + key);
        }
    }
    public int get(String key) {
        String value = values.getProperty(key);
        if (value == null) throw new IllegalArgumentException("Unknown instance limit: " + key);
        return Integer.parseInt(value);
    }
    public static InstanceLimits defaults() { return new InstanceLimits(new Properties()); }
    public static InstanceLimits load() {
        Properties properties = new Properties();
        File file = new File("settings/instances.properties");
        // Conservative production throttles also apply if the optional file is missing.
        properties.setProperty("createsPerCycle", "4");
        properties.setProperty("buildChunksPerCycle", "512");
        properties.setProperty("admissionsPerCycle", "32");
        properties.setProperty("admissionsPerAccount", "8");
        if (file.exists()) try (InputStream in = new FileInputStream(file)) { properties.load(in); }
        catch (IOException e) { throw new IllegalStateException("Cannot read " + file, e); }
        return new InstanceLimits(properties);
    }
}
