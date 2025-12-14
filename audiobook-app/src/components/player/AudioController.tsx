import { useRef, useEffect } from 'react';
import { useStore } from '../../store/useStore';
import { updateMediaMetadata, updatePlaybackState, isAndroid } from '../../utils/mediaControl';

/**
 * Global audio controller that persists across view changes.
 * This component manages the actual audio playback while the AudioPlayer
 * and MiniPlayer components just control the UI state.
 */
export function AudioController() {
  const audioRef = useRef<HTMLAudioElement>(null);
  const currentBookIdRef = useRef<string | null>(null);

  const {
    currentBook,
    playerState,
    setPlayerState,
    updateBook,
  } = useStore();

  const { isPlaying, currentTime, duration, volume, playbackRate, isMuted } = playerState;

  // Load audio when book changes
  useEffect(() => {
    const audio = audioRef.current;
    if (!audio || !currentBook) return;

    // Only reload if book changed
    if (currentBookIdRef.current === currentBook.id && audio.src) {
      return;
    }

    currentBookIdRef.current = currentBook.id;

    audio.pause();
    audio.currentTime = 0;
    audio.src = currentBook.fileUrl;
    audio.volume = isMuted ? 0 : volume;
    audio.playbackRate = playbackRate;

    const handleLoadedMetadata = () => {
      setPlayerState({ duration: audio.duration });
      if (currentBook.currentPosition > 0 && audio.duration > 0) {
        audio.currentTime = currentBook.currentPosition * audio.duration;
      }
    };

    const handleTimeUpdate = () => {
      setPlayerState({ currentTime: audio.currentTime });

      // Save position periodically
      if (currentBook && audio.duration > 0) {
        const position = audio.currentTime / audio.duration;
        // Only update if position changed significantly (every ~10 seconds)
        if (Math.abs(position - (currentBook.currentPosition || 0)) > 0.01) {
          updateBook(currentBook.id, { currentPosition: position });
        }
      }
    };

    const handleEnded = () => {
      setPlayerState({ isPlaying: false });
      updateBook(currentBook.id, { isFinished: true, currentPosition: 1 });
    };

    const handleError = () => {
      console.error('[AudioController] Audio error');
      setPlayerState({ isPlaying: false });
    };

    audio.addEventListener('loadedmetadata', handleLoadedMetadata);
    audio.addEventListener('timeupdate', handleTimeUpdate);
    audio.addEventListener('ended', handleEnded);
    audio.addEventListener('error', handleError);
    audio.load();

    return () => {
      audio.removeEventListener('loadedmetadata', handleLoadedMetadata);
      audio.removeEventListener('timeupdate', handleTimeUpdate);
      audio.removeEventListener('ended', handleEnded);
      audio.removeEventListener('error', handleError);
    };
  }, [currentBook?.id, currentBook?.fileUrl]);

  // Play/pause control
  useEffect(() => {
    const audio = audioRef.current;
    if (!audio || !currentBook) return;

    if (isPlaying) {
      const attemptPlay = () => {
        if (audio.readyState >= 2) {
          audio.play().catch(() => setPlayerState({ isPlaying: false }));
        } else {
          const handleCanPlay = () => {
            audio.removeEventListener('canplay', handleCanPlay);
            audio.play().catch(() => setPlayerState({ isPlaying: false }));
          };
          audio.addEventListener('canplay', handleCanPlay);
        }
      };
      attemptPlay();
    } else {
      audio.pause();
    }
  }, [isPlaying, currentBook?.id]);

  // Volume control
  useEffect(() => {
    if (audioRef.current) {
      audioRef.current.volume = isMuted ? 0 : volume;
    }
  }, [volume, isMuted]);

  // Playback rate control
  useEffect(() => {
    if (audioRef.current) {
      audioRef.current.playbackRate = playbackRate;
    }
  }, [playbackRate]);

  // Seek control - listen for seek requests
  useEffect(() => {
    const handleSeek = (e: CustomEvent<number>) => {
      if (audioRef.current) {
        audioRef.current.currentTime = e.detail;
      }
    };

    window.addEventListener('audio-seek' as keyof WindowEventMap, handleSeek as EventListener);
    return () => {
      window.removeEventListener('audio-seek' as keyof WindowEventMap, handleSeek as EventListener);
    };
  }, []);

  // Android Auto metadata
  useEffect(() => {
    if (isAndroid() && currentBook && isPlaying && duration > 0) {
      updateMediaMetadata(
        currentBook.title,
        currentBook.author || 'Unknown Author',
        currentBook.cover,
        duration
      );
    }
  }, [currentBook?.id, isPlaying, duration]);

  // Android Auto playback state
  useEffect(() => {
    if (isAndroid() && currentBook && (isPlaying || currentTime > 0)) {
      updatePlaybackState(isPlaying, currentTime, playbackRate);
    }
  }, [isPlaying, currentBook?.id]);

  return (
    <audio
      ref={audioRef}
      preload="metadata"
      crossOrigin="anonymous"
      style={{ display: 'none' }}
    />
  );
}

// Helper function to seek audio from anywhere in the app
export function seekAudio(time: number) {
  window.dispatchEvent(new CustomEvent('audio-seek', { detail: time }));
}
