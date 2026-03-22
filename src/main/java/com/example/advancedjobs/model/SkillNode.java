package com.example.advancedjobs.model;

public record SkillNode(
    String id,
    String translationKey,
    int requiredLevel,
    int cost,
    String parentId,
    String effectType,
    double effectValue
) {
}
