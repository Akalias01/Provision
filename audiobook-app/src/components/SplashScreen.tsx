import { motion } from 'framer-motion';
import { useState, useEffect } from 'react';

interface SplashScreenProps {
  onComplete: () => void;
}

const TAGLINE = "Audiobooks Reimagined. Resonate With Every Word.";

// Simple glitch effect on logo only
function GlitchLogo({ isGlitching }: { isGlitching: boolean }) {
  return (
    <div className="relative">
      {/* Main text */}
      <motion.h1
        className="text-5xl sm:text-7xl md:text-8xl font-black tracking-wider text-white"
        style={{ fontFamily: "'Orbitron', sans-serif" }}
        animate={isGlitching ? {
          x: [0, -4, 6, -3, 2, -5, 0],
          skewX: [0, 2, -3, 1, -2, 3, 0],
        } : {}}
        transition={{ duration: 0.15 }}
      >
        REZON
      </motion.h1>

      {/* Red channel offset */}
      <motion.h1
        className="absolute inset-0 text-5xl sm:text-7xl md:text-8xl font-black tracking-wider"
        style={{
          fontFamily: "'Orbitron', sans-serif",
          color: '#ff0040',
          mixBlendMode: 'screen',
        }}
        animate={isGlitching ? {
          x: [0, -8, 6, -4, 8, 0],
          opacity: [0.8, 1, 0.6, 1, 0.7, 0.8],
        } : {
          x: [0, -2, 2, -1, 0],
          opacity: [0.4, 0.3, 0.5, 0.4],
        }}
        transition={{
          duration: isGlitching ? 0.12 : 2,
          repeat: Infinity,
        }}
      >
        REZON
      </motion.h1>

      {/* Cyan channel offset */}
      <motion.h1
        className="absolute inset-0 text-5xl sm:text-7xl md:text-8xl font-black tracking-wider"
        style={{
          fontFamily: "'Orbitron', sans-serif",
          color: '#00ffff',
          mixBlendMode: 'screen',
        }}
        animate={isGlitching ? {
          x: [0, 8, -6, 4, -8, 0],
          opacity: [0.8, 0.6, 1, 0.7, 1, 0.8],
        } : {
          x: [0, 2, -2, 1, 0],
          opacity: [0.4, 0.5, 0.3, 0.4],
        }}
        transition={{
          duration: isGlitching ? 0.12 : 2,
          repeat: Infinity,
          delay: 0.02
        }}
      >
        REZON
      </motion.h1>

      {/* Glitch slice effect during glitch */}
      {isGlitching && (
        <motion.div
          className="absolute inset-0 overflow-hidden"
          style={{ clipPath: 'inset(30% 0 40% 0)' }}
          animate={{ x: [0, 15, -10, 0] }}
          transition={{ duration: 0.1 }}
        >
          <h1
            className="text-5xl sm:text-7xl md:text-8xl font-black tracking-wider text-cyan-400"
            style={{ fontFamily: "'Orbitron', sans-serif" }}
          >
            REZON
          </h1>
        </motion.div>
      )}
    </div>
  );
}

export function SplashScreen({ onComplete }: SplashScreenProps) {
  const [progress, setProgress] = useState(0);
  const [showTagline, setShowTagline] = useState(false);
  const [isGlitching, setIsGlitching] = useState(false);

  // Progress animation
  useEffect(() => {
    const duration = 2500; // Total splash duration
    const interval = 50;
    const increment = 100 / (duration / interval);

    const timer = setInterval(() => {
      setProgress(prev => {
        const next = prev + increment;
        if (next >= 100) {
          clearInterval(timer);
          setTimeout(onComplete, 300);
          return 100;
        }
        return next;
      });
    }, interval);

    // Show tagline after 800ms
    const taglineTimer = setTimeout(() => setShowTagline(true), 800);

    return () => {
      clearInterval(timer);
      clearTimeout(taglineTimer);
    };
  }, [onComplete]);

  // Random glitch bursts
  useEffect(() => {
    const glitchInterval = setInterval(() => {
      setIsGlitching(true);
      const duration = 80 + Math.random() * 150;
      setTimeout(() => setIsGlitching(false), duration);
    }, 400 + Math.random() * 800);

    return () => clearInterval(glitchInterval);
  }, []);

  return (
    <motion.div
      initial={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      transition={{ duration: 0.4 }}
      className="fixed inset-0 z-[100] bg-[#0a0a0f] flex flex-col items-center justify-center overflow-hidden"
    >
      {/* Subtle background glow */}
      <div
        className="absolute w-96 h-96 rounded-full opacity-20"
        style={{
          background: 'radial-gradient(circle, rgba(6,182,212,0.4) 0%, transparent 70%)',
          filter: 'blur(80px)',
        }}
      />

      {/* Main content */}
      <div className="relative z-10 flex flex-col items-center px-4">
        {/* Logo with glitch */}
        <GlitchLogo isGlitching={isGlitching} />

        {/* Loading bar */}
        <div className="w-48 sm:w-64 h-1 mt-8 rounded-full overflow-hidden bg-white/10">
          <motion.div
            className="h-full rounded-full"
            style={{
              background: 'linear-gradient(90deg, #06b6d4, #8b5cf6, #ec4899)',
              width: `${progress}%`,
            }}
            transition={{ duration: 0.1 }}
          />
        </div>

        {/* Tagline */}
        <motion.p
          className="mt-6 text-xs sm:text-sm text-center text-white/50 tracking-wider max-w-xs"
          initial={{ opacity: 0, y: 10 }}
          animate={showTagline ? { opacity: 1, y: 0 } : {}}
          transition={{ duration: 0.5 }}
        >
          {TAGLINE}
        </motion.p>
      </div>
    </motion.div>
  );
}
