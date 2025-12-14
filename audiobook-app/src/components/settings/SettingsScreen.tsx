import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  ArrowLeft,
  Wifi,
  FolderSync,
  Trash2,
  SkipBack,
  SkipForward,
  Pause,
  Bell,
  Battery,
  Play,
  Image,
  Moon,
  FileText,
  Upload,
  HelpCircle,
  Heart,
  Info,
  ChevronRight,
  X,
  Folder,
  Plus,
} from 'lucide-react';
import { Modal } from '../ui';
import { useStore, type SkipInterval, type NotificationAction } from '../../store/useStore';

interface SettingsScreenProps {
  isOpen: boolean;
  onClose: () => void;
}

export function SettingsScreen({ isOpen, onClose }: SettingsScreenProps) {
  const { appSettings, setAppSettings, addFolderToScan, removeFolderToScan } = useStore();
  const [showSkipBackward, setShowSkipBackward] = useState(false);
  const [showSkipForward, setShowSkipForward] = useState(false);
  const [showSkipAfterPause, setShowSkipAfterPause] = useState(false);
  const [showNotificationAction, setShowNotificationAction] = useState(false);
  const [showFoldersToScan, setShowFoldersToScan] = useState(false);
  const [showAbout, setShowAbout] = useState(false);

  const skipIntervalOptions: SkipInterval[] = [5, 10, 15, 30, 45, 60];
  const skipAfterPauseOptions = [0, 1, 2, 3, 5, 10];
  const notificationOptions: { value: NotificationAction; label: string }[] = [
    { value: 'pause', label: 'Pause' },
    { value: 'duck', label: 'Lower volume' },
    { value: 'nothing', label: 'Do nothing' },
  ];

  const ToggleSwitch = ({
    enabled,
    onChange,
  }: {
    enabled: boolean;
    onChange: (value: boolean) => void;
  }) => (
    <button
      onClick={() => onChange(!enabled)}
      className={`relative w-12 h-7 rounded-full transition-colors ${
        enabled ? 'bg-primary-500' : 'bg-surface-600'
      }`}
    >
      <motion.div
        className="absolute top-1 w-5 h-5 bg-white rounded-full shadow-md"
        animate={{ left: enabled ? '24px' : '4px' }}
        transition={{ type: 'spring', stiffness: 500, damping: 30 }}
      />
    </button>
  );

  const SettingRow = ({
    icon: Icon,
    label,
    description,
    value,
    onClick,
    toggle,
    toggleValue,
    onToggle,
  }: {
    icon?: React.ElementType;
    label: string;
    description?: string;
    value?: string;
    onClick?: () => void;
    toggle?: boolean;
    toggleValue?: boolean;
    onToggle?: (value: boolean) => void;
  }) => (
    <div
      className={`flex items-center justify-between py-4 ${onClick ? 'cursor-pointer active:bg-surface-800/50' : ''}`}
      onClick={onClick}
    >
      <div className="flex items-center gap-3 flex-1 min-w-0">
        {Icon && <Icon className="w-5 h-5 text-surface-400 flex-shrink-0" />}
        <div className="flex-1 min-w-0">
          <p className="font-semibold text-white">{label}</p>
          {description && <p className="text-sm text-surface-400">{description}</p>}
        </div>
      </div>
      {toggle && onToggle ? (
        <ToggleSwitch enabled={toggleValue || false} onChange={onToggle} />
      ) : value ? (
        <div className="flex items-center gap-2">
          <span className="text-sm text-surface-400">{value}</span>
          {onClick && <ChevronRight className="w-4 h-4 text-surface-500" />}
        </div>
      ) : onClick ? (
        <ChevronRight className="w-4 h-4 text-surface-500" />
      ) : null}
    </div>
  );

  const SectionHeader = ({ title }: { title: string }) => (
    <h3 className="text-sm font-bold text-primary-500 uppercase tracking-wider mb-2 mt-6 first:mt-0">
      {title}
    </h3>
  );

  return (
    <AnimatePresence>
      {isOpen && (
        <motion.div
          initial={{ opacity: 0, x: '100%' }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: '100%' }}
          transition={{ type: 'spring', damping: 25, stiffness: 200 }}
          className="fixed inset-0 z-50 bg-surface-950 flex flex-col"
        >
          {/* Header */}
          <div className="flex-shrink-0 flex items-center gap-4 px-4 py-4 safe-area-top border-b border-surface-800">
            <button
              onClick={onClose}
              className="p-2 -ml-2 text-surface-400 hover:text-white transition-colors"
            >
              <ArrowLeft className="w-6 h-6" />
            </button>
            <h1 className="text-xl font-bold text-white">Settings</h1>
          </div>

          {/* Content */}
          <div className="flex-1 overflow-auto px-4 safe-area-bottom">
            {/* DOWNLOADS */}
            <SectionHeader title="Downloads" />
            <SettingRow
              icon={Wifi}
              label="Download over Wi-Fi only"
              toggle
              toggleValue={appSettings.downloadOverWifiOnly}
              onToggle={(v) => setAppSettings({ downloadOverWifiOnly: v })}
            />

            {/* LIBRARY */}
            <SectionHeader title="Library" />
            <SettingRow
              icon={FolderSync}
              label="Scan folders on application startup"
              toggle
              toggleValue={appSettings.scanFoldersOnStartup}
              onToggle={(v) => setAppSettings({ scanFoldersOnStartup: v })}
            />
            <SettingRow
              icon={Trash2}
              label="Delete audiobooks if files are missing"
              toggle
              toggleValue={appSettings.deleteIfMissing}
              onToggle={(v) => setAppSettings({ deleteIfMissing: v })}
            />
            <SettingRow
              icon={Folder}
              label="Folders to scan"
              value={`${appSettings.foldersToScan.length} folders`}
              onClick={() => setShowFoldersToScan(true)}
            />

            {/* PLAYER */}
            <SectionHeader title="Player" />
            <SettingRow
              icon={SkipBack}
              label="Skip backward amount"
              value={`${appSettings.skipBackwardAmount} seconds`}
              onClick={() => setShowSkipBackward(true)}
            />
            <SettingRow
              icon={SkipForward}
              label="Skip forward amount"
              value={`${appSettings.skipForwardAmount} seconds`}
              onClick={() => setShowSkipForward(true)}
            />
            <SettingRow
              icon={Pause}
              label="Skip after pause amount"
              value={`${appSettings.skipAfterPauseAmount} second${appSettings.skipAfterPauseAmount !== 1 ? 's' : ''}`}
              onClick={() => setShowSkipAfterPause(true)}
            />
            <SettingRow
              icon={Bell}
              label="Action on notification sound"
              value={notificationOptions.find((o) => o.value === appSettings.actionOnNotification)?.label}
              onClick={() => setShowNotificationAction(true)}
            />
            <SettingRow
              icon={Play}
              label="Keep the playback service active on pause"
              toggle
              toggleValue={appSettings.keepPlaybackServiceActive}
              onToggle={(v) => setAppSettings({ keepPlaybackServiceActive: v })}
            />
            <SettingRow
              label="Stop playback on close"
              toggle
              toggleValue={appSettings.stopPlaybackOnClose}
              onToggle={(v) => setAppSettings({ stopPlaybackOnClose: v })}
            />
            <SettingRow
              icon={Image}
              label="Show cover on lock screen"
              toggle
              toggleValue={appSettings.showCoverOnLockScreen}
              onToggle={(v) => setAppSettings({ showCoverOnLockScreen: v })}
            />
            <SettingRow
              icon={Moon}
              label="Sleep timer"
              description="Set from player controls"
            />
            <SettingRow
              icon={Battery}
              label="Disable battery optimization"
              description="Enabled"
            />

            {/* DEBUGGING */}
            <SectionHeader title="Debugging" />
            <SettingRow
              icon={FileText}
              label="Enable file logging"
              toggle
              toggleValue={appSettings.fileLoggingEnabled}
              onToggle={(v) => setAppSettings({ fileLoggingEnabled: v })}
            />

            {/* BOTTOM SECTION */}
            <div className="border-t border-surface-800 mt-6 pt-4">
              <SettingRow
                icon={Upload}
                label="Backup"
                description="Export library data"
                onClick={() => {}}
              />
              <SettingRow
                icon={HelpCircle}
                label="Help"
                onClick={() => {}}
              />
              <SettingRow
                icon={Heart}
                label="Acknowledgements"
                onClick={() => {}}
              />
              <SettingRow
                icon={Info}
                label="About"
                onClick={() => setShowAbout(true)}
              />
            </div>

            {/* Spacer for safe area */}
            <div className="h-8" />
          </div>

          {/* Skip Backward Modal */}
          <Modal
            isOpen={showSkipBackward}
            onClose={() => setShowSkipBackward(false)}
            title="Skip Backward Amount"
            size="sm"
          >
            <div className="space-y-2">
              {skipIntervalOptions.map((option) => (
                <button
                  key={option}
                  onClick={() => {
                    setAppSettings({ skipBackwardAmount: option });
                    setShowSkipBackward(false);
                  }}
                  className={`w-full p-4 rounded-xl flex items-center justify-between transition-colors ${
                    appSettings.skipBackwardAmount === option
                      ? 'bg-primary-500 text-white'
                      : 'bg-surface-800 hover:bg-surface-700 text-surface-300'
                  }`}
                >
                  <span className="font-semibold">{option} seconds</span>
                </button>
              ))}
            </div>
          </Modal>

          {/* Skip Forward Modal */}
          <Modal
            isOpen={showSkipForward}
            onClose={() => setShowSkipForward(false)}
            title="Skip Forward Amount"
            size="sm"
          >
            <div className="space-y-2">
              {skipIntervalOptions.map((option) => (
                <button
                  key={option}
                  onClick={() => {
                    setAppSettings({ skipForwardAmount: option });
                    setShowSkipForward(false);
                  }}
                  className={`w-full p-4 rounded-xl flex items-center justify-between transition-colors ${
                    appSettings.skipForwardAmount === option
                      ? 'bg-primary-500 text-white'
                      : 'bg-surface-800 hover:bg-surface-700 text-surface-300'
                  }`}
                >
                  <span className="font-semibold">{option} seconds</span>
                </button>
              ))}
            </div>
          </Modal>

          {/* Skip After Pause Modal */}
          <Modal
            isOpen={showSkipAfterPause}
            onClose={() => setShowSkipAfterPause(false)}
            title="Skip After Pause Amount"
            size="sm"
          >
            <div className="space-y-2">
              {skipAfterPauseOptions.map((option) => (
                <button
                  key={option}
                  onClick={() => {
                    setAppSettings({ skipAfterPauseAmount: option });
                    setShowSkipAfterPause(false);
                  }}
                  className={`w-full p-4 rounded-xl flex items-center justify-between transition-colors ${
                    appSettings.skipAfterPauseAmount === option
                      ? 'bg-primary-500 text-white'
                      : 'bg-surface-800 hover:bg-surface-700 text-surface-300'
                  }`}
                >
                  <span className="font-semibold">{option} second{option !== 1 ? 's' : ''}</span>
                </button>
              ))}
            </div>
          </Modal>

          {/* Notification Action Modal */}
          <Modal
            isOpen={showNotificationAction}
            onClose={() => setShowNotificationAction(false)}
            title="Action on Notification Sound"
            size="sm"
          >
            <div className="space-y-2">
              {notificationOptions.map((option) => (
                <button
                  key={option.value}
                  onClick={() => {
                    setAppSettings({ actionOnNotification: option.value });
                    setShowNotificationAction(false);
                  }}
                  className={`w-full p-4 rounded-xl flex items-center justify-between transition-colors ${
                    appSettings.actionOnNotification === option.value
                      ? 'bg-primary-500 text-white'
                      : 'bg-surface-800 hover:bg-surface-700 text-surface-300'
                  }`}
                >
                  <span className="font-semibold">{option.label}</span>
                </button>
              ))}
            </div>
          </Modal>

          {/* Folders to Scan Modal */}
          <Modal
            isOpen={showFoldersToScan}
            onClose={() => setShowFoldersToScan(false)}
            title="Folders to Scan"
            size="md"
          >
            <div className="space-y-4">
              <p className="text-sm text-surface-400">
                These folders will be scanned for audiobooks when the app starts.
              </p>

              {appSettings.foldersToScan.length === 0 ? (
                <div className="text-center py-8 text-surface-500">
                  No folders configured
                </div>
              ) : (
                <div className="space-y-2">
                  {appSettings.foldersToScan.map((folder) => (
                    <div
                      key={folder}
                      className="flex items-center justify-between p-3 bg-surface-800 rounded-xl"
                    >
                      <div className="flex items-center gap-3 flex-1 min-w-0">
                        <Folder className="w-5 h-5 text-primary-500 flex-shrink-0" />
                        <span className="text-sm text-white truncate">{folder}</span>
                      </div>
                      <button
                        onClick={() => removeFolderToScan(folder)}
                        className="p-2 text-surface-400 hover:text-red-500"
                      >
                        <X className="w-4 h-4" />
                      </button>
                    </div>
                  ))}
                </div>
              )}

              <button
                onClick={() => {
                  // In a real app, this would open a folder picker
                  // For now, we'll just show a placeholder
                  const folder = prompt('Enter folder path:');
                  if (folder) {
                    addFolderToScan(folder);
                  }
                }}
                className="w-full flex items-center justify-center gap-2 p-4 rounded-xl border-2 border-dashed border-surface-700 text-surface-400 hover:border-primary-500 hover:text-primary-500 transition-colors"
              >
                <Plus className="w-5 h-5" />
                <span className="font-semibold">Add Folder</span>
              </button>
            </div>
          </Modal>

          {/* About Modal */}
          <Modal
            isOpen={showAbout}
            onClose={() => setShowAbout(false)}
            title="About Rezon"
            size="sm"
          >
            <div className="text-center space-y-4">
              <div className="w-20 h-20 mx-auto bg-gradient-to-br from-primary-500 to-primary-700 rounded-2xl flex items-center justify-center">
                <span className="text-4xl font-bold text-white">R</span>
              </div>
              <div>
                <h2 className="text-xl font-bold text-white">Rezon</h2>
                <p className="text-surface-400">Audiobooks Reimagined</p>
              </div>
              <div className="text-sm text-surface-500">
                <p>Version 1.0.16</p>
              </div>
              <p className="text-sm text-surface-400">
                Resonate With Every Word.
              </p>
            </div>
          </Modal>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
