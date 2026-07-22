import { ActivityIndicator, Pressable, type PressableProps, StyleSheet } from 'react-native';
import Animated, { useAnimatedStyle, useSharedValue, withSpring } from 'react-native-reanimated';

import { ThemedText } from '@/components/themed-text';

import { useTheme } from '@/hooks/use-theme';

import { FontFamily, Radius, Spacing } from '@/constants/theme';

const AnimatedPressable = Animated.createAnimatedComponent(Pressable);

export type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger';

export type ButtonProps = Omit<PressableProps, 'style' | 'children'> & {
  title: string;
  variant?: ButtonVariant;
  loading?: boolean;
};

export function Button({
  title,
  variant = 'primary',
  loading = false,
  disabled,
  onPressIn,
  onPressOut,
  ...pressableProps
}: ButtonProps) {
  const theme = useTheme();
  const scale = useSharedValue(1);
  const blocked = disabled || loading;

  const animatedStyle = useAnimatedStyle(() => ({
    transform: [{ scale: scale.value }],
  }));

  const background = {
    primary: theme.primary,
    secondary: theme.backgroundElement,
    ghost: 'transparent',
    danger: theme.backgroundElement,
  }[variant];

  const textColor = {
    primary: theme.onPrimary,
    secondary: theme.text,
    ghost: theme.primary,
    danger: theme.danger,
  }[variant];

  return (
    <AnimatedPressable
      accessibilityRole="button"
      disabled={blocked}
      onPressIn={(event) => {
        scale.value = withSpring(0.97, { damping: 20, stiffness: 400 });
        onPressIn?.(event);
      }}
      onPressOut={(event) => {
        scale.value = withSpring(1, { damping: 20, stiffness: 400 });
        onPressOut?.(event);
      }}
      style={[
        styles.base,
        { backgroundColor: background, opacity: blocked && !loading ? 0.5 : 1 },
        animatedStyle,
      ]}
      {...pressableProps}
    >
      {loading ? (
        <ActivityIndicator color={textColor} />
      ) : (
        <ThemedText type="default" style={[styles.title, { color: textColor }]}>
          {title}
        </ThemedText>
      )}
    </AnimatedPressable>
  );
}

const styles = StyleSheet.create({
  base: {
    minHeight: 52,
    borderRadius: Radius.xl,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: Spacing.four,
  },
  title: {
    fontFamily: FontFamily.bold,
  },
});
