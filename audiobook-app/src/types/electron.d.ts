// Type declarations for Electron IPC API exposed via preload.cjs

interface TorrentProgress {
  id: string;
  name: string;
  progress: number;
  downloadSpeed: number;
  numPeers: number;
  status: 'downloading' | 'seeding' | 'done';
}

interface TorrentComplete {
  id: string;
  name: string;
  files: string[];
  totalSize: number;
  downloadPath: string;
}

interface TorrentError {
  id: string;
  error: string;
}

interface TorrentStartResult {
  success: boolean;
  id?: string;
  name?: string;
  error?: string;
}

interface ElectronAPI {
  // Torrent operations
  startTorrentDownload: (torrentSource: string) => Promise<TorrentStartResult>;
  cancelTorrentDownload: (torrentId: string) => Promise<{ success: boolean; error?: string }>;

  // Torrent event listeners
  onTorrentProgress: (callback: (data: TorrentProgress) => void) => () => void;
  onTorrentComplete: (callback: (data: TorrentComplete) => void) => () => void;
  onTorrentError: (callback: (data: TorrentError) => void) => () => void;

  // File operations
  getDownloadsPath: () => Promise<string>;
  showItemInFolder: (path: string) => Promise<void>;
}

declare global {
  interface Window {
    electronAPI?: ElectronAPI;
  }
}

export {};
