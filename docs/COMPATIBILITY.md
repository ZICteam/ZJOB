# Compatibility

This document describes the intended compatibility surface for `Advanced Jobs RPG` on `Minecraft 1.20.1`.

## Target Runtime

- Minecraft `1.20.1`
- Forge `47.4.18+`
- Java `17`

## Compatibility Tiers

### Core Server Stack

Expected to work as the baseline:

- Forge `47.4.18+`
- `Advanced Jobs RPG`
- no optional client helpers
- internal economy provider

Use this stack when:

- validating raw jobs logic
- checking config generation
- testing migration behavior
- isolating gameplay bugs without extra integrations

### Economy Server Stack

Recommended for real server deployment with money routing:

- Forge `47.4.18+`
- `Advanced Jobs RPG`
- `Z_Economy`

Use this stack when:

- validating salary payout
- checking tax sink routing
- confirming job change cost and contract reroll cost
- testing economy diagnostics in `/jobsadmin health` and `/jobsadmin status`

### Visual Verification Client Stack

Recommended for UI and content checks:

- Forge `47.4.18+`
- `Advanced Jobs RPG`
- JEI
- JourneyMap

Use this stack when:

- checking GUI visuals
- reviewing NPC presentation and labels
- validating board interactions and navigation flow
- confirming optional client integrations still compile and behave normally

### Full Verification Stack

Recommended before public release:

- Forge `47.4.18+`
- `Advanced Jobs RPG`
- `Z_Economy`
- JEI
- JourneyMap

Use this stack when:

- validating release candidates
- checking cross-feature behavior
- confirming no regressions in economy, navigation, and UI together

## Optional Integration Notes

### Z_Economy

- runtime integration only
- required only if you want external economy routing
- if missing, the mod falls back to internal economy mode

See:

- [`docs/INTEGRATION_ZECONOMY.md`](/Users/novaevent/Documents/CODEX/ZAdvancedJobs/docs/INTEGRATION_ZECONOMY.md)

### JEI

- optional
- intended mainly for client-side visual or convenience use
- absence of JEI must not break the core jobs gameplay loop

### JourneyMap

- optional
- useful for visual verification and navigation-heavy QA sessions
- absence of JourneyMap must not break the core jobs gameplay loop

## Recommended Verification Matrix

Before release, run at least these checks:

1. Core server stack:
   - config generation
   - `/jobs`
   - `/jobs info`
   - `/jobs salary`
   - daily/contracts/skills

2. Economy server stack:
   - external provider routing
   - salary payout
   - tax sink behavior
   - reroll and job-change withdrawal

3. Visual verification client stack:
   - screen open flow
   - My Job / Salary / Daily / Contracts / Skills / Top
   - NPC desk interactions
   - label and skin presentation

4. Full verification stack:
   - one end-to-end regression pass using `docs/TESTING_CHECKLIST.md`
   - one practical runtime pass using `docs/PERFORMANCE_CHECKLIST.md`

## Service Client

Keep the dedicated visual verification client aligned with:

- Minecraft `1.20.1`
- Forge `47.4.18+`
- the current `Advanced Jobs` release jar

Recommended use:

- test GUI and NPC visuals there first
- only then move to server-side economy and migration verification

For the exact release-pass order, use:

- [`docs/VERIFICATION_FLOW.md`](/Users/novaevent/Documents/CODEX/ZAdvancedJobs/docs/VERIFICATION_FLOW.md)

## Non-Goals

This document does not promise compatibility with:

- other Minecraft versions
- Fabric or NeoForge
- arbitrary economy APIs besides internal mode and `Z_Economy`
- client helper mods that are not already part of the documented verification flow
