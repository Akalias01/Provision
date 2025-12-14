import { motion } from 'framer-motion';
import { useStore, colorThemes } from '../../store/useStore';

interface RezonLogoProps {
  size?: 'sm' | 'md' | 'lg' | 'xl';
  animated?: boolean;
  variant?: 'waveform' | 'headphones' | 'pulse' | 'minimal';
}

// ============================================
// LOGO OPTION 1: Waveform (Modern, Clean)
// ============================================
function WaveformLogo({ iconSize, animated }: { iconSize: number; animated: boolean }) {
  return (
    <motion.div
      whileHover={animated ? { scale: 1.05 } : undefined}
      className="relative"
    >
      <svg width={iconSize} height={iconSize} viewBox="0 0 100 100">
        <defs>
          <linearGradient id="waveGrad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="var(--logo-start, #06b6d4)" />
            <stop offset="100%" stopColor="var(--logo-end, #0891b2)" />
          </linearGradient>
        </defs>

        {/* Circle background */}
        <circle cx="50" cy="50" r="45" fill="url(#waveGrad)" />

        {/* Sound wave bars */}
        {[0, 1, 2, 3, 4].map((i) => {
          const heights = [24, 36, 48, 36, 24];
          const height = heights[i];
          const x = 20 + i * 15;
          return (
            <motion.rect
              key={i}
              x={x}
              y={50 - height / 2}
              width="6"
              height={height}
              rx="3"
              fill="white"
              animate={animated ? {
                height: [height, height * 0.5, height],
                y: [50 - height / 2, 50 - (height * 0.5) / 2, 50 - height / 2],
              } : {}}
              transition={{
                duration: 0.8,
                repeat: Infinity,
                delay: i * 0.1,
                ease: "easeInOut"
              }}
            />
          );
        })}
      </svg>
    </motion.div>
  );
}

// ============================================
// LOGO OPTION 2: Headphones (Audio-focused)
// ============================================
function HeadphonesLogo({ iconSize, animated }: { iconSize: number; animated: boolean }) {
  return (
    <motion.div
      whileHover={animated ? { scale: 1.05, rotate: 5 } : undefined}
      className="relative"
    >
      <svg width={iconSize} height={iconSize} viewBox="0 0 100 100">
        <defs>
          <linearGradient id="headGrad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="var(--logo-start, #06b6d4)" />
            <stop offset="100%" stopColor="var(--logo-end, #0891b2)" />
          </linearGradient>
        </defs>

        {/* Circle background */}
        <circle cx="50" cy="50" r="45" fill="url(#headGrad)" />

        {/* Headphone arc */}
        <path
          d="M25 55 Q25 30 50 25 Q75 30 75 55"
          fill="none"
          stroke="white"
          strokeWidth="5"
          strokeLinecap="round"
        />

        {/* Left ear cup */}
        <rect x="18" y="50" width="12" height="22" rx="4" fill="white" />

        {/* Right ear cup */}
        <rect x="70" y="50" width="12" height="22" rx="4" fill="white" />

        {/* Sound waves from right cup */}
        {animated && (
          <>
            <motion.path
              d="M85 55 Q90 61 85 67"
              fill="none"
              stroke="white"
              strokeWidth="2"
              strokeLinecap="round"
              initial={{ opacity: 0, pathLength: 0 }}
              animate={{ opacity: [0, 0.8, 0], pathLength: [0, 1, 1] }}
              transition={{ duration: 1.5, repeat: Infinity }}
            />
            <motion.path
              d="M90 52 Q97 61 90 70"
              fill="none"
              stroke="white"
              strokeWidth="2"
              strokeLinecap="round"
              initial={{ opacity: 0, pathLength: 0 }}
              animate={{ opacity: [0, 0.5, 0], pathLength: [0, 1, 1] }}
              transition={{ duration: 1.5, repeat: Infinity, delay: 0.2 }}
            />
          </>
        )}
      </svg>
    </motion.div>
  );
}

// ============================================
// LOGO OPTION 3: Pulse Circle (Energetic)
// ============================================
function PulseLogo({ iconSize, animated }: { iconSize: number; animated: boolean }) {
  return (
    <motion.div
      whileHover={animated ? { scale: 1.05 } : undefined}
      className="relative"
    >
      <svg width={iconSize} height={iconSize} viewBox="0 0 100 100">
        <defs>
          <linearGradient id="pulseGrad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="var(--logo-start, #06b6d4)" />
            <stop offset="100%" stopColor="var(--logo-end, #0891b2)" />
          </linearGradient>
        </defs>

        {/* Outer pulse rings */}
        {animated && [0, 1].map((i) => (
          <motion.circle
            key={i}
            cx="50"
            cy="50"
            r="35"
            fill="none"
            stroke="url(#pulseGrad)"
            strokeWidth="2"
            initial={{ r: 35, opacity: 0.8 }}
            animate={{ r: 50, opacity: 0 }}
            transition={{
              duration: 1.5,
              repeat: Infinity,
              delay: i * 0.5,
            }}
          />
        ))}

        {/* Main circle */}
        <circle cx="50" cy="50" r="35" fill="url(#pulseGrad)" />

        {/* Play button / V shape */}
        <path
          d="M40 35 L40 65 L65 50 Z"
          fill="white"
        />
      </svg>
    </motion.div>
  );
}

// ============================================
// LOGO OPTION 4: Minimal R (Ultra Clean)
// ============================================
function MinimalLogo({ iconSize, animated }: { iconSize: number; animated: boolean }) {
  return (
    <motion.div
      whileHover={animated ? { scale: 1.05 } : undefined}
      className="relative"
    >
      <svg width={iconSize} height={iconSize} viewBox="0 0 100 100">
        <defs>
          <linearGradient id="minGrad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="var(--logo-start, #06b6d4)" />
            <stop offset="100%" stopColor="var(--logo-end, #0891b2)" />
          </linearGradient>
        </defs>

        {/* Circle background */}
        <circle cx="50" cy="50" r="45" fill="url(#minGrad)" />

        {/* Stylized R with wave */}
        <motion.path
          d="M35 70 L35 30 Q35 25 40 25 L55 25 Q70 25 70 40 Q70 50 55 50 L45 50 L65 70"
          fill="none"
          stroke="white"
          strokeWidth="6"
          strokeLinecap="round"
          strokeLinejoin="round"
          animate={animated ? {
            d: [
              "M35 70 L35 30 Q35 25 40 25 L55 25 Q70 25 70 40 Q70 50 55 50 L45 50 L65 70",
              "M35 68 L35 32 Q35 27 40 27 L55 27 Q68 27 68 40 Q68 50 55 50 L45 50 L63 68",
              "M35 70 L35 30 Q35 25 40 25 L55 25 Q70 25 70 40 Q70 50 55 50 L45 50 L65 70",
            ]
          } : {}}
          transition={{ duration: 2, repeat: Infinity, ease: "easeInOut" }}
        />

        {/* Sound dot */}
        <motion.circle
          cx="52"
          cy="40"
          r="4"
          fill="white"
          animate={animated ? { scale: [1, 1.3, 1], opacity: [1, 0.7, 1] } : {}}
          transition={{ duration: 1, repeat: Infinity }}
        />
      </svg>
    </motion.div>
  );
}

export function RezonLogo({ size = 'md', animated = true, variant = 'waveform' }: RezonLogoProps) {
  const colorTheme = useStore((state) => state.colorTheme);
  const themeConfig = colorThemes[colorTheme];
  const isMultiColor = themeConfig?.isMultiColor;

  const sizes = {
    sm: { icon: 32, text: 'text-lg' },
    md: { icon: 40, text: 'text-xl' },
    lg: { icon: 56, text: 'text-3xl' },
    xl: { icon: 72, text: 'text-4xl' },
  };

  const { icon, text } = sizes[size];

  const LogoComponent = {
    waveform: WaveformLogo,
    headphones: HeadphonesLogo,
    pulse: PulseLogo,
    minimal: MinimalLogo,
  }[variant];

  // Multi-color gradient for cyber-glitch theme
  const gradientStyle = isMultiColor
    ? `linear-gradient(135deg, ${themeConfig.logoStart}, ${themeConfig.logoMid}, ${themeConfig.logoEnd})`
    : 'linear-gradient(135deg, var(--primary-400, #22d3ee), var(--primary-500, #06b6d4), var(--primary-600, #0891b2))';

  return (
    <div className="flex items-center gap-3">
      <LogoComponent iconSize={icon} animated={animated} />

      {/* REZON Text - Using Orbitron font for a techy/cool look */}
      <motion.h1
        className={`${text} font-bold tracking-widest rezon-font-orbitron`}
        whileHover={animated ? { scale: 1.02 } : undefined}
      >
        <span
          className="bg-clip-text text-transparent"
          style={{
            backgroundImage: gradientStyle,
          }}
        >
          REZON
        </span>
      </motion.h1>
    </div>
  );
}

// Also export as VocaLogo for backwards compatibility during transition
export { RezonLogo as VocaLogo };
