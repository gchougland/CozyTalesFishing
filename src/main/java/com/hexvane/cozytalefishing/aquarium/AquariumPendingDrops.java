package com.hexvane.cozytalefishing.aquarium;

import com.hexvane.cozytalefishing.fish.AquariumSize;
import com.hypixel.hytale.server.core.universe.world.World;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3i;

/**
 * Defers aquarium content drops so prefab reconstruct / force-paste can re-place the same stocked aquarium
 * without spawning duplicate ground items. {@link AquariumPlaceSystem} cancels a pending drop when a matching
 * aquarium is restored at the origin.
 */
final class AquariumPendingDrops {
    private static final ConcurrentHashMap<Long, Pending> PENDING = new ConcurrentHashMap<>();

    private AquariumPendingDrops() {}

    static void schedule(
        @Nonnull World world,
        @Nonnull Vector3i origin,
        @Nonnull AquariumSize aquariumSize,
        @Nullable String fishItemId,
        @Nonnull List<String> decorationItemIds
    ) {
        if ((fishItemId == null || fishItemId.isBlank()) && decorationItemIds.isEmpty()) {
            return;
        }
        long key = pack(origin);
        Pending pending =
            new Pending(
                world,
                new Vector3i(origin),
                aquariumSize,
                fishItemId,
                new ArrayList<>(decorationItemIds)
            );
        PENDING.put(key, pending);
        // Two nested executes: clear+reconstruct queues paste after the first break callback; the second
        // pass runs after that paste's world.execute work in typical reconstruct flows.
        world.execute(() -> world.execute(() -> flush(key, pending)));
    }

    static void cancelIfMatches(
        @Nonnull Vector3i origin,
        @Nullable String fishItemId,
        @Nonnull List<String> decorationItemIds
    ) {
        long key = pack(origin);
        Pending pending = PENDING.get(key);
        if (pending == null) {
            return;
        }
        if (pending.matches(fishItemId, decorationItemIds)) {
            PENDING.remove(key, pending);
        }
    }

    private static void flush(long key, @Nonnull Pending expected) {
        if (!PENDING.remove(key, expected)) {
            return;
        }
        AquariumService.dropStoredFish(
            expected.world,
            expected.origin,
            expected.aquariumSize,
            expected.fishItemId
        );
        AquariumService.dropStoredDecorations(
            expected.world,
            expected.origin,
            expected.aquariumSize,
            expected.decorationItemIds
        );
    }

    private static long pack(@Nonnull Vector3i origin) {
        return ((long) origin.x & 0x3FFFFFFL) << 38
            | ((long) origin.y & 0xFFFL) << 26
            | (long) origin.z & 0x3FFFFFFL;
    }

    private static final class Pending {
        private final World world;
        private final Vector3i origin;
        private final AquariumSize aquariumSize;
        @Nullable
        private final String fishItemId;
        private final List<String> decorationItemIds;

        private Pending(
            @Nonnull World world,
            @Nonnull Vector3i origin,
            @Nonnull AquariumSize aquariumSize,
            @Nullable String fishItemId,
            @Nonnull List<String> decorationItemIds
        ) {
            this.world = world;
            this.origin = origin;
            this.aquariumSize = aquariumSize;
            this.fishItemId = fishItemId;
            this.decorationItemIds = decorationItemIds;
        }

        private boolean matches(@Nullable String fishItemId, @Nonnull List<String> decorationItemIds) {
            boolean fishMatch =
                (this.fishItemId == null || this.fishItemId.isBlank())
                    ? (fishItemId == null || fishItemId.isBlank())
                    : this.fishItemId.equals(fishItemId);
            if (!fishMatch || this.decorationItemIds.size() != decorationItemIds.size()) {
                return false;
            }
            for (int i = 0; i < decorationItemIds.size(); i++) {
                if (!this.decorationItemIds.get(i).equals(decorationItemIds.get(i))) {
                    return false;
                }
            }
            return true;
        }
    }
}
