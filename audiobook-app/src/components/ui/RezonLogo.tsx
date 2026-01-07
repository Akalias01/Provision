import { motion } from 'framer-motion';
import { useStore, colorThemes } from '../../store/useStore';

interface RezonLogoProps {
  size?: 'sm' | 'md' | 'lg' | 'xl';
  animated?: boolean;
  variant?: 'waveform' | 'headphones' | 'pulse' | 'minimal';
}

// ============================================
// LOGO OPTION 1: R with Soundwave Bars (Default)
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
          <clipPath id="rClip">
            <path d="M25 80 L25 20 L55 20 Q75 20 75 38 Q75 52 60 54 L75 80 L60 80 L47 56 L40 56 L40 80 Z M40 32 L40 46 L52 46 Q60 46 60 39 Q60 32 52 32 Z" />
          </clipPath>
        </defs>

        {/* Square background with rounded corners */}
        <rect x="5" y="5" width="90" height="90" rx="18" fill="url(#waveGrad)" />

        {/* R shape as container for soundwaves */}
        <g clipPath="url(#rClip)">
          {/* Background of R */}
          <rect x="20" y="15" width="60" height="70" fill="white" fillOpacity="0.3" />

          {/* Animated soundwave bars inside R */}
          {[0, 1, 2, 3, 4, 5, 6, 7].map((i) => {
            const heights = [35, 50, 65, 45, 55, 40, 60, 30];
            const height = heights[i];
            const x = 22 + i * 7;
            return (
              <motion.rect
                key={i}
                x={x}
                y={50 - height / 2}
                width="5"
                height={height}
                fill="white"
                animate={animated ? {
                  height: [height, height * 0.4, height * 0.7, height],
                  y: [50 - height / 2, 50 - (height * 0.4) / 2, 50 - (height * 0.7) / 2, 50 - height / 2],
                } : {}}
                transition={{
                  duration: 1.2,
                  repeat: Infinity,
                  delay: i * 0.1,
                  ease: "easeInOut"
                }}
              />
            );
          })}
        </g>

        {/* R outline for definition */}
        <path
          d="M25 80 L25 20 L55 20 Q75 20 75 38 Q75 52 60 54 L75 80 L60 80 L47 56 L40 56 L40 80 Z M40 32 L40 46 L52 46 Q60 46 60 39 Q60 32 52 32 Z"
          fill="none"
          stroke="white"
          strokeWidth="2"
          strokeOpacity="0.5"
        />
      </svg>
    </motion.div>
  );
}

// ============================================
// LOGO OPTION 2: Neon R with Glow Effect
// ============================================
function HeadphonesLogo({ iconSize, animated }: { iconSize: number; animated: boolean }) {
  return (
    <motion.div
      whileHover={animated ? { scale: 1.05 } : undefined}
      className="relative"
    >
      <svg width={iconSize} height={iconSize} viewBox="0 0 100 100">
        <defs>
          <linearGradient id="neonGrad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="var(--logo-start, #06b6d4)" />
            <stop offset="100%" stopColor="var(--logo-end, #0891b2)" />
          </linearGradient>
          <filter id="glow" x="-50%" y="-50%" width="200%" height="200%">
            <feGaussianBlur stdDeviation="3" result="coloredBlur"/>
            <feMerge>
              <feMergeNode in="coloredBlur"/>
              <feMergeNode in="SourceGraphic"/>
            </feMerge>
          </filter>
        </defs>

        {/* Dark square background */}
        <rect x="5" y="5" width="90" height="90" rx="18" fill="#0a0a0a" />

        {/* Inner border glow */}
        <rect
          x="8" y="8"
          width="84" height="84"
          rx="15"
          fill="none"
          stroke="url(#neonGrad)"
          strokeWidth="2"
          filter="url(#glow)"
        />

        {/* Stylized R with neon effect */}
        <motion.g filter="url(#glow)">
          <path
            d="M30 75 L30 25 L55 25 Q72 25 72 40 Q72 52 58 54 L72 75"
            fill="none"
            stroke="url(#neonGrad)"
            strokeWidth="6"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
          <path
            d="M30 54 L50 54"
            fill="none"
            stroke="url(#neonGrad)"
            strokeWidth="6"
            strokeLinecap="round"
          />

          {/* Inner R highlight */}
          <path
            d="M40 35 L40 45 L50 45 Q58 45 58 40 Q58 35 50 35 Z"
            fill="url(#neonGrad)"
            fillOpacity="0.3"
          />
        </motion.g>

        {/* Animated pulse effect */}
        {animated && (
          <motion.rect
            x="8" y="8"
            width="84" height="84"
            rx="15"
            fill="none"
            stroke="url(#neonGrad)"
            strokeWidth="1"
            initial={{ opacity: 0.8 }}
            animate={{ opacity: [0.8, 0.2, 0.8] }}
            transition={{ duration: 2, repeat: Infinity }}
          />
        )}
      </svg>
    </motion.div>
  );
}

// ============================================
// LOGO OPTION 3: R with Soundwave Emanating
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

        {/* Square background */}
        <rect x="5" y="5" width="90" height="90" rx="18" fill="url(#pulseGrad)" />

        {/* Solid R */}
        <path
          d="M28 78 L28 22 L56 22 Q74 22 74 38 Q74 51 60 54 L74 78 L60 78 L48 56 L42 56 L42 78 Z M42 34 L42 46 L52 46 Q60 46 60 40 Q60 34 52 34 Z"
          fill="white"
        />

        {/* Soundwave arcs emanating from R */}
        {animated && [0, 1, 2].map((i) => (
          <motion.path
            key={i}
            d={`M78 ${35 + i * 10} Q${88 + i * 5} 50 78 ${65 - i * 10}`}
            fill="none"
            stroke="white"
            strokeWidth="2"
            strokeLinecap="round"
            initial={{ opacity: 0, pathLength: 0 }}
            animate={{
              opacity: [0, 0.8, 0],
              pathLength: [0, 1, 1]
            }}
            transition={{
              duration: 1.5,
              repeat: Infinity,
              delay: i * 0.3
            }}
          />
        ))}
      </svg>
    </motion.div>
  );
}

// ============================================
// LOGO OPTION 4: Minimal R with Wave Line
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

        {/* Square background */}
        <rect x="5" y="5" width="90" height="90" rx="18" fill="url(#minGrad)" />

        {/* R outline stroke */}
        <path
          d="M30 75 L30 25 L55 25 Q72 25 72 40 Q72 52 57 54 L72 75"
          fill="none"
          stroke="white"
          strokeWidth="7"
          strokeLinecap="round"
          strokeLinejoin="round"
        />

        {/* Horizontal connector */}
        <path
          d="M30 54 L48 54"
          fill="none"
          stroke="white"
          strokeWidth="7"
          strokeLinecap="round"
        />

        {/* Sound wave line through middle */}
        <motion.path
          d="M20 50 Q25 35 30 50 Q35 65 40 50 Q45 35 50 50 Q55 65 60 50 Q65 35 70 50 Q75 65 80 50"
          fill="none"
          stroke="white"
          strokeWidth="2"
          strokeLinecap="round"
          strokeOpacity="0.6"
          animate={animated ? {
            d: [
              "M20 50 Q25 35 30 50 Q35 65 40 50 Q45 35 50 50 Q55 65 60 50 Q65 35 70 50 Q75 65 80 50",
              "M20 50 Q25 42 30 50 Q35 58 40 50 Q45 42 50 50 Q55 58 60 50 Q65 42 70 50 Q75 58 80 50",
              "M20 50 Q25 35 30 50 Q35 65 40 50 Q45 35 50 50 Q55 65 60 50 Q65 35 70 50 Q75 65 80 50",
            ]
          } : {}}
          transition={{ duration: 1.5, repeat: Infinity, ease: "easeInOut" }}
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
