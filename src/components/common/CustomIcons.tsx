import React from 'react';
import { View, StyleSheet, ViewStyle, Platform, Text } from 'react-native';

interface IconProps {
  color?: string;
  size?: number;
  style?: ViewStyle;
}

/**
 * 1. Home Icon (Outline House)
 */
export const IconHome = ({ color = '#0F172A', size = 20, style }: IconProps) => {
  return (
    <View style={[{ width: size, height: size, justifyContent: 'center', alignItems: 'center' }, style]}>
      {/* Roof */}
      <View
        style={{
          width: size * 0.6,
          height: size * 0.6,
          borderTopWidth: 2,
          borderLeftWidth: 2,
          borderColor: color,
          transform: [{ rotate: '45deg' }],
          position: 'absolute',
          top: size * 0.12,
          borderRadius: 1.5,
        }}
      />
      {/* Body */}
      <View
        style={{
          width: size * 0.6,
          height: size * 0.45,
          borderWidth: 2,
          borderTopWidth: 0,
          borderColor: color,
          position: 'absolute',
          bottom: size * 0.1,
          borderBottomLeftRadius: 2,
          borderBottomRightRadius: 2,
        }}
      />
    </View>
  );
};

/**
 * 2. Insights Icon (Chart / Analytics)
 */
export const IconInsights = ({ color = '#0F172A', size = 20, style }: IconProps) => {
  return (
    <View style={[{ width: size, height: size, flexDirection: 'row', alignItems: 'flex-end', justifyContent: 'space-between', paddingHorizontal: size * 0.1, paddingBottom: size * 0.1 }, style]}>
      <View style={{ width: size * 0.22, height: size * 0.45, backgroundColor: color, borderRadius: 1.5 }} />
      <View style={{ width: size * 0.22, height: size * 0.8, backgroundColor: color, borderRadius: 1.5 }} />
      <View style={{ width: size * 0.22, height: size * 0.6, backgroundColor: color, borderRadius: 1.5 }} />
    </View>
  );
};

/**
 * 3. Talk to Urti Icon (Speech Chat Bubble)
 */
export const IconTalkUrti = ({ color = '#0F172A', size = 20, style }: IconProps) => {
  return (
    <View style={[{ width: size, height: size, justifyContent: 'center', alignItems: 'center' }, style]}>
      <View
        style={{
          width: size * 0.85,
          height: size * 0.65,
          borderWidth: 1.8,
          borderColor: color,
          borderRadius: size * 0.25,
          justifyContent: 'center',
          alignItems: 'center',
        }}
      >
        <View style={{ flexDirection: 'row', gap: 2 }}>
          <View style={{ width: 2.5, height: 2.5, borderRadius: 1.25, backgroundColor: color }} />
          <View style={{ width: 2.5, height: 2.5, borderRadius: 1.25, backgroundColor: color }} />
          <View style={{ width: 2.5, height: 2.5, borderRadius: 1.25, backgroundColor: color }} />
        </View>
      </View>
      {/* Speech bubble tail */}
      <View
        style={{
          position: 'absolute',
          bottom: size * 0.1,
          left: size * 0.25,
          width: size * 0.18,
          height: size * 0.18,
          backgroundColor: color,
          transform: [{ rotate: '45deg' }],
          borderBottomRightRadius: 1,
        }}
      />
    </View>
  );
};

/**
 * 4. Library Icon (Book)
 */
export const IconLibrary = ({ color = '#0F172A', size = 20, style }: IconProps) => {
  return (
    <View style={[{ width: size, height: size, justifyContent: 'center', alignItems: 'center' }, style]}>
      <View
        style={{
          width: size * 0.75,
          height: size * 0.85,
          borderWidth: 1.8,
          borderColor: color,
          borderRadius: 3,
          position: 'relative',
        }}
      >
        {/* Spine line */}
        <View style={{ position: 'absolute', left: size * 0.15, top: 0, bottom: 0, width: 1.5, backgroundColor: color }} />
        {/* Page detail lines */}
        <View style={{ position: 'absolute', left: size * 0.28, top: size * 0.2, right: size * 0.1, height: 1.5, backgroundColor: color, borderRadius: 1 }} />
        <View style={{ position: 'absolute', left: size * 0.28, top: size * 0.4, right: size * 0.1, height: 1.5, backgroundColor: color, borderRadius: 1 }} />
      </View>
    </View>
  );
};

/**
 * 5. Profile Icon (User Circle)
 */
export const IconProfile = ({ color = '#0F172A', size = 20, style }: IconProps) => {
  const headSize = size * 0.36;
  return (
    <View style={[{ width: size, height: size, justifyContent: 'center', alignItems: 'center', position: 'relative' }, style]}>
      {/* Head */}
      <View
        style={{
          width: headSize,
          height: headSize,
          borderRadius: headSize / 2,
          borderWidth: 1.8,
          borderColor: color,
          position: 'absolute',
          top: size * 0.08,
        }}
      />
      {/* Shoulders */}
      <View
        style={{
          width: size * 0.8,
          height: size * 0.4,
          borderWidth: 1.8,
          borderBottomWidth: 0,
          borderColor: color,
          borderTopLeftRadius: size * 0.4,
          borderTopRightRadius: size * 0.4,
          position: 'absolute',
          bottom: size * 0.08,
        }}
      />
    </View>
  );
};

/**
 * 6. Medication Capsule Icon
 */
export const IconCapsule = ({ color = '#0F172A', size = 20, style }: IconProps) => {
  return (
    <View style={[{ width: size, height: size, justifyContent: 'center', alignItems: 'center' }, style]}>
      <View
        style={{
          width: size * 0.45,
          height: size * 0.85,
          borderRadius: (size * 0.45) / 2,
          borderWidth: 1.8,
          borderColor: color,
          transform: [{ rotate: '-45deg' }],
          backgroundColor: 'transparent',
          position: 'relative',
          justifyContent: 'center',
          alignItems: 'center',
        }}
      >
        {/* Middle divider line */}
        <View
          style={{
            position: 'absolute',
            width: '100%',
            height: 1.8,
            backgroundColor: color,
          }}
        />
      </View>
    </View>
  );
};

/**
 * 6b. Took A Pill Icon (Solid dual-tone capsule on light green circle backdrop)
 */
export const IconTookAPill = ({ size = 20, style }: IconProps) => {
  const backdropSize = size * 1.1;
  const capsuleWidth = size * 0.6;
  const capsuleHeight = size * 1.25;
  
  return (
    <View style={[{ width: size * 1.6, height: size * 1.6, justifyContent: 'center', alignItems: 'center', position: 'relative' }, style]}>
      {/* Backdrop Circle offset to bottom right */}
      <View
        style={{
          width: backdropSize,
          height: backdropSize,
          borderRadius: backdropSize / 2,
          backgroundColor: '#DCFCE7', // Soft green backdrop
          position: 'absolute',
          bottom: size * 0.1,
          right: size * 0.1,
          opacity: 0.95,
        }}
      />
      {/* Capsule rotated at -45 degrees and offset to top left */}
      <View
        style={{
          width: capsuleWidth,
          height: capsuleHeight,
          borderRadius: capsuleWidth / 2,
          backgroundColor: '#A7F3D0', // Light green top-right half
          justifyContent: 'flex-end',
          alignItems: 'center',
          transform: [{ rotate: '-45deg' }],
          overflow: 'hidden',
          position: 'absolute',
          top: size * 0.1,
          left: size * 0.1,
        }}
      >
        {/* Darker green bottom-left half */}
        <View
          style={{
            width: '100%',
            height: '50%',
            backgroundColor: '#059669', // Emerald green
          }}
        />
      </View>
    </View>
  );
};

/**
 * 6c. Flare Up Widget Icon (Splash flame in purple/pink on light purple circle backdrop)
 */
export const IconFlareUpWidget = ({ size = 20, style }: IconProps) => {
  if (Platform.OS === 'web') {
    return (
      <View style={[{ width: size * 1.6, height: size * 1.6, justifyContent: 'center', alignItems: 'center' }, style]}>
        <svg width={size * 1.6} height={size * 1.6} viewBox="0 0 24 24" fill="none">
          <circle cx="13" cy="13" r="8.5" fill="#F5F3FF" />
          {/* Outer flame shape */}
          <path d="M12 4C12 4 14.5 7.5 15.5 9.5C16.5 11.5 16.5 13.5 15 15C13.5 16.5 10.5 16.5 9 15C7.5 13.5 7.5 11.5 8.5 9.5C9.5 7.5 12 4 12 4Z" fill="#C084FC" opacity="0.65" />
          {/* Inner flame shape */}
          <path d="M12 7.5C12 7.5 13.5 10 14 11.2C14.5 12.5 14.5 13.8 13.5 14.8C12.5 15.8 11.5 15.8 10.5 14.8C9.5 13.8 9.5 12.5 10 11.2C10.5 10 12 7.5 12 7.5Z" fill="#814B92" />
          {/* Left splash dot */}
          <circle cx="6" cy="12" r="1.5" fill="#C084FC" />
          {/* Right splash dot */}
          <circle cx="18" cy="8" r="1.2" fill="#C084FC" />
        </svg>
      </View>
    );
  }
  return (
    <View style={[{ width: size * 1.6, height: size * 1.6, justifyContent: 'center', alignItems: 'center', position: 'relative' }, style]}>
      {/* Backdrop Circle */}
      <View style={{
        width: size * 1.1,
        height: size * 1.1,
        borderRadius: (size * 1.1) / 2,
        backgroundColor: '#F5F3FF',
        position: 'absolute',
        bottom: size * 0.1,
        right: size * 0.1,
        opacity: 0.95
      }} />
      {/* Native Fallback: Custom droplet/fire style in purple */}
      <View style={{
        width: size * 0.75,
        height: size * 0.75,
        borderRadius: size * 0.375,
        backgroundColor: '#814B92',
        justifyContent: 'center',
        alignItems: 'center'
      }}>
        <Text style={{ fontSize: size * 0.45, color: '#FFFFFF' }}>🔥</Text>
      </View>
    </View>
  );
};

/**
 * 6d. Consumption Widget Icon (Cup, orange and drumstick in orange on light orange circle backdrop)
 */
export const IconConsumptionWidget = ({ size = 20, style }: IconProps) => {
  if (Platform.OS === 'web') {
    return (
      <View style={[{ width: size * 1.6, height: size * 1.6, justifyContent: 'center', alignItems: 'center' }, style]}>
        <svg width={size * 1.6} height={size * 1.6} viewBox="0 0 24 24" fill="none">
          <circle cx="13" cy="13" r="8.5" fill="#FFF7ED" />
          
          {/* Cup body (center back) */}
          <polygon points="10.5,8.5 13.5,8.5 13,15.5 11,15.5" fill="#FFDBB5" />
          {/* Lid */}
          <rect x="10" y="7.5" width="4" height="1" rx="0.5" fill="#F97316" />
          {/* Straw */}
          <line x1="12.5" y1="4.5" x2="11.8" y2="7.5" stroke="#F97316" strokeWidth="1.2" strokeLinecap="round" />

          {/* Orange fruit (left front) */}
          <circle cx="9.2" cy="14.2" r="3.2" fill="#F97316" />
          {/* Stem & leaf */}
          <path d="M9.2,11 C9.8,10.4 10.4,10.7 10.4,10.7" stroke="#84CC16" strokeWidth="0.8" fill="none" />

          {/* Chicken leg drumstick (right front) */}
          <g transform="translate(13.5, 11) rotate(15)">
            <ellipse cx="2.5" cy="2.5" rx="3.2" ry="2.2" fill="#EA580C" />
            <line x1="4.5" y1="3.5" x2="6.8" y2="4.5" stroke="#FFDBB5" strokeWidth="1.6" strokeLinecap="round" />
            <circle cx="6.8" cy="3.9" r="0.9" fill="#FFDBB5" />
            <circle cx="6.4" cy="5.1" r="0.9" fill="#FFDBB5" />
          </g>
        </svg>
      </View>
    );
  }
  return (
    <View style={[{ width: size * 1.6, height: size * 1.6, justifyContent: 'center', alignItems: 'center', position: 'relative' }, style]}>
      {/* Backdrop Circle */}
      <View style={{
        width: size * 1.1,
        height: size * 1.1,
        borderRadius: (size * 1.1) / 2,
        backgroundColor: '#FFF7ED',
        position: 'absolute',
        bottom: size * 0.1,
        right: size * 0.1,
        opacity: 0.95
      }} />
      {/* Native Fallback */}
      <View style={{
        width: size * 0.75,
        height: size * 0.75,
        borderRadius: size * 0.375,
        backgroundColor: '#F97316',
        justifyContent: 'center',
        alignItems: 'center'
      }}>
        <Text style={{ fontSize: size * 0.45, color: '#FFFFFF' }}>🍎</Text>
      </View>
    </View>
  );
};



/**
 * 7. Flare Up Droplet Icon
 */
export const IconDroplet = ({ color = '#FC9AA3', size = 20, style }: IconProps) => {
  return (
    <View style={[{ width: size, height: size, justifyContent: 'center', alignItems: 'center' }, style]}>
      <View
        style={{
          width: size * 0.65,
          height: size * 0.65,
          borderWidth: 1.8,
          borderColor: color,
          borderTopLeftRadius: 0,
          borderTopRightRadius: size * 0.4,
          borderBottomLeftRadius: size * 0.4,
          borderBottomRightRadius: size * 0.4,
          transform: [{ rotate: '-45deg' }],
        }}
      />
    </View>
  );
};

/**
 * 8. Corticosteroids Tablet Icon
 */
export const IconTablet = ({ color = '#0EA5E9', size = 20, style }: IconProps) => {
  return (
    <View style={[{ width: size, height: size, justifyContent: 'center', alignItems: 'center' }, style]}>
      <View
        style={{
          width: size * 0.75,
          height: size * 0.75,
          borderRadius: (size * 0.75) / 2,
          borderWidth: 1.8,
          borderColor: color,
          justifyContent: 'center',
          alignItems: 'center',
        }}
      >
        <View style={{ width: '100%', height: 1.8, backgroundColor: color }} />
      </View>
    </View>
  );
};

/**
 * 9. Edit Pencil Icon
 */
export const IconEdit = ({ color = '#64748B', size = 16, style }: IconProps) => {
  return (
    <View style={[{ width: size, height: size, justifyContent: 'center', alignItems: 'center' }, style]}>
      <View
        style={{
          width: size * 0.35,
          height: size * 0.75,
          borderWidth: 1.5,
          borderColor: color,
          borderRadius: 2,
          transform: [{ rotate: '45deg' }],
        }}
      />
    </View>
  );
};

/**
 * 10. Delete Trash Icon
 */
export const IconDelete = ({ color = '#EF4444', size = 16, style }: IconProps) => {
  return (
    <View style={[{ width: size, height: size, justifyContent: 'center', alignItems: 'center' }, style]}>
      {/* Lid */}
      <View style={{ width: size * 0.7, height: 1.5, backgroundColor: color, marginBottom: 1 }} />
      {/* Bucket */}
      <View
        style={{
          width: size * 0.55,
          height: size * 0.55,
          borderWidth: 1.5,
          borderTopWidth: 0,
          borderColor: color,
          borderBottomLeftRadius: 2,
          borderBottomRightRadius: 2,
        }}
      />
    </View>
  );
};

/**
 * 11. Plus Sign Icon
 */
export const IconPlus = ({ color = '#0F172A', size = 20, style }: IconProps) => {
  return (
    <View style={[{ width: size, height: size, justifyContent: 'center', alignItems: 'center', position: 'relative' }, style]}>
      <View style={{ position: 'absolute', width: size * 0.65, height: 2.2, backgroundColor: color, borderRadius: 1.5 }} />
      <View style={{ position: 'absolute', height: size * 0.65, width: 2.2, backgroundColor: color, borderRadius: 1.5 }} />
    </View>
  );
};

// Legacy icon exports for backward compatibility
export const IconDashboard = IconHome;
export const IconTracker = IconInsights;
export const IconShowcase = IconLibrary;
export const IconSettings = IconProfile;
export const IconUser = IconProfile;
export const IconSearch = ({ color = '#64748B', size = 18 }: IconProps) => (
  <View style={{ width: size, height: size, borderWidth: 1.5, borderColor: color, borderRadius: size / 2 }} />
);
export const IconChevronDown = ({ color = '#64748B', size = 16 }: IconProps) => (
  <View style={{ width: size, height: size, justifyContent: 'center', alignItems: 'center' }}>
    <View style={{ width: size * 0.4, height: size * 0.4, borderBottomWidth: 2, borderRightWidth: 2, borderColor: color, transform: [{ rotate: '45deg' }] }} />
  </View>
);

export const IconChevronRight = ({ color = '#64748B', size = 16 }: IconProps) => (
  <View style={{ width: size, height: size, justifyContent: 'center', alignItems: 'center' }}>
    <View style={{ width: size * 0.4, height: size * 0.4, borderTopWidth: 2, borderRightWidth: 2, borderColor: color, transform: [{ rotate: '45deg' }] }} />
  </View>
);

/**
 * 12. Signature Rounded Green-to-Teal Gradient Back Arrow Icon
 */
export const IconBackArrow = ({ size = 20, style }: IconProps) => {
  if (Platform.OS === 'web') {
    return (
      <View style={[{ width: size, height: size, justifyContent: 'center', alignItems: 'center' }, style]}>
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none">
          <defs>
            <linearGradient id="customBackArrowGrad" x1="0%" y1="0%" x2="100%" y2="0%">
              <stop offset="0%" stopColor="#509729" />
              <stop offset="50%" stopColor="#2E8B75" />
              <stop offset="100%" stopColor="#1A7E97" />
            </linearGradient>
          </defs>
          <path
            d="M21 12H3.5M11.5 19.5L3.5 12L11.5 4.5"
            stroke="url(#customBackArrowGrad)"
            strokeWidth="3.2"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      </View>
    );
  }
  return (
    <View style={[{ width: size, height: size, justifyContent: 'center', alignItems: 'center' }, style]}>
      <Text style={{ fontSize: size * 0.85, fontWeight: 'bold', color: '#509729' }}>←</Text>
    </View>
  );
};

export const IconClose = ({ color = '#64748B', size = 16, style }: IconProps) => {
  if (Platform.OS === 'web') {
    return (
      <View style={[{ width: size, height: size, justifyContent: 'center', alignItems: 'center' }, style]}>
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none">
          <path
            d="M18 6L6 18M6 6l12 12"
            stroke={color}
            strokeWidth="2.5"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      </View>
    );
  }
  return (
    <View style={[{ width: size, height: size, justifyContent: 'center', alignItems: 'center' }, style]}>
      <Text style={{ fontSize: size * 0.95, fontWeight: 'bold', color }}>×</Text>
    </View>
  );
};

export const IconLightning = ({ color = '#814B92', size = 20, style }: IconProps) => {
  if (Platform.OS === 'web') {
    return (
      <View style={[{ width: size, height: size, justifyContent: 'center', alignItems: 'center' }, style]}>
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none">
          <path
            d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"
            fill={color}
          />
        </svg>
      </View>
    );
  }
  return (
    <View style={[{ width: size, height: size, justifyContent: 'center', alignItems: 'center' }, style]}>
      <Text style={{ fontSize: size * 0.9, color }}>⚡</Text>
    </View>
  );
};

export const IconCheck = ({ color = '#509729', size = 18, style }: IconProps) => {
  if (Platform.OS === 'web') {
    return (
      <View style={[{ width: size, height: size, justifyContent: 'center', alignItems: 'center' }, style]}>
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none">
          <path
            d="M20 6L9 17l-5-5"
            stroke={color}
            strokeWidth="2.5"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      </View>
    );
  }
  return (
    <View style={[{ width: size, height: size, justifyContent: 'center', alignItems: 'center' }, style]}>
      <Text style={{ fontSize: size * 0.9, fontWeight: 'bold', color }}>✓</Text>
    </View>
  );
};

export const IconInfo = ({ color = '#1A7E97', size = 18, style }: IconProps) => {
  if (Platform.OS === 'web') {
    return (
      <View style={[{ width: size, height: size, justifyContent: 'center', alignItems: 'center' }, style]}>
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none">
          <circle cx="12" cy="12" r="10" stroke={color} strokeWidth="2.5" />
          <path d="M12 7h.01M12 11v5" stroke={color} strokeWidth="2.5" strokeLinecap="round" />
        </svg>
      </View>
    );
  }
  return (
    <View style={[{ width: size, height: size, justifyContent: 'center', alignItems: 'center' }, style]}>
      <Text style={{ fontSize: size * 0.9, fontWeight: 'bold', color }}>i</Text>
    </View>
  );
};

export const IconReminder = ({ color = '#814B92', size = 18, style }: IconProps) => {
  if (Platform.OS === 'web') {
    return (
      <View style={[{ width: size, height: size, justifyContent: 'center', alignItems: 'center' }, style]}>
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none">
          <circle cx="12" cy="12" r="10" stroke={color} strokeWidth="2.5" />
          <path d="M12 8v5M12 16h.01" stroke={color} strokeWidth="2.5" strokeLinecap="round" />
        </svg>
      </View>
    );
  }
  return (
    <View style={[{ width: size, height: size, justifyContent: 'center', alignItems: 'center' }, style]}>
      <Text style={{ fontSize: size * 0.9, fontWeight: 'bold', color }}>!</Text>
    </View>
  );
};

export const IconError = ({ color = '#EF4444', size = 18, style }: IconProps) => {
  if (Platform.OS === 'web') {
    return (
      <View style={[{ width: size, height: size, justifyContent: 'center', alignItems: 'center' }, style]}>
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none">
          <circle cx="12" cy="12" r="10" stroke={color} strokeWidth="2.5" />
          <path d="M15 9l-6 6M9 9l6 6" stroke={color} strokeWidth="2.5" strokeLinecap="round" />
        </svg>
      </View>
    );
  }
  return (
    <View style={[{ width: size, height: size, justifyContent: 'center', alignItems: 'center' }, style]}>
      <Text style={{ fontSize: size * 0.9, fontWeight: 'bold', color }}>⚠</Text>
    </View>
  );
};

export const IconBiological = ({ color = '#0F172A', size = 20, style }: IconProps) => {
  return (
    <View style={[{ width: size, height: size, justifyContent: 'center', alignItems: 'center', position: 'relative' }, style]}>
      {/* Needle */}
      <View style={{ width: 1.5, height: size * 0.2, backgroundColor: color, position: 'absolute', top: size * 0.1 }} />
      {/* Barrel */}
      <View style={{ width: size * 0.3, height: size * 0.45, borderWidth: 1.8, borderColor: color, borderRadius: 1.5, backgroundColor: 'transparent', position: 'absolute', top: size * 0.3 }} />
      {/* Fluid markings */}
      <View style={{ width: size * 0.15, height: 1.2, backgroundColor: color, position: 'absolute', top: size * 0.42, left: size * 0.4 }} />
      <View style={{ width: size * 0.15, height: 1.2, backgroundColor: color, position: 'absolute', top: size * 0.52, left: size * 0.4 }} />
      <View style={{ width: size * 0.15, height: 1.2, backgroundColor: color, position: 'absolute', top: size * 0.62, left: size * 0.4 }} />
      {/* Plunger */}
      <View style={{ width: 1.8, height: size * 0.25, backgroundColor: color, position: 'absolute', top: size * 0.72 }} />
      <View style={{ width: size * 0.25, height: 1.8, backgroundColor: color, position: 'absolute', top: size * 0.85 }} />
    </View>
  );
};

export const IconSyringeOutline = ({ color = '#238A9C', size = 18, style }: IconProps) => {
  return (
    <View style={[{ width: size, height: size * 1.5, justifyContent: 'center', alignItems: 'center' }, style]}>
      {/* Plunger handle at top */}
      <View style={{ width: 6, height: 1.5, backgroundColor: color }} />
      <View style={{ width: 1.5, height: 3, backgroundColor: color }} />
      {/* Barrel */}
      <View style={{ 
        width: 10, 
        height: 14, 
        borderWidth: 1.5, 
        borderColor: color, 
        borderRadius: 1.2,
        backgroundColor: 'transparent'
      }} />
      {/* Needle at bottom */}
      <View style={{ width: 1.5, height: 4, backgroundColor: color }} />
    </View>
  );
};

export const IconPlusInCircle = ({ color = '#4C9A2A', size = 20, style }: IconProps) => {
  return (
    <View style={[{ width: size, height: size, justifyContent: 'center', alignItems: 'center' }, style]}>
      {/* Circular boundary */}
      <View style={{
        width: size,
        height: size,
        borderRadius: size / 2,
        borderWidth: 1.8,
        borderColor: color,
        justifyContent: 'center',
        alignItems: 'center',
        backgroundColor: 'transparent'
      }}>
        {/* Plus sign */}
        <View style={{ width: size * 0.45, height: 1.8, backgroundColor: color, position: 'absolute' }} />
        <View style={{ width: 1.8, height: size * 0.45, backgroundColor: color, position: 'absolute' }} />
      </View>
    </View>
  );
};

export const IconDownloadArrow = ({ color = '#4C9A2A', size = 20, style }: IconProps) => {
  return (
    <View style={[{ width: size, height: size, justifyContent: 'center', alignItems: 'center' }, style]}>
      {/* Downward arrow stem */}
      <View style={{ width: 1.8, height: size * 0.45, backgroundColor: color, position: 'absolute', top: size * 0.15 }} />
      {/* Arrow head */}
      <View style={{ 
        width: size * 0.25, 
        height: 1.8, 
        backgroundColor: color, 
        position: 'absolute', 
        top: size * 0.45,
        left: size * 0.25,
        transform: [{ rotate: '45deg' }]
      }} />
      <View style={{ 
        width: size * 0.25, 
        height: 1.8, 
        backgroundColor: color, 
        position: 'absolute', 
        top: size * 0.45,
        right: size * 0.25,
        transform: [{ rotate: '-45deg' }]
      }} />
      {/* Bottom bar */}
      <View style={{ width: size * 0.55, height: 1.8, backgroundColor: color, position: 'absolute', bottom: size * 0.18 }} />
    </View>
  );
};

export const IconTrashCan = ({ color = '#E53935', size = 20, style }: IconProps) => {
  return (
    <View style={[{ width: size, height: size * 1.1, justifyContent: 'center', alignItems: 'center' }, style]}>
      {/* Lid line/handle */}
      <View style={{ width: size * 0.45, height: 1.5, backgroundColor: color, borderTopLeftRadius: 1, borderTopRightRadius: 1, marginBottom: 1 }} />
      {/* Lid tray */}
      <View style={{ width: size * 0.7, height: 1.5, backgroundColor: color }} />
      {/* Basket body with outline */}
      <View style={{
        width: size * 0.55,
        height: size * 0.65,
        borderWidth: 1.5,
        borderColor: color,
        borderBottomLeftRadius: 2,
        borderBottomRightRadius: 2,
        marginTop: 1,
        backgroundColor: 'transparent'
      }} />
    </View>
  );
};



