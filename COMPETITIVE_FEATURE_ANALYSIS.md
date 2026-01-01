# Competitive Feature Analysis: Ebook Reader Apps

## Executive Summary

This document analyzes the key features of the top three ebook reading platforms (Kindle, Apple Books, Google Play Books) to identify features REVERIE should implement to be competitive in the market.

---

## 1. Text Selection and Highlighting

### Kindle
- Multi-color highlighting (yellow, blue, pink, orange)
- Tap and drag to select text
- Highlights sync across all devices via Whispersync
- **Popular Highlights** - Shows passages other readers highlighted most
- Highlights automatically pause Assistive Reader (TTS) when created

### Apple Books
- Five highlight colors (yellow, green, blue, pink, purple)
- Underline option as alternative annotation style
- Select text by tap-and-hold, drag to extend
- Highlights appear in dedicated "Notes" tab in table of contents
- iCloud sync across all Apple devices

### Google Play Books
- Multiple highlight colors available
- Touch and hold to select, blue sliders to extend selection
- **Annotation Collections** - Organize highlights into custom groups
- Highlights sync across devices automatically

### REVERIE Should Implement
- [ ] **Multi-color highlighting** (minimum 5 colors: yellow, green, blue, pink, orange)
- [ ] **Underline option** as alternative to highlighting
- [ ] **Tap-and-hold selection** with drag handles for extension
- [ ] **Highlight sync** across devices (if implementing cloud sync)
- [ ] **Popular Highlights** feature (show community-highlighted passages)
- [ ] **Highlight collections/folders** for organization

---

## 2. Note-Taking and Annotations

### Kindle
- Add notes to any highlighted passage
- Notes appear as small icons in margin
- Notes sync via Whispersync
- Can edit notes directly on Goodreads (syncs back to Kindle)
- Annotations auto-pause TTS when editing

### Apple Books
- Attach notes to any highlight (colored squares in margin)
- Notes viewable in dedicated "Notes" section
- Can select multiple highlights and bulk-share/export
- Notes sync via iCloud

### Google Play Books
- Add notes to any bookmark or highlight
- **Annotation Collections** - Create named collections for organization
- **Share Collections** - Share annotation sets with others via link
- **Export Annotations** - Export to file with citation style options
- **Google Drive Sync** - Notes saved to Drive as documents per book

### REVERIE Should Implement
- [ ] **Attach notes to highlights** (mandatory)
- [ ] **Margin indicators** for notes (subtle icons)
- [ ] **Dedicated notes view** showing all annotations per book
- [ ] **Note collections/folders** for organization
- [ ] **Export annotations** (Markdown, PDF, plain text formats)
- [ ] **Share annotation collections** via link
- [ ] **Cloud sync for notes** (Google Drive integration exists)

---

## 3. Text-to-Speech Features

### Kindle
- **Assistive Reader** - New TTS with synchronized word highlighting
- Underlines words as they're read (unique feature)
- Speed control with adjustable playback rate
- 30-second rewind button
- Auto-pause when opening menus or creating annotations
- Requires Bluetooth for e-readers (no speakers)
- Works offline
- Available on 11th gen Kindle and newer (2021+)
- Only works with Enhanced Typesetting books

### Apple Books
- Uses iOS VoiceOver for TTS
- Integrates with system accessibility features
- Read-along support in some books

### Google Play Books
- **Read Aloud** feature with natural voice option
- Auto-start option (TTS begins when book opens)
- Speed control (speech rate adjustment)
- Uses device default TTS engine
- Natural voice requires internet connection
- Works with VoiceOver on iOS
- Not available on all books (publisher controlled)

### REVERIE Current State
- System TTS (device voices)
- Kokoro AI TTS (Sherpa-ONNX, local, high-quality)
- 10 voice options with preview
- Pre-generation with caching

### REVERIE Should Implement
- [ ] **Synchronized word/sentence highlighting** during TTS (like Kindle's Assistive Reader)
- [ ] **Auto-pause on interaction** (when user opens menu, creates note, etc.)
- [ ] **30-second rewind** button in TTS controls
- [ ] **Auto-start TTS option** (begin reading when book opens)
- [ ] **Reading position tracking** during TTS (exact word position)
- [x] Speed control (already implemented)
- [x] Multiple voice options (already implemented)
- [x] Offline support (already implemented with Kokoro)

---

## 4. Reading Progress Tracking

### Kindle
- **Whispersync** - Tracks position down to individual sentences
- Progress syncs across all Kindle devices and apps
- Shows time remaining in chapter/book
- "Reading Insights" with reading speed data
- **Whispersync for Voice** - Syncs between ebook and audiobook

### Apple Books
- **Daily Reading Goals** - Set minutes per day (default 5 min)
- **Yearly Reading Goals** - Books per year (default 3 books)
- **Reading Streaks** - Consecutive days of reaching goal
- Progress counter in Home tab
- **Coaching notifications** for encouragement
- PDFs can optionally count toward goals
- Sync via iCloud
- **Share progress** as image for social sharing
- Only counts books read in Apple Books (not physical/other apps)

### Google Play Books
- Reading position syncs across devices
- Progress bar showing completion percentage
- Page count and estimated time remaining

### REVERIE Current State
- Progress tracking per book
- Listening statistics (gamified)

### REVERIE Should Implement
- [ ] **Daily reading/listening goals** (configurable minutes)
- [ ] **Yearly book goals** (configurable count)
- [ ] **Reading/listening streaks** with notifications
- [ ] **Time remaining estimates** (chapter and book level)
- [ ] **Cross-format sync** (audiobook + ebook position for same title)
- [ ] **Progress sharing** as shareable image/card
- [ ] **Reading speed insights** (words/pages per minute)
- [ ] **Coaching/motivational notifications**
- [x] Progress tracking per book (already implemented)
- [x] Listening statistics (already implemented)

---

## 5. Search Functionality

### Kindle
- Full-text search within book
- Search entire library
- **X-Ray** - Quick reference for characters, places, terms
- X-Ray shows Notable clips, People, Terms, Images tabs
- Wikipedia lookup integration
- Search in notes and highlights

### Apple Books
- Search within current book
- Search across library
- Search in notes and bookmarks

### Google Play Books
- Full-text search within book
- Search across library
- Dictionary integration for single words
- **Ask Gemini** (upcoming) - AI-powered contextual search and summaries

### REVERIE Should Implement
- [ ] **Full-text search** within book (mandatory)
- [ ] **Library-wide search** across all books
- [ ] **Search in annotations** (notes and highlights)
- [ ] **X-Ray style feature** - Character/place reference guide
- [ ] **AI-powered search** (summaries, context, explanations)
- [ ] **Search history** with recent queries

---

## 6. Font and Theme Customization

### Kindle
- **Font options**: Bookerly, Amazon Ember, Baskerville, Futura, Caecilla, OpenDyslexic, Helvetica, Palatino
- Font size and boldness controls
- Margin adjustment
- Line spacing control
- Text alignment options
- **Custom Themes** - Save font/layout preferences as named presets
- 4 built-in themes: Large, Standard, Compact, Low Vision
- **Dark Mode** - System-wide inverted colors
- Page color options (including dark)

### Apple Books
- Multiple font families
- Font size slider
- **6 Themes**: Original, Quiet, Paper, Bold, Calm, Focus
- **Theme customization**: Line spacing, character spacing, word spacing, side margins
- Real-time preview while adjusting
- Dark/Light mode support

### Google Play Books
- Font style selection (multiple typefaces)
- Font size adjustment (pinch to zoom also works)
- Line spacing controls
- Text alignment (Default, Left, Justify)
- Page layout options (Auto adjusts by orientation)
- **Dark Theme** - App-wide and reading view
- **Night Light** - Blue light filter that adjusts with sunset

### REVERIE Current State
- Font/spacing customization
- Dark/Light/System themes
- Dynamic colors from cover

### REVERIE Should Implement
- [ ] **Named theme presets** (save custom configurations)
- [ ] **Built-in theme presets** (Compact, Standard, Large, Low Vision)
- [ ] **OpenDyslexic font** option (accessibility)
- [ ] **Blue light filter/Night mode** (warm amber at night)
- [ ] **Per-book theme memory** (remember settings per book)
- [ ] **Character/word spacing** controls
- [ ] **Margin adjustment** sliders
- [ ] **Text alignment** options (left, justify, centered)
- [x] Font size/style selection (already implemented)
- [x] Dark/Light themes (already implemented)

---

## 7. Dictionary and Lookup Features

### Kindle
- Built-in dictionary (tap any word)
- **Word Wise** - Inline definitions above difficult words
- Word Wise hint density slider (show more/fewer hints)
- Works offline
- **Vocabulary Builder** - Saves looked-up words
- Flashcard-style review of vocabulary
- "Mark as Mastered" for learned words
- Wikipedia lookup
- **Instant Translations** - Translate highlighted text

### Apple Books
- **Look Up** - Dictionary definition via iOS
- Wikipedia summary in lookup
- Web search from selection

### Google Play Books
- Dictionary definition for single words
- **Ask Gemini** (upcoming) - AI explanations for selected text
- Translation via Google Translate integration

### REVERIE Should Implement
- [ ] **Tap-to-define** dictionary lookup (mandatory)
- [ ] **Word Wise mode** - Inline definitions above difficult words
- [ ] **Vocabulary Builder** - Track looked-up words per book
- [ ] **Flashcard review** for vocabulary learning
- [ ] **Translation feature** (integrate Google Translate API)
- [ ] **Wikipedia lookup** integration
- [ ] **Difficulty level hints** (slider for word hint frequency)
- [ ] **Export vocabulary** to Anki or other flashcard apps

---

## 8. Sharing and Exporting Highlights

### Kindle
- Share highlights to Goodreads
- Export notebook as HTML with citation styles
- Share quotes with book link
- **Goodreads integration** - View/edit notes on Goodreads.com
- Highlights sync back to Kindle from Goodreads edits
- Third-party export tools (Chrome extensions, Readwise)

### Apple Books
- Share individual highlights via AirDrop, Mail, Messages
- Add selections to Notes app
- Export all notes as PDF via share menu
- Multi-select highlights for bulk sharing
- Includes link to book in shared content
- Third-party tools for Obsidian, Evernote integration

### Google Play Books
- **Export Annotations** - Export with citation style options
- **Share Collections** - Generate shareable link for annotation sets
- **Google Drive Sync** - Auto-save notes to Drive folder per book
- Annotations saved as documents in "Play Books Notes" folder

### REVERIE Should Implement
- [ ] **Share individual quotes** with book info and cover image
- [ ] **Export all annotations** (Markdown, PDF, HTML, plain text)
- [ ] **Citation style options** (MLA, APA, Chicago, custom)
- [ ] **Shareable annotation links** (for collections)
- [ ] **Google Drive export** (already have integration)
- [ ] **Social media formatted quotes** (pretty images for Instagram/Twitter)
- [ ] **Goodreads integration** for sharing to reading community
- [ ] **Obsidian/Notion export** (popular note-taking apps)

---

## 9. Unique Premium Features

### Kindle Unique Features
- **X-Ray** - Deep book reference (characters, places, terms, images)
- **Whispersync for Voice** - Seamless audiobook/ebook switching
- **Popular Highlights** - Social highlighting (see what others marked)
- **Word Wise** - Inline vocabulary assistance
- **Vocabulary Builder** - Learning tool with flashcards
- **Reading Ruler** - Line-by-line focus tool (accessibility)
- **Kindle Unlimited** - Subscription library (5M+ titles)
- **Page Flip** - Browse without losing place
- **Enhanced Typesetting** - Optimal text layout

### Apple Books Unique Features
- **Reading Goals** - Daily/yearly goals with streaks
- **Coaching** - Motivational notifications
- **Book Store integration** - Seamless purchasing
- **Family Sharing** - Share purchases with family
- **Read Now curated experience** - Personalized recommendations
- **Collections sync** - Organize across all Apple devices

### Google Play Books Unique Features
- **Ask Gemini** (upcoming) - AI-powered insights while reading
- **Annotation Collections** - Organize annotations into shareable groups
- **Google Drive sync** - Notes as documents in Drive
- **Real-time translation** with Google Translate
- **Upload your books** - Read any EPUB/PDF in Play Books
- **Family Library** - Share purchases with family

### Social/Gamification Features (from competitors like Glose)
- Social reading with shared annotations in margins
- Book clubs with group reading
- Reading challenges and competitions
- Leaderboards among friends
- Achievement badges for milestones
- Reading streaks with visual progress

---

## REVERIE Competitive Feature Roadmap

### Priority 1: Essential (Must-Have for Launch)
| Feature | Status | Notes |
|---------|--------|-------|
| Multi-color highlighting | To implement | 5+ colors |
| Notes on highlights | To implement | With margin indicators |
| Full-text search | To implement | Within book |
| Tap-to-define dictionary | To implement | Offline capable |
| Synced word highlighting in TTS | To implement | Like Kindle Assistive Reader |
| Named theme presets | To implement | Save custom configs |
| Export annotations | To implement | Markdown, PDF |

### Priority 2: Competitive Advantage
| Feature | Status | Notes |
|---------|--------|-------|
| Reading goals (daily/yearly) | To implement | With streaks |
| X-Ray style reference | To implement | Characters, places, terms |
| Vocabulary Builder | To implement | With flashcards |
| Word Wise mode | To implement | Inline definitions |
| Popular Highlights | To implement | Community feature |
| Quote sharing (pretty images) | To implement | Social media ready |
| Cross-format position sync | To implement | Audiobook + ebook |

### Priority 3: Differentiation (Innovation)
| Feature | Status | Notes |
|---------|--------|-------|
| AI-powered search/summaries | Proposed | Like Gemini integration |
| Book clubs | Proposed | Social reading groups |
| Reading challenges | To implement | Gamification |
| Sleep detection | Proposed | Auto-pause when asleep |
| Voice control | Proposed | Hands-free navigation |
| Obsidian/Notion export | To implement | Popular integrations |
| Annotation collections | To implement | Shareable groups |

### Already Implemented in REVERIE
| Feature | Competitive Position |
|---------|---------------------|
| Dual TTS Engine (System + Kokoro AI) | Industry leading |
| 10-Band Professional EQ | Unique differentiator |
| Torrent Integration | Unique differentiator |
| Glass Morphism UI | Premium positioning |
| Series Intelligence | Competitive |
| Listening Statistics | Competitive |
| Google Drive/Dropbox Integration | Competitive |

---

## Feature Comparison Matrix

| Feature | Kindle | Apple Books | Google Play | REVERIE (Current) | REVERIE (Target) |
|---------|--------|-------------|-------------|-------------------|------------------|
| Multi-color highlights | Yes (4) | Yes (5) | Yes | No | Yes (5+) |
| Notes on highlights | Yes | Yes | Yes | No | Yes |
| Synced TTS highlighting | Yes | No | No | No | Yes |
| Reading goals/streaks | Partial | Yes | No | Partial | Yes |
| X-Ray reference | Yes | No | No | No | Yes |
| Vocabulary Builder | Yes | No | No | No | Yes |
| Word Wise | Yes | No | No | No | Yes |
| Popular Highlights | Yes | No | No | No | Yes |
| AI search/summaries | No | No | Coming | No | Yes |
| Export annotations | Yes | Partial | Yes | No | Yes |
| Dual TTS engines | No | No | No | Yes | Yes |
| Professional EQ | No | No | No | Yes | Yes |
| Torrent support | No | No | No | Yes | Yes |

---

## Conclusion

REVERIE already has several unique differentiators (Dual TTS, Professional EQ, Torrent integration) that position it as a power-user focused app. To compete with the mainstream players, the priority should be:

1. **Text interaction fundamentals** (highlighting, notes, dictionary)
2. **TTS enhancement** (synchronized highlighting during playback)
3. **Reading analytics and gamification** (goals, streaks, vocabulary)
4. **Social features** (popular highlights, quote sharing)
5. **AI integration** (smart search, summaries)

The combination of REVERIE's existing unique features with these competitive features would create a truly differentiated product that serves both casual readers and power users.

---

## Sources

### Kindle Features
- [Kindle's New Assistive Reader](https://blog.the-ebook-reader.com/2025/08/27/kindles-new-assistive-reader-text-to-speech-video-demo/)
- [Amazon Kindle Accessibility Features](https://www.aboutamazon.com/news/books-and-authors/kindle-accessibility-features-for-all-readers)
- [X-Ray is Your Kindle's Best-Kept Secret](https://www.pocket-lint.com/what-is-x-ray-on-kindle/)
- [How to Use Kindle Whispersync](https://casesocietyco.com/blogs/news/how-to-use-kindle-whispersync-in-2025-5-pro-tips-to-transform-your-reading-experience)
- [Kindle Export Highlights](https://umatechnology.org/how-to-export-kindle-notes-and-highlights/)
- [Goodreads Kindle Notes Integration](https://www.goodreads.com/blog/show/1084-take-note-it-s-now-even-easier-to-share-your-kindle-notes-and-highlight)

### Apple Books Features
- [Set Reading Goals in Apple Books](https://support.apple.com/guide/iphone/set-reading-goals-iph6013e96f4/ios)
- [Change Book Appearance in Apple Books](https://support.apple.com/guide/books/change-a-books-appearance-ibks8923126d/mac)
- [How to Use Themes in Apple Books](https://www.idownloadblog.com/2022/09/21/how-to-use-themes-in-books-app-on-ipad-iphone/)
- [Export Highlights from Apple Books](https://nativespeak.net/how-to-export-book-highlights-from-apple-book-app/)
- [Set Up iCloud for Apple Books](https://support.apple.com/guide/icloud/set-up-books-mm3941ae3362/icloud)

### Google Play Books Features
- [Add Bookmarks, Notes & Highlights](https://support.google.com/googleplay/answer/3165868?hl=en&co=GENIE.Platform%3DAndroid)
- [Change Font Size, Color & More](https://support.google.com/googleplay/answer/9755756?hl=en&co=GENIE.Platform%3DAndroid)
- [Hear Books Read Aloud](https://support.google.com/googleplay/answer/11938821?hl=en&co=GENIE.Platform%3DAndroid)
- [Gemini Integration Coming to Play Books](https://www.androidauthority.com/google-play-books-ask-gemini-apk-teardown-3624252/)

### Gamification & Social Reading
- [How to Gamify Your Reading App](https://fgfactory.com/10-gamification-mechanics-to-supercharge-your-reading-app)
- [Glose Social Reading App](https://www.bookrunch.com/overview/gamified_reading_apps/)
- [Top Social Reading Apps](https://www.bookrunch.com/overview/social_reading_apps/)
