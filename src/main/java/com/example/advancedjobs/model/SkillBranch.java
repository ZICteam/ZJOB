package com.example.advancedjobs.model;

import java.util.List;

public record SkillBranch(String id, String translationKey, List<SkillNode> nodes) {
}
