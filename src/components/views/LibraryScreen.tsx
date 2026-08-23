import React from 'react';
import { View, Text, StyleSheet, ScrollView, TouchableOpacity, Platform } from 'react-native';
import { theme } from '../../styles/theme';
import { IconBackArrow } from '../common/CustomIcons';

export default function LibraryScreen() {
  const articles = [
    { title: 'Understanding Chronic Spontaneous Urticaria (CSU)', tag: 'Clinical Guide', readTime: '4 min read' },
    { title: 'Antihistamine Updosing Guidelines: EAACI Recommendations', tag: 'Pharmacology', readTime: '6 min read' },
    { title: 'Identifying Trigger Foods & Environmental Histamines', tag: 'Lifestyle', readTime: '5 min read' },
    { title: 'When to Consider Omalizumab (Xolair) Biological Therapy', tag: 'Treatment Options', readTime: '8 min read' },
  ];

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      {/* Header with Tricolor Accent & Back Button */}
      <View style={styles.headerCard}>
        {Platform.OS === 'web' && <View style={styles.topGradientBar as any} />}
        <View style={styles.headerRow}>
          <TouchableOpacity style={styles.backBtn} activeOpacity={0.75} onPress={() => {}}>
            <IconBackArrow size={18} />
          </TouchableOpacity>
          <Text style={styles.headerTitle}>UrtiCare Library</Text>
        </View>
        <Text style={styles.subtitle}>Evidence-based educational guides and treatment guidelines</Text>
      </View>

      <View style={styles.articleList}>
        {articles.map((item, index) => (
          <TouchableOpacity key={index} style={styles.articleCard} activeOpacity={0.8}>
            <View style={styles.tagBadge as any}>
              <Text style={styles.tagText}>{item.tag}</Text>
            </View>
            <Text style={styles.articleTitle}>{item.title}</Text>
            <Text style={styles.readTime}>{item.readTime}</Text>
          </TouchableOpacity>
        ))}
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: theme.colors.bgLight,
  },
  content: {
    padding: 16,
    paddingBottom: 100,
  },
  headerCard: {
    backgroundColor: '#FFFFFF',
    borderRadius: theme.borderRadius.xlarge,
    padding: 20,
    borderWidth: 1,
    borderColor: '#E2E8F0',
    position: 'relative',
    overflow: 'hidden',
    marginBottom: 16,
    ...theme.shadows.subtle,
  },
  topGradientBar: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    height: 4,
    background: 'linear-gradient(90deg, #814B92 0%, #509729 50%, #1A7E97 100%)',
  } as any,
  headerRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
  },
  backBtn: {
    width: 34,
    height: 34,
    borderRadius: 17,
    backgroundColor: '#FFFFFF',
    borderWidth: 1,
    borderColor: '#E2E8F0',
    justifyContent: 'center',
    alignItems: 'center',
    ...theme.shadows.subtle,
  },
  headerTitle: {
    fontSize: 20,
    fontWeight: theme.typography.weight.heavy,
    color: theme.colors.textDark,
  },
  subtitle: {
    fontSize: 13,
    color: theme.colors.textMuted,
    marginTop: 6,
  },
  articleList: {
    gap: 12,
  },
  articleCard: {
    backgroundColor: '#FFFFFF',
    borderRadius: theme.borderRadius.large,
    padding: 16,
    borderWidth: 1,
    borderColor: '#E2E8F0',
    ...theme.shadows.subtle,
  },
  tagBadge: {
    alignSelf: 'flex-start',
    backgroundColor: '#F5F3FF',
    paddingHorizontal: 12,
    paddingVertical: 4,
    borderRadius: theme.borderRadius.full,
    marginBottom: 8,
    borderWidth: 1,
    borderColor: '#E2E8F0',
    ...Platform.select({
      web: {
        background: 'linear-gradient(135deg, rgba(129, 75, 146, 0.12), rgba(80, 151, 41, 0.12), rgba(26, 126, 151, 0.12))',
      } as any
    })
  },
  tagText: {
    fontSize: 11,
    fontWeight: theme.typography.weight.bold,
    color: '#814B92',
  },
  articleTitle: {
    fontSize: 15,
    fontWeight: theme.typography.weight.bold,
    color: theme.colors.textDark,
    marginBottom: 6,
    lineHeight: 20,
  },
  readTime: {
    fontSize: 11,
    color: theme.colors.textMuted,
  },
});
