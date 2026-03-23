package com.example.advancedjobs.job;

import com.example.advancedjobs.api.EconomyProvider;
import com.example.advancedjobs.config.ConfigManager;
import com.example.advancedjobs.model.JobDefinition;
import com.example.advancedjobs.model.JobProgress;
import com.example.advancedjobs.model.PlayerJobProfile;
import com.example.advancedjobs.util.TimeUtil;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;

class JobProfileMutationService {
    private final JobAssignmentService assignmentService;
    private final JobProgressionService progressionService;

    JobProfileMutationService(JobAssignmentService assignmentService, JobProgressionService progressionService) {
        this.assignmentService = assignmentService;
        this.progressionService = progressionService;
    }

    boolean chooseJob(ServerPlayer player,
                      PlayerJobProfile profile,
                      Optional<JobDefinition> definition,
                      String jobId,
                      boolean secondary,
                      EconomyProvider economyProvider,
                      Consumer<PlayerJobProfile> saveProfile,
                      Consumer<String> debugLog) {
        if (definition.isEmpty()) {
            return false;
        }
        assignmentService.ensureAssignments(profile, jobId);
        if (secondary && !ConfigManager.COMMON.allowSecondaryJob.get()) {
            return false;
        }
        String currentJobId = secondary ? profile.secondaryJobId() : profile.activeJobId();
        String otherJobId = secondary ? profile.activeJobId() : profile.secondaryJobId();
        if (jobId.equals(otherJobId)) {
            return false;
        }
        long now = TimeUtil.now();
        if (currentJobId != null && !currentJobId.equals(jobId)) {
            if (now - profile.lastJobChangeEpochSecond() < ConfigManager.COMMON.jobChangeCooldownSeconds.get()) {
                return false;
            }
            double price = ConfigManager.COMMON.jobChangePrice.get();
            if (price > 0.0D && !economyProvider.withdraw(player.getUUID(), price, "job_change")) {
                return false;
            }
            forgetProgress(profile, currentJobId);
        }
        profile.progress(jobId);
        if (secondary) {
            profile.setSecondaryJobId(jobId);
        } else {
            profile.setActiveJobId(jobId);
        }
        profile.setLastJobChangeEpochSecond(now);
        saveProfile.accept(profile);
        debugLog.accept("Job selected: player=" + player.getGameProfile().getName() + " job=" + jobId + " slot=" + (secondary ? "secondary" : "primary"));
        return true;
    }

    boolean leaveJob(ServerPlayer player,
                     PlayerJobProfile profile,
                     boolean secondary,
                     Consumer<PlayerJobProfile> saveProfile,
                     Consumer<String> debugLog) {
        String currentJobId = secondary ? profile.secondaryJobId() : profile.activeJobId();
        if (currentJobId == null) {
            return false;
        }
        forgetProgress(profile, currentJobId);
        if (secondary) {
            profile.setSecondaryJobId(null);
        } else {
            profile.setActiveJobId(null);
        }
        profile.setLastJobChangeEpochSecond(TimeUtil.now());
        saveProfile.accept(profile);
        debugLog.accept("Job left: player=" + player.getGameProfile().getName() + " job=" + currentJobId + " slot=" + (secondary ? "secondary" : "primary"));
        return true;
    }

    void resetProfile(PlayerJobProfile profile, Consumer<PlayerJobProfile> saveProfile) {
        profile.jobs().clear();
        profile.setActiveJobId(null);
        profile.setSecondaryJobId(null);
        profile.setLastJobChangeEpochSecond(0L);
        saveProfile.accept(profile);
    }

    boolean adminSetJob(PlayerJobProfile profile, Optional<JobDefinition> definition, String jobId, boolean secondary, Consumer<PlayerJobProfile> saveProfile) {
        if (definition.isEmpty()) {
            return false;
        }
        profile.progress(jobId);
        if (secondary) {
            if (!ConfigManager.COMMON.allowSecondaryJob.get()) {
                return false;
            }
            if (jobId.equals(profile.activeJobId())) {
                return false;
            }
            profile.setSecondaryJobId(jobId);
        } else {
            if (jobId.equals(profile.secondaryJobId())) {
                return false;
            }
            profile.setActiveJobId(jobId);
        }
        saveProfile.accept(profile);
        return true;
    }

    boolean adminSetLevel(ServerPlayer player, PlayerJobProfile profile, Optional<JobDefinition> definition, String jobId, int level, Consumer<PlayerJobProfile> saveProfile, Consumer<String> grantTitle) {
        if (definition.isEmpty()) {
            return false;
        }
        JobProgress progress = profile.progress(jobId);
        progress.setLevel(level);
        progressionService.evaluateMilestones(player, profile, jobId, progress, grantTitle);
        saveProfile.accept(profile);
        return true;
    }

    boolean adminAddXp(ServerPlayer player, PlayerJobProfile profile, Optional<JobDefinition> definition, String jobId, double amount, Consumer<PlayerJobProfile> saveProfile, Consumer<String> grantTitle) {
        if (definition.isEmpty()) {
            return false;
        }
        JobProgress progress = profile.progress(jobId);
        progress.addXp(Math.max(0.0D, amount));
        progressionService.evaluateMilestones(player, profile, jobId, progress, grantTitle);
        profile.markDirty();
        saveProfile.accept(profile);
        return true;
    }

    boolean adminAddSalary(PlayerJobProfile profile, double amount, Consumer<PlayerJobProfile> saveProfile) {
        if (profile.activeJobId() == null) {
            return false;
        }
        profile.progress(profile.activeJobId()).addPendingSalary(Math.max(0.0D, amount));
        profile.markDirty();
        saveProfile.accept(profile);
        return true;
    }

    boolean adminAdjustSkillPoints(PlayerJobProfile profile, int amount, Consumer<PlayerJobProfile> saveProfile) {
        if (profile.activeJobId() == null) {
            return false;
        }
        JobProgress progress = profile.progress(profile.activeJobId());
        if (amount >= 0) {
            progress.refundSkillPoints(amount);
        } else {
            progress.spendSkillPoints(-amount);
        }
        profile.markDirty();
        saveProfile.accept(profile);
        return true;
    }

    void forgetProgress(PlayerJobProfile profile, String jobId) {
        if (!ConfigManager.COMMON.resetProgressOnChange.get()) {
            return;
        }
        if (ConfigManager.COMMON.storeAllJobProgress.get()) {
            JobProgress progress = profile.progress(jobId);
            progress.resetLeveling();
            progress.claimPendingSalary();
            progress.unlockedNodes().clear();
            progress.dailyTasks().clear();
            progress.contracts().clear();
            progress.dailyHistory().clear();
            progress.contractHistory().clear();
            progress.unlockedMilestones().clear();
            progress.actionStats().clear();
            profile.markDirty();
            return;
        }
        profile.jobs().remove(jobId);
        profile.markDirty();
    }
}
