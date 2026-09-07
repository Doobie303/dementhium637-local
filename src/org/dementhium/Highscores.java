package org.dementhium;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.dementhium.model.player.Player;
import org.dementhium.util.SQL;

/**
 *
 * @author 'Mystic Flow <Steven@rune-server.org>
 */
public class Highscores extends Thread {
	
	public static final Highscores INSTANCE = new Highscores();
	
	public Highscores() {
		super("Highscores");
		start();
	}
	
	public final BlockingQueue<Player> players = new LinkedBlockingQueue<Player>();
	
	public void init() {
		SQL.createConnection();
	}
	
	@Override
	public void run() {
		while (true) {
			Player player = null;
			try {
				player = players.take();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (player.getRights() != 2) {
			//	SQL.saveHighScore(player);
			}
		}
	}

}
