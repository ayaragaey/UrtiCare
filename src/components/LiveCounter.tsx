import React from 'react';
import { View, Text, StyleSheet, Platform } from 'react-native';
import { useTrackerStore } from '../store/useTrackerStore';
import { useLiveTimer } from '../hooks/useLiveTimer';

export default function LiveCounter() {
  // Extract ONLY the latest Antihistamine entry's timestamp.
  // This memoized selection ensures this component re-renders ONLY when the last dose timestamp actually changes,
  // while the internal ticker operates inside the hook.
  const lastAntihistamine = useTrackerStore(state => {
    const ahList = state.entries.filter(e => e.type === 'ANTIHISTAMINE');
    return ahList.length > 0 ? ahList[0].timestamp : undefined;
  });

  const elapsed = useLiveTimer(lastAntihistamine);

  return (
    <View style={styles.card}>
      <View style={styles.pulseContainer}>
        <View style={[styles.pulseDot, lastAntihistamine ? styles.activeDot : styles.inactiveDot]} />
        <Text style={styles.label}>LAST ANTIHISTAMINE WAS</Text>
      </View>
      
      <Text style={[styles.timer, lastAntihistamine ? styles.activeTimer : styles.inactiveTimer]}>
        {lastAntihistamine ? elapsed : 'No dosage recorded'}
      </Text>

      {lastAntihistamine && (
        <Text style={styles.indicatorText}>
          ● Live elapsed time tracker active
        </Text>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    backgroundColor: '#FFFFFF',
    borderWidth: 1,
    borderColor: '#EAEAEA',
    borderRadius: 16,
    padding: 20,
    marginHorizontal: 16,
    marginVertical: 12,
    alignItems: 'center',
    justifyContent: 'center',
    ...Platform.select({
      ios: {
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.1,
        shadowRadius: 6,
      },
      android: {
        elevation: 4,
      },
      web: {
        boxShadow: '0 4px 15px rgba(0, 0, 0, 0.05)',
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
    backgroundColor: '#1b8097',
  },
  inactiveDot: {
    backgroundColor: '#CCCCCC',
  },
  label: {
    color: '#666666',
    fontSize: 11,
    fontWeight: '700',
    letterSpacing: 1.5,
  },
  timer: {
    fontSize: 28,
    fontWeight: '800',
    textAlign: 'center',
    marginVertical: 4,
    letterSpacing: -0.5,
  },
  activeTimer: {
    color: '#1b8097', // Premium dark contrast teal
  },
  inactiveTimer: {
    color: '#888888',
  },
  indicatorText: {
    color: '#888888',
    fontSize: 10,
    fontWeight: '600',
    marginTop: 6,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  }
});
