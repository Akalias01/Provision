import { motion } from 'framer-motion';
import { BookOpen, Headphones, FileText, Play, MoreVertical, Trash2, Clock } from 'lucide-react';
import { useState } from 'react';
import type { Book } from '../../types';
import { formatDuration } from '../../utils/formatTime';

interface BookCardProps {
  book: Book;
  onClick: () => void;
  onDelete: () => void;
  index: number;
}

export function BookCard({ book, onClick, onDelete, index }: BookCardProps) {
  const [showMenu, setShowMenu] = useState(false);

  const formatIcon = {
    audio: Headphones,
    epub: BookOpen,
    pdf: FileText,
  };

  const FormatIcon = formatIcon[book.format];

  const progress = Math.round(book.currentPosition * 100);

  const formatBadgeColor = {
    audio: 'bg-emerald-500/20 text-emerald-400',
    epub: 'bg-primary-500/20 text-primary-400',
    pdf: 'bg-orange-500/20 text-orange-400',
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: index * 0.05 }}
      className="group relative"
    >
      <motion.div
        whileHover={{ y: -4 }}
        whileTap={{ scale: 0.98 }}
        onClick={onClick}
        className="card-hover p-0 overflow-hidden cursor-pointer"
      >
        {/* Cover */}
        <div className="relative aspect-[3/4] overflow-hidden">
          {book.cover ? (
            <img
              src={book.cover}
              alt={book.title}
              className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
            />
          ) : (
            <div className="w-full h-full bg-gradient-to-br from-primary-500 to-primary-700 flex items-center justify-center">
              <FormatIcon className="w-16 h-16 text-white/60" />
            </div>
          )}

          {/* Overlay gradient */}
          <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300" />

          {/* Play button on hover */}
          <motion.div
            initial={{ opacity: 0, scale: 0.8 }}
            whileHover={{ scale: 1.1 }}
            className="absolute inset-0 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity duration-300"
          >
            <div className="w-14 h-14 rounded-full bg-white/90 flex items-center justify-center shadow-lg">
              <Play className="w-6 h-6 text-surface-900 ml-1" fill="currentColor" />
            </div>
          </motion.div>

          {/* Format badge */}
          <div className={`absolute top-3 left-3 px-2 py-1 rounded-full text-xs font-medium ${formatBadgeColor[book.format]}`}>
            {book.format.toUpperCase()}
          </div>

          {/* Menu button */}
          <button
            onClick={(e) => {
              e.stopPropagation();
              setShowMenu(!showMenu);
            }}
            className="absolute top-3 right-3 p-1.5 rounded-full bg-black/30 text-white opacity-0 group-hover:opacity-100 transition-opacity hover:bg-black/50"
          >
            <MoreVertical className="w-4 h-4" />
          </button>

          {/* Dropdown menu */}
          {showMenu && (
            <motion.div
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              className="absolute top-12 right-3 bg-white dark:bg-surface-800 rounded-xl shadow-xl overflow-hidden z-10"
              onClick={(e) => e.stopPropagation()}
            >
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  onDelete();
                  setShowMenu(false);
                }}
                className="flex items-center gap-2 px-4 py-2.5 text-sm text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20 w-full"
              >
                <Trash2 className="w-4 h-4" />
                Remove
              </button>
            </motion.div>
          )}

          {/* Progress bar */}
          {progress > 0 && (
            <div className="absolute bottom-0 left-0 right-0 h-1 bg-black/30">
              <motion.div
                initial={{ width: 0 }}
                animate={{ width: `${progress}%` }}
                className="h-full bg-primary-500"
              />
            </div>
          )}
        </div>

        {/* Info */}
        <div className="p-4">
          <h3 className="font-semibold text-surface-900 dark:text-white truncate">
            {book.title}
          </h3>
          <p className="text-sm text-surface-500 dark:text-surface-400 truncate mt-0.5">
            {book.author}
          </p>

          <div className="flex items-center gap-3 mt-3 text-xs text-surface-400 dark:text-surface-500">
            {book.duration && (
              <span className="flex items-center gap-1">
                <Clock className="w-3.5 h-3.5" />
                {formatDuration(book.duration)}
              </span>
            )}
            {progress > 0 && (
              <span className="flex items-center gap-1">
                {progress}% complete
              </span>
            )}
          </div>
        </div>
      </motion.div>
    </motion.div>
  );
}
