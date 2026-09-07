package org.dementhium.task;

import java.util.Deque;

/**
 * @author 'Mystic Flow <Steven@rune-server.org>
 */
public class SequentialTaskExecutor {

	public void performTasks(Deque<Task> tasks) {
		Task task;
		while ((task = tasks.poll()) != null) {
			try {
				task.execute();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
