import React from 'react';
import { View, StyleSheet, ViewStyle } from 'react-native';
import { theme } from '../../styles/theme';

interface ProgressBarProps {
  progress: number; // 0 to 1
  color?: string;
  style?: ViewStyle;
}

export default function ProgressBar({ progress, color = theme.colors.primaryPurple, style }: ProgressBarProps) {
  // Clamp progress between 0 and 1
  const clampedProgress = Math.max(0, Math.min(1, progress));

  return (
    <View style={[styles.container, style]}>
      <View
        style={[
          styles.fill,
          {
            width: `${clampedProgress * 100}%`,
            backgroundColor: color,
          },
        ]}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    height: 8,
    width: '100%',
    backgroundColor: '#EAEAEA',
    borderRadius: theme.borderRadius.full,
    overflow: 'hidden',
  },
  fill: {
    height: '100%',
    borderRadius: theme.borderRadius.full,
  },
});
