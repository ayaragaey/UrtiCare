import React from 'react';
import { View, Text, StyleSheet, Platform, ViewStyle } from 'react-native';
import { theme } from '../../styles/theme';
import Badge from './Badge';

interface ChartProps {
  data: number[];
  labels: string[];
  height?: number;
  style?: ViewStyle;
}

export default function Chart({ data, labels, height = 150, style }: ChartProps) {
  const maxVal = Math.max(...data, 1);
  const isWeb = typeof window !== 'undefined';

  return (
    <View style={[styles.container, isWeb && styles.glassContainer, style]}>
      {/* Top Header Row */}
      <View style={styles.chartHeader}>
        <View>
          <Text style={styles.chartTitle}>Activity Analytics</Text>
          <Text style={styles.chartSubtitle}>Frequency trends over time</Text>
        </View>
        <Badge label="↑ 12.4%" type="completed" />
      </View>

      {/* Main Grid & Bars */}
      <View style={[styles.chartBody, { height }]}>
        <View style={styles.gridLinesContainer}>
          <View style={styles.gridLine} />
          <View style={styles.gridLine} />
          <View style={styles.gridLine} />
          <View style={styles.gridLine} />
        </View>

        <View style={styles.barsRow}>
          {data.map((val, idx) => {
            const barHeightPct = (val / maxVal) * 100;
            return (
              <View key={idx} style={styles.barColumn}>
                <View style={styles.barTrack}>
                  <View
                    style={[
                      styles.barFill,
                      {
                        height: `${barHeightPct}%`,
                      },
                    ]}
                  />
                </View>
                <Text style={styles.barLabel}>{labels[idx]}</Text>
              </View>
            );
          })}
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    backgroundColor: Platform.OS === 'web' ? theme.colors.glassBg : 'rgba(30, 30, 30, 0.7)',
    borderRadius: theme.borderRadius.xlarge,
    padding: 16,
    borderWidth: 1,
    borderColor: theme.colors.glassBorder,
    width: '100%',
    ...theme.shadows.subtle,
  },
  glassContainer: {
    backdropFilter: 'blur(16px)',
    boxShadow: '0 8px 32px 0 rgba(0, 0, 0, 0.15)',
  } as any,
  chartHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
  },
  chartTitle: {
    fontSize: theme.typography.size.regular,
    fontWeight: theme.typography.weight.bold,
    color: theme.colors.textLight,
  },
  chartSubtitle: {
    fontSize: theme.typography.size.xsmall,
    color: theme.colors.textMuted,
    marginTop: 2,
  },
  chartBody: {
    position: 'relative',
    justifyContent: 'flex-end',
    width: '100%',
  },
  gridLinesContainer: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 20,
    justifyContent: 'space-between',
  },
  gridLine: {
    height: 1,
    backgroundColor: 'rgba(255, 255, 255, 0.05)',
    width: '100%',
  },
  barsRow: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    justifyContent: 'space-around',
    height: '100%',
    zIndex: 2,
  },
  barColumn: {
    alignItems: 'center',
    height: '100%',
    justifyContent: 'flex-end',
    flex: 1,
  },
  barTrack: {
    flex: 1,
    width: 14,
    backgroundColor: 'rgba(255, 255, 255, 0.05)',
    borderRadius: theme.borderRadius.small,
    justifyContent: 'flex-end',
    overflow: 'hidden',
    marginBottom: 6,
  },
  barFill: {
    width: '100%',
    backgroundColor: theme.colors.secondaryTeal, // Teal for analytics/charts
    borderRadius: theme.borderRadius.small,
  },
  barLabel: {
    fontSize: 9,
    color: theme.colors.textMuted,
    fontWeight: theme.typography.weight.semibold,
  },
});
