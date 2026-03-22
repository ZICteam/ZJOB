package com.example.advancedjobs.job;

import com.example.advancedjobs.AdvancedJobsMod;
import com.example.advancedjobs.api.EconomyProvider;
import com.example.advancedjobs.api.JobActionContext;
import com.example.advancedjobs.config.ConfigManager;
import com.example.advancedjobs.data.JsonPlayerDataRepository;
import com.example.advancedjobs.data.PlayerDataRepository;
import com.example.advancedjobs.data.SqlitePlayerDataRepository;
import com.example.advancedjobs.economy.ExternalEconomyBridge;
import com.example.advancedjobs.economy.InternalEconomyProvider;
import com.example.advancedjobs.model.ActionRewardEntry;
import com.example.advancedjobs.model.ContractProgress;
import com.example.advancedjobs.model.DailyTaskProgress;
import com.example.advancedjobs.model.JobDefinition;
import com.example.advancedjobs.model.JobProgress;
import com.example.advancedjobs.model.PlayerJobProfile;
import com.example.advancedjobs.entity.NpcRole;
import com.example.advancedjobs.model.RewardDefinition;
import com.example.advancedjobs.network.OpenJobsScreenPacket;
import com.example.advancedjobs.network.PacketHandler;
import com.example.advancedjobs.network.SyncJobCatalogPacket;
import com.example.advancedjobs.network.SyncLeaderboardPacket;
import com.example.advancedjobs.network.SyncPlayerJobsPacket;
import com.example.advancedjobs.util.DebugLog;
import com.example.advancedjobs.util.ResourceLocationUtil;
import com.example.advancedjobs.util.RewardUtil;
import com.example.advancedjobs.util.TextUtil;
import com.example.advancedjobs.util.TimeUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

public class JobManager {
    private static final int AUTOSAVE_TICKS = 20 * 60;
    private static final int LEADERBOARD_SYNC_LIMIT = 25;

    private final JobRegistry registry = new JobRegistry();
    private final JobProgressService progressService = new JobProgressService();
    private final SkillTreeService skillTreeService = new SkillTreeService();
    private final PerkService perkService = new PerkService();
    private final Map<UUID, PlayerJobProfile> cache = new LinkedHashMap<>();
    private final InternalEconomyProvider internalEconomyProvider = new InternalEconomyProvider();
    private final ExternalEconomyBridge externalEconomyBridge = new ExternalEconomyBridge();
    private volatile String cachedCatalogPayload;
    private final Map<String, List<PlayerJobProfile>> cachedLeaderboards = new LinkedHashMap<>();
    private volatile List<PlayerJobProfile> cachedOverallLeaderboard;

    private PlayerDataRepository repository;
    private EconomyProvider economyProvider = internalEconomyProvider;
    private MinecraftServer server;
    private int autosaveCounter;

    public void onServerStarting(MinecraftServer server) {
        this.server = server;
        ConfigManager.reloadJsonConfigs();
        DebugLog.initFromConfig();
        registry.reload();
        repository = "sqlite".equalsIgnoreCase(ConfigManager.COMMON.storageMode.get()) ? new SqlitePlayerDataRepository() : new JsonPlayerDataRepository();
        repository.init(server);
        cache.clear();
        invalidateLeaderboardCaches();
        for (PlayerJobProfile profile : repository.all()) {
            cache.put(profile.playerId(), profile);
        }
        economyProvider = chooseEconomyProvider();
        if (economyProvider == internalEconomyProvider) {
            restoreInternalBalances();
        }
        logEconomyStartupState("server_start");
        invalidateCatalogCache();
        invalidateLeaderboardCaches();
        autosaveCounter = 0;
    }

    public void reload() {
        ConfigManager.reloadJsonConfigs();
        DebugLog.initFromConfig();
        registry.reload();
        economyProvider = chooseEconomyProvider();
        if (economyProvider == internalEconomyProvider) {
            restoreInternalBalances();
        }
        logEconomyStartupState("reload");
        invalidateCatalogCache();
        invalidateLeaderboardCaches();
        syncAllOnlinePlayers();
    }

    public PlayerJobProfile getOrCreateProfile(ServerPlayer player) {
        PlayerJobProfile existing = cache.get(player.getUUID());
        if (existing != null) {
            return existing;
        }
        PlayerJobProfile created = new PlayerJobProfile(player.getUUID(), player.getGameProfile().getName());
        cache.put(player.getUUID(), created);
        invalidateLeaderboardCaches();
        return created;
    }

    public Collection<JobDefinition> jobs() {
        return registry.all();
    }

    public int profileCacheSize() {
        return cache.size();
    }

    public boolean isCatalogPayloadCached() {
        return cachedCatalogPayload != null;
    }

    public int catalogPayloadSize() {
        return cachedCatalogPayload == null ? 0 : cachedCatalogPayload.length();
    }

    public int leaderboardCacheCount() {
        return cachedLeaderboards.size();
    }

    public boolean isOverallLeaderboardCached() {
        return cachedOverallLeaderboard != null;
    }

    public int rewardIndexJobCount() {
        return registry.rewardIndexJobCount();
    }

    public int rewardIndexEntryCount() {
        return registry.rewardIndexEntryCount();
    }

    public int jobCount() {
        return registry.jobCount();
    }

    public WarmCacheStats warmCaches() {
        int profileCount = cache.size();
        int jobCount = 0;
        createCatalogPayload();
        overallLeaderboard();
        for (JobDefinition definition : registry.all()) {
            leaderboard(definition.id());
            jobCount++;
        }
        int warmedLeaderboards = cachedLeaderboards.size();
        return new WarmCacheStats(
            profileCount,
            jobCount,
            cachedCatalogPayload != null ? cachedCatalogPayload.length() : 0,
            warmedLeaderboards,
            cachedOverallLeaderboard != null);
    }

    public void clearCaches() {
        invalidateCatalogCache();
        invalidateLeaderboardCaches();
    }

    public Optional<JobDefinition> job(String jobId) {
        return registry.get(jobId);
    }

    public boolean chooseJob(ServerPlayer player, String jobId) {
        return chooseJob(player, jobId, false);
    }

    public boolean chooseJob(ServerPlayer player, String jobId, boolean secondary) {
        Optional<JobDefinition> definition = registry.get(jobId);
        if (definition.isEmpty()) {
            return false;
        }
        PlayerJobProfile profile = getOrCreateProfile(player);
        ensureAssignments(profile, jobId);
        if (secondary && !ConfigManager.COMMON.allowSecondaryJob.get()) {
            return false;
        }
        String currentJobId = secondary ? profile.secondaryJobId() : profile.activeJobId();
        String otherJobId = secondary ? profile.activeJobId() : profile.secondaryJobId();
        if (jobId.equals(otherJobId)) {
            return false;
        }
        long now = TimeUtil.now();
        if (currentJobId != null && !currentJobId.equals(jobId)) {
            if (now - profile.lastJobChangeEpochSecond() < ConfigManager.COMMON.jobChangeCooldownSeconds.get()) {
                return false;
            }
            double price = ConfigManager.COMMON.jobChangePrice.get();
            if (price > 0.0D && !economyProvider.withdraw(player.getUUID(), price, "job_change")) {
                return false;
            }
            forgetProgress(profile, currentJobId);
        }
        profile.progress(jobId);
        if (secondary) {
            profile.setSecondaryJobId(jobId);
        } else {
            profile.setActiveJobId(jobId);
        }
        profile.setLastJobChangeEpochSecond(now);
        saveProfile(profile);
        syncToPlayer(player);
        DebugLog.log("Job selected: player=" + player.getGameProfile().getName() + " job=" + jobId + " slot=" + (secondary ? "secondary" : "primary"));
        return true;
    }

    public boolean leaveJob(ServerPlayer player, boolean secondary) {
        PlayerJobProfile profile = getOrCreateProfile(player);
        String currentJobId = secondary ? profile.secondaryJobId() : profile.activeJobId();
        if (currentJobId == null) {
            return false;
        }
        forgetProgress(profile, currentJobId);
        if (secondary) {
            profile.setSecondaryJobId(null);
        } else {
            profile.setActiveJobId(null);
        }
        profile.setLastJobChangeEpochSecond(TimeUtil.now());
        saveProfile(profile);
        syncToPlayer(player);
        DebugLog.log("Job left: player=" + player.getGameProfile().getName() + " job=" + currentJobId + " slot=" + (secondary ? "secondary" : "primary"));
        return true;
    }

    public void resetProfile(ServerPlayer player) {
        PlayerJobProfile profile = getOrCreateProfile(player);
        profile.jobs().clear();
        profile.setActiveJobId(null);
        profile.setSecondaryJobId(null);
        profile.setLastJobChangeEpochSecond(0L);
        saveProfile(profile);
        syncToPlayer(player);
    }

    public boolean adminSetJob(ServerPlayer player, String jobId, boolean secondary) {
        Optional<JobDefinition> definition = registry.get(jobId);
        if (definition.isEmpty()) {
            return false;
        }
        PlayerJobProfile profile = getOrCreateProfile(player);
        profile.progress(jobId);
        if (secondary) {
            if (!ConfigManager.COMMON.allowSecondaryJob.get()) {
                return false;
            }
            if (jobId.equals(profile.activeJobId())) {
                return false;
            }
            profile.setSecondaryJobId(jobId);
        } else {
            if (jobId.equals(profile.secondaryJobId())) {
                return false;
            }
            profile.setActiveJobId(jobId);
        }
        saveProfile(profile);
        syncToPlayer(player);
        return true;
    }

    public boolean adminSetLevel(ServerPlayer player, String jobId, int level) {
        if (registry.get(jobId).isEmpty()) {
            return false;
        }
        PlayerJobProfile profile = getOrCreateProfile(player);
        JobProgress progress = profile.progress(jobId);
        progress.setLevel(level);
        evaluateMilestones(player, profile, jobId, progress);
        saveProfile(profile);
        syncToPlayer(player);
        return true;
    }

    public boolean adminAddXp(ServerPlayer player, String jobId, double amount) {
        if (registry.get(jobId).isEmpty()) {
            return false;
        }
        PlayerJobProfile profile = getOrCreateProfile(player);
        JobProgress progress = profile.progress(jobId);
        progress.addXp(Math.max(0.0D, amount));
        evaluateMilestones(player, profile, jobId, progress);
        profile.markDirty();
        saveProfile(profile);
        syncToPlayer(player);
        return true;
    }

    public boolean adminAddSalary(ServerPlayer player, double amount) {
        PlayerJobProfile profile = getOrCreateProfile(player);
        if (profile.activeJobId() == null) {
            return false;
        }
        profile.progress(profile.activeJobId()).addPendingSalary(Math.max(0.0D, amount));
        profile.markDirty();
        saveProfile(profile);
        syncToPlayer(player);
        return true;
    }

    public boolean adminAdjustSkillPoints(ServerPlayer player, int amount) {
        PlayerJobProfile profile = getOrCreateProfile(player);
        if (profile.activeJobId() == null) {
            return false;
        }
        JobProgress progress = profile.progress(profile.activeJobId());
        if (amount >= 0) {
            progress.refundSkillPoints(amount);
        } else {
            progress.spendSkillPoints(-amount);
        }
        profile.markDirty();
        saveProfile(profile);
        syncToPlayer(player);
        return true;
    }

    public RewardDefinition handleAction(JobActionContext context) {
        PlayerJobProfile profile = getOrCreateProfile(context.player());
        profile.setPlayerName(context.player().getGameProfile().getName());
        List<String> assignedJobs = assignedJobIds(profile);
        if (assignedJobs.isEmpty()) {
            return RewardDefinition.EMPTY;
        }
        double totalSalary = 0.0D;
        double totalXp = 0.0D;
        boolean rewarded = false;
        for (String jobId : assignedJobs) {
            JobDefinition definition = registry.get(jobId).orElse(null);
            if (definition == null) {
                continue;
            }
            RewardDefinition baseReward = findReward(definition, context);
            if (baseReward == RewardDefinition.EMPTY) {
                continue;
            }
            rewarded = true;
            JobProgress progress = profile.progress(definition.id());
            ensureAssignments(profile, definition.id());
            double salaryBonus = perkService.aggregateEffect(definition.skillBranches(), progress, "salary_bonus");
            RewardDefinition finalReward = RewardUtil.applyMultipliers(
                baseReward.scaled(1.0D + salaryBonus),
                context,
                1.0D,
                ConfigManager.economy().worldMultipliers(),
                ConfigManager.economy().biomeMultipliers(),
                ConfigManager.economy().vipMultiplier(),
                ConfigManager.economy().eventMultiplier()
            );

            progressService.addXp(profile, definition, finalReward.xp());
            creditSalary(context.player(), profile, definition.id(), finalReward.salary(), "job_action");
            progress.actionStats().merge(context.actionType().name() + "|" + context.targetId(), 1, Integer::sum);
            updateDailyTasks(context.player(), profile, definition.id(), context, finalReward);
            updateContracts(context.player(), profile, definition.id(), context, finalReward);
            evaluateMilestones(context.player(), profile, definition.id(), progress);
            totalSalary += finalReward.salary();
            totalXp += finalReward.xp();
            DebugLog.log("Reward: player=" + context.player().getGameProfile().getName()
                + " job=" + definition.id()
                + " action=" + context.actionType()
                + " target=" + context.targetId()
                + " salary=" + finalReward.salary()
                + " xp=" + finalReward.xp());
        }
        if (!rewarded) {
            return RewardDefinition.EMPTY;
        }
        saveProfile(profile);
        syncToPlayer(context.player());
        return new RewardDefinition(totalSalary, totalXp, 0.0D, 0.0D);
    }

    public double claimSalary(ServerPlayer player) {
        PlayerJobProfile profile = getOrCreateProfile(player);
        long remainingCooldown = salaryClaimCooldownRemaining(profile);
        if (remainingCooldown > 0L) {
            return -remainingCooldown;
        }
        List<String> assignedJobs = assignedJobIds(profile);
        if (assignedJobs.isEmpty()) {
            return 0.0D;
        }
        Map<String, Double> claimedPerJob = new LinkedHashMap<>();
        double remainingCap = ConfigManager.COMMON.maxSalaryPerClaim.get();
        double gross = 0.0D;
        for (String jobId : assignedJobs) {
            if (remainingCap <= 0.0D) {
                break;
            }
            JobProgress progress = profile.progress(jobId);
            double claimed = progress.takePendingSalary(remainingCap);
            if (claimed <= 0.0D) {
                continue;
            }
            claimedPerJob.put(jobId, claimed);
            gross += claimed;
            remainingCap -= claimed;
        }
        if (gross <= 0.0D) {
            return 0.0D;
        }
        double taxAmount = gross * ConfigManager.COMMON.salaryTaxRate.get();
        double payout = Math.max(0.0D, gross - taxAmount);
        AdvancedJobsMod.LOGGER.info(
            "AdvancedJobs salary claim requested: player={} provider={} currency={} gross={} tax={} net={} jobs={}",
            player.getGameProfile().getName(),
            economyProvider.id(),
            ConfigManager.economy().externalCurrencyId(),
            gross,
            taxAmount,
            payout,
            assignedJobs
        );
        if (!economyProvider.deposit(player.getUUID(), payout, "job_salary_claim")) {
            AdvancedJobsMod.LOGGER.warn(
                "AdvancedJobs salary claim deposit failed: player={} provider={} currency={} net={}",
                player.getGameProfile().getName(),
                economyProvider.id(),
                ConfigManager.economy().externalCurrencyId(),
                payout
            );
            claimedPerJob.forEach((jobId, amount) -> profile.progress(jobId).restorePendingSalary(amount));
            return 0.0D;
        }
        AdvancedJobsMod.LOGGER.info(
            "AdvancedJobs salary claim deposit ok: player={} provider={} currency={} net={}",
            player.getGameProfile().getName(),
            economyProvider.id(),
            ConfigManager.economy().externalCurrencyId(),
            payout
        );
        routeTaxToServerAccount(taxAmount, "job_salary_claim_tax");
        profile.setLastSalaryClaimEpochSecond(TimeUtil.now());
        profile.markDirty();
        saveProfile(profile);
        syncToPlayer(player);
        DebugLog.log("Salary claim: player=" + player.getGameProfile().getName() + " jobs=" + assignedJobs + " paid=" + payout);
        return payout;
    }

    public long salaryClaimCooldownRemaining(PlayerJobProfile profile) {
        return Math.max(0L, ConfigManager.COMMON.salaryClaimIntervalSeconds.get() - (TimeUtil.now() - profile.lastSalaryClaimEpochSecond()));
    }

    public long contractRerollCooldownRemaining(PlayerJobProfile profile) {
        return Math.max(0L, ConfigManager.COMMON.contractRerollCooldownSeconds.get() - (TimeUtil.now() - profile.lastContractRerollEpochSecond()));
    }

    public ContractRerollResult previewContractReroll(ServerPlayer player, String jobId) {
        if (jobId == null || jobId.isBlank()) {
            return ContractRerollResult.NO_JOB;
        }
        PlayerJobProfile profile = getOrCreateProfile(player);
        if (!assignedJobIds(profile).contains(jobId)) {
            return ContractRerollResult.NOT_ASSIGNED;
        }
        if (contractRerollCooldownRemaining(profile) > 0L) {
            return ContractRerollResult.COOLDOWN;
        }
        double price = ConfigManager.COMMON.contractRerollPrice.get();
        if (price > 0.0D && economyProvider.getBalance(player.getUUID()) < price) {
            return ContractRerollResult.INSUFFICIENT_FUNDS;
        }
        return ContractRerollResult.SUCCESS;
    }

    private void creditSalary(ServerPlayer player, PlayerJobProfile profile, String jobId, double grossSalary, String reason) {
        double safeGross = Math.max(0.0D, grossSalary);
        if (safeGross <= 0.0D) {
            return;
        }
        if (!ConfigManager.COMMON.instantSalary.get()) {
            progressService.addSalary(profile, jobId, safeGross);
            return;
        }
        double taxAmount = safeGross * ConfigManager.COMMON.salaryTaxRate.get();
        double netPayout = Math.max(0.0D, safeGross - taxAmount);
        AdvancedJobsMod.LOGGER.info(
            "AdvancedJobs instant salary requested: player={} job={} provider={} currency={} gross={} tax={} net={} reason={}",
            player.getGameProfile().getName(),
            jobId,
            economyProvider.id(),
            ConfigManager.economy().externalCurrencyId(),
            safeGross,
            taxAmount,
            netPayout,
            reason
        );
        if (economyProvider.deposit(profile.playerId(), netPayout, reason)) {
            AdvancedJobsMod.LOGGER.info(
                "AdvancedJobs instant salary deposit ok: player={} job={} provider={} currency={} net={} reason={}",
                player.getGameProfile().getName(),
                jobId,
                economyProvider.id(),
                ConfigManager.economy().externalCurrencyId(),
                netPayout,
                reason
            );
            routeTaxToServerAccount(taxAmount, reason + "_tax");
            progressService.addEarnedSalary(profile, jobId, safeGross);
            DebugLog.log("Instant salary: player=" + player.getGameProfile().getName()
                + " job=" + jobId
                + " gross=" + safeGross
                + " net=" + netPayout
                + " reason=" + reason);
            return;
        }
        AdvancedJobsMod.LOGGER.warn(
            "AdvancedJobs instant salary deposit failed: player={} job={} provider={} currency={} net={} reason={}",
            player.getGameProfile().getName(),
            jobId,
            economyProvider.id(),
            ConfigManager.economy().externalCurrencyId(),
            netPayout,
            reason
        );
        progressService.addSalary(profile, jobId, safeGross);
        DebugLog.log("Instant salary fallback to pending: player=" + player.getGameProfile().getName()
            + " job=" + jobId
            + " gross=" + safeGross
            + " reason=" + reason);
    }

    private void routeTaxToServerAccount(double taxAmount, String reason) {
        if (taxAmount <= 0.0D) {
            return;
        }
        UUID sinkId = ConfigManager.economy().taxSinkAccountId();
        if (sinkId == null) {
            AdvancedJobsMod.LOGGER.warn("Skipping tax routing due to invalid taxSinkAccountUuid");
            return;
        }
        if (!economyProvider.deposit(sinkId, taxAmount, reason)) {
            AdvancedJobsMod.LOGGER.warn("Failed to route tax to server account: account={} amount={} reason={}",
                sinkId, taxAmount, reason);
            return;
        }
        AdvancedJobsMod.LOGGER.info("AdvancedJobs tax routed: account={} provider={} currency={} amount={} reason={}",
            sinkId, economyProvider.id(), ConfigManager.economy().externalCurrencyId(), taxAmount, reason);
    }

    public boolean unlockSkill(ServerPlayer player, String jobId, String nodeId) {
        Optional<JobDefinition> definition = registry.get(jobId);
        if (definition.isEmpty()) {
            return false;
        }
        PlayerJobProfile profile = getOrCreateProfile(player);
        boolean unlocked = skillTreeService.unlock(profile, definition.get(), nodeId);
        if (unlocked) {
            saveProfile(profile);
            syncToPlayer(player);
            DebugLog.log("Skill unlocked: player=" + player.getGameProfile().getName() + " job=" + jobId + " node=" + nodeId);
        }
        return unlocked;
    }

    public Collection<PlayerJobProfile> leaderboard(String jobId) {
        return cachedLeaderboards.computeIfAbsent(jobId, key -> cache.values().stream()
            .filter(profile -> profile.jobs().containsKey(key))
            .sorted(Comparator.<PlayerJobProfile>comparingInt(profile -> profile.progress(key).level()).reversed()
                .thenComparingDouble(profile -> profile.progress(key).earnedTotal()).reversed())
            .toList());
    }

    public Collection<PlayerJobProfile> overallLeaderboard() {
        List<PlayerJobProfile> leaderboard = cachedOverallLeaderboard;
        if (leaderboard != null) {
            return leaderboard;
        }
        leaderboard = cache.values().stream()
            .filter(profile -> !profile.jobs().isEmpty())
            .sorted(Comparator.<PlayerJobProfile>comparingDouble(this::totalEarnedAcrossJobs).reversed()
                .thenComparingInt(this::totalLevelsAcrossJobs).reversed())
            .toList();
        cachedOverallLeaderboard = leaderboard;
        return leaderboard;
    }

    public EconomyProvider economy() {
        return economyProvider;
    }

    public double totalEarnedAcrossJobs(PlayerJobProfile profile) {
        return profile.jobs().values().stream().mapToDouble(JobProgress::earnedTotal).sum();
    }

    public int totalLevelsAcrossJobs(PlayerJobProfile profile) {
        return profile.jobs().values().stream().mapToInt(JobProgress::level).sum();
    }

    public double effectBonus(ServerPlayer player, String effectType) {
        PlayerJobProfile profile = getOrCreateProfile(player);
        List<String> assignedJobs = assignedJobIds(profile);
        if (assignedJobs.isEmpty()) {
            return 0.0D;
        }
        double bonus = 0.0D;
        for (String jobId : assignedJobs) {
            JobDefinition definition = registry.get(jobId).orElse(null);
            if (definition == null) {
                continue;
            }
            bonus = Math.max(bonus, resolveEffectBonus(profile, definition, effectType));
        }
        return bonus;
    }

    public int effectNodes(ServerPlayer player, String effectType) {
        PlayerJobProfile profile = getOrCreateProfile(player);
        List<String> assignedJobs = assignedJobIds(profile);
        if (assignedJobs.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (String jobId : assignedJobs) {
            JobDefinition definition = registry.get(jobId).orElse(null);
            if (definition == null) {
                continue;
            }
            count = Math.max(count, perkService.unlockedCount(definition.skillBranches(), profile.progress(definition.id()), effectType));
        }
        return count;
    }

    public String activeJobId(ServerPlayer player) {
        return getOrCreateProfile(player).activeJobId();
    }

    public boolean hasAssignedJob(ServerPlayer player, String jobId) {
        PlayerJobProfile profile = getOrCreateProfile(player);
        return jobId != null && (jobId.equals(profile.activeJobId()) || jobId.equals(profile.secondaryJobId()));
    }

    public String firstAssignedJob(ServerPlayer player, String... jobIds) {
        PlayerJobProfile profile = getOrCreateProfile(player);
        for (String assigned : assignedJobIds(profile)) {
            for (String candidate : jobIds) {
                if (assigned.equals(candidate)) {
                    return assigned;
                }
            }
        }
        return null;
    }

    public void flush() {
        if (repository == null) {
            return;
        }
        persistInternalBalances();
        if (cache.values().stream().anyMatch(PlayerJobProfile::dirty)) {
            repository.saveAll(cache.values());
            cache.values().forEach(PlayerJobProfile::clearDirty);
            DebugLog.log("Flush completed for " + cache.size() + " profiles");
        }
    }

    public void tick() {
        expireRewardEventIfNeeded();
        autosaveCounter++;
        if (autosaveCounter >= AUTOSAVE_TICKS) {
            autosaveCounter = 0;
            flush();
        }
    }

    public void syncToPlayer(ServerPlayer player) {
        PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncPlayerJobsPacket(createPlayerPayload(player, getOrCreateProfile(player))));
    }

    public void syncCatalogToPlayer(ServerPlayer player) {
        PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncJobCatalogPacket(createCatalogPayload()));
    }

    public void syncLeaderboardToPlayer(ServerPlayer player, String jobId) {
        PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncLeaderboardPacket(jobId, createLeaderboardPayload(jobId)));
    }

    public void syncAllOnlinePlayers() {
        invalidateCatalogCache();
        if (server == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncCatalogToPlayer(player);
            syncToPlayer(player);
            PlayerJobProfile profile = getOrCreateProfile(player);
            syncLeaderboardToPlayer(player, profile.activeJobId() != null ? profile.activeJobId() : "all");
        }
    }

    public void openScreen(ServerPlayer player) {
        openScreen(player, "jobs");
    }

    public void openScreen(ServerPlayer player, String tab) {
        openScreen(player, tab, null);
    }

    public void openScreen(ServerPlayer player, String tab, String preferredJobId) {
        syncCatalogToPlayer(player);
        syncToPlayer(player);
        syncLeaderboardToPlayer(player, getOrCreateProfile(player).activeJobId() != null ? getOrCreateProfile(player).activeJobId() : "all");
        PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenJobsScreenPacket(tab, preferredJobId));
    }

    public boolean rerollContracts(ServerPlayer player, String jobId) {
        ContractRerollResult preview = previewContractReroll(player, jobId);
        if (preview != ContractRerollResult.SUCCESS) {
            return false;
        }
        PlayerJobProfile profile = getOrCreateProfile(player);
        double price = ConfigManager.COMMON.contractRerollPrice.get();
        if (price > 0.0D && !economyProvider.withdraw(player.getUUID(), price, "contract_reroll")) {
            return false;
        }
        JobProgress progress = profile.progress(jobId);
        long now = TimeUtil.now();
        progress.contracts().clear();
        for (ConfigManager.ContractTemplate template : pickContracts(jobId)) {
            progress.contracts().add(new ContractProgress(template.id(), jobId, template.rarity(), template.goal(), 0, now + template.durationSeconds(), false));
        }
        profile.setLastContractRerollEpochSecond(now);
        profile.markDirty();
        saveProfile(profile);
        syncToPlayer(player);
        return true;
    }

    public enum ContractRerollResult {
        SUCCESS,
        NO_JOB,
        NOT_ASSIGNED,
        COOLDOWN,
        INSUFFICIENT_FUNDS
    }

    private void saveProfile(PlayerJobProfile profile) {
        persistInternalBalances();
        invalidateLeaderboardCaches();
        if (repository != null) {
            repository.save(profile);
            profile.clearDirty();
        }
    }

    private EconomyProvider chooseEconomyProvider() {
        if ("external".equalsIgnoreCase(ConfigManager.economy().providerId()) && externalEconomyBridge.isAvailable()) {
            AdvancedJobsMod.LOGGER.info("Using external economy bridge");
            return externalEconomyBridge;
        }
        AdvancedJobsMod.LOGGER.info("Using internal economy provider");
        return internalEconomyProvider;
    }

    private void logEconomyStartupState(String phase) {
        AdvancedJobsMod.LOGGER.info(
            "AdvancedJobs economy status [{}]: configuredProvider={} activeProvider={} configuredCurrency={} activeCurrency={} bridgeAvailable={}",
            phase,
            ConfigManager.economy().providerId(),
            economyProvider.id(),
            ConfigManager.economy().externalCurrencyId(),
            economyProvider == externalEconomyBridge ? externalEconomyBridge.currencyDebugId() : "internal",
            externalEconomyBridge.isAvailable()
        );
    }

    private void restoreInternalBalances() {
        Map<UUID, Double> balances = new LinkedHashMap<>();
        for (PlayerJobProfile profile : cache.values()) {
            balances.put(profile.playerId(), profile.internalBalance());
        }
        internalEconomyProvider.load(balances);
    }

    private void persistInternalBalances() {
        if (economyProvider != internalEconomyProvider) {
            return;
        }
        Map<UUID, Double> snapshot = internalEconomyProvider.snapshot();
        for (Map.Entry<UUID, Double> entry : snapshot.entrySet()) {
            PlayerJobProfile profile = cache.get(entry.getKey());
            if (profile != null) {
                if (Double.compare(profile.internalBalance(), entry.getValue()) != 0) {
                    profile.setInternalBalance(entry.getValue());
                }
            }
        }
    }

    private double resolveEffectBonus(PlayerJobProfile profile, JobDefinition definition, String effectType) {
        double direct = perkService.aggregateEffect(definition.skillBranches(), profile.progress(definition.id()), effectType);
        if (direct > 0.0D) {
            return direct;
        }
        return switch (effectType) {
            case "ore_bonus", "wood_bonus", "crop_bonus", "breed_bonus", "fish_bonus", "combat_bonus",
                "build_bonus", "craft_bonus", "trade_bonus", "magic_bonus", "explore_bonus",
                "rare_gem_bonus", "sapling_return", "seed_keep", "treasure_chance", "loot_bonus",
                "block_refund", "discount_bonus", "ingredient_save", "artifact_bonus", "honey_bonus", "junk_reduction",
                "emerald_bonus", "rare_magic_bonus", "cache_finder", "elite_tracker", "decor_bonus" ->
                perkService.aggregateEffect(definition.skillBranches(), profile.progress(definition.id()), "resource_bonus");
            case "mine_aura", "forest_aura", "farm_aura", "pasture_aura", "water_aura", "combat_aura",
                "builder_aura", "craft_aura", "merchant_aura", "magic_aura", "explore_aura", "fall_guard", "ore_vision" ->
                perkService.aggregateEffect(definition.skillBranches(), profile.progress(definition.id()), "utility_bonus");
            case "auto_smelt" ->
                perkService.aggregateEffect(definition.skillBranches(), profile.progress(definition.id()), "resource_bonus");
            default -> 0.0D;
        };
    }

    private RewardDefinition findReward(JobDefinition definition, JobActionContext context) {
        return registry.rewardFor(definition.id(), context.actionType(), context.targetId());
    }

    private String createCatalogPayload() {
        String payload = cachedCatalogPayload;
        if (payload != null) {
            return payload;
        }
        JsonObject root = new JsonObject();
        JsonArray jobs = new JsonArray();
        for (JobDefinition definition : registry.all()) {
            JsonObject job = new JsonObject();
            job.addProperty("id", definition.id());
            job.addProperty("nameKey", definition.translationKey());
            job.addProperty("descriptionKey", definition.descriptionKey());
            JsonArray passives = new JsonArray();
            definition.passivePerks().forEach((level, keys) -> {
                JsonObject passive = new JsonObject();
                passive.addProperty("level", level);
                JsonArray items = new JsonArray();
                keys.forEach(items::add);
                passive.add("keys", items);
                passives.add(passive);
            });
            job.add("passives", passives);
            JsonArray rewardPreview = new JsonArray();
            int rewardCount = 0;
            for (ActionRewardEntry entry : definition.actionRewards()) {
                rewardCount++;
                if (rewardPreview.size() >= 4) {
                    continue;
                }
                JsonObject reward = new JsonObject();
                reward.addProperty("type", entry.actionType().name());
                reward.addProperty("targetId", entry.targetId().toString());
                reward.addProperty("salary", entry.rewardDefinition().salary());
                reward.addProperty("xp", entry.rewardDefinition().xp());
                rewardPreview.add(reward);
            }
            job.addProperty("rewardCount", rewardCount);
            job.add("rewardPreview", rewardPreview);
            JsonArray branches = new JsonArray();
            for (var branch : definition.skillBranches()) {
                JsonObject branchObj = new JsonObject();
                branchObj.addProperty("id", branch.id());
                branchObj.addProperty("translationKey", branch.translationKey());
                JsonArray nodes = new JsonArray();
                for (var node : branch.nodes()) {
                    JsonObject nodeObj = new JsonObject();
                    nodeObj.addProperty("id", node.id());
                    nodeObj.addProperty("translationKey", node.translationKey());
                    nodeObj.addProperty("requiredLevel", node.requiredLevel());
                    nodeObj.addProperty("cost", node.cost());
                    if (node.parentId() != null) {
                        nodeObj.addProperty("parentId", node.parentId());
                    }
                    nodeObj.addProperty("effectType", node.effectType());
                    nodeObj.addProperty("effectValue", node.effectValue());
                    nodeObj.addProperty("unlocked", false);
                    nodes.add(nodeObj);
                }
                branchObj.add("nodes", nodes);
                branches.add(branchObj);
            }
            job.add("skillBranches", branches);
            jobs.add(job);
        }
        root.add("jobs", jobs);
        JsonObject npcSkins = new JsonObject();
        for (NpcRole role : NpcRole.values()) {
            JsonObject profile = ConfigManager.npcSkins().profile(role.id()).deepCopy();
            String imageBase64 = ConfigManager.npcSkins().localSkinBase64(role.id());
            if (imageBase64 != null) {
                profile.addProperty("imageBase64", imageBase64);
            }
            npcSkins.add(role.id(), profile);
        }
        root.add("npcSkins", npcSkins);
        payload = ConfigManager.GSON.toJson(root);
        cachedCatalogPayload = payload;
        return payload;
    }

    private void invalidateCatalogCache() {
        cachedCatalogPayload = null;
    }

    private void invalidateLeaderboardCaches() {
        cachedLeaderboards.clear();
        cachedOverallLeaderboard = null;
    }

    public record WarmCacheStats(
        int profileCount,
        int jobCount,
        int catalogPayloadChars,
        int warmedLeaderboards,
        boolean overallLeaderboardCached) {
    }

    private String createPlayerPayload(ServerPlayer player, PlayerJobProfile profile) {
        JsonObject root = new JsonObject();
        List<String> assignedJobs = assignedJobIds(profile);
        root.addProperty("playerName", profile.playerName());
        if (profile.activeJobId() != null) {
            root.addProperty("activeJobId", profile.activeJobId());
        }
        if (profile.secondaryJobId() != null) {
            root.addProperty("secondaryJobId", profile.secondaryJobId());
        }
        root.addProperty("balance", economyProvider.getBalance(profile.playerId()));
        root.addProperty("allowSecondaryJob", ConfigManager.COMMON.allowSecondaryJob.get());
        root.addProperty("instantSalary", ConfigManager.COMMON.instantSalary.get());
        root.addProperty("jobChangePrice", ConfigManager.COMMON.jobChangePrice.get());
        root.addProperty("jobChangeCooldownSeconds", ConfigManager.COMMON.jobChangeCooldownSeconds.get());
        root.addProperty("jobChangeCooldownRemaining", Math.max(0L, ConfigManager.COMMON.jobChangeCooldownSeconds.get() - (TimeUtil.now() - profile.lastJobChangeEpochSecond())));
        root.addProperty("salaryClaimIntervalSeconds", ConfigManager.COMMON.salaryClaimIntervalSeconds.get());
        root.addProperty("salaryClaimCooldownRemaining", salaryClaimCooldownRemaining(profile));
        root.addProperty("maxSalaryPerClaim", ConfigManager.COMMON.maxSalaryPerClaim.get());
        root.addProperty("salaryTaxRate", ConfigManager.COMMON.salaryTaxRate.get());
        root.addProperty("contractRerollPrice", ConfigManager.COMMON.contractRerollPrice.get());
        root.addProperty("contractRerollCooldownRemaining", contractRerollCooldownRemaining(profile));
        root.addProperty("economyProvider", economyProvider == internalEconomyProvider ? "internal" : "external");
        root.addProperty("economyCurrency", ConfigManager.economy().externalCurrencyId());
        root.addProperty("taxSinkAccountUuid", ConfigManager.economy().taxSinkAccountUuid());
        root.addProperty("blockArtificialMobRewards", ConfigManager.COMMON.blockArtificialMobRewards.get());
        root.addProperty("blockBabyMobRewards", ConfigManager.COMMON.blockBabyMobRewards.get());
        root.addProperty("blockTamedMobRewards", ConfigManager.COMMON.blockTamedMobRewards.get());
        root.addProperty("lootContainerRewardCooldownSeconds", ConfigManager.COMMON.lootContainerRewardCooldownSeconds.get());
        root.addProperty("exploredChunkRewardCooldownSeconds", ConfigManager.COMMON.exploredChunkRewardCooldownSeconds.get());
        String worldId = player.serverLevel().dimension().location().toString();
        String biomeId = player.serverLevel().getBiome(player.blockPosition()).unwrapKey().map(key -> key.location().toString()).orElse("minecraft:plains");
        double worldMultiplier = ConfigManager.economy().worldMultipliers().getOrDefault(worldId, 1.0D);
        double biomeMultiplier = ConfigManager.economy().biomeMultipliers().getOrDefault(biomeId, 1.0D);
        root.addProperty("currentWorldId", worldId);
        root.addProperty("currentBiomeId", biomeId);
        root.addProperty("worldRewardMultiplier", worldMultiplier);
        root.addProperty("biomeRewardMultiplier", biomeMultiplier);
        root.addProperty("eventRewardMultiplier", ConfigManager.economy().eventMultiplier());
        root.addProperty("eventEndsAtEpochSecond", ConfigManager.economy().eventEndsAtEpochSecond());
        root.addProperty("eventRemainingSeconds", Math.max(0L, ConfigManager.economy().eventEndsAtEpochSecond() - TimeUtil.now()));
        root.addProperty("vipRewardMultiplier", ConfigManager.economy().vipMultiplier());
        root.addProperty("effectiveRewardMultiplier",
            worldMultiplier * biomeMultiplier * ConfigManager.economy().eventMultiplier() * ConfigManager.economy().vipMultiplier());
        JsonArray unlockedTitles = new JsonArray();
        profile.unlockedTitles().forEach(unlockedTitles::add);
        root.add("unlockedTitles", unlockedTitles);
        JsonArray jobs = new JsonArray();
        for (JobDefinition definition : registry.all()) {
            JobProgress progress = profile.progress(definition.id());
            JsonObject job = new JsonObject();
            job.addProperty("id", definition.id());
            job.addProperty("level", progress.level());
            job.addProperty("xp", progress.xp());
            job.addProperty("pendingSalary", progress.pendingSalary());
            job.addProperty("earnedTotal", progress.earnedTotal());
            job.addProperty("skillPoints", profile.availableSkillPoints(definition.id()));
            job.addProperty("milestoneCount", progress.unlockedMilestones().size());
            JsonArray milestones = new JsonArray();
            progress.unlockedMilestones().forEach(milestones::add);
            job.add("milestones", milestones);
            JsonArray unlockedNodes = new JsonArray();
            progress.unlockedNodes().stream()
                .filter(state -> state.unlocked())
                .forEach(state -> unlockedNodes.add(state.nodeId()));
            job.add("unlockedNodes", unlockedNodes);
            if (assignedJobs.contains(definition.id())) {
                ensureAssignments(profile, definition.id());
                progress.dailyTasks().stream()
                    .mapToLong(DailyTaskProgress::resetEpochSecond)
                    .min()
                    .ifPresent(resetAt -> job.addProperty("dailyResetAt", resetAt));
                JsonArray daily = new JsonArray();
                for (DailyTaskProgress task : progress.dailyTasks()) {
                    JsonObject dailyObj = new JsonObject();
                    dailyObj.addProperty("id", task.taskId());
                    dailyObj.addProperty("progress", task.progress());
                    dailyObj.addProperty("target", task.target());
                    dailyObj.addProperty("completed", task.completed());
                    ConfigManager.dailyTasks().tasksForJob(definition.id()).stream()
                        .filter(template -> template.id().equals(task.taskId()))
                        .findFirst()
                        .ifPresent(template -> {
                            dailyObj.addProperty("type", template.type().name());
                            dailyObj.addProperty("targetId", template.target().toString());
                            dailyObj.addProperty("salaryReward", template.salary());
                            dailyObj.addProperty("xpReward", template.xp());
                            if (template.bonusItem() != null && template.bonusCount() > 0) {
                                dailyObj.addProperty("bonusItem", template.bonusItem());
                                dailyObj.addProperty("bonusCount", template.bonusCount());
                            }
                            if (template.buffEffect() != null && template.buffDurationSeconds() > 0) {
                                dailyObj.addProperty("buffEffect", template.buffEffect());
                                dailyObj.addProperty("buffDurationSeconds", template.buffDurationSeconds());
                                dailyObj.addProperty("buffAmplifier", template.buffAmplifier());
                            }
                            if (template.bonusTitle() != null) {
                                dailyObj.addProperty("bonusTitle", template.bonusTitle());
                            }
                        });
                    daily.add(dailyObj);
                }
                job.add("dailyTasks", daily);
                job.addProperty("completedDailyCount", progress.dailyHistory().size());
                JsonArray dailyHistory = new JsonArray();
                progress.dailyHistory().stream().limit(3).forEach(entry -> {
                    JsonObject historyObj = new JsonObject();
                    historyObj.addProperty("id", entry.taskId());
                    historyObj.addProperty("completedAt", entry.completedAtEpochSecond());
                    historyObj.addProperty("salaryReward", entry.salaryReward());
                    historyObj.addProperty("xpReward", entry.xpReward());
                    ConfigManager.dailyTasks().tasksForJob(definition.id()).stream()
                        .filter(template -> template.id().equals(entry.taskId()))
                        .findFirst()
                        .ifPresent(template -> {
                            historyObj.addProperty("type", template.type().name());
                            historyObj.addProperty("targetId", template.target().toString());
                            if (template.bonusItem() != null && template.bonusCount() > 0) {
                                historyObj.addProperty("bonusItem", template.bonusItem());
                                historyObj.addProperty("bonusCount", template.bonusCount());
                            }
                            if (template.buffEffect() != null && template.buffDurationSeconds() > 0) {
                                historyObj.addProperty("buffEffect", template.buffEffect());
                                historyObj.addProperty("buffDurationSeconds", template.buffDurationSeconds());
                                historyObj.addProperty("buffAmplifier", template.buffAmplifier());
                            }
                            if (template.bonusTitle() != null) {
                                historyObj.addProperty("bonusTitle", template.bonusTitle());
                            }
                        });
                    dailyHistory.add(historyObj);
                });
                job.add("dailyHistory", dailyHistory);
                JsonArray contracts = new JsonArray();
                for (ContractProgress contract : progress.contracts()) {
                    JsonObject contractObj = new JsonObject();
                    contractObj.addProperty("id", contract.contractId());
                    contractObj.addProperty("rarity", contract.rarity());
                    contractObj.addProperty("progress", contract.progress());
                    contractObj.addProperty("target", contract.target());
                    contractObj.addProperty("completed", contract.completed());
                    contractObj.addProperty("expiresAt", contract.expiresAtEpochSecond());
                    ConfigManager.contracts().contractsForJob(definition.id()).stream()
                        .filter(template -> template.id().equals(contract.contractId()))
                        .findFirst()
                        .ifPresent(template -> {
                            contractObj.addProperty("type", template.type().name());
                            contractObj.addProperty("targetId", template.target().toString());
                            contractObj.addProperty("salaryReward", template.salary());
                            contractObj.addProperty("xpReward", template.xp());
                            if (template.bonusItem() != null && template.bonusCount() > 0) {
                                contractObj.addProperty("bonusItem", template.bonusItem());
                                contractObj.addProperty("bonusCount", template.bonusCount());
                            }
                            if (template.buffEffect() != null && template.buffDurationSeconds() > 0) {
                                contractObj.addProperty("buffEffect", template.buffEffect());
                                contractObj.addProperty("buffDurationSeconds", template.buffDurationSeconds());
                                contractObj.addProperty("buffAmplifier", template.buffAmplifier());
                            }
                            if (template.bonusTitle() != null) {
                                contractObj.addProperty("bonusTitle", template.bonusTitle());
                            }
                        });
                    contracts.add(contractObj);
                }
                job.add("contracts", contracts);
                job.addProperty("completedContractCount", progress.contractHistory().size());
                JsonArray contractHistory = new JsonArray();
                progress.contractHistory().stream().limit(3).forEach(entry -> {
                    JsonObject historyObj = new JsonObject();
                    historyObj.addProperty("id", entry.contractId());
                    historyObj.addProperty("rarity", entry.rarity());
                    historyObj.addProperty("completedAt", entry.completedAtEpochSecond());
                    historyObj.addProperty("salaryReward", entry.salaryReward());
                    historyObj.addProperty("xpReward", entry.xpReward());
                    ConfigManager.contracts().contractsForJob(definition.id()).stream()
                        .filter(template -> template.id().equals(entry.contractId()))
                        .findFirst()
                        .ifPresent(template -> {
                            historyObj.addProperty("type", template.type().name());
                            historyObj.addProperty("targetId", template.target().toString());
                            if (template.bonusItem() != null && template.bonusCount() > 0) {
                                historyObj.addProperty("bonusItem", template.bonusItem());
                                historyObj.addProperty("bonusCount", template.bonusCount());
                            }
                            if (template.buffEffect() != null && template.buffDurationSeconds() > 0) {
                                historyObj.addProperty("buffEffect", template.buffEffect());
                                historyObj.addProperty("buffDurationSeconds", template.buffDurationSeconds());
                                historyObj.addProperty("buffAmplifier", template.buffAmplifier());
                            }
                            if (template.bonusTitle() != null) {
                                historyObj.addProperty("bonusTitle", template.bonusTitle());
                            }
                        });
                    contractHistory.add(historyObj);
                });
                job.add("contractHistory", contractHistory);
                progress.contracts().stream()
                    .mapToLong(ContractProgress::expiresAtEpochSecond)
                    .min()
                    .ifPresent(expiresAt -> job.addProperty("nextContractRotationAt", expiresAt));
            }
            jobs.add(job);
        }
        root.add("jobs", jobs);
        return ConfigManager.GSON.toJson(root);
    }

    private void expireRewardEventIfNeeded() {
        long endsAt = ConfigManager.economy().eventEndsAtEpochSecond();
        if (endsAt <= 0L || endsAt > TimeUtil.now()) {
            return;
        }
        if (Double.compare(ConfigManager.economy().eventMultiplier(), 1.0D) == 0) {
            ConfigManager.economy().setEventEndsAtEpochSecond(0L);
            ConfigManager.saveEconomyConfig();
            return;
        }
        ConfigManager.economy().setEventMultiplier(1.0D);
        ConfigManager.economy().setEventEndsAtEpochSecond(0L);
        ConfigManager.saveEconomyConfig();
        if (server != null) {
            server.getPlayerList().broadcastSystemMessage(TextUtil.tr("message.advancedjobs.event_stopped"), false);
            server.getPlayerList().getPlayers().forEach(this::syncToPlayer);
        }
    }

    private String createLeaderboardPayload(String jobId) {
        JsonArray top = new JsonArray();
        int i = 0;
        Iterable<PlayerJobProfile> entries = "all".equalsIgnoreCase(jobId) ? overallLeaderboard() : leaderboard(jobId);
        for (PlayerJobProfile leaderboardEntry : entries) {
            if (i++ >= LEADERBOARD_SYNC_LIMIT) {
                break;
            }
            JsonObject entry = new JsonObject();
            entry.addProperty("player", leaderboardEntry.playerName());
            entry.addProperty("jobId", jobId);
            if ("all".equalsIgnoreCase(jobId)) {
                entry.addProperty("level", totalLevelsAcrossJobs(leaderboardEntry));
                entry.addProperty("xp", leaderboardEntry.jobs().values().stream().mapToDouble(JobProgress::xp).sum());
                entry.addProperty("earned", totalEarnedAcrossJobs(leaderboardEntry));
            } else {
                entry.addProperty("level", leaderboardEntry.progress(jobId).level());
                entry.addProperty("xp", leaderboardEntry.progress(jobId).xp());
                entry.addProperty("earned", leaderboardEntry.progress(jobId).earnedTotal());
            }
            top.add(entry);
        }
        return ConfigManager.GSON.toJson(top);
    }

    private void ensureAssignments(PlayerJobProfile profile, String jobId) {
        JobProgress progress = profile.progress(jobId);
        long resetAt = TimeUtil.nextResetEpochSecond(java.time.ZoneId.systemDefault(), ConfigManager.dailyResetTime());
        long now = TimeUtil.now();
        if (progress.dailyTasks().isEmpty() || progress.dailyTasks().stream().anyMatch(task -> task.resetEpochSecond() <= now)) {
            progress.dailyTasks().clear();
            for (ConfigManager.DailyTaskTemplate template : ConfigManager.dailyTasks().tasksForJob(jobId).stream().limit(3).toList()) {
                progress.dailyTasks().add(new DailyTaskProgress(template.id(), jobId, template.goal(), 0, resetAt, false));
            }
        }
        progress.contracts().removeIf(contract -> contract.expiresAtEpochSecond() < now);
        List<ConfigManager.ContractTemplate> desiredContracts = pickContracts(jobId);
        for (ConfigManager.ContractTemplate template : desiredContracts) {
            boolean alreadyPresent = progress.contracts().stream()
                .anyMatch(contract -> contract.contractId().equals(template.id()) && contract.expiresAtEpochSecond() >= now);
            if (!alreadyPresent) {
                progress.contracts().add(new ContractProgress(template.id(), jobId, template.rarity(), template.goal(), 0, now + template.durationSeconds(), false));
            }
        }
    }

    private List<ConfigManager.ContractTemplate> pickContracts(String jobId) {
        List<ConfigManager.ContractTemplate> templates = ConfigManager.contracts().contractsForJob(jobId);
        if (templates.isEmpty()) {
            return List.of();
        }
        List<ConfigManager.ContractTemplate> picked = new java.util.ArrayList<>();
        pickRandomByRarity(templates, "common").ifPresent(picked::add);
        pickRandomByRarity(templates, "rare").ifPresent(picked::add);
        pickRandomByRarity(templates, "elite").ifPresent(picked::add);
        if (picked.isEmpty()) {
            return templates.stream().limit(3).toList();
        }
        return picked;
    }

    private Optional<ConfigManager.ContractTemplate> pickRandomByRarity(List<ConfigManager.ContractTemplate> templates, String rarity) {
        List<ConfigManager.ContractTemplate> filtered = templates.stream()
            .filter(template -> rarity.equalsIgnoreCase(template.rarity()))
            .toList();
        if (filtered.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(filtered.get(ThreadLocalRandom.current().nextInt(filtered.size())));
    }

    private void updateDailyTasks(ServerPlayer player, PlayerJobProfile profile, String jobId, JobActionContext context, RewardDefinition reward) {
        JobProgress progress = profile.progress(jobId);
        for (DailyTaskProgress task : progress.dailyTasks()) {
            ConfigManager.dailyTasks().tasksForJob(jobId).stream()
                .filter(template -> template.id().equals(task.taskId()))
                .filter(template -> template.type() == context.actionType() && template.target().equals(context.targetId()))
                .findFirst()
                .ifPresent(template -> {
                    boolean wasComplete = task.completed();
                    task.addProgress(1);
                    if (!wasComplete && task.completed()) {
                        creditSalary(player, profile, jobId, template.salary(), "daily_task");
                        progress.addXp(template.xp());
                        grantTaskExtras(player, template.bonusItem(), template.bonusCount(),
                            template.buffEffect(), template.buffDurationSeconds(), template.buffAmplifier());
                        grantTitle(player, profile, template.bonusTitle());
                        player.sendSystemMessage(completionMessage("message.advancedjobs.daily_completed", template.type().name(), template.target().toString(),
                            template.salary(), template.xp(), template.bonusItem(), template.bonusCount(),
                            template.buffEffect(), template.buffDurationSeconds(), template.buffAmplifier()));
                        progress.recordDailyCompletion(template.id(), TimeUtil.now(), template.salary(), template.xp());
                    }
                });
        }
    }

    private void updateContracts(ServerPlayer player, PlayerJobProfile profile, String jobId, JobActionContext context, RewardDefinition reward) {
        JobProgress progress = profile.progress(jobId);
        for (ContractProgress contract : progress.contracts()) {
            ConfigManager.contracts().contractsForJob(jobId).stream()
                .filter(template -> template.id().equals(contract.contractId()))
                .filter(template -> template.type() == context.actionType() && template.target().equals(context.targetId()))
                .findFirst()
                .ifPresent(template -> {
                    boolean wasComplete = contract.completed();
                    contract.addProgress(1);
                    if (!wasComplete && contract.completed()) {
                        creditSalary(player, profile, jobId, template.salary(), "job_contract");
                        progress.addXp(template.xp());
                        grantTaskExtras(player, template.bonusItem(), template.bonusCount(),
                            template.buffEffect(), template.buffDurationSeconds(), template.buffAmplifier());
                        grantTitle(player, profile, template.bonusTitle());
                        player.sendSystemMessage(completionMessage("message.advancedjobs.contract_completed", template.type().name(), template.target().toString(),
                            template.salary(), template.xp(), template.bonusItem(), template.bonusCount(),
                            template.buffEffect(), template.buffDurationSeconds(), template.buffAmplifier()));
                        progress.recordContractCompletion(template.id(), template.rarity(), TimeUtil.now(), template.salary(), template.xp());
                    }
                });
        }
    }

    private void grantTaskExtras(ServerPlayer player, String bonusItemId, int bonusCount, String buffEffectId, int buffDurationSeconds, int buffAmplifier) {
        if (bonusItemId != null && !bonusItemId.isBlank() && bonusCount > 0) {
            try {
                Item item = ForgeRegistries.ITEMS.getValue(ResourceLocationUtil.parse(bonusItemId));
                if (item != null) {
                    ItemStack stack = new ItemStack(item, bonusCount);
                    if (!player.getInventory().add(stack)) {
                        player.drop(stack, false);
                    }
                }
            } catch (Exception ignored) {
            }
        }
        if (buffEffectId != null && !buffEffectId.isBlank() && buffDurationSeconds > 0) {
            try {
                MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocationUtil.parse(buffEffectId));
                if (effect != null) {
                    player.addEffect(new MobEffectInstance(effect, buffDurationSeconds * 20, Math.max(0, buffAmplifier)));
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void grantTitle(ServerPlayer player, PlayerJobProfile profile, String titleId) {
        if (profile.unlockTitle(titleId)) {
            player.sendSystemMessage(TextUtil.tr("message.advancedjobs.title_unlocked", titleComponent(titleId)));
        }
    }

    private void evaluateMilestones(ServerPlayer player, PlayerJobProfile profile, String jobId, JobProgress progress) {
        unlockMilestoneIf(player, progress, "level_10", progress.level() >= 10);
        unlockMilestoneIf(player, progress, "level_25", progress.level() >= 25);
        unlockMilestoneIf(player, progress, "level_50", progress.level() >= 50);
        unlockMilestoneIf(player, progress, "actions_100", totalActionCount(progress) >= 100);
        unlockMilestoneIf(player, progress, "salary_10000", progress.earnedTotal() >= 10_000.0D);
        unlockMilestoneIf(player, progress, "daily_10", progress.dailyHistory().size() >= 10);
        unlockMilestoneIf(player, progress, "contracts_10", progress.contractHistory().size() >= 10);
        evaluateProfessionMilestones(player, progress, jobId);
        profile.markDirty();
    }

    private void evaluateProfessionMilestones(ServerPlayer player, JobProgress progress, String jobId) {
        switch (jobId) {
            case "miner", "deep_miner" -> {
                unlockMilestoneIf(player, progress, "miner_diamond_25",
                    actionCount(progress, "BREAK_BLOCK|minecraft:diamond_ore") + actionCount(progress, "BREAK_BLOCK|minecraft:deepslate_diamond_ore") >= 25);
                unlockMilestoneIf(player, progress, "miner_redstone_100",
                    actionCount(progress, "BREAK_BLOCK|minecraft:redstone_ore") >= 100);
                unlockMilestoneIf(player, progress, "miner_emerald_16",
                    actionCount(progress, "BREAK_BLOCK|minecraft:emerald_ore") + actionCount(progress, "BREAK_BLOCK|minecraft:deepslate_emerald_ore") >= 16);
                unlockMilestoneIf(player, progress, "deep_miner_deepslate_750",
                    actionCount(progress, "BREAK_BLOCK|minecraft:deepslate") >= 750);
                unlockMilestoneIf(player, progress, "deep_miner_diamond_48",
                    actionCount(progress, "BREAK_BLOCK|minecraft:deepslate_diamond_ore") >= 48);
            }
            case "lumberjack", "forester" -> {
                unlockMilestoneIf(player, progress, "lumberjack_logs_500", totalBreakCount(progress) >= 500);
                unlockMilestoneIf(player, progress, "lumberjack_oak_250",
                    actionCount(progress, "BREAK_BLOCK|minecraft:oak_log") + actionCount(progress, "BREAK_BLOCK|minecraft:stripped_oak_log") >= 250);
                unlockMilestoneIf(player, progress, "forester_cherry_100", actionCount(progress, "BREAK_BLOCK|minecraft:cherry_log") >= 100);
                unlockMilestoneIf(player, progress, "forester_birch_150", actionCount(progress, "BREAK_BLOCK|minecraft:birch_log") >= 150);
                unlockMilestoneIf(player, progress, "forester_spruce_180", actionCount(progress, "BREAK_BLOCK|minecraft:spruce_log") >= 180);
            }
            case "farmer", "harvester" -> {
                unlockMilestoneIf(player, progress, "farmer_harvest_500",
                    totalActionTypeCount(progress, "HARVEST_CROP") >= 500);
                unlockMilestoneIf(player, progress, "farmer_planter_250",
                    totalActionTypeCount(progress, "PLANT_CROP") >= 250);
                unlockMilestoneIf(player, progress, "farmer_wheat_384",
                    actionCount(progress, "HARVEST_CROP|minecraft:wheat") >= 384);
                unlockMilestoneIf(player, progress, "harvester_patch_128",
                    actionCount(progress, "HARVEST_CROP|minecraft:pumpkin") + actionCount(progress, "HARVEST_CROP|minecraft:melon") >= 128);
                unlockMilestoneIf(player, progress, "harvester_roots_256",
                    actionCount(progress, "HARVEST_CROP|minecraft:potatoes")
                        + actionCount(progress, "HARVEST_CROP|minecraft:carrots")
                        + actionCount(progress, "HARVEST_CROP|minecraft:beetroots") >= 256);
            }
            case "fisher" -> {
                unlockMilestoneIf(player, progress, "fisher_cod_128",
                    actionCount(progress, "FISH|minecraft:cod") >= 128);
                unlockMilestoneIf(player, progress, "fisher_treasure_24",
                    actionCount(progress, "FISH|minecraft:name_tag") + actionCount(progress, "FISH|minecraft:enchanted_book") >= 24);
                unlockMilestoneIf(player, progress, "fisher_salmon_96",
                    actionCount(progress, "FISH|minecraft:salmon") >= 96);
            }
            case "animal_breeder" -> {
                unlockMilestoneIf(player, progress, "animal_breeder_stock_150",
                    totalActionTypeCount(progress, "BREED_ANIMAL") >= 150);
                unlockMilestoneIf(player, progress, "animal_breeder_cattle_75",
                    actionCount(progress, "BREED_ANIMAL|minecraft:cow") >= 75);
                unlockMilestoneIf(player, progress, "animal_breeder_chicken_96",
                    actionCount(progress, "BREED_ANIMAL|minecraft:chicken") >= 96);
            }
            case "hunter", "monster_slayer" -> {
                unlockMilestoneIf(player, progress, "hunter_zombie_100", actionCount(progress, "KILL_MOB|minecraft:zombie") >= 100);
                unlockMilestoneIf(player, progress, "hunter_spider_120",
                    actionCount(progress, "KILL_MOB|minecraft:spider") + actionCount(progress, "KILL_MOB|minecraft:cave_spider") >= 120);
                unlockMilestoneIf(player, progress, "monster_slayer_creeper_120",
                    actionCount(progress, "KILL_MOB|minecraft:creeper") >= 120);
            }
            case "builder", "mason", "carpenter" -> {
                unlockMilestoneIf(player, progress, "builder_blocks_500", totalActionTypeCount(progress, "PLACE_BLOCK") >= 500);
                unlockMilestoneIf(player, progress, "builder_decor_200",
                    actionCount(progress, "PLACE_BLOCK|minecraft:terracotta") + actionCount(progress, "PLACE_BLOCK|minecraft:glass") >= 200);
                unlockMilestoneIf(player, progress, "builder_glass_150",
                    actionCount(progress, "PLACE_BLOCK|minecraft:glass") + actionCount(progress, "PLACE_BLOCK|minecraft:glass_pane") >= 150);
                unlockMilestoneIf(player, progress, "mason_bricks_300",
                    actionCount(progress, "PLACE_BLOCK|minecraft:stone_bricks")
                        + actionCount(progress, "PLACE_BLOCK|minecraft:bricks") >= 300);
                unlockMilestoneIf(player, progress, "mason_polished_220",
                    actionCount(progress, "PLACE_BLOCK|minecraft:polished_andesite") >= 220);
                unlockMilestoneIf(player, progress, "carpenter_planks_400",
                    actionCount(progress, "PLACE_BLOCK|minecraft:oak_planks")
                        + actionCount(progress, "PLACE_BLOCK|minecraft:spruce_planks")
                        + actionCount(progress, "PLACE_BLOCK|minecraft:birch_planks") >= 400);
                unlockMilestoneIf(player, progress, "carpenter_stairs_240",
                    actionCount(progress, "PLACE_BLOCK|minecraft:oak_stairs")
                        + actionCount(progress, "PLACE_BLOCK|minecraft:spruce_stairs")
                        + actionCount(progress, "PLACE_BLOCK|minecraft:birch_stairs") >= 240);
            }
            case "merchant" -> {
                unlockMilestoneIf(player, progress, "merchant_trade_100", totalActionTypeCount(progress, "TRADE_WITH_VILLAGER") >= 100);
                unlockMilestoneIf(player, progress, "merchant_emerald_256",
                    actionCount(progress, "TRADE_WITH_VILLAGER|minecraft:emerald") >= 256);
                unlockMilestoneIf(player, progress, "merchant_cache_24",
                    totalActionTypeCount(progress, "OPEN_LOOT_CHEST") >= 24);
            }
            case "alchemist", "enchanter" -> {
                unlockMilestoneIf(player, progress, "alchemist_brew_64", totalActionTypeCount(progress, "BREW_POTION") >= 64);
                unlockMilestoneIf(player, progress, "enchanter_books_32", actionCount(progress, "ENCHANT_ITEM|minecraft:book") >= 32);
                unlockMilestoneIf(player, progress, "alchemist_glass_128",
                    actionCount(progress, "BREW_POTION|minecraft:glass_bottle") >= 128);
                unlockMilestoneIf(player, progress, "enchanter_table_96",
                    totalActionTypeCount(progress, "ENCHANT_ITEM") >= 96);
                unlockMilestoneIf(player, progress, "alchemist_wart_128",
                    actionCount(progress, "BREW_POTION|minecraft:nether_wart") >= 128);
                unlockMilestoneIf(player, progress, "enchanter_lapis_192",
                    actionCount(progress, "ENCHANT_ITEM|minecraft:lapis_lazuli") >= 192);
            }
            case "blacksmith" -> {
                unlockMilestoneIf(player, progress, "blacksmith_iron_128",
                    actionCount(progress, "SMELT_ITEM|minecraft:iron_ingot") >= 128);
                unlockMilestoneIf(player, progress, "blacksmith_gold_96",
                    actionCount(progress, "SMELT_ITEM|minecraft:gold_ingot") >= 96);
            }
            case "armorer" -> {
                unlockMilestoneIf(player, progress, "armorer_iron_set_48",
                    actionCount(progress, "CRAFT_ITEM|minecraft:iron_chestplate")
                        + actionCount(progress, "CRAFT_ITEM|minecraft:iron_helmet")
                        + actionCount(progress, "CRAFT_ITEM|minecraft:iron_leggings")
                        + actionCount(progress, "CRAFT_ITEM|minecraft:iron_boots") >= 48);
                unlockMilestoneIf(player, progress, "armorer_shield_64",
                    actionCount(progress, "CRAFT_ITEM|minecraft:shield") >= 64);
            }
            case "cook" -> {
                unlockMilestoneIf(player, progress, "cook_feast_128",
                    actionCount(progress, "CRAFT_ITEM|minecraft:bread")
                        + actionCount(progress, "CRAFT_ITEM|minecraft:cooked_beef")
                        + actionCount(progress, "CRAFT_ITEM|minecraft:baked_potato") >= 128);
                unlockMilestoneIf(player, progress, "cook_sweets_96",
                    actionCount(progress, "CRAFT_ITEM|minecraft:cake")
                        + actionCount(progress, "CRAFT_ITEM|minecraft:pumpkin_pie") >= 96);
            }
            case "explorer", "treasure_hunter", "archaeologist" -> {
                unlockMilestoneIf(player, progress, "explorer_chunks_64", totalActionTypeCount(progress, "EXPLORE_CHUNK") >= 64);
                unlockMilestoneIf(player, progress, "treasure_loot_25", totalActionTypeCount(progress, "OPEN_LOOT_CHEST") >= 25);
                unlockMilestoneIf(player, progress, "explorer_cache_40",
                    totalActionTypeCount(progress, "OPEN_LOOT_CHEST") >= 40);
                unlockMilestoneIf(player, progress, "explorer_distance_128",
                    totalActionTypeCount(progress, "EXPLORE_CHUNK") >= 128);
                unlockMilestoneIf(player, progress, "explorer_route_192",
                    totalActionTypeCount(progress, "EXPLORE_CHUNK") >= 192);
                unlockMilestoneIf(player, progress, "treasure_hunter_tags_16",
                    actionCount(progress, "FISH|minecraft:name_tag") + actionCount(progress, "OPEN_LOOT_CHEST|minecraft:name_tag") >= 16);
                unlockMilestoneIf(player, progress, "treasure_hunter_loot_80",
                    totalActionTypeCount(progress, "OPEN_LOOT_CHEST") >= 80);
                unlockMilestoneIf(player, progress, "archaeologist_relic_32",
                    actionCount(progress, "OPEN_LOOT_CHEST|minecraft:brush")
                        + actionCount(progress, "BREAK_BLOCK|minecraft:suspicious_sand")
                        + actionCount(progress, "BREAK_BLOCK|minecraft:suspicious_gravel") >= 32);
                unlockMilestoneIf(player, progress, "archaeologist_brush_96",
                    actionCount(progress, "OPEN_LOOT_CHEST|minecraft:brush") >= 96);
                unlockMilestoneIf(player, progress, "archaeologist_suspicious_96",
                    actionCount(progress, "BREAK_BLOCK|minecraft:suspicious_sand")
                        + actionCount(progress, "BREAK_BLOCK|minecraft:suspicious_gravel")
                        + actionCount(progress, "OPEN_LOOT_CHEST|minecraft:suspicious_sand") >= 96);
            }
            case "engineer", "redstone_technician" -> {
                unlockMilestoneIf(player, progress, "engineer_redstone_250", totalActionTypeCount(progress, "REDSTONE_USE") >= 250);
                unlockMilestoneIf(player, progress, "engineer_quartz_128",
                    actionCount(progress, "CRAFT_ITEM|minecraft:comparator")
                        + actionCount(progress, "CRAFT_ITEM|minecraft:observer")
                        + actionCount(progress, "CRAFT_ITEM|minecraft:daylight_detector") >= 128);
                unlockMilestoneIf(player, progress, "redstone_technician_repeaters_192",
                    actionCount(progress, "CRAFT_ITEM|minecraft:repeater")
                        + actionCount(progress, "CRAFT_ITEM|minecraft:redstone_torch") >= 192);
                unlockMilestoneIf(player, progress, "redstone_technician_comparators_96",
                    actionCount(progress, "CRAFT_ITEM|minecraft:comparator") >= 96);
            }
            case "guard", "bounty_hunter", "defender" -> {
                unlockMilestoneIf(player, progress, "guard_patrol_150", totalActionTypeCount(progress, "KILL_MOB") >= 150);
                unlockMilestoneIf(player, progress, "bounty_hunter_blaze_50",
                    actionCount(progress, "KILL_MOB|minecraft:blaze") >= 50);
                unlockMilestoneIf(player, progress, "defender_skeleton_120",
                    actionCount(progress, "KILL_MOB|minecraft:skeleton") + actionCount(progress, "KILL_MOB|minecraft:stray") >= 120);
                unlockMilestoneIf(player, progress, "guard_raider_80",
                    actionCount(progress, "KILL_MOB|minecraft:pillager")
                        + actionCount(progress, "KILL_MOB|minecraft:vindicator") >= 80);
                unlockMilestoneIf(player, progress, "bounty_hunter_witch_36",
                    actionCount(progress, "KILL_MOB|minecraft:witch") >= 36);
                unlockMilestoneIf(player, progress, "defender_zombie_180",
                    actionCount(progress, "KILL_MOB|minecraft:zombie")
                        + actionCount(progress, "KILL_MOB|minecraft:husk") >= 180);
            }
            case "boss_hunter" -> {
                unlockMilestoneIf(player, progress, "boss_hunter_boss_5", totalActionTypeCount(progress, "KILL_BOSS") >= 5);
                unlockMilestoneIf(player, progress, "boss_hunter_boss_12", totalActionTypeCount(progress, "KILL_BOSS") >= 12);
            }
            case "quarry_worker" -> {
                unlockMilestoneIf(player, progress, "quarry_stone_1000", actionCount(progress, "BREAK_BLOCK|minecraft:stone") >= 1000);
                unlockMilestoneIf(player, progress, "quarry_deepslate_600",
                    actionCount(progress, "BREAK_BLOCK|minecraft:deepslate") >= 600);
            }
            case "digger" -> {
                unlockMilestoneIf(player, progress, "digger_gravel_500", actionCount(progress, "BREAK_BLOCK|minecraft:gravel") >= 500);
                unlockMilestoneIf(player, progress, "digger_clay_320",
                    actionCount(progress, "BREAK_BLOCK|minecraft:clay") + actionCount(progress, "BREAK_BLOCK|minecraft:mud") >= 320);
            }
            case "sand_collector" -> {
                unlockMilestoneIf(player, progress, "sand_collector_sand_1000", actionCount(progress, "BREAK_BLOCK|minecraft:sand") >= 1000);
                unlockMilestoneIf(player, progress, "sand_collector_red_400",
                    actionCount(progress, "BREAK_BLOCK|minecraft:red_sand") >= 400);
            }
            case "ice_harvester" -> {
                unlockMilestoneIf(player, progress, "ice_harvester_blue_100", actionCount(progress, "BREAK_BLOCK|minecraft:blue_ice") >= 100);
                unlockMilestoneIf(player, progress, "ice_harvester_packed_320",
                    actionCount(progress, "BREAK_BLOCK|minecraft:packed_ice") >= 320);
            }
            case "shepherd" -> {
                unlockMilestoneIf(player, progress, "shepherd_flock_100", actionCount(progress, "BREED_ANIMAL|minecraft:sheep") >= 100);
                unlockMilestoneIf(player, progress, "shepherd_wool_256",
                    actionCount(progress, "CRAFT_ITEM|minecraft:white_wool")
                        + actionCount(progress, "CRAFT_ITEM|minecraft:black_wool")
                        + actionCount(progress, "CRAFT_ITEM|minecraft:gray_wool") >= 256);
                unlockMilestoneIf(player, progress, "shepherd_carpet_192",
                    actionCount(progress, "CRAFT_ITEM|minecraft:white_carpet")
                        + actionCount(progress, "CRAFT_ITEM|minecraft:red_carpet")
                        + actionCount(progress, "CRAFT_ITEM|minecraft:black_carpet") >= 192);
            }
            case "beekeeper" -> {
                unlockMilestoneIf(player, progress, "beekeeper_honey_128", actionCount(progress, "CRAFT_ITEM|minecraft:honey_bottle") >= 128);
                unlockMilestoneIf(player, progress, "beekeeper_comb_192",
                    actionCount(progress, "BREAK_BLOCK|minecraft:beehive")
                        + actionCount(progress, "BREAK_BLOCK|minecraft:bee_nest")
                        + actionCount(progress, "CRAFT_ITEM|minecraft:honeycomb") >= 192);
                unlockMilestoneIf(player, progress, "beekeeper_hive_64",
                    actionCount(progress, "BREAK_BLOCK|minecraft:beehive")
                        + actionCount(progress, "BREAK_BLOCK|minecraft:bee_nest") >= 64);
            }
            case "herbalist" -> {
                unlockMilestoneIf(player, progress, "herbalist_wart_256", actionCount(progress, "BREAK_BLOCK|minecraft:nether_wart") >= 256);
                unlockMilestoneIf(player, progress, "herbalist_berries_192",
                    actionCount(progress, "BREAK_BLOCK|minecraft:sweet_berry_bush")
                        + actionCount(progress, "BREAK_BLOCK|minecraft:cave_vines") >= 192);
                unlockMilestoneIf(player, progress, "herbalist_potion_72",
                    totalActionTypeCount(progress, "BREW_POTION") >= 72);
            }
            default -> {
            }
        }
    }

    private void unlockMilestoneIf(ServerPlayer player, JobProgress progress, String milestoneId, boolean condition) {
        if (condition && progress.unlockMilestone(milestoneId)) {
            player.sendSystemMessage(TextUtil.tr("message.advancedjobs.milestone_unlocked",
                TextUtil.tr("milestone.advancedjobs." + milestoneId)));
            grantTitleForMilestone(player, getOrCreateProfile(player), milestoneId);
        }
    }

    private int totalActionCount(JobProgress progress) {
        return progress.actionStats().values().stream().mapToInt(Integer::intValue).sum();
    }

    private int totalActionTypeCount(JobProgress progress, String actionType) {
        return progress.actionStats().entrySet().stream()
            .filter(entry -> entry.getKey().startsWith(actionType + "|"))
            .mapToInt(Map.Entry::getValue)
            .sum();
    }

    private int totalBreakCount(JobProgress progress) {
        return totalActionTypeCount(progress, "BREAK_BLOCK");
    }

    private int actionCount(JobProgress progress, String key) {
        return progress.actionStats().getOrDefault(key, 0);
    }

    private void grantTitleForMilestone(ServerPlayer player, PlayerJobProfile profile, String milestoneId) {
        String titleId = switch (milestoneId) {
            case "miner_diamond_25" -> "gem_cutter";
            case "miner_redstone_100" -> "redstone_vein";
            case "miner_emerald_16" -> "emerald_core";
            case "deep_miner_deepslate_750" -> "deepslate_heart";
            case "deep_miner_diamond_48" -> "vein_lord";
            case "lumberjack_logs_500" -> "timber_champion";
            case "lumberjack_oak_250" -> "oak_warden";
            case "forester_cherry_100" -> "cherry_keeper";
            case "forester_birch_150" -> "birch_sentinel";
            case "forester_spruce_180" -> "evergreen_keeper";
            case "farmer_harvest_500" -> "field_master";
            case "farmer_planter_250" -> "green_thumb";
            case "farmer_wheat_384" -> "grainlord";
            case "harvester_patch_128" -> "harvest_titan";
            case "harvester_roots_256" -> "root_reaper";
            case "fisher_cod_128" -> "river_provider";
            case "fisher_treasure_24" -> "deep_angler";
            case "fisher_salmon_96" -> "current_hunter";
            case "animal_breeder_stock_150" -> "stock_breeder";
            case "animal_breeder_cattle_75" -> "cattle_baron";
            case "animal_breeder_chicken_96" -> "henkeeper";
            case "hunter_zombie_100" -> "undead_bane";
            case "hunter_spider_120" -> "web_reaper";
            case "boss_hunter_boss_5" -> "bossbreaker";
            case "monster_slayer_creeper_120" -> "blast_bane";
            case "builder_blocks_500" -> "stonehands";
            case "builder_decor_200" -> "artisan_builder";
            case "builder_glass_150" -> "lightweaver";
            case "mason_bricks_300" -> "keystone_master";
            case "mason_polished_220" -> "andesite_archon";
            case "carpenter_planks_400" -> "timberwright";
            case "carpenter_stairs_240" -> "stairwright";
            case "merchant_trade_100" -> "deal_maker";
            case "merchant_emerald_256" -> "emerald_broker";
            case "merchant_cache_24" -> "caravan_factor";
            case "alchemist_brew_64" -> "potion_savant";
            case "alchemist_glass_128" -> "flask_master";
            case "alchemist_wart_128" -> "nether_distiller";
            case "enchanter_books_32" -> "rune_reader";
            case "enchanter_table_96" -> "arcane_lector";
            case "enchanter_lapis_192" -> "lapis_scholar";
            case "blacksmith_iron_128" -> "forge_keeper";
            case "blacksmith_gold_96" -> "golden_anvil";
            case "armorer_iron_set_48" -> "steel_tailor";
            case "armorer_shield_64" -> "shieldwright";
            case "cook_feast_128" -> "feastcaller";
            case "cook_sweets_96" -> "sugar_master";
            case "explorer_chunks_64" -> "trailblazer";
            case "explorer_cache_40" -> "path_seeker";
            case "explorer_distance_128" -> "horizon_runner";
            case "explorer_route_192" -> "far_pathfinder";
            case "treasure_loot_25" -> "vault_seeker";
            case "treasure_hunter_tags_16" -> "tide_reclaimer";
            case "treasure_hunter_loot_80" -> "wreck_diviner";
            case "archaeologist_relic_32" -> "dust_curator";
            case "archaeologist_brush_96" -> "brush_keeper";
            case "archaeologist_suspicious_96" -> "relic_sifter";
            case "engineer_redstone_250" -> "circuit_master";
            case "engineer_quartz_128" -> "signal_architect";
            case "redstone_technician_repeaters_192" -> "pulse_smith";
            case "redstone_technician_comparators_96" -> "relay_lord";
            case "guard_patrol_150" -> "warden_of_roads";
            case "guard_raider_80" -> "raid_marshal";
            case "bounty_hunter_blaze_50" -> "ember_hunter";
            case "bounty_hunter_witch_36" -> "hex_tracker";
            case "defender_skeleton_120" -> "shield_of_dawn";
            case "defender_zombie_180" -> "bulwark_of_ashes";
            case "boss_hunter_boss_12" -> "void_slayer";
            case "quarry_stone_1000" -> "heart_of_stone";
            case "quarry_deepslate_600" -> "deep_quarry";
            case "digger_gravel_500" -> "graveborn_digger";
            case "digger_clay_320" -> "mud_delver";
            case "sand_collector_sand_1000" -> "sea_of_sand";
            case "sand_collector_red_400" -> "red_dune_keeper";
            case "ice_harvester_blue_100" -> "blue_frost";
            case "ice_harvester_packed_320" -> "glacier_hand";
            case "shepherd_flock_100" -> "high_shepherd";
            case "shepherd_wool_256" -> "wool_archon";
            case "shepherd_carpet_192" -> "loom_lord";
            case "beekeeper_honey_128" -> "hive_warden";
            case "beekeeper_comb_192" -> "comb_lord";
            case "beekeeper_hive_64" -> "queen_tender";
            case "herbalist_wart_256" -> "wart_whisperer";
            case "herbalist_berries_192" -> "berry_sage";
            case "herbalist_potion_72" -> "wild_apothecary";
            default -> null;
        };
        grantTitle(player, profile, titleId);
    }

    private Component completionMessage(String key, String actionType, String targetId, double salary, double xp,
                                        String bonusItemId, int bonusCount, String buffEffectId, int buffDurationSeconds, int buffAmplifier) {
        String itemText = bonusItemId != null && !bonusItemId.isBlank() && bonusCount > 0
            ? itemName(bonusItemId).getString() + " x" + bonusCount
            : TextUtil.tr("command.advancedjobs.common.none").getString();
        String buffText = buffEffectId != null && !buffEffectId.isBlank() && buffDurationSeconds > 0
            ? effectName(buffEffectId).getString() + " " + TimeUtil.formatRemainingSeconds(buffDurationSeconds) + " Lv." + (buffAmplifier + 1)
            : TextUtil.tr("command.advancedjobs.common.none").getString();
        return TextUtil.tr(key, humanizeAction(actionType), targetName(targetId), TextUtil.fmt2(salary), TextUtil.fmt2(xp), itemText, buffText);
    }

    private Component itemName(String itemId) {
        try {
            Item item = ForgeRegistries.ITEMS.getValue(ResourceLocationUtil.parse(itemId));
            if (item != null) {
                return item.getDescription();
            }
        } catch (Exception ignored) {
        }
        return Component.literal(itemId);
    }

    private Component effectName(String effectId) {
        try {
            MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocationUtil.parse(effectId));
            if (effect != null) {
                return effect.getDisplayName();
            }
        } catch (Exception ignored) {
        }
        return Component.literal(effectId);
    }

    private Component titleComponent(String titleId) {
        if (titleId == null || titleId.isBlank()) {
            return TextUtil.tr("command.advancedjobs.common.none");
        }
        return TextUtil.tr("title.advancedjobs." + titleId);
    }

    private String targetName(String targetId) {
        try {
            ResourceLocation id = ResourceLocationUtil.parse(targetId);
            Item item = ForgeRegistries.ITEMS.getValue(id);
            if (item != null) {
                return item.getDescription().getString();
            }
            var block = ForgeRegistries.BLOCKS.getValue(id);
            if (block != null) {
                return block.getName().getString();
            }
            var entity = ForgeRegistries.ENTITY_TYPES.getValue(id);
            if (entity != null) {
                return entity.getDescription().getString();
            }
        } catch (Exception ignored) {
        }
        return targetId;
    }

    private String humanizeAction(String actionType) {
        String[] parts = actionType.toLowerCase().split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }

    private void forgetProgress(PlayerJobProfile profile, String jobId) {
        if (!ConfigManager.COMMON.resetProgressOnChange.get()) {
            return;
        }
        if (ConfigManager.COMMON.storeAllJobProgress.get()) {
            JobProgress progress = profile.progress(jobId);
            progress.resetLeveling();
            progress.claimPendingSalary();
            progress.unlockedNodes().clear();
            progress.dailyTasks().clear();
            progress.contracts().clear();
            progress.dailyHistory().clear();
            progress.contractHistory().clear();
            progress.unlockedMilestones().clear();
            progress.actionStats().clear();
            profile.markDirty();
            return;
        }
        profile.jobs().remove(jobId);
        profile.markDirty();
    }

    private List<String> assignedJobIds(PlayerJobProfile profile) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        if (profile.activeJobId() != null) {
            ids.add(profile.activeJobId());
        }
        if (ConfigManager.COMMON.allowSecondaryJob.get() && profile.secondaryJobId() != null) {
            ids.add(profile.secondaryJobId());
        }
        return List.copyOf(ids);
    }
}
