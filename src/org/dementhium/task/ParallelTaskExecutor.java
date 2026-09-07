package org.dementhium.task;

import java.util.Deque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.dementhium.task.runnable.CountdownTask;
import org.dementhium.util.DementhiumThreadFactory;

/**
 * @author 'Mystic Flow
 */
public class ParallelTaskExecutor {

    private ExecutorService parallelExecutor;

    public ParallelTaskExecutor() {
        parallelExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), new DementhiumThreadFactory("ParallelTaskExecutor"));
    }

    public void performTasks(Deque<Task> tasks) {
        if (!tasks.isEmpty()) {
            CountDownLatch latch = new CountDownLatch(tasks.size());
            Task task;
            while ((task = tasks.poll()) != null) {
                parallelExecutor.submit(new CountdownTask(task, latch));
            }
            try {
                latch.await(600, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
