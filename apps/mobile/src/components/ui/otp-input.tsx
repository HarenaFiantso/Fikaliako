import { useEffect, useRef, useState } from 'react';

import { Pressable, StyleSheet, TextInput } from 'react-native';
import Animated, {
  useAnimatedStyle,
  useSharedValue,
  withSequence,
  withTiming,
  ZoomIn,
} from 'react-native-reanimated';

import { ThemedText } from '@/components/themed-text';

import { useTheme } from '@/hooks/use-theme';

import { Spacing } from '@/constants/theme';

const CODE_LENGTH = 6;

export type OtpInputProps = {
  value: string;
  onChange: (value: string) => void;
  error?: string;
  autoFocus?: boolean;
};

export function OtpInput({ value, onChange, error, autoFocus = false }: OtpInputProps) {
  const theme = useTheme();
  const inputRef = useRef<TextInput>(null);
  const [focused, setFocused] = useState(false);
  const shake = useSharedValue(0);

  useEffect(() => {
    if (error) {
      shake.value = withSequence(
        withTiming(-8, { duration: 50 }),
        withTiming(8, { duration: 50 }),
        withTiming(-5, { duration: 50 }),
        withTiming(5, { duration: 50 }),
        withTiming(0, { duration: 50 })
      );
    }
  }, [error, shake]);

  const shakeStyle = useAnimatedStyle(() => ({
    transform: [{ translateX: shake.value }],
  }));

  const activeIndex = Math.min(value.length, CODE_LENGTH - 1);

  return (
    <Animated.View style={[styles.container, shakeStyle]}>
      <Pressable style={styles.cells} onPress={() => inputRef.current?.focus()}>
        {Array.from({ length: CODE_LENGTH }, (_, index) => {
          const digit = value[index];
          const active = focused && index === activeIndex && value.length < CODE_LENGTH + 1;
          return (
            <Animated.View
              key={index}
              style={[
                styles.cell,
                {
                  backgroundColor: theme.backgroundElement,
                  borderColor: error ? theme.danger : active ? theme.primary : theme.border,
                },
              ]}
            >
              {digit ? (
                <Animated.View entering={ZoomIn.duration(120)}>
                  <ThemedText type="subtitle" style={styles.digit}>
                    {digit}
                  </ThemedText>
                </Animated.View>
              ) : null}
            </Animated.View>
          );
        })}
      </Pressable>
      <TextInput
        ref={inputRef}
        style={[StyleSheet.absoluteFill, styles.hiddenInput]}
        value={value}
        onChangeText={(text) => onChange(text.replace(/[^0-9]/g, '').slice(0, CODE_LENGTH))}
        onFocus={() => setFocused(true)}
        onBlur={() => setFocused(false)}
        keyboardType="number-pad"
        inputMode="numeric"
        autoComplete="one-time-code"
        textContentType="oneTimeCode"
        autoFocus={autoFocus}
        caretHidden
      />
    </Animated.View>
  );
}

const styles = StyleSheet.create({
  container: {
    gap: Spacing.two,
  },
  cells: {
    flexDirection: 'row',
    gap: Spacing.two,
    justifyContent: 'center',
  },
  cell: {
    width: 48,
    height: 56,
    borderWidth: 1,
    borderRadius: Spacing.three,
    alignItems: 'center',
    justifyContent: 'center',
  },
  digit: {
    fontSize: 24,
    lineHeight: 32,
  },
  hiddenInput: {
    opacity: 0,
  },
});
