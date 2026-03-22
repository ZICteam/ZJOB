package com.example.advancedjobs.data;

import com.example.advancedjobs.AdvancedJobsMod;
import com.example.advancedjobs.config.ConfigManager;
import com.example.advancedjobs.model.PlayerJobProfile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

public class SqlitePlayerDataRepository implements PlayerDataRepository {
    private Path dbPath;

    @Override
    public void init(MinecraftServer server) {
        try {
            dbPath = server.getWorldPath(LevelResource.ROOT).resolve("serverconfig").resolve("advancedjobs").resolve("players.db");
            Files.createDirectories(dbPath.getParent());
            try (Connection connection = open(); Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE IF NOT EXISTS player_jobs (uuid TEXT PRIMARY KEY, payload TEXT NOT NULL)");
            }
        } catch (Exception e) {
            AdvancedJobsMod.LOGGER.error("Failed to init SQLite repository", e);
        }
    }

    @Override
    public Optional<PlayerJobProfile> find(UUID playerId) {
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement("SELECT payload FROM player_jobs WHERE uuid = ?")) {
            statement.setString(1, playerId.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                return Optional.ofNullable(ConfigManager.GSON.fromJson(result.getString("payload"), PlayerJobProfile.class));
            }
        } catch (Exception e) {
            AdvancedJobsMod.LOGGER.error("Failed to read profile from SQLite", e);
            return Optional.empty();
        }
    }

    @Override
    public Collection<PlayerJobProfile> all() {
        List<PlayerJobProfile> list = new ArrayList<>();
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement("SELECT payload FROM player_jobs");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                PlayerJobProfile profile = ConfigManager.GSON.fromJson(result.getString("payload"), PlayerJobProfile.class);
                if (profile != null) {
                    list.add(profile);
                }
            }
        } catch (Exception e) {
            AdvancedJobsMod.LOGGER.error("Failed to read all profiles from SQLite", e);
        }
        return list;
    }

    @Override
    public void save(PlayerJobProfile profile) {
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement("INSERT OR REPLACE INTO player_jobs (uuid, payload) VALUES (?, ?)")) {
            statement.setString(1, profile.playerId().toString());
            statement.setString(2, ConfigManager.GSON.toJson(profile));
            statement.executeUpdate();
        } catch (Exception e) {
            AdvancedJobsMod.LOGGER.error("Failed to save profile to SQLite", e);
        }
    }

    @Override
    public void saveAll(Collection<PlayerJobProfile> profiles) {
        for (PlayerJobProfile profile : profiles) {
            save(profile);
        }
    }

    private Connection open() throws Exception {
        return DriverManager.getConnection("jdbc:sqlite:" + dbPath.toAbsolutePath());
    }
}
