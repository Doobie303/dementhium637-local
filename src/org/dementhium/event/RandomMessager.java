package org.dementhium.event;

import org.dementhium.model.player.Player;
import org.dementhium.model.World;
import org.dementhium.util.Misc;

/**
 * @author Wildking72
 */
public class RandomMessager implements Runnable {

		private int delay;
		
		public RandomMessager(int delay) {
			this.delay = delay;
		}
		
		private String[] messages = {
			"Don't forget to vote at www.dynamicpvp.com",
			"We are currently on bonus XP for four more days!",
			"Let's get some PvP action going on at ::edge",
			"See where you stand at www.dynamicpvp.com/highscores"
		};
		
		@Override
		public void run() {
			try {
				while (true) {
					for (Player p : World.getWorld().getPlayers()) {
						if(!p.isOnline() || p == null) continue;
						p.sendMessage("<img=5>News: <col=ff0000>" + messages[Misc.random(messages.length - 1)]);
					}
					Thread.sleep(delay);
				}
			} catch (Exception e) {
				System.out.println("Error: " + e);
			}
		}
		
}