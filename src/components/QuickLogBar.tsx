import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Platform } from 'react-native';
import { useTrackerStore } from '../store/useTrackerStore';
import { theme } from '../styles/theme';
import { IconTookAPill, IconFlareUpWidget, IconConsumptionWidget } from './common/CustomIcons';

interface Props {
  onOpenConsumption: () => void;
}

export default function QuickLogBar({ onOpenConsumption }: Props) {
  const addEntry = useTrackerStore(state => state.addEntry);
  const setCollisionWarning = useTrackerStore(state => state.setCollisionWarning);
  const favoriteAntihistamine = useTrackerStore(state => state.favoriteAntihistamine);
  const defaultFlareUpSymptom = useTrackerStore(state => state.defaultFlareUpSymptom);
  const defaultFlareUpSeverity = useTrackerStore(state => state.defaultFlareUpSeverity);

  const handleTookAPill = () => {
    const timestamp = new Date().toISOString();
    const result = addEntry('ANTIHISTAMINE', timestamp, false, favoriteAntihistamine, 'Favorite');
    
    if (result.collision) {
      setCollisionWarning({
        type: 'ANTIHISTAMINE',
        timestamp,
        isEdit: false
      });
    }
  };

  const handleFlareUp = () => {
    const timestamp = new Date().toISOString();
    const result = addEntry('FLARE_UP', timestamp, false, defaultFlareUpSymptom, defaultFlareUpSeverity);
    
    if (result.collision) {
      setCollisionWarning({
        type: 'FLARE_UP',
        timestamp,
        isEdit: false
      });
    }
  };

  return (
    <View style={styles.container}>
      {/* 1. Took A Pill! Action */}
      <TouchableOpacity
        style={styles.button}
        onPress={handleTookAPill}
        activeOpacity={0.7}
      >
        <IconTookAPill size={18} />
        <Text style={styles.label}>Took A Pill!</Text>
      </TouchableOpacity>

      {/* Vertical Divider */}
      <View style={styles.divider} />

      {/* 2. Flare Up! Action */}
      <TouchableOpacity
        style={styles.button}
        onPress={handleFlareUp}
        activeOpacity={0.7}
      >
        <IconFlareUpWidget size={18} />
        <Text style={styles.label}>Flare Up!</Text>
      </TouchableOpacity>

      {/* Vertical Divider */}
      <View style={styles.divider} />

      {/* 3. Consumption! Action */}
      <TouchableOpacity
        style={styles.button}
        onPress={onOpenConsumption}
        activeOpacity={0.7}
      >
        <IconConsumptionWidget size={18} />
        <Text style={styles.label}>Log Food</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#FFFFFF',
    borderRadius: theme.borderRadius.xlarge,
    paddingVertical: 14,
    paddingHorizontal: 8,
    marginHorizontal: 16,
    marginVertical: 14,
    borderWidth: 1,
    borderColor: '#E2E8F0',
    justifyContent: 'space-between',
    ...theme.shadows.subtle,
    ...Platform.select({
      web: {
        background: 'linear-gradient(135deg, rgba(255, 255, 255, 0.9) 0%, rgba(255, 255, 255, 0.75) 100%)',
        backdropFilter: 'blur(20px)',
        boxShadow: '0 8px 24px rgba(129, 75, 146, 0.06)',
      } as any
    })
  },
  button: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 6,
    gap: 8,
  },
  divider: {
    width: 1.5,
    height: 24,
    backgroundColor: '#F1F5F9',
  },
  label: {
    fontSize: 12,
    fontWeight: theme.typography.weight.bold,
    color: theme.colors.textDark,
  }
});
