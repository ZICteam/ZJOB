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

## Validation

- run the regression checklist in `docs/TESTING_CHECKLIST.md`
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
