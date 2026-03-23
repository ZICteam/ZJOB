# Testing Checklist

Use this checklist before releases and after any change that touches gameplay, economy, GUI, configs, or NPC workflows.

## Environment

- build succeeds on Java `17`
- target version is Minecraft `1.20.1` with Forge `47.4.18`
- optional integrations are tested only when their jars are present
- service client uses a clean `mods/` setup for the scenario being checked
- compatibility guidance in `docs/COMPATIBILITY.md` still matches the current release and verification stacks
- release flow in `docs/VERIFICATION_FLOW.md` still matches the current service-client and server verification order
- release verification report template still matches the evidence we expect to capture before publishing
- performance checklist in `docs/PERFORMANCE_CHECKLIST.md` still matches the current hot paths and admin tooling
- balance checklist in `docs/BALANCE_CHECKLIST.md` still matches the current progression and economy review goals

## Build And Startup

- `./gradlew clean build` completes successfully
- output jar name matches the current mod version
- server starts without config generation errors
- generated config path is `config/ZAdvancedJobs/`
- legacy `config/advancedjobs/` data migrates safely when present
- curated server profile examples still match current `common.json` and `economy.json` keys
- migration notes in `docs/MIGRATIONS.md` still match the current release behavior

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
- `/jobs info` shows onboarding guidance when no profession is assigned
- `/jobs info` shows a meaningful next-step hint for an assigned profession slot
- `/jobs info`, `/jobs stats`, and the Help tab explain common anti-abuse restrictions for mobs, loot, and exploration
- My Job / Salary / Daily / Contracts / Skills / Leaderboard screens show actionable empty-state guidance
- Salary and Contracts screens explain current cooldown/auto-pay/reroll state with a useful next-step line
- `/jobsadmin health` reflects current runtime and economy routing accurately
- `/jobsadmin perfcheck` summarizes performance-readiness accurately before the practical perf pass
- `/jobsadmin economycheck` reflects external-provider readiness accurately
- `/jobsadmin readycheck` summarizes release-readiness accurately before the staged smoke-pass
- `/jobsadmin payoutcheck <player>` explains salary payout readiness accurately for a live player profile
- `/jobsadmin balancecheck <player>` explains progression and economy state accurately for a live player profile
- `/jobsadmin balanceoverview` explains the server-wide balance shape accurately for cached profiles
- `/jobsadmin balancejobs` explains profession dominance and average job health accurately for cached profiles
- `/jobsadmin balancejob <job>` explains live balance health for one chosen profession accurately
- `/jobsadmin balanceprogress <job>` explains live progression pressure for one chosen profession accurately
- `/jobs info` and `/jobs stats` explain the current progression focus clearly for skill points and milestone pacing
- My Job tooltips explain progression focus clearly on XP, skill points, and milestone hover states
- Skill Tree tooltip states explain the next unlock action clearly for available and locked nodes
- Daily and Contracts views explain the next recommended action clearly in both command output and GUI tooltips
- Help shows both the current next step and the correct service-board destination for the active slot
- My Job / Skills / Daily / Contracts empty and onboarding states point to the right service board for the current slot
- My Job / Daily / Contracts allow quick in-screen switching between primary and secondary assigned slots
- compact My Job / Skills / Daily / Contracts screens expose a working clickable flow link to the next relevant tab
- `/jobsadmin reload` refreshes config safely
- `/jobsadmin status` reflects current state accurately
- `/jobsadmin status` shows recovery advice when economy routing mismatches, caches are cold, or a reward event is still active
- `/jobsadmin status` and `/jobsadmin caches` explain empty reward-index and NPC skin cache states clearly
- admin progression commands mutate the right player/job data
- doctor, cache, and hub commands remain operational after config reload

## UI And Localization

- primary screens render without missing textures
- Russian and English localization keys resolve correctly
- titles, milestone text, and reward text remain readable
- leaderboard, salary, daily, contracts, and skills views still navigate correctly

## Performance Pass

- run the practical pass in [`docs/PERFORMANCE_CHECKLIST.md`](/Users/novaevent/Documents/CODEX/ZAdvancedJobs/docs/PERFORMANCE_CHECKLIST.md)
- verify `/jobs`, salary claim, contracts, hub interaction, reload, and warmcaches do not introduce obvious hitching
- verify the service client remains responsive while switching core screens

## Balance Pass

- run the practical pass in [`docs/BALANCE_CHECKLIST.md`](/Users/novaevent/Documents/CODEX/ZAdvancedJobs/docs/BALANCE_CHECKLIST.md)
- review early, mid, and late progression against at least one relevant server profile
- verify salary, dailies, contracts, reroll friction, and skill pacing still feel intentional

## Release Gate

- changelog entry exists for the new version
- mod version is synchronized across build and docs
- README references the current jar version
- installation and configuration docs still match current behavior
- verification-flow documentation still matches the current release process
- release verification report can be filled without missing fields
- performance checklist still matches the release verification expectations
- balance checklist still matches the release and tuning expectations
