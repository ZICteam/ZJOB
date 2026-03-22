package com.example.advancedjobs.model;

public class DailyTaskHistoryEntry {
    private String taskId;
    private long completedAtEpochSecond;
    private double salaryReward;
    private double xpReward;

    public DailyTaskHistoryEntry() {
    }

    public DailyTaskHistoryEntry(String taskId, long completedAtEpochSecond, double salaryReward, double xpReward) {
        this.taskId = taskId;
        this.completedAtEpochSecond = completedAtEpochSecond;
        this.salaryReward = salaryReward;
        this.xpReward = xpReward;
    }

    public String taskId() { return taskId; }
    public long completedAtEpochSecond() { return completedAtEpochSecond; }
    public double salaryReward() { return salaryReward; }
    public double xpReward() { return xpReward; }
}
