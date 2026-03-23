# Advanced Jobs RPG

`Advanced Jobs RPG` is a Forge `1.20.1` professions mod focused on long-term player progression:

- primary and secondary job slots
- salary, tax, and economy routing
- skill trees and perk unlocks
- daily tasks and rotating contracts
- service-desk NPC hub
- leaderboard, milestones, and titles
- compact vanilla-style GUI
- direct support for `Z_Economy` through its public API

## Requirements

- Minecraft `1.20.1`
- Forge `47.4.18+`
- Java `17`

## Features

- configurable jobs, perks, tasks, and contracts through JSON
- player onboarding through `/jobs`, guide commands, and service NPCs
- actionable empty-state guidance in My Job, Salary, Daily, Contracts, Skills, and Leaderboard
- clearer salary and contract feedback with player-facing reasons and next-step guidance
- clearer anti-abuse and restriction guidance in Help, Info, and Stats
- curated server profile presets for casual, progression, and economy-heavy deployments
- documented upgrade and migration flow for live servers
- stronger admin observability for reward-index and NPC-skin cache issues
- lighter player payload generation for UI sync paths
- lighter overall leaderboard aggregation for top and sync paths
- lighter full-state runtime sync on join, reload, and screen-open flows
- lighter admin NPC presentation updates for skins and labels
- lighter admin doctorfix refresh behavior
- documented compatibility stacks for core, economy, visual, and full verification setups
- documented a dedicated release verification flow using the service client
- documented a reusable release verification report template for QA handoff
- added `/jobsadmin readycheck` for compact pre-release runtime diagnostics
- documented a unified release workflow from version bump to verified push
- added `/jobsadmin economycheck` for focused external economy diagnostics
- added `/jobsadmin payoutcheck <player>` for per-player salary payout diagnostics
- documented a practical performance checklist for server and service-client validation
- added `/jobsadmin perfcheck` for compact performance-readiness diagnostics
- documented a dedicated balance checklist for progression and economy pacing review
- added `/jobsadmin balancecheck <player>` for per-player progression and economy review
- added `/jobsadmin balanceoverview` for server-wide balance snapshots
- added `/jobsadmin balancejobs` for profession-level balance snapshots
- added `/jobsadmin balancejob <job>` for one-profession balance drill-down
- added `/jobsadmin balanceprogress <job>` for profession-level progression diagnostics
- improved player-facing progression guidance in `/jobs info`, `/jobs stats`, and My Job tooltips
- improved Skill Tree onboarding with clearer locked-node next actions and stronger no-tree guidance
- improved Daily and Contracts guidance in commands and GUI tooltips with clearer next actions
- improved Help-screen service-board navigation with a clearer "where next" line for each slot
- improved in-screen board routing for My Job, Skills, Daily, and Contracts onboarding states
- added quick in-screen slot switching for My Job, Daily, and Contracts when both assigned slots are present
- added clickable compact-screen cross-links between the main profession workflow tabs
- added compact contextual summaries to Salary, Skills, Daily, and Contracts workflow screens
- added starter-focus guidance to Jobs and My Job for clearer first-hour profession onboarding
- added clearer first-hour route messaging to Jobs and Help for smoother profession onboarding
- extended first-hour route guidance into commands and service-desk NPC hints
- extended first-hour route guidance into guide, navigate, and where summary commands
- internal economy fallback
- external economy mode through `Z_Economy`
- admin tools for hub deployment, repair, diagnostics, cache control, and anti-abuse checks
- Russian and English localization

## Screenshots

### Skills
![Skills](docs/images/skills.png)

### My Job
![My Job](docs/images/myjob.png)

### Daily
![Daily](docs/images/daily.png)

### Contracts
![Contracts](docs/images/contracts.png)

### Salary
![Salary](docs/images/salary.png)

### Leaderboard
![Top](docs/images/top.png)

## Quick Start

1. Build the mod or take `build/libs/advancedjobs-1.0.76.jar`.
2. Put the jar into the server `mods` directory.
3. Start the server once so configs are generated.
4. Review `config/ZAdvancedJobs/`.
5. Restart the server after changing config files.

First player flow:

1. Run `/jobs`.
2. Pick a primary profession.
3. Run `/jobs info` to see salary, resets, and your next unlock.
4. Use `/jobs guide` or `/jobs where ready` to find the next useful board nearby.

Server profile presets:

- use [`docs/SERVER_PROFILES.md`](/Users/novaevent/Documents/CODEX/ZAdvancedJobs/docs/SERVER_PROFILES.md) for ready-to-copy `common.json` and `economy.json` starting points
- available profiles:
  - `casual`
  - `progression`
  - `economy_heavy`

Upgrades:

- use [`docs/MIGRATIONS.md`](/Users/novaevent/Documents/CODEX/ZAdvancedJobs/docs/MIGRATIONS.md) before updating a live server or migrating old config folders

Compatibility:

- use [`docs/COMPATIBILITY.md`](/Users/novaevent/Documents/CODEX/ZAdvancedJobs/docs/COMPATIBILITY.md) for recommended server/client verification stacks
- use [`docs/VERIFICATION_FLOW.md`](/Users/novaevent/Documents/CODEX/ZAdvancedJobs/docs/VERIFICATION_FLOW.md) for the release smoke-pass order with the service client
- use [`docs/RELEASE_VERIFICATION_REPORT.md`](/Users/novaevent/Documents/CODEX/ZAdvancedJobs/docs/RELEASE_VERIFICATION_REPORT.md) as the handoff template after verification is complete

External economy setup:

1. Install `Z_Economy` on the same server.
2. Set `"provider": "external"` in `config/ZAdvancedJobs/economy.json`.
3. Set `"externalCurrency": "z_coin"`.

Minimal `economy.json` example:

```json
{
  "provider": "external",
  "externalCurrency": "z_coin",
  "taxSinkAccountUuid": "00000000-0000-0000-0000-000000000001"
}
```

## Build

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat build
```

Optional client integrations:

- JEI support is compiled automatically if a nearby jar is identified as JEI through Forge mod metadata, with filename prefix matching kept as fallback
- JourneyMap support is compiled automatically if a nearby jar is identified as JourneyMap through Forge mod metadata, with filename prefix matching kept as fallback
- if these jars are absent, the core mod still builds without the optional integration classes

Output:

```text
build/libs/advancedjobs-1.0.76.jar
```

## Generated Config Files

After first start the mod generates:

- `config/ZAdvancedJobs/common.json`
- `config/ZAdvancedJobs/jobs.json`
- `config/ZAdvancedJobs/perks.json`
- `config/ZAdvancedJobs/daily_tasks.json`
- `config/ZAdvancedJobs/contracts.json`
- `config/ZAdvancedJobs/economy.json`
- `config/ZAdvancedJobs/client.json`
- `config/ZAdvancedJobs/npc_skins.json`
- `config/ZAdvancedJobs/npc_labels.json`

## Main Commands

Player:

- `/jobs`
- `/jobs info`
- `/jobs help`
- `/jobs salary`
- `/jobs daily`
- `/jobs contracts`
- `/jobs skills`
- `/jobs top`
- `/jobs where ...`
- `/jobs guide ...`
- `/jobs navigate ...`

Admin:

- `/jobsadmin help`
- `/jobsadmin health`
- `/jobsadmin perfcheck`
- `/jobsadmin economycheck`
- `/jobsadmin readycheck`
- `/jobsadmin payoutcheck <player>`
- `/jobsadmin routecheck <player>`
- `/jobsadmin balancecheck <player>`
- `/jobsadmin balanceoverview`
- `/jobsadmin balancejobs`
- `/jobsadmin balancejob <job>`
- `/jobsadmin balanceprogress <job>`
- `/jobsadmin reload`
- `/jobsadmin status`
- `/jobsadmin spawnhub`
- `/jobsadmin repairhub`
- `/jobsadmin doctor`
- `/jobsadmin doctorfix`
- `/jobsadmin normalizehub`
- `/jobsadmin alignhub`
- `/jobsadmin hardenhub`
- `/jobsadmin unlockskill <player> <job> <node>`

Full reference:

- [docs/COMMANDS.md](docs/COMMANDS.md)

## Documentation

- [docs/INSTALLATION.md](docs/INSTALLATION.md)
- [docs/CONFIGURATION.md](docs/CONFIGURATION.md)
- [docs/COMMANDS.md](docs/COMMANDS.md)
- [docs/INTEGRATION_ZECONOMY.md](docs/INTEGRATION_ZECONOMY.md)
- [docs/UI.md](docs/UI.md)
- [docs/ROADMAP.md](docs/ROADMAP.md)
- [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md)
- [docs/MIGRATIONS.md](docs/MIGRATIONS.md)
- [docs/SERVER_PROFILES.md](docs/SERVER_PROFILES.md)
- [docs/VERIFICATION_FLOW.md](docs/VERIFICATION_FLOW.md)
- [docs/RELEASE_VERIFICATION_REPORT.md](docs/RELEASE_VERIFICATION_REPORT.md)
- [docs/RELEASE_WORKFLOW.md](docs/RELEASE_WORKFLOW.md)
- [docs/PERFORMANCE_CHECKLIST.md](docs/PERFORMANCE_CHECKLIST.md)
- [docs/BALANCE_CHECKLIST.md](docs/BALANCE_CHECKLIST.md)
- [docs/TESTING_CHECKLIST.md](docs/TESTING_CHECKLIST.md)
- [docs/RELEASE_CHECKLIST.md](docs/RELEASE_CHECKLIST.md)
- [UI_STYLE_NOTES.md](UI_STYLE_NOTES.md)
- [VANILLA_UI_TEXTURES.md](VANILLA_UI_TEXTURES.md)
- [CHANGELOG.md](CHANGELOG.md)
- [CONTRIBUTING.md](CONTRIBUTING.md)
- [LICENSE](LICENSE)

## Z_Economy Integration

`Advanced Jobs` uses the public `Z_Economy` API in external economy mode.

- bridge: `src/main/java/com/example/advancedjobs/economy/ExternalEconomyBridge.java`
- default external currency: `z_coin`

More details:

- [docs/INTEGRATION_ZECONOMY.md](docs/INTEGRATION_ZECONOMY.md)
