# REZON8 Visual Design Reference

## Icon Visual Breakdown

### App Icon Layers

```
┌─────────────────────────────────────┐
│     BACKGROUND LAYER                │
│  (ic_launcher_background.xml)       │
│                                     │
│  ╔═══════════════════════════╗      │
│  ║   Deep Black #000000      ║      │
│  ║                           ║      │
│  ║     Subtle gradient       ║      │
│  ║     circles creating      ║      │
│  ║     depth from center     ║      │
│  ║                           ║      │
│  ║   Faint copper glow at    ║      │
│  ║   center (3% opacity)     ║      │
│  ║                           ║      │
│  ╚═══════════════════════════╝      │
└─────────────────────────────────────┘

         +  (layered on top)

┌─────────────────────────────────────┐
│     FOREGROUND LAYER                │
│  (ic_launcher_foreground.xml)       │
│                                     │
│  ╔═══════════════════════════╗      │
│  ║                           ║      │
│  ║    ⭘ ← Copper ring        ║      │
│  ║   /   \                   ║      │
│  ║  │ ▊▊▊ │ ← Sound waves     ║      │
│  ║   \ ○ /   (copper)        ║      │
│  ║                           ║      │
│  ║  Headphones (white lines) ║      │
│  ║  + Ear cups (copper)      ║      │
│  ║  + Glass highlights       ║      │
│  ║                           ║      │
│  ╚═══════════════════════════╝      │
└─────────────────────────────────────┘

         =  (final result)

┌─────────────────────────────────────┐
│      FINAL APP ICON                 │
│                                     │
│         ⚪ ← Copper glow ring       │
│        ╱   ╲                        │
│       │ 🎧  │ ← Headphones (white)  │
│       │ ▊▊▊ │ ← Waves (copper)      │
│        ╲ ● ╱  ← Ear cups (copper)   │
│                                     │
│  Glass highlights on top/bottom     │
│  Dark sophisticated appearance      │
└─────────────────────────────────────┘
```

---

## Splash Screen Layout

```
┌──────────────────────────────────────┐
│                                      │
│          (Black Background)          │
│                                      │
│              180dp ↓                 │
│                                      │
│            ┌─────────┐               │
│            │         │               │
│            │   🎧    │  160x160dp    │
│            │  ▊▊▊    │  Logo Icon    │
│            │         │               │
│            └─────────┘               │
│                                      │
│              20dp ↓                  │
│                                      │
│         ╔═══════════════╗            │
│         ║   REZON8      ║ 240x60dp   │
│         ║   ───────     ║ Text       │
│         ╚═══════════════╝            │
│              ↑                       │
│         Copper "8" + underline       │
│                                      │
│                                      │
│          (remaining space)           │
│                                      │
└──────────────────────────────────────┘
```

---

## Color Usage Examples

### Copper Accent Applications

```
┌────────────────────────────────────┐
│  INTERACTIVE ELEMENTS              │
├────────────────────────────────────┤
│                                    │
│  Play Button:    ⬤  #CD7F32        │
│  Progress Bar:   ▰▰▰▰▱▱▱  (copper) │
│  Selected Tab:   ────  (copper)    │
│  Link Text:      Click  (copper)   │
│                                    │
└────────────────────────────────────┘

┌────────────────────────────────────┐
│  GLASS SURFACE EXAMPLE             │
├────────────────────────────────────┤
│                                    │
│  ╭──────────────────────────╮      │
│  │  #1A1A1A background      │      │
│  │                          │      │
│  │  White text              │      │
│  │  Copper accent ⬤         │      │
│  │                          │      │
│  │  1px white border (10%)  │      │
│  ╰──────────────────────────╯      │
│                                    │
└────────────────────────────────────┘
```

---

## Typography Scale

```
┌────────────────────────────────────────┐
│                                        │
│  Display (40sp)                        │
│  Book Title Here                       │
│                                        │
│  Headline (28sp)                       │
│  Chapter Name                          │
│                                        │
│  Title (22sp)                          │
│  Section Heading                       │
│                                        │
│  Body (16sp)                           │
│  This is the main reading text for     │
│  descriptions and content.             │
│                                        │
│  Caption (14sp)                        │
│  Author name, metadata                 │
│                                        │
└────────────────────────────────────────┘
```

---

## UI Component Examples

### Card Design (Glass Style)

```xml
Shape Properties:
  Background: #1A1A1A
  Corner Radius: 16dp
  Stroke: 1dp #FFFFFF @ 10% opacity
  Elevation: None (use glow instead)

Glow Effect (optional):
  Color: #CD7F32 @ 5% opacity
  Blur: 8dp
  Offset: 0,2dp
```

Visual:
```
╭────────────────────────────╮
│  #1A1A1A                   │
│                            │
│  ⬤ Audio Title             │ ← Copper accent
│  by Author Name            │
│                            │
│  ▰▰▰▰▰▱▱▱▱▱ 47%           │ ← Copper progress
│                            │
╰────────────────────────────╯
 └─ subtle glow underneath
```

### Button Styles

**Primary Button (Copper)**
```
╭──────────────╮
│   Play Now   │  Background: #CD7F32
╰──────────────╯  Text: #FFFFFF
                  Radius: 24dp
```

**Secondary Button (Glass)**
```
╭──────────────╮
│   Library    │  Background: transparent
╰──────────────╯  Border: 2dp #FFFFFF @ 20%
                  Text: #FFFFFF
                  Radius: 24dp
```

**Text Button**
```
  Skip  →         No background
                  Text: #CD7F32
                  No border
```

---

## State Variations

### Copper Accent States

```
Normal:    #CD7F32 (100%)  ⬤
Hover:     #E09850 (120%)  ⬤  (lighter)
Pressed:   #A66528 (80%)   ⬤  (darker)
Disabled:  #CD7F32 (40%)   ⬤  (faded)
```

### Glass Surface States

```
Normal:    #1A1A1A          ╭────╮
                            │    │
                            ╰────╯

Hover:     #1A1A1A          ╭────╮
           + white 5%       │    │  (slightly lighter)
                            ╰────╯

Pressed:   #0D0D0D          ╭────╮
                            │    │  (darker)
                            ╰────╯

Selected:  #1A1A1A          ╭────╮
           + copper ring    │ ⬤  │  (accent indicator)
                            ╰────╯
```

---

## Icon Style Guide

### Line Icons (UI)
- Stroke width: 2dp
- Color: #FFFFFF or #CD7F32
- Style: Rounded line caps
- Size: 24dp default

Examples:
```
Play:     ▶
Pause:    ⏸
Next:     ⏭
Previous: ⏮
Settings: ⚙
Search:   🔍
Menu:     ☰
```

### Filled Icons (States)
Use copper fill for:
- Active states
- Favorites/bookmarks
- Selected items

Use white outline for:
- Inactive states
- Unselected items
- Default state

---

## Design Patterns

### Audio Player Controls

```
╭────────────────────────────────────╮
│                                    │
│  ⏪    ⏸    ⏩                      │ ← White icons
│                                    │
│  ▰▰▰▰▰▰▰▰▰▱▱▱▱▱▱                  │ ← Copper bar
│  12:34 ──────────────── 45:67      │
│                                    │
╰────────────────────────────────────╯
```

### Book Card Grid

```
╭──────╮  ╭──────╮  ╭──────╮
│ 🖼   │  │ 🖼   │  │ 🖼   │  Cover images
│      │  │      │  │      │
╰──────╯  ╰──────╯  ╰──────╯
 Title     Title     Title    White text
 Author    Author    Author   Gray caption
```

### Bottom Navigation

```
╭────────────────────────────────────╮
│                                    │
│  🏠    📚    ⬤    ⚙               │
│ Home  Lib  Play  Sets              │
│            ↑                       │
│     (copper = selected)            │
│                                    │
╰────────────────────────────────────╯
```

---

## Spacing System

```
Micro:    4dp    ·
Small:    8dp    · ·
Medium:   16dp   · · · ·
Large:    24dp   · · · · · ·
XLarge:   32dp   · · · · · · · ·
XXLarge:  48dp   · · · · · · · · · · · ·
```

Usage:
- Micro: Icon padding
- Small: Between related elements
- Medium: Card padding, between sections
- Large: Screen margins
- XLarge: Between major sections
- XXLarge: Top/bottom screen padding

---

## Shadow & Depth

Instead of material elevation, use:

1. **Layering**: Darker backgrounds beneath lighter surfaces
2. **Glows**: Soft copper or white glows at 5-10% opacity
3. **Borders**: Subtle white borders at 10-20% opacity
4. **Gradients**: Subtle gradients to suggest curvature

```
Traditional Shadow:       Glass Approach:
┌──────────┐             ┌──────────┐
│  Card    │             │  Card    │ ← border glow
└──────────┘             └──────────┘
  └─ shadow                 └─ soft glow
```

---

## Accessibility Notes

### Contrast Ratios
- White on Black: 21:1 ✓ (WCAG AAA)
- Copper on Black: 4.8:1 ✓ (WCAG AA for large text)
- White on Glass Dark: 15:1 ✓ (WCAG AAA)

### Color Blindness
- Copper accent distinguishable in most types of color blindness
- Don't rely solely on color; use icons/text labels
- Provide sufficient contrast for all interactive elements

### Touch Targets
- Minimum: 48dp x 48dp
- Recommended: 56dp x 56dp for primary actions
- Spacing: 8dp minimum between targets

---

## Animation Guidelines

### Timing
```
Fast:     150ms   (small changes, hover)
Medium:   300ms   (standard transitions)
Slow:     450ms   (large movements, reveals)
```

### Easing
- **Ease out**: Elements entering screen
- **Ease in**: Elements leaving screen
- **Ease in-out**: Moving between states

### Effects
- Fade: Opacity transitions
- Slide: Enter/exit animations
- Scale: Button press feedback
- Glow: Pulse for loading states

No excessive bouncing or overshoot - keep it smooth and premium.

---

## Do's and Don'ts

### Do ✓
- Use copper sparingly for impact
- Maintain generous whitespace
- Layer surfaces for depth
- Use soft glows instead of hard shadows
- Keep animations smooth and subtle

### Don't ✗
- Use multiple bright colors
- Add heavy gradients everywhere
- Use hard shadows
- Overcomplicate designs
- Use decorative elements without purpose

---

## Implementation Checklist

When applying this design system:

- [ ] Use defined color values from colors.xml
- [ ] Apply 16dp corner radius to cards
- [ ] Use 24dp for primary buttons
- [ ] Include 16dp padding in cards
- [ ] Use copper only for accents/highlights
- [ ] Maintain 48dp minimum touch targets
- [ ] Use appropriate text sizes from scale
- [ ] Add subtle borders to glass surfaces
- [ ] Test contrast ratios
- [ ] Verify dark theme compliance

---

Last Updated: December 22, 2025
