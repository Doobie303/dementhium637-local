package org.dementhium.content.activity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.dementhium.model.World;

/** Stable registration IDs. ID 0 remains the first (Castle Wars) registration. */
public class ActivityManager {
    private static final ActivityManager SINGLETON = new ActivityManager();
    private final Map<Integer, Activity<?>> activities = new LinkedHashMap<Integer, Activity<?>>();
    private final Map<Activity<?>, Integer> ids = new IdentityHashMap<Activity<?>, Integer>();
    private int nextId;

    public synchronized boolean register(Activity<?> activity) {
        if (activity == null || !activity.isRunning() || ids.containsKey(activity) || activity.registrationManager() != null)
            return false;
        if (nextId == Integer.MAX_VALUE) throw new IllegalStateException("Activity ID capacity exhausted");
        int id = nextId++;
        activity.attachRegistration(this, id);
        activities.put(id, activity);
        ids.put(activity, id);
        World.getWorld().submit(new org.dementhium.tickable.Tick(1) {
            public void execute() {
                synchronized (ActivityManager.this) {
                    if (activities.get(id) != activity || !activity.run()) stop();
                }
            }
        });
        return true;
    }

    public synchronized boolean unregister(int id, boolean endSession) {
        Activity<?> activity = activities.remove(id);
        if (activity == null) return false;
        ids.remove(activity);
        // Detach and stop BEFORE invoking user code: recursive stop cannot end twice.
        activity.detachRegistration();
        if (endSession) activity.finishSession();
        return true;
    }
    public synchronized boolean unregister(Activity<?> activity, boolean endSession) {
        Integer id = ids.get(activity);
        return id != null && unregister(id, endSession);
    }
    public boolean unregister(Activity<?> activity) { return unregister(activity, true); }

    public synchronized boolean reset() {
        RuntimeException failure = null;
        for (Activity<?> activity : new ArrayList<Activity<?>>(activities.values())) {
            try { unregister(activity, true); }
            catch (RuntimeException e) {
                if (failure == null) failure = e; else failure.addSuppressed(e);
            }
        }
        if (failure != null) throw failure;
        return activities.isEmpty();
    }
    /** Compatibility setter performs lifecycle operations, never installs a caller-owned list. */
    public synchronized void setActivities(List<Activity<?>> replacements) {
        if (replacements == null) throw new IllegalArgumentException("Missing activities");
        List<Activity<?>> copy = new ArrayList<Activity<?>>(replacements);
        java.util.Set<Activity<?>> unique = Collections.newSetFromMap(new IdentityHashMap<Activity<?>, Boolean>());
        for (Activity<?> activity : copy)
            if (activity == null || !unique.add(activity) || !activity.isRunning()
                    || (activity.registrationManager() != null && activity.registrationManager() != this))
                throw new IllegalArgumentException("Invalid, duplicate or foreign activity");
        for (Activity<?> activity : new ArrayList<Activity<?>>(activities.values()))
            if (!unique.contains(activity)) unregister(activity, true);
        for (Activity<?> activity : copy) if (!ids.containsKey(activity)) register(activity);
    }
    public synchronized List<Activity<?>> getActivities() {
        return Collections.unmodifiableList(new ArrayList<Activity<?>>(activities.values()));
    }
    public synchronized Activity<?> getActivity(int id) { return activities.get(id); }
    public static ActivityManager getSingleton() { return SINGLETON; }
}
