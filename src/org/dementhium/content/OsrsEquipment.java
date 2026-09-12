package org.dementhium.content;

import org.dementhium.content.items.CustomItems;

/** Compatibility entry point used by the developer client's CapeProof. */
public final class OsrsEquipment {

    private OsrsEquipment() {
    }

    public static byte[] cacheDefinition(int id) {
        if (!CustomItems.osrsEquipmentIds().contains(id)) {
            throw new IllegalArgumentException("Not a custom OSRS equipment item: " + id);
        }
        return CustomItems.cacheDefinition(id);
    }
}
