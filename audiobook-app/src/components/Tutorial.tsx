import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  X,
  ChevronRight,
  ChevronLeft,
  Library,
  Settings,
  Download,
  Play,
  BookOpen,
  Palette,
  Moon,
  Search,
  Plus,
  SlidersHorizontal,
  Timer,
  Bookmark,
  SkipForward
} from 'lucide-react';
import { Button } from './ui';

interface TutorialStep {
  id: string;
  title: string;
  description: string;
  icon: React.ElementType;
  position: 'center' | 'top-left' | 'top-right' | 'bottom-left' | 'bottom-right' | 'left' | 'right';
  highlight?: {
    x: string;
    y: string;
    width: string;
    height: string;
  };
}

const tutorialSteps: TutorialStep[] = [
  {
    id: 'welcome',
    title: 'Welcome to Rezon!',
    description: 'Let us show you around. This quick tour will help you discover all the features Rezon has to offer.',
    icon: BookOpen,
    position: 'center',
  },
  {
    id: 'library',
    title: 'Your Library',
    description: 'All your audiobooks and ebooks appear here. Use the filters to sort by format or reading progress.',
    icon: Library,
    position: 'center',
    highlight: { x: '50%', y: '50%', width: '80%', height: '60%' },
  },
  {
    id: 'add-books',
    title: 'Add Books',
    description: 'Click the + button to add new books. You can upload files, scan folders, or download from torrents.',
    icon: Plus,
    position: 'top-right',
    highlight: { x: '85%', y: '10%', width: '120px', height: '50px' },
  },
  {
    id: 'search',
    title: 'Search Your Library',
    description: 'Use the search bar to quickly find any book in your collection by title or author.',
    icon: Search,
    position: 'top-left',
    highlight: { x: '30%', y: '10%', width: '300px', height: '50px' },
  },
  {
    id: 'player',
    title: 'Audio Player',
    description: 'Click any audiobook to open the full-featured player with playback controls, speed adjustment, and more.',
    icon: Play,
    position: 'center',
  },
  {
    id: 'equalizer',
    title: 'Equalizer',
    description: 'Fine-tune your audio with the built-in equalizer. Choose from presets like "Spoken Word" or create your own.',
    icon: SlidersHorizontal,
    position: 'center',
  },
  {
    id: 'sleep-timer',
    title: 'Sleep Timer',
    description: 'Set a sleep timer to automatically pause playback. Perfect for listening before bed.',
    icon: Timer,
    position: 'center',
  },
  {
    id: 'bookmarks',
    title: 'Bookmarks',
    description: 'Save your favorite moments with bookmarks. Add notes and jump back to them anytime.',
    icon: Bookmark,
    position: 'center',
  },
  {
    id: 'skip-controls',
    title: 'Skip Controls',
    description: 'Customize skip intervals (10s, 15s, 30s, etc.) to navigate through your audiobooks quickly.',
    icon: SkipForward,
    position: 'center',
  },
  {
    id: 'themes',
    title: 'Customize Your Theme',
    description: 'Choose from many color themes including Classic, Modern, Steampunk, and more. Make Rezon truly yours.',
    icon: Palette,
    position: 'left',
  },
  {
    id: 'dark-mode',
    title: 'Dark Mode',
    description: 'Toggle between light and dark mode anytime. Your eyes will thank you during late-night listening sessions.',
    icon: Moon,
    position: 'top-right',
  },
  {
    id: 'torrents',
    title: 'Torrent Downloads',
    description: 'Download audiobooks via magnet links or .torrent files. Files are saved to your Rezon downloads folder.',
    icon: Download,
    position: 'center',
  },
  {
    id: 'settings',
    title: 'Settings',
    description: 'Access all settings from the sidebar menu. Customize splash screens, logo styles, and more.',
    icon: Settings,
    position: 'left',
  },
  {
    id: 'complete',
    title: 'You\'re All Set!',
    description: 'You now know the basics of Rezon. Enjoy your audiobooks! You can access this tutorial again from Player Tips.',
    icon: BookOpen,
    position: 'center',
  },
];

interface TutorialProps {
  isOpen: boolean;
  onClose: () => void;
}

export function Tutorial({ isOpen, onClose }: TutorialProps) {
  const [currentStep, setCurrentStep] = useState(0);
  const [isAnimating, setIsAnimating] = useState(false);

  const step = tutorialSteps[currentStep];
  const isFirstStep = currentStep === 0;
  const isLastStep = currentStep === tutorialSteps.length - 1;

  useEffect(() => {
    if (isOpen) {
      setCurrentStep(0);
    }
  }, [isOpen]);

  const goToNext = () => {
    if (isAnimating || isLastStep) return;
    setIsAnimating(true);
    setTimeout(() => {
      setCurrentStep(prev => prev + 1);
      setIsAnimating(false);
    }, 150);
  };

  const goToPrev = () => {
    if (isAnimating || isFirstStep) return;
    setIsAnimating(true);
    setTimeout(() => {
      setCurrentStep(prev => prev - 1);
      setIsAnimating(false);
    }, 150);
  };

  const handleComplete = () => {
    onClose();
  };

  // Position styles for the tutorial card
  const getPositionStyles = (position: TutorialStep['position']) => {
    switch (position) {
      case 'top-left':
        return 'top-24 left-8';
      case 'top-right':
        return 'top-24 right-8';
      case 'bottom-left':
        return 'bottom-24 left-8';
      case 'bottom-right':
        return 'bottom-24 right-8';
      case 'left':
        return 'top-1/2 -translate-y-1/2 left-8';
      case 'right':
        return 'top-1/2 -translate-y-1/2 right-8';
      case 'center':
      default:
        return 'top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2';
    }
  };

  return (
    <AnimatePresence>
      {isOpen && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          className="fixed inset-0 z-[200]"
        >
          {/* Backdrop with spotlight effect */}
          <div className="absolute inset-0 bg-black/80 backdrop-blur-sm">
            {/* Animated particles */}
            {[...Array(20)].map((_, i) => (
              <motion.div
                key={i}
                className="absolute w-1 h-1 bg-primary-500/30 rounded-full"
                style={{
                  left: `${Math.random() * 100}%`,
                  top: `${Math.random() * 100}%`,
                }}
                animate={{
                  y: [0, -30, 0],
                  opacity: [0.3, 0.8, 0.3],
                }}
                transition={{
                  duration: 2 + Math.random() * 2,
                  repeat: Infinity,
                  delay: Math.random() * 2,
                }}
              />
            ))}

            {/* Highlight area if specified */}
            {step.highlight && (
              <motion.div
                className="absolute border-2 border-primary-500 rounded-xl"
                style={{
                  left: step.highlight.x,
                  top: step.highlight.y,
                  width: step.highlight.width,
                  height: step.highlight.height,
                  transform: 'translate(-50%, -50%)',
                  boxShadow: '0 0 0 9999px rgba(0, 0, 0, 0.7), 0 0 30px rgba(6, 182, 212, 0.5)',
                }}
                initial={{ opacity: 0, scale: 0.9 }}
                animate={{ opacity: 1, scale: 1 }}
                transition={{ delay: 0.2 }}
              >
                {/* Pulsing border effect */}
                <motion.div
                  className="absolute inset-0 border-2 border-primary-400 rounded-xl"
                  animate={{
                    scale: [1, 1.05, 1],
                    opacity: [1, 0.5, 1],
                  }}
                  transition={{ duration: 1.5, repeat: Infinity }}
                />
              </motion.div>
            )}
          </div>

          {/* Close button */}
          <button
            onClick={onClose}
            className="absolute top-6 right-6 p-2 rounded-full bg-white/10 hover:bg-white/20 transition-colors z-10"
          >
            <X className="w-6 h-6 text-white" />
          </button>

          {/* Skip button */}
          <button
            onClick={onClose}
            className="absolute top-6 left-6 px-4 py-2 rounded-full bg-white/10 hover:bg-white/20 transition-colors text-white/70 text-sm z-10"
          >
            Skip Tutorial
          </button>

          {/* Progress indicators */}
          <div className="absolute bottom-8 left-1/2 -translate-x-1/2 flex gap-2 z-10">
            {tutorialSteps.map((_, index) => (
              <motion.div
                key={index}
                className={`h-1.5 rounded-full transition-all ${
                  index === currentStep
                    ? 'w-8 bg-primary-500'
                    : index < currentStep
                    ? 'w-1.5 bg-primary-500/50'
                    : 'w-1.5 bg-white/20'
                }`}
                animate={index === currentStep ? { scale: [1, 1.1, 1] } : {}}
                transition={{ duration: 0.5 }}
              />
            ))}
          </div>

          {/* Tutorial card */}
          <motion.div
            key={step.id}
            initial={{ opacity: 0, scale: 0.9, y: 20 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.9, y: -20 }}
            transition={{ type: 'spring', duration: 0.5 }}
            className={`absolute ${getPositionStyles(step.position)} w-full max-w-md p-8 z-10`}
          >
            <div className="bg-gradient-to-br from-surface-900 to-surface-950 rounded-3xl p-8 shadow-2xl border border-surface-700/50 relative overflow-hidden">
              {/* Background glow */}
              <div className="absolute -top-20 -right-20 w-40 h-40 bg-primary-500/20 rounded-full blur-3xl" />
              <div className="absolute -bottom-20 -left-20 w-40 h-40 bg-purple-500/20 rounded-full blur-3xl" />

              {/* Icon */}
              <motion.div
                className="relative mb-6"
                initial={{ scale: 0 }}
                animate={{ scale: 1, rotate: [0, -10, 10, 0] }}
                transition={{ type: 'spring', delay: 0.2 }}
              >
                <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-primary-500 to-primary-600 flex items-center justify-center shadow-lg shadow-primary-500/30">
                  <step.icon className="w-8 h-8 text-white" />
                </div>
                {/* Animated ring */}
                <motion.div
                  className="absolute inset-0 rounded-2xl border-2 border-primary-400"
                  animate={{ scale: [1, 1.2, 1], opacity: [1, 0, 1] }}
                  transition={{ duration: 2, repeat: Infinity }}
                />
              </motion.div>

              {/* Content */}
              <motion.h2
                className="text-2xl font-bold text-white mb-3"
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: 0.3 }}
              >
                {step.title}
              </motion.h2>
              <motion.p
                className="text-surface-300 leading-relaxed mb-8"
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: 0.4 }}
              >
                {step.description}
              </motion.p>

              {/* Step counter */}
              <div className="text-sm text-surface-500 mb-4">
                Step {currentStep + 1} of {tutorialSteps.length}
              </div>

              {/* Navigation */}
              <div className="flex gap-3">
                {!isFirstStep && (
                  <Button
                    variant="secondary"
                    onClick={goToPrev}
                    disabled={isAnimating}
                    className="flex-1"
                  >
                    <ChevronLeft className="w-4 h-4" />
                    Previous
                  </Button>
                )}
                {isLastStep ? (
                  <Button
                    variant="primary"
                    onClick={handleComplete}
                    className="flex-1"
                  >
                    Get Started
                  </Button>
                ) : (
                  <Button
                    variant="primary"
                    onClick={goToNext}
                    disabled={isAnimating}
                    className="flex-1"
                  >
                    Next
                    <ChevronRight className="w-4 h-4" />
                  </Button>
                )}
              </div>
            </div>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
