package com.example.advancedjobs.client;

import com.example.advancedjobs.AdvancedJobsMod;
import com.example.advancedjobs.entity.NpcRole;
import com.example.advancedjobs.util.ResourceLocationUtil;
import com.google.gson.JsonObject;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public final class NpcSkinManager {
    private static final Map<NpcRole, ResourceLocation> LOCAL_TEXTURES = new EnumMap<>(NpcRole.class);
    private static final Map<NpcRole, ResourceLocation> ONLINE_TEXTURES = new EnumMap<>(NpcRole.class);

    private NpcSkinManager() {
    }

    public static void reload(JsonObject catalogRoot) {
        clearLocalTextures();
        ONLINE_TEXTURES.clear();
        if (catalogRoot == null || !catalogRoot.has("npcSkins")) {
            return;
        }
        JsonObject skins = catalogRoot.getAsJsonObject("npcSkins");
        for (NpcRole role : NpcRole.values()) {
            if (!skins.has(role.id())) {
                continue;
            }
            JsonObject profile = skins.getAsJsonObject(role.id());
            String type = profile.has("type") ? profile.get("type").getAsString() : "online";
            if ("local".equalsIgnoreCase(type) && profile.has("imageBase64")) {
                registerLocalTexture(role, profile.get("imageBase64").getAsString());
            } else if ("online".equalsIgnoreCase(type) && profile.has("value")) {
                registerOnlineTexture(role, profile.get("value").getAsString());
            }
        }
    }

    public static ResourceLocation texture(NpcRole role) {
        if (LOCAL_TEXTURES.containsKey(role)) {
            return LOCAL_TEXTURES.get(role);
        }
        if (ONLINE_TEXTURES.containsKey(role)) {
            return ONLINE_TEXTURES.get(role);
        }
        return DefaultPlayerSkin.getDefaultSkin(UUID.nameUUIDFromBytes(("advancedjobs:" + role.id()).getBytes()));
    }

    private static void registerLocalTexture(NpcRole role, String imageBase64) {
        try {
            byte[] bytes = Base64.getDecoder().decode(imageBase64);
            var image = com.mojang.blaze3d.platform.NativeImage.read(new ByteArrayInputStream(bytes));
            ResourceLocation id = ResourceLocationUtil.mod("npc_skin/" + role.id());
            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(image));
            LOCAL_TEXTURES.put(role, id);
        } catch (Exception e) {
            AdvancedJobsMod.LOGGER.warn("Failed to register local NPC skin for {}", role.id(), e);
        }
    }

    private static void registerOnlineTexture(NpcRole role, String nickname) {
        try {
            ResourceLocation fallback = DefaultPlayerSkin.getDefaultSkin(UUID.nameUUIDFromBytes(nickname.getBytes()));
            ONLINE_TEXTURES.put(role, fallback);
            ResourceLocation skin = AbstractClientPlayer.getSkinLocation(nickname);
            AbstractClientPlayer.registerSkinTexture(skin, nickname);
            ONLINE_TEXTURES.put(role, skin);
        } catch (Exception e) {
            AdvancedJobsMod.LOGGER.warn("Failed to register online NPC skin for {}", nickname, e);
        }
    }

    private static void clearLocalTextures() {
        LOCAL_TEXTURES.clear();
    }
}
