import { useEffect, useState } from 'react';

import { GlassView, isLiquidGlassAvailable } from 'expo-glass-effect';
import { Platform, Pressable, StyleSheet, View } from 'react-native';
import Animated, { useAnimatedStyle, useSharedValue, withSpring } from 'react-native-reanimated';

import { ThemedText } from '@/components/themed-text';

import { useTheme } from '@/hooks/use-theme';

import { Radius } from '@/constants/theme';

const THUMB_PADDING = 3;

export type SegmentedOption<T extends string> = {
  value: T;
  label: string;
};

export type SegmentedControlProps<T extends string> = {
  options: readonly SegmentedOption<T>[];
  value: T;
  onChange: (value: T) => void;
};

export function SegmentedControl<T extends string>({
  options,
  value,
  onChange,
}: SegmentedControlProps<T>) {
  const theme = useTheme();

  const [width, setWidth] = useState(0);
  const index = Math.max(
    0,
    options.findIndex((option) => option.value === value)
  );
  const segmentWidth = width > 0 ? (width - THUMB_PADDING * 2) / options.length : 0;

  const position = useSharedValue(index);

  useEffect(() => {
    position.value = withSpring(index, { damping: 14, stiffness: 220 });
  }, [index, position]);

  /**
   * Same droplet motion as the tab bar: the distance to the nearest segment
   * drives a horizontal stretch and vertical squash, so the thumb deforms
   * mid-flight and settles round.
   */
  const thumbStyle = useAnimatedStyle(() => {
    const wobble = Math.abs(position.value - Math.round(position.value));
    return {
      transform: [
        { translateX: position.value * segmentWidth },
        { scaleX: 1 + wobble * 0.25 },
        { scaleY: 1 - wobble * 0.1 },
      ],
    };
  });

  const liquidGlass = Platform.OS === 'ios' && isLiquidGlassAvailable();

  return (
    <View
      style={[styles.track, { backgroundColor: theme.backgroundElement }]}
      onLayout={(event) => setWidth(event.nativeEvent.layout.width)}
    >
      {segmentWidth > 0 && (
        <Animated.View style={[styles.thumb, { width: segmentWidth }, thumbStyle]}>
          {liquidGlass ? (
            <GlassView
              glassEffectStyle="regular"
              isInteractive
              tintColor={`${theme.accent}80`}
              style={styles.thumbFill}
            />
          ) : (
            <View style={[styles.thumbFill, { backgroundColor: theme.background }]} />
          )}
        </Animated.View>
      )}
      {options.map((option) => (
        <Pressable
          key={option.value}
          accessibilityRole="button"
          style={styles.segment}
          onPress={() => onChange(option.value)}
        >
          <ThemedText
            type={option.value === value ? 'smallBold' : 'small'}
            style={{ color: option.value === value ? theme.primary : theme.textSecondary }}
          >
            {option.label}
          </ThemedText>
        </Pressable>
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  track: {
    flexDirection: 'row',
    borderRadius: Radius.xl,
    padding: THUMB_PADDING,
    minHeight: 44,
    alignItems: 'stretch',
    overflow: 'hidden',
  },
  thumb: {
    position: 'absolute',
    top: THUMB_PADDING,
    bottom: THUMB_PADDING,
    left: THUMB_PADDING,
  },
  thumbFill: {
    flex: 1,
    borderRadius: Radius.xl - THUMB_PADDING,
    overflow: 'hidden',
  },
  segment: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
