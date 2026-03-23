package com.example.advancedjobs.job;

import com.example.advancedjobs.AdvancedJobsMod;
import com.example.advancedjobs.api.EconomyProvider;
import com.example.advancedjobs.config.ConfigManager;
import com.example.advancedjobs.data.JsonPlayerDataRepository;
import com.example.advancedjobs.data.PlayerDataRepository;
import com.example.advancedjobs.data.SqlitePlayerDataRepository;
import com.example.advancedjobs.economy.ExternalEconomyBridge;
import com.example.advancedjobs.economy.InternalEconomyProvider;
import com.example.advancedjobs.model.PlayerJobProfile;
import com.example.advancedjobs.util.DebugLog;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;

class JobPersistenceService {
    private final InternalEconomyProvider internalEconomyProvider = new InternalEconomyProvider();
    private final ExternalEconomyBridge externalEconomyBridge = new ExternalEconomyBridge();

    private PlayerDataRepository repository;
    private EconomyProvider economyProvider = internalEconomyProvider;

    void initialize(MinecraftServer server, Map<UUID, PlayerJobProfile> cache) {
        repository = createRepository();
        repository.init(server);
        cache.clear();
        for (PlayerJobProfile profile : repository.all()) {
            cache.put(profile.playerId(), profile);
        }
        refreshEconomy(cache, "server_start");
    }

    void refreshEconomy(Map<UUID, PlayerJobProfile> cache, String phase) {
        economyProvider = chooseEconomyProvider();
        if (economyProvider == internalEconomyProvider) {
            restoreInternalBalances(cache);
        }
        logEconomyStartupState(phase);
    }

    EconomyProvider economy() {
        return economyProvider;
    }

    boolean usesInternalEconomy() {
        return economyProvider == internalEconomyProvider;
    }

    void saveProfile(PlayerJobProfile profile, Map<UUID, PlayerJobProfile> cache) {
        persistInternalBalances(cache);
        if (repository != null) {
            repository.save(profile);
            profile.clearDirty();
        }
    }

    void flush(Map<UUID, PlayerJobProfile> cache) {
        if (repository == null) {
            return;
        }
        persistInternalBalances(cache);
        if (cache.values().stream().anyMatch(PlayerJobProfile::dirty)) {
            repository.saveAll(cache.values());
            cache.values().forEach(PlayerJobProfile::clearDirty);
            DebugLog.log("Flush completed for " + cache.size() + " profiles");
        }
    }

    private PlayerDataRepository createRepository() {
        return "sqlite".equalsIgnoreCase(ConfigManager.COMMON.storageMode.get()) ? new SqlitePlayerDataRepository() : new JsonPlayerDataRepository();
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

    private void restoreInternalBalances(Map<UUID, PlayerJobProfile> cache) {
        Map<UUID, Double> balances = new LinkedHashMap<>();
        for (PlayerJobProfile profile : cache.values()) {
            balances.put(profile.playerId(), profile.internalBalance());
        }
        internalEconomyProvider.load(balances);
    }

    private void persistInternalBalances(Map<UUID, PlayerJobProfile> cache) {
        if (economyProvider != internalEconomyProvider) {
            return;
        }
        Map<UUID, Double> snapshot = internalEconomyProvider.snapshot();
        for (Map.Entry<UUID, Double> entry : snapshot.entrySet()) {
            PlayerJobProfile profile = cache.get(entry.getKey());
            if (profile != null && Double.compare(profile.internalBalance(), entry.getValue()) != 0) {
                profile.setInternalBalance(entry.getValue());
            }
        }
    }
}
