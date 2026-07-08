package com.hexvane.cozytalefishing.leaderboard;

import com.hexvane.cozytalefishing.fish.FishCatchRecordComponent;
import com.hexvane.cozytalefishing.fish.FishScoreCalculator;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class FishingLeaderboardService {
    private static final AtomicBoolean loading = new AtomicBoolean(false);

    private static volatile LeaderboardSnapshot cachedSnapshot;

    private FishingLeaderboardService() {}

    public static void invalidate() {
        cachedSnapshot = null;
    }

    @Nullable
    public static LeaderboardSnapshot getCachedSnapshot() {
        return cachedSnapshot;
    }

    public static boolean isLoading() {
        return loading.get();
    }

    public static void requestRebuild(@Nonnull World world, @Nullable Runnable onComplete) {
        world.execute(
            () -> {
                if (!loading.compareAndSet(false, true)) {
                    return;
                }
                try {
                    cachedSnapshot = buildSnapshot();
                } catch (IOException | RuntimeException ignored) {
                    if (cachedSnapshot == null) {
                        cachedSnapshot = LeaderboardSnapshot.empty();
                    }
                } finally {
                    loading.set(false);
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
            }
        );
    }

    @Nonnull
    private static LeaderboardSnapshot buildSnapshot() throws IOException {
        Set<UUID> playerUuids = Universe.get().getPlayerStorage().getPlayers();
        List<LeaderboardSnapshot.RawLeaderboardEntry> raw = new ArrayList<>();
        for (UUID playerUuid : playerUuids) {
            LeaderboardSnapshot.RawLeaderboardEntry entry = loadRawEntry(playerUuid);
            if (entry != null && (entry.totalScore() > 0 || entry.bestCatchScore() > 0 || entry.totalCaught() > 0)) {
                raw.add(entry);
            }
        }
        return LeaderboardSnapshot.from(raw);
    }

    @Nullable
    private static LeaderboardSnapshot.RawLeaderboardEntry loadRawEntry(@Nonnull UUID playerUuid) {
        PlayerRef online = Universe.get().getPlayer(playerUuid);
        if (online != null) {
            Ref<EntityStore> ref = online.getReference();
            if (ref != null && ref.isValid()) {
                Store<EntityStore> store = ref.getStore();
                FishCatchRecordComponent records = store.getComponent(ref, FishCatchRecordComponent.getComponentType());
                return toRawEntry(playerUuid, records);
            }
        }

        Holder<EntityStore> holder = Universe.get().getPlayerStorage().load(playerUuid).join();
        return toRawEntry(playerUuid, holder);
    }

    @Nullable
    private static LeaderboardSnapshot.RawLeaderboardEntry toRawEntry(
        @Nonnull UUID playerUuid,
        @Nullable Holder<EntityStore> holder
    ) {
        if (holder == null) {
            return null;
        }
        FishCatchRecordComponent records = holder.getComponent(FishCatchRecordComponent.getComponentType());
        return toRawEntry(playerUuid, records);
    }

    @Nonnull
    private static LeaderboardSnapshot.RawLeaderboardEntry toRawEntry(
        @Nonnull UUID playerUuid,
        @Nullable FishCatchRecordComponent records
    ) {
        if (records == null) {
            return new LeaderboardSnapshot.RawLeaderboardEntry(playerUuid, "", 0, 0, 0);
        }
        return new LeaderboardSnapshot.RawLeaderboardEntry(
            playerUuid,
            records.getLeaderboardDisplayName(),
            FishScoreCalculator.totalScore(records),
            FishScoreCalculator.bestCatchScore(records),
            records.getTotalCatchCount()
        );
    }
}
