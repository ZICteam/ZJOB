package com.example.advancedjobs.job;

import com.example.advancedjobs.config.ConfigManager;
import com.example.advancedjobs.model.JobDefinition;
import com.example.advancedjobs.model.JobProgress;
import com.example.advancedjobs.model.PlayerJobProfile;
import com.example.advancedjobs.util.XpFormulaUtil;

public class JobProgressService {
    public void addXp(PlayerJobProfile profile, JobDefinition definition, double xp) {
        JobProgress progress = profile.progress(definition.id());
        progress.addXp(Math.max(0.0D, xp));
        levelUpIfNeeded(progress, definition.maxLevel());
        profile.markDirty();
    }

    public void addSalary(PlayerJobProfile profile, String jobId, double salary) {
        profile.progress(jobId).addPendingSalary(salary);
        profile.markDirty();
    }

    public void addEarnedSalary(PlayerJobProfile profile, String jobId, double salary) {
        profile.progress(jobId).addEarnedSalary(salary);
        profile.markDirty();
    }

    private void levelUpIfNeeded(JobProgress progress, int maxLevel) {
        while (progress.level() < maxLevel) {
            double required = XpFormulaUtil.requiredXpForLevel(progress.level(), ConfigManager.COMMON.baseXp.get(), ConfigManager.COMMON.growthFactor.get());
            if (progress.xp() < required) {
                break;
            }
            progress.addXp(-required);
            progress.setLevel(progress.level() + 1);
        }
    }
}
