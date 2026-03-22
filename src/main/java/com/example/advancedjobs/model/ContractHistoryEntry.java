package com.example.advancedjobs.model;

public class ContractHistoryEntry {
    private String contractId;
    private String rarity;
    private long completedAtEpochSecond;
    private double salaryReward;
    private double xpReward;

    public ContractHistoryEntry() {
    }

    public ContractHistoryEntry(String contractId, String rarity, long completedAtEpochSecond, double salaryReward, double xpReward) {
        this.contractId = contractId;
        this.rarity = rarity;
        this.completedAtEpochSecond = completedAtEpochSecond;
        this.salaryReward = salaryReward;
        this.xpReward = xpReward;
    }

    public String contractId() { return contractId; }
    public String rarity() { return rarity; }
    public long completedAtEpochSecond() { return completedAtEpochSecond; }
    public double salaryReward() { return salaryReward; }
    public double xpReward() { return xpReward; }
}
