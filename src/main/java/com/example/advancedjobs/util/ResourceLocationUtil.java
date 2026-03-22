package com.example.advancedjobs.util;

import com.example.advancedjobs.AdvancedJobsMod;
import net.minecraft.resources.ResourceLocation;

public final class ResourceLocationUtil {
    private ResourceLocationUtil() {
    }

    public static ResourceLocation parse(String id) {
        return ResourceLocation.parse(id);
    }

    public static ResourceLocation of(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    public static ResourceLocation minecraft(String path) {
        return of("minecraft", path);
    }

    public static ResourceLocation mod(String path) {
        return of(AdvancedJobsMod.MOD_ID, path);
    }
}
