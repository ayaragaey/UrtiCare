import React, { useState } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Platform, ScrollView } from 'react-native';
import { useTrackerStore } from '../store/useTrackerStore';
import { theme } from '../styles/theme';
import { IconClose, IconPlus, IconChevronRight, IconCapsule, IconTablet } from './common/CustomIcons';
import MedicationEntryModal from './MedicationEntryModal';
import { parseISO, format } from 'date-fns';
import { LogEntry } from '../types/tracker.types';

// Simple custom clock icon drawn with views
const MiniClockIcon = ({ color = '#64748B', size = 13 }) => (
  <View style={{ width: size, height: size, borderRadius: size / 2, borderWidth: 1.2, borderColor: color, justifyContent: 'center', alignItems: 'center' }}>
    <View style={{ width: 1.2, height: size * 0.35, backgroundColor: color, position: 'absolute', top: size * 0.16 }} />
    <View style={{ width: size * 0.25, height: 1.2, backgroundColor: color, position: 'absolute', left: size * 0.44, top: size * 0.44 }} />
  </View>
);

// Simple custom calendar icon drawn with views
const MiniCalendarIcon = ({ color = '#64748B', size = 13 }) => (
  <View style={{ width: size, height: size, borderWidth: 1.2, borderColor: color, borderRadius: 2, padding: 1, justifyContent: 'space-between', position: 'relative' }}>
    <View style={{ flexDirection: 'row', justifyContent: 'space-around', position: 'absolute', top: -3, left: 0, right: 0 }}>
      <View style={{ width: 1.2, height: 3, backgroundColor: color }} />
      <View style={{ width: 1.2, height: 3, backgroundColor: color }} />
    </View>
    <View style={{ height: 1, backgroundColor: color, width: '100%', marginTop: 2 }} />
    <View style={{ flexDirection: 'row', justifyContent: 'space-around', width: '100%', marginBottom: 1 }}>
      <View style={{ width: 1.5, height: 1.5, backgroundColor: color, borderRadius: 0.75 }} />
      <View style={{ width: 1.5, height: 1.5, backgroundColor: color, borderRadius: 0.75 }} />
      <View style={{ width: 1.5, height: 1.5, backgroundColor: color, borderRadius: 0.75 }} />
    </View>
  </View>
);

// Custom vertical three dots icon
const ThreeDotsIcon = ({ color = '#64748B', size = 16 }) => (
  <View style={{ width: size, height: size, justifyContent: 'center', alignItems: 'center', gap: 2.5 }}>
    <View style={{ width: 3.5, height: 3.5, borderRadius: 1.75, backgroundColor: color }} />
    <View style={{ width: 3.5, height: 3.5, borderRadius: 1.75, backgroundColor: color }} />
    <View style={{ width: 3.5, height: 3.5, borderRadius: 1.75, backgroundColor: color }} />
  </View>
);

// Helper function to format timestamp into friendly relative date-time format
const getFriendlyRelativeDateTime = (timestampStr: string) => {
  try {
    const d = parseISO(timestampStr);
    const now = new Date();
    
    // Format time as hh:mm a
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

// Helper function to parse medication name and dose from standard entry titles
const parseNameAndDose = (fullName: string) => {
  const match = fullName.match(/^(.*?)\s*\((.*?)\)$/);
  if (match) {
    return { name: match[1].trim(), dose: match[2].trim() };
  }
  return { name: fullName.trim(), dose: '' };
};

// Map medication to its category, emoji, and parsed name/dose
const getMedicationDetails = (type: string, fullName: string) => {
  const { name, dose } = parseNameAndDose(fullName);
  const upperName = name.toUpperCase();
  
  let category = 'Antihistamine';
  let isAntihistamine = true;
  
  if (upperName.includes('XOLAIR')) {
    category = 'Biological Treatment';
    isAntihistamine = false;
  } else if (upperName.includes('DEXAZONE') || type === 'CORTISONE' || upperName.includes('PREDNISOLONE')) {
    category = 'Corticosteroid';
    isAntihistamine = false;
  }
  
  return { name, dose, category, isAntihistamine };
};

// Calculate friendly relative day count in Xd ago format
const getRelativeTimeText = (timestampStr: string) => {
  try {
    const d = parseISO(timestampStr);
    const now = new Date();
    
    // Clear time parts to compare calendar dates
    const dDate = new Date(d.getFullYear(), d.getMonth(), d.getDate());
    const nowDate = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    
    const diffTime = nowDate.getTime() - dDate.getTime();
    const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24));
    
    if (diffDays === 0) {
      return 'Today';
    } else if (diffDays > 0) {
      return `${diffDays}d ago`;
    }
    return '';
  } catch {
    return '';
  }
};

interface RecentMedicationLogProps {
  onViewHistory?: () => void;
}

export default function RecentMedicationLog({ onViewHistory }: RecentMedicationLogProps) {
  const entries = useTrackerStore(state => state.entries);
  const deleteEntry = useTrackerStore(state => state.deleteEntry);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [isCollapsed, setIsCollapsed] = useState(false);
  const [activeMenuId, setActiveMenuId] = useState<string | null>(null);

  const getEntryTitle = (entry: LogEntry) => {
    if (entry.itemName) {
      return entry.itemName + (entry.notes ? ` (${entry.notes})` : '');
    }
    switch (entry.type) {
      case 'ANTIHISTAMINE':
        return 'Antihistamine' + (entry.notes ? ` (${entry.notes})` : '');
      case 'CORTISONE':
        return 'Corticosteroid' + (entry.notes ? ` (${entry.notes})` : '');
      default:
        return 'Medication Dose' + (entry.notes ? ` (${entry.notes})` : '');
    }
  };

  // Filter for pills only (Antihistamines and Cortisone) - limit to last 4 entries
  const pillEntries = entries.filter(e => e.type === 'ANTIHISTAMINE' || e.type === 'CORTISONE').slice(0, 4);

  const displayList = pillEntries.map(e => ({
    id: e.id,
    type: e.type,
    name: getEntryTitle(e),
    timestamp: e.timestamp,
    isReal: true,
  }));

  // Get latest entry dynamically for header summary card
  const latestEntry = displayList[0];
  const lastTakenText = latestEntry ? getFriendlyRelativeDateTime(latestEntry.timestamp) : '';

  const handleEdit = (id: string) => {
    setEditingId(id);
    setModalVisible(true);
  };

  const handleDelete = (id: string, isReal: boolean) => {
    if (isReal) {
      deleteEntry(id);
    }
  };

  // Get styles based on treatment type
  const getCardThemeColors = (type: string, name: string) => {
    const upperName = name.toUpperCase();
    if (upperName.includes('XOLAIR')) {
      return {
        bg: '#FBFBFF', // Clean very soft purple
        borderColor: '#F2ECF5',
        accentColor: '#814B92', // UrtiCare Purple
      };
    } else if (upperName.includes('DEXAZONE') || type === 'CORTISONE' || upperName.includes('PREDNISOLONE')) {
      return {
        bg: '#F8FCFD', // Clean very soft teal
        borderColor: '#E6F2F4',
        accentColor: '#1A7E97', // UrtiCare Teal
      };
    } else {
      return {
        bg: '#F7FAF8', // Clean very soft green
        borderColor: '#ECF2EC',
        accentColor: '#509729', // UrtiCare Green
      };
    }
  };

  return (
    <View style={styles.outerContainer}>
      <View style={styles.cardContainer}>
        {/* Top Drag Handle */}
        <View style={styles.dragHandle} />

        {/* Header Row */}
        <View style={styles.headerRow}>
          <View style={styles.headerTitleContainer}>
            <View style={styles.headerTitleRow}>
              {/* Removed pill icon from next to the title */}
              <Text style={styles.containerTitle}>
                <Text style={styles.containerTitlePurple}>Recent Medication </Text>
                <Text style={styles.containerTitleGreen}>Intake</Text>
              </Text>
            </View>
          </View>
          
          <TouchableOpacity
            style={styles.closeBtn}
            onPress={() => setIsCollapsed(!isCollapsed)}
            activeOpacity={0.7}
          >
            {isCollapsed ? (
              <IconPlus color="#64748B" size={14} />
            ) : (
              <IconClose color="#64748B" size={14} />
            )}
          </TouchableOpacity>
        </View>

        {!isCollapsed && (
          <View style={styles.bodyContent}>
            {/* Last Taken Summary Card (dynamic, soft green background) */}
            {lastTakenText ? (
              <View style={styles.lastTakenCard}>
                <MiniClockIcon color="#509729" size={14} />
                <Text style={styles.lastTakenLabel}>
                  Last taken: <Text style={styles.lastTakenValue}>{lastTakenText}</Text>
                </Text>
              </View>
            ) : null}

            {displayList.length === 0 ? (
              /* Empty State View */
              <View style={styles.emptyStateContainer}>
                <Text style={styles.emptyStateEmoji}>💊</Text>
                <Text style={styles.emptyStateTitle}>No medication intake yet</Text>
                <Text style={styles.emptyStateSubtitle}>
                  Your medication history will appear here once you start logging.
                </Text>
              </View>
            ) : (
              /* Timeline ScrollView */
              <ScrollView
                style={styles.scrollArea}
                contentContainerStyle={styles.scrollContent}
                nestedScrollEnabled={true}
              >
                {displayList.map((item, idx) => {
                  const isFirst = idx === 0;
                  const isLast = idx === displayList.length - 1;

                  // Parse date & time parts
                  let dateStr = '';
                  let timeStr = '';
                  try {
                    const d = parseISO(item.timestamp);
                    dateStr = format(d, 'MMM dd, yyyy');
                    const hour = d.getHours();
                    const minute = d.getMinutes();
                    const ampm = hour >= 12 ? 'PM' : 'AM';
                    const displayHour = String(hour % 12 === 0 ? 12 : hour % 12).padStart(2, '0');
                    const displayMinute = String(minute).padStart(2, '0');
                    timeStr = `${displayHour}:${displayMinute} ${ampm}`;
                  } catch {
                    dateStr = '';
                    timeStr = '';
                  }

                  // Timeline connector line layout styling (starts at center of first, stops at center of last)
                  const lineStyle = displayList.length === 1 
                    ? { display: 'none' } 
                    : (isFirst 
                        ? { top: '50%', bottom: 0 } 
                        : (isLast 
                            ? { top: 0, bottom: '50%' } 
                            : { top: 0, bottom: 0 }));

                  const details = getMedicationDetails(item.type, item.name);
                  const cardTheme = getCardThemeColors(item.type, item.name);
                  const relativeTime = getRelativeTimeText(item.timestamp);

                  return (
                    <View key={item.id} style={styles.timelineRow}>
                      {/* Left side: vertical line & green timeline dot centered vertically */}
                      <View style={styles.timelineColumn}>
                        {lineStyle.display !== 'none' && (
                          <View style={[styles.timelineLine, lineStyle as any]} />
                        )}
                        <View style={styles.timelineDot} />
                      </View>

                      {/* Right side: medication card */}
                      <View style={[styles.timelineCard, { backgroundColor: cardTheme.bg, borderColor: cardTheme.borderColor }]}>
                        <View style={styles.cardRow}>
                          {/* Content Column */}
                          <View style={styles.cardContentCol}>
                            {/* Row 1: Name & Dose badge (Left) + relative status text (Right) */}
                            <View style={styles.nameAndDoseRow}>
                              <View style={styles.nameAndDoseLeft}>
                                <Text style={styles.medicationName} numberOfLines={1} ellipsizeMode="tail">
                                  {details.name}
                                </Text>
                                {details.dose ? (
                                  <View style={styles.doseBadge}>
                                    <Text style={styles.doseBadgeText}>{details.dose}</Text>
                                  </View>
                                ) : null}
                              </View>
                              
                              {relativeTime ? (
                                <Text style={styles.relativeStatusText} numberOfLines={1}>
                                  {relativeTime}
                                </Text>
                              ) : null}
                            </View>

                            {/* Row 2: Category Badge */}
                            <View style={styles.categoryBadgeRow}>
                              <View style={styles.categoryBadge}>
                                <Text style={styles.categoryBadgeText} numberOfLines={1}>
                                  {details.category}
                                </Text>
                              </View>
                            </View>

                            {/* Row 3: Date & Time Metadata using emoji icons */}
                            <View style={styles.dateTimeContainer}>
                              <Text style={styles.dateTimeText} numberOfLines={1}>
                                📅 {dateStr}    ⏰ {timeStr}
                              </Text>
                            </View>
                          </View>

                          {/* Absolute Actions Trigger (Top-Right) */}
                          <View style={styles.menuContainer}>
                            <TouchableOpacity
                              style={styles.menuTriggerBtn}
                              onPress={() => setActiveMenuId(activeMenuId === item.id ? null : item.id)}
                              activeOpacity={0.7}
                            >
                              <ThreeDotsIcon color="#64748B" size={18} />
                            </TouchableOpacity>

                            {activeMenuId === item.id && (
                              <View style={styles.dropdownMenu}>
                                <TouchableOpacity
                                  style={styles.dropdownItem}
                                  onPress={() => {
                                    setActiveMenuId(null);
                                    handleEdit(item.id);
                                  }}
                                  activeOpacity={0.6}
                                >
                                  <Text style={styles.dropdownItemText}>✏️ Edit</Text>
                                </TouchableOpacity>
                                <View style={styles.dropdownDivider} />
                                <TouchableOpacity
                                  style={[styles.dropdownItem, styles.dropdownItemDelete]}
                                  onPress={() => {
                                    setActiveMenuId(null);
                                    handleDelete(item.id, item.isReal);
                                  }}
                                  activeOpacity={0.6}
                                >
                                  <Text style={[styles.dropdownItemText, styles.deleteText]}>🗑️ Delete</Text>
                                </TouchableOpacity>
                              </View>
                            )}
                          </View>
                        </View>
                      </View>
                    </View>
                  );
                })}
              </ScrollView>
            )}

            {/* Bottom Subtle Navigation action (View full medication history) */}
            {onViewHistory && (
              <TouchableOpacity
                style={styles.historyLinkContainer}
                onPress={onViewHistory}
                activeOpacity={0.7}
              >
                <View style={styles.historyLinkLeft}>
                  <MiniCalendarIcon color="#509729" size={20} />
                  <View style={styles.historyTextContainer}>
                    <Text style={styles.historyLinkText}>View full medication history</Text>
                    <Text style={styles.historySubtitle}>See all your medications and intake details</Text>
                  </View>
                </View>
                <IconChevronRight color="#64748B" size={16} />
              </TouchableOpacity>
            )}
          </View>
        )}
      </View>

      {/* Medication Entry / Edit Modal */}
      <MedicationEntryModal
        visible={modalVisible}
        onClose={() => setModalVisible(false)}
        editEntryId={editingId}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  outerContainer: {
    marginHorizontal: 16,
    marginVertical: 12,
    marginBottom: 40,
  },
  cardContainer: {
    backgroundColor: '#FFFFFF',
    borderTopLeftRadius: 28,
    borderTopRightRadius: 28,
    borderBottomLeftRadius: 24,
    borderBottomRightRadius: 24,
    paddingHorizontal: 20,
    paddingTop: 12,
    paddingBottom: 20,
    borderWidth: 1,
    borderColor: '#F1F5F9',
    ...Platform.select({
      ios: {
        shadowColor: '#000000',
        shadowOffset: { width: 0, height: -6 },
        shadowOpacity: 0.05,
        shadowRadius: 16,
      },
      android: {
        elevation: 4,
      },
      web: {
        boxShadow: '0 -6px 30px rgba(0, 0, 0, 0.04), 0 8px 30px rgba(0, 0, 0, 0.06)',
      } as any,
    }),
  },
  dragHandle: {
    width: 36,
    height: 4,
    backgroundColor: '#E2E8F0',
    borderRadius: 2,
    alignSelf: 'center',
    marginBottom: 16,
  },
  headerRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 16,
  },
  headerTitleContainer: {
    flexDirection: 'column',
    flex: 1,
  },
  headerTitleRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  containerTitle: {
    fontSize: 20,
    fontWeight: '700',
  },
  containerTitlePurple: {
    color: '#814B92', // UrtiCare Purple
  },
  containerTitleGreen: {
    color: '#509729', // UrtiCare Green
  },
  closeBtn: {
    width: 28,
    height: 28,
    borderRadius: 14,
    backgroundColor: '#F1F5F9',
    justifyContent: 'center',
    alignItems: 'center',
  },
  bodyContent: {
    gap: 12,
  },
  lastTakenCard: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#F4FBF7', // Very soft green background from visual reference
    borderRadius: 12,
    paddingVertical: 8,
    paddingHorizontal: 12,
    marginBottom: 8,
    alignSelf: 'flex-start',
    gap: 6,
  },
  lastTakenLabel: {
    fontSize: 13,
    color: '#475569',
    fontWeight: '500',
  },
  lastTakenValue: {
    fontWeight: '700',
    color: '#509729', // Bold green time value
  },
  scrollArea: {
    maxHeight: 380,
  },
  scrollContent: {
    paddingBottom: 4,
  },
  emptyStateContainer: {
    paddingVertical: 40,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
  },
  emptyStateEmoji: {
    fontSize: 32,
    marginBottom: 4,
  },
  emptyStateTitle: {
    fontSize: 16,
    fontWeight: '700',
    color: '#334155',
  },
  emptyStateSubtitle: {
    fontSize: 13,
    color: '#64748B',
    textAlign: 'center',
    paddingHorizontal: 20,
    lineHeight: 18,
  },
  timelineRow: {
    flexDirection: 'row',
    alignItems: 'center',
    minHeight: 90,
  },
  timelineColumn: {
    width: 24,
    alignSelf: 'stretch',
    alignItems: 'center',
    justifyContent: 'center',
    position: 'relative',
  },
  timelineLine: {
    position: 'absolute',
    width: 2,
    backgroundColor: '#E2E8F0', // Grey timeline connector line
    left: 11,
    zIndex: 1,
  },
  timelineDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: '#509729', // Green timeline dot
    zIndex: 2,
  },
  timelineCard: {
    flex: 1,
    borderRadius: 20, // 20px rounded corners
    borderWidth: 1,
    marginVertical: 6,
    marginLeft: 8,
    padding: 16,
    position: 'relative',
  },
  cardRow: {
    flexDirection: 'row',
    alignItems: 'center',
    width: '100%',
  },
  iconCircle: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: '#E6F4EA', // Soft green background
    justifyContent: 'center',
    alignItems: 'center',
  },
  cardContentCol: {
    flex: 1,
    paddingRight: 32, // Leave space for absolute top-right action button
    flexDirection: 'column',
    gap: 4,
  },
  nameAndDoseRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    width: '100%',
  },
  nameAndDoseLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    flexShrink: 1,
  },
  medicationName: {
    fontSize: 16,
    fontWeight: '700',
    color: '#0F172A',
    flexShrink: 1,
  },
  doseBadge: {
    backgroundColor: '#ECFDF5',
    borderRadius: 6,
    paddingHorizontal: 8,
    paddingVertical: 2,
    marginLeft: 8,
  },
  doseBadgeText: {
    fontSize: 11,
    fontWeight: '700',
    color: '#509729', // Green dosage text
  },
  relativeStatusText: {
    fontSize: 12,
    fontWeight: '700',
    color: '#509729', // Green status indicator
  },
  categoryBadgeRow: {
    flexDirection: 'row',
  },
  categoryBadge: {
    backgroundColor: '#F5F3FF',
    borderRadius: 6,
    paddingHorizontal: 8,
    paddingVertical: 2,
  },
  categoryBadgeText: {
    fontSize: 11,
    fontWeight: '700',
    color: '#814B92', // Purple category text
  },
  dateTimeContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 4,
  },
  dateTimeText: {
    fontSize: 12,
    color: '#64748B', // Grey metadata text
    fontWeight: '500',
  },
  menuContainer: {
    position: 'absolute',
    top: 16,
    right: 16,
    zIndex: 50,
  },
  menuTriggerBtn: {
    width: 36,
    height: 36,
    borderRadius: 18,
    borderWidth: 1,
    borderColor: '#E2E8F0',
    backgroundColor: '#FFFFFF',
    justifyContent: 'center',
    alignItems: 'center',
  },
  dropdownMenu: {
    position: 'absolute',
    right: 0,
    top: 38,
    backgroundColor: '#FFFFFF',
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#E2E8F0',
    paddingVertical: 4,
    width: 90,
    zIndex: 999,
    ...Platform.select({
      ios: {
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.1,
        shadowRadius: 4,
      },
      android: {
        elevation: 5,
      },
      web: {
        boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
      } as any,
    }),
  },
  dropdownItem: {
    paddingHorizontal: 12,
    paddingVertical: 8,
  },
  dropdownItemText: {
    fontSize: 12,
    fontWeight: '600',
    color: '#334155',
  },
  deleteText: {
    color: '#EF4444',
  },
  dropdownDivider: {
    height: 1,
    backgroundColor: '#F1F5F9',
  },
  dropdownItemDelete: {},

  historyLinkContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: 16,
    borderWidth: 1,
    borderColor: '#509729', // UrtiCare Green border
    borderRadius: 18,
    backgroundColor: '#FFFFFF',
    marginTop: 12,
  },
  historyLinkLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    flex: 1,
  },
  historyTextContainer: {
    flexDirection: 'column',
    gap: 2,
  },
  historyLinkText: {
    fontSize: 14,
    fontWeight: '700',
    color: '#509729', // UrtiCare Green
  },
  historySubtitle: {
    fontSize: 11,
    color: '#64748B',
    fontWeight: '500',
  },
});
