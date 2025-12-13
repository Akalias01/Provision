import { motion } from 'framer-motion';
import { useState, useEffect } from 'react';
import { useStore, type SplashVariant } from '../store/useStore';

interface SplashScreenProps {
  onComplete: () => void;
}

// ============================================
// OPTION 1: Aurora Waves - Premium gradient waves
// ============================================
function AuroraWavesSplash({ onComplete }: SplashScreenProps) {
  const [phase, setPhase] = useState(0);

  useEffect(() => {
    const timer = setTimeout(() => {
      if (phase < 3) setPhase(phase + 1);
      else onComplete();
    }, phase === 0 ? 400 : phase === 3 ? 500 : 900);
    return () => clearTimeout(timer);
  }, [phase, onComplete]);

  return (
    <motion.div
      initial={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="fixed inset-0 z-[100] bg-[#0a0a0f] flex items-center justify-center overflow-hidden"
    >
      {/* Animated aurora background */}
      <div className="absolute inset-0">
        {[0, 1, 2].map((i) => (
          <motion.div
            key={i}
            className="absolute inset-0"
            style={{
              background: `radial-gradient(ellipse 80% 50% at ${50 + i * 10}% ${40 + i * 20}%,
                ${i === 0 ? 'rgba(6, 182, 212, 0.3)' : i === 1 ? 'rgba(168, 85, 247, 0.25)' : 'rgba(236, 72, 153, 0.2)'} 0%,
                transparent 70%)`,
            }}
            animate={{
              x: [0, 30, -20, 0],
              y: [0, -20, 30, 0],
              scale: [1, 1.1, 0.95, 1],
            }}
            transition={{
              duration: 8 + i * 2,
              repeat: Infinity,
              ease: "easeInOut",
              delay: i * 0.5,
            }}
          />
        ))}
      </div>

      {/* Floating particles */}
      {phase >= 1 && [...Array(20)].map((_, i) => (
        <motion.div
          key={i}
          className="absolute w-1 h-1 rounded-full"
          style={{
            background: i % 3 === 0 ? '#06b6d4' : i % 3 === 1 ? '#a855f7' : '#ec4899',
            left: `${Math.random() * 100}%`,
            top: `${Math.random() * 100}%`,
          }}
          animate={{
            y: [-20, -100],
            opacity: [0, 1, 0],
            scale: [0, 1, 0],
          }}
          transition={{
            duration: 3 + Math.random() * 2,
            repeat: Infinity,
            delay: Math.random() * 2,
          }}
        />
      ))}

      {/* Main content */}
      <div className="relative z-10 flex flex-col items-center">
        {/* Logo with animated gradient */}
        <motion.div
          initial={{ scale: 0, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          transition={{ type: "spring", duration: 0.8 }}
        >
          <motion.h1
            className="text-8xl font-black voca-font-orbitron relative"
            animate={phase >= 1 ? {
              filter: ['drop-shadow(0 0 20px rgba(6,182,212,0.5))', 'drop-shadow(0 0 40px rgba(168,85,247,0.5))', 'drop-shadow(0 0 20px rgba(236,72,153,0.5))'],
            } : {}}
            transition={{ duration: 2, repeat: Infinity }}
          >
            <span
              className="bg-clip-text text-transparent"
              style={{
                backgroundImage: 'linear-gradient(135deg, #06b6d4 0%, #a855f7 50%, #ec4899 100%)',
              }}
            >
              VOCA
            </span>
          </motion.h1>
        </motion.div>

        {/* Animated underline */}
        <motion.div
          className="h-1 w-48 mt-4 rounded-full overflow-hidden"
          initial={{ scaleX: 0 }}
          animate={phase >= 1 ? { scaleX: 1 } : {}}
          transition={{ duration: 0.6 }}
        >
          <motion.div
            className="h-full w-full"
            style={{
              background: 'linear-gradient(90deg, #06b6d4, #a855f7, #ec4899, #06b6d4)',
              backgroundSize: '200% 100%',
            }}
            animate={{ backgroundPosition: ['0% 0%', '200% 0%'] }}
            transition={{ duration: 2, repeat: Infinity, ease: "linear" }}
          />
        </motion.div>

        {/* Tagline */}
        <motion.p
          className="mt-6 text-lg tracking-[0.2em] uppercase voca-font-orbitron"
          style={{
            background: 'linear-gradient(90deg, #06b6d4, #a855f7)',
            WebkitBackgroundClip: 'text',
            WebkitTextFillColor: 'transparent',
          }}
          initial={{ opacity: 0, y: 10 }}
          animate={phase >= 2 ? { opacity: 1, y: 0 } : {}}
          transition={{ duration: 0.5 }}
        >
          Audiobooks Reimagined
        </motion.p>
      </div>
    </motion.div>
  );
}

// ============================================
// OPTION 2: Cyber Glitch - Neon cyberpunk style
// ============================================
function CyberGlitchSplash({ onComplete }: SplashScreenProps) {
  const [phase, setPhase] = useState(0);
  const [glitchActive, setGlitchActive] = useState(false);

  useEffect(() => {
    const timer = setTimeout(() => {
      if (phase < 4) setPhase(phase + 1);
      else onComplete();
    }, phase === 0 ? 300 : phase === 4 ? 500 : 700);
    return () => clearTimeout(timer);
  }, [phase, onComplete]);

  useEffect(() => {
    if (phase >= 1) {
      const glitchInterval = setInterval(() => {
        setGlitchActive(true);
        setTimeout(() => setGlitchActive(false), 100 + Math.random() * 100);
      }, 600 + Math.random() * 800);
      return () => clearInterval(glitchInterval);
    }
  }, [phase]);

  return (
    <motion.div
      initial={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="fixed inset-0 z-[100] bg-black flex items-center justify-center overflow-hidden"
    >
      {/* Grid background */}
      <div
        className="absolute inset-0 opacity-20"
        style={{
          backgroundImage: `
            linear-gradient(rgba(255,0,128,0.1) 1px, transparent 1px),
            linear-gradient(90deg, rgba(0,255,255,0.1) 1px, transparent 1px)
          `,
          backgroundSize: '50px 50px',
        }}
      />

      {/* Scan lines */}
      <motion.div
        className="absolute inset-0 pointer-events-none"
        style={{
          backgroundImage: 'repeating-linear-gradient(0deg, transparent, transparent 2px, rgba(0,255,255,0.03) 2px, rgba(0,255,255,0.03) 4px)'
        }}
        animate={{ backgroundPosition: ['0 0', '0 100px'] }}
        transition={{ duration: 2, repeat: Infinity, ease: "linear" }}
      />

      {/* Glitch bars */}
      {glitchActive && (
        <>
          <motion.div
            className="absolute left-0 right-0 h-2"
            style={{
              top: `${20 + Math.random() * 60}%`,
              background: 'linear-gradient(90deg, transparent, #00ffff, #ff0080, transparent)',
            }}
            initial={{ scaleX: 0 }}
            animate={{ scaleX: [0, 1, 0] }}
            transition={{ duration: 0.1 }}
          />
        </>
      )}

      {/* Main content */}
      <div className="relative flex flex-col items-center">
        {/* Glitch text layers */}
        <div className="relative">
          {/* Magenta offset */}
          <motion.h1
            className="absolute text-8xl font-black voca-font-orbitron"
            style={{ color: '#ff0080', textShadow: '0 0 20px #ff0080' }}
            animate={phase >= 1 ? {
              x: glitchActive ? [-6, 6, -3, 0] : [-1, 1, 0],
              opacity: glitchActive ? [0.8, 1, 0.5] : [0.3, 0.4, 0.3],
            } : { opacity: 0 }}
            transition={{ duration: glitchActive ? 0.05 : 0.5, repeat: Infinity }}
          >
            VOCA
          </motion.h1>

          {/* Cyan offset */}
          <motion.h1
            className="absolute text-8xl font-black voca-font-orbitron"
            style={{ color: '#00ffff', textShadow: '0 0 20px #00ffff' }}
            animate={phase >= 1 ? {
              x: glitchActive ? [6, -6, 3, 0] : [1, -1, 0],
              opacity: glitchActive ? [0.8, 1, 0.5] : [0.3, 0.4, 0.3],
            } : { opacity: 0 }}
            transition={{ duration: glitchActive ? 0.05 : 0.5, repeat: Infinity }}
          >
            VOCA
          </motion.h1>

          {/* Main text */}
          <motion.h1
            className="text-8xl font-black voca-font-orbitron relative"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0, x: glitchActive ? [0, 2, -2, 0] : 0 }}
          >
            <span
              className="bg-clip-text text-transparent"
              style={{
                backgroundImage: 'linear-gradient(135deg, #ff0080, #00ffff)',
                filter: 'drop-shadow(0 0 30px rgba(255,0,128,0.5)) drop-shadow(0 0 60px rgba(0,255,255,0.3))',
              }}
            >
              VOCA
            </span>
          </motion.h1>
        </div>

        {/* Animated line */}
        <motion.div
          className="h-0.5 w-64 mt-4 relative overflow-hidden"
          initial={{ scaleX: 0 }}
          animate={phase >= 2 ? { scaleX: 1 } : {}}
        >
          <motion.div
            className="absolute inset-0"
            style={{ background: 'linear-gradient(90deg, #ff0080, #00ffff, #ff0080)' }}
            animate={{ x: ['-100%', '100%'] }}
            transition={{ duration: 1.5, repeat: Infinity, ease: "linear" }}
          />
        </motion.div>

        {/* Tagline */}
        <motion.p
          className="mt-6 tracking-[0.25em] uppercase text-sm voca-font-orbitron"
          style={{ color: '#00ffff', textShadow: '0 0 10px #00ffff' }}
          initial={{ opacity: 0 }}
          animate={phase >= 3 ? { opacity: 1, x: glitchActive ? [0, -2, 2, 0] : 0 } : {}}
        >
          Audiobooks Reimagined
        </motion.p>
      </div>

      {/* Corner accents */}
      {phase >= 2 && (
        <>
          <motion.div
            className="absolute top-8 left-8 w-20 h-20"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
          >
            <div className="absolute top-0 left-0 w-full h-0.5 bg-gradient-to-r from-cyan-500 to-transparent" />
            <div className="absolute top-0 left-0 w-0.5 h-full bg-gradient-to-b from-cyan-500 to-transparent" />
          </motion.div>
          <motion.div
            className="absolute bottom-8 right-8 w-20 h-20"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
          >
            <div className="absolute bottom-0 right-0 w-full h-0.5 bg-gradient-to-l from-pink-500 to-transparent" />
            <div className="absolute bottom-0 right-0 w-0.5 h-full bg-gradient-to-t from-pink-500 to-transparent" />
          </motion.div>
        </>
      )}
    </motion.div>
  );
}

// ============================================
// OPTION 3: Sunset Gradient - Warm elegant style
// ============================================
function SunsetGradientSplash({ onComplete }: SplashScreenProps) {
  const [phase, setPhase] = useState(0);

  useEffect(() => {
    const timer = setTimeout(() => {
      if (phase < 3) setPhase(phase + 1);
      else onComplete();
    }, phase === 0 ? 400 : phase === 3 ? 500 : 900);
    return () => clearTimeout(timer);
  }, [phase, onComplete]);

  return (
    <motion.div
      initial={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="fixed inset-0 z-[100] flex items-center justify-center overflow-hidden"
      style={{
        background: 'linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f0f23 100%)',
      }}
    >
      {/* Animated gradient orbs */}
      <motion.div
        className="absolute w-[600px] h-[600px] rounded-full"
        style={{
          background: 'radial-gradient(circle, rgba(251,146,60,0.3) 0%, transparent 70%)',
          filter: 'blur(40px)',
        }}
        animate={{
          x: [-100, 100, -100],
          y: [-50, 50, -50],
          scale: [1, 1.2, 1],
        }}
        transition={{ duration: 10, repeat: Infinity, ease: "easeInOut" }}
      />
      <motion.div
        className="absolute w-[500px] h-[500px] rounded-full"
        style={{
          background: 'radial-gradient(circle, rgba(244,63,94,0.25) 0%, transparent 70%)',
          filter: 'blur(40px)',
        }}
        animate={{
          x: [100, -100, 100],
          y: [50, -50, 50],
          scale: [1.2, 1, 1.2],
        }}
        transition={{ duration: 8, repeat: Infinity, ease: "easeInOut" }}
      />

      {/* Content */}
      <div className="relative z-10 flex flex-col items-center">
        <motion.div
          initial={{ scale: 0.8, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          transition={{ type: "spring", duration: 0.8 }}
        >
          <h1 className="text-8xl font-black voca-font-orbitron">
            <span
              className="bg-clip-text text-transparent"
              style={{
                backgroundImage: 'linear-gradient(135deg, #fb923c 0%, #f43f5e 50%, #a855f7 100%)',
                filter: 'drop-shadow(0 0 30px rgba(251,146,60,0.4))',
              }}
            >
              VOCA
            </span>
          </h1>
        </motion.div>

        {/* Decorative line */}
        <motion.div
          className="flex items-center gap-4 mt-4"
          initial={{ opacity: 0 }}
          animate={phase >= 1 ? { opacity: 1 } : {}}
        >
          <motion.div
            className="h-0.5 w-16 rounded-full"
            style={{ background: 'linear-gradient(90deg, transparent, #fb923c)' }}
            initial={{ scaleX: 0 }}
            animate={phase >= 1 ? { scaleX: 1 } : {}}
          />
          <motion.div
            className="w-2 h-2 rounded-full"
            style={{ background: 'linear-gradient(135deg, #fb923c, #f43f5e)' }}
            animate={{ scale: [1, 1.3, 1] }}
            transition={{ duration: 1.5, repeat: Infinity }}
          />
          <motion.div
            className="h-0.5 w-16 rounded-full"
            style={{ background: 'linear-gradient(90deg, #f43f5e, transparent)' }}
            initial={{ scaleX: 0 }}
            animate={phase >= 1 ? { scaleX: 1 } : {}}
          />
        </motion.div>

        {/* Tagline */}
        <motion.p
          className="mt-6 text-lg tracking-[0.15em] voca-font-orbitron"
          style={{
            background: 'linear-gradient(90deg, #fb923c, #f43f5e)',
            WebkitBackgroundClip: 'text',
            WebkitTextFillColor: 'transparent',
          }}
          initial={{ opacity: 0, y: 10 }}
          animate={phase >= 2 ? { opacity: 1, y: 0 } : {}}
        >
          Audiobooks Reimagined
        </motion.p>
      </div>
    </motion.div>
  );
}

// ============================================
// OPTION 4: Electric Pulse - High energy neon
// ============================================
function ElectricPulseSplash({ onComplete }: SplashScreenProps) {
  const [phase, setPhase] = useState(0);
  const [pulse, setPulse] = useState(false);

  useEffect(() => {
    const timer = setTimeout(() => {
      if (phase < 3) setPhase(phase + 1);
      else onComplete();
    }, phase === 0 ? 200 : phase === 1 ? 800 : 1000);
    return () => clearTimeout(timer);
  }, [phase, onComplete]);

  useEffect(() => {
    if (phase >= 1) {
      const pulseInterval = setInterval(() => {
        setPulse(true);
        setTimeout(() => setPulse(false), 200);
      }, 2000);
      return () => clearInterval(pulseInterval);
    }
  }, [phase]);

  return (
    <motion.div
      initial={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="fixed inset-0 z-[100] bg-[#050510] flex items-center justify-center overflow-hidden"
    >
      {/* Electric rings */}
      {phase >= 1 && [...Array(3)].map((_, i) => (
        <motion.div
          key={i}
          className="absolute rounded-full"
          style={{
            width: 200 + i * 150,
            height: 200 + i * 150,
            border: '2px solid transparent',
            borderImage: `linear-gradient(${45 + i * 30}deg, #00ff88, #00d4ff, #a855f7) 1`,
            opacity: 0.3,
          }}
          animate={{
            rotate: i % 2 === 0 ? [0, 360] : [360, 0],
            scale: pulse ? [1, 1.05, 1] : 1,
          }}
          transition={{
            rotate: { duration: 20 + i * 5, repeat: Infinity, ease: "linear" },
            scale: { duration: 0.2 },
          }}
        />
      ))}

      {/* Pulse wave on trigger */}
      {pulse && (
        <motion.div
          className="absolute rounded-full"
          style={{
            width: 100,
            height: 100,
            background: 'radial-gradient(circle, rgba(0,255,136,0.4) 0%, transparent 70%)',
          }}
          initial={{ scale: 1, opacity: 1 }}
          animate={{ scale: 5, opacity: 0 }}
          transition={{ duration: 0.5 }}
        />
      )}

      {/* Content */}
      <div className="relative z-10 flex flex-col items-center">
        <motion.div
          initial={{ scale: 0, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          transition={{ type: "spring", duration: 0.6 }}
        >
          {/* Glow layers */}
          <h1
            className="text-9xl font-black voca-font-orbitron absolute blur-2xl"
            style={{ color: '#00ff88', opacity: 0.5 }}
          >
            VOCA
          </h1>
          <h1
            className="text-9xl font-black voca-font-orbitron absolute blur-md"
            style={{ color: '#00d4ff', opacity: 0.6 }}
          >
            VOCA
          </h1>
          <h1
            className="text-9xl font-black voca-font-orbitron relative"
            style={{
              background: 'linear-gradient(135deg, #00ff88, #00d4ff, #a855f7)',
              WebkitBackgroundClip: 'text',
              WebkitTextFillColor: 'transparent',
              filter: pulse ? 'brightness(1.3)' : 'brightness(1)',
            }}
          >
            VOCA
          </h1>
        </motion.div>

        {/* Energy bar */}
        <motion.div
          className="h-1 w-56 mt-6 rounded-full overflow-hidden bg-white/10"
          initial={{ opacity: 0 }}
          animate={phase >= 1 ? { opacity: 1 } : {}}
        >
          <motion.div
            className="h-full rounded-full"
            style={{ background: 'linear-gradient(90deg, #00ff88, #00d4ff, #a855f7)' }}
            initial={{ width: 0 }}
            animate={phase >= 1 ? { width: '100%' } : {}}
            transition={{ duration: 0.8 }}
          />
        </motion.div>

        {/* Tagline */}
        <motion.p
          className="mt-6 text-xl tracking-widest voca-font-orbitron"
          style={{
            background: 'linear-gradient(90deg, #00ff88, #00d4ff)',
            WebkitBackgroundClip: 'text',
            WebkitTextFillColor: 'transparent',
            textShadow: pulse ? '0 0 20px rgba(0,255,136,0.5)' : 'none',
          }}
          initial={{ opacity: 0 }}
          animate={phase >= 2 ? { opacity: 1 } : {}}
        >
          Audiobooks Reimagined
        </motion.p>
      </div>
    </motion.div>
  );
}

// Map variant names to components
const splashComponents: Record<SplashVariant, React.ComponentType<SplashScreenProps>> = {
  pulseWave: AuroraWavesSplash,
  glitchCyber: CyberGlitchSplash,
  waveformMorph: SunsetGradientSplash,
  neonFlicker: ElectricPulseSplash,
};

// ============================================
// MAIN EXPORT - Uses store to determine variant
// ============================================
export function SplashScreen({ onComplete }: SplashScreenProps) {
  const splashVariant = useStore((state) => state.splashVariant);
  const SplashComponent = splashComponents[splashVariant] || CyberGlitchSplash;

  return <SplashComponent onComplete={onComplete} />;
}
