package com.example.advancedjobs.model;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PlayerJobProfile {
    private UUID playerId;
    private String playerName;
    private String activeJobId;
    private String secondaryJobId;
    private long lastJobChangeEpochSecond;
    private long lastSalaryClaimEpochSecond;
    private long lastContractRerollEpochSecond;
    private double internalBalance;
    private boolean dirty;
    private Map<String, JobProgress> jobs = new HashMap<>();
    private Set<String> unlockedTitles = new LinkedHashSet<>();

    public PlayerJobProfile() {
    }

    public PlayerJobProfile(UUID playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
    }

    public UUID playerId() { return playerId; }
    public String playerName() { return playerName; }
    public String activeJobId() { return activeJobId; }
    public String secondaryJobId() { return secondaryJobId; }
    public long lastJobChangeEpochSecond() { return lastJobChangeEpochSecond; }
    public long lastSalaryClaimEpochSecond() { return lastSalaryClaimEpochSecond; }
    public long lastContractRerollEpochSecond() { return lastContractRerollEpochSecond; }
    public double internalBalance() { return internalBalance; }
    public Map<String, JobProgress> jobs() { return jobs; }
    public Set<String> unlockedTitles() { return unlockedTitles; }
    public boolean dirty() { return dirty; }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
        this.dirty = true;
    }

    public void setActiveJobId(String activeJobId) {
        this.activeJobId = activeJobId;
        this.dirty = true;
    }

    public void setSecondaryJobId(String secondaryJobId) {
        this.secondaryJobId = secondaryJobId;
        this.dirty = true;
    }

    public void setLastJobChangeEpochSecond(long value) {
        this.lastJobChangeEpochSecond = value;
        this.dirty = true;
    }

    public void setInternalBalance(double internalBalance) {
        this.internalBalance = Math.max(0.0D, internalBalance);
        this.dirty = true;
    }

    public void setLastSalaryClaimEpochSecond(long value) {
        this.lastSalaryClaimEpochSecond = value;
        this.dirty = true;
    }

    public void setLastContractRerollEpochSecond(long value) {
        this.lastContractRerollEpochSecond = value;
        this.dirty = true;
    }

    public JobProgress progress(String jobId) {
        return jobs.computeIfAbsent(jobId, JobProgress::new);
    }

    public int availableSkillPoints(String jobId) {
        JobProgress progress = progress(jobId);
        return Math.max(0, progress.level() / 5 - progress.spentSkillPoints());
    }

    public void markDirty() {
        this.dirty = true;
    }

    public boolean unlockTitle(String titleId) {
        if (titleId == null || titleId.isBlank()) {
            return false;
        }
        boolean added = unlockedTitles.add(titleId);
        if (added) {
            this.dirty = true;
        }
        return added;
    }

    public void clearDirty() {
        this.dirty = false;
    }
}
