package com.example.advancedjobs.model;

public class ContractProgress {
    private String contractId;
    private String jobId;
    private String rarity;
    private int target;
    private int progress;
    private long expiresAtEpochSecond;
    private boolean completed;

    public ContractProgress() {
    }

    public ContractProgress(String contractId, String jobId, String rarity, int target, int progress, long expiresAtEpochSecond, boolean completed) {
        this.contractId = contractId;
        this.jobId = jobId;
        this.rarity = rarity;
        this.target = target;
        this.progress = progress;
        this.expiresAtEpochSecond = expiresAtEpochSecond;
        this.completed = completed;
    }

    public void addProgress(int amount) {
        if (!completed) {
            progress = Math.min(target, progress + amount);
            completed = progress >= target;
        }
    }

    public String contractId() { return contractId; }
    public String jobId() { return jobId; }
    public String rarity() { return rarity; }
    public int target() { return target; }
    public int progress() { return progress; }
    public long expiresAtEpochSecond() { return expiresAtEpochSecond; }
    public boolean completed() { return completed; }
}
