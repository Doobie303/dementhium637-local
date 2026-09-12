package org.dementhium;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.dementhium.model.World;
import org.dementhium.model.instance.InstanceManager;

/** Test-only executor instrumentation. The runner substitutes only the world executor factory
 * in an isolated source copy; ServerThread.check and World.run retain production logic. */
public final class WorldTickProbe {
    public static final long PERIOD = TimeUnit.MILLISECONDS.toNanos(600);
    public static final List<Executor> executors = new ArrayList<Executor>();
    public static final List<Row> rows = new ArrayList<Row>();
    public static volatile int request;
    public static volatile long deadline, submission;
    public static volatile Row current;
    public static final AtomicInteger interruptRequests = new AtomicInteger();
    public static int active, maxActive, ownerFailures;
    public static volatile int maxQueue;

    public static final class Row {
        public int request, generation, queue, tick, completedTick;
        public long deadline, submission, workerStart, entry, completion, threadId;
        public boolean interruptAtEntry, interruptAtExit, discarded;
        public Future<?> future;
    }

    public static synchronized Executor executor() {
        Executor executor = new Executor(executors.size());
        executors.add(executor);
        return executor;
    }

    public static final class Executor extends ScheduledThreadPoolExecutor {
        public final int generation;
        public final List<Thread> workers = new ArrayList<Thread>();
        public int replacements, returned;
        Executor(final int generation) {
            super(1);
            this.generation = generation;
            setThreadFactory(action -> {
                Thread thread = new Thread(action, "GameLogic-0") {
                    @Override public void interrupt() {
                        if (isAlive()) interruptRequests.incrementAndGet();
                        super.interrupt();
                    }
                };
                thread.setPriority(Thread.MAX_PRIORITY);
                synchronized (workers) { workers.add(thread); }
                return thread;
            });
        }
        @Override public Future<?> submit(final Runnable action) {
            if (action != World.getWorld()) throw new AssertionError("Unexpected world executor task");
            final Row row = new Row();
            row.request = request; row.generation = generation; row.queue = getQueue().size();
            row.deadline = deadline; row.submission = System.nanoTime();
            synchronized (rows) {
                if (rows.size() >= 2048) throw new AssertionError("Trace bound");
                rows.add(row);
            }
            row.future = super.submit(() -> {
                row.workerStart = System.nanoTime(); row.threadId = Thread.currentThread().getId();
                // Reentrant acquisition of the exact production monitor measures waiting separately.
                synchronized (World.getWorld()) {
                    row.entry = System.nanoTime(); row.tick = World.getTicks();
                    row.interruptAtEntry = Thread.currentThread().isInterrupted();
                    current = row; maxActive = Math.max(maxActive, ++active);
                    try { action.run(); }
                    finally {
                        row.completedTick = World.getTicks();
                        row.interruptAtExit = Thread.currentThread().isInterrupted();
                        if (InstanceManager.getSingleton().isCycleThread()) ownerFailures++;
                        row.completion = System.nanoTime(); active--; current = null;
                    }
                }
            });
            maxQueue = Math.max(maxQueue, getQueue().size());
            return row.future;
        }
        @Override public List<Runnable> shutdownNow() {
            replacements++;
            List<Runnable> pending = super.shutdownNow();
            returned += pending.size();
            synchronized (rows) {
                for (Row row : rows) if (pending.contains(row.future)) row.discarded = true;
            }
            return pending;
        }
        public void cleanup() { super.shutdownNow(); }
    }

    public static void ownerCheck() {
        if (!Thread.holdsLock(World.getWorld()) || !InstanceManager.getSingleton().isCycleThread()
                || current == null || current.threadId != Thread.currentThread().getId()) ownerFailures++;
    }

    public static void reset() {
        executors.clear(); rows.clear(); interruptRequests.set(0);
        active = 0; maxActive = 0; ownerFailures = 0;
    }
}
