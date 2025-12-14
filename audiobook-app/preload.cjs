const { contextBridge, ipcRenderer } = require('electron');

// Expose protected methods to renderer process
contextBridge.exposeInMainWorld('electronAPI', {
  // Torrent operations
  startTorrentDownload: (torrentSource) => ipcRenderer.invoke('torrent:start', torrentSource),
  cancelTorrentDownload: (torrentId) => ipcRenderer.invoke('torrent:cancel', torrentId),

  // Listen for torrent progress updates
  onTorrentProgress: (callback) => {
    const listener = (_event, data) => callback(data);
    ipcRenderer.on('torrent:progress', listener);
    return () => ipcRenderer.removeListener('torrent:progress', listener);
  },

  // Listen for torrent completion
  onTorrentComplete: (callback) => {
    const listener = (_event, data) => callback(data);
    ipcRenderer.on('torrent:complete', listener);
    return () => ipcRenderer.removeListener('torrent:complete', listener);
  },

  // Listen for torrent errors
  onTorrentError: (callback) => {
    const listener = (_event, data) => callback(data);
    ipcRenderer.on('torrent:error', listener);
    return () => ipcRenderer.removeListener('torrent:error', listener);
  },

  // Get downloads path
  getDownloadsPath: () => ipcRenderer.invoke('get-downloads-path'),

  // Open file in explorer
  showItemInFolder: (path) => ipcRenderer.invoke('show-item-in-folder', path),
});
