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
// Maze Brain - White lines forming a brain-shaped maze
// ============================================
function MazeBrainAnimation({ phase }: { phase: number }) {
  // Maze paths forming brain shape
  const mazePaths = [
    // Outer brain shape
    "M60 5 L60 15 L45 15 L45 25 L30 25 L30 40 L20 40 L20 60 L25 60 L25 75 L35 75 L35 85 L50 85 L50 95 L70 95 L70 85 L85 85 L85 75 L95 75 L95 60 L100 60 L100 40 L90 40 L90 25 L75 25 L75 15 L60 15",
    // Inner maze lines - left side
    "M35 35 L35 45 L25 45 L25 55 L35 55 L35 65 L45 65 L45 55 L55 55",
    "M40 30 L40 40 L50 40 L50 50 L40 50 L40 60",
    "M30 50 L30 60 L40 60 L40 70 L50 70",
    // Inner maze lines - right side
    "M85 35 L85 45 L95 45 L95 55 L85 55 L85 65 L75 65 L75 55 L65 55",
    "M80 30 L80 40 L70 40 L70 50 L80 50 L80 60",
    "M90 50 L90 60 L80 60 L80 70 L70 70",
    // Center connection
    "M55 35 L55 45 L65 45 L65 35",
    "M55 60 L55 75 L65 75 L65 60",
    "M50 45 L70 45",
    "M50 70 L70 70",
  ];

  return (
    <motion.div
      className="absolute -top-36"
      initial={{ opacity: 0, scale: 0.8 }}
      animate={phase >= 1 ? { opacity: 1, scale: 1 } : {}}
      transition={{ duration: 0.5 }}
    >
      <svg width="120" height="100" viewBox="0 0 120 100">
        {/* Maze brain paths */}
        {mazePaths.map((path, index) => (
          <motion.path
            key={index}
            d={path}
            fill="none"
            stroke="white"
            strokeWidth="1.5"
            strokeLinecap="square"
            initial={{ pathLength: 0, opacity: 0 }}
            animate={phase >= 1 ? { pathLength: 1, opacity: 0.8 } : {}}
            transition={{
              duration: 0.8,
              delay: index * 0.1,
              ease: "easeOut"
            }}
          />
        ))}

        {/* Reverberating signal waves from brain */}
        {[0, 1, 2].map((i) => (
          <motion.rect
            key={`wave-${i}`}
            x="10"
            y="25"
            width="100"
            height="50"
            rx="25"
            fill="none"
            stroke="white"
            strokeWidth="1"
            initial={{ scale: 1, opacity: 0 }}
            animate={phase >= 2 ? {
              scale: [1, 1.3, 1.6],
              opacity: [0.5, 0.25, 0],
            } : {}}
            transition={{
              duration: 1.8,
              repeat: Infinity,
              delay: i * 0.5,
              ease: "easeOut"
            }}
          />
        ))}

        {/* Pulsing center node */}
        <motion.circle
          cx="60"
          cy="50"
          r="3"
          fill="white"
          initial={{ opacity: 0 }}
          animate={phase >= 1 ? {
            opacity: [0.5, 1, 0.5],
            scale: [1, 1.2, 1],
          } : {}}
          transition={{
            duration: 1,
            repeat: Infinity,
            ease: "easeInOut"
          }}
        />
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
        {/* Maze Brain animation */}
        <MazeBrainAnimation phase={phase} />

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
