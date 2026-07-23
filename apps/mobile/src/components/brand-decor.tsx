import { useEffect } from 'react';

import { Ionicons } from '@expo/vector-icons';
import { type StyleProp, StyleSheet, type ViewStyle } from 'react-native';
import Animated, {
  Easing,
  interpolate,
  useAnimatedStyle,
  useSharedValue,
  withDelay,
  withRepeat,
  withTiming,
} from 'react-native-reanimated';

import { Colors } from '@/constants/theme';

/**
 * Fixed palette for the full-bleed brand screens (welcome/onboarding): the
 * terracotta moment stays identical in light and dark schemes on purpose.
 */
export const Brand = {
  terracotta: Colors.light.primary,
  cream: Colors.light.onPrimary,
  espresso: Colors.light.text,
  success: Colors.light.success,
  accent: Colors.light.accent,
} as const;

export type MotifShape = 'ring' | 'disc' | 'pill' | 'diamond';

export type MotifProps = {
  shape: MotifShape;
  size: number;
  x: number;
  y: number;
  rotate?: number;
  delay?: number;
  opacity?: number;
  color?: string;
  drift?: number;
};

/**
 * One oversized background shape, floating slowly. The vocabulary is culinary:
 * ring = plate, disc = mofo gasy, pill = rice grain, diamond = folded napkin.
 */
export function Motif({
  shape,
  size,
  x,
  y,
  rotate = 0,
  delay = 0,
  opacity = 0.08,
  color = Brand.cream,
  drift = 18,
}: MotifProps) {
  const progress = useSharedValue(0);

  useEffect(() => {
    progress.value = withDelay(
      delay,
      withRepeat(withTiming(1, { duration: 7000, easing: Easing.inOut(Easing.sin) }), -1, true)
    );
  }, [delay, progress]);

  const floatStyle = useAnimatedStyle(() => ({
    transform: [
      { translateY: interpolate(progress.value, [0, 1], [0, -drift]) },
      { rotate: `${rotate + interpolate(progress.value, [0, 1], [0, 6])}deg` },
    ],
  }));

  const shapeStyle: StyleProp<ViewStyle> = {
    ring: {
      width: size,
      height: size,
      borderRadius: size / 2,
      borderWidth: size * 0.14,
      borderColor: color,
    },
    disc: { width: size, height: size, borderRadius: size / 2, backgroundColor: color },
    pill: { width: size, height: size * 0.4, borderRadius: size, backgroundColor: color },
    diamond: {
      width: size * 0.8,
      height: size * 0.8,
      borderRadius: size * 0.22,
      backgroundColor: color,
      transform: [{ rotate: '45deg' }],
    },
  }[shape];

  return (
    <Animated.View
      pointerEvents="none"
      style={[styles.motif, { left: x, top: y, opacity }, shapeStyle, floatStyle]}
    />
  );
}

export type BrandMarkProps = {
  size?: number;
  tone?: 'cream' | 'terracotta';
};

/** The squircle app mark: a plate-and-fork glyph, used on brand and auth headers. */
export function BrandMark({ size = 44, tone = 'cream' }: BrandMarkProps) {
  const background = tone === 'cream' ? Brand.cream : Brand.terracotta;
  const glyph = tone === 'cream' ? Brand.terracotta : Brand.cream;

  return (
    <Animated.View
      style={[
        styles.mark,
        { width: size, height: size, borderRadius: size * 0.3, backgroundColor: background },
      ]}
    >
      <Ionicons name="restaurant" size={size * 0.52} color={glyph} />
    </Animated.View>
  );
}

const styles = StyleSheet.create({
  motif: {
    position: 'absolute',
  },
  mark: {
    alignItems: 'center',
    justifyContent: 'center',
  },
});
