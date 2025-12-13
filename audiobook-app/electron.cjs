const { app, BrowserWindow } = require('electron');
const path = require('path');

let mainWindow;

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1400,
    height: 900,
    minWidth: 800,
    minHeight: 600,
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      webSecurity: false,
    },
    backgroundColor: '#09090b',
    show: false,
    autoHideMenuBar: true,
  });

  // Load the app - always try dist first in production
  const indexPath = path.join(__dirname, 'dist', 'index.html');

  mainWindow.loadFile(indexPath).catch((err) => {
    console.error('Failed to load:', err);
    // Fallback to dev server if dist doesn't exist
    mainWindow.loadURL('http://localhost:5173');
  });

  // Show window when ready
  mainWindow.once('ready-to-show', () => {
    mainWindow.show();
    mainWindow.focus();
  });

  // Handle load failures
  mainWindow.webContents.on('did-fail-load', () => {
    console.log('Failed to load, retrying...');
    mainWindow.loadFile(indexPath);
  });

  mainWindow.on('closed', () => {
    mainWindow = null;
  });
}

app.whenReady().then(createWindow);

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});

app.on('activate', () => {
  if (BrowserWindow.getAllWindows().length === 0) {
    createWindow();
  }
});
