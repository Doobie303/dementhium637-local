package org.dementhium.content.items;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.dementhium.model.player.Equipment;

/** Server-side cache-definition bytes; model assets remain in the developer client. */
abstract class CustomItemCache {

    void prepare() {
    }

    abstract byte[] encode(ItemSpec item);

    static CustomItemCache models(int inventoryModel, int maleModel, int femaleModel,
            int zoom, int rotation1, int rotation2, int offset1, int offset2, boolean infernalTexture) {
        return new Models(inventoryModel, maleModel, femaleModel, zoom, rotation1, rotation2,
            offset1, offset2, infernalTexture);
    }

    private static final class Models extends CustomItemCache {
        private final int inventoryModel, maleModel, femaleModel, zoom, rotation1, rotation2;
        private final int offset1, offset2;
        private final boolean infernalTexture;

        private Models(int inventoryModel, int maleModel, int femaleModel, int zoom,
                int rotation1, int rotation2, int offset1, int offset2, boolean infernalTexture) {
            this.inventoryModel = inventoryModel;
            this.maleModel = maleModel;
            this.femaleModel = femaleModel;
            this.zoom = zoom;
            this.rotation1 = rotation1;
            this.rotation2 = rotation2;
            this.offset1 = offset1;
            this.offset2 = offset2;
            this.infernalTexture = infernalTexture;
        }

        byte[] encode(ItemSpec item) {
            try {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(bytes);
                out.writeByte(1); out.writeShort(inventoryModel);
                out.writeByte(2); out.writeBytes(item.name); out.writeByte(0);
                out.writeByte(4); out.writeShort(zoom);
                out.writeByte(5); out.writeShort(rotation1);
                out.writeByte(6); out.writeShort(rotation2);
                if (offset1 != 0) { out.writeByte(7); out.writeShort(offset1); }
                if (offset2 != 0) { out.writeByte(8); out.writeShort(offset2); }
                out.writeByte(12); out.writeInt(item.value);
                out.writeByte(16);
                out.writeByte(23); out.writeShort(maleModel);
                out.writeByte(25); out.writeShort(femaleModel);
                out.writeByte(36); out.writeBytes(item.slot == Equipment.SLOT_SHIELD ? "Wield" : "Wear"); out.writeByte(0);
                if (infernalTexture) {
                    out.writeByte(41); out.writeByte(1); out.writeShort(59); out.writeShort(915);
                }
                out.writeByte(0);
                return bytes.toByteArray();
            } catch (IOException exception) {
                throw new AssertionError(exception);
            }
        }
    }
}
