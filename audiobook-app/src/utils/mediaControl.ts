/**
 * Media Control Plugin for Android Auto integration
 * Provides bridge between web app and native Android media services
 */

import { Capacitor, registerPlugin } from '@capacitor/core';

// Define the plugin interface
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

// Register the plugin
const MediaControl = registerPlugin<MediaControlPlugin>('MediaControl');

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
