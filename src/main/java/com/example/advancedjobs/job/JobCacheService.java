package com.example.advancedjobs.job;

import com.example.advancedjobs.model.JobDefinition;
import com.example.advancedjobs.model.JobProgress;
import com.example.advancedjobs.model.PlayerJobProfile;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.UUID;

class JobCacheService {
    private String cachedCatalogPayload;
    private final Map<String, List<PlayerJobProfile>> cachedLeaderboards = new LinkedHashMap<>();
    private List<PlayerJobProfile> cachedOverallLeaderboard;
    private final Map<UUID, AggregateStats> cachedAggregateStats = new LinkedHashMap<>();

    boolean isCatalogPayloadCached() {
        return cachedCatalogPayload != null;
    }

    int catalogPayloadSize() {
        return cachedCatalogPayload == null ? 0 : cachedCatalogPayload.length();
    }

    int leaderboardCacheCount() {
        return cachedLeaderboards.size();
    }

    boolean isOverallLeaderboardCached() {
        return cachedOverallLeaderboard != null;
    }

    String createCatalogPayload(JobPayloadService payloadService, Collection<JobDefinition> jobs) {
        if (cachedCatalogPayload == null) {
            cachedCatalogPayload = payloadService.createCatalogPayload(jobs);
        }
        return cachedCatalogPayload;
    }

    Collection<PlayerJobProfile> leaderboard(String jobId, Collection<PlayerJobProfile> profiles) {
        return cachedLeaderboards.computeIfAbsent(jobId, key -> profiles.stream()
            .filter(profile -> profile.jobs().containsKey(key))
            .sorted(Comparator.<PlayerJobProfile>comparingInt(profile -> profile.progress(key).level()).reversed()
                .thenComparingDouble(profile -> profile.progress(key).earnedTotal()).reversed())
            .toList());
    }

    Collection<PlayerJobProfile> overallLeaderboard(Collection<PlayerJobProfile> profiles,
                                                    ToDoubleFunction<PlayerJobProfile> earnedTotal,
                                                    ToIntFunction<PlayerJobProfile> totalLevels) {
        if (cachedOverallLeaderboard == null) {
            cachedOverallLeaderboard = profiles.stream()
                .filter(profile -> !profile.jobs().isEmpty())
                .sorted(Comparator.<PlayerJobProfile>comparingDouble(earnedTotal).reversed()
                    .thenComparingInt(totalLevels).reversed())
                .toList();
        }
        return cachedOverallLeaderboard;
    }

    double totalEarnedAcrossJobs(PlayerJobProfile profile) {
        return aggregateStats(profile).earned();
    }

    int totalLevelsAcrossJobs(PlayerJobProfile profile) {
        return aggregateStats(profile).levels();
    }

    double totalXpAcrossJobs(PlayerJobProfile profile) {
        return aggregateStats(profile).xp();
    }

    void invalidateCatalogCache() {
        cachedCatalogPayload = null;
    }

    void invalidateLeaderboardCaches() {
        cachedLeaderboards.clear();
        cachedOverallLeaderboard = null;
        cachedAggregateStats.clear();
    }

    private AggregateStats aggregateStats(PlayerJobProfile profile) {
        return cachedAggregateStats.computeIfAbsent(profile.playerId(), ignored -> {
            int levels = 0;
            double xp = 0.0D;
            double earned = 0.0D;
            for (JobProgress progress : profile.jobs().values()) {
                levels += progress.level();
                xp += progress.xp();
                earned += progress.earnedTotal();
            }
            return new AggregateStats(levels, xp, earned);
        });
    }

    private record AggregateStats(int levels, double xp, double earned) {
    }
}
