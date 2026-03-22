package com.example.advancedjobs.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ClientConfig {
    public final ForgeConfigSpec.BooleanValue showActionToasts;
    public final ForgeConfigSpec.BooleanValue compactGui;

    public ClientConfig(ForgeConfigSpec.Builder builder) {
        builder.push("client");
        showActionToasts = builder.define("showActionToasts", true);
        compactGui = builder.define("compactGui", false);
        builder.pop();
    }
}
