package org.dementhium.tickable.impl;

import org.dementhium.model.Location;
import org.dementhium.model.map.Region;
import org.dementhium.model.npc.impl.Nex.NexAreaEvent;
import org.dementhium.model.player.Player;
import org.dementhium.model.player.Skills;
import org.dementhium.tickable.Tick;

/**
 * @author 'Mystic Flow <Steven@rune-server.org>
 */
public class NexVirusTick extends Tick {

    private Player victim;
    private final org.dementhium.model.npc.impl.Nex owner;
    private final long ownerLife;
    private final org.dementhium.model.combat.NPCCombatContext context;

	private static final String COUGH = "*cough*";

	private int ticksPassed;
	private int prayerDrainTenths;
    
    private static final Location AREA_CENTER = NexAreaEvent.AREA_CENTER;

    public NexVirusTick(Player victim) {
   
        super(1);
        this.victim = victim;
        this.owner = NexAreaEvent.getNexAreaEvent().getNex();
        this.ownerLife = owner == null ? -1 : owner.getCombatGeneration();
        this.context = new org.dementhium.model.combat.NPCCombatContext(owner, victim);
        this.victim.forceText(COUGH);
    }

    /**
     * Being next to another infected player refreshes the virus, as it did in the
     * original encounter.  Keeping the refresh on the existing tick avoids
     * repeatedly replacing ticks while a group is stacked together.
     */
    private void refresh() {
        ticksPassed = 0;
    }

    @Override
    public void execute() {
        if (!isRunning()) {
            return;
        }
        if (++ticksPassed >= 60 || owner == null
                || !owner.isPhaseCurrent(ownerLife, org.dementhium.model.npc.impl.Nex.NexPhase.SMOKE)
                || !context.isCurrent()
                || NexAreaEvent.getNexAreaEvent().getNex() != owner
                || !victim.isOnline() || victim.isDead()
                || !NexAreaEvent.getNexAreaEvent().isInNexRoom(victim)) {
            victim.sendMessage("The smoke clouds around you dissipate.");
            stop();
            return;
        }
		// One game tick is 0.6 seconds: twelve tenths per tick averages the
		// original two prayer points drained per second.
		prayerDrainTenths += 12;
		if (prayerDrainTenths >= 10) {
			victim.getSkills().drainPray(prayerDrainTenths / 10);
			prayerDrainTenths %= 10;
		}
		if (ticksPassed % 8 == 0) {
			victim.forceText(COUGH);
		}
		if (ticksPassed % 5 == 0) {
			for (int i : Skills.COMBAT_SKILLS) {
				if (i != Skills.CONSTITUTION) {
					if (victim.getSkills().getLevel(i) > 2
							&& !Boolean.TRUE.equals(victim.getAttribute("overloads"))) {
						victim.getSkills().decreaseLevelOnce(i, 2);
					}
				}
			}
		}
		for (Player local : Region.getLocalPlayers(victim.getLocation(), 2)) {
            if (local == victim || !local.isOnline() || local.isDead()
                    || !local.hasReceivedStarter() || !NexAreaEvent.getNexAreaEvent().isInNexRoom(local)) {
                continue;
            }
            Tick virus = local.getTick("nex_virus");
            if (virus instanceof NexVirusTick) {
                ((NexVirusTick) virus).refresh();
            } else {
                local.submitTick("nex_virus", new NexVirusTick(local));
            }
        }
    }

}
