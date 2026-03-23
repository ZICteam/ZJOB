# Changelog

## 1.0.64

Maintenance update.

Highlights:

- added clickable compact-screen flow links that jump directly between My Job, Skills, Daily, Contracts, and Salary
- improved compact GUI navigation so each work screen now points to the next relevant tab for the current slot
- synchronized release metadata for the compact cross-link navigation roadmap step

## 1.0.63

Maintenance update.

Highlights:

- added quick primary-secondary slot switching directly inside My Job, Daily, and Contracts screens
- improved compact-screen slot guidance with clickable in-screen hints instead of forcing players back to the jobs browser
- synchronized release metadata for the compact slot-switch UX roadmap step

## 1.0.62

Maintenance update.

Highlights:

- added slot-aware board-routing hints to My Job, Skills, Daily, and Contracts empty or onboarding states
- improved My Job slot hover guidance so players can see the right next service board without leaving the screen
- synchronized release metadata for the in-screen service-board routing roadmap step

## 1.0.61

Maintenance update.

Highlights:

- added a dedicated Help line that points players to the right service board for their current slot
- improved Help-screen navigation so the next action now includes a clearer board destination for salary, skills, dailies, and contracts
- synchronized release metadata for the service-board navigation UX roadmap step

## 1.0.60

Maintenance update.

Highlights:

- improved Daily and Contracts guidance with clearer next-action lines in command output and GUI tooltips
- strengthened empty states for task loops so players understand when to wait, reroll, or keep pushing progression
- synchronized release metadata for the tasks-and-contracts UX roadmap step

## 1.0.59

Maintenance update.

Highlights:

- improved Skill Tree onboarding with clearer next-action tooltip states for available and locked nodes
- added stronger progression guidance when a profession has no skill tree data yet
- synchronized release metadata for the skill-navigation UX roadmap step

## 1.0.58

Maintenance update.

Highlights:

- improved player-facing progression guidance in `/jobs info` and `/jobs stats` with clearer skill-point and milestone focus
- added progression-focused My Job tooltips for XP, skill points, and milestone hover states
- synchronized release metadata for the progression UX roadmap step

## 1.0.57

Maintenance update.

Highlights:

- added `/jobsadmin balanceprogress <job>` to inspect profession-level skill point pressure, unlocked nodes, milestones, and adoption share
- linked the new progression diagnostic into README, installation, commands, testing, and balance guidance
- synchronized release metadata for the progression-polish roadmap step

## 1.0.56

Maintenance update.

Highlights:

- added `/jobsadmin balancejob <job>` to inspect one profession's live adoption, earnings, pending salary, and task activity in detail
- linked the new profession drill-down diagnostic into README, installation, commands, testing, and balance guidance
- synchronized release metadata for the balance-observability roadmap step

## 1.0.55

Maintenance update.

Highlights:

- added `/jobsadmin balancejobs` to inspect which professions dominate assigned slots, average levels, earnings, and pending salary across cached player profiles
- linked the new profession-level balance diagnostic into README, installation, commands, testing, and balance guidance
- synchronized release metadata for the balance-observability roadmap step

## 1.0.54

Maintenance update.

Highlights:

- added `/jobsadmin balanceoverview` to inspect average progression, earnings, pending salary, and task saturation across cached player profiles
- linked the new server-wide balance diagnostic into README, installation, commands, testing, and balance guidance
- synchronized release metadata for the balance-observability roadmap step

## 1.0.53

Maintenance update.

Highlights:

- added `/jobsadmin balancecheck <player>` to inspect progression stage, earnings, pending salary, skill points, and daily/contract saturation for a specific player
- linked the new balance diagnostic into README, installation, commands, testing, and balance guidance
- synchronized release metadata for the balance-observability roadmap step

## 1.0.52

Maintenance update.

Highlights:

- added a dedicated balance checklist for reviewing early, mid, and late progression plus economy pressure across server profiles
- linked balance review into testing, server profile usage, and release discipline docs
- synchronized release metadata for the balance-readiness roadmap step

## 1.0.51

Maintenance update.

Highlights:

- added `/jobsadmin perfcheck` as a compact runtime performance-readiness diagnostic for caches, debug/event flags, visual prep, and anti-abuse tracker activity
- documented the new performance diagnostic command in README, installation, commands, testing, and performance guidance
- synchronized release metadata for the performance-observability roadmap step

## 1.0.50

Maintenance update.

Highlights:

- added a dedicated performance checklist for practical server and service-client validation
- linked runtime performance checks into testing, compatibility, verification, and release workflow docs
- synchronized release metadata for the performance-readiness roadmap step

## 1.0.49

Maintenance update.

Highlights:

- added `/jobsadmin payoutcheck <player>` to inspect pending salary, cooldown, claim cap, tax preview, and payout readiness for a specific player
- documented the new player payout diagnostic in README, installation, commands, and testing guidance
- synchronized release metadata for the runtime admin-diagnostics roadmap step

## 1.0.48

Maintenance update.

Highlights:

- added `/jobsadmin economycheck` as a focused external-economy diagnostic for provider routing, `Z_Economy` bridge availability, currency setup, and tax sink validity
- documented the new economy diagnostic command in README, installation, commands, and testing guidance
- synchronized release metadata for the admin observability roadmap step

## 1.0.47

Maintenance update.

Highlights:

- added a unified release workflow document that connects versioning, build, staged verification, QA evidence, and publish rules
- linked the new workflow into the main release docs so the process is now easier to follow end-to-end
- synchronized release metadata for the release-discipline roadmap step

## 1.0.46

Maintenance update.

Highlights:

- added `/jobsadmin readycheck` as a compact pre-release runtime diagnostic for jobs data, economy routing, cache warmup, visual verification prep, and release-gate flags
- documented the new readiness command in README, installation, commands, and testing guidance
- synchronized release metadata for the admin observability roadmap step

## 1.0.45

Maintenance update.

Highlights:

- added a reusable release verification report template so each staged QA pass can record service-client, economy, and admin-diagnostics results consistently
- linked the report template into release, testing, installation, and verification docs to make QA handoff repeatable
- synchronized release metadata for the release-evidence roadmap step

## 1.0.44

Maintenance update.

Highlights:

- added a dedicated release verification flow for the service client, covering build, visual QA, economy routing, and final release-gate review
- linked the new verification document into installation, compatibility, release, and testing docs so the QA process is now repeatable
- synchronized release metadata for the service-client verification roadmap step

## 1.0.43

Maintenance update.

Highlights:

- added a dedicated compatibility guide covering baseline, economy, visual verification, and full verification stacks for `Minecraft 1.20.1`
- documented where `Z_Economy`, JEI, and JourneyMap fit into real deployment and QA flows
- synchronized release metadata for the compatibility-readiness roadmap step

## 1.0.42

Maintenance update.

Highlights:

- optimized `jobsadmin doctorfix` by removing unnecessary full online-player refresh after purely world-side NPC repairs
- doctorfix now only refreshes catalog data when NPC skin profiles were actually reset
- synchronized release metadata for the admin cache-discipline roadmap step

## 1.0.41

Maintenance update.

Highlights:

- optimized admin NPC presentation paths by replacing full online-player refresh with catalog-only sync for skin changes
- removed unnecessary full player-state sync after NPC label edits, keeping those updates server-side where only visible entity names changed
- synchronized release metadata for the admin-sync performance roadmap step

## 1.0.40

Maintenance update.

Highlights:

- optimized runtime sync orchestration by batching full player state delivery for join, mass sync, and screen-open paths
- reduced repeated per-player profile and leaderboard orchestration during full-state UI refresh flows without changing packet format
- synchronized release metadata for the runtime-sync performance roadmap step

## 1.0.39

Maintenance update.

Highlights:

- optimized overall leaderboard and leaderboard payload generation by caching per-profile aggregate totals for levels, XP, and earned salary
- reduced repeated full-profile summations on top views, admin summaries, and overall leaderboard sync paths
- synchronized release metadata for the second performance hot-path roadmap step

## 1.0.38

Maintenance update.

Highlights:

- optimized player payload generation by caching daily-task and contract template lookups per job instead of rescanning config lists for every payload entry
- reduced repeated stream/filter work during UI sync and payload building without changing gameplay behavior
- synchronized release metadata for the first performance hot-path roadmap step

## 1.0.37

Maintenance update.

Highlights:

- improved admin observability in `jobsadmin status` and `jobsadmin caches` with clearer advice for empty reward indexes and unprepared NPC skin caches
- diagnostics now point admins back toward config validation, reload, warmcaches, and local skin verification instead of leaving empty cache states unexplained
- synchronized release metadata for the observability roadmap step

## 1.0.36

Maintenance update.

Highlights:

- added a dedicated migrations and upgrades guide for live servers, covering config path migration, external currency updates, validation expectations, and safe restart flow
- documented the recommended update process for moving from legacy `config/advancedjobs/` to `config/ZAdvancedJobs/`
- synchronized release metadata for the upgrades-and-migrations roadmap step

## 1.0.35

Maintenance update.

Highlights:

- added curated server profile presets for casual, progression, and economy-heavy deployments
- documented how to apply `common.json` and `economy.json` profile templates without rebuilding the full config pack
- synchronized release metadata for the server-profiles production-readiness roadmap step

## 1.0.34

Maintenance update.

Highlights:

- improved player-facing anti-abuse explainability in Help, Info, and Stats so players now see why some kills, loot, or exploration actions may not count
- added clearer next-step guidance for reliable progression paths around natural mobs, fresh containers, and new chunks
- synchronized release metadata for the restriction-explainability UX roadmap step

## 1.0.33

Maintenance update.

Highlights:

- improved salary and contract explainability in GUI with clearer reason lines for manual claim cooldowns, auto-pay mode, reroll state, and next useful action
- extended `/jobs salary` and contract reroll feedback so players now get a direct next-step hint instead of only a success or failure line
- synchronized release metadata for the progression-explainability UX roadmap step

## 1.0.32

Maintenance update.

Highlights:

- improved empty-state guidance in Skills and Leaderboard so players now see a useful next step when a profession is not selected, no skill tree exists yet, or a ranking is still empty
- added clearer recovery guidance when the leaderboard has no professions loaded, pointing admins back toward config reload and generation flows
- synchronized release metadata for the extended GUI empty-state UX roadmap step

## 1.0.31

Maintenance update.

Highlights:

- improved empty-state UX in My Job, Salary, Daily, and Contracts with actionable next-step guidance
- reduced dead-end screens by telling players what to do when no profession is chosen or when a slot currently has no dailies/contracts
- synchronized release metadata for the empty-state UX roadmap step

## 1.0.30

Maintenance update.

Highlights:

- improved the in-game Help tab with a clearer first-run flow and a live next-step line based on the current or selected profession slot
- kept player guidance consistent between commands and GUI so onboarding no longer depends on using `/jobs help` alone
- synchronized release metadata for the help-tab onboarding UX roadmap step

## 1.0.29

Maintenance update.

Highlights:

- improved player onboarding in `/jobs help` and `/jobs info` with a clearer first-run flow and slot-aware next-step hints
- added first-player guidance to the README and command/testing docs so the same onboarding path exists in-game and in documentation
- synchronized release metadata for the player onboarding UX roadmap step

## 1.0.28

Maintenance update.

Highlights:

- added recovery guidance to `jobsadmin help` and conditional advice lines to `jobsadmin status`
- status output now points admins toward the next fix step when economy routing mismatches, caches are cold, reward events are still active, or anti-abuse runtime trackers look busy
- synchronized release metadata for the admin guidance UX roadmap step

## 1.0.27

Maintenance update.

Highlights:

- improved admin feedback for `reload`, `warmcaches`, `clearcaches`, and event commands so they now report the resulting runtime state instead of only confirming execution
- added a follow-up cache summary after warmup and richer event status output for multiplier stop/update flows
- synchronized release metadata for the admin feedback UX roadmap step

## 1.0.26

Maintenance update.

Highlights:

- added `/jobsadmin health` as a fast admin health-check for online state, economy routing, caches, and anti-abuse runtime
- expanded `/jobsadmin status` with clearer runtime snapshot fields, active economy provider reporting, and richer cache details
- synchronized command documentation and release metadata for the diagnostics/admin UX roadmap step

## 1.0.25

Maintenance update.

Highlights:

- extracted profile query helpers and title-unlock messaging from `JobManager` into `JobProfileViewService`
- moved assigned-job resolution, active/leaderboard job lookup, and title presentation behind a dedicated profile-view layer
- synchronized release metadata for the final profile-helper architecture cleanup step

## 1.0.24

Maintenance update.

Highlights:

- extracted online-player sync orchestration, screen opening flow, and reward-event expiration handling from `JobManager` into `JobRuntimeService`
- kept packet sync behavior intact while moving runtime coordination behind a focused service layer
- synchronized release metadata for the runtime architecture cleanup step

## 1.0.23

Maintenance update.

Highlights:

- extracted repository and economy lifecycle orchestration from `JobManager` into `JobPersistenceService`
- moved storage initialization, economy-provider selection, internal-balance restore/persist, and flush/save flow behind a dedicated persistence layer
- synchronized release metadata for the persistence-and-economy architecture cleanup step

## 1.0.22

Maintenance update.

Highlights:

- extracted catalog and leaderboard cache management from `JobManager` into `JobCacheService`
- extracted packet-based player, catalog, and leaderboard sync into `JobClientSyncService`
- synchronized release metadata for the cache-and-sync architecture cleanup step

## 1.0.21

Maintenance update.

Highlights:

- extracted action reward processing and effect-bonus resolution from `JobManager` into a dedicated `JobRewardService`
- moved reward calculation, salary credit routing, task progression hooks, and perk-effect aggregation behind a focused domain service
- synchronized release metadata for the reward-layer architecture cleanup step

## 1.0.20

Maintenance update.

Highlights:

- extracted job selection, leave/reset flows, and admin profile mutations from `JobManager` into a dedicated `JobProfileMutationService`
- kept the public job-management API intact while moving profile state changes out of the main coordinator
- synchronized release metadata for the profile-mutation cleanup step

## 1.0.19

Maintenance update.

Highlights:

- extracted catalog, player, and leaderboard JSON payload building from `JobManager` into a dedicated `JobPayloadService`
- kept cache usage and network packet flow intact while separating serialization from core job orchestration
- synchronized release metadata for the payload-layer architecture cleanup step

## 1.0.18

Maintenance update.

Highlights:

- extracted milestone and title progression checks from `JobManager` into a dedicated `JobProgressionService`
- moved profession milestone routing and milestone-to-title mapping out of the job manager to keep progression rules isolated
- synchronized release metadata for the next domain-service extraction in the job layer

## 1.0.17

Maintenance update.

Highlights:

- extracted daily-task and contract assignment/completion flow from `JobManager` into a dedicated `JobAssignmentService`
- moved contract reroll and assignment refresh logic into the same domain service to reduce `JobManager` branching
- synchronized release metadata for the next job-layer architecture cleanup step

## 1.0.16

Maintenance update.

Highlights:

- extracted salary claim and salary credit logic from `JobManager` into a dedicated `JobSalaryService`
- kept manual salary claims, instant salary payouts, tax routing, and cooldown behavior intact while reducing `JobManager` responsibility
- synchronized release metadata for the first domain-service extraction inside the job layer

## 1.0.15

Maintenance update.

Highlights:

- extracted reward bonus and passive perk logic from `JobEventHandler` into a dedicated `JobBonusHandler`
- reduced `JobEventHandler` to a much smaller event-routing role while keeping existing smelting, fishing, combat, and passive job bonuses intact
- synchronized release metadata for the next major event architecture cleanup step

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
