import { useEffect, useRef, useState, useCallback } from 'react';
import { motion } from 'framer-motion';
import ePub, { Book as EpubBook, Rendition } from 'epubjs';
import {
  ChevronLeft,
  ChevronRight,
  Settings,
  BookOpen,
  Volume2,
  Pause,
  Home,
  List,
  Sun,
  Moon,
  Type,
} from 'lucide-react';
import { Button, Slider, Modal } from '../ui';
import { useStore } from '../../store/useStore';

export function EpubReader() {
  const containerRef = useRef<HTMLDivElement>(null);
  const bookRef = useRef<EpubBook | null>(null);
  const renditionRef = useRef<Rendition | null>(null);

  const {
    currentBook,
    readerSettings,
    setReaderSettings,
    ttsState,
    setTTSState,
    updateBook,
    setCurrentView,
  } = useStore();

  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(0);
  const [showSettings, setShowSettings] = useState(false);
  const [showToc, setShowToc] = useState(false);
  const [toc, setToc] = useState<{ label: string; href: string }[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const { fontSize, fontFamily, lineHeight, theme, marginSize } = readerSettings;
  const { isReading } = ttsState;

  useEffect(() => {
    if (!containerRef.current || !currentBook) return;

    setIsLoading(true);
    setError(null);

    const initBook = async () => {
      try {
        // Fetch the file as ArrayBuffer for better compatibility
        let bookData: ArrayBuffer | string = currentBook.fileUrl;

        if (currentBook.fileUrl.startsWith('blob:')) {
          try {
            const response = await fetch(currentBook.fileUrl);
            bookData = await response.arrayBuffer();
          } catch (fetchErr) {
            console.warn('Could not fetch blob, using URL directly:', fetchErr);
          }
        }

        const book = ePub(bookData);
        bookRef.current = book;

        // Wait for book to be ready before rendering
        await book.ready;

        const rendition = book.renderTo(containerRef.current!, {
          width: '100%',
          height: '100%',
          spread: 'none',
          flow: 'paginated',
          allowScriptedContent: true,
        });

        renditionRef.current = rendition;

        // Apply theme
        applyTheme(rendition);

        // Load book
        await rendition.display();
        setIsLoading(false);

        // Get TOC
        try {
          const nav = await book.loaded.navigation;
          setToc(nav.toc.map((item) => ({ label: item.label, href: item.href })));
        } catch (e) {
          console.warn('Could not load TOC:', e);
        }

        // Track pagination
        try {
          await book.locations.generate(1024);
          setTotalPages(book.locations.length());
        } catch (e) {
          console.warn('Could not generate locations:', e);
        }

        // Track location changes
        rendition.on('locationChanged', (location: { start: { percentage: number } }) => {
          const percentage = location.start.percentage;
          if (bookRef.current?.locations) {
            const page = Math.ceil(percentage * (bookRef.current.locations.length() || 1));
            setCurrentPage(page || 1);
            updateBook(currentBook.id, { currentPosition: percentage });
          }
        });

      } catch (err) {
        console.error('Error loading EPUB:', err);
        setError('Failed to load this EPUB file. The file may be corrupted or in an unsupported format.');
        setIsLoading(false);
      }
    };

    initBook();

    return () => {
      if (renditionRef.current) {
        renditionRef.current.destroy();
      }
      if (bookRef.current) {
        bookRef.current.destroy();
      }
    };
  }, [currentBook?.fileUrl]);

  useEffect(() => {
    if (renditionRef.current) {
      applyTheme(renditionRef.current);
    }
  }, [fontSize, fontFamily, lineHeight, theme, marginSize]);

  const applyTheme = (rendition: Rendition) => {
    const themes: Record<string, { body: Record<string, string> }> = {
      light: {
        body: {
          background: '#ffffff',
          color: '#1a1a1a',
        },
      },
      dark: {
        body: {
          background: '#18181b',
          color: '#f4f4f5',
        },
      },
      sepia: {
        body: {
          background: '#f4ecd8',
          color: '#5c4b37',
        },
      },
    };

    rendition.themes.default({
      body: {
        ...themes[theme].body,
        'font-size': `${fontSize}px`,
        'font-family': fontFamily,
        'line-height': `${lineHeight}`,
        padding: `0 ${marginSize}px`,
      },
      p: {
        'font-size': `${fontSize}px`,
        'line-height': `${lineHeight}`,
      },
    });
  };

  const goNext = useCallback(() => {
    renditionRef.current?.next();
  }, []);

  const goPrev = useCallback(() => {
    renditionRef.current?.prev();
  }, []);

  const goToChapter = useCallback((href: string) => {
    renditionRef.current?.display(href);
    setShowToc(false);
  }, []);

  // Keyboard navigation
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'ArrowRight' || e.key === ' ') {
        goNext();
      } else if (e.key === 'ArrowLeft') {
        goPrev();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [goNext, goPrev]);

  // TTS functionality
  const toggleTTS = useCallback(() => {
    if (isReading) {
      window.speechSynthesis.cancel();
      setTTSState({ isReading: false });
    } else {
      // Get visible text from current view - use type assertion for epubjs Contents
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const contents = renditionRef.current?.getContents() as any;
      const content = contents?.[0];
      const selection = content?.document?.body?.textContent as string | undefined;
      if (selection) {
        const utterance = new SpeechSynthesisUtterance(selection.slice(0, 5000));
        utterance.rate = ttsState.rate;
        utterance.pitch = ttsState.pitch;
        utterance.onend = () => setTTSState({ isReading: false });
        window.speechSynthesis.speak(utterance);
        setTTSState({ isReading: true });
      }
    }
  }, [isReading, ttsState.rate, ttsState.pitch]);

  if (!currentBook) return null;

  const themeClasses = {
    light: 'bg-white text-surface-900',
    dark: 'bg-surface-950 text-surface-100',
    sepia: 'bg-[#f4ecd8] text-[#5c4b37]',
  };

  return (
    <div className={`h-full flex flex-col ${themeClasses[theme]}`}>
      {/* Header */}
      <motion.header
        initial={{ y: -20, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        className="flex items-center justify-between p-4 border-b border-surface-200/20"
      >
        <Button
          variant="ghost"
          onClick={() => setCurrentView('library')}
        >
          <Home className="w-5 h-5" />
        </Button>

        <div className="flex-1 text-center px-4">
          <h1 className="text-sm font-medium truncate">{currentBook.title}</h1>
          <p className="text-xs opacity-60">
            {currentPage} of {totalPages} pages
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Button variant="ghost" onClick={() => setShowToc(true)}>
            <List className="w-5 h-5" />
          </Button>
          <Button variant="ghost" onClick={() => setShowSettings(true)}>
            <Settings className="w-5 h-5" />
          </Button>
        </div>
      </motion.header>

      {/* Reader */}
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
              <BookOpen className="w-16 h-16 mx-auto mb-4 text-surface-400" />
              <h3 className="text-lg font-semibold mb-2">Unable to Load Book</h3>
              <p className="text-surface-500 mb-4">{error}</p>
              <Button variant="secondary" onClick={() => setCurrentView('library')}>
                Back to Library
              </Button>
            </div>
          </div>
        )}

        <div
          ref={containerRef}
          className={`h-full ${error ? 'hidden' : ''}`}
          onClick={(e) => {
            const rect = e.currentTarget.getBoundingClientRect();
            const x = e.clientX - rect.left;
            if (x < rect.width / 3) {
              goPrev();
            } else if (x > (rect.width * 2) / 3) {
              goNext();
            }
          }}
        />

        {/* Navigation arrows */}
        <button
          onClick={goPrev}
          className="absolute left-2 top-1/2 -translate-y-1/2 p-2 rounded-full bg-surface-900/10 dark:bg-white/10 hover:bg-surface-900/20 dark:hover:bg-white/20 transition-colors"
        >
          <ChevronLeft className="w-6 h-6" />
        </button>
        <button
          onClick={goNext}
          className="absolute right-2 top-1/2 -translate-y-1/2 p-2 rounded-full bg-surface-900/10 dark:bg-white/10 hover:bg-surface-900/20 dark:hover:bg-white/20 transition-colors"
        >
          <ChevronRight className="w-6 h-6" />
        </button>
      </div>

      {/* Footer controls */}
      <motion.footer
        initial={{ y: 20, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        className="p-4 border-t border-surface-200/20"
      >
        <div className="flex items-center justify-center gap-4">
          <Button variant="ghost" onClick={toggleTTS}>
            {isReading ? (
              <Pause className="w-5 h-5" />
            ) : (
              <Volume2 className="w-5 h-5" />
            )}
          </Button>

          <div className="flex-1 max-w-md">
            <Slider
              value={currentPage}
              max={totalPages || 100}
              onChange={(page) => {
                if (bookRef.current?.locations) {
                  const cfi = bookRef.current.locations.cfiFromPercentage(page / totalPages);
                  renditionRef.current?.display(cfi);
                }
              }}
            />
          </div>

          <span className="text-sm opacity-60 min-w-[80px] text-center">
            {Math.round((currentPage / totalPages) * 100 || 0)}%
          </span>
        </div>
      </motion.footer>

      {/* Settings Modal */}
      <Modal isOpen={showSettings} onClose={() => setShowSettings(false)} title="Reading Settings">
        <div className="space-y-6">
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
          <div>
            <label className="text-sm font-medium mb-3 block">TTS Speed</label>
            <Slider
              value={ttsState.rate}
              min={0.5}
              max={2}
              step={0.1}
              onChange={(v) => setTTSState({ rate: v })}
            />
          </div>
        </div>
      </Modal>

      {/* TOC Modal */}
      <Modal isOpen={showToc} onClose={() => setShowToc(false)} title="Table of Contents">
        <div className="max-h-96 overflow-y-auto space-y-1">
          {toc.map((item, index) => (
            <button
              key={index}
              onClick={() => goToChapter(item.href)}
              className="w-full text-left px-4 py-3 rounded-xl hover:bg-surface-100 dark:hover:bg-surface-800 transition-colors"
            >
              {item.label}
            </button>
          ))}
        </div>
      </Modal>
    </div>
  );
}
