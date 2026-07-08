package com.hexvane.cozytalefishing.leaderboard;

import java.util.UUID;
import javax.annotation.Nonnull;

public record RankedLeaderboardEntry(@Nonnull UUID playerUuid, @Nonnull String displayName, int score, int rank) {}
