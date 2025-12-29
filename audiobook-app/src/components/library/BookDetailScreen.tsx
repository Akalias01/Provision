import { motion } from 'framer-motion';
import {
  ChevronLeft,
  Share2,
  MoreVertical,
  Headphones,
  CheckCircle,
  Clock,
} from 'lucide-react';
import type { Book } from '../../types';

interface BookDetailScreenProps {
  book: Book;
  onBack: () => void;
  onPlay: () => void;
  onMenuClick: () => void;
}

export function BookDetailScreen({ book, onBack, onPlay, onMenuClick }: BookDetailScreenProps) {

  // Calculate progress percentage
  const progress = Math.round(book.currentPosition * 100);

  // Format duration
  const formatDuration = (seconds?: number) => {
    if (!seconds) return 'Unknown';
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    if (hours > 0) {
      return `${hours} h ${minutes} min`;
    }
    return `${minutes} min`;
  };

  // Get year
  const year = book.dateAdded ? new Date(book.dateAdded).getFullYear() : '';

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="h-full flex flex-col bg-surface-950 overflow-hidden"
    >
      {/* Cover area with overlay controls */}
      <div className="relative">
        {/* Large cover image */}
        <div className="aspect-square w-full max-h-[50vh]">
          {book.cover ? (
            <img
              src={book.cover}
              alt={book.title}
              className="w-full h-full object-cover"
            />
          ) : (
            <div className="w-full h-full flex items-center justify-center bg-gradient-to-br from-primary-500/30 to-primary-700/30">
              <Headphones className="w-24 h-24 text-primary-400/50" />
            </div>
          )}
        </div>

        {/* Overlay controls */}
        <div className="absolute top-0 left-0 right-0 flex items-center justify-between p-4 bg-gradient-to-b from-black/60 to-transparent safe-area-top">
          <button
            onClick={onBack}
            className="p-2 text-white/90 hover:text-white"
          >
            <ChevronLeft className="w-6 h-6" />
          </button>
          <div className="flex items-center gap-2">
            <button className="p-2 text-white/90 hover:text-white">
              <Share2 className="w-5 h-5" />
            </button>
            <button
              onClick={onMenuClick}
              className="p-2 text-white/90 hover:text-white"
            >
              <MoreVertical className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Play FAB */}
        <button
          onClick={onPlay}
          className="absolute bottom-4 right-4 w-14 h-14 rounded-full bg-primary-500 flex items-center justify-center shadow-lg shadow-primary-500/30 hover:bg-primary-600 transition-colors"
        >
          <Headphones className="w-7 h-7 text-white" />
        </button>
      </div>

      {/* Content area - scrollable */}
      <div className="flex-1 overflow-auto px-4 py-4 space-y-4">
        {/* Stats row */}
        <div className="flex items-center gap-2 text-sm text-surface-400 flex-wrap">
          {progress > 0 && (
            <>
              <CheckCircle className="w-4 h-4" />
              <span>{progress}%</span>
              <span className="text-surface-600">•</span>
            </>
          )}
          {year && (
            <>
              <span>{year}</span>
              <span className="text-surface-600">•</span>
            </>
          )}
          <Clock className="w-4 h-4" />
          <span>{formatDuration(book.duration)}</span>
        </div>

        {/* Title */}
        <h1 className="text-2xl font-bold text-white">
          {book.title}
        </h1>

        {/* Author */}
        <p className="text-primary-400 font-medium">
          {book.author}
        </p>

        {/* Narrator */}
        {book.narrator && (
          <p className="text-surface-400 text-sm">
            {book.narrator}
          </p>
        )}

        {/* Description */}
        {book.description && (
          <p className="text-surface-300 text-sm leading-relaxed">
            {book.description}
          </p>
        )}

        {/* Chapters count */}
        {book.chapters && book.chapters.length > 0 && (
          <p className="text-surface-500 text-sm">
            {book.chapters.length} chapters
          </p>
        )}
      </div>
    </motion.div>
  );
}
