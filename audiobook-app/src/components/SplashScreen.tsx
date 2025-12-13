import { motion, AnimatePresence } from 'framer-motion';
import { useState, useEffect } from 'react';

interface SplashScreenProps {
  onComplete: () => void;
}

export function SplashScreen({ onComplete }: SplashScreenProps) {
  const [phase, setPhase] = useState<'logo' | 'waves' | 'brain' | 'done'>('logo');

  useEffect(() => {
    const timers = [
      setTimeout(() => setPhase('waves'), 800),
      setTimeout(() => setPhase('brain'), 1800),
      setTimeout(() => setPhase('done'), 2800),
      setTimeout(() => onComplete(), 3200),
    ];
    return () => timers.forEach(clearTimeout);
  }, [onComplete]);

  return (
    <AnimatePresence>
      {phase !== 'done' && (
        <motion.div
          initial={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          transition={{ duration: 0.4 }}
          className="fixed inset-0 z-[100] bg-gradient-to-br from-surface-950 via-surface-900 to-primary-950 flex items-center justify-center"
        >
          <div className="relative flex flex-col items-center">
            {/* VOCA Text */}
            <motion.div
              initial={{ opacity: 0, scale: 0.8 }}
              animate={{
                opacity: phase === 'logo' ? 1 : 0,
                scale: phase === 'logo' ? 1 : 1.2,
              }}
              transition={{ duration: 0.5, ease: 'easeOut' }}
              className="absolute"
            >
              <h1 className="text-7xl font-black tracking-tight">
                <span className="text-gradient">VOCA</span>
              </h1>
            </motion.div>

            {/* Sound Waves */}
            <motion.div
              initial={{ opacity: 0 }}
              animate={{
                opacity: phase === 'waves' ? 1 : 0,
              }}
              transition={{ duration: 0.3 }}
              className="absolute flex items-center gap-1"
            >
              {[...Array(12)].map((_, i) => (
                <motion.div
                  key={i}
                  className="w-2 bg-gradient-to-t from-primary-600 to-primary-400 rounded-full"
                  initial={{ height: 8 }}
                  animate={phase === 'waves' ? {
                    height: [8, 20 + Math.sin(i * 0.8) * 40, 8],
                  } : { height: 8 }}
                  transition={{
                    duration: 0.6,
                    repeat: phase === 'waves' ? 2 : 0,
                    delay: i * 0.05,
                    ease: 'easeInOut',
                  }}
                />
              ))}
            </motion.div>

            {/* Brain */}
            <motion.div
              initial={{ opacity: 0, scale: 0.5 }}
              animate={{
                opacity: phase === 'brain' ? 1 : 0,
                scale: phase === 'brain' ? 1 : 0.5,
              }}
              transition={{ duration: 0.4, ease: 'easeOut' }}
              className="absolute"
            >
              <svg
                width="120"
                height="120"
                viewBox="0 0 100 100"
                className="drop-shadow-2xl"
              >
                {/* Brain shape */}
                <motion.path
                  d="M50 15 C30 15 20 30 20 45 C20 55 25 62 30 67 C25 72 22 80 25 85 C28 90 35 90 40 88 C42 92 46 95 50 95 C54 95 58 92 60 88 C65 90 72 90 75 85 C78 80 75 72 70 67 C75 62 80 55 80 45 C80 30 70 15 50 15"
                  fill="none"
                  stroke="url(#brainGradient)"
                  strokeWidth="3"
                  initial={{ pathLength: 0 }}
                  animate={{ pathLength: 1 }}
                  transition={{ duration: 0.8, ease: 'easeInOut' }}
                />

                {/* Brain details */}
                <motion.path
                  d="M35 35 Q40 30 50 35 Q60 30 65 35 M30 50 Q40 45 50 50 Q60 45 70 50 M35 65 Q45 60 55 65"
                  fill="none"
                  stroke="url(#brainGradient)"
                  strokeWidth="2"
                  strokeLinecap="round"
                  initial={{ pathLength: 0, opacity: 0 }}
                  animate={{ pathLength: 1, opacity: 1 }}
                  transition={{ duration: 0.6, delay: 0.4 }}
                />

                {/* Glow effect */}
                <motion.circle
                  cx="50"
                  cy="50"
                  r="45"
                  fill="url(#glowGradient)"
                  initial={{ opacity: 0 }}
                  animate={{ opacity: [0, 0.6, 0.3] }}
                  transition={{ duration: 0.8, delay: 0.3 }}
                />

                {/* Sparkles */}
                {[
                  { cx: 35, cy: 30, delay: 0.5 },
                  { cx: 65, cy: 35, delay: 0.6 },
                  { cx: 45, cy: 55, delay: 0.7 },
                  { cx: 60, cy: 70, delay: 0.8 },
                ].map((spark, i) => (
                  <motion.circle
                    key={i}
                    cx={spark.cx}
                    cy={spark.cy}
                    r="3"
                    fill="#818cf8"
                    initial={{ opacity: 0, scale: 0 }}
                    animate={{ opacity: [0, 1, 0], scale: [0, 1.5, 0] }}
                    transition={{ duration: 0.5, delay: spark.delay }}
                  />
                ))}

                <defs>
                  <linearGradient id="brainGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                    <stop offset="0%" stopColor="#818cf8" />
                    <stop offset="50%" stopColor="#6366f1" />
                    <stop offset="100%" stopColor="#4f46e5" />
                  </linearGradient>
                  <radialGradient id="glowGradient" cx="50%" cy="50%" r="50%">
                    <stop offset="0%" stopColor="#6366f1" stopOpacity="0.4" />
                    <stop offset="100%" stopColor="#6366f1" stopOpacity="0" />
                  </radialGradient>
                </defs>
              </svg>
            </motion.div>

            {/* Subtitle */}
            <motion.p
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: phase === 'brain' ? 1 : 0, y: phase === 'brain' ? 60 : 20 }}
              transition={{ duration: 0.4, delay: 0.3 }}
              className="absolute top-24 text-surface-400 text-sm tracking-widest uppercase"
            >
              Voice to Mind
            </motion.p>
          </div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
