import { useState, useRef, useMemo, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Search,
  Plus,
  Moon,
  Sun,
  BookOpen,
  Headphones,
  FileText,
  Grid3X3,
  List,
  Upload,
  X,
  Download,
  Play,
  Clock,
  CheckCircle,
  Magnet,
  Loader2,
} from 'lucide-react';
import { Button, Modal, VocaLogo } from '../ui';
import { BookCard } from './BookCard';
import { useStore, type ProgressFilter } from '../../store/useStore';
import type { Book, BookFormat } from '../../types';

// Fetch book metadata from Open Library API
async function fetchBookMetadata(title: string, author: string): Promise<{ cover?: string; description?: string }> {
  try {
    const query = encodeURIComponent(`${title} ${author}`);
    const response = await fetch(`https://openlibrary.org/search.json?q=${query}&limit=1`);
    const data = await response.json();

    if (data.docs && data.docs.length > 0) {
      const book = data.docs[0];
      const coverId = book.cover_i;
      const cover = coverId ? `https://covers.openlibrary.org/b/id/${coverId}-L.jpg` : undefined;

      // Fetch description if available
      let description: string | undefined;
      if (book.key) {
        try {
          const workResponse = await fetch(`https://openlibrary.org${book.key}.json`);
          const workData = await workResponse.json();
          description = typeof workData.description === 'string'
            ? workData.description
            : workData.description?.value;
        } catch {
          // Ignore description fetch errors
        }
      }

      return { cover, description };
    }
  } catch (error) {
    console.error('Error fetching book metadata:', error);
  }
  return {};
}

export function Library() {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [isTorrentModalOpen, setIsTorrentModalOpen] = useState(false);
  const [torrentUrl, setTorrentUrl] = useState('');
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');
  const [dragOver, setDragOver] = useState(false);
  const [isLoadingMetadata, setIsLoadingMetadata] = useState(false);

  const {
    books,
    addBook,
    removeBook,
    updateBook,
    setCurrentBook,
    setCurrentView,
    searchQuery,
    setSearchQuery,
    formatFilter,
    setFormatFilter,
    progressFilter,
    setProgressFilter,
    isDarkMode,
    toggleDarkMode,
    activeTorrents,
    addTorrent,
    updateTorrent,
    removeTorrent,
  } = useStore();

  // Get progress status of a book
  const getProgressStatus = (book: Book): ProgressFilter => {
    if (book.isFinished || book.currentPosition >= 0.95) return 'finished';
    if (book.currentPosition > 0.01) return 'in_progress';
    return 'not_started';
  };

  const filteredBooks = useMemo(() => {
    return books.filter((book) => {
      const matchesSearch =
        book.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
        book.author.toLowerCase().includes(searchQuery.toLowerCase());
      const matchesFormat = formatFilter === 'all' || book.format === formatFilter;
      const matchesProgress = progressFilter === 'all' || getProgressStatus(book) === progressFilter;
      return matchesSearch && matchesFormat && matchesProgress;
    });
  }, [books, searchQuery, formatFilter, progressFilter]);

  const handleFileSelect = useCallback(async (files: FileList | null) => {
    if (!files) return;
    setIsLoadingMetadata(true);

    for (const file of Array.from(files)) {
      const format = getFileFormat(file.name);
      if (!format) continue;

      const cleanTitle = file.name.replace(/\.[^/.]+$/, '').replace(/_/g, ' ').replace(/-/g, ' ');

      const book: Book = {
        id: crypto.randomUUID(),
        title: cleanTitle,
        author: 'Unknown Author',
        format,
        fileUrl: URL.createObjectURL(file),
        currentPosition: 0,
        dateAdded: new Date(),
      };

      // Try to get duration for audio files
      if (format === 'audio') {
        const audio = new Audio(book.fileUrl);
        audio.addEventListener('loadedmetadata', () => {
          updateBook(book.id, { duration: audio.duration });
        });
      }

      addBook(book);

      // Fetch metadata (cover and description) from Open Library
      try {
        const metadata = await fetchBookMetadata(cleanTitle, '');
        if (metadata.cover || metadata.description) {
          updateBook(book.id, {
            cover: metadata.cover,
            description: metadata.description,
          });
        }
      } catch (error) {
        console.error('Error fetching metadata:', error);
      }
    }

    setIsLoadingMetadata(false);
    setIsAddModalOpen(false);
  }, [addBook, updateBook]);

  const getFileFormat = (filename: string): BookFormat | null => {
    const ext = filename.split('.').pop()?.toLowerCase();
    const audioFormats = ['mp3', 'm4b', 'aac', 'ogg', 'flac', 'wav', 'm4a'];
    if (audioFormats.includes(ext || '')) return 'audio';
    if (ext === 'epub') return 'epub';
    if (ext === 'pdf') return 'pdf';
    return null;
  };

  const handleBookClick = (book: Book) => {
    setCurrentBook(book);
    if (book.format === 'audio') {
      setCurrentView('player');
    } else {
      setCurrentView('reader');
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(false);
    handleFileSelect(e.dataTransfer.files);
  };

  const handleTorrentDownload = useCallback(() => {
    if (!torrentUrl.trim()) return;

    // Note: WebTorrent would be used here in the actual Electron app
    // For now, we'll show a placeholder
    const torrentId = crypto.randomUUID();
    addTorrent({ id: torrentId, name: torrentUrl.slice(0, 50), progress: 0 });

    // Simulate progress (in real app, this would come from WebTorrent)
    let progress = 0;
    const interval = setInterval(() => {
      progress += Math.random() * 10;
      if (progress >= 100) {
        progress = 100;
        clearInterval(interval);
        setTimeout(() => removeTorrent(torrentId), 2000);
      }
      updateTorrent(torrentId, progress);
    }, 500);

    setTorrentUrl('');
    setIsTorrentModalOpen(false);
  }, [torrentUrl, addTorrent, updateTorrent, removeTorrent]);

  const formatFilterOptions = [
    { value: 'all', label: 'All', icon: Grid3X3 },
    { value: 'audio', label: 'Audio', icon: Headphones },
    { value: 'epub', label: 'EPUB', icon: BookOpen },
    { value: 'pdf', label: 'PDF', icon: FileText },
  ];

  const progressFilterOptions = [
    { value: 'all', label: 'All', icon: Grid3X3 },
    { value: 'not_started', label: 'Not Started', icon: Clock },
    { value: 'in_progress', label: 'In Progress', icon: Play },
    { value: 'finished', label: 'Finished', icon: CheckCircle },
  ];

  return (
    <div
      className="min-h-screen bg-surface-50 dark:bg-surface-950 transition-colors duration-300"
      onDragOver={(e) => {
        e.preventDefault();
        setDragOver(true);
      }}
      onDragLeave={() => setDragOver(false)}
      onDrop={handleDrop}
    >
      {/* Drop overlay */}
      <AnimatePresence>
        {dragOver && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 bg-primary-500/20 backdrop-blur-sm flex items-center justify-center"
          >
            <motion.div
              initial={{ scale: 0.9 }}
              animate={{ scale: 1 }}
              className="bg-white dark:bg-surface-900 rounded-3xl p-12 shadow-2xl text-center"
            >
              <Upload className="w-16 h-16 mx-auto text-primary-500 mb-4" />
              <h3 className="text-xl font-semibold text-surface-900 dark:text-white">
                Drop your files here
              </h3>
              <p className="text-surface-500 mt-2">
                Supports MP3, M4B, EPUB, and PDF
              </p>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Active Torrents */}
      <AnimatePresence>
        {activeTorrents.length > 0 && (
          <motion.div
            initial={{ opacity: 0, y: -20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
            className="fixed top-20 right-4 z-40 space-y-2"
          >
            {activeTorrents.map((torrent) => (
              <div
                key={torrent.id}
                className="bg-white dark:bg-surface-800 rounded-xl p-4 shadow-lg min-w-[280px]"
              >
                <div className="flex items-center gap-3 mb-2">
                  <Download className="w-4 h-4 text-primary-500 animate-bounce" />
                  <span className="text-sm font-medium truncate flex-1">{torrent.name}</span>
                </div>
                <div className="h-2 bg-surface-200 dark:bg-surface-700 rounded-full overflow-hidden">
                  <motion.div
                    className="h-full bg-gradient-to-r from-primary-500 to-primary-400"
                    initial={{ width: 0 }}
                    animate={{ width: `${torrent.progress}%` }}
                  />
                </div>
                <p className="text-xs text-surface-500 mt-1">{Math.round(torrent.progress)}%</p>
              </div>
            ))}
          </motion.div>
        )}
      </AnimatePresence>

      {/* Header */}
      <header className="sticky top-0 z-40 glass">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16">
            {/* Logo */}
            <motion.div
              initial={{ x: -20, opacity: 0 }}
              animate={{ x: 0, opacity: 1 }}
            >
              <VocaLogo size="md" />
            </motion.div>

            {/* Search */}
            <motion.div
              initial={{ y: -20, opacity: 0 }}
              animate={{ y: 0, opacity: 1 }}
              transition={{ delay: 0.1 }}
              className="flex-1 max-w-md mx-8"
            >
              <div className="relative">
                <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-surface-400" />
                <input
                  type="text"
                  placeholder="Search your library..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="input pl-12"
                />
                {searchQuery && (
                  <button
                    onClick={() => setSearchQuery('')}
                    className="absolute right-4 top-1/2 -translate-y-1/2 text-surface-400 hover:text-surface-600"
                  >
                    <X className="w-4 h-4" />
                  </button>
                )}
              </div>
            </motion.div>

            {/* Actions */}
            <motion.div
              initial={{ x: 20, opacity: 0 }}
              animate={{ x: 0, opacity: 1 }}
              className="flex items-center gap-2"
            >
              <Button variant="icon" onClick={toggleDarkMode}>
                {isDarkMode ? (
                  <Sun className="w-5 h-5" />
                ) : (
                  <Moon className="w-5 h-5" />
                )}
              </Button>
              <Button variant="ghost" onClick={() => setIsTorrentModalOpen(true)}>
                <Magnet className="w-5 h-5" />
              </Button>
              <Button
                variant="primary"
                onClick={() => setIsAddModalOpen(true)}
              >
                <Plus className="w-5 h-5" />
                Add Book
              </Button>
            </motion.div>
          </div>
        </div>
      </header>

      {/* Main content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Filters */}
        <motion.div
          initial={{ y: 20, opacity: 0 }}
          animate={{ y: 0, opacity: 1 }}
          transition={{ delay: 0.2 }}
          className="flex flex-wrap items-center justify-between gap-4 mb-8"
        >
          <div className="flex flex-wrap items-center gap-4">
            {/* Format Filter */}
            <div className="flex gap-1 p-1 bg-surface-100 dark:bg-surface-800 rounded-xl">
              {formatFilterOptions.map(({ value, label, icon: Icon }) => (
                <button
                  key={value}
                  onClick={() => setFormatFilter(value as BookFormat | 'all')}
                  className={`flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-all ${
                    formatFilter === value
                      ? 'bg-white dark:bg-surface-700 text-surface-900 dark:text-white shadow-sm'
                      : 'text-surface-500 hover:text-surface-700 dark:hover:text-surface-300'
                  }`}
                >
                  <Icon className="w-4 h-4" />
                  {label}
                </button>
              ))}
            </div>

            {/* Progress Filter */}
            <div className="flex gap-1 p-1 bg-surface-100 dark:bg-surface-800 rounded-xl">
              {progressFilterOptions.map(({ value, label, icon: Icon }) => (
                <button
                  key={value}
                  onClick={() => setProgressFilter(value as ProgressFilter)}
                  className={`flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-all ${
                    progressFilter === value
                      ? 'bg-white dark:bg-surface-700 text-surface-900 dark:text-white shadow-sm'
                      : 'text-surface-500 hover:text-surface-700 dark:hover:text-surface-300'
                  }`}
                >
                  <Icon className="w-4 h-4" />
                  <span className="hidden sm:inline">{label}</span>
                </button>
              ))}
            </div>
          </div>

          <div className="flex gap-1 p-1 bg-surface-100 dark:bg-surface-800 rounded-xl">
            <button
              onClick={() => setViewMode('grid')}
              className={`p-2 rounded-lg transition-all ${
                viewMode === 'grid'
                  ? 'bg-white dark:bg-surface-700 shadow-sm'
                  : 'text-surface-500 hover:text-surface-700'
              }`}
            >
              <Grid3X3 className="w-5 h-5" />
            </button>
            <button
              onClick={() => setViewMode('list')}
              className={`p-2 rounded-lg transition-all ${
                viewMode === 'list'
                  ? 'bg-white dark:bg-surface-700 shadow-sm'
                  : 'text-surface-500 hover:text-surface-700'
              }`}
            >
              <List className="w-5 h-5" />
            </button>
          </div>
        </motion.div>

        {/* Loading indicator for metadata */}
        {isLoadingMetadata && (
          <div className="flex items-center gap-2 mb-4 text-primary-500">
            <Loader2 className="w-4 h-4 animate-spin" />
            <span className="text-sm">Fetching book metadata...</span>
          </div>
        )}

        {/* Books grid */}
        {filteredBooks.length > 0 ? (
          <div
            className={
              viewMode === 'grid'
                ? 'grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-6'
                : 'space-y-4'
            }
          >
            {filteredBooks.map((book, index) => (
              <BookCard
                key={book.id}
                book={book}
                onClick={() => handleBookClick(book)}
                onDelete={() => removeBook(book.id)}
                index={index}
              />
            ))}
          </div>
        ) : (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="text-center py-20"
          >
            <div className="w-20 h-20 mx-auto mb-6 rounded-full bg-surface-100 dark:bg-surface-800 flex items-center justify-center">
              <BookOpen className="w-10 h-10 text-surface-400" />
            </div>
            <h3 className="text-xl font-semibold text-surface-900 dark:text-white mb-2">
              {searchQuery || formatFilter !== 'all' || progressFilter !== 'all'
                ? 'No books found'
                : 'Your library is empty'}
            </h3>
            <p className="text-surface-500 dark:text-surface-400 mb-6">
              {searchQuery || formatFilter !== 'all' || progressFilter !== 'all'
                ? 'Try adjusting your filters'
                : 'Add your first audiobook, ebook, or PDF to get started'}
            </p>
            {!searchQuery && formatFilter === 'all' && progressFilter === 'all' && (
              <Button variant="primary" onClick={() => setIsAddModalOpen(true)}>
                <Plus className="w-5 h-5" />
                Add Your First Book
              </Button>
            )}
          </motion.div>
        )}
      </main>

      {/* Add Book Modal */}
      <Modal
        isOpen={isAddModalOpen}
        onClose={() => setIsAddModalOpen(false)}
        title="Add Books"
        size="lg"
      >
        <div className="space-y-6">
          {/* Drop zone */}
          <div
            onClick={() => fileInputRef.current?.click()}
            className="border-2 border-dashed border-surface-300 dark:border-surface-600 rounded-2xl p-12 text-center cursor-pointer hover:border-primary-500 hover:bg-primary-500/5 transition-colors"
          >
            <Upload className="w-12 h-12 mx-auto text-surface-400 mb-4" />
            <p className="text-surface-900 dark:text-white font-medium">
              Click to browse or drag files here
            </p>
            <p className="text-sm text-surface-500 mt-2">
              Supports MP3, M4B, AAC, OGG, FLAC, WAV, EPUB, and PDF
            </p>
          </div>

          <input
            ref={fileInputRef}
            type="file"
            multiple
            accept=".mp3,.m4b,.m4a,.aac,.ogg,.flac,.wav,.epub,.pdf"
            onChange={(e) => handleFileSelect(e.target.files)}
            className="hidden"
          />

          {/* Supported formats */}
          <div className="grid grid-cols-3 gap-4">
            {[
              { icon: Headphones, label: 'Audio', formats: 'MP3, M4B, AAC, OGG' },
              { icon: BookOpen, label: 'EPUB', formats: 'EPUB format' },
              { icon: FileText, label: 'PDF', formats: 'PDF documents' },
            ].map(({ icon: Icon, label, formats }) => (
              <div
                key={label}
                className="p-4 rounded-xl bg-surface-50 dark:bg-surface-800"
              >
                <Icon className="w-6 h-6 text-primary-500 mb-2" />
                <p className="font-medium text-surface-900 dark:text-white">
                  {label}
                </p>
                <p className="text-xs text-surface-500 mt-1">{formats}</p>
              </div>
            ))}
          </div>
        </div>
      </Modal>

      {/* Torrent Download Modal */}
      <Modal
        isOpen={isTorrentModalOpen}
        onClose={() => setIsTorrentModalOpen(false)}
        title="Download from Torrent"
        size="md"
      >
        <div className="space-y-4">
          <p className="text-sm text-surface-500 dark:text-surface-400">
            Enter a magnet link or torrent URL to download audiobooks or ebooks.
            Only download content you have the right to access.
          </p>
          <input
            type="text"
            placeholder="magnet:?xt=urn:btih:... or https://..."
            value={torrentUrl}
            onChange={(e) => setTorrentUrl(e.target.value)}
            className="input"
          />
          <div className="flex gap-3">
            <Button
              variant="secondary"
              className="flex-1"
              onClick={() => setIsTorrentModalOpen(false)}
            >
              Cancel
            </Button>
            <Button
              variant="primary"
              className="flex-1"
              onClick={handleTorrentDownload}
              disabled={!torrentUrl.trim()}
            >
              <Download className="w-4 h-4" />
              Download
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
