package com.example.advancedjobs.entity;

import com.example.advancedjobs.AdvancedJobsMod;
import com.example.advancedjobs.content.ModEntities;
import com.example.advancedjobs.config.ConfigManager;
import com.example.advancedjobs.model.JobProgress;
import com.example.advancedjobs.model.PlayerJobProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class HelpBoardEntity extends Villager implements RoleBasedNpc {
    public HelpBoardEntity(EntityType<? extends Villager> entityType, Level level) {
        super(entityType, level);
        this.setVillagerData(new VillagerData(VillagerType.SWAMP, VillagerProfession.NITWIT, 5));
        this.setCustomName(roleLabel());
        this.setCustomNameVisible(true);
        this.setPersistenceRequired();
        this.setNoAi(true);
        this.setInvulnerable(true);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            PlayerJobProfile profile = AdvancedJobsMod.get().jobManager().getOrCreateProfile(serverPlayer);
            boolean useSecondary = hand == InteractionHand.OFF_HAND
                && ConfigManager.COMMON.allowSecondaryJob.get()
                && profile.secondaryJobId() != null;
            String targetJobId = useSecondary ? profile.secondaryJobId() : profile.activeJobId();
            sendHelpHint(serverPlayer, profile, useSecondary, targetJobId);
            if (serverPlayer.isShiftKeyDown()) {
                AdvancedJobsMod.get().jobManager().openScreen(serverPlayer, "jobs", targetJobId);
            } else {
                AdvancedJobsMod.get().jobManager().openScreen(serverPlayer, "help", targetJobId);
            }
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean canBreed() {
        return false;
    }

    @Override
    public boolean canBeLeashed(Player player) {
        return false;
    }

    @Override
    public NpcRole npcRole() {
        return NpcRole.HELP_BOARD;
    }

    private void sendHelpHint(ServerPlayer player, PlayerJobProfile profile, boolean secondary, String targetJobId) {
        final int radius = 16;
        player.sendSystemMessage(Component.translatable("message.advancedjobs.help_board.slot_hint",
            Component.translatable(secondary
                ? "command.advancedjobs.common.slot.secondary"
                : "command.advancedjobs.common.slot.primary")));
        if (targetJobId == null) {
            player.sendSystemMessage(Component.translatable("message.advancedjobs.help_board.no_job"));
        } else {
            player.sendSystemMessage(Component.translatable("message.advancedjobs.help_board.job_hint",
                Component.translatable("job.advancedjobs." + targetJobId)));
        }
        player.sendSystemMessage(Component.translatable("message.advancedjobs.help_board.salary_hint",
            Component.translatable(ConfigManager.COMMON.instantSalary.get()
                ? "gui.advancedjobs.salary_mode.instant"
                : "gui.advancedjobs.salary_mode.manual")));
        NpcRole nextRole = recommendedRole(profile, secondary);
        player.sendSystemMessage(Component.translatable("message.advancedjobs.help_board.next_step",
            Component.translatable(nextRole.translationKey()),
            guideReason(profile, nextRole, secondary)));
        int nextRoleCount = nearbyBoardCount(player, nextRole, radius);
        if (nextRoleCount <= 0) {
            player.sendSystemMessage(Component.translatable("message.advancedjobs.help_board.next_step_missing",
                Component.translatable(nextRole.translationKey()),
                radius));
        } else {
            String nextRoleSource = nearestSource(player, nextRole, radius);
            player.sendSystemMessage(Component.translatable("message.advancedjobs.help_board.next_step_available",
                Component.translatable(nextRole.translationKey()),
                nextRoleCount,
                radius,
                nextRoleSource));
        }
        NpcRole followUpRole = secondaryRecommendedRole(profile, nextRole);
        if (followUpRole != null) {
            int followUpCount = nearbyBoardCount(player, followUpRole, radius);
            if (followUpCount <= 0) {
                player.sendSystemMessage(Component.translatable("message.advancedjobs.help_board.follow_up_missing",
                    Component.translatable(followUpRole.translationKey()),
                    radius));
            } else {
                String followUpSource = nearestSource(player, followUpRole, radius);
                player.sendSystemMessage(Component.translatable("message.advancedjobs.help_board.follow_up_available",
                    Component.translatable(followUpRole.translationKey()),
                    followUpCount,
                    radius,
                    followUpSource));
            }
        }
        player.sendSystemMessage(Component.translatable("message.advancedjobs.help_board.hub_hint",
            nearbyBoardCount(player, NpcRole.JOBS_MASTER, radius),
            nearbyBoardCount(player, NpcRole.DAILY_BOARD, radius),
            nearbyBoardCount(player, NpcRole.CONTRACTS_BOARD, radius),
            nearbyBoardCount(player, NpcRole.SALARY_BOARD, radius),
            nearbyBoardCount(player, NpcRole.SKILLS_BOARD, radius),
            nearbyBoardCount(player, NpcRole.LEADERBOARD_BOARD, radius),
            nearbyBoardCount(player, NpcRole.STATUS_BOARD, radius)));
    }

    private NpcRole recommendedRole(PlayerJobProfile profile, boolean secondary) {
        String jobId = secondary ? profile.secondaryJobId() : profile.activeJobId();
        if (jobId == null) {
            return NpcRole.JOBS_MASTER;
        }
        JobProgress progress = profile.progress(jobId);
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

    private String guideReason(PlayerJobProfile profile, NpcRole role, boolean secondary) {
        String slotKey = secondary
            ? "command.advancedjobs.common.slot.secondary"
            : "command.advancedjobs.common.slot.primary";
        return switch (role) {
            case JOBS_MASTER -> Component.translatable("command.advancedjobs.guide.reason.jobs_master",
                Component.translatable(slotKey)).getString();
            case SALARY_BOARD -> Component.translatable("command.advancedjobs.guide.reason.salary",
                com.example.advancedjobs.util.TextUtil.fmt2(pendingSalaryForSlot(profile, secondary))).getString();
            case DAILY_BOARD -> Component.translatable("command.advancedjobs.guide.reason.daily",
                Component.translatable(slotKey)).getString();
            case CONTRACTS_BOARD -> Component.translatable("command.advancedjobs.guide.reason.contracts",
                Component.translatable(slotKey)).getString();
            case SKILLS_BOARD -> Component.translatable("command.advancedjobs.guide.reason.skills",
                Component.translatable(slotKey)).getString();
            case STATUS_BOARD -> Component.translatable("command.advancedjobs.guide.reason.status",
                Component.translatable(slotKey)).getString();
            default -> Component.empty().getString();
        };
    }

    private double pendingSalaryForSlot(PlayerJobProfile profile, boolean secondary) {
        String jobId = secondary ? profile.secondaryJobId() : profile.activeJobId();
        return jobId == null ? 0.0D : profile.progress(jobId).pendingSalary();
    }

    private int nearbyBoardCount(ServerPlayer player, NpcRole role, double radius) {
        var box = this.getBoundingBox().inflate(radius);
        int count = 0;
        for (var entity : player.serverLevel().getEntities(this, box)) {
            if (!(entity instanceof net.minecraft.world.entity.Mob mob)) {
                continue;
            }
            if (mob.distanceToSqr(this) > radius * radius) {
                continue;
            }
            if (matchesRole(mob, role)) {
                count++;
            }
        }
        return count;
    }

    private String nearestSource(ServerPlayer player, NpcRole role, double radius) {
        var box = this.getBoundingBox().inflate(radius);
        double nearest = Double.MAX_VALUE;
        String nearestSource = "-";
        for (var entity : player.serverLevel().getEntities(this, box)) {
            if (!(entity instanceof net.minecraft.world.entity.Mob mob)) {
                continue;
            }
            if (mob.distanceToSqr(this) > radius * radius || !matchesRole(mob, role)) {
                continue;
            }
            double distance = mob.distanceToSqr(this);
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

    private boolean matchesRole(net.minecraft.world.entity.Mob mob, NpcRole role) {
        return switch (role) {
            case JOBS_MASTER -> mob.getType() == ModEntities.JOBS_MASTER.get()
                || mob.getPersistentData().getBoolean(com.example.advancedjobs.event.JobEventHandler.JOB_NPC_TAG);
            case DAILY_BOARD -> mob.getType() == ModEntities.DAILY_BOARD.get()
                || mob.getPersistentData().getBoolean(com.example.advancedjobs.event.JobEventHandler.DAILY_BOARD_TAG);
            case STATUS_BOARD -> mob.getType() == ModEntities.STATUS_BOARD.get()
                || mob.getPersistentData().getBoolean(com.example.advancedjobs.event.JobEventHandler.STATUS_BOARD_TAG);
            case CONTRACTS_BOARD -> mob.getType() == ModEntities.CONTRACTS_BOARD.get()
                || mob.getPersistentData().getBoolean(com.example.advancedjobs.event.JobEventHandler.CONTRACTS_BOARD_TAG);
            case SALARY_BOARD -> mob.getType() == ModEntities.SALARY_BOARD.get()
                || mob.getPersistentData().getBoolean(com.example.advancedjobs.event.JobEventHandler.SALARY_BOARD_TAG);
            case SKILLS_BOARD -> mob.getType() == ModEntities.SKILLS_BOARD.get()
                || mob.getPersistentData().getBoolean(com.example.advancedjobs.event.JobEventHandler.SKILLS_BOARD_TAG);
            case LEADERBOARD_BOARD -> mob.getType() == ModEntities.LEADERBOARD_BOARD.get()
                || mob.getPersistentData().getBoolean(com.example.advancedjobs.event.JobEventHandler.LEADERBOARD_BOARD_TAG);
            case HELP_BOARD -> mob.getType() == ModEntities.HELP_BOARD.get()
                || mob.getPersistentData().getBoolean(com.example.advancedjobs.event.JobEventHandler.HELP_BOARD_TAG);
        };
    }
}
