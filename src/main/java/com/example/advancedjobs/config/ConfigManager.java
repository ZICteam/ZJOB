package com.example.advancedjobs.config;

import com.example.advancedjobs.AdvancedJobsMod;
import com.example.advancedjobs.model.ActionRewardEntry;
import com.example.advancedjobs.model.JobActionType;
import com.example.advancedjobs.model.JobDefinition;
import com.example.advancedjobs.model.RewardDefinition;
import com.example.advancedjobs.model.SkillBranch;
import com.example.advancedjobs.model.SkillNode;
import com.example.advancedjobs.util.ResourceLocationUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.FileTime;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Locale;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.loading.FMLPaths;

public final class ConfigManager {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final CommonConfig COMMON;
    public static final ClientConfig CLIENT;
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec CLIENT_SPEC;
    private static final String CONFIG_DIR_NAME = "ZAdvancedJobs";
    private static final String LEGACY_CONFIG_DIR_NAME = "advancedjobs";

    private static JobsConfigLoader jobsConfig;
    private static PerksConfigLoader perksConfig;
    private static DailyTasksConfigLoader dailyConfig;
    private static ContractsConfigLoader contractsConfig;
    private static EconomyJsonConfig economyConfig;
    private static NpcSkinsConfig npcSkinsConfig;
    private static NpcLabelsConfig npcLabelsConfig;

    static {
        ForgeConfigSpec.Builder commonBuilder = new ForgeConfigSpec.Builder();
        COMMON = new CommonConfig(commonBuilder);
        COMMON_SPEC = commonBuilder.build();

        ForgeConfigSpec.Builder clientBuilder = new ForgeConfigSpec.Builder();
        CLIENT = new ClientConfig(clientBuilder);
        CLIENT_SPEC = clientBuilder.build();
    }

    private ConfigManager() {
    }

    public static void init() {
        reloadJsonConfigs();
    }

    public static void reloadJsonConfigs() {
        Path root = resolveConfigRoot();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            AdvancedJobsMod.LOGGER.error("Failed to create config directory", e);
        }
        jobsConfig = JobsConfigLoader.load(root.resolve("jobs.json"));
        perksConfig = PerksConfigLoader.load(root.resolve("perks.json"));
        dailyConfig = DailyTasksConfigLoader.load(root.resolve("daily_tasks.json"));
        contractsConfig = ContractsConfigLoader.load(root.resolve("contracts.json"));
        economyConfig = EconomyJsonConfig.load(root.resolve("economy.json"));
        npcSkinsConfig = NpcSkinsConfig.load(root.resolve("npc_skins.json"), root.resolve("npc_skins"));
        npcLabelsConfig = NpcLabelsConfig.load(root.resolve("npc_labels.json"));
        writeCommonJson(root.resolve("common.json"));
        writeClientJson(root.resolve("client.json"));
    }

    public static JobsConfigLoader jobs() { return jobsConfig; }
    public static PerksConfigLoader perks() { return perksConfig; }
    public static DailyTasksConfigLoader dailyTasks() { return dailyConfig; }
    public static ContractsConfigLoader contracts() { return contractsConfig; }
    public static EconomyJsonConfig economy() { return economyConfig; }
    public static NpcSkinsConfig npcSkins() { return npcSkinsConfig; }
    public static NpcLabelsConfig npcLabels() { return npcLabelsConfig; }

    public static void saveEconomyConfig() {
        Path root = resolveConfigRoot();
        economyConfig.save(root.resolve("economy.json"));
        writeCommonJson(root.resolve("common.json"));
    }

    private static Path resolveConfigRoot() {
        Path configDir = FMLPaths.CONFIGDIR.get();
        Path root = configDir.resolve(CONFIG_DIR_NAME);
        Path legacyRoot = configDir.resolve(LEGACY_CONFIG_DIR_NAME);
        migrateLegacyConfigDirectory(legacyRoot, root);
        return root;
    }

    private static void migrateLegacyConfigDirectory(Path legacyRoot, Path root) {
        if (!Files.exists(legacyRoot) || Files.exists(root)) {
            return;
        }
        try {
            Files.move(legacyRoot, root, StandardCopyOption.ATOMIC_MOVE);
            AdvancedJobsMod.LOGGER.info("Migrated config directory from {} to {}", legacyRoot, root);
            return;
        } catch (IOException ignored) {
        }

        try {
            Files.createDirectories(root);
            try (var paths = Files.walk(legacyRoot)) {
                paths.forEach(source -> copyLegacyPath(source, legacyRoot, root));
            }
            AdvancedJobsMod.LOGGER.info("Copied legacy config directory from {} to {}", legacyRoot, root);
        } catch (IOException e) {
            AdvancedJobsMod.LOGGER.error("Failed to migrate legacy config directory from {} to {}", legacyRoot, root, e);
        }
    }

    private static void copyLegacyPath(Path source, Path legacyRoot, Path root) {
        try {
            Path relative = legacyRoot.relativize(source);
            Path target = root.resolve(relative);
            if (Files.isDirectory(source)) {
                Files.createDirectories(target);
            } else if (!Files.exists(target)) {
                Files.copy(source, target);
            }
        } catch (IOException e) {
            AdvancedJobsMod.LOGGER.error("Failed to copy legacy config path {}", source, e);
        }
    }

    public static LocalTime dailyResetTime() {
        try {
            return LocalTime.parse(COMMON.resetTime.get());
        } catch (Exception ignored) {
            return LocalTime.of(4, 0);
        }
    }

    private static boolean safeBoolean(ForgeConfigSpec.BooleanValue value, boolean fallback) {
        try {
            return value.get();
        } catch (IllegalStateException ignored) {
            return fallback;
        }
    }

    private static int safeInt(ForgeConfigSpec.IntValue value, int fallback) {
        try {
            return value.get();
        } catch (IllegalStateException ignored) {
            return fallback;
        }
    }

    private static double safeDouble(ForgeConfigSpec.DoubleValue value, double fallback) {
        try {
            return value.get();
        } catch (IllegalStateException ignored) {
            return fallback;
        }
    }

    private static String safeString(ForgeConfigSpec.ConfigValue<String> value, String fallback) {
        try {
            return value.get();
        } catch (IllegalStateException ignored) {
            return fallback;
        }
    }

    private static void writeCommonJson(Path path) {
        if (Files.exists(path) && !isPlaceholder(path)) {
            return;
        }
        JsonObject root = new JsonObject();
        root.addProperty("description", "Mirror of key server-side job settings for easier review and backup");
        JsonObject jobs = new JsonObject();
        jobs.addProperty("allowSecondaryJob", safeBoolean(COMMON.allowSecondaryJob, false));
        jobs.addProperty("jobChangePrice", safeDouble(COMMON.jobChangePrice, 250.0D));
        jobs.addProperty("jobChangeCooldownSeconds", safeInt(COMMON.jobChangeCooldownSeconds, 3600));
        jobs.addProperty("resetProgressOnChange", safeBoolean(COMMON.resetProgressOnChange, false));
        jobs.addProperty("storeAllJobProgress", safeBoolean(COMMON.storeAllJobProgress, true));
        jobs.addProperty("instantSalary", safeBoolean(COMMON.instantSalary, false));
        jobs.addProperty("salaryClaimIntervalSeconds", safeInt(COMMON.salaryClaimIntervalSeconds, 30));
        jobs.addProperty("maxSalaryPerClaim", safeDouble(COMMON.maxSalaryPerClaim, 50_000.0D));
        jobs.addProperty("salaryTaxRate", safeDouble(COMMON.salaryTaxRate, 0.05D));
        jobs.addProperty("contractRerollPrice", safeDouble(COMMON.contractRerollPrice, 250.0D));
        jobs.addProperty("contractRerollCooldownSeconds", safeInt(COMMON.contractRerollCooldownSeconds, 600));
        jobs.addProperty("maxJobLevel", safeInt(COMMON.maxJobLevel, 100));
        jobs.addProperty("baseXp", safeDouble(COMMON.baseXp, 100.0D));
        jobs.addProperty("growthFactor", safeDouble(COMMON.growthFactor, 1.5D));
        jobs.addProperty("dailyResetTime", safeString(COMMON.resetTime, "04:00"));
        root.add("jobs", jobs);

        JsonObject antiExploit = new JsonObject();
        antiExploit.addProperty("debugLogging", safeBoolean(COMMON.debugLogging, false));
        antiExploit.addProperty("blockArtificialMobRewards", safeBoolean(COMMON.blockArtificialMobRewards, true));
        antiExploit.addProperty("blockBabyMobRewards", safeBoolean(COMMON.blockBabyMobRewards, true));
        antiExploit.addProperty("blockTamedMobRewards", safeBoolean(COMMON.blockTamedMobRewards, true));
        antiExploit.addProperty("repeatedKillDecayThreshold", safeInt(COMMON.repeatedKillDecayThreshold, 48));
        antiExploit.addProperty("lootContainerRewardCooldownSeconds", safeInt(COMMON.lootContainerRewardCooldownSeconds, 21600));
        antiExploit.addProperty("exploredChunkRewardCooldownSeconds", safeInt(COMMON.exploredChunkRewardCooldownSeconds, 43200));
        root.add("antiExploit", antiExploit);

        JsonObject storage = new JsonObject();
        storage.addProperty("storageMode", safeString(COMMON.storageMode, "json"));
        root.add("storage", storage);

        JsonObject economy = new JsonObject();
        economy.addProperty("provider", economy().providerId());
        economy.addProperty("externalCurrency", economy().externalCurrencyId());
        economy.addProperty("taxSinkAccountUuid", economy().taxSinkAccountUuid());
        economy.addProperty("eventEndsAtEpochSecond", economy().eventEndsAtEpochSecond());
        root.add("economy", economy);

        JsonObject npc = new JsonObject();
        npc.addProperty("skinsConfig", "npc_skins.json");
        npc.addProperty("skinsFolder", "npc_skins/");
        npc.addProperty("labelsConfig", "npc_labels.json");
        root.add("npc", npc);
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        } catch (IOException e) {
            AdvancedJobsMod.LOGGER.error("Failed to create {}", path, e);
        }
    }

    public static final class NpcLabelsConfig {
        private final Map<String, String> labels;
        private final Path configPath;

        private NpcLabelsConfig(Map<String, String> labels, Path configPath) {
            this.labels = labels;
            this.configPath = configPath;
        }

        public static NpcLabelsConfig load(Path path) {
            if (!Files.exists(path)) {
                writeDefaults(path);
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                Map<String, String> map = new LinkedHashMap<>();
                for (var entry : defaultLabels().entrySet()) {
                    map.put(entry.getKey(), root != null && root.has(entry.getKey()) ? root.get(entry.getKey()).getAsString() : entry.getValue());
                }
                return new NpcLabelsConfig(map, path);
            } catch (Exception e) {
                AdvancedJobsMod.LOGGER.error("Failed to load npc labels config, using defaults", e);
                return new NpcLabelsConfig(new LinkedHashMap<>(defaultLabels()), path);
            }
        }

        public String label(String roleId) {
            return labels.getOrDefault(roleId, defaultLabels().getOrDefault(roleId, roleId));
        }

        public void setLabel(String roleId, String label) {
            labels.put(roleId, label);
            save();
        }

        public void resetLabel(String roleId) {
            if (defaultLabels().containsKey(roleId)) {
                labels.put(roleId, defaultLabels().get(roleId));
                save();
            }
        }

        public void save() {
            JsonObject root = new JsonObject();
            for (var entry : labels.entrySet()) {
                root.addProperty(entry.getKey(), entry.getValue());
            }
            try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            } catch (IOException e) {
                AdvancedJobsMod.LOGGER.error("Failed to save {}", configPath, e);
            }
        }

        private static void writeDefaults(Path path) {
            JsonObject root = new JsonObject();
            for (var entry : defaultLabels().entrySet()) {
                root.addProperty(entry.getKey(), entry.getValue());
            }
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            } catch (IOException e) {
                AdvancedJobsMod.LOGGER.error("Failed to create {}", path, e);
            }
        }

        private static Map<String, String> defaultLabels() {
            Map<String, String> labels = new LinkedHashMap<>();
            labels.put("jobs_master", "Employment Center");
            labels.put("contracts_board", "Contract Desk");
            labels.put("daily_board", "Quest Desk");
            labels.put("status_board", "Career Ledger");
            labels.put("salary_board", "Salary Clerk");
            labels.put("skills_board", "Skills Mentor");
            labels.put("leaderboard_board", "Hall of Fame");
            labels.put("help_board", "Guide Desk");
            return labels;
        }
    }

    public static final class NpcSkinsConfig {
        private final Map<String, JsonObject> profiles;
        private final Path configPath;
        private final Path skinsDirectory;
        private List<String> cachedLocalSkinFiles = List.of();
        private final Map<String, CachedSkinFile> cachedBase64ByFile = new HashMap<>();

        private NpcSkinsConfig(Map<String, JsonObject> profiles, Path configPath, Path skinsDirectory) {
            this.profiles = profiles;
            this.configPath = configPath;
            this.skinsDirectory = skinsDirectory;
        }

        public static NpcSkinsConfig load(Path path, Path skinsDirectory) {
            try {
                Files.createDirectories(skinsDirectory);
            } catch (IOException e) {
                AdvancedJobsMod.LOGGER.error("Failed to create npc skin directory", e);
            }
            if (!Files.exists(path)) {
                writeDefaults(path);
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                Map<String, JsonObject> map = new LinkedHashMap<>();
                for (var entry : root.entrySet()) {
                    map.put(entry.getKey(), entry.getValue().getAsJsonObject());
                }
                return new NpcSkinsConfig(map, path, skinsDirectory);
            } catch (Exception e) {
                AdvancedJobsMod.LOGGER.error("Failed to load npc skins config, using defaults", e);
                JsonObject root = defaultProfiles();
                Map<String, JsonObject> map = new LinkedHashMap<>();
                for (var entry : root.entrySet()) {
                    map.put(entry.getKey(), entry.getValue().getAsJsonObject());
                }
                return new NpcSkinsConfig(map, path, skinsDirectory);
            }
        }

        public JsonObject profile(String roleId) {
            return profiles.getOrDefault(roleId, defaultProfiles().getAsJsonObject(roleId));
        }

        public boolean hasRole(String roleId) {
            return defaultProfiles().has(roleId);
        }

        public boolean localSkinFileExists(String filename) {
            return Files.exists(skinsDirectory.resolve(filename));
        }

        public List<String> localSkinFiles() {
            if (!Files.exists(skinsDirectory)) {
                return List.of();
            }
            try (var stream = Files.list(skinsDirectory)) {
                cachedLocalSkinFiles = stream
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.toLowerCase(Locale.ROOT).endsWith(".png"))
                    .sorted(Comparator.naturalOrder())
                    .toList();
                return cachedLocalSkinFiles;
            } catch (IOException e) {
                AdvancedJobsMod.LOGGER.warn("Failed to list local npc skin files in {}", skinsDirectory, e);
                return List.of();
            }
        }

        public void setProfile(String roleId, String type, String value) {
            profiles.put(roleId, profile(type, value));
            invalidateCaches();
            save();
        }

        public void resetProfile(String roleId) {
            if (defaultProfiles().has(roleId)) {
                profiles.put(roleId, defaultProfiles().getAsJsonObject(roleId));
                invalidateCaches();
                save();
            }
        }

        public void save() {
            JsonObject root = new JsonObject();
            for (var entry : profiles.entrySet()) {
                root.add(entry.getKey(), entry.getValue());
            }
            try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            } catch (IOException e) {
                AdvancedJobsMod.LOGGER.error("Failed to save {}", configPath, e);
            }
        }

        public String localSkinBase64(String roleId) {
            JsonObject profile = profile(roleId);
            if (!profile.has("type") || !"local".equalsIgnoreCase(profile.get("type").getAsString()) || !profile.has("value")) {
                return null;
            }
            Path file = skinsDirectory.resolve(profile.get("value").getAsString());
            if (!Files.exists(file)) {
                return null;
            }
            try {
                String fileName = file.getFileName().toString();
                long size = Files.size(file);
                FileTime lastModified = Files.getLastModifiedTime(file);
                CachedSkinFile cached = cachedBase64ByFile.get(fileName);
                if (cached != null && cached.matches(size, lastModified)) {
                    return cached.base64();
                }
                String base64 = java.util.Base64.getEncoder().encodeToString(Files.readAllBytes(file));
                cachedBase64ByFile.put(fileName, new CachedSkinFile(base64, size, lastModified));
                return base64;
            } catch (IOException e) {
                AdvancedJobsMod.LOGGER.warn("Failed to read local npc skin {}", file, e);
                return null;
            }
        }

        public int cachedLocalSkinFileCount() {
            return cachedLocalSkinFiles.size();
        }

        public int cachedBase64EntryCount() {
            return cachedBase64ByFile.size();
        }

        public int warmCaches() {
            int warmed = 0;
            localSkinFiles();
            for (String roleId : profiles.keySet()) {
                if (localSkinBase64(roleId) != null) {
                    warmed++;
                }
            }
            return warmed;
        }

        public void clearCaches() {
            invalidateCaches();
        }

        private void invalidateCaches() {
            cachedLocalSkinFiles = List.of();
            cachedBase64ByFile.clear();
        }

        private static void writeDefaults(Path path) {
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(defaultProfiles(), writer);
            } catch (IOException e) {
                AdvancedJobsMod.LOGGER.error("Failed to create {}", path, e);
            }
        }

        private static JsonObject defaultProfiles() {
            JsonObject root = new JsonObject();
            root.add("jobs_master", profile("online", "ZICteam"));
            root.add("contracts_board", profile("online", "ZICteam"));
            root.add("daily_board", profile("online", "ZICteam"));
            root.add("status_board", profile("online", "ZICteam"));
            root.add("salary_board", profile("online", "ZICteam"));
            root.add("skills_board", profile("online", "ZICteam"));
            root.add("leaderboard_board", profile("online", "ZICteam"));
            root.add("help_board", profile("online", "ZICteam"));
            return root;
        }

        private static JsonObject profile(String type, String value) {
            JsonObject profile = new JsonObject();
            profile.addProperty("type", type);
            profile.addProperty("value", value);
            return profile;
        }

        private record CachedSkinFile(String base64, long size, FileTime lastModified) {
            private boolean matches(long size, FileTime lastModified) {
                return this.size == size && this.lastModified.equals(lastModified);
            }
        }
    }

    private static void writeClientJson(Path path) {
        if (Files.exists(path) && !isPlaceholder(path)) {
            return;
        }
        JsonObject root = new JsonObject();
        root.addProperty("description", "Mirror of client-side display settings");
        JsonObject client = new JsonObject();
        client.addProperty("showActionToasts", safeBoolean(CLIENT.showActionToasts, true));
        client.addProperty("compactGui", safeBoolean(CLIENT.compactGui, false));
        root.add("client", client);
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        } catch (IOException e) {
            AdvancedJobsMod.LOGGER.error("Failed to create {}", path, e);
        }
    }

    private static boolean isPlaceholder(Path path) {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            return root != null
                && root.size() == 1
                && root.has("description")
                && root.get("description").isJsonPrimitive();
        } catch (Exception ignored) {
            return false;
        }
    }

    public static final class JobsConfigLoader {
        private final Map<String, JobDefinition> definitions;

        private JobsConfigLoader(Map<String, JobDefinition> definitions) {
            this.definitions = definitions;
        }

        public static JobsConfigLoader load(Path path) {
            if (!Files.exists(path)) {
                writeDefaultJobs(path);
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                return new JobsConfigLoader(parseDefinitions(root));
            } catch (Exception e) {
                AdvancedJobsMod.LOGGER.error("Failed to load jobs config, using defaults", e);
                return new JobsConfigLoader(defaultDefinitions());
            }
        }

        public Map<String, JobDefinition> definitions() {
            return definitions;
        }

        private static Map<String, JobDefinition> parseDefinitions(JsonObject root) {
            Map<String, JobDefinition> map = new LinkedHashMap<>();
            JsonArray jobs = root.getAsJsonArray("jobs");
            for (JsonElement element : jobs) {
                JsonObject obj = element.getAsJsonObject();
                String id = obj.get("id").getAsString();
                List<ActionRewardEntry> rewards = new ArrayList<>();
                for (JsonElement rewardElement : obj.getAsJsonArray("rewards")) {
                    JsonObject reward = rewardElement.getAsJsonObject();
                    rewards.add(new ActionRewardEntry(
                        JobActionType.valueOf(reward.get("type").getAsString()),
                        ResourceLocationUtil.parse(reward.get("target").getAsString()),
                        new RewardDefinition(reward.get("salary").getAsDouble(), reward.get("xp").getAsDouble(), 0.0D, 0.0D)
                    ));
                }
                map.put(id, new JobDefinition(
                    id,
                    obj.get("category").getAsString(),
                    obj.get("icon").getAsString(),
                    obj.get("translationKey").getAsString(),
                    obj.get("descriptionKey").getAsString(),
                    obj.has("maxLevel") ? obj.get("maxLevel").getAsInt() : 100,
                    rewards,
                    List.of(),
                    defaultPassives(id, obj.get("translationKey").getAsString()),
                    toStringList(obj.getAsJsonArray("dailyTaskPool")),
                    toStringList(obj.getAsJsonArray("contractPool"))
                ));
            }
            return map;
        }

        private static void writeDefaultJobs(Path path) {
            JsonObject root = new JsonObject();
            JsonArray jobs = new JsonArray();
            for (JobDefinition definition : defaultDefinitions().values()) {
                JsonObject obj = new JsonObject();
                obj.addProperty("id", definition.id());
                obj.addProperty("category", definition.category());
                obj.addProperty("icon", definition.iconItem());
                obj.addProperty("translationKey", definition.translationKey());
                obj.addProperty("descriptionKey", definition.descriptionKey());
                obj.addProperty("maxLevel", definition.maxLevel());
                JsonArray rewards = new JsonArray();
                for (ActionRewardEntry entry : definition.actionRewards()) {
                    JsonObject reward = new JsonObject();
                    reward.addProperty("type", entry.actionType().name());
                    reward.addProperty("target", entry.targetId().toString());
                    reward.addProperty("salary", entry.rewardDefinition().salary());
                    reward.addProperty("xp", entry.rewardDefinition().xp());
                    rewards.add(reward);
                }
                obj.add("rewards", rewards);
                obj.add("dailyTaskPool", toJsonArray(definition.dailyTaskPool()));
                obj.add("contractPool", toJsonArray(definition.contractPool()));
                jobs.add(obj);
            }
            root.add("jobs", jobs);
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            } catch (IOException e) {
                AdvancedJobsMod.LOGGER.error("Failed to write default jobs config", e);
            }
        }

        static Map<String, JobDefinition> defaultDefinitions() {
            Map<String, JobDefinition> map = new LinkedHashMap<>();
            addDefaultJob(map, "miner", "mining", "minecraft:iron_pickaxe", "job.advancedjobs.miner", "job.advancedjobs.miner.desc",
                List.of(
                    reward(JobActionType.BREAK_BLOCK, "minecraft:coal_ore", 5, 10),
                    reward(JobActionType.BREAK_BLOCK, "minecraft:iron_ore", 8, 12),
                    reward(JobActionType.BREAK_BLOCK, "minecraft:copper_ore", 7, 11),
                    reward(JobActionType.BREAK_BLOCK, "minecraft:gold_ore", 11, 15),
                    reward(JobActionType.BREAK_BLOCK, "minecraft:redstone_ore", 10, 14),
                    reward(JobActionType.BREAK_BLOCK, "minecraft:lapis_ore", 12, 16),
                    reward(JobActionType.BREAK_BLOCK, "minecraft:diamond_ore", 20, 25),
                    reward(JobActionType.BREAK_BLOCK, "minecraft:emerald_ore", 24, 28)
                ));
            addDefaultJob(map, "deep_miner", "mining", "minecraft:deepslate", "job.advancedjobs.deep_miner", "job.advancedjobs.deep_miner.desc",
                List.of(reward(JobActionType.BREAK_BLOCK, "minecraft:deepslate_iron_ore", 10, 15), reward(JobActionType.BREAK_BLOCK, "minecraft:deepslate_gold_ore", 12, 17), reward(JobActionType.BREAK_BLOCK, "minecraft:deepslate_diamond_ore", 24, 28)));
            addDefaultJob(map, "quarry_worker", "mining", "minecraft:stone_pickaxe", "job.advancedjobs.quarry_worker", "job.advancedjobs.quarry_worker.desc",
                List.of(reward(JobActionType.BREAK_BLOCK, "minecraft:stone", 2, 4), reward(JobActionType.BREAK_BLOCK, "minecraft:cobblestone", 2, 4), reward(JobActionType.BREAK_BLOCK, "minecraft:andesite", 3, 5)));
            addDefaultJob(map, "lumberjack", "gathering", "minecraft:iron_axe", "job.advancedjobs.lumberjack", "job.advancedjobs.lumberjack.desc",
                List.of(
                    reward(JobActionType.BREAK_BLOCK, "minecraft:oak_log", 4, 8),
                    reward(JobActionType.BREAK_BLOCK, "minecraft:spruce_log", 4, 8),
                    reward(JobActionType.BREAK_BLOCK, "minecraft:birch_log", 4, 8),
                    reward(JobActionType.BREAK_BLOCK, "minecraft:jungle_log", 5, 9),
                    reward(JobActionType.BREAK_BLOCK, "minecraft:acacia_log", 5, 9),
                    reward(JobActionType.BREAK_BLOCK, "minecraft:dark_oak_log", 6, 10),
                    reward(JobActionType.BREAK_BLOCK, "minecraft:mangrove_log", 6, 10),
                    reward(JobActionType.BREAK_BLOCK, "minecraft:cherry_log", 7, 11)
                ));
            addDefaultJob(map, "forester", "gathering", "minecraft:oak_sapling", "job.advancedjobs.forester", "job.advancedjobs.forester.desc",
                List.of(reward(JobActionType.PLANT_CROP, "minecraft:oak_sapling", 3, 6), reward(JobActionType.BREAK_BLOCK, "minecraft:birch_log", 5, 8), reward(JobActionType.BREAK_BLOCK, "minecraft:cherry_log", 6, 9)));
            addDefaultJob(map, "digger", "mining", "minecraft:iron_shovel", "job.advancedjobs.digger", "job.advancedjobs.digger.desc",
                List.of(reward(JobActionType.BREAK_BLOCK, "minecraft:dirt", 2, 4), reward(JobActionType.BREAK_BLOCK, "minecraft:grass_block", 2, 4), reward(JobActionType.BREAK_BLOCK, "minecraft:gravel", 3, 5)));
            addDefaultJob(map, "sand_collector", "mining", "minecraft:sand", "job.advancedjobs.sand_collector", "job.advancedjobs.sand_collector.desc",
                List.of(reward(JobActionType.BREAK_BLOCK, "minecraft:sand", 3, 5), reward(JobActionType.BREAK_BLOCK, "minecraft:red_sand", 4, 6)));
            addDefaultJob(map, "ice_harvester", "mining", "minecraft:packed_ice", "job.advancedjobs.ice_harvester", "job.advancedjobs.ice_harvester.desc",
                List.of(reward(JobActionType.BREAK_BLOCK, "minecraft:ice", 5, 8), reward(JobActionType.BREAK_BLOCK, "minecraft:packed_ice", 6, 9), reward(JobActionType.BREAK_BLOCK, "minecraft:blue_ice", 8, 12)));
            addDefaultJob(map, "farmer", "farming", "minecraft:wheat", "job.advancedjobs.farmer", "job.advancedjobs.farmer.desc",
                List.of(
                    reward(JobActionType.HARVEST_CROP, "minecraft:wheat", 3, 7),
                    reward(JobActionType.HARVEST_CROP, "minecraft:carrots", 3, 7),
                    reward(JobActionType.HARVEST_CROP, "minecraft:potatoes", 3, 7),
                    reward(JobActionType.HARVEST_CROP, "minecraft:beetroots", 4, 8),
                    reward(JobActionType.HARVEST_CROP, "minecraft:pumpkin", 5, 9),
                    reward(JobActionType.HARVEST_CROP, "minecraft:melon", 5, 9),
                    reward(JobActionType.HARVEST_CROP, "minecraft:sweet_berry_bush", 5, 8),
                    reward(JobActionType.PLANT_CROP, "minecraft:wheat", 2, 5),
                    reward(JobActionType.PLANT_CROP, "minecraft:carrots", 2, 5),
                    reward(JobActionType.PLANT_CROP, "minecraft:potatoes", 2, 5)
                ));
            addDefaultJob(map, "harvester", "farming", "minecraft:golden_hoe", "job.advancedjobs.harvester", "job.advancedjobs.harvester.desc",
                List.of(reward(JobActionType.HARVEST_CROP, "minecraft:potatoes", 4, 8), reward(JobActionType.HARVEST_CROP, "minecraft:beetroots", 4, 8), reward(JobActionType.HARVEST_CROP, "minecraft:pumpkin", 6, 10)));
            addDefaultJob(map, "animal_breeder", "farming", "minecraft:wheat", "job.advancedjobs.animal_breeder", "job.advancedjobs.animal_breeder.desc",
                List.of(reward(JobActionType.BREED_ANIMAL, "minecraft:cow", 8, 10), reward(JobActionType.BREED_ANIMAL, "minecraft:sheep", 7, 10), reward(JobActionType.BREED_ANIMAL, "minecraft:pig", 7, 10)));
            addDefaultJob(map, "fisher", "farming", "minecraft:fishing_rod", "job.advancedjobs.fisher", "job.advancedjobs.fisher.desc",
                List.of(
                    reward(JobActionType.FISH, "minecraft:cod", 8, 10),
                    reward(JobActionType.FISH, "minecraft:salmon", 10, 12),
                    reward(JobActionType.FISH, "minecraft:tropical_fish", 12, 14),
                    reward(JobActionType.FISH, "minecraft:pufferfish", 13, 15),
                    reward(JobActionType.FISH, "minecraft:name_tag", 18, 20)
                ));
            addDefaultJob(map, "beekeeper", "farming", "minecraft:honeycomb", "job.advancedjobs.beekeeper", "job.advancedjobs.beekeeper.desc",
                List.of(reward(JobActionType.BREAK_BLOCK, "minecraft:beehive", 12, 16), reward(JobActionType.CRAFT_ITEM, "minecraft:honey_bottle", 8, 11)));
            addDefaultJob(map, "herbalist", "farming", "minecraft:fern", "job.advancedjobs.herbalist", "job.advancedjobs.herbalist.desc",
                List.of(reward(JobActionType.BREAK_BLOCK, "minecraft:sweet_berry_bush", 5, 8), reward(JobActionType.BREAK_BLOCK, "minecraft:nether_wart", 6, 9), reward(JobActionType.BREW_POTION, "minecraft:potion", 7, 10)));
            addDefaultJob(map, "shepherd", "farming", "minecraft:white_wool", "job.advancedjobs.shepherd", "job.advancedjobs.shepherd.desc",
                List.of(reward(JobActionType.BREED_ANIMAL, "minecraft:sheep", 8, 11), reward(JobActionType.CRAFT_ITEM, "minecraft:white_wool", 5, 8)));
            addDefaultJob(map, "hunter", "combat", "minecraft:bow", "job.advancedjobs.hunter", "job.advancedjobs.hunter.desc",
                List.of(
                    reward(JobActionType.KILL_MOB, "minecraft:zombie", 6, 9),
                    reward(JobActionType.KILL_MOB, "minecraft:skeleton", 7, 10),
                    reward(JobActionType.KILL_MOB, "minecraft:spider", 7, 10),
                    reward(JobActionType.KILL_MOB, "minecraft:creeper", 9, 12),
                    reward(JobActionType.KILL_MOB, "minecraft:witch", 14, 18),
                    reward(JobActionType.KILL_MOB, "minecraft:enderman", 12, 16),
                    reward(JobActionType.KILL_MOB, "minecraft:blaze", 14, 18)
                ));
            addDefaultJob(map, "monster_slayer", "combat", "minecraft:diamond_sword", "job.advancedjobs.monster_slayer", "job.advancedjobs.monster_slayer.desc",
                List.of(reward(JobActionType.KILL_MOB, "minecraft:creeper", 8, 12), reward(JobActionType.KILL_MOB, "minecraft:witch", 14, 18), reward(JobActionType.KILL_MOB, "minecraft:enderman", 10, 14)));
            addDefaultJob(map, "guard", "combat", "minecraft:shield", "job.advancedjobs.guard", "job.advancedjobs.guard.desc",
                List.of(reward(JobActionType.KILL_MOB, "minecraft:pillager", 10, 14), reward(JobActionType.KILL_MOB, "minecraft:vindicator", 12, 16)));
            addDefaultJob(map, "bounty_hunter", "combat", "minecraft:crossbow", "job.advancedjobs.bounty_hunter", "job.advancedjobs.bounty_hunter.desc",
                List.of(reward(JobActionType.KILL_MOB, "minecraft:evoker", 18, 24), reward(JobActionType.KILL_MOB, "minecraft:blaze", 12, 16)));
            addDefaultJob(map, "defender", "combat", "minecraft:iron_chestplate", "job.advancedjobs.defender", "job.advancedjobs.defender.desc",
                List.of(reward(JobActionType.KILL_MOB, "minecraft:zombie", 5, 8), reward(JobActionType.KILL_MOB, "minecraft:husk", 7, 10)));
            addDefaultJob(map, "boss_hunter", "combat", "minecraft:nether_star", "job.advancedjobs.boss_hunter", "job.advancedjobs.boss_hunter.desc",
                List.of(reward(JobActionType.KILL_BOSS, "minecraft:wither", 120, 150), reward(JobActionType.KILL_BOSS, "minecraft:ender_dragon", 150, 200)));
            addDefaultJob(map, "builder", "craft", "minecraft:bricks", "job.advancedjobs.builder", "job.advancedjobs.builder.desc",
                List.of(
                    reward(JobActionType.PLACE_BLOCK, "minecraft:stone_bricks", 2, 5),
                    reward(JobActionType.PLACE_BLOCK, "minecraft:glass", 3, 5),
                    reward(JobActionType.PLACE_BLOCK, "minecraft:bricks", 3, 6),
                    reward(JobActionType.PLACE_BLOCK, "minecraft:oak_planks", 2, 5),
                    reward(JobActionType.PLACE_BLOCK, "minecraft:white_concrete", 4, 7),
                    reward(JobActionType.PLACE_BLOCK, "minecraft:terracotta", 4, 7)
                ));
            addDefaultJob(map, "mason", "craft", "minecraft:stone_bricks", "job.advancedjobs.mason", "job.advancedjobs.mason.desc",
                List.of(reward(JobActionType.PLACE_BLOCK, "minecraft:stone_bricks", 3, 6), reward(JobActionType.PLACE_BLOCK, "minecraft:bricks", 4, 7), reward(JobActionType.PLACE_BLOCK, "minecraft:polished_andesite", 3, 6)));
            addDefaultJob(map, "carpenter", "craft", "minecraft:oak_planks", "job.advancedjobs.carpenter", "job.advancedjobs.carpenter.desc",
                List.of(reward(JobActionType.PLACE_BLOCK, "minecraft:oak_planks", 3, 6), reward(JobActionType.CRAFT_ITEM, "minecraft:oak_door", 4, 7), reward(JobActionType.CRAFT_ITEM, "minecraft:chest", 6, 9)));
            addDefaultJob(map, "merchant", "utility", "minecraft:emerald", "job.advancedjobs.merchant", "job.advancedjobs.merchant.desc",
                List.of(
                    reward(JobActionType.TRADE_WITH_VILLAGER, "minecraft:villager", 12, 14),
                    reward(JobActionType.OPEN_LOOT_CHEST, "minecraft:chest", 8, 10)
                ));
            addDefaultJob(map, "alchemist", "magic", "minecraft:brewing_stand", "job.advancedjobs.alchemist", "job.advancedjobs.alchemist.desc",
                List.of(
                    reward(JobActionType.BREW_POTION, "minecraft:potion", 10, 14),
                    reward(JobActionType.CRAFT_ITEM, "minecraft:glistering_melon_slice", 9, 12),
                    reward(JobActionType.CRAFT_ITEM, "minecraft:fermented_spider_eye", 10, 13)
                ));
            addDefaultJob(map, "enchanter", "magic", "minecraft:enchanting_table", "job.advancedjobs.enchanter", "job.advancedjobs.enchanter.desc",
                List.of(
                    reward(JobActionType.ENCHANT_ITEM, "minecraft:book", 15, 18),
                    reward(JobActionType.ENCHANT_ITEM, "minecraft:iron_sword", 18, 20),
                    reward(JobActionType.ENCHANT_ITEM, "minecraft:diamond_pickaxe", 24, 28)
                ));
            addDefaultJob(map, "explorer", "utility", "minecraft:compass", "job.advancedjobs.explorer", "job.advancedjobs.explorer.desc",
                List.of(
                    reward(JobActionType.EXPLORE_CHUNK, "minecraft:chunk", 4, 6),
                    reward(JobActionType.OPEN_LOOT_CHEST, "minecraft:chest", 12, 15),
                    reward(JobActionType.OPEN_LOOT_CHEST, "minecraft:barrel", 10, 13)
                ));
            addDefaultJob(map, "blacksmith", "craft", "minecraft:anvil", "job.advancedjobs.blacksmith", "job.advancedjobs.blacksmith.desc",
                List.of(
                    reward(JobActionType.SMELT_ITEM, "minecraft:iron_ingot", 6, 10),
                    reward(JobActionType.SMELT_ITEM, "minecraft:gold_ingot", 7, 11),
                    reward(JobActionType.CRAFT_ITEM, "minecraft:iron_sword", 15, 16),
                    reward(JobActionType.CRAFT_ITEM, "minecraft:iron_pickaxe", 16, 17),
                    reward(JobActionType.CRAFT_ITEM, "minecraft:shield", 14, 16)
                ));
            addDefaultJob(map, "armorer", "craft", "minecraft:diamond_chestplate", "job.advancedjobs.armorer", "job.advancedjobs.armorer.desc",
                List.of(
                    reward(JobActionType.CRAFT_ITEM, "minecraft:iron_helmet", 14, 15),
                    reward(JobActionType.CRAFT_ITEM, "minecraft:iron_chestplate", 18, 18),
                    reward(JobActionType.CRAFT_ITEM, "minecraft:iron_leggings", 16, 17),
                    reward(JobActionType.CRAFT_ITEM, "minecraft:iron_boots", 13, 14),
                    reward(JobActionType.CRAFT_ITEM, "minecraft:diamond_chestplate", 30, 34)
                ));
            addDefaultJob(map, "cook", "craft", "minecraft:cooked_beef", "job.advancedjobs.cook", "job.advancedjobs.cook.desc",
                List.of(
                    reward(JobActionType.SMELT_ITEM, "minecraft:cooked_beef", 5, 8),
                    reward(JobActionType.SMELT_ITEM, "minecraft:cooked_porkchop", 5, 8),
                    reward(JobActionType.SMELT_ITEM, "minecraft:cooked_chicken", 4, 7),
                    reward(JobActionType.CRAFT_ITEM, "minecraft:bread", 4, 6),
                    reward(JobActionType.CRAFT_ITEM, "minecraft:cake", 12, 14),
                    reward(JobActionType.CRAFT_ITEM, "minecraft:pumpkin_pie", 8, 10)
                ));
            addDefaultJob(map, "engineer", "utility", "minecraft:redstone", "job.advancedjobs.engineer", "job.advancedjobs.engineer.desc",
                List.of(
                    reward(JobActionType.REDSTONE_USE, "minecraft:redstone", 4, 8),
                    reward(JobActionType.PLACE_BLOCK, "minecraft:piston", 6, 10),
                    reward(JobActionType.PLACE_BLOCK, "minecraft:observer", 7, 11),
                    reward(JobActionType.CRAFT_ITEM, "minecraft:repeater", 8, 12),
                    reward(JobActionType.CRAFT_ITEM, "minecraft:comparator", 9, 13)
                ));
            addDefaultJob(map, "treasure_hunter", "utility", "minecraft:heart_of_the_sea", "job.advancedjobs.treasure_hunter", "job.advancedjobs.treasure_hunter.desc",
                List.of(
                    reward(JobActionType.OPEN_LOOT_CHEST, "minecraft:buried_treasure", 20, 25),
                    reward(JobActionType.OPEN_LOOT_CHEST, "minecraft:chest", 14, 18),
                    reward(JobActionType.OPEN_LOOT_CHEST, "minecraft:barrel", 12, 16),
                    reward(JobActionType.FISH, "minecraft:name_tag", 12, 14),
                    reward(JobActionType.FISH, "minecraft:enchanted_book", 18, 22)
                ));
            addDefaultJob(map, "redstone_technician", "utility", "minecraft:repeater", "job.advancedjobs.redstone_technician", "job.advancedjobs.redstone_technician.desc",
                List.of(reward(JobActionType.REDSTONE_USE, "minecraft:redstone", 5, 9), reward(JobActionType.CRAFT_ITEM, "minecraft:repeater", 8, 12), reward(JobActionType.CRAFT_ITEM, "minecraft:comparator", 9, 13)));
            addDefaultJob(map, "archaeologist", "magic", "minecraft:brush", "job.advancedjobs.archaeologist", "job.advancedjobs.archaeologist.desc",
                List.of(reward(JobActionType.OPEN_LOOT_CHEST, "minecraft:suspicious_sand", 18, 22), reward(JobActionType.BREAK_BLOCK, "minecraft:suspicious_gravel", 16, 20)));
            return map;
        }

        private static void addDefaultJob(Map<String, JobDefinition> map, String id, String category, String icon, String key, String descKey, List<ActionRewardEntry> rewards) {
            List<ActionRewardEntry> tunedRewards = rewards.stream().map(entry -> tuneReward(id, entry)).toList();
            map.put(id, new JobDefinition(id, category, icon, key, descKey, 100, tunedRewards, List.of(), defaultPassives(id, key),
                List.of(id + "_daily_1", id + "_daily_2", id + "_daily_3"),
                List.of(id + "_contract_common", id + "_contract_rare", id + "_contract_elite")));
        }

        private static Map<Integer, List<String>> defaultPassives(String jobId, String key) {
            return switch (jobId) {
                case "miner", "deep_miner", "lumberjack", "forester", "farmer", "harvester", "animal_breeder",
                     "fisher", "hunter", "monster_slayer", "guard", "bounty_hunter", "defender", "boss_hunter",
                     "builder", "mason", "carpenter", "merchant", "alchemist", "enchanter", "explorer",
                     "blacksmith", "armorer", "cook", "engineer", "treasure_hunter", "redstone_technician",
                     "archaeologist", "sand_collector", "ice_harvester", "quarry_worker", "digger", "shepherd",
                     "beekeeper", "herbalist" ->
                    Map.of(
                        10, List.of(key + ".perk.one"),
                        25, List.of(key + ".perk.two"),
                        50, List.of(key + ".perk.three")
                    );
                default -> genericPassives(key);
            };
        }

        private static Map<Integer, List<String>> genericPassives(String key) {
            return Map.of(
                10, List.of(key + ".perk.one"),
                25, List.of(key + ".perk.two"),
                50, List.of(key + ".perk.three")
            );
        }

        private static ActionRewardEntry reward(JobActionType type, String target, double salary, double xp) {
            return new ActionRewardEntry(type, ResourceLocationUtil.parse(target), new RewardDefinition(salary, xp, 0.0D, 0.0D));
        }

        private static ActionRewardEntry tuneReward(String jobId, ActionRewardEntry entry) {
            double salary = entry.rewardDefinition().salary() * salaryMultiplier(jobId, entry.actionType(), entry.targetId());
            double xp = entry.rewardDefinition().xp() * xpMultiplier(jobId, entry.actionType(), entry.targetId());
            salary *= starterSalaryOverride(jobId, entry.actionType(), entry.targetId());
            xp *= starterXpOverride(jobId, entry.actionType(), entry.targetId());
            return new ActionRewardEntry(
                entry.actionType(),
                entry.targetId(),
                new RewardDefinition(round2(salary), round2(xp), entry.rewardDefinition().bonusLootChance(), entry.rewardDefinition().buffChance())
            );
        }

        private static double starterSalaryOverride(String jobId, JobActionType actionType, ResourceLocation targetId) {
            String path = resourcePath(targetId);
            return switch (jobId) {
                case "miner" -> {
                    if (path.contains("coal")) yield 0.92D;
                    if (path.contains("copper")) yield 0.96D;
                    if (path.contains("diamond")) yield 1.18D;
                    if (path.contains("emerald")) yield 1.24D;
                    yield 1.0D;
                }
                case "lumberjack" -> {
                    if (path.contains("oak") || path.contains("spruce") || path.contains("birch")) yield 0.96D;
                    if (path.contains("dark_oak") || path.contains("mangrove") || path.contains("cherry")) yield 1.12D;
                    yield 1.0D;
                }
                case "farmer" -> {
                    if (actionType == JobActionType.PLANT_CROP) yield 0.78D;
                    if (path.contains("pumpkin") || path.contains("melon")) yield 1.14D;
                    if (path.contains("sweet_berry")) yield 1.08D;
                    yield 1.0D;
                }
                case "fisher" -> {
                    if (path.contains("cod")) yield 0.9D;
                    if (path.contains("salmon")) yield 0.96D;
                    if (path.contains("tropical_fish") || path.contains("pufferfish")) yield 1.12D;
                    if (path.contains("name_tag")) yield 1.25D;
                    yield 1.0D;
                }
                case "hunter" -> {
                    if (path.contains("zombie") || path.contains("spider")) yield 0.94D;
                    if (path.contains("creeper") || path.contains("witch") || path.contains("blaze")) yield 1.12D;
                    if (path.contains("enderman")) yield 1.08D;
                    yield 1.0D;
                }
                case "builder" -> {
                    if (path.contains("oak_planks") || path.contains("stone_bricks")) yield 0.94D;
                    if (path.contains("glass")) yield 1.02D;
                    if (path.contains("concrete") || path.contains("terracotta")) yield 1.14D;
                    yield 1.0D;
                }
                case "merchant" -> {
                    if (actionType == JobActionType.TRADE_WITH_VILLAGER) yield 1.16D;
                    if (actionType == JobActionType.OPEN_LOOT_CHEST) yield 0.82D;
                    yield 1.0D;
                }
                case "alchemist" -> {
                    if (actionType == JobActionType.BREW_POTION) yield 1.15D;
                    if (path.contains("glistering_melon")) yield 1.05D;
                    if (path.contains("fermented_spider_eye")) yield 1.1D;
                    yield 1.0D;
                }
                case "enchanter" -> {
                    if (path.contains("book")) yield 0.94D;
                    if (path.contains("iron_sword")) yield 1.08D;
                    if (path.contains("diamond_pickaxe")) yield 1.18D;
                    yield 1.0D;
                }
                case "explorer" -> {
                    if (actionType == JobActionType.EXPLORE_CHUNK) yield 0.92D;
                    if (path.contains("barrel")) yield 0.95D;
                    if (path.contains("chest")) yield 1.08D;
                    yield 1.0D;
                }
                case "blacksmith" -> {
                    if (actionType == JobActionType.SMELT_ITEM) yield 0.94D;
                    if (path.contains("shield")) yield 1.04D;
                    if (path.contains("iron_pickaxe") || path.contains("iron_sword")) yield 1.1D;
                    yield 1.0D;
                }
                case "armorer" -> {
                    if (path.contains("diamond_chestplate")) yield 1.24D;
                    if (path.contains("iron_chestplate") || path.contains("iron_leggings")) yield 1.08D;
                    if (path.contains("iron_boots") || path.contains("iron_helmet")) yield 0.96D;
                    yield 1.0D;
                }
                case "cook" -> {
                    if (actionType == JobActionType.SMELT_ITEM && (path.contains("beef") || path.contains("porkchop"))) yield 0.96D;
                    if (path.contains("cake")) yield 1.24D;
                    if (path.contains("pumpkin_pie")) yield 1.08D;
                    yield 1.0D;
                }
                case "engineer" -> {
                    if (actionType == JobActionType.REDSTONE_USE) yield 0.86D;
                    if (path.contains("piston") || path.contains("observer")) yield 1.12D;
                    if (path.contains("comparator")) yield 1.18D;
                    yield 1.0D;
                }
                case "treasure_hunter" -> {
                    if (path.contains("buried_treasure")) yield 1.3D;
                    if (path.contains("enchanted_book")) yield 1.16D;
                    if (path.contains("name_tag")) yield 1.08D;
                    if (path.contains("barrel")) yield 0.92D;
                    yield 1.0D;
                }
                case "deep_miner" -> {
                    if (path.contains("iron")) yield 0.94D;
                    if (path.contains("gold")) yield 1.02D;
                    if (path.contains("diamond")) yield 1.2D;
                    yield 1.0D;
                }
                case "forester" -> {
                    if (path.contains("oak_log")) yield 0.94D;
                    if (path.contains("spruce_log")) yield 1.02D;
                    if (path.contains("cherry_log") || path.contains("mangrove_log")) yield 1.16D;
                    yield 1.0D;
                }
                case "quarry_worker" -> {
                    if (path.contains("stone") || path.contains("cobblestone")) yield 0.9D;
                    if (path.contains("andesite")) yield 1.06D;
                    if (path.contains("deepslate")) yield 1.1D;
                    if (path.contains("tuff")) yield 1.08D;
                    yield 1.0D;
                }
                case "digger" -> {
                    if (path.contains("dirt") || path.contains("grass_block")) yield 0.9D;
                    if (path.contains("gravel")) yield 1.12D;
                    if (path.contains("clay")) yield 1.14D;
                    if (path.contains("mud")) yield 1.08D;
                    yield 1.0D;
                }
                case "sand_collector" -> {
                    if (path.contains("sand") && !path.contains("red_sand")) yield 0.94D;
                    if (path.contains("red_sand")) yield 1.16D;
                    if (path.contains("sandstone")) yield 1.08D;
                    yield 1.0D;
                }
                case "ice_harvester" -> {
                    if (path.contains("ice") && !path.contains("packed") && !path.contains("blue")) yield 0.92D;
                    if (path.contains("packed_ice")) yield 1.04D;
                    if (path.contains("blue_ice")) yield 1.22D;
                    if (path.contains("snow")) yield 0.96D;
                    yield 1.0D;
                }
                case "harvester" -> {
                    if (path.contains("potato") || path.contains("beetroot")) yield 0.94D;
                    if (path.contains("pumpkin") || path.contains("melon")) yield 1.14D;
                    yield 1.0D;
                }
                case "animal_breeder" -> {
                    if (path.contains("cow")) yield 1.12D;
                    if (path.contains("sheep")) yield 1.02D;
                    if (path.contains("pig")) yield 0.94D;
                    yield 1.0D;
                }
                case "beekeeper" -> {
                    if (path.contains("beehive")) yield 1.14D;
                    if (path.contains("honey_bottle")) yield 0.94D;
                    if (path.contains("honeycomb")) yield 1.08D;
                    if (path.contains("bee_nest")) yield 1.18D;
                    yield 1.0D;
                }
                case "herbalist" -> {
                    if (path.contains("sweet_berry")) yield 0.96D;
                    if (path.contains("nether_wart")) yield 1.12D;
                    if (actionType == JobActionType.BREW_POTION) yield 1.08D;
                    if (path.contains("glow_berries")) yield 1.06D;
                    yield 1.0D;
                }
                case "shepherd" -> {
                    if (path.contains("sheep")) yield 1.08D;
                    if (path.contains("white_wool")) yield 0.94D;
                    if (path.contains("carpet")) yield 1.08D;
                    yield 1.0D;
                }
                case "monster_slayer" -> {
                    if (path.contains("creeper")) yield 0.96D;
                    if (path.contains("witch")) yield 1.14D;
                    if (path.contains("enderman")) yield 1.08D;
                    yield 1.0D;
                }
                case "guard" -> {
                    if (path.contains("pillager")) yield 0.98D;
                    if (path.contains("vindicator")) yield 1.12D;
                    yield 1.0D;
                }
                case "bounty_hunter" -> {
                    if (path.contains("blaze")) yield 0.96D;
                    if (path.contains("evoker")) yield 1.18D;
                    yield 1.0D;
                }
                case "defender" -> {
                    if (path.contains("zombie")) yield 0.92D;
                    if (path.contains("husk")) yield 1.12D;
                    yield 1.0D;
                }
                case "boss_hunter" -> {
                    if (path.contains("wither")) yield 0.96D;
                    if (path.contains("ender_dragon")) yield 1.18D;
                    yield 1.0D;
                }
                case "mason" -> {
                    if (path.contains("stone_bricks")) yield 0.96D;
                    if (path.contains("polished_andesite")) yield 1.08D;
                    if (path.contains("bricks")) yield 1.12D;
                    yield 1.0D;
                }
                case "carpenter" -> {
                    if (path.contains("oak_planks")) yield 0.94D;
                    if (path.contains("oak_door")) yield 1.08D;
                    if (path.contains("chest")) yield 1.14D;
                    yield 1.0D;
                }
                case "redstone_technician" -> {
                    if (actionType == JobActionType.REDSTONE_USE) yield 0.88D;
                    if (path.contains("repeater")) yield 1.06D;
                    if (path.contains("comparator")) yield 1.18D;
                    yield 1.0D;
                }
                case "archaeologist" -> {
                    if (path.contains("suspicious_sand") || path.contains("suspicious_gravel")) yield 1.14D;
                    if (path.contains("pottery_sherd")) yield 1.18D;
                    if (path.contains("brush")) yield 0.96D;
                    yield 1.0D;
                }
                default -> 1.0D;
            };
        }

        private static double starterXpOverride(String jobId, JobActionType actionType, ResourceLocation targetId) {
            String path = resourcePath(targetId);
            return switch (jobId) {
                case "miner" -> {
                    if (path.contains("coal")) yield 0.94D;
                    if (path.contains("diamond") || path.contains("emerald")) yield 1.16D;
                    yield 1.0D;
                }
                case "farmer" -> {
                    if (actionType == JobActionType.PLANT_CROP) yield 0.82D;
                    if (path.contains("pumpkin") || path.contains("melon")) yield 1.1D;
                    yield 1.0D;
                }
                case "fisher" -> {
                    if (path.contains("name_tag")) yield 1.2D;
                    if (path.contains("tropical_fish") || path.contains("pufferfish")) yield 1.08D;
                    yield 1.0D;
                }
                case "hunter" -> {
                    if (path.contains("witch") || path.contains("blaze")) yield 1.12D;
                    if (path.contains("zombie")) yield 0.95D;
                    yield 1.0D;
                }
                case "merchant" -> {
                    if (actionType == JobActionType.TRADE_WITH_VILLAGER) yield 1.12D;
                    yield 1.0D;
                }
                case "alchemist", "enchanter" -> {
                    if (actionType == JobActionType.BREW_POTION || actionType == JobActionType.ENCHANT_ITEM) yield 1.12D;
                    yield 1.0D;
                }
                case "explorer", "treasure_hunter" -> {
                    if (actionType == JobActionType.EXPLORE_CHUNK || actionType == JobActionType.OPEN_LOOT_CHEST) yield 1.1D;
                    yield 1.0D;
                }
                case "engineer" -> {
                    if (path.contains("comparator") || path.contains("observer")) yield 1.14D;
                    if (actionType == JobActionType.REDSTONE_USE) yield 0.88D;
                    yield 1.0D;
                }
                case "deep_miner" -> {
                    if (path.contains("diamond")) yield 1.14D;
                    if (path.contains("gold")) yield 1.06D;
                    yield 1.0D;
                }
                case "forester" -> {
                    if (path.contains("oak_log")) yield 0.96D;
                    if (path.contains("spruce_log")) yield 1.02D;
                    if (path.contains("cherry_log") || path.contains("mangrove_log")) yield 1.12D;
                    yield 1.0D;
                }
                case "quarry_worker", "digger" -> {
                    if (path.contains("andesite") || path.contains("gravel")) yield 1.1D;
                    if ("quarry_worker".equals(jobId) && (path.contains("deepslate") || path.contains("tuff"))) yield 1.12D;
                    if ("digger".equals(jobId) && (path.contains("clay") || path.contains("mud"))) yield 1.1D;
                    yield 1.0D;
                }
                case "sand_collector", "ice_harvester" -> {
                    if (path.contains("red_sand") || path.contains("blue_ice")) yield 1.14D;
                    if ("sand_collector".equals(jobId) && path.contains("sandstone")) yield 1.08D;
                    if ("ice_harvester".equals(jobId) && path.contains("snow")) yield 0.98D;
                    yield 1.0D;
                }
                case "animal_breeder", "shepherd" -> {
                    if (actionType == JobActionType.BREED_ANIMAL) yield 1.08D;
                    if ("animal_breeder".equals(jobId) && path.contains("cow")) yield 1.12D;
                    if ("shepherd".equals(jobId) && path.contains("carpet")) yield 1.1D;
                    yield 1.0D;
                }
                case "beekeeper", "herbalist" -> {
                    if (path.contains("beehive") || path.contains("nether_wart")) yield 1.1D;
                    if (actionType == JobActionType.BREW_POTION) yield 1.08D;
                    if ("beekeeper".equals(jobId) && (path.contains("honeycomb") || path.contains("bee_nest"))) yield 1.12D;
                    if ("herbalist".equals(jobId) && path.contains("glow_berries")) yield 1.08D;
                    yield 1.0D;
                }
                case "monster_slayer", "guard", "bounty_hunter", "defender", "boss_hunter" -> {
                    if (actionType == JobActionType.KILL_BOSS) yield 1.16D;
                    if (path.contains("evoker") || path.contains("vindicator") || path.contains("husk")) yield 1.1D;
                    yield 1.0D;
                }
                case "mason", "carpenter" -> {
                    if (path.contains("bricks") || path.contains("chest")) yield 1.08D;
                    yield 1.0D;
                }
                case "redstone_technician" -> {
                    if (path.contains("comparator")) yield 1.16D;
                    if (actionType == JobActionType.REDSTONE_USE) yield 0.9D;
                    yield 1.0D;
                }
                case "archaeologist" -> {
                    if (path.contains("suspicious") || path.contains("pottery_sherd")) yield 1.14D;
                    yield 1.0D;
                }
                default -> 1.0D;
            };
        }

        private static double salaryMultiplier(String jobId, JobActionType actionType, ResourceLocation targetId) {
            double base = switch (jobId) {
                case "boss_hunter" -> 2.6D;
                case "bounty_hunter", "treasure_hunter", "archaeologist" -> 1.9D;
                case "explorer", "enchanter", "alchemist", "redstone_technician", "engineer" -> 1.55D;
                case "monster_slayer", "guard", "defender", "merchant", "blacksmith", "armorer" -> 1.3D;
                case "beekeeper" -> 1.22D;
                case "herbalist" -> 1.24D;
                case "shepherd" -> 1.2D;
                case "quarry_worker" -> 1.06D;
                case "digger" -> 1.04D;
                case "sand_collector" -> 1.08D;
                case "ice_harvester" -> 1.1D;
                case "farmer", "harvester", "lumberjack", "builder", "mason", "carpenter", "forester" -> 1.0D;
                case "animal_breeder" -> 1.18D;
                default -> 1.1D;
            };
            if (actionType == JobActionType.KILL_BOSS) {
                base += 0.8D;
            }
            if (actionType == JobActionType.BREED_ANIMAL || actionType == JobActionType.BREW_POTION) {
                base += 0.08D;
            }
            if (isRareTarget(targetId)) {
                base += 0.2D;
            }
            return base;
        }

        private static double xpMultiplier(String jobId, JobActionType actionType, ResourceLocation targetId) {
            double base = switch (jobId) {
                case "boss_hunter" -> 2.2D;
                case "bounty_hunter", "treasure_hunter", "archaeologist" -> 1.8D;
                case "explorer", "enchanter", "alchemist", "redstone_technician", "engineer" -> 1.5D;
                case "monster_slayer", "guard", "defender", "merchant", "blacksmith", "armorer" -> 1.25D;
                case "beekeeper" -> 1.24D;
                case "herbalist" -> 1.26D;
                case "shepherd" -> 1.22D;
                case "quarry_worker" -> 1.18D;
                case "digger" -> 1.16D;
                case "sand_collector" -> 1.18D;
                case "ice_harvester" -> 1.22D;
                case "forester" -> 1.16D;
                case "animal_breeder" -> 1.2D;
                default -> 1.1D;
            };
            if (actionType == JobActionType.EXPLORE_CHUNK || actionType == JobActionType.OPEN_LOOT_CHEST) {
                base += 0.15D;
            }
            if (actionType == JobActionType.BREED_ANIMAL || actionType == JobActionType.BREW_POTION) {
                base += 0.08D;
            }
            if (isRareTarget(targetId)) {
                base += 0.15D;
            }
            return base;
        }

        private static boolean isRareTarget(ResourceLocation targetId) {
            String path = resourcePath(targetId);
            return path.contains("diamond")
                || path.contains("emerald")
                || path.contains("ancient_debris")
                || path.contains("evoker")
                || path.contains("wither")
                || path.contains("ender_dragon")
                || path.contains("buried_treasure")
                || path.contains("name_tag")
                || path.contains("suspicious");
        }

        private static String resourcePath(ResourceLocation id) {
            String raw = id.toString();
            int colon = raw.indexOf(':');
            return colon >= 0 ? raw.substring(colon + 1) : raw;
        }

        private static double round2(double value) {
            return Math.round(value * 100.0D) / 100.0D;
        }
    }

    public static final class PerksConfigLoader {
        private final Map<String, List<SkillBranch>> skillTrees;

        private PerksConfigLoader(Map<String, List<SkillBranch>> skillTrees) {
            this.skillTrees = skillTrees;
        }

        public static PerksConfigLoader load(Path path) {
            if (!Files.exists(path)) {
                writeDefault(path);
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                return new PerksConfigLoader(parse(root));
            } catch (Exception e) {
                AdvancedJobsMod.LOGGER.error("Failed to load perks config, using defaults", e);
                return new PerksConfigLoader(defaultTrees());
            }
        }

        public List<SkillBranch> treeFor(String jobId) {
            return skillTrees.getOrDefault(jobId, List.of());
        }

        private static Map<String, List<SkillBranch>> parse(JsonObject root) {
            Map<String, List<SkillBranch>> trees = new HashMap<>();
            JsonObject jobs = root.getAsJsonObject("jobs");
            for (String jobId : jobs.keySet()) {
                List<SkillBranch> branches = new ArrayList<>();
                for (JsonElement branchElement : jobs.getAsJsonArray(jobId)) {
                    JsonObject branchObj = branchElement.getAsJsonObject();
                    List<SkillNode> nodes = new ArrayList<>();
                    for (JsonElement nodeElement : branchObj.getAsJsonArray("nodes")) {
                        JsonObject nodeObj = nodeElement.getAsJsonObject();
                        nodes.add(new SkillNode(
                            nodeObj.get("id").getAsString(),
                            nodeObj.get("translationKey").getAsString(),
                            nodeObj.get("requiredLevel").getAsInt(),
                            nodeObj.get("cost").getAsInt(),
                            nodeObj.has("parentId") ? nodeObj.get("parentId").getAsString() : null,
                            nodeObj.get("effectType").getAsString(),
                            nodeObj.get("effectValue").getAsDouble()
                        ));
                    }
                    branches.add(new SkillBranch(branchObj.get("id").getAsString(), branchObj.get("translationKey").getAsString(), nodes));
                }
                trees.put(jobId, branches);
            }
            return trees;
        }

        private static void writeDefault(Path path) {
            JsonObject root = new JsonObject();
            JsonObject jobs = new JsonObject();
            for (Map.Entry<String, List<SkillBranch>> entry : defaultTrees().entrySet()) {
                JsonArray branches = new JsonArray();
                for (SkillBranch branch : entry.getValue()) {
                    JsonObject branchObj = new JsonObject();
                    branchObj.addProperty("id", branch.id());
                    branchObj.addProperty("translationKey", branch.translationKey());
                    JsonArray nodes = new JsonArray();
                    for (SkillNode node : branch.nodes()) {
                        JsonObject nodeObj = new JsonObject();
                        nodeObj.addProperty("id", node.id());
                        nodeObj.addProperty("translationKey", node.translationKey());
                        nodeObj.addProperty("requiredLevel", node.requiredLevel());
                        nodeObj.addProperty("cost", node.cost());
                        if (node.parentId() != null) {
                            nodeObj.addProperty("parentId", node.parentId());
                        }
                        nodeObj.addProperty("effectType", node.effectType());
                        nodeObj.addProperty("effectValue", node.effectValue());
                        nodes.add(nodeObj);
                    }
                    branchObj.add("nodes", nodes);
                    branches.add(branchObj);
                }
                jobs.add(entry.getKey(), branches);
            }
            root.add("jobs", jobs);
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            } catch (IOException e) {
                AdvancedJobsMod.LOGGER.error("Failed to write default perks config", e);
            }
        }

        private static Map<String, List<SkillBranch>> defaultTrees() {
            Map<String, List<SkillBranch>> trees = new HashMap<>();
            for (String jobId : JobsConfigLoader.defaultDefinitions().keySet()) {
                trees.put(jobId, defaultTreeFor(jobId));
            }
            return trees;
        }

        private static List<SkillBranch> defaultTreeFor(String jobId) {
            return switch (jobId) {
                case "miner" -> minerTree(jobId);
                case "deep_miner" -> deepMinerTree(jobId);
                case "lumberjack" -> lumberjackTree(jobId);
                case "farmer" -> starterFarmerTree(jobId);
                case "harvester" -> harvesterTree(jobId);
                case "animal_breeder" -> animalBreederTree(jobId);
                case "fisher" -> starterFisherTree(jobId);
                case "hunter" -> starterHunterTree(jobId);
                case "monster_slayer" -> monsterSlayerTree(jobId);
                case "guard" -> guardTree(jobId);
                case "bounty_hunter" -> bountyHunterTree(jobId);
                case "defender" -> defenderTree(jobId);
                case "boss_hunter" -> bossHunterTree(jobId);
                case "builder" -> starterBuilderTree(jobId);
                case "mason" -> masonTree(jobId);
                case "carpenter" -> carpenterTree(jobId);
                case "merchant" -> starterMerchantTree(jobId);
                case "alchemist" -> starterAlchemistTree(jobId);
                case "enchanter" -> starterEnchanterTree(jobId);
                case "explorer" -> starterExplorerTree(jobId);
                case "redstone_technician" -> redstoneTechnicianTree(jobId);
                case "blacksmith" -> starterBlacksmithTree(jobId);
                case "armorer" -> starterArmorerTree(jobId);
                case "cook" -> starterCookTree(jobId);
                case "engineer" -> starterEngineerTree(jobId);
                case "treasure_hunter" -> starterTreasureHunterTree(jobId);
                case "archaeologist" -> archaeologistTree(jobId);
                case "forester" -> foresterTree(jobId);
                case "shepherd" -> shepherdTree(jobId);
                case "beekeeper" -> beekeeperTree(jobId);
                case "sand_collector" -> sandCollectorTree(jobId);
                case "ice_harvester" -> iceHarvesterTree(jobId);
                case "quarry_worker" -> quarryWorkerTree(jobId);
                case "digger" -> diggerTree(jobId);
                case "herbalist" -> herbalistTree(jobId);
                default -> List.of(
                    branch(jobId, "income", "skill." + jobId + ".income", "salary_bonus"),
                    branch(jobId, "resource", "skill." + jobId + ".resource", "resource_bonus"),
                    branch(jobId, "utility", "skill." + jobId + ".utility", "utility_bonus")
                );
            };
        }

        private static List<SkillBranch> lumberjackTree(String jobId) {
            return List.of(
                branch(jobId, "income", "skill." + jobId + ".income", "salary_bonus"),
                customBranch("resource", "skill." + jobId + ".resource", jobId,
                    node("resource_1", "skill." + jobId + ".resource.1", 5, 1, null, "wood_bonus", 0.05D),
                    node("resource_2", "skill." + jobId + ".resource.2", 10, 1, "resource_1", "wood_bonus", 0.08D),
                    node("resource_3", "skill." + jobId + ".resource.3", 20, 1, "resource_2", "sapling_return", 0.10D),
                    node("resource_4", "skill." + jobId + ".resource.4", 35, 2, "resource_3", "wood_bonus", 0.14D),
                    node("resource_5", "skill." + jobId + ".resource.5", 50, 2, "resource_4", "sapling_return", 0.16D)
                ),
                branch(jobId, "utility", "skill." + jobId + ".utility", "forest_aura")
            );
        }

        private static List<SkillBranch> deepMinerTree(String jobId) {
            return List.of(
                customBranch("income", "skill." + jobId + ".income", jobId,
                    node("income_1", "skill." + jobId + ".income.1", 5, 1, null, "salary_bonus", 0.06D),
                    node("income_2", "skill." + jobId + ".income.2", 10, 1, "income_1", "salary_bonus", 0.11D),
                    node("income_3", "skill." + jobId + ".income.3", 20, 1, "income_2", "salary_bonus", 0.16D),
                    node("income_4", "skill." + jobId + ".income.4", 35, 2, "income_3", "salary_bonus", 0.22D),
                    node("income_5", "skill." + jobId + ".income.5", 50, 2, "income_4", "salary_bonus", 0.28D)
                ),
                customBranch("resource", "skill." + jobId + ".resource", jobId,
                    node("resource_1", "skill." + jobId + ".resource.1", 5, 1, null, "ore_bonus", 0.06D),
                    node("resource_2", "skill." + jobId + ".resource.2", 10, 1, "resource_1", "rare_gem_bonus", 0.08D),
                    node("resource_3", "skill." + jobId + ".resource.3", 20, 1, "resource_2", "auto_smelt", 1.0D),
                    node("resource_4", "skill." + jobId + ".resource.4", 35, 2, "resource_3", "ore_bonus", 0.14D),
                    node("resource_5", "skill." + jobId + ".resource.5", 50, 2, "resource_4", "rare_gem_bonus", 0.14D)
                ),
                customBranch("utility", "skill." + jobId + ".utility", jobId,
                    node("utility_1", "skill." + jobId + ".utility.1", 5, 1, null, "mine_aura", 0.06D),
                    node("utility_2", "skill." + jobId + ".utility.2", 15, 1, "utility_1", "mine_aura", 0.10D),
                    node("utility_3", "skill." + jobId + ".utility.3", 25, 1, "utility_2", "fall_guard", 0.15D),
                    node("utility_4", "skill." + jobId + ".utility.4", 35, 2, "utility_3", "mine_aura", 0.18D),
                    node("utility_5", "skill." + jobId + ".utility.5", 50, 2, "utility_4", "ore_vision", 1.0D)
                )
            );
        }

        private static List<SkillBranch> starterFarmerTree(String jobId) {
            return List.of(
                branch(jobId, "income", "skill." + jobId + ".income", "salary_bonus"),
                customBranch("resource", "skill." + jobId + ".resource", jobId,
                    node("resource_1", "skill." + jobId + ".resource.1", 5, 1, null, "crop_bonus", 0.05D),
                    node("resource_2", "skill." + jobId + ".resource.2", 10, 1, "resource_1", "seed_keep", 0.08D),
                    node("resource_3", "skill." + jobId + ".resource.3", 20, 1, "resource_2", "crop_bonus", 0.12D),
                    node("resource_4", "skill." + jobId + ".resource.4", 35, 2, "resource_3", "seed_keep", 0.12D),
                    node("resource_5", "skill." + jobId + ".resource.5", 50, 2, "resource_4", "crop_bonus", 0.18D)
                ),
                branch(jobId, "utility", "skill." + jobId + ".utility", "farm_aura")
            );
        }

        private static List<SkillBranch> harvesterTree(String jobId) {
            return List.of(
                branch(jobId, "income", "skill." + jobId + ".income", "salary_bonus"),
                customBranch("resource", "skill." + jobId + ".resource", jobId,
                    node("resource_1", "skill." + jobId + ".resource.1", 5, 1, null, "crop_bonus", 0.06D),
                    node("resource_2", "skill." + jobId + ".resource.2", 10, 1, "resource_1", "seed_keep", 0.08D),
                    node("resource_3", "skill." + jobId + ".resource.3", 20, 1, "resource_2", "crop_bonus", 0.13D),
                    node("resource_4", "skill." + jobId + ".resource.4", 35, 2, "resource_3", "crop_bonus", 0.16D),
                    node("resource_5", "skill." + jobId + ".resource.5", 50, 2, "resource_4", "seed_keep", 0.16D)
                ),
                branch(jobId, "utility", "skill." + jobId + ".utility", "farm_aura")
            );
        }

        private static List<SkillBranch> animalBreederTree(String jobId) {
            return List.of(
                branch(jobId, "income", "skill." + jobId + ".income", "salary_bonus"),
                customBranch("resource", "skill." + jobId + ".resource", jobId,
                    node("resource_1", "skill." + jobId + ".resource.1", 5, 1, null, "breed_bonus", 0.05D),
                    node("resource_2", "skill." + jobId + ".resource.2", 10, 1, "resource_1", "breed_bonus", 0.08D),
                    node("resource_3", "skill." + jobId + ".resource.3", 20, 1, "resource_2", "honey_bonus", 0.08D),
                    node("resource_4", "skill." + jobId + ".resource.4", 35, 2, "resource_3", "breed_bonus", 0.14D),
                    node("resource_5", "skill." + jobId + ".resource.5", 50, 2, "resource_4", "breed_bonus", 0.18D)
                ),
                branch(jobId, "utility", "skill." + jobId + ".utility", "pasture_aura")
            );
        }

        private static List<SkillBranch> starterFisherTree(String jobId) {
            return List.of(
                branch(jobId, "income", "skill." + jobId + ".income", "salary_bonus"),
                customBranch("resource", "skill." + jobId + ".resource", jobId,
                    node("resource_1", "skill." + jobId + ".resource.1", 5, 1, null, "fish_bonus", 0.05D),
                    node("resource_2", "skill." + jobId + ".resource.2", 10, 1, "resource_1", "treasure_chance", 0.08D),
                    node("resource_3", "skill." + jobId + ".resource.3", 20, 1, "resource_2", "fish_bonus", 0.12D),
                    node("resource_4", "skill." + jobId + ".resource.4", 35, 2, "resource_3", "junk_reduction", 0.12D),
                    node("resource_5", "skill." + jobId + ".resource.5", 50, 2, "resource_4", "treasure_chance", 0.16D)
                ),
                branch(jobId, "utility", "skill." + jobId + ".utility", "water_aura")
            );
        }

        private static List<SkillBranch> starterHunterTree(String jobId) {
            return List.of(
                branch(jobId, "income", "skill." + jobId + ".income", "salary_bonus"),
                customBranch("resource", "skill." + jobId + ".resource", jobId,
                    node("resource_1", "skill." + jobId + ".resource.1", 5, 1, null, "combat_bonus", 0.05D),
                    node("resource_2", "skill." + jobId + ".resource.2", 10, 1, "resource_1", "loot_bonus", 0.08D),
                    node("resource_3", "skill." + jobId + ".resource.3", 20, 1, "resource_2", "combat_bonus", 0.12D),
                    node("resource_4", "skill." + jobId + ".resource.4", 35, 2, "resource_3", "loot_bonus", 0.12D),
                    node("resource_5", "skill." + jobId + ".resource.5", 50, 2, "resource_4", "combat_bonus", 0.18D)
                ),
                branch(jobId, "utility", "skill." + jobId + ".utility", "combat_aura")
            );
        }

        private static List<SkillBranch> monsterSlayerTree(String jobId) {
            return List.of(
                branch(jobId, "income", "skill." + jobId + ".income", "salary_bonus"),
                customBranch("resource", "skill." + jobId + ".resource", jobId,
                    node("resource_1", "skill." + jobId + ".resource.1", 5, 1, null, "combat_bonus", 0.05D),
                    node("resource_2", "skill." + jobId + ".resource.2", 10, 1, "resource_1", "loot_bonus", 0.08D),
                    node("resource_3", "skill." + jobId + ".resource.3", 20, 1, "resource_2", "combat_bonus", 0.12D),
                    node("resource_4", "skill." + jobId + ".resource.4", 35, 2, "resource_3", "elite_tracker", 1.0D),
                    node("resource_5", "skill." + jobId + ".resource.5", 50, 2, "resource_4", "loot_bonus", 0.16D)
                ),
                branch(jobId, "utility", "skill." + jobId + ".utility", "combat_aura")
            );
        }

        private static List<SkillBranch> guardTree(String jobId) {
            return List.of(
                branch(jobId, "income", "skill." + jobId + ".income", "salary_bonus"),
                customBranch("resource", "skill." + jobId + ".resource", jobId,
                    node("resource_1", "skill." + jobId + ".resource.1", 5, 1, null, "combat_bonus", 0.05D),
                    node("resource_2", "skill." + jobId + ".resource.2", 10, 1, "resource_1", "loot_bonus", 0.08D),
                    node("resource_3", "skill." + jobId + ".resource.3", 20, 1, "resource_2", "combat_bonus", 0.12D),
                    node("resource_4", "skill." + jobId + ".resource.4", 35, 2, "resource_3", "combat_bonus", 0.16D),
                    node("resource_5", "skill." + jobId + ".resource.5", 50, 2, "resource_4", "loot_bonus", 0.16D)
                ),
                branch(jobId, "utility", "skill." + jobId + ".utility", "combat_aura")
            );
        }

        private static List<SkillBranch> bountyHunterTree(String jobId) {
            return List.of(
                branch(jobId, "income", "skill." + jobId + ".income", "salary_bonus"),
                customBranch("resource", "skill." + jobId + ".resource", jobId,
                    node("resource_1", "skill." + jobId + ".resource.1", 5, 1, null, "combat_bonus", 0.05D),
                    node("resource_2", "skill." + jobId + ".resource.2", 10, 1, "resource_1", "elite_tracker", 1.0D),
                    node("resource_3", "skill." + jobId + ".resource.3", 20, 1, "resource_2", "loot_bonus", 0.10D),
                    node("resource_4", "skill." + jobId + ".resource.4", 35, 2, "resource_3", "combat_bonus", 0.16D),
                    node("resource_5", "skill." + jobId + ".resource.5", 50, 2, "resource_4", "loot_bonus", 0.18D)
                ),
                branch(jobId, "utility", "skill." + jobId + ".utility", "combat_aura")
            );
        }

        private static List<SkillBranch> defenderTree(String jobId) {
            return List.of(
                branch(jobId, "income", "skill." + jobId + ".income", "salary_bonus"),
                customBranch("resource", "skill." + jobId + ".resource", jobId,
                    node("resource_1", "skill." + jobId + ".resource.1", 5, 1, null, "combat_bonus", 0.05D),
                    node("resource_2", "skill." + jobId + ".resource.2", 10, 1, "resource_1", "combat_bonus", 0.08D),
                    node("resource_3", "skill." + jobId + ".resource.3", 20, 1, "resource_2", "loot_bonus", 0.08D),
                    node("resource_4", "skill." + jobId + ".resource.4", 35, 2, "resource_3", "combat_bonus", 0.14D),
                    node("resource_5", "skill." + jobId + ".resource.5", 50, 2, "resource_4", "combat_bonus", 0.18D)
                ),
                branch(jobId, "utility", "skill." + jobId + ".utility", "combat_aura")
            );
        }

        private static List<SkillBranch> bossHunterTree(String jobId) {
            return List.of(
                customBranch("income", "skill." + jobId + ".income", jobId,
                    node("income_1", "skill." + jobId + ".income.1", 5, 1, null, "salary_bonus", 0.08D),
                    node("income_2", "skill." + jobId + ".income.2", 10, 1, "income_1", "salary_bonus", 0.12D),
                    node("income_3", "skill." + jobId + ".income.3", 20, 1, "income_2", "salary_bonus", 0.18D),
                    node("income_4", "skill." + jobId + ".income.4", 35, 2, "income_3", "salary_bonus", 0.24D),
                    node("income_5", "skill." + jobId + ".income.5", 50, 2, "income_4", "salary_bonus", 0.30D)
                ),
                customBranch("resource", "skill." + jobId + ".resource", jobId,
                    node("resource_1", "skill." + jobId + ".resource.1", 5, 1, null, "loot_bonus", 0.08D),
                    node("resource_2", "skill." + jobId + ".resource.2", 10, 1, "resource_1", "elite_tracker", 1.0D),
                    node("resource_3", "skill." + jobId + ".resource.3", 20, 1, "resource_2", "combat_bonus", 0.12D),
                    node("resource_4", "skill." + jobId + ".resource.4", 35, 2, "resource_3", "loot_bonus", 0.16D),
                    node("resource_5", "skill." + jobId + ".resource.5", 50, 2, "resource_4", "combat_bonus", 0.20D)
                ),
                branch(jobId, "utility", "skill." + jobId + ".utility", "combat_aura")
            );
        }

        private static List<SkillBranch> starterBuilderTree(String jobId) {
            return List.of(
                branch(jobId, "income", "skill." + jobId + ".income", "salary_bonus"),
                customBranch("resource", "skill." + jobId + ".resource", jobId,
                    node("resource_1", "skill." + jobId + ".resource.1", 5, 1, null, "build_bonus", 0.05D),
                    node("resource_2", "skill." + jobId + ".resource.2", 10, 1, "resource_1", "block_refund", 0.08D),
                    node("resource_3", "skill." + jobId + ".resource.3", 20, 1, "resource_2", "build_bonus", 0.12D),
                    node("resource_4", "skill." + jobId + ".resource.4", 35, 2, "resource_3", "decor_bonus", 0.10D),
                    node("resource_5", "skill." + jobId + ".resource.5", 50, 2, "resource_4", "build_bonus", 0.18D)
                ),
                branch(jobId, "utility", "skill." + jobId + ".utility", "builder_aura")
            );
        }

        private static List<SkillBranch> masonTree(String jobId) {
            return List.of(
                branch(jobId, "income", "skill." + jobId + ".income", "salary_bonus"),
                customBranch("resource", "skill." + jobId + ".resource", jobId,
                    node("resource_1", "skill." + jobId + ".resource.1", 5, 1, null, "build_bonus", 0.05D),
                    node("resource_2", "skill." + jobId + ".resource.2", 10, 1, "resource_1", "block_refund", 0.08D),
                    node("resource_3", "skill." + jobId + ".resource.3", 20, 1, "resource_2", "build_bonus", 0.12D),
                    node("resource_4", "skill." + jobId + ".resource.4", 35, 2, "resource_3", "decor_bonus", 0.10D),
                    node("resource_5", "skill." + jobId + ".resource.5", 50, 2, "resource_4", "build_bonus", 0.17D)
                ),
                branch(jobId, "utility", "skill." + jobId + ".utility", "builder_aura")
            );
        }

        private static List<SkillBranch> carpenterTree(String jobId) {
            return List.of(
                branch(jobId, "income", "skill." + jobId + ".income", "salary_bonus"),
                customBranch("resource", "skill." + jobId + ".resource", jobId,
                    node("resource_1", "skill." + jobId + ".resource.1", 5, 1, null, "build_bonus", 0.05D),
                    node("resource_2", "skill." + jobId + ".resource.2", 10, 1, "resource_1", "block_refund", 0.08D),
                    node("resource_3", "skill." + jobId + ".resource.3", 20, 1, "resource_2", "build_bonus", 0.12D),
                    node("resource_4", "skill." + jobId + ".resource.4", 35, 2, "resource_3", "decor_bonus", 0.10D),
                    node("resource_5", "skill." + jobId + ".resource.5", 50, 2, "resource_4", "build_bonus", 0.17D)
                ),
                branch(jobId, "utility", "skill." + jobId + ".utility", "builder_aura")
            );
        }

        private static List<SkillBranch> starterMerchantTree(String jobId) {
            return List.of(
                branch(jobId, "income", "skill." + jobId + ".income", "salary_bonus"),
                customBranch("resource", "skill." + jobId + ".resource", jobId,
                    node("resource_1", "skill." + jobId + ".resource.1", 5, 1, null, "trade_bonus", 0.05D),
                    node("resource_2", "skill." + jobId + ".resource.2", 10, 1, "resource_1", "discount_bonus", 0.08D),
                    node("resource_3", "skill." + jobId + ".resource.3", 20, 1, "resource_2", "trade_bonus", 0.12D),
                    node("resource_4", "skill." + jobId + ".resource.4", 35, 2, "resource_3", "emerald_bonus", 0.10D),
                    node("resource_5", "skill." + jobId + ".resource.5", 50, 2, "resource_4", "trade_bonus", 0.18D)
                ),
                branch(jobId, "utility", "skill." + jobId + ".utility", "merchant_aura")
            );
        }

        private static List<SkillBranch> starterAlchemistTree(String jobId) {
            return List.of(
                branch(jobId, "income", "skill." + jobId + ".income", "salary_bonus"),
                customBranch("resource", "skill." + jobId + ".resource", jobId,
                    node("resource_1", "skill." + jobId + ".resource.1", 5, 1, null, "magic_bonus", 0.05D),
                    node("resource_2", "skill." + jobId + ".resource.2", 10, 1, "resource_1", "ingredient_save", 0.08D),
                    node("resource_3", "skill." + jobId + ".resource.3", 20, 1, "resource_2", "magic_bonus", 0.12D),
                    node("resource_4", "skill." + jobId + ".resource.4", 35, 2, "resource_3", "rare_magic_bonus", 0.10D),
                    node("resource_5", "skill." + jobId + ".resource.5", 50, 2, "resource_4", "ingredient_save", 0.16D)
                ),
                branch(jobId, "utility", "skill." + jobId + ".utility", "magic_aura")
            );
        }

        private static List<SkillBranch> starterEnchanterTree(String jobId) {
            return List.of(
                branch(jobId, "income", "skill." + jobId + ".income", "salary_bonus"),
                customBranch("resource", "skill." + jobId + ".resource", jobId,
                    node("resource_1", "skill." + jobId + ".resource.1", 5, 1, null, "magic_bonus", 0.05D),
                    node("resource_2", "skill." + jobId + ".resource.2", 10, 1, "resource_1", "ingredient_save", 0.08D),
                    node("resource_3", "skill." + jobId + ".resource.3", 20, 1, "resource_2", "rare_magic_bonus", 0.08D),
                    node("resource_4", "skill." + jobId + ".resource.4", 35, 2, "resource_3", "magic_bonus", 0.14D),
                    node("resource_5", "skill." + jobId + ".resource.5", 50, 2, "resource_4", "rare_magic_bonus", 0.14D)
                ),
                branch(jobId, "utility", "skill." + jobId + ".utility", "magic_aura")
            );
        }

        private static List<SkillBranch> starterExplorerTree(String jobId) {
            return List.of(
                branch(jobId, "income", "skill." + jobId + ".income", "salary_bonus"),
                customBranch("resource", "skill." + jobId + ".resource", jobId,
                    node("resource_1", "skill." + jobId + ".resource.1", 5, 1, null, "explore_bonus", 0.05D),
                    node("resource_2", "skill." + jobId + ".resource.2", 10, 1, "resource_1", "cache_finder", 0.08D),
                    node("resource_3", "skill." + jobId + ".resource.3", 20, 1, "resource_2", "explore_bonus", 0.12D),
                    node("resource_4", "skill." + jobId + ".resource.4", 35, 2, "resource_3", "artifact_bonus", 0.10D),
                    node("resource_5", "skill." + jobId + ".resource.5", 50, 2, "resource_4", "cache_finder", 0.16D)
                ),
                branch(jobId, "utility", "skill." + jobId + ".utility", "explore_aura")
            );
        }

        private static List<SkillBranch> starterBlacksmithTree(String jobId) {
            return List.of(
                branch(jobId, "income", "skill." + jobId + ".income", "salary_bonus"),
                customBranch("resource", "skill." + jobId + ".resource", jobId,
                    node("resource_1", "skill." + jobId + ".resource.1", 5, 1, null, "craft_bonus", 0.05D),
                    node("resource_2", "skill." + jobId + ".resource.2", 10, 1, "resource_1", "ingredient_save", 0.08D),
                    node("resource_3", "skill." + jobId + ".resource.3", 20, 1, "resource_2", "craft_bonus", 0.12D),
                    node("resource_4", "skill." + jobId + ".resource.4", 35, 2, "resource_3", "craft_bonus", 0.16D),
                    node("resource_5", "skill." + jobId + ".resource.5", 50, 2, "resource_4", "ingredient_save", 0.16D)
                ),
                branch(jobId, "utility", "skill." + jobId + ".utility", "craft_aura")
            );
        }

        private static List<SkillBranch> starterArmorerTree(String jobId) {
            return List.of(
                branch(jobId, "income", "skill." + jobId + ".income", "salary_bonus"),
                customBranch("resource", "skill." + jobId + ".resource", jobId,
                    node("resource_1", "skill." + jobId + ".resource.1", 5, 1, null, "craft_bonus", 0.05D),
                    node("resource_2", "skill." + jobId + ".resource.2", 10, 1, "resource_1", "ingredient_save", 0.08D),
                    node("resource_3", "skill." + jobId + ".resource.3", 20, 1, "resource_2", "craft_bonus", 0.12D),
                    node("resource_4", "skill." + jobId + ".resource.4", 35, 2, "resource_3", "craft_bonus", 0.16D),
                    node("resource_5", "skill." + jobId + ".resource.5", 50, 2, "resource_4", "ingredient_save", 0.16D)
                ),
                branch(jobId, "utility", "skill." + jobId + ".utility", "craft_aura")
            );
        }

        private static List<SkillBranch> starterCookTree(String jobId) {
            return List.of(
                branch(jobId, "income", "skill." + jobId + ".income", "salary_bonus"),
                customBranch("resource", "skill." + jobId + ".resource", jobId,
                    node("resource_1", "skill." + jobId + ".resource.1", 5, 1, null, "craft_bonus", 0.05D),
                    node("resource_2", "skill." + jobId + ".resource.2", 10, 1, "resource_1", "ingredient_save", 0.08D),
                    node("resource_3", "skill." + jobId + ".resource.3", 20, 1, "resource_2", "craft_bonus", 0.12D),
                    node("resource_4", "skill." + jobId + ".resource.4", 35, 2, "resource_3", "ingredient_save", 0.12D),
                    node("resource_5", "skill." + jobId + ".resource.5", 50, 2, "resource_4", "craft_bonus", 0.18D)
                ),
                branch(jobId, "utility", "skill." + jobId + ".utility", "craft_aura")
            );
        }

        private static List<SkillBranch> starterEngineerTree(String jobId) {
            return List.of(
                branch(jobId, "income", "skill." + jobId + ".income", "salary_bonus"),
                customBranch("resource", "skill." + jobId + ".resource", jobId,
                    node("resource_1", "skill." + jobId + ".resource.1", 5, 1, null, "craft_bonus", 0.05D),
                    node("resource_2", "skill." + jobId + ".resource.2", 10, 1, "resource_1", "craft_bonus", 0.08D),
                    node("resource_3", "skill." + jobId + ".resource.3", 20, 1, "resource_2", "ingredient_save", 0.08D),
                    node("resource_4", "skill." + jobId + ".resource.4", 35, 2, "resource_3", "craft_bonus", 0.14D),
                    node("resource_5", "skill." + jobId + ".resource.5", 50, 2, "resource_4", "craft_bonus", 0.18D)
                ),
                branch(jobId, "utility", "skill." + jobId + ".utility", "craft_aura")
            );
        }

        private static List<SkillBranch> redstoneTechnicianTree(String jobId) {
            return List.of(
                branch(jobId, "income", "skill." + jobId + ".income", "salary_bonus"),
                customBranch("resource", "skill." + jobId + ".resource", jobId,
                    node("resource_1", "skill." + jobId + ".resource.1", 5, 1, null, "craft_bonus", 0.05D),
                    node("resource_2", "skill." + jobId + ".resource.2", 10, 1, "resource_1", "craft_bonus", 0.08D),
                    node("resource_3", "skill." + jobId + ".resource.3", 20, 1, "resource_2", "ingredient_save", 0.08D),
                    node("resource_4", "skill." + jobId + ".resource.4", 35, 2, "resource_3", "craft_bonus", 0.14D),
                    node("resource_5", "skill." + jobId + ".resource.5", 50, 2, "resource_4", "craft_bonus", 0.18D)
                ),
                branch(jobId, "utility", "skill." + jobId + ".utility", "craft_aura")
            );
        }

        private static List<SkillBranch> starterTreasureHunterTree(String jobId) {
            return List.of(
                branch(jobId, "income", "skill." + jobId + ".income", "salary_bonus"),
                customBranch("resource", "skill." + jobId + ".resource", jobId,
                    node("resource_1", "skill." + jobId + ".resource.1", 5, 1, null, "explore_bonus", 0.05D),
                    node("resource_2", "skill." + jobId + ".resource.2", 10, 1, "resource_1", "treasure_chance", 0.08D),
                    node("resource_3", "skill." + jobId + ".resource.3", 20, 1, "resource_2", "cache_finder", 0.08D),
                    node("resource_4", "skill." + jobId + ".resource.4", 35, 2, "resource_3", "artifact_bonus", 0.10D),
                    node("resource_5", "skill." + jobId + ".resource.5", 50, 2, "resource_4", "treasure_chance", 0.16D)
                ),
                branch(jobId, "utility", "skill." + jobId + ".utility", "explore_aura")
            );
        }

        private static List<SkillBranch> archaeologistTree(String jobId) {
            return List.of(
                branch(jobId, "income", "skill." + jobId + ".income", "salary_bonus"),
                customBranch("resource", "skill." + jobId + ".resource", jobId,
                    node("resource_1", "skill." + jobId + ".resource.1", 5, 1, null, "explore_bonus", 0.05D),
                    node("resource_2", "skill." + jobId + ".resource.2", 10, 1, "resource_1", "artifact_bonus", 0.08D),
                    node("resource_3", "skill." + jobId + ".resource.3", 20, 1, "resource_2", "cache_finder", 0.08D),
                    node("resource_4", "skill." + jobId + ".resource.4", 35, 2, "resource_3", "artifact_bonus", 0.12D),
                    node("resource_5", "skill." + jobId + ".resource.5", 50, 2, "resource_4", "explore_bonus", 0.18D)
                ),
                branch(jobId, "utility", "skill." + jobId + ".utility", "explore_aura")
            );
        }

        private static List<SkillBranch> shepherdTree(String jobId) {
            return List.of(
                branch(jobId, "income", "skill." + jobId + ".income", "salary_bonus"),
                customBranch("resource", "skill." + jobId + ".resource", jobId,
                    node("resource_1", "skill." + jobId + ".resource.1", 5, 1, null, "breed_bonus", 0.05D),
                    node("resource_2", "skill." + jobId + ".resource.2", 10, 1, "resource_1", "breed_bonus", 0.08D),
                    node("resource_3", "skill." + jobId + ".resource.3", 20, 1, "resource_2", "honey_bonus", 0.08D),
                    node("resource_4", "skill." + jobId + ".resource.4", 35, 2, "resource_3", "breed_bonus", 0.14D),
                    node("resource_5", "skill." + jobId + ".resource.5", 50, 2, "resource_4", "breed_bonus", 0.18D)
                ),
                branch(jobId, "utility", "skill." + jobId + ".utility", "pasture_aura")
            );
        }

        private static List<SkillBranch> beekeeperTree(String jobId) {
            return List.of(
                branch(jobId, "income", "skill." + jobId + ".income", "salary_bonus"),
                customBranch("resource", "skill." + jobId + ".resource", jobId,
                    node("resource_1", "skill." + jobId + ".resource.1", 5, 1, null, "breed_bonus", 0.05D),
                    node("resource_2", "skill." + jobId + ".resource.2", 10, 1, "resource_1", "honey_bonus", 0.08D),
                    node("resource_3", "skill." + jobId + ".resource.3", 20, 1, "resource_2", "breed_bonus", 0.10D),
                    node("resource_4", "skill." + jobId + ".resource.4", 35, 2, "resource_3", "honey_bonus", 0.12D),
                    node("resource_5", "skill." + jobId + ".resource.5", 50, 2, "resource_4", "honey_bonus", 0.18D)
                ),
                branch(jobId, "utility", "skill." + jobId + ".utility", "pasture_aura")
            );
        }

        private static List<SkillBranch> sandCollectorTree(String jobId) {
            return List.of(
                branch(jobId, "income", "skill." + jobId + ".income", "salary_bonus"),
                customBranch("resource", "skill." + jobId + ".resource", jobId,
                    node("resource_1", "skill." + jobId + ".resource.1", 5, 1, null, "explore_bonus", 0.05D),
                    node("resource_2", "skill." + jobId + ".resource.2", 10, 1, "resource_1", "explore_bonus", 0.08D),
                    node("resource_3", "skill." + jobId + ".resource.3", 20, 1, "resource_2", "cache_finder", 0.08D),
                    node("resource_4", "skill." + jobId + ".resource.4", 35, 2, "resource_3", "artifact_bonus", 0.10D),
                    node("resource_5", "skill." + jobId + ".resource.5", 50, 2, "resource_4", "explore_bonus", 0.18D)
                ),
                branch(jobId, "utility", "skill." + jobId + ".utility", "explore_aura")
            );
        }

        private static List<SkillBranch> iceHarvesterTree(String jobId) {
            return List.of(
                branch(jobId, "income", "skill." + jobId + ".income", "salary_bonus"),
                customBranch("resource", "skill." + jobId + ".resource", jobId,
                    node("resource_1", "skill." + jobId + ".resource.1", 5, 1, null, "explore_bonus", 0.05D),
                    node("resource_2", "skill." + jobId + ".resource.2", 10, 1, "resource_1", "explore_bonus", 0.08D),
                    node("resource_3", "skill." + jobId + ".resource.3", 20, 1, "resource_2", "cache_finder", 0.08D),
                    node("resource_4", "skill." + jobId + ".resource.4", 35, 2, "resource_3", "artifact_bonus", 0.10D),
                    node("resource_5", "skill." + jobId + ".resource.5", 50, 2, "resource_4", "explore_bonus", 0.18D)
                ),
                branch(jobId, "utility", "skill." + jobId + ".utility", "explore_aura")
            );
        }

        private static List<SkillBranch> quarryWorkerTree(String jobId) {
            return List.of(
                branch(jobId, "income", "skill." + jobId + ".income", "salary_bonus"),
                customBranch("resource", "skill." + jobId + ".resource", jobId,
                    node("resource_1", "skill." + jobId + ".resource.1", 5, 1, null, "explore_bonus", 0.05D),
                    node("resource_2", "skill." + jobId + ".resource.2", 10, 1, "resource_1", "explore_bonus", 0.08D),
                    node("resource_3", "skill." + jobId + ".resource.3", 20, 1, "resource_2", "cache_finder", 0.08D),
                    node("resource_4", "skill." + jobId + ".resource.4", 35, 2, "resource_3", "artifact_bonus", 0.10D),
                    node("resource_5", "skill." + jobId + ".resource.5", 50, 2, "resource_4", "explore_bonus", 0.18D)
                ),
                branch(jobId, "utility", "skill." + jobId + ".utility", "explore_aura")
            );
        }

        private static List<SkillBranch> diggerTree(String jobId) {
            return List.of(
                branch(jobId, "income", "skill." + jobId + ".income", "salary_bonus"),
                customBranch("resource", "skill." + jobId + ".resource", jobId,
                    node("resource_1", "skill." + jobId + ".resource.1", 5, 1, null, "explore_bonus", 0.05D),
                    node("resource_2", "skill." + jobId + ".resource.2", 10, 1, "resource_1", "explore_bonus", 0.08D),
                    node("resource_3", "skill." + jobId + ".resource.3", 20, 1, "resource_2", "cache_finder", 0.08D),
                    node("resource_4", "skill." + jobId + ".resource.4", 35, 2, "resource_3", "artifact_bonus", 0.10D),
                    node("resource_5", "skill." + jobId + ".resource.5", 50, 2, "resource_4", "explore_bonus", 0.18D)
                ),
                branch(jobId, "utility", "skill." + jobId + ".utility", "explore_aura")
            );
        }

        private static List<SkillBranch> herbalistTree(String jobId) {
            return List.of(
                branch(jobId, "income", "skill." + jobId + ".income", "salary_bonus"),
                customBranch("resource", "skill." + jobId + ".resource", jobId,
                    node("resource_1", "skill." + jobId + ".resource.1", 5, 1, null, "crop_bonus", 0.05D),
                    node("resource_2", "skill." + jobId + ".resource.2", 10, 1, "resource_1", "seed_keep", 0.08D),
                    node("resource_3", "skill." + jobId + ".resource.3", 20, 1, "resource_2", "crop_bonus", 0.12D),
                    node("resource_4", "skill." + jobId + ".resource.4", 35, 2, "resource_3", "ingredient_save", 0.10D),
                    node("resource_5", "skill." + jobId + ".resource.5", 50, 2, "resource_4", "crop_bonus", 0.18D)
                ),
                branch(jobId, "utility", "skill." + jobId + ".utility", "farm_aura")
            );
        }

        private static List<SkillBranch> minerTree(String jobId) {
            return List.of(
                customBranch("income", "skill." + jobId + ".income", jobId,
                    node("income_1", "skill." + jobId + ".income.1", 5, 1, null, "salary_bonus", 0.05D),
                    node("income_2", "skill." + jobId + ".income.2", 10, 1, "income_1", "salary_bonus", 0.10D),
                    node("income_3", "skill." + jobId + ".income.3", 20, 1, "income_2", "salary_bonus", 0.15D),
                    node("income_4", "skill." + jobId + ".income.4", 35, 2, "income_3", "salary_bonus", 0.20D),
                    node("income_5", "skill." + jobId + ".income.5", 50, 2, "income_4", "salary_bonus", 0.25D)
                ),
                customBranch("resource", "skill." + jobId + ".resource", jobId,
                    node("resource_1", "skill." + jobId + ".resource.1", 5, 1, null, "ore_bonus", 0.05D),
                    node("resource_2", "skill." + jobId + ".resource.2", 10, 1, "resource_1", "ore_bonus", 0.08D),
                    node("resource_3", "skill." + jobId + ".resource.3", 20, 1, "resource_2", "auto_smelt", 1.0D),
                    node("resource_4", "skill." + jobId + ".resource.4", 35, 2, "resource_3", "ore_bonus", 0.12D),
                    node("resource_5", "skill." + jobId + ".resource.5", 50, 2, "resource_4", "rare_gem_bonus", 0.10D)
                ),
                customBranch("utility", "skill." + jobId + ".utility", jobId,
                    node("utility_1", "skill." + jobId + ".utility.1", 5, 1, null, "mine_aura", 0.05D),
                    node("utility_2", "skill." + jobId + ".utility.2", 15, 1, "utility_1", "mine_aura", 0.10D),
                    node("utility_3", "skill." + jobId + ".utility.3", 25, 1, "utility_2", "fall_guard", 0.15D),
                    node("utility_4", "skill." + jobId + ".utility.4", 35, 2, "utility_3", "mine_aura", 0.18D),
                    node("utility_5", "skill." + jobId + ".utility.5", 50, 2, "utility_4", "ore_vision", 1.0D)
                )
            );
        }

        private static List<SkillBranch> foresterTree(String jobId) {
            return List.of(
                branch(jobId, "income", "skill." + jobId + ".income", "salary_bonus"),
                customBranch("resource", "skill." + jobId + ".resource", jobId,
                    node("resource_1", "skill." + jobId + ".resource.1", 5, 1, null, "wood_bonus", 0.05D),
                    node("resource_2", "skill." + jobId + ".resource.2", 10, 1, "resource_1", "sapling_return", 0.10D),
                    node("resource_3", "skill." + jobId + ".resource.3", 20, 1, "resource_2", "wood_bonus", 0.12D),
                    node("resource_4", "skill." + jobId + ".resource.4", 35, 2, "resource_3", "wood_bonus", 0.16D),
                    node("resource_5", "skill." + jobId + ".resource.5", 50, 2, "resource_4", "forest_cache", 0.12D)
                ),
                branch(jobId, "utility", "skill." + jobId + ".utility", "forest_aura")
            );
        }

        private static List<SkillBranch> farmerTree(String jobId) {
            return List.of(
                branch(jobId, "income", "skill." + jobId + ".income", "salary_bonus"),
                customBranch("resource", "skill." + jobId + ".resource", jobId,
                    node("resource_1", "skill." + jobId + ".resource.1", 5, 1, null, "crop_bonus", 0.05D),
                    node("resource_2", "skill." + jobId + ".resource.2", 10, 1, "resource_1", "seed_keep", 0.10D),
                    node("resource_3", "skill." + jobId + ".resource.3", 20, 1, "resource_2", "crop_bonus", 0.12D),
                    node("resource_4", "skill." + jobId + ".resource.4", 35, 2, "resource_3", "crop_bonus", 0.15D),
                    node("resource_5", "skill." + jobId + ".resource.5", 50, 2, "resource_4", "rare_crop_bonus", 0.10D)
                ),
                branch(jobId, "utility", "skill." + jobId + ".utility", "farm_aura")
            );
        }

        private static List<SkillBranch> breederTree(String jobId) {
            return List.of(
                branch(jobId, "income", "skill." + jobId + ".income", "salary_bonus"),
                customBranch("resource", "skill." + jobId + ".resource", jobId,
                    node("resource_1", "skill." + jobId + ".resource.1", 5, 1, null, "breed_bonus", 0.05D),
                    node("resource_2", "skill." + jobId + ".resource.2", 10, 1, "resource_1", "offspring_bonus", 0.08D),
                    node("resource_3", "skill." + jobId + ".resource.3", 20, 1, "resource_2", "breed_bonus", 0.12D),
                    node("resource_4", "skill." + jobId + ".resource.4", 35, 2, "resource_3", "honey_bonus", 0.10D),
                    node("resource_5", "skill." + jobId + ".resource.5", 50, 2, "resource_4", "breed_bonus", 0.16D)
                ),
                branch(jobId, "utility", "skill." + jobId + ".utility", "pasture_aura")
            );
        }

        private static List<SkillBranch> fisherTree(String jobId) {
            return List.of(
                branch(jobId, "income", "skill." + jobId + ".income", "salary_bonus"),
                customBranch("resource", "skill." + jobId + ".resource", jobId,
                    node("resource_1", "skill." + jobId + ".resource.1", 5, 1, null, "fish_bonus", 0.05D),
                    node("resource_2", "skill." + jobId + ".resource.2", 10, 1, "resource_1", "treasure_chance", 0.08D),
                    node("resource_3", "skill." + jobId + ".resource.3", 20, 1, "resource_2", "fish_bonus", 0.12D),
                    node("resource_4", "skill." + jobId + ".resource.4", 35, 2, "resource_3", "junk_reduction", 0.10D),
                    node("resource_5", "skill." + jobId + ".resource.5", 50, 2, "resource_4", "fish_bonus", 0.16D)
                ),
                branch(jobId, "utility", "skill." + jobId + ".utility", "water_aura")
            );
        }

        private static List<SkillBranch> combatTree(String jobId) {
            return List.of(
                branch(jobId, "income", "skill." + jobId + ".income", "salary_bonus"),
                customBranch("resource", "skill." + jobId + ".resource", jobId,
                    node("resource_1", "skill." + jobId + ".resource.1", 5, 1, null, "combat_bonus", 0.05D),
                    node("resource_2", "skill." + jobId + ".resource.2", 10, 1, "resource_1", "loot_bonus", 0.08D),
                    node("resource_3", "skill." + jobId + ".resource.3", 20, 1, "resource_2", "combat_bonus", 0.12D),
                    node("resource_4", "skill." + jobId + ".resource.4", 35, 2, "resource_3", "elite_tracker", 1.0D),
                    node("resource_5", "skill." + jobId + ".resource.5", 50, 2, "resource_4", "combat_bonus", 0.16D)
                ),
                branch(jobId, "utility", "skill." + jobId + ".utility", "combat_aura")
            );
        }

        private static List<SkillBranch> builderTree(String jobId) {
            return List.of(
                branch(jobId, "income", "skill." + jobId + ".income", "salary_bonus"),
                customBranch("resource", "skill." + jobId + ".resource", jobId,
                    node("resource_1", "skill." + jobId + ".resource.1", 5, 1, null, "build_bonus", 0.05D),
                    node("resource_2", "skill." + jobId + ".resource.2", 10, 1, "resource_1", "block_refund", 0.08D),
                    node("resource_3", "skill." + jobId + ".resource.3", 20, 1, "resource_2", "build_bonus", 0.12D),
                    node("resource_4", "skill." + jobId + ".resource.4", 35, 2, "resource_3", "decor_bonus", 0.10D),
                    node("resource_5", "skill." + jobId + ".resource.5", 50, 2, "resource_4", "build_bonus", 0.16D)
                ),
                branch(jobId, "utility", "skill." + jobId + ".utility", "builder_aura")
            );
        }

        private static List<SkillBranch> crafterTree(String jobId) {
            return List.of(
                branch(jobId, "income", "skill." + jobId + ".income", "salary_bonus"),
                customBranch("resource", "skill." + jobId + ".resource", jobId,
                    node("resource_1", "skill." + jobId + ".resource.1", 5, 1, null, "craft_bonus", 0.05D),
                    node("resource_2", "skill." + jobId + ".resource.2", 10, 1, "resource_1", "material_save", 0.08D),
                    node("resource_3", "skill." + jobId + ".resource.3", 20, 1, "resource_2", "craft_bonus", 0.12D),
                    node("resource_4", "skill." + jobId + ".resource.4", 35, 2, "resource_3", "quality_bonus", 0.10D),
                    node("resource_5", "skill." + jobId + ".resource.5", 50, 2, "resource_4", "craft_bonus", 0.16D)
                ),
                branch(jobId, "utility", "skill." + jobId + ".utility", "craft_aura")
            );
        }

        private static List<SkillBranch> merchantTree(String jobId) {
            return List.of(
                branch(jobId, "income", "skill." + jobId + ".income", "salary_bonus"),
                customBranch("resource", "skill." + jobId + ".resource", jobId,
                    node("resource_1", "skill." + jobId + ".resource.1", 5, 1, null, "trade_bonus", 0.05D),
                    node("resource_2", "skill." + jobId + ".resource.2", 10, 1, "resource_1", "discount_bonus", 0.08D),
                    node("resource_3", "skill." + jobId + ".resource.3", 20, 1, "resource_2", "trade_bonus", 0.12D),
                    node("resource_4", "skill." + jobId + ".resource.4", 35, 2, "resource_3", "emerald_bonus", 0.10D),
                    node("resource_5", "skill." + jobId + ".resource.5", 50, 2, "resource_4", "trade_bonus", 0.16D)
                ),
                branch(jobId, "utility", "skill." + jobId + ".utility", "merchant_aura")
            );
        }

        private static List<SkillBranch> magicTree(String jobId) {
            return List.of(
                branch(jobId, "income", "skill." + jobId + ".income", "salary_bonus"),
                customBranch("resource", "skill." + jobId + ".resource", jobId,
                    node("resource_1", "skill." + jobId + ".resource.1", 5, 1, null, "magic_bonus", 0.05D),
                    node("resource_2", "skill." + jobId + ".resource.2", 10, 1, "resource_1", "ingredient_save", 0.08D),
                    node("resource_3", "skill." + jobId + ".resource.3", 20, 1, "resource_2", "magic_bonus", 0.12D),
                    node("resource_4", "skill." + jobId + ".resource.4", 35, 2, "resource_3", "rare_magic_bonus", 0.10D),
                    node("resource_5", "skill." + jobId + ".resource.5", 50, 2, "resource_4", "magic_bonus", 0.16D)
                ),
                branch(jobId, "utility", "skill." + jobId + ".utility", "magic_aura")
            );
        }

        private static List<SkillBranch> exploreTree(String jobId) {
            return List.of(
                branch(jobId, "income", "skill." + jobId + ".income", "salary_bonus"),
                customBranch("resource", "skill." + jobId + ".resource", jobId,
                    node("resource_1", "skill." + jobId + ".resource.1", 5, 1, null, "explore_bonus", 0.05D),
                    node("resource_2", "skill." + jobId + ".resource.2", 10, 1, "resource_1", "cache_finder", 0.08D),
                    node("resource_3", "skill." + jobId + ".resource.3", 20, 1, "resource_2", "explore_bonus", 0.12D),
                    node("resource_4", "skill." + jobId + ".resource.4", 35, 2, "resource_3", "artifact_bonus", 0.10D),
                    node("resource_5", "skill." + jobId + ".resource.5", 50, 2, "resource_4", "explore_bonus", 0.16D)
                ),
                branch(jobId, "utility", "skill." + jobId + ".utility", "explore_aura")
            );
        }

        private static SkillBranch customBranch(String id, String key, String jobId, SkillNode... nodes) {
            return new SkillBranch(id, key, List.of(nodes));
        }

        private static SkillNode node(String shortId, String key, int level, int cost, String parentShortId, String effectType, double effectValue) {
            String parent = parentShortId == null ? null : parentShortId;
            return new SkillNode(shortId, key, level, cost, parent, effectType, effectValue);
        }

        private static SkillBranch branch(String jobId, String id, String key, String effectType) {
            return new SkillBranch(id, key, List.of(
                new SkillNode(jobId + "_" + id + "_1", key + ".1", 5, 1, null, effectType, 0.05D),
                new SkillNode(jobId + "_" + id + "_2", key + ".2", 10, 1, jobId + "_" + id + "_1", effectType, 0.10D),
                new SkillNode(jobId + "_" + id + "_3", key + ".3", 20, 1, jobId + "_" + id + "_2", effectType, 0.15D),
                new SkillNode(jobId + "_" + id + "_4", key + ".4", 35, 2, jobId + "_" + id + "_3", effectType, 0.20D),
                new SkillNode(jobId + "_" + id + "_5", key + ".5", 50, 2, jobId + "_" + id + "_4", effectType, 0.25D)
            ));
        }
    }

    public static final class DailyTasksConfigLoader {
        private final List<DailyTaskTemplate> tasks;

        private DailyTasksConfigLoader(List<DailyTaskTemplate> tasks) {
            this.tasks = tasks;
        }

        public static DailyTasksConfigLoader load(Path path) {
            if (!Files.exists(path)) {
                writeDefaultDailyTasks(path);
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                List<DailyTaskTemplate> tasks = new ArrayList<>();
                for (JsonElement element : root.getAsJsonArray("tasks")) {
                    JsonObject obj = element.getAsJsonObject();
                    tasks.add(new DailyTaskTemplate(
                        obj.get("id").getAsString(),
                        obj.get("jobId").getAsString(),
                        JobActionType.valueOf(obj.get("type").getAsString()),
                        ResourceLocationUtil.parse(obj.get("target").getAsString()),
                        obj.get("goal").getAsInt(),
                        obj.get("salary").getAsDouble(),
                        obj.get("xp").getAsDouble(),
                        obj.has("bonusItem") ? obj.get("bonusItem").getAsString() : null,
                        obj.has("bonusCount") ? obj.get("bonusCount").getAsInt() : 0,
                        obj.has("buffEffect") ? obj.get("buffEffect").getAsString() : null,
                        obj.has("buffDurationSeconds") ? obj.get("buffDurationSeconds").getAsInt() : 0,
                        obj.has("buffAmplifier") ? obj.get("buffAmplifier").getAsInt() : 0,
                        obj.has("bonusTitle") ? obj.get("bonusTitle").getAsString() : null
                    ));
                }
                return new DailyTasksConfigLoader(tasks);
            } catch (Exception e) {
                AdvancedJobsMod.LOGGER.error("Failed to read daily tasks config", e);
                return new DailyTasksConfigLoader(List.of());
            }
        }

        public List<DailyTaskTemplate> tasksForJob(String jobId) {
            return tasks.stream().filter(task -> task.jobId().equals(jobId)).toList();
        }

        private static void writeDefaultDailyTasks(Path path) {
            JsonObject root = new JsonObject();
            JsonArray tasks = new JsonArray();
            for (JobDefinition definition : JobsConfigLoader.defaultDefinitions().values()) {
                List<JsonObject> handcrafted = handcraftedDailyTasks(definition.id());
                if (!handcrafted.isEmpty()) {
                    handcrafted.forEach(tasks::add);
                    continue;
                }
                int index = 1;
                for (ActionRewardEntry reward : definition.actionRewards().stream().limit(3).toList()) {
                    double difficulty = professionDifficulty(definition.id(), reward.actionType());
                    tasks.add(task(
                        definition.id() + "_daily_" + index++,
                        definition.id(),
                        reward.actionType().name(),
                        reward.targetId().toString(),
                        balancedDailyGoal(definition.id(), reward.actionType()),
                        Math.max(120.0D, reward.rewardDefinition().salary() * (18.0D + difficulty * 6.0D)),
                        Math.max(100.0D, reward.rewardDefinition().xp() * (15.0D + difficulty * 5.0D))
                    ));
                }
            }
            root.add("tasks", tasks);
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            } catch (IOException e) {
                AdvancedJobsMod.LOGGER.error("Failed to write daily tasks config", e);
            }
        }

        private static JsonObject task(String id, String jobId, String type, String target, int goal, double salary, double xp) {
            return task(id, jobId, type, target, goal, salary, xp, null, 0, null, 0, 0, null);
        }

        private static JsonObject task(String id, String jobId, String type, String target, int goal, double salary, double xp,
                                       String bonusItem, int bonusCount, String buffEffect, int buffDurationSeconds, int buffAmplifier, String bonusTitle) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", id);
            obj.addProperty("jobId", jobId);
            obj.addProperty("type", type);
            obj.addProperty("target", target);
            obj.addProperty("goal", goal);
            obj.addProperty("salary", salary);
            obj.addProperty("xp", xp);
            if (bonusItem != null && !bonusItem.isBlank() && bonusCount > 0) {
                obj.addProperty("bonusItem", bonusItem);
                obj.addProperty("bonusCount", bonusCount);
            }
            if (buffEffect != null && !buffEffect.isBlank() && buffDurationSeconds > 0) {
                obj.addProperty("buffEffect", buffEffect);
                obj.addProperty("buffDurationSeconds", buffDurationSeconds);
                obj.addProperty("buffAmplifier", Math.max(0, buffAmplifier));
            }
            if (bonusTitle != null && !bonusTitle.isBlank()) {
                obj.addProperty("bonusTitle", bonusTitle);
            }
            return obj;
        }
    }

    public static final class ContractsConfigLoader {
        private final List<ContractTemplate> contracts;

        private ContractsConfigLoader(List<ContractTemplate> contracts) {
            this.contracts = contracts;
        }

        public static ContractsConfigLoader load(Path path) {
            if (!Files.exists(path)) {
                writeDefaultContracts(path);
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                List<ContractTemplate> contracts = new ArrayList<>();
                for (JsonElement element : root.getAsJsonArray("contracts")) {
                    JsonObject obj = element.getAsJsonObject();
                    contracts.add(new ContractTemplate(
                        obj.get("id").getAsString(),
                        obj.get("jobId").getAsString(),
                        obj.get("rarity").getAsString(),
                        JobActionType.valueOf(obj.get("type").getAsString()),
                        ResourceLocationUtil.parse(obj.get("target").getAsString()),
                        obj.get("goal").getAsInt(),
                        obj.get("salary").getAsDouble(),
                        obj.get("xp").getAsDouble(),
                        obj.get("durationSeconds").getAsInt(),
                        obj.has("bonusItem") ? obj.get("bonusItem").getAsString() : null,
                        obj.has("bonusCount") ? obj.get("bonusCount").getAsInt() : 0,
                        obj.has("buffEffect") ? obj.get("buffEffect").getAsString() : null,
                        obj.has("buffDurationSeconds") ? obj.get("buffDurationSeconds").getAsInt() : 0,
                        obj.has("buffAmplifier") ? obj.get("buffAmplifier").getAsInt() : 0,
                        obj.has("bonusTitle") ? obj.get("bonusTitle").getAsString() : null
                    ));
                }
                return new ContractsConfigLoader(contracts);
            } catch (Exception e) {
                AdvancedJobsMod.LOGGER.error("Failed to read contracts config", e);
                return new ContractsConfigLoader(List.of());
            }
        }

        public List<ContractTemplate> contractsForJob(String jobId) {
            return contracts.stream().filter(contract -> contract.jobId().equals(jobId)).toList();
        }

        private static void writeDefaultContracts(Path path) {
            JsonObject root = new JsonObject();
            JsonArray tasks = new JsonArray();
            for (JobDefinition definition : JobsConfigLoader.defaultDefinitions().values()) {
                List<JsonObject> handcrafted = handcraftedContracts(definition.id());
                if (!handcrafted.isEmpty()) {
                    handcrafted.forEach(tasks::add);
                    continue;
                }
                List<ActionRewardEntry> rewards = definition.actionRewards();
                if (rewards.isEmpty()) {
                    continue;
                }
                ActionRewardEntry first = rewards.get(0);
                tasks.add(contract(
                    definition.id() + "_contract_common",
                    definition.id(),
                    "common",
                    first.actionType().name(),
                    first.targetId().toString(),
                    balancedContractGoal(definition.id(), first.actionType(), false, "common"),
                    Math.max(450.0D, first.rewardDefinition().salary() * (60.0D + professionDifficulty(definition.id(), first.actionType()) * 15.0D)),
                    Math.max(320.0D, first.rewardDefinition().xp() * (45.0D + professionDifficulty(definition.id(), first.actionType()) * 10.0D)),
                    contractDurationSeconds(first.actionType(), "common")
                ));
                ActionRewardEntry rare = rewards.get(Math.min(1, rewards.size() - 1));
                tasks.add(contract(
                    definition.id() + "_contract_rare",
                    definition.id(),
                    "rare",
                    rare.actionType().name(),
                    rare.targetId().toString(),
                    balancedContractGoal(definition.id(), rare.actionType(), true, "rare"),
                    Math.max(800.0D, rare.rewardDefinition().salary() * (95.0D + professionDifficulty(definition.id(), rare.actionType()) * 20.0D)),
                    Math.max(520.0D, rare.rewardDefinition().xp() * (72.0D + professionDifficulty(definition.id(), rare.actionType()) * 14.0D)),
                    contractDurationSeconds(rare.actionType(), "rare")
                ));
                ActionRewardEntry elite = rewards.get(rewards.size() - 1);
                tasks.add(contract(
                    definition.id() + "_contract_elite",
                    definition.id(),
                    "elite",
                    elite.actionType().name(),
                    elite.targetId().toString(),
                    balancedContractGoal(definition.id(), elite.actionType(), true, "elite"),
                    Math.max(1200.0D, elite.rewardDefinition().salary() * (135.0D + professionDifficulty(definition.id(), elite.actionType()) * 28.0D)),
                    Math.max(780.0D, elite.rewardDefinition().xp() * (96.0D + professionDifficulty(definition.id(), elite.actionType()) * 18.0D)),
                    contractDurationSeconds(elite.actionType(), "elite")
                ));
            }
            root.add("contracts", tasks);
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            } catch (IOException e) {
                AdvancedJobsMod.LOGGER.error("Failed to write contracts config", e);
            }
        }

        private static JsonObject contract(String id, String jobId, String rarity, String type, String target, int goal, double salary, double xp, int duration) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", id);
            obj.addProperty("jobId", jobId);
            obj.addProperty("rarity", rarity);
            obj.addProperty("type", type);
            obj.addProperty("target", target);
            obj.addProperty("goal", goal);
            obj.addProperty("salary", salary);
            obj.addProperty("xp", xp);
            obj.addProperty("durationSeconds", duration);
            return obj;
        }
    }

    public static final class EconomyJsonConfig {
        private final JsonObject root;

        private EconomyJsonConfig(JsonObject root) {
            this.root = root;
        }

        public static EconomyJsonConfig load(Path path) {
            if (!Files.exists(path)) {
                JsonObject root = new JsonObject();
                root.addProperty("provider", "external");
                root.addProperty("externalCurrency", "z_coin");
                root.addProperty("taxSinkAccountUuid", "00000000-0000-0000-0000-000000000001");
                root.addProperty("vipMultiplier", 1.0D);
                root.addProperty("eventMultiplier", 1.0D);
                root.addProperty("eventEndsAtEpochSecond", 0L);
                JsonObject worldMultipliers = new JsonObject();
                worldMultipliers.addProperty("minecraft:overworld", 1.0D);
                worldMultipliers.addProperty("minecraft:the_nether", 1.08D);
                worldMultipliers.addProperty("minecraft:the_end", 1.12D);
                root.add("worldMultipliers", worldMultipliers);
                JsonObject biomeMultipliers = new JsonObject();
                biomeMultipliers.addProperty("minecraft:desert", 1.06D);
                biomeMultipliers.addProperty("minecraft:frozen_peaks", 1.08D);
                biomeMultipliers.addProperty("minecraft:dripstone_caves", 1.05D);
                biomeMultipliers.addProperty("minecraft:deep_dark", 1.14D);
                biomeMultipliers.addProperty("minecraft:flower_forest", 1.05D);
                root.add("biomeMultipliers", biomeMultipliers);
                try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                    GSON.toJson(root, writer);
                } catch (IOException e) {
                    AdvancedJobsMod.LOGGER.error("Failed to write economy config", e);
                }
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                return new EconomyJsonConfig(GSON.fromJson(reader, JsonObject.class));
            } catch (Exception e) {
                AdvancedJobsMod.LOGGER.error("Failed to load economy config", e);
                JsonObject fallback = new JsonObject();
                fallback.addProperty("provider", "external");
                fallback.addProperty("externalCurrency", "z_coin");
                fallback.addProperty("taxSinkAccountUuid", "00000000-0000-0000-0000-000000000001");
                fallback.addProperty("vipMultiplier", 1.0D);
                fallback.addProperty("eventMultiplier", 1.0D);
                fallback.addProperty("eventEndsAtEpochSecond", 0L);
                return new EconomyJsonConfig(fallback);
            }
        }

        public String providerId() {
            return root.has("provider") ? root.get("provider").getAsString() : "internal";
        }

        public double vipMultiplier() {
            return root.has("vipMultiplier") ? root.get("vipMultiplier").getAsDouble() : 1.0D;
        }

        public String externalCurrencyId() {
            return root.has("externalCurrency") ? root.get("externalCurrency").getAsString() : "z_coin";
        }

        public String taxSinkAccountUuid() {
            return root.has("taxSinkAccountUuid") ? root.get("taxSinkAccountUuid").getAsString() : "00000000-0000-0000-0000-000000000001";
        }

        public UUID taxSinkAccountId() {
            try {
                return UUID.fromString(taxSinkAccountUuid());
            } catch (Exception ignored) {
                return null;
            }
        }

        public double eventMultiplier() {
            return root.has("eventMultiplier") ? root.get("eventMultiplier").getAsDouble() : 1.0D;
        }

        public void setEventMultiplier(double value) {
            root.addProperty("eventMultiplier", Math.max(0.0D, value));
        }

        public long eventEndsAtEpochSecond() {
            return root.has("eventEndsAtEpochSecond") ? root.get("eventEndsAtEpochSecond").getAsLong() : 0L;
        }

        public void setEventEndsAtEpochSecond(long value) {
            root.addProperty("eventEndsAtEpochSecond", Math.max(0L, value));
        }

        public Map<String, Double> worldMultipliers() {
            return readMultiplierMap("worldMultipliers");
        }

        public Map<String, Double> biomeMultipliers() {
            return readMultiplierMap("biomeMultipliers");
        }

        private Map<String, Double> readMultiplierMap(String key) {
            Map<String, Double> multipliers = new LinkedHashMap<>();
            if (!root.has(key) || !root.get(key).isJsonObject()) {
                return multipliers;
            }
            for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject(key).entrySet()) {
                if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isNumber()) {
                    multipliers.put(entry.getKey(), entry.getValue().getAsDouble());
                }
            }
            return multipliers;
        }

        public void save(Path path) {
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            } catch (IOException e) {
                AdvancedJobsMod.LOGGER.error("Failed to save economy config", e);
            }
        }
    }

    public record DailyTaskTemplate(String id, String jobId, JobActionType type, ResourceLocation target, int goal, double salary, double xp,
                                    String bonusItem, int bonusCount, String buffEffect, int buffDurationSeconds, int buffAmplifier, String bonusTitle) {
    }

    public record ContractTemplate(String id, String jobId, String rarity, JobActionType type, ResourceLocation target, int goal, double salary, double xp, int durationSeconds,
                                   String bonusItem, int bonusCount, String buffEffect, int buffDurationSeconds, int buffAmplifier, String bonusTitle) {
    }

    private static List<JsonObject> handcraftedDailyTasks(String jobId) {
        return switch (jobId) {
            case "miner" -> List.of(
                dailyTask("miner_daily_iron_shift", "miner", "BREAK_BLOCK", "minecraft:iron_ore", 64, 780.0D, 520.0D),
                dailyTask("miner_daily_redstone", "miner", "BREAK_BLOCK", "minecraft:redstone_ore", 48, 720.0D, 480.0D),
                dailyTask("miner_daily_diamond", "miner", "BREAK_BLOCK", "minecraft:diamond_ore", 12, 1100.0D, 760.0D,
                    "minecraft:diamond", 2, "minecraft:haste", 360, 0, "deep_delver")
            );
            case "lumberjack" -> List.of(
                dailyTask("lumberjack_daily_oak", "lumberjack", "BREAK_BLOCK", "minecraft:oak_log", 96, 760.0D, 500.0D),
                dailyTask("lumberjack_daily_spruce", "lumberjack", "BREAK_BLOCK", "minecraft:spruce_log", 96, 760.0D, 500.0D),
                dailyTask("lumberjack_daily_cherry", "lumberjack", "BREAK_BLOCK", "minecraft:cherry_log", 48, 920.0D, 620.0D)
            );
            case "forester" -> List.of(
                dailyTask("forester_daily_oak", "forester", "BREAK_BLOCK", "minecraft:oak_log", 80, 740.0D, 480.0D),
                dailyTask("forester_daily_spruce", "forester", "BREAK_BLOCK", "minecraft:spruce_log", 80, 760.0D, 500.0D),
                dailyTask("forester_daily_restoration", "forester", "BREAK_BLOCK", "minecraft:cherry_log", 40, 1080.0D, 700.0D,
                    "minecraft:oak_sapling", 12, "minecraft:speed", 240, 0, "grove_warden")
            );
            case "deep_miner" -> List.of(
                dailyTask("deep_miner_daily_iron", "deep_miner", "BREAK_BLOCK", "minecraft:deepslate_iron_ore", 64, 920.0D, 620.0D),
                dailyTask("deep_miner_daily_gold", "deep_miner", "BREAK_BLOCK", "minecraft:deepslate_gold_ore", 48, 980.0D, 660.0D),
                dailyTask("deep_miner_daily_diamond", "deep_miner", "BREAK_BLOCK", "minecraft:deepslate_diamond_ore", 16, 1380.0D, 920.0D,
                    "minecraft:diamond", 3, "minecraft:fire_resistance", 300, 0, "abyss_digger")
            );
            case "farmer" -> List.of(
                dailyTask("farmer_daily_wheat", "farmer", "HARVEST_CROP", "minecraft:wheat", 200, 820.0D, 520.0D),
                dailyTask("farmer_daily_carrot", "farmer", "HARVEST_CROP", "minecraft:carrots", 160, 760.0D, 500.0D),
                dailyTask("farmer_daily_seed", "farmer", "PLANT_CROP", "minecraft:wheat", 128, 640.0D, 420.0D)
            );
            case "harvester" -> List.of(
                dailyTask("harvester_daily_potato", "harvester", "HARVEST_CROP", "minecraft:potatoes", 192, 820.0D, 520.0D),
                dailyTask("harvester_daily_beetroot", "harvester", "HARVEST_CROP", "minecraft:beetroots", 160, 800.0D, 500.0D),
                dailyTask("harvester_daily_pumpkin", "harvester", "HARVEST_CROP", "minecraft:pumpkin", 96, 1160.0D, 760.0D,
                    "minecraft:golden_carrot", 6, "minecraft:haste", 240, 0, "field_reaper")
            );
            case "animal_breeder" -> List.of(
                dailyTask("animal_breeder_daily_cow", "animal_breeder", "BREED_ANIMAL", "minecraft:cow", 18, 760.0D, 500.0D),
                dailyTask("animal_breeder_daily_sheep", "animal_breeder", "BREED_ANIMAL", "minecraft:sheep", 20, 740.0D, 480.0D),
                dailyTask("animal_breeder_daily_stock", "animal_breeder", "BREED_ANIMAL", "minecraft:cow", 28, 1120.0D, 720.0D,
                    "minecraft:hay_block", 12, "minecraft:regeneration", 240, 0, "stockmaster")
            );
            case "fisher" -> List.of(
                dailyTask("fisher_daily_cod", "fisher", "FISH", "minecraft:cod", 32, 760.0D, 500.0D),
                dailyTask("fisher_daily_salmon", "fisher", "FISH", "minecraft:salmon", 24, 720.0D, 480.0D),
                dailyTask("fisher_daily_treasure", "fisher", "FISH", "minecraft:name_tag", 6, 1100.0D, 760.0D,
                    "minecraft:prismarine_crystals", 8, "minecraft:dolphins_grace", 240, 0)
            );
            case "hunter" -> List.of(
                dailyTask("hunter_daily_zombie", "hunter", "KILL_MOB", "minecraft:zombie", 25, 700.0D, 480.0D),
                dailyTask("hunter_daily_skeleton", "hunter", "KILL_MOB", "minecraft:skeleton", 25, 720.0D, 500.0D),
                dailyTask("hunter_daily_creeper", "hunter", "KILL_MOB", "minecraft:creeper", 12, 860.0D, 560.0D)
            );
            case "monster_slayer" -> List.of(
                dailyTask("monster_slayer_daily_creeper", "monster_slayer", "KILL_MOB", "minecraft:creeper", 20, 860.0D, 560.0D),
                dailyTask("monster_slayer_daily_enderman", "monster_slayer", "KILL_MOB", "minecraft:enderman", 12, 980.0D, 640.0D),
                dailyTask("monster_slayer_daily_witch", "monster_slayer", "KILL_MOB", "minecraft:witch", 10, 1120.0D, 720.0D,
                    "minecraft:fermented_spider_eye", 4, "minecraft:strength", 240, 0, "night_reaper")
            );
            case "guard" -> List.of(
                dailyTask("guard_daily_pillager", "guard", "KILL_MOB", "minecraft:pillager", 24, 860.0D, 560.0D,
                    "minecraft:crossbow", 1, "minecraft:resistance", 180, 0),
                dailyTask("guard_daily_patrol", "guard", "KILL_MOB", "minecraft:vindicator", 10, 1040.0D, 680.0D,
                    "minecraft:iron_ingot", 3, "minecraft:hero_of_the_village", 200, 0),
                dailyTask("guard_daily_raidbreak", "guard", "KILL_MOB", "minecraft:evoker", 4, 1380.0D, 900.0D,
                    "minecraft:shield", 1, "minecraft:resistance", 240, 0, "watch_captain")
            );
            case "bounty_hunter" -> List.of(
                dailyTask("bounty_hunter_daily_witch", "bounty_hunter", "KILL_MOB", "minecraft:witch", 8, 980.0D, 620.0D,
                    "minecraft:glass_bottle", 4, "minecraft:luck", 200, 0),
                dailyTask("bounty_hunter_daily_blaze", "bounty_hunter", "KILL_MOB", "minecraft:blaze", 12, 1120.0D, 700.0D,
                    "minecraft:blaze_powder", 4, "minecraft:fire_resistance", 220, 0),
                dailyTask("bounty_hunter_daily_enderman", "bounty_hunter", "KILL_MOB", "minecraft:enderman", 14, 1240.0D, 780.0D,
                    "minecraft:ender_pearl", 6, "minecraft:speed", 240, 0, "marked_executioner")
            );
            case "defender" -> List.of(
                dailyTask("defender_daily_shield", "defender", "KILL_MOB", "minecraft:zombie", 24, 760.0D, 500.0D,
                    "minecraft:bread", 4, "minecraft:absorption", 180, 0),
                dailyTask("defender_daily_line", "defender", "KILL_MOB", "minecraft:husk", 18, 860.0D, 560.0D,
                    "minecraft:iron_nugget", 8, "minecraft:resistance", 200, 0),
                dailyTask("defender_daily_hold", "defender", "KILL_MOB", "minecraft:creeper", 8, 1240.0D, 800.0D,
                    "minecraft:shield", 1, "minecraft:resistance", 300, 0, "bulwark")
            );
            case "boss_hunter" -> List.of(
                dailyTask("boss_hunter_daily_blaze", "boss_hunter", "KILL_MOB", "minecraft:blaze", 20, 1180.0D, 760.0D,
                    "minecraft:blaze_rod", 2, "minecraft:strength", 220, 0),
                dailyTask("boss_hunter_daily_evoker", "boss_hunter", "KILL_MOB", "minecraft:evoker", 8, 1320.0D, 860.0D,
                    "minecraft:totem_of_undying", 1, "minecraft:speed", 220, 0),
                dailyTask("boss_hunter_daily_boss", "boss_hunter", "KILL_BOSS", "minecraft:wither", 1, 2200.0D, 1440.0D,
                    "minecraft:nether_star", 1, "minecraft:strength", 360, 1, "raidbane")
            );
            case "builder" -> List.of(
                dailyTask("builder_daily_bricks", "builder", "PLACE_BLOCK", "minecraft:stone_bricks", 160, 760.0D, 500.0D),
                dailyTask("builder_daily_glass", "builder", "PLACE_BLOCK", "minecraft:glass", 96, 700.0D, 460.0D),
                dailyTask("builder_daily_decor", "builder", "PLACE_BLOCK", "minecraft:terracotta", 96, 840.0D, 560.0D)
            );
            case "mason" -> List.of(
                dailyTask("mason_daily_bricks", "mason", "PLACE_BLOCK", "minecraft:stone_bricks", 192, 820.0D, 540.0D),
                dailyTask("mason_daily_bricks_2", "mason", "PLACE_BLOCK", "minecraft:bricks", 144, 860.0D, 560.0D),
                dailyTask("mason_daily_polished", "mason", "PLACE_BLOCK", "minecraft:polished_andesite", 128, 940.0D, 620.0D,
                    "minecraft:stone_bricks", 32, "minecraft:resistance", 240, 0, "stone_warden")
            );
            case "carpenter" -> List.of(
                dailyTask("carpenter_daily_planks", "carpenter", "PLACE_BLOCK", "minecraft:oak_planks", 192, 760.0D, 500.0D),
                dailyTask("carpenter_daily_stairs", "carpenter", "PLACE_BLOCK", "minecraft:oak_stairs", 128, 820.0D, 540.0D),
                dailyTask("carpenter_daily_workshop", "carpenter", "PLACE_BLOCK", "minecraft:bookshelf", 64, 1180.0D, 760.0D,
                    "minecraft:bookshelf", 8, "minecraft:haste", 240, 0, "woodwright")
            );
            case "merchant" -> List.of(
                dailyTask("merchant_daily_trade", "merchant", "TRADE_WITH_VILLAGER", "minecraft:villager", 12, 880.0D, 560.0D),
                dailyTask("merchant_daily_trade_2", "merchant", "TRADE_WITH_VILLAGER", "minecraft:villager", 18, 1120.0D, 720.0D),
                dailyTask("merchant_daily_trade_3", "merchant", "TRADE_WITH_VILLAGER", "minecraft:villager", 24, 1380.0D, 860.0D,
                    "minecraft:emerald", 10, "minecraft:hero_of_the_village", 300, 0, "market_favorite")
            );
            case "alchemist" -> List.of(
                dailyTask("alchemist_daily_brew", "alchemist", "BREW_POTION", "minecraft:potion", 8, 820.0D, 540.0D),
                dailyTask("alchemist_daily_batch", "alchemist", "BREW_POTION", "minecraft:potion", 12, 1080.0D, 700.0D),
                dailyTask("alchemist_daily_master", "alchemist", "BREW_POTION", "minecraft:potion", 16, 1360.0D, 880.0D,
                    "minecraft:glowstone_dust", 4, "minecraft:regeneration", 240, 0)
            );
            case "enchanter" -> List.of(
                dailyTask("enchanter_daily_books", "enchanter", "ENCHANT_ITEM", "minecraft:book", 6, 920.0D, 620.0D),
                dailyTask("enchanter_daily_books_2", "enchanter", "ENCHANT_ITEM", "minecraft:book", 10, 1240.0D, 820.0D),
                dailyTask("enchanter_daily_books_3", "enchanter", "ENCHANT_ITEM", "minecraft:book", 14, 1560.0D, 980.0D)
            );
            case "explorer" -> List.of(
                dailyTask("explorer_daily_chunks", "explorer", "EXPLORE_CHUNK", "minecraft:chunk", 16, 900.0D, 600.0D),
                dailyTask("explorer_daily_loot", "explorer", "OPEN_LOOT_CHEST", "minecraft:chest", 8, 980.0D, 640.0D),
                dailyTask("explorer_daily_route", "explorer", "EXPLORE_CHUNK", "minecraft:chunk", 24, 1320.0D, 860.0D,
                    "minecraft:compass", 1, "minecraft:speed", 300, 0, "pathfinder")
            );
            case "redstone_technician" -> List.of(
                dailyTask("redstone_technician_daily_repeaters", "redstone_technician", "CRAFT_ITEM", "minecraft:repeater", 16, 860.0D, 560.0D),
                dailyTask("redstone_technician_daily_comparators", "redstone_technician", "CRAFT_ITEM", "minecraft:comparator", 12, 980.0D, 640.0D),
                dailyTask("redstone_technician_daily_redstone", "redstone_technician", "REDSTONE_USE", "minecraft:redstone", 48, 1180.0D, 760.0D,
                    "minecraft:redstone", 16, "minecraft:night_vision", 300, 0, "signal_keeper")
            );
            case "blacksmith" -> List.of(
                dailyTask("blacksmith_daily_iron", "blacksmith", "SMELT_ITEM", "minecraft:iron_ingot", 48, 820.0D, 540.0D),
                dailyTask("blacksmith_daily_tool", "blacksmith", "CRAFT_ITEM", "minecraft:iron_sword", 8, 980.0D, 640.0D),
                dailyTask("blacksmith_daily_batch", "blacksmith", "SMELT_ITEM", "minecraft:iron_ingot", 96, 1380.0D, 900.0D,
                    "minecraft:iron_ingot", 12, "minecraft:strength", 300, 0)
            );
            case "armorer" -> List.of(
                dailyTask("armorer_daily_chest", "armorer", "CRAFT_ITEM", "minecraft:iron_chestplate", 4, 960.0D, 620.0D),
                dailyTask("armorer_daily_helmet", "armorer", "CRAFT_ITEM", "minecraft:iron_helmet", 6, 980.0D, 640.0D),
                dailyTask("armorer_daily_set", "armorer", "CRAFT_ITEM", "minecraft:iron_leggings", 6, 1120.0D, 740.0D,
                    "minecraft:iron_chestplate", 1, "minecraft:resistance", 300, 0)
            );
            case "cook" -> List.of(
                dailyTask("cook_daily_beef", "cook", "SMELT_ITEM", "minecraft:cooked_beef", 48, 700.0D, 460.0D),
                dailyTask("cook_daily_bread", "cook", "CRAFT_ITEM", "minecraft:bread", 32, 680.0D, 440.0D),
                dailyTask("cook_daily_feast", "cook", "SMELT_ITEM", "minecraft:cooked_beef", 96, 1180.0D, 760.0D)
            );
            case "engineer" -> List.of(
                dailyTask("engineer_daily_redstone", "engineer", "REDSTONE_USE", "minecraft:redstone", 32, 820.0D, 520.0D),
                dailyTask("engineer_daily_piston", "engineer", "PLACE_BLOCK", "minecraft:piston", 32, 900.0D, 600.0D),
                dailyTask("engineer_daily_system", "engineer", "REDSTONE_USE", "minecraft:redstone", 64, 1380.0D, 860.0D)
            );
            case "treasure_hunter" -> List.of(
                dailyTask("treasure_daily_buried", "treasure_hunter", "OPEN_LOOT_CHEST", "minecraft:buried_treasure", 4, 1200.0D, 760.0D,
                    "minecraft:heart_of_the_sea", 1, null, 0, 0),
                dailyTask("treasure_daily_tag", "treasure_hunter", "FISH", "minecraft:name_tag", 8, 980.0D, 620.0D),
                dailyTask("treasure_daily_cache", "treasure_hunter", "OPEN_LOOT_CHEST", "minecraft:buried_treasure", 6, 1560.0D, 980.0D,
                    "minecraft:nautilus_shell", 2, "minecraft:luck", 300, 0)
            );
            case "archaeologist" -> List.of(
                dailyTask("archaeologist_daily_sand", "archaeologist", "OPEN_LOOT_CHEST", "minecraft:suspicious_sand", 10, 980.0D, 620.0D),
                dailyTask("archaeologist_daily_gravel", "archaeologist", "BREAK_BLOCK", "minecraft:suspicious_gravel", 12, 1040.0D, 680.0D),
                dailyTask("archaeologist_daily_relic", "archaeologist", "OPEN_LOOT_CHEST", "minecraft:suspicious_sand", 16, 1420.0D, 920.0D,
                    "minecraft:brush", 1, "minecraft:luck", 300, 0, "dustwalker")
            );
            case "quarry_worker" -> List.of(
                dailyTask("quarry_daily_stone", "quarry_worker", "BREAK_BLOCK", "minecraft:stone", 288, 760.0D, 480.0D),
                dailyTask("quarry_daily_cut", "quarry_worker", "BREAK_BLOCK", "minecraft:diorite", 192, 860.0D, 560.0D),
                dailyTask("quarry_daily_face", "quarry_worker", "BREAK_BLOCK", "minecraft:granite", 160, 1080.0D, 700.0D,
                    "minecraft:stone", 64, "minecraft:haste", 240, 0, "quarry_foreman")
            );
            case "digger" -> List.of(
                dailyTask("digger_daily_dirt", "digger", "BREAK_BLOCK", "minecraft:dirt", 288, 700.0D, 460.0D),
                dailyTask("digger_daily_gravel", "digger", "BREAK_BLOCK", "minecraft:gravel", 224, 820.0D, 540.0D),
                dailyTask("digger_daily_trench", "digger", "BREAK_BLOCK", "minecraft:rooted_dirt", 96, 1040.0D, 680.0D,
                    "minecraft:iron_shovel", 1, "minecraft:haste", 240, 0, "trench_master")
            );
            case "sand_collector" -> List.of(
                dailyTask("sand_collector_daily_sand", "sand_collector", "BREAK_BLOCK", "minecraft:sand", 320, 720.0D, 480.0D),
                dailyTask("sand_collector_daily_red_sand", "sand_collector", "BREAK_BLOCK", "minecraft:red_sand", 224, 880.0D, 580.0D),
                dailyTask("sand_collector_daily_dune", "sand_collector", "BREAK_BLOCK", "minecraft:red_sand", 320, 1200.0D, 780.0D,
                    "minecraft:glass", 32, "minecraft:speed", 240, 0, "dune_runner")
            );
            case "ice_harvester" -> List.of(
                dailyTask("ice_harvester_daily_ice", "ice_harvester", "BREAK_BLOCK", "minecraft:ice", 160, 820.0D, 520.0D),
                dailyTask("ice_harvester_daily_packed", "ice_harvester", "BREAK_BLOCK", "minecraft:packed_ice", 112, 920.0D, 600.0D),
                dailyTask("ice_harvester_daily_blue", "ice_harvester", "BREAK_BLOCK", "minecraft:blue_ice", 72, 1340.0D, 860.0D,
                    "minecraft:packed_ice", 16, "minecraft:fire_resistance", 240, 0, "frost_hand")
            );
            case "beekeeper" -> List.of(
                dailyTask("beekeeper_daily_bottle", "beekeeper", "CRAFT_ITEM", "minecraft:honey_bottle", 16, 760.0D, 500.0D),
                dailyTask("beekeeper_daily_hive", "beekeeper", "BREAK_BLOCK", "minecraft:beehive", 8, 920.0D, 620.0D),
                dailyTask("beekeeper_daily_honey", "beekeeper", "CRAFT_ITEM", "minecraft:honey_bottle", 32, 1120.0D, 720.0D,
                    "minecraft:honeycomb", 12, "minecraft:luck", 240, 0, "golden_apiarist")
            );
            case "herbalist" -> List.of(
                dailyTask("herbalist_daily_berries", "herbalist", "BREAK_BLOCK", "minecraft:sweet_berry_bush", 64, 720.0D, 480.0D),
                dailyTask("herbalist_daily_wart", "herbalist", "BREAK_BLOCK", "minecraft:nether_wart", 48, 840.0D, 560.0D),
                dailyTask("herbalist_daily_potion", "herbalist", "BREW_POTION", "minecraft:potion", 10, 1160.0D, 760.0D,
                    "minecraft:glow_berries", 8, "minecraft:night_vision", 240, 0, "wild_apothecary")
            );
            case "shepherd" -> List.of(
                dailyTask("shepherd_daily_sheep", "shepherd", "BREED_ANIMAL", "minecraft:sheep", 20, 760.0D, 500.0D),
                dailyTask("shepherd_daily_wool", "shepherd", "CRAFT_ITEM", "minecraft:white_wool", 64, 820.0D, 540.0D),
                dailyTask("shepherd_daily_flock", "shepherd", "BREED_ANIMAL", "minecraft:sheep", 32, 1120.0D, 720.0D,
                    "minecraft:white_carpet", 48, "minecraft:jump_boost", 240, 0, "flockmaster")
            );
            default -> List.of();
        };
    }

    private static List<JsonObject> handcraftedContracts(String jobId) {
        return switch (jobId) {
            case "miner" -> List.of(
                contractJson("miner_contract_common", "miner", "common", "BREAK_BLOCK", "minecraft:iron_ore", 128, 2200.0D, 1400.0D, 10800),
                contractJson("miner_contract_rare", "miner", "rare", "BREAK_BLOCK", "minecraft:redstone_ore", 96, 2800.0D, 1760.0D, 12600),
                contractJson("miner_contract_elite", "miner", "elite", "BREAK_BLOCK", "minecraft:diamond_ore", 32, 4200.0D, 2600.0D, 14400,
                    "minecraft:diamond", 6, "minecraft:haste", 480, 1, "master_miner")
            );
            case "lumberjack" -> List.of(
                contractJson("lumberjack_contract_common", "lumberjack", "common", "BREAK_BLOCK", "minecraft:oak_log", 192, 2100.0D, 1320.0D, 10800),
                contractJson("lumberjack_contract_rare", "lumberjack", "rare", "BREAK_BLOCK", "minecraft:spruce_log", 192, 2600.0D, 1640.0D, 12600),
                contractJson("lumberjack_contract_elite", "lumberjack", "elite", "BREAK_BLOCK", "minecraft:cherry_log", 96, 3600.0D, 2240.0D, 14400)
            );
            case "forester" -> List.of(
                contractJson("forester_contract_common", "forester", "common", "BREAK_BLOCK", "minecraft:oak_log", 160, 2140.0D, 1340.0D, 10800),
                contractJson("forester_contract_rare", "forester", "rare", "BREAK_BLOCK", "minecraft:spruce_log", 160, 2660.0D, 1660.0D, 12600),
                contractJson("forester_contract_elite", "forester", "elite", "BREAK_BLOCK", "minecraft:cherry_log", 96, 3880.0D, 2420.0D, 14400,
                    "minecraft:cherry_sapling", 8, "minecraft:speed", 300, 0, "canopy_keeper")
            );
            case "deep_miner" -> List.of(
                contractJson("deep_miner_contract_common", "deep_miner", "common", "BREAK_BLOCK", "minecraft:deepslate_iron_ore", 128, 2500.0D, 1560.0D, 10800),
                contractJson("deep_miner_contract_rare", "deep_miner", "rare", "BREAK_BLOCK", "minecraft:deepslate_gold_ore", 96, 3200.0D, 2000.0D, 12600),
                contractJson("deep_miner_contract_elite", "deep_miner", "elite", "BREAK_BLOCK", "minecraft:deepslate_diamond_ore", 32, 4700.0D, 2920.0D, 14400,
                    "minecraft:diamond_block", 1, "minecraft:fire_resistance", 420, 0, "abyss_lord")
            );
            case "farmer" -> List.of(
                contractJson("farmer_contract_common", "farmer", "common", "HARVEST_CROP", "minecraft:wheat", 256, 2200.0D, 1360.0D, 10800),
                contractJson("farmer_contract_rare", "farmer", "rare", "HARVEST_CROP", "minecraft:carrots", 224, 2600.0D, 1640.0D, 12600),
                contractJson("farmer_contract_elite", "farmer", "elite", "PLANT_CROP", "minecraft:wheat", 192, 3200.0D, 2080.0D, 14400)
            );
            case "harvester" -> List.of(
                contractJson("harvester_contract_common", "harvester", "common", "HARVEST_CROP", "minecraft:potatoes", 240, 2240.0D, 1380.0D, 10800),
                contractJson("harvester_contract_rare", "harvester", "rare", "HARVEST_CROP", "minecraft:beetroots", 192, 2720.0D, 1700.0D, 12600),
                contractJson("harvester_contract_elite", "harvester", "elite", "HARVEST_CROP", "minecraft:pumpkin", 128, 4040.0D, 2520.0D, 14400,
                    "minecraft:golden_apple", 1, "minecraft:haste", 300, 0, "granary_master")
            );
            case "animal_breeder" -> List.of(
                contractJson("animal_breeder_contract_common", "animal_breeder", "common", "BREED_ANIMAL", "minecraft:cow", 36, 2200.0D, 1360.0D, 10800),
                contractJson("animal_breeder_contract_rare", "animal_breeder", "rare", "BREED_ANIMAL", "minecraft:sheep", 40, 2680.0D, 1660.0D, 12600),
                contractJson("animal_breeder_contract_elite", "animal_breeder", "elite", "BREED_ANIMAL", "minecraft:cow", 56, 3820.0D, 2380.0D, 14400,
                    "minecraft:golden_carrot", 8, "minecraft:absorption", 300, 0, "pasture_lord")
            );
            case "fisher" -> List.of(
                contractJson("fisher_contract_common", "fisher", "common", "FISH", "minecraft:cod", 48, 2100.0D, 1320.0D, 10800),
                contractJson("fisher_contract_rare", "fisher", "rare", "FISH", "minecraft:salmon", 36, 2560.0D, 1600.0D, 12600),
                contractJson("fisher_contract_elite", "fisher", "elite", "FISH", "minecraft:name_tag", 12, 3900.0D, 2440.0D, 14400)
            );
            case "hunter" -> List.of(
                contractJson("hunter_contract_common", "hunter", "common", "KILL_MOB", "minecraft:zombie", 50, 2200.0D, 1400.0D, 10800),
                contractJson("hunter_contract_rare", "hunter", "rare", "KILL_MOB", "minecraft:skeleton", 50, 2500.0D, 1600.0D, 12600),
                contractJson("hunter_contract_elite", "hunter", "elite", "KILL_MOB", "minecraft:creeper", 25, 3600.0D, 2280.0D, 14400)
            );
            case "monster_slayer" -> List.of(
                contractJson("monster_slayer_contract_common", "monster_slayer", "common", "KILL_MOB", "minecraft:creeper", 36, 2400.0D, 1520.0D, 10800),
                contractJson("monster_slayer_contract_rare", "monster_slayer", "rare", "KILL_MOB", "minecraft:enderman", 24, 3200.0D, 2000.0D, 12600),
                contractJson("monster_slayer_contract_elite", "monster_slayer", "elite", "KILL_MOB", "minecraft:witch", 18, 4300.0D, 2680.0D, 14400,
                    "minecraft:glowstone_dust", 8, "minecraft:strength", 420, 1, "hex_hunter")
            );
            case "guard" -> List.of(
                contractJson("guard_contract_common", "guard", "common", "KILL_MOB", "minecraft:pillager", 48, 2480.0D, 1560.0D, 10800,
                    "minecraft:crossbow", 1, "minecraft:resistance", 300, 0),
                contractJson("guard_contract_rare", "guard", "rare", "KILL_MOB", "minecraft:vindicator", 24, 3240.0D, 2040.0D, 12600,
                    "minecraft:shield", 1, "minecraft:hero_of_the_village", 360, 0),
                contractJson("guard_contract_elite", "guard", "elite", "KILL_MOB", "minecraft:evoker", 10, 4700.0D, 2960.0D, 14400,
                    "minecraft:shield", 1, "minecraft:resistance", 420, 1, "city_sentinel")
            );
            case "bounty_hunter" -> List.of(
                contractJson("bounty_hunter_contract_common", "bounty_hunter", "common", "KILL_MOB", "minecraft:witch", 18, 2500.0D, 1560.0D, 10800,
                    "minecraft:glass_bottle", 8, "minecraft:luck", 300, 0),
                contractJson("bounty_hunter_contract_rare", "bounty_hunter", "rare", "KILL_MOB", "minecraft:blaze", 24, 3200.0D, 2000.0D, 12600,
                    "minecraft:blaze_powder", 8, "minecraft:fire_resistance", 360, 0),
                contractJson("bounty_hunter_contract_elite", "bounty_hunter", "elite", "KILL_MOB", "minecraft:enderman", 32, 4500.0D, 2840.0D, 14400,
                    "minecraft:ender_pearl", 12, "minecraft:speed", 420, 1, "star_bounty")
            );
            case "defender" -> List.of(
                contractJson("defender_contract_common", "defender", "common", "KILL_MOB", "minecraft:zombie", 48, 2200.0D, 1400.0D, 10800,
                    "minecraft:bread", 8, "minecraft:absorption", 300, 0),
                contractJson("defender_contract_rare", "defender", "rare", "KILL_MOB", "minecraft:husk", 36, 2840.0D, 1800.0D, 12600,
                    "minecraft:iron_nugget", 16, "minecraft:resistance", 360, 0),
                contractJson("defender_contract_elite", "defender", "elite", "KILL_MOB", "minecraft:creeper", 16, 4300.0D, 2720.0D, 14400,
                    "minecraft:shield", 1, "minecraft:resistance", 420, 1, "wall_of_iron")
            );
            case "boss_hunter" -> List.of(
                contractJson("boss_hunter_contract_common", "boss_hunter", "common", "KILL_MOB", "minecraft:blaze", 32, 2800.0D, 1760.0D, 12600,
                    "minecraft:blaze_rod", 4, "minecraft:strength", 300, 0),
                contractJson("boss_hunter_contract_rare", "boss_hunter", "rare", "KILL_MOB", "minecraft:evoker", 12, 3600.0D, 2280.0D, 14400,
                    "minecraft:totem_of_undying", 1, "minecraft:speed", 360, 0),
                contractJson("boss_hunter_contract_elite", "boss_hunter", "elite", "KILL_BOSS", "minecraft:wither", 2, 6200.0D, 3880.0D, 16200,
                    "minecraft:nether_star", 1, "minecraft:strength", 480, 1, "thronebreaker")
            );
            case "builder" -> List.of(
                contractJson("builder_contract_common", "builder", "common", "PLACE_BLOCK", "minecraft:stone_bricks", 220, 2200.0D, 1400.0D, 10800),
                contractJson("builder_contract_rare", "builder", "rare", "PLACE_BLOCK", "minecraft:glass", 128, 2600.0D, 1640.0D, 12600),
                contractJson("builder_contract_elite", "builder", "elite", "PLACE_BLOCK", "minecraft:terracotta", 128, 3400.0D, 2120.0D, 14400)
            );
            case "mason" -> List.of(
                contractJson("mason_contract_common", "mason", "common", "PLACE_BLOCK", "minecraft:stone_bricks", 256, 2300.0D, 1440.0D, 10800),
                contractJson("mason_contract_rare", "mason", "rare", "PLACE_BLOCK", "minecraft:bricks", 192, 3000.0D, 1880.0D, 12600),
                contractJson("mason_contract_elite", "mason", "elite", "PLACE_BLOCK", "minecraft:polished_andesite", 160, 3900.0D, 2440.0D, 14400,
                    "minecraft:stonecutter", 1, "minecraft:resistance", 420, 1, "citadel_mason")
            );
            case "carpenter" -> List.of(
                contractJson("carpenter_contract_common", "carpenter", "common", "PLACE_BLOCK", "minecraft:oak_planks", 256, 2200.0D, 1400.0D, 10800),
                contractJson("carpenter_contract_rare", "carpenter", "rare", "PLACE_BLOCK", "minecraft:oak_stairs", 160, 2900.0D, 1840.0D, 12600),
                contractJson("carpenter_contract_elite", "carpenter", "elite", "PLACE_BLOCK", "minecraft:bookshelf", 96, 4200.0D, 2640.0D, 14400,
                    "minecraft:bookshelf", 12, "minecraft:haste", 420, 1, "grand_carpenter")
            );
            case "merchant" -> List.of(
                contractJson("merchant_contract_common", "merchant", "common", "TRADE_WITH_VILLAGER", "minecraft:villager", 20, 2600.0D, 1640.0D, 10800),
                contractJson("merchant_contract_rare", "merchant", "rare", "TRADE_WITH_VILLAGER", "minecraft:villager", 32, 3400.0D, 2120.0D, 12600),
                contractJson("merchant_contract_elite", "merchant", "elite", "TRADE_WITH_VILLAGER", "minecraft:villager", 48, 4600.0D, 2840.0D, 14400,
                    "minecraft:emerald", 24, "minecraft:hero_of_the_village", 480, 0, "golden_trader")
            );
            case "alchemist" -> List.of(
                contractJson("alchemist_contract_common", "alchemist", "common", "BREW_POTION", "minecraft:potion", 16, 2500.0D, 1560.0D, 10800),
                contractJson("alchemist_contract_rare", "alchemist", "rare", "BREW_POTION", "minecraft:potion", 24, 3200.0D, 2000.0D, 12600),
                contractJson("alchemist_contract_elite", "alchemist", "elite", "BREW_POTION", "minecraft:potion", 32, 4400.0D, 2720.0D, 14400,
                    "minecraft:glowstone_dust", 8, "minecraft:regeneration", 420, 1)
            );
            case "enchanter" -> List.of(
                contractJson("enchanter_contract_common", "enchanter", "common", "ENCHANT_ITEM", "minecraft:book", 12, 2800.0D, 1760.0D, 10800),
                contractJson("enchanter_contract_rare", "enchanter", "rare", "ENCHANT_ITEM", "minecraft:book", 18, 3600.0D, 2240.0D, 12600),
                contractJson("enchanter_contract_elite", "enchanter", "elite", "ENCHANT_ITEM", "minecraft:book", 24, 5000.0D, 3080.0D, 14400)
            );
            case "explorer" -> List.of(
                contractJson("explorer_contract_common", "explorer", "common", "EXPLORE_CHUNK", "minecraft:chunk", 20, 2400.0D, 1520.0D, 12600),
                contractJson("explorer_contract_rare", "explorer", "rare", "OPEN_LOOT_CHEST", "minecraft:chest", 12, 3200.0D, 2000.0D, 14400),
                contractJson("explorer_contract_elite", "explorer", "elite", "EXPLORE_CHUNK", "minecraft:chunk", 36, 4700.0D, 2920.0D, 16200,
                    "minecraft:map", 1, "minecraft:speed", 420, 1, "frontier_legend")
            );
            case "redstone_technician" -> List.of(
                contractJson("redstone_technician_contract_common", "redstone_technician", "common", "CRAFT_ITEM", "minecraft:repeater", 24, 2400.0D, 1480.0D, 10800),
                contractJson("redstone_technician_contract_rare", "redstone_technician", "rare", "CRAFT_ITEM", "minecraft:comparator", 18, 3200.0D, 1960.0D, 12600),
                contractJson("redstone_technician_contract_elite", "redstone_technician", "elite", "REDSTONE_USE", "minecraft:redstone", 96, 4300.0D, 2680.0D, 14400,
                    "minecraft:comparator", 4, "minecraft:night_vision", 420, 0, "relay_master")
            );
            case "blacksmith" -> List.of(
                contractJson("blacksmith_contract_common", "blacksmith", "common", "SMELT_ITEM", "minecraft:iron_ingot", 96, 2400.0D, 1520.0D, 10800),
                contractJson("blacksmith_contract_rare", "blacksmith", "rare", "CRAFT_ITEM", "minecraft:iron_sword", 16, 3200.0D, 2000.0D, 12600),
                contractJson("blacksmith_contract_elite", "blacksmith", "elite", "SMELT_ITEM", "minecraft:iron_ingot", 160, 4300.0D, 2680.0D, 14400,
                    "minecraft:iron_block", 4, "minecraft:strength", 420, 1)
            );
            case "armorer" -> List.of(
                contractJson("armorer_contract_common", "armorer", "common", "CRAFT_ITEM", "minecraft:iron_helmet", 10, 2300.0D, 1480.0D, 10800),
                contractJson("armorer_contract_rare", "armorer", "rare", "CRAFT_ITEM", "minecraft:iron_chestplate", 8, 3200.0D, 2040.0D, 12600),
                contractJson("armorer_contract_elite", "armorer", "elite", "CRAFT_ITEM", "minecraft:diamond_chestplate", 3, 5200.0D, 3280.0D, 14400,
                    "minecraft:diamond_chestplate", 1, "minecraft:resistance", 420, 1)
            );
            case "cook" -> List.of(
                contractJson("cook_contract_common", "cook", "common", "SMELT_ITEM", "minecraft:cooked_beef", 96, 2100.0D, 1320.0D, 10800),
                contractJson("cook_contract_rare", "cook", "rare", "CRAFT_ITEM", "minecraft:pumpkin_pie", 32, 2700.0D, 1720.0D, 12600),
                contractJson("cook_contract_elite", "cook", "elite", "CRAFT_ITEM", "minecraft:cake", 18, 3900.0D, 2480.0D, 14400)
            );
            case "engineer" -> List.of(
                contractJson("engineer_contract_common", "engineer", "common", "REDSTONE_USE", "minecraft:redstone", 64, 2400.0D, 1480.0D, 10800),
                contractJson("engineer_contract_rare", "engineer", "rare", "PLACE_BLOCK", "minecraft:piston", 64, 3200.0D, 1960.0D, 12600),
                contractJson("engineer_contract_elite", "engineer", "elite", "REDSTONE_USE", "minecraft:redstone", 128, 4500.0D, 2800.0D, 14400)
            );
            case "treasure_hunter" -> List.of(
                contractJson("treasure_contract_common", "treasure_hunter", "common", "OPEN_LOOT_CHEST", "minecraft:buried_treasure", 6, 3000.0D, 1880.0D, 12600),
                contractJson("treasure_contract_rare", "treasure_hunter", "rare", "FISH", "minecraft:name_tag", 12, 3400.0D, 2120.0D, 14400),
                contractJson("treasure_contract_elite", "treasure_hunter", "elite", "OPEN_LOOT_CHEST", "minecraft:buried_treasure", 10, 5200.0D, 3200.0D, 16200,
                    "minecraft:heart_of_the_sea", 1, "minecraft:luck", 420, 1, "relic_lord")
            );
            case "archaeologist" -> List.of(
                contractJson("archaeologist_contract_common", "archaeologist", "common", "OPEN_LOOT_CHEST", "minecraft:suspicious_sand", 18, 2600.0D, 1640.0D, 12600),
                contractJson("archaeologist_contract_rare", "archaeologist", "rare", "BREAK_BLOCK", "minecraft:suspicious_gravel", 24, 3400.0D, 2120.0D, 14400),
                contractJson("archaeologist_contract_elite", "archaeologist", "elite", "OPEN_LOOT_CHEST", "minecraft:suspicious_sand", 32, 4700.0D, 2920.0D, 16200,
                    "minecraft:brush", 2, "minecraft:luck", 420, 1, "relic_curator")
            );
            case "quarry_worker" -> List.of(
                contractJson("quarry_contract_common", "quarry_worker", "common", "BREAK_BLOCK", "minecraft:stone", 448, 2240.0D, 1400.0D, 10800),
                contractJson("quarry_contract_rare", "quarry_worker", "rare", "BREAK_BLOCK", "minecraft:diorite", 320, 2920.0D, 1840.0D, 12600),
                contractJson("quarry_contract_elite", "quarry_worker", "elite", "BREAK_BLOCK", "minecraft:granite", 256, 4380.0D, 2760.0D, 14400,
                    "minecraft:stonecutter", 1, "minecraft:haste", 420, 1, "grand_quarry")
            );
            case "digger" -> List.of(
                contractJson("digger_contract_common", "digger", "common", "BREAK_BLOCK", "minecraft:dirt", 448, 2120.0D, 1320.0D, 10800),
                contractJson("digger_contract_rare", "digger", "rare", "BREAK_BLOCK", "minecraft:gravel", 320, 2860.0D, 1800.0D, 12600),
                contractJson("digger_contract_elite", "digger", "elite", "BREAK_BLOCK", "minecraft:rooted_dirt", 144, 4200.0D, 2640.0D, 14400,
                    "minecraft:diamond_shovel", 1, "minecraft:haste", 420, 1, "earthshaper")
            );
            case "sand_collector" -> List.of(
                contractJson("sand_collector_contract_common", "sand_collector", "common", "BREAK_BLOCK", "minecraft:sand", 448, 2200.0D, 1360.0D, 10800),
                contractJson("sand_collector_contract_rare", "sand_collector", "rare", "BREAK_BLOCK", "minecraft:red_sand", 320, 3040.0D, 1920.0D, 12600),
                contractJson("sand_collector_contract_elite", "sand_collector", "elite", "BREAK_BLOCK", "minecraft:red_sand", 448, 4560.0D, 2880.0D, 14400,
                    "minecraft:glass", 64, "minecraft:speed", 420, 1, "desert_ghost")
            );
            case "ice_harvester" -> List.of(
                contractJson("ice_harvester_contract_common", "ice_harvester", "common", "BREAK_BLOCK", "minecraft:ice", 224, 2420.0D, 1520.0D, 10800),
                contractJson("ice_harvester_contract_rare", "ice_harvester", "rare", "BREAK_BLOCK", "minecraft:packed_ice", 160, 3160.0D, 2000.0D, 12600),
                contractJson("ice_harvester_contract_elite", "ice_harvester", "elite", "BREAK_BLOCK", "minecraft:blue_ice", 112, 4920.0D, 3120.0D, 14400,
                    "minecraft:blue_ice", 16, "minecraft:fire_resistance", 420, 1, "glacier_lord")
            );
            case "beekeeper" -> List.of(
                contractJson("beekeeper_contract_common", "beekeeper", "common", "CRAFT_ITEM", "minecraft:honey_bottle", 32, 2200.0D, 1400.0D, 10800),
                contractJson("beekeeper_contract_rare", "beekeeper", "rare", "BREAK_BLOCK", "minecraft:beehive", 18, 3000.0D, 1880.0D, 12600),
                contractJson("beekeeper_contract_elite", "beekeeper", "elite", "CRAFT_ITEM", "minecraft:honey_bottle", 64, 4200.0D, 2640.0D, 14400,
                    "minecraft:honeycomb_block", 4, "minecraft:luck", 420, 1, "queen_keeper")
            );
            case "herbalist" -> List.of(
                contractJson("herbalist_contract_common", "herbalist", "common", "BREAK_BLOCK", "minecraft:sweet_berry_bush", 128, 2100.0D, 1320.0D, 10800),
                contractJson("herbalist_contract_rare", "herbalist", "rare", "BREAK_BLOCK", "minecraft:nether_wart", 96, 2900.0D, 1840.0D, 12600),
                contractJson("herbalist_contract_elite", "herbalist", "elite", "BREW_POTION", "minecraft:potion", 24, 4300.0D, 2720.0D, 14400,
                    "minecraft:glow_berries", 16, "minecraft:night_vision", 420, 1, "grove_alchemist")
            );
            case "shepherd" -> List.of(
                contractJson("shepherd_contract_common", "shepherd", "common", "BREED_ANIMAL", "minecraft:sheep", 32, 2200.0D, 1400.0D, 10800),
                contractJson("shepherd_contract_rare", "shepherd", "rare", "CRAFT_ITEM", "minecraft:white_wool", 128, 2900.0D, 1840.0D, 12600),
                contractJson("shepherd_contract_elite", "shepherd", "elite", "BREED_ANIMAL", "minecraft:sheep", 48, 4200.0D, 2640.0D, 14400,
                    "minecraft:white_carpet", 96, "minecraft:jump_boost", 420, 1, "wool_lord")
            );
            default -> List.of();
        };
    }

    private static JsonObject dailyTask(String id, String jobId, String type, String target, int goal, double salary, double xp) {
        return dailyTask(id, jobId, type, target, goal, salary, xp, null, 0, null, 0, 0, null);
    }

    private static JsonObject dailyTask(String id, String jobId, String type, String target, int goal, double salary, double xp,
                                        String bonusItem, int bonusCount, String buffEffect, int buffDurationSeconds, int buffAmplifier) {
        return dailyTask(id, jobId, type, target, goal, salary, xp, bonusItem, bonusCount, buffEffect, buffDurationSeconds, buffAmplifier, null);
    }

    private static JsonObject dailyTask(String id, String jobId, String type, String target, int goal, double salary, double xp,
                                        String bonusItem, int bonusCount, String buffEffect, int buffDurationSeconds, int buffAmplifier, String bonusTitle) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", id);
        obj.addProperty("jobId", jobId);
        obj.addProperty("type", type);
        obj.addProperty("target", target);
        obj.addProperty("goal", goal);
        obj.addProperty("salary", salary);
        obj.addProperty("xp", xp);
        if (bonusItem != null && !bonusItem.isBlank() && bonusCount > 0) {
            obj.addProperty("bonusItem", bonusItem);
            obj.addProperty("bonusCount", bonusCount);
        }
        if (buffEffect != null && !buffEffect.isBlank() && buffDurationSeconds > 0) {
            obj.addProperty("buffEffect", buffEffect);
            obj.addProperty("buffDurationSeconds", buffDurationSeconds);
            obj.addProperty("buffAmplifier", Math.max(0, buffAmplifier));
        }
        if (bonusTitle != null && !bonusTitle.isBlank()) {
            obj.addProperty("bonusTitle", bonusTitle);
        }
        return obj;
    }

    private static JsonObject contractJson(String id, String jobId, String rarity, String type, String target, int goal, double salary, double xp, int duration) {
        return contractJson(id, jobId, rarity, type, target, goal, salary, xp, duration, null, 0, null, 0, 0, null);
    }

    private static JsonObject contractJson(String id, String jobId, String rarity, String type, String target, int goal, double salary, double xp,
                                           int duration, String bonusItem, int bonusCount, String buffEffect, int buffDurationSeconds, int buffAmplifier) {
        return contractJson(id, jobId, rarity, type, target, goal, salary, xp, duration, bonusItem, bonusCount, buffEffect, buffDurationSeconds, buffAmplifier, null);
    }

    private static JsonObject contractJson(String id, String jobId, String rarity, String type, String target, int goal, double salary, double xp,
                                           int duration, String bonusItem, int bonusCount, String buffEffect, int buffDurationSeconds, int buffAmplifier, String bonusTitle) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", id);
        obj.addProperty("jobId", jobId);
        obj.addProperty("rarity", rarity);
        obj.addProperty("type", type);
        obj.addProperty("target", target);
        obj.addProperty("goal", goal);
        obj.addProperty("salary", salary);
        obj.addProperty("xp", xp);
        obj.addProperty("durationSeconds", duration);
        if (bonusItem != null && !bonusItem.isBlank() && bonusCount > 0) {
            obj.addProperty("bonusItem", bonusItem);
            obj.addProperty("bonusCount", bonusCount);
        }
        if (buffEffect != null && !buffEffect.isBlank() && buffDurationSeconds > 0) {
            obj.addProperty("buffEffect", buffEffect);
            obj.addProperty("buffDurationSeconds", buffDurationSeconds);
            obj.addProperty("buffAmplifier", Math.max(0, buffAmplifier));
        }
        if (bonusTitle != null && !bonusTitle.isBlank()) {
            obj.addProperty("bonusTitle", bonusTitle);
        }
        return obj;
    }

    private static int balancedDailyGoal(String jobId, JobActionType actionType) {
        int base = switch (actionType) {
            case BREAK_BLOCK -> 64;
            case PLACE_BLOCK -> 72;
            case HARVEST_CROP -> 96;
            case PLANT_CROP -> 64;
            case KILL_MOB -> 18;
            case KILL_BOSS -> 1;
            case BREED_ANIMAL, FISH -> 16;
            case CRAFT_ITEM, SMELT_ITEM -> 18;
            case BREW_POTION, ENCHANT_ITEM, TRADE_WITH_VILLAGER, OPEN_LOOT_CHEST, REDSTONE_USE -> 10;
            case EXPLORE_CHUNK -> 8;
        };
        return Math.max(1, Math.round(base * goalModifier(jobId, actionType)));
    }

    private static int balancedContractGoal(String jobId, JobActionType actionType, boolean rare, String rarity) {
        int base = switch (actionType) {
            case BREAK_BLOCK -> rare ? 256 : 128;
            case PLACE_BLOCK -> rare ? 300 : 150;
            case HARVEST_CROP -> rare ? 320 : 160;
            case PLANT_CROP -> rare ? 256 : 128;
            case KILL_MOB -> rare ? 50 : 25;
            case KILL_BOSS -> rare ? 2 : 1;
            case BREED_ANIMAL, FISH -> rare ? 40 : 20;
            case CRAFT_ITEM, SMELT_ITEM -> rare ? 48 : 24;
            case BREW_POTION, ENCHANT_ITEM, TRADE_WITH_VILLAGER, OPEN_LOOT_CHEST, REDSTONE_USE -> rare ? 24 : 12;
            case EXPLORE_CHUNK -> rare ? 24 : 12;
        };
        float rarityModifier = switch (rarity) {
            case "elite" -> 1.45F;
            case "rare" -> 1.15F;
            default -> 1.0F;
        };
        int adjusted = Math.round(base * goalModifier(jobId, actionType) * rarityModifier);
        return Math.max(1, adjusted);
    }

    private static int contractDurationSeconds(JobActionType actionType, String rarity) {
        int base = switch (actionType) {
            case KILL_BOSS -> 21600;
            case EXPLORE_CHUNK, OPEN_LOOT_CHEST -> 14400;
            case BREW_POTION, ENCHANT_ITEM, TRADE_WITH_VILLAGER -> 12600;
            default -> 10800;
        };
        return switch (rarity) {
            case "common" -> Math.max(5400, base - 3600);
            case "elite" -> base + 5400;
            default -> base;
        };
    }

    private static float goalModifier(String jobId, JobActionType actionType) {
        float modifier = switch (jobId) {
            case "boss_hunter" -> 0.5F;
            case "explorer", "treasure_hunter", "archaeologist" -> 0.85F;
            case "merchant", "alchemist", "enchanter", "redstone_technician" -> 0.9F;
            case "armorer" -> 0.8F;
            case "cook" -> 1.05F;
            case "farmer", "harvester", "builder", "mason", "carpenter" -> 1.15F;
            default -> 1.0F;
        };
        if (actionType == JobActionType.KILL_BOSS) {
            return Math.min(modifier, 1.0F);
        }
        return modifier;
    }

    private static double professionDifficulty(String jobId, JobActionType actionType) {
        double modifier = switch (jobId) {
            case "boss_hunter", "treasure_hunter", "archaeologist", "bounty_hunter" -> 2.0D;
            case "explorer", "enchanter", "alchemist", "redstone_technician", "engineer" -> 1.6D;
            case "monster_slayer", "guard", "merchant", "blacksmith", "armorer" -> 1.4D;
            case "cook" -> 1.15D;
            case "farmer", "harvester", "lumberjack", "builder", "mason", "carpenter" -> 1.1D;
            default -> 1.0D;
        };
        return actionType == JobActionType.KILL_BOSS ? modifier + 1.0D : modifier;
    }

    private static List<String> toStringList(JsonArray array) {
        List<String> list = new ArrayList<>();
        if (array == null) {
            return list;
        }
        for (JsonElement element : array) {
            list.add(element.getAsString());
        }
        return list;
    }

    private static JsonArray toJsonArray(List<String> values) {
        JsonArray array = new JsonArray();
        for (String value : values) {
            array.add(value);
        }
        return array;
    }
}
