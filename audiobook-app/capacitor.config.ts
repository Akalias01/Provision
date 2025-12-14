import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.rezon.app',
  appName: 'Rezon',
  webDir: 'dist',
  android: {
    backgroundColor: '#0a0a0f',
    allowMixedContent: true,
    captureInput: true,
    webContentsDebuggingEnabled: true,
  },
  plugins: {
    SplashScreen: {
      launchShowDuration: 2000,
      backgroundColor: '#0a0a0f',
      showSpinner: false,
      androidScaleType: 'CENTER_CROP',
    },
  },
  server: {
    androidScheme: 'https',
  },
};

export default config;
