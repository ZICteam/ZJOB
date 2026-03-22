# Testing Checklist

Use this checklist before releases and after any change that touches gameplay, economy, GUI, configs, or NPC workflows.

## Environment

- build succeeds on Java `17`
- target version is Minecraft `1.20.1` with Forge `47.4.18`
- optional integrations are tested only when their jars are present
- service client uses a clean `mods/` setup for the scenario being checked

## Build And Startup

- `./gradlew clean build` completes successfully
- output jar name matches the current mod version
- server starts without config generation errors
- generated config path is `config/ZAdvancedJobs/`
- legacy `config/advancedjobs/` data migrates safely when present

## Core Player Flow

- `/jobs` opens the main UI
- player can choose a primary job
- player can leave a primary job
- secondary job flow works when enabled
- `/jobs info` reflects current progression and economy state

## Salary

- `/jobs salary` opens salary flow correctly
- pending salary increases after expected gameplay actions
- salary claim succeeds
- salary cooldown and instant-salary settings behave correctly
- tax routing behaves correctly in internal and external economy modes

## Daily Tasks

- `/jobs daily` opens daily tasks
- progress increases on valid actions
- invalid actions do not count
- daily reset behavior matches configured reset time
- bonus rewards and XP apply correctly

## Contracts

- `/jobs contracts` opens contracts
- progress updates on valid actions
- reroll cost and cooldown match config
- contract rewards apply correctly
- expired or completed contracts behave correctly

## Skills And Progression

- `/jobs skills` opens the skill UI
- XP gains level the correct job
- skill points are awarded correctly
- unlock requirements are enforced
- unlocked perks apply their intended effects

## NPC And Hub Workflow

- service NPCs open the correct screens
- `/jobs where`, `/jobs guide`, and `/jobs navigate` behave correctly
- `/jobsadmin spawnhub` creates a valid hub layout
- `/jobsadmin repairhub` and `/jobsadmin doctorfix` repair expected issues
- NPC labels and skin settings load correctly from config

## Economy Integration

- internal economy mode works with no external provider
- external mode activates correctly with `Z_Economy`
- provider status is visible in admin diagnostics
- failed external transactions degrade safely and log actionable information

## Anti-Abuse

- repeated low-value farming is blocked when expected
- tamed, artificial, or invalid mob rewards are rejected according to config
- loot and exploration cooldowns work
- blocked actions do not grant salary or XP

## Commands And Admin Tools

- `/jobs help` and `/jobsadmin help` list valid command flows
- `/jobsadmin reload` refreshes config safely
- `/jobsadmin status` reflects current state accurately
- admin progression commands mutate the right player/job data
- doctor, cache, and hub commands remain operational after config reload

## UI And Localization

- primary screens render without missing textures
- Russian and English localization keys resolve correctly
- titles, milestone text, and reward text remain readable
- leaderboard, salary, daily, contracts, and skills views still navigate correctly

## Release Gate

- changelog entry exists for the new version
- mod version is synchronized across build and docs
- README references the current jar version
- installation and configuration docs still match current behavior
