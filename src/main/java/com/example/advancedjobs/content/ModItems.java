package com.example.advancedjobs.content;

import com.example.advancedjobs.AdvancedJobsMod;
import com.example.advancedjobs.content.ModEntities;
import com.example.advancedjobs.item.JobNpcWandItem;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, AdvancedJobsMod.MOD_ID);

    public static final RegistryObject<Item> JOB_NPC_WAND = ITEMS.register("job_npc_wand",
        () -> new JobNpcWandItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> JOBS_MASTER_SPAWN_EGG = ITEMS.register("jobs_master_spawn_egg",
        () -> new ForgeSpawnEggItem(jobsMasterType(), 0x9b7b55, 0xe3cf99, new Item.Properties()));
    public static final RegistryObject<Item> CONTRACTS_BOARD_SPAWN_EGG = ITEMS.register("contracts_board_spawn_egg",
        () -> new ForgeSpawnEggItem(contractsBoardType(), 0x4d6d7a, 0xd7caa0, new Item.Properties()));
    public static final RegistryObject<Item> DAILY_BOARD_SPAWN_EGG = ITEMS.register("daily_board_spawn_egg",
        () -> new ForgeSpawnEggItem(dailyBoardType(), 0x6d8a57, 0xe8dfac, new Item.Properties()));
    public static final RegistryObject<Item> STATUS_BOARD_SPAWN_EGG = ITEMS.register("status_board_spawn_egg",
        () -> new ForgeSpawnEggItem(statusBoardType(), 0x63718a, 0xe5e9f5, new Item.Properties()));
    public static final RegistryObject<Item> SALARY_BOARD_SPAWN_EGG = ITEMS.register("salary_board_spawn_egg",
        () -> new ForgeSpawnEggItem(salaryBoardType(), 0x8a6f4c, 0xf0d49b, new Item.Properties()));
    public static final RegistryObject<Item> SKILLS_BOARD_SPAWN_EGG = ITEMS.register("skills_board_spawn_egg",
        () -> new ForgeSpawnEggItem(skillsBoardType(), 0x4f7a57, 0xb8e3c0, new Item.Properties()));
    public static final RegistryObject<Item> LEADERBOARD_BOARD_SPAWN_EGG = ITEMS.register("leaderboard_board_spawn_egg",
        () -> new ForgeSpawnEggItem(leaderboardBoardType(), 0x8a5c3a, 0xf3d8a5, new Item.Properties()));
    public static final RegistryObject<Item> HELP_BOARD_SPAWN_EGG = ITEMS.register("help_board_spawn_egg",
        () -> new ForgeSpawnEggItem(helpBoardType(), 0x5a7d8c, 0xd7eef8, new Item.Properties()));

    private ModItems() {
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }

    private static Supplier<? extends EntityType<? extends Mob>> jobsMasterType() {
        return () -> ModEntities.JOBS_MASTER.get();
    }

    private static Supplier<? extends EntityType<? extends Mob>> contractsBoardType() {
        return () -> ModEntities.CONTRACTS_BOARD.get();
    }

    private static Supplier<? extends EntityType<? extends Mob>> dailyBoardType() {
        return () -> ModEntities.DAILY_BOARD.get();
    }

    private static Supplier<? extends EntityType<? extends Mob>> statusBoardType() {
        return () -> ModEntities.STATUS_BOARD.get();
    }

    private static Supplier<? extends EntityType<? extends Mob>> salaryBoardType() {
        return () -> ModEntities.SALARY_BOARD.get();
    }

    private static Supplier<? extends EntityType<? extends Mob>> skillsBoardType() {
        return () -> ModEntities.SKILLS_BOARD.get();
    }

    private static Supplier<? extends EntityType<? extends Mob>> leaderboardBoardType() {
        return () -> ModEntities.LEADERBOARD_BOARD.get();
    }

    private static Supplier<? extends EntityType<? extends Mob>> helpBoardType() {
        return () -> ModEntities.HELP_BOARD.get();
    }
}
