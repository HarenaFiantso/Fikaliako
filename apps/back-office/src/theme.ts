import { defaultDarkTheme, defaultLightTheme, type RaThemeOptions } from 'react-admin';

/**
 * Fikaliako design tokens — the warm, food-first look shared with the other
 * apps: terracotta primary, cream surfaces, espresso text, Raleway type.
 * Hex values mirror apps/mobile/src/constants/theme.ts, itself derived from
 * the OKLCH tokens in apps/web/app/globals.css.
 */
type Tokens = {
  text: string;
  textSecondary: string;
  background: string;
  card: string;
  primary: string;
  onPrimary: string;
  accent: string;
  danger: string;
  success: string;
  border: string;
};

const light: Tokens = {
  text: '#261B16',
  textSecondary: '#6F6056',
  background: '#FCFAF6',
  card: '#FEFDFB',
  primary: '#B74B21',
  onPrimary: '#FDFAF3',
  accent: '#F9E4D0',
  danger: '#CC2827',
  success: '#218358',
  border: '#E4DDD3',
};

const dark: Tokens = {
  text: '#F3F0EA',
  textSecondary: '#A1968B',
  background: '#110B08',
  card: '#1A130F',
  primary: '#EC854D',
  onPrimary: '#170D08',
  accent: '#2E241D',
  danger: '#E8594F',
  success: '#3DD68C',
  border: '#2C251F',
};

const fontFamily = "'Raleway', ui-sans-serif, system-ui, sans-serif";

const typography: RaThemeOptions['typography'] = {
  fontFamily,
  h6: { fontWeight: 700 },
  button: { fontWeight: 600, textTransform: 'none' },
};

// Matches the web app's --radius: 0.625rem.
const shape = { borderRadius: 10 };

function components(tokens: Tokens, base: RaThemeOptions): RaThemeOptions['components'] {
  return {
    ...base.components,
    MuiButton: {
      styleOverrides: {
        root: { textTransform: 'none', fontWeight: 600 },
      },
    },
    MuiAppBar: {
      styleOverrides: {
        root: {
          backgroundColor: tokens.card,
          color: tokens.text,
          borderBottom: `1px solid ${tokens.border}`,
          boxShadow: 'none',
        },
      },
    },
    MuiPaper: {
      styleOverrides: {
        root: { backgroundImage: 'none' },
      },
    },
  };
}

export const lightTheme: RaThemeOptions = {
  ...defaultLightTheme,
  palette: {
    mode: 'light',
    primary: { main: light.primary, contrastText: light.onPrimary },
    secondary: { main: light.textSecondary },
    error: { main: light.danger },
    success: { main: light.success },
    background: { default: light.background, paper: light.card },
    text: { primary: light.text, secondary: light.textSecondary },
    divider: light.border,
  },
  typography,
  shape,
  components: components(light, defaultLightTheme),
};

export const darkTheme: RaThemeOptions = {
  ...defaultDarkTheme,
  palette: {
    mode: 'dark',
    primary: { main: dark.primary, contrastText: dark.onPrimary },
    secondary: { main: dark.textSecondary },
    error: { main: dark.danger },
    success: { main: dark.success },
    background: { default: dark.background, paper: dark.card },
    text: { primary: dark.text, secondary: dark.textSecondary },
    divider: dark.border,
  },
  typography,
  shape,
  components: components(dark, defaultDarkTheme),
};
