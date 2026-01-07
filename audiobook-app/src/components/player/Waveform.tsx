import { motion } from 'framer-motion';
import { useMemo } from 'react';

interface WaveformProps {
  isPlaying: boolean;
  progress: number;
  barCount?: number;
  className?: string;
}

export function Waveform({ isPlaying, progress, barCount = 40, className = '' }: WaveformProps) {
  const bars = useMemo(() => {
    return Array.from({ length: barCount }, (_, i) => {
      const height = 20 + Math.sin(i * 0.5) * 15 + Math.random() * 30;
      return { height, delay: i * 0.05 };
    });
  }, [barCount]);

  const activeBarIndex = Math.floor(progress * barCount);

  return (
    <div className={`flex items-center justify-center gap-[3px] h-16 ${className}`}>
      {bars.map((bar, i) => (
        <motion.div
          key={i}
          className={`w-1 rounded-full ${
            i <= activeBarIndex
              ? 'bg-gradient-to-t from-primary-600 to-primary-400'
              : 'bg-surface-300 dark:bg-surface-600'
          }`}
          initial={{ height: 4 }}
          animate={{
            height: isPlaying ? bar.height : Math.max(8, bar.height * 0.3),
            opacity: i <= activeBarIndex ? 1 : 0.5,
          }}
          transition={{
            height: isPlaying
              ? {
                  duration: 0.4,
                  repeat: Infinity,
                  repeatType: 'reverse',
                  delay: bar.delay,
                  ease: 'easeInOut',
                }
              : { duration: 0.3 },
            opacity: { duration: 0.2 },
          }}
        />
      ))}
    </div>
  );
}
