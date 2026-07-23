import { Ionicons } from '@expo/vector-icons';
import { Pressable, StyleSheet } from 'react-native';
import Animated, {
  useAnimatedStyle,
  useSharedValue,
  withSequence,
  withSpring,
} from 'react-native-reanimated';

import type { EstablishmentSummary } from '@fikaliako/api-client';

import { useTheme } from '@/hooks/use-theme';

import { useFavorites } from '@/lib/favorites-store';

import { Spacing } from '@/constants/theme';

export function FavoriteButton({ establishment }: { establishment: EstablishmentSummary }) {
  const theme = useTheme();

  const favorite = useFavorites((state) => state.ids.has(establishment.id));
  const toggle = useFavorites((state) => state.toggle);

  const scale = useSharedValue(1);

  const heartStyle = useAnimatedStyle(() => ({
    transform: [{ scale: scale.value }],
  }));

  const onPress = () => {
    scale.value = withSequence(
      withSpring(favorite ? 0.75 : 1.35, { damping: 12, stiffness: 500 }),
      withSpring(1, { damping: 14, stiffness: 320 })
    );
    void toggle(establishment);
  };

  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={favorite ? 'Remove from favorites' : 'Add to favorites'}
      hitSlop={Spacing.two}
      onPress={onPress}
      style={styles.button}
    >
      <Animated.View style={heartStyle}>
        <Ionicons
          name={favorite ? 'heart' : 'heart-outline'}
          size={24}
          color={favorite ? theme.primary : theme.textSecondary}
        />
      </Animated.View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  button: {
    minWidth: 44,
    minHeight: 44,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
