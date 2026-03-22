package com.example.advancedjobs.economy;

import com.example.advancedjobs.api.EconomyProvider;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InternalEconomyProvider implements EconomyProvider {
    private final Map<UUID, Double> balances = new HashMap<>();

    @Override
    public String id() {
        return "internal";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public double getBalance(UUID playerId) {
        return balances.getOrDefault(playerId, 0.0D);
    }

    @Override
    public boolean deposit(UUID playerId, double amount, String reason) {
        balances.merge(playerId, Math.max(0.0D, amount), Double::sum);
        return true;
    }

    @Override
    public boolean withdraw(UUID playerId, double amount, String reason) {
        double current = balances.getOrDefault(playerId, 0.0D);
        if (current < amount) {
            return false;
        }
        balances.put(playerId, current - amount);
        return true;
    }

    public void load(Map<UUID, Double> snapshot) {
        balances.clear();
        balances.putAll(snapshot);
    }

    public Map<UUID, Double> snapshot() {
        return new LinkedHashMap<>(balances);
    }
}
