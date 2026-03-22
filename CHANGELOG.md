# Changelog

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
