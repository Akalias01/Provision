# Android Auto Architecture - REZON8

## System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                         ANDROID AUTO HEAD UNIT                       │
│                    (Car Display or Phone Screen)                     │
└────────────────────┬────────────────────────────────────────────────┘
                     │
                     │ Media Browse/Control Protocol
                     │
        ┌────────────┴─────────────┐
        │                          │
        ▼                          ▼
┌──────────────────┐      ┌──────────────────┐
│  Modern API      │      │  Legacy API      │
│  (Auto 6.0+)     │      │  (Auto 5.x)      │
└────────┬─────────┘      └────────┬─────────┘
         │                         │
         │                         │
┌────────▼─────────────────────────▼──────────────────────────────────┐
│                          REZON8 APP                                  │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │              SERVICE LAYER (2 Services)                        │  │
│  │                                                                │  │
│  │  ┌─────────────────────────┐  ┌─────────────────────────┐     │  │
│  │  │ RezonPlaybackService    │  │ RezonLegacyBrowserService│    │  │
│  │  │ (MediaLibraryService)   │  │ (MediaBrowserServiceCompat)│  │  │
│  │  │                         │  │                         │     │  │
│  │  │ • Media3 API            │  │ • Support Library API   │     │  │
│  │  │ • Modern MediaSession   │  │ • Legacy MediaSession   │     │  │
│  │  │ • onGetLibraryRoot()    │  │ • onGetRoot()           │     │  │
│  │  │ • onGetChildren()       │  │ • onLoadChildren()      │     │  │
│  │  │ • Content Style Hints   │  │ • Content Style Hints   │     │  │
│  │  │ • Root Hints Logging    │  │ • Root Hints Logging    │     │  │
│  │  └────────────┬────────────┘  └────────────┬────────────┘     │  │
│  │               │                            │                  │  │
│  │               └────────────┬───────────────┘                  │  │
│  │                            │                                  │  │
│  └────────────────────────────┼──────────────────────────────────┘  │
│                               │                                     │
│  ┌────────────────────────────▼──────────────────────────────────┐  │
│  │              SHARED RESOURCES (via Hilt DI)                    │  │
│  │                                                                │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐        │  │
│  │  │  ExoPlayer   │  │  Repository  │  │ AudioHandler │        │  │
│  │  │  (Singleton) │  │  (Singleton) │  │  (Singleton) │        │  │
│  │  └──────────────┘  └──────────────┘  └──────────────┘        │  │
│  │                                                                │  │
│  └────────────────────────────────────────────────────────────────┘  │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │                    DATA LAYER                                  │  │
│  │                                                                │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐        │  │
│  │  │  Room DB     │  │  DataStore   │  │  Media Files │        │  │
│  │  │  (Books)     │  │  (Prefs)     │  │  (Audio)     │        │  │
│  │  └──────────────┘  └──────────────┘  └──────────────┘        │  │
│  │                                                                │  │
│  └────────────────────────────────────────────────────────────────┘  │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

## Service Discovery Flow

```
┌────────────────────────────────────────────────────────────────────┐
│                    1. Android Auto Starts                          │
│                  Scans for Media Apps                              │
└─────────────────────────┬──────────────────────────────────────────┘
                          │
                          ▼
┌────────────────────────────────────────────────────────────────────┐
│              2. Package Manager Queries                            │
│   Looking for services with intent filters:                        │
│   • androidx.media3.session.MediaLibraryService                    │
│   • android.media.browse.MediaBrowserService                       │
│   • android.media.action.MEDIA_PLAY_FROM_SEARCH                    │
│   • android.intent.action.MEDIA_BUTTON                             │
└─────────────────────────┬──────────────────────────────────────────┘
                          │
                          ▼
┌────────────────────────────────────────────────────────────────────┐
│              3. Finds REZON8 Services                              │
│   ✓ RezonPlaybackService (Media3)                                 │
│   ✓ RezonLegacyBrowserService (Support Library)                   │
└─────────────────────────┬──────────────────────────────────────────┘
                          │
                          ▼
┌────────────────────────────────────────────────────────────────────┐
│              4. Checks MediaSession Active State                   │
│   Query: dumpsys media_session                                     │
│   Looking for: active=true                                         │
└─────────────────────────┬──────────────────────────────────────────┘
                          │
                          ▼
┌────────────────────────────────────────────────────────────────────┐
│              5. REZON8 Added to Available Apps                     │
│   Appears in "Customize launcher" list                             │
└────────────────────────────────────────────────────────────────────┘
```

---

## Connection Flow (When User Opens REZON8)

```
┌─────────────┐
│ Android Auto│
│  Connects   │
└──────┬──────┘
       │
       │ 1. Connect to service
       ▼
┌──────────────────────┐
│  Service binds       │
│  • Modern or Legacy  │
│  • Based on Auto ver │
└──────┬───────────────┘
       │
       │ 2. Request root
       ▼
┌──────────────────────────────────────┐
│  onGetLibraryRoot() / onGetRoot()    │
│  • Log incoming hints                │
│  • Check EXTRA_RECENT, etc.          │
│  • Return root with content hints    │
└──────┬───────────────────────────────┘
       │
       │ 3. Root returned
       ▼
┌──────────────────────────────────────┐
│  Auto displays "REZON8" folder       │
│  • Shows app icon                    │
│  • Shows app name                    │
└──────┬───────────────────────────────┘
       │
       │ 4. User taps REZON8
       ▼
┌──────────────────────────────────────┐
│  onGetChildren("root")               │
│  • Fetch from repository             │
│  • Return 3 folders:                 │
│    - Continue Listening              │
│    - Recent Books                    │
│    - Library                         │
└──────┬───────────────────────────────┘
       │
       │ 5. Folders displayed
       ▼
┌──────────────────────────────────────┐
│  User browses into folder            │
└──────┬───────────────────────────────┘
       │
       │ 6. Request folder content
       ▼
┌──────────────────────────────────────┐
│  onGetChildren("continue_listening") │
│  • Fetch recent book from repo       │
│  • Return playable MediaItem         │
└──────┬───────────────────────────────┘
       │
       │ 7. Books displayed
       ▼
┌──────────────────────────────────────┐
│  User selects book                   │
└──────┬───────────────────────────────┘
       │
       │ 8. Playback request
       ▼
┌──────────────────────────────────────┐
│  onAddMediaItems()                   │
│  • Load book via AudioHandler        │
│  • Return playable MediaItem         │
└──────┬───────────────────────────────┘
       │
       │ 9. Start playback
       ▼
┌──────────────────────────────────────┐
│  ExoPlayer plays audio               │
│  • MediaSession broadcasts state     │
│  • Auto displays Now Playing         │
└──────────────────────────────────────┘
```

---

## Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                    ANDROID AUTO REQUEST                             │
└────────────────────────┬────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────────┐
│              SERVICE LAYER (Entry Point)                            │
│  • Receives request from Auto                                       │
│  • Logs diagnostics                                                 │
│  • Validates parameters                                             │
└────────────────────────┬────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────────┐
│              REPOSITORY LAYER                                       │
│  • getBooksForAuto()          Returns all books as MediaItems       │
│  • getMostRecentBookDirect()  Returns last played book              │
│  • getBookById(id)            Returns specific book                 │
└────────────────────────┬────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────────┐
│              DATABASE LAYER (Room)                                  │
│  • Query books table                                                │
│  • Join with playback state                                         │
│  • Sort by lastPlayedAt                                             │
└────────────────────────┬────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────────┐
│              TRANSFORMATION                                         │
│  • Book entity → MediaItem                                          │
│  • Add metadata (title, author, artwork)                            │
│  • Set browsable/playable flags                                     │
│  • Add content style hints                                          │
└────────────────────────┬────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────────┐
│              RETURN TO AUTO                                         │
│  • ImmutableList<MediaItem> (Media3)                                │
│  • List<MediaBrowserCompat.MediaItem> (Legacy)                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Intent Filter Hierarchy

```
AndroidManifest.xml
│
├── RezonPlaybackService (Primary - Media3)
│   │
│   ├── Intent Filter #1 (Core Media Services)
│   │   ├── androidx.media3.session.MediaLibraryService
│   │   └── android.media.browse.MediaBrowserService
│   │
│   ├── Intent Filter #2 (Voice Search Discovery)
│   │   ├── android.media.action.MEDIA_PLAY_FROM_SEARCH
│   │   └── category: android.intent.category.DEFAULT
│   │
│   └── Intent Filter #3 (Button-based Discovery)
│       └── android.intent.action.MEDIA_BUTTON
│
└── RezonLegacyBrowserService (Fallback - Support Library)
    │
    ├── Intent Filter #1 (Legacy Media Service)
    │   └── android.media.browse.MediaBrowserService
    │
    └── Intent Filter #2 (Legacy Button Support)
        └── android.intent.action.MEDIA_BUTTON
```

---

## Content Hierarchy

```
Root
├── Continue Listening (Folder)
│   └── [Most Recent Book] (Playable)
│       ├── Title: "Book Name"
│       ├── Artist: "Author Name"
│       └── Artwork: Cover URL
│
├── Recent Books (Folder)
│   ├── Book 1 (Playable)
│   ├── Book 2 (Playable)
│   └── Book N (Playable)
│
└── Library (Folder)
    ├── Book 1 (Playable)
    ├── Book 2 (Playable)
    └── Book N (Playable)
```

**Content Style Hints Applied:**
- Folders: Grid layout (BROWSABLE_HINT = 2)
- Books: List layout (PLAYABLE_HINT = 1)

---

## Logging Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                        SERVICE STARTUP                              │
└────────────────────────┬────────────────────────────────────────────┘
                         │
                         ▼
                  Log: "onCreate"
                  Log: "MediaSession created and activated"
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    AUTO CONNECTION REQUEST                          │
└────────────────────────┬────────────────────────────────────────────┘
                         │
                         ▼
              Log: "onGetLibraryRoot called by: [package]"
              Log: "Root hints received:"
              Log: "  [key] = [value]" (for each hint)
              Log: "Root flags - Recent: X, Offline: Y, Suggested: Z"
              Log: "Returning root with content style hints"
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    CONTENT REQUEST                                  │
└────────────────────────┬────────────────────────────────────────────┘
                         │
                         ▼
              Log: "onGetChildren called for parentId: [id]"
              Log: "Returning X items for parentId: [id]"
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    PLAYBACK REQUEST                                 │
└────────────────────────┬────────────────────────────────────────────┘
                         │
                         ▼
              Log: "Playing media item: [mediaId]"
              Log: "Book loaded and seek completed" (from AudioHandler)
```

**Log Tags to Monitor:**
- `RezonPlaybackService` - Modern service activity
- `RezonLegacyBrowserService` - Legacy service activity
- `MediaBrowser` - System MediaBrowser activity
- `MediaSession` - System MediaSession activity
- `CarMediaService` - Android Auto media service

---

## Dependency Injection Graph (Hilt)

```
Application
│
├── ExoPlayerModule
│   └── @Singleton ExoPlayer
│       └── Shared by both services
│
├── RepositoryModule
│   └── @Singleton LibraryRepository
│       ├── Used by RezonPlaybackService
│       └── Used by RezonLegacyBrowserService
│
└── AudioModule
    └── @Singleton AudioHandler
        ├── Wraps ExoPlayer
        └── Provides high-level playback API

Services (Injected via @AndroidEntryPoint)
│
├── RezonPlaybackService
│   ├── @Inject ExoPlayer
│   ├── @Inject LibraryRepository
│   └── @Inject AudioHandler
│
└── RezonLegacyBrowserService
    └── @Inject LibraryRepository
```

**Why This Matters:**
- Both services share same player instance (no duplication)
- Both services read from same database
- Playback state synchronized across services
- Minimal memory overhead

---

## Error Handling Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Request from Android Auto                        │
└────────────────────────┬────────────────────────────────────────────┘
                         │
                         ▼
                 Try to process request
                         │
         ┌───────────────┴───────────────┐
         │                               │
         ▼                               ▼
    [Success]                       [Failure]
         │                               │
         │                               ▼
         │                    Log error with details
         │                               │
         │                               ▼
         │                    Return appropriate result:
         │                    • LibraryResult.ofError()
         │                    • Empty list
         │                    • Null (if allowed)
         │                               │
         └───────────────┬───────────────┘
                         │
                         ▼
              Return result to Auto
```

**Common Error Scenarios:**
1. **Repository returns null** → Return empty list
2. **Database query fails** → Log error, return empty list
3. **Book file not found** → Log warning, skip book
4. **Hilt injection fails** → Service won't start (caught at build time)

---

## Testing Strategy

```
Level 1: Unit Testing
├── Repository
│   ├── getBooksForAuto() returns MediaItems
│   ├── getMostRecentBookDirect() returns Book
│   └── getBookById() returns correct Book
│
└── Service Callbacks
    ├── onGetLibraryRoot() returns root
    ├── onGetChildren("root") returns 3 folders
    └── onGetChildren("library") returns books

Level 2: Integration Testing
├── Service Discovery
│   ├── dumpsys media_session shows REZON8
│   └── dumpsys package shows both services
│
└── Service Connection
    ├── MediaBrowser can connect
    └── Can retrieve root and children

Level 3: Android Auto Testing
├── DHU (Desktop Head Unit)
│   ├── App appears in launcher
│   └── Can browse and play content
│
└── Physical Car
    ├── App appears in launcher customization
    ├── Can browse folders
    └── Playback works correctly

Level 4: Version Testing
├── Android Auto 5.x (Legacy)
│   └── RezonLegacyBrowserService handles
│
└── Android Auto 9.x (Modern)
    └── RezonPlaybackService handles
```

---

## Performance Metrics

### Service Startup Time
```
onCreate() → MediaSession active → Ready for connections
Expected: < 100ms
```

### Root Request Time
```
onGetLibraryRoot() → Return root with hints
Expected: < 50ms (synchronous)
```

### Content Request Time
```
onGetChildren() → Database query → Transform → Return list
Expected: < 200ms for 100 books
```

### Memory Footprint
```
RezonPlaybackService:     ~1MB
RezonLegacyBrowserService: ~100KB
Shared Resources:         ~5MB (ExoPlayer + Repository)
Total Additional:         ~1.1MB for dual-service
```

---

## Version Compatibility Matrix

| Component | Min Version | Target Version | Notes |
|-----------|-------------|----------------|-------|
| Android OS | 10 (API 29) | 13+ (API 33+) | minSdk = 29 |
| Android Auto App | 5.0 | 11.0+ | Legacy service for 5.x |
| Media3 | 1.6.0 | 1.6.0 | Latest stable |
| androidx.media | 1.7.0 | 1.7.0 | Legacy support |
| ExoPlayer | 1.6.0 | 1.6.0 | Via Media3 |

---

## Conclusion

The dual-service architecture provides:
1. **Maximum Compatibility** - Works with all Auto versions
2. **Minimal Overhead** - Shared resources via Hilt
3. **Complete Diagnostics** - Comprehensive logging
4. **Easy Maintenance** - Clear separation of concerns

Both services operate independently but share the same underlying playback engine and data sources, ensuring consistent behavior regardless of which API Android Auto uses to connect.
