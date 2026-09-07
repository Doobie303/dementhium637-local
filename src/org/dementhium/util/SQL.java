package org.dementhium.util;

import java.sql.*;

import org.dementhium.model.player.Player;


public class SQL {


	public static Connection con = null;
	public static Statement stmt;
	public static boolean connectionMade;
	public static void createConnection() {
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			con = DriverManager.getConnection("jdbc:mysql://www.dynamicpvp.com:3306/dynamicp_highscores","dynamicp_hiscore","password001");
			stmt = con.createStatement();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static ResultSet query(String s) throws SQLException {
		try {
			if (s.toLowerCase().startsWith("select")) {
				ResultSet rs = stmt.executeQuery(s);
				return rs;
			} else {
				stmt.executeUpdate(s);
			}
			return null;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void destroyConnection() {
		try {
			stmt.close();
			con.close();
			connectionMade = false;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static boolean saveHighScore(Player player) {
		try {
			query("DELETE FROM `skills` WHERE playerName = '"+player.getUsername()+"';");
			query("DELETE FROM `skillsoverall` WHERE playerName = '"+ player.getUsername() +"';");
			query("DELETE FROM `skills` WHERE playerName = '"+player.getUsername()+"';");
			query("DELETE FROM `skillsoverall` WHERE playerName = '"+player.getUsername()+"';");
			query("INSERT INTO `skills` (`playerName`,`Attacklvl`,`Attackxp`,`Defencelvl`,`Defencexp`,`Strengthlvl`,`Strengthxp`,`Hitpointslvl`,`Hitpointsxp`,`Rangelvl`,`Rangexp`,`Prayerlvl`,`Prayerxp`,`Magiclvl`,`Magicxp`,`Cookinglvl`,`Cookingxp`,`Woodcuttinglvl`,`Woodcuttingxp`,`Fletchinglvl`,`Fletchingxp`,`Fishinglvl`,`Fishingxp`,`Firemakinglvl`,`Firemakingxp`,`Craftinglvl`,`Craftingxp`,`Smithinglvl`,`Smithingxp`,`Mininglvl`,`Miningxp`,`Herblorelvl`,`Herblorexp`,`Agilitylvl`,`Agilityxp`,`Thievinglvl`,`Thievingxp`,`Slayerlvl`,`Slayerxp`,`Farminglvl`,`Farmingxp`,`Runecraftlvl`,`Runecraftxp`,`Hunterlvl`,`Hunterxp`,`Constructionlvl`,`Constructionxp`,`Summoninglvl`,`Summoningxp`, `Dungeoneeringlvl`,`Dungeoneeringxp`) VALUES ('"+player.getUsername()+"',"+player.getSkills().getLevel(0)+","+player.getSkills().getXp(0)+","+player.getSkills().getLevel(1)+","+player.getSkills().getXp(1)+","+player.getSkills().getLevel(2)+","+player.getSkills().getXp(2)+","+player.getSkills().getLevel(3)+","+player.getSkills().getXp(3)+","+player.getSkills().getLevel(4)+","+player.getSkills().getXp(4)+","+player.getSkills().getLevel(5)+","+player.getSkills().getXp(5)+","+player.getSkills().getLevel(6)+","+player.getSkills().getXp(6)+","+player.getSkills().getLevel(7)+","+player.getSkills().getXp(7)+","+player.getSkills().getLevel(8)+","+player.getSkills().getXp(8)+","+player.getSkills().getLevel(9)+","+player.getSkills().getXp(9)+","+player.getSkills().getLevel(10)+","+player.getSkills().getXp(10)+","+player.getSkills().getLevel(11)+","+player.getSkills().getXp(11)+","+player.getSkills().getLevel(12)+","+player.getSkills().getXp(12)+","+player.getSkills().getLevel(13)+","+player.getSkills().getXp(13)+","+player.getSkills().getLevel(14)+","+player.getSkills().getXp(14)+","+player.getSkills().getLevel(15)+","+player.getSkills().getXp(15)+","+player.getSkills().getLevel(16)+","+player.getSkills().getXp(16)+","+player.getSkills().getLevel(17)+","+player.getSkills().getXp(17)+","+player.getSkills().getLevel(18)+","+player.getSkills().getXp(18)+","+player.getSkills().getLevel(19)+","+player.getSkills().getXp(19)+","+player.getSkills().getLevel(20)+","+player.getSkills().getXp(20)+","+player.getSkills().getLevel(21)+","+player.getSkills().getXp(21)+","+player.getSkills().getLevel(22)+","+player.getSkills().getXp(22)+","+player.getSkills().getLevel(23)+","+player.getSkills().getXp(23)+","+player.getSkills().getLevel(24)+","+player.getSkills().getXp(24)+");");
			query("INSERT INTO `skillsoverall` (`playerName`,`lvl`,`xp`) VALUES ('"+player.getUsername()+"',"+(player.getSkills().getLevel(0) + player.getSkills().getLevel(1) + player.getSkills().getLevel(2) + player.getSkills().getLevel(3) + player.getSkills().getLevel(4) + player.getSkills().getLevel(5) + player.getSkills().getLevel(24) + player.getSkills().getLevel(6) + player.getSkills().getLevel(7) + player.getSkills().getLevel(8) + player.getSkills().getLevel(9) + player.getSkills().getLevel(10) + player.getSkills().getLevel(11) + player.getSkills().getLevel(12) + player.getSkills().getLevel(13) + player.getSkills().getLevel(14) + player.getSkills().getLevel(15) + player.getSkills().getLevel(16) + player.getSkills().getLevel(17) + player.getSkills().getLevel(18) + player.getSkills().getLevel(19) + player.getSkills().getLevel(20) + player.getSkills().getLevel(21) + player.getSkills().getLevel(22) + player.getSkills().getLevel(23))+" ,"+((player.getSkills().getXp(0)) + (player.getSkills().getXp(1)) + (player.getSkills().getXp(2)) + (player.getSkills().getXp(3)) + (player.getSkills().getXp(4)) + (player.getSkills().getXp(5)) + (player.getSkills().getXp(6)) + (player.getSkills().getXp(7)) + (player.getSkills().getXp(8)) + (player.getSkills().getXp(9)) + (player.getSkills().getXp(10)) + (player.getSkills().getXp(11)) + (player.getSkills().getXp(12)) + (player.getSkills().getXp(13)) + (player.getSkills().getXp(14)) + (player.getSkills().getXp(15)) + (player.getSkills().getXp(16)) + (player.getSkills().getXp(17)) + (player.getSkills().getXp(18)) + (player.getSkills().getXp(19)) + (player.getSkills().getXp(20)) + (player.getSkills().getXp(21)) + (player.getSkills().getXp(22)) + (player.getSkills().getXp(23)) + (player.getSkills().getXp(24)))+");");
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
}