import { type ReactNode } from 'react';

import { Pressable, type PressableProps, StyleSheet } from 'react-native';
import Animated, { useAnimatedStyle, useSharedValue, withSpring } from 'react-native-reanimated';

const AnimatedPressable = Animated.createAnimatedComponent(Pressable);

export type RoundButtonProps = Omit<PressableProps, 'style' | 'children'> & {
  size?: number;
  backgroundColor: string;
  accessibilityLabel: string;
  children: ReactNode;
};

/** Circular icon action, the primary navigation gesture on the brand screens. */
export function RoundButton({
  size = 64,
  backgroundColor,
  children,
  onPressIn,
  onPressOut,
  ...pressableProps
}: RoundButtonProps) {
  const scale = useSharedValue(1);

  const animatedStyle = useAnimatedStyle(() => ({
    transform: [{ scale: scale.value }],
  }));

  return (
    <AnimatedPressable
      accessibilityRole="button"
      onPressIn={(event) => {
        scale.value = withSpring(0.9, { damping: 18, stiffness: 380 });
        onPressIn?.(event);
      }}
      onPressOut={(event) => {
        scale.value = withSpring(1, { damping: 18, stiffness: 380 });
        onPressOut?.(event);
      }}
      style={[
        styles.base,
        { width: size, height: size, borderRadius: size / 2, backgroundColor },
        animatedStyle,
      ]}
      {...pressableProps}
    >
      {children}
    </AnimatedPressable>
  );
}

const styles = StyleSheet.create({
  base: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
  },
});
