package com.example.advancedjobs.entity;

import com.example.advancedjobs.config.ConfigManager;
import net.minecraft.network.chat.Component;

public interface RoleBasedNpc {
    NpcRole npcRole();

    default Component roleLabel() {
        return Component.literal(ConfigManager.npcLabels().label(npcRole().id()));
    }
}
