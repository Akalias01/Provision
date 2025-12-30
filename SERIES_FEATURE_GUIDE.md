# REZON8 Series Grouping Feature

## Overview

The Series Grouping feature allows users to organize and track their audiobook and ebook series with a premium Glass UI design. This feature integrates seamlessly with the existing REZON8 app architecture.

## Features Implemented

### 1. Series Detection & Parsing
- **Automatic series detection** from the `seriesInfo` field in Book entity
- **Smart parsing** supports multiple common series naming formats:
  - "The Expanse #1"
  - "Harry Potter, Book 1"
  - "Mistborn: Book 2"
  - "The Dark Tower 3"
  - "Foundation Series - 4"
  - Decimal numbering for novellas (e.g., "1.5")

### 2. Series View in Library
- **New view mode** added to LibraryScreenGlass
- **View mode toggle** cycles through: LIST → GRID → RECENTS → **SERIES**
- **Series icon** (AutoStories) indicates series view mode
- **Filtering**: Series view respects both Master Filter (Audio/Read) and tab filters (Not Started/In Progress/Finished/All)

### 3. Series List Display
Two display modes available:

#### Grid View (`SeriesGridView`)
- Compact 2-column grid layout
- Cover collage thumbnails (1-4 book covers)
- Book count badge
- Progress indicator bar
- Progress status text

#### List View (`SeriesListView`)
- Expandable/collapsible cards
- Premium Glass card design
- Detailed series information:
  - Total duration
  - Books count
  - Finished count
  - Next unread book preview
- Smooth expand/collapse animations

### 4. Series Detail Screen
Premium Glass UI screen showing:
- **Cover collage header**:
  - 2x2 grid for 4+ books
  - Row layout for 2-3 books
  - Single cover for 1 book
- **Series statistics card**:
  - Overall progress bar
  - Not Started / In Progress / Finished counts
- **Books list**: All books ordered by series number
- **Quick actions**: Play, navigate to book details
- **Series menu**: Edit, mark all as finished/unread, delete

### 5. Series Management

#### Assign Book to Series Dialog
- Choose from existing series or create new
- Set book number (supports decimals)
- Auto-complete from existing series names

#### Edit Series Dialog
- Rename series
- Reorder books within series
- Change individual book numbers
- Visual list of all books in series

#### Merge Series Dialog
- Merge two series into one
- Preserves book numbering
- Dropdown selection of target series

#### Additional Management Features
- **Auto-fix numbering**: Extracts numbers from book titles
- **Split series**: Move books to new series
- **Mark entire series**: Finished or unread
- **Delete series**: Remove all books in series

### 6. Book Detail Integration
When viewing a book that belongs to a series:
- **Series card** displayed below author
- Shows series name and book number
- **Clickable**: Navigates to Series Detail Screen
- Premium Glass styling with accent color
- Icon indicator (AutoStories)

## Architecture

### Data Models (`Series.kt`)

```kotlin
data class SeriesInfo(
    val name: String,
    val bookNumber: Float?
)

data class Series(
    val name: String,
    val books: List<Book>
)
```

### Extension Functions
- `List<Book>.groupBySeries()`: Groups books into series
- `Book.getSeriesInfo()`: Parses series info from book
- `Book.belongsToSeries()`: Checks if book has series info

### ViewModel (`SeriesViewModel.kt`)
Provides:
- `allSeries`: StateFlow of all series
- `booksWithoutSeries`: StateFlow of standalone books
- `seriesStats`: Statistics for all series
- Management methods: assign, rename, merge, split, reorder

### Database (`BookDao.kt`)
New queries added:
- `getBooksInSeries()`: Flow of books with series info
- `getAllSeriesNames()`: Distinct series names
- `updateSeriesInfo()`: Update book's series
- `renameSeriesForAllBooks()`: Bulk rename

### Navigation (`Routes.kt`)
New route added:
```kotlin
@Serializable
data class SeriesDetail(val seriesName: String)
```

### UI Components

#### Screens
- `SeriesDetailScreen.kt`: Full series detail view
- `SeriesLibraryView.kt`: Grid and list views for library
- Updated `LibraryScreenGlass.kt`: Integrated series view mode
- Updated `BookDetailScreen.kt`: Series info display

#### Dialogs (`SeriesDialogs.kt`)
- `AssignBookToSeriesDialog`: Assign books to series
- `EditSeriesDialog`: Edit series and reorder books
- `MergeSeriesDialog`: Merge series
- `ReorderBookDialog`: Change book number

## Usage Guide

### For Users

#### Viewing Series
1. Open Library screen
2. Tap the view mode icon (top right) until you see the AutoStories icon
3. Browse series in list or grid format
4. Tap a series to see details
5. Tap "expand" arrow in list view for quick stats

#### Assigning Books to Series
1. Long-press a book in library OR open book details menu
2. Select "Assign to Series"
3. Choose existing series or create new
4. Enter book number (optional)
5. Tap "Assign"

#### Managing Series
1. Open a series detail screen
2. Tap the menu icon (three dots)
3. Options available:
   - Edit Series (rename, reorder books)
   - Mark All as Finished
   - Mark All as Unread
   - Delete Series

#### Editing Series
1. From series menu, select "Edit Series"
2. Change series name in text field
3. Tap any book to change its order number
4. Tap "Save" when done

#### Merging Series
1. Open first series detail screen
2. Access series menu (requires custom implementation in navigation)
3. Select "Merge Series"
4. Choose target series
5. Confirm merge

### For Developers

#### Adding Series to Navigation
In your navigation graph, add:

```kotlin
composable<SeriesDetail> { backStackEntry ->
    val args = backStackEntry.toRoute<SeriesDetail>()
    SeriesDetailScreen(
        seriesName = args.seriesName,
        isDark = isDark,
        onBackClick = { navController.navigateUp() },
        onBookClick = { bookId ->
            navController.navigate(BookDetail(bookId))
        },
        onPlayBook = { book -> /* handle play */ },
        onEditSeries = { /* show edit dialog */ }
    )
}
```

#### Accessing Series Data
```kotlin
val seriesViewModel: SeriesViewModel = hiltViewModel()
val allSeries by seriesViewModel.allSeries.collectAsState()
val seriesStats by seriesViewModel.seriesStats.collectAsState()
```

#### Updating LibraryScreenGlass
Make sure to pass the series click handler:

```kotlin
LibraryScreenGlass(
    libraryViewModel = libraryViewModel,
    seriesViewModel = seriesViewModel,
    isDark = isDark,
    onBookClick = { bookId -> /* navigate */ },
    onSeriesClick = { seriesName ->
        navController.navigate(SeriesDetail(seriesName))
    },
    // ... other params
)
```

## Design System Integration

### Glass UI Theme
All series components use the Glass design system:
- Glass cards with blur effects
- Copper accent colors (Rezon Dark mode)
- Smooth spring animations
- Haptic feedback on interactions
- Premium spacing and typography

### Color Scheme
- **Dark mode**: True black background, subtle white glass
- **Rezon Dark**: Deep black, copper accents
- **Light mode**: White background, subtle black glass

### Animations
- Spring-based expand/collapse
- Scale animations on press
- Smooth transitions
- Medium bouncy damping ratio

## Performance Considerations

### Efficient Data Flow
- Uses StateFlow for reactive updates
- Lazy loading with LazyColumn/LazyVerticalGrid
- Filtered at ViewModel level
- Minimal recomposition with stable keys

### Memory Management
- Extension functions for on-demand grouping
- No caching of large data sets
- Flows automatically cleaned up

## Testing Recommendations

### Unit Tests
- Test `SeriesInfo.parse()` with various formats
- Test series grouping logic
- Test book filtering by series

### Integration Tests
- Test series assignment flow
- Test series rename updates all books
- Test merge preserves book numbers

### UI Tests
- Test series view mode switching
- Test expand/collapse animations
- Test navigation to series detail

## Future Enhancements

Potential additions:
1. **Series covers**: Auto-generated series cover from first book
2. **Series recommendations**: Suggest similar series
3. **Reading order**: Custom ordering vs. publication order
4. **Series collections**: Group related series (e.g., universe)
5. **Series sync**: Cloud sync series assignments
6. **Bulk import**: Import series data from CSV/JSON
7. **Series badges**: Visual indicators in grid view
8. **Series search**: Filter library by series name

## Troubleshooting

### Series Not Appearing
- Check that `seriesInfo` field is not empty
- Verify series name format is supported
- Ensure book format matches master filter (Audio/Read)

### Books Out of Order
- Use "Edit Series" to manually set book numbers
- Use "Auto-fix numbering" to extract from titles
- Verify book numbers are set correctly

### Merge Issues
- Ensure target series exists
- Check for duplicate book numbers after merge
- Manually reorder if needed

## Files Created/Modified

### New Files
1. `app/src/main/java/com/example/rezon8/data/Series.kt`
2. `app/src/main/java/com/example/rezon8/ui/viewmodel/SeriesViewModel.kt`
3. `app/src/main/java/com/example/rezon8/ui/screens/SeriesDetailScreen.kt`
4. `app/src/main/java/com/example/rezon8/ui/screens/SeriesLibraryView.kt`
5. `app/src/main/java/com/example/rezon8/ui/components/SeriesDialogs.kt`

### Modified Files
1. `app/src/main/java/com/example/rezon8/data/BookDao.kt` - Added series queries
2. `app/src/main/java/com/example/rezon8/ui/viewmodel/LibraryViewModel.kt` - Added SERIES view mode
3. `app/src/main/java/com/example/rezon8/ui/screens/LibraryScreenGlass.kt` - Integrated series view
4. `app/src/main/java/com/example/rezon8/ui/screens/BookDetailScreen.kt` - Added series display
5. `app/src/main/java/com/example/rezon8/navigation/Routes.kt` - Added SeriesDetail route

## Summary

The Series Grouping feature is a comprehensive addition to REZON8 that provides:
- Automatic series detection and parsing
- Beautiful Glass UI design
- Full series management capabilities
- Seamless integration with existing features
- Performance-optimized implementation
- Extensible architecture for future enhancements

All components follow REZON8's premium design language and maintain consistency with the existing Glass UI system.
