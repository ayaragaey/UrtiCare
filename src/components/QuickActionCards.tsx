import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { useTrackerStore } from '../store/useTrackerStore';
import { theme } from '../styles/theme';
import { IconDroplet, IconCapsule, IconTablet, IconTookAPill, IconFlareUpWidget, IconConsumptionWidget } from './common/CustomIcons';

interface Props {
  onOpenConsumption?: () => void;
}

export default function QuickActionCards({ onOpenConsumption }: Props) {
  const addEntry = useTrackerStore(state => state.addEntry);
  const setCollisionWarning = useTrackerStore(state => state.setCollisionWarning);
  const favoriteAntihistamine = useTrackerStore(state => state.favoriteAntihistamine);
  const defaultFlareUpSymptom = useTrackerStore(state => state.defaultFlareUpSymptom);
  const defaultFlareUpSeverity = useTrackerStore(state => state.defaultFlareUpSeverity);

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
      {/* 1. Flare Up! Card */}
      <TouchableOpacity
        style={[styles.card, styles.flareUpCard]}
        onPress={handleFlareUp}
        activeOpacity={0.75}
      >
        <View style={[styles.iconCircle, styles.flareUpIconCircle, { borderWidth: 0, backgroundColor: 'transparent' }]}>
          <IconFlareUpWidget size={24} />
        </View>
        <Text style={styles.cardLabel}>Flare Up!</Text>
      </TouchableOpacity>

      {/* 2. Took A Pill! Card (#509729) */}
      <TouchableOpacity
        style={[styles.card, styles.antihistamineCard]}
        onPress={handleTookAPill}
        activeOpacity={0.75}
      >
        <View style={[styles.iconCircle, styles.antihistamineIconCircle, { borderWidth: 0, backgroundColor: 'transparent' }]}>
          <IconTookAPill size={24} />
        </View>
        <Text style={styles.cardLabel}>Took A Pill!</Text>
      </TouchableOpacity>

      {/* 3. Corticosteroids Card (#1a7e97) */}
      <TouchableOpacity
        style={[styles.card, styles.corticosteroidsCard]}
        onPress={() => handleAction('CORTISONE')}
        activeOpacity={0.75}
      >
        <View style={[styles.iconCircle, styles.corticosteroidsIconCircle]}>
          <IconTablet color="#1a7e97" size={24} />
        </View>
        <Text style={styles.cardLabel}>Corticosteroids</Text>
      </TouchableOpacity>

      {/* 4. Consumption! Card (#F78325) */}
      <TouchableOpacity
        style={[styles.card, styles.consumptionCard]}
        onPress={() => {
          if (onOpenConsumption) onOpenConsumption();
        }}
        activeOpacity={0.75}
      >
        <View style={[styles.iconCircle, styles.consumptionIconCircle, { borderWidth: 0, backgroundColor: 'transparent' }]}>
          <IconConsumptionWidget size={24} />
        </View>
        <Text style={styles.cardLabel}>Consumption!</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    marginVertical: 16,
    width: '100%',
    gap: 12,
  },
  card: {
    flex: 1,
    backgroundColor: '#FFFFFF',
    borderRadius: theme.borderRadius.xlarge,
    paddingVertical: 16,
    paddingHorizontal: 8,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1.5,
    ...theme.shadows.subtle,
  },
  flareUpCard: {
    borderColor: '#F5F3FF',
  },
  antihistamineCard: {
    borderColor: '#ECFDF5',
  },
  corticosteroidsCard: {
    borderColor: '#F0FDFA',
  },
  iconCircle: {
    width: 48,
    height: 48,
    borderRadius: 24,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 8,
    borderWidth: 2,
  },
  flareUpIconCircle: {
    borderColor: '#814B92',
    backgroundColor: '#F5F3FF',
  },
  antihistamineIconCircle: {
    borderColor: '#509729',
    backgroundColor: '#ECFDF5',
  },
  corticosteroidsIconCircle: {
    borderColor: '#1a7e97',
    backgroundColor: '#F0FDFA',
  },
  consumptionCard: {
    borderColor: '#FFF7ED',
  },
  consumptionIconCircle: {
    borderColor: '#F78325',
    backgroundColor: '#FFF7ED',
  },
  cardLabel: {
    fontSize: 12,
    fontWeight: theme.typography.weight.bold,
    color: theme.colors.textDark,
    textAlign: 'center',
  },
});
