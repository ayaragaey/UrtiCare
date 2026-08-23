import React from 'react';
import { View, Text, StyleSheet, ViewStyle } from 'react-native';
import { theme } from '../../styles/theme';
import { IconCheck, IconInfo, IconReminder, IconError } from './CustomIcons';

interface AlertProps {
  type: 'success' | 'info' | 'reminder' | 'error';
  message: string;
  style?: ViewStyle;
}

export default function Alert({ type, message, style }: AlertProps) {
  const getAlertStyles = () => {
    switch (type) {
      case 'success':
        return [styles.alertBase, styles.alertSuccess, style];
      case 'info':
        return [styles.alertBase, styles.alertInfo, style];
      case 'reminder':
        return [styles.alertBase, styles.alertReminder, style];
      case 'error':
        return [styles.alertBase, styles.alertError, style];
    }
  };

  const getTextStyle = () => {
    switch (type) {
      case 'success':
        return styles.textSuccess;
      case 'info':
        return styles.textInfo;
      case 'reminder':
        return styles.textReminder;
      case 'error':
        return styles.textError;
    }
  };

  const renderIcon = () => {
    switch (type) {
      case 'success':
        return <IconCheck color={theme.colors.successGreen} style={styles.icon} />;
      case 'info':
        return <IconInfo color={theme.colors.secondaryTeal} style={styles.icon} />;
      case 'reminder':
        return <IconReminder color={theme.colors.primaryPurple} style={styles.icon} />;
      case 'error':
        return <IconError color={theme.colors.alertErrorText} style={styles.icon} />;
    }
  };

  return (
    <View style={getAlertStyles()}>
      {renderIcon()}
      <Text style={[styles.message, getTextStyle()]}>{message}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  alertBase: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 12,
    paddingHorizontal: 16,
    borderRadius: theme.borderRadius.medium,
    borderWidth: 1,
    marginBottom: 12,
    width: '100%',
  },
  alertSuccess: {
    backgroundColor: theme.colors.alertSuccessBg,
    borderColor: 'rgba(80, 151, 41, 0.25)',
  },
  alertInfo: {
    backgroundColor: theme.colors.alertInfoBg,
    borderColor: 'rgba(26, 126, 151, 0.25)',
  },
  alertReminder: {
    backgroundColor: theme.colors.alertReminderBg,
    borderColor: 'rgba(129, 75, 146, 0.25)',
  },
  alertError: {
    backgroundColor: theme.colors.alertErrorBg,
    borderColor: 'rgba(217, 83, 79, 0.25)',
  },
  icon: {
    marginRight: 12,
  },
  message: {
    fontSize: theme.typography.size.regular,
    fontWeight: theme.typography.weight.semibold,
    flex: 1,
  },
  textSuccess: {
    color: theme.colors.successGreen,
  },
  textInfo: {
    color: theme.colors.secondaryTeal,
  },
  textReminder: {
    color: theme.colors.primaryPurple,
  },
  textError: {
    color: theme.colors.alertErrorText,
  },
});
