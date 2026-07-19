import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet, Dimensions, Platform } from 'react-native';
import { useTrackerStore } from '../store/useTrackerStore';

interface GlossyPillProps {
  type: 'FLARE_UP' | 'ANTIHISTAMINE' | 'CORTISONE';
  label: string;
  subtext?: string;
  size: number;
  onPress: () => void;
}

function GlossyActionPill({ type, label, subtext, size, onPress }: GlossyPillProps) {
  // Gel/glossy base colors
  const baseColor = 
    type === 'FLARE_UP' ? '#814b92' : 
    type === 'ANTIHISTAMINE' ? '#509729' : 
    '#1b8097';

  // Increased ring size factor from 0.76 to 0.85 to provide more space for text
  const ringSize = size * 0.85;

  return (
    <TouchableOpacity 
      style={[
        styles.pillContainer, 
        { 
          width: size, 
          height: size, 
          borderRadius: size / 2, 
          backgroundColor: baseColor 
        },
        type === 'ANTIHISTAMINE' && styles.focalPillContainer
      ]} 
      onPress={onPress}
      activeOpacity={0.85}
    >
      {/* Outer 3D Bevel Highlights (lighter top border, darker bottom border) */}
      <View 
        style={[
          styles.bevelHighlight, 
          { 
            width: size, 
            height: size, 
            borderRadius: size / 2 
          }
        ]} 
      />
      
      {/* Inner Ring Drop Shadow for 3D depth */}
      <View 
        style={[
          styles.ringShadow, 
          { 
            width: ringSize, 
            height: ringSize, 
            borderRadius: ringSize / 2,
            top: (size - ringSize) / 2 + 1.2,
            left: (size - ringSize) / 2 + 0.6,
          }
        ]} 
      />
      
      {/* Inner White Ring containing the labels */}
      <View 
        style={[
          styles.whiteRing, 
          { 
            width: ringSize, 
            height: ringSize, 
            borderRadius: ringSize / 2,
            top: (size - ringSize) / 2,
            left: (size - ringSize) / 2,
          }
        ]}
      >
        <View style={styles.textContainer}>
          <Text 
            style={[
              styles.pillText, 
              type === 'ANTIHISTAMINE' && styles.focalText,
              type === 'CORTISONE' && styles.cortisoneText
            ]}
            numberOfLines={type === 'CORTISONE' ? 2 : 1}
            adjustsFontSizeToFit={true}
            minimumFontScale={0.7}
          >
            {label}
          </Text>
          {subtext && (
            <Text 
              style={styles.pillSubtext}
              numberOfLines={1}
              adjustsFontSizeToFit={true}
              minimumFontScale={0.75}
            >
              {subtext}
            </Text>
          )}
        </View>
      </View>
    </TouchableOpacity>
  );
}

export default function ActionPillsGroup() {
  const addEntry = useTrackerStore(state => state.addEntry);
  const setCollisionWarning = useTrackerStore(state => state.setCollisionWarning);

  const handleAction = (type: 'FLARE_UP' | 'ANTIHISTAMINE' | 'CORTISONE') => {
    const timestamp = new Date().toISOString();
    const result = addEntry(type, timestamp);
    
    if (result.collision) {
      setCollisionWarning({
        type,
        timestamp,
        isEdit: false
      });
    }
  };

  return (
    <View style={styles.container}>
      {/* Flare Up Button */}
      <GlossyActionPill 
        type="FLARE_UP"
        label="Flare Up"
        size={basePillSize}
        onPress={() => handleAction('FLARE_UP')}
      />

      {/* Antihistamine Taken Button (Focal Element - scaled by exactly 15%) */}
      <GlossyActionPill 
        type="ANTIHISTAMINE"
        label="Antihistamine"
        subtext="Taken"
        size={basePillSize * 1.15}
        onPress={() => handleAction('ANTIHISTAMINE')}
      />

      {/* Cortisone/Steroid Button */}
      <GlossyActionPill 
        type="CORTISONE"
        label={"Cortico-\nsteroids"}
        size={basePillSize}
        onPress={() => handleAction('CORTISONE')}
      />
    </View>
  );
}

const screenWidth = Dimensions.get('window').width;
const basePillSize = Math.min(96, screenWidth * 0.24);

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    marginVertical: 24,
    width: '100%',
  },
  pillContainer: {
    justifyContent: 'center',
    alignItems: 'center',
    overflow: 'hidden',
    position: 'relative',
    ...Platform.select({
      ios: {
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.3,
        shadowRadius: 5,
      },
      android: {
        elevation: 6,
      },
      web: {
        boxShadow: '0 4px 12px rgba(0, 0, 0, 0.4)',
      }
    }),
  },
  focalPillContainer: {
    zIndex: 10,
    ...Platform.select({
      ios: {
        shadowColor: '#1b8097',
        shadowOffset: { width: 0, height: 6 },
        shadowOpacity: 0.4,
        shadowRadius: 8,
      },
      android: {
        elevation: 8,
      },
      web: {
        boxShadow: '0 6px 20px rgba(80, 151, 41, 0.35)',
      }
    }),
  },
  bevelHighlight: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    borderWidth: 1.5,
    borderTopColor: 'rgba(255, 255, 255, 0.4)',
    borderLeftColor: 'rgba(255, 255, 255, 0.4)',
    borderBottomColor: 'rgba(0, 0, 0, 0.35)',
    borderRightColor: 'rgba(0, 0, 0, 0.35)',
    zIndex: 2,
  },
  ringShadow: {
    position: 'absolute',
    borderWidth: 2,
    borderColor: 'rgba(0, 0, 0, 0.15)',
    zIndex: 1,
  },
  whiteRing: {
    position: 'absolute',
    borderWidth: 2.2,
    borderColor: '#FFFFFF',
    justifyContent: 'center',
    alignItems: 'center',
    zIndex: 2,
    backgroundColor: 'transparent',
  },
  textContainer: {
    justifyContent: 'center',
    alignItems: 'center',
    padding: 2,
  },
  pillText: {
    color: '#FFFFFF',
    fontSize: 10.5,
    fontWeight: '900',
    textAlign: 'center',
    letterSpacing: -0.2,
    textShadowColor: 'rgba(0, 0, 0, 0.25)',
    textShadowOffset: { width: 0, height: 1 },
    textShadowRadius: 1,
  },
  focalText: {
    fontSize: 10.5,
  },
  cortisoneText: {
    fontSize: 8.5,
    lineHeight: 10,
  },
  pillSubtext: {
    color: '#FFFFFF',
    fontSize: 7.5,
    fontWeight: '800',
    textAlign: 'center',
    marginTop: 0.5,
    textShadowColor: 'rgba(0, 0, 0, 0.25)',
    textShadowOffset: { width: 0, height: 1 },
    textShadowRadius: 1,
  }
});
