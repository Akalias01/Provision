import { motion } from 'framer-motion';
import { useRef, useState, useCallback } from 'react';

interface SliderProps {
  value: number;
  min?: number;
  max?: number;
  step?: number;
  onChange: (value: number) => void;
  className?: string;
  showTooltip?: boolean;
  formatTooltip?: (value: number) => string;
  trackClassName?: string;
  fillClassName?: string;
}

export function Slider({
  value,
  min = 0,
  max = 100,
  step = 1,
  onChange,
  className = '',
  showTooltip = false,
  formatTooltip,
  trackClassName = '',
  fillClassName = '',
}: SliderProps) {
  const trackRef = useRef<HTMLDivElement>(null);
  const [isDragging, setIsDragging] = useState(false);
  const [hoverValue, setHoverValue] = useState<number | null>(null);

  const percentage = ((value - min) / (max - min)) * 100;

  const calculateValue = useCallback(
    (clientX: number) => {
      if (!trackRef.current) return value;
      const rect = trackRef.current.getBoundingClientRect();
      const x = clientX - rect.left;
      const percent = Math.max(0, Math.min(1, x / rect.width));
      const rawValue = min + percent * (max - min);
      return Math.round(rawValue / step) * step;
    },
    [min, max, step, value]
  );

  const handleMouseDown = (e: React.MouseEvent) => {
    setIsDragging(true);
    onChange(calculateValue(e.clientX));

    const handleMouseMove = (e: MouseEvent) => {
      onChange(calculateValue(e.clientX));
    };

    const handleMouseUp = () => {
      setIsDragging(false);
      document.removeEventListener('mousemove', handleMouseMove);
      document.removeEventListener('mouseup', handleMouseUp);
    };

    document.addEventListener('mousemove', handleMouseMove);
    document.addEventListener('mouseup', handleMouseUp);
  };

  const handleMouseMove = (e: React.MouseEvent) => {
    if (showTooltip) {
      setHoverValue(calculateValue(e.clientX));
    }
  };

  return (
    <div className={`relative ${className}`}>
      <div
        ref={trackRef}
        className={`relative h-2 rounded-full bg-surface-200 dark:bg-surface-700 cursor-pointer ${trackClassName}`}
        onMouseDown={handleMouseDown}
        onMouseMove={handleMouseMove}
        onMouseLeave={() => setHoverValue(null)}
      >
        {/* Fill */}
        <motion.div
          className={`absolute left-0 top-0 h-full rounded-full bg-gradient-to-r from-primary-500 to-primary-400 ${fillClassName}`}
          style={{ width: `${percentage}%` }}
          layoutId="slider-fill"
        />

        {/* Thumb */}
        <motion.div
          className="absolute top-1/2 -translate-y-1/2 w-4 h-4 rounded-full bg-white shadow-lg shadow-black/20 border-2 border-primary-500"
          style={{ left: `calc(${percentage}% - 8px)` }}
          animate={{
            scale: isDragging ? 1.2 : 1,
          }}
          transition={{ type: 'spring', stiffness: 300, damping: 30 }}
        />

        {/* Tooltip */}
        {showTooltip && (isDragging || hoverValue !== null) && (
          <motion.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 10 }}
            className="absolute -top-8 px-2 py-1 bg-surface-900 dark:bg-surface-100 text-white dark:text-surface-900 text-xs rounded-md"
            style={{
              left: `${((hoverValue ?? value) - min) / (max - min) * 100}%`,
              transform: 'translateX(-50%)',
            }}
          >
            {formatTooltip ? formatTooltip(hoverValue ?? value) : (hoverValue ?? value)}
          </motion.div>
        )}
      </div>
    </div>
  );
}
