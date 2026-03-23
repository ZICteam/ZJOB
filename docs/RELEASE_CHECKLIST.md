# Release Checklist

Use this checklist every time the mod version changes.

## Versioning

- update `mod_version` in `gradle.properties`
- verify generated jar name matches the target release version
- confirm docs no longer reference the previous jar version

## Documentation

- add a new `CHANGELOG.md` entry
- update `README.md` if behavior, setup, version, or outputs changed
- update installation, configuration, and integration docs if affected
- add new docs links to `README.md` when new documentation files are introduced
- verify new server profile docs or preset templates stay aligned with the current config keys
- update `docs/MIGRATIONS.md` when a release changes config paths, schema expectations, provider behavior, or manual upgrade steps
- update `docs/COMPATIBILITY.md` when supported stacks, optional integrations, or verification expectations change
- update `docs/VERIFICATION_FLOW.md` when the release smoke-pass order, service client role, or verification stack changes
- update `docs/RELEASE_VERIFICATION_REPORT.md` when the expected QA handoff fields or release evidence change
- update `docs/RELEASE_WORKFLOW.md` when the overall release order or publishing gate changes
- update `docs/PERFORMANCE_CHECKLIST.md` when the practical perf-pass expectations or hot paths change
- update `docs/BALANCE_CHECKLIST.md` when progression, economy, or tuning review expectations change

## Validation

- run the regression checklist in `docs/TESTING_CHECKLIST.md`
- follow the staged release pass in `docs/VERIFICATION_FLOW.md`
- run the practical checks in `docs/PERFORMANCE_CHECKLIST.md`
- run the relevant checks in `docs/BALANCE_CHECKLIST.md` when progression or economy behavior changed
- run `/jobsadmin readycheck` before final release sign-off
- fill out the template in `docs/RELEASE_VERIFICATION_REPORT.md`
- run `./gradlew clean build`
- confirm build output path and artifact name
- review logs for warnings or newly introduced noise

## Distribution

- commit the release changes with a clear message
- push the branch or `main` only after build verification passes
- verify remote push completed successfully

## Post-Release

- keep the service client ready for visual verification
- carry forward any follow-up debt into the roadmap instead of leaving it implicit
