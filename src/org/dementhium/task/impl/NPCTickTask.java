package org.dementhium.task.impl;
import org.dementhium.content.minigames.FightCaves;

import org.dementhium.model.World;
import org.dementhium.model.map.Region;
import org.dementhium.model.map.path.PathState;
import org.dementhium.model.map.path.PrimitivePathFinder;
import org.dementhium.model.misc.GodwarsUtils.Faction;
import org.dementhium.model.npc.NPC;
import org.dementhium.model.player.Player;
import org.dementhium.task.Task;

/**
 * @author 'Mystic Flow <Steven@rune-server.org>
 */
public class NPCTickTask implements Task {

    private final NPC npc;

    public NPCTickTask(NPC npc) {
        this.npc = npc;
    }

    @Override
    public void execute() {
        if (!instanceAllowsTick()) return;
        org.dementhium.model.npc.encounter.EncounterNPC encounter=npc instanceof org.dementhium.model.npc.encounter.EncounterNPC?(org.dementhium.model.npc.encounter.EncounterNPC)npc:null;
        if(encounter!=null)encounter.beginCombatTick();
        try{executeCycle();}finally{if(encounter!=null)encounter.endCombatTick();}
    }

    private void executeCycle() {
    	if (npc.getMask().resetTurnToNeeded()) {
    		npc.resetTurnTo();
    		npc.getMask().resetTurnToNeeded(false);
    	}
        if (npc.getCombatExecutor().getVictim() == null) {
            Player caveTarget = FightCaves.getCombatTarget(npc);
            if (caveTarget != null) {
                npc.getCombatExecutor().setVictim(caveTarget);
            }
        }
        // CombatExecutor validates lifetime/admission. A first approach has no last attacker yet.
        org.dementhium.model.Mob target=npc.getCombatExecutor().getVictim();
        if(target!=null&&(!target.isAttackable(npc)||(target.isPlayer()&&target.getPlayer().isInvisible())))npc.getCombatExecutor().reset();
        npc.getCombatExecutor().tick();
        if (!instanceAllowsTick()) return;
        npc.processTicks();
        if (!instanceAllowsTick()) return;
        npc.tick();
        if (!instanceAllowsTick()) return;
        npc.getWalkingQueue().getNextEntityMovement();
        if (npc instanceof org.dementhium.model.npc.godwars.GodWarsNPC || npc instanceof org.dementhium.model.npc.encounter.EncounterNPC || npc instanceof org.dementhium.model.npc.impl.Nex) return; // Room owns aggression and chase bounds.
        if (Boolean.TRUE.equals(npc.getAttribute("fightcaves")) && FightCaves.isBlockingNpc(npc.getId())) {
            // Cave ownership determines aggression, independent of local-player search range.
            return;
        }
        if (npc.isDead() || npc.isHidden() || npc.isReturningHome() || npc.getCombatExecutor().getVictim() != null || !npc.getDefinition().isAggressive()) {
        	return;
        }
        int currentDistance = 20;
        Player toAttack = null;
        int depth = npc.getAttribute("activity") == "FightCavesActivity" ? 50 : 5;
        for (Player player : Region.getLocalPlayers(npc.getLocation(), 5)) {
            if(!org.dementhium.model.combat.NPCCombatContext.validPair(npc,player))continue;
        	try {
            	PathState path = World.getWorld().doPath(new PrimitivePathFinder(), npc, player.getLocation().getX(), player.getLocation().getY(), false, false, true);
            	if ((path == null || !path.isRouteFound() || !player.isAttackable(npc) || player.isInvisible())
            			&& npc.getAttribute("activity") != "FightCavesActivity") {
            		continue;
            	}
            	int combatLevel = player.getSkills().getCombatLevel();
            	if (combatLevel > (npc.getDefinition().getCombatLevel() * 2) + 2
            			&& (npc.getId() < 2025 || npc.getId() > 2030)
            			&& npc.getAttribute("activity") != "FightCavesActivity") {
            		continue;
            	}
            	int distance = npc.getLocation().getDistance(player.getLocation());
            	if (distance < currentDistance) {
            		currentDistance = distance;
            		toAttack = player;
            	}
        	} catch (NullPointerException e) {
        		continue;
        	}
        }
        npc.getCombatExecutor().setVictim(toAttack);
    }

    private boolean instanceAllowsTick() {
        org.dementhium.model.instance.GameInstance instance = npc.getOwningInstance();
        return instance == null || (instance.isActive() && instance.owns(npc));
    }

    private boolean ignoreGodItems(int id) {
        switch (id) {
            case 6222:
            case 6223:
            case 6225:
            case 6227:
            case 6203:
            case 6204:
            case 6206:
            case 6208:
            case 6247:
            case 6248:
            case 6250:
            case 6252:
            case 6260:
            case 6261:
            case 6263:
            case 6265:
                return true;
        }
        return false;
    }

    private boolean canAttackFaction(Faction faction, Faction faction2) {
        if (faction == null || faction2 == null) {
            return false;
        } else if (faction.equals(Faction.ARMADYL) && faction2.equals(Faction.SARADOMIN)) {
            return false;
        } else if (faction.equals(Faction.SARADOMIN) && faction2.equals(Faction.ARMADYL)) {
            return false;
        } else {
            return !faction.equals(faction2);
        }
    }

}
