import { useRef, useEffect, useCallback, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Play,
  Pause,
  ChevronDown,
  Moon,
  Bookmark,
  BookmarkPlus,
  Clock,
  Trash2,
  Edit3,
  Check,
  RotateCcw,
  RotateCw,
  Headphones,
  StickyNote,
  List,
  Settings,
} from 'lucide-react';
import { Slider, Modal } from '../ui';
import { Waveform } from './Waveform';
import { Equalizer, resumeAudioContext } from './Equalizer';
import { useStore, type SleepTimerMode } from '../../store/useStore';
import { formatTime } from '../../utils/formatTime';
import { updateMediaMetadata, updatePlaybackState, isAndroid } from '../../utils/mediaControl';

interface AudioPlayerProps {
  onBack?: () => void;
}

export function AudioPlayer({ onBack }: AudioPlayerProps) {
  const audioRef = useRef<HTMLAudioElement>(null);
  const sleepTimerRef = useRef<NodeJS.Timeout | null>(null);
  const coverRef = useRef<HTMLDivElement>(null);
  const isDraggingRef = useRef<boolean>(false);
  const dragStartXRef = useRef<number>(0);
  const dragStartTimeRef = useRef<number>(0);

  const [showSleepTimer, setShowSleepTimer] = useState(false);
  const [showBookmarks, setShowBookmarks] = useState(false);
  const [showSpeedPicker, setShowSpeedPicker] = useState(false);
  const [showSettings, setShowSettings] = useState(false);
  const [bookmarkNote, setBookmarkNote] = useState('');
  const [editingBookmarkId, setEditingBookmarkId] = useState<string | null>(null);
  const [editingNote, setEditingNote] = useState('');
  const [gestureIndicator, setGestureIndicator] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'chapters' | 'notes'>('chapters');

  const {
    currentBook,
    playerState,
    setPlayerState,
    updateBook,
    setCurrentView,
    sleepTimerMode,
    sleepTimerRemaining,
    setSleepTimerMode,
    setSleepTimerRemaining,
    addBookmark,
    removeBookmark,
    updateBookmarkNote,
    showWaveform,
    setShowWaveform,
  } = useStore();

  const { isPlaying, currentTime, duration, volume, playbackRate, isMuted } = playerState;

  // Audio setup
  useEffect(() => {
    const audio = audioRef.current;
    if (!audio || !currentBook) return;

    console.log('[AudioPlayer] Setting up audio for:', currentBook.title);

    audio.pause();
    audio.currentTime = 0;
    audio.src = currentBook.fileUrl;
    audio.volume = volume;
    audio.playbackRate = playbackRate;

    const handleLoadedMetadata = () => {
      console.log('[AudioPlayer] Loaded metadata, duration:', audio.duration);
      setPlayerState({ duration: audio.duration });
      if (currentBook.currentPosition > 0 && audio.duration > 0) {
        audio.currentTime = currentBook.currentPosition * audio.duration;
      }
    };

    const handleTimeUpdate = () => {
      setPlayerState({ currentTime: audio.currentTime });
    };

    const handleEnded = () => {
      setPlayerState({ isPlaying: false });
      updateBook(currentBook.id, { isFinished: true, currentPosition: 1 });
    };

    const handleError = () => {
      const mediaError = audio.error;
      console.error('[AudioPlayer] Audio error:', mediaError?.code, mediaError?.message);
      setPlayerState({ isPlaying: false });
    };

    audio.addEventListener('loadedmetadata', handleLoadedMetadata);
    audio.addEventListener('timeupdate', handleTimeUpdate);
    audio.addEventListener('ended', handleEnded);
    audio.addEventListener('error', handleError);
    audio.load();

    return () => {
      audio.removeEventListener('loadedmetadata', handleLoadedMetadata);
      audio.removeEventListener('timeupdate', handleTimeUpdate);
      audio.removeEventListener('ended', handleEnded);
      audio.removeEventListener('error', handleError);
    };
  }, [currentBook?.id, currentBook?.fileUrl]);

  // Play/pause control
  useEffect(() => {
    const audio = audioRef.current;
    if (!audio) return;

    if (isPlaying) {
      resumeAudioContext();
      const attemptPlay = () => {
        if (audio.readyState >= 2) {
          audio.play().catch((error) => {
            console.error('[AudioPlayer] Play error:', error.message);
            setPlayerState({ isPlaying: false });
          });
        } else {
          const handleCanPlay = () => {
            audio.removeEventListener('canplay', handleCanPlay);
            audio.play().catch(() => setPlayerState({ isPlaying: false }));
          };
          audio.addEventListener('canplay', handleCanPlay);
        }
      };
      attemptPlay();
    } else {
      audio.pause();
    }
  }, [isPlaying]);

  // Volume control
  useEffect(() => {
    if (audioRef.current) {
      audioRef.current.volume = isMuted ? 0 : volume;
    }
  }, [volume, isMuted]);

  // Playback rate control
  useEffect(() => {
    if (audioRef.current) {
      audioRef.current.playbackRate = playbackRate;
    }
  }, [playbackRate]);

  // Android Auto metadata
  useEffect(() => {
    if (isAndroid() && currentBook && isPlaying && duration > 0) {
      updateMediaMetadata(currentBook.title, currentBook.author || 'Unknown Author', currentBook.cover, duration);
    }
  }, [currentBook?.id, isPlaying, duration]);

  // Android Auto playback state
  useEffect(() => {
    if (isAndroid() && currentBook && (isPlaying || currentTime > 0)) {
      updatePlaybackState(isPlaying, currentTime, playbackRate);
    }
  }, [isPlaying, currentBook?.id]);

  // Sleep timer countdown
  useEffect(() => {
    if (sleepTimerRemaining !== null && sleepTimerRemaining > 0 && isPlaying) {
      sleepTimerRef.current = setInterval(() => {
        setSleepTimerRemaining(sleepTimerRemaining - 1);
      }, 1000);
      return () => {
        if (sleepTimerRef.current) clearInterval(sleepTimerRef.current);
      };
    } else if (sleepTimerRemaining === 0) {
      setPlayerState({ isPlaying: false });
      setSleepTimerMode('off');
      setSleepTimerRemaining(null);
    }
  }, [sleepTimerRemaining, isPlaying]);

  // Start sleep timer when mode changes
  useEffect(() => {
    if (sleepTimerMode !== 'off' && sleepTimerMode !== 'endOfChapter') {
      const minutes = parseInt(sleepTimerMode);
      setSleepTimerRemaining(minutes * 60);
    } else if (sleepTimerMode === 'off') {
      setSleepTimerRemaining(null);
    }
  }, [sleepTimerMode]);

  const handleSeek = useCallback((value: number) => {
    if (audioRef.current) {
      audioRef.current.currentTime = value;
      setPlayerState({ currentTime: value });
    }
  }, []);

  const handleSkip = useCallback((seconds: number) => {
    if (audioRef.current) {
      const newTime = Math.max(0, Math.min(duration, currentTime + seconds));
      audioRef.current.currentTime = newTime;
      setPlayerState({ currentTime: newTime });
    }
  }, [currentTime, duration]);

  const togglePlayPause = useCallback(() => {
    setPlayerState({ isPlaying: !isPlaying });
  }, [isPlaying]);

  // Gesture handlers for cover area
  const handleTouchStart = useCallback((e: React.TouchEvent | React.MouseEvent) => {
    const clientX = 'touches' in e ? e.touches[0].clientX : e.clientX;
    dragStartXRef.current = clientX;
    dragStartTimeRef.current = currentTime;
    isDraggingRef.current = false;
  }, [currentTime]);

  const handleTouchMove = useCallback((e: React.TouchEvent | React.MouseEvent) => {
    if (!coverRef.current) return;
    const clientX = 'touches' in e ? e.touches[0].clientX : e.clientX;
    const deltaX = clientX - dragStartXRef.current;

    if (Math.abs(deltaX) > 20) {
      isDraggingRef.current = true;
      const scrubAmount = (deltaX / 100) * 30;
      const newTime = Math.max(0, Math.min(duration, dragStartTimeRef.current + scrubAmount));
      setGestureIndicator(deltaX > 0 ? `+${formatTime(scrubAmount)}` : formatTime(scrubAmount));
      handleSeek(newTime);
    }
  }, [duration, handleSeek]);

  const handleTouchEnd = useCallback(() => {
    if (isDraggingRef.current) {
      isDraggingRef.current = false;
      setGestureIndicator(null);
    }
  }, []);

  const handleAddBookmark = useCallback(() => {
    if (!currentBook) return;
    const bookmark = {
      id: crypto.randomUUID(),
      position: currentTime,
      note: bookmarkNote || undefined,
      createdAt: new Date(),
    };
    addBookmark(currentBook.id, bookmark);
    setBookmarkNote('');
  }, [currentBook, currentTime, bookmarkNote, addBookmark]);

  const handleJumpToBookmark = useCallback((position: number) => {
    if (audioRef.current) {
      audioRef.current.currentTime = position;
      setPlayerState({ currentTime: position });
    }
  }, []);

  const handleJumpToChapter = useCallback((startTime: number) => {
    if (audioRef.current) {
      audioRef.current.currentTime = startTime;
      setPlayerState({ currentTime: startTime });
    }
  }, []);

  const progress = duration > 0 ? currentTime / duration : 0;

  const sleepTimerOptions: { value: SleepTimerMode; label: string }[] = [
    { value: 'off', label: 'Off' },
    { value: '5', label: '5 min' },
    { value: '10', label: '10 min' },
    { value: '15', label: '15 min' },
    { value: '30', label: '30 min' },
    { value: '45', label: '45 min' },
    { value: '60', label: '60 min' },
    { value: 'endOfChapter', label: 'End of Chapter' },
  ];

  const speedOptions = [0.5, 0.75, 1, 1.25, 1.5, 1.75, 2];

  if (!currentBook) return null;

  const bookmarks = currentBook.bookmarks || [];
  const chapters = currentBook.chapters || [];

  // Find current chapter
  const currentChapter = chapters.find(
    (ch, i) => (ch.startTime || 0) <= currentTime &&
      (i === chapters.length - 1 || (chapters[i + 1].startTime || 0) > currentTime)
  );
  const currentChapterIndex = currentChapter
    ? chapters.findIndex(ch => ch.id === currentChapter.id) + 1
    : 1;

  // Calculate remaining time
  const remainingTime = duration - currentTime;

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="h-full flex flex-col bg-surface-950 overflow-hidden"
    >
      <audio ref={audioRef} preload="metadata" crossOrigin="anonymous" />

      {/* New Header: Thumbnail + Chapter + Author */}
      <div className="flex-shrink-0 flex items-center gap-3 p-4 bg-surface-900/50 safe-area-top">
        <button
          onClick={onBack || (() => setCurrentView('library'))}
          className="p-2 text-surface-400 hover:text-white transition-colors"
        >
          <ChevronDown className="w-6 h-6" />
        </button>

        {/* Thumbnail */}
        <div className="w-10 h-10 rounded-lg overflow-hidden flex-shrink-0 bg-surface-800">
          {currentBook.cover ? (
            <img src={currentBook.cover} alt="" className="w-full h-full object-cover" />
          ) : (
            <div className="w-full h-full flex items-center justify-center bg-primary-500/20">
              <Headphones className="w-5 h-5 text-primary-500" />
            </div>
          )}
        </div>

        {/* Chapter + Author */}
        <div className="flex-1 min-w-0">
          <p className="text-sm font-medium text-white truncate">
            {currentChapter?.title || currentBook.title}
          </p>
          <p className="text-xs text-surface-400 truncate">
            {currentBook.author}
          </p>
        </div>

        {/* Settings button */}
        <button
          onClick={() => setShowSettings(true)}
          className="p-2 text-surface-400 hover:text-white transition-colors"
        >
          <Settings className="w-5 h-5" />
        </button>
      </div>

      {/* Cover Art with gesture support */}
      <div
        ref={coverRef}
        className="flex-1 min-h-0 flex items-center justify-center px-8 py-4 select-none"
        onTouchStart={handleTouchStart}
        onTouchMove={handleTouchMove}
        onTouchEnd={handleTouchEnd}
        onMouseDown={handleTouchStart}
        onMouseMove={(e) => e.buttons === 1 && handleTouchMove(e)}
        onMouseUp={handleTouchEnd}
      >
        <motion.div
          className="relative w-full max-w-xs aspect-square"
          initial={{ scale: 0.9, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          transition={{ type: 'spring', stiffness: 200, damping: 20 }}
        >
          {currentBook.cover ? (
            <img
              src={currentBook.cover}
              alt={currentBook.title}
              className="w-full h-full object-cover rounded-2xl shadow-2xl shadow-black/50 pointer-events-none"
              draggable={false}
            />
          ) : (
            <div className="w-full h-full rounded-2xl bg-gradient-to-br from-primary-500 to-primary-700 flex items-center justify-center shadow-2xl">
              <Headphones className="w-20 h-20 text-white/80" />
            </div>
          )}

          {/* Gesture indicator overlay */}
          <AnimatePresence>
            {gestureIndicator && (
              <motion.div
                initial={{ opacity: 0, scale: 0.8 }}
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0, scale: 0.8 }}
                className="absolute inset-0 flex items-center justify-center bg-black/60 rounded-2xl"
              >
                <span className="text-white text-2xl font-bold">{gestureIndicator}</span>
              </motion.div>
            )}
          </AnimatePresence>
        </motion.div>
      </div>

      {/* Optional Waveform */}
      {showWaveform && (
        <div className="flex-shrink-0 px-6 py-2">
          <Waveform isPlaying={isPlaying} progress={progress} />
        </div>
      )}

      {/* Progress Bar */}
      <div className="flex-shrink-0 px-6 py-2">
        <Slider
          value={currentTime}
          max={duration || 100}
          onChange={handleSeek}
          showTooltip
          formatTooltip={formatTime}
        />
        {/* Progress Info: elapsed | chapter x/y | remaining */}
        <div className="flex justify-between mt-2 text-xs text-surface-400">
          <span>{formatTime(currentTime)}</span>
          <span>
            {chapters.length > 0 ? `Chapter ${currentChapterIndex}/${chapters.length}` : ''}
          </span>
          <span>-{formatTime(remainingTime)}</span>
        </div>
      </div>

      {/* Simplified Controls: Speed | Rewind | Play | Forward | Sleep */}
      <div className="flex-shrink-0 px-6 py-4">
        <div className="flex items-center justify-between">
          {/* Speed */}
          <button
            onClick={() => setShowSpeedPicker(true)}
            className="w-14 h-10 rounded-lg bg-surface-800 text-sm font-semibold text-surface-300 hover:bg-surface-700 transition-colors"
          >
            {playbackRate}x
          </button>

          {/* Rewind 10s */}
          <button
            onClick={() => handleSkip(-10)}
            className="w-12 h-12 flex items-center justify-center text-surface-300 hover:text-white transition-colors relative"
          >
            <RotateCcw className="w-7 h-7" />
            <span className="absolute text-[10px] font-bold">10</span>
          </button>

          {/* Play/Pause */}
          <motion.button
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.95 }}
            onClick={togglePlayPause}
            className="w-16 h-16 rounded-full bg-primary-500 text-white shadow-lg shadow-primary-500/30 flex items-center justify-center"
          >
            <AnimatePresence mode="wait">
              {isPlaying ? (
                <motion.div key="pause" initial={{ scale: 0 }} animate={{ scale: 1 }} exit={{ scale: 0 }}>
                  <Pause className="w-8 h-8" fill="currentColor" />
                </motion.div>
              ) : (
                <motion.div key="play" initial={{ scale: 0 }} animate={{ scale: 1 }} exit={{ scale: 0 }}>
                  <Play className="w-8 h-8 ml-1" fill="currentColor" />
                </motion.div>
              )}
            </AnimatePresence>
          </motion.button>

          {/* Forward 10s */}
          <button
            onClick={() => handleSkip(10)}
            className="w-12 h-12 flex items-center justify-center text-surface-300 hover:text-white transition-colors relative"
          >
            <RotateCw className="w-7 h-7" />
            <span className="absolute text-[10px] font-bold">10</span>
          </button>

          {/* Sleep Timer */}
          <button
            onClick={() => setShowSleepTimer(true)}
            className="w-14 h-10 rounded-lg bg-surface-800 flex items-center justify-center text-surface-300 hover:bg-surface-700 transition-colors relative"
          >
            <Moon className="w-5 h-5" />
            {sleepTimerRemaining !== null && (
              <span className="absolute -top-1 -right-1 text-[10px] bg-primary-500 text-white rounded-full px-1.5 py-0.5">
                {Math.ceil(sleepTimerRemaining / 60)}
              </span>
            )}
          </button>
        </div>
      </div>

      {/* Chapters/Notes Tabs */}
      <div className="flex-shrink-0 border-t border-surface-800">
        <div className="flex">
          <button
            onClick={() => setActiveTab('chapters')}
            className={`flex-1 py-3 text-sm font-medium flex items-center justify-center gap-2 transition-colors ${
              activeTab === 'chapters'
                ? 'text-primary-500 border-b-2 border-primary-500'
                : 'text-surface-400 hover:text-white'
            }`}
          >
            <List className="w-4 h-4" />
            Chapters ({chapters.length})
          </button>
          <button
            onClick={() => setActiveTab('notes')}
            className={`flex-1 py-3 text-sm font-medium flex items-center justify-center gap-2 transition-colors ${
              activeTab === 'notes'
                ? 'text-primary-500 border-b-2 border-primary-500'
                : 'text-surface-400 hover:text-white'
            }`}
          >
            <StickyNote className="w-4 h-4" />
            Notes ({bookmarks.length})
          </button>
        </div>
      </div>

      {/* Tab Content */}
      <div className="flex-1 overflow-auto px-4 py-2 safe-area-bottom" style={{ maxHeight: '30vh' }}>
        {activeTab === 'chapters' ? (
          <div className="space-y-1">
            {chapters.length === 0 ? (
              <p className="text-center text-surface-500 py-4 text-sm">No chapters available</p>
            ) : (
              chapters.map((chapter, index) => {
                const isCurrentChapter = currentChapter?.id === chapter.id;
                return (
                  <button
                    key={chapter.id}
                    onClick={() => handleJumpToChapter(chapter.startTime || 0)}
                    className={`w-full flex items-center gap-3 p-3 rounded-lg transition-colors ${
                      isCurrentChapter
                        ? 'bg-primary-500/20 text-primary-400'
                        : 'text-surface-300 hover:bg-surface-800'
                    }`}
                  >
                    <span className={`w-6 h-6 rounded-full text-xs flex items-center justify-center ${
                      isCurrentChapter ? 'bg-primary-500 text-white' : 'bg-surface-700 text-surface-400'
                    }`}>
                      {index + 1}
                    </span>
                    <span className="flex-1 text-left text-sm truncate">{chapter.title}</span>
                    {chapter.startTime !== undefined && (
                      <span className="text-xs text-surface-500">{formatTime(chapter.startTime)}</span>
                    )}
                  </button>
                );
              })
            )}
          </div>
        ) : (
          <div className="space-y-2">
            {/* Add bookmark button */}
            <button
              onClick={handleAddBookmark}
              className="w-full flex items-center gap-2 p-3 rounded-lg bg-surface-800 text-primary-400 hover:bg-surface-700 transition-colors"
            >
              <BookmarkPlus className="w-4 h-4" />
              <span className="text-sm">Add bookmark at {formatTime(currentTime)}</span>
            </button>

            {bookmarks.length === 0 ? (
              <p className="text-center text-surface-500 py-4 text-sm">No bookmarks yet</p>
            ) : (
              bookmarks
                .sort((a, b) => a.position - b.position)
                .map((bookmark) => (
                  <div
                    key={bookmark.id}
                    className="flex items-center gap-3 p-3 bg-surface-800 rounded-lg"
                  >
                    <button
                      onClick={() => handleJumpToBookmark(bookmark.position)}
                      className="flex-1 text-left"
                    >
                      <div className="flex items-center gap-2">
                        <Bookmark className="w-4 h-4 text-primary-500" />
                        <span className="text-sm font-medium text-white">
                          {formatTime(bookmark.position)}
                        </span>
                      </div>
                      {editingBookmarkId === bookmark.id ? (
                        <div className="flex items-center gap-2 mt-1">
                          <input
                            type="text"
                            value={editingNote}
                            onChange={(e) => setEditingNote(e.target.value)}
                            className="flex-1 bg-surface-700 px-2 py-1 rounded text-sm text-white"
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
                            className="p-1 text-green-500"
                          >
                            <Check className="w-4 h-4" />
                          </button>
                        </div>
                      ) : bookmark.note ? (
                        <p className="text-xs text-surface-400 mt-1">{bookmark.note}</p>
                      ) : null}
                    </button>
                    <div className="flex items-center gap-1">
                      <button
                        onClick={() => {
                          setEditingBookmarkId(bookmark.id);
                          setEditingNote(bookmark.note || '');
                        }}
                        className="p-2 text-surface-400 hover:text-primary-500"
                      >
                        <Edit3 className="w-4 h-4" />
                      </button>
                      <button
                        onClick={() => currentBook && removeBookmark(currentBook.id, bookmark.id)}
                        className="p-2 text-surface-400 hover:text-red-500"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                ))
            )}
          </div>
        )}
      </div>

      {/* Speed Picker Modal */}
      <Modal
        isOpen={showSpeedPicker}
        onClose={() => setShowSpeedPicker(false)}
        title="Playback Speed"
        size="sm"
      >
        <div className="grid grid-cols-4 gap-2">
          {speedOptions.map((speed) => (
            <button
              key={speed}
              onClick={() => {
                setPlayerState({ playbackRate: speed });
                setShowSpeedPicker(false);
              }}
              className={`py-3 rounded-xl text-sm font-medium transition-colors ${
                playbackRate === speed
                  ? 'bg-primary-500 text-white'
                  : 'bg-surface-800 hover:bg-surface-700 text-surface-300'
              }`}
            >
              {speed}x
            </button>
          ))}
        </div>
      </Modal>

      {/* Sleep Timer Modal */}
      <Modal
        isOpen={showSleepTimer}
        onClose={() => setShowSleepTimer(false)}
        title="Sleep Timer"
        size="sm"
      >
        <div className="space-y-2">
          {sleepTimerOptions.map((option) => (
            <button
              key={option.value}
              onClick={() => {
                setSleepTimerMode(option.value);
                setShowSleepTimer(false);
              }}
              className={`w-full p-4 rounded-xl flex items-center justify-between transition-colors ${
                sleepTimerMode === option.value
                  ? 'bg-primary-500 text-white'
                  : 'bg-surface-800 hover:bg-surface-700 text-surface-300'
              }`}
            >
              <span className="font-medium">{option.label}</span>
              {sleepTimerMode === option.value && sleepTimerRemaining !== null && (
                <span className="text-sm opacity-80">{formatTime(sleepTimerRemaining)}</span>
              )}
            </button>
          ))}
        </div>
      </Modal>

      {/* Settings Modal */}
      <Modal
        isOpen={showSettings}
        onClose={() => setShowSettings(false)}
        title="Player Settings"
        size="sm"
      >
        <div className="space-y-4">
          {/* Waveform Toggle */}
          <div className="flex items-center justify-between p-4 bg-surface-800 rounded-xl">
            <div>
              <p className="font-medium text-white">Show Waveform</p>
              <p className="text-sm text-surface-400">Display audio visualization</p>
            </div>
            <button
              onClick={() => setShowWaveform(!showWaveform)}
              className={`relative w-12 h-7 rounded-full transition-colors ${
                showWaveform ? 'bg-primary-500' : 'bg-surface-600'
              }`}
            >
              <motion.div
                className="absolute top-1 w-5 h-5 bg-white rounded-full shadow-md"
                animate={{ left: showWaveform ? '24px' : '4px' }}
                transition={{ type: 'spring', stiffness: 500, damping: 30 }}
              />
            </button>
          </div>

          {/* Equalizer */}
          <Equalizer audioRef={audioRef} asMenuItem onClose={() => setShowSettings(false)} />

          {/* Bookmarks */}
          <button
            onClick={() => {
              setShowSettings(false);
              setShowBookmarks(true);
            }}
            className="w-full flex items-center gap-3 p-4 rounded-xl bg-surface-800 hover:bg-surface-700 transition-colors"
          >
            <Bookmark className="w-5 h-5 text-primary-500" />
            <div className="text-left">
              <p className="font-medium text-white">Bookmarks</p>
              <p className="text-sm text-surface-400">{bookmarks.length} saved</p>
            </div>
          </button>
        </div>
      </Modal>

      {/* Bookmarks Modal */}
      <Modal
        isOpen={showBookmarks}
        onClose={() => setShowBookmarks(false)}
        title="Bookmarks"
        size="md"
      >
        <div className="space-y-4">
          <div className="flex gap-2">
            <input
              type="text"
              placeholder="Add note (optional)..."
              value={bookmarkNote}
              onChange={(e) => setBookmarkNote(e.target.value)}
              className="flex-1 bg-surface-800 px-4 py-2 rounded-xl text-white placeholder-surface-500"
            />
            <button
              onClick={handleAddBookmark}
              className="px-4 py-2 bg-primary-500 text-white rounded-xl hover:bg-primary-600 transition-colors flex items-center gap-2"
            >
              <BookmarkPlus className="w-4 h-4" />
              Add
            </button>
          </div>

          <div className="space-y-2 max-h-64 overflow-y-auto">
            {bookmarks.length === 0 ? (
              <p className="text-center text-surface-500 py-8">No bookmarks yet</p>
            ) : (
              bookmarks
                .sort((a, b) => a.position - b.position)
                .map((bookmark) => (
                  <div
                    key={bookmark.id}
                    className="flex items-center gap-3 p-3 bg-surface-800 rounded-xl"
                  >
                    <button
                      onClick={() => {
                        handleJumpToBookmark(bookmark.position);
                        setShowBookmarks(false);
                      }}
                      className="flex-1 text-left"
                    >
                      <div className="flex items-center gap-2">
                        <Clock className="w-4 h-4 text-primary-500" />
                        <span className="font-medium text-white">{formatTime(bookmark.position)}</span>
                      </div>
                      {bookmark.note && (
                        <p className="text-sm text-surface-400 mt-1">{bookmark.note}</p>
                      )}
                    </button>
                    <button
                      onClick={() => currentBook && removeBookmark(currentBook.id, bookmark.id)}
                      className="p-2 text-surface-400 hover:text-red-500"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                ))
            )}
          </div>
        </div>
      </Modal>
    </motion.div>
  );
}
