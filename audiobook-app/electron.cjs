const { app, BrowserWindow } = require('electron');
const path = require('path');
const fs = require('fs');

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
    // Show immediately for debugging
    show: true,
    autoHideMenuBar: false,
  });

  // ALWAYS open DevTools for debugging
  mainWindow.webContents.openDevTools();

  // Try to find the index.html
  const distPath = path.join(__dirname, 'dist', 'index.html');

  console.log('==========================================');
  console.log('VOCA Electron Debug Info:');
  console.log('__dirname:', __dirname);
  console.log('Looking for:', distPath);
  console.log('File exists:', fs.existsSync(distPath));
  console.log('app.isPackaged:', app.isPackaged);
  console.log('app.getAppPath():', app.getAppPath());
  console.log('==========================================');

  if (fs.existsSync(distPath)) {
    console.log('Loading file:', distPath);
    mainWindow.loadFile(distPath);
  } else {
    console.log('dist/index.html not found! Trying dev server...');
    mainWindow.loadURL('http://localhost:5173').catch((err) => {
      console.error('Dev server also failed:', err);
      mainWindow.loadURL(`data:text/html,
        <html>
        <head><title>VOCA Error</title></head>
        <body style="background:#1a1a2e;color:white;font-family:system-ui;padding:40px;">
          <h1 style="color:#f43f5e;">VOCA - File Not Found</h1>
          <p>Could not find: <code>${distPath}</code></p>
          <p>Please run <code>npm run build</code> first.</p>
          <h3>Debug Info:</h3>
          <pre style="background:#0d0d1a;padding:20px;border-radius:8px;">
__dirname: ${__dirname}
app.getAppPath(): ${app.getAppPath()}
          </pre>
        </body>
        </html>
      `);
    });
  }

  // Log when content loads
  mainWindow.webContents.on('did-finish-load', () => {
    console.log('Page finished loading!');
  });

  mainWindow.webContents.on('did-fail-load', (event, errorCode, errorDescription) => {
    console.error('Failed to load:', errorCode, errorDescription);
  });

  // Log any console messages from the renderer
  mainWindow.webContents.on('console-message', (event, level, message, line, sourceId) => {
    console.log('Renderer:', message);
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
