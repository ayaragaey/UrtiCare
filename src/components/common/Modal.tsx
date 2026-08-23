import React from 'react';
import { View, Text, StyleSheet, Modal as RNModal, TouchableOpacity, Platform, ViewStyle } from 'react-native';
import { theme } from '../../styles/theme';
import { IconClose } from './CustomIcons';

interface ModalProps {
  visible: boolean;
  onClose: () => void;
  title: string;
  children: React.ReactNode;
  footerActions?: React.ReactNode;
  style?: ViewStyle;
}

export default function Modal({ visible, onClose, title, children, footerActions, style }: ModalProps) {
  const isWeb = typeof window !== 'undefined';
  return (
    <RNModal
      visible={visible}
      transparent={true}
      animationType="fade"
      onRequestClose={onClose}
    >
      <View style={styles.backdrop}>
        <View style={[styles.modalCard, isWeb && styles.modalGlass, style]}>
          {/* Header */}
          <View style={styles.header}>
            <Text style={styles.title}>{title}</Text>
            <TouchableOpacity onPress={onClose} style={styles.closeBtn} activeOpacity={0.7}>
              <IconClose color={theme.colors.textMuted} size={16} />
            </TouchableOpacity>
          </View>

          {/* Content */}
          <View style={styles.content}>{children}</View>

          {/* Footer actions */}
          {footerActions && <View style={styles.footer}>{footerActions}</View>}
        </View>
      </View>
    </RNModal>
  );
}

const styles = StyleSheet.create({
  backdrop: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.55)',
    justifyContent: 'center',
    alignItems: 'center',
    padding: 24,
  },
  modalCard: {
    width: '100%',
    maxWidth: 400,
    backgroundColor: Platform.OS === 'web' ? theme.colors.glassBg : 'rgba(30, 30, 30, 0.85)',
    borderRadius: theme.borderRadius.xlarge, // 18px
    borderWidth: 1,
    borderColor: theme.colors.glassBorder,
    padding: 24,
    ...theme.shadows.strong,
  },
  modalGlass: {
    backdropFilter: 'blur(20px)',
    boxShadow: '0 12px 40px rgba(0, 0, 0, 0.3)',
  } as any,
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
  },
  title: {
    fontSize: theme.typography.size.medium,
    fontWeight: theme.typography.weight.bold,
    color: theme.colors.textLight,
  },
  closeBtn: {
    padding: 4,
  },
  content: {
    marginBottom: 20,
  },
  footer: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
    gap: 12,
  },
});
