import { type Ref, useEffect, useState } from 'react';

import { Ionicons } from '@expo/vector-icons';
import { Pressable, StyleSheet, TextInput, type TextInputProps, View } from 'react-native';
import Animated, {
  FadeInDown,
  FadeOut,
  interpolateColor,
  useAnimatedStyle,
  useSharedValue,
  withSequence,
  withTiming,
} from 'react-native-reanimated';

import { ThemedText } from '@/components/themed-text';

import { useTheme } from '@/hooks/use-theme';

import { FontFamily, Radius, Spacing } from '@/constants/theme';

export type TextFieldProps = Omit<TextInputProps, 'style'> & {
  label: string;
  error?: string;
  secure?: boolean;
  inputRef?: Ref<TextInput>;
};

export function TextField({
  label,
  error,
  secure = false,
  inputRef,
  onFocus,
  onBlur,
  ...inputProps
}: TextFieldProps) {
  const theme = useTheme();

  const [hidden, setHidden] = useState(secure);

  const focus = useSharedValue(0);
  const shake = useSharedValue(0);

  useEffect(() => {
    if (error) {
      shake.value = withSequence(
        withTiming(-6, { duration: 50 }),
        withTiming(6, { duration: 50 }),
        withTiming(-4, { duration: 50 }),
        withTiming(4, { duration: 50 }),
        withTiming(0, { duration: 50 })
      );
    }
  }, [error, shake]);

  const wrapperStyle = useAnimatedStyle(() => ({
    transform: [{ translateX: shake.value }],
    borderColor: error
      ? theme.danger
      : interpolateColor(focus.value, [0, 1], [theme.border, theme.primary]),
  }));

  const handleFocus: TextInputProps['onFocus'] = (event) => {
    focus.value = withTiming(1, { duration: 150 });
    onFocus?.(event);
  };

  const handleBlur: TextInputProps['onBlur'] = (event) => {
    focus.value = withTiming(0, { duration: 150 });
    onBlur?.(event);
  };

  return (
    <View style={styles.container}>
      <ThemedText type="smallBold">{label}</ThemedText>
      <Animated.View style={[styles.inputWrapper, { backgroundColor: theme.card }, wrapperStyle]}>
        <TextInput
          ref={inputRef}
          style={[styles.input, { color: theme.text }]}
          placeholderTextColor={theme.textSecondary}
          secureTextEntry={hidden}
          onFocus={handleFocus}
          onBlur={handleBlur}
          {...inputProps}
        />
        {secure && (
          <Pressable
            onPress={() => setHidden((value) => !value)}
            hitSlop={Spacing.two}
            accessibilityLabel={hidden ? 'Show password' : 'Hide password'}
          >
            <Ionicons
              name={hidden ? 'eye-outline' : 'eye-off-outline'}
              size={20}
              color={theme.textSecondary}
            />
          </Pressable>
        )}
      </Animated.View>
      {error ? (
        <Animated.View entering={FadeInDown.duration(150)} exiting={FadeOut.duration(100)}>
          <ThemedText type="small" style={{ color: theme.danger }}>
            {error}
          </ThemedText>
        </Animated.View>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    gap: Spacing.two,
  },
  inputWrapper: {
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1,
    borderRadius: Radius.full,
    paddingHorizontal: Spacing.three + Spacing.one,
  },
  input: {
    flex: 1,
    fontFamily: FontFamily.medium,
    fontSize: 16,
    paddingVertical: 14,
    minHeight: 48,
  },
});
