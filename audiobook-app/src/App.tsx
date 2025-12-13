import { AnimatePresence, motion } from 'framer-motion';
import { Library } from './components/library';
import { AudioPlayer } from './components/player';
import { EpubReader, PdfReader } from './components/reader';
// import { SplashScreen } from './components/SplashScreen';
import { useStore } from './store/useStore';

function App() {
  const { currentView, currentBook } = useStore();
  // Splash temporarily disabled for debugging
  // const { showSplash, setShowSplash } = useStore();

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
    <div style={{ minHeight: '100vh', background: '#1e1e2e', padding: '40px' }}>
      {/* DEBUG TEST - Remove after fixing */}
      <h1 style={{ color: '#ff0000', fontSize: '48px', marginBottom: '20px' }}>
        VOCA IS LOADING!
      </h1>
      <p style={{ color: '#00ff00', fontSize: '24px' }}>
        If you can see this, React is working.
      </p>
      <p style={{ color: '#00ffff', fontSize: '18px', marginTop: '20px' }}>
        Current view: {currentView}
      </p>

      {/* Original App Content */}
      <div style={{ marginTop: '40px' }}>
        <AnimatePresence mode="wait">
          {renderView()}
        </AnimatePresence>
      </div>
    </div>
  );
}

export default App;
