import React, { useState, useEffect } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, TextInput, Platform } from 'react-native';
import { parseISO, format } from 'date-fns';
import { useTrackerStore } from '../store/useTrackerStore';
import { theme } from '../styles/theme';
import Modal from './common/Modal';
import Button from './common/Button';
import { IconCapsule, IconTablet, IconDroplet } from './common/CustomIcons';

const getFriendlyRelativeDateTime = (timestampStr: string) => {
  try {
    const d = parseISO(timestampStr);
    const now = new Date();
    
    const hour = d.getHours();
    const minute = d.getMinutes();
    const ampm = hour >= 12 ? 'PM' : 'AM';
    const displayHour = String(hour % 12 === 0 ? 12 : hour % 12).padStart(2, '0');
    const displayMinute = String(minute).padStart(2, '0');
    const timeStr = `${displayHour}:${displayMinute} ${ampm}`;
    
    const dStr = format(d, 'yyyy-MM-dd');
    const nowStr = format(now, 'yyyy-MM-dd');
    
    const yesterday = new Date();
    yesterday.setDate(now.getDate() - 1);
    const yestStr = format(yesterday, 'yyyy-MM-dd');
    
    let relativeDate = '';
    if (dStr === nowStr) {
      relativeDate = 'Today';
    } else if (dStr === yestStr) {
      relativeDate = 'Yesterday';
    } else {
      relativeDate = format(d, 'MMM dd, yyyy');
    }
    
    return `${relativeDate} • ${timeStr}`;
  } catch {
    return '';
  }
};

interface Props {
  visible: boolean;
  onClose: () => void;
  editEntryId?: string | null;
  initialType?: 'ANTIHISTAMINE' | 'CORTISONE' | 'FLARE_UP';
  initialNotes?: string;
  initialTimestamp?: string | null;
}

export default function MedicationEntryModal({
  visible,
  onClose,
  editEntryId,
  initialType = 'ANTIHISTAMINE',
  initialTimestamp,
}: Props) {
  const [selectedType, setSelectedType] = useState<'ANTIHISTAMINE' | 'CORTISONE' | 'FLARE_UP'>(initialType);
  const [medName, setMedName] = useState('Telefast (180 mg)');
  const [dosageNote, setDosageNote] = useState('+1d');
  const [entryTimestamp, setEntryTimestamp] = useState<string>(new Date().toISOString());
  
  const entries = useTrackerStore(state => state.entries);
  const addEntry = useTrackerStore(state => state.addEntry);
  const updateEntry = useTrackerStore(state => state.updateEntry);
  const setCollisionWarning = useTrackerStore(state => state.setCollisionWarning);
  const defaultFlareUpSymptom = useTrackerStore(state => state.defaultFlareUpSymptom);
  const defaultFlareUpSeverity = useTrackerStore(state => state.defaultFlareUpSeverity);

  useEffect(() => {
    if (visible && editEntryId) {
      const entry = entries.find(e => e.id === editEntryId);
      if (entry) {
        setSelectedType(entry.type as 'ANTIHISTAMINE' | 'CORTISONE' | 'FLARE_UP');
        setMedName(entry.itemName || '');
        setDosageNote(entry.notes || '');
        setEntryTimestamp(entry.timestamp);
      }
    } else if (visible && !editEntryId) {
      setSelectedType(initialType);
      setEntryTimestamp(initialTimestamp || new Date().toISOString());
      if (initialType === 'ANTIHISTAMINE') {
        setMedName('Telefast (180 mg)');
        setDosageNote('+1d');
      } else if (initialType === 'CORTISONE') {
        setMedName('Prednisolone (5 mg)');
        setDosageNote('Course');
      } else {
        setMedName(defaultFlareUpSymptom);
        setDosageNote(defaultFlareUpSeverity);
      }
    }
  }, [visible, editEntryId, entries, initialType, initialTimestamp, defaultFlareUpSymptom, defaultFlareUpSeverity]);

  const handleSubmit = () => {
    const timestamp = entryTimestamp || new Date().toISOString();
    
    if (editEntryId) {
      const res = updateEntry(editEntryId, timestamp, false, medName, dosageNote);
      if (res.collision) {
        setCollisionWarning({
          type: selectedType,
          timestamp,
          isEdit: true,
          entryId: editEntryId,
        });
      }
    } else {
      const res = addEntry(selectedType, timestamp, false, medName, dosageNote);
      if (res.collision) {
        setCollisionWarning({
          type: selectedType,
          timestamp,
          isEdit: false,
        });
      }
    }
    onClose();
  };

  const presetMeds = [
    { name: 'Telefast (180 mg)', note: '+1d', type: 'ANTIHISTAMINE' as const },
    { name: 'Bilastine (20 mg)', note: 'Daily', type: 'ANTIHISTAMINE' as const },
    { name: 'Cetirizine (10 mg)', note: 'Single dose', type: 'ANTIHISTAMINE' as const },
    { name: 'Prednisolone (5 mg)', note: 'Short course', type: 'CORTISONE' as const },
  ];

  return (
    <Modal
      visible={visible}
      onClose={onClose}
      title={editEntryId ? "Edit Medication Entry" : "Log Medication Intake"}
      footerActions={
        <>
          <Button title="Cancel" onPress={onClose} variant="secondary" style={styles.btnHalf} />
          <Button
            title={editEntryId ? "Save Changes" : "Log Intake"}
            onPress={handleSubmit}
            variant="primary"
            style={{
              ...styles.btnHalf,
              backgroundColor:
                selectedType === 'CORTISONE'
                  ? '#1A7E97'
                  : selectedType === 'FLARE_UP'
                  ? '#814B92'
                  : '#4C9A2A',
            }}
          />
        </>
      }
    >
      <View style={styles.body}>
        <View style={styles.timeBadgeContainer}>
          <Text style={styles.timeBadgeLabel}>Date & Time:</Text>
          <Text style={styles.timeBadgeValue}>
            {getFriendlyRelativeDateTime(entryTimestamp || new Date().toISOString())}
          </Text>
        </View>

        <Text style={styles.sectionLabel}>Select Medication / Intake Type</Text>
        <View style={styles.typeSelectorRow}>
          <TouchableOpacity
            style={[styles.typeOption, selectedType === 'ANTIHISTAMINE' && styles.typeOptionActiveMint]}
            onPress={() => { setSelectedType('ANTIHISTAMINE'); setMedName('Telefast (180 mg)'); setDosageNote('+1d'); }}
          >
            <IconCapsule color={selectedType === 'ANTIHISTAMINE' ? '#059669' : '#64748B'} size={18} />
            <Text style={[styles.typeOptionText, selectedType === 'ANTIHISTAMINE' && styles.typeOptionTextActive]}>Antihistamine</Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={[styles.typeOption, selectedType === 'CORTISONE' && styles.typeOptionActiveTeal]}
            onPress={() => { setSelectedType('CORTISONE'); setMedName('Prednisolone (5 mg)'); setDosageNote('Course'); }}
          >
            <IconTablet color={selectedType === 'CORTISONE' ? '#1A7E97' : '#64748B'} size={18} />
            <Text style={[styles.typeOptionText, selectedType === 'CORTISONE' && [styles.typeOptionTextActive, { color: '#1A7E97' }]]}>Corticosteroid</Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={[styles.typeOption, selectedType === 'FLARE_UP' && styles.typeOptionActivePink]}
            onPress={() => { setSelectedType('FLARE_UP'); setMedName(defaultFlareUpSymptom); setDosageNote(defaultFlareUpSeverity); }}
          >
            <IconDroplet color={selectedType === 'FLARE_UP' ? '#814B92' : '#64748B'} size={18} />
            <Text style={[styles.typeOptionText, selectedType === 'FLARE_UP' && [styles.typeOptionTextActive, { color: '#814B92' }]]}>Flare Up</Text>
          </TouchableOpacity>
        </View>

        <Text style={styles.sectionLabel}>Quick Presets</Text>
        <View style={styles.presetRow}>
          {presetMeds.map((item, idx) => (
            <TouchableOpacity
              key={idx}
              style={styles.presetChip}
              onPress={() => {
                setSelectedType(item.type);
                setMedName(item.name);
                setDosageNote(item.note);
              }}
            >
              <Text style={styles.presetChipText}>{item.name}</Text>
            </TouchableOpacity>
          ))}
        </View>

        <Text style={styles.sectionLabel}>Medication Name & Dosage</Text>
        <TextInput
          style={styles.input}
          value={medName}
          onChangeText={setMedName}
          placeholder="e.g. Telefast (180 mg)"
          placeholderTextColor="#94A3B8"
        />

        <Text style={styles.sectionLabel}>Dose Note / Interval</Text>
        <TextInput
          style={styles.input}
          value={dosageNote}
          onChangeText={setDosageNote}
          placeholder="e.g. (+1d)"
          placeholderTextColor="#94A3B8"
        />
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  body: {
    paddingVertical: 8,
  },
  sectionLabel: {
    fontSize: theme.typography.size.small,
    fontWeight: theme.typography.weight.semibold,
    color: theme.colors.textDark,
    marginBottom: 8,
    marginTop: 12,
  },
  typeSelectorRow: {
    flexDirection: 'row',
    gap: 8,
  },
  typeOption: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 10,
    borderRadius: theme.borderRadius.medium,
    borderWidth: 1.5,
    borderColor: '#E2E8F0',
    backgroundColor: '#F8FAFC',
    gap: 6,
  },
  typeOptionActiveMint: {
    borderColor: '#34D399',
    backgroundColor: '#ECFDF5',
  },
  typeOptionActiveTeal: {
    borderColor: '#1A7E97',
    backgroundColor: '#E6F4F8',
  },
  typeOptionActivePink: {
    borderColor: '#814B92',
    backgroundColor: '#F3E8FF',
  },
  typeOptionText: {
    fontSize: 11,
    fontWeight: theme.typography.weight.semibold,
    color: theme.colors.textMuted,
  },
  typeOptionTextActive: {
    color: theme.colors.textDark,
    fontWeight: theme.typography.weight.bold,
  },
  presetRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
  presetChip: {
    paddingHorizontal: 12,
    paddingVertical: 6,
    backgroundColor: '#F1F5F9',
    borderRadius: theme.borderRadius.full,
    borderWidth: 1,
    borderColor: '#E2E8F0',
  },
  presetChipText: {
    fontSize: 11,
    color: theme.colors.textDark,
    fontWeight: theme.typography.weight.medium,
  },
  input: {
    backgroundColor: '#F8FAFC',
    borderWidth: 1.5,
    borderColor: '#E2E8F0',
    borderRadius: theme.borderRadius.medium,
    paddingHorizontal: 14,
    paddingVertical: 10,
    fontSize: 14,
    color: theme.colors.textDark,
  },
  btnHalf: {
    flex: 1,
  },
  timeBadgeContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#F1F5F9',
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: theme.borderRadius.medium,
    marginBottom: 6,
    gap: 6,
  },
  timeBadgeLabel: {
    fontSize: 12,
    fontWeight: theme.typography.weight.semibold,
    color: '#64748B',
  },
  timeBadgeValue: {
    fontSize: 12,
    fontWeight: theme.typography.weight.bold,
    color: '#0F172A',
  },
});
