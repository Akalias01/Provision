import { useEffect, useRef, useState, useCallback } from 'react';
import { motion } from 'framer-motion';
import mammoth from 'mammoth';
import {
  ChevronUp,
  ChevronDown,
  Settings,
  FileText,
  Volume2,
  Pause,
  Home,
  Sun,
  Moon,
  Type,
  Bookmark,
  BookmarkPlus,
  Trash2,
  Edit3,
  Check,
  Clock,
  List,
  BookOpen,
} from 'lucide-react';
import { Button, Slider, Modal } from '../ui';
import { useStore } from '../../store/useStore';

export function DocReader() {
  const containerRef = useRef<HTMLDivElement>(null);
  const contentRef = useRef<HTMLDivElement>(null);

  const {
    currentBook,
    readerSettings,
    setReaderSettings,
    ttsState,
    setTTSState,
    updateBook,
    setCurrentView,
    addBookmark,
    removeBookmark,
    updateBookmarkNote,
  } = useStore();

  const [htmlContent, setHtmlContent] = useState<string>('');
  const [plainText, setPlainText] = useState<string>('');
  const [showSettings, setShowSettings] = useState(false);
  const [showBookmarks, setShowBookmarks] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [bookmarkNote, setBookmarkNote] = useState('');
  const [editingBookmarkId, setEditingBookmarkId] = useState<string | null>(null);
  const [editingNote, setEditingNote] = useState('');
  const [viewMode, setViewMode] = useState<'paginated' | 'scrolled'>('scrolled');
  const [availableVoices, setAvailableVoices] = useState<SpeechSynthesisVoice[]>([]);
  const [selectedVoice, setSelectedVoice] = useState<string>('');
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [scrollProgress, setScrollProgress] = useState(0);

  const { fontSize, fontFamily, lineHeight, theme, marginSize } = readerSettings;
  const { isReading } = ttsState;

  // Load available voices
  useEffect(() => {
    const loadVoices = () => {
      const voices = window.speechSynthesis.getVoices();
      const englishVoices = voices.filter(v => v.lang.startsWith('en'));
      const sortedVoices = englishVoices.sort((a, b) => {
        const aScore = (a.name.includes('Natural') || a.name.includes('Premium') || a.name.includes('Enhanced')) ? 1 : 0;
        const bScore = (b.name.includes('Natural') || b.name.includes('Premium') || b.name.includes('Enhanced')) ? 1 : 0;
        return bScore - aScore;
      });
      setAvailableVoices(sortedVoices.length > 0 ? sortedVoices : voices);
      if (sortedVoices.length > 0 && !selectedVoice) {
        setSelectedVoice(sortedVoices[0].name);
      }
    };

    loadVoices();
    window.speechSynthesis.onvoiceschanged = loadVoices;

    return () => {
      window.speechSynthesis.onvoiceschanged = null;
    };
  }, [selectedVoice]);

  // Load document
  useEffect(() => {
    if (!currentBook) return;

    setIsLoading(true);
    setError(null);

    const loadDocument = async () => {
      try {
        // Fetch the file
        const response = await fetch(currentBook.fileUrl);
        const arrayBuffer = await response.arrayBuffer();

        // Convert DOCX to HTML using mammoth
        const result = await mammoth.convertToHtml({ arrayBuffer });
        setHtmlContent(result.value);

        // Also get plain text for TTS
        const textResult = await mammoth.extractRawText({ arrayBuffer });
        setPlainText(textResult.value);

        // Calculate approximate pages (assuming ~3000 chars per page)
        const estimatedPages = Math.max(1, Math.ceil(textResult.value.length / 3000));
        setTotalPages(estimatedPages);

        // Restore position
        if (currentBook.currentPosition > 0 && containerRef.current) {
          setTimeout(() => {
            if (containerRef.current) {
              const scrollHeight = containerRef.current.scrollHeight - containerRef.current.clientHeight;
              containerRef.current.scrollTop = scrollHeight * currentBook.currentPosition;
            }
          }, 100);
        }

        setIsLoading(false);
      } catch (err) {
        console.error('Error loading document:', err);
        setError('Failed to load this document. The file may be corrupted or in an unsupported format.');
        setIsLoading(false);
      }
    };

    loadDocument();
  }, [currentBook?.fileUrl]);

  // Track scroll position
  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    const handleScroll = () => {
      const scrollHeight = container.scrollHeight - container.clientHeight;
      const progress = scrollHeight > 0 ? container.scrollTop / scrollHeight : 0;
      setScrollProgress(progress);
      setCurrentPage(Math.max(1, Math.ceil(progress * totalPages)));

      // Update book position
      if (currentBook) {
        updateBook(currentBook.id, { currentPosition: progress });
      }
    };

    container.addEventListener('scroll', handleScroll);
    return () => container.removeEventListener('scroll', handleScroll);
  }, [currentBook, totalPages, updateBook]);

  // Keyboard navigation
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (!containerRef.current) return;

      if (e.key === 'ArrowDown' || e.key === ' ') {
        e.preventDefault();
        containerRef.current.scrollBy({ top: 100, behavior: 'smooth' });
      } else if (e.key === 'ArrowUp') {
        e.preventDefault();
        containerRef.current.scrollBy({ top: -100, behavior: 'smooth' });
      } else if (e.key === 'PageDown') {
        e.preventDefault();
        containerRef.current.scrollBy({ top: containerRef.current.clientHeight * 0.9, behavior: 'smooth' });
      } else if (e.key === 'PageUp') {
        e.preventDefault();
        containerRef.current.scrollBy({ top: -containerRef.current.clientHeight * 0.9, behavior: 'smooth' });
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  // Page navigation for paginated mode
  const goNextPage = useCallback(() => {
    if (!containerRef.current) return;
    containerRef.current.scrollBy({ top: containerRef.current.clientHeight * 0.95, behavior: 'smooth' });
  }, []);

  const goPrevPage = useCallback(() => {
    if (!containerRef.current) return;
    containerRef.current.scrollBy({ top: -containerRef.current.clientHeight * 0.95, behavior: 'smooth' });
  }, []);

  // TTS functionality
  const toggleTTS = useCallback(() => {
    if (isReading) {
      window.speechSynthesis.cancel();
      setTTSState({ isReading: false });
    } else {
      if (plainText) {
        // Get text from current position
        const startIndex = Math.floor(scrollProgress * plainText.length);
        const textToRead = plainText.slice(startIndex, startIndex + 5000);

        const utterance = new SpeechSynthesisUtterance(textToRead);
        utterance.rate = ttsState.rate;
        utterance.pitch = ttsState.pitch;

        if (selectedVoice) {
          const voice = availableVoices.find(v => v.name === selectedVoice);
          if (voice) {
            utterance.voice = voice;
          }
        }

        utterance.onend = () => setTTSState({ isReading: false });
        window.speechSynthesis.speak(utterance);
        setTTSState({ isReading: true });
      }
    }
  }, [isReading, plainText, scrollProgress, ttsState.rate, ttsState.pitch, selectedVoice, availableVoices, setTTSState]);

  // Bookmark functionality
  const handleAddBookmark = useCallback(() => {
    if (!currentBook) return;
    const bookmark = {
      id: crypto.randomUUID(),
      position: scrollProgress,
      note: bookmarkNote || undefined,
      createdAt: new Date(),
    };
    addBookmark(currentBook.id, bookmark);
    setBookmarkNote('');
  }, [currentBook, scrollProgress, bookmarkNote, addBookmark]);

  const handleJumpToBookmark = useCallback((position: number) => {
    if (containerRef.current) {
      const scrollHeight = containerRef.current.scrollHeight - containerRef.current.clientHeight;
      containerRef.current.scrollTo({ top: scrollHeight * position, behavior: 'smooth' });
    }
    setShowBookmarks(false);
  }, []);

  if (!currentBook) return null;

  const bookmarks = currentBook.bookmarks || [];

  const themeClasses = {
    light: 'bg-white text-surface-900',
    dark: 'bg-surface-950 text-surface-100',
    sepia: 'bg-[#f4ecd8] text-[#5c4b37]',
  };

  const themeStyles = {
    light: { background: '#ffffff', color: '#1a1a1a' },
    dark: { background: '#09090b', color: '#f4f4f5' },
    sepia: { background: '#f4ecd8', color: '#5c4b37' },
  };

  return (
    <div className={`h-full flex flex-col ${themeClasses[theme]}`}>
      {/* Header */}
      <motion.header
        initial={{ y: -20, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        className="flex items-center justify-between p-4 border-b border-surface-200/20"
      >
        <Button variant="ghost" onClick={() => setCurrentView('library')}>
          <Home className="w-5 h-5" />
        </Button>

        <div className="flex-1 text-center px-4">
          <h1 className="text-sm font-medium truncate">{currentBook.title}</h1>
          <p className="text-xs opacity-60">
            Page {currentPage} of {totalPages} • {Math.round(scrollProgress * 100)}%
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Button variant="ghost" onClick={() => setShowBookmarks(true)}>
            <Bookmark className="w-5 h-5" />
          </Button>
          <Button variant="ghost" onClick={() => setShowSettings(true)}>
            <Settings className="w-5 h-5" />
          </Button>
        </div>
      </motion.header>

      {/* Document Viewer */}
      <div className="flex-1 relative overflow-hidden">
        {isLoading && (
          <div className="absolute inset-0 flex items-center justify-center">
            <motion.div
              animate={{ rotate: 360 }}
              transition={{ duration: 1, repeat: Infinity, ease: 'linear' }}
              className="w-8 h-8 border-2 border-primary-500 border-t-transparent rounded-full"
            />
          </div>
        )}

        {error && (
          <div className="absolute inset-0 flex items-center justify-center p-8">
            <div className="text-center">
              <FileText className="w-16 h-16 mx-auto mb-4 text-surface-400" />
              <h3 className="text-lg font-semibold mb-2">Unable to Load Document</h3>
              <p className="text-surface-500 mb-4">{error}</p>
              <Button variant="secondary" onClick={() => setCurrentView('library')}>
                Back to Library
              </Button>
            </div>
          </div>
        )}

        <div
          ref={containerRef}
          className={`h-full overflow-auto ${error ? 'hidden' : ''}`}
          style={{
            ...themeStyles[theme],
            scrollSnapType: viewMode === 'paginated' ? 'y mandatory' : 'none',
          }}
        >
          <div
            ref={contentRef}
            className="max-w-4xl mx-auto px-4 py-8 doc-content"
            style={{
              fontSize: `${fontSize}px`,
              fontFamily: fontFamily,
              lineHeight: lineHeight,
              padding: `2rem ${marginSize}px`,
            }}
            dangerouslySetInnerHTML={{ __html: htmlContent }}
          />
        </div>

        {/* Navigation buttons for paginated mode */}
        {viewMode === 'paginated' && (
          <>
            <button
              onClick={goPrevPage}
              className="absolute top-4 left-1/2 -translate-x-1/2 p-2 rounded-full bg-surface-900/10 dark:bg-white/10 hover:bg-surface-900/20 dark:hover:bg-white/20 transition-colors"
            >
              <ChevronUp className="w-6 h-6" />
            </button>
            <button
              onClick={goNextPage}
              className="absolute bottom-20 left-1/2 -translate-x-1/2 p-2 rounded-full bg-surface-900/10 dark:bg-white/10 hover:bg-surface-900/20 dark:hover:bg-white/20 transition-colors"
            >
              <ChevronDown className="w-6 h-6" />
            </button>
          </>
        )}
      </div>

      {/* Footer controls */}
      <motion.footer
        initial={{ y: 20, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        className="p-4 border-t border-surface-200/20"
      >
        <div className="flex items-center justify-center gap-4">
          {/* Prominent Read Aloud button */}
          <button
            onClick={toggleTTS}
            className={`flex items-center gap-2 px-4 py-2 rounded-xl font-medium transition-all ${
              isReading
                ? 'bg-primary-500 text-white shadow-lg shadow-primary-500/30'
                : 'bg-surface-100 dark:bg-surface-800 hover:bg-surface-200 dark:hover:bg-surface-700'
            }`}
          >
            {isReading ? (
              <>
                <Pause className="w-5 h-5" />
                <span>Stop</span>
              </>
            ) : (
              <>
                <Volume2 className="w-5 h-5" />
                <span>Read Aloud</span>
              </>
            )}
          </button>

          <div className="flex-1 max-w-md">
            <Slider
              value={scrollProgress * 100}
              max={100}
              onChange={(v) => {
                if (containerRef.current) {
                  const scrollHeight = containerRef.current.scrollHeight - containerRef.current.clientHeight;
                  containerRef.current.scrollTo({ top: scrollHeight * (v / 100), behavior: 'smooth' });
                }
              }}
            />
          </div>

          <span className="text-sm opacity-60 min-w-[60px] text-center">
            {Math.round(scrollProgress * 100)}%
          </span>
        </div>
      </motion.footer>

      {/* Settings Modal */}
      <Modal isOpen={showSettings} onClose={() => setShowSettings(false)} title="Document Settings">
        <div className="space-y-6">
          {/* View Mode */}
          <div>
            <label className="text-sm font-medium mb-3 block">View Mode</label>
            <div className="flex gap-3">
              <button
                onClick={() => setViewMode('paginated')}
                className={`flex-1 p-3 rounded-xl border-2 transition-all ${
                  viewMode === 'paginated'
                    ? 'border-primary-500 bg-primary-500/10'
                    : 'border-surface-200 dark:border-surface-700'
                }`}
              >
                <BookOpen className="w-5 h-5 mx-auto mb-1" />
                <span className="text-xs">Page Flip</span>
              </button>
              <button
                onClick={() => setViewMode('scrolled')}
                className={`flex-1 p-3 rounded-xl border-2 transition-all ${
                  viewMode === 'scrolled'
                    ? 'border-primary-500 bg-primary-500/10'
                    : 'border-surface-200 dark:border-surface-700'
                }`}
              >
                <List className="w-5 h-5 mx-auto mb-1" />
                <span className="text-xs">Scroll</span>
              </button>
            </div>
          </div>

          {/* Theme */}
          <div>
            <label className="text-sm font-medium mb-3 block">Theme</label>
            <div className="flex gap-3">
              {[
                { value: 'light', icon: Sun, label: 'Light' },
                { value: 'dark', icon: Moon, label: 'Dark' },
                { value: 'sepia', icon: BookOpen, label: 'Sepia' },
              ].map(({ value, icon: Icon, label }) => (
                <button
                  key={value}
                  onClick={() => setReaderSettings({ theme: value as typeof theme })}
                  className={`flex-1 p-3 rounded-xl border-2 transition-all ${
                    theme === value
                      ? 'border-primary-500 bg-primary-500/10'
                      : 'border-surface-200 dark:border-surface-700'
                  }`}
                >
                  <Icon className="w-5 h-5 mx-auto mb-1" />
                  <span className="text-xs">{label}</span>
                </button>
              ))}
            </div>
          </div>

          {/* Font Size */}
          <div>
            <label className="text-sm font-medium mb-3 flex items-center gap-2">
              <Type className="w-4 h-4" />
              Font Size: {fontSize}px
            </label>
            <Slider
              value={fontSize}
              min={12}
              max={32}
              onChange={(v) => setReaderSettings({ fontSize: v })}
            />
          </div>

          {/* Line Height */}
          <div>
            <label className="text-sm font-medium mb-3 block">
              Line Height: {lineHeight}
            </label>
            <Slider
              value={lineHeight}
              min={1.2}
              max={2.5}
              step={0.1}
              onChange={(v) => setReaderSettings({ lineHeight: v })}
            />
          </div>

          {/* TTS Settings */}
          <div className="border-t border-surface-200 dark:border-surface-700 pt-6">
            <div className="flex items-center gap-2 mb-4">
              <Volume2 className="w-5 h-5 text-primary-500" />
              <h3 className="font-medium">Text-to-Speech Settings</h3>
            </div>

            <div className="space-y-4">
              {/* Voice Selection */}
              <div>
                <label className="text-sm font-medium mb-2 block">Voice</label>
                <select
                  value={selectedVoice}
                  onChange={(e) => setSelectedVoice(e.target.value)}
                  className="w-full p-3 rounded-xl bg-surface-100 dark:bg-surface-800 border-none text-sm focus:ring-2 focus:ring-primary-500"
                >
                  {availableVoices.length === 0 ? (
                    <option value="">Loading voices...</option>
                  ) : (
                    availableVoices.map((voice) => (
                      <option key={voice.name} value={voice.name}>
                        {voice.name} ({voice.lang})
                      </option>
                    ))
                  )}
                </select>
                <p className="text-xs text-surface-500 mt-1">
                  {availableVoices.length} voices available. Premium voices provide more natural speech.
                </p>
              </div>

              <div>
                <label className="text-sm font-medium mb-2 flex justify-between">
                  <span>Speed</span>
                  <span className="text-primary-500">{ttsState.rate}x</span>
                </label>
                <Slider
                  value={ttsState.rate}
                  min={0.5}
                  max={2}
                  step={0.1}
                  onChange={(v) => setTTSState({ rate: v })}
                />
              </div>

              <div>
                <label className="text-sm font-medium mb-2 flex justify-between">
                  <span>Pitch</span>
                  <span className="text-primary-500">{ttsState.pitch}</span>
                </label>
                <Slider
                  value={ttsState.pitch}
                  min={0.5}
                  max={2}
                  step={0.1}
                  onChange={(v) => setTTSState({ pitch: v })}
                />
              </div>
            </div>
          </div>
        </div>
      </Modal>

      {/* Bookmarks Modal */}
      <Modal isOpen={showBookmarks} onClose={() => setShowBookmarks(false)} title="Bookmarks">
        <div className="space-y-4">
          {/* Add new bookmark */}
          <div className="flex gap-2">
            <input
              type="text"
              placeholder="Add note (optional)..."
              value={bookmarkNote}
              onChange={(e) => setBookmarkNote(e.target.value)}
              className="input flex-1"
            />
            <Button variant="primary" onClick={handleAddBookmark}>
              <BookmarkPlus className="w-4 h-4" />
              Add
            </Button>
          </div>

          {/* Bookmark list */}
          <div className="space-y-2 max-h-64 overflow-y-auto">
            {bookmarks.length === 0 ? (
              <p className="text-center text-surface-500 py-8">No bookmarks yet</p>
            ) : (
              bookmarks
                .sort((a, b) => a.position - b.position)
                .map((bookmark) => (
                  <div
                    key={bookmark.id}
                    className="flex items-center gap-3 p-3 bg-surface-100 dark:bg-surface-800 rounded-xl"
                  >
                    <button
                      onClick={() => handleJumpToBookmark(bookmark.position)}
                      className="flex-1 text-left"
                    >
                      <div className="flex items-center gap-2">
                        <Clock className="w-4 h-4 text-primary-500" />
                        <span className="font-medium text-surface-900 dark:text-white">
                          {Math.round(bookmark.position * 100)}% through document
                        </span>
                      </div>
                      {editingBookmarkId === bookmark.id ? (
                        <div className="flex items-center gap-2 mt-1">
                          <input
                            type="text"
                            value={editingNote}
                            onChange={(e) => setEditingNote(e.target.value)}
                            className="input text-sm py-1 flex-1"
                            autoFocus
                            onClick={(e) => e.stopPropagation()}
                          />
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              if (currentBook) {
                                updateBookmarkNote(currentBook.id, bookmark.id, editingNote);
                              }
                              setEditingBookmarkId(null);
                            }}
                            className="p-1 text-green-500 hover:bg-green-500/10 rounded"
                          >
                            <Check className="w-4 h-4" />
                          </button>
                        </div>
                      ) : bookmark.note ? (
                        <p className="text-sm text-surface-500 mt-1">{bookmark.note}</p>
                      ) : null}
                    </button>
                    <div className="flex items-center gap-1">
                      <button
                        onClick={() => {
                          setEditingBookmarkId(bookmark.id);
                          setEditingNote(bookmark.note || '');
                        }}
                        className="p-2 text-surface-400 hover:text-primary-500 hover:bg-primary-500/10 rounded-lg"
                      >
                        <Edit3 className="w-4 h-4" />
                      </button>
                      <button
                        onClick={() => currentBook && removeBookmark(currentBook.id, bookmark.id)}
                        className="p-2 text-surface-400 hover:text-red-500 hover:bg-red-500/10 rounded-lg"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                ))
            )}
          </div>
        </div>
      </Modal>

      {/* Custom styles for document content */}
      <style>{`
        .doc-content h1, .doc-content h2, .doc-content h3, .doc-content h4, .doc-content h5, .doc-content h6 {
          font-weight: bold;
          margin-top: 1.5em;
          margin-bottom: 0.5em;
        }
        .doc-content h1 { font-size: 2em; }
        .doc-content h2 { font-size: 1.5em; }
        .doc-content h3 { font-size: 1.25em; }
        .doc-content p {
          margin-bottom: 1em;
        }
        .doc-content ul, .doc-content ol {
          margin-left: 1.5em;
          margin-bottom: 1em;
        }
        .doc-content li {
          margin-bottom: 0.25em;
        }
        .doc-content table {
          width: 100%;
          border-collapse: collapse;
          margin-bottom: 1em;
        }
        .doc-content th, .doc-content td {
          border: 1px solid currentColor;
          padding: 0.5em;
          opacity: 0.3;
        }
        .doc-content th {
          background: currentColor;
          opacity: 0.1;
        }
        .doc-content img {
          max-width: 100%;
          height: auto;
        }
        .doc-content a {
          color: inherit;
          text-decoration: underline;
        }
      `}</style>
    </div>
  );
}
