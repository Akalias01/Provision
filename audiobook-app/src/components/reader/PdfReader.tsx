import { useEffect, useRef, useState, useCallback } from 'react';
import { motion } from 'framer-motion';
import * as pdfjsLib from 'pdfjs-dist';
import {
  ChevronLeft,
  ChevronRight,
  ZoomIn,
  ZoomOut,
  Home,
  Volume2,
  Pause,
  RotateCw,
  Settings,
} from 'lucide-react';
import { Button, Slider, Modal } from '../ui';
import { useStore } from '../../store/useStore';

// Set worker path
pdfjsLib.GlobalWorkerOptions.workerSrc = `https://cdnjs.cloudflare.com/ajax/libs/pdf.js/${pdfjsLib.version}/pdf.worker.min.js`;

export function PdfReader() {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const pdfDocRef = useRef<pdfjsLib.PDFDocumentProxy | null>(null);

  const {
    currentBook,
    ttsState,
    setTTSState,
    updateBook,
    setCurrentView,
    readerSettings,
  } = useStore();

  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(0);
  const [scale, setScale] = useState(1.5);
  const [rotation, setRotation] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [pageText, setPageText] = useState('');
  const [showSettings, setShowSettings] = useState(false);

  const { isReading } = ttsState;
  const { theme } = readerSettings;

  useEffect(() => {
    if (!currentBook) return;

    setIsLoading(true);

    const loadPdf = async () => {
      try {
        const loadingTask = pdfjsLib.getDocument(currentBook.fileUrl);
        const pdf = await loadingTask.promise;
        pdfDocRef.current = pdf;
        setTotalPages(pdf.numPages);

        // Restore position
        const savedPage = Math.ceil(currentBook.currentPosition * pdf.numPages);
        setCurrentPage(savedPage || 1);
        setIsLoading(false);
      } catch (error) {
        console.error('Error loading PDF:', error);
        setIsLoading(false);
      }
    };

    loadPdf();

    return () => {
      pdfDocRef.current?.destroy();
    };
  }, [currentBook?.fileUrl]);

  const renderPage = useCallback(async (pageNum: number) => {
    if (!pdfDocRef.current || !canvasRef.current) return;

    const page = await pdfDocRef.current.getPage(pageNum);
    const viewport = page.getViewport({ scale, rotation });

    const canvas = canvasRef.current;
    const context = canvas.getContext('2d');
    if (!context) return;

    canvas.height = viewport.height;
    canvas.width = viewport.width;

    // Apply theme filter for dark mode
    if (theme === 'dark') {
      context.filter = 'invert(1) hue-rotate(180deg)';
    } else if (theme === 'sepia') {
      context.filter = 'sepia(0.3)';
    } else {
      context.filter = 'none';
    }

    const renderContext = {
      canvasContext: context,
      viewport: viewport,
      canvas: canvas,
    };

    await page.render(renderContext).promise;

    // Extract text for TTS
    const textContent = await page.getTextContent();
    const text = textContent.items
      .map((item) => ('str' in item ? item.str : ''))
      .join(' ');
    setPageText(text);
  }, [scale, rotation, theme]);

  useEffect(() => {
    renderPage(currentPage);
  }, [currentPage, renderPage]);

  const goToPage = useCallback((page: number) => {
    const newPage = Math.max(1, Math.min(totalPages, page));
    setCurrentPage(newPage);
    if (currentBook) {
      updateBook(currentBook.id, { currentPosition: newPage / totalPages });
    }
  }, [totalPages, currentBook]);

  const goNext = useCallback(() => goToPage(currentPage + 1), [currentPage, goToPage]);
  const goPrev = useCallback(() => goToPage(currentPage - 1), [currentPage, goToPage]);

  const handleZoomIn = () => setScale((s) => Math.min(3, s + 0.25));
  const handleZoomOut = () => setScale((s) => Math.max(0.5, s - 0.25));
  const handleRotate = () => setRotation((r) => (r + 90) % 360);

  // Keyboard navigation
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'ArrowRight' || e.key === ' ') {
        goNext();
      } else if (e.key === 'ArrowLeft') {
        goPrev();
      } else if (e.key === '+' || e.key === '=') {
        handleZoomIn();
      } else if (e.key === '-') {
        handleZoomOut();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [goNext, goPrev]);

  // TTS
  const toggleTTS = useCallback(() => {
    if (isReading) {
      window.speechSynthesis.cancel();
      setTTSState({ isReading: false });
    } else {
      if (pageText) {
        const utterance = new SpeechSynthesisUtterance(pageText);
        utterance.rate = ttsState.rate;
        utterance.pitch = ttsState.pitch;
        utterance.onend = () => {
          setTTSState({ isReading: false });
          // Auto advance to next page
          if (currentPage < totalPages) {
            goNext();
          }
        };
        window.speechSynthesis.speak(utterance);
        setTTSState({ isReading: true });
      }
    }
  }, [isReading, pageText, currentPage, totalPages, goNext, ttsState.rate, ttsState.pitch]);

  if (!currentBook) return null;

  const themeClasses = {
    light: 'bg-surface-100',
    dark: 'bg-surface-900',
    sepia: 'bg-[#f4ecd8]',
  };

  return (
    <div className={`h-full flex flex-col ${themeClasses[theme]}`}>
      {/* Header */}
      <motion.header
        initial={{ y: -20, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        className="flex items-center justify-between p-4 bg-white/80 dark:bg-surface-900/80 backdrop-blur-lg border-b border-surface-200 dark:border-surface-800"
      >
        <Button variant="ghost" onClick={() => setCurrentView('library')}>
          <Home className="w-5 h-5" />
        </Button>

        <div className="flex-1 text-center px-4">
          <h1 className="text-sm font-medium truncate text-surface-900 dark:text-white">
            {currentBook.title}
          </h1>
          <p className="text-xs text-surface-500 dark:text-surface-400">
            Page {currentPage} of {totalPages}
          </p>
        </div>

        <div className="flex items-center gap-1">
          <Button variant="ghost" size="sm" onClick={handleZoomOut}>
            <ZoomOut className="w-4 h-4" />
          </Button>
          <span className="text-xs w-12 text-center text-surface-600 dark:text-surface-400">
            {Math.round(scale * 100)}%
          </span>
          <Button variant="ghost" size="sm" onClick={handleZoomIn}>
            <ZoomIn className="w-4 h-4" />
          </Button>
          <Button variant="ghost" size="sm" onClick={handleRotate}>
            <RotateCw className="w-4 h-4" />
          </Button>
          <Button variant="ghost" size="sm" onClick={() => setShowSettings(true)}>
            <Settings className="w-4 h-4" />
          </Button>
        </div>
      </motion.header>

      {/* PDF Viewer */}
      <div
        ref={containerRef}
        className="flex-1 overflow-auto flex items-start justify-center p-4"
      >
        {isLoading ? (
          <div className="flex items-center justify-center h-full">
            <motion.div
              animate={{ rotate: 360 }}
              transition={{ duration: 1, repeat: Infinity, ease: 'linear' }}
              className="w-8 h-8 border-2 border-primary-500 border-t-transparent rounded-full"
            />
          </div>
        ) : (
          <motion.canvas
            ref={canvasRef}
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className="shadow-2xl rounded-lg"
            style={{
              maxWidth: '100%',
              height: 'auto',
            }}
          />
        )}
      </div>

      {/* Navigation arrows */}
      <button
        onClick={goPrev}
        disabled={currentPage <= 1}
        className="fixed left-4 top-1/2 -translate-y-1/2 p-3 rounded-full bg-white/90 dark:bg-surface-800/90 shadow-lg hover:bg-white dark:hover:bg-surface-700 transition-all disabled:opacity-30 disabled:cursor-not-allowed"
      >
        <ChevronLeft className="w-6 h-6" />
      </button>
      <button
        onClick={goNext}
        disabled={currentPage >= totalPages}
        className="fixed right-4 top-1/2 -translate-y-1/2 p-3 rounded-full bg-white/90 dark:bg-surface-800/90 shadow-lg hover:bg-white dark:hover:bg-surface-700 transition-all disabled:opacity-30 disabled:cursor-not-allowed"
      >
        <ChevronRight className="w-6 h-6" />
      </button>

      {/* Footer */}
      <motion.footer
        initial={{ y: 20, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        className="p-4 bg-white/80 dark:bg-surface-900/80 backdrop-blur-lg border-t border-surface-200 dark:border-surface-800"
      >
        <div className="flex items-center gap-4 max-w-2xl mx-auto">
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

          <div className="flex-1">
            <Slider
              value={currentPage}
              min={1}
              max={totalPages || 1}
              onChange={goToPage}
              showTooltip
              formatTooltip={(v) => `Page ${v}`}
            />
          </div>

          <input
            type="number"
            min={1}
            max={totalPages}
            value={currentPage}
            onChange={(e) => goToPage(parseInt(e.target.value) || 1)}
            className="w-16 px-2 py-1 text-center text-sm rounded-lg bg-surface-100 dark:bg-surface-800 border-none focus:ring-2 focus:ring-primary-500"
          />
        </div>
      </motion.footer>

      {/* Settings Modal */}
      <Modal isOpen={showSettings} onClose={() => setShowSettings(false)} title="PDF Settings">
        <div className="space-y-6">
          {/* TTS Settings */}
          <div>
            <div className="flex items-center gap-2 mb-4">
              <Volume2 className="w-5 h-5 text-primary-500" />
              <h3 className="font-medium">Text-to-Speech Settings</h3>
            </div>

            <div className="space-y-4">
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
    </div>
  );
}
