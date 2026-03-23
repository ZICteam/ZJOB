# Migrations And Upgrades

This document describes the safe update path for `Advanced Jobs RPG` on live servers.

## Recommended Upgrade Flow

1. Stop the server fully.
2. Back up:
   - `mods/advancedjobs-*.jar`
   - `config/ZAdvancedJobs/`
   - old `config/advancedjobs/` if it still exists
   - player data and world save
3. Replace the mod jar with the new release.
4. Start the server once and watch logs for config migration or validation messages.
5. Stop the server again if new config files were generated.
6. Compare your custom config values against the latest generated structure.
7. Run through `docs/TESTING_CHECKLIST.md` before reopening the server to players.

## Config Directory Migration

Current config root:

```text
config/ZAdvancedJobs/
```

Legacy config root:

```text
config/advancedjobs/
```

Current behavior:

- the mod migrates legacy config files from `config/advancedjobs/` into `config/ZAdvancedJobs/` when possible
- if migration fails, the server log reports the failure and the mod falls back to the current config root

Recommended admin action:

- do not keep editing both folders
- after successful migration, keep only `config/ZAdvancedJobs/` as your active config source

## Economy Migration Notes

The external provider config now expects:

```json
{
  "provider": "external",
  "externalCurrency": "z_coin"
}
```

If your old config still uses:

```json
"externalCurrency": "sdm_coin"
```

update it manually to:

```json
"externalCurrency": "z_coin"
```

Recommended verification after update:

1. Start the server.
2. Confirm the economy status line reports:
   - `configuredProvider=external`
   - `activeProvider=external`
   - `bridgeAvailable=true`
3. Claim salary in game.
4. Confirm the `Z_Economy` balance actually changes.

## Config Schema Safety

The mod now validates config structure and business rules on load.

This means updates may now report:

- malformed JSON paths
- duplicate ids
- broken references between jobs, daily pools, contracts, or skill trees
- invalid negative or impossible values

When this happens:

1. read the exact failing field path in the log
2. fix the config manually
3. restart the server

Do not ignore these errors on production servers, because fallback behavior may hide broken balance data behind safe defaults.

## Before Reopening A Live Server

Check these high-risk areas first:

- `/jobs`
- `/jobs info`
- `/jobs salary`
- `/jobs contracts`
- `/jobs skills`
- `/jobsadmin health`
- `/jobsadmin status`
- external economy payout flow if you use `Z_Economy`

## Breaking-Change Policy

Current policy for this project:

- document path changes explicitly
- document manual config edits when automatic migration is not enough
- call out currency, provider, or schema changes in `CHANGELOG.md`
- keep new migration notes in this file instead of scattering them across release entries only
