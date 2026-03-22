package com.example.advancedjobs.integration.jei;

import com.example.advancedjobs.AdvancedJobsMod;
import com.example.advancedjobs.content.ModItems;
import com.example.advancedjobs.util.ResourceLocationUtil;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@JeiPlugin
public final class AdvancedJobsJeiPlugin implements IModPlugin {
    private static final ResourceLocation PLUGIN_UID = ResourceLocationUtil.mod("jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addItemStackInfo(new ItemStack(ModItems.JOB_NPC_WAND.get()),
            Component.translatable("jei.advancedjobs.job_npc_wand.1"),
            Component.translatable("jei.advancedjobs.job_npc_wand.2"),
            Component.translatable("jei.advancedjobs.job_npc_wand.3"));
        registration.addItemStackInfo(new ItemStack(ModItems.JOBS_MASTER_SPAWN_EGG.get()),
            Component.translatable("jei.advancedjobs.jobs_master"));
        registration.addItemStackInfo(new ItemStack(ModItems.DAILY_BOARD_SPAWN_EGG.get()),
            Component.translatable("jei.advancedjobs.daily_board"));
        registration.addItemStackInfo(new ItemStack(ModItems.STATUS_BOARD_SPAWN_EGG.get()),
            Component.translatable("jei.advancedjobs.status_board"));
        registration.addItemStackInfo(new ItemStack(ModItems.CONTRACTS_BOARD_SPAWN_EGG.get()),
            Component.translatable("jei.advancedjobs.contracts_board"));
        registration.addItemStackInfo(new ItemStack(ModItems.SALARY_BOARD_SPAWN_EGG.get()),
            Component.translatable("jei.advancedjobs.salary_board"));
        registration.addItemStackInfo(new ItemStack(ModItems.SKILLS_BOARD_SPAWN_EGG.get()),
            Component.translatable("jei.advancedjobs.skills_board"));
        registration.addItemStackInfo(new ItemStack(ModItems.LEADERBOARD_BOARD_SPAWN_EGG.get()),
            Component.translatable("jei.advancedjobs.leaderboard_board"));
        registration.addItemStackInfo(new ItemStack(ModItems.HELP_BOARD_SPAWN_EGG.get()),
            Component.translatable("jei.advancedjobs.help_board"));
    }
}
