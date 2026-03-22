package com.example.advancedjobs.client;

import com.example.advancedjobs.config.ConfigManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

public final class ClientJobState {
    private static JsonObject catalogRoot = new JsonObject();
    private static JsonObject playerRoot = new JsonObject();
    private static JsonObject leaderboardRoot = new JsonObject();

    private ClientJobState() {
    }

    public static void updateCatalog(String payload) {
        try {
            catalogRoot = ConfigManager.GSON.fromJson(payload, JsonObject.class);
            if (catalogRoot == null) {
                catalogRoot = new JsonObject();
            }
            NpcSkinManager.reload(catalogRoot);
        } catch (Exception ignored) {
            catalogRoot = new JsonObject();
        }
    }

    public static void update(String payload) {
        try {
            playerRoot = ConfigManager.GSON.fromJson(payload, JsonObject.class);
            if (playerRoot == null) {
                playerRoot = new JsonObject();
            }
        } catch (Exception ignored) {
            playerRoot = new JsonObject();
        }
    }

    public static void updateLeaderboard(String jobId, String payload) {
        try {
            JsonArray leaderboard = ConfigManager.GSON.fromJson(payload, JsonArray.class);
            if (leaderboard != null) {
                leaderboardRoot.add(jobId, leaderboard);
            }
        } catch (Exception ignored) {
        }
    }

    public static JsonObject root() {
        JsonObject merged = catalogRoot.deepCopy();
        for (var entry : playerRoot.entrySet()) {
            if (!"jobs".equals(entry.getKey())) {
                merged.add(entry.getKey(), entry.getValue());
            }
        }
        JsonArray jobs = new JsonArray();
        for (JsonObject job : jobs()) {
            jobs.add(job);
        }
        merged.add("jobs", jobs);
        return merged;
    }

    public static String activeJobId() {
        return playerRoot.has("activeJobId") && !playerRoot.get("activeJobId").isJsonNull() ? playerRoot.get("activeJobId").getAsString() : null;
    }

    public static String secondaryJobId() {
        return playerRoot.has("secondaryJobId") && !playerRoot.get("secondaryJobId").isJsonNull() ? playerRoot.get("secondaryJobId").getAsString() : null;
    }

    public static boolean allowSecondaryJob() {
        return playerRoot.has("allowSecondaryJob") && playerRoot.get("allowSecondaryJob").getAsBoolean();
    }

    public static boolean instantSalary() {
        return playerRoot.has("instantSalary") && playerRoot.get("instantSalary").getAsBoolean();
    }

    public static double jobChangePrice() {
        return playerRoot.has("jobChangePrice") ? playerRoot.get("jobChangePrice").getAsDouble() : 0.0D;
    }

    public static long jobChangeCooldownRemaining() {
        return playerRoot.has("jobChangeCooldownRemaining") ? playerRoot.get("jobChangeCooldownRemaining").getAsLong() : 0L;
    }

    public static long salaryClaimCooldownRemaining() {
        return playerRoot.has("salaryClaimCooldownRemaining") ? playerRoot.get("salaryClaimCooldownRemaining").getAsLong() : 0L;
    }

    public static double maxSalaryPerClaim() {
        return playerRoot.has("maxSalaryPerClaim") ? playerRoot.get("maxSalaryPerClaim").getAsDouble() : 0.0D;
    }

    public static double salaryTaxRate() {
        return playerRoot.has("salaryTaxRate") ? playerRoot.get("salaryTaxRate").getAsDouble() : 0.0D;
    }

    public static double contractRerollPrice() {
        return playerRoot.has("contractRerollPrice") ? playerRoot.get("contractRerollPrice").getAsDouble() : 0.0D;
    }

    public static long contractRerollCooldownRemaining() {
        return playerRoot.has("contractRerollCooldownRemaining") ? playerRoot.get("contractRerollCooldownRemaining").getAsLong() : 0L;
    }

    public static String economyProvider() {
        return playerRoot.has("economyProvider") ? playerRoot.get("economyProvider").getAsString() : "internal";
    }

    public static String economyCurrency() {
        return playerRoot.has("economyCurrency") ? playerRoot.get("economyCurrency").getAsString() : "z_coin";
    }

    public static String taxSinkAccountUuid() {
        return playerRoot.has("taxSinkAccountUuid")
            ? playerRoot.get("taxSinkAccountUuid").getAsString()
            : "00000000-0000-0000-0000-000000000001";
    }

    public static boolean blockArtificialMobRewards() {
        return playerRoot.has("blockArtificialMobRewards") && playerRoot.get("blockArtificialMobRewards").getAsBoolean();
    }

    public static boolean blockBabyMobRewards() {
        return playerRoot.has("blockBabyMobRewards") && playerRoot.get("blockBabyMobRewards").getAsBoolean();
    }

    public static boolean blockTamedMobRewards() {
        return playerRoot.has("blockTamedMobRewards") && playerRoot.get("blockTamedMobRewards").getAsBoolean();
    }

    public static long lootContainerRewardCooldownSeconds() {
        return playerRoot.has("lootContainerRewardCooldownSeconds") ? playerRoot.get("lootContainerRewardCooldownSeconds").getAsLong() : 0L;
    }

    public static long exploredChunkRewardCooldownSeconds() {
        return playerRoot.has("exploredChunkRewardCooldownSeconds") ? playerRoot.get("exploredChunkRewardCooldownSeconds").getAsLong() : 0L;
    }

    public static double balance() {
        return playerRoot.has("balance") ? playerRoot.get("balance").getAsDouble() : 0.0D;
    }

    public static String currentWorldId() {
        return playerRoot.has("currentWorldId") ? playerRoot.get("currentWorldId").getAsString() : "minecraft:overworld";
    }

    public static String currentBiomeId() {
        return playerRoot.has("currentBiomeId") ? playerRoot.get("currentBiomeId").getAsString() : "minecraft:plains";
    }

    public static double worldRewardMultiplier() {
        return playerRoot.has("worldRewardMultiplier") ? playerRoot.get("worldRewardMultiplier").getAsDouble() : 1.0D;
    }

    public static double biomeRewardMultiplier() {
        return playerRoot.has("biomeRewardMultiplier") ? playerRoot.get("biomeRewardMultiplier").getAsDouble() : 1.0D;
    }

    public static double eventRewardMultiplier() {
        return playerRoot.has("eventRewardMultiplier") ? playerRoot.get("eventRewardMultiplier").getAsDouble() : 1.0D;
    }

    public static long eventEndsAtEpochSecond() {
        return playerRoot.has("eventEndsAtEpochSecond") ? playerRoot.get("eventEndsAtEpochSecond").getAsLong() : 0L;
    }

    public static long eventRemainingSeconds() {
        return playerRoot.has("eventRemainingSeconds") ? playerRoot.get("eventRemainingSeconds").getAsLong() : 0L;
    }

    public static double vipRewardMultiplier() {
        return playerRoot.has("vipRewardMultiplier") ? playerRoot.get("vipRewardMultiplier").getAsDouble() : 1.0D;
    }

    public static double effectiveRewardMultiplier() {
        return playerRoot.has("effectiveRewardMultiplier") ? playerRoot.get("effectiveRewardMultiplier").getAsDouble() : 1.0D;
    }

    public static List<String> unlockedTitles() {
        List<String> list = new ArrayList<>();
        JsonArray titles = playerRoot.has("unlockedTitles") ? playerRoot.getAsJsonArray("unlockedTitles") : new JsonArray();
        for (JsonElement element : titles) {
            list.add(element.getAsString());
        }
        return list;
    }

    public static List<JsonObject> jobs() {
        List<JsonObject> list = new ArrayList<>();
        JsonArray catalogJobs = catalogRoot.has("jobs") ? catalogRoot.getAsJsonArray("jobs") : new JsonArray();
        JsonArray playerJobs = playerRoot.has("jobs") ? playerRoot.getAsJsonArray("jobs") : new JsonArray();
        for (JsonElement element : catalogJobs) {
            JsonObject merged = element.getAsJsonObject().deepCopy();
            String id = merged.get("id").getAsString();
            for (JsonElement playerElement : playerJobs) {
                JsonObject playerJob = playerElement.getAsJsonObject();
                if (!id.equals(playerJob.get("id").getAsString())) {
                    continue;
                }
                for (var entry : playerJob.entrySet()) {
                    if (!"id".equals(entry.getKey())) {
                        merged.add(entry.getKey(), entry.getValue());
                    }
                }
                if (merged.has("skillBranches") && playerJob.has("unlockedNodes")) {
                    applyUnlockedNodes(merged.getAsJsonArray("skillBranches"), playerJob.getAsJsonArray("unlockedNodes"));
                }
                break;
            }
            list.add(merged);
        }
        return list;
    }

    public static List<JsonObject> leaderboard(String jobId) {
        List<JsonObject> list = new ArrayList<>();
        if (jobId == null || !leaderboardRoot.has(jobId)) {
            return list;
        }
        JsonArray array = leaderboardRoot.getAsJsonArray(jobId);
        for (JsonElement element : array) {
            list.add(element.getAsJsonObject());
        }
        return list;
    }

    private static void applyUnlockedNodes(JsonArray branches, JsonArray unlockedNodes) {
        List<String> unlocked = new ArrayList<>();
        for (JsonElement element : unlockedNodes) {
            unlocked.add(element.getAsString());
        }
        for (JsonElement branchElement : branches) {
            JsonObject branch = branchElement.getAsJsonObject();
            JsonArray nodes = branch.getAsJsonArray("nodes");
            for (JsonElement nodeElement : nodes) {
                JsonObject node = nodeElement.getAsJsonObject();
                node.addProperty("unlocked", unlocked.contains(node.get("id").getAsString()));
            }
        }
    }
}
