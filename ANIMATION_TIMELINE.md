# iOS 26 Mini Player Animation Timeline

## EXPAND ANIMATION (Circle → Pill)
Duration: ~350ms total

```
TIME (ms)    SHAPE                 SCALE           ROTATION      CONTENT
═══════════  ════════════════════  ══════════════  ════════════  ═══════════════════
0            56×56, r=28           0.90            0°            Circle: α=1.0
             ░░░░                  ↓ compress                    Pill:   α=0.0

50           64×58, r=28.5         0.95            +2°           Circle: α=0.8
             ░░░░░░                ↓ rising        ↗ rotate      Pill:   α=0.0

100          80×60, r=29           0.98            +4°           Circle: α=0.5  ← fade starts
             ░░░░░░░░              ↓ accelerate    ↗ peak        Pill:   α=0.1  ← stagger delay

150          120×62, r=30          1.02            +5°           Circle: α=0.2
             ░░░░░░░░░░░░          ↑ overshoot!    ↗ max         Pill:   α=0.4

200          200×63, r=31          1.03            +3°           Circle: α=0.0  ← fully faded
             ░░░░░░░░░░░░░░░░      ↑ PEAK!         ↙ spring      Pill:   α=0.7

250          300×63.5, r=31.5      1.01            +1°           Circle: α=0.0
             ░░░░░░░░░░░░░░░░░░░   ↓ settling      ↙ return      Pill:   α=0.9

300          360×64, r=32          1.00            0°            Circle: α=0.0
             ░░░░░░░░░░░░░░░░░░░░  ↓ approach      ← settled     Pill:   α=1.0  ← fully visible

350          400×64, r=32          1.00            0°            Circle: α=0.0
             ░░░░░░░░░░░░░░░░░░░░  ← RESTING                     Pill:   α=1.0
             ████████████████████
```

### Key Moments:
- **t=0**: Haptic `CLOCK_TICK`, scale compress to 0.9
- **t=100**: Content crossfade begins (100ms stagger)
- **t=200**: Peak overshoot (1.03x scale, +5° rotation)
- **t=350**: Settled into pill state

---

## SHRINK ANIMATION (Pill → Circle)
Duration: ~350ms total

```
TIME (ms)    SHAPE                 SCALE           ROTATION      CONTENT
═══════════  ════════════════════  ══════════════  ════════════  ═══════════════════
0            400×64, r=32          1.00            0°            Circle: α=0.0
             ████████████████████  ↓ start                       Pill:   α=1.0

50           300×63, r=31          0.98            -2°           Circle: α=0.0
             ░░░░░░░░░░░░░░░░░░░   ↓ compress      ↙ rotate      Pill:   α=0.8

100          200×62, r=30          0.96            -4°           Circle: α=0.1  ← stagger delay
             ░░░░░░░░░░░░░░░░      ↓ shrinking     ↙ peak        Pill:   α=0.5  ← fade starts

150          120×61, r=29          0.95            -5°           Circle: α=0.4
             ░░░░░░░░░░░░          ↓ COMPRESS!     ↙ max         Pill:   α=0.2

200          80×59, r=28.5         0.96            -3°           Circle: α=0.7
             ░░░░░░░░              ↑ bounce back   ↗ spring      Pill:   α=0.0  ← fully faded

250          64×57, r=28.2         0.98            -1°           Circle: α=0.9
             ░░░░░░                ↑ rising        ↗ return      Pill:   α=0.0

300          58×56.5, r=28         0.99            0°            Circle: α=1.0  ← fully visible
             ░░░░░                 ↑ approach      ← settled     Pill:   α=0.0

350          56×56, r=28           1.00            0°            Circle: α=1.0
             ░░░░                  ← RESTING                     Pill:   α=0.0
```

### Key Moments:
- **t=0**: Haptic `CLOCK_TICK`, begin shrink
- **t=100**: Content crossfade begins (100ms stagger)
- **t=150**: Peak compress (0.95x scale, -5° rotation)
- **t=350**: Settled into circle state

---

## SPRING PHYSICS COMPARISON

### EXPAND (Pill appears)
```
Damping: 0.75 (high)    Stiffness: 250 (medium)

Position
  │
1.03├─────╮
1.02│      ╲
1.01│       ╲___
1.00│           ╲___________  ← smooth settle, minimal bounce
0.99│
0.98│
0.97│
0.96│
0.95│              ╱
0.94│           ╱
0.93│        ╱
0.92│     ╱
0.91│  ╱
0.90├─╯
    └─────────────────────────────────── Time
    0   50  100 150 200 250 300 350
```

### SHRINK (Circle appears)
```
Damping: 0.65 (low)     Stiffness: 300 (high)

Position
  │
1.00├─╮
0.99│  ╲            ╱╲       ← bouncy! underdamped
0.98│   ╲          ╱  ╲___
0.97│    ╲        ╱       ╲___
0.96│     ╲      ╱            ╲
0.95│      ╲    ╱              ╲___
0.94│       ╲  ╱                   ╲
0.93│        ╲╱                     ╲___
    └─────────────────────────────────── Time
    0   50  100 150 200 250 300 350
```

---

## CONTENT CROSSFADE STAGGER

### Visual representation:

```
EXPAND (Circle → Pill)

Circle Content Alpha:
1.0 ████████████░░░░░░░░░░░░░░░░░░░░░░░░░░  ← immediate fade
0.8 ████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░
0.5 ████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░
0.0 ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░
    └───────────────────────────────────
    0   50  100 150 200 250 300 350

Pill Content Alpha:
1.0 ░░░░░░░░░░░░░░░░░░░░░░░░████████████████  ← 100ms delay
0.8 ░░░░░░░░░░░░░░░░░░░░░░████████░░░░░░░░░░
0.5 ░░░░░░░░░░░░░░░░░░████░░░░░░░░░░░░░░░░░░
0.0 ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░
    └───────────────────────────────────
    0   50  100 150 200 250 300 350

STAGGER: Shape starts at t=0, pill content starts at t=100
RESULT: Smooth handoff, no jarring "pop"
```

---

## ROTATION FLOURISH

### Expand (+5° clockwise flourish)
```
Rotation (degrees)
     │
  +5 ├──────────╮
  +4 │           ╲
  +3 │            ╲
  +2 │             ╲___
  +1 │                 ╲___
   0 ├─────────────────────╲_________
  -1 │
     └────────────────────────────────
     0   50  100 150 200 250 300 350
```

### Shrink (-5° counter-clockwise flourish)
```
Rotation (degrees)
     │
  +1 │
   0 ├─╮
  -1 │  ╲___
  -2 │      ╲___
  -3 │          ╲
  -4 │           ╲
  -5 ├────────────╲____
  -6 │                 ╲_________
     └────────────────────────────────
     0   50  100 150 200 250 300 350
```

**Why rotate?**
- Adds **organic, alive feeling**
- Mimics **real-world physics** (objects spin when morphing)
- **iOS parallel**: Notification banners, Control Center toggles

---

## HAPTIC TIMING

```
USER ACTION          HAPTIC TYPE      STRENGTH    TIMING
═══════════════════  ═══════════════  ══════════  ════════
Tap circle           VIRTUAL_KEY      Light       Instant
→ Expand starts      CLOCK_TICK       Refined     t=0

Auto-collapse        CLOCK_TICK       Refined     t=0
(3s timeout)

Tap pill             (none)           -           -
→ Opens player       -                -           -

Long-press pill      LONG_PRESS       Strong      t=500ms
→ Shows indicator    -                -           -

Swipe down (fail)    (none)           -           -
→ Snap back          -                -           -

Swipe down (success) LONG_PRESS       Strong      When dismissed
→ Dismiss            -                -           -
```

**Android Haptic Constants**:
- `VIRTUAL_KEY`: 1-2ms buzz (light tap)
- `CLOCK_TICK`: 2-3ms buzz (refined, iOS "selection")
- `LONG_PRESS`: 10-15ms buzz (strong, definitive)

---

## COMPARISON: OLD vs NEW

### OLD (AnimatedVisibility approach)

```
STATE CHANGE: isCollapsed = true

t=0:     [CIRCLE] visible=false  ← AnimatedVisibility
         [PILL]   visible=true

         CIRCLE starts scaleOut + fadeOut
         PILL starts shrinkHorizontally + fadeIn

t=150:   [CIRCLE] alpha=0.5, scale=0.75  ← overlapping!
         [PILL]   alpha=0.5, width=50%

t=300:   [CIRCLE] removed from composition
         [PILL]   settled

ISSUES:
- Two composables exist simultaneously
- Layout shifts as width changes
- No content stagger
- Generic spring curves
- No rotation flourish
```

### NEW (Morphing approach)

```
STATE CHANGE: isCollapsed = true

t=0:     [MORPH] width=400dp, height=64dp, r=32dp

         Shape starts morphing
         Scale starts overshoot
         Rotation starts flourish
         Haptic: CLOCK_TICK

t=100:   [MORPH] width=200dp, height=62dp, r=30dp

         Pill content starts fading out
         Circle content starts fading in (stagger!)

t=200:   [MORPH] width=80dp, height=59dp, r=28.5dp

         Pill content alpha=0 (fully gone)
         Circle content alpha=0.7 (appearing)
         Scale compress peak (0.95x)

t=350:   [MORPH] width=56dp, height=56dp, r=28dp

         Circle content alpha=1.0 (fully visible)
         Settled into circle state

BENEFITS:
- Single composable throughout
- No layout shifts (uses fixed width)
- Staggered content crossfade
- Tuned spring physics
- Rotation flourish adds life
```

---

## PERFORMANCE METRICS

### Frame Budget (60fps)
- **Frame time**: 16.67ms
- **Animation budget**: ~13ms (80% of frame)
- **Composition**: ~2ms
- **Drawing**: ~10ms
- **Remaining**: ~3ms buffer

### Optimizations Applied
1. **Alpha gating**: Skip composition when α < 0.01
2. **Conditional modifiers**: Only apply gestures when needed
3. **GraphicsLayer**: Hardware-accelerated scale/rotate
4. **Content reuse**: Single AsyncImage per state

### Expected Performance
- **60fps**: Smooth on modern devices (2020+)
- **30fps**: Acceptable on budget devices
- **Jank**: <1% missed frames (target)

---

## ACCESSIBILITY

### Motion Preferences
Respect `Settings.Global.TRANSITION_ANIMATION_SCALE`:

```kotlin
val animationScale = Settings.Global.getFloat(
    context.contentResolver,
    Settings.Global.TRANSITION_ANIMATION_SCALE,
    1.0f
)

val effectiveDelay = (contentDelay * animationScale).toInt()
val effectiveRotation = rotation * animationScale
```

**Result**:
- Motion disabled (scale = 0): Instant transitions
- Motion reduced (scale = 0.5): Half-speed animations
- Normal (scale = 1.0): Full animations

---

## CONCLUSION

The new morphing animation system delivers:

1. **TRUE shape morphing**: Single composable that transforms
2. **iOS 26 physics**: Tuned spring curves with overshoot
3. **Organic motion**: Rotation flourish + breathing effect
4. **Smooth crossfade**: 100ms stagger prevents jarring transitions
5. **Premium feel**: Haptic feedback on state changes
6. **High performance**: Hardware-accelerated, optimized composition

**The result feels ALIVE.**
