import { useRef, useEffect, useCallback, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Play,
  Pause,
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
  List,
  MoreVertical,
  SlidersHorizontal,
  Lightbulb,
  Search,
} from 'lucide-react';
import { Slider, Modal } from '../ui';
import { seekAudio } from './AudioController';
import { useStore, type SleepTimerMode } from '../../store/useStore';
import { formatTime } from '../../utils/formatTime';

interface AudioPlayerProps {
  onBack?: () => void;
}

export function AudioPlayer(_props: AudioPlayerProps) {
  const sleepTimerRef = useRef<NodeJS.Timeout | null>(null);

  const [showChaptersSheet, setShowChaptersSheet] = useState(false);
  const [showMenu, setShowMenu] = useState(false);
  const [showSleepTimer, setShowSleepTimer] = useState(false);
  const [showSpeedPicker, setShowSpeedPicker] = useState(false);
  const [showBookmarkModal, setShowBookmarkModal] = useState(false);
  const [showTips, setShowTips] = useState(false);
  const [bookmarkNote, setBookmarkNote] = useState('');
  const [editingBookmarkId, setEditingBookmarkId] = useState<string | null>(null);
  const [editingNote, setEditingNote] = useState('');
  const [chaptersTab, setChaptersTab] = useState<'chapters' | 'notes'>('chapters');

  const {
    currentBook,
    playerState,
    setPlayerState,
    sleepTimerMode,
    sleepTimerRemaining,
    setSleepTimerMode,
    setSleepTimerRemaining,
    addBookmark,
    removeBookmark,
    updateBookmarkNote,
  } = useStore();

  const { isPlaying, currentTime, duration, playbackRate } = playerState;

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
    seekAudio(value);
    setPlayerState({ currentTime: value });
  }, [setPlayerState]);

  const handleSkip = useCallback((seconds: number) => {
    const newTime = Math.max(0, Math.min(duration, currentTime + seconds));
    seekAudio(newTime);
    setPlayerState({ currentTime: newTime });
  }, [currentTime, duration, setPlayerState]);

  const togglePlayPause = useCallback(() => {
    setPlayerState({ isPlaying: !isPlaying });
  }, [isPlaying]);

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
    setShowBookmarkModal(false);
  }, [currentBook, currentTime, bookmarkNote, addBookmark]);

  const handleJumpToBookmark = useCallback((position: number) => {
    seekAudio(position);
    setPlayerState({ currentTime: position });
    setShowChaptersSheet(false);
  }, [setPlayerState]);

  const handleJumpToChapter = useCallback((startTime: number) => {
    seekAudio(startTime);
    setPlayerState({ currentTime: startTime });
    setShowChaptersSheet(false);
  }, [setPlayerState]);


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

  // Calculate times
  const bookRemainingTime = duration - currentTime;

  // Chapter progress (if chapters exist)
  const chapterStartTime = currentChapter?.startTime || 0;
  const nextChapterIndex = currentChapterIndex < chapters.length ? currentChapterIndex : -1;
  const chapterEndTime = nextChapterIndex >= 0 && chapters[nextChapterIndex]
    ? chapters[nextChapterIndex].startTime || duration
    : duration;
  const chapterDuration = chapterEndTime - chapterStartTime;
  const chapterCurrentTime = currentTime - chapterStartTime;
  const chapterRemainingTime = chapterEndTime - currentTime;

  return (
    <motion.div
      initial={{ opacity: 0, y: '100%' }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: '100%' }}
      transition={{ type: 'spring', damping: 25, stiffness: 200 }}
      className="h-full flex flex-col bg-surface-950 overflow-hidden"
    >
      {/* Header: Thumbnail | Title + Author | Chapters Button | 3-dot Menu */}
      <div className="flex-shrink-0 flex items-center gap-3 px-4 py-3 safe-area-top">
        {/* Thumbnail */}
        <div className="w-12 h-12 rounded-lg overflow-hidden flex-shrink-0 bg-surface-800">
          {currentBook.cover ? (
            <img src={currentBook.cover} alt="" className="w-full h-full object-cover" />
          ) : (
            <div className="w-full h-full flex items-center justify-center bg-primary-500/20">
              <Headphones className="w-6 h-6 text-primary-500" />
            </div>
          )}
        </div>

        {/* Title + Author */}
        <div className="flex-1 min-w-0">
          <p className="text-sm font-semibold text-white truncate">
            {currentBook.title}
          </p>
          <p className="text-xs text-surface-400 truncate">
            {currentBook.author || 'Unknown Author'}
          </p>
        </div>

        {/* Chapters Button */}
        <button
          onClick={() => setShowChaptersSheet(true)}
          className="p-2 text-surface-400 hover:text-white transition-colors"
        >
          <List className="w-6 h-6" />
        </button>

        {/* 3-dot Menu */}
        <button
          onClick={() => setShowMenu(true)}
          className="p-2 text-surface-400 hover:text-white transition-colors"
        >
          <MoreVertical className="w-6 h-6" />
        </button>
      </div>

      {/* Large Cover Art - Full Width, Squared */}
      <div className="flex-1 min-h-0 flex items-center justify-center p-4">
        <div className="relative w-full max-w-sm aspect-square">
          {currentBook.cover ? (
            <img
              src={currentBook.cover}
              alt={currentBook.title}
              className="w-full h-full object-cover rounded-lg shadow-2xl shadow-black/50"
              draggable={false}
            />
          ) : (
            <div className="w-full h-full rounded-lg bg-gradient-to-br from-primary-500 to-primary-700 flex items-center justify-center shadow-2xl">
              <Headphones className="w-24 h-24 text-white/80" />
            </div>
          )}
        </div>
      </div>

      {/* Book Progress Bar: elapsed | chapter x/y | remaining */}
      <div className="flex-shrink-0 px-4">
        <div className="h-1 bg-surface-800 rounded-full overflow-hidden">
          <motion.div
            className="h-full bg-primary-500"
            style={{ width: `${(currentTime / duration) * 100}%` }}
          />
        </div>
        <div className="flex justify-between mt-1.5 text-xs text-surface-400">
          <span>{formatTime(currentTime)}</span>
          <span className="font-medium">
            {chapters.length > 0 ? `${currentChapterIndex}/${chapters.length}` : ''}
          </span>
          <span>-{formatTime(bookRemainingTime)}</span>
        </div>
      </div>

      {/* Chapter Name */}
      <div className="flex-shrink-0 px-4 py-3">
        <p className="text-center text-lg font-semibold text-white">
          {currentChapter?.title || currentBook.title}
        </p>
      </div>

      {/* Chapter Progress Bar */}
      {chapters.length > 0 && (
        <div className="flex-shrink-0 px-4 pb-2">
          <Slider
            value={chapterCurrentTime}
            max={chapterDuration || 100}
            onChange={(v) => handleSeek(chapterStartTime + v)}
            showTooltip
            formatTooltip={formatTime}
          />
          <div className="flex justify-between mt-1.5 text-xs text-surface-400">
            <span>{formatTime(chapterCurrentTime)}</span>
            <span>-{formatTime(chapterRemainingTime)}</span>
          </div>
        </div>
      )}

      {/* If no chapters, show main progress slider */}
      {chapters.length === 0 && (
        <div className="flex-shrink-0 px-4 pb-2">
          <Slider
            value={currentTime}
            max={duration || 100}
            onChange={handleSeek}
            showTooltip
            formatTooltip={formatTime}
          />
          <div className="flex justify-between mt-1.5 text-xs text-surface-400">
            <span>{formatTime(currentTime)}</span>
            <span>-{formatTime(bookRemainingTime)}</span>
          </div>
        </div>
      )}

      {/* Controls: Speed | Rewind | Play | Forward | Sleep */}
      <div className="flex-shrink-0 px-6 py-4 safe-area-bottom">
        <div className="flex items-center justify-between">
          {/* Speed */}
          <button
            onClick={() => setShowSpeedPicker(true)}
            className="w-12 h-12 flex items-center justify-center text-sm font-bold text-surface-300 hover:text-white transition-colors"
          >
            {playbackRate}x
          </button>

          {/* Rewind 10s */}
          <button
            onClick={() => handleSkip(-10)}
            className="w-14 h-14 flex items-center justify-center text-surface-300 hover:text-white transition-colors relative"
          >
            <RotateCcw className="w-8 h-8" />
            <span className="absolute text-[10px] font-bold">10</span>
          </button>

          {/* Play/Pause */}
          <motion.button
            whileTap={{ scale: 0.95 }}
            onClick={togglePlayPause}
            className="w-18 h-18 rounded-full bg-primary-500 text-white shadow-lg shadow-primary-500/30 flex items-center justify-center"
            style={{ width: 72, height: 72 }}
          >
            {isPlaying ? (
              <Pause className="w-9 h-9" fill="currentColor" />
            ) : (
              <Play className="w-9 h-9 ml-1" fill="currentColor" />
            )}
          </motion.button>

          {/* Forward 10s */}
          <button
            onClick={() => handleSkip(10)}
            className="w-14 h-14 flex items-center justify-center text-surface-300 hover:text-white transition-colors relative"
          >
            <RotateCw className="w-8 h-8" />
            <span className="absolute text-[10px] font-bold">10</span>
          </button>

          {/* Sleep Timer */}
          <button
            onClick={() => setShowSleepTimer(true)}
            className="w-12 h-12 flex items-center justify-center text-surface-300 hover:text-white transition-colors relative"
          >
            <Moon className="w-6 h-6" />
            {sleepTimerRemaining !== null && (
              <span className="absolute -top-1 -right-1 text-[9px] bg-primary-500 text-white rounded-full w-5 h-5 flex items-center justify-center font-bold">
                {Math.ceil(sleepTimerRemaining / 60)}
              </span>
            )}
          </button>
        </div>
      </div>

      {/* ===== CHAPTERS SHEET (Full Screen Animated) ===== */}
      <AnimatePresence>
        {showChaptersSheet && (
          <motion.div
            initial={{ opacity: 0, y: '100%' }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: '100%' }}
            transition={{ type: 'spring', damping: 25, stiffness: 200 }}
            className="fixed inset-0 z-50 bg-surface-950 flex flex-col"
          >
            {/* Header */}
            <div className="flex-shrink-0 flex items-center gap-3 px-4 py-3 safe-area-top border-b border-surface-800">
              {/* Thumbnail */}
              <div className="w-12 h-12 rounded-lg overflow-hidden flex-shrink-0 bg-surface-800">
                {currentBook.cover ? (
                  <img src={currentBook.cover} alt="" className="w-full h-full object-cover" />
                ) : (
                  <div className="w-full h-full flex items-center justify-center bg-primary-500/20">
                    <Headphones className="w-6 h-6 text-primary-500" />
                  </div>
                )}
              </div>

              {/* Title + Author */}
              <div className="flex-1 min-w-0">
                <p className="text-sm font-semibold text-white truncate">{currentBook.title}</p>
                <p className="text-xs text-surface-400 truncate">{currentBook.author || 'Unknown Author'}</p>
              </div>

              {/* Close Button */}
              <button
                onClick={() => setShowChaptersSheet(false)}
                className="p-2 text-surface-400 hover:text-white transition-colors"
              >
                <List className="w-6 h-6" />
              </button>

              <button
                onClick={() => setShowMenu(true)}
                className="p-2 text-surface-400 hover:text-white transition-colors"
              >
                <MoreVertical className="w-6 h-6" />
              </button>
            </div>

            {/* Tabs: CHAPTERS | NOTES */}
            <div className="flex-shrink-0 flex border-b border-surface-800">
              <button
                onClick={() => setChaptersTab('chapters')}
                className={`flex-1 py-4 text-sm font-bold uppercase tracking-wider transition-colors ${
                  chaptersTab === 'chapters'
                    ? 'text-primary-500 border-b-2 border-primary-500'
                    : 'text-surface-400 hover:text-white'
                }`}
              >
                Chapters
              </button>
              <button
                onClick={() => setChaptersTab('notes')}
                className={`flex-1 py-4 text-sm font-bold uppercase tracking-wider transition-colors ${
                  chaptersTab === 'notes'
                    ? 'text-primary-500 border-b-2 border-primary-500'
                    : 'text-surface-400 hover:text-white'
                }`}
              >
                Notes
              </button>
            </div>

            {/* Content */}
            <div className="flex-1 overflow-auto safe-area-bottom">
              {chaptersTab === 'chapters' ? (
                <div className="py-2">
                  {chapters.length === 0 ? (
                    <p className="text-center text-surface-500 py-12">No chapters available</p>
                  ) : (
                    chapters.map((chapter, index) => {
                      const isCurrentChapter = currentChapter?.id === chapter.id;
                      const chapterDur = index < chapters.length - 1
                        ? (chapters[index + 1].startTime || 0) - (chapter.startTime || 0)
                        : duration - (chapter.startTime || 0);
                      return (
                        <button
                          key={chapter.id}
                          onClick={() => handleJumpToChapter(chapter.startTime || 0)}
                          className={`w-full flex items-center justify-between px-4 py-4 transition-colors ${
                            isCurrentChapter ? 'text-primary-400' : 'text-white hover:bg-surface-900'
                          }`}
                        >
                          <span className={`font-semibold ${isCurrentChapter ? 'text-primary-400' : ''}`}>
                            {chapter.title}
                          </span>
                          <span className={`text-sm ${isCurrentChapter ? 'text-primary-400' : 'text-surface-400'}`}>
                            {formatTime(chapterDur)}
                          </span>
                        </button>
                      );
                    })
                  )}
                </div>
              ) : (
                <div className="p-4 space-y-3">
                  {/* Add bookmark button */}
                  <button
                    onClick={() => setShowBookmarkModal(true)}
                    className="w-full flex items-center gap-3 p-4 rounded-xl bg-surface-800 text-primary-400 hover:bg-surface-700 transition-colors"
                  >
                    <BookmarkPlus className="w-5 h-5" />
                    <span className="font-semibold">Add bookmark at {formatTime(currentTime)}</span>
                  </button>

                  {bookmarks.length === 0 ? (
                    <p className="text-center text-surface-500 py-12">No bookmarks yet</p>
                  ) : (
                    bookmarks
                      .sort((a, b) => a.position - b.position)
                      .map((bookmark) => (
                        <div
                          key={bookmark.id}
                          className="flex items-center gap-3 p-4 bg-surface-800 rounded-xl"
                        >
                          <button
                            onClick={() => handleJumpToBookmark(bookmark.position)}
                            className="flex-1 text-left"
                          >
                            <div className="flex items-center gap-2">
                              <Bookmark className="w-4 h-4 text-primary-500" />
                              <span className="font-semibold text-white">{formatTime(bookmark.position)}</span>
                            </div>
                            {editingBookmarkId === bookmark.id ? (
                              <div className="flex items-center gap-2 mt-2">
                                <input
                                  type="text"
                                  value={editingNote}
                                  onChange={(e) => setEditingNote(e.target.value)}
                                  className="flex-1 bg-surface-700 px-3 py-2 rounded-lg text-sm text-white"
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
                                  className="p-2 text-green-500"
                                >
                                  <Check className="w-5 h-5" />
                                </button>
                              </div>
                            ) : bookmark.note ? (
                              <p className="text-sm text-surface-400 mt-1">{bookmark.note}</p>
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
                              <Edit3 className="w-5 h-5" />
                            </button>
                            <button
                              onClick={() => currentBook && removeBookmark(currentBook.id, bookmark.id)}
                              className="p-2 text-surface-400 hover:text-red-500"
                            >
                              <Trash2 className="w-5 h-5" />
                            </button>
                          </div>
                        </div>
                      ))
                  )}
                </div>
              )}
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* ===== 3-DOT MENU MODAL ===== */}
      <Modal
        isOpen={showMenu}
        onClose={() => setShowMenu(false)}
        title="Options"
        size="sm"
      >
        <div className="space-y-2">
          {/* Equalizer & Amplifier */}
          <button
            onClick={() => {
              setShowMenu(false);
              // TODO: Open equalizer
            }}
            className="w-full flex items-center gap-4 p-4 rounded-xl bg-surface-800 hover:bg-surface-700 transition-colors"
          >
            <SlidersHorizontal className="w-6 h-6 text-primary-500" />
            <div className="text-left">
              <p className="font-semibold text-white">Equalizer & Amplifier</p>
              <p className="text-sm text-surface-400">Adjust audio settings</p>
            </div>
          </button>

          {/* Add Bookmark */}
          <button
            onClick={() => {
              setShowMenu(false);
              setShowBookmarkModal(true);
            }}
            className="w-full flex items-center gap-4 p-4 rounded-xl bg-surface-800 hover:bg-surface-700 transition-colors"
          >
            <BookmarkPlus className="w-6 h-6 text-primary-500" />
            <div className="text-left">
              <p className="font-semibold text-white">Add Bookmark</p>
              <p className="text-sm text-surface-400">Save current position with notes</p>
            </div>
          </button>

          {/* Edit Book Info */}
          <button
            onClick={() => {
              setShowMenu(false);
              // TODO: Open edit book info modal to fetch metadata
            }}
            className="w-full flex items-center gap-4 p-4 rounded-xl bg-surface-800 hover:bg-surface-700 transition-colors"
          >
            <Search className="w-6 h-6 text-primary-500" />
            <div className="text-left">
              <p className="font-semibold text-white">Edit Book Info</p>
              <p className="text-sm text-surface-400">Search for cover, author, synopsis</p>
            </div>
          </button>

          {/* Tips */}
          <button
            onClick={() => {
              setShowMenu(false);
              setShowTips(true);
            }}
            className="w-full flex items-center gap-4 p-4 rounded-xl bg-surface-800 hover:bg-surface-700 transition-colors"
          >
            <Lightbulb className="w-6 h-6 text-primary-500" />
            <div className="text-left">
              <p className="font-semibold text-white">Tips</p>
              <p className="text-sm text-surface-400">Learn how to use the player</p>
            </div>
          </button>
        </div>
      </Modal>

      {/* ===== ADD BOOKMARK MODAL ===== */}
      <Modal
        isOpen={showBookmarkModal}
        onClose={() => setShowBookmarkModal(false)}
        title="Add Bookmark"
        size="sm"
      >
        <div className="space-y-4">
          <div className="p-4 bg-surface-800 rounded-xl">
            <div className="flex items-center gap-2 mb-2">
              <Clock className="w-5 h-5 text-primary-500" />
              <span className="font-semibold text-white">{formatTime(currentTime)}</span>
            </div>
            <p className="text-sm text-surface-400">
              {currentChapter?.title || currentBook.title}
            </p>
          </div>
          <input
            type="text"
            placeholder="Add a note (optional)..."
            value={bookmarkNote}
            onChange={(e) => setBookmarkNote(e.target.value)}
            className="w-full bg-surface-800 px-4 py-3 rounded-xl text-white placeholder-surface-500"
          />
          <button
            onClick={handleAddBookmark}
            className="w-full py-3 bg-primary-500 text-white rounded-xl font-semibold hover:bg-primary-600 transition-colors"
          >
            Save Bookmark
          </button>
        </div>
      </Modal>

      {/* ===== TIPS MODAL ===== */}
      <Modal
        isOpen={showTips}
        onClose={() => setShowTips(false)}
        title="Player Tips"
        size="md"
      >
        <div className="space-y-4 text-sm">
          <div className="p-4 bg-surface-800 rounded-xl">
            <p className="font-semibold text-white mb-1">Swipe Down</p>
            <p className="text-surface-400">Swipe down on the player to minimize to mini player and return to library while continuing playback.</p>
          </div>
          <div className="p-4 bg-surface-800 rounded-xl">
            <p className="font-semibold text-white mb-1">Swipe on Cover</p>
            <p className="text-surface-400">Swipe left or right on the cover art to scrub through the audio.</p>
          </div>
          <div className="p-4 bg-surface-800 rounded-xl">
            <p className="font-semibold text-white mb-1">Sleep Timer</p>
            <p className="text-surface-400">Set a sleep timer to automatically pause playback after a set time.</p>
          </div>
          <div className="p-4 bg-surface-800 rounded-xl">
            <p className="font-semibold text-white mb-1">Bookmarks</p>
            <p className="text-surface-400">Add bookmarks with notes to easily return to important sections.</p>
          </div>
          <div className="p-4 bg-surface-800 rounded-xl">
            <p className="font-semibold text-white mb-1">Chapters</p>
            <p className="text-surface-400">Tap the chapters button to view and jump to any chapter in the book.</p>
          </div>
        </div>
      </Modal>

      {/* ===== SPEED PICKER MODAL ===== */}
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
              className={`py-3 rounded-xl text-sm font-bold transition-colors ${
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

      {/* ===== SLEEP TIMER MODAL ===== */}
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
              <span className="font-semibold">{option.label}</span>
              {sleepTimerMode === option.value && sleepTimerRemaining !== null && (
                <span className="text-sm opacity-80">{formatTime(sleepTimerRemaining)}</span>
              )}
            </button>
          ))}
        </div>
      </Modal>
    </motion.div>
  );
}
