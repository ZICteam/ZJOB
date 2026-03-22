package com.example.advancedjobs.data;

import com.example.advancedjobs.AdvancedJobsMod;
import com.example.advancedjobs.config.ConfigManager;
import com.example.advancedjobs.model.PlayerJobProfile;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

public class JsonPlayerDataRepository implements PlayerDataRepository {
    private static final Type MAP_TYPE = new TypeToken<Map<String, PlayerJobProfile>>() { }.getType();

    private final Map<UUID, PlayerJobProfile> cache = new LinkedHashMap<>();
    private Path dataFile;

    @Override
    public void init(MinecraftServer server) {
        dataFile = server.getWorldPath(LevelResource.ROOT).resolve("serverconfig").resolve("advancedjobs").resolve("players.json");
        try {
            Files.createDirectories(dataFile.getParent());
            if (!Files.exists(dataFile)) {
                saveAll(Collections.emptyList());
                return;
            }
            try (Reader reader = Files.newBufferedReader(dataFile, StandardCharsets.UTF_8)) {
                Map<String, PlayerJobProfile> raw = ConfigManager.GSON.fromJson(reader, MAP_TYPE);
                cache.clear();
                if (raw != null) {
                    for (Map.Entry<String, PlayerJobProfile> entry : raw.entrySet()) {
                        cache.put(UUID.fromString(entry.getKey()), entry.getValue());
                    }
                }
            }
        } catch (Exception e) {
            AdvancedJobsMod.LOGGER.error("Failed to init JSON repository", e);
            cache.clear();
        }
    }

    @Override
    public Optional<PlayerJobProfile> find(UUID playerId) {
        return Optional.ofNullable(cache.get(playerId));
    }

    @Override
    public Collection<PlayerJobProfile> all() {
        return cache.values();
    }

    @Override
    public void save(PlayerJobProfile profile) {
        cache.put(profile.playerId(), profile);
        writeAll();
    }

    @Override
    public void saveAll(Collection<PlayerJobProfile> profiles) {
        for (PlayerJobProfile profile : profiles) {
            cache.put(profile.playerId(), profile);
        }
        writeAll();
    }

    private void writeAll() {
        if (dataFile == null) {
            return;
        }
        Map<String, PlayerJobProfile> raw = new LinkedHashMap<>();
        for (Map.Entry<UUID, PlayerJobProfile> entry : cache.entrySet()) {
            raw.put(entry.getKey().toString(), entry.getValue());
        }
        try (Writer writer = Files.newBufferedWriter(dataFile, StandardCharsets.UTF_8)) {
            ConfigManager.GSON.toJson(raw, writer);
        } catch (IOException e) {
            AdvancedJobsMod.LOGGER.error("Failed to write player JSON data", e);
        }
    }
}
