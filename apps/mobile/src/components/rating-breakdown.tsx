import { useEffect } from 'react';

import { StyleSheet, View } from 'react-native';
import Animated, {
  useAnimatedStyle,
  useSharedValue,
  withDelay,
  withTiming,
} from 'react-native-reanimated';

import type { RatingSummary } from '@fikaliako/api-client';

import { StarRow } from '@/components/ui/star-rating';

import { ThemedText } from '@/components/themed-text';

import { useTheme } from '@/hooks/use-theme';

import { FontFamily, Radius, Spacing } from '@/constants/theme';

const CRITERIA = [
  { key: 'avg_quality', label: 'Quality' },
  { key: 'avg_price', label: 'Price' },
  { key: 'avg_cleanliness', label: 'Cleanliness' },
  { key: 'avg_speed', label: 'Speed' },
  { key: 'avg_welcome', label: 'Welcome' },
] as const;

export function RatingBreakdown({ rating }: { rating: RatingSummary }) {
  const theme = useTheme();

  if (rating.count === 0 || rating.avg_global == null) return null;

  return (
    <View style={[styles.container, { backgroundColor: theme.card, borderColor: theme.border }]}>
      <View style={styles.overall}>
        <ThemedText style={styles.bigNote}>{rating.avg_global.toFixed(1)}</ThemedText>
        <StarRow value={rating.avg_global} size={15} />
        <ThemedText type="small" themeColor="textSecondary">
          {rating.count} review{rating.count > 1 ? 's' : ''}
        </ThemedText>
      </View>
      <View style={styles.bars}>
        {CRITERIA.map(
          (criterion, index) =>
            rating[criterion.key] != null && (
              <CriterionBar
                key={criterion.key}
                label={criterion.label}
                value={rating[criterion.key] ?? 0}
                delay={index * 80}
              />
            )
        )}
      </View>
    </View>
  );
}

function CriterionBar({ label, value, delay }: { label: string; value: number; delay: number }) {
  const theme = useTheme();

  const progress = useSharedValue(0);

  useEffect(() => {
    progress.value = withDelay(delay, withTiming(value / 5, { duration: 600 }));
  }, [delay, progress, value]);

  const fillStyle = useAnimatedStyle(() => ({
    width: `${progress.value * 100}%`,
  }));

  return (
    <View style={styles.barRow}>
      <ThemedText type="small" themeColor="textSecondary" style={styles.barLabel}>
        {label}
      </ThemedText>
      <View style={[styles.barTrack, { backgroundColor: theme.backgroundElement }]}>
        <Animated.View style={[styles.barFill, { backgroundColor: theme.primary }, fillStyle]} />
      </View>
      <ThemedText type="smallBold" style={styles.barValue}>
        {value.toFixed(1)}
      </ThemedText>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.four,
    borderWidth: 1,
    borderRadius: Radius.xxl,
    padding: Spacing.three,
  },
  overall: {
    alignItems: 'center',
    gap: Spacing.one,
  },
  bigNote: {
    fontFamily: FontFamily.extraBold,
    fontSize: 40,
    lineHeight: 46,
  },
  bars: {
    flex: 1,
    gap: Spacing.two,
  },
  barRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.two,
  },
  barLabel: {
    width: 76,
  },
  barTrack: {
    flex: 1,
    height: 6,
    borderRadius: Radius.full,
    overflow: 'hidden',
  },
  barFill: {
    height: '100%',
    borderRadius: Radius.full,
  },
  barValue: {
    width: 26,
    textAlign: 'right',
  },
});
