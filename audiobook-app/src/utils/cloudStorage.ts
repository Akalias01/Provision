import { Dropbox, DropboxAuth } from 'dropbox';

// Cloud file type
export interface CloudFile {
  id: string;
  name: string;
  path: string;
  isFolder: boolean;
  size?: number;
  modifiedTime?: Date;
  mimeType?: string;
  provider: 'google' | 'dropbox';
}

// Supported file extensions for cloud storage
const SUPPORTED_EXTENSIONS = [
  '.mp3', '.m4b', '.m4a', '.aac', '.ogg', '.flac', '.wav',
  '.epub', '.pdf', '.doc', '.docx'
];

function isSupportedFile(filename: string): boolean {
  const ext = filename.toLowerCase().slice(filename.lastIndexOf('.'));
  return SUPPORTED_EXTENSIONS.includes(ext);
}

// ==================== GOOGLE DRIVE ====================

// Google Drive configuration
// To use Google Drive:
// 1. Create a project in Google Cloud Console
// 2. Enable the Google Drive API
// 3. Create OAuth 2.0 credentials (Web application)
// 4. Set the authorized redirect URI to your app's domain
// 5. Replace the CLIENT_ID below with your credentials
const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID || '';
const GOOGLE_API_KEY = import.meta.env.VITE_GOOGLE_API_KEY || '';
const GOOGLE_SCOPES = 'https://www.googleapis.com/auth/drive.readonly';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
let googleTokenClient: any = null;
let googleAccessToken: string | null = null;

// Load the Google API script
export async function loadGoogleApi(): Promise<boolean> {
  return new Promise((resolve) => {
    if (!GOOGLE_CLIENT_ID) {
      console.warn('[CloudStorage] Google Client ID not configured');
      resolve(false);
      return;
    }

    // Check if already loaded
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const w = window as any;
    if (w.gapi && w.google?.accounts) {
      resolve(true);
      return;
    }

    // Load GAPI
    const gapiScript = document.createElement('script');
    gapiScript.src = 'https://apis.google.com/js/api.js';
    gapiScript.async = true;
    gapiScript.defer = true;

    // Load GSI (Google Sign-In)
    const gsiScript = document.createElement('script');
    gsiScript.src = 'https://accounts.google.com/gsi/client';
    gsiScript.async = true;
    gsiScript.defer = true;

    let gapiLoaded = false;
    let gsiLoaded = false;

    const checkBothLoaded = () => {
      if (gapiLoaded && gsiLoaded) {
        // Initialize GAPI client
        w.gapi.load('client', async () => {
          try {
            await w.gapi.client.init({
              apiKey: GOOGLE_API_KEY,
              discoveryDocs: ['https://www.googleapis.com/discovery/v1/apis/drive/v3/rest'],
            });
            resolve(true);
          } catch (error) {
            console.error('[CloudStorage] Failed to initialize Google API:', error);
            resolve(false);
          }
        });
      }
    };

    gapiScript.onload = () => {
      gapiLoaded = true;
      checkBothLoaded();
    };

    gsiScript.onload = () => {
      gsiLoaded = true;
      checkBothLoaded();
    };

    gapiScript.onerror = () => resolve(false);
    gsiScript.onerror = () => resolve(false);

    document.head.appendChild(gapiScript);
    document.head.appendChild(gsiScript);

    // Timeout after 10 seconds
    setTimeout(() => resolve(false), 10000);
  });
}

export async function connectGoogleDrive(): Promise<boolean> {
  if (!GOOGLE_CLIENT_ID) {
    console.error('[CloudStorage] Google Client ID not configured');
    return false;
  }

  const loaded = await loadGoogleApi();
  if (!loaded) return false;

  return new Promise((resolve) => {
    try {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const w = window as any;
      googleTokenClient = w.google.accounts.oauth2.initTokenClient({
        client_id: GOOGLE_CLIENT_ID,
        scope: GOOGLE_SCOPES,
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        callback: (response: any) => {
          if (response.access_token) {
            googleAccessToken = response.access_token;
            resolve(true);
          } else {
            resolve(false);
          }
        },
      });

      googleTokenClient.requestAccessToken({ prompt: 'consent' });
    } catch (error) {
      console.error('[CloudStorage] Google Drive connection error:', error);
      resolve(false);
    }
  });
}

export function isGoogleDriveConnected(): boolean {
  return googleAccessToken !== null;
}

export async function listGoogleDriveFiles(folderId: string = 'root'): Promise<CloudFile[]> {
  if (!googleAccessToken) {
    throw new Error('Not connected to Google Drive');
  }

  try {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const w = window as any;
    const response = await w.gapi.client.drive.files.list({
      q: `'${folderId}' in parents and trashed = false`,
      fields: 'files(id, name, mimeType, size, modifiedTime)',
      orderBy: 'folder,name',
    });

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const files: CloudFile[] = response.result.files
      .filter((file: { name: string; mimeType: string }) =>
        file.mimeType === 'application/vnd.google-apps.folder' || isSupportedFile(file.name)
      )
      .map((file: { id: string; name: string; mimeType: string; size?: string; modifiedTime?: string }) => ({
        id: file.id,
        name: file.name,
        path: file.id,
        isFolder: file.mimeType === 'application/vnd.google-apps.folder',
        size: file.size ? parseInt(file.size, 10) : undefined,
        modifiedTime: file.modifiedTime ? new Date(file.modifiedTime) : undefined,
        mimeType: file.mimeType,
        provider: 'google' as const,
      }));

    return files;
  } catch (error) {
    console.error('[CloudStorage] Error listing Google Drive files:', error);
    throw error;
  }
}

export async function downloadGoogleDriveFile(fileId: string): Promise<Blob> {
  if (!googleAccessToken) {
    throw new Error('Not connected to Google Drive');
  }

  try {
    const response = await fetch(
      `https://www.googleapis.com/drive/v3/files/${fileId}?alt=media`,
      {
        headers: {
          Authorization: `Bearer ${googleAccessToken}`,
        },
      }
    );

    if (!response.ok) {
      throw new Error(`Failed to download file: ${response.statusText}`);
    }

    return response.blob();
  } catch (error) {
    console.error('[CloudStorage] Error downloading Google Drive file:', error);
    throw error;
  }
}

export function disconnectGoogleDrive(): void {
  googleAccessToken = null;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const w = window as any;
  if (w.google?.accounts) {
    w.google.accounts.id.disableAutoSelect();
  }
}

// ==================== DROPBOX ====================

// Dropbox configuration
// To use Dropbox:
// 1. Create an app at https://www.dropbox.com/developers/apps
// 2. Set the app type to "Scoped access"
// 3. Set permissions: files.metadata.read, files.content.read
// 4. Set the redirect URI to your app's domain
// 5. Replace the APP_KEY below with your app key
const DROPBOX_APP_KEY = import.meta.env.VITE_DROPBOX_APP_KEY || '';
const DROPBOX_REDIRECT_URI = window.location.origin + '/dropbox-callback';

let dropboxClient: Dropbox | null = null;
let dropboxAuth: DropboxAuth | null = null;
let dropboxAccessToken: string | null = null;

export async function getDropboxAuthUrl(): Promise<string> {
  if (!DROPBOX_APP_KEY) {
    throw new Error('Dropbox App Key not configured');
  }

  dropboxAuth = new DropboxAuth({ clientId: DROPBOX_APP_KEY });
  const url = await dropboxAuth.getAuthenticationUrl(
    DROPBOX_REDIRECT_URI,
    undefined,
    'code',
    'offline',
    undefined,
    undefined,
    true
  );
  return url as unknown as string;
}

export async function connectDropbox(authCode?: string): Promise<boolean> {
  if (!DROPBOX_APP_KEY) {
    console.error('[CloudStorage] Dropbox App Key not configured');
    return false;
  }

  try {
    // Check for stored token
    const storedToken = localStorage.getItem('dropbox_access_token');
    if (storedToken) {
      dropboxAccessToken = storedToken;
      dropboxClient = new Dropbox({ accessToken: storedToken });
      return true;
    }

    // If we have an auth code, exchange it for a token
    if (authCode) {
      // Initialize auth if needed
      if (!dropboxAuth) {
        dropboxAuth = new DropboxAuth({ clientId: DROPBOX_APP_KEY });
      }
      const response = await dropboxAuth.getAccessTokenFromCode(DROPBOX_REDIRECT_URI, authCode);
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const result = response.result as any;

      if (result.access_token) {
        const token: string = result.access_token;
        dropboxAccessToken = token;
        dropboxClient = new Dropbox({ accessToken: token });
        localStorage.setItem('dropbox_access_token', token);
        return true;
      }
    }

    return false;
  } catch (error) {
    console.error('[CloudStorage] Dropbox connection error:', error);
    return false;
  }
}

export function isDropboxConnected(): boolean {
  return dropboxClient !== null && dropboxAccessToken !== null;
}

export async function listDropboxFiles(path: string = ''): Promise<CloudFile[]> {
  if (!dropboxClient) {
    throw new Error('Not connected to Dropbox');
  }

  try {
    const response = await dropboxClient.filesListFolder({ path: path || '' });

    const files: CloudFile[] = response.result.entries
      .filter((entry) =>
        entry['.tag'] === 'folder' ||
        (entry['.tag'] === 'file' && isSupportedFile(entry.name))
      )
      .map((entry) => ({
        id: entry['.tag'] === 'file' ? (entry as { id: string }).id : entry.path_lower || '',
        name: entry.name,
        path: entry.path_lower || '',
        isFolder: entry['.tag'] === 'folder',
        size: entry['.tag'] === 'file' ? (entry as { size: number }).size : undefined,
        modifiedTime: entry['.tag'] === 'file' && (entry as { server_modified?: string }).server_modified
          ? new Date((entry as { server_modified: string }).server_modified)
          : undefined,
        provider: 'dropbox' as const,
      }));

    return files;
  } catch (error) {
    console.error('[CloudStorage] Error listing Dropbox files:', error);
    throw error;
  }
}

export async function downloadDropboxFile(path: string): Promise<Blob> {
  if (!dropboxClient) {
    throw new Error('Not connected to Dropbox');
  }

  try {
    const response = await dropboxClient.filesDownload({ path });
    // The response includes fileBlob for browser environments
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const result = response.result as any;
    if (result.fileBlob) {
      return result.fileBlob;
    }
    throw new Error('No file blob in response');
  } catch (error) {
    console.error('[CloudStorage] Error downloading Dropbox file:', error);
    throw error;
  }
}

export function disconnectDropbox(): void {
  dropboxClient = null;
  dropboxAuth = null;
  dropboxAccessToken = null;
  localStorage.removeItem('dropbox_access_token');
}

// ==================== UNIFIED INTERFACE ====================

export type CloudProvider = 'google' | 'dropbox';

export function isCloudConnected(provider: CloudProvider): boolean {
  switch (provider) {
    case 'google':
      return isGoogleDriveConnected();
    case 'dropbox':
      return isDropboxConnected();
    default:
      return false;
  }
}

export async function connectCloud(provider: CloudProvider): Promise<boolean> {
  switch (provider) {
    case 'google':
      return connectGoogleDrive();
    case 'dropbox':
      // For Dropbox, we need to redirect to auth URL
      const authUrl = await getDropboxAuthUrl();
      window.location.href = authUrl;
      return false; // Will complete after redirect
    default:
      return false;
  }
}

export async function listCloudFiles(provider: CloudProvider, folderId?: string): Promise<CloudFile[]> {
  switch (provider) {
    case 'google':
      return listGoogleDriveFiles(folderId);
    case 'dropbox':
      return listDropboxFiles(folderId);
    default:
      return [];
  }
}

export async function downloadCloudFile(provider: CloudProvider, fileIdOrPath: string, filename: string): Promise<File> {
  let blob: Blob;

  switch (provider) {
    case 'google':
      blob = await downloadGoogleDriveFile(fileIdOrPath);
      break;
    case 'dropbox':
      blob = await downloadDropboxFile(fileIdOrPath);
      break;
    default:
      throw new Error(`Unknown provider: ${provider}`);
  }

  return new File([blob], filename, { type: blob.type });
}

export function disconnectCloud(provider: CloudProvider): void {
  switch (provider) {
    case 'google':
      disconnectGoogleDrive();
      break;
    case 'dropbox':
      disconnectDropbox();
      break;
  }
}

// Check for Dropbox callback on page load
export function handleDropboxCallback(): boolean {
  const urlParams = new URLSearchParams(window.location.search);
  const code = urlParams.get('code');

  if (code && window.location.pathname.includes('dropbox-callback')) {
    connectDropbox(code).then((success) => {
      if (success) {
        // Clear the URL and redirect back to main page
        window.history.replaceState({}, document.title, '/');
        window.location.reload();
      }
    });
    return true;
  }
  return false;
}
