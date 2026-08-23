import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  Modal as RNModal,
  TouchableOpacity,
  Image,
  Platform,
  ScrollView,
} from 'react-native';
import { theme } from '../styles/theme';
import { IconClose, IconProfile, IconInsights, IconLightning } from './common/CustomIcons';

interface OnboardingModalProps {
  visible: boolean;
  onClose: () => void;
  onComplete: () => void;
}

export default function OnboardingModal({ visible, onClose, onComplete }: OnboardingModalProps) {
  if (!visible) return null;

  const content = (
    <View style={styles.backdrop}>
      <TouchableOpacity
        style={styles.backdropTouchArea}
        activeOpacity={1}
        onPress={onClose}
      />
      <View style={styles.modalCard}>
        {/* Close button X top-right */}
        <TouchableOpacity onPress={onClose} style={styles.closeBtn} activeOpacity={0.7}>
          <IconClose color="#94A3B8" size={16} />
        </TouchableOpacity>

        <ScrollView
          showsVerticalScrollIndicator={false}
          contentContainerStyle={styles.scrollContent}
        >
          {/* Friendly healthcare illustration */}
          <Image
            source={require('../assets/onboarding_illustration.jpg')}
            style={styles.illustration}
          />

          {/* Title Section */}
          <Text style={styles.title}>
            Complete <Text style={styles.titleGreen}>your profile</Text>
          </Text>

          {/* Subtitle Section */}
          <Text style={styles.subtitle}>
            Set up your profile to get the most out of UrtiCare and make tracking your health journey easier.
          </Text>

          {/* Benefits Cards */}
          <View style={styles.benefitsContainer}>
            {/* Benefit 1 */}
            <View style={styles.benefitCard}>
              <View style={styles.iconCircle}>
                <IconProfile color={theme.colors.primaryPurple} size={18} />
              </View>
              <View style={styles.benefitTextContainer}>
                <Text style={styles.benefitTitle}>Personalized health tracking</Text>
                <Text style={styles.benefitDesc}>
                  Track your symptoms, medications, and condition based on your information.
                </Text>
              </View>
            </View>

            {/* Benefit 2 */}
            <View style={styles.benefitCard}>
              <View style={styles.iconCircle}>
                <IconInsights color={theme.colors.primaryPurple} size={18} />
              </View>
              <View style={styles.benefitTextContainer}>
                <Text style={styles.benefitTitle}>Better pattern insights</Text>
                <Text style={styles.benefitDesc}>
                  Understand your symptoms and triggers through your logged health patterns.
                </Text>
              </View>
            </View>

            {/* Benefit 3 */}
            <View style={styles.benefitCard}>
              <View style={styles.iconCircle}>
                <IconLightning color={theme.colors.primaryPurple} size={18} />
              </View>
              <View style={styles.benefitTextContainer}>
                <Text style={styles.benefitTitle}>Quick logging experience</Text>
                <Text style={styles.benefitDesc}>
                  Easily record medications, flare-ups, and daily updates faster.
                </Text>
              </View>
            </View>
          </View>

          {/* Buttons */}
          <View style={styles.buttonContainer}>
            <TouchableOpacity
              style={styles.primaryBtn}
              onPress={onComplete}
              activeOpacity={0.85}
            >
              <Text style={styles.primaryBtnText}>Complete your profile</Text>
            </TouchableOpacity>

            <TouchableOpacity
              style={styles.secondaryBtn}
              onPress={onClose}
              activeOpacity={0.85}
            >
              <Text style={styles.secondaryBtnText}>Later</Text>
            </TouchableOpacity>
          </View>
        </ScrollView>
      </View>
    </View>
  );

  if (Platform.OS === 'web') {
    return content;
  }

  return (
    <RNModal
      visible={visible}
      transparent={true}
      animationType="fade"
      onRequestClose={onClose}
    >
      {content}
    </RNModal>
  );
}

const styles = StyleSheet.create({
  backdrop: {
    ...Platform.select({
      web: {
        position: 'fixed' as any,
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        zIndex: 999999,
      },
      default: {
        flex: 1,
      },
    }),
    backgroundColor: 'rgba(15, 23, 42, 0.65)', // Dimmed background
    justifyContent: 'center',
    alignItems: 'center',
    padding: 16,
  },
  backdropTouchArea: {
    ...StyleSheet.absoluteFillObject,
  },
  modalCard: {
    width: '100%',
    maxWidth: 420,
    maxHeight: '90%',
    backgroundColor: '#FFFFFF',
    borderRadius: theme.borderRadius.xlarge,
    padding: 24,
    borderWidth: 1,
    borderColor: '#E2E8F0',
    position: 'relative',
    zIndex: 1000000,
    ...Platform.select({
      web: {
        boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.2), 0 10px 10px -5px rgba(0, 0, 0, 0.1)',
      } as any,
      default: {
        ...theme.shadows.strong,
      }
    }),
  },
  closeBtn: {
    position: 'absolute',
    top: 16,
    right: 16,
    zIndex: 10,
    width: 28,
    height: 28,
    borderRadius: 14,
    backgroundColor: '#F1F5F9',
    justifyContent: 'center',
    alignItems: 'center',
  },
  scrollContent: {
    alignItems: 'center',
    paddingTop: 8,
  },
  illustration: {
    width: 180,
    height: 180,
    resizeMode: 'contain',
    marginBottom: 16,
    borderRadius: theme.borderRadius.large,
  },
  title: {
    fontSize: 22,
    fontWeight: theme.typography.weight.heavy,
    color: theme.colors.textDark,
    textAlign: 'center',
    marginBottom: 8,
  },
  titleGreen: {
    color: '#388E3C', // Friendly medical success green
  },
  subtitle: {
    fontSize: 13,
    color: theme.colors.textMuted,
    textAlign: 'center',
    lineHeight: 18,
    paddingHorizontal: 8,
    marginBottom: 20,
  },
  benefitsContainer: {
    width: '100%',
    marginBottom: 24,
  },
  benefitCard: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#F4FBF7', // Clean soft green tint
    borderColor: '#E8F5E9',
    borderWidth: 1,
    borderRadius: theme.borderRadius.medium,
    padding: 12,
    marginBottom: 10,
  },
  iconCircle: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: '#E8F5E9',
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 12,
  },
  benefitTextContainer: {
    flex: 1,
  },
  benefitTitle: {
    fontSize: 13.5,
    fontWeight: theme.typography.weight.bold,
    color: theme.colors.textDark,
  },
  benefitDesc: {
    fontSize: 11.5,
    color: theme.colors.textMuted,
    lineHeight: 15,
    marginTop: 2,
  },
  buttonContainer: {
    width: '100%',
    gap: 8,
  },
  primaryBtn: {
    backgroundColor: '#388E3C', // Green brand color
    paddingVertical: 14,
    borderRadius: theme.borderRadius.medium,
    alignItems: 'center',
    width: '100%',
    ...Platform.select({
      web: {
        boxShadow: '0 4px 6px -1px rgba(56, 142, 60, 0.2)',
      } as any,
    }),
  },
  primaryBtnText: {
    color: '#FFFFFF',
    fontWeight: theme.typography.weight.bold,
    fontSize: 14.5,
  },
  secondaryBtn: {
    backgroundColor: '#FFFFFF',
    borderWidth: 1.5,
    borderColor: '#388E3C',
    paddingVertical: 14,
    borderRadius: theme.borderRadius.medium,
    alignItems: 'center',
    width: '100%',
  },
  secondaryBtnText: {
    color: '#388E3C',
    fontWeight: theme.typography.weight.bold,
    fontSize: 14.5,
  },
});

