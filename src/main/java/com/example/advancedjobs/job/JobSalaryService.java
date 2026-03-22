package com.example.advancedjobs.job;

import com.example.advancedjobs.AdvancedJobsMod;
import com.example.advancedjobs.api.EconomyProvider;
import com.example.advancedjobs.config.ConfigManager;
import com.example.advancedjobs.model.PlayerJobProfile;
import com.example.advancedjobs.util.DebugLog;
import com.example.advancedjobs.util.TimeUtil;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;

class JobSalaryService {
    private final JobProgressService progressService;

    JobSalaryService(JobProgressService progressService) {
        this.progressService = progressService;
    }

    long salaryClaimCooldownRemaining(PlayerJobProfile profile) {
        return Math.max(0L, ConfigManager.COMMON.salaryClaimIntervalSeconds.get() - (TimeUtil.now() - profile.lastSalaryClaimEpochSecond()));
    }

    double claimSalary(ServerPlayer player,
                       PlayerJobProfile profile,
                       List<String> assignedJobs,
                       EconomyProvider economyProvider,
                       Consumer<PlayerJobProfile> saveProfile,
                       Consumer<ServerPlayer> syncPlayer) {
        long remainingCooldown = salaryClaimCooldownRemaining(profile);
        if (remainingCooldown > 0L) {
            return -remainingCooldown;
        }
        if (assignedJobs.isEmpty()) {
            return 0.0D;
        }
        Map<String, Double> claimedPerJob = new LinkedHashMap<>();
        double remainingCap = ConfigManager.COMMON.maxSalaryPerClaim.get();
        double gross = 0.0D;
        for (String jobId : assignedJobs) {
            if (remainingCap <= 0.0D) {
                break;
            }
            double claimed = profile.progress(jobId).takePendingSalary(remainingCap);
            if (claimed <= 0.0D) {
                continue;
            }
            claimedPerJob.put(jobId, claimed);
            gross += claimed;
            remainingCap -= claimed;
        }
        if (gross <= 0.0D) {
            return 0.0D;
        }
        double taxAmount = gross * ConfigManager.COMMON.salaryTaxRate.get();
        double payout = Math.max(0.0D, gross - taxAmount);
        AdvancedJobsMod.LOGGER.info(
            "AdvancedJobs salary claim requested: player={} provider={} currency={} gross={} tax={} net={} jobs={}",
            player.getGameProfile().getName(),
            economyProvider.id(),
            ConfigManager.economy().externalCurrencyId(),
            gross,
            taxAmount,
            payout,
            assignedJobs
        );
        if (!economyProvider.deposit(player.getUUID(), payout, "job_salary_claim")) {
            AdvancedJobsMod.LOGGER.warn(
                "AdvancedJobs salary claim deposit failed: player={} provider={} currency={} net={}",
                player.getGameProfile().getName(),
                economyProvider.id(),
                ConfigManager.economy().externalCurrencyId(),
                payout
            );
            claimedPerJob.forEach((jobId, amount) -> profile.progress(jobId).restorePendingSalary(amount));
            return 0.0D;
        }
        AdvancedJobsMod.LOGGER.info(
            "AdvancedJobs salary claim deposit ok: player={} provider={} currency={} net={}",
            player.getGameProfile().getName(),
            economyProvider.id(),
            ConfigManager.economy().externalCurrencyId(),
            payout
        );
        routeTaxToServerAccount(economyProvider, taxAmount, "job_salary_claim_tax");
        profile.setLastSalaryClaimEpochSecond(TimeUtil.now());
        profile.markDirty();
        saveProfile.accept(profile);
        syncPlayer.accept(player);
        DebugLog.log("Salary claim: player=" + player.getGameProfile().getName() + " jobs=" + assignedJobs + " paid=" + payout);
        return payout;
    }

    void creditSalary(ServerPlayer player,
                      PlayerJobProfile profile,
                      String jobId,
                      double grossSalary,
                      String reason,
                      EconomyProvider economyProvider) {
        double safeGross = Math.max(0.0D, grossSalary);
        if (safeGross <= 0.0D) {
            return;
        }
        if (!ConfigManager.COMMON.instantSalary.get()) {
            progressService.addSalary(profile, jobId, safeGross);
            return;
        }
        double taxAmount = safeGross * ConfigManager.COMMON.salaryTaxRate.get();
        double netPayout = Math.max(0.0D, safeGross - taxAmount);
        AdvancedJobsMod.LOGGER.info(
            "AdvancedJobs instant salary requested: player={} job={} provider={} currency={} gross={} tax={} net={} reason={}",
            player.getGameProfile().getName(),
            jobId,
            economyProvider.id(),
            ConfigManager.economy().externalCurrencyId(),
            safeGross,
            taxAmount,
            netPayout,
            reason
        );
        if (economyProvider.deposit(profile.playerId(), netPayout, reason)) {
            AdvancedJobsMod.LOGGER.info(
                "AdvancedJobs instant salary deposit ok: player={} job={} provider={} currency={} net={} reason={}",
                player.getGameProfile().getName(),
                jobId,
                economyProvider.id(),
                ConfigManager.economy().externalCurrencyId(),
                netPayout,
                reason
            );
            routeTaxToServerAccount(economyProvider, taxAmount, reason + "_tax");
            progressService.addEarnedSalary(profile, jobId, safeGross);
            DebugLog.log("Instant salary: player=" + player.getGameProfile().getName()
                + " job=" + jobId
                + " gross=" + safeGross
                + " net=" + netPayout
                + " reason=" + reason);
            return;
        }
        AdvancedJobsMod.LOGGER.warn(
            "AdvancedJobs instant salary deposit failed: player={} job={} provider={} currency={} net={} reason={}",
            player.getGameProfile().getName(),
            jobId,
            economyProvider.id(),
            ConfigManager.economy().externalCurrencyId(),
            netPayout,
            reason
        );
        progressService.addSalary(profile, jobId, safeGross);
        DebugLog.log("Instant salary fallback to pending: player=" + player.getGameProfile().getName()
            + " job=" + jobId
            + " gross=" + safeGross
            + " reason=" + reason);
    }

    private void routeTaxToServerAccount(EconomyProvider economyProvider, double taxAmount, String reason) {
        if (taxAmount <= 0.0D) {
            return;
        }
        UUID sinkId = ConfigManager.economy().taxSinkAccountId();
        if (sinkId == null) {
            AdvancedJobsMod.LOGGER.warn("Skipping tax routing due to invalid taxSinkAccountUuid");
            return;
        }
        if (!economyProvider.deposit(sinkId, taxAmount, reason)) {
            AdvancedJobsMod.LOGGER.warn("Failed to route tax to server account: account={} amount={} reason={}",
                sinkId, taxAmount, reason);
            return;
        }
        AdvancedJobsMod.LOGGER.info("AdvancedJobs tax routed: account={} provider={} currency={} amount={} reason={}",
            sinkId, economyProvider.id(), ConfigManager.economy().externalCurrencyId(), taxAmount, reason);
    }
}
