package com.hexvane.cozytalefishing.leaderboard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.ToIntFunction;
import javax.annotation.Nonnull;

public final class LeaderboardSnapshot {
    @Nonnull
    private final List<RankedLeaderboardEntry> totalScoreEntries;
    @Nonnull
    private final List<RankedLeaderboardEntry> bestCatchEntries;
    @Nonnull
    private final List<RankedLeaderboardEntry> totalCaughtEntries;
    private final long builtAtEpochMs;

    private LeaderboardSnapshot(
        @Nonnull List<RankedLeaderboardEntry> totalScoreEntries,
        @Nonnull List<RankedLeaderboardEntry> bestCatchEntries,
        @Nonnull List<RankedLeaderboardEntry> totalCaughtEntries,
        long builtAtEpochMs
    ) {
        this.totalScoreEntries = List.copyOf(totalScoreEntries);
        this.bestCatchEntries = List.copyOf(bestCatchEntries);
        this.totalCaughtEntries = List.copyOf(totalCaughtEntries);
        this.builtAtEpochMs = builtAtEpochMs;
    }

    @Nonnull
    public static LeaderboardSnapshot empty() {
        return new LeaderboardSnapshot(List.of(), List.of(), List.of(), System.currentTimeMillis());
    }

    @Nonnull
    public static LeaderboardSnapshot from(@Nonnull List<RawLeaderboardEntry> raw) {
        return new LeaderboardSnapshot(
            rankBy(raw, RawLeaderboardEntry::totalScore),
            rankBy(raw, RawLeaderboardEntry::bestCatchScore),
            rankBy(raw, RawLeaderboardEntry::totalCaught),
            System.currentTimeMillis()
        );
    }

    @Nonnull
    public List<RankedLeaderboardEntry> getTotalScoreEntries() {
        return totalScoreEntries;
    }

    @Nonnull
    public List<RankedLeaderboardEntry> getBestCatchEntries() {
        return bestCatchEntries;
    }

    @Nonnull
    public List<RankedLeaderboardEntry> getTotalCaughtEntries() {
        return totalCaughtEntries;
    }

    public long getBuiltAtEpochMs() {
        return builtAtEpochMs;
    }

    @Nonnull
    private static List<RankedLeaderboardEntry> rankBy(
        @Nonnull List<RawLeaderboardEntry> raw,
        @Nonnull ToIntFunction<RawLeaderboardEntry> scoreFn
    ) {
        List<RawLeaderboardEntry> sorted = raw.stream()
            .filter(entry -> scoreFn.applyAsInt(entry) > 0)
            .sorted(Comparator.comparingInt(scoreFn).reversed())
            .toList();

        List<RankedLeaderboardEntry> ranked = new ArrayList<>();
        int rank = 0;
        int position = 0;
        int lastScore = -1;
        for (RawLeaderboardEntry entry : sorted) {
            position++;
            int score = scoreFn.applyAsInt(entry);
            if (score != lastScore) {
                rank = position;
                lastScore = score;
            }
            ranked.add(new RankedLeaderboardEntry(entry.playerUuid(), entry.displayName(), score, rank));
        }
        return ranked;
    }

    public record RawLeaderboardEntry(
        @Nonnull UUID playerUuid,
        @Nonnull String displayName,
        int totalScore,
        int bestCatchScore,
        int totalCaught
    ) {}
}
