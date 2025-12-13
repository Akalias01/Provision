import { useRef, useEffect, useCallback } from 'react';
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
  List,
  ChevronDown,
} from 'lucide-react';
import { Button, Slider } from '../ui';
import { Waveform } from './Waveform';
import { useStore } from '../../store/useStore';
import { formatTime } from '../../utils/formatTime';

interface AudioPlayerProps {
  onBack?: () => void;
}

export function AudioPlayer({ onBack }: AudioPlayerProps) {
  const audioRef = useRef<HTMLAudioElement>(null);
  const {
    currentBook,
    playerState,
    setPlayerState,
    updateBook,
    setCurrentView,
  } = useStore();

  const { isPlaying, currentTime, duration, volume, playbackRate, isMuted } = playerState;

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

  useEffect(() => {
    const audio = audioRef.current;
    if (!audio) return;

    if (isPlaying) {
      audio.play().catch(() => setPlayerState({ isPlaying: false }));
    } else {
      audio.pause();
    }
  }, [isPlaying]);

  useEffect(() => {
    if (audioRef.current) {
      audioRef.current.volume = isMuted ? 0 : volume;
    }
  }, [volume, isMuted]);

  useEffect(() => {
    if (audioRef.current) {
      audioRef.current.playbackRate = playbackRate;
    }
  }, [playbackRate]);

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

  const progress = duration > 0 ? currentTime / duration : 0;

  if (!currentBook) return null;

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
        <Button variant="icon">
          <List className="w-5 h-5" />
        </Button>
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
            onClick={() => handleSkip(-30)}
            className="relative"
          >
            <Rewind className="w-6 h-6" />
            <span className="absolute -bottom-1 text-[10px] font-medium">30</span>
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
            onClick={() => handleSkip(30)}
            className="relative"
          >
            <FastForward className="w-6 h-6" />
            <span className="absolute -bottom-1 text-[10px] font-medium">30</span>
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
    </motion.div>
  );
}
