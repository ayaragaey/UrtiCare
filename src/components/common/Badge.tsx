import React from 'react';
import { View, Text, StyleSheet, ViewStyle, Platform } from 'react-native';
import { theme } from '../../styles/theme';

export type BadgeType = 'completed' | 'inProgress' | 'premium' | 'inactive' | 'antihistamine' | 'flare' | 'cortisone' | 'consumption';

interface BadgeProps {
  label: string;
  type?: BadgeType;
  style?: ViewStyle;
}

export default function Badge({ label, type = 'inactive', style }: BadgeProps) {
  const getBadgeStyle = () => {
    switch (type) {
      case 'antihistamine':
        return [styles.badgeBase, styles.badgeAntihistamine, style];
      case 'flare':
        return [styles.badgeBase, styles.badgeFlare, style];
      case 'cortisone':
        return [styles.badgeBase, styles.badgeCortisone, style];
      case 'consumption':
        return [styles.badgeBase, styles.badgeConsumption, style];
      case 'completed':
        return [styles.badgeBase, styles.badgeCompleted, style];
      case 'inProgress':
        return [styles.badgeBase, styles.badgeInProgress, style];
      case 'premium':
        return [styles.badgeBase, styles.badgePremium, style];
      default:
        return [styles.badgeBase, styles.badgeInactive, style];
    }
  };

  const getTextStyle = () => {
    switch (type) {
      case 'antihistamine':
        return styles.textAntihistamine;
      case 'flare':
        return styles.textFlare;
      case 'cortisone':
        return styles.textCortisone;
      case 'consumption':
        return styles.textConsumption;
      case 'completed':
        return styles.textCompleted;
      case 'inProgress':
        return styles.textInProgress;
      case 'premium':
        return styles.textPremium;
      default:
        return styles.textInactive;
    }
  };

  return (
    <View style={getBadgeStyle() as any}>
      <Text style={[styles.textBase, getTextStyle()]}>{label}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  badgeBase: {
    paddingVertical: 4,
    paddingHorizontal: 12,
    borderRadius: theme.borderRadius.full,
    alignSelf: 'flex-start',
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#E2E8F0',
  },

  /* Pills Log - Green Shades Gradient */
  badgeAntihistamine: {
    backgroundColor: '#ECFDF5',
    borderColor: '#A7F3D0',
    ...Platform.select({
      web: {
        background: 'linear-gradient(135deg, rgba(80, 151, 41, 0.15), rgba(52, 211, 153, 0.15))',
      } as any
    })
  },
  textAntihistamine: {
    color: '#38761D',
  },

  /* Flare Up - Reddish Shades Gradient */
  badgeFlare: {
    backgroundColor: '#FDF2F4',
    borderColor: '#FECDD3',
    ...Platform.select({
      web: {
        background: 'linear-gradient(135deg, rgba(252, 154, 163, 0.2), rgba(239, 68, 68, 0.15))',
      } as any
    })
  },
  textFlare: {
    color: '#E11D48',
  },

  /* Bio. Treatment - Teal Shades Gradient */
  badgeCortisone: {
    backgroundColor: '#F0FDFA',
    borderColor: '#99F6E4',
    ...Platform.select({
      web: {
        background: 'linear-gradient(135deg, rgba(26, 126, 151, 0.15), rgba(14, 165, 233, 0.15))',
      } as any
    })
  },
  textCortisone: {
    color: '#1A7E97',
  },

  badgeCompleted: {
    backgroundColor: '#ECFDF5',
    borderColor: '#34D399',
  },
  badgeInProgress: {
    backgroundColor: '#F0FDFA',
    borderColor: '#0EA5E9',
  },
  badgePremium: {
    backgroundColor: '#F5F3FF',
    borderColor: '#814B92',
  },
  badgeInactive: {
    backgroundColor: '#F8FAFC',
    borderColor: '#E2E8F0',
  },
  textBase: {
    fontSize: 11,
    fontWeight: theme.typography.weight.heavy,
    letterSpacing: 0.3,
  },
  textCompleted: {
    color: '#509729',
  },
  textInProgress: {
    color: '#1A7E97',
  },
  textPremium: {
    color: '#814B92',
  },
  textInactive: {
    color: '#64748B',
  },
  badgeConsumption: {
    backgroundColor: '#FFF7ED',
    borderColor: '#FED7AA',
    ...Platform.select({
      web: {
        background: 'linear-gradient(135deg, rgba(247, 131, 37, 0.15), rgba(251, 146, 60, 0.15))',
      } as any
    })
  },
  textConsumption: {
    color: '#C2410C',
  },
});
