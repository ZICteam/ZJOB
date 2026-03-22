package com.example.advancedjobs.event;

import com.example.advancedjobs.api.JobActionContext;
import com.example.advancedjobs.content.ModItems;
import com.example.advancedjobs.config.ConfigManager;
import com.example.advancedjobs.entity.SalaryBoardEntity;
import com.example.advancedjobs.job.JobManager;
import com.example.advancedjobs.model.JobActionType;
import com.example.advancedjobs.util.DebugLog;
import com.example.advancedjobs.util.ResourceLocationUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

public class JobEventHandler {
    public static final String JOB_NPC_TAG = "advancedjobs_npc";
    public static final String CONTRACTS_BOARD_TAG = "advancedjobs_contracts_board";
    public static final String DAILY_BOARD_TAG = "advancedjobs_daily_board";
    public static final String STATUS_BOARD_TAG = "advancedjobs_status_board";
    public static final String SALARY_BOARD_TAG = "advancedjobs_salary_board";
    public static final String SKILLS_BOARD_TAG = "advancedjobs_skills_board";
    public static final String LEADERBOARD_BOARD_TAG = "advancedjobs_leaderboard_board";
    public static final String HELP_BOARD_TAG = "advancedjobs_help_board";
    private final JobManager jobManager;
    private final RewardCalculationHandler rewards;
    private final AntiExploitHandler antiExploit = new AntiExploitHandler();
    private final ServiceDeskInteractionHandler serviceDeskInteractions;
    private final JobBlockInteractionHandler blockInteractions;
    private final JobBonusHandler bonusHandler;

    public JobEventHandler(JobManager jobManager) {
        this.jobManager = jobManager;
        this.rewards = new RewardCalculationHandler(jobManager);
        this.serviceDeskInteractions = new ServiceDeskInteractionHandler(jobManager);
        this.blockInteractions = new JobBlockInteractionHandler(jobManager, rewards, antiExploit);
        this.bonusHandler = new JobBonusHandler(jobManager);
    }

    public AntiExploitHandler antiExploit() {
        return antiExploit;
    }

    @SubscribeEvent
    public void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        if (antiExploit.shouldBlockBreakReward(player.getUUID(), event.getPos())) {
            DebugLog.log("Blocked break reward for recently placed block: player=" + player.getGameProfile().getName() + " pos=" + event.getPos());
            return;
        }
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(event.getState().getBlock());
        if (antiExploit.isOnCooldown(player.getUUID(), "break_cd|" + id)) {
            DebugLog.log("Blocked by cooldown: player=" + player.getGameProfile().getName() + " action=break target=" + id);
            return;
        }
        if (antiExploit.shouldDecay(player.getUUID(), "break|" + id, 128)) {
            DebugLog.log("Blocked by decay: player=" + player.getGameProfile().getName() + " action=break target=" + id);
            return;
        }
        JobActionType actionType = resolveBreakAction(event.getState());
        rewards.reward(new JobActionContext(player, actionType, id, player.serverLevel(), event.getPos(), false));
        bonusHandler.applyBreakBonuses(player, event.getPos(), event.getState(), actionType);
    }

    @SubscribeEvent
    public void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        BlockPos pos = event.getPos();
        antiExploit.trackPlacedBlock(player.getUUID(), pos);
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(event.getPlacedBlock().getBlock());
        if (antiExploit.isOnCooldown(player.getUUID(), "place_cd|" + id)) {
            DebugLog.log("Blocked by cooldown: player=" + player.getGameProfile().getName() + " action=place target=" + id);
            return;
        }
        JobActionType actionType = isCrop(event.getPlacedBlock()) ? JobActionType.PLANT_CROP : JobActionType.PLACE_BLOCK;
        rewards.reward(new JobActionContext(player, actionType, id, player.serverLevel(), pos, false));
        bonusHandler.applyPlaceBonuses(player, event.getPlacedBlock());
    }

    @SubscribeEvent
    public void onKill(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        LivingEntity target = event.getEntity();
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(event.getEntity().getType());
        if (id == null) {
            return;
        }
        if (antiExploit.isOnCooldown(player.getUUID(), "kill_cd|" + id)) {
            DebugLog.log("Blocked by cooldown: player=" + player.getGameProfile().getName() + " action=kill target=" + id);
            return;
        }
        int killThreshold = isBoss(id) ? Math.max(6, ConfigManager.COMMON.repeatedKillDecayThreshold.get() / 4) : ConfigManager.COMMON.repeatedKillDecayThreshold.get();
        if (antiExploit.shouldDecay(player.getUUID(), "kill|" + id, killThreshold)) {
            DebugLog.log("Blocked by decay: player=" + player.getGameProfile().getName() + " action=kill target=" + id);
            return;
        }
        boolean artificial = antiExploit.isArtificialEntity(target.getUUID()) || target.hasCustomName();
        if (ConfigManager.COMMON.blockArtificialMobRewards.get() && artificial && !isBoss(id)) {
            DebugLog.log("Blocked artificial mob reward: player=" + player.getGameProfile().getName() + " target=" + id + " uuid=" + target.getUUID());
            antiExploit.clearArtificialEntity(target.getUUID());
            return;
        }
        if (ConfigManager.COMMON.blockBabyMobRewards.get() && target instanceof Mob mob && mob.isBaby() && !isBoss(id)) {
            DebugLog.log("Blocked baby mob reward: player=" + player.getGameProfile().getName() + " target=" + id + " uuid=" + target.getUUID());
            antiExploit.clearArtificialEntity(target.getUUID());
            return;
        }
        if (ConfigManager.COMMON.blockTamedMobRewards.get() && target instanceof TamableAnimal tamable && tamable.isTame() && !isBoss(id)) {
            DebugLog.log("Blocked tamed mob reward: player=" + player.getGameProfile().getName() + " target=" + id + " uuid=" + target.getUUID());
            antiExploit.clearArtificialEntity(target.getUUID());
            return;
        }
        JobActionType actionType = isBoss(id) ? JobActionType.KILL_BOSS : JobActionType.KILL_MOB;
        rewards.reward(new JobActionContext(player, actionType, id, player.serverLevel(), target.blockPosition(), artificial));
        antiExploit.clearArtificialEntity(target.getUUID());
        bonusHandler.applyKillBonuses(player, target.blockPosition(), id);
    }

    @SubscribeEvent
    public void onCraft(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack stack = event.getCrafting();
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (antiExploit.isOnCooldown(player.getUUID(), "craft_cd|" + id)) {
            DebugLog.log("Blocked by cooldown: player=" + player.getGameProfile().getName() + " action=craft target=" + id);
            return;
        }
        rewards.reward(new JobActionContext(player, JobActionType.CRAFT_ITEM, id, player.serverLevel(), player.blockPosition(), false));
        bonusHandler.applyCraftBonuses(player, stack);
    }

    @SubscribeEvent
    public void onSmelt(PlayerEvent.ItemSmeltedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack stack = event.getSmelting();
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (antiExploit.isOnCooldown(player.getUUID(), "smelt_cd|" + id)) {
            DebugLog.log("Blocked by cooldown: player=" + player.getGameProfile().getName() + " action=smelt target=" + id);
            return;
        }
        rewards.reward(new JobActionContext(player, JobActionType.SMELT_ITEM, id, player.serverLevel(), player.blockPosition(), false));
        if (jobManager.hasAssignedJob(player, "blacksmith")
            && (jobManager.effectBonus(player, "craft_bonus") >= 0.10D || jobManager.effectBonus(player, "ingredient_save") > 0.0D)
            && player.getRandom().nextDouble() < 0.20D) {
            ItemStack extra = stack.copy();
            extra.setCount(1);
            if (!player.addItem(extra)) {
                player.drop(extra, false);
            }
        }
        if (jobManager.hasAssignedJob(player, "blacksmith") && isMetalIngot(stack)) {
            if (jobManager.effectBonus(player, "craft_aura") >= 0.10D) {
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 140, 0, true, false));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 0, true, false));
            }
            if (jobManager.effectBonus(player, "rare_gem_bonus") > 0.0D && player.getRandom().nextDouble() < 0.10D) {
                ItemStack slag = stack.is(Items.GOLD_INGOT) ? new ItemStack(Items.GOLD_NUGGET, 2) : new ItemStack(Items.IRON_NUGGET, 3);
                if (!player.addItem(slag)) {
                    player.drop(slag, false);
                }
            }
        }
        if (jobManager.hasAssignedJob(player, "cook") && isPreparedMeal(stack)) {
            if (jobManager.effectBonus(player, "craft_aura") >= 0.10D) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 0, true, false));
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0, true, false));
            }
            if (jobManager.effectBonus(player, "ingredient_save") > 0.0D && player.getRandom().nextDouble() < 0.12D) {
                ItemStack garnish = new ItemStack(Items.GOLDEN_CARROT);
                if (!player.addItem(garnish)) {
                    player.drop(garnish, false);
                }
            }
        }
    }

    @SubscribeEvent
    public void onFish(ItemFishedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (event.getDrops().isEmpty()) {
            return;
        }
        ItemStack stack = event.getDrops().get(0);
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (antiExploit.isOnCooldown(player.getUUID(), "fish_cd|" + id)) {
            DebugLog.log("Blocked by cooldown: player=" + player.getGameProfile().getName() + " action=fish target=" + id);
            return;
        }
        rewards.reward(new JobActionContext(player, JobActionType.FISH, id, player.serverLevel(), player.blockPosition(), false));
        bonusHandler.applyFishingBonuses(player, stack, event);
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            jobManager.getOrCreateProfile(player);
            jobManager.syncCatalogToPlayer(player);
            jobManager.syncToPlayer(player);
        }
    }

    @SubscribeEvent
    public void onInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getTarget() instanceof LivingEntity living)) {
            return;
        }
        if (serviceDeskInteractions.handle(event, player, living)) {
            return;
        }
        if (living instanceof Villager villager) {
            ResourceLocation id = ResourceLocationUtil.minecraft("villager");
            if (antiExploit.isOnCooldown(player.getUUID(), "trade_cd|" + id)) {
                DebugLog.log("Blocked by cooldown: player=" + player.getGameProfile().getName() + " action=trade target=" + id);
                return;
            }
            rewards.reward(new JobActionContext(player, JobActionType.TRADE_WITH_VILLAGER, id, player.serverLevel(), villager.blockPosition(), false));
            if (jobManager.hasAssignedJob(player, "merchant")
                && (jobManager.effectBonus(player, "trade_bonus") >= 0.10D || jobManager.effectBonus(player, "discount_bonus") > 0.0D)
                && player.getRandom().nextDouble() < 0.20D) {
                ItemStack emerald = new ItemStack(Items.EMERALD);
                if (!player.addItem(emerald)) {
                    player.drop(emerald, false);
                }
            }
            if (jobManager.hasAssignedJob(player, "merchant")
                && jobManager.effectBonus(player, "emerald_bonus") > 0.0D
                && player.getRandom().nextDouble() < 0.12D) {
                ItemStack emeralds = new ItemStack(Items.EMERALD, 2);
                if (!player.addItem(emeralds)) {
                    player.drop(emeralds, false);
                }
            }
            if (jobManager.hasAssignedJob(player, "merchant") && jobManager.effectBonus(player, "merchant_aura") >= 0.10D) {
                player.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 200, 0, true, false));
                player.addEffect(new MobEffectInstance(MobEffects.LUCK, 120, 0, true, false));
            }
            if (jobManager.hasAssignedJob(player, "merchant")
                && jobManager.effectBonus(player, "merchant_aura") >= 0.10D
                && player.getRandom().nextDouble() < 0.10D) {
                ItemStack ledger = player.getRandom().nextBoolean() ? new ItemStack(Items.PAPER, 3) : new ItemStack(Items.BOOK);
                if (!player.addItem(ledger)) {
                    player.drop(ledger, false);
                }
            }
        }
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        BlockState state = player.serverLevel().getBlockState(event.getPos());
        blockInteractions.handleRightClickBlock(event, player, state);
    }

    @SubscribeEvent
    public void onBreed(BabyEntitySpawnEvent event) {
        if (!(event.getCausedByPlayer() instanceof ServerPlayer player)) {
            return;
        }
        antiExploit.markArtificialEntity(event.getChild().getUUID());
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(event.getChild().getType());
        if (id == null || antiExploit.isOnCooldown(player.getUUID(), "breed_cd|" + id)) {
            if (id != null) {
                DebugLog.log("Blocked by cooldown: player=" + player.getGameProfile().getName() + " action=breed target=" + id);
            }
            return;
        }
        rewards.reward(new JobActionContext(player, JobActionType.BREED_ANIMAL, id, player.serverLevel(), event.getChild().blockPosition(), false));
        if ((jobManager.hasAssignedJob(player, "animal_breeder") || jobManager.hasAssignedJob(player, "shepherd"))
            && (jobManager.effectBonus(player, "breed_bonus") >= 0.10D || jobManager.effectBonus(player, "resource_bonus") >= 0.10D)
            && player.getRandom().nextDouble() < 0.18D) {
            ItemStack wheat = new ItemStack(Items.WHEAT);
            if (!player.addItem(wheat)) {
                player.drop(wheat, false);
            }
        }
        if (jobManager.hasAssignedJob(player, "animal_breeder")
            && (jobManager.effectBonus(player, "breed_bonus") >= 0.10D || jobManager.effectBonus(player, "pasture_aura") >= 0.10D)) {
            if (id.equals(ResourceLocationUtil.minecraft("cow")) && player.getRandom().nextDouble() < 0.16D) {
                ItemStack milkFood = new ItemStack(Items.LEATHER);
                if (!player.addItem(milkFood)) {
                    player.drop(milkFood, false);
                }
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 120, 0, true, false));
            } else if (id.equals(ResourceLocationUtil.minecraft("chicken")) && player.getRandom().nextDouble() < 0.16D) {
                ItemStack eggs = new ItemStack(Items.EGG);
                if (!player.addItem(eggs)) {
                    player.drop(eggs, false);
                }
            } else if (id.equals(ResourceLocationUtil.minecraft("pig")) && player.getRandom().nextDouble() < 0.14D) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 140, 0, true, false));
            }
            if (id.equals(ResourceLocationUtil.minecraft("chicken")) && player.getRandom().nextDouble() < 0.12D) {
                ItemStack feathers = new ItemStack(Items.FEATHER, 2);
                if (!player.addItem(feathers)) {
                    player.drop(feathers, false);
                }
            }
        }
        if (jobManager.hasAssignedJob(player, "shepherd")
            && (jobManager.effectBonus(player, "pasture_aura") >= 0.10D || jobManager.effectBonus(player, "breed_bonus") >= 0.10D)
            && id.equals(ResourceLocationUtil.minecraft("sheep"))
            && player.getRandom().nextDouble() < 0.18D) {
            ItemStack wool = new ItemStack(Items.WHITE_WOOL, 2);
            if (!player.addItem(wool)) {
                player.drop(wool, false);
            }
            if (player.getRandom().nextDouble() < 0.10D) {
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 120, 0, true, false));
            }
        }
        if (jobManager.hasAssignedJob(player, "beekeeper")
            && (jobManager.effectBonus(player, "honey_bonus") > 0.0D || jobManager.effectBonus(player, "pasture_aura") >= 0.10D)
            && player.getRandom().nextDouble() < 0.16D) {
            ItemStack beeReward = id.equals(ResourceLocationUtil.minecraft("bee"))
                ? new ItemStack(Items.HONEYCOMB, 2)
                : new ItemStack(Items.HONEY_BOTTLE);
            if (!player.addItem(beeReward)) {
                player.drop(beeReward, false);
            }
            if (id.equals(ResourceLocationUtil.minecraft("bee"))) {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 0, true, false));
            }
            if (id.equals(ResourceLocationUtil.minecraft("bee")) && player.getRandom().nextDouble() < 0.10D) {
                ItemStack flower = new ItemStack(Items.SUNFLOWER);
                if (!player.addItem(flower)) {
                    player.drop(flower, false);
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        if ((player.tickCount % 200) == 0) {
            ChunkPos chunkPos = new ChunkPos(player.blockPosition());
            long exploredChunkCooldownMs = ConfigManager.COMMON.exploredChunkRewardCooldownSeconds.get() * 1000L;
            if (antiExploit.markChunkExplored(player.getUUID(), chunkPos, exploredChunkCooldownMs)) {
                JobActionContext context = new JobActionContext(player, JobActionType.EXPLORE_CHUNK, ResourceLocationUtil.minecraft("chunk"), player.serverLevel(), player.blockPosition(), false);
                rewards.reward(context);
                if (jobManager.hasAssignedJob(player, "explorer") && jobManager.effectBonus(player, "explore_bonus") >= 0.10D && player.getRandom().nextDouble() < 0.10D) {
                    ItemStack map = new ItemStack(Items.MAP);
                    if (!player.addItem(map)) {
                        player.drop(map, false);
                    }
                }
                if (jobManager.hasAssignedJob(player, "explorer")
                    && jobManager.effectBonus(player, "explore_aura") >= 0.10D
                    && player.getRandom().nextDouble() < 0.08D) {
                    player.addEffect(new MobEffectInstance(MobEffects.JUMP, 140, 0, true, false));
                }
                if ((jobManager.hasAssignedJob(player, "explorer") || jobManager.hasAssignedJob(player, "treasure_hunter"))
                    && jobManager.effectBonus(player, "cache_finder") > 0.0D
                    && player.getRandom().nextDouble() < 0.08D) {
                    ItemStack reward = player.getRandom().nextBoolean() ? new ItemStack(Items.COMPASS) : new ItemStack(Items.SPYGLASS);
                    if (!player.addItem(reward)) {
                        player.drop(reward, false);
                    }
                }
                if (jobManager.hasAssignedJob(player, "archaeologist")
                    && jobManager.effectBonus(player, "artifact_bonus") > 0.0D
                    && player.getRandom().nextDouble() < 0.06D) {
                    ItemStack brush = new ItemStack(Items.BRUSH);
                    if (!player.addItem(brush)) {
                        player.drop(brush, false);
                    }
                }
                if (jobManager.hasAssignedJob(player, "archaeologist")
                    && jobManager.effectBonus(player, "explore_aura") >= 0.10D
                    && player.getRandom().nextDouble() < 0.06D) {
                    player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 160, 0, true, false));
                }
            }
        }
        if ((player.tickCount % 40) == 0) {
            bonusHandler.applyPassiveEffects(player);
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            antiExploit.cleanupIfDue();
            jobManager.tick();
        }
    }

    @SubscribeEvent
    public void onMobFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (event.getEntity() == null) {
            return;
        }
        MobSpawnType spawnType = event.getSpawnType();
        if (spawnType == MobSpawnType.SPAWNER
            || spawnType == MobSpawnType.SPAWN_EGG
            || spawnType == MobSpawnType.COMMAND
            || spawnType == MobSpawnType.DISPENSER
            || spawnType == MobSpawnType.BUCKET
            || spawnType == MobSpawnType.REINFORCEMENT) {
            antiExploit.markArtificialEntity(event.getEntity().getUUID());
        }
    }

    private JobActionType resolveBreakAction(BlockState state) {
        if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
            return JobActionType.HARVEST_CROP;
        }
        if (state.is(Blocks.NETHER_WART) && state.getValue(net.minecraft.world.level.block.NetherWartBlock.AGE) >= 3) {
            return JobActionType.HARVEST_CROP;
        }
        if (state.is(Blocks.SWEET_BERRY_BUSH) && state.getValue(net.minecraft.world.level.block.SweetBerryBushBlock.AGE) >= 2) {
            return JobActionType.HARVEST_CROP;
        }
        return JobActionType.BREAK_BLOCK;
    }

    private boolean isCrop(BlockState state) {
        return state.getBlock() instanceof CropBlock
            || state.is(Blocks.NETHER_WART)
            || state.is(Blocks.SWEET_BERRY_BUSH)
            || state.getBlock() instanceof BushBlock;
    }

    private boolean isMetalIngot(ItemStack stack) {
        return stack.is(Items.IRON_INGOT) || stack.is(Items.GOLD_INGOT) || stack.is(Items.COPPER_INGOT) || stack.is(Items.NETHERITE_SCRAP);
    }

    private boolean isPreparedMeal(ItemStack stack) {
        return stack.is(Items.COOKED_BEEF)
            || stack.is(Items.COOKED_PORKCHOP)
            || stack.is(Items.COOKED_CHICKEN)
            || stack.is(Items.COOKED_MUTTON)
            || stack.is(Items.COOKED_RABBIT)
            || stack.is(Items.BREAD)
            || stack.is(Items.CAKE)
            || stack.is(Items.PUMPKIN_PIE)
            || stack.is(Items.COOKIE);
    }

    private boolean isBoss(ResourceLocation id) {
        return id.equals(ResourceLocationUtil.minecraft("wither")) || id.equals(ResourceLocationUtil.minecraft("ender_dragon"));
    }
}
