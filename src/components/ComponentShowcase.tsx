import React, { useState } from 'react';
import { View, Text, StyleSheet, ScrollView, TouchableOpacity, ActivityIndicator } from 'react-native';
import { theme } from '../styles/theme';
import Button from './common/Button';
import { Input, SearchBar, Dropdown } from './common/Input';
import Badge from './common/Badge';
import Alert from './common/Alert';
import { Card, KPICard } from './common/Card';
import ProgressBar from './common/ProgressBar';
import Chart from './common/Chart';
import Table from './common/Table';
import Modal from './common/Modal';
import { IconDashboard, IconSettings } from './common/CustomIcons';

export default function ComponentShowcase() {
  const [inputText, setInputText] = useState('');
  const [searchText, setSearchText] = useState('');
  const [dropdownValue, setDropdownValue] = useState('one');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [activeSubTab, setActiveSubTab] = useState<'variants' | 'alerts' | 'states'>('variants');

  const dropdownOptions = [
    { label: 'Option A (SaaS Core)', value: 'one' },
    { label: 'Option B (Analytics Pro)', value: 'two' },
    { label: 'Option C (Custom Dev)', value: 'three' },
  ];

  // Dummy table contents
  const tableHeaders = ['User Profile', 'Subscription', 'Date Created', 'Status'];
  const tableRows = [
    [
      'Aria Sterling',
      'Premium Tier',
      '2026-07-10',
      <Badge label="Premium" type="premium" key="1" />,
    ],
    [
      'Marcus Vance',
      'Enterprise',
      '2026-07-12',
      <Badge label="In Progress" type="inProgress" key="2" />,
    ],
    [
      'Clara Oswald',
      'Free Basic',
      '2026-07-14',
      <Badge label="Completed" type="completed" key="3" />,
    ],
    [
      'Dan Stark',
      'Developer',
      '2026-07-15',
      <Badge label="Inactive" type="inactive" key="4" />,
    ],
  ];

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
      <Text style={styles.showcaseTitle}>Design System Showcase</Text>
      <Text style={styles.showcaseSubtitle}>Interactive catalogue of the reconstructed UI components</Text>

      {/* Tabs */}
      <View style={styles.tabContainer}>
        {(['variants', 'alerts', 'states'] as const).map((tab) => (
          <TouchableOpacity
            key={tab}
            style={[styles.tabButton, activeSubTab === tab && styles.tabButtonActive]}
            onPress={() => setActiveSubTab(tab)}
          >
            <Text style={[styles.tabButtonText, activeSubTab === tab && styles.tabButtonTextActive]}>
              {tab.toUpperCase()}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      {activeSubTab === 'variants' && (
        <View style={styles.gridSection}>
          {/* Section: Buttons */}
          <Card style={styles.showcaseCard}>
            <Text style={styles.sectionHeader}>1. Brand Button Styles</Text>
            <View style={styles.buttonRow}>
              <Button title="Primary Button" onPress={() => {}} variant="primary" style={styles.flexBtn} />
              <Button title="Secondary" onPress={() => {}} variant="secondary" style={styles.flexBtn} />
            </View>
            <View style={styles.buttonRow}>
              <Button title="Success Action" onPress={() => {}} variant="success" style={styles.flexBtn} />
              <Button title="Disabled" onPress={() => {}} variant="disabled" style={styles.flexBtn} />
            </View>
            <Button title="Text Button Link" onPress={() => {}} variant="text" />
          </Card>

          {/* Section: Inputs, Dropdowns, Search */}
          <Card style={styles.showcaseCard}>
            <Text style={styles.sectionHeader}>2. Search & Form Inputs</Text>
            <SearchBar value={searchText} onChangeText={setSearchText} placeholder="Search anything here..." style={styles.mb8} />
            <Input label="Email address" value={inputText} onChangeText={setInputText} placeholder="e.g. name@domain.com" style={styles.mb8} />
            <Dropdown label="Select tier level" selectedValue={dropdownValue} options={dropdownOptions} onSelect={setDropdownValue} />
          </Card>

          {/* Section: KPI Cards */}
          <View style={styles.kpiRow}>
            <KPICard title="Revenue Growth" value="$12,480" trendLabel="↑ +12.4%" isPositive={true} style={styles.kpiCardItem} />
            <KPICard title="Active Registrations" value="1,840" trendLabel="↑ +5.2%" isPositive={true} style={styles.kpiCardItem} />
          </View>

          {/* Section: Progress & Badges */}
          <Card style={styles.showcaseCard}>
            <Text style={styles.sectionHeader}>3. Badges & Progress Bars</Text>
            <View style={styles.badgeRow}>
              <Badge label="Completed" type="completed" />
              <Badge label="In Progress" type="inProgress" />
              <Badge label="Premium" type="premium" />
              <Badge label="Inactive" type="inactive" />
            </View>
            <Text style={styles.progressLabel}>Completion Progress (72%)</Text>
            <ProgressBar progress={0.72} color={theme.colors.primaryPurple} style={styles.mb8} />
            <Text style={styles.progressLabel}>System Sync Status (48%)</Text>
            <ProgressBar progress={0.48} color={theme.colors.secondaryTeal} />
          </Card>

          {/* Section: Teal Chart with green indicators */}
          <Chart data={[24, 45, 12, 60, 48, 80, 52]} labels={['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']} height={140} style={styles.mb16} />

          {/* Section: Data Table inside rounded white card */}
          <View style={styles.mb16}>
            <Text style={styles.sectionHeader}>4. Responsive Data Table</Text>
            <Table headers={tableHeaders} rows={tableRows} />
          </View>

          {/* Modal Trigger Section */}
          <Card style={styles.showcaseCard}>
            <Text style={styles.sectionHeader}>5. Overlay Modals</Text>
            <Button title="Launch Modal Window" onPress={() => setIsModalOpen(true)} variant="primary" />
          </Card>
        </View>
      )}

      {activeSubTab === 'alerts' && (
        <View style={styles.gridSection}>
          <Text style={styles.sectionHeader}>System Alerts & Notifications</Text>
          <Alert type="success" message="Success Alert: The task has been completed successfully." />
          <Alert type="info" message="Information Alert: System configuration has been loaded in background." />
          <Alert type="reminder" message="Reminder Alert: Don't forget to record your daily antihistamine dosage." />
          <Alert type="error" message="Error Alert: We could not process your transaction. Please try again." />
        </View>
      )}

      {activeSubTab === 'states' && (
        <View style={styles.gridSection}>
          {/* Empty State */}
          <Text style={styles.sectionHeader}>Empty State Component</Text>
          <Card style={[styles.showcaseCard, styles.emptyStateCenter]}>
            <View style={styles.emptyStateIconContainer}>
              <IconDashboard color={theme.colors.neutralGrey} size={36} />
            </View>
            <Text style={styles.emptyStateTitle}>No Records Found</Text>
            <Text style={styles.emptyStateDesc}>
              There are currently no items logged in this dashboard filter category.
            </Text>
            <Button title="Import Records" onPress={() => {}} variant="secondary" />
          </Card>

          {/* Loading States */}
          <Text style={styles.sectionHeader}>Loading / Skeleton States</Text>
          <Card style={styles.showcaseCard}>
            <View style={styles.skeletonRow}>
              <ActivityIndicator color={theme.colors.secondaryTeal} size="small" style={styles.spinner} />
              <Text style={styles.loadingText}>Fetching database insights...</Text>
            </View>
            
            {/* Pulsating skeleton blocks */}
            <View style={styles.skeletonBlock}>
              <View style={styles.skeletonLineShort} />
              <View style={styles.skeletonLineLong} />
              <View style={styles.skeletonLineMedium} />
            </View>
          </Card>
        </View>
      )}

      {/* Modal Popup */}
      <Modal
        visible={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Interactive Showcase Modal"
        footerActions={
          <>
            <Button title="Cancel" onPress={() => setIsModalOpen(false)} variant="secondary" style={styles.modalCancel} />
            <Button title="Confirm Action" onPress={() => setIsModalOpen(false)} variant="primary" />
          </>
        }
      >
        <Text style={styles.modalBodyText}>
          This dialog window is structured inside our design system, featuring outline icons, responsive action footers, and generous borders.
        </Text>
      </Modal>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  scrollContent: {
    padding: 16,
    paddingBottom: 40,
  },
  showcaseTitle: {
    fontSize: theme.typography.size.xlarge,
    fontWeight: theme.typography.weight.heavy,
    color: theme.colors.primaryPurple,
    letterSpacing: -0.5,
  },
  showcaseSubtitle: {
    fontSize: theme.typography.size.regular,
    color: theme.colors.neutralGrey,
    marginTop: 4,
    marginBottom: 20,
  },
  tabContainer: {
    flexDirection: 'row',
    backgroundColor: '#EEEEEE',
    borderRadius: theme.borderRadius.medium,
    padding: 3,
    marginBottom: 20,
  },
  tabButton: {
    flex: 1,
    paddingVertical: 10,
    alignItems: 'center',
    borderRadius: theme.borderRadius.small,
  },
  tabButtonActive: {
    backgroundColor: theme.colors.white,
    ...theme.shadows.subtle,
  },
  tabButtonText: {
    fontSize: theme.typography.size.small,
    fontWeight: theme.typography.weight.bold,
    color: theme.colors.neutralGrey,
  },
  tabButtonTextActive: {
    color: theme.colors.textDark,
  },
  gridSection: {
    flexDirection: 'column',
  },
  sectionHeader: {
    fontSize: theme.typography.size.regular,
    fontWeight: theme.typography.weight.bold,
    color: theme.colors.textDark,
    marginBottom: 12,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  },
  showcaseCard: {
    marginBottom: 16,
  },
  buttonRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 12,
    gap: 12,
  },
  flexBtn: {
    flex: 1,
  },
  mb8: {
    marginBottom: 8,
  },
  mb16: {
    marginBottom: 16,
  },
  kpiRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    gap: 12,
    marginBottom: 16,
  },
  kpiCardItem: {
    flex: 1,
  },
  badgeRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
    marginBottom: 16,
  },
  progressLabel: {
    fontSize: theme.typography.size.small,
    color: theme.colors.neutralGrey,
    fontWeight: theme.typography.weight.bold,
    marginBottom: 6,
  },
  emptyStateCenter: {
    alignItems: 'center',
    paddingVertical: 32,
  },
  emptyStateIconContainer: {
    width: 64,
    height: 64,
    borderRadius: 32,
    backgroundColor: '#FAFAFA',
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 16,
    borderWidth: 1.5,
    borderColor: '#EEEEEE',
  },
  emptyStateTitle: {
    fontSize: theme.typography.size.medium,
    fontWeight: theme.typography.weight.bold,
    color: theme.colors.textDark,
    marginBottom: 6,
  },
  emptyStateDesc: {
    fontSize: theme.typography.size.regular,
    color: theme.colors.neutralGrey,
    textAlign: 'center',
    marginBottom: 20,
    paddingHorizontal: 24,
    lineHeight: 18,
  },
  skeletonRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 16,
  },
  spinner: {
    marginRight: 10,
  },
  loadingText: {
    fontSize: theme.typography.size.regular,
    color: theme.colors.textDark,
    fontWeight: theme.typography.weight.semibold,
  },
  skeletonBlock: {
    backgroundColor: '#F5F5F5',
    borderRadius: theme.borderRadius.medium,
    padding: 16,
    width: '100%',
    gap: 10,
  },
  skeletonLineShort: {
    height: 12,
    backgroundColor: '#EAEAEA',
    borderRadius: 4,
    width: '40%',
  },
  skeletonLineLong: {
    height: 12,
    backgroundColor: '#EAEAEA',
    borderRadius: 4,
    width: '90%',
  },
  skeletonLineMedium: {
    height: 12,
    backgroundColor: '#EAEAEA',
    borderRadius: 4,
    width: '65%',
  },
  modalBodyText: {
    fontSize: theme.typography.size.regular,
    color: theme.colors.textDark,
    lineHeight: 20,
  },
  modalCancel: {
    marginRight: 8,
  },
});
