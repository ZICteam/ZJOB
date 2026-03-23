# Balance Checklist

Use this checklist when reviewing progression, salary pacing, reroll friction, and overall server economy pressure in `Advanced Jobs RPG`.

This document is for practical balance review, not code correctness.

## Goal

Confirm that:

- early game is understandable and rewarding
- mid game does not flatten into routine too early
- late game does not explode the economy or trivialize progression

## Test Profiles

Run balance checks against at least these profile styles:

1. `casual`
2. `progression`
3. `economy_heavy`

Use the examples in [`docs/SERVER_PROFILES.md`](/Users/novaevent/Documents/CODEX/ZAdvancedJobs/docs/SERVER_PROFILES.md).

## Early Game

Check the first 30-90 minutes:

- first profession choice feels meaningful
- first salary arrives quickly enough to teach the loop
- first daily tasks are understandable
- first contract is achievable without obscure setup
- first skill point or level-up arrives at a satisfying pace
- job-switch cost does not punish exploration too early

Watch for:

- confusion about what counts for progress
- too little money to feel progress
- too much money too early

## Mid Game

Check the first few real play sessions:

- salary, dailies, and contracts all remain relevant
- one system does not completely dominate the others
- reroll costs feel intentional rather than mandatory friction
- at least two professions remain competitively useful
- skill unlocks continue to matter

Watch for:

- one optimal money loop crowding out everything else
- contracts becoming mandatory instead of optional high-value content
- secondary job turning into a free passive multiplier with no tradeoff

## Late Game

Check long-session or admin-accelerated profiles:

- economy does not inflate too quickly
- tax sink still matters
- maxed or near-maxed jobs do not trivialize money generation
- title and milestone progression still feels like progression instead of noise
- experienced players still have a reason to engage with more than one screen

Watch for:

- runaway pending salary
- trivial reroll costs
- perks stacking into obviously dominant loops
- old content becoming fully irrelevant

## Economy Pressure

Review:

- salary payout size
- job-change cost
- contract reroll cost
- tax rate
- external economy interaction if `Z_Economy` is enabled

Questions to answer:

- does money leave the system at a meaningful rate
- can players recover from a bad choice without feeling trapped
- does external routing behave differently enough to require separate tuning

## Profession Comparison

For each profile style, compare:

- earning speed
- leveling speed
- contract completion effort
- daily task clarity
- perk usefulness

Look for:

- one profession always being the best first pick
- one profession becoming dead weight later
- one profession being balanced only because it is annoying to play

## Recommended Review Notes

Capture:

- profile used
- player count
- early/mid/late impressions
- obvious winners and losers
- any config values that likely caused the imbalance

Keep those notes with your release or balance pass records.

Useful live admin command during this pass:

- `/jobsadmin balancecheck <player>`
- `/jobsadmin balanceoverview`
- `/jobsadmin balancejobs`
- `/jobsadmin balancejob <job>`
- `/jobsadmin balanceprogress <job>`
