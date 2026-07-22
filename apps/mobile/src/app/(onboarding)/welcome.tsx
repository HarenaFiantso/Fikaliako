import { useEffect } from 'react';

import { Ionicons } from '@expo/vector-icons';
import { LinearGradient } from 'expo-linear-gradient';
import { useRouter } from 'expo-router';
import { StyleSheet, View } from 'react-native';
import Animated, {
  Easing,
  FadeInDown,
  interpolate,
  useAnimatedStyle,
  useSharedValue,
  withDelay,
  withRepeat,
  withTiming,
  ZoomIn,
} from 'react-native-reanimated';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Button } from '@/components/ui/button';

import { ThemedText } from '@/components/themed-text';

import { useTheme } from '@/hooks/use-theme';

import { useOnboarding } from '@/lib/onboarding-store';

import { MaxContentWidth, Radius, Spacing } from '@/constants/theme';

export default function WelcomeScreen() {
  const router = useRouter();
  const theme = useTheme();
  const complete = useOnboarding((state) => state.complete);

  const browse = () => {
    void complete();
    router.replace('/');
  };

  return (
    <View style={[styles.root, { backgroundColor: theme.background }]}>
      <LinearGradient
        colors={[theme.accent, theme.background]}
        style={StyleSheet.absoluteFill}
        start={{ x: 0.5, y: 0 }}
        end={{ x: 0.5, y: 0.7 }}
      />
      <FloatingBlob color={theme.primary} size={260} x={-90} y={-40} delay={0} drift={26} />
      <FloatingBlob color={theme.accent} size={200} x={220} y={140} delay={900} drift={34} />
      <FloatingBlob color={theme.primary} size={160} x={40} y={420} delay={400} drift={20} />

      <SafeAreaView style={styles.safeArea}>
        <View style={styles.hero}>
          <Animated.View
            entering={ZoomIn.duration(500).springify().damping(14)}
            style={[styles.brandIcon, { backgroundColor: theme.primary }]}
          >
            <Ionicons name="restaurant" size={44} color={theme.onPrimary} />
          </Animated.View>
          <Animated.View entering={FadeInDown.duration(400).delay(150)}>
            <ThemedText type="title" style={styles.centered}>
              Fikaliako
            </ThemedText>
          </Animated.View>
          <Animated.View entering={FadeInDown.duration(400).delay(280)}>
            <ThemedText type="subtitle" style={styles.centered}>
              What am I going to eat today?
            </ThemedText>
          </Animated.View>
          <Animated.View entering={FadeInDown.duration(400).delay(410)}>
            <ThemedText type="default" themeColor="textSecondary" style={styles.centered}>
              Antananarivo&apos;s street food, gargottes and restaurants — found by budget, craving
              and distance.
            </ThemedText>
          </Animated.View>
        </View>

        <Animated.View entering={FadeInDown.duration(400).delay(550)} style={styles.actions}>
          <Button title="Get started" onPress={() => router.push('/onboarding')} />
          <Button title="I'll just browse" variant="ghost" onPress={browse} />
        </Animated.View>
      </SafeAreaView>
    </View>
  );
}

function FloatingBlob({
  color,
  size,
  x,
  y,
  delay,
  drift,
}: {
  color: string;
  size: number;
  x: number;
  y: number;
  delay: number;
  drift: number;
}) {
  const progress = useSharedValue(0);

  useEffect(() => {
    progress.value = withDelay(
      delay,
      withRepeat(withTiming(1, { duration: 6000, easing: Easing.inOut(Easing.sin) }), -1, true)
    );
  }, [delay, progress]);

  const animatedStyle = useAnimatedStyle(() => ({
    transform: [
      { translateY: interpolate(progress.value, [0, 1], [0, -drift]) },
      { translateX: interpolate(progress.value, [0, 1], [0, drift / 2]) },
      { scale: interpolate(progress.value, [0, 1], [1, 1.12]) },
    ],
  }));

  return (
    <Animated.View
      pointerEvents="none"
      style={[
        styles.blob,
        { backgroundColor: color, width: size, height: size, borderRadius: size / 2 },
        { left: x, top: y },
        animatedStyle,
      ]}
    />
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    alignItems: 'center',
  },
  safeArea: {
    flex: 1,
    width: '100%',
    maxWidth: MaxContentWidth,
    padding: Spacing.four,
    gap: Spacing.five,
  },
  hero: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: Spacing.three,
    paddingHorizontal: Spacing.three,
  },
  brandIcon: {
    width: 96,
    height: 96,
    borderRadius: Radius.full,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: Spacing.two,
  },
  centered: {
    textAlign: 'center',
  },
  actions: {
    gap: Spacing.two,
  },
  blob: {
    position: 'absolute',
    opacity: 0.14,
  },
});
