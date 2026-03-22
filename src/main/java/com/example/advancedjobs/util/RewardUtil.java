package com.example.advancedjobs.util;

import com.example.advancedjobs.api.JobActionContext;
import com.example.advancedjobs.model.RewardDefinition;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

public final class RewardUtil {
    private RewardUtil() {
    }

    public static RewardDefinition applyMultipliers(
        RewardDefinition reward,
        JobActionContext context,
        double professionMultiplier,
        Map<String, Double> worldMultipliers,
        Map<String, Double> biomeMultipliers,
        double vipMultiplier,
        double eventMultiplier
    ) {
        ResourceKey<Level> levelKey = context.dimensionKey();
        ResourceKey<Biome> biomeKey = context.biomeKey();
        double worldFactor = worldMultipliers.getOrDefault(levelKey.location().toString(), 1.0D);
        double biomeFactor = biomeMultipliers.getOrDefault(biomeKey.location().toString(), 1.0D);
        return reward.scaled(professionMultiplier * worldFactor * biomeFactor * vipMultiplier * eventMultiplier);
    }
}
