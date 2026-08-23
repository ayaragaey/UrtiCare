import React from 'react';
import { View, Text, StyleSheet, Platform } from 'react-native';
import { useTrackerStore } from '../store/useTrackerStore';
import { useLiveTimer } from '../hooks/useLiveTimer';
import { theme } from '../styles/theme';

export default function LiveCounter() {
  const lastAntihistamine = useTrackerStore(state => {
    const ahList = state.entries.filter(e => e.type === 'ANTIHISTAMINE');
    return ahList.length > 0 ? ahList[0].timestamp : undefined;
  });

  const elapsed = useLiveTimer(lastAntihistamine);

  return (
    <View style={styles.card}>
      <View style={styles.pulseContainer}>
        <View style={[styles.pulseDot, lastAntihistamine ? styles.activeDot : styles.inactiveDot]} />
        <Text style={styles.label}>Last antihistamine was</Text>
      </View>
      
      <Text style={[styles.timer, lastAntihistamine ? styles.activeTimer : styles.inactiveTimer]}>
        {lastAntihistamine ? elapsed : 'No dosage recorded'}
      </Text>

      {lastAntihistamine && (
        <View style={styles.statusBadge}>
          <Text style={styles.indicatorText}>
            ⏱ Live Tracking Active
          </Text>
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    backgroundColor: 'rgba(80, 151, 41, 0.12)', // Tinted green glass background
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.8)', // Translucent border
    borderRadius: 20, // 20px radius
    padding: 24,
    marginHorizontal: 16,
    marginVertical: 8,
    alignItems: 'center',
    justifyContent: 'center',
    ...Platform.select({
      ios: {
        shadowColor: '#323246',
        shadowOffset: { width: 0, height: 6 },
        shadowOpacity: 0.04,
        shadowRadius: 10,
      },
      android: {
        elevation: 0,
      },
      web: {
        backdropFilter: 'blur(18px)',
        boxShadow: '0 6px 15px rgba(50, 50, 70, 0.04)',
      }
    }),
  },
  pulseContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 8,
  },
  pulseDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    marginRight: 8,
  },
  activeDot: {
    backgroundColor: theme.colors.successGreen, // Green status dot
  },
  inactiveDot: {
    backgroundColor: theme.colors.neutralGrey,
  },
  label: {
    color: theme.colors.neutralGrey,
    fontSize: theme.typography.size.small,
    fontWeight: theme.typography.weight.semibold,
  },
  timer: {
    fontSize: 38, // Dominant visual element
    fontWeight: theme.typography.weight.bold,
    textAlign: 'center',
    marginVertical: 4,
    letterSpacing: -0.5,
  },
  activeTimer: {
    color: theme.colors.successGreen, // Green timer digits
  },
  inactiveTimer: {
    color: theme.colors.neutralGrey,
  },
  statusBadge: {
    backgroundColor: 'rgba(80, 151, 41, 0.12)',
    paddingVertical: 4,
    paddingHorizontal: 10,
    borderRadius: theme.borderRadius.full,
    marginTop: 6,
  },
  indicatorText: {
    color: theme.colors.successGreen,
    fontSize: 10,
    fontWeight: theme.typography.weight.bold,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  }
});
