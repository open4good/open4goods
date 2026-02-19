# UI acceptance criteria

## Text integrity

- No UI copy in critical landing sections may be truncated or clipped.
- This rule applies to desktop, tablet, and mobile layouts.
- This rule also applies under browser zoom scenarios (at least 110% and 125%).
- Critical sections currently covered by automated visual checks are:
  - Home hero section.
  - Home marketing sections (`home-problems` and `home-solution`).
- Any regression that introduces hidden overflow, line clamping, or text clipping in these sections blocks release.
