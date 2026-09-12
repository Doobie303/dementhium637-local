package org.dementhium;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import org.dementhium.model.World;
import org.dementhium.util.DementhiumThreadFactory;

/**
 *
 * @author 'Mystic Flow <Steven@rune-server.org>
 */
public class ServerThread extends Thread {
	
	public ServerThread() {
		super("ServerThread");
		start();
	}
	

	public static ScheduledExecutorService service = Executors.newScheduledThreadPool(1, new DementhiumThreadFactory("GameLogic", Thread.MAX_PRIORITY));
	public static ScheduledExecutorService service2 = Executors.newScheduledThreadPool(4, new DementhiumThreadFactory("GameLogic", Thread.MAX_PRIORITY));
	
	public void run() {
		while (true) {
			check();
		}
	}

	private void check() {
		ThreadPoolExecutor executor = (ThreadPoolExecutor) service;
		// This thread is the sole world-tick producer. Keep at most one pending
		// tick; further wake-ups coalesce while it waits. Never interrupt the
		// active update or create replacement workers during ordinary backlog.
		if (executor.getQueue().isEmpty()) {
			service.submit(World.getWorld());
		}
		try {
			Thread.sleep(600);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
