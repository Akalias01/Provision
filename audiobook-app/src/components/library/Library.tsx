import { useState, useRef, useMemo, useCallback, useEffect } from 'react';
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
  Settings,
  Palette,
  Sparkles,
  FolderSearch,
  FileDown,
} from 'lucide-react';
import { Button, Modal, RezonLogo, SidebarMenu } from '../ui';
import { BookCard } from './BookCard';
import { useStore, type ProgressFilter, type ColorTheme, type LogoVariant, type SplashVariant, colorThemes } from '../../store/useStore';
import { useTranslation } from '../../i18n';
import type { Book, BookFormat } from '../../types';
import '../../types/electron.d.ts';

// Check if running in Electron
const isElectron = () => typeof window !== 'undefined' && !!window.electronAPI;

// Play download complete sound notification
function playDownloadCompleteSound() {
  try {
    const audioContext = new (window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext)();

    // First tone (ascending)
    const osc1 = audioContext.createOscillator();
    const gain1 = audioContext.createGain();
    osc1.connect(gain1);
    gain1.connect(audioContext.destination);
    osc1.frequency.value = 880; // A5
    osc1.type = 'sine';
    gain1.gain.setValueAtTime(0.3, audioContext.currentTime);
    gain1.gain.exponentialRampToValueAtTime(0.01, audioContext.currentTime + 0.3);
    osc1.start(audioContext.currentTime);
    osc1.stop(audioContext.currentTime + 0.3);

    // Second tone (higher, delayed)
    const osc2 = audioContext.createOscillator();
    const gain2 = audioContext.createGain();
    osc2.connect(gain2);
    gain2.connect(audioContext.destination);
    osc2.frequency.value = 1318.5; // E6
    osc2.type = 'sine';
    gain2.gain.setValueAtTime(0, audioContext.currentTime);
    gain2.gain.setValueAtTime(0.3, audioContext.currentTime + 0.15);
    gain2.gain.exponentialRampToValueAtTime(0.01, audioContext.currentTime + 0.5);
    osc2.start(audioContext.currentTime + 0.15);
    osc2.stop(audioContext.currentTime + 0.5);
  } catch (e) {
    console.log('Could not play notification sound:', e);
  }
}

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

interface ScannedFile {
  file: File;
  format: BookFormat;
  path: string;
  selected: boolean;
}

export function Library() {
  const { t } = useTranslation();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const torrentInputRef = useRef<HTMLInputElement>(null);
  const folderInputRef = useRef<HTMLInputElement>(null);
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [isTorrentModalOpen, setIsTorrentModalOpen] = useState(false);
  const [isSettingsModalOpen, setIsSettingsModalOpen] = useState(false);
  const [isScanModalOpen, setIsScanModalOpen] = useState(false);
  const [torrentUrl, setTorrentUrl] = useState('');
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');
  const [dragOver, setDragOver] = useState(false);
  const [isLoadingMetadata, setIsLoadingMetadata] = useState(false);
  const [isScanning, setIsScanning] = useState(false);
  const [foundTorrentFiles, setFoundTorrentFiles] = useState<File[]>([]);
  const [scannedFiles, setScannedFiles] = useState<ScannedFile[]>([]);

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
    colorTheme,
    setColorTheme,
    logoVariant,
    setLogoVariant,
    splashVariant,
    setSplashVariant,
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

  // Set up Electron torrent event listeners
  useEffect(() => {
    if (!isElectron()) return;

    const api = window.electronAPI!;

    // Handle progress updates
    const unsubProgress = api.onTorrentProgress((data) => {
      updateTorrent(data.id, data.progress);
    });

    // Handle completion
    const unsubComplete = api.onTorrentComplete((data) => {
      // Play completion sound
      playDownloadCompleteSound();

      // Get the first audio file path for playback
      const fileUrl = data.files.length > 0 ? `file://${data.files[0]}` : data.downloadPath;

      // Create book entry
      const cleanName = data.name
        .replace(/[_-]/g, ' ')
        .replace(/\.[^/.]+$/, '');

      const book: Book = {
        id: crypto.randomUUID(),
        title: cleanName,
        author: 'Unknown Author',
        format: 'audio',
        fileUrl: fileUrl,
        currentPosition: 0,
        dateAdded: new Date(),
      };
      addBook(book);

      // Fetch metadata for the new book
      fetchBookMetadata(cleanName, '').then((metadata) => {
        if (metadata.cover || metadata.description) {
          updateBook(book.id, {
            cover: metadata.cover,
            description: metadata.description,
          });
        }
      });

      // Remove torrent progress indicator after a delay
      setTimeout(() => removeTorrent(data.id), 2000);
    });

    // Handle errors
    const unsubError = api.onTorrentError((data) => {
      console.error('Torrent error:', data.error);
      removeTorrent(data.id);
    });

    return () => {
      unsubProgress();
      unsubComplete();
      unsubError();
    };
  }, [addBook, updateBook, removeTorrent, updateTorrent]);

  // Simulated download for browser testing or fallback
  const runSimulatedDownload = useCallback((cleanName: string) => {
    const torrentId = crypto.randomUUID();
    addTorrent({ id: torrentId, name: cleanName, progress: 0 });
    console.log('[Torrent] Starting simulated download:', torrentId);

    let progress = 0;
    const interval = setInterval(() => {
      progress += Math.random() * 15 + 5;
      if (progress >= 100) {
        progress = 100;
        clearInterval(interval);

        // Play completion sound
        playDownloadCompleteSound();

        // Add the downloaded content to library (simulated)
        const book: Book = {
          id: crypto.randomUUID(),
          title: cleanName,
          author: 'Unknown Author',
          format: 'audio',
          fileUrl: `simulated://${torrentId}`, // Placeholder for browser testing
          currentPosition: 0,
          dateAdded: new Date(),
        };
        addBook(book);

        // Fetch metadata for the new book
        fetchBookMetadata(cleanName, '').then((metadata) => {
          if (metadata.cover || metadata.description) {
            updateBook(book.id, {
              cover: metadata.cover,
              description: metadata.description,
            });
          }
        });

        // Remove torrent progress indicator after a delay
        setTimeout(() => removeTorrent(torrentId), 2000);
      }
      updateTorrent(torrentId, progress);
    }, 400);
  }, [addTorrent, updateTorrent, removeTorrent, addBook, updateBook]);

  // Shared function to start a torrent download and add book when complete
  const startTorrentDownload = useCallback(async (torrentSource: string | Uint8Array, fileName?: string) => {
    // Use filename if provided, otherwise extract from source
    const displayName = fileName || (typeof torrentSource === 'string' ? torrentSource : 'Unknown Torrent');
    const cleanName = displayName
      .replace(/\.torrent$/i, '')
      .replace(/[_-]/g, ' ')
      .replace(/\.[^/.]+$/, '')
      .slice(0, 100);

    console.log('[Torrent] Starting download:', cleanName, 'isElectron:', isElectron());

    // Use real Electron torrent API if available
    if (isElectron()) {
      try {
        // Convert Uint8Array to regular array for IPC serialization
        const sourceForIPC = torrentSource instanceof Uint8Array
          ? Array.from(torrentSource)
          : torrentSource;

        console.log('[Torrent] Sending to Electron IPC, data type:', typeof sourceForIPC, 'isArray:', Array.isArray(sourceForIPC));

        const result = await window.electronAPI!.startTorrentDownload(sourceForIPC, fileName);

        console.log('[Torrent] IPC result:', result);

        if (result.success && result.id) {
          addTorrent({ id: result.id, name: result.name || cleanName, progress: 0 });
          console.log('[Torrent] Added to active torrents:', result.id);
        } else {
          console.error('[Torrent] Failed to start:', result.error);
          // Fall back to simulation if Electron torrent fails
          console.log('[Torrent] Falling back to simulation...');
          runSimulatedDownload(cleanName);
        }
      } catch (error) {
        console.error('[Torrent] Error starting torrent:', error);
        // Fall back to simulation on error
        console.log('[Torrent] Falling back to simulation due to error...');
        runSimulatedDownload(cleanName);
      }
      return;
    }

    // Browser mode: Simulate download
    runSimulatedDownload(cleanName);
  }, [addTorrent, runSimulatedDownload]);

  const handleTorrentDownload = useCallback(() => {
    if (!torrentUrl.trim()) return;

    // Close modal immediately when download starts
    setIsTorrentModalOpen(false);

    startTorrentDownload(torrentUrl);
    setTorrentUrl('');
  }, [torrentUrl, startTorrentDownload]);

  // Handle folder scan
  const handleFolderScan = useCallback((files: FileList | null) => {
    if (!files) return;
    setIsScanning(true);

    const scanned: ScannedFile[] = [];

    for (const file of Array.from(files)) {
      const format = getFileFormat(file.name);
      if (format) {
        // Get the relative path from webkitRelativePath
        const path = (file as File & { webkitRelativePath?: string }).webkitRelativePath || file.name;
        scanned.push({
          file,
          format,
          path,
          selected: true, // Default to selected
        });
      }
    }

    setScannedFiles(scanned);
    setIsScanning(false);
    setIsScanModalOpen(true);
  }, []);

  // Toggle scanned file selection
  const toggleScannedFile = useCallback((index: number) => {
    setScannedFiles(prev => prev.map((f, i) =>
      i === index ? { ...f, selected: !f.selected } : f
    ));
  }, []);

  // Select/deselect all scanned files
  const toggleAllScannedFiles = useCallback((selected: boolean) => {
    setScannedFiles(prev => prev.map(f => ({ ...f, selected })));
  }, []);

  // Import selected scanned files
  const importScannedFiles = useCallback(async () => {
    const selectedFiles = scannedFiles.filter(f => f.selected);
    if (selectedFiles.length === 0) return;

    setIsLoadingMetadata(true);

    for (const { file, format } of selectedFiles) {
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
    setIsScanModalOpen(false);
    setScannedFiles([]);
  }, [scannedFiles, addBook, updateBook]);

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

  const logoVariants: { value: LogoVariant; label: string }[] = [
    { value: 'waveform', label: 'Waveform' },
    { value: 'headphones', label: 'Headphones' },
    { value: 'pulse', label: 'Pulse' },
    { value: 'minimal', label: 'Minimal' },
  ];

  const splashVariants: { value: SplashVariant; label: string }[] = [
    { value: 'pulseWave', label: 'Pulse Wave' },
    { value: 'glitchCyber', label: 'Glitch Cyber' },
    { value: 'waveformMorph', label: 'Waveform Morph' },
    { value: 'neonFlicker', label: 'Neon Flicker' },
  ];

  // Check if library is empty (not just filtered)
  const isLibraryEmpty = books.length === 0;

  return (
    <div
      id="main-app-content"
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

      {/* Active Torrents - Circular Progress */}
      <AnimatePresence>
        {activeTorrents.length > 0 && (
          <motion.div
            initial={{ opacity: 0, y: -20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
            className="fixed top-20 right-4 z-40 space-y-3"
          >
            {activeTorrents.map((torrent) => {
              const circumference = 2 * Math.PI * 36; // radius 36
              const strokeDashoffset = circumference - (torrent.progress / 100) * circumference;

              return (
                <motion.div
                  key={torrent.id}
                  initial={{ scale: 0.8, opacity: 0 }}
                  animate={{ scale: 1, opacity: 1 }}
                  exit={{ scale: 0.8, opacity: 0 }}
                  className="bg-white dark:bg-surface-800 rounded-2xl p-4 shadow-xl border border-surface-200 dark:border-surface-700 min-w-[240px]"
                >
                  <div className="flex items-center gap-4">
                    {/* Circular Progress */}
                    <div className="relative w-16 h-16 flex-shrink-0">
                      <svg className="w-16 h-16 -rotate-90" viewBox="0 0 80 80">
                        {/* Background circle */}
                        <circle
                          cx="40"
                          cy="40"
                          r="36"
                          fill="none"
                          strokeWidth="6"
                          className="stroke-surface-200 dark:stroke-surface-700"
                        />
                        {/* Progress circle */}
                        <motion.circle
                          cx="40"
                          cy="40"
                          r="36"
                          fill="none"
                          strokeWidth="6"
                          strokeLinecap="round"
                          className="stroke-primary-500"
                          initial={{ strokeDashoffset: circumference }}
                          animate={{ strokeDashoffset }}
                          style={{ strokeDasharray: circumference }}
                          transition={{ duration: 0.3, ease: "easeOut" }}
                        />
                      </svg>
                      {/* Percentage in center */}
                      <div className="absolute inset-0 flex items-center justify-center">
                        <span className="text-sm font-bold text-surface-900 dark:text-white">
                          {Math.round(torrent.progress)}%
                        </span>
                      </div>
                      {/* Animated glow effect */}
                      {torrent.progress < 100 && (
                        <motion.div
                          className="absolute inset-0 rounded-full"
                          style={{
                            background: 'radial-gradient(circle, rgba(6,182,212,0.3) 0%, transparent 70%)',
                          }}
                          animate={{ scale: [1, 1.2, 1], opacity: [0.5, 0.8, 0.5] }}
                          transition={{ duration: 1.5, repeat: Infinity }}
                        />
                      )}
                    </div>

                    {/* Info */}
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 mb-1">
                        <Download className="w-4 h-4 text-primary-500 animate-bounce" />
                        <span className="text-xs font-medium text-primary-500 uppercase tracking-wider">
                          {torrent.progress >= 100 ? 'Complete' : 'Downloading'}
                        </span>
                      </div>
                      <p className="text-sm font-semibold text-surface-900 dark:text-white truncate">
                        {torrent.name}
                      </p>
                    </div>
                  </div>
                </motion.div>
              );
            })}
          </motion.div>
        )}
      </AnimatePresence>

      {/* Header */}
      <header className="sticky top-0 z-40 glass">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16">
            {/* Menu and Logo */}
            <motion.div
              initial={{ x: -20, opacity: 0 }}
              animate={{ x: 0, opacity: 1 }}
              className="flex items-center gap-2"
            >
              <SidebarMenu
                onOpenTorrent={() => setIsTorrentModalOpen(true)}
                onOpenSettings={() => setIsSettingsModalOpen(true)}
              />
              <RezonLogo size="md" variant={logoVariant} />
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
                  placeholder={`${t('search')}...`}
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
              <Button variant="icon" onClick={() => setIsSettingsModalOpen(true)}>
                <Settings className="w-5 h-5" />
              </Button>
              <Button variant="ghost" onClick={() => setIsTorrentModalOpen(true)}>
                <Magnet className="w-5 h-5" />
              </Button>
              <Button
                variant="primary"
                onClick={() => setIsAddModalOpen(true)}
              >
                <Plus className="w-5 h-5" />
                {t('addBooks')}
              </Button>
            </motion.div>
          </div>
        </div>
      </header>

      {/* Main content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Show filters only if library is not empty */}
        {!isLibraryEmpty && (
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
        )}

        {/* Loading indicator for metadata */}
        {isLoadingMetadata && (
          <div className="flex items-center gap-2 mb-4 text-primary-500">
            <Loader2 className="w-4 h-4 animate-spin" />
            <span className="text-sm">Fetching book metadata...</span>
          </div>
        )}

        {/* Books grid or Empty State */}
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
        ) : isLibraryEmpty ? (
          /* Empty Library - First time user experience */
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="flex flex-col items-center justify-center py-16"
          >
            {/* Large animated logo */}
            <motion.div
              initial={{ scale: 0.8 }}
              animate={{ scale: 1 }}
              transition={{ type: 'spring', duration: 0.8 }}
              className="mb-8"
            >
              <RezonLogo size="xl" variant={logoVariant} />
            </motion.div>

            <h2 className="text-3xl font-bold text-surface-900 dark:text-white mb-4 text-center">
              Welcome to Rezon
            </h2>
            <p className="text-lg text-surface-500 dark:text-surface-400 mb-8 text-center max-w-md">
              Audiobooks Reimagined. Resonate With Every Word.
            </p>

            {/* Import options */}
            <div className="flex flex-col sm:flex-row gap-4 mb-12">
              <motion.button
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
                onClick={() => fileInputRef.current?.click()}
                className="flex items-center gap-3 px-8 py-4 bg-gradient-to-r from-primary-500 to-primary-600 text-white rounded-2xl font-semibold shadow-lg shadow-primary-500/25 hover:shadow-xl hover:shadow-primary-500/30 transition-shadow"
              >
                <Upload className="w-6 h-6" />
                Import Your First Book
              </motion.button>

              <motion.button
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
                onClick={() => setIsTorrentModalOpen(true)}
                className="flex items-center gap-3 px-8 py-4 bg-surface-100 dark:bg-surface-800 text-surface-700 dark:text-surface-200 rounded-2xl font-semibold hover:bg-surface-200 dark:hover:bg-surface-700 transition-colors"
              >
                <Magnet className="w-6 h-6" />
                Download from Torrent
              </motion.button>
            </div>

            {/* Supported formats */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-6 max-w-2xl">
              {[
                { icon: Headphones, label: t('audiobooks'), formats: t('audioFormat'), color: 'from-cyan-500 to-teal-500' },
                { icon: BookOpen, label: t('ebooks'), formats: t('epubFormat'), color: 'from-violet-500 to-purple-500' },
                { icon: FileText, label: t('documents'), formats: t('pdfFormat'), color: 'from-orange-500 to-red-500' },
              ].map(({ icon: Icon, label, formats, color }) => (
                <motion.div
                  key={label}
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  className="p-6 rounded-2xl bg-white dark:bg-surface-900 border border-surface-200 dark:border-surface-800 text-center"
                >
                  <div className={`w-14 h-14 mx-auto mb-4 rounded-xl bg-gradient-to-br ${color} flex items-center justify-center`}>
                    <Icon className="w-7 h-7 text-white" />
                  </div>
                  <h3 className="font-semibold text-surface-900 dark:text-white mb-1">{label}</h3>
                  <p className="text-sm text-surface-500">{formats}</p>
                </motion.div>
              ))}
            </div>

            {/* Drag and drop hint */}
            <p className="mt-8 text-sm text-surface-400 flex items-center gap-2">
              <Sparkles className="w-4 h-4" />
              You can also drag and drop files anywhere on this page
            </p>
          </motion.div>
        ) : (
          /* Filtered results empty */
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="text-center py-20"
          >
            <div className="w-20 h-20 mx-auto mb-6 rounded-full bg-surface-100 dark:bg-surface-800 flex items-center justify-center">
              <BookOpen className="w-10 h-10 text-surface-400" />
            </div>
            <h3 className="text-xl font-semibold text-surface-900 dark:text-white mb-2">
              {t('noBooks')}
            </h3>
            <p className="text-surface-500 dark:text-surface-400 mb-6">
              {t('noBooksDescription')}
            </p>
            <Button variant="secondary" onClick={() => {
              setSearchQuery('');
              setFormatFilter('all');
              setProgressFilter('all');
            }}>
              Clear Filters
            </Button>
          </motion.div>
        )}
      </main>

      {/* Hidden file input */}
      <input
        ref={fileInputRef}
        type="file"
        multiple
        accept=".mp3,.m4b,.m4a,.aac,.ogg,.flac,.wav,.epub,.pdf"
        onChange={(e) => handleFileSelect(e.target.files)}
        className="hidden"
      />

      {/* Hidden folder input for scanning */}
      <input
        ref={folderInputRef}
        type="file"
        multiple
        // @ts-expect-error webkitdirectory is not in standard types
        webkitdirectory=""
        onChange={(e) => handleFolderScan(e.target.files)}
        className="hidden"
      />

      {/* Add Book Modal */}
      <Modal
        isOpen={isAddModalOpen}
        onClose={() => setIsAddModalOpen(false)}
        title={t('addBooks')}
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
              {t('dragAndDrop')}
            </p>
            <p className="text-sm text-surface-500 mt-2">
              {t('supportedFormats')}
            </p>
          </div>

          {/* Scan Folder Button */}
          <div className="border-t border-surface-200 dark:border-surface-700 pt-6">
            <button
              onClick={() => {
                setIsAddModalOpen(false);
                folderInputRef.current?.click();
              }}
              className="w-full flex items-center justify-center gap-3 p-4 bg-gradient-to-r from-violet-500 to-purple-600 text-white rounded-xl font-medium hover:from-violet-600 hover:to-purple-700 transition-all shadow-lg shadow-violet-500/25"
            >
              <FolderSearch className="w-5 h-5" />
              Scan Folder for Compatible Content
            </button>
            <p className="text-xs text-surface-500 text-center mt-2">
              Scan an entire folder to find audiobooks, ebooks, and PDFs
            </p>
          </div>

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
        onClose={() => {
          setIsTorrentModalOpen(false);
          setFoundTorrentFiles([]);
        }}
        title="Torrent Downloads"
        size="lg"
      >
        <div className="space-y-6">
          {/* Browse for torrent files section */}
          <div>
            <h4 className="flex items-center gap-2 font-medium text-surface-900 dark:text-white mb-3">
              <FolderSearch className="w-5 h-5 text-primary-500" />
              Browse for Torrent Files
            </h4>
            <div className="flex gap-3">
              <Button
                variant="secondary"
                className="flex-1"
                onClick={() => torrentInputRef.current?.click()}
              >
                <FileDown className="w-4 h-4" />
                Select .torrent Files
              </Button>
            </div>
            <input
              ref={torrentInputRef}
              type="file"
              accept=".torrent"
              multiple
              onChange={async (e) => {
                const files = Array.from(e.target.files || []);
                if (files.length === 0) return;

                // Auto-close modal and start downloads immediately
                setIsTorrentModalOpen(false);
                setFoundTorrentFiles([]);

                // Start all selected torrents
                for (const file of files) {
                  try {
                    const arrayBuffer = await file.arrayBuffer();
                    const buffer = new Uint8Array(arrayBuffer);
                    startTorrentDownload(buffer, file.name);
                  } catch (error) {
                    console.error('Error reading torrent file:', error);
                  }
                }

                // Reset the input so the same file can be selected again
                e.target.value = '';
              }}
              className="hidden"
            />

            {/* Show found torrent files */}
            {foundTorrentFiles.length > 0 && (
              <div className="mt-4 space-y-2">
                <p className="text-sm text-surface-500">Found {foundTorrentFiles.length} torrent file(s):</p>
                {foundTorrentFiles.map((file, index) => (
                  <div
                    key={index}
                    className="flex items-center justify-between p-3 bg-surface-100 dark:bg-surface-800 rounded-xl"
                  >
                    <div className="flex items-center gap-3">
                      <Magnet className="w-5 h-5 text-primary-500" />
                      <span className="text-sm font-medium truncate max-w-[300px]">{file.name}</span>
                    </div>
                    <Button
                      variant="primary"
                      onClick={async () => {
                        // Close modal immediately when download starts
                        setIsTorrentModalOpen(false);
                        setFoundTorrentFiles([]);

                        // Read the torrent file as ArrayBuffer for WebTorrent
                        try {
                          const arrayBuffer = await file.arrayBuffer();
                          const buffer = new Uint8Array(arrayBuffer);
                          // For Electron, we need to pass the buffer data
                          // WebTorrent can accept Buffer, so we convert it
                          startTorrentDownload(buffer as unknown as string, file.name);
                        } catch (error) {
                          console.error('Error reading torrent file:', error);
                          // Fallback to blob URL
                          const blobUrl = URL.createObjectURL(file);
                          startTorrentDownload(blobUrl, file.name);
                        }
                      }}
                    >
                      <Download className="w-4 h-4" />
                      Download
                    </Button>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="border-t border-surface-200 dark:border-surface-700" />

          {/* Magnet link section */}
          <div>
            <h4 className="flex items-center gap-2 font-medium text-surface-900 dark:text-white mb-3">
              <Magnet className="w-5 h-5 text-primary-500" />
              Enter Magnet Link
            </h4>
            <p className="text-sm text-surface-500 dark:text-surface-400 mb-3">
              Paste a magnet link to download audiobooks or ebooks.
              Only download content you have the right to access.
            </p>
            <input
              type="text"
              placeholder="magnet:?xt=urn:btih:..."
              value={torrentUrl}
              onChange={(e) => setTorrentUrl(e.target.value)}
              className="input"
            />
            <div className="flex gap-3 mt-4">
              <Button
                variant="secondary"
                className="flex-1"
                onClick={() => {
                  setIsTorrentModalOpen(false);
                  setFoundTorrentFiles([]);
                }}
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
        </div>
      </Modal>

      {/* Settings Modal */}
      <Modal
        isOpen={isSettingsModalOpen}
        onClose={() => setIsSettingsModalOpen(false)}
        title={t('appearance')}
        size="lg"
      >
        <div className="space-y-8">
          {/* Color Theme */}
          <div>
            <h3 className="flex items-center gap-2 text-lg font-semibold text-surface-900 dark:text-white mb-4">
              <Palette className="w-5 h-5" />
              {t('colorTheme')}
            </h3>
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
              {(Object.entries(colorThemes) as [ColorTheme, typeof colorThemes[ColorTheme]][]).map(([key, theme]) => (
                <button
                  key={key}
                  onClick={() => setColorTheme(key)}
                  className={`p-4 rounded-xl border-2 transition-all ${
                    colorTheme === key
                      ? 'border-primary-500 bg-primary-500/10'
                      : 'border-surface-200 dark:border-surface-700 hover:border-surface-300 dark:hover:border-surface-600'
                  }`}
                >
                  <div
                    className="w-full h-8 rounded-lg mb-2"
                    style={{
                      background: `linear-gradient(135deg, ${theme.logoStart}, ${theme.logoEnd})`
                    }}
                  />
                  <p className="text-sm font-medium text-surface-900 dark:text-white">
                    {theme.name}
                  </p>
                </button>
              ))}
            </div>
          </div>

          {/* Logo Style */}
          <div>
            <h3 className="flex items-center gap-2 text-lg font-semibold text-surface-900 dark:text-white mb-4">
              <Sparkles className="w-5 h-5" />
              Logo Style
            </h3>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
              {logoVariants.map(({ value, label }) => (
                <button
                  key={value}
                  onClick={() => setLogoVariant(value)}
                  className={`p-4 rounded-xl border-2 transition-all ${
                    logoVariant === value
                      ? 'border-primary-500 bg-primary-500/10'
                      : 'border-surface-200 dark:border-surface-700 hover:border-surface-300 dark:hover:border-surface-600'
                  }`}
                >
                  <div className="flex justify-center mb-2">
                    <RezonLogo size="sm" variant={value} animated={false} />
                  </div>
                  <p className="text-xs font-medium text-surface-900 dark:text-white text-center">
                    {label}
                  </p>
                </button>
              ))}
            </div>
          </div>

          {/* Splash Animation */}
          <div>
            <h3 className="flex items-center gap-2 text-lg font-semibold text-surface-900 dark:text-white mb-4">
              <Play className="w-5 h-5" />
              Splash Animation
            </h3>
            <div className="grid grid-cols-2 gap-3">
              {splashVariants.map(({ value, label }) => (
                <button
                  key={value}
                  onClick={() => setSplashVariant(value)}
                  className={`p-4 rounded-xl border-2 transition-all ${
                    splashVariant === value
                      ? 'border-primary-500 bg-primary-500/10'
                      : 'border-surface-200 dark:border-surface-700 hover:border-surface-300 dark:hover:border-surface-600'
                  }`}
                >
                  <p className="font-medium text-surface-900 dark:text-white">
                    {label}
                  </p>
                  <p className="text-xs text-surface-500 mt-1">
                    {value === 'pulseWave' && 'Clean, modern pulse rings'}
                    {value === 'glitchCyber' && 'Edgy neon glitch effect'}
                    {value === 'waveformMorph' && 'Smooth animated waveforms'}
                    {value === 'neonFlicker' && 'Retro neon sign flicker'}
                  </p>
                </button>
              ))}
            </div>
          </div>

          {/* Preview Splash Button */}
          <div className="pt-4 border-t border-surface-200 dark:border-surface-700">
            <Button
              variant="secondary"
              className="w-full"
              onClick={() => {
                setIsSettingsModalOpen(false);
                useStore.getState().setShowSplash(true);
              }}
            >
              <Play className="w-4 h-4" />
              Preview Splash Animation
            </Button>
          </div>
        </div>
      </Modal>

      {/* Scan Results Modal */}
      <Modal
        isOpen={isScanModalOpen}
        onClose={() => {
          setIsScanModalOpen(false);
          setScannedFiles([]);
        }}
        title="Scan Results"
        size="lg"
      >
        <div className="space-y-4">
          {isScanning ? (
            <div className="flex flex-col items-center py-12">
              <Loader2 className="w-12 h-12 text-primary-500 animate-spin mb-4" />
              <p className="text-surface-600 dark:text-surface-400">Scanning folder...</p>
            </div>
          ) : scannedFiles.length === 0 ? (
            <div className="text-center py-12">
              <FolderSearch className="w-16 h-16 mx-auto text-surface-400 mb-4" />
              <p className="text-surface-600 dark:text-surface-400">No compatible files found</p>
              <p className="text-sm text-surface-500 mt-2">Try scanning a different folder</p>
            </div>
          ) : (
            <>
              {/* Stats */}
              <div className="flex items-center justify-between p-4 bg-surface-100 dark:bg-surface-800 rounded-xl">
                <div>
                  <p className="text-sm text-surface-500">Found</p>
                  <p className="text-2xl font-bold text-surface-900 dark:text-white">
                    {scannedFiles.length} files
                  </p>
                </div>
                <div className="flex gap-4 text-sm">
                  <div className="flex items-center gap-2">
                    <Headphones className="w-4 h-4 text-cyan-500" />
                    <span className="text-surface-600 dark:text-surface-400">
                      {scannedFiles.filter(f => f.format === 'audio').length} audio
                    </span>
                  </div>
                  <div className="flex items-center gap-2">
                    <BookOpen className="w-4 h-4 text-violet-500" />
                    <span className="text-surface-600 dark:text-surface-400">
                      {scannedFiles.filter(f => f.format === 'epub').length} epub
                    </span>
                  </div>
                  <div className="flex items-center gap-2">
                    <FileText className="w-4 h-4 text-orange-500" />
                    <span className="text-surface-600 dark:text-surface-400">
                      {scannedFiles.filter(f => f.format === 'pdf').length} pdf
                    </span>
                  </div>
                </div>
              </div>

              {/* Select all / none */}
              <div className="flex items-center justify-between">
                <p className="text-sm text-surface-500">
                  {scannedFiles.filter(f => f.selected).length} selected
                </p>
                <div className="flex gap-2">
                  <button
                    onClick={() => toggleAllScannedFiles(true)}
                    className="text-sm text-primary-500 hover:text-primary-600"
                  >
                    Select all
                  </button>
                  <span className="text-surface-300">|</span>
                  <button
                    onClick={() => toggleAllScannedFiles(false)}
                    className="text-sm text-surface-500 hover:text-surface-600"
                  >
                    Select none
                  </button>
                </div>
              </div>

              {/* File list */}
              <div className="max-h-64 overflow-y-auto space-y-2">
                {scannedFiles.map((file, index) => (
                  <div
                    key={index}
                    onClick={() => toggleScannedFile(index)}
                    className={`flex items-center gap-3 p-3 rounded-xl cursor-pointer transition-colors ${
                      file.selected
                        ? 'bg-primary-500/10 border border-primary-500/30'
                        : 'bg-surface-100 dark:bg-surface-800 border border-transparent'
                    }`}
                  >
                    <div className={`w-5 h-5 rounded-md border-2 flex items-center justify-center transition-colors ${
                      file.selected
                        ? 'bg-primary-500 border-primary-500'
                        : 'border-surface-300 dark:border-surface-600'
                    }`}>
                      {file.selected && <CheckCircle className="w-3.5 h-3.5 text-white" />}
                    </div>
                    {file.format === 'audio' && <Headphones className="w-4 h-4 text-cyan-500" />}
                    {file.format === 'epub' && <BookOpen className="w-4 h-4 text-violet-500" />}
                    {file.format === 'pdf' && <FileText className="w-4 h-4 text-orange-500" />}
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium text-surface-900 dark:text-white truncate">
                        {file.file.name}
                      </p>
                      <p className="text-xs text-surface-500 truncate">{file.path}</p>
                    </div>
                  </div>
                ))}
              </div>

              {/* Import button */}
              <div className="flex gap-3 pt-4 border-t border-surface-200 dark:border-surface-700">
                <Button
                  variant="secondary"
                  className="flex-1"
                  onClick={() => {
                    setIsScanModalOpen(false);
                    setScannedFiles([]);
                  }}
                >
                  Cancel
                </Button>
                <Button
                  variant="primary"
                  className="flex-1"
                  onClick={importScannedFiles}
                  disabled={scannedFiles.filter(f => f.selected).length === 0 || isLoadingMetadata}
                >
                  {isLoadingMetadata ? (
                    <>
                      <Loader2 className="w-4 h-4 animate-spin" />
                      Importing...
                    </>
                  ) : (
                    <>
                      <Download className="w-4 h-4" />
                      Import {scannedFiles.filter(f => f.selected).length} Files
                    </>
                  )}
                </Button>
              </div>
            </>
          )}
        </div>
      </Modal>
    </div>
  );
}
