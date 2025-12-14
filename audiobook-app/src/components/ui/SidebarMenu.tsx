import { useState, useEffect } from 'react';
import { createPortal } from 'react-dom';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Menu,
  X,
  FolderOpen,
  Palette,
  Cloud,
  ChevronRight,
  Download,
  CheckCircle2,
  HardDrive,
  Trash2,
  RefreshCw,
  Magnet,
} from 'lucide-react';
import { Button, Modal } from './index';
import { useStore } from '../../store/useStore';
import { RezonLogo } from './RezonLogo';
import {
  connectGoogleDrive,
  isGoogleDriveConnected,
  disconnectGoogleDrive,
  isDropboxConnected,
  disconnectDropbox,
  getDropboxAuthUrl,
} from '../../utils/cloudStorage';

interface SidebarMenuProps {
  onOpenTorrent: () => void;
  onOpenSettings: () => void;
}

const SIDEBAR_WIDTH = 300;

export function SidebarMenu({ onOpenTorrent, onOpenSettings }: SidebarMenuProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [isCloudModalOpen, setIsCloudModalOpen] = useState(false);
  const [isStorageModalOpen, setIsStorageModalOpen] = useState(false);
  const [googleConnected, setGoogleConnected] = useState(false);
  const [dropboxConnected, setDropboxConnected] = useState(false);
  const [connectingGoogle, setConnectingGoogle] = useState(false);
  const [connectingDropbox, setConnectingDropbox] = useState(false);

  const {
    books,
    removeBook,
    logoVariant,
    isDarkMode,
    activeTorrents,
  } = useStore();

  // Check cloud connection status on mount
  useEffect(() => {
    setGoogleConnected(isGoogleDriveConnected());
    setDropboxConnected(isDropboxConnected());
  }, [isCloudModalOpen]);

  // Handle escape key to close menu
  useEffect(() => {
    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isOpen) {
        setIsOpen(false);
      }
    };
    window.addEventListener('keydown', handleEscape);
    return () => window.removeEventListener('keydown', handleEscape);
  }, [isOpen]);

  // Prevent body scroll when menu is open
  useEffect(() => {
    document.body.style.overflow = isOpen ? 'hidden' : '';
    return () => {
      document.body.style.overflow = '';
    };
  }, [isOpen]);

  const handleConnectGoogle = async () => {
    setConnectingGoogle(true);
    try {
      const success = await connectGoogleDrive();
      setGoogleConnected(success);
    } catch (error) {
      console.error('Google Drive connection error:', error);
    }
    setConnectingGoogle(false);
  };

  const handleConnectDropbox = async () => {
    setConnectingDropbox(true);
    try {
      const authUrl = await getDropboxAuthUrl();
      window.location.href = authUrl;
    } catch (error) {
      console.error('Dropbox connection error:', error);
      setConnectingDropbox(false);
    }
  };

  const handleDisconnectGoogle = () => {
    disconnectGoogleDrive();
    setGoogleConnected(false);
  };

  const handleDisconnectDropbox = () => {
    disconnectDropbox();
    setDropboxConnected(false);
  };

  // Menu items
  const menuItems = [
    {
      id: 'files',
      icon: FolderOpen,
      label: 'Add Files',
      description: 'Import audiobooks from device',
      onClick: () => {
        const input = document.createElement('input');
        input.type = 'file';
        input.multiple = true;
        input.accept = '.mp3,.m4b,.m4a,.epub,.pdf';
        input.onchange = (e) => {
          const files = (e.target as HTMLInputElement).files;
          if (files) {
            console.log('Selected', files.length, 'files');
            // File handling is done by the Library component
          }
        };
        input.click();
        setIsOpen(false);
      },
    },
    {
      id: 'torrent',
      icon: Magnet,
      label: 'Torrent Downloads',
      description: activeTorrents.length > 0 ? `${activeTorrents.length} active` : 'Download from torrents',
      badge: activeTorrents.length > 0,
      onClick: () => {
        setIsOpen(false);
        onOpenTorrent();
      },
    },
    {
      id: 'cloud',
      icon: Cloud,
      label: 'Cloud Storage',
      description: googleConnected || dropboxConnected ? 'Connected' : 'Google Drive & Dropbox',
      badge: googleConnected || dropboxConnected,
      onClick: () => setIsCloudModalOpen(true),
    },
    {
      id: 'appearance',
      icon: Palette,
      label: 'Appearance',
      description: 'Theme & colors',
      onClick: () => {
        setIsOpen(false);
        onOpenSettings();
      },
    },
    {
      id: 'storage',
      icon: HardDrive,
      label: 'Library',
      description: `${books.length} books`,
      onClick: () => setIsStorageModalOpen(true),
    },
  ];

  const toggleMenu = () => setIsOpen(!isOpen);

  // Calculate storage stats
  const audioBooks = books.filter(b => b.format === 'audio').length;
  const epubBooks = books.filter(b => b.format === 'epub').length;
  const pdfBooks = books.filter(b => b.format === 'pdf').length;

  // Sidebar content - rendered via portal
  const sidebarContent = (
    <AnimatePresence>
      {isOpen && (
        <>
          {/* Backdrop */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.2 }}
            onClick={() => setIsOpen(false)}
            className="fixed inset-0 bg-black/50 z-[9998]"
          />

          {/* Sidebar Panel */}
          <motion.aside
            initial={{ x: -SIDEBAR_WIDTH }}
            animate={{ x: 0 }}
            exit={{ x: -SIDEBAR_WIDTH }}
            transition={{ type: 'spring', damping: 30, stiffness: 300 }}
            className="fixed left-0 top-0 h-full z-[9999] flex flex-col shadow-2xl"
            style={{
              width: SIDEBAR_WIDTH,
              backgroundColor: isDarkMode ? '#0c0a09' : '#ffffff',
            }}
          >
            {/* Header */}
            <div className="p-5 border-b border-surface-800 safe-area-top">
              <div className="flex items-center justify-between">
                <RezonLogo size="md" variant={logoVariant} animated={false} />
                <button
                  onClick={() => setIsOpen(false)}
                  className="p-2 rounded-lg hover:bg-surface-800 transition-colors"
                >
                  <X className="w-5 h-5 text-surface-400" />
                </button>
              </div>
            </div>

            {/* Menu Items */}
            <nav className="flex-1 overflow-y-auto p-3">
              <div className="space-y-1">
                {menuItems.map((item, index) => (
                  <motion.button
                    key={item.id}
                    initial={{ opacity: 0, x: -20 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: index * 0.05 }}
                    onClick={item.onClick}
                    className="w-full flex items-center gap-3 p-3 rounded-xl hover:bg-surface-800 transition-colors group"
                  >
                    <div className="w-10 h-10 rounded-xl bg-primary-500/10 flex items-center justify-center group-hover:bg-primary-500/20 transition-colors">
                      <item.icon className="w-5 h-5 text-primary-500" />
                    </div>
                    <div className="flex-1 text-left">
                      <p className="font-medium text-white text-sm">{item.label}</p>
                      <p className="text-xs text-surface-500">{item.description}</p>
                    </div>
                    {item.badge && (
                      <span className="w-2 h-2 bg-green-500 rounded-full" />
                    )}
                    <ChevronRight className="w-4 h-4 text-surface-500 group-hover:text-primary-500 transition-colors" />
                  </motion.button>
                ))}
              </div>

              {/* Downloads Section */}
              {activeTorrents.length > 0 && (
                <motion.div
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  className="mt-4 pt-4 border-t border-surface-800"
                >
                  <h3 className="flex items-center gap-2 px-3 mb-2 text-xs font-semibold text-surface-500 uppercase">
                    <Download className="w-3 h-3" />
                    Downloads
                  </h3>
                  <div className="space-y-2">
                    {activeTorrents.map((torrent) => {
                      const isComplete = torrent.progress >= 100;
                      return (
                        <div
                          key={torrent.id}
                          className="flex items-center gap-3 p-3 bg-surface-800 rounded-xl"
                        >
                          <div className="w-8 h-8 rounded-full bg-surface-700 flex items-center justify-center">
                            {isComplete ? (
                              <CheckCircle2 className="w-4 h-4 text-green-500" />
                            ) : (
                              <span className="text-[10px] font-bold text-white">
                                {Math.round(torrent.progress)}%
                              </span>
                            )}
                          </div>
                          <div className="flex-1 min-w-0">
                            <p className="text-xs font-medium text-white truncate">{torrent.name}</p>
                            <p className={`text-[10px] ${isComplete ? 'text-green-500' : 'text-primary-500'}`}>
                              {isComplete ? 'Complete' : 'Downloading...'}
                            </p>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </motion.div>
              )}
            </nav>

            {/* Footer */}
            <div className="p-4 border-t border-surface-800 safe-area-bottom">
              <div className="text-center text-xs text-surface-500">
                <p className="font-semibold">Rezon v1.0.15</p>
              </div>
            </div>
          </motion.aside>
        </>
      )}
    </AnimatePresence>
  );

  return (
    <>
      {/* Menu Toggle Button */}
      <Button variant="icon" onClick={toggleMenu}>
        <motion.div animate={isOpen ? { rotate: 90 } : { rotate: 0 }} transition={{ duration: 0.2 }}>
          {isOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
        </motion.div>
      </Button>

      {/* Render sidebar via portal */}
      {typeof document !== 'undefined' && createPortal(sidebarContent, document.body)}

      {/* Cloud Storage Modal */}
      <Modal
        isOpen={isCloudModalOpen}
        onClose={() => setIsCloudModalOpen(false)}
        title="Cloud Storage"
        size="md"
      >
        <div className="space-y-4">
          <p className="text-sm text-surface-400">
            Connect your cloud storage to import audiobooks directly.
          </p>

          {/* Google Drive */}
          <div className="p-4 border border-surface-700 rounded-xl">
            <div className="flex items-center gap-4">
              <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-blue-500 via-green-500 to-yellow-500 flex items-center justify-center">
                <Cloud className="w-6 h-6 text-white" />
              </div>
              <div className="flex-1">
                <p className="font-medium text-white">Google Drive</p>
                <p className="text-sm text-surface-400">
                  {googleConnected ? 'Connected' : 'Not connected'}
                </p>
              </div>
              {googleConnected ? (
                <button
                  onClick={handleDisconnectGoogle}
                  className="px-4 py-2 text-sm bg-red-500/10 text-red-500 rounded-lg hover:bg-red-500/20 transition-colors"
                >
                  Disconnect
                </button>
              ) : (
                <button
                  onClick={handleConnectGoogle}
                  disabled={connectingGoogle}
                  className="px-4 py-2 text-sm bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors disabled:opacity-50 flex items-center gap-2"
                >
                  {connectingGoogle && <RefreshCw className="w-4 h-4 animate-spin" />}
                  Connect
                </button>
              )}
            </div>
          </div>

          {/* Dropbox */}
          <div className="p-4 border border-surface-700 rounded-xl">
            <div className="flex items-center gap-4">
              <div className="w-12 h-12 rounded-xl bg-blue-600 flex items-center justify-center">
                <Cloud className="w-6 h-6 text-white" />
              </div>
              <div className="flex-1">
                <p className="font-medium text-white">Dropbox</p>
                <p className="text-sm text-surface-400">
                  {dropboxConnected ? 'Connected' : 'Not connected'}
                </p>
              </div>
              {dropboxConnected ? (
                <button
                  onClick={handleDisconnectDropbox}
                  className="px-4 py-2 text-sm bg-red-500/10 text-red-500 rounded-lg hover:bg-red-500/20 transition-colors"
                >
                  Disconnect
                </button>
              ) : (
                <button
                  onClick={handleConnectDropbox}
                  disabled={connectingDropbox}
                  className="px-4 py-2 text-sm bg-primary-500 text-white rounded-lg hover:bg-primary-600 transition-colors disabled:opacity-50 flex items-center gap-2"
                >
                  {connectingDropbox && <RefreshCw className="w-4 h-4 animate-spin" />}
                  Connect
                </button>
              )}
            </div>
          </div>

          <p className="text-xs text-surface-500 text-center pt-2">
            Your files remain in the cloud. We only download what you select.
          </p>
        </div>
      </Modal>

      {/* Library Storage Modal */}
      <Modal
        isOpen={isStorageModalOpen}
        onClose={() => setIsStorageModalOpen(false)}
        title="Library"
        size="lg"
      >
        <div className="space-y-4">
          {/* Stats */}
          <div className="grid grid-cols-4 gap-2">
            {[
              { label: 'Total', value: books.length, color: 'bg-primary-500' },
              { label: 'Audio', value: audioBooks, color: 'bg-cyan-500' },
              { label: 'EPUB', value: epubBooks, color: 'bg-violet-500' },
              { label: 'PDF', value: pdfBooks, color: 'bg-orange-500' },
            ].map((stat) => (
              <div key={stat.label} className="text-center p-3 bg-surface-800 rounded-xl">
                <div className={`w-2 h-2 ${stat.color} rounded-full mx-auto mb-1`} />
                <p className="text-xl font-bold text-white">{stat.value}</p>
                <p className="text-[10px] text-surface-500">{stat.label}</p>
              </div>
            ))}
          </div>

          {/* File List */}
          <div className="max-h-64 overflow-y-auto space-y-1">
            {books.length === 0 ? (
              <p className="text-center text-surface-500 py-8 text-sm">No books in library</p>
            ) : (
              books.map((book) => (
                <div
                  key={book.id}
                  className="flex items-center justify-between p-3 bg-surface-800 rounded-xl"
                >
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-white truncate">{book.title}</p>
                    <p className="text-xs text-surface-500 uppercase">{book.format}</p>
                  </div>
                  <button
                    onClick={() => removeBook(book.id)}
                    className="p-2 text-red-500 hover:bg-red-500/10 rounded-lg transition-colors"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              ))
            )}
          </div>
        </div>
      </Modal>
    </>
  );
}
