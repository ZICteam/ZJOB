package com.example.advancedjobs.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JobProgress {
    private String jobId;
    private int level;
    private double xp;
    private double pendingSalary;
    private double earnedTotal;
    private int spentSkillPoints;
    private Map<String, Integer> actionStats = new HashMap<>();
    private List<PerkNodeState> unlockedNodes = new ArrayList<>();
    private List<DailyTaskProgress> dailyTasks = new ArrayList<>();
    private List<ContractProgress> contracts = new ArrayList<>();
    private List<DailyTaskHistoryEntry> dailyHistory = new ArrayList<>();
    private List<ContractHistoryEntry> contractHistory = new ArrayList<>();
    private List<String> unlockedMilestones = new ArrayList<>();
    private static final int HISTORY_LIMIT = 24;

    public JobProgress() {
    }

    public JobProgress(String jobId) {
        this.jobId = jobId;
        this.level = 1;
    }

    public String jobId() { return jobId; }
    public int level() { return level; }
    public void setLevel(int level) { this.level = Math.max(1, level); }
    public double xp() { return xp; }
    public void addXp(double amount) { this.xp += amount; }
    public double pendingSalary() { return pendingSalary; }
    public double earnedTotal() { return earnedTotal; }
    public int spentSkillPoints() { return spentSkillPoints; }
    public void spendSkillPoints(int amount) { spentSkillPoints += Math.max(0, amount); }
    public void refundSkillPoints(int amount) { spentSkillPoints = Math.max(0, spentSkillPoints - Math.max(0, amount)); }
    public void setSpentSkillPoints(int amount) { spentSkillPoints = Math.max(0, amount); }
    public Map<String, Integer> actionStats() { return actionStats; }
    public List<PerkNodeState> unlockedNodes() { return unlockedNodes; }
    public List<DailyTaskProgress> dailyTasks() { return dailyTasks; }
    public List<ContractProgress> contracts() { return contracts; }
    public List<DailyTaskHistoryEntry> dailyHistory() { return dailyHistory; }
    public List<ContractHistoryEntry> contractHistory() { return contractHistory; }
    public List<String> unlockedMilestones() { return unlockedMilestones; }

    public void addPendingSalary(double amount) {
        double safe = Math.max(0.0D, amount);
        pendingSalary += safe;
        earnedTotal += safe;
    }

    public void addEarnedSalary(double amount) {
        earnedTotal += Math.max(0.0D, amount);
    }

    public double claimPendingSalary() {
        double value = pendingSalary;
        pendingSalary = 0.0D;
        return value;
    }

    public double takePendingSalary(double amount) {
        double value = Math.max(0.0D, Math.min(amount, pendingSalary));
        pendingSalary -= value;
        return value;
    }

    public void restorePendingSalary(double amount) {
        pendingSalary += Math.max(0.0D, amount);
    }

    public void resetLeveling() {
        level = 1;
        xp = 0.0D;
        pendingSalary = 0.0D;
        earnedTotal = 0.0D;
        spentSkillPoints = 0;
    }

    public void recordDailyCompletion(String taskId, long completedAtEpochSecond, double salaryReward, double xpReward) {
        dailyHistory.add(0, new DailyTaskHistoryEntry(taskId, completedAtEpochSecond, salaryReward, xpReward));
        if (dailyHistory.size() > HISTORY_LIMIT) {
            dailyHistory = new ArrayList<>(dailyHistory.subList(0, HISTORY_LIMIT));
        }
    }

    public void recordContractCompletion(String contractId, String rarity, long completedAtEpochSecond, double salaryReward, double xpReward) {
        contractHistory.add(0, new ContractHistoryEntry(contractId, rarity, completedAtEpochSecond, salaryReward, xpReward));
        if (contractHistory.size() > HISTORY_LIMIT) {
            contractHistory = new ArrayList<>(contractHistory.subList(0, HISTORY_LIMIT));
        }
    }

    public boolean unlockMilestone(String milestoneId) {
        if (milestoneId == null || milestoneId.isBlank() || unlockedMilestones.contains(milestoneId)) {
            return false;
        }
        unlockedMilestones.add(milestoneId);
        return true;
    }
}
