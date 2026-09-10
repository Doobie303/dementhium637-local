package org.dementhium.event.impl.object;

import org.dementhium.event.EventListener;
import org.dementhium.event.EventManager;
import org.dementhium.model.Location;
import org.dementhium.model.map.GameObject;
import org.dementhium.model.npc.impl.Nex;
import org.dementhium.model.player.Player;

/** Lets another player shatter an icicle and release Nex's prison target. */
public class NexIcePrisonListener extends EventListener {

	@Override
	public void register(EventManager manager) {
		manager.registerObjectListener(57263, this);
	}

	@Override
	public boolean objectOption(Player player, int objectId, GameObject gameObject,
			Location location, ClickOption option) {
		return Nex.NexAreaEvent.getNexAreaEvent().breakIcePrison(player, location);
	}
}
