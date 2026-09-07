package org.dementhium.util.handlers;

import java.io.IOException;
import java.util.ArrayList;

import org.dementhium.io.XMLHandler;
import org.dementhium.model.player.Player;

/**
 * 
 * NOTE: 
 * THIS FILE ISN'T USED YET..
 *
 */

public class RightsHandler {

	private ArrayList<String> superAdministrators = new ArrayList<String>();
	private ArrayList<String> administrators = new ArrayList<String>();
	private ArrayList<String> playerModerators = new ArrayList<String>();
	
    public void makeAdministrator(Player player, boolean superAdmin) {
        if (administrators == null) {
        	administrators = new ArrayList<String>();
        }
        if (superAdministrators == null) {
        	superAdministrators = new ArrayList<String>();
        }
        if (superAdmin) {
        	superAdministrators.add(player.getFormattedUsername());
    		administrators.remove(player.getFormattedUsername());
    		playerModerators.remove(player.getFormattedUsername());
        } else {
        	administrators.add(player.getFormattedUsername());
    		superAdministrators.remove(player.getFormattedUsername());
    		playerModerators.remove(player.getFormattedUsername());
        }
    }
    
    public void makePlayerModerator(Player player) {
        if (playerModerators == null) {
        	playerModerators = new ArrayList<String>();
        }
    	playerModerators.add(player.getFormattedUsername());
		superAdministrators.remove(player.getFormattedUsername());
		administrators.remove(player.getFormattedUsername());
    }
    
    public void unMod(String user) {
		superAdministrators.remove(user);
		administrators.remove(user);
		playerModerators.remove(user);
    }

    //public static final String DIRECTORY = Misc.isVPS() ? "data/xml/" : "root/xml/";
    public static final String DIRECTORY = "data/xml/";
    
    public void save() {
        try {
        	XMLHandler.toXML(DIRECTORY + "superAdministrators.xml", superAdministrators);
            XMLHandler.toXML(DIRECTORY + "administrators.xml", administrators);
            XMLHandler.toXML(DIRECTORY + "playerModerators.xml", playerModerators);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void load() {
        try {
        	superAdministrators = XMLHandler.fromXML(DIRECTORY + "superAdministrators.xml");
        	administrators = XMLHandler.fromXML(DIRECTORY + "administrators.xml");
            playerModerators = XMLHandler.fromXML(DIRECTORY + "playerModerators.xml");
        } catch (IOException e) {
        }
    }
	
}
