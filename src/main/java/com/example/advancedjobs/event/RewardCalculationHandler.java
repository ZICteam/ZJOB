package com.example.advancedjobs.event;

import com.example.advancedjobs.api.JobActionContext;
import com.example.advancedjobs.job.JobManager;
import com.example.advancedjobs.model.RewardDefinition;

public class RewardCalculationHandler {
    private final JobManager jobManager;

    public RewardCalculationHandler(JobManager jobManager) {
        this.jobManager = jobManager;
    }

    public RewardDefinition reward(JobActionContext context) {
        return jobManager.handleAction(context);
    }
}
