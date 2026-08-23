import React from 'react';
import { View, Text, StyleSheet, ScrollView, ViewStyle, Platform } from 'react-native';
import { theme } from '../../styles/theme';

interface TableProps {
  headers: string[];
  rows: React.ReactNode[][];
  style?: ViewStyle;
}

export default function Table({ headers, rows, style }: TableProps) {
  const isWeb = typeof window !== 'undefined';
  return (
    <View style={[styles.container, isWeb && styles.glassContainer, style]}>
      <ScrollView horizontal={true} showsHorizontalScrollIndicator={false}>
        <View style={styles.tableInner}>
          {/* Header Row */}
          <View style={styles.headerRow}>
            {headers.map((header, idx) => (
              <View key={idx} style={[styles.cell, { flex: 1, minWidth: 100 }]}>
                <Text style={styles.headerText}>{header}</Text>
              </View>
            ))}
          </View>

          {/* Data Rows */}
          {rows.map((row, rowIdx) => (
            <View key={rowIdx} style={styles.dataRow}>
              {row.map((cellContent, cellIdx) => (
                <View key={cellIdx} style={[styles.cell, { flex: 1, minWidth: 100 }]}>
                  {typeof cellContent === 'string' || typeof cellContent === 'number' ? (
                    <Text style={styles.cellText}>{cellContent}</Text>
                  ) : (
                    cellContent
                  )}
                </View>
              ))}
            </View>
          ))}
        </View>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    backgroundColor: Platform.OS === 'web' ? theme.colors.glassBg : 'rgba(30, 30, 30, 0.7)',
    borderRadius: theme.borderRadius.xlarge,
    borderWidth: 1,
    borderColor: theme.colors.glassBorder,
    overflow: 'hidden',
    width: '100%',
  },
  glassContainer: {
    backdropFilter: 'blur(16px)',
    boxShadow: '0 8px 32px 0 rgba(0, 0, 0, 0.15)',
  } as any,
  tableInner: {
    minWidth: '100%',
  },
  headerRow: {
    flexDirection: 'row',
    backgroundColor: 'rgba(255, 255, 255, 0.04)',
    borderBottomWidth: 1,
    borderBottomColor: theme.colors.glassBorder,
    paddingVertical: 12,
    paddingHorizontal: 16,
  },
  headerText: {
    fontSize: theme.typography.size.xsmall,
    fontWeight: theme.typography.weight.bold,
    color: theme.colors.textMuted,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  },
  dataRow: {
    flexDirection: 'row',
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(255, 255, 255, 0.05)',
    paddingVertical: 12,
    paddingHorizontal: 16,
    alignItems: 'center',
  },
  cell: {
    justifyContent: 'center',
    paddingRight: 8,
  },
  cellText: {
    fontSize: theme.typography.size.regular,
    color: theme.colors.textLight,
    fontWeight: theme.typography.weight.medium,
  },
});
