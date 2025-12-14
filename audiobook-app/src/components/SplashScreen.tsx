import { motion } from 'framer-motion';
import { useState, useEffect } from 'react';
import { useStore, type SplashVariant } from '../store/useStore';

interface SplashScreenProps {
  onComplete: () => void;
}

const TAGLINE = "Audiobooks Reimagined. Resonate With Every Word.";

// Animated tagline component - letters appear from left to right
function AnimatedTagline({ show, className, style }: { show: boolean; className?: string; style?: React.CSSProperties }) {
  const letters = TAGLINE.split('');

  return (
    <motion.p
      className={`text-center ${className}`}
      style={style}
      initial={{ opacity: 0 }}
      animate={show ? { opacity: 1 } : {}}
    >
      {letters.map((letter, index) => (
        <motion.span
          key={index}
          initial={{ opacity: 0, y: 10 }}
          animate={show ? { opacity: 1, y: 0 } : {}}
          transition={{
            duration: 0.03,
            delay: index * 0.02,
            ease: "easeOut"
          }}
        >
          {letter}
        </motion.span>
      ))}
    </motion.p>
  );
}

// ============================================
// OPTION 1: Minimal Elegance - Clean, sophisticated
// ============================================
function MinimalEleganceSplash({ onComplete }: SplashScreenProps) {
  const [phase, setPhase] = useState(0);

  useEffect(() => {
    const timer = setTimeout(() => {
      if (phase < 3) setPhase(phase + 1);
      else onComplete();
    }, phase === 0 ? 300 : phase === 3 ? 1200 : 800);
    return () => clearTimeout(timer);
  }, [phase, onComplete]);

  return (
    <motion.div
      initial={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      transition={{ duration: 0.5 }}
      className="fixed inset-0 z-[100] bg-[#0c0c0c] flex items-center justify-center overflow-hidden"
    >
      {/* Subtle gradient background */}
      <div className="absolute inset-0">
        <div className="absolute inset-0 bg-gradient-to-br from-[#1a1a2e]/50 via-transparent to-[#16213e]/30" />
      </div>

      {/* Animated lines */}
      <motion.div
        className="absolute left-0 right-0 h-px top-1/3"
        style={{ background: 'linear-gradient(90deg, transparent, rgba(255,255,255,0.1), transparent)' }}
        initial={{ scaleX: 0 }}
        animate={phase >= 1 ? { scaleX: 1 } : {}}
        transition={{ duration: 1, ease: "easeOut" }}
      />
      <motion.div
        className="absolute left-0 right-0 h-px bottom-1/3"
        style={{ background: 'linear-gradient(90deg, transparent, rgba(255,255,255,0.1), transparent)' }}
        initial={{ scaleX: 0 }}
        animate={phase >= 1 ? { scaleX: 1 } : {}}
        transition={{ duration: 1, ease: "easeOut", delay: 0.2 }}
      />

      {/* Main content */}
      <div className="relative z-10 flex flex-col items-center">
        {/* Logo */}
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8, ease: "easeOut" }}
        >
          <h1 className="text-8xl font-extralight tracking-[0.3em] text-white/90 rezon-font-orbitron">
            REZON
          </h1>
        </motion.div>

        {/* Subtle underline */}
        <motion.div
          className="h-px w-32 mt-6 bg-gradient-to-r from-transparent via-white/40 to-transparent"
          initial={{ scaleX: 0, opacity: 0 }}
          animate={phase >= 1 ? { scaleX: 1, opacity: 1 } : {}}
          transition={{ duration: 0.8, delay: 0.3 }}
        />

        {/* Animated Tagline */}
        <div className="mt-8">
          <AnimatedTagline
            show={phase >= 2}
            className="text-sm tracking-[0.2em] uppercase text-white/40 font-light"
          />
        </div>
      </div>

      {/* Corner accents */}
      <motion.div
        className="absolute top-8 left-8 w-16 h-16 border-l border-t border-white/10"
        initial={{ opacity: 0 }}
        animate={phase >= 1 ? { opacity: 1 } : {}}
        transition={{ delay: 0.5 }}
      />
      <motion.div
        className="absolute bottom-8 right-8 w-16 h-16 border-r border-b border-white/10"
        initial={{ opacity: 0 }}
        animate={phase >= 1 ? { opacity: 1 } : {}}
        transition={{ delay: 0.5 }}
      />
    </motion.div>
  );
}

// ============================================
// OPTION 2: Gradient Flow - Smooth color transitions
// ============================================
function GradientFlowSplash({ onComplete }: SplashScreenProps) {
  const [phase, setPhase] = useState(0);

  useEffect(() => {
    const timer = setTimeout(() => {
      if (phase < 3) setPhase(phase + 1);
      else onComplete();
    }, phase === 0 ? 400 : phase === 3 ? 1200 : 900);
    return () => clearTimeout(timer);
  }, [phase, onComplete]);

  return (
    <motion.div
      initial={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      transition={{ duration: 0.5 }}
      className="fixed inset-0 z-[100] bg-[#050505] flex items-center justify-center overflow-hidden"
    >
      {/* Animated gradient blobs */}
      <motion.div
        className="absolute w-[800px] h-[800px] rounded-full opacity-30"
        style={{
          background: 'radial-gradient(circle, #6366f1 0%, transparent 70%)',
          filter: 'blur(80px)',
        }}
        animate={{
          x: [-200, 200, -200],
          y: [-100, 100, -100],
        }}
        transition={{ duration: 15, repeat: Infinity, ease: "linear" }}
      />
      <motion.div
        className="absolute w-[600px] h-[600px] rounded-full opacity-30"
        style={{
          background: 'radial-gradient(circle, #ec4899 0%, transparent 70%)',
          filter: 'blur(80px)',
        }}
        animate={{
          x: [200, -200, 200],
          y: [100, -100, 100],
        }}
        transition={{ duration: 12, repeat: Infinity, ease: "linear" }}
      />
      <motion.div
        className="absolute w-[500px] h-[500px] rounded-full opacity-20"
        style={{
          background: 'radial-gradient(circle, #06b6d4 0%, transparent 70%)',
          filter: 'blur(60px)',
        }}
        animate={{
          x: [0, 150, -150, 0],
          y: [150, -100, 100, 150],
        }}
        transition={{ duration: 10, repeat: Infinity, ease: "linear" }}
      />

      {/* Content */}
      <div className="relative z-10 flex flex-col items-center">
        <motion.div
          initial={{ scale: 0.8, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          transition={{ type: "spring", duration: 1 }}
        >
          <h1 className="text-9xl font-black rezon-font-orbitron">
            <span
              className="bg-clip-text text-transparent"
              style={{
                backgroundImage: 'linear-gradient(135deg, #6366f1 0%, #ec4899 50%, #06b6d4 100%)',
              }}
            >
              REZON
            </span>
          </h1>
        </motion.div>

        {/* Animated bar */}
        <motion.div
          className="h-1 w-48 mt-6 rounded-full overflow-hidden bg-white/5"
          initial={{ opacity: 0 }}
          animate={phase >= 1 ? { opacity: 1 } : {}}
        >
          <motion.div
            className="h-full rounded-full"
            style={{
              background: 'linear-gradient(90deg, #6366f1, #ec4899, #06b6d4)',
            }}
            initial={{ x: '-100%' }}
            animate={phase >= 1 ? { x: '0%' } : {}}
            transition={{ duration: 0.8, ease: "easeOut" }}
          />
        </motion.div>

        {/* Animated Tagline */}
        <div className="mt-8">
          <AnimatedTagline
            show={phase >= 2}
            className="text-lg tracking-[0.1em] rezon-font-orbitron bg-clip-text text-transparent"
            style={{
              backgroundImage: 'linear-gradient(90deg, #6366f1, #ec4899)',
            }}
          />
        </div>
      </div>
    </motion.div>
  );
}

// ============================================
// OPTION 3: Sound Wave - Audio-inspired design
// ============================================
function SoundWaveSplash({ onComplete }: SplashScreenProps) {
  const [phase, setPhase] = useState(0);

  useEffect(() => {
    const timer = setTimeout(() => {
      if (phase < 3) setPhase(phase + 1);
      else onComplete();
    }, phase === 0 ? 200 : phase === 3 ? 1200 : 800);
    return () => clearTimeout(timer);
  }, [phase, onComplete]);

  return (
    <motion.div
      initial={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      transition={{ duration: 0.5 }}
      className="fixed inset-0 z-[100] bg-gradient-to-b from-[#0f172a] to-[#020617] flex items-center justify-center overflow-hidden"
    >
      {/* Sound wave bars */}
      <div className="absolute inset-0 flex items-center justify-center gap-1 opacity-20">
        {[...Array(50)].map((_, i) => (
          <motion.div
            key={i}
            className="w-1 bg-gradient-to-t from-cyan-500 to-violet-500 rounded-full"
            initial={{ height: 20 }}
            animate={phase >= 1 ? {
              height: [20, 40 + Math.sin(i * 0.3) * 60, 20],
            } : {}}
            transition={{
              duration: 1.5,
              repeat: Infinity,
              delay: i * 0.02,
              ease: "easeInOut",
            }}
          />
        ))}
      </div>

      {/* Circular pulse */}
      {phase >= 1 && (
        <motion.div
          className="absolute w-64 h-64 rounded-full border border-cyan-500/30"
          initial={{ scale: 0.5, opacity: 0 }}
          animate={{ scale: [0.5, 1.5, 0.5], opacity: [0.5, 0, 0.5] }}
          transition={{ duration: 3, repeat: Infinity }}
        />
      )}

      {/* Content */}
      <div className="relative z-10 flex flex-col items-center">
        <motion.div
          initial={{ scale: 0, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          transition={{ type: "spring", duration: 0.8 }}
        >
          <h1
            className="text-8xl font-bold rezon-font-orbitron"
            style={{
              background: 'linear-gradient(135deg, #06b6d4, #8b5cf6)',
              WebkitBackgroundClip: 'text',
              WebkitTextFillColor: 'transparent',
              filter: 'drop-shadow(0 0 40px rgba(6,182,212,0.3))',
            }}
          >
            REZON
          </h1>
        </motion.div>

        {/* Waveform line */}
        <motion.svg
          width="200"
          height="40"
          viewBox="0 0 200 40"
          className="mt-4"
          initial={{ opacity: 0 }}
          animate={phase >= 1 ? { opacity: 1 } : {}}
        >
          <motion.path
            d="M0,20 Q25,5 50,20 T100,20 T150,20 T200,20"
            fill="none"
            stroke="url(#waveGradient)"
            strokeWidth="2"
            initial={{ pathLength: 0 }}
            animate={phase >= 1 ? { pathLength: 1 } : {}}
            transition={{ duration: 1 }}
          />
          <defs>
            <linearGradient id="waveGradient" x1="0%" y1="0%" x2="100%" y2="0%">
              <stop offset="0%" stopColor="#06b6d4" />
              <stop offset="100%" stopColor="#8b5cf6" />
            </linearGradient>
          </defs>
        </motion.svg>

        {/* Animated Tagline */}
        <div className="mt-6">
          <AnimatedTagline
            show={phase >= 2}
            className="text-lg tracking-[0.1em] text-cyan-400/80 rezon-font-orbitron"
          />
        </div>
      </div>
    </motion.div>
  );
}

// ============================================
// OPTION 4: Luxe Dark - Premium dark aesthetic
// ============================================
function LuxeDarkSplash({ onComplete }: SplashScreenProps) {
  const [phase, setPhase] = useState(0);

  useEffect(() => {
    const timer = setTimeout(() => {
      if (phase < 3) setPhase(phase + 1);
      else onComplete();
    }, phase === 0 ? 300 : phase === 3 ? 1200 : 900);
    return () => clearTimeout(timer);
  }, [phase, onComplete]);

  return (
    <motion.div
      initial={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      transition={{ duration: 0.5 }}
      className="fixed inset-0 z-[100] bg-black flex items-center justify-center overflow-hidden"
    >
      {/* Gold accent lighting */}
      <motion.div
        className="absolute w-full h-1/2 top-0"
        style={{
          background: 'radial-gradient(ellipse 50% 100% at 50% 0%, rgba(251,191,36,0.08) 0%, transparent 70%)',
        }}
        animate={{ opacity: [0.5, 0.8, 0.5] }}
        transition={{ duration: 4, repeat: Infinity }}
      />

      {/* Subtle grid */}
      <div
        className="absolute inset-0 opacity-[0.02]"
        style={{
          backgroundImage: 'linear-gradient(rgba(251,191,36,0.5) 1px, transparent 1px), linear-gradient(90deg, rgba(251,191,36,0.5) 1px, transparent 1px)',
          backgroundSize: '100px 100px',
        }}
      />

      {/* Content */}
      <div className="relative z-10 flex flex-col items-center">
        {/* Logo with gold accent */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8 }}
          className="relative"
        >
          <h1 className="text-8xl font-bold rezon-font-orbitron text-white">
            REZON
          </h1>
          {/* Gold shimmer effect */}
          <motion.div
            className="absolute inset-0 text-8xl font-bold rezon-font-orbitron bg-clip-text text-transparent"
            style={{
              backgroundImage: 'linear-gradient(90deg, transparent, rgba(251,191,36,0.4), transparent)',
              backgroundSize: '200% 100%',
            }}
            animate={{ backgroundPosition: ['200% 0', '-200% 0'] }}
            transition={{ duration: 3, repeat: Infinity, ease: "linear" }}
          >
            REZON
          </motion.div>
        </motion.div>

        {/* Gold line */}
        <motion.div
          className="h-px w-40 mt-6"
          style={{ background: 'linear-gradient(90deg, transparent, #fbbf24, transparent)' }}
          initial={{ scaleX: 0, opacity: 0 }}
          animate={phase >= 1 ? { scaleX: 1, opacity: 1 } : {}}
          transition={{ duration: 0.8 }}
        />

        {/* Animated Tagline */}
        <div className="mt-8">
          <AnimatedTagline
            show={phase >= 2}
            className="text-sm tracking-[0.2em] uppercase text-amber-500/60 font-light"
          />
        </div>

        {/* Decorative dots */}
        <motion.div
          className="flex gap-2 mt-6"
          initial={{ opacity: 0 }}
          animate={phase >= 2 ? { opacity: 1 } : {}}
        >
          {[0, 1, 2].map((i) => (
            <motion.div
              key={i}
              className="w-1 h-1 rounded-full bg-amber-500/40"
              animate={{ opacity: [0.4, 1, 0.4] }}
              transition={{ duration: 1.5, repeat: Infinity, delay: i * 0.2 }}
            />
          ))}
        </motion.div>
      </div>
    </motion.div>
  );
}

// Map variant names to components
const splashComponents: Record<SplashVariant, React.ComponentType<SplashScreenProps>> = {
  pulseWave: MinimalEleganceSplash,
  glitchCyber: GradientFlowSplash,
  waveformMorph: SoundWaveSplash,
  neonFlicker: LuxeDarkSplash,
};

// ============================================
// MAIN EXPORT - Uses store to determine variant
// ============================================
export function SplashScreen({ onComplete }: SplashScreenProps) {
  const splashVariant = useStore((state) => state.splashVariant);
  const SplashComponent = splashComponents[splashVariant] || GradientFlowSplash;

  return <SplashComponent onComplete={onComplete} />;
}
