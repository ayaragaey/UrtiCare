import React, { useState, useEffect } from 'react';
import { View, Text, StyleSheet, ScrollView, TouchableOpacity, Platform, Alert } from 'react-native';
import { theme } from '../../styles/theme';
import { Input } from '../common/Input';
import { IconProfile, IconBackArrow } from '../common/CustomIcons';
import { useTrackerStore } from '../../store/useTrackerStore';

export default function ProfileScreen() {
  const profile = useTrackerStore(state => state.profile);
  const setProfile = useTrackerStore(state => state.setProfile);
  const favoriteAntihistamine = useTrackerStore(state => state.favoriteAntihistamine);
  const setFavoriteAntihistamine = useTrackerStore(state => state.setFavoriteAntihistamine);
  const defaultFlareUpSymptom = useTrackerStore(state => state.defaultFlareUpSymptom);
  const defaultFlareUpSeverity = useTrackerStore(state => state.defaultFlareUpSeverity);
  const setDefaultFlareUpSymptom = useTrackerStore(state => state.setDefaultFlareUpSymptom);
  const setDefaultFlareUpSeverity = useTrackerStore(state => state.setDefaultFlareUpSeverity);

  const [name, setName] = useState(profile?.name || '');
  const [age, setAge] = useState(profile?.age || '');
  const [gender, setGender] = useState(profile?.gender || '');
  const [email, setEmail] = useState(profile?.email || '');
  const [physician, setPhysician] = useState(profile?.physician || '');
  const [primaryMed, setPrimaryMed] = useState(favoriteAntihistamine);
  const [flareSymptom, setFlareSymptom] = useState(defaultFlareUpSymptom);
  const [flareSeverity, setFlareSeverity] = useState(defaultFlareUpSeverity);

  useEffect(() => {
    if (profile) {
      setName(profile.name || '');
      setAge(profile.age || '');
      setGender(profile.gender || '');
      setEmail(profile.email || '');
      setPhysician(profile.physician || '');
    }
  }, [profile]);

  useEffect(() => {
    setPrimaryMed(favoriteAntihistamine);
  }, [favoriteAntihistamine]);

  useEffect(() => {
    setFlareSymptom(defaultFlareUpSymptom);
  }, [defaultFlareUpSymptom]);

  useEffect(() => {
    setFlareSeverity(defaultFlareUpSeverity);
  }, [defaultFlareUpSeverity]);

  const handleSave = () => {
    setProfile({
      name: name.trim(),
      age: age.trim(),
      gender: gender.trim(),
      email: email.trim(),
      physician: physician.trim(),
    });
    setFavoriteAntihistamine(primaryMed);
    setDefaultFlareUpSymptom(flareSymptom);
    setDefaultFlareUpSeverity(flareSeverity);
    Alert.alert('Success', 'Profile settings saved successfully.');
  };

  const genderOptions = ['Female', 'Male', 'Non-binary', 'Other'];

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      {/* Profile Header */}
      <View style={styles.profileHeaderCard}>
        {Platform.OS === 'web' && <View style={styles.topGradientBar as any} />}
        
        <View style={styles.headerTopRow}>
          <TouchableOpacity style={styles.backBtn} activeOpacity={0.75} onPress={() => {}}>
            <IconBackArrow size={18} />
          </TouchableOpacity>
        </View>
 
        <View style={styles.avatarCircle as any}>
          <IconProfile color="#814B92" size={28} />
        </View>
        <Text style={styles.profileName}>{name.trim() || 'Your Profile'}</Text>
        <Text style={styles.profileEmail}>
          {email.trim() || (age && gender ? `${age} yrs • ${gender}` : 'Essential details: Name, Age, Gender')}
        </Text>
      </View>

      {/* Settings & Treatment Info */}
      <View style={styles.card}>
        <Text style={styles.cardTitle}>Patient Details & Settings</Text>
        <Input label="Full Name *" placeholder="e.g. Sarah Jenkins" value={name} onChangeText={setName} style={styles.mb14} />
        <Input label="Age *" placeholder="e.g. 28" value={age} onChangeText={setAge} style={styles.mb14} />
        
        {/* Gender field with quick selectors */}
        <View style={styles.mb14}>
          <Input label="Gender *" placeholder="e.g. Female, Male, Non-binary" value={gender} onChangeText={setGender} />
          <View style={styles.genderPillsContainer}>
            {genderOptions.map((g) => {
              const isSelected = gender.toLowerCase() === g.toLowerCase();
              return (
                <TouchableOpacity
                  key={g}
                  style={[styles.genderPill, isSelected && styles.genderPillActive]}
                  onPress={() => setGender(g)}
                  activeOpacity={0.75}
                >
                  <Text style={[styles.genderPillText, isSelected && styles.genderPillTextActive]}>{g}</Text>
                </TouchableOpacity>
              );
            })}
          </View>
        </View>

        <Input label="Email Address" placeholder="e.g. sarah.j@urticare.com" value={email} onChangeText={setEmail} style={styles.mb14} />
        <Input label="Primary Maintenance Antihistamine" value={primaryMed} onChangeText={setPrimaryMed} style={styles.mb14} />
        <Input label="Default Flare-up Symptom" value={flareSymptom} onChangeText={setFlareSymptom} style={styles.mb14} />
        <Input label="Default Flare-up Severity" value={flareSeverity} onChangeText={setFlareSeverity} style={styles.mb14} />
        <Input label="Attending Allergist / Physician" placeholder="e.g. Dr. Robert Vance, MD" value={physician} onChangeText={setPhysician} style={styles.mb14} />

        {/* Save Settings Button with Tricolor Gradient Mix */}
        <TouchableOpacity style={styles.saveBtn as any} activeOpacity={0.85} onPress={handleSave}>
          <Text style={styles.saveBtnText}>Save Profile Settings</Text>
        </TouchableOpacity>
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
  profileHeaderCard: {
    backgroundColor: '#FFFFFF',
    borderRadius: theme.borderRadius.xlarge,
    padding: 20,
    alignItems: 'center',
    marginBottom: 16,
    borderWidth: 1,
    borderColor: '#E2E8F0',
    position: 'relative',
    overflow: 'hidden',
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
  headerTopRow: {
    width: '100%',
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 8,
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
  avatarCircle: {
    width: 64,
    height: 64,
    borderRadius: 32,
    backgroundColor: '#F5F3FF',
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 10,
    borderWidth: 2,
    borderColor: '#814B92',
    ...Platform.select({
      web: {
        background: 'linear-gradient(135deg, #F5F3FF, #ECFDF5, #F0FDFA)',
        borderColor: '#814B92',
        boxShadow: '0 4px 12px rgba(129, 75, 146, 0.2)',
      } as any
    })
  },
  profileName: {
    fontSize: 18,
    fontWeight: theme.typography.weight.bold,
    color: theme.colors.textDark,
  },
  profileEmail: {
    fontSize: 12,
    color: theme.colors.textMuted,
    marginTop: 2,
  },
  card: {
    backgroundColor: '#FFFFFF',
    borderRadius: theme.borderRadius.xlarge,
    padding: 20,
    borderWidth: 1,
    borderColor: '#E2E8F0',
    ...theme.shadows.subtle,
  },
  cardTitle: {
    fontSize: 16,
    fontWeight: theme.typography.weight.bold,
    color: '#814B92',
    marginBottom: 16,
    ...Platform.select({
      web: {
        background: 'linear-gradient(90deg, #814B92 0%, #509729 50%, #1A7E97 100%)',
        WebkitBackgroundClip: 'text',
        WebkitTextFillColor: 'transparent',
      } as any
    })
  },
  mb14: {
    marginBottom: 14,
  },
  genderPillsContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
    marginTop: -6,
    marginBottom: 4,
  },
  genderPill: {
    paddingVertical: 6,
    paddingHorizontal: 12,
    borderRadius: 20,
    backgroundColor: '#F1F5F9',
    borderWidth: 1,
    borderColor: '#E2E8F0',
  },
  genderPillActive: {
    backgroundColor: '#ECFDF5',
    borderColor: '#509729',
  },
  genderPillText: {
    fontSize: 12.5,
    color: '#64748B',
    fontWeight: '500',
  },
  genderPillTextActive: {
    color: '#509729',
    fontWeight: '700',
  },
  saveBtn: {
    backgroundColor: '#814B92',
    paddingVertical: 14,
    borderRadius: theme.borderRadius.medium,
    alignItems: 'center',
    marginTop: 10,
    ...Platform.select({
      web: {
        background: 'linear-gradient(135deg, #814B92 0%, #509729 50%, #1A7E97 100%)',
        boxShadow: '0 4px 16px rgba(80, 151, 41, 0.25)',
      } as any
    })
  },
  saveBtnText: {
    color: '#FFFFFF',
    fontWeight: theme.typography.weight.bold,
    fontSize: 14,
  },
});

