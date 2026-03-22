package com.example.advancedjobs.job;

import com.example.advancedjobs.model.JobProgress;
import com.example.advancedjobs.model.PerkNodeState;
import com.example.advancedjobs.model.SkillBranch;
import com.example.advancedjobs.model.SkillNode;
import java.util.HashSet;
import java.util.Set;

public class PerkService {
    public double aggregateEffect(Iterable<SkillBranch> branches, JobProgress progress, String effectType) {
        Set<String> unlocked = new HashSet<>();
        addUnlocked(progress, unlocked);
        double total = 0.0D;
        for (SkillBranch branch : branches) {
            for (SkillNode node : branch.nodes()) {
                if (effectType.equals(node.effectType()) && unlocked.contains(node.id())) {
                    total += node.effectValue();
                }
            }
        }
        return total;
    }

    public int unlockedCount(Iterable<SkillBranch> branches, JobProgress progress, String effectType) {
        Set<String> unlocked = new HashSet<>();
        addUnlocked(progress, unlocked);
        int total = 0;
        for (SkillBranch branch : branches) {
            for (SkillNode node : branch.nodes()) {
                if (effectType.equals(node.effectType()) && unlocked.contains(node.id())) {
                    total++;
                }
            }
        }
        return total;
    }

    private void addUnlocked(JobProgress progress, Set<String> unlocked) {
        for (PerkNodeState node : progress.unlockedNodes()) {
            if (node.unlocked()) {
                unlocked.add(node.nodeId());
            }
        }
    }
}
