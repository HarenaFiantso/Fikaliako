import { Ionicons } from '@expo/vector-icons';
import { StyleSheet, View } from 'react-native';

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

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.half,
  },
});
