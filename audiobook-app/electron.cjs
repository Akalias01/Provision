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
    show: false,
    autoHideMenuBar: true,
  });

  // Determine the correct path for both dev and production
  const isDev = !app.isPackaged;

  let indexPath;
  if (isDev) {
    // Development: use __dirname
    indexPath = path.join(__dirname, 'dist', 'index.html');
  } else {
    // Production: packaged app - files are in resources/app/dist
    indexPath = path.join(process.resourcesPath, 'app', 'dist', 'index.html');

    // Fallback: try app.getAppPath()
    if (!fs.existsSync(indexPath)) {
      indexPath = path.join(app.getAppPath(), 'dist', 'index.html');
    }
  }

  console.log('Loading from:', indexPath);
  console.log('File exists:', fs.existsSync(indexPath));
  console.log('App path:', app.getAppPath());
  console.log('Is packaged:', app.isPackaged);

  // Load the file
  if (fs.existsSync(indexPath)) {
    mainWindow.loadFile(indexPath).then(() => {
      console.log('Loaded successfully');
    }).catch((err) => {
      console.error('Failed to load file:', err);
      // Show error in window
      mainWindow.loadURL(`data:text/html,<h1>Error loading app</h1><p>${err.message}</p><p>Path: ${indexPath}</p>`);
    });
  } else {
    // File doesn't exist - try dev server or show error
    console.log('Index file not found, trying dev server...');
    mainWindow.loadURL('http://localhost:5173').catch(() => {
      mainWindow.loadURL(`data:text/html,
        <html>
          <body style="background:#09090b;color:white;font-family:sans-serif;padding:40px;">
            <h1>VOCA - Loading Error</h1>
            <p>Could not find the app files.</p>
            <p>Tried path: ${indexPath}</p>
            <p>App path: ${app.getAppPath()}</p>
            <p>Resources path: ${process.resourcesPath}</p>
          </body>
        </html>
      `);
    });
  }

  // Show window when ready
  mainWindow.once('ready-to-show', () => {
    mainWindow.show();
    mainWindow.focus();

    // Open DevTools in dev mode for debugging
    if (isDev) {
      mainWindow.webContents.openDevTools();
    }
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
