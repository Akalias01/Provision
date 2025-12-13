import { motion } from 'framer-motion';
import { useState, useEffect } from 'react';
import { useStore, type SplashVariant } from '../store/useStore';

interface SplashScreenProps {
  onComplete: () => void;
}

// ============================================
// OPTION 1: Pulse Wave (Clean, Modern)
// ============================================
function PulseWaveSplash({ onComplete }: SplashScreenProps) {
  const [phase, setPhase] = useState(0);

  useEffect(() => {
    const timer = setTimeout(() => {
      if (phase < 3) setPhase(phase + 1);
      else onComplete();
    }, phase === 0 ? 500 : phase === 3 ? 400 : 800);
    return () => clearTimeout(timer);
  }, [phase, onComplete]);

  return (
    <motion.div
      initial={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="fixed inset-0 z-[100] bg-gradient-to-br from-slate-950 via-slate-900 to-cyan-950 flex items-center justify-center overflow-hidden"
    >
      {/* Pulse rings */}
      {[...Array(4)].map((_, i) => (
        <motion.div
          key={i}
          className="absolute rounded-full border-2 border-cyan-500/30"
          initial={{ width: 100, height: 100, opacity: 0 }}
          animate={phase >= 1 ? {
            width: [100, 600],
            height: [100, 600],
            opacity: [0.8, 0],
          } : {}}
          transition={{
            duration: 2,
            delay: i * 0.3,
            repeat: Infinity,
            ease: "easeOut"
          }}
        />
      ))}

      {/* Logo */}
      <motion.div
        initial={{ scale: 0, opacity: 0 }}
        animate={{ scale: 1, opacity: 1 }}
        transition={{ type: "spring", duration: 0.8 }}
        className="relative z-10"
      >
        <motion.h1
          className="text-7xl font-black tracking-tighter"
          animate={phase >= 2 ? {
            textShadow: ["0 0 20px rgba(6,182,212,0)", "0 0 60px rgba(6,182,212,0.8)", "0 0 20px rgba(6,182,212,0.3)"]
          } : {}}
          transition={{ duration: 1 }}
        >
          <span className="bg-gradient-to-r from-cyan-400 via-teal-300 to-emerald-400 bg-clip-text text-transparent">
            VOCA
          </span>
        </motion.h1>

        {/* Sound bars under text */}
        <div className="flex justify-center gap-1 mt-4">
          {[...Array(20)].map((_, i) => (
            <motion.div
              key={i}
              className="w-1.5 bg-gradient-to-t from-cyan-600 to-teal-400 rounded-full"
              initial={{ height: 4 }}
              animate={phase >= 1 ? {
                height: [4, 15 + Math.random() * 30, 4],
              } : {}}
              transition={{
                duration: 0.5,
                repeat: Infinity,
                delay: i * 0.05,
                ease: "easeInOut"
              }}
            />
          ))}
        </div>
      </motion.div>
    </motion.div>
  );
}

// ============================================
// OPTION 2: Glitch Cyber (Edgy, Neon)
// ============================================
function GlitchCyberSplash({ onComplete }: SplashScreenProps) {
  const [phase, setPhase] = useState(0);

  useEffect(() => {
    const timer = setTimeout(() => {
      if (phase < 4) setPhase(phase + 1);
      else onComplete();
    }, phase === 0 ? 300 : phase === 4 ? 500 : 600);
    return () => clearTimeout(timer);
  }, [phase, onComplete]);

  return (
    <motion.div
      initial={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="fixed inset-0 z-[100] bg-black flex items-center justify-center overflow-hidden"
    >
      {/* Scan lines */}
      <div className="absolute inset-0 opacity-10" style={{
        backgroundImage: 'repeating-linear-gradient(0deg, transparent, transparent 2px, rgba(0,255,255,0.1) 2px, rgba(0,255,255,0.1) 4px)'
      }} />

      {/* Glitch layers */}
      <div className="relative">
        {/* Red offset */}
        <motion.h1
          className="absolute text-8xl font-black text-red-500/50"
          animate={phase >= 1 ? {
            x: [-4, 4, -2, 0],
            opacity: [0.5, 0.8, 0.3, 0]
          } : { opacity: 0 }}
          transition={{ duration: 0.3, repeat: 3 }}
        >
          VOCA
        </motion.h1>

        {/* Cyan offset */}
        <motion.h1
          className="absolute text-8xl font-black text-cyan-500/50"
          animate={phase >= 1 ? {
            x: [4, -4, 2, 0],
            opacity: [0.5, 0.8, 0.3, 0]
          } : { opacity: 0 }}
          transition={{ duration: 0.3, repeat: 3 }}
        >
          VOCA
        </motion.h1>

        {/* Main text */}
        <motion.h1
          className="text-8xl font-black relative"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
        >
          <span className="bg-gradient-to-r from-red-500 via-pink-500 to-cyan-400 bg-clip-text text-transparent">
            VOCA
          </span>
        </motion.h1>

        {/* Underline beam */}
        <motion.div
          className="h-1 bg-gradient-to-r from-transparent via-cyan-400 to-transparent mt-2"
          initial={{ scaleX: 0, opacity: 0 }}
          animate={phase >= 2 ? { scaleX: 1, opacity: 1 } : {}}
          transition={{ duration: 0.5 }}
        />

        {/* Tagline */}
        <motion.p
          className="text-center mt-6 text-cyan-400/80 tracking-[0.3em] uppercase text-sm"
          initial={{ opacity: 0 }}
          animate={phase >= 3 ? { opacity: 1 } : {}}
        >
          Audio Reimagined
        </motion.p>
      </div>

      {/* Corner decorations */}
      {phase >= 2 && (
        <>
          <motion.div
            className="absolute top-8 left-8 w-20 h-20 border-l-2 border-t-2 border-cyan-500/50"
            initial={{ opacity: 0 }} animate={{ opacity: 1 }}
          />
          <motion.div
            className="absolute bottom-8 right-8 w-20 h-20 border-r-2 border-b-2 border-pink-500/50"
            initial={{ opacity: 0 }} animate={{ opacity: 1 }}
          />
        </>
      )}
    </motion.div>
  );
}

// ============================================
// OPTION 3: Waveform Morph (Smooth, Elegant)
// ============================================
function WaveformMorphSplash({ onComplete }: SplashScreenProps) {
  const [phase, setPhase] = useState(0);

  useEffect(() => {
    const timer = setTimeout(() => {
      if (phase < 3) setPhase(phase + 1);
      else onComplete();
    }, phase === 0 ? 400 : 1000);
    return () => clearTimeout(timer);
  }, [phase, onComplete]);

  const wavePoints = 50;
  const generateWavePath = (offset: number) => {
    let path = 'M 0 50';
    for (let i = 0; i <= wavePoints; i++) {
      const x = (i / wavePoints) * 300;
      const y = 50 + Math.sin((i / wavePoints) * Math.PI * 4 + offset) * 20;
      path += ` L ${x} ${y}`;
    }
    return path;
  };

  return (
    <motion.div
      initial={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="fixed inset-0 z-[100] bg-gradient-to-b from-indigo-950 via-purple-950 to-slate-950 flex items-center justify-center"
    >
      <div className="relative">
        {/* Animated waveform */}
        <motion.svg
          width="300"
          height="100"
          className="absolute -top-16 left-1/2 -translate-x-1/2"
          initial={{ opacity: 0 }}
          animate={phase >= 1 ? { opacity: 1 } : {}}
        >
          {[0, 1, 2].map((i) => (
            <motion.path
              key={i}
              d={generateWavePath(i)}
              fill="none"
              stroke={`rgba(168, 85, 247, ${0.3 + i * 0.2})`}
              strokeWidth="2"
              animate={{
                d: [
                  generateWavePath(i),
                  generateWavePath(i + Math.PI),
                  generateWavePath(i + Math.PI * 2),
                ]
              }}
              transition={{
                duration: 2,
                repeat: Infinity,
                ease: "linear"
              }}
            />
          ))}
        </motion.svg>

        {/* Logo */}
        <motion.h1
          className="text-8xl font-black tracking-tight"
          initial={{ opacity: 0, scale: 0.9 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ type: "spring", duration: 0.8 }}
        >
          <span className="bg-gradient-to-r from-violet-400 via-purple-400 to-fuchsia-400 bg-clip-text text-transparent">
            VOCA
          </span>
        </motion.h1>

        {/* Glow orb */}
        <motion.div
          className="absolute -inset-20 rounded-full"
          style={{
            background: 'radial-gradient(circle, rgba(168,85,247,0.2) 0%, transparent 70%)'
          }}
          animate={phase >= 2 ? {
            scale: [1, 1.2, 1],
            opacity: [0.5, 0.8, 0.5]
          } : {}}
          transition={{ duration: 2, repeat: Infinity }}
        />
      </div>
    </motion.div>
  );
}

// ============================================
// OPTION 4: Neon Flicker (Retro, Bold)
// ============================================
function NeonFlickerSplash({ onComplete }: SplashScreenProps) {
  const [phase, setPhase] = useState(0);
  const [flicker, setFlicker] = useState(true);

  useEffect(() => {
    const timer = setTimeout(() => {
      if (phase < 3) setPhase(phase + 1);
      else onComplete();
    }, phase === 0 ? 200 : phase === 1 ? 800 : 1200);
    return () => clearTimeout(timer);
  }, [phase, onComplete]);

  useEffect(() => {
    if (phase === 1) {
      const flickerInterval = setInterval(() => {
        setFlicker(f => !f);
      }, 100);
      setTimeout(() => {
        clearInterval(flickerInterval);
        setFlicker(true);
      }, 600);
      return () => clearInterval(flickerInterval);
    }
  }, [phase]);

  return (
    <motion.div
      initial={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="fixed inset-0 z-[100] bg-slate-950 flex items-center justify-center"
    >
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: flicker ? 1 : 0.3 }}
        className="relative"
      >
        {/* Neon glow layers */}
        <h1
          className="text-9xl font-black absolute blur-2xl"
          style={{
            color: '#ff0080',
            textShadow: '0 0 80px #ff0080'
          }}
        >
          VOCA
        </h1>
        <h1
          className="text-9xl font-black absolute blur-md"
          style={{
            color: '#ff0080',
            textShadow: '0 0 40px #ff0080'
          }}
        >
          VOCA
        </h1>
        <h1
          className="text-9xl font-black relative"
          style={{
            color: '#fff',
            textShadow: '0 0 10px #fff, 0 0 20px #ff0080, 0 0 40px #ff0080, 0 0 80px #ff0080'
          }}
        >
          VOCA
        </h1>

        {/* Subtitle */}
        <motion.p
          className="text-center mt-8 text-2xl tracking-widest"
          style={{
            color: '#00ffff',
            textShadow: '0 0 10px #00ffff, 0 0 20px #00ffff'
          }}
          initial={{ opacity: 0 }}
          animate={phase >= 2 ? { opacity: 1 } : {}}
        >
          LISTEN • LEARN • LIVE
        </motion.p>
      </motion.div>
    </motion.div>
  );
}

// Map variant names to components
const splashComponents: Record<SplashVariant, React.ComponentType<SplashScreenProps>> = {
  pulseWave: PulseWaveSplash,
  glitchCyber: GlitchCyberSplash,
  waveformMorph: WaveformMorphSplash,
  neonFlicker: NeonFlickerSplash,
};

// ============================================
// MAIN EXPORT - Uses store to determine variant
// ============================================
export function SplashScreen({ onComplete }: SplashScreenProps) {
  const splashVariant = useStore((state) => state.splashVariant);
  const SplashComponent = splashComponents[splashVariant] || GlitchCyberSplash;

  return <SplashComponent onComplete={onComplete} />;
}
