package com.example.advancedjobs.job;

import com.example.advancedjobs.config.ConfigManager;
import com.example.advancedjobs.api.EconomyProvider;
import com.example.advancedjobs.entity.NpcRole;
import com.example.advancedjobs.model.ActionRewardEntry;
import com.example.advancedjobs.model.ContractProgress;
import com.example.advancedjobs.model.DailyTaskProgress;
import com.example.advancedjobs.model.JobDefinition;
import com.example.advancedjobs.model.JobProgress;
import com.example.advancedjobs.model.PlayerJobProfile;
import com.example.advancedjobs.util.TimeUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import net.minecraft.server.level.ServerPlayer;

class JobPayloadService {
    private final JobAssignmentService assignmentService;
    private final JobSalaryService salaryService;

    JobPayloadService(JobAssignmentService assignmentService, JobSalaryService salaryService) {
        this.assignmentService = assignmentService;
        this.salaryService = salaryService;
    }

    String createCatalogPayload(Collection<JobDefinition> definitions) {
        JsonObject root = new JsonObject();
        JsonArray jobs = new JsonArray();
        for (JobDefinition definition : definitions) {
            JsonObject job = new JsonObject();
            job.addProperty("id", definition.id());
            job.addProperty("nameKey", definition.translationKey());
            job.addProperty("descriptionKey", definition.descriptionKey());
            JsonArray passives = new JsonArray();
            definition.passivePerks().forEach((level, keys) -> {
                JsonObject passive = new JsonObject();
                passive.addProperty("level", level);
                JsonArray items = new JsonArray();
                keys.forEach(items::add);
                passive.add("keys", items);
                passives.add(passive);
            });
            job.add("passives", passives);
            JsonArray rewardPreview = new JsonArray();
            int rewardCount = 0;
            for (ActionRewardEntry entry : definition.actionRewards()) {
                rewardCount++;
                if (rewardPreview.size() >= 4) {
                    continue;
                }
                JsonObject reward = new JsonObject();
                reward.addProperty("type", entry.actionType().name());
                reward.addProperty("targetId", entry.targetId().toString());
                reward.addProperty("salary", entry.rewardDefinition().salary());
                reward.addProperty("xp", entry.rewardDefinition().xp());
                rewardPreview.add(reward);
            }
            job.addProperty("rewardCount", rewardCount);
            job.add("rewardPreview", rewardPreview);
            JsonArray branches = new JsonArray();
            for (var branch : definition.skillBranches()) {
                JsonObject branchObj = new JsonObject();
                branchObj.addProperty("id", branch.id());
                branchObj.addProperty("translationKey", branch.translationKey());
                JsonArray nodes = new JsonArray();
                for (var node : branch.nodes()) {
                    JsonObject nodeObj = new JsonObject();
                    nodeObj.addProperty("id", node.id());
                    nodeObj.addProperty("translationKey", node.translationKey());
                    nodeObj.addProperty("requiredLevel", node.requiredLevel());
                    nodeObj.addProperty("cost", node.cost());
                    if (node.parentId() != null) {
                        nodeObj.addProperty("parentId", node.parentId());
                    }
                    nodeObj.addProperty("effectType", node.effectType());
                    nodeObj.addProperty("effectValue", node.effectValue());
                    nodeObj.addProperty("unlocked", false);
                    nodes.add(nodeObj);
                }
                branchObj.add("nodes", nodes);
                branches.add(branchObj);
            }
            job.add("skillBranches", branches);
            jobs.add(job);
        }
        root.add("jobs", jobs);
        JsonObject npcSkins = new JsonObject();
        for (NpcRole role : NpcRole.values()) {
            JsonObject profile = ConfigManager.npcSkins().profile(role.id()).deepCopy();
            String imageBase64 = ConfigManager.npcSkins().localSkinBase64(role.id());
            if (imageBase64 != null) {
                profile.addProperty("imageBase64", imageBase64);
            }
            npcSkins.add(role.id(), profile);
        }
        root.add("npcSkins", npcSkins);
        return ConfigManager.GSON.toJson(root);
    }

    String createPlayerPayload(ServerPlayer player,
                               PlayerJobProfile profile,
                               Collection<JobDefinition> definitions,
                               EconomyProvider economyProvider,
                               boolean internalEconomy) {
        JsonObject root = new JsonObject();
        List<String> assignedJobs = assignedJobIds(profile);
        root.addProperty("playerName", profile.playerName());
        if (profile.activeJobId() != null) {
            root.addProperty("activeJobId", profile.activeJobId());
        }
        if (profile.secondaryJobId() != null) {
            root.addProperty("secondaryJobId", profile.secondaryJobId());
        }
        root.addProperty("balance", economyProvider.getBalance(profile.playerId()));
        root.addProperty("allowSecondaryJob", ConfigManager.COMMON.allowSecondaryJob.get());
        root.addProperty("instantSalary", ConfigManager.COMMON.instantSalary.get());
        root.addProperty("jobChangePrice", ConfigManager.COMMON.jobChangePrice.get());
        root.addProperty("jobChangeCooldownSeconds", ConfigManager.COMMON.jobChangeCooldownSeconds.get());
        root.addProperty("jobChangeCooldownRemaining", Math.max(0L, ConfigManager.COMMON.jobChangeCooldownSeconds.get() - (TimeUtil.now() - profile.lastJobChangeEpochSecond())));
        root.addProperty("salaryClaimIntervalSeconds", ConfigManager.COMMON.salaryClaimIntervalSeconds.get());
        root.addProperty("salaryClaimCooldownRemaining", salaryService.salaryClaimCooldownRemaining(profile));
        root.addProperty("maxSalaryPerClaim", ConfigManager.COMMON.maxSalaryPerClaim.get());
        root.addProperty("salaryTaxRate", ConfigManager.COMMON.salaryTaxRate.get());
        root.addProperty("contractRerollPrice", ConfigManager.COMMON.contractRerollPrice.get());
        root.addProperty("contractRerollCooldownRemaining", assignmentService.contractRerollCooldownRemaining(profile));
        root.addProperty("economyProvider", internalEconomy ? "internal" : "external");
        root.addProperty("economyCurrency", ConfigManager.economy().externalCurrencyId());
        root.addProperty("taxSinkAccountUuid", ConfigManager.economy().taxSinkAccountUuid());
        root.addProperty("blockArtificialMobRewards", ConfigManager.COMMON.blockArtificialMobRewards.get());
        root.addProperty("blockBabyMobRewards", ConfigManager.COMMON.blockBabyMobRewards.get());
        root.addProperty("blockTamedMobRewards", ConfigManager.COMMON.blockTamedMobRewards.get());
        root.addProperty("lootContainerRewardCooldownSeconds", ConfigManager.COMMON.lootContainerRewardCooldownSeconds.get());
        root.addProperty("exploredChunkRewardCooldownSeconds", ConfigManager.COMMON.exploredChunkRewardCooldownSeconds.get());
        String worldId = player.serverLevel().dimension().location().toString();
        String biomeId = player.serverLevel().getBiome(player.blockPosition()).unwrapKey().map(key -> key.location().toString()).orElse("minecraft:plains");
        double worldMultiplier = ConfigManager.economy().worldMultipliers().getOrDefault(worldId, 1.0D);
        double biomeMultiplier = ConfigManager.economy().biomeMultipliers().getOrDefault(biomeId, 1.0D);
        root.addProperty("currentWorldId", worldId);
        root.addProperty("currentBiomeId", biomeId);
        root.addProperty("worldRewardMultiplier", worldMultiplier);
        root.addProperty("biomeRewardMultiplier", biomeMultiplier);
        root.addProperty("eventRewardMultiplier", ConfigManager.economy().eventMultiplier());
        root.addProperty("eventEndsAtEpochSecond", ConfigManager.economy().eventEndsAtEpochSecond());
        root.addProperty("eventRemainingSeconds", Math.max(0L, ConfigManager.economy().eventEndsAtEpochSecond() - TimeUtil.now()));
        root.addProperty("vipRewardMultiplier", ConfigManager.economy().vipMultiplier());
        root.addProperty("effectiveRewardMultiplier",
            worldMultiplier * biomeMultiplier * ConfigManager.economy().eventMultiplier() * ConfigManager.economy().vipMultiplier());
        JsonArray unlockedTitles = new JsonArray();
        profile.unlockedTitles().forEach(unlockedTitles::add);
        root.add("unlockedTitles", unlockedTitles);
        JsonArray jobs = new JsonArray();
        for (JobDefinition definition : definitions) {
            JobProgress progress = profile.progress(definition.id());
            Map<String, ConfigManager.DailyTaskTemplate> dailyTemplatesById = dailyTemplatesById(definition.id());
            Map<String, ConfigManager.ContractTemplate> contractTemplatesById = contractTemplatesById(definition.id());
            JsonObject job = new JsonObject();
            job.addProperty("id", definition.id());
            job.addProperty("level", progress.level());
            job.addProperty("xp", progress.xp());
            job.addProperty("pendingSalary", progress.pendingSalary());
            job.addProperty("earnedTotal", progress.earnedTotal());
            job.addProperty("skillPoints", profile.availableSkillPoints(definition.id()));
            job.addProperty("milestoneCount", progress.unlockedMilestones().size());
            JsonArray milestones = new JsonArray();
            progress.unlockedMilestones().forEach(milestones::add);
            job.add("milestones", milestones);
            JsonArray unlockedNodes = new JsonArray();
            progress.unlockedNodes().stream()
                .filter(state -> state.unlocked())
                .forEach(state -> unlockedNodes.add(state.nodeId()));
            job.add("unlockedNodes", unlockedNodes);
            if (assignedJobs.contains(definition.id())) {
                assignmentService.ensureAssignments(profile, definition.id());
                progress.dailyTasks().stream()
                    .mapToLong(DailyTaskProgress::resetEpochSecond)
                    .min()
                    .ifPresent(resetAt -> job.addProperty("dailyResetAt", resetAt));
                JsonArray daily = new JsonArray();
                for (DailyTaskProgress task : progress.dailyTasks()) {
                    JsonObject dailyObj = new JsonObject();
                    dailyObj.addProperty("id", task.taskId());
                    dailyObj.addProperty("progress", task.progress());
                    dailyObj.addProperty("target", task.target());
                    dailyObj.addProperty("completed", task.completed());
                    applyDailyTemplate(dailyObj, dailyTemplatesById.get(task.taskId()), true);
                    daily.add(dailyObj);
                }
                job.add("dailyTasks", daily);
                job.addProperty("completedDailyCount", progress.dailyHistory().size());
                JsonArray dailyHistory = new JsonArray();
                progress.dailyHistory().stream().limit(3).forEach(entry -> {
                    JsonObject historyObj = new JsonObject();
                    historyObj.addProperty("id", entry.taskId());
                    historyObj.addProperty("completedAt", entry.completedAtEpochSecond());
                    historyObj.addProperty("salaryReward", entry.salaryReward());
                    historyObj.addProperty("xpReward", entry.xpReward());
                    applyDailyTemplate(historyObj, dailyTemplatesById.get(entry.taskId()), false);
                    dailyHistory.add(historyObj);
                });
                job.add("dailyHistory", dailyHistory);
                JsonArray contracts = new JsonArray();
                for (ContractProgress contract : progress.contracts()) {
                    JsonObject contractObj = new JsonObject();
                    contractObj.addProperty("id", contract.contractId());
                    contractObj.addProperty("rarity", contract.rarity());
                    contractObj.addProperty("progress", contract.progress());
                    contractObj.addProperty("target", contract.target());
                    contractObj.addProperty("completed", contract.completed());
                    contractObj.addProperty("expiresAt", contract.expiresAtEpochSecond());
                    applyContractTemplate(contractObj, contractTemplatesById.get(contract.contractId()), true);
                    contracts.add(contractObj);
                }
                job.add("contracts", contracts);
                job.addProperty("completedContractCount", progress.contractHistory().size());
                JsonArray contractHistory = new JsonArray();
                progress.contractHistory().stream().limit(3).forEach(entry -> {
                    JsonObject historyObj = new JsonObject();
                    historyObj.addProperty("id", entry.contractId());
                    historyObj.addProperty("rarity", entry.rarity());
                    historyObj.addProperty("completedAt", entry.completedAtEpochSecond());
                    historyObj.addProperty("salaryReward", entry.salaryReward());
                    historyObj.addProperty("xpReward", entry.xpReward());
                    applyContractTemplate(historyObj, contractTemplatesById.get(entry.contractId()), false);
                    contractHistory.add(historyObj);
                });
                job.add("contractHistory", contractHistory);
                progress.contracts().stream()
                    .mapToLong(ContractProgress::expiresAtEpochSecond)
                    .min()
                    .ifPresent(expiresAt -> job.addProperty("nextContractRotationAt", expiresAt));
            }
            jobs.add(job);
        }
        root.add("jobs", jobs);
        return ConfigManager.GSON.toJson(root);
    }

    String createLeaderboardPayload(String jobId,
                                    Iterable<PlayerJobProfile> entries,
                                    int limit,
                                    ToIntFunction<PlayerJobProfile> totalLevelsAcrossJobs,
                                    ToDoubleFunction<PlayerJobProfile> totalXpAcrossJobs,
                                    ToDoubleFunction<PlayerJobProfile> totalEarnedAcrossJobs) {
        JsonArray top = new JsonArray();
        int i = 0;
        for (PlayerJobProfile leaderboardEntry : entries) {
            if (i++ >= limit) {
                break;
            }
            JsonObject entry = new JsonObject();
            entry.addProperty("player", leaderboardEntry.playerName());
            entry.addProperty("jobId", jobId);
            if ("all".equalsIgnoreCase(jobId)) {
                entry.addProperty("level", totalLevelsAcrossJobs.applyAsInt(leaderboardEntry));
                entry.addProperty("xp", totalXpAcrossJobs.applyAsDouble(leaderboardEntry));
                entry.addProperty("earned", totalEarnedAcrossJobs.applyAsDouble(leaderboardEntry));
            } else {
                entry.addProperty("level", leaderboardEntry.progress(jobId).level());
                entry.addProperty("xp", leaderboardEntry.progress(jobId).xp());
                entry.addProperty("earned", leaderboardEntry.progress(jobId).earnedTotal());
            }
            top.add(entry);
        }
        return ConfigManager.GSON.toJson(top);
    }

    private Map<String, ConfigManager.DailyTaskTemplate> dailyTemplatesById(String jobId) {
        Map<String, ConfigManager.DailyTaskTemplate> templates = new LinkedHashMap<>();
        for (ConfigManager.DailyTaskTemplate template : ConfigManager.dailyTasks().tasksForJob(jobId)) {
            templates.put(template.id(), template);
        }
        return templates;
    }

    private Map<String, ConfigManager.ContractTemplate> contractTemplatesById(String jobId) {
        Map<String, ConfigManager.ContractTemplate> templates = new LinkedHashMap<>();
        for (ConfigManager.ContractTemplate template : ConfigManager.contracts().contractsForJob(jobId)) {
            templates.put(template.id(), template);
        }
        return templates;
    }

    private void applyDailyTemplate(JsonObject target, ConfigManager.DailyTaskTemplate template, boolean includeRewardValues) {
        if (template == null) {
            return;
        }
        target.addProperty("type", template.type().name());
        target.addProperty("targetId", template.target().toString());
        if (includeRewardValues) {
            target.addProperty("salaryReward", template.salary());
            target.addProperty("xpReward", template.xp());
        }
        if (template.bonusItem() != null && template.bonusCount() > 0) {
            target.addProperty("bonusItem", template.bonusItem());
            target.addProperty("bonusCount", template.bonusCount());
        }
        if (template.buffEffect() != null && template.buffDurationSeconds() > 0) {
            target.addProperty("buffEffect", template.buffEffect());
            target.addProperty("buffDurationSeconds", template.buffDurationSeconds());
            target.addProperty("buffAmplifier", template.buffAmplifier());
        }
        if (template.bonusTitle() != null) {
            target.addProperty("bonusTitle", template.bonusTitle());
        }
    }

    private void applyContractTemplate(JsonObject target, ConfigManager.ContractTemplate template, boolean includeRewardValues) {
        if (template == null) {
            return;
        }
        target.addProperty("type", template.type().name());
        target.addProperty("targetId", template.target().toString());
        if (includeRewardValues) {
            target.addProperty("salaryReward", template.salary());
            target.addProperty("xpReward", template.xp());
        }
        if (template.bonusItem() != null && template.bonusCount() > 0) {
            target.addProperty("bonusItem", template.bonusItem());
            target.addProperty("bonusCount", template.bonusCount());
        }
        if (template.buffEffect() != null && template.buffDurationSeconds() > 0) {
            target.addProperty("buffEffect", template.buffEffect());
            target.addProperty("buffDurationSeconds", template.buffDurationSeconds());
            target.addProperty("buffAmplifier", template.buffAmplifier());
        }
        if (template.bonusTitle() != null) {
            target.addProperty("bonusTitle", template.bonusTitle());
        }
    }

    private List<String> assignedJobIds(PlayerJobProfile profile) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        if (profile.activeJobId() != null) {
            ids.add(profile.activeJobId());
        }
        if (ConfigManager.COMMON.allowSecondaryJob.get() && profile.secondaryJobId() != null) {
            ids.add(profile.secondaryJobId());
        }
        return List.copyOf(ids);
    }
}
