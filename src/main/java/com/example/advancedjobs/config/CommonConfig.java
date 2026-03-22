package com.example.advancedjobs.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class CommonConfig {
    public final ForgeConfigSpec.BooleanValue allowSecondaryJob;
    public final ForgeConfigSpec.DoubleValue jobChangePrice;
    public final ForgeConfigSpec.IntValue jobChangeCooldownSeconds;
    public final ForgeConfigSpec.BooleanValue resetProgressOnChange;
    public final ForgeConfigSpec.BooleanValue storeAllJobProgress;
    public final ForgeConfigSpec.BooleanValue instantSalary;
    public final ForgeConfigSpec.IntValue salaryClaimIntervalSeconds;
    public final ForgeConfigSpec.DoubleValue maxSalaryPerClaim;
    public final ForgeConfigSpec.DoubleValue salaryTaxRate;
    public final ForgeConfigSpec.DoubleValue contractRerollPrice;
    public final ForgeConfigSpec.IntValue contractRerollCooldownSeconds;
    public final ForgeConfigSpec.IntValue maxJobLevel;
    public final ForgeConfigSpec.DoubleValue baseXp;
    public final ForgeConfigSpec.DoubleValue growthFactor;
    public final ForgeConfigSpec.BooleanValue debugLogging;
    public final ForgeConfigSpec.BooleanValue blockArtificialMobRewards;
    public final ForgeConfigSpec.BooleanValue blockBabyMobRewards;
    public final ForgeConfigSpec.BooleanValue blockTamedMobRewards;
    public final ForgeConfigSpec.IntValue repeatedKillDecayThreshold;
    public final ForgeConfigSpec.IntValue lootContainerRewardCooldownSeconds;
    public final ForgeConfigSpec.IntValue exploredChunkRewardCooldownSeconds;
    public final ForgeConfigSpec.ConfigValue<String> storageMode;
    public final ForgeConfigSpec.ConfigValue<String> resetTime;

    public CommonConfig(ForgeConfigSpec.Builder builder) {
        builder.push("jobs");
        allowSecondaryJob = builder.define("allowSecondaryJob", false);
        jobChangePrice = builder.defineInRange("jobChangePrice", 250.0D, 0.0D, 10_000_000.0D);
        jobChangeCooldownSeconds = builder.defineInRange("jobChangeCooldownSeconds", 3600, 0, Integer.MAX_VALUE);
        resetProgressOnChange = builder.define("resetProgressOnChange", false);
        storeAllJobProgress = builder.define("storeAllJobProgress", true);
        instantSalary = builder.define("instantSalary", false);
        salaryClaimIntervalSeconds = builder.defineInRange("salaryClaimIntervalSeconds", 30, 0, Integer.MAX_VALUE);
        maxSalaryPerClaim = builder.defineInRange("maxSalaryPerClaim", 50_000.0D, 1.0D, 100_000_000.0D);
        salaryTaxRate = builder.defineInRange("salaryTaxRate", 0.05D, 0.0D, 1.0D);
        contractRerollPrice = builder.defineInRange("contractRerollPrice", 250.0D, 0.0D, 10_000_000.0D);
        contractRerollCooldownSeconds = builder.defineInRange("contractRerollCooldownSeconds", 600, 0, Integer.MAX_VALUE);
        maxJobLevel = builder.defineInRange("maxJobLevel", 100, 1, 1000);
        baseXp = builder.defineInRange("baseXp", 100.0D, 1.0D, 1_000_000.0D);
        growthFactor = builder.defineInRange("growthFactor", 1.5D, 1.0D, 10.0D);
        debugLogging = builder.define("debugLogging", false);
        blockArtificialMobRewards = builder.define("blockArtificialMobRewards", true);
        blockBabyMobRewards = builder.define("blockBabyMobRewards", true);
        blockTamedMobRewards = builder.define("blockTamedMobRewards", true);
        repeatedKillDecayThreshold = builder.defineInRange("repeatedKillDecayThreshold", 48, 1, 10_000);
        lootContainerRewardCooldownSeconds = builder.defineInRange("lootContainerRewardCooldownSeconds", 21600, 0, Integer.MAX_VALUE);
        exploredChunkRewardCooldownSeconds = builder.defineInRange("exploredChunkRewardCooldownSeconds", 43200, 0, Integer.MAX_VALUE);
        storageMode = builder.define("storageMode", "json");
        resetTime = builder.define("dailyResetTime", "04:00");
        builder.pop();
    }
}
