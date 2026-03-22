package com.example.advancedjobs.economy;

import com.example.advancedjobs.AdvancedJobsMod;
import com.example.advancedjobs.api.EconomyProvider;
import com.example.advancedjobs.config.ConfigManager;
import io.zicteam.zeconomy.api.ZEconomyApi;
import io.zicteam.zeconomy.api.ZEconomyApiProvider;
import io.zicteam.zeconomy.api.model.ApiResult;
import io.zicteam.zeconomy.api.model.PlayerEconomySnapshot;
import java.util.UUID;
import net.minecraftforge.fml.ModList;

public class ExternalEconomyBridge implements EconomyProvider {
    private static final String ZE_MOD_ID = "zeconomy";

    @Override
    public String id() {
        return "external";
    }

    public String currencyDebugId() {
        return currencyId();
    }

    @Override
    public boolean isAvailable() {
        return ModList.get().isLoaded(ZE_MOD_ID);
    }

    @Override
    public double getBalance(UUID playerId) {
        if (!isAvailable() || playerId == null) {
            return 0.0D;
        }
        try {
            ApiResult<PlayerEconomySnapshot> result = api().getPlayerSnapshot(playerId);
            if (!result.success() || result.value() == null) {
                return 0.0D;
            }
            return Math.max(0.0D, result.value().walletBalances().getOrDefault(currencyId(), 0.0D));
        } catch (Exception e) {
            logFailure("read balance", e);
            return 0.0D;
        }
    }

    @Override
    public boolean deposit(UUID playerId, double amount, String reason) {
        return changeBalance(playerId, Math.max(0.0D, amount), reason);
    }

    @Override
    public boolean withdraw(UUID playerId, double amount, String reason) {
        return changeBalance(playerId, -Math.max(0.0D, amount), reason);
    }

    private boolean changeBalance(UUID playerId, double delta, String reason) {
        if (!isAvailable() || playerId == null || delta == 0.0D) {
            return false;
        }
        try {
            ApiResult<Double> result = api().addBalance(playerId, currencyId(), delta);
            if (result.success()) {
                return true;
            }
            AdvancedJobsMod.LOGGER.warn(
                "ZEconomy bridge rejected balance update for {} currency={} delta={} reason={} code={} message={}",
                playerId,
                currencyId(),
                delta,
                reason,
                result.errorCode(),
                result.message()
            );
        } catch (Exception e) {
            logFailure("change balance", e);
        }
        return false;
    }

    private ZEconomyApi api() {
        return ZEconomyApiProvider.get();
    }

    private String currencyId() {
        String configured = ConfigManager.economy().externalCurrencyId();
        return configured == null || configured.isBlank() ? "z_coin" : configured;
    }

    private void logFailure(String action, Exception e) {
        AdvancedJobsMod.LOGGER.warn("Failed to {} via ZEconomy bridge", action, e);
    }
}
