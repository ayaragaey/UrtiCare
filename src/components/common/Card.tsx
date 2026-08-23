import React from 'react';
import { View, Text, StyleSheet, ViewStyle, Platform } from 'react-native';
import { theme } from '../../styles/theme';
import Badge from './Badge';

interface CardProps {
  children?: React.ReactNode;
  style?: ViewStyle | ViewStyle[];
}

export function Card({ children, style }: CardProps) {
  const isWeb = typeof window !== 'undefined';
  return (
    <View style={[styles.cardBase, isWeb && styles.cardGlass, style]}>
      {children}
    </View>
  );
}

interface KPICardProps {
  title: string;
  value: string;
  trendLabel?: string;
  isPositive?: boolean;
  style?: ViewStyle | ViewStyle[];
}

export function KPICard({ title, value, trendLabel, isPositive = true, style }: KPICardProps) {
  const isWeb = typeof window !== 'undefined';
  return (
    <View style={[styles.cardBase, styles.kpiCard, isWeb && styles.cardGlass, style]}>
      <Text style={styles.kpiTitle}>{title}</Text>
      <Text style={styles.kpiValue}>{value}</Text>
      {trendLabel && (
        <View style={styles.trendRow}>
          <Badge
            label={trendLabel}
            type={isPositive ? 'completed' : 'inactive'}
            style={styles.trendBadge}
          />
          <Text style={styles.trendLabelText}>vs last month</Text>
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  cardBase: {
    backgroundColor: Platform.OS === 'web' ? theme.colors.glassBg : 'rgba(30, 30, 30, 0.75)',
    borderRadius: theme.borderRadius.xlarge, // 18px (corners between 16-20px)
    padding: 20,
    borderWidth: 1,
    borderColor: theme.colors.glassBorder,
    ...theme.shadows.subtle,
  },
  cardGlass: {
    // Add premium web glassmorphism styles
    backdropFilter: 'blur(16px)',
    boxShadow: '0 8px 32px 0 rgba(0, 0, 0, 0.15)',
  } as any,
  kpiCard: {
    minWidth: 150,
    flex: 1,
  },
  kpiTitle: {
    color: theme.colors.textMuted,
    fontSize: theme.typography.size.small,
    fontWeight: theme.typography.weight.bold,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
    marginBottom: 8,
  },
  kpiValue: {
    color: theme.colors.textLight,
    fontSize: theme.typography.size.title,
    fontWeight: theme.typography.weight.heavy,
  },
  trendRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 12,
  },
  trendBadge: {
    marginRight: 8,
  },
  trendLabelText: {
    fontSize: theme.typography.size.xsmall,
    color: theme.colors.textMuted,
    fontWeight: theme.typography.weight.medium,
  },
});
