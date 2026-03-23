package com.example.advancedjobs.command;

import com.example.advancedjobs.AdvancedJobsMod;
import com.example.advancedjobs.content.ModEntities;
import com.example.advancedjobs.config.ConfigManager;
import com.example.advancedjobs.economy.ExternalEconomyBridge;
import com.example.advancedjobs.entity.NpcRole;
import com.example.advancedjobs.entity.RoleBasedNpc;
import com.example.advancedjobs.util.DebugLog;
import com.example.advancedjobs.util.TextUtil;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.server.level.ServerPlayer;

public final class JobsAdminCommand {
    private static final SuggestionProvider<CommandSourceStack> NPC_ROLE_SUGGESTIONS =
        (context, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
            java.util.Arrays.stream(NpcRole.values()).map(NpcRole::id), builder);
    private static final SuggestionProvider<CommandSourceStack> NPC_LOCAL_FILE_SUGGESTIONS =
        (context, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
            ConfigManager.npcSkins().localSkinFiles(), builder);
    private static final SuggestionProvider<CommandSourceStack> JOB_SUGGESTIONS =
        (context, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
            ConfigManager.jobs().definitions().keySet(), builder);

    private JobsAdminCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("jobsadmin")
            .requires(src -> src.hasPermission(2))
            .then(Commands.literal("help").executes(ctx -> help(ctx.getSource())))
            .then(Commands.literal("health").executes(ctx -> health(ctx.getSource())))
            .then(Commands.literal("perfcheck").executes(ctx -> perfCheck(ctx.getSource())))
            .then(Commands.literal("economycheck").executes(ctx -> economyCheck(ctx.getSource())))
            .then(Commands.literal("readycheck").executes(ctx -> readyCheck(ctx.getSource())))
            .then(Commands.literal("status").executes(ctx -> status(ctx.getSource())))
            .then(Commands.literal("reload").executes(ctx -> reload(ctx.getSource())))
            .then(Commands.literal("spawnhub").executes(ctx -> spawnHub(ctx.getSource())))
            .then(Commands.literal("repairhub")
                .executes(ctx -> repairHub(ctx.getSource(), 16))
                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 128))
                    .executes(ctx -> repairHub(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radius")))))
            .then(Commands.literal("hubstatus")
                .executes(ctx -> hubStatus(ctx.getSource(), 16))
                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 128))
                    .executes(ctx -> hubStatus(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radius")))))
            .then(Commands.literal("hublist")
                .executes(ctx -> hubList(ctx.getSource(), 16))
                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 128))
                    .executes(ctx -> hubList(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radius")))))
            .then(Commands.literal("exporthub")
                .executes(ctx -> exportHub(ctx.getSource(), 16))
                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 128))
                    .executes(ctx -> exportHub(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radius")))))
            .then(Commands.literal("previewhub")
                .executes(ctx -> previewHub(ctx.getSource(), 16))
                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 128))
                    .executes(ctx -> previewHub(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radius")))))
            .then(Commands.literal("hardenhub")
                .executes(ctx -> hardenHub(ctx.getSource(), 16))
                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 128))
                    .executes(ctx -> hardenHub(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radius")))))
            .then(Commands.literal("doctor")
                .executes(ctx -> doctor(ctx.getSource(), 16))
                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 128))
                    .executes(ctx -> doctor(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radius")))))
            .then(Commands.literal("doctorfix")
                .executes(ctx -> doctorFix(ctx.getSource(), 16))
                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 128))
                    .executes(ctx -> doctorFix(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radius")))))
            .then(Commands.literal("normalizehub")
                .executes(ctx -> normalizeHub(ctx.getSource(), 16))
                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 128))
                    .executes(ctx -> normalizeHub(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radius")))))
            .then(Commands.literal("migratenative")
                .executes(ctx -> migrateNative(ctx.getSource(), 16))
                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 128))
                    .executes(ctx -> migrateNative(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radius")))))
            .then(Commands.literal("alignhub")
                .executes(ctx -> alignHub(ctx.getSource(), 16))
                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 128))
                    .executes(ctx -> alignHub(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radius")))))
            .then(Commands.literal("replacerole")
                .then(Commands.argument("role", StringArgumentType.word()).suggests(NPC_ROLE_SUGGESTIONS)
                    .executes(ctx -> replaceRole(ctx.getSource(), StringArgumentType.getString(ctx, "role"), 16))
                    .then(Commands.argument("radius", IntegerArgumentType.integer(1, 128))
                        .executes(ctx -> replaceRole(ctx.getSource(),
                            StringArgumentType.getString(ctx, "role"),
                            IntegerArgumentType.getInteger(ctx, "radius"))))))
            .then(Commands.literal("inspectrole")
                .then(Commands.argument("role", StringArgumentType.word()).suggests(NPC_ROLE_SUGGESTIONS)
                    .executes(ctx -> inspectRole(ctx.getSource(), StringArgumentType.getString(ctx, "role"), 16))
                    .then(Commands.argument("radius", IntegerArgumentType.integer(1, 128))
                        .executes(ctx -> inspectRole(ctx.getSource(),
                            StringArgumentType.getString(ctx, "role"),
                            IntegerArgumentType.getInteger(ctx, "radius"))))))
            .then(Commands.literal("inspectissues")
                .executes(ctx -> inspectIssues(ctx.getSource(), 16))
                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 128))
                    .executes(ctx -> inspectIssues(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radius")))))
            .then(Commands.literal("clearhub")
                .executes(ctx -> clearHub(ctx.getSource(), 16))
                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 128))
                    .executes(ctx -> clearHub(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radius")))))
            .then(Commands.literal("spawnmaster").executes(ctx -> spawnNpc(ctx.getSource(), ModEntities.JOBS_MASTER.get(),
                "command.advancedjobs.admin.spawnmaster.success",
                "command.advancedjobs.admin.spawnmaster.failed")))
            .then(Commands.literal("spawncontracts").executes(ctx -> spawnNpc(ctx.getSource(), ModEntities.CONTRACTS_BOARD.get(),
                "command.advancedjobs.admin.spawncontracts.success",
                "command.advancedjobs.admin.spawncontracts.failed")))
            .then(Commands.literal("spawndaily").executes(ctx -> spawnNpc(ctx.getSource(), ModEntities.DAILY_BOARD.get(),
                "command.advancedjobs.admin.spawndaily.success",
                "command.advancedjobs.admin.spawndaily.failed")))
            .then(Commands.literal("spawnstatus").executes(ctx -> spawnNpc(ctx.getSource(), ModEntities.STATUS_BOARD.get(),
                "command.advancedjobs.admin.spawnstatus.success",
                "command.advancedjobs.admin.spawnstatus.failed")))
            .then(Commands.literal("spawnsalary").executes(ctx -> spawnNpc(ctx.getSource(), ModEntities.SALARY_BOARD.get(),
                "command.advancedjobs.admin.spawnsalary.success",
                "command.advancedjobs.admin.spawnsalary.failed")))
            .then(Commands.literal("spawnskills").executes(ctx -> spawnNpc(ctx.getSource(), ModEntities.SKILLS_BOARD.get(),
                "command.advancedjobs.admin.spawnskills.success",
                "command.advancedjobs.admin.spawnskills.failed")))
            .then(Commands.literal("spawntop").executes(ctx -> spawnNpc(ctx.getSource(), ModEntities.LEADERBOARD_BOARD.get(),
                "command.advancedjobs.admin.spawntop.success",
                "command.advancedjobs.admin.spawntop.failed")))
            .then(Commands.literal("spawnhelp").executes(ctx -> spawnNpc(ctx.getSource(), ModEntities.HELP_BOARD.get(),
                "command.advancedjobs.admin.spawnhelp.success",
                "command.advancedjobs.admin.spawnhelp.failed")))
            .then(Commands.literal("npcskin")
                .then(Commands.literal("list").executes(ctx -> listNpcSkins(ctx.getSource())))
                .then(Commands.literal("files").executes(ctx -> listNpcSkinFiles(ctx.getSource())))
                .then(Commands.literal("setall")
                    .then(Commands.literal("online")
                        .then(Commands.argument("nickname", StringArgumentType.word())
                            .executes(ctx -> setAllNpcSkinsOnline(ctx.getSource(),
                                StringArgumentType.getString(ctx, "nickname")))))
                    .then(Commands.literal("local")
                        .then(Commands.argument("file", StringArgumentType.string()).suggests(NPC_LOCAL_FILE_SUGGESTIONS)
                            .executes(ctx -> setAllNpcSkinsLocal(ctx.getSource(),
                                StringArgumentType.getString(ctx, "file")))))
                    .then(Commands.literal("reset")
                        .executes(ctx -> resetAllNpcSkins(ctx.getSource()))))
                .then(Commands.literal("copy")
                    .then(Commands.argument("fromRole", StringArgumentType.word()).suggests(NPC_ROLE_SUGGESTIONS)
                        .then(Commands.argument("toRole", StringArgumentType.word()).suggests(NPC_ROLE_SUGGESTIONS)
                            .executes(ctx -> copyNpcSkin(ctx.getSource(),
                                StringArgumentType.getString(ctx, "fromRole"),
                                StringArgumentType.getString(ctx, "toRole"))))))
                .then(Commands.argument("role", StringArgumentType.word()).suggests(NPC_ROLE_SUGGESTIONS)
                    .then(Commands.literal("online")
                        .then(Commands.argument("nickname", StringArgumentType.word())
                            .executes(ctx -> setNpcSkinOnline(ctx.getSource(),
                                StringArgumentType.getString(ctx, "role"),
                                StringArgumentType.getString(ctx, "nickname")))))
                    .then(Commands.literal("local")
                        .then(Commands.argument("file", StringArgumentType.string()).suggests(NPC_LOCAL_FILE_SUGGESTIONS)
                            .executes(ctx -> setNpcSkinLocal(ctx.getSource(),
                                StringArgumentType.getString(ctx, "role"),
                                StringArgumentType.getString(ctx, "file")))))
                    .then(Commands.literal("reset")
                        .executes(ctx -> resetNpcSkin(ctx.getSource(), StringArgumentType.getString(ctx, "role"))))))
            .then(Commands.literal("npclabel")
                .then(Commands.literal("list").executes(ctx -> listNpcLabels(ctx.getSource())))
                .then(Commands.literal("copy")
                    .then(Commands.argument("fromRole", StringArgumentType.word()).suggests(NPC_ROLE_SUGGESTIONS)
                        .then(Commands.argument("toRole", StringArgumentType.word()).suggests(NPC_ROLE_SUGGESTIONS)
                            .executes(ctx -> copyNpcLabel(ctx.getSource(),
                                StringArgumentType.getString(ctx, "fromRole"),
                                StringArgumentType.getString(ctx, "toRole"))))))
                .then(Commands.literal("setall")
                    .then(Commands.argument("label", StringArgumentType.greedyString())
                        .executes(ctx -> setAllNpcLabels(ctx.getSource(), StringArgumentType.getString(ctx, "label")))))
                .then(Commands.literal("resetall")
                    .executes(ctx -> resetAllNpcLabels(ctx.getSource())))
                .then(Commands.argument("role", StringArgumentType.word()).suggests(NPC_ROLE_SUGGESTIONS)
                    .then(Commands.literal("set")
                        .then(Commands.argument("label", StringArgumentType.greedyString())
                            .executes(ctx -> setNpcLabel(ctx.getSource(),
                                StringArgumentType.getString(ctx, "role"),
                                StringArgumentType.getString(ctx, "label")))))
                    .then(Commands.literal("reset")
                        .executes(ctx -> resetNpcLabel(ctx.getSource(), StringArgumentType.getString(ctx, "role"))))))
            .then(Commands.literal("reset")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> reset(EntityArgument.getPlayer(ctx, "player")))))
            .then(Commands.literal("payoutcheck")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> payoutCheck(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
            .then(Commands.literal("routecheck")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> routeCheck(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
            .then(Commands.literal("balancecheck")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> balanceCheck(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
            .then(Commands.literal("balanceoverview")
                .executes(ctx -> balanceOverview(ctx.getSource())))
            .then(Commands.literal("balancejobs")
                .executes(ctx -> balanceJobs(ctx.getSource())))
            .then(Commands.literal("balancejob")
                .then(Commands.argument("job", StringArgumentType.word()).suggests(JOB_SUGGESTIONS)
                    .executes(ctx -> balanceJob(ctx.getSource(), StringArgumentType.getString(ctx, "job")))))
            .then(Commands.literal("balanceprogress")
                .then(Commands.argument("job", StringArgumentType.word()).suggests(JOB_SUGGESTIONS)
                    .executes(ctx -> balanceProgress(ctx.getSource(), StringArgumentType.getString(ctx, "job")))))
            .then(Commands.literal("setjob")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("job", StringArgumentType.word())
                        .executes(ctx -> setJob(EntityArgument.getPlayer(ctx, "player"), StringArgumentType.getString(ctx, "job"), false))
                        .then(Commands.literal("secondary")
                            .executes(ctx -> setJob(EntityArgument.getPlayer(ctx, "player"), StringArgumentType.getString(ctx, "job"), true))))))
            .then(Commands.literal("setlevel")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("job", StringArgumentType.word())
                        .then(Commands.argument("level", IntegerArgumentType.integer(1))
                            .executes(ctx -> setLevel(EntityArgument.getPlayer(ctx, "player"), StringArgumentType.getString(ctx, "job"), IntegerArgumentType.getInteger(ctx, "level")))))))
            .then(Commands.literal("addxp")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("job", StringArgumentType.word())
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0D))
                            .executes(ctx -> addXp(EntityArgument.getPlayer(ctx, "player"), StringArgumentType.getString(ctx, "job"), DoubleArgumentType.getDouble(ctx, "amount")))))))
            .then(Commands.literal("addsalary")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0D))
                        .executes(ctx -> addSalary(EntityArgument.getPlayer(ctx, "player"), DoubleArgumentType.getDouble(ctx, "amount"))))))
            .then(Commands.literal("skillpoints")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                        .executes(ctx -> skillpoints(EntityArgument.getPlayer(ctx, "player"), IntegerArgumentType.getInteger(ctx, "amount"))))))
            .then(Commands.literal("unlockskill")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("job", StringArgumentType.word())
                        .then(Commands.argument("node", StringArgumentType.word())
                            .executes(ctx -> unlockSkill(
                                EntityArgument.getPlayer(ctx, "player"),
                                StringArgumentType.getString(ctx, "job"),
                                StringArgumentType.getString(ctx, "node")))))))
            .then(Commands.literal("eventmultiplier")
                .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0D))
                    .executes(ctx -> eventMultiplier(ctx.getSource(), DoubleArgumentType.getDouble(ctx, "value")))))
            .then(Commands.literal("eventstart")
                .executes(ctx -> startEvent(ctx.getSource(), 2.0D))
                .then(Commands.argument("value", DoubleArgumentType.doubleArg(1.0D))
                    .executes(ctx -> startEvent(ctx.getSource(), DoubleArgumentType.getDouble(ctx, "value"), 60))
                    .then(Commands.argument("minutes", IntegerArgumentType.integer(1))
                        .executes(ctx -> startEvent(ctx.getSource(),
                            DoubleArgumentType.getDouble(ctx, "value"),
                            IntegerArgumentType.getInteger(ctx, "minutes"))))))
            .then(Commands.literal("eventstop")
                .executes(ctx -> stopEvent(ctx.getSource())))
            .then(Commands.literal("antiabuse")
                .executes(ctx -> antiAbuse(ctx.getSource())))
            .then(Commands.literal("caches")
                .executes(ctx -> caches(ctx.getSource())))
            .then(Commands.literal("warmcaches")
                .executes(ctx -> warmCaches(ctx.getSource())))
            .then(Commands.literal("clearcaches")
                .executes(ctx -> clearCaches(ctx.getSource())))
            .then(Commands.literal("debug")
                .then(Commands.literal("on").executes(ctx -> debug(ctx.getSource(), true)))
                .then(Commands.literal("off").executes(ctx -> debug(ctx.getSource(), false)))));
    }

    private static int reload(CommandSourceStack source) {
        AdvancedJobsMod.get().jobManager().reload();
        var manager = AdvancedJobsMod.get().jobManager();
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.reload",
            manager.economy().id(),
            manager.profileCacheSize(),
            manager.jobCount(),
            manager.rewardIndexEntryCount()), true);
        return 1;
    }

    private static int help(CommandSourceStack source) {
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.help.header"), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.help.health"), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.help.perfcheck"), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.help.economycheck"), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.help.readycheck"), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.help.status"), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.help.recovery"), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.help.hub"), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.help.exporthub"), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.help.previewhub"), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.help.hardenhub"), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.help.doctor"), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.help.doctorfix"), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.help.normalizehub"), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.help.migratenative"), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.help.alignhub"), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.help.replacerole"), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.help.inspectrole"), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.help.inspectissues"), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.help.spawns"), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.help.skin"), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.help.label"), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.help.event"), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.help.antiabuse"), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.help.caches"), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.help.warmcaches"), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.help.clearcaches"), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.help.profile"), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.help.payoutcheck"), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.help.routecheck"), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.help.balancecheck"), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.help.balanceoverview"), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.help.balancejobs"), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.help.balancejob"), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.help.balanceprogress"), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.help.debug"), false);
        return 1;
    }

    private static int health(CommandSourceStack source) {
        var manager = AdvancedJobsMod.get().jobManager();
        var antiExploit = AdvancedJobsMod.get().jobEventHandler().antiExploit();
        boolean eventActive = Double.compare(ConfigManager.economy().eventMultiplier(), 1.0D) != 0
            && ConfigManager.economy().eventEndsAtEpochSecond() > com.example.advancedjobs.util.TimeUtil.now();
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.health.header"), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.health.summary",
            source.getServer().getPlayerList().getPlayerCount(),
            manager.profileCacheSize(),
            manager.jobCount(),
            manager.rewardIndexEntryCount(),
            yesNo(DebugLog.enabled())), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.health.economy",
            ConfigManager.economy().providerId(),
            manager.economy().id(),
            ConfigManager.economy().externalCurrencyId(),
            ConfigManager.economy().taxSinkAccountUuid()), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.health.event",
            yesNo(eventActive),
            TextUtil.fmt2(ConfigManager.economy().eventMultiplier()),
            com.example.advancedjobs.util.TimeUtil.formatRemainingSeconds(Math.max(0L,
                ConfigManager.economy().eventEndsAtEpochSecond() - com.example.advancedjobs.util.TimeUtil.now()))), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.health.caches",
            yesNo(manager.isCatalogPayloadCached()),
            manager.catalogPayloadSize(),
            manager.leaderboardCacheCount(),
            yesNo(manager.isOverallLeaderboardCached()),
            ConfigManager.npcSkins().cachedLocalSkinFileCount(),
            ConfigManager.npcSkins().cachedBase64EntryCount()), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.health.runtime",
            antiExploit.trackedPlacedBlocks(),
            antiExploit.trackedRepeatedActions(),
            antiExploit.trackedExploredChunks(),
            antiExploit.trackedLootContainers(),
            antiExploit.trackedArtificialEntities()), false);
        return 1;
    }

    private static int economyCheck(CommandSourceStack source) {
        var manager = AdvancedJobsMod.get().jobManager();
        var externalBridge = new ExternalEconomyBridge();
        boolean externalConfigured = "external".equalsIgnoreCase(ConfigManager.economy().providerId());
        boolean activeExternal = "external".equalsIgnoreCase(manager.economy().id());
        boolean bridgeAvailable = externalBridge.isAvailable();
        boolean sinkValid = ConfigManager.economy().taxSinkAccountId() != null;
        boolean currencyConfigured = ConfigManager.economy().externalCurrencyId() != null
            && !ConfigManager.economy().externalCurrencyId().isBlank();
        boolean releaseReady = !externalConfigured || (activeExternal && bridgeAvailable && sinkValid && currencyConfigured);

        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.economycheck.header"), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.economycheck.provider",
            readyState(!externalConfigured || activeExternal),
            ConfigManager.economy().providerId(),
            manager.economy().id()), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.economycheck.bridge",
            readyState(!externalConfigured || bridgeAvailable),
            yesNo(bridgeAvailable),
            yesNo(externalConfigured)), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.economycheck.currency",
            readyState(currencyConfigured),
            ConfigManager.economy().externalCurrencyId()), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.economycheck.taxsink",
            readyState(sinkValid),
            ConfigManager.economy().taxSinkAccountUuid()), false);
        source.sendSuccess(() -> TextUtil.tr(releaseReady
            ? "command.advancedjobs.admin.economycheck.summary.ready"
            : "command.advancedjobs.admin.economycheck.summary.warn"), false);
        if (externalConfigured && !activeExternal) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.economycheck.advice.provider"), false);
        }
        if (externalConfigured && !bridgeAvailable) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.economycheck.advice.bridge"), false);
        }
        if (!currencyConfigured) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.economycheck.advice.currency"), false);
        }
        if (!sinkValid) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.economycheck.advice.taxsink"), false);
        }
        return releaseReady ? 1 : 0;
    }

    private static int perfCheck(CommandSourceStack source) {
        var manager = AdvancedJobsMod.get().jobManager();
        var antiExploit = AdvancedJobsMod.get().jobEventHandler().antiExploit();
        int onlinePlayers = source.getServer().getPlayerList().getPlayerCount();
        boolean cachesWarm = manager.isCatalogPayloadCached()
            && manager.leaderboardCacheCount() > 0
            && manager.isOverallLeaderboardCached();
        boolean rewardIndexReady = manager.jobCount() > 0 && manager.rewardIndexEntryCount() > 0;
        boolean npcSkinCachesReady = ConfigManager.npcSkins().cachedLocalSkinFileCount() > 0
            || ConfigManager.npcSkins().cachedBase64EntryCount() > 0;
        boolean eventActive = Double.compare(ConfigManager.economy().eventMultiplier(), 1.0D) != 0
            && ConfigManager.economy().eventEndsAtEpochSecond() > com.example.advancedjobs.util.TimeUtil.now();
        boolean debugEnabled = DebugLog.enabled();
        boolean trackerBusy = antiExploit.trackedRepeatedActions() > 0
            || antiExploit.trackedLootContainers() > 0
            || antiExploit.trackedExploredChunks() > 0;
        boolean perfReady = cachesWarm && rewardIndexReady && !eventActive && !debugEnabled;

        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.perfcheck.header"), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.perfcheck.runtime",
            onlinePlayers,
            manager.profileCacheSize(),
            manager.jobCount(),
            manager.rewardIndexEntryCount()), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.perfcheck.caches",
            readyState(cachesWarm),
            yesNo(manager.isCatalogPayloadCached()),
            manager.catalogPayloadSize(),
            manager.leaderboardCacheCount(),
            yesNo(manager.isOverallLeaderboardCached())), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.perfcheck.visual",
            npcSkinCachesReady ? TextUtil.tr("command.advancedjobs.common.ready") : TextUtil.tr("command.advancedjobs.common.warn"),
            ConfigManager.npcSkins().cachedLocalSkinFileCount(),
            ConfigManager.npcSkins().cachedBase64EntryCount()), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.perfcheck.flags",
            readyState(!eventActive && !debugEnabled),
            yesNo(eventActive),
            yesNo(debugEnabled),
            yesNo(trackerBusy)), false);
        source.sendSuccess(() -> TextUtil.tr(perfReady
            ? "command.advancedjobs.admin.perfcheck.summary.ready"
            : "command.advancedjobs.admin.perfcheck.summary.warn"), false);
        if (!cachesWarm) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.perfcheck.advice.caches"), false);
        }
        if (!rewardIndexReady) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.perfcheck.advice.reward_index"), false);
        }
        if (!npcSkinCachesReady) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.perfcheck.advice.visual"), false);
        }
        if (eventActive || debugEnabled) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.perfcheck.advice.runtime"), false);
        }
        if (trackerBusy) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.perfcheck.advice.trackers"), false);
        }
        return perfReady ? 1 : 0;
    }

    private static int status(CommandSourceStack source) {
        var manager = AdvancedJobsMod.get().jobManager();
        var antiExploit = AdvancedJobsMod.get().jobEventHandler().antiExploit();
        boolean economyMismatch = !ConfigManager.economy().providerId().equalsIgnoreCase(manager.economy().id());
        boolean cachesCold = !manager.isCatalogPayloadCached() || manager.leaderboardCacheCount() == 0 || !manager.isOverallLeaderboardCached();
        boolean rewardIndexCold = manager.rewardIndexEntryCount() == 0 || manager.jobCount() == 0;
        boolean npcSkinCachesEmpty = ConfigManager.npcSkins().cachedLocalSkinFileCount() == 0
            && ConfigManager.npcSkins().cachedBase64EntryCount() == 0;
        boolean eventActive = Double.compare(ConfigManager.economy().eventMultiplier(), 1.0D) != 0
            && ConfigManager.economy().eventEndsAtEpochSecond() > com.example.advancedjobs.util.TimeUtil.now();
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.status.header"), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.status.summary",
            source.getServer().getPlayerList().getPlayerCount(),
            yesNo(DebugLog.enabled()),
            manager.profileCacheSize(),
            manager.jobCount()), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.status.economy",
            ConfigManager.economy().providerId(),
            manager.economy().id(),
            ConfigManager.economy().externalCurrencyId(),
            ConfigManager.economy().taxSinkAccountUuid()), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.status.event",
            TextUtil.fmt2(ConfigManager.economy().eventMultiplier()),
            com.example.advancedjobs.util.TimeUtil.formatRemainingSeconds(Math.max(0L,
                ConfigManager.economy().eventEndsAtEpochSecond() - com.example.advancedjobs.util.TimeUtil.now()))), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.status.antiabuse",
            yesNo(ConfigManager.COMMON.blockArtificialMobRewards.get()),
            yesNo(ConfigManager.COMMON.blockBabyMobRewards.get()),
            yesNo(ConfigManager.COMMON.blockTamedMobRewards.get()),
            com.example.advancedjobs.util.TimeUtil.formatRemainingSeconds(ConfigManager.COMMON.lootContainerRewardCooldownSeconds.get()),
            com.example.advancedjobs.util.TimeUtil.formatRemainingSeconds(ConfigManager.COMMON.exploredChunkRewardCooldownSeconds.get())), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.status.antiabuse_runtime",
            antiExploit.trackedPlacedBlocks(),
            antiExploit.trackedRepeatedActions(),
            antiExploit.trackedExploredChunks(),
            antiExploit.trackedLootContainers(),
            antiExploit.trackedArtificialEntities()), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.status.caches",
            manager.profileCacheSize(),
            yesNo(manager.isCatalogPayloadCached()),
            manager.catalogPayloadSize(),
            manager.leaderboardCacheCount(),
            yesNo(manager.isOverallLeaderboardCached()),
            ConfigManager.npcSkins().cachedLocalSkinFileCount(),
            ConfigManager.npcSkins().cachedBase64EntryCount()), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.status.reward_index",
            manager.jobCount(),
            manager.rewardIndexEntryCount()), false);
        if (economyMismatch) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.status.advice.economy",
                ConfigManager.economy().providerId(),
                manager.economy().id()), false);
        }
        if (cachesCold) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.status.advice.caches"), false);
        }
        if (eventActive) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.status.advice.event",
                TextUtil.fmt2(ConfigManager.economy().eventMultiplier()),
                com.example.advancedjobs.util.TimeUtil.formatRemainingSeconds(Math.max(0L,
                    ConfigManager.economy().eventEndsAtEpochSecond() - com.example.advancedjobs.util.TimeUtil.now()))), false);
        }
        if (antiExploit.trackedRepeatedActions() > 0 || antiExploit.trackedLootContainers() > 0 || antiExploit.trackedExploredChunks() > 0) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.status.advice.antiabuse"), false);
        }
        if (rewardIndexCold) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.status.advice.reward_index"), false);
        }
        if (npcSkinCachesEmpty) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.status.advice.npc_skins"), false);
        }
        return 1;
    }

    private static int readyCheck(CommandSourceStack source) {
        var manager = AdvancedJobsMod.get().jobManager();
        boolean jobsLoaded = manager.jobCount() > 0 && manager.rewardIndexEntryCount() > 0;
        boolean externalConfigured = "external".equalsIgnoreCase(ConfigManager.economy().providerId());
        boolean economyReady = !externalConfigured || "external".equalsIgnoreCase(manager.economy().id());
        boolean cachesReady = manager.isCatalogPayloadCached()
            && manager.leaderboardCacheCount() > 0
            && manager.isOverallLeaderboardCached();
        boolean eventActive = Double.compare(ConfigManager.economy().eventMultiplier(), 1.0D) != 0
            && ConfigManager.economy().eventEndsAtEpochSecond() > com.example.advancedjobs.util.TimeUtil.now();
        boolean debugEnabled = DebugLog.enabled();
        boolean npcSkinCachesEmpty = ConfigManager.npcSkins().cachedLocalSkinFileCount() == 0
            && ConfigManager.npcSkins().cachedBase64EntryCount() == 0;
        boolean releaseReady = jobsLoaded && economyReady && cachesReady && !eventActive && !debugEnabled;

        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.readycheck.header"), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.readycheck.jobs",
            readyState(jobsLoaded),
            manager.jobCount(),
            manager.rewardIndexEntryCount()), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.readycheck.economy",
            readyState(economyReady),
            ConfigManager.economy().providerId(),
            manager.economy().id()), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.readycheck.caches",
            readyState(cachesReady),
            yesNo(manager.isCatalogPayloadCached()),
            manager.leaderboardCacheCount(),
            yesNo(manager.isOverallLeaderboardCached())), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.readycheck.visual",
            npcSkinCachesEmpty ? TextUtil.tr("command.advancedjobs.common.warn") : TextUtil.tr("command.advancedjobs.common.ready"),
            ConfigManager.npcSkins().cachedLocalSkinFileCount(),
            ConfigManager.npcSkins().cachedBase64EntryCount()), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.readycheck.runtime",
            readyState(!eventActive && !debugEnabled),
            yesNo(eventActive),
            yesNo(debugEnabled)), false);
        source.sendSuccess(() -> TextUtil.tr(releaseReady
            ? "command.advancedjobs.admin.readycheck.summary.ready"
            : "command.advancedjobs.admin.readycheck.summary.warn"), false);
        if (!jobsLoaded) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.readycheck.advice.jobs"), false);
        }
        if (!economyReady) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.readycheck.advice.economy"), false);
        }
        if (!cachesReady) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.readycheck.advice.caches"), false);
        }
        if (npcSkinCachesEmpty) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.readycheck.advice.visual"), false);
        }
        if (eventActive || debugEnabled) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.readycheck.advice.runtime"), false);
        }
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.readycheck.next"), false);
        return releaseReady ? 1 : 0;
    }

    private static int reset(ServerPlayer player) {
        AdvancedJobsMod.get().jobManager().resetProfile(player);
        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.admin.reset"));
        return 1;
    }

    private static int payoutCheck(CommandSourceStack source, ServerPlayer player) {
        var manager = AdvancedJobsMod.get().jobManager();
        var profile = manager.getOrCreateProfile(player);
        var assignedJobs = assignedJobIds(profile);
        long cooldownRemaining = manager.salaryClaimCooldownRemaining(profile);
        double pendingTotal = assignedJobs.stream().mapToDouble(jobId -> profile.progress(jobId).pendingSalary()).sum();
        double claimCap = Math.max(0.0D, ConfigManager.COMMON.maxSalaryPerClaim.get());
        double grossPreview = Math.min(pendingTotal, claimCap);
        double taxPreview = grossPreview * ConfigManager.COMMON.salaryTaxRate.get();
        double netPreview = Math.max(0.0D, grossPreview - taxPreview);
        boolean instantMode = ConfigManager.COMMON.instantSalary.get();
        boolean hasAssignedJobs = !assignedJobs.isEmpty();
        boolean hasPendingSalary = pendingTotal > 0.0D;
        boolean cooldownActive = cooldownRemaining > 0L;

        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.payoutcheck.header",
            player.getGameProfile().getName()), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.payoutcheck.summary",
            instantMode ? TextUtil.tr("gui.advancedjobs.salary_mode.instant") : TextUtil.tr("gui.advancedjobs.salary_mode.manual"),
            TextUtil.fmt2(pendingTotal),
            TextUtil.fmt2(grossPreview),
            TextUtil.fmt2(netPreview)), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.payoutcheck.rules",
            TextUtil.fmt2(ConfigManager.COMMON.salaryTaxRate.get() * 100.0D),
            TextUtil.fmt2(claimCap),
            com.example.advancedjobs.util.TimeUtil.formatRemainingSeconds(cooldownRemaining)), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.payoutcheck.economy",
            manager.economy().id(),
            ConfigManager.economy().externalCurrencyId(),
            ConfigManager.economy().taxSinkAccountUuid()), false);
        if (assignedJobs.isEmpty()) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.payoutcheck.slot.empty"), false);
        } else {
            for (String jobId : assignedJobs) {
                source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.payoutcheck.slot.line",
                    jobId,
                    TextUtil.fmt2(profile.progress(jobId).pendingSalary()),
                    profile.progress(jobId).level(),
                    TextUtil.fmt2(profile.progress(jobId).earnedTotal())), false);
            }
        }
        if (instantMode) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.payoutcheck.advice.instant"), false);
            return 1;
        }
        if (!hasAssignedJobs) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.payoutcheck.advice.no_jobs"), false);
            return 0;
        }
        if (!hasPendingSalary) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.payoutcheck.advice.empty"), false);
            return 0;
        }
        if (cooldownActive) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.payoutcheck.advice.cooldown",
                com.example.advancedjobs.util.TimeUtil.formatRemainingSeconds(cooldownRemaining)), false);
            return 0;
        }
        if (pendingTotal > claimCap) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.payoutcheck.advice.cap",
                TextUtil.fmt2(claimCap),
                TextUtil.fmt2(pendingTotal - claimCap)), false);
        } else {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.payoutcheck.advice.ready"), false);
        }
        return 1;
    }

    private static int routeCheck(CommandSourceStack source, ServerPlayer player) {
        var manager = AdvancedJobsMod.get().jobManager();
        var profile = manager.getOrCreateProfile(player);
        int radius = 24;

        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.routecheck.header",
            player.getGameProfile().getName(),
            radius), false);
        routeCheckSlot(source, player, profile, false, radius);
        if (profile.secondaryJobId() != null || ConfigManager.COMMON.allowSecondaryJob.get()) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.routecheck.separator"), false);
            routeCheckSlot(source, player, profile, true, radius);
        }
        return 1;
    }

    private static void routeCheckSlot(CommandSourceStack source, ServerPlayer player,
                                       com.example.advancedjobs.model.PlayerJobProfile profile,
                                       boolean secondary, int radius) {
        NpcRole role = JobsCommand.recommendedRole(profile, secondary);
        String nearest = JobsCommand.nearestDistanceForRole(player, role, radius);
        boolean missingDesk = "-".equals(nearest);
        String blocker = JobsCommand.routeBlockerLabel(profile, role, secondary, missingDesk);
        String recovery = blocker == null
            ? JobsCommand.roleActionCommand(role, secondary)
            : JobsCommand.routeRecoveryLabel(profile, role, secondary, missingDesk, radius);

        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.routecheck.slot",
            TextUtil.tr(secondary ? "command.advancedjobs.slot.secondary" : "command.advancedjobs.slot.primary"),
            TextUtil.tr(role.translationKey()),
            nearest), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.routecheck.mode",
            JobsCommand.routeModeLabel(role, profile, secondary),
            JobsCommand.roleActionLabel(role, profile, secondary),
            JobsCommand.roleActionCommand(role, secondary)), false);
        if (blocker == null) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.routecheck.blocker.none"), false);
        } else {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.routecheck.blocker", blocker), false);
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.routecheck.recovery", recovery), false);
        }
    }

    private static int balanceCheck(CommandSourceStack source, ServerPlayer player) {
        var manager = AdvancedJobsMod.get().jobManager();
        var profile = manager.getOrCreateProfile(player);
        var assignedJobs = assignedJobIds(profile);
        double totalEarned = assignedJobs.stream().mapToDouble(jobId -> profile.progress(jobId).earnedTotal()).sum();
        double totalPending = assignedJobs.stream().mapToDouble(jobId -> profile.progress(jobId).pendingSalary()).sum();
        int totalLevels = assignedJobs.stream().mapToInt(jobId -> profile.progress(jobId).level()).sum();
        int totalDailies = assignedJobs.stream().mapToInt(jobId -> profile.progress(jobId).dailyTasks().size()).sum();
        int totalContracts = assignedJobs.stream().mapToInt(jobId -> profile.progress(jobId).contracts().size()).sum();
        long salaryCooldown = manager.salaryClaimCooldownRemaining(profile);

        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.balancecheck.header",
            player.getGameProfile().getName()), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.balancecheck.summary",
            assignedJobs.size(),
            totalLevels,
            TextUtil.fmt2(totalEarned),
            TextUtil.fmt2(totalPending)), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.balancecheck.activity",
            totalDailies,
            totalContracts,
            com.example.advancedjobs.util.TimeUtil.formatRemainingSeconds(salaryCooldown)), false);
        if (assignedJobs.isEmpty()) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.balancecheck.slot.empty"), false);
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.balancecheck.advice.empty"), false);
            return 0;
        }
        for (String jobId : assignedJobs) {
            var progress = profile.progress(jobId);
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.balancecheck.slot.line",
                jobId,
                progress.level(),
                TextUtil.fmt2(progress.xp()),
                TextUtil.fmt2(progress.earnedTotal()),
                TextUtil.fmt2(progress.pendingSalary()),
                profile.availableSkillPoints(jobId),
                progress.dailyTasks().size(),
                progress.contracts().size()), false);
        }
        if (totalLevels <= 10 || totalEarned < 1_000.0D) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.balancecheck.advice.early"), false);
        } else if (totalLevels <= 40 || totalEarned < 10_000.0D) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.balancecheck.advice.mid"), false);
        } else {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.balancecheck.advice.late"), false);
        }
        if (totalPending > ConfigManager.COMMON.maxSalaryPerClaim.get()) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.balancecheck.advice.pending",
                TextUtil.fmt2(ConfigManager.COMMON.maxSalaryPerClaim.get()),
                TextUtil.fmt2(totalPending)), false);
        }
        if (totalContracts == 0) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.balancecheck.advice.contracts"), false);
        }
        if (totalDailies == 0) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.balancecheck.advice.dailies"), false);
        }
        return 1;
    }

    private static int balanceOverview(CommandSourceStack source) {
        var manager = AdvancedJobsMod.get().jobManager();
        var profiles = manager.overallLeaderboard();
        int profiledPlayers = profiles.size();
        double totalEarned = 0.0D;
        double totalPending = 0.0D;
        int totalLevels = 0;
        int activeDailies = 0;
        int activeContracts = 0;
        String topPlayer = "-";
        double topEarned = 0.0D;

        for (var profile : profiles) {
            double earned = manager.totalEarnedAcrossJobs(profile);
            totalEarned += earned;
            totalLevels += manager.totalLevelsAcrossJobs(profile);
            if (earned > topEarned) {
                topEarned = earned;
                topPlayer = profile.playerName();
            }
            for (String jobId : assignedJobIds(profile)) {
                var progress = profile.progress(jobId);
                totalPending += progress.pendingSalary();
                activeDailies += progress.dailyTasks().size();
                activeContracts += progress.contracts().size();
            }
        }

        double avgEarned = profiledPlayers == 0 ? 0.0D : totalEarned / profiledPlayers;
        double avgLevels = profiledPlayers == 0 ? 0.0D : (double) totalLevels / profiledPlayers;
        double totalPendingSnapshot = totalPending;
        int activeDailiesSnapshot = activeDailies;
        int activeContractsSnapshot = activeContracts;
        double totalEarnedSnapshot = totalEarned;
        String topPlayerSnapshot = topPlayer;
        double topEarnedSnapshot = topEarned;
        double maxSalaryPerClaim = ConfigManager.COMMON.maxSalaryPerClaim.get();

        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.balanceoverview.header"), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.balanceoverview.summary",
            profiledPlayers,
            TextUtil.fmt2(avgLevels),
            TextUtil.fmt2(avgEarned),
            TextUtil.fmt2(totalPendingSnapshot)), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.balanceoverview.activity",
            activeDailiesSnapshot,
            activeContractsSnapshot,
            TextUtil.fmt2(totalEarnedSnapshot)), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.balanceoverview.top",
            topPlayerSnapshot,
            TextUtil.fmt2(topEarnedSnapshot)), false);
        if (profiledPlayers == 0) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.balanceoverview.advice.empty"), false);
            return 0;
        }
        if (avgLevels <= 10.0D || avgEarned <= 1_000.0D) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.balanceoverview.advice.early"), false);
        } else if (avgLevels <= 40.0D || avgEarned <= 10_000.0D) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.balanceoverview.advice.mid"), false);
        } else {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.balanceoverview.advice.late"), false);
        }
        if (totalPendingSnapshot > profiledPlayers * Math.max(1.0D, maxSalaryPerClaim)) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.balanceoverview.advice.pending",
                TextUtil.fmt2(totalPendingSnapshot),
                TextUtil.fmt2(maxSalaryPerClaim)), false);
        }
        if (activeContractsSnapshot == 0) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.balanceoverview.advice.contracts"), false);
        }
        if (activeDailiesSnapshot == 0) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.balanceoverview.advice.dailies"), false);
        }
        return 1;
    }

    private static int balanceJobs(CommandSourceStack source) {
        var manager = AdvancedJobsMod.get().jobManager();
        var profiles = manager.overallLeaderboard();
        java.util.Map<String, BalanceJobStats> statsByJob = new java.util.LinkedHashMap<>();
        int assignedSlots = 0;

        for (var profile : profiles) {
            for (String jobId : assignedJobIds(profile)) {
                assignedSlots++;
                var progress = profile.progress(jobId);
                var stats = statsByJob.computeIfAbsent(jobId, ignored -> new BalanceJobStats());
                stats.players++;
                stats.totalLevels += progress.level();
                stats.totalEarned += progress.earnedTotal();
                stats.totalPending += progress.pendingSalary();
                stats.activeDailies += progress.dailyTasks().size();
                stats.activeContracts += progress.contracts().size();
            }
        }

        java.util.List<java.util.Map.Entry<String, BalanceJobStats>> ranked = new java.util.ArrayList<>(statsByJob.entrySet());
        ranked.sort(java.util.Map.Entry.<String, BalanceJobStats>comparingByValue(
            java.util.Comparator.comparingInt((BalanceJobStats stats) -> stats.players).reversed()
                .thenComparingDouble(stats -> stats.totalEarned).reversed()
                .thenComparingInt(stats -> stats.totalLevels).reversed()));
        int assignedSlotsSnapshot = assignedSlots;

        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.balancejobs.header"), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.balancejobs.summary",
            profiles.size(),
            assignedSlotsSnapshot,
            ranked.size()), false);

        if (ranked.isEmpty()) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.balancejobs.advice.empty"), false);
            return 0;
        }

        int limit = Math.min(5, ranked.size());
        for (int i = 0; i < limit; i++) {
            int place = i + 1;
            var entry = ranked.get(i);
            String jobId = entry.getKey();
            BalanceJobStats stats = entry.getValue();
            double share = assignedSlotsSnapshot == 0 ? 0.0D : stats.players * 100.0D / assignedSlotsSnapshot;
            double avgLevel = stats.players == 0 ? 0.0D : (double) stats.totalLevels / stats.players;
            double avgEarned = stats.players == 0 ? 0.0D : stats.totalEarned / stats.players;
            double avgPending = stats.players == 0 ? 0.0D : stats.totalPending / stats.players;
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.balancejobs.line",
                place,
                jobName(jobId),
                stats.players,
                TextUtil.fmt2(share),
                TextUtil.fmt2(avgLevel),
                TextUtil.fmt2(avgEarned),
                TextUtil.fmt2(avgPending),
                stats.activeDailies,
                stats.activeContracts), false);
        }

        var topEntry = ranked.get(0);
        double topShare = assignedSlotsSnapshot == 0 ? 0.0D : topEntry.getValue().players * 100.0D / assignedSlotsSnapshot;
        double avgPendingTop = topEntry.getValue().players == 0 ? 0.0D : topEntry.getValue().totalPending / topEntry.getValue().players;
        if (topShare >= 60.0D) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.balancejobs.advice.dominant",
                jobName(topEntry.getKey()),
                TextUtil.fmt2(topShare)), false);
        }
        if (ranked.size() <= 2) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.balancejobs.advice.shallow"), false);
        }
        if (avgPendingTop > ConfigManager.COMMON.maxSalaryPerClaim.get()) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.balancejobs.advice.pending",
                jobName(topEntry.getKey()),
                TextUtil.fmt2(avgPendingTop),
                TextUtil.fmt2(ConfigManager.COMMON.maxSalaryPerClaim.get())), false);
        }
        if (topEntry.getValue().activeContracts == 0) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.balancejobs.advice.contracts",
                jobName(topEntry.getKey())), false);
        }
        if (topEntry.getValue().activeDailies == 0) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.balancejobs.advice.dailies",
                jobName(topEntry.getKey())), false);
        }
        return 1;
    }

    private static int balanceJob(CommandSourceStack source, String jobId) {
        var manager = AdvancedJobsMod.get().jobManager();
        if (manager.job(jobId).isEmpty()) {
            source.sendFailure(TextUtil.tr("command.advancedjobs.admin.balancejob.invalid", jobId));
            return 0;
        }

        var profiles = manager.overallLeaderboard();
        int profiledPlayers = profiles.size();
        int assignedPlayers = 0;
        int totalLevels = 0;
        double totalEarned = 0.0D;
        double totalPending = 0.0D;
        int activeDailies = 0;
        int activeContracts = 0;
        String topPlayer = "-";
        int topLevel = 0;
        double topEarned = 0.0D;

        for (var profile : profiles) {
            if (!assignedJobIds(profile).contains(jobId)) {
                continue;
            }
            assignedPlayers++;
            var progress = profile.progress(jobId);
            totalLevels += progress.level();
            totalEarned += progress.earnedTotal();
            totalPending += progress.pendingSalary();
            activeDailies += progress.dailyTasks().size();
            activeContracts += progress.contracts().size();
            if (progress.earnedTotal() > topEarned
                || (progress.earnedTotal() == topEarned && progress.level() > topLevel)) {
                topPlayer = profile.playerName();
                topLevel = progress.level();
                topEarned = progress.earnedTotal();
            }
        }

        double share = profiledPlayers == 0 ? 0.0D : assignedPlayers * 100.0D / profiledPlayers;
        double avgLevel = assignedPlayers == 0 ? 0.0D : (double) totalLevels / assignedPlayers;
        double avgEarned = assignedPlayers == 0 ? 0.0D : totalEarned / assignedPlayers;
        double avgPending = assignedPlayers == 0 ? 0.0D : totalPending / assignedPlayers;
        double maxSalaryPerClaim = ConfigManager.COMMON.maxSalaryPerClaim.get();
        int assignedPlayersSnapshot = assignedPlayers;
        int activeDailiesSnapshot = activeDailies;
        int activeContractsSnapshot = activeContracts;
        double totalEarnedSnapshot = totalEarned;
        double totalPendingSnapshot = totalPending;
        String topPlayerSnapshot = topPlayer;
        int topLevelSnapshot = topLevel;
        double topEarnedSnapshot = topEarned;

        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.balancejob.header", jobName(jobId)), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.balancejob.summary",
            assignedPlayersSnapshot,
            profiledPlayers,
            TextUtil.fmt2(share),
            TextUtil.fmt2(avgLevel),
            TextUtil.fmt2(avgEarned),
            TextUtil.fmt2(avgPending)), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.balancejob.activity",
            activeDailiesSnapshot,
            activeContractsSnapshot,
            TextUtil.fmt2(totalEarnedSnapshot),
            TextUtil.fmt2(totalPendingSnapshot)), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.balancejob.top",
            topPlayerSnapshot,
            topLevelSnapshot,
            TextUtil.fmt2(topEarnedSnapshot)), false);

        if (assignedPlayersSnapshot == 0) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.balancejob.advice.empty", jobName(jobId)), false);
            return 0;
        }
        if (share <= 10.0D) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.balancejob.advice.low_share",
                jobName(jobId),
                TextUtil.fmt2(share)), false);
        } else if (share >= 50.0D) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.balancejob.advice.high_share",
                jobName(jobId),
                TextUtil.fmt2(share)), false);
        }
        if (avgLevel <= 10.0D || avgEarned <= 1_000.0D) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.balancejob.advice.early", jobName(jobId)), false);
        } else if (avgLevel >= 40.0D || avgEarned >= 10_000.0D) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.balancejob.advice.late", jobName(jobId)), false);
        }
        if (avgPending > maxSalaryPerClaim) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.balancejob.advice.pending",
                jobName(jobId),
                TextUtil.fmt2(avgPending),
                TextUtil.fmt2(maxSalaryPerClaim)), false);
        }
        if (activeContracts == 0) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.balancejob.advice.contracts", jobName(jobId)), false);
        }
        if (activeDailies == 0) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.balancejob.advice.dailies", jobName(jobId)), false);
        }
        return 1;
    }

    private static int balanceProgress(CommandSourceStack source, String jobId) {
        var manager = AdvancedJobsMod.get().jobManager();
        if (manager.job(jobId).isEmpty()) {
            source.sendFailure(TextUtil.tr("command.advancedjobs.admin.balanceprogress.invalid", jobId));
            return 0;
        }

        var profiles = manager.overallLeaderboard();
        int profiledPlayers = profiles.size();
        int assignedPlayers = 0;
        int totalLevels = 0;
        int totalAvailableSkillPoints = 0;
        int totalSpentSkillPoints = 0;
        int totalUnlockedNodes = 0;
        int totalUnlockedMilestones = 0;
        String topPlayer = "-";
        int topMilestones = 0;
        int topLevel = 0;

        for (var profile : profiles) {
            if (!assignedJobIds(profile).contains(jobId)) {
                continue;
            }
            assignedPlayers++;
            var progress = profile.progress(jobId);
            totalLevels += progress.level();
            totalAvailableSkillPoints += profile.availableSkillPoints(jobId);
            totalSpentSkillPoints += progress.spentSkillPoints();
            totalUnlockedNodes += progress.unlockedNodes().size();
            totalUnlockedMilestones += progress.unlockedMilestones().size();
            if (progress.unlockedMilestones().size() > topMilestones
                || (progress.unlockedMilestones().size() == topMilestones && progress.level() > topLevel)) {
                topPlayer = profile.playerName();
                topMilestones = progress.unlockedMilestones().size();
                topLevel = progress.level();
            }
        }

        double share = profiledPlayers == 0 ? 0.0D : assignedPlayers * 100.0D / profiledPlayers;
        double avgLevel = assignedPlayers == 0 ? 0.0D : (double) totalLevels / assignedPlayers;
        double avgAvailableSkillPoints = assignedPlayers == 0 ? 0.0D : (double) totalAvailableSkillPoints / assignedPlayers;
        double avgSpentSkillPoints = assignedPlayers == 0 ? 0.0D : (double) totalSpentSkillPoints / assignedPlayers;
        double avgUnlockedNodes = assignedPlayers == 0 ? 0.0D : (double) totalUnlockedNodes / assignedPlayers;
        double avgUnlockedMilestones = assignedPlayers == 0 ? 0.0D : (double) totalUnlockedMilestones / assignedPlayers;

        int assignedPlayersSnapshot = assignedPlayers;
        String topPlayerSnapshot = topPlayer;
        int topMilestonesSnapshot = topMilestones;
        int topLevelSnapshot = topLevel;

        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.balanceprogress.header", jobName(jobId)), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.balanceprogress.summary",
            assignedPlayersSnapshot,
            profiledPlayers,
            TextUtil.fmt2(share),
            TextUtil.fmt2(avgLevel),
            TextUtil.fmt2(avgAvailableSkillPoints),
            TextUtil.fmt2(avgSpentSkillPoints)), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.balanceprogress.unlocks",
            TextUtil.fmt2(avgUnlockedNodes),
            TextUtil.fmt2(avgUnlockedMilestones)), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.balanceprogress.top",
            topPlayerSnapshot,
            topLevelSnapshot,
            topMilestonesSnapshot), false);

        if (assignedPlayersSnapshot == 0) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.balanceprogress.advice.empty", jobName(jobId)), false);
            return 0;
        }
        if (share <= 10.0D) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.balanceprogress.advice.low_share",
                jobName(jobId),
                TextUtil.fmt2(share)), false);
        }
        if (avgAvailableSkillPoints >= 2.0D && avgAvailableSkillPoints > avgSpentSkillPoints * 0.5D) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.balanceprogress.advice.unspent",
                jobName(jobId),
                TextUtil.fmt2(avgAvailableSkillPoints)), false);
        }
        if (avgUnlockedNodes <= 0.5D) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.balanceprogress.advice.nodes", jobName(jobId)), false);
        }
        if (avgUnlockedMilestones <= 0.5D && avgLevel >= 10.0D) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.balanceprogress.advice.milestones", jobName(jobId)), false);
        }
        if (avgUnlockedMilestones >= 4.0D && avgLevel <= 12.0D) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.balanceprogress.advice.fast", jobName(jobId)), false);
        }
        return 1;
    }

    private static int setJob(ServerPlayer player, String jobId, boolean secondary) {
        boolean ok = AdvancedJobsMod.get().jobManager().adminSetJob(player, jobId, secondary);
        player.sendSystemMessage(TextUtil.tr(ok
            ? "command.advancedjobs.admin.setjob.success"
            : "command.advancedjobs.admin.setjob.failed"));
        return ok ? 1 : 0;
    }

    private static int setLevel(ServerPlayer player, String jobId, int level) {
        boolean ok = AdvancedJobsMod.get().jobManager().adminSetLevel(player, jobId, level);
        player.sendSystemMessage(TextUtil.tr(ok
            ? "command.advancedjobs.admin.setlevel.success"
            : "command.advancedjobs.admin.setlevel.failed"));
        return ok ? 1 : 0;
    }

    private static int addXp(ServerPlayer player, String jobId, double amount) {
        boolean ok = AdvancedJobsMod.get().jobManager().adminAddXp(player, jobId, amount);
        player.sendSystemMessage(TextUtil.tr(ok
            ? "command.advancedjobs.admin.addxp.success"
            : "command.advancedjobs.admin.addxp.failed"));
        return ok ? 1 : 0;
    }

    private static int addSalary(ServerPlayer player, double amount) {
        boolean ok = AdvancedJobsMod.get().jobManager().adminAddSalary(player, amount);
        player.sendSystemMessage(TextUtil.tr(ok
            ? "command.advancedjobs.admin.addsalary.success"
            : "command.advancedjobs.admin.addsalary.failed"));
        return ok ? 1 : 0;
    }

    private static int skillpoints(ServerPlayer player, int amount) {
        boolean ok = AdvancedJobsMod.get().jobManager().adminAdjustSkillPoints(player, amount);
        player.sendSystemMessage(TextUtil.tr(ok
            ? "command.advancedjobs.admin.skillpoints.success"
            : "command.advancedjobs.admin.skillpoints.failed"));
        return ok ? 1 : 0;
    }

    private static int unlockSkill(ServerPlayer player, String jobId, String nodeId) {
        boolean ok = AdvancedJobsMod.get().jobManager().unlockSkill(player, jobId, nodeId);
        player.sendSystemMessage(TextUtil.tr(ok
            ? "command.advancedjobs.admin.unlockskill.success"
            : "command.advancedjobs.admin.unlockskill.failed"));
        return ok ? 1 : 0;
    }

    private static int debug(CommandSourceStack source, boolean enabled) {
        DebugLog.setEnabled(enabled);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.debug",
            TextUtil.tr(enabled ? "command.advancedjobs.common.enabled" : "command.advancedjobs.common.disabled"),
            ConfigManager.COMMON.debugLogging.get()), true);
        return 1;
    }

    private static int eventMultiplier(CommandSourceStack source, double value) {
        setEventMultiplierAndSync(source, value);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.event_multiplier",
            TextUtil.fmt2(ConfigManager.economy().eventMultiplier()),
            com.example.advancedjobs.util.TimeUtil.formatRemainingSeconds(Math.max(0L,
                ConfigManager.economy().eventEndsAtEpochSecond() - com.example.advancedjobs.util.TimeUtil.now()))), true);
        return 1;
    }

    private static int startEvent(CommandSourceStack source, double value) {
        return startEvent(source, value, 60);
    }

    private static int startEvent(CommandSourceStack source, double value, int minutes) {
        long endsAt = com.example.advancedjobs.util.TimeUtil.now() + (minutes * 60L);
        setEventMultiplierAndSync(source, value, endsAt);
        source.getServer().getPlayerList().broadcastSystemMessage(
            TextUtil.tr("message.advancedjobs.event_started",
                TextUtil.fmt2(ConfigManager.economy().eventMultiplier()),
                com.example.advancedjobs.util.TimeUtil.formatRemainingSeconds(Math.max(0L, endsAt - com.example.advancedjobs.util.TimeUtil.now()))), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.event_started",
            TextUtil.fmt2(ConfigManager.economy().eventMultiplier()),
            com.example.advancedjobs.util.TimeUtil.formatRemainingSeconds(Math.max(0L, endsAt - com.example.advancedjobs.util.TimeUtil.now()))), true);
        return 1;
    }

    private static int stopEvent(CommandSourceStack source) {
        setEventMultiplierAndSync(source, 1.0D, 0L);
        source.getServer().getPlayerList().broadcastSystemMessage(
            TextUtil.tr("message.advancedjobs.event_stopped"), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.event_stopped",
            TextUtil.fmt2(ConfigManager.economy().eventMultiplier()),
            com.example.advancedjobs.util.TimeUtil.formatRemainingSeconds(Math.max(0L,
                ConfigManager.economy().eventEndsAtEpochSecond() - com.example.advancedjobs.util.TimeUtil.now()))), true);
        return 1;
    }

    private static int antiAbuse(CommandSourceStack source) {
        var antiExploit = AdvancedJobsMod.get().jobEventHandler().antiExploit();
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.antiabuse.header"), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.antiabuse.mob_filters",
            yesNo(ConfigManager.COMMON.blockArtificialMobRewards.get()),
            yesNo(ConfigManager.COMMON.blockBabyMobRewards.get()),
            yesNo(ConfigManager.COMMON.blockTamedMobRewards.get())), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.antiabuse.loot_chunk_filters",
            com.example.advancedjobs.util.TimeUtil.formatRemainingSeconds(ConfigManager.COMMON.lootContainerRewardCooldownSeconds.get()),
            com.example.advancedjobs.util.TimeUtil.formatRemainingSeconds(ConfigManager.COMMON.exploredChunkRewardCooldownSeconds.get())), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.antiabuse.kill_decay",
            ConfigManager.COMMON.repeatedKillDecayThreshold.get(),
            yesNo(ConfigManager.COMMON.blockArtificialMobRewards.get())), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.antiabuse.runtime",
            antiExploit.trackedPlacedBlocks(),
            antiExploit.trackedRepeatedActions(),
            antiExploit.trackedExploredChunks(),
            antiExploit.trackedLootContainers(),
            antiExploit.trackedArtificialEntities()), false);
        return 1;
    }

    private static int caches(CommandSourceStack source) {
        var manager = AdvancedJobsMod.get().jobManager();
        boolean rewardIndexCold = manager.rewardIndexEntryCount() == 0 || manager.jobCount() == 0;
        boolean npcSkinCachesEmpty = ConfigManager.npcSkins().cachedLocalSkinFileCount() == 0
            && ConfigManager.npcSkins().cachedBase64EntryCount() == 0;
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.caches.header"), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.caches.runtime",
            manager.profileCacheSize(),
            manager.jobCount()), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.caches.catalog",
            yesNo(manager.isCatalogPayloadCached()),
            manager.catalogPayloadSize()), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.caches.leaderboards",
            manager.leaderboardCacheCount(),
            yesNo(manager.isOverallLeaderboardCached())), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.caches.reward_index",
            manager.rewardIndexJobCount(),
            manager.rewardIndexEntryCount()), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.caches.npc_skins",
            ConfigManager.npcSkins().cachedLocalSkinFileCount(),
            ConfigManager.npcSkins().cachedBase64EntryCount()), false);
        if (rewardIndexCold) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.caches.advice.reward_index"), false);
        }
        if (npcSkinCachesEmpty) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.caches.advice.npc_skins"), false);
        }
        return 1;
    }

    private static int warmCaches(CommandSourceStack source) {
        var manager = AdvancedJobsMod.get().jobManager();
        var stats = manager.warmCaches();
        int warmedLocalSkins = ConfigManager.npcSkins().warmCaches();
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.warmcaches",
            stats.profileCount(),
            stats.jobCount(),
            stats.catalogPayloadChars(),
            stats.warmedLeaderboards(),
            yesNo(stats.overallLeaderboardCached()),
            warmedLocalSkins,
            ConfigManager.npcSkins().cachedBase64EntryCount()), true);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.warmcaches.note",
            yesNo(manager.isCatalogPayloadCached()),
            manager.catalogPayloadSize(),
            manager.leaderboardCacheCount(),
            yesNo(manager.isOverallLeaderboardCached())), false);
        return 1;
    }

    private static int clearCaches(CommandSourceStack source) {
        AdvancedJobsMod.get().jobManager().clearCaches();
        ConfigManager.npcSkins().clearCaches();
        var manager = AdvancedJobsMod.get().jobManager();
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.clearcaches",
            yesNo(manager.isCatalogPayloadCached()),
            manager.leaderboardCacheCount(),
            yesNo(manager.isOverallLeaderboardCached()),
            ConfigManager.npcSkins().cachedLocalSkinFileCount(),
            ConfigManager.npcSkins().cachedBase64EntryCount()), true);
        return 1;
    }

    private static Component yesNo(boolean value) {
        return TextUtil.tr(value ? "command.advancedjobs.common.enabled" : "command.advancedjobs.common.disabled");
    }

    private static Component readyState(boolean value) {
        return TextUtil.tr(value ? "command.advancedjobs.common.ready" : "command.advancedjobs.common.warn");
    }

    private static Component jobName(String jobId) {
        if (jobId == null) {
            return TextUtil.tr("command.advancedjobs.common.none");
        }
        return AdvancedJobsMod.get().jobManager().job(jobId)
            .map(definition -> TextUtil.tr(definition.translationKey()))
            .orElse(Component.literal(jobId));
    }

    private static java.util.List<String> assignedJobIds(com.example.advancedjobs.model.PlayerJobProfile profile) {
        java.util.List<String> ids = new java.util.ArrayList<>();
        if (profile.activeJobId() != null) {
            ids.add(profile.activeJobId());
        }
        if (ConfigManager.COMMON.allowSecondaryJob.get() && profile.secondaryJobId() != null) {
            ids.add(profile.secondaryJobId());
        }
        return ids;
    }

    private static final class BalanceJobStats {
        private int players;
        private int totalLevels;
        private double totalEarned;
        private double totalPending;
        private int activeDailies;
        private int activeContracts;
    }

    private static void setEventMultiplierAndSync(CommandSourceStack source, double value) {
        setEventMultiplierAndSync(source, value, 0L);
    }

    private static void setEventMultiplierAndSync(CommandSourceStack source, double value, long endsAt) {
        ConfigManager.economy().setEventMultiplier(value);
        ConfigManager.economy().setEventEndsAtEpochSecond(endsAt);
        ConfigManager.saveEconomyConfig();
        source.getServer().getPlayerList().getPlayers().forEach(player -> AdvancedJobsMod.get().jobManager().syncToPlayer(player));
    }

    private static int spawnHub(CommandSourceStack source) {
        var level = source.getLevel();
        double baseX = source.getPosition().x;
        double baseY = source.getPosition().y;
        double baseZ = source.getPosition().z;
        float yaw = source.getRotation().y;
        int spawned = 0;
        for (HubNpcPlacement placement : hubPlacements(baseX, baseY, baseZ, yaw)) {
            spawned += spawnNpcAt(level, placement.type(), placement.x(), placement.y(), placement.z(), placement.yaw()) ? 1 : 0;
        }
        if (spawned == 0) {
            source.sendFailure(TextUtil.tr("command.advancedjobs.admin.spawnhub.failed"));
            return 0;
        }
        final int spawnedCount = spawned;
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.spawnhub.success", spawnedCount), true);
        return 1;
    }

    private static int repairHub(CommandSourceStack source, int radius) {
        var level = source.getLevel();
        var center = source.getPosition();
        double radiusSq = radius * radius;
        java.util.Set<String> presentRoles = new java.util.LinkedHashSet<>();
        for (var entity : level.getEntities(null, new net.minecraft.world.phys.AABB(
            center.x - radius, center.y - radius, center.z - radius,
            center.x + radius, center.y + radius, center.z + radius))) {
            if (!(entity instanceof Mob mob)) {
                continue;
            }
            if (mob.distanceToSqr(center.x, center.y, center.z) > radiusSq) {
                continue;
            }
            String roleId = roleIdForServiceNpc(mob);
            if (roleId != null) {
                presentRoles.add(roleId);
            }
        }
        int spawned = 0;
        for (HubNpcPlacement placement : hubPlacements(center.x, center.y, center.z, source.getRotation().y)) {
            if (presentRoles.contains(placement.role().id())) {
                continue;
            }
            spawned += spawnNpcAt(level, placement.type(), placement.x(), placement.y(), placement.z(), placement.yaw()) ? 1 : 0;
        }
        final int spawnedCount = spawned;
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.repairhub.success", spawnedCount, radius), true);
        return 1;
    }

    private static int clearHub(CommandSourceStack source, int radius) {
        var level = source.getLevel();
        var center = source.getPosition();
        double radiusSq = radius * radius;
        int removed = 0;
        for (var entity : level.getEntities(null, new net.minecraft.world.phys.AABB(
            center.x - radius, center.y - radius, center.z - radius,
            center.x + radius, center.y + radius, center.z + radius))) {
            if (!(entity instanceof Mob mob)) {
                continue;
            }
            if (mob.distanceToSqr(center.x, center.y, center.z) > radiusSq) {
                continue;
            }
            if (!isServiceNpc(mob)) {
                continue;
            }
            mob.discard();
            removed++;
        }
        final int removedCount = removed;
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.clearhub.success", removedCount, radius), true);
        return removed > 0 ? 1 : 0;
    }

    private static int hubStatus(CommandSourceStack source, int radius) {
        var level = source.getLevel();
        var center = source.getPosition();
        double radiusSq = radius * radius;
        java.util.Map<String, Integer> counts = new java.util.LinkedHashMap<>();
        for (NpcRole role : NpcRole.values()) {
            counts.put(role.id(), 0);
        }
        int total = 0;
        for (var entity : level.getEntities(null, new net.minecraft.world.phys.AABB(
            center.x - radius, center.y - radius, center.z - radius,
            center.x + radius, center.y + radius, center.z + radius))) {
            if (!(entity instanceof Mob mob)) {
                continue;
            }
            if (mob.distanceToSqr(center.x, center.y, center.z) > radiusSq) {
                continue;
            }
            String roleId = roleIdForServiceNpc(mob);
            if (roleId == null) {
                continue;
            }
            counts.merge(roleId, 1, Integer::sum);
            total++;
        }
        final int totalCount = total;
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.hubstatus.header", totalCount, radius), false);
        for (NpcRole role : NpcRole.values()) {
            final int count = counts.getOrDefault(role.id(), 0);
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.hubstatus.line",
                TextUtil.tr(role.translationKey()),
                role.id(),
                count,
                ConfigManager.npcLabels().label(role.id())), false);
        }
        return 1;
    }

    private static int hubList(CommandSourceStack source, int radius) {
        var level = source.getLevel();
        var center = source.getPosition();
        double radiusSq = radius * radius;
        java.util.List<HubNpcEntry> entries = new java.util.ArrayList<>();
        for (var entity : level.getEntities(null, new net.minecraft.world.phys.AABB(
            center.x - radius, center.y - radius, center.z - radius,
            center.x + radius, center.y + radius, center.z + radius))) {
            if (!(entity instanceof Mob mob)) {
                continue;
            }
            if (mob.distanceToSqr(center.x, center.y, center.z) > radiusSq) {
                continue;
            }
            String roleId = roleIdForServiceNpc(mob);
            if (roleId == null) {
                continue;
            }
            entries.add(new HubNpcEntry(
                roleId,
                roleSource(mob),
                mob.blockPosition().getX(),
                mob.blockPosition().getY(),
                mob.blockPosition().getZ(),
                mob.getStringUUID()));
        }
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.hublist.header", entries.size(), radius), false);
        for (HubNpcEntry entry : entries) {
            NpcRole role = parseRole(entry.roleId());
            String roleName = role != null ? TextUtil.tr(role.translationKey()).getString() : entry.roleId();
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.hublist.line",
                roleName,
                entry.roleId(),
                entry.source(),
                entry.x(),
                entry.y(),
                entry.z(),
                entry.uuid()), false);
        }
        return 1;
    }

    private static int exportHub(CommandSourceStack source, int radius) {
        var level = source.getLevel();
        var center = source.getPosition();
        double radiusSq = radius * radius;
        java.util.List<HubNpcEntry> entries = new java.util.ArrayList<>();
        for (var entity : level.getEntities(null, new net.minecraft.world.phys.AABB(
            center.x - radius, center.y - radius, center.z - radius,
            center.x + radius, center.y + radius, center.z + radius))) {
            if (!(entity instanceof Mob mob)) {
                continue;
            }
            if (mob.distanceToSqr(center.x, center.y, center.z) > radiusSq) {
                continue;
            }
            String roleId = roleIdForServiceNpc(mob);
            if (roleId == null) {
                continue;
            }
            entries.add(new HubNpcEntry(
                roleId,
                roleSource(mob),
                mob.blockPosition().getX(),
                mob.blockPosition().getY(),
                mob.blockPosition().getZ(),
                mob.getStringUUID()));
        }
        entries.sort(java.util.Comparator.comparing(HubNpcEntry::roleId).thenComparing(HubNpcEntry::source));
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.exporthub.header", entries.size(), radius), false);
        for (HubNpcEntry entry : entries) {
            NpcRole role = parseRole(entry.roleId());
            String roleName = role != null ? TextUtil.tr(role.translationKey()).getString() : entry.roleId();
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.exporthub.line",
                roleName,
                entry.roleId(),
                entry.source(),
                ConfigManager.npcLabels().label(entry.roleId()),
                entry.x(),
                entry.y(),
                entry.z()), false);
        }
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.exporthub.commands"), false);
        source.sendSuccess(() -> Component.literal("/jobsadmin doctor " + radius), false);
        source.sendSuccess(() -> Component.literal("/jobsadmin doctorfix " + radius), false);
        source.sendSuccess(() -> Component.literal("/jobsadmin migratenative " + radius), false);
        source.sendSuccess(() -> Component.literal("/jobsadmin normalizehub " + radius), false);
        source.sendSuccess(() -> Component.literal("/jobsadmin alignhub " + radius), false);
        return 1;
    }

    private static int previewHub(CommandSourceStack source, int radius) {
        var level = source.getLevel();
        var center = source.getPosition();
        double radiusSq = radius * radius;
        java.util.Map<String, Integer> counts = collectServiceRoleCounts(source, radius);
        int missing = 0;
        int duplicates = 0;
        int fallback = 0;
        int brokenSkins = 0;
        int emptyLabels = 0;
        for (NpcRole role : NpcRole.values()) {
            int count = counts.getOrDefault(role.id(), 0);
            if (count == 0) {
                missing++;
            } else if (count > 1) {
                duplicates += count - 1;
            }
            var profile = ConfigManager.npcSkins().profile(role.id());
            String type = profile.has("type") ? profile.get("type").getAsString() : "online";
            String value = profile.has("value") ? profile.get("value").getAsString() : "";
            if ("local".equalsIgnoreCase(type) && !ConfigManager.npcSkins().localSkinFileExists(value)) {
                brokenSkins++;
            }
            String label = ConfigManager.npcLabels().label(role.id());
            if (label == null || label.isBlank()) {
                emptyLabels++;
            }
        }
        for (var entity : level.getEntities(null, new net.minecraft.world.phys.AABB(
            center.x - radius, center.y - radius, center.z - radius,
            center.x + radius, center.y + radius, center.z + radius))) {
            if (!(entity instanceof Mob mob)) {
                continue;
            }
            if (mob.distanceToSqr(center.x, center.y, center.z) > radiusSq) {
                continue;
            }
            if (roleIdForFallbackNpc(mob) != null) {
                fallback++;
            }
        }
        final int previewRadius = radius;
        final int missingCount = missing;
        final int duplicateCount = duplicates;
        final int fallbackCount = fallback;
        final int brokenSkinCount = brokenSkins;
        final int emptyLabelCount = emptyLabels;
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.previewhub.header", previewRadius), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.previewhub.summary",
            missingCount, duplicateCount, fallbackCount, brokenSkinCount, emptyLabelCount), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.previewhub.commands"), false);
        source.sendSuccess(() -> Component.literal("/jobsadmin hardenhub " + radius), false);
        return 1;
    }

    private static int hardenHub(CommandSourceStack source, int radius) {
        int fix = doctorFix(source, radius);
        int migrate = migrateNative(source, radius);
        int normalize = normalizeHub(source, radius);
        int align = alignHub(source, radius);
        final int hardenRadius = radius;
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.hardenhub.summary",
            hardenRadius, fix, migrate, normalize, align), true);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.hardenhub.note"), false);
        return 1;
    }

    private static int doctor(CommandSourceStack source, int radius) {
        java.util.Map<String, Integer> counts = collectServiceRoleCounts(source, radius);
        int issues = 0;
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.doctor.header", radius), false);
        for (NpcRole role : NpcRole.values()) {
            int count = counts.getOrDefault(role.id(), 0);
            if (count == 0) {
                issues++;
                source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.doctor.missing",
                    TextUtil.tr(role.translationKey()), role.id()), false);
            } else if (count > 1) {
                issues++;
                source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.doctor.duplicate",
                    TextUtil.tr(role.translationKey()), role.id(), count), false);
            }

            var profile = ConfigManager.npcSkins().profile(role.id());
            String type = profile.has("type") ? profile.get("type").getAsString() : "online";
            String value = profile.has("value") ? profile.get("value").getAsString() : "";
            if ("local".equalsIgnoreCase(type) && !ConfigManager.npcSkins().localSkinFileExists(value)) {
                issues++;
                source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.doctor.skin_missing",
                    TextUtil.tr(role.translationKey()), value), false);
            }

            String label = ConfigManager.npcLabels().label(role.id());
            if (label == null || label.isBlank()) {
                issues++;
                source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.doctor.label_empty",
                    TextUtil.tr(role.translationKey()), role.id()), false);
            }
        }
        final int totalIssues = issues;
        source.sendSuccess(() -> TextUtil.tr(
            totalIssues == 0
                ? "command.advancedjobs.admin.doctor.clean"
                : "command.advancedjobs.admin.doctor.summary",
            totalIssues), false);
        return 1;
    }

    private static int doctorFix(CommandSourceStack source, int radius) {
        var level = source.getLevel();
        var center = source.getPosition();
        java.util.Map<String, Integer> counts = collectServiceRoleCounts(source, radius);
        int spawned = 0;
        for (HubNpcPlacement placement : hubPlacements(center.x, center.y, center.z, source.getRotation().y)) {
            if (counts.getOrDefault(placement.role().id(), 0) > 0) {
                continue;
            }
            spawned += spawnNpcAt(level, placement.type(), placement.x(), placement.y(), placement.z(), placement.yaw()) ? 1 : 0;
        }

        int skinResets = 0;
        int labelResets = 0;
        for (NpcRole role : NpcRole.values()) {
            var profile = ConfigManager.npcSkins().profile(role.id());
            String type = profile.has("type") ? profile.get("type").getAsString() : "online";
            String value = profile.has("value") ? profile.get("value").getAsString() : "";
            if ("local".equalsIgnoreCase(type) && !ConfigManager.npcSkins().localSkinFileExists(value)) {
                ConfigManager.npcSkins().resetProfile(role.id());
                skinResets++;
            }
            String label = ConfigManager.npcLabels().label(role.id());
            if (label == null || label.isBlank()) {
                ConfigManager.npcLabels().resetLabel(role.id());
                labelResets++;
            }
        }

        if (labelResets > 0) {
            refreshNpcLabels(source.getServer());
        }
        if (skinResets > 0) {
            AdvancedJobsMod.get().jobManager().syncCatalogToAllPlayers();
        }
        final int fixedRadius = radius;
        final int spawnedCount = spawned;
        final int skinResetCount = skinResets;
        final int labelResetCount = labelResets;
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.doctorfix.summary",
            fixedRadius, spawnedCount, skinResetCount, labelResetCount), true);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.doctorfix.note"), false);
        return 1;
    }

    private static int normalizeHub(CommandSourceStack source, int radius) {
        var level = source.getLevel();
        var center = source.getPosition();
        double radiusSq = radius * radius;
        java.util.Map<String, java.util.List<Mob>> byRole = new java.util.LinkedHashMap<>();
        for (NpcRole role : NpcRole.values()) {
            byRole.put(role.id(), new java.util.ArrayList<>());
        }
        for (var entity : level.getEntities(null, new net.minecraft.world.phys.AABB(
            center.x - radius, center.y - radius, center.z - radius,
            center.x + radius, center.y + radius, center.z + radius))) {
            if (!(entity instanceof Mob mob)) {
                continue;
            }
            if (mob.distanceToSqr(center.x, center.y, center.z) > radiusSq) {
                continue;
            }
            String roleId = roleIdForServiceNpc(mob);
            if (roleId == null) {
                continue;
            }
            byRole.computeIfAbsent(roleId, key -> new java.util.ArrayList<>()).add(mob);
        }

        int removed = 0;
        for (NpcRole role : NpcRole.values()) {
            java.util.List<Mob> entries = byRole.get(role.id());
            if (entries == null || entries.size() <= 1) {
                continue;
            }
            entries.sort(java.util.Comparator
                .comparingInt((Mob mob) -> "native".equals(roleSource(mob)) ? 0 : 1)
                .thenComparingDouble(mob -> mob.distanceToSqr(center.x, center.y, center.z)));
            for (int i = 1; i < entries.size(); i++) {
                entries.get(i).discard();
                removed++;
            }
        }

        final int normalizedRadius = radius;
        final int removedCount = removed;
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.normalizehub.summary",
            normalizedRadius, removedCount), true);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.normalizehub.note"), false);
        return 1;
    }

    private static int migrateNative(CommandSourceStack source, int radius) {
        var level = source.getLevel();
        var center = source.getPosition();
        double radiusSq = radius * radius;
        java.util.List<Mob> fallbackMobs = new java.util.ArrayList<>();
        int migrated = 0;
        for (var entity : level.getEntities(null, new net.minecraft.world.phys.AABB(
            center.x - radius, center.y - radius, center.z - radius,
            center.x + radius, center.y + radius, center.z + radius))) {
            if (!(entity instanceof Mob mob)) {
                continue;
            }
            if (mob.distanceToSqr(center.x, center.y, center.z) > radiusSq) {
                continue;
            }
            String roleId = roleIdForFallbackNpc(mob);
            if (roleId == null) {
                continue;
            }
            NpcRole role = parseRole(roleId);
            EntityType<? extends Mob> nativeType = role == null ? null : nativeEntityType(role);
            if (nativeType == null) {
                continue;
            }
            if (spawnNpcAt(level, nativeType, mob.getX(), mob.getY(), mob.getZ(), mob.getYRot())) {
                fallbackMobs.add(mob);
                migrated++;
            }
        }
        fallbackMobs.forEach(Mob::discard);
        final int migratedCount = migrated;
        final int migrateRadius = radius;
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.migratenative.summary",
            migrateRadius, migratedCount), true);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.migratenative.note"), false);
        return 1;
    }

    private static int alignHub(CommandSourceStack source, int radius) {
        var level = source.getLevel();
        var center = source.getPosition();
        double radiusSq = radius * radius;
        java.util.Map<String, java.util.List<Mob>> byRole = new java.util.LinkedHashMap<>();
        for (NpcRole role : NpcRole.values()) {
            byRole.put(role.id(), new java.util.ArrayList<>());
        }
        for (var entity : level.getEntities(null, new net.minecraft.world.phys.AABB(
            center.x - radius, center.y - radius, center.z - radius,
            center.x + radius, center.y + radius, center.z + radius))) {
            if (!(entity instanceof Mob mob)) {
                continue;
            }
            if (mob.distanceToSqr(center.x, center.y, center.z) > radiusSq) {
                continue;
            }
            String roleId = roleIdForServiceNpc(mob);
            if (roleId == null) {
                continue;
            }
            byRole.computeIfAbsent(roleId, key -> new java.util.ArrayList<>()).add(mob);
        }

        int moved = 0;
        for (HubNpcPlacement placement : hubPlacements(center.x, center.y, center.z, source.getRotation().y)) {
            java.util.List<Mob> entries = byRole.get(placement.role().id());
            if (entries == null || entries.isEmpty()) {
                continue;
            }
            entries.sort(java.util.Comparator
                .comparingInt((Mob mob) -> "native".equals(roleSource(mob)) ? 0 : 1)
                .thenComparingDouble(mob -> mob.distanceToSqr(placement.x(), placement.y(), placement.z())));
            Mob chosen = entries.get(0);
            chosen.moveTo(placement.x(), placement.y(), placement.z(), placement.yaw(), chosen.getXRot());
            moved++;
        }

        final int alignedRadius = radius;
        final int movedCount = moved;
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.alignhub.summary",
            alignedRadius, movedCount), true);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.alignhub.note"), false);
        return 1;
    }

    private static int replaceRole(CommandSourceStack source, String roleId, int radius) {
        NpcRole role = parseRole(roleId);
        if (role == null) {
            source.sendFailure(TextUtil.tr("command.advancedjobs.admin.npcskin.invalid_role", roleId));
            return 0;
        }
        EntityType<? extends Mob> nativeType = nativeEntityType(role);
        if (nativeType == null) {
            source.sendFailure(TextUtil.tr("command.advancedjobs.admin.replacerole.unsupported", roleId));
            return 0;
        }
        var target = findClosestServiceRole(source, role.id(), radius);
        if (target == null) {
            source.sendFailure(TextUtil.tr("command.advancedjobs.admin.replacerole.not_found",
                TextUtil.tr(role.translationKey()), radius));
            return 0;
        }
        var level = source.getLevel();
        boolean spawned = spawnNpcAt(level, nativeType, target.getX(), target.getY(), target.getZ(), target.getYRot());
        if (!spawned) {
            source.sendFailure(TextUtil.tr("command.advancedjobs.admin.replacerole.failed",
                TextUtil.tr(role.translationKey())));
            return 0;
        }
        target.discard();
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.replacerole.success",
            TextUtil.tr(role.translationKey()),
            roleSource(target),
            radius), true);
        return 1;
    }

    private static int inspectRole(CommandSourceStack source, String roleId, int radius) {
        NpcRole role = parseRole(roleId);
        if (role == null) {
            source.sendFailure(TextUtil.tr("command.advancedjobs.admin.npcskin.invalid_role", roleId));
            return 0;
        }
        Mob target = findClosestServiceRole(source, role.id(), radius);
        if (target == null) {
            source.sendFailure(TextUtil.tr("command.advancedjobs.admin.replacerole.not_found",
                TextUtil.tr(role.translationKey()), radius));
            return 0;
        }
        var profile = ConfigManager.npcSkins().profile(role.id());
        String skinType = profile.has("type") ? profile.get("type").getAsString() : "online";
        String skinValue = profile.has("value") ? profile.get("value").getAsString() : "-";
        String skinState = "local".equalsIgnoreCase(skinType)
            ? (ConfigManager.npcSkins().localSkinFileExists(skinValue) ? "ok" : "missing")
            : "-";
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.inspectrole.header",
            TextUtil.tr(role.translationKey()), radius), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.inspectrole.entity",
            roleSource(target),
            target.blockPosition().getX(),
            target.blockPosition().getY(),
            target.blockPosition().getZ(),
            target.getStringUUID()), false);
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.inspectrole.presentation",
            ConfigManager.npcLabels().label(role.id()),
            skinType,
            skinValue,
            skinState), false);
        return 1;
    }

    private static int inspectIssues(CommandSourceStack source, int radius) {
        java.util.Map<String, Integer> counts = collectServiceRoleCounts(source, radius);
        int issues = 0;
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.inspectissues.header", radius), false);
        for (NpcRole role : NpcRole.values()) {
            int count = counts.getOrDefault(role.id(), 0);
            var profile = ConfigManager.npcSkins().profile(role.id());
            String type = profile.has("type") ? profile.get("type").getAsString() : "online";
            String value = profile.has("value") ? profile.get("value").getAsString() : "";
            String localState = "local".equalsIgnoreCase(type)
                ? (ConfigManager.npcSkins().localSkinFileExists(value) ? "ok" : "missing")
                : "-";
            String label = ConfigManager.npcLabels().label(role.id());
            boolean hasIssue = count != 1
                || ("local".equalsIgnoreCase(type) && !"ok".equals(localState))
                || label == null
                || label.isBlank();
            if (!hasIssue) {
                continue;
            }
            issues++;
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.inspectissues.line",
                TextUtil.tr(role.translationKey()),
                role.id(),
                count,
                type,
                value,
                localState,
                label == null || label.isBlank() ? "<empty>" : label), false);
        }
        final int totalIssues = issues;
        source.sendSuccess(() -> TextUtil.tr(
            totalIssues == 0
                ? "command.advancedjobs.admin.inspectissues.clean"
                : "command.advancedjobs.admin.inspectissues.summary",
            totalIssues), false);
        return 1;
    }

    private static int setNpcSkinOnline(CommandSourceStack source, String roleId, String nickname) {
        NpcRole role = parseRole(roleId);
        if (role == null) {
            source.sendFailure(TextUtil.tr("command.advancedjobs.admin.npcskin.invalid_role", roleId));
            return 0;
        }
        ConfigManager.npcSkins().setProfile(role.id(), "online", nickname);
        AdvancedJobsMod.get().jobManager().syncCatalogToAllPlayers();
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.npcskin.updated",
            TextUtil.tr(role.translationKey()), "online", nickname), true);
        return 1;
    }

    private static int setNpcSkinLocal(CommandSourceStack source, String roleId, String file) {
        NpcRole role = parseRole(roleId);
        if (role == null) {
            source.sendFailure(TextUtil.tr("command.advancedjobs.admin.npcskin.invalid_role", roleId));
            return 0;
        }
        if (!ConfigManager.npcSkins().localSkinFileExists(file)) {
            source.sendFailure(TextUtil.tr("command.advancedjobs.admin.npcskin.local_missing", file));
            return 0;
        }
        ConfigManager.npcSkins().setProfile(role.id(), "local", file);
        AdvancedJobsMod.get().jobManager().syncCatalogToAllPlayers();
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.npcskin.updated",
            TextUtil.tr(role.translationKey()), "local", file), true);
        return 1;
    }

    private static int resetNpcSkin(CommandSourceStack source, String roleId) {
        NpcRole role = parseRole(roleId);
        if (role == null) {
            source.sendFailure(TextUtil.tr("command.advancedjobs.admin.npcskin.invalid_role", roleId));
            return 0;
        }
        ConfigManager.npcSkins().resetProfile(role.id());
        AdvancedJobsMod.get().jobManager().syncCatalogToAllPlayers();
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.npcskin.reset",
            TextUtil.tr(role.translationKey())), true);
        return 1;
    }

    private static int listNpcSkins(CommandSourceStack source) {
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.npcskin.list.header"), false);
        for (NpcRole role : NpcRole.values()) {
            var profile = ConfigManager.npcSkins().profile(role.id());
            String type = profile.has("type") ? profile.get("type").getAsString() : "online";
            String value = profile.has("value") ? profile.get("value").getAsString() : "-";
            String localState = "local".equalsIgnoreCase(type)
                ? (ConfigManager.npcSkins().localSkinFileExists(value) ? "ok" : "missing")
                : "-";
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.npcskin.list.line",
                role.id(),
                TextUtil.tr(role.translationKey()),
                type,
                value,
                localState,
                ConfigManager.npcLabels().label(role.id())), false);
        }
        return 1;
    }

    private static int listNpcSkinFiles(CommandSourceStack source) {
        var files = ConfigManager.npcSkins().localSkinFiles();
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.npcskin.files.header"), false);
        if (files.isEmpty()) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.npcskin.files.empty"), false);
            return 1;
        }
        for (String file : files) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.npcskin.files.line", file), false);
        }
        return 1;
    }

    private static int setAllNpcSkinsOnline(CommandSourceStack source, String nickname) {
        for (NpcRole role : NpcRole.values()) {
            ConfigManager.npcSkins().setProfile(role.id(), "online", nickname);
        }
        AdvancedJobsMod.get().jobManager().syncCatalogToAllPlayers();
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.npcskin.setall.online", nickname), true);
        return 1;
    }

    private static int setAllNpcSkinsLocal(CommandSourceStack source, String file) {
        if (!ConfigManager.npcSkins().localSkinFileExists(file)) {
            source.sendFailure(TextUtil.tr("command.advancedjobs.admin.npcskin.local_missing", file));
            return 0;
        }
        for (NpcRole role : NpcRole.values()) {
            ConfigManager.npcSkins().setProfile(role.id(), "local", file);
        }
        AdvancedJobsMod.get().jobManager().syncCatalogToAllPlayers();
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.npcskin.setall.local", file), true);
        return 1;
    }

    private static int resetAllNpcSkins(CommandSourceStack source) {
        for (NpcRole role : NpcRole.values()) {
            ConfigManager.npcSkins().resetProfile(role.id());
        }
        AdvancedJobsMod.get().jobManager().syncCatalogToAllPlayers();
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.npcskin.setall.reset"), true);
        return 1;
    }

    private static int copyNpcSkin(CommandSourceStack source, String fromRoleId, String toRoleId) {
        NpcRole fromRole = parseRole(fromRoleId);
        NpcRole toRole = parseRole(toRoleId);
        if (fromRole == null) {
            source.sendFailure(TextUtil.tr("command.advancedjobs.admin.npcskin.invalid_role", fromRoleId));
            return 0;
        }
        if (toRole == null) {
            source.sendFailure(TextUtil.tr("command.advancedjobs.admin.npcskin.invalid_role", toRoleId));
            return 0;
        }
        var profile = ConfigManager.npcSkins().profile(fromRole.id());
        String type = profile.has("type") ? profile.get("type").getAsString() : "online";
        String value = profile.has("value") ? profile.get("value").getAsString() : "ZICteam";
        if ("local".equalsIgnoreCase(type) && !ConfigManager.npcSkins().localSkinFileExists(value)) {
            source.sendFailure(TextUtil.tr("command.advancedjobs.admin.npcskin.local_missing", value));
            return 0;
        }
        ConfigManager.npcSkins().setProfile(toRole.id(), type, value);
        AdvancedJobsMod.get().jobManager().syncCatalogToAllPlayers();
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.npcskin.copy",
            TextUtil.tr(fromRole.translationKey()),
            TextUtil.tr(toRole.translationKey()),
            type,
            value), true);
        return 1;
    }

    private static int listNpcLabels(CommandSourceStack source) {
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.npclabel.list.header"), false);
        for (NpcRole role : NpcRole.values()) {
            source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.npclabel.list.line",
                role.id(),
                TextUtil.tr(role.translationKey()),
                ConfigManager.npcLabels().label(role.id())), false);
        }
        return 1;
    }

    private static int setNpcLabel(CommandSourceStack source, String roleId, String label) {
        NpcRole role = parseRole(roleId);
        if (role == null) {
            source.sendFailure(TextUtil.tr("command.advancedjobs.admin.npclabel.invalid_role", roleId));
            return 0;
        }
        String trimmed = label.trim();
        if (trimmed.isEmpty()) {
            source.sendFailure(TextUtil.tr("command.advancedjobs.admin.npclabel.empty"));
            return 0;
        }
        ConfigManager.npcLabels().setLabel(role.id(), trimmed);
        refreshNpcLabels(source.getServer());
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.npclabel.updated",
            TextUtil.tr(role.translationKey()), trimmed), true);
        return 1;
    }

    private static int resetNpcLabel(CommandSourceStack source, String roleId) {
        NpcRole role = parseRole(roleId);
        if (role == null) {
            source.sendFailure(TextUtil.tr("command.advancedjobs.admin.npclabel.invalid_role", roleId));
            return 0;
        }
        ConfigManager.npcLabels().resetLabel(role.id());
        refreshNpcLabels(source.getServer());
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.npclabel.reset",
            TextUtil.tr(role.translationKey()),
            ConfigManager.npcLabels().label(role.id())), true);
        return 1;
    }

    private static int copyNpcLabel(CommandSourceStack source, String fromRoleId, String toRoleId) {
        NpcRole fromRole = parseRole(fromRoleId);
        NpcRole toRole = parseRole(toRoleId);
        if (fromRole == null) {
            source.sendFailure(TextUtil.tr("command.advancedjobs.admin.npclabel.invalid_role", fromRoleId));
            return 0;
        }
        if (toRole == null) {
            source.sendFailure(TextUtil.tr("command.advancedjobs.admin.npclabel.invalid_role", toRoleId));
            return 0;
        }
        String label = ConfigManager.npcLabels().label(fromRole.id());
        ConfigManager.npcLabels().setLabel(toRole.id(), label);
        refreshNpcLabels(source.getServer());
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.npclabel.copy",
            TextUtil.tr(fromRole.translationKey()),
            TextUtil.tr(toRole.translationKey()),
            label), true);
        return 1;
    }

    private static int setAllNpcLabels(CommandSourceStack source, String label) {
        String trimmed = label.trim();
        if (trimmed.isEmpty()) {
            source.sendFailure(TextUtil.tr("command.advancedjobs.admin.npclabel.empty"));
            return 0;
        }
        for (NpcRole role : NpcRole.values()) {
            ConfigManager.npcLabels().setLabel(role.id(), trimmed);
        }
        refreshNpcLabels(source.getServer());
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.npclabel.setall", trimmed), true);
        return 1;
    }

    private static int resetAllNpcLabels(CommandSourceStack source) {
        for (NpcRole role : NpcRole.values()) {
            ConfigManager.npcLabels().resetLabel(role.id());
        }
        refreshNpcLabels(source.getServer());
        source.sendSuccess(() -> TextUtil.tr("command.advancedjobs.admin.npclabel.resetall"), true);
        return 1;
    }

    private static int spawnNpc(CommandSourceStack source, EntityType<? extends Mob> type, String successKey, String failedKey) {
        var level = source.getLevel();
        Mob entity = type.create(level);
        if (entity == null) {
            source.sendFailure(TextUtil.tr(failedKey));
            return 0;
        }
        entity.moveTo(source.getPosition().x, source.getPosition().y, source.getPosition().z, source.getRotation().y, 0.0F);
        level.addFreshEntity(entity);
        source.sendSuccess(() -> TextUtil.tr(successKey), true);
        return 1;
    }

    private static boolean spawnNpcAt(net.minecraft.server.level.ServerLevel level, EntityType<? extends Mob> type,
                                      double x, double y, double z, float yaw) {
        Mob entity = type.create(level);
        if (entity == null) {
            return false;
        }
        entity.moveTo(x, y, z, yaw, 0.0F);
        level.addFreshEntity(entity);
        return true;
    }

    private static boolean isServiceNpc(Mob mob) {
        return roleIdForServiceNpc(mob) != null;
    }

    private static Mob findClosestServiceRole(CommandSourceStack source, String roleId, int radius) {
        var level = source.getLevel();
        var center = source.getPosition();
        double radiusSq = radius * radius;
        Mob best = null;
        double bestDistance = Double.MAX_VALUE;
        for (var entity : level.getEntities(null, new net.minecraft.world.phys.AABB(
            center.x - radius, center.y - radius, center.z - radius,
            center.x + radius, center.y + radius, center.z + radius))) {
            if (!(entity instanceof Mob mob)) {
                continue;
            }
            if (mob.distanceToSqr(center.x, center.y, center.z) > radiusSq) {
                continue;
            }
            if (!roleId.equalsIgnoreCase(roleIdForServiceNpc(mob))) {
                continue;
            }
            double distance = mob.distanceToSqr(center.x, center.y, center.z);
            if (distance < bestDistance) {
                best = mob;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static java.util.Map<String, Integer> collectServiceRoleCounts(CommandSourceStack source, int radius) {
        var level = source.getLevel();
        var center = source.getPosition();
        double radiusSq = radius * radius;
        java.util.Map<String, Integer> counts = new java.util.LinkedHashMap<>();
        for (NpcRole role : NpcRole.values()) {
            counts.put(role.id(), 0);
        }
        for (var entity : level.getEntities(null, new net.minecraft.world.phys.AABB(
            center.x - radius, center.y - radius, center.z - radius,
            center.x + radius, center.y + radius, center.z + radius))) {
            if (!(entity instanceof Mob mob)) {
                continue;
            }
            if (mob.distanceToSqr(center.x, center.y, center.z) > radiusSq) {
                continue;
            }
            String roleId = roleIdForServiceNpc(mob);
            if (roleId == null) {
                continue;
            }
            counts.merge(roleId, 1, Integer::sum);
        }
        return counts;
    }

    private static void refreshNpcLabels(net.minecraft.server.MinecraftServer server) {
        for (var level : server.getAllLevels()) {
            for (var entity : level.getAllEntities()) {
                if (entity instanceof RoleBasedNpc roleNpc && entity instanceof Mob mob) {
                    mob.setCustomName(roleNpc.roleLabel());
                    mob.setCustomNameVisible(true);
                    continue;
                }
                if (!(entity instanceof Mob mob)) {
                    continue;
                }
                String roleId = roleIdForFallbackNpc(mob);
                if (roleId == null) {
                    continue;
                }
                mob.setCustomName(net.minecraft.network.chat.Component.literal(ConfigManager.npcLabels().label(roleId)));
                mob.setCustomNameVisible(true);
            }
        }
    }

    private static String roleIdForFallbackNpc(Mob mob) {
        if (mob.getPersistentData().getBoolean(com.example.advancedjobs.event.JobEventHandler.JOB_NPC_TAG)) {
            return NpcRole.JOBS_MASTER.id();
        }
        if (mob.getPersistentData().getBoolean(com.example.advancedjobs.event.JobEventHandler.DAILY_BOARD_TAG)) {
            return NpcRole.DAILY_BOARD.id();
        }
        if (mob.getPersistentData().getBoolean(com.example.advancedjobs.event.JobEventHandler.STATUS_BOARD_TAG)) {
            return NpcRole.STATUS_BOARD.id();
        }
        if (mob.getPersistentData().getBoolean(com.example.advancedjobs.event.JobEventHandler.CONTRACTS_BOARD_TAG)) {
            return NpcRole.CONTRACTS_BOARD.id();
        }
        if (mob.getPersistentData().getBoolean(com.example.advancedjobs.event.JobEventHandler.SALARY_BOARD_TAG)) {
            return NpcRole.SALARY_BOARD.id();
        }
        if (mob.getPersistentData().getBoolean(com.example.advancedjobs.event.JobEventHandler.SKILLS_BOARD_TAG)) {
            return NpcRole.SKILLS_BOARD.id();
        }
        if (mob.getPersistentData().getBoolean(com.example.advancedjobs.event.JobEventHandler.LEADERBOARD_BOARD_TAG)) {
            return NpcRole.LEADERBOARD_BOARD.id();
        }
        if (mob.getPersistentData().getBoolean(com.example.advancedjobs.event.JobEventHandler.HELP_BOARD_TAG)) {
            return NpcRole.HELP_BOARD.id();
        }
        return null;
    }

    private static String roleIdForServiceNpc(Mob mob) {
        if (mob.getType() == ModEntities.JOBS_MASTER.get()) {
            return NpcRole.JOBS_MASTER.id();
        }
        if (mob.getType() == ModEntities.DAILY_BOARD.get()) {
            return NpcRole.DAILY_BOARD.id();
        }
        if (mob.getType() == ModEntities.STATUS_BOARD.get()) {
            return NpcRole.STATUS_BOARD.id();
        }
        if (mob.getType() == ModEntities.CONTRACTS_BOARD.get()) {
            return NpcRole.CONTRACTS_BOARD.id();
        }
        if (mob.getType() == ModEntities.SALARY_BOARD.get()) {
            return NpcRole.SALARY_BOARD.id();
        }
        if (mob.getType() == ModEntities.SKILLS_BOARD.get()) {
            return NpcRole.SKILLS_BOARD.id();
        }
        if (mob.getType() == ModEntities.LEADERBOARD_BOARD.get()) {
            return NpcRole.LEADERBOARD_BOARD.id();
        }
        if (mob.getType() == ModEntities.HELP_BOARD.get()) {
            return NpcRole.HELP_BOARD.id();
        }
        return roleIdForFallbackNpc(mob);
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

    private static EntityType<? extends Mob> nativeEntityType(NpcRole role) {
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

    private static java.util.List<HubNpcPlacement> hubPlacements(double baseX, double baseY, double baseZ, float yaw) {
        return java.util.List.of(
            new HubNpcPlacement(NpcRole.JOBS_MASTER, ModEntities.JOBS_MASTER.get(), baseX, baseY, baseZ, yaw),
            new HubNpcPlacement(NpcRole.DAILY_BOARD, ModEntities.DAILY_BOARD.get(), baseX + 2.5D, baseY, baseZ, yaw),
            new HubNpcPlacement(NpcRole.STATUS_BOARD, ModEntities.STATUS_BOARD.get(), baseX - 2.5D, baseY, baseZ, yaw),
            new HubNpcPlacement(NpcRole.CONTRACTS_BOARD, ModEntities.CONTRACTS_BOARD.get(), baseX, baseY, baseZ + 2.5D, yaw),
            new HubNpcPlacement(NpcRole.SALARY_BOARD, ModEntities.SALARY_BOARD.get(), baseX, baseY, baseZ - 2.5D, yaw),
            new HubNpcPlacement(NpcRole.SKILLS_BOARD, ModEntities.SKILLS_BOARD.get(), baseX + 2.5D, baseY, baseZ + 2.5D, yaw),
            new HubNpcPlacement(NpcRole.LEADERBOARD_BOARD, ModEntities.LEADERBOARD_BOARD.get(), baseX - 2.5D, baseY, baseZ + 2.5D, yaw),
            new HubNpcPlacement(NpcRole.HELP_BOARD, ModEntities.HELP_BOARD.get(), baseX + 2.5D, baseY, baseZ - 2.5D, yaw)
        );
    }

    private record HubNpcPlacement(
        NpcRole role,
        EntityType<? extends Mob> type,
        double x,
        double y,
        double z,
        float yaw
    ) {
    }

    private record HubNpcEntry(
        String roleId,
        String source,
        int x,
        int y,
        int z,
        String uuid
    ) {
    }

    private static NpcRole parseRole(String roleId) {
        for (NpcRole role : NpcRole.values()) {
            if (role.id().equalsIgnoreCase(roleId)) {
                return role;
            }
        }
        return null;
    }
}
