import { useRef, useEffect, useCallback, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Play,
  Pause,
  SkipBack,
  SkipForward,
  Volume2,
  VolumeX,
  Rewind,
  FastForward,
  BookOpen,
  ChevronDown,
  Moon,
  Bookmark,
  BookmarkPlus,
  Settings,
  Clock,
  Trash2,
  Edit3,
  Check,
} from 'lucide-react';
import { Button, Slider, Modal } from '../ui';
import { Waveform } from './Waveform';
import { Equalizer } from './Equalizer';
import { useStore, type SleepTimerMode, type SkipInterval } from '../../store/useStore';
import { formatTime } from '../../utils/formatTime';

interface AudioPlayerProps {
  onBack?: () => void;
}

export function AudioPlayer({ onBack }: AudioPlayerProps) {
  const audioRef = useRef<HTMLAudioElement>(null);
  const sleepTimerRef = useRef<NodeJS.Timeout | null>(null);
  const [showSleepTimer, setShowSleepTimer] = useState(false);
  const [showBookmarks, setShowBookmarks] = useState(false);
  const [showSettings, setShowSettings] = useState(false);
  const [bookmarkNote, setBookmarkNote] = useState('');
  const [editingBookmarkId, setEditingBookmarkId] = useState<string | null>(null);
  const [editingNote, setEditingNote] = useState('');

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
    skipInterval,
    setSkipInterval,
    addBookmark,
    removeBookmark,
    updateBookmarkNote,
  } = useStore();

  const { isPlaying, currentTime, duration, volume, playbackRate, isMuted } = playerState;

  // Audio setup
  useEffect(() => {
    const audio = audioRef.current;
    if (!audio || !currentBook) return;

    audio.src = currentBook.fileUrl;
    audio.volume = volume;
    audio.playbackRate = playbackRate;
    audio.currentTime = currentBook.currentPosition * (audio.duration || 0);

    const handleLoadedMetadata = () => {
      setPlayerState({ duration: audio.duration });
    };

    const handleTimeUpdate = () => {
      setPlayerState({ currentTime: audio.currentTime });
    };

    const handleEnded = () => {
      setPlayerState({ isPlaying: false });
      updateBook(currentBook.id, { isFinished: true, currentPosition: 1 });
    };

    audio.addEventListener('loadedmetadata', handleLoadedMetadata);
    audio.addEventListener('timeupdate', handleTimeUpdate);
    audio.addEventListener('ended', handleEnded);

    return () => {
      audio.removeEventListener('loadedmetadata', handleLoadedMetadata);
      audio.removeEventListener('timeupdate', handleTimeUpdate);
      audio.removeEventListener('ended', handleEnded);
    };
  }, [currentBook]);

  // Play/pause control
  useEffect(() => {
    const audio = audioRef.current;
    if (!audio) return;

    if (isPlaying) {
      audio.play().catch(() => setPlayerState({ isPlaying: false }));
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

  const handleSpeedChange = useCallback(() => {
    const speeds = [0.5, 0.75, 1, 1.25, 1.5, 1.75, 2];
    const currentIndex = speeds.indexOf(playbackRate);
    const nextIndex = (currentIndex + 1) % speeds.length;
    setPlayerState({ playbackRate: speeds[nextIndex] });
  }, [playbackRate]);

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
    setShowBookmarks(false);
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

  const skipIntervalOptions: SkipInterval[] = [10, 15, 30, 45, 60];

  if (!currentBook) return null;

  const bookmarks = currentBook.bookmarks || [];

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="h-full flex flex-col bg-gradient-to-b from-surface-100 to-surface-50 dark:from-surface-900 dark:to-surface-950"
    >
      <audio ref={audioRef} preload="metadata" />

      {/* Header */}
      <div className="flex items-center justify-between p-4">
        <Button variant="icon" onClick={onBack || (() => setCurrentView('library'))}>
          <ChevronDown className="w-6 h-6" />
        </Button>
        <span className="text-sm text-surface-500 dark:text-surface-400">
          Now Playing
        </span>
        <div className="flex items-center gap-1">
          <Button variant="icon" onClick={() => setShowBookmarks(true)}>
            <Bookmark className="w-5 h-5" />
          </Button>
          <Button variant="icon" onClick={() => setShowSettings(true)}>
            <Settings className="w-5 h-5" />
          </Button>
        </div>
      </div>

      {/* Cover Art */}
      <div className="flex-1 flex items-center justify-center px-8 py-4">
        <motion.div
          className="relative w-full max-w-sm aspect-square"
          initial={{ scale: 0.9, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          transition={{ type: 'spring', stiffness: 200, damping: 20 }}
        >
          {currentBook.cover ? (
            <img
              src={currentBook.cover}
              alt={currentBook.title}
              className="w-full h-full object-cover rounded-3xl shadow-2xl shadow-primary-500/20"
            />
          ) : (
            <div className="w-full h-full rounded-3xl bg-gradient-to-br from-primary-500 to-primary-700 flex items-center justify-center shadow-2xl shadow-primary-500/20">
              <BookOpen className="w-24 h-24 text-white/80" />
            </div>
          )}

          {/* Glow effect when playing */}
          <AnimatePresence>
            {isPlaying && (
              <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                className="absolute inset-0 rounded-3xl bg-primary-500/20 blur-3xl -z-10"
              />
            )}
          </AnimatePresence>
        </motion.div>
      </div>

      {/* Info */}
      <div className="px-8 py-4 text-center">
        <motion.h2
          className="text-2xl font-bold text-surface-900 dark:text-white truncate"
          initial={{ y: 20, opacity: 0 }}
          animate={{ y: 0, opacity: 1 }}
          transition={{ delay: 0.1 }}
        >
          {currentBook.title}
        </motion.h2>
        <motion.p
          className="text-surface-500 dark:text-surface-400 mt-1"
          initial={{ y: 20, opacity: 0 }}
          animate={{ y: 0, opacity: 1 }}
          transition={{ delay: 0.15 }}
        >
          {currentBook.author}
          {currentBook.narrator && ` • Narrated by ${currentBook.narrator}`}
        </motion.p>
      </div>

      {/* Waveform */}
      <div className="px-8 py-2">
        <Waveform isPlaying={isPlaying} progress={progress} />
      </div>

      {/* Progress */}
      <div className="px-8 py-4">
        <Slider
          value={currentTime}
          max={duration || 100}
          onChange={handleSeek}
          showTooltip
          formatTooltip={formatTime}
        />
        <div className="flex justify-between mt-2 text-sm text-surface-500 dark:text-surface-400">
          <span>{formatTime(currentTime)}</span>
          <span>{formatTime(duration)}</span>
        </div>
      </div>

      {/* Controls */}
      <div className="px-8 py-6">
        <div className="flex items-center justify-center gap-4">
          {/* Skip backward */}
          <Button
            variant="ghost"
            size="lg"
            onClick={() => handleSkip(-skipInterval)}
            className="relative"
          >
            <Rewind className="w-6 h-6" />
            <span className="absolute -bottom-1 text-[10px] font-medium">{skipInterval}</span>
          </Button>

          {/* Previous */}
          <Button variant="ghost" size="lg">
            <SkipBack className="w-6 h-6" />
          </Button>

          {/* Play/Pause */}
          <motion.button
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.95 }}
            onClick={togglePlayPause}
            className="w-16 h-16 rounded-full bg-gradient-to-r from-primary-500 to-primary-600 text-white shadow-lg shadow-primary-500/30 flex items-center justify-center"
          >
            <AnimatePresence mode="wait">
              {isPlaying ? (
                <motion.div
                  key="pause"
                  initial={{ scale: 0 }}
                  animate={{ scale: 1 }}
                  exit={{ scale: 0 }}
                >
                  <Pause className="w-7 h-7" fill="currentColor" />
                </motion.div>
              ) : (
                <motion.div
                  key="play"
                  initial={{ scale: 0 }}
                  animate={{ scale: 1 }}
                  exit={{ scale: 0 }}
                >
                  <Play className="w-7 h-7 ml-1" fill="currentColor" />
                </motion.div>
              )}
            </AnimatePresence>
          </motion.button>

          {/* Next */}
          <Button variant="ghost" size="lg">
            <SkipForward className="w-6 h-6" />
          </Button>

          {/* Skip forward */}
          <Button
            variant="ghost"
            size="lg"
            onClick={() => handleSkip(skipInterval)}
            className="relative"
          >
            <FastForward className="w-6 h-6" />
            <span className="absolute -bottom-1 text-[10px] font-medium">{skipInterval}</span>
          </Button>
        </div>

        {/* Secondary controls */}
        <div className="flex items-center justify-between mt-6">
          {/* Speed */}
          <Button
            variant="ghost"
            size="sm"
            onClick={handleSpeedChange}
            className="text-xs font-semibold min-w-[60px]"
          >
            {playbackRate}x
          </Button>

          {/* Sleep Timer */}
          <Button
            variant="ghost"
            size="sm"
            onClick={() => setShowSleepTimer(true)}
            className="relative"
          >
            <Moon className="w-5 h-5" />
            {sleepTimerRemaining !== null && (
              <span className="absolute -top-1 -right-1 text-[10px] bg-primary-500 text-white rounded-full px-1">
                {Math.ceil(sleepTimerRemaining / 60)}
              </span>
            )}
          </Button>

          {/* Quick Bookmark */}
          <Button
            variant="ghost"
            size="sm"
            onClick={handleAddBookmark}
          >
            <BookmarkPlus className="w-5 h-5" />
          </Button>

          {/* Equalizer */}
          <Equalizer audioRef={audioRef} />

          {/* Volume */}
          <div className="flex items-center gap-2">
            <Button
              variant="ghost"
              size="sm"
              onClick={() => setPlayerState({ isMuted: !isMuted })}
            >
              {isMuted || volume === 0 ? (
                <VolumeX className="w-5 h-5" />
              ) : (
                <Volume2 className="w-5 h-5" />
              )}
            </Button>
            <div className="w-24">
              <Slider
                value={isMuted ? 0 : volume}
                max={1}
                step={0.01}
                onChange={(v) => setPlayerState({ volume: v, isMuted: false })}
              />
            </div>
          </div>
        </div>
      </div>

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
                  : 'bg-surface-100 dark:bg-surface-800 hover:bg-surface-200 dark:hover:bg-surface-700'
              }`}
            >
              <span className="font-medium">{option.label}</span>
              {sleepTimerMode === option.value && sleepTimerRemaining !== null && (
                <span className="text-sm opacity-80">
                  {formatTime(sleepTimerRemaining)}
                </span>
              )}
            </button>
          ))}
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
                          {formatTime(bookmark.position)}
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

      {/* Settings Modal */}
      <Modal
        isOpen={showSettings}
        onClose={() => setShowSettings(false)}
        title="Player Settings"
        size="sm"
      >
        <div className="space-y-6">
          {/* Skip Interval */}
          <div>
            <h4 className="text-sm font-medium text-surface-900 dark:text-white mb-3">
              Skip Interval
            </h4>
            <div className="flex gap-2">
              {skipIntervalOptions.map((interval) => (
                <button
                  key={interval}
                  onClick={() => setSkipInterval(interval)}
                  className={`flex-1 py-2 rounded-lg text-sm font-medium transition-colors ${
                    skipInterval === interval
                      ? 'bg-primary-500 text-white'
                      : 'bg-surface-100 dark:bg-surface-800 hover:bg-surface-200 dark:hover:bg-surface-700'
                  }`}
                >
                  {interval}s
                </button>
              ))}
            </div>
          </div>

          {/* Playback Speed */}
          <div>
            <h4 className="text-sm font-medium text-surface-900 dark:text-white mb-3">
              Playback Speed
            </h4>
            <div className="flex gap-2 flex-wrap">
              {[0.5, 0.75, 1, 1.25, 1.5, 1.75, 2].map((speed) => (
                <button
                  key={speed}
                  onClick={() => setPlayerState({ playbackRate: speed })}
                  className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                    playbackRate === speed
                      ? 'bg-primary-500 text-white'
                      : 'bg-surface-100 dark:bg-surface-800 hover:bg-surface-200 dark:hover:bg-surface-700'
                  }`}
                >
                  {speed}x
                </button>
              ))}
            </div>
          </div>
        </div>
      </Modal>
    </motion.div>
  );
}
