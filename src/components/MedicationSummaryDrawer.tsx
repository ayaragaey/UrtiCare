import React, { useState, useMemo } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Animated, Platform } from 'react-native';
import { useTrackerStore } from '../store/useTrackerStore';
import { getMovingWindowCount, getMonthCounts, MONTH_NAMES } from '../utils/dateHelpers';

export default function MedicationSummaryDrawer() {
  const entries = useTrackerStore(state => state.entries);
  const [isOpen, setIsOpen] = useState(true); // default open for gorgeous landing view
  
  // States for calendar month navigation
  const [navDate, setNavDate] = useState(() => new Date());

  const handlePrevMonth = () => {
    setNavDate(prev => {
      const d = new Date(prev);
      d.setMonth(d.getMonth() - 1);
      return d;
    });
  };

  const handleNextMonth = () => {
    setNavDate(prev => {
      const d = new Date(prev);
      d.setMonth(d.getMonth() + 1);
      return d;
    });
  };

  // Perform memoized lookbacks based on current date
  const metrics = useMemo(() => {
    const now = new Date();
    return {
      ah24: getMovingWindowCount(entries, 'ANTIHISTAMINE', 24, now),
      ah48: getMovingWindowCount(entries, 'ANTIHISTAMINE', 48, now),
      ah72: getMovingWindowCount(entries, 'ANTIHISTAMINE', 72, now),
      
      flare24: getMovingWindowCount(entries, 'FLARE_UP', 24, now),
      cortisone24: getMovingWindowCount(entries, 'CORTISONE', 24, now),
    };
  }, [entries]);

  // Calculate monthly stats based on navigated month
  const targetYear = navDate.getFullYear();
  const targetMonth = navDate.getMonth();
  const monthName = MONTH_NAMES[targetMonth];

  const monthlyAH = useMemo(() => getMonthCounts(entries, 'ANTIHISTAMINE', targetYear, targetMonth), [entries, targetYear, targetMonth]);
  const monthlyCortisone = useMemo(() => getMonthCounts(entries, 'CORTISONE', targetYear, targetMonth), [entries, targetYear, targetMonth]);
  const monthlyFlare = useMemo(() => getMonthCounts(entries, 'FLARE_UP', targetYear, targetMonth), [entries, targetYear, targetMonth]);

  const toggleOpen = () => {
    setIsOpen(!isOpen);
  };

  return (
    <View style={styles.container}>
      {/* Collapse Action Toggle Header */}
      <TouchableOpacity onPress={toggleOpen} activeOpacity={0.8} style={styles.header}>
        <View style={styles.headerTitleRow}>
          <Text style={styles.icon}>📊</Text>
          <Text style={styles.headerTitle}>Medication Summary & Insights</Text>
        </View>
        <Text style={styles.arrow}>{isOpen ? '▲' : '▼'}</Text>
      </TouchableOpacity>

      {/* Expanded Accordion Body */}
      {isOpen && (
        <View style={styles.body}>
          
          {/* Time-box Lookback Aggregation Matrix */}
          <Text style={styles.sectionTitle}>MOVING LOOKBACK WINDOWS (ANTIHISTAMINE)</Text>
          <View style={styles.matrixRow}>
            <View style={styles.matrixCard}>
              <Text style={styles.matrixLabel}>LAST 24H</Text>
              <Text style={styles.matrixVal}>{metrics.ah24}</Text>
              <Text style={styles.matrixSub}>pill{metrics.ah24 === 1 ? '' : 's'}</Text>
            </View>

            <View style={styles.matrixCard}>
              <Text style={styles.matrixLabel}>LAST 48H</Text>
              <Text style={styles.matrixVal}>{metrics.ah48}</Text>
              <Text style={styles.matrixSub}>pill{metrics.ah48 === 1 ? '' : 's'}</Text>
            </View>

            <View style={styles.matrixCard}>
              <Text style={styles.matrixLabel}>LAST 72H</Text>
              <Text style={styles.matrixVal}>{metrics.ah72}</Text>
              <Text style={styles.matrixSub}>pill{metrics.ah72 === 1 ? '' : 's'}</Text>
            </View>
          </View>

          <View style={styles.divider} />

          {/* Rolling Calendar Monthly Summary Overview */}
          <View style={styles.calendarHeader}>
            <Text style={styles.sectionTitle}>MONTHLY CALENDAR OVERVIEW</Text>
            <View style={styles.navControls}>
              <TouchableOpacity onPress={handlePrevMonth} style={styles.navBtn}>
                <Text style={styles.navBtnText}>◀</Text>
              </TouchableOpacity>
              
              <Text style={styles.monthDisplay}>{monthName} {targetYear}</Text>
              
              <TouchableOpacity onPress={handleNextMonth} style={styles.navBtn}>
                <Text style={styles.navBtnText}>▶</Text>
              </TouchableOpacity>
            </View>
          </View>

          <View style={styles.monthlyList}>
            {/* Antihistamine Total */}
            <View style={styles.monthlyRow}>
              <View style={styles.monthlyRowLeft}>
                <View style={[styles.bulletDot, { backgroundColor: '#509729' }]} />
                <Text style={styles.monthlyRowLabel}>Antihistamines Taken</Text>
              </View>
              <Text style={styles.monthlyRowValue}>
                {monthlyAH} pill{monthlyAH === 1 ? '' : 's'}
              </Text>
            </View>

            {/* Cortisone/Steroid Total */}
            <View style={styles.monthlyRow}>
              <View style={styles.monthlyRowLeft}>
                <View style={[styles.bulletDot, { backgroundColor: '#1b8097' }]} />
                <Text style={styles.monthlyRowLabel}>Cortisone Dosages</Text>
              </View>
              <Text style={styles.monthlyRowValue}>
                {monthlyCortisone} dose{monthlyCortisone === 1 ? '' : 's'}
              </Text>
            </View>

            {/* Symptom Flare Up Total */}
            <View style={styles.monthlyRow}>
              <View style={styles.monthlyRowLeft}>
                <View style={[styles.bulletDot, { backgroundColor: '#814b92' }]} />
                <Text style={styles.monthlyRowLabel}>Symptom Flare-ups</Text>
              </View>
              <Text style={styles.monthlyRowValue}>
                {monthlyFlare} event{monthlyFlare === 1 ? '' : 's'}
              </Text>
            </View>
          </View>

        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    backgroundColor: '#FFFFFF',
    borderWidth: 1,
    borderColor: '#EAEAEA',
    borderRadius: 16,
    marginHorizontal: 16,
    marginVertical: 12,
    overflow: 'hidden',
    ...Platform.select({
      ios: {
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 6 },
        shadowOpacity: 0.1,
        shadowRadius: 10,
      },
      android: {
        elevation: 6,
      },
      web: {
        boxShadow: '0 6px 20px rgba(0, 0, 0, 0.05)',
      }
    }),
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 16,
    paddingHorizontal: 18,
    backgroundColor: '#FAFAFA',
    borderBottomWidth: 1,
    borderBottomColor: '#EEEEEE',
  },
  headerTitleRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  icon: {
    marginRight: 10,
    fontSize: 16,
  },
  headerTitle: {
    color: '#111111',
    fontSize: 14,
    fontWeight: '700',
    letterSpacing: 0.3,
  },
  arrow: {
    color: '#888888',
    fontSize: 10,
  },
  body: {
    padding: 16,
    backgroundColor: '#FFFFFF',
  },
  sectionTitle: {
    color: '#737373',
    fontSize: 10,
    fontWeight: '800',
    letterSpacing: 1,
    marginBottom: 10,
  },
  matrixRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    gap: 8,
  },
  matrixCard: {
    flex: 1,
    backgroundColor: '#F9F9F9',
    borderWidth: 1,
    borderColor: '#EEEEEE',
    borderRadius: 10,
    paddingVertical: 12,
    alignItems: 'center',
    justifyContent: 'center',
  },
  matrixLabel: {
    color: '#666666',
    fontSize: 9,
    fontWeight: '700',
    marginBottom: 4,
    letterSpacing: 0.5,
  },
  matrixVal: {
    color: '#1b8097', // Premium dark contrast teal
    fontSize: 22,
    fontWeight: '800',
  },
  matrixSub: {
    color: '#666666',
    fontSize: 9,
    fontWeight: '600',
    marginTop: 2,
    textTransform: 'uppercase',
  },
  divider: {
    height: 1,
    backgroundColor: '#EEEEEE',
    marginVertical: 16,
  },
  calendarHeader: {
    marginBottom: 12,
  },
  navControls: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: '#F9F9F9',
    borderWidth: 1,
    borderColor: '#EAEAEA',
    borderRadius: 8,
    padding: 4,
    marginTop: 4,
  },
  navBtn: {
    padding: 8,
    borderRadius: 6,
    backgroundColor: '#EAEAEA',
    minWidth: 32,
    alignItems: 'center',
  },
  navBtnText: {
    color: '#555555',
    fontSize: 10,
    fontWeight: '800',
  },
  monthDisplay: {
    color: '#111111',
    fontSize: 12,
    fontWeight: '700',
  },
  monthlyList: {
    backgroundColor: '#F9F9F9',
    borderWidth: 1,
    borderColor: '#EAEAEA',
    borderRadius: 10,
    paddingHorizontal: 12,
    paddingVertical: 4,
  },
  monthlyRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 10,
    borderBottomWidth: 1,
    borderBottomColor: '#EEEEEE',
  },
  monthlyRowLeft: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  bulletDot: {
    width: 6,
    height: 6,
    borderRadius: 3,
    marginRight: 8,
  },
  monthlyRowLabel: {
    color: '#333333',
    fontSize: 12,
    fontWeight: '600',
  },
  monthlyRowValue: {
    color: '#111111',
    fontSize: 12,
    fontWeight: '700',
  }
});
