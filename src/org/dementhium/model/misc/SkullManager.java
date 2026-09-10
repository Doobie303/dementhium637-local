package org.dementhium.model.misc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


import org.dementhium.model.World;
import org.dementhium.model.player.Player;

/**
 * Handles skulling in the wilderness.
 * @author Emperor
 *
 */
public class SkullManager {

	/**
	 * The player.
	 */
	private final Player player;
	
	/**
	 * The amount of ticks left for the skull to disappear (if any).
	 */
	private int skullTicks = -1;
	
	/**
	 * A list of players this player has attacked first.
	 */
	private List<Player> victims = new ArrayList<Player>();
	
	/**
	 * A list of players who attacked this player first.
	 */
	private List<Player> attackers = new ArrayList<Player>();
	
	/**
	 * Constructs a new {@code SkullManager} {@code Object}.
	 * @param player The player.
	 */
	public SkullManager(Player player) {
		this.player = player;
	}
	
	/**
	 * Appends a skull on this player, if required.
	 * @param other The other player.
	 */
	public void appendSkull(Player other) { //Player gets skulled on Other
		//IF OTHER HAS ATTACKED PLAYER & OTHER IS SKULLED: RETURN.
		if (other.getSkullManager().isSkulled()) {
			for (Player othersVictim : other.getSkullManager().getVictims()) {
				if (othersVictim != null) {
					if (othersVictim.getUsername().equals(player.getUsername()))
						return;
				}
			}
			for (Player playersAttacker : attackers) {
				if (playersAttacker != null) {
					if (playersAttacker.getUsername().equals(other.getUsername()))
						return;
				}
			}
		}
		
		/*
		 * Note:
		 * Below I couldn't just do: 
		 * if (!victims.contains(other)) { 
		 * 		victims.add(other); 
		 * }
		 * The reason for this is that this wouldn't work if the player logged out and logged back in.
		 * Because if a player re-logs he will get a new player id value assigned (looks like org.dementhium.model.player.Player38@&u4)
		 * and this value, is the actual value which is stored in the ArrayList. So as I mentioned earlier; I couldn't just do:
		 * if (!victims.contains(other)) { 
		 * 		victims.add(other); 
		 * }
		 * as it would look for the wrong value when looking whether or not ArrayList 'victims' contained 'other'.
		 * 
		 * Therefore I used integer 'i' which found the position of the value of the player (which 
		 * looks like org.dementhium.model.player.Player38@&u4, in case you forgot).
		 * When integer 'i' had found the position, it would remove the value on that position. 
		 * 
		 * After that I re-added the player to the list.
		 * That way there would never be duplicate names in a list and each value would get the 
		 * right position in the ArreyList (based on which time they were (re-)added).
		 * 
		 * I also used CopyOnWriteArrayList to avoid the ConcurrentModificationException.
		 * 
		 * ~(The whole above technique was also used in the removeSkull() method)~
		 * 
		 * Lol, that whole story and I never really learned Java. Not bad?
		 * -Mod Nick / Sixpack
		 */
		
		//ADD VICTIM (OTHER) TO PLAYER's (PLAYER) VICTIM LIST:
		int i = 0;
		List<Player> victimList = new CopyOnWriteArrayList<Player>(victims);
		//for (Player victim : victims) {
		for (Player victim : victimList) {
			if (victim != null) {
				if (victim.getUsername().equals(other.getUsername())) {
					//victims.remove(i);
					victimList.remove(i);
				}
				i ++;
			}
		}
		//victims.add(other);
		victimList.add(other);
		victims = victimList;
	
		//ADD ATTACKER (PLAYER) TO OTHER's (VICTIM) ATTACKER LIST:
		i = 0;
		List<Player> attackerList = new CopyOnWriteArrayList<Player>(other.getSkullManager().getAttackers());
		for (Player attacker : attackerList) {
		//for (Player attacker : other.getSkullManager().getAttackers()) {
			if (attacker != null) {
				if (attacker.getUsername().equals(player.getUsername())) {
					//other.getSkullManager().getAttackers().remove(i);
					attackerList.remove(i);
				}
				i ++;
			}
		}
		//other.getSkullManager().getAttackers().add(player);
		attackerList.add(player);
		other.getSkullManager().attackers = attackerList;
		
		//SETS THE OTHER SKULL STUFF:
		player.setAttribute("skulled", true);
		skullTicks = World.getTicks() + 3333; //2000, (3333 (if each tick is 0.6 seconds) = 20 mins)
		player.getMask().setAppearanceUpdate(true);
	}
	
	/**
	 * Appends a skull on this player.
	 * Used for things like the Abyss.
	 */
	public void appendSkullWithoutCombat() {
		player.setAttribute("skulled", true);
		/*
		 * We don't want people who are skulled for 20 mins, to walk into the Abyss and recieve a 10 min skull.
		 */
		if ((skullTicks - World.getTicks()) < 1650)
			skullTicks = World.getTicks() + 1650; //(1650 (if each tick is 0.6 seconds) =+- 10 mins)		
		player.getMask().setAppearanceUpdate(true);
	}
	
	/**
	 * Removes a skull from this player.
	 * Used for death and commands.
	 */
    public void removeSkull() {
        for (Player other : new ArrayList<Player>(victims)) {
            if (other != null) other.getSkullManager().attackers.removeIf(p -> p == null || p.getUsername().equals(player.getUsername()));
        }
        for (Player other : new ArrayList<Player>(attackers)) {
            if (other != null) other.getSkullManager().victims.removeIf(p -> p == null || p.getUsername().equals(player.getUsername()));
        }
        victims.clear();
        attackers.clear();
        player.setAttribute("skulled",false);
        skullTicks=-1;
        player.getMask().setAppearanceUpdate(true);
    }
	/**
	 * Gets the amount of ticks left.
	 * The skullTicks will remain the same (if no new tick is set); it's the World.getTicks() 
	 * that goes up and tries to pass the amount of skullTicks.
	 * @return The amount of skullTicks.
	 */
	public int getTicks() {
		if (skullTicks != -1 && (skullTicks <= World.getTicks())) {
            removeSkull();
		}	
		return skullTicks;
	}
	
	/**
	 * Sets the amount of skull ticks left.
	 * @param ticks The ticks.
	 */
	public void setTicks(int ticks) {
		this.skullTicks = ticks;
		player.getMask().setAppearanceUpdate(true);
	}
	
	/**
	 * Checks if the player is skulled.
	 * @return {@code True} if so, {@code false} if not.
	 */
	public boolean isSkulled() {
		return getTicks() > World.getTicks();
	}

	/**
	 * @return the victims
	 */
	public List<Player> getVictims() {
		return victims;
	}

	/**
	 * @return the attackers
	 */
	public List<Player> getAttackers() {
		return attackers;
	}
}