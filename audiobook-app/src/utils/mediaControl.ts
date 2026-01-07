/**
 * Media Control Plugin for Android Auto integration
 * Provides bridge between web app and native Android media services
 */

import { Capacitor, registerPlugin } from '@capacitor/core';

// Define the media control plugin interface
export interface MediaControlPlugin {
  updateMetadata(options: {
    title: string;
    author: string;
    coverUrl?: string;
    duration: number;
  }): Promise<{ success: boolean }>;

  updatePlaybackState(options: {
    isPlaying: boolean;
    position: number;
    speed: number;
  }): Promise<{ success: boolean }>;

  stopService(): Promise<{ success: boolean }>;
}

// Define the file picker plugin interface
export interface FilePickerPlugin {
  pickFiles(options: {
    type?: 'audio' | 'document' | 'all';
    multiple?: boolean;
  }): Promise<{
    files: Array<{
      uri: string;
      name: string;
      size: number;
      mimeType: string;
      extension?: string;
    }>;
  }>;

  readFile(options: { uri: string }): Promise<{
    data: string; // Base64 encoded
    uri: string;
  }>;

  getFileUrl(options: { uri: string }): Promise<{
    url: string;
  }>;
}

// Register the plugins
const MediaControl = registerPlugin<MediaControlPlugin>('MediaControl');
const FilePicker = registerPlugin<FilePickerPlugin>('FilePicker');

/**
 * Check if we're running on Android
 */
export function isAndroid(): boolean {
  return Capacitor.getPlatform() === 'android';
}

/**
 * Check if we're running in Capacitor (native app)
 */
export function isNative(): boolean {
  return Capacitor.isNativePlatform();
}

/**
 * Update media metadata for Android Auto and notification
 */
export async function updateMediaMetadata(
  title: string,
  author: string,
  coverUrl?: string,
  duration?: number
): Promise<void> {
  if (!isAndroid()) return;

  try {
    await MediaControl.updateMetadata({
      title,
      author,
      coverUrl: coverUrl || '',
      duration: duration ? Math.floor(duration * 1000) : 0, // Convert to milliseconds
    });
  } catch (error) {
    console.error('[MediaControl] Failed to update metadata:', error);
  }
}

/**
 * Update playback state for Android Auto
 */
export async function updatePlaybackState(
  isPlaying: boolean,
  position: number,
  speed: number = 1.0
): Promise<void> {
  if (!isAndroid()) return;

  try {
    await MediaControl.updatePlaybackState({
      isPlaying,
      position: Math.floor(position * 1000), // Convert to milliseconds
      speed,
    });
  } catch (error) {
    console.error('[MediaControl] Failed to update playback state:', error);
  }
}

/**
 * Stop the background playback service
 */
export async function stopPlaybackService(): Promise<void> {
  if (!isAndroid()) return;

  try {
    await MediaControl.stopService();
  } catch (error) {
    console.error('[MediaControl] Failed to stop service:', error);
  }
}

/**
 * Pick audio files from device storage
 */
export async function pickAudioFiles(): Promise<Array<{
  uri: string;
  name: string;
  size: number;
  mimeType: string;
}>> {
  if (!isAndroid()) return [];

  try {
    const result = await FilePicker.pickFiles({
      type: 'audio',
      multiple: true,
    });
    return result.files;
  } catch (error) {
    console.error('[FilePicker] Failed to pick audio files:', error);
    return [];
  }
}

/**
 * Pick document files (PDF, EPUB, DOC) from device storage
 */
export async function pickDocumentFiles(): Promise<Array<{
  uri: string;
  name: string;
  size: number;
  mimeType: string;
}>> {
  if (!isAndroid()) return [];

  try {
    const result = await FilePicker.pickFiles({
      type: 'document',
      multiple: true,
    });
    return result.files;
  } catch (error) {
    console.error('[FilePicker] Failed to pick document files:', error);
    return [];
  }
}

/**
 * Pick any supported files from device storage
 */
export async function pickAllFiles(): Promise<Array<{
  uri: string;
  name: string;
  size: number;
  mimeType: string;
}>> {
  if (!isAndroid()) return [];

  try {
    const result = await FilePicker.pickFiles({
      type: 'all',
      multiple: true,
    });
    return result.files;
  } catch (error) {
    console.error('[FilePicker] Failed to pick files:', error);
    return [];
  }
}

/**
 * Read a file as base64 data
 */
export async function readFileAsBase64(uri: string): Promise<string | null> {
  if (!isAndroid()) return null;

  try {
    const result = await FilePicker.readFile({ uri });
    return result.data;
  } catch (error) {
    console.error('[FilePicker] Failed to read file:', error);
    return null;
  }
}

/**
 * Get a usable URL for a file URI
 */
export async function getFileUrl(uri: string): Promise<string | null> {
  if (!isAndroid()) return null;

  try {
    const result = await FilePicker.getFileUrl({ uri });
    return result.url;
  } catch (error) {
    console.error('[FilePicker] Failed to get file URL:', error);
    return null;
  }
}

/**
 * Setup listener for media control events from notification/Android Auto
 */
export function setupMediaControlListener(callbacks: {
  onPlay?: () => void;
  onPause?: () => void;
  onNext?: () => void;
  onPrev?: () => void;
  onForward?: () => void;
  onRewind?: () => void;
  onSeek?: (position: number) => void;
}): () => void {
  const handler = (event: CustomEvent<{ action: string; position?: number }>) => {
    const { action, position } = event.detail;

    switch (action) {
      case 'com.rezon.PLAY':
      case 'com.rezon.ACTION_PLAY':
        callbacks.onPlay?.();
        break;
      case 'com.rezon.PAUSE':
      case 'com.rezon.ACTION_PAUSE':
        callbacks.onPause?.();
        break;
      case 'com.rezon.SKIP_NEXT':
      case 'com.rezon.ACTION_NEXT':
        callbacks.onNext?.();
        break;
      case 'com.rezon.SKIP_PREV':
      case 'com.rezon.ACTION_PREV':
        callbacks.onPrev?.();
        break;
      case 'com.rezon.FORWARD':
      case 'com.rezon.ACTION_FORWARD':
        callbacks.onForward?.();
        break;
      case 'com.rezon.REWIND':
      case 'com.rezon.ACTION_REWIND':
        callbacks.onRewind?.();
        break;
      case 'com.rezon.SEEK':
        if (position !== undefined) {
          callbacks.onSeek?.(position / 1000); // Convert from ms to seconds
        }
        break;
    }
  };

  window.addEventListener('mediaControl', handler as EventListener);

  // Return cleanup function
  return () => {
    window.removeEventListener('mediaControl', handler as EventListener);
  };
}
