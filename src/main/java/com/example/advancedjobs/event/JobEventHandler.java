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

    public JobEventHandler(JobManager jobManager) {
        this.jobManager = jobManager;
        this.rewards = new RewardCalculationHandler(jobManager);
        this.serviceDeskInteractions = new ServiceDeskInteractionHandler(jobManager);
        this.blockInteractions = new JobBlockInteractionHandler(jobManager, rewards, antiExploit);
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
        applyBreakBonuses(player, event.getPos(), event.getState());
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
        applyPlaceBonuses(player, pos, event.getPlacedBlock());
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
        applyKillBonuses(player, target.blockPosition(), id);
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
        applyCraftBonuses(player, stack);
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
        applyFishingBonuses(player, stack, event);
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
            applyPassiveEffects(player);
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

    private void applyBreakBonuses(ServerPlayer player, BlockPos pos, BlockState state) {
        String jobId = jobManager.firstAssignedJob(player,
            "miner", "deep_miner", "lumberjack", "forester", "farmer", "harvester", "herbalist",
            "explorer", "treasure_hunter", "archaeologist", "sand_collector", "ice_harvester", "quarry_worker", "digger",
            "engineer", "redstone_technician", "blacksmith", "cook", "armorer");
        if (jobId == null) {
            return;
        }
        double resourceBonus = switch (jobId) {
            case "miner", "deep_miner" -> jobManager.effectBonus(player, "ore_bonus");
            case "lumberjack", "forester" -> jobManager.effectBonus(player, "wood_bonus");
            case "farmer", "harvester", "herbalist" -> jobManager.effectBonus(player, "crop_bonus");
            case "explorer", "treasure_hunter", "archaeologist", "sand_collector", "ice_harvester", "quarry_worker", "digger" ->
                jobManager.effectBonus(player, "explore_bonus");
            case "engineer", "redstone_technician", "blacksmith", "cook", "armorer" -> jobManager.effectBonus(player, "craft_bonus");
            default -> jobManager.effectBonus(player, "resource_bonus");
        };
        if (resourceBonus > 0.0D && player.getRandom().nextDouble() < Math.min(0.35D, resourceBonus)) {
            ItemStack extraDrop = new ItemStack(state.getBlock().asItem());
            if (!extraDrop.isEmpty()) {
                net.minecraft.world.level.block.Block.popResource(player.serverLevel(), pos, extraDrop);
            }
        }
        if (("miner".equals(jobId) || "deep_miner".equals(jobId)) && (jobManager.effectNodes(player, "ore_bonus") >= 3 || jobManager.effectNodes(player, "resource_bonus") >= 3)) {
            ItemStack smelted = smeltResult(state);
            if (!smelted.isEmpty()) {
                net.minecraft.world.level.block.Block.popResource(player.serverLevel(), pos, smelted.copy());
            }
        } else if ("forester".equals(jobId) && isLog(state) && (jobManager.effectBonus(player, "sapling_return") > 0.0D || resourceBonus >= 0.10D) && player.getRandom().nextDouble() < 0.18D) {
            ItemStack sapling = saplingResult(state);
            if (!sapling.isEmpty()) {
                net.minecraft.world.level.block.Block.popResource(player.serverLevel(), pos, sapling);
            }
        } else if (("farmer".equals(jobId) || "harvester".equals(jobId)) && resolveBreakAction(state) == JobActionType.HARVEST_CROP && (jobManager.effectBonus(player, "seed_keep") > 0.0D || resourceBonus >= 0.10D) && player.getRandom().nextDouble() < 0.25D) {
            ItemStack seeds = seedResult(state);
            if (!seeds.isEmpty()) {
                net.minecraft.world.level.block.Block.popResource(player.serverLevel(), pos, seeds);
            }
        } else if ("forester".equals(jobId)
            && isLog(state)
            && (jobManager.effectBonus(player, "forest_aura") >= 0.10D || isForestBiome(player, pos))
            && player.getRandom().nextDouble() < 0.16D) {
            ItemStack groveDrop = state.is(Blocks.CHERRY_LOG) ? new ItemStack(Items.APPLE) : new ItemStack(Items.STICK, 2);
            net.minecraft.world.level.block.Block.popResource(player.serverLevel(), pos, groveDrop);
        } else if ("harvester".equals(jobId)
            && isBulkHarvestBlock(state)
            && (jobManager.effectBonus(player, "farm_aura") >= 0.10D || jobManager.effectBonus(player, "crop_bonus") >= 0.12D)
            && player.getRandom().nextDouble() < 0.20D) {
            ItemStack bulkHarvest = bulkHarvestResult(state);
            if (!bulkHarvest.isEmpty()) {
                net.minecraft.world.level.block.Block.popResource(player.serverLevel(), pos, bulkHarvest);
            }
        } else if ("sand_collector".equals(jobId) && (state.is(Blocks.SAND) || state.is(Blocks.RED_SAND)) && resourceBonus >= 0.10D) {
            net.minecraft.world.level.block.Block.popResource(player.serverLevel(), pos, new ItemStack(state.is(Blocks.RED_SAND) ? Items.RED_SANDSTONE : Items.SANDSTONE));
        } else if ("ice_harvester".equals(jobId) && (state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE)) && resourceBonus >= 0.10D) {
            net.minecraft.world.level.block.Block.popResource(player.serverLevel(), pos, new ItemStack(Items.SNOWBALL, 2));
        } else if ("quarry_worker".equals(jobId) && isStoneFamily(state) && resourceBonus >= 0.10D) {
            net.minecraft.world.level.block.Block.popResource(player.serverLevel(), pos, new ItemStack(Items.COBBLESTONE));
        } else if ("digger".equals(jobId) && isLooseTerrain(state) && resourceBonus >= 0.10D) {
            net.minecraft.world.level.block.Block.popResource(player.serverLevel(), pos, new ItemStack(Items.FLINT));
        } else if ("quarry_worker".equals(jobId)
            && state.is(Blocks.DEEPSLATE)
            && (jobManager.effectBonus(player, "artifact_bonus") > 0.0D || resourceBonus >= 0.12D)
            && player.getRandom().nextDouble() < 0.12D) {
            net.minecraft.world.level.block.Block.popResource(player.serverLevel(), pos, new ItemStack(Items.COAL, 2));
        } else if ("digger".equals(jobId)
            && (state.is(Blocks.CLAY) || state.is(Blocks.MUD))
            && (jobManager.effectBonus(player, "cache_finder") > 0.0D || resourceBonus >= 0.12D)
            && player.getRandom().nextDouble() < 0.12D) {
            net.minecraft.world.level.block.Block.popResource(player.serverLevel(), pos,
                new ItemStack(state.is(Blocks.CLAY) ? Items.CLAY_BALL : Items.WHEAT_SEEDS, state.is(Blocks.CLAY) ? 3 : 2));
        } else if ("sand_collector".equals(jobId)
            && state.is(Blocks.RED_SAND)
            && (jobManager.effectBonus(player, "cache_finder") > 0.0D || resourceBonus >= 0.12D)
            && player.getRandom().nextDouble() < 0.12D) {
            net.minecraft.world.level.block.Block.popResource(player.serverLevel(), pos, new ItemStack(Items.BRICK, 2));
        } else if ("ice_harvester".equals(jobId)
            && state.is(Blocks.PACKED_ICE)
            && (jobManager.effectBonus(player, "artifact_bonus") > 0.0D || resourceBonus >= 0.12D)
            && player.getRandom().nextDouble() < 0.12D) {
            net.minecraft.world.level.block.Block.popResource(player.serverLevel(), pos, new ItemStack(Items.SNOWBALL, 4));
        } else if ("herbalist".equals(jobId)
            && (state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.NETHER_WART))
            && (jobManager.effectBonus(player, "crop_bonus") >= 0.10D || jobManager.effectBonus(player, "ingredient_save") > 0.0D)
            && player.getRandom().nextDouble() < 0.20D) {
            ItemStack herb = state.is(Blocks.NETHER_WART) ? new ItemStack(Items.NETHER_WART, 2) : new ItemStack(Items.SWEET_BERRIES, 2);
            net.minecraft.world.level.block.Block.popResource(player.serverLevel(), pos, herb);
            if (state.is(Blocks.SWEET_BERRY_BUSH) && player.getRandom().nextDouble() < 0.10D) {
                net.minecraft.world.level.block.Block.popResource(player.serverLevel(), pos, new ItemStack(Items.GLOW_BERRIES, 2));
            }
            if (jobManager.effectBonus(player, "farm_aura") >= 0.10D) {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0, true, false));
            }
        } else if ("beekeeper".equals(jobId)
            && (state.is(Blocks.BEEHIVE) || state.is(Blocks.BEE_NEST))
            && (jobManager.effectBonus(player, "honey_bonus") > 0.0D || resourceBonus >= 0.10D)
            && player.getRandom().nextDouble() < 0.18D) {
            net.minecraft.world.level.block.Block.popResource(player.serverLevel(), pos, new ItemStack(Items.HONEYCOMB, state.is(Blocks.BEE_NEST) ? 3 : 2));
            if (state.is(Blocks.BEE_NEST)) {
                net.minecraft.world.level.block.Block.popResource(player.serverLevel(), pos, new ItemStack(Items.HONEY_BOTTLE));
                player.addEffect(new MobEffectInstance(MobEffects.LUCK, 120, 0, true, false));
            }
        } else if (("explorer".equals(jobId) || "archaeologist".equals(jobId) || "treasure_hunter".equals(jobId)) && jobManager.effectBonus(player, "explore_bonus") >= 0.15D && player.getRandom().nextDouble() < 0.08D) {
            net.minecraft.world.level.block.Block.popResource(player.serverLevel(), pos, new ItemStack(Items.COMPASS));
            if ("treasure_hunter".equals(jobId) && jobManager.effectBonus(player, "treasure_chance") > 0.0D) {
                net.minecraft.world.level.block.Block.popResource(player.serverLevel(), pos, new ItemStack(Items.GOLD_NUGGET, 3));
            }
        } else if (("miner".equals(jobId) || "deep_miner".equals(jobId)) && jobManager.effectBonus(player, "rare_gem_bonus") > 0.0D && player.getRandom().nextDouble() < 0.08D) {
            net.minecraft.world.level.block.Block.popResource(player.serverLevel(), pos, new ItemStack(Items.EMERALD));
        } else if (("engineer".equals(jobId) || "redstone_technician".equals(jobId)) && state.is(Blocks.REDSTONE_ORE) && jobManager.effectBonus(player, "craft_bonus") >= 0.10D) {
            net.minecraft.world.level.block.Block.popResource(player.serverLevel(), pos, new ItemStack(Items.REDSTONE));
        } else if ("archaeologist".equals(jobId) && (state.is(Blocks.SUSPICIOUS_SAND) || state.is(Blocks.SUSPICIOUS_GRAVEL)) && (jobManager.effectBonus(player, "artifact_bonus") > 0.0D || resourceBonus >= 0.10D)) {
            net.minecraft.world.level.block.Block.popResource(player.serverLevel(), pos, new ItemStack(Items.BRUSH));
            if (player.getRandom().nextDouble() < 0.12D) {
                net.minecraft.world.level.block.Block.popResource(player.serverLevel(), pos, new ItemStack(Items.PAPER, 2));
            }
        } else if ("quarry_worker".equals(jobId) && state.is(Blocks.GRANITE) && jobManager.effectBonus(player, "artifact_bonus") > 0.0D && player.getRandom().nextDouble() < 0.10D) {
            net.minecraft.world.level.block.Block.popResource(player.serverLevel(), pos, new ItemStack(Items.FLINT));
        } else if ("digger".equals(jobId) && state.is(Blocks.ROOTED_DIRT) && jobManager.effectBonus(player, "cache_finder") > 0.0D && player.getRandom().nextDouble() < 0.12D) {
            net.minecraft.world.level.block.Block.popResource(player.serverLevel(), pos, new ItemStack(Items.HANGING_ROOTS));
        } else if ("sand_collector".equals(jobId) && state.is(Blocks.RED_SAND) && jobManager.effectBonus(player, "cache_finder") > 0.0D && player.getRandom().nextDouble() < 0.10D) {
            net.minecraft.world.level.block.Block.popResource(player.serverLevel(), pos, new ItemStack(Items.DEAD_BUSH));
        } else if ("ice_harvester".equals(jobId) && state.is(Blocks.BLUE_ICE) && jobManager.effectBonus(player, "artifact_bonus") > 0.0D && player.getRandom().nextDouble() < 0.10D) {
            net.minecraft.world.level.block.Block.popResource(player.serverLevel(), pos, new ItemStack(Items.PACKED_ICE));
        }
    }

    private void applyPlaceBonuses(ServerPlayer player, BlockPos pos, BlockState state) {
        String jobId = jobManager.firstAssignedJob(player, "builder", "mason", "carpenter");
        if (jobId == null) {
            return;
        }
        double utility = ("builder".equals(jobId) || "mason".equals(jobId) || "carpenter".equals(jobId))
            ? jobManager.effectBonus(player, "builder_aura")
            : jobManager.effectBonus(player, "utility_bonus");
        if (("builder".equals(jobId) || "mason".equals(jobId) || "carpenter".equals(jobId))
            && (utility >= 0.10D || jobManager.effectBonus(player, "block_refund") > 0.0D)
            && player.getRandom().nextDouble() < 0.12D) {
            ItemStack refund = new ItemStack(state.getBlock().asItem());
            if (!refund.isEmpty()) {
                if (!player.addItem(refund)) {
                    player.drop(refund, false);
                }
            }
            if ("builder".equals(jobId)) {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 0, true, false));
            }
        }
        if (("builder".equals(jobId) || "mason".equals(jobId) || "carpenter".equals(jobId))
            && jobManager.effectBonus(player, "decor_bonus") > 0.0D
            && isDecorativeBlock(state)
            && player.getRandom().nextDouble() < 0.10D) {
            ItemStack bonus = new ItemStack(Items.GLOWSTONE_DUST);
            if (!player.addItem(bonus)) {
                player.drop(bonus, false);
            }
        }
    }

    private void applyCraftBonuses(ServerPlayer player, ItemStack stack) {
        String jobId = jobManager.firstAssignedJob(player, "cook", "blacksmith", "armorer", "engineer", "redstone_technician", "carpenter", "beekeeper", "shepherd");
        if (jobId == null) {
            return;
        }
        if ("cook".equals(jobId)
            && isFood(stack)
            && (jobManager.effectBonus(player, "craft_bonus") >= 0.10D || jobManager.effectBonus(player, "ingredient_save") > 0.0D)
            && player.getRandom().nextDouble() < 0.20D) {
            ItemStack extra = stack.copy();
            extra.setCount(1);
            if (!player.addItem(extra)) {
                player.drop(extra, false);
            }
            if (jobManager.effectBonus(player, "craft_aura") >= 0.10D) {
                player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 20, 0, true, false));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 0, true, false));
            }
            if (isPreparedMeal(stack) && player.getRandom().nextDouble() < 0.10D) {
                ItemStack spice = new ItemStack(Items.COOKIE);
                if (!player.addItem(spice)) {
                    player.drop(spice, false);
                }
            }
        } else if ("blacksmith".equals(jobId)
            && isWeapon(stack)
            && (jobManager.effectBonus(player, "craft_bonus") >= 0.10D || jobManager.effectBonus(player, "ingredient_save") > 0.0D)) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 160, 0, true, false));
            if (jobManager.effectBonus(player, "craft_aura") >= 0.10D) {
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 120, 0, true, false));
            }
            if (jobManager.effectBonus(player, "ingredient_save") > 0.0D && player.getRandom().nextDouble() < 0.10D) {
                ItemStack grit = new ItemStack(Items.FLINT);
                if (!player.addItem(grit)) {
                    player.drop(grit, false);
                }
            }
        } else if ("armorer".equals(jobId)
            && isArmor(stack)
            && (jobManager.effectBonus(player, "craft_bonus") >= 0.10D || jobManager.effectBonus(player, "ingredient_save") > 0.0D)) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 0, true, false));
            if (jobManager.effectBonus(player, "craft_aura") >= 0.10D) {
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 140, 0, true, false));
            }
            if (jobManager.effectBonus(player, "ingredient_save") > 0.0D && player.getRandom().nextDouble() < 0.10D) {
                ItemStack lining = new ItemStack(Items.LEATHER);
                if (!player.addItem(lining)) {
                    player.drop(lining, false);
                }
            }
        } else if (("engineer".equals(jobId) || "redstone_technician".equals(jobId)) && isRedstoneTech(stack) && jobManager.effectBonus(player, "craft_bonus") >= 0.10D && player.getRandom().nextDouble() < 0.18D) {
            ItemStack redstone = new ItemStack(Items.REDSTONE);
            if (!player.addItem(redstone)) {
                player.drop(redstone, false);
            }
            if (jobManager.effectBonus(player, "craft_aura") >= 0.10D) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 140, 0, true, false));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 0, true, false));
            }
            if ("engineer".equals(jobId) && player.getRandom().nextDouble() < 0.12D) {
                ItemStack quartz = new ItemStack(Items.QUARTZ);
                if (!player.addItem(quartz)) {
                    player.drop(quartz, false);
                }
            }
        } else if ("carpenter".equals(jobId) && stack.is(Items.CHEST) && jobManager.effectBonus(player, "craft_bonus") >= 0.10D) {
            ItemStack planks = new ItemStack(Items.OAK_PLANKS, 2);
            if (!player.addItem(planks)) {
                player.drop(planks, false);
            }
        } else if ("beekeeper".equals(jobId)
            && (stack.is(Items.HONEY_BOTTLE) || stack.is(Items.HONEYCOMB))
            && (jobManager.effectBonus(player, "breed_bonus") >= 0.10D || jobManager.effectBonus(player, "honey_bonus") > 0.0D)) {
            ItemStack honey = new ItemStack(Items.HONEYCOMB);
            if (!player.addItem(honey)) {
                player.drop(honey, false);
            }
            if (player.getRandom().nextDouble() < 0.12D) {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 0, true, false));
            }
        } else if ("shepherd".equals(jobId)
            && isWoolCraft(stack)
            && (jobManager.effectBonus(player, "breed_bonus") >= 0.10D || jobManager.effectBonus(player, "pasture_aura") >= 0.10D)
            && player.getRandom().nextDouble() < 0.20D) {
            ItemStack wool = new ItemStack(Items.WHITE_WOOL, 2);
            if (!player.addItem(wool)) {
                player.drop(wool, false);
            }
            if (isCarpetCraft(stack) && player.getRandom().nextDouble() < 0.12D) {
                ItemStack carpet = new ItemStack(Items.WHITE_CARPET, 2);
                if (!player.addItem(carpet)) {
                    player.drop(carpet, false);
                }
            }
        }
    }

    private void applyKillBonuses(ServerPlayer player, BlockPos pos, ResourceLocation targetId) {
        String jobId = jobManager.firstAssignedJob(player, "hunter", "monster_slayer", "guard", "bounty_hunter", "defender", "boss_hunter");
        if (jobId == null) {
            return;
        }
        double combatAura = jobManager.effectBonus(player, "combat_aura");
        double combatBonus = jobManager.effectBonus(player, "combat_bonus");
        if ("hunter".equals(jobId) && combatAura >= 0.10D) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0, true, false));
        }
        if ("hunter".equals(jobId) && combatBonus >= 0.15D && player.getRandom().nextDouble() < 0.20D) {
            net.minecraft.world.level.block.Block.popResource(player.serverLevel(), pos, new ItemStack(Items.EMERALD));
        } else if ("monster_slayer".equals(jobId)
            && (combatBonus >= 0.10D || jobManager.effectBonus(player, "loot_bonus") > 0.0D)
            && player.getRandom().nextDouble() < 0.18D) {
            net.minecraft.world.level.block.Block.popResource(player.serverLevel(), pos, new ItemStack(Items.IRON_INGOT));
            if (jobManager.effectBonus(player, "elite_tracker") > 0.0D) {
                player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, true, false));
            }
        } else if ("guard".equals(jobId)
            && (targetId.equals(ResourceLocationUtil.minecraft("pillager")) || targetId.equals(ResourceLocationUtil.minecraft("vindicator")))
            && (combatBonus >= 0.10D || jobManager.effectBonus(player, "combat_aura") >= 0.10D)) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 0, true, false));
            player.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 160, 0, true, false));
            if (player.getRandom().nextDouble() < 0.14D) {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, true, false));
            }
            if (player.getRandom().nextDouble() < 0.16D) {
                net.minecraft.world.level.block.Block.popResource(player.serverLevel(), pos, new ItemStack(Items.SHIELD));
            }
            if (targetId.equals(ResourceLocationUtil.minecraft("vindicator")) && player.getRandom().nextDouble() < 0.12D) {
                net.minecraft.world.level.block.Block.popResource(player.serverLevel(), pos, new ItemStack(Items.IRON_INGOT));
            } else if (targetId.equals(ResourceLocationUtil.minecraft("pillager")) && player.getRandom().nextDouble() < 0.12D) {
                net.minecraft.world.level.block.Block.popResource(player.serverLevel(), pos, new ItemStack(Items.CROSSBOW));
            }
        } else if ("bounty_hunter".equals(jobId)
            && (targetId.equals(ResourceLocationUtil.minecraft("evoker"))
            || targetId.equals(ResourceLocationUtil.minecraft("blaze"))
            || targetId.equals(ResourceLocationUtil.minecraft("witch")))
            && (combatBonus >= 0.10D || jobManager.effectBonus(player, "elite_tracker") > 0.0D)) {
            if (player.getRandom().nextDouble() < 0.18D) {
                net.minecraft.world.level.block.Block.popResource(player.serverLevel(), pos, new ItemStack(Items.EMERALD, 2));
            }
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 0, true, false));
            if (targetId.equals(ResourceLocationUtil.minecraft("blaze"))) {
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 140, 0, true, false));
                if (player.getRandom().nextDouble() < 0.12D) {
                    net.minecraft.world.level.block.Block.popResource(player.serverLevel(), pos, new ItemStack(Items.BLAZE_POWDER, 2));
                }
            } else if (targetId.equals(ResourceLocationUtil.minecraft("witch"))) {
                player.addEffect(new MobEffectInstance(MobEffects.LUCK, 180, 0, true, false));
                if (player.getRandom().nextDouble() < 0.16D) {
                    net.minecraft.world.level.block.Block.popResource(player.serverLevel(), pos, new ItemStack(Items.GLASS_BOTTLE, 2));
                }
            } else {
                player.addEffect(new MobEffectInstance(MobEffects.LUCK, 140, 0, true, false));
            }
        } else if ("defender".equals(jobId)
            && (targetId.equals(ResourceLocationUtil.minecraft("zombie")) || targetId.equals(ResourceLocationUtil.minecraft("husk")))
            && (combatBonus >= 0.10D || combatAura >= 0.10D)) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 140, 0, true, false));
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 120, 0, true, false));
            if (player.getRandom().nextDouble() < 0.16D) {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, true, false));
            }
            if (player.getRandom().nextDouble() < 0.20D) {
                net.minecraft.world.level.block.Block.popResource(player.serverLevel(), pos, new ItemStack(Items.IRON_NUGGET, 3));
            }
            if (targetId.equals(ResourceLocationUtil.minecraft("husk")) && player.getRandom().nextDouble() < 0.12D) {
                net.minecraft.world.level.block.Block.popResource(player.serverLevel(), pos, new ItemStack(Items.ROTTEN_FLESH, 2));
            } else if (targetId.equals(ResourceLocationUtil.minecraft("zombie")) && player.getRandom().nextDouble() < 0.12D) {
                net.minecraft.world.level.block.Block.popResource(player.serverLevel(), pos, new ItemStack(Items.BREAD));
            }
        } else if ("boss_hunter".equals(jobId)
            && (combatBonus >= 0.10D || jobManager.effectBonus(player, "loot_bonus") > 0.0D || jobManager.effectBonus(player, "salary_bonus") >= 0.10D)) {
            net.minecraft.world.level.block.Block.popResource(player.serverLevel(), pos, new ItemStack(Items.GOLDEN_APPLE));
            if (jobManager.effectBonus(player, "elite_tracker") > 0.0D) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 160, 0, true, false));
            }
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 120, 0, true, false));
            if (player.getRandom().nextDouble() < 0.14D) {
                net.minecraft.world.level.block.Block.popResource(player.serverLevel(), pos, new ItemStack(Items.ENDER_PEARL));
            }
        }
    }

    private void applyFishingBonuses(ServerPlayer player, ItemStack original, ItemFishedEvent event) {
        if (!jobManager.hasAssignedJob(player, "fisher")) {
            return;
        }
        double resourceBonus = Math.max(jobManager.effectBonus(player, "fish_bonus"), jobManager.effectBonus(player, "treasure_chance"));
        if (resourceBonus > 0.0D && player.getRandom().nextDouble() < Math.min(0.30D, resourceBonus)) {
            ItemStack extra = original.copy();
            extra.setCount(1);
            if (!player.addItem(extra)) {
                player.drop(extra, false);
            }
        }
        if (jobManager.effectBonus(player, "junk_reduction") > 0.0D
            && isFishingJunk(original)
            && player.getRandom().nextDouble() < 0.35D) {
            event.getDrops().removeIf(drop -> drop.getItem() == original.getItem());
            ItemStack replacement = new ItemStack(Items.COD);
            if (!player.addItem(replacement)) {
                player.drop(replacement, false);
            }
        }
    }

    private void applyPassiveEffects(ServerPlayer player) {
        String jobId = jobManager.firstAssignedJob(player,
            "miner", "deep_miner", "lumberjack", "forester", "farmer", "harvester", "herbalist",
            "animal_breeder", "shepherd", "beekeeper", "fisher", "hunter", "monster_slayer", "guard", "bounty_hunter", "defender", "boss_hunter",
            "builder", "mason", "carpenter", "blacksmith", "armorer", "cook", "engineer", "redstone_technician",
            "merchant", "alchemist", "enchanter", "explorer", "treasure_hunter", "archaeologist", "sand_collector", "ice_harvester", "quarry_worker", "digger");
        if (jobId == null) {
            return;
        }
        double utility = switch (jobId) {
            case "miner", "deep_miner" -> jobManager.effectBonus(player, "mine_aura");
            case "lumberjack", "forester" -> jobManager.effectBonus(player, "forest_aura");
            case "farmer", "harvester", "herbalist" -> jobManager.effectBonus(player, "farm_aura");
            case "animal_breeder", "shepherd", "beekeeper" -> jobManager.effectBonus(player, "pasture_aura");
            case "fisher" -> jobManager.effectBonus(player, "water_aura");
            case "hunter", "monster_slayer", "guard", "bounty_hunter", "defender", "boss_hunter" -> jobManager.effectBonus(player, "combat_aura");
            case "builder", "mason", "carpenter" -> jobManager.effectBonus(player, "builder_aura");
            case "blacksmith", "armorer", "cook", "engineer", "redstone_technician" -> jobManager.effectBonus(player, "craft_aura");
            case "merchant" -> jobManager.effectBonus(player, "merchant_aura");
            case "alchemist", "enchanter" -> jobManager.effectBonus(player, "magic_aura");
            case "explorer", "treasure_hunter", "archaeologist", "sand_collector", "ice_harvester", "quarry_worker", "digger" -> jobManager.effectBonus(player, "explore_aura");
            default -> jobManager.effectBonus(player, "utility_bonus");
        };
        if (("miner".equals(jobId) || "deep_miner".equals(jobId)) && player.getY() < 50) {
            if (utility >= 0.05D) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 120, 0, true, false));
            }
            if (utility >= 0.15D) {
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 120, 0, true, false));
            }
            if (jobManager.effectBonus(player, "ore_vision") > 0.0D) {
                player.addEffect(new MobEffectInstance(MobEffects.LUCK, 120, 0, true, false));
            }
            if (jobManager.effectBonus(player, "fall_guard") > 0.0D && player.fallDistance > 3.0F) {
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 80, 0, true, false));
            }
        } else if (("lumberjack".equals(jobId) || "forester".equals(jobId)) && utility >= 0.10D) {
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 120, 0, true, false));
        } else if (("farmer".equals(jobId) || "harvester".equals(jobId) || "herbalist".equals(jobId)) && utility >= 0.10D) {
            player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 40, 0, true, false));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 0, true, false));
        } else if (("animal_breeder".equals(jobId) || "shepherd".equals(jobId) || "beekeeper".equals(jobId)) && utility >= 0.10D) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0, true, false));
            if ("animal_breeder".equals(jobId)) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 0, true, false));
                player.addEffect(new MobEffectInstance(MobEffects.JUMP, 120, 0, true, false));
            } else if ("shepherd".equals(jobId)) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 0, true, false));
                player.addEffect(new MobEffectInstance(MobEffects.JUMP, 120, 0, true, false));
            } else if ("beekeeper".equals(jobId)) {
                player.addEffect(new MobEffectInstance(MobEffects.LUCK, 120, 0, true, false));
            }
        } else if (("hunter".equals(jobId) || "monster_slayer".equals(jobId) || "guard".equals(jobId) || "defender".equals(jobId)) && utility >= 0.10D) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 0, true, false));
        } else if ("sand_collector".equals(jobId) && utility >= 0.10D) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 0, true, false));
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, 120, 0, true, false));
        } else if ("ice_harvester".equals(jobId) && utility >= 0.10D) {
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 120, 0, true, false));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 0, true, false));
        } else if ("quarry_worker".equals(jobId) && utility >= 0.10D) {
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 120, 0, true, false));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 0, true, false));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, true, false));
        } else if ("digger".equals(jobId) && utility >= 0.10D) {
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 120, 0, true, false));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 0, true, false));
        } else if ("explorer".equals(jobId) && utility >= 0.10D) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 120, 0, true, false));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 0, true, false));
        } else if ("treasure_hunter".equals(jobId) && utility >= 0.10D) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 120, 0, true, false));
            player.addEffect(new MobEffectInstance(MobEffects.LUCK, 120, 0, true, false));
        } else if ("archaeologist".equals(jobId) && utility >= 0.10D) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 120, 0, true, false));
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 120, 0, true, false));
        } else if ("redstone_technician".equals(jobId) && utility >= 0.10D) {
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 120, 0, true, false));
        } else if ("engineer".equals(jobId) && utility >= 0.10D) {
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 120, 0, true, false));
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 120, 0, true, false));
        } else if (("builder".equals(jobId) || "mason".equals(jobId) || "carpenter".equals(jobId)) && utility >= 0.10D) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 0, true, false));
        } else if ("merchant".equals(jobId) && utility >= 0.10D) {
            player.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 120, 0, true, false));
        } else if ("cook".equals(jobId) && utility >= 0.10D) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0, true, false));
        } else if ("armorer".equals(jobId) && utility >= 0.10D) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 0, true, false));
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 80, 0, true, false));
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

    private ItemStack smeltResult(BlockState state) {
        if (state.is(Blocks.IRON_ORE) || state.is(Blocks.DEEPSLATE_IRON_ORE)) {
            return new ItemStack(Items.IRON_INGOT);
        }
        if (state.is(Blocks.GOLD_ORE) || state.is(Blocks.DEEPSLATE_GOLD_ORE)) {
            return new ItemStack(Items.GOLD_INGOT);
        }
        if (state.is(Blocks.COPPER_ORE) || state.is(Blocks.DEEPSLATE_COPPER_ORE)) {
            return new ItemStack(Items.COPPER_INGOT);
        }
        if (state.is(Blocks.ANCIENT_DEBRIS)) {
            return new ItemStack(Items.NETHERITE_SCRAP);
        }
        return ItemStack.EMPTY;
    }

    private ItemStack seedResult(BlockState state) {
        if (state.is(Blocks.WHEAT)) {
            return new ItemStack(Items.WHEAT_SEEDS);
        }
        if (state.is(Blocks.BEETROOTS)) {
            return new ItemStack(Items.BEETROOT_SEEDS);
        }
        if (state.is(Blocks.CARROTS)) {
            return new ItemStack(Items.CARROT);
        }
        if (state.is(Blocks.POTATOES)) {
            return new ItemStack(Items.POTATO);
        }
        return ItemStack.EMPTY;
    }

    private boolean isFood(ItemStack stack) {
        return stack.isEdible() || stack.is(Items.BREAD) || stack.is(Items.CAKE) || stack.is(Items.COOKED_BEEF);
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

    private boolean isMetalIngot(ItemStack stack) {
        return stack.is(Items.IRON_INGOT) || stack.is(Items.GOLD_INGOT) || stack.is(Items.COPPER_INGOT) || stack.is(Items.NETHERITE_SCRAP);
    }

    private boolean isWeapon(ItemStack stack) {
        return stack.is(Items.WOODEN_SWORD) || stack.is(Items.STONE_SWORD) || stack.is(Items.IRON_SWORD) || stack.is(Items.DIAMOND_SWORD)
            || stack.is(Items.NETHERITE_SWORD) || stack.is(Items.IRON_AXE) || stack.is(Items.DIAMOND_AXE) || stack.is(Items.NETHERITE_AXE);
    }

    private boolean isArmor(ItemStack stack) {
        return stack.is(Items.LEATHER_HELMET) || stack.is(Items.LEATHER_CHESTPLATE) || stack.is(Items.LEATHER_LEGGINGS) || stack.is(Items.LEATHER_BOOTS)
            || stack.is(Items.CHAINMAIL_HELMET) || stack.is(Items.CHAINMAIL_CHESTPLATE) || stack.is(Items.CHAINMAIL_LEGGINGS) || stack.is(Items.CHAINMAIL_BOOTS)
            || stack.is(Items.IRON_HELMET) || stack.is(Items.IRON_CHESTPLATE) || stack.is(Items.IRON_LEGGINGS) || stack.is(Items.IRON_BOOTS)
            || stack.is(Items.DIAMOND_HELMET) || stack.is(Items.DIAMOND_CHESTPLATE) || stack.is(Items.DIAMOND_LEGGINGS) || stack.is(Items.DIAMOND_BOOTS)
            || stack.is(Items.NETHERITE_HELMET) || stack.is(Items.NETHERITE_CHESTPLATE) || stack.is(Items.NETHERITE_LEGGINGS) || stack.is(Items.NETHERITE_BOOTS);
    }

    private boolean isRedstoneTech(ItemStack stack) {
        return stack.is(Items.REDSTONE) || stack.is(Items.REPEATER) || stack.is(Items.COMPARATOR) || stack.is(Items.OBSERVER)
            || stack.is(Items.PISTON) || stack.is(Items.STICKY_PISTON) || stack.is(Items.DISPENSER) || stack.is(Items.DROPPER);
    }

    private boolean isBoss(ResourceLocation id) {
        return id.equals(ResourceLocationUtil.minecraft("wither")) || id.equals(ResourceLocationUtil.minecraft("ender_dragon"));
    }

    private boolean isLog(BlockState state) {
        return state.is(Blocks.OAK_LOG) || state.is(Blocks.SPRUCE_LOG) || state.is(Blocks.BIRCH_LOG) || state.is(Blocks.JUNGLE_LOG)
            || state.is(Blocks.ACACIA_LOG) || state.is(Blocks.DARK_OAK_LOG) || state.is(Blocks.MANGROVE_LOG) || state.is(Blocks.CHERRY_LOG);
    }

    private boolean isStoneFamily(BlockState state) {
        return state.is(Blocks.STONE) || state.is(Blocks.COBBLESTONE) || state.is(Blocks.ANDESITE) || state.is(Blocks.DIORITE) || state.is(Blocks.GRANITE);
    }

    private boolean isDecorativeBlock(BlockState state) {
        return state.is(Blocks.GLASS)
            || state.is(Blocks.TERRACOTTA)
            || state.is(Blocks.WHITE_TERRACOTTA)
            || state.is(Blocks.ORANGE_TERRACOTTA)
            || state.is(Blocks.BLUE_TERRACOTTA)
            || state.is(Blocks.BRICKS)
            || state.is(Blocks.STONE_BRICKS)
            || state.is(Blocks.CHISELED_STONE_BRICKS)
            || state.is(Blocks.POLISHED_ANDESITE)
            || state.is(Blocks.POLISHED_DIORITE)
            || state.is(Blocks.POLISHED_GRANITE);
    }

    private boolean isLooseTerrain(BlockState state) {
        return state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.GRAVEL) || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.ROOTED_DIRT);
    }

    private boolean isForestBiome(ServerPlayer player, BlockPos pos) {
        ResourceLocation id = player.serverLevel().getBiome(pos).unwrapKey().map(key -> key.location()).orElse(null);
        if (id == null) {
            return false;
        }
        String path = resourcePath(id);
        return path.contains("forest") || path.contains("grove") || path.contains("taiga") || path.contains("cherry");
    }

    private boolean isBulkHarvestBlock(BlockState state) {
        return state.is(Blocks.PUMPKIN) || state.is(Blocks.MELON);
    }

    private ItemStack bulkHarvestResult(BlockState state) {
        if (state.is(Blocks.PUMPKIN)) {
            return new ItemStack(Items.PUMPKIN_PIE);
        }
        if (state.is(Blocks.MELON)) {
            return new ItemStack(Items.MELON_SLICE, 3);
        }
        return ItemStack.EMPTY;
    }

    private ItemStack saplingResult(BlockState state) {
        if (state.is(Blocks.OAK_LOG)) {
            return new ItemStack(Items.OAK_SAPLING);
        }
        if (state.is(Blocks.SPRUCE_LOG)) {
            return new ItemStack(Items.SPRUCE_SAPLING);
        }
        if (state.is(Blocks.BIRCH_LOG)) {
            return new ItemStack(Items.BIRCH_SAPLING);
        }
        if (state.is(Blocks.JUNGLE_LOG)) {
            return new ItemStack(Items.JUNGLE_SAPLING);
        }
        if (state.is(Blocks.ACACIA_LOG)) {
            return new ItemStack(Items.ACACIA_SAPLING);
        }
        if (state.is(Blocks.DARK_OAK_LOG)) {
            return new ItemStack(Items.DARK_OAK_SAPLING);
        }
        if (state.is(Blocks.MANGROVE_LOG)) {
            return new ItemStack(Items.MANGROVE_PROPAGULE);
        }
        if (state.is(Blocks.CHERRY_LOG)) {
            return new ItemStack(Items.CHERRY_SAPLING);
        }
        return ItemStack.EMPTY;
    }

    private boolean isFishingJunk(ItemStack stack) {
        return stack.is(Items.BOWL)
            || stack.is(Items.LEATHER)
            || stack.is(Items.ROTTEN_FLESH)
            || stack.is(Items.STICK)
            || stack.is(Items.STRING)
            || stack.is(Items.BONE)
            || stack.is(Items.TRIPWIRE_HOOK)
            || stack.is(Items.INK_SAC);
    }

    private boolean isWoolCraft(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) {
            return false;
        }
        String path = resourcePath(id);
        return path.endsWith("_wool") || path.endsWith("_carpet");
    }

    private boolean isCarpetCraft(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && resourcePath(id).endsWith("_carpet");
    }

    private String resourcePath(ResourceLocation id) {
        String raw = id.toString();
        int colon = raw.indexOf(':');
        return colon >= 0 ? raw.substring(colon + 1) : raw;
    }
}
