package org.dementhium;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Restart {

	public static final boolean restart(String message, long delay) {
		ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
		executor.schedule(new Runnable() {
			@Override
			public void run() {
				try {
					File file = new File("./~Run.bat");
					Runtime.getRuntime().exec("cmd.exe /C start " + file.getPath());
					File file2 = new File("./~Run.bat"); //Change to your Server runner
					Runtime.getRuntime().exec("cmd.exe /C start " + file2.getPath());
				} catch (Exception e) {
					e.printStackTrace();
				}
					System.exit(0);
			}
		}, delay, TimeUnit.MILLISECONDS);
		return true;
	}
}