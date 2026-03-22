# Vanilla UI Textures Reference

This file is a working reference for building `Z_Jobs` screens with vanilla Minecraft textures instead of custom UI art.

Goal:
- keep the mod visually close to vanilla Minecraft;
- reuse stable vanilla GUI assets;
- avoid random stretching and mixed styles;
- speed up future UI work in `JobsMainScreen.java`.

## Core Rules

1. Prefer vanilla texture families over custom fills.
2. Do not invent new color systems if a vanilla panel already solves it.
3. Do not stretch small widget textures arbitrarily unless the source asset is meant to tile or scale cleanly.
4. Use `advancements` textures for:
- skill trees
- progression maps
- branch tabs
- node widgets
5. Use container textures for:
- headers
- info strips
- inventory-like blocks
- framed utility panels
6. If a list can overflow, use a scrollbar instead of hiding entries.
7. If an asset looks blurry or dirty when stretched, switch to:
- tiled background
- sliced panel
- smaller usage area

## Vanilla Texture Families

### Advancements

Best for:
- large progression windows
- skill trees
- node chains
- branch tabs

Paths:
- `minecraft:textures/gui/advancements/window.png`
- `minecraft:textures/gui/advancements/widgets.png`
- `minecraft:textures/gui/advancements/backgrounds/stone.png`

Usage in `Z_Jobs`:
- `window.png`:
  used as the outer `Skills` frame
- `widgets.png`:
  used for skill tabs and node frames
- `backgrounds/stone.png`:
  tiled inside the skill viewport

Notes:
- `window.png` is good as a large frame, but full freeform stretching can produce ugly header artifacts.
- `widgets.png` is safe for node-sized pieces, not for big backgrounds.
- `backgrounds/stone.png` should be tiled, not stretched.

### Container Textures

Best for:
- clean header strips
- inventory-like panels
- vanilla neutral framing

Paths:
- `minecraft:textures/gui/container/generic_54.png`
- `minecraft:textures/gui/container/dispenser.png`
- `minecraft:textures/gui/container/hopper.png`
- `minecraft:textures/gui/container/furnace.png`
- `minecraft:textures/gui/container/anvil.png`
- `minecraft:textures/gui/container/cartography_table.png`
- `minecraft:textures/gui/container/stonecutter.png`
- `minecraft:textures/gui/container/smithing.png`
- `minecraft:textures/gui/container/grindstone.png`
- `minecraft:textures/gui/container/loom.png`

Recommended use:
- `generic_54.png`:
  default choice for neutral strips and top bars
- `cartography_table.png`:
  useful for map-like or board-like informational areas
- `anvil.png` / `smithing.png`:
  useful for upgrade or profession-specialized subpanels

Notes:
- container textures are usually better for headers than `advancements/window.png`.
- use them as slices or small strips, not full-screen stretched wallpapers.

### Recipe Book / Buttons / Icons

Best for:
- small utility widgets
- icon buttons
- tab accents

Common paths:
- `minecraft:textures/gui/recipe_book.png`
- `minecraft:textures/gui/widgets.png`
- `minecraft:textures/gui/icons.png`

Recommended use:
- `widgets.png`:
  classic button look, vanilla clickable controls
- `icons.png`:
  hearts, armor-like or utility accents only when matching vanilla semantics

Notes:
- do not overuse `icons.png` just because it exists.
- prefer real `ItemStack` icons over fake symbolic glyphs.

## Recommended Mapping For Z_Jobs

### Skills

Current direction:
- outer frame: `advancements/window.png`
- inner background: `advancements/backgrounds/stone.png`
- node frames: `advancements/widgets.png`
- branch tabs: `advancements/widgets.png` + real item icons
- inner info strip: `container/generic_54.png`

Why:
- this keeps the screen close to vanilla `Advancements`;
- branch tabs and nodes read like real progression UI;
- top strip stays readable without custom dark overlays.

### Daily / Contracts / Salary / Top

Recommended direction:
- use vanilla container-style panels
- keep rows flatter and simpler than current “achievement card” approach
- prefer:
  - `generic_54.png`
  - `widgets.png`
  - real `ItemStack` icons

Why:
- these screens are more list-like than tree-like;
- `advancements` visuals fit `Skills` best, not every tab.

### My Job

Recommended direction:
- summary/header: container textures
- progression/history blocks: limited `advancements` influence is acceptable
- bonuses/effects should look like vanilla info cards, not fully custom badges

Why:
- this tab is mostly a profile/summary screen, not a map screen.

## Real Item Icons

Prefer rendering actual vanilla items with `graphics.renderItem(...)` when possible.

Good examples:
- money/income: `Items.EMERALD`
- resources/mining: `Items.IRON_PICKAXE`
- utility/navigation: `Items.COMPASS`
- xp: `Items.EXPERIENCE_BOTTLE`
- combat: `Items.IRON_SWORD`
- defense: `Items.SHIELD`
- movement: `Items.FEATHER`
- luck: `Items.RABBIT_FOOT`
- fire resistance: `Items.MAGMA_CREAM`
- night vision: `Items.GOLDEN_CARROT`
- healing/health: `Items.GOLDEN_APPLE`

Why:
- item icons read immediately to players;
- they feel more vanilla than letters or custom marks.

## Safe Patterns

### Good

- tiled background textures
- item icons inside vanilla frames
- small textured headers
- progression nodes with fixed-size widgets
- scrollbars for overflow
- container textures for readable strips

### Bad

- stretching tiny widget textures over wide panels
- mixing too many texture families in one small area
- custom dark overlays on top of already decorative vanilla textures
- letter badges when a vanilla item icon can represent the same meaning
- hiding overflow instead of scroll

## Implementation Notes For `JobsMainScreen.java`

Current relevant assets in code:
- `ADVANCEMENTS_WINDOW`
- `ADVANCEMENTS_WIDGETS`
- `ADVANCEMENTS_STONE`
- `GENERIC_54`

Current main render helpers:
- `drawVanillaAdvancementWindow(...)`
- `drawVanillaSkillsHeader(...)`
- `drawSkillViewportBackground(...)`
- `renderVanillaSkillNode(...)`
- `renderSkillBranchTabs(...)`

If adding new vanilla assets:
1. add a dedicated `ResourceLocation` constant
2. keep asset usage limited to one UI purpose
3. prefer a helper method instead of inlining texture math everywhere
4. validate with autotest screenshots on multiple GUI scales

## Testing Checklist

Whenever a vanilla texture is introduced or changed, check:
- normal GUI scale
- small window
- larger GUI scale
- Unicode font enabled
- right HUD enabled and disabled
- Russian locale

Current matrix script:
- `Z:\My_mods\auto_test_mine\scripts\run-jobs-skills-matrix.ps1`

Current screenshot output directory:
- `Z:\My_mods\auto_test_mine\run-codex-client\matrix-results`

## Next Recommended UI Direction

If continuing vanilla alignment:
1. keep `Skills` closest to `Advancements`
2. simplify `Daily`, `Contracts`, `Salary`, and `Top` toward container/list layouts
3. reduce custom decorative panels in `My Job`
4. prefer vanilla icons + cleaner spacing over complex card art
