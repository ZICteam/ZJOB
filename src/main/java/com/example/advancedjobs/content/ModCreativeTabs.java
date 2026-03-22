package com.example.advancedjobs.content;

import com.example.advancedjobs.AdvancedJobsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    private static final ResourceLocation CREATIVE_TAB_REGISTRY = new ResourceLocation("minecraft", "creative_mode_tab");
    private static final ResourceLocation MAIN_ID = new ResourceLocation(AdvancedJobsMod.MOD_ID, "main");

    private ModCreativeTabs() {
    }

    private static boolean isCreativeTabRegistry(Object key) {
        return String.valueOf(key).contains("minecraft:creative_mode_tab");
    }

    private static CreativeModeTab createMainTab() {
        return CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.advancedjobs.main"))
            .icon(() -> new ItemStack(ModItems.JOB_NPC_WAND.get()))
            .displayItems((parameters, output) -> ModItems.ITEMS.getEntries().stream()
                .map(RegistryObject::get)
                .forEach(output::accept))
            .build();
    }

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        if (!isCreativeTabRegistry(event.getRegistryKey())) {
            return;
        }
        event.register((net.minecraft.resources.ResourceKey<? extends net.minecraft.core.Registry<CreativeModeTab>>) event.getRegistryKey(),
            helper -> helper.register(MAIN_ID, createMainTab()));
    }

    public static void register(IEventBus bus) {
        bus.register(ModCreativeTabs.class);
    }
}
