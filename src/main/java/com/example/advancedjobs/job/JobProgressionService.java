package com.example.advancedjobs.job;

import com.example.advancedjobs.model.JobProgress;
import com.example.advancedjobs.model.PlayerJobProfile;
import com.example.advancedjobs.util.TextUtil;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

class JobProgressionService {
    void evaluateMilestones(ServerPlayer player,
                            PlayerJobProfile profile,
                            String jobId,
                            JobProgress progress,
                            Consumer<String> grantTitle) {
        unlockMilestoneIf(player, progress, "level_10", progress.level() >= 10, grantTitle);
        unlockMilestoneIf(player, progress, "level_25", progress.level() >= 25, grantTitle);
        unlockMilestoneIf(player, progress, "level_50", progress.level() >= 50, grantTitle);
        unlockMilestoneIf(player, progress, "actions_100", totalActionCount(progress) >= 100, grantTitle);
        unlockMilestoneIf(player, progress, "salary_10000", progress.earnedTotal() >= 10_000.0D, grantTitle);
        unlockMilestoneIf(player, progress, "daily_10", progress.dailyHistory().size() >= 10, grantTitle);
        unlockMilestoneIf(player, progress, "contracts_10", progress.contractHistory().size() >= 10, grantTitle);
        evaluateProfessionMilestones(player, progress, jobId, grantTitle);
        profile.markDirty();
    }

    private void evaluateProfessionMilestones(ServerPlayer player, JobProgress progress, String jobId, Consumer<String> grantTitle) {
        switch (jobId) {
            case "miner", "deep_miner" -> {
                unlockMilestoneIf(player, progress, "miner_diamond_25",
                    actionCount(progress, "BREAK_BLOCK|minecraft:diamond_ore") + actionCount(progress, "BREAK_BLOCK|minecraft:deepslate_diamond_ore") >= 25, grantTitle);
                unlockMilestoneIf(player, progress, "miner_redstone_100",
                    actionCount(progress, "BREAK_BLOCK|minecraft:redstone_ore") >= 100, grantTitle);
                unlockMilestoneIf(player, progress, "miner_emerald_16",
                    actionCount(progress, "BREAK_BLOCK|minecraft:emerald_ore") + actionCount(progress, "BREAK_BLOCK|minecraft:deepslate_emerald_ore") >= 16, grantTitle);
                unlockMilestoneIf(player, progress, "deep_miner_deepslate_750",
                    actionCount(progress, "BREAK_BLOCK|minecraft:deepslate") >= 750, grantTitle);
                unlockMilestoneIf(player, progress, "deep_miner_diamond_48",
                    actionCount(progress, "BREAK_BLOCK|minecraft:deepslate_diamond_ore") >= 48, grantTitle);
            }
            case "lumberjack", "forester" -> {
                unlockMilestoneIf(player, progress, "lumberjack_logs_500", totalBreakCount(progress) >= 500, grantTitle);
                unlockMilestoneIf(player, progress, "lumberjack_oak_250",
                    actionCount(progress, "BREAK_BLOCK|minecraft:oak_log") + actionCount(progress, "BREAK_BLOCK|minecraft:stripped_oak_log") >= 250, grantTitle);
                unlockMilestoneIf(player, progress, "forester_cherry_100", actionCount(progress, "BREAK_BLOCK|minecraft:cherry_log") >= 100, grantTitle);
                unlockMilestoneIf(player, progress, "forester_birch_150", actionCount(progress, "BREAK_BLOCK|minecraft:birch_log") >= 150, grantTitle);
                unlockMilestoneIf(player, progress, "forester_spruce_180", actionCount(progress, "BREAK_BLOCK|minecraft:spruce_log") >= 180, grantTitle);
            }
            case "farmer", "harvester" -> {
                unlockMilestoneIf(player, progress, "farmer_harvest_500", totalActionTypeCount(progress, "HARVEST_CROP") >= 500, grantTitle);
                unlockMilestoneIf(player, progress, "farmer_planter_250", totalActionTypeCount(progress, "PLANT_CROP") >= 250, grantTitle);
                unlockMilestoneIf(player, progress, "farmer_wheat_384", actionCount(progress, "HARVEST_CROP|minecraft:wheat") >= 384, grantTitle);
                unlockMilestoneIf(player, progress, "harvester_patch_128",
                    actionCount(progress, "HARVEST_CROP|minecraft:pumpkin") + actionCount(progress, "HARVEST_CROP|minecraft:melon") >= 128, grantTitle);
                unlockMilestoneIf(player, progress, "harvester_roots_256",
                    actionCount(progress, "HARVEST_CROP|minecraft:potatoes")
                        + actionCount(progress, "HARVEST_CROP|minecraft:carrots")
                        + actionCount(progress, "HARVEST_CROP|minecraft:beetroots") >= 256, grantTitle);
            }
            case "fisher" -> {
                unlockMilestoneIf(player, progress, "fisher_cod_128", actionCount(progress, "FISH|minecraft:cod") >= 128, grantTitle);
                unlockMilestoneIf(player, progress, "fisher_treasure_24",
                    actionCount(progress, "FISH|minecraft:name_tag") + actionCount(progress, "FISH|minecraft:enchanted_book") >= 24, grantTitle);
                unlockMilestoneIf(player, progress, "fisher_salmon_96", actionCount(progress, "FISH|minecraft:salmon") >= 96, grantTitle);
            }
            case "animal_breeder" -> {
                unlockMilestoneIf(player, progress, "animal_breeder_stock_150", totalActionTypeCount(progress, "BREED_ANIMAL") >= 150, grantTitle);
                unlockMilestoneIf(player, progress, "animal_breeder_cattle_75", actionCount(progress, "BREED_ANIMAL|minecraft:cow") >= 75, grantTitle);
                unlockMilestoneIf(player, progress, "animal_breeder_chicken_96", actionCount(progress, "BREED_ANIMAL|minecraft:chicken") >= 96, grantTitle);
            }
            case "hunter", "monster_slayer" -> {
                unlockMilestoneIf(player, progress, "hunter_zombie_100", actionCount(progress, "KILL_MOB|minecraft:zombie") >= 100, grantTitle);
                unlockMilestoneIf(player, progress, "hunter_spider_120",
                    actionCount(progress, "KILL_MOB|minecraft:spider") + actionCount(progress, "KILL_MOB|minecraft:cave_spider") >= 120, grantTitle);
                unlockMilestoneIf(player, progress, "monster_slayer_creeper_120", actionCount(progress, "KILL_MOB|minecraft:creeper") >= 120, grantTitle);
            }
            case "builder", "mason", "carpenter" -> {
                unlockMilestoneIf(player, progress, "builder_blocks_500", totalActionTypeCount(progress, "PLACE_BLOCK") >= 500, grantTitle);
                unlockMilestoneIf(player, progress, "builder_decor_200",
                    actionCount(progress, "PLACE_BLOCK|minecraft:terracotta") + actionCount(progress, "PLACE_BLOCK|minecraft:glass") >= 200, grantTitle);
                unlockMilestoneIf(player, progress, "builder_glass_150",
                    actionCount(progress, "PLACE_BLOCK|minecraft:glass") + actionCount(progress, "PLACE_BLOCK|minecraft:glass_pane") >= 150, grantTitle);
                unlockMilestoneIf(player, progress, "mason_bricks_300",
                    actionCount(progress, "PLACE_BLOCK|minecraft:stone_bricks")
                        + actionCount(progress, "PLACE_BLOCK|minecraft:bricks") >= 300, grantTitle);
                unlockMilestoneIf(player, progress, "mason_polished_220",
                    actionCount(progress, "PLACE_BLOCK|minecraft:polished_andesite") >= 220, grantTitle);
                unlockMilestoneIf(player, progress, "carpenter_planks_400",
                    actionCount(progress, "PLACE_BLOCK|minecraft:oak_planks")
                        + actionCount(progress, "PLACE_BLOCK|minecraft:spruce_planks")
                        + actionCount(progress, "PLACE_BLOCK|minecraft:birch_planks") >= 400, grantTitle);
                unlockMilestoneIf(player, progress, "carpenter_stairs_240",
                    actionCount(progress, "PLACE_BLOCK|minecraft:oak_stairs")
                        + actionCount(progress, "PLACE_BLOCK|minecraft:spruce_stairs")
                        + actionCount(progress, "PLACE_BLOCK|minecraft:birch_stairs") >= 240, grantTitle);
            }
            case "merchant" -> {
                unlockMilestoneIf(player, progress, "merchant_trade_100", totalActionTypeCount(progress, "TRADE_WITH_VILLAGER") >= 100, grantTitle);
                unlockMilestoneIf(player, progress, "merchant_emerald_256", actionCount(progress, "TRADE_WITH_VILLAGER|minecraft:emerald") >= 256, grantTitle);
                unlockMilestoneIf(player, progress, "merchant_cache_24", totalActionTypeCount(progress, "OPEN_LOOT_CHEST") >= 24, grantTitle);
            }
            case "alchemist", "enchanter" -> {
                unlockMilestoneIf(player, progress, "alchemist_brew_64", totalActionTypeCount(progress, "BREW_POTION") >= 64, grantTitle);
                unlockMilestoneIf(player, progress, "enchanter_books_32", actionCount(progress, "ENCHANT_ITEM|minecraft:book") >= 32, grantTitle);
                unlockMilestoneIf(player, progress, "alchemist_glass_128", actionCount(progress, "BREW_POTION|minecraft:glass_bottle") >= 128, grantTitle);
                unlockMilestoneIf(player, progress, "enchanter_table_96", totalActionTypeCount(progress, "ENCHANT_ITEM") >= 96, grantTitle);
                unlockMilestoneIf(player, progress, "alchemist_wart_128", actionCount(progress, "BREW_POTION|minecraft:nether_wart") >= 128, grantTitle);
                unlockMilestoneIf(player, progress, "enchanter_lapis_192", actionCount(progress, "ENCHANT_ITEM|minecraft:lapis_lazuli") >= 192, grantTitle);
            }
            case "blacksmith" -> {
                unlockMilestoneIf(player, progress, "blacksmith_iron_128", actionCount(progress, "SMELT_ITEM|minecraft:iron_ingot") >= 128, grantTitle);
                unlockMilestoneIf(player, progress, "blacksmith_gold_96", actionCount(progress, "SMELT_ITEM|minecraft:gold_ingot") >= 96, grantTitle);
            }
            case "armorer" -> {
                unlockMilestoneIf(player, progress, "armorer_iron_set_48",
                    actionCount(progress, "CRAFT_ITEM|minecraft:iron_chestplate")
                        + actionCount(progress, "CRAFT_ITEM|minecraft:iron_helmet")
                        + actionCount(progress, "CRAFT_ITEM|minecraft:iron_leggings")
                        + actionCount(progress, "CRAFT_ITEM|minecraft:iron_boots") >= 48, grantTitle);
                unlockMilestoneIf(player, progress, "armorer_shield_64", actionCount(progress, "CRAFT_ITEM|minecraft:shield") >= 64, grantTitle);
            }
            case "cook" -> {
                unlockMilestoneIf(player, progress, "cook_feast_128",
                    actionCount(progress, "CRAFT_ITEM|minecraft:bread")
                        + actionCount(progress, "CRAFT_ITEM|minecraft:cooked_beef")
                        + actionCount(progress, "CRAFT_ITEM|minecraft:baked_potato") >= 128, grantTitle);
                unlockMilestoneIf(player, progress, "cook_sweets_96",
                    actionCount(progress, "CRAFT_ITEM|minecraft:cake")
                        + actionCount(progress, "CRAFT_ITEM|minecraft:pumpkin_pie") >= 96, grantTitle);
            }
            case "explorer", "treasure_hunter", "archaeologist" -> {
                unlockMilestoneIf(player, progress, "explorer_chunks_64", totalActionTypeCount(progress, "EXPLORE_CHUNK") >= 64, grantTitle);
                unlockMilestoneIf(player, progress, "treasure_loot_25", totalActionTypeCount(progress, "OPEN_LOOT_CHEST") >= 25, grantTitle);
                unlockMilestoneIf(player, progress, "explorer_cache_40", totalActionTypeCount(progress, "OPEN_LOOT_CHEST") >= 40, grantTitle);
                unlockMilestoneIf(player, progress, "explorer_distance_128", totalActionTypeCount(progress, "EXPLORE_CHUNK") >= 128, grantTitle);
                unlockMilestoneIf(player, progress, "explorer_route_192", totalActionTypeCount(progress, "EXPLORE_CHUNK") >= 192, grantTitle);
                unlockMilestoneIf(player, progress, "treasure_hunter_tags_16",
                    actionCount(progress, "FISH|minecraft:name_tag") + actionCount(progress, "OPEN_LOOT_CHEST|minecraft:name_tag") >= 16, grantTitle);
                unlockMilestoneIf(player, progress, "treasure_hunter_loot_80", totalActionTypeCount(progress, "OPEN_LOOT_CHEST") >= 80, grantTitle);
                unlockMilestoneIf(player, progress, "archaeologist_relic_32",
                    actionCount(progress, "OPEN_LOOT_CHEST|minecraft:brush")
                        + actionCount(progress, "BREAK_BLOCK|minecraft:suspicious_sand")
                        + actionCount(progress, "BREAK_BLOCK|minecraft:suspicious_gravel") >= 32, grantTitle);
                unlockMilestoneIf(player, progress, "archaeologist_brush_96", actionCount(progress, "OPEN_LOOT_CHEST|minecraft:brush") >= 96, grantTitle);
                unlockMilestoneIf(player, progress, "archaeologist_suspicious_96",
                    actionCount(progress, "BREAK_BLOCK|minecraft:suspicious_sand")
                        + actionCount(progress, "BREAK_BLOCK|minecraft:suspicious_gravel")
                        + actionCount(progress, "OPEN_LOOT_CHEST|minecraft:suspicious_sand") >= 96, grantTitle);
            }
            case "engineer", "redstone_technician" -> {
                unlockMilestoneIf(player, progress, "engineer_redstone_250", totalActionTypeCount(progress, "REDSTONE_USE") >= 250, grantTitle);
                unlockMilestoneIf(player, progress, "engineer_quartz_128",
                    actionCount(progress, "CRAFT_ITEM|minecraft:comparator")
                        + actionCount(progress, "CRAFT_ITEM|minecraft:observer")
                        + actionCount(progress, "CRAFT_ITEM|minecraft:daylight_detector") >= 128, grantTitle);
                unlockMilestoneIf(player, progress, "redstone_technician_repeaters_192",
                    actionCount(progress, "CRAFT_ITEM|minecraft:repeater")
                        + actionCount(progress, "CRAFT_ITEM|minecraft:redstone_torch") >= 192, grantTitle);
                unlockMilestoneIf(player, progress, "redstone_technician_comparators_96", actionCount(progress, "CRAFT_ITEM|minecraft:comparator") >= 96, grantTitle);
            }
            case "guard", "bounty_hunter", "defender" -> {
                unlockMilestoneIf(player, progress, "guard_patrol_150", totalActionTypeCount(progress, "KILL_MOB") >= 150, grantTitle);
                unlockMilestoneIf(player, progress, "bounty_hunter_blaze_50", actionCount(progress, "KILL_MOB|minecraft:blaze") >= 50, grantTitle);
                unlockMilestoneIf(player, progress, "defender_skeleton_120",
                    actionCount(progress, "KILL_MOB|minecraft:skeleton") + actionCount(progress, "KILL_MOB|minecraft:stray") >= 120, grantTitle);
                unlockMilestoneIf(player, progress, "guard_raider_80",
                    actionCount(progress, "KILL_MOB|minecraft:pillager")
                        + actionCount(progress, "KILL_MOB|minecraft:vindicator") >= 80, grantTitle);
                unlockMilestoneIf(player, progress, "bounty_hunter_witch_36", actionCount(progress, "KILL_MOB|minecraft:witch") >= 36, grantTitle);
                unlockMilestoneIf(player, progress, "defender_zombie_180",
                    actionCount(progress, "KILL_MOB|minecraft:zombie")
                        + actionCount(progress, "KILL_MOB|minecraft:husk") >= 180, grantTitle);
            }
            case "boss_hunter" -> {
                unlockMilestoneIf(player, progress, "boss_hunter_boss_5", totalActionTypeCount(progress, "KILL_BOSS") >= 5, grantTitle);
                unlockMilestoneIf(player, progress, "boss_hunter_boss_12", totalActionTypeCount(progress, "KILL_BOSS") >= 12, grantTitle);
            }
            case "quarry_worker" -> {
                unlockMilestoneIf(player, progress, "quarry_stone_1000", actionCount(progress, "BREAK_BLOCK|minecraft:stone") >= 1000, grantTitle);
                unlockMilestoneIf(player, progress, "quarry_deepslate_600", actionCount(progress, "BREAK_BLOCK|minecraft:deepslate") >= 600, grantTitle);
            }
            case "digger" -> {
                unlockMilestoneIf(player, progress, "digger_gravel_500", actionCount(progress, "BREAK_BLOCK|minecraft:gravel") >= 500, grantTitle);
                unlockMilestoneIf(player, progress, "digger_clay_320",
                    actionCount(progress, "BREAK_BLOCK|minecraft:clay") + actionCount(progress, "BREAK_BLOCK|minecraft:mud") >= 320, grantTitle);
            }
            case "sand_collector" -> {
                unlockMilestoneIf(player, progress, "sand_collector_sand_1000", actionCount(progress, "BREAK_BLOCK|minecraft:sand") >= 1000, grantTitle);
                unlockMilestoneIf(player, progress, "sand_collector_red_400", actionCount(progress, "BREAK_BLOCK|minecraft:red_sand") >= 400, grantTitle);
            }
            case "ice_harvester" -> {
                unlockMilestoneIf(player, progress, "ice_harvester_blue_100", actionCount(progress, "BREAK_BLOCK|minecraft:blue_ice") >= 100, grantTitle);
                unlockMilestoneIf(player, progress, "ice_harvester_packed_320", actionCount(progress, "BREAK_BLOCK|minecraft:packed_ice") >= 320, grantTitle);
            }
            case "shepherd" -> {
                unlockMilestoneIf(player, progress, "shepherd_flock_100", actionCount(progress, "BREED_ANIMAL|minecraft:sheep") >= 100, grantTitle);
                unlockMilestoneIf(player, progress, "shepherd_wool_256",
                    actionCount(progress, "CRAFT_ITEM|minecraft:white_wool")
                        + actionCount(progress, "CRAFT_ITEM|minecraft:black_wool")
                        + actionCount(progress, "CRAFT_ITEM|minecraft:gray_wool") >= 256, grantTitle);
                unlockMilestoneIf(player, progress, "shepherd_carpet_192",
                    actionCount(progress, "CRAFT_ITEM|minecraft:white_carpet")
                        + actionCount(progress, "CRAFT_ITEM|minecraft:red_carpet")
                        + actionCount(progress, "CRAFT_ITEM|minecraft:black_carpet") >= 192, grantTitle);
            }
            case "beekeeper" -> {
                unlockMilestoneIf(player, progress, "beekeeper_honey_128", actionCount(progress, "CRAFT_ITEM|minecraft:honey_bottle") >= 128, grantTitle);
                unlockMilestoneIf(player, progress, "beekeeper_comb_192",
                    actionCount(progress, "BREAK_BLOCK|minecraft:beehive")
                        + actionCount(progress, "BREAK_BLOCK|minecraft:bee_nest")
                        + actionCount(progress, "CRAFT_ITEM|minecraft:honeycomb") >= 192, grantTitle);
                unlockMilestoneIf(player, progress, "beekeeper_hive_64",
                    actionCount(progress, "BREAK_BLOCK|minecraft:beehive")
                        + actionCount(progress, "BREAK_BLOCK|minecraft:bee_nest") >= 64, grantTitle);
            }
            case "herbalist" -> {
                unlockMilestoneIf(player, progress, "herbalist_wart_256", actionCount(progress, "BREAK_BLOCK|minecraft:nether_wart") >= 256, grantTitle);
                unlockMilestoneIf(player, progress, "herbalist_berries_192",
                    actionCount(progress, "BREAK_BLOCK|minecraft:sweet_berry_bush")
                        + actionCount(progress, "BREAK_BLOCK|minecraft:cave_vines") >= 192, grantTitle);
                unlockMilestoneIf(player, progress, "herbalist_potion_72", totalActionTypeCount(progress, "BREW_POTION") >= 72, grantTitle);
            }
            default -> {
            }
        }
    }

    private void unlockMilestoneIf(ServerPlayer player, JobProgress progress, String milestoneId, boolean condition, Consumer<String> grantTitle) {
        if (condition && progress.unlockMilestone(milestoneId)) {
            player.sendSystemMessage(TextUtil.tr("message.advancedjobs.milestone_unlocked",
                TextUtil.tr("milestone.advancedjobs." + milestoneId)));
            grantTitle.accept(titleIdForMilestone(milestoneId));
        }
    }

    private int totalActionCount(JobProgress progress) {
        return progress.actionStats().values().stream().mapToInt(Integer::intValue).sum();
    }

    private int totalActionTypeCount(JobProgress progress, String actionType) {
        return progress.actionStats().entrySet().stream()
            .filter(entry -> entry.getKey().startsWith(actionType + "|"))
            .mapToInt(Map.Entry::getValue)
            .sum();
    }

    private int totalBreakCount(JobProgress progress) {
        return totalActionTypeCount(progress, "BREAK_BLOCK");
    }

    private int actionCount(JobProgress progress, String key) {
        return progress.actionStats().getOrDefault(key, 0);
    }

    private String titleIdForMilestone(String milestoneId) {
        return switch (milestoneId) {
            case "miner_diamond_25" -> "gem_cutter";
            case "miner_redstone_100" -> "redstone_vein";
            case "miner_emerald_16" -> "emerald_core";
            case "deep_miner_deepslate_750" -> "deepslate_heart";
            case "deep_miner_diamond_48" -> "vein_lord";
            case "lumberjack_logs_500" -> "timber_champion";
            case "lumberjack_oak_250" -> "oak_warden";
            case "forester_cherry_100" -> "cherry_keeper";
            case "forester_birch_150" -> "birch_sentinel";
            case "forester_spruce_180" -> "evergreen_keeper";
            case "farmer_harvest_500" -> "field_master";
            case "farmer_planter_250" -> "green_thumb";
            case "farmer_wheat_384" -> "grainlord";
            case "harvester_patch_128" -> "harvest_titan";
            case "harvester_roots_256" -> "root_reaper";
            case "fisher_cod_128" -> "river_provider";
            case "fisher_treasure_24" -> "deep_angler";
            case "fisher_salmon_96" -> "current_hunter";
            case "animal_breeder_stock_150" -> "stock_breeder";
            case "animal_breeder_cattle_75" -> "cattle_baron";
            case "animal_breeder_chicken_96" -> "henkeeper";
            case "hunter_zombie_100" -> "undead_bane";
            case "hunter_spider_120" -> "web_reaper";
            case "boss_hunter_boss_5" -> "bossbreaker";
            case "monster_slayer_creeper_120" -> "blast_bane";
            case "builder_blocks_500" -> "stonehands";
            case "builder_decor_200" -> "artisan_builder";
            case "builder_glass_150" -> "lightweaver";
            case "mason_bricks_300" -> "keystone_master";
            case "mason_polished_220" -> "andesite_archon";
            case "carpenter_planks_400" -> "timberwright";
            case "carpenter_stairs_240" -> "stairwright";
            case "merchant_trade_100" -> "deal_maker";
            case "merchant_emerald_256" -> "emerald_broker";
            case "merchant_cache_24" -> "caravan_factor";
            case "alchemist_brew_64" -> "potion_savant";
            case "alchemist_glass_128" -> "flask_master";
            case "alchemist_wart_128" -> "nether_distiller";
            case "enchanter_books_32" -> "rune_reader";
            case "enchanter_table_96" -> "arcane_lector";
            case "enchanter_lapis_192" -> "lapis_scholar";
            case "blacksmith_iron_128" -> "forge_keeper";
            case "blacksmith_gold_96" -> "golden_anvil";
            case "armorer_iron_set_48" -> "steel_tailor";
            case "armorer_shield_64" -> "shieldwright";
            case "cook_feast_128" -> "feastcaller";
            case "cook_sweets_96" -> "sugar_master";
            case "explorer_chunks_64" -> "trailblazer";
            case "explorer_cache_40" -> "path_seeker";
            case "explorer_distance_128" -> "horizon_runner";
            case "explorer_route_192" -> "far_pathfinder";
            case "treasure_loot_25" -> "vault_seeker";
            case "treasure_hunter_tags_16" -> "tide_reclaimer";
            case "treasure_hunter_loot_80" -> "wreck_diviner";
            case "archaeologist_relic_32" -> "dust_curator";
            case "archaeologist_brush_96" -> "brush_keeper";
            case "archaeologist_suspicious_96" -> "relic_sifter";
            case "engineer_redstone_250" -> "circuit_master";
            case "engineer_quartz_128" -> "signal_architect";
            case "redstone_technician_repeaters_192" -> "pulse_smith";
            case "redstone_technician_comparators_96" -> "relay_lord";
            case "guard_patrol_150" -> "warden_of_roads";
            case "guard_raider_80" -> "raid_marshal";
            case "bounty_hunter_blaze_50" -> "ember_hunter";
            case "bounty_hunter_witch_36" -> "hex_tracker";
            case "defender_skeleton_120" -> "shield_of_dawn";
            case "defender_zombie_180" -> "bulwark_of_ashes";
            case "boss_hunter_boss_12" -> "void_slayer";
            case "quarry_stone_1000" -> "heart_of_stone";
            case "quarry_deepslate_600" -> "deep_quarry";
            case "digger_gravel_500" -> "graveborn_digger";
            case "digger_clay_320" -> "mud_delver";
            case "sand_collector_sand_1000" -> "sea_of_sand";
            case "sand_collector_red_400" -> "red_dune_keeper";
            case "ice_harvester_blue_100" -> "blue_frost";
            case "ice_harvester_packed_320" -> "glacier_hand";
            case "shepherd_flock_100" -> "high_shepherd";
            case "shepherd_wool_256" -> "wool_archon";
            case "shepherd_carpet_192" -> "loom_lord";
            case "beekeeper_honey_128" -> "hive_warden";
            case "beekeeper_comb_192" -> "comb_lord";
            case "beekeeper_hive_64" -> "queen_tender";
            case "herbalist_wart_256" -> "wart_whisperer";
            case "herbalist_berries_192" -> "berry_sage";
            case "herbalist_potion_72" -> "wild_apothecary";
            default -> null;
        };
    }

    Component titleComponent(String titleId) {
        if (titleId == null || titleId.isBlank()) {
            return TextUtil.tr("command.advancedjobs.common.none");
        }
        return TextUtil.tr("title.advancedjobs." + titleId);
    }
}
