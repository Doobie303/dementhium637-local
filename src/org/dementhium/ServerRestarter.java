package org.dementhium;

import org.dementhium.Restart;

public class ServerRestarter {

	public static void main(String[] args) {
		
		int time = 1000 * 7200; // 2 Hours
		
		Restart.restart("Server Restart Initialized!", time);
	
		System.out.println("\nTime until Server Restart:");
		
		while (time > 0) {
		try {
			Thread.sleep(1000);
			if (time < 1000 * 11) {
				System.out.println("Server Restart in " + time / 1000 + " seconds!");
			} else {
				System.out.println(time / 1000 + " seconds left.");
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			}
			time -= 1000;
		}
	}
}
