import { useEffect } from 'react';

import * as SplashScreen from 'expo-splash-screen';
import {
  Raleway_400Regular,
  Raleway_500Medium,
  Raleway_600SemiBold,
  Raleway_700Bold,
  Raleway_800ExtraBold,
} from '@expo-google-fonts/raleway';
import { useFonts } from 'expo-font';
import { DarkTheme, DefaultTheme, Stack, ThemeProvider } from 'expo-router';
import { useColorScheme } from 'react-native';

import { AnimatedSplashOverlay } from '@/components/animated-icon';

import { useSession } from '@/lib/auth/session-store';
import { useOnboarding } from '@/lib/onboarding-store';

SplashScreen.preventAutoHideAsync();

export default function RootLayout() {
  const colorScheme = useColorScheme();
  const restore = useSession((state) => state.restore);
  const restoreOnboarding = useOnboarding((state) => state.restore);
  const [fontsReady, fontsError] = useFonts({
    Raleway_400Regular,
    Raleway_500Medium,
    Raleway_600SemiBold,
    Raleway_700Bold,
    Raleway_800ExtraBold,
  });

  useEffect(() => {
    void restore();
    void restoreOnboarding();
  }, [restore, restoreOnboarding]);

  if (!fontsReady && !fontsError) {
    return null;
  }

  return (
    <ThemeProvider value={colorScheme === 'dark' ? DarkTheme : DefaultTheme}>
      <AnimatedSplashOverlay />
      <Stack screenOptions={{ headerShown: false }}>
        <Stack.Screen name="(tabs)" />
        <Stack.Screen name="(onboarding)" options={{ animation: 'fade' }} />
        <Stack.Screen name="(auth)" options={{ presentation: 'modal' }} />
      </Stack>
    </ThemeProvider>
  );
}
