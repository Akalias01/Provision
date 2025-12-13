const { app, BrowserWindow } = require('electron');

let mainWindow;

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1200,
    height: 800,
    backgroundColor: '#1e1e2e',
  });

  // Load a simple test page directly - no files, no server
  mainWindow.loadURL(`data:text/html;charset=utf-8,
    <!DOCTYPE html>
    <html>
    <head>
      <title>VOCA Test</title>
    </head>
    <body style="background: #1e1e2e; color: white; font-family: Arial; padding: 50px;">
      <h1 style="color: #00ff00; font-size: 60px;">ELECTRON IS WORKING!</h1>
      <p style="font-size: 24px;">If you see this green text, Electron can display content.</p>
      <p style="color: #00ffff; font-size: 18px;">App path: ${app.getAppPath()}</p>
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
