package org.dementhium.model.misc;

import org.dementhium.model.Location;
import org.dementhium.model.Mob;
import org.dementhium.model.player.Player;
import org.dementhium.net.ActionSender;

/**
 * @author 'Mystic Flow <Steven@rune-server.org>
 */
public class IconManager {

    public static final int ARROW_ON_FEET = 40497;

    /**
     * @author 'Mystic Flow
     */
    public static class Icon {

        private int index = -1, slot, targetType, arrowId, modelId, distance = 5000;

        private Location l;

        public Icon(int slot, int index, int targetType, int arrowId, int modelId) {
            this.slot = slot;
            this.index = index;
            this.targetType = targetType;
            this.arrowId = arrowId;
            this.modelId = modelId;
        }

        public Icon(int slot, Location l, int targetType, int arrowId, int modelId) {
            this.slot = slot;
            this.l = l;
            this.targetType = targetType;
            this.arrowId = arrowId;
            this.modelId = modelId;
        }

        public int getSlot() {
            return slot;
        }

        public int getIndex() {
            return index;
        }

        public int getTargetType() {
            return targetType;
        }

        public int getArrowId() {
            return arrowId;
        }

        public int getModelId() {
            return modelId;
        }

        public Location getLocation() {
            return l;
        }

        /**
         * @return the distance
         */
        public int getDistance() {
            return distance;
        }

        /**
         * @param distance the distance to set
         */
        public void setDistance(int distance) {
            this.distance = distance;
        }
    }

    public static final int MAX_ICONS = 8;

    public static int iconOnMob(Player player, Mob mob, int arrowId, int modelId) {
        int slot = freeIconSlot(player, mob);
        if (slot > -1) {
            Icon icon = new Icon(slot, mob.getIndex(), mob.isPlayer() ? 10 : 1, arrowId, modelId);
            player.setAttribute("icon_slot" + slot, icon);
            ActionSender.sendHintIcon(player, icon);
        }
        return slot;
    }

    public static void removeIcon(Player player, Mob mob) {
        if (mob == null) {
            return;
        }
        int slot = findIconSlot(player, mob);
        if (slot == -1) {
            return;
        }
        Icon icon = player.getAttribute("icon_slot" + slot);
        if (icon != null) {
            icon.targetType = 0;
            icon.index = mob.getIndex();
            ActionSender.sendHintIcon(player, icon);
            player.removeAttribute("icon_slot" + slot);
        }
    }

    public static void removeIcon(Player player, Location l) {
        if (l == null) {
            return;
        }
        int slot = findIconSlot(player, l);
        if (slot == -1) {
            return;
        }
        Icon icon = player.getAttribute("icon_slot" + slot);
        if (icon != null) {
            icon.targetType = 0;
            ActionSender.sendHintIcon(player, icon);
            player.removeAttribute("icon_slot" + slot);
        }
    }

    private static int freeIconSlot(Player player, Object o) {
        if (!(o instanceof Mob) && !(o instanceof Location)) {
            return -1;
        }
        int existing = findIconSlot(player, o);
        if (existing != -1) {
            return existing;
        }
        for (int i = 0; i < MAX_ICONS; i++) {
            if (player.getAttribute("icon_slot" + i) == null) {
                return i;
            }
        }
        // Preserve replacement of slot zero when all icon slots are occupied.
        return 0;
    }

    private static int findIconSlot(Player player, Object o) {
        boolean isMob = o instanceof Mob;
        boolean isLocation = o instanceof Location;
        if (!isMob && !isLocation) {
            return -1;
        }
        Mob mob = isMob ? (Mob) o : null;
        Location l = isLocation ? (Location) o : null;
        for (int i = 0; i < MAX_ICONS; i++) {
            Icon icon = player.getAttribute("icon_slot" + i);
            if (icon != null && ((mob != null && icon.getIndex() == mob.getIndex()
                    && icon.getTargetType() == (mob.isPlayer() ? 10 : 1))
                    || (l != null && icon.getLocation() == l))) {
                return i;
            }
        }
        return -1;
    }

    public static int iconOnCoordinate(Player player, Location l, int arrowId, int modelId) {
        int slot = freeIconSlot(player, l);
        if (slot > -1) {
            Icon icon = new Icon(slot, l, 2, arrowId, modelId);
            player.setAttribute("icon_slot" + slot, icon);
            ActionSender.sendHintIcon(player, icon);
        }
        return slot;
    }

}
