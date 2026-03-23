# Commands

## Player Commands

### Core

- `/jobs`
  - opens the main UI
- `/jobs help`
  - prints command help
- `/jobs info`
  - shows current job/economy summary
- `/jobs info`
  - now also explains common anti-abuse reasons for failed mob, loot, and exploration progress
- `/jobs stats`
  - shows detailed player progression data
- `/jobs stats`
  - now also gives reliable next-step guidance for combat, loot, and exploration progress

### Job Selection

- `/jobs choose <job>`
- `/jobs choose secondary <job>`
- `/jobs leave`
- `/jobs leave secondary`

Recommended first-run flow:
- `/jobs`
- `/jobs choose <job>`
- `/jobs info`
- `/jobs guide`

### Progression Screens

- `/jobs salary`
- `/jobs salary` explains the next useful action when salary is automatic, on cooldown, empty, or worth delaying for a larger manual claim
- `/jobs daily`
- `/jobs contracts`
- `/jobs contracts reroll`
- `/jobs contracts reroll` explains whether the player should wait for cooldown, earn more money, choose a profession, or inspect the newly rolled list
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
- `/jobsadmin health`
- `/jobsadmin perfcheck`
  - gives a compact performance-readiness snapshot for cache warmth, runtime flags, and anti-abuse tracker activity
- `/jobsadmin economycheck`
  - checks configured vs active provider, `Z_Economy` bridge availability, external currency presence, and tax sink UUID validity
- `/jobsadmin readycheck`
  - gives a compact release-readiness summary for jobs data, economy routing, caches, visual prep, and runtime flags before the staged smoke-pass
- `/jobsadmin payoutcheck <player>`
  - inspects pending salary, manual-claim cooldown, cap, tax preview, and payout readiness for one player
- `/jobsadmin routecheck <player>`
  - inspects live route mode, next desk, blocker, and recovery path for each player slot
- `/jobsadmin balancecheck <player>`
  - inspects progression stage, earnings, pending salary, skill points, and daily/contract saturation for one player
- `/jobsadmin balanceoverview`
  - shows average progression and economy shape across cached player profiles
- `/jobsadmin balancejobs`
  - shows which professions dominate assigned slots, average level, average earnings, and pending salary across cached player profiles
- `/jobsadmin balancejob <job>`
  - shows a single profession's live balance loop: adoption share, average level, average earnings, pending salary, and task activity
- `/jobsadmin balanceprogress <job>`
  - shows a single profession's progression pressure: free skill points, spent points, unlocked nodes, milestones, and adoption share
- `/jobsadmin reload`
- `/jobsadmin status`
  - now also warns when the reward index is empty or NPC skin caches look unprepared
- `/jobsadmin antiabuse`
- `/jobsadmin caches`
- `/jobsadmin caches`
  - now also explains whether empty reward-index or NPC-skin cache state likely needs reload, warmup, or local skin verification
- `/jobsadmin warmcaches`
- `/jobsadmin clearcaches`

Recommended recovery flow:
- `/jobsadmin status`
- `/jobsadmin warmcaches`
- `/jobsadmin reload`
- `/jobsadmin doctor [radius]`

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
