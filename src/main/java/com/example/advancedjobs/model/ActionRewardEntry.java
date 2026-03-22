package com.example.advancedjobs.model;

import net.minecraft.resources.ResourceLocation;

public record ActionRewardEntry(JobActionType actionType, ResourceLocation targetId, RewardDefinition rewardDefinition) {
}
