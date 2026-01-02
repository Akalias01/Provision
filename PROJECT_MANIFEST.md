# PROJECT MANIFEST: LITHOS

**App:** LITHOS (Audiobook Player) | **Studio:** MOSSGLEN | **Package:** com.mossglen.lithos
**Architecture:** MVVM + Clean Architecture + Jetpack Compose + Media3
**Platform:** Android 10+ (API 29) | Target: Android 16 (API 36)

---

## REPOSITORY INFO

| Property | Value |
|----------|-------|
| **Repository** | github.com/Akalias01/Provision |
| **Branch** | lithos |
| **Related Project** | LITHOS READ (Ebook Reader) - separate repo: github.com/Akalias01/LithosRead |

---

## VISION

LITHOS is a bleeding-edge audiobook player that sets the industry standard. Every interaction should feel effortless, every feature should anticipate user needs, and every pixel should demonstrate premium craftsmanship.

**MANDATE:**
1. Lead the market in innovation
2. Execute every feature to the highest standard
3. Feel native to premium devices (Galaxy S/Z, Pixel Pro)
4. Push boundaries while maintaining intuitive simplicity

---

## LITHOS AMBER DESIGN LANGUAGE

### 1. The Design Philosophy: "Lithos"
The app should feel **Premium, Natural, and Tactile**.
* **No Neon/Cyberpunk:** Avoid "glowing" or "digital" effects.
* **Material:** Think of Stone (Slate), Fossilized Resin (Amber), and Forests (Moss).
* **Finish:** Surfaces should be **Matte** or **Satin**, not glossy/shiny.

### 2. The Color Palette (3-Mode System)

**A. The "Amber" Accent (Primary Highlight)**
* **Hex:** `#D48C2C` (Deep Burnished Amber).
* **Usage:** Progress Ring stroke, Scrubber Bar, Current Chapter Text, Active Icons.
* **Style:** Matte finish. High legibility, warm tone.

**B. The "Moss" Anchor (Primary Action)**
* **Hex:** `#4A5D45` (Deep Moss Green).
* **Usage:** EXCLUSIVELY for the Main Play/Pause Button background.
* **Style:** Solid, non-gradient.

**C. Background Modes**
| Mode | Hex | Description |
|------|-----|-------------|
| **Standard Dark** | `#1A1D21` | Deep Slate - primary dark mode |
| **Reader Light** | `#F2F0E9` | Warm Oat/Paper - light reading mode |
| **OLED Night** | `#000000` | True Black - battery saving |

### 3. Component Specifications

**A. Progress Ring**
* **Stroke Width:** VERY THIN (`2px` or `3px`) - fine wire inlay aesthetic
* **Color:** Solid `#D48C2C` (Amber)
* **Track:** Subtle dark grey (`rgba(0,0,0,0.2)`)

**B. Main Scrubber/Seek Bar**
* **Color:** `#D48C2C` (Amber)
* **Track:** Muted slate background
* **Timestamps:** Standard positioning

**C. Moss Anchor Play Button**
* **Color:** Solid `#4A5D45` (Moss Green)
* **Effect:** Subtle inner shadow (`inset 0 2px 4px rgba(0,0,0,0.25)`) for pressed feel
* **Style:** No gradient, tactile appearance

**D. Glass Pill Bottom Nav**
* **Style:** Frosted Glass effect (`blur(20px)`)
* **Color:** Semi-transparent slate (`rgba(26, 29, 33, 0.85)`)
* **Border:** Subtle 1px border at 10% white

### 4. Typography
* **Headers:** Bold, high contrast
* **Body:** Regular weight, excellent readability
* **Captions:** Light weight, secondary color

### 5. Iconography
* **Style:** Outlined for inactive, filled for active
* **Color:** Amber for active, muted slate for inactive
* **Size:** Consistent 24dp with 48dp touch targets

---

## CORE PRINCIPLES

| Principle | Meaning |
|-----------|---------|
| **Invisible Perfection** | Best design is invisible. Users accomplish tasks without thinking how. |
| **Premium by Default** | No placeholder designs. If Apple wouldn't ship it, neither do we. |
| **Anticipatory Intelligence** | Predict what users want before they ask. |
| **Effortless Power** | Complex features feel simple. Power users get depth; casual users get simplicity. |
| **Sensory Harmony** | Visual, haptic, and audio feedback work in concert. |

---

## DEVELOPMENT MANDATE: UNCOMPROMISING EXCELLENCE

**THIS SECTION IS NON-NEGOTIABLE. READ IT BEFORE EVERY TASK.**

### The Standard

LITHOS competes with Apple, Audible, and Kindle. We do not aspire to match them—**we aim to exceed them**. Every feature, every animation, every interaction must be premium quality or it does not ship.

### For AI Assistants (Claude, etc.)

When working on this codebase, you MUST:

1. **NEVER simplify or cut corners** - "Good enough" is never acceptable. If the first implementation isn't premium, iterate until it is.

2. **ALWAYS research when uncertain** - If you don't know the best practice for something, search the web, read documentation, analyze how top apps do it. Ignorance is not an excuse for mediocre work.

3. **Match or exceed Apple quality** - Before implementing any UI element, animation, or interaction, ask: "Would Apple ship this?" If not, redesign until the answer is yes.

4. **Use proper techniques** - No hacky workarounds. Use the correct APIs, proper animation specs, industry-standard patterns. Research if needed.

5. **Test on real devices** - Compile and install. Verify it works AND looks/feels premium.

6. **Follow Lithos Amber** - All UI must use the Lithos Amber design language. No neon, no glowing effects, matte/satin finishes only.

### Quality Checklist (EVERY Implementation)

```
□ Did I research best practices for this?
□ Does this match or exceed Apple's implementation?
□ Would a premium user be impressed?
□ Are the animations smooth (60fps+)?
□ Does it feel native to the platform?
□ Is the code using proper techniques (not hacks)?
□ Did I test on a real device?
□ Does it follow Lithos Amber design language?
```

### When You Encounter a Bug or Issue

1. **Understand the root cause** - Don't just patch symptoms
2. **Research the correct solution** - How do top apps handle this?
3. **Implement properly** - Use the right APIs and patterns
4. **Verify quality** - Test thoroughly, ensure premium feel
5. **Never regress** - The fix must not break other things

### Animation & Interaction Standards (2025 Bleeding-Edge)

**Animation is a core differentiator. These are NON-NEGOTIABLE:**

1. **Spring Physics**
   - Use `Spring.DampingRatioMediumBouncy` (0.75) with `Spring.StiffnessLow` (200) for premium feel
   - Velocity preservation: animations must inherit gesture velocity
   - No linear easing - always physics-based springs

2. **visionOS/iOS 18-Inspired Motion**
   - Micro-interactions on every state change (scale, fade, translate)
   - Parallax effects on scrollable content
   - Spatial depth cues (z-axis animation, shadow changes)
   - Frosted glass transparency effects (NOT liquid glass/neon)

3. **Gesture-Driven Animation**
   - Use `VelocityTracker` for fling animations
   - `animateDecay` for natural deceleration
   - Interruptible animations that respect user input

4. **Performance**
   - 60fps minimum, 120fps target on capable devices
   - Use `graphicsLayer` for GPU-accelerated transforms
   - Avoid recomposition during animation

5. **Apple-Quality Checklist**
   - Does it feel "alive" and responsive?
   - Would it look at home in iOS 18 or visionOS?
   - Is the timing natural, not mechanical?
   - Does it respond to gesture velocity?

**Reference Values:**
```kotlin
// Premium spring - smooth with subtle overshoot
spring(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessLow
)

// Clean, no-bounce transitions
spring(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMedium
)

// Playful, bouncy interactions
spring(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessLow
)
```

**Sources to Research:**
- Apple WWDC23: "Animate with springs"
- Android Developers: Compose Animation December 2025 release
- visionOS design guidelines for spatial UI

### The Bottom Line

**If you wouldn't be proud to show it to an Apple designer, it's not done.**

---

## INTERACTION DESIGN LAWS

### Universal Gesture Vocabulary
| Gesture | Behavior | Notes |
|---------|----------|-------|
| **TAP** | Select / Open | Universal expectation |
| **LONG PRESS** | Context menu | Platform standard |
| **SWIPE DOWN** | Dismiss / Close | Natural gravity |
| **SWIPE UP** | Expand / See more | Progressive disclosure |

**Novel gestures (swipe left/right) are opt-in only, never the primary way to do something.**

### The 2-Tap Principle
Primary actions require maximum 2 taps from any screen:
- **Tap 1:** Confirm selection (user sees what they're acting on)
- **Tap 2:** Confirm action (user intentionally triggers it)

### Consistency Rule
**Same action = Same result. Always.**
- If tapping a book shows a half-sheet in Library, it shows a half-sheet everywhere
- If long-press opens a menu in one place, it opens a menu everywhere

### Pre-Implementation Checklist
Before implementing ANY interaction:
- [ ] Invisible Design Test: Users can accomplish without thinking
- [ ] Uses only Universal Gestures (or is opt-in)
- [ ] Primary action reachable in ≤2 taps
- [ ] Consistent with same action elsewhere
- [ ] First-time user would understand immediately

---

## TECHNOLOGY STANDARDS

### Performance Targets
- **Cold start:** < 1.5 seconds
- **Frame rate:** 60fps minimum, 120fps target
- **Memory:** No leaks, aggressive cleanup
- **Battery:** Background-optimized, respect Doze

### Architecture
- **Pattern:** MVVM + Clean Architecture
- **DI:** Hilt with KSP
- **Async:** Coroutines + Flow (no callbacks)
- **State:** Unidirectional with StateFlow
- **Navigation:** Type-safe Kotlin Serialization

### Technology Choices
| Prefer | Over |
|--------|------|
| Jetpack Compose | XML layouts |
| Material 3 | Material 2 |
| Kotlin | Java |
| Coroutines | RxJava |
| Room | Raw SQLite |
| Coil | Glide/Picasso |

---

## QUALITY GATES

**Every change must pass ALL gates before shipping.**

### Gate 1: Functional
- Works as intended (not just compiles)
- No regressions
- Edge cases handled
- Tested on real device

### Gate 2: User Experience
- Passes Invisible Design Test
- Uses Universal Gestures only
- 2-tap maximum for primary actions
- Consistent behavior everywhere

### Gate 3: Visual
- Matches Lithos Amber aesthetic
- Uses theme colors (not hardcoded)
- Proper spacing/alignment
- Works in dark AND light mode
- 48dp minimum touch targets

### Gate 4: Error Handling
- Graceful file not found
- Clear permission explanations
- Offline-friendly with retry
- No crashes from bad data

### Gate 5: Code Quality
- No TODO comments (complete or track)
- No dead code
- Proper error logging
- Resource cleanup
- Null safety

### The Ship-It Standard
A change is ready when:
1. All 5 gates pass
2. Tested on real device
3. Multiple scenarios verified
4. Looks premium
5. Feels right

**Philosophy: Better to ship nothing than ship broken. Quality is non-negotiable.**

---

## FEATURE REFERENCE

### Audio Playback
- ExoPlayer/Media3 gapless playback
- Speed control 0.5x - 2.0x with presets
- Sleep timer (presets + end of chapter + custom)
- Shake to extend sleep timer
- Smart auto-rewind (based on pause duration)
- Audio focus management
- Background playback with notification
- Lock screen controls

### Audio Effects
- 10-band equalizer with 20+ presets
- Bass boost, virtualizer, reverb
- Loudness normalization (-14 LUFS)
- Stereo balance control
- Custom preset saving

### Library
- **Audio:** M4B, MP3, M4A, OGG, OPUS, FLAC, WAV
- **Text:** EPUB, PDF, DOCX, DOC, TXT
- Grid/List/Recents views
- Filter by status, sort by author/title/series
- Folder scanning with import
- Progress tracking per book
- Split book into chapters (M4B/M4A/MP4 lossless)

### Smart Cover Art System
- **Aspect Ratio Detection:** Identifies non-square covers (< 0.9 or > 1.1 ratio)
- **Web Image Search:** DuckDuckGo Images API for high-quality cover discovery
- **Multi-Source Fallback:** Web → Google Books → OpenLibrary
- **Auto-Replacement:** Background process replaces non-square covers with square versions
- **Local Storage:** All covers downloaded and stored locally (no hotlinking)
- **Efficient Detection:** BitmapFactory bounds-only decoding, partial HTTP downloads for remote images

### Enhanced Metadata Parsing
- **Multi-Pattern Filename Parsing:**
  - `Author - Title (Series #1)` - Full extraction
  - `Author - Series Book 1 - Title` - Three-part with book number
  - `Title by Author` - Simple "by" separator
  - `Author - Title` - Heuristic name detection
  - Folder structure parsing for author/series
- **Embedded Tag Intelligence:**
  - METADATA_KEY_ALBUMARTIST → Author (preferred)
  - METADATA_KEY_ALBUM → Series
  - ARTIST vs ALBUMARTIST → Narrator detection
- **Source Merging:** Embedded data preferred, filename fills gaps
- **Book Number Extraction:** #1, Book 1, Volume 2, Part 3, 3 of 10 patterns

### Chapters & Bookmarks
- Auto-extraction from all audio formats
- Manual chapter editor
- Bookmark with notes
- Auto-bookmark after 30s pause

### Text-to-Speech
- System TTS (device voices)
- Kokoro AI TTS (Sherpa-ONNX, local, high-quality)
- 10 voice options with preview
- Pre-generation with caching (no lag)
- Front matter skip
- Preference persistence

### Series Management
- Auto-detection from metadata
- Intelligent title parsing
- Series grouping and progress tracking
- Fractional numbering (1.5)

### Cloud Storage
- Google Drive integration
- Dropbox integration
- File browsing and download
- **Auto-connect:** Automatically reconnects when returning to cloud browser
- **"Shared with me" folder:** Access files shared by others in Google Drive
- **Incompatible file visibility:** Non-importable files shown grayed out with "Unsupported" label
- **Smart filtering:** Block icon for unsupported formats, download icon for compatible files

### Torrents
- Magnet link handling
- .torrent file import
- Queue management
- Speed limiting
- Wi-Fi only mode

### Document Reader
- EPUB 2/3 with full formatting
- PDF rendering
- DOCX/DOC/TXT support
- Font/spacing customization
- Page navigation
- Reading themes (Paper, Sepia, Amber, Night, Dark Gray)
- Quote sharing with attribution
- Double-tap sentence navigation (TTS)

### Android Auto
- Media library browsing
- Playback controls
- Artwork display

### UI/UX
- Lithos Amber design language
- Material Design 3
- Dark/Light/System themes
- Dynamic colors from cover
- Haptic feedback
- 12 languages

### Other
- Kids Mode with PIN
- Listening statistics
- Metadata from Google Books / OpenLibrary / Web Search
- Smart cover art replacement (auto-detects and replaces non-square covers)
- Enhanced metadata parsing (multi-pattern filename + embedded tag extraction)

---

## ACTIVE FIXES REQUIRED

### Theme Consistency Issues (Priority: CRITICAL)
- [x] Player screen: Dark mode looks like OLED black - needs proper Slate (#1A1D21) ✓ FIXED
- [x] eReader: Not using Lithos theme system - must inherit main app theme ✓ FIXED (APP_DEFAULT theme)
- [x] Now page: Dark mode appears OLED black instead of Slate ✓ FIXED
- [x] Book Details slide-up: Using old UI colors, not Lithos palette ✓ FIXED (LithosSlate, LithosSurfaceDark)
- [x] Share window: Using old UI colors, not Lithos palette ✓ FIXED (LithosAmber accent)
- [x] Blue icons/text in popups: Must be Amber (#D48C2C) ✓ FIXED (EditBookScreen, LithosDialog)

### UI Behavior Issues (Priority: HIGH)
- [x] eReader Sleep Timer: Center popup → slide-up bottom sheet ✓ FIXED (ModalBottomSheet)
- [x] eReader Chapters/Bookmarks: Center popup → slide-up bottom sheet ✓ FIXED (ModalBottomSheet)
- [x] Highlight colors: Brown/orange boxes → use neutral slate for selection background ✓ FIXED (LithosColors.selection)
- [x] Selected text/icons: Must be Amber, not orange/brown variants ✓ Using LithosAmber
- [ ] **HeroCardStack snap-back animation**: Now page depth carousel has slight stutter when returning to position. Needs buttery smooth spring animation matching Apple-quality standards. Current implementation uses snapshotFlow monitoring but still has micro-stutter before final settle. (HeroCardStack.kt)

### Design Polish Issues (Priority: MEDIUM)
- [x] Moss green (#4A5D45): Darken by 1-2 shades for better contrast ✓ FIXED (#3D4F39)
- [x] Progress ring thickness: Increase slightly in Player screen ✓ FIXED (4dp)
- [ ] Settings page: Redesign to match Profile page style (icons, layout)
- [ ] Now page stats: Redesign - current feels childish, basic color palette

### Feature Enhancements (Priority: LOW)
- [ ] Achievements/Badges: Robust system like Audible with meaningful milestones
- [x] Theme persistence: Save selected theme to DataStore ✓ FIXED (ThemeViewModel + SettingsRepository)
- [ ] Theme picker UI: Settings with live preview

---

## KNOWN GAPS

### Not Yet Working
- Crash reporting (needs Firebase/Sentry)
- Play Billing integration

### Proposed Future
- AI chapter summaries
- Sleep detection (auto-pause)
- Voice control
- Book clubs

---

## UNIQUE DIFFERENTIATORS

1. **Dual TTS Engine** - System + Sherpa-ONNX AI voices
2. **Professional 10-Band EQ** - Studio-quality audio
3. **Torrent Integration** - Seamless content acquisition
4. **Lithos Amber UI** - Premium tactile visual identity
5. **Series Intelligence** - Automatic organization
6. **Comprehensive Stats** - Gamified listening
7. **Smart Cover Art** - Auto-detects non-square covers and replaces with high-quality square versions
8. **Enhanced Metadata Parsing** - Multi-pattern filename parsing + embedded tag intelligence

---

## FILE STRUCTURE

```
app/src/main/java/com/mossglen/lithos/
├── data/           # Repositories, DAOs, database
├── tts/            # TTS engines, voice models
├── ui/
│   ├── screens/    # All screen composables
│   ├── viewmodel/  # ViewModels
│   ├── components/ # Reusable UI components
│   └── theme/      # Lithos Amber theme system
├── service/        # Background services
└── util/           # Utilities, extensions
```

---

## QUICK REFERENCE

| Metric | Value |
|--------|-------|
| Screens | 26 |
| ViewModels | 11 |
| Kotlin Files | 99 |
| Audio Formats | 7 |
| Text Formats | 5 |
| TTS Engines | 2 |
| Languages | 12 |
| Cloud Providers | 2 |

---

*This manifest defines expectations. All implementations must meet these standards.*

---

## LITHOS AMBER COLOR REFERENCE (Quick Copy)

```kotlin
// Primary Accent - Amber
val LithosAmber = Color(0xFFD48C2C)

// Primary Action - Moss
val LithosMoss = Color(0xFF4A5D45)

// Backgrounds
val LithosSlate = Color(0xFF1A1D21)      // Standard Dark
val LithosOat = Color(0xFFF2F0E9)        // Reader Light
val LithosBlack = Color(0xFF000000)      // OLED Night

// Supporting
val LithosAmberLight = Color(0xFFE6A84D)
val LithosAmberDark = Color(0xFFB57420)
val LithosMossLight = Color(0xFF5C7356)
val LithosMossDark = Color(0xFF3A4A36)

// Glass Effects
val LithosGlass = Color(0xD91A1D21)       // rgba(26, 29, 33, 0.85)
val LithosGlassBorder = Color(0x1AFFFFFF) // rgba(255, 255, 255, 0.1)
```
