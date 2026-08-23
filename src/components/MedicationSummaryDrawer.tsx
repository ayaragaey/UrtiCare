import React, { useState, useMemo } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Platform } from 'react-native';
import { useTrackerStore } from '../store/useTrackerStore';
import { getMovingWindowCount, getMonthCounts, MONTH_NAMES } from '../utils/dateHelpers';
import { theme } from '../styles/theme';
import { IconChevronDown } from './common/CustomIcons';

export default function MedicationSummaryDrawer() {
  const entries = useTrackerStore(state => state.entries);
  const [isOpen, setIsOpen] = useState(true);
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

  const metrics = useMemo(() => {
    const now = new Date();
    return {
      ah24: getMovingWindowCount(entries, 'ANTIHISTAMINE', 24, now),
      ah48: getMovingWindowCount(entries, 'ANTIHISTAMINE', 48, now),
      ah72: getMovingWindowCount(entries, 'ANTIHISTAMINE', 72, now),
    };
  }, [entries]);

  const targetYear = navDate.getFullYear();
  const targetMonth = navDate.getMonth();
  const monthName = MONTH_NAMES[targetMonth];

  const monthlyAH = useMemo(() => getMonthCounts(entries, 'ANTIHISTAMINE', targetYear, targetMonth), [entries, targetYear, targetMonth]);
  const monthlyCortisone = useMemo(() => getMonthCounts(entries, 'CORTISONE', targetYear, targetMonth), [entries, targetYear, targetMonth]);
  const monthlyFlare = useMemo(() => getMonthCounts(entries, 'FLARE_UP', targetYear, targetMonth), [entries, targetYear, targetMonth]);

  return (
    <View style={styles.container}>
      {/* Drawer Toggle Header */}
      <TouchableOpacity onPress={() => setIsOpen(!isOpen)} activeOpacity={0.8} style={styles.header}>
        <View style={styles.headerTitleRow}>
          <Text style={styles.headerTitle}>Medication Summary</Text>
        </View>
        <View style={[styles.arrowContainer, isOpen && styles.arrowOpen]}>
          <IconChevronDown color={theme.colors.neutralGrey} size={14} />
        </View>
      </TouchableOpacity>

      {/* Accordion Body */}
      {isOpen && (
        <View style={styles.body}>
          {/* Moving Lookbacks - Styled in Teal */}
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

          {/* Calendar navigation */}
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

          {/* List display */}
          <View style={styles.monthlyList}>
            {/* Antihistamine */}
            <View style={styles.monthlyRow}>
              <View style={styles.monthlyRowLeft}>
                <View style={[styles.bulletDot, { backgroundColor: theme.colors.secondaryTeal }]} />
                <Text style={styles.monthlyRowLabel}>Antihistamines Taken</Text>
              </View>
              <Text style={styles.monthlyRowValue}>
                {monthlyAH} pill{monthlyAH === 1 ? '' : 's'}
              </Text>
            </View>

            {/* Cortisone */}
            <View style={styles.monthlyRow}>
              <View style={styles.monthlyRowLeft}>
                <View style={[styles.bulletDot, { backgroundColor: theme.colors.successGreen }]} />
                <Text style={styles.monthlyRowLabel}>Cortisone Dosages</Text>
              </View>
              <Text style={styles.monthlyRowValue}>
                {monthlyCortisone} dose{monthlyCortisone === 1 ? '' : 's'}
              </Text>
            </View>

            {/* Flare Up */}
            <View style={styles.monthlyRow}>
              <View style={styles.monthlyRowLeft}>
                <View style={[styles.bulletDot, { backgroundColor: theme.colors.primaryPurple }]} />
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
    backgroundColor: theme.colors.glassBg,
    borderWidth: 1,
    borderColor: theme.colors.glassBorder,
    borderRadius: 20, // 20px radius
    marginHorizontal: 16,
    marginVertical: 12,
    overflow: 'hidden',
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
        boxShadow: '0 6px 20px rgba(50, 50, 70, 0.04)',
      }
    }),
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 16,
    paddingHorizontal: 18,
    backgroundColor: 'transparent',
    borderBottomWidth: 1,
    borderBottomColor: theme.colors.glassBorder,
  },
  headerTitleRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  headerTitle: {
    color: '#814B92',
    fontSize: theme.typography.size.regular,
    fontWeight: theme.typography.weight.bold,
    ...Platform.select({
      web: {
        background: 'linear-gradient(90deg, #814B92 0%, #509729 50%, #1A7E97 100%)',
        WebkitBackgroundClip: 'text',
        WebkitTextFillColor: 'transparent',
      } as any
    })
  },
  arrowContainer: {
    transform: [{ rotate: '0deg' }],
  },
  arrowOpen: {
    transform: [{ rotate: '180deg' }],
  },
  body: {
    padding: 16,
    backgroundColor: 'transparent',
  },
  sectionTitle: {
    color: theme.colors.neutralGrey,
    fontSize: theme.typography.size.xsmall,
    fontWeight: theme.typography.weight.bold,
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
    backgroundColor: 'transparent',
    borderWidth: 1,
    borderColor: '#737373',
    borderRadius: theme.borderRadius.medium,
    paddingVertical: 12,
    alignItems: 'center',
    justifyContent: 'center',
  },
  matrixLabel: {
    color: '#737373',
    fontSize: 9,
    fontWeight: theme.typography.weight.bold,
    marginBottom: 4,
    letterSpacing: 0.5,
  },
  matrixVal: {
    color: '#737373',
    fontSize: theme.typography.size.xlarge,
    fontWeight: theme.typography.weight.heavy,
  },
  matrixSub: {
    color: '#737373',
    fontSize: 9,
    fontWeight: theme.typography.weight.bold,
    marginTop: 2,
    textTransform: 'uppercase',
  },
  divider: {
    height: 1,
    backgroundColor: theme.colors.glassBorder,
    marginVertical: 16,
  },
  calendarHeader: {
    marginBottom: 12,
  },
  navControls: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: 'rgba(255, 255, 255, 0.4)',
    borderWidth: 1,
    borderColor: theme.colors.glassBorder,
    borderRadius: theme.borderRadius.small,
    padding: 4,
    marginTop: 4,
  },
  navBtn: {
    paddingHorizontal: 8,
    alignItems: 'center',
    justifyContent: 'center',
  },
  navBtnText: {
    color: '#64748B',
    fontSize: 11,
    fontWeight: 'bold',
  },
  monthDisplay: {
    color: theme.colors.textDark,
    fontSize: theme.typography.size.small,
    fontWeight: theme.typography.weight.bold,
  },
  monthlyList: {
    backgroundColor: 'rgba(255, 255, 255, 0.4)',
    borderWidth: 1,
    borderColor: theme.colors.glassBorder,
    borderRadius: theme.borderRadius.medium,
    paddingHorizontal: 12,
    paddingVertical: 4,
  },
  monthlyRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 10,
    borderBottomWidth: 1,
    borderBottomColor: theme.colors.glassBorder,
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
    color: theme.colors.textDark,
    fontSize: theme.typography.size.small,
    fontWeight: theme.typography.weight.semibold,
  },
  monthlyRowValue: {
    color: theme.colors.textDark,
    fontSize: theme.typography.size.small,
    fontWeight: theme.typography.weight.bold,
  }
});
