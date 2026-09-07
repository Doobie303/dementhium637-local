package org.dementhium.util.handlers;

import java.util.HashMap;
import java.util.Map;

import org.dementhium.content.clans.Clan;
import org.dementhium.content.skills.summoning.SummoningPouch;
import org.dementhium.io.XMLHandler;
import org.dementhium.model.player.Player;
import org.dementhium.util.Misc;


public class DisplayNamesHandler {

	private static Map<String, String> displayNames;
	//				 DisplayName, userName

	public DisplayNamesHandler() {
		try {
			displayNames = XMLHandler.fromXML(DIRECTORY+"DisplayNames.xml");
		} catch (Throwable e) {
			e.printStackTrace();
		}
		if (displayNames == null) {
			displayNames = new HashMap<String, String>();
		}
	}
	
    //public static final String DIRECTORY = Misc.isVPS() ? "data/xml/" : "root/xml/";
    public static final String DIRECTORY = "data/xml/";

	public static void setDisplayName(Player player, String displayName, String oldDisplayName) {
		String user = player.getFormattedUsername();
		if (displayNames.containsKey(oldDisplayName))
			displayNames.remove(oldDisplayName);
		if (displayName != null)
			displayNames.put(displayName, user);
	}
    
	public static String getUsernameFromDisplayName(String displayName) {
		return displayNames.get(displayName);
	}
	
	public static String getDisplayNameFromUsername(String username) {
		for (String displayName : displayNames.keySet()) {
			if (displayName != null && displayNames.get(displayName) != null 
					&& displayNames.get(displayName).equals(Misc.formatPlayerNameForDisplay(username)))
				return displayName;
		}
		return null;
	}

	public static Map<String, String> getDisplayNames() {
		return displayNames;
	}
	
}
