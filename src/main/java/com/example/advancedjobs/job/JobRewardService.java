package com.example.advancedjobs.job;

import com.example.advancedjobs.api.EconomyProvider;
import com.example.advancedjobs.api.JobActionContext;
import com.example.advancedjobs.config.ConfigManager;
import com.example.advancedjobs.model.JobDefinition;
import com.example.advancedjobs.model.JobProgress;
import com.example.advancedjobs.model.PlayerJobProfile;
import com.example.advancedjobs.model.RewardDefinition;
import com.example.advancedjobs.util.DebugLog;
import com.example.advancedjobs.util.RewardUtil;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;

class JobRewardService {
    private final JobRegistry registry;
    private final JobProgressService progressService;
    private final PerkService perkService;
    private final JobSalaryService salaryService;
    private final JobAssignmentService assignmentService;
    private final JobProgressionService progressionService;

    JobRewardService(JobRegistry registry,
                     JobProgressService progressService,
                     PerkService perkService,
                     JobSalaryService salaryService,
                     JobAssignmentService assignmentService,
                     JobProgressionService progressionService) {
        this.registry = registry;
        this.progressService = progressService;
        this.perkService = perkService;
        this.salaryService = salaryService;
        this.assignmentService = assignmentService;
        this.progressionService = progressionService;
    }

    RewardDefinition handleAction(JobActionContext context,
                                  PlayerJobProfile profile,
                                  List<String> assignedJobs,
                                  EconomyProvider economyProvider,
                                  Consumer<PlayerJobProfile> saveProfile,
                                  Consumer<ServerPlayer> syncToPlayer,
                                  Consumer<String> grantTitle) {
        if (assignedJobs.isEmpty()) {
            return RewardDefinition.EMPTY;
        }
        profile.setPlayerName(context.player().getGameProfile().getName());
        double totalSalary = 0.0D;
        double totalXp = 0.0D;
        boolean rewarded = false;
        for (String jobId : assignedJobs) {
            Optional<JobDefinition> definitionOptional = registry.get(jobId);
            if (definitionOptional.isEmpty()) {
                continue;
            }
            JobDefinition definition = definitionOptional.get();
            RewardDefinition baseReward = registry.rewardFor(definition.id(), context.actionType(), context.targetId());
            if (baseReward == RewardDefinition.EMPTY) {
                continue;
            }
            rewarded = true;
            JobProgress progress = profile.progress(definition.id());
            assignmentService.ensureAssignments(profile, definition.id());
            double salaryBonus = perkService.aggregateEffect(definition.skillBranches(), progress, "salary_bonus");
            RewardDefinition finalReward = RewardUtil.applyMultipliers(
                baseReward.scaled(1.0D + salaryBonus),
                context,
                1.0D,
                ConfigManager.economy().worldMultipliers(),
                ConfigManager.economy().biomeMultipliers(),
                ConfigManager.economy().vipMultiplier(),
                ConfigManager.economy().eventMultiplier()
            );

            progressService.addXp(profile, definition, finalReward.xp());
            salaryService.creditSalary(context.player(), profile, definition.id(), finalReward.salary(), "job_action", economyProvider);
            progress.actionStats().merge(context.actionType().name() + "|" + context.targetId(), 1, Integer::sum);
            assignmentService.updateDailyTasks(context.player(), profile, definition.id(), context, economyProvider, grantTitle);
            assignmentService.updateContracts(context.player(), profile, definition.id(), context, economyProvider, grantTitle);
            progressionService.evaluateMilestones(context.player(), profile, definition.id(), progress, grantTitle);
            totalSalary += finalReward.salary();
            totalXp += finalReward.xp();
            DebugLog.log("Reward: player=" + context.player().getGameProfile().getName()
                + " job=" + definition.id()
                + " action=" + context.actionType()
                + " target=" + context.targetId()
                + " salary=" + finalReward.salary()
                + " xp=" + finalReward.xp());
        }
        if (!rewarded) {
            return RewardDefinition.EMPTY;
        }
        saveProfile.accept(profile);
        syncToPlayer.accept(context.player());
        return new RewardDefinition(totalSalary, totalXp, 0.0D, 0.0D);
    }

    double effectBonus(PlayerJobProfile profile, List<String> assignedJobs, String effectType) {
        double bonus = 0.0D;
        for (String jobId : assignedJobs) {
            Optional<JobDefinition> definitionOptional = registry.get(jobId);
            if (definitionOptional.isEmpty()) {
                continue;
            }
            bonus = Math.max(bonus, resolveEffectBonus(profile, definitionOptional.get(), effectType));
        }
        return bonus;
    }

    int effectNodes(PlayerJobProfile profile, List<String> assignedJobs, String effectType) {
        int count = 0;
        for (String jobId : assignedJobs) {
            Optional<JobDefinition> definitionOptional = registry.get(jobId);
            if (definitionOptional.isEmpty()) {
                continue;
            }
            JobDefinition definition = definitionOptional.get();
            count = Math.max(count, perkService.unlockedCount(definition.skillBranches(), profile.progress(definition.id()), effectType));
        }
        return count;
    }

    double totalEarnedAcrossJobs(PlayerJobProfile profile) {
        return profile.jobs().values().stream().mapToDouble(JobProgress::earnedTotal).sum();
    }

    int totalLevelsAcrossJobs(PlayerJobProfile profile) {
        return profile.jobs().values().stream().mapToInt(JobProgress::level).sum();
    }

    private double resolveEffectBonus(PlayerJobProfile profile, JobDefinition definition, String effectType) {
        double direct = perkService.aggregateEffect(definition.skillBranches(), profile.progress(definition.id()), effectType);
        if (direct > 0.0D) {
            return direct;
        }
        return switch (effectType) {
            case "ore_bonus", "wood_bonus", "crop_bonus", "breed_bonus", "fish_bonus", "combat_bonus",
                "build_bonus", "craft_bonus", "trade_bonus", "magic_bonus", "explore_bonus",
                "rare_gem_bonus", "sapling_return", "seed_keep", "treasure_chance", "loot_bonus",
                "block_refund", "discount_bonus", "ingredient_save", "artifact_bonus", "honey_bonus", "junk_reduction",
                "emerald_bonus", "rare_magic_bonus", "cache_finder", "elite_tracker", "decor_bonus" ->
                perkService.aggregateEffect(definition.skillBranches(), profile.progress(definition.id()), "resource_bonus");
            case "mine_aura", "forest_aura", "farm_aura", "pasture_aura", "water_aura", "combat_aura",
                "builder_aura", "craft_aura", "merchant_aura", "magic_aura", "explore_aura", "fall_guard", "ore_vision" ->
                perkService.aggregateEffect(definition.skillBranches(), profile.progress(definition.id()), "utility_bonus");
            case "auto_smelt" ->
                perkService.aggregateEffect(definition.skillBranches(), profile.progress(definition.id()), "resource_bonus");
            default -> 0.0D;
        };
    }
}
