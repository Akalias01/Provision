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
    useLegacyBridge: false,
  },
  plugins: {
    SplashScreen: {
      launchShowDuration: 1500,
      launchAutoHide: true,
      backgroundColor: '#0a0a0f',
      showSpinner: false,
      androidScaleType: 'FIT_CENTER',
      splashFullScreen: true,
      splashImmersive: true,
    },
  },
  server: {
    androidScheme: 'https',
    cleartext: true,
  },
};

export default config;
