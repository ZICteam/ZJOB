package com.example.advancedjobs.content;

import com.example.advancedjobs.AdvancedJobsMod;
import com.example.advancedjobs.entity.ContractsBoardEntity;
import com.example.advancedjobs.entity.DailyBoardEntity;
import com.example.advancedjobs.entity.HelpBoardEntity;
import com.example.advancedjobs.entity.JobsMasterEntity;
import com.example.advancedjobs.entity.LeaderboardBoardEntity;
import com.example.advancedjobs.entity.SalaryBoardEntity;
import com.example.advancedjobs.entity.SkillsBoardEntity;
import com.example.advancedjobs.entity.StatusBoardEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, AdvancedJobsMod.MOD_ID);

    public static final RegistryObject<EntityType<JobsMasterEntity>> JOBS_MASTER = ENTITY_TYPES.register("jobs_master",
        () -> EntityType.Builder.of(JobsMasterEntity::new, MobCategory.CREATURE)
            .sized(0.6F, 1.95F)
            .clientTrackingRange(8)
            .build("jobs_master"));
    public static final RegistryObject<EntityType<ContractsBoardEntity>> CONTRACTS_BOARD = ENTITY_TYPES.register("contracts_board",
        () -> EntityType.Builder.of(ContractsBoardEntity::new, MobCategory.CREATURE)
            .sized(0.6F, 1.95F)
            .clientTrackingRange(8)
            .build("contracts_board"));
    public static final RegistryObject<EntityType<DailyBoardEntity>> DAILY_BOARD = ENTITY_TYPES.register("daily_board",
        () -> EntityType.Builder.of(DailyBoardEntity::new, MobCategory.CREATURE)
            .sized(0.6F, 1.95F)
            .clientTrackingRange(8)
            .build("daily_board"));
    public static final RegistryObject<EntityType<StatusBoardEntity>> STATUS_BOARD = ENTITY_TYPES.register("status_board",
        () -> EntityType.Builder.of(StatusBoardEntity::new, MobCategory.CREATURE)
            .sized(0.6F, 1.95F)
            .clientTrackingRange(8)
            .build("status_board"));
    public static final RegistryObject<EntityType<SalaryBoardEntity>> SALARY_BOARD = ENTITY_TYPES.register("salary_board",
        () -> EntityType.Builder.of(SalaryBoardEntity::new, MobCategory.CREATURE)
            .sized(0.6F, 1.95F)
            .clientTrackingRange(8)
            .build("salary_board"));
    public static final RegistryObject<EntityType<SkillsBoardEntity>> SKILLS_BOARD = ENTITY_TYPES.register("skills_board",
        () -> EntityType.Builder.of(SkillsBoardEntity::new, MobCategory.CREATURE)
            .sized(0.6F, 1.95F)
            .clientTrackingRange(8)
            .build("skills_board"));
    public static final RegistryObject<EntityType<LeaderboardBoardEntity>> LEADERBOARD_BOARD = ENTITY_TYPES.register("leaderboard_board",
        () -> EntityType.Builder.of(LeaderboardBoardEntity::new, MobCategory.CREATURE)
            .sized(0.6F, 1.95F)
            .clientTrackingRange(8)
            .build("leaderboard_board"));
    public static final RegistryObject<EntityType<HelpBoardEntity>> HELP_BOARD = ENTITY_TYPES.register("help_board",
        () -> EntityType.Builder.of(HelpBoardEntity::new, MobCategory.CREATURE)
            .sized(0.6F, 1.95F)
            .clientTrackingRange(8)
            .build("help_board"));

    private ModEntities() {
    }

    public static void register(IEventBus bus) {
        ENTITY_TYPES.register(bus);
        bus.addListener(ModEntities::onAttributes);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(JOBS_MASTER.get(), net.minecraft.world.entity.npc.Villager.createAttributes().build());
        event.put(CONTRACTS_BOARD.get(), net.minecraft.world.entity.npc.Villager.createAttributes().build());
        event.put(DAILY_BOARD.get(), net.minecraft.world.entity.npc.Villager.createAttributes().build());
        event.put(STATUS_BOARD.get(), net.minecraft.world.entity.npc.Villager.createAttributes().build());
        event.put(SALARY_BOARD.get(), net.minecraft.world.entity.npc.Villager.createAttributes().build());
        event.put(SKILLS_BOARD.get(), net.minecraft.world.entity.npc.Villager.createAttributes().build());
        event.put(LEADERBOARD_BOARD.get(), net.minecraft.world.entity.npc.Villager.createAttributes().build());
        event.put(HELP_BOARD.get(), net.minecraft.world.entity.npc.Villager.createAttributes().build());
    }
}
