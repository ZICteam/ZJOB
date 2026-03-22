package com.example.advancedjobs.job;

import com.example.advancedjobs.config.ConfigManager;
import com.example.advancedjobs.model.ActionRewardEntry;
import com.example.advancedjobs.model.JobDefinition;
import com.example.advancedjobs.model.JobActionType;
import com.example.advancedjobs.model.RewardDefinition;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public class JobRegistry {
    private final Map<String, JobDefinition> jobs = new LinkedHashMap<>();
    private final Map<String, Map<String, RewardDefinition>> rewardIndex = new LinkedHashMap<>();

    public void reload() {
        jobs.clear();
        rewardIndex.clear();
        for (Map.Entry<String, JobDefinition> entry : ConfigManager.jobs().definitions().entrySet()) {
            JobDefinition definition = entry.getValue();
            JobDefinition hydrated = new JobDefinition(
                definition.id(),
                definition.category(),
                definition.iconItem(),
                definition.translationKey(),
                definition.descriptionKey(),
                definition.maxLevel(),
                definition.actionRewards(),
                ConfigManager.perks().treeFor(definition.id()),
                definition.passivePerks(),
                definition.dailyTaskPool(),
                definition.contractPool()
            );
            jobs.put(entry.getKey(), hydrated);
            rewardIndex.put(entry.getKey(), buildRewardIndex(hydrated.actionRewards()));
        }
    }

    public Optional<JobDefinition> get(String jobId) {
        return Optional.ofNullable(jobs.get(jobId));
    }

    public Collection<JobDefinition> all() {
        return jobs.values();
    }

    public RewardDefinition rewardFor(String jobId, JobActionType actionType, ResourceLocation targetId) {
        Map<String, RewardDefinition> byAction = rewardIndex.get(jobId);
        if (byAction == null) {
            return RewardDefinition.EMPTY;
        }
        return byAction.getOrDefault(rewardKey(actionType, targetId), RewardDefinition.EMPTY);
    }

    public int jobCount() {
        return jobs.size();
    }

    public int rewardIndexJobCount() {
        return rewardIndex.size();
    }

    public int rewardIndexEntryCount() {
        int total = 0;
        for (Map<String, RewardDefinition> byAction : rewardIndex.values()) {
            total += byAction.size();
        }
        return total;
    }

    private static Map<String, RewardDefinition> buildRewardIndex(java.util.List<ActionRewardEntry> actionRewards) {
        Map<String, RewardDefinition> index = new LinkedHashMap<>();
        for (ActionRewardEntry entry : actionRewards) {
            index.put(rewardKey(entry.actionType(), entry.targetId()), entry.rewardDefinition());
        }
        return index;
    }

    private static String rewardKey(JobActionType actionType, ResourceLocation targetId) {
        return actionType.name() + "|" + targetId;
    }
}
