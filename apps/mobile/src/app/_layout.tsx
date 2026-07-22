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
  const status = useSession((state) => state.status);
  const restore = useSession((state) => state.restore);
  const onboardingReady = useOnboarding((state) => state.ready);
  const onboarded = useOnboarding((state) => state.completed);
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

  const ready = (fontsReady || fontsError) && status !== 'restoring' && onboardingReady;
  if (!ready) {
    return null;
  }

  const signedIn = status === 'signedIn';

  return (
    <ThemeProvider value={colorScheme === 'dark' ? DarkTheme : DefaultTheme}>
      <AnimatedSplashOverlay />
      <Stack screenOptions={{ headerShown: false }}>
        <Stack.Protected guard={signedIn}>
          <Stack.Screen name="(tabs)" />
          <Stack.Screen name="(account)" />
        </Stack.Protected>
        <Stack.Protected guard={!signedIn && !onboarded}>
          <Stack.Screen name="(onboarding)" options={{ animation: 'fade' }} />
        </Stack.Protected>
        <Stack.Protected guard={!signedIn}>
          <Stack.Screen name="(auth)" />
        </Stack.Protected>
      </Stack>
    </ThemeProvider>
  );
}
