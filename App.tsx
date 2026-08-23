import React, { useState } from 'react';
import {
  SafeAreaView,
  ScrollView,
  View,
  Text,
  StyleSheet,
  StatusBar,
  Platform,
  TextInput,
  Alert,
} from 'react-native';
import { useTrackerStore, isProfileComplete } from './src/store/useTrackerStore';
import { theme } from './src/styles/theme';
import Button from './src/components/common/Button';
import Modal from './src/components/common/Modal';

// New Home Screen & Bottom Navigation Components
import CircularStatusTimer from './src/components/CircularStatusTimer';
import QuickLogBar from './src/components/QuickLogBar';
import RecentMedicationLog from './src/components/RecentMedicationLog';
import BottomNavBar, { MainTabType } from './src/components/BottomNavBar';
import HistoryLogList from './src/components/HistoryLogList';
import AddConsumptionScreen from './src/components/views/AddConsumptionScreen';
import QuickLogConsumptionBottomSheet from './src/components/QuickLogConsumptionBottomSheet';
import { TouchableOpacity } from 'react-native';

// Views for other Bottom Nav tabs
import InsightsScreen from './src/components/views/InsightsScreen';
import TalkToUrtiScreen from './src/components/views/TalkToUrtiScreen';
import LibraryScreen from './src/components/views/LibraryScreen';
import ProfileScreen from './src/components/views/ProfileScreen';
import OnboardingModal from './src/components/OnboardingModal';

export default function App() {
  const [activeTab, setActiveTab] = useState<MainTabType>('home');
  const [subScreen, setSubScreen] = useState<'main' | 'logs' | 'add_consumption'>('main');

  const profile = useTrackerStore(state => state.profile);
  const [dismissedThisSession, setDismissedThisSession] = useState(false);

  // Directly determine popup visibility: shown whenever app is open and profile is incomplete
  const showProfileReminder = !dismissedThisSession && !isProfileComplete(profile);



  const [showQuickLog, setShowQuickLog] = useState(false);
  const [showConsumptionModal, setShowConsumptionModal] = useState(false);
  const [consumptionText, setConsumptionText] = useState('');
  const [prefilledItem, setPrefilledItem] = useState<string | undefined>(undefined);
  const [prefilledCat, setPrefilledCat] = useState<string | undefined>(undefined);
  const [prefilledTime, setPrefilledTime] = useState<string | undefined>(undefined);
  const [snackbar, setSnackbar] = useState<{ visible: boolean; message: string; undoAction?: () => void } | null>(null);
  const addConsumptionEntry = useTrackerStore(state => state.addConsumptionEntry);

  React.useEffect(() => {
    if (snackbar?.visible) {
      const timer = setTimeout(() => {
        setSnackbar(prev => prev ? { ...prev, visible: false } : null);
      }, 4000);
      return () => clearTimeout(timer);
    }
  }, [snackbar]);

  const collisionWarning = useTrackerStore(state => state.collisionWarning);
  const setCollisionWarning = useTrackerStore(state => state.setCollisionWarning);
  const addEntry = useTrackerStore(state => state.addEntry);
  const updateEntry = useTrackerStore(state => state.updateEntry);
  const undoConsumptionLogs = useTrackerStore(state => state.undoConsumptionLogs);

  const handleQuickLogLogged = (ids: string[], previousRecentlyUsed: string[], message: string) => {
    setSnackbar({
      visible: true,
      message,
      undoAction: () => {
        undoConsumptionLogs(ids, previousRecentlyUsed);
        setSnackbar({
          visible: true,
          message: 'Logging undone successfully',
        });
      }
    });
  };

  const handleResolveCollision = (confirm: boolean) => {
    if (!collisionWarning) return;

    if (confirm) {
      if (collisionWarning.isEdit && collisionWarning.entryId) {
        updateEntry(collisionWarning.entryId, collisionWarning.timestamp, true);
      } else {
        addEntry(collisionWarning.type, collisionWarning.timestamp, true);
      }
    }
    setCollisionWarning(null);
  };

  const getFriendlyTypeName = (type?: string) => {
    if (!type) return '';
    if (type === 'FLARE_UP') return 'Symptom Flare-up';
    if (type === 'ANTIHISTAMINE') return 'Antihistamine Intake';
    if (type === 'CORTISONE') return 'Cortisone/Steroid Dose';
    return type;
  };

  // Render main content based on active tab
  const renderTabContent = () => {
    if (activeTab === 'home') {
      if (subScreen === 'logs') {
        return (
          <HistoryLogList
            onBack={() => setSubScreen('main')}
            onOpenAddConsumption={(time) => {
              setPrefilledTime(time);
              setSubScreen('add_consumption');
            }}
          />
        );
      }
      if (subScreen === 'add_consumption') {
        return (
          <AddConsumptionScreen
            onBack={() => {
              setSubScreen('main');
              setPrefilledItem(undefined);
              setPrefilledCat(undefined);
              setPrefilledTime(undefined);
            }}
            prefilledItemName={prefilledItem}
            prefilledCategory={prefilledCat}
            prefilledTimestamp={prefilledTime}
          />
        );
      }
      return (
        <View style={styles.homeContentWrapper}>
          {/* Top Logo Header with Tricolor Brand Accent */}
          <View style={styles.logoHeaderContainer}>
            <View style={styles.logoMarkContainer as any}>
              <View style={styles.logoPulseCircle} />
              <Text style={styles.logoMarkText}>U</Text>
            </View>
            <Text style={styles.logoTitle}>UrtiCare</Text>
          </View>

          {/* Circular Progress/Status Timer Component with 3-Color Gradient Frame */}
          <CircularStatusTimer />

          {/* Combined Quick Log Dock / Bar */}
          <QuickLogBar onOpenConsumption={() => setShowConsumptionModal(true)} />

          {/* Nav Card to Open Timeline Logs */}
          <View style={{ paddingHorizontal: 16, marginTop: 4, marginBottom: 8 }}>
            <TouchableOpacity
              style={styles.viewLogsBtn}
              onPress={() => setSubScreen('logs')}
              activeOpacity={0.8}
            >
              <Text style={styles.viewLogsBtnText}>View Event Log History</Text>
            </TouchableOpacity>
          </View>

          {/* Recent Medication Intake Log Container & Floating Gradient Add Button */}
          <RecentMedicationLog onViewHistory={() => setSubScreen('logs')} />
        </View>
      );
    }

    switch (activeTab) {
      case 'insights':
        return <InsightsScreen />;

      case 'chat':
        return <TalkToUrtiScreen />;

      case 'library':
        return <LibraryScreen />;

      case 'profile':
        return <ProfileScreen />;

      default:
        return null;
    }
  };

  const isLogsScreen = subScreen === 'logs';

  return (
    <SafeAreaView style={[styles.container, isLogsScreen && { backgroundColor: '#000000' }]}>
      <StatusBar
        barStyle={isLogsScreen ? 'light-content' : 'dark-content'}
        backgroundColor={isLogsScreen ? '#000000' : '#F8FAFC'}
      />

      {/* Main Scrollable View */}
      <View style={[styles.mainLayout, isLogsScreen && { backgroundColor: '#000000' }]}>
        <ScrollView
          style={[styles.scrollArea, isLogsScreen && { backgroundColor: '#000000' }]}
          contentContainerStyle={[styles.scrollContent, isLogsScreen && { backgroundColor: '#000000' }]}
          showsVerticalScrollIndicator={false}
        >
          {renderTabContent()}
        </ScrollView>

        {/* Fixed 5-Tab Bottom Navigation Bar with Gradient Border Line */}
        <BottomNavBar activeTab={activeTab} onSelectTab={setActiveTab} />
      </View>

      {/* Quick Log Consumption Modal */}
      <Modal
        visible={showConsumptionModal}
        onClose={() => setShowConsumptionModal(false)}
        title="Log Consumption"
        footerActions={
          <>
            <Button
              title="Cancel"
              onPress={() => setShowConsumptionModal(false)}
              variant="secondary"
              style={styles.modalBtn}
            />
            <Button
              title="Log"
              onPress={() => {
                if (!consumptionText.trim()) {
                  Alert.alert('Error', 'Please enter what you consumed.');
                  return;
                }
                const timestamp = new Date().toISOString();
                addConsumptionEntry(consumptionText.trim(), 'Other', '', '', timestamp);
                setSnackbar({
                  visible: true,
                  message: `Successfully logged: "${consumptionText.trim()}"`,
                });
                setConsumptionText('');
                setShowConsumptionModal(false);
              }}
              variant="primary"
              style={styles.modalBtn}
            />
          </>
        }
      >
        <Text style={{ fontSize: 13, color: '#64748B', marginBottom: 12 }}>
          What did you consume?
        </Text>
        <TextInput
          style={{
            backgroundColor: '#F8FAFC',
            borderWidth: 1.5,
            borderColor: '#E2E8F0',
            borderRadius: theme.borderRadius.medium,
            paddingHorizontal: 14,
            paddingVertical: 12,
            fontSize: 14,
            color: theme.colors.textDark,
            width: '100%',
          }}
          value={consumptionText}
          onChangeText={setConsumptionText}
          placeholder="e.g. Morning Coffee, Banana"
          placeholderTextColor="#94A3B8"
          autoFocus
        />
      </Modal>

      {/* Success Snackbar with Undo action */}
      {snackbar?.visible && (
        <View style={styles.snackbarContainer}>
          <Text style={styles.snackbarText}>{snackbar.message}</Text>
          {snackbar.undoAction && (
            <TouchableOpacity style={styles.snackbarUndoBtn} onPress={snackbar.undoAction}>
              <Text style={styles.snackbarUndoText}>Undo</Text>
            </TouchableOpacity>
          )}
        </View>
      )}

      {/* Temporal Collision Warning Modal */}
      <Modal
        visible={collisionWarning !== null}
        onClose={() => handleResolveCollision(false)}
        title="TEMPORAL COLLISION DETECTED"
        footerActions={
          <>
            <Button
              title="Cancel"
              onPress={() => handleResolveCollision(false)}
              variant="secondary"
              style={styles.modalBtn}
            />
            <Button
              title="Merge Entries"
              onPress={() => handleResolveCollision(true)}
              variant="primary"
              style={styles.modalBtn}
            />
          </>
        }
      >
        <Text style={styles.modalWarningDesc}>
          You already logged an entry for{' '}
          <Text style={styles.modalHighlightText}>
            {getFriendlyTypeName(collisionWarning?.type)}
          </Text>{' '}
          within this exact minute timeframe.
        </Text>

        <Text style={styles.modalWarningQuestion}>
          Would you like to automatically merge this timestamp and overwrite the duplicate?
        </Text>
      </Modal>

      {/* Profile Completion Reminder */}
      <OnboardingModal
        visible={showProfileReminder}
        onClose={() => {
          // Dismiss for current session only (does NOT permanently dismiss)
          setDismissedThisSession(true);
        }}
        onComplete={() => {
          // Navigate directly to Profile page
          setDismissedThisSession(true);
          setActiveTab('profile');
        }}
      />

    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F8FAFC',
  },
  mainLayout: {
    flex: 1,
    position: 'relative',
  },
  scrollArea: {
    flex: 1,
  },
  scrollContent: {
    paddingBottom: 110,
    maxWidth: 600,
    alignSelf: 'center',
    width: '100%',
  },
  homeContentWrapper: {
    width: '100%',
  },

  /* Logo Header Styling */
  logoHeaderContainer: {
    alignItems: 'center',
    justifyContent: 'center',
    paddingTop: Platform.OS === 'ios' ? 24 : 36,
    paddingBottom: 16,
    flexDirection: 'row',
    gap: 12,
  },
  logoMarkContainer: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: '#FFFFFF',
    justifyContent: 'center',
    alignItems: 'center',
    position: 'relative',
    ...Platform.select({
      web: {
        background: 'linear-gradient(135deg, #F5F3FF, #ECFDF5, #F0FDFA)',
        border: '2px solid transparent',
        borderImage: 'linear-gradient(135deg, #814B92, #509729, #1A7E97) 1',
        borderRadius: '22px',
        boxShadow: '0 4px 12px rgba(129, 75, 146, 0.15)',
      } as any,
      default: {
        borderWidth: 2,
        borderColor: '#814B92',
      }
    })
  },
  logoPulseCircle: {
    position: 'absolute',
    width: 10,
    height: 10,
    borderRadius: 5,
    backgroundColor: '#509729',
    top: 2,
    right: 2,
  },
  logoMarkText: {
    fontSize: 22,
    fontWeight: theme.typography.weight.heavy,
    color: '#814B92',
  },
  logoTitle: {
    fontSize: 28,
    fontWeight: theme.typography.weight.heavy,
    color: theme.colors.textDark,
    letterSpacing: 0.5,
  },

  /* Modal Buttons */
  modalBtn: {
    flex: 1,
  },
  modalWarningDesc: {
    fontSize: 14,
    color: theme.colors.textDark,
    lineHeight: 20,
    marginBottom: 8,
  },
  modalHighlightText: {
    fontWeight: '700',
    color: '#814B92',
  },
  modalWarningQuestion: {
    fontSize: 13,
    color: theme.colors.textMuted,
  },
  viewLogsBtn: {
    backgroundColor: '#FFFFFF',
    borderWidth: 1.5,
    borderColor: '#E2E8F0',
    borderRadius: theme.borderRadius.medium,
    paddingVertical: 12,
    alignItems: 'center',
    justifyContent: 'center',
    ...theme.shadows.subtle,
  },
  viewLogsBtnText: {
    fontSize: 13,
    fontWeight: theme.typography.weight.bold,
    color: '#814B92',
  },
  snackbarContainer: {
    position: 'absolute',
    bottom: 90, // Above bottom navigation bar
    left: 20,
    right: 20,
    backgroundColor: '#FFFFFF',
    borderWidth: 1.5,
    borderColor: '#E2E8F0',
    borderRadius: 16,
    paddingVertical: 14,
    paddingHorizontal: 16,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    shadowColor: '#0F172A',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.1,
    shadowRadius: 8,
    elevation: 5,
    zIndex: 999,
  },
  snackbarText: {
    fontSize: 13.5,
    fontWeight: '600',
    color: '#0F172A',
    flex: 1,
  },
  snackbarUndoBtn: {
    marginLeft: 12,
    paddingVertical: 4,
    paddingHorizontal: 8,
  },
  snackbarUndoText: {
    fontSize: 13.5,
    fontWeight: '700',
    color: '#814B92',
  },
});
