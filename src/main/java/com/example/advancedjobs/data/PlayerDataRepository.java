package com.example.advancedjobs.data;

import com.example.advancedjobs.model.PlayerJobProfile;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;

public interface PlayerDataRepository {
    void init(MinecraftServer server);

    Optional<PlayerJobProfile> find(UUID playerId);

    Collection<PlayerJobProfile> all();

    void save(PlayerJobProfile profile);

    void saveAll(Collection<PlayerJobProfile> profiles);
}
