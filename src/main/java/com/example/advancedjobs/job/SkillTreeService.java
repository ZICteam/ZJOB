package com.example.advancedjobs.job;

import com.example.advancedjobs.model.JobDefinition;
import com.example.advancedjobs.model.JobProgress;
import com.example.advancedjobs.model.PerkNodeState;
import com.example.advancedjobs.model.PlayerJobProfile;
import com.example.advancedjobs.model.SkillNode;
import java.util.Optional;

public class SkillTreeService {
    public boolean unlock(PlayerJobProfile profile, JobDefinition definition, String nodeId) {
        JobProgress progress = profile.progress(definition.id());
        Optional<SkillNode> nodeOptional = definition.skillBranches().stream()
            .flatMap(branch -> branch.nodes().stream())
            .filter(node -> node.id().equals(nodeId))
            .findFirst();
        if (nodeOptional.isEmpty()) {
            return false;
        }
        SkillNode node = nodeOptional.get();
        if (progress.level() < node.requiredLevel()) {
            return false;
        }
        if (node.parentId() != null && progress.unlockedNodes().stream().noneMatch(state -> state.nodeId().equals(node.parentId()) && state.unlocked())) {
            return false;
        }
        if (profile.availableSkillPoints(definition.id()) < node.cost()) {
            return false;
        }
        if (progress.unlockedNodes().stream().anyMatch(state -> state.nodeId().equals(nodeId) && state.unlocked())) {
            return false;
        }
        progress.unlockedNodes().add(new PerkNodeState(nodeId, true));
        progress.spendSkillPoints(node.cost());
        profile.markDirty();
        return true;
    }
}
