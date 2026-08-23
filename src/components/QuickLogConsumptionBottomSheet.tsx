import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
  TextInput,
  Modal,
  Animated,
  Dimensions,
  Alert
} from 'react-native';
import { useTrackerStore } from '../store/useTrackerStore';
import { theme } from '../styles/theme';

interface Props {
  visible: boolean;
  onClose: () => void;
  onLogged: (entryIds: string[], previousRecentlyUsed: string[], message: string) => void;
  onAddMoreDetails: (itemName: string, category: string) => void;
}

const PRESET_ITEMS = [
  { name: 'Spicy Food', category: 'Food' },
  { name: 'Seafood', category: 'Food' },
  { name: 'Tomato', category: 'Food' },
  { name: 'Nuts', category: 'Food' },
  { name: 'Egg', category: 'Food' },
  { name: 'Apple', category: 'Food' },
  { name: 'Cheese', category: 'Food' },
  { name: 'Dark Chocolate', category: 'Food' },
  { name: 'Pizza', category: 'Food' },
  { name: 'Banana', category: 'Food' },
  { name: 'Coffee', category: 'Drinks' },
  { name: 'Morning Coffee', category: 'Drinks' },
  { name: 'Milk', category: 'Drinks' },
  { name: 'Orange Juice', category: 'Drinks' },
  { name: 'Green Tea', category: 'Drinks' },
  { name: 'Soda', category: 'Drinks' },
  { name: 'Alcohol', category: 'Drinks' },
  { name: 'Water', category: 'Drinks' },
  { name: 'Paracetamol', category: 'Medications' },
  { name: 'Antihistamine', category: 'Medications' },
  { name: 'Ibuprofen', category: 'Medications' },
  { name: 'Cortisone', category: 'Medications' },
  { name: 'Cetirizine', category: 'Medications' },
  { name: 'Vitamin D', category: 'Supplements' },
  { name: 'Vitamin C', category: 'Supplements' },
  { name: 'Fish Oil', category: 'Supplements' },
  { name: 'Zinc', category: 'Supplements' },
  { name: 'Magnesium', category: 'Supplements' },
  { name: 'Tobacco', category: 'Other' },
  { name: 'Perfume', category: 'Other' },
  { name: 'Hair Dye', category: 'Other' },
  { name: 'Latex', category: 'Other' },
];

function splitAmount(amount: string): { quantity: string; unit: string } {
  if (!amount) return { quantity: '', unit: '' };
  const match = amount.match(/^(\d+(?:\.\d+)?)\s*(.*)$/);
  if (match) {
    return { quantity: match[1], unit: match[2] };
  }
  return { quantity: amount, unit: '' };
}

export default function QuickLogConsumptionBottomSheet({
  visible,
  onClose,
  onLogged,
  onAddMoreDetails
}: Props) {
  const store = useTrackerStore();
  const [selectedItems, setSelectedItems] = useState<Record<string, {
    quantity: string;
    unit: string;
    status: 'Logged' | 'Trigger';
    category: string;
  }>>({});
  const [expandedItem, setExpandedItem] = useState<string | null>(null);
  
  const [slideAnim] = useState(new Animated.Value(Dimensions.get('window').height));
  const [fadeAnim] = useState(new Animated.Value(0));

  useEffect(() => {
    if (visible) {
      // Reset state
      setSelectedItems({});
      setExpandedItem(null);
      
      Animated.parallel([
        Animated.timing(fadeAnim, {
          toValue: 0.5,
          duration: 300,
          useNativeDriver: true,
        }),
        Animated.timing(slideAnim, {
          toValue: 0,
          duration: 350,
          useNativeDriver: true,
        })
      ]).start();
    } else {
      Animated.parallel([
        Animated.timing(fadeAnim, {
          toValue: 0,
          duration: 250,
          useNativeDriver: true,
        }),
        Animated.timing(slideAnim, {
          toValue: Dimensions.get('window').height,
          duration: 300,
          useNativeDriver: true,
        })
      ]).start();
    }
  }, [visible]);

  // Load defaults for a selected item
  const handleToggleSelectItem = (name: string) => {
    if (selectedItems[name]) {
      const copy = { ...selectedItems };
      delete copy[name];
      setSelectedItems(copy);
      if (expandedItem === name) {
        setExpandedItem(null);
      }
    } else {
      const preset = PRESET_ITEMS.find(p => p.name === name);
      const category = preset ? preset.category : 'Other';

      // Find last logged item to get previous amount defaults
      const lastEntry = store.entries.find(
        e => e.type === 'CONSUMPTION' && e.itemName === name
      );

      let initialQty = '';
      let initialUnit = '';

      if (lastEntry && lastEntry.amount) {
        const parts = splitAmount(lastEntry.amount);
        initialQty = parts.quantity;
        initialUnit = parts.unit;
      } else {
        // Basic defaults
        if (name.toLowerCase().includes('coffee')) {
          initialQty = '1';
          initialUnit = 'cup';
        } else if (name.toLowerCase().includes('milk')) {
          initialQty = '1';
          initialUnit = 'cup';
        } else if (name.toLowerCase().includes('chocolate')) {
          initialQty = '1';
          initialUnit = 'piece';
        } else if (category === 'Medications') {
          // Medication starts blank (do not suggest dosages)
          initialQty = '';
          initialUnit = '';
        } else {
          initialQty = '1';
          initialUnit = 'portion';
        }
      }

      setSelectedItems({
        ...selectedItems,
        [name]: {
          quantity: initialQty,
          unit: initialUnit,
          status: 'Logged',
          category
        }
      });

      // Require medication confirmation -> expand by default
      if (category === 'Medications') {
        setExpandedItem(name);
      }
    }
  };

  const handleUpdateItemField = (name: string, field: 'quantity' | 'unit' | 'status', value: string) => {
    setSelectedItems({
      ...selectedItems,
      [name]: {
        ...selectedItems[name],
        [field]: value
      }
    });
  };

  // Check if any medication is missing dosage confirmation
  const isFormValid = () => {
    for (const name in selectedItems) {
      const item = selectedItems[name];
      if (item.category === 'Medications' && (!item.quantity || !item.unit)) {
        return false;
      }
    }
    return true;
  };

  const handleLogSelected = () => {
    const selectedKeys = Object.keys(selectedItems);
    if (selectedKeys.length === 0) return;

    // Check duplicate: within last 5 minutes
    const nowMs = Date.now();
    const duplicateItem = selectedKeys.find(name => {
      return store.entries.some(e => {
        if (e.type !== 'CONSUMPTION' || e.itemName !== name) return false;
        const entryTime = new Date(e.timestamp).getTime();
        return Math.abs(nowMs - entryTime) < 5 * 60 * 1000; // 5 mins
      });
    });

    if (duplicateItem) {
      Alert.alert(
        'Log Again?',
        `You logged ${duplicateItem} recently. Log it again?`,
        [
          { text: 'Cancel', style: 'cancel' },
          { text: 'Log Again', onPress: () => proceedLogging(selectedKeys) }
        ]
      );
    } else {
      proceedLogging(selectedKeys);
    }
  };

  const proceedLogging = (keys: string[]) => {
    const previousRecentlyUsed = [...store.recentlyUsedConsumptions];
    const timestamp = new Date().toISOString();
    const createdIds: string[] = [];

    keys.forEach(name => {
      const item = selectedItems[name];
      const finalAmount = item.quantity ? (item.unit ? `${item.quantity} ${item.unit}` : item.quantity) : item.unit;
      
      // Perform log logic directly via store action
      const id = `CONSUMPTION_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
      
      // Add entry to entries
      const newEntry = {
        id,
        timestamp,
        type: 'CONSUMPTION' as const,
        itemName: name,
        category: item.category,
        amount: finalAmount,
        notes: '',
        status: item.status
      };

      // update recents list
      let updatedRecents = [...store.recentlyUsedConsumptions];
      updatedRecents = updatedRecents.filter(r => r !== name);
      updatedRecents.unshift(name);
      if (updatedRecents.length > 8) {
        updatedRecents = updatedRecents.slice(0, 8);
      }

      store.entries.push(newEntry);
      store.entries.sort((a, b) => b.timestamp.localeCompare(a.timestamp));
      
      // update state
      store.recentlyUsedConsumptions = updatedRecents;
      createdIds.push(id);
    });

    // Notify state updates to trigger re-renders
    store.setCollisionWarning(null);

    // Close and show success snackbar
    onClose();

    const successMsg = keys.length === 1
      ? `${keys[0]} logged successfully`
      : `${keys.length} consumptions logged successfully`;
    
    onLogged(createdIds, previousRecentlyUsed, successMsg);
  };

  const selectedCount = Object.keys(selectedItems).length;

  return (
    <Modal visible={visible} transparent animationType="none" onRequestClose={onClose}>
      <View style={styles.overlay}>
        {/* Animated backdrop */}
        <Animated.View 
          style={[styles.backdrop, { opacity: fadeAnim }]}
          onStartShouldSetResponder={() => true}
          onResponderRelease={onClose}
        />

        {/* Sliding Panel */}
        <Animated.View style={[styles.panel, { transform: [{ translateY: slideAnim }] }]}>
          <View style={styles.handle} />

          <View style={styles.header}>
            <Text style={styles.title}>Quick Log Consumption</Text>
            <Text style={styles.subtitle}>Select what you consumed</Text>
          </View>

          <ScrollView style={styles.scrollArea} showsVerticalScrollIndicator={false}>
            {/* Recently Used Section */}
            <Text style={styles.sectionHeader}>RECENTLY USED</Text>
            <View style={styles.listContainer}>
              {store.recentlyUsedConsumptions.map(name => {
                const isSelected = !!selectedItems[name];
                const isExpanded = expandedItem === name;
                const item = selectedItems[name];
                
                return (
                  <View key={`recent-${name}`} style={styles.rowWrapper}>
                    <TouchableOpacity
                      style={[styles.itemRow, isSelected && styles.itemRowSelected]}
                      onPress={() => handleToggleSelectItem(name)}
                      activeOpacity={0.7}
                    >
                      <Text style={[styles.itemName, isSelected && styles.itemNameSelected]}>
                        {name}
                      </Text>
                      <View style={[styles.indicator, isSelected && styles.indicatorSelected]}>
                        {isSelected && <View style={styles.indicatorInner} />}
                      </View>
                    </TouchableOpacity>

                    {isSelected && (
                      <TouchableOpacity 
                        style={styles.expandTrigger} 
                        onPress={() => setExpandedItem(isExpanded ? null : name)}
                      >
                        <Text style={styles.expandTriggerText}>
                          {isExpanded ? 'Collapse Edit' : 'Quick Edit ⚙️'}
                        </Text>
                      </TouchableOpacity>
                    )}

                    {isSelected && isExpanded && (
                      <View style={styles.compactEditor}>
                        {item.category === 'Medications' && (
                          <Text style={styles.medWarning}>
                            ⚠️ Please confirm medication dosage & unit. We do not suggest default medication amounts.
                          </Text>
                        )}
                        <View style={styles.editorRow}>
                          <View style={{ flex: 1 }}>
                            <Text style={styles.editorLabel}>Quantity</Text>
                            <TextInput
                              style={[styles.editorInput, item.category === 'Medications' && !item.quantity && styles.editorInputError]}
                              value={item.quantity}
                              onChangeText={text => handleUpdateItemField(name, 'quantity', text)}
                              placeholder="e.g. 1, 250"
                              placeholderTextColor="#94A3B8"
                            />
                          </View>
                          <View style={{ flex: 1, marginLeft: 12 }}>
                            <Text style={styles.editorLabel}>Unit</Text>
                            <TextInput
                              style={[styles.editorInput, item.category === 'Medications' && !item.unit && styles.editorInputError]}
                              value={item.unit}
                              onChangeText={text => handleUpdateItemField(name, 'unit', text)}
                              placeholder="e.g. cup, ml, mg"
                              placeholderTextColor="#94A3B8"
                            />
                          </View>
                        </View>

                        <Text style={styles.editorLabel}>Status</Text>
                        <View style={styles.statusContainer}>
                          <TouchableOpacity
                            style={[
                              styles.statusBtn,
                              item.status === 'Logged' && styles.statusBtnLoggedActive
                            ]}
                            onPress={() => handleUpdateItemField(name, 'status', 'Logged')}
                          >
                            <Text style={[
                              styles.statusBtnText,
                              item.status === 'Logged' && styles.statusBtnTextActive
                            ]}>Logged</Text>
                          </TouchableOpacity>
                          <TouchableOpacity
                            style={[
                              styles.statusBtn,
                              item.status === 'Trigger' && styles.statusBtnTriggerActive
                            ]}
                            onPress={() => handleUpdateItemField(name, 'status', 'Trigger')}
                          >
                            <Text style={[
                              styles.statusBtnText,
                              item.status === 'Trigger' && styles.statusBtnTextActive
                            ]}>Trigger</Text>
                          </TouchableOpacity>
                        </View>
                      </View>
                    )}
                  </View>
                );
              })}
            </View>

            {/* Favorites Section */}
            <Text style={styles.sectionHeader}>FAVORITES</Text>
            <View style={styles.listContainer}>
              {store.favoriteConsumptions.map(name => {
                const isSelected = !!selectedItems[name];
                const isExpanded = expandedItem === name;
                const item = selectedItems[name];

                return (
                  <View key={`fav-${name}`} style={styles.rowWrapper}>
                    <TouchableOpacity
                      style={[styles.itemRow, isSelected && styles.itemRowSelected]}
                      onPress={() => handleToggleSelectItem(name)}
                      activeOpacity={0.7}
                    >
                      <Text style={[styles.itemName, isSelected && styles.itemNameSelected]}>
                        {name}
                      </Text>
                      <View style={[styles.indicator, isSelected && styles.indicatorSelected]}>
                        {isSelected && <View style={styles.indicatorInner} />}
                      </View>
                    </TouchableOpacity>

                    {isSelected && (
                      <TouchableOpacity 
                        style={styles.expandTrigger} 
                        onPress={() => setExpandedItem(isExpanded ? null : name)}
                      >
                        <Text style={styles.expandTriggerText}>
                          {isExpanded ? 'Collapse Edit' : 'Quick Edit ⚙️'}
                        </Text>
                      </TouchableOpacity>
                    )}

                    {isSelected && isExpanded && (
                      <View style={styles.compactEditor}>
                        {item.category === 'Medications' && (
                          <Text style={styles.medWarning}>
                            ⚠️ Please confirm medication dosage & unit. We do not suggest default medication amounts.
                          </Text>
                        )}
                        <View style={styles.editorRow}>
                          <View style={{ flex: 1 }}>
                            <Text style={styles.editorLabel}>Quantity</Text>
                            <TextInput
                              style={[styles.editorInput, item.category === 'Medications' && !item.quantity && styles.editorInputError]}
                              value={item.quantity}
                              onChangeText={text => handleUpdateItemField(name, 'quantity', text)}
                              placeholder="e.g. 1, 250"
                              placeholderTextColor="#94A3B8"
                            />
                          </View>
                          <View style={{ flex: 1, marginLeft: 12 }}>
                            <Text style={styles.editorLabel}>Unit</Text>
                            <TextInput
                              style={[styles.editorInput, item.category === 'Medications' && !item.unit && styles.editorInputError]}
                              value={item.unit}
                              onChangeText={text => handleUpdateItemField(name, 'unit', text)}
                              placeholder="e.g. cup, ml, mg"
                              placeholderTextColor="#94A3B8"
                            />
                          </View>
                        </View>

                        <Text style={styles.editorLabel}>Status</Text>
                        <View style={styles.statusContainer}>
                          <TouchableOpacity
                            style={[
                              styles.statusBtn,
                              item.status === 'Logged' && styles.statusBtnLoggedActive
                            ]}
                            onPress={() => handleUpdateItemField(name, 'status', 'Logged')}
                          >
                            <Text style={[
                              styles.statusBtnText,
                              item.status === 'Logged' && styles.statusBtnTextActive
                            ]}>Logged</Text>
                          </TouchableOpacity>
                          <TouchableOpacity
                            style={[
                              styles.statusBtn,
                              item.status === 'Trigger' && styles.statusBtnTriggerActive
                            ]}
                            onPress={() => handleUpdateItemField(name, 'status', 'Trigger')}
                          >
                            <Text style={[
                              styles.statusBtnText,
                              item.status === 'Trigger' && styles.statusBtnTextActive
                            ]}>Trigger</Text>
                          </TouchableOpacity>
                        </View>
                      </View>
                    )}
                  </View>
                );
              })}
            </View>

            {/* Add More Details Link */}
            <TouchableOpacity
              style={styles.detailsLink}
              onPress={() => {
                const keys = Object.keys(selectedItems);
                const firstItem = keys.length > 0 ? keys[0] : '';
                const category = firstItem ? selectedItems[firstItem].category : 'Other';
                onClose();
                onAddMoreDetails(firstItem, category);
              }}
              activeOpacity={0.7}
            >
              <Text style={styles.detailsLinkText}>Add More Details</Text>
            </TouchableOpacity>
            
            <View style={{ height: 40 }} />
          </ScrollView>

          {/* Action Button */}
          <View style={styles.footer}>
            <TouchableOpacity
              style={[
                styles.submitBtn,
                (selectedCount === 0 || !isFormValid()) && styles.submitBtnDisabled
              ]}
              disabled={selectedCount === 0 || !isFormValid()}
              onPress={handleLogSelected}
              activeOpacity={0.8}
            >
              <Text style={styles.submitBtnText}>
                {selectedCount > 0 ? `Log Selected (${selectedCount})` : 'Log Selected'}
              </Text>
            </TouchableOpacity>
          </View>
        </Animated.View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  overlay: {
    flex: 1,
    justifyContent: 'flex-end',
  },
  backdrop: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: '#000000',
  },
  panel: {
    backgroundColor: '#FFFFFF',
    borderTopLeftRadius: theme.borderRadius.xlarge,
    borderTopRightRadius: theme.borderRadius.xlarge,
    maxHeight: '85%',
    width: '100%',
    shadowColor: '#0F172A',
    shadowOffset: { width: 0, height: -4 },
    shadowOpacity: 0.1,
    shadowRadius: 10,
    elevation: 20,
  },
  handle: {
    width: 40,
    height: 5,
    borderRadius: 2.5,
    backgroundColor: '#E2E8F0',
    alignSelf: 'center',
    marginTop: 10,
  },
  header: {
    paddingHorizontal: 20,
    paddingTop: 16,
    paddingBottom: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#F1F5F9',
  },
  title: {
    fontSize: 18,
    fontWeight: theme.typography.weight.bold,
    color: theme.colors.textDark,
    textAlign: 'center',
  },
  subtitle: {
    fontSize: 13,
    color: theme.colors.textMuted,
    textAlign: 'center',
    marginTop: 3,
  },
  scrollArea: {
    paddingHorizontal: 20,
    paddingTop: 16,
  },
  sectionHeader: {
    fontSize: 11,
    fontWeight: theme.typography.weight.heavy,
    color: theme.colors.textMuted,
    letterSpacing: 0.8,
    marginBottom: 10,
  },
  listContainer: {
    marginBottom: 20,
  },
  rowWrapper: {
    borderBottomWidth: 1,
    borderBottomColor: '#F1F5F9',
    paddingVertical: 4,
  },
  itemRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 12,
  },
  itemRowSelected: {
    backgroundColor: '#FAF5FF',
    borderRadius: theme.borderRadius.medium,
    paddingHorizontal: 8,
  },
  itemName: {
    fontSize: 14.5,
    fontWeight: theme.typography.weight.medium,
    color: theme.colors.textDark,
  },
  itemNameSelected: {
    fontWeight: theme.typography.weight.bold,
    color: '#814B92',
  },
  indicator: {
    width: 20,
    height: 20,
    borderRadius: 10,
    borderWidth: 1.5,
    borderColor: '#CBD5E1',
    alignItems: 'center',
    justifyContent: 'center',
  },
  indicatorSelected: {
    borderColor: '#814B92',
  },
  indicatorInner: {
    width: 10,
    height: 10,
    borderRadius: 5,
    backgroundColor: '#814B92',
  },
  expandTrigger: {
    alignSelf: 'flex-start',
    paddingVertical: 6,
    paddingHorizontal: 8,
    marginBottom: 8,
  },
  expandTriggerText: {
    fontSize: 11.5,
    fontWeight: theme.typography.weight.bold,
    color: '#814B92',
  },
  compactEditor: {
    backgroundColor: '#F8FAFC',
    borderRadius: theme.borderRadius.medium,
    padding: 12,
    marginBottom: 12,
    borderWidth: 1,
    borderColor: '#E2E8F0',
  },
  editorRow: {
    flexDirection: 'row',
    marginBottom: 12,
  },
  editorLabel: {
    fontSize: 11.5,
    fontWeight: theme.typography.weight.bold,
    color: theme.colors.textDark,
    marginBottom: 4,
  },
  editorInput: {
    backgroundColor: '#FFFFFF',
    borderWidth: 1,
    borderColor: '#CBD5E1',
    borderRadius: theme.borderRadius.small,
    paddingHorizontal: 10,
    paddingVertical: 6,
    fontSize: 13,
    color: theme.colors.textDark,
  },
  editorInputError: {
    borderColor: '#EF4444',
    backgroundColor: '#FEF2F2',
  },
  medWarning: {
    fontSize: 11,
    color: '#EF4444',
    fontWeight: theme.typography.weight.semibold,
    lineHeight: 15,
    marginBottom: 10,
  },
  statusContainer: {
    flexDirection: 'row',
    gap: 8,
    marginTop: 4,
  },
  statusBtn: {
    flex: 1,
    paddingVertical: 8,
    borderRadius: theme.borderRadius.small,
    borderWidth: 1,
    borderColor: '#CBD5E1',
    backgroundColor: '#FFFFFF',
    alignItems: 'center',
  },
  statusBtnLoggedActive: {
    borderColor: '#509729',
    backgroundColor: '#ECFDF5',
  },
  statusBtnTriggerActive: {
    borderColor: '#F78325',
    backgroundColor: '#FFF7ED',
  },
  statusBtnText: {
    fontSize: 12,
    fontWeight: theme.typography.weight.bold,
    color: theme.colors.textMuted,
  },
  statusBtnTextActive: {
    color: theme.colors.textDark,
  },
  detailsLink: {
    alignSelf: 'center',
    paddingVertical: 12,
    marginTop: 10,
    marginBottom: 20,
  },
  detailsLinkText: {
    fontSize: 13.5,
    fontWeight: theme.typography.weight.bold,
    color: '#814B92',
    textDecorationLine: 'underline',
  },
  footer: {
    paddingHorizontal: 20,
    paddingTop: 12,
    paddingBottom: 24,
    borderTopWidth: 1,
    borderTopColor: '#F1F5F9',
    backgroundColor: '#FFFFFF',
  },
  submitBtn: {
    backgroundColor: '#814B92', // Purple primary action
    borderRadius: theme.borderRadius.medium,
    paddingVertical: 14,
    alignItems: 'center',
    justifyContent: 'center',
  },
  submitBtnDisabled: {
    backgroundColor: '#E2E8F0',
  },
  submitBtnText: {
    color: '#FFFFFF',
    fontWeight: theme.typography.weight.heavy,
    fontSize: 14,
  },
});
