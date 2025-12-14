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
  Clock,
  Trash2,
  Edit3,
  Check,
  MoreVertical,
  List,
  HelpCircle,
  Gauge,
  Sliders,
} from 'lucide-react';
import { Button, Slider, Modal } from '../ui';
import { Waveform } from './Waveform';
import { Equalizer, setAmplifierGain, resumeAudioContext } from './Equalizer';
import { Tutorial } from '../Tutorial';
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
  const lastTapRef = useRef<number>(0);
  const lastTapXRef = useRef<number>(0);
  const isDraggingRef = useRef<boolean>(false);
  const dragStartXRef = useRef<number>(0);
  const dragStartTimeRef = useRef<number>(0);

  const [showSleepTimer, setShowSleepTimer] = useState(false);
  const [showBookmarks, setShowBookmarks] = useState(false);
  const [showChapters, setShowChapters] = useState(false);
  const [showMoreMenu, setShowMoreMenu] = useState(false);
  const [showTips, setShowTips] = useState(false);
  const [showTutorial, setShowTutorial] = useState(false);
  const [showSpeedPicker, setShowSpeedPicker] = useState(false);
  const [showAmplifier, setShowAmplifier] = useState(false);
  const [bookmarkNote, setBookmarkNote] = useState('');
  const [editingBookmarkId, setEditingBookmarkId] = useState<string | null>(null);
  const [editingNote, setEditingNote] = useState('');
  const [amplifierGain, setLocalAmplifierGain] = useState(1);
  const [gestureIndicator, setGestureIndicator] = useState<string | null>(null);

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
    addBookmark,
    removeBookmark,
    updateBookmarkNote,
  } = useStore();

  const { isPlaying, currentTime, duration, volume, playbackRate, isMuted } = playerState;

  // Update amplifier gain using shared audio context
  useEffect(() => {
    setAmplifierGain(amplifierGain);
  }, [amplifierGain]);

  // Audio setup
  useEffect(() => {
    const audio = audioRef.current;
    if (!audio || !currentBook) return;

    console.log('[AudioPlayer] Setting up audio for:', currentBook.title);
    console.log('[AudioPlayer] File URL:', currentBook.fileUrl);

    // Reset audio state
    audio.pause();
    audio.currentTime = 0;

    // Set the source
    audio.src = currentBook.fileUrl;
    audio.volume = volume;
    audio.playbackRate = playbackRate;

    const handleLoadedMetadata = () => {
      console.log('[AudioPlayer] Loaded metadata, duration:', audio.duration);
      setPlayerState({ duration: audio.duration });
      // Restore position after metadata is loaded
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

    const handleCanPlay = () => {
      console.log('[AudioPlayer] Audio can play');
    };

    audio.addEventListener('loadedmetadata', handleLoadedMetadata);
    audio.addEventListener('timeupdate', handleTimeUpdate);
    audio.addEventListener('ended', handleEnded);
    audio.addEventListener('error', handleError);
    audio.addEventListener('canplay', handleCanPlay);

    // Try to load the audio
    audio.load();

    return () => {
      audio.removeEventListener('loadedmetadata', handleLoadedMetadata);
      audio.removeEventListener('timeupdate', handleTimeUpdate);
      audio.removeEventListener('ended', handleEnded);
      audio.removeEventListener('error', handleError);
      audio.removeEventListener('canplay', handleCanPlay);
    };
  }, [currentBook?.id, currentBook?.fileUrl]);

  // Play/pause control
  useEffect(() => {
    const audio = audioRef.current;
    if (!audio) return;

    if (isPlaying) {
      // Resume shared audio context if suspended
      resumeAudioContext();

      // Wait for audio to be ready before playing
      const attemptPlay = () => {
        if (audio.readyState >= 2) { // HAVE_CURRENT_DATA or higher
          audio.play()
            .then(() => {
              console.log('[AudioPlayer] Playback started successfully');
            })
            .catch((error) => {
              console.error('[AudioPlayer] Play error:', error.name, error.message);
              setPlayerState({ isPlaying: false });
            });
        } else {
          console.log('[AudioPlayer] Audio not ready, waiting... readyState:', audio.readyState);
          // Wait for canplay event
          const handleCanPlay = () => {
            audio.removeEventListener('canplay', handleCanPlay);
            audio.play()
              .then(() => {
                console.log('[AudioPlayer] Playback started after waiting');
              })
              .catch((error) => {
                console.error('[AudioPlayer] Play error after waiting:', error.name, error.message);
                setPlayerState({ isPlaying: false });
              });
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

  // Android Auto: Update media metadata when book changes
  useEffect(() => {
    if (isAndroid() && currentBook) {
      updateMediaMetadata(
        currentBook.title,
        currentBook.author || 'Unknown Author',
        currentBook.cover,
        duration
      );
    }
  }, [currentBook?.id, currentBook?.title, currentBook?.author, currentBook?.cover, duration]);

  // Android Auto: Update playback state when playing/pausing
  useEffect(() => {
    if (isAndroid()) {
      updatePlaybackState(isPlaying, currentTime, playbackRate);
    }
  }, [isPlaying, playbackRate]);

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

  const goToNextChapter = useCallback(() => {
    const chapters = currentBook?.chapters || [];
    if (chapters.length === 0) {
      // No chapters, skip forward 5 minutes
      handleSkip(300);
      return;
    }
    const nextChapter = chapters.find(ch => (ch.startTime || 0) > currentTime);
    if (nextChapter && nextChapter.startTime !== undefined) {
      handleSeek(nextChapter.startTime);
    }
  }, [currentBook, currentTime, handleSeek, handleSkip]);

  const goToPrevChapter = useCallback(() => {
    const chapters = currentBook?.chapters || [];
    if (chapters.length === 0) {
      // No chapters, skip backward 5 minutes
      handleSkip(-300);
      return;
    }
    // Find current chapter and go to previous
    const currentChapterIndex = chapters.findIndex(
      (ch, i) => (ch.startTime || 0) <= currentTime &&
        (i === chapters.length - 1 || (chapters[i + 1].startTime || 0) > currentTime)
    );
    if (currentChapterIndex > 0) {
      const prevChapter = chapters[currentChapterIndex - 1];
      if (prevChapter.startTime !== undefined) {
        handleSeek(prevChapter.startTime);
      }
    } else {
      handleSeek(0);
    }
  }, [currentBook, currentTime, handleSeek, handleSkip]);

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
      // Scrub: 100px = 30 seconds
      const scrubAmount = (deltaX / 100) * 30;
      const newTime = Math.max(0, Math.min(duration, dragStartTimeRef.current + scrubAmount));
      setGestureIndicator(deltaX > 0 ? `+${formatTime(scrubAmount)}` : formatTime(scrubAmount));
      handleSeek(newTime);
    }
  }, [duration, handleSeek]);

  const handleTouchEnd = useCallback((e: React.TouchEvent | React.MouseEvent) => {
    if (isDraggingRef.current) {
      isDraggingRef.current = false;
      setGestureIndicator(null);
      return;
    }

    // Double-tap detection
    const now = Date.now();
    const clientX = 'changedTouches' in e ? e.changedTouches[0].clientX : e.clientX;

    if (now - lastTapRef.current < 300) {
      // Double tap detected
      const rect = coverRef.current?.getBoundingClientRect();
      if (rect) {
        const relativeX = clientX - rect.left;
        const third = rect.width / 3;

        if (relativeX < third) {
          // Left third - previous chapter
          goToPrevChapter();
          setGestureIndicator('Previous Chapter');
        } else if (relativeX > third * 2) {
          // Right third - next chapter
          goToNextChapter();
          setGestureIndicator('Next Chapter');
        } else {
          // Center - play/pause
          togglePlayPause();
          setGestureIndicator(isPlaying ? 'Pause' : 'Play');
        }
        setTimeout(() => setGestureIndicator(null), 800);
      }
    }

    lastTapRef.current = now;
    lastTapXRef.current = clientX;
  }, [goToPrevChapter, goToNextChapter, togglePlayPause, isPlaying]);

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
    setShowMoreMenu(false);
  }, [currentBook, currentTime, bookmarkNote, addBookmark]);

  const handleJumpToBookmark = useCallback((position: number) => {
    if (audioRef.current) {
      audioRef.current.currentTime = position;
      setPlayerState({ currentTime: position });
    }
    setShowBookmarks(false);
  }, []);

  const handleJumpToChapter = useCallback((startTime: number) => {
    if (audioRef.current) {
      audioRef.current.currentTime = startTime;
      setPlayerState({ currentTime: startTime });
    }
    setShowChapters(false);
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

  // Speed options from 1.0 to 2.0 in 0.1 increments
  const speedOptions = Array.from({ length: 11 }, (_, i) => Number((1 + i * 0.1).toFixed(1)));

  if (!currentBook) return null;

  const bookmarks = currentBook.bookmarks || [];
  const chapters = currentBook.chapters || [];

  // Find current chapter
  const currentChapter = chapters.find(
    (ch, i) => (ch.startTime || 0) <= currentTime &&
      (i === chapters.length - 1 || (chapters[i + 1].startTime || 0) > currentTime)
  );

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="h-full flex flex-col bg-gradient-to-b from-surface-100 to-surface-50 dark:from-surface-900 dark:to-surface-950"
    >
      <audio ref={audioRef} preload="metadata" crossOrigin="anonymous" />

      {/* Header */}
      <div className="flex items-center justify-between p-4">
        <Button variant="icon" onClick={onBack || (() => setCurrentView('library'))}>
          <ChevronDown className="w-6 h-6" />
        </Button>

        {/* Chapter selector button */}
        <button
          onClick={() => setShowChapters(true)}
          className="flex items-center gap-2 px-3 py-1.5 rounded-lg hover:bg-surface-200 dark:hover:bg-surface-800 transition-colors"
        >
          <List className="w-4 h-4 text-surface-500" />
          <span className="text-sm text-surface-500 dark:text-surface-400 max-w-[150px] truncate">
            {currentChapter?.title || 'Now Playing'}
          </span>
        </button>

        {/* 3-dot menu */}
        <Button variant="icon" onClick={() => setShowMoreMenu(true)}>
          <MoreVertical className="w-5 h-5" />
        </Button>
      </div>

      {/* Cover Art with gesture support */}
      <div
        ref={coverRef}
        className="flex-1 flex items-center justify-center px-8 py-4 select-none"
        onTouchStart={handleTouchStart}
        onTouchMove={handleTouchMove}
        onTouchEnd={handleTouchEnd}
        onMouseDown={handleTouchStart}
        onMouseMove={(e) => e.buttons === 1 && handleTouchMove(e)}
        onMouseUp={handleTouchEnd}
      >
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
              className="w-full h-full object-cover rounded-3xl shadow-2xl shadow-primary-500/20 pointer-events-none"
              draggable={false}
            />
          ) : (
            <div className="w-full h-full rounded-3xl bg-gradient-to-br from-primary-500 to-primary-700 flex items-center justify-center shadow-2xl shadow-primary-500/20">
              <BookOpen className="w-24 h-24 text-white/80" />
            </div>
          )}

          {/* Gesture indicator overlay */}
          <AnimatePresence>
            {gestureIndicator && (
              <motion.div
                initial={{ opacity: 0, scale: 0.8 }}
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0, scale: 0.8 }}
                className="absolute inset-0 flex items-center justify-center bg-black/50 rounded-3xl"
              >
                <span className="text-white text-2xl font-bold">{gestureIndicator}</span>
              </motion.div>
            )}
          </AnimatePresence>

          {/* Glow effect when playing */}
          <AnimatePresence>
            {isPlaying && !gestureIndicator && (
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

          {/* Previous Chapter */}
          <Button variant="ghost" size="lg" onClick={goToPrevChapter}>
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

          {/* Next Chapter */}
          <Button variant="ghost" size="lg" onClick={goToNextChapter}>
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
            onClick={() => setShowSpeedPicker(true)}
            className="text-xs font-semibold min-w-[60px]"
          >
            {playbackRate.toFixed(1)}x
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

          {/* Bookmarks */}
          <Button
            variant="ghost"
            size="sm"
            onClick={() => setShowBookmarks(true)}
          >
            <Bookmark className="w-5 h-5" />
          </Button>

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

      {/* 3-Dot More Menu */}
      <Modal
        isOpen={showMoreMenu}
        onClose={() => setShowMoreMenu(false)}
        title="Options"
        size="sm"
      >
        <div className="space-y-2">
          <Equalizer audioRef={audioRef} asMenuItem onClose={() => setShowMoreMenu(false)} />

          <button
            onClick={() => {
              setShowMoreMenu(false);
              setShowAmplifier(true);
            }}
            className="w-full flex items-center gap-3 p-4 rounded-xl hover:bg-surface-100 dark:hover:bg-surface-800 transition-colors"
          >
            <Gauge className="w-5 h-5 text-primary-500" />
            <div className="text-left">
              <p className="font-medium text-surface-900 dark:text-white">Amplifier</p>
              <p className="text-sm text-surface-500">Boost audio volume</p>
            </div>
          </button>

          <button
            onClick={handleAddBookmark}
            className="w-full flex items-center gap-3 p-4 rounded-xl hover:bg-surface-100 dark:hover:bg-surface-800 transition-colors"
          >
            <BookmarkPlus className="w-5 h-5 text-primary-500" />
            <div className="text-left">
              <p className="font-medium text-surface-900 dark:text-white">Add Bookmark</p>
              <p className="text-sm text-surface-500">Save current position</p>
            </div>
          </button>

          <button
            onClick={() => {
              setShowMoreMenu(false);
              setShowTips(true);
            }}
            className="w-full flex items-center gap-3 p-4 rounded-xl hover:bg-surface-100 dark:hover:bg-surface-800 transition-colors"
          >
            <HelpCircle className="w-5 h-5 text-primary-500" />
            <div className="text-left">
              <p className="font-medium text-surface-900 dark:text-white">Player Tips</p>
              <p className="text-sm text-surface-500">Learn gestures & controls</p>
            </div>
          </button>

          <button
            onClick={() => {
              setShowMoreMenu(false);
              setShowSpeedPicker(true);
            }}
            className="w-full flex items-center gap-3 p-4 rounded-xl hover:bg-surface-100 dark:hover:bg-surface-800 transition-colors"
          >
            <Sliders className="w-5 h-5 text-primary-500" />
            <div className="text-left">
              <p className="font-medium text-surface-900 dark:text-white">Playback Speed</p>
              <p className="text-sm text-surface-500">Current: {playbackRate.toFixed(1)}x</p>
            </div>
          </button>
        </div>
      </Modal>

      {/* Chapter Selection Modal */}
      <Modal
        isOpen={showChapters}
        onClose={() => setShowChapters(false)}
        title="Chapters"
        size="md"
      >
        <div className="max-h-96 overflow-y-auto space-y-1">
          {chapters.length === 0 ? (
            <p className="text-center text-surface-500 py-8">No chapters available</p>
          ) : (
            chapters.map((chapter, index) => {
              const isCurrentChapter = currentChapter?.id === chapter.id;
              return (
                <button
                  key={chapter.id}
                  onClick={() => handleJumpToChapter(chapter.startTime || 0)}
                  className={`w-full flex items-center gap-3 p-4 rounded-xl transition-colors ${
                    isCurrentChapter
                      ? 'bg-primary-500/10 border border-primary-500'
                      : 'hover:bg-surface-100 dark:hover:bg-surface-800'
                  }`}
                >
                  <div className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-medium ${
                    isCurrentChapter
                      ? 'bg-primary-500 text-white'
                      : 'bg-surface-200 dark:bg-surface-700 text-surface-600 dark:text-surface-300'
                  }`}>
                    {index + 1}
                  </div>
                  <div className="flex-1 text-left">
                    <p className={`font-medium ${isCurrentChapter ? 'text-primary-500' : 'text-surface-900 dark:text-white'}`}>
                      {chapter.title}
                    </p>
                    {chapter.duration && (
                      <p className="text-sm text-surface-500">{formatTime(chapter.duration)}</p>
                    )}
                  </div>
                  {chapter.startTime !== undefined && (
                    <span className="text-sm text-surface-400">{formatTime(chapter.startTime)}</span>
                  )}
                </button>
              );
            })
          )}
        </div>
      </Modal>

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
                  : 'bg-surface-100 dark:bg-surface-800 hover:bg-surface-200 dark:hover:bg-surface-700'
              }`}
            >
              {speed.toFixed(1)}x
            </button>
          ))}
        </div>
      </Modal>

      {/* Amplifier Modal */}
      <Modal
        isOpen={showAmplifier}
        onClose={() => setShowAmplifier(false)}
        title="Audio Amplifier"
        size="sm"
      >
        <div className="space-y-6">
          <p className="text-sm text-surface-500">
            Boost your audio beyond normal volume. Use carefully to avoid distortion.
          </p>
          <div>
            <div className="flex justify-between mb-2">
              <span className="text-sm font-medium">Gain</span>
              <span className="text-sm text-primary-500 font-bold">{amplifierGain.toFixed(1)}x</span>
            </div>
            <Slider
              value={amplifierGain}
              min={1}
              max={3}
              step={0.1}
              onChange={setLocalAmplifierGain}
            />
            <div className="flex justify-between mt-1 text-xs text-surface-400">
              <span>1x (Normal)</span>
              <span>3x (Max)</span>
            </div>
          </div>
          <button
            onClick={() => setLocalAmplifierGain(1)}
            className="w-full py-2 text-sm text-surface-500 hover:text-surface-700 dark:hover:text-surface-300"
          >
            Reset to Normal
          </button>
        </div>
      </Modal>

      {/* Tips Modal */}
      <Modal
        isOpen={showTips}
        onClose={() => setShowTips(false)}
        title="Player Tips"
        size="md"
      >
        <div className="space-y-4">
          <div className="p-4 bg-surface-100 dark:bg-surface-800 rounded-xl">
            <h4 className="font-medium text-surface-900 dark:text-white mb-2">Gesture Controls</h4>
            <ul className="space-y-2 text-sm text-surface-600 dark:text-surface-400">
              <li className="flex items-center gap-2">
                <span className="w-24 font-medium">Double-tap left</span>
                <span>Previous chapter</span>
              </li>
              <li className="flex items-center gap-2">
                <span className="w-24 font-medium">Double-tap right</span>
                <span>Next chapter</span>
              </li>
              <li className="flex items-center gap-2">
                <span className="w-24 font-medium">Double-tap center</span>
                <span>Play/Pause</span>
              </li>
              <li className="flex items-center gap-2">
                <span className="w-24 font-medium">Swipe & hold</span>
                <span>Scrub forward/backward</span>
              </li>
            </ul>
          </div>

          <div className="p-4 bg-surface-100 dark:bg-surface-800 rounded-xl">
            <h4 className="font-medium text-surface-900 dark:text-white mb-2">Playback Controls</h4>
            <ul className="space-y-2 text-sm text-surface-600 dark:text-surface-400">
              <li className="flex items-center gap-2">
                <span className="w-24 font-medium">Speed</span>
                <span>1.0x to 2.0x in 0.1 increments</span>
              </li>
              <li className="flex items-center gap-2">
                <span className="w-24 font-medium">Skip buttons</span>
                <span>Jump {skipInterval} seconds</span>
              </li>
              <li className="flex items-center gap-2">
                <span className="w-24 font-medium">Sleep timer</span>
                <span>Auto-stop playback</span>
              </li>
            </ul>
          </div>

          <div className="p-4 bg-surface-100 dark:bg-surface-800 rounded-xl">
            <h4 className="font-medium text-surface-900 dark:text-white mb-2">Audio Features</h4>
            <ul className="space-y-2 text-sm text-surface-600 dark:text-surface-400">
              <li className="flex items-center gap-2">
                <span className="w-24 font-medium">Equalizer</span>
                <span>Adjust bass, mid, treble</span>
              </li>
              <li className="flex items-center gap-2">
                <span className="w-24 font-medium">Amplifier</span>
                <span>Boost volume up to 3x</span>
              </li>
              <li className="flex items-center gap-2">
                <span className="w-24 font-medium">Bookmarks</span>
                <span>Save positions with notes</span>
              </li>
            </ul>
          </div>

          {/* Interactive Tutorial Button */}
          <Button
            variant="primary"
            className="w-full mt-4"
            onClick={() => {
              setShowTips(false);
              setShowTutorial(true);
            }}
          >
            <HelpCircle className="w-4 h-4" />
            Start Interactive Tutorial
          </Button>
        </div>
      </Modal>

      {/* Interactive Tutorial */}
      <Tutorial isOpen={showTutorial} onClose={() => setShowTutorial(false)} />

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
    </motion.div>
  );
}
