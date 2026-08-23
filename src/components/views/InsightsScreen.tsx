import React, { useMemo } from 'react';
import { View, Text, StyleSheet, ScrollView, TouchableOpacity, Platform } from 'react-native';
import { theme } from '../../styles/theme';
import Chart from '../common/Chart';
import MedicationSummaryDrawer from '../MedicationSummaryDrawer';
import { IconBackArrow } from '../common/CustomIcons';
import { useTrackerStore } from '../../store/useTrackerStore';
import { format, subDays, startOfDay, endOfDay, isWithinInterval, parseISO } from 'date-fns';

export default function InsightsScreen() {
  const entries = useTrackerStore(state => state.entries);

  const { chartData, chartLabels } = useMemo(() => {
    const now = new Date();
    const data: number[] = [];
    const labels: string[] = [];

    for (let i = 6; i >= 0; i--) {
      const d = subDays(now, i);
      const dayStart = startOfDay(d);
      const dayEnd = endOfDay(d);
      const count = entries.filter(e => {
        try {
          const entryDate = parseISO(e.timestamp);
          return isWithinInterval(entryDate, { start: dayStart, end: dayEnd });
        } catch {
          return false;
        }
      }).length;

      data.push(count);
      labels.push(format(d, 'EEE'));
    }

    return { chartData: data, chartLabels: labels };
  }, [entries]);

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      {/* Header Card with Signature Tricolor Gradient Back Arrow */}
      <View style={styles.headerCard}>
        {Platform.OS === 'web' && <View style={styles.topGradientBar as any} />}
        <View style={styles.headerRow}>
          <TouchableOpacity style={styles.backBtn} activeOpacity={0.75} onPress={() => {}}>
            <IconBackArrow size={18} />
          </TouchableOpacity>
          <Text style={styles.headerTitle}>My Pattern</Text>
        </View>
        <Text style={styles.subtitle}>Pattern analytics and medication stability logs</Text>
      </View>

      <Chart
        data={chartData}
        labels={chartLabels}
        style={{ marginVertical: 16 }}
      />

      <MedicationSummaryDrawer />
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: theme.colors.bgLight,
  },
  content: {
    padding: 16,
    paddingBottom: 100,
  },
  headerCard: {
    backgroundColor: '#FFFFFF',
    borderRadius: theme.borderRadius.xlarge,
    padding: 20,
    borderWidth: 1,
    borderColor: '#E2E8F0',
    position: 'relative',
    overflow: 'hidden',
    ...theme.shadows.subtle,
  },
  topGradientBar: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    height: 4,
    background: 'linear-gradient(90deg, #814B92 0%, #509729 50%, #1A7E97 100%)',
  } as any,
  headerRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
  },
  backBtn: {
    width: 34,
    height: 34,
    borderRadius: 17,
    backgroundColor: '#FFFFFF',
    borderWidth: 1,
    borderColor: '#E2E8F0',
    justifyContent: 'center',
    alignItems: 'center',
    ...theme.shadows.subtle,
  },
  headerTitle: {
    fontSize: 20,
    fontWeight: theme.typography.weight.heavy,
    color: theme.colors.textDark,
  },
  subtitle: {
    fontSize: 13,
    color: theme.colors.textMuted,
    marginTop: 6,
  },
});
