package com.example.advancedjobs.model;

public record RewardDefinition(double salary, double xp, double bonusLootChance, double buffChance) {
    public static final RewardDefinition EMPTY = new RewardDefinition(0.0D, 0.0D, 0.0D, 0.0D);

    public RewardDefinition scaled(double factor) {
        return new RewardDefinition(salary * factor, xp * factor, bonusLootChance, buffChance);
    }
}
