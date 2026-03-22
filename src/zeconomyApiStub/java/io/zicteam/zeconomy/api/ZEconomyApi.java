package io.zicteam.zeconomy.api;

import io.zicteam.zeconomy.api.model.ApiResult;
import io.zicteam.zeconomy.api.model.PlayerEconomySnapshot;
import java.util.UUID;

public interface ZEconomyApi {
    ApiResult<PlayerEconomySnapshot> getPlayerSnapshot(UUID playerId);

    ApiResult<Double> addBalance(UUID playerId, String currencyId, double amount);
}
