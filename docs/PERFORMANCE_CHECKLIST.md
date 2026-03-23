# Performance Checklist

Use this checklist when checking whether `Advanced Jobs RPG` is behaving safely on a real server and in the dedicated visual-verification client.

This is not a synthetic benchmark document. It is a practical pass for catching obvious lag sources before release or after large gameplay changes.

## Goal

Confirm that:

- the server does not show obvious spikes during common jobs workflows
- the client remains responsive while opening the main screens
- admin maintenance flows do not trigger avoidable heavy refreshes

## Test Environments

Run the checklist in at least these environments:

1. baseline server stack
2. economy server stack with `Z_Economy`
3. service client visual stack

Use the compatibility tiers from [`docs/COMPATIBILITY.md`](/Users/novaevent/Documents/CODEX/ZAdvancedJobs/docs/COMPATIBILITY.md).

## Server Pass

Check these actions on a running Forge `1.20.1` server:

1. player join
2. first `/jobs` open
3. repeated screen open for `Salary`, `Daily`, `Contracts`, `Skills`, and `Top`
4. salary claim
5. contract reroll
6. `/jobsadmin reload`
7. `/jobsadmin warmcaches`
8. `/jobsadmin readycheck`
9. `/jobsadmin economycheck`
10. `/jobsadmin payoutcheck <player>`
11. NPC hub interaction with multiple nearby service desks

Watch for:

- long server pauses
- visible tick hitching
- repeated lag after admin cache or reload commands
- economy-related delays during salary payout

## Client Pass

Use the dedicated service client and check:

1. GUI open responsiveness
2. screen switching between tabs
3. board interaction delay
4. NPC label and skin presentation
5. leaderboard open time
6. repeated open/close loops after `/jobsadmin reload`

Watch for:

- freezing when opening `/jobs`
- slow tab switching
- delayed leaderboard rendering
- visible hitching around the hub

## Suggested Test Sizes

Minimum:

- 1 player for baseline validation

Recommended:

- 2 to 5 concurrent players opening the UI and using salary/contracts

This does not replace real production load, but it is enough to catch obvious regressions from payload, cache, and sync changes.

## Admin Signals To Review

During the pass, review:

- `/jobsadmin health`
- `/jobsadmin perfcheck`
- `/jobsadmin status`
- `/jobsadmin caches`
- `/jobsadmin readycheck`
- `/jobsadmin economycheck`

These should help explain whether issues come from:

- cold caches
- bad economy routing
- empty reward indexes
- visual verification setup problems

## Release Gate

Do not treat the performance pass as clean if:

- the server stutters on normal `/jobs` open flow
- admin reload or cache warmup leaves the server in a visibly degraded state
- salary payout becomes noticeably delayed in the economy stack
- the service client becomes slow around the jobs hub or main screens

## Recommended Notes

When performance looks suspicious, capture:

- which stack was used
- how many players were online
- which command or screen triggered the slowdown
- whether caches were warm
- whether external economy mode was active

Store those notes together with the normal release verification report.
