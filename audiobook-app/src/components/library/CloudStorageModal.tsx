import { useState, useEffect, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Cloud,
  Folder,
  FileText,
  Headphones,
  BookOpen,
  ChevronRight,
  ArrowLeft,
  Download,
  Loader2,
  Check,
  X,
  HardDrive,
} from 'lucide-react';
import { Modal, Button } from '../ui';
import {
  type CloudProvider,
  type CloudFile,
  isCloudConnected,
  connectCloud,
  listCloudFiles,
  downloadCloudFile,
  disconnectCloud,
  handleDropboxCallback,
} from '../../utils/cloudStorage';

interface CloudStorageModalProps {
  isOpen: boolean;
  onClose: () => void;
  onFilesSelected: (files: File[]) => void;
}

interface BreadcrumbItem {
  id: string;
  name: string;
}

// Check if env vars are configured
const GOOGLE_CONFIGURED = !!import.meta.env.VITE_GOOGLE_CLIENT_ID;
const DROPBOX_CONFIGURED = !!import.meta.env.VITE_DROPBOX_APP_KEY;

export function CloudStorageModal({ isOpen, onClose, onFilesSelected }: CloudStorageModalProps) {
  const [selectedProvider, setSelectedProvider] = useState<CloudProvider | null>(null);
  const [isConnecting, setIsConnecting] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [files, setFiles] = useState<CloudFile[]>([]);
  const [selectedFiles, setSelectedFiles] = useState<Set<string>>(new Set());
  const [breadcrumbs, setBreadcrumbs] = useState<BreadcrumbItem[]>([{ id: '', name: 'Root' }]);
  const [error, setError] = useState<string | null>(null);
  const [isDownloading, setIsDownloading] = useState(false);
  const [downloadProgress, setDownloadProgress] = useState({ current: 0, total: 0 });

  // Check for Dropbox callback on mount
  useEffect(() => {
    handleDropboxCallback();
  }, []);

  const googleConnected = isCloudConnected('google');
  const dropboxConnected = isCloudConnected('dropbox');

  // Load files when provider is selected and connected
  useEffect(() => {
    if (selectedProvider && isCloudConnected(selectedProvider)) {
      loadFiles();
    }
  }, [selectedProvider]);

  const loadFiles = useCallback(async (folderId?: string) => {
    if (!selectedProvider) return;

    setIsLoading(true);
    setError(null);

    try {
      const fileList = await listCloudFiles(selectedProvider, folderId);
      setFiles(fileList);
    } catch (err) {
      setError('Failed to load files. Please try again.');
      console.error(err);
    } finally {
      setIsLoading(false);
    }
  }, [selectedProvider]);

  const handleConnect = async (provider: CloudProvider) => {
    setIsConnecting(true);
    setError(null);

    try {
      const success = await connectCloud(provider);
      if (success) {
        setSelectedProvider(provider);
      } else if (provider !== 'dropbox') {
        // Dropbox redirects, so no error for that
        setError('Failed to connect. Please try again.');
      }
    } catch (err) {
      setError('Connection failed. Please try again.');
      console.error(err);
    } finally {
      setIsConnecting(false);
    }
  };

  const handleDisconnect = (provider: CloudProvider) => {
    disconnectCloud(provider);
    if (selectedProvider === provider) {
      setSelectedProvider(null);
      setFiles([]);
      setBreadcrumbs([{ id: '', name: 'Root' }]);
    }
  };

  const handleFolderClick = (folder: CloudFile) => {
    setBreadcrumbs([...breadcrumbs, { id: folder.path, name: folder.name }]);
    loadFiles(folder.path);
  };

  const handleBreadcrumbClick = (index: number) => {
    const newBreadcrumbs = breadcrumbs.slice(0, index + 1);
    setBreadcrumbs(newBreadcrumbs);
    loadFiles(newBreadcrumbs[newBreadcrumbs.length - 1].id || undefined);
  };

  const handleFileSelect = (file: CloudFile) => {
    const newSelected = new Set(selectedFiles);
    if (newSelected.has(file.id)) {
      newSelected.delete(file.id);
    } else {
      newSelected.add(file.id);
    }
    setSelectedFiles(newSelected);
  };

  const handleSelectAll = () => {
    const fileItems = files.filter(f => !f.isFolder);
    if (selectedFiles.size === fileItems.length) {
      setSelectedFiles(new Set());
    } else {
      setSelectedFiles(new Set(fileItems.map(f => f.id)));
    }
  };

  const handleDownloadSelected = async () => {
    if (!selectedProvider || selectedFiles.size === 0) return;

    setIsDownloading(true);
    setDownloadProgress({ current: 0, total: selectedFiles.size });

    const downloadedFiles: File[] = [];
    const selectedFileObjects = files.filter(f => selectedFiles.has(f.id));

    for (let i = 0; i < selectedFileObjects.length; i++) {
      const file = selectedFileObjects[i];
      try {
        setDownloadProgress({ current: i + 1, total: selectedFiles.size });
        const downloadedFile = await downloadCloudFile(selectedProvider, file.path, file.name);
        downloadedFiles.push(downloadedFile);
      } catch (err) {
        console.error(`Failed to download ${file.name}:`, err);
      }
    }

    setIsDownloading(false);
    setDownloadProgress({ current: 0, total: 0 });

    if (downloadedFiles.length > 0) {
      onFilesSelected(downloadedFiles);
      onClose();
    }
  };

  const getFileIcon = (file: CloudFile) => {
    if (file.isFolder) return Folder;
    const ext = file.name.toLowerCase();
    if (ext.endsWith('.mp3') || ext.endsWith('.m4b') || ext.endsWith('.m4a') || ext.endsWith('.aac') || ext.endsWith('.ogg') || ext.endsWith('.flac') || ext.endsWith('.wav')) {
      return Headphones;
    }
    if (ext.endsWith('.epub')) return BookOpen;
    return FileText;
  };

  const formatFileSize = (bytes?: number) => {
    if (!bytes) return '';
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
    return `${(bytes / (1024 * 1024 * 1024)).toFixed(1)} GB`;
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Cloud Storage" size="lg">
      <div className="space-y-4">
        {/* Provider Selection */}
        {!selectedProvider && (
          <div className="space-y-4">
            <p className="text-sm text-surface-500">
              Connect your cloud storage to import audiobooks and ebooks directly.
            </p>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {/* Google Drive */}
              <div className="p-4 rounded-xl border-2 border-surface-200 dark:border-surface-700">
                <div className="flex items-center gap-3 mb-3">
                  <div className="w-10 h-10 rounded-lg bg-gradient-to-br from-blue-500 to-green-500 flex items-center justify-center">
                    <HardDrive className="w-5 h-5 text-white" />
                  </div>
                  <div>
                    <h3 className="font-medium text-surface-900 dark:text-white">Google Drive</h3>
                    <p className="text-xs text-surface-500">
                      {googleConnected ? 'Connected' : GOOGLE_CONFIGURED ? 'Click to connect' : 'Not configured'}
                    </p>
                  </div>
                </div>

                {googleConnected ? (
                  <div className="flex gap-2">
                    <Button
                      variant="primary"
                      className="flex-1"
                      onClick={() => setSelectedProvider('google')}
                    >
                      Browse Files
                    </Button>
                    <Button
                      variant="ghost"
                      onClick={() => handleDisconnect('google')}
                    >
                      <X className="w-4 h-4" />
                    </Button>
                  </div>
                ) : GOOGLE_CONFIGURED ? (
                  <Button
                    variant="secondary"
                    className="w-full"
                    onClick={() => handleConnect('google')}
                    disabled={isConnecting}
                  >
                    {isConnecting ? (
                      <>
                        <Loader2 className="w-4 h-4 animate-spin" />
                        Connecting...
                      </>
                    ) : (
                      <>
                        <Cloud className="w-4 h-4" />
                        Connect
                      </>
                    )}
                  </Button>
                ) : (
                  <p className="text-xs text-surface-400 text-center py-2">
                    Set VITE_GOOGLE_CLIENT_ID in .env to enable
                  </p>
                )}
              </div>

              {/* Dropbox */}
              <div className="p-4 rounded-xl border-2 border-surface-200 dark:border-surface-700">
                <div className="flex items-center gap-3 mb-3">
                  <div className="w-10 h-10 rounded-lg bg-gradient-to-br from-blue-600 to-blue-400 flex items-center justify-center">
                    <Cloud className="w-5 h-5 text-white" />
                  </div>
                  <div>
                    <h3 className="font-medium text-surface-900 dark:text-white">Dropbox</h3>
                    <p className="text-xs text-surface-500">
                      {dropboxConnected ? 'Connected' : DROPBOX_CONFIGURED ? 'Click to connect' : 'Not configured'}
                    </p>
                  </div>
                </div>

                {dropboxConnected ? (
                  <div className="flex gap-2">
                    <Button
                      variant="primary"
                      className="flex-1"
                      onClick={() => setSelectedProvider('dropbox')}
                    >
                      Browse Files
                    </Button>
                    <Button
                      variant="ghost"
                      onClick={() => handleDisconnect('dropbox')}
                    >
                      <X className="w-4 h-4" />
                    </Button>
                  </div>
                ) : DROPBOX_CONFIGURED ? (
                  <Button
                    variant="secondary"
                    className="w-full"
                    onClick={() => handleConnect('dropbox')}
                    disabled={isConnecting}
                  >
                    {isConnecting ? (
                      <>
                        <Loader2 className="w-4 h-4 animate-spin" />
                        Connecting...
                      </>
                    ) : (
                      <>
                        <Cloud className="w-4 h-4" />
                        Connect
                      </>
                    )}
                  </Button>
                ) : (
                  <p className="text-xs text-surface-400 text-center py-2">
                    Set VITE_DROPBOX_APP_KEY in .env to enable
                  </p>
                )}
              </div>
            </div>

            {error && (
              <p className="text-sm text-red-500 text-center">{error}</p>
            )}

            <div className="text-xs text-surface-400 text-center pt-4 border-t border-surface-200 dark:border-surface-700">
              <p>Only compatible files will be shown (audiobooks, ebooks, PDFs, documents)</p>
            </div>
          </div>
        )}

        {/* File Browser */}
        {selectedProvider && (
          <div className="space-y-4">
            {/* Back button and breadcrumbs */}
            <div className="flex items-center gap-2">
              <Button
                variant="ghost"
                onClick={() => {
                  setSelectedProvider(null);
                  setFiles([]);
                  setBreadcrumbs([{ id: '', name: 'Root' }]);
                  setSelectedFiles(new Set());
                }}
              >
                <ArrowLeft className="w-4 h-4" />
              </Button>

              <div className="flex items-center gap-1 text-sm overflow-x-auto">
                {breadcrumbs.map((crumb, index) => (
                  <div key={crumb.id} className="flex items-center">
                    {index > 0 && <ChevronRight className="w-4 h-4 text-surface-400 mx-1" />}
                    <button
                      onClick={() => handleBreadcrumbClick(index)}
                      className={`hover:text-primary-500 transition-colors whitespace-nowrap ${
                        index === breadcrumbs.length - 1
                          ? 'font-medium text-surface-900 dark:text-white'
                          : 'text-surface-500'
                      }`}
                    >
                      {crumb.name}
                    </button>
                  </div>
                ))}
              </div>
            </div>

            {/* Loading state */}
            {isLoading && (
              <div className="flex items-center justify-center py-12">
                <Loader2 className="w-8 h-8 text-primary-500 animate-spin" />
              </div>
            )}

            {/* Error state */}
            {error && !isLoading && (
              <div className="text-center py-12">
                <p className="text-red-500">{error}</p>
                <Button variant="secondary" className="mt-4" onClick={() => loadFiles()}>
                  Retry
                </Button>
              </div>
            )}

            {/* File list */}
            {!isLoading && !error && (
              <>
                {files.length === 0 ? (
                  <div className="text-center py-12">
                    <Folder className="w-12 h-12 mx-auto text-surface-400 mb-4" />
                    <p className="text-surface-500">No compatible files found</p>
                  </div>
                ) : (
                  <>
                    {/* Select all */}
                    {files.some(f => !f.isFolder) && (
                      <div className="flex items-center justify-between pb-2 border-b border-surface-200 dark:border-surface-700">
                        <button
                          onClick={handleSelectAll}
                          className="text-sm text-primary-500 hover:text-primary-600"
                        >
                          {selectedFiles.size === files.filter(f => !f.isFolder).length
                            ? 'Deselect all'
                            : 'Select all'}
                        </button>
                        <span className="text-sm text-surface-500">
                          {selectedFiles.size} selected
                        </span>
                      </div>
                    )}

                    {/* File grid */}
                    <div className="max-h-80 overflow-y-auto space-y-1">
                      <AnimatePresence>
                        {files.map((file) => {
                          const Icon = getFileIcon(file);
                          const isSelected = selectedFiles.has(file.id);

                          return (
                            <motion.div
                              key={file.id}
                              initial={{ opacity: 0, y: 10 }}
                              animate={{ opacity: 1, y: 0 }}
                              exit={{ opacity: 0, y: -10 }}
                              className={`flex items-center gap-3 p-3 rounded-xl cursor-pointer transition-colors ${
                                file.isFolder
                                  ? 'hover:bg-surface-100 dark:hover:bg-surface-800'
                                  : isSelected
                                  ? 'bg-primary-500/10 border border-primary-500/30'
                                  : 'hover:bg-surface-100 dark:hover:bg-surface-800'
                              }`}
                              onClick={() => file.isFolder ? handleFolderClick(file) : handleFileSelect(file)}
                            >
                              {!file.isFolder && (
                                <div
                                  className={`w-5 h-5 rounded-md border-2 flex items-center justify-center transition-colors ${
                                    isSelected
                                      ? 'bg-primary-500 border-primary-500'
                                      : 'border-surface-300 dark:border-surface-600'
                                  }`}
                                >
                                  {isSelected && <Check className="w-3.5 h-3.5 text-white" />}
                                </div>
                              )}

                              <Icon className={`w-5 h-5 ${
                                file.isFolder ? 'text-yellow-500' : 'text-surface-400'
                              }`} />

                              <div className="flex-1 min-w-0">
                                <p className="text-sm font-medium text-surface-900 dark:text-white truncate">
                                  {file.name}
                                </p>
                                {!file.isFolder && file.size && (
                                  <p className="text-xs text-surface-500">
                                    {formatFileSize(file.size)}
                                  </p>
                                )}
                              </div>

                              {file.isFolder && (
                                <ChevronRight className="w-4 h-4 text-surface-400" />
                              )}
                            </motion.div>
                          );
                        })}
                      </AnimatePresence>
                    </div>
                  </>
                )}
              </>
            )}

            {/* Download button */}
            {selectedFiles.size > 0 && (
              <div className="flex gap-3 pt-4 border-t border-surface-200 dark:border-surface-700">
                <Button
                  variant="secondary"
                  className="flex-1"
                  onClick={() => setSelectedFiles(new Set())}
                >
                  Cancel
                </Button>
                <Button
                  variant="primary"
                  className="flex-1"
                  onClick={handleDownloadSelected}
                  disabled={isDownloading}
                >
                  {isDownloading ? (
                    <>
                      <Loader2 className="w-4 h-4 animate-spin" />
                      Downloading {downloadProgress.current}/{downloadProgress.total}...
                    </>
                  ) : (
                    <>
                      <Download className="w-4 h-4" />
                      Import {selectedFiles.size} file{selectedFiles.size > 1 ? 's' : ''}
                    </>
                  )}
                </Button>
              </div>
            )}
          </div>
        )}
      </div>
    </Modal>
  );
}
