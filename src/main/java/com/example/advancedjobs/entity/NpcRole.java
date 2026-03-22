package com.example.advancedjobs.entity;

public enum NpcRole {
    JOBS_MASTER("jobs_master", "entity.advancedjobs.jobs_master"),
    CONTRACTS_BOARD("contracts_board", "entity.advancedjobs.contracts_board"),
    DAILY_BOARD("daily_board", "entity.advancedjobs.daily_board"),
    STATUS_BOARD("status_board", "entity.advancedjobs.status_board"),
    SALARY_BOARD("salary_board", "entity.advancedjobs.salary_board"),
    SKILLS_BOARD("skills_board", "entity.advancedjobs.skills_board"),
    LEADERBOARD_BOARD("leaderboard_board", "entity.advancedjobs.leaderboard_board"),
    HELP_BOARD("help_board", "entity.advancedjobs.help_board");

    private final String id;
    private final String translationKey;

    NpcRole(String id, String translationKey) {
        this.id = id;
        this.translationKey = translationKey;
    }

    public String id() {
        return id;
    }

    public String translationKey() {
        return translationKey;
    }
}
