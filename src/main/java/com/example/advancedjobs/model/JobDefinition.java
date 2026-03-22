package com.example.advancedjobs.model;

import java.util.List;
import java.util.Map;

public record JobDefinition(
    String id,
    String category,
    String iconItem,
    String translationKey,
    String descriptionKey,
    int maxLevel,
    List<ActionRewardEntry> actionRewards,
    List<SkillBranch> skillBranches,
    Map<Integer, List<String>> passivePerks,
    List<String> dailyTaskPool,
    List<String> contractPool
) {
}
