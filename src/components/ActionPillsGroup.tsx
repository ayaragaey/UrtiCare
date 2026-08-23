import React from 'react';
import { View, Text, StyleSheet, Platform, TouchableOpacity } from 'react-native';
import { useTrackerStore } from '../store/useTrackerStore';

export default function ActionPillsGroup() {
  const addEntry = useTrackerStore(state => state.addEntry);
  const setCollisionWarning = useTrackerStore(state => state.setCollisionWarning);

  const handleAction = (type: 'FLARE_UP' | 'ANTIHISTAMINE' | 'CORTISONE') => {
    const timestamp = new Date().toISOString();
    const result = addEntry(type, timestamp);
    
    if (result.collision) {
      setCollisionWarning({
        type,
        timestamp,
        isEdit: false
      });
    }
  };

  return (
    <View style={styles.container}>
      {/* 1. Flare Up Button - #FCB9A4 */}
      <TouchableOpacity 
        style={[styles.octagonOuter, styles.flareUpOuter, { width: 86, height: 86 }]} 
        onPress={() => handleAction('FLARE_UP')}
        activeOpacity={0.8}
      >
        <View style={[styles.octagonInner, styles.flareUpInner]}>
          <View style={styles.iconContainer}>
            <Text style={styles.flareIcon}>🔥</Text>
          </View>
          <Text style={[styles.btnLabel, { color: '#5A1212' }]}>Flare Up</Text>
        </View>
      </TouchableOpacity>

      {/* 2. Antihistamine Button - #509729 */}
      <TouchableOpacity 
        style={[styles.octagonOuter, styles.antihistamineOuter, { width: 90, height: 90 }]} 
        onPress={() => handleAction('ANTIHISTAMINE')}
        activeOpacity={0.8}
      >
        <View style={[styles.octagonInner, styles.antihistamineInner]}>
          <View style={styles.iconContainer}>
            <View style={[styles.capsuleIcon, { borderColor: '#509729' }]}>
              <View style={[styles.capsuleLine, { backgroundColor: '#509729' }]} />
            </View>
          </View>
          <Text style={[styles.btnLabel, { color: '#0F3E14' }]}>Antihestamine</Text>
        </View>
      </TouchableOpacity>

      {/* 3. Corticosteroids Button - #1a7e97 */}
      <TouchableOpacity 
        style={[styles.octagonOuter, styles.corticosteroidsOuter, { width: 86, height: 86 }]} 
        onPress={() => handleAction('CORTISONE')}
        activeOpacity={0.8}
      >
        <View style={[styles.octagonInner, styles.corticosteroidsInner]}>
          <View style={styles.iconContainer}>
            <View style={[styles.tabletIcon, { borderColor: '#1a7e97' }]}>
              <View style={[styles.tabletLine, { backgroundColor: '#1a7e97' }]} />
            </View>
          </View>
          <Text style={[styles.btnLabel, { color: '#0D3B66' }]}>Corticosteroids</Text>
        </View>
      </TouchableOpacity>
    </View>
  );
}

const octClip = 'polygon(28% 0%, 72% 0%, 100% 28%, 100% 72%, 72% 100%, 28% 100%, 0% 72%, 0% 28%)';

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-evenly',
    paddingHorizontal: 16,
    marginVertical: 18,
    width: '100%',
  },
  octagonOuter: {
    padding: 3.5,
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 2,
    borderRadius: 20,
    ...Platform.select({
      web: {
        clipPath: octClip,
        WebkitClipPath: octClip,
      } as any
    })
  },
  octagonInner: {
    width: '100%',
    height: '100%',
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 1,
    borderRadius: 16,
    padding: 3,
    ...Platform.select({
      web: {
        clipPath: octClip,
        WebkitClipPath: octClip,
      } as any
    })
  },
  flareUpOuter: {
    borderColor: '#FC9AA3',
    backgroundColor: 'rgba(252, 154, 163, 0.15)',
  },
  flareUpInner: {
    borderColor: 'rgba(252, 154, 163, 0.45)',
    backgroundColor: 'rgba(255, 255, 255, 0.35)',
  },
  antihistamineOuter: {
    borderColor: '#509729',
    backgroundColor: 'rgba(80, 151, 41, 0.15)',
  },
  antihistamineInner: {
    borderColor: 'rgba(80, 151, 41, 0.45)',
    backgroundColor: 'rgba(255, 255, 255, 0.35)',
  },
  corticosteroidsOuter: {
    borderColor: '#1a7e97',
    backgroundColor: 'rgba(26, 126, 151, 0.15)',
  },
  corticosteroidsInner: {
    borderColor: 'rgba(26, 126, 151, 0.45)',
    backgroundColor: 'rgba(255, 255, 255, 0.35)',
  },
  iconContainer: {
    height: 22,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 2,
  },
  flareIcon: {
    fontSize: 16,
  },
  capsuleIcon: {
    width: 12,
    height: 18,
    borderRadius: 6,
    borderWidth: 1.6,
    justifyContent: 'center',
    alignItems: 'center',
    transform: [{ rotate: '-45deg' }],
  },
  capsuleLine: {
    width: '100%',
    height: 1.6,
  },
  tabletIcon: {
    width: 18,
    height: 12,
    borderRadius: 6,
    borderWidth: 1.6,
    justifyContent: 'center',
    alignItems: 'center',
  },
  tabletLine: {
    width: '100%',
    height: 1.6,
  },
  btnLabel: {
    fontSize: 8.5,
    fontWeight: '700',
    textAlign: 'center',
    lineHeight: 10,
  },
});
