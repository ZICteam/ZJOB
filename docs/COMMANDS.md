# Commands

## Player Commands

### Core

- `/jobs`
  - opens the main UI
- `/jobs help`
  - prints command help
- `/jobs info`
  - shows current job/economy summary
- `/jobs stats`
  - shows detailed player progression data

### Job Selection

- `/jobs choose <job>`
- `/jobs choose secondary <job>`
- `/jobs leave`
- `/jobs leave secondary`

### Progression Screens

- `/jobs salary`
- `/jobs daily`
- `/jobs contracts`
- `/jobs contracts reroll`
- `/jobs skills`
- `/jobs titles`
- `/jobs milestones`
- `/jobs top`
- `/jobs top all`

### Hub Navigation

- `/jobs where [radius]`
- `/jobs where <role> [radius]`
- `/jobs where native [radius]`
- `/jobs where wand [radius]`
- `/jobs where missing [radius]`
- `/jobs where ready [radius]`
- `/jobs where ready secondary [radius]`
- `/jobs where ready all [radius]`
- `/jobs where summary [radius]`
- `/jobs where summary secondary [radius]`
- `/jobs where summary all [radius]`
- `/jobs guide [radius]`
- `/jobs guide secondary [radius]`
- `/jobs guide all [radius]`
- `/jobs navigate [radius]`

### Economy Convenience

- `/money`
- `/bal`
- `/pay <player> <amount>`

## Admin Commands

### Core Admin

- `/jobsadmin help`
- `/jobsadmin reload`
- `/jobsadmin status`
- `/jobsadmin antiabuse`
- `/jobsadmin caches`
- `/jobsadmin warmcaches`
- `/jobsadmin clearcaches`

### Player/Profile Admin

- `/jobsadmin setjob <player> <job>`
- `/jobsadmin setjob <player> secondary <job>`
- `/jobsadmin setlevel <player> <job> <level>`
- `/jobsadmin addxp <player> <job> <amount>`
- `/jobsadmin addsalary <player> <amount>`
- `/jobsadmin skillpoints <player> <amount>`
- `/jobsadmin unlockskill <player> <job> <node>`
- `/jobsadmin reset <player>`
- `/jobsadmin resetall`

### Hub / NPC Workflow

- `/jobsadmin spawnmaster`
- `/jobsadmin spawndaily`
- `/jobsadmin spawncontracts`
- `/jobsadmin spawnsalary`
- `/jobsadmin spawnskills`
- `/jobsadmin spawnstatus`
- `/jobsadmin spawnhelp`
- `/jobsadmin spawntop`
- `/jobsadmin spawnhub`
- `/jobsadmin repairhub [radius]`
- `/jobsadmin clearhub [radius]`
- `/jobsadmin hubstatus [radius]`
- `/jobsadmin hublist [radius]`
- `/jobsadmin doctor [radius]`
- `/jobsadmin doctorfix [radius]`
- `/jobsadmin previewhub [radius]`
- `/jobsadmin hardenhub [radius]`
- `/jobsadmin normalizehub [radius]`
- `/jobsadmin migratenative [radius]`
- `/jobsadmin alignhub [radius]`
- `/jobsadmin exporthub [radius]`
- `/jobsadmin replacerole <role> [radius]`
- `/jobsadmin inspectrole <role> [radius]`
- `/jobsadmin inspectissues [radius]`

### NPC Appearance

- `/jobsadmin npcskin ...`
- `/jobsadmin npclabel ...`

### Event Controls

- `/jobsadmin eventstart`
- `/jobsadmin eventstop`
- `/jobsadmin eventmultiplier <value>`

## Notes

- `where`, `guide`, and `navigate` are the main player-facing hub routing tools.
- `summary`, `ready`, and `missing` are useful for quickly checking hub completeness.
- `doctor`, `doctorfix`, `inspectissues`, and `inspectrole` are the main admin diagnostics for service desks.
