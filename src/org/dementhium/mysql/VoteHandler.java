package org.dementhium.mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.dementhium.model.World;
import org.dementhium.model.player.Player;
import org.dementhium.util.Misc;

/**
 *
 * @author Grumpy/Orel
 * @author Sixpack [rewriting]
 *
 */

public class VoteHandler {
	public static boolean Vote = true;

	final static int[][] itemData = {{995, 3000000}, {11180, 5000},{2714, 1}}; // {{itemID, amount}, {itemID, amount}}

	private static final String DB = "dynamicp_vote";
	private static final String URL = "www.dynamicpvp.com";
	private static final String USER = "dynamicp_vote";
	private static final String PASS = "password001";
	private static final Properties prop;
	static {
		prop = new Properties();
		prop.put("user", USER);
		prop.put("password", PASS);
		prop.put("autoReconnect", "true");
		prop.put("maxReconnects", "4");
	}

	public static Connection conn = null;

	/**
	 * Connects to the database
	 */
	public static synchronized void connect() {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection("jdbc:mysql://" + URL + "/" + DB, prop);
			System.out.println("Vote Handler: Success");
		} catch (Exception e) {
			System.out.println("Vote Handler Error: " + e);
			System.out.println("Setting vote to false to help not cause anymore errors.");
			Vote = false;
		}
	}


	public static synchronized Connection getConnection() {
		try {
			if (conn == null || conn.isClosed()) {
				conn = DriverManager.getConnection("jdbc:mysql://" + URL + "/"+ DB, prop);
			}
		} catch (SQLException e) {
			System.out.println(e);
			e.printStackTrace();
			Vote = false;
		}
		return conn;
	}


	/**
	 * giveItems, does a loop to give the player all of the items in the array
	 */
	public static synchronized void giveItems(Player player) {
		if(player.getInventory().getFreeSlots() >= itemData.length) {
			for (int i = 0; i < itemData.length; i++) {
				player.getInventory().addItem(itemData[i][0], itemData[i][1]);
				player.getInventory().refresh();
			}
			for (Player p : World.getWorld().getPlayers()) {
		
			p.sendMessage("<img=2><col=FF0000><shad=000000>" + Misc.formatPlayerNameForDisplay(player.getDisplayName())+ " has just claimed their vote reward, vote @ www.dynamicpvp.com");
			}
			player.sendMessage("Thanks for voting! You can vote every 24 hours @ www.dynamicpvp.com");
		} else {
			player.sendMessage("You must have " + itemData.length + " item slots to get your reward.");
		}
	}

	/**
	 * checkVote, will return true or false depending if the player has voted
	 */
	public static synchronized boolean checkVote(String auth) {
		try {
			ResultSet res = getConnection().createStatement().executeQuery("SELECT * FROM `votes` WHERE `used` = '0' AND `authcode` ='" + auth + "'LIMIT 1;");
			if (res.next())
				return true;
			else
				return false;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Updates the users vote in the database
	 */
	public static synchronized void updateVote(String auth) {
		try {
			getConnection().createStatement().execute("UPDATE `votes` SET `used` = '1' WHERE `authcode` = '" + auth + "';");
		} catch (Exception e) {
			System.out.println(e);
			e.printStackTrace();
	}
}
}