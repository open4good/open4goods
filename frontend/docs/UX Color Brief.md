# UX Color Theme Brief & Engineering Guidelines

## Theme Name

**Fresh Tech / Calm Energy**

A modern, friendly, and trustworthy color system built around teal–green foundations, balanced with warm accents and clear functional contrasts. Designed for data-heavy, productivity, or wellbeing-oriented digital products.

---

## Core Principles

- **Clarity first**: Colors must reinforce hierarchy, affordance, and readability.
- **Emotional balance**: Cool tones for stability and trust, warm tones for attention and feedback.
- **Scalability**: Palette supports light/dark UI, states, charts, and brand surfaces.
- **Accessibility-aware**: Contrast ratios must be validated (WCAG 2.1 AA minimum).

---

## Color Palette & Semantic Roles

### 1. Primary Brand Colors

| Token               | Hex       | Usage                                                         |
| ------------------- | --------- | ------------------------------------------------------------- |
| `brand.primary`     | `#00DE9F` | Primary brand color, main CTAs, highlights, hero accents      |
| `brand.primary.alt` | `#00A1C2` | Secondary brand tone, navigation bars, headers, active states |

**Guideline**

- Use gradients between `#00DE9F → #00A1C2` for large surfaces or hero sections.
- Avoid overusing primary color for text; prefer neutral text on primary backgrounds.

---

### 2. Supporting Greens (Success & Growth)

| Token                  | Hex       | Usage                                              |
| ---------------------- | --------- | -------------------------------------------------- |
| `status.success.dark`  | `#008879` | Success states, confirmations, positive indicators |
| `status.success.light` | `#5BDB3B` | Growth metrics, positive trends, progress visuals  |
| `accent.green.cool`    | `#00D19F` | Secondary accents, toggles, icons                  |
| `accent.green.warm`    | `#A6E242` | Highlights, badges, secondary emphasis             |

**Guideline**

- Dark green (`#008879`) for text/icons on light backgrounds.
- Light greens (`#5BDB3B`, `#A6E242`) for charts, progress bars, and decorative emphasis.

---

### 3. Informational & Neutral Accents

| Token              | Hex       | Usage                                                  |
| ------------------ | --------- | ------------------------------------------------------ |
| `accent.info.warm` | `#FFCD90` | Tips, onboarding hints, soft warnings                  |
| `accent.info.cool` | `#00D1CE` | Informational messages, helper UI, links (non-primary) |

**Guideline**

- Use warm info for human-facing guidance.
- Use cool info for system-driven or data-driven information.

---

### 4. Feedback & Attention Colors

| Token           | Hex       | Usage                                      |
| --------------- | --------- | ------------------------------------------ |
| `status.error`  | `#FF8479` | Errors, destructive actions, alerts        |
| `status.action` | `#0088D6` | Focus states, active links, system actions |

**Guideline**

- Never use error color without an icon or text explanation.
- Blue action color should be reserved for interactive emphasis, not decoration.

---

## Interaction States

| State    | Color Rule                                     |
| -------- | ---------------------------------------------- |
| Hover    | Increase brightness or saturation by ~8–12%    |
| Active   | Shift toward darker variant                    |
| Disabled | Reduce opacity to 40–50% and remove saturation |
| Focus    | Use `#0088D6` outline or glow (2px min)        |

---

## Typography & Contrast Rules

- Body text: Neutral dark gray or near-black (not pure black).
- Text on primary backgrounds: White or off-white with contrast ≥ 4.5:1.
- Avoid green-on-green text combinations.
- Charts must not rely on color alone—use labels or patterns.

---

## Charting & Data Visualization

Recommended mapping:

- Positive trend → `#5BDB3B`
- Neutral trend → `#00D1CE`
- Warning → `#FFCD90`
- Negative trend → `#FF8479`
- Baseline / reference → `#0088D6`

---

## Do / Don’t Summary

**Do**

- Use teal/green as the emotional backbone.
- Keep warm colors rare and meaningful.
- Validate contrast in every new component.

**Don’t**

- Use all colors in a single screen.
- Use error color as decorative accent.
- Stack saturated greens next to each other without separation.

---

## UX Dev Engineer Notes

- Define colors as **design tokens** (CSS variables / theme objects).
- Support theming (light/dark) by adjusting luminance, not hue.
- Ensure system states (error, success, info) are globally consistent.
- Pair colors with icons and microcopy for clarity.

---

## Agent / Machine Prompt (for UI Generation)

> You are a UX/UI generation agent.  
> Use a teal–green primary color system centered on #00DE9F and #00A1C2.  
> Apply greens for success and growth, blues for actions, warm peach for warnings, and coral for errors.  
> Maintain clear hierarchy, accessible contrast, and restrained use of warm colors.  
> Prioritize clarity, calmness, and trust.  
> Never rely on color alone to communicate meaning.

---
