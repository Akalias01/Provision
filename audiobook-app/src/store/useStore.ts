import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { Book, PlayerState, TTSState, ReaderSettings, BookFormat } from '../types';

interface AppState {
  // Theme
  isDarkMode: boolean;
  toggleDarkMode: () => void;

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

  // Filter
  formatFilter: BookFormat | 'all';
  setFormatFilter: (filter: BookFormat | 'all') => void;
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

      // Filter
      formatFilter: 'all',
      setFormatFilter: (filter) => set({ formatFilter: filter }),
    }),
    {
      name: 'audiobook-storage',
      partialize: (state) => ({
        isDarkMode: state.isDarkMode,
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

// Initialize dark mode on load
if (typeof window !== 'undefined') {
  const stored = localStorage.getItem('audiobook-storage');
  if (stored) {
    const data = JSON.parse(stored);
    if (data.state?.isDarkMode) {
      document.documentElement.classList.add('dark');
    }
  } else {
    // Default to dark mode
    document.documentElement.classList.add('dark');
  }
}
