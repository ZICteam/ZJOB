# Release Workflow

This document ties together the release process for `Advanced Jobs RPG` from version bump to final push.

Use it when preparing any public jar, not only large feature releases.

## Goal

Keep every release reproducible:

- version is synchronized
- build artifact is verified
- service-client smoke-pass is repeatable
- QA evidence is captured
- push happens only after the release gate is clean

## Workflow

### 1. Prepare The Release

1. Update the mod version in `gradle.properties`.
2. Update `CHANGELOG.md`.
3. Update `README.md` and any affected docs.
4. Confirm jar references point to the new version.

Primary references:

- [`docs/RELEASE_CHECKLIST.md`](/Users/novaevent/Documents/CODEX/ZAdvancedJobs/docs/RELEASE_CHECKLIST.md)
- [`docs/MIGRATIONS.md`](/Users/novaevent/Documents/CODEX/ZAdvancedJobs/docs/MIGRATIONS.md)

### 2. Build The Release

1. Run `./gradlew clean build`.
2. Confirm the jar exists in `build/libs/`.
3. Review build output for warnings or unexpected noise.

Primary reference:

- [`docs/INSTALLATION.md`](/Users/novaevent/Documents/CODEX/ZAdvancedJobs/docs/INSTALLATION.md)

### 3. Run The Staged Verification Flow

1. Follow the order in [`docs/VERIFICATION_FLOW.md`](/Users/novaevent/Documents/CODEX/ZAdvancedJobs/docs/VERIFICATION_FLOW.md).
2. Use the dedicated service client for the visual pass.
3. Use `Z_Economy` for the economy-routing pass when external mode is relevant.
4. Run `/jobsadmin readycheck` before final sign-off.

Primary references:

- [`docs/VERIFICATION_FLOW.md`](/Users/novaevent/Documents/CODEX/ZAdvancedJobs/docs/VERIFICATION_FLOW.md)
- [`docs/COMPATIBILITY.md`](/Users/novaevent/Documents/CODEX/ZAdvancedJobs/docs/COMPATIBILITY.md)
- [`docs/TESTING_CHECKLIST.md`](/Users/novaevent/Documents/CODEX/ZAdvancedJobs/docs/TESTING_CHECKLIST.md)

### 4. Capture QA Evidence

1. Fill out the report template in [`docs/RELEASE_VERIFICATION_REPORT.md`](/Users/novaevent/Documents/CODEX/ZAdvancedJobs/docs/RELEASE_VERIFICATION_REPORT.md).
2. Record any accepted risks instead of leaving them implicit.
3. If release is blocked, mark it as `hold`.

### 5. Publish

1. Commit with a clear release message.
2. Push only after the release gate is clean.
3. Verify the remote push completed successfully.

## Release Gate

Do not publish if any of these are still unresolved:

- build failed
- version references are inconsistent
- `readycheck` reports critical warnings
- migration notes are missing for a breaking config change
- release verification report is incomplete

## Recommended Rhythm

For normal releases, use this compact order:

1. update version and docs
2. build
3. verify
4. record QA evidence
5. commit and push

This keeps release discipline lightweight while still leaving a reliable audit trail.
