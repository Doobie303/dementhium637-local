package org.dementhium.tickable.impl;

import org.dementhium.model.Location;
import org.dementhium.model.map.Region;
import org.dementhium.model.npc.impl.Nex.NexAreaEvent;
import org.dementhium.model.player.Player;
import org.dementhium.model.player.Skills;
import org.dementhium.tickable.Tick;
import org.dementhium.util.Misc;

/**
 * @author 'Mystic Flow <Steven@rune-server.org>
 */
public class NexVirusTick extends Tick {

    private Player victim;

    private long lastSpeak = System.currentTimeMillis();
    private long lastEffect = System.currentTimeMillis();

    private static final String COUGH = "*cough*";

    private int ticksPassed;
    
    private static final Location AREA_CENTER = NexAreaEvent.AREA_CENTER;

    public NexVirusTick(Player victim) {
   
        super(1);
        this.victim = victim;
        this.victim.forceText(COUGH);
    }

    @Override
    public void execute() {
        if (!isRunning()) {
            return;
        }
        if (++ticksPassed >= 60 || !(victim.getLocation().distance(AREA_CENTER) < 16)) {
            victim.sendMessage("The smoke clouds around you dissipate.");
            stop();
            return;
        }
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSpeak >= 10000 + Misc.random(5000)) {
            victim.forceText(COUGH);
            lastSpeak = currentTime;
        }
        if (currentTime - lastEffect >= 9000 + Misc.random(5000)) {
            for (int i : Skills.COMBAT_SKILLS) {
                if (i != Skills.CONSTITUTION) {
                    float modification = victim.getSkills().getLevel(i) * 0.20F;
                    if (modification < 1) {
                        modification = 1;
                    }
                    if (victim.getSkills().getLevel(i) - modification > 1 && victim.getAttribute("overloads") == Boolean.FALSE) {
                        victim.getSkills().decreaseLevelOnce(i, Math.round(modification));
                    }
                }
            }
            lastEffect = currentTime;
        }
        for (Player local : Region.getLocalPlayers(victim.getLocation(), 1)) {
            if (!local.hasTick("nex_virus") && local.getLocation().distance(AREA_CENTER) < 16) {
                local.submitTick("nex_virus", new NexVirusTick(local));
            }
        }
    }

}
