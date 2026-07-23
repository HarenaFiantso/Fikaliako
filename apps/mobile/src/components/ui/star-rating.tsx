import { Ionicons } from '@expo/vector-icons';
import { Pressable, StyleSheet, View } from 'react-native';
import Animated, {
  useAnimatedStyle,
  useSharedValue,
  withSequence,
  withSpring,
} from 'react-native-reanimated';

import { ThemedText } from '@/components/themed-text';

import { useTheme } from '@/hooks/use-theme';

import { Spacing } from '@/constants/theme';

export function StarRow({ value, size = 14 }: { value: number; size?: number }) {
  const theme = useTheme();

  return (
    <View style={styles.row}>
      {Array.from({ length: 5 }, (_, position) => {
        const name: keyof typeof Ionicons.glyphMap =
          value >= position + 0.75
            ? 'star'
            : value >= position + 0.25
              ? 'star-half'
              : 'star-outline';
        return <Ionicons key={position} name={name} size={size} color={theme.primary} />;
      })}
    </View>
  );
}

export function StarRatingInput({
  label,
  value,
  onChange,
}: {
  label: string;
  value: number;
  onChange: (value: number) => void;
}) {
  return (
    <View style={styles.inputRow}>
      <ThemedText type="default" style={styles.inputLabel}>
        {label}
      </ThemedText>
      <View style={styles.inputStars}>
        {Array.from({ length: 5 }, (_, position) => (
          <StarButton
            key={position}
            filled={value > position}
            label={`${position + 1} star${position > 0 ? 's' : ''}`}
            onPress={() => onChange(position + 1)}
          />
        ))}
      </View>
    </View>
  );
}

function StarButton({
  filled,
  label,
  onPress,
}: {
  filled: boolean;
  label: string;
  onPress: () => void;
}) {
  const theme = useTheme();

  const scale = useSharedValue(1);

  const starStyle = useAnimatedStyle(() => ({
    transform: [{ scale: scale.value }],
  }));

  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={label}
      hitSlop={Spacing.one}
      onPress={() => {
        scale.value = withSequence(
          withSpring(1.3, { damping: 12, stiffness: 500 }),
          withSpring(1, { damping: 14, stiffness: 320 })
        );
        onPress();
      }}
    >
      <Animated.View style={starStyle}>
        <Ionicons
          name={filled ? 'star' : 'star-outline'}
          size={28}
          color={filled ? theme.primary : theme.textSecondary}
        />
      </Animated.View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.half,
  },
  inputRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: Spacing.three,
    minHeight: 44,
  },
  inputLabel: {
    flexShrink: 1,
  },
  inputStars: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.one,
  },
});
