## Z_Jobs UI Style Notes

### General direction
- Prefer vanilla Minecraft GUI composition over custom glossy panels.
- Use vanilla textures where possible: `advancements`, `container`, `widgets`, item icons.
- Avoid oversized windows that dominate the screen.
- If a list can overflow, show a visible scrollbar.

### Skills screen
- Keep the screen visually close to vanilla `Advancements`.
- Title row pattern:
  - left: one section title only, e.g. `Навыки`
  - right: one short status label only, e.g. `Очки: 2`
  - do not duplicate the title inside the same header
  - do not place a verbose counter like `Очки навыков: 2` in the title row
- Render both left and right header labels with the same text style:
  - plain `drawString(...)`
  - no custom glow
  - no outlined/chunky badge look
- The right-side header label may use a different color, but must keep the same font style and weight as the left title.

### Text rules
- Short text in headers, detailed text in tooltips.
- For skills state, prefer concise row markers and use tooltip for requirements/details.
- Avoid long wrapped labels in narrow header space.

### Layout rules
- Keep safe margins so the window never clips on common GUI scales.
- Center content sensibly, but do not waste large side areas with empty space.
- Tabs and controls should align to the window frame, not float far outside it.

### Validation
- After changing UI:
  - run project build
  - run live runtime matrix test with different window sizes and GUI scales
  - inspect screenshot output before considering the change finished
