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
    console.log('[WebTorrent] Initializing client...');

    // Dynamic import for WebTorrent
    WebTorrent = require('webtorrent');

    // Create client with tracker configuration
    torrentClient = new WebTorrent({
      // Use websocket trackers (work without WebRTC)
      tracker: {
        announce: [
          'wss://tracker.openwebtorrent.com',
          'wss://tracker.btorrent.xyz',
          'wss://tracker.fastcast.nz',
        ]
      }
    });

    torrentClient.on('error', (err) => {
      console.error('[WebTorrent] Client error:', err);
    });

    console.log('[WebTorrent] Client initialized successfully');
    return torrentClient;
  } catch (error) {
    console.error('[WebTorrent] Failed to initialize:', error);
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
    icon: path.join(__dirname, 'public', 'logo.svg'),
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      preload: path.join(__dirname, 'preload.cjs'),
    },
  });

  // In development, load from dev server
  if (process.env.NODE_ENV === 'development' || process.argv.includes('--dev')) {
    mainWindow.loadURL('http://localhost:5173');
    mainWindow.webContents.openDevTools();
  } else {
    // In production, load the built app
    const indexPath = path.join(__dirname, 'dist', 'index.html');
    mainWindow.loadFile(indexPath);
  }

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

ipcMain.handle('torrent:start', async (_event, torrentSource, fileName) => {
  console.log('[Torrent] Starting torrent download...');
  console.log('[Torrent] Source type:', typeof torrentSource, 'isArray:', Array.isArray(torrentSource));

  const client = await initTorrentClient();

  if (!client) {
    console.error('[Torrent] WebTorrent client not available');
    return { success: false, error: 'WebTorrent not available' };
  }

  const torrentId = `torrent-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
  const downloadsPath = getDownloadsPath();
  console.log('[Torrent] Downloads path:', downloadsPath);
  console.log('[Torrent] Torrent ID:', torrentId);

  try {
    // Convert array back to Buffer if needed (for torrent file data)
    let source = torrentSource;
    if (Array.isArray(torrentSource)) {
      console.log('[Torrent] Converting array to Buffer, length:', torrentSource.length);
      source = Buffer.from(torrentSource);
    } else if (torrentSource instanceof Uint8Array) {
      source = Buffer.from(torrentSource);
    }

    console.log('[Torrent] Adding torrent to client...');

    // Add torrent with options
    const torrent = client.add(source, {
      path: downloadsPath,
      announce: [
        'udp://tracker.opentrackr.org:1337/announce',
        'udp://tracker.openbittorrent.com:6969/announce',
        'udp://open.stealth.si:80/announce',
        'udp://tracker.torrent.eu.org:451/announce',
        'udp://exodus.desync.com:6969/announce',
        'wss://tracker.openwebtorrent.com',
        'wss://tracker.btorrent.xyz',
      ]
    });

    activeTorrents.set(torrentId, torrent);
    console.log('[Torrent] Torrent added, waiting for metadata...');

    // Send initial progress immediately
    mainWindow?.webContents.send('torrent:progress', {
      id: torrentId,
      name: fileName || 'Loading...',
      progress: 0,
      downloadSpeed: 0,
      numPeers: 0,
      status: 'connecting',
    });

    // Torrent ready event
    torrent.on('ready', () => {
      console.log('[Torrent] Torrent ready:', torrent.name);
      mainWindow?.webContents.send('torrent:progress', {
        id: torrentId,
        name: torrent.name,
        progress: 0,
        downloadSpeed: 0,
        numPeers: torrent.numPeers,
        status: 'downloading',
      });
    });

    // Send initial info when metadata received
    torrent.on('metadata', () => {
      console.log('[Torrent] Metadata received:', torrent.name);
      console.log('[Torrent] Files:', torrent.files.map(f => f.name));
      mainWindow?.webContents.send('torrent:progress', {
        id: torrentId,
        name: torrent.name,
        progress: 0,
        downloadSpeed: 0,
        numPeers: torrent.numPeers,
        status: 'downloading',
      });
    });

    // Progress updates - throttled
    let lastProgressUpdate = 0;
    torrent.on('download', () => {
      const now = Date.now();
      if (now - lastProgressUpdate < 500) return; // Throttle to every 500ms
      lastProgressUpdate = now;

      const progress = Math.round(torrent.progress * 100);
      console.log(`[Torrent] Progress: ${progress}%, Peers: ${torrent.numPeers}, Speed: ${(torrent.downloadSpeed / 1024).toFixed(1)} KB/s`);

      mainWindow?.webContents.send('torrent:progress', {
        id: torrentId,
        name: torrent.name,
        progress,
        downloadSpeed: torrent.downloadSpeed,
        numPeers: torrent.numPeers,
        status: 'downloading',
      });
    });

    // Wire connected (peer found)
    torrent.on('wire', (wire) => {
      console.log('[Torrent] Connected to peer:', wire.remoteAddress);
    });

    // Completion
    torrent.on('done', () => {
      console.log('[Torrent] Download complete:', torrent.name);

      // Find audio/ebook files in the torrent
      const mediaExtensions = ['.mp3', '.m4b', '.m4a', '.aac', '.ogg', '.flac', '.wav', '.epub', '.pdf'];
      const mediaFiles = torrent.files.filter((file) =>
        mediaExtensions.some((ext) => file.name.toLowerCase().endsWith(ext))
      );

      // Get file paths
      const filePaths = mediaFiles.map((file) => path.join(downloadsPath, file.path));
      console.log('[Torrent] Media files found:', filePaths);

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
      console.error('[Torrent] Error:', err);
      mainWindow?.webContents.send('torrent:error', {
        id: torrentId,
        error: err.message,
      });
      activeTorrents.delete(torrentId);
    });

    // Warning (non-fatal)
    torrent.on('warning', (warn) => {
      console.warn('[Torrent] Warning:', warn);
    });

    // Return success immediately (download happens async)
    const displayName = torrent.name || fileName || (typeof torrentSource === 'string' ? torrentSource.slice(0, 50) : 'Unknown');
    console.log('[Torrent] Started successfully, ID:', torrentId, 'Name:', displayName);

    return {
      success: true,
      id: torrentId,
      name: displayName,
    };
  } catch (error) {
    console.error('[Torrent] Error starting torrent:', error);
    return { success: false, error: error.message };
  }
});

ipcMain.handle('torrent:cancel', async (_event, torrentId) => {
  console.log('[Torrent] Cancelling torrent:', torrentId);
  const torrent = activeTorrents.get(torrentId);

  if (torrent) {
    torrent.destroy();
    activeTorrents.delete(torrentId);
    return { success: true };
  }

  return { success: false, error: 'Torrent not found' };
});

app.whenReady().then(() => {
  console.log('[App] Starting Rezon...');
  console.log('[App] User data path:', app.getPath('userData'));
  console.log('[App] Downloads path:', getDownloadsPath());
  createWindow();
});

app.on('window-all-closed', () => {
  // Clean up torrent client
  if (torrentClient) {
    console.log('[App] Cleaning up torrent client...');
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
