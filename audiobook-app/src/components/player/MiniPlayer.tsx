import { motion } from 'framer-motion';
import { Play, Pause } from 'lucide-react';
import { useStore } from '../../store/useStore';

interface MiniPlayerProps {
  onExpand: () => void;
}

export function MiniPlayer({ onExpand }: MiniPlayerProps) {
  const { currentBook, playerState, setPlayerState } = useStore();
  const { isPlaying } = playerState;

  if (!currentBook) return null;

  const togglePlayPause = (e: React.MouseEvent) => {
    e.stopPropagation();
    setPlayerState({ isPlaying: !isPlaying });
  };

  // Get current chapter name if available
  const currentChapter = currentBook.chapters?.find(
    (ch, i, arr) =>
      (ch.startTime || 0) <= playerState.currentTime &&
      (i === arr.length - 1 || (arr[i + 1].startTime || 0) > playerState.currentTime)
  );

  return (
    <motion.div
      initial={{ y: 100, opacity: 0 }}
      animate={{ y: 0, opacity: 1 }}
      exit={{ y: 100, opacity: 0 }}
      className="fixed bottom-0 left-0 right-0 z-50 bg-surface-900 border-t border-surface-800 safe-area-bottom"
    >
      <div
        className="flex items-center gap-3 px-4 py-3 cursor-pointer"
        onClick={onExpand}
      >
        {/* Cover thumbnail */}
        <div
          className="w-12 h-12 rounded-lg overflow-hidden flex-shrink-0 bg-surface-800"
        >
          {currentBook.cover ? (
            <img
              src={currentBook.cover}
              alt={currentBook.title}
              className="w-full h-full object-cover"
            />
          ) : (
            <div className="w-full h-full flex items-center justify-center bg-primary-500/20">
              <span className="text-primary-500 text-lg font-bold">
                {currentBook.title.charAt(0)}
              </span>
            </div>
          )}
        </div>

        {/* Info */}
        <div className="flex-1 min-w-0">
          <p className="text-sm font-medium text-white truncate">
            {currentChapter?.title || currentBook.title}
          </p>
          <p className="text-xs text-surface-400 truncate">
            {currentBook.author}
          </p>
        </div>

        {/* Play/Pause button */}
        <button
          onClick={togglePlayPause}
          className="w-10 h-10 flex items-center justify-center text-primary-500"
        >
          {isPlaying ? (
            <Pause className="w-6 h-6" fill="currentColor" />
          ) : (
            <Play className="w-6 h-6 ml-0.5" fill="currentColor" />
          )}
        </button>
      </div>

      {/* Progress bar */}
      <div className="h-0.5 bg-surface-800">
        <div
          className="h-full bg-primary-500 transition-all duration-300"
          style={{
            width: `${playerState.duration > 0
              ? (playerState.currentTime / playerState.duration) * 100
              : 0}%`
          }}
        />
      </div>
    </motion.div>
  );
}
