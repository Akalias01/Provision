import { useState, useEffect } from 'react';
import { createPortal } from 'react-dom';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Menu,
  X,
  FolderOpen,
  FolderSearch,
  HardDrive,
  Palette,
  Languages,
  Cloud,
  Trash2,
  ChevronRight,
  Magnet,
} from 'lucide-react';
import { Button, Modal } from './index';
import { useStore } from '../../store/useStore';
import { useTranslation } from '../../i18n';
import { VocaLogo } from './VocaLogo';

interface SidebarMenuProps {
  onOpenTorrent: () => void;
  onOpenSettings: () => void;
}

const SIDEBAR_WIDTH = 320;

export function SidebarMenu({ onOpenTorrent, onOpenSettings }: SidebarMenuProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [isStorageModalOpen, setIsStorageModalOpen] = useState(false);
  const [isFolderModalOpen, setIsFolderModalOpen] = useState(false);
  const [isLanguageModalOpen, setIsLanguageModalOpen] = useState(false);
  const [isCloudModalOpen, setIsCloudModalOpen] = useState(false);

  const {
    books,
    removeBook,
    logoVariant,
    language,
    setLanguage,
    isDarkMode,
  } = useStore();

  const { t } = useTranslation();

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

  // Push main content when sidebar opens
  useEffect(() => {
    const mainContent = document.getElementById('main-app-content');
    if (mainContent) {
      mainContent.style.transition = 'margin-left 0.3s ease-out';
      mainContent.style.marginLeft = isOpen ? `${SIDEBAR_WIDTH}px` : '0';
    }
    // Prevent body scroll when menu is open
    document.body.style.overflow = isOpen ? 'hidden' : '';
    return () => {
      document.body.style.overflow = '';
    };
  }, [isOpen]);

  const languages = [
    { code: 'en', name: 'English', nativeName: 'English' },
    { code: 'es', name: 'Spanish', nativeName: 'Español' },
    { code: 'fr', name: 'French', nativeName: 'Français' },
    { code: 'de', name: 'German', nativeName: 'Deutsch' },
    { code: 'it', name: 'Italian', nativeName: 'Italiano' },
    { code: 'pt', name: 'Portuguese', nativeName: 'Português' },
    { code: 'zh', name: 'Chinese', nativeName: '中文' },
    { code: 'ja', name: 'Japanese', nativeName: '日本語' },
    { code: 'ko', name: 'Korean', nativeName: '한국어' },
    { code: 'ar', name: 'Arabic', nativeName: 'العربية' },
    { code: 'hi', name: 'Hindi', nativeName: 'हिन्दी' },
    { code: 'ru', name: 'Russian', nativeName: 'Русский' },
  ];

  const menuItems = [
    {
      id: 'folders',
      icon: FolderOpen,
      label: t('scanFolders'),
      description: t('addBooksFromFolders'),
      onClick: () => setIsFolderModalOpen(true),
    },
    {
      id: 'torrent',
      icon: Magnet,
      label: t('downloadTorrent'),
      description: t('downloadFromMagnet'),
      onClick: () => {
        setIsOpen(false);
        onOpenTorrent();
      },
    },
    {
      id: 'storage',
      icon: HardDrive,
      label: t('storage'),
      description: t('manageLibrary'),
      onClick: () => setIsStorageModalOpen(true),
    },
    {
      id: 'appearance',
      icon: Palette,
      label: t('appearance'),
      description: t('colorsAndThemes'),
      onClick: () => {
        setIsOpen(false);
        onOpenSettings();
      },
    },
    {
      id: 'language',
      icon: Languages,
      label: t('language'),
      description: `${t('currentLanguage')}: ${languages.find(l => l.code === language)?.nativeName || 'English'}`,
      onClick: () => setIsLanguageModalOpen(true),
    },
    {
      id: 'cloud',
      icon: Cloud,
      label: t('cloudSync'),
      description: t('connectGoogleDrive'),
      onClick: () => setIsCloudModalOpen(true),
    },
  ];

  // Calculate storage stats
  const totalBooks = books.length;
  const audioBooks = books.filter(b => b.format === 'audio').length;
  const epubBooks = books.filter(b => b.format === 'epub').length;
  const pdfBooks = books.filter(b => b.format === 'pdf').length;

  const toggleMenu = () => setIsOpen(!isOpen);

  // Sidebar content - rendered via portal to document.body
  const sidebarContent = (
    <AnimatePresence>
      {isOpen && (
        <>
          {/* Backdrop - clicking closes menu */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.2 }}
            onClick={() => setIsOpen(false)}
            style={{
              position: 'fixed',
              inset: 0,
              backgroundColor: 'rgba(0, 0, 0, 0.4)',
              zIndex: 9998,
            }}
          />

          {/* Sidebar Panel */}
          <motion.aside
            initial={{ x: -SIDEBAR_WIDTH }}
            animate={{ x: 0 }}
            exit={{ x: -SIDEBAR_WIDTH }}
            transition={{ type: 'spring', damping: 30, stiffness: 300 }}
            drag="x"
            dragConstraints={{ left: -SIDEBAR_WIDTH, right: 0 }}
            dragElastic={0.1}
            onDragEnd={(_, info) => {
              if (info.offset.x < -100 || info.velocity.x < -500) {
                setIsOpen(false);
              }
            }}
            style={{
              position: 'fixed',
              left: 0,
              top: 0,
              height: '100vh',
              width: SIDEBAR_WIDTH,
              backgroundColor: isDarkMode ? '#0c0a09' : '#ffffff',
              zIndex: 9999,
              display: 'flex',
              flexDirection: 'column',
              boxShadow: '4px 0 25px rgba(0, 0, 0, 0.3)',
            }}
          >
            {/* Header */}
            <div className="p-6 border-b border-surface-200 dark:border-surface-700">
              <div className="flex items-center justify-between">
                <VocaLogo size="md" variant={logoVariant} animated={false} />
                <button
                  onClick={() => setIsOpen(false)}
                  className="p-2 rounded-lg hover:bg-surface-100 dark:hover:bg-surface-800 transition-colors"
                >
                  <X className="w-5 h-5 text-surface-500" />
                </button>
              </div>
            </div>

            {/* Menu Items */}
            <nav className="flex-1 overflow-y-auto p-4">
              <div className="space-y-1">
                {menuItems.map((item, index) => (
                  <motion.button
                    key={item.id}
                    initial={{ opacity: 0, x: -20 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: index * 0.05 }}
                    onClick={item.onClick}
                    className="w-full flex items-center gap-4 p-4 rounded-xl hover:bg-surface-100 dark:hover:bg-surface-800 transition-colors group"
                  >
                    <div className="w-10 h-10 rounded-xl bg-primary-500/10 flex items-center justify-center group-hover:bg-primary-500/20 transition-colors">
                      <item.icon className="w-5 h-5 text-primary-500" />
                    </div>
                    <div className="flex-1 text-left">
                      <p className="font-medium text-surface-900 dark:text-white">
                        {item.label}
                      </p>
                      <p className="text-sm text-surface-500">{item.description}</p>
                    </div>
                    <ChevronRight className="w-5 h-5 text-surface-400 group-hover:text-primary-500 group-hover:translate-x-1 transition-all" />
                  </motion.button>
                ))}
              </div>
            </nav>

            {/* Footer */}
            <div className="p-4 border-t border-surface-200 dark:border-surface-700">
              <div className="text-center text-sm text-surface-500">
                <p className="font-medium">VOCA v1.0.0</p>
                <p className="text-xs mt-1">{totalBooks} {t('booksInLibrary')}</p>
              </div>
            </div>
          </motion.aside>
        </>
      )}
    </AnimatePresence>
  );

  return (
    <>
      {/* Menu Toggle Button - Hamburger */}
      <Button variant="icon" onClick={toggleMenu}>
        <motion.div
          animate={isOpen ? { rotate: 90 } : { rotate: 0 }}
          transition={{ duration: 0.2 }}
        >
          {isOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
        </motion.div>
      </Button>

      {/* Render sidebar via portal to document.body */}
      {typeof document !== 'undefined' && createPortal(sidebarContent, document.body)}

      {/* Folder Scan Modal */}
      <Modal
        isOpen={isFolderModalOpen}
        onClose={() => setIsFolderModalOpen(false)}
        title={t('scanFoldersForBooks')}
        size="md"
      >
        <div className="space-y-4">
          <p className="text-sm text-surface-500 dark:text-surface-400">
            {t('selectFolderToScan')}
          </p>

          <div className="space-y-3">
            <button
              onClick={() => {
                const input = document.createElement('input');
                input.type = 'file';
                input.setAttribute('webkitdirectory', '');
                input.setAttribute('directory', '');
                input.onchange = (e) => {
                  const files = (e.target as HTMLInputElement).files;
                  if (files) {
                    console.log('Selected folder with', files.length, 'files');
                  }
                };
                input.click();
              }}
              className="w-full p-4 border-2 border-dashed border-surface-300 dark:border-surface-600 rounded-xl hover:border-primary-500 hover:bg-primary-500/5 transition-colors"
            >
              <FolderSearch className="w-8 h-8 mx-auto text-surface-400 mb-2" />
              <p className="font-medium text-surface-900 dark:text-white">
                {t('browseForFolder')}
              </p>
              <p className="text-sm text-surface-500 mt-1">
                {t('selectFolder')}
              </p>
            </button>
          </div>

          <div className="text-xs text-surface-400 mt-4">
            {t('supportedFormats')}
          </div>
        </div>
      </Modal>

      {/* Storage Modal */}
      <Modal
        isOpen={isStorageModalOpen}
        onClose={() => setIsStorageModalOpen(false)}
        title={t('storageManagement')}
        size="lg"
      >
        <div className="space-y-6">
          {/* Stats */}
          <div className="grid grid-cols-4 gap-4">
            {[
              { label: t('total'), value: totalBooks, color: 'bg-primary-500' },
              { label: t('audio'), value: audioBooks, color: 'bg-cyan-500' },
              { label: t('epub'), value: epubBooks, color: 'bg-violet-500' },
              { label: t('pdf'), value: pdfBooks, color: 'bg-orange-500' },
            ].map((stat) => (
              <div key={stat.label} className="text-center p-4 bg-surface-100 dark:bg-surface-800 rounded-xl">
                <div className={`w-3 h-3 ${stat.color} rounded-full mx-auto mb-2`} />
                <p className="text-2xl font-bold text-surface-900 dark:text-white">{stat.value}</p>
                <p className="text-xs text-surface-500">{stat.label}</p>
              </div>
            ))}
          </div>

          {/* File List */}
          <div>
            <h4 className="font-medium text-surface-900 dark:text-white mb-3">{t('libraryFiles')}</h4>
            <div className="max-h-64 overflow-y-auto space-y-2">
              {books.length === 0 ? (
                <p className="text-center text-surface-500 py-8">{t('noFilesInLibrary')}</p>
              ) : (
                books.map((book) => (
                  <div
                    key={book.id}
                    className="flex items-center justify-between p-3 bg-surface-100 dark:bg-surface-800 rounded-xl"
                  >
                    <div className="flex-1 min-w-0">
                      <p className="font-medium text-surface-900 dark:text-white truncate">
                        {book.title}
                      </p>
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
        </div>
      </Modal>

      {/* Language Modal */}
      <Modal
        isOpen={isLanguageModalOpen}
        onClose={() => setIsLanguageModalOpen(false)}
        title={t('selectLanguage')}
        size="md"
      >
        <div className="grid grid-cols-2 gap-2 max-h-96 overflow-y-auto">
          {languages.map((lang) => (
            <button
              key={lang.code}
              onClick={() => {
                setLanguage(lang.code);
                setIsLanguageModalOpen(false);
              }}
              className={`p-4 rounded-xl border-2 transition-all text-left ${
                language === lang.code
                  ? 'border-primary-500 bg-primary-500/10'
                  : 'border-surface-200 dark:border-surface-700 hover:border-surface-300'
              }`}
            >
              <p className="font-medium text-surface-900 dark:text-white">
                {lang.nativeName}
              </p>
              <p className="text-sm text-surface-500">{lang.name}</p>
            </button>
          ))}
        </div>
        <div className="text-xs text-surface-400 text-center pt-4 border-t border-surface-200 dark:border-surface-700 mt-4">
          {t('translationsComingSoon')}
        </div>
      </Modal>

      {/* Cloud Sync Modal */}
      <Modal
        isOpen={isCloudModalOpen}
        onClose={() => setIsCloudModalOpen(false)}
        title={t('cloudSync')}
        size="md"
      >
        <div className="space-y-4">
          <p className="text-sm text-surface-500 dark:text-surface-400">
            {t('connectCloudStorage')}
          </p>

          <button
            onClick={() => {
              alert(t('googleDriveIntegration'));
            }}
            className="w-full flex items-center gap-4 p-4 border-2 border-surface-200 dark:border-surface-700 rounded-xl hover:border-primary-500 transition-colors"
          >
            <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-blue-500 via-green-500 to-yellow-500 flex items-center justify-center">
              <Cloud className="w-6 h-6 text-white" />
            </div>
            <div className="flex-1 text-left">
              <p className="font-medium text-surface-900 dark:text-white">
                Google Drive
              </p>
              <p className="text-sm text-surface-500">
                {t('connectGoogleDrive')}
              </p>
            </div>
            <ChevronRight className="w-5 h-5 text-surface-400" />
          </button>

          <div className="text-xs text-surface-400 text-center pt-4">
            {t('moreCloudProviders')}
          </div>
        </div>
      </Modal>
    </>
  );
}
