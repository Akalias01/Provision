const { app, BrowserWindow } = require('electron');

// Disable GPU acceleration - fixes black screen on some systems
app.disableHardwareAcceleration();
app.commandLine.appendSwitch('disable-gpu');
app.commandLine.appendSwitch('disable-software-rasterizer');

let mainWindow;

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1200,
    height: 800,
    x: 100,
    y: 100,
    backgroundColor: '#ffffff',
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
    },
  });

  // Load a simple test page directly
  mainWindow.loadURL(`data:text/html;charset=utf-8,
    <!DOCTYPE html>
    <html>
    <head>
      <title>VOCA Test</title>
    </head>
    <body style="background: white; color: black; font-family: Arial; padding: 50px;">
      <h1 style="color: green; font-size: 60px;">ELECTRON IS WORKING!</h1>
      <p style="font-size: 24px;">If you see this text, the GPU fix worked.</p>
    </body>
    </html>
  `);

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
