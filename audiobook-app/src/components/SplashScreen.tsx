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
// Cartoon Brain with reverberating lines
// ============================================
function BrainAnimation({ phase }: { phase: number }) {
  return (
    <motion.div
      className="absolute -top-32"
      initial={{ opacity: 0, scale: 0.8 }}
      animate={phase >= 1 ? { opacity: 1, scale: 1 } : {}}
      transition={{ duration: 0.5 }}
    >
      <svg width="120" height="100" viewBox="0 0 120 100">
        {/* Brain outline - simple cartoon style */}
        <motion.path
          d="M60 10
             C30 10 20 30 25 45
             C15 50 15 65 25 70
             C20 80 30 90 45 88
             C50 95 70 95 75 88
             C90 90 100 80 95 70
             C105 65 105 50 95 45
             C100 30 90 10 60 10Z"
          fill="none"
          stroke="white"
          strokeWidth="2"
          initial={{ pathLength: 0 }}
          animate={{ pathLength: 1 }}
          transition={{ duration: 1, delay: 0.2 }}
        />

        {/* Brain center line */}
        <motion.path
          d="M60 15 C60 25 55 40 60 50 C65 60 55 75 60 85"
          fill="none"
          stroke="white"
          strokeWidth="1.5"
          initial={{ pathLength: 0 }}
          animate={{ pathLength: 1 }}
          transition={{ duration: 0.8, delay: 0.5 }}
        />

        {/* Left brain squiggles */}
        <motion.path
          d="M35 35 Q45 40 40 50 Q35 55 45 60"
          fill="none"
          stroke="white"
          strokeWidth="1.5"
          initial={{ pathLength: 0 }}
          animate={{ pathLength: 1 }}
          transition={{ duration: 0.6, delay: 0.7 }}
        />

        {/* Right brain squiggles */}
        <motion.path
          d="M85 35 Q75 40 80 50 Q85 55 75 60"
          fill="none"
          stroke="white"
          strokeWidth="1.5"
          initial={{ pathLength: 0 }}
          animate={{ pathLength: 1 }}
          transition={{ duration: 0.6, delay: 0.7 }}
        />

        {/* Reverberating circles/waves */}
        {[0, 1, 2].map((i) => (
          <motion.circle
            key={i}
            cx="60"
            cy="50"
            r="40"
            fill="none"
            stroke="white"
            strokeWidth="1"
            initial={{ r: 40, opacity: 0 }}
            animate={phase >= 2 ? {
              r: [40, 70, 100],
              opacity: [0.6, 0.3, 0],
            } : {}}
            transition={{
              duration: 1.5,
              repeat: Infinity,
              delay: i * 0.4,
              ease: "easeOut"
            }}
          />
        ))}
      </svg>
    </motion.div>
  );
}

// ============================================
// OPTION 2: Glitch Cyber (Edgy, Neon) - Enhanced
// ============================================
function GlitchCyberSplash({ onComplete }: SplashScreenProps) {
  const [phase, setPhase] = useState(0);
  const [glitchActive, setGlitchActive] = useState(false);

  useEffect(() => {
    const timer = setTimeout(() => {
      if (phase < 4) setPhase(phase + 1);
      else onComplete();
    }, phase === 0 ? 300 : phase === 4 ? 600 : 700);
    return () => clearTimeout(timer);
  }, [phase, onComplete]);

  // Random glitch effect
  useEffect(() => {
    if (phase >= 1) {
      const glitchInterval = setInterval(() => {
        setGlitchActive(true);
        setTimeout(() => setGlitchActive(false), 100 + Math.random() * 150);
      }, 500 + Math.random() * 1000);
      return () => clearInterval(glitchInterval);
    }
  }, [phase]);

  return (
    <motion.div
      initial={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="fixed inset-0 z-[100] bg-black flex items-center justify-center overflow-hidden"
    >
      {/* Animated scan lines */}
      <motion.div
        className="absolute inset-0 pointer-events-none"
        style={{
          backgroundImage: 'repeating-linear-gradient(0deg, transparent, transparent 2px, rgba(0,255,255,0.03) 2px, rgba(0,255,255,0.03) 4px)'
        }}
        animate={{
          backgroundPosition: ['0 0', '0 100px'],
        }}
        transition={{
          duration: 2,
          repeat: Infinity,
          ease: "linear"
        }}
      />

      {/* Horizontal glitch bars */}
      {glitchActive && (
        <>
          <motion.div
            className="absolute left-0 right-0 h-2 bg-cyan-500/30"
            style={{ top: `${20 + Math.random() * 60}%` }}
            initial={{ scaleX: 0 }}
            animate={{ scaleX: [0, 1, 0] }}
            transition={{ duration: 0.1 }}
          />
          <motion.div
            className="absolute left-0 right-0 h-1 bg-pink-500/40"
            style={{ top: `${20 + Math.random() * 60}%` }}
            initial={{ scaleX: 0 }}
            animate={{ scaleX: [0, 1, 0] }}
            transition={{ duration: 0.15 }}
          />
        </>
      )}

      {/* Noise overlay */}
      <div
        className="absolute inset-0 opacity-5 pointer-events-none"
        style={{
          backgroundImage: `url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noise'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%' height='100%' filter='url(%23noise)'/%3E%3C/svg%3E")`,
        }}
      />

      {/* Content */}
      <div className="relative flex flex-col items-center">
        {/* Brain animation */}
        <BrainAnimation phase={phase} />

        {/* Glitch layers */}
        <div className="relative mt-8">
          {/* Red offset - more aggressive */}
          <motion.h1
            className="absolute text-8xl font-black"
            style={{
              color: 'rgba(255,0,128,0.5)',
              textShadow: '0 0 10px rgba(255,0,128,0.5)'
            }}
            animate={phase >= 1 ? {
              x: glitchActive ? [-8, 8, -4, 0] : [-2, 2, -1, 0],
              y: glitchActive ? [2, -2, 1, 0] : 0,
              opacity: glitchActive ? [0.8, 1, 0.6, 0.3] : [0.3, 0.5, 0.3],
              skewX: glitchActive ? [0, 2, -2, 0] : 0,
            } : { opacity: 0 }}
            transition={{ duration: glitchActive ? 0.1 : 0.5, repeat: glitchActive ? 1 : Infinity }}
          >
            VOCA
          </motion.h1>

          {/* Cyan offset - more aggressive */}
          <motion.h1
            className="absolute text-8xl font-black"
            style={{
              color: 'rgba(0,255,255,0.5)',
              textShadow: '0 0 10px rgba(0,255,255,0.5)'
            }}
            animate={phase >= 1 ? {
              x: glitchActive ? [8, -8, 4, 0] : [2, -2, 1, 0],
              y: glitchActive ? [-2, 2, -1, 0] : 0,
              opacity: glitchActive ? [0.8, 1, 0.6, 0.3] : [0.3, 0.5, 0.3],
              skewX: glitchActive ? [0, -2, 2, 0] : 0,
            } : { opacity: 0 }}
            transition={{ duration: glitchActive ? 0.1 : 0.5, repeat: glitchActive ? 1 : Infinity }}
          >
            VOCA
          </motion.h1>

          {/* Main text with clip effect during glitch */}
          <motion.h1
            className="text-8xl font-black relative"
            initial={{ opacity: 0, y: 20 }}
            animate={{
              opacity: 1,
              y: 0,
              x: glitchActive ? [0, 3, -3, 0] : 0,
            }}
            style={{
              clipPath: glitchActive
                ? `polygon(0 0, 100% 0, 100% ${30 + Math.random() * 20}%, 0 ${30 + Math.random() * 20}%, 0 ${60 + Math.random() * 20}%, 100% ${60 + Math.random() * 20}%, 100% 100%, 0 100%)`
                : 'none'
            }}
          >
            <span
              className="bg-clip-text text-transparent"
              style={{
                backgroundImage: 'linear-gradient(135deg, #ff0080, #00ffff, #ff0080)',
                textShadow: '0 0 30px rgba(255,0,128,0.5), 0 0 60px rgba(0,255,255,0.3)'
              }}
            >
              VOCA
            </span>
          </motion.h1>

          {/* Underline beam - animated */}
          <motion.div
            className="h-1 mt-2 relative overflow-hidden"
            initial={{ scaleX: 0, opacity: 0 }}
            animate={phase >= 2 ? { scaleX: 1, opacity: 1 } : {}}
            transition={{ duration: 0.5 }}
          >
            <motion.div
              className="absolute inset-0 bg-gradient-to-r from-pink-500 via-cyan-400 to-pink-500"
              animate={{
                x: ['-100%', '100%'],
              }}
              transition={{
                duration: 2,
                repeat: Infinity,
                ease: "linear"
              }}
            />
          </motion.div>

          {/* Tagline with glitch */}
          <motion.p
            className="text-center mt-6 tracking-[0.3em] uppercase text-sm font-mono"
            style={{
              color: '#00ffff',
              textShadow: '0 0 10px rgba(0,255,255,0.8)'
            }}
            initial={{ opacity: 0 }}
            animate={phase >= 3 ? {
              opacity: 1,
              x: glitchActive ? [0, -2, 2, 0] : 0,
            } : {}}
          >
            Audio Reimagined
          </motion.p>
        </div>
      </div>

      {/* Corner decorations - animated */}
      {phase >= 2 && (
        <>
          <motion.div
            className="absolute top-8 left-8 w-24 h-24"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
          >
            <motion.div
              className="absolute top-0 left-0 w-full h-0.5 bg-gradient-to-r from-cyan-500 to-transparent"
              animate={{ scaleX: [0, 1] }}
              transition={{ duration: 0.3 }}
            />
            <motion.div
              className="absolute top-0 left-0 w-0.5 h-full bg-gradient-to-b from-cyan-500 to-transparent"
              animate={{ scaleY: [0, 1] }}
              transition={{ duration: 0.3, delay: 0.1 }}
            />
          </motion.div>
          <motion.div
            className="absolute bottom-8 right-8 w-24 h-24"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
          >
            <motion.div
              className="absolute bottom-0 right-0 w-full h-0.5 bg-gradient-to-l from-pink-500 to-transparent"
              animate={{ scaleX: [0, 1] }}
              transition={{ duration: 0.3 }}
            />
            <motion.div
              className="absolute bottom-0 right-0 w-0.5 h-full bg-gradient-to-t from-pink-500 to-transparent"
              animate={{ scaleY: [0, 1] }}
              transition={{ duration: 0.3, delay: 0.1 }}
            />
          </motion.div>

          {/* Extra corner accents */}
          <motion.div
            className="absolute top-8 right-8 w-16 h-16 border-r-2 border-t-2 border-pink-500/30"
            initial={{ opacity: 0 }}
            animate={{ opacity: [0, 1, 0.5] }}
            transition={{ duration: 1, repeat: Infinity }}
          />
          <motion.div
            className="absolute bottom-8 left-8 w-16 h-16 border-l-2 border-b-2 border-cyan-500/30"
            initial={{ opacity: 0 }}
            animate={{ opacity: [0, 1, 0.5] }}
            transition={{ duration: 1, repeat: Infinity, delay: 0.5 }}
          />
        </>
      )}

      {/* Floating glitch particles */}
      {phase >= 1 && [...Array(8)].map((_, i) => (
        <motion.div
          key={i}
          className="absolute w-1 h-1 bg-cyan-400"
          style={{
            left: `${10 + Math.random() * 80}%`,
            top: `${10 + Math.random() * 80}%`,
          }}
          animate={{
            opacity: [0, 1, 0],
            scale: [0, 1.5, 0],
            x: [0, (Math.random() - 0.5) * 50],
            y: [0, (Math.random() - 0.5) * 50],
          }}
          transition={{
            duration: 1 + Math.random(),
            repeat: Infinity,
            delay: Math.random() * 2,
          }}
        />
      ))}
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
