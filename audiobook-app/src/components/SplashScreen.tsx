import { motion, AnimatePresence } from 'framer-motion';
import { useState, useEffect, useMemo } from 'react';
import { useStore, type SplashVariant } from '../store/useStore';

interface SplashScreenProps {
  onComplete: () => void;
}

const TAGLINE = "Audiobooks Reimagined. Resonate With Every Word.";

// Animated tagline component - letters appear with glitch effect
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
          className="inline-block"
          initial={{ opacity: 0, y: 20, rotateX: -90 }}
          animate={show ? {
            opacity: 1,
            y: 0,
            rotateX: 0,
          } : {}}
          transition={{
            duration: 0.05,
            delay: index * 0.025,
            ease: "easeOut"
          }}
        >
          {letter === ' ' ? '\u00A0' : letter}
        </motion.span>
      ))}
    </motion.p>
  );
}

// EXTREME Glitch text effect - THE TEXT ITSELF GLITCHES HEAVILY
function GlitchText({ children, className, intensity = 1, isGlitching = false }: { children: string; className?: string; intensity?: number; isGlitching?: boolean }) {
  const glitchAmount = isGlitching ? intensity * 8 : 2;

  return (
    <div className={`relative ${className}`}>
      {/* Main text - shakes and distorts during glitch */}
      <motion.span
        className="relative z-10 inline-block"
        animate={isGlitching ? {
          x: [0, -8, 12, -6, 4, -10, 0],
          y: [0, 4, -6, 3, -2, 5, 0],
          skewX: [0, 3, -5, 2, -3, 4, 0],
          scaleX: [1, 1.02, 0.97, 1.03, 0.98, 1],
        } : {
          x: [0, -1, 1, 0],
        }}
        transition={{
          duration: isGlitching ? 0.12 : 2,
          repeat: Infinity,
          ease: "linear"
        }}
      >
        {children}
      </motion.span>

      {/* RED channel - VERY visible offset */}
      <motion.span
        className="absolute inset-0 inline-block"
        style={{
          color: '#ff0040',
          mixBlendMode: 'screen',
        }}
        animate={{
          x: isGlitching
            ? [0, -glitchAmount * 2, glitchAmount * 1.5, -glitchAmount, glitchAmount * 2, 0]
            : [0, -3, 2, -1, 0],
          y: isGlitching ? [0, 3, -4, 2, -3, 0] : [0],
          opacity: isGlitching ? [0.8, 1, 0.6, 1, 0.7, 0.8] : [0.5, 0.3, 0.6, 0.5],
          skewX: isGlitching ? [0, 8, -6, 10, -4, 0] : [0],
        }}
        transition={{
          duration: isGlitching ? 0.1 : 0.4,
          repeat: Infinity,
          repeatDelay: isGlitching ? 0 : 1
        }}
      >
        {children}
      </motion.span>

      {/* CYAN/BLUE channel - opposite direction */}
      <motion.span
        className="absolute inset-0 inline-block"
        style={{
          color: '#00ffff',
          mixBlendMode: 'screen',
        }}
        animate={{
          x: isGlitching
            ? [0, glitchAmount * 2, -glitchAmount * 1.5, glitchAmount, -glitchAmount * 2, 0]
            : [0, 3, -2, 1, 0],
          y: isGlitching ? [0, -3, 4, -2, 3, 0] : [0],
          opacity: isGlitching ? [0.8, 0.6, 1, 0.7, 1, 0.8] : [0.5, 0.6, 0.3, 0.5],
          skewX: isGlitching ? [0, -8, 6, -10, 4, 0] : [0],
        }}
        transition={{
          duration: isGlitching ? 0.1 : 0.4,
          repeat: Infinity,
          repeatDelay: isGlitching ? 0 : 1,
          delay: 0.02
        }}
      >
        {children}
      </motion.span>

      {/* GREEN channel for extra chromatic split */}
      <motion.span
        className="absolute inset-0 inline-block"
        style={{
          color: '#00ff00',
          mixBlendMode: 'screen',
          opacity: isGlitching ? 0.6 : 0,
        }}
        animate={isGlitching ? {
          x: [0, glitchAmount, -glitchAmount * 1.5, glitchAmount * 0.5, 0],
          y: [0, -5, 3, -2, 0],
          opacity: [0.4, 0.7, 0.3, 0.6, 0.4],
        } : {}}
        transition={{ duration: 0.08, repeat: Infinity }}
      >
        {children}
      </motion.span>

      {/* Glitch "clone" that appears offset during heavy glitch */}
      {isGlitching && (
        <>
          <motion.span
            className="absolute inset-0 inline-block text-white/60"
            animate={{
              x: [20, -30, 15, -25, 20],
              y: [0, 5, -3, 2, 0],
              opacity: [0, 0.7, 0, 0.5, 0],
              scaleY: [1, 1.1, 0.9, 1.05, 1],
            }}
            transition={{ duration: 0.15, repeat: Infinity }}
          >
            {children}
          </motion.span>
          <motion.span
            className="absolute inset-0 inline-block text-pink-500/50"
            animate={{
              x: [-25, 35, -20, 30, -25],
              y: [0, -4, 2, -3, 0],
              opacity: [0, 0.6, 0, 0.4, 0],
            }}
            transition={{ duration: 0.12, repeat: Infinity, delay: 0.05 }}
          >
            {children}
          </motion.span>
        </>
      )}

      {/* Flickering white flash on text */}
      {isGlitching && (
        <motion.span
          className="absolute inset-0 inline-block text-white"
          animate={{
            opacity: [0, 0.8, 0, 0.5, 0, 0.9, 0, 0.3, 0],
          }}
          transition={{ duration: 0.2, repeat: Infinity }}
        >
          {children}
        </motion.span>
      )}
    </div>
  );
}

// Particle system
function Particles({ count = 50 }: { count?: number }) {
  const particles = useMemo(() =>
    Array.from({ length: count }, (_, i) => ({
      id: i,
      x: Math.random() * 100,
      y: Math.random() * 100,
      size: Math.random() * 3 + 1,
      duration: Math.random() * 3 + 2,
      delay: Math.random() * 2,
    })), [count]
  );

  return (
    <div className="absolute inset-0 overflow-hidden">
      {particles.map((p) => (
        <motion.div
          key={p.id}
          className="absolute rounded-full bg-white"
          style={{
            left: `${p.x}%`,
            top: `${p.y}%`,
            width: p.size,
            height: p.size,
          }}
          initial={{ opacity: 0, scale: 0 }}
          animate={{
            opacity: [0, 0.8, 0],
            scale: [0, 1, 0],
            y: [0, -100],
          }}
          transition={{
            duration: p.duration,
            repeat: Infinity,
            delay: p.delay,
            ease: "easeOut",
          }}
        />
      ))}
    </div>
  );
}

// Scanlines overlay
function Scanlines() {
  return (
    <div
      className="absolute inset-0 pointer-events-none opacity-[0.03]"
      style={{
        backgroundImage: 'repeating-linear-gradient(0deg, transparent, transparent 2px, rgba(255,255,255,0.1) 2px, rgba(255,255,255,0.1) 4px)',
      }}
    />
  );
}

// Noise overlay
function Noise() {
  return (
    <motion.div
      className="absolute inset-0 pointer-events-none opacity-[0.05]"
      style={{
        backgroundImage: `url("data:image/svg+xml,%3Csvg viewBox='0 0 256 256' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noise'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noise)'/%3E%3C/svg%3E")`,
      }}
      animate={{ opacity: [0.03, 0.06, 0.03] }}
      transition={{ duration: 0.1, repeat: Infinity }}
    />
  );
}

// ============================================
// OPTION 1: Pulse Wave - Clean with subtle effects
// ============================================
function PulseWaveSplash({ onComplete }: SplashScreenProps) {
  const [phase, setPhase] = useState(0);

  useEffect(() => {
    const timings = [300, 800, 800, 1500];
    const timer = setTimeout(() => {
      if (phase < 3) setPhase(phase + 1);
      else onComplete();
    }, timings[phase]);
    return () => clearTimeout(timer);
  }, [phase, onComplete]);

  return (
    <motion.div
      initial={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      transition={{ duration: 0.5 }}
      className="fixed inset-0 z-[100] bg-[#030303] flex items-center justify-center overflow-hidden"
    >
      <Particles count={30} />
      <Scanlines />

      {/* Radial pulse rings */}
      {phase >= 1 && [...Array(3)].map((_, i) => (
        <motion.div
          key={i}
          className="absolute w-96 h-96 rounded-full border border-primary-500/20"
          initial={{ scale: 0.5, opacity: 0 }}
          animate={{ scale: [0.5, 2.5], opacity: [0.6, 0] }}
          transition={{ duration: 2, repeat: Infinity, delay: i * 0.6 }}
        />
      ))}

      <div className="relative z-10 flex flex-col items-center">
        <motion.div
          initial={{ opacity: 0, scale: 0.8 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.8, type: "spring" }}
        >
          <h1 className="text-8xl font-bold tracking-wider rezon-font-orbitron text-white">
            REZON
          </h1>
        </motion.div>

        <motion.div
          className="h-1 w-48 mt-6 rounded-full overflow-hidden bg-white/10"
          initial={{ scaleX: 0 }}
          animate={phase >= 1 ? { scaleX: 1 } : {}}
          transition={{ duration: 0.8 }}
        >
          <motion.div
            className="h-full w-full bg-gradient-to-r from-primary-400 to-primary-600"
            animate={{ x: ['-100%', '100%'] }}
            transition={{ duration: 1.5, repeat: Infinity }}
          />
        </motion.div>

        <div className="mt-8">
          <AnimatedTagline
            show={phase >= 2}
            className="text-sm tracking-[0.2em] uppercase text-white/50"
          />
        </div>
      </div>
    </motion.div>
  );
}

// ============================================
// OPTION 2: Glitch Cyber - EXTREME TV STATION glitch effects
// ============================================
function GlitchCyberSplash({ onComplete }: SplashScreenProps) {
  const [phase, setPhase] = useState(0);
  const [glitchActive, setGlitchActive] = useState(false);
  const [glitchIntensity, setGlitchIntensity] = useState(1);
  const [staticNoise, setStaticNoise] = useState(false);

  useEffect(() => {
    const timings = [200, 600, 800, 1800];
    const timer = setTimeout(() => {
      if (phase < 3) setPhase(phase + 1);
      else onComplete();
    }, timings[phase]);
    return () => clearTimeout(timer);
  }, [phase, onComplete]);

  // Frequent heavy glitch bursts - TV station style
  useEffect(() => {
    const glitchInterval = setInterval(() => {
      // More frequent glitches with varying intensity
      const intensity = 1 + Math.random() * 2;
      setGlitchIntensity(intensity);
      setGlitchActive(true);

      // Random duration for each glitch
      const duration = 100 + Math.random() * 200;
      setTimeout(() => setGlitchActive(false), duration);
    }, 300 + Math.random() * 700); // More frequent: 300-1000ms between glitches

    return () => clearInterval(glitchInterval);
  }, []);

  // Static noise bursts
  useEffect(() => {
    const noiseInterval = setInterval(() => {
      setStaticNoise(true);
      setTimeout(() => setStaticNoise(false), 50 + Math.random() * 100);
    }, 1500 + Math.random() * 2000);

    return () => clearInterval(noiseInterval);
  }, []);

  return (
    <motion.div
      initial={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      transition={{ duration: 0.3 }}
      className="fixed inset-0 z-[100] bg-black flex items-center justify-center overflow-hidden"
    >
      {/* Heavy TV static noise overlay */}
      <motion.div
        className="absolute inset-0 pointer-events-none z-50"
        style={{
          backgroundImage: `url("data:image/svg+xml,%3Csvg viewBox='0 0 256 256' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noise'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noise)'/%3E%3C/svg%3E")`,
        }}
        animate={{
          opacity: staticNoise ? [0.15, 0.25, 0.1, 0.2, 0.15] : (glitchActive ? [0.05, 0.1, 0.05] : 0.03),
        }}
        transition={{ duration: 0.1, repeat: Infinity }}
      />

      {/* Heavy scanlines */}
      <div
        className="absolute inset-0 pointer-events-none z-40"
        style={{
          backgroundImage: 'repeating-linear-gradient(0deg, transparent, transparent 2px, rgba(0,0,0,0.3) 2px, rgba(0,0,0,0.3) 4px)',
          opacity: glitchActive ? 0.4 : 0.15,
        }}
      />

      <Particles count={60} />

      {/* Glitch screen shake - more intense */}
      <motion.div
        className="absolute inset-0"
        animate={glitchActive ? {
          x: [0, -10 * glitchIntensity, 8 * glitchIntensity, -6 * glitchIntensity, 4 * glitchIntensity, 0],
          y: [0, 5 * glitchIntensity, -4 * glitchIntensity, 3 * glitchIntensity, -2 * glitchIntensity, 0],
          skewX: [0, 1, -1, 0.5, 0],
        } : {}}
        transition={{ duration: 0.15 }}
      >
        {/* Neon grid floor */}
        <div className="absolute inset-x-0 bottom-0 h-1/2 perspective-[500px]">
          <motion.div
            className="absolute inset-0 origin-bottom"
            style={{
              transform: 'rotateX(60deg)',
              backgroundImage: `
                linear-gradient(to bottom, transparent 0%, rgba(6,182,212,0.1) 100%),
                linear-gradient(90deg, rgba(6,182,212,0.3) 1px, transparent 1px),
                linear-gradient(0deg, rgba(6,182,212,0.3) 1px, transparent 1px)
              `,
              backgroundSize: '100% 100%, 40px 40px, 40px 40px',
            }}
            animate={{
              backgroundPosition: ['0 0', '0 40px'],
              opacity: glitchActive ? [1, 0.5, 1] : 1,
            }}
            transition={{ duration: 1, repeat: Infinity, ease: "linear" }}
          />
        </div>

        {/* Horizontal glitch bars - more and heavier */}
        <AnimatePresence>
          {glitchActive && [...Array(15)].map((_, i) => (
            <motion.div
              key={i}
              className="absolute left-0 right-0"
              style={{
                top: `${Math.random() * 100}%`,
                height: `${Math.random() * 30 + 5}px`,
                background: i % 3 === 0
                  ? 'rgba(255,0,100,0.4)'
                  : i % 3 === 1
                  ? 'rgba(0,255,255,0.4)'
                  : 'rgba(255,255,255,0.3)',
                transform: `translateX(${(Math.random() - 0.5) * 20}px)`,
              }}
              initial={{ scaleX: 0, opacity: 0 }}
              animate={{ scaleX: [0, 1, 0.5, 1], opacity: [0, 1, 0.5, 0] }}
              exit={{ opacity: 0 }}
              transition={{ duration: 0.15 }}
            />
          ))}
        </AnimatePresence>

        {/* RGB split flash - more intense */}
        {glitchActive && (
          <>
            <motion.div
              className="absolute inset-0 bg-red-500"
              style={{ mixBlendMode: 'screen' }}
              animate={{
                opacity: [0.1, 0.2, 0.05, 0.15, 0.1],
                x: [-10 * glitchIntensity, 5 * glitchIntensity, -8 * glitchIntensity, 0],
              }}
              transition={{ duration: 0.1 }}
            />
            <motion.div
              className="absolute inset-0 bg-cyan-500"
              style={{ mixBlendMode: 'screen' }}
              animate={{
                opacity: [0.1, 0.15, 0.2, 0.1, 0.15],
                x: [10 * glitchIntensity, -5 * glitchIntensity, 8 * glitchIntensity, 0],
              }}
              transition={{ duration: 0.1 }}
            />
            <motion.div
              className="absolute inset-0 bg-green-500"
              style={{ mixBlendMode: 'screen' }}
              animate={{
                opacity: [0.05, 0.1, 0.05],
                y: [5 * glitchIntensity, -5 * glitchIntensity, 0],
              }}
              transition={{ duration: 0.1 }}
            />
          </>
        )}

        {/* White flash burst during heavy glitch */}
        {glitchActive && glitchIntensity > 2 && (
          <motion.div
            className="absolute inset-0 bg-white"
            initial={{ opacity: 0 }}
            animate={{ opacity: [0, 0.3, 0] }}
            transition={{ duration: 0.1 }}
          />
        )}
      </motion.div>

      {/* Main content */}
      <div className="relative z-10 flex flex-col items-center">
        {/* Logo with EXTREME TV station glitch */}
        <motion.div
          initial={{ opacity: 0, scale: 1.5, filter: 'blur(20px)' }}
          animate={{ opacity: 1, scale: 1, filter: 'blur(0px)' }}
          transition={{ duration: 0.5, type: "spring" }}
          className="relative"
        >
          <GlitchText
            className="text-9xl font-black rezon-font-orbitron text-white"
            intensity={glitchIntensity}
            isGlitching={glitchActive}
          >
            REZON
          </GlitchText>

          {/* Neon glow - pulses more during glitch */}
          <motion.div
            className="absolute inset-0 text-9xl font-black rezon-font-orbitron blur-xl"
            style={{ color: '#06b6d4' }}
            animate={{
              opacity: glitchActive ? [0.3, 0.8, 0.2, 0.7, 0.3] : [0.3, 0.6, 0.3],
              scale: glitchActive ? [1, 1.02, 0.98, 1] : 1,
            }}
            transition={{ duration: glitchActive ? 0.15 : 2, repeat: Infinity }}
          >
            REZON
          </motion.div>

          {/* Multiple glitch slice effects - TV tearing */}
          {glitchActive && (
            <>
              {/* Top slice */}
              <motion.div
                className="absolute overflow-hidden"
                style={{
                  top: '10%',
                  left: 0,
                  right: 0,
                  height: '15%',
                }}
                animate={{
                  x: [0, 15 * glitchIntensity, -10 * glitchIntensity, 0],
                }}
                transition={{ duration: 0.1 }}
              >
                <div
                  className="text-9xl font-black rezon-font-orbitron text-pink-400"
                  style={{ marginTop: '-10%' }}
                >
                  REZON
                </div>
              </motion.div>

              {/* Middle slice */}
              <motion.div
                className="absolute overflow-hidden"
                style={{
                  top: '35%',
                  left: 0,
                  right: 0,
                  height: '20%',
                }}
                animate={{
                  x: [0, -20 * glitchIntensity, 12 * glitchIntensity, 0],
                }}
                transition={{ duration: 0.12 }}
              >
                <div
                  className="text-9xl font-black rezon-font-orbitron text-cyan-400"
                  style={{ marginTop: '-35%' }}
                >
                  REZON
                </div>
              </motion.div>

              {/* Bottom slice */}
              <motion.div
                className="absolute overflow-hidden"
                style={{
                  top: '65%',
                  left: 0,
                  right: 0,
                  height: '20%',
                }}
                animate={{
                  x: [0, 18 * glitchIntensity, -8 * glitchIntensity, 0],
                }}
                transition={{ duration: 0.08 }}
              >
                <div
                  className="text-9xl font-black rezon-font-orbitron text-yellow-400"
                  style={{ marginTop: '-65%' }}
                >
                  REZON
                </div>
              </motion.div>
            </>
          )}

          {/* Static noise overlay on text during glitch */}
          {(glitchActive || staticNoise) && (
            <motion.div
              className="absolute inset-0 mix-blend-overlay"
              style={{
                backgroundImage: `url("data:image/svg+xml,%3Csvg viewBox='0 0 100 100' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.8'/%3E%3C/filter%3E%3Crect width='100' height='100' filter='url(%23n)'/%3E%3C/svg%3E")`,
              }}
              animate={{ opacity: [0.2, 0.4, 0.2, 0.5, 0.2] }}
              transition={{ duration: 0.1, repeat: Infinity }}
            />
          )}
        </motion.div>

        {/* Cyber loading bar */}
        <motion.div
          className="mt-8 w-64 h-2 bg-white/5 rounded overflow-hidden border border-cyan-500/30"
          initial={{ opacity: 0, y: 20 }}
          animate={phase >= 1 ? { opacity: 1, y: 0 } : {}}
        >
          <motion.div
            className="h-full bg-gradient-to-r from-cyan-500 via-purple-500 to-pink-500"
            initial={{ x: '-100%' }}
            animate={phase >= 1 ? { x: '0%' } : {}}
            transition={{ duration: 1, ease: "easeOut" }}
          />
          {/* Shimmer */}
          <motion.div
            className="absolute inset-0 bg-gradient-to-r from-transparent via-white/30 to-transparent"
            animate={{ x: ['-100%', '200%'] }}
            transition={{ duration: 1.5, repeat: Infinity, delay: 1 }}
          />
        </motion.div>

        {/* Status text */}
        <motion.div
          className="mt-4 font-mono text-xs text-cyan-500/70 tracking-wider"
          initial={{ opacity: 0 }}
          animate={phase >= 1 ? { opacity: 1 } : {}}
        >
          <motion.span
            animate={{ opacity: [1, 0, 1] }}
            transition={{ duration: 0.5, repeat: Infinity }}
          >
            ▶
          </motion.span>
          {' INITIALIZING AUDIO CORE...'}
        </motion.div>

        {/* Animated tagline */}
        <div className="mt-6">
          <AnimatedTagline
            show={phase >= 2}
            className="text-lg tracking-[0.15em] rezon-font-orbitron bg-clip-text text-transparent"
            style={{
              backgroundImage: 'linear-gradient(90deg, #06b6d4, #ec4899, #06b6d4)',
              backgroundSize: '200% 100%',
            }}
          />
        </div>

        {/* Corner brackets */}
        <motion.div
          className="absolute -top-20 -left-20 w-16 h-16 border-l-2 border-t-2 border-cyan-500/50"
          initial={{ opacity: 0, scale: 0.5 }}
          animate={phase >= 1 ? { opacity: 1, scale: 1 } : {}}
        />
        <motion.div
          className="absolute -bottom-20 -right-20 w-16 h-16 border-r-2 border-b-2 border-pink-500/50"
          initial={{ opacity: 0, scale: 0.5 }}
          animate={phase >= 1 ? { opacity: 1, scale: 1 } : {}}
        />
      </div>

      {/* Side data streams */}
      <div className="absolute left-4 top-1/4 bottom-1/4 w-px overflow-hidden">
        <motion.div
          className="w-full h-20 bg-gradient-to-b from-transparent via-cyan-500 to-transparent"
          animate={{ y: ['-100%', '500%'] }}
          transition={{ duration: 2, repeat: Infinity, ease: "linear" }}
        />
      </div>
      <div className="absolute right-4 top-1/4 bottom-1/4 w-px overflow-hidden">
        <motion.div
          className="w-full h-20 bg-gradient-to-b from-transparent via-pink-500 to-transparent"
          animate={{ y: ['500%', '-100%'] }}
          transition={{ duration: 2, repeat: Infinity, ease: "linear" }}
        />
      </div>
    </motion.div>
  );
}

// ============================================
// OPTION 3: Waveform Morph - Audio visualization
// ============================================
function WaveformMorphSplash({ onComplete }: SplashScreenProps) {
  const [phase, setPhase] = useState(0);

  useEffect(() => {
    const timings = [200, 700, 900, 1600];
    const timer = setTimeout(() => {
      if (phase < 3) setPhase(phase + 1);
      else onComplete();
    }, timings[phase]);
    return () => clearTimeout(timer);
  }, [phase, onComplete]);

  return (
    <motion.div
      initial={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      transition={{ duration: 0.5 }}
      className="fixed inset-0 z-[100] bg-gradient-to-b from-[#0f0f23] to-[#000011] flex items-center justify-center overflow-hidden"
    >
      <Particles count={40} />

      {/* Animated waveform bars background */}
      <div className="absolute inset-0 flex items-center justify-center gap-[2px] opacity-30">
        {[...Array(80)].map((_, i) => (
          <motion.div
            key={i}
            className="w-1 rounded-full"
            style={{
              background: `linear-gradient(to top, #06b6d4, #8b5cf6)`,
            }}
            initial={{ height: 20 }}
            animate={phase >= 1 ? {
              height: [
                20,
                40 + Math.sin(i * 0.2) * 80 + Math.random() * 40,
                20 + Math.cos(i * 0.3) * 30,
                40 + Math.sin(i * 0.25) * 60,
                20,
              ],
            } : {}}
            transition={{
              duration: 2,
              repeat: Infinity,
              delay: i * 0.02,
              ease: "easeInOut",
            }}
          />
        ))}
      </div>

      {/* Circular pulse rings */}
      {phase >= 1 && [...Array(4)].map((_, i) => (
        <motion.div
          key={i}
          className="absolute rounded-full"
          style={{
            width: 200 + i * 100,
            height: 200 + i * 100,
            border: '1px solid',
            borderColor: i % 2 === 0 ? 'rgba(6,182,212,0.3)' : 'rgba(139,92,246,0.3)',
          }}
          initial={{ scale: 0.5, opacity: 0 }}
          animate={{
            scale: [0.8, 1.2, 0.8],
            opacity: [0.3, 0.6, 0.3],
            rotate: [0, 180, 360],
          }}
          transition={{
            duration: 4 + i,
            repeat: Infinity,
            delay: i * 0.3,
          }}
        />
      ))}

      <div className="relative z-10 flex flex-col items-center">
        <motion.div
          initial={{ scale: 0, opacity: 0, rotate: -10 }}
          animate={{ scale: 1, opacity: 1, rotate: 0 }}
          transition={{ type: "spring", duration: 0.8 }}
        >
          <h1
            className="text-8xl font-bold rezon-font-orbitron"
            style={{
              background: 'linear-gradient(135deg, #06b6d4 0%, #8b5cf6 50%, #ec4899 100%)',
              WebkitBackgroundClip: 'text',
              WebkitTextFillColor: 'transparent',
              filter: 'drop-shadow(0 0 60px rgba(139,92,246,0.5))',
            }}
          >
            REZON
          </h1>
        </motion.div>

        {/* Animated waveform SVG */}
        <motion.svg
          width="300"
          height="60"
          viewBox="0 0 300 60"
          className="mt-6"
          initial={{ opacity: 0 }}
          animate={phase >= 1 ? { opacity: 1 } : {}}
        >
          <defs>
            <linearGradient id="waveGrad" x1="0%" y1="0%" x2="100%" y2="0%">
              <stop offset="0%" stopColor="#06b6d4" />
              <stop offset="50%" stopColor="#8b5cf6" />
              <stop offset="100%" stopColor="#ec4899" />
            </linearGradient>
          </defs>
          <motion.path
            d="M0,30 Q37.5,10 75,30 T150,30 T225,30 T300,30"
            fill="none"
            stroke="url(#waveGrad)"
            strokeWidth="3"
            strokeLinecap="round"
            initial={{ pathLength: 0 }}
            animate={phase >= 1 ? {
              pathLength: 1,
              d: [
                "M0,30 Q37.5,10 75,30 T150,30 T225,30 T300,30",
                "M0,30 Q37.5,50 75,30 T150,30 T225,30 T300,30",
                "M0,30 Q37.5,10 75,30 T150,30 T225,30 T300,30",
              ]
            } : {}}
            transition={{
              pathLength: { duration: 1 },
              d: { duration: 2, repeat: Infinity, ease: "easeInOut" }
            }}
          />
        </motion.svg>

        <div className="mt-6">
          <AnimatedTagline
            show={phase >= 2}
            className="text-lg tracking-[0.1em] text-violet-300/80 rezon-font-orbitron"
          />
        </div>
      </div>
    </motion.div>
  );
}

// ============================================
// OPTION 4: Neon Flicker - Retro neon aesthetic
// ============================================
function NeonFlickerSplash({ onComplete }: SplashScreenProps) {
  const [phase, setPhase] = useState(0);
  const [flicker, setFlicker] = useState(false);

  useEffect(() => {
    const timings = [300, 700, 900, 1600];
    const timer = setTimeout(() => {
      if (phase < 3) setPhase(phase + 1);
      else onComplete();
    }, timings[phase]);
    return () => clearTimeout(timer);
  }, [phase, onComplete]);

  // Neon flicker effect
  useEffect(() => {
    const flickerInterval = setInterval(() => {
      setFlicker(true);
      setTimeout(() => setFlicker(false), 50 + Math.random() * 100);
    }, 2000 + Math.random() * 3000);
    return () => clearInterval(flickerInterval);
  }, []);

  return (
    <motion.div
      initial={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      transition={{ duration: 0.5 }}
      className="fixed inset-0 z-[100] bg-[#0a0a0a] flex items-center justify-center overflow-hidden"
    >
      <Scanlines />
      <Noise />

      {/* Ambient glow */}
      <motion.div
        className="absolute w-[600px] h-[600px] rounded-full"
        style={{
          background: 'radial-gradient(circle, rgba(236,72,153,0.15) 0%, transparent 70%)',
          filter: 'blur(60px)',
        }}
        animate={{
          scale: [1, 1.2, 1],
          opacity: flicker ? 0.3 : [0.4, 0.6, 0.4],
        }}
        transition={{ duration: 3, repeat: Infinity }}
      />

      <div className="relative z-10 flex flex-col items-center">
        {/* Neon sign effect */}
        <motion.div
          initial={{ opacity: 0, scale: 0.9 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.5 }}
          className="relative"
        >
          <h1
            className="text-9xl font-black rezon-font-orbitron"
            style={{
              color: flicker ? 'rgba(236,72,153,0.3)' : '#ec4899',
              textShadow: flicker ? 'none' : `
                0 0 10px #ec4899,
                0 0 20px #ec4899,
                0 0 40px #ec4899,
                0 0 80px #ec4899,
                0 0 120px rgba(236,72,153,0.5)
              `,
              transition: 'all 0.05s',
            }}
          >
            REZON
          </h1>

          {/* Double stroke effect */}
          <div
            className="absolute inset-0 text-9xl font-black rezon-font-orbitron"
            style={{
              WebkitTextStroke: '2px rgba(6,182,212,0.5)',
              color: 'transparent',
              transform: 'translate(2px, 2px)',
            }}
          >
            REZON
          </div>
        </motion.div>

        {/* Neon underline */}
        <motion.div
          className="h-1 w-64 mt-8 rounded-full"
          style={{
            background: flicker ? 'rgba(6,182,212,0.2)' : '#06b6d4',
            boxShadow: flicker ? 'none' : '0 0 20px #06b6d4, 0 0 40px #06b6d4',
          }}
          initial={{ scaleX: 0 }}
          animate={phase >= 1 ? { scaleX: 1 } : {}}
          transition={{ duration: 0.8 }}
        />

        <div className="mt-8">
          <AnimatedTagline
            show={phase >= 2}
            className="text-lg tracking-[0.15em] text-pink-400/80 rezon-font-orbitron"
            style={{
              textShadow: '0 0 10px rgba(236,72,153,0.5)',
            }}
          />
        </div>

        {/* Decorative elements */}
        <motion.div
          className="flex gap-4 mt-8"
          initial={{ opacity: 0 }}
          animate={phase >= 2 ? { opacity: 1 } : {}}
        >
          {[...Array(5)].map((_, i) => (
            <motion.div
              key={i}
              className="w-2 h-2 rounded-full bg-cyan-400"
              style={{
                boxShadow: '0 0 10px #06b6d4',
              }}
              animate={{
                opacity: [0.5, 1, 0.5],
                scale: [1, 1.2, 1],
              }}
              transition={{
                duration: 1,
                repeat: Infinity,
                delay: i * 0.2,
              }}
            />
          ))}
        </motion.div>
      </div>
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
