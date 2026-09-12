package org.dementhium.content.items;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/** Retains the original resource's first-use initialization, failure and copy semantics. */
final class InfernalCapeCache extends CustomItemCache {

    void prepare() {
        Resource.initialize();
    }

    byte[] encode(ItemSpec item) {
        return Resource.BYTES.clone();
    }

    private static final class Resource {
        private static final byte[] BYTES = load();

        private static void initialize() {
        }

        private static byte[] load() {
            try {
                return Files.readAllBytes(Paths.get(CustomItems.INFERNAL_CAPE_DEFINITION));
            } catch (IOException exception) {
                throw new IllegalStateException("Missing Infernal Cape definition", exception);
            }
        }
    }
}
