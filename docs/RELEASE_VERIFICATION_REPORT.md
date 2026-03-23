# Release Verification Report

Use this template after every staged release pass from [`docs/VERIFICATION_FLOW.md`](/Users/novaevent/Documents/CODEX/ZAdvancedJobs/docs/VERIFICATION_FLOW.md).

Copy the template into the release notes, a QA issue, or a deployment log before publishing a new jar.

## Template

```text
Release Version:
Build Artifact:
Verification Date:
Verifier:

Build And Config Generation:
- Status:
- Notes:

Visual Verification Client:
- Client Version:
- Optional Mods:
- Status:
- Notes:

Economy Server Verification:
- Provider Mode:
- External Currency:
- Status:
- Notes:

Admin Diagnostics:
- /jobsadmin health:
- /jobsadmin status:
- Notes:

Regression Highlights:
- Core player flow:
- Salary:
- Dailies:
- Contracts:
- Skills:
- NPC hub:

Known Risks:
- none / list remaining follow-up items

Release Decision:
- pass / hold
```

## Minimum Capture Rules

Always record:

- release version and artifact path
- whether the dedicated service client was used
- whether `Z_Economy` routing was checked
- whether admin diagnostics were reviewed
- any known risks that were accepted for release

## Recommended Storage

Keep the report next to the release process you already use:

- GitHub release draft
- internal release note
- QA ticket
- deployment changelog entry

The goal is consistency, not a specific storage system.

For the broader release sequence around this report, use:

- [`docs/RELEASE_WORKFLOW.md`](/Users/novaevent/Documents/CODEX/ZAdvancedJobs/docs/RELEASE_WORKFLOW.md)
