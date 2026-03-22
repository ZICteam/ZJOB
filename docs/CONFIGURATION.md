# Configuration

All generated configuration files are stored in:

```text
config/ZAdvancedJobs/
```

## File Map

### `common.json`

Core gameplay, progression, salary cadence, cooldowns, and anti-abuse settings.

Typical keys:

- `allowSecondaryJob`
- `jobChangePrice`
- `jobChangeCooldownSeconds`
- `resetProgressOnChange`
- `storeAllJobProgress`
- `instantSalary`
- `salaryClaimIntervalSeconds`
- `maxSalaryPerClaim`
- `salaryTaxRate`
- `contractRerollPrice`
- `contractRerollCooldownSeconds`
- `maxJobLevel`
- `baseXp`
- `growthFactor`
- `dailyResetTime`

### `jobs.json`

Main profession catalog and reward logic.

Contains:

- job ids
- display names
- descriptions
- action groups
- reward values
- level scaling

### `perks.json`

Skill trees and perk node definitions.

Contains:

- branches
- nodes
- node parents
- unlock requirements
- perk effects

### `daily_tasks.json`

Daily quest templates.

Contains:

- task ids
- target counts
- reward values
- XP rewards
- filters by job or action

### `contracts.json`

Rotating contracts and rarity metadata.

Contains:

- contract ids
- rarity tiers
- reward scaling
- reroll-related data

### `economy.json`

Economy provider, external currency, tax sink, and dynamic multipliers.

Main keys:

- `provider`
  - `internal`
  - `external`
- `externalCurrency`
  - default: `z_coin`
- `taxSinkAccountUuid`
- `vipMultiplier`
- `eventMultiplier`
- `eventEndsAtEpochSecond`
- `worldMultipliers`
- `biomeMultipliers`

### `client.json`

Client-facing mirror values and UI hints.

### `npc_skins.json`

Service NPC skin sources.

Supports:

- `online`
- `local`

### `npc_labels.json`

Overhead labels for service desks and hub NPCs.

Examples:

- `Employment Center`
- `Quest Desk`
- `Contract Desk`
- `Salary Clerk`
- `Skills Mentor`
- `Hall of Fame`

## Anti-Abuse Settings

Important anti-abuse keys in `common.json` include:

- `blockArtificialMobRewards`
- `blockBabyMobRewards`
- `blockTamedMobRewards`
- `repeatedKillDecayThreshold`
- `lootContainerRewardCooldownSeconds`
- `exploredChunkRewardCooldownSeconds`

These control repetitive farm protection and low-value exploit loops.

## Recommended Workflow

1. Start with generated defaults.
2. Tune `jobs.json` and `perks.json` first.
3. Tune `daily_tasks.json` and `contracts.json` second.
4. Configure `economy.json` after deciding between internal or external provider.
5. Only then customize skins and labels.

## Validation And Fallbacks

- generated defaults are intended to be the starting point for manual edits
- malformed JSON or missing required fields now produce clearer log errors with the failing field path
- invalid business values such as duplicate ids, broken references, or impossible goals are rejected during config load
- when a file cannot be loaded, the mod falls back to safe defaults or empty runtime data depending on the config type

## External Economy Example

```json
{
  "provider": "external",
  "externalCurrency": "z_coin",
  "taxSinkAccountUuid": "00000000-0000-0000-0000-000000000001"
}
```
