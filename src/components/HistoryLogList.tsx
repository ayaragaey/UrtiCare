import React, { useState } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Modal, TextInput, Platform, ScrollView } from 'react-native';
import { useTrackerStore } from '../store/useTrackerStore';
import { LogEntry, EntryType } from '../types/tracker.types';
import { getTimeGap, MONTH_NAMES } from '../utils/dateHelpers';
import { parseISO, format, addMinutes, subMinutes, addHours, subHours, addDays, subDays } from 'date-fns';

export default function HistoryLogList() {
  const entries = useTrackerStore(state => state.entries);
  const deleteEntry = useTrackerStore(state => state.deleteEntry);
  const updateEntry = useTrackerStore(state => state.updateEntry);
  const setCollisionWarning = useTrackerStore(state => state.setCollisionWarning);
  
  const [activeCategory, setActiveCategory] = useState<EntryType | 'ALL'>('ALL');
  
  // States for retroactive editing modal
  const [editingEntry, setEditingEntry] = useState<LogEntry | null>(null);
  const [editTimestamp, setEditTimestamp] = useState<string>('');
  const [customError, setCustomError] = useState<string | null>(null);

  // Filter and pick last 4 entries
  const filteredEntries = activeCategory === 'ALL' 
    ? entries 
    : entries.filter(e => e.type === activeCategory);

  const displayEntries = filteredEntries.slice(0, 4);

  // Format Date for premium visual hierarchy
  const formatEntryDate = (isoString: string) => {
    try {
      const date = parseISO(isoString);
      // Format as "May 28, 2026 at 18:23"
      const month = MONTH_NAMES[date.getMonth()].substring(0, 3);
      const day = date.getDate();
      const year = date.getFullYear();
      const hrs = String(date.getHours()).padStart(2, '0');
      const mins = String(date.getMinutes()).padStart(2, '0');
      return `${month} ${day}, ${year} at ${hrs}:${mins}`;
    } catch {
      return isoString;
    }
  };

  // Open Edit Modal
  const startEdit = (entry: LogEntry) => {
    setEditingEntry(entry);
    setEditTimestamp(entry.timestamp);
    setCustomError(null);
  };

  // Alter timestamp and run validation
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
    } catch (e) {
      setCustomError('Invalid date operations');
    }
  };

  // Save Retroactive Edit
  const saveEdit = (force = false) => {
    if (!editingEntry) return;

    try {
      // Validate string integrity
      parseISO(editTimestamp);
    } catch {
      setCustomError('Timestamp format is corrupted. Must be ISO 8601.');
      return;
    }

    const result = updateEntry(editingEntry.id, editTimestamp, force);

    if (result.collision) {
      // Trigger warning dialog in the store (auto-merges if approved)
      setCollisionWarning({
        type: editingEntry.type,
        timestamp: editTimestamp,
        isEdit: true,
        entryId: editingEntry.id
      });
      setEditingEntry(null); // Close modal
    } else {
      setEditingEntry(null); // Close modal on success
    }
  };

  const getPillTheme = (type: EntryType) => {
    switch (type) {
      case 'FLARE_UP':
        return { bg: '#F3EBF5', label: 'Flare Up', dot: '#814b92', textColor: '#814b92' };
      case 'ANTIHISTAMINE':
        return { bg: '#E8F2F5', label: 'Antihistamine', dot: '#1b8097', textColor: '#1b8097' };
      case 'CORTISONE':
        return { bg: '#EDF5EB', label: 'Cortisone', dot: '#509729', textColor: '#509729' };
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.headerTitle}>Activity & Logs</Text>
      
      {/* Category Tab Selector */}
      <View style={styles.tabBar}>
        {(['ALL', 'FLARE_UP', 'ANTIHISTAMINE', 'CORTISONE'] as const).map(cat => (
          <TouchableOpacity
            key={cat}
            style={[
              styles.tab,
              activeCategory === cat && styles.activeTab,
              cat === 'FLARE_UP' && activeCategory === cat && styles.activeFlareTab,
              cat === 'ANTIHISTAMINE' && activeCategory === cat && styles.activeAHTab,
              cat === 'CORTISONE' && activeCategory === cat && styles.activeCortisoneTab
            ]}
            onPress={() => setActiveCategory(cat)}
          >
            <Text style={[
              styles.tabText,
              activeCategory === cat && styles.activeTabText
            ]}>
              {cat === 'ALL' ? 'All' : cat === 'FLARE_UP' ? 'Flares' : cat === 'ANTIHISTAMINE' ? 'AH' : 'Steroid'}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      {/* Logs Render Container */}
      <View style={styles.listContainer}>
        {displayEntries.length === 0 ? (
          <View style={styles.zeroState}>
            <Text style={styles.zeroStateText}>No entries found for this tracking window</Text>
          </View>
        ) : (
          displayEntries.map((item, index) => {
            const pill = getPillTheme(item.type);
            
            // Calculate chronological time gap to next older entry in the filtered view
            let timeGap = '';
            const nextOlder = filteredEntries[filteredEntries.indexOf(item) + 1];
            if (nextOlder) {
              timeGap = getTimeGap(item.timestamp, nextOlder.timestamp);
            }

            return (
              <View key={item.id} style={styles.logRowContainer}>
                <View style={styles.logCard}>
                  {/* Left Metadata Indicators */}
                  <View style={styles.cardHeader}>
                    <View style={styles.badgeContainer}>
                      <View style={[styles.badgeDot, { backgroundColor: pill.dot }]} />
                      <Text style={[styles.badgeLabel, { color: pill.textColor }]}>{pill.label}</Text>
                    </View>

                    {/* Actions panel */}
                    <View style={styles.rowActions}>
                      <TouchableOpacity onPress={() => startEdit(item)} style={styles.actionBtn}>
                        <Text style={styles.editActionText}>Edit</Text>
                      </TouchableOpacity>
                      <TouchableOpacity onPress={() => deleteEntry(item.id)} style={styles.actionBtn}>
                        <Text style={styles.deleteActionText}>Clear</Text>
                      </TouchableOpacity>
                    </View>
                  </View>

                  <Text style={styles.timestampText}>{formatEntryDate(item.timestamp)}</Text>
                </View>

                {/* Inline Chronological Gap Connector */}
                {timeGap !== '' && (
                  <View style={styles.gapContainer}>
                    <View style={styles.gapConnectorLine} />
                    <View style={styles.gapBadge}>
                      <Text style={styles.gapText}>{timeGap}</Text>
                    </View>
                    <View style={styles.gapConnectorLine} />
                  </View>
                )}
              </View>
            );
          })
        )}
      </View>

      {/* Edit Modal (Time Picker retrospectively altering) */}
      <Modal
        visible={editingEntry !== null}
        transparent={true}
        animationType="fade"
        onRequestClose={() => setEditingEntry(null)}
      >
        <View style={styles.modalBackdrop}>
          <View style={styles.modalContent}>
            <Text style={styles.modalTitle}>Retroactive Log Editor</Text>
            {editingEntry && (
              <View style={styles.modalBadge}>
                <Text style={{ color: getPillTheme(editingEntry.type).textColor, fontWeight: '700', fontSize: 12 }}>
                  Altering: {getPillTheme(editingEntry.type).label}
                </Text>
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
              placeholderTextColor="#888"
            />
            {customError && <Text style={styles.errorText}>{customError}</Text>}

            {/* Micro-adjusters for premium tactile interaction (eliminates spinners) */}
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

            <View style={styles.modalActionRow}>
              <TouchableOpacity onPress={() => setEditingEntry(null)} style={[styles.modalBtn, styles.cancelBtn]}>
                <Text style={styles.cancelBtnText}>Cancel</Text>
              </TouchableOpacity>
              <TouchableOpacity onPress={() => saveEdit(false)} style={[styles.modalBtn, styles.saveBtn]}>
                <Text style={styles.saveBtnText}>Save</Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    marginHorizontal: 16,
    marginVertical: 12,
  },
  headerTitle: {
    color: '#111111',
    fontSize: 16,
    fontWeight: '700',
    marginBottom: 12,
    letterSpacing: 0.5,
  },
  tabBar: {
    flexDirection: 'row',
    backgroundColor: '#F3F3F3',
    borderRadius: 8,
    padding: 3,
    marginBottom: 16,
    borderWidth: 1,
    borderColor: '#E2E2E2',
  },
  tab: {
    flex: 1,
    paddingVertical: 8,
    borderRadius: 6,
    alignItems: 'center',
    justifyContent: 'center',
  },
  activeTab: {
    backgroundColor: '#FFFFFF',
    ...Platform.select({
      ios: {
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.05,
        shadowRadius: 3,
      },
      android: {
        elevation: 2,
      },
      web: {
        boxShadow: '0 2px 5px rgba(0,0,0,0.05)',
      }
    })
  },
  activeFlareTab: {
    backgroundColor: 'rgba(255, 179, 179, 0.25)',
    borderWidth: 1,
    borderColor: 'rgba(255, 179, 179, 0.5)',
  },
  activeAHTab: {
    backgroundColor: 'rgba(153, 221, 255, 0.25)',
    borderWidth: 1,
    borderColor: 'rgba(153, 221, 255, 0.5)',
  },
  activeCortisoneTab: {
    backgroundColor: 'rgba(255, 255, 179, 0.25)',
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 179, 0.5)',
  },
  tabText: {
    color: '#666666',
    fontSize: 11,
    fontWeight: '700',
  },
  activeTabText: {
    color: '#111111',
  },
  listContainer: {
    minHeight: 120,
  },
  zeroState: {
    backgroundColor: '#F9F9F9',
    borderWidth: 1,
    borderColor: '#EEEEEE',
    borderRadius: 12,
    paddingVertical: 36,
    paddingHorizontal: 16,
    alignItems: 'center',
    justifyContent: 'center',
  },
  zeroStateText: {
    color: '#777777',
    fontSize: 13,
    fontWeight: '500',
    textAlign: 'center',
  },
  logRowContainer: {
    marginBottom: 0,
  },
  logCard: {
    backgroundColor: '#FFFFFF',
    borderWidth: 1,
    borderColor: '#EAEAEA',
    borderRadius: 12,
    padding: 14,
    marginVertical: 4,
    ...Platform.select({
      ios: {
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.03,
        shadowRadius: 4,
      },
      android: {
        elevation: 1,
      },
      web: {
        boxShadow: '0 2px 6px rgba(0,0,0,0.02)',
      }
    })
  },
  cardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 6,
  },
  badgeContainer: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  badgeDot: {
    width: 6,
    height: 6,
    borderRadius: 3,
    marginRight: 6,
  },
  badgeLabel: {
    fontSize: 11,
    fontWeight: '700',
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  },
  rowActions: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  actionBtn: {
    paddingVertical: 4,
    paddingHorizontal: 8,
    marginLeft: 6,
  },
  editActionText: {
    color: '#007AFF', // High-contrast utility blue
    fontSize: 11,
    fontWeight: '600',
  },
  deleteActionText: {
    color: '#D9534F', // Solid contrast warning red
    fontSize: 11,
    fontWeight: '600',
  },
  timestampText: {
    color: '#333333',
    fontSize: 13,
    fontWeight: '600',
  },
  gapContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    marginVertical: 4,
  },
  gapConnectorLine: {
    flex: 1,
    height: 1,
    backgroundColor: '#EAEAEA',
  },
  gapBadge: {
    backgroundColor: '#F0F0F0',
    borderWidth: 1,
    borderColor: '#E2E2E2',
    paddingHorizontal: 10,
    paddingVertical: 3,
    borderRadius: 10,
    marginHorizontal: 8,
  },
  gapText: {
    color: '#555555',
    fontSize: 10,
    fontWeight: '800',
    letterSpacing: 0.2,
  },
  
  // Modal Overlays
  modalBackdrop: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    justifyContent: 'center',
    alignItems: 'center',
    padding: 24,
  },
  modalContent: {
    width: '100%',
    maxWidth: 360,
    backgroundColor: '#FFFFFF',
    borderWidth: 1,
    borderColor: '#E2E2E2',
    borderRadius: 16,
    padding: 20,
    ...Platform.select({
      ios: {
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 10 },
        shadowOpacity: 0.15,
        shadowRadius: 15,
      },
      web: {
        boxShadow: '0 10px 30px rgba(0,0,0,0.1)'
      }
    })
  },
  modalTitle: {
    color: '#111111',
    fontSize: 16,
    fontWeight: '700',
    marginBottom: 4,
    textAlign: 'center',
  },
  modalBadge: {
    backgroundColor: '#F3F3F3',
    paddingVertical: 4,
    paddingHorizontal: 12,
    borderRadius: 8,
    alignSelf: 'center',
    marginBottom: 16,
  },
  inputLabel: {
    color: '#555555',
    fontSize: 12,
    fontWeight: '600',
    marginBottom: 6,
  },
  modalInput: {
    backgroundColor: '#FAFAFA',
    borderWidth: 1,
    borderColor: '#D0D0D0',
    borderRadius: 8,
    paddingHorizontal: 12,
    paddingVertical: 10,
    color: '#111111',
    fontSize: 13,
    fontWeight: '600',
    marginBottom: 8,
  },
  errorText: {
    color: '#D9534F',
    fontSize: 11,
    fontWeight: '500',
    marginBottom: 8,
  },
  adjusterGrid: {
    backgroundColor: '#F9F9F9',
    borderWidth: 1,
    borderColor: '#E5E5E5',
    borderRadius: 8,
    padding: 8,
    marginBottom: 20,
  },
  adjusterRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 4,
  },
  adjusterLabel: {
    color: '#444444',
    fontSize: 11,
    fontWeight: '700',
    width: 44,
  },
  adjustBtn: {
    backgroundColor: '#EAEAEA',
    paddingVertical: 6,
    paddingHorizontal: 12,
    borderRadius: 6,
    minWidth: 54,
    alignItems: 'center',
  },
  adjustBtnText: {
    color: '#333333',
    fontSize: 11,
    fontWeight: '700',
  },
  modalActionRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  modalBtn: {
    flex: 1,
    paddingVertical: 12,
    borderRadius: 8,
    alignItems: 'center',
    justifyContent: 'center',
  },
  cancelBtn: {
    backgroundColor: '#EAEAEA',
    marginRight: 8,
  },
  saveBtn: {
    backgroundColor: '#007AFF',
    marginLeft: 8,
  },
  cancelBtnText: {
    color: '#555555',
    fontSize: 12,
    fontWeight: '700',
  },
  saveBtnText: {
    color: '#FFFFFF',
    fontSize: 12,
    fontWeight: '700',
  }
});
