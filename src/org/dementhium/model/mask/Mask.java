package org.dementhium.model.mask;

import org.dementhium.model.Location;
import org.dementhium.model.Mob;
import org.dementhium.model.World;
import org.dementhium.model.definition.NPCDefinition;
import org.dementhium.model.player.Player;
import org.dementhium.tickable.Tick;

public class Mask {

    private Mob mob;

    private ChatMessage lastChatMessage;
    private Graphic lastGraphics;
    private Animation lastAnimation;
    private long npcAnimationStart, npcAnimationEnd, npcAnimationGeneration;
    private Heal lastHeal;
    private Location facePosition;

    private Mob interactingEntity;
    private int npcTurnToId = 0;

    private ForceText forceText;

    private boolean resetTurnTo = false;
    //These need to be flagged since they can be reset
    private boolean forceTextUpdate, appearanceUpdate, teleport, faceEntityUpdate, forceMovementUpdate;

    private int switchId = -1;

    public Mask(Mob mob) {
        this.mob = mob;
        this.setAppearanceUpdate(true);
    }

    public void reset() {
        switchId = -1;
        lastChatMessage = null;
        lastGraphics = null;
        lastAnimation = null;
        lastHeal = null;
        appearanceUpdate = false;
        faceEntityUpdate = false;
        forceText = null;
        forceMovementUpdate = false;
        forceTextUpdate = false;
        setTeleport(false);
        facePosition = null;
    }

    public Mob getPlayer() {
        return mob;
    }

    public boolean requiresUpdate() {
        if (mob.getDamageManager().getHits().size() > 0 || facePosition != null || forceMovementUpdate || forceTextUpdate || switchId > -1 || forceText != null || appearanceUpdate || faceEntityUpdate || lastChatMessage != null || lastGraphics != null || lastAnimation != null || lastHeal != null) {
            return true;
        }
        if (mob.isNPC()) {
            return mob.getNPC().testingmask;
        }
        if (mob.isPlayer()) {
            Player player = mob.getPlayer();
            return player.getWalkingQueue().getWalkDir() != -1 || player.getWalkingQueue().getRunDir() != -1 || player.getWalkingQueue().isDidTele();
        }
        return false;
    }

    public void setLastChatMessage(ChatMessage lastChatMessage) {
        this.lastChatMessage = lastChatMessage;
    }

    public ChatMessage getLastChatMessage() {
        return lastChatMessage;
    }

    public void setLastGraphics(Graphic lastGraphics) {
        this.lastGraphics = lastGraphics;
    }

    public Graphic getLastGraphics() {
        return lastGraphics;
    }

    public void setLastAnimation(Animation lastAnimation) {
        setLastAnimation(lastAnimation, false);
    }

    public void setLastAnimation(Animation lastAnimation, boolean ignoreFlag) {
        if (!mob.canAnimate() && !ignoreFlag) {
            return;
        }
        if (mob.isNPC() && lastAnimation != null) {
            long now = (long) World.getTicks() * 30;
            int id = lastAnimation.getId();
            // reset() clears an outgoing mask, not a sequence still playing on the client.
            // Only flinch/defence is suppressed; attacks, death and explicit resets win.
            if (!ignoreFlag && id >= 0 && id == mob.getDefenceAnimation()
                    && npcAnimationGeneration == mob.getNPC().getCombatGeneration()
                    && now >= npcAnimationStart && now < npcAnimationEnd) return;
            if (id < 0) npcAnimationEnd = 0;
            else if (id != mob.getDefenceAnimation()) {
                npcAnimationStart = now;
                npcAnimationEnd = now + lastAnimation.getDelay() + NPCAnimation.frames(id);
                npcAnimationGeneration = mob.getNPC().getCombatGeneration();
            }
        }
        this.lastAnimation = lastAnimation;
    }

    public Animation getLastAnimation() {
        return lastAnimation;
    }

    public void setLastHeal(Heal lastHeal) {
        this.lastHeal = lastHeal;
    }

    public Heal getLastHeal() {
        return lastHeal;
    }

    public void setLastForceText(ForceText text) {
        setForceText(text);
    }

    public boolean isForceTextUpdate() {
        return forceTextUpdate;
    }

    public void setForceText(ForceText forceText) {
        this.forceText = forceText;
        forceTextUpdate = true;
    }

    public void setForceTextUpdate(boolean forceText) {
        this.forceTextUpdate = forceText;
    }

    public ForceText getForceText() {
        return forceText;
    }

    public void setTeleport(boolean teleport) {
        this.teleport = teleport;
    }

    public boolean isTeleport() {
        return teleport;
    }

    public void setAppearanceUpdate(boolean appearanceUpdate) {
        this.appearanceUpdate = appearanceUpdate;
    }

    public boolean isAppearanceUpdate() {
        return appearanceUpdate;
    }

    /*
     * Used in npc turning to player in dialogues (after a while the npc should stop turning to a player).
     */
    public boolean resetTurnToNeeded() {
        return resetTurnTo;
    }
    
    public void resetTurnToNeeded(boolean needed) {
    	this.resetTurnTo = needed;
    }
    
    
    //npc.setInteractingEntity(player);
    public void setInteractingEntity(final Mob interactingEntity, boolean temporarilyInteraction) {
        this.interactingEntity = interactingEntity;
        this.faceEntityUpdate = true;
        if (temporarilyInteraction == false)
        	return;
        if (interactingEntity == null)
        	return;
        if (interactingEntity.isPlayer()) {
        	if (interactingEntity.getPlayer() == null) {
        		return;
        	}
        	npcTurnToId++;
        	if (npcTurnToId < 0) //If the npc has been clicked on over 2.147b times, it won't bug (only needed if you leave your server on reeeeaaaalllyyy long =D (not))
        		npcTurnToId = 0;
        	final int checker = npcTurnToId;
        	this.resetTurnTo = false;
            World.getWorld().submit(new Tick(15) {
                @Override
                public void execute() {
                	stop();
                	if (interactingEntity.getPlayer() != null && mob.isNPC() && mob.getNPC().getSpeakingTo() == null) {
                    	if (npcTurnToId == checker)
                    		resetTurnTo = true;
                	}
                }
            });
        }
    }
    
    //LOL:
    /*public void setInteractingEntity(final Mob interactingEntity) {
    this.interactingEntity = interactingEntity;
    this.faceEntityUpdate = true;
    try {
        if (interactingEntity.isNPC()) {
            final int stage = interactingEntity.getNPC().getDialogueStage();
            try {
            	this.resetTurnTo = false;
                World.getWorld().submit(new Tick(6) {
                    @Override
                    public void execute() {
                    	try {
                    		if (stage == interactingEntity.getNPC().getDialogueStage()) //If stage stayed the same
                                resetTurnTo = true;
                    	} catch (NullPointerException e) {
                    		resetTurnTo = true;
                        }
                        stop();
                    }
                });
            } catch (NullPointerException e) {
                World.getWorld().submit(new Tick(6) {
                    @Override
                    public void execute() {
                    	resetTurnTo = true;
                        stop();
                    }
                });
            }
        }
    } catch (NullPointerException e) {
        World.getWorld().submit(new Tick(6) {
            @Override
            public void execute() {
            	resetTurnTo = true;
                stop();
            }
        });
    }
}*/

    public Mob getInteractingEntity() {
        return interactingEntity;
    }

    public boolean isFaceEntityUpdate() {
        return faceEntityUpdate;
    }

    public void setFacePosition(Location facePosition, int sizeX, int sizeY) {
        if (sizeX <= 1 && sizeY <= 1) {
            this.facePosition = facePosition;
        } else {
            int faceX = facePosition.getX() + Math.round(sizeX / 2);
            int faceY = facePosition.getY() + Math.round(sizeY / 2);
            //			int objectCalcX = (-48 + 8 * facePosition.getRegionX());
            //            int objectCalcY = (-48 + 8 * facePosition.getRegionY());
            //            int offsetY = ((faceY + sizeY) - (objectCalcY + objectCalcY) / 64) + ((objectCalcY + objectCalcY) / 64);
            //            int offsetX = ((faceX + sizeX) - (objectCalcX + objectCalcX) / 64) + ((objectCalcX + objectCalcX) / 64);
            //            if (sizeX < sizeY)
            //                offsetX += 1;
            //            else if (sizeX > sizeY)
            //                offsetX -= 1;
            //            faceY = offsetY;
            //            faceX = offsetX;
            this.facePosition = Location.locate(faceX, faceY, 0);
        }
    }

    public Location getFacePosition() {
        return facePosition;
    }

    public void setSwitchId(int switchId) {
        this.switchId = switchId;
        mob.getNPC().setId(switchId);
        mob.getNPC().setDefinition(NPCDefinition.forId(switchId));
    }

    public int getSwitchId() {
        return switchId;
    }

    public void setForceMovementUpdate(boolean forceMovementUpdate) {
        this.forceMovementUpdate = forceMovementUpdate;
    }

    public boolean isForceMovementUpdate() {
        return forceMovementUpdate;
    }

}
