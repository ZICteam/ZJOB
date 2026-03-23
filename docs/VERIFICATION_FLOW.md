# Verification Flow

This document describes the recommended release-verification flow for `Advanced Jobs RPG` using the dedicated service client.

## Goal

Use one repeatable pass before every public release so visual issues, config regressions, and economy-routing problems are caught in the same order every time.

## Required Assets

- built release jar from `build/libs/advancedjobs-1.0.50.jar`
- dedicated service client for `Minecraft 1.20.1` with Forge `47.4.18+`
- optional visual helpers when needed:
  - JEI
  - JourneyMap
- `Z_Economy` jar for economy-routing verification

## Recommended Order

Run verification in four stages:

1. build and config generation
2. visual verification client pass
3. economy server pass
4. final release gate review

Do not start with the full stack first. It is easier to isolate regressions when the simpler environment passes before the integrated one.

## Stage 1: Build And Config Generation

1. Run `./gradlew clean build`.
2. Confirm the output jar is `build/libs/advancedjobs-1.0.50.jar`.
3. Start a clean Forge `1.20.1` server with only `Advanced Jobs RPG`.
4. Confirm the mod generates files in `config/ZAdvancedJobs/`.
5. Review logs for config-validation errors, migration warnings, or unexpected fallback messages.

Expected result:

- server boots successfully
- no missing-config crash
- generated config path is correct

## Stage 2: Visual Verification Client Pass

Use the dedicated service client for this stage.

Recommended stack:

- `Advanced Jobs RPG`
- JEI
- JourneyMap

Checklist:

1. Join the test server with the service client.
2. Open `/jobs`.
3. Check `My Job`, `Salary`, `Daily`, `Contracts`, `Skills`, and `Leaderboard`.
4. Verify empty-state guidance appears when expected.
5. Verify onboarding and Help messaging for a fresh player.
6. Interact with service-desk NPCs and boards.
7. Confirm labels, skins, and navigation prompts look correct.
8. Run the client-side section from [`docs/PERFORMANCE_CHECKLIST.md`](/Users/novaevent/Documents/CODEX/ZAdvancedJobs/docs/PERFORMANCE_CHECKLIST.md).

Expected result:

- no missing textures or untranslated keys
- no dead-end screens without guidance
- NPC hub interactions open the correct screens

## Stage 3: Economy Server Pass

Recommended stack:

- `Advanced Jobs RPG`
- `Z_Economy`

Checklist:

1. Set `"provider": "external"` in `config/ZAdvancedJobs/economy.json`.
2. Confirm `"externalCurrency": "z_coin"` is configured.
3. Restart the server.
4. Run `/jobsadmin health` and `/jobsadmin status`.
5. Verify `configuredProvider=external` and `activeProvider=external`.
6. Earn salary through valid gameplay actions.
7. Claim salary and confirm currency changes in `Z_Economy`.
8. Test job-change and contract-reroll payments.
9. Run `/jobsadmin economycheck` and `/jobsadmin payoutcheck <player>` during the pass.

Expected result:

- external routing is active
- failed transactions degrade safely
- admin diagnostics explain provider state clearly

## Stage 4: Final Release Gate

Before publishing:

1. Run `/jobsadmin readycheck`.
2. Run the full checklist in [`docs/TESTING_CHECKLIST.md`](/Users/novaevent/Documents/CODEX/ZAdvancedJobs/docs/TESTING_CHECKLIST.md).
3. Review compatibility targets in [`docs/COMPATIBILITY.md`](/Users/novaevent/Documents/CODEX/ZAdvancedJobs/docs/COMPATIBILITY.md).
4. Review upgrade notes in [`docs/MIGRATIONS.md`](/Users/novaevent/Documents/CODEX/ZAdvancedJobs/docs/MIGRATIONS.md).
5. Fill out [`docs/RELEASE_VERIFICATION_REPORT.md`](/Users/novaevent/Documents/CODEX/ZAdvancedJobs/docs/RELEASE_VERIFICATION_REPORT.md).
6. Run the relevant checks in [`docs/PERFORMANCE_CHECKLIST.md`](/Users/novaevent/Documents/CODEX/ZAdvancedJobs/docs/PERFORMANCE_CHECKLIST.md).
7. Confirm version, changelog, and jar references all match the release.
8. Push only after the build and verification flow are clean.

## Service Client Rules

Keep the service client aligned with:

- Minecraft `1.20.1`
- Forge `47.4.18+`
- the current release jar

Use it for:

- GUI checks
- NPC and hub presentation checks
- visual confirmation after admin hub repairs
- final smoke-pass before publishing a release

Do not treat it as the only validation environment. Economy and migration checks still belong on a server stack.

For the full release process around this staged verification pass, use:

- [`docs/RELEASE_WORKFLOW.md`](/Users/novaevent/Documents/CODEX/ZAdvancedJobs/docs/RELEASE_WORKFLOW.md)
