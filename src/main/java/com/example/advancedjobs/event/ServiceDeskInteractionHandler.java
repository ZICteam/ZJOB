package com.example.advancedjobs.event;

import com.example.advancedjobs.config.ConfigManager;
import com.example.advancedjobs.content.ModEntities;
import com.example.advancedjobs.content.ModItems;
import com.example.advancedjobs.entity.ContractsBoardEntity;
import com.example.advancedjobs.entity.DailyBoardEntity;
import com.example.advancedjobs.entity.HelpBoardEntity;
import com.example.advancedjobs.entity.JobsMasterEntity;
import com.example.advancedjobs.entity.LeaderboardBoardEntity;
import com.example.advancedjobs.entity.NpcRole;
import com.example.advancedjobs.entity.SalaryBoardEntity;
import com.example.advancedjobs.entity.SkillsBoardEntity;
import com.example.advancedjobs.entity.StatusBoardEntity;
import com.example.advancedjobs.job.JobManager;
import com.example.advancedjobs.model.JobDefinition;
import com.example.advancedjobs.model.PlayerJobProfile;
import com.example.advancedjobs.util.TextUtil;
import com.example.advancedjobs.util.TimeUtil;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

class ServiceDeskInteractionHandler {
    private static final int ROUTE_HINT_RADIUS = 16;

    private final JobManager jobManager;

    ServiceDeskInteractionHandler(JobManager jobManager) {
        this.jobManager = jobManager;
    }

    boolean handle(PlayerInteractEvent.EntityInteract event, ServerPlayer player, LivingEntity living) {
        if (tryMarkRoleWithWand(event, player, living)) {
            return true;
        }
        if (handleNativeBoardEntity(event, player, living)) {
            return true;
        }
        return handleTaggedBoardEntity(event, player, living);
    }

    private boolean tryMarkRoleWithWand(PlayerInteractEvent.EntityInteract event, ServerPlayer player, LivingEntity living) {
        if (!player.getMainHandItem().is(ModItems.JOB_NPC_WAND.get()) && !player.getOffhandItem().is(ModItems.JOB_NPC_WAND.get())) {
            return false;
        }
        boolean sprintMode = player.isSprinting();
        boolean helpMode = player.getOffhandItem().is(ModItems.JOB_NPC_WAND.get()) && sprintMode && !player.isShiftKeyDown();
        boolean statusMode = player.getOffhandItem().is(ModItems.JOB_NPC_WAND.get()) && sprintMode && player.isShiftKeyDown();
        boolean salaryMode = player.getOffhandItem().is(ModItems.JOB_NPC_WAND.get()) && player.isShiftKeyDown() && !sprintMode;
        boolean dailyMode = player.getOffhandItem().is(ModItems.JOB_NPC_WAND.get()) && !player.isShiftKeyDown() && !sprintMode;
        boolean contractsMode = !dailyMode && !salaryMode && player.isShiftKeyDown() && !sprintMode;
        boolean skillsMode = player.getMainHandItem().is(ModItems.JOB_NPC_WAND.get()) && sprintMode && !player.isShiftKeyDown();
        boolean leaderboardMode = player.getMainHandItem().is(ModItems.JOB_NPC_WAND.get()) && sprintMode && player.isShiftKeyDown();
        living.getPersistentData().putBoolean(JobEventHandler.JOB_NPC_TAG, !contractsMode && !dailyMode && !statusMode && !salaryMode && !skillsMode && !leaderboardMode && !helpMode);
        living.getPersistentData().putBoolean(JobEventHandler.CONTRACTS_BOARD_TAG, contractsMode);
        living.getPersistentData().putBoolean(JobEventHandler.DAILY_BOARD_TAG, dailyMode);
        living.getPersistentData().putBoolean(JobEventHandler.STATUS_BOARD_TAG, statusMode);
        living.getPersistentData().putBoolean(JobEventHandler.SALARY_BOARD_TAG, salaryMode);
        living.getPersistentData().putBoolean(JobEventHandler.SKILLS_BOARD_TAG, skillsMode);
        living.getPersistentData().putBoolean(JobEventHandler.LEADERBOARD_BOARD_TAG, leaderboardMode);
        living.getPersistentData().putBoolean(JobEventHandler.HELP_BOARD_TAG, helpMode);
        String roleId = dailyMode
            ? "daily_board"
            : statusMode
                ? "status_board"
            : salaryMode
                ? "salary_board"
            : helpMode
                ? "help_board"
            : skillsMode
                ? "skills_board"
            : leaderboardMode
                ? "leaderboard_board"
            : contractsMode
                ? "contracts_board"
                : "jobs_master";
        living.setCustomName(Component.literal(ConfigManager.npcLabels().label(roleId)));
        living.setCustomNameVisible(true);
        if (living instanceof Mob mob
            && !(living instanceof JobsMasterEntity)
            && !(living instanceof ContractsBoardEntity)
            && !(living instanceof DailyBoardEntity)
            && !(living instanceof StatusBoardEntity)
            && !(living instanceof SalaryBoardEntity)
            && !(living instanceof SkillsBoardEntity)
            && !(living instanceof LeaderboardBoardEntity)
            && !(living instanceof HelpBoardEntity)) {
            mob.setNoAi(true);
            mob.setPersistenceRequired();
        }
        player.sendSystemMessage(Component.translatable(dailyMode
            ? "message.advancedjobs.daily_board_marked"
            : statusMode
                ? "message.advancedjobs.status_board_marked"
            : salaryMode
                ? "message.advancedjobs.salary_board_marked"
            : helpMode
                ? "message.advancedjobs.help_board_marked"
            : skillsMode
                ? "message.advancedjobs.skills_board_marked"
            : leaderboardMode
                ? "message.advancedjobs.leaderboard_board_marked"
            : contractsMode
                ? "message.advancedjobs.contracts_board_marked"
                : "message.advancedjobs.jobs_npc_marked"));
        consume(event);
        return true;
    }

    private boolean handleNativeBoardEntity(PlayerInteractEvent.EntityInteract event, ServerPlayer player, LivingEntity living) {
        if (living instanceof JobsMasterEntity) {
            String targetJobId = resolveTargetJobId(player, event.getHand());
            jobManager.openScreen(player, player.isShiftKeyDown() ? "my_job" : "jobs", targetJobId);
            consume(event);
            return true;
        }
        if (living instanceof ContractsBoardEntity) {
            jobManager.openScreen(player, "contracts");
            consume(event);
            return true;
        }
        if (living instanceof DailyBoardEntity) {
            String targetJobId = resolveTargetJobId(player, event.getHand());
            jobManager.openScreen(player, player.isShiftKeyDown() ? "my_job" : "daily", targetJobId);
            consume(event);
            return true;
        }
        if (living instanceof StatusBoardEntity) {
            String targetJobId = resolveTargetJobId(player, event.getHand());
            jobManager.openScreen(player, player.isShiftKeyDown() ? "jobs" : "my_job", targetJobId);
            consume(event);
            return true;
        }
        if (living instanceof SalaryBoardEntity) {
            String targetJobId = resolveTargetJobId(player, event.getHand());
            if (player.isShiftKeyDown()) {
                handleSalaryClaim(player);
            } else {
                jobManager.openScreen(player, "salary", targetJobId);
            }
            consume(event);
            return true;
        }
        if (living instanceof HelpBoardEntity) {
            String targetJobId = resolveTargetJobId(player, event.getHand());
            jobManager.openScreen(player, player.isShiftKeyDown() ? "jobs" : "help", targetJobId);
            consume(event);
            return true;
        }
        if (living instanceof SkillsBoardEntity) {
            String targetJobId = resolveTargetJobId(player, event.getHand());
            jobManager.openScreen(player, player.isShiftKeyDown() ? "my_job" : "skills", targetJobId);
            consume(event);
            return true;
        }
        if (living instanceof LeaderboardBoardEntity) {
            String targetJobId = resolveTargetJobId(player, event.getHand());
            jobManager.openScreen(player, player.isShiftKeyDown() ? "my_job" : "top", targetJobId);
            consume(event);
            return true;
        }
        return false;
    }

    private boolean handleTaggedBoardEntity(PlayerInteractEvent.EntityInteract event, ServerPlayer player, LivingEntity living) {
        PlayerJobProfile profile = jobManager.getOrCreateProfile(player);
        boolean useSecondary = useSecondarySlot(profile, event.getHand());
        String targetJobId = useSecondary ? profile.secondaryJobId() : profile.activeJobId();
        if (living.getPersistentData().getBoolean(JobEventHandler.CONTRACTS_BOARD_TAG)) {
            sendContractsBoardHint(player, profile, useSecondary, targetJobId);
            if (player.isShiftKeyDown()) {
                JobManager.ContractRerollResult preview = jobManager.previewContractReroll(player, targetJobId);
                if (preview == JobManager.ContractRerollResult.SUCCESS && jobManager.rerollContracts(player, targetJobId)) {
                    Component jobName = Component.literal(targetJobId == null ? "none" : targetJobId);
                    Optional<JobDefinition> definition = targetJobId == null ? Optional.empty() : jobManager.job(targetJobId);
                    if (definition.isPresent()) {
                        jobName = Component.translatable(definition.get().translationKey());
                    }
                    player.sendSystemMessage(Component.translatable("message.advancedjobs.contracts_board_rerolled", jobName));
                } else {
                    player.sendSystemMessage(contractRerollFailureMessage(player, profile, preview));
                }
            } else {
                jobManager.openScreen(player, "contracts", targetJobId);
            }
            consume(event);
            return true;
        }
        if (living.getPersistentData().getBoolean(JobEventHandler.DAILY_BOARD_TAG)) {
            sendDailyBoardHint(player, profile, useSecondary, targetJobId);
            jobManager.openScreen(player, player.isShiftKeyDown() ? "my_job" : "daily", targetJobId);
            consume(event);
            return true;
        }
        if (living.getPersistentData().getBoolean(JobEventHandler.STATUS_BOARD_TAG)) {
            sendStatusBoardHint(player, profile, useSecondary, targetJobId);
            jobManager.openScreen(player, player.isShiftKeyDown() ? "jobs" : "my_job", targetJobId);
            consume(event);
            return true;
        }
        if (living.getPersistentData().getBoolean(JobEventHandler.SALARY_BOARD_TAG)) {
            sendSalaryBoardHint(player, profile, useSecondary, targetJobId);
            if (player.isShiftKeyDown()) {
                handleSalaryClaim(player);
            } else {
                jobManager.openScreen(player, "salary", targetJobId);
            }
            consume(event);
            return true;
        }
        if (living.getPersistentData().getBoolean(JobEventHandler.HELP_BOARD_TAG)) {
            sendHelpBoardHint(player, profile, useSecondary, targetJobId);
            jobManager.openScreen(player, player.isShiftKeyDown() ? "jobs" : "help", targetJobId);
            consume(event);
            return true;
        }
        if (living.getPersistentData().getBoolean(JobEventHandler.SKILLS_BOARD_TAG)) {
            sendSkillsBoardHint(player, profile, useSecondary, targetJobId);
            jobManager.openScreen(player, player.isShiftKeyDown() ? "my_job" : "skills", targetJobId);
            consume(event);
            return true;
        }
        if (living.getPersistentData().getBoolean(JobEventHandler.LEADERBOARD_BOARD_TAG)) {
            sendLeaderboardBoardHint(player, profile, useSecondary, targetJobId);
            jobManager.openScreen(player, player.isShiftKeyDown() ? "my_job" : "top", targetJobId);
            consume(event);
            return true;
        }
        if (living.getPersistentData().getBoolean(JobEventHandler.JOB_NPC_TAG)) {
            sendJobsMasterHint(player, profile, useSecondary, targetJobId);
            jobManager.openScreen(player, player.isShiftKeyDown() ? "my_job" : "jobs", targetJobId);
            consume(event);
            return true;
        }
        return false;
    }

    private void handleSalaryClaim(ServerPlayer player) {
        if (ConfigManager.COMMON.instantSalary.get()) {
            player.sendSystemMessage(TextUtil.tr("command.advancedjobs.salary.auto_mode"));
            return;
        }
        double paid = jobManager.claimSalary(player);
        if (paid < 0.0D) {
            player.sendSystemMessage(TextUtil.tr("command.advancedjobs.salary.cooldown",
                TimeUtil.formatRemainingSeconds((long) -paid)));
        } else if (paid > 0.0D) {
            player.sendSystemMessage(TextUtil.tr("command.advancedjobs.salary.claimed", TextUtil.fmt2(paid)));
        } else {
            player.sendSystemMessage(Component.translatable("message.advancedjobs.salary_board_empty"));
        }
    }

    private boolean useSecondarySlot(PlayerJobProfile profile, InteractionHand hand) {
        return hand == InteractionHand.OFF_HAND
            && ConfigManager.COMMON.allowSecondaryJob.get()
            && profile.secondaryJobId() != null;
    }

    private String resolveTargetJobId(ServerPlayer player, InteractionHand hand) {
        PlayerJobProfile profile = jobManager.getOrCreateProfile(player);
        return useSecondarySlot(profile, hand) ? profile.secondaryJobId() : profile.activeJobId();
    }

    private Component contractRerollFailureMessage(ServerPlayer player, PlayerJobProfile profile, JobManager.ContractRerollResult result) {
        return switch (result) {
            case NO_JOB, NOT_ASSIGNED -> Component.translatable("message.advancedjobs.contracts_board_reroll_no_job");
            case COOLDOWN -> Component.translatable("message.advancedjobs.contracts_board_reroll_cooldown",
                TimeUtil.formatRemainingSeconds(jobManager.contractRerollCooldownRemaining(profile)));
            case INSUFFICIENT_FUNDS -> Component.translatable("message.advancedjobs.contracts_board_reroll_insufficient_funds",
                TextUtil.fmt2(ConfigManager.COMMON.contractRerollPrice.get()));
            case SUCCESS -> Component.translatable("message.advancedjobs.contracts_board_rerolled", player.getDisplayName());
        };
    }

    private void sendJobsMasterHint(ServerPlayer player, PlayerJobProfile profile, boolean secondary, String targetJobId) {
        player.sendSystemMessage(Component.translatable("message.advancedjobs.jobs_master.slot_hint",
            Component.translatable(secondary ? "command.advancedjobs.common.slot.secondary" : "command.advancedjobs.common.slot.primary")));
        if (targetJobId == null) {
            player.sendSystemMessage(Component.translatable("message.advancedjobs.jobs_master.no_job"));
        } else {
            player.sendSystemMessage(Component.translatable("message.advancedjobs.jobs_master.job_hint",
                Component.translatable("job.advancedjobs." + targetJobId)));
            player.sendSystemMessage(Component.translatable("message.advancedjobs.jobs_master.first_hour_focus",
                starterFocusLabel(profile, targetJobId),
                starterFocusCommand(profile, targetJobId, secondary)));
        }
        long remaining = Math.max(0L, ConfigManager.COMMON.jobChangeCooldownSeconds.get() - (TimeUtil.now() - profile.lastJobChangeEpochSecond()));
        player.sendSystemMessage(Component.translatable("message.advancedjobs.jobs_master.change_hint",
            ConfigManager.COMMON.jobChangePrice.get(),
            TimeUtil.formatRemainingSeconds(remaining)));
        NpcRole nextRole = recommendedRole(profile, secondary);
        if (nextRole != NpcRole.JOBS_MASTER) {
            player.sendSystemMessage(Component.translatable("message.advancedjobs.jobs_master.next_step",
                Component.translatable(nextRole.translationKey()),
                guideReason(profile, nextRole, secondary)));
            int nextRoleCount = nearbyBoardCount(player, nextRole);
            if (nextRoleCount > 0) {
                String nextRoleSource = nearestSource(player, nextRole);
                player.sendSystemMessage(Component.translatable("message.advancedjobs.route.next_step_available",
                    Component.translatable(nextRole.translationKey()), nextRoleCount, ROUTE_HINT_RADIUS, nextRoleSource));
            } else {
                player.sendSystemMessage(Component.translatable("message.advancedjobs.route.next_step_missing",
                    Component.translatable(nextRole.translationKey()), ROUTE_HINT_RADIUS));
            }
        }
    }

    private void sendDailyBoardHint(ServerPlayer player, PlayerJobProfile profile, boolean secondary, String targetJobId) {
        player.sendSystemMessage(Component.translatable("message.advancedjobs.daily_board.slot_hint",
            Component.translatable(secondary ? "command.advancedjobs.common.slot.secondary" : "command.advancedjobs.common.slot.primary")));
        if (targetJobId == null) {
            player.sendSystemMessage(Component.translatable("message.advancedjobs.daily_board.no_job"));
            return;
        }
        var dailies = profile.progress(targetJobId).dailyTasks();
        String resetAt = dailies.isEmpty() ? "--" : TimeUtil.formatRemainingSeconds(Math.max(0L, dailies.get(0).resetEpochSecond() - TimeUtil.now()));
        player.sendSystemMessage(Component.translatable("message.advancedjobs.daily_board.job_hint",
            Component.translatable("job.advancedjobs." + targetJobId), dailies.size(), resetAt));
        NpcRole nextRole = followUpRoleAfterDaily(profile, secondary);
        player.sendSystemMessage(Component.translatable("message.advancedjobs.daily_board.next_step",
            Component.translatable(nextRole.translationKey()),
            guideReason(profile, nextRole, secondary)));
        int nextRoleCount = nearbyBoardCount(player, nextRole);
        if (nextRoleCount > 0) {
            String nextRoleSource = nearestSource(player, nextRole);
            player.sendSystemMessage(Component.translatable("message.advancedjobs.route.next_step_available",
                Component.translatable(nextRole.translationKey()), nextRoleCount, ROUTE_HINT_RADIUS, nextRoleSource));
        } else {
            player.sendSystemMessage(Component.translatable("message.advancedjobs.route.next_step_missing",
                Component.translatable(nextRole.translationKey()), ROUTE_HINT_RADIUS));
        }
    }

    private void sendStatusBoardHint(ServerPlayer player, PlayerJobProfile profile, boolean secondary, String targetJobId) {
        player.sendSystemMessage(Component.translatable("message.advancedjobs.status_board.slot_hint",
            Component.translatable(secondary ? "command.advancedjobs.common.slot.secondary" : "command.advancedjobs.common.slot.primary")));
        if (targetJobId == null) {
            player.sendSystemMessage(Component.translatable("message.advancedjobs.status_board.no_job"));
            return;
        }
        var progress = profile.progress(targetJobId);
        String latestTitle = profile.unlockedTitles().isEmpty() ? "-" : profile.unlockedTitles().stream().reduce((a, b) -> b).orElse("-");
        String latestMilestone = progress.unlockedMilestones().isEmpty() ? "-" : progress.unlockedMilestones().get(progress.unlockedMilestones().size() - 1);
        player.sendSystemMessage(Component.translatable("message.advancedjobs.status_board.job_hint",
            Component.translatable("job.advancedjobs." + targetJobId), progress.level(), progress.unlockedMilestones().size(), latestMilestone, latestTitle));
        NpcRole nextRole = recommendedRole(profile, secondary);
        if (nextRole != NpcRole.STATUS_BOARD) {
            player.sendSystemMessage(Component.translatable("message.advancedjobs.status_board.next_step",
                Component.translatable(nextRole.translationKey()),
                guideReason(profile, nextRole, secondary)));
            int nextRoleCount = nearbyBoardCount(player, nextRole);
            if (nextRoleCount > 0) {
                String nextRoleSource = nearestSource(player, nextRole);
                player.sendSystemMessage(Component.translatable("message.advancedjobs.route.next_step_available",
                    Component.translatable(nextRole.translationKey()), nextRoleCount, ROUTE_HINT_RADIUS, nextRoleSource));
            } else {
                player.sendSystemMessage(Component.translatable("message.advancedjobs.route.next_step_missing",
                    Component.translatable(nextRole.translationKey()), ROUTE_HINT_RADIUS));
            }
        }
    }

    private void sendSalaryBoardHint(ServerPlayer player, PlayerJobProfile profile, boolean secondary, String targetJobId) {
        player.sendSystemMessage(Component.translatable("message.advancedjobs.salary_board.slot_hint",
            Component.translatable(secondary ? "command.advancedjobs.common.slot.secondary" : "command.advancedjobs.common.slot.primary")));
        if (targetJobId == null) {
            player.sendSystemMessage(Component.translatable("message.advancedjobs.salary_board.no_job"));
        } else {
            player.sendSystemMessage(Component.translatable("message.advancedjobs.salary_board.job_hint",
                Component.translatable("job.advancedjobs." + targetJobId),
                TextUtil.fmt2(profile.progress(targetJobId).pendingSalary())));
        }
        player.sendSystemMessage(Component.translatable("message.advancedjobs.salary_board.mode_hint",
            Component.translatable(ConfigManager.COMMON.instantSalary.get() ? "gui.advancedjobs.salary_mode.instant" : "gui.advancedjobs.salary_mode.manual")));
        player.sendSystemMessage(Component.translatable("message.advancedjobs.salary_board.cooldown_hint",
            TimeUtil.formatRemainingSeconds(jobManager.salaryClaimCooldownRemaining(profile))));
        NpcRole nextRole = followUpRoleAfterSalary(profile, secondary);
        if (nextRole != NpcRole.SALARY_BOARD) {
            player.sendSystemMessage(Component.translatable("message.advancedjobs.salary_board.next_step",
                Component.translatable(nextRole.translationKey()),
                guideReason(profile, nextRole, secondary)));
            int nextRoleCount = nearbyBoardCount(player, nextRole);
            if (nextRoleCount > 0) {
                String nextRoleSource = nearestSource(player, nextRole);
                player.sendSystemMessage(Component.translatable("message.advancedjobs.route.next_step_available",
                    Component.translatable(nextRole.translationKey()), nextRoleCount, ROUTE_HINT_RADIUS, nextRoleSource));
            } else {
                player.sendSystemMessage(Component.translatable("message.advancedjobs.route.next_step_missing",
                    Component.translatable(nextRole.translationKey()), ROUTE_HINT_RADIUS));
            }
        }
    }

    private void sendHelpBoardHint(ServerPlayer player, PlayerJobProfile profile, boolean secondary, String targetJobId) {
        player.sendSystemMessage(Component.translatable("message.advancedjobs.help_board.slot_hint",
            Component.translatable(secondary ? "command.advancedjobs.common.slot.secondary" : "command.advancedjobs.common.slot.primary")));
        if (targetJobId == null) {
            player.sendSystemMessage(Component.translatable("message.advancedjobs.help_board.no_job"));
        } else {
            player.sendSystemMessage(Component.translatable("message.advancedjobs.help_board.job_hint",
                Component.translatable("job.advancedjobs." + targetJobId)));
            player.sendSystemMessage(Component.translatable("message.advancedjobs.help_board.first_hour_focus",
                starterFocusLabel(profile, targetJobId),
                starterFocusCommand(profile, targetJobId, secondary)));
        }
        player.sendSystemMessage(Component.translatable("message.advancedjobs.help_board.salary_hint",
            Component.translatable(ConfigManager.COMMON.instantSalary.get() ? "gui.advancedjobs.salary_mode.instant" : "gui.advancedjobs.salary_mode.manual")));
        NpcRole nextRole = recommendedRole(profile, secondary);
        player.sendSystemMessage(Component.translatable("message.advancedjobs.help_board.next_step",
            Component.translatable(nextRole.translationKey()),
            guideReason(profile, nextRole, secondary)));
        int nextRoleCount = nearbyBoardCount(player, nextRole);
        if (nextRoleCount <= 0) {
            player.sendSystemMessage(Component.translatable("message.advancedjobs.help_board.next_step_missing",
                Component.translatable(nextRole.translationKey()),
                ROUTE_HINT_RADIUS));
        } else {
            String nextRoleSource = nearestSource(player, nextRole);
            player.sendSystemMessage(Component.translatable("message.advancedjobs.help_board.next_step_available",
                Component.translatable(nextRole.translationKey()),
                nextRoleCount,
                ROUTE_HINT_RADIUS,
                nextRoleSource));
        }
        NpcRole followUpRole = secondaryRecommendedRole(profile, nextRole);
        if (followUpRole != null) {
            int followUpCount = nearbyBoardCount(player, followUpRole);
            if (followUpCount <= 0) {
                player.sendSystemMessage(Component.translatable("message.advancedjobs.help_board.follow_up_missing",
                    Component.translatable(followUpRole.translationKey()),
                    ROUTE_HINT_RADIUS));
            } else {
                String followUpSource = nearestSource(player, followUpRole);
                player.sendSystemMessage(Component.translatable("message.advancedjobs.help_board.follow_up_available",
                    Component.translatable(followUpRole.translationKey()),
                    followUpCount,
                    ROUTE_HINT_RADIUS,
                    followUpSource));
            }
        }
        player.sendSystemMessage(Component.translatable("message.advancedjobs.help_board.hub_hint",
            nearbyBoardCount(player, NpcRole.JOBS_MASTER),
            nearbyBoardCount(player, NpcRole.DAILY_BOARD),
            nearbyBoardCount(player, NpcRole.CONTRACTS_BOARD),
            nearbyBoardCount(player, NpcRole.SALARY_BOARD),
            nearbyBoardCount(player, NpcRole.SKILLS_BOARD),
            nearbyBoardCount(player, NpcRole.LEADERBOARD_BOARD),
            nearbyBoardCount(player, NpcRole.STATUS_BOARD)));
    }

    private void sendSkillsBoardHint(ServerPlayer player, PlayerJobProfile profile, boolean secondary, String targetJobId) {
        player.sendSystemMessage(Component.translatable("message.advancedjobs.skills_board.slot_hint",
            Component.translatable(secondary ? "command.advancedjobs.common.slot.secondary" : "command.advancedjobs.common.slot.primary")));
        if (targetJobId == null) {
            player.sendSystemMessage(Component.translatable("message.advancedjobs.skills_board.no_job"));
            return;
        }
        var progress = profile.progress(targetJobId);
        player.sendSystemMessage(Component.translatable("message.advancedjobs.skills_board.job_hint",
            Component.translatable("job.advancedjobs." + targetJobId),
            profile.availableSkillPoints(targetJobId),
            progress.unlockedNodes().size()));
        NpcRole nextRole = followUpRoleAfterSkills(profile, secondary);
        player.sendSystemMessage(Component.translatable("message.advancedjobs.skills_board.next_step",
            Component.translatable(nextRole.translationKey()),
            guideReason(profile, nextRole, secondary)));
        int nextRoleCount = nearbyBoardCount(player, nextRole);
        if (nextRoleCount > 0) {
            String nextRoleSource = nearestSource(player, nextRole);
            player.sendSystemMessage(Component.translatable("message.advancedjobs.route.next_step_available",
                Component.translatable(nextRole.translationKey()), nextRoleCount, ROUTE_HINT_RADIUS, nextRoleSource));
        } else {
            player.sendSystemMessage(Component.translatable("message.advancedjobs.route.next_step_missing",
                Component.translatable(nextRole.translationKey()), ROUTE_HINT_RADIUS));
        }
    }

    private void sendLeaderboardBoardHint(ServerPlayer player, PlayerJobProfile profile, boolean secondary, String targetJobId) {
        player.sendSystemMessage(Component.translatable("message.advancedjobs.leaderboard_board.slot_hint",
            Component.translatable(secondary ? "command.advancedjobs.common.slot.secondary" : "command.advancedjobs.common.slot.primary")));
        if (targetJobId == null) {
            var entries = jobManager.overallLeaderboard();
            String leader = entries.isEmpty() ? "-" : entries.iterator().next().playerName();
            player.sendSystemMessage(Component.translatable("message.advancedjobs.leaderboard_board.overall_hint", entries.size(), leader));
            NpcRole nextRole = recommendedRole(profile, secondary);
            player.sendSystemMessage(Component.translatable("message.advancedjobs.leaderboard_board.next_step",
                Component.translatable(nextRole.translationKey()),
                guideReason(profile, nextRole, secondary)));
            int nextRoleCount = nearbyBoardCount(player, nextRole);
            if (nextRoleCount > 0) {
                String nextRoleSource = nearestSource(player, nextRole);
                player.sendSystemMessage(Component.translatable("message.advancedjobs.route.next_step_available",
                    Component.translatable(nextRole.translationKey()), nextRoleCount, ROUTE_HINT_RADIUS, nextRoleSource));
            } else {
                player.sendSystemMessage(Component.translatable("message.advancedjobs.route.next_step_missing",
                    Component.translatable(nextRole.translationKey()), ROUTE_HINT_RADIUS));
            }
            return;
        }
        var entries = jobManager.leaderboard(targetJobId);
        String leader = entries.isEmpty() ? "-" : entries.iterator().next().playerName();
        player.sendSystemMessage(Component.translatable("message.advancedjobs.leaderboard_board.job_hint",
            Component.translatable("job.advancedjobs." + targetJobId), entries.size(), leader));
        NpcRole nextRole = recommendedRole(profile, secondary);
        player.sendSystemMessage(Component.translatable("message.advancedjobs.leaderboard_board.next_step",
            Component.translatable(nextRole.translationKey()),
            guideReason(profile, nextRole, secondary)));
        int nextRoleCount = nearbyBoardCount(player, nextRole);
        if (nextRoleCount > 0) {
            String nextRoleSource = nearestSource(player, nextRole);
            player.sendSystemMessage(Component.translatable("message.advancedjobs.route.next_step_available",
                Component.translatable(nextRole.translationKey()), nextRoleCount, ROUTE_HINT_RADIUS, nextRoleSource));
        } else {
            player.sendSystemMessage(Component.translatable("message.advancedjobs.route.next_step_missing",
                Component.translatable(nextRole.translationKey()), ROUTE_HINT_RADIUS));
        }
    }

    private void sendContractsBoardHint(ServerPlayer player, PlayerJobProfile profile, boolean secondary, String targetJobId) {
        player.sendSystemMessage(Component.translatable("message.advancedjobs.contracts_board.slot_hint",
            Component.translatable(secondary ? "command.advancedjobs.common.slot.secondary" : "command.advancedjobs.common.slot.primary")));
        if (targetJobId == null) {
            player.sendSystemMessage(Component.translatable("message.advancedjobs.contracts_board.no_job"));
        } else {
            player.sendSystemMessage(Component.translatable("message.advancedjobs.contracts_board.job_hint",
                Component.translatable("job.advancedjobs." + targetJobId),
                profile.progress(targetJobId).contracts().size()));
        }
        player.sendSystemMessage(Component.translatable("message.advancedjobs.contracts_board.reroll_hint",
            TextUtil.fmt2(ConfigManager.COMMON.contractRerollPrice.get()),
            TimeUtil.formatRemainingSeconds(jobManager.contractRerollCooldownRemaining(profile))));
        NpcRole nextRole = followUpRoleAfterContracts(profile, secondary);
        player.sendSystemMessage(Component.translatable("message.advancedjobs.contracts_board.next_step",
            Component.translatable(nextRole.translationKey()),
            guideReason(profile, nextRole, secondary)));
        int nextRoleCount = nearbyBoardCount(player, nextRole);
        if (nextRoleCount > 0) {
            String nextRoleSource = nearestSource(player, nextRole);
            player.sendSystemMessage(Component.translatable("message.advancedjobs.route.next_step_available",
                Component.translatable(nextRole.translationKey()), nextRoleCount, ROUTE_HINT_RADIUS, nextRoleSource));
        } else {
            player.sendSystemMessage(Component.translatable("message.advancedjobs.route.next_step_missing",
                Component.translatable(nextRole.translationKey()), ROUTE_HINT_RADIUS));
        }
    }

    private NpcRole recommendedRole(PlayerJobProfile profile, boolean secondary) {
        String jobId = secondary ? profile.secondaryJobId() : profile.activeJobId();
        if (jobId == null) {
            return NpcRole.JOBS_MASTER;
        }
        var progress = profile.progress(jobId);
        if (!ConfigManager.COMMON.instantSalary.get() && progress.pendingSalary() > 0.0D) {
            return NpcRole.SALARY_BOARD;
        }
        if (progress.dailyTasks().stream().anyMatch(task -> !task.completed())) {
            return NpcRole.DAILY_BOARD;
        }
        if (progress.contracts().stream().anyMatch(task -> !task.completed())) {
            return NpcRole.CONTRACTS_BOARD;
        }
        if (profile.availableSkillPoints(jobId) > 0) {
            return NpcRole.SKILLS_BOARD;
        }
        return NpcRole.STATUS_BOARD;
    }

    private NpcRole secondaryRecommendedRole(PlayerJobProfile profile, NpcRole primary) {
        if (primary != NpcRole.LEADERBOARD_BOARD && profile.activeJobId() != null) {
            return NpcRole.LEADERBOARD_BOARD;
        }
        if (primary != NpcRole.HELP_BOARD) {
            return NpcRole.HELP_BOARD;
        }
        return null;
    }

    private NpcRole followUpRoleAfterDaily(PlayerJobProfile profile, boolean secondary) {
        String jobId = secondary ? profile.secondaryJobId() : profile.activeJobId();
        if (jobId == null) {
            return NpcRole.JOBS_MASTER;
        }
        var progress = profile.progress(jobId);
        if (progress.contracts().stream().anyMatch(task -> !task.completed())) {
            return NpcRole.CONTRACTS_BOARD;
        }
        if (profile.availableSkillPoints(jobId) > 0) {
            return NpcRole.SKILLS_BOARD;
        }
        if (!ConfigManager.COMMON.instantSalary.get() && progress.pendingSalary() > 0.0D) {
            return NpcRole.SALARY_BOARD;
        }
        return NpcRole.STATUS_BOARD;
    }

    private NpcRole followUpRoleAfterSkills(PlayerJobProfile profile, boolean secondary) {
        String jobId = secondary ? profile.secondaryJobId() : profile.activeJobId();
        if (jobId == null) {
            return NpcRole.JOBS_MASTER;
        }
        var progress = profile.progress(jobId);
        if (!ConfigManager.COMMON.instantSalary.get() && progress.pendingSalary() > 0.0D) {
            return NpcRole.SALARY_BOARD;
        }
        if (progress.dailyTasks().stream().anyMatch(task -> !task.completed())) {
            return NpcRole.DAILY_BOARD;
        }
        if (progress.contracts().stream().anyMatch(task -> !task.completed())) {
            return NpcRole.CONTRACTS_BOARD;
        }
        return NpcRole.STATUS_BOARD;
    }

    private NpcRole followUpRoleAfterSalary(PlayerJobProfile profile, boolean secondary) {
        return recommendedRole(profile, secondary);
    }

    private NpcRole followUpRoleAfterContracts(PlayerJobProfile profile, boolean secondary) {
        String jobId = secondary ? profile.secondaryJobId() : profile.activeJobId();
        if (jobId == null) {
            return NpcRole.JOBS_MASTER;
        }
        var progress = profile.progress(jobId);
        if (profile.availableSkillPoints(jobId) > 0) {
            return NpcRole.SKILLS_BOARD;
        }
        if (!ConfigManager.COMMON.instantSalary.get() && progress.pendingSalary() > 0.0D) {
            return NpcRole.SALARY_BOARD;
        }
        return NpcRole.STATUS_BOARD;
    }

    private String guideReason(PlayerJobProfile profile, NpcRole role, boolean secondary) {
        return switch (role) {
            case JOBS_MASTER -> TextUtil.tr("command.advancedjobs.guide.reason.jobs_master",
                Component.translatable(secondary ? "command.advancedjobs.common.slot.secondary" : "command.advancedjobs.common.slot.primary")).getString();
            case SALARY_BOARD -> TextUtil.tr("command.advancedjobs.guide.reason.salary",
                TextUtil.fmt2(pendingSalaryForSlot(profile, secondary))).getString();
            case DAILY_BOARD -> TextUtil.tr("command.advancedjobs.guide.reason.daily",
                Component.translatable(secondary ? "command.advancedjobs.common.slot.secondary" : "command.advancedjobs.common.slot.primary")).getString();
            case CONTRACTS_BOARD -> TextUtil.tr("command.advancedjobs.guide.reason.contracts",
                Component.translatable(secondary ? "command.advancedjobs.common.slot.secondary" : "command.advancedjobs.common.slot.primary")).getString();
            case SKILLS_BOARD -> TextUtil.tr("command.advancedjobs.guide.reason.skills",
                Component.translatable(secondary ? "command.advancedjobs.common.slot.secondary" : "command.advancedjobs.common.slot.primary")).getString();
            case STATUS_BOARD -> TextUtil.tr("command.advancedjobs.guide.reason.status",
                Component.translatable(secondary ? "command.advancedjobs.common.slot.secondary" : "command.advancedjobs.common.slot.primary")).getString();
            default -> null;
        };
    }

    private double pendingSalaryForSlot(PlayerJobProfile profile, boolean secondary) {
        String jobId = secondary ? profile.secondaryJobId() : profile.activeJobId();
        return jobId == null ? 0.0D : profile.progress(jobId).pendingSalary();
    }

    private String starterFocusLabel(PlayerJobProfile profile, String jobId) {
        var progress = profile.progress(jobId);
        if (progress.level() < 3) {
            return TextUtil.tr("message.advancedjobs.first_hour.level", 3).getString();
        }
        if (profile.availableSkillPoints(jobId) > 0) {
            return TextUtil.tr("message.advancedjobs.first_hour.skills", profile.availableSkillPoints(jobId)).getString();
        }
        long remainingDaily = progress.dailyTasks().stream().filter(task -> !task.completed()).count();
        if (remainingDaily > 0) {
            return TextUtil.tr("message.advancedjobs.first_hour.daily", remainingDaily).getString();
        }
        long remainingContracts = progress.contracts().stream().filter(contract -> !contract.completed()).count();
        if (remainingContracts > 0) {
            return TextUtil.tr("message.advancedjobs.first_hour.contracts", remainingContracts).getString();
        }
        return TextUtil.tr("message.advancedjobs.first_hour.progression").getString();
    }

    private String starterFocusCommand(PlayerJobProfile profile, String jobId, boolean secondary) {
        var progress = profile.progress(jobId);
        if (progress.level() < 3) {
            return secondary ? "/jobs stats secondary" : "/jobs stats";
        }
        if (profile.availableSkillPoints(jobId) > 0) {
            return secondary ? "/jobs skills secondary" : "/jobs skills";
        }
        if (progress.dailyTasks().stream().anyMatch(task -> !task.completed())) {
            return secondary ? "/jobs daily secondary" : "/jobs daily";
        }
        if (progress.contracts().stream().anyMatch(contract -> !contract.completed())) {
            return secondary ? "/jobs contracts secondary" : "/jobs contracts";
        }
        return secondary ? "/jobs guide secondary" : "/jobs guide";
    }

    private int nearbyBoardCount(ServerPlayer player, NpcRole role) {
        var box = player.getBoundingBox().inflate(ROUTE_HINT_RADIUS);
        int count = 0;
        for (var entity : player.serverLevel().getEntities(player, box)) {
            if (!(entity instanceof Mob mob)) {
                continue;
            }
            if (matchesRole(mob, role)) {
                count++;
            }
        }
        return count;
    }

    private String nearestSource(ServerPlayer player, NpcRole role) {
        var box = player.getBoundingBox().inflate(ROUTE_HINT_RADIUS);
        double nearest = Double.MAX_VALUE;
        String nearestSource = "-";
        for (var entity : player.serverLevel().getEntities(player, box)) {
            if (!(entity instanceof Mob mob) || !matchesRole(mob, role)) {
                continue;
            }
            double distance = mob.distanceToSqr(player);
            if (distance < nearest) {
                nearest = distance;
                nearestSource = mob.getType() == nativeType(role) ? "native" : "wand";
            }
        }
        return nearestSource;
    }

    private net.minecraft.world.entity.EntityType<?> nativeType(NpcRole role) {
        return switch (role) {
            case JOBS_MASTER -> ModEntities.JOBS_MASTER.get();
            case DAILY_BOARD -> ModEntities.DAILY_BOARD.get();
            case STATUS_BOARD -> ModEntities.STATUS_BOARD.get();
            case CONTRACTS_BOARD -> ModEntities.CONTRACTS_BOARD.get();
            case SALARY_BOARD -> ModEntities.SALARY_BOARD.get();
            case SKILLS_BOARD -> ModEntities.SKILLS_BOARD.get();
            case LEADERBOARD_BOARD -> ModEntities.LEADERBOARD_BOARD.get();
            case HELP_BOARD -> ModEntities.HELP_BOARD.get();
        };
    }

    private boolean matchesRole(Mob mob, NpcRole role) {
        return switch (role) {
            case JOBS_MASTER -> mob.getType() == ModEntities.JOBS_MASTER.get()
                || mob.getPersistentData().getBoolean(JobEventHandler.JOB_NPC_TAG);
            case DAILY_BOARD -> mob.getType() == ModEntities.DAILY_BOARD.get()
                || mob.getPersistentData().getBoolean(JobEventHandler.DAILY_BOARD_TAG);
            case STATUS_BOARD -> mob.getType() == ModEntities.STATUS_BOARD.get()
                || mob.getPersistentData().getBoolean(JobEventHandler.STATUS_BOARD_TAG);
            case CONTRACTS_BOARD -> mob.getType() == ModEntities.CONTRACTS_BOARD.get()
                || mob.getPersistentData().getBoolean(JobEventHandler.CONTRACTS_BOARD_TAG);
            case SALARY_BOARD -> mob.getType() == ModEntities.SALARY_BOARD.get()
                || mob.getPersistentData().getBoolean(JobEventHandler.SALARY_BOARD_TAG);
            case SKILLS_BOARD -> mob.getType() == ModEntities.SKILLS_BOARD.get()
                || mob.getPersistentData().getBoolean(JobEventHandler.SKILLS_BOARD_TAG);
            case LEADERBOARD_BOARD -> mob.getType() == ModEntities.LEADERBOARD_BOARD.get()
                || mob.getPersistentData().getBoolean(JobEventHandler.LEADERBOARD_BOARD_TAG);
            case HELP_BOARD -> mob.getType() == ModEntities.HELP_BOARD.get()
                || mob.getPersistentData().getBoolean(JobEventHandler.HELP_BOARD_TAG);
        };
    }

    private void consume(PlayerInteractEvent.EntityInteract event) {
        event.setCancellationResult(InteractionResult.CONSUME);
        event.setCanceled(true);
    }
}
