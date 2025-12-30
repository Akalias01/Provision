# PROJECT MANIFEST: REVERIE

**App:** REVERIE | **Studio:** MOSSGLEN | **Package:** com.mossglen.reverie
**Architecture:** MVVM + Clean Architecture + Jetpack Compose + Media3
**Platform:** Android 10+ (API 29) | Target: Android 16 (API 36)

---

## VISION

REVERIE is a bleeding-edge audiobook and e-book platform that sets the industry standard. Every interaction should feel effortless, every feature should anticipate user needs, and every pixel should demonstrate premium craftsmanship.

**MANDATE:**
1. Lead the market in innovation
2. Execute every feature to the highest standard
3. Feel native to premium devices (Galaxy S/Z, Pixel Pro)
4. Push boundaries while maintaining intuitive simplicity

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
- Matches glass morphism aesthetic
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

### Android Auto
- Media library browsing
- Playback controls
- Artwork display

### UI/UX
- Glassmorphism design
- Material Design 3
- Dark/Light/System themes
- Dynamic colors from cover
- Haptic feedback
- 12 languages

### Other
- Kids Mode with PIN
- Listening statistics
- Metadata from Google Books / OpenLibrary

---

## KNOWN GAPS

### Not Yet Working
- Split/Merge Books
- Reading mode themes (Amber/Sepia)
- Crash reporting (needs Firebase/Sentry)
- Play Billing integration

### Proposed Future
- AI chapter summaries
- Sleep detection (auto-pause)
- Voice control
- Quote sharing
- Book clubs

---

## UNIQUE DIFFERENTIATORS

1. **Dual TTS Engine** - System + Sherpa-ONNX AI voices
2. **Professional 10-Band EQ** - Studio-quality audio
3. **Torrent Integration** - Seamless content acquisition
4. **Glass Morphism UI** - Premium visual identity
5. **Series Intelligence** - Automatic organization
6. **Comprehensive Stats** - Gamified listening

---

## FILE STRUCTURE

```
app/src/main/java/com/mossglen/reverie/
├── data/           # Repositories, DAOs, database
├── tts/            # TTS engines, voice models
├── ui/
│   ├── screens/    # All screen composables
│   ├── viewmodel/  # ViewModels
│   ├── components/ # Reusable UI components
│   └── theme/      # Colors, typography, shapes
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
