const { app, BrowserWindow, ipcMain, shell } = require('electron');
const path = require('path');
const fs = require('fs');

// Disable GPU acceleration - fixes black screen on some systems
app.disableHardwareAcceleration();
app.commandLine.appendSwitch('disable-gpu');
app.commandLine.appendSwitch('disable-software-rasterizer');

let mainWindow;
let WebTorrent;
let torrentClient;
const activeTorrents = new Map();

// Downloads directory
const getDownloadsPath = () => {
  const userDataPath = app.getPath('userData');
  const downloadsPath = path.join(userDataPath, 'downloads');

  // Create downloads directory if it doesn't exist
  if (!fs.existsSync(downloadsPath)) {
    fs.mkdirSync(downloadsPath, { recursive: true });
  }

  return downloadsPath;
};

// Initialize WebTorrent client lazily
const initTorrentClient = async () => {
  if (torrentClient) return torrentClient;

  try {
    // Dynamic import for WebTorrent
    WebTorrent = require('webtorrent');
    torrentClient = new WebTorrent();

    torrentClient.on('error', (err) => {
      console.error('WebTorrent client error:', err);
    });

    return torrentClient;
  } catch (error) {
    console.error('Failed to initialize WebTorrent:', error);
    return null;
  }
};

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1400,
    height: 900,
    minWidth: 800,
    minHeight: 600,
    backgroundColor: '#09090b',
    autoHideMenuBar: true,
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      preload: path.join(__dirname, 'preload.cjs'),
    },
  });

  // Load the built app
  const indexPath = path.join(__dirname, 'dist', 'index.html');
  mainWindow.loadFile(indexPath);

  mainWindow.on('closed', () => {
    mainWindow = null;
  });
}

// IPC Handlers
ipcMain.handle('get-downloads-path', () => {
  return getDownloadsPath();
});

ipcMain.handle('show-item-in-folder', async (_event, filePath) => {
  shell.showItemInFolder(filePath);
});

ipcMain.handle('torrent:start', async (_event, torrentSource) => {
  const client = await initTorrentClient();

  if (!client) {
    return { success: false, error: 'WebTorrent not available' };
  }

  const torrentId = `torrent-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
  const downloadsPath = getDownloadsPath();

  try {
    // Add torrent
    const torrent = client.add(torrentSource, {
      path: downloadsPath,
    });

    activeTorrents.set(torrentId, torrent);

    // Send initial info
    torrent.on('metadata', () => {
      mainWindow?.webContents.send('torrent:progress', {
        id: torrentId,
        name: torrent.name,
        progress: 0,
        downloadSpeed: 0,
        numPeers: torrent.numPeers,
        status: 'downloading',
      });
    });

    // Progress updates
    torrent.on('download', () => {
      const progress = Math.round(torrent.progress * 100);
      mainWindow?.webContents.send('torrent:progress', {
        id: torrentId,
        name: torrent.name,
        progress,
        downloadSpeed: torrent.downloadSpeed,
        numPeers: torrent.numPeers,
        status: 'downloading',
      });
    });

    // Completion
    torrent.on('done', () => {
      // Find audio files in the torrent
      const audioExtensions = ['.mp3', '.m4b', '.m4a', '.aac', '.ogg', '.flac', '.wav'];
      const audioFiles = torrent.files.filter((file) =>
        audioExtensions.some((ext) => file.name.toLowerCase().endsWith(ext))
      );

      // Get file paths
      const filePaths = audioFiles.map((file) => path.join(downloadsPath, file.path));

      mainWindow?.webContents.send('torrent:complete', {
        id: torrentId,
        name: torrent.name,
        files: filePaths,
        totalSize: torrent.length,
        downloadPath: path.join(downloadsPath, torrent.name),
      });

      // Clean up
      activeTorrents.delete(torrentId);

      // Remove torrent from client but keep files
      torrent.destroy();
    });

    // Error handling
    torrent.on('error', (err) => {
      mainWindow?.webContents.send('torrent:error', {
        id: torrentId,
        error: err.message,
      });
      activeTorrents.delete(torrentId);
    });

    return {
      success: true,
      id: torrentId,
      name: torrent.name || torrentSource.slice(0, 50),
    };
  } catch (error) {
    console.error('Error starting torrent:', error);
    return { success: false, error: error.message };
  }
});

ipcMain.handle('torrent:cancel', async (_event, torrentId) => {
  const torrent = activeTorrents.get(torrentId);

  if (torrent) {
    torrent.destroy();
    activeTorrents.delete(torrentId);
    return { success: true };
  }

  return { success: false, error: 'Torrent not found' };
});

app.whenReady().then(createWindow);

app.on('window-all-closed', () => {
  // Clean up torrent client
  if (torrentClient) {
    torrentClient.destroy();
  }

  if (process.platform !== 'darwin') {
    app.quit();
  }
});

app.on('activate', () => {
  if (BrowserWindow.getAllWindows().length === 0) {
    createWindow();
  }
});
