package com.example.advancedjobs.api;

import java.util.UUID;

public interface EconomyProvider {
    String id();

    boolean isAvailable();

    double getBalance(UUID playerId);

    boolean deposit(UUID playerId, double amount, String reason);

    boolean withdraw(UUID playerId, double amount, String reason);
}
