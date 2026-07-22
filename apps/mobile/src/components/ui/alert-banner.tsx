import { Ionicons } from '@expo/vector-icons';
import { StyleSheet } from 'react-native';
import Animated, { FadeInDown, FadeOut } from 'react-native-reanimated';

import { ThemedText } from '@/components/themed-text';

import { useTheme } from '@/hooks/use-theme';

import { Spacing } from '@/constants/theme';

export type AlertBannerProps = {
  kind: 'error' | 'success';
  message: string;
};

export function AlertBanner({ kind, message }: AlertBannerProps) {
  const theme = useTheme();
  const color = kind === 'error' ? theme.danger : theme.success;

  return (
    <Animated.View
      entering={FadeInDown.duration(200)}
      exiting={FadeOut.duration(150)}
      style={[styles.container, { backgroundColor: `${color}1A`, borderColor: `${color}4D` }]}
    >
      <Ionicons
        name={kind === 'error' ? 'alert-circle-outline' : 'checkmark-circle-outline'}
        size={20}
        color={color}
      />
      <ThemedText type="small" style={[styles.message, { color }]}>
        {message}
      </ThemedText>
    </Animated.View>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.two,
    borderWidth: 1,
    borderRadius: Spacing.three,
    paddingHorizontal: Spacing.three,
    paddingVertical: Spacing.two + Spacing.one,
  },
  message: {
    flex: 1,
  },
});
