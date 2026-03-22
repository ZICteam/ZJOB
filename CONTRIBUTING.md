# Contributing

## Scope

This repository contains the `Advanced Jobs RPG` mod for Forge `1.20.1`.

## Development Rules

- use Java `17`
- keep UI changes aligned with `UI_STYLE_NOTES.md`
- prefer vanilla Minecraft GUI patterns over custom visual systems
- keep `Z_Economy` integration on the public API only
- do not commit generated build output

## Build

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat build
```

## Before Opening a PR

- verify the project builds locally
- update docs when commands, config, or UI behavior changes
- include screenshots for UI changes when practical
- keep localization files in sync when adding player-facing text
