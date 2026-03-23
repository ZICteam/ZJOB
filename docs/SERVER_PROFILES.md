# Server Profiles

These presets are starting points for real servers running `Advanced Jobs RPG`.

All profile examples live under:

```text
docs/server_profiles/
```

Each profile currently includes:

- `common.json`
- `economy.json`

Apply them by copying the chosen files into:

```text
config/ZAdvancedJobs/
```

Then restart the server.

## Casual

Folder:

```text
docs/server_profiles/casual/
```

Best for:

- small friend groups
- low-friction survival servers
- servers where jobs should support gameplay instead of dominating it

Design goals:

- secondary job enabled
- cheaper job changes
- faster salary claims
- cheaper contract rerolls
- lower tax pressure

## Progression

Folder:

```text
docs/server_profiles/progression/
```

Best for:

- long-term RPG servers
- guided progression with meaningful unlock pacing
- servers where professions should matter over time

Design goals:

- slower job switching
- stronger commitment to chosen jobs
- moderate tax sink
- moderate reroll friction
- higher XP curve than casual

## Economy Heavy

Folder:

```text
docs/server_profiles/economy_heavy/
```

Best for:

- economy-first servers
- long retention loops
- servers already using `Z_Economy`

Design goals:

- external economy routing by default
- meaningful tax sink
- slower salary extraction
- more expensive rerolls and job switching
- stronger inflation control

## Recommended Workflow

1. Start with the nearest profile instead of editing defaults from scratch.
2. Copy only `common.json` and `economy.json` first.
3. Keep generated `jobs.json`, `perks.json`, `daily_tasks.json`, and `contracts.json` unless you already have a curated balance pack.
4. Run through `docs/TESTING_CHECKLIST.md` after switching profiles.
5. Run through [`docs/BALANCE_CHECKLIST.md`](/Users/novaevent/Documents/CODEX/ZAdvancedJobs/docs/BALANCE_CHECKLIST.md) after 1-2 real play sessions.
6. Adjust salary, reroll, and XP pacing only after that balance pass.

## Notes

- These are examples, not hardcoded presets.
- They are safe to edit further after copying.
- If you use `economy_heavy`, pair it with `Z_Economy` for the intended server flow.
