package com.example.advancedjobs.config;

import com.example.advancedjobs.model.JobActionType;
import com.example.advancedjobs.model.JobDefinition;
import com.example.advancedjobs.util.ResourceLocationUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

final class ConfigValidationService {
    private ConfigValidationService() {
    }

    static JsonObject requireRootObject(JsonObject root, String configName) {
        if (root == null) {
            throw new ConfigValidationException(configName, "expected a JSON object at the root");
        }
        return root;
    }

    static JsonObject requireObject(JsonObject parent, String key, String path) {
        if (!parent.has(key) || !parent.get(key).isJsonObject()) {
            throw new ConfigValidationException(path + "." + key, "expected an object");
        }
        return parent.getAsJsonObject(key);
    }

    static JsonArray requireArray(JsonObject parent, String key, String path) {
        if (!parent.has(key) || !parent.get(key).isJsonArray()) {
            throw new ConfigValidationException(path + "." + key, "expected an array");
        }
        return parent.getAsJsonArray(key);
    }

    static JsonObject requireObjectElement(JsonElement element, String path) {
        if (element == null || !element.isJsonObject()) {
            throw new ConfigValidationException(path, "expected an object");
        }
        return element.getAsJsonObject();
    }

    static String requireString(JsonObject obj, String key, String path) {
        if (!obj.has(key) || !obj.get(key).isJsonPrimitive() || !obj.get(key).getAsJsonPrimitive().isString()) {
            throw new ConfigValidationException(path + "." + key, "expected a string");
        }
        return obj.get(key).getAsString();
    }

    static int requireInt(JsonObject obj, String key, String path) {
        if (!obj.has(key) || !obj.get(key).isJsonPrimitive() || !obj.get(key).getAsJsonPrimitive().isNumber()) {
            throw new ConfigValidationException(path + "." + key, "expected an integer number");
        }
        return obj.get(key).getAsInt();
    }

    static double requireDouble(JsonObject obj, String key, String path) {
        if (!obj.has(key) || !obj.get(key).isJsonPrimitive() || !obj.get(key).getAsJsonPrimitive().isNumber()) {
            throw new ConfigValidationException(path + "." + key, "expected a numeric value");
        }
        return obj.get(key).getAsDouble();
    }

    static String optionalString(JsonObject obj, String key, String path, String fallback) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) {
            return fallback;
        }
        if (!obj.get(key).isJsonPrimitive() || !obj.get(key).getAsJsonPrimitive().isString()) {
            throw new ConfigValidationException(path + "." + key, "expected a string");
        }
        return obj.get(key).getAsString();
    }

    static int optionalInt(JsonObject obj, String key, String path, int fallback) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) {
            return fallback;
        }
        if (!obj.get(key).isJsonPrimitive() || !obj.get(key).getAsJsonPrimitive().isNumber()) {
            throw new ConfigValidationException(path + "." + key, "expected an integer number");
        }
        return obj.get(key).getAsInt();
    }

    static double optionalDouble(JsonObject obj, String key, String path, double fallback) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) {
            return fallback;
        }
        if (!obj.get(key).isJsonPrimitive() || !obj.get(key).getAsJsonPrimitive().isNumber()) {
            throw new ConfigValidationException(path + "." + key, "expected a numeric value");
        }
        return obj.get(key).getAsDouble();
    }

    static ResourceLocation requireResourceLocation(JsonObject obj, String key, String path) {
        String id = requireString(obj, key, path);
        try {
            return ResourceLocationUtil.parse(id);
        } catch (Exception e) {
            throw new ConfigValidationException(path + "." + key, "expected a valid resource location: " + id, e);
        }
    }

    static <E extends Enum<E>> E requireEnum(Class<E> enumType, JsonObject obj, String key, String path) {
        String value = requireString(obj, key, path);
        try {
            return Enum.valueOf(enumType, value);
        } catch (Exception e) {
            throw new ConfigValidationException(path + "." + key, "expected one of " + List.of(enumType.getEnumConstants()) + " but got " + value, e);
        }
    }

    static void validatePositiveInt(String path, int value, boolean allowZero) {
        if (allowZero ? value < 0 : value <= 0) {
            throw new ConfigValidationException(path, allowZero ? "expected a non-negative integer" : "expected an integer greater than 0");
        }
    }

    static void validatePositiveDouble(String path, double value, boolean allowZero) {
        if (allowZero ? value < 0.0D : value <= 0.0D) {
            throw new ConfigValidationException(path, allowZero ? "expected a non-negative number" : "expected a number greater than 0");
        }
    }

    static void validateUniqueId(Set<String> ids, String id, String path) {
        if (!ids.add(id)) {
            throw new ConfigValidationException(path, "duplicate id: " + id);
        }
    }

    static void validateCrossConfigReferences(Map<String, JobDefinition> jobs,
                                              List<ConfigManager.DailyTaskTemplate> dailyTasks,
                                              List<ConfigManager.ContractTemplate> contracts) {
        Set<String> dailyIds = new java.util.HashSet<>();
        for (ConfigManager.DailyTaskTemplate task : dailyTasks) {
            dailyIds.add(task.id());
        }
        Set<String> contractIds = new java.util.HashSet<>();
        for (ConfigManager.ContractTemplate contract : contracts) {
            contractIds.add(contract.id());
        }
        for (JobDefinition definition : jobs.values()) {
            for (String taskId : definition.dailyTaskPool()) {
                if (!dailyIds.contains(taskId)) {
                    throw new ConfigValidationException("jobs.dailyTaskPool", "job " + definition.id() + " references missing daily task id " + taskId);
                }
            }
            for (String contractId : definition.contractPool()) {
                if (!contractIds.contains(contractId)) {
                    throw new ConfigValidationException("jobs.contractPool", "job " + definition.id() + " references missing contract id " + contractId);
                }
            }
        }
    }

    static void validateMultiplierMap(JsonObject root, String key, String path) {
        if (!root.has(key) || root.get(key).isJsonNull()) {
            return;
        }
        if (!root.get(key).isJsonObject()) {
            throw new ConfigValidationException(path + "." + key, "expected an object");
        }
        for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject(key).entrySet()) {
            if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isNumber()) {
                throw new ConfigValidationException(path + "." + key + "." + entry.getKey(), "expected a numeric multiplier");
            }
        }
    }

    static final class ConfigValidationException extends IllegalStateException {
        ConfigValidationException(String path, String message) {
            super("Invalid config at " + path + ": " + message);
        }

        ConfigValidationException(String path, String message, Throwable cause) {
            super("Invalid config at " + path + ": " + message, cause);
        }
    }
}
