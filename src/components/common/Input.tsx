import React, { useState } from 'react';
import { View, TextInput, Text, StyleSheet, TouchableOpacity, ViewStyle, Platform, ScrollView } from 'react-native';
import { theme } from '../../styles/theme';
import { IconSearch, IconChevronDown } from './CustomIcons';

interface InputProps {
  label?: string;
  placeholder?: string;
  value: string;
  onChangeText: (text: string) => void;
  style?: ViewStyle;
  secureTextEntry?: boolean;
}

export function Input({ label, placeholder, value, onChangeText, style, secureTextEntry }: InputProps) {
  const [isFocused, setIsFocused] = useState(false);

  return (
    <View style={[styles.container, style]}>
      {label && <Text style={styles.label}>{label}</Text>}
      <TextInput
        style={[
          styles.input,
          isFocused && styles.inputFocused,
        ]}
        placeholder={placeholder}
        placeholderTextColor={theme.colors.textMuted}
        value={value}
        onChangeText={onChangeText}
        onFocus={() => setIsFocused(true)}
        onBlur={() => setIsFocused(false)}
        secureTextEntry={secureTextEntry}
      />
    </View>
  );
}

interface SearchBarProps {
  placeholder?: string;
  value: string;
  onChangeText: (text: string) => void;
  style?: ViewStyle;
}

export function SearchBar({ placeholder = 'Search...', value, onChangeText, style }: SearchBarProps) {
  const [isFocused, setIsFocused] = useState(false);

  return (
    <View style={[styles.searchBarContainer, isFocused && styles.searchBarFocused, style]}>
      <IconSearch color={theme.colors.textMuted} style={styles.searchIcon} />
      <TextInput
        style={styles.searchTextInput}
        placeholder={placeholder}
        placeholderTextColor={theme.colors.textMuted}
        value={value}
        onChangeText={onChangeText}
        onFocus={() => setIsFocused(true)}
        onBlur={() => setIsFocused(false)}
      />
    </View>
  );
}

interface DropdownProps {
  label?: string;
  selectedValue: string;
  options: { label: string; value: string }[];
  onSelect: (value: string) => void;
  style?: ViewStyle;
}

export function Dropdown({ label, selectedValue, options, onSelect, style }: DropdownProps) {
  const [isOpen, setIsOpen] = useState(false);
  const selectedOption = options.find((opt) => opt.value === selectedValue);

  return (
    <View style={[styles.container, style]}>
      {label && <Text style={styles.label}>{label}</Text>}
      <TouchableOpacity
        style={styles.dropdownTrigger}
        activeOpacity={0.8}
        onPress={() => setIsOpen(!isOpen)}
      >
        <Text style={[styles.dropdownTriggerText, !selectedOption && styles.placeholderText]}>
          {selectedOption ? selectedOption.label : 'Select option...'}
        </Text>
        <IconChevronDown color={theme.colors.textMuted} />
      </TouchableOpacity>

      {isOpen && (
        <View style={styles.dropdownOverlay}>
          <ScrollView style={styles.dropdownScroll} nestedScrollEnabled={true}>
            {options.map((opt) => (
              <TouchableOpacity
                key={opt.value}
                style={[
                  styles.dropdownItem,
                  opt.value === selectedValue && styles.dropdownItemSelected,
                ]}
                onPress={() => {
                  onSelect(opt.value);
                  setIsOpen(false);
                }}
              >
                <Text
                  style={[
                    styles.dropdownItemText,
                    opt.value === selectedValue && styles.dropdownItemTextSelected,
                  ]}
                >
                  {opt.label}
                </Text>
              </TouchableOpacity>
            ))}
          </ScrollView>
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    marginBottom: 16,
    width: '100%',
  },
  label: {
    color: theme.colors.textMuted,
    fontSize: theme.typography.size.small,
    fontWeight: theme.typography.weight.bold,
    marginBottom: 6,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  },
  input: {
    backgroundColor: Platform.OS === 'web' ? 'rgba(255, 255, 255, 0.05)' : 'rgba(40, 40, 40, 0.6)',
    borderWidth: 1.5,
    borderColor: theme.colors.glassBorder,
    borderRadius: theme.borderRadius.medium,
    paddingVertical: 10,
    paddingHorizontal: 16,
    fontSize: theme.typography.size.regular,
    color: theme.colors.textLight,
  },
  inputFocused: {
    borderColor: theme.colors.primaryPurple,
    backgroundColor: 'rgba(255, 255, 255, 0.08)',
  },
  searchBarContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Platform.OS === 'web' ? 'rgba(255, 255, 255, 0.05)' : 'rgba(40, 40, 40, 0.6)',
    borderWidth: 1.5,
    borderColor: theme.colors.glassBorder,
    borderRadius: theme.borderRadius.medium,
    paddingHorizontal: 12,
    height: 42,
    flex: 1,
  },
  searchBarFocused: {
    borderColor: theme.colors.secondaryTeal,
  },
  searchIcon: {
    marginRight: 8,
  },
  searchTextInput: {
    flex: 1,
    height: '100%',
    fontSize: theme.typography.size.regular,
    color: theme.colors.textLight,
    padding: 0,
  },
  dropdownTrigger: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: Platform.OS === 'web' ? 'rgba(255, 255, 255, 0.05)' : 'rgba(40, 40, 40, 0.6)',
    borderWidth: 1.5,
    borderColor: theme.colors.glassBorder,
    borderRadius: theme.borderRadius.medium,
    paddingVertical: 10,
    paddingHorizontal: 16,
    height: 44,
  },
  dropdownTriggerText: {
    fontSize: theme.typography.size.regular,
    color: theme.colors.textLight,
  },
  placeholderText: {
    color: theme.colors.textMuted,
  },
  dropdownOverlay: {
    marginTop: 4,
    backgroundColor: 'rgba(30, 30, 30, 0.95)',
    borderWidth: 1.5,
    borderColor: theme.colors.glassBorder,
    borderRadius: theme.borderRadius.medium,
    maxHeight: 180,
    overflow: 'hidden',
    zIndex: 999,
  },
  dropdownScroll: {
    width: '100%',
  },
  dropdownItem: {
    paddingVertical: 10,
    paddingHorizontal: 16,
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(255, 255, 255, 0.05)',
  },
  dropdownItemSelected: {
    backgroundColor: 'rgba(129, 75, 146, 0.2)',
  },
  dropdownItemText: {
    fontSize: theme.typography.size.regular,
    color: theme.colors.textLight,
  },
  dropdownItemTextSelected: {
    color: theme.colors.primaryPurple,
    fontWeight: theme.typography.weight.bold,
  },
});
