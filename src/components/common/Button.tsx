import React from 'react';
import { TouchableOpacity, Text, StyleSheet, ViewStyle, TextStyle } from 'react-native';
import { theme } from '../../styles/theme';

interface ButtonProps {
  title: string;
  onPress: () => void;
  variant?: 'primary' | 'secondary' | 'success' | 'disabled' | 'text';
  style?: ViewStyle;
  textStyle?: TextStyle;
}

export default function Button({
  title,
  onPress,
  variant = 'primary',
  style,
  textStyle,
}: ButtonProps) {
  const isWeb = typeof window !== 'undefined';
  const getButtonStyles = () => {
    switch (variant) {
      case 'primary':
        return [
          styles.btnBase,
          styles.btnPrimary,
          isWeb && styles.btnShadow,
          style,
        ];
      case 'secondary':
        return [styles.btnBase, styles.btnSecondary, style];
      case 'success':
        return [
          styles.btnBase,
          styles.btnSuccess,
          isWeb && styles.btnShadow,
          style,
        ];
      case 'disabled':
        return [styles.btnBase, styles.btnDisabled, style];
      case 'text':
        return [styles.btnTextOnly, style];
    }
  };

  const getTextStyle = () => {
    switch (variant) {
      case 'primary':
        return [styles.textBase, styles.textPrimary, textStyle];
      case 'secondary':
        return [styles.textBase, styles.textSecondary, textStyle];
      case 'success':
        return [styles.textBase, styles.textSuccess, textStyle];
      case 'disabled':
        return [styles.textBase, styles.textDisabled, textStyle];
      case 'text':
        return [styles.textBase, styles.textTextOnly, textStyle];
    }
  };

  return (
    <TouchableOpacity
      onPress={variant === 'disabled' ? undefined : onPress}
      activeOpacity={variant === 'disabled' ? 1 : 0.75}
      style={getButtonStyles()}
    >
      <Text style={getTextStyle()}>{title}</Text>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  btnBase: {
    paddingVertical: 12,
    paddingHorizontal: 20,
    borderRadius: theme.borderRadius.medium, // Between 12px and 18px (we set 12px for standard and can style higher)
    alignItems: 'center',
    justifyContent: 'center',
    flexDirection: 'row',
  },
  btnPrimary: {
    backgroundColor: theme.colors.primaryPurple,
  },
  btnSecondary: {
    backgroundColor: theme.colors.white,
    borderWidth: 1.5,
    borderColor: theme.colors.secondaryTeal,
  },
  btnSuccess: {
    backgroundColor: theme.colors.successGreen,
  },
  btnDisabled: {
    backgroundColor: '#EAEAEA',
  },
  btnTextOnly: {
    paddingVertical: 8,
    paddingHorizontal: 12,
    alignItems: 'center',
    justifyContent: 'center',
  },
  btnShadow: {
    ...theme.shadows.subtle,
  },
  textBase: {
    fontSize: theme.typography.size.regular,
    fontWeight: theme.typography.weight.bold,
  },
  textPrimary: {
    color: theme.colors.white,
  },
  textSecondary: {
    color: theme.colors.secondaryTeal,
  },
  textSuccess: {
    color: theme.colors.white,
  },
  textDisabled: {
    color: theme.colors.neutralGrey,
  },
  textTextOnly: {
    color: theme.colors.secondaryTeal,
    fontWeight: theme.typography.weight.semibold,
  },
});
