import React, { useState, useMemo, useEffect } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, TextInput, Platform, ScrollView, Alert } from 'react-native';
import { useTrackerStore } from '../store/useTrackerStore';
import { LogEntry, EntryType } from '../types/tracker.types';
import { getTimeGap, MONTH_NAMES } from '../utils/dateHelpers';
import { parseISO, addMinutes, subMinutes, addHours, subHours, addDays, subDays, startOfDay, endOfDay, isWithinInterval, format } from 'date-fns';
import { theme } from '../styles/theme';
import Badge from './common/Badge';
import Button from './common/Button';
import Modal from './common/Modal';
import MedicationEntryModal from './MedicationEntryModal';
import DateTimePickerModal from './DateTimePickerModal';
import { IconEdit, IconDelete, IconBackArrow, IconPlus, IconCheck, IconChevronRight, IconClose, IconCapsule, IconBiological, IconSyringeOutline, IconPlusInCircle, IconDownloadArrow, IconTrashCan } from './common/CustomIcons';
import { exportLogsToCSV } from '../utils/csvExporter';

// Simple custom export icon drawn with React Native views
const IconExport = ({ color = '#64748B', size = 15 }) => (
  <View style={{ width: size, height: size, justifyContent: 'center', alignItems: 'center' }}>
    <View style={{ width: size * 0.75, height: size * 0.75, borderWidth: 1.5, borderColor: color, borderRadius: 2, justifyContent: 'center', alignItems: 'center', position: 'relative' }}>
      <View style={{ width: size * 0.45, height: 1.5, backgroundColor: color, transform: [{ rotate: '-45deg' }], position: 'absolute', top: size * 0.22, right: size * 0.08 }} />
      <View style={{ width: size * 0.2, height: size * 0.2, borderTopWidth: 1.5, borderRightWidth: 1.5, borderColor: color, position: 'absolute', top: size * 0.05, right: size * 0.05 }} />
    </View>
  </View>
);

interface Props {
  onBack?: () => void;
  onOpenAddConsumption?: (timestamp?: string) => void;
}

export default function HistoryLogList({ onBack, onOpenAddConsumption }: Props) {
  const entries = useTrackerStore(state => state.entries);
  const deleteEntry = useTrackerStore(state => state.deleteEntry);
  const updateEntry = useTrackerStore(state => state.updateEntry);
  const setCollisionWarning = useTrackerStore(state => state.setCollisionWarning);
  const toggleConsumptionTriggerStatus = useTrackerStore(state => state.toggleConsumptionTriggerStatus);
  const favoriteAntihistamine = useTrackerStore(state => state.favoriteAntihistamine);

  const hasBeenTriggerBefore = (itemName: string, currentId: string) => {
    return entries.some(e => 
      e.type === 'CONSUMPTION' && 
      e.id !== currentId && 
      e.itemName?.toLowerCase() === itemName?.toLowerCase() && 
      e.status === 'Trigger'
    );
  };
  
  // Tabs: MEDICATION, FLARE_UP, CONSUMPTION
  type CategoryTab = 'MEDICATION' | 'FLARE_UP' | 'CONSUMPTION';
  const [activeCategory, setActiveCategory] = useState<CategoryTab>('MEDICATION');
  
  // Medication sub-filters
  const [isBioTreatmentActive, setIsBioTreatmentActive] = useState(false);
  const [activeAntihistamineFilter, setActiveAntihistamineFilter] = useState<string | null>(null);

  // Consumption sub-filter
  const [selectedConsumptionCategoryFilter, setSelectedConsumptionCategoryFilter] = useState<string | null>(null);

  // Edit / Add Modal States
  const [editingEntry, setEditingEntry] = useState<LogEntry | null>(null);
  const [editTimestamp, setEditTimestamp] = useState<string>('');
  const [customError, setCustomError] = useState<string | null>(null);
  const [isDateTimePickerVisible, setIsDateTimePickerVisible] = useState(false);
  const [pendingAddTimestamp, setPendingAddTimestamp] = useState<string | null>(null);
  const [isAddModalVisible, setIsAddModalVisible] = useState(false);
  const [addModalType, setAddModalType] = useState<'ANTIHISTAMINE' | 'CORTISONE' | 'FLARE_UP'>('ANTIHISTAMINE');

  // Date Filter State
  type DateFilterType = 'ALL' | 'TODAY' | 'YESTERDAY' | 'LAST_7' | 'LAST_30' | 'CUSTOM';
  const [dateFilter, setDateFilter] = useState<DateFilterType>('ALL');
  const [customStartDate, setCustomStartDate] = useState(() => format(new Date(), 'yyyy-MM-dd'));
  const [customEndDate, setCustomEndDate] = useState(() => format(new Date(), 'yyyy-MM-dd'));
  const [dateRangeError, setDateRangeError] = useState<string | null>(null);

  // Pagination State
  const [visibleCount, setVisibleCount] = useState(10);

  // Generate dynamic list of antihistamines logged + favorite Maintenance Antihistamine
  const antihistamines = useMemo(() => {
    const list = new Set<string>();
    if (favoriteAntihistamine) {
      list.add(favoriteAntihistamine.split(' (')[0]); // Use shortened name if formatted like "Telefast (Fexofenadine 180 mg)"
    }
    entries.forEach(e => {
      if (e.type === 'ANTIHISTAMINE' && e.itemName) {
        list.add(e.itemName.split(' (')[0]);
      }
    });
    return Array.from(list);
  }, [entries, favoriteAntihistamine]);

  // Get full medication display name (recovering dosage if available)
  const getMedicationDisplayName = (shortName: string) => {
    if (favoriteAntihistamine && favoriteAntihistamine.toLowerCase().includes(shortName.toLowerCase())) {
      return favoriteAntihistamine;
    }
    const matchingEntry = entries.find(
      e => e.type === 'ANTIHISTAMINE' && e.itemName && e.itemName.toLowerCase().includes(shortName.toLowerCase())
    );
    if (matchingEntry && matchingEntry.itemName) {
      return matchingEntry.itemName;
    }
    return shortName;
  };

  // Format e.g. "Telefast (Fexofenadine 180 mg)" to "Telefast (180 mg)"
  const formatMedicationLabel = (name: string) => {
    const match = name.match(/^(.*?)\s*\((?:.*\s+)?(\d+\s*mg)\)$/i);
    if (match) {
      return `${match[1]} (${match[2]})`;
    }
    return name;
  };

  // Handle manual date range validation
  const validateCustomDates = (startStr: string, endStr: string) => {
    const dateRegex = /^\d{4}-\d{2}-\d{2}$/;
    if (!startStr || !endStr) {
      setDateRangeError('Please fill in both dates');
      return;
    }
    if (!dateRegex.test(startStr) || !dateRegex.test(endStr)) {
      setDateRangeError('Use YYYY-MM-DD format');
      return;
    }
    try {
      const start = parseISO(startStr);
      const end = parseISO(endStr);
      if (isNaN(start.getTime()) || isNaN(end.getTime())) {
        setDateRangeError('Invalid date values');
        return;
      }
      if (start > end) {
        setDateRangeError('Start date cannot be after End date');
        return;
      }
      setDateRangeError(null);
    } catch {
      setDateRangeError('Invalid date values');
    }
  };

  // Filter logs for display based on category, sub-filters and dates
  const filteredEntries = useMemo(() => {
    return entries.filter((entry) => {
      // 1. Category and Sub-filters
      if (activeCategory === 'MEDICATION') {
        if (isBioTreatmentActive) {
          if (entry.type !== 'CORTISONE') return false;
        } else {
          if (entry.type !== 'ANTIHISTAMINE') return false;
          // If specific antihistamine filter active
          if (activeAntihistamineFilter) {
            const shortItemName = entry.itemName?.split(' (')[0] || '';
            const match = shortItemName.toLowerCase() === activeAntihistamineFilter.toLowerCase();
            // Display generic logs (missing itemName) even under filters
            if (entry.itemName && !match) return false;
          }
        }
      } else if (activeCategory === 'FLARE_UP') {
        if (entry.type !== 'FLARE_UP') return false;
      } else if (activeCategory === 'CONSUMPTION') {
        if (entry.type !== 'CONSUMPTION') return false;
        if (selectedConsumptionCategoryFilter && entry.category !== selectedConsumptionCategoryFilter) return false;
      }

      // 2. Date Filters
      try {
        const entryDate = parseISO(entry.timestamp);
        const now = new Date();
        
        switch (dateFilter) {
          case 'TODAY':
            return isWithinInterval(entryDate, { start: startOfDay(now), end: endOfDay(now) });
          case 'YESTERDAY': {
            const yesterday = new Date();
            yesterday.setDate(yesterday.getDate() - 1);
            return isWithinInterval(entryDate, { start: startOfDay(yesterday), end: endOfDay(yesterday) });
          }
          case 'LAST_7': {
            const sevenDays = new Date();
            sevenDays.setDate(sevenDays.getDate() - 7);
            return isWithinInterval(entryDate, { start: startOfDay(sevenDays), end: endOfDay(now) });
          }
          case 'LAST_30': {
            const thirtyDays = new Date();
            thirtyDays.setDate(thirtyDays.getDate() - 30);
            return isWithinInterval(entryDate, { start: startOfDay(thirtyDays), end: endOfDay(now) });
          }
          case 'CUSTOM': {
            const dateRegex = /^\d{4}-\d{2}-\d{2}$/;
            if (!dateRegex.test(customStartDate) || !dateRegex.test(customEndDate)) return true;
            const start = startOfDay(parseISO(customStartDate));
            const end = endOfDay(parseISO(customEndDate));
            if (isNaN(start.getTime()) || isNaN(end.getTime()) || start > end) return true;
            return isWithinInterval(entryDate, { start, end });
          }
          default:
            return true;
        }
      } catch {
        return true;
      }
    });
  }, [entries, activeCategory, isBioTreatmentActive, activeAntihistamineFilter, selectedConsumptionCategoryFilter, dateFilter, customStartDate, customEndDate]);

  // Paginated and chronological group mapping
  const displayEntries = useMemo(() => {
    return filteredEntries.slice(0, visibleCount);
  }, [filteredEntries, visibleCount]);

  const groupedEntries = useMemo(() => {
    const groups: { [key: string]: LogEntry[] } = {};
    displayEntries.forEach(entry => {
      try {
        const date = parseISO(entry.timestamp);
        const monthYear = format(date, 'MMM yyyy');
        if (!groups[monthYear]) {
          groups[monthYear] = [];
        }
        groups[monthYear].push(entry);
      } catch {
        const fallback = 'Unknown Date';
        if (!groups[fallback]) {
          groups[fallback] = [];
        }
        groups[fallback].push(entry);
      }
    });
    
    return Object.keys(groups).map(monthYear => ({
      monthYear,
      data: groups[monthYear]
    }));
  }, [displayEntries]);

  const formatEntryDate = (isoString: string) => {
    try {
      const d = parseISO(isoString);
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
        const month = MONTH_NAMES[d.getMonth()].substring(0, 3);
        relativeDate = `${month} ${d.getDate()}, ${d.getFullYear()}`;
      }
      
      return `${relativeDate} • ${timeStr}`;
    } catch {
      return isoString;
    }
  };

  const startEdit = (entry: LogEntry) => {
    setEditingEntry(entry);
    setEditTimestamp(entry.timestamp);
    setCustomError(null);
  };

  const shiftTime = (amount: number, unit: 'minute' | 'hour' | 'day') => {
    try {
      const currentDate = parseISO(editTimestamp);
      let newDate = currentDate;

      if (unit === 'minute') {
        newDate = amount > 0 ? addMinutes(currentDate, amount) : subMinutes(currentDate, Math.abs(amount));
      } else if (unit === 'hour') {
        newDate = amount > 0 ? addHours(currentDate, amount) : subHours(currentDate, Math.abs(amount));
      } else if (unit === 'day') {
        newDate = amount > 0 ? addDays(currentDate, amount) : subDays(currentDate, Math.abs(amount));
      }

      setEditTimestamp(newDate.toISOString());
      setCustomError(null);
    } catch {
      setCustomError('Invalid date value');
    }
  };

  const saveEdit = (force = false) => {
    if (!editingEntry) return;
    const result = updateEntry(editingEntry.id, editTimestamp, force);
    if (result.collision) {
      setCollisionWarning({
        type: editingEntry.type,
        timestamp: editTimestamp,
        isEdit: true,
        entryId: editingEntry.id
      });
      setEditingEntry(null);
    } else {
      setEditingEntry(null);
    }
  };

  // Timeline markers & Category Themes
  const getEntryColor = (type: EntryType) => {
    switch (type) {
      case 'FLARE_UP': return '#814B92'; // Purple
      case 'ANTIHISTAMINE': return '#4C9A2A'; // Green
      case 'CORTISONE': return '#1A7E97'; // Teal
      case 'CONSUMPTION': return '#F78325'; // Orange
      default: return '#64748B';
    }
  };

  const getRelativeTimeText = (timestampStr: string) => {
    try {
      const d = parseISO(timestampStr);
      const now = new Date();
      
      const dDate = new Date(d.getFullYear(), d.getMonth(), d.getDate());
      const nowDate = new Date(now.getFullYear(), now.getMonth(), now.getDate());
      
      const diffTime = nowDate.getTime() - dDate.getTime();
      const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24));
      
      if (diffDays <= 0) {
        return 'Today';
      } else {
        return `${diffDays}d ago`;
      }
    } catch {
      return '';
    }
  };

  const getMedTimeSince = (item: LogEntry) => {
    if (item.type !== 'ANTIHISTAMINE' && item.type !== 'CORTISONE') return null;
    const meds = entries
      .filter(e => e.type === 'ANTIHISTAMINE' || e.type === 'CORTISONE')
      .sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime());
    const idx = meds.findIndex(e => e.id === item.id);
    if (idx < 0 || idx >= meds.length - 1) return null;
    const prevOlderMed = meds[idx + 1];
    return getTimeGap(item.timestamp, prevOlderMed.timestamp);
  };

  const getFlareUpTimeSince = (item: LogEntry) => {
    if (item.type !== 'FLARE_UP') return null;
    const flareUps = entries
      .filter(e => e.type === 'FLARE_UP')
      .sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime());
    const idx = flareUps.findIndex(e => e.id === item.id);
    if (idx < 0 || idx >= flareUps.length - 1) return null;
    const prevOlderFlareUp = flareUps[idx + 1];
    return getTimeGap(item.timestamp, prevOlderFlareUp.timestamp);
  };

  const getConsumptionTimeSince = (item: LogEntry) => {
    if (item.type !== 'CONSUMPTION') return null;
    const cons = entries
      .filter(e => e.type === 'CONSUMPTION')
      .sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime());
    const idx = cons.findIndex(e => e.id === item.id);
    if (idx < 0 || idx >= cons.length - 1) return null;
    const prevOlderCons = cons[idx + 1];
    return getTimeGap(item.timestamp, prevOlderCons.timestamp);
  };

  const cleanMedName = (rawName?: string) => {
    if (!rawName) return '';
    return rawName
      .replace(/\s*[-–—]\s*(?:1st|2nd|3rd|\d+(?:st|nd|rd|th)?)\s*generation\s*/gi, ' ')
      .replace(/\s+/g, ' ')
      .trim();
  };

  const getPillLabel = (entry: LogEntry) => {
    if (entry.type === 'FLARE_UP') {
      const isAngio = entry.itemName?.toLowerCase().includes('angioedema');
      return isAngio ? 'Angioedema Only' : 'Flare Up';
    }
    switch (entry.type) {
      case 'ANTIHISTAMINE': return 'Medication';
      case 'CORTISONE': return 'Biological Treatment';
      case 'CONSUMPTION': return 'Consumption';
      default: return 'Entry';
    }
  };

  const getBadgeType = (type: EntryType) => {
    switch (type) {
      case 'FLARE_UP': return 'flare';
      case 'ANTIHISTAMINE': return 'antihistamine';
      case 'CORTISONE': return 'cortisone';
      case 'CONSUMPTION': return 'consumption';
      default: return 'antihistamine';
    }
  };

  const getActiveTabTheme = () => {
    if (activeCategory === 'MEDICATION') {
      return isBioTreatmentActive 
        ? { text: '#1A7E97', bg: 'rgba(26, 126, 151, 0.15)', border: 'rgba(26, 126, 151, 0.3)' }
        : { text: '#509729', bg: 'rgba(80, 151, 41, 0.15)', border: 'rgba(80, 151, 41, 0.3)' };
    } else if (activeCategory === 'FLARE_UP') {
      return { text: '#814B92', bg: 'rgba(129, 75, 146, 0.15)', border: 'rgba(129, 75, 146, 0.3)' };
    } else {
      return { text: '#F78325', bg: 'rgba(247, 131, 37, 0.15)', border: 'rgba(247, 131, 37, 0.3)' };
    }
  };

  const currentTheme = getActiveTabTheme();

  // Action Triggers
  const performManualAddFlow = () => {
    setIsDateTimePickerVisible(true);
  };

  const handleConfirmDateTime = (timestamp: string) => {
    setIsDateTimePickerVisible(false);
    setPendingAddTimestamp(timestamp);
    if (activeCategory === 'CONSUMPTION') {
      if (onOpenAddConsumption) onOpenAddConsumption(timestamp);
    } else {
      if (activeCategory === 'MEDICATION') {
        setAddModalType(isBioTreatmentActive ? 'CORTISONE' : 'ANTIHISTAMINE');
      } else {
        setAddModalType('FLARE_UP');
      }
      setIsAddModalVisible(true);
    }
  };

  const performExportFlow = async () => {
    if (filteredEntries.length === 0) {
      Alert.alert('No entries', 'There are no entries in the current filtered view to export.');
      return;
    }
    const success = await exportLogsToCSV(filteredEntries);
    if (success) {
      Alert.alert('Export Success', 'Logs exported successfully.');
    } else {
      Alert.alert('Export Failed', 'An error occurred during export.');
    }
  };

  const performResetFlow = () => {
    Alert.alert(
      'Reset Log',
      'Are you sure you want to clear all tracked entries? This action cannot be undone.',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Clear All',
          style: 'destructive',
          onPress: () => {
            const clearEntries = useTrackerStore.getState().clearEntries;
            clearEntries();
          }
        }
      ]
    );
  };

  // Render log detail rows in timeline cards
  const renderCardDetails = (item: LogEntry) => {
    switch (item.type) {
      case 'CONSUMPTION':
        return (
          <View style={styles.detailRow}>
            <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' }}>
              <View style={{ flex: 1, paddingRight: 8 }}>
                <Text style={styles.detailTitle}>
                  Consumed: <Text style={{ fontWeight: '700', color: '#F78325' }}>{item.itemName}</Text>
                </Text>
                {item.amount ? (
                  <Text style={styles.detailSubtitle}>Amount: {item.amount}</Text>
                ) : null}
                {item.notes ? (
                  <Text style={styles.detailSubtitle}>Notes: {item.notes}</Text>
                ) : null}
                <Text style={[
                  styles.detailSubtitle,
                  { color: item.status === 'Trigger' ? '#F78325' : '#509729', fontWeight: '600', marginTop: 4 }
                ]}>
                  Status: {item.status || 'Logged'}
                </Text>
                {hasBeenTriggerBefore(item.itemName || '', item.id) && (
                  <Text style={styles.previouslyTriggeredText}>
                    ⚠️ Previously marked as trigger
                  </Text>
                )}
              </View>

              <TouchableOpacity
                style={[
                  styles.triggerBtn,
                  item.status === 'Trigger' && styles.triggerBtnActive
                ]}
                onPress={() => toggleConsumptionTriggerStatus(item.id)}
                activeOpacity={0.7}
              >
                <Text style={[
                  styles.triggerBtnText,
                  item.status === 'Trigger' && styles.triggerBtnTextActive
                ]}>
                  {item.status === 'Trigger' ? 'Marked as Trigger' : 'Trigger'}
                </Text>
              </TouchableOpacity>
            </View>
          </View>
        );
      case 'ANTIHISTAMINE':
        return (
          <View style={styles.detailRow}>
            <Text style={styles.detailTitle}>
              Medication: <Text style={{ fontWeight: '700', color: '#509729' }}>{item.itemName || 'Antihistamine (Generic)'}</Text>
            </Text>
            {item.notes ? (
              <Text style={styles.detailSubtitle}>Notes: {item.notes}</Text>
            ) : null}
          </View>
        );
      case 'CORTISONE':
        return (
          <View style={styles.detailRow}>
            <Text style={styles.detailTitle}>
              Biological Treatment: <Text style={{ fontWeight: '700', color: '#1A7E97' }}>{item.itemName || 'Biological (Generic)'}</Text>
            </Text>
            {item.notes ? (
              <Text style={styles.detailSubtitle}>Notes: {item.notes}</Text>
            ) : null}
          </View>
        );
      case 'FLARE_UP': {
        const isSeverity = item.notes === 'Mild' || item.notes === 'Moderate' || item.notes === 'Severe' || item.notes === 'Very Severe';
        return (
          <View style={styles.detailRow}>
            <Text style={styles.detailTitle}>
              Symptom: <Text style={{ fontWeight: '700', color: '#814B92' }}>{item.itemName || 'Symptom Flare-up'}</Text>
            </Text>
            {isSeverity && item.notes ? (
              <Text style={styles.detailSubtitle}>Severity: {item.notes}</Text>
            ) : null}
            {!isSeverity && item.notes ? (
              <Text style={styles.detailSubtitle}>Potential Reason: {item.notes}</Text>
            ) : null}
          </View>
        );
      }
      default:
        return null;
    }
  };

  return (
    <View style={styles.container}>
      {/* Redesigned Header row with centered title */}
      <View style={styles.headerRow}>
        {onBack && (
          <TouchableOpacity onPress={onBack} style={styles.headerBackBtn} activeOpacity={0.75}>
            <IconBackArrow size={18} />
          </TouchableOpacity>
        )}
        <View style={styles.headerTitleContainer}>
          <Text style={styles.headerTitleText}>My Logs</Text>
        </View>
        <View style={{ width: 34 }} />
      </View>

      {/* Main Category Tabs Selector divided by vertical separators */}
      <View style={styles.tabOuterRow}>
        <View style={styles.tabInnerRow}>
          {/* Tab 1: Medication */}
          <TouchableOpacity
            onPress={() => {
              setActiveCategory('MEDICATION');
              setSelectedConsumptionCategoryFilter(null);
            }}
            style={[
              styles.tabButton,
              activeCategory === 'MEDICATION' ? [styles.tabButtonActive, { backgroundColor: '#E8F7EC' }] : styles.tabButtonInactive
            ]}
          >
            <Text style={[
              styles.tabText,
              activeCategory === 'MEDICATION' ? [styles.tabTextActive, { color: '#4C9A2A' }] : styles.tabTextInactive
            ]}>
              Medication
            </Text>
            {activeCategory === 'MEDICATION' && <View style={[styles.tabUnderline, { backgroundColor: '#4C9A2A' }]} />}
          </TouchableOpacity>

          {/* Separator 1 */}
          <View style={styles.tabSeparator} />

          {/* Tab 2: Flare Up */}
          <TouchableOpacity
            onPress={() => {
              setActiveCategory('FLARE_UP');
              setSelectedConsumptionCategoryFilter(null);
            }}
            style={[
              styles.tabButton,
              activeCategory === 'FLARE_UP' ? [styles.tabButtonActive, { backgroundColor: '#F3E8FF' }] : styles.tabButtonInactive
            ]}
          >
            <Text style={[
              styles.tabText,
              activeCategory === 'FLARE_UP' ? [styles.tabTextActive, { color: '#814B92' }] : styles.tabTextInactive
            ]}>
              Flare Up
            </Text>
            {activeCategory === 'FLARE_UP' && <View style={[styles.tabUnderline, { backgroundColor: '#814B92' }]} />}
          </TouchableOpacity>

          {/* Separator 2 */}
          <View style={styles.tabSeparator} />

          {/* Tab 3: Consumption */}
          <TouchableOpacity
            onPress={() => {
              setActiveCategory('CONSUMPTION');
            }}
            style={[
              styles.tabButton,
              activeCategory === 'CONSUMPTION' ? [styles.tabButtonActive, { backgroundColor: '#FFEAD2' }] : styles.tabButtonInactive
            ]}
          >
            <Text style={[
              styles.tabText,
              activeCategory === 'CONSUMPTION' ? [styles.tabTextActive, { color: '#F78325' }] : styles.tabTextInactive
            ]}>
              Consumptions
            </Text>
            {activeCategory === 'CONSUMPTION' && <View style={[styles.tabUnderline, { backgroundColor: '#F78325' }]} />}
          </TouchableOpacity>
        </View>
      </View>

      {/* Medication Tab-Specific Sub-filters */}
      {activeCategory === 'MEDICATION' && (
        <View style={styles.subFiltersContainer}>
          {/* Saved antihistamine medications wrapping naturally */}
          {antihistamines.length > 0 && (
            <View style={styles.medicationCardsContainer}>
              {antihistamines.map((name, index) => {
                const isSelected = activeAntihistamineFilter === name;
                const isFavorite = index === 0;
                const fullName = getMedicationDisplayName(name);
                const cleanLabel = formatMedicationLabel(fullName);
                
                const parts = cleanLabel.split(' (');
                const rawMedName = parts[0];
                const medName = rawMedName.replace(/\s*[–-]\s*\d+(?:st|nd|rd|th)\s+generation/i, '').trim();
                const medDosage = parts[1] ? parts[1].replace(')', '') : '';

                return (
                  <TouchableOpacity
                    key={name}
                    onPress={() => {
                      setActiveAntihistamineFilter(isSelected ? null : name);
                      setIsBioTreatmentActive(false); // clear bio treatment filter if selecting antihistamine
                    }}
                    style={[
                      styles.medicationCard,
                      isSelected ? styles.medicationCardSelected : styles.medicationCardUnselected
                    ]}
                    activeOpacity={0.8}
                  >
                    <View style={styles.medCardTopRow}>
                      {isFavorite && (
                        <Text style={styles.favoriteStarText}>
                          ★
                        </Text>
                      )}
                      <IconCapsule 
                        color="#4C9A2A" 
                        size={12} 
                      />
                      <Text style={styles.medicationNameText}>
                        {medName}{medDosage ? ` (${medDosage})` : ''}
                      </Text>
                    </View>
                  </TouchableOpacity>
                );
              })}
            </View>
          )}

          {/* Biological Treatment Premium Action Card */}
          <TouchableOpacity
            onPress={() => {
              setIsBioTreatmentActive(!isBioTreatmentActive);
              setActiveAntihistamineFilter(null); // clear antihistamine filter if switching to bio
            }}
            style={[
              styles.bioTreatmentCard,
              isBioTreatmentActive ? styles.bioTreatmentCardActive : styles.bioTreatmentCardInactive
            ]}
            activeOpacity={0.8}
          >
            <View style={styles.bioCardLeftSection}>
              <IconSyringeOutline 
                color="#238A9C" 
                size={14} 
              />
              <View style={styles.bioTextContainer}>
                <Text style={styles.bioCardTitle}>
                  Biological Treatment Log
                </Text>
                <Text style={styles.bioCardSubtitle}>
                  View or add treatment doses
                </Text>
              </View>
            </View>
            <Text style={styles.bioChevron}>
              ›
            </Text>
          </TouchableOpacity>
        </View>
      )}

      {/* Consumption Tab-Specific Category chips */}
      {activeCategory === 'CONSUMPTION' && (
        <ScrollView
          horizontal
          showsHorizontalScrollIndicator={false}
          style={styles.horizontalScrollRow}
          contentContainerStyle={styles.horizontalScrollContent}
        >
          {['All', 'Food', 'Drinks', 'Medications', 'Supplements', 'Other'].map(cat => {
            const isSelected = (cat === 'All' && !selectedConsumptionCategoryFilter) || (selectedConsumptionCategoryFilter === cat);
            return (
              <TouchableOpacity
                key={cat}
                onPress={() => setSelectedConsumptionCategoryFilter(cat === 'All' ? null : cat)}
                style={[
                  styles.subFilterChip,
                  isSelected && styles.subFilterChipActiveCons
                ]}
              >
                <Text style={[
                  styles.subFilterChipText,
                  isSelected && styles.subFilterChipTextActive
                ]}>
                  {cat}
                </Text>
              </TouchableOpacity>
            );
          })}
        </ScrollView>
      )}

      {/* Redesigned Premium Actions Bar: Add Entry | Export | Reset Log */}
      <View style={styles.actionsPanel}>
        {/* Action 1: Add Entry */}
        <TouchableOpacity
          style={styles.actionBtn}
          activeOpacity={0.8}
          onPress={performManualAddFlow}
        >
          <IconPlusInCircle color="#4C9A2A" size={16} style={{ marginBottom: 3 }} />
          <Text style={styles.actionBtnText}>Add Entry</Text>
        </TouchableOpacity>

        {/* Divider 1 */}
        <View style={styles.actionsDivider} />

        {/* Action 2: Export */}
        <TouchableOpacity
          style={styles.actionBtn}
          activeOpacity={0.8}
          onPress={performExportFlow}
        >
          <IconDownloadArrow color="#4C9A2A" size={16} style={{ marginBottom: 3 }} />
          <Text style={styles.actionBtnText}>Export</Text>
        </TouchableOpacity>

        {/* Divider 2 */}
        <View style={styles.actionsDivider} />

        {/* Action 3: Reset Log */}
        <TouchableOpacity
          style={styles.actionBtn}
          activeOpacity={0.8}
          onPress={performResetFlow}
        >
          <IconTrashCan color="#E53935" size={16} style={{ marginBottom: 3 }} />
          <Text style={[styles.actionBtnText, styles.actionBtnTextReset]}>Reset Log</Text>
        </TouchableOpacity>
      </View>

      {/* Date Filter Selection Row */}
      <View style={styles.dateFilterCard}>
        <Text style={styles.dateFilterHeader}>FILTER BY DATE</Text>
        <ScrollView
          horizontal
          showsHorizontalScrollIndicator={false}
          style={styles.dateScrollRow}
          contentContainerStyle={styles.dateScrollContent}
        >
          {(['ALL', 'TODAY', 'YESTERDAY', 'LAST_7', 'LAST_30', 'CUSTOM'] as const).map(filterOpt => {
            const isSelected = dateFilter === filterOpt;
            const label = filterOpt === 'ALL' ? 'All Time'
                        : filterOpt === 'TODAY' ? 'Today'
                        : filterOpt === 'YESTERDAY' ? 'Yesterday'
                        : filterOpt === 'LAST_7' ? 'Last 7 Days'
                        : filterOpt === 'LAST_30' ? 'Last 30 Days'
                        : 'Custom Range';
            return (
              <TouchableOpacity
                key={filterOpt}
                onPress={() => {
                  setDateFilter(filterOpt);
                  if (filterOpt !== 'CUSTOM') {
                    setDateRangeError(null);
                  }
                }}
                style={[
                  styles.dateChip,
                  isSelected && { backgroundColor: currentTheme.text, borderColor: 'transparent' }
                ]}
              >
                <Text style={[
                  styles.dateChipText,
                  isSelected ? { color: '#000000' } : { color: currentTheme.text }
                ]}>
                  {label}
                </Text>
              </TouchableOpacity>
            );
          })}
        </ScrollView>

        {/* Custom Range Range Date Inputs */}
        {dateFilter === 'CUSTOM' && (
          <View style={styles.customDateInputsRow}>
            <View style={styles.dateInputWrapper}>
              <Text style={styles.dateInputLabel}>Start Date</Text>
              <TextInput
                style={[styles.dateInput, dateRangeError ? styles.dateInputError : null]}
                value={customStartDate}
                onChangeText={(text) => {
                  setCustomStartDate(text);
                  validateCustomDates(text, customEndDate);
                }}
                placeholder="YYYY-MM-DD"
                placeholderTextColor="#64748B"
              />
            </View>
            <View style={styles.dateInputWrapper}>
              <Text style={styles.dateInputLabel}>End Date</Text>
              <TextInput
                style={[styles.dateInput, dateRangeError ? styles.dateInputError : null]}
                value={customEndDate}
                onChangeText={(text) => {
                  setCustomEndDate(text);
                  validateCustomDates(customStartDate, text);
                }}
                placeholder="YYYY-MM-DD"
                placeholderTextColor="#64748B"
              />
            </View>
          </View>
        )}
        {dateFilter === 'CUSTOM' && dateRangeError && (
          <Text style={styles.dateErrorText}>{dateRangeError}</Text>
        )}
      </View>

      {/* Chronological Month-Grouped Log Timeline */}
      <View style={styles.timelineList}>
        {displayEntries.length === 0 ? (
          <View style={styles.zeroState}>
            <Text style={styles.zeroStateText}>No entries found for this tracking window</Text>
          </View>
        ) : (
          groupedEntries.map((group) => (
            <View key={group.monthYear} style={styles.monthGroupContainer}>
              {/* Chronological Month Header */}
              <Text style={styles.monthHeader}>{group.monthYear}</Text>
              <View style={styles.monthItemsContainer}>
                {group.data.map((item, idx) => {
                  const friendlyDate = formatEntryDate(item.timestamp);
                  const pillColor = getEntryColor(item.type);

                  // Determine type-based styling
                  let dotColor = '#4C9A2A';
                  let lineColor = '#C8E6C9';
                  let pillBg = '#E8F7EC';
                  let pillText = '#2E7D32';
                  let timeGapText: string | null = null;
                  let displayTitle = '';
                  let detailsComponent: React.ReactNode = null;

                  if (item.type === 'ANTIHISTAMINE' || item.type === 'CORTISONE') {
                    const isCortisone = item.type === 'CORTISONE';
                    dotColor = isCortisone ? '#1A7E97' : '#4C9A2A';
                    lineColor = isCortisone ? '#A8D8D8' : '#C8E6C9';
                    pillBg = isCortisone ? '#E6F4F8' : '#E8F7EC';
                    pillText = isCortisone ? '#1A7E97' : '#2E7D32';
                    timeGapText = getMedTimeSince(item);
                    displayTitle = cleanMedName(item.itemName) || (isCortisone ? 'Corticosteroid' : 'Antihistamine');
                  } else if (item.type === 'FLARE_UP') {
                    dotColor = '#814B92';
                    lineColor = 'rgba(129, 75, 146, 0.3)';
                    pillBg = '#F3E8FF';
                    pillText = '#814B92';
                    timeGapText = getFlareUpTimeSince(item);
                    displayTitle = item.itemName || 'Symptom Flare-up';
                    
                    const isSeverity = item.notes === 'Mild' || item.notes === 'Moderate' || item.notes === 'Severe' || item.notes === 'Very Severe' || item.notes === 'Critical';
                    if (isSeverity && item.notes) {
                      let severityColor = '#64748B';
                      if (item.notes === 'Mild') severityColor = '#10B981';
                      else if (item.notes === 'Moderate') severityColor = '#D97706';
                      else if (item.notes === 'Severe' || item.notes === 'Very Severe') severityColor = '#EA580C';
                      else if (item.notes === 'Critical') severityColor = '#EF4444';

                      detailsComponent = (
                        <Text style={styles.medDetailLabel}>
                          Severity:{' '}
                          <Text style={{ color: severityColor, fontWeight: 'bold' }}>
                            {item.notes}
                          </Text>
                        </Text>
                      );
                    } else if (item.notes) {
                      detailsComponent = (
                        <Text style={styles.medDetailLabel}>Potential Reason: {item.notes}</Text>
                      );
                    }
                  } else if (item.type === 'CONSUMPTION') {
                    dotColor = '#F78325';
                    lineColor = 'rgba(247, 131, 37, 0.3)';
                    pillBg = '#FFEAD2';
                    pillText = '#F78325';
                    timeGapText = getConsumptionTimeSince(item);
                    displayTitle = item.itemName || 'Consumption';

                    const detailsList: string[] = [];
                    if (item.amount) detailsList.push(`Amount: ${item.amount}`);
                    if (item.notes) detailsList.push(`Notes: ${item.notes}`);
                    const detailLabel = detailsList.join(' • ');

                    detailsComponent = (
                      <View style={{ marginTop: 3 }}>
                        {detailLabel ? <Text style={styles.medDetailLabel}>{detailLabel}</Text> : null}
                        <View style={{ flexDirection: 'row', alignItems: 'center', marginTop: 4 }}>
                          <TouchableOpacity
                            style={[
                              styles.triggerBtnCompact,
                              item.status === 'Trigger' && styles.triggerBtnCompactActive
                            ]}
                            onPress={() => toggleConsumptionTriggerStatus(item.id)}
                            activeOpacity={0.7}
                          >
                            <Text style={[
                              styles.triggerBtnCompactText,
                              item.status === 'Trigger' && styles.triggerBtnCompactTextActive
                            ]}>
                              {item.status === 'Trigger' ? '⚠️ Marked as Trigger' : 'Mark as Trigger'}
                            </Text>
                          </TouchableOpacity>
                          {hasBeenTriggerBefore(item.itemName || '', item.id) && (
                            <Text style={styles.previouslyTriggeredCompactText}>
                              ⚠️ Previously marked as trigger
                            </Text>
                          )}
                        </View>
                      </View>
                    );
                  }

                  return (
                    <View key={item.id} style={styles.medItemWrapper}>
                      <View style={styles.medRowContainer}>
                        {/* Left vertical timeline line & dot */}
                        <View style={styles.medTimelineLeft}>
                          <View style={[styles.medTimelineLine, { backgroundColor: lineColor }]} />
                          <View style={[styles.medTimelineDot, { backgroundColor: dotColor }]} />
                        </View>

                        {/* Middle Column: Date & Title/Details */}
                        <View style={styles.medContentColumn}>
                          <Text style={styles.medDateText}>{friendlyDate}</Text>
                          <Text style={styles.medNameText}>{displayTitle}</Text>
                          {detailsComponent}
                        </View>

                        {/* Right Column: Time-since pill & Actions (styled identically to Recent) */}
                        <View style={styles.medRightColumn}>
                          {timeGapText ? (
                            <View style={[styles.medTimePill, { backgroundColor: pillBg }]}>
                              <Text style={[styles.medTimeText, { color: pillText }]}>{timeGapText}</Text>
                            </View>
                          ) : null}

                          <View style={styles.medActionsRow}>
                            <TouchableOpacity onPress={() => startEdit(item)} style={styles.recentActionEditBtn} activeOpacity={0.75}>
                              <IconEdit color="#64748B" size={15} />
                            </TouchableOpacity>
                            <TouchableOpacity onPress={() => deleteEntry(item.id)} style={styles.recentActionDeleteBtn} activeOpacity={0.75}>
                              <IconDelete color="#EF4444" size={15} />
                            </TouchableOpacity>
                          </View>
                        </View>
                      </View>
                      {idx < group.data.length - 1 && <View style={styles.medDivider} />}
                    </View>
                  );
                })}
              </View>
            </View>
          ))
        )}
      </View>

      {/* Pagination Load More logs */}
      {filteredEntries.length > visibleCount && (
        <TouchableOpacity
          style={styles.loadMoreBtn}
          onPress={() => setVisibleCount(prev => prev + 10)}
          activeOpacity={0.7}
        >
          <Text style={[styles.loadMoreBtnText, { color: currentTheme.text }]}>
            Load More Logs ({filteredEntries.length - visibleCount} remaining)
          </Text>
        </TouchableOpacity>
      )}

      {/* Retroactive Edit Modal */}
      <Modal
        visible={editingEntry !== null}
        onClose={() => setEditingEntry(null)}
        title="Retroactive Log Editor"
        footerActions={
          <>
            <Button title="Cancel" onPress={() => setEditingEntry(null)} variant="disabled" style={styles.modalCancel} />
            <Button title="Save Changes" onPress={() => saveEdit(false)} variant="primary" style={{ backgroundColor: getEntryColor(editingEntry?.type || 'ANTIHISTAMINE') }} />
          </>
        }
      >
        {editingEntry && (
          <View style={styles.modalBadgeContainer}>
            <Badge label={`Altering: ${getPillLabel(editingEntry)}`} type={getBadgeType(editingEntry.type)} />
          </View>
        )}

        <Text style={styles.inputLabel}>Adjust Date & Time:</Text>
        <TextInput
          style={styles.modalInput}
          value={editTimestamp}
          onChangeText={(text) => {
            setEditTimestamp(text);
            setCustomError(null);
          }}
          placeholder="ISO 8601 Timestamp"
          placeholderTextColor="#64748B"
        />
        {customError && <Text style={styles.errorText}>{customError}</Text>}

        {/* Tactile micro shifters */}
        <View style={styles.adjusterGrid}>
          <View style={styles.adjusterRow}>
            <Text style={styles.adjusterLabel}>Mins</Text>
            <TouchableOpacity onPress={() => shiftTime(-10, 'minute')} style={styles.adjustBtn}>
              <Text style={styles.adjustBtnText}>-10m</Text>
            </TouchableOpacity>
            <TouchableOpacity onPress={() => shiftTime(10, 'minute')} style={styles.adjustBtn}>
              <Text style={styles.adjustBtnText}>+10m</Text>
            </TouchableOpacity>
          </View>

          <View style={styles.adjusterRow}>
            <Text style={styles.adjusterLabel}>Hours</Text>
            <TouchableOpacity onPress={() => shiftTime(-1, 'hour')} style={styles.adjustBtn}>
              <Text style={styles.adjustBtnText}>-1h</Text>
            </TouchableOpacity>
            <TouchableOpacity onPress={() => shiftTime(1, 'hour')} style={styles.adjustBtn}>
              <Text style={styles.adjustBtnText}>+1h</Text>
            </TouchableOpacity>
          </View>

          <View style={styles.adjusterRow}>
            <Text style={styles.adjusterLabel}>Days</Text>
            <TouchableOpacity onPress={() => shiftTime(-1, 'day')} style={styles.adjustBtn}>
              <Text style={styles.adjustBtnText}>-1d</Text>
            </TouchableOpacity>
            <TouchableOpacity onPress={() => shiftTime(1, 'day')} style={styles.adjustBtn}>
              <Text style={styles.adjustBtnText}>+1d</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>

      {/* Date & Time Selection Modal */}
      <DateTimePickerModal
        visible={isDateTimePickerVisible}
        onClose={() => setIsDateTimePickerVisible(false)}
        onConfirm={handleConfirmDateTime}
        themeColor={
          activeCategory === 'MEDICATION'
            ? (isBioTreatmentActive ? '#1A7E97' : '#4C9A2A')
            : activeCategory === 'FLARE_UP'
            ? '#814B92'
            : '#F78325'
        }
      />

      {/* Manual Entry Modal */}
      <MedicationEntryModal
        visible={isAddModalVisible}
        onClose={() => {
          setIsAddModalVisible(false);
          setPendingAddTimestamp(null);
        }}
        initialType={addModalType}
        initialTimestamp={pendingAddTimestamp}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    backgroundColor: '#000000', // AMOLED Black
    paddingHorizontal: 16,
    paddingTop: 16,
    paddingBottom: 40,
  },

  headerRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    position: 'relative',
    height: 48,
    marginBottom: 16,
  },
  headerBackBtn: {
    position: 'absolute',
    left: 0,
    width: 34,
    height: 34,
    borderRadius: 17,
    borderWidth: 1.2,
    borderColor: '#E2E8F0', // light grey border matching Library
    backgroundColor: '#FFFFFF',
    justifyContent: 'center',
    alignItems: 'center',
  },
  headerTitleContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
  },
  headerTitleText: {
    fontSize: 20,
    fontWeight: '800',
    letterSpacing: 0.2,
    color: '#1E293B',
  },
  headerTitleLetter: {
    fontSize: 20, // same size as Library title (20)
    fontWeight: '800',
    letterSpacing: 0.2,
  },
  headerFilterBtn: {
    position: 'absolute',
    right: 0,
    width: 32,
    height: 32,
    borderRadius: 8,
    borderWidth: 1.2,
    borderColor: '#1E1E20',
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: 'transparent',
  },
  filterSymbol: {
    color: '#64748B',
    fontSize: 14,
    fontWeight: 'bold',
  },
  tabOuterRow: {
    backgroundColor: '#EEF1F7',
    borderRadius: 10,
    padding: 3,
    marginBottom: 12,
  },
  tabInnerRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  tabButton: {
    flex: 1,
    height: 38,
    borderRadius: 8,
    alignItems: 'center',
    justifyContent: 'center',
    position: 'relative',
  },
  tabButtonActive: {
    backgroundColor: '#EAF9F0',
  },
  tabButtonInactive: {
    backgroundColor: '#EEF1F7',
  },
  tabSeparator: {
    width: 1,
    height: 18,
    backgroundColor: '#CBD5E1',
  },
  tabText: {
    fontSize: 12.5,
    fontWeight: '700',
    textAlign: 'center',
  },
  tabTextActive: {
    color: '#4C9A2A',
  },
  tabTextInactive: {
    color: '#64748B',
  },
  tabUnderline: {
    position: 'absolute',
    bottom: 3,
    width: 24,
    height: 2.5,
    borderRadius: 1.25,
    backgroundColor: '#4C9A2A',
  },
  subFiltersContainer: {
    marginBottom: 8,
  },
  horizontalScrollRow: {
    marginVertical: 4,
  },
  horizontalScrollContent: {
    gap: 8,
    paddingHorizontal: 2,
    paddingBottom: 4,
  },
  subFilterChip: {
    height: 34,
    borderRadius: 17,
    borderWidth: 1.2,
    borderColor: '#E2E8F0',
    backgroundColor: '#F1F5F9',
    paddingHorizontal: 14,
    justifyContent: 'center',
    alignItems: 'center',
  },
  subFilterChipActiveMed: {
    backgroundColor: 'rgba(80, 151, 41, 0.15)',
    borderColor: 'rgba(80, 151, 41, 0.5)',
  },
  subFilterChipActiveCons: {
    backgroundColor: '#F78325',
    borderColor: 'transparent',
  },
  subFilterChipText: {
    color: '#64748B',
    fontSize: 11,
    fontWeight: 'bold',
  },
  subFilterChipTextActive: {
    color: '#FFFFFF',
  },
  bioTreatmentBtn: {
    backgroundColor: '#1A7E97',
    borderRadius: 20,
    paddingVertical: 8,
    paddingHorizontal: 16,
    alignItems: 'center',
    justifyContent: 'center',
    marginVertical: 6,
    alignSelf: 'flex-start',
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.1)',
  },
  bioTreatmentBtnActive: {
    backgroundColor: '#11596B',
  },
  bioTreatmentBtnText: {
    color: '#FFFFFF',
    fontSize: 11,
    fontWeight: 'bold',
  },
  medicationCardsContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
    marginVertical: 8,
  },
  medicationCard: {
    paddingVertical: 5,
    paddingHorizontal: 9,
    borderRadius: 10,
    borderWidth: 1.2,
    alignItems: 'center',
    justifyContent: 'center',
    minWidth: 80,
  },
  medicationCardUnselected: {
    backgroundColor: '#F1FAF3',
    borderColor: '#BFD9B8',
  },
  medicationCardSelected: {
    backgroundColor: '#F1FAF3',
    borderColor: '#4C9A2A',
  },
  medCardTopRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 5,
  },
  favoriteStarText: {
    fontSize: 11.5,
    fontWeight: 'bold',
    color: '#F2C94C',
  },
  medicationNameText: {
    fontSize: 11.5,
    fontWeight: '700',
    color: '#4C9A2A',
  },
  medicationDosageText: {
    fontSize: 10,
    fontWeight: '600',
    marginTop: 2,
    color: '#708070',
  },
  textWhite: {
    color: '#FFFFFF',
  },
  textWhiteMuted: {
    color: 'rgba(255, 255, 255, 0.75)',
  },
  textDark: {
    color: '#0F172A',
  },
  textMuted: {
    color: '#64748B',
  },
  bioTreatmentCard: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 8,
    paddingHorizontal: 12,
    borderRadius: 14,
    borderWidth: 1.2,
    marginVertical: 5,
    shadowColor: '#238A9C',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.05,
    shadowRadius: 3,
    elevation: 2,
  },
  bioTreatmentCardInactive: {
    backgroundColor: '#F0FAFA',
    borderColor: '#A8D8D8',
  },
  bioTreatmentCardActive: {
    backgroundColor: '#F0FAFA',
    borderColor: '#238A9C',
    borderWidth: 1.8,
  },
  bioCardLeftSection: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
  },
  bioTextContainer: {
    justifyContent: 'center',
  },
  bioCardTitle: {
    fontSize: 12.5,
    fontWeight: '700',
    color: '#1F2937',
  },
  bioCardSubtitle: {
    fontSize: 10,
    marginTop: 1,
    color: '#64748B',
  },
  bioChevron: {
    color: '#238A9C',
    fontSize: 16,
    fontWeight: 'bold',
  },
  actionsPanel: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: '#FFFFFF',
    borderWidth: 1.2,
    borderColor: '#E5E7EB',
    borderRadius: 14,
    paddingVertical: 10,
    paddingHorizontal: 14,
    marginVertical: 6,
    shadowColor: '#000000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.05,
    shadowRadius: 3,
    elevation: 1,
  },
  actionBtn: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  actionsDivider: {
    width: 1,
    height: 22,
    backgroundColor: '#E5E7EB',
  },
  actionBtnText: {
    fontSize: 11,
    fontWeight: '700',
    color: '#1F2937',
  },
  actionBtnTextReset: {
    color: '#E53935',
  },
  dateFilterCard: {
    backgroundColor: '#0F172A',
    borderWidth: 1,
    borderColor: '#1E1E20',
    borderRadius: 16,
    padding: 12,
    marginVertical: 10,
    ...theme.shadows.subtle,
  },
  dateFilterHeader: {
    color: '#64748B',
    fontSize: 10,
    fontWeight: 'bold',
    letterSpacing: 1,
    marginBottom: 8,
  },
  dateScrollRow: {
    marginVertical: 4,
  },
  dateScrollContent: {
    gap: 8,
    paddingHorizontal: 2,
  },
  dateChip: {
    height: 30,
    borderRadius: 15,
    borderWidth: 1.2,
    borderColor: '#1E1E20',
    backgroundColor: '#000000',
    paddingHorizontal: 12,
    justifyContent: 'center',
    alignItems: 'center',
  },
  dateChipText: {
    fontSize: 10.5,
    fontWeight: 'bold',
  },
  customDateInputsRow: {
    flexDirection: 'row',
    gap: 12,
    marginTop: 10,
  },
  dateInputWrapper: {
    flex: 1,
  },
  dateInputLabel: {
    fontSize: 10,
    fontWeight: 'bold',
    color: '#64748B',
    marginBottom: 4,
  },
  dateInput: {
    backgroundColor: '#000000',
    borderWidth: 1,
    borderColor: '#1E1E20',
    borderRadius: 8,
    paddingHorizontal: 10,
    paddingVertical: 6,
    fontSize: 11,
    color: '#FFFFFF',
  },
  dateInputError: {
    borderColor: '#EF4444',
  },
  dateErrorText: {
    color: '#EF4444',
    fontSize: 10,
    marginTop: 6,
    fontWeight: '600',
  },
  timelineList: {
    marginTop: 12,
  },
  monthGroupContainer: {
    marginBottom: 16,
  },
  monthHeader: {
    fontSize: 13,
    fontWeight: 'bold',
    color: '#344054', // Month title: #344054
    marginLeft: 8,
    marginBottom: 8,
    letterSpacing: 0.5,
  },
  monthItemsContainer: {
    position: 'relative',
    backgroundColor: '#FFFFFF',
    borderRadius: 16,
    borderWidth: 1,
    borderColor: '#E2E8F0',
    overflow: 'hidden',
    paddingVertical: 4,
    ...theme.shadows.subtle,
  },
  medItemWrapper: {
    paddingHorizontal: 12,
  },
  medRowContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 14,
    position: 'relative',
  },
  medTimelineLeft: {
    width: 24,
    alignItems: 'center',
    justifyContent: 'center',
    position: 'relative',
    height: '100%',
    marginRight: 8,
  },
  medTimelineLine: {
    position: 'absolute',
    top: -14,
    bottom: -14,
    width: 2,
    zIndex: 1,
  },
  medTimelineDot: {
    width: 10,
    height: 10,
    borderRadius: 5,
    zIndex: 2,
  },
  medContentColumn: {
    flex: 1,
  },
  medDateText: {
    fontSize: 11,
    color: '#64748B', // Date & time: #64748B
    fontWeight: '500',
    marginBottom: 3,
  },
  medNameText: {
    fontSize: 14,
    color: '#0F172A', // Medication name: #0F172A
    fontWeight: 'bold',
  },
  medRightColumn: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
  },
  medTimePill: {
    paddingHorizontal: 7,
    paddingVertical: 2.5,
    borderRadius: 12,
  },
  medTimeText: {
    fontSize: 10,
    fontWeight: 'bold',
  },
  medActionsRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  medActionBtn: {
    padding: 4,
  },
  recentActionEditBtn: {
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: '#F1F5F9',
    justifyContent: 'center',
    alignItems: 'center',
  },
  recentActionDeleteBtn: {
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: '#FEF2F2',
    justifyContent: 'center',
    alignItems: 'center',
  },
  medDivider: {
    height: 1,
    backgroundColor: '#EDF1F5', // Divider: #EDF1F5
    marginLeft: 32,
  },
  timelineItemRow: {
    flexDirection: 'row',
    position: 'relative',
  },
  timelineLeftColumn: {
    width: 26,
    alignItems: 'center',
    justifyContent: 'center',
    position: 'relative',
  },
  timelineVerticalLine: {
    position: 'absolute',
    top: 0,
    bottom: 0,
    width: 2,
    backgroundColor: '#1E1E20',
  },
  timelineDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    borderWidth: 2,
    borderColor: '#000000',
    zIndex: 2,
  },
  timelineCard: {
    flex: 1,
    backgroundColor: '#0F172A',
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#1E1E20',
    padding: 12,
    marginRight: 4,
    marginBottom: 8,
    ...theme.shadows.subtle,
  },
  cardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 6,
  },
  badgeContainer: {
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: 6,
  },
  badgeText: {
    fontSize: 9.5,
    fontWeight: 'bold',
  },
  rowActions: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  circularIconBtn: {
    width: 26,
    height: 26,
    borderRadius: 13,
    borderWidth: 1,
    borderColor: '#1E1E20',
    backgroundColor: '#000000',
    justifyContent: 'center',
    alignItems: 'center',
  },
  timestampText: {
    fontSize: 11,
    color: '#64748B',
    marginBottom: 6,
  },
  detailRow: {
    marginTop: 6,
    paddingTop: 6,
    borderTopWidth: 1,
    borderTopColor: '#1E1E20',
  },
  detailTitle: {
    fontSize: 12.5,
    color: '#FFFFFF',
  },
  detailSubtitle: {
    fontSize: 11,
    color: '#94A3B8',
    marginTop: 2,
  },
  previouslyTriggeredText: {
    fontSize: 10,
    color: '#F78325',
    marginTop: 4,
    fontWeight: '700',
  },
  triggerBtn: {
    backgroundColor: '#1E1E20',
    borderColor: 'transparent',
    borderWidth: 1,
    borderRadius: 8,
    paddingHorizontal: 10,
    paddingVertical: 5,
  },
  triggerBtnActive: {
    backgroundColor: 'rgba(247, 131, 37, 0.15)',
    borderColor: '#F78325',
  },
  triggerBtnText: {
    color: '#F78325',
    fontSize: 10.5,
    fontWeight: 'bold',
  },
  triggerBtnTextActive: {
    color: '#FFFFFF',
  },
  gapContainer: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 4,
    marginRight: 4,
    marginBottom: 8,
  },
  gapConnectorLine: {
    flex: 1,
    height: 1.5,
    backgroundColor: '#1E1E20',
  },
  gapBadge: {
    backgroundColor: '#1E1E20',
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: 4,
    marginHorizontal: 8,
  },
  gapText: {
    color: '#64748B',
    fontSize: 9.5,
    fontWeight: 'bold',
  },
  loadMoreBtn: {
    backgroundColor: '#0F172A',
    borderWidth: 1.5,
    borderColor: '#1E1E20',
    borderRadius: 12,
    paddingVertical: 12,
    alignItems: 'center',
    justifyContent: 'center',
    marginVertical: 14,
    ...theme.shadows.subtle,
  },
  loadMoreBtnText: {
    fontSize: 11.5,
    fontWeight: 'bold',
  },
  zeroState: {
    paddingVertical: 32,
    alignItems: 'center',
    justifyContent: 'center',
  },
  zeroStateText: {
    color: '#64748B',
    fontSize: 12,
  },
  modalCancel: {
    marginRight: 8,
  },
  modalBadgeContainer: {
    alignSelf: 'flex-start',
    marginBottom: 12,
  },
  modalInput: {
    backgroundColor: '#000000',
    borderWidth: 1.5,
    borderColor: '#1E1E20',
    borderRadius: 8,
    paddingHorizontal: 12,
    paddingVertical: 10,
    color: '#FFFFFF',
    fontSize: 14,
    fontWeight: '600',
    marginBottom: 8,
  },
  inputLabel: {
    fontSize: 11,
    fontWeight: 'bold',
    color: '#64748B',
    marginBottom: 6,
  },
  adjusterGrid: {
    gap: 10,
    marginTop: 8,
  },
  adjusterRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 8,
  },
  adjusterLabel: {
    fontSize: 11,
    fontWeight: 'bold',
    color: '#64748B',
    width: 48,
  },
  adjustBtn: {
    flex: 1,
    height: 34,
    borderRadius: 8,
    backgroundColor: '#0F172A',
    borderWidth: 1,
    borderColor: '#1E1E20',
    alignItems: 'center',
    justifyContent: 'center',
  },
  adjustBtnText: {
    color: '#FFFFFF',
    fontSize: 11.5,
    fontWeight: 'bold',
  },
  errorText: {
    color: '#EF4444',
    fontSize: 11,
    marginBottom: 8,
    fontWeight: '600',
  },
  medDetailLabel: {
    fontSize: 12,
    color: '#64748B',
    marginTop: 2,
    fontWeight: '500',
  },
  triggerBtnCompact: {
    backgroundColor: '#F8FAFC',
    borderColor: '#E2E8F0',
    borderWidth: 1,
    borderRadius: 6,
    paddingHorizontal: 8,
    paddingVertical: 3.5,
  },
  triggerBtnCompactActive: {
    backgroundColor: 'rgba(247, 131, 37, 0.12)',
    borderColor: '#F78325',
  },
  triggerBtnCompactText: {
    color: '#64748B',
    fontSize: 10,
    fontWeight: 'bold',
  },
  triggerBtnCompactTextActive: {
    color: '#F78325',
  },
  previouslyTriggeredCompactText: {
    fontSize: 9.5,
    color: '#F78325',
    fontWeight: '700',
    marginLeft: 6,
  },
});
