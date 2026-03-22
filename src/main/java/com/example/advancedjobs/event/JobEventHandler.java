package com.example.advancedjobs.event;

import com.example.advancedjobs.api.JobActionContext;
import com.example.advancedjobs.content.ModItems;
import com.example.advancedjobs.config.ConfigManager;
import com.example.advancedjobs.entity.ContractsBoardEntity;
import com.example.advancedjobs.entity.DailyBoardEntity;
import com.example.advancedjobs.entity.HelpBoardEntity;
import com.example.advancedjobs.entity.JobsMasterEntity;
import com.example.advancedjobs.entity.LeaderboardBoardEntity;
import com.example.advancedjobs.entity.NpcRole;
import com.example.advancedjobs.entity.SalaryBoardEntity;
import com.example.advancedjobs.entity.SkillsBoardEntity;
import com.example.advancedjobs.entity.StatusBoardEntity;
import com.example.advancedjobs.job.JobManager;
import com.example.advancedjobs.model.JobActionType;
import com.example.advancedjobs.util.DebugLog;
import com.example.advancedjobs.util.TextUtil;
import com.example.advancedjobs.util.TimeUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
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

    public JobEventHandler(JobManager jobManager) {
        this.jobManager = jobManager;
        this.rewards = new RewardCalculationHandler(jobManager);
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
        if (player.getMainHandItem().is(ModItems.JOB_NPC_WAND.get()) || player.getOffhandItem().is(ModItems.JOB_NPC_WAND.get())) {
            boolean sprintMode = player.isSprinting();
            boolean helpMode = player.getOffhandItem().is(ModItems.JOB_NPC_WAND.get()) && sprintMode && !player.isShiftKeyDown();
            boolean statusMode = player.getOffhandItem().is(ModItems.JOB_NPC_WAND.get()) && sprintMode && player.isShiftKeyDown();
            boolean salaryMode = player.getOffhandItem().is(ModItems.JOB_NPC_WAND.get()) && player.isShiftKeyDown() && !sprintMode;
            boolean dailyMode = player.getOffhandItem().is(ModItems.JOB_NPC_WAND.get()) && !player.isShiftKeyDown() && !sprintMode;
            boolean contractsMode = !dailyMode && !salaryMode && player.isShiftKeyDown() && !sprintMode;
            boolean skillsMode = player.getMainHandItem().is(ModItems.JOB_NPC_WAND.get()) && sprintMode && !player.isShiftKeyDown();
            boolean leaderboardMode = player.getMainHandItem().is(ModItems.JOB_NPC_WAND.get()) && sprintMode && player.isShiftKeyDown();
            living.getPersistentData().putBoolean(JOB_NPC_TAG, !contractsMode && !dailyMode && !statusMode && !salaryMode && !skillsMode && !leaderboardMode && !helpMode);
            living.getPersistentData().putBoolean(CONTRACTS_BOARD_TAG, contractsMode);
            living.getPersistentData().putBoolean(DAILY_BOARD_TAG, dailyMode);
            living.getPersistentData().putBoolean(STATUS_BOARD_TAG, statusMode);
            living.getPersistentData().putBoolean(SALARY_BOARD_TAG, salaryMode);
            living.getPersistentData().putBoolean(SKILLS_BOARD_TAG, skillsMode);
            living.getPersistentData().putBoolean(LEADERBOARD_BOARD_TAG, leaderboardMode);
            living.getPersistentData().putBoolean(HELP_BOARD_TAG, helpMode);
            String roleId = dailyMode
                ? "daily_board"
                : statusMode
                    ? "status_board"
                : salaryMode
                    ? "salary_board"
                : helpMode
                    ? "help_board"
                : skillsMode
                    ? "skills_board"
                : leaderboardMode
                    ? "leaderboard_board"
                : contractsMode
                    ? "contracts_board"
                    : "jobs_master";
            living.setCustomName(Component.literal(ConfigManager.npcLabels().label(roleId)));
            living.setCustomNameVisible(true);
            if (living instanceof Mob mob
                && !(living instanceof JobsMasterEntity)
                && !(living instanceof ContractsBoardEntity)
                && !(living instanceof DailyBoardEntity)
                && !(living instanceof StatusBoardEntity)
                && !(living instanceof SalaryBoardEntity)
                && !(living instanceof SkillsBoardEntity)
                && !(living instanceof LeaderboardBoardEntity)
                && !(living instanceof HelpBoardEntity)) {
                mob.setNoAi(true);
                mob.setPersistenceRequired();
            }
            player.sendSystemMessage(Component.translatable(dailyMode
                ? "message.advancedjobs.daily_board_marked"
                : statusMode
                    ? "message.advancedjobs.status_board_marked"
                : salaryMode
                    ? "message.advancedjobs.salary_board_marked"
                : helpMode
                    ? "message.advancedjobs.help_board_marked"
                : skillsMode
                    ? "message.advancedjobs.skills_board_marked"
                : leaderboardMode
                    ? "message.advancedjobs.leaderboard_board_marked"
                : contractsMode
                    ? "message.advancedjobs.contracts_board_marked"
                    : "message.advancedjobs.jobs_npc_marked"));
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }
        if (living instanceof JobsMasterEntity) {
            var profile = jobManager.getOrCreateProfile(player);
            boolean useSecondary = event.getHand() == net.minecraft.world.InteractionHand.OFF_HAND
                && ConfigManager.COMMON.allowSecondaryJob.get()
                && profile.secondaryJobId() != null;
            String targetJobId = useSecondary ? profile.secondaryJobId() : profile.activeJobId();
            if (player.isShiftKeyDown()) {
                jobManager.openScreen(player, "my_job", targetJobId);
            } else {
                jobManager.openScreen(player, "jobs", targetJobId);
            }
            event.setCancellationResult(InteractionResult.CONSUME);
            event.setCanceled(true);
            return;
        }
        if (living instanceof ContractsBoardEntity) {
            jobManager.openScreen(player, "contracts");
            event.setCancellationResult(InteractionResult.CONSUME);
            event.setCanceled(true);
            return;
        }
        if (living instanceof DailyBoardEntity) {
            var profile = jobManager.getOrCreateProfile(player);
            boolean useSecondary = event.getHand() == net.minecraft.world.InteractionHand.OFF_HAND
                && ConfigManager.COMMON.allowSecondaryJob.get()
                && profile.secondaryJobId() != null;
            String targetJobId = useSecondary ? profile.secondaryJobId() : profile.activeJobId();
            if (player.isShiftKeyDown()) {
                jobManager.openScreen(player, "my_job", targetJobId);
            } else {
                jobManager.openScreen(player, "daily", targetJobId);
            }
            event.setCancellationResult(InteractionResult.CONSUME);
            event.setCanceled(true);
            return;
        }
        if (living instanceof StatusBoardEntity) {
            var profile = jobManager.getOrCreateProfile(player);
            boolean useSecondary = event.getHand() == net.minecraft.world.InteractionHand.OFF_HAND
                && ConfigManager.COMMON.allowSecondaryJob.get()
                && profile.secondaryJobId() != null;
            String targetJobId = useSecondary ? profile.secondaryJobId() : profile.activeJobId();
            if (player.isShiftKeyDown()) {
                jobManager.openScreen(player, "jobs", targetJobId);
            } else {
                jobManager.openScreen(player, "my_job", targetJobId);
            }
            event.setCancellationResult(InteractionResult.CONSUME);
            event.setCanceled(true);
            return;
        }
        if (living instanceof SalaryBoardEntity) {
            var profile = jobManager.getOrCreateProfile(player);
            boolean useSecondary = event.getHand() == net.minecraft.world.InteractionHand.OFF_HAND
                && ConfigManager.COMMON.allowSecondaryJob.get()
                && profile.secondaryJobId() != null;
            String targetJobId = useSecondary ? profile.secondaryJobId() : profile.activeJobId();
            if (player.isShiftKeyDown()) {
                if (ConfigManager.COMMON.instantSalary.get()) {
                    player.sendSystemMessage(TextUtil.tr("command.advancedjobs.salary.auto_mode"));
                } else {
                    double paid = jobManager.claimSalary(player);
                    if (paid < 0.0D) {
                        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.salary.cooldown",
                            TimeUtil.formatRemainingSeconds((long) -paid)));
                    } else if (paid > 0.0D) {
                        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.salary.claimed", TextUtil.fmt2(paid)));
                    } else {
                        player.sendSystemMessage(Component.translatable("message.advancedjobs.salary_board_empty"));
                    }
                }
            } else {
                jobManager.openScreen(player, "salary", targetJobId);
            }
            event.setCancellationResult(InteractionResult.CONSUME);
            event.setCanceled(true);
            return;
        }
        if (living instanceof HelpBoardEntity) {
            var profile = jobManager.getOrCreateProfile(player);
            boolean useSecondary = event.getHand() == net.minecraft.world.InteractionHand.OFF_HAND
                && ConfigManager.COMMON.allowSecondaryJob.get()
                && profile.secondaryJobId() != null;
            String targetJobId = useSecondary ? profile.secondaryJobId() : profile.activeJobId();
            if (player.isShiftKeyDown()) {
                jobManager.openScreen(player, "jobs", targetJobId);
            } else {
                jobManager.openScreen(player, "help", targetJobId);
            }
            event.setCancellationResult(InteractionResult.CONSUME);
            event.setCanceled(true);
            return;
        }
        if (living instanceof SkillsBoardEntity) {
            var profile = jobManager.getOrCreateProfile(player);
            boolean useSecondary = event.getHand() == net.minecraft.world.InteractionHand.OFF_HAND
                && ConfigManager.COMMON.allowSecondaryJob.get()
                && profile.secondaryJobId() != null;
            String targetJobId = useSecondary ? profile.secondaryJobId() : profile.activeJobId();
            if (player.isShiftKeyDown()) {
                jobManager.openScreen(player, "my_job", targetJobId);
            } else {
                jobManager.openScreen(player, "skills", targetJobId);
            }
            event.setCancellationResult(InteractionResult.CONSUME);
            event.setCanceled(true);
            return;
        }
        if (living instanceof LeaderboardBoardEntity) {
            var profile = jobManager.getOrCreateProfile(player);
            boolean useSecondary = event.getHand() == net.minecraft.world.InteractionHand.OFF_HAND
                && ConfigManager.COMMON.allowSecondaryJob.get()
                && profile.secondaryJobId() != null;
            String targetJobId = useSecondary ? profile.secondaryJobId() : profile.activeJobId();
            if (player.isShiftKeyDown()) {
                jobManager.openScreen(player, "my_job", targetJobId);
            } else {
                jobManager.openScreen(player, "top", targetJobId);
            }
            event.setCancellationResult(InteractionResult.CONSUME);
            event.setCanceled(true);
            return;
        }
        if (living.getPersistentData().getBoolean(CONTRACTS_BOARD_TAG)) {
            var profile = jobManager.getOrCreateProfile(player);
            boolean useSecondary = event.getHand() == net.minecraft.world.InteractionHand.OFF_HAND
                && ConfigManager.COMMON.allowSecondaryJob.get()
                && profile.secondaryJobId() != null;
            String targetJobId = useSecondary ? profile.secondaryJobId() : profile.activeJobId();
            sendContractsBoardHint(player, profile, useSecondary, targetJobId);
            if (player.isShiftKeyDown()) {
                var preview = jobManager.previewContractReroll(player, targetJobId);
                if (preview == JobManager.ContractRerollResult.SUCCESS && jobManager.rerollContracts(player, targetJobId)) {
                    Component jobName = Component.literal(targetJobId == null ? "none" : targetJobId);
                    var definition = targetJobId == null
                        ? java.util.Optional.<com.example.advancedjobs.model.JobDefinition>empty()
                        : jobManager.job(targetJobId);
                    if (definition.isPresent()) {
                        jobName = Component.translatable(definition.get().translationKey());
                    }
                    player.sendSystemMessage(Component.translatable("message.advancedjobs.contracts_board_rerolled", jobName));
                } else {
                    player.sendSystemMessage(contractRerollFailureMessage(player, profile, preview));
                }
            } else {
                jobManager.openScreen(player, "contracts", targetJobId);
            }
            event.setCancellationResult(InteractionResult.CONSUME);
            event.setCanceled(true);
            return;
        }
        if (living.getPersistentData().getBoolean(DAILY_BOARD_TAG)) {
            var profile = jobManager.getOrCreateProfile(player);
            boolean useSecondary = event.getHand() == net.minecraft.world.InteractionHand.OFF_HAND
                && ConfigManager.COMMON.allowSecondaryJob.get()
                && profile.secondaryJobId() != null;
            String targetJobId = useSecondary ? profile.secondaryJobId() : profile.activeJobId();
            sendDailyBoardHint(player, profile, useSecondary, targetJobId);
            if (player.isShiftKeyDown()) {
                jobManager.openScreen(player, "my_job", targetJobId);
            } else {
                jobManager.openScreen(player, "daily", targetJobId);
            }
            event.setCancellationResult(InteractionResult.CONSUME);
            event.setCanceled(true);
            return;
        }
        if (living.getPersistentData().getBoolean(STATUS_BOARD_TAG)) {
            var profile = jobManager.getOrCreateProfile(player);
            boolean useSecondary = event.getHand() == net.minecraft.world.InteractionHand.OFF_HAND
                && ConfigManager.COMMON.allowSecondaryJob.get()
                && profile.secondaryJobId() != null;
            String targetJobId = useSecondary ? profile.secondaryJobId() : profile.activeJobId();
            sendStatusBoardHint(player, profile, useSecondary, targetJobId);
            if (player.isShiftKeyDown()) {
                jobManager.openScreen(player, "jobs", targetJobId);
            } else {
                jobManager.openScreen(player, "my_job", targetJobId);
            }
            event.setCancellationResult(InteractionResult.CONSUME);
            event.setCanceled(true);
            return;
        }
        if (living.getPersistentData().getBoolean(SALARY_BOARD_TAG)) {
            var profile = jobManager.getOrCreateProfile(player);
            boolean useSecondary = event.getHand() == net.minecraft.world.InteractionHand.OFF_HAND
                && ConfigManager.COMMON.allowSecondaryJob.get()
                && profile.secondaryJobId() != null;
            String targetJobId = useSecondary ? profile.secondaryJobId() : profile.activeJobId();
            sendSalaryBoardHint(player, profile, useSecondary, targetJobId);
            if (player.isShiftKeyDown()) {
                if (ConfigManager.COMMON.instantSalary.get()) {
                    player.sendSystemMessage(TextUtil.tr("command.advancedjobs.salary.auto_mode"));
                } else {
                    double paid = jobManager.claimSalary(player);
                    if (paid < 0.0D) {
                        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.salary.cooldown",
                            TimeUtil.formatRemainingSeconds((long) -paid)));
                    } else if (paid > 0.0D) {
                        player.sendSystemMessage(TextUtil.tr("command.advancedjobs.salary.claimed", TextUtil.fmt2(paid)));
                    } else {
                        player.sendSystemMessage(Component.translatable("message.advancedjobs.salary_board_empty"));
                    }
                }
            } else {
                jobManager.openScreen(player, "salary", targetJobId);
            }
            event.setCancellationResult(InteractionResult.CONSUME);
            event.setCanceled(true);
            return;
        }
        if (living.getPersistentData().getBoolean(HELP_BOARD_TAG)) {
            var profile = jobManager.getOrCreateProfile(player);
            boolean useSecondary = event.getHand() == net.minecraft.world.InteractionHand.OFF_HAND
                && ConfigManager.COMMON.allowSecondaryJob.get()
                && profile.secondaryJobId() != null;
            String targetJobId = useSecondary ? profile.secondaryJobId() : profile.activeJobId();
            sendHelpBoardHint(player, profile, useSecondary, targetJobId);
            if (player.isShiftKeyDown()) {
                jobManager.openScreen(player, "jobs", targetJobId);
            } else {
                jobManager.openScreen(player, "help", targetJobId);
            }
            event.setCancellationResult(InteractionResult.CONSUME);
            event.setCanceled(true);
            return;
        }
        if (living.getPersistentData().getBoolean(SKILLS_BOARD_TAG)) {
            var profile = jobManager.getOrCreateProfile(player);
            boolean useSecondary = event.getHand() == net.minecraft.world.InteractionHand.OFF_HAND
                && ConfigManager.COMMON.allowSecondaryJob.get()
                && profile.secondaryJobId() != null;
            String targetJobId = useSecondary ? profile.secondaryJobId() : profile.activeJobId();
            sendSkillsBoardHint(player, profile, useSecondary, targetJobId);
            if (player.isShiftKeyDown()) {
                jobManager.openScreen(player, "my_job", targetJobId);
            } else {
                jobManager.openScreen(player, "skills", targetJobId);
            }
            event.setCancellationResult(InteractionResult.CONSUME);
            event.setCanceled(true);
            return;
        }
        if (living.getPersistentData().getBoolean(LEADERBOARD_BOARD_TAG)) {
            var profile = jobManager.getOrCreateProfile(player);
            boolean useSecondary = event.getHand() == net.minecraft.world.InteractionHand.OFF_HAND
                && ConfigManager.COMMON.allowSecondaryJob.get()
                && profile.secondaryJobId() != null;
            String targetJobId = useSecondary ? profile.secondaryJobId() : profile.activeJobId();
            sendLeaderboardBoardHint(player, profile, useSecondary, targetJobId);
            if (player.isShiftKeyDown()) {
                jobManager.openScreen(player, "my_job", targetJobId);
            } else {
                jobManager.openScreen(player, "top", targetJobId);
            }
            event.setCancellationResult(InteractionResult.CONSUME);
            event.setCanceled(true);
            return;
        }
        if (living.getPersistentData().getBoolean(JOB_NPC_TAG)) {
            var profile = jobManager.getOrCreateProfile(player);
            boolean useSecondary = event.getHand() == net.minecraft.world.InteractionHand.OFF_HAND
                && ConfigManager.COMMON.allowSecondaryJob.get()
                && profile.secondaryJobId() != null;
            String targetJobId = useSecondary ? profile.secondaryJobId() : profile.activeJobId();
            sendJobsMasterHint(player, profile, useSecondary, targetJobId);
            if (player.isShiftKeyDown()) {
                jobManager.openScreen(player, "my_job", targetJobId);
            } else {
                jobManager.openScreen(player, "jobs", targetJobId);
            }
            event.setCancellationResult(InteractionResult.CONSUME);
            event.setCanceled(true);
            return;
        }
        if (living instanceof Villager villager) {
            ResourceLocation id = new ResourceLocation("minecraft", "villager");
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
        if (state.is(Blocks.ENCHANTING_TABLE)) {
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
        } else if (state.is(Blocks.BREWING_STAND)) {
            if (antiExploit.isOnCooldown(player.getUUID(), "brew_cd|minecraft:potion")) {
                DebugLog.log("Blocked by cooldown: player=" + player.getGameProfile().getName() + " action=brew target=minecraft:potion");
                return;
            }
            rewards.reward(new JobActionContext(player, JobActionType.BREW_POTION, new ResourceLocation("minecraft", "potion"), player.serverLevel(), event.getPos(), false));
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
        } else if (isLootContainer(state)) {
            ResourceLocation id = state.is(Blocks.BARREL) ? new ResourceLocation("minecraft", "barrel") : new ResourceLocation("minecraft", "chest");
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
        } else if (isRedstoneComponent(state)) {
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
            if (id.equals(new ResourceLocation("minecraft", "cow")) && player.getRandom().nextDouble() < 0.16D) {
                ItemStack milkFood = new ItemStack(Items.LEATHER);
                if (!player.addItem(milkFood)) {
                    player.drop(milkFood, false);
                }
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 120, 0, true, false));
            } else if (id.equals(new ResourceLocation("minecraft", "chicken")) && player.getRandom().nextDouble() < 0.16D) {
                ItemStack eggs = new ItemStack(Items.EGG);
                if (!player.addItem(eggs)) {
                    player.drop(eggs, false);
                }
            } else if (id.equals(new ResourceLocation("minecraft", "pig")) && player.getRandom().nextDouble() < 0.14D) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 140, 0, true, false));
            }
            if (id.equals(new ResourceLocation("minecraft", "chicken")) && player.getRandom().nextDouble() < 0.12D) {
                ItemStack feathers = new ItemStack(Items.FEATHER, 2);
                if (!player.addItem(feathers)) {
                    player.drop(feathers, false);
                }
            }
        }
        if (jobManager.hasAssignedJob(player, "shepherd")
            && (jobManager.effectBonus(player, "pasture_aura") >= 0.10D || jobManager.effectBonus(player, "breed_bonus") >= 0.10D)
            && id.equals(new ResourceLocation("minecraft", "sheep"))
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
            ItemStack beeReward = id.equals(new ResourceLocation("minecraft", "bee"))
                ? new ItemStack(Items.HONEYCOMB, 2)
                : new ItemStack(Items.HONEY_BOTTLE);
            if (!player.addItem(beeReward)) {
                player.drop(beeReward, false);
            }
            if (id.equals(new ResourceLocation("minecraft", "bee"))) {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 0, true, false));
            }
            if (id.equals(new ResourceLocation("minecraft", "bee")) && player.getRandom().nextDouble() < 0.10D) {
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
                JobActionContext context = new JobActionContext(player, JobActionType.EXPLORE_CHUNK, new ResourceLocation("minecraft", "chunk"), player.serverLevel(), player.blockPosition(), false);
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
            && (targetId.equals(new ResourceLocation("minecraft", "pillager")) || targetId.equals(new ResourceLocation("minecraft", "vindicator")))
            && (combatBonus >= 0.10D || jobManager.effectBonus(player, "combat_aura") >= 0.10D)) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 0, true, false));
            player.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 160, 0, true, false));
            if (player.getRandom().nextDouble() < 0.14D) {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, true, false));
            }
            if (player.getRandom().nextDouble() < 0.16D) {
                net.minecraft.world.level.block.Block.popResource(player.serverLevel(), pos, new ItemStack(Items.SHIELD));
            }
            if (targetId.equals(new ResourceLocation("minecraft", "vindicator")) && player.getRandom().nextDouble() < 0.12D) {
                net.minecraft.world.level.block.Block.popResource(player.serverLevel(), pos, new ItemStack(Items.IRON_INGOT));
            } else if (targetId.equals(new ResourceLocation("minecraft", "pillager")) && player.getRandom().nextDouble() < 0.12D) {
                net.minecraft.world.level.block.Block.popResource(player.serverLevel(), pos, new ItemStack(Items.CROSSBOW));
            }
        } else if ("bounty_hunter".equals(jobId)
            && (targetId.equals(new ResourceLocation("minecraft", "evoker"))
            || targetId.equals(new ResourceLocation("minecraft", "blaze"))
            || targetId.equals(new ResourceLocation("minecraft", "witch")))
            && (combatBonus >= 0.10D || jobManager.effectBonus(player, "elite_tracker") > 0.0D)) {
            if (player.getRandom().nextDouble() < 0.18D) {
                net.minecraft.world.level.block.Block.popResource(player.serverLevel(), pos, new ItemStack(Items.EMERALD, 2));
            }
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 0, true, false));
            if (targetId.equals(new ResourceLocation("minecraft", "blaze"))) {
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 140, 0, true, false));
                if (player.getRandom().nextDouble() < 0.12D) {
                    net.minecraft.world.level.block.Block.popResource(player.serverLevel(), pos, new ItemStack(Items.BLAZE_POWDER, 2));
                }
            } else if (targetId.equals(new ResourceLocation("minecraft", "witch"))) {
                player.addEffect(new MobEffectInstance(MobEffects.LUCK, 180, 0, true, false));
                if (player.getRandom().nextDouble() < 0.16D) {
                    net.minecraft.world.level.block.Block.popResource(player.serverLevel(), pos, new ItemStack(Items.GLASS_BOTTLE, 2));
                }
            } else {
                player.addEffect(new MobEffectInstance(MobEffects.LUCK, 140, 0, true, false));
            }
        } else if ("defender".equals(jobId)
            && (targetId.equals(new ResourceLocation("minecraft", "zombie")) || targetId.equals(new ResourceLocation("minecraft", "husk")))
            && (combatBonus >= 0.10D || combatAura >= 0.10D)) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 140, 0, true, false));
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 120, 0, true, false));
            if (player.getRandom().nextDouble() < 0.16D) {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, true, false));
            }
            if (player.getRandom().nextDouble() < 0.20D) {
                net.minecraft.world.level.block.Block.popResource(player.serverLevel(), pos, new ItemStack(Items.IRON_NUGGET, 3));
            }
            if (targetId.equals(new ResourceLocation("minecraft", "husk")) && player.getRandom().nextDouble() < 0.12D) {
                net.minecraft.world.level.block.Block.popResource(player.serverLevel(), pos, new ItemStack(Items.ROTTEN_FLESH, 2));
            } else if (targetId.equals(new ResourceLocation("minecraft", "zombie")) && player.getRandom().nextDouble() < 0.12D) {
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

    private boolean isLootContainer(BlockState state) {
        return state.getBlock() instanceof ChestBlock || state.is(Blocks.BARREL) || state.is(Blocks.TRAPPED_CHEST);
    }

    private Component contractRerollFailureMessage(ServerPlayer player, com.example.advancedjobs.model.PlayerJobProfile profile,
                                                   JobManager.ContractRerollResult result) {
        return switch (result) {
            case NO_JOB, NOT_ASSIGNED -> Component.translatable("message.advancedjobs.contracts_board_reroll_no_job");
            case COOLDOWN -> Component.translatable("message.advancedjobs.contracts_board_reroll_cooldown",
                TimeUtil.formatRemainingSeconds(jobManager.contractRerollCooldownRemaining(profile)));
            case INSUFFICIENT_FUNDS -> Component.translatable("message.advancedjobs.contracts_board_reroll_insufficient_funds",
                TextUtil.fmt2(ConfigManager.COMMON.contractRerollPrice.get()));
            case SUCCESS -> Component.translatable("message.advancedjobs.contracts_board_rerolled", player.getDisplayName());
        };
    }

    private void sendJobsMasterHint(ServerPlayer player, com.example.advancedjobs.model.PlayerJobProfile profile, boolean secondary, String targetJobId) {
        final int radius = 16;
        player.sendSystemMessage(Component.translatable("message.advancedjobs.jobs_master.slot_hint",
            Component.translatable(secondary ? "command.advancedjobs.common.slot.secondary" : "command.advancedjobs.common.slot.primary")));
        if (targetJobId == null) {
            player.sendSystemMessage(Component.translatable("message.advancedjobs.jobs_master.no_job"));
        } else {
            player.sendSystemMessage(Component.translatable("message.advancedjobs.jobs_master.job_hint",
                Component.translatable("job.advancedjobs." + targetJobId)));
        }
        long remaining = Math.max(0L, ConfigManager.COMMON.jobChangeCooldownSeconds.get() - (TimeUtil.now() - profile.lastJobChangeEpochSecond()));
        player.sendSystemMessage(Component.translatable("message.advancedjobs.jobs_master.change_hint",
            ConfigManager.COMMON.jobChangePrice.get(),
            TimeUtil.formatRemainingSeconds(remaining)));
        NpcRole nextRole = recommendedRole(profile, secondary);
        if (nextRole != NpcRole.JOBS_MASTER) {
            player.sendSystemMessage(Component.translatable("message.advancedjobs.jobs_master.next_step",
                Component.translatable(nextRole.translationKey()),
                guideReason(profile, nextRole, secondary)));
            int nextRoleCount = nearbyBoardCount(player, nextRole, radius);
            if (nextRoleCount > 0) {
                String nextRoleSource = nearestSource(player, nextRole, radius);
                player.sendSystemMessage(Component.translatable("message.advancedjobs.route.next_step_available",
                    Component.translatable(nextRole.translationKey()), nextRoleCount, radius, nextRoleSource));
            } else {
                player.sendSystemMessage(Component.translatable("message.advancedjobs.route.next_step_missing",
                    Component.translatable(nextRole.translationKey()), radius));
            }
        }
    }

    private void sendDailyBoardHint(ServerPlayer player, com.example.advancedjobs.model.PlayerJobProfile profile, boolean secondary, String targetJobId) {
        final int radius = 16;
        player.sendSystemMessage(Component.translatable("message.advancedjobs.daily_board.slot_hint",
            Component.translatable(secondary ? "command.advancedjobs.common.slot.secondary" : "command.advancedjobs.common.slot.primary")));
        if (targetJobId == null) {
            player.sendSystemMessage(Component.translatable("message.advancedjobs.daily_board.no_job"));
            return;
        }
        var dailies = profile.progress(targetJobId).dailyTasks();
        String resetAt = dailies.isEmpty() ? "--" : TimeUtil.formatRemainingSeconds(Math.max(0L, dailies.get(0).resetEpochSecond() - TimeUtil.now()));
        player.sendSystemMessage(Component.translatable("message.advancedjobs.daily_board.job_hint",
            Component.translatable("job.advancedjobs." + targetJobId), dailies.size(), resetAt));
        NpcRole nextRole = followUpRoleAfterDaily(profile, secondary);
        player.sendSystemMessage(Component.translatable("message.advancedjobs.daily_board.next_step",
            Component.translatable(nextRole.translationKey()),
            guideReason(profile, nextRole, secondary)));
        int nextRoleCount = nearbyBoardCount(player, nextRole, radius);
        if (nextRoleCount > 0) {
            String nextRoleSource = nearestSource(player, nextRole, radius);
            player.sendSystemMessage(Component.translatable("message.advancedjobs.route.next_step_available",
                Component.translatable(nextRole.translationKey()), nextRoleCount, radius, nextRoleSource));
        } else {
            player.sendSystemMessage(Component.translatable("message.advancedjobs.route.next_step_missing",
                Component.translatable(nextRole.translationKey()), radius));
        }
    }

    private void sendStatusBoardHint(ServerPlayer player, com.example.advancedjobs.model.PlayerJobProfile profile, boolean secondary, String targetJobId) {
        final int radius = 16;
        player.sendSystemMessage(Component.translatable("message.advancedjobs.status_board.slot_hint",
            Component.translatable(secondary ? "command.advancedjobs.common.slot.secondary" : "command.advancedjobs.common.slot.primary")));
        if (targetJobId == null) {
            player.sendSystemMessage(Component.translatable("message.advancedjobs.status_board.no_job"));
            return;
        }
        var progress = profile.progress(targetJobId);
        String latestTitle = profile.unlockedTitles().isEmpty() ? "-" : profile.unlockedTitles().stream().reduce((a, b) -> b).orElse("-");
        String latestMilestone = progress.unlockedMilestones().isEmpty() ? "-" : progress.unlockedMilestones().get(progress.unlockedMilestones().size() - 1);
        player.sendSystemMessage(Component.translatable("message.advancedjobs.status_board.job_hint",
            Component.translatable("job.advancedjobs." + targetJobId), progress.level(), progress.unlockedMilestones().size(), latestMilestone, latestTitle));
        NpcRole nextRole = recommendedRole(profile, secondary);
        if (nextRole != NpcRole.STATUS_BOARD) {
            player.sendSystemMessage(Component.translatable("message.advancedjobs.status_board.next_step",
                Component.translatable(nextRole.translationKey()),
                guideReason(profile, nextRole, secondary)));
            int nextRoleCount = nearbyBoardCount(player, nextRole, radius);
            if (nextRoleCount > 0) {
                String nextRoleSource = nearestSource(player, nextRole, radius);
                player.sendSystemMessage(Component.translatable("message.advancedjobs.route.next_step_available",
                    Component.translatable(nextRole.translationKey()), nextRoleCount, radius, nextRoleSource));
            } else {
                player.sendSystemMessage(Component.translatable("message.advancedjobs.route.next_step_missing",
                    Component.translatable(nextRole.translationKey()), radius));
            }
        }
    }

    private void sendSalaryBoardHint(ServerPlayer player, com.example.advancedjobs.model.PlayerJobProfile profile, boolean secondary, String targetJobId) {
        final int radius = 16;
        player.sendSystemMessage(Component.translatable("message.advancedjobs.salary_board.slot_hint",
            Component.translatable(secondary ? "command.advancedjobs.common.slot.secondary" : "command.advancedjobs.common.slot.primary")));
        if (targetJobId == null) {
            player.sendSystemMessage(Component.translatable("message.advancedjobs.salary_board.no_job"));
        } else {
            player.sendSystemMessage(Component.translatable("message.advancedjobs.salary_board.job_hint",
                Component.translatable("job.advancedjobs." + targetJobId),
                TextUtil.fmt2(profile.progress(targetJobId).pendingSalary())));
        }
        player.sendSystemMessage(Component.translatable("message.advancedjobs.salary_board.mode_hint",
            Component.translatable(ConfigManager.COMMON.instantSalary.get() ? "gui.advancedjobs.salary_mode.instant" : "gui.advancedjobs.salary_mode.manual")));
        player.sendSystemMessage(Component.translatable("message.advancedjobs.salary_board.cooldown_hint",
            TimeUtil.formatRemainingSeconds(jobManager.salaryClaimCooldownRemaining(profile))));
        NpcRole nextRole = followUpRoleAfterSalary(profile, secondary);
        if (nextRole != NpcRole.SALARY_BOARD) {
            player.sendSystemMessage(Component.translatable("message.advancedjobs.salary_board.next_step",
                Component.translatable(nextRole.translationKey()),
                guideReason(profile, nextRole, secondary)));
            int nextRoleCount = nearbyBoardCount(player, nextRole, radius);
            if (nextRoleCount > 0) {
                String nextRoleSource = nearestSource(player, nextRole, radius);
                player.sendSystemMessage(Component.translatable("message.advancedjobs.route.next_step_available",
                    Component.translatable(nextRole.translationKey()), nextRoleCount, radius, nextRoleSource));
            } else {
                player.sendSystemMessage(Component.translatable("message.advancedjobs.route.next_step_missing",
                    Component.translatable(nextRole.translationKey()), radius));
            }
        }
    }

    private void sendHelpBoardHint(ServerPlayer player, com.example.advancedjobs.model.PlayerJobProfile profile, boolean secondary, String targetJobId) {
        final int radius = 16;
        player.sendSystemMessage(Component.translatable("message.advancedjobs.help_board.slot_hint",
            Component.translatable(secondary ? "command.advancedjobs.common.slot.secondary" : "command.advancedjobs.common.slot.primary")));
        if (targetJobId == null) {
            player.sendSystemMessage(Component.translatable("message.advancedjobs.help_board.no_job"));
        } else {
            player.sendSystemMessage(Component.translatable("message.advancedjobs.help_board.job_hint",
                Component.translatable("job.advancedjobs." + targetJobId)));
        }
        player.sendSystemMessage(Component.translatable("message.advancedjobs.help_board.salary_hint",
            Component.translatable(ConfigManager.COMMON.instantSalary.get() ? "gui.advancedjobs.salary_mode.instant" : "gui.advancedjobs.salary_mode.manual")));
        NpcRole nextRole = recommendedRole(profile, secondary);
        player.sendSystemMessage(Component.translatable("message.advancedjobs.help_board.next_step",
            Component.translatable(nextRole.translationKey()),
            guideReason(profile, nextRole, secondary)));
        int nextRoleCount = nearbyBoardCount(player, nextRole, radius);
        if (nextRoleCount <= 0) {
            player.sendSystemMessage(Component.translatable("message.advancedjobs.help_board.next_step_missing",
                Component.translatable(nextRole.translationKey()),
                radius));
        } else {
            String nextRoleSource = nearestSource(player, nextRole, radius);
            player.sendSystemMessage(Component.translatable("message.advancedjobs.help_board.next_step_available",
                Component.translatable(nextRole.translationKey()),
                nextRoleCount,
                radius,
                nextRoleSource));
        }
        NpcRole followUpRole = secondaryRecommendedRole(profile, nextRole);
        if (followUpRole != null) {
            int followUpCount = nearbyBoardCount(player, followUpRole, radius);
            if (followUpCount <= 0) {
                player.sendSystemMessage(Component.translatable("message.advancedjobs.help_board.follow_up_missing",
                    Component.translatable(followUpRole.translationKey()),
                    radius));
            } else {
                String followUpSource = nearestSource(player, followUpRole, radius);
                player.sendSystemMessage(Component.translatable("message.advancedjobs.help_board.follow_up_available",
                    Component.translatable(followUpRole.translationKey()),
                    followUpCount,
                    radius,
                    followUpSource));
            }
        }
        player.sendSystemMessage(Component.translatable("message.advancedjobs.help_board.hub_hint",
            nearbyBoardCount(player, NpcRole.JOBS_MASTER, radius),
            nearbyBoardCount(player, NpcRole.DAILY_BOARD, radius),
            nearbyBoardCount(player, NpcRole.CONTRACTS_BOARD, radius),
            nearbyBoardCount(player, NpcRole.SALARY_BOARD, radius),
            nearbyBoardCount(player, NpcRole.SKILLS_BOARD, radius),
            nearbyBoardCount(player, NpcRole.LEADERBOARD_BOARD, radius),
            nearbyBoardCount(player, NpcRole.STATUS_BOARD, radius)));
    }

    private void sendSkillsBoardHint(ServerPlayer player, com.example.advancedjobs.model.PlayerJobProfile profile, boolean secondary, String targetJobId) {
        final int radius = 16;
        player.sendSystemMessage(Component.translatable("message.advancedjobs.skills_board.slot_hint",
            Component.translatable(secondary ? "command.advancedjobs.common.slot.secondary" : "command.advancedjobs.common.slot.primary")));
        if (targetJobId == null) {
            player.sendSystemMessage(Component.translatable("message.advancedjobs.skills_board.no_job"));
            return;
        }
        var progress = profile.progress(targetJobId);
        player.sendSystemMessage(Component.translatable("message.advancedjobs.skills_board.job_hint",
            Component.translatable("job.advancedjobs." + targetJobId),
            profile.availableSkillPoints(targetJobId),
            progress.unlockedNodes().size()));
        NpcRole nextRole = followUpRoleAfterSkills(profile, secondary);
        player.sendSystemMessage(Component.translatable("message.advancedjobs.skills_board.next_step",
            Component.translatable(nextRole.translationKey()),
            guideReason(profile, nextRole, secondary)));
        int nextRoleCount = nearbyBoardCount(player, nextRole, radius);
        if (nextRoleCount > 0) {
            String nextRoleSource = nearestSource(player, nextRole, radius);
            player.sendSystemMessage(Component.translatable("message.advancedjobs.route.next_step_available",
                Component.translatable(nextRole.translationKey()), nextRoleCount, radius, nextRoleSource));
        } else {
            player.sendSystemMessage(Component.translatable("message.advancedjobs.route.next_step_missing",
                Component.translatable(nextRole.translationKey()), radius));
        }
    }

    private void sendLeaderboardBoardHint(ServerPlayer player, com.example.advancedjobs.model.PlayerJobProfile profile, boolean secondary, String targetJobId) {
        final int radius = 16;
        player.sendSystemMessage(Component.translatable("message.advancedjobs.leaderboard_board.slot_hint",
            Component.translatable(secondary ? "command.advancedjobs.common.slot.secondary" : "command.advancedjobs.common.slot.primary")));
        if (targetJobId == null) {
            var entries = jobManager.overallLeaderboard();
            String leader = entries.isEmpty() ? "-" : entries.iterator().next().playerName();
            player.sendSystemMessage(Component.translatable("message.advancedjobs.leaderboard_board.overall_hint", entries.size(), leader));
            NpcRole nextRole = recommendedRole(profile, secondary);
            player.sendSystemMessage(Component.translatable("message.advancedjobs.leaderboard_board.next_step",
                Component.translatable(nextRole.translationKey()),
                guideReason(profile, nextRole, secondary)));
            int nextRoleCount = nearbyBoardCount(player, nextRole, radius);
            if (nextRoleCount > 0) {
                String nextRoleSource = nearestSource(player, nextRole, radius);
                player.sendSystemMessage(Component.translatable("message.advancedjobs.route.next_step_available",
                    Component.translatable(nextRole.translationKey()), nextRoleCount, radius, nextRoleSource));
            } else {
                player.sendSystemMessage(Component.translatable("message.advancedjobs.route.next_step_missing",
                    Component.translatable(nextRole.translationKey()), radius));
            }
            return;
        }
        var entries = jobManager.leaderboard(targetJobId);
        String leader = entries.isEmpty() ? "-" : entries.iterator().next().playerName();
        player.sendSystemMessage(Component.translatable("message.advancedjobs.leaderboard_board.job_hint",
            Component.translatable("job.advancedjobs." + targetJobId), entries.size(), leader));
        NpcRole nextRole = recommendedRole(profile, secondary);
        player.sendSystemMessage(Component.translatable("message.advancedjobs.leaderboard_board.next_step",
            Component.translatable(nextRole.translationKey()),
            guideReason(profile, nextRole, secondary)));
        int nextRoleCount = nearbyBoardCount(player, nextRole, radius);
        if (nextRoleCount > 0) {
            String nextRoleSource = nearestSource(player, nextRole, radius);
            player.sendSystemMessage(Component.translatable("message.advancedjobs.route.next_step_available",
                Component.translatable(nextRole.translationKey()), nextRoleCount, radius, nextRoleSource));
        } else {
            player.sendSystemMessage(Component.translatable("message.advancedjobs.route.next_step_missing",
                Component.translatable(nextRole.translationKey()), radius));
        }
    }

    private void sendContractsBoardHint(ServerPlayer player, com.example.advancedjobs.model.PlayerJobProfile profile, boolean secondary, String targetJobId) {
        final int radius = 16;
        player.sendSystemMessage(Component.translatable("message.advancedjobs.contracts_board.slot_hint",
            Component.translatable(secondary ? "command.advancedjobs.common.slot.secondary" : "command.advancedjobs.common.slot.primary")));
        if (targetJobId == null) {
            player.sendSystemMessage(Component.translatable("message.advancedjobs.contracts_board.no_job"));
        } else {
            player.sendSystemMessage(Component.translatable("message.advancedjobs.contracts_board.job_hint",
                Component.translatable("job.advancedjobs." + targetJobId),
                profile.progress(targetJobId).contracts().size()));
        }
        player.sendSystemMessage(Component.translatable("message.advancedjobs.contracts_board.reroll_hint",
            TextUtil.fmt2(ConfigManager.COMMON.contractRerollPrice.get()),
            TimeUtil.formatRemainingSeconds(jobManager.contractRerollCooldownRemaining(profile))));
        NpcRole nextRole = followUpRoleAfterContracts(profile, secondary);
        player.sendSystemMessage(Component.translatable("message.advancedjobs.contracts_board.next_step",
            Component.translatable(nextRole.translationKey()),
            guideReason(profile, nextRole, secondary)));
        int nextRoleCount = nearbyBoardCount(player, nextRole, radius);
        if (nextRoleCount > 0) {
            String nextRoleSource = nearestSource(player, nextRole, radius);
            player.sendSystemMessage(Component.translatable("message.advancedjobs.route.next_step_available",
                Component.translatable(nextRole.translationKey()), nextRoleCount, radius, nextRoleSource));
        } else {
            player.sendSystemMessage(Component.translatable("message.advancedjobs.route.next_step_missing",
                Component.translatable(nextRole.translationKey()), radius));
        }
    }

    private NpcRole recommendedRole(com.example.advancedjobs.model.PlayerJobProfile profile, boolean secondary) {
        String jobId = secondary ? profile.secondaryJobId() : profile.activeJobId();
        if (jobId == null) {
            return NpcRole.JOBS_MASTER;
        }
        var progress = profile.progress(jobId);
        if (!ConfigManager.COMMON.instantSalary.get() && progress.pendingSalary() > 0.0D) {
            return NpcRole.SALARY_BOARD;
        }
        if (progress.dailyTasks().stream().anyMatch(task -> !task.completed())) {
            return NpcRole.DAILY_BOARD;
        }
        if (progress.contracts().stream().anyMatch(task -> !task.completed())) {
            return NpcRole.CONTRACTS_BOARD;
        }
        if (profile.availableSkillPoints(jobId) > 0) {
            return NpcRole.SKILLS_BOARD;
        }
        return NpcRole.STATUS_BOARD;
    }

    private NpcRole secondaryRecommendedRole(com.example.advancedjobs.model.PlayerJobProfile profile, NpcRole primary) {
        if (primary != NpcRole.LEADERBOARD_BOARD && profile.activeJobId() != null) {
            return NpcRole.LEADERBOARD_BOARD;
        }
        if (primary != NpcRole.HELP_BOARD) {
            return NpcRole.HELP_BOARD;
        }
        return null;
    }

    private NpcRole followUpRoleAfterDaily(com.example.advancedjobs.model.PlayerJobProfile profile, boolean secondary) {
        String jobId = secondary ? profile.secondaryJobId() : profile.activeJobId();
        if (jobId == null) {
            return NpcRole.JOBS_MASTER;
        }
        var progress = profile.progress(jobId);
        if (progress.contracts().stream().anyMatch(task -> !task.completed())) {
            return NpcRole.CONTRACTS_BOARD;
        }
        if (profile.availableSkillPoints(jobId) > 0) {
            return NpcRole.SKILLS_BOARD;
        }
        if (!ConfigManager.COMMON.instantSalary.get() && progress.pendingSalary() > 0.0D) {
            return NpcRole.SALARY_BOARD;
        }
        return NpcRole.STATUS_BOARD;
    }

    private NpcRole followUpRoleAfterSkills(com.example.advancedjobs.model.PlayerJobProfile profile, boolean secondary) {
        String jobId = secondary ? profile.secondaryJobId() : profile.activeJobId();
        if (jobId == null) {
            return NpcRole.JOBS_MASTER;
        }
        var progress = profile.progress(jobId);
        if (!ConfigManager.COMMON.instantSalary.get() && progress.pendingSalary() > 0.0D) {
            return NpcRole.SALARY_BOARD;
        }
        if (progress.dailyTasks().stream().anyMatch(task -> !task.completed())) {
            return NpcRole.DAILY_BOARD;
        }
        if (progress.contracts().stream().anyMatch(task -> !task.completed())) {
            return NpcRole.CONTRACTS_BOARD;
        }
        return NpcRole.STATUS_BOARD;
    }

    private NpcRole followUpRoleAfterSalary(com.example.advancedjobs.model.PlayerJobProfile profile, boolean secondary) {
        return recommendedRole(profile, secondary);
    }

    private NpcRole followUpRoleAfterContracts(com.example.advancedjobs.model.PlayerJobProfile profile, boolean secondary) {
        String jobId = secondary ? profile.secondaryJobId() : profile.activeJobId();
        if (jobId == null) {
            return NpcRole.JOBS_MASTER;
        }
        var progress = profile.progress(jobId);
        if (profile.availableSkillPoints(jobId) > 0) {
            return NpcRole.SKILLS_BOARD;
        }
        if (!ConfigManager.COMMON.instantSalary.get() && progress.pendingSalary() > 0.0D) {
            return NpcRole.SALARY_BOARD;
        }
        return NpcRole.STATUS_BOARD;
    }

    private String guideReason(com.example.advancedjobs.model.PlayerJobProfile profile, NpcRole role, boolean secondary) {
        return switch (role) {
            case JOBS_MASTER -> TextUtil.tr("command.advancedjobs.guide.reason.jobs_master",
                Component.translatable(secondary ? "command.advancedjobs.common.slot.secondary" : "command.advancedjobs.common.slot.primary")).getString();
            case SALARY_BOARD -> TextUtil.tr("command.advancedjobs.guide.reason.salary",
                TextUtil.fmt2(pendingSalaryForSlot(profile, secondary))).getString();
            case DAILY_BOARD -> TextUtil.tr("command.advancedjobs.guide.reason.daily",
                Component.translatable(secondary ? "command.advancedjobs.common.slot.secondary" : "command.advancedjobs.common.slot.primary")).getString();
            case CONTRACTS_BOARD -> TextUtil.tr("command.advancedjobs.guide.reason.contracts",
                Component.translatable(secondary ? "command.advancedjobs.common.slot.secondary" : "command.advancedjobs.common.slot.primary")).getString();
            case SKILLS_BOARD -> TextUtil.tr("command.advancedjobs.guide.reason.skills",
                Component.translatable(secondary ? "command.advancedjobs.common.slot.secondary" : "command.advancedjobs.common.slot.primary")).getString();
            case STATUS_BOARD -> TextUtil.tr("command.advancedjobs.guide.reason.status",
                Component.translatable(secondary ? "command.advancedjobs.common.slot.secondary" : "command.advancedjobs.common.slot.primary")).getString();
            default -> null;
        };
    }

    private double pendingSalaryForSlot(com.example.advancedjobs.model.PlayerJobProfile profile, boolean secondary) {
        String jobId = secondary ? profile.secondaryJobId() : profile.activeJobId();
        return jobId == null ? 0.0D : profile.progress(jobId).pendingSalary();
    }

    private int nearbyBoardCount(ServerPlayer player, NpcRole role, double radius) {
        var box = player.getBoundingBox().inflate(radius);
        int count = 0;
        for (var entity : player.serverLevel().getEntities(player, box)) {
            if (!(entity instanceof Mob mob)) {
                continue;
            }
            if (matchesRole(mob, role)) {
                count++;
            }
        }
        return count;
    }

    private String nearestSource(ServerPlayer player, NpcRole role, double radius) {
        var box = player.getBoundingBox().inflate(radius);
        double nearest = Double.MAX_VALUE;
        String nearestSource = "-";
        for (var entity : player.serverLevel().getEntities(player, box)) {
            if (!(entity instanceof Mob mob) || !matchesRole(mob, role)) {
                continue;
            }
            double distance = mob.distanceToSqr(player);
            if (distance < nearest) {
                nearest = distance;
                nearestSource = mob.getType() == nativeType(role) ? "native" : "wand";
            }
        }
        return nearestSource;
    }

    private net.minecraft.world.entity.EntityType<?> nativeType(NpcRole role) {
        return switch (role) {
            case JOBS_MASTER -> com.example.advancedjobs.content.ModEntities.JOBS_MASTER.get();
            case DAILY_BOARD -> com.example.advancedjobs.content.ModEntities.DAILY_BOARD.get();
            case STATUS_BOARD -> com.example.advancedjobs.content.ModEntities.STATUS_BOARD.get();
            case CONTRACTS_BOARD -> com.example.advancedjobs.content.ModEntities.CONTRACTS_BOARD.get();
            case SALARY_BOARD -> com.example.advancedjobs.content.ModEntities.SALARY_BOARD.get();
            case SKILLS_BOARD -> com.example.advancedjobs.content.ModEntities.SKILLS_BOARD.get();
            case LEADERBOARD_BOARD -> com.example.advancedjobs.content.ModEntities.LEADERBOARD_BOARD.get();
            case HELP_BOARD -> com.example.advancedjobs.content.ModEntities.HELP_BOARD.get();
        };
    }

    private boolean matchesRole(Mob mob, NpcRole role) {
        return switch (role) {
            case JOBS_MASTER -> mob.getType() == com.example.advancedjobs.content.ModEntities.JOBS_MASTER.get()
                || mob.getPersistentData().getBoolean(JOB_NPC_TAG);
            case DAILY_BOARD -> mob.getType() == com.example.advancedjobs.content.ModEntities.DAILY_BOARD.get()
                || mob.getPersistentData().getBoolean(DAILY_BOARD_TAG);
            case STATUS_BOARD -> mob.getType() == com.example.advancedjobs.content.ModEntities.STATUS_BOARD.get()
                || mob.getPersistentData().getBoolean(STATUS_BOARD_TAG);
            case CONTRACTS_BOARD -> mob.getType() == com.example.advancedjobs.content.ModEntities.CONTRACTS_BOARD.get()
                || mob.getPersistentData().getBoolean(CONTRACTS_BOARD_TAG);
            case SALARY_BOARD -> mob.getType() == com.example.advancedjobs.content.ModEntities.SALARY_BOARD.get()
                || mob.getPersistentData().getBoolean(SALARY_BOARD_TAG);
            case SKILLS_BOARD -> mob.getType() == com.example.advancedjobs.content.ModEntities.SKILLS_BOARD.get()
                || mob.getPersistentData().getBoolean(SKILLS_BOARD_TAG);
            case LEADERBOARD_BOARD -> mob.getType() == com.example.advancedjobs.content.ModEntities.LEADERBOARD_BOARD.get()
                || mob.getPersistentData().getBoolean(LEADERBOARD_BOARD_TAG);
            case HELP_BOARD -> mob.getType() == com.example.advancedjobs.content.ModEntities.HELP_BOARD.get()
                || mob.getPersistentData().getBoolean(HELP_BOARD_TAG);
        };
    }

    private boolean isRedstoneComponent(BlockState state) {
        return state.is(Blocks.LEVER)
            || state.is(Blocks.STONE_BUTTON) || state.is(Blocks.OAK_BUTTON)
            || state.is(Blocks.REPEATER) || state.is(Blocks.COMPARATOR)
            || state.is(Blocks.OBSERVER) || state.is(Blocks.DISPENSER)
            || state.is(Blocks.DROPPER) || state.is(Blocks.PISTON) || state.is(Blocks.STICKY_PISTON)
            || state.is(Blocks.REDSTONE_WIRE) || state.is(Blocks.REDSTONE_TORCH);
    }

    private boolean isBoss(ResourceLocation id) {
        return id.equals(new ResourceLocation("minecraft", "wither")) || id.equals(new ResourceLocation("minecraft", "ender_dragon"));
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
