package com.example.advancedjobs.model;

public class DailyTaskProgress {
    private String taskId;
    private String jobId;
    private int target;
    private int progress;
    private long resetEpochSecond;
    private boolean completed;

    public DailyTaskProgress() {
    }

    public DailyTaskProgress(String taskId, String jobId, int target, int progress, long resetEpochSecond, boolean completed) {
        this.taskId = taskId;
        this.jobId = jobId;
        this.target = target;
        this.progress = progress;
        this.resetEpochSecond = resetEpochSecond;
        this.completed = completed;
    }

    public void addProgress(int amount) {
        if (!completed) {
            progress = Math.min(target, progress + amount);
            completed = progress >= target;
        }
    }

    public String taskId() { return taskId; }
    public String jobId() { return jobId; }
    public int target() { return target; }
    public int progress() { return progress; }
    public long resetEpochSecond() { return resetEpochSecond; }
    public boolean completed() { return completed; }
}
