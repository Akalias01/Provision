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
          className="text-7xl font-black tracking-tighter voca-font-orbitron"
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
// Brain Animation - Detailed brain with lightning bolts
// Starts B&W, lights up coral when reverberating
// ============================================
function BrainWithLightning({ phase }: { phase: number }) {
  const isActive = phase >= 2;

  // Brain outline path - detailed with folds
  const brainPath = `
    M60 15
    C45 15 35 20 30 30
    C25 40 20 50 22 60
    C24 70 30 78 40 82
    C50 86 55 88 60 88
    C65 88 70 86 80 82
    C90 78 96 70 98 60
    C100 50 95 40 90 30
    C85 20 75 15 60 15
    Z
  `;

  // Brain fold paths (internal details)
  const foldPaths = [
    "M35 35 Q45 30 50 40 Q55 50 45 55 Q35 50 35 40",
    "M40 55 Q50 50 55 60 Q50 70 40 65 Q35 60 40 55",
    "M55 30 Q60 25 65 30 Q70 35 65 40 Q60 35 55 35",
    "M70 40 Q80 35 85 45 Q80 55 70 50 Q65 45 70 40",
    "M65 55 Q75 50 80 60 Q75 70 65 65 Q60 60 65 55",
    "M50 45 Q55 40 60 45 Q65 50 60 55 Q55 50 50 45",
  ];

  // Lightning bolt paths
  const lightningBolts = [
    { path: "M25 25 L20 35 L28 33 L18 50", angle: -30, x: -15, y: -10 },
    { path: "M95 25 L100 35 L92 33 L102 50", angle: 30, x: 15, y: -10 },
    { path: "M15 50 L8 55 L15 54 L5 65", angle: -45, x: -20, y: 5 },
    { path: "M105 50 L112 55 L105 54 L115 65", angle: 45, x: 20, y: 5 },
  ];

  return (
    <motion.div
      className="absolute -top-40"
      initial={{ opacity: 0, scale: 0.8 }}
      animate={phase >= 1 ? { opacity: 1, scale: 1 } : {}}
      transition={{ duration: 0.5 }}
    >
      <svg width="140" height="120" viewBox="-10 0 140 120">
        <defs>
          {/* Gradient for lit-up brain */}
          <linearGradient id="brainGradient" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="#ff6b6b" />
            <stop offset="50%" stopColor="#f06595" />
            <stop offset="100%" stopColor="#ff8787" />
          </linearGradient>

          {/* Glow filter */}
          <filter id="brainGlow" x="-50%" y="-50%" width="200%" height="200%">
            <feGaussianBlur stdDeviation="3" result="coloredBlur"/>
            <feMerge>
              <feMergeNode in="coloredBlur"/>
              <feMergeNode in="SourceGraphic"/>
            </feMerge>
          </filter>
        </defs>

        {/* Brain body - starts gray, becomes coral */}
        <motion.path
          d={brainPath}
          initial={{ fill: "#4a5568", stroke: "#718096" }}
          animate={isActive ? {
            fill: ["#4a5568", "#ff6b6b", "#f06595"],
            stroke: ["#718096", "#ff8787", "#ffa8a8"],
          } : {}}
          transition={{ duration: 0.8 }}
          strokeWidth="2"
          filter={isActive ? "url(#brainGlow)" : "none"}
        />

        {/* Brain folds - internal details */}
        {foldPaths.map((path, index) => (
          <motion.path
            key={index}
            d={path}
            fill="none"
            initial={{ stroke: "#2d3748" }}
            animate={isActive ? {
              stroke: ["#2d3748", "#c53030", "#e53e3e"],
            } : {}}
            transition={{ duration: 0.8, delay: index * 0.05 }}
            strokeWidth="1.5"
          />
        ))}

        {/* Lightning bolts - appear and animate when active */}
        {lightningBolts.map((bolt, index) => (
          <motion.g key={index}>
            <motion.path
              d={bolt.path}
              fill="none"
              stroke={isActive ? "#ffd43b" : "#a0aec0"}
              strokeWidth="3"
              strokeLinecap="round"
              strokeLinejoin="round"
              initial={{ pathLength: 0, opacity: 0 }}
              animate={phase >= 1 ? {
                pathLength: 1,
                opacity: isActive ? [1, 0.5, 1] : 0.4,
              } : {}}
              transition={{
                pathLength: { duration: 0.3, delay: 0.5 + index * 0.1 },
                opacity: { duration: 0.3, repeat: isActive ? Infinity : 0, repeatType: "reverse" }
              }}
              filter={isActive ? "url(#brainGlow)" : "none"}
            />
          </motion.g>
        ))}

        {/* Energy waves emanating when active */}
        {isActive && [0, 1, 2].map((i) => (
          <motion.ellipse
            key={`wave-${i}`}
            cx="60"
            cy="50"
            rx="40"
            ry="30"
            fill="none"
            stroke="#ff6b6b"
            strokeWidth="1"
            initial={{ scale: 1, opacity: 0.6 }}
            animate={{
              scale: [1, 1.5, 2],
              opacity: [0.6, 0.3, 0],
            }}
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
// App Logo - Square with V and integrated play button
// ============================================
function AppLogo({ size = 80 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 100 100">
      <defs>
        <linearGradient id="logoGrad" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stopColor="#ff0080" />
          <stop offset="50%" stopColor="#00ffff" />
          <stop offset="100%" stopColor="#ff0080" />
        </linearGradient>
      </defs>

      {/* Rounded square background */}
      <rect
        x="5"
        y="5"
        width="90"
        height="90"
        rx="18"
        fill="url(#logoGrad)"
      />

      {/* V shape with integrated play button */}
      <path
        d="M25 25 L50 70 L75 25"
        fill="none"
        stroke="white"
        strokeWidth="8"
        strokeLinecap="round"
        strokeLinejoin="round"
      />

      {/* Play triangle integrated in V */}
      <path
        d="M42 45 L58 55 L42 65 Z"
        fill="white"
      />
    </svg>
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
        {/* Brain with lightning animation */}
        <BrainWithLightning phase={phase} />

        {/* Glitch layers */}
        <div className="relative mt-8">
          {/* Red offset - more aggressive */}
          <motion.h1
            className="absolute text-8xl font-black voca-font-orbitron"
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
            className="absolute text-8xl font-black voca-font-orbitron"
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
            className="text-8xl font-black relative voca-font-orbitron"
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
            className="text-center mt-6 tracking-[0.3em] uppercase text-sm voca-font-orbitron"
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
          className="text-8xl font-black tracking-tight voca-font-orbitron"
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
          className="text-9xl font-black absolute blur-2xl voca-font-orbitron"
          style={{
            color: '#ff0080',
            textShadow: '0 0 80px #ff0080'
          }}
        >
          VOCA
        </h1>
        <h1
          className="text-9xl font-black absolute blur-md voca-font-orbitron"
          style={{
            color: '#ff0080',
            textShadow: '0 0 40px #ff0080'
          }}
        >
          VOCA
        </h1>
        <h1
          className="text-9xl font-black relative voca-font-orbitron"
          style={{
            color: '#fff',
            textShadow: '0 0 10px #fff, 0 0 20px #ff0080, 0 0 40px #ff0080, 0 0 80px #ff0080'
          }}
        >
          VOCA
        </h1>

        {/* Subtitle */}
        <motion.p
          className="text-center mt-8 text-2xl tracking-widest voca-font-orbitron"
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
