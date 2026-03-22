# Changelog

## 1.0.14

Maintenance update.

Highlights:

- extracted right-click block interaction rewards from `JobEventHandler` into a dedicated `JobBlockInteractionHandler`
- moved enchanting, brewing, loot-container, and redstone interaction logic behind a focused helper without changing gameplay outcomes
- synchronized release metadata for the second event-handler cleanup step

## 1.0.13

Maintenance update.

Highlights:

- extracted service desk, board, and NPC interaction routing from `JobEventHandler` into a dedicated `ServiceDeskInteractionHandler`
- reduced `JobEventHandler` size and responsibility surface while keeping the existing board GUI and hint behavior intact
- synchronized release metadata for the first large event-handler refactor step

## 1.0.12

Maintenance update.

Highlights:

- extracted config validation logic into a dedicated validation service to reduce `ConfigManager` growth
- kept the new structural and business-rule checks while improving internal separation of responsibilities
- synchronized release metadata for the architecture cleanup step

## 1.0.11

Maintenance update.

Highlights:

- added business-rule validation for duplicate ids, invalid numeric ranges, and broken config references
- jobs config now checks daily-task and contract pool references against loaded task and contract ids
- perk trees now validate duplicate node ids and invalid parent links before runtime use

## 1.0.10

Maintenance update.

Highlights:

- added structural validation for jobs, perks, daily tasks, contracts, and economy JSON configs
- config load failures now report which field path is invalid before falling back to defaults or safe empty data
- synchronized version references for the config-validation release

## 1.0.9

Maintenance update.

Highlights:

- added a full project roadmap document to anchor future development priorities
- added a regression testing checklist for gameplay, config, economy, GUI, and hub workflows
- added a release checklist to standardize versioning, docs, build verification, and publish steps

## 1.0.8

Maintenance update.

Highlights:

- removed the remaining unchecked cast warning from creative tab registration
- kept the Forge `47.4.18` build warning-free apart from standard Gradle notes
- synchronized version references for the clean-build release

## 1.0.7

Maintenance update.

Highlights:

- silenced the final Forge `47.4.18` removal warnings in mod bootstrap where Forge still exposes deprecated context accessors
- kept the bootstrap behavior unchanged while finishing the compatibility cleanup pass
- synchronized the mod version for the warning-free build release

## 1.0.6

Maintenance update.

Highlights:

- replaced deprecated `ResourceLocation` constructors with current Forge-compatible factory methods across config, GUI, jobs, networking, and event code
- reduced compiler noise on Forge `47.4.18` while preserving existing gameplay behavior
- synchronized the mod version for the compatibility cleanup release

## 1.0.5

Maintenance update.

Highlights:

- updated the project to the current official Minecraft Forge `47.4.18` for Minecraft `1.20.1`
- raised the minimum required Forge version in mod metadata to `47.4.18+`
- synchronized version references in build and setup documentation

## 1.0.4

Maintenance update.

Highlights:

- updated the project to the current official Minecraft Forge `47.3.34` for Minecraft `1.20.1`
- raised the minimum required Forge version in mod metadata to match the updated build target
- synchronized README and installation documentation with the new Forge baseline

## 1.0.3

Maintenance update.

Highlights:

- optional JEI and JourneyMap dependencies are now detected from nearby `mods` jars by Forge mod metadata, not just filename prefixes
- documentation was updated to describe metadata-based auto-discovery for optional integrations
- version references were synchronized for the new release

## 1.0.2

Maintenance update.

Highlights:

- JEI and JourneyMap compile-time jars are now auto-detected from nearby `mods` folders instead of a fixed local path
- config files now live in `config/ZAdvancedJobs/` with automatic migration from the old `config/advancedjobs/` folder
- synchronized build and setup documentation with the new version and config path

## 1.0.1

Maintenance update.

Highlights:

- removed the unused root-level `icon-pack.png` asset to keep the repository cleaner
- fixed the Gradle wrapper script permissions for local Unix-like builds
- removed machine-specific build paths from Gradle and switched optional JEI and JourneyMap jars to `libs/optional`
- synchronized documentation with the new mod version

## 1.0.0

Initial public release of `Advanced Jobs RPG`.

Highlights:

- jobs, salary, skills, daily tasks, contracts, titles, milestones, and leaderboard
- service-desk NPC hub with admin deployment and repair tooling
- compact vanilla-style GUI with dedicated screens and screenshots
- Russian and English localization
- external economy integration through the public `Z_Economy` API
- JSON-driven configuration for jobs, perks, daily tasks, contracts, labels, and skins
