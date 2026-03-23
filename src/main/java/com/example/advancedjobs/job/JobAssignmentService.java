package com.example.advancedjobs.job;

import com.example.advancedjobs.config.ConfigManager;
import com.example.advancedjobs.api.EconomyProvider;
import com.example.advancedjobs.api.JobActionContext;
import com.example.advancedjobs.model.ContractProgress;
import com.example.advancedjobs.model.DailyTaskProgress;
import com.example.advancedjobs.model.JobProgress;
import com.example.advancedjobs.model.PlayerJobProfile;
import com.example.advancedjobs.util.ResourceLocationUtil;
import com.example.advancedjobs.util.TextUtil;
import com.example.advancedjobs.util.TimeUtil;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

class JobAssignmentService {
    private final JobSalaryService salaryService;

    JobAssignmentService(JobSalaryService salaryService) {
        this.salaryService = salaryService;
    }

    void ensureAssignments(PlayerJobProfile profile, String jobId) {
        JobProgress progress = profile.progress(jobId);
        long resetAt = TimeUtil.nextResetEpochSecond(java.time.ZoneId.systemDefault(), ConfigManager.dailyResetTime());
        long now = TimeUtil.now();
        if (progress.dailyTasks().isEmpty() || progress.dailyTasks().stream().anyMatch(task -> task.resetEpochSecond() <= now)) {
            progress.dailyTasks().clear();
            for (ConfigManager.DailyTaskTemplate template : ConfigManager.dailyTasks().tasksForJob(jobId).stream().limit(3).toList()) {
                progress.dailyTasks().add(new DailyTaskProgress(template.id(), jobId, template.goal(), 0, resetAt, false));
            }
        }
        progress.contracts().removeIf(contract -> contract.expiresAtEpochSecond() < now);
        List<ConfigManager.ContractTemplate> desiredContracts = pickContracts(jobId);
        for (ConfigManager.ContractTemplate template : desiredContracts) {
            boolean alreadyPresent = progress.contracts().stream()
                .anyMatch(contract -> contract.contractId().equals(template.id()) && contract.expiresAtEpochSecond() >= now);
            if (!alreadyPresent) {
                progress.contracts().add(new ContractProgress(template.id(), jobId, template.rarity(), template.goal(), 0, now + template.durationSeconds(), false));
            }
        }
    }

    long contractRerollCooldownRemaining(PlayerJobProfile profile) {
        return Math.max(0L, ConfigManager.COMMON.contractRerollCooldownSeconds.get() - (TimeUtil.now() - profile.lastContractRerollEpochSecond()));
    }

    JobManager.ContractRerollResult previewContractReroll(PlayerJobProfile profile, String jobId, List<String> assignedJobIds, EconomyProvider economyProvider) {
        if (jobId == null || jobId.isBlank()) {
            return JobManager.ContractRerollResult.NO_JOB;
        }
        if (!assignedJobIds.contains(jobId)) {
            return JobManager.ContractRerollResult.NOT_ASSIGNED;
        }
        if (contractRerollCooldownRemaining(profile) > 0L) {
            return JobManager.ContractRerollResult.COOLDOWN;
        }
        double price = ConfigManager.COMMON.contractRerollPrice.get();
        if (price > 0.0D && economyProvider.getBalance(profile.playerId()) < price) {
            return JobManager.ContractRerollResult.INSUFFICIENT_FUNDS;
        }
        return JobManager.ContractRerollResult.SUCCESS;
    }

    boolean rerollContracts(ServerPlayer player,
                            PlayerJobProfile profile,
                            String jobId,
                            EconomyProvider economyProvider,
                            Consumer<PlayerJobProfile> saveProfile,
                            Consumer<ServerPlayer> syncPlayer) {
        JobManager.ContractRerollResult preview = previewContractReroll(profile, jobId, assignedJobIds(profile), economyProvider);
        if (preview != JobManager.ContractRerollResult.SUCCESS) {
            return false;
        }
        double price = ConfigManager.COMMON.contractRerollPrice.get();
        if (price > 0.0D && !economyProvider.withdraw(player.getUUID(), price, "contract_reroll")) {
            return false;
        }
        JobProgress progress = profile.progress(jobId);
        long now = TimeUtil.now();
        progress.contracts().clear();
        for (ConfigManager.ContractTemplate template : pickContracts(jobId)) {
            progress.contracts().add(new ContractProgress(template.id(), jobId, template.rarity(), template.goal(), 0, now + template.durationSeconds(), false));
        }
        profile.setLastContractRerollEpochSecond(now);
        profile.markDirty();
        saveProfile.accept(profile);
        syncPlayer.accept(player);
        return true;
    }

    void updateDailyTasks(ServerPlayer player,
                          PlayerJobProfile profile,
                          String jobId,
                          JobActionContext context,
                          EconomyProvider economyProvider,
                          Consumer<String> grantTitle) {
        JobProgress progress = profile.progress(jobId);
        for (DailyTaskProgress task : progress.dailyTasks()) {
            ConfigManager.dailyTasks().tasksForJob(jobId).stream()
                .filter(template -> template.id().equals(task.taskId()))
                .filter(template -> template.type() == context.actionType() && template.target().equals(context.targetId()))
                .findFirst()
                .ifPresent(template -> {
                    boolean wasComplete = task.completed();
                    task.addProgress(1);
                    if (!wasComplete && task.completed()) {
                        salaryService.creditSalary(player, profile, jobId, template.salary(), "daily_task", economyProvider);
                        progress.addXp(template.xp());
                        grantTaskExtras(player, template.bonusItem(), template.bonusCount(),
                            template.buffEffect(), template.buffDurationSeconds(), template.buffAmplifier());
                        grantTitle.accept(template.bonusTitle());
                        player.sendSystemMessage(completionMessage("message.advancedjobs.daily_completed", template.type().name(), template.target().toString(),
                            template.salary(), template.xp(), template.bonusItem(), template.bonusCount(),
                            template.buffEffect(), template.buffDurationSeconds(), template.buffAmplifier()));
                        progress.recordDailyCompletion(template.id(), TimeUtil.now(), template.salary(), template.xp());
                    }
                });
        }
    }

    void updateContracts(ServerPlayer player,
                         PlayerJobProfile profile,
                         String jobId,
                         JobActionContext context,
                         EconomyProvider economyProvider,
                         Consumer<String> grantTitle) {
        JobProgress progress = profile.progress(jobId);
        for (ContractProgress contract : progress.contracts()) {
            ConfigManager.contracts().contractsForJob(jobId).stream()
                .filter(template -> template.id().equals(contract.contractId()))
                .filter(template -> template.type() == context.actionType() && template.target().equals(context.targetId()))
                .findFirst()
                .ifPresent(template -> {
                    boolean wasComplete = contract.completed();
                    contract.addProgress(1);
                    if (!wasComplete && contract.completed()) {
                        salaryService.creditSalary(player, profile, jobId, template.salary(), "job_contract", economyProvider);
                        progress.addXp(template.xp());
                        grantTaskExtras(player, template.bonusItem(), template.bonusCount(),
                            template.buffEffect(), template.buffDurationSeconds(), template.buffAmplifier());
                        grantTitle.accept(template.bonusTitle());
                        player.sendSystemMessage(completionMessage("message.advancedjobs.contract_completed", template.type().name(), template.target().toString(),
                            template.salary(), template.xp(), template.bonusItem(), template.bonusCount(),
                            template.buffEffect(), template.buffDurationSeconds(), template.buffAmplifier()));
                        progress.recordContractCompletion(template.id(), template.rarity(), TimeUtil.now(), template.salary(), template.xp());
                    }
                });
        }
    }

    private List<ConfigManager.ContractTemplate> pickContracts(String jobId) {
        List<ConfigManager.ContractTemplate> templates = ConfigManager.contracts().contractsForJob(jobId);
        if (templates.isEmpty()) {
            return List.of();
        }
        List<ConfigManager.ContractTemplate> picked = new java.util.ArrayList<>();
        pickRandomByRarity(templates, "common").ifPresent(picked::add);
        pickRandomByRarity(templates, "rare").ifPresent(picked::add);
        pickRandomByRarity(templates, "elite").ifPresent(picked::add);
        if (picked.isEmpty()) {
            return templates.stream().limit(3).toList();
        }
        return picked;
    }

    private Optional<ConfigManager.ContractTemplate> pickRandomByRarity(List<ConfigManager.ContractTemplate> templates, String rarity) {
        List<ConfigManager.ContractTemplate> filtered = templates.stream()
            .filter(template -> rarity.equalsIgnoreCase(template.rarity()))
            .toList();
        if (filtered.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(filtered.get(ThreadLocalRandom.current().nextInt(filtered.size())));
    }

    private void grantTaskExtras(ServerPlayer player, String bonusItemId, int bonusCount, String buffEffectId, int buffDurationSeconds, int buffAmplifier) {
        if (bonusItemId != null && !bonusItemId.isBlank() && bonusCount > 0) {
            try {
                Item item = ForgeRegistries.ITEMS.getValue(ResourceLocationUtil.parse(bonusItemId));
                if (item != null) {
                    ItemStack stack = new ItemStack(item, bonusCount);
                    if (!player.addItem(stack)) {
                        player.drop(stack, false);
                    }
                }
            } catch (Exception ignored) {
            }
        }
        if (buffEffectId != null && !buffEffectId.isBlank() && buffDurationSeconds > 0) {
            try {
                MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocationUtil.parse(buffEffectId));
                if (effect != null) {
                    player.addEffect(new MobEffectInstance(effect, buffDurationSeconds * 20, Math.max(0, buffAmplifier)));
                }
            } catch (Exception ignored) {
            }
        }
    }

    private Component completionMessage(String key, String actionType, String targetId, double salary, double xp,
                                        String bonusItemId, int bonusCount, String buffEffectId, int buffDurationSeconds, int buffAmplifier) {
        String itemText = bonusItemId != null && !bonusItemId.isBlank() && bonusCount > 0
            ? itemName(bonusItemId).getString() + " x" + bonusCount
            : TextUtil.tr("command.advancedjobs.common.none").getString();
        String buffText = buffEffectId != null && !buffEffectId.isBlank() && buffDurationSeconds > 0
            ? effectName(buffEffectId).getString() + " " + TimeUtil.formatRemainingSeconds(buffDurationSeconds) + " Lv." + (buffAmplifier + 1)
            : TextUtil.tr("command.advancedjobs.common.none").getString();
        return TextUtil.tr(key, humanizeAction(actionType), targetName(targetId), TextUtil.fmt2(salary), TextUtil.fmt2(xp), itemText, buffText);
    }

    private Component itemName(String itemId) {
        try {
            Item item = ForgeRegistries.ITEMS.getValue(ResourceLocationUtil.parse(itemId));
            if (item != null) {
                return item.getDescription();
            }
        } catch (Exception ignored) {
        }
        return Component.literal(itemId);
    }

    private Component effectName(String effectId) {
        try {
            MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocationUtil.parse(effectId));
            if (effect != null) {
                return effect.getDisplayName();
            }
        } catch (Exception ignored) {
        }
        return Component.literal(effectId);
    }

    private String targetName(String targetId) {
        try {
            var id = ResourceLocationUtil.parse(targetId);
            Item item = ForgeRegistries.ITEMS.getValue(id);
            if (item != null) {
                return item.getDescription().getString();
            }
            var block = ForgeRegistries.BLOCKS.getValue(id);
            if (block != null) {
                return block.getName().getString();
            }
            var entity = ForgeRegistries.ENTITY_TYPES.getValue(id);
            if (entity != null) {
                return entity.getDescription().getString();
            }
        } catch (Exception ignored) {
        }
        return targetId;
    }

    private String humanizeAction(String actionType) {
        String[] parts = actionType.toLowerCase().split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }

    private List<String> assignedJobIds(PlayerJobProfile profile) {
        java.util.LinkedHashSet<String> ids = new java.util.LinkedHashSet<>();
        if (profile.activeJobId() != null) {
            ids.add(profile.activeJobId());
        }
        if (ConfigManager.COMMON.allowSecondaryJob.get() && profile.secondaryJobId() != null) {
            ids.add(profile.secondaryJobId());
        }
        return List.copyOf(ids);
    }
}
