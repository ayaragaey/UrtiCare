import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  TextInput,
  Platform,
  SafeAreaView
} from 'react-native';
import { useTrackerStore } from '../../store/useTrackerStore';
import { theme } from '../../styles/theme';
import {
  IconBackArrow,
  IconSearch,
  IconChevronRight,
  IconPlus
} from '../common/CustomIcons';

// Categories Definition
const CATEGORIES = [
  {
    id: 'Food',
    name: 'Food',
    description: 'Meals, snacks, fruits and more',
    icon: '🍎',
    color: '#FFF7ED', // Soft pastel orange
    accent: '#F97316',
  },
  {
    id: 'Drinks',
    name: 'Drinks',
    description: 'Coffee, tea, juices and more',
    icon: '☕',
    color: '#F0F9FF', // Soft pastel light blue
    accent: '#0EA5E9',
  },
  {
    id: 'Medications',
    name: 'Medications',
    description: 'Medicines you take',
    icon: '💊',
    color: '#ECFDF5', // Soft pastel green
    accent: '#10B981',
  },
  {
    id: 'Supplements',
    name: 'Supplements',
    description: 'Vitamins and other supplements',
    icon: '🧬',
    color: '#F5F3FF', // Soft pastel purple
    accent: '#8B5CF6',
  },
  {
    id: 'Other',
    name: 'Other',
    description: 'Anything else',
    icon: '📦',
    color: '#F3F4F6', // Soft pastel grey
    accent: '#6B7280',
  }
] as const;

// Static Items list
const PRESET_ITEMS = [
  // Food
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
  // Drinks
  { name: 'Coffee', category: 'Drinks' },
  { name: 'Morning Coffee', category: 'Drinks' },
  { name: 'Milk', category: 'Drinks' },
  { name: 'Orange Juice', category: 'Drinks' },
  { name: 'Green Tea', category: 'Drinks' },
  { name: 'Soda', category: 'Drinks' },
  { name: 'Alcohol', category: 'Drinks' },
  { name: 'Water', category: 'Drinks' },
  // Medications
  { name: 'Paracetamol', category: 'Medications' },
  { name: 'Antihistamine', category: 'Medications' },
  { name: 'Ibuprofen', category: 'Medications' },
  { name: 'Cortisone', category: 'Medications' },
  { name: 'Cetirizine', category: 'Medications' },
  // Supplements
  { name: 'Vitamin D', category: 'Supplements' },
  { name: 'Vitamin C', category: 'Supplements' },
  { name: 'Fish Oil', category: 'Supplements' },
  { name: 'Zinc', category: 'Supplements' },
  { name: 'Magnesium', category: 'Supplements' },
  // Other
  { name: 'Tobacco', category: 'Other' },
  { name: 'Perfume', category: 'Other' },
  { name: 'Hair Dye', category: 'Other' },
  { name: 'Latex', category: 'Other' },
];

interface Props {
  onBack: () => void;
  prefilledItemName?: string;
  prefilledCategory?: string;
  prefilledTimestamp?: string;
}

type ScreenMode = 'main' | 'category_list' | 'see_all' | 'details';

export default function AddConsumptionScreen({ onBack, prefilledItemName, prefilledCategory, prefilledTimestamp }: Props) {
  // Store connection
  const recentlyUsed = useTrackerStore(state => state.recentlyUsedConsumptions);
  const favorites = useTrackerStore(state => state.favoriteConsumptions);
  const addFavorite = useTrackerStore(state => state.addFavoriteConsumption);
  const removeFavorite = useTrackerStore(state => state.removeFavoriteConsumption);
  const addConsumptionEntry = useTrackerStore(state => state.addConsumptionEntry);

  const [mode, setMode] = useState<ScreenMode>(prefilledItemName ? 'details' : 'main');
  
  // Navigation history to support back stack preservation
  const [history, setHistory] = useState<ScreenMode[]>(prefilledItemName ? ['main'] : []);
  
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<typeof CATEGORIES[number] | null>(null);
  const [seeAllType, setSeeAllType] = useState<'recents' | 'favorites' | null>(null);
  
  // Details form fields
  const [selectedItemName, setSelectedItemName] = useState(prefilledItemName || '');
  const [selectedItemCategory, setSelectedItemCategory] = useState<string>(prefilledCategory || 'Other');
  const [amount, setAmount] = useState('');
  const [notes, setNotes] = useState('');
  const [isFavorite, setIsFavorite] = useState(prefilledItemName ? favorites.includes(prefilledItemName) : false);
  const [status, setStatus] = useState<'Logged' | 'Trigger'>('Logged');

  const navigateTo = (newMode: ScreenMode) => {
    setHistory(prev => [...prev, mode]);
    setMode(newMode);
  };

  const handleBack = () => {
    if (history.length > 0) {
      const prevMode = history[history.length - 1];
      setHistory(prev => prev.slice(0, -1));
      setMode(prevMode);
    } else {
      onBack();
    }
  };

  const handleSelectItem = (name: string, category: string) => {
    setSelectedItemName(name);
    setSelectedItemCategory(category);
    setAmount('');
    setNotes('');
    setIsFavorite(favorites.includes(name));
    navigateTo('details');
  };

  const handleLogConsumption = () => {
    const timestamp = prefilledTimestamp || new Date().toISOString();
    
    // Log in store
    addConsumptionEntry(selectedItemName, selectedItemCategory, amount, notes, timestamp, false, status);

    // Save favorite state if user toggled it
    if (isFavorite) {
      addFavorite(selectedItemName);
    } else {
      removeFavorite(selectedItemName);
    }

    // Go back to the main logs list view
    onBack();
  };

  // Filter items based on search
  const searchFilteredItems = PRESET_ITEMS.filter(item =>
    item.name.toLowerCase().includes(searchQuery.toLowerCase())
  );

  // Render header
  const renderHeader = (title: string) => (
    <View style={styles.headerContainer}>
      <TouchableOpacity style={styles.backButton} onPress={handleBack} activeOpacity={0.7}>
        <IconBackArrow size={18} />
      </TouchableOpacity>
      <Text style={styles.headerTitle}>{title}</Text>
      <View style={styles.headerPlaceholder} />
    </View>
  );

  return (
    <SafeAreaView style={styles.safeArea}>
      {/* 1. MAIN SCREEN */}
      {mode === 'main' && (
        <View style={styles.container}>
          {renderHeader('Add Consumption')}
          
          <ScrollView
            style={styles.scrollArea}
            contentContainerStyle={styles.scrollContent}
            showsVerticalScrollIndicator={false}
            keyboardShouldPersistTaps="handled"
          >
            {/* Search input field */}
            <View style={styles.searchSection}>
              <View style={styles.searchBarWrapper}>
                <IconSearch color={theme.colors.neutralGrey} size={18} style={styles.searchIcon} />
                <TextInput
                  style={styles.searchInput}
                  value={searchQuery}
                  onChangeText={setSearchQuery}
                  placeholder="Search for what you consumed"
                  placeholderTextColor="#94A3B8"
                />
              </View>
            </View>

            {searchQuery.length > 0 ? (
              /* Search results */
              <View style={styles.searchResultsContainer}>
                <Text style={styles.sectionTitle}>Search Results</Text>
                {searchFilteredItems.length > 0 ? (
                  searchFilteredItems.map((item, idx) => (
                    <TouchableOpacity
                      key={idx}
                      style={styles.resultItem}
                      onPress={() => handleSelectItem(item.name, item.category)}
                    >
                      <View>
                        <Text style={styles.resultItemName}>{item.name}</Text>
                        <Text style={styles.resultItemCategory}>{item.category}</Text>
                      </View>
                      <IconChevronRight color={theme.colors.neutralGrey} size={16} />
                    </TouchableOpacity>
                  ))
                ) : (
                  /* Custom item option if not found */
                  <TouchableOpacity
                    style={styles.customAddButton}
                    onPress={() => handleSelectItem(searchQuery, 'Other')}
                  >
                    <IconPlus color="#F78325" size={16} />
                    <Text style={styles.customAddText}>Log Custom item: "{searchQuery}"</Text>
                  </TouchableOpacity>
                )}
              </View>
            ) : (
              /* Standard views */
              <>
                {/* Recently Used */}
                <View style={styles.sectionContainer}>
                  <View style={styles.sectionHeaderRow}>
                    <Text style={styles.sectionTitle}>Recently Used</Text>
                    <TouchableOpacity
                      onPress={() => {
                        setSeeAllType('recents');
                        navigateTo('see_all');
                      }}
                    >
                      <Text style={styles.seeAllText}>See all</Text>
                    </TouchableOpacity>
                  </View>
                  <ScrollView
                    horizontal
                    showsHorizontalScrollIndicator={false}
                    contentContainerStyle={styles.horizontalScrollPadding}
                  >
                    {recentlyUsed.map((item, index) => {
                      const category = PRESET_ITEMS.find(p => p.name === item)?.category || 'Other';
                      return (
                        <TouchableOpacity
                          key={index}
                          style={styles.chip}
                          onPress={() => handleSelectItem(item, category)}
                        >
                          <Text style={styles.chipText}>{item}</Text>
                        </TouchableOpacity>
                      );
                    })}
                  </ScrollView>
                </View>

                {/* Favorites */}
                <View style={styles.sectionContainer}>
                  <View style={styles.sectionHeaderRow}>
                    <Text style={styles.sectionTitle}>Favorites</Text>
                    <TouchableOpacity
                      onPress={() => {
                        setSeeAllType('favorites');
                        navigateTo('see_all');
                      }}
                    >
                      <Text style={styles.seeAllText}>See all</Text>
                    </TouchableOpacity>
                  </View>
                  <ScrollView
                    horizontal
                    showsHorizontalScrollIndicator={false}
                    contentContainerStyle={styles.horizontalScrollPadding}
                  >
                    {favorites.map((item, index) => {
                      const category = PRESET_ITEMS.find(p => p.name === item)?.category || 'Other';
                      return (
                        <TouchableOpacity
                          key={index}
                          style={styles.chip}
                          onPress={() => handleSelectItem(item, category)}
                        >
                          <Text style={styles.chipText}>{item}</Text>
                        </TouchableOpacity>
                      );
                    })}
                  </ScrollView>
                </View>

                {/* Choose a Category */}
                <View style={styles.categoryContainer}>
                  <Text style={styles.sectionTitleMargin}>Choose a Category</Text>
                  {CATEGORIES.map(category => (
                    <TouchableOpacity
                      key={category.id}
                      style={styles.categoryCard}
                      onPress={() => {
                        setSelectedCategory(category);
                        navigateTo('category_list');
                      }}
                    >
                      <View
                        style={[
                          styles.categoryIconCircle,
                          { backgroundColor: category.color }
                        ]}
                      >
                        <Text style={styles.categoryEmoji}>{category.icon}</Text>
                      </View>
                      <View style={styles.categoryInfo}>
                        <Text style={styles.categoryCardName}>{category.name}</Text>
                        <Text style={styles.categoryCardDesc}>{category.description}</Text>
                      </View>
                      <IconChevronRight color={theme.colors.neutralGrey} size={18} />
                    </TouchableOpacity>
                  ))}
                </View>
              </>
            )}
          </ScrollView>
        </View>
      )}

      {/* 2. CATEGORY ITEM LIST */}
      {mode === 'category_list' && selectedCategory && (
        <View style={styles.container}>
          {renderHeader(selectedCategory.name)}
          <ScrollView style={styles.scrollArea} contentContainerStyle={styles.scrollContent}>
            <Text style={styles.listSubtitle}>Select an item in {selectedCategory.name} to log:</Text>
            <View style={styles.listContainer}>
              {PRESET_ITEMS.filter(item => item.category === selectedCategory.id).map(
                (item, index) => (
                  <TouchableOpacity
                    key={index}
                    style={styles.listItem}
                    onPress={() => handleSelectItem(item.name, item.category)}
                  >
                    <Text style={styles.listItemText}>{item.name}</Text>
                    <IconChevronRight color={theme.colors.neutralGrey} size={16} />
                  </TouchableOpacity>
                )
              )}
            </View>
          </ScrollView>
        </View>
      )}

      {/* 3. SEE ALL LIST */}
      {mode === 'see_all' && seeAllType && (
        <View style={styles.container}>
          {renderHeader(seeAllType === 'recents' ? 'Recently Used' : 'Favorites')}
          <ScrollView style={styles.scrollArea} contentContainerStyle={styles.scrollContent}>
            <Text style={styles.listSubtitle}>
              Tapping an item opens the log details screen.
            </Text>
            <View style={styles.listContainer}>
              {(seeAllType === 'recents' ? recentlyUsed : favorites).map((item, index) => {
                const category = PRESET_ITEMS.find(p => p.name === item)?.category || 'Other';
                return (
                  <TouchableOpacity
                    key={index}
                    style={styles.listItem}
                    onPress={() => handleSelectItem(item, category)}
                  >
                    <View style={styles.listItemRow}>
                      <Text style={styles.listItemText}>{item}</Text>
                      <Text style={styles.listItemCategoryBadge}>{category}</Text>
                    </View>
                    <IconChevronRight color={theme.colors.neutralGrey} size={16} />
                  </TouchableOpacity>
                );
              })}
            </View>
          </ScrollView>
        </View>
      )}

      {/* 4. DETAILS FORM */}
      {mode === 'details' && (
        <View style={styles.container}>
          {renderHeader('Log Details')}
          <ScrollView style={styles.scrollArea} contentContainerStyle={styles.scrollContent}>
            <View style={styles.detailsCard}>
              <View style={styles.detailsHeader}>
                <View>
                  <Text style={styles.detailsItemName}>{selectedItemName}</Text>
                  <Text style={styles.detailsItemCategory}>{selectedItemCategory}</Text>
                </View>
                {/* Favorite Toggle button */}
                <TouchableOpacity
                  style={[styles.favBtn, isFavorite && styles.favBtnActive]}
                  onPress={() => setIsFavorite(!isFavorite)}
                >
                  <Text style={styles.favBtnText}>{isFavorite ? '★ Favorite' : '☆ Add Fav'}</Text>
                </TouchableOpacity>
              </View>

              <View style={styles.formSpacer} />

              <Text style={styles.formLabel}>Amount Consumed</Text>
              <TextInput
                style={styles.formInput}
                value={amount}
                onChangeText={setAmount}
                placeholder="e.g. 1 cup, 250ml, 1 pill, 50g"
                placeholderTextColor="#94A3B8"
              />

              <Text style={styles.formLabel}>Notes (Optional)</Text>
              <TextInput
                style={[styles.formInput, styles.formInputNotes]}
                value={notes}
                onChangeText={setNotes}
                placeholder="e.g. with breakfast, post-workout"
                placeholderTextColor="#94A3B8"
                multiline
                numberOfLines={3}
              />

              <Text style={styles.formLabel}>Status</Text>
              <View style={styles.statusContainer}>
                <TouchableOpacity
                  style={[
                    styles.statusBtn,
                    status === 'Logged' && styles.statusBtnLoggedActive
                  ]}
                  onPress={() => setStatus('Logged')}
                >
                  <Text style={[
                    styles.statusBtnText,
                    status === 'Logged' && styles.statusBtnTextActive
                  ]}>Logged</Text>
                </TouchableOpacity>
                <TouchableOpacity
                  style={[
                    styles.statusBtn,
                    status === 'Trigger' && styles.statusBtnTriggerActive
                  ]}
                  onPress={() => setStatus('Trigger')}
                >
                  <Text style={[
                    styles.statusBtnText,
                    status === 'Trigger' && styles.statusBtnTextActive
                  ]}>Trigger</Text>
                </TouchableOpacity>
              </View>

              <TouchableOpacity
                style={styles.logSubmitBtn}
                onPress={handleLogConsumption}
                activeOpacity={0.8}
              >
                <Text style={styles.logSubmitBtnText}>Log Consumption</Text>
              </TouchableOpacity>
            </View>
          </ScrollView>
        </View>
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: '#FFFFFF',
  },
  container: {
    flex: 1,
    backgroundColor: '#FFFFFF',
  },
  headerContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#F1F5F9',
    backgroundColor: '#FFFFFF',
    ...Platform.select({
      ios: {
        paddingTop: 8,
      }
    })
  },
  backButton: {
    width: 36,
    height: 36,
    borderRadius: 18,
    borderWidth: 1,
    borderColor: '#E2E8F0',
    backgroundColor: '#FFFFFF',
    justifyContent: 'center',
    alignItems: 'center',
  },
  headerTitle: {
    fontSize: 18,
    fontWeight: theme.typography.weight.heavy,
    color: theme.colors.textDark,
  },
  headerPlaceholder: {
    width: 36,
  },
  scrollArea: {
    flex: 1,
  },
  scrollContent: {
    paddingBottom: 40,
  },
  searchSection: {
    paddingHorizontal: 16,
    paddingVertical: 12,
  },
  searchBarWrapper: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#F8FAFC',
    borderRadius: theme.borderRadius.full,
    borderWidth: 1.5,
    borderColor: '#E2E8F0',
    paddingHorizontal: 14,
    height: 48,
  },
  searchIcon: {
    marginRight: 8,
  },
  searchInput: {
    flex: 1,
    fontSize: 14,
    color: theme.colors.textDark,
    fontWeight: theme.typography.weight.medium,
  },
  sectionContainer: {
    marginVertical: 12,
  },
  sectionHeaderRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 16,
    marginBottom: 8,
  },
  sectionTitle: {
    fontSize: 14,
    fontWeight: theme.typography.weight.heavy,
    color: theme.colors.textDark,
  },
  sectionTitleMargin: {
    fontSize: 14,
    fontWeight: theme.typography.weight.heavy,
    color: theme.colors.textDark,
    paddingHorizontal: 16,
    marginBottom: 12,
  },
  seeAllText: {
    color: '#F78325', // Main interactions color: orange
    fontSize: 13,
    fontWeight: theme.typography.weight.bold,
  },
  horizontalScrollPadding: {
    paddingHorizontal: 12,
    gap: 8,
  },
  chip: {
    backgroundColor: '#FFFFFF',
    borderWidth: 1.2,
    borderColor: '#E2E8F0',
    borderRadius: 20,
    paddingHorizontal: 14,
    paddingVertical: 8,
  },
  chipText: {
    fontSize: 13,
    fontWeight: theme.typography.weight.semibold,
    color: theme.colors.textDark,
  },
  categoryContainer: {
    marginVertical: 16,
  },
  categoryCard: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#FFFFFF',
    borderWidth: 1.2,
    borderColor: '#E2E8F0',
    borderRadius: theme.borderRadius.large,
    padding: 14,
    marginHorizontal: 16,
    marginBottom: 10,
    ...theme.shadows.subtle,
  },
  categoryIconCircle: {
    width: 44,
    height: 44,
    borderRadius: 22,
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 14,
  },
  categoryEmoji: {
    fontSize: 20,
  },
  categoryInfo: {
    flex: 1,
  },
  categoryCardName: {
    fontSize: 14,
    fontWeight: theme.typography.weight.heavy,
    color: theme.colors.textDark,
    marginBottom: 2,
  },
  categoryCardDesc: {
    fontSize: 12,
    color: theme.colors.textMuted,
  },
  listSubtitle: {
    fontSize: 13,
    color: theme.colors.textMuted,
    paddingHorizontal: 16,
    marginTop: 12,
    marginBottom: 8,
  },
  listContainer: {
    paddingHorizontal: 16,
    marginTop: 8,
  },
  listItem: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 14,
    borderBottomWidth: 1,
    borderBottomColor: '#F1F5F9',
  },
  listItemRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
  },
  listItemText: {
    fontSize: 14,
    fontWeight: theme.typography.weight.bold,
    color: theme.colors.textDark,
  },
  listItemCategoryBadge: {
    fontSize: 10,
    fontWeight: theme.typography.weight.semibold,
    color: '#F78325',
    backgroundColor: '#FFEAD2',
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 4,
  },
  detailsCard: {
    margin: 16,
    padding: 20,
    backgroundColor: '#FFFFFF',
    borderWidth: 1.2,
    borderColor: '#E2E8F0',
    borderRadius: theme.borderRadius.large,
    ...theme.shadows.subtle,
  },
  detailsHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
  },
  detailsItemName: {
    fontSize: 18,
    fontWeight: theme.typography.weight.heavy,
    color: theme.colors.textDark,
  },
  detailsItemCategory: {
    fontSize: 12,
    color: '#F78325',
    fontWeight: theme.typography.weight.semibold,
    marginTop: 2,
  },
  favBtn: {
    borderWidth: 1.2,
    borderColor: '#E2E8F0',
    borderRadius: theme.borderRadius.full,
    paddingHorizontal: 10,
    paddingVertical: 6,
    backgroundColor: '#FFFFFF',
  },
  favBtnActive: {
    borderColor: '#F59E0B',
    backgroundColor: '#FEF3C7',
  },
  favBtnText: {
    fontSize: 11,
    fontWeight: theme.typography.weight.bold,
    color: theme.colors.textDark,
  },
  formSpacer: {
    height: 16,
  },
  formLabel: {
    fontSize: 12,
    fontWeight: theme.typography.weight.semibold,
    color: theme.colors.textDark,
    marginTop: 14,
    marginBottom: 6,
  },
  formInput: {
    backgroundColor: '#F8FAFC',
    borderWidth: 1.2,
    borderColor: '#E2E8F0',
    borderRadius: theme.borderRadius.medium,
    paddingHorizontal: 12,
    paddingVertical: 10,
    fontSize: 14,
    color: theme.colors.textDark,
  },
  formInputNotes: {
    minHeight: 60,
    textAlignVertical: 'top',
  },
  logSubmitBtn: {
    backgroundColor: '#F78325', // Adapted Orange Accent Color
    borderRadius: theme.borderRadius.medium,
    paddingVertical: 14,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 24,
    ...theme.shadows.medium,
  },
  logSubmitBtnText: {
    color: '#FFFFFF',
    fontWeight: theme.typography.weight.heavy,
    fontSize: 14,
  },
  statusContainer: {
    flexDirection: 'row',
    gap: 12,
    marginTop: 6,
  },
  statusBtn: {
    flex: 1,
    paddingVertical: 12,
    borderRadius: theme.borderRadius.medium,
    borderWidth: 1.5,
    borderColor: '#E2E8F0',
    backgroundColor: '#F8FAFC',
    alignItems: 'center',
    justifyContent: 'center',
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
    fontSize: 14,
    fontWeight: theme.typography.weight.bold,
    color: theme.colors.textMuted,
  },
  statusBtnTextActive: {
    color: theme.colors.textDark,
  },
  searchResultsContainer: {
    paddingHorizontal: 16,
    marginTop: 8,
  },
  resultItem: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#F1F5F9',
  },
  resultItemName: {
    fontSize: 14,
    fontWeight: theme.typography.weight.bold,
    color: theme.colors.textDark,
  },
  resultItemCategory: {
    fontSize: 11,
    color: theme.colors.textMuted,
    marginTop: 2,
  },
  customAddButton: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#FFEAD2',
    borderRadius: theme.borderRadius.medium,
    borderWidth: 1.5,
    borderStyle: 'dashed',
    borderColor: '#F78325',
    padding: 14,
    marginTop: 16,
    justifyContent: 'center',
    gap: 8,
  },
  customAddText: {
    fontSize: 13,
    color: '#F78325',
    fontWeight: theme.typography.weight.bold,
  }
});
