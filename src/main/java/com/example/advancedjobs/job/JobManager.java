package com.example.advancedjobs.job;

import com.example.advancedjobs.AdvancedJobsMod;
import com.example.advancedjobs.api.EconomyProvider;
import com.example.advancedjobs.api.JobActionContext;
import com.example.advancedjobs.config.ConfigManager;
import com.example.advancedjobs.model.ActionRewardEntry;
import com.example.advancedjobs.model.ContractProgress;
import com.example.advancedjobs.model.DailyTaskProgress;
import com.example.advancedjobs.model.JobDefinition;
import com.example.advancedjobs.model.JobProgress;
import com.example.advancedjobs.model.PlayerJobProfile;
import com.example.advancedjobs.entity.NpcRole;
import com.example.advancedjobs.model.RewardDefinition;
import com.example.advancedjobs.util.DebugLog;
import com.example.advancedjobs.util.ResourceLocationUtil;
import com.example.advancedjobs.util.RewardUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.Collection;
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
import net.minecraftforge.registries.ForgeRegistries;

public class JobManager {
    private static final int AUTOSAVE_TICKS = 20 * 60;
    private static final int LEADERBOARD_SYNC_LIMIT = 25;

    private final JobRegistry registry = new JobRegistry();
    private final JobProgressService progressService = new JobProgressService();
    private final SkillTreeService skillTreeService = new SkillTreeService();
    private final PerkService perkService = new PerkService();
    private final JobSalaryService salaryService = new JobSalaryService(progressService);
    private final JobAssignmentService assignmentService = new JobAssignmentService(salaryService);
    private final JobProgressionService progressionService = new JobProgressionService();
    private final JobPayloadService payloadService = new JobPayloadService(assignmentService, salaryService);
    private final JobProfileMutationService mutationService = new JobProfileMutationService(assignmentService, progressionService);
    private final JobRewardService rewardService = new JobRewardService(registry, progressService, perkService, salaryService, assignmentService, progressionService);
    private final JobCacheService cacheService = new JobCacheService();
    private final JobClientSyncService clientSyncService = new JobClientSyncService();
    private final JobPersistenceService persistenceService = new JobPersistenceService();
    private final JobRuntimeService runtimeService = new JobRuntimeService(clientSyncService);
    private final JobProfileViewService profileViewService = new JobProfileViewService();
    private final Map<UUID, PlayerJobProfile> cache = new LinkedHashMap<>();

    private MinecraftServer server;
    private int autosaveCounter;

    public void onServerStarting(MinecraftServer server) {
        this.server = server;
        ConfigManager.reloadJsonConfigs();
        DebugLog.initFromConfig();
        registry.reload();
        persistenceService.initialize(server, cache);
        invalidateLeaderboardCaches();
        invalidateCatalogCache();
        autosaveCounter = 0;
    }

    public void reload() {
        ConfigManager.reloadJsonConfigs();
        DebugLog.initFromConfig();
        registry.reload();
        persistenceService.refreshEconomy(cache, "reload");
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
        return cacheService.isCatalogPayloadCached();
    }

    public int catalogPayloadSize() {
        return cacheService.catalogPayloadSize();
    }

    public int leaderboardCacheCount() {
        return cacheService.leaderboardCacheCount();
    }

    public boolean isOverallLeaderboardCached() {
        return cacheService.isOverallLeaderboardCached();
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
        int warmedLeaderboards = cacheService.leaderboardCacheCount();
        return new WarmCacheStats(
            profileCount,
            jobCount,
            cacheService.catalogPayloadSize(),
            warmedLeaderboards,
            cacheService.isOverallLeaderboardCached());
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
        boolean changed = mutationService.chooseJob(player, getOrCreateProfile(player), registry.get(jobId), jobId, secondary, persistenceService.economy(), this::saveProfile, DebugLog::log);
        if (changed) {
            syncToPlayer(player);
        }
        return changed;
    }

    public boolean leaveJob(ServerPlayer player, boolean secondary) {
        boolean changed = mutationService.leaveJob(player, getOrCreateProfile(player), secondary, this::saveProfile, DebugLog::log);
        if (changed) {
            syncToPlayer(player);
        }
        return changed;
    }

    public void resetProfile(ServerPlayer player) {
        mutationService.resetProfile(getOrCreateProfile(player), this::saveProfile);
        syncToPlayer(player);
    }

    public boolean adminSetJob(ServerPlayer player, String jobId, boolean secondary) {
        boolean changed = mutationService.adminSetJob(getOrCreateProfile(player), registry.get(jobId), jobId, secondary, this::saveProfile);
        if (changed) {
            syncToPlayer(player);
        }
        return changed;
    }

    public boolean adminSetLevel(ServerPlayer player, String jobId, int level) {
        boolean changed = mutationService.adminSetLevel(player, getOrCreateProfile(player), registry.get(jobId), jobId, level, this::saveProfile,
            titleId -> profileViewService.grantTitle(player, getOrCreateProfile(player), titleId, progressionService));
        if (changed) {
            syncToPlayer(player);
        }
        return changed;
    }

    public boolean adminAddXp(ServerPlayer player, String jobId, double amount) {
        boolean changed = mutationService.adminAddXp(player, getOrCreateProfile(player), registry.get(jobId), jobId, amount, this::saveProfile,
            titleId -> profileViewService.grantTitle(player, getOrCreateProfile(player), titleId, progressionService));
        if (changed) {
            syncToPlayer(player);
        }
        return changed;
    }

    public boolean adminAddSalary(ServerPlayer player, double amount) {
        boolean changed = mutationService.adminAddSalary(getOrCreateProfile(player), amount, this::saveProfile);
        if (changed) {
            syncToPlayer(player);
        }
        return changed;
    }

    public boolean adminAdjustSkillPoints(ServerPlayer player, int amount) {
        boolean changed = mutationService.adminAdjustSkillPoints(getOrCreateProfile(player), amount, this::saveProfile);
        if (changed) {
            syncToPlayer(player);
        }
        return changed;
    }

    public RewardDefinition handleAction(JobActionContext context) {
        PlayerJobProfile profile = getOrCreateProfile(context.player());
        return rewardService.handleAction(
            context,
            profile,
            profileViewService.assignedJobIds(profile),
            persistenceService.economy(),
            this::saveProfile,
            this::syncToPlayer,
            titleId -> profileViewService.grantTitle(context.player(), profile, titleId, progressionService)
        );
    }

    public double claimSalary(ServerPlayer player) {
        PlayerJobProfile profile = getOrCreateProfile(player);
        return salaryService.claimSalary(player, profile, profileViewService.assignedJobIds(profile), persistenceService.economy(), this::saveProfile, this::syncToPlayer);
    }

    public long salaryClaimCooldownRemaining(PlayerJobProfile profile) {
        return salaryService.salaryClaimCooldownRemaining(profile);
    }

    public long contractRerollCooldownRemaining(PlayerJobProfile profile) {
        return assignmentService.contractRerollCooldownRemaining(profile);
    }

    public ContractRerollResult previewContractReroll(ServerPlayer player, String jobId) {
        PlayerJobProfile profile = getOrCreateProfile(player);
        return assignmentService.previewContractReroll(profile, jobId, profileViewService.assignedJobIds(profile), persistenceService.economy());
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
        return cacheService.leaderboard(jobId, cache.values());
    }

    public Collection<PlayerJobProfile> overallLeaderboard() {
        return cacheService.overallLeaderboard(cache.values(), rewardService::totalEarnedAcrossJobs, rewardService::totalLevelsAcrossJobs);
    }

    public EconomyProvider economy() {
        return persistenceService.economy();
    }

    public double totalEarnedAcrossJobs(PlayerJobProfile profile) {
        return cacheService.totalEarnedAcrossJobs(profile);
    }

    public int totalLevelsAcrossJobs(PlayerJobProfile profile) {
        return cacheService.totalLevelsAcrossJobs(profile);
    }

    public double totalXpAcrossJobs(PlayerJobProfile profile) {
        return cacheService.totalXpAcrossJobs(profile);
    }

    public double effectBonus(ServerPlayer player, String effectType) {
        PlayerJobProfile profile = getOrCreateProfile(player);
        List<String> assignedJobs = profileViewService.assignedJobIds(profile);
        if (assignedJobs.isEmpty()) {
            return 0.0D;
        }
        return rewardService.effectBonus(profile, assignedJobs, effectType);
    }

    public int effectNodes(ServerPlayer player, String effectType) {
        PlayerJobProfile profile = getOrCreateProfile(player);
        List<String> assignedJobs = profileViewService.assignedJobIds(profile);
        if (assignedJobs.isEmpty()) {
            return 0;
        }
        return rewardService.effectNodes(profile, assignedJobs, effectType);
    }

    public String activeJobId(ServerPlayer player) {
        return profileViewService.activeJobId(getOrCreateProfile(player));
    }

    public boolean hasAssignedJob(ServerPlayer player, String jobId) {
        return profileViewService.hasAssignedJob(getOrCreateProfile(player), jobId);
    }

    public String firstAssignedJob(ServerPlayer player, String... jobIds) {
        return profileViewService.firstAssignedJob(getOrCreateProfile(player), jobIds);
    }

    public void flush() {
        persistenceService.flush(cache);
    }

    public void tick() {
        runtimeService.expireRewardEventIfNeeded(server, this::syncToPlayer);
        autosaveCounter++;
        if (autosaveCounter >= AUTOSAVE_TICKS) {
            autosaveCounter = 0;
            flush();
        }
    }

    public void syncToPlayer(ServerPlayer player) {
        runtimeService.syncToPlayer(player, createPlayerPayload(player, getOrCreateProfile(player)));
    }

    public void syncCatalogToPlayer(ServerPlayer player) {
        runtimeService.syncCatalogToPlayer(player, createCatalogPayload());
    }

    public void syncLeaderboardToPlayer(ServerPlayer player, String jobId) {
        runtimeService.syncLeaderboardToPlayer(player, jobId, createLeaderboardPayload(jobId));
    }

    public void syncAllOnlinePlayers() {
        invalidateCatalogCache();
        runtimeService.syncAllOnlinePlayers(server, this::syncFullStateToPlayer);
    }

    public void syncCatalogToAllPlayers() {
        invalidateCatalogCache();
        runtimeService.syncCatalogToAllPlayers(server, this::syncCatalogToPlayer);
    }

    public void syncFullStateToPlayer(ServerPlayer player) {
        syncFullState(player);
    }

    public void openScreen(ServerPlayer player) {
        openScreen(player, "jobs");
    }

    public void openScreen(ServerPlayer player, String tab) {
        openScreen(player, tab, null);
    }

    public void openScreen(ServerPlayer player, String tab, String preferredJobId) {
        runtimeService.openScreen(player, tab, preferredJobId, this::syncFullStateToPlayer);
    }

    public boolean rerollContracts(ServerPlayer player, String jobId) {
        return assignmentService.rerollContracts(player, getOrCreateProfile(player), jobId, persistenceService.economy(), this::saveProfile, this::syncToPlayer);
    }

    public enum ContractRerollResult {
        SUCCESS,
        NO_JOB,
        NOT_ASSIGNED,
        COOLDOWN,
        INSUFFICIENT_FUNDS
    }

    private void saveProfile(PlayerJobProfile profile) {
        invalidateLeaderboardCaches();
        persistenceService.saveProfile(profile, cache);
    }

    private String createCatalogPayload() {
        return cacheService.createCatalogPayload(payloadService, registry.all());
    }

    private void invalidateCatalogCache() {
        cacheService.invalidateCatalogCache();
    }

    private void invalidateLeaderboardCaches() {
        cacheService.invalidateLeaderboardCaches();
    }

    public record WarmCacheStats(
        int profileCount,
        int jobCount,
        int catalogPayloadChars,
        int warmedLeaderboards,
        boolean overallLeaderboardCached) {
    }

    private String createPlayerPayload(ServerPlayer player, PlayerJobProfile profile) {
        return payloadService.createPlayerPayload(player, profile, registry.all(), persistenceService.economy(), persistenceService.usesInternalEconomy());
    }

    private String createLeaderboardPayload(String jobId) {
        Iterable<PlayerJobProfile> entries = "all".equalsIgnoreCase(jobId) ? overallLeaderboard() : leaderboard(jobId);
        return payloadService.createLeaderboardPayload(jobId, entries, LEADERBOARD_SYNC_LIMIT,
            this::totalLevelsAcrossJobs, this::totalXpAcrossJobs, this::totalEarnedAcrossJobs);
    }

    private void syncFullState(ServerPlayer player) {
        PlayerJobProfile profile = getOrCreateProfile(player);
        String catalogPayload = createCatalogPayload();
        String playerPayload = createPlayerPayload(player, profile);
        String leaderboardJobId = profileViewService.leaderboardJobId(profile);
        String leaderboardPayload = createLeaderboardPayload(leaderboardJobId);
        runtimeService.syncFullState(player, catalogPayload, playerPayload, leaderboardJobId, leaderboardPayload);
    }

    private String leaderboardJobId(ServerPlayer player) {
        return profileViewService.leaderboardJobId(getOrCreateProfile(player));
    }
}
