package com.hexvane.cozytalefishing.leaderboard;

import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public record RankedLeaderboardEntry(
    @Nonnull UUID playerUuid,
    @Nonnull String displayName,
    int score,
    int rank,
    @Nullable String bestCatchSpeciesId,
    float bestCatchSizeCm
) {
    public RankedLeaderboardEntry(@Nonnull UUID playerUuid, @Nonnull String displayName, int score, int rank) {
        this(playerUuid, displayName, score, rank, null, 0.0f);
    }
}
