/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dementhium.mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.dementhium.model.World;

/**
 *
 * @author James <james@lacunapk.info>
 */
public class PlayersOnline {

    public static Connection CONNECTION = null;
    public static Statement STATEMENT;
    private static String HOST = "174.120.194.2";
    private static String DATABASE = "dynamicp_online";
    private static String USERNAME = "dynamicp_online";
    private static String PASSWORD = "poop12345";

    public static void createConnection() {
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            CONNECTION = DriverManager.getConnection("jdbc:mysql://" + HOST + "/" + DATABASE, USERNAME, PASSWORD);
            STATEMENT = CONNECTION.createStatement();
            System.out.println("Updated players online.");
        } catch (Exception e) {
            System.out.println("Connection to SQL database failed");
            e.printStackTrace();
        }
    }

    public static void destroyConnection() {
        try {
            STATEMENT.close();
            CONNECTION.close();
        } catch (Exception e) {
        }
    }

    public static void updatePlayers() {
        try {
            String query = "DELETE FROM online";
            String query2 = "INSERT INTO online (total) VALUES ('" + getPlayersOnline() + "')";
            STATEMENT.executeUpdate(query);
            STATEMENT.executeUpdate(query2);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static int getPlayersOnline() {
        return World.getWorld().getPlayers().size();
    }
}
