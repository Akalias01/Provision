import { motion } from 'framer-motion';

interface VocaLogoProps {
  size?: 'sm' | 'md' | 'lg';
  animated?: boolean;
}

export function VocaLogo({ size = 'md', animated = true }: VocaLogoProps) {
  const sizes = {
    sm: { icon: 32, text: 'text-lg' },
    md: { icon: 40, text: 'text-xl' },
    lg: { icon: 56, text: 'text-3xl' },
  };

  const { icon, text } = sizes[size];

  return (
    <div className="flex items-center gap-3">
      {/* Brain + Sound Wave Icon */}
      <motion.div
        whileHover={animated ? { scale: 1.05 } : undefined}
        className="relative"
      >
        <svg
          width={icon}
          height={icon}
          viewBox="0 0 100 100"
          className="drop-shadow-lg"
        >
          {/* Background circle */}
          <defs>
            <linearGradient id="logoGradient" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stopColor="#818cf8" />
              <stop offset="100%" stopColor="#4f46e5" />
            </linearGradient>
            <linearGradient id="waveGradient" x1="0%" y1="0%" x2="100%" y2="0%">
              <stop offset="0%" stopColor="#c7d2fe" />
              <stop offset="100%" stopColor="#818cf8" />
            </linearGradient>
          </defs>

          {/* Circle background */}
          <circle cx="50" cy="50" r="45" fill="url(#logoGradient)" />

          {/* Simplified brain outline */}
          <path
            d="M50 25 C38 25 30 35 30 45 C30 52 33 58 38 62 C35 66 34 72 36 76 C38 80 43 80 47 78 C48 82 49 84 50 84 C51 84 52 82 53 78 C57 80 62 80 64 76 C66 72 65 66 62 62 C67 58 70 52 70 45 C70 35 62 25 50 25"
            fill="none"
            stroke="white"
            strokeWidth="3"
            strokeLinecap="round"
          />

          {/* Sound waves on the right */}
          <path
            d="M72 40 Q78 50 72 60"
            fill="none"
            stroke="url(#waveGradient)"
            strokeWidth="2.5"
            strokeLinecap="round"
            opacity="0.9"
          />
          <path
            d="M78 35 Q87 50 78 65"
            fill="none"
            stroke="url(#waveGradient)"
            strokeWidth="2"
            strokeLinecap="round"
            opacity="0.6"
          />
        </svg>
      </motion.div>

      {/* VOCA Text */}
      <h1 className={`${text} font-black tracking-tight`}>
        <span className="bg-gradient-to-r from-primary-400 via-primary-500 to-primary-600 bg-clip-text text-transparent">
          VOCA
        </span>
      </h1>
    </div>
  );
}
