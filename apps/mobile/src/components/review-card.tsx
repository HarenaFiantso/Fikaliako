import { StyleSheet, View } from 'react-native';

import type { ReviewItem } from '@fikaliako/api-client';

import { StarRow } from '@/components/ui/star-rating';

import { ThemedText } from '@/components/themed-text';

import { useTheme } from '@/hooks/use-theme';

import { Radius, Spacing } from '@/constants/theme';

export function ReviewCard({ review }: { review: ReviewItem }) {
  const theme = useTheme();

  const initial = review.author_name.trim()[0]?.toUpperCase() ?? '?';
  const date = new Date(review.created_at).toLocaleDateString('en', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  });

  return (
    <View style={[styles.card, { backgroundColor: theme.card, borderColor: theme.border }]}>
      <View style={styles.header}>
        <View style={[styles.avatar, { backgroundColor: theme.accent }]}>
          <ThemedText type="smallBold" style={{ color: theme.primary }}>
            {initial}
          </ThemedText>
        </View>
        <View style={styles.identity}>
          <ThemedText type="smallBold" numberOfLines={1}>
            {review.author_name}
          </ThemedText>
          <ThemedText type="small" themeColor="textSecondary">
            {date}
          </ThemedText>
        </View>
        <View style={styles.note}>
          <StarRow value={review.global_note} size={12} />
          <ThemedText type="smallBold">{review.global_note.toFixed(1)}</ThemedText>
        </View>
      </View>
      {review.comment ? (
        <ThemedText type="small" style={styles.comment}>
          {review.comment}
        </ThemedText>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    borderWidth: 1,
    borderRadius: Radius.xxl,
    padding: Spacing.three,
    gap: Spacing.two,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.two,
  },
  avatar: {
    width: 36,
    height: 36,
    borderRadius: Radius.full,
    alignItems: 'center',
    justifyContent: 'center',
  },
  identity: {
    flex: 1,
    gap: Spacing.half,
  },
  note: {
    alignItems: 'flex-end',
    gap: Spacing.half,
  },
  comment: {
    lineHeight: 21,
  },
});
