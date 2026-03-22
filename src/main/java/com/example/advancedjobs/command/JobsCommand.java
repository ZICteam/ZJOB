package com.example.advancedjobs.command;

import com.example.advancedjobs.AdvancedJobsMod;
import com.example.advancedjobs.content.ModEntities;
import com.example.advancedjobs.config.ConfigManager;
import com.example.advancedjobs.entity.NpcRole;
import com.example.advancedjobs.model.ContractHistoryEntry;
import com.example.advancedjobs.model.ContractProgress;
import com.example.advancedjobs.model.DailyTaskHistoryEntry;
import com.example.advancedjobs.model.DailyTaskProgress;
import com.example.advancedjobs.model.JobDefinition;
import com.example.advancedjobs.model.JobProgress;
import com.example.advancedjobs.model.PlayerJobProfile;
import com.example.advancedjobs.model.SkillBranch;
import com.example.advancedjobs.model.SkillNode;
import com.example.advancedjobs.util.ResourceLocationUtil;
import com.example.advancedjobs.util.TextUtil;
import com.example.advancedjobs.util.TimeUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

public final class JobsCommand {
    private static final com.mojang.brigadier.suggestion.SuggestionProvider<CommandSourceStack> NPC_ROLE_SUGGESTIONS =
        (context, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
            java.util.Arrays.stream(NpcRole.values()).map(NpcRole::id), builder);

    private JobsCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("jobs")
            .executes(ctx -> open(ctx.getSource().getPlayerOrException()))
            .then(Commands.literal("help").executes(ctx -> help(ctx.getSource().getPlayerOrException())))
            .then(Commands.literal("navigate")
                .executes(ctx -> navigate(ctx.getSource().getPlayerOrException(), false, 32))
                .then(Commands.literal("all")
                    .executes(ctx -> navigateAll(ctx.getSource().getPlayerOrException(), 32))
                    .then(Commands.argument("radius", IntegerArgumentType.integer(4, 128))
                        .executes(ctx -> navigateAll(ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "radius")))))
                .then(Commands.literal("secondary")
                    .executes(ctx -> navigate(ctx.getSource().getPlayerOrException(), true, 32))
                    .then(Commands.argument("radius", IntegerArgumentType.integer(4, 128))
                        .executes(ctx -> navigate(ctx.getSource().getPlayerOrException(), true, IntegerArgumentType.getInteger(ctx, "radius")))))
                .then(Commands.argument("radius", IntegerArgumentType.integer(4, 128))
                    .executes(ctx -> navigate(ctx.getSource().getPlayerOrException(), false, IntegerArgumentType.getInteger(ctx, "radius")))))
            .then(Commands.literal("guide")
                .executes(ctx -> guide(ctx.getSource().getPlayerOrException(), false, 32))
                .then(Commands.literal("all")
                    .executes(ctx -> guideAll(ctx.getSource().getPlayerOrException(), 32))
                    .then(Commands.argument("radius", IntegerArgumentType.integer(4, 128))
                        .executes(ctx -> guideAll(ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "radius")))))
                .then(Commands.literal("secondary")
                    .executes(ctx -> guide(ctx.getSource().getPlayerOrException(), true, 32))
                    .then(Commands.argument("radius", IntegerArgumentType.integer(4, 128))
                        .executes(ctx -> guide(ctx.getSource().getPlayerOrException(), true, IntegerArgumentType.getInteger(ctx, "radius")))))
                .then(Commands.argument("radius", IntegerArgumentType.integer(4, 128))
                    .executes(ctx -> guide(ctx.getSource().getPlayerOrException(), false, IntegerArgumentType.getInteger(ctx, "radius")))))
            .then(Commands.literal("where")
                .executes(ctx -> where(ctx.getSource().getPlayerOrException(), 24))
                .then(Commands.literal("ready")
                    .executes(ctx -> whereReady(ctx.getSource().getPlayerOrException(), false, 24))
                    .then(Commands.literal("all")
                        .executes(ctx -> whereReadyAll(ctx.getSource().getPlayerOrException(), 24))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(4, 128))
                            .executes(ctx -> whereReadyAll(ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "radius")))))
                    .then(Commands.literal("secondary")
                        .executes(ctx -> whereReady(ctx.getSource().getPlayerOrException(), true, 24))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(4, 128))
                            .executes(ctx -> whereReady(ctx.getSource().getPlayerOrException(), true, IntegerArgumentType.getInteger(ctx, "radius")))))
                    .then(Commands.argument("radius", IntegerArgumentType.integer(4, 128))
                        .executes(ctx -> whereReady(ctx.getSource().getPlayerOrException(), false, IntegerArgumentType.getInteger(ctx, "radius")))))
                .then(Commands.literal("native")
                    .executes(ctx -> whereBySource(ctx.getSource().getPlayerOrException(), "native", 24))
                    .then(Commands.argument("radius", IntegerArgumentType.integer(4, 128))
                        .executes(ctx -> whereBySource(ctx.getSource().getPlayerOrException(), "native", IntegerArgumentType.getInteger(ctx, "radius")))))
                .then(Commands.literal("wand")
                    .executes(ctx -> whereBySource(ctx.getSource().getPlayerOrException(), "wand", 24))
                    .then(Commands.argument("radius", IntegerArgumentType.integer(4, 128))
                        .executes(ctx -> whereBySource(ctx.getSource().getPlayerOrException(), "wand", IntegerArgumentType.getInteger(ctx, "radius")))))
                .then(Commands.literal("summary")
                    .executes(ctx -> whereSummary(ctx.getSource().getPlayerOrException(), 24))
                    .then(Commands.literal("all")
                        .executes(ctx -> whereSummaryAll(ctx.getSource().getPlayerOrException(), 24))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(4, 128))
                            .executes(ctx -> whereSummaryAll(ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "radius")))))
                    .then(Commands.literal("secondary")
                        .executes(ctx -> whereSummary(ctx.getSource().getPlayerOrException(), true, 24))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(4, 128))
                            .executes(ctx -> whereSummary(ctx.getSource().getPlayerOrException(), true, IntegerArgumentType.getInteger(ctx, "radius")))))
                    .then(Commands.argument("radius", IntegerArgumentType.integer(4, 128))
                        .executes(ctx -> whereSummary(ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "radius")))))
                .then(Commands.literal("missing")
                    .executes(ctx -> whereMissing(ctx.getSource().getPlayerOrException(), 24))
                    .then(Commands.argument("radius", IntegerArgumentType.integer(4, 128))
                        .executes(ctx -> whereMissing(ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "radius")))))
                .then(Commands.argument("role", StringArgumentType.word()).suggests(NPC_ROLE_SUGGESTIONS)
                    .executes(ctx -> whereRole(ctx.getSource().getPlayerOrException(),
                        StringArgumentType.getString(ctx, "role"), 24))
                    .then(Commands.argument("radius", IntegerArgumentType.integer(4, 128))
                        .executes(ctx -> whereRole(ctx.getSource().getPlayerOrException(),
                            StringArgumentType.getString(ctx, "role"),
                            IntegerArgumentType.getInteger(ctx, "radius")))))
                .then(Commands.argument("radius", IntegerArgumentType.integer(4, 128))
                    .executes(ctx -> where(ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "radius")))))
            .then(Commands.literal("info")
                .executes(ctx -> info(ctx.getSource().getPlayerOrException(), false))
                .then(Commands.literal("secondary").executes(ctx -> info(ctx.getSource().getPlayerOrException(), true))))
            .then(Commands.literal("stats")
                .executes(ctx -> stats(ctx.getSource().getPlayerOrException(), false))
                .then(Commands.literal("secondary").executes(ctx -> stats(ctx.getSource().getPlayerOrException(), true))))
            .then(Commands.literal("salary").executes(ctx -> salary(ctx.getSource().getPlayerOrException())))
            .then(Commands.literal("leave")
                .executes(ctx -> leave(ctx.getSource().getPlayerOrException(), false))
                .then(Commands.literal("secondary").executes(ctx -> leave(ctx.getSource().getPlayerOrException(), true))))
            .then(Commands.literal("top")
                .executes(ctx -> topOverall(ctx.getSource()))
                .then(Commands.argument("job", StringArgumentType.word())
                    .executes(ctx -> top(ctx.getSource(), StringArgumentType.getString(ctx, "job")))))
            .then(Commands.literal("daily")
                .executes(ctx -> daily(ctx.getSource().getPlayerOrException(), false))
                .then(Commands.literal("secondary").executes(ctx -> daily(ctx.getSource().getPlayerOrException(), true))))
            .then(Commands.literal("contracts")
                .executes(ctx -> contracts(ctx.getSource().getPlayerOrException(), false))
                .then(Commands.literal("secondary").executes(ctx -> contracts(ctx.getSource().getPlayerOrException(), true)))
                .then(Commands.literal("reroll")
                    .executes(ctx -> rerollContracts(ctx.getSource().getPlayerOrException(), false))
                    .then(Commands.literal("secondary").executes(ctx -> rerollContracts(ctx.getSource().getPlayerOrException(), true)))))
            .then(Commands.literal("skills")
                .executes(ctx -> skills(ctx.getSource().getPlayerOrException(), false))
                .then(Commands.literal("secondary").executes(ctx -> skills(ctx.getSource().getPlayerOrException(), true))))
            .then(Commands.literal("titles").executes(ctx -> titles(ctx.getSource().getPlayerOrException())))
            .then(Commands.literal("milestones")
                .executes(ctx -> milestones(ctx.getSource().getPlayerOrException(), false))
                .then(Commands.literal("secondary").executes(ctx -> milestones(ctx.getSource().getPlayerOrException(), true))))
            .then(Commands.literal("choose")
                .then(Commands.argument("job", StringArgumentType.word())
                    .executes(ctx -> choose(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "job"), false))
                    .then(Commands.literal("secondary")
                        .executes(ctx -> choose(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "job"), true))))));
    }

    private static int open(ServerPlayer player) {
        AdvancedJobsMod.get().jobManager().openScreen(player);
        return info(player, false);
    }

    private static int info(ServerPlayer player, boolean secondary) {
        PlayerJobProfile profile = AdvancedJobsMod.get().jobManager().getOrCreateProfile(player);
        String viewedJobId = selectedSlotJobId(profile, secondary);
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.info.header"));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.info.primary", jobName(profile.activeJobId())));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.info.secondary", jobName(profile.secondaryJobId())));
        appendJobSlotSummary(player, profile, profile.activeJobId(), "command.advancedjobs.info.primary_details");
        appendJobSlotSummary(player, profile, profile.secondaryJobId(), "command.advancedjobs.info.secondary_details");
        if (viewedJobId != null) {
            JobProgress activeProgress = profile.progress(viewedJobId);
            player.sendSystemMessage(TextUtil.tr("command.advancedjobs.info.slot_view", slotLabel(secondary), jobName(viewedJobId)));
            player.sendSystemMessage(TextUtil.tr("command.advancedjobs.info.title",
                professionTitle(viewedJobId, activeProgress.level())));
            player.sendSystemMessage(TextUtil.tr("command.advancedjobs.info.next_passive",
                nextPassiveUnlock(viewedJobId, activeProgress.level())));
            player.sendSystemMessage(TextUtil.tr("command.advancedjobs.info.next_skill_point",
                nextSkillPointLevel(activeProgress.level())));
            player.sendSystemMessage(TextUtil.tr("command.advancedjobs.info.daily_cycle",
                TimeUtil.formatRemainingSeconds(Math.max(0L, nextDailyReset(activeProgress) - TimeUtil.now())),
                TimeUtil.formatRemainingSeconds(Math.max(0L, nextContractRotation(activeProgress) - TimeUtil.now()))));
        } else if (secondary) {
            player.sendSystemMessage(TextUtil.tr("command.advancedjobs.error.no_slot_job"));
        }
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.info.unlocked_titles",
            profile.unlockedTitles().size(), latestUnlockedTitle(profile)));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.info.balance", TextUtil.fmt2(AdvancedJobsMod.get().jobManager().economy().getBalance(player.getUUID()))));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.info.salary_mode",
            TextUtil.tr(ConfigManager.COMMON.instantSalary.get() ? "gui.advancedjobs.salary_mode.instant" : "gui.advancedjobs.salary_mode.manual")));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.info.change",
            TextUtil.fmt2(ConfigManager.COMMON.jobChangePrice.get()),
            TimeUtil.formatRemainingSeconds(Math.max(0L, ConfigManager.COMMON.jobChangeCooldownSeconds.get() - (TimeUtil.now() - profile.lastJobChangeEpochSecond())))));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.info.salary_rules",
            TextUtil.fmt2(ConfigManager.COMMON.salaryTaxRate.get() * 100.0D),
            TextUtil.fmt2(ConfigManager.COMMON.maxSalaryPerClaim.get()),
            TimeUtil.formatRemainingSeconds(ConfigManager.COMMON.salaryClaimIntervalSeconds.get())));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.info.economy_context",
            ConfigManager.economy().providerId(), ConfigManager.economy().externalCurrencyId()));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.info.tax_sink_account",
            ConfigManager.economy().taxSinkAccountUuid()));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.info.reward_context",
            targetName(player.serverLevel().dimension().location()),
            TextUtil.fmt2(currentWorldMultiplier(player)),
            targetName(currentBiomeId(player)),
            TextUtil.fmt2(currentBiomeMultiplier(player)),
            TextUtil.fmt2(ConfigManager.economy().vipMultiplier()),
            TextUtil.fmt2(ConfigManager.economy().eventMultiplier()),
            TextUtil.fmt2(effectiveRewardMultiplier(player))));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.info.mob_filters",
            yesNo(ConfigManager.COMMON.blockArtificialMobRewards.get()),
            yesNo(ConfigManager.COMMON.blockBabyMobRewards.get()),
            yesNo(ConfigManager.COMMON.blockTamedMobRewards.get())));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.info.loot_chunk_filters",
            TimeUtil.formatRemainingSeconds(ConfigManager.COMMON.lootContainerRewardCooldownSeconds.get()),
            TimeUtil.formatRemainingSeconds(ConfigManager.COMMON.exploredChunkRewardCooldownSeconds.get())));
        appendActiveEffects(player);
        return 1;
    }

    private static int help(ServerPlayer player) {
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.help.header"));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.help.open"));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.help.navigate"));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.help.navigate_all"));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.help.navigate_secondary"));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.help.guide"));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.help.guide_all"));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.help.guide_secondary"));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.help.where"));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.help.where_ready"));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.help.where_ready_all"));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.help.where_ready_secondary"));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.help.where_native"));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.help.where_wand"));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.help.where_role"));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.help.where_summary"));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.help.where_missing"));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.help.choose"));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.help.secondary"));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.help.secondary_support"));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.help.progress"));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.help.salary"));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.help.salary_mode",
            TextUtil.tr(ConfigManager.COMMON.instantSalary.get() ? "gui.advancedjobs.salary_mode.instant" : "gui.advancedjobs.salary_mode.manual")));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.help.salary_board_quick_action"));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.help.economy_context",
            ConfigManager.economy().providerId(), ConfigManager.economy().externalCurrencyId()));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.help.mob_filters",
            yesNo(ConfigManager.COMMON.blockArtificialMobRewards.get()),
            yesNo(ConfigManager.COMMON.blockBabyMobRewards.get()),
            yesNo(ConfigManager.COMMON.blockTamedMobRewards.get())));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.help.loot_chunk_filters",
            TimeUtil.formatRemainingSeconds(ConfigManager.COMMON.lootContainerRewardCooldownSeconds.get()),
            TimeUtil.formatRemainingSeconds(ConfigManager.COMMON.exploredChunkRewardCooldownSeconds.get())));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.help.tax_sink_account",
            ConfigManager.economy().taxSinkAccountUuid()));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.help.event_status",
            TextUtil.fmt2(ConfigManager.economy().eventMultiplier()),
            TimeUtil.formatRemainingSeconds(Math.max(0L, ConfigManager.economy().eventEndsAtEpochSecond() - TimeUtil.now()))));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.help.daily"));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.help.daily_board_quick_action"));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.help.status_board_quick_action"));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.help.contracts"));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.help.contract_board",
            TextUtil.fmt2(ConfigManager.COMMON.contractRerollPrice.get()),
            TimeUtil.formatRemainingSeconds(ConfigManager.COMMON.contractRerollCooldownSeconds.get())));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.help.contract_board_quick_action"));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.help.jobs_master_quick_action"));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.help.help_board_quick_action"));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.help.skills_board_quick_action"));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.help.skills"));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.help.titles"));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.help.milestones"));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.help.leaderboard_board_quick_action"));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.help.wand_quick_action"));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.help.top"));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.help.leave"));
        return 1;
    }

    private static int navigate(ServerPlayer player, boolean secondary, int radius) {
        PlayerJobProfile profile = AdvancedJobsMod.get().jobManager().getOrCreateProfile(player);
        NpcRole recommendedRole = recommendedRole(profile, secondary);
        String nearest = nearestDistanceForRole(player, recommendedRole, radius);
        String nearestSource = nearestSourceForRole(player, recommendedRole, radius);
        int readyCount = readyRoles(profile, secondary).size();
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.navigate.header", slotLabel(secondary), radius));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.navigate.line",
            TextUtil.tr(recommendedRole.translationKey()),
            nearest,
            readyCount,
            nearestSource));
        String reason = guideReason(profile, recommendedRole, secondary);
        if (reason != null) {
            player.sendSystemMessage(TextUtil.tr("command.advancedjobs.navigate.reason", reason));
        }
        if ("-".equals(nearest)) {
            player.sendSystemMessage(TextUtil.tr("command.advancedjobs.guide.missing_hint", radius));
        }
        NpcRole followUpRole = secondaryRecommendedRole(profile, recommendedRole);
        if (followUpRole != null) {
            String followUpNearest = nearestDistanceForRole(player, followUpRole, radius);
            String followUpSource = nearestSourceForRole(player, followUpRole, radius);
            player.sendSystemMessage(TextUtil.tr("command.advancedjobs.navigate.follow_up",
                TextUtil.tr(followUpRole.translationKey()),
                followUpNearest,
                followUpSource));
            if ("-".equals(followUpNearest)) {
                player.sendSystemMessage(TextUtil.tr("command.advancedjobs.navigate.follow_up_missing",
                    TextUtil.tr(followUpRole.translationKey())));
                player.sendSystemMessage(TextUtil.tr("command.advancedjobs.guide.missing_hint", radius));
            }
        }
        return 1;
    }

    private static int navigateAll(ServerPlayer player, int radius) {
        PlayerJobProfile profile = AdvancedJobsMod.get().jobManager().getOrCreateProfile(player);
        int totalSlots = 1;
        int nearbyReadySlots = "-".equals(nearestDistanceForRole(player, recommendedRole(profile, false), radius)) ? 0 : 1;
        if (profile.secondaryJobId() != null || ConfigManager.COMMON.allowSecondaryJob.get()) {
            totalSlots++;
            if (!"-".equals(nearestDistanceForRole(player, recommendedRole(profile, true), radius))) {
                nearbyReadySlots++;
            }
        }
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.navigate.all_header", radius));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.navigate.all_summary", nearbyReadySlots, totalSlots));
        player.sendSystemMessage(TextUtil.tr(
            nearbyReadySlots == totalSlots
                ? "command.advancedjobs.navigate.all_ready"
                : "command.advancedjobs.navigate.all_missing",
            radius));
        navigate(player, false, radius);
        if (profile.secondaryJobId() != null || ConfigManager.COMMON.allowSecondaryJob.get()) {
            player.sendSystemMessage(TextUtil.tr("command.advancedjobs.navigate.all_separator"));
            navigate(player, true, radius);
        }
        return 1;
    }

    private static int guide(ServerPlayer player, boolean secondary, int radius) {
        PlayerJobProfile profile = AdvancedJobsMod.get().jobManager().getOrCreateProfile(player);
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.guide.header", slotLabel(secondary), radius));

        NpcRole recommendedRole = recommendedRole(profile, secondary);
        String recommendedDistance = nearestDistanceForRole(player, recommendedRole, radius);
        String recommendedSource = nearestSourceForRole(player, recommendedRole, radius);
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.guide.primary",
            TextUtil.tr(recommendedRole.translationKey()),
            recommendedDistance,
            recommendedSource));
        if ("-".equals(recommendedDistance)) {
            player.sendSystemMessage(TextUtil.tr("command.advancedjobs.guide.missing",
                TextUtil.tr(recommendedRole.translationKey())));
            player.sendSystemMessage(TextUtil.tr("command.advancedjobs.guide.missing_hint", radius));
        }

        String reason = guideReason(profile, recommendedRole, secondary);
        if (reason != null) {
            player.sendSystemMessage(Component.literal(reason));
        }

        NpcRole secondaryRole = secondaryRecommendedRole(profile, recommendedRole);
        if (secondaryRole != null) {
            String secondaryDistance = nearestDistanceForRole(player, secondaryRole, radius);
            String secondarySource = nearestSourceForRole(player, secondaryRole, radius);
            player.sendSystemMessage(TextUtil.tr("command.advancedjobs.guide.secondary",
                TextUtil.tr(secondaryRole.translationKey()),
                secondaryDistance,
                secondarySource));
            if ("-".equals(secondaryDistance)) {
                player.sendSystemMessage(TextUtil.tr("command.advancedjobs.guide.secondary_missing",
                    TextUtil.tr(secondaryRole.translationKey())));
                player.sendSystemMessage(TextUtil.tr("command.advancedjobs.guide.missing_hint", radius));
            }
        }
        return 1;
    }

    private static int guideAll(ServerPlayer player, int radius) {
        PlayerJobProfile profile = AdvancedJobsMod.get().jobManager().getOrCreateProfile(player);
        int totalSlots = 1;
        int nearbyReadySlots = "-".equals(nearestDistanceForRole(player, recommendedRole(profile, false), radius)) ? 0 : 1;
        if (profile.secondaryJobId() != null || ConfigManager.COMMON.allowSecondaryJob.get()) {
            totalSlots++;
            if (!"-".equals(nearestDistanceForRole(player, recommendedRole(profile, true), radius))) {
                nearbyReadySlots++;
            }
        }
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.guide.all_header", radius));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.guide.all_summary", nearbyReadySlots, totalSlots));
        player.sendSystemMessage(TextUtil.tr(
            nearbyReadySlots == totalSlots
                ? "command.advancedjobs.guide.all_ready"
                : "command.advancedjobs.guide.all_missing",
            radius));
        guide(player, false, radius);
        if (profile.secondaryJobId() != null || ConfigManager.COMMON.allowSecondaryJob.get()) {
            player.sendSystemMessage(TextUtil.tr("command.advancedjobs.guide.all_separator"));
            guide(player, true, radius);
        }
        return 1;
    }

    private static int where(ServerPlayer player, int radius) {
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.where.header", radius));
        for (NpcRole role : NpcRole.values()) {
            int count = 0;
            double nearest = Double.MAX_VALUE;
            for (var entity : player.serverLevel().getEntities(null, player.getBoundingBox().inflate(radius))) {
                if (!(entity instanceof Mob mob)) {
                    continue;
                }
                if (!matchesRole(mob, role)) {
                    continue;
                }
                count++;
                nearest = Math.min(nearest, Math.sqrt(mob.distanceToSqr(player)));
            }
            player.sendSystemMessage(TextUtil.tr("command.advancedjobs.where.line",
                TextUtil.tr(role.translationKey()),
                count,
                count == 0 ? "-" : TextUtil.fmt2(nearest)));
        }
        return 1;
    }

    private static int whereRole(ServerPlayer player, String roleId, int radius) {
        NpcRole role = parseRole(roleId);
        if (role == null) {
            player.sendSystemMessage(TextUtil.tr("command.advancedjobs.where.invalid_role", roleId));
            return 0;
        }
        int count = 0;
        double nearest = Double.MAX_VALUE;
        String nearestSource = "-";
        for (var entity : player.serverLevel().getEntities(null, player.getBoundingBox().inflate(radius))) {
            if (!(entity instanceof Mob mob)) {
                continue;
            }
            if (!matchesRole(mob, role)) {
                continue;
            }
            count++;
            double distance = Math.sqrt(mob.distanceToSqr(player));
            if (distance < nearest) {
                nearest = distance;
                nearestSource = roleSource(mob);
            }
        }
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.where.role_header",
            TextUtil.tr(role.translationKey()), radius));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.where.role_line",
            count,
            count == 0 ? "-" : TextUtil.fmt2(nearest),
            nearestSource));
        return 1;
    }

    private static int whereMissing(ServerPlayer player, int radius) {
        int missing = 0;
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.where.missing_header", radius));
        for (NpcRole role : NpcRole.values()) {
            int count = 0;
            for (var entity : player.serverLevel().getEntities(null, player.getBoundingBox().inflate(radius))) {
                if (!(entity instanceof Mob mob)) {
                    continue;
                }
                if (matchesRole(mob, role)) {
                    count++;
                }
            }
            if (count == 0) {
                missing++;
                player.sendSystemMessage(TextUtil.tr("command.advancedjobs.where.missing_line",
                    TextUtil.tr(role.translationKey())));
            }
        }
        if (missing == 0) {
            player.sendSystemMessage(TextUtil.tr("command.advancedjobs.where.missing_none"));
        }
        return 1;
    }

    private static int whereSummary(ServerPlayer player, int radius) {
        return whereSummary(player, false, radius);
    }

    private static int whereSummary(ServerPlayer player, boolean secondary, int radius) {
        PlayerJobProfile profile = AdvancedJobsMod.get().jobManager().getOrCreateProfile(player);
        int nativeCount = 0;
        int fallbackCount = 0;
        int coveredRoles = 0;
        for (NpcRole role : NpcRole.values()) {
            boolean present = false;
            for (var entity : player.serverLevel().getEntities(null, player.getBoundingBox().inflate(radius))) {
                if (!(entity instanceof Mob mob)) {
                    continue;
                }
                if (!matchesRole(mob, role)) {
                    continue;
                }
                present = true;
                if ("native".equals(roleSource(mob))) {
                    nativeCount++;
                } else {
                    fallbackCount++;
                }
            }
            if (present) {
                coveredRoles++;
            }
        }
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.where.summary_header", radius));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.where.summary_line",
            coveredRoles,
            NpcRole.values().length,
            nativeCount,
            fallbackCount));
        NpcRole recommendedRole = recommendedRole(profile, secondary);
        String nearest = nearestDistanceForRole(player, recommendedRole, radius);
        String nearestSource = nearestSourceForRole(player, recommendedRole, radius);
        String reason = guideReason(profile, recommendedRole, secondary);
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.where.summary_recommended",
            slotLabel(secondary),
            TextUtil.tr(recommendedRole.translationKey()),
            nearest,
            nearestSource));
        if ("-".equals(nearest)) {
            player.sendSystemMessage(TextUtil.tr("command.advancedjobs.where.summary_recommended_missing",
                TextUtil.tr(recommendedRole.translationKey()),
                radius));
        } else {
            player.sendSystemMessage(TextUtil.tr("command.advancedjobs.where.summary_recommended_available",
                TextUtil.tr(recommendedRole.translationKey()),
                nearestSource));
        }
        if (reason != null) {
            player.sendSystemMessage(TextUtil.tr("command.advancedjobs.where.summary_reason", reason));
        }
        NpcRole followUpRole = secondaryRecommendedRole(profile, recommendedRole);
        if (followUpRole != null) {
            String followUpNearest = nearestDistanceForRole(player, followUpRole, radius);
            String followUpSource = nearestSourceForRole(player, followUpRole, radius);
            player.sendSystemMessage(TextUtil.tr("command.advancedjobs.where.summary_follow_up",
                TextUtil.tr(followUpRole.translationKey()),
                followUpNearest,
                followUpSource));
            if ("-".equals(followUpNearest)) {
                player.sendSystemMessage(TextUtil.tr("command.advancedjobs.where.summary_follow_up_missing",
                    TextUtil.tr(followUpRole.translationKey()),
                    radius));
            } else {
                player.sendSystemMessage(TextUtil.tr("command.advancedjobs.where.summary_follow_up_available",
                    TextUtil.tr(followUpRole.translationKey()),
                    followUpSource));
            }
        }
        if (coveredRoles < NpcRole.values().length) {
            player.sendSystemMessage(TextUtil.tr("command.advancedjobs.where.summary_hint"));
        }
        return 1;
    }

    private static int whereSummaryAll(ServerPlayer player, int radius) {
        PlayerJobProfile profile = AdvancedJobsMod.get().jobManager().getOrCreateProfile(player);
        int totalSlots = 1;
        int nearbyReadySlots = "-".equals(nearestDistanceForRole(player, recommendedRole(profile, false), radius)) ? 0 : 1;
        if (profile.secondaryJobId() != null || ConfigManager.COMMON.allowSecondaryJob.get()) {
            totalSlots++;
            if (!"-".equals(nearestDistanceForRole(player, recommendedRole(profile, true), radius))) {
                nearbyReadySlots++;
            }
        }
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.where.summary_all_header", radius));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.where.summary_all_coverage", nearbyReadySlots, totalSlots));
        player.sendSystemMessage(TextUtil.tr(
            nearbyReadySlots == totalSlots
                ? "command.advancedjobs.where.summary_all_ready"
                : "command.advancedjobs.where.summary_all_missing",
            radius));
        whereSummary(player, false, radius);
        if (profile.secondaryJobId() != null || ConfigManager.COMMON.allowSecondaryJob.get()) {
            player.sendSystemMessage(TextUtil.tr("command.advancedjobs.where.summary_all_separator"));
            whereSummary(player, true, radius);
        }
        return 1;
    }

    private static int whereBySource(ServerPlayer player, String source, int radius) {
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.where.source_header", source, radius));
        for (NpcRole role : NpcRole.values()) {
            int count = 0;
            double nearest = Double.MAX_VALUE;
            for (var entity : player.serverLevel().getEntities(null, player.getBoundingBox().inflate(radius))) {
                if (!(entity instanceof Mob mob)) {
                    continue;
                }
                if (!matchesRole(mob, role) || !source.equals(roleSource(mob))) {
                    continue;
                }
                count++;
                nearest = Math.min(nearest, Math.sqrt(mob.distanceToSqr(player)));
            }
            player.sendSystemMessage(TextUtil.tr("command.advancedjobs.where.line",
                TextUtil.tr(role.translationKey()),
                count,
                count == 0 ? "-" : TextUtil.fmt2(nearest)));
        }
        return 1;
    }

    private static int whereReady(ServerPlayer player, boolean secondary, int radius) {
        PlayerJobProfile profile = AdvancedJobsMod.get().jobManager().getOrCreateProfile(player);
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.where.ready_header", slotLabel(secondary), radius));
        int shown = 0;
        int nearbyReady = 0;
        for (NpcRole role : readyRoles(profile, secondary)) {
            int count = 0;
            double nearest = Double.MAX_VALUE;
            String nearestSource = "-";
            for (var entity : player.serverLevel().getEntities(null, player.getBoundingBox().inflate(radius))) {
                if (!(entity instanceof Mob mob)) {
                    continue;
                }
                if (!matchesRole(mob, role)) {
                    continue;
                }
                count++;
                double distance = Math.sqrt(mob.distanceToSqr(player));
                if (distance < nearest) {
                    nearest = distance;
                    nearestSource = roleSource(mob);
                }
            }
            player.sendSystemMessage(TextUtil.tr("command.advancedjobs.where.ready_line",
                TextUtil.tr(role.translationKey()),
                count,
                count == 0 ? "-" : TextUtil.fmt2(nearest),
                nearestSource));
            String reason = guideReason(profile, role, secondary);
            if (reason != null) {
                player.sendSystemMessage(TextUtil.tr("command.advancedjobs.where.ready_reason", reason));
            }
            if (count == 0) {
                player.sendSystemMessage(TextUtil.tr("command.advancedjobs.where.ready_missing",
                    TextUtil.tr(role.translationKey())));
            } else {
                nearbyReady++;
            }
            shown++;
        }
        if (shown == 0) {
            player.sendSystemMessage(TextUtil.tr("command.advancedjobs.where.ready_none", slotLabel(secondary)));
        } else {
            player.sendSystemMessage(TextUtil.tr("command.advancedjobs.where.ready_summary",
                nearbyReady,
                shown));
        }
        return 1;
    }

    private static int whereReadyAll(ServerPlayer player, int radius) {
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.where.ready_all_header", radius));
        PlayerJobProfile profile = AdvancedJobsMod.get().jobManager().getOrCreateProfile(player);
        int activeSlots = profile.activeJobId() != null ? 1 : 0;
        if (profile.secondaryJobId() != null) {
            activeSlots++;
        }
        int coveredSlots = nearbyReadyCoverage(profile, player, radius, false);
        if (profile.secondaryJobId() != null) {
            coveredSlots += nearbyReadyCoverage(profile, player, radius, true);
        }
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.where.ready_all_summary", coveredSlots, activeSlots));
        if (activeSlots > 0 && coveredSlots < activeSlots) {
            player.sendSystemMessage(TextUtil.tr("command.advancedjobs.where.ready_all_missing", radius));
        }
        whereReady(player, false, radius);
        if (profile.secondaryJobId() != null || ConfigManager.COMMON.allowSecondaryJob.get()) {
            player.sendSystemMessage(TextUtil.tr("command.advancedjobs.where.ready_all_separator"));
            whereReady(player, true, radius);
        }
        return 1;
    }

    private static NpcRole recommendedRole(PlayerJobProfile profile, boolean secondary) {
        String jobId = selectedSlotJobId(profile, secondary);
        if (jobId == null) {
            return NpcRole.JOBS_MASTER;
        }
        if (!ConfigManager.COMMON.instantSalary.get() && pendingSalaryForSlot(profile, secondary) > 0.0D) {
            return NpcRole.SALARY_BOARD;
        }
        if (hasIncompleteDaily(profile, secondary)) {
            return NpcRole.DAILY_BOARD;
        }
        if (hasIncompleteContracts(profile, secondary)) {
            return NpcRole.CONTRACTS_BOARD;
        }
        if (hasSpendableSkillPoints(profile, secondary)) {
            return NpcRole.SKILLS_BOARD;
        }
        return NpcRole.STATUS_BOARD;
    }

    private static NpcRole secondaryRecommendedRole(PlayerJobProfile profile, NpcRole primary) {
        if (primary != NpcRole.LEADERBOARD_BOARD && profile.activeJobId() != null) {
            return NpcRole.LEADERBOARD_BOARD;
        }
        if (primary != NpcRole.HELP_BOARD) {
            return NpcRole.HELP_BOARD;
        }
        return null;
    }

    private static java.util.List<NpcRole> readyRoles(PlayerJobProfile profile, boolean secondary) {
        java.util.LinkedHashSet<NpcRole> roles = new java.util.LinkedHashSet<>();
        String jobId = selectedSlotJobId(profile, secondary);
        if (jobId == null) {
            roles.add(NpcRole.JOBS_MASTER);
        }
        if (!ConfigManager.COMMON.instantSalary.get() && pendingSalaryForSlot(profile, secondary) > 0.0D) {
            roles.add(NpcRole.SALARY_BOARD);
        }
        if (hasIncompleteDaily(profile, secondary)) {
            roles.add(NpcRole.DAILY_BOARD);
        }
        if (hasIncompleteContracts(profile, secondary)) {
            roles.add(NpcRole.CONTRACTS_BOARD);
        }
        if (hasSpendableSkillPoints(profile, secondary)) {
            roles.add(NpcRole.SKILLS_BOARD);
        }
        if (!roles.isEmpty()) {
            roles.add(NpcRole.STATUS_BOARD);
        }
        return new java.util.ArrayList<>(roles);
    }

    private static int nearbyReadyCoverage(PlayerJobProfile profile, ServerPlayer player, int radius, boolean secondary) {
        String jobId = selectedSlotJobId(profile, secondary);
        if (jobId == null) {
            return 0;
        }
        for (NpcRole role : readyRoles(profile, secondary)) {
            if (!"-".equals(nearestDistanceForRole(player, role, radius))) {
                return 1;
            }
        }
        return 0;
    }

    private static String guideReason(PlayerJobProfile profile, NpcRole role, boolean secondary) {
        return switch (role) {
            case JOBS_MASTER -> TextUtil.tr("command.advancedjobs.guide.reason.jobs_master", slotLabel(secondary)).getString();
            case SALARY_BOARD -> TextUtil.tr("command.advancedjobs.guide.reason.salary",
                TextUtil.fmt2(pendingSalaryForSlot(profile, secondary))).getString();
            case DAILY_BOARD -> TextUtil.tr("command.advancedjobs.guide.reason.daily", slotLabel(secondary)).getString();
            case CONTRACTS_BOARD -> TextUtil.tr("command.advancedjobs.guide.reason.contracts", slotLabel(secondary)).getString();
            case SKILLS_BOARD -> TextUtil.tr("command.advancedjobs.guide.reason.skills", slotLabel(secondary)).getString();
            case STATUS_BOARD -> TextUtil.tr("command.advancedjobs.guide.reason.status", slotLabel(secondary)).getString();
            default -> null;
        };
    }

    private static boolean hasIncompleteDaily(PlayerJobProfile profile, boolean secondary) {
        String jobId = selectedSlotJobId(profile, secondary);
        return jobId != null && profile.progress(jobId).dailyTasks().stream().anyMatch(task -> !task.completed());
    }

    private static boolean hasIncompleteContracts(PlayerJobProfile profile, boolean secondary) {
        String jobId = selectedSlotJobId(profile, secondary);
        return jobId != null && profile.progress(jobId).contracts().stream().anyMatch(task -> !task.completed());
    }

    private static boolean hasSpendableSkillPoints(PlayerJobProfile profile, boolean secondary) {
        String jobId = selectedSlotJobId(profile, secondary);
        return jobId != null && profile.availableSkillPoints(jobId) > 0;
    }

    private static String nearestDistanceForRole(ServerPlayer player, NpcRole role, int radius) {
        double nearest = Double.MAX_VALUE;
        for (var entity : player.serverLevel().getEntities(null, player.getBoundingBox().inflate(radius))) {
            if (!(entity instanceof Mob mob)) {
                continue;
            }
            if (!matchesRole(mob, role)) {
                continue;
            }
            nearest = Math.min(nearest, Math.sqrt(mob.distanceToSqr(player)));
        }
        return nearest == Double.MAX_VALUE ? "-" : TextUtil.fmt2(nearest);
    }

    private static String nearestSourceForRole(ServerPlayer player, NpcRole role, int radius) {
        double nearest = Double.MAX_VALUE;
        String nearestSource = "-";
        for (var entity : player.serverLevel().getEntities(null, player.getBoundingBox().inflate(radius))) {
            if (!(entity instanceof Mob mob)) {
                continue;
            }
            if (!matchesRole(mob, role)) {
                continue;
            }
            double distance = Math.sqrt(mob.distanceToSqr(player));
            if (distance < nearest) {
                nearest = distance;
                nearestSource = roleSource(mob);
            }
        }
        return nearestSource;
    }

    private static int stats(ServerPlayer player, boolean secondary) {
        PlayerJobProfile profile = AdvancedJobsMod.get().jobManager().getOrCreateProfile(player);
        String jobId = selectedSlotJobId(profile, secondary);
        if (jobId == null) {
            player.sendSystemMessage(TextUtil.tr(secondary
                ? "command.advancedjobs.error.no_slot_job"
                : "command.advancedjobs.error.no_active"));
            return 1;
        }
        var progress = profile.progress(jobId);
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.stats.header", slotLabel(secondary)));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.stats.job", jobName(jobId)));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.stats.level_xp", progress.level(), TextUtil.fmt2(progress.xp())));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.stats.title", professionTitle(jobId, progress.level())));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.stats.salary", TextUtil.fmt2(progress.pendingSalary()), TextUtil.fmt2(progress.earnedTotal())));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.stats.salary_rules",
            TextUtil.fmt2(ConfigManager.COMMON.salaryTaxRate.get() * 100.0D),
            TextUtil.fmt2(ConfigManager.COMMON.maxSalaryPerClaim.get()),
            TimeUtil.formatRemainingSeconds(ConfigManager.COMMON.salaryClaimIntervalSeconds.get())));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.stats.salary_mode",
            TextUtil.tr(ConfigManager.COMMON.instantSalary.get() ? "gui.advancedjobs.salary_mode.instant" : "gui.advancedjobs.salary_mode.manual")));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.stats.skill_points", profile.availableSkillPoints(jobId)));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.stats.next_passive",
            nextPassiveUnlock(jobId, progress.level())));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.stats.next_skill_point",
            nextSkillPointLevel(progress.level())));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.stats.daily_cycle",
            TimeUtil.formatRemainingSeconds(Math.max(0L, nextDailyReset(progress) - TimeUtil.now())),
            TimeUtil.formatRemainingSeconds(Math.max(0L, nextContractRotation(progress) - TimeUtil.now()))));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.stats.daily",
            progress.dailyTasks().stream().filter(DailyTaskProgress::completed).count(), progress.dailyTasks().size()));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.stats.contracts",
            progress.contracts().stream().filter(ContractProgress::completed).count(), progress.contracts().size()));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.stats.history",
            progress.dailyHistory().size(), progress.contractHistory().size()));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.stats.unlocked_titles",
            profile.unlockedTitles().size(), latestUnlockedTitle(profile)));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.stats.milestones",
            progress.unlockedMilestones().size(), latestMilestone(progress)));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.stats.economy_context",
            ConfigManager.economy().providerId(), ConfigManager.economy().externalCurrencyId()));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.stats.tax_sink_account",
            ConfigManager.economy().taxSinkAccountUuid()));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.stats.reward_context",
            targetName(player.serverLevel().dimension().location()),
            TextUtil.fmt2(currentWorldMultiplier(player)),
            targetName(currentBiomeId(player)),
            TextUtil.fmt2(currentBiomeMultiplier(player)),
            TextUtil.fmt2(ConfigManager.economy().vipMultiplier()),
            TextUtil.fmt2(ConfigManager.economy().eventMultiplier()),
            TextUtil.fmt2(effectiveRewardMultiplier(player))));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.stats.mob_filters",
            yesNo(ConfigManager.COMMON.blockArtificialMobRewards.get()),
            yesNo(ConfigManager.COMMON.blockBabyMobRewards.get()),
            yesNo(ConfigManager.COMMON.blockTamedMobRewards.get())));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.stats.loot_chunk_filters",
            TimeUtil.formatRemainingSeconds(ConfigManager.COMMON.lootContainerRewardCooldownSeconds.get()),
            TimeUtil.formatRemainingSeconds(ConfigManager.COMMON.exploredChunkRewardCooldownSeconds.get())));
        appendActiveEffects(player);
        progress.actionStats().entrySet().stream()
            .max(Map.Entry.comparingByValue(Comparator.naturalOrder()))
            .ifPresent(entry -> player.sendSystemMessage(TextUtil.tr("command.advancedjobs.stats.top_action",
                humanizeActionKey(entry.getKey()), entry.getValue())));
        progress.dailyHistory().stream().findFirst()
            .ifPresent(entry -> player.sendSystemMessage(recentDailyHistoryLine(jobId, entry)));
        progress.contractHistory().stream().findFirst()
            .ifPresent(entry -> player.sendSystemMessage(recentContractHistoryLine(jobId, entry)));
        return 1;
    }

    private static int salary(ServerPlayer player) {
        PlayerJobProfile profile = AdvancedJobsMod.get().jobManager().getOrCreateProfile(player);
        appendSalarySlotSummary(player, profile, profile.activeJobId(), "command.advancedjobs.salary.primary");
        appendSalarySlotSummary(player, profile, profile.secondaryJobId(), "command.advancedjobs.salary.secondary");
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.salary.economy_context",
            ConfigManager.economy().providerId(), ConfigManager.economy().externalCurrencyId()));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.salary.tax_sink_account",
            ConfigManager.economy().taxSinkAccountUuid()));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.salary.reward_context",
            targetName(player.serverLevel().dimension().location()),
            TextUtil.fmt2(currentWorldMultiplier(player)),
            targetName(currentBiomeId(player)),
            TextUtil.fmt2(currentBiomeMultiplier(player)),
            TextUtil.fmt2(effectiveRewardMultiplier(player))));
        if (ConfigManager.COMMON.instantSalary.get()) {
            player.sendSystemMessage(TextUtil.tr("command.advancedjobs.salary.auto_mode"));
            player.sendSystemMessage(TextUtil.tr("command.advancedjobs.salary.rules",
                TextUtil.fmt2(ConfigManager.COMMON.salaryTaxRate.get() * 100.0D),
                TextUtil.fmt2(ConfigManager.COMMON.maxSalaryPerClaim.get()),
                TimeUtil.formatRemainingSeconds(ConfigManager.COMMON.salaryClaimIntervalSeconds.get())));
            return 1;
        }
        double pending = totalPendingSalary(profile);
        double grossPreview = Math.min(pending, ConfigManager.COMMON.maxSalaryPerClaim.get());
        double taxPreview = grossPreview * ConfigManager.COMMON.salaryTaxRate.get();
        double netPreview = Math.max(0.0D, grossPreview - taxPreview);
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.salary.preview",
            TextUtil.fmt2(grossPreview),
            TextUtil.fmt2(taxPreview),
            TextUtil.fmt2(netPreview)));
        double paid = AdvancedJobsMod.get().jobManager().claimSalary(player);
        if (paid < 0.0D) {
            player.sendSystemMessage(TextUtil.tr("command.advancedjobs.salary.cooldown",
                TimeUtil.formatRemainingSeconds((long) -paid)));
            return 0;
        }
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.salary.claimed", TextUtil.fmt2(paid)));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.salary.rules",
            TextUtil.fmt2(ConfigManager.COMMON.salaryTaxRate.get() * 100.0D),
            TextUtil.fmt2(ConfigManager.COMMON.maxSalaryPerClaim.get()),
            TimeUtil.formatRemainingSeconds(ConfigManager.COMMON.salaryClaimIntervalSeconds.get())));
        return 1;
    }

    private static int leave(ServerPlayer player, boolean secondary) {
        if (AdvancedJobsMod.get().jobManager().leaveJob(player, secondary)) {
            player.sendSystemMessage(TextUtil.tr(secondary
                ? "command.advancedjobs.leave.secondary"
                : "command.advancedjobs.leave.primary"));
            return 1;
        }
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.error.no_slot_job"));
        return 0;
    }

    private static int top(CommandSourceStack source, String jobId) {
        if ("all".equalsIgnoreCase(jobId)) {
            return topOverall(source);
        }
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.top.header", jobName(jobId)), false);
        ArrayList<PlayerJobProfile> entries = new ArrayList<>();
        int selfRank = 0;
        double selfXp = 0.0D;
        double selfEarned = 0.0D;
        double aboveXp = 0.0D;
        double aboveEarned = 0.0D;
        String abovePlayer = null;
        int i = 1;
        for (PlayerJobProfile profile : AdvancedJobsMod.get().jobManager().leaderboard(jobId)) {
            entries.add(profile);
            int place = i++;
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.top.line",
                place, profile.playerName(), profile.progress(jobId).level(),
                TextUtil.fmt2(profile.progress(jobId).xp()),
                TextUtil.fmt2(profile.progress(jobId).earnedTotal())), false);
            if (source.getEntity() instanceof ServerPlayer player
                && profile.playerName().equalsIgnoreCase(player.getGameProfile().getName())) {
                selfRank = place;
                selfXp = profile.progress(jobId).xp();
                selfEarned = profile.progress(jobId).earnedTotal();
                if (place > 1 && entries.size() >= 2) {
                    PlayerJobProfile above = entries.get(entries.size() - 2);
                    aboveXp = above.progress(jobId).xp();
                    aboveEarned = above.progress(jobId).earnedTotal();
                    abovePlayer = above.playerName();
                }
            }
            if (i > 10) {
                break;
            }
        }
        if (entries.isEmpty()) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.top.none"), false);
            return 1;
        }
        PlayerJobProfile leader = entries.get(0);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.top.summary",
            entries.size(),
            leader.playerName(),
            leader.progress(jobId).level()), false);
        appendTopSelfSummary(source, selfRank, selfXp, selfEarned, abovePlayer, aboveXp, aboveEarned, false);
        return 1;
    }

    private static int topOverall(CommandSourceStack source) {
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.top.overall_header"), false);
        ArrayList<PlayerJobProfile> entries = new ArrayList<>();
        int selfRank = 0;
        int selfLevels = 0;
        double selfEarned = 0.0D;
        int aboveLevels = 0;
        double aboveEarned = 0.0D;
        String abovePlayer = null;
        int i = 1;
        for (PlayerJobProfile profile : AdvancedJobsMod.get().jobManager().overallLeaderboard()) {
            entries.add(profile);
            int place = i++;
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.top.overall_line",
                place, profile.playerName(),
                AdvancedJobsMod.get().jobManager().totalLevelsAcrossJobs(profile),
                TextUtil.fmt2(AdvancedJobsMod.get().jobManager().totalEarnedAcrossJobs(profile))), false);
            if (source.getEntity() instanceof ServerPlayer player
                && profile.playerName().equalsIgnoreCase(player.getGameProfile().getName())) {
                selfRank = place;
                selfLevels = AdvancedJobsMod.get().jobManager().totalLevelsAcrossJobs(profile);
                selfEarned = AdvancedJobsMod.get().jobManager().totalEarnedAcrossJobs(profile);
                if (place > 1 && entries.size() >= 2) {
                    PlayerJobProfile above = entries.get(entries.size() - 2);
                    aboveLevels = AdvancedJobsMod.get().jobManager().totalLevelsAcrossJobs(above);
                    aboveEarned = AdvancedJobsMod.get().jobManager().totalEarnedAcrossJobs(above);
                    abovePlayer = above.playerName();
                }
            }
            if (i > 10) {
                break;
            }
        }
        if (entries.isEmpty()) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.top.none"), false);
            return 1;
        }
        PlayerJobProfile leader = entries.get(0);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.top.overall_summary",
            entries.size(),
            leader.playerName(),
            AdvancedJobsMod.get().jobManager().totalLevelsAcrossJobs(leader)), false);
        appendTopOverallSelfSummary(source, selfRank, selfLevels, selfEarned, abovePlayer, aboveLevels, aboveEarned);
        return 1;
    }

    private static int choose(ServerPlayer player, String jobId, boolean secondary) {
        if (AdvancedJobsMod.get().jobManager().chooseJob(player, jobId, secondary)) {
            JobDefinition definition = AdvancedJobsMod.get().jobManager().job(jobId).orElseThrow();
            player.sendSystemMessage(TextUtil.tr(secondary
                ? "command.advancedjobs.choose.secondary"
                : "command.advancedjobs.choose.primary", TextUtil.tr(definition.translationKey())));
            return 1;
        }
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.error.choose_failed"));
        return 0;
    }

    private static int daily(ServerPlayer player, boolean secondary) {
        PlayerJobProfile profile = AdvancedJobsMod.get().jobManager().getOrCreateProfile(player);
        String jobId = selectedSlotJobId(profile, secondary);
        if (jobId == null) {
            player.sendSystemMessage(TextUtil.tr(secondary
                ? "command.advancedjobs.error.no_slot_job"
                : "command.advancedjobs.error.no_active"));
            return 0;
        }
        var progress = profile.progress(jobId);
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.daily.header", jobName(jobId), slotLabel(secondary)));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.daily.reset",
            TimeUtil.formatRemainingSeconds(nextDailyReset(progress) - TimeUtil.now())));
        if (progress.dailyTasks().isEmpty()) {
            player.sendSystemMessage(TextUtil.tr("command.advancedjobs.daily.none"));
            return 1;
        }
        progress.dailyTasks().forEach(task -> {
            player.sendSystemMessage(dailyLine(jobId, task));
            ConfigManager.dailyTasks().tasksForJob(jobId).stream()
                .filter(entry -> entry.id().equals(task.taskId()))
                .findFirst()
                .ifPresent(template -> sendRewardBonusLines(player,
                    template.bonusItem(), template.bonusCount(),
                    template.buffEffect(), template.buffDurationSeconds(), template.buffAmplifier(),
                    template.bonusTitle()));
        });
        return 1;
    }

    private static int contracts(ServerPlayer player, boolean secondary) {
        PlayerJobProfile profile = AdvancedJobsMod.get().jobManager().getOrCreateProfile(player);
        String jobId = selectedSlotJobId(profile, secondary);
        if (jobId == null) {
            player.sendSystemMessage(TextUtil.tr(secondary
                ? "command.advancedjobs.error.no_slot_job"
                : "command.advancedjobs.error.no_active"));
            return 0;
        }
        var progress = profile.progress(jobId);
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.contracts.header", jobName(jobId), slotLabel(secondary)));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.contracts.rotation",
            TimeUtil.formatRemainingSeconds(Math.max(0L, nextContractRotation(progress) - TimeUtil.now()))));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.contracts.reroll.rules",
            TextUtil.fmt2(ConfigManager.COMMON.contractRerollPrice.get()),
            TimeUtil.formatRemainingSeconds(AdvancedJobsMod.get().jobManager().contractRerollCooldownRemaining(profile))));
        if (progress.contracts().isEmpty()) {
            player.sendSystemMessage(TextUtil.tr("command.advancedjobs.contracts.none"));
            return 1;
        }
        progress.contracts().forEach(contract -> {
            player.sendSystemMessage(contractLine(jobId, contract));
            ConfigManager.contracts().contractsForJob(jobId).stream()
                .filter(entry -> entry.id().equals(contract.contractId()))
                .findFirst()
                .ifPresent(template -> sendRewardBonusLines(player,
                    template.bonusItem(), template.bonusCount(),
                    template.buffEffect(), template.buffDurationSeconds(), template.buffAmplifier(),
                    template.bonusTitle()));
        });
        return 1;
    }

    private static int rerollContracts(ServerPlayer player, boolean secondary) {
        PlayerJobProfile profile = AdvancedJobsMod.get().jobManager().getOrCreateProfile(player);
        String jobId = selectedSlotJobId(profile, secondary);
        if (jobId == null) {
            player.sendSystemMessage(TextUtil.tr(secondary
                ? "command.advancedjobs.error.no_slot_job"
                : "command.advancedjobs.error.no_active"));
            return 0;
        }
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.contracts.reroll.rules",
            TextUtil.fmt2(ConfigManager.COMMON.contractRerollPrice.get()),
            TimeUtil.formatRemainingSeconds(AdvancedJobsMod.get().jobManager().contractRerollCooldownRemaining(profile))));
        var preview = AdvancedJobsMod.get().jobManager().previewContractReroll(player, jobId);
        if (preview != com.example.advancedjobs.job.JobManager.ContractRerollResult.SUCCESS) {
            player.sendSystemMessage(contractRerollFailureMessage(preview, profile, jobId));
            return 0;
        }
        boolean ok = AdvancedJobsMod.get().jobManager().rerollContracts(player, jobId);
        player.sendSystemMessage(TextUtil.tr(ok
            ? "command.advancedjobs.contracts.reroll.success"
            : "command.advancedjobs.contracts.reroll.failed", jobName(jobId)));
        return ok ? 1 : 0;
    }

    private static int skills(ServerPlayer player, boolean secondary) {
        PlayerJobProfile profile = AdvancedJobsMod.get().jobManager().getOrCreateProfile(player);
        String jobId = selectedSlotJobId(profile, secondary);
        if (jobId == null) {
            player.sendSystemMessage(TextUtil.tr(secondary
                ? "command.advancedjobs.error.no_slot_job"
                : "command.advancedjobs.error.no_active"));
            return 0;
        }
        JobDefinition definition = AdvancedJobsMod.get().jobManager().job(jobId).orElse(null);
        if (definition == null) {
            player.sendSystemMessage(TextUtil.tr("command.advancedjobs.error.missing_profession_data"));
            return 0;
        }
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.skills.header", TextUtil.tr(definition.translationKey()), slotLabel(secondary)));
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.skills.available", profile.availableSkillPoints(definition.id())));
        for (SkillBranch branch : definition.skillBranches()) {
            long unlockedInBranch = branch.nodes().stream().filter(node -> isUnlocked(profile, definition.id(), node.id())).count();
            player.sendSystemMessage(TextUtil.tr("command.advancedjobs.skills.branch",
                TextUtil.tr(branch.translationKey()), unlockedInBranch, branch.nodes().size()));
            SkillNode nextNode = nextUnlockableNode(profile, definition.id(), branch);
            if (nextNode != null) {
                player.sendSystemMessage(TextUtil.tr("command.advancedjobs.skills.next",
                    TextUtil.tr(nextNode.translationKey()),
                    nextNode.requiredLevel(),
                    nextNode.cost(),
                    humanizeAction(nextNode.effectType()),
                    TextUtil.fmt2(nextNode.effectValue())));
            }
            for (SkillNode node : branch.nodes()) {
                if (isUnlocked(profile, definition.id(), node.id())) {
                    player.sendSystemMessage(TextUtil.tr("command.advancedjobs.skills.node",
                        TextUtil.tr(node.translationKey()), humanizeAction(node.effectType()), TextUtil.fmt2(node.effectValue())));
                }
            }
        }
        return 1;
    }

    private static int titles(ServerPlayer player) {
        PlayerJobProfile profile = AdvancedJobsMod.get().jobManager().getOrCreateProfile(player);
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.titles.header", profile.unlockedTitles().size()));
        if (profile.unlockedTitles().isEmpty()) {
            player.sendSystemMessage(TextUtil.tr("command.advancedjobs.titles.none"));
            return 1;
        }
        var list = new ArrayList<>(profile.unlockedTitles());
        int start = Math.max(0, list.size() - 8);
        for (int i = start; i < list.size(); i++) {
            player.sendSystemMessage(TextUtil.tr("command.advancedjobs.titles.line",
                TextUtil.tr("title.advancedjobs." + list.get(i))));
        }
        return 1;
    }

    private static int milestones(ServerPlayer player, boolean secondary) {
        PlayerJobProfile profile = AdvancedJobsMod.get().jobManager().getOrCreateProfile(player);
        String jobId = selectedSlotJobId(profile, secondary);
        if (jobId == null) {
            player.sendSystemMessage(TextUtil.tr(secondary
                ? "command.advancedjobs.error.no_slot_job"
                : "command.advancedjobs.error.no_active"));
            return 0;
        }
        var progress = profile.progress(jobId);
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.milestones.header",
            jobName(jobId), slotLabel(secondary), progress.unlockedMilestones().size()));
        if (progress.unlockedMilestones().isEmpty()) {
            player.sendSystemMessage(TextUtil.tr("command.advancedjobs.milestones.none"));
            return 1;
        }
        int start = Math.max(0, progress.unlockedMilestones().size() - 8);
        for (int i = start; i < progress.unlockedMilestones().size(); i++) {
            String milestoneId = progress.unlockedMilestones().get(i);
            player.sendSystemMessage(TextUtil.tr("command.advancedjobs.milestones.line",
                TextUtil.tr("milestone.advancedjobs." + milestoneId)));
            String rewardTitleId = milestoneTitleId(milestoneId);
            if (rewardTitleId != null) {
                player.sendSystemMessage(TextUtil.tr("command.advancedjobs.milestones.title_reward",
                    TextUtil.tr("title.advancedjobs." + rewardTitleId)));
            }
        }
        return 1;
    }

    private static Component dailyLine(String jobId, DailyTaskProgress progress) {
        ConfigManager.DailyTaskTemplate template = ConfigManager.dailyTasks().tasksForJob(jobId).stream()
            .filter(entry -> entry.id().equals(progress.taskId()))
            .findFirst()
            .orElse(null);
        if (template == null) {
            return TextUtil.tr("command.advancedjobs.daily.line.fallback", progress.taskId(), progress.progress(), progress.target());
        }
        return TextUtil.tr(progress.completed()
                ? "command.advancedjobs.daily.line.complete"
                : "command.advancedjobs.daily.line",
            actionLabel(template.type().name()),
            targetName(template.target()),
            progress.progress(),
            progress.target(),
            TextUtil.fmt2(template.salary()),
            TextUtil.fmt2(template.xp()));
    }

    private static Component contractLine(String jobId, ContractProgress progress) {
        ConfigManager.ContractTemplate template = ConfigManager.contracts().contractsForJob(jobId).stream()
            .filter(entry -> entry.id().equals(progress.contractId()))
            .findFirst()
            .orElse(null);
        if (template == null) {
            return TextUtil.tr("command.advancedjobs.contracts.line.fallback", progress.contractId(), progress.progress(), progress.target());
        }
        return TextUtil.tr(progress.completed()
                ? "command.advancedjobs.contracts.line.complete"
                : "command.advancedjobs.contracts.line",
            rarityLabel(template.rarity()),
            actionLabel(template.type().name()),
            targetName(template.target()),
            progress.progress(),
            progress.target(),
            TextUtil.fmt2(template.salary()),
            TextUtil.fmt2(template.xp()),
            TimeUtil.formatRemainingSeconds(progress.expiresAtEpochSecond() - TimeUtil.now()));
    }

    private static Component recentDailyHistoryLine(String jobId, DailyTaskHistoryEntry entry) {
        ConfigManager.DailyTaskTemplate template = ConfigManager.dailyTasks().tasksForJob(jobId).stream()
            .filter(candidate -> candidate.id().equals(entry.taskId()))
            .findFirst()
            .orElse(null);
        if (template == null) {
            return TextUtil.tr("command.advancedjobs.stats.recent_daily.fallback",
                entry.taskId(), TextUtil.fmt2(entry.salaryReward()), TextUtil.fmt2(entry.xpReward()));
        }
        return TextUtil.tr("command.advancedjobs.stats.recent_daily",
            actionLabel(template.type().name()),
            targetName(template.target()),
            TextUtil.fmt2(entry.salaryReward()),
            TextUtil.fmt2(entry.xpReward()),
            TimeUtil.formatRemainingSeconds(Math.max(0L, TimeUtil.now() - entry.completedAtEpochSecond())));
    }

    private static long nextDailyReset(JobProgress progress) {
        return progress.dailyTasks().stream()
            .mapToLong(DailyTaskProgress::resetEpochSecond)
            .min()
            .orElse(TimeUtil.now());
    }

    private static long nextContractRotation(JobProgress progress) {
        return progress.contracts().stream()
            .mapToLong(ContractProgress::expiresAtEpochSecond)
            .min()
            .orElse(TimeUtil.now());
    }

    private static void appendJobSlotSummary(ServerPlayer player, PlayerJobProfile profile, String jobId, String key) {
        if (jobId == null) {
            return;
        }
        JobProgress progress = profile.progress(jobId);
        player.sendSystemMessage(TextUtil.tr(key,
            progress.level(),
            TextUtil.fmt2(progress.pendingSalary()),
            TextUtil.fmt2(progress.earnedTotal()),
            TextUtil.fmt2(claimPreviewForJob(profile, jobId))));
    }

    private static void appendSalarySlotSummary(ServerPlayer player, PlayerJobProfile profile, String jobId, String key) {
        if (jobId == null) {
            return;
        }
        JobProgress progress = profile.progress(jobId);
        player.sendSystemMessage(TextUtil.tr(key,
            jobName(jobId),
            TextUtil.fmt2(progress.pendingSalary()),
            TextUtil.fmt2(progress.earnedTotal()),
            TextUtil.fmt2(claimPreviewForJob(profile, jobId))));
    }

    private static String selectedSlotJobId(PlayerJobProfile profile, boolean secondary) {
        return secondary ? profile.secondaryJobId() : profile.activeJobId();
    }

    private static Component slotLabel(boolean secondary) {
        return TextUtil.tr(secondary ? "command.advancedjobs.slot.secondary" : "command.advancedjobs.slot.primary");
    }

    private static Component yesNo(boolean value) {
        return TextUtil.tr(value ? "command.advancedjobs.common.enabled" : "command.advancedjobs.common.disabled");
    }

    private static double totalPendingSalary(PlayerJobProfile profile) {
        double total = 0.0D;
        if (profile.activeJobId() != null) {
            total += profile.progress(profile.activeJobId()).pendingSalary();
        }
        if (profile.secondaryJobId() != null) {
            total += profile.progress(profile.secondaryJobId()).pendingSalary();
        }
        return total;
    }

    private static double pendingSalaryForSlot(PlayerJobProfile profile, boolean secondary) {
        String jobId = selectedSlotJobId(profile, secondary);
        return jobId == null ? 0.0D : profile.progress(jobId).pendingSalary();
    }

    private static double claimPreviewForJob(PlayerJobProfile profile, String jobId) {
        double remaining = ConfigManager.COMMON.maxSalaryPerClaim.get();
        if (profile.activeJobId() != null) {
            double activeShare = Math.min(profile.progress(profile.activeJobId()).pendingSalary(), remaining);
            if (profile.activeJobId().equals(jobId)) {
                return activeShare;
            }
            remaining -= activeShare;
        }
        if (profile.secondaryJobId() != null) {
            double secondaryShare = Math.min(profile.progress(profile.secondaryJobId()).pendingSalary(), remaining);
            if (profile.secondaryJobId().equals(jobId)) {
                return secondaryShare;
            }
        }
        return 0.0D;
    }

    private static void appendTopSelfSummary(CommandSourceStack source, int selfRank, double selfXp, double selfEarned,
                                             String abovePlayer, double aboveXp, double aboveEarned, boolean overall) {
        if (selfRank <= 0) {
            return;
        }
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.top.self",
            selfRank, TextUtil.fmt2(selfXp), TextUtil.fmt2(selfEarned)), false);
        if (abovePlayer != null) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.top.gap",
                abovePlayer,
                TextUtil.fmt2(Math.max(0.0D, aboveXp - selfXp)),
                TextUtil.fmt2(Math.max(0.0D, aboveEarned - selfEarned))), false);
        }
    }

    private static void appendTopOverallSelfSummary(CommandSourceStack source, int selfRank, int selfLevels, double selfEarned,
                                                    String abovePlayer, int aboveLevels, double aboveEarned) {
        if (selfRank <= 0) {
            return;
        }
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.top.overall_self",
            selfRank, selfLevels, TextUtil.fmt2(selfEarned)), false);
        if (abovePlayer != null) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.top.overall_gap",
                abovePlayer,
                Math.max(0, aboveLevels - selfLevels),
                TextUtil.fmt2(Math.max(0.0D, aboveEarned - selfEarned))), false);
        }
    }

    private static ResourceLocation currentBiomeId(ServerPlayer player) {
        return player.serverLevel().getBiome(player.blockPosition()).unwrapKey()
            .map(key -> key.location())
            .orElse(ResourceLocationUtil.minecraft("plains"));
    }

    private static double currentWorldMultiplier(ServerPlayer player) {
        return ConfigManager.economy().worldMultipliers()
            .getOrDefault(player.serverLevel().dimension().location().toString(), 1.0D);
    }

    private static double currentBiomeMultiplier(ServerPlayer player) {
        return ConfigManager.economy().biomeMultipliers()
            .getOrDefault(currentBiomeId(player).toString(), 1.0D);
    }

    private static double effectiveRewardMultiplier(ServerPlayer player) {
        return currentWorldMultiplier(player)
            * currentBiomeMultiplier(player)
            * ConfigManager.economy().eventMultiplier()
            * ConfigManager.economy().vipMultiplier();
    }

    private static void appendActiveEffects(ServerPlayer player) {
        var effects = new ArrayList<>(player.getActiveEffects());
        effects.sort(Comparator
            .comparingInt(MobEffectInstance::getAmplifier).reversed()
            .thenComparingInt(MobEffectInstance::getDuration).reversed()
            .thenComparing(effect -> effect.getEffect().getDisplayName().getString()));
        if (effects.isEmpty()) {
            player.sendSystemMessage(TextUtil.tr("command.advancedjobs.active_effects.none"));
            return;
        }
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.active_effects.header"));
        for (int i = 0; i < Math.min(5, effects.size()); i++) {
            MobEffectInstance effect = effects.get(i);
            player.sendSystemMessage(TextUtil.tr("command.advancedjobs.active_effects.line",
                effect.getEffect().getDisplayName(),
                effect.getAmplifier() + 1,
                TimeUtil.formatRemainingSeconds(Math.max(0, effect.getDuration() / 20))));
        }
    }

    private static Component recentContractHistoryLine(String jobId, ContractHistoryEntry entry) {
        ConfigManager.ContractTemplate template = ConfigManager.contracts().contractsForJob(jobId).stream()
            .filter(candidate -> candidate.id().equals(entry.contractId()))
            .findFirst()
            .orElse(null);
        if (template == null) {
            return TextUtil.tr("command.advancedjobs.stats.recent_contract.fallback",
                entry.contractId(), TextUtil.fmt2(entry.salaryReward()), TextUtil.fmt2(entry.xpReward()));
        }
        return TextUtil.tr("command.advancedjobs.stats.recent_contract",
            rarityLabel(entry.rarity()),
            actionLabel(template.type().name()),
            targetName(template.target()),
            TextUtil.fmt2(entry.salaryReward()),
            TextUtil.fmt2(entry.xpReward()),
            TimeUtil.formatRemainingSeconds(Math.max(0L, TimeUtil.now() - entry.completedAtEpochSecond())));
    }

    private static Component jobName(String jobId) {
        if (jobId == null) {
            return TextUtil.tr("command.advancedjobs.common.none");
        }
        return AdvancedJobsMod.get().jobManager().job(jobId)
            .map(definition -> TextUtil.tr(definition.translationKey()))
            .orElse(Component.literal(jobId));
    }

    private static Component professionTitle(String jobId, int level) {
        return TextUtil.tr("command.advancedjobs.title.format", rankLabel(level), jobName(jobId));
    }

    private static Component nextPassiveUnlock(String jobId, int level) {
        JobDefinition definition = AdvancedJobsMod.get().jobManager().job(jobId).orElse(null);
        if (definition == null) {
            return TextUtil.tr("command.advancedjobs.common.none");
        }
        return definition.passivePerks().entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .filter(entry -> entry.getKey() > level && !entry.getValue().isEmpty())
            .findFirst()
            .map(entry -> TextUtil.tr("command.advancedjobs.next_passive.format",
                entry.getKey(), TextUtil.tr(entry.getValue().get(0))))
            .orElse(TextUtil.tr("command.advancedjobs.common.none"));
    }

    private static int nextSkillPointLevel(int level) {
        return ((level / 5) + 1) * 5;
    }

    private static Component latestUnlockedTitle(PlayerJobProfile profile) {
        if (profile.unlockedTitles().isEmpty()) {
            return TextUtil.tr("command.advancedjobs.common.none");
        }
        var list = new ArrayList<>(profile.unlockedTitles());
        return TextUtil.tr("title.advancedjobs." + list.get(list.size() - 1));
    }

    private static Component latestMilestone(com.example.advancedjobs.model.JobProgress progress) {
        if (progress.unlockedMilestones().isEmpty()) {
            return TextUtil.tr("command.advancedjobs.common.none");
        }
        return TextUtil.tr("milestone.advancedjobs." + progress.unlockedMilestones().get(progress.unlockedMilestones().size() - 1));
    }

    private static String milestoneTitleId(String milestoneId) {
        return switch (milestoneId) {
            case "miner_diamond_25" -> "gem_cutter";
            case "miner_redstone_100" -> "redstone_vein";
            case "lumberjack_logs_500" -> "timber_champion";
            case "forester_cherry_100" -> "cherry_keeper";
            case "forester_birch_150" -> "birch_sentinel";
            case "farmer_harvest_500" -> "field_master";
            case "farmer_planter_250" -> "green_thumb";
            case "harvester_patch_128" -> "harvest_titan";
            case "animal_breeder_stock_150" -> "stock_breeder";
            case "animal_breeder_cattle_75" -> "cattle_baron";
            case "hunter_zombie_100" -> "undead_bane";
            case "boss_hunter_boss_5" -> "bossbreaker";
            case "builder_blocks_500" -> "stonehands";
            case "builder_decor_200" -> "artisan_builder";
            case "merchant_trade_100" -> "deal_maker";
            case "alchemist_brew_64" -> "potion_savant";
            case "enchanter_books_32" -> "rune_reader";
            case "explorer_chunks_64" -> "trailblazer";
            case "treasure_loot_25" -> "vault_seeker";
            case "engineer_redstone_250" -> "circuit_master";
            case "guard_patrol_150" -> "warden_of_roads";
            case "quarry_stone_1000" -> "heart_of_stone";
            case "digger_gravel_500" -> "graveborn_digger";
            case "sand_collector_sand_1000" -> "sea_of_sand";
            case "ice_harvester_blue_100" -> "blue_frost";
            case "shepherd_flock_100" -> "high_shepherd";
            case "shepherd_wool_256" -> "wool_archon";
            case "beekeeper_honey_128" -> "hive_warden";
            case "beekeeper_comb_192" -> "comb_lord";
            case "herbalist_wart_256" -> "wart_whisperer";
            case "herbalist_berries_192" -> "berry_sage";
            default -> null;
        };
    }

    private static String humanizeAction(String action) {
        String lower = action.toLowerCase(Locale.ROOT);
        String[] parts = lower.split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.toString();
    }

    private static Component rankLabel(int level) {
        String key = level >= 75 ? "gui.advancedjobs.rank.legend"
            : level >= 50 ? "gui.advancedjobs.rank.master"
            : level >= 25 ? "gui.advancedjobs.rank.specialist"
            : "gui.advancedjobs.rank.novice";
        return TextUtil.tr(key);
    }

    private static String humanizeActionKey(String actionKey) {
        String[] parts = actionKey.split("\\|", 2);
        if (parts.length != 2) {
            return humanizeAction(actionKey);
        }
        return humanizeAction(parts[0]) + " " + targetName(ResourceLocationUtil.parse(parts[1]));
    }

    private static Component actionLabel(String action) {
        String lower = action.toLowerCase(Locale.ROOT);
        String key = "gui.advancedjobs.action." + lower;
        return Component.translatable(key).getString().equals(key)
            ? Component.literal(humanizeAction(action))
            : TextUtil.tr(key);
    }

    private static Component rarityLabel(String rarity) {
        String lower = rarity.toLowerCase(Locale.ROOT);
        String key = "gui.advancedjobs.rarity." + lower;
        return Component.translatable(key).getString().equals(key)
            ? Component.literal(humanizeAction(rarity))
            : TextUtil.tr(key);
    }

    private static void sendRewardBonusLines(ServerPlayer player, String bonusItem, int bonusCount,
                                             String buffEffect, int buffDurationSeconds, int buffAmplifier,
                                             String bonusTitle) {
        if (bonusItem != null && !bonusItem.isBlank() && bonusCount > 0) {
            player.sendSystemMessage(TextUtil.tr("command.advancedjobs.reward_bonus_item",
                targetName(ResourceLocationUtil.parse(bonusItem)), bonusCount));
        }
        if (buffEffect != null && !buffEffect.isBlank() && buffDurationSeconds > 0) {
            player.sendSystemMessage(TextUtil.tr("command.advancedjobs.reward_bonus_buff",
                effectName(ResourceLocationUtil.parse(buffEffect)),
                TimeUtil.formatRemainingSeconds(buffDurationSeconds),
                buffAmplifier + 1));
        }
        if (bonusTitle != null && !bonusTitle.isBlank()) {
            player.sendSystemMessage(TextUtil.tr("command.advancedjobs.reward_bonus_title",
                TextUtil.tr("title.advancedjobs." + bonusTitle)));
        }
    }

    private static Component contractRerollFailureMessage(com.example.advancedjobs.job.JobManager.ContractRerollResult result,
                                                          PlayerJobProfile profile, String jobId) {
        return switch (result) {
            case NO_JOB, NOT_ASSIGNED -> TextUtil.tr("command.advancedjobs.contracts.reroll.failed", jobName(jobId));
            case COOLDOWN -> TextUtil.tr("command.advancedjobs.contracts.reroll.cooldown",
                TimeUtil.formatRemainingSeconds(AdvancedJobsMod.get().jobManager().contractRerollCooldownRemaining(profile)));
            case INSUFFICIENT_FUNDS -> TextUtil.tr("command.advancedjobs.contracts.reroll.insufficient_funds",
                TextUtil.fmt2(ConfigManager.COMMON.contractRerollPrice.get()));
            case SUCCESS -> TextUtil.tr("command.advancedjobs.contracts.reroll.success", jobName(jobId));
        };
    }

    private static boolean isUnlocked(PlayerJobProfile profile, String jobId, String nodeId) {
        return profile.progress(jobId).unlockedNodes().stream().anyMatch(state -> state.nodeId().equals(nodeId) && state.unlocked());
    }

    private static SkillNode nextUnlockableNode(PlayerJobProfile profile, String jobId, SkillBranch branch) {
        return branch.nodes().stream()
            .filter(node -> !isUnlocked(profile, jobId, node.id()))
            .filter(node -> node.parentId() == null || isUnlocked(profile, jobId, node.parentId()))
            .min(Comparator.comparingInt(SkillNode::requiredLevel).thenComparingInt(SkillNode::cost))
            .orElse(null);
    }

    private static String targetName(ResourceLocation id) {
        if (ForgeRegistries.ITEMS.containsKey(id)) {
            return ForgeRegistries.ITEMS.getValue(id).getDescription().getString();
        }
        if (ForgeRegistries.BLOCKS.containsKey(id)) {
            return ForgeRegistries.BLOCKS.getValue(id).getName().getString();
        }
        if (ForgeRegistries.ENTITY_TYPES.containsKey(id)) {
            return ForgeRegistries.ENTITY_TYPES.getValue(id).getDescription().getString();
        }
        return id.toString();
    }

    private static boolean matchesRole(Mob mob, NpcRole role) {
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

    private static String roleSource(Mob mob) {
        if (mob.getType() == ModEntities.JOBS_MASTER.get()
            || mob.getType() == ModEntities.DAILY_BOARD.get()
            || mob.getType() == ModEntities.STATUS_BOARD.get()
            || mob.getType() == ModEntities.CONTRACTS_BOARD.get()
            || mob.getType() == ModEntities.SALARY_BOARD.get()
            || mob.getType() == ModEntities.SKILLS_BOARD.get()
            || mob.getType() == ModEntities.LEADERBOARD_BOARD.get()
            || mob.getType() == ModEntities.HELP_BOARD.get()) {
            return "native";
        }
        return "wand";
    }

    private static NpcRole parseRole(String roleId) {
        for (NpcRole role : NpcRole.values()) {
            if (role.id().equalsIgnoreCase(roleId)) {
                return role;
            }
        }
        return null;
    }

    private static String effectName(ResourceLocation id) {
        if (ForgeRegistries.MOB_EFFECTS.containsKey(id)) {
            return ForgeRegistries.MOB_EFFECTS.getValue(id).getDisplayName().getString();
        }
        return humanizeAction(resourcePath(id));
    }

    private static String resourcePath(ResourceLocation id) {
        String raw = id.toString();
        int colon = raw.indexOf(':');
        return colon >= 0 ? raw.substring(colon + 1) : raw;
    }
}
