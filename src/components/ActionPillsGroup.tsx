import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet, Dimensions, Platform } from 'react-native';
import { useTrackerStore } from '../store/useTrackerStore';

export default function ActionPillsGroup() {
  const addEntry = useTrackerStore(state => state.addEntry);
  const setCollisionWarning = useTrackerStore(state => state.setCollisionWarning);

  const handleAction = (type: 'FLARE_UP' | 'ANTIHISTAMINE' | 'CORTISONE') => {
    const timestamp = new Date().toISOString();
    const result = addEntry(type, timestamp);
    
    if (result.collision) {
      // Trigger warning dialog in the store
      setCollisionWarning({
        type,
        timestamp,
        isEdit: false
      });
    }
  };

  return (
    <View style={styles.container}>
      {/* Flare Up Button */}
      <TouchableOpacity 
        style={[styles.pill, styles.flareUpPill]} 
        onPress={() => handleAction('FLARE_UP')}
        activeOpacity={0.8}
      >
        <Text style={styles.pillText}>Flare Up</Text>
      </TouchableOpacity>

      {/* Antihistamine Taken Button (Focal Element - scaled by exactly 15%) */}
      <TouchableOpacity 
        style={[styles.pill, styles.antihistaminePill, styles.focalPill]} 
        onPress={() => handleAction('ANTIHISTAMINE')}
        activeOpacity={0.8}
      >
        <Text style={[styles.pillText, styles.focalText]}>Antihistamine</Text>
        <Text style={styles.pillSubtext}>Taken</Text>
      </TouchableOpacity>

      {/* Cortisone/Steroid Button */}
      <TouchableOpacity 
        style={[styles.pill, styles.cortisonePill]} 
        onPress={() => handleAction('CORTISONE')}
        activeOpacity={0.8}
      >
        <Text style={[styles.pillText, { fontSize: 10, lineHeight: 12 }]}>Cortico{"\n"}steroids</Text>
      </TouchableOpacity>
    </View>
  );
}

const screenWidth = Dimensions.get('window').width;
const basePillSize = Math.min(100, screenWidth * 0.24);

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    marginVertical: 24,
    width: '100%',
  },
  pill: {
    width: basePillSize,
    height: basePillSize,
    borderRadius: basePillSize / 2,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 8,
    ...Platform.select({
      ios: {
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.3,
        shadowRadius: 5,
      },
      android: {
        elevation: 6,
      },
      web: {
        boxShadow: '0 4px 12px rgba(0, 0, 0, 0.4)',
      }
    }),
  },
  focalPill: {
    width: basePillSize * 1.15,
    height: basePillSize * 1.15,
    borderRadius: (basePillSize * 1.15) / 2,
    zIndex: 10,
    ...Platform.select({
      ios: {
        shadowScale: 1.1,
        shadowColor: '#1b8097',
        shadowOffset: { width: 0, height: 6 },
        shadowOpacity: 0.4,
        shadowRadius: 8,
      },
      android: {
        elevation: 8,
      },
      web: {
        boxShadow: '0 6px 20px rgba(27, 128, 151, 0.3)',
      }
    }),
  },
  flareUpPill: {
    backgroundColor: '#814b92',
  },
  antihistaminePill: {
    backgroundColor: '#509729',
  },
  cortisonePill: {
    backgroundColor: '#1b8097',
  },
  pillText: {
    color: '#FFFFFF',
    fontSize: 13,
    fontWeight: '700',
    textAlign: 'center',
    letterSpacing: -0.2,
  },
  focalText: {
    fontSize: 14,
  },
  pillSubtext: {
    color: '#FFFFFF',
    fontSize: 10,
    fontWeight: '500',
    textAlign: 'center',
    marginTop: 1,
  }
});
