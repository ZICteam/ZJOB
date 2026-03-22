package com.example.advancedjobs.client;

import com.example.advancedjobs.entity.NpcRole;
import com.example.advancedjobs.entity.RoleBasedNpc;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

public class PlayerNpcRenderer<T extends Mob & RoleBasedNpc> extends MobRenderer<T, PlayerModel<T>> {
    public PlayerNpcRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return NpcSkinManager.texture(entity.npcRole());
    }

    @Override
    protected void scale(T entity, PoseStack poseStack, float partialTickTime) {
        poseStack.scale(0.9375F, 0.9375F, 0.9375F);
    }
}
