/**
 * Fikaliako design tokens, mirroring the web app (apps/web/app/globals.css):
 * warm, food-first palette — terracotta primary, cream surfaces, espresso
 * text — converted from OKLCH to sRGB hex, and Raleway as the brand font.
 */
import { Platform } from 'react-native';

import '@/global.css';

export const Colors = {
  light: {
    text: '#261B16',
    background: '#FCFAF6',
    backgroundElement: '#F4EFE7',
    backgroundSelected: '#F9E4D0',
    textSecondary: '#6F6056',
    card: '#FEFDFB',
    primary: '#B74B21',
    onPrimary: '#FDFAF3',
    accent: '#F9E4D0',
    danger: '#CC2827',
    success: '#218358',
    border: '#E4DDD3',
  },
  dark: {
    text: '#F3F0EA',
    background: '#110B08',
    backgroundElement: '#241E1A',
    backgroundSelected: '#2E241D',
    textSecondary: '#A1968B',
    card: '#1A130F',
    primary: '#EC854D',
    onPrimary: '#170D08',
    accent: '#2E241D',
    danger: '#E8594F',
    success: '#3DD68C',
    border: '#2C251F',
  },
} as const;

export type ThemeColor = keyof typeof Colors.light & keyof typeof Colors.dark;

export const FontFamily = {
  regular: 'Raleway_400Regular',
  medium: 'Raleway_500Medium',
  semiBold: 'Raleway_600SemiBold',
  bold: 'Raleway_700Bold',
  extraBold: 'Raleway_800ExtraBold',
} as const;

export const Fonts = Platform.select({
  ios: {
    sans: 'system-ui',
    serif: 'ui-serif',
    rounded: 'ui-rounded',
    mono: 'ui-monospace',
  },
  default: {
    sans: 'normal',
    serif: 'serif',
    rounded: 'normal',
    mono: 'monospace',
  },
  web: {
    sans: 'var(--font-display)',
    serif: 'var(--font-serif)',
    rounded: 'var(--font-rounded)',
    mono: 'var(--font-mono)',
  },
});

export const Spacing = {
  half: 2,
  one: 4,
  two: 8,
  three: 16,
  four: 24,
  five: 32,
  six: 64,
} as const;

export const Radius = {
  sm: 6,
  md: 8,
  lg: 10,
  xl: 14,
  xxl: 20,
  full: 999,
} as const;

// Clearance for the floating glass tab bar (64 tall + 8 gap + breathing room).
export const BottomTabInset = 88;
export const MaxContentWidth = 800;
