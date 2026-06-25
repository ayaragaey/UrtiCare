import React from 'react';
import { SafeAreaView, ScrollView, View, Text, StyleSheet, StatusBar, Modal, TouchableOpacity, Platform } from 'react-native';
import { useTrackerStore } from './src/store/useTrackerStore';
import ActionPillsGroup from './src/components/ActionPillsGroup';
import LiveCounter from './src/components/LiveCounter';
import HistoryLogList from './src/components/HistoryLogList';
import MedicationSummaryDrawer from './src/components/MedicationSummaryDrawer';

export default function App() {
  const collisionWarning = useTrackerStore(state => state.collisionWarning);
  const setCollisionWarning = useTrackerStore(state => state.setCollisionWarning);
  const addEntry = useTrackerStore(state => state.addEntry);
  const updateEntry = useTrackerStore(state => state.updateEntry);

  const handleResolveCollision = (confirm: boolean) => {
    if (!collisionWarning) return;

    if (confirm) {
      // Force / Auto-merge
      if (collisionWarning.isEdit && collisionWarning.entryId) {
        updateEntry(collisionWarning.entryId, collisionWarning.timestamp, true);
      } else {
        addEntry(collisionWarning.type, collisionWarning.timestamp, true);
      }
    }
    
    // Clear warning
    setCollisionWarning(null);
  };

  const getFriendlyTypeName = (type?: string) => {
    if (!type) return '';
    if (type === 'FLARE_UP') return 'Symptom Flare-up';
    if (type === 'ANTIHISTAMINE') return 'Antihistamine Intake';
    if (type === 'CORTISONE') return 'Cortisone/Steroid Dose';
    return type;
  };

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="dark-content" backgroundColor="#FFFFFF" />
      
      {/* Scrollable Layout Container */}
      <ScrollView 
        contentContainerStyle={styles.scrollContent}
        showsVerticalScrollIndicator={false}
      >
        {/* Sleek App Branding Header */}
        <View style={styles.brandingHeader}>
          <Text style={styles.appTitle}>U R T I C A R E</Text>
          <Text style={styles.appSubtitle}>SYMPTOM & MEDICATION CHRONOLOGY</Text>
          <View style={styles.accentLine} />
        </View>

        {/* Top-aligned Action Pills */}
        <ActionPillsGroup />

        {/* Performance-isolated Live Elapsed Counter */}
        <LiveCounter />

        {/* Dynamic Log List */}
        <HistoryLogList />

        {/* Analytical Aggregation Bottom Accordion */}
        <MedicationSummaryDrawer />
      </ScrollView>

      {/* High-Fidelity Temporal Collision Warning Modal */}
      <Modal
        visible={collisionWarning !== null}
        transparent={true}
        animationType="fade"
      >
        <View style={styles.modalBackdrop}>
          <View style={styles.warningCard}>
            <View style={styles.warningIconHeader}>
              <Text style={styles.warningIcon}>⚠️</Text>
              <Text style={styles.warningTitle}>TEMPORAL COLLISION DETECTED</Text>
            </View>

            <Text style={styles.warningDescription}>
              You already logged an entry for{' '}
              <Text style={styles.highlightText}>
                {getFriendlyTypeName(collisionWarning?.type)}
              </Text>{' '}
              within this exact minute timeframe.
            </Text>

            <Text style={styles.warningQuestion}>
              Would you like to automatically merge this timestamp and overwrite the duplicate?
            </Text>

            <View style={styles.warningActionRow}>
              <TouchableOpacity
                onPress={() => handleResolveCollision(false)}
                style={[styles.warningBtn, styles.dismissBtn]}
              >
                <Text style={styles.dismissBtnText}>Cancel</Text>
              </TouchableOpacity>
              
              <TouchableOpacity
                onPress={() => handleResolveCollision(true)}
                style={[styles.warningBtn, styles.mergeBtn]}
              >
                <Text style={styles.mergeBtnText}>Merge Entries</Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#FFFFFF', // Pure White
  },
  scrollContent: {
    paddingBottom: 40,
  },
  brandingHeader: {
    alignItems: 'center',
    marginTop: Platform.OS === 'ios' ? 16 : 28,
    marginBottom: 8,
  },
  appTitle: {
    color: '#111111',
    fontSize: 22,
    fontWeight: '300',
    letterSpacing: 8,
  },
  appSubtitle: {
    color: '#666666',
    fontSize: 8,
    fontWeight: '700',
    letterSpacing: 2,
    marginTop: 6,
  },
  accentLine: {
    width: 40,
    height: 2,
    backgroundColor: '#E2E2E2',
    borderRadius: 1,
    marginTop: 12,
  },

  // Collision Warning modal
  modalBackdrop: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    justifyContent: 'center',
    alignItems: 'center',
    padding: 24,
  },
  warningCard: {
    width: '100%',
    maxWidth: 340,
    backgroundColor: '#FFFFFF',
    borderWidth: 1.5,
    borderColor: '#814b92', // Soft Purple border warning accent
    borderRadius: 16,
    padding: 24,
    alignItems: 'center',
    ...Platform.select({
      ios: {
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.15,
        shadowRadius: 10,
      },
      android: {
        elevation: 6,
      },
      web: {
        boxShadow: '0 4px 20px rgba(0,0,0,0.1)',
      }
    })
  },
  warningIconHeader: {
    alignItems: 'center',
    marginBottom: 16,
  },
  warningIcon: {
    fontSize: 32,
    marginBottom: 6,
  },
  warningTitle: {
    color: '#814b92', // Darker purple for readability on white background
    fontSize: 13,
    fontWeight: '800',
    letterSpacing: 1.5,
    textAlign: 'center',
  },
  warningDescription: {
    color: '#333333',
    fontSize: 13,
    fontWeight: '600',
    textAlign: 'center',
    lineHeight: 18,
    marginBottom: 12,
  },
  highlightText: {
    color: '#111111',
    fontWeight: '800',
  },
  warningQuestion: {
    color: '#666666',
    fontSize: 11,
    fontWeight: '600',
    textAlign: 'center',
    lineHeight: 15,
    marginBottom: 24,
  },
  warningActionRow: {
    flexDirection: 'row',
    width: '100%',
    justifyContent: 'space-between',
  },
  warningBtn: {
    flex: 1,
    paddingVertical: 12,
    borderRadius: 8,
    alignItems: 'center',
    justifyContent: 'center',
  },
  dismissBtn: {
    backgroundColor: '#EAEAEA',
    marginRight: 8,
  },
  mergeBtn: {
    backgroundColor: '#814b92',
    marginLeft: 8,
  },
  dismissBtnText: {
    color: '#555555',
    fontSize: 12,
    fontWeight: '700',
  },
  mergeBtnText: {
    color: '#FFFFFF',
    fontSize: 12,
    fontWeight: '800',
  }
});
