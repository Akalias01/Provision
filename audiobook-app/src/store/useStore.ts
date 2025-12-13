import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { Book, PlayerState, TTSState, ReaderSettings, BookFormat } from '../types';

export type ProgressFilter = 'all' | 'not_started' | 'in_progress' | 'finished';
export type ColorTheme = 'cyber-teal' | 'neon-red' | 'ocean-blue' | 'violet-pulse' | 'emerald-glow';
export type LogoVariant = 'waveform' | 'headphones' | 'pulse' | 'minimal';
export type SplashVariant = 'pulseWave' | 'glitchCyber' | 'waveformMorph' | 'neonFlicker';

// Color theme definitions
export const colorThemes: Record<ColorTheme, {
  name: string;
  colors: Record<string, string>;
  logoStart: string;
  logoEnd: string;
}> = {
  'cyber-teal': {
    name: 'Cyber Teal',
    colors: {
      '--primary-50': '#ecfeff',
      '--primary-100': '#cffafe',
      '--primary-200': '#a5f3fc',
      '--primary-300': '#67e8f9',
      '--primary-400': '#22d3ee',
      '--primary-500': '#06b6d4',
      '--primary-600': '#0891b2',
      '--primary-700': '#0e7490',
      '--primary-800': '#155e75',
      '--primary-900': '#164e63',
      '--primary-950': '#083344',
    },
    logoStart: '#06b6d4',
    logoEnd: '#0891b2',
  },
  'neon-red': {
    name: 'Neon Red',
    colors: {
      '--primary-50': '#fff1f2',
      '--primary-100': '#ffe4e6',
      '--primary-200': '#fecdd3',
      '--primary-300': '#fda4af',
      '--primary-400': '#fb7185',
      '--primary-500': '#f43f5e',
      '--primary-600': '#e11d48',
      '--primary-700': '#be123c',
      '--primary-800': '#9f1239',
      '--primary-900': '#881337',
      '--primary-950': '#4c0519',
    },
    logoStart: '#f43f5e',
    logoEnd: '#e11d48',
  },
  'ocean-blue': {
    name: 'Ocean Blue',
    colors: {
      '--primary-50': '#eff6ff',
      '--primary-100': '#dbeafe',
      '--primary-200': '#bfdbfe',
      '--primary-300': '#93c5fd',
      '--primary-400': '#60a5fa',
      '--primary-500': '#3b82f6',
      '--primary-600': '#2563eb',
      '--primary-700': '#1d4ed8',
      '--primary-800': '#1e40af',
      '--primary-900': '#1e3a8a',
      '--primary-950': '#172554',
    },
    logoStart: '#3b82f6',
    logoEnd: '#2563eb',
  },
  'violet-pulse': {
    name: 'Violet Pulse',
    colors: {
      '--primary-50': '#faf5ff',
      '--primary-100': '#f3e8ff',
      '--primary-200': '#e9d5ff',
      '--primary-300': '#d8b4fe',
      '--primary-400': '#c084fc',
      '--primary-500': '#a855f7',
      '--primary-600': '#9333ea',
      '--primary-700': '#7e22ce',
      '--primary-800': '#6b21a8',
      '--primary-900': '#581c87',
      '--primary-950': '#3b0764',
    },
    logoStart: '#a855f7',
    logoEnd: '#9333ea',
  },
  'emerald-glow': {
    name: 'Emerald Glow',
    colors: {
      '--primary-50': '#ecfdf5',
      '--primary-100': '#d1fae5',
      '--primary-200': '#a7f3d0',
      '--primary-300': '#6ee7b7',
      '--primary-400': '#34d399',
      '--primary-500': '#10b981',
      '--primary-600': '#059669',
      '--primary-700': '#047857',
      '--primary-800': '#065f46',
      '--primary-900': '#064e3b',
      '--primary-950': '#022c22',
    },
    logoStart: '#10b981',
    logoEnd: '#059669',
  },
};

interface AppState {
  // Theme
  isDarkMode: boolean;
  toggleDarkMode: () => void;
  colorTheme: ColorTheme;
  setColorTheme: (theme: ColorTheme) => void;
  logoVariant: LogoVariant;
  setLogoVariant: (variant: LogoVariant) => void;
  splashVariant: SplashVariant;
  setSplashVariant: (variant: SplashVariant) => void;

  // Splash screen
  showSplash: boolean;
  setShowSplash: (show: boolean) => void;

  // Library
  books: Book[];
  addBook: (book: Book) => void;
  removeBook: (id: string) => void;
  updateBook: (id: string, updates: Partial<Book>) => void;

  // Current book
  currentBook: Book | null;
  setCurrentBook: (book: Book | null) => void;

  // View
  currentView: 'library' | 'player' | 'reader';
  setCurrentView: (view: 'library' | 'player' | 'reader') => void;

  // Player state
  playerState: PlayerState;
  setPlayerState: (state: Partial<PlayerState>) => void;

  // TTS state
  ttsState: TTSState;
  setTTSState: (state: Partial<TTSState>) => void;

  // Reader settings
  readerSettings: ReaderSettings;
  setReaderSettings: (settings: Partial<ReaderSettings>) => void;

  // Search
  searchQuery: string;
  setSearchQuery: (query: string) => void;

  // Filters
  formatFilter: BookFormat | 'all';
  setFormatFilter: (filter: BookFormat | 'all') => void;
  progressFilter: ProgressFilter;
  setProgressFilter: (filter: ProgressFilter) => void;

  // Torrent
  activeTorrents: { id: string; name: string; progress: number }[];
  addTorrent: (torrent: { id: string; name: string; progress: number }) => void;
  updateTorrent: (id: string, progress: number) => void;
  removeTorrent: (id: string) => void;
}

// Apply color theme to document
function applyColorTheme(theme: ColorTheme) {
  const themeConfig = colorThemes[theme];
  const root = document.documentElement;

  Object.entries(themeConfig.colors).forEach(([key, value]) => {
    root.style.setProperty(key, value);
  });

  // Set logo colors
  root.style.setProperty('--logo-start', themeConfig.logoStart);
  root.style.setProperty('--logo-end', themeConfig.logoEnd);
}

export const useStore = create<AppState>()(
  persist(
    (set) => ({
      // Theme
      isDarkMode: true,
      toggleDarkMode: () =>
        set((state) => {
          const newDarkMode = !state.isDarkMode;
          if (newDarkMode) {
            document.documentElement.classList.add('dark');
          } else {
            document.documentElement.classList.remove('dark');
          }
          return { isDarkMode: newDarkMode };
        }),

      colorTheme: 'cyber-teal',
      setColorTheme: (theme) => {
        applyColorTheme(theme);
        set({ colorTheme: theme });
      },

      logoVariant: 'waveform',
      setLogoVariant: (variant) => set({ logoVariant: variant }),

      splashVariant: 'glitchCyber',
      setSplashVariant: (variant) => set({ splashVariant: variant }),

      // Splash screen
      showSplash: true,
      setShowSplash: (show) => set({ showSplash: show }),

      // Library
      books: [],
      addBook: (book) =>
        set((state) => ({ books: [...state.books, book] })),
      removeBook: (id) =>
        set((state) => ({ books: state.books.filter((b) => b.id !== id) })),
      updateBook: (id, updates) =>
        set((state) => ({
          books: state.books.map((b) =>
            b.id === id ? { ...b, ...updates } : b
          ),
        })),

      // Current book
      currentBook: null,
      setCurrentBook: (book) => set({ currentBook: book }),

      // View
      currentView: 'library',
      setCurrentView: (view) => set({ currentView: view }),

      // Player state
      playerState: {
        isPlaying: false,
        currentTime: 0,
        duration: 0,
        volume: 0.8,
        playbackRate: 1,
        isMuted: false,
      },
      setPlayerState: (state) =>
        set((prev) => ({
          playerState: { ...prev.playerState, ...state },
        })),

      // TTS state
      ttsState: {
        isReading: false,
        currentSentence: 0,
        voice: null,
        rate: 1,
        pitch: 1,
      },
      setTTSState: (state) =>
        set((prev) => ({
          ttsState: { ...prev.ttsState, ...state },
        })),

      // Reader settings
      readerSettings: {
        fontSize: 18,
        fontFamily: 'Georgia',
        lineHeight: 1.8,
        theme: 'dark',
        marginSize: 40,
      },
      setReaderSettings: (settings) =>
        set((prev) => ({
          readerSettings: { ...prev.readerSettings, ...settings },
        })),

      // Search
      searchQuery: '',
      setSearchQuery: (query) => set({ searchQuery: query }),

      // Filters
      formatFilter: 'all',
      setFormatFilter: (filter) => set({ formatFilter: filter }),
      progressFilter: 'all',
      setProgressFilter: (filter) => set({ progressFilter: filter }),

      // Torrent
      activeTorrents: [],
      addTorrent: (torrent) =>
        set((state) => ({ activeTorrents: [...state.activeTorrents, torrent] })),
      updateTorrent: (id, progress) =>
        set((state) => ({
          activeTorrents: state.activeTorrents.map((t) =>
            t.id === id ? { ...t, progress } : t
          ),
        })),
      removeTorrent: (id) =>
        set((state) => ({
          activeTorrents: state.activeTorrents.filter((t) => t.id !== id),
        })),
    }),
    {
      name: 'voca-storage',
      partialize: (state) => ({
        isDarkMode: state.isDarkMode,
        colorTheme: state.colorTheme,
        logoVariant: state.logoVariant,
        splashVariant: state.splashVariant,
        books: state.books,
        readerSettings: state.readerSettings,
        playerState: {
          volume: state.playerState.volume,
          playbackRate: state.playerState.playbackRate,
        },
        ttsState: {
          rate: state.ttsState.rate,
          pitch: state.ttsState.pitch,
        },
      }),
    }
  )
);

// Initialize theme on load
if (typeof window !== 'undefined') {
  const stored = localStorage.getItem('voca-storage');
  if (stored) {
    const data = JSON.parse(stored);
    if (data.state?.isDarkMode) {
      document.documentElement.classList.add('dark');
    }
    if (data.state?.colorTheme) {
      applyColorTheme(data.state.colorTheme);
    } else {
      applyColorTheme('cyber-teal');
    }
  } else {
    // Default to dark mode and cyber-teal theme
    document.documentElement.classList.add('dark');
    applyColorTheme('cyber-teal');
  }
}
