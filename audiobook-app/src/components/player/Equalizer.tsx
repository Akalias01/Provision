import { useState, useCallback } from 'react';
import { motion } from 'framer-motion';
import { SlidersHorizontal, RotateCcw, Music2 } from 'lucide-react';
import { Modal } from '../ui';

// Simplified EQ presets - visual only for now to prevent crashes
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

const BAND_LABELS = ['Bass', 'Low Mid', 'Mid', 'High Mid', 'Treble'];

// Stub functions to prevent errors from other components
export function resumeAudioContext() {
  // No-op for now - prevents crashes
}

export function setAmplifierGain(_gain: number) {
  // No-op for now - prevents crashes
}

interface EqualizerProps {
  audioRef: React.RefObject<HTMLAudioElement | null>;
  asMenuItem?: boolean;
  onClose?: () => void;
}

export function Equalizer({ asMenuItem, onClose }: EqualizerProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [isEnabled, setIsEnabled] = useState(false);
  const [currentPreset, setCurrentPreset] = useState<EQPreset>('flat');
  const [customGains, setCustomGains] = useState<number[]>([...EQ_PRESETS.flat.gains]);
  const [isCustom, setIsCustom] = useState(false);

  // Handle preset selection
  const handlePresetSelect = useCallback((preset: EQPreset) => {
    setCurrentPreset(preset);
    setCustomGains([...EQ_PRESETS[preset].gains]);
    setIsCustom(false);
  }, []);

  // Handle individual band change
  const handleBandChange = useCallback((index: number, value: number) => {
    const newGains = [...customGains];
    newGains[index] = value;
    setCustomGains(newGains);
    setIsCustom(true);
  }, [customGains]);

  // Reset to flat
  const handleReset = useCallback(() => {
    handlePresetSelect('flat');
  }, [handlePresetSelect]);

  // Toggle EQ on/off
  const handleToggle = useCallback(() => {
    setIsEnabled(!isEnabled);
  }, [isEnabled]);

  const handleOpen = useCallback((e: React.MouseEvent) => {
    e.stopPropagation();
    e.preventDefault();
    onClose?.();
    setTimeout(() => setIsOpen(true), 100);
  }, [onClose]);

  const handleClose = useCallback(() => {
    setIsOpen(false);
  }, []);

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
            <p className="font-semibold text-white">Equalizer</p>
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
          {/* Info Banner */}
          <div className="p-3 bg-amber-500/10 border border-amber-500/20 rounded-xl">
            <p className="text-sm text-amber-400">
              EQ settings are visual presets. Audio processing is being improved for stability.
            </p>
          </div>

          {/* Enable/Disable Toggle */}
          <div className="flex items-center justify-between p-4 bg-surface-800 rounded-xl">
            <div className="flex items-center gap-3">
              <Music2 className="w-5 h-5 text-primary-500" />
              <div>
                <p className="font-semibold text-white">Equalizer</p>
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
              <h4 className="font-semibold text-white">Presets</h4>
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
                  <p className="font-semibold text-sm text-white">{preset.name}</p>
                  <p className="text-xs text-surface-400">{preset.description}</p>
                </button>
              ))}
            </div>
            {isCustom && (
              <p className="text-sm text-primary-500 mt-2">* Custom settings</p>
            )}
          </div>

          {/* Frequency Bands - Visual Sliders */}
          <div>
            <h4 className="font-semibold text-white mb-4">Frequency Bands</h4>
            <div className="space-y-4">
              {customGains.map((gain, index) => (
                <div key={index} className="flex items-center gap-4">
                  <span className="w-16 text-sm font-medium text-surface-400">{BAND_LABELS[index]}</span>
                  <input
                    type="range"
                    min="-12"
                    max="12"
                    step="1"
                    value={gain}
                    onChange={(e) => handleBandChange(index, parseInt(e.target.value))}
                    className="flex-1 h-2 bg-surface-700 rounded-lg appearance-none cursor-pointer accent-primary-500"
                  />
                  <span className={`w-12 text-sm font-semibold text-right ${gain >= 0 ? 'text-primary-400' : 'text-red-400'}`}>
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
