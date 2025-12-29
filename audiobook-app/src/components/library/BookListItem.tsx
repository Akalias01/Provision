import { motion } from 'framer-motion';
import { MoreVertical, Headphones, BookOpen, FileText } from 'lucide-react';
import type { Book } from '../../types';

interface BookListItemProps {
  book: Book;
  onSelect: (book: Book) => void;
  onMenuClick: (book: Book, e: React.MouseEvent) => void;
}

export function BookListItem({ book, onSelect, onMenuClick }: BookListItemProps) {
  const progress = book.currentPosition * 100;

  // Format duration
  const formatDuration = (seconds?: number) => {
    if (!seconds) return '';
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    if (hours > 0) {
      return `${hours} h ${minutes} min`;
    }
    return `${minutes} min`;
  };

  // Get year from dateAdded or metadata
  const year = book.dateAdded ? new Date(book.dateAdded).getFullYear() : '';

  // Get icon based on format
  const FormatIcon = book.format === 'audio' ? Headphones
    : book.format === 'epub' ? BookOpen
    : FileText;

  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      className="flex items-center gap-3 p-3 rounded-xl hover:bg-surface-800/50 transition-colors cursor-pointer"
      onClick={() => onSelect(book)}
    >
      {/* Cover with progress bar */}
      <div className="relative w-20 h-28 flex-shrink-0 rounded-lg overflow-hidden bg-surface-800">
        {book.cover ? (
          <img
            src={book.cover}
            alt={book.title}
            className="w-full h-full object-cover"
          />
        ) : (
          <div className="w-full h-full flex items-center justify-center bg-gradient-to-br from-primary-500/30 to-primary-700/30">
            <FormatIcon className="w-8 h-8 text-primary-400" />
          </div>
        )}

        {/* Progress bar at bottom */}
        {progress > 0 && (
          <div className="absolute bottom-0 left-0 right-0 h-1 bg-black/50">
            <div
              className="h-full bg-primary-500"
              style={{ width: `${progress}%` }}
            />
          </div>
        )}
      </div>

      {/* Book info */}
      <div className="flex-1 min-w-0 py-1">
        <h3 className="font-medium text-white text-sm leading-tight line-clamp-2">
          {book.title}
        </h3>
        <p className="text-surface-400 text-xs mt-1 truncate">
          {book.author}
        </p>
        <p className="text-surface-500 text-xs mt-1">
          {year && `${year} • `}
          {formatDuration(book.duration)}
        </p>
      </div>

      {/* Actions */}
      <div className="flex flex-col items-center gap-2 flex-shrink-0">
        <button
          onClick={(e) => onMenuClick(book, e)}
          className="p-2 text-surface-400 hover:text-white transition-colors"
        >
          <MoreVertical className="w-5 h-5" />
        </button>
        <FormatIcon className="w-5 h-5 text-surface-500" />
      </div>
    </motion.div>
  );
}
