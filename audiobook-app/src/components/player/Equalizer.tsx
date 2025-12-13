import { useState, useEffect, useRef, useCallback } from 'react';
import { motion } from 'framer-motion';
import { SlidersHorizontal, RotateCcw, Music2 } from 'lucide-react';
import { Button, Modal } from '../ui';

// EQ frequency bands (Hz)
const FREQUENCY_BANDS = [60, 170, 310, 600, 1000, 3000, 6000, 12000, 14000, 16000];
const BAND_LABELS = ['60', '170', '310', '600', '1K', '3K', '6K', '12K', '14K', '16K'];

// Equalizer presets
export const EQ_PRESETS = {
  flat: {
    name: 'Flat',
    gains: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
    description: 'No equalization',
  },
  spoken_word: {
    name: 'Spoken Word',
    gains: [-2, -1, 0, 3, 5, 4, 2, 0, -1, -2],
    description: 'Optimized for audiobooks',
  },
  bass_boost: {
    name: 'Bass Boost',
    gains: [6, 5, 4, 2, 0, 0, 0, 0, 0, 0],
    description: 'Enhanced low frequencies',
  },
  treble_boost: {
    name: 'Treble Boost',
    gains: [0, 0, 0, 0, 0, 2, 4, 5, 6, 6],
    description: 'Enhanced high frequencies',
  },
  vocal_clarity: {
    name: 'Vocal Clarity',
    gains: [-2, -1, 0, 2, 4, 5, 4, 3, 1, 0],
    description: 'Clear voice reproduction',
  },
  podcast: {
    name: 'Podcast',
    gains: [-1, 0, 1, 3, 4, 4, 3, 1, 0, -1],
    description: 'Optimized for podcasts',
  },
  night_mode: {
    name: 'Night Mode',
    gains: [-4, -3, -2, 0, 2, 2, 0, -2, -3, -4],
    description: 'Reduced dynamic range',
  },
  loudness: {
    name: 'Loudness',
    gains: [5, 4, 2, 0, -1, -1, 0, 2, 4, 5],
    description: 'V-shaped enhancement',
  },
} as const;

export type EQPreset = keyof typeof EQ_PRESETS;

interface EqualizerProps {
  audioRef: React.RefObject<HTMLAudioElement | null>;
}

export function Equalizer({ audioRef }: EqualizerProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [isEnabled, setIsEnabled] = useState(true);
  const [currentPreset, setCurrentPreset] = useState<EQPreset>('flat');
  const [customGains, setCustomGains] = useState<number[]>([...EQ_PRESETS.flat.gains]);
  const [isCustom, setIsCustom] = useState(false);

  // Web Audio API refs
  const audioContextRef = useRef<AudioContext | null>(null);
  const sourceNodeRef = useRef<MediaElementAudioSourceNode | null>(null);
  const filtersRef = useRef<BiquadFilterNode[]>([]);
  const gainNodeRef = useRef<GainNode | null>(null);

  // Initialize Web Audio API
  const initializeAudio = useCallback(() => {
    if (!audioRef.current || audioContextRef.current) return;

    try {
      const audioContext = new (window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext)();
      audioContextRef.current = audioContext;

      // Create source from audio element
      const source = audioContext.createMediaElementSource(audioRef.current);
      sourceNodeRef.current = source;

      // Create filters for each frequency band
      const filters: BiquadFilterNode[] = FREQUENCY_BANDS.map((freq, index) => {
        const filter = audioContext.createBiquadFilter();

        if (index === 0) {
          filter.type = 'lowshelf';
        } else if (index === FREQUENCY_BANDS.length - 1) {
          filter.type = 'highshelf';
        } else {
          filter.type = 'peaking';
        }

        filter.frequency.value = freq;
        filter.Q.value = 1;
        filter.gain.value = customGains[index];

        return filter;
      });

      filtersRef.current = filters;

      // Create master gain node
      const gainNode = audioContext.createGain();
      gainNode.gain.value = 1;
      gainNodeRef.current = gainNode;

      // Connect nodes: source -> filters -> gain -> destination
      let currentNode: AudioNode = source;
      filters.forEach((filter) => {
        currentNode.connect(filter);
        currentNode = filter;
      });
      currentNode.connect(gainNode);
      gainNode.connect(audioContext.destination);

    } catch (error) {
      console.error('Error initializing audio context:', error);
    }
  }, [audioRef, customGains]);

  // Apply gains to filters
  const applyGains = useCallback((gains: number[]) => {
    filtersRef.current.forEach((filter, index) => {
      if (filter && gains[index] !== undefined) {
        filter.gain.value = isEnabled ? gains[index] : 0;
      }
    });
  }, [isEnabled]);

  // Initialize audio context on first open
  useEffect(() => {
    if (isOpen && !audioContextRef.current && audioRef.current) {
      initializeAudio();
    }
  }, [isOpen, initializeAudio, audioRef]);

  // Apply gains when they change
  useEffect(() => {
    applyGains(customGains);
  }, [customGains, applyGains]);

  // Handle preset selection
  const handlePresetSelect = (preset: EQPreset) => {
    setCurrentPreset(preset);
    setCustomGains([...EQ_PRESETS[preset].gains]);
    setIsCustom(false);
  };

  // Handle individual band change
  const handleBandChange = (index: number, value: number) => {
    const newGains = [...customGains];
    newGains[index] = value;
    setCustomGains(newGains);
    setIsCustom(true);
  };

  // Reset to flat
  const handleReset = () => {
    handlePresetSelect('flat');
  };

  // Toggle EQ on/off
  const handleToggle = () => {
    setIsEnabled(!isEnabled);
    if (gainNodeRef.current) {
      gainNodeRef.current.gain.value = !isEnabled ? 1 : 0.001;
    }
    applyGains(customGains);
  };

  return (
    <>
      {/* EQ Button */}
      <Button
        variant="ghost"
        size="sm"
        onClick={() => setIsOpen(true)}
        className={`relative ${isEnabled && currentPreset !== 'flat' ? 'text-primary-500' : ''}`}
      >
        <SlidersHorizontal className="w-5 h-5" />
        {isEnabled && currentPreset !== 'flat' && (
          <span className="absolute -top-0.5 -right-0.5 w-2 h-2 bg-primary-500 rounded-full" />
        )}
      </Button>

      {/* EQ Modal */}
      <Modal
        isOpen={isOpen}
        onClose={() => setIsOpen(false)}
        title="Audio Equalizer"
        size="lg"
      >
        <div className="space-y-6">
          {/* Enable/Disable Toggle */}
          <div className="flex items-center justify-between p-4 bg-surface-100 dark:bg-surface-800 rounded-xl">
            <div className="flex items-center gap-3">
              <Music2 className="w-5 h-5 text-primary-500" />
              <div>
                <p className="font-medium text-surface-900 dark:text-white">Equalizer</p>
                <p className="text-sm text-surface-500">
                  {isEnabled ? 'Enabled' : 'Disabled'}
                </p>
              </div>
            </div>
            <button
              onClick={handleToggle}
              className={`relative w-12 h-7 rounded-full transition-colors ${
                isEnabled ? 'bg-primary-500' : 'bg-surface-300 dark:bg-surface-600'
              }`}
            >
              <motion.div
                className="absolute top-1 w-5 h-5 bg-white rounded-full shadow-md"
                animate={{ left: isEnabled ? '24px' : '4px' }}
                transition={{ type: 'spring', stiffness: 500, damping: 30 }}
              />
            </button>
          </div>

          {/* Presets */}
          <div>
            <div className="flex items-center justify-between mb-3">
              <h4 className="font-medium text-surface-900 dark:text-white">Presets</h4>
              <Button variant="ghost" size="sm" onClick={handleReset}>
                <RotateCcw className="w-4 h-4" />
                Reset
              </Button>
            </div>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
              {(Object.entries(EQ_PRESETS) as [EQPreset, typeof EQ_PRESETS[EQPreset]][]).map(([key, preset]) => (
                <button
                  key={key}
                  onClick={() => handlePresetSelect(key)}
                  className={`p-3 rounded-xl border-2 transition-all text-left ${
                    currentPreset === key && !isCustom
                      ? 'border-primary-500 bg-primary-500/10'
                      : 'border-surface-200 dark:border-surface-700 hover:border-surface-300 dark:hover:border-surface-600'
                  }`}
                >
                  <p className="font-medium text-sm text-surface-900 dark:text-white">
                    {preset.name}
                  </p>
                  <p className="text-xs text-surface-500 mt-0.5">{preset.description}</p>
                </button>
              ))}
            </div>
            {isCustom && (
              <p className="text-sm text-primary-500 mt-2">* Custom settings applied</p>
            )}
          </div>

          {/* Frequency Bands */}
          <div>
            <h4 className="font-medium text-surface-900 dark:text-white mb-4">
              Frequency Bands
            </h4>
            <div className="flex items-end justify-between gap-2 h-48 px-2">
              {customGains.map((gain, index) => (
                <div key={index} className="flex flex-col items-center flex-1">
                  {/* Vertical slider */}
                  <div className="flex-1 flex flex-col items-center justify-end relative w-full">
                    <div className="absolute inset-x-0 top-0 bottom-6 flex items-center justify-center">
                      <div className="w-1.5 h-full bg-surface-200 dark:bg-surface-700 rounded-full relative">
                        {/* Zero line */}
                        <div className="absolute left-1/2 top-1/2 -translate-x-1/2 w-3 h-0.5 bg-surface-400" />
                        {/* Value indicator */}
                        <motion.div
                          className={`absolute left-1/2 -translate-x-1/2 w-4 h-4 rounded-full ${
                            gain >= 0 ? 'bg-primary-500' : 'bg-red-500'
                          } shadow-lg cursor-grab active:cursor-grabbing`}
                          style={{
                            top: `${50 - (gain / 12) * 50}%`,
                          }}
                          drag="y"
                          dragConstraints={{ top: 0, bottom: 0 }}
                          dragElastic={0}
                          onDrag={(_, info) => {
                            const parentHeight = 160;
                            const percentage = info.point.y / parentHeight;
                            const newGain = Math.round((0.5 - percentage) * 24);
                            const clampedGain = Math.max(-12, Math.min(12, newGain));
                            handleBandChange(index, clampedGain);
                          }}
                        />
                        {/* Fill bar */}
                        <motion.div
                          className={`absolute left-0 right-0 rounded-full ${
                            gain >= 0 ? 'bg-primary-500/40' : 'bg-red-500/40'
                          }`}
                          style={{
                            top: gain >= 0 ? `${50 - (gain / 12) * 50}%` : '50%',
                            bottom: gain >= 0 ? '50%' : `${50 + (gain / 12) * 50}%`,
                          }}
                        />
                      </div>
                    </div>
                  </div>
                  {/* dB value */}
                  <span className="text-xs font-mono text-surface-500 mb-1">
                    {gain > 0 ? '+' : ''}{gain}
                  </span>
                  {/* Frequency label */}
                  <span className="text-xs text-surface-400">{BAND_LABELS[index]}</span>
                </div>
              ))}
            </div>
          </div>

          {/* Quick adjust buttons */}
          <div className="flex items-center justify-center gap-4 pt-2">
            {[
              { label: '-3dB All', action: () => setCustomGains(customGains.map(g => Math.max(-12, g - 3))) },
              { label: 'Flat', action: handleReset },
              { label: '+3dB All', action: () => setCustomGains(customGains.map(g => Math.min(12, g + 3))) },
            ].map(({ label, action }) => (
              <Button key={label} variant="secondary" size="sm" onClick={action}>
                {label}
              </Button>
            ))}
          </div>
        </div>
      </Modal>
    </>
  );
}
