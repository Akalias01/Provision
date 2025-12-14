import { AnimatePresence, motion } from 'framer-motion';
import { Library } from './components/library';
import { AudioPlayer } from './components/player';
import { EpubReader, PdfReader, DocReader } from './components/reader';
import { SplashScreen } from './components/SplashScreen';
import { useStore } from './store/useStore';
import { useEffect } from 'react';
import { isAndroid } from './utils/mediaControl';

function App() {
  const { currentView, currentBook, showSplash, setShowSplash } = useStore();

  // Handle Android status bar and navigation bar
  useEffect(() => {
    if (isAndroid()) {
      // Set status bar color
      document.body.style.backgroundColor = '#0a0a0f';
    }
  }, []);

  const renderView = () => {
    switch (currentView) {
      case 'library':
        return (
          <motion.div
            key="library"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.2 }}
            className="h-full"
          >
            <Library />
          </motion.div>
        );

      case 'player':
        return (
          <motion.div
            key="player"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 20 }}
            transition={{ duration: 0.2 }}
            className="h-full"
          >
            <AudioPlayer />
          </motion.div>
        );

      case 'reader':
        if (!currentBook) return null;
        const renderReader = () => {
          switch (currentBook.format) {
            case 'epub':
              return <EpubReader />;
            case 'pdf':
              return <PdfReader />;
            case 'doc':
              return <DocReader />;
            default:
              return <EpubReader />;
          }
        };
        return (
          <motion.div
            key="reader"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.2 }}
            className="h-full"
          >
            {renderReader()}
          </motion.div>
        );

      default:
        return <Library />;
    }
  };

  return (
    <div className="app-container">
      {/* Splash Screen */}
      {showSplash && (
        <SplashScreen onComplete={() => setShowSplash(false)} />
      )}

      {/* Main App */}
      <AnimatePresence mode="wait">
        {renderView()}
      </AnimatePresence>
    </div>
  );
}

export default App;
