import { useState, useEffect, useRef, useCallback } from 'react';
import { motion } from 'framer-motion';
import { SlidersHorizontal, RotateCcw, Music2 } from 'lucide-react';
import { Modal } from '../ui';

// Simplified EQ with 5 bands for mobile
const FREQUENCY_BANDS = [60, 250, 1000, 4000, 16000];
const BAND_LABELS = ['Bass', 'Low Mid', 'Mid', 'High Mid', 'Treble'];

// Simplified presets
export const EQ_PRESETS = {
  flat: {
    name: 'Flat',
    gains: [0, 0, 0, 0, 0],
    description: 'No equalization',
  },
  spoken_word: {
    name: 'Spoken Word',
    gains: [-2, 0, 4, 3, 0],
    description: 'Optimized for audiobooks',
  },
  bass_boost: {
    name: 'Bass Boost',
    gains: [6, 3, 0, 0, 0],
    description: 'Enhanced bass',
  },
  treble_boost: {
    name: 'Treble Boost',
    gains: [0, 0, 0, 3, 6],
    description: 'Enhanced treble',
  },
  vocal_clarity: {
    name: 'Vocal Clarity',
    gains: [-2, 1, 4, 2, 0],
    description: 'Clear voices',
  },
  night_mode: {
    name: 'Night Mode',
    gains: [-3, 0, 2, 0, -3],
    description: 'Reduced extremes',
  },
} as const;

export type EQPreset = keyof typeof EQ_PRESETS;

// Shared audio context
let sharedAudioContext: AudioContext | null = null;
let sharedSourceNode: MediaElementAudioSourceNode | null = null;
let sharedGainNode: GainNode | null = null;
let connectedAudioElement: HTMLAudioElement | null = null;

// Resume audio context (needed for autoplay policies)
export function resumeAudioContext() {
  if (sharedAudioContext?.state === 'suspended') {
    sharedAudioContext.resume().catch(() => {});
  }
}

// Update amplifier gain
export function setAmplifierGain(gain: number) {
  if (sharedGainNode) {
    try {
      sharedGainNode.gain.value = gain;
    } catch {
      // Ignore errors
    }
  }
}

// Safe audio context initialization
function initializeAudioContext(audioElement: HTMLAudioElement): {
  audioContext: AudioContext;
  sourceNode: MediaElementAudioSourceNode;
  gainNode: GainNode;
} | null {
  try {
    // Check if audio element is ready
    if (!audioElement || !audioElement.src) {
      return null;
    }

    // Return existing context if same element
    if (sharedAudioContext && connectedAudioElement === audioElement && sharedSourceNode && sharedGainNode) {
      return {
        audioContext: sharedAudioContext,
        sourceNode: sharedSourceNode,
        gainNode: sharedGainNode,
      };
    }

    // Clean up old context
    if (sharedAudioContext) {
      try {
        sharedAudioContext.close();
      } catch {
        // Ignore
      }
      sharedAudioContext = null;
      sharedSourceNode = null;
      sharedGainNode = null;
      connectedAudioElement = null;
    }

    // Create new context
    const AudioContextClass = window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
    sharedAudioContext = new AudioContextClass();
    sharedSourceNode = sharedAudioContext.createMediaElementSource(audioElement);
    sharedGainNode = sharedAudioContext.createGain();
    sharedGainNode.gain.value = 1;
    connectedAudioElement = audioElement;

    sharedSourceNode.connect(sharedGainNode);
    sharedGainNode.connect(sharedAudioContext.destination);

    return {
      audioContext: sharedAudioContext,
      sourceNode: sharedSourceNode,
      gainNode: sharedGainNode,
    };
  } catch (error) {
    console.error('[Equalizer] Initialization error:', error);
    return null;
  }
}

interface EqualizerProps {
  audioRef: React.RefObject<HTMLAudioElement | null>;
  asMenuItem?: boolean;
  onClose?: () => void;
}

export function Equalizer({ audioRef, asMenuItem, onClose }: EqualizerProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [isEnabled, setIsEnabled] = useState(true);
  const [currentPreset, setCurrentPreset] = useState<EQPreset>('flat');
  const [customGains, setCustomGains] = useState<number[]>([...EQ_PRESETS.flat.gains]);
  const [isCustom, setIsCustom] = useState(false);
  const [isInitialized, setIsInitialized] = useState(false);

  const filtersRef = useRef<BiquadFilterNode[]>([]);
  const audioContextRef = useRef<AudioContext | null>(null);

  // Initialize EQ when modal opens
  const initializeEQ = useCallback(() => {
    if (!audioRef.current || isInitialized) return;

    const audioElement = audioRef.current;
    if (!audioElement.src) {
      console.log('[Equalizer] No audio source yet');
      return;
    }

    try {
      const context = initializeAudioContext(audioElement);
      if (!context) {
        console.log('[Equalizer] Could not initialize audio context');
        return;
      }

      const { audioContext, sourceNode, gainNode } = context;
      audioContextRef.current = audioContext;

      // Disconnect source from gain to insert filters
      try {
        sourceNode.disconnect();
      } catch {
        // Already disconnected
      }

      // Create filters
      const filters: BiquadFilterNode[] = FREQUENCY_BANDS.map((freq, index) => {
        const filter = audioContext.createBiquadFilter();

        if (index === 0) {
          filter.type = 'lowshelf';
        } else if (index === FREQUENCY_BANDS.length - 1) {
          filter.type = 'highshelf';
        } else {
          filter.type = 'peaking';
          filter.Q.value = 1;
        }

        filter.frequency.value = freq;
        filter.gain.value = customGains[index];

        return filter;
      });

      filtersRef.current = filters;

      // Connect: source -> filters -> gain -> destination
      let currentNode: AudioNode = sourceNode;
      filters.forEach((filter) => {
        currentNode.connect(filter);
        currentNode = filter;
      });
      currentNode.connect(gainNode);

      setIsInitialized(true);
      console.log('[Equalizer] Initialized successfully');
    } catch (error) {
      console.error('[Equalizer] Error:', error);
    }
  }, [audioRef, isInitialized, customGains]);

  // Apply gains to filters
  const applyGains = useCallback((gains: number[]) => {
    try {
      filtersRef.current.forEach((filter, index) => {
        if (filter && gains[index] !== undefined) {
          const targetValue = isEnabled ? gains[index] : 0;
          filter.gain.value = targetValue;
        }
      });
    } catch (error) {
      console.error('[Equalizer] Error applying gains:', error);
    }
  }, [isEnabled]);

  // Initialize on modal open
  useEffect(() => {
    if (isOpen && !isInitialized && audioRef.current) {
      // Wait a bit for audio to be ready
      const timer = setTimeout(() => {
        initializeEQ();
      }, 100);
      return () => clearTimeout(timer);
    }
  }, [isOpen, isInitialized, initializeEQ, audioRef]);

  // Apply gains when they change
  useEffect(() => {
    if (isInitialized) {
      applyGains(customGains);
    }
  }, [customGains, applyGains, isInitialized]);

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
    applyGains(customGains);
  };

  const handleOpen = (e: React.MouseEvent) => {
    e.stopPropagation();
    e.preventDefault();
    onClose?.();
    setTimeout(() => setIsOpen(true), 100);
  };

  const handleClose = () => {
    setIsOpen(false);
  };

  return (
    <>
      {/* EQ Button/Menu Item */}
      {asMenuItem ? (
        <button
          onClick={handleOpen}
          className="w-full flex items-center gap-3 p-4 rounded-xl bg-surface-800 hover:bg-surface-700 transition-colors"
        >
          <SlidersHorizontal className="w-5 h-5 text-primary-500" />
          <div className="text-left flex-1">
            <p className="font-medium text-white">Equalizer</p>
            <p className="text-sm text-surface-400">
              {isEnabled && currentPreset !== 'flat' ? EQ_PRESETS[currentPreset].name : 'Adjust frequencies'}
            </p>
          </div>
          {isEnabled && currentPreset !== 'flat' && (
            <span className="w-2 h-2 bg-primary-500 rounded-full" />
          )}
        </button>
      ) : (
        <button
          onClick={handleOpen}
          className={`p-2 rounded-lg transition-colors ${
            isEnabled && currentPreset !== 'flat' ? 'text-primary-500' : 'text-surface-400'
          } hover:bg-surface-800`}
        >
          <SlidersHorizontal className="w-5 h-5" />
        </button>
      )}

      {/* EQ Modal */}
      <Modal
        isOpen={isOpen}
        onClose={handleClose}
        title="Equalizer"
        size="md"
      >
        <div className="space-y-6">
          {/* Enable/Disable Toggle */}
          <div className="flex items-center justify-between p-4 bg-surface-800 rounded-xl">
            <div className="flex items-center gap-3">
              <Music2 className="w-5 h-5 text-primary-500" />
              <div>
                <p className="font-medium text-white">Equalizer</p>
                <p className="text-sm text-surface-400">{isEnabled ? 'Enabled' : 'Disabled'}</p>
              </div>
            </div>
            <button
              onClick={handleToggle}
              className={`relative w-12 h-7 rounded-full transition-colors ${
                isEnabled ? 'bg-primary-500' : 'bg-surface-600'
              }`}
            >
              <motion.div
                className="absolute top-1 w-5 h-5 bg-white rounded-full shadow-md"
                animate={{ left: isEnabled ? '24px' : '4px' }}
                transition={{ type: 'spring', stiffness: 500, damping: 30 }}
              />
            </button>
          </div>

          {/* Presets Grid */}
          <div>
            <div className="flex items-center justify-between mb-3">
              <h4 className="font-medium text-white">Presets</h4>
              <button
                onClick={handleReset}
                className="flex items-center gap-1 text-sm text-surface-400 hover:text-white transition-colors"
              >
                <RotateCcw className="w-4 h-4" />
                Reset
              </button>
            </div>
            <div className="grid grid-cols-2 gap-2">
              {(Object.entries(EQ_PRESETS) as [EQPreset, typeof EQ_PRESETS[EQPreset]][]).map(([key, preset]) => (
                <button
                  key={key}
                  onClick={() => handlePresetSelect(key)}
                  className={`p-3 rounded-xl border-2 transition-all text-left ${
                    currentPreset === key && !isCustom
                      ? 'border-primary-500 bg-primary-500/10'
                      : 'border-surface-700 hover:border-surface-600'
                  }`}
                >
                  <p className="font-medium text-sm text-white">{preset.name}</p>
                  <p className="text-xs text-surface-400">{preset.description}</p>
                </button>
              ))}
            </div>
            {isCustom && (
              <p className="text-sm text-primary-500 mt-2">* Custom settings</p>
            )}
          </div>

          {/* Frequency Bands - Simplified Sliders */}
          <div>
            <h4 className="font-medium text-white mb-4">Frequency Bands</h4>
            <div className="space-y-4">
              {customGains.map((gain, index) => (
                <div key={index} className="flex items-center gap-4">
                  <span className="w-16 text-sm text-surface-400">{BAND_LABELS[index]}</span>
                  <input
                    type="range"
                    min="-12"
                    max="12"
                    step="1"
                    value={gain}
                    onChange={(e) => handleBandChange(index, parseInt(e.target.value))}
                    className="flex-1 h-2 bg-surface-700 rounded-lg appearance-none cursor-pointer accent-primary-500"
                  />
                  <span className={`w-12 text-sm text-right ${gain >= 0 ? 'text-primary-400' : 'text-red-400'}`}>
                    {gain > 0 ? '+' : ''}{gain}dB
                  </span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </Modal>
    </>
  );
}
