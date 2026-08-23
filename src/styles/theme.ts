export const theme = {
  colors: {
    // Core Brand Colors
    primaryPurple: '#814B92',    // Purple
    successGreen: '#509729',     // Green
    secondaryTeal: '#1A7E97',     // Teal
    mintGreen: '#509729',
    softPink: '#FC9AA3',
    
    // Signature 3-Color Gradient Stops
    gradientPurple: '#814B92',
    gradientGreen: '#509729',
    gradientTeal: '#1A7E97',
    brandGradientCss: 'linear-gradient(135deg, #814B92 0%, #509729 50%, #1A7E97 100%)',
    brandGradientCssHorizontal: 'linear-gradient(90deg, #814B92 0%, #509729 50%, #1A7E97 100%)',
    
    // Background Tints
    softPinkBg: '#FDF2F4',
    softGreenBg: '#ECFDF5',
    softTealBg: '#F0FDFA',
    softPurpleBg: '#F5F3FF',
    softMintPill: '#E6F4EA',
    
    // Neutral Canvas & Surfaces
    neutralGrey: '#64748B',
    bgLight: '#F8FAFC',          // Very light calm healthcare background
    surfaceLight: '#FFFFFF',      // Clean white cards
    glassBg: 'rgba(255, 255, 255, 0.92)',
    glassBorder: '#E2E8F0',
    glassNavBg: '#FFFFFF',
    
    // Typography
    textDark: '#0F172A',
    textLight: '#0F172A',
    textMuted: '#64748B',
    border: '#E2E8F0',
    white: '#FFFFFF',
    
    // Alerts
    alertSuccessBg: '#ECFDF5',
    alertInfoBg: '#F0FDFA',
    alertReminderBg: '#F5F3FF',
    alertErrorBg: '#FDF2F4',
    alertErrorText: '#EF4444'
  },
  borderRadius: {
    small: 8,
    medium: 12,
    large: 16,
    xlarge: 24,
    xxlarge: 32,
    full: 9999
  },
  shadows: {
    subtle: {
      shadowColor: '#0F172A',
      shadowOffset: { width: 0, height: 4 },
      shadowOpacity: 0.05,
      shadowRadius: 12,
      elevation: 2,
    },
    medium: {
      shadowColor: '#0F172A',
      shadowOffset: { width: 0, height: 8 },
      shadowOpacity: 0.08,
      shadowRadius: 18,
      elevation: 4,
    },
    strong: {
      shadowColor: '#0F172A',
      shadowOffset: { width: 0, height: 14 },
      shadowOpacity: 0.12,
      shadowRadius: 24,
      elevation: 7,
    }
  },
  typography: {
    fontFamily: 'System',
    size: {
      xsmall: 11,
      small: 13,
      regular: 15,
      medium: 17,
      large: 20,
      xlarge: 24,
      title: 28,
    },
    weight: {
      light: '300' as const,
      regular: '400' as const,
      medium: '500' as const,
      semibold: '600' as const,
      bold: '700' as const,
      heavy: '800' as const,
    }
  }
};
