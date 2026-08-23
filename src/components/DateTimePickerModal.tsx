import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  Modal,
  Platform,
} from 'react-native';
import { format, subMinutes, subHours, subDays, parseISO } from 'date-fns';

interface Props {
  visible: boolean;
  initialTimestamp?: string;
  onClose: () => void;
  onConfirm: (timestamp: string) => void;
  themeColor?: string;
}

export default function DateTimePickerModal({
  visible,
  initialTimestamp,
  onClose,
  onConfirm,
  themeColor,
}: Props) {
  const primaryColor = themeColor || '#814B92';
  const lightBgColor = 
    primaryColor === '#1A7E97' ? '#E6F4F8' :
    primaryColor === '#F78325' ? '#FFEAD2' :
    primaryColor === '#4C9A2A' ? '#E8F7EC' :
    '#F6EEFA';

  const [selectedDate, setSelectedDate] = useState<Date>(new Date());
  const [selectedPreset, setSelectedPreset] = useState<string | null>('Now');

  // Manual picker states
  const [pickerMode, setPickerMode] = useState<'date' | 'time' | null>(null);

  useEffect(() => {
    if (visible) {
      if (initialTimestamp) {
        try {
          setSelectedDate(parseISO(initialTimestamp));
        } catch {
          setSelectedDate(new Date());
        }
      } else {
        setSelectedDate(new Date());
      }
      setSelectedPreset('Now');
      setPickerMode(null);
    }
  }, [visible, initialTimestamp]);

  const presets = [
    { label: 'Now', getVal: () => new Date() },
    { label: '15m ago', getVal: () => subMinutes(new Date(), 15) },
    { label: '30m ago', getVal: () => subMinutes(new Date(), 30) },
    { label: '1h ago', getVal: () => subHours(new Date(), 1) },
    { label: '2h ago', getVal: () => subHours(new Date(), 2) },
    { label: 'Yesterday', getVal: () => subDays(new Date(), 1) },
  ];

  const handleSelectPreset = (label: string, getVal: () => Date) => {
    setSelectedPreset(label);
    setSelectedDate(getVal());
  };

  const handleDateStep = (days: number) => {
    setSelectedPreset(null);
    const newD = new Date(selectedDate);
    newD.setDate(newD.getDate() + days);
    setSelectedDate(newD);
  };

  const handleTimeStep = (minutes: number) => {
    setSelectedPreset(null);
    const newD = new Date(selectedDate);
    newD.setMinutes(newD.getMinutes() + minutes);
    setSelectedDate(newD);
  };

  const formattedDate = format(selectedDate, 'EEE, MMM dd, yyyy');
  const formattedTime = format(selectedDate, 'hh:mm a');

  return (
    <Modal
      visible={visible}
      transparent
      animationType="fade"
      onRequestClose={onClose}
    >
      <View style={styles.overlay}>
        <View style={styles.modalCard}>
          {/* Header */}
          <View style={styles.headerRow}>
            <Text style={styles.headerIcon}>📅</Text>
            <Text style={styles.headerTitle}>Select Date & Time</Text>
          </View>

          {/* Smart Presets Section */}
          <View style={styles.sectionContainer}>
            <Text style={styles.sectionLabel}>Smart Presets</Text>
            <View style={styles.presetsGrid}>
              {presets.map((preset) => {
                const isSelected = selectedPreset === preset.label;
                return (
                  <TouchableOpacity
                    key={preset.label}
                    style={[
                      styles.presetButton,
                      isSelected ? {
                        backgroundColor: lightBgColor,
                        borderColor: primaryColor,
                        borderWidth: 1.5,
                      } : null,
                    ]}
                    onPress={() => handleSelectPreset(preset.label, preset.getVal)}
                    activeOpacity={0.7}
                  >
                    <Text
                      style={[
                        styles.presetButtonText,
                        { color: primaryColor },
                        isSelected && styles.presetButtonTextSelected,
                      ]}
                    >
                      {preset.label}
                    </Text>
                  </TouchableOpacity>
                );
              })}
            </View>
          </View>

          {/* Divider */}
          <View style={styles.divider} />

          {/* Date & Time Fields */}
          <View style={styles.fieldsRow}>
            {/* Date Field */}
            <View style={styles.fieldColumn}>
              <Text style={styles.fieldLabel}>Date</Text>
              <TouchableOpacity
                style={styles.fieldBox}
                onPress={() => setPickerMode(pickerMode === 'date' ? null : 'date')}
                activeOpacity={0.8}
              >
                <Text style={styles.fieldValueText}>{formattedDate}</Text>
              </TouchableOpacity>
            </View>

            {/* Time Field */}
            <View style={styles.fieldColumn}>
              <Text style={styles.fieldLabel}>Time</Text>
              <TouchableOpacity
                style={styles.fieldBox}
                onPress={() => setPickerMode(pickerMode === 'time' ? null : 'time')}
                activeOpacity={0.8}
              >
                <Text style={styles.fieldValueText}>{formattedTime}</Text>
              </TouchableOpacity>
            </View>
          </View>

          {/* Sub picker controls when user taps Date or Time */}
          {pickerMode === 'date' && (
            <View style={styles.subPickerContainer}>
              <Text style={styles.subPickerHint}>Adjust Date:</Text>
              <View style={styles.stepperRow}>
                <TouchableOpacity
                  style={styles.stepBtn}
                  onPress={() => handleDateStep(-1)}
                >
                  <Text style={styles.stepBtnText}>- 1 Day</Text>
                </TouchableOpacity>
                <TouchableOpacity
                  style={styles.stepBtn}
                  onPress={() => handleDateStep(1)}
                >
                  <Text style={styles.stepBtnText}>+ 1 Day</Text>
                </TouchableOpacity>
              </View>
            </View>
          )}

          {pickerMode === 'time' && (
            <View style={styles.subPickerContainer}>
              <Text style={styles.subPickerHint}>Adjust Time:</Text>
              <View style={styles.stepperRow}>
                <TouchableOpacity
                  style={styles.stepBtn}
                  onPress={() => handleTimeStep(-15)}
                >
                  <Text style={styles.stepBtnText}>- 15m</Text>
                </TouchableOpacity>
                <TouchableOpacity
                  style={styles.stepBtn}
                  onPress={() => handleTimeStep(-60)}
                >
                  <Text style={styles.stepBtnText}>- 1h</Text>
                </TouchableOpacity>
                <TouchableOpacity
                  style={styles.stepBtn}
                  onPress={() => handleTimeStep(15)}
                >
                  <Text style={styles.stepBtnText}>+ 15m</Text>
                </TouchableOpacity>
                <TouchableOpacity
                  style={styles.stepBtn}
                  onPress={() => handleTimeStep(60)}
                >
                  <Text style={styles.stepBtnText}>+ 1h</Text>
                </TouchableOpacity>
              </View>
            </View>
          )}

          {/* Bottom Actions */}
          <View style={styles.actionsRow}>
            <TouchableOpacity onPress={onClose} style={styles.actionBtn}>
              <Text style={styles.cancelText}>Cancel</Text>
            </TouchableOpacity>
            <TouchableOpacity
              onPress={() => onConfirm(selectedDate.toISOString())}
              style={styles.actionBtn}
            >
              <Text style={[styles.saveText, { color: primaryColor }]}>Save</Text>
            </TouchableOpacity>
          </View>
        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  overlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.45)',
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
  modalCard: {
    width: '100%',
    maxWidth: 380,
    backgroundColor: '#FFFFFF',
    borderRadius: 20,
    padding: 20,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.1,
    shadowRadius: 10,
    elevation: 6,
  },
  headerRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 14,
  },
  headerIcon: {
    fontSize: 18,
    marginRight: 8,
  },
  headerTitle: {
    fontSize: 17,
    fontWeight: '700',
    color: '#1F2937',
  },
  sectionContainer: {
    marginBottom: 14,
  },
  sectionLabel: {
    fontSize: 12,
    fontWeight: '600',
    color: '#64748B',
    marginBottom: 8,
  },
  presetsGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'space-between',
    gap: 8,
  },
  presetButton: {
    width: '31%',
    height: 34,
    backgroundColor: '#FFFFFF',
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#E2E8F0',
    justifyContent: 'center',
    alignItems: 'center',
  },
  presetButtonSelected: {
    backgroundColor: '#F6EEFA',
    borderColor: '#814B92',
    borderWidth: 1.5,
  },
  presetButtonText: {
    fontSize: 11.5,
    color: '#814B92',
    fontWeight: '500',
  },
  presetButtonTextSelected: {
    fontWeight: '700',
  },
  divider: {
    height: 1,
    backgroundColor: '#E2E8F0',
    marginBottom: 14,
  },
  fieldsRow: {
    flexDirection: 'row',
    gap: 10,
    marginBottom: 8,
  },
  fieldColumn: {
    flex: 1,
  },
  fieldLabel: {
    fontSize: 11.5,
    fontWeight: '600',
    color: '#64748B',
    marginBottom: 4,
  },
  fieldBox: {
    height: 44,
    backgroundColor: '#F8FAFC',
    borderRadius: 10,
    borderWidth: 1,
    borderColor: '#E2E8F0',
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: 8,
  },
  fieldValueText: {
    fontSize: 12,
    fontWeight: '700',
    color: '#1F2937',
  },
  subPickerContainer: {
    backgroundColor: '#F1F5F9',
    borderRadius: 8,
    padding: 8,
    marginVertical: 6,
  },
  subPickerHint: {
    fontSize: 11,
    color: '#64748B',
    fontWeight: '600',
    marginBottom: 6,
  },
  stepperRow: {
    flexDirection: 'row',
    gap: 8,
    justifyContent: 'center',
  },
  stepBtn: {
    backgroundColor: '#FFFFFF',
    borderRadius: 6,
    borderWidth: 1,
    borderColor: '#CBD5E1',
    paddingVertical: 5,
    paddingHorizontal: 10,
  },
  stepBtnText: {
    fontSize: 11.5,
    color: '#1E293B',
    fontWeight: '600',
  },
  actionsRow: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
    alignItems: 'center',
    marginTop: 10,
    gap: 8,
  },
  actionBtn: {
    paddingVertical: 6,
    paddingHorizontal: 12,
  },
  cancelText: {
    fontSize: 14,
    color: '#64748B',
    fontWeight: '600',
  },
  saveText: {
    fontSize: 14,
    color: '#814B92',
    fontWeight: '700',
  },
});
