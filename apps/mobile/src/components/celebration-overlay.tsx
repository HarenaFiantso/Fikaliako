import { useEffect } from 'react';

import { Ionicons } from '@expo/vector-icons';
import { Pressable, StyleSheet, View } from 'react-native';
import Animated, {
  Easing,
  FadeIn,
  FadeOut,
  interpolate,
  useAnimatedStyle,
  useSharedValue,
  withDelay,
  withTiming,
  ZoomIn,
} from 'react-native-reanimated';

import { Brand } from '@/components/brand-decor';
import { ThemedText } from '@/components/themed-text';

import { useTheme } from '@/hooks/use-theme';

import { Radius, Spacing } from '@/constants/theme';

const AUTO_DISMISS_MS = 2600;

const CONFETTI: { x: number; delay: number; color: string; tilt: number }[] = [
  { x: 18, delay: 0, color: Brand.terracotta, tilt: 40 },
  { x: 52, delay: 140, color: Brand.success, tilt: -60 },
  { x: 84, delay: 60, color: Brand.accent, tilt: 90 },
  { x: 118, delay: 200, color: Brand.terracotta, tilt: -30 },
  { x: 150, delay: 30, color: Brand.accent, tilt: 70 },
  { x: 184, delay: 170, color: Brand.success, tilt: -80 },
  { x: 216, delay: 100, color: Brand.terracotta, tilt: 50 },
  { x: 244, delay: 240, color: Brand.accent, tilt: -45 },
];

export function CelebrationOverlay({
  message,
  onDismiss,
}: {
  message: string;
  onDismiss: () => void;
}) {
  const theme = useTheme();

  useEffect(() => {
    const timer = setTimeout(onDismiss, AUTO_DISMISS_MS);
    return () => clearTimeout(timer);
  }, [onDismiss]);

  return (
    <Animated.View
      entering={FadeIn.duration(200)}
      exiting={FadeOut.duration(250)}
      style={styles.scrim}
    >
      <Pressable style={StyleSheet.absoluteFill} onPress={onDismiss} accessibilityLabel="Dismiss" />
      <Animated.View
        entering={ZoomIn.duration(350).springify().damping(15)}
        style={[styles.card, { backgroundColor: theme.card }]}
      >
        <View style={styles.confettiZone} pointerEvents="none">
          {CONFETTI.map((piece) => (
            <ConfettiPiece key={`${piece.x}-${piece.delay}`} {...piece} />
          ))}
        </View>
        <View style={styles.badge}>
          <Ionicons name="checkmark" size={44} color={Brand.cream} />
        </View>
        <ThemedText type="subtitle" style={styles.centered}>
          Tongasoa!
        </ThemedText>
        <ThemedText type="default" themeColor="textSecondary" style={styles.centered}>
          {message}
        </ThemedText>
      </Animated.View>
    </Animated.View>
  );
}

function ConfettiPiece({ x, delay, color, tilt }: (typeof CONFETTI)[number]) {
  const progress = useSharedValue(0);

  useEffect(() => {
    progress.value = withDelay(
      delay,
      withTiming(1, { duration: 1400, easing: Easing.out(Easing.quad) })
    );
  }, [delay, progress]);

  const style = useAnimatedStyle(() => ({
    opacity: interpolate(progress.value, [0, 0.1, 1], [0, 1, 0]),
    transform: [
      { translateY: interpolate(progress.value, [0, 1], [-16, 120]) },
      { rotate: `${interpolate(progress.value, [0, 1], [0, tilt * 4])}deg` },
    ],
  }));

  return <Animated.View style={[styles.confetti, { left: x, backgroundColor: color }, style]} />;
}

const styles = StyleSheet.create({
  scrim: {
    ...StyleSheet.absoluteFill,
    backgroundColor: 'rgba(17, 11, 8, 0.5)',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 900,
    padding: Spacing.four,
  },
  card: {
    width: '100%',
    maxWidth: 340,
    borderRadius: Radius.xxl + 8,
    padding: Spacing.five,
    alignItems: 'center',
    gap: Spacing.two,
    overflow: 'hidden',
  },
  confettiZone: {
    ...StyleSheet.absoluteFill,
  },
  confetti: {
    position: 'absolute',
    top: 0,
    width: 8,
    height: 14,
    borderRadius: 3,
  },
  badge: {
    width: 92,
    height: 92,
    borderRadius: Radius.full,
    backgroundColor: Brand.success,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: Spacing.two,
  },
  centered: {
    textAlign: 'center',
  },
});
