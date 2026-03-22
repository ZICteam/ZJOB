package com.example.advancedjobs.api;

import com.example.advancedjobs.model.PlayerJobProfile;
import com.example.advancedjobs.model.RewardDefinition;

public interface JobBonusProvider {
    RewardDefinition apply(PlayerJobProfile profile, String jobId, RewardDefinition baseReward, JobActionContext context);
}
