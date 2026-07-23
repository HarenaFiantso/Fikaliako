import { useState } from 'react';

import { Pressable, StyleSheet, View } from 'react-native';
import Animated, { useAnimatedStyle, withSpring } from 'react-native-reanimated';

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

  const thumbStyle = useAnimatedStyle(() => ({
    transform: [{ translateX: withSpring(index * segmentWidth, { damping: 22, stiffness: 300 }) }],
  }));

  return (
    <View
      style={[styles.track, { backgroundColor: theme.backgroundElement }]}
      onLayout={(event) => setWidth(event.nativeEvent.layout.width)}
    >
      {segmentWidth > 0 && (
        <Animated.View
          style={[
            styles.thumb,
            { width: segmentWidth, backgroundColor: theme.background },
            thumbStyle,
          ]}
        />
      )}
      {options.map((option) => (
        <Pressable
          key={option.value}
          accessibilityRole="button"
          style={styles.segment}
          onPress={() => onChange(option.value)}
        >
          <ThemedText type="small" themeColor={option.value === value ? 'text' : 'textSecondary'}>
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
  },
  thumb: {
    position: 'absolute',
    top: THUMB_PADDING,
    bottom: THUMB_PADDING,
    left: THUMB_PADDING,
    borderRadius: Radius.xl - THUMB_PADDING,
  },
  segment: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
