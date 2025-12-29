import * as musicMetadata from 'music-metadata-browser';
import type { Chapter } from '../types';

export interface AudioBookMetadata {
  title?: string;
  author?: string;
  narrator?: string;
  description?: string;
  coverUrl?: string;
  duration?: number;
  chapters: Chapter[];
}

// Extract chapters from M4B/MP4 native metadata
function extractM4BChapters(nativeMetadata: Record<string, Array<{ id: string; value: unknown }>>, duration: number): Chapter[] {
  const chapters: Chapter[] = [];

  // Try iTunes native metadata first
  const itunesTags = nativeMetadata['iTunes'] || nativeMetadata['m4a'] || nativeMetadata['mp4'] || [];

  console.log('[AudioMetadata] Available native formats:', Object.keys(nativeMetadata));
  console.log('[AudioMetadata] iTunes/M4A tags count:', itunesTags.length);

  // Look for chapter markers - M4B stores chapters in different ways
  for (const tag of itunesTags) {
    console.log('[AudioMetadata] Tag:', tag.id, typeof tag.value);

    // Chapter list (chpl atom)
    if (tag.id === 'chpl' || tag.id === '©chp') {
      const chapterList = tag.value as Array<{ title: string; startTime: number }>;
      if (Array.isArray(chapterList)) {
        chapterList.forEach((ch, index) => {
          chapters.push({
            id: crypto.randomUUID(),
            title: ch.title || `Chapter ${index + 1}`,
            startTime: ch.startTime / 1000, // Convert ms to seconds
            duration: 0, // Will be calculated later
          });
        });
      }
    }
  }

  // If no chapters found in iTunes tags, try the 'chapter' native format
  const chapterTags = nativeMetadata['chapter'] || [];
  if (chapterTags.length > 0 && chapters.length === 0) {
    console.log('[AudioMetadata] Found chapter tags:', chapterTags.length);
    chapterTags.forEach((tag, index) => {
      const chapterData = tag.value as { title?: string; startTime?: number; endTime?: number };
      chapters.push({
        id: crypto.randomUUID(),
        title: chapterData.title || tag.id || `Chapter ${index + 1}`,
        startTime: (chapterData.startTime || 0) / 1000,
        duration: chapterData.endTime ? (chapterData.endTime - (chapterData.startTime || 0)) / 1000 : 0,
      });
    });
  }

  // Calculate durations if not set
  if (chapters.length > 0) {
    for (let i = 0; i < chapters.length; i++) {
      const chapter = chapters[i];
      const nextChapter = chapters[i + 1];
      if (chapter && (chapter.duration === 0 || chapter.duration === undefined)) {
        const chapterStart = chapter.startTime ?? 0;
        if (i < chapters.length - 1 && nextChapter) {
          const nextStart = nextChapter.startTime ?? 0;
          chapter.duration = nextStart - chapterStart;
        } else {
          chapter.duration = duration - chapterStart;
        }
      }
    }
  }

  return chapters;
}

// Extract chapters from MP3 ID3v2 CHAP frames
function extractMP3Chapters(nativeMetadata: Record<string, Array<{ id: string; value: unknown }>>, duration: number): Chapter[] {
  const chapters: Chapter[] = [];

  // Try ID3v2.3 or ID3v2.4
  const id3Tags = nativeMetadata['ID3v2.4'] || nativeMetadata['ID3v2.3'] || nativeMetadata['ID3v2.2'] || [];

  console.log('[AudioMetadata] ID3 tags count:', id3Tags.length);

  for (const tag of id3Tags) {
    // CHAP frame contains chapter information
    if (tag.id === 'CHAP') {
      const chapData = tag.value as {
        elementID?: string;
        startTimeMs?: number;
        endTimeMs?: number;
        tags?: Array<{ id: string; value: string }>;
      };

      let title = `Chapter ${chapters.length + 1}`;

      // Get title from sub-tags
      if (chapData.tags) {
        const titleTag = chapData.tags.find(t => t.id === 'TIT2');
        if (titleTag) {
          title = titleTag.value;
        }
      }

      chapters.push({
        id: crypto.randomUUID(),
        title,
        startTime: (chapData.startTimeMs || 0) / 1000,
        duration: chapData.endTimeMs && chapData.startTimeMs
          ? (chapData.endTimeMs - chapData.startTimeMs) / 1000
          : 0,
      });
    }
  }

  // Calculate durations if not set
  if (chapters.length > 0) {
    for (let i = 0; i < chapters.length; i++) {
      const chapter = chapters[i];
      const nextChapter = chapters[i + 1];
      if (chapter && (chapter.duration === 0 || chapter.duration === undefined)) {
        const chapterStart = chapter.startTime ?? 0;
        if (i < chapters.length - 1 && nextChapter) {
          const nextStart = nextChapter.startTime ?? 0;
          chapter.duration = nextStart - chapterStart;
        } else {
          chapter.duration = duration - chapterStart;
        }
      }
    }
  }

  return chapters;
}

// Generate automatic chapters based on duration
function generateAutoChapters(duration: number): Chapter[] {
  const chapters: Chapter[] = [];
  const CHAPTER_DURATION = 30 * 60; // 30 minutes

  // For very short files (< 10 min), create single chapter
  if (duration < 600) {
    return [{
      id: crypto.randomUUID(),
      title: 'Chapter 1',
      startTime: 0,
      duration: duration,
    }];
  }

  // For longer files, create 30-minute chapters
  const numChapters = Math.max(1, Math.ceil(duration / CHAPTER_DURATION));
  const chapterLength = duration / numChapters;

  for (let i = 0; i < numChapters; i++) {
    chapters.push({
      id: crypto.randomUUID(),
      title: `Chapter ${i + 1}`,
      startTime: i * chapterLength,
      duration: i === numChapters - 1 ? duration - (i * chapterLength) : chapterLength,
    });
  }

  return chapters;
}

// Main function to extract all audiobook metadata from a file
export async function extractAudioMetadata(file: File): Promise<AudioBookMetadata> {
  const result: AudioBookMetadata = {
    chapters: [],
  };

  try {
    console.log('[AudioMetadata] Parsing file:', file.name, 'Type:', file.type, 'Size:', file.size);

    const metadata = await musicMetadata.parseBlob(file, {
      duration: true,
      skipCovers: false,
    });

    console.log('[AudioMetadata] Parsed metadata:', {
      format: metadata.format,
      common: {
        title: metadata.common.title,
        artist: metadata.common.artist,
        album: metadata.common.album,
        hasPicture: metadata.common.picture?.length || 0,
      },
      nativeFormats: Object.keys(metadata.native || {}),
    });

    // Extract basic info
    if (metadata.common.title) {
      result.title = metadata.common.title;
    }

    // Author can be artist, albumartist, or composer
    result.author = metadata.common.artist
      || metadata.common.albumartist
      || metadata.common.composer?.[0]
      || undefined;

    // Description from comment or description tags
    if (metadata.common.comment) {
      result.description = Array.isArray(metadata.common.comment)
        ? metadata.common.comment[0]
        : metadata.common.comment;
    }

    // Duration
    if (metadata.format.duration) {
      result.duration = metadata.format.duration;
    }

    // Extract cover art
    if (metadata.common.picture && metadata.common.picture.length > 0) {
      const picture = metadata.common.picture[0];
      console.log('[AudioMetadata] Found cover art:', picture.format, 'Size:', picture.data.length);

      try {
        // Convert Buffer to Uint8Array for Blob compatibility
        const uint8Array = new Uint8Array(picture.data);
        const blob = new Blob([uint8Array], { type: picture.format || 'image/jpeg' });
        result.coverUrl = URL.createObjectURL(blob);
        console.log('[AudioMetadata] Created cover URL:', result.coverUrl);
      } catch (e) {
        console.error('[AudioMetadata] Error creating cover URL:', e);
      }
    }

    // Extract chapters based on format
    const duration = result.duration || 0;
    const nativeMetadata = metadata.native || {};

    // Determine format and extract chapters accordingly
    const container = metadata.format.container?.toLowerCase() || '';
    const codec = metadata.format.codec?.toLowerCase() || '';

    console.log('[AudioMetadata] Container:', container, 'Codec:', codec);

    if (container.includes('m4a') || container.includes('mp4') || file.name.toLowerCase().endsWith('.m4b')) {
      // M4B/M4A/MP4 format
      result.chapters = extractM4BChapters(nativeMetadata, duration);
    } else if (container.includes('mpeg') || codec.includes('mp3')) {
      // MP3 format
      result.chapters = extractMP3Chapters(nativeMetadata, duration);
    }

    // If no chapters found, try all native formats
    if (result.chapters.length === 0 && Object.keys(nativeMetadata).length > 0) {
      console.log('[AudioMetadata] No chapters found, trying all native formats...');

      // Log all available tags for debugging
      for (const [format, tags] of Object.entries(nativeMetadata)) {
        console.log(`[AudioMetadata] ${format} has ${tags.length} tags`);
        // Log first few tags
        tags.slice(0, 10).forEach(tag => {
          console.log(`  - ${tag.id}:`, typeof tag.value === 'object' ? JSON.stringify(tag.value).substring(0, 100) : tag.value);
        });
      }
    }

    // Generate automatic chapters if none found
    if (result.chapters.length === 0 && duration > 0) {
      console.log('[AudioMetadata] No chapters in metadata, generating automatic chapters');
      result.chapters = generateAutoChapters(duration);
    }

    console.log('[AudioMetadata] Final result:', {
      title: result.title,
      author: result.author,
      hasCover: !!result.coverUrl,
      duration: result.duration,
      chaptersCount: result.chapters.length,
    });

  } catch (error) {
    console.error('[AudioMetadata] Error parsing metadata:', error);

    // Fallback: try to get duration from Audio element
    try {
      const url = URL.createObjectURL(file);
      const audio = new Audio(url);

      await new Promise<void>((resolve, reject) => {
        audio.onloadedmetadata = () => {
          result.duration = audio.duration;
          if (audio.duration > 0) {
            result.chapters = generateAutoChapters(audio.duration);
          }
          URL.revokeObjectURL(url);
          resolve();
        };
        audio.onerror = () => {
          URL.revokeObjectURL(url);
          reject(new Error('Failed to load audio'));
        };
      });
    } catch (e) {
      console.error('[AudioMetadata] Fallback also failed:', e);
    }
  }

  return result;
}
