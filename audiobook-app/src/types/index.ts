export type BookFormat = 'audio' | 'epub' | 'pdf';

export type AudioFormat = 'mp3' | 'm4b' | 'aac' | 'ogg' | 'flac' | 'wav';

export interface Book {
  id: string;
  title: string;
  author: string;
  cover?: string;
  format: BookFormat;
  fileUrl: string;
  duration?: number; // for audio books, in seconds
  pageCount?: number; // for epub/pdf
  currentPosition: number; // percentage or timestamp
  lastPlayed?: Date;
  dateAdded: Date;
  description?: string;
  narrator?: string;
  genre?: string;
  chapters?: Chapter[];
  bookmarks?: Bookmark[];
  isFinished?: boolean;
}

export interface Chapter {
  id: string;
  title: string;
  startTime?: number; // for audio
  startPage?: number; // for epub/pdf
  duration?: number;
}

export interface Bookmark {
  id: string;
  position: number;
  note?: string;
  createdAt: Date;
}

export interface PlayerState {
  isPlaying: boolean;
  currentTime: number;
  duration: number;
  volume: number;
  playbackRate: number;
  isMuted: boolean;
}

export interface TTSState {
  isReading: boolean;
  currentSentence: number;
  voice: SpeechSynthesisVoice | null;
  rate: number;
  pitch: number;
}

export interface ReaderSettings {
  fontSize: number;
  fontFamily: string;
  lineHeight: number;
  theme: 'light' | 'dark' | 'sepia';
  marginSize: number;
}

export interface AppView {
  currentView: 'library' | 'player' | 'reader';
  currentBook: Book | null;
}
