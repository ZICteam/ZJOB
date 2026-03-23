# Installation

## Requirements

- Minecraft `1.20.1`
- Forge `47.4.18+`
- Java `17`

## Server Installation

1. Build the project or take the ready jar from `build/libs/advancedjobs-1.0.64.jar`.
2. Put the jar into the server `mods` folder.
3. Start the server once.
4. Stop the server.
5. Review generated files in `config/ZAdvancedJobs/`.
6. Start the server again after editing config files.

## First Boot Output

On first launch the mod creates:

- `common.json`
- `jobs.json`
- `perks.json`
- `daily_tasks.json`
- `contracts.json`
- `economy.json`
- `client.json`
- `npc_skins.json`
- `npc_labels.json`

All of them are written under:

```text
config/ZAdvancedJobs/
```

## Optional Client Mods

The core mod works without client-side companions, but these are supported:

- JEI
- JourneyMap

They are optional and not required for jobs, salary, skills, or NPC hub features.

## External Economy Installation

To route salary and payments through `Z_Economy`:

1. Install `Z_Economy` on the same server.
2. Open `config/ZAdvancedJobs/economy.json`.
3. Set:
   - `"provider": "external"`
   - `"externalCurrency": "z_coin"`
4. Restart the server.

Example:

```json
{
  "provider": "external",
  "externalCurrency": "z_coin",
  "taxSinkAccountUuid": "00000000-0000-0000-0000-000000000001"
}
```

## Verifying External Economy

Check the server log after start. The jobs mod writes an economy status line.

Expected state:

- `configuredProvider=external`
- `activeProvider=external`
- `bridgeAvailable=true`

Then claim salary in game and verify:

- `/jobs salary`
- `Z_Economy` balance command

## Build From Source

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat build
```

Artifact:

```text
build/libs/advancedjobs-1.0.60.jar
```

Optional starting presets for `common.json` and `economy.json` are documented in:

- [`docs/SERVER_PROFILES.md`](/Users/novaevent/Documents/CODEX/ZAdvancedJobs/docs/SERVER_PROFILES.md)

Live-server upgrades and legacy config migration are documented in:

- [`docs/MIGRATIONS.md`](/Users/novaevent/Documents/CODEX/ZAdvancedJobs/docs/MIGRATIONS.md)

Recommended compatibility stacks and verification setups are documented in:

- [`docs/COMPATIBILITY.md`](/Users/novaevent/Documents/CODEX/ZAdvancedJobs/docs/COMPATIBILITY.md)

Recommended release smoke-pass order with the service client is documented in:

- [`docs/VERIFICATION_FLOW.md`](/Users/novaevent/Documents/CODEX/ZAdvancedJobs/docs/VERIFICATION_FLOW.md)

Recommended QA handoff template after that smoke-pass is documented in:

- [`docs/RELEASE_VERIFICATION_REPORT.md`](/Users/novaevent/Documents/CODEX/ZAdvancedJobs/docs/RELEASE_VERIFICATION_REPORT.md)

Optional integrations are discovered automatically during build if their jars are found in:

- `mods/`
- `run/mods/`
- `../mods/`

Detection is done by Forge mod metadata inside the jar, with filename-prefix matching only as fallback.
