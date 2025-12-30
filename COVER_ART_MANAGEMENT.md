# Cover Art Management System

## Overview

The REZON8 audiobook app now includes a comprehensive cover art management system that allows users to search, download, and manage cover artwork for their audiobooks. The system is built using the Glass UI design system and integrates seamlessly with the existing app architecture.

## Features

### 1. Multiple Cover Art Sources
- **Online Search**: Search for cover art from Google Books and OpenLibrary APIs
- **Device Gallery**: Pick cover art from the device's photo gallery
- **Direct URL**: Enter a direct URL to any cover image

### 2. Glass UI Design
- Consistent with REZON8's Glass UI design system
- Supports both Dark and Rezon Dark themes
- Uses copper accent colors for brand consistency
- Smooth animations and loading states

### 3. Integration Points
- **Book Detail Screen**: "Change Cover" option in the overflow menu
- **Edit Book Screen**: Clickable cover preview with edit button overlay
- **Automatic Download**: All cover images are downloaded and stored locally

## Architecture

### Files Created

1. **CoverArtRepository.kt** (`data/CoverArtRepository.kt`)
   - Manages all cover art operations
   - Handles API calls to Google Books and OpenLibrary
   - Downloads and saves cover images locally
   - Updates book cover URLs in the database

2. **CoverArtViewModel.kt** (`ui/viewmodel/CoverArtViewModel.kt`)
   - State management for cover art operations
   - Exposes flows for search results, loading states, and errors
   - Handles user interactions and business logic

3. **CoverArtPickerDialog.kt** (`ui/screens/CoverArtPickerDialog.kt`)
   - Full-screen dialog with Glass UI design
   - Three tabs: Search, URL, and Gallery
   - Grid view for search results
   - Preview functionality for URLs

### Files Modified

1. **EditBookScreen.kt**
   - Added cover art picker dialog integration
   - Made cover preview clickable with edit button overlay
   - Added `isRezonDark` parameter for theme consistency

2. **BookDetailScreen.kt**
   - Added "Change Cover" option to overflow menu
   - Integrated cover art picker dialog
   - Shows success toast on cover update

3. **MainLayoutGlass.kt & MainLayout.kt**
   - Updated navigation to pass `isRezonDark` parameter to EditBookScreen

4. **AndroidManifest.xml**
   - Added `READ_MEDIA_IMAGES` permission for gallery access

## Usage

### For Users

#### From Book Detail Screen:
1. Open any book detail screen
2. Tap the three-dot menu (⋮) in the top-right corner
3. Select "Change Cover"
4. Choose from three options:
   - **Search**: Search by title/author and select from results
   - **URL**: Paste a direct link to a cover image
   - **Gallery**: Pick an image from your device

#### From Edit Book Screen:
1. Navigate to Edit Book screen
2. Tap on the cover preview or the edit button overlay
3. Follow the same process as above

### For Developers

#### Using CoverArtRepository

```kotlin
@Inject
lateinit var coverArtRepository: CoverArtRepository

// Search for cover art
val results = coverArtRepository.searchCoverArt(
    title = "The Hobbit",
    author = "J.R.R. Tolkien"
)

// Download and save a cover
val localPath = coverArtRepository.downloadAndSaveCover(
    url = "https://covers.openlibrary.org/b/id/12345-L.jpg"
)

// Update book cover
coverArtRepository.updateBookCover(
    bookId = "book-id-123",
    coverUrl = localPath
)

// Save from gallery
val savedPath = coverArtRepository.saveCoverFromGallery(
    uri = galleryUri
)
```

#### Using CoverArtViewModel

```kotlin
@Composable
fun MyScreen(viewModel: CoverArtViewModel = hiltViewModel()) {
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Search for covers
    viewModel.searchCoverArt(title = "Book Title", author = "Author Name")

    // Download and save
    scope.launch {
        val localPath = viewModel.downloadAndSaveCover(url)
        if (localPath != null) {
            // Success!
        }
    }

    // Update book cover
    viewModel.updateBookCover(bookId, newCoverUrl)
}
```

#### Using CoverArtPickerDialog

```kotlin
@Composable
fun MyScreen() {
    var showCoverPicker by remember { mutableStateOf(false) }

    if (showCoverPicker) {
        CoverArtPickerDialog(
            currentCoverUrl = book.coverUrl,
            bookTitle = book.title,
            bookAuthor = book.author,
            accentColor = GlassColors.RezonAccent,
            isRezonDark = true,
            onDismiss = { showCoverPicker = false },
            onCoverSelected = { newCoverUrl ->
                // Handle cover selection
                coverArtViewModel.updateBookCover(bookId, newCoverUrl)
            }
        )
    }
}
```

## API Integration

### Google Books API
- **Endpoint**: `https://www.googleapis.com/books/v1/volumes`
- **Query Format**: `intitle:Title+inauthor:Author`
- **Max Results**: 6 covers per search
- **Image Quality**: Automatically upgraded to higher quality (zoom=2)

### OpenLibrary API
- **Search Endpoint**: `https://openlibrary.org/search.json`
- **Cover Endpoint**: `https://covers.openlibrary.org/b/id/{id}-L.jpg`
- **Max Results**: 6 covers per search
- **Image Size**: Large (L) for best quality

### Search Strategy
1. Searches both APIs concurrently
2. Removes duplicate covers
3. Limits results to 12 total covers
4. Shows source badge on each result (GoogleBooks/OpenLibrary)

## Error Handling

### Network Errors
- Graceful fallback if one API fails
- User-friendly error messages
- Crash reporting for debugging

### Download Errors
- Timeout handling (15 seconds)
- HTTP status code validation
- File system error handling

### Validation
- URL format validation for image URLs
- File extension checking (.jpg, .jpeg, .png, .webp, .gif)
- Domain whitelisting for known cover art sources

## Storage Management

### Local Storage
- All covers are downloaded and saved to internal storage
- File naming: `cover_{UUID}.jpg`
- Old covers are automatically deleted when replaced
- Storage location: `context.filesDir`

### Database
- Cover URLs stored in `Book.coverUrl` field
- Supports both local paths and remote URLs
- Automatic cleanup on book deletion

## Performance Considerations

### Image Loading
- Uses Coil for efficient image loading
- Automatic caching
- Progressive loading with placeholders

### Network Calls
- Concurrent API requests (Google Books + OpenLibrary)
- Connection timeout: 15 seconds
- Read timeout: 30 seconds
- Non-blocking coroutines for all operations

### Memory Management
- Grid view with lazy loading
- Image recycling
- Proper cleanup on dialog dismiss

## Accessibility

- All interactive elements meet 44dp minimum touch target
- Descriptive content descriptions for screen readers
- Clear visual feedback for loading states
- Error messages are announced to screen readers

## Theme Support

### Dark Theme
- White text on dark backgrounds
- Semi-transparent glass surfaces
- High contrast for readability

### Rezon Dark Theme
- Subdued text colors (soft gray-white)
- Copper accent borders and highlights
- Deep black background (#050505)
- Premium, sophisticated appearance

## Future Enhancements

### Potential Features
- [ ] Crop/edit cover images
- [ ] Multiple cover art per book (variants)
- [ ] Community-contributed cover art
- [ ] AI-generated covers based on book content
- [ ] Batch cover art updates
- [ ] Cover art quality settings (low/medium/high)
- [ ] Integration with additional APIs (Audible, iTunes)

### Performance Improvements
- [ ] Implement image compression
- [ ] Add cover art cache size limits
- [ ] Background sync for cover updates
- [ ] Progressive image loading

## Testing

### Manual Testing Checklist
- [ ] Search from Book Detail screen
- [ ] Search from Edit Book screen
- [ ] Search with title only
- [ ] Search with title + author
- [ ] Select cover from Google Books results
- [ ] Select cover from OpenLibrary results
- [ ] Enter URL manually
- [ ] Pick from device gallery
- [ ] Test with no internet connection
- [ ] Test with invalid URL
- [ ] Test Dark theme
- [ ] Test Rezon Dark theme
- [ ] Verify cover persists after app restart
- [ ] Verify old cover is deleted when replaced

### Edge Cases
- Books with no author
- Books with special characters in title
- Very long book titles
- Network timeout scenarios
- Invalid image URLs
- Corrupted image files
- No search results
- Gallery picker cancellation

## Troubleshooting

### Cover Not Appearing
1. Check internet connection
2. Verify READ_MEDIA_IMAGES permission granted
3. Check if cover URL is valid
4. Look for error messages in logs (tag: CoverArtRepository)

### Search Returns No Results
1. Try searching with title only (remove author)
2. Simplify search query (remove special characters)
3. Check if APIs are accessible (Google Books, OpenLibrary)
4. Try using URL or Gallery instead

### Gallery Picker Not Working
1. Verify READ_MEDIA_IMAGES permission in manifest
2. Check if permission is granted at runtime
3. Ensure device has photos in gallery
4. Try restarting the app

## Contributing

When adding new features to the cover art system:

1. Follow Glass UI design system guidelines
2. Maintain theme consistency (Dark + Rezon Dark)
3. Add proper error handling and logging
4. Update this documentation
5. Test on both themes
6. Ensure accessibility standards are met

## License

This feature is part of the REZON8 audiobook application.
