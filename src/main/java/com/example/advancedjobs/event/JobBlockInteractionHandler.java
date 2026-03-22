package com.example.advancedjobs.event;

import com.example.advancedjobs.api.JobActionContext;
import com.example.advancedjobs.config.ConfigManager;
import com.example.advancedjobs.job.JobManager;
import com.example.advancedjobs.model.JobActionType;
import com.example.advancedjobs.util.DebugLog;
import com.example.advancedjobs.util.ResourceLocationUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.registries.ForgeRegistries;

class JobBlockInteractionHandler {
    private final JobManager jobManager;
    private final RewardCalculationHandler rewards;
    private final AntiExploitHandler antiExploit;

    JobBlockInteractionHandler(JobManager jobManager, RewardCalculationHandler rewards, AntiExploitHandler antiExploit) {
        this.jobManager = jobManager;
        this.rewards = rewards;
        this.antiExploit = antiExploit;
    }

    void handleRightClickBlock(PlayerInteractEvent.RightClickBlock event, ServerPlayer player, BlockState state) {
        if (state.is(Blocks.ENCHANTING_TABLE)) {
            handleEnchanting(event, player);
            return;
        }
        if (state.is(Blocks.BREWING_STAND)) {
            handleBrewing(event, player);
            return;
        }
        if (isLootContainer(state)) {
            handleLootContainer(event, player, state);
            return;
        }
        if (isRedstoneComponent(state)) {
            handleRedstone(event, player, state);
        }
    }

    private void handleEnchanting(PlayerInteractEvent.RightClickBlock event, ServerPlayer player) {
        ItemStack stack = player.getMainHandItem().isEmpty() ? new ItemStack(Items.BOOK) : player.getMainHandItem();
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (antiExploit.isOnCooldown(player.getUUID(), "enchant_cd|" + id)) {
            DebugLog.log("Blocked by cooldown: player=" + player.getGameProfile().getName() + " action=enchant target=" + id);
            return;
        }
        rewards.reward(new JobActionContext(player, JobActionType.ENCHANT_ITEM, id, player.serverLevel(), event.getPos(), false));
        if (jobManager.hasAssignedJob(player, "enchanter") && jobManager.effectBonus(player, "salary_bonus") >= 0.10D) {
            player.addEffect(new MobEffectInstance(MobEffects.LUCK, 200, 0, true, false));
        }
        if (jobManager.hasAssignedJob(player, "enchanter") && jobManager.effectBonus(player, "rare_magic_bonus") > 0.0D) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 0, true, false));
        }
        if (jobManager.hasAssignedJob(player, "enchanter") && jobManager.effectBonus(player, "magic_aura") >= 0.10D) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0, true, false));
            player.addEffect(new MobEffectInstance(MobEffects.LUCK, 120, 0, true, false));
        }
        if (jobManager.hasAssignedJob(player, "enchanter")
            && jobManager.effectBonus(player, "magic_aura") >= 0.10D
            && player.getRandom().nextDouble() < 0.12D) {
            ItemStack lapis = new ItemStack(Items.LAPIS_LAZULI, 2);
            if (!player.addItem(lapis)) {
                player.drop(lapis, false);
            }
        }
        if (jobManager.hasAssignedJob(player, "enchanter")
            && jobManager.effectBonus(player, "rare_magic_bonus") > 0.0D
            && player.getRandom().nextDouble() < 0.08D) {
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 120, 0, true, false));
        }
    }

    private void handleBrewing(PlayerInteractEvent.RightClickBlock event, ServerPlayer player) {
        if (antiExploit.isOnCooldown(player.getUUID(), "brew_cd|minecraft:potion")) {
            DebugLog.log("Blocked by cooldown: player=" + player.getGameProfile().getName() + " action=brew target=minecraft:potion");
            return;
        }
        rewards.reward(new JobActionContext(player, JobActionType.BREW_POTION, ResourceLocationUtil.minecraft("potion"), player.serverLevel(), event.getPos(), false));
        if (jobManager.hasAssignedJob(player, "alchemist")
            && (jobManager.effectBonus(player, "magic_bonus") >= 0.10D || jobManager.effectBonus(player, "ingredient_save") > 0.0D)
            && player.getRandom().nextDouble() < 0.20D) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 0, true, false));
        }
        if (jobManager.hasAssignedJob(player, "alchemist") && jobManager.effectBonus(player, "rare_magic_bonus") > 0.0D) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 0, true, false));
        }
        if (jobManager.hasAssignedJob(player, "alchemist")
            && jobManager.effectBonus(player, "ingredient_save") > 0.0D
            && player.getRandom().nextDouble() < 0.12D) {
            ItemStack ingredient = player.getRandom().nextBoolean() ? new ItemStack(Items.NETHER_WART, 2) : new ItemStack(Items.GLASS_BOTTLE, 2);
            if (!player.addItem(ingredient)) {
                player.drop(ingredient, false);
            }
        }
        if (jobManager.hasAssignedJob(player, "alchemist") && jobManager.effectBonus(player, "magic_aura") >= 0.10D) {
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 160, 0, true, false));
        }
        if (jobManager.hasAssignedJob(player, "herbalist")
            && (jobManager.effectBonus(player, "farm_aura") >= 0.10D || jobManager.effectBonus(player, "ingredient_save") > 0.0D)) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 160, 0, true, false));
            if (player.getRandom().nextDouble() < 0.14D) {
                ItemStack herb = player.getRandom().nextBoolean() ? new ItemStack(Items.SWEET_BERRIES, 3) : new ItemStack(Items.NETHER_WART, 2);
                if (!player.addItem(herb)) {
                    player.drop(herb, false);
                }
            }
        }
    }

    private void handleLootContainer(PlayerInteractEvent.RightClickBlock event, ServerPlayer player, BlockState state) {
        ResourceLocation id = state.is(Blocks.BARREL) ? ResourceLocationUtil.minecraft("barrel") : ResourceLocationUtil.minecraft("chest");
        if (antiExploit.isOnCooldown(player.getUUID(), "loot_cd|" + id + "|" + event.getPos().asLong())) {
            DebugLog.log("Blocked by cooldown: player=" + player.getGameProfile().getName() + " action=loot target=" + id);
            return;
        }
        long lootCooldownMs = ConfigManager.COMMON.lootContainerRewardCooldownSeconds.get() * 1000L;
        if (!antiExploit.markLootContainer(player.getUUID(), event.getPos(), lootCooldownMs)) {
            DebugLog.log("Blocked repeated loot container reward: player=" + player.getGameProfile().getName() + " action=loot target=" + id + " pos=" + event.getPos());
            return;
        }
        rewards.reward(new JobActionContext(player, JobActionType.OPEN_LOOT_CHEST, id, player.serverLevel(), event.getPos(), false));
        if (jobManager.hasAssignedJob(player, "treasure_hunter")
            && (jobManager.effectBonus(player, "explore_bonus") >= 0.10D || jobManager.effectBonus(player, "treasure_chance") > 0.0D)
            && player.getRandom().nextDouble() < 0.15D) {
            ItemStack reward = new ItemStack(Items.GOLD_INGOT);
            if (!player.addItem(reward)) {
                player.drop(reward, false);
            }
        }
        if ((jobManager.hasAssignedJob(player, "explorer") || jobManager.hasAssignedJob(player, "treasure_hunter"))
            && jobManager.effectBonus(player, "cache_finder") > 0.0D
            && player.getRandom().nextDouble() < 0.12D) {
            ItemStack reward = player.getRandom().nextBoolean() ? new ItemStack(Items.COMPASS) : new ItemStack(Items.MAP);
            if (!player.addItem(reward)) {
                player.drop(reward, false);
            }
        }
        if (jobManager.hasAssignedJob(player, "explorer")
            && jobManager.effectBonus(player, "explore_aura") >= 0.10D
            && player.getRandom().nextDouble() < 0.10D) {
            ItemStack fieldTool = player.getRandom().nextBoolean() ? new ItemStack(Items.SPYGLASS) : new ItemStack(Items.CLOCK);
            if (!player.addItem(fieldTool)) {
                player.drop(fieldTool, false);
            }
        }
        if (jobManager.hasAssignedJob(player, "archaeologist")
            && (jobManager.effectBonus(player, "artifact_bonus") > 0.0D || jobManager.effectBonus(player, "explore_aura") >= 0.10D)
            && player.getRandom().nextDouble() < 0.12D) {
            ItemStack relic = player.getRandom().nextBoolean() ? new ItemStack(Items.BRICK) : new ItemStack(Items.FLOWER_POT);
            if (!player.addItem(relic)) {
                player.drop(relic, false);
            }
        }
        if (jobManager.hasAssignedJob(player, "archaeologist")
            && jobManager.effectBonus(player, "artifact_bonus") > 0.0D
            && player.getRandom().nextDouble() < 0.08D) {
            ItemStack notes = player.getRandom().nextBoolean() ? new ItemStack(Items.PAPER, 2) : new ItemStack(Items.CLAY_BALL, 3);
            if (!player.addItem(notes)) {
                player.drop(notes, false);
            }
        }
    }

    private void handleRedstone(PlayerInteractEvent.RightClickBlock event, ServerPlayer player, BlockState state) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (antiExploit.isOnCooldown(player.getUUID(), "redstone_cd|" + id)) {
            DebugLog.log("Blocked by cooldown: player=" + player.getGameProfile().getName() + " action=redstone target=" + id);
            return;
        }
        rewards.reward(new JobActionContext(player, JobActionType.REDSTONE_USE, id, player.serverLevel(), event.getPos(), false));
        if (jobManager.hasAssignedJob(player, "redstone_technician") && jobManager.effectBonus(player, "craft_bonus") >= 0.10D && player.getRandom().nextDouble() < 0.15D) {
            ItemStack reward = new ItemStack(Items.REDSTONE, 2);
            if (!player.addItem(reward)) {
                player.drop(reward, false);
            }
        }
    }

    private boolean isLootContainer(BlockState state) {
        return state.getBlock() instanceof ChestBlock || state.is(Blocks.BARREL) || state.is(Blocks.TRAPPED_CHEST);
    }

    private boolean isRedstoneComponent(BlockState state) {
        return state.is(Blocks.LEVER)
            || state.is(Blocks.STONE_BUTTON) || state.is(Blocks.OAK_BUTTON)
            || state.is(Blocks.REPEATER) || state.is(Blocks.COMPARATOR)
            || state.is(Blocks.OBSERVER) || state.is(Blocks.DISPENSER)
            || state.is(Blocks.DROPPER) || state.is(Blocks.PISTON) || state.is(Blocks.STICKY_PISTON)
            || state.is(Blocks.REDSTONE_WIRE) || state.is(Blocks.REDSTONE_TORCH);
    }
}
