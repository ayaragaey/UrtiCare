import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Platform } from 'react-native';
import { theme } from '../styles/theme';
import {
  IconHome,
  IconInsights,
  IconTalkUrti,
  IconLibrary,
  IconProfile,
} from './common/CustomIcons';

export type MainTabType = 'home' | 'insights' | 'chat' | 'library' | 'profile';

interface Props {
  activeTab: MainTabType;
  onSelectTab: (tab: MainTabType) => void;
}

export default function BottomNavBar({ activeTab, onSelectTab }: Props) {
  const tabs = [
    { key: 'home' as MainTabType, label: 'Home', icon: IconHome },
    { key: 'insights' as MainTabType, label: 'Logs', icon: IconInsights },
    { key: 'chat' as MainTabType, label: 'Talk to Urti', icon: IconTalkUrti },
    { key: 'library' as MainTabType, label: 'Library', icon: IconLibrary },
    { key: 'profile' as MainTabType, label: 'Profile', icon: IconProfile },
  ];

  return (
    <View style={styles.fixedBottomNavWrapper}>
      {/* Tricolor Top Border Accent Line */}
      {Platform.OS === 'web' && (
        <View style={styles.topGradientBar as any} />
      )}

      <View style={styles.navContainer}>
        {tabs.map(({ key, label, icon: IconComponent }) => {
          const isSelected = activeTab === key;
          return (
            <TouchableOpacity
              key={key}
              style={styles.tabItem}
              onPress={() => onSelectTab(key)}
              activeOpacity={0.8}
            >
              {/* Selected Tab Pill with Subtle Tricolor Gradient Tint */}
              <View style={[styles.pillWrapper, isSelected && styles.pillSelected]}>
                <IconComponent
                  color={isSelected ? '#814B92' : '#509729'}
                  size={20}
                />
              </View>


              <Text
                style={[
                  styles.tabLabel,
                  isSelected ? styles.tabLabelSelected : styles.tabLabelUnselected,
                ]}
                numberOfLines={1}
              >
                {label}
              </Text>
            </TouchableOpacity>
          );
        })}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  fixedBottomNavWrapper: {
    position: Platform.OS === 'web' ? ('fixed' as any) : 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    zIndex: 100,
    backgroundColor: '#FFFFFF',
    borderTopWidth: 1,
    borderTopColor: '#E2E8F0',
    paddingBottom: Platform.OS === 'ios' ? 20 : 8,
    paddingTop: 8,
    paddingHorizontal: 8,
    overflow: 'hidden',
    ...theme.shadows.medium,
  },
  topGradientBar: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    height: 2.5,
    background: 'linear-gradient(90deg, #814B92 0%, #509729 50%, #1A7E97 100%)',
  } as any,
  navContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-around',
    maxWidth: 600,
    alignSelf: 'center',
    width: '100%',
  },
  tabItem: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 2,
  },
  pillWrapper: {
    paddingHorizontal: 14,
    paddingVertical: 4,
    borderRadius: theme.borderRadius.full,
    marginBottom: 3,
    justifyContent: 'center',
    alignItems: 'center',
  },
  pillSelected: {
    backgroundColor: '#F5F3FF',
    borderWidth: 1,
    borderColor: '#E2E8F0',
    ...Platform.select({
      web: {
        background: 'linear-gradient(135deg, rgba(129, 75, 146, 0.15), rgba(80, 151, 41, 0.15), rgba(26, 126, 151, 0.15))',
        borderColor: 'rgba(129, 75, 146, 0.25)',
      } as any
    })
  },
  tabLabel: {
    fontSize: 10,
    textAlign: 'center',
  },
  tabLabelSelected: {
    color: '#814B92',
    fontWeight: theme.typography.weight.bold,
  },
  tabLabelUnselected: {
    color: '#0F172A',
    fontWeight: theme.typography.weight.medium,
  },
});
