package com.example.advancedjobs.api;

import com.example.advancedjobs.model.JobActionType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

public record JobActionContext(
    ServerPlayer player,
    JobActionType actionType,
    ResourceLocation targetId,
    ServerLevel level,
    BlockPos pos,
    boolean artificialSource
) {
    public ResourceKey<Level> dimensionKey() {
        return level.dimension();
    }

    public ResourceKey<Biome> biomeKey() {
        return level.getBiome(pos).unwrapKey().orElseThrow();
    }
}
