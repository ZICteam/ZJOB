package io.zicteam.zeconomy.api.model;

import java.util.Map;
import java.util.UUID;

public record PlayerEconomySnapshot(
    UUID playerId,
    String playerName,
    Map<String, Double> walletBalances,
    Map<String, Double> bankBalances,
    Map<String, Double> vaultBalances,
    int pendingMail,
    int dailyStreak,
    boolean claimedDailyToday,
    boolean hasVaultPin
) {
}
