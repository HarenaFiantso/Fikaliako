import { useEffect } from 'react';

import * as SplashScreen from 'expo-splash-screen';
import { DarkTheme, DefaultTheme, Stack, ThemeProvider } from 'expo-router';
import { useColorScheme } from 'react-native';

import { AnimatedSplashOverlay } from '@/components/animated-icon';

import { useSession } from '@/lib/auth/session-store';

SplashScreen.preventAutoHideAsync();

export default function RootLayout() {
  const colorScheme = useColorScheme();
  const restore = useSession((state) => state.restore);

  useEffect(() => {
    void restore();
  }, [restore]);

  return (
    <ThemeProvider value={colorScheme === 'dark' ? DarkTheme : DefaultTheme}>
      <AnimatedSplashOverlay />
      <Stack screenOptions={{ headerShown: false }}>
        <Stack.Screen name="(tabs)" />
        <Stack.Screen name="(auth)" options={{ presentation: 'modal' }} />
      </Stack>
    </ThemeProvider>
  );
}
