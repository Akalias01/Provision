import { AnimatePresence, motion } from 'framer-motion';
import { Library } from './components/library';
import { AudioPlayer } from './components/player';
import { EpubReader, PdfReader } from './components/reader';
import { useStore } from './store/useStore';

function App() {
  const { currentView, currentBook } = useStore();

  const renderView = () => {
    switch (currentView) {
      case 'library':
        return (
          <motion.div
            key="library"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0, x: -20 }}
            transition={{ duration: 0.3 }}
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
            transition={{ duration: 0.3 }}
            className="h-screen"
          >
            <AudioPlayer />
          </motion.div>
        );

      case 'reader':
        if (!currentBook) return null;
        return (
          <motion.div
            key="reader"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.3 }}
            className="h-screen"
          >
            {currentBook.format === 'epub' ? <EpubReader /> : <PdfReader />}
          </motion.div>
        );

      default:
        return <Library />;
    }
  };

  return (
    <div className="min-h-screen bg-surface-50 dark:bg-surface-950">
      <AnimatePresence mode="wait">
        {renderView()}
      </AnimatePresence>
    </div>
  );
}

export default App;
