# Roadmap

This roadmap is the working plan for `Advanced Jobs RPG`.

It is organized around four major phases:

1. Stabilize the project foundation.
2. Polish the player and admin experience.
3. Harden the mod for production server use.
4. Grow the mod as a long-term platform.

## Phase 1: Stabilize The Foundation

Goal: make the project safer to extend without increasing regression risk.

### Architecture

- split oversized classes by responsibility, especially event, GUI, and command-heavy code
- move reward, salary, contract, daily-task, and anti-abuse calculations into focused services
- reduce duplicated formatting, registry lookup, and config parsing logic

### Domain Boundaries

- introduce clearer service boundaries for reward flow, contracts, daily tasks, salary, job selection, and NPC hub behavior
- keep pure game rules separate from Forge event wiring
- prefer explicit response models for GUI, commands, and status output

### Config Reliability

- document expected structure for generated JSON configs
- add better validation and more actionable log messages around broken config data
- define fallback behavior for invalid records instead of relying on raw exceptions

### Regression Discipline

- keep a living regression checklist for core player and admin workflows
- validate job selection, salary, contracts, skills, NPC interaction, config migration, and economy integration on every release
- expand automated tests where logic is cleanly separable from Forge runtime hooks

### Technical Hygiene

- keep the build warning-free
- sync mod version, changelog, and docs on every shipped change
- standardize package structure and internal helper usage

## Phase 2: Polish The Experience

Goal: improve clarity, onboarding, and motivation for players and admins.

### First-Session UX

- improve first-use guidance for job selection and initial progression
- show the next useful action inside GUI and NPC flows
- reduce confusion around why actions succeed, fail, or are delayed

### Progress Clarity

- explain why XP, salary, cooldowns, and restrictions apply
- surface anti-abuse decisions in a player-friendly way where appropriate
- improve visibility of available tasks, ready rewards, and next unlocks

### GUI Polish

- simplify overloaded screens
- strengthen visual hierarchy around current state, next reward, and next unlock
- reduce navigation friction between jobs, daily tasks, contracts, skills, and leaderboard

### Balance

- review early, mid, and late progression pacing
- compare salary, contracts, tasks, and perks across jobs
- watch for inflation and job-role imbalance on server economies

### Admin UX

- improve diagnostics, health summaries, and maintenance flows
- make config and economy state easier to inspect
- improve clarity around hub status, reload results, and exploit protections

## Phase 3: Production Server Readiness

Goal: make the mod easier to deploy, support, and update on real servers.

### Server Profiles

- provide curated config presets for different server styles
- document recommended setups for economy-heavy, casual, and progression-focused servers

### Integrations

- strengthen `Z_Economy` operational guidance and diagnostics
- keep optional integrations easy to compile and verify
- continue compatibility checks with common `1.20.1` server stacks

### Upgrades And Migrations

- document version-to-version migration expectations
- make config path and schema changes safer to adopt
- call out breaking changes and update steps clearly

### Performance

- profile high-frequency event paths
- reduce repeated registry and data lookups in hot loops
- review leaderboard, persistence, and anti-exploit caching behavior under load

### Observability

- improve debug and doctor tooling
- add better status summaries for economy, jobs, and hub state
- track common failure modes in a way admins can act on quickly

## Phase 4: Platform Growth

Goal: expand the mod carefully after the foundation is stable.

### Content Growth

- add new professions only after balance and UX are stable
- introduce seasonal or rare contracts
- expand milestones, titles, and long-term goals

### System Depth

- explore profession reputation, specializations, and prestige loops
- add regional or biome-sensitive progression where it improves gameplay
- expand elite or event-driven career content

### Ecosystem

- strengthen public extension points for future integrations
- support cleaner external job packs or server-specific content packs

## Immediate Priority Order

1. Finish foundation stabilization work.
2. Improve onboarding and first-hour UX.
3. Keep the visual test client and regression process up to date.
4. Revisit balance after stability and UX improve.
5. Expand content only after the earlier phases are healthy.

## Active Start Point

The first roadmap work started in `1.0.9`:

- roadmap document added
- regression checklist added
- release checklist added

Next practical target:

- formalize config validation and fallback behavior for malformed JSON entries
