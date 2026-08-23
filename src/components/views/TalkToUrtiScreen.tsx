import React, { useState } from 'react';
import { View, Text, StyleSheet, TextInput, TouchableOpacity, ScrollView, Platform } from 'react-native';
import { theme } from '../../styles/theme';
import { IconTalkUrti, IconBackArrow } from '../common/CustomIcons';

export default function TalkToUrtiScreen() {
  const [messages, setMessages] = useState([
    { id: '1', sender: 'urti', text: 'Hello! I am Urti, your personal Urticaria wellness assistant. How can I help you manage your symptoms today?' },
    { id: '2', sender: 'user', text: 'Is it safe to take my antihistamine twice a day if flares worsen?' },
    { id: '3', sender: 'urti', text: 'Second-generation antihistamines (like Cetirizine or Bilastine) are often updosed up to 4x daily under clinical supervision for chronic urticaria. Always consult your allergist before adjusting dosages.' },
  ]);
  const [inputText, setInputText] = useState('');

  const handleSend = () => {
    if (!inputText.trim()) return;
    const userMsg = { id: String(Date.now()), sender: 'user', text: inputText.trim() };
    const replyMsg = { id: String(Date.now() + 1), sender: 'urti', text: 'Thank you for sharing. I will log this query and monitor your symptom timeline.' };
    setMessages(prev => [...prev, userMsg, replyMsg]);
    setInputText('');
  };

  return (
    <View style={styles.container}>
      {/* Header with Tricolor Gradient Accent & Back Button */}
      <View style={styles.header}>
        {Platform.OS === 'web' && <View style={styles.topGradientBar as any} />}
        <TouchableOpacity style={styles.backBtn} activeOpacity={0.75} onPress={() => {}}>
          <IconBackArrow size={18} />
        </TouchableOpacity>
        <View style={styles.iconCircle}>
          <IconTalkUrti color="#814B92" size={20} />
        </View>
        <View style={{ flex: 1 }}>
          <Text style={styles.headerTitle}>Talk to Urti</Text>
          <Text style={styles.headerSub}>Clinical Urticaria Assistant</Text>
        </View>
        <TouchableOpacity style={styles.newChatBtn as any} activeOpacity={0.85} onPress={() => setMessages([])}>
          <Text style={styles.newChatBtnText}>+ New Chat</Text>
        </TouchableOpacity>
      </View>


      <ScrollView style={styles.chatScroll} contentContainerStyle={styles.chatContent}>
        {messages.map(m => (
          <View
            key={m.id}
            style={[
              styles.msgBubble,
              m.sender === 'user' ? (styles.userBubble as any) : styles.urtiBubble,
            ]}
          >
            <Text style={[styles.msgText, m.sender === 'user' ? styles.userMsgText : styles.urtiMsgText]}>
              {m.text}
            </Text>
          </View>
        ))}
      </ScrollView>

      {/* Input row with Tricolor Gradient Send Button */}
      <View style={styles.inputContainer}>
        <TextInput
          style={styles.input}
          value={inputText}
          onChangeText={setInputText}
          placeholder="Ask Urti anything..."
          placeholderTextColor="#94A3B8"
        />
        <TouchableOpacity style={styles.sendBtn as any} onPress={handleSend} activeOpacity={0.85}>
          <Text style={styles.sendBtnText}>Send</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: theme.colors.bgLight,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#E2E8F0',
    backgroundColor: '#FFFFFF',
    position: 'relative',
    overflow: 'hidden',
    gap: 12,
  },
  topGradientBar: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    height: 3.5,
    background: 'linear-gradient(90deg, #814B92 0%, #509729 50%, #1A7E97 100%)',
  } as any,
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
  iconCircle: {
    width: 38,
    height: 38,
    borderRadius: 19,
    backgroundColor: '#F5F3FF',
    borderWidth: 1,
    borderColor: '#E2E8F0',
    justifyContent: 'center',
    alignItems: 'center',
  },
  headerTitle: {
    fontSize: 18,
    fontWeight: theme.typography.weight.bold,
    color: theme.colors.textDark,
  },
  headerSub: {
    fontSize: 12,
    color: theme.colors.textMuted,
  },
  chatScroll: {
    flex: 1,
  },
  chatContent: {
    padding: 16,
    paddingBottom: 100,
    gap: 12,
  },
  msgBubble: {
    maxWidth: '82%',
    paddingHorizontal: 14,
    paddingVertical: 10,
    borderRadius: theme.borderRadius.large,
  },
  userBubble: {
    alignSelf: 'flex-end',
    backgroundColor: '#814B92',
    ...Platform.select({
      web: {
        background: 'linear-gradient(135deg, #814B92 0%, #509729 60%, #1A7E97 100%)',
      } as any
    })
  },
  urtiBubble: {
    alignSelf: 'flex-start',
    backgroundColor: '#FFFFFF',
    borderWidth: 1,
    borderColor: '#E2E8F0',
  },
  msgText: {
    fontSize: 14,
    lineHeight: 20,
  },
  userMsgText: {
    color: '#FFFFFF',
    fontWeight: theme.typography.weight.medium,
  },
  urtiMsgText: {
    color: theme.colors.textDark,
  },
  inputContainer: {
    flexDirection: 'row',
    padding: 12,
    backgroundColor: '#FFFFFF',
    borderTopWidth: 1,
    borderTopColor: '#E2E8F0',
    marginBottom: 60,
    gap: 8,
  },
  input: {
    flex: 1,
    backgroundColor: '#F8FAFC',
    borderWidth: 1,
    borderColor: '#E2E8F0',
    borderRadius: theme.borderRadius.medium,
    paddingHorizontal: 14,
    fontSize: 14,
    color: theme.colors.textDark,
  },
  sendBtn: {
    backgroundColor: '#509729',
    paddingHorizontal: 20,
    justifyContent: 'center',
    borderRadius: theme.borderRadius.medium,
    ...Platform.select({
      web: {
        background: 'linear-gradient(135deg, #814B92 0%, #509729 50%, #1A7E97 100%)',
      } as any
    })
  },
  sendBtnText: {
    color: '#FFFFFF',
    fontWeight: theme.typography.weight.bold,
    fontSize: 13,
  },
  newChatBtn: {
    backgroundColor: '#814B92',
    paddingHorizontal: 12,
    paddingVertical: 7,
    borderRadius: theme.borderRadius.medium,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.4)',
    ...Platform.select({
      web: {
        background: 'linear-gradient(180deg, #9B62AC 0%, #814B92 60%, #5F326E 100%)',
        boxShadow: '0 4px 12px rgba(129, 75, 146, 0.3)',
      } as any
    })
  },
  newChatBtnText: {
    color: '#FFFFFF',
    fontWeight: theme.typography.weight.bold,
    fontSize: 11,
  },
});

